//========================================================================================
//  LnF.java
//    en:Look and feel
//    ja:ルックアンドフィール
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.plaf.*;  //ColorUIResource,FontUIResource,IconUIResource,InsetsUIResource
import javax.swing.plaf.metal.*;  //MetalLookAndFeel,MetalTheme,OceanTheme

public class LnF {

  //色
  public static final int LNF_H0 =  667;
  public static final int LNF_H1 =  667;
  public static final int LNF_S0 =  700;
  public static final int LNF_S1 =  300;
  public static final int LNF_B0 =    0;
  public static final int LNF_B1 = 1000;
  public static int lnfH0;
  public static int lnfH1;
  public static int lnfS0;
  public static int lnfS1;
  public static int lnfB0;
  public static int lnfB1;
  public static final int[] LNF_RGB = new int[15];
  public static ColorUIResource lnfSecondary3;
  public static ColorUIResource lnfWhite;
  public static ColorUIResource lnfPrimary3;
  public static ColorUIResource lnfPrimary2;
  public static ColorUIResource lnfSecondary2;
  public static ColorUIResource lnfPrimary1;
  public static ColorUIResource lnfSecondary1;
  public static ColorUIResource lnfBlack;

  //フォント
  public static FontUIResource lnfFont10;
  public static FontUIResource lnfFont12;

  //アイコン
  //  Xマークが左右にはみ出しているイメージ
  //  perl misc/favicon.pl
  public static final BufferedImage LNF_ICON_IMAGE_16 = XEiJ.createImage (
    16, 16,
    "................" +
    "1111111111.11111" +
    "1........1..1..." +
    ".1........1.1..." +
    ".1........1..1.." +
    "..1........1.1.1" +
    "..1........1..1." +
    "...1........1..." +
    "...1........1..." +
    ".1..1........1.." +
    "1.1.1........1.." +
    "..1..1........1." +
    "...1.1........1." +
    "...1..1........1" +
    "11111.1111111111" +
    "................",
    0xff000000,
    0xffffff00
    );
  public static final BufferedImage LNF_ICON_IMAGE_24 = XEiJ.createImage (
    24, 24,
    "........................" +
    "11111111111111...1111111" +
    "111111111111111..1111111" +
    "11............1..11....." +
    ".1............11..1....." +
    ".11............1..11...." +
    "..1............11..1...." +
    "..11............1..11..1" +
    "...1............11..1.11" +
    "...11............1..111." +
    "....1............11..1.." +
    "....11............1....." +
    ".....1............11...." +
    "..1..11............1...." +
    ".111..1............11..." +
    "11.1..11............1..." +
    "1..11..1............11.." +
    "....1..11............1.." +
    "....11..1............11." +
    ".....1..11............1." +
    ".....11..1............11" +
    "1111111..111111111111111" +
    "1111111...11111111111111" +
    "........................",
    0xff000000,
    0xffffff00
    );
  public static final BufferedImage LNF_ICON_IMAGE_32 = XEiJ.createImage (
    32, 32,
    "................................" +
    "................................" +
    "1111111111111111111...1111111111" +
    "11111111111111111111..1111111111" +
    "11................11...11......." +
    ".11................11..111......" +
    ".11................11...11......" +
    "..11................11..111....." +
    "..11................11...11....." +
    "...11................11..111...1" +
    "...11................11...11..11" +
    "....11................11..111111" +
    "....11................11...1111." +
    ".....11................11..111.." +
    ".....11................11...1..." +
    "......11................11......" +
    "......11................11......" +
    "...1...11................11....." +
    "..111..11................11....." +
    ".1111...11................11...." +
    "111111..11................11...." +
    "11..11...11................11..." +
    "1...111..11................11..." +
    ".....11...11................11.." +
    ".....111..11................11.." +
    "......11...11................11." +
    "......111..11................11." +
    ".......11...11................11" +
    "1111111111..11111111111111111111" +
    "1111111111...1111111111111111111" +
    "................................" +
    "................................",
    0xff000000,
    0xffffff00
    );
  public static final BufferedImage LNF_ICON_IMAGE_48 = XEiJ.createImage (
    48, 48,
    "................................................" +
    "................................................" +
    "................................................" +
    "11111111111111111111111111111....111111111111111" +
    "111111111111111111111111111111...111111111111111" +
    "111111111111111111111111111111....11111111111111" +
    "111........................1111...111..........." +
    "1111........................111....111.........." +
    ".111........................1111...111.........." +
    ".1111........................111....111........." +
    "..111........................1111...111........." +
    "..1111........................111....111........" +
    "...111........................1111...111........" +
    "...1111........................111....111......." +
    "....111........................1111...111......1" +
    "....1111........................111....111....11" +
    ".....111........................1111...111...111" +
    ".....1111........................111....111.1111" +
    "......111........................1111...1111111." +
    "......1111........................111....11111.." +
    ".......111........................1111...1111..." +
    ".......1111........................111....11...." +
    "........111........................1111...1....." +
    "........1111........................111........." +
    ".........111........................1111........" +
    ".....1...1111........................111........" +
    "....11....111........................1111......." +
    "...1111...1111........................111......." +
    "..11111....111........................1111......" +
    ".1111111...1111........................111......" +
    "1111.111....111........................1111....." +
    "111..1111...1111........................111....." +
    "11....111....111........................1111...." +
    "1.....1111...1111........................111...." +
    ".......111....111........................1111..." +
    ".......1111...1111........................111..." +
    "........111....111........................1111.." +
    "........1111...1111........................111.." +
    ".........111....111........................1111." +
    ".........1111...1111........................111." +
    "..........111....111........................1111" +
    "..........1111...1111........................111" +
    "11111111111111....111111111111111111111111111111" +
    "111111111111111...111111111111111111111111111111" +
    "111111111111111....11111111111111111111111111111" +
    "................................................" +
    "................................................" +
    "................................................",
    0xff000000,
    0xffffff00
    );
  public static final BufferedImage LNF_ICON_IMAGE_64 = XEiJ.createImage (
    64, 64,
    "................................................................" +
    "................................................................" +
    "................................................................" +
    "................................................................" +
    "111111111111111111111111111111111111111....111111111111111111111" +
    "111111111111111111111111111111111111111.....11111111111111111111" +
    "1111111111111111111111111111111111111111....11111111111111111111" +
    "1111111111111111111111111111111111111111.....1111111111111111111" +
    "1111................................11111....11111.............." +
    "11111................................1111.....1111.............." +
    ".1111................................11111....11111............." +
    ".11111................................1111.....1111............." +
    "..1111................................11111....11111............" +
    "..11111................................1111.....1111............" +
    "...1111................................11111....11111..........." +
    "...11111................................1111.....1111..........." +
    "....1111................................11111....11111.........." +
    "....11111................................1111.....1111.........." +
    ".....1111................................11111....11111........1" +
    ".....11111................................1111.....1111.......11" +
    "......1111................................11111....11111.....111" +
    "......11111................................1111.....1111....1111" +
    ".......1111................................11111....11111..11111" +
    ".......11111................................1111.....1111.111111" +
    "........1111................................11111....1111111111." +
    "........11111................................1111.....11111111.." +
    ".........1111................................11111....1111111..." +
    ".........11111................................1111.....11111...." +
    "..........1111................................11111....1111....." +
    "..........11111................................1111.....11......" +
    "...........1111................................11111............" +
    "...........11111................................1111............" +
    "............1111................................11111..........." +
    "............11111................................1111..........." +
    "......11.....1111................................11111.........." +
    ".....1111....11111................................1111.........." +
    "....11111.....1111................................11111........." +
    "...1111111....11111................................1111........." +
    "..11111111.....1111................................11111........" +
    ".1111111111....11111................................1111........" +
    "11111111111.....1111................................11111......." +
    "11111..11111....11111................................1111......." +
    "1111....1111.....1111................................11111......" +
    "111.....11111....11111................................1111......" +
    "11.......1111.....1111................................11111....." +
    "1........11111....11111................................1111....." +
    "..........1111.....1111................................11111...." +
    "..........11111....11111................................1111...." +
    "...........1111.....1111................................11111..." +
    "...........11111....11111................................1111..." +
    "............1111.....1111................................11111.." +
    "............11111....11111................................1111.." +
    ".............1111.....1111................................11111." +
    ".............11111....11111................................1111." +
    "..............1111.....1111................................11111" +
    "..............11111....11111................................1111" +
    "1111111111111111111.....1111111111111111111111111111111111111111" +
    "11111111111111111111....1111111111111111111111111111111111111111" +
    "11111111111111111111.....111111111111111111111111111111111111111" +
    "111111111111111111111....111111111111111111111111111111111111111" +
    "................................................................" +
    "................................................................" +
    "................................................................" +
    "................................................................",
    0xff000000,
    0xffffff00
    );
  public static final BufferedImage[] LNF_ICON_IMAGES = {
    LNF_ICON_IMAGE_16,
    LNF_ICON_IMAGE_24,
    LNF_ICON_IMAGE_32,
    LNF_ICON_IMAGE_48,
    LNF_ICON_IMAGE_64,
  };

  //アイコンのパターンとイメージ
  public static final String[] LNF_NUMBER_PATTERN_ARRAY = {
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1.....11.....1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11.........1" +
      "1.11.........1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11.........1" +
      "1.11.........1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11.........1" +
      "1.11.........1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1.........11.1" +
      "1.........11.1" +
      "1.1111111111.1" +
      "1.1111111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1..11....11..1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11.11......1" +
      "1.11.11......1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.11...11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1............1" +
      "11111111111111"),
    (
      "11111111111111" +
      "1............1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11.11......1" +
      "1.11.11......1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1.11......11.1" +
      "1.11......11.1" +
      "1.11.1111111.1" +
      "1.11.1111111.1" +
      "1............1" +
      "11111111111111"),
  };
  public static final Image[] LNF_NUMBER_IMAGE_ARRAY = new Image[LNF_NUMBER_PATTERN_ARRAY.length];
  public static final Image[] LNF_NUMBER_SELECTED_IMAGE_ARRAY = new Image[LNF_NUMBER_PATTERN_ARRAY.length];

  public static final String LNF_EJECT_PATTERN = (
    ".............." +
    "......11......" +
    ".....1..1....." +
    "....1....1...." +
    "...1......1..." +
    "..1........1.." +
    ".1..........1." +
    ".1..........1." +
    ".111111111111." +
    ".............." +
    ".111111111111." +
    ".1..........1." +
    ".1..........1." +
    ".111111111111.");
  public static Image LNF_EJECT_IMAGE;
  public static Image LNF_EJECT_DISABLED_IMAGE;

  public static final String LNF_OPEN_PATTERN = (
    "...11111111111" +
    "...1.........1" +
    "...1.........1" +
    "11111111111..1" +
    "1.........1..1" +
    "1.........1..1" +
    "1.........1..1" +
    "1.........1..1" +
    "1.........1..1" +
    "1.........1..1" +
    "1.........1111" +
    "1.........1..." +
    "1.........1..." +
    "11111111111...");
  public static Image LNF_OPEN_IMAGE;
  public static Image LNF_OPEN_DISABLED_IMAGE;

  public static final String LNF_PROTECT_PATTERN = (
    "11111111111111" +
    "1............1" +
    "1..........111" +
    "1..........1.." +
    "1..........1.." +
    "1.....11...111" +
    "1....1..1....1" +
    "1....1..1....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "11111111111111");
  public static Image LNF_PROTECT_IMAGE;
  public static Image LNF_PROTECT_DISABLED_IMAGE;

  public static final String LNF_PROTECT_SELECTED_PATTERN = (
    "11111111111111" +
    "1............1" +
    "1............1" +
    "1............1" +
    "1............1" +
    "1.....11.....1" +
    "1....1..1....1" +
    "1....1..1....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "1.....11.....1" +
    "11111111111111");
  public static Image LNF_PROTECT_SELECTED_IMAGE;
  public static Image LNF_PROTECT_DISABLED_SELECTED_IMAGE;

  public static final String LNF_HD_PATTERN = (
    ".............." +
    "....111111...." +
    ".111......111." +
    "1............1" +
    "1............1" +
    ".111......111." +
    "1...111111...1" +
    "1............1" +
    ".111......111." +
    "1...111111...1" +
    "1............1" +
    ".111......111." +
    "....111111...." +
    "..............");
  public static ImageIcon LNF_HD_ICON;
  public static ImageIcon LNF_HD_DISABLED_ICON;

  public static final String LNF_MO_PATTERN = (
    "...11111111111" +
    "..1..........1" +
    ".1...1111....1" +
    "1...1....1...1" +
    "1..1......1..1" +
    "1.1...11...1.1" +
    "1.1..1..1..1.1" +
    "1.1..1..1..1.1" +
    "1.1...11...1.1" +
    "1..1......1..1" +
    "1...1....1...1" +
    "1....1111....1" +
    "1............1" +
    "11111111111111");
  public static ImageIcon LNF_MO_ICON;
  public static ImageIcon LNF_MO_DISABLED_ICON;

  public static final String LNF_CD_PATTERN = (
    ".....1111....." +
    "...11....11..." +
    "..1........1.." +
    ".1..........1." +
    ".1....11....1." +
    "1....1..1....1" +
    "1...1....1...1" +
    "1...1....1...1" +
    "1....1..1....1" +
    ".1....11....1." +
    ".1..........1." +
    "..1........1.." +
    "...11....11..." +
    ".....1111.....");
  public static ImageIcon LNF_CD_ICON;
  public static ImageIcon LNF_CD_DISABLED_ICON;

  //lnfInit ()
  //  Look&Feelを初期化する
  //  既存のコンポーネントのUIを切り替えると部分的に更新されず汚くなることがあるのでコンポーネントを作る前に行うこと
  //  既存のコンポーネントのUIを切り替える方法
  //    SwingUtilities.updateComponentTreeUI (rootPaneContainer.getRootPane ());
  public static void lnfInit () {

    if (false) {
      //利用可能なすべてのLook&Feelを表示する
      //  UIManager.setLookAndFeel(info.getClassName())とするとLook&Feelが変更される
      //  Metal以外はJavaのバージョンによって位置が異なる場合があるらしい
      System.out.println ("\n[UIManager.getInstalledLookAndFeels()]");
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels ()) {
        System.out.println ("        //  " + info.getName () + " = " + info.getClassName ());
        //  Metal = javax.swing.plaf.metal.MetalLookAndFeel
        //  Nimbus = javax.swing.plaf.nimbus.NimbusLookAndFeel
        //  CDE/Motif = com.sun.java.swing.plaf.motif.MotifLookAndFeel
        //  Windows = com.sun.java.swing.plaf.windows.WindowsLookAndFeel
        //  Windows Classic = com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel
      }
    }
    if (false) {
      //UIDefaultsをダンプする
      System.out.println ("\n[UIManager.getDefaults()]");
      TreeMap<String,String> m = new TreeMap<String,String> ();
      //UIManager.getDefaults ().forEach ((k, v) -> m.put (k.toString (), v.toString ()));  //UIManager.getDefaults()はHashtable<Object,Object>
      //なぜかUIManager.getDefaults().forEach(BiConsumer)がBiConsumerを1回も呼び出さずに終了してしまう
      for (Map.Entry<Object,Object> e : UIManager.getDefaults ().entrySet ()) {
        m.put (e.getKey ().toString (), e.getValue ().toString ());
      }
      m.forEach ((k, v) -> System.out.println (k + " = " + v));
    }

    //色
    for (int i = 0; i <= 14; i++) {
      LNF_RGB[i] = Color.HSBtoRGB ((float) (lnfH0 * (14 - i) + lnfH1 * i) / 14000F,
                                   (float) (lnfS0 * (14 - i) + lnfS1 * i) / 14000F,
                                   (float) (lnfB0 * (14 - i) + lnfB1 * i) / 14000F);
      if (false) {
        System.out.printf ("        //  LNF_RGB[%2d] = 0x%08x\n", i, LNF_RGB[i]);
        //  LNF_RGB[ 0] = 0xff000000
        //  LNF_RGB[ 1] = 0xff060612
        //  LNF_RGB[ 2] = 0xff0d0d24
        //  LNF_RGB[ 3] = 0xff151537
        //  LNF_RGB[ 4] = 0xff1e1e49
        //  LNF_RGB[ 5] = 0xff28285b
        //  LNF_RGB[ 6] = 0xff34346d
        //  LNF_RGB[ 7] = 0xff404080
        //  LNF_RGB[ 8] = 0xff4d4d92
        //  LNF_RGB[ 9] = 0xff5b5ba4
        //  LNF_RGB[10] = 0xff6b6bb6
        //  LNF_RGB[11] = 0xff7b7bc8
        //  LNF_RGB[12] = 0xff8d8ddb
        //  LNF_RGB[13] = 0xff9f9fed
        //  LNF_RGB[14] = 0xffb3b3ff
      }
    }
    lnfSecondary3 = new ColorUIResource (LNF_RGB[0]);
    lnfWhite      = new ColorUIResource (LNF_RGB[2]);
    lnfPrimary3   = new ColorUIResource (LNF_RGB[4]);
    lnfPrimary2   = new ColorUIResource (LNF_RGB[6]);
    lnfSecondary2 = new ColorUIResource (LNF_RGB[8]);
    lnfPrimary1   = new ColorUIResource (LNF_RGB[10]);
    lnfSecondary1 = new ColorUIResource (LNF_RGB[12]);
    lnfBlack      = new ColorUIResource (LNF_RGB[14]);
    //フォント
    lnfFont10 = new FontUIResource ("Dialog", Font.PLAIN, 10);
    lnfFont12 = new FontUIResource ("Dialog", Font.PLAIN, 12);
    //Look&Feel
    //  アプレットのインスタンスはLook&Feelを設定する前に作られているので背景が黒になっていない
    //  ウインドウに合わせるモードでリサイズしたときにちらつくので個別に背景色を設定する必要がある
    if (XEiJ.prgIsApplet) {
      XEiJApplet.appApplet.setBackground (new Color (LNF_RGB[0]));
    }
    JFrame.setDefaultLookAndFeelDecorated (true);
    JDialog.setDefaultLookAndFeelDecorated (true);
    MetalLookAndFeel.setCurrentTheme (new XEiJTheme ());
    try {
      UIManager.setLookAndFeel (new MetalLookAndFeel ());
    } catch (UnsupportedLookAndFeelException ulafe) {
    }

    //アイコン
    for (int i = 0; i < LNF_NUMBER_PATTERN_ARRAY.length; i++) {
      LNF_NUMBER_IMAGE_ARRAY[i] = XEiJ.createImage (14, 14, LNF_NUMBER_PATTERN_ARRAY[i], LNF_RGB[0], LNF_RGB[6]);
      LNF_NUMBER_SELECTED_IMAGE_ARRAY[i] = XEiJ.createImage (14, 14, LNF_NUMBER_PATTERN_ARRAY[i], LNF_RGB[0], LNF_RGB[12]);
    }

    LNF_EJECT_IMAGE = XEiJ.createImage (14, 14, LNF_EJECT_PATTERN, LNF_RGB[0], LNF_RGB[12]);
    LNF_EJECT_DISABLED_IMAGE = XEiJ.createImage (14, 14, LNF_EJECT_PATTERN, LNF_RGB[0], LNF_RGB[6]);

    LNF_OPEN_IMAGE = XEiJ.createImage (14, 14, LNF_OPEN_PATTERN, LNF_RGB[0], LNF_RGB[12]);
    LNF_OPEN_DISABLED_IMAGE = XEiJ.createImage (14, 14, LNF_OPEN_PATTERN, LNF_RGB[0], LNF_RGB[6]);

    LNF_PROTECT_IMAGE = XEiJ.createImage (14, 14, LNF_PROTECT_PATTERN, LNF_RGB[0], LNF_RGB[12]);
    LNF_PROTECT_DISABLED_IMAGE = XEiJ.createImage (14, 14, LNF_PROTECT_PATTERN, LNF_RGB[0], LNF_RGB[6]);
    LNF_PROTECT_SELECTED_IMAGE = XEiJ.createImage (14, 14, LNF_PROTECT_SELECTED_PATTERN, LNF_RGB[0], LNF_RGB[12]);
    LNF_PROTECT_DISABLED_SELECTED_IMAGE = XEiJ.createImage (14, 14, LNF_PROTECT_SELECTED_PATTERN, LNF_RGB[0], LNF_RGB[6]);

    LNF_HD_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_HD_PATTERN, LNF_RGB[0], LNF_RGB[12]));
    LNF_HD_DISABLED_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_HD_PATTERN, LNF_RGB[0], LNF_RGB[6]));

    LNF_MO_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_MO_PATTERN, LNF_RGB[0], LNF_RGB[12]));
    LNF_MO_DISABLED_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_MO_PATTERN, LNF_RGB[0], LNF_RGB[6]));

    LNF_CD_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_CD_PATTERN, LNF_RGB[0], LNF_RGB[12]));
    LNF_CD_DISABLED_ICON = new ImageIcon (XEiJ.createImage (14, 14, LNF_CD_PATTERN, LNF_RGB[0], LNF_RGB[6]));

  }  //lnfInit()



  //$$XET XEiJのテーマ
  public static class XEiJTheme extends MetalTheme {

    //名前
    @Override public String getName () {
      return "XEiJ";
    }  //getName()

    //色
    //  背景を黒にする
    //  以下の順序で明るさを変化させると綺麗に見える
    //  secondary3
    //    アクティブでないウインドウのタイトルバーの背景
    //    メニューバーの背景
    //    コンテンツの背景
    //  white
    //    ウインドウのタイトルバーのドットの明るい部分(左上)
    //    クローズボタンなどの溝の明るい部分
    //    メニューのボーダーの明るい部分
    //  primary3
    //    アクティブなウインドウのタイトルバーの背景
    //  primary2
    //    アクティブなウインドウの枠の溝の明るい部分
    //    アクティブなウインドウのメニューバーの上のボーダー
    //    選択されているメニューの背景
    //  secondary2
    //    アクティブでないウインドウの枠の溝の明るい部分
    //    アクティブでないウインドウのメニューバーの上のボーダー
    //    メニューバーの下のボーダー
    //  primary1
    //    アクティブなウインドウの枠
    //    アクティブなウインドウのクローズボタンなどの溝の底
    //  secondary1
    //    アクティブでないウインドウの枠
    //    アクティブでないウインドウのクローズボタンなどの溝の底
    //    アクティブでないウインドウのタイトルバーのドットの暗い部分(右下)
    //  black
    //    ウインドウの枠の溝の暗い部分
    //    クローズボタンなどの溝の暗い部分
    //    タイトルバーの文字
    //    メニューの文字
    @Override protected ColorUIResource getSecondary3 () {
      return lnfSecondary3;
    }  //getSecondary3()
    @Override protected ColorUIResource getWhite () {
      return lnfWhite;
    }  //getWhite()
    @Override protected ColorUIResource getPrimary3 () {
      return lnfPrimary3;
    }  //getPrimary3()
    @Override protected ColorUIResource getPrimary2 () {
      return lnfPrimary2;
    }  //getPrimary2()
    @Override protected ColorUIResource getSecondary2 () {
      return lnfSecondary2;
    }  //getSecondary2()
    @Override protected ColorUIResource getPrimary1 () {
      return lnfPrimary1;
    }  //getPrimary1()
    @Override protected ColorUIResource getSecondary1 () {
      return lnfSecondary1;
    }  //getSecondary1()
    @Override protected ColorUIResource getBlack () {
      return lnfBlack;
    }  //getBlack()

    //フォント
    //  MetalThemeのデフォルトはボールドだがプレーンに変更する
    //    メニューやボタンの文字がボールドだと日本語のとき読みにくい
    @Override public FontUIResource getControlTextFont () {
      return lnfFont12;
    }  //getControlTextFont()
    @Override public FontUIResource getMenuTextFont () {
      return lnfFont12;
    }  //getMenuTextFont()
    @Override public FontUIResource getSubTextFont () {
      return lnfFont10;
    }  //getSubTextFont()
    @Override public FontUIResource getSystemTextFont () {
      return lnfFont12;
    }  //getSystemTextFont()
    @Override public FontUIResource getUserTextFont () {
      return lnfFont12;
    }  //getUserTextFont()
    @Override public FontUIResource getWindowTitleFont () {
      return lnfFont12;
    }  //getWindowTitleFont()

    //カスタム
    @Override public void addCustomEntriesToTable (UIDefaults table) {
      super.addCustomEntriesToTable (table);
      table.putDefaults (new Object[] {
        //ボタン
        //  隙間を詰める
        "Button.margin", new InsetsUIResource (1, 7, 1, 7),  //2,14,2,14
        //アイコン
        //  ウインドウのタイトルバーの左端のアイコンはこれだけで変更できる
        //  タスクバーのアイコンはこれだけでは変更できない
        //    おそらく変更する前にコピーされている
        //  メインのウインドウだけwindow.setIconImage(LNF_ICON_IMAGE_16)などと書くことにする
        "InternalFrame.icon", new IconUIResource (new ImageIcon (LNF_ICON_IMAGE_16)),
      });
    }  //addCustomEntriesToTable(UIDefaults)

  }  //class XEiJTheme



}  //class LnF



