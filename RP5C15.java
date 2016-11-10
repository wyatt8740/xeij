//========================================================================================
//  RP5C15.java
//    en:RTC -- Real-Time Clock
//    ja:RTC -- リアルタイムクロック
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  カウンタ
//    日時をSystem.currentTimeMillis()からのずれとして記憶する
//      ずれの初期値はデフォルトのタイムゾーンのオフセット
//    年カウンタの起点は常に1980年とする
//      X68000のハードウェアが提供する時計なのだからOSによって異なる起点を使用することがあってはならない
//      2079年まで問題なく使用できるのだから起点を変更する必要はない
//    年カウンタの起点が固定されているので閏年カウンタへの書き込みは無効
//      閏年カウンタの値は常に年カウンタの値を4で割った余りに等しい
//      年カウンタに書き込むと閏年カウンタも更新される
//    カウンタとホストの時計の同期はmpuTask.run()で最初にカウンタがアクセスされたときに行う
//      2回目以降はカウンタの値を更新しない
//      mpuTask.run()は10ms間隔で動作するので、ホストの時計の秒針が動いてからRTCの秒カウンタが更新されるまでに最大で10msかかる
//      mpuTask.run()の動作は不連続なので、カウンタが参照される度にホストの時計を読みに行ったとしてもMPUのクロックとRTCの進み方に最大で10msのずれが生じることに変わりはない
//      正規化されていない日時を書き込んだとき、更新後は正規化された日時が読み出される
//! 以下は未対応
//  アラーム
//    アラームが無効のとき、rtcClock=FAR_FUTURE
//    アラームが有効のとき、rtcClock=直近の発動日時(歴通ミリ秒)
//    mpuTask.run()の先頭でrtcClock<=System.currentTimeMillis()ならばアラームを発動する
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,TimeZone,Timer,TimerTask,TreeMap

public class RP5C15 {

  //RP5C15のレジスタ
  //レジスタバンク0
  public static final int RTC_0_SECO0  = 0x01;  //0x00e8a001  bit0-3 1秒カウンタ
  public static final int RTC_0_SECO1  = 0x03;  //0x00e8a003  bit0-2 10秒カウンタ
  public static final int RTC_0_MINU0  = 0x05;  //0x00e8a005  bit0-3 1分カウンタ
  public static final int RTC_0_MINU1  = 0x07;  //0x00e8a007  bit0-2 10分カウンタ
  public static final int RTC_0_HOUR0  = 0x09;  //0x00e8a009  bit0-3 1時間カウンタ
  public static final int RTC_0_HOUR1  = 0x0b;  //0x00e8a00b  bit0-1 10時間カウンタ。12時間制のときbit1は0=AM,1=PM
  public static final int RTC_0_WDAY   = 0x0d;  //0x00e8a00d  bit0-2 曜日カウンタ
  //  曜日カウンタは7日でオーバーフローする日カウンタである
  //  日カウンタと同時に初期化する必要があり、日カウンタと同時にカウントアップする
  //  0～6のどれが日曜日なのかは初期化したときに決まる
  //  X68000では0=日曜日で初期化されるが、RTC側で0=日曜日と決まっているわけではない
  public static final int RTC_0_MDAY0  = 0x0f;  //0x00e8a00f  bit0-3  1日カウンタ。1=1日
  public static final int RTC_0_MDAY1  = 0x11;  //0x00e8a011  bit0-1  10日カウンタ
  //  閏年カウンタが0のとき2月を29日までカウントする
  public static final int RTC_0_MONT0  = 0x13;  //0x00e8a013  bit0-3  1月カウンタ。1=1月
  public static final int RTC_0_MONT1  = 0x15;  //0x00e8a015  bit0    10月カウンタ
  public static final int RTC_0_YEAR0  = 0x17;  //0x00e8a017  bit0-3  1年カウンタ
  public static final int RTC_0_YEAR1  = 0x19;  //0x00e8a019  bit0-3  10年カウンタ
  //  年カウンタはただの12ヶ月カウンタである
  //  閏年カウンタと同時に初期化する必要があり、閏年カウンタと同時にカウントアップする
  //  0が西暦何年なのかは初期化したときに決まる
  //  X68000では0=1980年で初期化されるが、RTC側で0=1980年と決まっているわけではない
  //レジスタバンク1
  public static final int RTC_1_CLKOUT = 0x01;  //0x00e8a001  bit0-2  CLKOUTセレクタ。0=点灯,1=16384Hz,2=1024Hz,3=128Hz,4=16Hz,5=1Hz,6=1/64Hz,7=消灯
  //  X68000ではRP5C15のCLKOUT信号がTIMER-LEDに接続されており、このレジスタはTIMER-LEDの状態を制御するために使われている
  //  IOCS _ALARMMODやIOCS _ALARMSETでアラームを許可したとき、0が書き込まれてTIMER-LEDが点灯する
  //  IOCS _ALARMMODでアラームを禁止したとき、7が書き込まれてTIMER-LEDが消灯する
  //  アラームで起動したとき、5が書き込まれてTIMER-LEDが点滅する
  //  KRAMD.SYSは転送開始時と転送終了時に7をEORすることでTIMER-LEDをRAMディスクのアクセスランプにしている
  //  #1000ではCLKOUTがMFPのGPIP5にも接続されることになっていたが、X68000では接続されていない
  public static final int RTC_1_ADJUST = 0x03;  //0x00e8a003  bit0    アジャスト。1=秒を29捨30入する
  public static final int RTC_1_MINU0  = 0x05;  //0x00e8a005  bit0-3  アラーム1分レジスタ
  public static final int RTC_1_MINU1  = 0x07;  //0x00e8a007  bit0-2  アラーム10分レジスタ
  public static final int RTC_1_HOUR0  = 0x09;  //0x00e8a009  bit0-3  アラーム1時間レジスタ
  public static final int RTC_1_HOUR1  = 0x0b;  //0x00e8a00b  bit0-2  アラーム10時間レジスタ
  public static final int RTC_1_WDAY   = 0x0d;  //0x00e8a00d  bit0-2  アラーム曜日レジスタ
  public static final int RTC_1_MDAY0  = 0x0f;  //0x00e8a00f  bit0-3  アラーム1日カウンタ
  public static final int RTC_1_MDAY1  = 0x11;  //0x00e8a011  bit0-1  アラーム10日カウンタ
  //  アラーム出力(負論理)は1分カウンタ～10日カウンタがアラーム1分レジスタ～アラーム10日レジスタとすべて一致したときに出力される
  //  ただし、アラームリセット後に一度も書き込まれていないアラームレジスタはdon't careで常に一致しているとみなされる
  //  アラームリセットすると全項目がdon't careになりアラーム出力が始まってしまうので、通常はアラーム出力をOFFにしてからアラームリセットする
  public static final int RTC_1_RULE   = 0x15;  //0x00e8a015  bit0    12時間制/24時間制セレクタ。0=12時間制,1=24時間制
  public static final int RTC_1_LEAP   = 0x17;  //0x00e8a017  bit0-1  閏年カウンタ。0=今年が閏年,1=3年後が閏年,2=2年後が閏年,3=来年が閏年
  //  閏年カウンタは4年でオーバーフローする年カウンタである
  //  年カウンタと同時に初期化する必要があり、年カウンタと同時にカウントアップする
  //  曜日カウンタと違ってRTC側で0=閏年と決まっている
  //  年カウンタが4の倍数になっているかどうかとは関係なく、閏年カウンタの値が0のときに2月を29日までカウントする
  //
  //設定
  public static final int RTC_MODE    = 0x1b;  //0x00e8a01b  bit0    0=バンク0,1=バンク1
  //                                                          bit2    0=アラーム出力OFF,1=アラーム出力ON
  //                                                          bit3    0=秒以後のカウンタ停止,1=計時開始
  public static final int RTC_TEST    = 0x1d;  //0x00e8a01d  テストレジスタ(write-only)
  //                                                          bit0-3  0=通常動作,14=テスト(高速動作)
  public static final int RTC_RESET   = 0x1f;  //0x00e8a01f  リセットレジスタ(write-only)
  //                                                          bit0    1=アラームリセット
  //                                                          bit1    1=秒以前の分周段リセット
  //                                                          bit2    0=16Hz ON
  //                                                          bit3    0=1Hz ON

  public static final byte[] rtcRegBank0 = new byte[32];  //レジスタバンク0
  public static final byte[] rtcRegBank1 = new byte[32];  //レジスタバンク1
  public static int rtcRule;  //0=12時間制,1=24時間制
  public static int rtcBank;  //0=バンク0,1=バンク1
  public static byte[] rtcRegCurrent;  //現在選択されているレジスタバンク。rtcRegBank0またはrtcRegBank1
  public static int rtcMove;  //0=停止中,8=動作中
  public static int rtcWeekGap;  //曜日のずれ。0..6。これをcdayに加えて7で割った余りが曜日カウンタの値と一致するように調整する。(cday+4)%7==0のとき日曜日なので初期値は4。曜日カウンタに書き込まれたとき増分を加え、日付カウンタに書き込まれたとき増分を引く
  public static int rtcCday;  //日付カウンタの値の歴通日
  public static int rtcDsec;  //時刻カウンタの値の日通秒
  public static long rtcCmil;  //日時カウンタの値の歴通ミリ秒
  public static long rtcCmilGap;  //rtcCmil-System.currentTimeMillis()。動作中のときだけ有効。初期値はローカルタイムを求めるときにUTCに加えるオフセット。日時カウンタに書き込まれたとき増分にスケールを掛けた値を加える。停止中から動作中に移行するとき日時カウンタの値からホストの日時を引いた値を設定する

  //rtcInit ()
  //  RTCを初期化する
  public static void rtcInit () {
    rtcRule = 1;  //24時間制
    rtcBank = 0;  //バンク0
    rtcRegCurrent = rtcRegBank0;
    rtcMove = 8;  //動作
    rtcWeekGap = 4;  //曜日のずれ
    rtcCday = -1;
    rtcDsec = -1;
    rtcCmilGap = TimeZone.getDefault ().getOffset (System.currentTimeMillis ());  //日時のずれ。ローカルタイムを求めるときにUTCに加えるオフセット。夏時間を考慮しない場合はgetRawOffset()
    rtcUpdate ();
    rtcReset ();
  }  //rtcInit()

  //リセット
  public static void rtcReset () {
  }  //rtcReset()

  //rtcUpdate ()
  //  カウンタを更新する
  //  mpuTask.run()で最初にカウンタがアクセスされたときに呼び出す
  //    00100000  42A7                  clr.l   -(sp)                       Bｧ
  //    00100002  FF20                  DOS     _SUPER                      . 
  //    00100004  4A3900E8A001          tst.b   $00E8A001.l                 J9.陟.
  //    0010000A  60F8                  bra.s   $00100004                   `.
  //  mew 100000 42a7 ff20 4a39 00e8 a001 60f8
  //  g=100000
  public static void rtcUpdate () {
    long csec = (rtcCmil = System.currentTimeMillis () + rtcCmilGap) / 1000L;  //csec=0..3471292799。2079年までカバーするためlongにする。逆数乗算を行うには被除数が大きすぎるので普通に割る。除数が小さいのでdoubleにキャストするとかえって遅くなる
    //  2079年12月31日23時59分59秒のcsecは3471292799
    //perl optdiv.pl 3471292799 86400
    //  x/86400==x*3257812231>>>48 (0<=x<=5895590398) [3471292799*3257812231==11308820137964424569]
    int mday = (int) (csec * 3257812231L >>> 48);  //cday=csec/86400=0..40176。2^63<11308820137964424569<2^64なので符号なし右シフトにすること
    //int mday = (int) (csec >> 7) / (86400 >> 7);  //cday=csec/86400。intの除算を使う。3nsくらい遅くなる
    int seco = (int) csec - mday * 86400;  //dsec=csec-cday*86400=0..86399。上位32bitは不要なのでintで計算する
    if (rtcDsec != seco) {
      rtcDsec = seco;
      //perl optdiv.pl 86399 3600
      //  x/3600==x*37283>>>27 (0<=x<=125998) [86399*37283==3221213917]
      int t = seco * 37283 >>> 27;  //hour=seco/3600=0..23。2^31<3221213917<2^32なので符号なし右シフトにすること
      //int t = seco / 3600;  //hour=seco/3600=0..23。intの除算を使う。1.5nsくらい遅くなる
      seco -= t * 3600;  //0..3599
      //  12時間制  00 01 02 03 04 05 06 07 08 09 10 11 20 21 22 23 24 25 26 27 28 29 30 31
      //  24時間制  00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23
      t = XEiJ.FMT_BCD4[t + ((1 - rtcRule & (t + 4 >> 4)) << 3)];
      rtcRegBank0[RTC_0_HOUR0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_HOUR1] = (byte) (t >> 4 & 3);
      //perl optdiv.pl 3599 60
      //  x/60==x*2185>>>17 (0<=x<=4738) [3599*2185==7863815]
      t = seco * 2185 >>> 17;  //minu=seco/60=0..59
      //t = seco / 60;
      seco -= t * 60;  //seco
      t = XEiJ.FMT_BCD4[t];
      rtcRegBank0[RTC_0_MINU0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_MINU1] = (byte) (t >> 4);
      t = XEiJ.FMT_BCD4[seco];
      rtcRegBank0[RTC_0_SECO0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_SECO1] = (byte) (t >> 4);
    }
    if (rtcCday != mday) {
      rtcCday = mday;  //日付カウンタの範囲は1980年1月1日～2079年12月31日なのでcdayの範囲は3652～40176
      int t = mday + rtcWeekGap;
      //perl optdiv.pl 40176 7
      //  x/7==x*18725>>>17 (0<=x<=43692) [40176*18725==752295600]
      rtcRegBank0[RTC_0_WDAY] = (byte) (t - (t * 18725 >>> 17) * 7);  //wday=t%7=0..6
      //rtcRegBank0[RTC_0_WDAY] = (byte) (t % 7);  //wday=t%7=0..6
      mday += 719468;  //0..759644
      //perl optdiv.pl 759644 146097
      //  x/146097==x*470369>>>36 (0<=x<=3068035) [759644*470369==357312988636]
      t = (int) ((long) mday * 470369L >>> 36);  //y400=mday/146097=0..5
      //t = mday / 146097;  //y400=mday/146097=0..5
      mday -= t * 146097;  //0..146096
      int year = t * 400;
      //perl optdiv.pl 146096 36524
      //  x/36524==x*235187>>>33 (0<=x<=255666) [146096*235187==34359879952]
      t = (int) ((long) mday * 235187L >>> 33);
      //t = mday / 36524;
      t -= t >> 2;  //y100=mday<146096?mday/36524:3=0..3
      mday -= t * 36524;  //0..36524
      year += t * 100;
      //perl optdiv.pl 36524 1461
      //  x/1461==x*22967>>>25 (0<=x<=94963) [36524*22967==838846708]
      t = mday * 22967 >>> 25;  //y4=mday/1461=0..24
      //t = mday / 1461;  //y4=mday/1461=0..24
      mday -= t * 1461;  //mday=mday%1461=0..1460
      year += t << 2;
      //perl optdiv.pl 1460 365
      //  x/365==x*1437>>>19 (0<=x<=2553) [1460*1437==2098020]
      t = mday * 1437 >>> 19;
      //t = mday / 365;
      t -= t >> 2;  //y1=mday<1460?mday/365:3=0..3
      //t = mday < 365 * 2 ? mday < 365 ? 0 : 1 : mday < 365 * 3 ? 2 : 3;  //y1=mday<1460?mday/365:3=0..3
      //perl -e "use integer;$a=99999;$b=-1;for$x(0..1460){$t=$x*1437>>19;$t-=$t>>2;$y=$x*10-$t*3650+922;$a>$y and$a=$y;$b<$y and$b=$y;}print$a.'..'.$b"
      //922..4572
      mday = mday * 10 - t * 3650 + 922;  //922..4572
      year += t;  //year=400*y400+100*y100+4*y4+y1
      //perl optdiv.pl 4572 306
      //  x/306==x*3427>>>20 (0<=x<=12238) [4572*3427==15668244]
      int mont = mday * 3427 >>> 20;  //mont=mday/306=3..14
      //int mont = mday / 306;  //mont=mday/306=3..14
      //perl -e "use integer;$a=99999;$b=-1;for$x(922..4572){$t=$x*3427>>20;$y=$x-$t*306;$a>$y and$a=$y;$b<$y and$b=$y;}print$a.'..'.$b"
      //0..305
      //perl optdiv.pl 305 10
      //  x/10==x*205>>>11 (0<=x<=1028) [305*205==62525]
      t = XEiJ.FMT_BCD4[((mday - mont * 306) * 205 >>> 11) + 1];  //(mday-mont*306)/10+1=1..31
      //t = BCD4[(mday - mont * 306) / 10 + 1];  //(mday-mont*306)/10+1=1..31
      rtcRegBank0[RTC_0_MDAY0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_MDAY1] = (byte) (t >> 4);
      if (mont > 12) {
        year++;
        mont -= 12;
      }
      t = XEiJ.FMT_BCD4[mont];
      rtcRegBank0[RTC_0_MONT0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_MONT1] = (byte) (t >> 4);
      t = XEiJ.FMT_BCD4[year - 1980] & 255;
      rtcRegBank0[RTC_0_YEAR0] = (byte) (t & 15);
      rtcRegBank0[RTC_0_YEAR1] = (byte) (t >> 4);
      rtcRegBank1[RTC_1_LEAP] = (byte) (year & 3);
    }
  }  //rtcUpdate()

}  //class RP5C15



