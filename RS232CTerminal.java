//========================================================================================
//  RS232CTerminal.java
//    en:RS-232C terminal
//    ja:RS-232Cターミナル
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  SCCのRS-232Cを接続することでROMデバッガに対応
//  メッセージ表示と兼用
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.datatransfer.*;  //Clipboard,DataFlavor,FlavorEvent,FlavorListener,Transferable,UnsupportedFlavorException
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class RS232CTerminal {

  public static final int TRM_MAX_OUTPUT_LENGTH = 1024 * 64;  //出力の上限を64KBとする
  public static final int TRM_CUT_OUTPUT_LENGTH = TRM_MAX_OUTPUT_LENGTH + 1024 * 4;  //出力が上限よりも4KB以上長くなったら上限でカットする

  //コンポーネント
  public static JFrame trmFrame;  //ウインドウ
  public static ScrollTextArea trmBoard;  //テキストエリア
  public static JPopupMenu trmPopupMenu;  //ポップアップメニュー
  public static JMenuItem trmPopupCutMenuItem;  //切り取り
  public static JMenuItem trmPopupCopyMenuItem;  //コピー
  public static JMenuItem trmPopupPasteMenuItem;  //貼り付け
  public static JMenuItem trmPopupSelectAllMenuItem;  //すべて選択
  public static StringBuilder trmOutputBuilder;  //ターミナルを最初に開くまでに出力された文字列を貯めておくバッファ
  public static int trmOutputEnd;  //出力された文字列の末尾。リターンキーが押されたらこれ以降に書かれた文字列をまとめて入力する
  public static int trmOutputSJIS1;  //出力するときに繰り越したSJISの1バイト目

  //trmInit ()
  //  ターミナルウインドウを初期化する
  public static void trmInit () {
    trmFrame = null;
    trmBoard = null;
    trmPopupMenu = null;
    trmPopupCutMenuItem = null;
    trmPopupCopyMenuItem = null;
    trmPopupPasteMenuItem = null;
    trmPopupSelectAllMenuItem = null;
    trmOutputBuilder = new StringBuilder ();
    trmOutputEnd = 0;
    trmOutputSJIS1 = 0;
  }  //trmInit()

  //trmMake ()
  //  ターミナルウインドウを作る
  //  ここでは開かない
  public static void trmMake () {

    //テキストエリア
    trmBoard = XEiJ.createScrollTextArea (trmOutputBuilder.toString (), 650, 350, true);  //作る前に出力されていた文字列を設定する
    trmOutputBuilder = null;  //これはもういらない
    trmBoard.setUnderlineCursorOn (true);
    trmBoard.setLineWrap (true);  //行を折り返す
    trmBoard.addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (de.getOffset () < trmOutputEnd) {
          trmOutputEnd += de.getLength ();  //出力された文字列の末尾を調整する
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (de.getOffset () < trmOutputEnd) {
          trmOutputEnd -= Math.min (de.getLength (), trmOutputEnd - de.getOffset ());  //出力された文字列の末尾を調整する
        }
      }
    });
    trmBoard.addKeyListener (new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        if (ke.getKeyCode () == KeyEvent.VK_ENTER) {  //ENTERキーが押された
          ke.consume ();  //ENTERキーをキャンセルする
          trmEnter ();  //ENTERキーを処理する
        }
      }
    });

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Cut":
          trmCut ();
          break;
        case "Copy":
          trmCopy ();
          break;
        case "Paste":
          trmPaste ();
          break;
        case "Select All":
          trmSelectAll ();
          break;
        }
      }
    };
    trmPopupMenu = XEiJ.createPopupMenu (
      trmPopupCutMenuItem = Multilingual.mlnText (XEiJ.createMenuItem ("Cut", 'T', popupActionListener), "ja", "切り取り"),
      trmPopupCopyMenuItem = Multilingual.mlnText (XEiJ.createMenuItem ("Copy", 'C', popupActionListener), "ja", "コピー"),
      trmPopupPasteMenuItem = Multilingual.mlnText (XEiJ.createMenuItem ("Paste", 'P', popupActionListener), "ja", "貼り付け"),
      XEiJ.createHorizontalSeparator (),
      trmPopupSelectAllMenuItem = Multilingual.mlnText (XEiJ.createMenuItem ("Select All", 'A', popupActionListener), "ja", "すべて選択")
      );
    trmBoard.addMouseListener (new MouseAdapter () {
      @Override public void mousePressed (MouseEvent me) {
        trmShowPopup (me);
      }
      @Override public void mouseReleased (MouseEvent me) {
        trmShowPopup (me);
      }
    });

    //ウインドウ
    trmFrame = Multilingual.mlnTitle (
      XEiJ.createRestorableSubFrame (
        Settings.SGS_TRM_FRAME_KEY,
        "Terminal",
        null,
        trmBoard
        ),
      "ja", "ターミナル");

  }  //trmMake()

  //trmShowPopup (me)
  //  ポップアップメニューを表示する
  //  テキストエリアのマウスリスナーが呼び出す
  public static void trmShowPopup (MouseEvent me) {
    if (me.isPopupTrigger ()) {
      //選択範囲があれば切り取りとコピーが有効
      boolean enableCutAndCopy = XEiJ.clpClipboard != null && trmBoard.getSelectionStart () != trmBoard.getSelectionEnd ();
      XEiJ.setEnabled (trmPopupCutMenuItem, enableCutAndCopy);
      XEiJ.setEnabled (trmPopupCopyMenuItem, enableCutAndCopy);
      //クリップボードに文字列があれば貼り付けが有効
      XEiJ.setEnabled (trmPopupPasteMenuItem, XEiJ.clpClipboard != null && XEiJ.clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor));
      //クリップボードがあればすべて選択が有効
      XEiJ.setEnabled (trmPopupSelectAllMenuItem, XEiJ.clpClipboard != null);
      //ポップアップメニューを表示する
      trmPopupMenu.show (me.getComponent (), me.getX (), me.getY ());
    }
  }  //trmShowPopup(MouseEvent)

  //trmCut ()
  //  切り取り
  public static void trmCut () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      XEiJ.clpClipboardString = trmBoard.getSelectedText ();
      try {
        XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
        XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を削除する
      trmBoard.replaceRange ("", trmBoard.getSelectionStart (), trmBoard.getSelectionEnd ());
    }
  }  //trmCut()

  //trmCopy ()
  //  コピー
  public static void trmCopy () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      String selectedText = trmBoard.getSelectedText ();
      if (selectedText != null) {
        XEiJ.clpClipboardString = selectedText;
        try {
          XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
          XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
        } catch (Exception e) {
          return;
        }
      }
    }
  }  //trmCopy()

  //trmPaste ()
  //  貼り付け
  public static void trmPaste () {
    if (XEiJ.clpClipboard != null) {
      //クリップボードから文字列を取り出す
      String string = null;
      try {
        string = (String) XEiJ.clpClipboard.getData (DataFlavor.stringFlavor);
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を置換する
      trmBoard.replaceRange (string, trmBoard.getSelectionStart (), trmBoard.getSelectionEnd ());
    }
  }  //trmPaste()

  //trmSelectAll ()
  //  すべて選択
  public static void trmSelectAll () {
    if (XEiJ.clpClipboard != null) {
      //すべて選択する
      trmBoard.selectAll ();
    }
  }  //trmSelectAll()

  //trmStart ()
  public static void trmStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_TRM_FRAME_KEY)) {
      trmOpen ();
    }
  }  //trmStart()

  //trmOpen ()
  //  ターミナルウインドウを開く
  public static void trmOpen () {
    if (trmFrame == null) {
      trmMake ();
    }
    trmFrame.setVisible (true);
  }  //trmOpen()

  //trmPrintSJIS (d)
  //  SJISで1バイト追加する
  //  SJISの1バイト目は繰り越して2バイト目が来たときに表示する
  public static void trmPrintSJIS (int d) {
    d &= 0xff;
    if (trmOutputSJIS1 != 0) {  //前回SJISの1バイト目を繰り越した
      if (0x40 <= d && d != 0x7f && d <= 0xfc) {  //SJISの2バイト目が来た
        int c = CharacterCode.chrSJISToChar[trmOutputSJIS1 << 8 | d];  //2バイトで変換する
        if (c != 0) {  //2バイトで変換できた
          trmPrintChar (c);  //1文字表示する
        } else {  //2バイトで変換できなかった
          //2バイトで変換できなかったがSJISの1バイト目と2バイト目であることはわかっているので2バイト分のコードを表示する
          trmPrintChar ('[');
          trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 >> 4));
          trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 & 15));
          trmPrintChar (XEiJ.fmtHexc (d >> 4));
          trmPrintChar (XEiJ.fmtHexc (d & 15));
          trmPrintChar (']');
        }
        trmOutputSJIS1 = 0;
        return;
      }
      //SJISの2バイト目が来なかった
      //前回繰り越したSJISの1バイト目を吐き出す
      trmPrintChar ('[');
      trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 >> 4));
      trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 & 15));
      trmPrintChar (']');
      trmOutputSJIS1 = 0;
    }
    if (0x81 <= d && d <= 0x9f || 0xe0 <= d && d <= 0xef) {  //SJISの1バイト目が来た
      trmOutputSJIS1 = d;  //次回に繰り越す
    } else {  //SJISの1バイト目が来なかった
      int c = CharacterCode.chrSJISToChar[d];  //1バイトで変換する
      if (c != 0) {  //1バイトで変換できた
        trmPrintChar (c);  //1文字表示する
      } else {  //1バイトで変換できなかった
        //1バイトで変換できなかったがSJISの1バイト目でないことはわかっているので1バイト分のコードを表示する
        trmPrintChar ('[');
        trmPrintChar (XEiJ.fmtHexc (d >> 4));
        trmPrintChar (XEiJ.fmtHexc (d & 15));
        trmPrintChar (']');
      }
    }
  }  //trmPrintSJIS(int)

  //trmPrintChar (c)
  //  末尾に1文字追加する
  public static void trmPrintChar (int c) {
    if (c == 0x08) {  //バックスペース
      if (trmOutputEnd > 0) {
        if (trmBoard != null) {
          trmBoard.replaceRange ("", trmOutputEnd - 1, trmOutputEnd);  //1文字削除
          trmOutputEnd--;
          trmBoard.setCaretPosition (trmOutputEnd);
        } else {
          trmOutputBuilder.delete (trmOutputEnd - 1, trmOutputEnd);  //1文字削除
          trmOutputEnd--;
        }
      }
    } else if (c >= 0x20 && c != 0x7f || c == 0x09 || c == 0x0a) {  //タブと改行以外の制御コードを除く
      if (trmBoard != null) {
        trmBoard.insert (String.valueOf ((char) c), trmOutputEnd);  //1文字追加
        trmOutputEnd++;
        if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
          trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
        }
        trmBoard.setCaretPosition (trmOutputEnd);
      } else {
        trmOutputBuilder.append ((char) c);  //1文字追加
        trmOutputEnd++;
        if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
          trmOutputBuilder.delete (0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
        }
      }
    }
  }  //trmPrintChar(int)

  //trmPrint (s)
  //  末尾に文字列を追加する
  //  情報表示用
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void trmPrint (String s) {
    if (s == null) {
      return;
    }
    if (trmFrame != null) {
      trmBoard.insert (s, trmOutputEnd);  //文字列追加
      trmOutputEnd += s.length ();
      if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
        trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
      }
      trmBoard.setCaretPosition (trmOutputEnd);
    } else {
      trmOutputBuilder.append (s);  //文字列追加
      trmOutputEnd += s.length ();
      if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
        trmOutputBuilder.delete (0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
      }
    }
  }  //trmPrint(String)

  //trmPrintln (s)
  //  末尾に文字列と改行を追加する
  //  情報表示用
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void trmPrintln (String s) {
    trmPrint (s);
    trmPrintChar ('\n');
  }  //trmPrintln(String)

  //trmEnter ()
  //  ENTERキーを処理する
  public static void trmEnter () {
    String text = trmBoard.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    int outputLineStart = text.lastIndexOf ('\n', trmOutputEnd - 1) + 1;  //出力の末尾の行の先頭。プロンプトの先頭
    int caretLineStart = text.lastIndexOf ('\n', trmBoard.getCaretPosition () - 1) + 1;  //キャレットがある行の先頭
    if (outputLineStart <= caretLineStart) {  //出力の末尾の行の先頭以降でENTERキーが押された
      trmBoard.replaceRange ("", trmOutputEnd, length);  //入力された文字列を一旦削除する
      trmSend (text.substring (trmOutputEnd, length) + "\r");  //入力された文字列を送信する
    } else if (outputLineStart < trmOutputEnd) {  //出力の末尾の行の先頭よりも手前でENTERキーが押されて、出力の末尾の行にプロンプトがあるとき
      String prompt = text.substring (outputLineStart, trmOutputEnd);  //出力の末尾の行のプロンプト
      int caretLineEnd = text.indexOf ('\n', caretLineStart);  //キャレットがある行の末尾
      if (caretLineEnd == -1) {
        caretLineEnd = length;
      }
      String line = text.substring (caretLineStart, caretLineEnd);  //キャレットがある行
      int start = line.indexOf (prompt);  //キャレットがある行のプロンプトの先頭
      if (start >= 0) {  //キャレットがある行にプロンプトがあるとき
        trmOutputEnd = length;  //入力された文字列を無効化する
        if (text.charAt (trmOutputEnd - 1) != '\n') {  //改行で終わっていないとき
          trmBoard.insert ("\n", trmOutputEnd);  //末尾にENTERを追加する
          trmOutputEnd++;
          if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
            trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
            trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
          }
        }
        trmBoard.setCaretPosition (trmOutputEnd);
        trmSend (line.substring (start + prompt.length ()) + "\r");  //プロンプトの後ろから行の末尾までを送信する
      }
    }
  }  //trmEnter()

  //trmSend (s)
  //  文字列をSJISに変換しながらRS-232C受信に渡す
  //  割り込み要求が競合しないようにコアのスレッドで行う
  public static void trmSend (String s) {
    if (!"".equals (s)) {
      XEiJ.tmrTimer.schedule (new TRMTask (s), 0L);
    }
  }  //trmSend(String)



  //$$TRT ターミナルタスク
  public static class TRMTask extends TimerTask {
    public String string;
    public TRMTask (String s) {
      string = s;
    }
    @Override public void run () {
      int l = string.length ();
      for (int i = 0; i < l; i++) {
        int c = CharacterCode.chrCharToSJIS[string.charAt (i)];
        if (c != 0) {
          if (c >= 0x0100) {
            Z8530.scc1Receive (c >> 8);
          }
          Z8530.scc1Receive (c);
        }
      }
    }
  }  //class TRMTask



}  //class RS232CTerminal



