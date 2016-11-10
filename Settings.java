//========================================================================================
//  Settings.java
//    en:Settings
//    ja:設定
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  設定ファイル
//      +-----------------+
//      | キー1=値1       |現在の設定sgsCurrentMap
//      |      :          |
//      | キーn=値n       |
//      |                 |
//      | _=設定A         |設定名="設定A"の設定
//      | キーA1=値A1     |
//      |       :         |
//      | キーAn=値An     |
//      |                 |
//      | _=設定B         |設定名="設定B"の設定
//      | キーB1=値B1     |
//      |       :         |
//      | キーBn=値Bn     |
//      +-----------------+
//    設定を設定名の昇順に列挙したテキストファイル
//    設定毎の設定名はキー"_"の値に格納されている
//    現在の設定sgsCurrentMapは設定名が""なので必ず先頭に来る
//    設定毎にキーを昇順にソートしてから出力する
//    設定名以外のキーはすべて英小文字で始まるので設定名のキー"_"の行が必ず先頭に来る
//    現在の設定sgsCurrentMapの設定名のキー"_"の行は出力しない
//    開始時の設定sgsStartMapと同じ値を持つキーは出力しない
//
//  現在の設定
//    sgsCurrentMap
//    現在の設定sgsCurrentMapは動的に更新できるが、通常は負荷を考慮して設定を保存するときにまとめて更新する
//    現在の設定sgsCurrentMapの設定名は""。すなわち、現在の設定sgsCurrentMapのキー"_"の値は""
//    すべての設定sgsRootMapのキー""の値が現在の設定sgsCurrentMap
//
//  設定ファイルを読み込む
//    設定ファイルを読み込んですべての設定sgsRootMapに格納する
//      開始時の設定sgsStartMapと同じ値を持つキーは保存されていないので、
//      個々の設定は開始時の設定sgsStartMapを複製してから(キー"_"の値は"")、設定ファイルの内容を上書きする形で構築する
//      設定ファイルの先頭は現在の設定sgsCurrentMap。設定名は""
//    コマンドラインなどのパラメータのマップを作る。キー"config"が含まれる場合がある
//    パラメータのマップにキー"config"があるとき
//      パラメータのマップのキー"config"の値を復元する設定の設定名とする
//      復元する設定があるとき
//        復元する設定をコピーして現在の設定sgsCurrentMapとする(キー"_"の値は"")
//      コマンドラインなどのパラメータのマップからキー"config"を取り除く
//    現在の設定sgsCurrentMapにコマンドラインなどのパラメータのマップを上書きする
//
//  設定ファイルを保存する
//    sgsRootMapをテキストに変換して設定ファイルに保存する
//      開始時の設定sgsStartMapと同じ値を持つキーは出力しない
//      現在の設定sgsCurrentMapのキー"_"は出力しない
//
//  「設定を保存」 Save Settings
//    設定ファイルを保存する
//
//  「設定に名前を付けて保存」 Save Settings As
//    新しい設定の設定名が""のときは何もしない
//    すべての設定sgsRootMapに新しい設定があるときは上書きしてよいか確認する
//    現在の設定をコピーして新しい設定を作る
//    新しい設定のキー"_"の値を新しい設定の設定名にする
//    すべての設定sgsRootMapに新しい設定の設定名をキー、新しい設定を値として加える
//    設定ファイルを保存する
//
//  「名前を付けた設定を削除」 Remove Named Settings
//    削除する設定の設定名が""のときは何もしない
//    すべての設定sgsRootMapに削除する設定があるときは削除してよいか確認する
//    すべての設定sgsRootMapから削除する設定の設定名のキーを取り除く
//    設定ファイルを保存する
//
//  「設定を選択して再スタート」 Choice Settings and Restart
//! 未対応
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.net.*;  //MalformedURLException,URI,URL
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import java.util.regex.*;  //Matcher,Pattern
import javax.jnlp.*;  //BasicService,PersistenceService,ServiceManager,UnavailableServiceException
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import netscape.javascript.*;  //JSException,JSObject。jfxrt.jarではなくplugin.jarを使うこと

public class Settings {

  public static final boolean SGS_NAMED_SETTINGS = false;  //true=ローカルのとき設定に名前を付けられる。メニューで名前を指定して再起動することができないとあまり役に立たない

  //RestorableFrameのキーの一覧
  //  ウインドウの位置とサイズ(～rect)、状態(～stat)、開くかどうか(～open)のパラメータで使う
  public static final String SGS_GSA_FRAME_KEY = "gsa";
  public static final String SGS_GRS_FRAME_KEY = "grs";
  public static final String SGS_FRM_FRAME_KEY = "frm";  //FRM フレーム
  public static final String SGS_DBP_FRAME_KEY = "dbp";  //DBP データブレークポイント
  public static final String SGS_RBP_FRAME_KEY = "rbp";  //RBP ラスタブレークポイント
  public static final String SGS_SMT_FRAME_KEY = "smt";  //SMT 表示モードテスト
  public static final String SGS_SMN_FRAME_KEY = "smn";  //SMN 音声モニタ
  public static final String SGS_TRM_FRAME_KEY = "trm";  //TRM ターミナル
  public static final String SGS_PPI_FRAME_KEY = "ppi";  //PPI PPI
  public static final String SGS_FNT_FRAME_KEY = "fnt";  //FNT フォント
  public static final String SGS_PFF_FRAME_KEY = "pff";  //PFF プロファイリング
  public static final String SGS_BLG_FRAME_KEY = "blg";  //BLG 分岐ログ
  public static final String SGS_PFV_FRAME_KEY = "pfv";  //PFV プログラムフロービジュアライザ
  public static final String SGS_DRP_FRAME_KEY = "drp";  //DRP レジスタ
  public static final String SGS_DDP_FRAME_KEY = "ddp";  //DDP 逆アセンブルリスト
  public static final String SGS_DMP_FRAME_KEY = "dmp";  //DMP メモリダンプリスト
  public static final String SGS_ATW_FRAME_KEY = "atw";  //ATW アドレス変換ウインドウ
  public static final String SGS_PAA_FRAME_KEY = "paa";  //PAA 物理アドレス空間ウインドウ
  public static final String SGS_DGT_FRAME_KEY = "dgt";  //DGT コンソールウインドウ
  public static final String SGS_RTL_FRAME_KEY = "rtl";  //RTL ルートポインタリスト
  public static final String SGS_SPV_FRAME_KEY = "spv";  //SPV スプライトパターンビュア
  public static final String SGS_ACM_FRAME_KEY = "acm";  //ACM アドレス変換キャッシュモニタ
  public static final String[] SGS_FRAME_KEYS = {
    SGS_GRS_FRAME_KEY,
    SGS_GSA_FRAME_KEY,
    SGS_FRM_FRAME_KEY,
    SGS_DBP_FRAME_KEY,
    SGS_RBP_FRAME_KEY,
    SGS_SMT_FRAME_KEY,
    SGS_SMN_FRAME_KEY,
    SGS_TRM_FRAME_KEY,
    SGS_PPI_FRAME_KEY,
    SGS_FNT_FRAME_KEY,
    SGS_PFF_FRAME_KEY,
    SGS_BLG_FRAME_KEY,
    SGS_PFV_FRAME_KEY,
    SGS_DRP_FRAME_KEY,
    SGS_DDP_FRAME_KEY,
    SGS_DMP_FRAME_KEY,
    SGS_ATW_FRAME_KEY,
    SGS_PAA_FRAME_KEY,
    SGS_DGT_FRAME_KEY,
    SGS_RTL_FRAME_KEY,
    SGS_SPV_FRAME_KEY,
    SGS_ACM_FRAME_KEY,
  };

  //デフォルトのパラメータ
  //  指定できるパラメータのキーとデフォルトの値をすべて書く
  //    例外
  //      ウインドウの位置とサイズ(～bounds)と状態(～state)のパラメータはまとめて初期化するのでここには書かない
  //      ローカルのときだけ指定できるベンチマークなどのオプションはsgsGetArgumentParameters()で処理するのでここには書かない
  //      設定名のキー"_"は個別に格納するのでここには書かない
  //        デフォルトの設定sgsDefaultMapと開始時の設定sgsStartMapにキー"_"は存在しない
  //    例外を除いてここにないパラメータは指定しても無視される
  //  キーはnullではない。値もnullではない
  //  キーは""ではない。値が""のときは指定されていないものとみなす
  //  設定名のキーは"_"。設定名以外のキーは英小文字で始まり、英小文字または数字のみから成る
  //  デフォルトの値が"off"または"on"のパラメータの値は"off"または"on"に限られる
  //    "0","no","off"を指定すると"off"、それ以外は"on"に読み替えられる
  public static final String SGS_DEFAULT_PARAMETERS = (
    //PRG
    "verbose;on;" +  //冗長表示(on/off)
    //SGS
    //"_;;" +  //この設定の設定名。ここには書かない
    "saveonexit;on;" +  //終了時に設定を保存(on/off)
    "config;;" +  //復元する設定の設定名。パラメータで指定する。設定ファイルには出力しない
    "lang;en;" +  //言語(en/ja)。初期値は動作環境の言語
    "home;;" +  //ホームディレクトリ。初期値は動作環境のホームディレクトリ
    "dir;;" +  //カレントディレクトリ。初期値は動作環境のカレントディレクトリ
    //LNF
    "hhssbb;667,667,700,300,0,1000;" +  //色
    //KBD
    "keyboard;standard;" +  //キーボードの種類(none,standard,compact)
    //PNL
    "fullscreen;off;" +  //全画面表示(on/off)
    "fitinwindow;on;" +  //ウインドウに合わせる(on/off)
    "fixedscale;100;" +  //固定サイズの倍率
    "interpolation;bilinear;" +  //補間アルゴリズム(nearest,bilinear,bicubic)
    //MUS
    "seamless;on;" +  //シームレス/エクスクルーシブ(on/off)
    "edgeaccel;off;" +  //縁部加速
    "mousespeed;20;" +  //マウスカーソルの速度(0～40)
    "hostspixelunits;off;" +  //ホストの画素単位で動く(on/off)
    //MPU
    "model;Hybrid;" +  //機種(EXPERT|SUPER|XVI|Compact|Hybrid|X68030|060turbo)
    "mpu;;" +  //MPUの種類(0=MC68000/3=MC68EC030)
    "clock;;" +  //MPUの動作周波数(1..1000/EXPERT=10/SUPER=10/XVI=16.7/Compact=25/Hybrid=33.3/X68030=25)
    "mhz;100;" +  //任意の周波数(1～1000)。任意の負荷率がonのときの周波数のスピナーの値
    "util;off;" +  //任意の負荷率(on/off)
    "ratio;100;" +  //任意の負荷率(1～100)
    //FPU
    "fpumode;1;" +  //FPUモード(0=なし/1=拡張精度/2=三倍精度)
    "fullspecfpu;off;" +  //フルスペックFPU(on/off)
    //FPK
    "fefunc;on;" +  //FEファンクション命令(on/off)
    "rejectfloat;off;" +  //FLOATn.Xを組み込まない(on/off)
    //BUS
    "highmemory;0;" +  //X68030のハイメモリのサイズ(MB)(0/16)
    "highmemorysave;off;" +  //X68030のハイメモリの内容を保存する(on/off)
    "highmemorydata;;" +  //X68030のハイメモリの内容(gzip+base64)
    "localmemory;128;" +  //060turboのローカルメモリのサイズ(MB)(0/16/32/64/128/256)
    "localmemorysave;off;" +  //060turboのローカルメモリの内容を保存する(on/off)
    "localmemorydata;;" +  //060turboのローカルメモリの内容(gzip+base64)
    "cutfc2pin;off;" +  //FC2ピンをカットする(on/off)
    //MMR
    "memory;12;" +  //メインメモリのサイズ(1/2/4/6/8/10/12)
    "memorysave;on;" +  //メインメモリの内容を保存する(on/off)
    "memorydata;;" +  //メインメモリの内容(gzip+base64)
    //CRT
    "intermittent;0;" +  //間欠描画(0～4)
    //SND
    "sound;on;" +  //音声出力(on/off)
    "volume;20;" +  //ボリューム(0～40)
    "soundinterpolation;linear;" +  //音声補間(thinning/linear/constant-area/linear-area)
    //OPM
    "opmoutput;on;" +  //OPM出力
    //PCM
    "pcmoutput;on;" +  //PCM出力
    "pcminterpolation;linear;" +  //PCM補間(constant/linear/hermite)
    "pcmoscfreq;0;" +  //PCM原発振周波数(0=8MHz/4MHz,1=8MHz/16MHz)
    //FDC
    //  FDDのイメージファイル
    "fd0;;fd1;;fd2;;fd3;;" +
    //HDC
    //  SASI HDDのイメージファイル
    "hd0;;hd1;;hd2;;hd3;;hd4;;hd5;;hd6;;hd7;;hd8;;hd9;;hd10;;hd11;;hd12;;hd13;;hd14;;hd15;;" +
    //SPC
    //  SCSI HDDのイメージファイル
    //    拡張  内蔵  sc[0-7]  sc[8-15]
    //    -----------------------------
    //    有効  有効    拡張     内蔵
    //    有効  無効    拡張     無効
    //    無効  有効    内蔵     無効
    //    無効  無効    無効     無効
    "sc0;;sc1;;sc2;;sc3;;sc4;;sc5;;sc6;;sc7;;sc8;;sc9;;sc10;;sc11;;sc12;;sc13;;sc14;;sc15;;" +  //SCSI
    //PPI
    "joykey;on;" +  //キーにボタンを割り当てる
    "joyauto;on;" +  //自動有効化
    "joyblock;on;" +  //変換されたキーを入力しない
    "joymap;38,40,37,39,,90,88,,83,68;" +  //ジョイスティックのボタンを割り当てるキーと連射の設定
    //HFS
    //  ホストのディレクトリ名
    //  hf0の初期値はカレントディレクトリ
    "hf0;;hf1;;hf2;;hf3;;hf4;;hf5;;hf6;;hf7;;hf8;;hf9;;hf10;;hf11;;hf12;;hf13;;hf14;;hf15;;" +
    //EXS
    "scsiex;off;" +  //on=拡張SCSIポートを有効にする
    "scsiexrom;;" +  //SCSIEXROMのイメージファイル(8KB)。SCSIEXROM.DAT
    //SMR
    "boot;;" +  //起動デバイス(std/fdN/hdN/scN/hfN/rom$X/ram$X)
    "keydly;-1;" +  //リピートディレイ(-1=既定/200+100*n)
    "keyrep;-1;" +  //リピートインターバル(-1=既定/30+5*n^2)
    "sram;;" +  //SRAMイメージファイル名
    "sramdata;;" +  //SRAMの内容(gzip+base64)
    //ROM
    "rom;;" +  //"ROM.DAT,ROM.TMP" ROMのイメージファイル(1MB)。ROM.DAT
    "iplrom;;" +  //IPLROMのイメージファイル(128KB)
    "rom30;;" +  //X68030のROMの拡張部分のイメージファイル(128KB)。ROM30.DAT
    "cgrom;CGROM_XEiJ.DAT,CGROM.DAT;" +  //フォントファイル。CGROM_XEiJ.DATはMISAKI_G.F8,GOX80.F12,GOL80.FON,MIN.F24を組み合わせたもの
    "romdb;off;" +  //IPLでROMデバッガを強制的に起動する(on/off)
    "scsiinrom;;" +  //SCSIINROMのイメージファイル(8KB)。SCSIINROM.DAT
    "omusubi;off;");  //おむすびフォント

  public static final String SGS_APPDATA_FOLDER = "XEiJ";  //Windowsのみ。AppData/Roamingフォルダに掘るフォルダの名前
  public static final String SGS_INI = "XEiJ.ini";  //設定ファイル名

  public static final Pattern SGS_BOOT_DEVICE_PATTERN = Pattern.compile ("^(?:std|(?:fd|hd|sc|hf)\\d+|r[oa]m\\$[0-9A-Fa-f]+)$", Pattern.CASE_INSENSITIVE);  //-bootに指定できる起動デバイス名

  public static String sgsAppDataRoamingFolder;  //Windowsのみ。AppData/Roamingフォルダ。1人のユーザが複数のPCで同期して利用できるファイルを入れる
  public static String sgsAppDataLocalFolder;  //Windowsのみ。AppData/Localフォルダ。1人のユーザがこのPCだけで利用できるファイルを入れる
  public static String sgsHomeDirectory;  //ホームディレクトリ
  public static String sgsCurrentDirectory;  //カレントディレクトリ
  public static File sgsLocalIniFile;  //ローカルのみ。設定ファイル
  public static String sgsLocalIniPath;  //ローカルのみ。設定ファイル名
  public static boolean sgsSaveOnExit;  //true=終了時に設定を保存
  public static JCheckBoxMenuItem sgsSaveOnExitCheckBox;
  public static boolean sgsOpmtestOn;  //true=OPMテスト実行
  public static String sgsSaveiconValue;
  public static String sgsIrbbenchValue;

  public static HashMap<String,String> sgsDefaultMap;  //デフォルトの設定。SGS_DEFAULT_PARAMETERSを変換したもの
  public static HashMap<String,String> sgsStartMap;  //開始時の設定。デフォルトの設定に言語などを加えたもの。これと異なる値を持つキーだけ保存する
  public static HashMap<String,String> sgsCurrentMap;  //現在の設定
  public static HashMap<String,HashMap<String,String>> sgsRootMap;  //保存されているすべての設定。設定名→設定。設定名""は現在の設定。設定のキー"_"は設定名
  public static SGSInterface sgsInterface;  //設定インタフェイス

  public static JMenu sgsMenu;  //設定メニュー

  public static String[] sgsNameArray;  //設定名の配列

  //「設定に名前を付けて保存する」ウインドウ
  public static JFrame sgsSaveSettingsAsFrame;
  public static ScrollList sgsSaveSettingsAsScrollList;
  public static JTextField sgsSaveSettingsAsTextField;

  //「名前を付けた設定を削除する」ウインドウ
  public static JFrame sgsRemoveNamedSettingsFrame;
  public static ScrollList sgsRemoveNamedSettingsScrollList;
  public static JButton sgsRemoveNamedSettingsRemoveButton;

  //DictionaryComparator
  //  辞書順コンパレータ
  //  大文字と小文字を区別しない
  //  数字の並びを数の大小で比較する
  //  一致したときは改めて大文字と小文字を区別して数字を特別扱いしないで比較し直す
  public static final Comparator<String> DictionaryComparator = new Comparator<String> () {
    @Override public int compare (String s1, String s2) {
      int l1 = s1.length ();
      int l2 = s2.length ();
      int b1, b2;  //部分文字列の開始位置(このインデックスを含む)
      int e1, e2;  //部分文字列の終了位置(このインデックスを含まない)
      int f = 0;  //比較結果
    compare:
      {
        for (b1 = 0, b2 = 0; b1 < l1 && b2 < l2; b1 = e1, b2 = e2) {
          int c1, c2;
          //数字と数字以外の境目を探して部分文字列の終了位置にする
          e1 = b1;
          c1 = s1.charAt (e1);
          c1 = ('0' - 1) - c1 & c1 - ('9' + 1);  //(c1<0)==isdigit(c1)
          for (e1++; e1 < l1; e1++) {
            c2 = s1.charAt (e1);
            c2 = ('0' - 1) - c2 & c2 - ('9' + 1);  //(c2<0)==isdigit(c2)
            if ((c1 ^ c2) < 0) {  //数字と数字以外の境目
              break;
            }
            c1 = c2;
          }
          e2 = b2;
          c1 = s2.charAt (e2);
          c1 = ('0' - 1) - c1 & c1 - ('9' + 1);  //(c1<0)==isdigit(c1)
          for (e2++; e2 < l2; e2++) {
            c2 = s2.charAt (e2);
            c2 = ('0' - 1) - c2 & c2 - ('9' + 1);  //(c2<0)==isdigit(c2)
            if ((c1 ^ c2) < 0) {  //数字と数字以外の境目
              break;
            }
            c1 = c2;
          }
          c1 = s1.charAt (b1);
          c2 = s2.charAt (b2);
          if ((('0' - 1) - c1 & c1 - ('9' + 1) & ('0' - 1) - c2 & c2 - ('9' + 1)) < 0) {  //両方数字のとき
            //ゼロサプレスする
            for (; b1 < e1 && s1.charAt (b1) == '0'; b1++) {
            }
            for (; b2 < e2 && s2.charAt (b2) == '0'; b2++) {
            }
            //桁数を比較する
            f = (e1 - b1) - (e2 - b2);
            if (f != 0) {
              break compare;
            }
            //数字を比較する
            for (; b1 < e1 && b2 < e2; b1++, b2++) {
              f = s1.charAt (b1) - s2.charAt (b2);
              if (f != 0) {
                break compare;
              }
            }
          } else {  //どちらかが数字ではないとき
            //大文字と小文字を区別しないで比較する
            //  小文字化してから比較する
            for (; b1 < e1 && b2 < e2; b1++, b2++) {
              c1 = s1.charAt (b1);
              c2 = s2.charAt (b2);
              f = ((c1 + ((('A' - 1) - c1 & c1 - ('Z' + 1)) >> 31 & 'a' - 'A')) -
                   (c2 + ((('A' - 1) - c2 & c2 - ('Z' + 1)) >> 31 & 'a' - 'A')));
              if (f != 0) {
                break compare;
              }
            }
            if (b1 < e1 || b2 < e2) {  //部分文字列が片方だけ残っているとき
              //  一致したまま片方だけ残るのは両方数字以外のときだけ
              //  部分文字列が先に終わった方は文字列が終わっているか数字が続いている
              //  部分文字列が残っている方は数字ではないので1文字比較するだけで大小関係がはっきりする
              //f = (b1 < l1 ? s1.charAt (b1) : -1) - (b2 < l2 ? s2.charAt (b2) : -1);
              f = (e1 - b1) - (e2 - b2);  //部分文字列が片方だけ残っているときは残っている方が大きい
              break compare;
            }
          }  //if 両方数字のとき/どちらかが数字ではないとき
        }  //for b1,b2
        f = (l1 - b1) - (l2 - b2);  //文字列が片方だけ残っているときは残っている方が大きい
        //一致したときは改めて大文字と小文字を区別して数字を特別扱いしないで比較し直す
        if (f == 0) {
          for (b1 = 0, b2 = 0; b1 < l1 && b2 < l2; b1++, b2++) {
            f = s1.charAt (b1) - s2.charAt (b2);
            if (f != 0) {
              break compare;
            }
          }
        }
      }  //compare
      return (f >> 31) - (-f >> 31);
    }  //compare(String,String)
  };  //DictionaryComparator

  //sgsInit ()
  //  設定の初期化
  public static void sgsInit () {

    sgsAppDataRoamingFolder = null;
    sgsAppDataLocalFolder = null;
    sgsHomeDirectory = null;
    sgsCurrentDirectory = null;
    sgsLocalIniFile = null;
    sgsLocalIniPath = null;
    sgsSaveOnExit = true;
    sgsSaveOnExitCheckBox = null;

    sgsOpmtestOn = false;
    sgsSaveiconValue = null;
    sgsIrbbenchValue = null;

    //デフォルトの設定
    //  SGS_DEFAULT_PARAMETERSを分解してデフォルトの設定sgsDefaultMapを作る
    //  デフォルトの設定sgsDefaultMapには設定名を表すキー"_"が存在しない
    sgsDefaultMap = new HashMap<String,String> ();
    String[] a = SGS_DEFAULT_PARAMETERS.split (";");
    for (int i = 0, l = a.length; i < l; i += 2) {
      String key = a[i];
      String value = i + 1 < l ? a[i + 1] : "";  //splitで末尾の空要素が削除されるのでa[i+1]が存在しないとき""とみなす
      sgsDefaultMap.put (key, value);
    }
    //  ウインドウの位置とサイズと状態
    for (String key : SGS_FRAME_KEYS) {
      sgsDefaultMap.put (key + "rect", "0,0,0,0");  //ウインドウの位置とサイズ(x,y,width,height)
      sgsDefaultMap.put (key + "stat", "normal");  //ウインドウの状態(iconified/maximized/h-maximized/v-maximized/normal)
      sgsDefaultMap.put (key + "open", "off");  //ウインドウが開いているかどうか。メインのフレームも終了時にはoffになっている
    }

    //開始時の設定
    //  デフォルトの設定sgsDefaultMapのコピーに言語やホームディレクトリを追加して開始時の設定sgsStartMapを作る
    //  開始時の設定sgsStartMapには設定名を表すキー"_"が存在しない
    sgsStartMap = new HashMap<String,String> (sgsDefaultMap);
    //  言語
    switch (Locale.getDefault ().getLanguage ()) {  //動作環境の言語
    case "ja":
      sgsStartMap.put ("lang", "ja");
      break;
    }
    if (XEiJ.prgIsLocal) {  //ローカルのとき
      if (false) {
        //すべての環境変数を表示する
        //  System.getenv()はコマンドラインのみ。アプレットとJNLPではブロックされる
        System.out.println ("\n[System.getenv()]");
        new TreeMap<String,String> (System.getenv ()).forEach ((k, v) -> System.out.println (k + " = " + v));  //System.getenv()はMap<String,String>
      }
      if (false) {
        //すべてのプロパティを表示する
        //  System.getProperties()はコマンドラインのみ。アプレットとJNLPではブロックされる
        System.out.println ("\n[System.getProperties()]");
        TreeMap<String,String> m = new TreeMap<String,String> ();
        System.getProperties ().forEach ((k, v) -> m.put (k.toString (), v.toString ()));  //System.getProperties()はHashtable<Object,Object>
        m.forEach ((k, v) -> System.out.println (k + " = " + v));
      }
      //  AppDataフォルダ
      boolean isWindows = System.getProperty ("os.name").indexOf ("Windows") >= 0;  //true=Windows
      sgsAppDataRoamingFolder = isWindows ? System.getenv ("APPDATA") : null;
      sgsAppDataLocalFolder = isWindows ? System.getenv ("LOCALAPPDATA") : null;
      //  ホームディレクトリ
      //    new File("")
      sgsHomeDirectory = System.getProperty ("user.home");
      //  カレントディレクトリ
      //    new File(".")
      sgsCurrentDirectory = System.getProperty ("user.dir");
      //  設定ファイル名
      sgsLocalIniFile = new File ((sgsAppDataRoamingFolder != null ? sgsAppDataRoamingFolder + File.separator + SGS_APPDATA_FOLDER :
                                   sgsHomeDirectory != null ? sgsHomeDirectory :
                                   sgsCurrentDirectory != null ? sgsCurrentDirectory :
                                   ".") + File.separator + SGS_INI).getAbsoluteFile ();
      sgsLocalIniPath = sgsLocalIniFile.getPath ();
      //  その他
      sgsStartMap.put ("hf0", sgsHomeDirectory != null ? sgsHomeDirectory : HFS.HFS_DUMMY_UNIT_NAME);
    } else {  //ローカルでないとき
      sgsStartMap.put ("hf0", HFS.HFS_DUMMY_UNIT_NAME);
    }
    //  ウインドウに合わせる
    //    アプレットのときは固定サイズ、JNLPまたはローカルのときはウインドウに合わせる
    sgsStartMap.put ("fitinwindow", XEiJ.prgIsApplet ? "off" : "on");

    //現在の設定
    //  開始時の設定sgsStartMapをコピーして現在の設定sgsCurrentMapを作る
    //  ここで初めて設定名を表すキー"_"を追加する
    sgsCurrentMap = new HashMap<String,String> (sgsStartMap);
    sgsCurrentMap.put ("_", "");

    //保存されているすべての設定のマップ
    sgsRootMap = new HashMap<String,HashMap<String,String>> ();
    sgsRootMap.put ("", sgsCurrentMap);

    //設定インタフェイス
    sgsInterface = (XEiJ.prgIsApplet ? new SGSApplet () :
                    XEiJ.prgIsJnlp ? new SGSJnlp () :
                    new SGSLocal ());

    //設定ファイルを読み込む
    sgsLoadSettings ();

    if (XEiJ.prgIsLocal) {  //ローカルのとき
      if (sgsIrbbenchValue != null) {
        InstructionBenchmark.irbBench (sgsIrbbenchValue);
        System.exit (0);
      }
    }

  }  //sgsInit()

  //sgsTini ()
  //  後始末
  public static void sgsTini () {
    if (sgsSaveOnExit) {  //終了時に設定を保存
      sgsSaveAllSettings ();
    }
  }  //sgsTini()

  //sgsMakeMenu ()
  //  「設定」メニューを作る
  public static void sgsMakeMenu () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Save Settings on Exit":  //終了時に設定を保存
          sgsSaveOnExit = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
          break;
        case "Save Settings Now":  //今すぐ設定を保存
          sgsSaveAllSettings ();
          break;
        case "Save Settings As":  //設定に名前を付けて保存
          if (SGS_NAMED_SETTINGS && XEiJ.prgIsLocal) {
            sgsSaveSettingsAs ();
          }
          break;
        case "Remove Named Settings":  //名前を付けた設定を削除
          if (SGS_NAMED_SETTINGS && XEiJ.prgIsLocal) {
            sgsRemoveNamedSettings ();
          }
          break;
        case "Remove All Settings":  //すべての設定を削除
          sgsRemoveAllSettings ();
          break;
        }
      }
    };
    //メニュー
    sgsMenu = Multilingual.mlnText (
      XEiJ.createMenu (
        "Configuration File",
        sgsSaveOnExitCheckBox = Multilingual.mlnText (XEiJ.createCheckBoxMenuItem (sgsSaveOnExit, "Save Settings on Exit", listener), "ja", "終了時に設定を保存"),
        Multilingual.mlnText (XEiJ.createMenuItem ("Save Settings Now", listener), "ja", "今すぐ設定を保存"),
        SGS_NAMED_SETTINGS ? XEiJ.setEnabled (Multilingual.mlnText (XEiJ.createMenuItem ("Save Settings As", listener), "ja", "設定に名前を付けて保存"), XEiJ.prgIsLocal) : null,
        SGS_NAMED_SETTINGS ? XEiJ.setEnabled (Multilingual.mlnText (XEiJ.createMenuItem ("Remove Named Settings", listener), "ja", "名前を付けた設定を削除"), XEiJ.prgIsLocal) : null,
        XEiJ.createHorizontalSeparator (),
        Multilingual.mlnText (XEiJ.createMenuItem ("Remove All Settings", listener), "ja", "すべての設定を削除")),
      "ja", "設定ファイル");
  }  //sgsMakeMenu()

  //sgsLoadSettings ()
  //  設定ファイルを読み込む
  public static void sgsLoadSettings () {

    //コマンドラインなどのパラメータを読み取る
    //  ローカルのときはここで設定ファイル名が変更される場合がある
    HashMap<String,String> map = sgsInterface.sgiGetParameters ();

    //設定ファイルを読み込んですべての設定sgsRootMapに格納する
    sgsDecodeRootMap (sgsInterface.sgiLoad ());

    //コマンドラインなどのパラメータで使用する設定を選択する
    if (map.containsKey ("config")) {  //キー"config"が指定されているとき
      String name = map.get ("config");  //使用する設定名
      if (name.equals ("default")) {  //デフォルトの設定
        sgsCurrentMap.clear ();  //古いマップを消しておく
        sgsCurrentMap = new HashMap<String,String> (sgsStartMap);  //開始時の設定を現在の設定にコピーする
        sgsCurrentMap.put ("_", "");  //設定名を加える
        sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
      } else if (name.length () != 0 &&  //使用する設定名が""以外で
                 sgsRootMap.containsKey (name)) {  //存在するとき
        sgsCurrentMap.clear ();  //古いマップを消しておく
        sgsCurrentMap = new HashMap<String,String> (sgsRootMap.get (name));  //指定された設定を現在の設定にコピーする
        sgsCurrentMap.put ("_", "");  //設定名を元に戻す
        sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
      }
      map.remove ("config");  //キー"config"は指定されなかったことにする
    }

    //コマンドラインなどのパラメータを現在の設定に上書きする
    //map.forEach ((k, v) -> sgsCurrentMap.put (k, v));
    for (String key : map.keySet ()) {
      sgsCurrentMap.put (key, map.get (key));
    }

    String s;
    String[] a;

    //PRG
    XEiJ.prgLang = sgsCurrentMap.get ("lang").toLowerCase ().equals ("ja") ? "ja" : "en";  //言語
    XEiJ.prgVerbose = sgsCurrentMap.get ("verbose").equals ("on");  //冗長表示
    //SGS
    sgsSaveOnExit = sgsCurrentMap.get ("saveonexit").equals ("on");  //終了時に設定を保存
    //LNF
    //  色
    a = sgsCurrentMap.get ("hhssbb").split (",");
    if (a.length == 6) {
      LnF.lnfH0 = XEiJ.fmtParseInt (a[0], 0, 0, 2000, LnF.LNF_H0);
      LnF.lnfH1 = XEiJ.fmtParseInt (a[1], 0, 0, 2000, LnF.LNF_H1);
      LnF.lnfS0 = XEiJ.fmtParseInt (a[2], 0, 0, 1000, LnF.LNF_S0);
      LnF.lnfS1 = XEiJ.fmtParseInt (a[3], 0, 0, 1000, LnF.LNF_S1);
      LnF.lnfB0 = XEiJ.fmtParseInt (a[4], 0, 0, 1000, LnF.LNF_B0);
      LnF.lnfB1 = XEiJ.fmtParseInt (a[5], 0, 0, 1000, LnF.LNF_B1);
    } else {
      LnF.lnfH0 = LnF.LNF_H0;
      LnF.lnfH1 = LnF.LNF_H1;
      LnF.lnfS0 = LnF.LNF_S0;
      LnF.lnfS1 = LnF.LNF_S1;
      LnF.lnfB0 = LnF.LNF_B0;
      LnF.lnfB1 = LnF.LNF_B1;
    }
    //KBD
    //  キーボードの種類
    s = sgsCurrentMap.get ("keyboard").toLowerCase ();
    Keyboard.kbdOn = !s.equals ("none");
    Keyboard.kbdType = s.equals ("compact") ? Keyboard.KBD_COMPACT_TYPE : Keyboard.KBD_STANDARD_TYPE;
    //PNL
    XEiJ.pnlFullscreenOn = false;  //sgsCurrentMap.get ("fullscreen").equals ("on");  //全画面表示
    XEiJ.pnlPrevFitInWindowOn = XEiJ.pnlFitInWindowOn = !XEiJ.prgIsApplet && sgsCurrentMap.get ("fitinwindow").equals ("on");  //ウインドウに合わせる
    XEiJ.pnlFixedScale = XEiJ.fmtParseInt (sgsCurrentMap.get ("fixedscale"), 0, 10, 1000, 100);  //固定サイズの倍率
    //  補間アルゴリズム
    s = sgsCurrentMap.get ("interpolation").toLowerCase ();
    XEiJ.pnlInterpolation = (s.equals ("bicubic") ? RenderingHints.VALUE_INTERPOLATION_BICUBIC :  //三次補間
                             s.equals ("bilinear") ? RenderingHints.VALUE_INTERPOLATION_BILINEAR :  //線形補間
                             RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);  //最近傍補間
    //MUS
    XEiJ.musSeamlessOn = sgsCurrentMap.get ("seamless").equals ("on");  //シームレス/エクスクルーシブ
    XEiJ.musEdgeAccelerationOn = sgsCurrentMap.get ("edgeaccel").equals ("on");  //縁部加速
    Z8530.scc0ScaleIndex = XEiJ.fmtParseInt (sgsCurrentMap.get ("mousespeed"), 0, 0, 40, 20);  //マウスカーソルの速度
    XEiJ.musHostsPixelUnitsOn = sgsCurrentMap.get ("hostspixelunits").equals ("on");  //ホストの画素単位で動く
    //MPU
    //  機種
    s = sgsCurrentMap.get ("model").toLowerCase ();
    XEiJ.mdlModel = (s.equals ("expert") ? XEiJ.MDL_EXPERT :
                     s.equals ("super") ? XEiJ.MDL_SUPER :
                     s.equals ("xvi") ? XEiJ.MDL_XVI :
                     s.equals ("compact") ? XEiJ.MDL_COMPACT :
                     s.equals ("hybrid") ? XEiJ.MDL_HYBRID :
                     s.equals ("x68030") ? XEiJ.MDL_X68030 :
                     s.equals ("060turbo") ? XEiJ.MDL_060TURBO :
                     XEiJ.MDL_HYBRID);
    //  MPU
    s = sgsCurrentMap.get ("mpu");
    XEiJ.mdlCoreRequest = (s.equals ("0") ? 0 :
                           s.equals ("3") ? 3 :
                           s.equals ("6") ? 6 :
                           XEiJ.mdlModel == XEiJ.MDL_EXPERT ||
                           XEiJ.mdlModel == XEiJ.MDL_SUPER ||
                           XEiJ.mdlModel == XEiJ.MDL_XVI ||
                           XEiJ.mdlModel == XEiJ.MDL_COMPACT ||
                           XEiJ.mdlModel == XEiJ.MDL_HYBRID ? 0 :
                           XEiJ.mdlModel == XEiJ.MDL_X68030 ? 3 :
                           XEiJ.mdlModel == XEiJ.MDL_060TURBO ? 6 :
                           0);
    //  クロック
    s = sgsCurrentMap.get ("clock").toLowerCase ();
    switch (s) {
    case "expert":
    case "super":
      XEiJ.mdlClockRequest = 10.0;
      XEiJ.mpuUtilOn = false;
      break;
    case "xvi":
      XEiJ.mdlClockRequest = 50.0 / 3.0;  //16.7MHz
      XEiJ.mpuUtilOn = false;
      break;
    case "compact":
    case "x68030":
      XEiJ.mdlClockRequest = 25.0;
      XEiJ.mpuUtilOn = false;
      break;
    case "hybrid":
      XEiJ.mdlClockRequest = 100.0 / 3.0;  //33.3MHz
      XEiJ.mpuUtilOn = false;
      break;
    default:
      if (s.matches ("^(?:" +
                     "[-+]?" +  //符号
                     "(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)" +  //仮数部
                     "(?:[Ee][-+]?[0-9]+)?" +  //指数部
                     ")$")) {
        double d = Double.parseDouble (s);
        if (1.0 <= d && d <= 1000.0) {
          XEiJ.mdlClockRequest = d;
          XEiJ.mpuUtilOn = false;
        }
      } else {
        XEiJ.mdlClockRequest = (XEiJ.mdlModel == XEiJ.MDL_EXPERT ||
                                XEiJ.mdlModel == XEiJ.MDL_SUPER ? 10.0 :
                                XEiJ.mdlModel == XEiJ.MDL_XVI ? 50.0 / 3.0 :  //16.7MHz
                                XEiJ.mdlModel == XEiJ.MDL_COMPACT ||
                                XEiJ.mdlModel == XEiJ.MDL_HYBRID ? 100.0 / 3.0 :  //33.3MHz
                                XEiJ.mdlModel == XEiJ.MDL_X68030 ? 25.0 :
                                XEiJ.mdlModel == XEiJ.MDL_060TURBO ? 50.0 :
                                100.0 / 3.0);  //33.3MHz
      }
    }
    XEiJ.mdlIPLROMRequest = (XEiJ.mdlModel == XEiJ.MDL_EXPERT ||
                             XEiJ.mdlModel == XEiJ.MDL_SUPER ? 0 :
                             XEiJ.mdlModel == XEiJ.MDL_XVI ? 1 :
                             XEiJ.mdlModel == XEiJ.MDL_COMPACT ? 2:
                             XEiJ.mdlModel == XEiJ.MDL_HYBRID ||
                             XEiJ.mdlModel == XEiJ.MDL_X68030 ||
                             XEiJ.mdlModel == XEiJ.MDL_060TURBO ? 3 :  //5は不可
                             3);
    XEiJ.mdlSCSIINRequest = (XEiJ.mdlModel == XEiJ.MDL_EXPERT ? true :
                             XEiJ.mdlModel == XEiJ.MDL_SUPER ||
                             XEiJ.mdlModel == XEiJ.MDL_XVI ||
                             XEiJ.mdlModel == XEiJ.MDL_COMPACT ||
                             XEiJ.mdlModel == XEiJ.MDL_HYBRID ||
                             XEiJ.mdlModel == XEiJ.MDL_X68030 ||
                             XEiJ.mdlModel == XEiJ.MDL_060TURBO ? false :
                             false);
    XEiJ.mpuUtilOn = sgsCurrentMap.get ("util").equals ("on");  //任意の負荷率
    XEiJ.mpuUtilRatio = XEiJ.fmtParseInt (sgsCurrentMap.get ("ratio"), 0, 1, 100, 90);  //任意の負荷率
    XEiJ.mpuArbFreqMHz = XEiJ.fmtParseInt (sgsCurrentMap.get ("mhz"), 0, 1, 1000, 100);  //任意の周波数
    if (XEiJ.mpuUtilOn) {
      XEiJ.mpuArbFreqOn = false;
    } else {
      XEiJ.mpuArbFreqOn = !(XEiJ.mdlClockRequest == 10.0 ||
                            XEiJ.mdlClockRequest == 50.0 / 3.0 ||  //16.7MHz
                            XEiJ.mdlClockRequest == 25.0 ||
                            XEiJ.mdlClockRequest == 100.0 / 3.0 ||  //33.3MHz
                            XEiJ.mdlClockRequest == 50.0);
      if (XEiJ.mpuArbFreqOn) {
        XEiJ.mpuArbFreqMHz = (int) XEiJ.mdlClockRequest;
      }
    }
    //FPU
    XEiJ.fpuMode = XEiJ.fmtParseInt (sgsCurrentMap.get ("fpumode"), 0, 0, 2, 1);  //FPUモード
    XEiJ.fpuOn = XEiJ.mdlCoreRequest >= 2 && XEiJ.fpuMode != 0;
    XEiJ.fpuFullSpec = sgsCurrentMap.get ("fullspecfpu").equals ("on");  //フルスペックFPU
    //FPK
    FEFunction.fpkOn = sgsCurrentMap.get ("fefunc").equals ("on");  //FEファンクション命令
    FEFunction.fpkRejectFloatOn = sgsCurrentMap.get ("rejectfloat").equals ("on");  //FLOATn.Xを組み込まない
    //BUS
    XEiJ.busHighMemorySize = XEiJ.fmtParseInt (sgsCurrentMap.get ("highmemory"), 0, 0, 16, 16) << 20;  //X68030のハイメモリのサイズ
    if (!(XEiJ.busHighMemorySize == 0 << 20 ||
          XEiJ.busHighMemorySize == 16 << 20)) {
      XEiJ.busHighMemorySize = 16 << 20;
    }
    XEiJ.busHighMemorySaveOn = XEiJ.prgIsLocal && sgsCurrentMap.get ("highmemorysave").equals ("on");  //X68030のハイメモリの内容を保存する
    XEiJ.busHighMemoryData = XEiJ.prgIsLocal && XEiJ.busHighMemorySaveOn ? sgsCurrentMap.get ("highmemorydata") : "";  //X68030のハイメモリの内容(gzip+base64)
    XEiJ.busLocalMemorySize = XEiJ.fmtParseInt (sgsCurrentMap.get ("localmemory"), 0, 0, 256, 128) << 20;  //060turboのローカルメモリのサイズ(MB)
    if (!(XEiJ.busLocalMemorySize == 0 << 20 ||
          XEiJ.busLocalMemorySize == 16 << 20 ||
          XEiJ.busLocalMemorySize == 32 << 20 ||
          XEiJ.busLocalMemorySize == 64 << 20 ||
          XEiJ.busLocalMemorySize == 128 << 20 ||
          XEiJ.busLocalMemorySize == 256 << 20)) {
      XEiJ.busLocalMemorySize = 128 << 20;
    }
    XEiJ.busLocalMemorySaveOn = XEiJ.prgIsLocal && sgsCurrentMap.get ("localmemorysave").equals ("on");  //060turboのローカルメモリの内容を保存する
    XEiJ.busLocalMemoryData = XEiJ.prgIsLocal && XEiJ.busLocalMemorySaveOn ? sgsCurrentMap.get ("localmemorydata") : "";  //060turboのローカルメモリの内容(gzip+base64)
    XEiJ.busRequestCutFC2Pin = sgsCurrentMap.get ("cutfc2pin").equals ("on");  //FC2ピンをカットする(on/off)
    //MMR
    MainMemory.mmrMemorySizeRequest = XEiJ.fmtParseInt (sgsCurrentMap.get ("memory"), 0, 1, 12, 12) << 20;  //メインメモリのサイズ
    if (MainMemory.mmrMemorySizeRequest > 1 << 20 && (MainMemory.mmrMemorySizeRequest & 1 << 20) != 0) {  //3,5,7,9,11は不可
      MainMemory.mmrMemorySizeRequest = 12 << 20;
    }
    if (MainMemory.MMR_SAVE) {
      MainMemory.mmrMemorySaveOn = XEiJ.prgIsLocal && sgsCurrentMap.get ("memorysave").equals ("on");  //メインメモリの内容を保存する
      MainMemory.mmrMemoryData = XEiJ.prgIsLocal && MainMemory.mmrMemorySaveOn ? sgsCurrentMap.get ("memorydata") : "";  //メインメモリの内容
    }
    //CRT
    if (CRTC.CRT_ENABLE_INTERMITTENT) {
      CRTC.crtIntermittentInterval = XEiJ.fmtParseInt (sgsCurrentMap.get ("intermittent"), 0, 0, 4, 0);  //間欠描画
    }
    //SND
    SoundSource.sndPlayOn = sgsCurrentMap.get ("sound").equals ("on");  //音声出力
    SoundSource.sndVolume = XEiJ.fmtParseInt (sgsCurrentMap.get ("volume"), 0, 0, SoundSource.SND_VOLUME_MAX, SoundSource.SND_VOLUME_DEFAULT);  //ボリューム
    s = sgsCurrentMap.get ("soundinterpolation").toLowerCase ();
    SoundSource.sndRateConverter = (s.equals ("thinning") ? SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO :  //間引き
                             s.equals ("linear") ? SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO :  //線形補間
                             s.equals ("constant-area") ? SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000 :  //区分定数面積補間
                        s.equals ("linear-area") ? SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000 :  //線形面積補間
                             SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO);  //線形補間
    //OPM
    YM2151.opmOutputMask = sgsCurrentMap.get ("opmoutput").equals ("on") ? -1 : 0;  //OPM出力
    //PCM
    ADPCM.pcmOutputOn = sgsCurrentMap.get ("pcmoutput").equals ("on");  //PCM出力
    s = sgsCurrentMap.get ("pcminterpolation").toLowerCase ();
    ADPCM.pcmInterpolationAlgorithm = (s.equals ("constant") ? ADPCM.PCM_INTERPOLATION_CONSTANT :  //区分定数補間
                                      s.equals ("linear") ? ADPCM.PCM_INTERPOLATION_LINEAR :  //線形補間
                                      s.equals ("hermite") ? ADPCM.PCM_INTERPOLATION_HERMITE :  //エルミート補間
                                      ADPCM.PCM_INTERPOLATION_LINEAR);  //線形補間
    ADPCM.pcmOSCFreqRequest = XEiJ.fmtParseInt (sgsCurrentMap.get ("pcmoscfreq"), 0, 0, 1, 0);  //原発振周波数
    //FDC
    //!!!
    //HDC
    //!!!
    //SPC
    //!!!
    //PPI
    //  ジョイスティック
    XEiJ.ppiJoyKey = sgsCurrentMap.get ("joykey").equals ("on");
    XEiJ.ppiJoyAuto = sgsCurrentMap.get ("joyauto").equals ("on");
    XEiJ.ppiJoyBlock = sgsCurrentMap.get ("joyblock").equals ("on");
    XEiJ.ppiParseParam (sgsCurrentMap.get ("joymap"));
    //HFS
    //!!!
    //EXS
    SPC.spcSCSIEXROM = sgsCurrentMap.get ("scsiexrom");  //拡張SCSI ROMイメージファイル名
    SPC.spcSCSIEXRequest = sgsCurrentMap.get ("scsiex").equals ("on");  //拡張SCSIポートの有無
    //SMR
    XEiJ.smrSramName = sgsCurrentMap.get ("sram");  //SRAMイメージファイル名
    XEiJ.smrSramData = sgsCurrentMap.get ("sramdata");  //SRAMの内容
    XEiJ.smrRepeatDelay = XEiJ.fmtParseInt (sgsCurrentMap.get ("keydly"), 0, -1, 15, -1);  //リピートディレイ
    XEiJ.smrRepeatInterval = XEiJ.fmtParseInt (sgsCurrentMap.get ("keyrep"), 0, -1, 15, -1);  //リピートインターバル
    //  起動デバイス
    //XEiJ.smrBootDevice = -1;  //起動デバイス
    //XEiJ.smrBootROM = -1;  //ROM起動ハンドル
    //XEiJ.smrBootRAM = -1;  //RAM起動アドレス
    XEiJ.smrParseBootDevice (sgsCurrentMap.get ("boot"));
    //ROM
    XEiJ.romROMName = sgsCurrentMap.get ("rom");
    XEiJ.romIPLROMName = sgsCurrentMap.get ("iplrom");
    XEiJ.romROMDBOn = sgsCurrentMap.get ("romdb").equals ("on");
    XEiJ.romSCSIINROMName = sgsCurrentMap.get ("scsiinrom");
    XEiJ.romROM30Name = sgsCurrentMap.get ("rom30");
    XEiJ.romCGROMName = sgsCurrentMap.get ("cgrom");
    XEiJ.romOmusubiOn = sgsCurrentMap.get ("omusubi").equals ("on");

    //ウインドウの位置とサイズと状態
    for (String key : SGS_FRAME_KEYS) {
      //ウインドウの位置とサイズ
      a = sgsCurrentMap.get (key + "rect").split (",");
      if (a.length == 4) {
        RestorableFrame.rfmSetBounds (key,
                                      new Rectangle (XEiJ.fmtParseInt (a[0], 0, -4096, 4096, 0),
                                                     XEiJ.fmtParseInt (a[1], 0, -4096, 4096, 0),
                                                     XEiJ.fmtParseInt (a[2], 0, 64, 4096, 0),
                                                     XEiJ.fmtParseInt (a[3], 0, 64, 4096, 0)));
      }
      //ウインドウの状態
      s = sgsCurrentMap.get (key + "stat").toLowerCase ();
      RestorableFrame.rfmSetState (key,
                                   s.equals ("iconified") ? Frame.ICONIFIED :  //アイコン化する
                                   s.equals ("maximized") ? Frame.MAXIMIZED_BOTH :  //最大化する
                                   s.equals ("h-maximized") ? Frame.MAXIMIZED_HORIZ :  //水平方向だけ最大化する
                                   s.equals ("v-maximized") ? Frame.MAXIMIZED_VERT :  //垂直方向だけ最大化する
                                   Frame.NORMAL);  //通常表示
      //ウインドウが開いているかどうか
      RestorableFrame.rfmSetOpened (key, sgsCurrentMap.get (key + "open").equals ("on"));
    }

  }  //sgsLoadSettings()

  //sgsSaveAllSettings ()
  //  設定ファイルを保存する
  public static void sgsSaveAllSettings () {
    //PRG
    sgsCurrentMap.put ("lang", XEiJ.prgLang);  //言語
    sgsCurrentMap.put ("verbose", XEiJ.prgVerbose ? "on" : "off");  //冗長表示
    //SGS
    sgsCurrentMap.put ("saveonexit", sgsSaveOnExit ? "on" : "off");  //終了時に設定を保存
    //LNF
    sgsCurrentMap.put ("hhssbb", LnF.lnfH0 + "," + LnF.lnfH1 + "," + LnF.lnfS0 + "," + LnF.lnfS1 + "," + LnF.lnfB0 + "," + LnF.lnfB1);  //色
    //KBD
    //  キーボードの種類
    sgsCurrentMap.put ("keyboard",
                       !Keyboard.kbdOn ? "none" :
                       Keyboard.kbdType == Keyboard.KBD_COMPACT_TYPE ? "compact" :
                       Keyboard.kbdType == Keyboard.KBD_STANDARD_TYPE ? "standard" :
                       "standard");
    //PNL
    sgsCurrentMap.put ("fullscreen", XEiJ.pnlFullscreenOn ? "on" : "off");  //全画面表示
    sgsCurrentMap.put ("fitinwindow", (XEiJ.pnlFullscreenOn ? XEiJ.pnlPrevFitInWindowOn : XEiJ.pnlFitInWindowOn) ? "on" : "off");  //ウインドウに合わせる
    sgsCurrentMap.put ("fixedscale", String.valueOf (XEiJ.pnlFixedScale));  //固定サイズの倍率
    //  補間アルゴリズム
    sgsCurrentMap.put ("interpolation",
                       XEiJ.pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR ? "nearest" :
                       XEiJ.pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR ? "bilinear" :
                       XEiJ.pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC ? "bicubic" :
                       "bilinear");
    //MUS
    sgsCurrentMap.put ("seamless", XEiJ.musSeamlessOn ? "on" : "off");  //シームレス/エクスクルーシブ
    sgsCurrentMap.put ("edgeaccel", XEiJ.musEdgeAccelerationOn ? "on" : "off");  //縁部加速
    sgsCurrentMap.put ("mousespeed", String.valueOf (Z8530.scc0ScaleIndex));  //マウスカーソルの速度
    sgsCurrentMap.put ("hostspixelunits", XEiJ.musHostsPixelUnitsOn ? "on" : "off");  //ホストの画素単位で動く
    //MPU
    //  機種とMPUの種類と動作周波数
    //  機種の指定にMPUの種類と動作周波数が含まれているので必要のないものを省略する
    switch (XEiJ.mdlModel) {
    case XEiJ.MDL_EXPERT:
      sgsCurrentMap.put ("model", "EXPERT");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 10.0 ? "" :
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_SUPER:
      sgsCurrentMap.put ("model", "SUPER");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 10.0 ? "" :
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_XVI:
      sgsCurrentMap.put ("model", "XVI");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "" :  //16.7MHz
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_COMPACT:
      sgsCurrentMap.put ("model", "Compact");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 25.0 ? "" :
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_HYBRID:
      sgsCurrentMap.put ("model", "Hybrid");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_X68030:
      sgsCurrentMap.put ("model", "X68030");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "0" : XEiJ.mpuCoreType == 3 ? "" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 25.0 ? "" :
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    case XEiJ.MDL_060TURBO:
      sgsCurrentMap.put ("model", "060turbo");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "0" : XEiJ.mpuCoreType == 3 ? "3" : "");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 50.0 ? "" :
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
      break;
    default:
      sgsCurrentMap.put ("model", "");
      sgsCurrentMap.put ("mpu", XEiJ.mpuCoreType == 0 ? "0" : XEiJ.mpuCoreType == 3 ? "3" : "6");
      sgsCurrentMap.put ("clock",
                         XEiJ.mpuClockMHz == 50.0 / 3.0 ? "XVI" :  //16.7MHz
                         XEiJ.mpuClockMHz == 100.0 / 3.0 ? "Hybrid" :  //33.3MHz
                         String.valueOf ((int) (XEiJ.mpuClockMHz + 0.5)));
    }
    sgsCurrentMap.put ("mhz", String.valueOf (XEiJ.mpuArbFreqMHz));  //任意の周波数
    sgsCurrentMap.put ("util", XEiJ.mpuUtilOn ? "on" : "off");  //任意の負荷率
    sgsCurrentMap.put ("ratio", String.valueOf (XEiJ.mpuUtilRatio));  //負荷率
    //FPU
    sgsCurrentMap.put ("fpumode", String.valueOf (XEiJ.fpuMode));  //FPUモード
    sgsCurrentMap.put ("fullspecfpu", XEiJ.fpuFullSpec ? "on" : "off");  //フルスペックFPU
    //FPK
    sgsCurrentMap.put ("fefunc", FEFunction.fpkOn ? "on" : "off");  //FEファンクション命令
    sgsCurrentMap.put ("rejectfloat", FEFunction.fpkRejectFloatOn ? "on" : "off");  //FLOATn.Xを組み込まない
    //BUS
    sgsCurrentMap.put ("highmemory", String.valueOf (XEiJ.busHighMemorySize >>> 20));  //X68030のハイメモリのサイズ(MB)
    if (XEiJ.prgIsLocal) {
      sgsCurrentMap.put ("highmemorysave", XEiJ.busHighMemorySaveOn ? "on" : "off");  //X68030のハイメモリの内容を保存する
      sgsCurrentMap.put ("highmemorydata", XEiJ.busHighMemorySaveOn ? ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (XEiJ.busHighMemoryArray, 0, Math.min (XEiJ.busHighMemoryArray.length, XEiJ.busHighMemorySize))) : "");  //X68030のハイメモリの内容
    }
    sgsCurrentMap.put ("localmemory", String.valueOf (XEiJ.busLocalMemorySize >>> 20));  //060turboのローカルメモリのサイズ(MB)
    if (XEiJ.prgIsLocal) {
      sgsCurrentMap.put ("localmemorysave", XEiJ.busLocalMemorySaveOn ? "on" : "off");  //060turboのローカルメモリの内容を保存する
      sgsCurrentMap.put ("localmemorydata", XEiJ.busLocalMemorySaveOn ? ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (XEiJ.busLocalMemoryArray, 0, Math.min (XEiJ.busLocalMemoryArray.length, XEiJ.busLocalMemorySize))) : "");  //060turboのローカルメモリの内容
    }
    sgsCurrentMap.put ("cutfc2pin", XEiJ.busRequestCutFC2Pin ? "on" : "off");  //FC2ピンをカットする(on/off)
    //MMR
    sgsCurrentMap.put ("memory", String.valueOf (MainMemory.mmrMemorySizeRequest >>> 20));  //メインメモリのサイズ
    if (MainMemory.MMR_SAVE) {
      if (XEiJ.prgIsLocal) {
        sgsCurrentMap.put ("memorysave", MainMemory.mmrMemorySaveOn ? "on" : "off");  //メインメモリの内容を保存する
        sgsCurrentMap.put ("memorydata", MainMemory.mmrMemorySaveOn ? ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (MainMemory.mmrM8, 0x00000000, MainMemory.mmrMemorySizeCurrent)) : "");  //メインメモリの内容
      }
    }
    //CRT
    if (CRTC.CRT_ENABLE_INTERMITTENT) {
      sgsCurrentMap.put ("intermittent", String.valueOf (CRTC.crtIntermittentInterval));  //間欠描画
    }
    //SND
    sgsCurrentMap.put ("sound", SoundSource.sndPlayOn ? "on" : "off");  //音声出力
    sgsCurrentMap.put ("volume", String.valueOf (SoundSource.sndVolume));  //ボリューム
    sgsCurrentMap.put ("soundinterpolation",
                       SoundSource.sndRateConverter == (SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO) ? "thinning" :  //間引き
                       SoundSource.sndRateConverter == (SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO) ? "linear" :  //線形補間
                       SoundSource.sndRateConverter == SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000 ? "constant-area" :  //区分定数面積補間
                       SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000 ? "linear-area" :  //線形面積補間
                       "linear");  //線形補間
    //OPM
    sgsCurrentMap.put ("opmoutput", YM2151.opmOutputMask != 0 ? "on" : "off");  //OPM出力
    //PCM
    sgsCurrentMap.put ("pcmoutput", ADPCM.pcmOutputOn ? "on" : "off");  //PCM出力
    sgsCurrentMap.put ("pcminterpolation",
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_CONSTANT ? "constant" :  //区分定数補間
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_LINEAR ? "linear" :  //線形補間
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_HERMITE ? "hermite" :  //エルミート補間
                       "linear");  //線形補間
    sgsCurrentMap.put ("pcmoscfreq", String.valueOf (ADPCM.pcmOSCFreqRequest));  //原発振周波数
    //FDC
    //  FDDのイメージファイル
    for (int u = 0; u < FDC.FDC_MAX_UNITS; u++) {
      AbstractUnit unit = FDC.fdcUnitArray[u];
      sgsCurrentMap.put ("fd" + u,
                         unit.abuConnected && unit.abuInserted ? unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath : "");
    }
    //HDC
    //  SASI HDDのイメージファイル
    for (int u = 0; u < 16; u++) {
      AbstractUnit unit = HDC.hdcUnitArray[u];
      sgsCurrentMap.put ("hd" + u,
                         unit.abuConnected && unit.abuInserted ? unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath : "");
    }
    //SPC
    //  SCSI HDDのイメージファイル
    for (int u = 0; u < 16; u++) {
      AbstractUnit unit = SPC.spcUnitArray[u];
      sgsCurrentMap.put ("sc" + u,
                         unit.abuConnected && unit.abuInserted ? unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath : "");
    }
    //PPI
    //  ジョイスティック
    sgsCurrentMap.put ("joykey", XEiJ.ppiJoyKey ? "on" : "off");
    sgsCurrentMap.put ("joyauto", XEiJ.ppiJoyAuto ? "on" : "off");
    sgsCurrentMap.put ("joyblock", XEiJ.ppiJoyBlock ? "on" : "off");
    sgsCurrentMap.put ("joymap", XEiJ.ppiMakeParam ());
    //HFS
    //  ホストのディレクトリ名
    for (int u = 0; u < HFS.HFS_MAX_UNITS; u++) {
      AbstractUnit unit = HFS.hfsUnitArray[u];
      sgsCurrentMap.put ("hf" + u,
                         unit.abuConnected && unit.abuInserted ? unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath : "");
    }
    //EXS
    sgsCurrentMap.put ("scsiexrom", SPC.spcSCSIEXROM);  //拡張SCSI ROMイメージファイル名
    sgsCurrentMap.put ("scsiex", SPC.spcSCSIEXRequest ? "on" : "off");  //拡張SCSIポートの有無
    //SMR
    sgsCurrentMap.put ("sram", XEiJ.smrSramName);  //SRAMイメージファイル名
    sgsCurrentMap.put ("sramdata", XEiJ.smrMakeSramData ());  //SRAMの内容
    sgsCurrentMap.put ("keydly", String.valueOf (XEiJ.smrRepeatDelay));  //リピートディレイ
    sgsCurrentMap.put ("keyrep", String.valueOf (XEiJ.smrRepeatInterval));  //リピートインターバル
    //  起動デバイス
    sgsCurrentMap.put ("boot",
                       XEiJ.smrBootDevice == -1 ? "" :
                       XEiJ.smrBootDevice == 0x0000 ? "std" :
                       (XEiJ.smrBootDevice & 0xf000) == 0x9000 ? "fd" + (XEiJ.smrBootDevice >> 8 & 3) :
                       (XEiJ.smrBootDevice & 0xf000) == 0x8000 ? "hd" + (XEiJ.smrBootDevice >> 8 & 15) :
                       XEiJ.smrBootDevice == 0xa000 ?
                       (XEiJ.smrBootROM & ~(7 << 2)) == SPC.SPC_HANDLE_EX ? "sc" + (XEiJ.smrBootROM >> 2 & 7) :
                       XEiJ.smrBootROM == HFS.HFS_BOOT_HANDLE ? "hf" + HFS.hfsBootUnit :
                       String.format ("rom$%08X", XEiJ.smrBootROM) :
                       XEiJ.smrBootDevice == 0xb000 ? String.format ("ram$%08X", XEiJ.smrBootRAM) :
                       "");
    //ROM
    sgsCurrentMap.put ("rom", XEiJ.romROMName);
    sgsCurrentMap.put ("iplrom", XEiJ.romIPLROMName);
    sgsCurrentMap.put ("romdb", XEiJ.romROMDBOn ? "on" : "off");
    sgsCurrentMap.put ("scsiinrom", XEiJ.romSCSIINROMName);
    sgsCurrentMap.put ("rom30", XEiJ.romROM30Name);
    sgsCurrentMap.put ("cgrom", XEiJ.romCGROMName);
    sgsCurrentMap.put ("omusubi", XEiJ.romOmusubiOn ? "on" : "off");

    //ウインドウの位置とサイズと状態
    for (String key : SGS_FRAME_KEYS) {
      //ウインドウの位置とサイズ
      Rectangle bounds = RestorableFrame.rfmGetBounds (key);  //位置とサイズ
      sgsCurrentMap.put (key + "rect",
                         new StringBuilder ().
                         append (bounds.x).append (',').
                         append (bounds.y).append (',').
                         append (bounds.width).append (',').
                         append (bounds.height).toString ());
      //ウインドウの状態
      int state = RestorableFrame.rfmGetState (key);  //状態
      sgsCurrentMap.put (key + "stat",
                         (state & Frame.ICONIFIED) == Frame.ICONIFIED ? "iconified" :  //アイコン化されている
                         (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH ? "maximized" :  //最大化されている
                         (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_HORIZ ? "h-maximized" :  //水平方向だけ最大化されている
                         (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_VERT ? "v-maximized" :  //垂直方向だけ最大化されている
                         "normal");  //通常表示
      //ウインドウが開いているかどうか
      sgsCurrentMap.put (key + "open", RestorableFrame.rfmGetOpened (key) ? "on" : "off");
    }

    //保存する
    sgsInterface.sgiSave (sgsEncodeRootMap ());

  }  //sgsSaveSettings()

  //sgsSaveSettingsAs ()
  //  「設定に名前を付けて保存」
  public static void sgsSaveSettingsAs () {
    sgsMakeNameArray ();
    if (sgsSaveSettingsAsFrame == null) {
      sgsMakeSaveSettingsAsFrame ();
    }
    sgsSaveSettingsAsScrollList.setTexts (sgsNameArray);
    sgsSaveSettingsAsScrollList.setEnabled (sgsNameArray.length != 0);  //名前が付いている設定がなければリストは動作しない
    sgsSaveSettingsAsTextField.setText ("");  //新しい設定の名前の初期値は""
    sgsSaveSettingsAsFrame.setVisible (true);
  }  //sgsSaveSettingsAs()

  //sgsRemoveNamedSettings ()
  //  「名前を付けた設定を削除」
  public static void sgsRemoveNamedSettings () {
    sgsMakeNameArray ();
    if (sgsRemoveNamedSettingsFrame == null) {
      sgsMakeRemoveNamedSettingsFrame ();
    }
    sgsRemoveNamedSettingsScrollList.setTexts (sgsNameArray);
    sgsRemoveNamedSettingsScrollList.setEnabled (sgsNameArray.length != 0);  //名前が付いている設定がなければリストは動作しない
    sgsRemoveNamedSettingsRemoveButton.setEnabled (sgsNameArray.length != 0);  //名前が付いている設定がなければ削除ボタンは動作しない
    sgsRemoveNamedSettingsFrame.setVisible (true);
  }  //sgsRemoveNamedSettings()

  //sgsMakeSaveSettingsAsFrame ()
  //  「設定に名前を付けて保存」ウインドウを作る
  public static void sgsMakeSaveSettingsAsFrame () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Save":
          {
            String name = sgsSaveSettingsAsTextField.getText ();  //新しい設定名
            if (name.length () != 0 &&  //新しい設定名が""ではなくて
                (!sgsRootMap.containsKey (name) ||  //新しい設定名が存在しないか
                 JOptionPane.showConfirmDialog (
                   XEiJ.prgIsApplet ? null : XEiJ.frmFrame,
                   Multilingual.mlnJapanese ? "設定 " + name + " を上書きしますか？" : "Do you want to overwrite settings named " + name + " ?",
                   Multilingual.mlnJapanese ? "設定の上書きの確認" : "Confirmation of overwriting settings",
                   JOptionPane.YES_NO_OPTION,
                   JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION)) {  //上書きが許可されたとき
              HashMap<String,String> map = new HashMap<String,String> (sgsCurrentMap);  //現在の設定をコピーする
              map.put ("_", name);  //設定名
              sgsRootMap.put (name, map);  //設定を上書きする
              sgsSaveAllSettings ();  //保存する
              sgsSaveSettingsAsFrame.setVisible (false);  //ウインドウを閉じる
            }
            //新しい設定名が""または新しい設定名が存在して上書きが許可されなかったときはウインドウを閉じない
          }
          break;
        case "Cancel":
          sgsSaveSettingsAsFrame.setVisible (false);  //ウインドウを閉じる
          break;
        }
      }
    };
    //ウインドウ
    sgsSaveSettingsAsFrame = Multilingual.mlnTitle (
      XEiJ.createRestorableSubFrame (
        SGS_GSA_FRAME_KEY,
        "Save Settings As",
        null,
        XEiJ.createBorderPanel (
          0, 0,
          XEiJ.createVerticalBox (
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createLabel ("Names of existing settings"), "ja", "保存されている設定の名前"),
              Box.createHorizontalGlue ()
              ),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              sgsSaveSettingsAsScrollList = XEiJ.createScrollList (new String[0], 5, 0, new ListSelectionListener () {
                @Override public void valueChanged (ListSelectionEvent lse) {
                  if (sgsNameArray != null && sgsSaveSettingsAsTextField != null) {
                    int index = sgsSaveSettingsAsScrollList.getSelectedIndex ();  //lse.getFirstIndex()は変化した項目のインデックスの最小値であって現在選択されているインデックスではない
                    if (0 <= index && index < sgsNameArray.length) {
                      sgsSaveSettingsAsTextField.setText (sgsNameArray[index]);
                    }
                  }
                }
              }),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createLabel ("New name"), "ja", "新しい名前"),
              Box.createHorizontalGlue ()
              ),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              sgsSaveSettingsAsTextField = XEiJ.setHorizontalAlignment (XEiJ.createTextField ("", 20), JTextField.LEFT),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              Multilingual.mlnText (XEiJ.createButton ("Save", listener), "ja", "保存する"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Cancel", listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "設定に名前を付けて保存");
  }  //sgsMakeSaveSettingsAsFrame()

  //sgsMakeRemoveNamedSettingsFrame ()
  //  「名前を付けた設定を削除」ウインドウを作る
  public static void sgsMakeRemoveNamedSettingsFrame () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Remove":
          {
            int index = sgsRemoveNamedSettingsScrollList.getSelectedIndex ();
            if (0 <= index && index < sgsNameArray.length) {  //選択されている
              String name = sgsNameArray[index];
              if (name.length () != 0 &&  //削除する設定名が""ではなくて
                  sgsRootMap.containsKey (name) &&  //削除する設定が存在して
                  JOptionPane.showConfirmDialog (
                    XEiJ.prgIsApplet ? null : XEiJ.frmFrame,
                    Multilingual.mlnJapanese ? "設定 " + name + " を削除しますか？" : "Do you want to remove settings named " + name + " ?",
                    Multilingual.mlnJapanese ? "設定の削除の確認" : "Confirmation of removing settings",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION) {  //削除が許可されたとき
                sgsRootMap.remove (name);  //設定を削除する
                sgsSaveAllSettings ();  //保存する
                sgsRemoveNamedSettingsFrame.setVisible (false);  //ウインドウを閉じる
              }
              //削除する設定名が""または削除する設定名が存在して上書きが許可されなかったときはダイアログを閉じない
            }
            //選択されていないか範囲外のときはウインドウを閉じない
          }
          break;
        case "Cancel":
          sgsRemoveNamedSettingsFrame.setVisible (false);  //ウインドウを閉じる
          break;
        }
      }
    };
    //ウインドウ
    sgsRemoveNamedSettingsFrame = Multilingual.mlnTitle (
      XEiJ.createRestorableSubFrame (
        SGS_GRS_FRAME_KEY,
        "Remove Named Settings",
        null,
        XEiJ.createBorderPanel (
          0, 0,
          XEiJ.createVerticalBox (
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createLabel ("Names of existing settings"), "ja", "保存されている設定の名前"),
              Box.createHorizontalGlue ()
              ),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              sgsRemoveNamedSettingsScrollList = XEiJ.createScrollList (new String[0], 5, 0, null),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              sgsRemoveNamedSettingsRemoveButton = Multilingual.mlnText (XEiJ.createButton ("Remove", listener), "ja", "削除する"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Cancel", listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "名前を付けた設定を削除");
  }  //sgsMakeRemoveNamedSettingsFrame()

  //sgsMakeNameArray ()
  //  設定名の配列を作る
  public static void sgsMakeNameArray () {
    ArrayList<String> nameList = new ArrayList<String> (sgsRootMap.keySet ());  //設定名のリスト
    nameList.sort (DictionaryComparator);  //設定名をソートする。設定名が""の現在の設定が先頭に来る
    nameList.remove (0);  //先頭にある現在の設定の設定名""を取り除く
    sgsNameArray = nameList.toArray (new String[0]);  //現在の設定を除いてソートされた設定名の配列
  }  //sgsMakeNameArray()

  //sgsRemoveAllSettings ()
  //  すべての設定を削除する
  public static void sgsRemoveAllSettings () {
    if (JOptionPane.showConfirmDialog (
      XEiJ.prgIsApplet ? null : XEiJ.frmFrame,
      Multilingual.mlnJapanese ? "すべての設定を削除しますか？" : "Do you want to remove all settings?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION) {  //Yes
      //すべての設定を削除する
      sgsInterface.sgiDelete ();
      //終了時に設定を保存をOFFにする
      sgsSaveOnExitCheckBox.setSelected (false);
      sgsSaveOnExit = false;
    }
  }  //sgsRemoveAllSettings()

  //sgsDecodeRootMap (text)
  //  テキストをsgsRootMapに変換する
  public static void sgsDecodeRootMap (String text) {
    sgsRootMap.clear ();  //すべての設定を消す
    sgsCurrentMap.clear ();  //古いマップを消しておく
    sgsCurrentMap = new HashMap<String,String> (sgsStartMap);  //開始時の設定を現在の設定にコピーする
    sgsCurrentMap.put ("_", "");  //設定名を加える
    sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
    HashMap<String,String> map = sgsCurrentMap;  //現在変換中の設定は現在の設定
    for (String line : text.split ("\n")) {
      line = line.trim ();  //キーの前の空白と値の後の空白を取り除く
      if (line.length () == 0 ||  //空行
          line.startsWith ("#")) {  //注釈
        continue;
      }
      int i = line.indexOf ('=');
      if (i < 0) {  //'='がない
        continue;
      }
      String key = line.substring (0, i).trim ().toLowerCase ();  //キー。後('='の前)の空白を取り除いて小文字化する
      String value = line.substring (i + 1).trim ();  //値。前('='の後)の空白を取り除く
      if (key.equals ("_")) {  //設定名。新しい設定の最初の行
        if (sgsRootMap.containsKey (value)) {  //同じ設定名が2回出てきたとき
          if (false) {
            map = null;  //新しい設定名が指定されるまで読み飛ばす(最初に書いた設定が残る)
          } else {
            map = sgsRootMap.get (value);  //既存の設定に上書きする(最後に書いた設定が残る)
          }
        } else {  //新しい設定
          map = new HashMap<String,String> (sgsStartMap);  //開始時の設定をコピーする
          map.put (key, value);  //sgsPutParameterは設定名のキー"_"を受け付けないことに注意
          sgsRootMap.put (value, map);
        }
        continue;
      }
      if (map == null) {  //新しい設定名が指定されるまで読み飛ばす
        continue;
      }
      sgsPutParameter (map, key, value);
    }  //for line
  }  //sgsDecodeRootMap()

  //text = sgsEncodeRootMap ()
  //  sgsRootMapをテキストに変換する
  public static String sgsEncodeRootMap () {
    StringBuilder sb = new StringBuilder ();
    String[] nameArray = sgsRootMap.keySet ().toArray (new String[0]);  //設定名の配列
    Arrays.sort (nameArray, DictionaryComparator);  //設定名をソートする。設定名が""の現在の設定が先頭に来る
    for (String name : nameArray) {
      HashMap<String,String> map = sgsRootMap.get (name);  //個々の設定
      if (map != sgsCurrentMap) {  //(先頭の)現在の設定でないとき
        sb.append ('\n');  //1行空ける
      }
      String[] keyArray = map.keySet ().toArray (new String[0]);  //キーの配列
      Arrays.sort (keyArray, DictionaryComparator);  //キーをソートする。設定名以外のキーはすべて英小文字で始まるので設定名のキー"_"が先頭に来る
      for (String key : keyArray) {
        String value = map.get (key);
        if (!(map == sgsCurrentMap && key.equals ("_")) &&  //現在の設定の設定名でない
            !key.equals ("config") &&  //キー"config"は設定ファイルに出力しない
            !value.equals (sgsStartMap.get (key))) {  //開始時の設定にないか、開始時の設定と異なる
          sb.append (key).append ('=').append (value).append ('\n');
        }
      }
    }
    return sb.toString ();
  }  //sgsEncodeRootMap()

  //map = sgsGetAppletParameters ()
  //  アプレットの<object><param>～</param></object>で指定されたパラメータを読み取る
  public static HashMap<String,String> sgsGetAppletParameters () {
    HashMap<String,String> map = new HashMap<String,String> ();
    int fdNumber = 0;
    int hdNumber = 0;
    int scNumber = 0;
    for (String key : sgsDefaultMap.keySet ()) {  //指定できる名前をすべて試す
      String value = XEiJApplet.appApplet.getParameter (key);
      if (value != null) {  //指定されている
        if (key.equals ("boot") && !SGS_BOOT_DEVICE_PATTERN.matcher (value).matches ()) {  //キーがbootだが値が起動デバイス名でない
          String valueWithoutColonR = value.toUpperCase ().endsWith (":R") ? value.substring (0, value.length () - 2) : value;  //末尾の":R"を取り除いた部分
          int length = XEiJ.ismLength (valueWithoutColonR, HDMedia.HDM_MAX_BYTES_PER_DISK + 1);  //ファイルサイズ。40MBのSASI HDのサイズまで区別できればよい
          if (length >= 0) {  //ファイルがある
            key = (FDMedia.fdmPathToMedia (valueWithoutColonR, null) != null ? "fd" + fdNumber++ :  //FD
                   HDMedia.hdmLengthToMedia (length) != null ? "hd" + hdNumber++ :  //SASI HD
                   "sc" + scNumber++);  //それ以外はSCSI ハードディスク/CD-ROM
            sgsPutParameter (map, "boot", key);  //起動デバイスを設定する
          } else {  //ファイルがない
            //!!! エラー
            continue;
          }
        }
        sgsPutParameter (map, key, value);  //パラメータを設定する
      }
    }
    return map;
  }  //sgsGetAppletParameters()

  //map = sgsGetArgumentParameters ()
  //  ローカルのコマンドラインまたは*.jnlpファイルの<application-desc><argument>～</argument></application-desc>で指定されたパラメータを読み取る
  public static HashMap<String,String> sgsGetArgumentParameters () {
    HashMap<String,String> map = new HashMap<String,String> ();
    int fdNumber = 0;
    int hdNumber = 0;
    int scNumber = 0;
    int hfNumber = 0;
    for (int i = 0; i < XEiJ.prgArgs.length; i++) {
      String key = null;  //キー
      String value = XEiJ.prgArgs[i];  //引数。nullではないはず
    arg:
      {
        boolean boot = false;  //true=valueは-bootの値
        if (value.startsWith ("-")) {  //引数が"-"で始まっている
          //!!! 値が必要なものと必要でないものを区別したい
          int k = value.indexOf ('=', 1);
          if (k >= 0) {  //引数が"-"で始まっていて2文字目以降に"="がある
            key = value.substring (1, k);  //"-"の後ろから"="の手前まではキー
            value = value.substring (k + 1);  //"="の後ろは値
          } else {  //引数が"-"で始まっていて2文字目以降に"="がない
            //!!! "-"で始まる引数はすべてキーとみなされるので"-キー 値"の形では値に負の数値を書くことはできない
            key = value.substring (1);  //"-"の後ろはキー
            value = (i + 1 < XEiJ.prgArgs.length && !XEiJ.prgArgs[i + 1].startsWith ("-") ?  //次の引数があって次の引数が"-"で始まっていない
                     XEiJ.prgArgs[++i]  //次の引数は値
                     :  //次の引数がないまたは次の引数が"-"で始まっている
                     "1");  //値は"1"
          }
          if (!key.equalsIgnoreCase ("boot")) {  //-bootではない
            break arg;
          }
          boot = true;
        }
        //引数が"-"で始まっていないまたは-bootの値
        if (SGS_BOOT_DEVICE_PATTERN.matcher (value).matches ()) {  //起動デバイス名のとき
          //ファイルやディレクトリを探さず起動デバイスだけ設定する
          key = "boot";
          break arg;
        }
        String valueWithoutColonR = value.toUpperCase ().endsWith (":R") ? value.substring (0, value.length () - 2) : value;  //末尾の":R"を取り除いた部分
        int length = XEiJ.ismLength (valueWithoutColonR, HDMedia.HDM_MAX_BYTES_PER_DISK + 1);  //ファイルサイズ。40MBのSASI HDのサイズまで区別できればよい
        if (length >= 0) {  //ファイルがある
          key = (FDMedia.fdmPathToMedia (valueWithoutColonR, null) != null ? "fd" + fdNumber++ :  //FD
                 HDMedia.hdmLengthToMedia (length) != null ? "hd" + hdNumber++ :  //SASI HD
                 "sc" + scNumber++);  //それ以外はSCSI ハードディスク/CD-ROM
        } else if (XEiJ.prgIsLocal &&  //ローカルのとき
                   new File (valueWithoutColonR).isDirectory ()) {  //ディレクトリがある
          key = "hf" + hfNumber++;
        } else {  //ファイルがなくディレクトリもない
          //!!! エラー
          continue;
        }
        if (boot) {  //-bootの値のとき
          sgsPutParameter (map, "boot", key);  //起動デバイスを設定する
        }
      }  //arg
      if (XEiJ.prgIsLocal) {  //ローカルのとき
        //ローカルのときだけ指定できるオプション
        switch (key) {
        case "ini":
          sgsLocalIniFile = new File (value).getAbsoluteFile ();
          sgsLocalIniPath = sgsLocalIniFile.getPath ();
          break;
        case "opmtest":
          sgsOpmtestOn = true;
          break;
        case "saveicon":
          sgsSaveiconValue = value;
          break;
        case "irbbench":
          sgsIrbbenchValue = value;
          break;
        default:
          sgsPutParameter (map, key, value);  //パラメータを設定する
        }
      } else {  //ローカルでないとき
        sgsPutParameter (map, key, value);  //パラメータを設定する
      }  //if XEiJ.prgIsLocal
    }
    return map;
  }  //sgsGetArgumentParameters()

  //sgsPutParameter (map, key, value)
  //  マップにパラメータを追加する
  //  デフォルトの設定sgsDefaultMapにないパラメータは無視される。設定名のキー"_"を受け付けないことに注意
  //  デフォルトの値が"off"または"on"のパラメータの値は"0","no","off"を指定すると"off"、それ以外は"on"に読み替えられる
  public static void sgsPutParameter (HashMap<String,String> map, String key, String value) {
    if (sgsDefaultMap.containsKey (key)) {  //設定できるパラメータ
      String defaultValue = sgsDefaultMap.get (key);  //デフォルトの値
      if (defaultValue.equals ("off") || defaultValue.equals ("on")) {  //デフォルトの値が"off"または"on"のとき
        value = (value.equals ("0") ||
                 value.equalsIgnoreCase ("no") ||
                 value.equalsIgnoreCase ("off") ? "off" : "on");  //"0","no","off"を"off"にそれ以外を"on"に読み替える
      }
      map.put (key, value);  //マップに追加する
    }
  }  //sgsPutParameter(HashMap<String,String>,String,String)



  //================================================================================
  //$$SGI 設定インタフェイス
  //  起動方法に応じてパラメータの読み取りと設定の入出力を行う
  public interface SGSInterface {

    //map = gate.sgiGetParameters ()
    //  パラメータを読み取る
    public HashMap<String,String> sgiGetParameters ();

    //text = gate.sgiLoad ()
    //  設定ファイルを読み込む
    public String sgiLoad ();

    //gate.sgiSave ()
    //  設定ファイルに書き出す
    public void sgiSave (String text);

    //gate.sgiDelete ()
    //  設定ファイルを削除する
    public void sgiDelete ();

  }



  //================================================================================
  //$$SGA アプレットの設定
  //  アプレットのときパラメータの読み取りと設定の入出力を行う
  //  アプレットの<object><param>～</param></object>で指定されたパラメータを読み取る
  //  localStorageに設定を保存する
  //    localStorageはCookieと違って任意の文字列を保存できる。Percent-Encodingやbase64でエンコードする必要はない
  //  ドキュメントベース毎にそれぞれ設定ファイルを作る
  public static class SGSApplet implements SGSInterface {

    private String sgaItemName;

    //コンストラクタ
    private SGSApplet () {
      sgaItemName = XEiJApplet.appApplet.getDocumentBase ().toString () + "/" + SGS_INI;
    }  //new SGSApplet()

    //map = gate.sgiGetParameters ()
    //  パラメータを読み取る
    @Override public HashMap<String,String> sgiGetParameters () {
      return sgsGetAppletParameters ();
    }  //sgiGetParameters()

    //text = gate.sgiLoad ()
    //  設定ファイルを読み込む
    @Override public String sgiLoad () {
      JSObject localStorage = XEiJApplet.appGetJSObject (XEiJApplet.appWindow, "localStorage");  //window.localStorage
      if (localStorage != null) {
        String value = XEiJApplet.appCallString (localStorage, "getItem", sgaItemName);
        if (value != null) {
          return value;
        }
      }
      return "";
    }  //sgiLoad()

    //gate.sgiSave ()
    //  設定ファイルに書き出す
    @Override public void sgiSave (String text) {
      JSObject localStorage = XEiJApplet.appGetJSObject (XEiJApplet.appWindow, "localStorage");  //window.localStorage
      if (localStorage != null) {
        XEiJApplet.appCallString (localStorage, "setItem", sgaItemName, text);
      }
    }  //sgiSave(String)

    //gate.sgiDelete ()
    //  設定ファイルを削除する
    @Override public void sgiDelete () {
      JSObject localStorage = XEiJApplet.appGetJSObject (XEiJApplet.appWindow, "localStorage");  //window.localStorage
      if (localStorage != null) {
        XEiJApplet.appCallString (localStorage, "removeItem", sgaItemName);
      }
    }  //sgiDelete()

  }  //class SGSApplet



  //================================================================================
  //$$SGJ JNLPの設定
  //  JNLPのときパラメータの読み取りと設定の入出力を行う
  //  *.jnlpファイルの<application-desc><argument>～</argument></application-desc>で指定されたパラメータを読み取る
  //  PersistenceServiceを用いてローカルファイルに設定を保存する
  //    Windows7の場合
  //      C:/Users/ユーザー名/AppData/LocalLow/Sun/Java/Deployment/cache/6.0/muffin/
  //    に保存される
  //    保存されたローカルファイルの残骸はJavaコントロールパネルで削除できる
  //
  //  ServiceManagerはJNLPでなければクラスパスが通っていないので初期化することもできない
  //  XEiJのstaticメソッドにstaticメソッドのServiceManager.lookup()を呼び出すコードがあると、
  //  JNLPでないときXEiJをロードしたときにServiceManagerを初期化しようとしてエラーが出て起動できない
  //  XEiJをロードしたときにエラーが出るのでServiceManager.lookup()を呼び出さなくてもtry～catchで囲んでも同じ
  public static class SGSJnlp implements SGSInterface {

    private static final int SGJ_MAXSIZE = 1024 * 64;  //設定ファイルの最大サイズ。64KB。SRAMの内容が複数入るので大きめにしておく

    private BasicService sgjBasicService;
    private PersistenceService sgjPersistenceService;
    private URL sgjURL;

    //コンストラクタ
    private SGSJnlp () {
      sgjBasicService = null;
      sgjPersistenceService = null;
      sgjURL = null;
      try {
        sgjBasicService = (BasicService) ServiceManager.lookup ("javax.jnlp.BasicService");
        sgjPersistenceService = (PersistenceService) ServiceManager.lookup ("javax.jnlp.PersistenceService");
        if (sgjBasicService != null && sgjPersistenceService != null) {
          sgjURL = new URL (sgjBasicService.getCodeBase ().toString () + SGS_INI);
        }
      } catch (UnavailableServiceException use) {  //利用できない
      } catch (MalformedURLException mue) {
      }
    }  //new SGSJnlp()

    //map = gate.sgiGetParameters ()
    //  パラメータを読み取る
    @Override public HashMap<String,String> sgiGetParameters () {
      return sgsGetArgumentParameters ();
    }  //sgiGetParameters()

    //text = gate.sgiLoad ()
    //  設定ファイルを読み込む
    @Override public String sgiLoad () {
      if (sgjURL != null) {
        FileContents fc;
        try {
          fc = sgjPersistenceService.get (sgjURL);
        } catch (FileNotFoundException fnfe) {  //ファイルが存在しない
          return "";
        } catch (IOException ioe) {  //入出力エラー
          return "";
        }
        try (BufferedInputStream in = new BufferedInputStream (fc.getInputStream ())) {
          int l = (int) fc.getLength ();
          byte[] bb = new byte[l];
          int k = 0;
          while (k < l) {
            int t = in.read (bb, k, l - k);
            if (t < 0) {
              break;
            }
            k += t;
          }
          if (k == l) {  //最後まで読み出せた
            return new String (bb, "UTF-8");
          }
        } catch (IOException ioe) {  //入出力エラー
          return "";
        }
      }  //if sgjURL!=null
      return "";
    }  //sgiLoad()

    //gate.sgiSave ()
    //  設定ファイルに書き出す
    @Override public void sgiSave (String text) {
      if (sgjURL != null) {
        FileContents fc;
        try {
          fc = sgjPersistenceService.get (sgjURL);
        } catch (FileNotFoundException fnfe) {  //ファイルが存在しない
          try {
            sgjPersistenceService.create (sgjURL, SGJ_MAXSIZE);  //ファイルを作る
            sgjPersistenceService.setTag (sgjURL, PersistenceService.DIRTY);  //サーバがコピーを持っていないのでdirty
            try {
              fc = sgjPersistenceService.get (sgjURL);
            } catch (FileNotFoundException fnfe2) {  //ファイルが存在しない
              return;
            }
          } catch (IOException ioe) {  //入出力エラー
            return;
          }
        } catch (IOException ioe) {  //入出力エラー
          return;
        }
        try (BufferedOutputStream out = new BufferedOutputStream (fc.getOutputStream (true))) {
          out.write (text.getBytes ("UTF-8"));
        } catch (IOException ioe) {  //入出力エラー
          return;
        }
      }
    }  //sgiSave(String)

    //gate.sgiDelete ()
    //  設定ファイルを削除する
    @Override public void sgiDelete () {
      if (sgjURL != null) {
        try {
          sgjPersistenceService.delete (sgjURL);
        } catch (IOException ioe) {  //入出力エラー
          return;
        }
      }
    }  //sgiDelete()

  }  //class SGSJnlp



  //================================================================================
  //$$SGL ローカルの設定
  //  ローカルのときパラメータの読み取りと設定の入出力を行う
  //  で指定されたパラメータを読み取る
  //  ホームディレクトリに設定ファイルを作る
  public static class SGSLocal implements SGSInterface {

    //map = gate.sgiGetParameters ()
    //  パラメータを読み取る
    @Override public HashMap<String,String> sgiGetParameters () {
      return sgsGetArgumentParameters ();
    }  //sgiGetParameters()

    //text = gate.sgiLoad ()
    //  設定ファイルを読み込む
    @Override public String sgiLoad () {
      StringBuilder sb = new StringBuilder ();
      if (sgsLocalIniFile.isFile ()) {
        try (BufferedReader r = new BufferedReader (new InputStreamReader (new FileInputStream (sgsLocalIniFile), "UTF-8"))) {
          for (String line = r.readLine (); line != null; line = r.readLine ()) {
            sb.append (line).append ('\n');
          }
        } catch (IOException ioe) {  //入出力エラー
          return "";
        }
      }
      return sb.toString ();
    }  //sgiLoad()

    //gate.sgiSave ()
    //  設定ファイルに書き出す
    @Override public void sgiSave (String text) {
      //内容が変化していなければ何もしない
      //  内容が変化していないのに保存すると*.bakの意味がなくなる
      if (sgiLoad ().equals (text)) {  //内容が変化していない
        return;
      }
      //親ディレクトリがなければ作る
      File parentFile = sgsLocalIniFile.getParentFile ();
      if (parentFile != null &&  //ルートディレクトリの可能性があるのでparentFile==nullは素通りさせる
          !parentFile.isDirectory () && !parentFile.mkdir ()) {  //親ディレクトリがなくて作れない
        return;
      }
      //*.tmpを削除する
      File tmpFile = new File (sgsLocalIniPath + ".tmp");
      if (tmpFile.exists () && !tmpFile.delete ()) {  //*.tmpがあるが*.tmpを削除できない
        return;
      }
      //*.tmpに書き出す
      try (BufferedWriter w = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (tmpFile), "UTF-8"))) {
        w.write (text);
      } catch (IOException ioe) {  //入出力エラー
        return;
      }
      //設定ファイルを*.bakにリネームする
      //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
      File bakFile = new File (sgsLocalIniPath + ".bak");
      if (sgsLocalIniFile.exists () &&  //設定ファイルがあるが
          (bakFile.exists () && !bakFile.delete () ||  //*.bakがあるが*.bakを削除できないまたは
           !sgsLocalIniFile.renameTo (bakFile))) {  //設定ファイルを*.bakにリネームできない
        return;
      }
      //*.tmpを設定ファイルにリネームする
      //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
      if (!tmpFile.renameTo (sgsLocalIniFile)) {  //*.tmpを設定ファイルにリネームできない
        return;
      }
    }  //sgiSave(String)

    //gate.sgiDelete ()
    //  設定ファイルを削除する
    @Override public void sgiDelete () {
      //設定ファイルを*.bakにリネームする
      //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
      File bakFile = new File (sgsLocalIniPath + ".bak");
      if (sgsLocalIniFile.exists () &&  //設定ファイルがあるが
          (bakFile.exists () && !bakFile.delete () ||  //*.bakがあるが*.bakを削除できないまたは
           !sgsLocalIniFile.renameTo (bakFile))) {  //設定ファイルを*.bakにリネームできない
        return;
      }
      //設定ファイルを削除する
      if (sgsLocalIniFile.exists () && !sgsLocalIniFile.delete ()) {  //設定ファイルがあるが設定ファイルを削除できない
        return;
      }
    }  //sgiDelete()

  }  //class SGSLocal



}  //class SGS



