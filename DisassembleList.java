//========================================================================================
//  DisassembleList.java
//    en:Disassemble list
//    ja:逆アセンブルリスト
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,TimeZone,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class DisassembleList {

  public static final int DDP_ITEM_SIZE = 0x00000002;  //行の最小サイズ
  public static final int DDP_PAGE_SIZE = 0x00000400;  //ページのサイズ
  public static final int DDP_ITEM_MASK = -DDP_ITEM_SIZE;
  public static final int DDP_PAGE_MASK = -DDP_PAGE_SIZE;
  public static final int DDP_MAX_ITEMS = DDP_PAGE_SIZE / DDP_ITEM_SIZE + 2;  //ページの最大項目数。先頭と末尾の番兵を含む

  public static final char[] DDP_MOVEQD0_BASE = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "moveq.l #$xx,d0").toCharArray ();
  public static final char[] DDP_DCW_BASE = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    ".dc.w   $xxxx").toCharArray ();

  public static int ddpItemCount;  //ページに含まれる項目の数。先頭と末尾の番兵を含む。0=構築前または再構築要求
  public static int ddpItemIndex;  //キャレットがある項目の番号
  public static int ddpItemAddress;  //キャレットがある項目の先頭アドレス
  public static int ddpPageAddress;  //ページの先頭アドレス
  public static final int[] ddpAddressArray = new int[DDP_MAX_ITEMS];  //項目の先頭アドレスの配列。先頭は前のページの末尾、末尾は次のページの先頭。スピナーのヒント
  public static final int[] ddpSplitArray = new int[DDP_MAX_ITEMS];  //項目を区切る位置の配列。先頭は0
  public static final int[] ddpCaretArray = new int[DDP_MAX_ITEMS];  //項目が選択されたときキャレットを移動させる位置の配列。行の手前にヘッダやラベルなどを挿入しないときはddpSplitArrayと同じ
  public static final boolean[] ddpDCWArray = new boolean[DDP_MAX_ITEMS];  //項目毎の.dc.wで出力したかどうかのフラグの配列。true=数ワード後にある表示しなければならないアドレスを跨がないために逆アセンブルせず.dc.wで出力した。この行がクリックされたとき逆アセンブルし直す

  public static JFrame ddpFrame;  //ウインドウ
  public static ScrollTextArea ddpBoard;  //スクロールテキストエリア
  public static JTextArea ddpTextArea;  //テキストエリア

  public static Hex8Spinner ddpSpinner;  //スピナー

  public static int ddpPopupAddress;  //クリックされた行のアドレス

  public static boolean ddpBacktraceOn;  //true=バックトレース
  public static long ddpBacktraceRecord;  //現在選択されている分岐レコードの通し番号。-1L=未選択
  public static SpinnerNumberModel ddpBacktraceModel;  //バックトレーススピナーのモデル。値はLong
  public static JSpinner ddpBacktraceSpinner;  //バックトレーススピナー
  public static JCheckBox ddpBacktraceCheckBox;  //バックトレースチェックボックス

  public static String ddpStoppedBy;  //停止理由
  public static int ddpStoppedAddress;  //停止位置

  public static int ddpSupervisorMode;  //0=ユーザモード,0以外=スーパーバイザモード
  public static JCheckBox ddpSupervisorCheckBox;  //ユーザ/スーパーバイザチェックボックス

  //ddpInit ()
  //  初期化
  public static void ddpInit () {

    ddpItemCount = 0;  //構築前
    ddpItemIndex = 0;
    ddpItemAddress = -1;
    ddpPageAddress = 0;
    ddpSupervisorMode = 1;
    //ddpAddressArray = new int[DDP_MAX_ITEMS];
    //ddpSplitArray = new int[DDP_MAX_ITEMS];
    //ddpCaretArray = new int[DDP_MAX_ITEMS];
    //ddpDCWArray = new boolean[DDP_MAX_ITEMS];

    ddpFrame = null;

  }  //ddpInit()

  //ddpStart ()
  public static void ddpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DDP_FRAME_KEY)) {
      ddpOpen (-1, -1, true);
    }
  }  //ddpStart()

  //ddpOpen (address, supervisor, forceUpdate)
  //  逆アセンブルリストウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void ddpOpen (int address, int supervisor, boolean forceUpdate) {
    if (ddpFrame == null) {
      ddpMake ();
    }
    ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
    ddpUpdate (address, supervisor, forceUpdate);
    ddpFrame.setVisible (true);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_DDP_VISIBLE_MASK;
  }  //ddpOpen(int,int,boolean)

  //ddpMake ()
  //  逆アセンブルリストウインドウを作る
  public static void ddpMake () {

    //スクロールテキストエリア
    ddpBoard = XEiJ.setPreferredSize (
      XEiJ.setFont (new ScrollTextArea (), new Font ("Monospaced", Font.PLAIN, 12)),
      500, 400);
    ddpBoard.setMargin (new Insets (2, 4, 2, 4));
    ddpBoard.setHighlightCursorOn (true);
    ddpTextArea = ddpBoard.getTextArea ();
    ddpTextArea.setEditable (false);

    //スピナー
    ddpSpinner = XEiJ.createHex8Spinner (ddpPageAddress, DDP_ITEM_MASK, true, new ChangeListener () {
      //スピナーのチェンジリスナー
      //  スピナーが操作されたとき、そのアドレスの行にテキストエリアのキャレットを移動させる
      //  ページの範囲外になったときはテキストエリアを再構築する
      //  ページの構築中に呼び出されたときは何もしない
      @Override public void stateChanged (ChangeEvent ce) {
        if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
          ddpUpdate (ddpSpinner.getIntValue (), ddpSupervisorMode, false);
        }
      }
    });

    //テキストエリアのキャレットリスナー
    //  テキストエリアがクリックされてキャレットが動いたとき、その行のアドレスをスピナーに設定する
    //  クリックでテキストエリアに移ってしまったフォーカスをスピナーに戻す
    //  ページの構築中に呼び出されたときは何もしない
    //    setText→キャレットリスナー→スピナーのチェンジリスナー→setTextとなるとsetTextの二重呼び出しでエラーが出る
    XEiJ.addRemovableListener (
      ddpTextArea,
      new CaretListener () {
        @Override public void caretUpdate (CaretEvent ce) {
          if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
            int p = ce.getDot ();  //キャレットの位置
            if (p == ce.getMark ()) {  //選択範囲がない
              int i = Arrays.binarySearch (ddpSplitArray, 1, ddpItemCount, p + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
              i = (i >> 31 ^ i) - 1;  //キャレットがある位置を含む項目の番号
              ddpSpinner.setHintIndex (i);
            }
          }
        }
      });

    //テキストエリアのマウスリスナー
    XEiJ.addRemovableListener (
      ddpTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, ddpTextArea, true);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, ddpTextArea, true);
          }
        }
      });

    //ボタンのアクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Backtrace":
          if (BranchLog.BLG_ON) {
            ddpBacktraceOn = ((JCheckBox) ae.getSource ()).isSelected ();
            if (XEiJ.dbgEventMask == 0) {
              ddpUpdate (ddpAddressArray[ddpItemIndex], ddpSupervisorMode, true);
            }
          }
          break;
        case "User/Supervisor":  //ユーザ/スーパーバイザ
          if (XEiJ.dbgEventMask == 0) {
            ddpUpdate (ddpAddressArray[ddpItemIndex], ((JCheckBox) ae.getSource ()).isSelected () ? 1 : 0, true);
          }
          break;
        }
      }
    };

    //バックトレース
    if (BranchLog.BLG_ON) {
      ddpBacktraceOn = false;
      ddpBacktraceRecord = -1L;  //未選択

      //バックトレーススピナー
      ddpBacktraceModel = new ReverseLongModel (0L, 0L, 0L, 1L);
      ddpBacktraceSpinner = XEiJ.createNumberSpinner (ddpBacktraceModel, 15, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (XEiJ.dbgEventMask == 0 && XEiJ.mpuTask == null) {  //MPU停止中
            long record = ddpBacktraceModel.getNumber ().longValue ();
            int i = (char) record << BranchLog.BLG_RECORD_SHIFT;
            if (//ddpBacktraceRecord < 0L ||  //現在選択されている分岐レコードがない
                ddpBacktraceRecord < record) {  //後ろへ移動する
              ddpBacktraceRecord = record;
              ddpUpdate (BranchLog.blgArray[i] & ~1, BranchLog.blgArray[i] & 1, false);  //分岐レコードの先頭
            } else if (record < ddpBacktraceRecord) {  //手前へ移動する
              ddpBacktraceRecord = record;
              ddpUpdate (BranchLog.blgArray[i + 1], BranchLog.blgArray[i] & 1, false);  //分岐レコードの末尾
            }
          }
        }
      });

      //バックトレースチェックボックス
      ddpBacktraceCheckBox =
        Multilingual.mlnToolTipText (
          XEiJ.createIconCheckBox (
            ddpBacktraceOn,
            XEiJ.createImage (
              20, 14,
              "22222222222222222222" +
              "2..................2" +
              "2.......1..........2" +
              "2......1.1.........2" +
              "2.....1...1........2" +
              "2....111.111.......2" +
              "2......1.1.........2" +
              "2......1.111111....2" +
              "2......1......1....2" +
              "2......111111.1....2" +
              "2...........1.1....2" +
              "2...........111....2" +
              "2..................2" +
              "22222222222222222222",
              LnF.LNF_RGB[0],
              LnF.LNF_RGB[6],
              LnF.LNF_RGB[12]),
            XEiJ.createImage (
              20, 14,
              "22222222222222222222" +
              "2..................2" +
              "2.......1..........2" +
              "2......1.1.........2" +
              "2.....1...1........2" +
              "2....111.111.......2" +
              "2......1.1.........2" +
              "2......1.111111....2" +
              "2......1......1....2" +
              "2......111111.1....2" +
              "2...........1.1....2" +
              "2...........111....2" +
              "2..................2" +
              "22222222222222222222",
              LnF.LNF_RGB[0],
              LnF.LNF_RGB[12],
              LnF.LNF_RGB[12]),
            "Backtrace", listener),
          "ja", "バックトレース");
    }

    //スーパーバイザチェックボックス
    ddpSupervisorCheckBox =
      Multilingual.mlnToolTipText (
        XEiJ.createIconCheckBox (
          ddpSupervisorMode != 0,
          XEiJ.createImage (
            20, 14,
            "22222222222222222222" +
            "2..................2" +
            "2..................2" +
            "2.....11....11.....2" +
            "2.....11....11.....2" +
            "2.....11....11.....2" +
            "2.....11....11.....2" +
            "2.....11....11.....2" +
            "2.....11....11.....2" +
            "2.....11111111.....2" +
            "2.....11111111.....2" +
            "2..................2" +
            "2..................2" +
            "22222222222222222222",
            LnF.LNF_RGB[0],
            LnF.LNF_RGB[12],
            LnF.LNF_RGB[12]),
          XEiJ.createImage (
            20, 14,
            "22222222222222222222" +
            "2..................2" +
            "2..................2" +
            "2.....11111111.....2" +
            "2.....11111111.....2" +
            "2.....11...........2" +
            "2.....11111111.....2" +
            "2.....11111111.....2" +
            "2...........11.....2" +
            "2.....11111111.....2" +
            "2.....11111111.....2" +
            "2..................2" +
            "2..................2" +
            "22222222222222222222",
            LnF.LNF_RGB[0],
            LnF.LNF_RGB[12],
            LnF.LNF_RGB[12]),
          "User/Supervisor", listener),
        "ja", "ユーザ/スーパーバイザ");

    //ウインドウ
    ddpFrame = Multilingual.mlnTitle (
      XEiJ.createRestorableSubFrame (
        Settings.SGS_DDP_FRAME_KEY,
        "Disassemble List",
        null,
        XEiJ.createBorderPanel (
          ddpBoard,
          XEiJ.createHorizontalBox (
            ddpSpinner,
            ddpSupervisorCheckBox,
            Box.createHorizontalStrut (12),
            BranchLog.BLG_ON ? XEiJ.createHorizontalBox (ddpBacktraceCheckBox,
                                                         ddpBacktraceSpinner,
                                                         Box.createHorizontalStrut (12)) : null,
            Box.createHorizontalGlue (),
            XEiJ.mpuMakeBreakButton (),  //停止ボタン
            XEiJ.mpuMakeTraceButton (),  //トレース実行ボタン
            XEiJ.mpuMakeStepButton (),  //ステップ実行ボタン
            XEiJ.mpuMakeRunButton ()  //実行ボタン
            )
          )
        ),
      "ja", "逆アセンブルリスト");
    XEiJ.addRemovableListener (
      ddpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_DDP_VISIBLE_MASK;
        }
      });

    ddpStoppedBy = null;
    ddpStoppedAddress = -1;

  }  //ddpMake()

  //ddpUpdate (address, supervisor, forceUpdate)
  //  逆アセンブルリストウインドウを更新する
  //  address  -1=pcまたはpc0を表示,0=前回と同じアドレスを表示
  public static void ddpUpdate (int address, int supervisor, boolean forceUpdate) {

    XEiJ.dbgEventMask++;  //構築開始

    if (address == -1) {  //pcまたはpc0を表示
      ddpStoppedAddress = address = ddpStoppedBy == null ? XEiJ.regPC : XEiJ.regPC0;
      forceUpdate = true;
    } else if (address == 0) {  //前回と同じアドレスを表示。同じアドレスで再構築したいときに使う
      address = ddpItemAddress;
    }

    if (supervisor == -1) {
      supervisor = XEiJ.regSRS;
      forceUpdate = true;
    }

    if ((ddpSupervisorMode != 0) != (supervisor != 0)) {  //ユーザ/スーパーバイザが一致しない
      ddpSupervisorMode = supervisor;
      forceUpdate = true;
      if (ddpSupervisorCheckBox.isSelected () != (supervisor != 0)) {
        ddpSupervisorCheckBox.setSelected (supervisor != 0);
      }
    }

    if (forceUpdate) {  //再構築要求
      ddpItemCount = 0;
    }

    address &= DDP_ITEM_MASK;  //目的のアドレスを含む項目の先頭アドレス

    //バックトレース
    if (BranchLog.BLG_ON) {
      if (XEiJ.mpuTask == null) {  //MPU停止中
        long newestRecord = BranchLog.blgNewestRecord;  //最新のレコードの番号
        long oldestRecord = Math.max (0L, newestRecord - 65535);  //最古のレコードの番号
        if (//ddpBacktraceRecord < 0L ||  //レコードが選択されていない
            ddpBacktraceRecord < oldestRecord || newestRecord < ddpBacktraceRecord) {  //選択されているレコードが存在しない
          ddpBacktraceRecord = newestRecord;  //最新のレコードを選択する
          ddpBacktraceModel.setMaximum (new Long (newestRecord));
          ddpBacktraceModel.setValue (new Long (newestRecord));
        }
        if (ddpBacktraceOn) {  //バックトレースモードのとき
          int i = (char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT;
          if (address >>> 1 < BranchLog.blgArray[i] >>> 1) {  //現在選択されているレコードよりも前
            if (oldestRecord < ddpBacktraceRecord) {  //直前にレコードがある
              ddpBacktraceRecord--;  //直前のレコードを選択する
              ddpBacktraceModel.setValue (new Long (ddpBacktraceRecord));
              address = BranchLog.blgArray[((char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT) + 1] & ~1;  //直前のレコードの末尾に移動する
            }
          } else if (BranchLog.blgArray[i + 1] >>> 1 < address >>> 1) {  //現在選択されているレコードよりも後
            if (ddpBacktraceRecord < newestRecord) {  //直後にレコードがある
              ddpBacktraceRecord++;  //直後のレコードを選択する
              ddpBacktraceModel.setValue (new Long (ddpBacktraceRecord));
              address = BranchLog.blgArray[(char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT] & ~1;  //直後のレコードの先頭に移動する
            }
          }
        }
      }
    }

    if (ddpItemCount != 0) {  //構築前または再構築要求のいずれでもない
      int i = Arrays.binarySearch (ddpAddressArray, 1, ddpItemCount, address + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
      i = (i >> 31 ^ i) - 1;  //目的のアドレスを含む項目の番号
      if (0 < i && i < ddpItemCount - 1 &&  //ページの内側
          ddpAddressArray[i] == address &&  //項目の先頭
          !ddpDCWArray[i]) {  //.dc.wで出力されていない

        //再構築しない

        ddpItemAddress = address;
        if (ddpItemIndex != i) {  //キャレットがある項目を変更する必要がある
          ddpItemIndex = i;
          ddpTextArea.setCaretPosition (ddpCaretArray[i]);
        }

        //!
        //バックトレースモードのとき分岐レコードの範囲が変わったときはハイライト表示を更新する

        XEiJ.dbgEventMask--;  //構築終了
        return;
      }
    }

    //再構築する
    ddpItemAddress = address;

    //構築前または再構築要求または先頭または末尾の番兵が選択された
    //  0x00000000の境界を跨ぐとき反対側を指すことがあるので先頭と末尾の番兵を区別しない
    ddpPageAddress = address & DDP_PAGE_MASK;  //ページの先頭アドレス
    int pageEndAddress = ddpPageAddress + DDP_PAGE_SIZE;  //ページの末尾アドレス。0になることがある

    //先頭の番兵
    ddpAddressArray[0] = ddpPageAddress - DDP_ITEM_SIZE;  //昇順を維持するためマスクしない
    ddpSplitArray[0] = 0;
    ddpCaretArray[0] = 0;
    StringBuilder sb = new StringBuilder (
      //         1111111111222222222233333333334444444444555555555566666666667777777777
      //1234567890123456789012345678901234567890123456789012345678901234567890123456789
      //xxxxxxx  xxxxxxxxxxxxxxxxxxxx  ssssssssssssssssssssssssssssssssssss..........
      //                               move.b  100(a0,d0.l),100(a0,d0.l)
      "          +0+1+2+3+4+5+6+7+8+9                                              ▲\n");
    int itemCount = 1;  //項目数
    int itemAddress = ddpPageAddress;  //項目の先頭アドレス
    int dcwAddress = pageEndAddress;  //.dc.wで出力する範囲
    int dcwEndAddress = pageEndAddress;
    boolean prevBranchFlag = false;  //true=直前が完全分岐命令だった

    //ラベル
    LabeledAddress.lblUpdate ();

    TreeMap<Integer,InstructionBreakPoint.InstructionBreakRecord> pointTable;
    if (InstructionBreakPoint.IBP_ON) {
      pointTable = supervisor != 0 ? InstructionBreakPoint.ibpSuperPointTable : InstructionBreakPoint.ibpUserPointTable;
    }

  itemLoop:
    do {
      int itemEndAddress;  //項目の末尾アドレス
      String code;  //逆アセンブル結果

      //逆アセンブルする
      //  以下のアドレスを跨いでしまうときは逆アセンブルせず1ワードずつ.dc.wまたはmoveqで出力する
      //    目的のアドレス
      //    命令ブレークポイント
      //  途中の行をクリックすることで途中から逆アセンブルし直せるようにするため、複数ワードあっても1行にまとめない
      //  途中に逆アセンブルできる命令があっても、跨いではいけないアドレスまですべて1ワードずつ出力する
      if (dcwAddress <= itemAddress && itemAddress < dcwEndAddress) {  //.dc.wで出力中
        Disassembler.disStatus = 0;  //念のため
        int oc = MC68060.mmuPeekWordZeroCode (itemAddress, supervisor);
        if ((oc & 0xfe00) == 0x7000 && MC68060.mmuPeekWordZeroCode (itemAddress + 2, supervisor) == 0x4e4f) {  //moveq.l #$xx,d0;trap#15
          //pcがIOCSコールのtrap#15を指しているときmoveqが.dc.wになってしまうのを避ける
          XEiJ.fmtHex2 (DDP_MOVEQD0_BASE, 10, oc);
          code = String.valueOf (DDP_MOVEQD0_BASE);
        } else {
          XEiJ.fmtHex4 (DDP_DCW_BASE, 9, oc);
          code = String.valueOf (DDP_DCW_BASE);
        }
        itemEndAddress = itemAddress + 2;
        ddpDCWArray[itemCount] = true;
      } else {  //.dc.wで出力中ではない
        code = Disassembler.disDisassemble (new StringBuilder (), itemAddress, supervisor).toString ();  //逆アセンブルする
        for (int t = itemAddress + 2; t < Disassembler.disPC; t += 2) {
          if (t == address ||  //目的のアドレスを跨いでしまった
              InstructionBreakPoint.IBP_ON && pointTable.containsKey (t)) {  //命令ブレークポイントを跨いでしまった
            //!
            //バックトレースモードのとき選択されている分岐レコードの先頭も跨がないようにする
            //  IOCSの_B_READと_B_WRITEでmoveq.l #<data>,d0だけ変更してtrap#15に飛び込んでいるところなど
            dcwAddress = itemAddress;  //.dc.wで出力し直す
            dcwEndAddress = t;
            continue itemLoop;
          }
        }
        itemEndAddress = Disassembler.disPC;
        ddpDCWArray[itemCount] = false;
      }

      //完全分岐命令の下に隙間を空けて読みやすくする
      if (prevBranchFlag) {
        sb.append ('\n');
      }

      //項目の開始
      if (itemAddress == address) {
        ddpItemIndex = itemCount;  //目的のアドレスを含む項目の番号
      }
      ddpAddressArray[itemCount] = itemAddress;  //項目の先頭アドレス
      ddpSplitArray[itemCount] = sb.length ();  //項目を区切る位置

      if (prevBranchFlag) {
        //ラベル
        if (true) {
          int i = sb.length ();
          LabeledAddress.lblSearch (sb, itemAddress);
          if (i < sb.length ()) {
            sb.append ('\n');
          }
        }
      }

      //停止理由
      if (itemAddress == ddpStoppedAddress && ddpStoppedBy != null) {
        sb.append (ddpStoppedBy).append ('\n');
      }

      ddpCaretArray[itemCount] = sb.length ();  //項目が選択されたときキャレットを移動させる位置

      //1行目
      int lineAddress = itemAddress;  //行の開始アドレス
      int lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
      //アドレス
      XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
      //データ
      for (int a = lineAddress; a < lineEndAddress; a += 2) {
        XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
      }
      sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + 2);
      //逆アセンブル結果
      sb.append (code).append (XEiJ.DBG_SPACES, 0, Math.max (1, 68 - 32 - code.length ()));
      //キャラクタ
      InstructionBreakPoint.InstructionBreakRecord r = InstructionBreakPoint.IBP_ON ? pointTable.get (itemAddress) : null;
      if (r != null) {  //命令ブレークポイントがある
        if (r.ibrThreshold < 0) {  //インスタント
          sb.append ("**********");
        } else {
          sb.append (r.ibrValue).append ('/').append (r.ibrThreshold);
        }
      } else {  //命令ブレークポイントがない
        for (int a = lineAddress; a < lineEndAddress; a++) {
          int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
          int c;
          if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
            int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
            if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
              c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
              if (c == 0) {  //対応する文字がない
                c = '※';
              }
              a++;
            } else {  //SJISの2バイトコードの2バイト目ではない
              c = '.';  //SJISの2バイトコードの1バイト目ではなかった
            }
          } else {  //SJISの2バイトコードの1バイト目ではない
            c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
            if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
              c = '.';
            }
          }
          sb.append ((char) c);
        }  //for a
      }
      sb.append ('\n');

      //2行目以降
      while (lineEndAddress < itemEndAddress) {
        lineAddress = lineEndAddress;  //行の開始アドレス
        lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
        //アドレス
        XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
        //データ
        for (int a = lineAddress; a < lineEndAddress; a += 2) {
          XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
        }
        sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + (2 + 68 - 32));
        //キャラクタ
        for (int a = lineAddress; a < lineEndAddress; a++) {
          int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
          int c;
          if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
            int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
            if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
              c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
              if (c == 0) {  //対応する文字がない
                c = '※';
              }
              a++;
            } else {  //SJISの2バイトコードの2バイト目ではない
              c = '.';  //SJISの2バイトコードの1バイト目ではなかった
            }
          } else {  //SJISの2バイトコードの1バイト目ではない
            c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
            if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
              c = '.';
            }
          }
          sb.append ((char) c);
        }  //for a
        sb.append ('\n');
      }

      //項目の終了
      itemCount++;
      itemAddress = itemEndAddress;

      //完全分岐命令の下に隙間を空けて読みやすくする
      prevBranchFlag = (Disassembler.disStatus & Disassembler.DIS_ALWAYS_BRANCH) != 0;

    } while (itemAddress < pageEndAddress);

    //末尾の番兵
    ddpAddressArray[itemCount] = itemAddress;  //昇順を維持するためマスクしない
    ddpSplitArray[itemCount] = sb.length ();
    ddpCaretArray[itemCount] = sb.length ();
    sb.append (
      //         1111111111222222222233333333334444444444555555555566666666667777777777
      //1234567890123456789012345678901234567890123456789012345678901234567890123456789
      "          +0+1+2+3+4+5+6+7+8+9                                              ▼");
    itemCount++;
    ddpItemCount = itemCount;

    //テキスト
    ddpTextArea.setText (sb.toString ());
    ddpTextArea.setCaretPosition (ddpCaretArray[ddpItemIndex]);

    //!
    //バックトレースモードのとき選択されている分岐レコードの範囲をハイライト表示する

    //スピナー
    ddpSpinner.setHintArray (ddpAddressArray, itemCount);
    ddpSpinner.setHintIndex (ddpItemIndex);

    XEiJ.dbgEventMask--;  //構築終了

  }  //ddpUpdate(int,int,boolean)

}  //class DisassembleList



