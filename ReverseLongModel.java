//========================================================================================
//  ReverseLongModel.java
//    en:Reverse long model -- It is a modified SpinnerNumberModel that has a Long value and reversely spins.
//    ja:リバースロングモデル -- SpinnerNumberModelの値をLongにして回転方向を逆にしたスピナーモデルです。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class ReverseLongModel extends SpinnerNumberModel {
  public ReverseLongModel (long value, long minimum, long maximum, long stepSize) {
    super (new Long(value), new Long (minimum), new Long (maximum), new Long (stepSize));
  }
  @Override public Object getNextValue () {
    return super.getPreviousValue ();
  }
  @Override public Object getPreviousValue () {
    return super.getNextValue ();
  }
}  //class ReverseLongModel



