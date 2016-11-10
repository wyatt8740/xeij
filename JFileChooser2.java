//========================================================================================
//  JFileChooser2.java
//    en:JFileChooser2
//    ja:JFileChooser2
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  JFileChooserのバグ対策
//
//  JFileChooserのgetSelectedFile()の説明には
//    Returns the selected file. This can be set either by the programmer via setSelectedFile or by a user action,
//    such as either typing the filename into the UI or selecting the file from a list in the UI.
//  と書かれており、テキストフィールドに入力されたファイル名もgetSelectedFile()で取り出せることになっている
//  しかし、実際にはsetSelectedFile()で設定したかリストをクリックして選択したファイル名しか取り出すことができない
//  これでは新しいファイルを作れないだけでなく、
//  リストをクリックしてテキストフィールドに既存のファイル名を表示させた後にそれを書き換えて新規のファイル名を入力すると、
//  入力した新規のファイル名ではなくクリックした既存のファイル名が返るため、既存のファイルを破壊してしまう可能性がある
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import java.util.regex.*;  //Matcher,Pattern
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class JFileChooser2 extends JFileChooser {
  protected JTextField jfc2TextField;  //ファイル名のテキストフィールド
  private static final Pattern JFC2_FILE_NAME_PATTERN = Pattern.compile ("\\s*(?:,\\s*)*(?:\"([^\"]*)\"?|([^\",]+))");
  public JFileChooser2 (File file) {
    super (file);
    jfc2TextField = null;
    try {
      //ファイル名のテキストフィールドを求める
      //  ファイルチューザーの構造が異なると失敗する可能性がある
      jfc2TextField = (JTextField) ((Container) ((JPanel) getAccessibleContext ().getAccessibleChild (3)).getComponent (0)).getComponent (1);
    } catch (Exception e) {
    }
  }
  //file = getSelectedFile2 ()
  //  選択されたファイルを1個取り出す
  public File getSelectedFile2 () {
    if (jfc2TextField == null) {
      return getSelectedFile ();
    }
    File[] files = getSelectedFiles2 ();
    return files.length == 0 ? null : files[0];
  }
  //files = getSelectedFiles2 ()
  //  選択されたファイルの配列を取り出す
  public File[] getSelectedFiles2 () {
    if (jfc2TextField == null) {
      return getSelectedFiles ();
    }
    Matcher matcher = JFC2_FILE_NAME_PATTERN.matcher (jfc2TextField.getText ());
    File directory = getCurrentDirectory ();
    ArrayList<File> list = new ArrayList<File> ();
    while (matcher.find ()) {
      list.add (new File (directory, matcher.group (1) != null ? matcher.group (1) : matcher.group (2)));
      if (!isMultiSelectionEnabled ()) {  //複数選択可能でなければ最初の1個で終わり
        break;
      }
    }
    return list.toArray (new File[0]);  //配列にして返す
  }
}  //class JFileChooser2



