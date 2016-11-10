//========================================================================================
//  MC68901.java
//    en:MFP -- Multi-Function Peripheral
//    ja:MFP -- マルチファンクションペリフェラル
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class MC68901 {

  public static final boolean MFP_DELAYED_INTERRUPT = true;  //true=MFPの割り込み要求を1命令遅延させる

  //レジスタ
  public static final int MFP_GPIP_DATA = 0x00e88001;  //GPIPデータレジスタ
  public static final int MFP_AER       = 0x00e88003;  //アクティブエッジレジスタ。各ビット0=1→0,1=0→1
  public static final int MFP_DDR       = 0x00e88005;  //データディレクションレジスタ。各ビット0=入力,1=出力。全ビット入力なので0x00に固定
  public static final int MFP_IERA      = 0x00e88007;  //割り込みイネーブルレジスタA。各ビット0=ディセーブル,1=イネーブル
  public static final int MFP_IERB      = 0x00e88009;  //割り込みイネーブルレジスタB。各ビット0=ディセーブル,1=イネーブル
  public static final int MFP_IPRA      = 0x00e8800b;  //割り込みペンディングレジスタA
  public static final int MFP_IPRB      = 0x00e8800d;  //割り込みペンディングレジスタB
  public static final int MFP_ISRA      = 0x00e8800f;  //割り込みインサービスレジスタA
  public static final int MFP_ISRB      = 0x00e88011;  //割り込みインサービスレジスタB
  public static final int MFP_IMRA      = 0x00e88013;  //割り込みマスクレジスタA
  public static final int MFP_IMRB      = 0x00e88015;  //割り込みマスクレジスタB
  public static final int MFP_VECTOR    = 0x00e88017;  //ベクタレジスタ
  public static final int MFP_TACR      = 0x00e88019;  //タイマAコントロールレジスタ
  public static final int MFP_TBCR      = 0x00e8801b;  //タイマBコントロールレジスタ
  public static final int MFP_TCDCR     = 0x00e8801d;  //タイマC,Dコントロールレジスタ
  //  タイマのカウンタに$00を書き込んで1/200プリスケールで開始したとき、カウンタから読み出される値は最初の50μs間は$00、次の50μs間は$FF
  public static final int MFP_TADR      = 0x00e8801f;  //タイマAデータレジスタ
  public static final int MFP_TBDR      = 0x00e88021;  //タイマBデータレジスタ
  public static final int MFP_TCDR      = 0x00e88023;  //タイマCデータレジスタ
  public static final int MFP_TDDR      = 0x00e88025;  //タイマDデータレジスタ
  public static final int MFP_SYNC_CHAR = 0x00e88027;  //同期キャラクタレジスタ
  public static final int MFP_UCR       = 0x00e88029;  //USARTコントロールレジスタ
  public static final int MFP_RSR       = 0x00e8802b;  //受信ステータスレジスタ
  public static final int MFP_TSR       = 0x00e8802d;  //送信ステータスレジスタ
  public static final int MFP_UDR       = 0x00e8802f;  //USARTデータレジスタ

  //GPIP
  //  GPIP7 H-SYNC
  //    1  水平帰線期間
  //    0  水平表示期間(水平バックポーチ／水平映像期間／水平フロントポーチ)
  //  GPIP6 CRTC IRQ
  //    0  指定されたラスタ
  //    1  その他のラスタ
  //    遷移は直前の水平フロントポーチの開始位置付近、V-DISPも遷移するときはその直前
  //    0番は垂直帰線期間の最初のラスタ
  //      CRTC R06+1==CRTC R09のとき、指定されたラスタの開始(CRTC IRQ 1→0)と垂直映像期間の開始(V-DISP 0→1)が同じラスタになる
  //  GPIP5
  //    RTCのCLKOUT(1Hz)が接続されることになっていたが欠番になった
  //  GPIP4 V-DISP
  //    1  垂直映像期間
  //    0  垂直フロントポーチ／垂直帰線期間／垂直バックポーチ
  //    遷移は直前の水平フロントポーチの開始位置付近
  public static final int MFP_GPIP_ALARM_LEVEL  = 0;  //0=ALARMによる電源ON
  public static final int MFP_GPIP_EXPWON_LEVEL = 1;  //0=EXPWONによる電源ON
  public static final int MFP_GPIP_POWER_LEVEL  = 2;  //0=POWERスイッチON
  public static final int MFP_GPIP_OPMIRQ_LEVEL = 3;  //0=OPM割り込み要求あり
  public static final int MFP_GPIP_VDISP_LEVEL  = 4;  //1=垂直映像期間,0=それ以外
  public static final int MFP_GPIP_RINT_LEVEL   = 6;  //0=指定されたラスタ,1=それ以外
  public static final int MFP_GPIP_HSYNC_LEVEL  = 7;  //0=水平表示期間,1=水平帰線期間

  //GPIPマスク
  public static final int MFP_GPIP_ALARM_MASK  = 1 << MFP_GPIP_ALARM_LEVEL;
  public static final int MFP_GPIP_EXPWON_MASK = 1 << MFP_GPIP_EXPWON_LEVEL;
  public static final int MFP_GPIP_POWER_MASK  = 1 << MFP_GPIP_POWER_LEVEL;
  public static final int MFP_GPIP_OPMIRQ_MASK = 1 << MFP_GPIP_OPMIRQ_LEVEL;
  public static final int MFP_GPIP_VDISP_MASK  = 1 << MFP_GPIP_VDISP_LEVEL;
  public static final int MFP_GPIP_RINT_MASK   = 1 << MFP_GPIP_RINT_LEVEL;
  public static final int MFP_GPIP_HSYNC_MASK  = 1 << MFP_GPIP_HSYNC_LEVEL;

  //割り込みレベル
  public static final int MFP_ALARM_LEVEL        =  0;  //40:MFP B0 GPIP0 RTC ALARM
  public static final int MFP_EXPWON_LEVEL       =  1;  //41:MFP B1 GPIP1 EXPWON
  public static final int MFP_POWER_LEVEL        =  2;  //42:MFP B2 GPIP2 POWER
  public static final int MFP_OPMIRQ_LEVEL       =  3;  //43:MFP B3 GPIP3 FM音源
  public static final int MFP_TIMER_D_LEVEL      =  4;  //44:MFP B4 Timer-D バックグラウンドスレッド
  public static final int MFP_TIMER_C_LEVEL      =  5;  //45:MFP B5 Timer-C マウス処理,テキストカーソル,FDDモーターOFF,稼働時間計測
  public static final int MFP_VDISP_LEVEL        =  6;  //46:MFP B6 GPIP4 V-DISP
  public static final int MFP_TIMER_B_LEVEL      =  8;  //48:MFP A0 Timer-B キーボードシリアルクロック(割り込み不可)
  public static final int MFP_OUTPUT_ERROR_LEVEL =  9;  //49:MFP A1 キーボードシリアル出力エラー
  public static final int MFP_OUTPUT_EMPTY_LEVEL = 10;  //4A:MFP A2 キーボードシリアル出力空
  public static final int MFP_INPUT_ERROR_LEVEL  = 11;  //4B:MFP A3 キーボードシリアル入力エラー
  public static final int MFP_INPUT_FULL_LEVEL   = 12;  //4C:MFP A4 キーボードシリアル入力あり
  public static final int MFP_TIMER_A_LEVEL      = 13;  //4D:MFP A5 Timer-A(V-DISPイベントカウント)
  public static final int MFP_RINT_LEVEL         = 14;  //4E:MFP A6 GPIP6 CRTC IRQ
  public static final int MFP_HSYNC_LEVEL        = 15;  //4F:MFP A7 GPIP7 H-SYNC

  //割り込みマスク
  public static final int MFP_ALARM_MASK        = 1 << MFP_ALARM_LEVEL;
  public static final int MFP_EXPWON_MASK       = 1 << MFP_EXPWON_LEVEL;
  public static final int MFP_POWER_MASK        = 1 << MFP_POWER_LEVEL;
  public static final int MFP_OPMIRQ_MASK       = 1 << MFP_OPMIRQ_LEVEL;
  public static final int MFP_TIMER_D_MASK      = 1 << MFP_TIMER_D_LEVEL;
  public static final int MFP_TIMER_C_MASK      = 1 << MFP_TIMER_C_LEVEL;
  public static final int MFP_VDISP_MASK        = 1 << MFP_VDISP_LEVEL;
  public static final int MFP_TIMER_B_MASK      = 1 << MFP_TIMER_B_LEVEL;
  public static final int MFP_OUTPUT_ERROR_MASK = 1 << MFP_OUTPUT_ERROR_LEVEL;
  public static final int MFP_OUTPUT_EMPTY_MASK = 1 << MFP_OUTPUT_EMPTY_LEVEL;
  public static final int MFP_INPUT_ERROR_MASK  = 1 << MFP_INPUT_ERROR_LEVEL;
  public static final int MFP_INPUT_FULL_MASK   = 1 << MFP_INPUT_FULL_LEVEL;
  public static final int MFP_TIMER_A_MASK      = 1 << MFP_TIMER_A_LEVEL;
  public static final int MFP_RINT_MASK         = 1 << MFP_RINT_LEVEL;
  public static final int MFP_HSYNC_MASK        = 1 << MFP_HSYNC_LEVEL;

  public static final long MFP_OSC_FREQ = 4000000L;  //MFPのオシレータの周波数

  //タイマのプリスケール
  public static final long MFP_DELTA[] = {
    XEiJ.FAR_FUTURE,                     //0:カウント禁止
    4 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,    //1:1/4プリスケール(1μs)
    10 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //2:1/10プリスケール(2.5μs)
    16 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //3:1/16プリスケール(4μs)
    50 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //4:1/50プリスケール(12.5μs)
    64 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //5:1/64プリスケール(16μs)
    100 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,  //6:1/100プリスケール(25μs)
    200 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,  //7:1/200プリスケール(50μs)
  };

  //MFP_UDRの入力データのキュー
  public static final int MFP_UDR_QUEUE_BITS = 4;  //キューの長さのビット数
  public static final int MFP_UDR_QUEUE_SIZE = 1 << MFP_UDR_QUEUE_BITS;  //キューの長さ
  public static final int MFP_UDR_QUEUE_MASK = MFP_UDR_QUEUE_SIZE - 1;  //キューの長さのマスク

  //GPIPデータレジスタ
  //  値は0または該当するビットのマスク
  //  ゼロ拡張
  public static int mfpGpipAlarm;  //0またはMFP_GPIP_ALARM_MASK
  public static int mfpGpipExpwon;  //0またはMFP_GPIP_EXPWON_MASK
  public static int mfpGpipPower;  //0またはMFP_GPIP_POWER_MASK
  public static int mfpGpipOpmirq;  //0またはMFP_GPIP_OPMIRQ_MASK
  public static int mfpGpipVdisp;  //0またはMFP_GPIP_VDISP_MASK
  public static int mfpGpipRint;  //0またはMFP_GPIP_RINT_MASK
  public static int mfpGpipHsync;  //0またはMFP_GPIP_HSYNC_MASK

  //レジスタ
  //  ゼロ拡張
  public static int mfpAer;  //アクティブエッジレジスタ
  public static int mfpIer;  //割り込みイネーブルレジスタ(上位バイトがMFP_IERA、下位バイトがMFP_IERB)
  public static int mfpImr;  //割り込みマスクレジスタ(上位バイトがMFP_IMRA、下位バイトがMFP_IMRB)
  public static int mfpVectorHigh;  //ベクタレジスタのビット7～4
  public static int mfpTaPrescale;  //タイマAのプリスケール(0～7、0はカウント禁止)
  public static int mfpTcPrescale;  //タイマCのプリスケール(0～7、0はカウント禁止)
  public static int mfpTdPrescale;  //タイマDのプリスケール(0～7、0はカウント禁止)
  public static boolean mfpTaEventcount;  //イベントカウントモード
  public static int mfpTaInitial;  //タイマAの初期値(1～256)
  public static int mfpTcInitial;  //タイマCの初期値(1～256)
  public static int mfpTdInitial;  //タイマDの初期値(1～256)
  public static int mfpTaCurrent;  //タイマAの現在の値(1～256、イベントカウントモードのときとディレイモードでカウンタが停止しているときだけ有効)
  public static int mfpTcCurrent;  //タイマCの現在の値(1～256、イベントカウントモードのときとディレイモードでカウンタが停止しているときだけ有効)
  public static int mfpTdCurrent;  //タイマDの現在の値(1～256、イベントカウントモードのときとディレイモードでカウンタが停止しているときだけ有効)

  //割り込み
  //  割り込み要求カウンタと割り込み受付カウンタの値が異なるときMFP_IPRA,MFP_IPRBの該当ビットがONになる
  //  MFP_IERA,MFP_IERBの該当ビットに0が書き込まれたときMFP_IPRA,MFP_IPRBの該当ビットを0にするためrequestをacknowledgedにコピーする
  public static final int[] mfpInnerRequest = new int[16];  //割り込み要求カウンタ
  public static final int[] mfpInnerAcknowledged = new int[16];  //割り込み受付カウンタ
  public static final boolean[] mfpInnerInService = new boolean[16];  //割り込み処理中のときtrue
  public static int mfpInnerLevel;  //割り込み処理中のレベル

  //タイマ
  public static long mfpTaStart;  //タイマAの初期値からスタートしたときのクロック
  public static long mfpTcStart;  //タイマCの初期値からスタートしたときのクロック
  public static long mfpTdStart;  //タイマDの初期値からスタートしたときのクロック
  public static long mfpTaDelta;  //タイマAのプリスケールに対応する1カウントあたりの時間
  public static long mfpTcDelta;  //タイマCのプリスケールに対応する1カウントあたりの時間
  public static long mfpTdDelta;  //タイマDのプリスケールに対応する1カウントあたりの時間
  public static long mfpTaClock;  //タイマAが次に割り込む時刻
  public static long mfpTcClock;  //タイマCが次に割り込む時刻
  public static long mfpTdClock;  //タイマDが次に割り込む時刻
  public static long mfpTcTdClock;  //Math.min(mfpTcClock,mfpTdClock)

  //MFP_UDRの入力データのキュー
  //  入力データはゼロ拡張すること
  //  キー入力を取りこぼさないためにキューを使う
  //  read==writeのときキューは空
  //  書き込むとread==writeになってしまうときは一杯なので書き込まない
  public static final int[] mfpUdrQueueArray = new int[MFP_UDR_QUEUE_SIZE];  //入力データ
  public static int mfpUdrQueueRead;  //最後に読み出したデータの位置
  public static int mfpUdrQueueWrite;  //最後に書き込んだデータの位置

  //mfpInit ()
  //  MFPを初期化する
  public static void mfpInit () {
    //mfpInnerRequest = new int[16];
    //mfpInnerAcknowledged = new int[16];
    //mfpInnerInService = new boolean[16];
    //mfpUdrQueueArray = new int[MFP_UDR_QUEUE_SIZE];
    for (int i = 0; i < MFP_UDR_QUEUE_SIZE; i++) {
      mfpUdrQueueArray[i] = 0;
    }
    mfpUdrQueueRead = 0;
    mfpUdrQueueWrite = 0;
    mfpReset ();
  }  //mfpInit()

  //リセット
  public static void mfpReset () {
    mfpGpipAlarm = 0;
    mfpGpipExpwon = MFP_GPIP_EXPWON_MASK;
    mfpGpipPower = 0;
    mfpGpipOpmirq = MFP_GPIP_OPMIRQ_MASK;
    mfpGpipVdisp = 0;
    mfpGpipRint = MFP_GPIP_RINT_MASK;
    mfpGpipHsync = 0;
    mfpAer = 0;
    mfpIer = 0;
    for (int i = 0; i < 16; i++) {
      mfpInnerRequest[i] = 0;
      mfpInnerAcknowledged[i] = 0;
      mfpInnerInService[i] = false;
    }
    mfpImr = 0;
    mfpVectorHigh = 0;
    mfpTaPrescale = 0;
    mfpTcPrescale = 0;
    mfpTdPrescale = 0;
    mfpTaEventcount = false;
    mfpTaInitial = 256;
    mfpTcInitial = 256;
    mfpTdInitial = 256;
    mfpTaCurrent = 0;
    mfpTcCurrent = 0;
    mfpTdCurrent = 0;
    mfpTaStart = 0L;
    mfpTcStart = 0L;
    mfpTdStart = 0L;
    mfpTaClock = XEiJ.FAR_FUTURE;
    mfpTcClock = XEiJ.FAR_FUTURE;
    mfpTdClock = XEiJ.FAR_FUTURE;
    mfpTcTdClock = XEiJ.FAR_FUTURE;
    if (MFP_KBD_ON) {
      //mfpKbdBuffer = new int[MFP_KBD_LIMIT];
      mfpKbdReadPointer = 0;
      mfpKbdWritePointer = 0;
      mfpKbdLastData = 0;
      mfpTkClock = XEiJ.FAR_FUTURE;
      mfpTaTkClock = XEiJ.FAR_FUTURE;
      //mfpTkTime = 0L;
    }
    TickerQueue.tkqRemove (mfpTaTicker);
    TickerQueue.tkqRemove (mfpTcTicker);
    TickerQueue.tkqRemove (mfpTdTicker);
    TickerQueue.tkqRemove (mfpTkTicker);
  }  //mfpReset()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  //
  //! 未対応
  //  スプリアス割り込みは発生しない
  //  実機ではデバイスからMFPへの割り込み要求とMPUからMFPへの割り込み禁止指示がほぼ同時に発生するとスプリアス割り込みが通知されることがある
  //    (1) デバイスがMFPに割り込みを要求する
  //    (2) MPUがMFPにデバイスの割り込みの禁止を指示する
  //    (3) MFPがデバイスの要求に従ってMPUに割り込みを要求する
  //    (4) MFPがMPUの指示に従ってデバイスの割り込みを禁止する
  //    (5) MPUがMFPの割り込み要求を受け付けてMFPに割り込みベクタの提出を指示する
  //    (6) MFPがMPUの指示に従って割り込みが許可されていて割り込みを要求しているデバイスを探すが見当たらないので応答しない
  //    (7) MPUがスプリアス割り込みを通知する
  //  ここではデバイスが見つからないとき割り込み要求を取り下げるのでスプリアス割り込みは発生しない
  public static int mfpAcknowledge () {
    for (int i = 15; i >= 0; i--) {
      if ((mfpImr & 1 << i) != 0) {
        int request = mfpInnerRequest[i];
        if (mfpInnerAcknowledged[i] != request) {
          mfpInnerAcknowledged[i] = request;
          mfpInnerInService[mfpInnerLevel = i] = true;
          return mfpVectorHigh + i;
        }
      }
    }
    return 0;
  }  //mfpAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void mfpDone () {
    mfpInnerInService[mfpInnerLevel] = false;
    for (int i = 15; i >= 0; i--) {
      if ((mfpImr & 1 << i) != 0 && mfpInnerAcknowledged[i] != mfpInnerRequest[i]) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
        return;
      }
    }
  }  //mfpDone()

  //mfpKeyboardInput (scanCode)
  //  キー入力
  //  コアのスレッドで呼び出すこと
  public static void mfpKeyboardInput (int scanCode) {
    scanCode &= 0xff;
    int write = mfpUdrQueueWrite + 1 & MFP_UDR_QUEUE_MASK;
    if (mfpUdrQueueRead != write) {  //キューに書き込めるとき
      mfpUdrQueueWrite = write;
      mfpUdrQueueArray[mfpUdrQueueWrite] = scanCode;
      if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
        mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
        if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpKeyboardInput


  //Timer-A
  public static final TickerQueue.Ticker mfpTaTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
        if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTaClock += mfpTaDelta * mfpTaInitial;
      TickerQueue.tkqAdd (mfpTaTicker, mfpTaClock);
    }
  };

  //Timer-C
  public static final TickerQueue.Ticker mfpTcTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_C_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_C_LEVEL]++;
        if ((mfpImr & MFP_TIMER_C_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTcClock += mfpTcDelta * mfpTcInitial;
      TickerQueue.tkqAdd (mfpTcTicker, mfpTcClock);
    }
  };

  //Timer-D
  public static final TickerQueue.Ticker mfpTdTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_D_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_D_LEVEL]++;
        if ((mfpImr & MFP_TIMER_D_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTdClock += mfpTdDelta * mfpTdInitial;
      TickerQueue.tkqAdd (mfpTdTicker, mfpTdClock);
    }
  };

  //キーボード
  public static final TickerQueue.Ticker mfpTkTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MFP_KBD_ON) {
        //  XEiJ.mpuClockTimeだけで割り込みのタイミングを決めると、
        //  コアのタスクが詰まっているときキー入力割り込みも詰まってリピートの開始と間隔が短くなってしまう
        long time = System.currentTimeMillis () - 10L;  //10msまでは早すぎてもよいことにする
        if (time < mfpTkTime) {  //早すぎる
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * (mfpTkTime - time);
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
        } else {
          if (mfpKbdReadPointer != mfpKbdWritePointer) {  //バッファが空でないとき
            if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
              mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
              if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
                //MFPのキー入力割り込みを要求する
                if (MFP_DELAYED_INTERRUPT) {
                  XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                } else {
                  XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                }
              }
            }
          }
          mfpTkClock = XEiJ.FAR_FUTURE;
          TickerQueue.tkqRemove (mfpTkTicker);
          //mfpTkTime = 0L;
        }
      }
    }  //tick()
  };


  //GPIP入力
  //  デバイスが呼び出す
  //GPIP0
  public static void mfpAlarmRise () {
    if (mfpGpipAlarm == 0) {  //0→1
      mfpGpipAlarm = MFP_GPIP_ALARM_MASK;
      if ((mfpAer & MFP_GPIP_ALARM_MASK) != 0 && (mfpIer & MFP_ALARM_MASK) != 0) {
        mfpInnerRequest[MFP_ALARM_LEVEL]++;
        if ((mfpImr & MFP_ALARM_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpAlarmRise()
  public static void mfpAlarmFall () {
    if (mfpGpipAlarm != 0) {  //1→0
      mfpGpipAlarm = 0;
      if ((mfpAer & MFP_GPIP_ALARM_MASK) == 0 && (mfpIer & MFP_ALARM_MASK) != 0) {
        mfpInnerRequest[MFP_ALARM_LEVEL]++;
        if ((mfpImr & MFP_ALARM_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpAlarmFall()

  //GPIP1
  public static void mfpExpwonRise () {
    if (mfpGpipExpwon == 0) {  //0→1
      mfpGpipExpwon = MFP_GPIP_EXPWON_MASK;
      if ((mfpAer & MFP_GPIP_EXPWON_MASK) != 0 && (mfpIer & MFP_EXPWON_MASK) != 0) {
        mfpInnerRequest[MFP_EXPWON_LEVEL]++;
        if ((mfpImr & MFP_EXPWON_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpExpwonRise()
  public static void mfpExpwonFall () {
    if (mfpGpipExpwon != 0) {  //1→0
      mfpGpipExpwon = 0;
      if ((mfpAer & MFP_GPIP_EXPWON_MASK) == 0 && (mfpIer & MFP_EXPWON_MASK) != 0) {
        mfpInnerRequest[MFP_EXPWON_LEVEL]++;
        if ((mfpImr & MFP_EXPWON_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpExpwonFall()

  //GPIP2
  public static void mfpPowerRise () {
    if (mfpGpipPower == 0) {  //0→1
      mfpGpipPower = MFP_GPIP_POWER_MASK;
      if ((mfpAer & MFP_GPIP_POWER_MASK) != 0 && (mfpIer & MFP_POWER_MASK) != 0) {
        mfpInnerRequest[MFP_POWER_LEVEL]++;
        if ((mfpImr & MFP_POWER_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpPowerRise()
  public static void mfpPowerFall () {
    if (mfpGpipPower != 0) {  //1→0
      mfpGpipPower = 0;
      if ((mfpAer & MFP_GPIP_POWER_MASK) == 0 && (mfpIer & MFP_POWER_MASK) != 0) {
        mfpInnerRequest[MFP_POWER_LEVEL]++;
        if ((mfpImr & MFP_POWER_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpPowerFall()

  //GPIP3
  public static void mfpOpmirqRise () {
    if (mfpGpipOpmirq == 0) {  //0→1
      mfpGpipOpmirq = MFP_GPIP_OPMIRQ_MASK;
      if ((mfpAer & MFP_GPIP_OPMIRQ_MASK) != 0 && (mfpIer & MFP_OPMIRQ_MASK) != 0) {
        mfpInnerRequest[MFP_OPMIRQ_LEVEL]++;
        if ((mfpImr & MFP_OPMIRQ_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpOpmirqRise()
  public static void mfpOpmirqFall () {
    if (mfpGpipOpmirq != 0) {  //1→0
      mfpGpipOpmirq = 0;
      if ((mfpAer & MFP_GPIP_OPMIRQ_MASK) == 0 && (mfpIer & MFP_OPMIRQ_MASK) != 0) {
        mfpInnerRequest[MFP_OPMIRQ_LEVEL]++;
        if ((mfpImr & MFP_OPMIRQ_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpOpmirqFall()

  //GPIP4
  public static void mfpVdispRise () {
    //if (mfpGpipVdisp == 0) {  //0→1
    mfpGpipVdisp = MFP_GPIP_VDISP_MASK;
    if ((mfpAer & MFP_GPIP_VDISP_MASK) != 0) {
      if ((mfpIer & MFP_VDISP_MASK) != 0) {
        mfpInnerRequest[MFP_VDISP_LEVEL]++;
        if ((mfpImr & MFP_VDISP_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      if (mfpTaEventcount && --mfpTaCurrent <= 0) {
        mfpTaCurrent = mfpTaInitial;
        if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
          mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
          if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      }
    }
    //}
  }  //mfpVdispRise()
  public static void mfpVdispFall () {
    //if (mfpGpipVdisp != 0) {  //1→0
    mfpGpipVdisp = 0;
    if ((mfpAer & MFP_GPIP_VDISP_MASK) == 0) {
      if ((mfpIer & MFP_VDISP_MASK) != 0) {
        mfpInnerRequest[MFP_VDISP_LEVEL]++;
        if ((mfpImr & MFP_VDISP_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      if (mfpTaEventcount && --mfpTaCurrent <= 0) {
        mfpTaCurrent = mfpTaInitial;
        if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
          mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
          if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      }
    }
    //}
  }  //mfpVdispFall()

  //GPIP6
  public static void mfpRintRise () {
    //if (mfpGpipRint == 0) {  //0→1
    mfpGpipRint = MFP_GPIP_RINT_MASK;
    if ((mfpAer & MFP_GPIP_RINT_MASK) != 0 && (mfpIer & MFP_RINT_MASK) != 0) {
      mfpInnerRequest[MFP_RINT_LEVEL]++;
      if ((mfpImr & MFP_RINT_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpRintRise()
  public static void mfpRintFall () {
    //if (mfpGpipRint != 0) {  //1→0
    mfpGpipRint = 0;
    if ((mfpAer & MFP_GPIP_RINT_MASK) == 0 && (mfpIer & MFP_RINT_MASK) != 0) {
      mfpInnerRequest[MFP_RINT_LEVEL]++;
      if ((mfpImr & MFP_RINT_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpRintFall()

  //GPIP7
  public static void mfpHsyncRise () {
    //if (mfpGpipHsync == 0) {  //0→1
    mfpGpipHsync = MFP_GPIP_HSYNC_MASK;
    if ((mfpAer & MFP_GPIP_HSYNC_MASK) != 0 && (mfpIer & MFP_HSYNC_MASK) != 0) {
      mfpInnerRequest[MFP_HSYNC_LEVEL]++;
      if ((mfpImr & MFP_HSYNC_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpHsyncRise()
  public static void mfpHsyncFall () {
    //if (mfpGpipHsync != 0) {  //1→0
    mfpGpipHsync = 0;
    if ((mfpAer & MFP_GPIP_HSYNC_MASK) == 0 && (mfpIer & MFP_HSYNC_MASK) != 0) {
      mfpInnerRequest[MFP_HSYNC_LEVEL]++;
      if ((mfpImr & MFP_HSYNC_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpHsyncFall()

  //キー入力のリピートの処理をMFPで行う
  //  キー入力バッファ
  //    キー入力データが並んでいるリングバッファ
  //    読み出しポインタ
  //      次に読み出す位置
  //      MFPが更新する
  //    書き込みポインタ
  //      次に書き込む位置
  //      KBDが更新する
  //    読み出しポインタと書き込みポインタが一致しているときバッファは空
  //    読み出しポインタを進めると書き込みポインタと一致するとき読み出しポインタはバッファの末尾
  //    書き込みポインタを進めると読み出しポインタと一致するときはバッファが一杯(なので書き込めない)
  //  キー入力データ
  //    1データにつきintを2個用いる
  //    data  0x00+キーコード  キーが押されたデータ
  //          0x80+キーコード  キーが離されたデータ
  //    repeat  -1  リピート開始後のデータ
  //                リピートするキーが押されて1回以上読み出されたデータ
  //             0  リピートしないデータ
  //                リピートしないキーが押されたかキーが離されたときのデータ
  //             1  リピート開始前のデータ
  //                リピートするキーが押されてまだ読み出されていないデータ
  //  最後に読み出したデータ
  //    キー入力バッファから最後に読み出したデータ
  //    キー入力バッファが空のときUDRから読み出される
  //  キーが押されたまたは離されたとき
  //    バッファが一杯でないとき
  //      キーが押されたとき
  //        書き込みポインタの位置にリピート開始前のデータを書き込む
  //      キーが離されたとき
  //        書き込みポインタの位置にリピートしないデータを書き込む
  //      書き込みポインタを進める
  //    MFPのキー入力割り込みを要求する
  //  UDR読み出し
  //    バッファが空でないとき
  //      バッファの末尾でなくて先頭がリピート開始後のデータのとき
  //        読み出しポインタを進める
  //      読み出しポインタの位置からデータを読み出して最後に読み出したデータとして保存する
  //      バッファの末尾でないとき
  //        読み出しポインタを進める
  //        MFPのキー入力割り込みを要求する
  //      バッファの末尾でリピートしないデータのとき
  //        読み出しポインタを進める
  //      バッファの末尾でリピート開始前のデータのとき
  //        読み出しポインタの位置のデータをリピート開始後に変更する
  //        現在時刻+リピート開始時間でMFPのTimer-Kをセットする
  //      バッファの末尾でリピート開始後のデータのとき
  //        現在時刻+リピート間隔時間でMFPのTimer-Kをセットする
  //    最後に読み出したデータを返す
  //  MFPのTimer-K
  //    キー入力のリピート処理のためにMFPに追加されたタイマー
  //    バッファが空でないとき
  //      MFPのキー入力割り込みを要求する
  //  UDR読み出しとMFPのTimer-Kはどちらもコアから呼ばれるので同時に呼び出されることはない
  //  キー入力割り込みがIERAで禁止されているとタイマーで割り込みがかからないのでリピートが止まってしまうが、
  //  キー入力割り込みを止めたいときはIMRAでマスクするのが原則なので通常は問題ないはず
  public static final boolean MFP_KBD_ON = true;  //true=キー入力のリピートの処理をMFPで行う
  public static final int MFP_KBD_LIMIT = 512;  //キー入力バッファのサイズ。2の累乗にすること
  public static final int[] mfpKbdBuffer = new int[MFP_KBD_LIMIT];  //キー入力バッファ
  public static int mfpKbdReadPointer;  //読み出しポインタ
  public static int mfpKbdWritePointer;  //書き込みポインタ
  public static int mfpKbdLastData;  //最後に読み出したデータ
  public static long mfpTkClock;  //Timer-Kの次の呼び出し時刻
  public static long mfpTaTkClock;  //Math.min(mfpTaClock,mfpTkClock)
  public static long mfpTkTime;  //次にTimer-Kが呼び出されるべき時刻(ms)。mfpTkClockがXEiJ.FAR_FUTUREでないときだけ有効

  //mfpKbdInput (data, repeat)
  //  キーが押されたまたは離されたとき
  //  data  キーコード
  //  repeat  リピートの有無。false=リピートしない,true=リピートする
  public static void mfpKbdInput (int data, boolean repeat) {
    int w = mfpKbdWritePointer;
    int w1 = w + 2 & MFP_KBD_LIMIT - 2;
    if (w1 != mfpKbdReadPointer) {  //バッファが一杯でないとき
      mfpKbdBuffer[w] = data;  //書き込みポインタの位置にデータを書き込む
      mfpKbdBuffer[w + 1] = repeat ? 1 : 0;  //0=リピートしないデータ,1=リピート開始前のデータ
      mfpKbdWritePointer = w1;  //書き込みポインタを進める
      if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
        mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
        if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
          //MFPのキー入力割り込みを要求する
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpKbdInput(int,boolean)

  //data = mfpKbdReadData ()
  //  UDR読み出し
  public static int mfpKbdReadData () {
    int r = mfpKbdReadPointer;
    int w = mfpKbdWritePointer;
    if (r != w) {  //バッファが空でないとき
      int r1 = r + 2 & MFP_KBD_LIMIT - 2;
      int s = mfpKbdBuffer[r + 1];  //-1=リピート開始後,0=リピートしない,1=リピート開始前
      if (r1 != w && s < 0) {  //バッファの末尾でなくて先頭がリピート開始後のデータのとき
        mfpKbdReadPointer = r = r1;  //読み出しポインタを進める
        r1 = r + 2 & MFP_KBD_LIMIT - 2;
        s = mfpKbdBuffer[r + 1];
      }
      mfpKbdLastData = mfpKbdBuffer[r];  //読み出しポインタの位置からデータを読み出して最後に読み出したデータとして保存する
      if (r1 != w) {  //バッファの末尾でないとき
        mfpKbdReadPointer = r1;  //読み出しポインタを進める
        if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
          mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
          if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
            //MFPのキー入力割り込みを要求する
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      } else if (s == 0) {  //バッファの末尾でリピートしないデータのとき
        mfpKbdReadPointer = r1;  //読み出しポインタを進める
      } else if (s > 0) {  //バッファの末尾でリピート開始前のデータのとき
        mfpKbdBuffer[r + 1] = -1;  //読み出しポインタの位置のデータをリピート開始後に変更する
        if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {  //キー入力割り込みが許可されているとき
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * Keyboard.kbdRepeatDelay;  //現在時刻+リピート開始時間でMFPのTimer-Kをセットする
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
          mfpTkTime = System.currentTimeMillis () + Keyboard.kbdRepeatDelay;  //次にTimer-Kが呼び出されるべき時刻(ms)
        }
      } else {  //バッファの末尾でリピート開始後のデータのとき
        if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {  //キー入力割り込みが許可されているとき
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * Keyboard.kbdRepeatInterval;  //現在時刻+リピート間隔時間でMFPのTimer-Kをセットする
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
          mfpTkTime = System.currentTimeMillis () + Keyboard.kbdRepeatInterval;  //次にTimer-Kが呼び出されるべき時刻(ms)
        }
      }
    }
    return mfpKbdLastData;  //最後に読み出したデータを返す
  }  //mfpKbdReadData()

}  //class MC68901



