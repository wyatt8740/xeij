//========================================================================================
//  XEiJ.java
//    en:Main class
//    ja:メインクラス
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.datatransfer.*;  //Clipboard,DataFlavor,FlavorEvent,FlavorListener,Transferable,UnsupportedFlavorException
import java.awt.dnd.*;  //DnDConstants,DropTarget,DropTargetAdapter,DropTargetDragEvent
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.font.*;  //FontRenderContext,LineMetrics,TextLayout
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.im.*;  //InputContext
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.beans.*;  //PropertyChangeListener,XMLDecoder,XMLEncoder
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,ByteArrayOutputStream,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile,UnsupportedEncodingException
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,InterruptedException,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.math.*;  //BigDecimal,BigInteger,MathContext,RoundingMode
import java.net.*;  //MalformedURLException,URI,URL
import java.nio.*;  //ByteBuffer,ByteOrder
import java.nio.charset.*;  //Charset
import java.text.*;  //DecimalFormat,NumberFormat,ParseException
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,TimeZone,Timer,TimerTask,TreeMap
import java.util.function.*;  //IntConsumer,IntSupplier
import java.util.regex.*;  //Matcher,Pattern
import java.util.zip.*;  //CRC32,Deflater,GZIPInputStream,GZIPOutputStream,ZipEntry,ZipInputStream
import javax.accessibility.*;  //AccessibleContext
import javax.imageio.*;  //ImageIO
import javax.imageio.stream.*;  //ImageOutputStream
import javax.jnlp.*;  //BasicService,PersistenceService,ServiceManager,UnavailableServiceException
import javax.sound.sampled.*;  //AudioFormat,AudioSystem,DataLine,LineUnavailableException,SourceDataLine
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.border.*;  //Border,CompoundBorder,EmptyBorder,EtchedBorder,LineBorder,TitledBorder
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.filechooser.*;  //FileFilter,FileNameExtensionFilter
import javax.swing.plaf.*;  //ColorUIResource,FontUIResource,IconUIResource,InsetsUIResource
import javax.swing.plaf.basic.*;  //BasicTextUI
import javax.swing.plaf.metal.*;  //MetalLookAndFeel,MetalTheme,OceanTheme
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument
import netscape.javascript.*;  //JSException,JSObject。jfxrt.jarではなくplugin.jarを使うこと

public class XEiJ {

  //名前とバージョン
  public static final String PRG_TITLE = "XEiJ (X68000 Emulator in Java)";  //タイトル
  public static final String PRG_VERSION = "0.16.08.17";  //バージョン
  public static final String PRG_JAVA_VENDOR = "Oracle Corporation";  //動作を確認しているJavaのベンダー
  public static final String PRG_JAVA_VERSION = "1.8.0_102";  //動作を確認しているJavaのバージョン
  public static final String PRG_ARCHITECTURE = "64";  //動作を確認しているアーキテクチャ

  //全体の設定
  //  bit0..3のテストにシフトを使う
  //    TEST_BIT_0_SHIFT ? a << 31 != 0 : (a & 1) != 0
  //    TEST_BIT_1_SHIFT ? a << 30 < 0 : (a & 2) != 0
  //    TEST_BIT_2_SHIFT ? a << 29 < 0 : (a & 4) != 0
  //    TEST_BIT_3_SHIFT ? a << 28 < 0 : (a & 8) != 0
  public static final boolean TEST_BIT_0_SHIFT = false;  //true=bit0のテストにシフトを使う
  public static final boolean TEST_BIT_1_SHIFT = false;  //true=bit1のテストにシフトを使う
  public static final boolean TEST_BIT_2_SHIFT = true;  //true=bit2のテストにシフトを使う
  public static final boolean TEST_BIT_3_SHIFT = true;  //true=bit3のテストにシフトを使う
  //  shortの飽和処理にキャストを使う
  //    x = SHORT_SATURATION_CAST ? (short) x == x ? x : x >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, x));
  //    m = SHORT_SATURATION_CAST ? (short) m == m ? m : m >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, m));
  //    l = SHORT_SATURATION_CAST ? (short) l == l ? l : l >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, l));
  //    r = SHORT_SATURATION_CAST ? (short) r == r ? r : r >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, r));
  //  または
  //    if (SHORT_SATURATION_CAST) {
  //      if ((short) x != x) {
  //        x = x >> 31 ^ 32767;
  //      }
  //    } else {
  //      if (x < -32768) {
  //        x = -32768;
  //      } else if (x > 32767) {
  //        x = 32767;
  //      }
  //    }
  public static final boolean SHORT_SATURATION_CAST = false;  //shortの飽和処理にキャストを使う

  //バイナリデータの埋め込み
  //  byte[]の場合
  //    Javaはbyteの定数配列をstatic final byte[] XXX={～}で直接記述しにくい
  //      bit7がセットされているデータをいちいち(byte)でキャストしなければならない
  //      初期化コードが巨大化してコンパイラを通らなくなる
  //    Stringに詰め込んで起動時にString.getBytes(Charset)を使ってbyte[]に展開する
  //    ISO-8859-1はすべてのJava実行環境で実装しなければならないことになっているので環境依存にはならない
  //    static final int[] XXX={～}で書いておいてPerlスクリプトで文字列に変換する
  //    final int[]をbyte[]に変更すると動作が遅くなる場合があることに注意する
  //    perl misc/itob.pl xeij/???.java XXX
  public static final Charset ISO_8859_1 = Charset.forName ("ISO-8859-1");
  static {
    if (false) {
      //ISO-8859-1が8bitバイナリデータを素通りさせるかどうかのテスト
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < 256; i++) {
        sb.append ((char) i);
      }
      byte[] bb = sb.toString ().getBytes (ISO_8859_1);
      for (int i = 0; i < 256; i++) {
        System.out.printf ("%02x %02x %s\n", i, bb[i] & 255, i == (bb[i] & 255) ? "OK" : "ERROR");
      }
    }
  }
  //  char[]の場合
  //    byte[]の場合と同様にStringに詰め込んで起動時にString.toCharArray()を使ってchar[]に展開する
  //    static final int[] XXX={～}で書いておいてPerlスクリプトで文字列に変換する
  //    final int[]をchar[]に変更すると動作が遅くなる場合があることに注意する
  //    perl misc/itoc.pl xeij/???.java XXX



  //========================================================================================
  //$$PRG プログラムの入り口と出口

  //動作環境
  public static String prgJavaVendor;  //動作環境のJavaのベンダー
  public static String prgJavaVersion;  //動作環境のJavaのバージョン
  public static String prgArchitecture;  //動作環境のアーキテクチャ

  //  アプレットのとき
  //    ホストのファイルシステムにアクセスできない
  //    フレームは不要
  //  JNLPのとき
  //    ホストのファイルシステムにアクセスできない
  //    フレームが必要
  //  ローカルのとき
  //    ホストのファイルシステムにアクセスできる
  //    フレームが必要
  public static boolean prgIsApplet;  //true=アプレット
  public static boolean prgIsJnlp;  //true=JNLP
  public static boolean prgIsLocal;  //true=ローカル

  public static String prgLang;
  public static boolean prgVerbose;

  public static String[] prgArgs;

  //main (args)
  //  コマンドラインまたはJNLPの開始
  public static void main (String[] args) {

    prgIsApplet = false;  //アプレットでない
    XEiJApplet.appApplet = null;
    try {
      new UnavailableServiceException ();
      prgIsJnlp = true;  //JNLP
      prgIsLocal = false;  //ローカルでない
    } catch (NoClassDefFoundError ncdfe) {
      prgIsJnlp = false;  //JNLPでない
      prgIsLocal = true;  //ローカル
    }
    prgArgs = args;

    //起動する
    SwingUtilities.invokeLater (new Runnable () {
      @Override public void run () {
        new XEiJ ();
      }
    });

  }  //main(String[])

  //XEiJ ()
  //  コンストラクタ
  public XEiJ () {
    prgInit ();  //開始
  }  //XEiJ()

  //prgInit ()
  //  開始
  public static void prgInit () {

    //初期化
    //  この段階でコンポーネントを参照してはならない
    //  メニューの初期値に必要な情報があればここで作っておく
    if (prgIsApplet) {  //アプレットのとき
      XEiJApplet.appInit ();
    }
    cpfInit ();  //CPF コンポーネントファクトリー
    RestorableFrame.rfmInit ();  //RFM RestorableFrame
    Settings.sgsInit ();  //SGS 設定
    LnF.lnfInit ();  //Look&Feel
    fmtInit ();  //BCD 10進数変換

    //  ターミナルウインドウにメッセージを表示するため最初に初期化する
    RS232CTerminal.trmInit ();  //TRM ターミナルウインドウ

    Multilingual.mlnInit ();  //MLN 多言語化

    CharacterCode.chrInit ();  //CHR 文字コード

    TickerQueue.tkqInit ();  //TKQ ティッカーキュー

    mdlInit ();  //MDL 機種

    SlowdownTest.sdtInit ();  //SDT 鈍化テスト
    Indicator.indInit ();  //IND インジケータ
    Keyboard.kbdInit ();  //KBD キーボード
    CONDevice.conInit ();  //CON CONデバイス制御
    musInit ();  //MUS マウス
    pnlInit ();  //PNL パネル
    rbtInit ();  //RBT ロボット
    if (!prgIsApplet) {  //JNLPまたはローカルのとき
      frmInit ();
    }

    dbgInit ();  //DBG デバッガ共通コンポーネント
    RegisterList.drpInit ();  //DRP レジスタ
    DisassembleList.ddpInit ();  //DDP 逆アセンブルリスト
    MemoryDumpList.dmpInit ();  //DMP メモリダンプリスト
    LogicalSpaceMonitor.atwInit ();  //ATW アドレス変換ウインドウ
    PhysicalSpaceMonitor.paaInit ();  //PAA 物理アドレス空間ウインドウ
    dgtInit ();  //DGT コンソール
    if (BranchLog.BLG_ON) {
      BranchLog.blgInit ();  //BLG 分岐ログ
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      ProgramFlowVisualizer.pfvInit ();  //PFV プログラムフロービジュアライザ
    }
    if (RasterBreakPoint.RBP_ON) {
      RasterBreakPoint.rbpInit ();  //RBP ラスタブレークポイント
    }
    if (ScreenModeTest.SMT_ON) {
      ScreenModeTest.smtInit ();  //SMT 表示モードテスト
    }
    if (RootPointerList.RTL_ON) {
      RootPointerList.rtlInit ();  //RTL ルートポインタリスト
    }
    if (SpritePatternViewer.SPV_ON) {
      SpritePatternViewer.spvInit ();  //SPV スプライトパターンビュア
    }
    if (ATCMonitor.ACM_ON) {
      ATCMonitor.acmInit ();  //ACM アドレス変換キャッシュモニタ
    }

    SoundSource.sndInit ();  //SND サウンド
    FEFunction.fpkInit ();  //FPK FPACK
    mpuInit ();  //MPU MPU
    if (InstructionBreakPoint.IBP_ON) {
      InstructionBreakPoint.ibpInit ();  //IBP 命令ブレークポイント
    }
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpInit ();  //DBP データブレークポイント
    }
    if (Profiling.PFF_ON) {
      Profiling.pffInit ();  //PRF 命令の所要時間
    }
    busInit ();  //BUS バスコントローラ
    MC68060.mmuInit ();  //MMU メモリ管理ユニット
    MainMemory.mmrInit ();  //MMR メインメモリ
    //graInit ();  //GRA グラフィックス画面
    txtInit ();  //TXT テキスト画面
    CRTC.crtInit ();  //CRT CRTコントローラ
    VideoController.vcnInit ();  //VCN ビデオコントローラ
    HD63450.dmaInit ();  //DMA DMAコントローラ
    //svsInit ();  //SVS スーパーバイザ領域設定
    MC68901.mfpInit ();  //MFP MFP
    RP5C15.rtcInit ();  //RTC RTC
    PrinterPort.prnInit ();  //PRN プリンタポート
    sysInit ();  //SYS システムポート
    YM2151.opmInit ();  //OPM FM音源
    SoundMonitor.smnInit ();  //SMN 音声モニタ
    ADPCM.pcmInit ();  //PCM ADPCM音源
    FDC.fdcInit ();  //FDC FDコントローラ
    HDC.hdcInit ();  //HDC SASI HDコントローラ
    SPC.spcInit ();  //SPC SCSIプロトコルコントローラ
    Z8530.sccInit ();  //SCC SCC
    ppiInit ();  //PPI PPI
    //ioiInit ();  //IOI I/O割り込み
    HFS.hfsInit ();  //HFS ホストファイルシステムインタフェイス
    //genInit ();  //XB2 拡張ボード領域2
    SpriteScreen.sprInit ();  //SPR スプライト画面
    //smrInit()はSPC.spcSCSIEXRequestとmdlSCSIINRequestを使うのでspcInit()よりも後であること
    smrInit ();  //SMR SRAM
    fntInit ();  //FNT フォント
    romInit ();  //ROM ROM

    //コンポーネントを作る
    //  他のコンポーネントを参照するときは順序に注意する
    Settings.sgsMakeMenu ();  //SGS 設定
    mpuMakeMenu ();  //MPU MPU
    pnlMake ();  //PNL パネル
    mnbMake ();  //MNB メニューバー
    if (prgIsApplet) {  //アプレットのとき
      XEiJApplet.appMake ();  //APP アプレット
    } else {  //JNLPまたはローカルのとき
      frmMake ();  //FRM フレーム
    }
    clpMake ();  //CLP クリップボード
    dbgMake ();  //DBG デバッガ共通コンポーネント
    //RS232CTerminal.trmMake ();  //TRM ターミナルウインドウ

    //JREのバージョンを確認する
    if (!prgCheckJava ()) {
      prgTini ();
      return;
    }

    //デバッグフラグを消し忘れないようにする
    if (prgIsLocal && (
      Keyboard.KBD_DEBUG_LED ||
      XEiJApplet.APP_DEBUG_TRACE ||
      FEFunction.FPK_DEBUG_TRACE ||
      HD63450.DMA_DEBUG_TRACE != 0 ||
      FDC.FDC_DEBUG_TRACE ||
      HDC.HDC_DEBUG_TRACE ||
      HDC.HDC_DEBUG_COMMAND ||
      SPC.SPC_DEBUG_TRACE ||
      SPC.SPC_DEBUG_SCSIROM ||
      Z8530.SCC_DEBUG_TRACE ||
      HFS.HFS_DEBUG_TRACE ||
      HFS.HFS_DEBUG_FILE_INFO ||
      HFS.HFS_COMMAND_TRACE ||
      HFS.HFS_BUFFER_TRACE ||
      M68kException.M6E_DEBUG_ERROR ||
      EFPBox.CIR_DEBUG_TRACE ||
      MC68060.MMU_DEBUG_COMMAND ||
      MC68060.MMU_DEBUG_TRANSLATION ||
      MC68060.MMU_NOT_ALLOCATE_CACHE)) {
      StringBuilder sb = new StringBuilder ("debug flags:");
      if (Keyboard.KBD_DEBUG_LED) {
        sb.append (" Keyboard.KBD_DEBUG_LED");
      }
      if (XEiJApplet.APP_DEBUG_TRACE) {
        sb.append (" XEiJApplet.APP_DEBUG_TRACE");
      }
      if (FEFunction.FPK_DEBUG_TRACE) {
        sb.append (" FEFunction.FPK_DEBUG_TRACE");
      }
      if (HD63450.DMA_DEBUG_TRACE != 0) {
        sb.append (" HD63450.DMA_DEBUG_TRACE");
      }
      if (FDC.FDC_DEBUG_TRACE) {
        sb.append (" FDC.FDC_DEBUG_TRACE");
      }
      if (HDC.HDC_DEBUG_TRACE) {
        sb.append (" HDC.HDC_DEBUG_TRACE");
      }
      if (HDC.HDC_DEBUG_COMMAND) {
        sb.append (" HDC.HDC_DEBUG_COMMAND");
      }
      if (SPC.SPC_DEBUG_TRACE) {
        sb.append (" SPC.SPC_DEBUG_TRACE");
      }
      if (SPC.SPC_DEBUG_SCSIROM) {
        sb.append (" SPC.SPC_DEBUG_SCSIROM");
      }
      if (Z8530.SCC_DEBUG_TRACE) {
        sb.append (" Z8530.SCC_DEBUG_TRACE");
      }
      if (HFS.HFS_DEBUG_TRACE) {
        sb.append (" HFS.HFS_DEBUG_TRACE");
      }
      if (HFS.HFS_DEBUG_FILE_INFO) {
        sb.append (" HFS.HFS_DEBUG_FILE_INFO");
      }
      if (HFS.HFS_COMMAND_TRACE) {
        sb.append (" HFS.HFS_COMMAND_TRACE");
      }
      if (HFS.HFS_BUFFER_TRACE) {
        sb.append (" HFS.HFS_BUFFER_TRACE");
      }
      if (M68kException.M6E_DEBUG_ERROR) {
        sb.append (" M68kException.M6E_DEBUG_ERROR");
      }
      if (EFPBox.CIR_DEBUG_TRACE) {
        sb.append (" EFPBox.CIR_DEBUG_TRACE");
      }
      if (MC68060.MMU_DEBUG_COMMAND) {
        sb.append (" MC68060.MMU_DEBUG_COMMAND");
      }
      if (MC68060.MMU_DEBUG_TRANSLATION) {
        sb.append (" MC68060.MMU_DEBUG_TRANSLATION");
      }
      if (MC68060.MMU_NOT_ALLOCATE_CACHE) {
        sb.append (" MC68060.MMU_NOT_ALLOCATE_CACHE");
      }
      JOptionPane.showMessageDialog (null, sb.toString ());
    }

    //動作を開始する
    //  イベントリスナーを設定する
    //  タイマーを起動する
    tmrStart ();  //TMR タイマー

    Keyboard.kbdStart ();  //KBD キーボード
    musStart ();  //MUS マウス
    pnlStart ();  //PNL パネル
    if (!prgIsApplet) {  //JNLPまたはローカルのとき
      frmStart ();  //FRM フレーム
    }
    SoundSource.sndStart ();  //SND サウンド

    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpStart ();  //DBP データブレークポイント
    }
    if (RasterBreakPoint.RBP_ON) {
      RasterBreakPoint.rbpStart ();  //RBP ラスタブレークポイント
    }
    if (ScreenModeTest.SMT_ON) {
      ScreenModeTest.smtStart ();  //SMT 表示モードテスト
    }
    SoundMonitor.smnStart ();  //SMN 音声モニタ
    RS232CTerminal.trmStart ();  //TRM ターミナルウインドウ
    ppiStart ();  //PPI PPI
    fntStart ();  //FNT フォント
    if (BranchLog.BLG_ON) {
      BranchLog.blgStart ();  //BLG 分岐ログ
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      ProgramFlowVisualizer.pfvStart ();  //PFV プログラムフロービジュアライザ
    }
    RegisterList.drpStart ();  //DRP レジスタ
    DisassembleList.ddpStart ();  //DDP 逆アセンブルリスト
    MemoryDumpList.dmpStart ();  //DMP メモリダンプリスト
    LogicalSpaceMonitor.atwStart ();  //ATW アドレス変換ウインドウ
    PhysicalSpaceMonitor.paaStart ();  //PAA 物理アドレス空間ウインドウ
    dgtStart ();  //DGT コンソール
    if (RootPointerList.RTL_ON) {
      RootPointerList.rtlStart ();  //RTL ルートポインタリスト
    }
    if (SpritePatternViewer.SPV_ON) {
      SpritePatternViewer.spvStart ();  //SPV スプライトパターンビュア
    }
    if (ATCMonitor.ACM_ON) {
      ATCMonitor.acmStart ();  //ACM アドレス変換キャッシュモニタ
    }

    if (Settings.sgsOpmtestOn) {  //OPMテスト
      OPMTest.otsTest ();
      prgTini ();
      return;
    }

    if (Settings.sgsSaveiconValue != null) {
      String[] a = Settings.sgsSaveiconValue.split (",");
      if (0 < a.length) {
        saveIcon (a[0], LnF.LNF_ICON_IMAGES);
        if (1 < a.length) {
          saveImage (LnF.LNF_ICON_IMAGE_16, a[1]);
          if (2 < a.length) {
            saveImage (LnF.LNF_ICON_IMAGE_24, a[2]);
            if (3 < a.length) {
              saveImage (LnF.LNF_ICON_IMAGE_32, a[3]);
              if (4 < a.length) {
                saveImage (LnF.LNF_ICON_IMAGE_48, a[4]);
                if (5 < a.length) {
                  saveImage (LnF.LNF_ICON_IMAGE_64, a[5]);
                }
              }
            }
          }
        }
      }
      prgTini ();
      return;
    }

    //コアを起動する
    mpuReset (-1, -1);

  }  //prgInit()

  //prgTini ()
  //  プログラムの後始末
  public static void prgTini () {
    try {
      SoundSource.sndTini ();  //SND サウンド
      FDC.fdcTini ();  //FDC FDコントローラ
      HDC.hdcTini ();  //HDC SASI HDコントローラ
      SPC.spcTini ();  //SPC SCSIプロトコルコントローラ
      HFS.hfsTini ();  //HFS ホストファイルシステムインタフェイス
      smrTini ();  //SMR SRAM
      tmrTini ();  //TMR タイマー
      Settings.sgsTini ();  //SGS 設定
    } catch (Exception e) {  //終了時に予期しないエラーが発生すると終了できなくなってしまうので、すべてのExceptionをcatchする
      e.printStackTrace ();
    }
    if (prgIsLocal) {  //ローカルのとき
      System.exit (0);
    }
  }  //prgTini()

  //prgMessage (s)
  //  冗長表示のときメッセージを出力する
  public static void prgMessage (String s) {
    if (prgVerbose) {
      RS232CTerminal.trmPrintln (s);
    }
  }  //prgMessage(s)

  //go = prgCheckJava ()
  //  Javaのバージョンを確認する
  //  続行するときtrueを返す
  public static boolean prgCheckJava () {
    String vendor = System.getProperty ("java.vendor");  //Javaのベンダー。Oracle Corporationなど
    if (vendor == null) {
      vendor = Multilingual.mlnJapanese ? "(不明なベンダー)" : "(unknown vendor)";
    }
    String version = System.getProperty ("java.version");  //Javaのバージョン。1.8.0_102など
    int v1 = 0;
    int v2 = 0;
    int v3 = 0;
    int v4 = 0;
    if (version != null) {
      //                            1             2             3           4
      Matcher m = Pattern.compile ("([0-9]+)(?:\\.([0-9]+)(?:\\.([0-9]+)(?:_([0-9]+))?)?)?").matcher (version);
      if (m.find ()) {
        v1 = m.group (1) != null ? Integer.parseInt (m.group (1)) : 0;
        v2 = m.group (2) != null ? Integer.parseInt (m.group (2)) : 0;
        v3 = m.group (3) != null ? Integer.parseInt (m.group (3)) : 0;
        v4 = m.group (4) != null ? Integer.parseInt (m.group (4)) : 0;
      } else {
        version = null;
      }
    }
    if (version == null) {
      version = Multilingual.mlnJapanese ? "(不明なバージョン)" : "(unknown version)";
    }
    prgJavaVendor = vendor;
    prgJavaVersion = version;

    String osArch = System.getProperty ("os.arch");  //os.archはJREのアーキテクチャであってOSのアーキテクチャではないことに注意
    prgArchitecture = osArch != null && osArch.endsWith ("64") ? "64" : "32";  //不明のときは32bitとみなす

    String message2 = Multilingual.mlnJapanese ? "古いバージョンのまま続行しますか？" : "Do you keep an old version up and continue?";
    Object[] messages;
    if (!vendor.equals (PRG_JAVA_VENDOR)) {
      messages = new Object[] {
        (Multilingual.mlnJapanese ?
         "このプログラムは " + PRG_JAVA_VENDOR + " の Java 実行環境で動作を確認しています。" :
         "This program has been tested on " + PRG_JAVA_VENDOR + "'s Java Runtime Environment."),
        (Multilingual.mlnJapanese ?
         vendor + " の Java では正常に動作しない可能性があります。" :
         vendor + "'s Java may cause a malfunction."),
        message2,
      };
    } else if (v1 == 0 ||  //不明なバージョン
               v1 == 1 && v2 < 8) {  //1.8未満
      messages = new Object[] {
        (Multilingual.mlnJapanese ?
         "このプログラムは Java バージョン " + PRG_JAVA_VERSION + " で動作を確認しています。" :
         "This program has been tested on Java version " + PRG_JAVA_VERSION + "."),
        (Multilingual.mlnJapanese ?
         "Java バージョン " + version + " では正常に動作しない可能性があります。" :
         "Java version " + version + " may cause a malfunction."),
        message2,
      };
    } else if (v1 == 1 && v2 == 8 && v3 == 0 && v4 < 101) {  //1.8.0以上1.8.0_101未満
      messages = new Object[] {
        (Multilingual.mlnJapanese ?
         "Java バージョン " + version + " には脆弱性があることが知られています。" :
         "There is a known vulnerability in Java version " + version + "."),
        (Multilingual.mlnJapanese ?
         "Java 実行環境を最新版に更新されることをお勧めします。" :
         "I recommend you update the Java Runtime Environment to the latest version."),
        message2,
      };
    } else {
      return true;
    }
    return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog (
      prgIsApplet ? null : frmFrame,
      messages,
      Multilingual.mlnJapanese ? "Javaの確認" : "Confirmation of Java",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE);
  }  //prgCheckJava()

  //prgOpenJavaDialog ()
  //  Java実行環境の情報
  public static void prgOpenJavaDialog () {
    JOptionPane.showMessageDialog (
      prgIsApplet ? null : frmFrame,
      createGridPanel (
        3, 5, "paddingLeft=6,paddingRight=6", "italic,right;left;left", "italic,center;colSpan=3,widen", "",
        null,  //(0,0)
        Multilingual.mlnJapanese ? "実行中" : "Running",  //(1,0)
        Multilingual.mlnJapanese ? "推奨" : "Recommended",  //(2,0)
        createHorizontalSeparator (),  //(0,1)
        Multilingual.mlnJapanese ? "Java のベンダー" : "Java Vendor",  //(0,2)
        prgJavaVendor,  //(1,2)
        PRG_JAVA_VENDOR,  //(2,2)
        Multilingual.mlnJapanese ? "Java のバージョン" : "Java Version",  //(0,3)
        prgJavaVersion,  //(1,3)
        PRG_JAVA_VERSION,  //(2,3)
        Multilingual.mlnJapanese ? "アーキテクチャ" : "Architecture",  //(0,4)
        prgArchitecture + (Multilingual.mlnJapanese ? " ビット" : "-bit"),  //(1,4)
        PRG_ARCHITECTURE + (Multilingual.mlnJapanese ? " ビット" : "-bit")  //(2,4)
        ),
      Multilingual.mlnJapanese ? "Java 実行環境の情報" : "Java Runtime Environment Information",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenJavaDialog()

  //prgOpenAboutDialog ()
  //  バージョン情報
  public static void prgOpenAboutDialog () {
    JOptionPane.showMessageDialog (
      prgIsApplet ? null : frmFrame,
      createGridPanel (
        2, 4, "paddingLeft=6,paddingRight=6", "italic,right;left", "", "",
        Multilingual.mlnJapanese ? "タイトル" : "Title"  ,  //(0,0)
        PRG_TITLE,  //(1,0)
        Multilingual.mlnJapanese ? "バージョン" : "Version",  //(0,1)
        PRG_VERSION,  //(1,1)
        Multilingual.mlnJapanese ? "作者" : "Author" ,  //(0,2)
        "Makoto Kamada",  //(1,2)
        Multilingual.mlnJapanese ? "ウェブページ" : "Webpage",  //(0,3)
        "http://stdkmd.com/xeij/"  //(1,3)
        ),
      Multilingual.mlnJapanese ? "バージョン情報" : "Version Information",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenAboutDialog()

  //prgOpenXEiJLicenseDialog ()
  //  XEiJ使用許諾条件ダイアログ
  public static void prgOpenXEiJLicenseDialog () {
    JOptionPane.showMessageDialog (
      prgIsApplet ? null : frmFrame,
      createScrollTextPane (ismGetResourceText ("license_XEiJ.txt", "UTF-8"), 550, 300),
      Multilingual.mlnJapanese ? "XEiJ 使用許諾条件" : "XEiJ License",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenXEiJLicenseDialog()

  //prgOpenSHARPLicenseDialog ()
  //  許諾条件.txtダイアログ
  public static void prgOpenSHARPLicenseDialog () {
    JOptionPane.showMessageDialog (
      prgIsApplet ? null : frmFrame,
      createScrollTextPane (ismGetResourceText ("license_FSHARP.txt", "Shift_JIS"), 550, 300),
      Multilingual.mlnJapanese ? "無償公開された X68000 の基本ソフトウェア製品の許諾条件" : "License of the basic software products for X68000 that were distributed free of charge",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenSHARPLicenseDialog()

  //prgOpenMAMELicenseDialog ()
  //  license.txtダイアログ
  public static void prgOpenMAMELicenseDialog () {
    JOptionPane.showMessageDialog (
      prgIsApplet ? null : frmFrame,
      createScrollTextPane (ismGetResourceText ("license_MAME.txt", "UTF-8"), 550, 300),
      "MAME License",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenMAMELicenseDialog()

  //prgPrintClass (o)
  //  オブジェクトを表示する
  public static void prgPrintClass (Object o) {
    System.out.println (o.toString ());
    //スーパークラスを遡る
    try {
      Stack<Class<?>> s = new Stack<Class<?>> ();
      for (Class<?> c = o.getClass (); c != null; c = c.getSuperclass ()) {
        s.push (c);
      }
      for (int i = 0; !s.empty (); i++) {
        for (int j = 0; j < i; j++) {
          System.out.print ("  ");
        }
        System.out.println (s.pop ().getName ());
      }
    } catch (Exception e) {
    }
  }  //prgPrintClass(Object)

  //prgPrintStackTrace ()
  //  スタックトレースを表示する
  //  メソッドがどのような経路で呼び出されたか確認したいときに使う
  public static void prgPrintStackTrace () {
    Exception e = new Exception ();
    e.fillInStackTrace ();
    prgPrintStackTraceOf (e);
  }  //prgPrintStackTrace()
  public static void prgPrintStackTraceOf (Exception e) {
    //e.printStackTrace ();
    prgMessage ("------------------------------------------------");
    prgMessage (e.toString ());
    prgMessage ("\t" + e.getMessage ());
    for (StackTraceElement ste : e.getStackTrace ()) {
      prgMessage ("\tat " + ste.toString ());
    }
    prgMessage ("------------------------------------------------");
  }  //prgPrintStackTraceOf()

  //prgStopOnce ()
  //  1回目だけ停止する。2回目以降は何もしない
  //  特定の条件で止めて近くにブレークポイントを仕掛けたいときに使う
  public static boolean prgStopDone = false;
  public static void prgStopOnce () {
    if (!prgStopDone) {
      prgStopDone = true;
      mpuStop (null);
    }
  }  //prgStopOnce()



  //========================================================================================
  //$$TMR タイマ
  //  tmrTimerは1つだけ存在する
  //  1つのタイマにスケジュールされたタスクはオーバーラップしない
  //  固定遅延実行
  //    tmrTimer.schedule (task, delay, interval)
  //    次回の実行開始予定時刻=max(今回の実行終了時刻,今回の実行開始時刻+interval)
  //  固定頻度実行
  //    tmrTimer.scheduleAtFixedRate (task, delay, interval)
  //    次回の実行開始予定時刻=max(今回の実行終了時刻,初回の実行開始時刻+interval*今回までの実行回数)

  //時刻の周波数
  //  mpuClockTimeなどのカウンタが1秒間に進む数
  //  10^10のとき
  //    1周期   0.1nanosecond
  //    2^31-1  0.2second
  //    2^53-1  10day
  //    2^63-1  29year
  //  10^11のとき
  //    1周期   10picosecond
  //    2^31-1  21millisecond
  //    2^53-1  1day
  //    2^63-1  2.9year
  //  10^12のとき
  //    1周期   1picosecond
  //    2^31-1  2.1millisecond
  //    2^53-1  2.5hour
  //    2^63-1  3.5month
  public static final long TMR_FREQ = 1000000000000L;  //10^12Hz。1ps

  //メインタイマ
  public static final long TMR_DELAY = 10L;  //ms
  public static final long TMR_INTERVAL = 10L;  //ms

  //タイマ
  public static java.util.Timer tmrTimer;  //Timerだけだとjavax.swing.Timerと紛らわしい

  //tmrStart ()
  //  タイマを開始する
  public static void tmrStart () {
    tmrTimer = new java.util.Timer ();  //Timerだけだとjavax.swing.Timerと紛らわしい
  }  //tmrStart()

  //tmrTini ()
  //  タイマの後始末
  public static void tmrTini () {
    if (tmrTimer != null) {
      tmrTimer.cancel ();
    }
  }  //tmrTini()



  //========================================================================================
  //$$PNL パネル
  //
  //  固定倍率のとき
  //    パネルの最小サイズはスクリーンのサイズに固定倍率を掛けて切り上げた値
  //    スクリーンの表示サイズはスクリーンのサイズに固定倍率を掛けて丸めた値
  //  ウインドウに合わせるとき
  //    倍率は
  //      パネルの幅をスクリーンの幅で割った結果
  //      パネルの高さからキーボードの高さを引いてスクリーンの高さで割った結果
  //    のどちらか小さい方
  //
  //  スクリーンの大きさに固定倍率を掛けて丸めた値から倍率を逆算すると固定倍率よりも小さくなってしまう場合がある
  //
  //  全画面表示
  //    キーボードと合わせてパネルにちょうど入り切るようにスクリーンの拡大率と表示位置を計算する
  //  ウインドウに合わせる
  //    可能ならばユーザがパネルの大きさを変更できるようにする
  //    パネルの大きさが最小倍率で入り切らないとき
  //      プログラムがパネルの大きさを変更できるとき
  //        パネルの大きさを最小倍率でちょうど入り切る大きさに変更する
  //      パネルの中央に表示する
  //    パネルの大きさが最小倍率で入り切るとき
  //      パネルの大きさに合わせてスクリーンを拡大縮小する
  //  固定倍率
  //    可能ならばユーザがパネルの大きさを変更できないようにする
  //    プログラムがパネルの大きさを変更できるとき
  //      パネルの大きさを固定倍率でちょうど入り切る大きさに変更する
  //    スクリーンを拡大縮小してキーボードと一緒にパネルの中央に表示する


  //ビットマップのサイズ
  public static final int PNL_BM_OFFSET_BITS = 10;
  public static final int PNL_BM_WIDTH = 1 << PNL_BM_OFFSET_BITS;
  public static final int PNL_BM_HEIGHT = 1024;

  //水平方向の拡大率
  //  [CRTC.crtHRLCurr<<2|CRTC.crtHResoCurr]
  public static final float[] PNL_STRETCH_BASE = {
    8.0F / 3.0F,  //HRL=0,width=256
    4.0F / 3.0F,  //HRL=0,width=512
    1.0F,         //HRL=0,width=768
    1.0F,         //HRL=0,width=768
    4.0F,         //HRL=1,width=256
    2.0F,         //HRL=1,width=512
    3.0F / 2.0F,  //HRL=1,width=768
    3.0F / 2.0F,  //HRL=1,width=768
  };

  //サイズと位置
  public static int pnlScreenWidth;  //X68000から見た表示領域のサイズ。幅は常に8の倍数
  public static int pnlScreenHeight;
  public static float pnlStretchMode;  //水平方向の拡大率。PNL_STRETCH_BASE[CRTC.crtHRLCurr<<2|CRTC.crtHResoCurr]
  public static int pnlStretchWidth;  //ピクセルの縦横比に合わせて伸縮された表示領域の幅。Math.round((float)pnlScreenWidth*pnlStretchMode)
  public static int pnlZoomWidth;  //描画サイズ。pnlStretchWidth,pnlScreenHeightを同じ比率で拡大
  public static int pnlZoomHeight;
  public static int pnlZoomRatioOut;  //65536*pnlZoomHeight/pnlScreenHeight
  public static int pnlZoomRatioInX;  //65536*pnlScreenWidth/pnlZoomWidth
  public static int pnlZoomRatioInY;  //65536*pnlScreenHeight/pnlZoomHeight
  public static int pnlWidth;  //パネルのサイズ
  public static int pnlHeight;
  public static Dimension pnlSize;  //パネルの推奨サイズ。pnlWidth,pnlHeight
  public static int pnlScreenX;  //スクリーンの表示位置。pnlWidth-pnlStretchWidth>>1,pnlHeight-pnlScreenHeight>>1
  public static int pnlScreenY;
  public static int pnlKeyboardX;  //キーボードの表示位置。pnlUpdateArrangement()が設定する
  public static int pnlKeyboardY;
  public static int pnlMinimumWidth;  //パネルの最小サイズ
  public static int pnlMinimumHeight;
  public static int pnlGlobalX;  //画面上の表示位置。アプレットではコンポーネントリスナーが動作しないのでコマンドラインのみ
  public static int pnlGlobalY;

  //モード
  public static final boolean PNL_FILL_BACKGROUND = true;  //true=常に背景を塗り潰してから描画する
  public static boolean pnlFillBackgroundRequest;  //true=次回のpaintで背景を塗り潰す
  public static boolean pnlIsFullscreenSupported;  //true=全画面表示に移行できる
  public static boolean pnlFullscreenOn;  //true=全画面表示
  public static boolean pnlIsFitInWindowSupported;  //true=ウインドウに合わせられる
  public static boolean pnlFitInWindowOn;  //true=ウインドウに合わせる
  public static boolean pnlPrevFitInWindowOn;  //全画面表示にする前のpnlFitInWindowOn

  //補間アルゴリズム
  //  RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR  軽い
  //  RenderingHints.VALUE_INTERPOLATION_BILINEAR          頑張る
  //  RenderingHints.VALUE_INTERPOLATION_BICUBIC           アプレットだと重すぎる？
  public static Object pnlInterpolation;  //補間アルゴリズム

  //イメージとビットマップ
  public static BufferedImage pnlScreenImage;  //イメージ。サイズは1024x1024に固定
  public static int[] pnlBM;  //ビットマップ

  //パネル
  public static JPanel pnlPanel;  //パネル

  //メニュー
  public static int pnlFixedScale;
  public static SpinnerNumberModel pnlFixedModel;
  public static JSpinner pnlFixedSpinner;
  public static Box pnlFixedBox;

  //pnlInit ()
  //  パネルのフィールドを初期化する
  public static void pnlInit () {

    //サイズと位置
    pnlScreenWidth = 768;
    pnlScreenHeight = 512;
    pnlStretchMode = 1.0F;
    pnlStretchWidth = Math.round ((float) pnlScreenWidth * pnlStretchMode);
    pnlZoomWidth = pnlStretchWidth;
    pnlZoomHeight = pnlScreenHeight;
    pnlZoomRatioOut = (pnlZoomHeight << 16) / pnlScreenHeight;
    pnlZoomRatioInX = (pnlScreenWidth << 16) / pnlZoomWidth;
    pnlZoomRatioInY = (pnlScreenHeight << 16) / pnlZoomHeight;
    pnlWidth = Math.max (pnlZoomWidth, Keyboard.kbdWidth);
    pnlHeight = pnlZoomHeight + Keyboard.kbdHeight;
    pnlSize = new Dimension (pnlWidth, pnlHeight);
    pnlScreenX = pnlWidth - pnlStretchWidth >> 1;
    pnlScreenY = 0;
    pnlKeyboardX = pnlWidth - Keyboard.kbdWidth >> 1;
    pnlKeyboardY = pnlZoomHeight;
    pnlMinimumWidth = Math.max (256, Keyboard.kbdWidth);
    pnlMinimumHeight = 64 + Keyboard.kbdHeight;
    pnlGlobalX = 0;
    pnlGlobalY = 0;

    //モード
    if (!PNL_FILL_BACKGROUND) {
      pnlFillBackgroundRequest = true;
    }

    //イメージとビットマップ
    pnlScreenImage = new BufferedImage (PNL_BM_WIDTH, PNL_BM_HEIGHT, BufferedImage.TYPE_INT_RGB);
    pnlBM = ((DataBufferInt) pnlScreenImage.getRaster ().getDataBuffer ()).getData ();
    //Arrays.fill (bm, 0xff330000);

    //メニュー
    pnlFixedModel = new SpinnerNumberModel (pnlFixedScale, 10, 1000, 1);
    pnlFixedSpinner = createNumberSpinner (pnlFixedModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        mnbFixedSizeMenuItem.setSelected (true);
        pnlSetFitInWindowOn (false);
        pnlSetFullscreenOn (false);
        pnlUpdateArrangement ();
      }
    });
    pnlFixedBox = createHorizontalBox (
      Box.createHorizontalStrut (20),
      pnlFixedSpinner,
      createLabel ("%"),
      Box.createHorizontalGlue ()
      );

  }  //pnlInit()

  //pnlMake ()
  //  パネルを作る
  public static void pnlMake () {

    //パネル
    pnlPanel = new JPanel () {
      @Override protected void paintComponent (Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (PNL_FILL_BACKGROUND || pnlFillBackgroundRequest) {  //背景を塗り潰す
          if (!PNL_FILL_BACKGROUND) {
            pnlFillBackgroundRequest = false;
          }
          g2.setColor (Color.black);
          g2.fillRect (0, 0, pnlWidth, pnlHeight);
        }
        g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, pnlInterpolation);
        g2.drawImage (pnlScreenImage,
                      pnlScreenX, pnlScreenY, pnlScreenX + pnlZoomWidth, pnlScreenY + pnlZoomHeight,
                      0, 0, pnlScreenWidth, pnlScreenHeight,
                      null);
        g2.drawImage (Keyboard.kbdImage, pnlKeyboardX, pnlKeyboardY, null);  //Graphics.drawImage()はimage==nullのとき何もしない
      }
      @Override protected void paintBorder (Graphics g) {
      }
      @Override protected void paintChildren (Graphics g) {
      }
    };
    pnlPanel.setBackground (Color.black);
    pnlPanel.setOpaque (true);
    pnlPanel.setPreferredSize (pnlSize);

    if (musCursorAvailable) {
      pnlPanel.setCursor (musCursorArray[1]);  //矢印カーソルを表示する
    }

  }  //pnlMake()

  //pnlStart ()
  //  パネルの動作を開始する
  //  イベントリスナーを設定する
  public static void pnlStart () {

    //コンポーネントリスナー
    addRemovableListener (
      pnlPanel,
      new ComponentAdapter () {
        @Override public void componentResized (ComponentEvent ce) {
          pnlWidth = pnlPanel.getWidth ();
          pnlHeight = pnlPanel.getHeight ();
          pnlUpdateArrangement ();
        }
      });

  }  //pnlStart()

  //pnlSetFullscreenOn (on)
  //  全画面表示を設定する
  public static void pnlSetFullscreenOn (boolean on) {
    if (pnlIsFullscreenSupported && pnlFullscreenOn != on) {
      if (prgIsApplet) {  //アプレットのとき
        XEiJApplet.appStandbyFullscreen (on);
      } else {  //JNLPまたはローカルのとき
        if (on) {  //OFF→ON
          pnlFullscreenOn = true;
          mnbFullscreenMenuItem.setSelected (true);
          frmSetFullscreenOn (true);  //全画面表示に移行する
          pnlPrevFitInWindowOn = pnlFitInWindowOn;  //全画面表示に移行する前にウインドウに合わせていたか
          pnlSetFitInWindowOn (true);  //ウインドウに合わせる
        } else {  //ON→OFF
          pnlFullscreenOn = false;
          if (!pnlPrevFitInWindowOn) {  //全画面表示に移行する前はウインドウに合わせていなかった
            pnlSetFitInWindowOn (false);  //ウインドウに合わせない
          }
          frmSetFullscreenOn (false);  //全画面表示を解除する
        }
      }
    }
  }  //pnlSetFullscreenOn(boolean)

  //pnlSetFitInWindowOn (mode)
  //  ウインドウに合わせるモードを設定する
  //  ウインドウに合わせるモードには全画面表示が含まれる
  public static void pnlSetFitInWindowOn (boolean on) {
    if (pnlIsFitInWindowSupported && pnlFitInWindowOn != on) {
      pnlFitInWindowOn = on;
      pnlUpdateArrangement ();  //スクリーンとキーボードの配置を再計算する
      if (prgIsApplet) {  //アプレットのとき
        XEiJApplet.appSetFitInWindowOn (on, pnlMinimumWidth, mnbMenuBar.getHeight () + pnlMinimumHeight);
      } else {  //JNLPまたはローカルのとき
        if (!pnlFullscreenOn) {  //全画面表示でないとき
          if (on) {
            mnbFitInWindowMenuItem.setSelected (true);
          } else {
            mnbFixedSizeMenuItem.setSelected (true);
          }
        }
      }
    }
  }  //pnlSetFitInWindowOn(boolean)

  //pnlUpdateArrangement ()
  //  スクリーンとキーボードの配置を再計算する
  //    リサイズ、最大化、全画面表示などの操作でパネルの大きさが変わったとき
  //    ウインドウに合わせるかどうかが変わったとき
  //    ウインドウに合わせないが固定倍率が変わったとき
  //    キーボードの有無または種類が変わったとき
  //    X68000の画面モードが変更されてスクリーンの大きさが変わったとき
  public static void pnlUpdateArrangement () {
    pnlFixedScale = pnlFixedModel.getNumber ().intValue ();  //固定サイズの倍率
    //スクリーンとキーボードの配置を決める
    if (!pnlFitInWindowOn) {  //固定倍率のとき
      //配置の計算
      //perl optdiv.pl 32768 100
      //  x/100==x*5243>>>19 (0<=x<=43698) [32768*5243==171802624]
      //pnlZoomWidth = (pnlStretchWidth * pnlFixedScale + 50) / 100;
      //pnlZoomHeight = (pnlScreenHeight * pnlFixedScale + 50) / 100;
      pnlZoomWidth = (pnlStretchWidth * pnlFixedScale + 50) * 5243 >>> 19;
      pnlZoomHeight = (pnlScreenHeight * pnlFixedScale + 50) * 5243 >>> 19;
      int width = Math.max (Math.max (256, pnlZoomWidth), Keyboard.kbdWidth);
      int height = Math.max (64, pnlZoomHeight) + Keyboard.kbdHeight;
      pnlScreenX = width - pnlZoomWidth >> 1;
      pnlScreenY = height - pnlZoomHeight - Keyboard.kbdHeight >> 1;
      if (pnlWidth != width || pnlHeight != height) {  //パネルの大きさが合っていないとき
        pnlWidth = width;
        pnlHeight = height;
        pnlMinimumWidth = width;  //固定サイズでは使わないがウインドウに合わせるモードに移行したとき最小サイズが変化したことを検知できるようにする
        pnlMinimumHeight = height;
        pnlSize.setSize (width, height);
        //最小サイズと最大サイズ
        if (!prgIsApplet) {  //JNLPまたはローカルのとき
          frmMinimumSize.setSize (width + frmMarginWidth, height + frmMarginHeight);
          frmFrame.setMinimumSize (frmMinimumSize);
          frmFrame.setMaximumSize (frmMinimumSize);
          frmFrame.setPreferredSize (frmMinimumSize);
          frmFrame.setResizable (false);
          pnlPanel.setMinimumSize (pnlSize);
          pnlPanel.setMaximumSize (pnlSize);
          pnlPanel.setPreferredSize (pnlSize);
          frmFrame.pack ();
        } else if (XEiJApplet.appLiveConnectSupported) {  //アプレットでLiveConnectが動作するとき
          XEiJApplet.appSetSize (width, mnbMenuBar.getHeight () + height);
          XEiJApplet.appSetMinSize (width, mnbMenuBar.getHeight () + height);
        }
      }
    } else {  //ウインドウに合わせるとき
      //配置の計算
      if (pnlWidth * pnlScreenHeight >= (pnlHeight - Keyboard.kbdHeight) * pnlStretchWidth) {  //ウインドウに合わせると上下に隙間ができないとき
        //パネルの下端にキーボード配置して残った部分にスクリーンを目一杯拡大する
        //    pnlScreenX                                          pnlScreenX            pnlScreenX
        //    |pnlZoomWidth|    |pnlZoomWidth|  |pnlZoomWidth|    |pnlZoomWidth|        |pnlZoomWidth|
        //  +-+------------+-+  +------------+  +------------+  +-+------------+-+  +---+------------+---+ --
        //  | |            | |  |            |  |            |  | |            | |  |   |            |   |
        //  | |   screen   | |  |   screen   |  |   screen   |  | |   screen   | |  |   |   screen   |   | pnlZoomHeight
        //  | |            | |  |            |  |            |  | |            | |  |   |            |   |
        //  | +-+--------+-+ |  +-+--------+-+  +------------+  +-+------------+-+  | +-+------------+-+ | -- pnlKeyboardY
        //  |   |keyboard|   |  | |keyboard| |  |  keyboard  |  |    keyboard    |  | |    keyboard    | | kbdHeight
        //  +---+--------+---+  +-+--------+-+  +------------+  +----------------+  +-+----------------+-+ --
        //      |                 |                             |    kbdWidth    |    |    kbdWidth    |
        //      pnlKeyboardX      pnlKeyboardX                                        pnlKeyboardX
        pnlZoomHeight = pnlHeight - Keyboard.kbdHeight;
        pnlZoomWidth = (pnlZoomHeight * pnlStretchWidth + (pnlScreenHeight >> 1)) / pnlScreenHeight;
        pnlScreenX = pnlWidth - pnlZoomWidth >> 1;
        pnlScreenY = 0;
      } else {  //ウインドウに合わせると上下に隙間ができるとき
        //左右が先につっかえたのだからスクリーンの幅がキーボードの幅よりも狭いということはない
        //  スクリーンの幅がキーボードの幅よりも狭かったらスクリーンの上と左右の両方に隙間があることになってしまうのでウインドウに合っていない
        //  |pnlZoomWidth|  |pnlZoomWidth|
        //  +------------+  +------------+
        //  |            |  |            |
        //  +------------+  +------------+ -- pnlScreenY
        //  |            |  |            |
        //  |   screen   |  |   screen   | pnlZoomHeight
        //  |            |  |            |
        //  +-+--------+-+  +------------+ -- pnlKeyboardY
        //  | |keyboard| |  |  keyboard  | kbdHeight
        //  | +--------+ |  +------------+ --
        //  |            |  |            |
        //  +------------+  +------------+
        //    |kbdWidth|    |  kbdWidth  |
        //    pnlKeyboardX
        pnlZoomWidth = pnlWidth;
        pnlZoomHeight = (pnlZoomWidth * pnlScreenHeight + (pnlStretchWidth >> 1)) / pnlStretchWidth;
        pnlScreenX = 0;
        pnlScreenY = pnlHeight - pnlZoomHeight - Keyboard.kbdHeight >> 1;
      }
      //最小サイズと最大サイズ
      int minimumWidth = Math.max (256, Keyboard.kbdWidth);
      int minimumHeight = 64 + Keyboard.kbdHeight;
      if (pnlMinimumWidth != minimumWidth || pnlMinimumHeight != minimumHeight) {  //最小サイズが変化した。ウインドウに合わせるモードに移行したかキーボードの有無または種類が変わった
        pnlMinimumWidth = minimumWidth;
        pnlMinimumHeight = minimumHeight;
        if (!prgIsApplet) {  //JNLPまたはローカルのとき
          frmMinimumSize.setSize (minimumWidth + frmMarginWidth, minimumHeight + frmMarginHeight);
          frmFrame.setMinimumSize (frmMinimumSize);
          frmFrame.setMaximumSize (null);
          frmFrame.setResizable (true);
        } else if (XEiJApplet.appLiveConnectSupported) {  //アプレットでLiveConnectが動作するとき
          XEiJApplet.appSetMinSize (minimumWidth, mnbMenuBar.getHeight () + minimumHeight);
        }
      }
    }
    pnlKeyboardX = pnlWidth - Keyboard.kbdWidth >> 1;
    pnlKeyboardY = pnlScreenY + pnlZoomHeight;
    pnlZoomRatioOut = (pnlZoomHeight << 16) / pnlScreenHeight;
    pnlZoomRatioInX = (pnlScreenWidth << 16) / pnlZoomWidth;
    pnlZoomRatioInY = (pnlScreenHeight << 16) / pnlZoomHeight;
    Z8530.scc0UpdateRatio ();
    if (!PNL_FILL_BACKGROUND) {
      pnlFillBackgroundRequest = true;
    }
    pnlPanel.repaint ();
  }  //pnlUpdateArrangement()



  //========================================================================================
  //$$RBT ロボット

  public static Robot rbtRobot;  //ロボット

  //rbtInit ()
  public static void rbtInit () {

    //ロボット
    rbtRobot = null;
    try {
      rbtRobot = new Robot ();  //プライマリスクリーンのみを対象とする
    } catch (Exception e) {  //アプレットのときjava.security.AccessControlExceptionが出る
    }

  }  //rbtInit()



  //========================================================================================
  //$$MUS マウス
  //  パネルのマウスカーソルを変更する
  //    X68000のマウスカーソルが表示されているときはホストのマウスカーソルを消す
  //    X68000のマウスカーソルが表示されていないときはホストのマウスカーソルをX68000と同じサイズにする
  //  SCCから要求があったときマウスデータを作る
  //    シームレスモードのときは逆アクセラレーションを行う
  //      X68000のマウスカーソルがホストのマウスカーソルの真下に来るようにマウスの移動データを作る
  //  キーボードが操作されたときはキーボードにデータを渡す

  //逆アクセラレーション
  //  ROMのアクセラレーション処理のコード
  //    ;<d0.w:移動量
  //    ;>d0.w:移動量
  //    ;?d1
  //    accelerate:
  //        clr.w   -(sp)
  //        tst.w   d0
  //        bgt.s   1f
  //        addq.w  #1,(sp)
  //        neg.w   d0
  //    1:  move.w  d0,d1   ;  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
  //        lsr.w   #3,d1   ;  0  0  0  0  0  0  0  0  1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  3  3
  //        bne.s   2f
  //        move.w  #1,d1   ;  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  3  3
  //    2:  mulu.w  d1,d0   ;  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 32 34 36 38 40 42 44 46 72 75
  //        move.w  d0,d1
  //        lsr.w   #2,d1   ;  0  0  0  0  1  1  1  1  2  2  2  2  3  3  3  3  8  8  9  9 10 10 11 11 18 18
  //        add.w   d1,d0   ;  0  1  2  3  5  6  7  8 10 11 12 13 15 16 17 18 40 42 45 47 50 52 55 57 90 93
  //        tst.w   (sp)+
  //        beq.s   3f
  //        neg.w   d0
  //    3:  rts
  //  アクセラレーションテーブル
  //    変位:移動距離
  //      0:   0   1:   1   2:   2   3:   3   4:   5   5:   6   6:   7   7:   8   8:  10   9:  11
  //     10:  12  11:  13  12:  15  13:  16  14:  17  15:  18  16:  40  17:  42  18:  45  19:  47
  //     20:  50  21:  52  22:  55  23:  57  24:  90  25:  93  26:  97  27: 101  28: 105  29: 108
  //     30: 112  31: 116  32: 160  33: 165  34: 170  35: 175  36: 180  37: 185  38: 190  39: 195
  //     40: 250  41: 256  42: 262  43: 268  44: 275  45: 281  46: 287  47: 293  48: 360  49: 367
  //     50: 375  51: 382  52: 390  53: 397  54: 405  55: 412  56: 490  57: 498  58: 507  59: 516
  //     60: 525  61: 533  62: 542  63: 551  64: 640  65: 650  66: 660  67: 670  68: 680  69: 690
  //     70: 700  71: 710  72: 810  73: 821  74: 832  75: 843  76: 855  77: 866  78: 877  79: 888
  //     80:1000  81:1012 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //    ~~~~~~~~~~~~~~~~~~ 82:1025  83:1037  84:1050  85:1062  86:1075  87:1087  88:1210  89:1223
  //     90:1237  91:1251  92:1265  93:1278  94:1292  95:1306  96:1440  97:1455  98:1470  99:1485
  //    100:1500 101:1515 102:1530 103:1545 104:1690 105:1706 106:1722 107:1738 108:1755 109:1771
  //    110:1787 111:1803 112:1960 113:1977 114:1995 115:2012 116:2030 117:2047 118:2065 119:2082
  //    120:2250 121:2268 122:2287 123:2306 124:2325 125:2343 126:2362 127:2381 128:2560
  //  逆アクセラレーション
  //    現在のマウスカーソルの位置とX68000のマウス座標の距離を超えない最大の移動距離をアクセラレーションテーブルから探してその変位に符号を付けて返す
  //    例えば距離が639ピクセルのとき63(639-551=88),23(88-57=31),15(31-18=13),11(13-13=0)の4回で移動が完了する
  public static final int[] MUS_DEACCELERATION_TABLE = new int[1025];  //逆アクセラレーションテーブル

  //マウスカーソル
  public static final String[][] MUS_CURSOR_PATTERN = {
    {
    },
    {
      "00.........",
      "010........",
      "0110.......",
      "01110......",
      "011110.....",
      "0111110....",
      "01111110...",
      "011111110..",
      "0111111110.",
      "01111100000",
      "0110110....",
      "010.0110...",
      "00..0110...",
      ".....0110..",
      ".....0110..",
      "......00...",
    },
  };

  //モード
  //  シームレス
  //    X68000のマウスカーソルが動作環境のマウスポインタを追いかける
  //  エクスクルーシブ
  //    動作環境のマウスポインタを独占して相対座標を利用できるようにする
  public static boolean musSeamlessOn;  //true=シームレス,false=エクスクルーシブ
  public static boolean musExclusiveStart;  //true=エクスクルーシブに切り替えた直後
  public static boolean musEdgeAccelerationOn;  //true=シームレスのとき縁部加速を行う
  public static boolean musHostsPixelUnitsOn;  //true=エクスクルーシブのときマウスはホストの画素単位で動く,false=X68000の画素単位で動く

  //マウスの状態
  public static boolean musButtonLeft;  //マウスの左ボタンの状態。true=押されている。スクリーン上になくても有効
  public static boolean musButtonRight;  //マウスの右ボタンの状態。true=押されている。スクリーン上になくても有効
  public static int musDataLeft;  //マウスデータの左ボタンの状態。1=押されている
  public static int musDataRight;  //マウスデータの右ボタンの状態。2=押されている
  public static int musPanelX;  //パネル座標
  public static int musPanelY;
  public static int musScreenX;  //スクリーン座標。スクリーン上にあるとは限らない
  public static int musScreenY;
  public static boolean musOnScreen;  //true=マウスカーソルがスクリーン上にある
  public static boolean musOnKeyboard;  //true=マウスカーソルがキーボード上にある

  //マウスカーソル
  public static boolean musCursorAvailable;  //true=カスタムカーソルを利用できる
  public static int musCursorNumber;  //表示されているマウスカーソルの番号
  public static Cursor[] musCursorArray;  //マウスカーソルの配列

  //musInit ()
  //  マウスを初期化する
  public static void musInit () {
    //musSeamlessOn = true;
    musExclusiveStart = false;
    //musEdgeAccelerationOn = false;
    //musHostsPixelUnitsOn = false;
    musButtonLeft = false;
    musButtonRight = false;
    musDataLeft = 0;
    musDataRight = 0;
    musPanelX = 0;
    musPanelY = 0;
    musScreenX = 0;
    musScreenY = 0;
    musOnScreen = false;
    musOnKeyboard = false;
    //逆アクセラレーション
    {
      int index = 0;
      for (int delta = 0; delta <= 81; delta++) {
        int next = delta + 1;
        if (next >= 8) {
          next *= next >> 3;
        }
        next += next >> 2;
        while (index < next) {  //delta==81のときnext==1025
          MUS_DEACCELERATION_TABLE[index++] = delta;
        }
      }
    }
    //マウスカーソル
    musCursorAvailable = false;
    try {
      Toolkit toolkit = Toolkit.getDefaultToolkit ();
      Dimension bestCursorSize = toolkit.getBestCursorSize (16, 16);
      int width = bestCursorSize.width;
      int height = bestCursorSize.height;
      if (width >= 16 && height >= 16) {  //カスタムカーソルを利用できるとき
        BufferedImage cursorImage = new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB);
        int[] cursorBitmap = ((DataBufferInt) cursorImage.getRaster ().getDataBuffer ()).getData ();
        Point point = new Point (0, 0);
        musCursorArray = new Cursor[MUS_CURSOR_PATTERN.length];
        for (int i = 0; i < MUS_CURSOR_PATTERN.length; i++) {
          String[] ss = MUS_CURSOR_PATTERN[i];
          int h = ss.length;
          for (int y = 0; y < height; y++) {
            String s = y < h ? ss[y] : "";
            int w = s.length ();
            for (int x = 0; x < width; x++) {
              char c = x < w ? s.charAt (x) : '.';
              cursorBitmap[x + width * y] = 0xff000000 & ('.' - c) | -(c & 1);
            }
          }
          musCursorArray[i] = toolkit.createCustomCursor (cursorImage, point, "XEiJ_" + i);
        }
        musCursorAvailable = true;
        musCursorNumber = 1;
      }
    } catch (Exception e) {
    }
  }  //musInit()

  //musStart ()
  //  マウスの動作を開始する
  public static void musStart () {

    //マウスリスナー、マウスモーションリスナー、マウスホイールリスナー
    addRemovableListener (
      pnlPanel,
      new MouseAdapter () {
        @Override public void mouseClicked (MouseEvent me) {
          if (!pnlPanel.isFocusOwner ()) {
            pnlPanel.requestFocusInWindow ();
          }
        }
        //@Override public void mouseEntered (MouseEvent me) {
        //}
        @Override public void mouseExited (MouseEvent me) {
          if (musOnScreen) {  //スクリーンから出た
            musOnScreen = false;
          }
          if (musOnKeyboard) {  //キーボードから出た
            musOnKeyboard = false;
            if (Keyboard.kbdPointedIndex >= 0) {  //ポイントされているキーがある
              Keyboard.kbdHover (0, 0);  //ポイントを解除する
            }
          }
        }
        @Override public void mousePressed (MouseEvent me) {
          musPressedOrReleased (me, true);  //マウスのボタンが操作された
        }
        @Override public void mouseReleased (MouseEvent me) {
          musPressedOrReleased (me, false);  //マウスのボタンが操作された
        }
        @Override public void mouseDragged (MouseEvent me) {
          musDraggedOrMoved (me);  //マウスが動いた
        }
        @Override public void mouseMoved (MouseEvent me) {
          musDraggedOrMoved (me);  //マウスが動いた
        }
        @Override public void mouseWheelMoved (MouseWheelEvent mwe) {
          //マウスホイールイベントを消費する
          //  アプレットでウインドウに合わせてDSHELLやエディタを表示しているときうっかりホイールにさわってブラウザがスクロールすると鬱陶しい
          //  ブラウザをスクロールさせたいときはパネルの外側でホイールを動かせばよい
          mwe.consume ();
        }
      });

  }  //musStart()

  //musPressedOrReleased (me, pressed)
  //  マウスのボタンが操作された
  public static void musPressedOrReleased (MouseEvent me, boolean pressed) {
    //  InputEvent.getModifiers()は変化したものだけ返す
    //  InputEvent.getModifiersEx()は変化していないものも含めて現在の状態を返す
    int modifiers = me.getModifiers ();
    if ((modifiers & MouseEvent.BUTTON1_MASK) != 0) {  //左ボタンが押されたまたは離された
      musButtonLeft = pressed;
      musDataLeft = pressed && musOnScreen ? 1 : 0;  //マウスデータはスクリーン上で押されたときだけON
    } else if ((modifiers & MouseEvent.BUTTON3_MASK) != 0) {  //右ボタンが押されたまたは離された
      musButtonRight = pressed;
      musDataRight = pressed && musOnScreen ? 2 : 0;  //マウスデータはスクリーン上で押されたときだけON
    } else if ((modifiers & MouseEvent.BUTTON2_MASK) != 0 && pressed) {  //中ボタンが押された
      musSetSeamlessOn (!musSeamlessOn);  //シームレス/エクスクルーシブを切り替える
    }
    musDraggedOrMoved (me);
  }  //musPressedOrReleased(MouseEvent,boolean)

  //musDraggedOrMoved (me)
  //  マウスが動いた
  public static void musDraggedOrMoved (MouseEvent me) {
    int x = musPanelX = me.getX ();
    int y = musPanelY = me.getY ();
    if (pnlScreenX <= x && x < pnlScreenX + pnlZoomWidth &&
        pnlScreenY <= y && y < pnlScreenY + pnlZoomHeight) {  //スクリーン上にある
      musOnScreen = true;  //スクリーンに入った
      musScreenX = (x - pnlScreenX) * pnlZoomRatioInX >> 16;  //端数は切り捨てる
      musScreenY = (y - pnlScreenY) * pnlZoomRatioInY >> 16;
    } else {  //スクリーン上にない
      musOnScreen = false;  //スクリーンから出た
    }
    if (pnlKeyboardX <= x && x < pnlKeyboardX + Keyboard.kbdWidth &&
        pnlKeyboardY <= y && y < pnlKeyboardY + Keyboard.kbdHeight) {  //キーボード上にある
      musOnKeyboard = true;  //キーボードに入った
      Keyboard.kbdHover (x - pnlKeyboardX, y - pnlKeyboardY);
    } else {  //キーボード上にない
      if (musOnKeyboard) {  //キーボードから出た
        musOnKeyboard = false;
        if (Keyboard.kbdPointedIndex >= 0) {  //ポイントされているキーがあった
          Keyboard.kbdHover (0, 0);  //ポイントを解除する
        }
      }
    }
  }  //musDraggedOrMoved(MouseEvent)

  //musSetSeamlessOn (on)
  //  シームレス/エクスクルーシブを切り替える
  public static void musSetSeamlessOn (boolean on) {
    if (rbtRobot == null) {  //ロボットが使えないときは切り替えない(シームレスのみ)
      return;
    }
    if (musSeamlessOn != on) {
      musSeamlessOn = on;
      if (on) {  //エクスクルーシブ→シームレス
        musShow ();
        //ホストのマウスカーソルをX68000のマウスカーソルの位置に移動させる
        int x, y;
        if (XEiJ.mpuCoreType < 6) {  //MMUなし
          if (Z8530.SCC_FSX_MOUSE &&
              Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
              MainMemory.mmrRls (0x0938) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
            int xy = MainMemory.mmrRls (Z8530.sccFSXMouseWork + 0x0a);
            x = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
            y = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
          } else {  //SX-Windowが動作中ではない
            int xy = MainMemory.mmrRls (0x0ace);
            x = xy >> 16;  //IOCSのマウスカーソルのX座標
            y = (short) xy;  //IOCSのマウスカーソルのY座標
          }
        } else {  //MMUあり
          if (Z8530.SCC_FSX_MOUSE &&
              Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
              MC68060.mmuPeekLongData (0x0938, 1) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
            int xy = MC68060.mmuPeekLongData (Z8530.sccFSXMouseWork + 0x0a, 1);
            x = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
            y = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
          } else {  //SX-Windowが動作中ではない
            int xy = MC68060.mmuPeekLongData (0x0ace, 1);
            x = xy >> 16;  //IOCSのマウスカーソルのX座標
            y = (short) xy;  //IOCSのマウスカーソルのY座標
          }
        }
        XEiJ.rbtRobot.mouseMove (x * pnlZoomWidth / pnlScreenWidth + pnlScreenX + pnlGlobalX,
                                 y * pnlZoomHeight / pnlScreenHeight + pnlScreenY + pnlGlobalY);
      } else {  //シームレス→エクスクルーシブ
        musHide ();
        Point point = pnlPanel.getLocationOnScreen ();
        pnlGlobalX = point.x;
        pnlGlobalY = point.y;
        musExclusiveStart = true;  //エクスクルーシブに切り替えた直後
      }
    }
    if (mnbSeamlessMenuItem.isSelected () != on) {
      mnbSeamlessMenuItem.setSelected (on);
    }
  }  //musSetSeamlessOn(boolean)

  //musHide ()
  //  マウスカーソルを消す
  public static void musHide () {
    if (musCursorNumber != 0 && musCursorAvailable) {
      musCursorNumber = 0;
      pnlPanel.setCursor (musCursorArray[0]);
    }
  }  //musHide()

  //musShow ()
  //  マウスカーソルを表示する
  public static void musShow () {
    if (musCursorNumber != 1 && musCursorAvailable) {
      musCursorNumber = 1;
      pnlPanel.setCursor (musCursorArray[1]);
    }
  }  //musShow()

  //musSetEdgeAccelerationOn (on)
  //  縁部加速
  public static void musSetEdgeAccelerationOn (boolean on) {
    musEdgeAccelerationOn = on;
  }  //musSetEdgeAccelerationOn(boolean)

  //musSetHostsPixelUnitsOn (on)
  //  true=エクスクルーシブのときマウスはホストの画素単位で動く,false=X68000の画素単位で動く
  public static void musSetHostsPixelUnitsOn (boolean on) {
    musHostsPixelUnitsOn = on;
    Z8530.scc0UpdateRatio ();
  }  //musSetHostsPixelUnitsOn(boolean)



  //========================================================================================
  //$$MNB メニューバー
  //  メニューバーの高さは23px

  public static final int MNB_MODIFIERS = InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;  //アクセラレータのモディファイヤ

  //メニューバー
  public static JMenuBar mnbMenuBar;  //メニューバー

  //メニュー
  public static JMenu mnbFileMenu;  //ファイル
  public static JMenu mnbDisplayMenu;  //画面
  public static JMenu mnbSoundMenu;  //音声
  public static JMenu mnbInputMenu;  //入力
  public static JMenu mnbConfigMenu;  //設定
  public static JMenu mnbLanguageMenu;  //言語

  //メニューアイテム
  //  チェックボックスなどの変更内容はアクションイベントから取り出せるので個々のメニューアイテムに名前を付ける必要はない
  //  メニュー以外の方法で変更できるアイテムと一時的に変更できなくなるアイテムに名前をつけておく
  //  最初から最後まで選択できないメニューアイテムはメニューバーを作る時点で無効化するか、表示しない
  public static JMenuItem mnbQuitMenuItem;  //終了
  public static JRadioButtonMenuItem mnbFullscreenMenuItem;  //全画面表示
  public static JRadioButtonMenuItem mnbFitInWindowMenuItem;  //ウインドウに合わせる
  public static JRadioButtonMenuItem mnbFixedSizeMenuItem;  //固定サイズ
  public static JCheckBoxMenuItem mnbPlayMenuItem;  //音声出力
  public static JMenuItem mnbPasteMenuItem;  //貼り付け
  public static JCheckBoxMenuItem mnbSeamlessMenuItem;  //シームレスマウス
  public static JRadioButtonMenuItem mnbStandardKeyboardMenuItem;  //標準キーボード
  public static JRadioButtonMenuItem mnbCompactKeyboardMenuItem;  //コンパクトキーボード
  public static JRadioButtonMenuItem mnbNoKeyboardMenuItem;  //キーボードなし
  public static JLabel mnbVolumeLabel;  //音量

  //mnbMake ()
  //  メニューバーを作る
  //  メニューバーの幅は狭くしたいがメニューの幅が狭すぎると隣のメニューに流れやすくなって操作しにくいのでメニューの数を必要最小限にする
  public static void mnbMake () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {

          //ファイルメニュー
        case "Quit":  //終了。コマンドラインのみ
          prgTini ();
          break;

          //画面メニュー
        case "Full Screen":  //全画面表示
          pnlSetFullscreenOn (true);
          break;
        case "Fit in Window":  //ウインドウに合わせる
          if (pnlFullscreenOn) {  //全画面表示になっているとき
            pnlPrevFitInWindowOn = true;
            pnlSetFullscreenOn (false);
          } else {  //全画面表示になっていないとき
            pnlSetFitInWindowOn (true);
          }
          break;
        case "Fixed Size":  //固定サイズ
          if (pnlFullscreenOn) {  //全画面表示になっているとき
            pnlPrevFitInWindowOn = false;
            pnlSetFullscreenOn (false);
          } else {  //全画面表示になっていないとき
            pnlSetFitInWindowOn (false);
          }
          break;
        case "Nearest Neighbor":  //最近傍補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
          pnlPanel.repaint ();
          break;
        case "Bilinear":  //線形補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
          pnlPanel.repaint ();
          break;
        case "Bicubic":  //三次補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
          pnlPanel.repaint ();
          break;
        case "Paint All Changed Frames":  //変化したフレームをすべて描画
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 0;
          }
          break;
        case "Paint 1/2 of Changed Frames":  //変化したフレームを 1/2 描画
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 1;
          }
          break;
        case "Paint 1/3 of Changed Frames":  //変化したフレームを 1/3 描画
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 2;
          }
          break;
        case "Paint 1/4 of Changed Frames":  //変化したフレームを 1/4 描画
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 3;
          }
          break;
        case "Paint 1/5 of Changed Frames":  //変化したフレームを 1/5 描画
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 4;
          }
          break;
        case "Sprite Pattern Viewer":  //スプライトパターンビュア
          if (SpritePatternViewer.SPV_ON) {
            SpritePatternViewer.spvOpen ();
          }
          break;
        case "Screen Mode Test":  //表示モードテスト
          if (ScreenModeTest.SMT_ON) {
            ScreenModeTest.smtOpen ();
          }
          break;

          //音声メニュー
        case "Play":  //音声出力
          SoundSource.sndSetPlayOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "OPM Output":  //OPM出力
          YM2151.opmSetOutputOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "PCM Output":  //PCM出力
          ADPCM.pcmSetOutputOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Sound Thinning":  //音声 間引き
          SoundSource.sndRateConverter = SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO;
          break;
        case "Sound Linear Interpolation":  //音声 線形補間
          SoundSource.sndRateConverter = SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO;
          break;
        case "Sound Piecewise-constant Area Interpolation":  //音声 区分定数面積補間
          SoundSource.sndRateConverter = SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000;
          break;
        case "Sound Linear Area Interpolation":  //音声 線形面積補間
          SoundSource.sndRateConverter = SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000;
          break;
        case "Sound Monitor":  //音声モニタ
          SoundMonitor.smnOpen ();
          break;
        case "PCM Piecewise-constant Interpolation":  //PCM 区分定数補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_CONSTANT);
          break;
        case "PCM Linear Interpolation":  //PCM 線形補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_LINEAR);
          break;
        case "PCM Hermite Interpolation":  //PCM エルミート補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_HERMITE);
          break;
        case "PCM 8MHz/4MHz":
          ADPCM.pcmOSCFreqRequest = 0;
          break;
        case "PCM 8MHz/16MHz":
          ADPCM.pcmOSCFreqRequest = 1;
          break;

          //入力メニュー
        case "Paste":  //貼り付け
          CONDevice.conDoPaste ();
          break;
        case "No Keyboard":  //キーボードなし
          Keyboard.kbdSetOn (false);
          break;
        case "Standard Keyboard":  //標準キーボード
          Keyboard.kbdSetType (Keyboard.KBD_STANDARD_TYPE);
          Keyboard.kbdSetOn (true);
          break;
        case "Compact Keyboard":  //コンパクトキーボード
          Keyboard.kbdSetType (Keyboard.KBD_COMPACT_TYPE);
          Keyboard.kbdSetOn (true);
          break;
        case "F11 Key to Fullscreen":  //F11 キーで全画面表示
          Keyboard.kbdF11Mode = Keyboard.KBD_F11_FULLSCREEN;
          break;
        case "F11 Key to Screen Shot":  //F11 キーでスクリーンショット
          Keyboard.kbdF11Mode = Keyboard.KBD_F11_SCREENSHOT;
          break;
        case "F11 Key to Stop and Start":  //F11 キーで停止と再開
          Keyboard.kbdF11Mode = Keyboard.KBD_F11_STOPSTART;
          break;
        case "Default Delay":  //既定の開始
          smrRepeatDelay = -1;
          Keyboard.kbdSetRepeatDelay (MainMemory.mmrRbs (0x00ed003a));
          break;
        case (200 + 100 *  0) + "ms":
        case (200 + 100 *  1) + "ms":
        case (200 + 100 *  2) + "ms":
        case (200 + 100 *  3) + "ms":
        case (200 + 100 *  4) + "ms":
        case (200 + 100 *  5) + "ms":
        case (200 + 100 *  6) + "ms":
        case (200 + 100 *  7) + "ms":
        case (200 + 100 *  8) + "ms":
        case (200 + 100 *  9) + "ms":
        case (200 + 100 * 10) + "ms":
        case (200 + 100 * 11) + "ms":
        case (200 + 100 * 12) + "ms":
        case (200 + 100 * 13) + "ms":
        case (200 + 100 * 14) + "ms":
        case (200 + 100 * 15) + "ms":
          Keyboard.kbdRepeatDelay = Integer.parseInt (command.substring (0, command.length () - 2));
          //MainMemory.mmrWb (0x00ed003a, smrRepeatDelay = (Keyboard.kbdRepeatDelay - 200) / 100);
          //perl optdiv.pl 1500 100
          //  x/100==x*1311>>>17 (0<=x<=4698) [1500*1311==1966500]
          MainMemory.mmrWb (0x00ed003a, smrRepeatDelay = (Keyboard.kbdRepeatDelay - 200) * 1311 >>> 17);
          break;
        case "Default Interval":  //既定の間隔
          smrRepeatInterval =  -1;
          Keyboard.kbdSetRepeatInterval (MainMemory.mmrRbs (0x00ed003b));
          break;
        case (30 + 5 *  0 *  0) + "ms":
        case (30 + 5 *  1 *  1) + "ms":
        case (30 + 5 *  2 *  2) + "ms":
        case (30 + 5 *  3 *  3) + "ms":
        case (30 + 5 *  4 *  4) + "ms":
        case (30 + 5 *  5 *  5) + "ms":
        case (30 + 5 *  6 *  6) + "ms":
        case (30 + 5 *  7 *  7) + "ms":
        case (30 + 5 *  8 *  8) + "ms":
        case (30 + 5 *  9 *  9) + "ms":
        case (30 + 5 * 10 * 10) + "ms":
        case (30 + 5 * 11 * 11) + "ms":
        case (30 + 5 * 12 * 12) + "ms":
        case (30 + 5 * 13 * 13) + "ms":
        case (30 + 5 * 14 * 14) + "ms":
        case (30 + 5 * 15 * 15) + "ms":
          Keyboard.kbdRepeatInterval = Integer.parseInt (command.substring (0, command.length () - 2));
          //MainMemory.mmrWb (0x00ed003b, smrRepeatInterval = (int) Math.sqrt ((double) ((Keyboard.kbdRepeatInterval - 30) / 5)));
          //perl optdiv.pl 1125 5
          //  x/5==x*1639>>>13 (0<=x<=2733) [1125*1639==1843875]
          MainMemory.mmrWb (0x00ed003b, smrRepeatInterval = (int) Math.sqrt ((double) ((Keyboard.kbdRepeatInterval - 30) * 1639 >>> 13)));
          break;
        case "Seamless Mouse":  //シームレスマウス
          musSetSeamlessOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Edge Acceleration":  //縁部加速
          musSetEdgeAccelerationOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Use Host's Pixel Units":  //ホストの画素単位を使う
          musSetHostsPixelUnitsOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Joystick Settings":  //ジョイスティックの設定
          ppiOpen ();
          break;

          //設定メニュー
        case "Main Memory 1MB":
          MainMemory.mmrMemorySizeRequest = 0x00100000;
          break;
        case "Main Memory 2MB":
          MainMemory.mmrMemorySizeRequest = 0x00200000;
          break;
        case "Main Memory 4MB":
          MainMemory.mmrMemorySizeRequest = 0x00400000;
          break;
        case "Main Memory 6MB":
          MainMemory.mmrMemorySizeRequest = 0x00600000;
          break;
        case "Main Memory 8MB":
          MainMemory.mmrMemorySizeRequest = 0x00800000;
          break;
        case "Main Memory 10MB":
          MainMemory.mmrMemorySizeRequest = 0x00a00000;
          break;
        case "Main Memory 12MB":
          MainMemory.mmrMemorySizeRequest = 0x00c00000;
          break;
        case "Save contents of the main memory":  //メインメモリの内容を保存する
          if (prgIsLocal) {
            MainMemory.mmrMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        case "No High Memory":  //ハイメモリなし
          busHighMemorySize = 0 << 20;
          break;
        case "High Memory 16MB":  //ハイメモリ 16MB
          busHighMemorySize = 16 << 20;
          break;
        case "Save contents of the high memory":  //ハイメモリの内容を保存する
          if (prgIsLocal) {
            busHighMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        case "No Local Memory":  //ローカルメモリなし
          busLocalMemorySize = 0 << 20;
          break;
        case "Local Memory 16MB":  //ローカルメモリ 16MB
          busLocalMemorySize = 16 << 20;
          break;
        case "Local Memory 32MB":  //ローカルメモリ 32MB
          busLocalMemorySize = 32 << 20;
          break;
        case "Local Memory 64MB":  //ローカルメモリ 64MB
          busLocalMemorySize = 64 << 20;
          break;
        case "Local Memory 128MB":  //ローカルメモリ 128MB
          busLocalMemorySize = 128 << 20;
          break;
        case "Local Memory 256MB":  //ローカルメモリ 256MB
          busLocalMemorySize = 256 << 20;
          break;
        case "Save contents of the local memory":  //ローカルメモリの内容を保存する
          if (prgIsLocal) {
            busLocalMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        case "ROM Debugger start flag":  //ROM デバッガ起動フラグ
          romROMDBOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Registers":  //レジスタ
          RegisterList.drpOpen ();
          break;
        case "Disassemble List":  //逆アセンブルリスト
          DisassembleList.ddpOpen (-1, -1, true);
          break;
        case "Memory Dump List":  //メモリダンプリスト
          MemoryDumpList.dmpOpen (-1, -1, true);
          break;
        case "Logical Space Monitor":  //論理空間モニタ
          LogicalSpaceMonitor.atwOpen ();
          break;
        case "Physical Space Monitor":  //物理空間モニタ
          PhysicalSpaceMonitor.paaOpen ();
          break;
        case "Address Translation Caches Monitor":  //アドレス変換キャッシュモニタ
          if (ATCMonitor.ACM_ON) {
            ATCMonitor.acmOpen ();
          }
          break;
        case "Console":  //コンソール
          dgtOpen ();
          break;
        case "Branch Log":  //分岐ログ
          if (BranchLog.BLG_ON) {
            BranchLog.blgOpen (BranchLog.BLG_SELECT_NONE);
          }
          break;
        case "Program Flow Visualizer":  //プログラムフロービジュアライザ
          if (ProgramFlowVisualizer.PFV_ON) {
            ProgramFlowVisualizer.pfvOpen ();
          }
          break;
        case "Raster Break Point":  //ラスタブレークポイント
          if (RasterBreakPoint.RBP_ON) {
            RasterBreakPoint.rbpOpen ();
          }
          break;
        case "Data Break Point":  //データブレークポイント
          if (DataBreakPoint.DBP_ON) {
            DataBreakPoint.dbpOpen ();
          }
          break;
        case "Root Pointer List":  //ルートポインタリスト
          if (RootPointerList.RTL_ON) {
            RootPointerList.rtlOpen ();
          }
          break;
        case "Terminal":  //ターミナル
          RS232CTerminal.trmOpen ();
          break;
        case "Font Editor":  //フォントエディタ
          fntOpen ();
          break;
        case "Java Runtime Environment Information":
          prgOpenJavaDialog ();
          break;
        case "Version Information":
          prgOpenAboutDialog ();
          break;
        case "XEiJ License":
          prgOpenXEiJLicenseDialog ();
          break;
        case "FSHARP License":
          prgOpenSHARPLicenseDialog ();
          break;
        case "MAME License":
          prgOpenMAMELicenseDialog ();
          break;

          //言語メニュー
        case "English":
          Multilingual.mlnChange ("en");
          break;
        case "日本語":
          Multilingual.mlnChange ("ja");
          break;

        }
      }
    };

    ButtonGroup unitGroup = new ButtonGroup ();
    ButtonGroup frameGroup = new ButtonGroup ();
    ButtonGroup fullGroup = new ButtonGroup ();
    ButtonGroup hintGroup = new ButtonGroup ();
    ButtonGroup intermittentGroup = new ButtonGroup ();
    ButtonGroup soundInterpolationGroup = new ButtonGroup ();
    ButtonGroup adpcmInterpolationGroup = new ButtonGroup ();
    ButtonGroup adpcmOSCFreqGroup = new ButtonGroup ();
    ButtonGroup keyboardGroup = new ButtonGroup ();
    ButtonGroup keydlyGroup = new ButtonGroup ();
    ButtonGroup keyrepGroup = new ButtonGroup ();
    ButtonGroup memoryGroup = new ButtonGroup ();
    ButtonGroup highGroup = new ButtonGroup ();
    ButtonGroup localGroup = new ButtonGroup ();
    ButtonGroup languageGroup = new ButtonGroup ();
    ButtonGroup f11Group = new ButtonGroup ();

    //メニューバー
    mnbMenuBar = createMenuBar (

      //ファイルメニュー
      mnbFileMenu = Multilingual.mlnText (
        createMenu (
          "File", 'F',
          //FDDメニュー
          FDC.fdcMenu,
          //SASI HDDメニュー
          HDC.hdcMenu,
          //内蔵 SCSI HDDメニュー
          SPC.spcMenu,
          //HFSメニュー
          HFS.hfsMenu,
          createHorizontalSeparator (),
          mnbQuitMenuItem = setEnabled (
            Multilingual.mlnText (createMenuItem ("Quit", 'Q', MNB_MODIFIERS, listener), "ja", "終了"),
            prgIsLocal)  //ローカルのとき
          ),
        "ja", "ファイル"),

      //MPUメニュー
      mpuMenu,

      //画面メニュー
      mnbDisplayMenu = Multilingual.mlnText (
        createMenu (
          "Display", 'D',
          mnbFullscreenMenuItem = setEnabled (
            Multilingual.mlnText (
              createRadioButtonMenuItem (
                fullGroup, pnlFullscreenOn,
                "Full Screen", KeyEvent.VK_F11, listener), "ja", "全画面表示"),
            pnlIsFullscreenSupported),  //全画面表示に移行できるとき
          mnbFitInWindowMenuItem = setEnabled (
            Multilingual.mlnText (
              createRadioButtonMenuItem (
                fullGroup, !pnlFullscreenOn && pnlFitInWindowOn,
                "Fit in Window", 'W', MNB_MODIFIERS, listener), "ja", "ウインドウに合わせる"),
            pnlIsFitInWindowSupported),  //ウインドウに合わせられるとき
          mnbFixedSizeMenuItem = Multilingual.mlnText (
            createRadioButtonMenuItem (
              fullGroup, !pnlFullscreenOn && !pnlFitInWindowOn,
              "Fixed Size", 'X', MNB_MODIFIERS, listener), "ja", "固定サイズ"),
          pnlFixedBox,
          createHorizontalSeparator (),
          Multilingual.mlnText (
            createRadioButtonMenuItem (
              hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
              "Nearest Neighbor", listener),
            "ja", "最近傍補間"),
          Multilingual.mlnText (
            createRadioButtonMenuItem (
              hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR,
              "Bilinear", listener),
            "ja", "線形補間"),
          Multilingual.mlnText (
            createRadioButtonMenuItem (
              hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC,
              "Bicubic", listener),
            "ja", "三次補間"),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : createHorizontalSeparator (),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            createRadioButtonMenuItem (
              intermittentGroup, CRTC.crtIntermittentInterval == 0, "Paint All Changed Frames", listener),
            "ja", "変化したフレームをすべて描画"),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            createRadioButtonMenuItem (
              intermittentGroup, CRTC.crtIntermittentInterval == 1, "Paint 1/2 of Changed Frames", listener),
            "ja", "変化したフレームを 1/2 描画"),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            createRadioButtonMenuItem (
              intermittentGroup, CRTC.crtIntermittentInterval == 2, "Paint 1/3 of Changed Frames", listener),
            "ja", "変化したフレームを 1/3 描画"),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            createRadioButtonMenuItem (
              intermittentGroup, CRTC.crtIntermittentInterval == 3, "Paint 1/4 of Changed Frames", listener),
            "ja", "変化したフレームを 1/4 描画"),
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            createRadioButtonMenuItem (
              intermittentGroup, CRTC.crtIntermittentInterval == 4, "Paint 1/5 of Changed Frames", listener),
            "ja", "変化したフレームを 1/5 描画"),
          createHorizontalSeparator (),
          SpritePatternViewer.SPV_ON ? Multilingual.mlnText (createMenuItem ("Sprite Pattern Viewer", listener), "ja", "スプライトパターンビュア") : null,
          ScreenModeTest.SMT_ON ? Multilingual.mlnText (createMenuItem ("Screen Mode Test", listener), "ja", "表示モードテスト") : null
          ),
        "ja", "画面"),

      //音声メニュー
      mnbSoundMenu = setEnabled (
        Multilingual.mlnText (
          createMenu (
            "Sound", 'S',
            mnbPlayMenuItem = setEnabled (Multilingual.mlnText (createCheckBoxMenuItem (SoundSource.sndPlayOn, "Play", 'P', MNB_MODIFIERS, listener), "ja", "音声出力"), SoundSource.sndLine != null),
            //ボリュームのラベル
            //  JLabelのalignmentでセンタリングしようとするとチェックボックスのサイズの分だけ右に寄ってしまう
            //  Boxで囲み、左右にglueを置くことでセンタリングする
            createHorizontalBox (
              Box.createHorizontalGlue (),
              Multilingual.mlnText (createLabel ("Volume "), "ja", "音量"),
              mnbVolumeLabel = createLabel (String.valueOf (SoundSource.sndVolume)),
              Box.createHorizontalGlue ()
              ),
            //ボリュームスライダ
            //  デフォルトのサイズだと間延びした感じになるので幅を狭くする
            //  高さの43pxはpreferredSizeを指定しなかった場合と同じ
            setPreferredSize (
              createHorizontalSlider (0, SoundSource.SND_VOLUME_MAX, SoundSource.sndVolume, SoundSource.SND_VOLUME_STEP, 1, new ChangeListener () {
                @Override public void stateChanged (ChangeEvent ce) {
                  SoundSource.sndSetVolume (((JSlider) ce.getSource ()).getValue ());
                }
              }),
              214, 43),
            Multilingual.mlnText (
              createMenu (
                "Sound Interpolation",
                Multilingual.mlnText (
                  createRadioButtonMenuItem (
                    soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.THINNING_STEREO,
                    "Sound Thinning", listener),
                  "ja", "音声 間引き"),
                Multilingual.mlnText (
                  createRadioButtonMenuItem (
                    soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_STEREO,
                    "Sound Linear Interpolation", listener),
                  "ja", "音声 線形補間"),
                setEnabled (
                  Multilingual.mlnText (
                    createRadioButtonMenuItem (
                      soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000,
                      "Sound Piecewise-constant Area Interpolation", listener),
                    "ja", "音声 区分定数面積補間"),
                  SoundSource.SND_CHANNELS == 2 && SoundSource.SND_SAMPLE_FREQ == 48000),
                setEnabled (
                  Multilingual.mlnText (
                    createRadioButtonMenuItem (
                      soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000,
                      "Sound Linear Area Interpolation", listener),
                    "ja", "音声 線形面積補間"),
                  SoundSource.SND_CHANNELS == 2 && SoundSource.SND_SAMPLE_FREQ == 48000)
                ),
              "ja", "音声補間"),
            Multilingual.mlnText (createMenuItem ("Sound Monitor", listener), "ja", "音声モニタ"),
            createHorizontalSeparator (),
            setEnabled (Multilingual.mlnText (createCheckBoxMenuItem (YM2151.opmOutputMask != 0, "OPM Output", listener), "ja", "OPM 出力"), SoundSource.sndLine != null),
            createHorizontalSeparator (),
            setEnabled (Multilingual.mlnText (createCheckBoxMenuItem (ADPCM.pcmOutputOn, "PCM Output", listener), "ja", "PCM 出力"), SoundSource.sndLine != null),
            Multilingual.mlnText (
              createMenu (
                "PCM Interpolation",
                Multilingual.mlnText (
                  createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_CONSTANT,
                    "PCM Piecewise-constant Interpolation", listener),
                  "ja", "PCM 区分定数補間"),
                Multilingual.mlnText (
                  createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_LINEAR,
                    "PCM Linear Interpolation", listener),
                  "ja", "PCM 線形補間"),
                Multilingual.mlnText (
                  createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_HERMITE,
                    "PCM Hermite Interpolation", listener),
                  "ja", "PCM エルミート補間")
                ),
              "ja", "PCM 補間"),
            Multilingual.mlnText (
              createMenu (
                "PCM Source OSC Freq.",
                createRadioButtonMenuItem (adpcmOSCFreqGroup, ADPCM.pcmOSCFreqRequest == 0, "PCM 8MHz/4MHz", listener),
                createRadioButtonMenuItem (adpcmOSCFreqGroup, ADPCM.pcmOSCFreqRequest == 1, "PCM 8MHz/16MHz", listener)
                ),
              "ja", "PCM 原発振周波数")
            ),
          "ja", "音声"),
        SoundSource.sndLine != null),

      //入力メニュー
      mnbInputMenu = Multilingual.mlnText (
        createMenu (
          "Input", 'I',
          setEnabled (
            mnbPasteMenuItem = Multilingual.mlnText (createMenuItem ("Paste", 'V', MNB_MODIFIERS, listener), "ja", "貼り付け"),
            prgIsLocal),  //ローカルのとき
          createHorizontalSeparator (),
          mnbNoKeyboardMenuItem = Multilingual.mlnText (
            createRadioButtonMenuItem (keyboardGroup, !Keyboard.kbdOn, "No Keyboard", 'K', MNB_MODIFIERS, listener),
            "ja", "キーボードなし"),
          mnbStandardKeyboardMenuItem = Multilingual.mlnText (
            createRadioButtonMenuItem (keyboardGroup, Keyboard.kbdOn && Keyboard.kbdType == Keyboard.KBD_STANDARD_TYPE, "Standard Keyboard", listener),
            "ja", "標準キーボード"),
          mnbCompactKeyboardMenuItem = Multilingual.mlnText (
            createRadioButtonMenuItem (keyboardGroup, Keyboard.kbdOn && Keyboard.kbdType == Keyboard.KBD_COMPACT_TYPE, "Compact Keyboard", listener),
            "ja", "コンパクトキーボード"),
          Multilingual.mlnText (
            createMenu (
              "Repeat Delay",
              Multilingual.mlnText (createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == -1, "Default Delay", listener), "ja", "既定の開始"),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  0, (200 + 100 *  0) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  1, (200 + 100 *  1) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  2, (200 + 100 *  2) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  3, (200 + 100 *  3) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  4, (200 + 100 *  4) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  5, (200 + 100 *  5) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  6, (200 + 100 *  6) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  7, (200 + 100 *  7) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  8, (200 + 100 *  8) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay ==  9, (200 + 100 *  9) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 10, (200 + 100 * 10) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 11, (200 + 100 * 11) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 12, (200 + 100 * 12) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 13, (200 + 100 * 13) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 14, (200 + 100 * 14) + "ms", listener),
              createRadioButtonMenuItem (keydlyGroup, smrRepeatDelay == 15, (200 + 100 * 15) + "ms", listener)
              ),
            "ja", "リピート開始"),
          Multilingual.mlnText (
            createMenu (
              "Repeat Interval",
              Multilingual.mlnText (createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == -1, "Default Interval", listener), "ja", "既定の間隔"),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  0, (30 + 5 *  0 *  0) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  1, (30 + 5 *  1 *  1) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  2, (30 + 5 *  2 *  2) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  3, (30 + 5 *  3 *  3) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  4, (30 + 5 *  4 *  4) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  5, (30 + 5 *  5 *  5) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  6, (30 + 5 *  6 *  6) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  7, (30 + 5 *  7 *  7) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  8, (30 + 5 *  8 *  8) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval ==  9, (30 + 5 *  9 *  9) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 10, (30 + 5 * 10 * 10) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 11, (30 + 5 * 11 * 11) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 12, (30 + 5 * 12 * 12) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 13, (30 + 5 * 13 * 13) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 14, (30 + 5 * 14 * 14) + "ms", listener),
              createRadioButtonMenuItem (keyrepGroup, smrRepeatInterval == 15, (30 + 5 * 15 * 15) + "ms", listener)
              ),
            "ja", "リピート間隔"),
          createHorizontalSeparator (),
          Multilingual.mlnText (
            createMenu (
              "F11 Key",
              Multilingual.mlnText (
                createRadioButtonMenuItem (f11Group, Keyboard.kbdF11Mode == Keyboard.KBD_F11_FULLSCREEN, "F11 Key to Fullscreen", listener),
                "ja", "F11 キーで全画面表示"),
              Multilingual.mlnText (
                createRadioButtonMenuItem (f11Group, Keyboard.kbdF11Mode == Keyboard.KBD_F11_SCREENSHOT, "F11 Key to Screen Shot", listener),
                "ja", "F11 キーでスクリーンショット"),
              Multilingual.mlnText (
                createRadioButtonMenuItem (f11Group, Keyboard.kbdF11Mode == Keyboard.KBD_F11_STOPSTART, "F11 Key to Stop and Start", listener),
                "ja", "F11 キーで停止と再開")),
            "ja", "F11 キー"),
          createHorizontalSeparator (),
          mnbSeamlessMenuItem = setEnabled (
            Multilingual.mlnText (
              createCheckBoxMenuItem (musSeamlessOn, "Seamless Mouse", KeyEvent.VK_F12, listener),
              "ja", "シームレスマウス"),
            rbtRobot != null),
          Multilingual.mlnText (createCheckBoxMenuItem (musEdgeAccelerationOn, "Edge Acceleration", listener), "ja", "縁部加速"),
          createHorizontalBox (
            Box.createHorizontalGlue (),
            Multilingual.mlnText (createLabel ("Speed of Mouse Cursor "), "ja", "マウスカーソルの速度 "),
            Z8530.scc0Label,
            Box.createHorizontalGlue ()
            ),
          Z8530.scc0Slider,
          setEnabled (
            Multilingual.mlnText (
              createCheckBoxMenuItem (musHostsPixelUnitsOn, "Use Host's Pixel Units", listener),
              "ja", "ホストの画素単位を使う"),
            rbtRobot != null),
          Z8530.SCC_DEBUG_TRACE ? Z8530.sccTraceMenuItem : null,
          createHorizontalSeparator (),
          Multilingual.mlnText (createMenuItem ("Joystick Settings", listener), "ja", "ジョイスティックの設定")
          ),
        "ja", "入力"),

      //設定メニュー
      mnbConfigMenu = Multilingual.mlnText (
        createMenu (
          "Config", 'C',
          Multilingual.mlnText (createMenuItem ("Terminal", listener), "ja", "ターミナル"),
          Multilingual.mlnText (
            createMenu (
              "Debug",
              Multilingual.mlnText (createMenuItem ("Console", listener), "ja", "コンソール"),
              Multilingual.mlnText (createMenuItem ("Registers", listener), "ja", "レジスタ"),
              Multilingual.mlnText (createMenuItem ("Disassemble List", listener), "ja", "逆アセンブルリスト"),
              Multilingual.mlnText (createMenuItem ("Memory Dump List", listener), "ja", "メモリダンプリスト"),
              Multilingual.mlnText (createMenuItem ("Logical Space Monitor", listener), "ja", "論理空間モニタ"),
              Multilingual.mlnText (createMenuItem ("Physical Space Monitor", listener), "ja", "物理空間モニタ"),
              ATCMonitor.ACM_ON ? Multilingual.mlnText (createMenuItem ("Address Translation Caches Monitor", listener), "ja", "アドレス変換キャッシュモニタ") : null,
              BranchLog.BLG_ON ? Multilingual.mlnText (createMenuItem ("Branch Log", listener), "ja", "分岐ログ") : null,
              ProgramFlowVisualizer.PFV_ON ? Multilingual.mlnText (createMenuItem ("Program Flow Visualizer", listener), "ja", "プログラムフロービジュアライザ") : null,
              RasterBreakPoint.RBP_ON ? Multilingual.mlnText (createMenuItem ("Raster Break Point", listener), "ja", "ラスタブレークポイント") : null,
              DataBreakPoint.DBP_ON ? Multilingual.mlnText (createMenuItem ("Data Break Point", listener), "ja", "データブレークポイント") : null,
              RootPointerList.RTL_ON ? Multilingual.mlnText (createMenuItem ("Root Pointer List", listener), "ja", "ルートポインタリスト") : null
              ),
            "ja", "デバッグ"),
          Multilingual.mlnText (createCheckBoxMenuItem (romROMDBOn, "ROM Debugger start flag", listener), "ja", "ROM デバッガ起動フラグ"),
          smrBootMenu,
          Multilingual.mlnText (
            createMenu (
              "Main Memory",
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00100000, "Main Memory 1MB", listener),
                       "ja", "メインメモリ 1MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00200000, "Main Memory 2MB", listener),
                       "ja", "メインメモリ 2MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00400000, "Main Memory 4MB", listener),
                       "ja", "メインメモリ 4MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00600000, "Main Memory 6MB", listener),
                       "ja", "メインメモリ 6MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00800000, "Main Memory 8MB", listener),
                       "ja", "メインメモリ 8MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00a00000, "Main Memory 10MB", listener),
                       "ja", "メインメモリ 10MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (memoryGroup, MainMemory.mmrMemorySizeRequest == 0x00c00000, "Main Memory 12MB", listener),
                       "ja", "メインメモリ 12MB"),
              MainMemory.MMR_SAVE ? createHorizontalSeparator () : null,
              MainMemory.MMR_SAVE ? setEnabled (
                Multilingual.mlnText (createCheckBoxMenuItem (MainMemory.mmrMemorySaveOn, "Save contents of the main memory", listener),
                         "ja", "メインメモリの内容を保存する"),
                prgIsLocal) : null
              ),
            "ja", "メインメモリ"),
          Multilingual.mlnText (
            createMenu (
              "High Memory on X68030",
              Multilingual.mlnText (createRadioButtonMenuItem (highGroup, busHighMemorySize == 0 << 20, "No High Memory", listener),
                       "ja", "ハイメモリなし"),
              Multilingual.mlnText (createRadioButtonMenuItem (highGroup, busHighMemorySize == 16 << 20, "High Memory 16MB", listener),
                       "ja", "ハイメモリ 16MB"),
              createHorizontalSeparator (),
              setEnabled (
                Multilingual.mlnText (createCheckBoxMenuItem (busHighMemorySaveOn, "Save contents of the high memory", listener),
                         "ja", "ハイメモリの内容を保存する"),
                prgIsLocal
                )
              ),
            "ja", "X68030 のハイメモリ"),
          Multilingual.mlnText (
            createMenu (
              "Local Memory on 060turbo",
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 0 << 20, "No Local Memory", listener),
                       "ja", "ローカルメモリなし"),
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 16 << 20, "Local Memory 16MB", listener),
                       "ja", "ローカルメモリ 16MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 32 << 20, "Local Memory 32MB", listener),
                       "ja", "ローカルメモリ 32MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 64 << 20, "Local Memory 64MB", listener),
                       "ja", "ローカルメモリ 64MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 128 << 20, "Local Memory 128MB", listener),
                       "ja", "ローカルメモリ 128MB"),
              Multilingual.mlnText (createRadioButtonMenuItem (localGroup, busLocalMemorySize == 256 << 20, "Local Memory 256MB", listener),
                       "ja", "ローカルメモリ 256MB"),
              createHorizontalSeparator (),
              setEnabled (
                Multilingual.mlnText (createCheckBoxMenuItem (busLocalMemorySaveOn, "Save contents of the local memory", listener),
                         "ja", "ローカルメモリの内容を保存する"),
                prgIsLocal
                )
              ),
            "ja", "060turbo のローカルメモリ"),
          smrMenu,
          Settings.sgsMenu,
          createHorizontalSeparator (),
          Multilingual.mlnText (
            createMenu (
              "Misc.",
              Multilingual.mlnText (createMenuItem ("Font Editor", listener), "ja", "フォントエディタ"),
              Profiling.pffCheckBoxMenuItem,
              SlowdownTest.sdtCheckBoxMenuItem,
              SlowdownTest.sdtBox
              ),
            "ja", "その他"),
          createHorizontalSeparator (),
          Multilingual.mlnText (createMenuItem ("Java Runtime Environment Information", listener), "ja", "Java 実行環境の情報"),
          Multilingual.mlnText (createMenuItem ("Version Information", listener), "ja", "バージョン情報"),
          Multilingual.mlnText (
            createMenu (
              "License",
              Multilingual.mlnText (createMenuItem ("XEiJ License", listener), "ja", "XEiJ 使用許諾条件"),
              Multilingual.mlnText (createMenuItem ("FSHARP License", listener), "ja", "FSHARP 許諾条件"),
              Multilingual.mlnText (createMenuItem ("MAME License", listener), "ja", "MAME License")
              ),
            "ja", "使用許諾条件")
          ),
        "ja", "設定"),

      //インジケータ
      Box.createHorizontalGlue (),  //インジケータをセンタリングする
      createVerticalBox (
        Box.createVerticalGlue (),
        Indicator.indBox,
        Box.createVerticalGlue ()
        ),
      Box.createHorizontalGlue (),  //言語メニューを右に寄せる

      //言語メニュー
      //  言語選択メニューはテキストで書く
      //    国旗アイコンは対象とするマーケットを選択させるときに使うものであり、表示言語を切り替えるだけのメニューに国旗を用いるのは不適切
      //  言語メニューのメニューアイテムは多言語化しない
      //  言語メニュー自体を多言語化しないという方法もある
      //    簡潔な日本語メニューの並びに幅を食うLanguageメニューが混在しているのはあまり美しくない
      mnbLanguageMenu = Multilingual.mlnText (
        createMenu (
          "Language", 'L',
          createRadioButtonMenuItem (languageGroup, Multilingual.mlnEnglish, "English", listener),
          createRadioButtonMenuItem (languageGroup, Multilingual.mlnJapanese, "日本語", listener)
          ),
        "ja", "言語")

      );
  }  //mnbMake()



  //========================================================================================
  //$$FRM フレーム
  //  コマンドラインのみ

  //モード
  public static boolean frmIsActive;  //true=フォーカスがある

  //フレーム
  public static JFrame frmFrame;  //フレーム
  public static int frmMarginWidth;  //パネルからフレームまでのマージン
  public static int frmMarginHeight;
  public static Dimension frmMinimumSize;  //pnlMinimumWidth+frmMarginWidth,pnlMinimumHeight+frmMarginHeight フレームの最小サイズ

  //スクリーンデバイス
  public static GraphicsDevice frmScreenDevice;  //スクリーンデバイス

  //ドラッグアンドドロップ
  public static DropTarget frmDropTarget;

  //frmInit ()
  //  フレームを初期化する
  public static void frmInit () {
    frmIsActive = false;
    frmScreenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ();  //スクリーンデバイス
    pnlIsFullscreenSupported = frmScreenDevice.isFullScreenSupported ();  //全画面表示に移行できるかどうか
    pnlIsFitInWindowSupported = true;  //ウインドウに合わせられるかどうか
  }  //frmInit()

  //frmMake ()
  //  フレームを作る
  public static void frmMake () {

    //フレーム
    frmFrame = createRestorableFrame (Settings.SGS_FRM_FRAME_KEY, PRG_TITLE + " version " + PRG_VERSION, mnbMenuBar, pnlPanel);
    frmFrame.setIconImage (LnF.LNF_ICON_IMAGE_16);  //タスクバーのアイコンを変更する
    frmFrame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
    //frmFrame.setResizable (false);  //リサイズ不可

    //パネルからフレームまでのマージンを確認する
    frmMarginWidth = frmFrame.getWidth () - pnlWidth;
    frmMarginHeight = frmFrame.getHeight () - pnlHeight;
    frmMinimumSize = new Dimension (pnlMinimumWidth + frmMarginWidth, pnlMinimumHeight + frmMarginHeight);
    frmFrame.setMinimumSize (frmMinimumSize);

    //ドラッグアンドドロップ
    //  FDイメージが放り込まれたらそこから再起動する
    //  コマンドラインのみ
    frmDropTarget = new DropTarget (pnlPanel, DnDConstants.ACTION_COPY, new DropTargetAdapter () {
      @Override public void dragOver (DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
          dtde.acceptDrag (DnDConstants.ACTION_COPY);
          return;
        }
        dtde.rejectDrag ();
      }
      @Override public void drop (DropTargetDropEvent dtde) {
        try {
          if (dtde.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrop (DnDConstants.ACTION_COPY);
            boolean reset = true;
            int fdu0 = -1;
            int fdu = 0;
            int hdu0 = -1;
            int hdu = 0;
            int scu0 = -1;
            int scu = 0;
            int hfu0 = -1;
            int hfu = 0;
            for (Object o : (java.util.List) dtde.getTransferable ().getTransferData (DataFlavor.javaFileListFlavor)) {
              if (o instanceof File) {
                File file = (File) o;
                if (file.isFile ()) {  //ファイルのとき。HFS以外のフィルタがディレクトリを受け入れてしまうのでディレクトリを除外する
                  if (FDC.fdcOpenFileFilter.accept (file)) {  //FDのイメージファイルのとき
                    if (fdu < FDC.FDC_MAX_UNITS && FDC.fdcUnitArray[fdu].insert (file.getPath ())) {  //挿入できた
                      if (fdu0 < 0) {
                        fdu0 = fdu;
                      }
                      fdu++;
                      continue;
                    }
                  }
                  if (HDC.hdcOpenFileFilter.accept (file)) {  //SASIハードディスクのイメージファイルのとき
                    if (hdu < 16 && HDC.hdcUnitArray[hdu].insert (file.getPath ())) {  //挿入できた
                      if (hdu0 < 0) {
                        hdu0 = hdu;
                      }
                      hdu++;
                      continue;
                    }
                  }
                  if (SPC.spcOpenFileFilter.accept (file)) {  //SCSIハードディスク/CD-ROMのイメージファイルのとき
                    if (scu < 16 && SPC.spcUnitArray[scu].insert (file.getPath ())) {  //挿入できた
                      if (scu0 < 0) {
                        scu0 = scu;
                      }
                      scu++;
                      continue;
                    }
                  }
                }
                if (HFS.hfsOpenFileFilter.accept (file)) {  //ディレクトリまたはHUMAN.SYSのとき
                  if (hfu < 16 && HFS.hfsUnitArray[hfu].insert (file.getPath ())) {  //挿入できた
                    if (hfu0 < 0) {
                      hfu0 = hfu;
                    }
                    hfu++;
                    continue;
                  }
                }
              }
              reset = false;  //挿入できないファイルがあったときはリセットをキャンセルする
            }
            dtde.dropComplete (true);
            if (reset) {
              if (fdu0 >= 0) {
                mpuReset (0x9070 | fdu0 << 8, -1);
              } else if (hdu0 >= 0) {
                mpuReset (0x8000 | hdu0 << 8, -1);
              } else if (scu0 >= 0) {
                mpuReset (0xa000, SPC.SPC_HANDLE_EX + (scu0 << 2));  //拡張SCSIがなければ内蔵SCSIに読み替えられる
              } else if (hfu0 >= 0) {
                HFS.hfsBootUnit = hfu0;
                mpuReset (0xa000, HFS.HFS_BOOT_HANDLE);
              }
            }
            return;
          }
        } catch (UnsupportedFlavorException ufe) {
          //ufe.printStackTrace ();
        } catch (IOException ioe) {
          //ioe.printStackTrace ();
        }
        dtde.rejectDrop();
      }
    });

  }  //frmMake()

  //frmStart ()
  //  フレームのイベントリスナーを設定して動作を開始する
  public static void frmStart () {

    //ウインドウリスナー
    //  ウインドウを開いたとき  activated,opened
    //  フォーカスを失ったとき  deactivated
    //  フォーカスを取得したとき  activated
    //  ウインドウをアイコン化したとき  iconified,[deactivated]
    //  ウインドウを元のサイズに戻したとき  deiconified,activated
    //  ウインドウを閉じたとき  closing,[deactivated],closed
    addRemovableListener (
      frmFrame,
      new WindowAdapter () {
        @Override public void windowActivated (WindowEvent we) {
          frmIsActive = true;
        }
        @Override public void windowClosing (WindowEvent we) {
          prgTini ();
        }
        @Override public void windowDeactivated (WindowEvent we) {
          frmIsActive = false;
          musSetSeamlessOn (true);  //フォーカスを失ったときはシームレスに切り替える
        }
      });

    //コンポーネントリスナー
    //  エクスクルーシブマウスモードのときに使うパネルの座標を得る
    //  全画面表示のON/OFFを行ったときcomponentMovedが呼ばれないことがあるのでcomponentResizedでもパネルの座標を得る
    addRemovableListener (
      frmFrame,
      new ComponentAdapter () {
        @Override public void componentMoved (ComponentEvent ce) {
          Point p = pnlPanel.getLocationOnScreen ();
          pnlGlobalX = p.x;
          pnlGlobalY = p.y;
        }
        @Override public void componentResized (ComponentEvent ce) {
          Point p = pnlPanel.getLocationOnScreen ();
          pnlGlobalX = p.x;
          pnlGlobalY = p.y;
        }
      });

  }  //frmStart()

  //frmSetFullscreenOn (on)
  //  全画面表示を設定する
  //  メニューアイテムは更新しないので必要ならば呼び出し側で更新すること
  public static void frmSetFullscreenOn (boolean on) {
    pnlFullscreenOn = on;
    //全画面表示を変更する
    if (on) {
      if (frmScreenDevice.getFullScreenWindow () != frmFrame) {  //自分が全画面表示でないとき
        frmFrame.getRootPane().setWindowDecorationStyle (JRootPane.NONE);  //飾り枠を消す
        frmScreenDevice.setFullScreenWindow (frmFrame);  //全画面表示に移行する
      }
    } else {
      if (frmScreenDevice.getFullScreenWindow () == frmFrame) {  //自分が全画面表示のとき
        frmScreenDevice.setFullScreenWindow (null);  //全画面表示を解除する
        frmFrame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
      }
    }
    //全画面表示を変更するとコンポーネントイベントが発生する
    //コンポーネントリスナーで画面とキーボードの配置を再計算する
  }  //frmSetFullscreenOn(boolean)



  //========================================================================================
  //$$CLP クリップボード

  public static BufferedImage clpClipboardImage;  //コピーされるイメージ
  public static String clpClipboardString;  //コピーされる文字列
  public static Clipboard clpClipboard;  //クリップボード
  public static Transferable clpImageContents;  //イメージをコピーするときに渡すデータ
  public static Transferable clpStringContents;  //文字列をコピーするときに渡すデータ
  public static ClipboardOwner clpClipboardOwner;  //クリップボードオーナー。コピーするときにデータに付ける情報
  public static boolean clpIsClipboardOwner;  //true=クリップボードに入っているデータは自分がコピーした

  //clpMake ()
  //  クリップボードを作る
  public static void clpMake () {
    Toolkit toolkit = Toolkit.getDefaultToolkit ();
    clpClipboard = null;
    try {
      clpClipboard = toolkit.getSystemClipboard ();  //クリップボード
    } catch (Exception e) {
      //アプレットのときjava.secury.AccessControlExceptionが出る
      return;
    }
    clpClipboardImage = null;  //コピーされるイメージ
    clpClipboardString = null;  //コピーされる文字列
    clpImageContents = new Transferable () {
      public Object getTransferData (DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor == DataFlavor.imageFlavor) {
          return clpClipboardImage;
        } else {
          throw new UnsupportedFlavorException (flavor);
        }
      }
      public DataFlavor[] getTransferDataFlavors () {
        return new DataFlavor[] { DataFlavor.imageFlavor };
      }
      public boolean isDataFlavorSupported (DataFlavor flavor) {
        return flavor == DataFlavor.imageFlavor;
      }
    };
    clpStringContents = new Transferable () {
      public Object getTransferData (DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor == DataFlavor.stringFlavor) {
          return clpClipboardString;
        } else {
          throw new UnsupportedFlavorException (flavor);
        }
      }
      public DataFlavor[] getTransferDataFlavors () {
        return new DataFlavor[] { DataFlavor.stringFlavor };
      }
      public boolean isDataFlavorSupported (DataFlavor flavor) {
        return flavor == DataFlavor.stringFlavor;
      }
    };
    clpIsClipboardOwner = false;  //自分はまだコピーしていない
    //クリップボードオーナー
    //  lostOwnership   クリップボードの所有者でなくなったとき
    clpClipboardOwner = new ClipboardOwner () {
      @Override public void lostOwnership (Clipboard clipboard, Transferable contents) {
        clpIsClipboardOwner = false;
      }
    };
    //フレーバーリスナー
    //  flavorsChanged  クリップボードのDataFlavorが変化したとき
    clpClipboard.addFlavorListener (new FlavorListener () {
      @Override public void flavorsChanged (FlavorEvent fe) {
        mnbPasteMenuItem.setEnabled (clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor));  //文字列ならば貼り付けできる
      }
    });
    if (!clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor)) {  //文字列がコピーされていない
      mnbPasteMenuItem.setEnabled (false);  //貼り付け選択不可
    }
  }  //clpMake



  //========================================================================================
  //$$MDL 機種
  //  MPUとIPLROMのバージョン

  //                                             MPU      clock     IPLROM    内蔵HD
  public static final int MDL_EXPERT   = 0;  //MC68000    10MHz    IPLROM1.0   SASI
  public static final int MDL_SUPER    = 1;  //MC68000    10MHz    IPLROM1.0   SCSI
  public static final int MDL_XVI      = 2;  //MC68000    16.7MHz  IPLROM1.1   SCSI
  public static final int MDL_COMPACT  = 3;  //MC68000    25MHz    IPLROM1.2   SCSI
  public static final int MDL_HYBRID   = 4;  //MC68000    33.3MHz  IPLROM1.3   SCSI
  public static final int MDL_X68030   = 5;  //MC68EC030  25MHz    IPLROM1.3   SCSI
  public static final int MDL_060TURBO = 6;  //MC68060    50MHz    IPLROM1.6   SCSI
  //public static final int MDL_DASH     = ;  //MC68030    33.3MHz  IPLROM1.3   SCSI
  //public static final int MDL_040TURBO = ;  //MC68040    25MHz    IPLROM1.3   SCSI

  public static JMenu mdlMenu;  //メニュー

  public static JRadioButtonMenuItem mdlEXPERTMenuItem;
  public static JRadioButtonMenuItem mdlSUPERMenuItem;
  public static JRadioButtonMenuItem mdlXVIMenuItem;
  public static JRadioButtonMenuItem mdlCompactMenuItem;
  public static JRadioButtonMenuItem mdlHybridMenuItem;
  public static JRadioButtonMenuItem mdlX68030MenuItem;
  public static JRadioButtonMenuItem mdl060turboMenuItem;

  public static JRadioButtonMenuItem fpuMenuItem0;
  public static JRadioButtonMenuItem fpuMenuItem1;
  public static JRadioButtonMenuItem fpuMenuItem2;
  public static JCheckBoxMenuItem fpuMenuItem3;

  public static JCheckBoxMenuItem mdlSASIMenuItem;
  public static JCheckBoxMenuItem mdlSCSIINMenuItem;

  public static int mdlModel;  //機種の指定
  public static int mdlCoreRequest;  //機種の指定によるMPUの指定
  public static int mdlIPLROMRequest;  //機種の指定によるIPLROMの指定。0/1/2/3のみ。5は不可
  public static double mdlClockRequest;  //機種の指定によるクロックの指定。-1.0=反映済み
  public static boolean mdlSCSIINRequest;  //機種の指定によるSCSIINの指定
  public static int fpuMode;  //0=FPUなし,1=拡張精度,2=三倍精度
  public static boolean fpuOn;  //true=FPUあり(mpuCoreType>=2&&fpuMode!=0)

  //mdlInit ()
  //  機種の指定を読み取る
  public static void mdlInit () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "X68000 EXPERT (10MHz)":
          mdlRequestModel (MDL_EXPERT, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "X68000 SUPER (10MHz)":
          mdlRequestModel (MDL_SUPER, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "X68000 XVI (16.7MHz)":
          mdlRequestModel (MDL_XVI, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "X68000 Compact (25MHz)":
          mdlRequestModel (MDL_COMPACT, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "X68000 Hybrid (33.3MHz)":
          mdlRequestModel (MDL_HYBRID, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "X68030 (25MHz)":
          mdlRequestModel (MDL_X68030, true);
          //リセット
          mpuReset (-1, -1);
          break;
        case "060turbo (50MHz)":
          mdlRequestModel (MDL_060TURBO, true);
          //リセット
          mpuReset (-1, -1);
          break;

        case "No FPU":  //FPUなし
          fpuMode = 0;
          fpuOn = false;
          break;
        case "Extended Precision (19-digit)":  //拡張精度 (19 桁)
          fpuMode = 1;
          fpuOn = mpuCoreType >= 2;
          break;
        case "Triple Precision (24-digit)":  //三倍精度 (24 桁)
          fpuMode = 2;
          fpuOn = mpuCoreType >= 2;
          break;
        case "Full Specification FPU":  //フルスペック FPU
          fpuFullSpec = ((JCheckBoxMenuItem) source).isSelected ();
          break;

        case "Internal SCSI Port":  //内蔵 SCSI ポート
          mdlSCSIINRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;

        }
      }
    };

    //メニュー
    ButtonGroup modelGroup = new ButtonGroup ();
    mdlMenu = Multilingual.mlnText (
      createMenu (
        "Change Model and Reset",
        mdlEXPERTMenuItem   = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_EXPERT  , "X68000 EXPERT (10MHz)",   listener),
        mdlSUPERMenuItem    = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_SUPER   , "X68000 SUPER (10MHz)",    listener),
        mdlXVIMenuItem      = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_XVI     , "X68000 XVI (16.7MHz)",    listener),
        mdlCompactMenuItem  = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_COMPACT , "X68000 Compact (25MHz)",  listener),
        mdlHybridMenuItem   = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_HYBRID  , "X68000 Hybrid (33.3MHz)", listener),
        createHorizontalSeparator (),
        mdlX68030MenuItem   = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_X68030  , "X68030 (25MHz)",          listener),
        createHorizontalSeparator (),
        mdl060turboMenuItem = createRadioButtonMenuItem (modelGroup, mdlModel == MDL_060TURBO, "060turbo (50MHz)",        listener)
        ),
      "ja", "機種を変更してリセット");

    ButtonGroup fpuGroup = new ButtonGroup ();
    fpuMenuItem0 = setEnabled (
      Multilingual.mlnText (createRadioButtonMenuItem (fpuGroup, fpuMode == 0, "No FPU" , listener),
               "ja", "FPU なし"),
      mdlCoreRequest >= 2);
    fpuMenuItem1 = setEnabled (
      Multilingual.mlnText (createRadioButtonMenuItem (fpuGroup, fpuMode == 1, "Extended Precision (19-digit)" , listener),
               "ja", "拡張精度 (19 桁)"),
      mdlCoreRequest >= 2);
    fpuMenuItem2 = setEnabled (
      Multilingual.mlnText (createRadioButtonMenuItem (fpuGroup, fpuMode == 2, "Triple Precision (24-digit)" , listener),
               "ja", "三倍精度 (24 桁)"),
      mdlCoreRequest >= 2);
    fpuMenuItem3 = setEnabled (
      Multilingual.mlnText (createCheckBoxMenuItem (fpuFullSpec, "Full Specification FPU", listener), "ja", "フルスペック FPU"),
      mdlCoreRequest >= 4);

    mdlSASIMenuItem = setEnabled (
      Multilingual.mlnText (createCheckBoxMenuItem (!mdlSCSIINRequest, "Internal SASI Port", listener), "ja", "内蔵 SASI ポート"),
      false);  //機種の指定で内蔵SASIと内蔵SCSIを切り替えるので操作できないことにする

    mdlSCSIINMenuItem = setEnabled (
      Multilingual.mlnText (createCheckBoxMenuItem (mdlSCSIINRequest, "Internal SCSI Port", listener), "ja", "内蔵 SCSI ポート"),
      false);  //機種の指定で内蔵SASIと内蔵SCSIを切り替えるので操作できないことにする

    //機種
    //mdlRequestModel (mdlModel, true);
    mdlRequestModel (mdlModel, false);  //SGSでmdlClockRequestを設定済み

    //MPU
    if (mdlCoreRequest == 0) {
      if (mdlModel == MDL_X68030 ||
          mdlModel == MDL_060TURBO) {
        mdlModel = MDL_HYBRID;
      }
    } else if (mdlCoreRequest == 3) {
      if (mdlModel != MDL_X68030) {
        mdlModel = MDL_X68030;
        mdlIPLROMRequest = 3;
        mdlRequestSCSIIN (true);
      }
    } else if (mdlCoreRequest == 6) {
      if (mdlModel != MDL_060TURBO) {
        mdlModel = MDL_060TURBO;
        mdlIPLROMRequest = 3;  //5は不可
        mdlRequestSCSIIN (true);
      }
    }

  }  //mdlInit()

  //mdlRequestModel (model, all)
  //  機種の指定をリクエストに変換する
  //  all  動的に変更できる項目も初期化する
  public static void mdlRequestModel (int model, boolean all) {
    mdlModel = model;
    switch (model) {
    case MDL_EXPERT:
      mdlEXPERTMenuItem.setSelected (true);
      mdlRequestCore (0);
      mdlIPLROMRequest = 0;
      mdlRequestSCSIIN (false);
      if (all) {
        mdlClockRequest = 10.0;
      }
      break;
    case MDL_SUPER:
      mdlSUPERMenuItem.setSelected (true);
      mdlRequestCore (0);
      mdlIPLROMRequest = 0;
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 10.0;
      }
      break;
    case MDL_XVI:
      mdlXVIMenuItem.setSelected (true);
      mdlRequestCore (0);
      mdlIPLROMRequest = 1;
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 50.0 / 3.0;  //16.7MHz
      }
      break;
    case MDL_COMPACT:
      mdlCompactMenuItem.setSelected (true);
      mdlRequestCore (0);
      mdlIPLROMRequest = 2;
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 25.0;
      }
      break;
    case MDL_HYBRID:
      mdlHybridMenuItem.setSelected (true);
      mdlRequestCore (0);
      mdlIPLROMRequest = 3;
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 100.0 / 3.0;  //33.3MHz
      }
      break;
    case MDL_X68030:
      mdlX68030MenuItem.setSelected (true);
      mdlRequestCore (3);
      mdlIPLROMRequest = 3;
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 25.0;
      }
      break;
    case MDL_060TURBO:
      mdl060turboMenuItem.setSelected (true);
      mdlRequestCore (6);
      mdlIPLROMRequest = 3;  //5は不可
      mdlRequestSCSIIN (true);
      if (all) {
        mdlClockRequest = 50.0;
      }
      break;
    }
  }  //mdlRequestModel(model)

  public static void mdlRequestCore (int core) {
    mdlCoreRequest = core;
    fpuMenuItem0.setEnabled (core >= 2);
    fpuMenuItem1.setEnabled (core >= 2);
    fpuMenuItem2.setEnabled (core >= 2);
    fpuMenuItem3.setEnabled (core >= 4);
  }

  public static void mdlRequestSCSIIN (boolean request) {
    mdlSCSIINRequest = request;
    mdlSASIMenuItem.setSelected (!request);
    mdlSCSIINMenuItem.setSelected (request);
  }  //mdlRequestSCSIIN()



  //========================================================================================
  //$$MPU MPU

  //コンパイルスイッチ
  public static final boolean MPU_INLINE_EXCEPTION = true;  //true=例外処理をインライン展開する。速くなる
  public static final boolean MPU_COMPOUND_POSTINCREMENT = false;  //true=(pc)+をbusRbs((pc+=2)-1),busRws((pc+=2)-2),busRls((pc+=4)-4)のように書く。見た目はスマートだが最適化しにくくなる？

  public static final boolean MPU_SWITCH_MISC_OPCODE = false;  //true=RTSなどのswitchのキーはオペコード全体,false=下位6bit
  public static final boolean MPU_SWITCH_BCC_CONDITION = false;  //true=オペコードのswitchでBccをccで分類する
  public static final boolean MPU_SWITCH_BCC_OFFSET = false;  //true=オペコードのswitchでBRA/BSR/Bccを8bitオフセットの上位2bitで分類する
  public static final boolean MPU_SWITCH_SCC_CONDITION = true;  //true=オペコードのswitchでScc/DBRA/DBcc/TRAPccをccで分類する

  //TMR_FREQ単位のタイマカウンタで到達し得ない時刻を表す定数
  //  TMR_FREQ=10^12のとき到達するのに3.5ヶ月かかる
  //  3.5ヶ月間動かしっぱなしにすると破綻する
  public static final long FAR_FUTURE = 0x7fffffffffffffffL;

  //ステータスレジスタ
  //  トレース
  //     srT1    srT0
  //    0x0000  0x0000  トレースなし
  //    0x0000  0x4000  フローの変化をトレース
  //    0x8000  0x0000  すべての命令をトレース
  //    0x8000  0x4000  未定義
  public static final int REG_SR_T1  = 0b10000000_00000000;
  public static final int REG_SR_T0  = 0b01000000_00000000;  //(020/030/040)
  //  モード
  //      srS     srM
  //    0x0000  0x0000  ユーザモード(USPを使用)
  //    0x0000  0x1000  ユーザモード(USPを使用)
  //    0x2000  0x0000  スーパーバイザ割り込みモード(ISPを使用)
  //    0x2000  0x1000  スーパーバイザマスタモード(MSPを使用)
  public static final int REG_SR_S   = 0b00100000_00000000;
  public static final int REG_SR_M   = 0b00010000_00000000;  //(020/030/040/060)
  //  割り込み
  public static final int REG_SR_I   = 0b00000111_00000000;

  //コンディションコードレジスタ
  public static final int REG_CCR_X  = 0b00000000_00010000;
  public static final int REG_CCR_N  = 0b00000000_00001000;
  public static final int REG_CCR_Z  = 0b00000000_00000100;
  public static final int REG_CCR_V  = 0b00000000_00000010;
  public static final int REG_CCR_C  = 0b00000000_00000001;
  public static final int REG_CCR_MASK = REG_CCR_X | REG_CCR_N | REG_CCR_Z | REG_CCR_V | REG_CCR_C;  //CCRの有効なビット

  public static char[] REG_CCRXMAP = "00000000000000001111111111111111".toCharArray ();
  public static char[] REG_CCRNMAP = "00000000111111110000000011111111".toCharArray ();
  public static char[] REG_CCRZMAP = "00001111000011110000111100001111".toCharArray ();
  public static char[] REG_CCRVMAP = "00110011001100110011001100110011".toCharArray ();
  public static char[] REG_CCRCMAP = "01010101010101010101010101010101".toCharArray ();

  //割り込みレベル
  //  順序の変更やレベルの追加はコードの変更が必要
  public static final int MPU_IOI_INTERRUPT_LEVEL = 1;
  public static final int MPU_EB2_INTERRUPT_LEVEL = 2;
  public static final int MPU_DMA_INTERRUPT_LEVEL = 3;
  public static final int MPU_SCC_INTERRUPT_LEVEL = 5;
  public static final int MPU_MFP_INTERRUPT_LEVEL = 6;
  public static final int MPU_SYS_INTERRUPT_LEVEL = 7;
  public static final int MPU_IOI_INTERRUPT_MASK = 0x80 >> MPU_IOI_INTERRUPT_LEVEL;  //0x40
  public static final int MPU_EB2_INTERRUPT_MASK = 0x80 >> MPU_EB2_INTERRUPT_LEVEL;  //0x20
  public static final int MPU_DMA_INTERRUPT_MASK = 0x80 >> MPU_DMA_INTERRUPT_LEVEL;  //0x10
  public static final int MPU_SCC_INTERRUPT_MASK = 0x80 >> MPU_SCC_INTERRUPT_LEVEL;  //0x04
  public static final int MPU_MFP_INTERRUPT_MASK = 0x80 >> MPU_MFP_INTERRUPT_LEVEL;  //0x02
  public static final int MPU_SYS_INTERRUPT_MASK = 0x80 >> MPU_SYS_INTERRUPT_LEVEL;  //0x01

  public static final boolean MPU_INTERRUPT_SWITCH = true;  //true=最上位の割り込みをswitchで判別する

  //コンディションコード
  public static final boolean T = true;
  public static final boolean F = false;
  //  cccc==CCCC_cc                               cccc  cc
  public static final int CCCC_T  = 0b0000;  //0000  T       1                always true
  public static final int CCCC_F  = 0b0001;  //0001  F       0                always false
  public static final int CCCC_HI = 0b0010;  //0010  HI      ~C&~Z            high
  public static final int CCCC_LS = 0b0011;  //0011  LS      C|Z              low or same
  public static final int CCCC_CC = 0b0100;  //0100  CC(HS)  ~C               carry clear (high or same)
  public static final int CCCC_CS = 0b0101;  //0101  CS(LO)  C                carry set (low)
  public static final int CCCC_NE = 0b0110;  //0110  NE      ~Z               not equal
  public static final int CCCC_EQ = 0b0111;  //0111  EQ      Z                equal
  public static final int CCCC_VC = 0b1000;  //1000  VC      ~V               overflow clear
  public static final int CCCC_VS = 0b1001;  //1001  VS      V                overflow set
  public static final int CCCC_PL = 0b1010;  //1010  PL      ~N               plus
  public static final int CCCC_MI = 0b1011;  //1011  MI      N                minus
  public static final int CCCC_GE = 0b1100;  //1100  GE      N&V|~N&~V        greater or equal
  public static final int CCCC_LT = 0b1101;  //1101  LT      N&~V|~N&V        less than
  public static final int CCCC_GT = 0b1110;  //1110  GT      N&V&~Z|~N&~V&~Z  greater than
  public static final int CCCC_LE = 0b1111;  //1111  LE      Z|N&~V|~N&V      less or equal
  //F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //X
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,  //V
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //C
  //  BCCMAP[CCCC_cc<<5|ccr]==trueならば条件成立
  public static final boolean[] BCCMAP = {
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //T       NF
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //F       NT
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //HI      NLS
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //LS      NHI
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //CC(HS)  NCS(NLO)
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //CS(LO)  NCC(NHS)
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //NE(NZ)  NEQ(NZE)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //EQ(ZE)  NNE(NNZ)
    T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,  //VC      NVS
    F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,  //VS      NVC
    T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,  //PL      NMI
    F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //MI      NPL
    T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,  //GE      NLT
    F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,  //LT      NGE
    T,T,F,F,F,F,F,F,F,F,T,T,F,F,F,F,T,T,F,F,F,F,F,F,F,F,T,T,F,F,F,F,  //GT      NLE
    F,F,T,T,T,T,T,T,T,T,F,F,T,T,T,T,F,F,T,T,T,T,T,T,T,T,F,F,T,T,T,T,  //LE      NGT
  };

  //  MPU_CCCMAP[cccc<<5|ccr]==(BCCMAP[cccc<<5|ccr]?'1':'0')
  public static final char[] MPU_CCCMAP = (
    "11111111111111111111111111111111" +
    "00000000000000000000000000000000" +
    "10100000101000001010000010100000" +
    "01011111010111110101111101011111" +
    "10101010101010101010101010101010" +
    "01010101010101010101010101010101" +
    "11110000111100001111000011110000" +
    "00001111000011110000111100001111" +
    "11001100110011001100110011001100" +
    "00110011001100110011001100110011" +
    "11111111000000001111111100000000" +
    "00000000111111110000000011111111" +
    "11001100001100111100110000110011" +
    "00110011110011000011001111001100" +
    "11000000001100001100000000110000" +
    "00111111110011110011111111001111").toCharArray ();

  //  (MPU_CC_cc<<ccr<0)==trueならば条件成立
  //  (MPU_CC_cc<<ccr<0)==BCCMAP[CCCC_cc<<5|ccr]
  public static final int MPU_CC_T  = 0b11111111111111111111111111111111;  //T
  public static final int MPU_CC_F  = 0b00000000000000000000000000000000;  //F
  public static final int MPU_CC_HI = 0b10100000101000001010000010100000;  //HI
  public static final int MPU_CC_LS = 0b01011111010111110101111101011111;  //LS
  public static final int MPU_CC_HS = 0b10101010101010101010101010101010;  //HS
  public static final int MPU_CC_LO = 0b01010101010101010101010101010101;  //LO
  public static final int MPU_CC_NE = 0b11110000111100001111000011110000;  //NE
  public static final int MPU_CC_EQ = 0b00001111000011110000111100001111;  //EQ
  public static final int MPU_CC_VC = 0b11001100110011001100110011001100;  //VC
  public static final int MPU_CC_VS = 0b00110011001100110011001100110011;  //VS
  public static final int MPU_CC_PL = 0b11111111000000001111111100000000;  //PL
  public static final int MPU_CC_MI = 0b00000000111111110000000011111111;  //MI
  public static final int MPU_CC_GE = 0b11001100001100111100110000110011;  //GE
  public static final int MPU_CC_LT = 0b00110011110011000011001111001100;  //LT
  public static final int MPU_CC_GT = 0b11000000001100001100000000110000;  //GT
  public static final int MPU_CC_LE = 0b00111111110011110011111111001111;  //LE

  //TST.Bのテーブル
  //  z=255&(～);ccr=ccr&CCR_X|MPU_TSTB_TABLE[z]をz=～;ccr=ccr&CCR_X|MPU_TSTB_TABLE[255&z]にすると速くなることがある
  //  インデックスが明示的にマスクされていると最適化しやすいのだろう
/*
  public static final byte[] MPU_TSTB_TABLE = new byte[256];
  static {
    for (int z = 0; z < 256; z++) {
      MPU_TSTB_TABLE[z] = (byte) (z >> 7 << 3 | z - 1 >> 6 & CCR_Z);
    }
  }  //static
*/
/*
  public static final byte[] MPU_TSTB_TABLE = {
    REG_CCR_Z, 0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
  };
*/
  //  perl misc/itob.pl xeij/XEiJ.java MPU_TSTB_TABLE
  public static final byte[] MPU_TSTB_TABLE = "\4\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b".getBytes (XEiJ.ISO_8859_1);

  //BITREVのテーブル
  //  初期化コードが大きくなりすぎるので最初から展開しておくにはクラスを分ける必要がある
  public static final int[] MPU_BITREV_TABLE_0 = new int[2048];
  public static final int[] MPU_BITREV_TABLE_1 = new int[2048];
  public static final int[] MPU_BITREV_TABLE_2 = new int[2048];
  static {
    for (int i = 0; i < 2048; i++) {
      MPU_BITREV_TABLE_2[i] = (MPU_BITREV_TABLE_1[i] = (MPU_BITREV_TABLE_0[i] = Integer.reverse (i)) >>> 11) >>> 11;
    }
  }

  //アドレッシングモード
  //                                                              data  memory  control  alterable
  public static final int EA_DR = 0b000_000;  //D  Dr             x                      x
  public static final int EA_AR = 0b001_000;  //A  Ar                                    x
  public static final int EA_MM = 0b010_000;  //M  (Ar)           x     x       x        x
  public static final int EA_MP = 0b011_000;  //+  (Ar)+          x     x                x
  public static final int EA_MN = 0b100_000;  //-  -(Ar)          x     x                x
  public static final int EA_MW = 0b101_000;  //W  (d16,Ar)       x     x       x        x
  public static final int EA_MX = 0b110_000;  //X  (d8,Ar,Rn.wl)  x     x       x        x
  public static final int EA_ZW = 0b111_000;  //Z  (xxx).W        x     x       x        x
  public static final int EA_ZL = 0b111_001;  //Z  (xxx).L        x     x       x        x
  public static final int EA_PW = 0b111_010;  //P  (d16,PC)       x     x       x
  public static final int EA_PX = 0b111_011;  //P  (d8,PC,Rn.wl)  x     x       x
  public static final int EA_IM = 0b111_100;  //I  #<data>        x
  public static final int MMM_DR = EA_DR >> 3;
  public static final int MMM_AR = EA_AR >> 3;
  public static final int MMM_MM = EA_MM >> 3;
  public static final int MMM_MP = EA_MP >> 3;
  public static final int MMM_MN = EA_MN >> 3;
  public static final int MMM_MW = EA_MW >> 3;
  public static final int MMM_MX = EA_MX >> 3;
  public static final long EAM_DR = 0xff00000000000000L >>> EA_DR;
  public static final long EAM_AR = 0xff00000000000000L >>> EA_AR;
  public static final long EAM_MM = 0xff00000000000000L >>> EA_MM;
  public static final long EAM_MP = 0xff00000000000000L >>> EA_MP;
  public static final long EAM_MN = 0xff00000000000000L >>> EA_MN;
  public static final long EAM_MW = 0xff00000000000000L >>> EA_MW;
  public static final long EAM_MX = 0xff00000000000000L >>> EA_MX;
  public static final long EAM_ZW = 0x8000000000000000L >>> EA_ZW;
  public static final long EAM_ZL = 0x8000000000000000L >>> EA_ZL;
  public static final long EAM_PW = 0x8000000000000000L >>> EA_PW;
  public static final long EAM_PX = 0x8000000000000000L >>> EA_PX;
  public static final long EAM_IM = 0x8000000000000000L >>> EA_IM;
  public static final long EAM_ALL = EAM_DR|EAM_AR|EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|DAM+-WXZPI|すべて
  public static final long EAM_ALT = EAM_DR|EAM_AR|EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|DAM+-WXZ  |可変
  public static final long EAM_DAT = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|D M+-WXZPI|データ
  public static final long EAM_DME = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|D M+-WXZP |データレジスタ直接またはメモリ
  public static final long EAM_DLT = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|D M+-WXZ  |データ可変
  public static final long EAM_DCN = EAM_DR       |EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|D M  WXZP |データレジスタ直接または制御
  public static final long EAM_DCL = EAM_DR       |EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|D M  WXZ  |データレジスタ直接または制御可変
  public static final long EAM_ANY =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|  M+-WXZPI|レジスタ以外
  public static final long EAM_MEM =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M+-WXZP |メモリ
  public static final long EAM_MLT =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M+-WXZ  |メモリ可変
  public static final long EAM_RDL =               EAM_MM|EAM_MP       |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M+ WXZP |リードリスト
  public static final long EAM_WTL =               EAM_MM       |EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M -WXZ  |ライトリスト
  public static final long EAM_CNT =               EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M  WXZP |制御
  public static final long EAM_CLT =               EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M  WXZ  |制御可変

  //汎用レジスタ
  //  r[15]は現在のスタックポインタ
  //                              ユーザモード      スーパーバイザモード
  //                                            マスタモード  割り込みモード
  //     ユーザスタックポインタ     regRn[15]       mpuUSP         mpuUSP
  //     マスタスタックポインタ      mpuMSP       regRn[15]        mpuMSP
  //    割り込みスタックポインタ     mpuISP        mpuISP        regRn[15]
  //  モードによってmpuUSP,mpuMSP,mpuISPのいずれかをregRn[15]にコピーして使う
  //  他のモードに切り替えるときはregRn[15]をコピー元のmpuUSP,mpuMSP,mpuISPのいずれかに書き戻してから切り替える
  //  例えばユーザモードから割り込みモードへ移行するときはregRn[15]→mpuUSP,mpuISP→regRn[15]とする
  public static final int[] regRn = new int[16 + 1];  //汎用レジスタ。regRn[16]はテンポラリ。レジスタの更新を回避するとき条件分岐する代わりにregRn[16]に書き込む

  //プログラムカウンタ
  public static int regPC;  //プログラムカウンタ
  public static int regPC0;  //実行中の命令の先頭アドレス

  //オペコードレジスタ
  public static int regOC;  //オペコード。16bit、0拡張

  //ステータスレジスタ
  public static int regSRT1;  //ステータスレジスタのT1。0=トレースなし,REG_SR_T1=すべての命令をトレース
  public static int regSRT0;  //ステータスレジスタのT0(020/030/040)。0=トレースなし,REG_SR_T0=フローの変化をトレース
  public static int mpuTraceFlag;  //トレースフラグ。命令実行前にregSRT1をコピーし、分岐命令を実行するときregSRT0をorする。例外が発生したとき0にする。命令実行後に0でなければトレース例外を発動する
  public static int regSRS;  //ステータスレジスタのS。0=ユーザモード,REG_SR_S=スーパーバイザモード
  public static int regSRM;  //ステータスレジスタのM(020/030/040/060)。0=割り込みモード,REG_SR_M=マスタモード。割り込みを開始するときクリアされる
  public static int regSRI;  //ステータスレジスタのI2,I1,I0

  //コンディションコードレジスタ
  public static int regCCR;  //コンディションコードレジスタ。000XNZVC。0のビットは0に固定

  //mpuIMR
  //  インタラプトマスクレジスタ
  //  ビットの並び順は01234567
  //  現在の割り込みマスクレベルよりも高いビットをセットしたマスク
  //    割り込みマスクレベルがnのときレベル1..nの割り込みが禁止されてレベルn+1..7の割り込みが許可される
  //    ただし、割り込みマスクレベルが7のときはレベル7割り込みの処理中でなければレベル7割り込みが許可される
  //  mpuIMR&mpuIRRで最下位の1のビットの割り込みを受け付ける
  //  irpSetSRでmpuIMR&mpuISRで1のビットの割り込みを終了する
  //  mpuIMR=0x7f>>(srI>>8)|~mpuISR&1
  //      srI     mpuIMR    受け付ける割り込みのレベルと順序
  //    0x0700  0b00000000  なし  レベル7割り込みの処理中のとき
  //            0b00000001  7     レベル7割り込みの処理中ではないとき
  //    0x0600  0b00000001  7
  //    0x0500  0b00000011  7 6
  //    0x0400  0b00000111  7 6 5
  //    0x0300  0b00001111  7 6 5 4
  //    0x0200  0b00011111  7 6 5 4 3
  //    0x0100  0b00111111  7 6 5 4 3 2
  //    0x0000  0b01111111  7 6 5 4 3 2 1
  public static int mpuIMR;

  //mpuIRR
  //  インタラプトリクエストレジスタ
  //  ビットの並び順は01234567
  //  デバイスが割り込みを要求するときレベルに対応するビットをセットする
  //  コアが割り込みを受け付けるときレベルに対応するビットをクリアしてデバイスのacknowledge()を呼び出す
  public static int mpuIRR;
  public static int mpuDIRR;  //遅延割り込み要求

  //mpuISR
  //  インタラプトインサービスレジスタ
  //  ビットの並び順は01234567
  //  コアが割り込み処理を開始するときレベルに対応するビットをセットする
  //  割り込みマスクレベルが下がったとき新しいレベルよりも高いレベルの割り込み処理が終了したものとみなし、
  //  レベルに対応するビットをクリアしてデバイスのdone()を呼び出す
  //  done()が呼び出された時点でまだ処理されていない割り込みが残っているデバイスは再度割り込みを要求する
  public static int mpuISR;

  //制御レジスタ
  public static int mpuSFC;    //000  -12346  SFC
  public static int mpuDFC;    //001  -12346  DFC
  public static int mpuCACR;   //002  --2346  CACR
  //protected static int mpuTC;     //003  ----46  TC                        030MMUのTC
  //protected static int mpuITT0;   //004  ----46  ITT0   IACR0 @ MC68EC040  030MMUのTT0
  //protected static int mpuITT1;   //005  ----46  ITT1   IACR1 @ MC68EC040  030MMUのTT1
  //protected static int mpuDTT0;   //006  ----46  DTT0   DACR0 @ MC68EC040
  //protected static int mpuDTT1;   //007  ----46  DTT1   DACR1 @ MC68EC040
  public static int mpuBUSCR;  //008  -----6  BUSCR
  public static int mpuUSP;    //800  -12346  USP    隠れたユーザスタックポインタ。ユーザモードのときはr[15]を参照すること
  public static int mpuVBR;    //801  -12346  VBR    ベクタベースレジスタ
  public static int mpuCAAR;   //802  --23--  CAAR
  public static int mpuMSP;    //803  --234-  MSP    隠れたマスタスタックポインタ。マスタモードのときはr[15]を参照すること
  public static int mpuISP;    //804  --234-  ISP    隠れた割り込みスタックポインタ。割り込みモードのときはr[15]を参照すること
  //protected static int mpuMMUSR;  //805  ----4-  MMUSR                     030MMUのMMUSR
  //protected static int mpuURP;    //806  ----46  URP                       030MMUのCRPの下位32bit
  //protected static int mpuSRP;    //807  ----46  SRP                       030MMUのSRPの下位32bit
  public static int mpuPCR;    //808  -----6  PCR
  //protected static int mpuHCRP;   //                                       030MMUのCRPの上位32bit
  //protected static int mpuHSRP;   //                                       030MMUのSRPの上位32bit

  public static final int MPU_060_REV = 7;  //MC68060のリビジョンナンバー。1=F43G,5=G65V,6=E41J

  //クロック
  //  時刻は開始からの経過時間
  public static long mpuClockTime;  //時刻(TMR_FREQ単位)
  public static long mpuClockLimit;  //タスクの終了時刻
  public static double mpuClockMHz;  //動作周波数の設定値(MHz)
  public static double mpuCurrentMHz;  //動作周波数の現在値(MHz)
  public static int mpuCycleCount;  //命令のサイクル数。実行アドレス計算のサイクル数を含む
  public static int mpuCycleUnit;  //周波数の表示に使用する1サイクルあたりの時間(TMR_FREQ単位)
  public static int mpuModifiedUnit;  //mpuCycleCountの1サイクルあたりの時間(TMR_FREQ単位)。MC68030のときmpuCycleUnit*3/5

  //タイマ
  //  mpuTaskはコアをスケジュールしなおすときに毎回作り直す
  public static TimerTask mpuTask;  //null=停止中。null以外=動作中

  //その他
  public static int mpuBootDevice;  //起動デバイス。-1=指定なし
  public static int mpuBootAddress;  //起動アドレス。-1=指定なし

  //コア
  public static int mpuCoreType;  //0=MC68000,3=MC68EC030,6=MC68060
  public static int mpuRomWait;  //ROMのアクセスウエイト。X68000のとき1、X68030のとき0。これがないとクロックの表示がずれる
  public static boolean mpuIgnoreAddressError;  //true=アドレスエラーを無視する

  //任意の周波数の指定
  public static boolean mpuArbFreqOn;  //true=任意の周波数の指定がある。mpuArbFreqOn&&mpuUtilOnは不可
  public static int mpuArbFreqMHz;  //任意の周波数の指定(MHz)。1～1000
  public static SpinnerNumberModel mpuArbFreqModel;
  public static JSpinner mpuArbFreqSpinner;
  public static JRadioButtonMenuItem mpuArbFreqRadioButtonMenuItem;

  //任意の負荷率の指定
  public static boolean mpuUtilOn;  //true=任意の負荷率の指定がある。mpuArbFreqOn&&mpuUtilOnは不可
  public static int mpuUtilRatio;  //任意の負荷率の指定(%)。1～100
  public static SpinnerNumberModel mpuUtilModel;
  public static JSpinner mpuUtilSpinner;
  public static JRadioButtonMenuItem mpuUtilRadioButtonMenuItem;

  //メニュー
  public static JMenu mpuMenu;
  public static JMenuItem mpuResetMenuItem;
  public static JMenuItem mpuOpt1ResetMenuItem;
  public static JRadioButtonMenuItem mpuClock10MenuItem;
  public static JRadioButtonMenuItem mpuClock16MenuItem;
  public static JRadioButtonMenuItem mpuClock25MenuItem;
  public static JRadioButtonMenuItem mpuClock33MenuItem;
  public static JRadioButtonMenuItem mpuClock50MenuItem;

  //デバッグ
  public static ActionListener mpuDebugActionListener;  //デバッグアクションリスナー
  public static ArrayList<AbstractButton> mpuButtonsRunning;  //MPUが動作中のときだけ有効なボタン
  public static ArrayList<AbstractButton> mpuButtonsStopped;  //MPUが停止中のときだけ有効なボタン

  //SX-Window ver3.1のバグ対策
  //
  //  無償公開されたSXWIN311.XDFから起動したときリソースファイルから読み込まれるコードに問題がある
  //  マウスカーソルが指しているメニューの項目の番号を返すサブルーチンでd1レジスタの上位ワードが不定のままdivu.w #$0010,d1を実行している
  //  このサブルーチンの引数ではないd1レジスタの上位ワードを0にしてから呼び出さないとメニューを選択できない
  //  実機で露見しないのは直前に呼び出されるサブルーチンが使っているmovem.w <ea>,<list>命令がd1レジスタの上位ワードをほぼ0にしているため
  //  XEiJで露見したのはこの命令の処理が間違っていてd1レジスタが符号拡張されず上位ワードが0になっていなかったため
  //
  //  問題のサブルーチン
  //    00BFCC74  48E77000              movem.l d1-d3,-(sp)                 H輛.
  //    00BFCC78  2600                  move.l  d0,d3                       &.
  //    00BFCC7A  2F00                  move.l  d0,-(sp)                    /.
  //    00BFCC7C  486DFF80              pea.l   $FF80(a5)                   Hm..
  //    00BFCC80  A156                  SXCALL  __GMPtInRect                ｡V
  //    00BFCC82  508F                  addq.l  #$08,sp                     P夙
  //    00BFCC84  6738                  beq.s   $00BFCCBE                   g8
  //    00BFCC86  2003                  move.l  d3,d0                        .
  //    00BFCC88  3200                  move.w  d0,d1                       2.
  //    00BFCC8A  4840                  swap.w  d0                          H@
  //    00BFCC8C  906DFF80              sub.w   $FF80(a5),d0                仁..
  //    00BFCC90  6B2C                  bmi.s   $00BFCCBE                   k,
  //    00BFCC92  B06DFF92              cmp.w   $FF92(a5),d0                ｰm.地
  //    00BFCC96  6E26                  bgt.s   $00BFCCBE                   n&
  //    00BFCC98  926DFF82              sub.w   $FF82(a5),d1                知.Ｌ
  //    00BFCC9C  6B20                  bmi.s   $00BFCCBE                   k 
  //    00BFCC9E  342DFF9C              move.w  $FF9C(a5),d2                4-.愼
  //    00BFCCA2  C4FC0010              mulu.w  #$0010,d2                   ﾄ...
  //    00BFCCA6  B242                  cmp.w   d2,d1                       ｲB
  //    00BFCCA8  6E14                  bgt.s   $00BFCCBE                   n.
  //    00BFCCAA  82FC0010              divu.w  #$0010,d1                   ※..    ←d1レジスタの上位ワードが不定のままdivu.wを実行している
  //    00BFCCAE  3001                  move.w  d1,d0                       0.
  //    00BFCCB0  4841                  swap.w  d1                          HA
  //    00BFCCB2  4A41                  tst.w   d1                          JA
  //    00BFCCB4  6702                  beq.s   $00BFCCB8                   g.
  //    00BFCCB6  5240                  addq.w  #$01,d0                     R@
  //    00BFCCB8  4CDF000E              movem.l (sp)+,d1-d3                 Lﾟ..
  //    00BFCCBC  4E75                  rts                                 Nu
  //    00BFCCBE  7000                  moveq.l #$00,d0                     p.
  //    00BFCCC0  60F6                  bra.s   $00BFCCB8                   `.
  //
  //  問題のサブルーチンの呼び出し元
  //    00BFC9D6  4E56FFF0              link.w  a6,#$FFF0                   NV..
  //    00BFC9DA  6100FEF4              bsr.w   $00BFC8D0                   a...    ←直前に呼び出されるサブルーチン
  //    00BFC9DE  202C000A              move.l  $000A(a4),d0                 ,..
  //    00BFC9E2  61000290              bsr.w   $00BFCC74                   a...    ←問題のサブルーチン
  //    00BFC9E6  3200                  move.w  d0,d1                       2.
  //    00BFC9E8  3600                  move.w  d0,d3                       6.
  //    (以下略)
  //
  //  直前に呼び出されるサブルーチン
  //    00BFC8D0  426DFFA2              clr.w   $FFA2(a5)                   Bm.｢
  //    00BFC8D4  206C0006              movea.l $0006(a4),a0                 l..
  //    00BFC8D8  4C90000F              movem.w (a0),d0-d3                  L...
  //    00BFC8DC  5240                  addq.w  #$01,d0                     R@
  //    00BFC8DE  5241                  addq.w  #$01,d1                     RA
  //    00BFC8E0  5342                  subq.w  #$01,d2                     SB
  //    00BFC8E2  5343                  subq.w  #$01,d3                     SC
  //    00BFC8E4  48AD000FFF80          movem.w d0-d3,$FF80(a5)             0/0
  //    00BFC8EA  206C0002              movea.l $0002(a4),a0                 l..
  //    00BFC8EE  2050                  movea.l (a0),a0                      P
  //    00BFC8F0  30280012              move.w  $0012(a0),d0                0(..
  //    00BFC8F4  5240                  addq.w  #$01,d0                     R@
  //    00BFC8F6  3B40FF90              move.w  d0,$FF90(a5)                ;@.食
  //    00BFC8FA  486DFFA4              pea.l   $FFA4(a5)                   Hm.､
  //    00BFC8FE  A432                  SXCALL  __SXGetDispRect             ､2
  //    00BFC900  588F                  addq.l  #$04,sp                     X臭
  //    00BFC902  4CAD000FFFA4          movem.w $FFA4(a5),d0-d3             Lｭ...､  ←ここでd1レジスタの上位ワードがほぼ0になる
  //    00BFC908  9641                  sub.w   d1,d3                       泡
  //    00BFC90A  48C3                  ext.l   d3                          Hﾃ      ←このサブルーチンはext.lで符号拡張してからdivu.wを使っているが、符号拡張しておいて符号なし除算というのもおかしい。movem.wでd3も符号拡張されているのでこのext.lは不要で、0<=d1.w<=d3.wでないときもext.lは役に立たない
  //    00BFC90C  86FC0010              divu.w  #$0010,d3                   ※..
  //    (以下略)
  //
  public static final boolean MPU_SXMENU = false;  //true=対策を施す。movem.w <ea>,<list>を修正して実害がなくなったので外しておく

  //mpuInit ()
  //  MPUを初期化する
  public static void mpuInit () {
    //コア
    mpuCoreType = mdlCoreRequest;
    mpuRomWait = mpuCoreType == 0 ? 1 : 0;
    mpuIgnoreAddressError = false;
    //レジスタ
    //r = new int[16];
    //FPU
    fpuInit ();  //FPU FPU
    //クロック
    mpuClockTime = 0L;
    mpuClockLimit = 0L;
    mpuClockMHz = mdlClockRequest;
    mpuSetClockMHz (mpuClockMHz);
    mpuCycleCount = 0;
    //タイマ
    mpuTask = null;
    //例外処理
    M68kException.m6eSignal = new M68kException ();
    M68kException.m6eNumber = 0;
    M68kException.m6eAddress = 0;
    M68kException.m6eDirection = MPU_WR_WRITE;
    M68kException.m6eSize = MPU_SS_BYTE;
    //その他
    mpuBootDevice = -1;
    mpuBootAddress = -1;
    //任意の周波数の指定
    //mpuArbFreqOn = !(clockMHz == 10.0 ||
    //                 clockMHz == 50.0 / 3.0 ||  //16.7MHz
    //                 clockMHz == 25.0 ||
    //                 clockMHz == 100.0 / 3.0 ||  //33.3MHz
    //                 clockMHz == 50.0);
    //mpuArbFreqMHz = mpuArbFreqOn ? (int) clockMHz : 100;
    //任意の負荷率の指定
    //mpuUtilOn = false;
    //mpuUtilRatio = 100;

    mpuButtonsRunning = new ArrayList<AbstractButton> ();
    mpuButtonsStopped = new ArrayList<AbstractButton> ();

    //デバッグアクションリスナー
    mpuDebugActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Stop":
          if (RootPointerList.RTL_ON) {
            if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
                RootPointerList.rtlCurrentUserTaskIsStoppable) {
              mpuStop (null);  //"Stop Button"
            }
          } else {
            mpuStop (null);  //"Stop Button"
          }
          break;
        case "Trace":
          mpuAdvance ();
          break;
        case "Skip":
          mpuSkip ();
          break;
        case "Run":
          mpuStart ();
          break;
        }
      }  //actionPerformed(ActionEvent)
    };  //mpuDebugActionListener

  }  //mpuInit()

  //mpuMakeMenu ()
  public static void mpuMakeMenu () {
    //メニュー
    ButtonGroup unitGroup = new ButtonGroup ();
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Reset":  //リセット
          mpuReset (-1, -1);
          break;
        case "Hold down OPT.1 and Reset":  //OPT.1 を押しながらリセット
          mpuReset (0, -1);
          break;
        case "Interrupt":  //インタラプト
          sysInterrupt ();  //インタラプトスイッチが押された
          break;
        case "10MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuClockMHz = 10.0;
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "16.7MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuClockMHz = 50.0 / 3.0;  //16.7MHz
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "25MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuClockMHz = 25.0;
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "33.3MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuClockMHz = 100.0 / 3.0;  //33.3MHz
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "50MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuClockMHz = 50.0;
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "Arbitrary Frequency":  //任意の周波数
          mpuArbFreqOn = true;
          mpuUtilOn = false;
          mpuClockMHz = (double) mpuArbFreqMHz;
          mpuSetClockMHz (mpuClockMHz);
          break;
        case "Arbitrary Utilization":  //任意の負荷率
          mpuArbFreqOn = false;
          mpuUtilOn = true;
          break;
        case "FE Function Instruction":  //FE ファンクション命令
          FEFunction.fpkOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Reject FLOATn.X":  //FLOATn.X を組み込まない
          FEFunction.fpkRejectFloatOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Cut FC2 Pin":  //FC2 ピンをカットする
          busRequestCutFC2Pin = ((JCheckBoxMenuItem) source).isSelected ();
          break;

        case "Run / Stop":  //実行／停止
          if (((JCheckBox) source).isSelected ()) {  //Run
            mpuStart ();
          } else {  //Stop
            if (RootPointerList.RTL_ON) {
              if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
                  RootPointerList.rtlCurrentUserTaskIsStoppable) {
                mpuStop (null);  //"Stop Button"
              }
            } else {
              mpuStop (null);  //"Stop Button"
            }
          }
          pnlPanel.requestFocusInWindow ();  //パネルにフォーカスを戻す。戻さないとキー入力が入らない
          break;
        }
      }
    };
    mpuMenu = createMenu (
      "MPU", 'M',
      mpuResetMenuItem = Multilingual.mlnText (createMenuItem ("Reset", 'R', MNB_MODIFIERS, listener), "ja", "リセット"),
      mpuOpt1ResetMenuItem = Multilingual.mlnText (createMenuItem ("Hold down OPT.1 and Reset", 'O', MNB_MODIFIERS, listener), "ja", "OPT.1 を押しながらリセット"),
      Multilingual.mlnText (createMenuItem ("Interrupt", listener), "ja", "インタラプト"),
      createHorizontalSeparator (),
      mdlMenu,
      createHorizontalSeparator (),
      mpuClock10MenuItem =
      createRadioButtonMenuItem (unitGroup, !mpuArbFreqOn && !mpuUtilOn && mpuClockMHz == 10.0       , "10MHz"  , listener),
      mpuClock16MenuItem =
      createRadioButtonMenuItem (unitGroup, !mpuArbFreqOn && !mpuUtilOn && mpuClockMHz == 50.0 / 3.0 , "16.7MHz", listener),
      mpuClock25MenuItem =
      createRadioButtonMenuItem (unitGroup, !mpuArbFreqOn && !mpuUtilOn && mpuClockMHz == 25.0       , "25MHz"  , listener),
      mpuClock33MenuItem =
      createRadioButtonMenuItem (unitGroup, !mpuArbFreqOn && !mpuUtilOn && mpuClockMHz == 100.0 / 3.0, "33.3MHz", listener),
      mpuClock50MenuItem =
      createRadioButtonMenuItem (unitGroup, !mpuArbFreqOn && !mpuUtilOn && mpuClockMHz == 50.0       , "50MHz"  , listener),
      mpuArbFreqRadioButtonMenuItem =
      Multilingual.mlnText (createRadioButtonMenuItem (unitGroup, mpuArbFreqOn, "Arbitrary Frequency", listener), "ja", "任意の周波数"),
      createHorizontalBox (
        Box.createHorizontalStrut (20),
        mpuArbFreqSpinner =
        createNumberSpinner (
          mpuArbFreqModel = new SpinnerNumberModel (mpuArbFreqMHz, 1, 1000, 1),
          4,
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              //mpuArbFreqRadioButtonMenuItem.setSelected (true);
              mpuArbFreqMHz = mpuArbFreqModel.getNumber ().intValue ();
              if (mpuArbFreqOn) {
                mpuClockMHz = (double) mpuArbFreqMHz;
                mpuSetClockMHz (mpuClockMHz);
              }
            }
          }
          ),
        createLabel ("MHz"),
        Box.createHorizontalGlue ()
        ),
      mpuUtilRadioButtonMenuItem =
      Multilingual.mlnText (createRadioButtonMenuItem (unitGroup, mpuUtilOn, "Arbitrary Utilization", listener), "ja", "任意の負荷率"),
      createHorizontalBox (
        Box.createHorizontalStrut (20),
        mpuUtilSpinner =
        createNumberSpinner (
          mpuUtilModel = new SpinnerNumberModel (mpuUtilRatio, 1, 100, 1),
          4,
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              //mpuUtilRadioButtonMenuItem.setSelected (true);
              mpuUtilRatio = mpuUtilModel.getNumber ().intValue ();
            }
          }
          ),
        createLabel ("%"),
        Box.createHorizontalGlue ()
        ),
      createHorizontalSeparator (),
      fpuMenuItem0,
      fpuMenuItem1,
      fpuMenuItem2,
      fpuMenuItem3,
      Multilingual.mlnText (createCheckBoxMenuItem (FEFunction.fpkOn, "FE Function Instruction", listener), "ja", "FE ファンクション命令"),
      Multilingual.mlnText (createCheckBoxMenuItem (FEFunction.fpkRejectFloatOn, "Reject FLOATn.X", listener), "ja", "FLOATn.X を組み込まない"),
      createHorizontalSeparator (),
      Multilingual.mlnText (createCheckBoxMenuItem (busRequestCutFC2Pin, "Cut FC2 Pin", listener), "ja", "FC2 ピンをカットする")
      );
  }  //mpuMakeMenu()

  //mpuSetClockMHz (mhz)
  //  mpuCurrentMHz,mpuCycleUnit,mpuModifiedUnitを設定する
  //  TMR_FREQ=10^12のとき
  //               mpuModifiedUnitと1サイクルの時間
  //  mpuClockMHz     000/060         030
  //    10.0MHz    100000(100ns)  60000(60ns)
  //    16.7MHz     60000 (60ns)  36000(36ns)
  //    25.0MHz     40000 (40ns)  24000(24ns)
  //    33.3MHz     30000 (30ns)  18000(18ns)
  //    50.0MHz     20000 (20ns)  12000(12ns)
  public static void mpuSetClockMHz (double mhz) {
    mhz = Math.max (1.0, Math.min (1000.0, mhz));
    mpuCurrentMHz = mhz;
    mpuCycleUnit = (int) (((double) TMR_FREQ / 1000000.0) / mhz + 0.5);
    //  DBRA命令で比較するとMC68030は3/5、MC68040は2/5、MC68060は1/5くらい？
    mpuModifiedUnit = (mpuCoreType == 0 ? mpuCycleUnit :
                       mpuCoreType == 3 ? (int) (((double) TMR_FREQ * 3.0 / (5.0 * 1000000.0)) / mhz + 0.5) :
                       mpuCoreType == 4 ? (int) (((double) TMR_FREQ * 2.0 / (5.0 * 1000000.0)) / mhz + 0.5) :
                       mpuCycleUnit);
  }  //mpuSetClockMHz(double)

  //mpuReset (device, address)
  //  MPUをリセットしてからコアのタスクを起動する
  //  コアのタスクが動いているときはそれを中断する
  //  動いていたタスクが完全に止まってから再起動する
  public static void mpuReset (int device, int address) {

    mpuBootDevice = device;
    mpuBootAddress = address;

    if (mpuTask != null) {
      mpuClockLimit = 0L;
      mpuTask.cancel ();
      mpuTask = null;
    }

    tmrTimer.schedule (new TimerTask () {
      @Override public void run () {

        //メモリマップを再構築する
        if (mdlCoreRequest == 0) {
          busRequestExMemoryStart = 0x10000000;
          busRequestExMemorySize = 0 << 20;
          busRequestExMemoryArray = BUS_DUMMY_MEMORY_ARRAY;
        } else if (mdlCoreRequest == 3) {
          busRequestExMemoryStart = 0x01000000;
          busRequestExMemorySize = busHighMemorySize;
          busRequestExMemoryArray = busHighMemoryArray;
        } else {
          busRequestExMemoryStart = 0x10000000;
          busRequestExMemorySize = busLocalMemorySize;
          busRequestExMemoryArray = busLocalMemoryArray;
        }
        busUpdateMemoryMap ();

        //IPLROMの切り替え
        if (romIPLVersion != mdlIPLROMRequest &&  //現在のバージョンがリクエストと違う
            !(mdlIPLROMRequest == 3 && romIPLVersion == 5)) {  //IPLROMのリクエストが3のときは5でも構わない
          byte[] rr = ismGetResource (mdlIPLROMRequest == 0 ? "IPLROM.DAT" :
                                      mdlIPLROMRequest == 1 ? "IPLROMXV.DAT" :
                                      mdlIPLROMRequest == 2 ? "IPLROMCO.DAT" : "IPLROM30.DAT");
          if (rr == null) {  //読み込めない
            return;
          }
          System.arraycopy (rr, 0, MainMemory.mmrM8, 0x00fe0000, 1024 * 128);
          romCheck ();  //IPLROMのバージョンを確認する
          romPatch ();  //IPLROMにパッチをあてる
          if (romIPLVersion < 3) {
            romCheck2 ();  //ROMデバッガとROM Humanのバージョンを確認する
            romPatch2 ();  //ROMデバッガにパッチをあてる
            romPatch3 ();  //ROM Humanにパッチをあてる
          }
        }

        //ROMデバッガ起動フラグ
        romSetROMDBOn (romROMDBOn);

        //MPUの切り替え
        mpuCoreType = mdlCoreRequest;

        //クロックの切り替え
        if (mdlClockRequest > 0.0) {  //機種の指定によるクロックの指定がまだ反映されていない
          if (mdlClockRequest == 10.0) {
            mpuClock10MenuItem.setSelected (true);
            mpuArbFreqOn = false;
            mpuUtilOn = false;
          } else if (mdlClockRequest == 50.0 / 3.0) {  //16.7MHz
            mpuClock16MenuItem.setSelected (true);
            mpuArbFreqOn = false;
            mpuUtilOn = false;
          } else if (mdlClockRequest == 25.0) {
            mpuClock25MenuItem.setSelected (true);
            mpuArbFreqOn = false;
            mpuUtilOn = false;
          } else if (mdlClockRequest == 100.0 / 3.0) {  //33.3MHz
            mpuClock33MenuItem.setSelected (true);
            mpuArbFreqOn = false;
            mpuUtilOn = false;
          } else if (mdlClockRequest == 50.0) {
            mpuClock50MenuItem.setSelected (true);
            mpuArbFreqOn = false;
            mpuUtilOn = false;
          }
          mpuClockMHz = mdlClockRequest;
          mdlClockRequest = -1.0;  //反映済み。クロックを調整してもリセットする度に元に戻ってしまうのは嫌なので2回目からは変更しない
        }
        mpuSetClockMHz (mpuClockMHz);  //mpuCoreTypeが変更されたときは再計算が必要なのでクロックが変わっていなくても省略できない

        RegisterList.drpSetMPU ();

        mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;
        mpuPCR = 0x04300500 | MPU_060_REV << 8;
        MC68060.mmuReset ();  //TCR,ITT0,ITT1,DTT0,DTT1,URP,SRP。060→000/030のときアドレス変換をOFFにする必要がある

        if (mpuCoreType == 0) {

          mpuRomWait = 1;
          mpuIgnoreAddressError = false;
          fpuOn = false;

          //mpuUSP = mpuISP = 0;

        } else if (mpuCoreType == 3) {

          mpuRomWait = 0;
          mpuIgnoreAddressError = true;
          fpuOn = fpuMode != 0;
          fpuBox = fpuMotherboardCoprocessor;
          fpuBox.epbSetMC68882 ();
          if (fpuMode == 1) {
            fpuBox.epbSetExtended ();
          } else if (fpuMode == 2) {
            fpuBox.epbSetTriple ();
          }
          fpuBox.epbReset ();
          fpuFPn = fpuBox.epbFPn;

          //mpuCACR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;

        } else if (mpuCoreType == 6) {

          mpuRomWait = 0;
          mpuIgnoreAddressError = true;
          fpuOn = fpuMode != 0;
          fpuBox = fpuOnChipFPU;
          if (fpuFullSpec) {
            fpuBox.epbSetFullSpec ();
          } else {
            fpuBox.epbSetMC68060 ();
          }
          if (fpuMode == 1) {
            fpuBox.epbSetExtended ();
          } else if (fpuMode == 2) {
            fpuBox.epbSetTriple ();
          }
          fpuBox.epbReset ();
          fpuFPn = fpuBox.epbFPn;

          //mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuURP = mpuSRP = 0;
          mpuPCR = 0x04300500 | MPU_060_REV << 8;

        }

        //! SSPとPCをROMのアドレスから直接読み出している
        regSRT1 = regSRT0 = 0;
        regSRS = REG_SR_S;
        regSRM = 0;
        regSRI = REG_SR_I;
        regCCR = 0;
        Arrays.fill (regRn, 0);
        //r[14] = 0x00001000;  //ROMDB2.32のバグ対策。コードにパッチをあてることにしたので不要
        regRn[15] = MainMemory.mmrRls (0x00ff0000);
        regPC = MainMemory.mmrRls (0x00ff0004);
        //メインメモリ
        MainMemory.mmrReset ();
        //バスコントローラ
        busReset ();
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
          InstructionBreakPoint.ibpReset ();
        }
        if (BranchLog.BLG_ON) {
          BranchLog.blgReset ();
        }
        //割り込み
        mpuIMR = 0;
        mpuIRR = 0;
        if (MC68901.MFP_DELAYED_INTERRUPT) {
          mpuDIRR = 0;
        }
        mpuISR = 0;
        //これでリセット命令が実行されるまでメインメモリとROM以外のデバイスは動かないはず
        //動作開始
        mpuStart ();
      }
    }, TMR_DELAY);

  }  //mpuReset(int,int)

  //mpuStart ()
  //  コアのタスクを起動する
  //  コアのタスクが動いているときはそれを中断する
  //  動いていたタスクが完全に止まってから再起動する
  public static void mpuStart () {
    if (mpuTask != null) {
      mpuClockLimit = 0L;
      mpuTask.cancel ();
      mpuTask = null;
    }
    //停止中だけ有効なボタンを無効にする
    for (AbstractButton button : mpuButtonsStopped) {
      button.setEnabled (false);
    }
    DisassembleList.ddpStoppedBy = null;
    if (mpuCoreType == 0) {
      mpuTask = new TimerTask () {
        @Override public void run () {
          mpuClockLimit = mpuClockTime + TMR_FREQ * TMR_INTERVAL / 1000;
          MC68000.mpuCore ();
        }
      };
    } else if (mpuCoreType == 3) {
      mpuTask = new TimerTask () {
        @Override public void run () {
          mpuClockLimit = mpuClockTime + TMR_FREQ * TMR_INTERVAL / 1000;
          MC68EC030.mpuCore ();
        }
      };
    } else {
      mpuTask = new TimerTask () {
        @Override public void run () {
          mpuClockLimit = mpuClockTime + TMR_FREQ * TMR_INTERVAL / 1000;
          MC68060.mpuCore ();
        }
      };
    }
    tmrTimer.scheduleAtFixedRate (mpuTask, TMR_DELAY, TMR_INTERVAL);  //固定頻度実行
    //動作中だけ有効なボタンを有効にする
    for (AbstractButton button : mpuButtonsRunning) {
      button.setEnabled (true);
    }
  }  //mpuStart()

  //mpuStop (message)
  //  コアのタスクが動いているとき、それを止める
  //  完全に止まってからデバッグダイアログを更新する
  public static void mpuStop (String message) {
    if (mpuTask == null) {  //既に停止しているか停止処理が始まっている
      return;
    }
    DisassembleList.ddpStoppedBy = message;  //停止理由
    mpuClockLimit = 0L;
    mpuTask.cancel ();
    mpuTask = null;
    //動作中だけ有効なボタンを無効にする
    for (AbstractButton button : mpuButtonsRunning) {
      button.setEnabled (false);
    }
    tmrTimer.schedule (new TimerTask () {
      @Override public void run () {
        if (dbgVisibleMask != 0) {  //デバッグ関連ウインドウが表示されている
          if ((dbgVisibleMask & DBG_DDP_VISIBLE_MASK) != 0) {
            DisassembleList.ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
            DisassembleList.ddpUpdate (-1, -1, false);
          }
          if (BranchLog.BLG_ON) {
            if ((dbgVisibleMask & DBG_BLG_VISIBLE_MASK) != 0) {
              BranchLog.blgUpdate (BranchLog.BLG_SELECT_NEWEST);
            }
          }
          if (ProgramFlowVisualizer.PFV_ON) {
            if ((dbgVisibleMask & DBG_PFV_VISIBLE_MASK) != 0) {
              ProgramFlowVisualizer.pfvUpdate ();
            }
          }
          if (RasterBreakPoint.RBP_ON) {
            if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
              RasterBreakPoint.rbpUpdateFrame ();
            }
          }
          if (ScreenModeTest.SMT_ON) {
            if ((dbgVisibleMask & DBG_SMT_VISIBLE_MASK) != 0) {
              ScreenModeTest.smtUpdateFrame ();
            }
          }
          if (RootPointerList.RTL_ON) {
            if ((dbgVisibleMask & DBG_RTL_VISIBLE_MASK) != 0) {
              RootPointerList.rtlUpdateFrame ();
            }
          }
          if (SpritePatternViewer.SPV_ON) {
            if ((dbgVisibleMask & DBG_SPV_VISIBLE_MASK) != 0) {
              SpritePatternViewer.spvUpdateFrame ();
            }
          }
          if (ATCMonitor.ACM_ON) {
            if ((dbgVisibleMask & DBG_ACM_VISIBLE_MASK) != 0) {
              ATCMonitor.acmUpdateFrame ();
            }
          }
        }
        //コンソールにレジスタ一覧を表示する
        if (dgtRequestRegs) {
          ExpressionEvaluator.ElementType.ETY_COMMAND_REGS.etyEval (null);
          dgtRequestRegs = false;
          dgtPrintPrompt ();
        }
        //停止中だけ有効なボタンを有効にする
        for (AbstractButton button : mpuButtonsStopped) {
          button.setEnabled (true);
        }
      }
    }, TMR_DELAY);
  }  //mpuStop(String)

  //mpuAdvance ()
  //  トレース実行
  //  1命令だけ実行する
  //  終わってからデバッグダイアログを更新する
  //  コアが止まっていないときは何もしない
  public static void mpuAdvance () {
    if (mpuTask != null) {  //コアが止まっていない
      return;
    }
    DisassembleList.ddpStoppedBy = null;
    mpuTask = new TimerTask () {
      @Override public void run () {
        mpuClockLimit = mpuClockTime + 1L;
        if (mpuCoreType == 0) {
          MC68000.mpuCore ();
        } else if (mpuCoreType == 3) {
          MC68EC030.mpuCore ();
        } else {
          MC68060.mpuCore ();
        }
        mpuClockLimit = 0L;
        if (mpuTask != null) {  //最初の命令のエラーで停止したときnullになっている場合がある
          mpuTask.cancel ();
          mpuTask = null;
        }
        if (dbgVisibleMask != 0) {  //デバッグ関連ウインドウが表示されている
          if ((dbgVisibleMask & DBG_DDP_VISIBLE_MASK) != 0) {
            DisassembleList.ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
            DisassembleList.ddpUpdate (-1, -1, false);
          }
          if (BranchLog.BLG_ON) {
            if ((dbgVisibleMask & DBG_BLG_VISIBLE_MASK) != 0) {
              BranchLog.blgUpdate (BranchLog.BLG_SELECT_NEWEST);
            }
          }
          if (ProgramFlowVisualizer.PFV_ON) {
            if ((dbgVisibleMask & DBG_PFV_VISIBLE_MASK) != 0) {
              ProgramFlowVisualizer.pfvUpdate ();
            }
          }
          if (RasterBreakPoint.RBP_ON) {
            if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
              RasterBreakPoint.rbpUpdateFrame ();
            }
          }
          if (ScreenModeTest.SMT_ON) {
            if ((dbgVisibleMask & DBG_SMT_VISIBLE_MASK) != 0) {
              ScreenModeTest.smtUpdateFrame ();
            }
          }
          if (RootPointerList.RTL_ON) {
            if ((dbgVisibleMask & DBG_RTL_VISIBLE_MASK) != 0) {
              RootPointerList.rtlUpdateFrame ();
            }
          }
          if (SpritePatternViewer.SPV_ON) {
            if ((dbgVisibleMask & DBG_SPV_VISIBLE_MASK) != 0) {
              SpritePatternViewer.spvUpdateFrame ();
            }
          }
          if (ATCMonitor.ACM_ON) {
            if ((dbgVisibleMask & DBG_ACM_VISIBLE_MASK) != 0) {
              ATCMonitor.acmUpdateFrame ();
            }
          }
        }
        //コンソールにレジスタ一覧を表示する
        if (dgtRequestRegs) {
          ExpressionEvaluator.ElementType.ETY_COMMAND_REGS.etyEval (null);
          dgtRequestRegs = false;
          dgtPrintPrompt ();
        }
      }
    };
    tmrTimer.schedule (mpuTask, TMR_DELAY);
  }  //mpuAdvance()

  //mpuSkip ()
  //  スキップ実行
  //  次の命令が分岐命令のときはトレース実行と同じ
  //  次の命令が分岐命令でないときはその直後まで実行する
  //  コアが止まっていないときは何もしない
  public static void mpuSkip () {
    if (mpuTask != null) {  //コアが止まっていない
      return;
    }
    Disassembler.disDisassemble (new StringBuilder (), regPC, regSRS);
    if ((Disassembler.disStatus & (Disassembler.DIS_ALWAYS_BRANCH | Disassembler.DIS_SOMETIMES_BRANCH)) != 0) {
      mpuAdvance ();
    } else {
      if (InstructionBreakPoint.IBP_ON) {
        InstructionBreakPoint.ibpInstant (Disassembler.disPC, DisassembleList.ddpSupervisorMode);
        mpuStart ();
      }
    }
  }  //mpuSkip()

  //button = mpuMakeBreakButton ()
  //  停止ボタンを作る
  public static final String MPU_BREAK_BUTTON_PATTERN = (
    "11111111111111111111" +
    "1..................1" +
    "1....11......11....1" +
    "1....111....111....1" +
    "1.....111..111.....1" +
    "1......111111......1" +
    "1.......1111.......1" +
    "1.......1111.......1" +
    "1......111111......1" +
    "1.....111..111.....1" +
    "1....111....111....1" +
    "1....11......11....1" +
    "1..................1" +
    "11111111111111111111"
    );
  public static JButton mpuMakeBreakButton () {
    return mpuAddButtonRunning (
      Multilingual.mlnToolTipText (
        createImageButton (
          createImage (20, 14, MPU_BREAK_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[12]),
          createImage (20, 14, MPU_BREAK_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[6]),
          "Stop", mpuDebugActionListener),
        "ja", "停止")
      );
  }  //mpuMakeBreakButton()

  //button = mpuMakeTraceButton ()
  //  トレース実行ボタンを作る
  public static final String MPU_TRACE_BUTTON_PATTERN = (
    "11111111111111111111" +
    "1..................1" +
    "1..................1" +
    "1....11111.........1" +
    "1....11111.........1" +
    "1.......11.........1" +
    "1.......11...1.....1" +
    "1.......11...11....1" +
    "1.......11111111...1" +
    "1.......11111111...1" +
    "1............11....1" +
    "1............1.....1" +
    "1..................1" +
    "11111111111111111111"
    );
  public static JButton mpuMakeTraceButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        createImageButton (
          createImage (20, 14, MPU_TRACE_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[12]),
          createImage (20, 14, MPU_TRACE_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[6]),
          "Trace", mpuDebugActionListener),
        "ja", "トレース実行")
      );
  }  //mpuMakeTraceButton()

  //button = mpuMakeStepButton ()
  //  ステップ実行ボタンを作る
  public static final String MPU_STEP_BUTTON_PATTERN = (
    "11111111111111111111" +
    "1..................1" +
    "1..................1" +
    "1.....111111.......1" +
    "1.....111111.......1" +
    "1.....11..11.......1" +
    "1.....11..11..1....1" +
    "1.....11..11..11...1" +
    "1...1111..1111111..1" +
    "1...1111..1111111..1" +
    "1.............11...1" +
    "1.............1....1" +
    "1..................1" +
    "11111111111111111111"
    );
  public static JButton mpuMakeStepButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        createImageButton (
          createImage (20, 14, MPU_STEP_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[12]),
          createImage (20, 14, MPU_STEP_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[6]),
          "Skip", mpuDebugActionListener),
        "ja", "スキップ実行")
      );
  }  //mpuMakeStepButton()

  //button = mpuMakeRunButton ()
  //  実行ボタンを作る
  public static final String MPU_RUN_BUTTON_PATTERN = (
    "11111111111111111111" +
    "1..................1" +
    "1........11........1" +
    "1........111.......1" +
    "1.........111......1" +
    "1..........111.....1" +
    "1....1111111111....1" +
    "1....1111111111....1" +
    "1..........111.....1" +
    "1.........111......1" +
    "1........111.......1" +
    "1........11........1" +
    "1..................1" +
    "11111111111111111111"
    );
  public static JButton mpuMakeRunButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        createImageButton (
          createImage (20, 14, MPU_RUN_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[12]),
          createImage (20, 14, MPU_RUN_BUTTON_PATTERN, LnF.LNF_RGB[0], LnF.LNF_RGB[6]),
          "Run", mpuDebugActionListener),
        "ja", "実行")
      );
  }  //mpuMakeRunButton()

  //button = mpuAddButtonRunning (button)
  //  MPUが動作中のときだけ有効なボタンを追加する
  public static <T extends AbstractButton> T mpuAddButtonRunning (T button) {
    button.setEnabled (mpuTask != null);
    mpuButtonsRunning.add (button);
    return button;
  }

  //button = mpuAddButtonStopped (button)
  //  MPUが停止中のときだけ有効なボタンを追加する
  public static <T extends AbstractButton> T mpuAddButtonStopped (T button) {
    button.setEnabled (mpuTask == null);
    mpuButtonsStopped.add (button);
    return button;
  }



  //========================================================================================
  //$$EMX エミュレータ拡張命令
  //  下位6bitの組み合わせで64個まで追加できる
  //  オペコードが変更される場合があるので原則としてユーザは使用禁止
  //  オペコード開始位置の上位10bitを変更するときはジャンプテーブルも修正すること
  //
  //  WAIT命令
  //    現在の割り込みマスクレベルで割り込みを受け付ける以外に何もしない
  //    やっていることはBRA *相当の無限ループだが、分岐ログは変化しない
  //    1回あたりの所要時間はMPUの種類や動作周波数に関係なく0.01msとする
  //    ホストのファイルにアクセスするといった時間のかかる処理を行うとき、
  //    コアを止めてしまうと本来ならば割り込みを用いて続けられる音楽やアニメーションが止まってしまうので、
  //    時間のかかる処理は別スレッドで行い、それが終わるまでコアはWAIT命令を繰り返して待つ
  public static final int EMX_OPCODE_BASE    = 0x4e00;  //オペコード開始位置
  public static final int EMX_OPCODE_HFSBOOT = EMX_OPCODE_BASE + 0x00;
  public static final int EMX_OPCODE_HFSINST = EMX_OPCODE_BASE + 0x01;
  public static final int EMX_OPCODE_HFSSTR  = EMX_OPCODE_BASE + 0x02;
  public static final int EMX_OPCODE_HFSINT  = EMX_OPCODE_BASE + 0x03;
  public static final int EMX_OPCODE_EMXNOP  = EMX_OPCODE_BASE + 0x04;

  public static final String[] EMX_MNEMONIC_ARRAY = {
    "hfsboot",
    "hfsinst",
    "hfsstr",
    "hfsint",
    "emxnop",
  };

  //emxNop ()
  //  命令としては何もしない
  //  コードに埋め込んでパッチをあてたりするのに使う
  public static void emxNop () {
    if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x00007140) {  //デバイスドライバを初期化する直前
      int head = regRn[9];  //初期化するデバイスドライバのデバイスヘッダのアドレス
      emxPatchPCM8A (head);
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000716c) {  //デバイスドライバを初期化した直後
      int head = regRn[9];  //初期化されたデバイスドライバのデバイスヘッダのアドレス
      //----------------------------------------------------------------
      //060turbo.sysにパッチをあてる
      //  sysStat_8000::
      //  00000EC0  203C302E3536  move.l  #'0.56',d0
      //  00000EC6  227C30363054  movea.l #'060T',a1
      //  00000ECC  4E75          rts
      if (MC68060.mmuPeekLongData (head + 0x00000ec0, 1) == 0x203c302e &&
          MC68060.mmuPeekLongData (head + 0x00000ec4, 1) == 0x3536227c &&
          MC68060.mmuPeekLongData (head + 0x00000ec8, 1) == 0x30363054) {  //060turbo.sys version 0.56
        //SCSIコールでバスエラーが出ることがある
        //  SRAMのソフト転送フラグを確認する命令がbtstではなくbsetになっている
        //  000021E6  08F9000400ED0070  bset.b  #4,$00ED0070  →  000021E6  0839000400ED0070  btst.b  #4,$00ED0070
        int patched = 0;
        int failed = 0;
        if (MC68060.mmuPeekLongData (head + 0x000021e6, 1) == 0x08f90004 &&
            MC68060.mmuPeekLongData (head + 0x000021ea, 1) == 0x00ed0070) {
          MC68060.mmuPokeWordData (head + 0x000021e6, 0x0839, 1);
          patched++;
        } else {
          failed++;
        }
        XEiJ.prgMessage (new StringBuilder ().
                         append ("060turbo.sys").
                         append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                         append (patched).
                         append ('/').
                         append (patched + failed).
                         append (')').toString ());
      }
      //----------------------------------------------------------------
      //FSX.Xのマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
      if (Z8530.SCC_FSX_MOUSE) {
        emxCheckFSX (head);
      }
      //----------------------------------------------------------------
      //TwentyOne.xのオプションのアドレスを保存する
      if (HFS.HFS_USE_TWENTY_ONE) {
        emxCheckTwentyOne (head);
      }
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000972c) {  //プロセスを起動する直前
      int head = regRn[8] + 256;  //起動するプロセスのメモリ管理テーブルのアドレス+256=プロセスの先頭
      //System.out.println (MC68060.mmuPeekStringZ (head - 60, 1));  //実行ファイル名
      emxPatchPCM8A (head);
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000a090) {  //プロセスが常駐した直後
      int head = regRn[8] + 256;  //常駐したプロセスのメモリ管理テーブルのアドレス+256=プロセスの先頭
      //----------------------------------------------------------------
      //FSX.Xのマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
      if (Z8530.SCC_FSX_MOUSE) {
        emxCheckFSX (head);
      }
      //----------------------------------------------------------------
      //TwentyOne.xのオプションのアドレスを保存する
      if (HFS.HFS_USE_TWENTY_ONE) {
        emxCheckTwentyOne (head);
      }
    }
  }  //emxNop()

  public static final int[] emxPCM8AFFMap = {
    0x00000138, 0x000001f6, 0x00000394, 0x000011ec, 0x0000120a, 0x00001400, 0x00001814, 0x00001870, 0x00001882, 0x0000188a,
    0x00001892, 0x000018a2, 0x000018a8, 0x000018ca, 0x000018d4, 0x000018e0, 0x000018e8, 0x00001908, 0x000019e4, 0x00001afa,
    0x00001b58, 0x00001b7c, 0x00001bac, 0x00001c38, 0x00001ccc, 0x000021f8, 0x00002250, 0x00002258, 0x00002290, 0x000022a6,
    0x000022b0, 0x000022c0, 0x000022c8, 0x000022de, 0x000022ea, 0x000030c8, 0x000030de, 0x000030e6, 0x000030ea, 0x000030f6,
    0x00003112, 0x00003188, 0x0000334c, 0x0000338a, 0x000033a2, 0x000033c4, 0x000033d0, 0x0000341a, 0x00003428, 0x00003496,
    0x000034a6, 0x000034d6, 0x0000fe0e, 0x0000fec8, 0x0000feec, 0x0000ff46, 0x0000ff4e,
  };

  //emxPatchPCM8A (head)
  //  headから始まるデバイスドライバまたはプロセスがPCM8A.X v1.02ならばパッチをあてる
  public static void emxPatchPCM8A (int head) {
    if (MC68060.mmuPeekLongData (head + 0x10f8, 1) == 0x50434d38 &&  //PCM8
        MC68060.mmuPeekLongData (head + 0x10fc, 1) == 0x41313032) {  //A102
      int patched = 0;
      int failed = 0;
      //  I/Oポートのアドレスの上位8ビットが$FFになっているところを$00に修正します。(57箇所)
      for (int offset : emxPCM8AFFMap) {
        if (MC68060.mmuPeekByteZeroData (head + offset, 1) == 0xff) {
          MC68060.mmuPokeByteData (head + offset, 0x00, 1);
          patched++;
        } else {
          failed++;
        }
      }
      if (patched != 0) {
        prgMessage (new StringBuilder ().
                    append ("PCM8A.X v1.02").
                    append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                    append (patched).
                    append ('/').
                    append (patched + failed).
                    append (')').toString ());
      }
    }
  }  //emxPatchPCM8A(int)

  //emxCheckFSX (head)
  //  headから始まるデバイスドライバまたはプロセスがFSX.Xならばマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
  public static void emxCheckFSX (int head) {
    if (Z8530.SCC_FSX_MOUSE) {
      if ("\r\nSX SYSTEM for X68000  version 3.10\r\nCopyright 1990,91,92,93,94 SHARP/First Class Technology\r\n".equals (MC68060.mmuPeekStringZ (head + 0x0001ae, 5))) {
        Z8530.sccFSXMouseHook = head + 0x04f82a;  //マウス受信データ処理ルーチン
        Z8530.sccFSXMouseWork = head + 0x063184;  //マウスワーク
        XEiJ.prgMessage (Multilingual.mlnJapanese ? "FSX.X を検出しました" : "FSX.X was detected");
      }
    }
  }  //emxCheckFSX(int)

  //emxCheckTwentyOne (head)
  //  headから始まるデバイスドライバまたはプロセスがTwentyOne.xならばオプションのアドレスを保存する
  public static void emxCheckTwentyOne (int head) {
    if (HFS.HFS_USE_TWENTY_ONE) {
      if (MainMemory.mmrTwentyOneOptionAddress != 0 ||  //TwentyOne.xのオプションのアドレスは確認済みまたは非対応のバージョン
          MainMemory.mmrHumanVersion <= 0) {  //Human68kのバージョンが未確認または未知のバージョン
        return;
      }
      int name1 = MC68060.mmuPeekLongData (head + 14, 1);
      if (name1 == ('*' << 24 | 'T' << 16 | 'w' << 8 | 'e')) {
        int name2 = MC68060.mmuPeekLongData (head + 18, 1);
        if (name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | '*')) {  //TwentyOne.x v1.10まで
          MainMemory.mmrTwentyOneOptionAddress = -1;  //非対応
        }
      } else if (name1 == ('?' << 24 | 'T' << 16 | 'w' << 8 | 'e')) {
        int name2 = MC68060.mmuPeekLongData (head + 18, 1);
        if (name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | '?') ||
            name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | 'E')) {  //TwentyOne.x v1.11から
          MainMemory.mmrTwentyOneOptionAddress = head + 22;
          XEiJ.prgMessage (Multilingual.mlnJapanese ? "TwentyOne.x を検出しました" : "TwentyOne.x was detected");
        }
      }
    }
  }  //emxCheckTwentyOne(int)



  //========================================================================================
  //$$IRP 命令の処理
  //
  //  変数名
  //    op                                   オペコード。iiii_qqq_nnn_mmm_rrr
  //    iiii  op >> 12                       命令の種類。ここでは定数
  //    qqq   (op >> 9) - (iiii << 3)        クイックイミディエイトまたはデータレジスタDqの番号
  //    aqq   (op >> 9) - ((iiii << 3) - 8)  アドレスレジスタAqの番号
  //    nnn   op >> 6 & 7                    デスティネーションの実効アドレスのモード
  //    ea    op & 63                        実効アドレスのモードとレジスタ
  //    mmm   ea >> 3                        実効アドレスのモード
  //    rrr   op & 7                         実効アドレスのレジスタ。DrまたはRrのときr[rrr]はr[ea]で代用できる
  //    cccc  op >> 8 & 15                   コンディションコード
  //    a                                    実効アドレス
  //    s                                    テンポラリ
  //    t                                    テンポラリ
  //    w                                    拡張ワード
  //    x                                    被演算数
  //    y                                    演算数
  //    z                                    結果
  //
  //  サイクル数
  //    mpuCycleCountにMC68000のサイクル数を加算する
  //      MC68030のサイクル数はMC68000のサイクル数の0.6倍とみなして計算する
  //      これはROM1.3で起動したときDBRA命令で計測される動作周波数の表示の辻褄を合わせるための係数であり、
  //      MC68000とMC68030のサイクル数の比の平均値ではない
  //        10MHzのMC68000と25MHzのMC68030の速度の比が25/10/0.6=4.17倍となるので何倍も外れてはいないと思われる
  //    MC68000に存在しない命令のサイクル数はMC68000に存在すると仮定した場合の推定値を用いる
  //      オペコードを含むリードとライトを1ワードあたり4サイクルとする
  //
  //  拡張命令
  //    差し障りのない範囲でいくつかの命令を追加してある
  //    エミュレータ拡張命令
  //      HFSBOOT                                         |-|012346|-|-----|-----|          |0100_111_000_000_000
  //      HFSINST                                         |-|012346|-|-----|-----|          |0100_111_000_000_001
  //      HFSSTR                                          |-|012346|-|-----|-----|          |0100_111_000_000_010
  //      HFSINT                                          |-|012346|-|-----|-----|          |0100_111_000_000_011
  //      EMXNOP                                          |-|012346|-|-----|-----|          |0100_111_000_000_100
  //    MC68000で欠番になっているオペコードに割り当てられているColdFireの命令
  //      BITREV.L Dr                                     |-|------|-|-----|-----|D         |0000_000_011_000_rrr (ISA_C)
  //      BYTEREV.L Dr                                    |-|------|-|-----|-----|D         |0000_001_011_000_rrr (ISA_C)
  //      FF1.L Dr                                        |-|------|-|-UUUU|-**00|D         |0000_010_011_000_rrr (ISA_C)
  //      MVS.B <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr (ISA_B)
  //      MVS.W <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr (ISA_B)
  //      MVZ.B <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr (ISA_B)
  //      MVZ.W <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr (ISA_B)
  //      SATS.L Dr                                       |-|------|-|-UUUU|-**00|D         |0100_110_010_000_rrr (ISA_B)

  public static final boolean IRP_BITREV_REVERSE = false;  //true=BITREVでInteger.reverseを使う
  public static final boolean IRP_BITREV_SHIFT = false;  //true=BITREVでシフト演算子を使う
  public static final boolean IRP_BITREV_TABLE = true;  //true=BITREVでテーブルを使う

  public static final boolean IRP_MOVEM_MAINMEMORY = true;  //true=000のときMOVEMでメインメモリを特別扱いにする
  public static final boolean IRP_MOVEM_EXPAND = false;  //true=MOVEMで16回展開する。遅くなる
  public static final boolean IRP_MOVEM_LOOP = false;  //true=MOVEMで16回ループする。コンパイラが展開する
  public static final boolean IRP_MOVEM_SHIFT_LEFT = false;  //true=MOVEMで0になるまで左にシフトする。reverseが入る分遅い
  public static final boolean IRP_MOVEM_SHIFT_RIGHT = true;  //true=MOVEMで0になるまで右にシフトする
  public static final boolean IRP_MOVEM_ZEROS = false;  //true=MOVEMでInteger.numberOfTrailingZerosを使う。ループ回数は少ないがスキップする処理が冗長になるので最速ではない

  public static void irpReset () {
    //メインメモリのmmrResetとバスコントローラのbusResetはmpuResetへ
    CRTC.crtReset ();  //CRT CRTコントローラ
    VideoController.vcnReset ();  //VCN ビデオコントローラ
    HD63450.dmaReset ();  //DMA DMAコントローラ
    MC68901.mfpReset ();  //MFP MFP
    Keyboard.kbdRePress();  //押されているキーを再入力する
    RP5C15.rtcReset ();  //RTC RTC
    PrinterPort.prnReset ();  //PRN プリンタポート
    SoundSource.sndReset ();  //SND サウンド
    YM2151.opmReset ();  //OPM FM音源
    ADPCM.pcmReset ();  //PCM ADPCM音源
    FDC.fdcReset ();  //FDC FDコントローラ
    XEiJ.ioiReset ();  //IOI I/O割り込み
    XEiJ.eb2Reset ();  //EB2 拡張ボードレベル2割り込み
    SPC.spcReset ();  //SPC SCSIプロトコルコントローラ
    Z8530.sccReset ();  //SCC SCC
    XEiJ.ppiReset ();  //PPI PPI
    HFS.hfsReset ();  //HFS ホストファイルシステムインタフェイス
    SpriteScreen.sprReset ();  //SPR スプライト画面
    //smrReset()はspcSCSIEXOnとspcSCSIINOnを使うのでSPC.spcReset()よりも後であること
    XEiJ.smrReset ();  //SMR SRAM
  }  //irpReset()

  //右シフト・ローテート命令
  //
  //  ASR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    ASR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｱｱｲｳｴｵｶｷ ｸ ｸ
  //       :
  //       7 ........................ｱｱｱｱｱｱｱｱ ｲ ｲ
  //       8 ........................ｱｱｱｱｱｱｱｱ ｱ ｱ
  //    ASR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       :
  //      15 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲ ｲ
  //      16 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱ ｱ
  //    ASR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       :
  //      31 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲ ｲ
  //      32 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱ ｱ
  //
  //  LSR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    LSR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................0ｱｲｳｴｵｶｷ ｸ ｸ
  //       :
  //       7 ........................0000000ｱ ｲ ｲ
  //       8 ........................00000000 ｱ ｱ
  //       9 ........................00000000 0 0
  //    LSR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       :
  //      15 ................000000000000000ｱ ｲ ｲ
  //      16 ................0000000000000000 ｱ ｱ
  //      17 ................0000000000000000 0 0
  //    LSR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       :
  //      31 0000000000000000000000000000000ｱ ｲ ｲ
  //      32 00000000000000000000000000000000 ｱ ｱ
  //      33 00000000000000000000000000000000 0 0
  //
  //  ROXR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  //    ROXR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X X
  //       1 ........................Xｱｲｳｴｵｶｷ ｸ ｸ
  //       2 ........................ｸXｱｲｳｴｵｶ ｷ ｷ
  //       :
  //       7 ........................ｳｴｵｶｷｸXｱ ｲ ｲ
  //       8 ........................ｲｳｴｵｶｷｸX ｱ ｱ
  //       9 ........................ｱｲｳｴｵｶｷｸ X X
  //    ROXR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //       1 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       2 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿ ｿ
  //       :
  //      15 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲ ｲ
  //      16 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱ ｱ
  //      17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //    ROXR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //       1 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       2 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏ ﾏ
  //       :
  //      31 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲ ｲ
  //      32 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱ ｱ
  //      33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //
  //  ROR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  //    ROR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｸｱｲｳｴｵｶｷ X ｸ
  //       :
  //       7 ........................ｲｳｴｵｶｷｸｱ X ｲ
  //       8 ........................ｱｲｳｴｵｶｷｸ X ｱ
  //    ROR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ X ﾀ
  //       :
  //      15 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ X ｲ
  //      16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X ｱ
  //    ROR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ X ﾐ
  //       :
  //      31 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ X ｲ
  //      32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X ｱ

  //左シフト・ローテート命令
  //
  //  ASL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    ASL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸ0 ｱ ｱ
  //       :
  //       7 ........................ｸ0000000 ｷ ｷ
  //       8 ........................00000000 ｸ ｸ
  //       9 ........................00000000 0 0
  //    ASL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱ ｱ
  //       :
  //      15 ................ﾀ000000000000000 ｿ ｿ
  //      16 ................0000000000000000 ﾀ ﾀ
  //      17 ................0000000000000000 0 0
  //    ASL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱ ｱ
  //       :
  //      31 ﾐ0000000000000000000000000000000 ﾏ ﾏ
  //      32 00000000000000000000000000000000 ﾐ ﾐ
  //      33 00000000000000000000000000000000 0 0
  //
  //  LSL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    LSL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸ0 ｱ ｱ
  //       :
  //       7 ........................ｸ0000000 ｷ ｷ
  //       8 ........................00000000 ｸ ｸ
  //       9 ........................00000000 0 0
  //    LSL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱ ｱ
  //       :
  //      15 ................ﾀ000000000000000 ｿ ｿ
  //      16 ................0000000000000000 ﾀ ﾀ
  //      17 ................0000000000000000 0 0
  //    LSL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱ ｱ
  //       :
  //      31 ﾐ0000000000000000000000000000000 ﾏ ﾏ
  //      32 00000000000000000000000000000000 ﾐ ﾐ
  //      33 00000000000000000000000000000000 0 0
  //
  //  ROXL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  //    ROXL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X X
  //       1 ........................ｲｳｴｵｶｷｸX ｱ ｱ
  //       2 ........................ｳｴｵｶｷｸXｱ ｲ ｲ
  //       :
  //       7 ........................ｸXｱｲｳｴｵｶ ｷ ｷ
  //       8 ........................Xｱｲｳｴｵｶｷ ｸ ｸ
  //       9 ........................ｱｲｳｴｵｶｷｸ X X
  //    ROXL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱ ｱ
  //       2 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲ ｲ
  //       :
  //      15 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿ ｿ
  //      16 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //      17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //    ROXL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱ ｱ
  //       2 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲ ｲ
  //       :
  //      31 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏ ﾏ
  //      32 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //      33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //
  //  ROL
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  //    ROL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸｱ X ｱ
  //       :
  //       7 ........................ｸｱｲｳｴｵｶｷ X ｷ
  //       8 ........................ｱｲｳｴｵｶｷｸ X ｸ
  //    ROL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ X ｱ
  //       :
  //      15 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ X ｿ
  //      16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X ﾀ
  //    ROL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ X ｱ
  //       :
  //      31 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ X ﾏ
  //      32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X ﾐ



  //========================================================================================
  //以下のメソッドはインライン展開する
/*
  public static void pushb (int x) throws M68kException {
    wb (r[15] -= 2, x);  //ワードの上位側
  }  //pushb(int)
  public static void pushw (int x) throws M68kException {
    ww (r[15] -= 2, x);
  }  //pushw(int)
  public static void pushl (int x) throws M68kException {
    wl (r[15] -= 4, x);
  }  //pushl(int)

  public static int popbs () throws M68kException {
    return rbs ((r[15] += 2) - 2);  //ワードの上位側
  }  //popbs()
  public static int popbz () throws M68kException {
    return rbz ((r[15] += 2) - 2);  //ワードの上位側
  }  //popbz()
  public static int popws () throws M68kException {
    return rws ((r[15] += 2) - 2);
  }  //popws()
  public static int popwz () throws M68kException {
    return rwz ((r[15] += 2) - 2);
  }  //popwz()
  public static int popls () throws M68kException {
    return rls ((r[15] += 4) - 4);
  }  //popls()

  public static int pcbs () throws M68kException {
    return rbs ((pc += 2) - 1);  //ワードの下位側
  }  //pcbs()
  public static int pcbz () throws M68kException {
    return rbz ((pc += 2) - 1);  //ワードの下位側
  }  //pcbz()
  public static int pcws () throws M68kException {
    return rwse ((pc += 2) - 2);
  }  //pcws()
  public static int pcwz () throws M68kException {
    return rwze ((pc += 2) - 2);
  }  //pcwz()
  public static int pcls () throws M68kException {
    return rlse ((pc += 4) - 4);
  }  //pcls()

  public static void ccr_tst (int z) {  //Xは変化しない。VとCはクリア
    ccr = z >> 28 & CCR_N | (z == 0 ? ccr & CCR_X | CCR_Z : ccr & CCR_X);  //ccr_tst
  }  //ccr_tst(int)
  public static void ccr_btst (int z) {  //Z以外は変化しない
    ccr = (ccr & (CCR_X | CCR_N | CCR_V | CCR_C) | (z == 0 ? CCR_Z : 0));
  }  //ccr_btst(int)
  public static void ccr_clr () {  //Xは変化しない。Zはセット。NとVとCはクリア
    ccr = ccr & CCR_X | CCR_Z;  //ccr_clr
  }  //ccr_clr(int)

  //                  x-y V                                  x-y C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  0  -1  -2  -3   4*  3   2   1     0 |  0  -1* -2* -3* -4* -5* -6* -7*
  //    1 |  1   0  -1  -2   5*  4*  3   2     1 |  1   0  -1* -2* -3* -4* -5* -6*
  //    2 |  2   1   0  -1   6*  5*  4*  3     2 |  2   1   0  -1* -2* -3* -4* -5*
  //    3 |  3   2   1   0   7*  6*  5*  4*    3 |  3   2   1   0  -1* -2* -3* -4*
  //   -4 | -4  -5* -6* -7*  0  -1  -2  -3     4 |  4   3   2   1   0  -1* -2* -3*
  //   -3 | -3  -4  -5* -6*  1   0  -1  -2     5 |  5   4   3   2   1   0  -1* -2*
  //   -2 | -2  -3  -4  -5*  2   1   0  -1     6 |  6   5   4   3   2   1   0  -1*
  //   -1 | -1  -2  -3  -4   3   2   1   0     7 |  7   6   5   4   3   2   1   0
  //                 x-y-1 V                                x-y-1 C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 | -1  -2  -3  -4   3   2   1   0     0 | -1* -2* -3* -4* -5* -6* -7* -8*
  //    1 |  0  -1  -2  -3   4*  3   2   1     1 |  0  -1* -2* -3* -4* -5* -6* -7*
  //    2 |  1   0  -1  -2   5*  4*  3   2     2 |  1   0  -1* -2* -3* -4* -5* -6*
  //    3 |  2   1   0  -1   6*  5*  4*  3     3 |  2   1   0  -1* -2* -3* -4* -5*
  //   -4 | -5* -6* -7* -8* -1  -2  -3  -4     4 |  3   2   1   0  -1* -2* -3* -4*
  //   -3 | -4  -5* -6* -7*  0  -1  -2  -3     5 |  4   3   2   1   0  -1* -2* -3*
  //   -2 | -3  -4  -5* -6*  1   0  -1  -2     6 |  5   4   3   2   1   0  -1* -2*
  //   -1 | -2  -3  -4  -5*  2   1   0  -1     7 |  6   5   4   3   2   1   0  -1*
  //  x        y         z=x-y    v        c         z=x-y-1  v        c
  //  00000000 00001111  01111000 00001000 01111111  11110000 00000000 11111111
  //  00000000 00001111  00111100 00001100 00111111  01111000 00001000 01111111
  //  00000000 00001111  00011110 00001110 00011111  00111100 00001100 00111111
  //  00000000 00001111  00001111 00001111 00001111  00011110 00001110 00011111
  //  11111111 00001111  10000111 01110000 00000111  00001111 11110000 00001111
  //  11111111 00001111  11000011 00110000 00000011  10000111 01110000 00000111
  //  11111111 00001111  11100001 00010000 00000001  11000011 00110000 00000011
  //  11111111 00001111  11110000 00000000 00000000  11100001 00010000 00000001
  //  Vは右上と左下でxとzが異なる部分
  //    V = ((x ^ y) & (x ^ z)) < 0
  //  Cは右上全部および左上と右下でzがある部分
  //    C = (~x & y | ~(x ^ y) & z) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    C = (x & (y ^ z) ^ (y | z)) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,(1^$x)&$y|(1^($x^$y))&$z,$x&($y^$z)^($y|$z)).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,1,1
  //      0,1,0,1,1
  //      0,1,1,1,1
  //      1,0,0,0,0
  //      1,0,1,0,0
  //      1,1,0,0,0
  //      1,1,1,1,1
  public static void ccr_sub (int x, int y, int z) {
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (CCR_X | CCR_C));
  }  //ccr_sub(int,int,int)
  public static void ccr_subx (int x, int y, int z) {  //Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (CCR_X | CCR_C));
  }  //ccr_subx(int,int,int)
  public static void ccr_subq (int x, int y, int z) {  //ccr_subを常にy>0としたもの。Vは負→正のとき1。Cは正→負のとき1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (x & ~z) >>> 31 << 1 |
           (~x & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_subq(int,int,int)
  public static void ccr_neg (int y, int z) {  //ccr_subを常にx==0としたもの。Vは-MAX→-MAXのみ1。Cは0→0以外1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_neg(int,int)
  public static void ccr_negx (int y, int z) {  //ccr_subxを常にx==0としたもの。Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_negx(int,int)
  public static void ccr_cmp (int x, int y, int z) {  //Xは変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_X | CCR_Z : ccr & CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);
  }  //ccr_cmp(int,int,int)

  //                  x+y V                                  x+y C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  0   1   2   3  -4  -3  -2  -1     0 |  0   1   2   3   4   5   6   7
  //    1 |  1   2   3   4* -3  -2  -1   0     1 |  1   2   3   4   5   6   7   8*
  //    2 |  2   3   4*  5* -2  -1   0   1     2 |  2   3   4   5   6   7   8*  9*
  //    3 |  3   4*  5*  6* -1   0   1   2     3 |  3   4   5   6   7   8*  9* 10*
  //   -4 | -4  -3  -2  -1  -8* -7* -6* -5*    4 |  4   5   6   7   8*  9* 10* 11*
  //   -3 | -3  -2  -1   0  -7* -6* -5* -4     5 |  5   6   7   8*  9* 10* 11* 12*
  //   -2 | -2  -1   0   1  -6* -5* -4  -3     6 |  6   7   8*  9* 10* 11* 12* 13*
  //   -1 | -1   0   1   2  -5* -4  -3  -2     7 |  7   8*  9* 10* 11* 12* 13* 14*
  //                 x+y+1 V                                x+y+1 C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  1   2   3   4* -3  -2  -1   0     0 |  1   2   3   4   5   6   7   8*
  //    1 |  2   3   4*  5* -2  -1   0   1     1 |  2   3   4   5   6   7   8*  9*
  //    2 |  3   4*  5*  6* -1   0   1   2     2 |  3   4   5   6   7   8*  9* 10*
  //    3 |  4*  5*  6*  7*  0   1   2   3     3 |  4   5   6   7   8*  9* 10* 11*
  //   -4 | -3  -2  -1   0  -7* -6* -5* -4     4 |  5   6   7   8*  9* 10* 11* 12*
  //   -3 | -2  -1   0   1  -6* -5* -4  -3     5 |  6   7   8*  9* 10* 11* 12* 13*
  //   -2 | -1   0   1   2  -5* -4  -3  -2     6 |  7   8*  9* 10* 11* 12* 13* 14*
  //   -1 |  0   1   2   3  -4  -3  -2  -1     7 |  8*  9* 10* 11* 12* 13* 14* 15*
  //  x        y         z=x+y    v        c         z=x+y+1  v        c
  //  00000000 00001111  00001111 00000000 00000000  00011110 00010000 00000001
  //  00000000 00001111  00011110 00010000 00000001  00111100 00110000 00000011
  //  00000000 00001111  00111100 00110000 00000011  01111000 01110000 00000111
  //  00000000 00001111  01111000 01110000 00000111  11110000 11110000 00001111
  //  11111111 00001111  11110000 00001111 00001111  11100001 00001110 00011111
  //  11111111 00001111  11100001 00001110 00011111  11000011 00001100 00111111
  //  11111111 00001111  11000011 00001100 00111111  10000111 00001000 01111111
  //  11111111 00001111  10000111 00001000 01111111  00001111 00000000 11111111
  //  Vは左上と右下でxとzが異なる部分
  //    V = (~(x ^ y) & (x ^ z)) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    V = ((x ^ z) & (y ^ z)) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,(1^($x^$y))&($x^$z),($x^$z)&($y^$z)).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,1,1
  //      0,1,0,0,0
  //      0,1,1,0,0
  //      1,0,0,0,0
  //      1,0,1,0,0
  //      1,1,0,1,1
  //      1,1,1,0,0
  //  Cは右下全部および右上と左下でzがない部分
  //    C = (x & y | (x ^ y) & ~z) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    C = ((x | y) ^ (x ^ y) & z) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,$x&$y|($x^$y)&(1^$z),($x|$y)^($x^$y)&$z).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,0,0
  //      0,1,0,1,1
  //      0,1,1,0,0
  //      1,0,0,1,1
  //      1,0,1,0,0
  //      1,1,0,1,1
  //      1,1,1,1,1
  public static void ccr_add (int x, int y, int z) {
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_add(int,int,int)
  public static void ccr_addx (int x, int y, int z) {  //Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_addx(int,int,int)
  public static void ccr_addq (int x, int y, int z) {  //ccr_addを常にy>0としたもの。Vは正→負のとき1。Cは負→正のとき1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (~x & z) >>> 31 << 1 |
           (x & ~z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_addq(int,int,int)
*/



  //========================================================================================
  //$$EFA 実効アドレス計算
  //
  //  アドレッシングモードとサイクル数
  //    DrまたはArを指定すると不当命令になる
  //    例えばArを指定できない命令ならDrだけを特別扱いにして残りをefa～に渡せばArは自動的に不当命令になる
  //    実効アドレス計算とオペランドのアクセス1回分のサイクル数がXEiJ.mpuCycleCountに加算される
  //                        mmm rrr  Any Mem Mlt Cnt Clt  Byte Word Long Quad Extd  LeaPea JmpJsr
  //         -------------  --- ---  --- --- --- --- ---  ---- ---- ---- ---- ----  ------ ------
  //      M  (Ar)           010 rrr    *   *   *   *   *     4    4    8   16   24       4      8
  //      +  (Ar)+          011 rrr    *   *   *             4    4    8   16   24
  //      -  -(Ar)          100 rrr    *   *   *             6    6   10   18   26
  //      W  (d16,Ar)       101 rrr    *   *   *   *   *     8    8   12   20   28       8     10
  //      X  (d8,Ar,Rn.wl)  110 rrr    *   *   *   *   *    10   10   14   22   30      12     14
  //      Z  (xxx).W        111 000    *   *   *   *   *     8    8   12   20   28       8     10
  //      Z  (xxx).L        111 001    *   *   *   *   *    12   12   16   24   32      12     12
  //      P  (d16,PC)       111 010    *   *       *         8    8   12   20   28       8     10
  //      P  (d8,PC,Rn.wl)  111 011    *   *       *        10   10   14   22   30      12     14
  //      I  #<data>        111 100    *                     4    4    8   16   24
  //    MoveToMemByte/MoveToMemWord/MoveToMemLongはデスティネーションが-(Aq)のとき2減らす
  //    AddToRegLong/AddaLong/AndToRegLong/OrToRegLong/SubToRegLong/SubaLongはソースがDr/Ar/#<data>のとき2増やす
  //
  //  フルフォーマットの拡張ワードの処理の冗長表現
  //      t = r[ea - (0b110_000 - 8)];  //ベースレジスタ
  //      w = rwze ((pc += 2) - 2);  //pcwz。拡張ワード
  //      x = r[w >> 12];  //インデックスレジスタ
  //      if ((w & 0x0800) == 0) {  //ワードインデックス
  //        x = (short) x;
  //      }
  //      x <<= w >> 9 & 3;  //スケールファクタ。ワードインデックスのときは符号拡張してから掛ける
  //      if ((w & 0x0100) == 0) {  //短縮フォーマット
  //        t += (byte) w + x;  //8ビットディスプレースメント
  //      } else {  //フルフォーマット
  //        if ((w & 0x0080) != 0) {  //ベースサプレス
  //          t = 0;
  //        }
  //        if ((w & 0x0040) != 0) {  //インデックスサプレス
  //          x = 0;
  //        }
  //        if ((w & 0x0020) != 0) {  //ベースディスプレースメントあり
  //          if ((w & 0x0010) == 0) {  //ワードベースディスプレースメント
  //            t += rwse ((pc += 2) - 2);  //pcws
  //          } else {  //ロングベースディスプレースメント
  //            t += rlse ((pc += 4) - 4);  //pcls
  //          }
  //        }
  //        if ((w & 0x0003) == 0) {  //メモリ間接なし
  //          t += x;
  //        } else {  //メモリ間接あり
  //          if ((w & 0x0004) == 0) {  //プリインデックス
  //            t = rls (t + x);
  //          } else {  //ポストインデックス
  //            t = rls (t) + x;
  //          }
  //          if ((w & 0x0002) != 0) {  //アウタディスプレースメントあり
  //            if ((w & 0x0001) == 0) {  //ワードアウタディスプレースメント
  //              t += rwse ((pc += 2) - 2);  //pcws
  //            } else {  //ロングアウタディスプレースメント
  //              t += rlse ((pc += 4) - 4);  //pcls
  //            }
  //          }
  //        }
  //      }
  //      return t;
  //
  //  フルフォーマットの拡張ワードのサイクル数
  //    ベースディスプレースメントとメモリ間接とアウターディスプレースメントのリード回数に応じてサイクル数を加算する
  //    ベースレジスタとインデックスレジスタとスケールファクタの有無はサイクル数に影響しないものとする
  //      fedcba9876543210  bd  []  od  計
  //      .......0........               0  (d8,～)
  //      .......1..01..00               0  (～)
  //      .......1..01..01       8       8  ([～])
  //      .......1..01..10       8   4  12  ([～],od.W)
  //      .......1..01..11       8   8  16  ([～],od.L)
  //      .......1..10..00   4           4  (bd.W,～)
  //      .......1..10..01   4   8      12  ([bd.W,～])
  //      .......1..10..10   4   8   4  16  ([bd.W,～],od.W)
  //      .......1..10..11   4   8   8  20  ([bd.W,～],od.L)
  //      .......1..11..00   8           8  (bd.L,～)
  //      .......1..11..01   8   8      16  ([bd.L,～])
  //      .......1..11..10   8   8   4  20  ([bd.L,～],od.W)
  //      .......1..11..11   8   8   8  24  ([bd.L,～],od.L)
  //    1つの式で書くこともできるが冗長になるのでテーブル参照にする
  //
  //  MC68060のサイクル数
  //    ブリーフフォーマットは0、フルフォーマットのメモリ間接なしは1、フルフォーマットのメモリ間接ありは3
  //

  //拡張ワードのサイクル数
/*
  public static final int[] EFA_EXTENSION_CLK = {                  //fedcba9876543210
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
  };
*/
  //  perl misc/itob.pl xeij/XEiJ.java EFA_EXTENSION_CLK
  public static final byte[] EFA_EXTENSION_CLK = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30".getBytes (XEiJ.ISO_8859_1);

  public static final boolean EFA_SEPARATE_AR = false;  //true=(Ar)を(A0)～(A7)に分ける



  //========================================================================================
  //$$BUS バスコントローラ

  public static final boolean BUS_SPLIT_UNALIGNED_LONG = false;  //true=4の倍数ではない偶数アドレスへのロングワードアクセスを常に分割する

  //マザーボードのアドレス空間
  public static final int BUS_MOTHER_BITS = 24;  //マザーボードのアドレス空間のビット数
  public static final int BUS_MOTHER_SIZE = BUS_MOTHER_BITS < 32 ? 1 << BUS_MOTHER_BITS : 0;  //マザーボードのアドレス空間のサイズ。1<<32は1が押し出されて0になるのではなくてシフトカウントが5bitでマスクされて1<<0=1になることに注意
  public static final int BUS_MOTHER_MASK = BUS_MOTHER_SIZE - 1;  //マザーボードのアドレスのマスク。a&BUS_MOTHER_MASK
  //ダミー4bitページ
  public static final int BUS_DUMMY_4BIT_PAGE = BUS_MOTHER_SIZE;
  public static final int BUS_DUMMY_4BIT_SIZE = 2 * 512 * 512;  //256KB
  //1024ドット256色(改造)
  public static final int BUS_MODIFIED_256_PAGE = BUS_DUMMY_4BIT_PAGE + BUS_DUMMY_4BIT_SIZE;
  public static final int BUS_MODIFIED_256_SIZE = 2 * 1024 * 1024;  //2MB。上位バイト(偶数アドレス)は0
  //1024ドット65536色(改造)
  public static final int BUS_MODIFIED_65536_PAGE = BUS_MODIFIED_256_PAGE + BUS_MODIFIED_256_SIZE;
  public static final int BUS_MODIFIED_65536_SIZE = 2 * 1024 * 1024;  //2MB
  //合計
  public static final int BUS_ARRAY_SIZE = BUS_MODIFIED_65536_PAGE + BUS_MODIFIED_65536_SIZE;

  //ページ
  public static final int BUS_PAGE_BITS = 12;  //ページのビット数。a>>>BUS_PAGE_BITS
  public static final int BUS_PAGE_SIZE = 1 << BUS_PAGE_BITS;  //ページのサイズ
  public static final int BUS_PAGE_COUNT = 1 << (32 - BUS_PAGE_BITS);  //ページの数

  //ss
  public static final int MPU_SS_BYTE = 0;
  public static final int MPU_SS_WORD = 1;
  public static final int MPU_SS_LONG = 2;

  //wr
  public static final int MPU_WR_WRITE = 0;
  public static final int MPU_WR_READ = 1;

  //us
  public static final int MPU_US_USER = 0;
  public static final int MPU_US_SUPERVISOR = 1;

  //メモリマップ
  public static final MemoryMappedDevice[] busUserMap = new MemoryMappedDevice[BUS_PAGE_COUNT];  //ユーザモード用のメモリマップ
  public static final MemoryMappedDevice[] busSuperMap = new MemoryMappedDevice[BUS_PAGE_COUNT];  //スーパーバイザモード用のメモリマップ
  public static MemoryMappedDevice[] busMemoryMap;  //現在のメモリマップ。srS==0?um:sm。DataBreakPoint.DBP_ON==trueのときは使わない

  //X68030のハイメモリ
  public static final int BUS_HIGH_MEMORY_START = 0x01000000;  //X68030のハイメモリの開始アドレス
  public static int busHighMemorySize;  //X68030のハイメモリのサイズ
  public static byte[] busHighMemoryArray;  //X68030のハイメモリの配列
  public static boolean busHighMemorySaveOn;  //true=X68030のハイメモリの内容を保存する
  public static String busHighMemoryData;  //X68030のハイメモリの内容(gzip+base64)

  //060turboのローカルメモリ
  public static final int BUS_LOCAL_MEMORY_START = 0x10000000;  //060turboのローカルメモリの開始アドレス
  public static int busLocalMemorySize;  //060turboのローカルメモリのサイズ
  public static byte[] busLocalMemoryArray;  //060turboのローカルメモリの配列
  public static boolean busLocalMemorySaveOn;  //true=060turboのローカルメモリの内容を保存する
  public static String busLocalMemoryData;  //060turboのローカルメモリの内容(gzip+base64)

  //拡張メモリ
  public static final byte[] BUS_DUMMY_MEMORY_ARRAY = new byte[0];  //X68030と060turbo以外の拡張メモリの配列
  public static int busRequestExMemoryStart;  //次回起動時の拡張メモリの開始アドレス
  public static int busRequestExMemorySize;  //次回起動時の拡張メモリの長さ
  public static byte[] busRequestExMemoryArray;  //次回起動時の拡張メモリの配列。BUS_DUMMY_MEMORY_ARRAY,busHighMemoryArray,busLocalMemoryArrayのいずれか
  public static int busExMemoryStart;  //拡張メモリの開始アドレス
  public static int busExMemorySize;  //拡張メモリの長さ
  public static byte[] busExMemoryArray;  //拡張メモリの配列。BUS_DUMMY_MEMORY_ARRAY,busHighMemoryArray,busLocalMemoryArrayのいずれか

  //FC2ピンをカットする
  public static boolean busRequestCutFC2Pin;
  public static boolean busCutFC2Pin;

  //busInit ()
  //  バスコントローラを初期化する
  public static void busInit () {
    //um = new MMD[BUS_PAGE_COUNT];
    //sm = new MMD[BUS_PAGE_COUNT];
    if (!DataBreakPoint.DBP_ON) {
      busMemoryMap = busSuperMap;
    }

    //X68030のハイメモリ
    //busHighMemorySize = 16 << 20;
    busHighMemoryArray = new byte[0];
    //busHighMemorySaveOn = false;
    //busHighMemoryData = "";
    if (busHighMemoryData != null && busHighMemoryData.length () != 0) {
      if (busHighMemoryArray.length != busHighMemorySize) {
        busHighMemoryArray = new byte[busHighMemorySize];
      }
      if ("zero".equalsIgnoreCase (busHighMemoryData)) {
        //ゼロクリアする
        Arrays.fill (busHighMemoryArray, (byte) 0);
      } else {
        //gzip+base64を解凍する
        byte[] bb = ByteArray.byaDecodeGzip (ByteArray.byaDecodeBase64 (busHighMemoryData));
        int copySize = Math.min (bb.length, busHighMemoryArray.length);
        if (copySize > 0) {
          System.arraycopy (bb, 0, busHighMemoryArray, 0, copySize);
        }
      }
    }

    //060turboのローカルメモリ
    //busLocalMemorySize = 128 << 20;
    busLocalMemoryArray = new byte[0];
    //busLocalMemorySaveOn = false;
    //busLocalMemoryData = "";
    if (busLocalMemoryData != null && busLocalMemoryData.length () != 0) {
      if (busLocalMemoryArray.length != busLocalMemorySize) {
        busLocalMemoryArray = new byte[busLocalMemorySize];
      }
      if ("zero".equalsIgnoreCase (busLocalMemoryData)) {
        //ゼロクリアする
        Arrays.fill (busLocalMemoryArray, (byte) 0);
      } else {
        //gzip+base64を解凍する
        byte[] bb = ByteArray.byaDecodeGzip (ByteArray.byaDecodeBase64 (busLocalMemoryData));
        int copySize = Math.min (bb.length, busLocalMemoryArray.length);
        if (copySize > 0) {
          System.arraycopy (bb, 0, busLocalMemoryArray, 0, copySize);
        }
      }
    }

    //現在の拡張メモリ
    busExMemoryStart = busRequestExMemoryStart = 0x10000000;
    busExMemorySize = busRequestExMemorySize = 0 << 20;
    busExMemoryArray = busRequestExMemoryArray = BUS_DUMMY_MEMORY_ARRAY;

    //FC2ピンをカットする
    //busRequestCutFC2Pin = false;
    busCutFC2Pin = !busRequestCutFC2Pin;

    busUpdateMemoryMap ();

  }  //busInit()

  public static void busUpdateMemoryMap () {
    if (busExMemoryStart == busRequestExMemoryStart &&
        busExMemorySize == busRequestExMemorySize &&
        busExMemoryArray == busRequestExMemoryArray &&
        busExMemoryArray.length == busExMemorySize &&
        busCutFC2Pin == busRequestCutFC2Pin) {
      return;
    }
    //拡張メモリ
    busExMemoryStart = busRequestExMemoryStart;
    busExMemorySize = busRequestExMemorySize;
    busExMemoryArray = busRequestExMemoryArray;
    if (busExMemoryArray.length != busExMemorySize) {
      byte[] newArray = new byte[busExMemorySize];
      int copySize = Math.min (busExMemoryArray.length, busExMemorySize);
      if (copySize > 0) {
        System.arraycopy (busExMemoryArray, 0, newArray, 0, copySize);
      }
      if (busExMemoryArray == busHighMemoryArray) {
        busHighMemoryArray = newArray;
      } else if (busExMemoryArray == busLocalMemoryArray) {
        busLocalMemoryArray = newArray;
      }
      busExMemoryArray = newArray;
    }
    //FC2ピンをカットする
    busCutFC2Pin = busRequestCutFC2Pin;
    //メモリマップを作る
    //  すべてのページにデバイスを割り当てること
    busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, 0x00002000);  //MMR メインメモリ
    busUser ( MemoryMappedDevice.MMD_MMR, 0x00002000, 0x00c00000);  //MMR メインメモリ
    //  0x00000000  メインメモリ
    busSuper (MemoryMappedDevice.MMD_GE0, 0x00c00000, 0x00c80000);  //GE0 グラフィックス画面(512ドット16色ページ0)
    busSuper (MemoryMappedDevice.MMD_GE1, 0x00c80000, 0x00d00000);  //GE1 グラフィックス画面(512ドット16色ページ1)
    busSuper (MemoryMappedDevice.MMD_GE2, 0x00d00000, 0x00d80000);  //GE2 グラフィックス画面(512ドット16色ページ2)
    busSuper (MemoryMappedDevice.MMD_GE3, 0x00d80000, 0x00e00000);  //GE3 グラフィックス画面(512ドット16色ページ3)
    //  0x00c00000  グラフィックスVRAM
    busSuper (MemoryMappedDevice.MMD_TXT, 0x00e00000, 0x00e80000);  //TXT テキスト画面
    //  0x00e00000  テキストVRAM
    busSuper (MemoryMappedDevice.MMD_CRT, 0x00e80000, 0x00e82000);  //CRT CRTコントローラ
    //  0x00e80000  CRTコントローラ
    busSuper (MemoryMappedDevice.MMD_VCN, 0x00e82000, 0x00e84000);  //VCN ビデオコントローラ
    //  0x00e82000  パレットレジスタ
    //  0x00e82400  ビデオコントローラ
    busSuper (MemoryMappedDevice.MMD_DMA, 0x00e84000, 0x00e86000);  //DMA DMAコントローラ
    //  0x00e84000  DMAコントローラ
    busSuper (MemoryMappedDevice.MMD_SVS, 0x00e86000, 0x00e88000);  //SVS スーパーバイザ領域設定
    //  0x00e86000  スーパーバイザ領域設定
    busSuper (MemoryMappedDevice.MMD_MFP, 0x00e88000, 0x00e8a000);  //MFP MFP
    //  0x00e88000  MFP
    busSuper (MemoryMappedDevice.MMD_RTC_FIRST, 0x00e8a000, 0x00e8c000);  //RTC RTC
    //  0x00e8a000  RTC
    busSuper (MemoryMappedDevice.MMD_PRN, 0x00e8c000, 0x00e8e000);  //PRN プリンタポート
    //  0x00e8c000  プリンタポート
    busSuper (MemoryMappedDevice.MMD_SYS, 0x00e8e000, 0x00e90000);  //SYS システムポート
    //  0x00e8e000  システムポート
    busSuper (MemoryMappedDevice.MMD_OPM, 0x00e90000, 0x00e92000);  //OPM FM音源
    //  0x00e90000  FM音源
    busSuper (MemoryMappedDevice.MMD_PCM, 0x00e92000, 0x00e94000);  //PCM ADPCM音源
    //  0x00e92000  ADPCM音源
    busSuper (MemoryMappedDevice.MMD_FDC, 0x00e94000, 0x00e96000);  //FDC FDコントローラ
    //  0x00e94000  FDC
    busSuper (MemoryMappedDevice.MMD_HDC, 0x00e96000, 0x00e98000);  //HDC SASI HDコントローラ
    //  0x00e96000  HDC SASI HDコントローラ
    //  0x00e96020  SPC 内蔵SCSIプロトコルコントローラ
    busSuper (MemoryMappedDevice.MMD_SCC, 0x00e98000, 0x00e9a000);  //SCC SCC
    //  0x00e98000  SCC
    busSuper (MemoryMappedDevice.MMD_PPI, 0x00e9a000, 0x00e9c000);  //PPI PPI
    //  0x00e9a000  PPI
    busSuper (MemoryMappedDevice.MMD_IOI, 0x00e9c000, 0x00e9e000);  //IOI I/O割り込み
    //  0x00e9c000  I/O割り込み
    busSuper (MemoryMappedDevice.MMD_XB1, 0x00e9e000, 0x00ea0000);  //XB1 拡張ボード領域1
    //  0x00e9e000  数値演算プロセッサボード(CZ-6BP1/CZ-6BP1A)
    //  0x00e9e200  ツクモグラフィックアクセラレータPCMボード(TS-6BGA)
    //  0x00e9f000  WINDRV
    //  0x00e9f000  040Excel
    //  0x00e9f020  HFS ホストファイルシステムインタフェイス
    busSuper (MemoryMappedDevice.MMD_NUL, 0x00ea0000, 0x00eae000);  //MemoryMappedDevice.MMD_EXSはSPC.spcReset()で必要なときだけ接続する
    //  0x00ea0000  拡張SCSI(SCSIボードCZ-6BS1/Mach-2)
    //  0x00ea1ff0  TS-6BS1
    busSuper (MemoryMappedDevice.MMD_XB2, 0x00eae000, 0x00eb0000);  //拡張ボード領域2
    //  0x00eaf900  FAXボード(CZ-6BC1)
    //  0x00eafa00  MIDIボード(CZ-6BM1)
    //  0x00eafb00  パラレルボード(CZ-6BN1)
    //  0x00eafc00  RS-232Cボード(CZ-6BF1)
    //  0x00eafd00  ユニバーサルI/Oボード(CZ-6BU1)
    //  0x00eafe00  GP-IBボード(CZ-6BG1)
    //  0x00eaff00  スーパーバイザエリア設定
    busSuper (MemoryMappedDevice.MMD_SPR, 0x00eb0000, 0x00ec0000);  //SPR スプライト画面
    //  0x00eb0000  スプライトレジスタ
    //  0x00eb0800  スプライトコントローラ
    //  0x00eb8000  スプライトPCGエリア
    //  0x00ebc000  スプライトテキストエリア0
    //  0x00ebe000  スプライトテキストエリア1
    busUser ( MemoryMappedDevice.MMD_NUL, 0x00ec0000, 0x00ed0000);
    //  0x00ec0000  ユーザI/Oエリア
    //  0x00ec0000  Awesome
    //  0x00ec0000  Xellent30
    //  0x00ecc000  Mercury
    //  0x00ece000  Neptune
    //  0x00ecf000  Venus-X/030
    busSuper (MemoryMappedDevice.MMD_SMR, 0x00ed0000, 0x00ed4000);  //SMR SRAM
    //  0x00ed0000  SRAM
    busSuper (MemoryMappedDevice.MMD_NUL, 0x00ed4000, 0x00f00000);
    //  0x00ee0000  GAフレームバッファウインドウ(サブ)
    //  0x00ef0000  GAフレームバッファウインドウ(メイン)
    //  0x00efff00  PSX16550
    busSuper (MemoryMappedDevice.MMD_CG1, 0x00f00000, 0x00f40000);  //CG1 CGROM1
    //  0x00f00000  KNJ16x16フォント(1～8区,非漢字752文字)
    //  0x00f05e00  KNJ16x16フォント(16～47区,第1水準漢字3008文字)
    //  0x00f1d600  KNJ16x16フォント(48～84区,第2水準漢字3478文字)
    //  0x00f3a000  ANK8x8フォント(256文字)
    //  0x00f3a800  ANK8x16フォント(256文字)
    //  0x00f3b800  ANK12x12フォント(256文字)
    //  0x00f3d000  ANK12x24フォント(256文字)
    busSuper (MemoryMappedDevice.MMD_CG2, 0x00f40000, 0x00fc0000);  //CG2 CGROM2
    //  0x00f40000  KNJ24x24フォント(1～8区,非漢字752文字)
    //  0x00f4d380  KNJ24x24フォント(16～47区,第1水準漢字3008文字)
    //  0x00f82180  KNJ24x24フォント(48～84区,第2水準漢字3478文字)
    //  0x00fbf400  [13]ANK6x12フォント(256文字)
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00fc0000, 0x01000000);  //ROM ROM
    //  0x00fc0000  [11,12]内蔵SCSI BIOS,[13]内蔵SCSIハンドル
    //  0x00fc0200  [13]ROM Human
    //  0x00fce000  [13]ROM Float
    //  0x00fd3800  [13]ROMデバッガ
    //  0x00fe0000  [10,11,12]ROMデバッガ
    //  0x00fe5000  [10,11,12]ROM Human
    //  0x00ff0000  IPLROM
    //  0x00ffd018  [10]ANK6x12フォント(254文字)
    //  0x00ffd344  [11]ANK6x12フォント(254文字)
    //  0x00ffd45e  [12]ANK6x12フォント(254文字)
    //  0x00ffdc00  [10]ROMディスク
  }  //busUpdateMemoryMap()

  public static void busReset () {
    if (regSRS != 0) {  //スーパーバイザモード
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;
      } else {
        busMemoryMap = busSuperMap;
      }
    } else {  //ユーザモード
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpUserMap;
      } else {
        busMemoryMap = busUserMap;
      }
    }
    //スーパーバイザ領域設定
    busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, 0x00002000);
    busUser ( MemoryMappedDevice.MMD_MMR, 0x00002000, MainMemory.mmrMemorySizeCurrent);
    if (MainMemory.mmrMemorySizeCurrent < 0x00c00000) {
      busSuper (MemoryMappedDevice.MMD_NUL, MainMemory.mmrMemorySizeCurrent, 0x00c00000);
    }
  }  //busReset()

  //busUser (mmd, motherStartAddress, motherEndAddress)
  //  ユーザ領域にデバイスを割り当てる
  //  motherStartAddress  マザーボード開始アドレス。BUS_PAGE_SIZEで割り切れること
  //  motherEndAddress    マザーボード終了アドレス。BUS_PAGE_SIZEで割り切れること
  public static void busUser (MemoryMappedDevice mmd, int motherStartAddress, int motherEndAddress) {
    int motherStartPage = motherStartAddress >>> BUS_PAGE_BITS;  //マザーボード開始ページ
    int motherEndPage = motherEndAddress >>> BUS_PAGE_BITS;  //マザーボード終了ページ
    if (false &&
        (motherStartPage << BUS_PAGE_BITS != motherStartAddress ||
         motherEndPage << BUS_PAGE_BITS != motherEndAddress)) {  //開始アドレスまたは終了アドレスがページサイズで割り切れない
      System.out.printf ("ERROR: busUser (\"%s\", 0x%08x, 0x%08x)\n", mmd.toString (), motherStartAddress, motherEndAddress);
    }
    int exMemoryStartPage = busExMemoryStart >>> BUS_PAGE_BITS;  //拡張メモリ開始ページ
    int exMemoryEndPage = exMemoryStartPage + (busExMemorySize >>> BUS_PAGE_BITS);  //拡張メモリ終了ページ
    for (int block = 0; block < 1 << 32 - BUS_MOTHER_BITS; block++) {  //ブロック
      int blockStartPage = block << BUS_MOTHER_BITS - BUS_PAGE_BITS;  //ブロック開始ページ
      int startPage = blockStartPage + motherStartPage;  //デバイス開始ページ
      int endPage = blockStartPage + motherEndPage;  //デバイス終了ページ
      for (int page = startPage; page < endPage; page++) {  //デバイスページ
        MemoryMappedDevice superMmd = exMemoryStartPage <= page && page < exMemoryEndPage ? MemoryMappedDevice.MMD_XMM : mmd;
        busUserMap[page] = busSuperMap[page] = superMmd;
        if (InstructionBreakPoint.IBP_ON) {
          if (InstructionBreakPoint.ibpUserMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpUserMap[page] = superMmd;
          }
          if (InstructionBreakPoint.ibpSuperMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpSuperMap[page] = superMmd;
          }
        }
        if (DataBreakPoint.DBP_ON) {
          if (DataBreakPoint.dbpUserMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpUserMap[page] = superMmd;
          }
          if (DataBreakPoint.dbpSuperMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpSuperMap[page] = superMmd;
          }
        }
      }
    }
  }  //busUser(MMD,int,int)

  //busSuper (mmd, motherStartAddress, motherEndAddress)
  //  スーパーバイザ領域にデバイスを割り当てる
  //  motherStartAddress  マザーボード開始アドレス。BUS_PAGE_SIZEで割り切れること
  //  motherEndAddress    マザーボード終了アドレス。BUS_PAGE_SIZEで割り切れること
  public static void busSuper (MemoryMappedDevice mmd, int motherStartAddress, int motherEndAddress) {
    int motherStartPage = motherStartAddress >>> BUS_PAGE_BITS;  //マザーボード開始ページ
    int motherEndPage = motherEndAddress >>> BUS_PAGE_BITS;  //マザーボード終了ページ
    if (false &&
        (motherStartPage << BUS_PAGE_BITS != motherStartAddress ||
         motherEndPage << BUS_PAGE_BITS != motherEndAddress)) {  //開始アドレスまたは終了アドレスがページサイズで割り切れない
      System.out.printf ("ERROR: busSuper (\"%s\", 0x%08x, 0x%08x)\n", mmd.toString (), motherStartAddress, motherEndAddress);
    }
    int exMemoryStartPage = busExMemoryStart >>> BUS_PAGE_BITS;  //拡張メモリ開始ページ
    int exMemoryEndPage = exMemoryStartPage + (busExMemorySize >>> BUS_PAGE_BITS);  //拡張メモリ終了ページ
    for (int block = 0; block < 1 << 32 - BUS_MOTHER_BITS; block++) {  //ブロック
      int blockStartPage = block << BUS_MOTHER_BITS - BUS_PAGE_BITS;  //ブロック開始ページ
      int startPage = blockStartPage + motherStartPage;  //デバイス開始ページ
      int endPage = blockStartPage + motherEndPage;  //デバイス終了ページ
      for (int page = startPage; page < endPage; page++) {  //デバイスページ
        boolean isExMemory = exMemoryStartPage <= page && page < exMemoryEndPage;
        MemoryMappedDevice userMmd = isExMemory ? MemoryMappedDevice.MMD_XMM : busCutFC2Pin ? mmd : MemoryMappedDevice.MMD_NUL;
        MemoryMappedDevice superMmd = isExMemory ? MemoryMappedDevice.MMD_XMM : mmd;
        busUserMap[page] = userMmd;
        busSuperMap[page] = superMmd;
        if (InstructionBreakPoint.IBP_ON) {
          if (InstructionBreakPoint.ibpUserMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpUserMap[page] = userMmd;
          }
          if (InstructionBreakPoint.ibpSuperMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpSuperMap[page] = superMmd;
          }
        }
        if (DataBreakPoint.DBP_ON) {
          if (DataBreakPoint.dbpUserMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpUserMap[page] = userMmd;
          }
          if (DataBreakPoint.dbpSuperMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpSuperMap[page] = superMmd;
          }
        }
      }
    }
  }  //busSuper(MMD,int,int)

  //d = busPbs (a)
  //  ピークバイト符号拡張
  public static byte busPbs (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a);
  }  //busPbs(int)

  //d = busPbz (a)
  //  ピークバイトゼロ拡張
  public static int busPbz (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a);
  }  //busPbz(int)

  //d = busPws (a)
  //  ピークワード符号拡張
  public static int busPws (int a) {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a);
    } else {  //奇数
      int a1 = a + 1;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a) << 8 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPbz (a1);
    }
  }  //busPws(int)

  //d = busPwse (a)
  //  ピークワード符号拡張(偶数)
  public static int busPwse (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a);
  }  //busPwse(int)

  //d = busPwz (a)
  //  ピークワードゼロ拡張
  public static int busPwz (int a) {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a);
    } else {  //奇数
      int a1 = a + 1;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a) << 8 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPbz (a1);
    }
  }  //busPwz(int)

  //d = busPwze (a)
  //  ピークワードゼロ拡張(偶数)
  public static int busPwze (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a);
  }  //busPwze(int)

  //d = busPls (a)
  //  ピークロング符号拡張
  public static int busPls (int a) {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPls (a);
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a) << 16 | busSuperMap[a2 >>> BUS_PAGE_BITS].mmdPwz (a2);
    } else {  //奇数
      int a1 = a + 1;
      int a3 = a + 3;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a) << 24 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPwz (a1) << 8 | busSuperMap[a3 >>> BUS_PAGE_BITS].mmdPbz (a3);
    }
  }  //busPls(int)

  //d = busPlsf (a)
  //  ピークロング符号拡張(4の倍数)
  public static int busPlsf (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPls (a);
  }  //busPlsf(int)

  //d = busPqs (a)
  //  ピーククワッド符号拡張
  public static long busPqs (int a) {
    return (long) busPls (a) << 32 | busPls (a + 4) & 0xffffffffL;
  }  //busPqs(int)

  //d = busRbs (a)
  //  リードバイト符号拡張
  public static byte busRbs (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a);
    }
  }  //busRbs(int)

  //d = busRbz (a)
  //  リードバイトゼロ拡張
  public static int busRbz (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a);
    }
  }  //busRbz(int)

  //d = busRws (a)
  //  リードワード符号拡張
  public static int busRws (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 8 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 8 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busRws(int)

  //d = busRwse (a)
  //  リードワード符号拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRwse (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
    }
  }  //busRwse(int)

  //d = busRwz (a)
  //  リードワードゼロ拡張
  public static int busRwz (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a) << 8 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a) << 8 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busRwz(int)

  //d = busRwze (a)
  //  リードワードゼロ拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRwze (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
    }
  }  //busRwze(int)

  //d = busRls (a)
  //  リードロング符号拡張
  public static int busRls (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      }
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        } else {
          return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        return mmd == mmd2 ? mmd.mmdRls (a) : mmd.mmdRws (a) << 16 | mmd2.mmdRwz (a2);  //デバイスを跨がない/デバイスを跨ぐ
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      int a3 = a + 3;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 24 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRwz (a1) << 8 | DataBreakPoint.dbpMemoryMap[a3 >>> BUS_PAGE_BITS].mmdRbz (a3);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 24 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRwz (a1) << 8 | busMemoryMap[a3 >>> BUS_PAGE_BITS].mmdRbz (a3);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_LONG;
      throw M68kException.m6eSignal;
    }
  }  //busRls(int)

  //d = busRlse (a)
  //  リードロング符号拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRlse (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      }
    } else {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        } else {
          return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        return mmd == mmd2 ? mmd.mmdRls (a) : mmd.mmdRws (a) << 16 | mmd2.mmdRwz (a2);  //デバイスを跨がない/デバイスを跨ぐ
      }
    }
  }  //busRlse(int)

  //d = busRlsf (a)
  //  リードロング符号拡張(4の倍数限定)
  //  例外ベクタテーブルなど、アドレスが4の倍数であることが分かっている場合
  public static int busRlsf (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
    }
  }  //busRlsf(int)

  //d = busRqs (a)
  //  リードクワッド符号拡張
  public static long busRqs (int a) throws M68kException {
    return (long) busRls (a) << 32 | busRls (a + 4) & 0xffffffffL;
  }  //busRqs(int)

  //busWb (a, d)
  //  ライトバイト
  public static void busWb (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
    }
  }  //busWb(int,int)

  //busWw (a, d)
  //  ライトワード
  public static void busWw (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 8);
        DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWb (a1, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 8);
        busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWb (a1, d);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_WRITE;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busWw(int,int)

  //busWwe (a, d)
  //  ライトワード(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static void busWwe (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
    }
  }  //busWwe(int,int)

  //busWl (a, d)
  //  ライトロング
  public static void busWl (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      }
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //デバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //デバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      int a3 = a + 3;
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 24);
        DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWw (a1, d >> 8);
        DataBreakPoint.dbpMemoryMap[a3 >>> BUS_PAGE_BITS].mmdWb (a3, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 24);
        busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWw (a1, d >> 8);
        busMemoryMap[a3 >>> BUS_PAGE_BITS].mmdWb (a3, d);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_WRITE;
      M68kException.m6eSize = MPU_SS_LONG;
      throw M68kException.m6eSignal;
    }
  }  //busWl(int,int)

  //busWlf (a, d)
  //  ライトロング(4の倍数限定)
  //  例外ベクタテーブルなど、アドレスが4の倍数であることが分かっている場合
  public static void busWlf (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
    }
  }  //busWlf(int,int)

  //busWle (a, d)
  //  ライトロング(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static void busWle (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      }
    } else {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //デバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //デバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    }
  }  //busWle(int,int)

  //busWq (a, d)
  //  ライトクワッド
  public static void busWq (int a, long d) throws M68kException {
    busWl (a, (int) (d >>> 32));
    busWl (a + 4, (int) d);
  }  //busWq(int,long)

  //以下は拡張

  //busRbb (a, bb, o, l)
  //  リードバイトバッファ
  public static void busRbb (int a, byte[] bb, int o, int l) throws M68kException {
    if (false) {
      for (int i = 0; i < l; i++) {
        int ai = a + i;
        if (DataBreakPoint.DBP_ON) {
          bb[o + i] = DataBreakPoint.dbpMemoryMap[ai >>> BUS_PAGE_BITS].mmdRbs (ai);
        } else {
          bb[o + i] = busMemoryMap[ai >>> BUS_PAGE_BITS].mmdRbs (ai);
        }
      }
    } else {
      int r = (~a & BUS_PAGE_SIZE - 1) + 1;  //最初のページの残りの長さ。1～BUS_PAGE_SIZE
      while (l > 0) {
        MemoryMappedDevice mmd;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        }
        int s = l <= r ? l : r;  //このページで転送する長さ
        l -= s;
        if (true) {
          for (s -= 16; s >= 0; s -= 16) {
            bb[o     ] = mmd.mmdRbs (a     );
            bb[o +  1] = mmd.mmdRbs (a +  1);
            bb[o +  2] = mmd.mmdRbs (a +  2);
            bb[o +  3] = mmd.mmdRbs (a +  3);
            bb[o +  4] = mmd.mmdRbs (a +  4);
            bb[o +  5] = mmd.mmdRbs (a +  5);
            bb[o +  6] = mmd.mmdRbs (a +  6);
            bb[o +  7] = mmd.mmdRbs (a +  7);
            bb[o +  8] = mmd.mmdRbs (a +  8);
            bb[o +  9] = mmd.mmdRbs (a +  9);
            bb[o + 10] = mmd.mmdRbs (a + 10);
            bb[o + 11] = mmd.mmdRbs (a + 11);
            bb[o + 12] = mmd.mmdRbs (a + 12);
            bb[o + 13] = mmd.mmdRbs (a + 13);
            bb[o + 14] = mmd.mmdRbs (a + 14);
            bb[o + 15] = mmd.mmdRbs (a + 15);
            a += 16;
            o += 16;
          }
          s += 16;
        }
        for (int i = 0; i < s; i++) {
          bb[o + i] = mmd.mmdRbs (a + i);
        }
        a += s;
        o += s;
        r = BUS_PAGE_SIZE;
      }
    }
  }  //busRbb(int,byte[],int,int)

  //busWbb (a, bb, o, l)
  //  ライトバイトバッファ
  public static void busWbb (int a, byte[] bb, int o, int l) throws M68kException {
    if (false) {
      for (int i = 0; i < l; i++) {
        int ai = a + i;
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[ai >>> BUS_PAGE_BITS].mmdWb (ai, bb[o + i]);
        } else {
          busMemoryMap[ai >>> BUS_PAGE_BITS].mmdWb (ai, bb[o + i]);
        }
      }
    } else {
      int r = (~a & BUS_PAGE_SIZE - 1) + 1;  //最初のページの残りの長さ。1～BUS_PAGE_SIZE
      while (l > 0) {
        MemoryMappedDevice mmd;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        }
        int s = l <= r ? l : r;  //このページで転送する長さ
        l -= s;
        if (true) {
          for (s -= 16; s >= 0; s -= 16) {
            mmd.mmdWb (a     , bb[o     ]);
            mmd.mmdWb (a +  1, bb[o +  1]);
            mmd.mmdWb (a +  2, bb[o +  2]);
            mmd.mmdWb (a +  3, bb[o +  3]);
            mmd.mmdWb (a +  4, bb[o +  4]);
            mmd.mmdWb (a +  5, bb[o +  5]);
            mmd.mmdWb (a +  6, bb[o +  6]);
            mmd.mmdWb (a +  7, bb[o +  7]);
            mmd.mmdWb (a +  8, bb[o +  8]);
            mmd.mmdWb (a +  9, bb[o +  9]);
            mmd.mmdWb (a + 10, bb[o + 10]);
            mmd.mmdWb (a + 11, bb[o + 11]);
            mmd.mmdWb (a + 12, bb[o + 12]);
            mmd.mmdWb (a + 13, bb[o + 13]);
            mmd.mmdWb (a + 14, bb[o + 14]);
            mmd.mmdWb (a + 15, bb[o + 15]);
            a += 16;
            o += 16;
          }
          s += 16;
        }
        for (int i = 0; i < s; i++) {
          mmd.mmdWb (a + i, bb[o + i]);
        }
        a += s;
        o += s;
        r = BUS_PAGE_SIZE;
      }
    }
  }  //busWbb(int,byte[],int,int)

  //busVb (a, d)
  //  ライトバイト(エラーなし)
  public static void busVb (int a, int d) {
    try {
      if (DataBreakPoint.DBP_ON) {
        (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWb (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
      }
    } catch (M68kException e) {
    }
  }  //busVb(int,int)

  //busVw (a, d)
  //  ライトワード(エラーなし)
  public static void busVw (int a, int d) {
    try {
      if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
        if (DataBreakPoint.DBP_ON) {
          (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWw (a, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
        }
      }
    } catch (M68kException e) {
    }
  }  //busVw(int,int)

  //busVl (a, d)
  //  ライトロング(エラーなし)
  public static void busVl (int a, int d) {
    try {
      if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
        if (DataBreakPoint.DBP_ON) {
          (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWl (a, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
        }
      } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
        int a2 = a + 2;
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS];
          mmd2 = (regSRS != 0 ? busSuperMap : busUserMap)[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //4の倍数ではない偶数でデバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //4の倍数ではない偶数でデバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    } catch (M68kException e) {
    }
  }  //busVl(int,int)



  //========================================================================================
  //$$GRA グラフィックス画面
  //  512ドットのときハードウェアスクロールが常に4ページあるのでメモリモードに関わらず4ビットゼロ拡張で格納する
  //  偶数アドレスの8ビット全部と奇数アドレスの上位4ビットは常に0
  //
  //  512ドット16色
  //                アドレス             アクセス                  格納
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  ............3210
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  ............7654
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  ............ba98
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  ............fedc
  //
  //  512ドット256色
  //                アドレス             アクセス                  格納
  //    GF0  0x00c00000～0x00c7ffff  ........76543210  ─┬  ............3210
  //    GF1  0x00c80000～0x00cfffff  ........fedcba98  ┐└  ............7654
  //         0x00d00000～0x00d7ffff                    ├─  ............ba98
  //         0x00d80000～0x00dfffff                    └─  ............fedc
  //
  //  512ドット65536色
  //                アドレス             アクセス                  格納
  //    GG0  0x00c00000～0x00c7ffff  fedcba9876543210  ─┬  ............3210
  //         0x00c80000～0x00cfffff                      ├  ............7654
  //         0x00d00000～0x00d7ffff                      ├  ............ba98
  //         0x00d80000～0x00dfffff                      └  ............fedc
  //
  //  1024ドット16色
  //                アドレス             アクセス                  格納
  //    GH0  0x00c00000～0x00dfffff  ............3210  ──  ............3210
  //



  //========================================================================================
  //$$TXT テキスト画面

  //txtInit ()
  //  テキスト画面の初期化
  public static void txtInit () {
  }  //txtInit()



  //========================================================================================
  //$$SVS スーパーバイザ領域設定
  public static final int SVS_AREASET = 0x00e86001;  //0x00 0x00000000～0x00002000
  //                                                      0x01 0x00000000～0x00004000
  //                                                      0x02 0x00000000～0x00008000
  //                                                      0x04 0x00000000～0x00010000
  //                                                      0x08 0x00000000～0x00020000
  //                                                      0x10 0x00000000～0x00040000
  //                                                      0x20 0x00000000～0x00080000
  //                                                      0x40 0x00000000～0x00100000
  //                                                      0x80 0x00000000～0x00200000



  //========================================================================================
  //$$SYS システムポート
  //
  //     アドレス   bit  RW  名前   X68030
  //    0x00e8e001  0-3  RW           13    コントラスト(0=最も暗い,15=最も明るい)
  //    0x00e8e003   3   R             1    TV ON/OFFステータス(0=ON,1=OFF)
  //                      W                 TVリモコン信号
  //                 2   R   FIELD     0
  //                 1   RW  3D-L      0    (3Dスコープ)シャッター左(0=OPEN,1=CLOSE)
  //                 0   RW  3D-R      0    (3Dスコープ)シャッター右(0=OPEN,1=CLOSE)
  //    0x00e8e005  4-0   W                 (カラーイメージユニット(デジタイズテロッパ))画像入力コントロール
  //                                          bit4  IMAGE IN bit17
  //                                          bit3  IMAGE IN bit18
  //                                          bit2  IMAGE IN bit19
  //                                          bit1  IMAGE IN bit20
  //                                          bit0  IMAGE IN bit21
  //    0x00e8e007   3   R             1    キージャックステータス(0=抜かれている,1=差し込まれている)
  //                      W                 キーレディ(0=キーデータ送信禁止,1=キーデータ送信許可)
  //                 2    W            1    1=NMIリセット
  //                 1   RW            0    HRL。詳細はCRTCを参照
  //                 0   RW            0    (現在は使用されていない)解像度LED(0=消灯,1=点灯)
  //    0x00e8e009  7-4   W                 (X68030のみ)ROMアクセスウェイト
  //                3-0   W                 (X68030のみ)RAMアクセスウェイト(0=25MHz,4=16MHz相当,10=10MHz相当)
  //    0x00e8e00b  7-4  R            13    機種(13=MC68030,15=MC68000)
  //                3-0  R            12    動作周波数(12=25MHz,14=16MHz,15=10MHz)
  //    0x00e8e00d  7-0   W                 SRAM WRITE ENABLE(49=SRAM書き込み可)
  //    0x00e8e00f  3-0   W                 フロント電源スイッチがOFFになっているとき0→15→15で電源OFF
  //
  //    未定義のbitはリードすると1、ライトしてもバスエラーは発生しない
  //    アドレスのbit12-4はデコードされない。0x00e8e000～0x00e8ffffの範囲に16バイトのポートが512回繰り返し現れる
  //

  public static boolean sysNMIFlag;  //true=INTERRUPTスイッチが押された

  //sysInit ()
  //  初期化
  public static void sysInit () {
    sysNMIFlag = false;
  }  //sysInit()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int sysAcknowledge () {
    return M68kException.M6E_LEVEL_7_INTERRUPT_AUTOVECTOR;
  }  //sysAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void sysDone () {
    if (sysNMIFlag) {  //NMIリセットされていない
      mpuIRR |= MPU_SYS_INTERRUPT_MASK;
    }
  }  //sysDone()

  //sysInterrupt ()
  //  INTERRUPTスイッチが押された
  public static void sysInterrupt () {
    sysNMIFlag = true;
    mpuIRR |= MPU_SYS_INTERRUPT_MASK;
  }  //sysInterrupt()

  //sysResetNMI ()
  //  NMIリセット
  public static void sysResetNMI () {
    sysNMIFlag = false;
  }  //sysResetNMI()



  //========================================================================================
  //$$PPI PPI
  //  ジョイスティック
  //    JOYKEYの入力を反映
  //  ADPCMの設定

  //ポート
  public static final int PPI_PORT_A  = 0x00e9a001;   //PPIポートA 入力 負論理 ジョイスティック1
  //    bit7    1
  //    bit6    0=ボタンBが押されている,1=ボタンBが押されていない
  //    bit5    0=ボタンAが押されている,1=ボタンAが押されていない
  //    bit4    1
  //    bit3    0=レバーが右に入っている
  //    bit2    0=レバーが左に入っている
  //    bit1    0=レバーが下に入っている
  //    bit0    0=レバーが上に入っている
  public static final int PPI_PORT_B  = 0x00e9a003;   //PPIポートB 入力 負論理 ジョイスティック2
  //    bit7    1
  //    bit6    0=ボタンBが押されている,1=ボタンBが押されていない
  //    bit5    0=ボタンAが押されている,1=ボタンAが押されていない
  //    bit4    1
  //    bit3    0=レバーが右に入っている
  //    bit2    0=レバーが左に入っている
  //    bit1    0=レバーが下に入っている
  //    bit0    0=レバーが上に入っている
  public static final int PPI_PORT_C  = 0x00e9a005;   //PPIポートC 出力 ADPCMコントロール
  //    bit7    ジョイスティック2のオプション。0=通常動作,1=オプション動作
  //    bit6    ジョイスティック1のオプション。0=通常動作,1=オプション動作
  //    bit5    ジョイスティック2のストローブ。0=通常動作,1=操作無効
  //    bit4    ジョイスティック1のストローブ。0=通常動作,1=操作無効
  //    bit3-2  ADPCMの分周比(00=1/1024,01=1/768,10=1/512,11=1/512)
  //    bit1    ADPCMの出力left。0=出力する,1=出力しない
  //    bit0    ADPCMの出力right。0=出力する,1=出力しない
  public static final int PPI_CONTROL = 0x00e9a007;   //PPIコントロール 0x92に固定
  //    bit7=1  モードの設定
  //      bit6-5  グループA(ポートAとポートCの上位)のモード(0=モード0,1=モード1,2/3=モード2)
  //      bit4    ポートAの方向(0=出力,1=入力)
  //      bit3    ポートCの上位の方向(0=出力,1=入力)
  //      bit2    グループB(ポートBとポートCの下位)のモード(0=モード0,1=モード1)
  //      bit1    ポートBの方向(0=出力,1=入力)
  //      bit0    ポートCの下位の方向(0=出力,1=入力)
  //    bit7=0  ポートCで出力に設定されているビットの操作
  //      bit3-1  ビット番号
  //      bit0    設定値

  //ボタンのマスク
  //  上と下または左と右のキーが同時に押された場合は両方キャンセルする
  //  上下同時押しのSELECTと左右同時押しのRUNを付ける
  //  bit4とbit7は入力できない
  //    8255の足がプルアップされているので実機ではどうすることもできない(外付けの回路でどうにかなるものではない)
  //    エミュレータで入力できるようにするのは簡単だか対応しているソフトは存在しないだろう
  //! 斜めボタンを付ける
  //! A,Bのキーを複数指定できるようにする
/*
  public static final int[] PPI_BUTTON_MASK = {
    0b00000001,  //ジョイスティック1の上
    0b00000010,  //ジョイスティック1の下
    0b00000100,  //ジョイスティック1の左
    0b00001000,  //ジョイスティック1の右
    0b00010000,  //ジョイスティック1のC
    0b00100000,  //ジョイスティック1のA
    0b01000000,  //ジョイスティック1のB
    0b10000000,  //ジョイスティック1のD
    0b00000011,  //ジョイスティック1のSELECT
    0b00001100,  //ジョイスティック1のRUN
    0b00000001,  //ジョイスティック2の上
    0b00000010,  //ジョイスティック2の下
    0b00000100,  //ジョイスティック2の左
    0b00001000,  //ジョイスティック2の右
    0b00010000,  //ジョイスティック2のC
    0b00100000,  //ジョイスティック2のA
    0b01000000,  //ジョイスティック2のB
    0b10000000,  //ジョイスティック2のD
    0b00000011,  //ジョイスティック2のSELECT
    0b00001100,  //ジョイスティック2のRUN
  };
*/
  //  perl misc/itoc.pl xeij/XEiJ.java PPI_BUTTON_MASK
  public static final char[] PPI_BUTTON_MASK = "\1\2\4\b\20 @\200\3\f\1\2\4\b\20 @\200\3\f".toCharArray ();

  //モード
  public static boolean ppiJoyKey;  //true=キーボード入力をジョイスティック入力とみなす
  public static boolean ppiJoyAuto;  //true=ジョイスティックポートが継続的にアクセスされている間だけ切り替える
  public static boolean ppiJoyBlock;  //true=ジョイスティック入力が有効なときはキーボード入力を取り除く

  //自動切り替え
  //  PPIのポートが参照されてから一定時間だけJOYKEYを有効にする
  public static final long PPI_JOY_TIME_SPAN = TMR_FREQ * 100 / 1000;  //自動切り替えの有効時間(TMR_FREQ単位)。100ms
  public static long ppiJoyTimeLimit;  //最後にPPIのポートがアクセスされたときのmpuClockTime+PPI_JOY_TIME_SPAN

  //ポートの値
  public static int ppiPortA;
  public static int ppiPortB;
  public static int ppiPortC;

  //状態
  public static final int[] ppiKeyCode = new int[20];  //キーコードの割り当て
  public static final boolean[] ppiRepeatOn = new boolean[20];  //リピートの可否。false=リピートが無効,true=リピートが有効
  public static final int[] ppiDelayTime = new int[20];  //リピート開始時間(ms)
  public static final int[] ppiIntervalTime = new int[20];  //リピート間隔(ms)

  public static final boolean[] ppiKeyPressed = new boolean[20];  //キーの押し下げ状態。false=押されていない,true=押されている
  public static final int[] ppiButtonStatus = new int[20];  //ボタンの状態。0=押されていない,PPI_KEY_MASK[i]=押されている。リピート中はキーが押されていてもボタンが押されていない状態がある
  public static final TimerTask[] ppiRepeatTask = new TimerTask[20];  //リピートのタスク

  //メニュー
  public static final JTextField[] ppiKeyTextField = new JTextField[20];
  public static final JCheckBox[] ppiRepeatCheckBox = new JCheckBox[20];
  public static final SpinnerNumberModel[] ppiDelayModel = new SpinnerNumberModel[20];
  public static final JSpinner[] ppiDelaySpinner = new JSpinner[20];
  public static final SpinnerNumberModel[] ppiIntervalModel = new SpinnerNumberModel[20];
  public static final JSpinner[] ppiIntervalSpinner = new JSpinner[20];

  //ウインドウ
  public static JFrame ppiFrame;

  //ppiInit ()
  //  PPIを初期化する
  public static void ppiInit () {

    for (int i = 0; i < 20; i++) {
      //ppiKeyCode[i] = KeyEvent.VK_UNDEFINED;
      //ppiRepeatOn[i] = false;
      //ppiDelayTime[i] = 50;
      //ppiIntervalTime[i] = 50;
      ppiKeyPressed[i] = false;
      ppiButtonStatus[i] = 0;
      ppiRepeatTask[i] = null;
    }

    //ppiKeyCode[0] = KeyEvent.VK_UP;  //上
    //ppiKeyCode[1] = KeyEvent.VK_DOWN;  //下
    //ppiKeyCode[2] = KeyEvent.VK_LEFT;  //左
    //ppiKeyCode[3] = KeyEvent.VK_RIGHT;  //右
    ////ppiKeyCode[4] = KeyEvent.VK_C;  //C
    //ppiKeyCode[5] = KeyEvent.VK_Z;  //A
    //ppiKeyCode[6] = KeyEvent.VK_X;  //B
    ////ppiKeyCode[7] = KeyEvent.VK_V;  //D
    //ppiKeyCode[8] = KeyEvent.VK_S;  //SELECT
    //ppiKeyCode[9] = KeyEvent.VK_D;  //RUN

    ppiReset ();

  }  //ppiInit()

  //  
  public static String ppiMakeParam () {
    StringBuilder sb = new StringBuilder ();
    for (int i = 0; i < 20; i++) {
      if (ppiKeyCode[i] != KeyEvent.VK_UNDEFINED) {
        sb.append (ppiKeyCode[i]);
      }
      if (ppiRepeatOn[i] || ppiDelayTime[i] != 50 || ppiIntervalTime[i] != 50) {
        sb.append (':');
        if (ppiRepeatOn[i]) {
          sb.append ('1');
        }
        if (ppiDelayTime[i] != 50 || ppiIntervalTime[i] != 50) {
          sb.append (':');
          if (ppiDelayTime[i] != 50) {
            sb.append (ppiDelayTime[i]);
          }
          if (ppiIntervalTime[i] != 50) {
            sb.append (':').append (ppiIntervalTime[i]);
          }
        }
      }
      sb.append (',');
    }
    //末尾の','の並びを取り除く
    {
      int end = sb.length ();
      int start = end;
      while (0 < start && sb.charAt (start - 1) == ',') {
        start--;
      }
      sb.delete (start, end);
    }
    return sb.toString ();
  }  //ppiMakeParam()

  public static void ppiParseParam (String param) {
    String[] a = param.split (",");
    for (int i = 0; i < 20; i++) {
      ppiKeyCode[i] = KeyEvent.VK_UNDEFINED;
      ppiRepeatOn[i] = false;
      ppiDelayTime[i] = 50;
      ppiIntervalTime[i] = 50;
      if (i < a.length) {
        String[] b = a[i].split (":");
        if (0 < b.length) {
          ppiKeyCode[i] = fmtParseInt (b[0], 0, 0, 65535, KeyEvent.VK_UNDEFINED);
          if (1 < b.length) {
            ppiRepeatOn[i] = fmtParseInt (b[1], 0, 0, 1, 0) != 0;
            if (2 < b.length) {
              ppiDelayTime[i] = fmtParseInt (b[2], 0, 10, 2000, 50) / 10 * 10;
              if (3 < b.length) {
                ppiIntervalTime[i] = fmtParseInt (b[3], 0, 10, 2000, 50) / 10 * 10;
              }
            }
          }
        }
      }
    }
  }  //ppiParseParam(String)

  //リセット
  public static void ppiReset () {
    ppiPortA = 255;
    ppiPortB = 255;
    ppiPortC = 0;
    ppiJoyTimeLimit = 0L;
    for (int i = 0; i < 20; i++) {
      ppiRelease (i);
    }
  }  //ppiReset()

  //ppiSetKey (i, keyCode)
  //  キー割り当てを変更する
  public static void ppiSetKey (int i, int keyCode) {
    ppiRelease (i);
    if (keyCode == KeyEvent.VK_ESCAPE) {  //Escキーは解除とみなす
      keyCode = KeyEvent.VK_UNDEFINED;
    }
    ppiKeyCode[i] = keyCode;
    ppiKeyTextField[i].setText (keyCode == KeyEvent.VK_UNDEFINED ? "なし" : KeyEvent.getKeyText (keyCode));
  }  //ppiSetKey(int,int)

  //ppiSetRepeat (i, on)
  //  リピートの可否を設定する
  public static void ppiSetRepeat (int i, boolean on) {
    ppiRelease (i);
    ppiRepeatOn[i] = on;
    ppiRepeatCheckBox[i].setSelected (on);
  }  //ppiSetRepeat(int,boolean)

  //ppiSetDelay (i, time)
  //  リピート開始時間を設定する
  public static void ppiSetDelay (int i, int time) {
    ppiRelease (i);
    ppiDelayTime[i] = time;
    ppiDelayModel[i].setValue (new Integer (time));
  }  //ppiSetDelay(int,int)

  //ppiSetInterval (i, time)
  //  リピート間隔を設定する
  public static void ppiSetInterval (int i, int time) {
    ppiRelease (i);
    ppiIntervalTime[i] = time;
    ppiIntervalModel[i].setValue (new Integer (time));
  }  //ppiSetInterval(int,int)

  //consume = ppiInput (ke, pressed)
  //  JOYKEYの処理
  //  consume  true=入力をキーボードに渡さない
  public static boolean ppiInput (KeyEvent ke, boolean pressed) {
    boolean consume = false;
    if (ppiJoyKey && (!ppiJoyAuto || mpuClockTime < ppiJoyTimeLimit)) {
      int keyCode = ke.getKeyCode ();
      for (int i = 0; i < 20; i++) {
        if (ppiKeyCode[i] == keyCode) {  //JOYKEYに設定されているキーが押されたまたは離された
          consume = pressed && ppiJoyBlock;  //押されたときだけキーボード入力を取り除く。特に自動有効化のときは押されている間に有効になって離されたデータだけ取り除かれるとキーボード側は押されたままになっていると判断してリピートが止まらなくなる
          if (ppiKeyPressed[i] != pressed) {  //押されていなかったキーが押されたまたは押されていたキーが離された
            ppiRelease (i);
            if (pressed) {  //押されていなかったキーが押された
              ppiKeyPressed[i] = true;
              ppiButtonStatus[i] = PPI_BUTTON_MASK[i];
              ppiUpdate (i);  //ポートの状態を更新する
              if (ppiRepeatOn[i]) {  //リピートが有効
                tmrTimer.schedule (ppiRepeatTask[i] = new PPIRepeatTask (i), ppiDelayTime[i], ppiIntervalTime[i]);  //リピート開始
              }
            }
          }
        }
      }
    }
    return consume;
  }  //ppiInput(KeyEvent,boolean)

  //ppiRelease (i)
  //  ボタンが押されていたら離されたことにする
  public static void ppiRelease (int i) {
    if (ppiKeyPressed[i]) {
      ppiKeyPressed[i] = false;
      TimerTask task = ppiRepeatTask[i];
      if (task != null) {
        task.cancel ();
        ppiRepeatTask[i] = null;
      }
      ppiButtonStatus[i] = 0;
      ppiUpdate (i);
    }
  }  //ppiRelease(int)

  //$$PPT PPIリピートタスク
  //  ボタンのリピート(連射)を行う
  public static class PPIRepeatTask extends TimerTask {
    private int i;
    public PPIRepeatTask (int i) {
      this.i = i;
    }
    @Override public void run () {
      if (ppiRepeatOn[i] && ppiKeyPressed[i]) {  //リピートが有効でキーはまだ押されている
        ppiButtonStatus[i] ^= PPI_BUTTON_MASK[i];  //ボタンを離すまたは押す
        ppiUpdate (i);  //ポートの状態を更新する
      } else {  //リピートが無効またはキーが離されている
        ppiButtonStatus[i] = 0;  //ボタンを離す
        ppiUpdate (i);  //ポートの状態を更新する
        cancel ();  //リピートを終了する
        ppiRepeatTask[i] = null;
      }
    }
  }  //class PPIRepeatTask

  //ppiUpdate (i)
  //  ポートの状態を更新する
  public static void ppiUpdate (int i) {
    //perl -e "for$t(0..15){printf'%04b,',2112>>($t&12)&12|36>>($t<<1&6)&3}"
    //0000,0001,0010,0000,0100,0101,0110,0100,1000,1001,1010,1000,0000,0001,0010,0000,
    if (i < 10) {
      int t = ppiButtonStatus[ 0] | ppiButtonStatus[ 1] | ppiButtonStatus[ 2] | ppiButtonStatus[ 3];  //上下左右
      ppiPortA = (2112 >> (t & 12) & 12 | 36 >> (t << 1 & 6) & 3 |  //上と下および左と右の同時押しをキャンセルする
                  ppiButtonStatus[ 4] | ppiButtonStatus[ 5] | ppiButtonStatus[ 6] | ppiButtonStatus[ 7] |
                  ppiButtonStatus[ 8] | ppiButtonStatus[ 9]) ^ 255;
    } else {
      int t = ppiButtonStatus[10] | ppiButtonStatus[11] | ppiButtonStatus[12] | ppiButtonStatus[13];  //上下左右
      ppiPortB = (2112 >> (t & 12) & 12 | 36 >> (t << 1 & 6) & 3 |  //上と下および左と右の同時押しをキャンセルする
                  ppiButtonStatus[14] | ppiButtonStatus[15] | ppiButtonStatus[16] | ppiButtonStatus[17] |
                  ppiButtonStatus[18] | ppiButtonStatus[19]) ^ 255;
    }
  }  //ppiUpdate(int)

  public static void ppiSetPortA (int i, boolean pressed) {
    if (pressed) {
      ppiPortA &= ~(1 << i);
    } else {
      ppiPortA |= 1 << i;
    }
  }  //ppiSetPortA(int,boolean)
  public static void ppiSetPortB (int i, boolean pressed) {
    if (pressed) {
      ppiPortB &= ~(1 << i);
    } else {
      ppiPortB |= 1 << i;
    }
  }  //ppiSetPortB(int,boolean)

  //ppiStart ()
  public static void ppiStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PPI_FRAME_KEY)) {
      ppiOpen ();
    }
  }  //ppiStart()

  //ppiOpen ()
  //  ジョイスティックの設定ウインドウを開く
  public static void ppiOpen () {
    if (ppiFrame == null) {
      ppiMakeFrame ();
    }
    ppiFrame.setVisible (true);
  }  //ppiOpen()

  //ppiMakeFrame ()
  //  ジョイスティックの設定ウインドウを作る
  //  ここでは開かない
  public static void ppiMakeFrame () {

    //キーリスナー
    KeyListener keyListener = new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        ppiSetKey (Integer.parseInt (((JTextField) ke.getSource ()).getName ()), ke.getKeyCode ());
        ke.consume ();
      }
        @Override public void keyReleased (KeyEvent ke) {
          ke.consume ();
        }
      @Override public void keyTyped (KeyEvent ke) {
        ke.consume ();
      }
    };

    //チェンジリスナー
    ChangeListener changeListener = new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        JSpinner spinner = (JSpinner) ce.getSource ();
        String name = spinner.getName ();
        if (name.startsWith ("Delay ")) {
          int i = Integer.parseInt (name.substring (6));
          ppiDelayTime[i] = ppiDelayModel[i].getNumber ().intValue ();
        } else if (name.startsWith ("Interval ")) {
          int i = Integer.parseInt (name.substring (9));
          ppiIntervalTime[i] = ppiIntervalModel[i].getNumber ().intValue ();
        }
      }
    };

    //アクションリスナー
    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Consider some keyboard-input as joystick-input":
          ppiJoyKey = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        case "Work only while joystick-port is read continuously":
          ppiJoyAuto = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        case "Block keyboard-input while joystick-input is effective":
          ppiJoyBlock = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        default:
          if (command.startsWith ("Repeat ")) {
            int i = Integer.parseInt (command.substring (7));
            ppiRepeatOn[i] = ppiRepeatCheckBox[i].isSelected ();
          }
        }
      }
    };

    for (int i = 0; i < 20; i++) {
      ppiKeyTextField[i] = setHorizontalAlignment (
        setName (
          createTextField (ppiKeyCode[i] == KeyEvent.VK_UNDEFINED ? "なし" : KeyEvent.getKeyText (ppiKeyCode[i]), 10),
          String.valueOf (i)),
        JTextField.CENTER);
      addRemovableListener (
        ppiKeyTextField[i],
        keyListener);
      ppiRepeatCheckBox[i] = setText (createCheckBox (ppiRepeatOn[i], "Repeat " + i, actionListener), "");
      ppiDelayModel[i] = new SpinnerNumberModel (ppiDelayTime[i], 10, 2000, 10);
      ppiDelaySpinner[i] = setName (createNumberSpinner (ppiDelayModel[i], 4, changeListener), "Delay " + i);
      ppiIntervalModel[i] = new SpinnerNumberModel (ppiIntervalTime[i], 10, 2000, 10);
      ppiIntervalSpinner[i] = setName (createNumberSpinner (ppiIntervalModel[i], 4, changeListener), "Interval " + i);
    }

    //ウインドウ
    ppiFrame = Multilingual.mlnTitle (
      createRestorableSubFrame (
        Settings.SGS_PPI_FRAME_KEY,
        "Joystick Settings",
        null,
        createVerticalBox (
          Multilingual.mlnText (createCheckBox (ppiJoyKey, "Consider some keyboard-input as joystick-input", actionListener),
                   "ja", "一部のキーボード入力をジョイスティック入力とみなす"),
          Multilingual.mlnText (createCheckBox (ppiJoyAuto, "Work only while joystick-port is read continuously", actionListener),
                   "ja", "ジョイスティックポートが継続的に読み出されている間だけ機能する"),
          Multilingual.mlnText (createCheckBox (ppiJoyBlock, "Block keyboard-input converted to joystick-input", actionListener),
                   "ja", "ジョイスティック入力に変換されたキーボード入力を遮断する"),
          //          0         1     2     3      4        5
          //   0      -      Button  Key  Burst  Delay  Interval
          //   1 -----------------------------------------------
          //   2 Joystick 1    Up     TF    -      -        -
          //   3              Down    TF    -      -        -
          //   4              Left    TF    -      -        -
          //   5              Right   TF    -      -        -
          //   6                A     TF    CB     S        S
          //   7                B     TF    CB     S        S
          //   8             SELECT   TF    -      -        -
          //   9               RUN    TF    -      -        -
          //  10 -----------------------------------------------
          //  11 Joystick 2    Up     TF    -      -        -
          //  12              Down    TF    -      -        -
          //  13              Left    TF    -      -        -
          //  14              Right   TF    -      -        -
          //  15                A     TF    CB     S        S
          //  16                B     TF    CB     S        S
          //  17             SELECT   TF    -      -        -
          //  18               RUN    TF    -      -        -
          createGridPanel (
            6, 19,
            "paddingLeft=3,paddingRight=3,center",   //gridStyles
            //0     1
            "italic;italic",  //colStyles
            //                              1
            //0     1               234567890
            "italic;colSpan=6,widen;;;;;;;;;colSpan=6,widen",  //rowStyles
            //                                                        11
            //0    12              3    4    5    6    7    8    9    01
            ";;;;;;;rowSpan=8;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;rowSpan=8",  //cellStyles
            null,  //(0,0)
            Multilingual.mlnText (createLabel ("Button"), "ja", "ボタン"),  //(1,0)
            Multilingual.mlnText (createLabel ("Key"), "ja", "キー"),  //(2,0)
            Multilingual.mlnText (createLabel ("Burst"), "ja", "連射"),  //(3,0)
            Multilingual.mlnText (createLabel ("Delay (ms)"), "ja", "開始 (ms)"),  //(4,0)
            Multilingual.mlnText (createLabel ("Interval (ms)"), "ja", "間隔 (ms)"),  //(5,0)
            createHorizontalSeparator (),  //(0,1)-(5,1)
            Multilingual.mlnText (createLabel ("Joystick 1"), "ja", "ジョイスティック 1"),  //(0,2)-(0,9)
            Multilingual.mlnText (createLabel ("Up"), "ja", "上"),  //(1,1)
            ppiKeyTextField[ 0],  //(2,1)
            null,  //(3,1)
            null,  //(4,1)
            null,  //(5,1)
            //(0,3)
            Multilingual.mlnText (createLabel ("Down"), "ja", "下"),  //(1,3)
            ppiKeyTextField[ 1],  //(2,3)
            null,  //(3,3)
            null,  //(4,3)
            null,  //(5,3)
            //(0,4)
            Multilingual.mlnText (createLabel ("Left"), "ja", "左"),  //(1,4)
            ppiKeyTextField[ 2],  //(2,4)
            null,  //(3,4)
            null,  //(4,4)
            null,  //(5,4)
            //(0,5)
            Multilingual.mlnText (createLabel ("Right"), "ja", "右"),  //(1,5)
            ppiKeyTextField[ 3],  //(2,5)
            null,  //(3,5)
            null,  //(4,5)
            null,  //(5,5)
            //(0,6)
            "A",  //(1,6)
            ppiKeyTextField[ 5],  //(2,6)
            ppiRepeatCheckBox[ 5],  //(3,6)
            ppiDelaySpinner[ 5],  //(4,6)
            ppiIntervalSpinner[ 5],  //(5,6)
            //(0,7)
            "B",  //(1,7)
            ppiKeyTextField[ 6],  //(2,7)
            ppiRepeatCheckBox[ 6],  //(3,7)
            ppiDelaySpinner[ 6],  //(4,7)
            ppiIntervalSpinner[ 6],  //(5,7)
            //(0,8)
            "SELECT",  //(1,8)
            ppiKeyTextField[ 8],  //(2,8)
            null,  //(3,8)
            null,  //(4,8)
            null,  //(5,8)
            //(0,9)
            "RUN",  //(1,9)
            ppiKeyTextField[ 9],  //(2,9)
            null,  //(3,9)
            null,  //(4,9)
            null,  //(5,9)
            createHorizontalSeparator (),  //(0,10)-(5,10)
            Multilingual.mlnText (createLabel ("Joystick 2"), "ja", "ジョイスティック 2"),  //(0,11)-(0,18)
            Multilingual.mlnText (createLabel ("Up"), "ja", "上"),  //(1,11)
            ppiKeyTextField[10],  //(2,11)
            null,  //(3,11)
            null,  //(4,11)
            null,  //(5,11)
            //(0,12)
            Multilingual.mlnText (createLabel ("Down"), "ja", "下"),  //(1,12)
            ppiKeyTextField[11],  //(2,12)
            null,  //(3,12)
            null,  //(4,12)
            null,  //(5,12)
            //(0,13)
            Multilingual.mlnText (createLabel ("Left"), "ja", "左"),  //(1,13)
            ppiKeyTextField[12],  //(2,13)
            null,  //(3,13)
            null,  //(4,13)
            null,  //(5,13)
            //(0,14)
            Multilingual.mlnText (createLabel ("Right"), "ja", "右"),  //(1,14)
            ppiKeyTextField[13],  //(2,14)
            null,  //(3,14)
            null,  //(4,14)
            null,  //(5,14)
            //(0,15)
            "A",  //(1,15)
            ppiKeyTextField[15],  //(2,15)
            ppiRepeatCheckBox[15],  //(3,15)
            ppiDelaySpinner[15],  //(4,15)
            ppiIntervalSpinner[15],  //(5,15)
            //(0,16)
            "B",  //(1,16)
            ppiKeyTextField[16],  //(2,16)
            ppiRepeatCheckBox[16],  //(3,16)
            ppiDelaySpinner[16],  //(4,16)
            ppiIntervalSpinner[16],  //(5,16)
            //(0,17)
            "SELECT",  //(1,17)
            ppiKeyTextField[18],  //(2,17)
            null,  //(3,17)
            null,  //(4,17)
            null,  //(5,17)
            //(0,18)
            "RUN",  //(1,18)
            ppiKeyTextField[19],  //(2,18)
            null,  //(3,18)
            null,  //(4,18)
            null  //(5,18)
            )
          )
        ),
      "ja", "ジョイスティックの設定");
  }  //ppiMakeFrame()



  //========================================================================================
  //$$IOI I/O割り込み
  //  0x00e9c000

  public static final int IOI_STATUS = 0x00e9c001;
  public static final int IOI_VECTOR = 0x00e9c003;

  public static final int IOI_FDC_STATUS = 0x80;
  public static final int IOI_FDD_STATUS = 0x40;
  public static final int IOI_PRN_STATUS = 0x20;
  public static final int IOI_HDC_STATUS = 0x10;

  public static final int IOI_SPC_MASK = 0x4000;
  public static final int IOI_HDC_MASK = 0x08;
  public static final int IOI_FDC_MASK = 0x04;
  public static final int IOI_FDD_MASK = 0x02;
  public static final int IOI_PRN_MASK = 0x01;

  public static final int IOI_SPC_VECTOR = 0x6c;  //内蔵SCSI
  public static final int IOI_FDC_VECTOR = 0;
  public static final int IOI_FDD_VECTOR = 1;
  public static final int IOI_HDC_VECTOR = 2;
  public static final int IOI_PRN_VECTOR = 3;

  public static int ioiStatus;  //ステータス(R)。ステータスレジスタの上位4ビット
  //  0b10000000  FDC割り込み要求あり。リザルトステータス読み取り要求、コマンド起動要求
  //  0b01000000  FDD割り込み要求あり。メディア挿入、メディア排出
  //  0b00100000  PRNビジー。1のとき0x00e8c001にデータをセットしてから0x00e8c003のbit0を0→1で出力
  //  0b00010000  HDC割り込み要求あり。コマンド終了
  public static int ioiMask;  //割り込みマスク(R/W)。ステータスレジスタの下位4ビット
  //  0b00001000  HDC割り込み可
  //  0b00000100  FDC割り込み可
  //  0b00000010  FDD割り込み可
  //  0b00000001  PRN割り込み可
  public static int ioiRequest;  //割り込み要求。デバイスは操作しないこと
  //  0b00010000_00000000  SPC割り込み要求あり
  //  0b00001000  HDC割り込み要求あり
  //  0b00000100  FDC割り込み要求あり
  //  0b00000010  FDD割り込み要求あり
  //  0b00000001  PRN割り込み要求あり
  public static int ioiVector;  //ベクタ。下位2ビットは常に0

  //ioiReset ()
  //  I/O割り込みをリセットする
  public static void ioiReset () {
    ioiStatus = PrinterPort.prnConnectedOn ? IOI_PRN_STATUS : 0;
    ioiMask = 0;
    ioiRequest = 0;
    ioiVector = 0;
  }  //ioiReset()

  //割り込み
  //  maskはIOI_SPC_MASK,IOI_HDC_MASK,IOI_FDC_MASK,IOI_FDD_MASK,IOI_PRN_MASKのいずれか
  public static void ioiInterrupt (int mask) {
    if (((IOI_SPC_MASK | ioiMask) & mask) != 0) {
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn &&
          (mask & (IOI_FDC_MASK | IOI_FDD_MASK)) != 0) {
        System.out.printf ("%08x %04x ioiInterrupt(0x%04x) ioiRequest=0x%02x->0x%02x,mpuIRR=0x%02x->0x%02x\n",
                           regPC0, regSRT1 | regSRT0 | regSRS | regSRM | regSRI | regCCR,
                           mask,
                           ioiRequest, ioiRequest | mask,
                           mpuIRR, mpuIRR | MPU_IOI_INTERRUPT_MASK);
      }
      ioiRequest |= mask;
      mpuIRR |= MPU_IOI_INTERRUPT_MASK;
    }
  }  //ioiInterrupt(int)

  //以下は0→1で割り込み発生

  public static void ioiFdcFall () {
    if ((ioiStatus & IOI_FDC_STATUS) != 0) {
      ioiStatus &= ~IOI_FDC_STATUS;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiFdcFall() ioiStatus=0x%02x\n", regPC0, ioiStatus);
      }
    }
  }  //ioiFdcFall()

  public static void ioiFdcRise () {
    if ((ioiStatus & IOI_FDC_STATUS) == 0) {
      ioiStatus |= IOI_FDC_STATUS;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiFdcRise() ioiStatus=0x%02x\n", regPC0, ioiStatus);
      }
      ioiInterrupt (IOI_FDC_MASK);
    }
  }  //ioiFdcRise()

  public static void ioiFddFall () {
    if ((ioiStatus & IOI_FDD_STATUS) != 0) {
      ioiStatus &= ~IOI_FDD_STATUS;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiFddFall() ioiStatus=0x%02x\n", regPC0, ioiStatus);
      }
    }
  }  //ioiFddFall()

  public static void ioiFddRise () {
    if ((ioiStatus & IOI_FDD_STATUS) == 0) {
      ioiStatus |= IOI_FDD_STATUS;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiFddRise() ioiStatus=0x%02x\n", regPC0, ioiStatus);
      }
      ioiInterrupt (IOI_FDD_MASK);
    }
  }  //ioiFddRise()

  public static void ioiHdcFall () {
    if ((ioiStatus & IOI_HDC_STATUS) != 0) {
      ioiStatus &= ~IOI_HDC_STATUS;
    }
  }  //ioiHdcFall()

  public static void ioiHdcRise () {
    if ((ioiStatus & IOI_HDC_STATUS) == 0) {
      ioiStatus |= IOI_HDC_STATUS;
      ioiInterrupt (IOI_HDC_MASK);
    }
  }  //ioiHdcRise()

  public static void ioiPrnFall () {
    if ((ioiStatus & IOI_PRN_STATUS) != 0) {
      ioiStatus &= ~IOI_PRN_STATUS;
    }
  }  //ioiPrnFall()

  public static void ioiPrnRise () {
    if ((ioiStatus & IOI_PRN_STATUS) == 0) {
      ioiStatus |= IOI_PRN_STATUS;
      ioiInterrupt (IOI_PRN_MASK);
    }
  }  //ioiPrnRise()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int ioiAcknowledge () {
    if ((ioiRequest & IOI_SPC_MASK) != 0) {
      ioiRequest &= ~IOI_SPC_MASK;
      return IOI_SPC_VECTOR;
    }
    //優先順位はFDC,FDD,HDC,PRN
    if ((ioiRequest & IOI_FDC_MASK) != 0) {
      int request = ioiRequest & ~IOI_FDC_MASK;
      int vector = ioiVector | IOI_FDC_VECTOR;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiAcknowledge() ioiRequest=0x%02x->0x%02x,vector=0x%02x\n",
                           regPC0,
                           ioiRequest, request,
                           vector);
      }
      ioiRequest = request;
      return vector;
    }
    if ((ioiRequest & IOI_FDD_MASK) != 0) {
      int request = ioiRequest & ~IOI_FDD_MASK;
      int vector = ioiVector | IOI_FDD_VECTOR;
      if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
        System.out.printf ("%08x ioiAcknowledge() ioiRequest=0x%02x->0x%02x,vector=0x%02x\n",
                           regPC0,
                           ioiRequest, request,
                           vector);
      }
      ioiRequest = request;
      return vector;
    }
    if ((ioiRequest & IOI_HDC_MASK) != 0) {
      ioiRequest &= ~IOI_HDC_MASK;
      return ioiVector | IOI_HDC_VECTOR;
    }
    if ((ioiRequest & IOI_PRN_MASK) != 0) {
      ioiRequest &= ~IOI_PRN_MASK;
      return ioiVector | IOI_PRN_VECTOR;
    }
    return 0;
  }  //ioiAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void ioiDone () {
    if (ioiRequest != 0) {
      mpuIRR |= MPU_IOI_INTERRUPT_MASK;
    }
  }  //ioiDone()

  //d = ioiReadStatus ()
  //  リードステータス
  //  0x00e9c001
  public static int ioiReadStatus () {
    int d = ioiStatus | ioiMask;
    if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
      System.out.printf ("%08x ioiReadStatus(0x%08x)=0x%02x\n",
                         regPC0, IOI_STATUS, d);
    }
    return d;
  }  //ioiReadStatus()

  //d = ioiReadVector ()
  //  リードベクタ
  //  0x00e9c003
  public static int ioiReadVector () {
    int d = ioiVector;
    if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
      System.out.printf ("%08x ioiReadVector(0x%08x)=0x%02x\n",
                         regPC0, IOI_VECTOR, d);
    }
    return d;
  }  //ioiReadVector()

  //ioiWriteMask (d)
  //  ライトマスク
  //  0x00e9c001
  //  割り込みマスクを設定する
  //  割り込みマスクがクリアされたとき割り込みステータスをクリアする
  //    割り込みマスクがセットされたとき割り込みステータスがセットされていたら割り込み要求をセットする？
  //    同時には成り立たない
  public static void ioiWriteMask (int d) {
    int mask = d & 0x0f;
    int status = ioiStatus;
    int disabled = ioiMask & ~mask;  //1=1→0
    //int request = ioiRequest;
    //int enabled = ~ioiMask & mask;  //1=0→1
    if ((disabled & IOI_FDC_MASK) != 0) {  //マスクがクリアされた
      status &= ~IOI_FDC_STATUS;  //ステータスをクリアする
      //} else if ((enabled & IOI_FDC_MASK) != 0 && (status & IOI_FDC_STATUS) != 0) {  //マスクがセットされたときステータスがセットされていた
      //  request |= IOI_FDC_MASK;  //要求をセットする
    }
    if ((disabled & IOI_FDD_MASK) != 0) {  //マスクがクリアされた
      status &= ~IOI_FDD_STATUS;  //ステータスをクリアする
      //} else if ((enabled & IOI_FDD_MASK) != 0 && (status & IOI_FDD_STATUS) != 0) {  //マスクがセットされたときステータスがセットされていた
      //  request |= IOI_FDD_MASK;  //要求をセットする
    }
    if ((disabled & IOI_HDC_MASK) != 0) {  //マスクがクリアされた
      status &= ~IOI_HDC_STATUS;  //ステータスをクリアする
      //} else if ((enabled & IOI_HDC_MASK) != 0 && (status & IOI_HDC_STATUS) != 0) {  //マスクがセットされたときステータスがセットされていた
      //  request |= IOI_HDC_MASK;  //要求をセットする
    }
    if ((disabled & IOI_PRN_MASK) != 0) {  //マスクがクリアされた
      status &= ~IOI_PRN_STATUS;  //ステータスをクリアする
      //} else if ((enabled & IOI_PRN_MASK) != 0 && (status & IOI_PRN_STATUS) != 0) {  //マスクがセットされたときステータスがセットされていた
      //  request |= IOI_PRN_MASK;  //要求をセットする
    }
    if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
      System.out.printf ("%08x ioiWriteMask(0x%08x,0x%02x) ioiMask=0x%04x->0x%04x,ioiStatus=0x%02x->0x%02x\n",
                         regPC0, IOI_STATUS, d & 255,
                         ioiMask, mask,
                         ioiStatus, status);
      //System.out.printf ("%08x ioiWriteMask(0x%08x,0x%02x) ioiMask=0x%04x->0x%04x,ioiStatus=0x%02x->0x%02x,ioiRequest=0x%02x->0x%02x\n",
      //                   regPC0, IOI_STATUS, d & 255,
      //                   ioiMask, mask,
      //                   ioiStatus, status,
      //                   ioiRequest, request);
    }
    ioiMask = mask;
    ioiStatus = status;
    //if (ioiRequest != request) {
    //  ioiRequest = request;
    //  mpuIRR |= MPU_IOI_INTERRUPT_MASK;
    //}
  }  //ioiWriteMask(int)

  //ioiWriteVector (d)
  //  ライトベクタ
  //  0x00e9c003
  //  割り込みベクタを設定する
  public static void ioiWriteVector (int d) {
    int vector = d & 0xfc;
    if (FDC.FDC_DEBUG_TRACE && FDC.fdcDebugLogOn) {
      System.out.printf ("%08x ioiWriteVector(0x%08x,0x%02x) ioiVector=0x%02x->0x%02x\n",
                         regPC0, IOI_VECTOR, d & 255,
                         ioiVector, vector);
    }
    ioiVector = vector;
  }  //ioiWriteVector(int)



  //========================================================================================
  //$$EB2 拡張ボードレベル2割り込み

  public static final int EB2_SPC_MASK = 0x4000;  //拡張SCSI
  public static final int EB2_SPC_VECTOR = 0xf6;  //拡張SCSI

  //割り込み要求
  //  0b00010000_00000000  SPC割り込み要求あり
  public static int eb2Request;  //割り込み要求。デバイスは操作しないこと

  //eb2Reset ()
  //  拡張ボードレベル2割り込みをリセットする
  public static void eb2Reset () {
    eb2Request = 0;
  }  //eb2Reset()

  //eb2Interrupt (mask)
  //  割り込み要求
  //  デバイスが割り込みを要求するときに呼び出す
  //  mask  EB2_SPC_VECTOR  拡張SCSI割り込みを要求する
  public static void eb2Interrupt (int mask) {
    eb2Request |= mask;
    mpuIRR |= MPU_EB2_INTERRUPT_MASK;
  }  //eb2Interrupt(int)

  //vector = eb2Acknowledge ()
  //  割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int eb2Acknowledge () {
    if ((eb2Request & EB2_SPC_MASK) != 0) {
      eb2Request &= ~EB2_SPC_MASK;
      return EB2_SPC_VECTOR;
    }
    return 0;
  }  //eb2Acknowledge()

  //eb2Done ()
  //  割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void eb2Done () {
    if (eb2Request != 0) {
      mpuIRR |= MPU_EB2_INTERRUPT_MASK;
    }
  }  //eb2Done()



  //========================================================================================
  //TGA ツクモグラフィックアクセラレータPCMボード
  //  0x00e9e200  ツクモグラフィックアクセラレータPCMボード(TS-6BGA)
  //! 非対応



  //========================================================================================
  //WIN WINDRV
  //  0x00e9f000  040Excel
  //  0x00e9f000  WINDRV
  //! 非対応



  //========================================================================================
  //$$EXS 拡張SCSI
  //  内蔵SCSIと共通



  //========================================================================================
  //$$XB2 拡張ボード領域2
  //  0x00eaf900  FAXボード(CZ-6BC1)
  //  0x00eafa00  MIDIボード(CZ-6BM1)
  //  0x00eafb00  パラレルボード(CZ-6BN1)
  //  0x00eafc00  RS-232Cボード(CZ-6BF1)
  //  0x00eafd00  ユニバーサルI/Oボード(CZ-6BU1)
  //  0x00eafe00  GP-IBボード(CZ-6BG1)
  //  0x00eaff00  スーパーバイザエリア設定
  //
  //  スーパーバイザエリア設定のみ対応

  //スーパーバイザエリア設定ポート
  //  0x00eaff81  bit0  0x00200000～0x0023ffff
  //              bit1  0x00240000～0x0027ffff
  //              bit2  0x00280000～0x002bffff
  //              bit3  0x002c0000～0x002fffff
  //              bit4  0x00300000～0x0033ffff
  //              bit5  0x00340000～0x0037ffff
  //              bit6  0x00380000～0x003bffff
  //              bit7  0x003c0000～0x003fffff
  //  0x00eaff83  bit0  0x00400000～0x0043ffff
  //              bit1  0x00440000～0x0047ffff
  //              bit2  0x00480000～0x004bffff
  //              bit3  0x004c0000～0x004fffff
  //              bit4  0x00500000～0x0053ffff
  //              bit5  0x00540000～0x0057ffff
  //              bit6  0x00580000～0x005bffff
  //              bit7  0x005c0000～0x005fffff
  //  0x00eaff85  bit0  0x00600000～0x0063ffff
  //              bit1  0x00640000～0x0067ffff
  //              bit2  0x00680000～0x006bffff
  //              bit3  0x006c0000～0x006fffff
  //              bit4  0x00700000～0x0073ffff
  //              bit5  0x00740000～0x0077ffff
  //              bit6  0x00780000～0x007bffff
  //              bit7  0x007c0000～0x007fffff
  //  0x00eaff87  bit0  0x00800000～0x0083ffff
  //              bit1  0x00840000～0x0087ffff
  //              bit2  0x00880000～0x008bffff
  //              bit3  0x008c0000～0x008fffff
  //              bit4  0x00900000～0x0093ffff
  //              bit5  0x00940000～0x0097ffff
  //              bit6  0x00980000～0x009bffff
  //              bit7  0x009c0000～0x009fffff
  //  0x00eaff89  bit0  0x00a00000～0x00a3ffff
  //              bit1  0x00a40000～0x00a7ffff
  //              bit2  0x00a80000～0x00abffff
  //              bit3  0x00ac0000～0x00afffff
  //              bit4  0x00b00000～0x00b3ffff
  //              bit5  0x00b40000～0x00b7ffff
  //              bit6  0x00b80000～0x00bbffff
  //              bit7  0x00bc0000～0x00bfffff



  //========================================================================================
  //$$SMR SRAM
  //
  //  起動デバイスの強制指定
  //    リセット後最初の1回だけSRAMからの起動デバイスのリードを偽ることでOPT.1キーを押しながら起動や特定のデバイスからの起動が可能になる

  public static File smrLastFile;  //最後にアクセスしたファイル
  public static String smrSramName;  //SRAMイメージファイル名。SRAM.DATなど
  public static String smrSramData;  //SRAMの内容。gzip+base64

  public static int smrBootDevice;  //起動デバイス。0x00ed0018。0x0000=STD,0x9070～0x9370=FDn,0x8000～0x8f00=HDn,0xa000=ROM,0xb000=RAM,-1=設定しない
  public static int smrBootROM;  //ROM起動ハンドル。0x00ed000c。-1=設定しない
  public static int smrBootRAM;  //RAM起動アドレス。0x00ed0010。-1=設定しない

  public static int smrRepeatDelay;  //リピートディレイ。0x00ed003a。-1=設定しない,0..15=200+100*n(ms)
  public static int smrRepeatInterval;  //リピートインターバル。0x00ed003b。-1=設定しない,0..15=30+5*n^2(ms)
  public static boolean smrWriteEnableOn;  //true=SRAM書き込み可

  public static JMenu smrMenu;
  public static JMenu smrBootMenu;  //起動デバイスメニュー
  public static JRadioButtonMenuItem smrSTDMenuItem;

  //smrInit ()
  //  SRAMを初期化する
  //  HDC.hdcInit(),FDC.fdcInit(),HFS.hfsInit()よりも後に呼び出すこと
  public static void smrInit () {

    if (false) {
      System.out.printf ("smrBootDevice=0x%x\n", smrBootDevice);
      System.out.printf ("smrBootROM=0x%x\n", smrBootROM);
      System.out.printf ("smrBootRAM=0x%x\n", smrBootRAM);
    }

    if (prgIsLocal) {  //ローカルのとき
      //最後にアクセスしたファイルの初期値＝最初にファイルチューザーを開いたときに表示するディレクトリ
      smrLastFile = new File (".");  //カレントディレクトリ
    }

    //SRAMの内容を初期化する
    Arrays.fill (MainMemory.mmrM8, 0x00ed0000, 0x00ed4000, (byte) 0x00);  //SRAMをゼロクリアする
    if (smrSramName.length () != 0) {  //-sram=SRAMイメージファイル名が指定されたとき
      if (!smrLoadData (smrSramName)) {  //読み込めなかった
        smrSramName = "";
      }
    } else if (smrSramData.length () != 0) {  //-sramdata=gzip+base64が指定されたとき
      byte[] bb = ByteArray.byaDecodeGzip (ByteArray.byaDecodeBase64 (smrSramData));  //gzip+base64を解凍する
      if (bb.length == 0x00ed4000 - 0x00ed0000 &&  //サイズが合っている
          ByteArray.byaRls (bb, 0) == 0x82773638 && ByteArray.byaRls (bb, 4) == 0x30303057) {  //マジックがある
        System.arraycopy (bb, 0, MainMemory.mmrM8, 0x00ed0000, 0x00ed4000 - 0x00ed0000);  //SRAMにコピーする
      } else {  //壊れている
        prgMessage (Multilingual.mlnJapanese ?
                    "SRAM のデータが壊れています" :
                    "SRAM data is broken");
        smrSramData = "";
      }
    }

    //メニュー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Clear SRAM":  //SRAM クリア
          smrClear ();
          break;
        case "Load SRAM":  //SRAM 読み込み
          smrLoad ();
          break;
        case "Save SRAM":  //SRAM 書き出し
          smrSave ();
          break;
        }
      }
    };
    smrMenu =
      createMenu (
        "SRAM",
        Multilingual.mlnText (createMenuItem ("Clear SRAM", listener), "ja", "SRAM クリア"),
        setEnabled (
          Multilingual.mlnText (createMenuItem ("Load SRAM", listener), "ja", "SRAM 読み込み"),
          prgIsLocal),  //ローカルのとき
        setEnabled (
          Multilingual.mlnText (createMenuItem ("Save SRAM", listener), "ja", "SRAM 書き出し"),
          prgIsLocal)  //ローカルのとき
        );

    //起動デバイスメニュー
    ButtonGroup bootGroup = new ButtonGroup ();
    ActionListener bootListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        smrParseBootDevice (
          command.startsWith ("FDD ") ? "fd" + command.substring (4) :
          command.startsWith ("SASI ") ? "hd" + command.substring (5) :
          command.startsWith ("SCSI ") ? "sc" + command.substring (5) :
          command.startsWith ("HFS ") ? "hf" + command.substring (4) :
          command);  //Default/STD
      }
    };
    JMenu bootMenuFDD = createMenu ("FDD");
    for (int u = 0; u < FDC.FDC_MAX_UNITS; u++) {
      bootMenuFDD.add (createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0x9070 + (u << 8),
        "FDD " + u, bootListener));
    }
    JMenu bootMenuSASI = createMenu ("SASI");
    for (int u = 0; u < 16; u++) {
      bootMenuSASI.add (createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0x8000 + (u << 8),
        "SASI " + u, bootListener));
    }
    JMenu bootMenuSCSI = createMenu ("SCSI");
    for (int u = 0; u < 8; u++) {
      bootMenuSCSI.add (createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0xa000 && (smrBootROM == SPC.SPC_HANDLE_EX + (u << 2) ||
                                               smrBootROM == SPC.SPC_HANDLE_IN + (u << 2)), "SCSI " + u, bootListener));
    }
    JMenu bootMenuHFS = createMenu ("HFS");
    for (int u = 0; u < HFS.HFS_MAX_UNITS; u++) {
      bootMenuHFS.add (createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0xa000 && smrBootROM == HFS.HFS_BOOT_HANDLE && HFS.hfsBootUnit == u,
        "HFS " + u, bootListener));
    }
    smrBootMenu =
      Multilingual.mlnText (
        createMenu (
          "Boot Device",
          Multilingual.mlnText (createRadioButtonMenuItem (bootGroup, smrBootDevice == -1, "Default", bootListener), "ja", "既定"),
          smrSTDMenuItem = createRadioButtonMenuItem (bootGroup, smrBootDevice == 0x0000, "STD", bootListener),
          bootMenuFDD,
          bootMenuSASI,
          bootMenuSCSI,
          bootMenuHFS
          ),
        "ja", "起動デバイス");

  }  //smrInit()

  //sramdata = smrMakeSramData ()
  //  sramdataを作る
  public static String smrMakeSramData () {
    return (MainMemory.mmrRls (0x00ed0000) == 0x82773638 && MainMemory.mmrRls (0x00ed0004) == 0x30303057 ?  //マジックがある
            ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (MainMemory.mmrM8, 0x00ed0000, 0x00ed4000 - 0x00ed0000))
            :  //壊れている
            "");
  }  //smrMakeSramData()

  //smrParseBootDevice ()
  //  起動デバイスの設定を読み取る
  //  既定は"default"を用いる
  public static void smrParseBootDevice (String boot) {
    smrBootDevice = -1;  //起動デバイス
    smrBootROM = -1;  //ROM起動ハンドル
    smrBootRAM = -1;  //RAM起動アドレス
    boot = boot.toLowerCase ();
    if (boot.equals ("std")) {  //std
      smrBootDevice = 0x0000;  //STD 起動
    } else if (boot.startsWith ("fd")) {  //fdN
      int u = fmtParseInt (boot, 2, 0, FDC.FDC_MAX_UNITS - 1, FDC.FDC_MAX_UNITS);  //起動ユニット番号
      if (u < FDC.FDC_MAX_UNITS) {
        smrBootDevice = 0x9070 + (u << 8);  //FDD起動
      }
    } else if (boot.startsWith ("hd")) {  //hdN
      int u = fmtParseInt (boot, 2, 0, 15, 16);  //起動ユニット番号
      if (u < 16) {
        smrBootDevice = 0x8000 + (u << 8);  //SASI起動
      }
    } else if (boot.startsWith ("sc")) {  //scN
      int u = fmtParseInt (boot, 2, 0, 7, 8);
      if (u < 8) {
        smrBootDevice = 0xa000;  //ROM起動
        smrBootROM = SPC.SPC_HANDLE_EX + ((u & 7) << 2);  //仮に拡張SCSI起動にしておく。リセットしたとき拡張SCSIがなければ内蔵SCSIに読み替えられる
      }
    } else if (boot.startsWith ("hf")) {  //hfN
      int u = fmtParseInt (boot, 2, 0, HFS.HFS_MAX_UNITS - 1, HFS.HFS_MAX_UNITS);  //起動ユニット番号
      if (u < HFS.HFS_MAX_UNITS) {
        HFS.hfsBootUnit = u;
        smrBootDevice = 0xa000;  //ROM起動
        smrBootROM = HFS.HFS_BOOT_HANDLE;  //IPL起動ハンドル
      }
    } else if (boot.startsWith ("rom$")) {  //rom$X
      int handle = fmtParseIntRadix (boot, 3, 0, 0x00ffffff, 0x01000000, 16);  //起動ハンドル
      if (handle < 0x01000000) {
        smrBootDevice = 0xa000;  //ROM起動
        smrBootROM = handle;
      }
    } else if (boot.startsWith ("ram$")) {  //ram$X
      int handle = fmtParseIntRadix (boot, 3, 0, 0x00ffffff, 0x01000000, 16);  //起動ハンドル
      if (handle < 0x01000000) {
        smrBootDevice = 0xb000;  //RAM起動
        smrBootRAM = handle;
      }
    }
  }  //smrParseBootDevice(String)

  //smrReset ()
  //  SRAMリセット
  //  ここでROMも上書きする
  //  ROMを初期化してから呼び出すこと
  //  SPC.spcReset()よりも後であること
  public static void smrReset () {
    smrWriteEnableOn = false;

    //SCSI起動が選択されたときSCSIポートの有無でROM起動ハンドルを調整する
    //  リセットされたときROM起動ハンドルが内蔵SCSI起動と拡張SCSI起動のどちらを指していてもどちらかからのSCSI起動とみなす
    //  拡張SCSIポートがあるときは拡張SCSI起動に、内蔵SCSIポートがあるときは内蔵SCSI起動に、どちらもなければSTD起動に変更する
    //  STD起動に変更するときはROM起動ハンドルを拡張SCSI起動にしておく
    //    ROM起動ハンドルのリードでバスエラーが発生するのでROM起動だけを中断させて他の起動デバイスから起動することができる
    //    内蔵SCSI起動では0x00fc00xxにROMがあるためROM起動アドレスにジャンプしてしまい起動できなくなる
    if (smrBootDevice == 0xa000 && ((smrBootROM & -32) == SPC.SPC_HANDLE_EX ||
                                    (smrBootROM & -32) == SPC.SPC_HANDLE_IN)) {  //SCSI起動
      if (SPC.spcSCSIEXOn) {  //拡張SCSIポートがある
        smrBootROM = SPC.SPC_HANDLE_EX | smrBootROM & 31;  //拡張SCSI起動
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) | 0x08);  //拡張フラグをセットする
      } else if (SPC.spcSCSIINOn) {  //内蔵SCSIポートがある
        smrBootROM = SPC.SPC_HANDLE_IN | smrBootROM & 31;  //内蔵SCSI起動
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) & ~0x08);  //拡張フラグをクリアする
      } else {  //SCSIポートがない
        smrBootDevice = 0x0000;  //STD起動
        smrBootROM = SPC.SPC_HANDLE_EX | smrBootROM & 31;  //拡張SCSI起動にしておく
        smrSTDMenuItem.setSelected (true);
      }
    }

    //  設定をSRAMに書き込む
    MainMemory.mmrWl (0x00ed0008, MainMemory.mmrMemorySizeCurrent);
    if (smrBootROM != -1) {
      MainMemory.mmrWl (0x00ed000c, smrBootROM);
    }
    if (smrBootRAM != -1) {
      MainMemory.mmrWl (0x00ed0010, smrBootRAM);
    }
    if (smrBootDevice != -1) {
      MainMemory.mmrWw (0x00ed0018, smrBootDevice);
    }
    if (smrRepeatDelay != -1) {
      MainMemory.mmrWb (0x00ed003a, smrRepeatDelay);
    }
    if (smrRepeatInterval != -1) {
      MainMemory.mmrWb (0x00ed003b, smrRepeatInterval);
    }
    MainMemory.mmrWb (0x00ed005a, SPC.spcSCSIINOn ? 0 : HDC.hdcHDMax);
    if (romSRAMInitialData != 0) {
      MainMemory.mmrWl (romSRAMInitialData + 0x08, MainMemory.mmrMemorySizeCurrent);
      if (smrBootROM != -1) {
        MainMemory.mmrWl (romSRAMInitialData + 0x0c, smrBootROM);
      }
      if (smrBootRAM != -1) {
        MainMemory.mmrWl (romSRAMInitialData + 0x10, smrBootRAM);
      }
      if (smrBootDevice != -1) {
        MainMemory.mmrWw (romSRAMInitialData + 0x18, smrBootDevice);
      }
      if (smrRepeatDelay != -1) {
        MainMemory.mmrWb (romSRAMInitialData + 0x3a, smrRepeatDelay);
      }
      if (smrRepeatInterval != -1) {
        MainMemory.mmrWb (romSRAMInitialData + 0x3b, smrRepeatInterval);
      }
      MainMemory.mmrWb (romSRAMInitialData + 0x5a, SPC.spcSCSIINOn ? 0 : HDC.hdcHDMax);
    }

    //「ここから再起動」
    if (mpuBootDevice == 0xa000 && ((mpuBootAddress & -32) == SPC.SPC_HANDLE_EX ||
                                    (mpuBootAddress & -32) == SPC.SPC_HANDLE_IN)) {  //SCSI起動
      if (SPC.spcSCSIEXOn) {  //拡張SCSI起動
        mpuBootAddress = SPC.SPC_HANDLE_EX | mpuBootAddress & 31;  //拡張SCSI起動
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) | 0x08);  //拡張フラグをセットする
      } else if (SPC.spcSCSIINOn) {  //内蔵SCSI起動
        mpuBootAddress = SPC.SPC_HANDLE_IN | mpuBootAddress & 31;  //内蔵SCSI起動
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) & ~0x08);  //拡張フラグをクリアする
      } else {  //SCSIポートがない
        mpuBootDevice = 0x0000;  //STD起動
        mpuBootAddress = SPC.SPC_HANDLE_EX | mpuBootAddress & 31;  //拡張SCSI起動にしておく
      }
    }
    //  内蔵SCSIのHUMAN.SYSが起動しても_BOOTINFがHFSを指しているとHFSがAドライブになってHFSのCONFIG.SYSから起動してしまう
    //  SRAMを書き換えずに「ここから再起動」を行うにはHuman68kが呼び出す_BOOTINFの結果も偽る必要がある

  }  //smrReset()

  //smrTini ()
  //  SRAMの後始末
  public static void smrTini () {
    //MainMemory.mmrM8[0x00ed001c] = MainMemory.mmrM8[0x00000810];  //キーボードのLEDの状態。0x00000810にあるとは限らない
  }  //smrTini()

  //smrClear ()
  //  SRAMクリア
  public static void smrClear () {
    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog (
      prgIsApplet ? null : frmFrame,
      Multilingual.mlnJapanese ? "SRAM をクリアしますか？" : "Do you want to clear SRAM?",
      Multilingual.mlnJapanese ? "SRAM クリアの確認" : "Confirmation of clearing SRAM",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE)) {
      Arrays.fill (MainMemory.mmrM8, 0x00ed0000, 0x00ed4000, (byte) 0x00);
    }
  }  //smrClear()

  //smrLoad ()
  //  SRAM読み込み
  //  ローカルのみ
  public static void smrLoad () {
    JFileChooser2 fileChooser = new JFileChooser2 (smrLastFile);
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("SRAM")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "SRAM データファイル (SRAM*.*)" :
                "SRAM data files (SRAM*.*)");
      }
    });
    if (fileChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile2 ();
      String name = file.getPath ();
      if (smrLoadData (name)) {  //読み込めた
        smrLastFile = file;
      } else {  //読み込めなかった
        JOptionPane.showMessageDialog (null,
                                       Multilingual.mlnJapanese ?
                                       name + " は SRAM のデータではないか、壊れています" :
                                       name + " is not SRAM data or it's broken");
        return;
      }
    }
  }  //smrLoad()

  //success = smrLoadData (name)
  //  SRAMのイメージファイルを読み込む
  public static boolean smrLoadData (String name) {
    byte[] bb = new byte[0x00ed4000 - 0x00ed0000];
    if (ismLoad (bb, 0, 0x00ed4000 - 0x00ed0000, name)) {  //読み込む
      if (bb.length == 0x00ed4000 - 0x00ed0000 &&  //サイズが合っている
          ByteArray.byaRls (bb, 0) == 0x82773638 && ByteArray.byaRls (bb, 4) == 0x30303057) {  //マジックがある
        System.arraycopy (bb, 0, MainMemory.mmrM8, 0x00ed0000, 0x00ed4000 - 0x00ed0000);  //SRAMにコピーする
        if (prgIsLocal) {  //ローカルのとき
          smrLastFile = new File (name);
        }
        return true;
      }
      //壊れている
      prgMessage (Multilingual.mlnJapanese ?
                  name + " は SRAM のデータではないか、壊れています" :
                  name + " is not SRAM data or it's broken");
    }
    //読み込めなかった
    return false;
  }

  //smrSave ()
  //  SRAM書き出し
  //  ローカルのみ
  public static void smrSave () {
    JFileChooser2 fileChooser = new JFileChooser2 (smrLastFile);
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("SRAM")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "SRAM データファイル (SRAM*.*)" :
                "SRAM data files (SRAM*.*)");
      }
    });
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile2 ();
      String path = file.getPath ();
      if (ismSave (MainMemory.mmrM8, 0x00ed0000, (long) (0x00ed4000 - 0x00ed0000), path, true)) {  //書き出せた
        smrLastFile = file;
      }
    }
  }  //smrSave()



  //========================================================================================
  //$$ROM ROM
  //
  //  ROMマップ
  //    0x00f00000  0x00f40000  CG1  CGROM1
  //    0x00f40000  0x00fc0000  CG2  CGROM2
  //    0x00fc0000  0x00fe0000       ROM30
  //    0x00fe0000  0x01000000       IPLROM
  //
  //  フォントマップ
  //       開始        終了     種類  サイズ  文字数         備考
  //    0x00f00000  0x00f388c0   KNJ   16x16   7238   1区～8区,16区～84区
  //    0x00f388c0  0x00f3a000                        空き(0x00で充填)
  //    0x00f3a000  0x00f3a800   ANK    8x8     256
  //    0x00f3a800  0x00f3b800   ANK    8x16    256
  //    0x00f3b800  0x00f3d000   ANK   12x12    256
  //    0x00f3d000  0x00f40000   ANK   12x24    256
  //    0x00f40000  0x00fbf3b0   KNJ   24x24   7238   1区～8区,16区～84区
  //    0x00fbf3b0  0x00fbf400                        空き(0x00で充填)
  //    0x00fbf400  0x00fc0000   ANK    6x12    256   IPLROM1.3
  //    0x00ffd018  0x00ffdc00   ANK    6x12    254   IPLROM1.0
  //    0x00ffd344  0x00ffdf2c   ANK    6x12    254   IPLROM1.1
  //    0x00ffd45e  0x00ffe046   ANK    6x12    254   IPLROM1.2
  //
  //  フォントファイル
  //    ファイル名または拡張子とファイルサイズで種類を見分ける
  //    IOCS.XはANK8x16のファイルとして2048バイト(128文字)と4096バイト(256文字)のどちらでも受け付けるが、ここでは256文字のみとする
  //
  //    ファイル名   ANK    KNJ     ANK256文字      ANK256文字+KNJ8836文字
  //       *.F8      4x8    8x8                    72736=1*8*256+1*8*94*94
  //       *.F12     6x12  12x12   3072=1*12*256  215136=1*12*256+2*12*94*94
  //       *.FON     8x16  16x16   4096=1*16*256  286848=1*16*256+2*16*94*94
  //       *.F24    12x24  24x24  12288=2*24*256  648480=2*24*256+3*24*94*94
  //     CGROM*.*                                 786432
  //
  //  ROMの構築手順
  //    1  ROM(0x00f00000～0x01000000;1MB)
  //       1.1  -romで指定されたROMのイメージファイル(1MB)を読み込む
  //                ROM.DAT(1MB)  実機で作成したもの
  //    2  IPLROM(0x00fe0000～0x01000000;128KB)
  //       2.1  -iplromで指定されたIPLROMのイメージファイル(128KB)を読み込む
  //                IPLROM.DAT(128KB)    IPLROM1.0
  //                IPLROMXV.DAT(128KB)  IPLROM1.1
  //                IPLROMCO.DAT(128KB)  IPLROM1.2
  //                IPLROM30.DAT(128KB)  IPLROM1.3
  //            -romが指定されていても-iplromが指定されたときは指定されたものを読み込む
  //            -romと-iplromのどちらも指定されなかったかされても読み込めなかったときはリソースからIPLROMXV.DATを読み込む
  //       2.2  IPLROMのバージョンを確認してパッチをあてる
  //    3  ROM30(0x00fc0000～0x00fe0000;128KB)
  //       3.1  -rom30で指定されたX68030のROMの拡張部分のイメージファイル(128KB)を読み込む
  //                ROM30.DAT(128KB)  実機で作成したもの
  //            -romが指定されていても-rom30が指定されたときは指定されたものを読み込む
  //       3.2  -scsiinromで指定されたSCSIINROMのイメージファイル(8KB)を読み込む
  //                SCSIINROM.DAT(8KB)  実機で作成したもの
  //            -romが指定されていても-scsiinromが指定されたときは指定されたものを読み込む
  //    4  CGROM(0x00f00000～0x00fc0000;768KB)
  //       4.1  -cgromで指定されたフォントファイルを読み込む
  //                CGROM_XEiJ.DAT(768KB)  以下を組み合わせたもの
  //                  MISAKI_G.F8  門真なむ氏の美咲ゴシック
  //                  GOX80.F12  平木敬太郎氏の小伝馬町12
  //                  GOL80.FON  平木敬太郎氏の小伝馬町16
  //                  MIN.F24  平木敬太郎氏の日比谷24
  //                CGROM.DAT(768KB)  実機で作成したもの
  //       4.2  ANK8x8がないときKNJ8x8からコピーして作る
  //       4.3  ANK12x12がないときKNJ12x12からコピーしてを作る
  //       4.4  ANK8x8がないときANK8x16を縮小して作る
  //       4.5  IPLROM1.0～IPLROM1.2のとき
  //            ANK6x12があるときIPLROMに上書きする
  //            ANK6x12がないときIPLROMにあればIPLROMから持ってくる
  //       4.6  足りないフォントを作る準備をする
  //    5  CGROM1(0x00f00000～0x00f40000;256KB)
  //       5.1  0x00f00000～0x00f40000が最初にアクセスされたとき
  //            KNJ16x16がないとき描画して作る
  //            ANK8x8がないとき描画して作る
  //            ANK8x16がないとき描画して作る
  //            ANK12x12がないとき描画して作る
  //            ANK12x24がないとき描画して作る
  //    6  CGROM2(0x00f40000～0x00fc0000;512KB)
  //       6.1  0x00f40000～0x00fc0000が最初にアクセスされたとき
  //            KNJ24x24がないとき描画して作る
  //            ANK6x12がないとき描画して作る

  //スイッチ
  public static final boolean ROM_CREATE_CGROM_LATER = true;  //足りないフォントは後で必要になったときに作る

  //等幅フォントファミリ
  public static final String[] ROM_GOTHIC_FAMILIES = {  //ゴシック
    "ＭＳ ゴシック", "MS Gothic",  //Windows
    "ヒラギノ角ゴ ProN W3", "Hiragino Kaku Gothic ProN",  //Mac
    "ヒラギノ角ゴ Pro W3", "Hiragino Kaku Gothic Pro",
    "Osaka－等幅", "Osaka-Mono",
    "VL ゴシック", "VL Gothic",  //Linux
    "Takaoゴシック", "TakaoGothic",
    "IPAゴシック", "IPAGothic",
  };
  public static final String[] ROM_MINCHO_FAMILIES = {  //明朝
    "ＭＳ 明朝", "MS Mincho",  //Windows
    "ヒラギノ明朝 ProN W3", "Hiragino Mincho ProN",  //Mac
    "ヒラギノ明朝 Pro W3", "Hiragino Mincho Pro",
    "さざなみ明朝", "Sazanami Mincho",  //Linux
    "Takao明朝", "TakaoMincho",
    "IPA明朝", "IPAMincho",
  };

  //文字セット

  //半角ANK
  //! 未対応。コントロールコードのフォントが入っていない
  //  0x1c～0x1fの矢印は\uffeb\uffe9\uffea\uffecだがビットマップフォントがないと綺麗に描けない
  //  0x5cは円マークなので\u00a5
  public static final String[] ROM_ANK_HALFWIDTH_BASE = {
    //123456789abcdef
    "                ",  //0
    "                ",  //1
    " !\"#$%&'()*+,-./",  //2
    "0123456789:;<=>?",  //3
    "@ABCDEFGHIJKLMNO",  //4
    "PQRSTUVWXYZ[\u00a5]^_",  //5
    "`abcdefghijklmno",  //6
    "pqrstuvwxyz{|}  ",  //7
    " \u007e\u00a6             ",  //8
    "                ",  //9
    " ｡｢｣､･ｦｧｨｩｪｫｬｭｮｯ",  //a
    "ｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ",  //b
    "ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ",  //c
    "ﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ",  //d
    "                ",  //e
    "                ",  //f
  };

  //半角ひらがな
  //  全角ひらがなを潰して半角ひらがなを作る
  //  OVERLINEは半角フォントが極端に短いことがあるので全角フォントを使う
  //  REVERSE SOLIDUSは日本語環境だと円マークになってしまうので全角フォントを使う。でもあまり綺麗に表示できないので斜線を描いたほうが良いかも
  public static final String[] ROM_ANK_HIRAGANA_BASE = {
    //0 1 2 3 4 5 6 7 8 9 a b c d e f
    "　　　　　　　　　　　　　　　　",  //0
    "　　　　　　　　　　　　　　　　",  //1
    "　　　　　　　　　　　　　　　　",  //2
    "　　　　　　　　　　　　　　　　",  //3
    "　　　　　　　　　　　　　　　　",  //4
    "　　　　　　　　　　　　　　　　",  //5
    "　　　　　　　　　　　　　　　　",  //6
    "　　　　　　　　　　　　　　￣　",  //7
    "＼　　　　　をぁぃぅぇぉゃゅょっ",  //8
    "　あいうえおかきくけこさしすせそ",  //9
    "　　　　　　　　　　　　　　　　",  //a
    "　　　　　　　　　　　　　　　　",  //b
    "　　　　　　　　　　　　　　　　",  //c
    "　　　　　　　　　　　　　　　　",  //d
    "たちつてとなにぬねのはひふへほま",  //e
    "みむめもやゆよらりるれろわん　　",  //f
  };

  //全角ANK
  public static final String[] ROM_ANK_FULLWIDTH_BASE = {
    //0 1 2 3 4 5 6 7 8 9 a b c d e f
    "　　　　　　　　　　　　　　　　",  //0
    "　　　　　　　　　　　　　　　　",  //1
    "　！”＃＄％＆’（）＊＋，－．／",  //2
    "０１２３４５６７８９：；＜＝＞？",  //3
    "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ",  //4
    "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿",  //5
    "｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ",  //6
    "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝￣　",  //7
    "＼～￤　　　をぁぃぅぇぉゃゅょっ",  //8
    "　あいうえおかきくけこさしすせそ",  //9
    "　。「」、・ヲァィゥェォャュョッ",  //a
    "ーアイウエオカキクケコサシスセソ",  //b
    "タチツテトナニヌネノハヒフヘホマ",  //c
    "ミムメモヤユヨラリルレロワン゛゜",  //d
    "たちつてとなにぬねのはひふへほま",  //e
    "みむめもやゆよらりるれろわん　　",  //f
  };

  //パラメータ
  public static String romROMName;  //-rom ROMのイメージファイル(1MB)
  public static String romIPLROMName;  //-iplrom IPLROMのイメージファイル(128KB)
  public static boolean romROMDBOn;  //-romdb IPLでROMデバッガを強制的に起動する(on/off)
  public static String romSCSIINROMName;  //-scsiinrom SCSIINROMのイメージファイル(8KB)
  public static String romROM30Name;  //-rom30 X68030のROMの拡張部分のイメージファイル(128KB)
  public static String romCGROMName;  //-cgrom CGROMのイメージファイル(768KB)
  public static boolean romOmusubiOn;  //-omusubi おむすびフォント

  public static int romCGROMRequired;

  //使用するフォント
  public static Font romGothic8Font;
  public static Font romGothic12Font;
  public static Font romGothic16Font;
  public static Font romMincho24Font;

  public static int romIPLVersion;             //IPLROMのバージョン。0=1.0,1=1.1,2=1.2,3=1.3,5=1.5,-1=不明。5になるのは1.3にパッチを当てたときだけ

  public static int romDebuggerVersion;        //ROMデバッガのバージョン。0x0100=1.0,0x0232=2.32,-1=不明
  public static int romHumanVersion;           //ROM Humanのバージョン。0x0100=1.00,0x0215=2.15,-1=不明

  public static int romSRAMInitialData;        //SRAM初期化データの先頭。0=不明
  public static int romBootDeviceRead;         //起動シーケンスで起動デバイスを読み出すmove.w $00ED0018,d1の先頭
  public static int romBootROMAddressRead;     //起動シーケンスでROM起動ハンドルを読み出すmovea.l $00ED000C,a0の先頭
  public static int romBootRAMAddressRead;     //起動シーケンスでRAM起動アドレスを読み出すmovea.l $00ED0010,a0の先頭
  public static int romBootinfROMAddressRead;  //IOCS _BOOTINFでROM起動ハンドルを読み出すmove.l $00ED000C,d0の先頭
  public static int romBootinfRAMAddressRead;  //IOCS _BOOTINFでRAM起動アドレスを読み出すmove.l $00ED0010,d0の先頭
  public static int romANK6X12;                //IPLROMにあるANK 6x12フォントの先頭。0=なし

  public static boolean romROM30Required;  //true=rom30を読み込んでいない
  public static byte[] romROM30Data;  //-romまたは-rom30で読み込まれた0x00fc0000～0x00fe0000のデータ

  public static boolean romSCSIINROMRequired;  //true=scsiinromを読み込んでいない
  public static byte[] romSCSIINROMData;  //-romまたは-scsiinromで読み込まれた0x00fc0000～0x00fc2000のデータ

  //romInit ()
  //  ROMを初期化する
  public static void romInit () {

    romCGROMRequired = FNT_MASK_CGROM;
    boolean iplromRequired = true;

    romGothic8Font = null;
    romGothic12Font = null;
    romGothic16Font = null;
    romMincho24Font = null;

    romROM30Required = true;
    romROM30Data = null;

    romSCSIINROMRequired = true;
    romSCSIINROMData = null;

    //-romで指定されたROMのイメージファイル(1MB)を読み込む
    if (ismLoad (MainMemory.mmrM8, 0x00f00000, 0x01000000 - 0x00f00000, romROMName)) {
      romCGROMRequired = MainMemory.mmrM8[FNT_ADDRESS_ANK6X12 + 12 * 'A' + 6] != 0 ? 0 : FNT_MASK_ANK6X12;  //ANK6x12の有無を確認する
      if (MainMemory.mmrRls (0x00fc021c + 0x003e) == ('2' << 24 | '.' << 16 | '1' << 8 | '5')) {  //Human68k version 2.15があるのでrom30は読み込み済み
        romROM30Data = new byte[1024 * 128];
        System.arraycopy (MainMemory.mmrM8, 0x00fc0000, romROM30Data, 0, 1024 * 128);
        romROM30Required = false;
      }
      if (MainMemory.mmrRls (0x00fc0024) == ('S' << 24 | 'C' << 16 | 'S' << 8 | 'I') &&
          MainMemory.mmrRwz (0x00fc0028) == ('I' << 8 | 'N') &&
          (MainMemory.mmrRls (0x00fc0000) & -0x2000) == 0x00fc0000) {  //SCSIINフラグがあってROM起動ハンドルがscsiinromを指しているのでscsiinromは読み込み済み
        romSCSIINROMData = new byte[8192];
        System.arraycopy (MainMemory.mmrM8, 0x00fc0000, romSCSIINROMData, 0, 8192);
        romSCSIINROMRequired = false;
      }
      iplromRequired = false;
    } else {
      romROMName = "";
    }

    //IPLROMのイメージファイル(128KB)を読み込む
    //  -romが指定されていても-iplromが指定されたときは指定されたものを読み込む
    if (romIPLROMName.length () > 0) {
      if (ismLoad (MainMemory.mmrM8, 0x00fe0000, 0x01000000 - 0x00fe0000, romIPLROMName)) {
        iplromRequired = false;
      } else {
        romIPLROMName = "";
      }
    }
    //  -romと-iplromのどちらも指定されなかったかされても読み込めなかったときはリソースからIPLROM*.DATを読み込む
    if (iplromRequired) {
      byte[] rr = ismGetResource (mdlIPLROMRequest == 0 ? "IPLROM.DAT" :
                                  mdlIPLROMRequest == 1 ? "IPLROMXV.DAT" :
                                  mdlIPLROMRequest == 2 ? "IPLROMCO.DAT" : "IPLROM30.DAT");
      if (rr != null) {
        System.arraycopy (rr, 0, MainMemory.mmrM8, 0x00fe0000, 0x01000000 - 0x00fe0000);
        iplromRequired = false;
      }
    }

    //IPLROMのバージョンを確認する
    romCheck ();
    //IPLROMにパッチをあてる
    romPatch ();

    //-rom30で指定されたX68030のROMの拡張部分のイメージファイル(128KB)を読み込む
    //  -romが指定されていても-rom30が指定されたときは指定されたものを読み込む
    if (romROM30Name.length () > 0) {  //-rom30が指定された
      byte[] bb = new byte[1024 * 128];
      if (ismLoad (bb, 0, 1024 * 128, romROM30Name)) {
        romROM30Data = bb;
        romROM30Required = false;
        if (romIPLVersion >= 3) {
          System.arraycopy (romROM30Data, 0, MainMemory.mmrM8, 0x00fc0000, 1024 * 128);
        }
      } else {
        romROM30Name = "";
      }
    }

    //-scsiinromで指定されたSCSIINROMのイメージファイル(8KB)を読み込む
    //  -romが指定されていても-scsiinromが指定されたときは指定されたものを読み込む
    if (romSCSIINROMName.length () > 0) {  //-scsiinromが指定された
      byte[] bb = new byte[8192];
      if (ismLoad (bb, 0, 8192, romSCSIINROMName)) {
        romSCSIINROMData = bb;
        romSCSIINROMRequired = false;
        if (romIPLVersion < 3) {
          System.arraycopy (romSCSIINROMData, 0, MainMemory.mmrM8, 0x00fc0000, 8192);
        }
      } else {
        romSCSIINROMName = "";
      }
    }

    romCheck2 ();  //ROMデバッガとROM Humanのバージョンを確認する
    romPatch2 ();  //ROMデバッガにパッチをあてる
    romPatch3 ();  //ROM Humanにパッチをあてる

    //-cgromで指定されたファイルを読み込む
    if (romCGROMRequired != 0) {
      romCGROMRequired = fntLoadCGROM (romCGROMName, FNT_MASK_CGROM);
    }

    //KNJ8X8からANK8X8を作る
    if ((romCGROMRequired & (FNT_MASK_ANK8X8 | FNT_MASK_KNJ8X8)) == FNT_MASK_ANK8X8) {
      fntConvertKNJ8X8toANK8X8 ();
      romCGROMRequired &= ~FNT_MASK_ANK8X8;
    }

    //KNJ12X12からANK12X12を作る
    if ((romCGROMRequired & (FNT_MASK_ANK12X12 | FNT_MASK_KNJ12X12)) == FNT_MASK_ANK12X12) {
      fntConvertKNJ12X12toANK12X12 ();
      romCGROMRequired &= ~FNT_MASK_ANK12X12;
    }

    //ANK8X16を縮小してANK8X8を作る
    if ((romCGROMRequired & (FNT_MASK_ANK8X8 | FNT_MASK_ANK8X16)) == FNT_MASK_ANK8X8) {
      fntConvertANK8X16toANK8X8 ();
      romCGROMRequired &= ~FNT_MASK_ANK8X8;
    }

    //IPLROMにANK6X12があるときCGROM優先でコピーする
    if (romANK6X12 != 0) {
      if ((romCGROMRequired & FNT_MASK_ANK6X12) == 0) {  //CGROMにある
        System.arraycopy (MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, MainMemory.mmrM8, romANK6X12, 1 * 12 * 254);  //CGROMからIPLROMへ254文字コピー
      } else {  //CGROMにない
        System.arraycopy (MainMemory.mmrM8, romANK6X12, MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, 1 * 12 * 254);  //IPLROMからCGROMへ254文字コピー
        Arrays.fill (MainMemory.mmrM8, FNT_ADDRESS_ANK6X12 + 1 * 12 * 254, FNT_ADDRESS_ANK6X12 + 1 * 12 * 256, (byte) 0);  //255文字目と256文字目はクリア
        romCGROMRequired &= ~FNT_MASK_ANK6X12;
      }
    }

    //おむすびフォント
    if (romOmusubiOn) {
      System.arraycopy (ROM_OMUSUBI, 0, MainMemory.mmrM8, FNT_ADDRESS_ANK8X16, 1 * 16 * 256);
      romCGROMRequired &= ~FNT_MASK_ANK8X16;
    }

    //足りないフォントを作る準備をする
    if (romCGROMRequired != 0) {
      //フォントファミリの一覧を用意する
      String[] availableFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment ().getAvailableFontFamilyNames ();
      Arrays.sort (availableFamilies, null);
      //ゴシック
      if ((romCGROMRequired & (FNT_MASK_ANK8X8 | FNT_MASK_ANK6X12 | FNT_MASK_ANK12X12 | FNT_MASK_ANK8X16 | FNT_MASK_KNJ16X16)) != 0) {
        String gothicFamily = "Monospaced";
        //使えそうなフォントを探す
        for (String family : ROM_GOTHIC_FAMILIES) {
          if (Arrays.binarySearch (availableFamilies, family, null) >= 0) {
            gothicFamily = family;
            break;
          }
        }
        prgMessage ((Multilingual.mlnJapanese ? "ゴシック体のフォント: " : "Gothic font: ") + gothicFamily);
        //8ドット
        if ((romCGROMRequired & FNT_MASK_ANK8X8) != 0) {
          romGothic8Font = new Font (gothicFamily, Font.PLAIN, 8);
        }
        //12ドット
        if ((romCGROMRequired & (FNT_MASK_ANK6X12 | FNT_MASK_ANK12X12)) != 0) {
          romGothic12Font = new Font (gothicFamily, Font.PLAIN, 12);
        }
        //16ドット
        if ((romCGROMRequired & (FNT_MASK_ANK8X16 | FNT_MASK_KNJ16X16)) != 0) {
          romGothic16Font = new Font (gothicFamily, Font.PLAIN, 16);
        }
      }
      //明朝
      if ((romCGROMRequired & (FNT_MASK_ANK12X24 | FNT_MASK_KNJ24X24)) != 0) {
        String minchoFamily = "Monospaced";
        //使えそうなフォントを探す
        for (String family : ROM_MINCHO_FAMILIES) {
          if (Arrays.binarySearch (availableFamilies, family, null) >= 0) {
            minchoFamily = family;
            break;
          }
        }
        prgMessage ((Multilingual.mlnJapanese ? "明朝体のフォント: " : "Mincho font: ") + minchoFamily);
        //24ドット
        romMincho24Font = new Font (minchoFamily, Font.PLAIN, 24);
      }
      if (!ROM_CREATE_CGROM_LATER) {
        romCreateCG1 ();
        romCreateCG2 ();
      }
    }

  }  //romInit()

  //romCheck ()
  //  IPLROMのバージョンを確認する
  public static void romCheck () {

    //IPLROMのバージョンを確認する
    //  0  1.0  0x00ff000a  0x10870507  初代,ACE,EXPERT,PRO,SUPER
    //  1  1.1  0x00ff000a  0x11910111  XVI
    //  2  1.2  0x00ff000a  0x12911024  XVICompact
    //  3  1.3  0x00ff0032  0x13921127  030
    romIPLVersion = (MainMemory.mmrRls (0x00ff000a) == 0x10870507 ? 0 :
                     MainMemory.mmrRls (0x00ff000a) == 0x11910111 ? 1 :
                     MainMemory.mmrRls (0x00ff000a) == 0x12911024 ? 2 :
                     MainMemory.mmrRls (0x00ff0032) == 0x13921127 ? 3 :
                     (MainMemory.mmrRls (0x00ff0032) & 0xfff00000) == 0x15100000 ? 5 :  //0x159?????と0x150?????は不可
                     -1);
    if (mdlModel == MDL_EXPERT ||
        mdlModel == MDL_SUPER    ? romIPLVersion != 0 :
        mdlModel == MDL_XVI      ? romIPLVersion != 1 :
        mdlModel == MDL_COMPACT  ? romIPLVersion != 2 :
        mdlModel == MDL_HYBRID ||
        mdlModel == MDL_X68030 ||
        mdlModel == MDL_060TURBO ? romIPLVersion < 3 :  //読み込んだIPLROMのバージョンが要求と異なる
        false) {
      //初回は-iplromで指定されたIPLROMによって機種を選択せざるを得ない場合があるのでここで切り替える
      //  -iplromは使わず-modelで機種を選択することを推奨する
      switch (romIPLVersion) {
      case 0:  //IPLROM1.0
        if (!mdlSCSIINRequest) {
          mdlRequestModel (MDL_EXPERT, false);
        } else {
          mdlRequestModel (MDL_SUPER, false);
        }
        break;
      case 1:  //IPLROM1.1
        mdlRequestModel (MDL_XVI, false);
        break;
      case 2:  //IPLROM1.2
        mdlRequestModel (MDL_COMPACT, false);
        break;
      case 3:  //IPLROM1.3
      case 5:  //IPLROM1.5
        if (mdlCoreRequest == 0) {
          mdlRequestModel (MDL_HYBRID, false);
        } else if (mdlCoreRequest == 3) {
          mdlRequestModel (MDL_X68030, false);
        } else {
          mdlRequestModel (MDL_060TURBO, false);
        }
        break;
      }
    }

    //IPLROMの中のアドレス
    switch (romIPLVersion) {
    case 0:  //IPLROM1.0
      romSRAMInitialData       = 0x00ff0792;  //SRAM初期化データの先頭
      romBootDeviceRead        = 0x00ff0144;  //起動シーケンスで起動デバイスを読み出すmove.w $00ED0018,d1の先頭
      romBootROMAddressRead    = 0x00ff0236;  //起動シーケンスでROM起動ハンドルを読み出すmovea.l $00ED000C,a0の先頭
      romBootRAMAddressRead    = 0x00ff026e;  //起動シーケンスでRAM起動アドレスを読み出すmovea.l $00ED0010,a0の先頭
      romBootinfROMAddressRead = 0x00ffaa88;  //IOCS _BOOTINFでROM起動ハンドルを読み出すmove.l $00ED000C,d0の先頭
      romBootinfRAMAddressRead = 0x00ffaa96;  //IOCS _BOOTINFでRAM起動アドレスを読み出すmove.l $00ED0010,d0の先頭
      romANK6X12               = 0x00ffd018;  //IPLROMにあるANK 6x12フォントの先頭
      break;
    case 1:  //IPLROM1.1
      romSRAMInitialData       = 0x00ff0842;  //SRAM初期化データの先頭
      romBootDeviceRead        = 0x00ff01e6;  //起動シーケンスで起動デバイスを読み出すmove.w $00ED0018,d1の先頭
      romBootROMAddressRead    = 0x00ff02d8;  //起動シーケンスでROM起動ハンドルを読み出すmovea.l $00ED000C,a0の先頭
      romBootRAMAddressRead    = 0x00ff0310;  //起動シーケンスでRAM起動アドレスを読み出すmovea.l $00ED0010,a0の先頭
      romBootinfROMAddressRead = 0x00ffadb4;  //IOCS _BOOTINFでROM起動ハンドルを読み出すmove.l $00ED000C,d0の先頭
      romBootinfRAMAddressRead = 0x00ffadc2;  //IOCS _BOOTINFでRAM起動アドレスを読み出すmove.l $00ED0010,d0の先頭
      romANK6X12               = 0x00ffd344;  //IPLROMにあるANK 6x12フォントの先頭
      break;
    case 2:  //IPLROM1.2
      romSRAMInitialData       = 0x00ff08ee;  //SRAM初期化データの先頭
      romBootDeviceRead        = 0x00ff0236;  //起動シーケンスで起動デバイスを読み出すmove.w $00ED0018,d1の先頭
      romBootROMAddressRead    = 0x00ff032c;  //起動シーケンスでROM起動ハンドルを読み出すmovea.l $00ED000C,a0の先頭
      romBootRAMAddressRead    = 0x00ff0368;  //起動シーケンスでRAM起動アドレスを読み出すmovea.l $00ED0010,a0の先頭
      romBootinfROMAddressRead = 0x00ffaece;  //IOCS _BOOTINFでROM起動ハンドルを読み出すmove.l $00ED000C,d0の先頭
      romBootinfRAMAddressRead = 0x00ffaedc;  //IOCS _BOOTINFでRAM起動アドレスを読み出すmove.l $00ED0010,d0の先頭
      romANK6X12               = 0x00ffd45e;  //IPLROMにあるANK 6x12フォントの先頭
      break;
    case 3:  //IPLROM1.3
    case 5:  //IPLROM1.5
      romSRAMInitialData       = 0x00ff09e8;  //SRAM初期化データの先頭
      romBootDeviceRead        = 0x00ff0386;  //起動シーケンスで起動デバイスを読み出すmove.w $00ED0018,d1の先頭
      romBootROMAddressRead    = 0x00ff045e;  //起動シーケンスでROM起動ハンドルを読み出すmovea.l $00ED000C,a0の先頭
      romBootRAMAddressRead    = 0x00ff048e;  //起動シーケンスでRAM起動アドレスを読み出すmovea.l $00ED0010,a0の先頭
      romBootinfROMAddressRead = 0x00ffa9f0;  //IOCS _BOOTINFでROM起動ハンドルを読み出すmove.l $00ED000C,d0の先頭
      romBootinfRAMAddressRead = 0x00ffa9fe;  //IOCS _BOOTINFでRAM起動アドレスを読み出すmove.l $00ED0010,d0の先頭
      romANK6X12               = 0;  //IPLROM1.3にはANK 6x12フォントがない
      break;
    default:  //不明
      romSRAMInitialData       = 0;
      romBootDeviceRead        = 0;
      romBootROMAddressRead    = 0;
      romBootRAMAddressRead    = 0;
      romBootinfROMAddressRead = 0;
      romBootinfRAMAddressRead = 0;
      romANK6X12               = 0;
    }

  }  //romCheck()

  //romCheck2 ()
  //  ROMデバッガとROM Humanのバージョンを確認する
  public static void romCheck2 () {
    romDebuggerVersion = (MainMemory.mmrRls (0x00fe00d6 + 20) == ('1' << 24 | '.' << 16 | '0' << 8 | 0x0d) ? 0x0100 :  //3文字
                          MainMemory.mmrRls (0x00fd3a12 + 24) == ('2' << 24 | '.' << 16 | '3' << 8 | '2') ? 0x0232 :
                          -1);
    romHumanVersion = (MainMemory.mmrRls (0x00fe5000 + 0x6fc0 - 0x6800 + 37) == ('1' << 24 | '.' << 16 | '0' << 8 | '0') ? 0x0100 :  //奇数アドレス
                       MainMemory.mmrRls (0x00fc025a) == ('2' << 24 | '.' << 16 | '1' << 8 | '5') ? 0x0215 :
                       -1);
  }  //romCheck2()

  //  perl misc/rompattobytes.pl ROM_PATCH13 misc/rompat13.x
  public static final int ROM_PATCH13_BASE = 0x00fff000;
  public static final byte[] ROM_PATCH13_TEXT = "C\372\1N!\311\0\20!\311\0,\"OA\372\1\30~\0p\0N{\b\1~\1p\1\300;\2\373g\0\1\4A\372\0004\6\372\0\0\0\32~\2p\bN{\0\2A\372\0\n\362\200\0\0\b\307\0\17`\0\0\342\0\0\0\0\0\377\360X\0\0\0\0\0\0\0\0\360\0\6\317.IA\372\0@Nz\b\2~\3 <\0\0\b\bN{\0\2/<\0\3304@\360\27@\0.\274\0\0\0\0\360\27\b\0\360\27\f\0X\217A\372\0\n\362\200\0\0\b\307\0\17a\0\0\272`\0\0\212A\372\0:Nz\b\5~\4p\0N{\0\2\364\330p\0N{\0\3p\0N{\0\4N{\0\5N{\0\6N{\0\7A\372\0\n\362\200\0\0\b\307\0\17a\0\0\352`LA\372\0HNz\30\b~\6 <\0\200\0\0N{\0\2\364\330 <\0\0\2\20N{\0\3p\0N{\0\4N{\0\5N{\0\6N{\0\7p\1N{\b\bA\372\0\n\362\200\0\0\b\307\0\17a\0\0\240`\2~\0\221\310C\371\0\377\7pr\1\340\231p\177 \311\323\301 \311\323\301Q\310\377\366!\374\0\377\362V\0\364N\371\0\377\0~.IN\320O\357\377\200 O \374\177\377\0\2 \17\350\210T\200\351\210 \200A\370\0\1\"@p\7\"\310\321\374\0 \0\0Q\310\377\366#|\0\360\0\1\377\344\360\0$\0\360\27L\0/<\200\3304@\360\27@\0C\371\0\360\0\0A\371\0 \0\0000<\7\377\261\211V\310\377\374f\4\b\307\0\37\b\227\0\7\360\27@\0O\357\0\204NuC\370 \0A\351\2\2p\177\"\310Q\310\377\374A\351\2\2p\177\"\310Q\310\377\374p\37\"\374\0\377\4AQ\310\377\370\"|\0\177\300@N{\220\4N{\220\6N{\220\5N{\220\7C\370 \0N{\230\7N{\230\6\"|\0\0\300\0N{\220\3\365\30\364\330A\371\200\377\0\0C\371\200\360\0\0000<\7\377\263\210V\310\377\374f\4\b\307\0\37\"|\0\0\2\0N{\220\3\223\311\365\30N{\220\4N{\220\6N{\220\5N{\220\7NuHo\0\bH\347\377\376t\5\b/\0\5\0@f\bt\1Nh/H\0< o\0B2\0300\1\300|\3618\260|\1\bg\22L\327\0\7 o\0 O\357\0@N\371\0\377\7pp\7\300A\"w\4 \322\330/H\0B0\1\340HA\367\2\376\322\1eDNz\20\0N{ \0k\22\16\21\0\0\341H\16)\0\0\0\0021@\0\2`\36\16\21\0\0\341\210\16)\0\0\0\2\341\210\16)\0\0\0\4\341\210\16)\0\0\0\6 \200N{\20\0L\327\0\377`FNz\20\1N{ \1k\0240(\0\2\341X\16\21\b\0\341X\16)\b\0\0\2`  \20\341\230\16\21\b\0\341\230\16)\b\0\0\2\341\230\16)\b\0\0\4\341\230\16)\b\0\0\6N{\20\1L\327\0\7L\357\3\0\0 O\357\0@J\27j\4\0|\200\0Nsp\0\209\0\350\340\13F\0\351H\350\b1\300\f\266\f8\0\2\f\274e0 <\0\0!\1\f8\0\4\f\274e\30g\22Nz\b\b\b\300\0\0N{\b\b <\0\200\0\0000<\200\0N{\0\2B9\0\350\340\tA\372\1 a<1\300\f\270O\357\377\342 \17\320|\0\16\300|\377\360X@ @!z\1\0\377\374 \272\0\376!z\0\376\0\4!z\0\374\0\ba\16O\357\0\0361\300\f\272N\371\0\377\1\234t\16@\347\0|\7\0C\371\0\350\200\35\37)\377\354\37)\377\370\37\21\2)\0\317\377\354\2)\0\317\377\370Q\321J\21f\374Q\351\0\6Q\351\0\b <\0@\0\0\344\250S\200\22\274\0qN\220Q\321J\21f\374p\0\220)\0\6r\0\222)\0\b\23|\0\310\0\6Q\351\0\b\22\237\23_\377\370\23_\377\354F\337\300\374\0002\260\1c\4\320|\1\0\20\1S@JBg\f\260|\23\210d\6SB`\0\377zC\372\0002r\0\228\f\274\262|\0\6b\4\345I\322\301\"\21\344\251J\200g\24\260\274\0\0\377\377b\f\202\300h\2r\377p\0000\1Nup\0Nu\372\0\0\0\372\0\0\0\372\0\0\0\372\0\0\0}\0\0\0\0\0\0\0)\252\252\253NqNqH@Q\310\377\376H@Q\310\377\366NuH\347\377\376O\357\377\300,Oa\"a\0\0\276a\0\1\210a\0\0046a\0\4\224a\0\4\356a\0\2\34O\357\0@L\337\177\377Nu NC\372\0va\0\6\252p\217NO\"\0B@\222\200H@\351\210\351\211\350H\350I\341\210\341\211\350H\350I\350\b\350\t\200\2740000\202\2740000\341\230\20\300\20\374\0.\341\230\20\300C\372\0Ma\0\6j\341\230\20\300\341\230\20\300\20\374\0-\341\231\20\301\341\231\20\301\20\374\0-\341\231\20\301\341\231\20\301C\372\0*a\0\6B\"Np!NONu\33[1mBIOS ROM version: \0 (20\0)\33[1m\r\n\0r\0p\254NO*\0 NC\372\0\230a\0\6\4t\377rM\272<\0\6f\22Nz\b\b\340Ht\0\24\0\264<\0\6d\2rX\20\301C\372\0\220a\0\5\336\272<\0\3e\36b\b\b\5\0\16f\26`\fJEk\20rL\b\5\0\16f\2rE\20\301\20\374\0C\20\374\0000r0\322\5\20\301\20\374\0000 \2k\n\20\374\0-r\3a\0\5$\20\374\0 \20\374\0( \5B@H@a\0\4\330\20\350\377\377\21|\0.\377\376C\372\0000a\0\5z\"Np!NONuMicro-Processing Unit (MPU): \0C68\0MHz)\r\n\0\0JEj@ NC\372\0>\272<\0\4d\4C\372\0Pa\0\0050C\372\0l\272<\0\4d\30\362\0\\\1\362\'h\0 \37\200\237\200\237C\372\0]g\4C\372\0aa\0\5\n\"Np!NONuFloating-Point Unit (FPU): \0Floating-Point Coprocessor (FPCP): \0on MPU\r\n\0MC68881\r\n\0MC68882\r\n\0\0\272<\0\4e&a\0\0\344k  NC\372\0\202a\0\4\216J\200C\372\0dg\4C\372\0ha\0\4~\"Np!NOp\0a\0\1lk  NC\372\0ta\0\4fJ\200C\372\0<g\4C\372\0@a\0\4V\"Np!NOp\1a\0\1Dk  NC\372\0la\0\4>J\200C\372\0\24g\4C\372\0\30a\0\4.\"Np!NONuMC68881\r\n\0MC68882\r\n\0Motherboard Coprocessor: \0Extension Board Coprocessor 1: \0Extension Board Coprocessor 2: \0H\347x\340p\1\300;\2\373S\200k\0\0\232A\371\0\2 \0p\377@\302\0|\7\0Nz@\1Nz0\0r\7N{\20\1N{\20\0C\372\0f$x\0\b!\311\0\b\"OBA\16h\30\0\0\6\16h\20\0\0\6\16P\20\0\262|\b\2f\3662<\\\1\16h\30\0\0\n\16P\20\0\262|\b\2f\3662<h\0\16h\30\0\0\n\16P\20\0\262|2\ff\366\16\250\0\0\0\20\16\250\20\0\0\20\200\201\16\250\20\0\0\20\200\201g\2p\1.I!\312\0\bN{0\0N{@\1F\302L\337\7\36J\200NuH\347`\340A\371\0\351\340\0J\200g\4A\350\0\200p\377@\302\0|\7\0C\372\0F$x\0\b!\311\0\b\"OBh\0\6Jh\0\6\fP\b\2f\3721|\\\1\0\n\fP\b\2f\3721|h\0\0\n\fP2\ff\372 (\0\20\"(\0\20\200\201\"(\0\20\200\201g\2p\1.I!\312\0\bF\302L\337\7\6J\200Nu\b\5\0\16g( NC\372\0&a\0\2\206\272<\0\3d\nC\372\0007a\0\2x`\bC\372\0007a\0\2n\"Np!NONuMemory Management Unit (MMU): \0MC68851\r\n\0on MPU\r\n\0@\347\0|\7\0/8\0\b*O!\374\0\377\371\232\0\b\225\312 \nJ\22\325\374\0\20\0\0\265\374\0\300\0\0e\360.M!\337\0\bF\337\"\n NC\372\0\22a\0\1\370a\0\0\360\"Np!NONuMotherboard Memory: $\0@\347\0|\7\0/8\0\b*O!\374\0\377\371\372\0\b\225\312C\370\"\0002<\0\377\225\374\1\0\0\0S\211\22\222\24\201.MQ\311\377\360!\374\0\377\372 \0\b\225\312C\370!\0002<\0\377\20\1R\0\225\374\1\0\0\0\23\0\22\222.MQ\311\377\360!\374\0\377\372F\0\b\225\312C\370!\0002<\0\377\225\374\1\0\0\0\223!f\4\24\251\1\0.MQ\311\377\356!\337\0\bF\337\225\312G\370 \1J\23f\b \nf\24$K`\20 \ng\f Na \"Np!NO\225\312R\213\266\374 \377c\334 \ng\n Na\b\"Np!NONu \nH@\341\210\"\13HA\341\211C\372\08a\0\1\6a\0\0\326\20\374\0-\20\374\0$/\0 \1S\200a\0\0\304\20\374\0 \20\374\0( \1\220\237H@\350Ha(C\372\0 a\0\0\326NuDaughterboard Memory: $\0MB)\r\n\0H\347\340@J\200f\6\20\374\0000`$j\6\20\374\0-D\200C\372\0D\260\231e\374Y\211`\nR\1\220\202d\372\320\202\20\301r/$\31f\360B\20L\337\2\7NuH\347\340@t\n\224\201d\2t\0\345JC\373 \22g\336\260\251\377\374e\330 )\377\374S\200`\320;\232\312\0\5\365\341\0\0\230\226\200\0\17B@\0\1\206\240\0\0\'\20\0\0\3\350\0\0\0d\0\0\0\n\0\0\0\1\0\0\0\0H\347\340\0t\7\351\230r\17\302\0\20\373\20\16Q\312\377\364B\20L\337\0\7Nu0123456789ABCDEF\20\331f\374S\210Nup\377\f8\0\2\f\274d\4JAf\24\262|\0\4b\16?\1\322A2;\20\nN\273\20\0062\37Nu\0\n\0>\0h\0x\0\246p\f\f8\0\2\f\274e\2p\6\300\370\f\270\320\274\0\0\0002\200\374\0dH@B@J8\f\276V\300\342XJ8\f\275V\300\342X\208\f\274Nup\0\f8\0\2\f\274e\36Nz\0\2\f8\0\4\f\274d\6\342\230\343\30`\4H@\343X\343\230\300\274\0\0\0\3Nu/\2t\0\249\0\355\0\220a2$\37Nu\f8\0\2\f\274e \f8\0\4\f\274d\32/\0Nz\0\2\200|\b\bN{\0\2\300|\367\367N{\0\2 \37Nu\364\370Nua\226\f8\0\2\f\274eP/\1?\2\f8\0\4\f\274d\26r\1\302B\304|\0\2DB\304|!\0\202BN{\20\2`*Nz\20\2\342J\323A\342Y\342J\323\201\342\231N{\20\0024\27FB\304@g\16UBb\6e\6\364x`\4\364x\364\2304\37\"\37Nu/\2t\0a\236$\37Nu/\2t\1a\224$\37Nu".getBytes (ISO_8859_1);
  public static final byte[] ROM_PATCH13_DATA = "\0\377\0000\0\0\0\6 <\25\26\b\26\0\377\0Z\0\0\0\6N\371\0\377\360\0\0\377\1<\0\0\0\6N\371\0\377\363L\0\377\0036\0\0\0\6p\0N{\0\2\0\377\3\200\0\0\0\6p\0N{\0\2\0\377\16\210\0\0\0\bN\271\0\377\364\304`\n\0\377\20\340\0\0\0\16r\1t\2aRr\1t\0aLr\b\0\377;\374\0\0\0\4H\347p\0\0\377<\26\0\0\0\4L\337\0\16\0\377<\34\0\0\0\4L\337\0\16\0\377<Z\0\0\0\4H\347p\0\0\377<v\0\0\0\4L\337\0\16\0\377DD\0\0\0H?\0/<\0\270\0\20/8\13\374?<\0\2a\0#nBo\0\nRWa\0#dO\357\0\f0\37g\4a\0e\360L\337\177\376NuRA\262|\0\37c\16\262x\trc\b28\trg\2SANu\0\377D\246\0\0\0\2a\314\0\377Z\250\0\0\0\2;\200\0\377j\332\0\0\0>H\347`@$\1HB x\4XN\220 @\260\374\fFg$H@\260|\0\360e\6\260|\1\0e\0260\1\300\302\320A\320B\20\331Q\310\377\374p\0L\337\2\6Nup\377`\366\0\377mp\0\0\0\"p\f\3008\t<\342H0;\0\20N\273\0\f\b\371\0\3\0\350\0(NuFjFjF~F\224\0\377\202\204\0\0\0\6N\371\0\377\374H\0\377\223\242\0\0\0\2v\b\0\377\223\362\0\0\0\2v\5\0\377\225\"\0\0\0\2v\b\0\377\2246\0\0\0\22 )\0\4\4\300z\27\232\200t\0v\4a\0\2V\0\377\225p\0\0\0\22 )\0\4\4\300z\27\232\200t\bv\4a\0\1\34\0\377\226\234\0\0\0\22p\377\353\250F\200\324\200\326\200\352\252\352\253p\365Nu\0\377\253\244\0\0\0\4\262|\4\0\0\377\253\264\0\0\0\4\264|\4\0\0\377\254r\0\0\0\nN\271\0\377\374\334/\0`\n\0\377\254\350\0\0\0\n$\37N\271\0\377\374v`\4\0\377\267@\0\0\0\4\0260\0\2\0\377\307Z\0\0\0\6N\371\0\377\373\254\0\377\314\270\0\0\0\322H\347Pb,z\306~p:\262\200e\2r\377\322A2;\20\22N\273\20\16L\337F\nNup\377Nu\377\372\0010\2\36\1\370\2\354\b\342\b\232\3\314\4\n\4H\4\264\4\310\3\210\3H\4\204\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\377\372\t4\nd\n\276\fN\7\22\7\276\13\34\13\202\f\"\t\254\t\354\0070\tp\r\32\7N\f\264\f\346\n,\f\202\377\372\377\372\377\372\rN\r\220\r\310\16\0\f8\0\6\f\274g\6\1\316\0\27Nu\341\230\35@\0\27\341\230\35@\0\31\341\230\35@\0\33\341\230\35@\0\35Nu\341\213\272<\0\3d\4\353\253`\2\347\213Nu\0\377\317F\0\0\0\4a\0\376\n\0\377\317\226\0\0\0\4a\0\375\272\0\377\321\312\0\0\0\4a\0\373\206\0\377\322\214\0\0\0\4a\0\372\304\0\377\3334\0\0\0\4a\0\362\34\0\377\333\226\0\0\0\4a\0\361\272\0\377\324`\0\0\0\4a\0\371\30\0\377\327x\0\0\0\4a\0\366\0\0\377\327\322\0\0\0\4a\0\365\246\0\377\330:\0\0\0\4a\0\365>\0\377\330\234\0\0\0\4a\0\364\334\0\377\334\352\0\0\0\4\262|\377\377\0\0\0\0".getBytes (ISO_8859_1);

  //おむすびフォント
  //  XEiJの作者がXEiJのスクリーンショットに特徴を付けるために描いたANK8x16フォント
  //  三角のおむすびを意識して、上を狭く丸く、下を広く平らにした
  //  OmusubiのOに海苔を付けて同じ形の0と区別できるようにした(が、読み難い)
/*
  public static final byte[] ROM_OMUSUBI = {
    //  perl misc/sjdump.pl misc/omusubifont.x 64 -0 -64 -code
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000000  ................
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x22,0x22,0x3e,0x22,0x22,0x00,0x00,0x00,  //00000010  ..8@8.x."">""...
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x22,0x14,0x08,0x14,0x22,0x00,0x00,0x00,  //00000020  ..8@8.x."..."...
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x22,0x14,0x08,0x14,0x22,0x00,0x00,0x00,  //00000030  ..x@x@|."..."...
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x3e,0x08,0x08,0x08,0x08,0x00,0x00,0x00,  //00000040  ..x@x@|.>.......
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x1c,0x22,0x2a,0x24,0x1a,0x00,0x00,0x00,  //00000050  ..x@x@|.."*$....
    0x00,0x00,0x38,0x44,0x7c,0x44,0x44,0x00,0x22,0x24,0x38,0x24,0x22,0x00,0x00,0x00,  //00000060  ..8D|DD."$8$"...
    0x00,0x00,0x78,0x44,0x78,0x44,0x78,0x00,0x20,0x20,0x20,0x20,0x3e,0x00,0x00,0x00,  //00000070  ..xDxDx.    >...
    0x00,0x00,0x78,0x44,0x78,0x44,0x78,0x00,0x1c,0x20,0x1c,0x02,0x3c,0x00,0x00,0x00,  //00000080  ..xDxDx.. ..<...
    0x00,0x00,0x44,0x44,0x7c,0x44,0x44,0x00,0x3e,0x08,0x08,0x08,0x08,0x00,0x00,0x00,  //00000090  ..DD|DD.>.......
    0x00,0x00,0x40,0x40,0x40,0x40,0x7c,0x00,0x3c,0x20,0x3c,0x20,0x20,0x00,0x00,0x00,  //000000a0  ..@@@@|.< <  ...
    0x00,0x00,0x44,0x44,0x44,0x28,0x10,0x00,0x3e,0x08,0x08,0x08,0x08,0x00,0x00,0x00,  //000000b0  ..DDD(..>.......
    0x00,0x00,0x78,0x40,0x78,0x40,0x40,0x00,0x3c,0x20,0x3c,0x20,0x20,0x00,0x00,0x00,  //000000c0  ..x@x@@.< <  ...
    0x00,0x00,0x38,0x40,0x40,0x40,0x3c,0x00,0x3c,0x22,0x3c,0x24,0x22,0x00,0x00,0x00,  //000000d0  ..8@@@<.<"<$"...
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x1c,0x22,0x22,0x22,0x1c,0x00,0x00,0x00,  //000000e0  ..8@8.x.."""....
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x1c,0x08,0x08,0x08,0x1c,0x00,0x00,0x00,  //000000f0  ..8@8.x.........
    0x00,0x00,0x78,0x44,0x44,0x44,0x78,0x00,0x3c,0x20,0x3c,0x20,0x3e,0x00,0x00,0x00,  //00000100  ..xDDDx.< < >...
    0x00,0x00,0x78,0x44,0x44,0x44,0x78,0x00,0x18,0x08,0x08,0x08,0x08,0x00,0x00,0x00,  //00000110  ..xDDDx.........
    0x00,0x00,0x78,0x44,0x44,0x44,0x78,0x00,0x1c,0x02,0x1c,0x20,0x3e,0x00,0x00,0x00,  //00000120  ..xDDDx.... >...
    0x00,0x00,0x78,0x44,0x44,0x44,0x78,0x00,0x1c,0x02,0x1c,0x02,0x3c,0x00,0x00,0x00,  //00000130  ..xDDDx.....<...
    0x00,0x00,0x78,0x44,0x44,0x44,0x78,0x00,0x04,0x0c,0x14,0x3e,0x04,0x00,0x00,0x00,  //00000140  ..xDDDx....>....
    0x00,0x00,0x44,0x64,0x54,0x4c,0x44,0x00,0x22,0x24,0x38,0x24,0x22,0x00,0x00,0x00,  //00000150  ..DdTLD."$8$"...
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x22,0x32,0x2a,0x26,0x22,0x00,0x00,0x00,  //00000160  ..8@8.x."2*&"...
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x3c,0x22,0x3c,0x22,0x3c,0x00,0x00,0x00,  //00000170  ..x@x@|.<"<"<...
    0x00,0x00,0x38,0x40,0x40,0x40,0x3c,0x00,0x22,0x32,0x2a,0x26,0x22,0x00,0x00,0x00,  //00000180  ..8@@@<."2*&"...
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x22,0x36,0x2a,0x22,0x22,0x00,0x00,0x00,  //00000190  ..x@x@|."6*""...
    0x00,0x00,0x38,0x40,0x38,0x04,0x78,0x00,0x3c,0x22,0x3c,0x22,0x3c,0x00,0x00,0x00,  //000001a0  ..8@8.x.<"<"<...
    0x00,0x00,0x78,0x40,0x78,0x40,0x7c,0x00,0x1c,0x20,0x20,0x20,0x1e,0x00,0x00,0x00,  //000001b0  ..x@x@|..   ....
    0x00,0x00,0x38,0x10,0x10,0x10,0x38,0x00,0x04,0x0c,0x14,0x3e,0x04,0x00,0x00,0x00,  //000001c0  ..8...8....>....
    0x00,0x00,0x38,0x10,0x10,0x10,0x38,0x00,0x1c,0x02,0x1c,0x02,0x3c,0x00,0x00,0x00,  //000001d0  ..8...8.....<...
    0x00,0x00,0x38,0x10,0x10,0x10,0x38,0x00,0x1c,0x02,0x1c,0x20,0x3e,0x00,0x00,0x00,  //000001e0  ..8...8.... >...
    0x00,0x00,0x38,0x10,0x10,0x10,0x38,0x00,0x18,0x08,0x08,0x08,0x08,0x00,0x00,0x00,  //000001f0  ..8...8.........
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000200  ................
    0x00,0x00,0x10,0x38,0x38,0x38,0x38,0x10,0x10,0x00,0x10,0x38,0x10,0x00,0x00,0x00,  //00000210  ...8888....8....
    0x00,0x00,0x12,0x36,0x6c,0x48,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000220  ...6lH..........
    0x00,0x00,0x44,0x44,0x44,0xfe,0x44,0x44,0x44,0xfe,0x44,0x44,0x44,0x00,0x00,0x00,  //00000230  ..DDD.DDD.DDD...
    0x00,0x00,0x10,0x10,0x7e,0x90,0x90,0x7c,0x12,0x12,0xfc,0x10,0x10,0x00,0x00,0x00,  //00000240  ....~..|........
    0x00,0x00,0x62,0x92,0x94,0x68,0x08,0x10,0x20,0x2c,0x52,0x92,0x8c,0x00,0x00,0x00,  //00000250  ..b..h.. ,R.....
    0x00,0x00,0x70,0x88,0x88,0x50,0x20,0x52,0x8a,0x84,0x84,0x8a,0x72,0x00,0x00,0x00,  //00000260  ..p..P R....r...
    0x00,0x00,0x08,0x18,0x30,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000270  ....0 ..........
    0x08,0x10,0x10,0x10,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x10,0x10,0x10,0x08,0x00,  //00000280  ....       .....
    0x20,0x10,0x10,0x10,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x10,0x10,0x10,0x20,0x00,  //00000290   ............. .
    0x00,0x00,0x00,0x00,0x00,0x10,0x92,0x7c,0x38,0x7c,0x92,0x10,0x00,0x00,0x00,0x00,  //000002a0  .......|8|......
    0x00,0x00,0x00,0x00,0x10,0x10,0x10,0xfe,0x10,0x10,0x10,0x00,0x00,0x00,0x00,0x00,  //000002b0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x08,0x18,0x30,0x20,0x00,0x00,  //000002c0  ............0 ..
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xfe,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000002d0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x38,0x10,0x00,0x00,0x00,  //000002e0  ...........8....
    0x00,0x00,0x02,0x02,0x04,0x08,0x08,0x10,0x20,0x20,0x40,0x80,0x80,0x00,0x00,0x00,  //000002f0  ........  @.....
    0x00,0x00,0x38,0x44,0x44,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0x7c,0x00,0x00,0x00,  //00000300  ..8DD.......|...
    0x00,0x00,0x70,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000310  ..p.............
    0x00,0x00,0x78,0x04,0x04,0x04,0x38,0x40,0x80,0x80,0x80,0x80,0xfe,0x00,0x00,0x00,  //00000320  ..x...8@........
    0x00,0x00,0x78,0x04,0x04,0x04,0x78,0x04,0x02,0x02,0x02,0x02,0xfc,0x00,0x00,0x00,  //00000330  ..x...x.........
    0x00,0x00,0x0c,0x14,0x24,0x44,0x44,0x84,0x84,0x84,0xfe,0x04,0x04,0x00,0x00,0x00,  //00000340  ....$DD.........
    0x00,0x00,0x7c,0x40,0x40,0x40,0x78,0x04,0x02,0x02,0x02,0x02,0xfc,0x00,0x00,0x00,  //00000350  ..|@@@x.........
    0x00,0x00,0x38,0x40,0x40,0x80,0xb8,0xc4,0x82,0x82,0x82,0x82,0x7c,0x00,0x00,0x00,  //00000360  ..8@@.......|...
    0x00,0x00,0x7e,0x02,0x04,0x04,0x08,0x08,0x08,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000370  ..~.............
    0x00,0x00,0x38,0x44,0x44,0x44,0x38,0x44,0x82,0x82,0x82,0x82,0x7c,0x00,0x00,0x00,  //00000380  ..8DDD8D....|...
    0x00,0x00,0x38,0x44,0x82,0x82,0x82,0x82,0x7e,0x02,0x04,0x04,0x38,0x00,0x00,0x00,  //00000390  ..8D....~...8...
    0x00,0x00,0x00,0x10,0x38,0x10,0x00,0x00,0x00,0x10,0x38,0x10,0x00,0x00,0x00,0x00,  //000003a0  ....8.....8.....
    0x00,0x00,0x00,0x10,0x38,0x10,0x00,0x00,0x00,0x08,0x18,0x30,0x20,0x00,0x00,0x00,  //000003b0  ....8......0 ...
    0x00,0x00,0x02,0x04,0x08,0x10,0x20,0x40,0x20,0x10,0x08,0x04,0x02,0x00,0x00,0x00,  //000003c0  ...... @ .......
    0x00,0x00,0x00,0x00,0x00,0x7e,0x00,0x00,0x00,0x7e,0x00,0x00,0x00,0x00,0x00,0x00,  //000003d0  .....~...~......
    0x00,0x00,0x40,0x20,0x10,0x08,0x04,0x02,0x04,0x08,0x10,0x20,0x40,0x00,0x00,0x00,  //000003e0  ..@ ....... @...
    0x00,0x00,0x38,0x44,0x44,0x04,0x08,0x10,0x10,0x00,0x00,0x10,0x10,0x00,0x00,0x00,  //000003f0  ..8DD...........
    0x00,0x00,0x38,0x44,0x82,0x82,0x9e,0xa2,0xa2,0xa2,0x9e,0x80,0x7e,0x00,0x00,0x00,  //00000400  ..8D........~...
    0x00,0x00,0x38,0x44,0x44,0x82,0xfe,0x82,0x82,0x82,0x82,0x82,0x82,0x00,0x00,0x00,  //00000410  ..8DD...........
    0x00,0x00,0xf8,0x84,0x84,0x84,0xf8,0x84,0x82,0x82,0x82,0x82,0xfc,0x00,0x00,0x00,  //00000420  ................
    0x00,0x00,0x3c,0x40,0x40,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x7e,0x00,0x00,0x00,  //00000430  ..<@@.......~...
    0x00,0x00,0xf8,0x84,0x84,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0xfc,0x00,0x00,0x00,  //00000440  ................
    0x00,0x00,0xfc,0x80,0x80,0x80,0xfc,0x80,0x80,0x80,0x80,0x80,0xfe,0x00,0x00,0x00,  //00000450  ................
    0x00,0x00,0xfc,0x80,0x80,0x80,0xfc,0x80,0x80,0x80,0x80,0x80,0x80,0x00,0x00,0x00,  //00000460  ................
    0x00,0x00,0x3c,0x40,0x40,0x80,0x9e,0x82,0x82,0x82,0x82,0x86,0x7a,0x00,0x00,0x00,  //00000470  ..<@@.......z...
    0x00,0x00,0x82,0x82,0x82,0x82,0xfe,0x82,0x82,0x82,0x82,0x82,0x82,0x00,0x00,0x00,  //00000480  ................
    0x00,0x00,0x38,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x38,0x00,0x00,0x00,  //00000490  ..8.........8...
    0x00,0x00,0x02,0x02,0x02,0x02,0x02,0x02,0x02,0x02,0x82,0x82,0x7c,0x00,0x00,0x00,  //000004a0  ............|...
    0x00,0x00,0x84,0x88,0x90,0xa0,0xe0,0x90,0x88,0x84,0x84,0x82,0x82,0x00,0x00,0x00,  //000004b0  ................
    0x00,0x00,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0xfe,0x00,0x00,0x00,  //000004c0  ................
    0x00,0x00,0x82,0x82,0xc6,0xc6,0xaa,0xaa,0xaa,0x92,0x92,0x92,0x82,0x00,0x00,0x00,  //000004d0  ................
    0x00,0x00,0x82,0xc2,0xa2,0xa2,0x92,0x92,0x8a,0x8a,0x86,0x86,0x82,0x00,0x00,0x00,  //000004e0  ................
    0x00,0x00,0x38,0x44,0x44,0x82,0x82,0x82,0xaa,0x92,0xaa,0x92,0x7c,0x00,0x00,0x00,  //000004f0  ..8DD.......|...
    0x00,0x00,0xf8,0x84,0x84,0x84,0xf8,0x80,0x80,0x80,0x80,0x80,0x80,0x00,0x00,0x00,  //00000500  ................
    0x00,0x00,0x38,0x44,0x44,0x82,0x82,0x82,0x82,0xb2,0xca,0x84,0x7a,0x00,0x00,0x00,  //00000510  ..8DD.......z...
    0x00,0x00,0xf8,0x84,0x84,0x84,0xf8,0x90,0x88,0x84,0x84,0x82,0x82,0x00,0x00,0x00,  //00000520  ................
    0x00,0x00,0x3c,0x40,0x40,0x40,0x38,0x04,0x02,0x02,0x02,0x02,0xfc,0x00,0x00,0x00,  //00000530  ..<@@@8.........
    0x00,0x00,0xfe,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000540  ................
    0x00,0x00,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0x7c,0x00,0x00,0x00,  //00000550  ............|...
    0x00,0x00,0x82,0x82,0x82,0x82,0x44,0x44,0x44,0x28,0x28,0x10,0x10,0x00,0x00,0x00,  //00000560  ......DDD((.....
    0x00,0x00,0x82,0x82,0x92,0x92,0x92,0xaa,0xaa,0xaa,0x44,0x44,0x44,0x00,0x00,0x00,  //00000570  ..........DDD...
    0x00,0x00,0x82,0x82,0x44,0x28,0x10,0x28,0x44,0x44,0x82,0x82,0x82,0x00,0x00,0x00,  //00000580  ....D(.(DD......
    0x00,0x00,0x82,0x82,0x44,0x44,0x28,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000590  ....DD(.........
    0x00,0x00,0x7c,0x04,0x08,0x08,0x10,0x20,0x20,0x40,0x40,0x80,0xfe,0x00,0x00,0x00,  //000005a0  ..|....  @@.....
    0x00,0x3c,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x3c,0x00,0x00,  //000005b0  .<           <..
    0x00,0x00,0x82,0x82,0x44,0x28,0x10,0xfe,0x10,0xfe,0x10,0x10,0x10,0x00,0x00,0x00,  //000005c0  ....D(..........
    0x00,0x78,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x78,0x00,0x00,  //000005d0  .x...........x..
    0x00,0x00,0x10,0x28,0x44,0x82,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000005e0  ...(D...........
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xfe,0x00,0x00,0x00,  //000005f0  ................
    0x00,0x00,0x20,0x30,0x18,0x08,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000600  .. 0............
    0x00,0x00,0x00,0x00,0x00,0x00,0x78,0x04,0x02,0x7e,0x82,0x82,0x7e,0x00,0x00,0x00,  //00000610  ......x..~..~...
    0x00,0x00,0x80,0x80,0x80,0x80,0xb8,0xc4,0x82,0x82,0x82,0x82,0xfc,0x00,0x00,0x00,  //00000620  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x1e,0x20,0x40,0x40,0x40,0x40,0x3e,0x00,0x00,0x00,  //00000630  ....... @@@@>...
    0x00,0x00,0x02,0x02,0x02,0x02,0x3a,0x46,0x82,0x82,0x82,0x82,0x7e,0x00,0x00,0x00,  //00000640  ......:F....~...
    0x00,0x00,0x00,0x00,0x00,0x00,0x38,0x44,0x82,0xfe,0x80,0x80,0x7e,0x00,0x00,0x00,  //00000650  ......8D....~...
    0x00,0x00,0x0e,0x10,0x10,0x10,0xfe,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000660  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x3a,0x46,0x82,0x82,0x82,0x86,0x7a,0x02,0x04,0x78,  //00000670  ......:F....z..x
    0x00,0x00,0x80,0x80,0x80,0x80,0xb8,0xc4,0x82,0x82,0x82,0x82,0x82,0x00,0x00,0x00,  //00000680  ................
    0x00,0x00,0x10,0x10,0x00,0x00,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //00000690  ................
    0x00,0x00,0x08,0x08,0x00,0x00,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x08,0x10,0xe0,  //000006a0  ................
    0x00,0x00,0x40,0x40,0x40,0x40,0x42,0x44,0x48,0x78,0x44,0x42,0x42,0x00,0x00,0x00,  //000006b0  ..@@@@BDHxDBB...
    0x00,0x00,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,0x00,  //000006c0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0xec,0x92,0x92,0x92,0x92,0x92,0x92,0x00,0x00,0x00,  //000006d0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0xb8,0xc4,0x82,0x82,0x82,0x82,0x82,0x00,0x00,0x00,  //000006e0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x38,0x44,0x82,0x82,0x82,0x82,0x7c,0x00,0x00,0x00,  //000006f0  ......8D....|...
    0x00,0x00,0x00,0x00,0x00,0x00,0xb8,0xc4,0x82,0x82,0x82,0x82,0xfc,0x80,0x80,0x80,  //00000700  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x3a,0x46,0x82,0x82,0x82,0x82,0x7e,0x02,0x02,0x02,  //00000710  ......:F....~...
    0x00,0x00,0x00,0x00,0x00,0x00,0x5e,0x60,0x40,0x40,0x40,0x40,0x40,0x00,0x00,0x00,  //00000720  ......^`@@@@@...
    0x00,0x00,0x00,0x00,0x00,0x00,0x3c,0x40,0x40,0x3c,0x02,0x02,0x7c,0x00,0x00,0x00,  //00000730  ......<@@<..|...
    0x00,0x00,0x00,0x20,0x20,0x20,0xfc,0x20,0x20,0x20,0x20,0x20,0x1c,0x00,0x00,0x00,  //00000740  ...   .     ....
    0x00,0x00,0x00,0x00,0x00,0x00,0x82,0x82,0x82,0x82,0x82,0x86,0x7a,0x00,0x00,0x00,  //00000750  ............z...
    0x00,0x00,0x00,0x00,0x00,0x00,0x82,0x82,0x82,0x44,0x44,0x28,0x10,0x00,0x00,0x00,  //00000760  .........DD(....
    0x00,0x00,0x00,0x00,0x00,0x00,0x82,0x92,0x92,0xaa,0xaa,0x44,0x44,0x00,0x00,0x00,  //00000770  ...........DD...
    0x00,0x00,0x00,0x00,0x00,0x00,0x44,0x28,0x10,0x28,0x44,0x82,0x82,0x00,0x00,0x00,  //00000780  ......D(.(D.....
    0x00,0x00,0x00,0x00,0x00,0x00,0x82,0x82,0x82,0x82,0x82,0x46,0x3a,0x02,0x04,0x78,  //00000790  ...........F:..x
    0x00,0x00,0x00,0x00,0x00,0x00,0x7c,0x04,0x08,0x10,0x20,0x40,0xfe,0x00,0x00,0x00,  //000007a0  ......|... @....
    0x00,0x18,0x20,0x20,0x20,0x20,0x20,0xc0,0x20,0x20,0x20,0x20,0x20,0x18,0x00,0x00,  //000007b0  ..     .     ...
    0x00,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,  //000007c0  ................
    0x00,0x30,0x08,0x08,0x08,0x08,0x08,0x06,0x08,0x08,0x08,0x08,0x08,0x30,0x00,0x00,  //000007d0  .0...........0..
    0x00,0x00,0xfe,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000007e0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000007f0  ................
    0x00,0x00,0x80,0x80,0x40,0x20,0x20,0x10,0x08,0x08,0x04,0x02,0x02,0x00,0x00,0x00,  //00000800  ....@  .........
    0x00,0x00,0x60,0x92,0x0c,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000810  ..`.............
    0x00,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x10,0x10,0x10,0x10,0x10,0x10,0x00,0x00,  //00000820  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000830  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000840  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000850  ................
    0x00,0x00,0x00,0x10,0x10,0x7c,0x20,0x70,0x8e,0x34,0x44,0x40,0x3e,0x00,0x00,0x00,  //00000860  .....| p.4D@>...
    0x00,0x00,0x00,0x00,0x00,0x00,0x20,0x78,0x20,0x7c,0xaa,0x92,0x64,0x00,0x00,0x00,  //00000870  ...... x |..d...
    0x00,0x00,0x00,0x00,0x00,0x00,0x88,0x84,0x84,0x82,0x82,0x52,0x20,0x00,0x00,0x00,  //00000880  ...........R ...
    0x00,0x00,0x00,0x00,0x00,0x00,0x70,0x00,0x78,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000890  ......p.x...0...
    0x00,0x00,0x00,0x00,0x00,0x00,0x38,0x00,0x7c,0x08,0x10,0x30,0x4c,0x00,0x00,0x00,  //000008a0  ......8.|..0L...
    0x00,0x00,0x00,0x00,0x00,0x00,0x20,0x74,0x22,0x7c,0xa2,0xa2,0x64,0x00,0x00,0x00,  //000008b0  ...... t"|..d...
    0x00,0x00,0x00,0x00,0x00,0x00,0x4c,0x20,0x7c,0xa2,0x24,0x10,0x08,0x00,0x00,0x00,  //000008c0  ......L |.$.....
    0x00,0x00,0x00,0x00,0x00,0x00,0x10,0xbc,0xd2,0x92,0x3c,0x10,0x20,0x00,0x00,0x00,  //000008d0  ..........<. ...
    0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x10,0x1e,0x10,0x7c,0x92,0x60,0x00,0x00,0x00,  //000008e0  ..........|.`...
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x7c,0x02,0x02,0x04,0x18,0x00,0x00,0x00,0x00,  //000008f0  .......|........
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000900  ................
    0x00,0x00,0x00,0x20,0x20,0x78,0x20,0x7c,0xaa,0xaa,0xaa,0x92,0x64,0x00,0x00,0x00,  //00000910  ...  x |....d...
    0x00,0x00,0x00,0x00,0x80,0x88,0x84,0x84,0x82,0x82,0x82,0x50,0x20,0x00,0x00,0x00,  //00000920  ...........P ...
    0x00,0x00,0x00,0x60,0x18,0x00,0x38,0xc4,0x02,0x02,0x04,0x08,0x30,0x00,0x00,0x00,  //00000930  ...`..8.....0...
    0x00,0x00,0x00,0x30,0x0c,0x00,0x3e,0xc4,0x08,0x10,0x30,0x52,0x8c,0x00,0x00,0x00,  //00000940  ...0..>...0R....
    0x00,0x00,0x00,0x20,0x20,0x74,0x22,0x22,0x7c,0xa2,0xa2,0xa2,0x64,0x00,0x00,0x00,  //00000950  ...  t""|...d...
    0x00,0x00,0x00,0x28,0x24,0x3a,0xe4,0x24,0x24,0x44,0x44,0x44,0x98,0x00,0x00,0x00,  //00000960  ...($:.$$DDD....
    0x00,0x00,0x00,0x10,0x1c,0x70,0x0e,0x38,0x04,0x7e,0x80,0x80,0x7c,0x00,0x00,0x00,  //00000970  .....p.8.~..|...
    0x00,0x00,0x00,0x04,0x04,0x08,0x10,0x20,0x40,0x20,0x10,0x08,0x04,0x00,0x00,0x00,  //00000980  ....... @ ......
    0x00,0x00,0x00,0x08,0x08,0x84,0x84,0xbe,0x84,0x84,0x84,0x84,0x68,0x00,0x00,0x00,  //00000990  ............h...
    0x00,0x00,0x00,0x1c,0x62,0x02,0x04,0x00,0x00,0x20,0x40,0x80,0x7e,0x00,0x00,0x00,  //000009a0  ....b.... @.~...
    0x00,0x00,0x00,0x10,0x10,0x1c,0x70,0x08,0x04,0x7e,0x80,0x80,0x7c,0x00,0x00,0x00,  //000009b0  ......p..~..|...
    0x00,0x00,0x00,0x80,0x40,0x40,0x80,0x80,0x80,0x80,0x82,0x44,0x38,0x00,0x00,0x00,  //000009c0  ....@@.....D8...
    0x00,0x00,0x00,0x08,0x08,0xfe,0x08,0x78,0x88,0x98,0x68,0x08,0x30,0x00,0x00,0x00,  //000009d0  .......x..h.0...
    0x00,0x00,0x00,0x44,0x44,0x44,0x5e,0xe4,0x44,0x4c,0x40,0x40,0x3e,0x00,0x00,0x00,  //000009e0  ...DDD^.DL@@>...
    0x00,0x00,0x00,0x1c,0x64,0x08,0x10,0x26,0xf8,0x10,0x20,0x40,0x3e,0x00,0x00,0x00,  //000009f0  ....d..&.. @>...
    0x00,0x00,0xaa,0x00,0xaa,0x00,0xaa,0x00,0xaa,0x00,0xaa,0x00,0xaa,0x00,0x00,0x00,  //00000a00  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x60,0x90,0x90,0x60,0x00,0x00,0x00,  //00000a10  .........`..`...
    0x00,0x00,0x00,0x3c,0x20,0x20,0x20,0x20,0x20,0x20,0x00,0x00,0x00,0x00,0x00,0x00,  //00000a20  ...<      ......
    0x00,0x00,0x00,0x00,0x00,0x00,0x08,0x08,0x08,0x08,0x08,0x08,0x78,0x00,0x00,0x00,  //00000a30  ............x...
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x80,0x40,0x20,0x10,0x00,0x00,0x00,  //00000a40  ..........@ ....
    0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x38,0x10,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000a50  .......8........
    0x00,0x00,0x00,0x7e,0x02,0x02,0x7e,0x02,0x02,0x04,0x08,0x10,0x60,0x00,0x00,0x00,  //00000a60  ...~..~.....`...
    0x00,0x00,0x00,0x00,0x00,0x00,0x7e,0x02,0x12,0x1c,0x10,0x20,0x40,0x00,0x00,0x00,  //00000a70  ......~.... @...
    0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x0c,0x18,0x68,0x08,0x08,0x08,0x00,0x00,0x00,  //00000a80  .........h......
    0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x7e,0x42,0x42,0x02,0x04,0x18,0x00,0x00,0x00,  //00000a90  .......~BB......
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x7e,0x10,0x10,0x10,0x10,0xfe,0x00,0x00,0x00,  //00000aa0  .......~........
    0x00,0x00,0x00,0x00,0x00,0x00,0x08,0x08,0x7e,0x18,0x28,0x48,0x18,0x00,0x00,0x00,  //00000ab0  ........~.(H....
    0x00,0x00,0x00,0x00,0x00,0x00,0x20,0x2e,0x72,0x12,0x10,0x08,0x08,0x00,0x00,0x00,  //00000ac0  ...... .r.......
    0x00,0x00,0x00,0x00,0x00,0x00,0x3c,0x04,0x04,0x08,0x08,0x08,0x7e,0x00,0x00,0x00,  //00000ad0  ......<.....~...
    0x00,0x00,0x00,0x00,0x00,0x00,0x3c,0x04,0x04,0x3c,0x04,0x04,0x3c,0x00,0x00,0x00,  //00000ae0  ......<..<..<...
    0x00,0x00,0x00,0x00,0x00,0x00,0x52,0x52,0x52,0x02,0x04,0x08,0x30,0x00,0x00,0x00,  //00000af0  ......RRR...0...
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x40,0x3e,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000b00  .......@>.......
    0x00,0x00,0x00,0xfe,0x02,0x02,0x12,0x14,0x18,0x10,0x10,0x20,0xc0,0x00,0x00,0x00,  //00000b10  ........... ....
    0x00,0x00,0x02,0x02,0x04,0x04,0x08,0x18,0x28,0x48,0x88,0x08,0x08,0x00,0x00,0x00,  //00000b20  ........(H......
    0x00,0x00,0x10,0x10,0x10,0xfe,0x82,0x82,0x82,0x02,0x04,0x08,0x30,0x00,0x00,0x00,  //00000b30  ............0...
    0x00,0x00,0x00,0x00,0x7e,0x10,0x10,0x10,0x10,0x10,0x10,0x10,0xfe,0x00,0x00,0x00,  //00000b40  ....~...........
    0x00,0x00,0x08,0x08,0x08,0x7e,0x08,0x18,0x28,0x48,0x88,0x08,0x18,0x00,0x00,0x00,  //00000b50  .....~..(H......
    0x00,0x00,0x10,0x10,0x10,0x7e,0x12,0x22,0x22,0x22,0x42,0x42,0x8c,0x00,0x00,0x00,  //00000b60  .....~."""BB....
    0x00,0x00,0x20,0x20,0x20,0x1c,0xf0,0x10,0x1e,0xf0,0x08,0x08,0x08,0x00,0x00,0x00,  //00000b70  ..   ...........
    0x00,0x00,0x20,0x20,0x3e,0x22,0x42,0x42,0x82,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000b80  ..  >"BB....0...
    0x00,0x00,0x80,0x80,0x80,0xfe,0x88,0x88,0x88,0x08,0x10,0x10,0x20,0x00,0x00,0x00,  //00000b90  ............ ...
    0x00,0x00,0x00,0x00,0x7e,0x02,0x02,0x02,0x02,0x02,0x02,0x02,0xfe,0x00,0x00,0x00,  //00000ba0  ....~...........
    0x00,0x00,0x44,0x44,0x44,0xfe,0x44,0x44,0x44,0x08,0x08,0x10,0x20,0x00,0x00,0x00,  //00000bb0  ..DDD.DDD... ...
    0x00,0x00,0x00,0xe0,0x10,0x02,0xe2,0x12,0x04,0x04,0x08,0x30,0xc0,0x00,0x00,0x00,  //00000bc0  ...........0....
    0x00,0x00,0x00,0x7e,0x02,0x02,0x04,0x04,0x08,0x14,0x24,0x42,0x82,0x00,0x00,0x00,  //00000bd0  ...~......$B....
    0x00,0x00,0x40,0x40,0x40,0x5e,0xe2,0x42,0x44,0x40,0x40,0x40,0x3e,0x00,0x00,0x00,  //00000be0  ..@@@^.BD@@@>...
    0x00,0x00,0x00,0x82,0x42,0x22,0x22,0x04,0x04,0x08,0x08,0x10,0x20,0x00,0x00,0x00,  //00000bf0  ....B""..... ...
    0x00,0x00,0x00,0x3e,0x22,0x22,0x52,0x8c,0x04,0x0a,0x10,0x20,0x40,0x00,0x00,0x00,  //00000c00  ...>""R.... @...
    0x00,0x00,0x00,0x04,0x18,0x70,0x08,0x08,0xfe,0x08,0x08,0x10,0x20,0x00,0x00,0x00,  //00000c10  .....p...... ...
    0x00,0x00,0x00,0x22,0x12,0x92,0x52,0x42,0x44,0x04,0x08,0x10,0x20,0x00,0x00,0x00,  //00000c20  ..."..RBD... ...
    0x00,0x00,0x00,0x7c,0x00,0x00,0xfe,0x10,0x10,0x10,0x10,0x20,0x40,0x00,0x00,0x00,  //00000c30  ...|....... @...
    0x00,0x00,0x00,0x40,0x40,0x40,0x40,0x70,0x48,0x44,0x40,0x40,0x40,0x00,0x00,0x00,  //00000c40  ...@@@@pHD@@@...
    0x00,0x00,0x00,0x10,0x10,0x10,0xfe,0x10,0x10,0x10,0x10,0x20,0x40,0x00,0x00,0x00,  //00000c50  ........... @...
    0x00,0x00,0x00,0x00,0x00,0x7c,0x00,0x00,0x00,0x00,0x00,0x00,0xfe,0x00,0x00,0x00,  //00000c60  .....|..........
    0x00,0x00,0x00,0xfe,0x02,0x02,0x04,0x44,0x28,0x10,0x28,0x44,0x80,0x00,0x00,0x00,  //00000c70  .......D(.(D....
    0x00,0x00,0x00,0x10,0x10,0xfe,0x02,0x04,0x08,0x10,0x34,0xd2,0x10,0x00,0x00,0x00,  //00000c80  ..........4.....
    0x00,0x00,0x00,0x02,0x02,0x02,0x04,0x04,0x04,0x08,0x10,0x20,0xc0,0x00,0x00,0x00,  //00000c90  ........... ....
    0x00,0x00,0x00,0x10,0x10,0x08,0x48,0x44,0x44,0x44,0x82,0x82,0x82,0x00,0x00,0x00,  //00000ca0  ......HDDD......
    0x00,0x00,0x00,0x80,0x80,0x8e,0xf0,0x80,0x80,0x80,0x80,0x80,0x7e,0x00,0x00,0x00,  //00000cb0  ............~...
    0x00,0x00,0x00,0xfe,0x02,0x02,0x02,0x02,0x02,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000cc0  ............0...
    0x00,0x00,0x00,0x20,0x50,0x48,0x88,0x84,0x04,0x02,0x02,0x02,0x02,0x00,0x00,0x00,  //00000cd0  ... PH..........
    0x00,0x00,0x00,0x10,0x10,0x10,0xfe,0x10,0x10,0x54,0x92,0x92,0x10,0x00,0x00,0x00,  //00000ce0  .........T......
    0x00,0x00,0x00,0xfe,0x02,0x02,0x04,0x04,0x88,0x50,0x20,0x10,0x08,0x00,0x00,0x00,  //00000cf0  .........P .....
    0x00,0x00,0x00,0x00,0x70,0x0c,0x02,0x70,0x0c,0x02,0xf0,0x0c,0x02,0x00,0x00,0x00,  //00000d00  ....p..p........
    0x00,0x00,0x00,0x10,0x10,0x10,0x20,0x20,0x28,0x44,0x44,0x9a,0xe2,0x00,0x00,0x00,  //00000d10  ......  (DD.....
    0x00,0x00,0x00,0x04,0x04,0x04,0x08,0xc8,0x30,0x18,0x24,0x42,0x80,0x00,0x00,0x00,  //00000d20  ........0.$B....
    0x00,0x00,0x00,0x7c,0x10,0x10,0x10,0xfe,0x10,0x10,0x10,0x10,0x0e,0x00,0x00,0x00,  //00000d30  ...|............
    0x00,0x00,0x00,0x40,0x40,0x2e,0xf2,0x22,0x14,0x10,0x10,0x08,0x08,0x00,0x00,0x00,  //00000d40  ...@@.."........
    0x00,0x00,0x00,0x7c,0x04,0x04,0x08,0x08,0x10,0x10,0x10,0xfe,0x00,0x00,0x00,0x00,  //00000d50  ...|............
    0x00,0x00,0x00,0xfe,0x02,0x02,0x02,0xfe,0x02,0x02,0x02,0xfe,0x02,0x00,0x00,0x00,  //00000d60  ................
    0x00,0x00,0x00,0xfe,0x00,0x00,0xfe,0x02,0x02,0x02,0x04,0x08,0x30,0x00,0x00,0x00,  //00000d70  ............0...
    0x00,0x00,0x00,0x82,0x82,0x82,0x82,0x82,0x02,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000d80  ............0...
    0x00,0x00,0x00,0x50,0x50,0x50,0x50,0x50,0x52,0x52,0x94,0x94,0x98,0x00,0x00,0x00,  //00000d90  ...PPPPPRR......
    0x00,0x00,0x00,0x80,0x80,0x80,0x80,0x80,0x82,0x82,0x84,0x98,0xe0,0x00,0x00,0x00,  //00000da0  ................
    0x00,0x00,0x00,0xfe,0x82,0x82,0x82,0x82,0x82,0x82,0x82,0xfe,0x82,0x00,0x00,0x00,  //00000db0  ................
    0x00,0x00,0x00,0xfe,0x82,0x82,0x82,0x02,0x02,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000dc0  ............0...
    0x00,0x00,0x00,0x80,0x40,0x20,0x00,0x02,0x02,0x04,0x08,0x30,0xc0,0x00,0x00,0x00,  //00000dd0  ....@ .....0....
    0x00,0x00,0x00,0x90,0x48,0x24,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000de0  ....H$..........
    0x00,0x00,0x00,0x60,0x90,0x90,0x60,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000df0  ...`..`.........
    0x00,0x00,0x00,0x20,0x20,0x20,0xfe,0x20,0x4e,0x42,0x40,0x90,0x8e,0x00,0x00,0x00,  //00000e00  ...   . NB@.....
    0x00,0x00,0x00,0x20,0x20,0xfc,0x40,0x58,0xe4,0x82,0x02,0x02,0x7c,0x00,0x00,0x00,  //00000e10  ...  .@X....|...
    0x00,0x00,0x00,0x00,0xf8,0x04,0x02,0x02,0x02,0x04,0x18,0x60,0x00,0x00,0x00,0x00,  //00000e20  ...........`....
    0x00,0x00,0x00,0xfe,0x04,0x08,0x10,0x20,0x20,0x20,0x20,0x10,0x0e,0x00,0x00,0x00,  //00000e30  .......    .....
    0x00,0x00,0x00,0x40,0x40,0x40,0x4e,0x30,0x40,0x80,0x80,0x80,0x7e,0x00,0x00,0x00,  //00000e40  ...@@@N0@...~...
    0x00,0x00,0x00,0x28,0x24,0x22,0xf2,0x48,0x88,0x38,0x4c,0x4a,0x30,0x00,0x00,0x00,  //00000e50  ...($".H.8LJ0...
    0x00,0x00,0x00,0x9c,0x82,0x84,0x80,0x80,0xa0,0xa0,0xa0,0x9e,0x00,0x00,0x00,0x00,  //00000e60  ................
    0x00,0x00,0x00,0x08,0x88,0x88,0x7c,0xd2,0xb2,0x96,0xaa,0xaa,0x46,0x00,0x00,0x00,  //00000e70  ......|.....F...
    0x00,0x00,0x00,0x20,0x20,0xec,0x32,0x22,0x62,0x66,0xaa,0xaa,0x26,0x00,0x00,0x00,  //00000e80  ...  .2"bf..&...
    0x00,0x00,0x00,0x00,0x38,0x54,0x92,0x92,0x92,0x92,0x92,0x64,0x00,0x00,0x00,0x00,  //00000e90  ....8T.....d....
    0x00,0x00,0x00,0x84,0x84,0xbe,0x84,0x84,0x84,0x9c,0xa6,0xa4,0x98,0x00,0x00,0x00,  //00000ea0  ................
    0x00,0x00,0x00,0xe8,0x2c,0x4a,0x48,0x84,0x84,0x84,0x84,0x88,0x70,0x00,0x00,0x00,  //00000eb0  ....,JH.....p...
    0x00,0x00,0x00,0x38,0x08,0x10,0x54,0x54,0x8a,0x8a,0x8a,0x8a,0x30,0x00,0x00,0x00,  //00000ec0  ...8..TT....0...
    0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x28,0xc4,0x02,0x00,0x00,0x00,0x00,0x00,0x00,  //00000ed0  .......(........
    0x00,0x00,0x00,0xbe,0x84,0x84,0xbe,0x84,0x84,0x9c,0xa6,0xa4,0x98,0x00,0x00,0x00,  //00000ee0  ................
    0x00,0x00,0x00,0x10,0x10,0xfe,0x10,0xfe,0x10,0x78,0x94,0x92,0x60,0x00,0x00,0x00,  //00000ef0  .........x..`...
    0x00,0x00,0x00,0x70,0x14,0x14,0x3e,0x54,0x94,0xa4,0xa4,0x44,0x08,0x00,0x00,0x00,  //00000f00  ...p..>T...D....
    0x00,0x00,0x00,0x20,0x20,0xfc,0x20,0x2c,0x62,0xa0,0xa2,0x62,0x1c,0x00,0x00,0x00,  //00000f10  ...  . ,b..b....
    0x00,0x00,0x00,0x04,0x44,0x5c,0x2a,0x6a,0xaa,0xaa,0x92,0x92,0x64,0x00,0x00,0x00,  //00000f20  ....D\*j....d...
    0x00,0x00,0x00,0x20,0x20,0xfc,0x20,0xfc,0x20,0x20,0x22,0x22,0x1c,0x00,0x00,0x00,  //00000f30  ...  . .  ""....
    0x00,0x00,0x00,0x08,0x88,0x48,0x5c,0xe2,0x22,0x24,0x10,0x10,0x10,0x00,0x00,0x00,  //00000f40  .....H\."$......
    0x00,0x00,0x00,0x10,0x10,0x88,0xbc,0xca,0x8a,0x8a,0xaa,0x1c,0x20,0x00,0x00,0x00,  //00000f50  ............ ...
    0x00,0x00,0x00,0x10,0x10,0x10,0x1e,0x10,0x10,0x78,0x94,0x92,0x60,0x00,0x00,0x00,  //00000f60  .........x..`...
    0x00,0x00,0x00,0xc0,0x30,0x00,0x80,0x80,0xbc,0xc2,0x82,0x04,0x38,0x00,0x00,0x00,  //00000f70  ....0.......8...
    0x00,0x00,0x00,0x9c,0xa2,0xc2,0xc2,0x82,0x82,0x04,0x04,0x08,0x30,0x00,0x00,0x00,  //00000f80  ............0...
    0x00,0x00,0x00,0x7c,0x08,0x10,0x20,0x7c,0x82,0x3a,0x46,0x42,0x3c,0x00,0x00,0x00,  //00000f90  ...|.. |.:FB<...
    0x00,0x00,0x00,0x20,0x20,0xec,0x32,0x22,0x62,0xa4,0xa8,0x2a,0x24,0x00,0x00,0x00,  //00000fa0  ...  .2"b..*$...
    0x00,0x00,0x00,0x7c,0x08,0x10,0x20,0x7c,0x82,0x02,0x02,0x04,0x38,0x00,0x00,0x00,  //00000fb0  ...|.. |....8...
    0x00,0x00,0x00,0x20,0x20,0x20,0xec,0x32,0x62,0x62,0xe2,0xa4,0x28,0x00,0x00,0x00,  //00000fc0  ...   .2bb..(...
    0x00,0x00,0x00,0x10,0x10,0x20,0x20,0x40,0x40,0x62,0x92,0x92,0x8c,0x00,0x00,0x00,  //00000fd0  .....  @@b......
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000fe0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000ff0  ................
  };
*/
  //  perl misc/itob.pl xeij/XEiJ.java ROM_OMUSUBI
  public static final byte[] ROM_OMUSUBI = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\08@8\4x\0\"\">\"\"\0\0\0\0\08@8\4x\0\"\24\b\24\"\0\0\0\0\0x@x@|\0\"\24\b\24\"\0\0\0\0\0x@x@|\0>\b\b\b\b\0\0\0\0\0x@x@|\0\34\"*$\32\0\0\0\0\08D|DD\0\"$8$\"\0\0\0\0\0xDxDx\0    >\0\0\0\0\0xDxDx\0\34 \34\2<\0\0\0\0\0DD|DD\0>\b\b\b\b\0\0\0\0\0@@@@|\0< <  \0\0\0\0\0DDD(\20\0>\b\b\b\b\0\0\0\0\0x@x@@\0< <  \0\0\0\0\08@@@<\0<\"<$\"\0\0\0\0\08@8\4x\0\34\"\"\"\34\0\0\0\0\08@8\4x\0\34\b\b\b\34\0\0\0\0\0xDDDx\0< < >\0\0\0\0\0xDDDx\0\30\b\b\b\b\0\0\0\0\0xDDDx\0\34\2\34 >\0\0\0\0\0xDDDx\0\34\2\34\2<\0\0\0\0\0xDDDx\0\4\f\24>\4\0\0\0\0\0DdTLD\0\"$8$\"\0\0\0\0\08@8\4x\0\"2*&\"\0\0\0\0\0x@x@|\0<\"<\"<\0\0\0\0\08@@@<\0\"2*&\"\0\0\0\0\0x@x@|\0\"6*\"\"\0\0\0\0\08@8\4x\0<\"<\"<\0\0\0\0\0x@x@|\0\34   \36\0\0\0\0\08\20\20\208\0\4\f\24>\4\0\0\0\0\08\20\20\208\0\34\2\34\2<\0\0\0\0\08\20\20\208\0\34\2\34 >\0\0\0\0\08\20\20\208\0\30\b\b\b\b\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\208888\20\20\0\208\20\0\0\0\0\0\0226lH\0\0\0\0\0\0\0\0\0\0\0\0DDD\376DDD\376DDD\0\0\0\0\0\20\20~\220\220|\22\22\374\20\20\0\0\0\0\0b\222\224h\b\20 ,R\222\214\0\0\0\0\0p\210\210P R\212\204\204\212r\0\0\0\0\0\b\0300 \0\0\0\0\0\0\0\0\0\0\b\20\20\20       \20\20\20\b\0 \20\20\20\b\b\b\b\b\b\b\20\20\20 \0\0\0\0\0\0\20\222|8|\222\20\0\0\0\0\0\0\0\0\20\20\20\376\20\20\20\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\0300 \0\0\0\0\0\0\0\0\0\376\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\208\20\0\0\0\0\0\2\2\4\b\b\20  @\200\200\0\0\0\0\08DD\202\202\202\202\202\202\202|\0\0\0\0\0p\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0x\4\4\48@\200\200\200\200\376\0\0\0\0\0x\4\4\4x\4\2\2\2\2\374\0\0\0\0\0\f\24$DD\204\204\204\376\4\4\0\0\0\0\0|@@@x\4\2\2\2\2\374\0\0\0\0\08@@\200\270\304\202\202\202\202|\0\0\0\0\0~\2\4\4\b\b\b\20\20\20\20\0\0\0\0\08DDD8D\202\202\202\202|\0\0\0\0\08D\202\202\202\202~\2\4\48\0\0\0\0\0\0\208\20\0\0\0\208\20\0\0\0\0\0\0\0\208\20\0\0\0\b\0300 \0\0\0\0\0\2\4\b\20 @ \20\b\4\2\0\0\0\0\0\0\0\0~\0\0\0~\0\0\0\0\0\0\0\0@ \20\b\4\2\4\b\20 @\0\0\0\0\08DD\4\b\20\20\0\0\20\20\0\0\0\0\08D\202\202\236\242\242\242\236\200~\0\0\0\0\08DD\202\376\202\202\202\202\202\202\0\0\0\0\0\370\204\204\204\370\204\202\202\202\202\374\0\0\0\0\0<@@\200\200\200\200\200\200\200~\0\0\0\0\0\370\204\204\202\202\202\202\202\202\202\374\0\0\0\0\0\374\200\200\200\374\200\200\200\200\200\376\0\0\0\0\0\374\200\200\200\374\200\200\200\200\200\200\0\0\0\0\0<@@\200\236\202\202\202\202\206z\0\0\0\0\0\202\202\202\202\376\202\202\202\202\202\202\0\0\0\0\08\20\20\20\20\20\20\20\20\208\0\0\0\0\0\2\2\2\2\2\2\2\2\202\202|\0\0\0\0\0\204\210\220\240\340\220\210\204\204\202\202\0\0\0\0\0\200\200\200\200\200\200\200\200\200\200\376\0\0\0\0\0\202\202\306\306\252\252\252\222\222\222\202\0\0\0\0\0\202\302\242\242\222\222\212\212\206\206\202\0\0\0\0\08DD\202\202\202\252\222\252\222|\0\0\0\0\0\370\204\204\204\370\200\200\200\200\200\200\0\0\0\0\08DD\202\202\202\202\262\312\204z\0\0\0\0\0\370\204\204\204\370\220\210\204\204\202\202\0\0\0\0\0<@@@8\4\2\2\2\2\374\0\0\0\0\0\376\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0\202\202\202\202\202\202\202\202\202\202|\0\0\0\0\0\202\202\202\202DDD((\20\20\0\0\0\0\0\202\202\222\222\222\252\252\252DDD\0\0\0\0\0\202\202D(\20(DD\202\202\202\0\0\0\0\0\202\202DD(\20\20\20\20\20\20\0\0\0\0\0|\4\b\b\20  @@\200\376\0\0\0\0<           <\0\0\0\0\202\202D(\20\376\20\376\20\20\20\0\0\0\0x\b\b\b\b\b\b\b\b\b\b\bx\0\0\0\0\20(D\202\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\376\0\0\0\0\0 0\30\b\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0x\4\2~\202\202~\0\0\0\0\0\200\200\200\200\270\304\202\202\202\202\374\0\0\0\0\0\0\0\0\0\36 @@@@>\0\0\0\0\0\2\2\2\2:F\202\202\202\202~\0\0\0\0\0\0\0\0\08D\202\376\200\200~\0\0\0\0\0\16\20\20\20\376\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0:F\202\202\202\206z\2\4x\0\0\200\200\200\200\270\304\202\202\202\202\202\0\0\0\0\0\20\20\0\0\20\20\20\20\20\20\20\0\0\0\0\0\b\b\0\0\b\b\b\b\b\b\b\b\20\340\0\0@@@@BDHxDBB\0\0\0\0\0\20\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0\354\222\222\222\222\222\222\0\0\0\0\0\0\0\0\0\270\304\202\202\202\202\202\0\0\0\0\0\0\0\0\08D\202\202\202\202|\0\0\0\0\0\0\0\0\0\270\304\202\202\202\202\374\200\200\200\0\0\0\0\0\0:F\202\202\202\202~\2\2\2\0\0\0\0\0\0^`@@@@@\0\0\0\0\0\0\0\0\0<@@<\2\2|\0\0\0\0\0\0   \374     \34\0\0\0\0\0\0\0\0\0\202\202\202\202\202\206z\0\0\0\0\0\0\0\0\0\202\202\202DD(\20\0\0\0\0\0\0\0\0\0\202\222\222\252\252DD\0\0\0\0\0\0\0\0\0D(\20(D\202\202\0\0\0\0\0\0\0\0\0\202\202\202\202\202F:\2\4x\0\0\0\0\0\0|\4\b\20 @\376\0\0\0\0\30     \300     \30\0\0\0\20\20\20\20\20\20\20\20\20\20\20\20\20\0\0\0000\b\b\b\b\b\6\b\b\b\b\b0\0\0\0\0\376\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\200\200@  \20\b\b\4\2\2\0\0\0\0\0`\222\f\0\0\0\0\0\0\0\0\0\0\0\0\20\20\20\20\20\20\0\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\20\20| p\2164D@>\0\0\0\0\0\0\0\0\0 x |\252\222d\0\0\0\0\0\0\0\0\0\210\204\204\202\202R \0\0\0\0\0\0\0\0\0p\0x\4\4\b0\0\0\0\0\0\0\0\0\08\0|\b\0200L\0\0\0\0\0\0\0\0\0 t\"|\242\242d\0\0\0\0\0\0\0\0\0L |\242$\20\b\0\0\0\0\0\0\0\0\0\20\274\322\222<\20 \0\0\0\0\0\0\0\0\0\20\20\36\20|\222`\0\0\0\0\0\0\0\0\0\0|\2\2\4\30\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0  x |\252\252\252\222d\0\0\0\0\0\0\0\200\210\204\204\202\202\202P \0\0\0\0\0\0`\30\08\304\2\2\4\b0\0\0\0\0\0\0000\f\0>\304\b\0200R\214\0\0\0\0\0\0  t\"\"|\242\242\242d\0\0\0\0\0\0($:\344$$DDD\230\0\0\0\0\0\0\20\34p\168\4~\200\200|\0\0\0\0\0\0\4\4\b\20 @ \20\b\4\0\0\0\0\0\0\b\b\204\204\276\204\204\204\204h\0\0\0\0\0\0\34b\2\4\0\0 @\200~\0\0\0\0\0\0\20\20\34p\b\4~\200\200|\0\0\0\0\0\0\200@@\200\200\200\200\202D8\0\0\0\0\0\0\b\b\376\bx\210\230h\b0\0\0\0\0\0\0DDD^\344DL@@>\0\0\0\0\0\0\34d\b\20&\370\20 @>\0\0\0\0\0\252\0\252\0\252\0\252\0\252\0\252\0\0\0\0\0\0\0\0\0\0\0\0`\220\220`\0\0\0\0\0\0<      \0\0\0\0\0\0\0\0\0\0\0\0\b\b\b\b\b\bx\0\0\0\0\0\0\0\0\0\0\0\0\200@ \20\0\0\0\0\0\0\0\0\0\208\20\0\0\0\0\0\0\0\0\0\0~\2\2~\2\2\4\b\20`\0\0\0\0\0\0\0\0\0~\2\22\34\20 @\0\0\0\0\0\0\0\0\0\2\f\30h\b\b\b\0\0\0\0\0\0\0\0\0\20~BB\2\4\30\0\0\0\0\0\0\0\0\0\0~\20\20\20\20\376\0\0\0\0\0\0\0\0\0\b\b~\30(H\30\0\0\0\0\0\0\0\0\0 .r\22\20\b\b\0\0\0\0\0\0\0\0\0<\4\4\b\b\b~\0\0\0\0\0\0\0\0\0<\4\4<\4\4<\0\0\0\0\0\0\0\0\0RRR\2\4\b0\0\0\0\0\0\0\0\0\0\0@>\0\0\0\0\0\0\0\0\0\0\376\2\2\22\24\30\20\20 \300\0\0\0\0\0\2\2\4\4\b\30(H\210\b\b\0\0\0\0\0\20\20\20\376\202\202\202\2\4\b0\0\0\0\0\0\0\0~\20\20\20\20\20\20\20\376\0\0\0\0\0\b\b\b~\b\30(H\210\b\30\0\0\0\0\0\20\20\20~\22\"\"\"BB\214\0\0\0\0\0   \34\360\20\36\360\b\b\b\0\0\0\0\0  >\"BB\202\4\4\b0\0\0\0\0\0\200\200\200\376\210\210\210\b\20\20 \0\0\0\0\0\0\0~\2\2\2\2\2\2\2\376\0\0\0\0\0DDD\376DDD\b\b\20 \0\0\0\0\0\0\340\20\2\342\22\4\4\b0\300\0\0\0\0\0\0~\2\2\4\4\b\24$B\202\0\0\0\0\0@@@^\342BD@@@>\0\0\0\0\0\0\202B\"\"\4\4\b\b\20 \0\0\0\0\0\0>\"\"R\214\4\n\20 @\0\0\0\0\0\0\4\30p\b\b\376\b\b\20 \0\0\0\0\0\0\"\22\222RBD\4\b\20 \0\0\0\0\0\0|\0\0\376\20\20\20\20 @\0\0\0\0\0\0@@@@pHD@@@\0\0\0\0\0\0\20\20\20\376\20\20\20\20 @\0\0\0\0\0\0\0\0|\0\0\0\0\0\0\376\0\0\0\0\0\0\376\2\2\4D(\20(D\200\0\0\0\0\0\0\20\20\376\2\4\b\0204\322\20\0\0\0\0\0\0\2\2\2\4\4\4\b\20 \300\0\0\0\0\0\0\20\20\bHDDD\202\202\202\0\0\0\0\0\0\200\200\216\360\200\200\200\200\200~\0\0\0\0\0\0\376\2\2\2\2\2\4\4\b0\0\0\0\0\0\0 PH\210\204\4\2\2\2\2\0\0\0\0\0\0\20\20\20\376\20\20T\222\222\20\0\0\0\0\0\0\376\2\2\4\4\210P \20\b\0\0\0\0\0\0\0p\f\2p\f\2\360\f\2\0\0\0\0\0\0\20\20\20  (DD\232\342\0\0\0\0\0\0\4\4\4\b\3100\30$B\200\0\0\0\0\0\0|\20\20\20\376\20\20\20\20\16\0\0\0\0\0\0@@.\362\"\24\20\20\b\b\0\0\0\0\0\0|\4\4\b\b\20\20\20\376\0\0\0\0\0\0\0\376\2\2\2\376\2\2\2\376\2\0\0\0\0\0\0\376\0\0\376\2\2\2\4\b0\0\0\0\0\0\0\202\202\202\202\202\2\4\4\b0\0\0\0\0\0\0PPPPPRR\224\224\230\0\0\0\0\0\0\200\200\200\200\200\202\202\204\230\340\0\0\0\0\0\0\376\202\202\202\202\202\202\202\376\202\0\0\0\0\0\0\376\202\202\202\2\2\4\4\b0\0\0\0\0\0\0\200@ \0\2\2\4\b0\300\0\0\0\0\0\0\220H$\0\0\0\0\0\0\0\0\0\0\0\0\0`\220\220`\0\0\0\0\0\0\0\0\0\0\0\0   \376 NB@\220\216\0\0\0\0\0\0  \374@X\344\202\2\2|\0\0\0\0\0\0\0\370\4\2\2\2\4\30`\0\0\0\0\0\0\0\376\4\b\20    \20\16\0\0\0\0\0\0@@@N0@\200\200\200~\0\0\0\0\0\0($\"\362H\2108LJ0\0\0\0\0\0\0\234\202\204\200\200\240\240\240\236\0\0\0\0\0\0\0\b\210\210|\322\262\226\252\252F\0\0\0\0\0\0  \3542\"bf\252\252&\0\0\0\0\0\0\08T\222\222\222\222\222d\0\0\0\0\0\0\0\204\204\276\204\204\204\234\246\244\230\0\0\0\0\0\0\350,JH\204\204\204\204\210p\0\0\0\0\0\08\b\20TT\212\212\212\2120\0\0\0\0\0\0\0\0\0\20(\304\2\0\0\0\0\0\0\0\0\0\276\204\204\276\204\204\234\246\244\230\0\0\0\0\0\0\20\20\376\20\376\20x\224\222`\0\0\0\0\0\0p\24\24>T\224\244\244D\b\0\0\0\0\0\0  \374 ,b\240\242b\34\0\0\0\0\0\0\4D\\*j\252\252\222\222d\0\0\0\0\0\0  \374 \374  \"\"\34\0\0\0\0\0\0\b\210H\\\342\"$\20\20\20\0\0\0\0\0\0\20\20\210\274\312\212\212\252\34 \0\0\0\0\0\0\20\20\20\36\20\20x\224\222`\0\0\0\0\0\0\3000\0\200\200\274\302\202\48\0\0\0\0\0\0\234\242\302\302\202\202\4\4\b0\0\0\0\0\0\0|\b\20 |\202:FB<\0\0\0\0\0\0  \3542\"b\244\250*$\0\0\0\0\0\0|\b\20 |\202\2\2\48\0\0\0\0\0\0   \3542bb\342\244(\0\0\0\0\0\0\20\20  @@b\222\222\214\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //romPatch ()
  //  IPLROMにパッチをあてる
  public static void romPatch () {

    //IPLROMのバグまたはおかしなところを修正する
    int patched = 0;
    int failed = 0;
    switch (romIPLVersion) {

    case 0:  //IPLROM1.0
      //
      //  電卓を使うと実行中のプログラムが誤動作することがある
      //    http://stdkmd.com/bugsx68k/#rom_dentakud3
      if (MainMemory.mmrRls (0x00ff32d8) == 0x48e76000) {
        MainMemory.mmrWb (0x00ff32d8 + 2, 0x70);  //00FF32D8  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff32f2 + 3, 0x0e);  //00FF32F2  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff32f8 + 3, 0x0e);  //00FF32F8  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff3342 + 2, 0x70);  //00FF3342  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff335e + 3, 0x0e);  //00FF335E  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        patched++;
      } else {
        failed++;
      }
      //
      //  カーソルが画面の最下行にあると電卓が画面外に表示される
      //    http://stdkmd.com/bugsx68k/#rom_dentaku64
      //    電卓の表示位置を決めるサブルーチンに電卓のY座標を調整するコードを追加する
      //    直前にある電卓を消すサブルーチンを詰めてできた隙間に電卓のY座標を調整するコードを押し込んでそれを呼び出す
      if (MainMemory.mmrRls (0x00ff3c6a) == 0x4fef000a) {
        MainMemory.mmrWwa (0x00ff3c6a,
                0x21fc, 0x00e6, 0x0000, 0x094c,  //00FF3C6A  4FEF000A              lea.l $000A(sp),sp            →  move.l #$00E60000,$094C.w
                //                                 00FF3C6E  23FC00E600000000094C  move.l #$00E60000,$0000094C.l →
                0x426f, 0x0008,                  //00FF3C72                                                      →  clr.w $0008(sp)
                0x6100, 0x2240,                  //00FF3C76                                                      →  bsr.w $00FF5EB8
                //                                 00FF3C78  4267                  clr.w -(sp)                   →
                0x4fef, 0x000a,                  //00FF3C7A  3F3C0010              move.w #$0010,-(sp)           →  lea.l $000A(sp),sp
                0x301f,                          //00FF3C7E  3F3C00B8              move.w #$00B8,-(sp)           →  move.w (sp)+,d0
                0x6704,                          //00FF3C80                                                      →  beq.s $00FF3C86
                0x6100, 0x633a,                  //00FF3C82  3F3900000BFE          move.w $00000BFE.l,-(sp)      →  bsr.w $00FF9FBE
                0x4cdf, 0x7ffe,                  //00FF3C86                                                      →  movem.l (sp)+,d1-d7/a0-a6
                //                                 00FF3C88  3F3900000BFC          move.w $00000BFC.l,-(sp)      →
                0x4e75,                          //00FF3C8A                                                      →  rts
                0x4e71,                          //00FF3C8C                                                      →  nop
                0xb27c, 0x001f,                  //00FF3C8E  61002228              bsr.w $00FF5EB8               →  cmp.w #$001F,d1
                0x630e,                          //00FF3C92  4FEF000A              lea.l $000A(sp),sp            →  bls.s $00FF3CA2
                0xb278, 0x0972,                  //00FF3C94                                                      →  cmp.w $0972.w,d1
                //                                 00FF3C96  301F                  move.w (sp)+,d0               →
                0x6308,                          //00FF3C98  6704                  beq.s $00FF3C9E               →  bls.s $00FF3CA2
                0x3238, 0x0972,                  //00FF3C9A  61006322              bsr.w $00FF9FBE               →  move.w $0972.w,d1
                0x6702,                          //00FF3C9E  4CDF7FFE              movem.l (sp)+,d1-d7/a0-a6     →  beq.s $00FF3CA2
                0x5341);                         //00FF3CA0                                                      →  subq.w #1,d1
        MainMemory.mmrWwa (0x00ff3cbc,
                0x3238, 0x0976,                  //00FF3CBC  323900000976          move.w $00000976.l,d1         →  move.w $0976.w,d1
                0x5241,                          //00FF3CC0                                                      →  addq.w #1,d1
                0x61ca);                         //00FF3CC2  5241                  addq.w #1,d1                  →  bsr.s $00FF3C8E
        patched++;
      } else {
        failed++;
      }
      //
      //  ソフトキーボードの↑キーの袋文字が閉じていない
      //    http://stdkmd.com/bugsx68k/#rom_softkeyboard
      if (MainMemory.mmrRwz (0x00ff540c) == 0x2b80) {
        MainMemory.mmrWb (0x00ff540c, 0x3b);  //00FF540C  2B80  .dc.b %00101011,%10000000  →  3B80  .dc.b %00111011,%10000000
        patched++;
      } else {
        failed++;
      }
      //
      //  _CRTMODが指定された画面モードと異なる色数でグラフィックパレットを初期化する
      //    http://stdkmd.com/bugsx68k/#rom_crtmod_gpalet
      if (MainMemory.mmrRwz (0x00ff658c) == 0x3039) {
        MainMemory.mmrWwa (0x00ff658c,
                0x700c,                          //00FF658C  moveq.l #$0C,d0
                0xc038, 0x093c,                  //00FF658E  and.b   $093C.w,d0
                0xe248,                          //00FF6592  lsr.w   #1,d0
                0x303b, 0x0010,                  //00FF6594  move.w  $00FF65A6(pc,d0.w),d0
                0x4ebb, 0x000c,                  //00FF6598  jsr     $00FF65A6(pc,d0.w)
                0x08f9, 0x0003, 0x00e8, 0x0028,  //00FF659C  bset.b  #$03,$00E80028
                0x4e75,                          //00FF65A4  rts
                0x45d0,                          //00FF65A6  .dc.w   $00FFAB76-$00FF65A6
                0x45d0,                          //00FF65A8  .dc.w   $00FFAB76-$00FF65A6
                0x45e6,                          //00FF65AA  .dc.w   $00FFAB8C-$00FF65A6
                0x45fe,                          //00FF65AC  .dc.w   $00FFABA4-$00FF65A6
                0x4e71,                          //00FF65AE  nop
                0x4e71,                          //00FF65B0  nop
                0x4e71,                          //00FF65B2  nop
                0x4e71,                          //00FF65B4  nop
                0x4e71);                         //00FF65B6  nop
        patched++;
      } else {
        failed++;
      }
      //
      //  _GPALETで65536色モードのパレットを正しく取得できない
      //    http://stdkmd.com/bugsx68k/#rom_gpalet
      if (MainMemory.mmrRls (0x00ffaecc) == 0x16300000) {
        MainMemory.mmrWb (0x00ffaecc + 3, 0x02);   //00FFAECC  16300000  move.b $00(a0,d0.w),d3  →  16300002  move.b $02(a0,d0.w),d3
        patched++;
      } else {
        failed++;
      }
      //
      break;

    case 1:  //IPLROM1.1
      //
      //  電卓を使うと実行中のプログラムが誤動作することがある
      //    http://stdkmd.com/bugsx68k/#rom_dentakud3
      if (MainMemory.mmrRls (0x00ff360e) == 0x48e76000) {
        MainMemory.mmrWb (0x00ff360e + 2, 0x70);  //00FF360E  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff3628 + 3, 0x0e);  //00FF3628  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff362e + 3, 0x0e);  //00FF362E  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff3678 + 2, 0x70);  //00FF3678  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff3694 + 3, 0x0e);  //00FF3694  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        patched++;
      } else {
        failed++;
      }
      //
      //  カーソルが画面の最下行にあると電卓が画面外に表示される
      //    http://stdkmd.com/bugsx68k/#rom_dentaku64
      //    電卓の表示位置を決めるサブルーチンに電卓のY座標を調整するコードを追加する
      //    直前にある電卓を消すサブルーチンを詰めてできた隙間に電卓のY座標を調整するコードを押し込んでそれを呼び出す
      if (MainMemory.mmrRls (0x00ff3fa0) == 0x4fef000a) {
        MainMemory.mmrWwa (0x00ff3fa0,
                0x21fc, 0x00e6, 0x0000, 0x094c,  //00FF3FA0  4FEF000A              lea.l $000A(sp),sp            →  move.l #$00E60000,$094C.w
                //                                 00FF3FA4  23FC00E600000000094C  move.l #$00E60000,$0000094C.l →
                0x426f, 0x0008,                  //00FF3FA8                                                      →  clr.w $0008(sp)
                0x6100, 0x2240,                  //00FF3FAC                                                      →  bsr.w $00FF61EE
                //                                 00FF3FAE  4267                  clr.w -(sp)                   →
                0x4fef, 0x000a,                  //00FF3FB0  3F3C0010              move.w #$0010,-(sp)           →  lea.l $000A(sp),sp
                0x301f,                          //00FF3FB4  3F3C00B8              move.w #$00B8,-(sp)           →  move.w (sp)+,d0
                0x6704,                          //00FF3FB6                                                      →  beq.s $00FF3FBC
                0x6100, 0x6384,                  //00FF3FB8  3F3900000BFE          move.w $00000BFE.l,-(sp)      →  bsr.w $00FFA33E
                0x4cdf, 0x7ffe,                  //00FF3FBC                                                      →  movem.l (sp)+,d1-d7/a0-a6
                //                                 00FF3FBE  3F3900000BFC          move.w $00000BFC.l,-(sp)      →
                0x4e75,                          //00FF3FC0                                                      →  rts
                0x4e71,                          //00FF3FC2                                                      →  nop
                0xb27c, 0x001f,                  //00FF3FC4  61002228              bsr.w $00FF61EE               →  cmp.w #$001F,d1
                0x630e,                          //00FF3FC8  4FEF000A              lea.l $000A(sp),sp            →  bls.s $00FF3FD8
                0xb278, 0x0972,                  //00FF3FCA                                                      →  cmp.w $0972.w,d1
                //                                 00FF3FCC  301F                  move.w (sp)+,d0               →
                0x6308,                          //00FF3FCE  6704                  beq.s $00FF3FD4               →  bls.s $00FF3FD8
                0x3238, 0x0972,                  //00FF3FD0  6100636C              bsr.w $00FFA33E               →  move.w $0972.w,d1
                0x6702,                          //00FF3FD4  4CDF7FFE              movem.l (sp)+,d1-d7/a0-a6     →  beq.s $00FF3FD8
                0x5341);                         //00FF3FD6                                                      →  subq.w #1,d1
        MainMemory.mmrWwa (0x00ff3ff2,
                0x3238, 0x0976,                  //00FF3FF2  323900000976          move.w $00000976.l,d1         →  move.w $0976.w,d1
                0x5241,                          //00FF3FF6                                                      →  addq.w #1,d1
                0x61ca);                         //00FF3FF8  5241                  addq.w #1,d1                  →  bsr.s $00FF3FC4
        patched++;
      } else {
        failed++;
      }
      //
      //  ソフトキーボードの↑キーの袋文字が閉じていない
      //    http://stdkmd.com/bugsx68k/#rom_softkeyboard
      if (MainMemory.mmrRwz (0x00ff5742) == 0x2b80) {
        MainMemory.mmrWb (0x00ff5742, 0x3b);  //00FF5742  2B80  .dc.b %00101011,%10000000  →  3B80  .dc.b %00111011,%10000000
        patched++;
      } else {
        failed++;
      }
      //
      //  _CRTMODが指定された画面モードと異なる色数でグラフィックパレットを初期化する
      //    http://stdkmd.com/bugsx68k/#rom_crtmod_gpalet
      if (MainMemory.mmrRwz (0x00ff68d4) == 0x3039) {
        MainMemory.mmrWwa (0x00ff68d4,
                0x700c,                          //00FF68D4  moveq.l #$0C,d0
                0xc038, 0x093c,                  //00FF68D6  and.b   $093C.w,d0
                0xe248,                          //00FF68DA  lsr.w   #1,d0
                0x303b, 0x0010,                  //00FF68DC  move.w  $00FF68EE(pc,d0.w),d0
                0x4ebb, 0x000c,                  //00FF68E0  jsr     $00FF68EE(pc,d0.w)
                0x08f9, 0x0003, 0x00e8, 0x0028,  //00FF68E4  bset.b  #$03,$00E80028
                0x4e75,                          //00FF68EC  rts
                0x45b4,                          //00FF68EE  .dc.w   $00FFAEA2-$00FF68EE
                0x45b4,                          //00FF68F0  .dc.w   $00FFAEA2-$00FF68EE
                0x45ca,                          //00FF68F2  .dc.w   $00FFAEB8-$00FF68EE
                0x45e2,                          //00FF68F4  .dc.w   $00FFAED0-$00FF68EE
                0x4e71,                          //00FF68F6  nop
                0x4e71,                          //00FF68F8  nop
                0x4e71,                          //00FF68FA  nop
                0x4e71,                          //00FF68FC  nop
                0x4e71);                         //00FF68FE  nop
        patched++;
      } else {
        failed++;
      }
      //
      //  IOCS _MS_PATSTが機能しない
      //    http://stdkmd.com/bugsx68k/#rom_mspatst
      //    ここではパターンアドレステーブルのオフセットを計算するlsl.w #2,d1の手前にd1.wを復元するmove.w $0AE6-$0A7A(a5),d1を押し込む
      //    設定後の11ワードは直後にある_MS_SELと同じなのでbra.sで飛ばせば10ワード確保できるのだが設定までの7ワードに入り切ってしまった
      if (MainMemory.mmrRwz (0x00ffa606) == 0xe549) {
        MainMemory.mmrWwa (0x00ffa606,
                0x7240,  //00FFA606  E549      lsl.w #2,d1                  →  7240          moveq.l #$40,d1
                0xd26d,  //00FFA608  41ED00F6  lea.l $0B70-$0A7A(a5),a0     →  D26D006C      add.w $0AE6-$0A7A(a5),d1
                0x006c,
                0xe549,  //00FFA60C  41F01000  lea.l $00(a0,d1.w),a0        →  E549          lsl.w #2,d1
                0x2bad,  //00FFA60E                                         →  2BAD007210F6  move.l $0AEC-$0A7A(a5),$0B70-$0A7A-$40*4(a5,d1.w)
                0x0072,  //00FFA610  20AD0072  move.l $0AEC-$0A7A(a5),(a0)  →
                0x10f6);
        patched++;
      } else {
        failed++;
      }
      //
      //  _GPALETで65536色モードのパレットを正しく取得できない
      //    http://stdkmd.com/bugsx68k/#rom_gpalet
      if (MainMemory.mmrRls (0x00ffb1f8) == 0x16300000) {
        MainMemory.mmrWb (0x00ffb1f8 + 3, 0x02);   //00FFB1F8  16300000  move.b $00(a0,d0.w),d3  →  16300002  move.b $02(a0,d0.w),d3
        patched++;
      } else {
        failed++;
      }
      //
      break;

    case 2:  //IPLROM1.2
      //
      //  電卓を使うと実行中のプログラムが誤動作することがある
      //    http://stdkmd.com/bugsx68k/#rom_dentakud3
      if (MainMemory.mmrRls (0x00ff36ca) == 0x48e76000) {
        MainMemory.mmrWb (0x00ff36ca + 2, 0x70);  //00FF36CA  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff36e4 + 3, 0x0e);  //00FF36E4  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff36ea + 3, 0x0e);  //00FF36EA  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        MainMemory.mmrWb (0x00ff3734 + 2, 0x70);  //00FF3734  48E76000  movem.l d1-d2,-(sp)  →  48E77000  movem.l d1-d3,-(sp)
        MainMemory.mmrWb (0x00ff3750 + 3, 0x0e);  //00FF3750  4CDF0006  movem.l (sp)+,d1-d2  →  4CDF000E  movem.l (sp)+,d1-d3
        patched++;
      } else {
        failed++;
      }
      //
      //  カーソルが画面の最下行にあると電卓が画面外に表示される
      //    http://stdkmd.com/bugsx68k/#rom_dentaku64
      //    電卓の表示位置を決めるサブルーチンに電卓のY座標を調整するコードを追加する
      //    直前にある電卓を消すサブルーチンを詰めてできた隙間に電卓のY座標を調整するコードを押し込んでそれを呼び出す
      if (MainMemory.mmrRls (0x00ff405c) == 0x4fef000a) {
        MainMemory.mmrWwa (0x00ff405c,
                0x21fc, 0x00e6, 0x0000, 0x094c,  //00FF405C  4FEF000A              lea.l $000A(sp),sp            →  move.l #$00E60000,$094C.w
                //                                 00FF4060  23FC00E600000000094C  move.l #$00E60000,$0000094C.l →
                0x426f, 0x0008,                  //00FF4064                                                      →  clr.w $0008(sp)
                0x6100, 0x2240,                  //00FF4068                                                      →  bsr.w $00FF62AA
                //                                 00FF406A  4267                  clr.w -(sp)                   →
                0x4fef, 0x000a,                  //00FF406C  3F3C0010              move.w #$0010,-(sp)           →  lea.l $000A(sp),sp
                0x301f,                          //00FF4070  3F3C00B8              move.w #$00B8,-(sp)           →  move.w (sp)+,d0
                0x6704,                          //00FF4072                                                      →  beq.s $00FF4078
                0x6100, 0x63e2,                  //00FF4074  3F3900000BFE          move.w $00000BFE.l,-(sp)      →  bsr.w $00FFA458
                0x4cdf, 0x7ffe,                  //00FF4078                                                      →  movem.l (sp)+,d1-d7/a0-a6
                //                                 00FF407A  3F3900000BFC          move.w $00000BFC.l,-(sp)      →
                0x4e75,                          //00FF407C                                                      →  rts
                0x4e71,                          //00FF407E                                                      →  nop
                0xb27c, 0x001f,                  //00FF4080  61002228              bsr.w $00FF62AA               →  cmp.w #$001F,d1
                0x630e,                          //00FF4084  4FEF000A              lea.l $000A(sp),sp            →  bls.s $00FF4094
                0xb278, 0x0972,                  //00FF4086                                                      →  cmp.w $0972.w,d1
                //                                 00FF4088  301F                  move.w (sp)+,d0               →
                0x6308,                          //00FF408A  6704                  beq.s $00FF4090               →  bls.s $00FF4094
                0x3238, 0x0972,                  //00FF408C  610063CA              bsr.w $00FFA458               →  move.w $0972.w,d1
                0x6702,                          //00FF4090  4CDF7FFE              movem.l (sp)+,d1-d7/a0-a6     →  beq.s $00FF4094
                0x5341);                         //00FF4092                                                      →  subq.w #1,d1
        MainMemory.mmrWwa (0x00ff40ae,
                0x3238, 0x0976,                  //00FF40AE  323900000976          move.w $00000976.l,d1         →  move.w $0976.w,d1
                0x5241,                          //00FF40B2                                                      →  addq.w #1,d1
                0x61ca);                         //00FF40B4  5241                  addq.w #1,d1                  →  bsr.s $00FF4080
        patched++;
      } else {
        failed++;
      }
      //
      //  ソフトキーボードの↑キーの袋文字が閉じていない
      //    http://stdkmd.com/bugsx68k/#rom_softkeyboard
      if (MainMemory.mmrRwz (0x00ff57fe) == 0x2b80) {
        MainMemory.mmrWb (0x00ff57fe, 0x3b);  //00FF57FE  2B80  .dc.b %00101011,%10000000  →  3B80  .dc.b %00111011,%10000000
        patched++;
      } else {
        failed++;
      }
      //
      //  _CRTMODが指定された画面モードと異なる色数でグラフィックパレットを初期化する
      //    http://stdkmd.com/bugsx68k/#rom_crtmod_gpalet
      if (MainMemory.mmrRwz (0x00ff69ac) == 0x3039) {
        MainMemory.mmrWwa (0x00ff69ac,
                0x700c,                          //00FF69AC  moveq.l #$0C,d0
                0xc038, 0x093c,                  //00FF69AE  and.b   $093C.w,d0
                0xe248,                          //00FF69B2  lsr.w   #1,d0
                0x303b, 0x0010,                  //00FF69B4  move.w  $00FF69C6(pc,d0.w),d0
                0x4ebb, 0x000c,                  //00FF69B8  jsr     $00FF69C6(pc,d0.w)
                0x08f9, 0x0003, 0x00e8, 0x0028,  //00FF69BC  bset.b  #$03,$00E80028
                0x4e75,                          //00FF69C4  rts
                0x45f6,                          //00FF69C6  .dc.w   $00FFAFBC-$00FF69C6
                0x45f6,                          //00FF69C8  .dc.w   $00FFAFBC-$00FF69C6
                0x460c,                          //00FF69CA  .dc.w   $00FFAFD2-$00FF69C6
                0x4624,                          //00FF69CC  .dc.w   $00FFAFEA-$00FF69C6
                0x4e71,                          //00FF69CE  nop
                0x4e71,                          //00FF69D0  nop
                0x4e71,                          //00FF69D2  nop
                0x4e71,                          //00FF69D4  nop
                0x4e71);                         //00FF69D6  nop
        patched++;
      } else {
        failed++;
      }
      //
      //  _GPALETで65536色モードのパレットを正しく取得できない
      //    http://stdkmd.com/bugsx68k/#rom_gpalet
      if (MainMemory.mmrRls (0x00ffb312) == 0x16300000) {
        MainMemory.mmrWb (0x00ffb312 + 3, 0x02);   //00FFB312  16300000  move.b $00(a0,d0.w),d3  →  16300002  move.b $02(a0,d0.w),d3
        patched++;
      } else {
        failed++;
      }
      //
      break;

    case 3:  //IPLROM1.3
      //パッチをあてる
      //  埋め込むコード
      for (int i = 0; i < ROM_PATCH13_DATA.length; ) {
        int a = ByteArray.byaRls (ROM_PATCH13_DATA, i);  //埋め込むアドレス(0=終了)
        if (a == 0) {
          break;
        }
        int l = ByteArray.byaRls (ROM_PATCH13_DATA, i + 4);  //埋め込むコードの長さ
        System.arraycopy (ROM_PATCH13_DATA, i + 8, MainMemory.mmrM8, a, l);
        i += 8 + l;
        patched++;
      }
      //  追加するコード
      System.arraycopy (ROM_PATCH13_TEXT, 0, MainMemory.mmrM8, ROM_PATCH13_BASE, ROM_PATCH13_TEXT.length);
      break;

    case 5:  //IPLROM1.5
      break;

    }  //switch romIPLVersion

    if (patched != 0) {
      prgMessage (new StringBuilder ().
                  append ("IPLROM 1.").
                  append ((char) ('0' + romIPLVersion)).
                  append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                  append (patched).
                  append ('/').
                  append (patched + failed).
                  append (')').toString ());
    }

  }  //romPatch()

  //romPatch2 ()
  //  ROMデバッガにパッチをあてる
  public static void romPatch2 () {

    //ROMデバッガのバグまたはおかしなところを修正する
    int patched = 0;
    int failed = 0;
    switch (romDebuggerVersion) {

    case -1:  //ROMデバッガがない
      int entry = romIPLVersion < 3 ? 0x00fe0000 : 0x00fd3800;  //エントリポイント
      MainMemory.mmrWwa (entry,
              0x48e7, 0xc080,                                       //00000000  48E7C080     movem.l d0-d1/a0,-(sp)
              0x41fa, 0x0012,                                       //00000004  41FA0012     lea.l   3f(pc),a0
              0x6004,                                               //00000008  6004         bra.s   2f
              0x7035, 0x4e4f,                                       //0000000A  70354E4F  1: IOCS    _OUT232C
              0x1218,                                               //0000000E  1218      2: move.b  (a0)+,d1
              0x66f8,                                               //00000010  66F8         bne.s   1b
              0x4cdf, 0x0103,                                       //00000012  4CDF0103     movem.l (sp)+,d0-d1/a0
              0x4e75);                                              //00000016  4E75         rts
      MainMemory.mmrWstr (entry + 0x0018, "ROM Debugger does not exist\r\n\0");  //00000018            3: .dc.b 'ROM Debugger does not exist',$0D,$0A,$00
      prgMessage (Multilingual.mlnJapanese ? "ROM デバッガはありません" : "ROM Debugger does not exist");
      return;

    case 0x0100:  //ROMデバッガ1.0
      //
      //  実効アドレスの計算で絶対ショートアドレスが符号拡張されない
      //    http://stdkmd.com/bugsx68k/#db_absoluteshort
      if (MainMemory.mmrRwz (0x00fe0a26) == 0x4240) {
        MainMemory.mmrWw (0x00fe0a26, 0x3015);  //00FE0A26  4240  clr.w d0        →  3015  move.w (a5),d0
        MainMemory.mmrWw (0x00fe0a28, 0x48c0);  //00FE0A26  3015  move.w (a5),d0  →  48C0  ext.l d0
        MainMemory.mmrWl (0x00fe0a40, 0x322dfffe);  //00FE0A40  4281      clr.l d1              →  322DFFFE  move.w -$0002(a5),d1
        //                                00FE0A42  222DFFFE  move.l -$0002(a5),d1
        MainMemory.mmrWw (0x00fe0a44, 0x48c1);      //00FE0A44                                  →  48C1      ext.l d1
        patched++;
      } else {
        failed++;
      }
      //
      //  CCRの未定義ビットを操作している
      //    http://stdkmd.com/bugsx68k/#db_ccrundefbit
      if (MainMemory.mmrRls (0x00fe2d10) == 0x023c007b) {
        MainMemory.mmrWb (0x00fe2d10 + 3, 0xfb);  //00FE2D10  023C007B  andi.b #$7B,ccr  →  023C00FB  andi.b #$FB,ccr
        patched++;
      } else {
        failed++;
      }
      //
      //  リモートターミナルからの入力が1文字置きになる
      //    http://stdkmd.com/bugsx68k/#db_ctrls
      //    ^Sのときだけ空読みする方法もあるがターミナルの仕様に合わないのでここでは^Sで一時停止する機能そのものを削除する
      //    ターミナルはスクロールすれば過去に出力されたメッセージを読むことができるので一時停止する機能は通常は必要ない
      //    1.0と2.32でオフセットが違うことに注意
      if (MainMemory.mmrRwz (0x00fe4ba6) == 0x6100) {
        MainMemory.mmrWw (0x00fe4ba6, 0x600e);  //00FE4BA6  610000EE  bsr.w ~FE4C96  →  600E  bra.s ~FE4BB6
        patched++;
      } else {
        failed++;
      }
      //
      break;

    case 0x0232:  //ROMデバッガ2.32
      //
      //  X68030でROMデバッガを有効にすると起動できない
      //    http://stdkmd.com/bugsx68k/#db_dirtya6
      //    ここではA6相対のディスプレースメントから$1000を引いて絶対ショートにする
      //    ..........101110 (d16,A6)
      //    ..........111000 (xxx).W
      //    ....110101...... (d16,A6)
      //    ....000111...... (xxx).W
      if (MainMemory.mmrRwz (0x00fd3834) == 0x2d4f) {
        MainMemory.mmrWl (0x00fd3834, 0x21cf1172);  //00FD3834  2D4F0172  move.l sp,$0172(a6)  →  21CF1172  move.l sp,$1172.w
        MainMemory.mmrWw (0x00fd3868, 0x21e8);  //00FD3868  2D680080059C  move.l $0080(a0),$059C(a6)  →  21E80080159C  move.l $0080(a0),$159C.w
        MainMemory.mmrWw (0x00fd3868 + 4, 0x159c);
        MainMemory.mmrWl (0x00fd3876, 0x40f81042);  //00FD3876  40EE0042  move.w sr,$0042(a6)  →  40F81042  move.w sr,$1042.w
        MainMemory.mmrWl (0x00fd387a, 0x21cf116a);  //00FD387A  2D4F016A  move.l sp,$016A(a6)  →  21CF116A  move.l sp,$116A.w
        MainMemory.mmrWw (0x00fd387e, 0x21fc);  //00FD387E  21FC00FDE4660046  move.l #$00FDE466,$0046(a6)  →  21FC00FDE4661046  move.l #$00FDE466,$1046.w
        MainMemory.mmrWw (0x00fd387e + 6, 0x1046);
        MainMemory.mmrWl (0x00fd3886, 0x4a781690);  //00FD3886  4A6E0690  tst.w $0690(a6)  →  4A781690  tst.w $1690.w
        MainMemory.mmrWw (0x00fd388c, 0x21fc);  //00FD388C  2D7C00FD46FA0176  move.l #$00FD46FA,$0176(a6)  →  21FC00FD46FA1176  move.l #$00FD46FA,$1176.w
        MainMemory.mmrWw (0x00fd388c + 6, 0x1176);
        MainMemory.mmrWw (0x00fd3896, 0x21fc);  //00FD3896  2D7C00FE0CE80176  move.l #$00FE0CE8,$0176(a6)  →  21FC00FE0CE81176  move.l #$00FE0CE8,$1176.w
        MainMemory.mmrWw (0x00fd3896 + 6, 0x1176);
        patched++;
      } else {
        failed++;
      }
      //
      //  FSINCOS.X FPm,FPc:FPs を正しくアセンブルできない
      //    http://stdkmd.com/bugsx68k/#db_fsincos
      if (MainMemory.mmrRwz (0x00fd6faa) == 0x8128) {
        MainMemory.mmrWw (0x00fd6faa, 0x812c);  //00FD6FAA  81280001  or.b d0,$0001(a0)  →  812C0001  or.b d0,$0001(a4)
        patched++;
      } else {
        failed++;
      }
      //
      //  リモートターミナルからの入力が1文字置きになる
      //    http://stdkmd.com/bugsx68k/#db_ctrls
      //    ^Sのときだけ空読みする方法もあるがターミナルの仕様に合わないのでここでは^Sで一時停止する機能そのものを削除する
      //    ターミナルはスクロールすれば過去に出力されたメッセージを読むことができるので一時停止する機能は通常は必要ない
      //    1.0と2.32でオフセットが違うことに注意
      if (MainMemory.mmrRwz (0x00fd8eb8) == 0x6100) {
        MainMemory.mmrWw (0x00fd8eb8, 0x6012);  //00FD8EB8  61000104  bsr.w $00FD8FBE  →  6012  bra.s $00FD8ECC
        patched++;
      } else {
        failed++;
      }
      //
      //  実効アドレスの計算でオペレーションサイズが.Q/.S/.D/.X/.Pのとき-(Ar)のオフセットが違う
      //    http://stdkmd.com/bugsx68k/#db_predecrement
      if (MainMemory.mmrRwz (0x00fda99c) == 0x0244) {
        MainMemory.mmrWwa (0x00fda99c,
                0x183b, 0x400a,   //00FDA99C  02440003      andi.w #$0003,d4       →  183B400A      move.b @f(pc,d4.w),d4
                0x90c4,           //00FDA9A0  D844          add.w d4,d4            →  90C4          suba.w d4,a0
                0x2d48, 0x0328,   //00FDA9A2  D0FB4008      adda.w @f(pc,d4.w),a0  →  2D480328      move.l a0,$0328(a6)
                0x4e75,           //00FDA9A6  2D480328      move.l a0,$0328(a6)    →  4E75          rts
                0x0201,           //00FDA9A8                                       →  0201      @@: .dc.b 2,1
                0x0204,           //00FDA9AA  4E75          rts                    →  0204          .dc.b 2,4
                0x0804,           //00FDA9AC  FFFE      @@: .dc.w -2               →  0804          .dc.b 8,4
                0x080c,           //00FDA9AE  FFFF      @@: .dc.w -1               →  080C          .dc.b 8,12
                0x0c00,           //00FDA9B0  FFFE          .dc.w -2               →  0C00          .dc.b 12,0
                0x0000);          //00FDA9B2  FFFC          .dc.w -4               →  0000          .dc.b 0,0
        patched++;
      } else {
        failed++;
      }
      //
      //  実効アドレスの計算で(d8,Ar,Rn.wl)/(d8,PC,Rn.wl)のd8が負数のときAr/PCがサプレスされる
      //    http://stdkmd.com/bugsx68k/#db_briefsuppress
      if (MainMemory.mmrRwz (0x00fda9c6) == 0x91c8) {
        MainMemory.mmrWwa (0x00fda9c6,
                0x4e71,   //00FDA9C6  91C8      suba.l a0,a0     →  4E71  nop
                0x4e71,   //00FDA9C8  4A2D0001  tst.b $0001(a5)  →  4E71  nop
                0x4e71,   //00FDA9CA                             →  4E71  nop
                0x4e71);  //00FDA9CC  6B04      bmi.s $00FDA9D2  →  4E71  nop
        MainMemory.mmrWwa (0x00fdab18,
                0x4e71,   //00FDAB18  91C8      suba.l a0,a0     →  4E71  nop
                0x4e71,   //00FDAB1A  4A2D0001  tst.b $0001(a5)  →  4E71  nop
                0x4e71,   //00FDAB1C                             →  4E71  nop
                0x4e71);  //00FDAB1E  6B02      bmi.s $00FDAB22  →  4E71  nop
        patched++;
      } else {
        failed++;
      }
      //
      //  実効アドレスの計算でスケールファクタを掛ける代わりにスケールファクタで割る
      //    http://stdkmd.com/bugsx68k/#db_scalefactor
      if (MainMemory.mmrRwz (0x00fdac3c) == 0xe2a8) {
        MainMemory.mmrWw (0x00fdac3c, 0xe3a8);  //00FDAC3C  E2A8  lsr.l d1,d0  →  E3A8  lsl.l d1,d0
        patched++;
      } else {
        failed++;
      }
      //
      //  実効アドレスの計算でインデックスレジスタの選択を間違える
      //    http://stdkmd.com/bugsx68k/#db_indexregister
      if (MainMemory.mmrRls (0x00fdac42) == 0x0240f000) {
        MainMemory.mmrWw (0x00fdac42 + 2, 0xf800);  //00FDAC42  0240F000  andi.w #$F000,d0  →  0240F800  andi.w #$F800,d0
        MainMemory.mmrWw (0x00fdac46, 0xed58);      //00FDAC46  EB58      rol.w #5,d0       →  ED58      rol.w #6,d0
        patched++;
      } else {
        failed++;
      }
      //
      //  実効アドレスの計算でインデックスサプレスが反映されない
      //    http://stdkmd.com/bugsx68k/#db_indexsuppress
      if (MainMemory.mmrRls (0x00fda9f8) == 0x61000238) {
        MainMemory.mmrWw (0x00fda9f8 + 2, 0x0256);  //00FDA9F8  61000238  bsr.w $00FDAC32  →  61000256  bsr.w $00FDAC50
        MainMemory.mmrWw (0x00fdaa5a + 2, 0x01f4);  //00FDAA5A  610001D6  bsr.w $00FDAC32  →  610001F4  bsr.w $00FDAC50
        MainMemory.mmrWw (0x00fdaaa0 + 2, 0x01ae);  //00FDAAA0  61000190  bsr.w $00FDAC32  →  610001AE  bsr.w $00FDAC50
        MainMemory.mmrWw (0x00fdab46 + 2, 0x0108);  //00FDAB46  610000EA  bsr.w $00FDAC32  →  61000108  bsr.w $00FDAC50
        MainMemory.mmrWw (0x00fdaba6 + 2, 0x00a8);  //00FDABA6  6100008A  bsr.w $00FDAC32  →  610000A8  bsr.w $00FDAC50
        MainMemory.mmrWw (0x00fdabfa, 0x6154);      //00FDABFA  6136      bsr.s $00FDAC32  →  6154      bsr.s $00FDAC50
        MainMemory.mmrWwa (0x00fdac48,
                0x303b, 0x0012,          //00FDAC48  303B0012      move.w $00FDAC5C(pc,d0.w),d0
                0x4efb, 0x000e,          //00FDAC4C  4EFB000E      jmp $00FDAC5C(pc,d0.w)
                0x7000,                  //00FDAC50  7000          moveq.l #0,d0
                0x082d, 0x0006, 0x0001,  //00FDAC52  082D00060001  btst.b #6,1(a5)
                0x67d8,                  //00FDAC58  67D8          beq.s $00FDAC32
                0x4e75,                  //00FDAC5A  4E75          rts
                0x0040, 0x0056,          //00FDAC5C  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC60  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC64  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC68  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC6C  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC70  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC74  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC78  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC7C  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC80  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC84  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC88  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC8C  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC90  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0040, 0x0056,          //00FDAC94  00400056      .dc.w $00FDAC9C-$00FDAC5C,$00FDACB2-$00FDAC5C
                0x0106, 0x013c,          //00FDAC98  0106013C      .dc.w $00FDAD62-$00FDAC5C,$00FDAD98-$00FDAC5C
                0x3015,                  //00FDAC9C  3015          move.w (a5),d0
                0xc07c, 0xf000,          //00FDAC9E  C07CF000      and.w #$F000,d0
                0xed58,                  //00FDACA2  ED58          rol.w #6,d0
                0x2f08,                  //00FDACA4  2F08          move.l a0,-(sp)
                0x3040,                  //00FDACA6  3040          move.w d0,a0
                0x3028, 0x1058,          //00FDACA8  30281058      move.w $1058(a0),d0
                0x205f,                  //00FDACAC  205F          movea.l (sp)+,a0
                0x48c0,                  //00FDACAE  48C0          ext.l d0
                0x4e75,                  //00FDACB0  4E75          rts
                0x3015,                  //00FDACB2  3015          move.w (a5),d0
                0xc07c, 0xf000,          //00FDACB4  C07CF000      and.w #$F000,d0
                0xed58,                  //00FDACB8  ED58          rol.w #6,d0
                0x2f08,                  //00FDACBA  2F08          move.l a0,-(sp)
                0x3040,                  //00FDACBC  3040          move.w d0,a0
                0x2028, 0x1056,          //00FDACBE  20281056      move.l $1056(a0),d0
                0x205f,                  //00FDACC2  205F          movea.l (sp)+,a0
                0x4e75);                 //00FDACC4  4E75          rts
        patched++;
      } else {
        failed++;
      }
      //
      //  例外発生時に浮動小数点レジスタを保存するときFSAVE/FRESTOREしていない
      //    http://stdkmd.com/bugsx68k/#db_fsavefrestore
      if (MainMemory.mmrRwz (0x00fe17cc) == 0x0000) {
        MainMemory.mmrWw (0x00fe17cc, 0x4e71);  //00FE17CC  0000  →  4E71  nop
        MainMemory.mmrWw (0x00fe17e0, 0x4e71);  //00FE17E0  0000  →  4E71  nop
        MainMemory.mmrWw (0x00fe17e2, 0xf35f);  //00FE17E2  F379  →  F35F  frestore (sp)+
        patched++;
      } else {
        failed++;
      }
      //
      //  浮動小数点数の10の累乗のテーブルの誤差が大きい
      //    http://stdkmd.com/bugsx68k/#db_base10
      if (MainMemory.mmrRls (0x00fe2dee + 8) == 0xa0000005) {
        MainMemory.mmrWla (0x00fe2dee,
                0x40300000, 0xe35fa931, 0xa0000000,
                0x402d0000, 0xb5e620f4, 0x80000000,
                0x402a0000, 0x9184e72a, 0x00000000,
                0x40260000, 0xe8d4a510, 0x00000000,
                0x40230000, 0xba43b740, 0x00000000,
                0x40200000, 0x9502f900, 0x00000000,
                0x401c0000, 0xee6b2800, 0x00000000,
                0x40190000, 0xbebc2000, 0x00000000,
                0x40160000, 0x98968000, 0x00000000,
                0x40120000, 0xf4240000, 0x00000000,
                0x400f0000, 0xc3500000, 0x00000000,
                0x400c0000, 0x9c400000, 0x00000000,
                0x40080000, 0xfa000000, 0x00000000,
                0x40050000, 0xc8000000, 0x00000000,
                0x40020000, 0xa0000000, 0x00000000,
                0x3fff0000, 0x80000000, 0x00000000,
                0x3ffb0000, 0xcccccccc, 0xcccccccd,
                0x3ff80000, 0xa3d70a3d, 0x70a3d70a,
                0x3ff50000, 0x83126e97, 0x8d4fdf3b,
                0x3ff10000, 0xd1b71758, 0xe219652c,
                0x3fee0000, 0xa7c5ac47, 0x1b478423,
                0x3feb0000, 0x8637bd05, 0xaf6c69b6,
                0x3fe70000, 0xd6bf94d5, 0xe57a42bc,
                0x3fe40000, 0xabcc7711, 0x8461cefd,
                0x3fe10000, 0x89705f41, 0x36b4a597,
                0x3fdd0000, 0xdbe6fece, 0xbdedd5bf,
                0x3fda0000, 0xafebff0b, 0xcb24aaff,
                0x3fd70000, 0x8cbccc09, 0x6f5088cc,
                0x3fd30000, 0xe12e1342, 0x4bb40e13,
                0x3fd00000, 0xb424dc35, 0x095cd80f,
                0x3fcd0000, 0x901d7cf7, 0x3ab0acd9,
                0x3fc90000, 0xe69594be, 0xc44de15b);
        patched++;
      } else {
        failed++;
      }
      //
      //  CCRの未定義ビットを操作している
      //    http://stdkmd.com/bugsx68k/#db_ccrundefbit
      if (MainMemory.mmrRls (0x00fde432) == 0x023c007b) {
        MainMemory.mmrWb (0x00fde432 + 3, 0xfb);  //00FDE432  023C007B  andi.b #$7B,ccr  →  023C00FB  andi.b #$FB,ccr
        MainMemory.mmrWb (0x00fe00d4 + 3, 0xfe);  //00FE00D4  023C007E  andi.b #$7E,ccr  →  023C00FE  andi.b #$FE,ccr
        MainMemory.mmrWb (0x00fe0520 + 3, 0xfe);  //00FE0520  023C007E  andi.b #$7E,ccr  →  023C00FE  andi.b #$FE,ccr
        patched++;
      } else {
        failed++;
      }
      //
      break;

    }  //switch romDebuggerVersion

    if (patched != 0) {
      prgMessage (new StringBuilder ().
                  append (Multilingual.mlnJapanese ? "ROM デバッガ " : "ROM Debugger ").
                  append ((char) ('0' + (romDebuggerVersion >> 8 & 15))).
                  append ('.').
                  append ((char) ('0' + (romDebuggerVersion >> 4 & 15))).
                  append ((char) ('0' + (romDebuggerVersion & 15))).
                  append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                  append (patched).
                  append ('/').
                  append (patched + failed).
                  append (')').toString ());
    }

  }  //romPatch2()

  //romPatch3 ()
  //  ROM Humanにパッチをあてる
  public static void romPatch3 () {

    //ROM Humanのバグまたはおかしなところを修正する
    int patched = 0;
    int failed = 0;
    switch (romHumanVersion) {

    case 0x0215:  //ROM Human 2.15
      //RAMまたはROMから起動してDISK2HDを初期化するときリクエストヘッダの初期化コマンドを設定していない(human215,human301,human302)
      //
      //x形式実行ファイルのメモリアロケーションモードが必要最小ブロックかどうかを確認するビット番号が間違っている(human215,human301,human302)
      //  00FC3476  08010001      btst.l  #$01,d1       →  00FC3476  08010000      btst.l  #$00,d1
      if (MainMemory.mmrRls (0x00fc3476) == 0x08010001) {
        MainMemory.mmrWb (0x00fc3476 + 3, 0x00);
        patched++;
      } else {
        failed++;
      }
      //
      //仮想ディレクトリを展開して実体のドライブに移るときドライブ管理テーブルのアドレスを変更する命令のオペレーションサイズが間違っている(human302)
      //
      //ディレクトリを延長するときルートディレクトリかどうかを判断するためにセクタ番号をデータ部の先頭セクタ番号と比較するとき上位ワードを無視している(human215,human301,human302)
      //                                                →  xxxxxxxx  7000          moveq.l #$00,d0
      //  00FC531C  30280014      move.w  $0014(a0),d0
      //  00FC5320  B240          cmp.w   d0,d1         →  00FC5320  B280          cmp.l   d0,d1
      //  00FC5322  6406          bcc.s   $00FC532A
      //  00FC5324  5241          addq.w  #$01,d1       →  00FC5324  5281          addq.l  #$01,d1
      //  00FC5326  B240          cmp.w   d0,d1         →  00FC5326  B280          cmp.l   d0,d1
      //  00FC5328  4E75          rts
      //
      //FILESのバッファのアドレスのbit31がセットされているとき拡張部分をコピーするループのループカウンタのレジスタが間違っている(human215)
      //  00FC565A  7255          moveq.l #$55,d1
      //  00FC565C  12D8          move.b  (a0)+,(a1)+
      //  00FC565E  51C8FFFC      dbra.w  d0,$00FC565C  →  00FC565E  51C9FFFC      dbra.w  d1,$00FC565C
      if (MainMemory.mmrRls (0x00fc565e) == 0x51c8fffc) {
        MainMemory.mmrWb (0x00fc565e + 1, 0xc9);
        patched++;
      } else {
        failed++;
      }
      //
      //リモートデバイスに対するchmodコマンドのコマンド番号が間違っている(human215)
      //  00FC7264  7057          moveq.l #$57,d0       →  00FC7264  7046          moveq.l #$46,d0
      if (MainMemory.mmrRwz (0x00fc7264) == 0x7057) {
        MainMemory.mmrWb (0x00fc7264 + 1, 0x46);
        patched++;
      } else {
        failed++;
      }
      //
      //サブのメモリ空間を削除するときサブの管理下で常駐したブロックをメインのメモリ空間からサブのメモリ空間に入る方向に繋いでいない(human215,human301,human302)
      //
      //スレッドを切り替えるためのTimer-D割り込みルーチンがMC68030のコプロセッサ命令途中割り込みに対応していない(human215,human301,human302)
      //
      //IOCTRL(19,1)でBPBテーブルをコピーする長さとPDAとイジェクトフラグを書き込む位置が間違っている(human215,human301,human302)
      //  00FCA504  700B          moveq.l #$0B,d0       →  00FCA504  700F          moveq.l #$0F,d0
      //  00FCA506  10DE          move.b  (a6)+,(a0)+
      //  00FCA508  51C8FFFC      dbra.w  d0,$00FCA506
      if (MainMemory.mmrRwz (0x00fca504) == 0x700b) {
        MainMemory.mmrWb (0x00fca504 + 1, 0x0f);
        patched++;
      } else {
        failed++;
      }
      //
      //IOCTRL(19,0)でBPBテーブルのハンドルをBPBテーブルのアドレスとして参照しようとしている(human215)
      //  00FCA520  61000084      bsr.w   $00FCA5A6
      //  00FCA524  206D000E      movea.l $000E(a5),a0
      //  ;BPBテーブルのハンドルを求めるときにd0.w=(d0.w&3)*4を計算しているのでd0.wの上位バイトは既に0になっている
      //  00FCA528  4240          clr.w   d0            →  00FCA528  2C56          movea.l (a6),a6
      //  00FCA52A  102E000A      move.b  $000A(a6),d0
      //  00FCA52E  3080          move.w  d0,(a0)
      if (MainMemory.mmrRwz (0x00fca528) == 0x4240) {
        MainMemory.mmrWw (0x00fca528 + 0, 0x2c56);
        patched++;
      } else {
        failed++;
      }
      //
      break;

    }  //switch romHumanVersion

    if (patched != 0) {
      prgMessage (new StringBuilder ().
                  append ("ROM Human ").
                  append ((char) ('0' + (romHumanVersion >> 8 & 15))).
                  append ('.').
                  append ((char) ('0' + (romHumanVersion >> 4 & 15))).
                  append ((char) ('0' + (romHumanVersion & 15))).
                  append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                  append (patched).
                  append ('/').
                  append (patched + failed).
                  append (')').toString ());
    }

  }  //romPatch3()

  //romSetROMDBOn (on)
  //  ROMデバッガをON/OFFする
  //  SRAMの初期化データに細工をする方法
  //    SRAMを初期化する必要がある
  //    MainMemory.mmrWb (sramInitialData + 0x58, romROMDBOn ? 0xff : 0x00)  //ROMデバッガ起動フラグの初期値
  //  SRAMのROMデバッガ起動フラグを書き換える方法
  //    SRAMの内容が変化する
  //    MainMemory.mmrWb (0x00ed0058, romROMDBOn ? 0xff : 0x00);  //ROMデバッガ起動フラグ
  //  IPLのコードを書き換える方法
  //    SRAMの内容が変化しない
  //    無条件にROMデバッガを起動することができる
  //    IPLのバージョンによって書き換える場所が異なる
  //    IPLROM1.0
  //        00FF00CE    clr.b   $000009DE.l         ;ROMデバッガ起動済みフラグ($00=未起動,$01=起動済み)をクリア
  //        00FF00D4    move.b  $0000080E.l,d0      ;NUM|OPT.2|OPT.1|CTRL|SHIFTキー押し下げフラグ
  //        00FF00DA    move.b  $00ED0058,d1        ;ROMデバッガ起動フラグ($00=起動しない,$FF=起動する)
  //        00FF00E0    eor.b   d1,d0
  //        00FF00E2    btst.l  #$03,d0             ;OPT.2キー押し下げフラグとROMデバッガ起動フラグの状態が同じときは
  //        00FF00E6    beq.s   $00FF00F6           ;ROMデバッガを起動しない
  //        00FF00E8    move.b  #$01,$000009DE.l    ;ROMデバッガ起動済みフラグ($00=未起動,$01=起動済み)をセット
  //        00FF00F0    jsr     $00FE0000           ;ROMデバッガを起動する
  //        00FF00F6
  //      0x00ff00e6のbeq.sをnopに変更する
  //    IPLROM1.1
  //      アドレスがずれるだけで該当部分のコードはIPLROM1.0と同じ
  //      0x00ff0188のbeq.sをnopに変更する
  //    IPLROM1.2
  //      アドレスがずれるだけで該当部分のコードはIPLROM1.0と同じ
  //      0x00ff01d4のbeq.sをnopに変更する
  //    IPLROM1.3
  //        00FF0286    clr.b   $09DE.w             ;ROMデバッガ起動済みフラグ($00=未起動,$01=起動済み)をクリア
  //        00FF028A    move.b  $080E.w,d0          ;NUM|OPT.2|OPT.1|CTRL|SHIFTキー押し下げフラグ
  //        00FF028E    move.b  $00ED0058,d1        ;ROMデバッガ起動フラグ($00=起動しない,$FF=起動する)
  //        00FF0294    eor.b   d1,d0
  //        00FF0296    btst.l  #$03,d0             ;OPT.2キー押し下げフラグとROMデバッガ起動フラグの状態が同じときは
  //        00FF029A    beq.s   $00FF02A8           ;ROMデバッガを起動しない
  //        00FF029C    move.b  #$01,$09DE.w        ;ROMデバッガ起動済みフラグ($00=未起動,$01=起動済み)をセット
  //        00FF02A2    movea.l $00FF0008(pc),a0    ;ROMデバッガエントリ
  //        00FF02A6    jsr     (a0)                ;ROMデバッガを起動する
  //        00FF02A8
  //      IPLROM1.0との違いは絶対ロングが絶対ショートになったこととROMデバッガエントリが間接参照になったこと
  //      0x00ff029aのbeq.sをnopに変更する
  public static void romSetROMDBOn (boolean on) {
    switch (romIPLVersion) {
    case 0:  //IPLROM1.0
      MainMemory.mmrWw (0x00ff00e6, on ? 0x4e71 : 0x670e);  //nop/beq.s
      break;
    case 1:  //IPLROM1.1
      MainMemory.mmrWw (0x00ff0188, on ? 0x4e71 : 0x670e);  //nop/beq.s
      break;
    case 2:  //IPLROM1.2
      MainMemory.mmrWw (0x00ff01d4, on ? 0x4e71 : 0x670e);  //nop/beq.s
      break;
    case 3:  //IPLROM1.3
    case 5:  //IPLROM1.5
      MainMemory.mmrWw (0x00ff029a, on ? 0x4e71 : 0x670c);  //nop/beq.s
      break;
    }
    romROMDBOn = on;
  }  //romSetROMDBOn(boolean)

  //romCreateCG1 ()
  //  CG1が最初にアクセスされたときフォントを作る
  public static void romCreateCG1 () {
    if ((romCGROMRequired & FNT_MASK_CG1) != 0) {
      prgMessage (Multilingual.mlnJapanese ? "CGROM1 を作ります" : "Creating CGROM1");
      if ((romCGROMRequired & FNT_MASK_KNJ16X16) != 0) {
        romCreateFont (FNT_IMAGE_KNJ16X16, MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16, romGothic16Font, 16, 16, CharacterCode.CHR_KANJI_BASE);
        romCGROMRequired &= ~FNT_MASK_KNJ16X16;
      }
      if ((romCGROMRequired & FNT_MASK_ANK8X8) != 0) {
        romCreateFont (FNT_IMAGE_ANK8X8, MainMemory.mmrM8, FNT_ADDRESS_ANK8X8, romGothic8Font, 8, 8, ROM_ANK_HALFWIDTH_BASE, ROM_ANK_HIRAGANA_BASE);
        romCGROMRequired &= ~FNT_MASK_ANK8X8;
      }
      if ((romCGROMRequired & FNT_MASK_ANK8X16) != 0) {
        romCreateFont (FNT_IMAGE_ANK8X16, MainMemory.mmrM8, FNT_ADDRESS_ANK8X16, romGothic16Font, 8, 16, ROM_ANK_HALFWIDTH_BASE, ROM_ANK_HIRAGANA_BASE);
        romCGROMRequired &= ~FNT_MASK_ANK8X16;
      }
      if ((romCGROMRequired & FNT_MASK_ANK12X12) != 0) {
        romCreateFont (FNT_IMAGE_ANK12X12, MainMemory.mmrM8, FNT_ADDRESS_ANK12X12, romGothic12Font, 12, 12, ROM_ANK_FULLWIDTH_BASE);
        romCGROMRequired &= ~FNT_MASK_ANK12X12;
      }
      if ((romCGROMRequired & FNT_MASK_ANK12X24) != 0) {
        romCreateFont (FNT_IMAGE_ANK12X24, MainMemory.mmrM8, FNT_ADDRESS_ANK12X24, romMincho24Font, 12, 24, ROM_ANK_HALFWIDTH_BASE, ROM_ANK_HIRAGANA_BASE);
        romCGROMRequired &= ~FNT_MASK_ANK12X24;
      }
    }
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00f00000, 0x00f40000);
  }  //romCreateCG1()

  //romCreateCG2 ()
  //  CG2が最初にアクセスされたときフォントを作る
  public static void romCreateCG2 () {
    if ((romCGROMRequired & FNT_MASK_CG2) != 0) {
      prgMessage (Multilingual.mlnJapanese ? "CGROM2 を作ります" : "Creating CGROM2");
      long t0 = System.nanoTime ();
      if ((romCGROMRequired & FNT_MASK_KNJ24X24) != 0) {
        romCreateFont (FNT_IMAGE_KNJ24X24, MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24, romMincho24Font, 24, 24, CharacterCode.CHR_KANJI_BASE);
        romCGROMRequired &= ~FNT_MASK_KNJ24X24;
      }
      if ((romCGROMRequired & FNT_MASK_ANK6X12) != 0) {
        romCreateFont (FNT_IMAGE_ANK6X12, MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, romGothic12Font, 6, 12, ROM_ANK_HALFWIDTH_BASE, ROM_ANK_HIRAGANA_BASE);
        romCGROMRequired &= ~FNT_MASK_ANK6X12;
      }
      long t1 = System.nanoTime ();
      prgMessage ((t1 - t0) / 1000000 + "ms");
    }
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00f40000, 0x00fc0000);
  }  //romCreateCG2()

  //romCreateFont (name, bb, index, font, width, height, bases, ...)
  //  フォントを作る
  public static void romCreateFont (String name, byte[] bb, int index, Font font, int width, int height, String[]... bases) {
    int cols = bases[0][0].length ();
    int rows = bases[0].length;
    //黒い紙を用意する
    byte[] r = new byte[] { 0x00, 0x55, (byte) 0xaa, (byte) 0xff };
    Color[] color = new Color[4];
    for (int i = 0; i < 4; i++) {
      color[i] = new Color (r[i] & 255, r[i] & 255, r[i] & 255);
    }
    int fontImageWidth = width * cols;
    int fontImageHeight = height * rows;
    BufferedImage image = new BufferedImage (fontImageWidth, fontImageHeight,
                                             BufferedImage.TYPE_BYTE_BINARY,
                                             new IndexColorModel (2, 4, r, r, r));
    byte[] fontBitmap = ((DataBufferByte) image.getRaster ().getDataBuffer ()).getData ();
    Graphics2D g2 = (Graphics2D) image.getGraphics ();
    if (false) {  //背景を黒で塗り潰す
      g2.setColor (color[0]);
      g2.fillRect (0, 0, fontImageWidth, fontImageHeight);
    } else {  //背景を市松模様に塗る。フォントが枠からはみ出していないか確認できる
      for (int row = 0; row < rows; row++) {
        int y = height * row;
        for (int col = 0; col < cols; col++) {
          int x = width * col;
          g2.setColor (color[(col ^ row) & 0x01]);
          g2.fillRect (x, y, width, height);
        }
      }
    }
    //白い文字を描く
    g2.setColor (color[3]);
    g2.setFont (font);
    AffineTransform savedTransform = g2.getTransform ();
    for (String[] base : bases) {
      //半角の場合はFULL BLOCK、全角の場合は罫線の十字を使って位置と幅を調整する
      Rectangle2D rect = new TextLayout (base[0].charAt (0) < 0x80 ? "\u2588" : "┼",
                                         font,
                                         g2.getFontRenderContext ()).getBounds ();
      double ox = rect.getX ();  //x方向のオフセット
      double oy = rect.getY ();  //y方向のオフセット
      double sx = rect.getWidth () / (double) width;  //表示すべき幅を1としたときのフォントの幅。全角フォントを横に潰して半角フォントを作るとき2
      double sy = rect.getHeight () / (double) height;  //表示すべき幅を1としたときのフォントの高さ。半角フォントを縦に潰して全角フォントを作るとき2
      g2.scale (1.0 / sx, 1.0 / sy);
      for (int row = 0; row < rows; row++) {
        String line = base[row];
        double y = (double) (height * row) * sy - oy;
        if (false) {  //1行まとめて描く
          g2.drawString (line, (float) -ox, (float) y);
        } else {  //1文字ずつ描く。等幅フォントがなくても読めるかもしれない
          for (int col = 0; col < cols; col++) {
            double x = (double) (width * col) * sx - ox;
            g2.drawString (line.substring (col, col + 1), (float) x, (float) y);
          }
        }
      }
      g2.setTransform (savedTransform);
    }
    //イメージをフォントに変換する
    int step = (width + 7) >> 3;  //幅のバイト数
    int gap = 8 * step - width;  //右側の空きビットの数。0～7
    for (int row = 0; row < rows; row++) {
      int y = height * row;
      for (int col = 0; col < cols; col++) {
        int x = width * col;
        for (int dy = 0; dy < height; dy++) {
          int t = 0;
          if (true) {  //getRGBを使わない。すべてのフォントを作るのに約1.1秒かかる
            int o = x + fontImageWidth * (y + dy);
            for (int dx = 0; dx < width; dx++) {
              t = t << 1 | fontBitmap[o + dx >> 2] >> 7 - ((o + dx & 0x03) << 1) & 0x01;  //上位から2ビットずつ充填されている。上位ビットを使う
            }
            t <<= gap;
          } else {  //getRGBを使う。すべてのフォントを作るのに約1.6秒かかる
            for (int dx = 0; dx < width; dx++) {
              t = t << 1 | 0x80 & image.getRGB (x + dx, y + dy);  //青成分のbit7を使う。幅は24ドットまでなので収まる
            }
            t >>>= 7 - gap;
          }
          if (step >= 3) {
            bb[index++] = (byte) (t >> 16);
          }
          if (step >= 2) {
            bb[index++] = (byte) (t >> 8);
          }
          bb[index++] = (byte) t;
        }
      }
    }
  }  //romCreateFont(String,byte[],int,Font,int,int,String[]...)



  //========================================================================================
  //$$FNT フォント

  //ページの名前
  public static final String FNT_EN_ANK8X8   = "Quarter-square ANK 8x8";
  public static final String FNT_JA_ANK8X8   = "1/4角 ANK 8x8";
  public static final String FNT_EN_ANK12X12 = "Quarter-square ANK 12x12";
  public static final String FNT_JA_ANK12X12 = "1/4角 ANK 12x12";
  public static final String FNT_EN_ANK6X12  = "Half-width ANK 6x12";
  public static final String FNT_JA_ANK6X12  = "半角 ANK 6x12";
  public static final String FNT_EN_ANK8X16  = "Half-width ANK 8x16";
  public static final String FNT_JA_ANK8X16  = "半角 ANK 8x16";
  public static final String FNT_EN_ANK12X24 = "Half-width ANK 12x24";
  public static final String FNT_JA_ANK12X24 = "半角 ANK 12x24";
  public static final String FNT_EN_ANK16X32 = "Half-width ANK 16x32 *";
  public static final String FNT_JA_ANK16X32 = "半角 ANK 16x32 *";
  public static final String FNT_EN_KNJ8X8   = "Full-width Kanji 8x8 *";
  public static final String FNT_JA_KNJ8X8   = "全角 漢字 8x8 *";
  public static final String FNT_EN_KNJ12X12 = "Full-width Kanji 12x12 *";
  public static final String FNT_JA_KNJ12X12 = "全角 漢字 12x12 *";
  public static final String FNT_EN_KNJ16X16 = "Full-width Kanji 16x16";
  public static final String FNT_JA_KNJ16X16 = "全角 漢字 16x16";
  public static final String FNT_EN_KNJ24X24 = "Full-width Kanji 24x24";
  public static final String FNT_JA_KNJ24X24 = "全角 漢字 24x24";
  public static final String FNT_EN_KNJ32X32 = "Full-width Kanji 32x32 *";
  public static final String FNT_JA_KNJ32X32 = "全角 漢字 32x32 *";

  //デフォルトのイメージファイル名
  public static final String FNT_IMAGE_ANK8X8   = "ank8x8.png";
  public static final String FNT_IMAGE_ANK12X12 = "ank12x12.png";
  public static final String FNT_IMAGE_ANK6X12  = "ank6x12.png";
  public static final String FNT_IMAGE_ANK8X16  = "ank8x16.png";
  public static final String FNT_IMAGE_ANK12X24 = "ank12x24.png";
  public static final String FNT_IMAGE_ANK16X32 = "ank16x32.png";
  public static final String FNT_IMAGE_KNJ8X8   = "knj8x8.png";
  public static final String FNT_IMAGE_KNJ12X12 = "knj12x12.png";
  public static final String FNT_IMAGE_KNJ16X16 = "knj16x16.png";
  public static final String FNT_IMAGE_KNJ24X24 = "knj24x24.png";
  public static final String FNT_IMAGE_KNJ32X32 = "knj32x32.png";

  //種類
  public static final int FNT_TYPE_ANK = 0;  //ANK。16桁×16行
  public static final int FNT_TYPE_KNJ = 1;  //漢字。94点×74区(1区～8区,16区～84区)

  public static final int FNT_MASK_ANK8X8   = 1 << 0;  //1/4角 ANK 8x8
  public static final int FNT_MASK_ANK12X12 = 1 << 1;  //1/4角 ANK 12x12
  public static final int FNT_MASK_ANK6X12  = 1 << 2;  //半角 ANK 6x12
  public static final int FNT_MASK_ANK8X16  = 1 << 3;  //半角 ANK 8x16
  public static final int FNT_MASK_ANK12X24 = 1 << 4;  //半角 ANK 12x24
  public static final int FNT_MASK_ANK16X32 = 1 << 5;  //半角 ANK 16x32 *
  public static final int FNT_MASK_KNJ8X8   = 1 << 6;  //全角 漢字 8x8 *
  public static final int FNT_MASK_KNJ12X12 = 1 << 7;  //全角 漢字 12x12 *
  public static final int FNT_MASK_KNJ16X16 = 1 << 8;  //全角 漢字 16x16
  public static final int FNT_MASK_KNJ24X24 = 1 << 9;  //全角 漢字 24x24
  public static final int FNT_MASK_KNJ32X32 = 1 << 10;  //全角 漢字 32x32 *
  public static final int FNT_MASK_CG1 = FNT_MASK_KNJ16X16 | FNT_MASK_ANK8X8 | FNT_MASK_ANK8X16 | FNT_MASK_ANK12X12 | FNT_MASK_ANK12X24;
  public static final int FNT_MASK_CG2 = FNT_MASK_KNJ24X24 | FNT_MASK_ANK6X12;
  public static final int FNT_MASK_CGROM = FNT_MASK_CG1 | FNT_MASK_CG2;
  public static final int FNT_MASK_ALL = FNT_MASK_CGROM | FNT_MASK_ANK16X32 | FNT_MASK_KNJ8X8 | FNT_MASK_KNJ12X12 | FNT_MASK_KNJ32X32;

  //アドレス
  public static final int FNT_ADDRESS_KNJ16X16 = 0x00f00000;
  public static final int FNT_ADDRESS_ANK8X8   = 0x00f3a000;
  public static final int FNT_ADDRESS_ANK8X16  = 0x00f3a800;
  public static final int FNT_ADDRESS_ANK12X12 = 0x00f3b800;
  public static final int FNT_ADDRESS_ANK12X24 = 0x00f3d000;
  public static final int FNT_ADDRESS_KNJ24X24 = 0x00f40000;
  public static final int FNT_ADDRESS_ANK6X12  = 0x00fbf400;

  //デフォルトのCGROMファイル名
  public static final String FNT_CGROM = "./CGROM.TMP";

  //全角→半角変換
  //  0x82のU+00A6 BROKEN BARはJIS X 0213:2000以降で9区の0x8544に割り当てられているが、X68000のCGROMには9区が存在しない
  public static final char[] FNT_CONTROL_BASE = (
    //0+1+2+3+4+5+6+7+8+9+A+B+C+D+E+F
    "  SHSXEXETEQAKBLBSHTLFVTFFCRSOSI" +  //0x
    "DED1D2D3D4NKSNEBCNEMSBEC        "    //1x
    ).toCharArray ();
  public static final char[] FNT_ANK_FULLWIDTH_BASE = (
    //0+1+2+3+4+5+6+7+8+9+A+B+C+D+E+F
    "　　　　　　　　　　　　　　　　" +  //0x
    "　　　　　　　　　　　　→←↑↓" +  //1x
    "　！”＃＄％＆’（）＊＋，－．／" +  //2x
    "０１２３４５６７８９：；＜＝＞？" +  //3x
    "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ" +  //4x
    "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿" +  //5x
    "｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ" +  //6x
    "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝￣　" +  //7x
    "＼～￤　　　をぁぃぅぇぉゃゅょっ" +  //8x
    "　あいうえおかきくけこさしすせそ" +  //9x
    "　。「」、・ヲァィゥェォャュョッ" +  //Ax
    "ーアイウエオカキクケコサシスセソ" +  //Bx
    "タチツテトナニヌネノハヒフヘホマ" +  //Cx
    "ミムメモヤユヨラリルレロワン゛゜" +  //Dx
    "たちつてとなにぬねのはひふへほま" +  //Ex
    "みむめもやゆよらりるれろわん　　"    //Fx
    ).toCharArray ();

  //編集モード
  public static final int FNT_MODE_BROWSE    = 0;
  public static final int FNT_MODE_PENCIL    = 1;
  public static final int FNT_MODE_ERASER    = 2;
  public static final int FNT_MODE_INVERTER  = 3;
  public static final int FNT_MODE_RANGE     = 4;
  public static final int FNT_MODE_CHARACTER = 5;

  //メモリにマップされないフォントデータの配列
  public static final byte[] fntMemoryANK16X32 = new byte[2 * 32 * 256];
  public static final byte[] fntMemoryKNJ8X8 = new byte[1 * 8 * 94 * 77];
  public static final byte[] fntMemoryKNJ12X12 = new byte[2 * 12 * 94 * 77];
  public static final byte[] fntMemoryKNJ32X32 = new byte[4 * 32 * 94 * 77];

  //ページ
  public static FontPage fntPageANK8X8;
  public static FontPage fntPageANK12X12;
  public static FontPage fntPageANK6X12;
  public static FontPage fntPageANK8X16;
  public static FontPage fntPageANK12X24;
  public static FontPage fntPageANK16X32;
  public static FontPage fntPageKNJ8X8;
  public static FontPage fntPageKNJ12X12;
  public static FontPage fntPageKNJ16X16;
  public static FontPage fntPageKNJ24X24;
  public static FontPage fntPageKNJ32X32;
  public static FontPage fntSelectedPage;

  public static String fntCGROM;

  //ウインドウ
  public static DrawingCanvas fntCanvas;
  public static JTextField fntStatusTextField;
  public static JFrame fntFrame;
  public static boolean fntUseMouseWheel;
  public static boolean fntReverseMouseWheel;
  public static int fntScaleShift;
  public static JRadioButtonMenuItem[] fntScaleRadioButtonMenuItem;
  public static int fntMode;
  public static int fntEditX;
  public static int fntEditY;

/*
  //範囲
  public static Area fntSelectionArea;
  public static BasicStroke fntSelectionStroke;
  public static Color fntSelectionPaint;
  public static Rectangle fntSelectionRectangle;
  public static int fntSelectionColStart;
  public static int fntSelectionRowStart;
  public static int fntSelectionColEnd;
  public static int fntSelectionRowEnd;
  public static int fntSelectionMode;
*/

  //fntInit ()
  //  初期化
  public static void fntInit () {
    //メモリにマップされないフォントデータの配列
    //fntMemoryANK16X32 = new byte[2 * 32 * 256];
    //fntMemoryKNJ8X8 = new byte[1 * 8 * 94 * 77];
    //fntMemoryKNJ12X12 = new byte[2 * 12 * 94 * 77];
    //fntMemoryKNJ32X32 = new byte[4 * 32 * 94 * 77];
    //ページ
    //  quarter square character  1/4角文字
    //  half width character      半角文字
    //  full width character      全角文字
    fntPageANK8X8   = new FontPage (FNT_MASK_ANK8X8  , MainMemory.mmrM8, FNT_ADDRESS_ANK8X8  ,  8,  8, FNT_TYPE_ANK, FNT_EN_ANK8X8,   FNT_JA_ANK8X8  , FNT_IMAGE_ANK8X8);
    fntPageANK12X12 = new FontPage (FNT_MASK_ANK12X12, MainMemory.mmrM8, FNT_ADDRESS_ANK12X12, 12, 12, FNT_TYPE_ANK, FNT_EN_ANK12X12, FNT_JA_ANK12X12, FNT_IMAGE_ANK12X12);
    fntPageANK6X12  = new FontPage (FNT_MASK_ANK6X12 , MainMemory.mmrM8, FNT_ADDRESS_ANK6X12 ,  6, 12, FNT_TYPE_ANK, FNT_EN_ANK6X12,  FNT_JA_ANK6X12 , FNT_IMAGE_ANK6X12);
    fntPageANK8X16  = new FontPage (FNT_MASK_ANK8X16 , MainMemory.mmrM8, FNT_ADDRESS_ANK8X16 ,  8, 16, FNT_TYPE_ANK, FNT_EN_ANK8X16,  FNT_JA_ANK8X16 , FNT_IMAGE_ANK8X16);
    fntPageANK12X24 = new FontPage (FNT_MASK_ANK12X24, MainMemory.mmrM8, FNT_ADDRESS_ANK12X24, 12, 24, FNT_TYPE_ANK, FNT_EN_ANK12X24, FNT_JA_ANK12X24, FNT_IMAGE_ANK12X24);
    fntPageANK16X32 = new FontPage (FNT_MASK_ANK16X32, fntMemoryANK16X32,     0, 16, 32, FNT_TYPE_ANK, FNT_EN_ANK16X32, FNT_JA_ANK16X32, FNT_IMAGE_ANK16X32);
    fntPageKNJ8X8   = new FontPage (FNT_MASK_KNJ8X8  , fntMemoryKNJ8X8,       0,  8,  8, FNT_TYPE_KNJ, FNT_EN_KNJ8X8,   FNT_JA_KNJ8X8  , FNT_IMAGE_KNJ8X8);
    fntPageKNJ12X12 = new FontPage (FNT_MASK_KNJ12X12, fntMemoryKNJ12X12,     0, 12, 12, FNT_TYPE_KNJ, FNT_EN_KNJ12X12, FNT_JA_KNJ12X12, FNT_IMAGE_KNJ12X12);
    fntPageKNJ16X16 = new FontPage (FNT_MASK_KNJ16X16, MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16, 16, 16, FNT_TYPE_KNJ, FNT_EN_KNJ16X16, FNT_JA_KNJ16X16, FNT_IMAGE_KNJ16X16);
    fntPageKNJ24X24 = new FontPage (FNT_MASK_KNJ24X24, MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24, 24, 24, FNT_TYPE_KNJ, FNT_EN_KNJ24X24, FNT_JA_KNJ24X24, FNT_IMAGE_KNJ24X24);
    fntPageKNJ32X32 = new FontPage (FNT_MASK_KNJ32X32, fntMemoryKNJ32X32,     0, 32, 32, FNT_TYPE_KNJ, FNT_EN_KNJ32X32, FNT_JA_KNJ32X32, FNT_IMAGE_KNJ32X32);
    fntSelectedPage = fntPageKNJ16X16;
    //CGROM
    fntCGROM = FNT_CGROM;
    //ウインドウ
    fntCanvas = null;
  }  //fntInit()

  //fntStart ()
  public static void fntStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_FNT_FRAME_KEY)) {
      fntOpen ();
    }
  }  //fntStart()

  //fntOpen ()
  //  ウインドウを開く
  public static void fntOpen () {
    if (fntFrame == null) {
      fntMakeFrame ();
    }
    fntFrame.setVisible (true);
  }  //fntOpen()

  //fntMakeFrame ()
  //  ウインドウを作る
  //  ここでは開かない
  public static void fntMakeFrame () {

    fntUseMouseWheel = true;
    fntReverseMouseWheel = false;
    fntScaleShift = 0;
    fntMode = FNT_MODE_BROWSE;
    fntEditX = -1;
    fntEditY = -1;

    //ステータステキストフィールド
    fntStatusTextField = setEditable (createTextField ("", 20), false);

/*
    //検索コンボボックス
    //  文字またはSJISコードを入力するテキストフィールド
    //  SJISコード順に空きコードを飛ばしながら回転するスピナー
*/

    //キャンバス
    fntCanvas = new DrawingCanvas (fntSelectedPage.fnpGetImage ());
    fntCanvas.setMatColor (new Color (LnF.LNF_RGB[10]));
    fntCanvas.setCenterPoint (new Point (0, 0));
    addRemovableListener (
      fntCanvas,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          int x = me.getX ();
          int y = me.getY ();
          if (fntMode == FNT_MODE_PENCIL ||
              fntMode == FNT_MODE_ERASER ||
              fntMode == FNT_MODE_INVERTER) {
            fntSelectedPage.fnpEditPixel (x, y, fntMode);
            me.consume ();
          }
        }
      });
    addRemovableListener (
      fntCanvas,
      new MouseMotionAdapter () {
        @Override public void mouseDragged (MouseEvent me) {
          int x = me.getX ();
          int y = me.getY ();
          if (fntMode == FNT_MODE_PENCIL ||
              fntMode == FNT_MODE_ERASER ||
              fntMode == FNT_MODE_INVERTER) {
            fntSelectedPage.fnpEditPixel (x, y, fntMode);
            me.consume ();
          }
        }
        @Override public void mouseMoved (MouseEvent me) {
          int x = me.getX ();
          int y = me.getY ();
          if (!me.isShiftDown ()) {  //Shiftキーが押されていない
            //ステータステキストを表示する
            fntStatusTextField.setText (fntSelectedPage.fnpStatusText (x, y));
            fntStatusTextField.repaint ();
          }
        }
      });
    addRemovableListener (
      fntCanvas,
      new MouseWheelListener () {
        @Override public void mouseWheelMoved (MouseWheelEvent mwe) {
          if (fntUseMouseWheel) {
            MouseWheelEvent2D mwe2D = (MouseWheelEvent2D) mwe;
            int n = mwe2D.getWheelRotation ();
            if (fntReverseMouseWheel) {
              n = -n;
            }
            n = n < 0 ? Math.min (4, fntScaleShift + 1) : Math.max (-4, fntScaleShift - 1);
            if (fntScaleShift != n) {
              fntScaleShift = n;
              fntCanvas.setScaleShift (n, mwe2D.getPoint2D ());
              fntScaleRadioButtonMenuItem[4 + n].setSelected (true);
            }
          }
          mwe.consume ();
        }
      });

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Load font files":
          fntLoadCGROM ();
          break;
        case "Save font files":
          if (prgIsLocal) {  //ローカルのとき
            fntSaveCGROM ();
          }
          break;
        case "Close":
          fntFrame.setVisible (false);
          break;
        case FNT_EN_ANK8X8:
          fntSelectPage (fntPageANK8X8);
          break;
        case FNT_EN_ANK12X12:
          fntSelectPage (fntPageANK12X12);
          break;
        case FNT_EN_ANK6X12:
          fntSelectPage (fntPageANK6X12);
          break;
        case FNT_EN_ANK8X16:
          fntSelectPage (fntPageANK8X16);
          break;
        case FNT_EN_ANK12X24:
          fntSelectPage (fntPageANK12X24);
          break;
        case FNT_EN_ANK16X32:
          fntSelectPage (fntPageANK16X32);
          break;
        case FNT_EN_KNJ8X8:
          fntSelectPage (fntPageKNJ8X8);
          break;
        case FNT_EN_KNJ12X12:
          fntSelectPage (fntPageKNJ12X12);
          break;
        case FNT_EN_KNJ16X16:
          fntSelectPage (fntPageKNJ16X16);
          break;
        case FNT_EN_KNJ24X24:
          fntSelectPage (fntPageKNJ24X24);
          break;
        case FNT_EN_KNJ32X32:
          fntSelectPage (fntPageKNJ32X32);
          break;
          //変換
        case "from " + FNT_EN_ANK8X16 + " to " + FNT_EN_ANK8X8:
          fntConvertANK8X16toANK8X8 ();
          break;
        case "from " + FNT_EN_KNJ8X8 + " to " + FNT_EN_ANK8X8:
          fntConvertKNJ8X8toANK8X8 ();
          break;
        case "from " + FNT_EN_KNJ12X12 + " to " + FNT_EN_ANK12X12:
          fntConvertKNJ12X12toANK12X12 ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_ANK8X16:
          fntConvertKNJ16X16toANK8X16 ();
          break;
        case "from " + FNT_EN_KNJ24X24 + " to " + FNT_EN_ANK12X24:
          fntConvertKNJ24X24toANK12X24 ();
          break;
        case "from " + FNT_EN_ANK8X16 + " to " + FNT_EN_ANK6X12 + " (SX-Window)":
          fntConvertANK8X16toANK6X12FSX ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (SX-Window)":
          fntConvertKNJ16X16toKNJ12X12FSX ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test1)":
          fntConvertKNJ16X16toKNJ12X12Test1 ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test2)":
          fntConvertKNJ16X16toKNJ12X12Test2 ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test3)":
          fntConvertKNJ16X16toKNJ12X12Test3 ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test4)":
          fntConvertKNJ16X16toKNJ12X12Test4 ();
          break;
        case "from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test5)":
          fntConvertKNJ16X16toKNJ12X12Test5 ();
          break;
        case "from " + FNT_EN_KNJ24X24 + " to " + FNT_EN_KNJ12X12 + " (IOCS)":
          fntConvertKNJ24X24toKNJ12X12IOCS ();
          break;
        case "Browse":
          fntMode = FNT_MODE_BROWSE;
          break;
        case "Pencil":
          fntMode = FNT_MODE_PENCIL;
          break;
        case "Eraser":
          fntMode = FNT_MODE_ERASER;
          break;
        case "Inverter":
          fntMode = FNT_MODE_INVERTER;
          break;
/*
        case "Range Selection":
          fntMode = FNT_MODE_RANGE;
          break;
        case "Character Selection":
          fntMode = FNT_MODE_CHARACTER;
          break;
        case "Clear":
          break;
        case "Fill":
          break;
        case "Invert":
          break;
        case "Copy":
          break;
        case "Paste":
          break;
        case "Paste (AND)":
          break;
        case "Paste (OR)":
          break;
        case "Paste (XOR)":
          break;
        case "Undo":
          break;
        case "Redo":
          break;
*/
        case "6.25%":
          fntCanvas.setScaleShift (-4);
          break;
        case "12.5%":
          fntCanvas.setScaleShift (-3);
          break;
        case "25%":
          fntCanvas.setScaleShift (-2);
          break;
        case "50%":
          fntCanvas.setScaleShift (-1);
          break;
        case "100%":
          fntCanvas.setScaleShift (0);
          break;
        case "200%":
          fntCanvas.setScaleShift (1);
          break;
        case "400%":
          fntCanvas.setScaleShift (2);
          break;
        case "800%":
          fntCanvas.setScaleShift (3);
          break;
        case "1600%":
          fntCanvas.setScaleShift (4);
          break;
        case "Use mouse wheel":
          fntUseMouseWheel = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Reverse mouse wheel":
          fntReverseMouseWheel = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };

    //ホストマシンのフォント
    Font[] hostFonts = GraphicsEnvironment.getLocalGraphicsEnvironment ().getAllFonts ();
    String[] hostFontNames = new String[hostFonts.length];
    for (int i = 0; i < hostFonts.length; i++) {
      hostFontNames[i] = hostFonts[i].getName ();
    }
    Arrays.sort (hostFontNames, new Comparator<String> () {
      @Override public int compare (String o1, String o2) {
        return o1.toUpperCase ().compareTo (o2.toUpperCase ());
      }
    });
    ActionListener fontListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        fntSelectedPage.fnpGenerate (ae.getActionCommand ());
        fntCanvas.repaint ();
      }
    };
    JMenu hostFontMenu = Multilingual.mlnText (createMenu ("Fonts on host machine", 'H'), "ja", "ホストマシンのフォント");
    //フォント名の先頭の文字が同じフォントが多数あるのでメニューを先頭1文字と先頭2文字の2段階にする
    //! 2段階でも足りなくなる可能性があるので先頭の文字の偏り方によって深さを可変にしたい
    String h1 = "";  //前回の先頭1文字
    String h2 = "";  //前回の先頭2文字
    JMenu hostFontSubMenu1 = null;
    JMenu hostFontSubMenu2 = null;
    for (int i = 0; i < hostFontNames.length; i++) {
      String name = hostFontNames[i];
      if (name == null || name.length () == 0) {  //念のため
        continue;
      }
      String c1 = name.substring (0, 1).toUpperCase ();  //今回の先頭1文字
      if (!h1.equalsIgnoreCase (c1)) {  //先頭1文字が前回と異なる
        h1 = c1;
        h2 = "";
        hostFontSubMenu1 = createMenu (c1);
        hostFontSubMenu2 = null;
        hostFontMenu.add (hostFontSubMenu1);
      }
      if (name.length () == 1) {  //1文字だけ
        hostFontSubMenu1.add (createMenuItem (name, fontListener));
      } else {  //2文字以上ある
        String c2 = name.substring (0, 2).toUpperCase ();  //今回の先頭2文字
        if (!h2.equalsIgnoreCase (c2)) {  //先頭2文字が前回と異なる
          h2 = c2;
          hostFontSubMenu2 = createMenu (c2);
          hostFontSubMenu1.add (hostFontSubMenu2);
        }
        hostFontSubMenu2.add (createMenuItem (name, fontListener));
      }
    }

    //メニューバー
    ButtonGroup setGroup = new ButtonGroup ();
    ButtonGroup editGroup = new ButtonGroup ();
    ButtonGroup zoomGroup = new ButtonGroup ();
    fntScaleRadioButtonMenuItem = new JRadioButtonMenuItem[9];
    JMenuBar fntMenuBar = createMenuBar (
      Multilingual.mlnText (
        createMenu (
          "File", 'F',
          setEnabled (
            Multilingual.mlnText (createMenuItem ("Load font files" , 'L', listener), "ja", "フォントファイルの読み込み"),
            prgIsLocal),  //ローカルのとき
          setEnabled (
            Multilingual.mlnText (createMenuItem ("Save font files" , 'S', listener), "ja", "フォントファイルの書き出し"),
            prgIsLocal),  //ローカルのとき
/*
          Multilingual.mlnText (createMenuItem ("Read image file" , 'I', listener), "ja", "イメージファイルの読み込み"),
          Multilingual.mlnText (createMenuItem ("Write image file", 'O', listener), "ja", "イメージファイルの書き出し"),
 */
          createHorizontalSeparator (),
          Multilingual.mlnText (createMenuItem ("Close"          , 'C', listener), "ja", "閉じる")
          ),
        "ja", "ファイル"),
      Multilingual.mlnText (
        createMenu (
          "Page", 'P',
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK8X8  , fntPageANK8X8  .fnpEn, '1', listener), "ja", fntPageANK8X8  .fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK12X12, fntPageANK12X12.fnpEn, '2', listener), "ja", fntPageANK12X12.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK6X12 , fntPageANK6X12 .fnpEn, '3', listener), "ja", fntPageANK6X12 .fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK8X16 , fntPageANK8X16 .fnpEn, '4', listener), "ja", fntPageANK8X16 .fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK12X24, fntPageANK12X24.fnpEn, '5', listener), "ja", fntPageANK12X24.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageANK16X32, fntPageANK16X32.fnpEn,      listener), "ja", fntPageANK16X32.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageKNJ8X8  , fntPageKNJ8X8  .fnpEn,      listener), "ja", fntPageKNJ8X8  .fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageKNJ12X12, fntPageKNJ12X12.fnpEn,      listener), "ja", fntPageKNJ12X12.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageKNJ16X16, fntPageKNJ16X16.fnpEn, '6', listener), "ja", fntPageKNJ16X16.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageKNJ24X24, fntPageKNJ24X24.fnpEn, '7', listener), "ja", fntPageKNJ24X24.fnpJa),
          Multilingual.mlnText (createRadioButtonMenuItem (
            setGroup, fntSelectedPage == fntPageKNJ32X32, fntPageKNJ32X32.fnpEn,      listener), "ja", fntPageKNJ32X32.fnpJa)
          ),
        "ja", "ページ"),
      Multilingual.mlnText (
        createMenu (
          "Automatic Creation", 'A',
          hostFontMenu,
          Multilingual.mlnText (
            createMenu (
              "Convert from another page", 'C',
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_ANK8X16 + " to " + FNT_EN_ANK8X8, listener),
                       "ja", FNT_JA_ANK8X16 + " から " + FNT_JA_ANK8X8 + " へ"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ8X8 + " to " + FNT_EN_ANK8X8, listener),
                       "ja", FNT_JA_KNJ8X8 + " から " + FNT_JA_ANK8X8 + " へ"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ12X12 + " to " + FNT_EN_ANK12X12, listener),
                       "ja", FNT_JA_KNJ12X12 + " から " + FNT_JA_ANK12X12 + " へ"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_ANK8X16, listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_ANK8X16 + " へ"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ24X24 + " to " + FNT_EN_ANK12X24, listener),
                       "ja", FNT_JA_KNJ24X24 + " から " + FNT_JA_ANK12X24 + " へ"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_ANK8X16 + " to " + FNT_EN_ANK6X12 + " (SX-Window)", listener),
                       "ja", FNT_JA_ANK8X16 + " から " + FNT_JA_ANK6X12 + " へ (SX-Window)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (SX-Window)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (SX-Window)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test1)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (Test1)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test2)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (Test2)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test3)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (Test3)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test4)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (Test4)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ16X16 + " to " + FNT_EN_KNJ12X12 + " (Test5)", listener),
                       "ja", FNT_JA_KNJ16X16 + " から " + FNT_JA_KNJ12X12 + " へ (Test5)"),
              Multilingual.mlnText (createMenuItem ("from " + FNT_EN_KNJ24X24 + " to " + FNT_EN_KNJ12X12 + " (IOCS)", listener),
                       "ja", FNT_JA_KNJ24X24 + " から " + FNT_JA_KNJ12X12 + " へ (IOCS)")
              ),
            "ja", "他のページのフォントから変換する")
          ),
        "ja", "自動生成"),
      Multilingual.mlnText (
        createMenu (
          "Edit", 'E',
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_BROWSE   , "Browse"             , 'B', listener), "ja", "閲覧"),
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_PENCIL   , "Pencil"             , 'P', listener), "ja", "鉛筆"),
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_ERASER   , "Eraser"             , 'E', listener), "ja", "消しゴム"),
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_INVERTER , "Inverter"           , 'I', listener), "ja", "白黒反転")/*,
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_RANGE    , "Range Selection"    , 'S', listener), "ja", "範囲選択"),
          Multilingual.mlnText (createRadioButtonMenuItem (editGroup, fntMode == FNT_MODE_CHARACTER, "Character Selection", 'C', listener), "ja", "文字選択"),
          createHorizontalSeparator (),
          Multilingual.mlnText (createMenuItem ("Clear"      , 'Z', listener), "ja", "消去"),
          Multilingual.mlnText (createMenuItem ("Fill"       , 'F', listener), "ja", "塗り潰し"),
          Multilingual.mlnText (createMenuItem ("Invert"     , 'I', listener), "ja", "反転"),
          Multilingual.mlnText (createMenuItem ("Copy"       , 'C', listener), "ja", "コピー"),
          Multilingual.mlnText (createMenuItem ("Paste"      , 'V', listener), "ja", "貼り付け"),
          Multilingual.mlnText (createMenuItem ("Paste (AND)", 'A', listener), "ja", "貼り付け (AND)"),
          Multilingual.mlnText (createMenuItem ("Paste (OR)" , 'O', listener), "ja", "貼り付け (OR)"),
          Multilingual.mlnText (createMenuItem ("Paste (XOR)", 'X', listener), "ja", "貼り付け (XOR)"),
          createHorizontalSeparator (),
          Multilingual.mlnText (createMenuItem ("Undo"       , 'U', listener), "ja", "元に戻す"),
          Multilingual.mlnText (createMenuItem ("Redo"       , 'R', listener), "ja", "やり直す")
*/
          ),
        "ja", "編集"),
      Multilingual.mlnText (
        createMenu (
          "Zoom In and Out", 'Z',
          fntScaleRadioButtonMenuItem[0] = createRadioButtonMenuItem (zoomGroup, fntScaleShift == -4, "6.25%", '1', listener),
          fntScaleRadioButtonMenuItem[1] = createRadioButtonMenuItem (zoomGroup, fntScaleShift == -3, "12.5%", '2', listener),
          fntScaleRadioButtonMenuItem[2] = createRadioButtonMenuItem (zoomGroup, fntScaleShift == -2, "25%"  , '3', listener),
          fntScaleRadioButtonMenuItem[3] = createRadioButtonMenuItem (zoomGroup, fntScaleShift == -1, "50%"  , '4', listener),
          fntScaleRadioButtonMenuItem[4] = createRadioButtonMenuItem (zoomGroup, fntScaleShift ==  0, "100%" , '5', listener),
          fntScaleRadioButtonMenuItem[5] = createRadioButtonMenuItem (zoomGroup, fntScaleShift ==  1, "200%" , '6', listener),
          fntScaleRadioButtonMenuItem[6] = createRadioButtonMenuItem (zoomGroup, fntScaleShift ==  2, "400%" , '7', listener),
          fntScaleRadioButtonMenuItem[7] = createRadioButtonMenuItem (zoomGroup, fntScaleShift ==  3, "800%" , '8', listener),
          fntScaleRadioButtonMenuItem[8] = createRadioButtonMenuItem (zoomGroup, fntScaleShift ==  4, "1600%", '9', listener),
          createHorizontalSeparator (),
          Multilingual.mlnText (createCheckBoxMenuItem (fntUseMouseWheel    , "Use mouse wheel"    , listener), "ja", "マウスホイールを使う"),
          Multilingual.mlnText (createCheckBoxMenuItem (fntReverseMouseWheel, "Reverse mouse wheel", listener), "ja", "マウスホイールの向きを逆にする")
          ),
        "ja", "拡大縮小")
      );

    //ウインドウ
    fntFrame = Multilingual.mlnTitle (
      createRestorableSubFrame (
        Settings.SGS_FNT_FRAME_KEY,
        "Font Editor",
        fntMenuBar,
        createBorderPanel (
          setPreferredSize (fntCanvas, 800, 600),
          null,
          null,
          fntStatusTextField
          )
        ),
      "ja", "フォントエディタ");

  }  //fntMakeFrame()

  //fntLoadCGROM ()
  //  フォントファイルを選択して読み込む
  //  コマンドラインのみ
  public static void fntLoadCGROM () {
    JFileChooser2 fileChooser = new JFileChooser2 (new File (fntCGROM));
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("CGROM") ||
                 upperName.endsWith (".F8") || upperName.endsWith (".F8.GZ") ||
                 upperName.endsWith (".F12") || upperName.endsWith (".F12.GZ") ||
                 upperName.endsWith (".FON") || upperName.endsWith (".FON.GZ") ||
                 upperName.endsWith (".F24") || upperName.endsWith (".F24.GZ") ||
                 upperName.endsWith (".F32") || upperName.endsWith (".F32.GZ")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "フォントファイル (CGROM*.*,*.F8,*.F12,*.FON,*.F24,*.F32)" :
                "Font files (CGROM*.*,*.F8,*.F12,*.FON,*.F24,*.F32)");
      }
    });
    fileChooser.setMultiSelectionEnabled (true);  //複数選択可能
    if (fileChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) {
      File[] list = fileChooser.getSelectedFiles2 ();
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < list.length; i++) {
        File file = list[i];
        if (file.isFile ()) {
          if (sb.length () == 0) {
            sb.append (',');
          }
          String name = file.getPath ();
          sb.append (name);
          fntCGROM = name;
        }
      }
      if (sb.length () > 0) {  //ファイルが指定されている
        int mask = fntLoadCGROM (sb.toString (), FNT_MASK_ALL) ^ FNT_MASK_ALL;  //読み込んだページ
        if (mask != 0 && (fntSelectedPage.fnpMask & mask) == 0) {  //読み込んだページがあるが表示されているページは読み込まれていない
          switch (mask & -mask) {  //読み込んだページの中で最も若いもの
          case FNT_MASK_ANK8X8:
            fntSelectPage (fntPageANK8X8);
            break;
          case FNT_MASK_ANK12X12:
            fntSelectPage (fntPageANK12X12);
            break;
          case FNT_MASK_ANK6X12:
            fntSelectPage (fntPageANK6X12);
            break;
          case FNT_MASK_ANK8X16:
            fntSelectPage (fntPageANK8X16);
            break;
          case FNT_MASK_ANK12X24:
            fntSelectPage (fntPageANK12X24);
            break;
          case FNT_MASK_ANK16X32:
            fntSelectPage (fntPageANK16X32);
            break;
          case FNT_MASK_KNJ8X8:
            fntSelectPage (fntPageKNJ8X8);
            break;
          case FNT_MASK_KNJ12X12:
            fntSelectPage (fntPageKNJ12X12);
            break;
          case FNT_MASK_KNJ16X16:
            fntSelectPage (fntPageKNJ16X16);
            break;
          case FNT_MASK_KNJ24X24:
            fntSelectPage (fntPageKNJ24X24);
            break;
          case FNT_MASK_KNJ32X32:
            fntSelectPage (fntPageKNJ32X32);
            break;
          }
        }
      }
    }
  }  //fntLoadCGROM

  //required = fntLoadCGROM (names, required)
  //  フォントファイルを読み込む
  //  IOCS.X,hiocs.xのバッファは更新されない
  //! 060turbo.sysがROMをローカルメモリにコピーするので060モードのときは再起動しないと表示に反映されない
  public static int fntLoadCGROM (String names, int required) {
    byte[] bb = new byte[Math.max (786432, 2 * 32 * 256 + 4 * 32 * 94 * 94)];
    for (String name : names.split (",")) {
      name = name.trim ();
      if (name.length () == 0) {
        continue;
      }
      String upperName = name.toUpperCase ();
      int guess = (upperName.startsWith ("CGROM") ? FNT_MASK_CGROM :
                   upperName.endsWith (".F8") || upperName.endsWith (".F8.GZ") ? FNT_MASK_KNJ8X8 :
                   upperName.endsWith (".F12") || upperName.endsWith (".F12.GZ") ? FNT_MASK_ANK6X12 | FNT_MASK_KNJ12X12 :
                   upperName.endsWith (".FON") || upperName.endsWith (".FON.GZ") ? FNT_MASK_ANK8X16 | FNT_MASK_KNJ16X16 :
                   upperName.endsWith (".F24") || upperName.endsWith (".F24.GZ") ? FNT_MASK_ANK12X24 | FNT_MASK_KNJ24X24 :
                   upperName.endsWith (".F32") || upperName.endsWith (".F32.GZ") ? FNT_MASK_ANK16X32 | FNT_MASK_KNJ32X32 :
                   FNT_MASK_CGROM);  //このファイルに含まれている可能性のあるフォントの種類
      if ((required & guess) == 0) {  //このファイルに含まれている可能性のある種類のフォントは不要
        continue;
      }
      //読み込む
      InputStream in = ismOpen (name);
      if (in == null) {  //読み込めない
        continue;
      }
      int length = ismRead (in, bb, 0,
                            guess == FNT_MASK_KNJ8X8 ? 1 * 8 * 256 + 1 * 8 * 94 * 94 :  //72736
                            guess == (FNT_MASK_ANK6X12 | FNT_MASK_KNJ12X12) ? 1 * 12 * 256 + 2 * 12 * 94 * 94 :  //215136
                            guess == (FNT_MASK_ANK8X16 | FNT_MASK_KNJ16X16) ? 1 * 16 * 256 + 2 * 16 * 94 * 94 :  //286848
                            guess == (FNT_MASK_ANK12X24 | FNT_MASK_KNJ24X24) ? 2 * 24 * 256 + 3 * 24 * 94 * 94 :  //648480
                            guess == (FNT_MASK_ANK16X32 | FNT_MASK_KNJ32X32) ? 2 * 32 * 256 + 4 * 32 * 94 * 94 :  //1147392
                            786432);  //実際に読み込んだ長さ
      ismClose (in);
      //読み込んだ長さで改めてフォントの種類を判断する
      switch (length) {
      case 786432:  //CGROM.DAT
        if ((required & FNT_MASK_CGROM) != 0) {
          System.arraycopy (bb, 0, MainMemory.mmrM8, 0x00f00000, 786432);
          if (MainMemory.mmrM8[FNT_ADDRESS_ANK6X12 + 12 * 'A' + 6] != 0) {  //ANK6X12が含まれている
            //ROM1.0～ROM1.2のときはCGROMに読み込んだANK6X12をIPLROMにコピーする
            if (romANK6X12 != 0) {
              System.arraycopy (MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, MainMemory.mmrM8, romANK6X12, 1 * 12 * 254);
            }
            required &= ~FNT_MASK_CGROM;
          } else {  //ANK6X12が含まれていない
            required &= ~(FNT_MASK_CGROM & ~FNT_MASK_ANK6X12);
          }
          fntPageKNJ16X16.fnpMemoryToImage ();
          fntPageANK8X8.fnpMemoryToImage ();
          fntPageANK8X16.fnpMemoryToImage ();
          fntPageANK12X12.fnpMemoryToImage ();
          fntPageANK12X24.fnpMemoryToImage ();
          fntPageKNJ24X24.fnpMemoryToImage ();
          fntPageANK6X12.fnpMemoryToImage ();
        }
        break;
      case 1 * 8 * 256 + 1 * 8 * 94 * 94:  //KNJ8X8
        if ((required & FNT_MASK_KNJ8X8) != 0) {
          System.arraycopy (bb, 1 * 8 * 256, fntMemoryKNJ8X8, 0, 1 * 8 * 94 * 8);  //1区～8区
          System.arraycopy (bb, 1 * 8 * 256 + 1 * 8 * 94 * 15, fntMemoryKNJ8X8, 1 * 8 * 94 * 8, 1 * 8 * 94 * 69);  //16区～84区
          required &= ~FNT_MASK_KNJ8X8;
          fntPageKNJ8X8.fnpMemoryToImage ();
        }
        break;
      case 1 * 12 * 256 + 2 * 12 * 94 * 94:  //KNJ12X12
        if ((required & FNT_MASK_KNJ12X12) != 0) {
          System.arraycopy (bb, 1 * 12 * 256, fntMemoryKNJ12X12, 0, 2 * 12 * 94 * 8);  //1区～8区
          System.arraycopy (bb, 1 * 12 * 256 + 2 * 12 * 94 * 15, fntMemoryKNJ12X12, 2 * 12 * 94 * 8, 2 * 12 * 94 * 69);  //16区～84区
          required &= ~FNT_MASK_KNJ12X12;
          fntPageKNJ12X12.fnpMemoryToImage ();
        }
        //ANK6X12とKNJ12X12は同じファイルから読み込む
      case 1 * 12 * 256:  //ANK6X12
        if ((required & FNT_MASK_ANK6X12) != 0) {
          System.arraycopy (bb, 0, MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, 1 * 12 * 256);
          required &= ~FNT_MASK_ANK6X12;
          fntPageANK6X12.fnpMemoryToImage ();
        }
        break;
      case 1 * 16 * 256 + 2 * 16 * 94 * 94:  //KNJ16X16
        if ((required & FNT_MASK_KNJ16X16) != 0) {
          System.arraycopy (bb, 1 * 16 * 256, MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16, 2 * 16 * 94 * 8);  //1区～8区
          System.arraycopy (bb, 1 * 16 * 256 + 2 * 16 * 94 * 15, MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16 + 2 * 16 * 94 * 8, 2 * 16 * 94 * 69);  //16区～84区
          required &= ~FNT_MASK_KNJ16X16;
          fntPageKNJ16X16.fnpMemoryToImage ();
        }
        //ANK8X16とKNJ16X16は同じファイルから読み込む
      case 1 * 16 * 256:  //ANK8X16
        if ((required & FNT_MASK_ANK8X16) != 0) {
          System.arraycopy (bb, 0, MainMemory.mmrM8, FNT_ADDRESS_ANK8X16, 1 * 16 * 256);
          required &= ~FNT_MASK_ANK8X16;
          fntPageANK8X16.fnpMemoryToImage ();
        }
        break;
      case 2 * 24 * 256 + 3 * 24 * 94 * 94:  //KNJ24X24
        if ((required & FNT_MASK_KNJ24X24) != 0) {
          System.arraycopy (bb, 2 * 24 * 256, MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24, 3 * 24 * 94 * 8);  //1区～8区
          System.arraycopy (bb, 2 * 24 * 256 + 3 * 24 * 94 * 15, MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24 + 3 * 24 * 94 * 8, 3 * 24 * 94 * 69);  //16区～84区
          required &= ~FNT_MASK_KNJ24X24;
          fntPageKNJ24X24.fnpMemoryToImage ();
        }
        //ANK12X24とKNJ24X24は同じファイルから読み込む
      case 2 * 24 * 256:  //ANK12X24
        if ((required & FNT_MASK_ANK12X24) != 0) {
          System.arraycopy (bb, 0, MainMemory.mmrM8, FNT_ADDRESS_ANK12X24, 2 * 24 * 256);
          required &= ~FNT_MASK_ANK12X24;
          fntPageANK12X24.fnpMemoryToImage ();
        }
        break;
      case 2 * 32 * 256 + 4 * 32 * 94 * 94:  //KNJ32X32
        if ((required & FNT_MASK_KNJ32X32) != 0) {
          System.arraycopy (bb, 2 * 32 * 256, fntMemoryKNJ32X32, 0, 4 * 32 * 94 * 8);  //1区～8区
          System.arraycopy (bb, 2 * 32 * 256 + 4 * 32 * 94 * 15, fntMemoryKNJ32X32, 4 * 32 * 94 * 8, 4 * 32 * 94 * 69);  //16区～84区
          required &= ~FNT_MASK_KNJ32X32;
          fntPageKNJ32X32.fnpMemoryToImage ();
        }
        //ANK16X32とKNJ32X32は同じファイルから読み込む
      case 2 * 32 * 256:  //ANK16X32
        if ((required & FNT_MASK_ANK16X32) != 0) {
          System.arraycopy (bb, 0, fntMemoryANK16X32, 0, 2 * 32 * 256);
          required &= ~FNT_MASK_ANK16X32;
          fntPageANK16X32.fnpMemoryToImage ();
        }
        break;
      default:  //種類が不明
        ;
      }  //switch
      if (required == 0) {
        break;
      }
    }  //for name
    return required;
  }  //fntLoadCGROM(String,int)

  //fntSaveCGROM ()
  //  フォントファイルを名前を指定して書き出す
  //  コマンドラインのみ
  public static void fntSaveCGROM () {
    JFileChooser2 fileChooser = new JFileChooser2 (new File (fntCGROM));
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("CGROM") ||
                 upperName.endsWith (".F8") ||
                 upperName.endsWith (".F12") ||
                 upperName.endsWith (".FON") ||
                 upperName.endsWith (".F24") ||
                 upperName.endsWith (".F32")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "フォントファイル (CGROM*.*,*.F8,*.F12,*.FON,*.F24,*.F32)" :
                "Font files (CGROM*.*,*.F8,*.F12,*.FON,*.F24,*.F32)");
      }
    });
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile2 ();
      String name = file.getPath ();
      //出力するページを決める
      //  *.F12のとき現在表示されているページがANK6X12ならばKNJ12X12を出力しない
      //  *.FONのとき現在表示されているページがANK8X16ならばKNJ16X16を出力しない
      //  *.F24のとき現在表示されているページがANK12X24ならばKNJ24X24を出力しない
      //  *.F32のとき現在表示されているページがANK16X32ならばKNJ32X32を出力しない
      String upperName = name.toUpperCase ();
      int mask = (upperName.startsWith ("CGROM") ? FNT_MASK_CGROM :
                  upperName.endsWith (".F8") ? FNT_MASK_KNJ8X8 :
                  upperName.endsWith (".F12") ? fntSelectedPage == fntPageANK6X12 ? FNT_MASK_ANK6X12 : FNT_MASK_ANK6X12 | FNT_MASK_KNJ12X12 :
                  upperName.endsWith (".FON") ? fntSelectedPage == fntPageANK8X16 ? FNT_MASK_ANK8X16 : FNT_MASK_ANK8X16 | FNT_MASK_KNJ16X16 :
                  upperName.endsWith (".F24") ? fntSelectedPage == fntPageANK12X24 ? FNT_MASK_ANK12X24 : FNT_MASK_ANK12X24 | FNT_MASK_KNJ24X24 :
                  upperName.endsWith (".F32") ? fntSelectedPage == fntPageANK16X32 ? FNT_MASK_ANK16X32 : FNT_MASK_ANK16X32 | FNT_MASK_KNJ32X32 :
                  FNT_MASK_CGROM);  //出力するページ
      File fileTmp = new File (name + ".tmp");
      File fileBak = new File (name + ".bak");
      try (OutputStream out = new BufferedOutputStream (new FileOutputStream (fileTmp))) {  //try-with-resourcesは1.7から
        if (mask == FNT_MASK_CGROM) {
          out.write (MainMemory.mmrM8, 0x00f00000, 0x00fc0000 - 0x00f00000);
        } else if (mask == FNT_MASK_KNJ8X8) {
          byte[] w = new byte[Math.max (1 * 8 * 256, 1 * 8 * 94 * 10)];
          out.write (w, 0, 1 * 8 * 256);  //ANK4X8
          out.write (fntMemoryKNJ8X8, 0, 1 * 8 * 94 * 8);  //KNJ8X8 1区～8区
          out.write (w, 0, 1 * 8 * 94 * 7);  //KNJ8X8 9区～15区
          out.write (fntMemoryKNJ8X8, 1 * 8 * 94 * 8, 1 * 8 * 94 * 69);  //KNJ8X8 16区～84区
          out.write (w, 0, 1 * 8 * 94 * 10);  //KNJ8X8 85区～94区
        } else if ((mask & FNT_MASK_ANK6X12) != 0) {
          out.write (MainMemory.mmrM8, FNT_ADDRESS_ANK6X12, 1 * 12 * 256);  //ANK6X12
          if ((mask & FNT_MASK_KNJ12X12) != 0) {
            byte[] w = new byte[2 * 12 * 94 * 10];
            out.write (fntMemoryKNJ12X12, 0, 2 * 12 * 94 * 8);  //KNJ12X12 1区～8区
            out.write (w, 0, 2 * 12 * 94 * 7);  //KNJ12X12 9区～15区
            out.write (fntMemoryKNJ12X12, 2 * 12 * 94 * 8, 2 * 12 * 94 * 69);  //KNJ12X12 16区～84区
            out.write (w, 0, 2 * 12 * 94 * 10);  //KNJ12X12 85区～94区
          }
        } else if ((mask & FNT_MASK_ANK8X16) != 0) {
          out.write (MainMemory.mmrM8, FNT_ADDRESS_ANK8X16, 1 * 16 * 256);  //ANK8X16
          if ((mask & FNT_MASK_KNJ16X16) != 0) {
            byte[] w = new byte[2 * 16 * 94 * 10];
            out.write (MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16, 2 * 16 * 94 * 8);  //KNJ16X16 1区～8区
            //!!! 外字を出力したい
            out.write (w, 0, 2 * 16 * 94 * 7);  //KNJ16X16 9区～15区
            out.write (MainMemory.mmrM8, FNT_ADDRESS_KNJ16X16 + 2 * 16 * 94 * 8, 2 * 16 * 94 * 69);  //KNJ16X16 16区～84区
            out.write (w, 0, 2 * 16 * 94 * 10);  //KNJ16X16 85区～94区
          }
        } else if ((mask & FNT_MASK_ANK12X24) != 0) {
          out.write (MainMemory.mmrM8, FNT_ADDRESS_ANK12X24, 2 * 24 * 256);  //ANK12X24
          if ((mask & FNT_MASK_KNJ24X24) != 0) {
            byte[] w = new byte[3 * 24 * 94 * 10];
            out.write (MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24, 3 * 24 * 94 * 8);  //KNJ24X24 1区～8区
            //!!! 外字を出力したい
            out.write (w, 0, 3 * 24 * 94 * 7);  //KNJ24X24 9区～15区
            out.write (MainMemory.mmrM8, FNT_ADDRESS_KNJ24X24 + 3 * 24 * 94 * 8, 3 * 24 * 94 * 69);  //KNJ24X24 16区～84区
            out.write (w, 0, 3 * 24 * 94 * 10);  //KNJ24X24 85区～94区
          }
        } else if ((mask & FNT_MASK_ANK16X32) != 0) {
          out.write (fntMemoryANK16X32, 0, 2 * 32 * 256);  //ANK16X32
          if ((mask & FNT_MASK_KNJ32X32) != 0) {
            byte[] w = new byte[4 * 32 * 94 * 10];
            out.write (fntMemoryKNJ32X32, 0, 4 * 32 * 94 * 8);  //KNJ32X32 1区～8区
            out.write (w, 0, 4 * 32 * 94 * 7);  //KNJ32X32 9区～15区
            out.write (fntMemoryKNJ32X32, 4 * 32 * 94 * 8, 4 * 32 * 94 * 69);  //KNJ32X32 16区～84区
            out.write (w, 0, 4 * 32 * 94 * 10);  //KNJ32X32 85区～94区
          }
        }
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? "失敗しました" : "Failed");
        return;
      }
      if (file.exists ()) {
        if (fileBak.exists ()) {
          fileBak.delete ();  //javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除する必要がある
        }
        file.renameTo (fileBak);
      }
      fileTmp.renameTo (file);
      fntCGROM = name;
    }
  }  //fntSaveCGROM()

  //fntConvertANK8X16toANK8X8 ()
  //  ANK8X16を縮小してANK8X8を作る
  //  □■□■□■□■    □■□■□■□■
  //  □■□■□■□■    ■□■□■□■□
  //  ■□■□■□■□    □■□■□■□■
  //  ■□■□■□■□ → ■□■□■□■□
  //  □■□■□■□■    □■□■□■□■
  //  □■□■□■□■    ■□■□■□■□
  //  ■□■□■□■□    □■□■□■□■
  //  ■□■□■□■□    ■□■□■□■□
  //  □■□■□■□■
  //  □■□■□■□■
  //  ■□■□■□■□
  //  ■□■□■□■□
  //  □■□■□■□■
  //  □■□■□■□■
  //  ■□■□■□■□
  //  ■□■□■□■□
  public static void fntConvertANK8X16toANK8X8 () {
    FontPage src = fntPageANK8X16;
    FontPage dst = fntPageANK8X8;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    for (int c = 0; c < 8 * 256; c++) {
      mdst[adst] = (byte) (msrc[asrc] | msrc[asrc + 1]);
      asrc += 1 * (16 / 8);
      adst += 1 * (8 / 8);
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertANK8X16toANK8X8()

  //fntConvertKNJ8X8toANK8X8 ()
  //  KNJ8X8からANK8X8へ変換する
  public static void fntConvertKNJ8X8toANK8X8 () {
    FontPage src = fntPageKNJ8X8;
    FontPage dst = fntPageANK8X8;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    for (int c = 0; c < 256; c++) {
      if (c == 0x82) {  //BROKEN BARを作る
        mdst[adst    ] = 0b00010000;
        mdst[adst + 1] = 0b00010000;
        mdst[adst + 2] = 0b00010000;
        mdst[adst + 3] = 0b00000000;
        mdst[adst + 4] = 0b00010000;
        mdst[adst + 5] = 0b00010000;
        mdst[adst + 6] = 0b00010000;
        mdst[adst + 7] = 0b00000000;
      } else {
        int s = CharacterCode.chrCharToSJIS[ROM_ANK_FULLWIDTH_BASE[c >> 4].charAt (c & 15)];
        int sh = s >> 8;
        int sl = s & 255;
        int t = asrc + ((sh - (sh < 0xe0 ? 0x81 : 0xe0 - (0xa0 - 0x81))) * 188 +
                        (sl - (sl < 0x80 ? 0x40 : 0x80 - (0x7f - 0x40))) << 3);
        mdst[adst    ] = msrc[t    ];
        mdst[adst + 1] = msrc[t + 1];
        mdst[adst + 2] = msrc[t + 2];
        mdst[adst + 3] = msrc[t + 3];
        mdst[adst + 4] = msrc[t + 4];
        mdst[adst + 5] = msrc[t + 5];
        mdst[adst + 6] = msrc[t + 6];
        mdst[adst + 7] = msrc[t + 7];
      }
      adst += 1 * 8;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ8X8toANK8X8()

  //fntConvertKNJ12X12toANK12X12 ()
  //  KNJ12X12からANK12X12へ変換する
  public static void fntConvertKNJ12X12toANK12X12 () {
    FontPage src = fntPageKNJ12X12;
    FontPage dst = fntPageANK12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    for (int c = 0; c < 256; c++) {
      if (c == 0x82) {  //BROKEN BARを作る
        mdst[adst     ] = 0b00000100;
        mdst[adst +  1] = 0b00000000;
        mdst[adst +  2] = 0b00000100;
        mdst[adst +  3] = 0b00000000;
        mdst[adst +  4] = 0b00000100;
        mdst[adst +  5] = 0b00000000;
        mdst[adst +  6] = 0b00000100;
        mdst[adst +  7] = 0b00000000;
        mdst[adst +  8] = 0b00000100;
        mdst[adst +  9] = 0b00000000;
        mdst[adst + 10] = 0b00000000;
        mdst[adst + 11] = 0b00000000;
        mdst[adst + 12] = 0b00000100;
        mdst[adst + 13] = 0b00000000;
        mdst[adst + 14] = 0b00000100;
        mdst[adst + 15] = 0b00000000;
        mdst[adst + 16] = 0b00000100;
        mdst[adst + 17] = 0b00000000;
        mdst[adst + 18] = 0b00000100;
        mdst[adst + 19] = 0b00000000;
        mdst[adst + 20] = 0b00000100;
        mdst[adst + 21] = 0b00000000;
        mdst[adst + 22] = 0b00000000;
        mdst[adst + 23] = 0b00000000;
      } else {
        int s = CharacterCode.chrCharToSJIS[ROM_ANK_FULLWIDTH_BASE[c >> 4].charAt (c & 15)];
        int sh = s >> 8;
        int sl = s & 255;
        int t = asrc + 2 * 12 * ((sh - (sh < 0xe0 ? 0x81 : 0xe0 - (0xa0 - 0x81))) * 188 +
                                 (sl - (sl < 0x80 ? 0x40 : 0x80 - (0x7f - 0x40))));
        mdst[adst     ] = msrc[t     ];
        mdst[adst +  1] = msrc[t +  1];
        mdst[adst +  2] = msrc[t +  2];
        mdst[adst +  3] = msrc[t +  3];
        mdst[adst +  4] = msrc[t +  4];
        mdst[adst +  5] = msrc[t +  5];
        mdst[adst +  6] = msrc[t +  6];
        mdst[adst +  7] = msrc[t +  7];
        mdst[adst +  8] = msrc[t +  8];
        mdst[adst +  9] = msrc[t +  9];
        mdst[adst + 10] = msrc[t + 10];
        mdst[adst + 11] = msrc[t + 11];
        mdst[adst + 12] = msrc[t + 12];
        mdst[adst + 13] = msrc[t + 13];
        mdst[adst + 14] = msrc[t + 14];
        mdst[adst + 15] = msrc[t + 15];
        mdst[adst + 16] = msrc[t + 16];
        mdst[adst + 17] = msrc[t + 17];
        mdst[adst + 18] = msrc[t + 18];
        mdst[adst + 19] = msrc[t + 19];
        mdst[adst + 20] = msrc[t + 20];
        mdst[adst + 21] = msrc[t + 21];
        mdst[adst + 22] = msrc[t + 22];
        mdst[adst + 23] = msrc[t + 23];
      }
      adst += 2 * 12;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ12X12toANK12X12()

  //fntConvertKNJ16X16toANK8X16 ()
  //  KNJ16X16を縮小してANK8X16を作る
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□ → ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  //  □□■■□□■■ □□■■□□■■    □■□■□■□■
  //  ■■□□■■□□ ■■□□■■□□    ■□■□■□■□
  public static void fntConvertKNJ16X16toANK8X16 () {
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageANK8X16;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w = new int[256];
    for (int i = 0; i < 256; i++) {
      w[i] = (i & 0b10000000) | (i & 0b01100000) << 1 | (i & 0b00011000) << 2 | (i & 0b00000110) << 3 | (i & 0b00000001) << 4;
    }
    for (int c = 0; c < 256; c++) {
      if (c == 0x82) {  //BROKEN BARを作る
        mdst[adst     ] = 0b00010000;
        mdst[adst +  1] = 0b00010000;
        mdst[adst +  2] = 0b00010000;
        mdst[adst +  3] = 0b00010000;
        mdst[adst +  4] = 0b00010000;
        mdst[adst +  5] = 0b00010000;
        mdst[adst +  6] = 0b00010000;
        mdst[adst +  7] = 0b00000000;
        mdst[adst +  8] = 0b00010000;
        mdst[adst +  9] = 0b00010000;
        mdst[adst + 10] = 0b00010000;
        mdst[adst + 11] = 0b00010000;
        mdst[adst + 12] = 0b00010000;
        mdst[adst + 13] = 0b00010000;
        mdst[adst + 14] = 0b00010000;
        mdst[adst + 15] = 0b00000000;
      } else {
        int s = CharacterCode.chrCharToSJIS[FNT_ANK_FULLWIDTH_BASE[c]];
        int sh = s >> 8;
        int sl = s & 255;
        int t = asrc + ((sh - (sh < 0xe0 ? 0x81 : 0xe0 - (0xa0 - 0x81))) * 188 +
                        (sl - (sl < 0x80 ? 0x40 : 0x80 - (0x7f - 0x40))) << 5);
        mdst[adst     ] = (byte) (w[msrc[t     ] & 255] | w[msrc[t +  1] & 255] >> 4);
        mdst[adst +  1] = (byte) (w[msrc[t +  2] & 255] | w[msrc[t +  3] & 255] >> 4);
        mdst[adst +  2] = (byte) (w[msrc[t +  4] & 255] | w[msrc[t +  5] & 255] >> 4);
        mdst[adst +  3] = (byte) (w[msrc[t +  6] & 255] | w[msrc[t +  7] & 255] >> 4);
        mdst[adst +  4] = (byte) (w[msrc[t +  8] & 255] | w[msrc[t +  9] & 255] >> 4);
        mdst[adst +  5] = (byte) (w[msrc[t + 10] & 255] | w[msrc[t + 11] & 255] >> 4);
        mdst[adst +  6] = (byte) (w[msrc[t + 12] & 255] | w[msrc[t + 13] & 255] >> 4);
        mdst[adst +  7] = (byte) (w[msrc[t + 14] & 255] | w[msrc[t + 15] & 255] >> 4);
        mdst[adst +  8] = (byte) (w[msrc[t + 16] & 255] | w[msrc[t + 17] & 255] >> 4);
        mdst[adst +  9] = (byte) (w[msrc[t + 18] & 255] | w[msrc[t + 19] & 255] >> 4);
        mdst[adst + 10] = (byte) (w[msrc[t + 20] & 255] | w[msrc[t + 21] & 255] >> 4);
        mdst[adst + 11] = (byte) (w[msrc[t + 22] & 255] | w[msrc[t + 23] & 255] >> 4);
        mdst[adst + 12] = (byte) (w[msrc[t + 24] & 255] | w[msrc[t + 25] & 255] >> 4);
        mdst[adst + 13] = (byte) (w[msrc[t + 26] & 255] | w[msrc[t + 27] & 255] >> 4);
        mdst[adst + 14] = (byte) (w[msrc[t + 28] & 255] | w[msrc[t + 29] & 255] >> 4);
        mdst[adst + 15] = (byte) (w[msrc[t + 30] & 255] | w[msrc[t + 31] & 255] >> 4);
      }
      adst += 16;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toANK8X16()

  //fntConvertKNJ24X24toANK12X24 ()
  //  KNJ24X24を縮小してANK12X24を作る
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□ → ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  public static void fntConvertKNJ24X24toANK12X24 () {
    FontPage src = fntPageKNJ24X24;
    FontPage dst = fntPageANK12X24;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w = new int[256];
    for (int i = 0; i < 256; i++) {
      w[i] = (i & 0b10000000) | (i & 0b01100000) << 1 | (i & 0b00011000) << 2 | (i & 0b00000110) << 3 | (i & 0b00000001) << 4;
    }
    for (int c = 0; c < 256; c++) {
      if (c == 0x82) {  //BROKEN BARを作る
        for (int i = 0; i < 24; i++) {
          mdst[adst    ] = (byte) (i == 11 || i == 12 ? 0b00000000 : 0b00000100);
          mdst[adst + 1] = 0b00000000;
          adst += 2;
        }
      } else {
        int s = CharacterCode.chrCharToSJIS[FNT_ANK_FULLWIDTH_BASE[c]];
        int sh = s >> 8;
        int sl = s & 255;
        int t = asrc + 3 * 24 * ((sh - (sh < 0xe0 ? 0x81 : 0xe0 - (0xa0 - 0x81))) * 188 +
                                 (sl - (sl < 0x80 ? 0x40 : 0x80 - (0x7f - 0x40))));
        for (int i = 0; i < 24; i++) {
          mdst[adst    ] = (byte) (w[msrc[t    ] & 255] | w[msrc[t + 1] & 255] >> 4);
          mdst[adst + 1] = (byte)  w[msrc[t + 2] & 255];
          t += 3;
          adst += 2;
        }
      }
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ24X24toANK12X24()

  //fntConvertANK8X16toANK6X12FSX ()
  //  SX-Window v3の方法でANK8X16をANK6X12に縮小する
  //  FSX310.X 0x02b03e
  //  □□■□■■□■    □■□■□■　　
  //  □□■□■■□■    ■□■□■□　　
  //  ■■□■□□■□    □■□■□■　　
  //  □□■□■■□■    ■□■□■□　　
  //  ■■□■□□■□    □■□■□■　　
  //  ■■□■□□■□ → ■□■□■□　　
  //  □□■□■■□■    □■□■□■　　
  //  ■■□■□□■□    ■□■□■□　　
  //  □□■□■■□■    □■□■□■　　
  //  □□■□■■□■    ■□■□■□　　
  //  ■■□■□□■□    □■□■□■　　
  //  □□■□■■□■    ■□■□■□　　
  //  ■■□■□□■□
  //  ■■□■□□■□
  //  □□■□■■□■
  //  ■■□■□□■□
  public static void fntConvertANK8X16toANK6X12FSX () {
    FontPage src = fntPageANK8X16;
    FontPage dst = fntPageANK6X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w = new int[256];
    for (int i = 0; i < 256; i++) {
      w[i] = (i & 0b10000000) | (i & 0b01111000) << 1 | (i & 0b00000111) << 2;
    }
    for (int c = 0; c < 4 * 256; c++) {
      mdst[adst    ] = (byte) w[(msrc[asrc    ] | msrc[asrc + 1]) & 255];
      mdst[adst + 1] = (byte) w[ msrc[asrc + 2]                   & 255];
      mdst[adst + 2] = (byte) w[ msrc[asrc + 3]                   & 255];
      asrc += 1 * (16 / 4);
      adst += 1 * (12 / 4);
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertANK8X16toANK6X12FSX()

  //fntConvertKNJ16X16toKNJ12X12FSX ()
  //  SX-Window v3の方法でKNJ16X16をKNJ12X12に縮小する
  //  FSX310.X 0x02b11e
  //  □□■□■■□■ □□■□■■□■    □■□■□■□■ □■□■
  //  □□■□■■□■ □□■□■■□■    ■□■□■□■□ ■□■□
  //  ■■□■□□■□ ■■□■□□■□    □■□■□■□■ □■□■
  //  □□■□■■□■ □□■□■■□■    ■□■□■□■□ ■□■□
  //  ■■□■□□■□ ■■□■□□■□    □■□■□■□■ □■□■
  //  ■■□■□□■□ ■■□■□□■□ → ■□■□■□■□ ■□■□
  //  □□■□■■□■ □□■□■■□■    □■□■□■□■ □■□■
  //  ■■□■□□■□ ■■□■□□■□    ■□■□■□■□ ■□■□
  //  □□■□■■□■ □□■□■■□■    □■□■□■□■ □■□■
  //  □□■□■■□■ □□■□■■□■    ■□■□■□■□ ■□■□
  //  ■■□■□□■□ ■■□■□□■□    □■□■□■□■ □■□■
  //  □□■□■■□■ □□■□■■□■    ■□■□■□■□ ■□■□
  //  ■■□■□□■□ ■■□■□□■□
  //  ■■□■□□■□ ■■□■□□■□
  //  □□■□■■□■ □□■□■■□■
  //  ■■□■□□■□ ■■□■□□■□
  public static void fntConvertKNJ16X16toKNJ12X12FSX () {
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w = new int[256];
    for (int i = 0; i < 256; i++) {
      w[i] = (i & 0b10000000) | (i & 0b01111000) << 1 | (i & 0b00000111) << 2;
    }
    for (int i = 0; i < 4 * 94 * 77; i++) {
      int t =                  w[(msrc[asrc + 1] | msrc[asrc + 3]) & 255];
      mdst[adst    ] = (byte) (w[(msrc[asrc    ] | msrc[asrc + 2]) & 255] | t >> 6);
      mdst[adst + 1] = (byte) (t << 2);
      t =                      w[ msrc[asrc + 5]                   & 255];
      mdst[adst + 2] = (byte) (w[ msrc[asrc + 4]                   & 255] | t >> 6);
      mdst[adst + 3] = (byte) (t << 2);
      t =                      w[ msrc[asrc + 7]                   & 255];
      mdst[adst + 4] = (byte) (w[ msrc[asrc + 6]                   & 255] | t >> 6);
      mdst[adst + 5] = (byte) (t << 2);
      asrc += 2 * (16 / 4);
      adst += 2 * (12 / 4);
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12FSX()

  //fntConvertKNJ16X16toKNJ12X12Test1 ()
  //  KNJ16X16をKNJ12X12に縮小する
  //  □□■■□■□■ □■□■□□■■    □■□■□■□■ □■□■
  //  □□■■□■□■ □■□■□□■■    ■□■□■□■□ ■□■□
  //  ■■□□■□■□ ■□■□■■□□    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■□□■■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□■■□□ → ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■□□■■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■□□■■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■□□■■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■□□■■
  //  □□■■□■□■ □■□■□□■■
  //  ■■□□■□■□ ■□■□■■□□
  //  ■■□□■□■□ ■□■□■■□□
  public static void fntConvertKNJ16X16toKNJ12X12Test1 () {
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w0 = new int[256];
    int[] w1 = new int[256];
    for (int i = 0; i < 256; i++) {
      w0[i] = (i & 0b10000000) | (i & 0b01100000) << 1 | (i & 0b00011111) << 2;
      w1[i] = (i & 0b11111000) | (i & 0b00000110) << 1 | (i & 0b00000001) << 2;
    }
    for (int i = 0; i < 94 * 77; i++) {
      int t =                   w1[(msrc[asrc +  1] | msrc[asrc +  3]) & 255];
      mdst[adst     ] = (byte) (w0[(msrc[asrc     ] | msrc[asrc +  2]) & 255] | t >> 6);
      mdst[adst +  1] = (byte) (t << 2);
      t =                       w1[(msrc[asrc +  5] | msrc[asrc +  7]) & 255];
      mdst[adst +  2] = (byte) (w0[(msrc[asrc +  4] | msrc[asrc +  6]) & 255] | t >> 6);
      mdst[adst +  3] = (byte) (t << 2);
      t =                       w1[ msrc[asrc +  9]                    & 255];
      mdst[adst +  4] = (byte) (w0[ msrc[asrc +  8]                    & 255] | t >> 6);
      mdst[adst +  5] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 11]                    & 255];
      mdst[adst +  6] = (byte) (w0[ msrc[asrc + 10]                    & 255] | t >> 6);
      mdst[adst +  7] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 13]                    & 255];
      mdst[adst +  8] = (byte) (w0[ msrc[asrc + 12]                    & 255] | t >> 6);
      mdst[adst +  9] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 15]                    & 255];
      mdst[adst + 10] = (byte) (w0[ msrc[asrc + 14]                    & 255] | t >> 6);
      mdst[adst + 11] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 17]                    & 255];
      mdst[adst + 12] = (byte) (w0[ msrc[asrc + 16]                    & 255] | t >> 6);
      mdst[adst + 13] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 19]                    & 255];
      mdst[adst + 14] = (byte) (w0[ msrc[asrc + 18]                    & 255] | t >> 6);
      mdst[adst + 15] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 21]                    & 255];
      mdst[adst + 16] = (byte) (w0[ msrc[asrc + 20]                    & 255] | t >> 6);
      mdst[adst + 17] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 23]                    & 255];
      mdst[adst + 18] = (byte) (w0[ msrc[asrc + 22]                    & 255] | t >> 6);
      mdst[adst + 19] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 25] | msrc[asrc + 27]) & 255];
      mdst[adst + 20] = (byte) (w0[(msrc[asrc + 24] | msrc[asrc + 26]) & 255] | t >> 6);
      mdst[adst + 21] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 29] | msrc[asrc + 31]) & 255];
      mdst[adst + 22] = (byte) (w0[(msrc[asrc + 28] | msrc[asrc + 30]) & 255] | t >> 6);
      mdst[adst + 23] = (byte) (t << 2);
      asrc += 2 * 16;
      adst += 2 * 12;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12Test1()

  //fntConvertKNJ16X16toKNJ12X12Test2 ()
  //  KNJ16X16をKNJ12X12に縮小する
  //  □□■■□■□■ □■□■■□□■    □■□■□■□■ □■□■
  //  □□■■□■□■ □■□■■□□■    ■□■□■□■□ ■□■□
  //  ■■□□■□■□ ■□■□□■■□    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□□■■□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■■□□■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□□■■□ → ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■■□□■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□□■■□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■■□□■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□□■■□    ■□■□■□■□ ■□■□
  //  □□■■□■□■ □■□■■□□■    □■□■□■□■ □■□■
  //  ■■□□■□■□ ■□■□□■■□    ■□■□■□■□ ■□■□
  //  ■■□□■□■□ ■□■□□■■□
  //  □□■■□■□■ □■□■■□□■
  //  □□■■□■□■ □■□■■□□■
  //  ■■□□■□■□ ■□■□□■■□
  public static void fntConvertKNJ16X16toKNJ12X12Test2 () {
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w0 = new int[256];
    int[] w1 = new int[256];
    for (int i = 0; i < 256; i++) {
      w0[i] = (i & 0b10000000) | (i & 0b01100000) << 1 | (i & 0b00011111) << 2;
      w1[i] = (i & 0b11110000) | (i & 0b00001100) << 1 | (i & 0b00000011) << 2;
    }
    for (int i = 0; i < 94 * 77; i++) {
      int t =                   w1[(msrc[asrc +  1] | msrc[asrc +  3]) & 255];
      mdst[adst     ] = (byte) (w0[(msrc[asrc     ] | msrc[asrc +  2]) & 255] | t >> 6);
      mdst[adst +  1] = (byte) (t << 2);
      t =                       w1[(msrc[asrc +  5] | msrc[asrc +  7]) & 255];
      mdst[adst +  2] = (byte) (w0[(msrc[asrc +  4] | msrc[asrc +  6]) & 255] | t >> 6);
      mdst[adst +  3] = (byte) (t << 2);
      t =                       w1[ msrc[asrc +  9]                    & 255];
      mdst[adst +  4] = (byte) (w0[ msrc[asrc +  8]                    & 255] | t >> 6);
      mdst[adst +  5] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 11]                    & 255];
      mdst[adst +  6] = (byte) (w0[ msrc[asrc + 10]                    & 255] | t >> 6);
      mdst[adst +  7] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 13]                    & 255];
      mdst[adst +  8] = (byte) (w0[ msrc[asrc + 12]                    & 255] | t >> 6);
      mdst[adst +  9] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 15]                    & 255];
      mdst[adst + 10] = (byte) (w0[ msrc[asrc + 14]                    & 255] | t >> 6);
      mdst[adst + 11] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 17]                    & 255];
      mdst[adst + 12] = (byte) (w0[ msrc[asrc + 16]                    & 255] | t >> 6);
      mdst[adst + 13] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 19]                    & 255];
      mdst[adst + 14] = (byte) (w0[ msrc[asrc + 18]                    & 255] | t >> 6);
      mdst[adst + 15] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 21]                    & 255];
      mdst[adst + 16] = (byte) (w0[ msrc[asrc + 20]                    & 255] | t >> 6);
      mdst[adst + 17] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 23] | msrc[asrc + 25]) & 255];
      mdst[adst + 18] = (byte) (w0[(msrc[asrc + 22] | msrc[asrc + 24]) & 255] | t >> 6);
      mdst[adst + 19] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 27] | msrc[asrc + 29]) & 255];
      mdst[adst + 20] = (byte) (w0[(msrc[asrc + 26] | msrc[asrc + 28]) & 255] | t >> 6);
      mdst[adst + 21] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 31]                    & 255];
      mdst[adst + 22] = (byte) (w0[ msrc[asrc + 30]                    & 255] | t >> 6);
      mdst[adst + 23] = (byte) (t << 2);
      asrc += 2 * 16;
      adst += 2 * 12;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12Test2()

  //fntConvertKNJ16X16toKNJ12X12Test3 ()
  //  KNJ16X16をKNJ12X12に縮小する
  //  □□■□□■□■ □■□□■□□■    □■□■□■□■ □■□■
  //  □□■□□■□■ □■□□■□□■    ■□■□■□■□ ■□■□
  //  ■■□■■□■□ ■□■■□■■□    □■□■□■□■ □■□■
  //  □□■□□■□■ □■□□■□□■    ■□■□■□■□ ■□■□
  //  □□■□□■□■ □■□□■□□■    □■□■□■□■ □■□■
  //  ■■□■■□■□ ■□■■□■■□ → ■□■□■□■□ ■□■□
  //  □□■□□■□■ □■□□■□□■    □■□■□■□■ □■□■
  //  ■■□■■□■□ ■□■■□■■□    ■□■□■□■□ ■□■□
  //  □□■□□■□■ □■□□■□□■    □■□■□■□■ □■□■
  //  ■■□■■□■□ ■□■■□■■□    ■□■□■□■□ ■□■□
  //  □□■□□■□■ □■□□■□□■    □■□■□■□■ □■□■
  //  □□■□□■□■ □■□□■□□■    ■□■□■□■□ ■□■□
  //  ■■□■■□■□ ■□■■□■■□
  //  □□■□□■□■ □■□□■□□■
  //  □□■□□■□■ □■□□■□□■
  //  ■■□■■□■□ ■□■■□■■□
  public static void fntConvertKNJ16X16toKNJ12X12Test3 () {
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w0 = new int[256];
    int[] w1 = new int[256];
    for (int i = 0; i < 256; i++) {
      w0[i] = (i & 0b10000000) | (i & 0b01110000) << 1 | (i & 0b00001111) << 2;
      w1[i] = (i & 0b11100000) | (i & 0b00011100) << 1 | (i & 0b00000011) << 2;
    }
    for (int i = 0; i < 94 * 77; i++) {
      int t =                   w1[(msrc[asrc +  1] | msrc[asrc +  3]) & 255];
      mdst[adst     ] = (byte) (w0[(msrc[asrc     ] | msrc[asrc +  2]) & 255] | t >> 6);
      mdst[adst +  1] = (byte) (t << 2);
      t =                       w1[ msrc[asrc +  5]                    & 255];
      mdst[adst +  2] = (byte) (w0[ msrc[asrc +  4]                    & 255] | t >> 6);
      mdst[adst +  3] = (byte) (t << 2);
      t =                       w1[(msrc[asrc +  7] | msrc[asrc +  9]) & 255];
      mdst[adst +  4] = (byte) (w0[(msrc[asrc +  6] | msrc[asrc +  8]) & 255] | t >> 6);
      mdst[adst +  5] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 11]                    & 255];
      mdst[adst +  6] = (byte) (w0[ msrc[asrc + 10]                    & 255] | t >> 6);
      mdst[adst +  7] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 13]                    & 255];
      mdst[adst +  8] = (byte) (w0[ msrc[asrc + 12]                    & 255] | t >> 6);
      mdst[adst +  9] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 15]                    & 255];
      mdst[adst + 10] = (byte) (w0[ msrc[asrc + 14]                    & 255] | t >> 6);
      mdst[adst + 11] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 17]                    & 255];
      mdst[adst + 12] = (byte) (w0[ msrc[asrc + 16]                    & 255] | t >> 6);
      mdst[adst + 13] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 19]                    & 255];
      mdst[adst + 14] = (byte) (w0[ msrc[asrc + 18]                    & 255] | t >> 6);
      mdst[adst + 15] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 21] | msrc[asrc + 23]) & 255];
      mdst[adst + 16] = (byte) (w0[(msrc[asrc + 20] | msrc[asrc + 22]) & 255] | t >> 6);
      mdst[adst + 17] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 25]                    & 255];
      mdst[adst + 18] = (byte) (w0[ msrc[asrc + 24]                    & 255] | t >> 6);
      mdst[adst + 19] = (byte) (t << 2);
      t =                       w1[(msrc[asrc + 27] | msrc[asrc + 29]) & 255];
      mdst[adst + 20] = (byte) (w0[(msrc[asrc + 26] | msrc[asrc + 28]) & 255] | t >> 6);
      mdst[adst + 21] = (byte) (t << 2);
      t =                       w1[ msrc[asrc + 31]                    & 255];
      mdst[adst + 22] = (byte) (w0[ msrc[asrc + 30]                    & 255] | t >> 6);
      mdst[adst + 23] = (byte) (t << 2);
      asrc += 2 * 16;
      adst += 2 * 12;
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12Test3()

  //fntConvertKNJ16X16toKNJ12X12Test4 ()
  //  KNJ16X16をKNJ12X12に縮小する
  //  □□□□■■■■ □□□□■■■■    □□□■■■□□ □■■■
  //  □□□□■■■■ □□□□■■■■    □□□■■■□□ □■■■
  //  □□□□■■■■ □□□□■■■■    □□□■■■□□ □■■■
  //  □□□□■■■■ □□□□■■■■    ■■■□□□■■ ■□□□
  //  ■■■■□□□□ ■■■■□□□□    ■■■□□□■■ ■□□□
  //  ■■■■□□□□ ■■■■□□□□ → ■■■□□□■■ ■□□□
  //  ■■■■□□□□ ■■■■□□□□    □□□■■■□□ □■■■
  //  ■■■■□□□□ ■■■■□□□□    □□□■■■□□ □■■■
  //  □□□□■■■■ □□□□■■■■    □□□■■■□□ □■■■
  //  □□□□■■■■ □□□□■■■■    ■■■□□□■■ ■□□□
  //  □□□□■■■■ □□□□■■■■    ■■■□□□■■ ■□□□
  //  □□□□■■■■ □□□□■■■■    ■■■□□□■■ ■□□□
  //  ■■■■□□□□ ■■■■□□□□
  //  ■■■■□□□□ ■■■■□□□□
  //  ■■■■□□□□ ■■■■□□□□
  //  ■■■■□□□□ ■■■■□□□□
  //   a b c d     e f g
  //  □□□□ → □□□    e = a | b & ~c & d
  //  □□□■ → □□■    f = b & c | ~a & (b | c) & ~d
  //  □□■□ → □■□    g = a & ~b & c | d
  //  □□■■ → □□■
  //  □■□□ → □■□
  //  □■□■ → ■□■
  //  □■■□ → □■□
  //  □■■■ → □■■
  //  ■□□□ → ■□□
  //  ■□□■ → ■□■
  //  ■□■□ → ■□■
  //  ■□■■ → ■□■
  //  ■■□□ → ■□□
  //  ■■□■ → ■□■
  //  ■■■□ → ■■□
  //  ■■■■ → ■■■
  //  for (int a = 0; a <= 1; a++) {
  //    for (int b = 0; b <= 1; b++) {
  //      for (int c = 0; c <= 1; c++) {
  //        for (int d = 0; d <= 1; d++) {
  //          int e = a | b & ~c & d;
  //          int f = b & c | ~a & (b | c) & ~d;
  //          int g = a & ~b & c | d;
  //          System.out.printf ("%d%d%d%d %d%d%d\n", a, b, c, d, e, f, g);
  //        }
  //      }
  //    }
  //  }
  //private static final long FNT_TEST4_BASE = 0b1110_1100_1010_1000_1010_1010_1010_1000_0110_0100_1010_0100_0010_0100_0010_0000L;
  public static final long FNT_TEST4_BASE = (
    0b000_0L       |  //0000
    0b001_0L <<  4 |  //0001
    0b010_0L <<  8 |  //0010
    0b011_0L << 12 |  //0011 *
    0b010_0L << 16 |  //0100
    0b101_0L << 20 |  //0101
    0b010_0L << 24 |  //0110
    0b011_0L << 28 |  //0111
    0b100_0L << 32 |  //1000
    0b101_0L << 36 |  //1001
    0b101_0L << 40 |  //1010
    0b101_0L << 44 |  //1011
    0b110_0L << 48 |  //1100 *
    0b101_0L << 52 |  //1101
    0b110_0L << 56 |  //1110
    0b111_0L << 60);  //1111
  public static int[] fntReverseMap;
  public static int[] fntTest4Map;
  public static void fntConvertKNJ16X16toKNJ12X12Test4 () {
    int[] r = fntReverseMap;
    if (r == null) {
      r = fntReverseMap = new int[65536];
      for (int i = 0; i < 65536; i++) {
        //  0123    048c
        //  4567 → 159d
        //  89ab    26ae
        //  cdef    37bf
        r[i] = VideoController.VCN_TXP3[i >> 12] | VideoController.VCN_TXP2[i >> 8 & 15] | VideoController.VCN_TXP1[i >> 4 & 15] | VideoController.VCN_TXP0[i & 15];
        //System.out.printf ("%04x %04x\n", i, r[i]);
      }
    }
    int[] m = fntTest4Map;
    if (m == null) {
      int[] n = new int[65536];
      for (int i = 0; i < 65536; i++) {
        n[i] = ((int) (FNT_TEST4_BASE >> (i >> 12 - 2 & 15 << 2)) << 12 & 0xf000 |
                (int) (FNT_TEST4_BASE >> (i >>  8 - 2 & 15 << 2)) <<  8 & 0x0f00 |
                (int) (FNT_TEST4_BASE >> (i >>  4 - 2 & 15 << 2)) <<  4 & 0x00f0 |
                (int) (FNT_TEST4_BASE >> (i <<      2 & 15 << 2))       & 0x000f);
        //System.out.printf ("%04x %04x\n", i, n[i]);
      }
      m = fntTest4Map = new int[65536];
      for (int i = 0; i < 65536; i++) {
        m[i] = r[n[r[n[i]]]] | n[r[n[r[i]]]];
        //System.out.printf ("%04x %04x\n", i, m[i]);
      }
    }
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    for (int i = 0; i < 4 * 94 * 77; i++) {
      int s0 = msrc[asrc    ];
      int s1 = msrc[asrc + 1];
      int s2 = msrc[asrc + 2];
      int s3 = msrc[asrc + 3];
      int s4 = msrc[asrc + 4];
      int s5 = msrc[asrc + 5];
      int s6 = msrc[asrc + 6];
      int s7 = msrc[asrc + 7];
      int t0 = m[s0 <<  8 & 0xf000 | s2 << 4 & 0x0f00 | s4      & 0x00f0 | s6 >> 4 & 0x000f];
      int t1 = m[s0 << 12 & 0xf000 | s2 << 8 & 0x0f00 | s4 << 4 & 0x00f0 | s6      & 0x000f];
      int t2 = m[s1 <<  8 & 0xf000 | s3 << 4 & 0x0f00 | s5      & 0x00f0 | s7 >> 4 & 0x000f];
      int t3 = m[s1 << 12 & 0xf000 | s3 << 8 & 0x0f00 | s5 << 4 & 0x00f0 | s7      & 0x000f];
      mdst[adst    ] = (byte) (t0 >> 13 - 5 & 0b11100000 | t1 >> 13 - 2 & 0b00011100 | t2 >> 13 + 1 & 0b00000011);
      mdst[adst + 1] = (byte) (t2 >> 13 - 7 & 0b10000000 | t3 >> 13 - 4 & 0b01110000);
      mdst[adst + 2] = (byte) (t0 >>  9 - 5 & 0b11100000 | t1 >>  9 - 2 & 0b00011100 | t2 >>  9 + 1 & 0b00000011);
      mdst[adst + 3] = (byte) (t2 >>  9 - 7 & 0b10000000 | t3 >>  9 - 4 & 0b01110000);
      mdst[adst + 4] = (byte) (t0           & 0b11100000 | t1 >>  5 - 2 & 0b00011100 | t2 >>  5 + 1 & 0b00000011);
      mdst[adst + 5] = (byte) (t2 <<  7 - 5 & 0b10000000 | t3 >>  5 - 4 & 0b01110000);
      asrc += 2 * (16 / 4);
      adst += 2 * (12 / 4);
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12Test4()

  //fntConvertKNJ16X16toKNJ12X12Test5 ()
  //  KNJ16X16をKNJ12X12に縮小する
  public static int[] fntTest5Map;
  public static void fntConvertKNJ16X16toKNJ12X12Test5 () {
    int[] map = fntTest5Map;
    if (map == null) {
      map = fntTest5Map = new int[65536];
      int[] o = new int[33];
      //  perl -e "use integer;for$i(0..32){printf'%d,',-($i>>1^-($i&1)),10}"
      //  0,1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10,11,-11,12,-12,13,-13,14,-14,15,-15,16,-16,
      for (int i = 0; i <= 32; i++) {
        o[i] = -(i >> 1 ^ - (i & 1));
      }
      for (int i = 0; i < 65536; i++) {
        //  0→1,1→0の変化点を列挙する。最初の0→1から最後の1→0まで最大で16箇所
        int x = i << 1 ^ i;  //変化点。マスクは17bit。変化点は偶数個なので必ず隙間がある
        //  変化点が14箇所以上あるとき中央に近い連続している2個で1→0,0→1であるものを省くことを繰り返して12箇所以内にする
        for (int j = 0, k = Integer.bitCount (x); k > 12; j++) {
          int p = 8 + o[j];
          if ((~x & 3 << p) == 0 &&  //2個連続している
              (Integer.bitCount (x & (p - 1)) & 1) != 0) {  //これより右側に変化点が奇数個あるすなわちこの2個は1→0,0→1である
            x -= 3 << p;  //2個省く
            k -= 2;
          }
        }
        //  中央に近い変化点から3/4の位置に動かす。入らないときは最も近い隙間に入れる。順序が変わるが連続している変化点がずれるだけ
        int y = 0;
        for (int j = 0; j <= 16; j++) {
          int p = 8 + o[j];  //元の位置
          if ((x & 1 << p) != 0) {  //変化点がある
            int q = p * 3 + 2 >> 2;  //3/4に最も近い位置
            int s = p * 3 >= q << 2 ? 1 : -1;  //3/4に最も近い位置が3/4と等しいか小さいとき次の候補は左側、3/4よりも大きいとき次の候補は右側
            for (int k = 0; k <= 32; k++) {
              int r = q + s * o[k];  //3/4に近い位置
              if (0 <= r && r <= 12 && (y & 1 << r) == 0) {  //範囲内で隙間がある
                y |= 1 << r;  //変化点を入れる
                break;
              }
            }
          }
        }
        //  変化点を0→1,1→0に戻す
        x = 0;
        for (int q = 12; q > 0; q--) {
          if ((y & 1 << q) != 0) {
            x ^= (1 << 4 << q) - 1;  //左に寄せる
          }
        }
        map[i] = x;
      }
    }
    int[] rev = fntReverseMap;
    if (rev == null) {
      rev = fntReverseMap = new int[65536];
      for (int i = 0; i < 65536; i++) {
        //  0123    048c
        //  4567 → 159d
        //  89ab    26ae
        //  cdef    37bf
        rev[i] = VideoController.VCN_TXP3[i >> 12] | VideoController.VCN_TXP2[i >> 8 & 15] | VideoController.VCN_TXP1[i >> 4 & 15] | VideoController.VCN_TXP0[i & 15];
        //System.out.printf ("%04x %04x\n", i, r[i]);
      }
    }
    FontPage src = fntPageKNJ16X16;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w1 = new int[16];
    int[] w2 = new int[16];
    for (int i = 0; i < 94 * 77; i++) {
      if (false) {
        //とりあえず横方向だけ圧縮してみる
        for (int v = 0; v < 12; v++) {
          int t = map[(char) (msrc[asrc] << 8 | msrc[asrc + 1] & 255)];
          mdst[adst    ] = (byte) (t >> 8);
          mdst[adst + 1] = (byte) t;
          asrc += 2;
          adst += 2;
        }
        asrc += 2 * (16 - 12);
      } else if (false) {
        //横方向に圧縮してから縦方向に圧縮する
        //入力
        for (int v = 0; v < 16; v++) {
          w2[v] = (char) (msrc[asrc] << 8 | msrc[asrc + 1] & 255);
          asrc += 2;
        }
        //圧縮
        for (int v = 0; v < 16; v++) {
          w1[v] = map[w2[v]];
          w2[v] = 0;
        }
        //反転
        for (int v = 0; v < 16; v += 4) {
          for (int u = 0; u < 16; u += 4) {
            int t = rev[w1[u    ] << v       & 0xf000 |
                        w1[u + 1] << v >>  4 & 0x0f00 |
                        w1[u + 2] << v >>  8 & 0x00f0 |
                        w1[u + 3] << v >> 12 & 0x000f];
            w2[v    ] |= (t & 0xf000)       >> u;
            w2[v + 1] |= (t & 0x0f00) <<  4 >> u;
            w2[v + 2] |= (t & 0x00f0) <<  8 >> u;
            w2[v + 3] |= (t & 0x000f) << 12 >> u;
          }
        }
        //圧縮
        for (int v = 0; v < 16; v++) {
          w1[v] = map[w2[v]];
          w2[v] = 0;
        }
        //反転
        for (int v = 0; v < 16; v += 4) {
          for (int u = 0; u < 16; u += 4) {
            int t = rev[w1[u    ] << v       & 0xf000 |
                        w1[u + 1] << v >>  4 & 0x0f00 |
                        w1[u + 2] << v >>  8 & 0x00f0 |
                        w1[u + 3] << v >> 12 & 0x000f];
            w2[v    ] |= (t & 0xf000)       >> u;
            w2[v + 1] |= (t & 0x0f00) <<  4 >> u;
            w2[v + 2] |= (t & 0x00f0) <<  8 >> u;
            w2[v + 3] |= (t & 0x000f) << 12 >> u;
          }
        }
        //出力
        for (int v = 0; v < 12; v++) {
          int t = w2[v];
          mdst[adst    ] = (byte) (t >> 8);
          mdst[adst + 1] = (byte) t;
          adst += 2;
        }
      } else {
        //縦方向に圧縮してから横方向に圧縮する
        //入力
        for (int v = 0; v < 16; v++) {
          w1[v] = (char) (msrc[asrc] << 8 | msrc[asrc + 1] & 255);
          w2[v] = 0;
          asrc += 2;
        }
        //反転
        for (int v = 0; v < 16; v += 4) {
          for (int u = 0; u < 16; u += 4) {
            int t = rev[w1[u    ] << v       & 0xf000 |
                        w1[u + 1] << v >>  4 & 0x0f00 |
                        w1[u + 2] << v >>  8 & 0x00f0 |
                        w1[u + 3] << v >> 12 & 0x000f];
            w2[v    ] |= (t & 0xf000)       >> u;
            w2[v + 1] |= (t & 0x0f00) <<  4 >> u;
            w2[v + 2] |= (t & 0x00f0) <<  8 >> u;
            w2[v + 3] |= (t & 0x000f) << 12 >> u;
          }
        }
        //圧縮
        for (int v = 0; v < 16; v++) {
          w1[v] = map[w2[v]];
          w2[v] = 0;
        }
        //反転
        for (int v = 0; v < 16; v += 4) {
          for (int u = 0; u < 16; u += 4) {
            int t = rev[w1[u    ] << v       & 0xf000 |
                        w1[u + 1] << v >>  4 & 0x0f00 |
                        w1[u + 2] << v >>  8 & 0x00f0 |
                        w1[u + 3] << v >> 12 & 0x000f];
            w2[v    ] |= (t & 0xf000)       >> u;
            w2[v + 1] |= (t & 0x0f00) <<  4 >> u;
            w2[v + 2] |= (t & 0x00f0) <<  8 >> u;
            w2[v + 3] |= (t & 0x000f) << 12 >> u;
          }
        }
        //圧縮
        for (int v = 0; v < 16; v++) {
          w1[v] = map[w2[v]];
        }
        //出力
        for (int v = 0; v < 12; v++) {
          int t = w1[v];
          mdst[adst    ] = (byte) (t >> 8);
          mdst[adst + 1] = (byte) t;
          adst += 2;
        }
      }
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ16X16toKNJ12X12Test5()

  //fntConvertKNJ24X24toKNJ12X12IOCS ()
  //  IOCS _FNTADRの方法でKNJ24X24をKNJ12X12に縮小する
  //  ROM1.0  0x00ff6c90
  //  ROM1.1  0x00ff6fde
  //  ROM1.2  0x00ff70d6
  //  ROM1.3  0x00ff74b0
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    ■□■□■□■□ ■□■□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■ → ■□■□■□■□ ■□■□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    □■□■□■□■ □■□■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■    ■□■□■□■□ ■□■□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    □■□■□■□■ □■□■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□    ■□■□■□■□ ■□■□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  □□■■□□■■ □□■■□□■■ □□■■□□■■
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  //  ■■□□■■□□ ■■□□■■□□ ■■□□■■□□
  public static void fntConvertKNJ24X24toKNJ12X12IOCS () {
    FontPage src = fntPageKNJ24X24;
    FontPage dst = fntPageKNJ12X12;
    byte[] msrc = src.fnpDataMemory;
    byte[] mdst = dst.fnpDataMemory;
    int asrc = src.fnpDataAddress;
    int adst = dst.fnpDataAddress;
    int[] w = new int[256];
    for (int i = 0; i < 256; i++) {
      w[i] = (i & 0b10000000) | (i & 0b01100000) << 1 | (i & 0b00011000) << 2 | (i & 0b00000110) << 3 | (i & 0b00000001) << 4;
    }
    for (int i = 0; i < 12 * 94 * 77; i++) {
      mdst[adst    ] = (byte) (w[(msrc[asrc    ] | msrc[asrc + 3]) & 255] | w[(msrc[asrc + 1] | msrc[asrc + 4]) & 255] >> 4);
      mdst[adst + 1] = (byte)  w[(msrc[asrc + 2] | msrc[asrc + 5]) & 255];
      asrc += 3 * (24 / 12);
      adst += 2 * (12 / 12);
    }
    dst.fnpMemoryToImage ();
    if (dst == fntSelectedPage) {
      fntCanvas.repaint ();
    }
  }  //fntConvertKNJ24X24toKNJ12X12IOCS()

  //! μEmacsの方法とKo-Windowの方法も確認する

  //fntSelectPage (page)
  //  ページを選択する
  public static void fntSelectPage (FontPage page) {
    fntSelectedPage = page;
    if (fntCanvas != null) {
      fntCanvas.setImage (page.fnpGetImage ());
    }
  }  //fntSelectPage(FontPage)



  //========================================================================================
  //$$FPU FPU

  //FPU/FPCP
  public static ExpressionEvaluator fpuMotherboardCoprocessor;  //マザーボードコプロセッサ
  public static ExpressionEvaluator fpuOnChipFPU;  //on-chip FPU
  public static ExpressionEvaluator fpuBox;  //浮動小数点命令を実行するFPU/FPCP

  //数値演算プロセッサボード
  public static EFPBox fpuCoproboard1;  //数値演算プロセッサボード1
  public static EFPBox fpuCoproboard2;  //数値演算プロセッサボード2

  //フルスペックFPU
  public static boolean fpuFullSpec;  //true=MC68060のon-chip FPUにすべての浮動小数点命令を実装する

  //浮動小数点レジスタ
  public static EFPBox.EFP[] fpuFPn;

  //FPCR control register
  //  exception enable byte
  public static final int FPU_FPCR_BSUN   = 0b00000000_00000000_10000000_00000000;  //branch/set on unordered
  public static final int FPU_FPCR_SNAN   = 0b00000000_00000000_01000000_00000000;  //signaling not a number
  public static final int FPU_FPCR_OPERR  = 0b00000000_00000000_00100000_00000000;  //operand error
  public static final int FPU_FPCR_OVFL   = 0b00000000_00000000_00010000_00000000;  //overflow
  public static final int FPU_FPCR_UNFL   = 0b00000000_00000000_00001000_00000000;  //underflow
  public static final int FPU_FPCR_DZ     = 0b00000000_00000000_00000100_00000000;  //divide by zero
  public static final int FPU_FPCR_INEX2  = 0b00000000_00000000_00000010_00000000;  //inexact operation
  public static final int FPU_FPCR_INEX1  = 0b00000000_00000000_00000001_00000000;  //inexact decimal input
  //  mode control byte
  //    rounding precision
  public static final int FPU_FPCR_PE     = 0b00000000_00000000_00000000_00000000;  //extended
  public static final int FPU_FPCR_PS     = 0b00000000_00000000_00000000_01000000;  //single
  public static final int FPU_FPCR_PD     = 0b00000000_00000000_00000000_10000000;  //double
  //    rounding mode
  public static final int FPU_FPCR_RN     = 0b00000000_00000000_00000000_00000000;  //to nearest
  public static final int FPU_FPCR_RZ     = 0b00000000_00000000_00000000_00010000;  //toward zero
  public static final int FPU_FPCR_RM     = 0b00000000_00000000_00000000_00100000;  //toward minus infinity
  public static final int FPU_FPCR_RP     = 0b00000000_00000000_00000000_00110000;  //toward plus infinity

  //FPSR status register
  //  condition code byte
  public static final int FPU_FPSR_N         = 0b00001000_00000000_00000000_00000000;  //negative
  public static final int FPU_FPSR_Z         = 0b00000100_00000000_00000000_00000000;  //zero
  public static final int FPU_FPSR_I         = 0b00000010_00000000_00000000_00000000;  //infinity
  public static final int FPU_FPSR_NAN       = 0b00000001_00000000_00000000_00000000;  //not a number or unordered
  //  quotient byte
  public static final int FPU_FPSR_S         = 0b00000000_10000000_00000000_00000000;  //sign of quotient
  public static final int FPU_FPSR_QUOTIENT  = 0b00000000_01111111_00000000_00000000;  //quotient
  //  exception status byte
  public static final int FPU_FPSR_EXC_BSUN  = 0b00000000_00000000_10000000_00000000;  //branch/set on unordered
  public static final int FPU_FPSR_EXC_SNAN  = 0b00000000_00000000_01000000_00000000;  //signaling not a number
  public static final int FPU_FPSR_EXC_OPERR = 0b00000000_00000000_00100000_00000000;  //operand error
  public static final int FPU_FPSR_EXC_OVFL  = 0b00000000_00000000_00010000_00000000;  //overflow
  public static final int FPU_FPSR_EXC_UNFL  = 0b00000000_00000000_00001000_00000000;  //underflow
  public static final int FPU_FPSR_EXC_DZ    = 0b00000000_00000000_00000100_00000000;  //divide by zero
  public static final int FPU_FPSR_EXC_INEX2 = 0b00000000_00000000_00000010_00000000;  //inexact operation
  public static final int FPU_FPSR_EXC_INEX1 = 0b00000000_00000000_00000001_00000000;  //inexact decimal input
  //  accrued exception byte
  public static final int FPU_FPSR_AEXC_IOP  = 0b00000000_00000000_00000000_10000000;  //invalid operation
  public static final int FPU_FPSR_AEXC_OVFL = 0b00000000_00000000_00000000_01000000;  //overflow
  public static final int FPU_FPSR_AEXC_UNFL = 0b00000000_00000000_00000000_00100000;  //underflow
  public static final int FPU_FPSR_AEXC_DZ   = 0b00000000_00000000_00000000_00010000;  //divide by zero
  public static final int FPU_FPSR_AEXC_INEX = 0b00000000_00000000_00000000_00001000;  //inexact

  //  EXCからAEXCへの変換
  //    AEXC_IOP |= EXC_BSUN | EXC_SNAN | EXC_OPERR
  //    AEXC_OVFL |= EXC_OVFL
  //    AEXC_UNFL |= EXC_UNFL & EXC_INEX2
  //    AEXC_DZ |= EXC_DZ
  //    AEXC_INEX |= EXC_OVFL | EXC_INEX2 | EXC_INEX1
  public static final int[] FPU_FPSR_EXC_TO_AEXC = new int[256];

  //コンディション
  //
  //  fpsrのbit27-24
  //    MZIN
  //    0000  0<x
  //    0001  NaN
  //    0010  +Inf
  //    0100  +0
  //    1000  x<0
  //    1010  -Inf
  //    1100  -0
  //
  //  FPU_CCMAP_882[(オペコードまたは拡張ワード&63)<<4|fpsr>>24&15]==trueならば条件成立
  //  FPU_CCMAP_060[(オペコードまたは拡張ワード&63)<<4|fpsr>>24&15]==trueならば条件成立
  //
  //  MC68882とMC68060ではOR,NE,GLE,SNEの条件が異なる

  //MC68882
  //  perl -e "@a=();for$a(0..15){$m=$a>>3&1;$z=$a>>2&1;$n=$a&1;@b=map{$_&1}(0,$z,~($n|$z|$m),$z|~($n|$m),$m&~($n|$z),$z|($m&~$n),~($n|$z),$z|~$n,$n,$n|$z,$n|~($m|$z),$n|($z|~$m),$n|($m&~$z),$n|$z|$m,$n|~$z,1);push@a,@b,@b,@b,@b}for$y(0..63){print'    ';$t=0;for$x(0..15){$c=$a[$x<<6|$y];$t=($t<<1)+$c;print(($c?'T':'F').',');}printf'  //%04x %06b%c',$t,$y,10;}
  //
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //NAN
  public static final boolean[] FPU_CCMAP_882 = {
    //                                       cccccc  cc    等式          意味
    //IEEEアウェアテスト
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 000000  F     0             偽
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 000001  EQ    Z             等しい
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 000010  OGT   ~(NAN|Z|N)    比較可能でより大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 000011  OGE   Z|~(NAN|N)    等しいか比較可能でより大きい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 000100  OLT   N&~(NAN|Z)    比較可能でより小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 000101  OLE   Z|(N&~NAN)    等しいか比較可能でより小さい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 000110  OGL   ~(NAN|Z)      比較可能で等しくない
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 000111  OR    Z|~NAN        等しいか比較可能
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 001000  UN    NAN           比較不能
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 001001  UEQ   NAN|Z         比較不能か等しい
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 001010  UGT   NAN|~(N|Z)    比較不能かより大きい
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 001011  UGE   NAN|(Z|~N)    比較不能かより大きいか等しい
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 001100  ULT   NAN|(N&~Z)    比較不能かより小さい
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 001101  ULE   NAN|Z|N       比較不能かより小さいか等しい
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 001110  NE    NAN|~Z        比較不能か等しくない
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 001111  T     1             真
    //IEEEノンアウェアテスト
    //  NANがセットされているとき、FPSRのBSUNがセットされ、許可されていれば例外が発生する
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 010000  SF    0             偽(シグナリング)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 010001  SEQ   Z             等しい(シグナリング)
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 010010  GT    ~(NAN|Z|N)    より大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 010011  GE    Z|~(NAN|N)    より大きいか等しい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 010100  LT    N&~(NAN|Z)    より小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 010101  LE    Z|(N&~NAN)    より小さいか等しい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 010110  GL    ~(NAN|Z)      等しくない
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 010111  GLE   Z|~NAN        より大きいか小さいか等しい
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 011000  NGLE  NAN           GLEでない
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 011001  NGL   NAN|Z         GLでない
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 011010  NLE   NAN|~(N|Z)    LEでない
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 011011  NLT   NAN|(Z|~N)    LTでない
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 011100  NGE   NAN|(N&~Z)    GEでない
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 011101  NGT   NAN|Z|N       GTでない
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 011110  SNE   NAN|~Z        等しくない(シグナリング)
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 011111  ST    1             真(シグナリング)
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 100000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 100001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 100010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 100011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 100100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 100101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 100110
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 100111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 101000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 101001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 101010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 101011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 101100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 101101
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 101110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 101111
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 110000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 110001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 110010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 110011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 110100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 110101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 110110
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 110111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 111000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 111001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 111010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 111011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 111100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 111101
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 111110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 111111
  };

  //MC68060
  //  perl -e "@a=();for$a(0..15){$m=$a>>3&1;$z=$a>>2&1;$n=$a&1;@b=map{$_&1}(0,$z,~($n|$z|$m),$z|~($n|$m),$m&~($n|$z),$z|($m&~$n),~($n|$z),~$n,$n,$n|$z,$n|~($m|$z),$n|($z|~$m),$n|($m&~$z),$n|$z|$m,~$z,1);push@a,@b,@b,@b,@b}for$y(0..63){print'    ';$t=0;for$x(0..15){$c=$a[$x<<6|$y];$t=($t<<1)+$c;print(($c?'T':'F').',');}printf'  //%04x %06b%c',$t,$y,10;}
  //
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //NAN
  public static final boolean[] FPU_CCMAP_060 = {
    //                                       cccccc  cc    等式          意味
    //IEEEアウェアテスト
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 000000  F     0             偽
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 000001  EQ    Z             等しい
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 000010  OGT   ~(NAN|Z|N)    比較可能でより大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 000011  OGE   Z|~(NAN|N)    等しいか比較可能でより大きい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 000100  OLT   N&~(NAN|Z)    比較可能でより小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 000101  OLE   Z|(N&~NAN)    等しいか比較可能でより小さい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 000110  OGL   ~(NAN|Z)      比較可能で等しくない
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 000111  OR    ~NAN          比較可能
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 001000  UN    NAN           比較不能
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 001001  UEQ   NAN|Z         比較不能か等しい
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 001010  UGT   NAN|~(N|Z)    比較不能かより大きい
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 001011  UGE   NAN|(Z|~N)    比較不能かより大きいか等しい
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 001100  ULT   NAN|(N&~Z)    比較不能かより小さい
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 001101  ULE   NAN|Z|N       比較不能かより小さいか等しい
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 001110  NE    ~Z            等しくない
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 001111  T     1             真
    //IEEEノンアウェアテスト
    //  NANがセットされているとき、FPSRのBSUNがセットされ、許可されていれば例外が発生する
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 010000  SF    0             偽(シグナリング)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 010001  SEQ   Z             等しい(シグナリング)
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 010010  GT    ~(NAN|Z|N)    より大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 010011  GE    Z|~(NAN|N)    より大きいか等しい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 010100  LT    N&~(NAN|Z)    より小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 010101  LE    Z|(N&~NAN)    より小さいか等しい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 010110  GL    ~(NAN|Z)      等しくない
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 010111  GLE   ~NAN          より大きいか小さいか等しい
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 011000  NGLE  NAN           GLEでない
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 011001  NGL   NAN|Z         GLでない
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 011010  NLE   NAN|~(N|Z)    LEでない
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 011011  NLT   NAN|(Z|~N)    LTでない
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 011100  NGE   NAN|(N&~Z)    GEでない
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 011101  NGT   NAN|Z|N       GTでない
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 011110  SNE   ~Z            等しくない(シグナリング)
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 011111  ST    1             真(シグナリング)
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 100000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 100001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 100010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 100011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 100100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 100101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 100110
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 100111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 101000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 101001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 101010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 101011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 101100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 101101
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 101110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 101111
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 110000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 110001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 110010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 110011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 110100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 110101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 110110
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 110111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 111000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 111001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 111010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 111011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 111100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 111101
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 111110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 111111
  };

  //fpuInit ()
  //  FPUを初期化する
  //  これはmpuInit()から呼ばれる
  public static void fpuInit () {
    for (int i = 0; i < 256; i++) {
      FPU_FPSR_EXC_TO_AEXC[i] = (((i << 8 & (FPU_FPSR_EXC_BSUN | FPU_FPSR_EXC_SNAN | FPU_FPSR_EXC_OPERR)) != 0 ? FPU_FPSR_AEXC_IOP : 0) |
                                 ((i << 8 & FPU_FPSR_EXC_OVFL) != 0 ? FPU_FPSR_AEXC_OVFL : 0) |
                                 ((i << 8 & (FPU_FPSR_EXC_UNFL | FPU_FPSR_EXC_INEX2)) == (FPU_FPSR_EXC_UNFL | FPU_FPSR_EXC_INEX2) ? FPU_FPSR_AEXC_UNFL : 0) |
                                 ((i << 8 & FPU_FPSR_EXC_DZ) != 0 ? FPU_FPSR_AEXC_DZ : 0) |
                                 ((i << 8 & (FPU_FPSR_EXC_OVFL | FPU_FPSR_EXC_INEX2 | FPU_FPSR_EXC_INEX1)) != 0 ? FPU_FPSR_AEXC_INEX : 0));
    }
    //フルスペックFPU
    //fpuFullSpec = false;
    //マザーボードコプロセッサ
    fpuMotherboardCoprocessor = new ExpressionEvaluator ();
    //on-chip FPU
    fpuOnChipFPU = new ExpressionEvaluator ();
    //浮動小数点命令を実行するFPU/FPCP
    fpuBox = mpuCoreType <= 3 ? fpuMotherboardCoprocessor : fpuOnChipFPU;
    //浮動小数点レジスタ
    fpuFPn = fpuBox.epbFPn;
    //数値演算プロセッサボード
    fpuCoproboard1 = new EFPBox ();
    fpuCoproboard2 = new EFPBox ();
  }  //fpuInit()



  //========================================================================================
  //$$DGT コンソール

  public static final int DGT_MAX_OUTPUT_LENGTH = 1024 * 64;  //出力の上限を64KBとする
  public static final int DGT_CUT_OUTPUT_LENGTH = DGT_MAX_OUTPUT_LENGTH + 1024 * 4;  //出力が上限よりも4KB以上長くなったら上限でカットする

  public static final String DGT_PROMPT = "> ";

  //コンポーネント
  public static JFrame dgtFrame;  //ウインドウ
  public static ScrollTextArea dgtBoard;  //テキストエリア
  public static JPopupMenu dgtPopupMenu;  //ポップアップメニュー
  public static JMenuItem dgtPopupCutMenuItem;  //切り取り
  public static JMenuItem dgtPopupCopyMenuItem;  //コピー
  public static JMenuItem dgtPopupPasteMenuItem;  //貼り付け
  public static JMenuItem dgtPopupSelectAllMenuItem;  //すべて選択
  public static int dgtOutputEnd;  //出力された文字列の末尾。リターンキーが押されたらこれ以降に書かれた文字列をまとめて入力する

  public static boolean dgtRequestRegs;  //true=コアが停止したらコンソールにレジスタ一覧とプロンプトを表示する

  //逆アセンブル
  public static int dgtDisassembleLastTail;  //前回逆アセンブルした範囲の終了アドレス
  public static int dgtDisassemblePC;  //前回逆アセンブルした範囲の直後のアドレス。0=PCを使う
  public static int dgtDisassembleFC;  //前回逆アセンブルした範囲のファンクションコード

  //ダンプ
  public static int dgtDumpAddress;  //次回のダンプ開始アドレス
  public static int dgtDumpFunctionCode;  //ファンクションコード

  //dgtInit ()
  //  ターミナルウインドウを初期化する
  public static void dgtInit () {
    dgtFrame = null;
    dgtBoard = null;
    dgtPopupMenu = null;
    dgtPopupCutMenuItem = null;
    dgtPopupCopyMenuItem = null;
    dgtPopupPasteMenuItem = null;
    dgtPopupSelectAllMenuItem = null;
    dgtOutputEnd = DGT_PROMPT.length ();
    dgtRequestRegs = false;
    dgtDisassembleLastTail = 0;
    dgtDisassemblePC = 0;
    dgtDisassembleFC = 6;
    dgtDumpAddress = 0;
    dgtDumpFunctionCode = 5;
  }  //dgtInit()

  //dgtMake ()
  //  コンソールウインドウを作る
  //  ここでは開かない
  public static void dgtMake () {

    //テキストエリア
    dgtBoard = createScrollTextArea (DGT_PROMPT, 500, 600, true);
    dgtBoard.setUnderlineCursorOn (true);
    dgtBoard.setLineWrap (true);  //行を折り返す
    dgtBoard.addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (de.getOffset () < dgtOutputEnd) {
          dgtOutputEnd += de.getLength ();  //出力された文字列の末尾を調整する
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (de.getOffset () < dgtOutputEnd) {
          dgtOutputEnd -= Math.min (de.getLength (), dgtOutputEnd - de.getOffset ());  //出力された文字列の末尾を調整する
        }
      }
    });
    dgtBoard.addKeyListener (new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        if (ke.getKeyCode () == KeyEvent.VK_ENTER) {  //ENTERキーが押された
          ke.consume ();  //ENTERキーをキャンセルする
          dgtEnter ();  //ENTERキーを処理する
        }
      }
    });

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Cut":
          dgtCut ();
          break;
        case "Copy":
          dgtCopy ();
          break;
        case "Paste":
          dgtPaste ();
          break;
        case "Select All":
          dgtSelectAll ();
          break;
        }
      }
    };
    dgtPopupMenu = createPopupMenu (
      dgtPopupCutMenuItem = Multilingual.mlnText (createMenuItem ("Cut", 'T', popupActionListener), "ja", "切り取り"),
      dgtPopupCopyMenuItem = Multilingual.mlnText (createMenuItem ("Copy", 'C', popupActionListener), "ja", "コピー"),
      dgtPopupPasteMenuItem = Multilingual.mlnText (createMenuItem ("Paste", 'P', popupActionListener), "ja", "貼り付け"),
      createHorizontalSeparator (),
      dgtPopupSelectAllMenuItem = Multilingual.mlnText (createMenuItem ("Select All", 'A', popupActionListener), "ja", "すべて選択")
      );
    dgtBoard.addMouseListener (new MouseAdapter () {
      @Override public void mousePressed (MouseEvent me) {
        dgtShowPopup (me);
      }
      @Override public void mouseReleased (MouseEvent me) {
        dgtShowPopup (me);
      }
    });

    //ウインドウ
    dgtFrame = Multilingual.mlnTitle (
      createRestorableSubFrame (
        Settings.SGS_DGT_FRAME_KEY,
        "Console",
        null,
        dgtBoard
        ),
      "ja", "コンソール");

    dgtBoard.setCaretPosition (dgtOutputEnd);

  }  //dgtMake()

  //dgtShowPopup (me)
  //  ポップアップメニューを表示する
  //  テキストエリアのマウスリスナーが呼び出す
  public static void dgtShowPopup (MouseEvent me) {
    if (me.isPopupTrigger ()) {
      //選択範囲があれば切り取りとコピーが有効
      boolean enableCutAndCopy = clpClipboard != null && dgtBoard.getSelectionStart () != dgtBoard.getSelectionEnd ();
      setEnabled (dgtPopupCutMenuItem, enableCutAndCopy);
      setEnabled (dgtPopupCopyMenuItem, enableCutAndCopy);
      //クリップボードに文字列があれば貼り付けが有効
      setEnabled (dgtPopupPasteMenuItem, clpClipboard != null && clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor));
      //クリップボードがあればすべて選択が有効
      setEnabled (dgtPopupSelectAllMenuItem, clpClipboard != null);
      //ポップアップメニューを表示する
      dgtPopupMenu.show (me.getComponent (), me.getX (), me.getY ());
    }
  }  //dgtShowPopup(MouseEvent)

  //dgtCut ()
  //  切り取り
  public static void dgtCut () {
    if (clpClipboard != null) {
      //選択範囲の文字列をコピーする
      clpClipboardString = dgtBoard.getSelectedText ();
      try {
        clpClipboard.setContents (clpStringContents, clpClipboardOwner);
        clpIsClipboardOwner = true;  //自分がコピーした
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を削除する
      dgtBoard.replaceRange ("", dgtBoard.getSelectionStart (), dgtBoard.getSelectionEnd ());
    }
  }  //dgtCut()

  //dgtCopy ()
  //  コピー
  public static void dgtCopy () {
    if (clpClipboard != null) {
      //選択範囲の文字列をコピーする
      String selectedText = dgtBoard.getSelectedText ();
      if (selectedText != null) {
        clpClipboardString = selectedText;
        try {
          clpClipboard.setContents (clpStringContents, clpClipboardOwner);
          clpIsClipboardOwner = true;  //自分がコピーした
        } catch (Exception e) {
          return;
        }
      }
    }
  }  //dgtCopy()

  //dgtPaste ()
  //  貼り付け
  public static void dgtPaste () {
    if (clpClipboard != null) {
      //クリップボードから文字列を取り出す
      String string = null;
      try {
        string = (String) clpClipboard.getData (DataFlavor.stringFlavor);
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を置換する
      dgtBoard.replaceRange (string, dgtBoard.getSelectionStart (), dgtBoard.getSelectionEnd ());
    }
  }  //dgtPaste()

  //dgtSelectAll ()
  //  すべて選択
  public static void dgtSelectAll () {
    if (clpClipboard != null) {
      //すべて選択する
      dgtBoard.selectAll ();
    }
  }  //dgtSelectAll()

  //dgtStart ()
  public static void dgtStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DGT_FRAME_KEY)) {
      dgtOpen ();
    }
  }  //dgtStart()

  //dgtOpen ()
  //  コンソールウインドウを開く
  public static void dgtOpen () {
    if (dgtFrame == null) {
      dgtMake ();
    }
    dgtFrame.setVisible (true);
  }  //dgtOpen()

  //dgtPrintChar (c)
  //  末尾に1文字追加する
  public static void dgtPrintChar (int c) {
    if (c == 0x08) {  //バックスペース
      if (dgtOutputEnd > 0) {
        if (dgtBoard != null) {
          dgtBoard.replaceRange ("", dgtOutputEnd - 1, dgtOutputEnd);  //1文字削除
          dgtOutputEnd--;
          dgtBoard.setCaretPosition (dgtOutputEnd);
        }
      }
    } else if (c >= 0x20 && c != 0x7f || c == 0x09 || c == 0x0a) {  //タブと改行以外の制御コードを除く
      if (dgtBoard != null) {
        dgtBoard.insert (String.valueOf ((char) c), dgtOutputEnd);  //1文字追加
        dgtOutputEnd++;
        if (dgtOutputEnd >= DGT_CUT_OUTPUT_LENGTH) {
          dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
        }
        dgtBoard.setCaretPosition (dgtOutputEnd);
      }
    }
  }  //dgtPrintChar(int)

  //dgtPrint (s)
  //  末尾に文字列を追加する
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void dgtPrint (String s) {
    if (s == null) {
      return;
    }
    if (dgtFrame != null) {
      dgtBoard.insert (s, dgtOutputEnd);  //文字列追加
      dgtOutputEnd += s.length ();
      if (dgtOutputEnd >= DGT_CUT_OUTPUT_LENGTH) {
        dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
      }
      dgtBoard.setCaretPosition (dgtOutputEnd);
    }
  }  //dgtPrint(String)

  //dgtPrintln (s)
  //  末尾に文字列と改行を追加する
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void dgtPrintln (String s) {
    dgtPrint (s);
    dgtPrintChar ('\n');
  }  //dgtPrintln(String)

  //dgtEnter ()
  //  ENTERキーを処理する
  public static void dgtEnter () {
    String text = dgtBoard.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    int outputLineStart = text.lastIndexOf ('\n', dgtOutputEnd - 1) + 1;  //出力の末尾の行の先頭。プロンプトの先頭
    int caretLineStart = text.lastIndexOf ('\n', dgtBoard.getCaretPosition () - 1) + 1;  //キャレットがある行の先頭
    if (outputLineStart <= caretLineStart) {  //出力の末尾の行の先頭以降でENTERキーが押された
      dgtBoard.replaceRange ("", dgtOutputEnd, length);  //入力された文字列を一旦削除する
      dgtSend (text.substring (dgtOutputEnd, length) + "\r");  //入力された文字列を送信する
    } else if (outputLineStart < dgtOutputEnd) {  //出力の末尾の行の先頭よりも手前でENTERキーが押されて、出力の末尾の行にプロンプトがあるとき
      String prompt = text.substring (outputLineStart, dgtOutputEnd);  //出力の末尾の行のプロンプト
      int caretLineEnd = text.indexOf ('\n', caretLineStart);  //キャレットがある行の末尾
      if (caretLineEnd == -1) {
        caretLineEnd = length;
      }
      String line = text.substring (caretLineStart, caretLineEnd);  //キャレットがある行
      int start = line.indexOf (prompt);  //キャレットがある行のプロンプトの先頭
      if (start >= 0) {  //キャレットがある行にプロンプトがあるとき
        dgtOutputEnd = length;  //入力された文字列を無効化する
        if (text.charAt (dgtOutputEnd - 1) != '\n' && !text.endsWith ("\n" + prompt)) {  //改行または改行+プロンプトで終わっていないとき
          dgtBoard.insert ("\n", dgtOutputEnd);  //末尾にENTERを追加する
          dgtOutputEnd++;
          if (dgtOutputEnd >= DGT_CUT_OUTPUT_LENGTH) {
            dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
            dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
          }
        }
        dgtBoard.setCaretPosition (dgtOutputEnd);
        dgtSend (line.substring (start + prompt.length ()) + "\r");  //プロンプトの後ろから行の末尾までを送信する
      }
    }
  }  //dgtEnter()

  //dgtSend (s)
  public static void dgtSend (String s) {
    dgtPrintln (s);
    ExpressionEvaluator.ExpressionElement codeTree = fpuBox.evxParse (s);
    if (codeTree != null) {
      if (false) {
        dgtPrintln (codeTree.toString ());
      }
      codeTree.exlEval ();
      if (true) {
        if (codeTree.exlValueType == ExpressionEvaluator.ElementType.ETY_FLOAT) {
          dgtPrintln (codeTree.exlFloatValue.toString ());
        } else if (codeTree.exlValueType == ExpressionEvaluator.ElementType.ETY_STRING) {
          dgtPrintln (codeTree.exlStringValue);
        }
      }
    }
    if (!dgtRequestRegs) {
      dgtPrintPrompt ();
    }
  }  //dgtSend(String)

  //dgtPrintPrompt (s)
  //  プロンプトを表示する
  //  既に表示されているときは何もしない
  public static void dgtPrintPrompt () {
    String text = dgtBoard.getText ();  //テキスト全体
    if (!text.substring (text.lastIndexOf ('\n', dgtOutputEnd - 1) + 1, dgtOutputEnd).equals (DGT_PROMPT)) {  //プロンプトが表示されていない
      dgtPrint (text.endsWith ("\n") ? DGT_PROMPT : "\n" + DGT_PROMPT);
    }
  }  //dgtPrintPrompt ()



  //========================================================================================
  //$$DBG デバッガ共通コンポーネント

  public static final boolean DBG_ORI_BYTE_ZERO_D0 = true;  //true=ORI.B #$00,D0(オペコード0x0000)を不当命令とみなす機能を有効にする。暴走をなるべく早く検出することで暴走の原因を特定しやすくする

  public static boolean dbgHexSelected;  //true=16進数が選択されている
  public static int dbgHexValue;  //選択されている16進数の値
  public static int dbgSupervisorMode;  //0=ユーザモード,0以外=スーパーバイザモード
  public static JPopupMenu dbgPopupMenu;  //ポップアップメニュー
  public static JMenu dbgPopupIBPMenu;  //命令ブレークポイントメニュー
  public static SpinnerNumberModel dbgPopupIBPCurrentModel;  //現在値のスピナーモデル
  public static int dbgPopupIBPCurrentValue;  //現在値
  public static SpinnerNumberModel dbgPopupIBPThresholdModel;  //閾値のスピナーモデル
  public static int dbgPopupIBPThresholdValue;  //閾値
  public static JMenuItem dbgPopupIBPClearMenuItem;  //解除
  public static JMenu dbgPopupHexMenu;  //16進数メニュー
  public static JMenuItem dbgPopupDisMenuItem;  //逆アセンブル
  public static JMenuItem dbgPopupMemMenuItem;  //メモリダンプ
  public static JMenuItem dbgPopupCopyMenuItem;  //コピー
  public static JMenuItem dbgPopupSelectAllMenuItem;  //すべて選択
  public static JTextArea dbgPopupTextArea;  //ポップアップメニューを表示したテキストエリア
  public static int dbgEventMask;  //イベントマスク。0でないときチェンジリスナーとキャレットリスナーを無効化
  public static boolean dbgStopOnError;  //true=エラーが発生したときコアを止める
  public static boolean dbgOriByteZeroD0;  //true=ORI.B #$00,D0を不当命令とみなす。普段はOFFにしておくこと

  //共通
  //  sb.append(DBG_SPACES,0,length)でStringBuilderに連続する空白を追加するための配列
  public static final char[] DBG_SPACES = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "                                                                                ").toCharArray ();

  public static final int DBG_DRP_VISIBLE_MASK = 1;  //レジスタウインドウが表示されている
  public static final int DBG_DDP_VISIBLE_MASK = 2;  //逆アセンブルリストウインドウが表示されている
  public static final int DBG_DMP_VISIBLE_MASK = 4;  //メモリダンプウインドウが表示されている
  public static final int DBG_BLG_VISIBLE_MASK = 8;  //分岐ログが表示されている
  public static final int DBG_PFV_VISIBLE_MASK = 16;  //プログラムフロービジュアライザが表示されている
  public static final int DBG_RBP_VISIBLE_MASK = 32;  //ラスタブレークポイントウインドウが表示されている
  public static final int DBG_DBP_VISIBLE_MASK = 64;  //データブレークポイントウインドウが表示されている
  public static final int DBG_SMT_VISIBLE_MASK = 128;  //表示モードテストが表示されている
  public static final int DBG_ATW_VISIBLE_MASK = 256;  //アドレス変換ウインドウが表示されている
  public static final int DBG_PAA_VISIBLE_MASK = 512;  //物理アドレス空間ウインドウが表示されている
  public static final int DBG_RTL_VISIBLE_MASK = 1024;  //ルートポインタリストが表示されている
  public static final int DBG_SPV_VISIBLE_MASK = 2048;  //スプライトパターンビュアが表示されている
  public static final int DBG_ACM_VISIBLE_MASK = 4096;  //アドレス変換キャッシュモニタが表示されている
  public static int dbgVisibleMask;  //表示されているデバッグ関連ウインドウのマスク

  //dbgInit ()
  //  初期化
  public static void dbgInit () {
    dbgVisibleMask = 0;
    dbgHexSelected = false;
    dbgHexValue = 0;
    dbgSupervisorMode = 1;
    dbgPopupMenu = null;
    dbgPopupDisMenuItem = null;
    dbgPopupMemMenuItem = null;
    dbgPopupCopyMenuItem = null;
    dbgPopupSelectAllMenuItem = null;
    dbgPopupIBPMenu = null;
    dbgPopupIBPCurrentModel = null;
    dbgPopupIBPCurrentValue = 0;
    dbgPopupIBPThresholdModel = null;
    dbgPopupIBPThresholdValue = 0;
    dbgPopupHexMenu = null;
    dbgPopupTextArea = null;
    dbgEventMask = 0;
    dbgStopOnError = false;  //ウインドウを表示する前にも必要なのでここで初期化すること
    if (DBG_ORI_BYTE_ZERO_D0) {
      dbgOriByteZeroD0 = false;
    }
  }  //dbgInit()

  //dbgMake ()
  //  デバッグ関連ウインドウの共通コンポーネントを作る
  public static void dbgMake () {

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Disassemble":
          DisassembleList.ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
          DisassembleList.ddpOpen (dbgHexValue, dbgSupervisorMode, false);
          break;
        case "Memory Dump":
          MemoryDumpList.dmpOpen (dbgHexValue, dbgSupervisorMode, false);
          break;
        case "Run to Here":
          if (InstructionBreakPoint.IBP_ON) {
            if (mpuTask == null) {
              InstructionBreakPoint.ibpInstant (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode);
              mpuStart ();
            }
          }
          break;
        case "Set Breakpoint":
          if (InstructionBreakPoint.IBP_ON) {
            InstructionBreakPoint.ibpPut (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode, dbgPopupIBPCurrentValue, dbgPopupIBPThresholdValue, null);
            DisassembleList.ddpOpen (0, DisassembleList.ddpSupervisorMode, true);
          }
          break;
        case "Clear Breakpoint":
          if (InstructionBreakPoint.IBP_ON) {
            InstructionBreakPoint.ibpRemove (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode);
            DisassembleList.ddpOpen (0, DisassembleList.ddpSupervisorMode, true);
          }
          break;
        case "Copy":
          dbgCopy ();
          break;
        case "Select All":
          dbgSelectAll ();
          break;
        }
      }
    };
    dbgPopupMenu = createPopupMenu (
      dbgPopupIBPMenu =
      InstructionBreakPoint.IBP_ON ?
      createMenu (
        "XXXXXXXX", KeyEvent.VK_UNDEFINED,
        Multilingual.mlnText (createMenuItem ("Run to Here", 'R', popupActionListener), "ja", "ここまで実行"),
        createHorizontalSeparator (),
        Multilingual.mlnText (createMenuItem ("Set Breakpoint", 'S', popupActionListener), "ja", "ブレークポイントを設定"),
        createHorizontalBox (
          Box.createHorizontalStrut (7),
          Box.createHorizontalGlue (),
          setPreferredSize (Multilingual.mlnText (createLabel ("current"), "ja", "現在値"), 60, 16),
          createNumberSpinner (dbgPopupIBPCurrentModel = new SpinnerNumberModel (0, 0, 0x7fffffff, 1), 10, new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              dbgPopupIBPCurrentValue = dbgPopupIBPCurrentModel.getNumber ().intValue ();
            }
          }),
          Box.createHorizontalGlue ()
          ),
        createHorizontalBox (
          Box.createHorizontalStrut (7),
          Box.createHorizontalGlue (),
          setPreferredSize (Multilingual.mlnText (createLabel ("threshold"), "ja", "閾値"), 60, 16),
          createNumberSpinner (dbgPopupIBPThresholdModel = new SpinnerNumberModel (0, 0, 0x7fffffff, 1), 10, new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              dbgPopupIBPThresholdValue = dbgPopupIBPThresholdModel.getNumber ().intValue ();
            }
          }),
          Box.createHorizontalGlue ()
          ),
        dbgPopupIBPClearMenuItem =
        Multilingual.mlnText (createMenuItem ("Clear Breakpoint", 'C', popupActionListener), "ja", "ブレークポイントを消去")
        ) :
      null,
      dbgPopupHexMenu =
      createMenu (
        "XXXXXXXX", KeyEvent.VK_UNDEFINED,
        dbgPopupDisMenuItem = Multilingual.mlnText (createMenuItem ("Disassemble", 'D', popupActionListener), "ja", "逆アセンブル"),
        dbgPopupMemMenuItem = Multilingual.mlnText (createMenuItem ("Memory Dump", 'M', popupActionListener), "ja", "メモリダンプ")
        ),
      dbgPopupCopyMenuItem = Multilingual.mlnText (createMenuItem ("Copy", 'C', popupActionListener), "ja", "コピー"),
      dbgPopupSelectAllMenuItem = Multilingual.mlnText (createMenuItem ("Select All", 'A', popupActionListener), "ja", "すべて選択")
      );

  }  //dbgMake()

  //dbgShowPopup (me, textArea, dis)
  //  ポップアップメニューを表示する
  public static void dbgShowPopup (MouseEvent me, JTextArea textArea, boolean dis) {
    dbgEventMask++;
    int x = me.getX ();
    int y = me.getY ();
    int p = textArea.viewToModel (me.getPoint ());  //クリックされた位置
    DisassembleList.ddpPopupAddress = -1;  //クリックされた行のアドレス
    if (dis) {
      int i = Arrays.binarySearch (DisassembleList.ddpSplitArray, 1, DisassembleList.ddpItemCount, p + 1);
      i = (i >> 31 ^ i) - 1;  //クリックされた項目の番号
      DisassembleList.ddpPopupAddress = DisassembleList.ddpAddressArray[i];  //クリックされた項目の先頭アドレス
    }
    int start = textArea.getSelectionStart ();  //選択範囲の開始位置
    int end = textArea.getSelectionEnd ();  //選択範囲の終了位置
    String text = textArea.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    if ((start == end ||  //選択範囲がないか
         p < start || end <= p) &&  //選択範囲の外側がクリックされて
        0 <= p && p < length && isWord (text.charAt (p))) {  //クリックされた位置に単語があるとき
      //クリックされた位置にある単語を選択する
      for (start = p; 0 < start && isWord (text.charAt (start - 1)); start--) {
      }
      for (end = p + 1; end < length && isWord (text.charAt (end)); end++) {
      }
      textArea.select (start, end);
    }
    dbgHexSelected = false;
    if (start < end) {  //選択範囲があるとき
      textArea.requestFocusInWindow ();  //フォーカスがないと選択範囲が見えない
      //選択範囲にある16進数の文字を取り出す
      //  以下の条件を加える
      //    選択範囲に16進数以外の単語の文字がないこと
      //    選択範囲に16進数の文字が9文字以上ないこと
      //    16進数の文字が偶数文字ずつの塊になっていること
      dbgHexValue = 0;
      int n = 0;
      for (int i = start; i < end; i++) {
        int t;
        if ((t = Character.digit (text.charAt (i), 16)) >= 0) {  //16進数の文字
          dbgHexValue = dbgHexValue << 4 | t;
          if (n >= 8 ||  //選択範囲に16進数の文字が9文字以上ある
              i + 1 >= end || (t = Character.digit (text.charAt (i + 1), 16)) < 0) {  //16進数の文字が偶数文字ずつの塊になっていない
            n = 0;
            break;
          }
          dbgHexValue = dbgHexValue << 4 | t;
          n += 2;
          i++;
        } else if (isWord (text.charAt (i))) {  //16進数以外の単語の文字
          n = 0;
          break;
        }
      }
      dbgHexSelected = n > 0;
      try {
        Rectangle r = textArea.modelToView (start);
        Rectangle s = textArea.modelToView (end - 1);
        if (r.y == s.y) {  //選択範囲が1行だけのとき
          //選択範囲を隠してしまわないようにポップアップを選択範囲の下側に表示する
          y = r.y + r.height;
        }
      } catch (BadLocationException ble) {
      }
    }
    //逆アセンブルリストでコアが止まっていて選択範囲がなくてクリックされた行のアドレスがわかるとき命令ブレークポイントメニューが有効
    if (InstructionBreakPoint.IBP_ON) {
      if (dis && mpuTask == null && DisassembleList.ddpPopupAddress != -1) {
        setText (dbgPopupIBPMenu, fmtHex8 (DisassembleList.ddpPopupAddress));
        TreeMap<Integer,InstructionBreakPoint.InstructionBreakRecord> pointTable = DisassembleList.ddpSupervisorMode != 0 ? InstructionBreakPoint.ibpSuperPointTable : InstructionBreakPoint.ibpUserPointTable;
        InstructionBreakPoint.InstructionBreakRecord r = pointTable.get (DisassembleList.ddpPopupAddress);
        if (r != null) {  //命令ブレークポイントがあるとき
          dbgPopupIBPCurrentModel.setValue (new Integer (dbgPopupIBPCurrentValue = r.ibrValue));  //現在値
          dbgPopupIBPThresholdModel.setValue (new Integer (dbgPopupIBPThresholdValue = r.ibrThreshold));  //閾値
          dbgPopupIBPClearMenuItem.setEnabled (true);  //消去できる
        } else {  //命令ブレークポイントがないとき
          dbgPopupIBPCurrentModel.setValue (new Integer (dbgPopupIBPCurrentValue = 0));  //現在値
          dbgPopupIBPThresholdModel.setValue (new Integer (dbgPopupIBPThresholdValue = 0));  //閾値
          dbgPopupIBPClearMenuItem.setEnabled (false);  //消去できない
        }
        setVisible (dbgPopupIBPMenu, true);
      } else {
        setVisible (dbgPopupIBPMenu, false);
      }
    }
    //16進数が選択されていれば16進数メニューが有効
    if (dbgHexSelected) {
      setText (dbgPopupHexMenu, fmtHex8 (dbgHexValue));
      setVisible (dbgPopupHexMenu, true);
    } else {
      setVisible (dbgPopupHexMenu, false);
    }
    //選択範囲があればコピーが有効
    setEnabled (dbgPopupCopyMenuItem, clpClipboard != null && start < end);
    //クリップボードがあればすべて選択が有効
    setEnabled (dbgPopupSelectAllMenuItem, clpClipboard != null);
    //ポップアップメニューを表示する
    dbgPopupTextArea = textArea;
    dbgPopupMenu.show (textArea, x, y);
    dbgEventMask--;
  }  //dbgShowPopup(MouseEvent,JTextArea,boolean)

  public static boolean isWord (char c) {
    return '0' <= c && c <= '9' || 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || c == '_';
  }  //isWord(char)

  //dbgCopy ()
  //  コピー
  public static void dbgCopy () {
    if (clpClipboard != null) {
      //選択範囲の文字列をコピーする
      String selectedText = dbgPopupTextArea.getSelectedText ();
      if (selectedText != null) {
        clpClipboardString = selectedText;
        try {
          clpClipboard.setContents (clpStringContents, clpClipboardOwner);
          clpIsClipboardOwner = true;  //自分がコピーした
        } catch (Exception e) {
          return;
        }
      }
    }
  }  //dbgCopy()

  //dbgSelectAll ()
  //  すべて選択
  public static void dbgSelectAll () {
    if (clpClipboard != null) {
      //すべて選択する
      dbgEventMask++;
      dbgPopupTextArea.selectAll ();
      dbgPopupTextArea.requestFocusInWindow ();
      dbgEventMask--;
    }
  }  //dbgSelectAll()

  //dbgUpdate ()
  //  デバッグウインドウを更新する
  //  コアのrun()の末尾でdbgVisibleMask!=0のとき呼び出す
  public static void dbgUpdate () {
    if ((dbgVisibleMask & DBG_DRP_VISIBLE_MASK) != 0) {
      RegisterList.drpUpdate ();  //レジスタウインドウを更新する
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      if ((dbgVisibleMask & DBG_PFV_VISIBLE_MASK) != 0) {
        if (ProgramFlowVisualizer.pfvTimer == 0) {
          ProgramFlowVisualizer.pfvUpdate ();  //プログラムフロービジュアライザを更新する
        } else {
          ProgramFlowVisualizer.pfvTimer--;
        }
      }
    }
    if (RasterBreakPoint.RBP_ON) {
      if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
        if (RasterBreakPoint.rbpTimer == 0) {
          RasterBreakPoint.rbpUpdateFrame ();  //ラスタブレークポイントウインドウを更新する
        } else {
          RasterBreakPoint.rbpTimer--;
        }
      }
    }
    if (ScreenModeTest.SMT_ON) {
      if ((dbgVisibleMask & DBG_SMT_VISIBLE_MASK) != 0) {
        if (ScreenModeTest.smtTimer == 0) {
          ScreenModeTest.smtUpdateFrame ();  //表示モードテストウインドウを更新する
        } else {
          ScreenModeTest.smtTimer--;
        }
      }
    }
    if (RootPointerList.RTL_ON) {
      if ((dbgVisibleMask & DBG_RTL_VISIBLE_MASK) != 0) {
        if (RootPointerList.rtlTimer == 0) {
          RootPointerList.rtlTimer = RootPointerList.RTL_INTERVAL - 1;
          RootPointerList.rtlUpdateFrame ();  //ルートポインタリストを更新する
        } else {
          RootPointerList.rtlTimer--;
        }
      }
    }
    if (SpritePatternViewer.SPV_ON) {
      if ((dbgVisibleMask & DBG_SPV_VISIBLE_MASK) != 0) {
        if (SpritePatternViewer.spvTimer == 0) {
          SpritePatternViewer.spvTimer = SpritePatternViewer.SPV_INTERVAL - 1;
          SpritePatternViewer.spvUpdateFrame ();  //スプライトパターンビュアを更新する
        } else {
          SpritePatternViewer.spvTimer--;
        }
      }
    }
    if (ATCMonitor.ACM_ON) {
      if ((dbgVisibleMask & DBG_ACM_VISIBLE_MASK) != 0) {
        if (ATCMonitor.acmTimer == 0) {
          ATCMonitor.acmTimer = ATCMonitor.ACM_INTERVAL - 1;
          ATCMonitor.acmUpdateFrame ();  //アドレス変換キャッシュモニタを更新する
        } else {
          ATCMonitor.acmTimer--;
        }
      }
    }
  }  //dbgUpdate()

  //dbgDoStopOnError ()
  //  エラーで停止する
  //  エラーを検出して例外スタックフレームを構築した後にdbgStopOnErrorならば呼び出してコアを停止させる
  //
  //  Human68kの_BUS_ERRの中では停止させない
  //  human302のシステムディスクで起動した場合、
  //  SCSIボード、RS-232Cボード、MIDIボードのテストで_BUS_ERRが呼び出されてバスエラーが発生する
  //    bus error on reading from 00EA0044 at 0000E2F4
  //    bus error on reading from 00EAFC04 at 0002D04A
  //    bus error on reading from 00EAFC14 at 0002D04A
  //    bus error on reading from 00EAFA01 at 0005CD54
  //  _BUS_ERRはレベル0で入ったDOSコールの番号を更新しないので、_BUS_ERRの中かどうかはpcで判断する
  //    0x0000e342 <= pc0 && pc0 < 0x0000e3b6  human200/human201の_BUS_ERR
  //    0x0000e3c8 <= pc0 && pc0 < 0x0000e43c  human202の_BUS_ERR
  //    0x0000e1a8 <= pc0 && pc0 < 0x0000e21c  human203の_BUS_ERR
  //    0x0000e256 <= pc0 && pc0 < 0x0000e2ca  human215の_BUS_ERR
  //    0x0000e174 <= pc0 && pc0 < 0x0000e1e8  human301の_BUS_ERR
  //    0x0000e28a <= pc0 && pc0 < 0x0000e2fe  human302の_BUS_ERR
  public static boolean dbgDoStopOnError () {
    String message = (
      M68kException.m6eNumber <= M68kException.M6E_ADDRESS_ERROR ?
      fmtHex8 (fmtHex8 (new StringBuilder ("ERROR: ").append (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
                  .append (M68kException.m6eDirection == 0 ? " on writing to " : " on reading from "), M68kException.m6eAddress)
            .append (" at "), regPC0).toString () :
      fmtHex8 (new StringBuilder (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
            .append (" at "), regPC0).toString ()
      );
    prgMessage (message);
    if (!(M68kException.m6eNumber == M68kException.M6E_ACCESS_FAULT &&
          0x0000e100 <= regPC0 && regPC0 < 0x0000e500)) {  //_BUS_ERRの中で発生したバスエラーでないとき
      mpuStop (message);
      return true;
    }
    return false;
  }  //dbgDoStopOnError()

  //dbgDoubleBusFault ()
  //  ダブルバスフォルト
  public static void dbgDoubleBusFault () {
    String message =
      fmtHex8 (fmtHex8 (new StringBuilder ("FATAL ERROR: ").append (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
                        .append (M68kException.m6eDirection == 0 ? " on writing to " : " on reading from "), M68kException.m6eAddress)
               .append (" at "), regPC0).toString ();
    prgMessage (message);
    mpuStop (message);
  }  //dbgDoubleBusFault()



  //========================================================================================
  //$$TXF テキストファイル

  //success = txfSaveText (name, text)
  //  ファイルにテキストを書き出す
  public static boolean txfSaveText (String name, String text) {
    try {
      File txt = new File (name);  //テキストファイル
      File bak = new File (name + ".bak");  //バックアップファイル
      File tmp = new File (name + ".tmp");  //テンポラリファイル
      BufferedWriter out = new BufferedWriter (new FileWriter (tmp));  //テンポラリファイルに
      out.write (text);  //書き出す
      out.close ();
      if (txt.exists ()) {  //テキストファイルがあるとき
        if (bak.exists ()) {  //バックアップファイルがあれば
          bak.delete ();  //バックアップファイルを削除してから
        }
        txt.renameTo (bak);  //テキストファイルをバックアップファイルにリネームして
      }
      tmp.renameTo (txt);  //テンポラリファイルをテキストファイルにリネームする
    } catch (IOException ioe) {
      return false;
    }
    return true;
  }  //txfSaveText(String,String)



  //========================================================================================
  //$$ISM InputStream

  public static final Pattern ISM_ZIP_SEPARATOR = Pattern.compile ("(?<=\\.(?:jar|zip))(?:/|\\\\)(?=.)", Pattern.CASE_INSENSITIVE);

  public static final HashMap<String,byte[]> ismResourceCache = new HashMap<String,byte[]> ();

  //bb = ismGetResource (name)
  //  リソースファイルを読み込む
  //  一度読み込んだリソースファイルはキャッシュされる
  //  キャッシュの配列を書き換えないように注意すること
  public static byte[] ismGetResource (String name) {
    byte[] bb = ismResourceCache.get (name);
    if (bb == null) {  //まだ読み込んでいない
      InputStream in = ismOpen (name, true);  //リソースファイルを開く
      if (in == null) {  //リソースファイルが見つからない
        return null;
      }
      byte[] tt = new byte[1024 * 256];  //最大256KB
      int length = ismRead (in, tt, 0, tt.length);
      ismClose (in);
      bb = new byte[length];
      System.arraycopy (tt, 0, bb, 0, length);
      ismResourceCache.put (name, bb);
    }
    return bb;
  }

  //in = ismOpen (name)
  //  InputStreamを開く
  //  InputStreamを返す
  //    失敗したときはnullを返す
  //  ZIPファイルの中のファイルを指定できる
  //    ZIPファイルの中のファイルはZipInputStreamで開く
  //    ZIPファイルの中のファイル名は{ZIPファイル名}/{ZIPファイルの中のファイル名}で指定する
  //    JARファイルもZIPファイルとして開くことができる
  //  GZIPで圧縮されているファイルを指定できる
  //    GZIPで圧縮されているファイルはGZIPInputStreamで開く
  public static InputStream ismOpen (String name) {
    InputStream in = null;
    if (prgIsLocal) {  //ローカルのとき
      in = ismOpen (name, false);  //ファイルを開く
    }
    if (in == null && name.indexOf ('/') < 0 && name.indexOf ('\\') < 0) {  //コマンドラインでファイルがないか、アプレットまたはJNLPのとき
      in = ismOpen (name, true);  //リソースを開く
    }
    return in;
  }  //ismOpen(String)
  public static InputStream ismOpen (String name, boolean useGetResource) {
    boolean gzipped = name.toUpperCase ().endsWith (".GZ");  //true=GZIPファイルが指定された
    String[] zipSplittedName = ISM_ZIP_SEPARATOR.split (name, 2);  //ZIPファイル名とZIPファイルの中のファイル名に分ける
    String fileName = zipSplittedName[0];  //通常のファイル名またはZIPファイル名
    String zipEntryName = zipSplittedName.length < 2 ? null : zipSplittedName[1];  //ZIPファイルの中のファイル名
    InputStream in = null;
    try {
      if (useGetResource) {  //getResourceを使うとき
        if (false) {
          URL url = XEiJ.class.getResource (fileName);
          if (url != null) {  //ファイルがある
            in = url.openStream ();
          }
        } else {
          in = XEiJ.class.getResourceAsStream (fileName);
        }
      } else {
        File file = new File (fileName);
        if (file.exists ()) {  //ファイルがある
          in = new FileInputStream (file);
        }
      }
      if (in != null && zipEntryName != null) {  //ZIPファイルの中のファイルが指定されたとき
        ZipInputStream zin = new ZipInputStream (in);
        in = null;
        ZipEntry entry;
        while ((entry = zin.getNextEntry ()) != null) {  //指定されたファイル名のエントリを探す
          if (zipEntryName.equals (entry.getName ())) {  //エントリが見つかった
            in = zin;
            break;
          }
        }
        if (in == null) {
          prgMessage (Multilingual.mlnJapanese ? fileName + " の中に " + zipEntryName + " がありません" :
                      zipEntryName + " does not exist in " + fileName);
        }
      }
      if (in != null && gzipped) {  //GZIPで圧縮されたファイルが指定されたとき
        in = new GZIPInputStream (in);
      }
      if (in != null) {
        prgMessage (Multilingual.mlnJapanese ? (useGetResource ? "リソースファイル " : "ファイル ") + name + " を読み込みます" :
                    (useGetResource ? "Reading resource file " : "Reading file ") + name);
        return new BufferedInputStream (in);
      }
    } catch (Exception ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    prgMessage (Multilingual.mlnJapanese ? (useGetResource ? "リソースファイル " : "ファイル ") + name + " が見つかりません" :
                (useGetResource ? "Resource file " : "File ") + name + " is not found");
    return null;  //失敗
  }  //ismOpen(String,boolean)

  //k = ismRead (in, bb, o, l)
  //  InputStreamからバイトバッファに読み込む
  //  読み込んだ長さを返す
  //    エラーのときは-1を返す
  //  指定されたサイズまたはファイルの末尾まで読み込む
  //    k=in.read(bb,o,l)は1回で指定されたサイズを読み込めるとは限らない
  //  ブロックされる可能性があるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static int ismRead (InputStream in, byte[] bb, int o, int l) {
    try {
      int k = 0;
      while (k < l) {
        int t = in.read (bb, o + k, l - k);
        if (t < 0) {
          break;
        }
        k += t;
      }
      return k;
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    return -1;
  }  //ismRead(InputStream,byte[],int,int)

  //k = ismSkip (in, l)
  //  InputStreamを読み飛ばす
  //  読み飛ばした長さを返す
  //    エラーのときは-1を返す
  //  指定されたサイズまたはファイルの末尾まで読み飛ばす
  //    k=in.skip(l)は1回で指定されたサイズを読み飛ばせるとは限らない
  //  ブロックされる可能性があるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static int ismSkip (InputStream in, int l) {
    try {
      int k = 0;
      while (k < l) {
        //skip(long)はファイルの末尾でなくても0を返す可能性があるのでファイルの末尾の判定はread()で行う
        //skip(long)する前に毎回read()しないとskip()がファイルの末尾で止まらなくなるらしい
        if (in.read () < 0) {
          break;
        }
        k++;
        if (k < l) {
          int t = (int) in.skip ((long) (l - k));
          if (t < 0) {
            break;
          }
          k += t;
        }
      }
      return k;
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    return -1;
  }  //ismSkip(InputStream,int)

  //ismClose (in)
  //  InputStreamを閉じる
  //  in==nullのときは何もしない
  //  in.close()でIOExceptionを無視するだけ
  public static void ismClose (InputStream in) {
    try {
      if (in != null) {
        in.close ();
      }
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
  }  //ismClose(InputStream)

  //text = ismGetResourceText (name, charset)
  //  リソースファイルからテキストを読み出す
  public static String ismGetResourceText (String name, String charset) {
    StringBuilder sb = new StringBuilder ();
    try {
      InputStream in = ismOpen (name, true);
      if (in != null) {
        BufferedReader reader = new BufferedReader (new InputStreamReader (in, charset));
        for (String line = reader.readLine (); line != null; line = reader.readLine ()) {
          sb.append (line).append ('\n');
        }
        reader.close ();
      }
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
      if (Multilingual.mlnJapanese) {
        sb.append (name).append (" を読み込めません\n");
      } else {
        sb.append ("Cannot read ").append (name).append ('\n');
      }
    }
    return sb.toString ();
  }  //ismGetResourceText(String,String)

  //length = ismLength (name, maxLength)
  //  ファイルの長さを数える
  //  ZIPファイルの中のファイルを指定できる
  //  GZIPで圧縮されているファイルを指定できる
  //  -1  ファイルがない
  public static int ismLength (String name, int maxLength) {
    int length;
    InputStream in = ismOpen (name);
    if (in == null) {  //ファイルがない
      length = -1;
    } else {  //ファイルがある
      length = ismSkip (in, maxLength);
      ismClose (in);
    }
    return length;
  }  //ismLength(String,int)

  //success = ismLoad (bb, o, l, names)
  //  ファイルからバイトバッファに読み込む
  //  ファイル名を,で区切って複数指定できる
  //    先頭から順に指定されたサイズまで読み込めるファイルを探す
  //    1つでも読み込むことができればその時点で成功、1つも読み込めなければ失敗
  //  成功したときtrueを返す
  //  ZIPファイルの中のファイルを指定できる
  //    ZIPファイルの中のファイルはZipInputStreamで開く
  //    ZIPファイルの中のファイル名は{ZIPファイル名}/{ZIPファイルの中のファイル名}で指定する
  //    JARファイルもZIPファイルとして開くことができる
  //  GZIPで圧縮されているファイルを指定できる
  //    GZIPで圧縮されているファイルはGZIPInputStreamで開く
  //  ブロックされることがあるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static boolean ismLoad (byte[] bb, int o, int l, String names) {
    for (String name : names.split (",")) {  //先頭から順に
      if (name.length () != 0) {  //ファイル名が指定されているとき
        InputStream in = ismOpen (name);  //開く
        if (in != null) {  //開けたら
          int k = ismRead (in, bb, o, l);  //読み込んで
          ismClose (in);  //閉じる
          if (k == l) {  //指定されたサイズまで読み込めたら
            return true;  //成功
          }
        }
      }
    }
    return false;  //1つも読み込めなかったので失敗
  }  //ismLoad(byte[],int,int,String)

  //success = ismSave (bb, offset, length, path, verbose)
  //  バイトバッファからファイルに書き出す
  //  出力範囲がバッファに収まっているとき
  //    ファイルが既に存在していてファイルサイズと内容が一致しているときは更新しない
  //  出力範囲がバッファに収まっていないとき
  //    バッファからはみ出した部分をゼロクリアする
  //    RandomAccessFileのsetLengthを使うとファイルサイズを簡単に変更できるが、ファイルを伸ばしたときに書き込まれる内容が仕様で定義されていない
  //    同じ手順で同じ内容のファイルができない可能性があるのは困るので明示的に0を書き込んでクリアする
  public static boolean ismSave (byte[] bb, int offset, long length, String path, boolean verbose) {
    if (!prgIsLocal ||  //ローカルでないとき
        ISM_ZIP_SEPARATOR.split (path, 2).length != 1) {  //ZIPファイルの中のファイル
      if (verbose) {
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? path + " に書き出せません" : "Cannot write " + path);
      }
      return false;
    }
    long step = 0;  //一度にゼロクリアする長さ。0=バッファに収まっている
    byte[] zz = null;  //ゼロクリア用の配列
    long pointer = (long) (bb.length - offset);  //バッファから出力できる長さ
    if (pointer < length) {  //バッファに収まっていない
      step = Math.min (1024L * 512, length - pointer);  //一度にゼロクリアする長さ。最大512KB
      zz = new byte[(int) step];  //ゼロクリア用の配列
      Arrays.fill (zz, (byte) 0);
    }
    //ファイル
    File file = new File (path);  //ファイル
    //ファイルが既に存在しているときはファイルサイズと内容が一致しているかどうか確認する
    if (step == 0 &&  //バッファに収まっている
        file.exists () && file.length () == length) {  //ファイルサイズが一致している
      //ファイルを読み込む
      if (length == 0L) {  //ファイルサイズが0で一致しているので成功
        return true;
      }
      InputStream in = ismOpen (path);
      if (in != null) {
        int l = (int) length;  //バッファに収まっているのだからintの範囲内
        byte[] tt = new byte[l];
        int k = ismRead (in, tt, 0, l);
        ismClose (in);
        if (k == l &&
            Arrays.equals (tt, bb.length == l ? bb : Arrays.copyOfRange (bb, offset, offset + l))) {  //内容が一致している
          return true;  //更新する必要がないので成功
        }
      }
    }  //check
    //*.tmpと*.bak
    String pathTmp = path + ".tmp";  //*.tmp
    String pathBak = path + ".bak";  //*.bak
    File fileTmp = new File (pathTmp);  //*.tmp
    File fileBak = new File (pathBak);  //*.bak
    //*.tmpを削除する
    if (fileTmp.exists ()) {  //*.tmpがあるとき
      if (!fileTmp.delete ()) {  //*.tmpを削除する
        if (verbose) {
          JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathTmp + " を削除できません" : "Cannot delete " + pathTmp);
        }
        return false;
      }
    }
    //*.tmpに書き出す
    try (OutputStream out = path.toLowerCase ().endsWith (".gz") ?  //pathの末尾が".gz"のときpathTmpの末尾は".gz.tmp"であることに注意
         new GZIPOutputStream (new BufferedOutputStream (new FileOutputStream (fileTmp))) {
           {
             //def.setLevel (Deflater.BEST_COMPRESSION);
             def.setLevel (Deflater.DEFAULT_COMPRESSION);
             //def.setLevel (Deflater.BEST_SPEED);
           }
         } :
         new BufferedOutputStream (new FileOutputStream (fileTmp))) {  //try-with-resourcesは1.7から
      if (step == 0) {  //バッファに収まっている
        out.write (bb, offset, (int) length);  //OutputStreamのwriteの返り値はvoid。エラーが出なければ1回で最後まで書き出される
      } else {  //バッファに収まっていない
        out.write (bb, offset, (int) pointer);  //バッファから出力できる範囲
        for (; pointer < length; pointer += step) {
          out.write (zz, 0, (int) Math.min (step, length - pointer));  //バッファから出力できない範囲
        }
      }
    } catch (IOException ioe) {
      if (verbose) {
        prgPrintStackTraceOf (ioe);
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathTmp + " に書き出せません" : "Cannot write " + pathTmp);
      }
      return false;
    }
    //ファイルを*.bakにリネームする
    //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
    if (file.exists ()) {  //ファイルがあるとき
      if (fileBak.exists ()) {  //*.bakがあるとき
        if (!fileBak.delete ()) {  //*.bakを削除する
          if (verbose) {
            JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathBak + " を削除できません" : "Cannot delete " + pathBak);
          }
          return false;
        }
      }
      if (!file.renameTo (fileBak)) {  //ファイルを*.bakにリネームする
        if (verbose) {
          JOptionPane.showMessageDialog (
            null, Multilingual.mlnJapanese ? path + " を " + pathBak + " にリネームできません" : "Cannot rename " + path + " to " + pathBak);
        }
        return false;
      }
    }
    //*.tmpをファイルにリネームする
    //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
    if (!fileTmp.renameTo (file)) {  //*.tmpをファイルにリネームする
      if (verbose) {
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? pathTmp + " を " + path + " にリネームできません" : "Cannot rename " + pathTmp + " to " + path);
      }
      return false;
    }
    return true;
  }  //ismSave(byte[],int,long,String,boolean)



  //========================================================================================
  //$$FMT フォーマット変換
  //  Formatterは遅いので自前で展開する

  public static final char[] FMT_TEMP = new char[32];

  //--------------------------------------------------------------------------------
  //2進数変換
  //  ainNは'.'と'*'、binNは'0'と'1'に変換する
  //
  //  x          00 01
  //  x<<2       00 04
  //  x<<2&4     00 04
  //  x<<2&4^46  2e 2a
  //              .  *

  public static final char[] FMT_AIN4_BASE = ".......*..*...**.*...*.*.**..****...*..**.*.*.****..**.****.****".toCharArray ();
  public static final char[] FMT_BIN4_BASE = "0000000100100011010001010110011110001001101010111100110111101111".toCharArray ();

  //fmtAin4 (a, o, x)
  //fmtBin4 (a, o, x)
  //s = fmtAin4 (x)
  //s = fmtBin4 (x)
  //sb = fmtAin4 (sb, x)
  //sb = fmtBin4 (sb, x)
  //  4桁2進数変換
  public static void fmtAin4 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  1 & 4 ^ 46);
    a[o +  1] = (char) (x       & 4 ^ 46);
    a[o +  2] = (char) (x <<  1 & 4 ^ 46);
    a[o +  3] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin4(char[],int,int)
  public static void fmtBin4 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  3 & 1 | 48);
    a[o +  1] = (char) (x >>>  2 & 1 | 48);
    a[o +  2] = (char) (x >>>  1 & 1 | 48);
    a[o +  3] = (char) (x        & 1 | 48);
  }  //fmtBin4(char[],int,int)
  public static String fmtAin4 (int x) {
    return String.valueOf (FMT_AIN4_BASE, (x & 15) << 2, 4);
  }  //fmtAin4(int)
  public static String fmtBin4 (int x) {
    return String.valueOf (FMT_BIN4_BASE, (x & 15) << 2, 4);
  }  //fmtBin4(int)
  public static StringBuilder fmtAin4 (StringBuilder sb, int x) {
    return sb.append (FMT_AIN4_BASE, (x & 15) << 2, 6);
  }  //fmtAin4(StringBuilder,int)
  public static StringBuilder fmtBin4 (StringBuilder sb, int x) {
    return sb.append (FMT_BIN4_BASE, (x & 15) << 2, 6);
  }  //fmtBin4(StringBuilder,int)

  //fmtAin6 (a, o, x)
  //fmtBin6 (a, o, x)
  //s = fmtAin6 (x)
  //s = fmtBin6 (x)
  //sb = fmtAin6 (sb, x)
  //sb = fmtBin6 (sb, x)
  //  6桁2進数変換
  public static void fmtAin6 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  3 & 4 ^ 46);
    a[o +  1] = (char) (x >>  2 & 4 ^ 46);
    a[o +  2] = (char) (x >>  1 & 4 ^ 46);
    a[o +  3] = (char) (x       & 4 ^ 46);
    a[o +  4] = (char) (x <<  1 & 4 ^ 46);
    a[o +  5] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin6(char[],int,int)
  public static void fmtBin6 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  5 & 1 | 48);
    a[o +  1] = (char) (x >>>  4 & 1 | 48);
    a[o +  2] = (char) (x >>>  3 & 1 | 48);
    a[o +  3] = (char) (x >>>  2 & 1 | 48);
    a[o +  4] = (char) (x >>>  1 & 1 | 48);
    a[o +  5] = (char) (x        & 1 | 48);
  }  //fmtBin6(char[],int,int)
  public static String fmtAin6 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtAin6(int)
  public static String fmtBin6 (int x) {
    FMT_TEMP[ 0] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtBin6(int)
  public static StringBuilder fmtAin6 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtAin6(StringBuilder,int)
  public static StringBuilder fmtBin6 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtBin6(StringBuilder,int)

  //fmtAin8 (a, o, x)
  //fmtBin8 (a, o, x)
  //s = fmtAin8 (x)
  //s = fmtBin8 (x)
  //sb = fmtAin8 (sb, x)
  //sb = fmtBin8 (sb, x)
  //  8桁2進数変換
  public static void fmtAin8 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  5 & 4 ^ 46);
    a[o +  1] = (char) (x >>  4 & 4 ^ 46);
    a[o +  2] = (char) (x >>  3 & 4 ^ 46);
    a[o +  3] = (char) (x >>  2 & 4 ^ 46);
    a[o +  4] = (char) (x >>  1 & 4 ^ 46);
    a[o +  5] = (char) (x       & 4 ^ 46);
    a[o +  6] = (char) (x <<  1 & 4 ^ 46);
    a[o +  7] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin8(char[],int,int)
  public static void fmtBin8 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  7 & 1 | 48);
    a[o +  1] = (char) (x >>>  6 & 1 | 48);
    a[o +  2] = (char) (x >>>  5 & 1 | 48);
    a[o +  3] = (char) (x >>>  4 & 1 | 48);
    a[o +  4] = (char) (x >>>  3 & 1 | 48);
    a[o +  5] = (char) (x >>>  2 & 1 | 48);
    a[o +  6] = (char) (x >>>  1 & 1 | 48);
    a[o +  7] = (char) (x        & 1 | 48);
  }  //fmtBin8(char[],int,int)
  public static String fmtAin8 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtAin8(int)
  public static String fmtBin8 (int x) {
    FMT_TEMP[ 0] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtBin8(int)
  public static StringBuilder fmtAin8 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtAin8(StringBuilder,int)
  public static StringBuilder fmtBin8 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtBin8(StringBuilder,int)

  //fmtAin12 (a, o, x)
  //fmtBin12 (a, o, x)
  //s = fmtAin12 (x)
  //s = fmtBin12 (x)
  //sb = fmtAin12 (sb, x)
  //sb = fmtBin12 (sb, x)
  //  12桁2進数変換
  public static void fmtAin12 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  9 & 4 ^ 46);
    a[o +  1] = (char) (x >>  8 & 4 ^ 46);
    a[o +  2] = (char) (x >>  7 & 4 ^ 46);
    a[o +  3] = (char) (x >>  6 & 4 ^ 46);
    a[o +  4] = (char) (x >>  5 & 4 ^ 46);
    a[o +  5] = (char) (x >>  4 & 4 ^ 46);
    a[o +  6] = (char) (x >>  3 & 4 ^ 46);
    a[o +  7] = (char) (x >>  2 & 4 ^ 46);
    a[o +  8] = (char) (x >>  1 & 4 ^ 46);
    a[o +  9] = (char) (x       & 4 ^ 46);
    a[o + 10] = (char) (x <<  1 & 4 ^ 46);
    a[o + 11] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin12(char[],int,int)
  public static void fmtBin12 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 11 & 1 | 48);
    a[o +  1] = (char) (x >>> 10 & 1 | 48);
    a[o +  2] = (char) (x >>>  9 & 1 | 48);
    a[o +  3] = (char) (x >>>  8 & 1 | 48);
    a[o +  4] = (char) (x >>>  7 & 1 | 48);
    a[o +  5] = (char) (x >>>  6 & 1 | 48);
    a[o +  6] = (char) (x >>>  5 & 1 | 48);
    a[o +  7] = (char) (x >>>  4 & 1 | 48);
    a[o +  8] = (char) (x >>>  3 & 1 | 48);
    a[o +  9] = (char) (x >>>  2 & 1 | 48);
    a[o + 10] = (char) (x >>>  1 & 1 | 48);
    a[o + 11] = (char) (x        & 1 | 48);
  }  //fmtBin12(char[],int,int)
  public static String fmtAin12 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x       & 4 ^ 46);
    FMT_TEMP[10] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 12);
  }  //fmtAin12(int)
  public static String fmtBin12 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[11] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 12);
  }  //fmtBin12(int)
  public static StringBuilder fmtAin12 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x       & 4 ^ 46);
    FMT_TEMP[10] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 12);
  }  //fmtAin12(StringBuilder,int)
  public static StringBuilder fmtBin12 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[11] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 12);
  }  //fmtBin12(StringBuilder,int)

  //fmtAin16 (a, o, x)
  //fmtBin16 (a, o, x)
  //s = fmtAin16 (x)
  //s = fmtBin16 (x)
  //sb = fmtAin16 (sb, x)
  //sb = fmtBin16 (sb, x)
  //  16桁2進数変換
  public static void fmtAin16 (char[] a, int o, int x) {
    a[o     ] = (char) (x >> 13 & 4 ^ 46);
    a[o +  1] = (char) (x >> 12 & 4 ^ 46);
    a[o +  2] = (char) (x >> 11 & 4 ^ 46);
    a[o +  3] = (char) (x >> 10 & 4 ^ 46);
    a[o +  4] = (char) (x >>  9 & 4 ^ 46);
    a[o +  5] = (char) (x >>  8 & 4 ^ 46);
    a[o +  6] = (char) (x >>  7 & 4 ^ 46);
    a[o +  7] = (char) (x >>  6 & 4 ^ 46);
    a[o +  8] = (char) (x >>  5 & 4 ^ 46);
    a[o +  9] = (char) (x >>  4 & 4 ^ 46);
    a[o + 10] = (char) (x >>  3 & 4 ^ 46);
    a[o + 11] = (char) (x >>  2 & 4 ^ 46);
    a[o + 12] = (char) (x >>  1 & 4 ^ 46);
    a[o + 13] = (char) (x       & 4 ^ 46);
    a[o + 14] = (char) (x <<  1 & 4 ^ 46);
    a[o + 15] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin16(char[],int,int)
  public static void fmtBin16 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 15 & 1 | 48);
    a[o +  1] = (char) (x >>> 14 & 1 | 48);
    a[o +  2] = (char) (x >>> 13 & 1 | 48);
    a[o +  3] = (char) (x >>> 12 & 1 | 48);
    a[o +  4] = (char) (x >>> 11 & 1 | 48);
    a[o +  5] = (char) (x >>> 10 & 1 | 48);
    a[o +  6] = (char) (x >>>  9 & 1 | 48);
    a[o +  7] = (char) (x >>>  8 & 1 | 48);
    a[o +  8] = (char) (x >>>  7 & 1 | 48);
    a[o +  9] = (char) (x >>>  6 & 1 | 48);
    a[o + 10] = (char) (x >>>  5 & 1 | 48);
    a[o + 11] = (char) (x >>>  4 & 1 | 48);
    a[o + 12] = (char) (x >>>  3 & 1 | 48);
    a[o + 13] = (char) (x >>>  2 & 1 | 48);
    a[o + 14] = (char) (x >>>  1 & 1 | 48);
    a[o + 15] = (char) (x        & 1 | 48);
  }  //fmtBin16(char[],int,int)
  public static String fmtAin16 (int x) {
    FMT_TEMP[ 0] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x       & 4 ^ 46);
    FMT_TEMP[14] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 16);
  }  //fmtAin16(int)
  public static String fmtBin16 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[15] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 16);
  }  //fmtBin16(int)
  public static StringBuilder fmtAin16 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x       & 4 ^ 46);
    FMT_TEMP[14] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtAin16(StringBuilder,int)
  public static StringBuilder fmtBin16 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[15] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtBin16(StringBuilder,int)

  //fmtAin24 (a, o, x)
  //fmtBin24 (a, o, x)
  //s = fmtAin24 (x)
  //s = fmtBin24 (x)
  //sb = fmtAin24 (sb, x)
  //sb = fmtBin24 (sb, x)
  //  24桁2進数変換
  public static void fmtAin24 (char[] a, int o, int x) {
    a[o     ] = (char) (x >> 21 & 4 ^ 46);
    a[o +  1] = (char) (x >> 20 & 4 ^ 46);
    a[o +  2] = (char) (x >> 19 & 4 ^ 46);
    a[o +  3] = (char) (x >> 18 & 4 ^ 46);
    a[o +  4] = (char) (x >> 17 & 4 ^ 46);
    a[o +  5] = (char) (x >> 16 & 4 ^ 46);
    a[o +  6] = (char) (x >> 15 & 4 ^ 46);
    a[o +  7] = (char) (x >> 14 & 4 ^ 46);
    a[o +  8] = (char) (x >> 13 & 4 ^ 46);
    a[o +  9] = (char) (x >> 12 & 4 ^ 46);
    a[o + 10] = (char) (x >> 11 & 4 ^ 46);
    a[o + 11] = (char) (x >> 10 & 4 ^ 46);
    a[o + 12] = (char) (x >>  9 & 4 ^ 46);
    a[o + 13] = (char) (x >>  8 & 4 ^ 46);
    a[o + 14] = (char) (x >>  7 & 4 ^ 46);
    a[o + 15] = (char) (x >>  6 & 4 ^ 46);
    a[o + 16] = (char) (x >>  5 & 4 ^ 46);
    a[o + 17] = (char) (x >>  4 & 4 ^ 46);
    a[o + 18] = (char) (x >>  3 & 4 ^ 46);
    a[o + 19] = (char) (x >>  2 & 4 ^ 46);
    a[o + 20] = (char) (x >>  1 & 4 ^ 46);
    a[o + 21] = (char) (x       & 4 ^ 46);
    a[o + 22] = (char) (x <<  1 & 4 ^ 46);
    a[o + 23] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin24(char[],int,int)
  public static void fmtBin24 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 23 & 1 | 48);
    a[o +  1] = (char) (x >>> 22 & 1 | 48);
    a[o +  2] = (char) (x >>> 21 & 1 | 48);
    a[o +  3] = (char) (x >>> 20 & 1 | 48);
    a[o +  4] = (char) (x >>> 19 & 1 | 48);
    a[o +  5] = (char) (x >>> 18 & 1 | 48);
    a[o +  6] = (char) (x >>> 17 & 1 | 48);
    a[o +  7] = (char) (x >>> 16 & 1 | 48);
    a[o +  8] = (char) (x >>> 15 & 1 | 48);
    a[o +  9] = (char) (x >>> 14 & 1 | 48);
    a[o + 10] = (char) (x >>> 13 & 1 | 48);
    a[o + 11] = (char) (x >>> 12 & 1 | 48);
    a[o + 12] = (char) (x >>> 11 & 1 | 48);
    a[o + 13] = (char) (x >>> 10 & 1 | 48);
    a[o + 14] = (char) (x >>>  9 & 1 | 48);
    a[o + 15] = (char) (x >>>  8 & 1 | 48);
    a[o + 16] = (char) (x >>>  7 & 1 | 48);
    a[o + 17] = (char) (x >>>  6 & 1 | 48);
    a[o + 18] = (char) (x >>>  5 & 1 | 48);
    a[o + 19] = (char) (x >>>  4 & 1 | 48);
    a[o + 20] = (char) (x >>>  3 & 1 | 48);
    a[o + 21] = (char) (x >>>  2 & 1 | 48);
    a[o + 22] = (char) (x >>>  1 & 1 | 48);
    a[o + 23] = (char) (x        & 1 | 48);
  }  //fmtBin24(char[],int,int)
  public static String fmtAin24 (int x) {
    FMT_TEMP[ 0] = (char) (x >> 21 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 20 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 19 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 18 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >> 17 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >> 16 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >> 15 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >> 14 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[14] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[16] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[17] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[18] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[19] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[20] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[21] = (char) (x       & 4 ^ 46);
    FMT_TEMP[22] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[23] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 24);
  }  //fmtAin24(int)
  public static String fmtBin24 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 23 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 22 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 21 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 20 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 19 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 18 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>> 17 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>> 16 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[15] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[16] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[17] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[18] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[19] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[20] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[21] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[22] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[23] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 24);
  }  //fmtBin24(int)
  public static StringBuilder fmtAin24 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >> 21 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 20 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 19 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 18 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >> 17 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >> 16 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >> 15 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >> 14 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[14] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[16] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[17] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[18] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[19] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[20] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[21] = (char) (x       & 4 ^ 46);
    FMT_TEMP[22] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[23] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 24);
  }  //fmtAin24(StringBuilder,int)
  public static StringBuilder fmtBin24 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 23 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 22 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 21 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 20 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 19 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 18 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>> 17 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>> 16 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[15] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[16] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[17] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[18] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[19] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[20] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[21] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[22] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[23] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 24);
  }  //fmtBin24(StringBuilder,int)

  //--------------------------------------------------------------------------------
  //16進数変換
  //
  //     x             00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
  //   9-x             09 08 07 06 05 04 03 02 01 00 ff fe fd fc fb fa
  //   9-x>>4          00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff
  //   9-x>>4&7        00 00 00 00 00 00 00 00 00 00 07 07 07 07 07 07
  //   9-x>>4&7|48     30 30 30 30 30 30 30 30 30 30 37 37 37 37 37 37
  //  (9-x>>4&7|48)+x  30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46
  //                    0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
  //
  //     x              00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
  //   9-x              09 08 07 06 05 04 03 02 01 00 ff fe fd fc fb fa
  //   9-x>>4           00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff
  //   9-x>>4&39        00 00 00 00 00 00 00 00 00 00 27 27 27 27 27 27
  //  (9-x>>4&39)+48    30 30 30 30 30 30 30 30 30 30 57 57 57 57 57 57
  //  (9-x>>4&39)+48+x  30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66
  //                     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
  //
  //             c            30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 61 62 63 64 65 66
  //          64-c            10 0f 0e 0d 0c 0b 0a 09 08 07 ff fe fd fc fb fa df de dd dc db da
  //          64-c>>8         00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff ff ff ff ff ff ff
  //          64-c>>8&39      00 00 00 00 00 00 00 00 00 00 27 27 27 27 27 27 27 27 27 27 27 27
  //          64-c>>8&39|48   30 30 30 30 30 30 30 30 30 30 57 57 57 57 57 57 57 57 57 57 57 57
  //   c|32                   30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 61 62 63 64 65 66
  //  (c|32)-(64-c>>8&39|48)  00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 0a 0b 0c 0d 0e 0f

  //c = fmtHexc (x)
  //a = fmtHex1 (a, o, x)
  //s = fmtHex1 (x)
  //sb = fmtHex1 (sb, x)
  //  1桁16進数変換
  public static char fmtHexc (int x) {
    x &= 15;
    return (char) ((9 - x >> 4 & 7 | 48) + x);
  }  //fmtHexc(int)
  public static void fmtHex1 (char[] a, int o, int x) {
    x &= 15;
    a[o] = (char) ((9 - x >> 4 & 7 | 48) + x);
  }  //fmtHex1(char[],int,int)
  public static String fmtHex1 (int x) {
    x &= 15;
    return Character.toString ((char) ((9 - x >> 4 & 7 | 48) + x));
  }  //fmtHex1(int)
  public static StringBuilder fmtHex1 (StringBuilder sb, int x) {
    x &= 15;
    return sb.append ((char) ((9 - x >> 4 & 7 | 48) + x));
  }  //fmtHex1(StringBuilder,int)

  //fmtHex2 (a, o, x)
  //s = fmtHex2 (x)
  //sb = fmtHex2 (sb, x)
  //  2桁16進数変換
  //  byte用
  public static void fmtHex2 (char[] a, int o, int x) {
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    a[o    ] = (char) ((9 - x1 >> 4 & 7 | 48) + x1);
    a[o + 1] = (char) ((9 - x0 >> 4 & 7 | 48) + x0);
  }  //fmtHex2(char[],int,int)
  public static String fmtHex2 (int x) {
    //fmtHex2 (FMT_TEMP, 0, x);
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    FMT_TEMP[0] = (char) ((9 - x1 >> 4 & 7 | 48) + x1);
    FMT_TEMP[1] = (char) ((9 - x0 >> 4 & 7 | 48) + x0);
    return String.valueOf (FMT_TEMP, 0, 2);
  }  //fmtHex2(int)
  public static StringBuilder fmtHex2 (StringBuilder sb, int x) {
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    return (sb.
            append ((char) ((9 - x1 >> 4 & 7 | 48) + x1)).
            append ((char) ((9 - x0 >> 4 & 7 | 48) + x0)));
  }  //fmtHex2(StringBuilder,int)

  //fmtHex4 (a, o, x)
  //s = fmtHex4 (x)
  //sb = fmtHex4 (sb, x)
  //  4桁16進数変換
  //  word用
  public static void fmtHex4 (char[] a, int o, int x) {
    int t;
    t = (char) x >>> 12;
    a[o    ] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    a[o + 1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    a[o + 2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    a[o + 3] = (char) ((9 - t >> 4 & 7 | 48) + t);
  }  //fmtHex4(char[],int,int)
  public static String fmtHex4 (int x) {
    //fmtHex4 (FMT_TEMP, 0, x);
    int t;
    t = (char) x >>> 12;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return String.valueOf (FMT_TEMP, 0, 4);
  }  //fmtHex4(int)
  public static StringBuilder fmtHex4 (StringBuilder sb, int x) {
    //fmtHex4 (FMT_TEMP, 0, x);
    int t;
    t = (char) x >>> 12;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return sb.append (FMT_TEMP, 0, 4);
  }  //fmtHex4(StringBuilder,int)

  //fmtHex6 (a, o, x)
  //s = fmtHex6 (x)
  //sb = fmtHex6 (sb, x)
  //  6桁16進数変換
  //  rgb用
  public static void fmtHex6 (char[] a, int o, int x) {
    int t;
    t =        x >>> 20 & 15;
    a[o    ] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    a[o + 1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    a[o + 2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    a[o + 3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    a[o + 4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    a[o + 5] = (char) ((9 - t >> 4 & 7 | 48) + t);
  }  //fmtHex6(char[],int,int)
  public static String fmtHex6 (int x) {
    //fmtHex6 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 20 & 15;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtHex6(int)
  public static StringBuilder fmtHex6 (StringBuilder sb, int x) {
    //fmtHex6 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 20 & 15;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtHex6(StringBuilder,int)

  //fmtHex8 (a, o, x)
  //s = fmtHex8 (x)
  //sb = fmtHex8 (sb, x)
  //  8桁16進数変換
  //  argb,long用
  public static void fmtHex8 (char[] a, int o, int x) {
    int t;
    t =        x >>> 28;
    a[o    ] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 24 & 15;
    a[o + 1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 20 & 15;
    a[o + 2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    a[o + 3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    a[o + 4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    a[o + 5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    a[o + 6] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    a[o + 7] = (char) ((9 - t >> 4 & 7 | 48) + t);
  }  //fmtHex8(char[],int,int)
  public static String fmtHex8 (int x) {
    //fmtHex8 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 28;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 24 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 20 & 15;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    FMT_TEMP[4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[6] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[7] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtHex8(int)
  public static StringBuilder fmtHex8 (StringBuilder sb, int x) {
    //fmtHex8 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 28;
    FMT_TEMP[0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 24 & 15;
    FMT_TEMP[1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 20 & 15;
    FMT_TEMP[2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) x >>> 12;
    FMT_TEMP[4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[6] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        x        & 15;
    FMT_TEMP[7] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtHex8(StringBuilder,int)

  public static StringBuilder fmtHex16 (StringBuilder sb, long x) {
    //fmtHex16 (FMT_TEMP, 0, x);
    int s, t;
    s = (int) (x >>> 32);
    t =        s >>> 28;
    FMT_TEMP[ 0] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 24 & 15;
    FMT_TEMP[ 1] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 20 & 15;
    FMT_TEMP[ 2] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 16 & 15;
    FMT_TEMP[ 3] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) s >>> 12;
    FMT_TEMP[ 4] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>>  8 & 15;
    FMT_TEMP[ 5] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>>  4 & 15;
    FMT_TEMP[ 6] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s        & 15;
    FMT_TEMP[ 7] = (char) ((9 - t >> 4 & 7 | 48) + t);
    s = (int)  x;
    t =        s >>> 28;
    FMT_TEMP[ 8] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 24 & 15;
    FMT_TEMP[ 9] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 20 & 15;
    FMT_TEMP[10] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>> 16 & 15;
    FMT_TEMP[11] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t = (char) s >>> 12;
    FMT_TEMP[12] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>>  8 & 15;
    FMT_TEMP[13] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s >>>  4 & 15;
    FMT_TEMP[14] = (char) ((9 - t >> 4 & 7 | 48) + t);
    t =        s        & 15;
    FMT_TEMP[15] = (char) ((9 - t >> 4 & 7 | 48) + t);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtHex16(StringBuilder,long)

  //--------------------------------------------------------------------------------
  //10進数変換
  //  除算は遅いので逆数乗算で商と余りを求めて下位から充填する
  //  x/yを計算する代わりにceil(pow(2,n)/y)を掛けてから右にnビットシフトする
  //  m=((1<<n)+y-1)/yとおくと0<=x<=((((m-1)/(m*y-(1<<n))+1)<<n)-1)/mの範囲でx/yはm*x>>nに置き換えられる
  //    >perl -e "use GMP::Mpz qw(:all);$y=mpz(10);for($n=mpz(0);$n<=31;$n++){$m=((1<<$n)+$y-1)/$y;$l=(((($m-1)/($m*$y-(1<<$n))+1)<<$n)-1)/$m;printf'x*%s>>%s (0<=x<=%s)%c',$m,$n,$l,10;}"
  //    x*1>>0 (0<=x<=0)
  //    x*1>>1 (0<=x<=1)
  //    x*1>>2 (0<=x<=3)
  //    x*1>>3 (0<=x<=7)
  //    x*2>>4 (0<=x<=7)
  //    x*4>>5 (0<=x<=7)
  //    x*7>>6 (0<=x<=18)
  //    x*13>>7 (0<=x<=68)
  //    x*26>>8 (0<=x<=68)
  //    x*52>>9 (0<=x<=68)
  //    x*103>>10 (0<=x<=178)  2桁
  //    x*205>>11 (0<=x<=1028)  3桁
  //    x*410>>12 (0<=x<=1028)
  //    x*820>>13 (0<=x<=1028)
  //    x*1639>>14 (0<=x<=2738)
  //    x*3277>>15 (0<=x<=16388)  4桁
  //    x*6554>>16 (0<=x<=16388)
  //    x*13108>>17 (0<=x<=16388)
  //    x*26215>>18 (0<=x<=43698)
  //    x*52429>>19 (0<=x<=262148)  5桁。ここからlong
  //    x*104858>>20 (0<=x<=262148)
  //    x*209716>>21 (0<=x<=262148)
  //    x*419431>>22 (0<=x<=699058)
  //    x*838861>>23 (0<=x<=4194308)  6桁
  //    x*1677722>>24 (0<=x<=4194308)
  //    x*3355444>>25 (0<=x<=4194308)
  //    x*6710887>>26 (0<=x<=11184818)  7桁
  //    x*13421773>>27 (0<=x<=67108868)
  //    x*26843546>>28 (0<=x<=67108868)
  //    x*53687092>>29 (0<=x<=67108868)
  //    x*107374183>>30 (0<=x<=178956978)  8桁
  //    x*214748365>>31 (0<=x<=1073741828)  9桁
  //
  //  検算
  //    >perl -e "use GMP::Mpz qw(:all);$y=mpz(10);for($n=mpz(0);$n<=23;$n++){$m=((1<<$n)+$y-1)/$y;$l=(((($m-1)/($m*$y-(1<<$n))+1)<<$n)-1)/$m;for($x=mpz(0);$x<=$l+1;$x++){$t=$m*$x>>$n;$z=$x/$y;if($z!=$t){printf'n=%d,m=%d,l=%d,x=%d,y=%d,z=%d,t=%d%c',$n,$m,$l,$x,$y,$z,$t,10;}}}"
  //    n=0,m=1,l=0,x=1,y=10,z=0,t=1
  //    n=1,m=1,l=1,x=2,y=10,z=0,t=1
  //    n=2,m=1,l=3,x=4,y=10,z=0,t=1
  //    n=3,m=1,l=7,x=8,y=10,z=0,t=1
  //    n=4,m=2,l=7,x=8,y=10,z=0,t=1
  //    n=5,m=4,l=7,x=8,y=10,z=0,t=1
  //    n=6,m=7,l=18,x=19,y=10,z=1,t=2
  //    n=7,m=13,l=68,x=69,y=10,z=6,t=7
  //    n=8,m=26,l=68,x=69,y=10,z=6,t=7
  //    n=9,m=52,l=68,x=69,y=10,z=6,t=7
  //    n=10,m=103,l=178,x=179,y=10,z=17,t=18
  //    n=11,m=205,l=1028,x=1029,y=10,z=102,t=103
  //    n=12,m=410,l=1028,x=1029,y=10,z=102,t=103
  //    n=13,m=820,l=1028,x=1029,y=10,z=102,t=103
  //    n=14,m=1639,l=2738,x=2739,y=10,z=273,t=274
  //    n=15,m=3277,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=16,m=6554,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=17,m=13108,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=18,m=26215,l=43698,x=43699,y=10,z=4369,t=4370
  //    n=19,m=52429,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=20,m=104858,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=21,m=209716,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=22,m=419431,l=699058,x=699059,y=10,z=69905,t=69906
  //    n=23,m=838861,l=4194308,x=4194309,y=10,z=419430,t=419431

  //  4桁まではあらかじめテーブルに展開しておく
  public static final int[] FMT_BCD4 = new int[10000];
  public static final int[] FMT_DCB4 = new int[65536];

  //--------------------------------------------------------------------------------
  //fmtInit ()
  //  初期化
  public static void fmtInit () {
    Arrays.fill (FMT_DCB4, -1);
    int i = 0;
    int x = 0;
    for (int a = 0; a < 10; a++) {
      for (int b = 0; b < 10; b++) {
        for (int c = 0; c < 10; c++) {
          FMT_DCB4[FMT_BCD4[i    ] = x    ] = i;
          FMT_DCB4[FMT_BCD4[i + 1] = x + 1] = i + 1;
          FMT_DCB4[FMT_BCD4[i + 2] = x + 2] = i + 2;
          FMT_DCB4[FMT_BCD4[i + 3] = x + 3] = i + 3;
          FMT_DCB4[FMT_BCD4[i + 4] = x + 4] = i + 4;
          FMT_DCB4[FMT_BCD4[i + 5] = x + 5] = i + 5;
          FMT_DCB4[FMT_BCD4[i + 6] = x + 6] = i + 6;
          FMT_DCB4[FMT_BCD4[i + 7] = x + 7] = i + 7;
          FMT_DCB4[FMT_BCD4[i + 8] = x + 8] = i + 8;
          FMT_DCB4[FMT_BCD4[i + 9] = x + 9] = i + 9;
          i += 10;
          x += 1 << 4;
        }
        x += 6 << 4;
      }
      x += 6 << 8;
    }
  }  //fmtInit()

  //y = fmtBcd4 (x)
  //  xを0～9999にクリッピングしてから4桁のBCDに変換する
  public static int fmtBcd4 (int x) {
    //x = Math.max (0, Math.min (9999, x));
    //perl optdiv.pl 9999 10
    //  x/10==x*3277>>>15 (0<=x<=16388) [9999*3277==32766723]
    //int t = x * 3277 >> 15;  //x/10
    //int y = x - t * 10;  //1の位
    //x = t * 3277 >> 15;  //x/100
    //y |= t - x * 10 << 4;  //10の位
    //t = x * 3277 >> 15;  //x/1000
    //return t << 12 | x - t * 10 << 8 | y;  //1000の位,100の位
    return FMT_BCD4[Math.max (0, Math.min (9999, x))];
  }  //fmtBcd4(int)

  //y = fmtBcd8 (x)
  //  xを0～99999999にクリッピングしてから8桁のBCDに変換する
  public static int fmtBcd8 (int x) {
    x = Math.max (0, Math.min (99999999, x));
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int q = (int) ((long) x * 109951163L >>> 40);  //x/10000。1.6ns@2000000000
    //int q = x / 10000;  //2.0ns@2000000000
    return FMT_BCD4[q] << 16 | FMT_BCD4[x - 10000 * q];
  }  //fmtBcd8(int)

  //y = fmtBcd12 (x)
  //  xを0～999999999999Lにクリッピングしてから12桁のBCDに変換する
  public static long fmtBcd12 (long x) {
    x = Math.max (0L, Math.min (999999999999L, x));
    int q = (int) ((double) x / 100000000.0);  //(int) (x / 100000000L);
    int r = (int) (x - 100000000L * q);
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int rq = (int) ((long) r * 109951163L >>> 40);  //r/10000
    //int rq = r / 10000;
    return (long) FMT_BCD4[q] << 32 | 0xffffffffL & (FMT_BCD4[rq] << 16 | FMT_BCD4[r - 10000 * rq]);
  }  //fmtBcd12(long)

  //y = fmtBcd16 (x)
  //  xを0～9999999999999999Lにクリッピングしてから16桁のBCDに変換する
  public static long fmtBcd16 (long x) {
    x = Math.max (0L, Math.min (9999999999999999L, x));
    int q = x <= (1L << 53) ? (int) ((double) x / 100000000.0) : (int) (x / 100000000L);
    int r = (int) (x - 100000000L * q);
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int qq = (int) ((long) q * 109951163L >>> 40);  //q/10000
    //int qq = q / 10000;
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int rq = (int) ((long) r * 109951163L >>> 40);  //r/10000
    //int rq = r / 10000;
    return (long) (FMT_BCD4[qq] << 16 | FMT_BCD4[q - 10000 * qq]) << 32 | 0xffffffffL & (FMT_BCD4[rq] << 16 | FMT_BCD4[r - 10000 * rq]);
  }  //fmtBcd16(long)

  //--------------------------------------------------------------------------------
  //o = fmtCA02u (a, o, x)
  //sb = fmtSB02u (sb, x)
  //  %02u
  //  2桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA02u (char[] a, int o, int x) {
    if (x < 0 || 99 < x) {
      x = 99;
    }
    x = FMT_BCD4[x];
    a[o    ] = (char) ('0' | x >>> 4);
    a[o + 1] = (char) ('0' | x        & 15);
    return o + 2;
  }  //fmtCA02u(char[],int,int)
  public static StringBuilder fmtSB02u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA02u (FMT_TEMP, 0, x));
  }  //fmtSB02u(StringBuilder,int)

  //o = fmtCA2u (a, o, x)
  //sb = fmtSB2u (sb, x)
  //  %2u
  //  2桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA2u (char[] a, int o, int x) {
    if (x < 0 || 99 < x) {
      x = 99;
    }
    x = FMT_BCD4[x];
    if (x <= 0x000f) {  //1桁
      a[o++] = (char) ('0' | x);
    } else {  //2桁
      a[o++] = (char) ('0' | x >>>  4);
      a[o++] = (char) ('0' | x        & 15);
    }
    return o;
  }  //fmtCA2u(char[],int,int)
  public static StringBuilder fmtSB2u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA2u (FMT_TEMP, 0, x));
  }  //fmtSB2u(StringBuilder,int)

  //o = fmtCA04u (a, o, x)
  //sb = fmtSB04u (sb, x)
  //  %04u
  //  4桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA04u (char[] a, int o, int x) {
    if (x < 0 || 9999 < x) {
      x = 9999;
    }
    x = FMT_BCD4[x];
    a[o    ] = (char) ('0' | x >>> 12);
    a[o + 1] = (char) ('0' | x >>>  8 & 15);
    a[o + 2] = (char) ('0' | x >>>  4 & 15);
    a[o + 3] = (char) ('0' | x        & 15);
    return o + 4;
  }  //fmtCA04u(char[],int,int)
  public static StringBuilder fmtSB04u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA04u (FMT_TEMP, 0, x));
  }  //fmtSB04u(StringBuilder,int)

  //o = fmtCA4u (a, o, x)
  //sb = fmtSB4u (sb, x)
  //  %4u
  //  4桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA4u (char[] a, int o, int x) {
    if (x < 0 || 9999 < x) {
      x = 9999;
    }
    x = FMT_BCD4[x];
    if (x <= 0x000f) {  //1桁
      a[o++] = (char) ('0' | x);
    } else if (x <= 0x00ff) {  //2桁
      a[o++] = (char) ('0' | x >>>  4);
      a[o++] = (char) ('0' | x        & 15);
    } else if (x <= 0x0fff) {  //3桁
      a[o++] = (char) ('0' | x >>>  8);
      a[o++] = (char) ('0' | x >>>  4 & 15);
      a[o++] = (char) ('0' | x        & 15);
    } else {  //4桁
      a[o++] = (char) ('0' | x >>> 12);
      a[o++] = (char) ('0' | x >>>  8 & 15);
      a[o++] = (char) ('0' | x >>>  4 & 15);
      a[o++] = (char) ('0' | x        & 15);
    }
    return o;
  }  //fmtCA4u(char[],int,int)
  public static StringBuilder fmtSB4u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA4u (FMT_TEMP, 0, x));
  }  //fmtSB4u(StringBuilder,int)

  //o = fmtCA08u (a, o, x)
  //sb = fmtSB08u (sb, x)
  //  %08u
  //  8桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA08u (char[] a, int o, int x) {
    if (x < 0 || 99999999 < x) {
      x = 99999999;
    }
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int h = (int) ((long) x * 109951163L >>> 40);  //x/10000
    return fmtCA04u (a, fmtCA04u (a, o, h), x - h * 10000);
  }  //fmtCA08u(char[],int,int)
  public static StringBuilder fmtSB08u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA08u (FMT_TEMP, 0, x));
  }  //fmtSB08u(StringBuilder,int)

  //o = fmtCA8u (a, o, x)
  //sb = fmtSB8u (sb, x)
  //  %8u
  //  8桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA8u (char[] a, int o, int x) {
    if (x < 0 || 99999999 < x) {
      x = 99999999;
    }
    if (x <= 9999) {  //1～4桁
      return fmtCA4u (a, o, x);
    } else {  //5～8桁
      //perl optdiv.pl 99999999 10000
      //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
      int h = (int) ((long) x * 109951163L >>> 40);  //x/10000
      return fmtCA04u (a, fmtCA4u (a, o, h), x - h * 10000);
    }
  }  //fmtCA8u(char[],int,int)
  public static StringBuilder fmtSB8u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA8u (FMT_TEMP, 0, x));
  }  //fmtSB8u(StringBuilder,int)

  //o = fmtCAd (a, o, x)
  //sb = fmtSBd (sb, x)
  //  %d
  //  10進数変換(符号あり,ゼロサプレスあり)
  public static int fmtCAd (char[] a, int o, long x) {
    if (x < 0L) {
      x = -x;
      a[o++] = '-';
    }
    if (x <= 99999999L) {  //1～8桁
      return fmtCA8u (a, o, (int) x);
    } else if (x <= 9999999999999999L) {  //9～16桁
      long h = x / 100000000L;
      return fmtCA08u (a, fmtCA8u (a, o, (int) h), (int) (x - h * 100000000L));
    } else {  //17～19桁
      long hh = x / 10000000000000000L;
      x -= hh * 10000000000000000L;
      long h = x / 100000000L;
      return fmtCA08u (a, fmtCA08u (a, fmtCA4u (a, o, (int) hh), (int) h), (int) (x - h * 100000000L));
    }
  }  //fmtCAd(char[],int,long)
  public static StringBuilder fmtSBd (StringBuilder sb, long x) {
    return sb.append (FMT_TEMP, 0, fmtCAd (FMT_TEMP, 0, x));
  }  //fmtSBd(StringBuilder,long)

  //o = fmtCAnd (a, o, n, x)
  //sb = fmtSBnd (sb, n, x)
  //  %*d
  //  n桁10進数変換(符号あり,ゼロサプレスあり)
  //  n桁に収まらないとき右側にはみ出すのでバッファのサイズに注意
  public static int fmtCAnd (char[] a, int o, int n, long x) {
    int t = fmtCAd (a, o, x);  //現在の末尾
    n += o;  //必要な末尾
    if (t < n) {  //余っている
      int i = n;
      while (o < t) {  //右から順に右にずらす
        a[--i] = a[--t];
      }
      while (o < i) {  //左にできた隙間を' 'で埋める
        a[--i] = ' ';
      }
      t = n;
    }
    return t;
  }  //fmtnu(char[],int,int,long)
  public static StringBuilder fmtSBnd (StringBuilder sb, int n, int x) {
    return sb.append (FMT_TEMP, 0, fmtCAnd (FMT_TEMP, 0, n, x));
  }  //fmtSBnu(StringBuilder,int,long)

  //--------------------------------------------------------------------------------
  //10進数文字列解析

  //x = fmtParseInt (s, i, min, max, err)
  //  文字列sのインデックスiから基数10で整数xを読み取る
  //x = fmtParseIntRadix (s, i, min, max, err, radix)
  //  文字列sのインデックスiから基数radixで整数xを読み取る
  //  基数radixは2,8,10,16のいずれかに限る
  //  1文字も読み取れないかmin<=x&&x<=maxでないときはerrを返す
  //  先頭の空白を読み飛ばす
  //  数値の先頭の'$'は16進数の強制指定とみなす
  //  数値の後のゴミは無視する
  public static int fmtParseInt (String s, int i, int min, int max, int err) {
    return fmtParseIntRadix (s, i, min, max, err, 10);
  }  //fmtParseInt(String,int,int,int,int)
  public static int fmtParseIntRadix (String s, int i, int min, int max, int err, int radix) {
    if (s == null) {
      return err;
    }
    int l = s.length ();
    int c = i < l ? s.charAt (i++) : -1;
    //空白を読み飛ばす
    while (c == ' ' || c == '\t') {
      c = i < l ? s.charAt (i++) : -1;
    }
    //符号を読み取る
    int n = 0;
    if (c == '+') {
      c = i < l ? s.charAt (i++) : -1;
    } else if (c == '-') {
      n = 1;
      c = i < l ? s.charAt (i++) : -1;
    }
    //基数を読み取る
    //        2進数の範囲        8進数の範囲       10進数の範囲        16進数の範囲
    //  +    0x3fffffff*2+1     0x0fffffff*8+7     214748364*10+7     0x07ffffff*16+15
    //  -  -(0x40000000*2+0)  -(0x10000000*8+0)  -(214748364*10+8)  -(0x08000000*16+ 0)
    int o;
    int p;
    if (c == '$') {  //16進数
      o = 0x07ffffff + n;
      p = 15 + n & 15;
      radix = 16;
      c = i < l ? s.charAt (i++) : -1;
    } else if (radix == 16) {  //16進数
      o = 0x07ffffff + n;
      p = 15 + n & 15;
    } else if (radix == 8) {  //8進数
      o = 0x0fffffff + n;
      p = 7 + n & 7;
    } else if (radix == 2) {  //2進数
      o = 0x3fffffff + n;
      p = 1 + n & 1;
    } else {  //10進数
      o = 214748364;
      p = 7 + n;
      radix = 10;
    }
    //数値を読み取る
    int x = Character.digit (c, radix);
    if (x < 0) {
      return err;
    }
    c = i < l ? Character.digit (s.charAt (i++), radix) : -1;
    while (c >= 0) {
      int t = x - o;
      if (t > 0 || t == 0 && c > p) {
        return err;
      }
      x = x * radix + c;
      c = i < l ? Character.digit (s.charAt (i++), radix) : -1;
    }
    if (n != 0) {
      x = -x;
    }
    return min <= x && x <= max ? x : err;
  }  //fmtParseIntRadix(String,int,int,int,int,int)



  //========================================================================================
  //$$MAT 数学関数

  //x = matMax3 (x1, x2, x3)
  //x = matMax4 (x1, x2, x3, x4)
  //x = matMax5 (x1, x2, x3, x4, x5)
  //  最大値
  public static long matMax3 (long x1, long x2, long x3) {
    return Math.max (Math.max (x1, x2), x3);
  }  //matMax3(long,long,long)
  public static long matMax4 (long x1, long x2, long x3, long x4) {
    return Math.max (Math.max (x1, x2), Math.max (x3, x4));
  }  //matMax4(long,long,long,long)
  public static long matMax5 (long x1, long x2, long x3, long x4, long x5) {
    return Math.max (Math.max (Math.max (x1, x2), Math.max (x3, x4)), x5);
  }  //matMax5(long,long,long,long,long)

  //x = matMin3 (x1, x2, x3)
  //x = matMin4 (x1, x2, x3, x4)
  //x = matMin5 (x1, x2, x3, x4, x5)
  //  最小値
  public static long matMin3 (long x1, long x2, long x3) {
    return Math.min (Math.min (x1, x2), x3);
  }  //matMin3(long,long,long)
  public static long matMin4 (long x1, long x2, long x3, long x4) {
    return Math.min (Math.min (x1, x2), Math.min (x3, x4));
  }  //matMin4(long,long,long,long)
  public static long matMin5 (long x1, long x2, long x3, long x4, long x5) {
    return Math.min (Math.min (Math.min (x1, x2), Math.min (x3, x4)), x5);
  }  //matMin5(long,long,long,long,long)



  //========================================================================================
  //$$STR 文字列

  //s = encodeUTF8 (s)
  //  UTF-8変換
  //  00000000 00000000 00000000 0xxxxxxx => 00000000 00000000 00000000 0xxxxxxx
  //  00000000 00000000 00000xxx xxyyyyyy => 00000000 00000000 110xxxxx 10yyyyyy
  //  00000000 00000000 xxxxyyyy yyzzzzzz => 00000000 1110xxxx 10yyyyyy 10zzzzzz
  //  00000000 000xxxyy yyyyzzzz zzxxxxxx => 11110xxx 10yyyyyy 10zzzzzz 10xxxxxx
  public static String strEncodeUTF8 (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int u = s.charAt (i);
      if (0xd800 <= u && u <= 0xdbff && i + 1 < l) {
        int v = s.charAt (i + 1);
        if (0xdc00 <= v && v <= 0xdfff) {  //surrogate pair
          u = 0x10000 + ((u & 0x3ff) << 10) + (v & 0x3ff);
          i++;
        }
      }
      if ((u & 0xffffff80) == 0) {  //7bit
        sb.append ((char) u);
      } else if ((u & 0xfffff800) == 0) {  //11bit
        u = (0x0000c080 |
             (u & 0x000007c0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) (u >> 8)).append ((char) (u & 0xff));
      } else if ((u & 0xffff0000) == 0 && !(0xd800 <= u && u <= 0xdfff)) {  //16bit except broken surrogate pair
        u = (0x00e08080 |
             (u & 0x0000f000) << 4 |
             (u & 0x00000fc0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) (u >> 16)).append ((char) ((u >> 8) & 0xff)).append ((char) (u & 0xff));
      } else if ((u & 0xffe00000) == 0) {  //21bit
        u = (0xf0808080 |
             (u & 0x001c0000) << 6 |
             (u & 0x0003f000) << 4 |
             (u & 0x00000fc0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) ((u >> 24) & 0xff)).append ((char) ((u >> 16) & 0xff)).append ((char) ((u >> 8) & 0xff)).append ((char) (u & 0xff));
      } else {  //out of range or broken surrogate pair
        sb.append ((char) 0xef).append ((char) 0xbf).append ((char) 0xbd);  //U+FFFD REPLACEMENT CHARACTER
      }
    }
    return sb.toString ();
  }  //encodeUTF8(String)

  //s = decodeUTF8 (s)
  //  UTF-8逆変換
  //  00000000 00000000 00000000 0xxxxxxx => 00000000 00000000 00000000 0xxxxxxx
  //  00000000 00000000 110xxxxx 10yyyyyy => 00000000 00000000 00000xxx xxyyyyyy
  //  00000000 1110xxxx 10yyyyyy 10zzzzzz => 00000000 00000000 xxxxyyyy yyzzzzzz
  //  11110xxx 10yyyyyy 10zzzzzz 10xxxxxx => 00000000 000xxxyy yyyyzzzz zzxxxxxx
  public static String strDecodeUTF8 (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i) & 0xff;
      for (int k = ((c & 0x80) == 0x00 ? 0 :  //0xxxxxxx 7bit
                    (c & 0xe0) == 0xc0 ? 1 :  //110xxxxx 11bit
                    (c & 0xf0) == 0xe0 ? 2 :  //1110xxxx 16bit
                    (c & 0xf8) == 0xf0 ? 3 :  //11110xxx 21bit
                    -1);  //not supported
           --k >= 0; ) {
        c = c << 8 | (i + 1 < l ? s.charAt (++i) & 0xff : 0);
      }
      int u = ((c & 0xffffff80) == 0x00000000 ? c :
               (c & 0xffffe0c0) == 0x0000c080 ? ((c & 0x00001f00) >> 2 |
                                                 (c & 0x0000003f)) :
               (c & 0xfff0c0c0) == 0x00e08080 ? ((c & 0x000f0000) >> 4 |
                                                 (c & 0x00003f00) >> 2 |
                                                 (c & 0x0000003f)) :
               (c & 0xf8c0c0c0) == 0xf0808080 ? ((c & 0x07000000) >> 6 |
                                                 (c & 0x003f0000) >> 4 |
                                                 (c & 0x00003f00) >> 2 |
                                                 (c & 0x0000003f)) :
               0xfffd);  //U+FFFD REPLACEMENT CHARACTER
      if (u <= 0x0000ffff) {
        sb.append (0xd800 <= u && u <= 0xdfff ? '\ufffd' :  //U+FFFD REPLACEMENT CHARACTER
                   (char) u);
      } else if (u <= 0x0010ffff) {
        u -= 0x000010000;
        sb.append ((char) (0xd800 + ((u >> 10) & 0x3ff))).append ((char) (0xdc00 + (u & 0x3ff)));
      }
    }
    return sb.toString ();
  }  //decodeUTF8(String)

  //uri = encodeURI (s)
  //  URI変換
  //  UTF-8変換を行ってからRFC3986のPercent-Encodingを行う
  //  フォームの送信に使用されるapplication/x-www-form-urlencodedではない。" "は"+"ではなく"%20"に変換される
  public static final int[] IsURIChar = {  //URIに使える文字。RFC3986のUnreserved Characters。[-.0-9A-Z_a-z~]
    //00000000 00000000 11111111 11111111
    //01234567 89abcdef 01234567 89abcdef
    0b00000000_00000000_00000000_00000000,  //0x00..0x1f
    0b00000000_00000110_11111111_11000000,  //0x20..0x3f [-.0-9]
    0b01111111_11111111_11111111_11100001,  //0x40..0x5f [A-Z_]
    0b01111111_11111111_11111111_11100010,  //0x60..0x7f [a-z~]
  };
  public static String strEncodeURI (String s) {
    s = strEncodeUTF8 (s);  //UTF-8変換
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i);
      if (c < 0x80 && IsURIChar[c >> 5] << c < 0) {  //URIに使える文字
        sb.append ((char) c);
      } else {
        fmtHex2 (sb.append ('%'), c);
      }
    }
    return sb.toString ();
  }  //encodeURI(String)

  //s = decodeURI (s)
  //  URI逆変換
  //  RFC3986のPercent-Encodingの逆変換を行ってからUTF-8逆変換を行う
  //  フォームの送信に使用されるapplication/x-www-form-urlencodedではない。"+"は" "に変換されない
  public static final byte[] strIsHexChar = {  //16進数に使えるASCII文字。[0-9A-Fa-f]
    // 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f
    -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x00..0x1f
    -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,-1,-1,-1,-1,-1,-1,  //0x20..0x3f [0-9]
    -1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x40..0x5f [A-F]
    -1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x60..0x7f [a-f]
  };
  public static String strDecodeURI (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i);
      if (c == '%' && i + 2 < l) {
        int d = s.charAt (i + 1);
        int e = s.charAt (i + 2);
        if (d < 0x80 && (d = strIsHexChar[d]) >= 0 &&
            e < 0x80 && (e = strIsHexChar[e]) >= 0) {
          sb.append ((char) (d << 4 | e));
        } else {
          sb.append ((char) c);
        }
      } else {
        sb.append ((char) c);
      }
    }
    return sb.toString ();
  }  //decodeURI(String)



  //========================================================================================
  //$$IMG イメージ

  //image = createImage (width, height, pattern, rgb, ...)
  //  イメージを作る
  public static BufferedImage createImage (int width, int height, String pattern, int... rgbs) {
    BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
    int[] bitmap = ((DataBufferInt) image.getRaster ().getDataBuffer ()).getData ();
    int length = width * height;
    for (int i = 0; i < length; i++) {
      char c = pattern.charAt (i);
      bitmap[i] = rgbs[c < '0' ? 0 : Character.digit (c, 16)];
    }
    return image;
  }  //createImage(int,int,String,int...)

  //icon = createImageIcon (width, height, pattern, rgb, ...)
  //  イメージアイコンを作る
  public static ImageIcon createImageIcon (int width, int height, String pattern, int... rgbs) {
    return new ImageIcon (createImage (width, height, pattern, rgbs));
  }  //createImageIcon(int,int,String,int...)

  //paint = createTexturePaint (width, height, pattern, rgb, ...)
  //  テクスチャペイントを作る
  public static TexturePaint createTexturePaint (int width, int height, String pattern, int... rgbs) {
    return new TexturePaint (createImage (width, height, pattern, rgbs), new Rectangle (0, 0, width, height));
  }  //createTexturePaint(int,int,String,int...)

  //image = loadImage (name)
  //  イメージを読み込む
  public static BufferedImage loadImage (String name) {
    BufferedImage image = null;
    try {
      image = ImageIO.read (new File (name));
    } catch (Exception e) {
    }
    return image;
  }  //loadImage(String)

  //sucess = saveImage (image, name)
  //sucess = saveImage (image, name, quality)
  //  イメージを書き出す
  public static boolean saveImage (BufferedImage image, String name) {
    return saveImage (image, name, 0.75F);
  }  //saveImage(BufferedImage,String)
  public static boolean saveImage (BufferedImage image, String name, float quality) {
    int index = name.lastIndexOf (".");
    if (index < 0) {  //拡張子がない
      return false;
    }
    if (name.substring (index).equalsIgnoreCase (".ico")) {  //アイコンファイルの作成
      return saveIcon (name, image);
    }
    Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix (name.substring (index + 1));  //拡張子に対応するImageWriterがないときは空のIteratorを返す
    if (!iterator.hasNext ()) {  //拡張子に対応するImageWriterがない
      return false;
    }
    ImageWriter imageWriter = iterator.next ();
    ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam ();
    if (imageWriteParam.canWriteCompressed ()) {
      imageWriteParam.setCompressionMode (ImageWriteParam.MODE_EXPLICIT);
      imageWriteParam.setCompressionQuality (quality);
    }
    try {
      File file = new File (name);
      ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream (file);
      imageWriter.setOutput (imageOutputStream);
      imageWriter.write (null, new IIOImage (image, null, null), imageWriteParam);
      imageOutputStream.close ();
    } catch (Exception e) {
      //e.printStackTrace ();
      return false;
    }
    return true;
  }  //saveImage(BufferedImage,String,float)



  //========================================================================================
  //$$ICO アイコンファイル
  //  サイズの異なる複数のアイコンを格納することができる
  //  ワードサイズとロングサイズのデータはすべてリトルエンディアン
  //
  //  アイコンファイル(.ico)
  //    ファイルヘッダ
  //    エントリデータ0
  //           :
  //    エントリデータn-1
  //    イメージデータ0
  //           :
  //    イメージデータn-1
  //
  //  ファイルヘッダ
  //    0000  .iw  0=予約
  //    0002  .iw  1=アイコン
  //    0004  .iw  n=アイコン数
  //    0006
  //
  //  エントリデータ
  //    0000  .b   幅
  //    0001  .b   高さ
  //    0002  .b   色数(0=256以上)
  //    0003  .b   0=予約
  //    0004  .iw  1=プレーン数
  //    0006  .iw  ピクセル毎のビット数
  //    0008  .il  イメージデータの長さ
  //    000c  .il  ファイルヘッダからイメージデータまでのオフセット
  //    0010
  //
  //  イメージデータ
  //    イメージヘッダ
  //    パレットテーブル
  //    パターンデータ
  //    マスクデータ
  //
  //  イメージヘッダ
  //    0000  .il  40=イメージヘッダの長さ
  //    0004  .il  幅
  //    0008  .il  高さ*2
  //    000c  .iw  1=プレーン数
  //    000e  .iw  ピクセル毎のビット数
  //    0010  .il  0=無圧縮
  //    0014  .il  0=画像データサイズ(省略)
  //    0018  .il  0=横解像度(省略)
  //    001c  .il  0=縦解像度(省略)
  //    0020  .il  p=パレット数
  //    0024  .il  0=重要なパレットのインデックス(省略)
  //    0028
  //
  //  パレットテーブル
  //    0000  .il  パレット0(BGR0)
  //                   :
  //          .il  パレットp-1(BGR0)
  //    4*p
  //
  //  パターンデータ
  //    ピクセルの順序は左下から右上
  //    バイト内のビットの順序は上位から下位
  //    1ラインのデータ長は4の倍数
  //
  //  マスクデータ
  //    0=描画,1=透過
  //    ピクセルの順序は左下から右上
  //    バイト内のビットの順序は上位から下位
  //    1ラインのデータ長は4の倍数

  //success = saveIcon (name, image, ...);
  //  アイコンファイルを出力する
  public static boolean saveIcon (String fileName, BufferedImage... arrayImage) {
    int iconCount = arrayImage.length;
    int[][] arrayPaletTable = new int[iconCount][];  //パレットテーブル
    int[] arrayPaletCount = new int[iconCount];  //パレット数。0=パレットを使わない
    int[] arrayPixelBits = new int[iconCount];  //ピクセル毎のビット数。24,8,4,2,1のいずれか
    int[] arrayPatternLineSize = new int[iconCount];  //パターンデータの1ラインのバイト数
    int[] arrayMaskLineSize = new int[iconCount];  //マスクデータの1ラインのバイト数
    int[] arrayImageSize = new int[iconCount];  //イメージデータの長さ
    int[] arrayImageOffset = new int[iconCount];  //ファイルヘッダからイメージデータまでのオフセット
    int fileSize = 6 + 16 * iconCount;
    for (int iconNumber = 0; iconNumber < iconCount; iconNumber++) {
      BufferedImage image = arrayImage[iconNumber];
      int width = image.getWidth ();
      int height = image.getHeight ();
      //パレットテーブルを作る
      int[] paletTable = new int[256];
      int paletCount = 0;
    countPalet:
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            continue;
          }
          int l = 0;
          int r = paletCount;
          while (l < r) {
            int m = l + r >> 1;
            if (paletTable[m] < rgb) {
              l = m + 1;
            } else {
              r = m;
            }
          }
          if (l == paletCount || paletTable[l] != rgb) {  //新しい色
            if (paletCount == 256) {  //色数が多すぎる
              paletCount = 0;
              break countPalet;
            }
            for (int i = paletCount; i > l; i--) {
              paletTable[i] = paletTable[i - 1];
            }
            paletTable[l] = rgb;
            paletCount++;
          }
        }  //for x
      }  //for y
      int pixelBits = (paletCount == 0 ? 24 :
                       paletCount > 16 ? 8 :
                       paletCount > 4 ? 4 :
                       paletCount > 2 ? 2 :
                       1);
      int patternLineSize = pixelBits * width + 31 >> 5 << 2;
      int maskLineSize = width + 31 >> 5 << 2;
      int imageSize = 40 + 4 * paletCount + patternLineSize * height + maskLineSize * height;
      arrayPaletTable[iconNumber] = paletTable;
      arrayPaletCount[iconNumber] = paletCount;
      arrayPixelBits[iconNumber] = pixelBits;
      arrayPatternLineSize[iconNumber] = patternLineSize;
      arrayMaskLineSize[iconNumber] = maskLineSize;
      arrayImageSize[iconNumber] = imageSize;
      arrayImageOffset[iconNumber] = fileSize;
      fileSize += imageSize;
    }  //for iconNumber
    byte[] bb = new byte[fileSize];
    //ファイルヘッダ
    ByteArray.byaWiw (bb, 0, 0);
    ByteArray.byaWiw (bb, 2, 1);
    ByteArray.byaWiw (bb, 4, iconCount);
    for (int iconNumber = 0; iconNumber < iconCount; iconNumber++) {
      BufferedImage image = arrayImage[iconNumber];
      int width = image.getWidth ();
      int height = image.getHeight ();
      int[] paletTable = arrayPaletTable[iconNumber];
      int paletCount = arrayPaletCount[iconNumber];
      int pixelBits = arrayPixelBits[iconNumber];
      int patternLineSize = arrayPatternLineSize[iconNumber];
      int maskLineSize = arrayMaskLineSize[iconNumber];
      int imageSize = arrayImageSize[iconNumber];
      int imageOffset = arrayImageOffset[iconNumber];
      //エントリデータ
      int o = 6 + 16 * iconNumber;
      ByteArray.byaWb (bb, o, width);
      ByteArray.byaWb (bb, o + 1, height);
      ByteArray.byaWb (bb, o + 2, paletCount);
      ByteArray.byaWb (bb, o + 3, 0);
      ByteArray.byaWiw (bb, o + 4, 1);
      ByteArray.byaWiw (bb, o + 6, pixelBits);
      ByteArray.byaWil (bb, o + 8, imageSize);
      ByteArray.byaWil (bb, o + 12, imageOffset);
      //イメージヘッダ
      o = imageOffset;
      ByteArray.byaWil (bb, o, 40);
      ByteArray.byaWil (bb, o + 4, width);
      ByteArray.byaWil (bb, o + 8, height * 2);
      ByteArray.byaWiw (bb, o + 12, 1);
      ByteArray.byaWiw (bb, o + 14, pixelBits);
      ByteArray.byaWil (bb, o + 16, 0);
      ByteArray.byaWil (bb, o + 20, 0);
      ByteArray.byaWil (bb, o + 24, 0);
      ByteArray.byaWil (bb, o + 28, 0);
      ByteArray.byaWil (bb, o + 32, paletCount);
      ByteArray.byaWil (bb, o + 36, 0);
      //パレットテーブル
      o += 40;
      for (int i = 0; i < paletCount; i++) {
        ByteArray.byaWil (bb, o, paletTable[i] & 0x00ffffff);
        o += 4;
      }
      //パターンデータ
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            continue;
          }
          if (pixelBits == 24) {  //パレットなし
            bb[o + 3 * x] = (byte) rgb;  //blue
            bb[o + 3 * x + 1] = (byte) (rgb >> 8);  //green
            bb[o + 3 * x + 2] = (byte) (rgb >> 16);  //red
            continue;
          }
          int l = 0;
          int r = paletCount;
          while (l < r) {
            int m = l + r >> 1;
            if (paletTable[m] < rgb) {
              l = m + 1;
            } else {
              r = m;
            }
          }
          if (l != 0) {
            if (pixelBits == 8) {
              bb[o + x] = (byte) l;
            } else if (pixelBits == 4) {
              bb[o + (x >> 1)] |= (byte) (l << ((~x & 1) << 2));
            } else if (pixelBits == 2) {
              bb[o + (x >> 2)] |= (byte) (l << ((~x & 3) << 1));
            } else {
              bb[o + (x >> 3)] |= (byte) (l << (~x & 7));
            }
          }
        }  //for x
        o += patternLineSize;
      }  //for y
      //マスクデータ
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            bb[o + (x >> 3)] |= (byte) (1 << (~x & 7));
          }
        }
        o += maskLineSize;
      }
    }  //for iconNumber
    return ismSave (bb, 0, (long) fileSize, fileName, false);
  }  //saveIcon(String,BufferedImage...)



  //========================================================================================
  //$$CPF コンポーネントファクトリー

  //取り外しできるリスナー
  public static boolean removableListenersAdded;  //addRemovableListener()で追加したリスナーが、true=追加されている,false=取り外されている
  public static HashMap<AbstractButton,ActionListener> cpfAbstractButtonToActionListener;
  public static HashMap<JComboBox<String>,ActionListener> cpfComboBoxStringToActionListener;
  public static HashMap<JSlider,ChangeListener> cpfSliderToChangeListener;
  public static HashMap<JSpinner,ChangeListener> cpfSpinnerToChangeListener;
  public static HashMap<ScrollList,ListSelectionListener> cpfScrollListToListSelectionListener;
  public static HashMap<Component,FocusListener> cpfComponentToFocusListener;
  public static HashMap<Component,KeyListener> cpfComponentToKeyListener;
  public static HashMap<Component,ComponentListener> cpfComponentToComponentListener;
  public static HashMap<Component,MouseListener> cpfComponentToMouseListener;
  public static HashMap<Component,MouseMotionListener> cpfComponentToMouseMotionListener;
  public static HashMap<Component,MouseWheelListener> cpfComponentToMouseWheelListener;
  public static HashMap<Window,WindowListener> cpfWindowToWindowListener;
  public static HashMap<Window,WindowStateListener> cpfWindowToWindowStateListener;
  public static HashMap<Window,WindowFocusListener> cpfWindowToWindowFocusListener;
  public static HashMap<JTextComponent,CaretListener> cpfJTextComponentToCaretListener;

  //初期化
  public static void cpfInit () {
    removableListenersAdded = true;
    cpfAbstractButtonToActionListener = new HashMap<AbstractButton,ActionListener> ();
    cpfComboBoxStringToActionListener = new HashMap<JComboBox<String>,ActionListener> ();
    cpfSliderToChangeListener = new HashMap<JSlider,ChangeListener> ();
    cpfSpinnerToChangeListener = new HashMap<JSpinner,ChangeListener> ();
    cpfScrollListToListSelectionListener = new HashMap<ScrollList,ListSelectionListener> ();
    cpfComponentToFocusListener = new HashMap<Component,FocusListener> ();
    cpfComponentToKeyListener = new HashMap<Component,KeyListener> ();
    cpfComponentToComponentListener = new HashMap<Component,ComponentListener> ();
    cpfComponentToMouseListener = new HashMap<Component,MouseListener> ();
    cpfComponentToMouseMotionListener = new HashMap<Component,MouseMotionListener> ();
    cpfComponentToMouseWheelListener = new HashMap<Component,MouseWheelListener> ();
    cpfWindowToWindowListener = new HashMap<Window,WindowListener> ();
    cpfWindowToWindowStateListener = new HashMap<Window,WindowStateListener> ();
    cpfWindowToWindowFocusListener = new HashMap<Window,WindowFocusListener> ();
    cpfJTextComponentToCaretListener = new HashMap<JTextComponent,CaretListener> ();
  }  //cpfInit()


  //--------------------------------------------------------------------------------
  //取り外しできるリスナー
  //  addRemovableListener()で追加する
  //  removeRemovableListeners()でまとめて取り外す
  //  addRemovableListeners()でまとめて追加し直す

  //button = addRemovableListener (button, listener)
  //  ボタンに取り外しできるアクションリスナーを追加する
  public static <T extends AbstractButton> T addRemovableListener (T button, ActionListener listener) {
    if (listener != null) {
      cpfAbstractButtonToActionListener.put (button, listener);
      if (removableListenersAdded) {
        button.addActionListener (listener);
      }
    }
    return button;
  }  //addRemovableListener(T extends AbstractButton,ActionListener)

  //comboBox = addRemovableListener (comboBox, listener)
  //  コンボボックスに取り外しできるアクションリスナーを追加する
  public static <T extends JComboBox<String>> T addRemovableListener (T comboBox, ActionListener listener) {
    if (listener != null) {
      cpfComboBoxStringToActionListener.put (comboBox, listener);
      if (removableListenersAdded) {
        comboBox.addActionListener (listener);
      }
    }
    return comboBox;
  }  //addRemovableListener(T extends JComboBox,ActionListener)

  //slider = addRemovableListener (slider, listener)
  //  スライダーに取り外しできるチェンジリスナーを追加する
  public static <T extends JSlider> T addRemovableListener (T slider, ChangeListener listener) {
    if (listener != null) {
      cpfSliderToChangeListener.put (slider, listener);
      if (removableListenersAdded) {
        slider.addChangeListener (listener);
      }
    }
    return slider;
  }  //addRemovableListener(T extends JSlider,ActionListener)

  //spinner = addRemovableListener (spinner, listener)
  //  スピナーに取り外しできるチェンジリスナーを追加する
  public static <T extends JSpinner> T addRemovableListener (T spinner, ChangeListener listener) {
    if (listener != null) {
      cpfSpinnerToChangeListener.put (spinner, listener);
      if (removableListenersAdded) {
        spinner.addChangeListener (listener);
      }
    }
    return spinner;
  }  //addRemovableListener(T extends JSpinner,ChangeListener)

  //scrollList = addRemovableListener (scrollList, listener)
  //  スクロールリストに取り外しできるリストセレクションリスナーを追加する
  public static <T extends ScrollList> T addRemovableListener (T scrollList, ListSelectionListener listener) {
    if (listener != null) {
      cpfScrollListToListSelectionListener.put (scrollList, listener);
      if (removableListenersAdded) {
        scrollList.addListSelectionListener (listener);
      }
    }
    return scrollList;
  }  //addRemovableListener(T extends ScrollList,ListSelectionListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるフォーカスリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, FocusListener listener) {
    if (listener != null) {
      cpfComponentToFocusListener.put (component, listener);
      if (removableListenersAdded) {
        component.addFocusListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,FocusListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるキーリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, KeyListener listener) {
    if (listener != null) {
      cpfComponentToKeyListener.put (component, listener);
      if (removableListenersAdded) {
        component.addKeyListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,KeyListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるコンポーネントリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, ComponentListener listener) {
    if (listener != null) {
      cpfComponentToComponentListener.put (component, listener);
      if (removableListenersAdded) {
        component.addComponentListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,ComponentListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるマウスリスナー、マウスモーションリスナー、マウスホイールリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, MouseAdapter listener) {
    if (listener != null) {
      cpfComponentToMouseListener.put (component, listener);
      cpfComponentToMouseMotionListener.put (component, listener);
      cpfComponentToMouseWheelListener.put (component, listener);
      if (removableListenersAdded) {
        component.addMouseListener (listener);
        component.addMouseMotionListener (listener);
        component.addMouseWheelListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,MouseAdapter)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるマウスリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, MouseListener listener) {
    if (listener != null) {
      cpfComponentToMouseListener.put (component, listener);
      if (removableListenersAdded) {
        component.addMouseListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,MouseListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるマウスモーションリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, MouseMotionListener listener) {
    if (listener != null) {
      cpfComponentToMouseMotionListener.put (component, listener);
      if (removableListenersAdded) {
        component.addMouseMotionListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,MouseMotionListener)

  //component = addRemovableListener (component, listener)
  //  コンポーネントに取り外しできるマウスホイールリスナーを追加する
  public static <T extends Component> T addRemovableListener (T component, MouseWheelListener listener) {
    if (listener != null) {
      cpfComponentToMouseWheelListener.put (component, listener);
      if (removableListenersAdded) {
        component.addMouseWheelListener (listener);
      }
    }
    return component;
  }  //addRemovableListener(T extends Component,MouseWheelListener)

  //window = addRemovableListener (window, listener)
  //  ウインドウに取り外しできるウインドウリスナー、ウインドウステートリスナー、ウインドウフォーカスリスナーを追加する
  public static <T extends Window> T addRemovableListener (T window, WindowAdapter listener) {
    if (listener != null) {
      cpfWindowToWindowListener.put (window, listener);
      cpfWindowToWindowStateListener.put (window, listener);
      cpfWindowToWindowFocusListener.put (window, listener);
      if (removableListenersAdded) {
        window.addWindowListener (listener);
        window.addWindowStateListener (listener);
        window.addWindowFocusListener (listener);
      }
    }
    return window;
  }  //addRemovableListener(T extends Window,WindowAdapter)

  //window = addRemovableListener (window, listener)
  //  ウインドウに取り外しできるウインドウリスナーを追加する
  public static <T extends Window> T addRemovableListener (T window, WindowListener listener) {
    if (listener != null) {
      cpfWindowToWindowListener.put (window, listener);
      if (removableListenersAdded) {
        window.addWindowListener (listener);
      }
    }
    return window;
  }  //addRemovableListener(T extends Window,WindowListener)

  //window = addRemovableListener (window, listener)
  //  ウインドウに取り外しできるウインドウステートリスナーを追加する
  public static <T extends Window> T addRemovableListener (T window, WindowStateListener listener) {
    if (listener != null) {
      cpfWindowToWindowStateListener.put (window, listener);
      if (removableListenersAdded) {
        window.addWindowStateListener (listener);
      }
    }
    return window;
  }  //addRemovableListener(T extends Window,WindowStateListener)

  //window = addRemovableListener (window, listener)
  //  ウインドウに取り外しできるウインドウフォーカスリスナーを追加する
  public static <T extends Window> T addRemovableListener (T window, WindowFocusListener listener) {
    if (listener != null) {
      cpfWindowToWindowFocusListener.put (window, listener);
      if (removableListenersAdded) {
        window.addWindowFocusListener (listener);
      }
    }
    return window;
  }  //addRemovableListener(T extends Window,WindowFocusListener)

  //textComponent = addRemovableListener (textComponent, listener)
  //  テキストコンポーネントに取り外しできるキャレットリスナーを追加する
  public static <T extends JTextComponent> T addRemovableListener (T textComponent, CaretListener listener) {
    if (listener != null) {
      cpfJTextComponentToCaretListener.put (textComponent, listener);
      if (removableListenersAdded) {
        textComponent.addCaretListener (listener);
      }
    }
    return textComponent;
  }  //addRemovableListener(T extends JTextComponent,CaretListener)

  //removeRemovableListers ()
  //  addRemovableListener()で追加した取り外しできるリスナーをまとめて取り外す
  public static void removeRemovableListers () {
    if (removableListenersAdded) {
      removableListenersAdded = false;
      cpfAbstractButtonToActionListener.forEach ((k, v) -> k.removeActionListener (v));
      cpfComboBoxStringToActionListener.forEach ((k, v) -> k.removeActionListener (v));
      cpfSliderToChangeListener.forEach ((k, v) -> k.removeChangeListener (v));
      cpfSpinnerToChangeListener.forEach ((k, v) -> k.removeChangeListener (v));
      cpfScrollListToListSelectionListener.forEach ((k, v) -> k.removeListSelectionListener (v));
      cpfComponentToFocusListener.forEach ((k, v) -> k.removeFocusListener (v));
      cpfComponentToKeyListener.forEach ((k, v) -> k.removeKeyListener (v));
    }
  }  //removeRemovableListers()

  //addRemovableListers ()
  //  addRemovableListener()で追加した取り外しできるリスナーをまとめて追加し直す
  public static void addRemovableListers () {
    if (!removableListenersAdded) {
      removableListenersAdded = true;
      cpfAbstractButtonToActionListener.forEach ((k, v) -> k.addActionListener (v));
      cpfComboBoxStringToActionListener.forEach ((k, v) -> k.addActionListener (v));
      cpfSliderToChangeListener.forEach ((k, v) -> k.addChangeListener (v));
      cpfSpinnerToChangeListener.forEach ((k, v) -> k.addChangeListener (v));
      cpfScrollListToListSelectionListener.forEach ((k, v) -> k.addListSelectionListener (v));
      cpfComponentToFocusListener.forEach ((k, v) -> k.addFocusListener (v));
      cpfComponentToKeyListener.forEach ((k, v) -> k.addKeyListener (v));
    }
  }  //addRemovableListers()


  //--------------------------------------------------------------------------------
  //コンポーネントに属性を追加する
  //  ジェネリクスを用いてパラメータのコンポーネントをクラスを変えずにそのまま返すメソッドを定義する
  //  コンポーネントのインスタンスメソッドがコンポーネント自身を返してくれると、
  //  メソッドチェーンが組めて括弧をネストする必要がなくなりコードが読みやすくなるのだが、
  //  元のクラスのまま利用できなければ既存のメソッドの返却値をいちいちキャストしなければならず、
  //  結局括弧が増えることになる

  //component = setToolTipText (component, toolTipText)
  public static <T extends JComponent> T setToolTipText (T component, String toolTipText) {
    component.setToolTipText (toolTipText);
    return component;
  }  //setToolTipText(T extends JComponent,String)

  //component = setName (component, name)
  public static <T extends Component> T setName (T component, String name) {
    component.setName (name);
    return component;
  }  //setName(T extends Component,String)

  //component = setVisible (component, visible)
  public static <T extends Component> T setVisible (T component, boolean visible) {
    component.setVisible (visible);
    return component;
  }  //setVisible(T extends Component,boolean)

  //component = setColor (component, foreground, background)
  public static <T extends Component> T setColor (T component, Color foreground, Color background) {
    component.setBackground (background);
    component.setForeground (foreground);
    return component;
  }  //setColor(T extends Component,Color,Color)

  //component = setFont (component, font)
  public static <T extends Component> T setFont (T component, Font font) {
    component.setFont (font);
    return component;
  }  //setFont(T extends Component,Font)

  //component = bold (component)
  //component = italic (component)
  //component = boldItalic (component)
  public static <T extends Component> T bold (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.BOLD));
  }  //bold(T extends Component)
  public static <T extends Component> T italic (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.ITALIC));
  }  //italic(T extends Component)
  public static <T extends Component> T boldItalic (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.BOLD | Font.ITALIC));
  }  //boldItalic(T extends Component)

  //component = setEnabled (component, enabled)
  //  コンポーネントが有効かどうか指定する
  public static <T extends Component> T setEnabled (T component, boolean enabled) {
    component.setEnabled (enabled);
    return component;
  }  //setEnabled(T extends Component,boolean)

  //component = setMaximumSize (component, width, height)
  //  コンポーネントの最大サイズを指定する
  public static <T extends Component> T setMaximumSize (T component, int width, int height) {
    component.setMaximumSize (new Dimension (width, height));
    return component;
  }  //setMaximumSize(T extends Component,int,int)

  //component = setMinimumSize (component, width, height)
  //  コンポーネントの最小サイズを指定する
  public static <T extends Component> T setMinimumSize (T component, int width, int height) {
    component.setMinimumSize (new Dimension (width, height));
    return component;
  }  //setMinimumSize(T extends Component,int,int)

  //component = setPreferredSize (component, width, height)
  //  コンポーネントの推奨サイズを指定する
  public static <T extends Component> T setPreferredSize (T component, int width, int height) {
    component.setPreferredSize (new Dimension (width, height));
    return component;
  }  //setPreferredSize(T extends Component,int,int)

  //component = setFixedSize (component, width, height)
  //  コンポーネントの固定サイズを指定する
  public static <T extends Component> T setFixedSize (T component, int width, int height) {
    Dimension d = new Dimension (width, height);
    component.setMinimumSize (d);
    component.setMaximumSize (d);
    component.setPreferredSize (d);
    return component;
  }  //setFixedSize(T extends Component,int,int)

  //component = setEmptyBorder (component, top, left, bottom, right)
  //  コンポーネントに透過ボーダーを付ける
  public static <T extends JComponent> T setEmptyBorder (T component, int top, int left, int bottom, int right) {
    component.setBorder (new EmptyBorder (top, left, bottom, right));
    return component;
  }  //setEmptyBorder(T extends JComponent,int,int,int,int)

  //component = setTitledLineBorder (component, title)
  //  コンポーネントにタイトル付きラインボーダーを付ける
  public static <T extends JComponent> T setTitledLineBorder (T component, String title) {
    component.setBorder (new TitledBorder (new LineBorder (new Color (LnF.LNF_RGB[10]), 1), title));
    return component;
  }  //setTitledLineBorder(T extends JComponent,String)

  //component = setTitledEtchedBorder (component, title)
  //  コンポーネントにタイトル付きエッチングボーダーを付ける
  public static <T extends JComponent> T setTitledEtchedBorder (T component, String title) {
    component.setBorder (new TitledBorder (new EtchedBorder (), title));
    return component;
  }  //setTitledEtchedBorder(T extends JComponent,String)

  //parent = addComponents (parent, component, ...)
  //  コンポーネントにコンポーネントを追加する
  public static <T extends JComponent> T addComponents (T parent, Component... components) {
    for (Component component : components) {
      if (component != null) {
        parent.add (component);
      }
    }
    return parent;
  }  //addComponents(T extends JComponent,Component...)

  //parent = removeComponents (parent, child, ...)
  //  コンポーネントからコンポーネントを取り除く
  public static <T extends JComponent> T removeComponents (T parent, Component... components) {
    for (Component component : components) {
      if (component != null) {
        parent.remove (component);
      }
    }
    return parent;
  }  //removeComponents(T extends JComponent,Component...)

  //ボタン

  //button = setText (button, text)
  public static <T extends AbstractButton> T setText (T button, String text) {
    button.setText (text);
    return button;
  }  //setText(T extends AbstractButton,String)

  //button = setHorizontalAlignment (button, alignment)
  //  SwingConstants.RIGHT
  //  SwingConstants.LEFT
  //  SwingConstants.CENTER (デフォルト)
  //  SwingConstants.LEADING
  //  SwingConstants.TRAILING
  public static <T extends AbstractButton> T setHorizontalAlignment (T button, int alignment) {
    button.setHorizontalAlignment (alignment);
    return button;
  }  //setHorizontalAlignment(T extends AbstractButton,int)

  //button = setVerticalAlignment (button, alignment)
  //  SwingConstants.CENTER (デフォルト)
  //  SwingConstants.TOP
  //  SwingConstants.BOTTOM
  public static <T extends AbstractButton> T setVerticalAlignment (T button, int alignment) {
    button.setVerticalAlignment (alignment);
    return button;
  }  //setVerticalAlignment(T extends AbstractButton,int)

  //テキストフィールド

  //button = setHorizontalAlignment (textField, alignment)
  //  JTextField.RIGHT
  //  JTextField.LEFT (デフォルト)
  //  JTextField.CENTER
  //  JTextField.LEADING
  //  JTextField.TRAILING
  public static <T extends JTextField> T setHorizontalAlignment (T textField, int alignment) {
    textField.setHorizontalAlignment (alignment);
    return textField;
  }  //setHorizontalAlignment(T extends JTextField,int)

  //テキストコンポーネント

  //component = setEditable (component, enabled)
  //  コンポーネントが編集可能かどうか指定する
  public static <T extends JTextComponent> T setEditable (T component, boolean enabled) {
    component.setEditable (enabled);
    return component;
  }  //setEditable(T extends JTextComponent,boolean)


  //--------------------------------------------------------------------------------
  //フレームとダイアログを作る
  //  ウインドウリスナー
  //    ウインドウを開いたとき  activated,opened
  //    フォーカスを失ったとき  deactivated
  //    フォーカスを取得したとき  activated
  //    ウインドウをアイコン化したとき  iconified,[deactivated]
  //    ウインドウを元のサイズに戻したとき  deiconified,activated
  //    ウインドウを閉じたとき  closing,[deactivated],closed

  //frame = createFrame (title, mnbMenuBar, component)
  //  フレームを作る
  //  すぐに開く
  //  デフォルトのクローズボタンの動作はEXIT_ON_CLOSE
  //  クローズボタンがクリックされたとき後始末を行なってから終了するとき
  //    frame = createFrame (title, mnbMenuBar, component);
  //    frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
  //    frame.addWindowListener (new WindowAdapter () {
  //      @Override public void windowClosed (WindowEvent we) {
  //        後始末;
  //        System.exit (0);
  //      }
  //    });
  public static JFrame createFrame (String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new JFrame (title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    frame.setVisible (true);
    return frame;
  }  //createFrame(String,JMenuBar,JComponent)

  public static JFrame createSubFrame (String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new JFrame (title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.HIDE_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    return frame;
  }  //createSubFrame(String,JMenuBar,JComponent)

  public static JFrame createRestorableFrame (String key, String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new RestorableFrame (key, title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    frame.setVisible (true);
    return frame;
  }  //createRestorableFrame(String,String,JMenuBar,JComponent)

  public static JFrame createRestorableSubFrame (String key, String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new RestorableFrame (key, title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.HIDE_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    return frame;
  }  //createRestorableSubFrame(String,String,JMenuBar,JComponent)

  //dialog = createModalDialog (owner, title, component)
  //dialog = createModelessDialog (owner, title, component)
  //dialog = createDialog (owner, title, component, modal)
  //  ダイアログを作る(ownerを指定する)
  //  まだ開かない
  //  デフォルトのクローズボタンの動作はHIDE_ON_CLOSE
  //  閉じてもdialog.setVisible(true)で再表示できる
  public static JDialog createModalDialog (Frame owner, String title, JComponent component) {
    return createDialog (owner, title, true, component);
  }  //createModalDialog(Frame,String,JComponent)
  public static JDialog createModelessDialog (Frame owner, String title, JComponent component) {
    return createDialog (owner, title, false, component);
  }  //createModelessDialog(Frame,String,JComponent)
  public static JDialog createDialog (Frame owner, String title, boolean modal, JComponent component) {
    JDialog dialog = new JDialog (owner, title, modal);
    dialog.setUndecorated (true);  //ウインドウの枠を消す
    dialog.getRootPane ().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    dialog.setAlwaysOnTop (modal);  //モーダルのときは常に手前に表示、モードレスのときは奥に移動できる
    dialog.setLocationByPlatform (true);
    dialog.setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
    dialog.getContentPane ().add (component, BorderLayout.CENTER);
    dialog.pack ();
    dialog.setVisible (false);
    return dialog;
  }  //createDialog(Frame,String,boolean,JComponent)


  //--------------------------------------------------------------------------------
  //パネルを作る

  //box = createHorizontalBox (component, ...)
  //  コンポーネントを横に並べるボックスを作る
  public static Box createHorizontalBox (Component... components) {
    return addComponents (Box.createHorizontalBox (), components);
  }  //createHorizontalBox(Component...)

  //box = createVerticalBox (component, ...)
  //  コンポーネントを縦に並べるボックスを作る
  public static Box createVerticalBox (Component... components) {
    return addComponents (Box.createVerticalBox (), components);
  }  //createVerticalBox(Component...)

  //box = createGlueBox (component)
  //box = createGlueBox (orientation, component)
  //  コンポーネントを引き伸ばさず指定された方向に寄せて表示する
  //  component自身がmaximumSizeを持っていること
  //  componentがBorderLayoutでCENTERがmaximumSizeを持っているのではダメ
  //  orientation
  //    SwingConstants.NORTH_WEST SwingConstants.NORTH  SwingConstants.NORTH_EAST
  //    SwingConstants.WEST       SwingConstants.CENTER SwingConstants.EAST
  //    SwingConstants.SOUTH_WEST SwingConstants.SOUTH  SwingConstants.SOUTH_EAST
  public static Box createGlueBox (JComponent component) {
    return createGlueBox (SwingConstants.CENTER, component);
  }  //createGlueBox(JComponent)
  public static Box createGlueBox (int orientation, JComponent component) {
    Box box = (orientation == SwingConstants.NORTH_WEST ||
               orientation == SwingConstants.WEST ||
               orientation == SwingConstants.SOUTH_WEST ?
               createHorizontalBox (component, Box.createHorizontalGlue ()) :
               orientation == SwingConstants.NORTH_EAST ||
               orientation == SwingConstants.EAST ||
               orientation == SwingConstants.SOUTH_EAST ?
               createHorizontalBox (Box.createHorizontalGlue (), component) :
               createHorizontalBox (Box.createHorizontalGlue (), component, Box.createHorizontalGlue ()));
    return (orientation == SwingConstants.NORTH_WEST ||
            orientation == SwingConstants.NORTH ||
            orientation == SwingConstants.NORTH_EAST ?
            createVerticalBox (box, Box.createVerticalGlue ()) :
            orientation == SwingConstants.SOUTH_WEST ||
            orientation == SwingConstants.SOUTH ||
            orientation == SwingConstants.SOUTH_EAST ?
            createVerticalBox (Box.createVerticalGlue (), box) :
            createVerticalBox (Box.createVerticalGlue (), box, Box.createVerticalGlue ()));
  }  //createGlueBox(int,JComponent)

  //panel = createFlowPanel (component, ...)
  //panel = createFlowPanel (align, component, ...)
  //panel = createFlowPanel (hgap ,vgap, component, ...)
  //panel = createFlowPanel (align, hgap, vgap, component, ...)
  //  FlowLayoutのパネルを作る
  //  align
  //    FlowLayout.CENTER  中央揃え
  //    FlowLayout.LEFT    左揃え
  //    FlowLayout.RIGHT   右揃え
  public static JPanel createFlowPanel (Component... components) {
    return createFlowPanel (FlowLayout.LEFT, 0, 0, components);
  }  //createFlowPanel(Component...)
  public static JPanel createFlowPanel (int align, Component... components) {
    return createFlowPanel (align, 0, 0, components);
  }  //createFlowPanel(int,Component...)
  public static JPanel createFlowPanel (int hgap, int vgap, Component... components) {
    return createFlowPanel (FlowLayout.LEFT, hgap, vgap, components);
  }  //createFlowPanel(int,int,Component...)
  public static JPanel createFlowPanel (int align, int hgap, int vgap, Component... components) {
    JPanel panel = new JPanel (new FlowLayout (align, hgap, vgap));
    panel.setOpaque (true);
    return addComponents (panel, components);
  }  //createFlowPanel(int,int,int,Component...)

  //panel = createBorderPanel (component, ...)
  //panel = createBorderPanel (hgap, vgap, component, ...)
  //  BorderLayoutのパネルを作る
  //  コンポーネントをCENTER,NORTH,WEST,SOUTH,EASTの順序で指定する
  //  末尾のコンポーネントを省略するか途中のコンポーネントにnullを指定するとその部分は設定されない
  public static JPanel createBorderPanel (JComponent... components) {
    return createBorderPanel (0, 0, components);
  }  //createBorderPanel(JComponent...)
  public static JPanel createBorderPanel (int hgap, int vgap, JComponent... components) {
    JPanel panel = new JPanel (new BorderLayout (hgap, vgap));
    panel.setOpaque (true);
    if (components.length >= 1) {
      if (components[0] != null) {
        panel.add (components[0], BorderLayout.CENTER);
      }
      if (components.length >= 2) {
        if (components[1] != null) {
          panel.add (components[1], BorderLayout.NORTH);
        }
        if (components.length >= 3) {
          if (components[2] != null) {
            panel.add (components[2], BorderLayout.WEST);
          }
          if (components.length >= 4) {
            if (components[3] != null) {
              panel.add (components[3], BorderLayout.SOUTH);
            }
            if (components.length >= 5 && components[4] != null) {
              panel.add (components[4], BorderLayout.EAST);
            }
          }
        }
      }
    }
    return panel;
  }  //createBorderPanel(int,int,JComponent...)

  //panel = createGridPanel (colCount, rowCount, gridStyles, colStyless, rowStyless, cellStyless, objectArray, ...)
  //  GridBagLayoutのパネルを作る
  //    colCount          列数
  //    rowCount          行数
  //    gridStyles        すべてのセルの共通のスタイル
  //    colStyles         列毎の共通のスタイル。列の区切りは";"。スタイルの区切りは","
  //    rowStyles         行毎の共通のスタイル。行の区切りは";"。スタイルの区切りは","
  //    cellStyles        個々のセルのスタイル。セルの区切りは";"。スタイルの区切りは","。上または左のセルが重なっているセルは含まない
  //                      colSpan        列数
  //                      rowSpan        行数
  //                      width          幅
  //                      height         高さ
  //                      widen          幅をいっぱいまで伸ばす
  //                      lengthen       高さをいっぱいまで伸ばす
  //                      center         左右に寄せない
  //                      left           左に寄せる
  //                      right          右に寄せる
  //                      middle         上下に寄せない
  //                      top            上に寄せる
  //                      bottom         下に寄せる
  //                      paddingTop     上端のパディング
  //                      paddingRight   右端のパディング
  //                      paddingBottom  下端のパディング
  //                      paddingLeft    左端のパディング
  //                      bold           ボールド
  //                      italic         イタリック
  //    objectArray, ...  セルに表示するオブジェクトの配列。長さが列数×行数よりも少ないときは上または左のセルが重なっているセルが詰められていると判断される。このときcellStylesも詰められたインデックスで参照される
  //  gridStyles;colStyles;rowStyles;cellStylesの順序で個々のセルに対応するスタイルが連結される
  //  同時に指定できないスタイルは後から指定した方が優先される
  public static JPanel createGridPanel (int colCount, int rowCount, String gridStyles, String colStyless, String rowStyless, String cellStyless, Object... objectArray) {
    String[] colStylesArray = (colStyless != null ? colStyless : "").split (";");
    String[] rowStylesArray = (rowStyless != null ? rowStyless : "").split (";");
    String[] cellStylesArray = (cellStyless != null ? cellStyless : "").split (";");
    int cellCount = colCount * rowCount;
    //Component[] componentArray = new Component[cellCount];  //セルのオブジェクト。上または左のセルが重なっているセルは含まない
    boolean[] cellFilledArray = new boolean[cellCount];  //セルが充填済みかどうか。colCount*rowCountのセルをすべて含む
    GridBagLayout gridbag = new GridBagLayout ();
    JPanel panel = new JPanel (gridbag);
    GridBagConstraints c = new GridBagConstraints ();
    int objectIndex = 0;  //objectArrayとcellStylesArrayの詰められたインデックス
    boolean objectClosed = objectArray.length < cellCount;  //objectArrayとcellStylesArrayが詰められている(上または左のセルが重なっているセルを含まない)。objectArray[objectClosed?objectIndex:cellIndex]
    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      for (int colIndex = 0; colIndex < colCount; colIndex++) {
        int cellIndex = colIndex + colCount * rowIndex;  //セルのインデックス。colCount*rowCountのセルをすべて含む
        if (cellFilledArray[cellIndex]) {  //充填済み
          continue;
        }
        int colSpan = 1;
        int rowSpan = 1;
        int width = 1;
        int height = 1;
        int fill = 0;  //1=widen,2=lengthen
        int anchor = 0;  //1=left,2=right,4=top,8=bottom
        int paddingTop = 0;
        int paddingRight = 0;
        int paddingBottom = 0;
        int paddingLeft = 0;
        int fontStyle = 0;  //1=bold,2=italic
        for (String style : ((gridStyles != null ? gridStyles : "") + "," +
                             (colIndex < colStylesArray.length ? colStylesArray[colIndex] : "") + "," +
                             (rowIndex < rowStylesArray.length ? rowStylesArray[rowIndex] : "") + "," +
                             ((objectClosed ? objectIndex : cellIndex) < cellStylesArray.length ? cellStylesArray[objectClosed ? objectIndex : cellIndex] : "")).split (",")) {
          String[] keyValue = style.split ("=");
          String key = keyValue.length < 1 ? "" : keyValue[0].trim ();
          int value = keyValue.length < 2 ? 1 : Integer.parseInt (keyValue[1]);
          switch (key) {
          case "colSpan":  //列数
            colSpan = value;
            break;
          case "rowSpan":  //行数
            rowSpan = value;
            break;
          case "width":  //幅
            width = value;
            break;
          case "height":  //高さ
            height = value;
            break;
          case "widen":  //幅をいっぱいまで伸ばす
            fill |= 1;
            break;
          case "lengthen":  //高さをいっぱいまで伸ばす
            fill |= 2;
            break;
          case "center":  //左右に寄せない
            anchor &= ~0b0011;
            break;
          case "left":  //左に寄せる
            anchor = anchor & ~0b0011 | 0b0001;
            break;
          case "right":  //右に寄せる
            anchor = anchor & ~0b0011 | 0b0010;
            break;
          case "middle":  //上下に寄せない
            anchor &= ~0b1100;
            break;
          case "top":  //上に寄せる
            anchor = anchor & ~0b1100 | 0b0100;
            break;
          case "bottom":  //下に寄せる
            anchor = anchor & ~0b1100 | 0b1000;
            break;
          case "paddingTop":  //上端のパディング
            paddingTop = value;
            break;
          case "paddingRight":  //右端のパディング
            paddingRight = value;
            break;
          case "paddingBottom":  //下端のパディング
            paddingBottom = value;
            break;
          case "paddingLeft":  //左端のパディング
            paddingLeft = value;
            break;
          case "bold":  //ボールド
            fontStyle |= 1;
            break;
          case "italic":  //イタリック
            fontStyle |= 2;
            break;
          }
        }
        c.gridx = colIndex;
        c.gridy = rowIndex;
        c.gridwidth = colSpan;
        c.gridheight = rowSpan;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = (fill == 1 ? GridBagConstraints.HORIZONTAL :
                  fill == 2 ? GridBagConstraints.VERTICAL :
                  fill == 3 ? GridBagConstraints.BOTH :
                  GridBagConstraints.NONE);
        c.anchor = (anchor == 0b0001 ? GridBagConstraints.WEST :
                    anchor == 0b0010 ? GridBagConstraints.EAST :
                    anchor == 0b0100 ? GridBagConstraints.NORTH :
                    anchor == 0b1000 ? GridBagConstraints.SOUTH :
                    anchor == 0b0101 ? GridBagConstraints.NORTHWEST :
                    anchor == 0b0110 ? GridBagConstraints.NORTHEAST :
                    anchor == 0b1001 ? GridBagConstraints.SOUTHWEST :
                    anchor == 0b1010 ? GridBagConstraints.SOUTHEAST :
                    GridBagConstraints.CENTER);
        c.insets = new Insets (paddingTop, paddingLeft, paddingBottom, paddingRight);
        Object object = (objectClosed ? objectIndex : cellIndex) < objectArray.length ? objectArray[objectClosed ? objectIndex : cellIndex] : null;
        Component component;
        if (object == null) {
          component = new JPanel ();
        } else if (object instanceof String) {
          String string = (String) object;
          component = string.startsWith ("http://") ? createAnchor (string, string) : createLabel ((String) object);
        } else if (object instanceof Component) {
          component = (Component) object;
        } else {
          component = new JPanel ();
        }
        if (component instanceof JLabel) {
          JLabel label = (JLabel) component;
          if (fontStyle == 1) {
            bold (label);
          } else if (fontStyle == 2) {
            italic (label);
          } else if (fontStyle == 3) {
            boldItalic (label);
          }
        }

        component.setMinimumSize (new Dimension (width, height));
        if (width > 1 || height > 1) {
          component.setPreferredSize (new Dimension (width, height));
        }
        gridbag.setConstraints (component, c);
        panel.add (component);
        //componentArray[objectIndex] = component;
        for (int y = 0; y < rowSpan; y++) {
          for (int x = 0; x < colSpan; x++) {
            cellFilledArray[(colIndex + x) + colCount * (rowIndex + y)] = true;
          }
        }

        objectIndex++;
      }  //for colIndex
    }  //for rowIndex
    return panel;
  }  //createGridPanel(int,int,String,String,String,String,Object...)

  //scrollPane = createScrollPane (view)
  //scrollPane = createScrollPane (view, vsbPolicy, hsbPolicy)
  //  スクロールペインを作る
  //  推奨サイズが必要なので通常はsetPreferredSize (createScrollPane (view), width, height)の形式で作る
  //  vsbPolicy
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
  //  hsbPolicy
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
  public static JScrollPane createScrollPane (Component view) {
    return createScrollPane (view,
                             ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }  //createScrollPane(Component)
  public static JScrollPane createScrollPane (Component view, int vsbPolicy, int hsbPolicy) {
    return new JScrollPane (view, vsbPolicy, hsbPolicy);
  }  //createScrollPane(Component,int,int)

  //splitPane = createHorizontalSplitPane (component, ...)
  //splitPane = createVerticalSplitPane (component, ...)
  //splitPane = createSplitPane (orientation, component, ...)
  //  スプリットペインを作る
  //  orientation
  //    JSplitPane.HORIZONTAL_SPLIT
  //    JSplitPane.VERTICAL_SPLIT
  public static JSplitPane createHorizontalSplitPane (Component... components) {
    return createSplitPane (JSplitPane.HORIZONTAL_SPLIT, components);
  }  //createHorizontalSplitPane(Component...)
  public static JSplitPane createVerticalSplitPane (Component... components) {
    return createSplitPane (JSplitPane.VERTICAL_SPLIT, components);
  }  //createVerticalSplitPane(Component...)
  public static JSplitPane createSplitPane (int orientation, Component... components) {
    JSplitPane splitPane = new JSplitPane (orientation, true, components[0], components[1]);
    for (int i = 2; i < components.length; i++) {
      splitPane = new JSplitPane (orientation, true, splitPane, components[i]);  //((0,1),2)...
    }
    return splitPane;
  }  //createSplitPane(int,Component...)


  //--------------------------------------------------------------------------------
  //セパレータを作る

  //separator = createHorizontalSeparator ()
  //separator = createVerticalSeparator ()
  //  セパレータを作る
  public static JSeparator createHorizontalSeparator () {
    return new JSeparator (SwingConstants.HORIZONTAL);
  }  //createHorizontalSeparator()
  public static JSeparator createVerticalSeparator () {
    return new JSeparator (SwingConstants.VERTICAL);
  }  //createVerticalSeparator()


  //--------------------------------------------------------------------------------
  //ラベルを作る

  //label = createLabel (enText)
  //label = createLabel (enText, alignment)
  //  ラベルを作る
  public static JLabel createLabel (String enText) {
    return createLabel (enText, SwingConstants.CENTER);
  }  //createLabel(String)
  public static JLabel createLabel (String enText, int alignment) {
    JLabel label = new JLabel (enText);
    label.setForeground (new Color (LnF.LNF_RGB[14]));
    if (alignment == SwingConstants.NORTH_WEST ||
        alignment == SwingConstants.NORTH ||
        alignment == SwingConstants.NORTH_EAST ||
        alignment == SwingConstants.TOP) {
      label.setVerticalAlignment (SwingConstants.TOP);
    } else if (alignment == SwingConstants.SOUTH_WEST ||
        alignment == SwingConstants.SOUTH ||
        alignment == SwingConstants.SOUTH_EAST ||
        alignment == SwingConstants.BOTTOM) {
      label.setVerticalAlignment (SwingConstants.BOTTOM);
    } else if (alignment == SwingConstants.CENTER) {
      label.setVerticalAlignment (SwingConstants.CENTER);
    }
    if (alignment == SwingConstants.NORTH_WEST ||
        alignment == SwingConstants.WEST ||
        alignment == SwingConstants.SOUTH_WEST ||
        alignment == SwingConstants.LEFT) {
      label.setHorizontalAlignment (SwingConstants.LEFT);
    } else if (alignment == SwingConstants.NORTH_EAST ||
        alignment == SwingConstants.EAST ||
        alignment == SwingConstants.SOUTH_EAST ||
        alignment == SwingConstants.RIGHT) {
      label.setHorizontalAlignment (SwingConstants.RIGHT);
    } else if (alignment == SwingConstants.CENTER) {
      label.setHorizontalAlignment (SwingConstants.CENTER);
    }
    return label;
  }  //createLabel(String,int)

  //label = createIconLabel (image)
  //  アイコンラベルを作る
  public static JLabel createIconLabel (Image image) {
    JLabel label = new JLabel (new ImageIcon (image));
    label.setBorder (new EmptyBorder (1, 1, 1, 1));  //アイコンボタンと同じサイズにする
    return label;
  }  //createIconLabel(Image)


  //--------------------------------------------------------------------------------
  //アンカーを作る
  //  下線付きラベル
  //  マウスカーソルは手の形
  //  クリックされたらあらかじめ設定されたURIをブラウザに渡す

  //label = createAnchor (enText, uri)
  //  アンカーを作る
  public static boolean isObsoleteURI (String uri) {
    return uri.startsWith ("http://www.nifty.ne.jp/forum/");  //"fsharp/"。リンク先が存在しないURI
  }  //isObsoleteURI(String)
  public static JLabel createAnchor (String enText, String uri) {
    JLabel label = new UnderlinedLabel (enText);  //下線付きラベル
    label.setForeground (new Color (LnF.LNF_RGB[14]));
    if (uri != null) {
      if (isObsoleteURI (uri)) {
        uri = "http://web.archive.org/web/*/" + uri;
      }
      label.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));  //マウスカーソルは手の形
      label.setToolTipText (uri);
      label.addMouseListener (new AnchorAdapter (uri));  //クリックされたらあらかじめ設定されたURIをブラウザに渡す
    }
    return label;
  }  //createAnchor(String,String)


  //--------------------------------------------------------------------------------
  //テキストフィールドを作る

  //textField = createTextField (text, columns)
  //  テキストフィールドを作る
  public static JTextField createTextField (String text, int columns) {
    return new JTextField (text, columns);
  }  //createTextField(String,int)

  //textField = createNumberField (text, columns)
  //  数値入力用のテキストフィールドを作る
  public static JTextField createNumberField (String text, int columns) {
    return setHorizontalAlignment (
      setFixedSize (
        setFont (
          new JTextField (text),  //columnを指定すると幅を調節できなくなる
          new Font ("Monospaced", Font.PLAIN, 12)),
        10 + 6 * columns, 16),
      JTextField.RIGHT);
  }  //createNumberField(int,int)


  //--------------------------------------------------------------------------------
  //スクロールテキストエリアを作る

  //scrollTextArea = createScrollTextArea (text, width, height)
  //scrollTextArea = createScrollTextArea (text, width, height, editable)
  //  スクロールテキストエリアを作る
  public static ScrollTextArea createScrollTextArea (String text, int width, int height) {
    return createScrollTextArea (text, width, height, false);
  }  //createScrollTextArea(String,int,int)
  public static ScrollTextArea createScrollTextArea (String text, int width, int height, boolean editable) {
    ScrollTextArea scrollTextArea = setPreferredSize (
      setFont (new ScrollTextArea (), new Font ("Monospaced", Font.PLAIN, 12)),
      width, height);
    setEmptyBorder (scrollTextArea, 0, 0, 0, 0);
    scrollTextArea.setMargin (new Insets (2, 4, 2, 4));  //グリッドを更新させるためJTextAreaではなくScrollTextAreaに設定する必要がある
    JTextArea textArea = scrollTextArea.getTextArea ();
    textArea.setEditable (editable);
    scrollTextArea.setText (text);
    scrollTextArea.setCaretPosition (0);
    return scrollTextArea;
  }  //createScrollTextArea(String,int,int,boolean)


  //--------------------------------------------------------------------------------
  //スクロールテキストペインを作る

  //scrollTextPane = createScrollTextPane (text, width, height)
  //  スクロールテキストペインを作る
  //  http://～の部分がハイパーリンクになる
  //    許諾条件.txtの中に"(http://www.nifty.ne.jp/forum/fsharp/)"という部分がある
  //    ')'はURIに使える文字なので正しい方法では分離することができない
  //    ここではhttp://の直前に'('があるときは')'をURIに使えない文字とみなすことにする
  public static JScrollPane createScrollTextPane (String text, int width, int height) {
    JTextPane textPane = new JTextPane ();
    StyledDocument document = textPane.getStyledDocument ();
    Style defaultStyle = document.addStyle ("default", StyleContext.getDefaultStyleContext ().getStyle (StyleContext.DEFAULT_STYLE));
    int anchorNumber = 0;
    //  http://user:passwd@host:port/path?query#hash → http://host/path?query
    //Matcher matcher = Pattern.compile ("\\bhttp://[-.0-9A-Za-z]*(?:/(?:[!$&-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?").matcher (text);
    Matcher matcher = Pattern.compile ("\\b" +
                                       "(?:" +
                                       "(?<!\\()http://[-.0-9A-Za-z]*(?:/(?:[!$&-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?" +
                                       "|" +
                                       "(?<=\\()http://[-.0-9A-Za-z]*(?:/(?:[!$&-(*-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?" +
                                       ")").matcher (text);
    try {
      int start = 0;
      while (matcher.find ()) {
        int end = matcher.start ();  //ハイパーリンクの開始位置
        if (start < end) {
          document.insertString (document.getLength (), text.substring (start, end), defaultStyle);  //ハイパーリンクの手前のテキスト
        }
        String anchorHref = matcher.group ();  //ハイパーリンク
        Style anchorStyle = document.addStyle ("anchor" + anchorNumber++, defaultStyle);
        JLabel anchorLabel = createAnchor (anchorHref, anchorHref);
        Dimension anchorSize = anchorLabel.getPreferredSize ();
        anchorLabel.setAlignmentY ((float) anchorLabel.getBaseline (anchorSize.width, anchorSize.height) / (float) anchorSize.height);  //JLabelのベースラインをテキストに合わせる
        StyleConstants.setComponent (anchorStyle, anchorLabel);
        document.insertString (document.getLength (), anchorHref, anchorStyle);
        start = matcher.end ();  //ハイパーリンクの終了位置
      }
      document.insertString (document.getLength (), text.substring (start), defaultStyle);  //残りのテキスト
    } catch (BadLocationException ble) {
    }
    textPane.setMargin (new Insets (2, 4, 2, 4));
    textPane.setEditable (false);
    textPane.setCaretPosition (0);
    JScrollPane scrollPane = new JScrollPane (textPane);
    scrollPane.setPreferredSize (new Dimension (width, height));
    return scrollPane;
  }  //createScrollTextPane(String,int,int)


  //--------------------------------------------------------------------------------
  //ボタンを作る

  //button = createButton (enText, listener)
  //button = createButton (enText, mnemonic, listener)
  //  テキストのボタンを作る
  public static JButton createButton (String enText, ActionListener listener) {
    return createButton (enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(String,ActionListener)
  public static JButton createButton (String enText, int mnemonic, ActionListener listener) {
    JButton button = new JButton ();
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createButton(String,int,ActionListener)

  //button = createButton (image, enText, listener)
  //button = createButton (image, enText, mnemonic, listener)
  //button = createButton (image, disabledImage, enText, listener)
  //button = createButton (image, disabledImage, enText, mnemonic, listener)
  //  アイコンとテキストのボタンを作る
  public static JButton createButton (Image image, String enText, ActionListener listener) {
    return createButton (image, null, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(Image,String,ActionListener)
  public static JButton createButton (Image image, String enText, int mnemonic, ActionListener listener) {
    return createButton (image, null, enText, mnemonic, listener);
  }  //createButton(Image,String,int,ActionListener)
  public static JButton createButton (Image image, Image disabledImage, String enText, ActionListener listener) {
    return createButton (image, disabledImage, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(Image,Image,String,ActionListener)
  public static JButton createButton (Image image, Image disabledImage, String enText, int mnemonic, ActionListener listener) {
    JButton button = new JButton (new ImageIcon (image));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    button.setIconTextGap (3);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createButton(Image,Image,String,int,ActionListener)

  //button = createIconButton (icon, enToolTipText, listener)
  //button = createIconButton (icon, disabledIcon, enToolTipText, listener)
  //button = createImageButton (image, enToolTipText, listener)
  //button = createImageButton (image, disabledImage, enToolTipText, listener)
  //  アイコンのみのボタンを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JButton createIconButton (ImageIcon icon, String enToolTipText, ActionListener listener) {
    return createIconButton (icon, null, enToolTipText, listener);
  }  //createIconButton(ImageIcon,String,ActionListener)
  public static JButton createIconButton (ImageIcon icon, ImageIcon disabledIcon, String enToolTipText, ActionListener listener) {
    JButton button = new JButton (icon);
    if (disabledIcon != null) {
      button.setDisabledIcon (disabledIcon);
    }
    //button.setContentAreaFilled (false);
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    //button.setBorderPainted (false);
    //button.setMargin (new Insets (0, 0, 0, 0));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addRemovableListener (button, listener);
  }  //createIconButton(ImageIcon,ImageIcon,String,ActionListener)
  public static JButton createImageButton (Image image, String enToolTipText, ActionListener listener) {
    return createImageButton (image, null, enToolTipText, listener);
  }  //createImageButton(Image,String,ActionListener)
  public static JButton createImageButton (Image image, Image disabledImage, String enToolTipText, ActionListener listener) {
    JButton button = new JButton (new ImageIcon (image));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    //button.setContentAreaFilled (false);
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    //button.setBorderPainted (false);
    //button.setMargin (new Insets (0, 0, 0, 0));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addRemovableListener (button, listener);
  }  //createImageButton(Image,Image,String,ActionListener)

  //button = createCheckBox (selected, enText, listener)
  //button = createCheckBox (selected, enText, mnemonic, listener)
  //  チェックボックスを作る
  public static JCheckBox createCheckBox (boolean selected, String enText, ActionListener listener) {
    return createCheckBox (selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createCheckBox(boolean,String,ActionListener)
  public static JCheckBox createCheckBox (boolean selected, String enText, int mnemonic, ActionListener listener) {
    JCheckBox button = new JCheckBox ();
    button.setBorder (new EmptyBorder (0, 0, 0, 0));
    button.setSelected (selected);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createCheckBox(boolean,String,int,ActionListener)

  //button = createIconCheckBox (selected, image, selectedImage, enToolTipText, listener)
  //button = createIconCheckBox (selected, image, selectedImage, disabledImage, disabledSelectedImage, enToolTipText, listener)
  //  アイコンチェックボックスを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JCheckBox createIconCheckBox (boolean selected, Image image, Image selectedImage, String enToolTipText, ActionListener listener) {
    return createIconCheckBox (selected, image, selectedImage, null, null, enToolTipText, listener);
  }  //createIconCheckBox(boolean,Image,Image,String,ActionListener)
  public static JCheckBox createIconCheckBox (boolean selected, Image image, Image selectedImage, Image disabledImage, Image disabledSelectedImage, String enToolTipText, ActionListener listener) {
    JCheckBox button = new JCheckBox (new ImageIcon (image));
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    button.setSelected (selected);
    button.setSelectedIcon (new ImageIcon (selectedImage));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    if (disabledSelectedImage != null) {
      button.setDisabledSelectedIcon (new ImageIcon (disabledSelectedImage));
    }
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addRemovableListener (button, listener);
  }  //createIconCheckBox(boolean,Image,Image,Image,Image,String,ActionListener)

  //radioButton = createRadioButton (group, selected, enText, listener)
  //radioButton = createRadioButton (group, selected, enText, mnemonic, listener)
  //  ラジオボタンを作る
  public static JRadioButton createRadioButton (ButtonGroup group, boolean selected, String enText, ActionListener listener) {
    return createRadioButton (group, selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createRadioButton(ButtonGroup,boolean,String,ActionListener)
  public static JRadioButton createRadioButton (ButtonGroup group, boolean selected, String enText, int mnemonic, ActionListener listener) {
    JRadioButton button = new JRadioButton ();
    button.setBorder (new EmptyBorder (0, 0, 0, 0));
    group.add (button);
    button.setSelected (selected);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createRadioButton(ButtonGroup,boolean,String,int,ActionListener)

  //button = createIconRadioButton (group, selected, image, selectedImage, enToolTipText, listener)
  //  アイコンラジオボタンを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JRadioButton createIconRadioButton (ButtonGroup group, boolean selected, Image image, Image selectedImage, String enToolTipText, ActionListener listener) {
    JRadioButton button = new JRadioButton (new ImageIcon (image));
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    group.add (button);
    button.setSelected (selected);
    button.setSelectedIcon (new ImageIcon (selectedImage));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addRemovableListener (button, listener);
  }  //createIconRadioButton(ButtonGroup,boolean,Image,Image,String,int,ActionListener)


  //--------------------------------------------------------------------------------
  //コンボボックスを作る

  //comboBox = createComboBox (selectedIndex, enToolTipText, listener, text, ...)
  //  コンボボックスを作る
  public static JComboBox<String> createComboBox (int selectedIndex, String enToolTipText, ActionListener listener, String... texts) {
    JComboBox<String> comboBox = new JComboBox<String> (texts);
    comboBox.setBorder (new EmptyBorder (0, 0, 0, 0));
    comboBox.setSelectedIndex (selectedIndex);
    if (enToolTipText != null) {
      comboBox.setToolTipText (enToolTipText);
      comboBox.setActionCommand (enToolTipText);
    }
    return addRemovableListener (comboBox, listener);
  }  //createComboBox(int,String,ActionListener,String...)


  //--------------------------------------------------------------------------------
  //スライダーを作る

  //slider = createHorizontalSlider (min, max, value, major, minor, texts, listener)
  //  ラベルのテキストを指定してスライダーを作る
  public static JSlider createHorizontalSlider (int min, int max, int value, int major, int minor, String[] texts, ChangeListener listener) {
    JSlider slider = createHorizontalSlider (min, max, value, major, minor, listener);
    Hashtable<Integer,JComponent> table = new Hashtable<Integer,JComponent> ();
    for (int i = min; i <= max; i++) {
      if (i % major == 0 && texts[i - min] != null) {  //メジャー目盛りの位置だけ書く
        table.put (i, createLabel (texts[i - min]));
      }
    }
    slider.setLabelTable (table);
    return slider;
  }  //createHorizontalSlider(int,int,int,int,int,String[],ChangeListener)

  //slider = createHorizontalSlider (min, max, value, major, minor, listener)
  //  スライダーを作る
  public static JSlider createHorizontalSlider (int min, int max, int value, int major, int minor, ChangeListener listener) {
    JSlider slider = new JSlider (SwingConstants.HORIZONTAL, min, max, value);
    if (major != 0) {
      slider.setLabelTable (slider.createStandardLabels (major));
      slider.setPaintLabels (true);
      slider.setMajorTickSpacing (major);
      if (minor != 0) {
        slider.setMinorTickSpacing (minor);
      }
      slider.setPaintTicks (true);
      slider.setSnapToTicks (true);
    }
    return addRemovableListener (slider, listener);
  }  //createHorizontalSlider(int,int,int,int,int,ChangeListener)


  //--------------------------------------------------------------------------------
  //メニューを作る

  //menuBar = createMenuBar (component, ...)
  //  メニューバーを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  Box.createHorizontalGlue()を追加すると残りのメニューを右に寄せることができる
  public static JMenuBar createMenuBar (Component... components) {
    JMenuBar bar = new JMenuBar ();
    for (Component component : components) {
      if (component != null) {
        bar.add (component);
      }
    }
    return bar;
  }  //createMenuBar(Component...)

  //menu = createMenu (enText, listener, item, ...)
  //menu = createMenu (enText, mnemonic, listener, item, ...)
  //  メニューを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  セパレータを入れるときはcreateHorizontalSeparator()を使う
  //  JSeparatorを受け付けるためJMenuItem...ではなくJComponent...にする
  public static JMenu createMenu (String enText, JComponent... items) {
    return createMenu (enText, KeyEvent.VK_UNDEFINED, items);
  }  //createMenu(String,JComponent...)
  public static JMenu createMenu (String enText, int mnemonic, JComponent... items) {
    JMenu menu = new JMenu ();
    for (JComponent item : items) {
      if (item != null) {
        menu.add (item);
      }
    }
    //menu.setAccelerator()は実行時エラーになる
    //  java.lang.Error: setAccelerator() is not defined for JMenu.  Use setMnemonic() instead.
    return setButtonCommons (menu, enText, mnemonic, null);  //ボタンの共通の設定
  }  //createMenu(String,int,JComponent...)

  //popupMenu = createPopupMenu (item, ...)
  //  ポップアップメニューを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  セパレータを入れるときはcreateHorizontalSeparator()を使う
  //  JSeparatorを受け付けるためJMenuItem...ではなくJComponent...にする
  public static JPopupMenu createPopupMenu (JComponent... items) {
    JPopupMenu popupMenu = new JPopupMenu ();
    for (JComponent item : items) {
      if (item != null) {
        popupMenu.add (item);
      }
    }
    return popupMenu;
  }  //createPopupMenu(JComponent...)

  //item = createMenuItem (enText, listener)
  //item = createMenuItem (enText, mnemonic, listener)
  //  メニューアイテムを作る
  public static JMenuItem createMenuItem (String enText, ActionListener listener) {
    return createMenuItem (enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createMenuItem(String,ActionListener)
  public static JMenuItem createMenuItem (String enText, int mnemonic, ActionListener listener) {
    return createMenuItem (enText, mnemonic, 0, listener);
  }  //createMenuItem(String,int,ActionListener)
  public static JMenuItem createMenuItem (String enText, int mnemonic, int modifiers, ActionListener listener) {
    JMenuItem item = new JMenuItem ();
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createMenuItem(String,int,int,ActionListener)

  //item = createCheckBoxMenuItem (selected, enText, listener)
  //item = createCheckBoxMenuItem (selected, enText, mnemonic, listener)
  //  チェックボックスメニューアイテムを作る
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, ActionListener listener) {
    return createCheckBoxMenuItem (selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createCheckBoxMenuItem(boolean,String,ActionListener)
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, int mnemonic, ActionListener listener) {
    return createCheckBoxMenuItem (selected, enText, mnemonic, 0, listener);
  }  //createCheckBoxMenuItem(boolean,String,int,ActionListener)
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, int mnemonic, int modifiers, ActionListener listener) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem ();
    item.setSelected (selected);
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createCheckBoxMenuItem(boolean,String,int,int,ActionListener)

  //item = createRadioButtonMenuItem (group, selected, enText, listener)
  //item = createRadioButtonMenuItem (group, selected, enText, mnemonic, listener)
  //  ラジオボタンメニューアイテムを作る
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, ActionListener listener) {
    return createRadioButtonMenuItem (group, selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,ActionListener)
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, int mnemonic, ActionListener listener) {
    return createRadioButtonMenuItem (group, selected, enText, mnemonic, 0, listener);
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,int,ActionListener)
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, int mnemonic, int modifiers, ActionListener listener) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem ();
    group.add (item);
    item.setSelected (selected);
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,int,int,ActionListener)

  //setButtonCommons (button, enText, mnemonic, listener)
  //  ボタンの共通の設定
  //  ニモニックを含まないテキストをそのままアクションコマンドにする
  //  Multilingual.mlnTextがアクションコマンドを英語のテキストとして使うのでアクションリスナーを省略してもアクションコマンドは設定される
  //  ニモニックはKeyEvent.VK_～で指定する。英数字は大文字のcharで指定しても問題ない
  //  Multilingual.mlnTextがニモニックの有無をgetMnemonicで確認するのでニモニックがKeyEvent.VK_UNDEFINEDのときもそのままニモニックとして設定される
  public static <T extends AbstractButton> T setButtonCommons (T button, String enText, int mnemonic, ActionListener listener) {
    button.setMnemonic (mnemonic);
    if (mnemonic == KeyEvent.VK_UNDEFINED) {  //ニモニックがないとき
      button.setText (enText);
    } else {  //ニモニックがあるとき
      //テキストにニモニックの大文字と小文字が両方含まれているとき、大文字と小文字が一致するほうにマークを付ける
      String mnemonicText = KeyEvent.getKeyText (mnemonic);
      int index = enText.indexOf (mnemonicText);  //大文字と小文字を区別して検索する
      if (index < 0) {
        index = enText.toLowerCase ().indexOf (mnemonicText.toLowerCase ());  //大文字と小文字を区別せずに検索する
      }
      if (index >= 0) {  //ニモニックがテキストに含まれているとき
        button.setText (enText);
        button.setDisplayedMnemonicIndex (index);
      } else {  //ニモニックがテキストに含まれていないとき
        button.setText (enText + "(" + mnemonicText + ")");
        button.setDisplayedMnemonicIndex (enText.length () + 1);
      }
    }
    button.setActionCommand (enText);
    return addRemovableListener (button, listener);
  }  //setButtonCommons(T extends AbstractButton,String,int,ActionListener)


  //--------------------------------------------------------------------------------
  //リストを作る

  //list = createScrollList (texts, visibleRowCount, selectedIndex, listener)
  //list = createScrollList (texts, visibleRowCount, selectedIndex, selectionMode, listener)
  //  selectionMode
  //    ListSelectionModel.SINGLE_SELECTION
  //    ListSelectionModel.SINGLE_INTERVAL_SELECTION
  //    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  public static ScrollList createScrollList (String[] texts, int visibleRowCount, int selectedIndex, ListSelectionListener listener) {
    return createScrollList (texts, visibleRowCount, selectedIndex, ListSelectionModel.SINGLE_SELECTION, listener);
  }  //createScrollList(String[],int,int)
  public static ScrollList createScrollList (String[] texts, int visibleRowCount, int selectedIndex, int selectionMode, ListSelectionListener listener) {
    DefaultListModel<String> listModel = new DefaultListModel<String> ();
    for (String text : texts) {
      listModel.addElement (text);
    }
    ScrollList list = new ScrollList (listModel);
    list.setVisibleRowCount (visibleRowCount);
    list.setSelectionMode (selectionMode);
    list.setSelectedIndex (selectedIndex);
    return addRemovableListener (list, listener);
  }  //createScrollList(String[],int,int,int)


  //--------------------------------------------------------------------------------
  //スピナーを作る

  //spinner = createNumberSpinner (model, digits, listener)
  //  ナンバースピナーを作る
  public static JSpinner createNumberSpinner (SpinnerNumberModel model, int digits, ChangeListener listener) {
    JSpinner spinner = new NumberSpinner (model);
    spinner.setBorder (new LineBorder (new Color (LnF.LNF_RGB[10]), 1));
    spinner.setPreferredSize (new Dimension (24 + 6 * digits, 16));
    spinner.setMaximumSize (new Dimension (24 + 6 * digits, 16));
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor ();
    editor.getFormat ().setGroupingUsed (false);  //3桁毎にグループ化しない
    JTextField textField = editor.getTextField ();
    textField.setHorizontalAlignment (JTextField.RIGHT);  //右寄せ
    textField.setFont (new Font ("Monospaced", Font.PLAIN, 12));
    return addRemovableListener (spinner, listener);
  }  //createNumberSpinner(SpinnerNumberModel,int,ChangeListener)

  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize)
  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize, option)
  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize, option, listener)
  //  10進数スピナーを作る
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize) {
    return createDecimalSpinner (value, minimum, maximum, stepSize, 0, null);
  }  //createDecimalSpinner(int,int,int,int)
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize, int option) {
    return createDecimalSpinner (value, minimum, maximum, stepSize, option, null);
  }  //createDecimalSpinner(int,int,int,int,int)
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize, int option, ChangeListener listener) {
    return addRemovableListener (new DecimalSpinner (value, minimum, maximum, stepSize, option), listener);
  }  //createDecimalSpinner(int,int,int,int,int,ChangeListener)

  //spinner = createHex8Spinner (value, mask, reverse, listener)
  //   8桁16進数スピナーを作る
  public static Hex8Spinner createHex8Spinner (int value, int mask, boolean reverse, ChangeListener listener) {
    return addRemovableListener (new Hex8Spinner (value, mask, reverse), listener);
  }  //createHex8Spinner(int,int,boolean,ChangeListener)

  //spinner = createListSpinner (list, value, listener)
  //  リストスピナーを作る
  public static JSpinner createListSpinner (java.util.List<?> list, Object value, ChangeListener listener) {
    SpinnerListModel model = new SpinnerListModel (list);
    JSpinner spinner = new JSpinner (model);
    spinner.setBorder (new LineBorder (new Color (LnF.LNF_RGB[10]), 1));
    int digits = 0;
    for (Object t : list) {
      digits = Math.max (digits, String.valueOf (t).length ());
    }
    spinner.setPreferredSize (new Dimension (24 + 10 * digits, 16));
    spinner.setMaximumSize (new Dimension (24 + 10 * digits, 16));
    JSpinner.ListEditor editor = (JSpinner.ListEditor) spinner.getEditor ();
    JTextField textField = editor.getTextField ();
    textField.setHorizontalAlignment (JTextField.RIGHT);  //右寄せ
    textField.setFont (new Font ("Monospaced", Font.PLAIN, 12));
    model.setValue (value);  //初期設定ではリスナーを呼び出さない
    return addRemovableListener (spinner, listener);
  }  //createListSpinner (java.util.List<?>,Object,ChangeListener)



}  //class XEiJ



