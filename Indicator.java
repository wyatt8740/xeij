//========================================================================================
//  Indicator.java
//    en:Indicator -- It displays the model, the clock frequency and the utilization rate.
//    ja:インジケータ -- 機種とクロック周波数と負荷率を表示します。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class Indicator {

  //設定
  public static final int IND_FONT_WIDTH = 4;  //文字の幅。4/6/8のいずれか
  public static final int IND_FONT_HEIGHT = IND_FONT_WIDTH == 4 ? 5 : IND_FONT_WIDTH == 6 ? 12 : 8;  //文字の高さ。5/12/8のいずれか

  public static final int IND_PANEL_WIDTH = IND_FONT_WIDTH * 15;  //パネルの幅
  public static final int IND_PANEL_HEIGHT = IND_FONT_HEIGHT * 2 + 1;  //パネルの高さ

  public static final int IND_INTERVAL = 50;  //間隔。XEiJ.TMR_INTERVAL*IND_INTERVAL毎に表示する

  public static final boolean IND_INLINE_PUTS = false;  //true=indPutsをインライン展開する

  //フォント
/*
  public static final short[] IND_ASCII_3X5 = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    //20
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //21 !
    (0b010 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b000 <<  3 |
     0b010),
    //22 "
    (0b101 << 12 |
     0b101 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //23 #
    (0b101 << 12 |
     0b111 <<  9 |
     0b101 <<  6 |
     0b111 <<  3 |
     0b101),
    //24 $
    (0b011 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b011 <<  3 |
     0b110),
    //25 %
    (0b101 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b011 <<  3 |
     0b101),
    //26 &
    (0b110 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b110),
    //27 '
    (0b010 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //28 (
    (0b001 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b001),
    //29 )
    (0b100 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b100),
    //2a *
    (0b000 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b000),
    //2b +
    (0b000 << 12 |
     0b010 <<  9 |
     0b111 <<  6 |
     0b010 <<  3 |
     0b000),
    //2c ,
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //2d -
    (0b000 << 12 |
     0b000 <<  9 |
     0b111 <<  6 |
     0b000 <<  3 |
     0b000),
    //2e .
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b010),
    //2f /
    (0b000 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b100 <<  3 |
     0b000),
    //30 0
    (0b111 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b111),
    //31 1
    (0b010 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //32 2
    (0b111 << 12 |
     0b001 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b111),
    //33 3
    (0b111 << 12 |
     0b001 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //34 4
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b001),
    //35 5
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //36 6
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b111),
    //37 7
    (0b111 << 12 |
     0b001 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b001),
    //38 8
    (0b111 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b111),
    //39 9
    (0b111 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //3a :
    (0b000 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b000),
    //3b ;
    (0b000 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //3c <
    (0b001 << 12 |
     0b010 <<  9 |
     0b100 <<  6 |
     0b010 <<  3 |
     0b001),
    //3d =
    (0b000 << 12 |
     0b111 <<  9 |
     0b000 <<  6 |
     0b111 <<  3 |
     0b000),
    //3e >
    (0b100 << 12 |
     0b010 <<  9 |
     0b001 <<  6 |
     0b010 <<  3 |
     0b100),
    //3f ?
    (0b110 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b000 <<  3 |
     0b010),
    //40 @
    (0b010 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b011),
    //41 A
    (0b010 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //42 B
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b110),
    //43 C
    (0b011 << 12 |
     0b100 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b011),
    //44 D
    (0b110 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b110),
    //45 E
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b111),
    //46 F
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b100),
    //47 G
    (0b011 << 12 |
     0b100 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //48 H
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //49 I
    (0b111 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b111),
    //4a J
    (0b001 << 12 |
     0b001 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b110),
    //4b K
    (0b101 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b101),
    //4c L
    (0b100 << 12 |
     0b100 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b111),
    //4d M
    (0b101 << 12 |
     0b111 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //4e N
    (0b110 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //4f O
    (0b010 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //50 P
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b100 <<  3 |
     0b100),
    //51 Q
    (0b010 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b111 <<  3 |
     0b011),
    //52 R
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b101),
    //53 S
    (0b011 << 12 |
     0b100 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b110),
    //54 T
    (0b111 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //55 U
    (0b101 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b111),
    //56 V
    (0b101 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //57 W
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b101),
    //58 X
    (0b101 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b101),
    //59 Y
    (0b101 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //5a Z
    (0b111 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b100 <<  3 |
     0b111),
    //5b [
    (0b011 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b011),
    //5c \ 
    (0b000 << 12 |
     0b100 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b000),
    //5d ]
    (0b110 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b110),
    //5e ^
    (0b010 << 12 |
     0b101 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //5f _
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b111),
    //60 `
    (0b010 << 12 |
     0b001 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //61 a
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //62 b
    (0b100 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b110),
    //63 c
    (0b000 << 12 |
     0b011 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b011),
    //64 d
    (0b001 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //65 e
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b110 <<  3 |
     0b011),
    //66 f
    (0b011 << 12 |
     0b111 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //67 g
    (0b011 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b110),
    //68 h
    (0b100 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //69 i
    (0b010 << 12 |
     0b000 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //6a j
    (0b001 << 12 |
     0b000 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b110),
    //6b k
    (0b100 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b110 <<  3 |
     0b101),
    //6c l
    (0b110 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //6d m
    (0b000 << 12 |
     0b110 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b101),
    //6e n
    (0b000 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //6f o
    (0b000 << 12 |
     0b010 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //70 p
    (0b000 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b110 <<  3 |
     0b100),
    //71 q
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b011 <<  3 |
     0b001),
    //72 r
    (0b000 << 12 |
     0b011 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b100),
    //73 s
    (0b000 << 12 |
     0b011 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b110),
    //74 t
    (0b010 << 12 |
     0b111 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b001),
    //75 u
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //76 v
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //77 w
    (0b000 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b011),
    //78 x
    (0b000 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b101),
    //79 y
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b010 <<  3 |
     0b100),
    //7a z
    (0b000 << 12 |
     0b111 <<  9 |
     0b001 <<  6 |
     0b010 <<  3 |
     0b111),
    //7b {
    (0b001 << 12 |
     0b010 <<  9 |
     0b110 <<  6 |
     0b010 <<  3 |
     0b001),
    //7c |
    (0b010 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //7d }
    (0b100 << 12 |
     0b010 <<  9 |
     0b011 <<  6 |
     0b010 <<  3 |
     0b100),
    //7e ~
    (0b111 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
  };
*/
  //  perl misc/itoc.pl xeij/Indicator.java IND_ASCII_3X5
  public static final char[] IND_ASCII_3X5 = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\u2482\u5a00\u5f7d\u3c9e\u5c9d\u6cae\u2400\u1491\u4494\u0aa8\u05d0\22\u01c0\2\u02a0\u7b6f\u2492\u73e7\u73cf\u5bc9\u79cf\u79ef\u7249\u7bef\u7bcf\u0410\u0412\u1511\u0e38\u4454\u6282\u2be3\u2bed\u6bae\u3923\u6b6e\u79e7\u79e4\u396b\u5bed\u7497\u124e\u5bad\u4927\u5fed\u6b6d\u2b6a\u6ba4\u2b7b\u6bad\u388e\u7492\u5b6f\u5b6a\u5bfd\u5aad\u5a92\u72a7\u3493\u0888\u6496\u2a00\7\u2200\u076b\u4d6e\u0723\u176b\u0773\u3e92\u3bce\u4d6d\u2092\u104e\u4bb5\u6492\u0dfd\u0d6d\u056a\u0d74\u0759\u0724\u068e\u2e91\u0b6b\u0b6a\u0bfb\u0a95\u0b54\u0e57\u1591\u2412\u44d4\u7000".toCharArray ();

  public static int indLastModel;  //前回表示した機種

  //パネル
  public static BufferedImage indImage;
  public static int[] indBitmap;
  public static Box indBox;

  //カウンタ
  public static int indCount;
  public static long indTotalNano;  //コアのスレッドの動作時間(ns)。本来の動作時間の1/10ならば負荷率10%
  public static long indLastNano;  //前回の時刻(ns)
  public static double indCoreNano1;
  public static double indCoreNano2;

  //indInit ()
  //  インジケータを初期化する
  public static void indInit () {
    indLastModel = -1;
    //パネル
    indImage = new BufferedImage (IND_PANEL_WIDTH, IND_PANEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
    indBitmap = ((DataBufferInt) indImage.getRaster ().getDataBuffer ()).getData ();
    //Arrays.fill (indBitmap, 0xff000000);
    indBox = XEiJ.setFixedSize (
      new Box (BoxLayout.LINE_AXIS) {
        @Override public void paint (Graphics g) {
          g.drawImage (indImage, 0, 0, null);
        }
        @Override protected void paintComponent (Graphics g) {
        }
        @Override protected void paintBorder (Graphics g) {
        }
        @Override protected void paintChildren (Graphics g) {
        }
        @Override public void update (Graphics g) {
        }
      },
      IND_PANEL_WIDTH, IND_PANEL_HEIGHT);
    //カウンタ
    indCount = IND_INTERVAL;
    indTotalNano = 0L;
    indLastNano = System.nanoTime ();
    indCoreNano1 = indCoreNano2 = 0.5 * 1e+6 * (double) (XEiJ.TMR_INTERVAL * IND_INTERVAL);  //負荷率50%
  }  //indInit()

  //indUpdate (nanoEnd)
  //  インジケータを更新する
  public static void indUpdate (long nanoEnd) {
    //本来の経過時間(ns)
    final double expectedNano = 1e+6 * (double) (XEiJ.TMR_INTERVAL * IND_INTERVAL);
    //コアの所要時間(ns)
    double coreNano0 = (double) indTotalNano;
    indTotalNano = 0L;
    double coreNanoA = (coreNano0 * 2.0 + indCoreNano1 + indCoreNano2) * 0.25;  //コアの所要時間(ns)
    indCoreNano2 = indCoreNano1;
    indCoreNano1 = coreNano0;
    //現在の負荷率(%)
    //  現在の負荷率(%) = 100.0 * コアの所要時間(ns) / 本来の経過時間(ns)
    //  処理が間に合っていないとき100%よりも大きくなる
    double actualPercent = 100.0 * coreNanoA / expectedNano;
    //負荷率の上限(%)
    double maxPercent = SoundSource.sndPlayOn ? 90.0 : 100.0;  //音声出力がONのとき負荷率の上限を90%に抑える
    //目標の負荷率(%)
    double targetPercent = Math.min (maxPercent, (double) XEiJ.mpuUtilRatio);
    //目標の動作周波数(MHz)
    //double targetMHz = XEiJ.mpuClockMHz;
    //現在の動作周波数(MHz)
    //double currentMHz = XEiJ.mpuCurrentMHz;
    if (XEiJ.mpuUtilOn) {  //任意の負荷率が指定されているとき
      //  新しい動作周波数(MHz) = (1.1 - 0.1 * 現在の負荷率(%) / 目標の負荷率(%)) * 現在の動作周波数(MHz)
      XEiJ.mpuSetClockMHz ((1.2 - 0.2 * actualPercent / targetPercent) * XEiJ.mpuCurrentMHz);  //現在の負荷率を目標の負荷率に近付ける
    } else {  //任意の負荷率が指定されていないとき
      //  新しい動作周波数(MHz) = Math.min (負荷率の上限(%) / Math.max (1(%), 現在の負荷率(%)),
      //                                    1.1 - 0.1 * 現在の動作周波数(MHz) / 目標の動作周波数(MHz)) * 現在の動作周波数(MHz)
      XEiJ.mpuSetClockMHz (Math.min (maxPercent / Math.max (1.0, actualPercent),
                                     1.2 - 0.2 * XEiJ.mpuCurrentMHz / XEiJ.mpuClockMHz) * XEiJ.mpuCurrentMHz);
    }
    //文字を表示する
    //    012345678901234
    //   +---------------
    //  0| X68000 EXPERT
    //  0| X68000 SUPER
    //  0| X68000 XVI
    //  0| X68000 Compact
    //  0| X68000 Hybrid
    //  0| X68030
    //  0| 060turbo
    if (indLastModel != XEiJ.mdlModel) {
      switch (XEiJ.mdlModel) {
      case XEiJ.MDL_EXPERT:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '0'), '0'), ' '), 'E'), 'X'), 'P'), 'E'), 'R'), 'T'), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "X68000 EXPERT ");
        }
        break;
      case XEiJ.MDL_SUPER:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '0'), '0'), ' '), 'S'), 'U'), 'P'), 'E'), 'R'), ' '), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "X68000 SUPER  ");
        }
        break;
      case XEiJ.MDL_XVI:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '0'), '0'), ' '), 'X'), 'V'), 'I'), ' '), ' '), ' '), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "X68000 XVI    ");
        }
        break;
      case XEiJ.MDL_COMPACT:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '0'), '0'), ' '), 'C'), 'o'), 'm'), 'p'), 'a'), 'c'), 't');
        } else {
          indPuts (IND_FONT_WIDTH, "X68000 Compact");
        }
        break;
      case XEiJ.MDL_HYBRID:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '0'), '0'), ' '), 'H'), 'y'), 'b'), 'r'), 'i'), 'd'), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "X68000 Hybrid ");
        }
        break;
      case XEiJ.MDL_X68030:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, 'X'), '6'), '8'), '0'), '3'), '0'), ' '), ' '), ' '), ' '), ' '), ' '), ' '), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "X68030        ");
        }
        break;
      case XEiJ.MDL_060TURBO:
        if (IND_INLINE_PUTS) {
          indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (indC (IND_FONT_WIDTH, '0'), '6'), '0'), 't'), 'u'), 'r'), 'b'), 'o'), ' '), ' '), ' '), ' '), ' '), ' ');
        } else {
          indPuts (IND_FONT_WIDTH, "060turbo      ");
        }
        break;
      }
      indLastModel = XEiJ.mdlModel;
    }
    //    012345678901234
    //   +---------------
    //  1|###.#MHz ###.#%
    indC (indF1 (indC (indC (indC (indC (indF1 (IND_PANEL_WIDTH * (IND_FONT_HEIGHT + 1),
                                                (int) (10.0 * XEiJ.mpuCurrentMHz + 0.5)), 'M'), 'H'), 'z'),
                       ' '),
                 (int) (10.0 * actualPercent + 0.5)), '%');
    indBox.repaint ();
    indCount = IND_INTERVAL;
  }  //indUpdate(long)

  //  ###.#
  public static int indF1 (int p, int x) {
    x = XEiJ.FMT_BCD4[Math.max (0, Math.min (9999, x))];
    return indC (indC (indC (indC (indC (p, x >= 0x1000 ? x >> 12 | '0' : ' '), x >= 0x100 ? x >> 8 & 15 | '0' : ' '), x >> 4 & 15 | '0'), '.'), x & 15 | '0');
  }  //indF1(int,int)

  //  ##.##
  public static int indF2 (int p, int x) {
    x = XEiJ.FMT_BCD4[Math.max (0, Math.min (9999, x))];
    return indC (indC (indC (indC (indC (p, x >= 0x1000 ? x >> 12 | '0' : ' '), x >> 8 & 15 | '0'), '.'), x >> 4 & 15 | '0'), x & 15 | '0');
  }  //indF2(int,int)

  //  1文字表示
  public static int indC (int p, int c) {
    int rgb = LnF.LNF_RGB[14];
    if (IND_FONT_WIDTH == 4) {
      int t = IND_ASCII_3X5[c];
      indBitmap[p + (IND_PANEL_WIDTH * 0 + 1)] = -(t >> 14 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 0 + 2)] = -(t >> 13 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 0 + 3)] = -(t >> 12 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 1 + 1)] = -(t >> 11 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 1 + 2)] = -(t >> 10 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 1 + 3)] = -(t >>  9 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 2 + 1)] = -(t >>  8 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 2 + 2)] = -(t >>  7 & 1) & rgb;  // (byte) t >> 7 & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 2 + 3)] = -(t >>  6 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 3 + 1)] = -(t >>  5 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 3 + 2)] = -(t >>  4 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 3 + 3)] = -(t >>  3 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 4 + 1)] = -(t >>  2 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 4 + 2)] = -(t >>  1 & 1) & rgb;
      indBitmap[p + (IND_PANEL_WIDTH * 4 + 3)] = -(t       & 1) & rgb;
    } else if (IND_FONT_WIDTH == 6) {
      int a = XEiJ.FNT_ADDRESS_ANK6X12 + IND_FONT_HEIGHT * c;
      for (int i = 0; i < IND_FONT_HEIGHT; i++) {
        int t = MainMemory.mmrM8[a++];
        indBitmap[p    ] = (byte) t >> 7 & rgb;  //-(t >> 7 & 1)
        indBitmap[p + 1] = -(t >> 6 & 1) & rgb;
        indBitmap[p + 2] = -(t >> 5 & 1) & rgb;
        indBitmap[p + 3] = -(t >> 4 & 1) & rgb;
        indBitmap[p + 4] = -(t >> 3 & 1) & rgb;
        indBitmap[p + 5] = -(t >> 2 & 1) & rgb;
        p += IND_PANEL_WIDTH;
      }
      p -= IND_PANEL_WIDTH * IND_FONT_HEIGHT;
    } else {
      int a = XEiJ.FNT_ADDRESS_ANK8X8 + IND_FONT_HEIGHT * c;
      for (int i = 0; i < IND_FONT_HEIGHT; i++) {
        int t = MainMemory.mmrM8[a++];
        indBitmap[p    ] = (byte) t >> 7 & rgb;  //-(t >> 7 & 1)
        indBitmap[p + 1] = -(t >> 6 & 1) & rgb;
        indBitmap[p + 2] = -(t >> 5 & 1) & rgb;
        indBitmap[p + 3] = -(t >> 4 & 1) & rgb;
        indBitmap[p + 4] = -(t >> 3 & 1) & rgb;
        indBitmap[p + 5] = -(t >> 2 & 1) & rgb;
        indBitmap[p + 6] = -(t >> 1 & 1) & rgb;
        indBitmap[p + 7] = -(t      & 1) & rgb;
        p += IND_PANEL_WIDTH;
      }
      p -= IND_PANEL_WIDTH * IND_FONT_HEIGHT;
    }
    return p + IND_FONT_WIDTH;
  }  //indC(int,int)

  //p = indPuts (p, s)
  //  文字列表示
  public static int indPuts (int p, String s) {
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      p = indC (p, s.charAt (i));
    }
    return p;
  }  //indPuts(int,String)

}  //class Indicator



