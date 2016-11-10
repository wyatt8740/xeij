//========================================================================================
//  HD63450.java
//    en:DMA controller
//    ja:DMAコントローラ
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class HD63450 {

  //フラグ
  //                                          3210
  public static final int DMA_DEBUG_TRACE = 0b0000;  //トレースするチャンネルをセット

  //レジスタ
  //  DMA_CERはread-only、その他はread/write
  //  DMA_DCR,DMA_SCR,DMA_MTC,DMA_MAR,DMA_DAR,DMA_MFC,DMA_DFCに動作中(dmaACT[i]!=0)に書き込むとDMA_TIMING_ERRORになる

  //Channel Status Register (R/W)
  public static final int DMA_CSR = 0x00;
  public static final int DMA_COC = 0b10000000;  //Channel Operation Complete。1=チャンネル動作完了
  public static final int DMA_BLC = 0b01000000;  //BLock transfer Complete。1=ブロック転送完了
  public static final int DMA_NDT = 0b00100000;  //Normal Device Termination。1=正常終了
  public static final int DMA_ERR = 0b00010000;  //ERRor。1=エラーあり
  public static final int DMA_ACT = 0b00001000;  //channel ACTive。1=チャンネル動作中
  public static final int DMA_DIT = 0b00000100;  //! 非対応。~DONE Input Transition。1=~DONE入力があった
  public static final int DMA_PCT = 0b00000010;  //~PCL Transition。1=~PCLの立下りがあった
  public static final int DMA_PCS = 0b00000001;  //~PCL Status。~PCLの状態

  //Channel Error Register (R)
  public static final int DMA_CER = 0x01;
  public static final int DMA_ERROR_CODE           = 0b00011111;  //エラーコード
  public static final int DMA_NO_ERROR             = 0b00000000;  //  エラーなし
  public static final int DMA_CONFIGURATION_ERROR  = 0b00000001;  //  コンフィギュレーションエラー
  public static final int DMA_TIMING_ERROR         = 0b00000010;  //  動作タイミングエラー
  public static final int DMA_MEMORY_ADDRESS_ERROR = 0b00000101;  //  アドレスエラー(メモリアドレス)
  public static final int DMA_DEVICE_ADDRESS_ERROR = 0b00000110;  //  アドレスエラー(デバイスアドレス)
  public static final int DMA_BASE_ADDRESS_ERROR   = 0b00000111;  //  アドレスエラー(ベースアドレス)
  public static final int DMA_MEMORY_BUS_ERROR     = 0b00001001;  //  バスエラー(メモリアドレス)
  public static final int DMA_DEVICE_BUS_ERROR     = 0b00001010;  //  バスエラー(デバイスアドレス)
  public static final int DMA_BASE_BUS_ERROR       = 0b00001011;  //  バスエラー(ベースアドレス)
  public static final int DMA_MEMORY_COUNT_ERROR   = 0b00001101;  //  カウントエラー(メモリカウンタ)
  public static final int DMA_BASE_COUNT_ERROR     = 0b00001111;  //  カウントエラー(ベースカウンタ)
  public static final int DMA_EXTERNAL_ABORT       = 0b00010000;  //! 非対応。外部強制停止
  public static final int DMA_SOFTWARE_ABORT       = 0b00010001;  //  ソフトウェア強制停止

  //Device Control Register (R/W)
  public static final int DMA_DCR = 0x04;
  public static final int DMA_XRM                    = 0b11000000;  //eXternal Request Mode
  public static final int DMA_BURST_TRANSFER         = 0b00000000;  //  バースト転送モード
  public static final int DMA_NO_HOLD_CYCLE_STEAL    = 0b10000000;  //  ホールドなしサイクルスチールモード
  public static final int DMA_HOLD_CYCLE_STEAL       = 0b11000000;  //! 非対応。ホールドありサイクルスチールモード。ホールドなしサイクルスチールモードと同じ
  public static final int DMA_DTYP                   = 0b00110000;  //Device TYPe
  public static final int DMA_HD68000_COMPATIBLE     = 0b00000000;  //  HD68000コンパチブル(デュアルアドレスモード)
  public static final int DMA_HD6800_COMPATIBLE      = 0b00010000;  //! 非対応。HD6800コンパチブル(デュアルアドレスモード)
  public static final int DMA_ACK_DEVICE             = 0b00100000;  //! 非対応。~ACK付きデバイス(シングルアドレスモード)
  public static final int DMA_ACK_READY_DEVICE       = 0b00110000;  //! 非対応。~ACK,~READY付きデバイス(シングルアドレスモード)
  public static final int DMA_DPS                    = 0b00001000;  //Device Port Size
  public static final int DMA_PORT_8_BIT             = 0b00000000;  //  8ビットポート
  public static final int DMA_PORT_16_BIT            = 0b00001000;  //  16ビットポート
  public static final int DMA_PCL                    = 0b00000011;  //Peripheral Control Line
  public static final int DMA_STATUS_INPUT           = 0b00000000;  //  STATUS入力
  public static final int DMA_STATUS_INPUT_INTERRUPT = 0b00000001;  //! 非対応。割り込みありSTATUS入力
  public static final int DMA_EIGHTH_START_PULSE     = 0b00000010;  //! 非対応。1/8スタートパルス
  public static final int DMA_ABORT_INPUT            = 0b00000011;  //! 非対応。ABORT入力

  //Operation Control Register (R/W)
  public static final int DMA_OCR = 0x05;
  public static final int DMA_DIR                 = 0b10000000;  //DIRection
  public static final int DMA_MEMORY_TO_DEVICE    = 0b00000000;  //  メモリ→デバイス。DMA_MAR→DMA_DAR
  public static final int DMA_DEVICE_TO_MEMORY    = 0b10000000;  //  デバイス→メモリ。DMA_DAR→DMA_MAR
  public static final int DMA_BTD                 = 0b01000000;  //! 非対応。multi Block Transfer with ~DONE mode
  public static final int DMA_SIZE                = 0b00110000;  //operand SIZE
  public static final int DMA_BYTE_SIZE           = 0b00000000;  //  8ビット
  public static final int DMA_WORD_SIZE           = 0b00010000;  //  16ビット
  public static final int DMA_LONG_WORD_SIZE      = 0b00100000;  //  32ビット
  public static final int DMA_UNPACKED_8_BIT      = 0b00110000;  //  パックなし8ビット
  public static final int DMA_CHAIN               = 0b00001100;  //CHAINing operation
  public static final int DMA_NO_CHAINING         = 0b00000000;  //  チェインなし
  public static final int DMA_ARRAY_CHAINING      = 0b00001000;  //  アレイチェイン
  public static final int DMA_LINK_ARRAY_CHAINING = 0b00001100;  //  リンクアレイチェイン
  public static final int DMA_REQG                = 0b00000011;  //DMA REQuest Generation method
  public static final int DMA_AUTO_REQUEST        = 0b00000000;  //  オートリクエスト限定速度。転送中にバスを開放する
  public static final int DMA_AUTO_REQUEST_MAX    = 0b00000001;  //  オートリクエスト最大速度。転送中にバスを開放しない
  public static final int DMA_EXTERNAL_REQUEST    = 0b00000010;  //  外部転送要求
  public static final int DMA_DUAL_REQUEST        = 0b00000011;  //  最初はオートリクエスト、2番目から外部転送要求

  //Sequence Control Register (R/W)
  public static final int DMA_SCR = 0x06;
  public static final int DMA_MAC =        0b00001100;  //Memory Address register Count
  public static final int DMA_STATIC_MAR = 0b00000000;  //  DMA_MAR固定
  public static final int DMA_INC_MAR    = 0b00000100;  //  DMA_MAR++
  public static final int DMA_DEC_MAR    = 0b00001000;  //  DMA_MAR--
  public static final int DMA_DAC =        0b00000011;  //Device Address register Count
  public static final int DMA_STATIC_DAR = 0b00000000;  //  DMA_DAR固定
  public static final int DMA_INC_DAR    = 0b00000001;  //  DMA_DAR++
  public static final int DMA_DEC_DAR    = 0b00000010;  //  DMA_DAR--

  //Channel Control Register (R/W)
  public static final int DMA_CCR = 0x07;
  public static final int DMA_STR = 0b10000000;  //STaRt operation。1=動作開始
  public static final int DMA_CNT = 0b01000000;  //CoNTinue operation。1=コンティニューあり
  public static final int DMA_HLT = 0b00100000;  //Halt operation。1=動作一時停止
  public static final int DMA_SAB = 0b00010000;  //Software ABort。1=動作中止
  public static final int DMA_ITE = 0b00001000;  //InTerrupt Enable。1=割り込み許可

  //Transfer Counter, Address Register
  public static final int DMA_MTC = 0x0a;  //Memory Transfer Counter (R/W)
  public static final int DMA_MAR = 0x0c;  //Memory Address Register (R/W)
  public static final int DMA_DAR = 0x14;  //Device Address Register (R/W)
  public static final int DMA_BTC = 0x1a;  //Base Transfer Counter (R/W)
  public static final int DMA_BAR = 0x1c;  //Base Address Register (R/W)

  //Interrupt Vector
  public static final int DMA_NIV = 0x25;  //Normal Interrupt Vector (R/W)
  public static final int DMA_EIV = 0x27;  //Error Interrupt Vector (R/W)

  //Function Codes
  public static final int DMA_MFC = 0x29;  //Memory Function Codes (R/W)
  public static final int DMA_FC2 = 0b00000100;  //Function Code 2
  public static final int DMA_FC1 = 0b00000010;  //! 非対応。Function Code 1
  public static final int DMA_FC0 = 0b00000001;  //! 非対応。Function Code 0

  //Channel Priority Register (R/W)
  public static final int DMA_CPR = 0x2d;
  public static final int DMA_CP = 0b00000011;  //! 未対応。Channel Priority。0=高,1,2,3=低

  //Function Codes
  public static final int DMA_DFC = 0x31;  //Device Function Codes (R/W)
  public static final int DMA_BFC = 0x39;  //Base Function Codes (R/W)

  //General Control Register (R/W)
  public static final int DMA_GCR = 0xff;
  public static final int DMA_BT = 0b00001100;  //Burst Time。0=16clk,1=32clk,2=64clk,3=128clk
  public static final int DMA_BR = 0b00000011;  //Bandwidth Ratio。0=1/2,1=1/4,2=1/8,3=1/16

  //レジスタ
  //  すべてゼロ拡張
  public static final int[] dmaPCS = new int[4];         //DMA_CSR bit0
  public static final int[] dmaPCT = new int[4];         //        bit1
  public static final int[] dmaDIT = new int[4];         //        bit2
  public static final int[] dmaACT = new int[4];         //        bit3
  public static final int[] dmaERR = new int[4];         //        bit4
  public static final int[] dmaNDT = new int[4];         //        bit5
  public static final int[] dmaBLC = new int[4];         //        bit6
  public static final int[] dmaCOC = new int[4];         //        bit7
  public static final int[] dmaErrorCode = new int[4];   //DMA_CER bit0-4
  public static final int[] dmaPCL = new int[4];         //DMA_DCR bit0-1
  public static final int[] dmaDPS = new int[4];         //        bit3
  public static final int[] dmaDTYP = new int[4];        //        bit4-5
  public static final int[] dmaXRM = new int[4];         //        bit6-7
  public static final int[] dmaREQG = new int[4];        //DMA_OCR bit0-1
  public static final int[] dmaCHAIN = new int[4];       //        bit2-3
  public static final int[] dmaSIZE = new int[4];        //        bit4-5
  public static final int[] dmaBTD = new int[4];         //        bit6
  public static final int[] dmaDIR = new int[4];         //        bit7
  public static final int[] dmaDAC = new int[4];         //DMA_SCR bit0-1
  public static final int[] dmaDACValue = new int[4];    //           dmaDAC==DMA_INC_DAR?1:dmaDAC==DMA_DEC_DAR?-1:0
  public static final int[] dmaMAC = new int[4];         //        bit2-3
  public static final int[] dmaMACValue = new int[4];    //           dmaMAC==DMA_INC_MAR?1:dmaMAC==DMA_DEC_MAR?-1:0
  public static final int[] dmaITE = new int[4];         //DMA_CCR bit3
  public static final int[] dmaSAB = new int[4];         //        bit4
  public static final int[] dmaHLT = new int[4];         //        bit5
  public static final int[] dmaCNT = new int[4];         //        bit6
  public static final int[] dmaSTR = new int[4];         //        bit7
  public static final int[] dmaMTC = new int[4];         //DMA_MTC bit0-15
  public static final int[] dmaMAR = new int[4];         //DMA_MAR bit0-31
  public static final int[] dmaDAR = new int[4];         //DMA_DAR bit0-31
  public static final int[] dmaBTC = new int[4];         //DMA_BTC bit0-15
  public static final int[] dmaBAR = new int[4];         //DMA_BAR bit0-31
  public static final int[] dmaNIV = new int[4];         //DMA_NIV bit0-7
  public static final int[] dmaEIV = new int[4];         //DMA_EIV bit0-7
  public static final int[] dmaMFC = new int[4];         //DMA_MFC bit2
  public static final MemoryMappedDevice[][] dmaMFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaMFC[i]==0?udm:sdm:dmaMFC[i]==0?um:sm
  public static final int[] dmaCP = new int[4];          //DMA_CPR bit0-1
  public static final int[] dmaDFC = new int[4];         //DMA_DFC bit2
  public static final MemoryMappedDevice[][] dmaDFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaDFC[i]==0?udm:sdm:dmaDFC[i]==0?um:sm
  public static final int[] dmaBFC = new int[4];         //DMA_BFC bit2
  public static final MemoryMappedDevice[][] dmaBFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaBFC[i]==0?udm:sdm:dmaBFC[i]==0?um:sm
  public static int dmaBR;                               //DMA_GCR bit0-1。0=1/2,1=1/4,2=1/8,3=1/16
  public static int dmaBT;                               //        bit2-3。0=16clk,1=32clk,2=64clk,3=128clk
  public static long dmaBurstInterval;  //バースト間隔。DMA_CLOCK_UNIT<<4+(dmaBT>>2)
  public static long dmaBurstSpan;  //バースト期間。dmaBurstInterval>>1+(dmaBR&3)
  public static long dmaBurstStart;  //バースト開始時刻
  public static long dmaBurstEnd;  //バースト終了時刻

  //割り込み
  public static final int[] dmaInnerRequest = new int[8];  //割り込み要求カウンタ
  public static final int[] dmaInnerAcknowleged = new int[8];  //割り込み受付カウンタ

  //クロック
  //  1回のバスアクセスに10MHz固定で1clkかかるものとする
  //  リードとライトで最短2clkかかるので最大転送速度は5Mワード/s
  public static final long DMA_CLOCK_UNIT = XEiJ.TMR_FREQ / (1024 * 1024 * 10);  //DMAのバスアクセス1回あたりの所要時間(XEiJ.TMR_FREQ単位)
  public static final long[] dmaInnerClock = new long[4];  //転送要求時刻(XEiJ.TMR_FREQ単位)

  public static final TickerQueue.Ticker[] dmaTickerArray = new TickerQueue.Ticker[] {
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (0);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (1);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (2);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (3);
      }
    },
  };

  //dmaInit ()
  //  DMAコントローラを初期化する
  public static void dmaInit () {
    //レジスタ
    //dmaPCS = new int[4];
    //dmaPCT = new int[4];
    //dmaDIT = new int[4];
    //dmaACT = new int[4];
    //dmaERR = new int[4];
    //dmaNDT = new int[4];
    //dmaBLC = new int[4];
    //dmaCOC = new int[4];
    //dmaErrorCode = new int[4];
    //dmaPCL = new int[4];
    //dmaDPS = new int[4];
    //dmaDTYP = new int[4];
    //dmaXRM = new int[4];
    //dmaREQG = new int[4];
    //dmaCHAIN = new int[4];
    //dmaSIZE = new int[4];
    //dmaBTD = new int[4];
    //dmaDIR = new int[4];
    //dmaDAC = new int[4];
    //dmaDACValue = new int[4];
    //dmaMAC = new int[4];
    //dmaMACValue = new int[4];
    //dmaITE = new int[4];
    //dmaSAB = new int[4];
    //dmaHLT = new int[4];
    //dmaCNT = new int[4];
    //dmaSTR = new int[4];
    //dmaMTC = new int[4];
    //dmaMAR = new int[4];
    //dmaDAR = new int[4];
    //dmaBTC = new int[4];
    //dmaBAR = new int[4];
    //dmaNIV = new int[4];
    //dmaEIV = new int[4];
    //dmaMFC = new int[4];
    //dmaMFCMap = new MMD[4];
    //dmaCP = new int[4];
    //dmaDFC = new int[4];
    //dmaDFCMap = new MMD[4];
    //dmaBFC = new int[4];
    //dmaBFCMap = new MMD[4];
    //dmaPCSはresetでは操作しない
    for (int i = 0; i < 4; i++) {
      dmaPCS[i] = 0;
    }
    //割り込み
    //dmaInnerRequest = new int[8];
    //dmaInnerAcknowleged = new int[8];
    //クロック
    //dmaInnerClock = new long[4];
    dmaReset ();
  }  //dmaInit()

  //リセット
  public static void dmaReset () {
    //レジスタ
    for (int i = 0; i < 4; i++) {
      //dmaPCSはresetでは操作しない
      dmaPCT[i] = 0;
      dmaDIT[i] = 0;
      dmaACT[i] = 0;
      dmaERR[i] = 0;
      dmaNDT[i] = 0;
      dmaBLC[i] = 0;
      dmaCOC[i] = 0;
      dmaErrorCode[i] = 0;
      dmaPCL[i] = 0;
      dmaDPS[i] = 0;
      dmaDTYP[i] = 0;
      dmaXRM[i] = 0;
      dmaREQG[i] = 0;
      dmaCHAIN[i] = 0;
      dmaSIZE[i] = 0;
      dmaBTD[i] = 0;
      dmaDIR[i] = 0;
      dmaDAC[i] = 0;
      dmaDACValue[i] = 0;
      dmaMAC[i] = 0;
      dmaMACValue[i] = 0;
      dmaITE[i] = 0;
      dmaSAB[i] = 0;
      dmaHLT[i] = 0;
      dmaCNT[i] = 0;
      dmaSTR[i] = 0;
      dmaMTC[i] = 0;
      dmaMAR[i] = 0;
      dmaDAR[i] = 0;
      dmaBTC[i] = 0;
      dmaBAR[i] = 0;
      dmaNIV[i] = 0x0f;  //割り込みベクタの初期値は未初期化割り込みを示す0x0f
      dmaEIV[i] = 0x0f;
      dmaMFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaMFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaMFCMap[i] = XEiJ.busUserMap;
      }
      dmaCP[i] = 0;
      dmaDFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaDFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaDFCMap[i] = XEiJ.busUserMap;
      }
      dmaBFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaBFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaBFCMap[i] = XEiJ.busUserMap;
      }
    }
    dmaBR = 0;
    dmaBT = 0;
    dmaBurstInterval = DMA_CLOCK_UNIT << 4 + (dmaBT >> 2);
    dmaBurstSpan = dmaBurstInterval >> 1 + (dmaBR & 3);
    dmaBurstStart = XEiJ.FAR_FUTURE;
    dmaBurstEnd = 0L;
    //割り込み
    for (int i = 0; i < 8; i++) {
      dmaInnerRequest[i] = 0;
      dmaInnerAcknowleged[i] = 0;
    }
    //クロック
    for (int i = 0; i < 4; i++) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaReset()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int dmaAcknowledge () {
    for (int i = 0; i < 8; i++) {  //! 未対応。本来はチャンネルプライオリティに従うべき
      int request = dmaInnerRequest[i];
      if (dmaInnerAcknowleged[i] != request) {
        dmaInnerAcknowleged[i] = request;
        return (i & 1) == 0 ? dmaNIV[i >> 1] : dmaEIV[i >> 1];
      }
    }
    return 0;
  }  //dmaAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void dmaDone () {
    for (int i = 0; i < 8; i++) {  //! 未対応。本来はチャンネルプライオリティに従うべき
      if (dmaInnerRequest[i] != dmaInnerAcknowleged[i]) {
        XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
        return;
      }
    }
  }  //dmaDone()

  //dmaStart (i)
  //  DMA転送開始
  public static void dmaStart (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaStart(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
      System.out.printf ("CSR=0x%02x,CER=0x%02x,DCR=0x%02x,OCR=0x%02x,SCR=0x%02x,CCR=0x%02x,MTC=0x%04x,MAR=0x%08x,DAR=0x%08x,BTC=0x%04x,BAR=0x%08x\n",
                         dmaCOC[i] | dmaBLC[i] | dmaNDT[i] | dmaERR[i] | dmaACT[i] | dmaDIT[i] | dmaPCT[i] | dmaPCS[i],  //CSR
                         dmaErrorCode[i],  //CER
                         dmaXRM[i] | dmaDTYP[i] | dmaDPS[i] | dmaPCL[i],  //DCR
                         dmaDIR[i] | dmaBTD[i] | dmaSIZE[i] | dmaCHAIN[i] | dmaREQG[i],  //OCR
                         dmaMAC[i] | dmaDAC[i],  //SCR
                         dmaSTR[i] | dmaCNT[i] | dmaHLT[i] | dmaSAB[i] | dmaITE[i],  //CCR
                         dmaMTC[i], dmaMAR[i], dmaDAR[i], dmaBTC[i], dmaBAR[i]);
    }
    if ((dmaCOC[i] | dmaBLC[i] | dmaNDT[i] | dmaERR[i] | dmaACT[i]) != 0) {  //DMA_CSRがクリアされていない状態でSTRをセットしようとした
      dmaErrorExit (i, DMA_TIMING_ERROR);
      return;
    }
    if (((dmaDTYP[i] == DMA_HD68000_COMPATIBLE || dmaDTYP[i] == DMA_HD6800_COMPATIBLE) &&  //デュアルアドレスモードで
         dmaDPS[i] == DMA_PORT_16_BIT && dmaSIZE[i] == DMA_BYTE_SIZE &&  //DMA_DPSが16ビットでSIZEが8ビットで
         (dmaREQG[i] == DMA_EXTERNAL_REQUEST || dmaREQG[i] == DMA_DUAL_REQUEST)) ||  //外部転送要求のとき、または
        dmaXRM[i] == 0b01000000 || dmaMAC[i] == 0b00001100 || dmaDAC[i] == 0b00000011 || dmaCHAIN[i] == 0b00000100 ||  //不正な値が指定されたとき
        (dmaSIZE[i] == 0b00000011 && !((dmaDTYP[i] == DMA_HD68000_COMPATIBLE || dmaDTYP[i] == DMA_HD6800_COMPATIBLE) && dmaDPS[i] == DMA_PORT_8_BIT))) {
      dmaErrorExit (i, DMA_CONFIGURATION_ERROR);
      return;
    }
    //strには書き込まない
    //チャンネル動作開始
    dmaACT[i] = DMA_ACT;
    if (dmaCHAIN[i] == DMA_ARRAY_CHAINING) {  //アレイチェインモードのとき
      if (dmaBTC[i] == 0) {  //カウントエラー
        dmaErrorExit (i, DMA_BASE_COUNT_ERROR);
        return;
      }
      if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
        dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
        return;
      }
      try {
        MemoryMappedDevice[] mm = dmaBFCMap[i];
        int a = dmaBAR[i];
        dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
        dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
        dmaBAR[i] += 6;
      } catch (M68kException e) {  //バスエラー
        dmaErrorExit (i, DMA_BASE_BUS_ERROR);
        return;
      }
      dmaBTC[i]--;
    } else if (dmaCHAIN[i] == DMA_LINK_ARRAY_CHAINING) {  //リンクアレイチェインモードのとき
      if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
        dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
        return;
      }
      try {
        MemoryMappedDevice[] mm = dmaBFCMap[i];
        int a = dmaBAR[i];
        dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
        dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
        dmaBAR[i] = mm[a + 6 >>> XEiJ.BUS_PAGE_BITS].mmdRws (a + 6) << 16 | mm[a + 8 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 8);
      } catch (M68kException e) {  //バスエラー
        dmaErrorExit (i, DMA_BASE_BUS_ERROR);
        return;
      }
    }
    if (dmaMTC[i] == 0) {  //カウントエラー
      dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
      return;
    }
    if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
      dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
      return;
    }
    if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && dmaDPS[i] == DMA_PORT_16_BIT && (dmaDAR[i] & 1) != 0) {  //アドレスエラー
      dmaErrorExit (i, DMA_DEVICE_ADDRESS_ERROR);
      return;
    }
    if (dmaREQG[i] == DMA_AUTO_REQUEST) {  //オートリクエスト限定速度
      dmaBurstStart = XEiJ.mpuClockTime;  //今回のバースト開始時刻
      dmaBurstEnd = dmaBurstStart + dmaBurstSpan;  //今回のバースト終了時刻
      dmaTransfer (i);  //最初のデータを転送する
    } else if (dmaREQG[i] != DMA_EXTERNAL_REQUEST ||  //オートリクエスト最大速度または最初はオートリクエスト、2番目から外部転送要求
               dmaPCT[i] != 0) {  //外部転送要求で既に要求がある
      dmaTransfer (i);  //最初のデータを転送する
    }
  }  //dmaStart(int)

  //dmaContinue (i)
  //  転送継続
  public static void dmaContinue (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaContinue(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    if (dmaREQG[i] == DMA_AUTO_REQUEST) {  //オートリクエスト限定速度
      if (XEiJ.mpuClockTime < dmaBurstEnd) {  //バースト継続
        //現在時刻に次の予約を入れる
        dmaInnerClock[i] = XEiJ.mpuClockTime;
        TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
      } else {  //バースト終了
        dmaBurstStart += dmaBurstInterval;  //次回のバースト開始時刻
        if (dmaBurstStart < XEiJ.mpuClockTime) {
          dmaBurstStart = XEiJ.mpuClockTime + dmaBurstInterval;  //間に合っていないとき1周だけ延期する
        }
        dmaBurstEnd = dmaBurstStart + dmaBurstSpan;  //次回のバースト終了時刻
        //次回のバースト開始時刻に次の予約を入れる
        dmaInnerClock[i] = dmaBurstStart;
        TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
      }
    } else if (dmaREQG[i] == DMA_AUTO_REQUEST_MAX) {  //オートリクエスト最大速度
      //現在時刻に次の予約を入れる
      dmaInnerClock[i] = XEiJ.mpuClockTime;
      TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
    }
  }  //dmaContinue(int)

  //dmaComplete (i)
  //  転送終了
  //  dmaCOC,dmaBLC,dmaNDTは個別に設定すること
  public static void dmaComplete (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaComplete(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaERR[i] = 0;
    dmaACT[i] = 0;
    dmaSTR[i] = 0;
    dmaCNT[i] = 0;
    dmaSAB[i] = 0;
    dmaErrorCode[i] = 0;
    if (dmaITE[i] != 0) {  //インタラプトイネーブル
      dmaInnerRequest[i << 1]++;
      XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
    }
    if (dmaInnerClock[i] != XEiJ.FAR_FUTURE) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaComplete(int)

  //dmaErrorExit (i, code)
  //  エラー終了
  //  dmaCOC,dmaBLC,dmaNDTは操作しない
  public static void dmaErrorExit (int i, int code) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaErrorExit(%d,%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i, code);
    }
    dmaERR[i] = DMA_ERR;
    dmaACT[i] = 0;
    dmaSTR[i] = 0;
    dmaCNT[i] = 0;
    dmaSAB[i] = 0;
    dmaErrorCode[i] = code;
    if (dmaITE[i] != 0) {  //インタラプトイネーブル
      dmaInnerRequest[i << 1 | 1]++;
      XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
    }
    if (dmaInnerClock[i] != XEiJ.FAR_FUTURE) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaErrorExit(int,int)

  //dmaFallPCL (i) {
  //  外部転送要求
  //  X68000ではREQ3とPCL3が直結されているので同時に変化する
  public static void dmaFallPCL (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaFallPCL(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaPCS[i] = 0;
    dmaPCT[i] = DMA_PCT;
    if (dmaACT[i] != 0 &&  //動作中
        (dmaREQG[i] & (DMA_EXTERNAL_REQUEST & DMA_DUAL_REQUEST)) != 0) {  //外部転送要求または最初はオートリクエスト、2番目から外部転送要求
      //現在時刻から1clk後に次の予約を入れる
      //  0clk後だとADPCMの再生に失敗する場合がある
      dmaInnerClock[i] = XEiJ.mpuClockTime + DMA_CLOCK_UNIT * 1;
      TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
    }
  }  //dmaFallPCL(int)

  //dmaRisePCL (i)
  //  外部転送要求解除
  public static void dmaRisePCL (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaRisePCL(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaPCS[i] = DMA_PCS;
    dmaPCT[i] = 0;
    dmaInnerClock[i] = XEiJ.FAR_FUTURE;
    TickerQueue.tkqRemove (dmaTickerArray[i]);
  }  //dmaRisePCL(int)

  //dmaTransfer (i)
  //  1データ転送する
  public static void dmaTransfer (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaTransfer(%d,0x%08x,0x%08x,%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i,
                         dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? dmaMAR[i] : dmaDAR[i],
                         dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? dmaDAR[i] : dmaMAR[i],
                         dmaSIZE[i] == DMA_BYTE_SIZE || dmaSIZE[i] == DMA_UNPACKED_8_BIT ? 1 : dmaSIZE[i] == DMA_WORD_SIZE ? 2 : 4);
    }
    if (Profiling.PFF_ON) {
      Profiling.pffStart[Profiling.PRF.dmaTransfer.ordinal ()] = System.nanoTime ();
    }
  transfer:
    {
      int code = 0;
      try {
        switch (dmaSIZE[i]) {
        case DMA_BYTE_SIZE:  //オペランドサイズ8ビット、パックあり
          //  オートリクエストで、2バイト以上あり、1バイト目と2バイト目のアドレスの奇遇が異なり、偶数境界を跨がないとき、
          //  2バイト分の転送を1ワードにまとめて行う
          if ((dmaREQG[i] == DMA_AUTO_REQUEST || dmaREQG[i] == DMA_AUTO_REQUEST_MAX) &&  //オートリクエストで
              dmaMTC[i] >= 2) {  //2バイト以上
            boolean mpk = (dmaMAR[i] & 1) == 0 ? dmaMACValue[i] > 0 : dmaMACValue[i] < 0;  //true=メモリをパックできる
            boolean dpk = dmaDPS[i] == DMA_PORT_16_BIT && ((dmaDAR[i] & 1) == 0 ? dmaDACValue[i] > 0 : dmaDACValue[i] < 0);  //true=デバイスをパックできる
            if (mpk || dpk) {  //メモリとデバイスの少なくともどちらか一方をパックできる
              if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリ→デバイス
                int data1;  //1バイト目
                int data2;  //2バイト目
                //メモリからリード
                code = DMA_MEMORY_BUS_ERROR;
                MemoryMappedDevice[] mm = dmaMFCMap[i];
                if (mpk) {  //パックする
                  if (dmaMACValue[i] > 0) {  //インクリメントでパックする
                    int a = dmaMAR[i];
                    int t = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
                    data1 = t >> 8;
                    data2 = t & 255;
                    dmaMAR[i] += 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  } else {  //デクリメントでパックする
                    int a = dmaMAR[i] - 1;
                    int t = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
                    data1 = t & 255;
                    data2 = t >> 8;
                    dmaMAR[i] -= 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                } else {  //パックしない
                  int a = dmaMAR[i];
                  data1 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                  a = dmaMAR[i] += dmaMACValue[i];
                  data2 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                  dmaMAR[i] += dmaMACValue[i];
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                }
                //デバイスへライト
                code = DMA_DEVICE_BUS_ERROR;
                mm = dmaDFCMap[i];
                if (dpk) {  //パックする
                  if (dmaDACValue[i] > 0) {  //インクリメントでパックする
                    int a = dmaDAR[i];
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data1 << 8 | data2);
                    dmaDAR[i] += 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  } else {  //デクリメントでパックする
                    int a = dmaDAR[i] - 1;
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data2 << 8 | data1);
                    dmaDAR[i] -= 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                } else {  //パックしない
                  if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
                    int a = dmaDAR[i];
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data1);
                    a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data2);
                    dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                  } else {  //ポートサイズ16ビット
                    int a = dmaDAR[i];
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data1);
                    a = dmaDAR[i] += dmaDACValue[i];
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data2);
                    dmaDAR[i] += dmaDACValue[i];
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                }
              } else {  //デバイス→メモリ
                int data1;  //1バイト目
                int data2;  //2バイト目
                //デバイスからリード
                code = DMA_DEVICE_BUS_ERROR;
                MemoryMappedDevice[] mm = dmaDFCMap[i];
                if (dpk) {  //パックする
                  if (dmaDACValue[i] > 0) {  //インクリメントでパックする
                    int a = dmaDAR[i];
                    int t = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
                    data1 = t >> 8;
                    data2 = t & 255;
                    dmaDAR[i] += 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  } else {  //デクリメントでパックする
                    int a = dmaDAR[i] - 1;
                    int t = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
                    data1 = t & 255;
                    data2 = t >> 8;
                    dmaDAR[i] -= 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                } else {  //パックしない
                  if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
                    int a = dmaDAR[i];
                    data1 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                    a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
                    data2 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                    dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                  } else {  //ポートサイズ16ビット
                    int a = dmaDAR[i];
                    data1 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                    a = dmaDAR[i] += dmaDACValue[i];
                    data2 = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
                    dmaDAR[i] += dmaDACValue[i];
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                }
                //メモリへライト
                code = DMA_MEMORY_BUS_ERROR;
                mm = dmaMFCMap[i];
                if (mpk) {  //パックする
                  if (dmaMACValue[i] > 0) {  //インクリメントでパックする
                    int a = dmaMAR[i];
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data1 << 8 | data2);
                    dmaMAR[i] += 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  } else {  //デクリメントでパックする
                    int a = dmaMAR[i] - 1;
                    mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data2 << 8 | data1);
                    dmaMAR[i] -= 2;
                    //XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                  }
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
                } else {  //パックしない
                  int a = dmaMAR[i];
                  mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data1);
                  a = dmaMAR[i] += dmaMACValue[i];
                  mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data2);
                  dmaMAR[i] += dmaMACValue[i];
                  XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
                }
              }
              //パックできた
              dmaMTC[i]--;  //1バイト余分に転送した
              break;
            }  //if mpk||dpk
          }  //if dmaMTC[i]>=2
          //パックできない
        case DMA_UNPACKED_8_BIT:  //オペランドサイズ8ビット、パックなし
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリ→デバイス
            int data;
            //メモリからリード
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];
            data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
            dmaMAR[i] += dmaMACValue[i];
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
            //デバイスへライト
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            a = dmaDAR[i];
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
            } else {  //ポートサイズ16ビット
              dmaDAR[i] += dmaDACValue[i];
            }
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
          } else {  //デバイス→メモリ
            int data;
            //デバイスからリード
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            int a = dmaDAR[i];
            data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
            } else {  //ポートサイズ16ビット
              dmaDAR[i] += dmaDACValue[i];
            }
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
            //メモリへライト
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            a = dmaMAR[i];
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);
            dmaMAR[i] += dmaMACValue[i];
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
          }
          break;
        case DMA_WORD_SIZE:  //オペランドサイズ16ビット
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリ→デバイス
            int data;
            //メモリからリード
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];
            data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
            dmaMAR[i] += dmaMACValue[i] << 1;
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
            //デバイスへライト
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              a = dmaDAR[i];
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 8);
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
            } else {  //ポートサイズ16ビット
              a = dmaDAR[i];
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);
              dmaDAR[i] += dmaDACValue[i] << 1;
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
            }
          } else {  //デバイス→メモリ
            int data;
            //デバイスからリード
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              int a = dmaDAR[i];
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 8;
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
            } else {  //ポートサイズ16ビット
              int a = dmaDAR[i];
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);;
              dmaDAR[i] += dmaDACValue[i] << 1;
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
            }
            //メモリへライト
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            int a = dmaMAR[i];
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);
            dmaMAR[i] += dmaMACValue[i] << 1;
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 1;
          }
          break;
        case DMA_LONG_WORD_SIZE:  //オペランドサイズ32ビット
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリ→デバイス
            int data;
            //メモリからリード
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];
            data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a) << 16;  //オペランドサイズが32ビットでも16ビットずつアクセスする
            a = dmaMAR[i] += dmaMACValue[i] << 1;
            data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
            dmaMAR[i] += dmaMACValue[i] << 1;
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
            //デバイスへライト
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              a = dmaDAR[i];
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 24);
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 16);
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 8);
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 4;
            } else {  //ポートサイズ16ビット
              a = dmaDAR[i];
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data >> 16);
              a = dmaDAR[i] += dmaDACValue[i] << 1;
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);
              dmaDAR[i] += dmaDACValue[i] << 1;
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
            }
          } else {  //デバイス→メモリ
            int data;
            //デバイスからリード
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //ポートサイズ8ビット
              int a = dmaDAR[i];
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 24;
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 16;
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 8;
              a = dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
              dmaDAR[i] += dmaDACValue[i] << 1;  //ポートサイズが8ビットでもDMA_DARは16ビットずつ変化する
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 4;
            } else {  //ポートサイズ16ビット
              int a = dmaDAR[i];
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a) << 16;
              a = dmaDAR[i] += dmaDACValue[i] << 1;
              data |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
              dmaDAR[i] += dmaDACValue[i] << 1;
              XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
            }
            //メモリへライト
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            int a = dmaMAR[i];
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data >> 16);  //オペランドサイズが32ビットでも16ビットずつアクセスする
            a = dmaMAR[i] += dmaMACValue[i] << 1;
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);
            dmaMAR[i] += dmaMACValue[i] << 1;
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 2;
          }
          break;
        }  //switch dmaSIZE[i]
      } catch (M68kException e) {
        dmaErrorExit (i, code);
        break transfer;
      }
      dmaMTC[i]--;
      if (dmaMTC[i] != 0) {  //継続
        dmaContinue (i);
      } else if (dmaCHAIN[i] == DMA_ARRAY_CHAINING) {  //アレイチェーンモードのとき
        if (dmaBTC[i] != 0) {  //継続
          //アドレスエラーのチェックは不要
          try {
            MemoryMappedDevice[] mm = dmaBFCMap[i];
            int a = dmaBAR[i];
            dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
            dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
            dmaBAR[i] += 6;
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 3;
          } catch (M68kException e) {  //バスエラー
            dmaErrorExit (i, DMA_BASE_BUS_ERROR);
            break transfer;
          }
          dmaBTC[i]--;
          if (dmaMTC[i] == 0) {  //カウントエラー
            dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
            break transfer;
          }
          if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
            break transfer;
          }
          dmaContinue (i);
        } else {  //終了
          dmaCOC[i] = DMA_COC;
          dmaBLC[i] = DMA_BLC;
          dmaNDT[i] = 0;
          dmaComplete (i);
        }
      } else if (dmaCHAIN[i] == DMA_LINK_ARRAY_CHAINING) {  //リンクアレイチェーンモードのとき
        if (dmaBAR[i] != 0) {  //継続
          if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
            break transfer;
          }
          try {
            MemoryMappedDevice[] mm = dmaBFCMap[i];
            int a = dmaBAR[i];
            dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
            dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
            dmaBAR[i] = mm[a + 6 >>> XEiJ.BUS_PAGE_BITS].mmdRws (a + 6) << 16 | mm[a + 8 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 8);
            XEiJ.mpuClockTime += DMA_CLOCK_UNIT * 5;
          } catch (M68kException e) {  //バスエラー
            dmaErrorExit (i, DMA_BASE_BUS_ERROR);
            break transfer;
          }
          if (dmaMTC[i] == 0) {  //カウントエラー
            dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
            break transfer;
          }
          if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
            break transfer;
          }
          dmaContinue (i);
        } else {  //終了
          dmaCOC[i] = DMA_COC;
          dmaBLC[i] = DMA_BLC;
          dmaNDT[i] = 0;
          dmaComplete (i);
        }
      } else if (dmaCNT[i] != 0) {  //コンティニューモードのとき
        dmaBLC[i] = DMA_BLC;
        dmaCNT[i] = 0;
        if (dmaITE[i] != 0) {  //インタラプトイネーブル
          dmaInnerRequest[i << 1]++;
          XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
        }
        dmaMTC[i] = dmaBTC[i];
        dmaMAR[i] = dmaBAR[i];
        if (dmaMTC[i] == 0) {  //カウントエラー
          dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
          break transfer;
        }
        if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
          dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
          break transfer;
        }
        dmaContinue (i);
      } else {  //終了
        dmaCOC[i] = DMA_COC;
        dmaBLC[i] = 0;
        dmaNDT[i] = 0;
        dmaComplete (i);
      }
    }  //transfer
    if (Profiling.PFF_ON) {
      Profiling.pffTotal[Profiling.PRF.dmaTransfer.ordinal ()] += System.nanoTime () - Profiling.pffStart[Profiling.PRF.dmaTransfer.ordinal ()];
      Profiling.pffCount[Profiling.PRF.dmaTransfer.ordinal ()]++;
    }
  }  //dmaTransfer

}  //class HD63450



