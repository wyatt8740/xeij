//========================================================================================
//  Profiling.java
//    en:Profiling -- It measures the elapsed time of methods.
//    ja:プロファイリング -- メソッドの所要時間を計ります。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  メソッドの所要時間を計る
//  メソッドの先頭
//    if (Profiling.PFF_ON) {
//      Profiling.pffStart[Profiling.PRF.<name-of-method>.ordinal ()] = System.nanoTime ();
//    }
//  メソッドの末尾
//    if (Profiling.PFF_ON) {
//      Profiling.pffTotal[Profiling.PRF.<name-of-method>.ordinal ()] += System.nanoTime () - Profiling.pffStart[Profiling.PRF.<name-of-method>.ordinal ()];
//      Profiling.pffCount[Profiling.PRF.<name-of-method>.ordinal ()]++;
//    }
//  returnやthrowで抜けてしまうと計れないことに注意
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class Profiling {

  public static final boolean PFF_ON = false;  //true=メソッドの所要時間を計る

  public static enum PRF {

    //MMU
    mmuTranslate,

    //IRP
    irpOriByte,
    irpOriWord,
    irpOriLong,
    irpCmp2Chk2Byte,
    irpAndiByte,
    irpAndiWord,
    irpAndiLong,
    irpCmp2Chk2Word,
    irpSubiByte,
    irpSubiWord,
    irpSubiLong,
    irpCmp2Chk2Long,
    irpAddiByte,
    irpAddiWord,
    irpAddiLong,
    irpCallm,
    irpBtstImm,
    irpBchgImm,
    irpBclrImm,
    irpBsetImm,
    irpEoriByte,
    irpEoriWord,
    irpEoriLong,
    irpCasByte,
    irpCmpiByte,
    irpCmpiWord,
    irpCmpiLong,
    irpCasWord,
    irpMovesByte,
    irpMovesWord,
    irpMovesLong,
    irpCasLong,
    irpBtstReg,
    irpBchgReg,
    irpBclrReg,
    irpBsetReg,
    irpMoveToDRByte,
    irpMoveToMMByte,
    irpMoveToMPByte,
    irpMoveToMNByte,
    irpMoveToMWByte,
    irpMoveToMXByte,
    irpMoveToZWByte,
    irpMoveToZLByte,
    irpMoveToDRLong,
    irpMoveaLong,
    irpMoveToMMLong,
    irpMoveToMPLong,
    irpMoveToMNLong,
    irpMoveToMWLong,
    irpMoveToMXLong,
    irpMoveToZWLong,
    irpMoveToZLLong,
    irpMoveToDRWord,
    irpMoveaWord,
    irpMoveToMMWord,
    irpMoveToMPWord,
    irpMoveToMNWord,
    irpMoveToMWWord,
    irpMoveToMXWord,
    irpMoveToZWWord,
    irpMoveToZLWord,
    irpNegxByte,
    irpNegxWord,
    irpNegxLong,
    irpMoveFromSR,
    irpClrByte,
    irpClrWord,
    irpClrLong,
    irpMoveFromCCR,
    irpNegByte,
    irpNegWord,
    irpNegLong,
    irpMoveToCCR,
    irpNotByte,
    irpNotWord,
    irpNotLong,
    irpMoveToSR,
    irpNbcd,
    irpPea,
    irpMovemToMemWord,
    irpMovemToMemLong,
    irpTstByte,
    irpTstWord,
    irpTstLong,
    irpTas,
    irpMuluMulsLong,
    irpDivuDivsLong,
    irpMovemToRegWord,
    irpMovemToRegLong,
    irpEmx,
    irpTrap,
    irpTrap15,
    irpLinkWord,
    irpUnlk,
    irpMoveToUsp,
    irpMoveFromUsp,
    irpReset,
    irpNop,
    irpStop,
    irpRte,
    irpRtd,
    irpRts,
    irpTrapv,
    irpRtr,
    irpMovecFromControl,
    irpMovecToControl,
    irpMisc,
    irpJsr,
    irpJmp,
    irpChkLong,
    irpChkWord,
    irpLea,
    irpAddqByte,
    irpAddqWord,
    irpAddqLong,
    irpSubqByte,
    irpSubqWord,
    irpSubqLong,
    irpSt,
    irpSf,
    irpScc,
    irpShi,
    irpSls,
    irpShs,
    irpSlo,
    irpSne,
    irpSeq,
    irpSvc,
    irpSvs,
    irpSpl,
    irpSmi,
    irpSge,
    irpSlt,
    irpSgt,
    irpSle,
    irpBrasw,
    irpBras,
    irpBrasl,
    irpBsrsw,
    irpBsrs,
    irpBsrsl,
    irpBccsw,
    irpBccs,
    irpBccsl,
    irpBhisw,
    irpBhis,
    irpBhisl,
    irpBlssw,
    irpBlss,
    irpBlssl,
    irpBhssw,
    irpBhss,
    irpBhssl,
    irpBlosw,
    irpBlos,
    irpBlosl,
    irpBnesw,
    irpBnes,
    irpBnesl,
    irpBeqsw,
    irpBeqs,
    irpBeqsl,
    irpBvcsw,
    irpBvcs,
    irpBvcsl,
    irpBvssw,
    irpBvss,
    irpBvssl,
    irpBplsw,
    irpBpls,
    irpBplsl,
    irpBmisw,
    irpBmis,
    irpBmisl,
    irpBgesw,
    irpBges,
    irpBgesl,
    irpBltsw,
    irpBlts,
    irpBltsl,
    irpBgtsw,
    irpBgts,
    irpBgtsl,
    irpBlesw,
    irpBles,
    irpBlesl,
    irpMoveq,
    irpMvsByte,
    irpMvsWord,
    irpMvzByte,
    irpMvzWord,
    irpOrToRegByte,
    irpOrToRegWord,
    irpOrToRegLong,
    irpDivuWord,
    irpOrToMemByte,
    irpOrToMemWord,
    irpOrToMemLong,
    irpDivsWord,
    irpSubToRegByte,
    irpSubToRegWord,
    irpSubToRegLong,
    irpSubaWord,
    irpSubToMemByte,
    irpSubToMemWord,
    irpSubToMemLong,
    irpSubaLong,
    irpAline,
    irpCmpByte,
    irpCmpWord,
    irpCmpLong,
    irpCmpaWord,
    irpEorByte,
    irpEorWord,
    irpEorLong,
    irpCmpaLong,
    irpAndToRegByte,
    irpAndToRegWord,
    irpAndToRegLong,
    irpMuluWord,
    irpAndToMemByte,
    irpAndToMemWord,
    irpAndToMemLong,
    irpMulsWord,
    irpAddToRegByte,
    irpAddToRegWord,
    irpAddToRegLong,
    irpAddaWord,
    irpAddToMemByte,
    irpAddToMemWord,
    irpAddToMemLong,
    irpAddaLong,
    irpXxrToRegByte,
    irpXxrToRegWord,
    irpXxrToRegLong,
    irpAsrToMem,
    irpLsrToMem,
    irpRoxrToMem,
    irpRorToMem,
    irpXxlToRegByte,
    irpXxlToRegWord,
    irpXxlToRegLong,
    irpAslToMem,
    irpLslToMem,
    irpRoxlToMem,
    irpRolToMem,
    irpBftst,
    irpBfextu,
    irpBfchg,
    irpBfexts,
    irpBfclr,
    irpBfffo,
    irpBfset,
    irpBfins,
    irpPgen,
    irpPscc,
    irpPbccWord,
    irpPbccLong,
    irpPsave,
    irpPrestore,
    irpFgen,
    irpFscc,
    irpFbccWord,
    irpFbccLong,
    irpFsave,
    irpFrestore,
    irpCinvCpushNC,
    irpCinvCpushDC,
    irpCinvCpushIC,
    irpCinvCpushBC,
    irpPflush,
    irpPlpaw,
    irpPlpar,
    irpMove16,
    irpLpstop,
    irpFpack,
    irpFline,
    irpIllegal,
    irpSetPC,
    irpSetSR,
    irpInterrupt,
    irpException,

    //CRT
    CRTTicker__START__,
    InitialStage,
    NormalStart,
    NormalDrawIdleFront,
    NormalDrawIdleSync,
    NormalDrawIdleBackDisp,
    NormalDrawDispFront,
    NormalDrawDispSync,
    NormalDrawDispBack,
    NormalDrawDispDisp,
    NormalOmitIdleFront,
    NormalOmitIdleSync,
    NormalOmitIdleBackDisp,
    NormalOmitDispFront,
    NormalOmitDispSync,
    NormalOmitDispBackDisp,
    DuplicationStart,
    DuplicationDrawIdleFront,
    DuplicationDrawIdleSync,
    DuplicationDrawIdleBackDisp,
    DuplicationDrawDispEvenFront,
    DuplicationDrawDispOddFront,
    DuplicationDrawDispEvenSync,
    DuplicationDrawDispOddSync,
    DuplicationDrawDispEvenBack,
    DuplicationDrawDispOddBack,
    DuplicationDrawDispEvenDisp,
    DuplicationDrawDispOddDisp,
    DuplicationOmitIdleFront,
    DuplicationOmitIdleSync,
    DuplicationOmitIdleBackDisp,
    DuplicationOmitDispEvenFront,
    DuplicationOmitDispOddFront,
    DuplicationOmitDispEvenSync,
    DuplicationOmitDispOddSync,
    DuplicationOmitDispEvenBackDisp,
    DuplicationOmitDispOddBackDisp,
    InterlaceStart,
    InterlaceDrawIdleFront,
    InterlaceDrawIdleSync,
    InterlaceDrawIdleBackDisp,
    InterlaceDrawDispFront,
    InterlaceDrawDispSync,
    InterlaceDrawDispBack,
    InterlaceDrawDispDisp,
    InterlaceOmitIdleFront,
    InterlaceOmitIdleSync,
    InterlaceOmitIdleBackDisp,
    InterlaceOmitDispFront,
    InterlaceOmitDispSync,
    InterlaceOmitDispBackDisp,
    SlitStart,
    SlitDrawIdleFront,
    SlitDrawIdleSync,
    SlitDrawIdleBackDisp,
    SlitDrawDispFront,
    SlitDrawDispSync,
    SlitDrawDispBack,
    SlitDrawDispDisp,
    SlitOmitIdleFront,
    SlitOmitIdleSync,
    SlitOmitIdleBackDisp,
    SlitOmitDispFront,
    SlitOmitDispSync,
    SlitOmitDispBackDisp,
    CRTTicker__END__,

    //DMA
    dmaTransfer,

    //MFP
    mfpTick,

    //SND
    sndTick,

    //OPM
    opmUpdate,

    //PCM
    pcmWriteBuffer,
    pcmFillBuffer,

    PRF__END__,

    CRTTicker,

  };  //enum PRF

  public static final int PFF_STAGE_STANDBY = 0;
  public static final int PFF_STAGE_START   = 1;
  public static final int PFF_STAGE_RUNNING = 2;
  public static final int PFF_STAGE_STOP    = 3;

  public static PRF[] pffValues;
  public static int pffLength;
  public static String[] pffNames;
  public static long[] pffStart;
  public static long[] pffTotal;
  public static long[] pffCount;
  public static JCheckBoxMenuItem pffCheckBoxMenuItem;
  public static int pffStage;
  public static JFrame pffFrame;
  public static ScrollTextArea pffBoard;
  public static JTextArea pffTextArea;

  public static void pffInit () {
    pffValues = PRF.values ();
    pffLength = pffValues.length;
    pffNames = new String[pffLength];
    for (PRF n : pffValues) {
      pffNames[n.ordinal ()] = n.toString ();
    }
    pffStart = new long[pffLength];
    pffTotal = new long[pffLength];
    pffCount = new long[pffLength];
    pffCheckBoxMenuItem =
      Multilingual.mlnText (
        XEiJ.createCheckBoxMenuItem (
          false, "Profiling",
          new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              if (pffCheckBoxMenuItem.isSelected ()) {
                if (pffStage == PFF_STAGE_STANDBY) {
                  pffStage = PFF_STAGE_START;
                }
              } else {
                if (pffStage == PFF_STAGE_RUNNING) {
                  pffStage = PFF_STAGE_STOP;
                }
              }
            }
          }),
        "ja", "プロファイリング");
    pffStage = PFF_STAGE_STANDBY;
  }  //pffInit

  //pffClear ()
  //  メソッドの所要時間を消去
  public static void pffClear () {
    Arrays.fill (pffStart, 0L);
    Arrays.fill (pffTotal, 0L);
    Arrays.fill (pffCount, 0L);
    pffStage = PFF_STAGE_RUNNING;
  }  //pffClear()

  //pffReport ()
  //  メソッドの所要時間を報告
  public static void pffReport () {
    long[] ttl = Arrays.copyOf (pffTotal, pffLength);  //ソート中に値が変わると気持ち悪いのでコピーしてからソートする
    long[] cnt = Arrays.copyOf (pffCount, pffLength);
    {
      int k = PRF.CRTTicker.ordinal ();
      ttl[k] = 0L;
      cnt[k] = 0L;
      for (int i = PRF.CRTTicker__START__.ordinal () + 1, l = PRF.CRTTicker__END__.ordinal (); i < l; i++) {
        ttl[k] += ttl[i];
        cnt[k] += cnt[i];
      }
    }
    int[] a = new int[pffLength];
    for (int i = 0; i < pffLength; i++) {
      long x = ttl[i];
      int l = 0;
      int r = i;
      while (l < r) {
        int m = (l + r) >> 1;
        if (ttl[a[m]] >= x) {
          l = m + 1;
        } else {
          r = m;
        }
      }
      r = i;
      while (l < r) {
        a[r] = a[r - 1];
        r--;
      }
      a[l] = i;
    }
    StringBuilder sb = new StringBuilder ();
    sb.append ("rank      time       ratio     count       average                    method\n");
    sb.append ("----  ------------  -------  ---------  -------------  ------------------------------------\n");
    long total = 0L;
    for (int i = 0, l = PRF.PRF__END__.ordinal (); i < l; i++) {
      total += ttl[i];
    }
    for (int i = 0; i < pffLength; i++) {
      int j = a[i];
      long k = ttl[j];
      if (k == 0L) {
        break;
      }
      //!!! 計測範囲の包含関係を考慮していないので占有率の表示が正しくない
      sb.append (String.format ("%4d  %10.3fms  %6.3f%%  %9d  %11.3fns  %s\n",
                                i + 1,
                                (double) k / 1000000.0,
                                100.0 * (double) k / (double) total,
                                cnt[j],
                                (double) k / (double) cnt[j],
                                pffNames[j]));
    }
    if (pffFrame == null) {
      pffBoard = XEiJ.setPreferredSize (
        XEiJ.setFont (new ScrollTextArea (), new Font ("Monospaced", Font.PLAIN, 12)),
        600, 400);
      pffBoard.setMargin (new Insets (2, 4, 2, 4));
      pffBoard.setHighlightCursorOn (true);
      pffTextArea = pffBoard.getTextArea ();
      pffTextArea.setEditable (false);
      pffFrame = Multilingual.mlnTitle (
        XEiJ.createRestorableSubFrame (
          Settings.SGS_PFF_FRAME_KEY,
          "Profiling",
          null,
          pffBoard
        ),
        "ja", "プロファイリング");
    }
    pffTextArea.setText (sb.toString ());
    pffTextArea.setCaretPosition (0);
    pffFrame.setVisible (true);
    pffStage = PFF_STAGE_STANDBY;
  }  //pffReport()

}  //class Profiling



