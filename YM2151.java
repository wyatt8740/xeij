//========================================================================================
//  YM2151.java
//    en:Frequency modulation sound source
//    ja:FM音源
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  YM2151
//  参考にしたもの
//    MAME (Multiple Arcade Machine Emulator)
//      http://mamedev.org/
//      mame0148s.zip/mame.zip/src/emu/sound/ym2151.c
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class YM2151 {

  public static final boolean OPM_ON = true;  //true=OPMを出力する
  public static int opmOutputMask;  //-1=OPMを出力する

  public static final boolean OPM_EG_X = true;
  public static final boolean OPM_TRANSITION_MASK = true;  //EGの遷移判定にマスクを使う
  public static final boolean OPM_EXTRA_TL_TABLE = true;  //opmTLTableに余分な0を追加して条件分岐を減らす

  public static final int OPM_OSC_FREQ = 4000000;  //クロック周波数(Hz)。4000000Hz
  public static final int OPM_SAMPLE_FREQ = OPM_OSC_FREQ / 64;  //サンプリング周波数(Hz)。62500Hz
  public static final long OPM_SAMPLE_TIME = XEiJ.TMR_FREQ / OPM_SAMPLE_FREQ;  //1サンプルの時間(XEiJ.TMR_FREQ単位)。16000000ps
  public static final int OPM_BLOCK_SAMPLES = OPM_SAMPLE_FREQ / SoundSource.SND_BLOCK_FREQ;  //1ブロックのサンプル数。2500

  //バッファ
  //  バッファはレジスタを更新する直前とライン出力に転送する直前とCSMでキーONする直前にopmUpdateで更新する
  //  ステレオのときはleft→rightの順序
  public static final int[] opmBuffer = new int[SoundSource.SND_CHANNELS * (OPM_BLOCK_SAMPLES + 1)];  //1サンプル追加
  public static int opmPointer;  //バッファの更新位置。opmUpdateで使用する。ライン出力に転送されると0に戻る

  //レジスタ
  public static int opmAddress;  //アドレス

  public static final int OPM_PHASE_SHIFT = 16;
  public static final int OPM_PHASE_MASK = (1 << OPM_PHASE_SHIFT) - 1;  //65535
  public static final int OPM_SIN_BITS = 10;
  public static final int OPM_SIN_TABLE_SIZE = 1 << OPM_SIN_BITS;  //1024
  public static final int OPM_SIN_MASK = OPM_SIN_TABLE_SIZE - 1;  //1023

  public static final int OPM_TL_BITS = 10;
  public static final int OPM_TL_SIZE = 1 << OPM_TL_BITS;  //1024

  public static final int OPM_TL_BLOCK_SIZE = 256 * 2;  //512
  public static final int OPM_TL_TABLE_SIZE = 13 * OPM_TL_BLOCK_SIZE;  //6656
  public static final int OPM_ENV_QUIET = OPM_TL_TABLE_SIZE >> 3;  //832

  //タイマ
  //  RESET,IRQEN,LOAD,ISTのビットの順序
  //  Inside X68000にISTのビットの順序が上位からA|Bと書かれているが、B|Aの誤り
  public static final int OPM_RESETB = 0b00100000;
  public static final int OPM_RESETA = 0b00010000;
  public static final int OPM_IRQENB = 0b00001000;
  public static final int OPM_IRQENA = 0b00000100;
  public static final int OPM_LOADB  = 0b00000010;
  public static final int OPM_LOADA  = 0b00000001;
  public static final int OPM_ISTB   = 0b00000010;
  public static final int OPM_ISTA   = 0b00000001;
  public static int opmCLKA;  //タイマAの周波数。0～1023
  public static int opmCLKB;  //タイマBの周波数。0～255
  public static long opmIntervalA;  //タイマAの周期。XEiJ.TMR_FREQ/OPM_OSC_FREQ*64*(1024-opmCLKA)(XEiJ.TMR_FREQ単位)
  public static long opmIntervalB;  //タイマBの周期。XEiJ.TMR_FREQ/OPM_OSC_FREQ*1024*(256-opmCLKB)(XEiJ.TMR_FREQ単位)
  public static long opmClockA;  //タイマAがオーバーフローする時刻(XEiJ.TMR_FREQ単位)。opmClockA!=FAR_FUTUREならばタイマA動作中
  public static long opmClockB;  //タイマBがオーバーフローする時刻(XEiJ.TMR_FREQ単位)。opmClockB!=FAR_FUTUREならばタイマB動作中
  public static int opmISTA;  //OPM_ISTA=タイマAオーバーフロー
  public static int opmISTB;  //OPM_ISTB=タイマBオーバーフロー。opmirqはopmISTAとopmISTBが両方0のとき1、どちらかが0でなければ0
  public static boolean opmIRQENA;  //true=タイマAのオーバーフローをopmISTAに反映する
  public static boolean opmIRQENB;  //true=タイマBのオーバーフローをopmISTBに反映する
  public static boolean opmCSMOn;  //true=タイマAがオーバーフローしたら全スロットキーON

  public static long opmBusyClock;  //ビジーが終了する時刻(XEiJ.TMR_FREQ単位)。FM音源レジスタに最後に書き込んだときのXEiJ.mpuClockTime+XEiJ.TMR_FREQ/OPM_OSC_FREQ*68。ステータスレジスタのビジーフラグを変化させるだけで、FM音源レジスタに書き込めなくなるわけではない

  //PG
  //
  //  YM2151は入力クロックとして3.58MHzを与えたときにラの音(キーコード0x4a)の周波数が440Hzになるように設計されている
  //    (2062*2^(4-2))*3.58e+6/2^26=439.9991
  //  X68000では4MHzが与えられているため、およそ2度高い音が出る
  //    (2062*2^(4-2))*4e+6/2^26=491.6191
  //  キーコードを2減らすことで本来のラの音に近い音が出る。キーコードを減らすときは欠番の跨ぎ方に注意する
  //    (1837*2^(4-2))*4e+6/2^26=437.9749
  //  より正確な音を出すにはキーフラクションも調整する。しかし、それでも3.58MHzを与えたときほど正確なラの音は出せない
  //    (1845*2^(4-2))*4e+6/2^26=439.8823
  //    (1846*2^(4-2))*4e+6/2^26=440.1207
  //
  //  細かい波形はD/AやA/Dを通るときに変化してしまうので観測が難しいが、周波数はある程度の時間観測して振動回数を数えれば分かる
  //  X68030の実機で観測し、1オクターブ(16*64=1024音)がすべて整数になる最小の係数を求めたところ、音が重複している箇所も含めてMAMEが持っているテーブルとまったく同じものが得られた
  //
  //  KFが平均音階の1度を正確に64分割した結果を丸めたものであればテーブルの全体を自動生成できるはずだが、間隔にばらつきがあるため単純に分割しただけでは再現できない
  //  故意にばらつかせてあると思われるが、どのように手を加えたのかは不明
  //
  //  Inside X68000にYM2151は3.579545MHzを与えたときに正確な音が出ると書かれているが、3.58MHzと書くべきである
  //  カラーサブキャリア周波数である315/88=3.579545454...MHzを流用しやすいように設計されていることは間違いないが、
  //  実際には3.58MHzを与えた方が正確なのだから、YM2151に与えるべき周波数としてカラーサブキャリア周波数を小数点以下6桁まで書くのは適切ではない
  //    echo l=["C#","D","D#","E","F","F#","G","G#","A","A#","B","C"];a=[1299,1376,1458,1545,1637,1734,1837,1946,2062,2185,2315,2452];print("       T Hz    @3.579545MHz  Ecent   @3.58MHz    Ecent");s1=0;s2=0;for(n=-8,3,t=eval("440*2^(n/12)");o1=eval("a[9+n]*4*3.579545e+6/2^26");o2=eval("a[9+n]*4*3.58e+6/2^26");e1=eval("log(o1/t)/log(2^(1/12))")*100;e2=eval("log(o2/t)/log(2^(1/12))")*100;printf("%-2s  %10.6f  %10.6f  %+6.3f  %10.6f  %+6.3f%c",l[9+n],t,o1,e1,o2,e2,10);s1+=abs(e1);s2+=abs(e2));printf("                            %6.3f              %6.3f%c",s1/12,s2/12,10) | gp-2.5 -q
  //           T Hz    @3.579545MHz  Ecent   @3.58MHz    Ecent
  //    C#  277.182631  277.151403  -0.195  277.186632  +0.025
  //    D   293.664768  293.579931  -0.500  293.617249  -0.280
  //    D#  311.126984  311.075247  -0.288  311.114788  -0.068
  //    E   329.627557  329.637350  +0.051  329.679251  +0.271
  //    F   349.228231  349.266241  +0.188  349.310637  +0.408
  //    F#  369.994423  369.961919  -0.152  370.008945  +0.068
  //    G   391.995436  391.937743  -0.255  391.987562  -0.035
  //    G#  415.304698  415.193711  -0.463  415.246487  -0.243
  //    A   440.000000  439.943182  -0.224  439.999104  -0.004
  //    A#  466.163762  466.186155  +0.083  466.245413  +0.303
  //    B   493.883301  493.922631  +0.138  493.985415  +0.358
  //    C   523.251131  523.152610  -0.326  523.219109  -0.106
  //                                 0.239               0.181
  //
/*
  public static final int[] OPM_PG_BASE_TABLE = {
    //0x1f,0x20
    1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1308, 1309, 1310, 1311, 1313, 1314, 1315, 1316,
    1318, 1319, 1320, 1321, 1322, 1323, 1324, 1325, 1327, 1328, 1329, 1330, 1332, 1333, 1334, 1335,
    1337, 1338, 1339, 1340, 1341, 1342, 1343, 1344, 1346, 1347, 1348, 1349, 1351, 1352, 1353, 1354,
    1356, 1357, 1358, 1359, 1361, 1362, 1363, 1364, 1366, 1367, 1368, 1369, 1371, 1372, 1373, 1374,
    //0x21
    1376, 1377, 1378, 1379, 1381, 1382, 1383, 1384, 1386, 1387, 1388, 1389, 1391, 1392, 1393, 1394,
    1396, 1397, 1398, 1399, 1401, 1402, 1403, 1404, 1406, 1407, 1408, 1409, 1411, 1412, 1413, 1414,
    1416, 1417, 1418, 1419, 1421, 1422, 1423, 1424, 1426, 1427, 1429, 1430, 1431, 1432, 1434, 1435,
    1437, 1438, 1439, 1440, 1442, 1443, 1444, 1445, 1447, 1448, 1449, 1450, 1452, 1453, 1454, 1455,
    //0x22
    1458, 1459, 1460, 1461, 1463, 1464, 1465, 1466, 1468, 1469, 1471, 1472, 1473, 1474, 1476, 1477,
    1479, 1480, 1481, 1482, 1484, 1485, 1486, 1487, 1489, 1490, 1492, 1493, 1494, 1495, 1497, 1498,
    1501, 1502, 1503, 1504, 1506, 1507, 1509, 1510, 1512, 1513, 1514, 1515, 1517, 1518, 1520, 1521,
    1523, 1524, 1525, 1526, 1528, 1529, 1531, 1532, 1534, 1535, 1536, 1537, 1539, 1540, 1542, 1543,
    //0x23,0x24
    1545, 1546, 1547, 1548, 1550, 1551, 1553, 1554, 1556, 1557, 1558, 1559, 1561, 1562, 1564, 1565,
    1567, 1568, 1569, 1570, 1572, 1573, 1575, 1576, 1578, 1579, 1580, 1581, 1583, 1584, 1586, 1587,
    1590, 1591, 1592, 1593, 1595, 1596, 1598, 1599, 1601, 1602, 1604, 1605, 1607, 1608, 1609, 1610,
    1613, 1614, 1615, 1616, 1618, 1619, 1621, 1622, 1624, 1625, 1627, 1628, 1630, 1631, 1632, 1633,
    //0x25
    1637, 1638, 1639, 1640, 1642, 1643, 1645, 1646, 1648, 1649, 1651, 1652, 1654, 1655, 1656, 1657,
    1660, 1661, 1663, 1664, 1666, 1667, 1669, 1670, 1672, 1673, 1675, 1676, 1678, 1679, 1681, 1682,
    1685, 1686, 1688, 1689, 1691, 1692, 1694, 1695, 1697, 1698, 1700, 1701, 1703, 1704, 1706, 1707,
    1709, 1710, 1712, 1713, 1715, 1716, 1718, 1719, 1721, 1722, 1724, 1725, 1727, 1728, 1730, 1731,
    //0x26
    1734, 1735, 1737, 1738, 1740, 1741, 1743, 1744, 1746, 1748, 1749, 1751, 1752, 1754, 1755, 1757,
    1759, 1760, 1762, 1763, 1765, 1766, 1768, 1769, 1771, 1773, 1774, 1776, 1777, 1779, 1780, 1782,
    1785, 1786, 1788, 1789, 1791, 1793, 1794, 1796, 1798, 1799, 1801, 1802, 1804, 1806, 1807, 1809,
    1811, 1812, 1814, 1815, 1817, 1819, 1820, 1822, 1824, 1825, 1827, 1828, 1830, 1832, 1833, 1835,
    //0x27,0x28
    1837, 1838, 1840, 1841, 1843, 1845, 1846, 1848, 1850, 1851, 1853, 1854, 1856, 1858, 1859, 1861,
    1864, 1865, 1867, 1868, 1870, 1872, 1873, 1875, 1877, 1879, 1880, 1882, 1884, 1885, 1887, 1888,
    1891, 1892, 1894, 1895, 1897, 1899, 1900, 1902, 1904, 1906, 1907, 1909, 1911, 1912, 1914, 1915,
    1918, 1919, 1921, 1923, 1925, 1926, 1928, 1930, 1932, 1933, 1935, 1937, 1939, 1940, 1942, 1944,
    //0x29
    1946, 1947, 1949, 1951, 1953, 1954, 1956, 1958, 1960, 1961, 1963, 1965, 1967, 1968, 1970, 1972,
    1975, 1976, 1978, 1980, 1982, 1983, 1985, 1987, 1989, 1990, 1992, 1994, 1996, 1997, 1999, 2001,
    2003, 2004, 2006, 2008, 2010, 2011, 2013, 2015, 2017, 2019, 2021, 2022, 2024, 2026, 2028, 2029,
    2032, 2033, 2035, 2037, 2039, 2041, 2043, 2044, 2047, 2048, 2050, 2052, 2054, 2056, 2058, 2059,
    //0x2a
    2062, 2063, 2065, 2067, 2069, 2071, 2073, 2074, 2077, 2078, 2080, 2082, 2084, 2086, 2088, 2089,
    2092, 2093, 2095, 2097, 2099, 2101, 2103, 2104, 2107, 2108, 2110, 2112, 2114, 2116, 2118, 2119,
    2122, 2123, 2125, 2127, 2129, 2131, 2133, 2134, 2137, 2139, 2141, 2142, 2145, 2146, 2148, 2150,
    2153, 2154, 2156, 2158, 2160, 2162, 2164, 2165, 2168, 2170, 2172, 2173, 2176, 2177, 2179, 2181,
    //0x2b,0x2c
    2185, 2186, 2188, 2190, 2192, 2194, 2196, 2197, 2200, 2202, 2204, 2205, 2208, 2209, 2211, 2213,
    2216, 2218, 2220, 2222, 2223, 2226, 2227, 2230, 2232, 2234, 2236, 2238, 2239, 2242, 2243, 2246,
    2249, 2251, 2253, 2255, 2256, 2259, 2260, 2263, 2265, 2267, 2269, 2271, 2272, 2275, 2276, 2279,
    2281, 2283, 2285, 2287, 2288, 2291, 2292, 2295, 2297, 2299, 2301, 2303, 2304, 2307, 2308, 2311,
    //0x2d
    2315, 2317, 2319, 2321, 2322, 2325, 2326, 2329, 2331, 2333, 2335, 2337, 2338, 2341, 2342, 2345,
    2348, 2350, 2352, 2354, 2355, 2358, 2359, 2362, 2364, 2366, 2368, 2370, 2371, 2374, 2375, 2378,
    2382, 2384, 2386, 2388, 2389, 2392, 2393, 2396, 2398, 2400, 2402, 2404, 2407, 2410, 2411, 2414,
    2417, 2419, 2421, 2423, 2424, 2427, 2428, 2431, 2433, 2435, 2437, 2439, 2442, 2445, 2446, 2449,
    //0x2e
    2452, 2454, 2456, 2458, 2459, 2462, 2463, 2466, 2468, 2470, 2472, 2474, 2477, 2480, 2481, 2484,
    2488, 2490, 2492, 2494, 2495, 2498, 2499, 2502, 2504, 2506, 2508, 2510, 2513, 2516, 2517, 2520,
    2524, 2526, 2528, 2530, 2531, 2534, 2535, 2538, 2540, 2542, 2544, 2546, 2549, 2552, 2553, 2556,
    2561, 2563, 2565, 2567, 2568, 2571, 2572, 2575, 2577, 2579, 2581, 2583, 2586, 2589, 2590, 2593,
  };
*/
  //  perl misc/itoc.pl xeij/YM2151.java OPM_PG_BASE_TABLE
  public static final char[] OPM_PG_BASE_TABLE = "\u0513\u0514\u0515\u0516\u0517\u0518\u0519\u051a\u051c\u051d\u051e\u051f\u0521\u0522\u0523\u0524\u0526\u0527\u0528\u0529\u052a\u052b\u052c\u052d\u052f\u0530\u0531\u0532\u0534\u0535\u0536\u0537\u0539\u053a\u053b\u053c\u053d\u053e\u053f\u0540\u0542\u0543\u0544\u0545\u0547\u0548\u0549\u054a\u054c\u054d\u054e\u054f\u0551\u0552\u0553\u0554\u0556\u0557\u0558\u0559\u055b\u055c\u055d\u055e\u0560\u0561\u0562\u0563\u0565\u0566\u0567\u0568\u056a\u056b\u056c\u056d\u056f\u0570\u0571\u0572\u0574\u0575\u0576\u0577\u0579\u057a\u057b\u057c\u057e\u057f\u0580\u0581\u0583\u0584\u0585\u0586\u0588\u0589\u058a\u058b\u058d\u058e\u058f\u0590\u0592\u0593\u0595\u0596\u0597\u0598\u059a\u059b\u059d\u059e\u059f\u05a0\u05a2\u05a3\u05a4\u05a5\u05a7\u05a8\u05a9\u05aa\u05ac\u05ad\u05ae\u05af\u05b2\u05b3\u05b4\u05b5\u05b7\u05b8\u05b9\u05ba\u05bc\u05bd\u05bf\u05c0\u05c1\u05c2\u05c4\u05c5\u05c7\u05c8\u05c9\u05ca\u05cc\u05cd\u05ce\u05cf\u05d1\u05d2\u05d4\u05d5\u05d6\u05d7\u05d9\u05da\u05dd\u05de\u05df\u05e0\u05e2\u05e3\u05e5\u05e6\u05e8\u05e9\u05ea\u05eb\u05ed\u05ee\u05f0\u05f1\u05f3\u05f4\u05f5\u05f6\u05f8\u05f9\u05fb\u05fc\u05fe\u05ff\u0600\u0601\u0603\u0604\u0606\u0607\u0609\u060a\u060b\u060c\u060e\u060f\u0611\u0612\u0614\u0615\u0616\u0617\u0619\u061a\u061c\u061d\u061f\u0620\u0621\u0622\u0624\u0625\u0627\u0628\u062a\u062b\u062c\u062d\u062f\u0630\u0632\u0633\u0636\u0637\u0638\u0639\u063b\u063c\u063e\u063f\u0641\u0642\u0644\u0645\u0647\u0648\u0649\u064a\u064d\u064e\u064f\u0650\u0652\u0653\u0655\u0656\u0658\u0659\u065b\u065c\u065e\u065f\u0660\u0661\u0665\u0666\u0667\u0668\u066a\u066b\u066d\u066e\u0670\u0671\u0673\u0674\u0676\u0677\u0678\u0679\u067c\u067d\u067f\u0680\u0682\u0683\u0685\u0686\u0688\u0689\u068b\u068c\u068e\u068f\u0691\u0692\u0695\u0696\u0698\u0699\u069b\u069c\u069e\u069f\u06a1\u06a2\u06a4\u06a5\u06a7\u06a8\u06aa\u06ab\u06ad\u06ae\u06b0\u06b1\u06b3\u06b4\u06b6\u06b7\u06b9\u06ba\u06bc\u06bd\u06bf\u06c0\u06c2\u06c3\u06c6\u06c7\u06c9\u06ca\u06cc\u06cd\u06cf\u06d0\u06d2\u06d4\u06d5\u06d7\u06d8\u06da\u06db\u06dd\u06df\u06e0\u06e2\u06e3\u06e5\u06e6\u06e8\u06e9\u06eb\u06ed\u06ee\u06f0\u06f1\u06f3\u06f4\u06f6\u06f9\u06fa\u06fc\u06fd\u06ff\u0701\u0702\u0704\u0706\u0707\u0709\u070a\u070c\u070e\u070f\u0711\u0713\u0714\u0716\u0717\u0719\u071b\u071c\u071e\u0720\u0721\u0723\u0724\u0726\u0728\u0729\u072b\u072d\u072e\u0730\u0731\u0733\u0735\u0736\u0738\u073a\u073b\u073d\u073e\u0740\u0742\u0743\u0745\u0748\u0749\u074b\u074c\u074e\u0750\u0751\u0753\u0755\u0757\u0758\u075a\u075c\u075d\u075f\u0760\u0763\u0764\u0766\u0767\u0769\u076b\u076c\u076e\u0770\u0772\u0773\u0775\u0777\u0778\u077a\u077b\u077e\u077f\u0781\u0783\u0785\u0786\u0788\u078a\u078c\u078d\u078f\u0791\u0793\u0794\u0796\u0798\u079a\u079b\u079d\u079f\u07a1\u07a2\u07a4\u07a6\u07a8\u07a9\u07ab\u07ad\u07af\u07b0\u07b2\u07b4\u07b7\u07b8\u07ba\u07bc\u07be\u07bf\u07c1\u07c3\u07c5\u07c6\u07c8\u07ca\u07cc\u07cd\u07cf\u07d1\u07d3\u07d4\u07d6\u07d8\u07da\u07db\u07dd\u07df\u07e1\u07e3\u07e5\u07e6\u07e8\u07ea\u07ec\u07ed\u07f0\u07f1\u07f3\u07f5\u07f7\u07f9\u07fb\u07fc\u07ff\u0800\u0802\u0804\u0806\u0808\u080a\u080b\u080e\u080f\u0811\u0813\u0815\u0817\u0819\u081a\u081d\u081e\u0820\u0822\u0824\u0826\u0828\u0829\u082c\u082d\u082f\u0831\u0833\u0835\u0837\u0838\u083b\u083c\u083e\u0840\u0842\u0844\u0846\u0847\u084a\u084b\u084d\u084f\u0851\u0853\u0855\u0856\u0859\u085b\u085d\u085e\u0861\u0862\u0864\u0866\u0869\u086a\u086c\u086e\u0870\u0872\u0874\u0875\u0878\u087a\u087c\u087d\u0880\u0881\u0883\u0885\u0889\u088a\u088c\u088e\u0890\u0892\u0894\u0895\u0898\u089a\u089c\u089d\u08a0\u08a1\u08a3\u08a5\u08a8\u08aa\u08ac\u08ae\u08af\u08b2\u08b3\u08b6\u08b8\u08ba\u08bc\u08be\u08bf\u08c2\u08c3\u08c6\u08c9\u08cb\u08cd\u08cf\u08d0\u08d3\u08d4\u08d7\u08d9\u08db\u08dd\u08df\u08e0\u08e3\u08e4\u08e7\u08e9\u08eb\u08ed\u08ef\u08f0\u08f3\u08f4\u08f7\u08f9\u08fb\u08fd\u08ff\u0900\u0903\u0904\u0907\u090b\u090d\u090f\u0911\u0912\u0915\u0916\u0919\u091b\u091d\u091f\u0921\u0922\u0925\u0926\u0929\u092c\u092e\u0930\u0932\u0933\u0936\u0937\u093a\u093c\u093e\u0940\u0942\u0943\u0946\u0947\u094a\u094e\u0950\u0952\u0954\u0955\u0958\u0959\u095c\u095e\u0960\u0962\u0964\u0967\u096a\u096b\u096e\u0971\u0973\u0975\u0977\u0978\u097b\u097c\u097f\u0981\u0983\u0985\u0987\u098a\u098d\u098e\u0991\u0994\u0996\u0998\u099a\u099b\u099e\u099f\u09a2\u09a4\u09a6\u09a8\u09aa\u09ad\u09b0\u09b1\u09b4\u09b8\u09ba\u09bc\u09be\u09bf\u09c2\u09c3\u09c6\u09c8\u09ca\u09cc\u09ce\u09d1\u09d4\u09d5\u09d8\u09dc\u09de\u09e0\u09e2\u09e3\u09e6\u09e7\u09ea\u09ec\u09ee\u09f0\u09f2\u09f5\u09f8\u09f9\u09fc\u0a01\u0a03\u0a05\u0a07\u0a08\u0a0b\u0a0c\u0a0f\u0a11\u0a13\u0a15\u0a17\u0a1a\u0a1d\u0a1e\u0a21".toCharArray ();

  //EG
  //  速度の選択
  //  opmEGCounterから3bit取り出すときのbit番号
  //  大きいほど変化が遅くなる
/*
  public static final int[] OPM_EG_SHIFT_TABLE = {
    //+0+1  +2  +3  +4  +5  +6  +7  +8  +9 +10 +11 +12 +13 +14 +15
    0 ,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //0..15
    0 ,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //16..31
    11, 11, 11, 11, 10, 10, 10, 10,  9,  9,  9,  9,  8,  8,  8,  8,  //32..47
    7 ,  7,  7,  7,  6,  6,  6,  6,  5,  5,  5,  5,  4,  4,  4,  4,  //48..63
    3 ,  3,  3,  3,  2,  2,  2,  2,  1,  1,  1,  1,  0,  0,  0,  0,  //64..79
    0 ,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //80..95
    0 ,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //96..111
    0 ,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //112..127
  };
*/
  //  perl misc/itob.pl xeij/YM2151.java OPM_EG_SHIFT_TABLE
  public static final byte[] OPM_EG_SHIFT_TABLE = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\13\13\13\13\n\n\n\n\t\t\t\t\b\b\b\b\7\7\7\7\6\6\6\6\5\5\5\5\4\4\4\4\3\3\3\3\2\2\2\2\1\1\1\1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //  傾きの選択
  //  OPM_EG_INCREMENTAL_TABLEから8個取り出すときの開始位置
  //  大きいほど変化が速くなる
/*
  public static final int[] OPM_EG_PAGE_TABLE_1 = {
    //+0  +1   +2   +3   +4   +5   +6   +7   +8   +9  +10  +11  +12  +13  +14  +15
    144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144,  //0..15
    144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144, 144,  //16..31
    0  ,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,  //32..47
    0  ,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,  //48..63
    0  ,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,   0,   8,  16,  24,  //64..79
    32 ,  40,  48,  56,  64,  72,  80,  88,  96, 104, 112, 120, 128, 128, 128, 128,  //80..95
    128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,  //96..111
    128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,  //112..127
  };
*/
  //  perl misc/itoc.pl xeij/YM2151.java OPM_EG_PAGE_TABLE_1
  public static final char[] OPM_EG_PAGE_TABLE_1 = "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30\0\b\20\30 (08@HPX`hpx\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200\200".toCharArray ();
  //  増分
/*
  public static final int[] OPM_EG_INCREMENTAL_TABLE = {
    0, 1, 0, 1, 0, 1, 0, 1,  //0..7
    0, 1, 0, 1, 1, 1, 0, 1,  //8..15
    0, 1, 1, 1, 0, 1, 1, 1,  //16..23
    0, 1, 1, 1, 1, 1, 1, 1,  //24..31
    1, 1, 1, 1, 1, 1, 1, 1,  //32..39
    1, 1, 1, 2, 1, 1, 1, 2,  //40..47
    1, 2, 1, 2, 1, 2, 1, 2,  //48..55
    1, 2, 2, 2, 1, 2, 2, 2,  //56..63
    2, 2, 2, 2, 2, 2, 2, 2,  //64..71
    2, 2, 2, 4, 2, 2, 2, 4,  //72..79
    2, 4, 2, 4, 2, 4, 2, 4,  //80..87
    2, 4, 4, 4, 2, 4, 4, 4,  //88..95
    4, 4, 4, 4, 4, 4, 4, 4,  //96..103
    4, 4, 4, 8, 4, 4, 4, 8,  //104..111
    4, 8, 4, 8, 4, 8, 4, 8,  //112..119
    4, 8, 8, 8, 4, 8, 8, 8,  //120..127
    8, 8, 8, 8, 8, 8, 8, 8,  //128..135
    16, 16, 16, 16, 16, 16, 16, 16,  //136..143
    0, 0, 0, 0, 0, 0, 0, 0,  //144..151
  };
*/
  //  perl misc/itob.pl xeij/YM2151.java OPM_EG_INCREMENTAL_TABLE
  public static final byte[] OPM_EG_INCREMENTAL_TABLE = "\0\1\0\1\0\1\0\1\0\1\0\1\1\1\0\1\0\1\1\1\0\1\1\1\0\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\2\1\1\1\2\1\2\1\2\1\2\1\2\1\2\2\2\1\2\2\2\2\2\2\2\2\2\2\2\2\2\2\4\2\2\2\4\2\4\2\4\2\4\2\4\2\4\4\4\2\4\4\4\4\4\4\4\4\4\4\4\4\4\4\b\4\4\4\b\4\b\4\b\4\b\4\b\4\b\b\b\4\b\b\b\b\b\b\b\b\b\b\b\20\20\20\20\20\20\20\20\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);
  public static final int[] OPM_EG_TABLE_X = {
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x01010101, 0x01011101, 0x01110111, 0x01111111, 0x01010101, 0x01011101, 0x01110111, 0x01111111,
    0x11111111, 0x11121112, 0x12121212, 0x12221222, 0x22222222, 0x22242224, 0x24242424, 0x24442444,
    0x44444444, 0x44484448, 0x48484848, 0x48884888, 0x88888888, 0x88888888, 0x88888888, 0x88888888,
    0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888,
    0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888,
    0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888,
    0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888,
  };
/*
  public static final int[] OPM_DT1_BASE_TABLE = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 8, 8,
    1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 16, 16, 16, 16,
    2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 20, 22, 22, 22, 22,
  };
*/
  //  perl misc/itob.pl xeij/YM2151.java OPM_DT1_BASE_TABLE
  public static final byte[] OPM_DT1_BASE_TABLE = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\1\1\1\1\1\1\1\1\2\2\2\2\2\3\3\3\4\4\4\5\5\6\6\7\b\b\b\b\1\1\1\1\2\2\2\2\2\3\3\3\4\4\4\5\5\6\6\7\b\b\t\n\13\f\r\16\20\20\20\20\2\2\2\2\2\3\3\3\4\4\4\5\5\6\6\7\b\b\t\n\13\f\r\16\20\21\23\24\26\26\26\26".getBytes (XEiJ.ISO_8859_1);

  public static int opmEGCounter;  //bit0は常に1。4ずつ増やす
  public static int opmEGTimer;

  //LFO
/*
  public static final int[] OPM_LFO_NOISE_BASE = {
    0xff, 0xee, 0xd3, 0x80, 0x58, 0xda, 0x7f, 0x94, 0x9e, 0xe3, 0xfa, 0x00, 0x4d, 0xfa, 0xff, 0x6a,
    0x7a, 0xde, 0x49, 0xf6, 0x00, 0x33, 0xbb, 0x63, 0x91, 0x60, 0x51, 0xff, 0x00, 0xd8, 0x7f, 0xde,
    0xdc, 0x73, 0x21, 0x85, 0xb2, 0x9c, 0x5d, 0x24, 0xcd, 0x91, 0x9e, 0x76, 0x7f, 0x20, 0xfb, 0xf3,
    0x00, 0xa6, 0x3e, 0x42, 0x27, 0x69, 0xae, 0x33, 0x45, 0x44, 0x11, 0x41, 0x72, 0x73, 0xdf, 0xa2,
    0x32, 0xbd, 0x7e, 0xa8, 0x13, 0xeb, 0xd3, 0x15, 0xdd, 0xfb, 0xc9, 0x9d, 0x61, 0x2f, 0xbe, 0x9d,
    0x23, 0x65, 0x51, 0x6a, 0x84, 0xf9, 0xc9, 0xd7, 0x23, 0xbf, 0x65, 0x19, 0xdc, 0x03, 0xf3, 0x24,
    0x33, 0xb6, 0x1e, 0x57, 0x5c, 0xac, 0x25, 0x89, 0x4d, 0xc5, 0x9c, 0x99, 0x15, 0x07, 0xcf, 0xba,
    0xc5, 0x9b, 0x15, 0x4d, 0x8d, 0x2a, 0x1e, 0x1f, 0xea, 0x2b, 0x2f, 0x64, 0xa9, 0x50, 0x3d, 0xab,
    0x50, 0x77, 0xe9, 0xc0, 0xac, 0x6d, 0x3f, 0xca, 0xcf, 0x71, 0x7d, 0x80, 0xa6, 0xfd, 0xff, 0xb5,
    0xbd, 0x6f, 0x24, 0x7b, 0x00, 0x99, 0x5d, 0xb1, 0x48, 0xb0, 0x28, 0x7f, 0x80, 0xec, 0xbf, 0x6f,
    0x6e, 0x39, 0x90, 0x42, 0xd9, 0x4e, 0x2e, 0x12, 0x66, 0xc8, 0xcf, 0x3b, 0x3f, 0x10, 0x7d, 0x79,
    0x00, 0xd3, 0x1f, 0x21, 0x93, 0x34, 0xd7, 0x19, 0x22, 0xa2, 0x08, 0x20, 0xb9, 0xb9, 0xef, 0x51,
    0x99, 0xde, 0xbf, 0xd4, 0x09, 0x75, 0xe9, 0x8a, 0xee, 0xfd, 0xe4, 0x4e, 0x30, 0x17, 0xdf, 0xce,
    0x11, 0xb2, 0x28, 0x35, 0xc2, 0x7c, 0x64, 0xeb, 0x91, 0x5f, 0x32, 0x0c, 0x6e, 0x00, 0xf9, 0x92,
    0x19, 0xdb, 0x8f, 0xab, 0xae, 0xd6, 0x12, 0xc4, 0x26, 0x62, 0xce, 0xcc, 0x0a, 0x03, 0xe7, 0xdd,
    0xe2, 0x4d, 0x8a, 0xa6, 0x46, 0x95, 0x0f, 0x8f, 0xf5, 0x15, 0x97, 0x32, 0xd4, 0x28, 0x1e, 0x55,
  };
*/
  //  perl misc/itoc.pl xeij/YM2151.java OPM_LFO_NOISE_BASE
  public static final char[] OPM_LFO_NOISE_BASE = "\377\356\323\200X\332\177\224\236\343\372\0M\372\377jz\336I\366\0003\273c\221`Q\377\0\330\177\336\334s!\205\262\234]$\315\221\236v\177 \373\363\0\246>B\'i\2563ED\21Ars\337\2422\275~\250\23\353\323\25\335\373\311\235a/\276\235#eQj\204\371\311\327#\277e\31\334\3\363$3\266\36W\\\254%\211M\305\234\231\25\7\317\272\305\233\25M\215*\36\37\352+/d\251P=\253Pw\351\300\254m?\312\317q}\200\246\375\377\265\275o${\0\231]\261H\260(\177\200\354\277on9\220B\331N.\22f\310\317;?\20}y\0\323\37!\2234\327\31\"\242\b \271\271\357Q\231\336\277\324\tu\351\212\356\375\344N0\27\337\316\21\262(5\302|d\353\221_2\fn\0\371\222\31\333\217\253\256\326\22\304&b\316\314\n\3\347\335\342M\212\246F\225\17\217\365\25\2272\324(\36U".toCharArray ();
  //
  //  振幅変調(AM)は符号なし、周波数変調(PM)は符号あり
  //
  //          W=0 SAW ノコギリ波     W=1 SQUARE 方形波    W=2 TRIANGLE 三角波    W=3 NOISE ノイズ
  //
  //      255 ┌＿            ┌    ┌───┐      ┌    Ｘ              Ｘ    ─────────
  //          │  ─＿        │    │      │      │      ＼          ／
  //  AM      │      ─＿    │    │      │      │        ＼      ／
  //          │          ─＿│    │      │      │          ＼  ／
  //        0 ┘…………………┘    ┘………└───┘    …………Ｘ…………    ─────────
  //
  //      128       ＿┐            ┌───┐      ┌        Ｘ                ─────────
  //            ＿─  │            │      │      │      ／  ＼
  //  PM    0 ─………│………─    │………│………│    ／………＼………／    ………………………
  //                  │  ─￣      │      │      │              ＼  ／
  //     -128         └￣          ┘      └───┘                Ｘ        ─────────
  //          0              255    0              255    0              255    0              255
  //
  public static final int[] opmLFOAMTable = new int[256 * 4];
  public static final int[] opmLFOPMTable = new int[256 * 4];

  public static int opmLFOCounterMinor;
  public static int opmLFOPeriodMinor;
  public static int opmLFOCounterMajor;
  public static int opmLFOPeriodMajor;
  public static int opmLFOWaveIndex;
  public static int opmWAVE256;
  public static int opmLFOAMValue;
  public static int opmLFOPMValue;
  public static int opmAMD;
  public static int opmPMD;
  public static int opmLFOAMOutput;
  public static int opmLFOPMOutput;
  public static boolean opmLFOActive;

  //NOISE
  public static int opmNoiseRegister;
  public static int opmNoisePhase;
  public static int opmNoiseFrequency;

  //CSM
  public static int opmCSMRequest;  //0=何もしない,1=次のデータの直後でキーONする,-1=次のデータの直後でキーOFFする

  //テーブル
  //private static final int[] opmTLTable = new int[OPM_TL_TABLE_SIZE];
  public static final int[] opmTLTable = new int[
    OPM_EXTRA_TL_TABLE ?
    Math.max (OPM_TL_TABLE_SIZE, OPM_ENV_QUIET * 8 + (int) (1.0 - Math.log (Math.sin (Math.PI / OPM_SIN_TABLE_SIZE)) / Math.log (2.0) * (OPM_TL_SIZE >> 1))) :
    OPM_TL_TABLE_SIZE
    ];
  public static final int[] opmSinTable = new int[OPM_SIN_TABLE_SIZE];
  public static final int[] opmFreqTable = new int[768 * 11];
  public static final int[] opmDT1FreqTable = new int[256];
  public static final int[] opmNoiseTable = new int[32];

  //チャンネルとスロット
  public static final OPMChannel[] opmChannel = new OPMChannel[8];
  public static OPMChannel opmChannel0;
  public static OPMChannel opmChannel1;
  public static OPMChannel opmChannel2;
  public static OPMChannel opmChannel3;
  public static OPMChannel opmChannel4;
  public static OPMChannel opmChannel5;
  public static OPMChannel opmChannel6;
  public static OPMChannel opmChannel7;
  public static final OPMChannel.Slot[] opmSlot = new OPMChannel.Slot[32];
  public static final class I_int {
    public int value;
  }
  public static I_int opmOutput0;
  public static I_int opmOutput1;
  public static I_int opmOutput2;
  public static I_int opmOutput3;
  public static I_int opmOutput4;
  public static I_int opmOutput5;
  public static I_int opmOutput6;
  public static I_int opmOutput7;
  public static final I_int opmJoint1 = new I_int ();
  public static final I_int opmJoint2 = new I_int ();
  public static final I_int opmJoint3 = new I_int ();
  public static final I_int opmJoint4 = new I_int ();

  //opmInit ()
  //  OPMを初期化する
  public static void opmInit () {

    //opmOutputMask = -1;  //OPMを出力する

    //LFO
    for (int i = 0; i < 256; i++) {
      //W=0 SAW ノコギリ波
      opmLFOAMTable[i] = 255 - i;
      opmLFOPMTable[i] = i < 128 ? i : i - 255;
      //W=1 SQUARE 方形波
      opmLFOAMTable[256 + i] = i < 128 ? 255 : 0;
      opmLFOPMTable[256 + i] = i < 128 ? 128 : -128;
      //W=2 TRIANGLE 三角波
      opmLFOAMTable[512 + i] = i < 128 ? 255 - i * 2 : i * 2 - 256;
      opmLFOPMTable[512 + i] = i < 64 ? i * 2 : i < 128 ? 255 - i * 2 : i < 192 ? 256 - i * 2 : i * 2 - 511;
      //W=3 NOISE ノイズ
      opmLFOAMTable[768 + i] = OPM_LFO_NOISE_BASE[i];
      opmLFOPMTable[768 + i] = OPM_LFO_NOISE_BASE[i] - 128;
    }

    //テーブル
/*
    opmTLTable = new int[
      OPM_EXTRA_TL_TABLE ?
      Math.max (OPM_TL_TABLE_SIZE, OPM_ENV_QUIET * 8 + (int) (1.0 - Math.log (Math.sin (Math.PI / OPM_SIN_TABLE_SIZE)) / Math.log (2.0) * (OPM_TL_SIZE >> 1))) :
      OPM_TL_TABLE_SIZE
      ];
*/
    for (int x = 0; x < OPM_TL_BLOCK_SIZE; x += 2) {
      int t = ((int) (Math.pow (2.0, 16.0 + (double) (x + 2) / -(OPM_TL_SIZE >> 1))) + 16 & -32) >> 3;
      opmTLTable[x] = t;
      opmTLTable[x + 1] = -t;
      for (int i = 1; i < 13; i++) {
        opmTLTable[x + OPM_TL_BLOCK_SIZE * i + 1] = -(opmTLTable[x + OPM_TL_BLOCK_SIZE * i] = t >> i);
      }
    }

    //opmSinTable = new int[OPM_SIN_TABLE_SIZE];
    double ln2 = Math.log (2.0);
    for (int i = 0; i < OPM_SIN_TABLE_SIZE; i++) {
      opmSinTable[i] = ((int) ((-OPM_TL_SIZE >> 1) / ln2 *
                               Math.log (Math.abs (Math.sin (Math.PI / OPM_SIN_TABLE_SIZE * (i * 2 + 1))))
                               ) + 1 & ~1) +
        (i < OPM_SIN_TABLE_SIZE >> 1 ? 0 : 1);  //0..4275。最大値は0,OPM_SIN_TABLE_SIZE/2,OPM_SIN_TABLE_SIZE-1の3箇所
    }

    //opmFreqTable = new int[768 * 11];
    for (int i = 0; i < 768; i++) {
      int t = OPM_PG_BASE_TABLE[i];
      opmFreqTable[768 * 1 + i] = t >> 2 <<     OPM_PHASE_SHIFT - 10;  //下位2ビットを削ってから左にシフトする
      opmFreqTable[768 * 2 + i] = t >> 1 <<     OPM_PHASE_SHIFT - 10;  //下位1ビットを削ってから左にシフトする
      opmFreqTable[768 * 3 + i] = t      <<     OPM_PHASE_SHIFT - 10;
      opmFreqTable[768 * 4 + i] = t      << 1 + OPM_PHASE_SHIFT - 10;
      opmFreqTable[768 * 5 + i] = t      << 2 + OPM_PHASE_SHIFT - 10;
      opmFreqTable[768 * 6 + i] = t      << 3 + OPM_PHASE_SHIFT - 10;
      opmFreqTable[768 * 7 + i] = t      << 4 + OPM_PHASE_SHIFT - 10;
      opmFreqTable[768 * 8 + i] = t      << 5 + OPM_PHASE_SHIFT - 10;
    }
    {
      int t = opmFreqTable[768 * 1 + 0];
      for (int i = 768 * 0; i < 768 * 1; i++) {
        opmFreqTable[i] = t;
      }
      t = opmFreqTable[768 * 9 - 1];
      for (int i = 768 * 9; i < 768 * 11; i++) {
        opmFreqTable[i] = t;
      }
    }

    //opmDT1FreqTable = new int[256];
    for (int i = 0; i < 128; i++) {
      int t = OPM_DT1_BASE_TABLE[i] << OPM_SIN_BITS + OPM_PHASE_SHIFT - 20;
      opmDT1FreqTable[i] = t;
      opmDT1FreqTable[128 + i] = -t;
    }

    //opmNoiseTable = new int[32];
    for (int i = 0; i < 32; i++) {
      opmNoiseTable[i] = 2048 / (32 - i + (i + 1 >> 5)) << 6;
    }

    //チャンネルとスロット
    //opmJoint1 = new I_int ();
    //opmJoint2 = new I_int ();
    //opmJoint3 = new I_int ();
    //opmJoint4 = new I_int ();
    //opmChannel = new OPMChannel[8];
    //opmSlot = new OPMChannel.Slot[32];
    for (int ch = 0; ch < 8; ch++) {
      OPMChannel channel = new OPMChannel ();
      opmChannel[ch] = channel;
      //  スロットの通し番号の順序は小さい方からM1,M2,C1,C2であり、OPMドライバの音色データの順序M1,C1,M2,C2と異なる
      //  OPMドライバは音色データの順序を入れ替えてレジスタに設定している
      opmSlot[ch     ] = channel.m1;
      opmSlot[ch +  8] = channel.m2;
      opmSlot[ch + 16] = channel.c1;
      opmSlot[ch + 24] = channel.c2;
    }
    opmChannel0 = opmChannel[0];
    opmChannel1 = opmChannel[1];
    opmChannel2 = opmChannel[2];
    opmChannel3 = opmChannel[3];
    opmChannel4 = opmChannel[4];
    opmChannel5 = opmChannel[5];
    opmChannel6 = opmChannel[6];
    opmChannel7 = opmChannel[7];
    opmOutput0 = opmChannel0.chOutput;
    opmOutput1 = opmChannel1.chOutput;
    opmOutput2 = opmChannel2.chOutput;
    opmOutput3 = opmChannel3.chOutput;
    opmOutput4 = opmChannel4.chOutput;
    opmOutput5 = opmChannel5.chOutput;
    opmOutput6 = opmChannel6.chOutput;
    opmOutput7 = opmChannel7.chOutput;

    //バッファ
    //opmBuffer = new int[SoundSource.SND_CHANNELS * OPM_BLOCK_SAMPLES];

    //リセット
    opmReset ();
  }  //opmInit

  //リセット
  public static void opmReset () {
    //タイマ
    opmClockA = XEiJ.FAR_FUTURE;
    opmClockB = XEiJ.FAR_FUTURE;
    TickerQueue.tkqRemove (SoundSource.sndOpmATicker);
    TickerQueue.tkqRemove (SoundSource.sndOpmBTicker);
    opmIntervalA = XEiJ.TMR_FREQ / OPM_OSC_FREQ * 64 * (1024 - 0);
    opmIntervalB = XEiJ.TMR_FREQ / OPM_OSC_FREQ * 1024 * (256 - 0);
    opmBusyClock = 0L;
    opmISTA = 0;
    opmISTB = 0;
    //バッファ
    //Arrays.fill (opmBuffer, 0);
    opmPointer = 0;
    //レジスタ
    opmAddress = 0;
    //チャンネル
    for (OPMChannel channel : opmChannel) {
      channel.chReset ();
    }
    opmJoint1.value = opmJoint2.value = opmJoint3.value = opmJoint4.value = 0;
    opmEGTimer = 3;  //ダウンカウントする。3回に1回、1→0のときEGを更新する
    opmEGCounter = 1;  //bit0は常に1
    opmLFOCounterMinor = opmLFOPeriodMinor = 1 << 18;
    opmLFOCounterMajor = 0;
    opmLFOPeriodMajor = 16;
    opmLFOWaveIndex = 0;
    opmWAVE256 = 0;
    opmLFOAMValue = opmLFOAMTable[0];
    opmLFOPMValue = opmLFOPMTable[0];
    opmPMD = 0;
    opmAMD = 0;
    opmLFOAMOutput = 0;
    opmLFOPMOutput = 0;
    opmLFOActive = true;
    opmNoiseRegister = 0;
    opmNoisePhase = 0;
    opmNoiseFrequency = opmNoiseTable[0];
    opmCSMRequest = 0;
    opmSetData (0x1b, 0);
    opmSetData (0x18, 0);
    for (int i = 0x20; i < 0x100; i++) {
      opmSetData (i, 0);
    }

  }  //opmReset

  //データポートに書き込む
  public static void opmSetData (int a, int d) {
    a &= 0xff;
    d &= 0xff;
    if (a < 0x20) {
      switch (a) {
      case 0x01:
        opmLFOActive = (d & 2) == 0;
        if (!opmLFOActive) {
          opmLFOWaveIndex = 0;
          opmLFOAMValue = opmLFOAMTable[opmWAVE256];
          opmLFOPMValue = opmLFOPMTable[opmWAVE256];
        }
        break;
      case 0x08:
        opmChannel[d & 7].chKeyOnOff (d >> 3);
        break;
      case 0x0f:
        opmChannel[7].chNoiseOn = (byte) d < 0;
        opmNoiseFrequency = opmNoiseTable[d & 31];
        break;
      case 0x10:  //CLKA1
        opmCLKA = d << 2 | opmCLKA & 0x03;
        opmIntervalA = XEiJ.TMR_FREQ / OPM_OSC_FREQ * 64 * (1024 - opmCLKA);
        if (opmClockA != XEiJ.FAR_FUTURE) {  //タイマA動作中
          opmClockA = XEiJ.mpuClockTime + opmIntervalA;
          TickerQueue.tkqAdd (SoundSource.sndOpmATicker, opmClockA);
        }
        break;
      case 0x11:  //CLKA2
        opmCLKA = opmCLKA & 0x3fc | d & 0x03;
        opmIntervalA = XEiJ.TMR_FREQ / OPM_OSC_FREQ * 64 * (1024 - opmCLKA);
        if (opmClockA != XEiJ.FAR_FUTURE) {  //タイマA動作中
          opmClockA = XEiJ.mpuClockTime + opmIntervalA;
          TickerQueue.tkqAdd (SoundSource.sndOpmATicker, opmClockA);
        }
        break;
      case 0x12:  //CLKB
        opmCLKB = d;
        opmIntervalB = XEiJ.TMR_FREQ / OPM_OSC_FREQ * 1024 * (256 - opmCLKB);
        if (opmClockB != XEiJ.FAR_FUTURE) {  //タイマB動作中
          opmClockB = XEiJ.mpuClockTime + opmIntervalB;
          TickerQueue.tkqAdd (SoundSource.sndOpmBTicker, opmClockB);
        }
        break;
      case 0x14:  //CSM,F RESET,IRQEN,LOAD
        if ((d & OPM_LOADA) != 0) {  //タイマA始動
          if (opmClockA == XEiJ.FAR_FUTURE) {  //LOADA 0→1
            if ((opmISTA | opmISTB) != 0) {
              MC68901.mfpOpmirqFall ();
            } else {
              MC68901.mfpOpmirqRise ();
            }
            opmClockA = XEiJ.mpuClockTime + opmIntervalA;
            TickerQueue.tkqAdd (SoundSource.sndOpmATicker, opmClockA);
          }
        } else {  //タイマA停止
          if (opmClockA != XEiJ.FAR_FUTURE) {  //LOADA 1→0
            if ((opmISTA | opmISTB) != 0) {
              MC68901.mfpOpmirqFall ();
            } else {
              MC68901.mfpOpmirqRise ();
            }
            opmClockA = XEiJ.FAR_FUTURE;
            TickerQueue.tkqRemove (SoundSource.sndOpmATicker);
          }
        }
        if ((d & OPM_LOADB) != 0) {  //タイマB始動
          if (opmClockB == XEiJ.FAR_FUTURE) {  //LOADB 0→1
            if ((opmISTA | opmISTB) != 0) {
              MC68901.mfpOpmirqFall ();
            } else {
              MC68901.mfpOpmirqRise ();
            }
            opmClockB = XEiJ.mpuClockTime + opmIntervalB;
            TickerQueue.tkqAdd (SoundSource.sndOpmBTicker, opmClockB);
          }
        } else {  //タイマB停止
          if (opmClockB != XEiJ.FAR_FUTURE) {  //LOADB 1→0
            if ((opmISTA | opmISTB) != 0) {
              MC68901.mfpOpmirqFall ();
            } else {
              MC68901.mfpOpmirqRise ();
            }
            opmClockB = XEiJ.FAR_FUTURE;
            TickerQueue.tkqRemove (SoundSource.sndOpmBTicker);
          }
        }
        opmIRQENA = (d & OPM_IRQENA) != 0;
        opmIRQENB = (d & OPM_IRQENB) != 0;
        if ((d & OPM_RESETA) != 0) {  //タイマAフラグレジスタリセット
          opmISTA = 0;
          if (opmISTB != 0) {
            MC68901.mfpOpmirqFall ();
          } else {
            MC68901.mfpOpmirqRise ();
          }
        }
        if ((d & OPM_RESETB) != 0) {  //タイマBフラグレジスタリセット
          opmISTB = 0;
          if (opmISTA != 0) {
            MC68901.mfpOpmirqFall ();
          } else {
            MC68901.mfpOpmirqRise ();
          }
        }
        opmCSMOn = (byte) d < 0;
        break;
      case 0x18:
        opmLFOPeriodMajor = 16 + (d & 15);
        opmLFOCounterMinor = opmLFOPeriodMinor = 1 << 18 - (d >> 4);
        break;
      case 0x19:
        if ((byte) d >= 0) {
          opmAMD = d & 127;
        } else {
          opmPMD = d & 127;
        }
        break;
      case 0x1b:
        opmWAVE256 = (d & 3) << 8;
        opmLFOAMValue = opmLFOAMTable[opmWAVE256 + opmLFOWaveIndex];
        opmLFOPMValue = opmLFOPMTable[opmWAVE256 + opmLFOWaveIndex];
        FDC.fdcSetEnforcedReady (d << 31 - 6 < 0);  //CT2 強制レディ状態の設定。0=通常動作,1=強制レディ状態
        ADPCM.pcmOscillator = d >> 7 & 1;  //CT1 ADPCMの原発振周波数の設定。0=8MHz/8MHz,1=4MHz/16MHz
        ADPCM.pcmUpdateRepeatInterval ();
        break;
      }
    } else if (a < 0x40) {
      OPMChannel channel = opmChannel[a & 7];
      switch (a >> 3) {
      case 0x20 >> 3:
        channel.chSetFLCON (d);
        channel.chSetPAN (d >> 6);
        break;
      case 0x28 >> 3:
        channel.chSetKC (d);
        break;
      case 0x30 >> 3:
        channel.chSetKF (d >> 2);
        break;
      case 0x38 >> 3:
        channel.chSetAMS (d);
        channel.chSetPMS (d >> 4);
        break;
      }
    } else {
      OPMChannel.Slot slot = opmSlot[a & 31];
      switch (a >> 5) {
      case 0x40 >> 5:
        slot.setMUL (d);
        slot.setDT1 (d >> 4);
        break;
      case 0x60 >> 5:
        slot.setTL (d);
        break;
      case 0x80 >> 5:
        slot.setAR (d);
        slot.setKS (d >> 6);
        break;
      case 0xa0 >> 5:
        slot.setD1R (d);
        slot.chSetAMSEN (d >> 7);
        break;
      case 0xc0 >> 5:
        slot.setD2R (d);
        slot.setDT2 (d >> 6);
        break;
      case 0xe0 >> 5:
        slot.setRR (d);
        slot.setD1L (d >> 4);
        break;
      }
    }
  }  //opmSetData

  //opmUpdate (endPointer)
  //  opmPointerをendPointerまで進める
  //  レジスタを更新する直前とライン出力に転送する直前とCSMでキーONする直前に呼び出す
  //  音声出力がOFFのときも、いつでもONにできるように、EG、PG、フィードバック回路などは常に動作している
  //  飽和処理はミキサで行うのでここでは行わない
  //  そのためshortに収まらない値が出力されることがある
  public static void opmUpdate (int endPointer) {
    if (Profiling.PFF_ON) {
      Profiling.pffStart[Profiling.PRF.opmUpdate.ordinal ()] = System.nanoTime ();
    }
    int p = opmPointer;
    int q = endPointer;
    while (p < q) {
      //EG
      if (--opmEGTimer == 0) {
        opmEGTimer = 3;
        opmEGCounter += 4;
        if (OPM_TRANSITION_MASK) {
          if (false) {
            for (OPMChannel channel : opmChannel) {
              if ((opmEGCounter & channel.m1.slTransitionMask) == 0) {
                channel.m1.slStage.transition (channel.m1);
              }
              if ((opmEGCounter & channel.m2.slTransitionMask) == 0) {
                channel.m2.slStage.transition (channel.m2);
              }
              if ((opmEGCounter & channel.c1.slTransitionMask) == 0) {
                channel.c1.slStage.transition (channel.c1);
              }
              if ((opmEGCounter & channel.c2.slTransitionMask) == 0) {
                channel.c2.slStage.transition (channel.c2);
              }
            }
          } else if (false) {
            for (OPMChannel.Slot slot : opmSlot) {
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
          } else if (false) {
            {
              final OPMChannel.Slot slot = opmSlot[0];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[1];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[2];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[3];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[4];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[5];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[6];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[7];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[8];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[9];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[10];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[11];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[12];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[13];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[14];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[15];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[16];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[17];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[18];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[19];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[20];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[21];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[22];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[23];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[24];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[25];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[26];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[27];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[28];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[29];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[30];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel.Slot slot = opmSlot[31];
              if ((opmEGCounter & slot.slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
          } else if (false) {
            for (OPMChannel channel : opmChannel) {
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
          } else if (true) {
            {
              final OPMChannel channel = opmChannel0;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel1;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel2;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel3;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel4;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel5;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel6;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
            {
              final OPMChannel channel = opmChannel7;
              OPMChannel.Slot slot;
              if ((opmEGCounter & (slot = channel.m1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.m2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c1).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
              if ((opmEGCounter & (slot = channel.c2).slTransitionMask) == 0) {
                slot.slStage.transition (slot);
              }
            }
          } else {
            if ((opmEGCounter & opmChannel0.m1.slTransitionMask) == 0) {
              opmChannel0.m1.slStage.transition (opmChannel0.m1);
            }
            if ((opmEGCounter & opmChannel0.m2.slTransitionMask) == 0) {
              opmChannel0.m2.slStage.transition (opmChannel0.m2);
            }
            if ((opmEGCounter & opmChannel0.c1.slTransitionMask) == 0) {
              opmChannel0.c1.slStage.transition (opmChannel0.c1);
            }
            if ((opmEGCounter & opmChannel0.c2.slTransitionMask) == 0) {
              opmChannel0.c2.slStage.transition (opmChannel0.c2);
            }
            if ((opmEGCounter & opmChannel1.m1.slTransitionMask) == 0) {
              opmChannel1.m1.slStage.transition (opmChannel1.m1);
            }
            if ((opmEGCounter & opmChannel1.m2.slTransitionMask) == 0) {
              opmChannel1.m2.slStage.transition (opmChannel1.m2);
            }
            if ((opmEGCounter & opmChannel1.c1.slTransitionMask) == 0) {
              opmChannel1.c1.slStage.transition (opmChannel1.c1);
            }
            if ((opmEGCounter & opmChannel1.c2.slTransitionMask) == 0) {
              opmChannel1.c2.slStage.transition (opmChannel1.c2);
            }
            if ((opmEGCounter & opmChannel2.m1.slTransitionMask) == 0) {
              opmChannel2.m1.slStage.transition (opmChannel2.m1);
            }
            if ((opmEGCounter & opmChannel2.m2.slTransitionMask) == 0) {
              opmChannel2.m2.slStage.transition (opmChannel2.m2);
            }
            if ((opmEGCounter & opmChannel2.c1.slTransitionMask) == 0) {
              opmChannel2.c1.slStage.transition (opmChannel2.c1);
            }
            if ((opmEGCounter & opmChannel2.c2.slTransitionMask) == 0) {
              opmChannel2.c2.slStage.transition (opmChannel2.c2);
            }
            if ((opmEGCounter & opmChannel3.m1.slTransitionMask) == 0) {
              opmChannel3.m1.slStage.transition (opmChannel3.m1);
            }
            if ((opmEGCounter & opmChannel3.m2.slTransitionMask) == 0) {
              opmChannel3.m2.slStage.transition (opmChannel3.m2);
            }
            if ((opmEGCounter & opmChannel3.c1.slTransitionMask) == 0) {
              opmChannel3.c1.slStage.transition (opmChannel3.c1);
            }
            if ((opmEGCounter & opmChannel3.c2.slTransitionMask) == 0) {
              opmChannel3.c2.slStage.transition (opmChannel3.c2);
            }
            if ((opmEGCounter & opmChannel4.m1.slTransitionMask) == 0) {
              opmChannel4.m1.slStage.transition (opmChannel4.m1);
            }
            if ((opmEGCounter & opmChannel4.m2.slTransitionMask) == 0) {
              opmChannel4.m2.slStage.transition (opmChannel4.m2);
            }
            if ((opmEGCounter & opmChannel4.c1.slTransitionMask) == 0) {
              opmChannel4.c1.slStage.transition (opmChannel4.c1);
            }
            if ((opmEGCounter & opmChannel4.c2.slTransitionMask) == 0) {
              opmChannel4.c2.slStage.transition (opmChannel4.c2);
            }
            if ((opmEGCounter & opmChannel5.m1.slTransitionMask) == 0) {
              opmChannel5.m1.slStage.transition (opmChannel5.m1);
            }
            if ((opmEGCounter & opmChannel5.m2.slTransitionMask) == 0) {
              opmChannel5.m2.slStage.transition (opmChannel5.m2);
            }
            if ((opmEGCounter & opmChannel5.c1.slTransitionMask) == 0) {
              opmChannel5.c1.slStage.transition (opmChannel5.c1);
            }
            if ((opmEGCounter & opmChannel5.c2.slTransitionMask) == 0) {
              opmChannel5.c2.slStage.transition (opmChannel5.c2);
            }
            if ((opmEGCounter & opmChannel6.m1.slTransitionMask) == 0) {
              opmChannel6.m1.slStage.transition (opmChannel6.m1);
            }
            if ((opmEGCounter & opmChannel6.m2.slTransitionMask) == 0) {
              opmChannel6.m2.slStage.transition (opmChannel6.m2);
            }
            if ((opmEGCounter & opmChannel6.c1.slTransitionMask) == 0) {
              opmChannel6.c1.slStage.transition (opmChannel6.c1);
            }
            if ((opmEGCounter & opmChannel6.c2.slTransitionMask) == 0) {
              opmChannel6.c2.slStage.transition (opmChannel6.c2);
            }
            if ((opmEGCounter & opmChannel7.m1.slTransitionMask) == 0) {
              opmChannel7.m1.slStage.transition (opmChannel7.m1);
            }
            if ((opmEGCounter & opmChannel7.m2.slTransitionMask) == 0) {
              opmChannel7.m2.slStage.transition (opmChannel7.m2);
            }
            if ((opmEGCounter & opmChannel7.c1.slTransitionMask) == 0) {
              opmChannel7.c1.slStage.transition (opmChannel7.c1);
            }
            if ((opmEGCounter & opmChannel7.c2.slTransitionMask) == 0) {
              opmChannel7.c2.slStage.transition (opmChannel7.c2);
            }
          }
        } else {
          if (false) {
            for (OPMChannel channel : opmChannel) {
              channel.m1.slStage.transition (channel.m1);
              channel.m2.slStage.transition (channel.m2);
              channel.c1.slStage.transition (channel.c1);
              channel.c2.slStage.transition (channel.c2);
            }
          } else if (true) {
            opmChannel0.m1.slStage.transition (opmChannel0.m1);
            opmChannel0.m2.slStage.transition (opmChannel0.m2);
            opmChannel0.c1.slStage.transition (opmChannel0.c1);
            opmChannel0.c2.slStage.transition (opmChannel0.c2);
            opmChannel1.m1.slStage.transition (opmChannel1.m1);
            opmChannel1.m2.slStage.transition (opmChannel1.m2);
            opmChannel1.c1.slStage.transition (opmChannel1.c1);
            opmChannel1.c2.slStage.transition (opmChannel1.c2);
            opmChannel2.m1.slStage.transition (opmChannel2.m1);
            opmChannel2.m2.slStage.transition (opmChannel2.m2);
            opmChannel2.c1.slStage.transition (opmChannel2.c1);
            opmChannel2.c2.slStage.transition (opmChannel2.c2);
            opmChannel3.m1.slStage.transition (opmChannel3.m1);
            opmChannel3.m2.slStage.transition (opmChannel3.m2);
            opmChannel3.c1.slStage.transition (opmChannel3.c1);
            opmChannel3.c2.slStage.transition (opmChannel3.c2);
            opmChannel4.m1.slStage.transition (opmChannel4.m1);
            opmChannel4.m2.slStage.transition (opmChannel4.m2);
            opmChannel4.c1.slStage.transition (opmChannel4.c1);
            opmChannel4.c2.slStage.transition (opmChannel4.c2);
            opmChannel5.m1.slStage.transition (opmChannel5.m1);
            opmChannel5.m2.slStage.transition (opmChannel5.m2);
            opmChannel5.c1.slStage.transition (opmChannel5.c1);
            opmChannel5.c2.slStage.transition (opmChannel5.c2);
            opmChannel6.m1.slStage.transition (opmChannel6.m1);
            opmChannel6.m2.slStage.transition (opmChannel6.m2);
            opmChannel6.c1.slStage.transition (opmChannel6.c1);
            opmChannel6.c2.slStage.transition (opmChannel6.c2);
            opmChannel7.m1.slStage.transition (opmChannel7.m1);
            opmChannel7.m2.slStage.transition (opmChannel7.m2);
            opmChannel7.c1.slStage.transition (opmChannel7.c1);
            opmChannel7.c2.slStage.transition (opmChannel7.c2);
          }
        }
      }
      //音色生成
      if (false) {
        for (OPMChannel channel : opmChannel) {
          channel.chAccumulate ();
        }
      } else {
        opmChannel0.chAccumulate ();
        opmChannel1.chAccumulate ();
        opmChannel2.chAccumulate ();
        opmChannel3.chAccumulate ();
        opmChannel4.chAccumulate ();
        opmChannel5.chAccumulate ();
        opmChannel6.chAccumulate ();
        opmChannel7.chAccumulate ();
      }
      if (SoundSource.sndPlayOn) {
        int l = ((opmOutput0.value & opmChannel0.chLeftMask) +
                 (opmOutput1.value & opmChannel1.chLeftMask) +
                 (opmOutput2.value & opmChannel2.chLeftMask) +
                 (opmOutput3.value & opmChannel3.chLeftMask) +
                 (opmOutput4.value & opmChannel4.chLeftMask) +
                 (opmOutput5.value & opmChannel5.chLeftMask) +
                 (opmOutput6.value & opmChannel6.chLeftMask) +
                 (opmOutput7.value & opmChannel7.chLeftMask));
        int r = ((opmOutput0.value & opmChannel0.chRightMask) +
                 (opmOutput1.value & opmChannel1.chRightMask) +
                 (opmOutput2.value & opmChannel2.chRightMask) +
                 (opmOutput3.value & opmChannel3.chRightMask) +
                 (opmOutput4.value & opmChannel4.chRightMask) +
                 (opmOutput5.value & opmChannel5.chRightMask) +
                 (opmOutput6.value & opmChannel6.chRightMask) +
                 (opmOutput7.value & opmChannel7.chRightMask));
        if (SoundSource.SND_CHANNELS == 1) {
          opmBuffer[p] = l + r >> 1;
        } else {
          opmBuffer[p    ] = l;
          opmBuffer[p + 1] = r;
        }
      } else {
        if (SoundSource.SND_CHANNELS == 1) {
          opmBuffer[p] = 0;
        } else {
          opmBuffer[p    ] = 0;
          opmBuffer[p + 1] = 0;
        }
      }
      p += SoundSource.SND_CHANNELS;
      //LFO
      if (opmLFOActive && --opmLFOCounterMinor == 0) {
        opmLFOCounterMinor = opmLFOPeriodMinor;
        int t = opmLFOCounterMajor + opmLFOPeriodMajor;
        opmLFOCounterMajor = t & 15;
        opmLFOWaveIndex = opmLFOWaveIndex + (t >>> 4) & 255;
        opmLFOAMValue = opmLFOAMTable[opmWAVE256 + opmLFOWaveIndex];
        opmLFOPMValue = opmLFOPMTable[opmWAVE256 + opmLFOWaveIndex];
      }
      opmLFOAMOutput = opmLFOAMValue * opmAMD >>> 7;
      int t = opmLFOPMValue * opmPMD;
      opmLFOPMOutput = t >= 0 ? t >> 7 : -(-t >> 7);  //opmLFOPMValue*opmPMD/128。xがintのときx/128とx>>7は異なることに注意。-64/128は0で-64>>7は-1
      //NOISE
      opmNoisePhase += opmNoiseFrequency;
      if (opmNoisePhase >>> 16 != 0) {  //opmNoisePhase >= 65536
        opmNoiseRegister = (((opmNoiseRegister ^ opmNoiseRegister >>> 3) & 1) ^ 1) << 16 | opmNoiseRegister >>> 1;
        opmNoisePhase = (char) opmNoisePhase;  //opmNoisePhase &= 65535;
      }
      //PG
      if (false) {
        for (OPMChannel channel : opmChannel) {
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
      } else {
        {
          final OPMChannel channel = opmChannel0;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel1;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel2;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel3;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel4;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel5;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel6;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
        {
          final OPMChannel channel = opmChannel7;
          OPMChannel.Slot m1 = channel.m1;
          OPMChannel.Slot m2 = channel.m2;
          OPMChannel.Slot c1 = channel.c1;
          OPMChannel.Slot c2 = channel.c2;
          int pmDepth = opmLFOPMOutput * channel.chShiftPMS >> 6;
          if (pmDepth != 0) {
            int keyIndex = channel.chKeyIndex + pmDepth;
            m1.slPhase += (opmFreqTable[keyIndex + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply >>> 1;
            m2.slPhase += (opmFreqTable[keyIndex + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply >>> 1;
            c1.slPhase += (opmFreqTable[keyIndex + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply >>> 1;
            c2.slPhase += (opmFreqTable[keyIndex + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply >>> 1;
          } else {
            m1.slPhase += m1.slFreq;
            m2.slPhase += m2.slFreq;
            c1.slPhase += c1.slFreq;
            c2.slPhase += c2.slFreq;
          }
        }
      }
      //CSM
      if (opmCSMRequest != 0) {
        if (opmCSMRequest > 0) {
          if (false) {
            for (OPMChannel channel : opmChannel) {
              channel.m1.keyOn (2);
              channel.m2.keyOn (2);
              channel.c1.keyOn (2);
              channel.c2.keyOn (2);
            }
          } else {
            opmChannel0.m1.keyOn (2);
            opmChannel0.m2.keyOn (2);
            opmChannel0.c1.keyOn (2);
            opmChannel0.c2.keyOn (2);
            opmChannel1.m1.keyOn (2);
            opmChannel1.m2.keyOn (2);
            opmChannel1.c1.keyOn (2);
            opmChannel1.c2.keyOn (2);
            opmChannel2.m1.keyOn (2);
            opmChannel2.m2.keyOn (2);
            opmChannel2.c1.keyOn (2);
            opmChannel2.c2.keyOn (2);
            opmChannel3.m1.keyOn (2);
            opmChannel3.m2.keyOn (2);
            opmChannel3.c1.keyOn (2);
            opmChannel3.c2.keyOn (2);
            opmChannel4.m1.keyOn (2);
            opmChannel4.m2.keyOn (2);
            opmChannel4.c1.keyOn (2);
            opmChannel4.c2.keyOn (2);
            opmChannel5.m1.keyOn (2);
            opmChannel5.m2.keyOn (2);
            opmChannel5.c1.keyOn (2);
            opmChannel5.c2.keyOn (2);
            opmChannel6.m1.keyOn (2);
            opmChannel6.m2.keyOn (2);
            opmChannel6.c1.keyOn (2);
            opmChannel6.c2.keyOn (2);
            opmChannel7.m1.keyOn (2);
            opmChannel7.m2.keyOn (2);
            opmChannel7.c1.keyOn (2);
            opmChannel7.c2.keyOn (2);
          }
          opmCSMRequest = -1;
        } else {
          if (false) {
            for (OPMChannel channel : opmChannel) {
              channel.m1.keyOff (~2);
              channel.m2.keyOff (~2);
              channel.c1.keyOff (~2);
              channel.c2.keyOff (~2);
            }
          } else {
            opmChannel0.m1.keyOff (~2);
            opmChannel0.m2.keyOff (~2);
            opmChannel0.c1.keyOff (~2);
            opmChannel0.c2.keyOff (~2);
            opmChannel1.m1.keyOff (~2);
            opmChannel1.m2.keyOff (~2);
            opmChannel1.c1.keyOff (~2);
            opmChannel1.c2.keyOff (~2);
            opmChannel2.m1.keyOff (~2);
            opmChannel2.m2.keyOff (~2);
            opmChannel2.c1.keyOff (~2);
            opmChannel2.c2.keyOff (~2);
            opmChannel3.m1.keyOff (~2);
            opmChannel3.m2.keyOff (~2);
            opmChannel3.c1.keyOff (~2);
            opmChannel3.c2.keyOff (~2);
            opmChannel4.m1.keyOff (~2);
            opmChannel4.m2.keyOff (~2);
            opmChannel4.c1.keyOff (~2);
            opmChannel4.c2.keyOff (~2);
            opmChannel5.m1.keyOff (~2);
            opmChannel5.m2.keyOff (~2);
            opmChannel5.c1.keyOff (~2);
            opmChannel5.c2.keyOff (~2);
            opmChannel6.m1.keyOff (~2);
            opmChannel6.m2.keyOff (~2);
            opmChannel6.c1.keyOff (~2);
            opmChannel6.c2.keyOff (~2);
            opmChannel7.m1.keyOff (~2);
            opmChannel7.m2.keyOff (~2);
            opmChannel7.c1.keyOff (~2);
            opmChannel7.c2.keyOff (~2);
          }
          opmCSMRequest = 0;
        }
      }
    }  //while p<q
    opmPointer = q;
    if (Profiling.PFF_ON) {
      Profiling.pffTotal[Profiling.PRF.opmUpdate.ordinal ()] += System.nanoTime () - Profiling.pffStart[Profiling.PRF.opmUpdate.ordinal ()];
      Profiling.pffCount[Profiling.PRF.opmUpdate.ordinal ()]++;
    }
  }  //opmUpdate

  public static void opmSetOutputOn (boolean on) {
    opmOutputMask = on ? -1 : 0;
    for (OPMChannel channel : opmChannel) {
      channel.chSetPAN (channel.chPAN);
    }
  }  //opmSetOutputOn(boolean)

  //enum OPMStage
  //  EGのステージ
  public static enum OPMStage {
    //アタック
    ATTACK {
      @Override public void transition (OPMChannel.Slot slot) {
        if (OPM_TRANSITION_MASK || (opmEGCounter & (4 << slot.slAttackShift) - 4) == 0) {
          if (OPM_EG_X) {
            slot.slVolume += ~slot.slVolume * (slot.slAttack3 | slot.slAttack4 << (opmEGCounter >>> slot.slAttackShift & 28) >>> 28) >> 4;
          } else {
            slot.slVolume += (~slot.slVolume * (OPM_EG_INCREMENTAL_TABLE[slot.slAttackSelect + (opmEGCounter >>> slot.slAttackShift + 2 & 7)])) >> 4;
          }
          if (slot.slVolume <= 0) {
            slot.slVolume = 0;
            slot.slStage = OPMStage.DECAY;
            if (OPM_TRANSITION_MASK) {
              slot.slTransitionMask = (4 << slot.slDecayShift) - 4;
            }
          }
        }
      }
    },
    //ファーストディケイ
    DECAY {
      @Override public void transition (OPMChannel.Slot slot) {
        if (OPM_TRANSITION_MASK || (opmEGCounter & (4 << slot.slDecayShift) - 4) == 0) {
          if (OPM_EG_X) {
            slot.slVolume += slot.slDecay3 | slot.slDecay4 << (opmEGCounter >>> slot.slDecayShift & 28) >>> 28;
          } else {
            slot.slVolume += OPM_EG_INCREMENTAL_TABLE[slot.slDecaySelect + (opmEGCounter >>> slot.slDecayShift + 2 & 7)];
          }
          if (slot.slVolume >= slot.slDecayLevel) {
            slot.slStage = OPMStage.SUSTAIN;
            if (OPM_TRANSITION_MASK) {
              slot.slTransitionMask = (4 << slot.slSustainShift) - 4;
            }
          }
        }
      }
    },
    //セカンドディケイ
    SUSTAIN {
      @Override public void transition (OPMChannel.Slot slot) {
        if (OPM_TRANSITION_MASK || (opmEGCounter & (4 << slot.slSustainShift) - 4) == 0) {
          if (OPM_EG_X) {
            slot.slVolume += slot.slSustain3 | slot.slSustain4 << (opmEGCounter >>> slot.slSustainShift & 28) >>> 28;
          } else {
            slot.slVolume += OPM_EG_INCREMENTAL_TABLE[slot.slSustainSelect + (opmEGCounter >>> slot.slSustainShift + 2 & 7)];
          }
          if (slot.slVolume >= OPM_TL_SIZE - 1) {
            slot.slVolume = OPM_TL_SIZE - 1;
            slot.slStage = OPMStage.SILENCE;
            if (OPM_TRANSITION_MASK) {
              slot.slTransitionMask = 1;
            }
          }
        }
      }
    },
    //リリース
    RELEASE {
      @Override public void transition (OPMChannel.Slot slot) {
        if (OPM_TRANSITION_MASK || (opmEGCounter & (4 << slot.slReleaseShift) - 4) == 0) {
          if (OPM_EG_X) {
            slot.slVolume += slot.slRelease3 | slot.slRelease4 << (opmEGCounter >>> slot.slReleaseShift & 28) >>> 28;
          } else {
            slot.slVolume += OPM_EG_INCREMENTAL_TABLE[slot.slReleaseSelect + (opmEGCounter >>> slot.slReleaseShift + 2 & 7)];
          }
          if (slot.slVolume >= OPM_TL_SIZE - 1) {
            slot.slVolume = OPM_TL_SIZE - 1;
            slot.slStage = OPMStage.SILENCE;
            if (OPM_TRANSITION_MASK) {
              slot.slTransitionMask = 1;
            }
          }
        }
      }
    },
    //停止
    SILENCE {
      @Override public void transition (OPMChannel.Slot slot) {
      }
    };
    public abstract void transition (OPMChannel.Slot slot);
  }  //enum OPMStage

  //OPMChannel
  //  OPMのチャンネル
  public static final class OPMChannel {

    //チャンネルのフィールド
    public Slot m1;  //M1
    public Slot c1;  //C1
    public Slot m2;  //M2
    public Slot c2;  //C2
    public int chKeyCode;  //0..127。キーコード
    public int chKeyIndex;  //キーコードとキーフラクションを合わせたインデックス。1オクターブは64*12=768で768の下駄履き
    public I_int chOutput;  //チャンネルの出力
    public I_int fbOutput;  //フィードバック回路の出力。前回の入力
    public int fbInputValue;  //フィードバック回路の前回の入力
    public int fbPreviousValue;  //フィードバック回路の前々回の入力
    public int fbScale;  //フィードバック回路のスケーリング。0=OFF,128=π/16,256=π/8,512=π/4,1024=π/2,2048=π,4096=2π,8192=4π
    public I_int bfInput;  //バッファの入力
    public int bfInputValue;  //バッファの値
    public I_int bfOutput;  //バッファの出力
    public int chLeftMask;  //左側の出力のマスク。0=OFF,-1=ON
    public int chRightMask;  //右側の出力のマスク。0=OFF,-1=ON
    public boolean chNoiseOn;  //true=C2がノイズスロットになる
    public int chShiftAMS;  //振幅変調のスケーリング
    public int chPMS;
    public I_int c1Input;
    public I_int c1Output;
    public I_int m2Input;
    public I_int m2Output;
    public I_int c2Input;
    public int chFLCON;
    public int chSLOT;
    public boolean chSYNC;
    public int chPAN;
    public int chAMS;
    public int chShiftPMS;

    //new OPMChannel ()
    //  チャンネルのコンストラクタ
    public OPMChannel () {
      m1 = new Slot ();
      c1 = new Slot ();
      m2 = new Slot ();
      c2 = new Slot ();
      chOutput = new I_int ();
      c1Input = opmJoint1;
      m2Input = opmJoint3;
      chReset ();
    }

    //channel.chReset ()
    //  チャンネルを初期化する
    public void chReset () {
      chKeyCode = 0;
      chKeyIndex = 768;
      m1.slReset ();
      c1.slReset ();
      m2.slReset ();
      c2.slReset ();
      chOutput.value = 0;
      fbOutput = null;
      fbInputValue = 0;
      fbPreviousValue = 0;
      fbScale = 0;
      bfInput = null;
      bfInputValue = 0;
      bfOutput = null;
      chLeftMask = 0;
      chRightMask = 0;
      chNoiseOn = false;
      chShiftAMS = 0;
      chPMS = 0;
      c1Output = null;
      c2Input = null;
      chFLCON = 0;
      chSLOT = 0;
      chSYNC = true;
      chPAN = 0;
      chAMS = 0;
      chShiftPMS = 0;
    }

    //channel.chAccumulate ()
    //  1回分のチャンネルの出力を作る
    //  出力はchOutputに書き込まれる
    public void chAccumulate () {
      opmJoint1.value = opmJoint2.value = opmJoint3.value = opmJoint4.value = chOutput.value = 0;
      bfOutput.value = bfInputValue;  //前回バッファに入力した値→今回バッファから出力する値
      fbOutput.value = fbInputValue;  //前回フィードバック回路に入力した値→今回フィードバック回路から出力する値
      int amsValue = opmLFOAMOutput * chShiftAMS;  //振幅変調回路の出力
      int env = m1.slTotalLevel + m1.slVolume + (amsValue & m1.slAMSMask);
      int m1InputValue = (fbPreviousValue + fbInputValue) * fbScale;  //今回フィードバックする値。前々回の入力と前回の入力の和をスケーリングしたもの
      fbPreviousValue = fbInputValue;  //前回フィードバック回路に入力した値
      if (OPM_EXTRA_TL_TABLE) {
        fbInputValue = (
          env < OPM_ENV_QUIET ?
          opmTLTable[(env << 3) + opmSinTable[(m1.slPhase & ~OPM_PHASE_MASK) + m1InputValue >> OPM_PHASE_SHIFT & OPM_SIN_MASK]] :
          0
          );
      } else {
        if (env < OPM_ENV_QUIET) {  //0..831
          int p = (env << 3) + opmSinTable[(m1.slPhase & ~OPM_PHASE_MASK) + m1InputValue >> OPM_PHASE_SHIFT & OPM_SIN_MASK];  //(0..831)*8+(0..4275)=0..10923
          fbInputValue = p < OPM_TL_TABLE_SIZE ? opmTLTable[p] : 0;  //今回フィードバック回路に入力する値
        } else {
          fbInputValue = 0;
        }
      }
      env = m2.slTotalLevel + m2.slVolume + (amsValue & m2.slAMSMask);
      if (OPM_EXTRA_TL_TABLE) {

        m2Output.value += (
          env < OPM_ENV_QUIET ?
          opmTLTable[(env << 3) + opmSinTable[(m2.slPhase >> OPM_PHASE_SHIFT) + (m2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]] :
          0
          );
/*
        if (env < OPM_ENV_QUIET) {
          m2Output.value += opmTLTable[(env << 3) + opmSinTable[(m2.slPhase >> OPM_PHASE_SHIFT) + (m2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]];
        }
*/
      } else {
        if (env < OPM_ENV_QUIET) {
          int p = (env << 3) + opmSinTable[(m2.slPhase >> OPM_PHASE_SHIFT) + (m2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK];  //(0..831)*8+(0..4275)=0..10923
          if (p < OPM_TL_TABLE_SIZE) {
            m2Output.value += opmTLTable[p];
          }
        }
      }
      env = c1.slTotalLevel + c1.slVolume + (amsValue & c1.slAMSMask);
      if (OPM_EXTRA_TL_TABLE) {

        c1Output.value += (
          env < OPM_ENV_QUIET ?
          opmTLTable[(env << 3) + opmSinTable[(c1.slPhase >> OPM_PHASE_SHIFT) + (c1Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]] :
          0
          );
/*
        if (env < OPM_ENV_QUIET) {
          c1Output.value += opmTLTable[(env << 3) + opmSinTable[(c1.slPhase >> OPM_PHASE_SHIFT) + (c1Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]];
        }
*/
      } else {
        if (env < OPM_ENV_QUIET) {
          int p = (env << 3) + opmSinTable[(c1.slPhase >> OPM_PHASE_SHIFT) + (c1Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK];  //(0..831)*8+(0..4275)=0..10923
          if (p < OPM_TL_TABLE_SIZE) {
            c1Output.value += opmTLTable[p];
          }
        }
      }
      env = c2.slTotalLevel + c2.slVolume + (amsValue & c2.slAMSMask);
      if (chNoiseOn) {
        chOutput.value += ((opmNoiseRegister >> 15 & 2) - 1) * (env < 0x3ff ? (env ^ 0x3ff) << 1 : 0);
      } else {
        if (OPM_EXTRA_TL_TABLE) {

          chOutput.value += (
            env < OPM_ENV_QUIET ?
            opmTLTable[(env << 3) + opmSinTable[(c2.slPhase >> OPM_PHASE_SHIFT) + (c2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]] :
            0
            );
/*
          if (env < OPM_ENV_QUIET) {
            chOutput.value += opmTLTable[(env << 3) + opmSinTable[(c2.slPhase >> OPM_PHASE_SHIFT) + (c2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK]];
          }
*/
        } else {
          if (env < OPM_ENV_QUIET) {
            int p = (env << 3) + opmSinTable[(c2.slPhase >> OPM_PHASE_SHIFT) + (c2Input.value >> OPM_PHASE_SHIFT - 15) & OPM_SIN_MASK];  //(0..831)*8+(0..4275)=0..10923
            if (p < OPM_TL_TABLE_SIZE) {
              chOutput.value += opmTLTable[p];
            }
          }
        }
      }
      bfInputValue = bfInput.value;  //今回バッファに入力する値
    }

    //channel.chKeyOnOff (mask)
    //  チャンネルの各スロットをキーONまたはキーOFFする
    public void chKeyOnOff (int mask) {
      if (mask != 0) {
        chSLOT = mask;
        chSYNC = opmLFOWaveIndex == 0;
      }
      //  Inside X68000にKONのスロットマスクのビットの順序が上位からC2|C1|M2|M1と書かれているが、C2|M2|C1|M1の誤り
      //  Human68k ver 3.0ユーザーズマニュアルのスロットマスクの説明ではC2|M2|C1|M1となっており、こちらが正しい
      if ((mask & 0b0001) != 0) {
        m1.keyOn (1);
      } else {
        m1.keyOff (~1);
      }
      if ((mask & 0b0010) != 0) {
        c1.keyOn (1);
      } else {
        c1.keyOff (~1);
      }
      if ((mask & 0b0100) != 0) {
        m2.keyOn (1);
      } else {
        m2.keyOff (~1);
      }
      if ((mask & 0b1000) != 0) {
        c2.keyOn (1);
      } else {
        c2.keyOff (~1);
      }
    }

    //channel.chSetFLCON (v)
    //  チャンネルのフィードバックレベルとコネクションを設定する
    public void chSetFLCON (int v) {
      chFLCON = v &= 63;
      switch (v & 7) {
        //  M1,M2        モジュレータ
        //  C1,C2        キャリア
        //  FB           フィードバック
        //  BF           バッファ
        //  ①,②,③,④  ジョイント
        //  ◯           アウトプット
        //  ①→C1と③→M2はすべてのコネクションで共通なのでコンストラクタで接続する
        //  opmJoint1,opmJoint3よりもc1Input,m2Inputの方がアクセスが速いのでc1Input,m2Inputを廃止することはしない
        //  バッファを使うコネクションからバッファを使わないコネクションに切り替えた直後にバッファからゴミが出てくる可能性があるので、
        //  バッファを使わないときはバッファの出力を空いているジョイントに繋いでおく
      case 0:
        //  ┌─┐
        //  ↓  │
        //  M1→FB→①→C1→②→BF→③→M2→④→C2→◯
        fbOutput = opmJoint1;
        c1Output = bfInput = opmJoint2;
        bfOutput = opmJoint3;
        m2Output = c2Input = opmJoint4;
        break;
      case 1:
        //  ┌─┐  ①→C1─┐
        //  ↓  │          ↓
        //  M1→FB────→②→BF→③→M2→④→C2→◯
        fbOutput = c1Output = bfInput = opmJoint2;
        bfOutput = opmJoint3;
        m2Output = c2Input = opmJoint4;
        break;
      case 2:
        //  ┌─┐  ①→C1→②→BF→③→M2─┐
        //  ↓  │                          ↓
        //  M1→FB────────────→④→C2→◯
        c1Output = bfInput = opmJoint2;
        bfOutput = opmJoint3;
        fbOutput = m2Output = c2Input = opmJoint4;
        break;
      case 3:
        //  ┌─┐                  ③→M2─┐
        //  ↓  │                          ↓
        //  M1→FB→①→C1→②→BF────→④→C2→◯
        fbOutput = opmJoint1;
        c1Output = bfInput = opmJoint2;
        bfOutput = m2Output = c2Input = opmJoint4;
        break;
      case 4:
        //  ┌─┐                  ③→M2→④→C2─┐
        //  ↓  │                                  ↓
        //  M1→FB→①→C1────────────→◯
        fbOutput = opmJoint1;
        bfOutput = opmJoint2;  //ゴミが出てくる可能性があるので空いているジョイントに繋いでおく
        m2Output = c2Input = opmJoint4;
        c1Output = chOutput;
        break;
      case 5:
        //  ┌─┐  ┌────→BF→③→M2─────┐
        //  ↓  │  │                              ↓
        //  M1→FB→①→C1────────────→◯
        //          │                              ↑
        //          └────────────→C2─┘
        fbOutput = bfInput = c2Input = opmJoint1;
        bfOutput = opmJoint3;
        c1Output = m2Output = chOutput;
        break;
      case 6:
        //  ┌─┐                  ③→M2─────┐
        //  ↓  │                                  ↓
        //  M1→FB→①→C1────────────→◯
        //                                          ↑
        //                                  ④→C2─┘
        fbOutput = opmJoint1;
        bfOutput = opmJoint2;  //ゴミが出てくる可能性があるので空いているジョイントに繋いでおく
        c2Input = opmJoint4;
        c1Output = m2Output = chOutput;
        break;
      case 7:
        //          ①→C1───────────────┐
        //                                              │
        //  ┌─┐                  ③→M2─────┐  │
        //  ↓  │                                  ↓  │
        //  M1→FB────────────────→◯←┘
        //                                          ↑
        //                                  ④→C2─┘
        bfOutput = opmJoint2;  //ゴミが出てくる可能性があるので空いているジョイントに繋いでおく
        c2Input = opmJoint4;
        fbOutput = c1Output = m2Output = chOutput;
        break;
      }
      v >>= 3;
      fbScale = v == 0 ? 0 : 1 << v + 6;  //(1 << v + 6) & ~(v - 1 >> 3)
    }

    //channel.chSetPAN (v)
    //  チャンネルのパンを設定する
    public void chSetPAN (int v) {
      chPAN = v &= 3;
      chLeftMask = -(v & 1) & opmOutputMask;
      chRightMask = -(v >> 1) & opmOutputMask;
    }

    //channel.chSetKC (v)
    //  チャンネルのキーコードを設定する
    public void chSetKC (int v) {
      v &= 0x7f;
      //  0..127
      if (chKeyCode != v) {
        chKeyCode = v;
        //KCが4の倍数のときKC-1と同じ音が出る
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
        //  0  1  2  3  3  4  5  6  6  7  8  9  9 10 11 12
        chKeyIndex = ((v - (v >> 2)) << 6) + 768 | chKeyIndex & 63;
        m1.refreshEG ();
        m2.refreshEG ();
        c1.refreshEG ();
        c2.refreshEG ();
      }
    }

    //channel.chSetKF (v)
    //  チャンネルのキーフラクションを設定する
    public void chSetKF (int v) {
      v = chKeyIndex & ~63 | v & 63;
      if (chKeyIndex != v) {
        chKeyIndex = v;
        m1.slFreq = ((opmFreqTable[v + m1.slDetune2Depth] + m1.slDetune1Freq) * m1.slMultiply) >>> 1;
        m2.slFreq = ((opmFreqTable[v + m2.slDetune2Depth] + m2.slDetune1Freq) * m2.slMultiply) >>> 1;
        c1.slFreq = ((opmFreqTable[v + c1.slDetune2Depth] + c1.slDetune1Freq) * c1.slMultiply) >>> 1;
        c2.slFreq = ((opmFreqTable[v + c2.slDetune2Depth] + c2.slDetune1Freq) * c2.slMultiply) >>> 1;
      }
    }

    //channel.chSetAMS (v)
    //  チャンネルの振幅変調を設定する
    public void chSetAMS (int v) {
      chAMS = v &= 3;
      chShiftAMS = v == 0 ? 0 : 1 << v - 1;
    }

    //channel.chSetPMS (v)
    //  チャンネルの周波数変調を設定する
    public void chSetPMS (int v) {
      chPMS = v &= 7;
      //  0   1   2   3   4   5   6   7
      //  *0 >>5 >>4 >>3 >>2 >>1 <<1 <<2
      chShiftPMS = v == 0 ? 0 : v < 6 ? 64 >> 6 - v : 64 << v - 5;
    }

    //OPMChannel.Slot
    //  OPMのスロット
    public final class Slot {

      //スロットのフィールド
      public int slPhase;
      public int slFreq;
      public int slDetune1Freq;
      public int slMultiply;  //周波数の倍率の2倍。1,2～30
      public int slDetune1Page;  //0..224
      public int slDetune2Depth;  //大きいデチューン
      public int slAMSMask;  //振幅変調のマスク。0=振幅変調しない,-1=振幅変調する
      public int slTotalLevel;  //トータルレベル
      public int slVolume;
      public int slKeyStatus;  //bit0:1=マニュアルでキーONされている,bit1:1=タイマーでキーONされている
      public int slKeyScale;  //2..5。5-KS。AR,D1R,D2RのRATEはmin(63,2*R+KC>>(5-KS))、RRのRATEはmin(63,2+4*RR+KC>>(5-KS))
      //EG
      public OPMStage slStage;  //EGのステージ
      public int slAttackRate;  //0..94。AR==0?0:32+2*AR。アタックレート
      public int slAttackShift;
      public int slAttack3;
      public int slAttack4;
      public int slAttackSelect;
      public int slDecayRate;  //0..94。D1R==0?0:32+2*D1R。ファーストディケイレート
      public int slDecayShift;
      public int slDecay3;
      public int slDecay4;
      public int slDecaySelect;
      public int slDecayLevel;  //(D1L<15?D1L:31+D1L)<<(OPM_TL_BITS-5)。ファーストディケイレベル
      public int slSustainRate;  //0..94。D2R==0?0:32+2*D2R。セカンドディケイレート
      public int slSustainShift;
      public int slSustain3;
      public int slSustain4;
      public int slSustainSelect;
      public int slReleaseRate;  //34..94。32+2+4*RR。リリースレート
      public int slReleaseShift;
      public int slRelease3;
      public int slRelease4;
      public int slReleaseSelect;
      public int slTransitionMask;  //SILENCEのとき1,その他(4<<slXXXShift)-4。opmEGCounterのbit0は常に1なのでSILENCEのときはマスクしても0にならない

      //new Slot ()
      //  スロットのコンストラクタ
      public Slot () {
        //slReset ();  OPMChannelのコンストラクタがリセットするので省略
      }

      //slot.slReset ()
      //  スロットを初期化する
      public void slReset () {
        slPhase = 0;
        slFreq = 0;
        slDetune1Freq = 0;
        slMultiply = 0;
        slDetune1Page = 0;
        slDetune2Depth = 0;
        slAMSMask = 0;
        slStage = OPMStage.SILENCE;
        if (OPM_TRANSITION_MASK) {
          slTransitionMask = 1;
        }
        slAttackShift = 0;
        if (OPM_EG_X) {
          slAttack3 = 0;
          slAttack4 = 0;
        } else {
          slAttackSelect = 0;
        }
        slTotalLevel = 0;
        slVolume = OPM_TL_SIZE - 1;
        slDecayShift = 0;
        if (OPM_EG_X) {
          slDecay3 = 0;
          slDecay4 = 0;
        } else {
          slDecaySelect = 0;
        }
        slDecayLevel = 0;
        slSustainShift = 0;
        if (OPM_EG_X) {
          slSustain3 = 0;
          slSustain4 = 0;
        } else {
          slSustainSelect = 0;
        }
        slReleaseShift = 0;
        if (OPM_EG_X) {
          slRelease3 = 0;
          slRelease4 = 0;
        } else {
          slReleaseSelect = 0;
        }
        slKeyStatus = 0;
        slKeyScale = 0;
        slAttackRate = 0;
        slDecayRate = 0;
        slSustainRate = 0;
        slReleaseRate = 0;
      }

      //slot.keyOn (mask)
      //  スロットをキーONする
      //  mask  bit0  0=マニュアルでキーONしない,1=マニュアルでキーONする
      //        bit1  0=タイマーでキーONしない,1=タイマーでキーONする
      public void keyOn (int mask) {
        if (slKeyStatus == 0) {  //マニュアルとタイマーのどちらでもキーONされていない
          slPhase = 0;
          slStage = OPMStage.ATTACK;  //アタックステージを開始する
          if (OPM_TRANSITION_MASK) {
            slTransitionMask = (4 << slAttackShift) - 4;
          }
          if (OPM_EG_X) {
            slVolume += ~slVolume * (slAttack3 | slAttack4 << (opmEGCounter >>> slAttackShift & 28) >>> 28) >> 4;
          } else {
            slVolume += ~slVolume * (OPM_EG_INCREMENTAL_TABLE[slAttackSelect + (opmEGCounter >>> slAttackShift + 2 & 7)]) >> 4;
          }
          if (slVolume <= 0) {
            slVolume = 0;
            slStage = OPMStage.DECAY;
            if (OPM_TRANSITION_MASK) {
              slTransitionMask = (4 << slDecayShift) - 4;
            }
          }
        }
        slKeyStatus |= mask;  //マニュアルまたはタイマーでキーONする
      }

      //slot.keyOff (mask)
      //  スロットをキーOFFする
      //  mask  bit0  0=マニュアルでキーOFFする,1=マニュアルでキーOFFしない
      //        bit1  0=タイマーでキーOFFする,1=タイマーでキーOFFしない
      public void keyOff (int mask) {
        if (slKeyStatus != 0) {  //マニュアルまたはタイマーのいずれかでキーONされている
          slKeyStatus &= mask;  //マニュアルまたはタイマーでキーOFFする
          if (slKeyStatus == 0 &&  //他方でキーONされておらず
              slStage.ordinal () < OPMStage.RELEASE.ordinal ()) {  //まだリリースされていない
            slStage = OPMStage.RELEASE;  //リリースステージに移行する
            if (OPM_TRANSITION_MASK) {
              slTransitionMask = (4 << slReleaseShift) - 4;
            }
          }
        }
      }

      //slot.refreshEG ()
      //  スロットのエンベロープジェネレータを再設定する
      public void refreshEG () {
        slDetune1Freq = opmDT1FreqTable[slDetune1Page + (chKeyCode >> 2)];
        slFreq = ((opmFreqTable[chKeyIndex + slDetune2Depth] + slDetune1Freq) * slMultiply) >>> 1;
        int u = chKeyCode >> slKeyScale;  //(0..127)>>(2..5)=0..31
        int v = slAttackRate + u;  //(0..94)+(0..31)=0..125
        if (v < 94) {
          slAttackShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
            slTransitionMask = (4 << slAttackShift) - 4;
          }
          if (OPM_EG_X) {
            slAttack3 = 0;
            slAttack4 = OPM_EG_TABLE_X[v];
          } else {
            slAttackSelect = OPM_EG_PAGE_TABLE_1[v];
          }
        } else {
          slAttackShift = 0;
          if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
            slTransitionMask = 0;
          }
          if (OPM_EG_X) {
            slAttack3 = 16;
            slAttack4 = 0;
          } else {
            slAttackSelect = 17 * 8;
          }
        }
        v = slDecayRate + u;  //(0..94)+(0..31)=0..125
        slDecayShift = OPM_EG_SHIFT_TABLE[v];
        if (OPM_TRANSITION_MASK && slStage == OPMStage.DECAY) {
          slTransitionMask = (4 << slDecayShift) - 4;
        }
        if (OPM_EG_X) {
          slDecay3 = 0;
          slDecay4 = OPM_EG_TABLE_X[v];
        } else {
          slDecaySelect = OPM_EG_PAGE_TABLE_1[v];
        }
        v = slSustainRate + u;  //(0..94)+(0..31)=0..125
        slSustainShift = OPM_EG_SHIFT_TABLE[v];
        if (OPM_TRANSITION_MASK && slStage == OPMStage.SUSTAIN) {
          slTransitionMask = (4 << slSustainShift) - 4;
        }
        if (OPM_EG_X) {
          slSustain3 = 0;
          slSustain4 = OPM_EG_TABLE_X[v];
        } else {
          slSustainSelect = OPM_EG_PAGE_TABLE_1[v];
        }
        v = slReleaseRate + u;  //(34..94)+(0..31)=34..125
        slReleaseShift = OPM_EG_SHIFT_TABLE[v];
        if (OPM_TRANSITION_MASK && slStage == OPMStage.RELEASE) {
          slTransitionMask = (4 << slReleaseShift) - 4;
        }
        if (OPM_EG_X) {
          slRelease3 = 0;
          slRelease4 = OPM_EG_TABLE_X[v];
        } else {
          slReleaseSelect = OPM_EG_PAGE_TABLE_1[v];
        }
      }

      //slot.setMUL (v)
      //  スロットの周波数の倍率を設定する
      public void setMUL (int v) {
        int t = (v & 15) << 1;
        if (t == 0) {
          t = 1;
        }
        if (slMultiply != t) {
          slMultiply = t;
          slFreq = (opmFreqTable[chKeyIndex + slDetune2Depth] + slDetune1Freq) * t >>> 1;
        }
      }

      //slot.setDT1 (v)
      //  スロットの小さいデチューンを設定する
      public void setDT1 (int v) {
        v = (v & 7) << 5;
        if (slDetune1Page != v) {
          slDetune1Page = v;  //0..224
          slDetune1Freq = opmDT1FreqTable[slDetune1Page + (chKeyCode >> 2)];
          slFreq = (opmFreqTable[chKeyIndex + slDetune2Depth] + slDetune1Freq) * slMultiply >>> 1;
        }
      }

      //slot.setTL (v)
      //  スロットのトータルレベルを設定する
      public void setTL (int v) {
        slTotalLevel = (v & 127) << OPM_TL_BITS - 7;
      }

      //slot.setAR (v)
      //  スロットのアタックレートを設定する
      public void setAR (int v) {
        v &= 31;
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
        //  0 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94
        v = (v + 31 & 32) + (v << 1);
        if (slAttackRate != v) {
          slAttackRate = v;
          v += chKeyCode >> slKeyScale;  //(0..94)+((0..127)>>(2..5))=0..125
          if (v < 94) {
            slAttackShift = OPM_EG_SHIFT_TABLE[v];
            if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
              slTransitionMask = (4 << slAttackShift) - 4;
            }
            if (OPM_EG_X) {
              slAttack3 = 0;
              slAttack4 = OPM_EG_TABLE_X[v];
            } else {
              slAttackSelect = OPM_EG_PAGE_TABLE_1[v];
            }
          } else {
            slAttackShift = 0;
            if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
              slTransitionMask = 0;
            }
            if (OPM_EG_X) {
              slAttack3 = 16;
              slAttack4 = 0;
            } else {
              slAttackSelect = 17 * 8;
            }
          }
        }
      }

      //slot.setKS (v)
      //  スロットのキースケーリングレベルを設定する
      public void setKS (int v) {
        v = 5 - (v & 3);
        //  0  1  2  3
        //  5  4  3  2
        if (slKeyScale != v) {
          slKeyScale = v;
          int u = chKeyCode >> v;
          v = slAttackRate + u;
          if (v < 94) {
            slAttackShift = OPM_EG_SHIFT_TABLE[v];
            if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
              slTransitionMask = (4 << slAttackShift) - 4;
            }
            if (OPM_EG_X) {
              slAttack3 = 0;
              slAttack4 = OPM_EG_TABLE_X[v];
            } else {
              slAttackSelect = OPM_EG_PAGE_TABLE_1[v];
            }
          } else {
            slAttackShift = 0;
            if (OPM_TRANSITION_MASK && slStage == OPMStage.ATTACK) {
              slTransitionMask = 0;
            }
            if (OPM_EG_X) {
              slAttack3 = 16;
              slAttack4 = 0;
            } else {
              slAttackSelect = 17 * 8;
            }
          }
          v = slDecayRate + u;
          slDecayShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.DECAY) {
            slTransitionMask = (4 << slDecayShift) - 4;
          }
          if (OPM_EG_X) {
            slDecay3 = 0;
            slDecay4 = OPM_EG_TABLE_X[v];
          } else {
            slDecaySelect = OPM_EG_PAGE_TABLE_1[v];
          }
          v = slSustainRate + u;
          slSustainShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.SUSTAIN) {
            slTransitionMask = (4 << slSustainShift) - 4;
          }
          if (OPM_EG_X) {
            slSustain3 = 0;
            slSustain4 = OPM_EG_TABLE_X[v];
          } else {
            slSustainSelect = OPM_EG_PAGE_TABLE_1[v];
          }
          v = slReleaseRate + u;
          slReleaseShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.RELEASE) {
            slTransitionMask = (4 << slReleaseShift) - 4;
          }
          if (OPM_EG_X) {
            slRelease3 = 0;
            slRelease4 = OPM_EG_TABLE_X[v];
          } else {
            slReleaseSelect = OPM_EG_PAGE_TABLE_1[v];
          }
        }
      }

      //slot.setD1R (v)
      //  スロットのファーストディケイレートを設定する
      //  RATE=min(63,2*D1R+(KC>>5-KS))
      public void setD1R (int v) {
        v &= 31;
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
        //  0 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94
        v = (v + 31 & 32) + (v << 1);
        if (slDecayRate != v) {
          slDecayRate = v;
          v += chKeyCode >> slKeyScale;
          slDecayShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.DECAY) {
            slTransitionMask = (4 << slDecayShift) - 4;
          }
          if (OPM_EG_X) {
            slDecay3 = 0;
            slDecay4 = OPM_EG_TABLE_X[v];
          } else {
            slDecaySelect = OPM_EG_PAGE_TABLE_1[v];
          }
        }
      }

      //slot.chSetAMSEN (v)
      //  スロットの振幅変調を有効にする
      public void chSetAMSEN (int v) {
        slAMSMask = -(v & 1);
      }

      //slot.setD2R (v)
      //  スロットのセカンドディケイレートを設定する
      //  RATE=min(63,2*D2R+(KC>>5-KS))
      public void setD2R (int v) {
        v &= 31;
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
        //  0 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94
        v = (v + 31 & 32) + (v << 1);
        if (slSustainRate != v) {
          slSustainRate = v;
          v += chKeyCode >> slKeyScale;
          slSustainShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.SUSTAIN) {
            slTransitionMask = (4 << slSustainShift) - 4;
          }
          if (OPM_EG_X) {
            slSustain3 = 0;
            slSustain4 = OPM_EG_TABLE_X[v];
          } else {
            slSustainSelect = OPM_EG_PAGE_TABLE_1[v];
          }
        }
      }

      //slot.setDT2 (v)
      //  スロットの大きいデチューンを設定する
      //  DT2  cent              倍率                slDetune2Depth
      //   1    600  (2^(1/12))^6.00=1.41421356237  600*64/100=384.00
      //   2    781  (2^(1/12))^7.81=1.57007484471  781*64/100=499.84
      //   3    950  (2^(1/12))^9.50=1.73107312201  950*64/100=608.00
      public void setDT2 (int v) {
        //  0   1   2   3
        //  0 384 500 608
        v = (384 << 20 | 500 << 10 | 608) >>> 30 - (v & 3) * 10 & 1023;
        if (slDetune2Depth != v) {
          slDetune2Depth = v;
          slFreq = (opmFreqTable[chKeyIndex + v] + slDetune1Freq) * slMultiply >>> 1;
        }
      }

      //slot.setRR (v)
      //  スロットのリリースレートを設定する
      //  RATE=min(63,2+4*RR+(KC>>5-KS))
      public void setRR (int v) {
        //   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
        //  34 38 42 46 50 54 58 62 66 70 74 78 82 86 90 94
        v = 32 + 2 + ((v & 15) << 2);
        if (slReleaseRate != v) {
          slReleaseRate = v;
          v += chKeyCode >> slKeyScale;  //(34..94)+((0..127)>>(2..5))=34..125
          slReleaseShift = OPM_EG_SHIFT_TABLE[v];
          if (OPM_TRANSITION_MASK && slStage == OPMStage.RELEASE) {
            slTransitionMask = (4 << slReleaseShift) - 4;
          }
          if (OPM_EG_X) {
            slRelease3 = 0;
            slRelease4 = OPM_EG_TABLE_X[v];
          } else {
            slReleaseSelect = OPM_EG_PAGE_TABLE_1[v];
          }
        }
      }

      //slot.setD1L (v)
      //  スロットのファーストディケイレベルを設定する
      //  D1L   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
      //   db   0  3  6  9 12 15 18 21 24 27 30 33 36 39 42 93
      public void setD1L (int v) {
        v &= 15;
        slDecayLevel = ((v + 1 & 16) + v) << (OPM_TL_BITS - 5);
      }

    }  //class OPMChannel.Slot

  }  //class OPMChannel

}  //class YM2151



