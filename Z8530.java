//========================================================================================
//  Z8530.java
//    en:SCC -- Serial Communication Controller
//    ja:SCC -- シリアルコミュニケーションコントローラ
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  マウス
//    マウスイベントとマウスモーションイベントで取得したデータを返す
//  RS-232C
//    ターミナルに接続
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class Z8530 {

  public static final boolean SCC_DEBUG_TRACE = false;
  public static boolean sccTraceOn;
  public static JMenuItem sccTraceMenuItem;

  public static final int SCC_FREQ = 5000000;  //SCCの動作周波数。5MHz

  //ポート
  public static final int SCC_0_COMMAND = 0x00e98001;
  public static final int SCC_0_DATA    = 0x00e98003;
  public static final int SCC_1_COMMAND = 0x00e98005;
  public static final int SCC_1_DATA    = 0x00e98007;

  //割り込み
  //  ベクタ
  //    0  ポート0B送信バッファ空(マウス送信)
  //    1  ポート0B外部/ステータス変化
  //    2  ポート0B受信バッファフル(マウス受信)
  //    3  ポート0B特別受信条件
  //    4  ポート1A送信バッファ空(RS-232C送信)
  //    5  ポート1A外部/ステータス変化
  //    6  ポート1A受信バッファフル(RS-232C受信)
  //    7  ポート1A特別受信条件
  //  マスク
  //    0x80  常に0
  //    0x40  常に0
  //    0x20  ポート1A受信バッファフル(RS-232C受信)
  //    0x10  ポート1A送信バッファ空(RS-232C送信)
  //    0x08  ポート1A外部/ステータス変化
  //    0x04  ポート0B受信バッファフル(マウス受信)
  //    0x02  ポート0B送信バッファ空(マウス送信)
  //    0x01  ポート0B外部/ステータス変化
  //  優先順位
  //    高い  1A受信バッファフル(RS-232C受信)
  //          1A送信バッファ空(RS-232C送信)
  //          1A外部/ステータス変化
  //          0B受信バッファフル(マウス受信)
  //          0B送信バッファ空(マウス送信)
  //    低い  0B外部/ステータス変化
  //!!! マウス送信、外部/ステータス変化、特別受信条件の割り込みは未実装
  public static int sccInterruptVector;  //非修飾ベクタ。WR2
  public static int sccVectorInclude;  //WR9&0x11
  //  マウス受信割り込み
  public static final int SCC_0B_RECEIVE_VECTOR = 2;
  public static final int SCC_0B_RECEIVE_MASK = 0x04;
  public static int scc0bReceiveMask;  //マスク
  public static int scc0bReceiveRR3;  //RR3のペンディングビット。割り込み発生でセット
  public static int scc0bReceiveRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc0bReceiveVector;  //修飾ベクタ
  //  RS-232C受信割り込み
  public static final int SCC_1A_RECEIVE_VECTOR = 6;
  public static final int SCC_1A_RECEIVE_MASK = 0x20;
  public static int scc1aReceiveMask;  //マスク
  public static int scc1aReceiveRR3;  //RR3のペンディングビット。割り込み発生でセット
  public static int scc1aReceiveRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc1aReceiveVector;  //修飾ベクタ
  //  RS-232C送信割り込み
  public static final int SCC_1A_SEND_VECTOR = 4;
  public static final int SCC_1A_SEND_MASK = 0x10;
  public static int scc1aSendMask;  //マスク
  public static int scc1aSendRR3;  //RR3のペンディングビット。割り込み発生でセット
  public static int scc1aSendRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc1aSendVector;  //修飾ベクタ

  //ポートB マウス
  public static int scc0RegisterNumber;
  public static int scc0Rts;  //RTS(0または1)
  public static int scc0BaudRateGen;  //WR13<<8|WR12
  public static int scc0InputCounter;  //マウスデータのカウンタ。0～2
  public static int scc0Data1;
  public static int scc0Data2;
  public static final int SCC0_SCALE_MIN = 0;
  public static final int SCC0_SCALE_MAX = 40;
  public static final int SCC0_SCALE_MID = (SCC0_SCALE_MAX - SCC0_SCALE_MIN) >> 1;
  public static int scc0ScaleIndex;  //マウスの移動速度のスケール。SCC0_SCALE_MIN～SCC0_SCALE_MAX
  public static int scc0RatioX;  //マウスの移動速度の係数*65536
  public static int scc0RatioY;
  public static final String[] scc0Texts = new String[SCC0_SCALE_MAX - SCC0_SCALE_MIN + 1];  //スケールのテキストの配列
  public static JLabel scc0Label;  //スケールのラベル
  public static JSlider scc0Slider;  //スケールを指定するスライダー

  public static final boolean SCC_FSX_MOUSE = true;  //true=SX-Windowをシームレスにする
  public static int sccFSXMouseHook;  //FSX.Xのマウス受信データ処理ルーチンのアドレス
  public static int sccFSXMouseWork;  //FSX.Xのマウスワークのアドレス

  //ポートA RS-232C
  public static final int SCC_1_INPUT_BITS = 12;
  public static final int SCC_1_INPUT_SIZE = 1 << SCC_1_INPUT_BITS;
  public static final int SCC_1_INPUT_MASK = SCC_1_INPUT_SIZE - 1;
  public static int scc1RegisterNumber;
  public static final int[] scc1InputBuffer = new int[SCC_1_INPUT_SIZE];  //RS-232C受信バッファ。データはゼロ拡張済み
  public static int scc1InputRead;  //RS-232C受信バッファから次に読み出すデータの位置。read==writeのときバッファエンプティ
  public static int scc1InputWrite;  //RS-232C受信バッファに次に書き込むデータの位置。(write+1&SCC_1_INPUT_MASK)==readのときバッファフル
  //  ボーレート
  public static int scc1ClockModeShift;  //WR4のbit6-7。0=2^0,1=2^4,2=2^5,3=2^6
  public static int scc1BaudRateGen;  //WR13<<8|WR12
  public static long scc1Interval;  //転送間隔(XEiJ.TMR_FREQ単位)
  public static long scc1InputClock;  //受信バッファの先頭のデータの受信予定時刻

  public static final TickerQueue.Ticker sccTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (SCC_DEBUG_TRACE && sccTraceOn) {
        System.out.printf ("%08x sccTicker.tick()\n", XEiJ.regPC0);
      }
      if (scc1aReceiveMask != 0) {
        scc1aReceiveRR3 = SCC_1A_RECEIVE_MASK;
        scc1aReceiveRequest = SCC_1A_RECEIVE_MASK;
        XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
      }
    }
  };

  //sccInit ()
  //  SCCを初期化する
  public static void sccInit () {

    if (SCC_DEBUG_TRACE) {
      sccTraceOn = true;
      sccTraceMenuItem = Multilingual.mlnText (
        XEiJ.createCheckBoxMenuItem (sccTraceOn, "SCC Trace", new ActionListener () {
          @Override public void actionPerformed (ActionEvent ae) {
            sccTraceOn = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
          }
        }),
        "ja", "SCC トレース");
    }

    //scc0ScaleIndex = SCC0_SCALE_MID;

    //ラベル
    //scc0Texts = new String[SCC0_SCALE_MAX - SCC0_SCALE_MIN + 1];
    for (int i = SCC0_SCALE_MIN; i <= SCC0_SCALE_MAX; i++) {
      scc0Texts[i - SCC0_SCALE_MIN] = String.format ("%4.2f", Math.pow (4.0, (double) (i - SCC0_SCALE_MID) / (double) SCC0_SCALE_MID));
    }
    scc0Label = XEiJ.createLabel (scc0Texts[SCC0_SCALE_MID]);
    //スライダー
    scc0Slider = XEiJ.setEnabled (
      XEiJ.setPreferredSize (
        XEiJ.createHorizontalSlider (SCC0_SCALE_MIN, SCC0_SCALE_MAX, scc0ScaleIndex, (SCC0_SCALE_MAX - SCC0_SCALE_MIN) / 4, 1, scc0Texts, new ChangeListener () {
          @Override public void stateChanged (ChangeEvent ce) {
            scc0SetScaleIndex (((JSlider) ce.getSource ()).getValue ());
          }
        }),
        224, 43),
      XEiJ.rbtRobot != null);

    //scc1InputBuffer = new int[SCC_1_INPUT_SIZE];

    sccReset ();

  }  //sccInit()

  //リセット
  public static void sccReset () {
    //割り込み
    sccInterruptVector = 0x00;
    sccVectorInclude = 0x00;
    scc0bReceiveMask = 0;
    scc0bReceiveRR3 = 0;
    scc0bReceiveRequest = 0;
    scc0bReceiveVector = 0;
    scc1aReceiveMask = 0;
    scc1aReceiveRR3 = 0;
    scc1aReceiveRequest = 0;
    scc1aReceiveVector = 0;
    scc1aSendMask = 0;
    scc1aSendRR3 = 0;
    scc1aSendRequest = 0;
    scc1aSendVector = 0;
    //マウス
    scc0RegisterNumber = 0;
    scc0Rts = 0;
    scc0BaudRateGen = 31;  //4800bps。(5000000/2/16)/4800-2=30.552。(5000000/2/16)/(31+2)=4734.848=4800*0.986
    scc0InputCounter = 0;
    scc0Data1 = 0;
    scc0Data2 = 0;
    //scc0ScaleIndex = SCC0_SCALE_MID;
    scc0SetScaleIndex (scc0ScaleIndex);
    if (SCC_FSX_MOUSE) {
      sccFSXMouseHook = 0;
      sccFSXMouseWork = 0;
    }
    //RS-232C
    scc1RegisterNumber = 0;
    Arrays.fill (scc1InputBuffer, 0);
    scc1InputRead = 0;
    scc1InputWrite = 0;
    //  ボーレート
    scc1BaudRateGen = 14;  //9600bps。(5000000/2/16)/9600-2=14.276。(5000000/2/16)/(14+2)=9765.625=9600*1.017
    scc1ClockModeShift = 1;  //1/16
    scc1Interval = XEiJ.TMR_FREQ / ((SCC_FREQ / 2 >> scc1ClockModeShift) / (scc1BaudRateGen + 2));
    scc1InputClock = XEiJ.FAR_FUTURE;
    TickerQueue.tkqRemove (sccTicker);
  }  //sccReset()

  public static void scc0SetScaleIndex (int i) {
    scc0ScaleIndex = i;
    scc0Label.setText (scc0Texts[i]);
    scc0UpdateRatio ();
  }  //scc0SetScaleIndex(int)

  public static void scc0UpdateRatio () {
    double scale = Math.pow (4.0, (double) (scc0ScaleIndex - SCC0_SCALE_MID) / (double) SCC0_SCALE_MID);
    if (XEiJ.musHostsPixelUnitsOn) {
      //scc0RatioX = (int) Math.round (65536.0 * scale * (double) XEiJ.pnlScreenWidth / (double) XEiJ.pnlZoomWidth);
      //scc0RatioY = (int) Math.round (65536.0 * scale * (double) XEiJ.pnlScreenHeight / (double) XEiJ.pnlZoomHeight);
      scc0RatioX = (int) (65536.0 * scale * (double) XEiJ.pnlScreenWidth / (double) XEiJ.pnlZoomWidth);
      scc0RatioY = (int) (65536.0 * scale * (double) XEiJ.pnlScreenHeight / (double) XEiJ.pnlZoomHeight);
    } else {
      //scc0RatioX = (int) Math.round (65536.0 * scale);
      //scc0RatioY = (int) Math.round (65536.0 * scale);
      scc0RatioX = (int) (65536.0 * scale);
      scc0RatioY = (int) (65536.0 * scale);
    }
  }  //scc0UpdateRatio()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int sccAcknowledge () {
    int d = 0;
    //優先順位は固定
    if (scc1aReceiveRequest != 0) {  //1A受信バッファフル(RS-232C受信)
      scc1aReceiveRequest = 0;
      d = scc1aReceiveVector;
    } else if (scc1aSendRequest != 0) {  //1A送信バッファ空(RS-232C送信)
      scc1aSendRequest = 0;
      d = scc1aSendVector;
    } else if (scc0bReceiveRequest != 0) {  //0B受信バッファフル(マウス受信)
      scc0bReceiveRequest = 0;
      d = scc0bReceiveVector;
    }
    if (SCC_DEBUG_TRACE && sccTraceOn) {
      System.out.printf ("%08x sccAcknowledge()=0x%02x\n", XEiJ.regPC0, d);
    }
    return d;
  }  //sccAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void sccDone () {
    if (SCC_DEBUG_TRACE && sccTraceOn) {
      System.out.printf ("%08x sccDone()\n", XEiJ.regPC0);
    }
    if ((scc1aReceiveRequest | scc1aSendRequest | scc0bReceiveRequest) != 0) {
      XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
    }
  }  //sccDone()

  //scc1Receive (d)
  //  RS-232C受信
  //  コアのスレッドで呼び出すこと
  public static void scc1Receive (int d) {
    int next = scc1InputWrite + 1 & SCC_1_INPUT_MASK;
    if (next != scc1InputRead) {  //バッファフルではない
      scc1InputBuffer[scc1InputWrite] = 0xff & d;  //ゼロ拡張してから書き込む
      if (scc1InputRead == scc1InputWrite) {  //バッファが空だったとき
        scc1InputClock = XEiJ.mpuClockTime;  //受信バッファの先頭のデータの受信予定時刻
        TickerQueue.tkqAdd (sccTicker, scc1InputClock);  //割り込みを発生させる
      }
      scc1InputWrite = next;
    }
  }  //scc1Receive(int)

  //sccUpdateVector ()
  //  scc0bReceiveVector,scc1aReceiveVector,scc1aSendVectorを更新する
  //  sccInterruptVector,sccVectorIncludeを更新したら呼び出す
  public static void sccUpdateVector () {
    if (sccVectorInclude == 0x00) {  //(WR9&0x01)==0x00
      scc0bReceiveVector = sccInterruptVector;
      scc1aReceiveVector = sccInterruptVector;
      scc1aSendVector    = sccInterruptVector;
    } else if (sccVectorInclude == 0x01) {  //(WR9&0x11)==0x01
      int t = sccInterruptVector & 0b11110001;
      scc0bReceiveVector = t | SCC_0B_RECEIVE_VECTOR << 1;
      scc1aReceiveVector = t | SCC_1A_RECEIVE_VECTOR << 1;
      scc1aSendVector    = t | SCC_1A_SEND_VECTOR << 1;
    } else {  //(WR9&0x11)==0x11
      int t = sccInterruptVector & 0b10001111;
      scc0bReceiveVector = t | SCC_0B_RECEIVE_VECTOR << 4;
      scc1aReceiveVector = t | SCC_1A_RECEIVE_VECTOR << 4;
      scc1aSendVector    = t | SCC_1A_SEND_VECTOR << 4;
    }
    if (SCC_DEBUG_TRACE) {
      System.out.printf ("scc0bReceiveVector=0x%02x\n", scc0bReceiveVector);
      System.out.printf ("scc1aReceiveVector=0x%02x\n", scc1aReceiveVector);
      System.out.printf ("scc1aSendVector=0x%02x\n", scc1aSendVector);
    }
  }  //sccUpdateVector()

}  //class Z8530



