//========================================================================================
//  RasterBreakPoint.java
//    en:Raster break point -- It stops the MPU at the horizontal front porch just before the specified break raster or the IRQ raster.
//    ja:ラスタブレークポイント -- 指定されたブレークラスタまたはIRQラスタの直前の水平フロントポーチでMPUを止めます。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class RasterBreakPoint {

  public static final boolean RBP_ON = true;  //true=ラスタブレークポイント機能を有効にする

  //ラスタブレーク
  public static boolean rbpBreakEnabled;  //true=指定されたブレークラスタでラスタブレークをかける
  public static int rbpBreakRaster;  //ブレークラスタ。0～1023
  public static int rbpActiveBreakRaster;  //使用中のブレークラスタ。rbpBreakEnabled?rbpBreakRaster:-1
  public static boolean rbpIRQBreakEnabled;  //true=IRQラスタでラスタブレークをかける
  public static int rbpCountValue;  //回数
  public static int rbpThresholdValue;  //閾値

  //ウインドウ
  public static JFrame rbpFrame;
  public static JCheckBox rbpEnabledCheckBox;
  public static JCheckBox rbpIRQEnabledCheckBox;
  public static JLabel rbpStatusLabel;
  public static JTextField rbpCurrentRasterTextField;
  public static JTextField rbpIRQRasterTextField;
  public static SpinnerNumberModel rbpBreakModel;
  public static SpinnerNumberModel rbpCountModel;
  public static SpinnerNumberModel rbpThresholdModel;

  //タイマー
  public static final int RBP_INTERVAL = 10;
  public static int rbpTimer;

  //rbpInit ()
  //  初期化
  public static void rbpInit () {
    //ラスタブレーク
    rbpBreakEnabled = false;
    rbpBreakRaster = 0;
    rbpActiveBreakRaster = -1;
    rbpIRQBreakEnabled = false;
    rbpCountValue = 0;
    rbpThresholdValue = 0;
    //ウインドウ
    rbpFrame = null;
    //タイマー
    rbpTimer = 0;
  }  //rbpInit()

  //rbpStart ()
  public static void rbpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_RBP_FRAME_KEY)) {
      rbpOpen ();
    }
  }  //rbpStart()

  //rbpOpen ()
  //  ラスタブレークポイントウインドウを開く
  public static void rbpOpen () {
    if (rbpFrame == null) {
      rbpMakeFrame ();
    } else {
      rbpUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_RBP_VISIBLE_MASK;
    rbpFrame.setVisible (true);
  }  //rbpOpen()

  //rbpMakeFrame ()
  //  ラスタブレークポイントウインドウを作る
  //  ここでは開かない
  public static void rbpMakeFrame () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Fixed Raster ":  //固定ラスタ
          rbpSetBreakEnabled (((JCheckBox) source).isSelected ());  //指定されたブレークラスタでラスタブレークをかけるかどうか
          break;
        case "IRQ Raster ":  //IRQ ラスタ
          rbpSetIRQEnabled (((JCheckBox) source).isSelected ());  //IRQラスタでラスタブレークをかけるかどうか
          break;
        case "Run to Next Raster":  //次のラスタまで実行
          if (XEiJ.mpuTask == null) {
            //一旦無効にする
            rbpSetBreakEnabled (false);
            //ブレークラスタをインクリメントする
            //  rbpSetBreakRaster()で指定してからrbpBreakModelを更新するとrbpSetBreakRaster()が2回呼び出されることになるが、
            //  rbpBreakModel経由だけにするとrbpBreakRasterの更新が遅れる可能性がある
            rbpSetBreakRaster (CRTC.crtRasterNumber < CRTC.crtR04VFrontEndCurr ? CRTC.crtRasterNumber + 1 : 0);
            rbpBreakModel.setValue (new Integer (rbpBreakRaster));
            //有効にする
            rbpSetBreakEnabled (true);
            rbpEnabledCheckBox.setSelected (true);
            //現在の値を表示する
            rbpUpdateFrame ();
            //実行する
            XEiJ.mpuStart ();
          }
          break;
        case "Run":  //実行
          if (XEiJ.mpuTask == null) {
            //実行する
            XEiJ.mpuStart ();
            //if (!rbpBreakEnabled) {  //無効になっているときは閉じる
            //  rbpFrame.setVisible (false);
            //}
          }
          break;
        }
      }
    };
    //ウインドウ
    rbpFrame = Multilingual.mlnTitle (
      XEiJ.createRestorableSubFrame (
        Settings.SGS_RBP_FRAME_KEY,
        "Raster Break Point",
        null,
        XEiJ.createVerticalBox (
          Box.createVerticalStrut (4),
          XEiJ.createHorizontalBox (
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            rbpStatusLabel = XEiJ.createLabel (rbpMakeStatusText ()),
            Box.createHorizontalGlue (),
            Box.createHorizontalStrut (12)
            ),
          Box.createVerticalStrut (4),
          XEiJ.createHorizontalBox (
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            Multilingual.mlnText (XEiJ.createLabel ("Current Raster "), "ja", "現在のラスタ "),
            rbpCurrentRasterTextField = XEiJ.setEditable (XEiJ.createNumberField (String.valueOf (CRTC.crtRasterNumber), 5), false),
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            rbpEnabledCheckBox = Multilingual.mlnText (XEiJ.createCheckBox (rbpBreakEnabled, "Fixed Raster ", listener), "ja", "固定ラスタ "),
            XEiJ.createNumberSpinner (rbpBreakModel = new SpinnerNumberModel (rbpBreakRaster, 0, 1023, 1), 5, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                rbpSetBreakRaster (rbpBreakModel.getNumber ().intValue ());  //ブレークラスタ
              }
            }),
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            rbpIRQEnabledCheckBox = Multilingual.mlnText (XEiJ.createCheckBox (rbpIRQBreakEnabled, "IRQ Raster ", listener), "ja", "IRQ ラスタ "),
            rbpIRQRasterTextField = XEiJ.setEditable (XEiJ.createNumberField (String.valueOf (CRTC.crtR09IRQRasterCurr), 5), false),
            Box.createHorizontalGlue (),
            Box.createHorizontalStrut (12)
            ),
          Box.createVerticalStrut (4),
          XEiJ.createHorizontalBox (
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            Multilingual.mlnText (XEiJ.createLabel ("Count "), "ja", "回数 "),
            XEiJ.createNumberSpinner (rbpCountModel = new SpinnerNumberModel (rbpCountValue, 0, 99999999, 1), 8, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                rbpCountValue = rbpCountModel.getNumber ().intValue ();
              }
            }),
            Box.createHorizontalStrut (12),
            Multilingual.mlnText (XEiJ.createLabel ("Threshold "), "ja", "閾値 "),
            XEiJ.createNumberSpinner (rbpThresholdModel = new SpinnerNumberModel (rbpThresholdValue, 0, 99999999, 1), 8, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                rbpThresholdValue = rbpThresholdModel.getNumber ().intValue ();
              }
            }),
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            XEiJ.mpuAddButtonStopped (Multilingual.mlnText (XEiJ.createButton ("Run to Next Raster", listener), "ja", "次のラスタまで実行")),
            Box.createHorizontalStrut (12),
            XEiJ.mpuAddButtonStopped (Multilingual.mlnText (XEiJ.createButton ("Run", listener), "ja", "実行")),
            Box.createHorizontalGlue (),
            Box.createHorizontalStrut (12)
            ),
          Box.createVerticalStrut (4)
          )
        ),
      "ja", "ラスタブレークポイント");
    //  ウインドウリスナー
    XEiJ.addRemovableListener (
      rbpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_RBP_VISIBLE_MASK;
        }
      });
  }  //rbpMakeFrame()

  //rbpMakeStatusText ()
  public static String rbpMakeStatusText () {
    StringBuilder sb = new StringBuilder ();
    return (Multilingual.mlnJapanese ? sb.
            append ("帰線期間 0-").append (CRTC.crtR05VSyncEndCurr).
            append ("    バックポーチ ").append (CRTC.crtR05VSyncEndCurr + 1).append ("-").append (CRTC.crtR06VBackEndCurr).
            append ("    映像期間 ").append (CRTC.crtR06VBackEndCurr + 1).append ("-").append (CRTC.crtR07VDispEndCurr).
            append ("    フロントポーチ ").append (CRTC.crtR07VDispEndCurr + 1).append ("-").append (CRTC.crtR04VFrontEndCurr)
            : sb.
            append ("Sync Period 0-").append (CRTC.crtR05VSyncEndCurr).
            append ("    Back Porch ").append (CRTC.crtR05VSyncEndCurr + 1).append ("-").append (CRTC.crtR06VBackEndCurr).
            append ("    Video Period ").append (CRTC.crtR06VBackEndCurr + 1).append ("-").append (CRTC.crtR07VDispEndCurr).
            append ("    Front Porch ").append (CRTC.crtR07VDispEndCurr + 1).append ("-").append (CRTC.crtR04VFrontEndCurr)
            ).toString ();
  }  //rbpMakeStatusText ()

  //rbpUpdateFrame ()
  //  ラスタブレークポイントウインドウを更新する
  //  ウインドウが構築済みであることを確認してから呼び出すこと
  public static void rbpUpdateFrame () {
    rbpStatusLabel.setText (rbpMakeStatusText ());
    rbpCurrentRasterTextField.setText (String.valueOf (CRTC.crtRasterNumber));
    rbpIRQRasterTextField.setText (String.valueOf (CRTC.crtR09IRQRasterCurr));
    rbpTimer = RBP_INTERVAL;
  }  //rbpUpdateFrame()

  //rbpSetBreakEnabled (enabled)
  //  指定されたブレークラスタでラスタブレークをかけるかどうかを設定する
  public static void rbpSetBreakEnabled (boolean enabled) {
    rbpBreakEnabled = enabled;
    rbpActiveBreakRaster = rbpBreakEnabled ? rbpBreakRaster : -1;
    if (CRTC.CRT_RASTER_HASH_ON) {
      CRTC.crtUpdateRasterHash ();
    }
  }  //rbpSetBreakEnabled(boolean)

  //rbpSetBreakRaster (breakRaster)
  //  ラスタブレークをかけるラスタ番号を設定する
  public static void rbpSetBreakRaster (int raster) {
    rbpBreakRaster = raster;
    rbpActiveBreakRaster = rbpBreakEnabled ? rbpBreakRaster : -1;
    if (CRTC.CRT_RASTER_HASH_ON) {
      CRTC.crtUpdateRasterHash ();
    }
  }  //rbpSetBreakRaster(int)

  //rbpSetIRQEnabled (atInterrupt)
  //  IRQラスタでラスタブレークをかけるかどうかを設定する
  public static void rbpSetIRQEnabled (boolean enabled) {
    rbpIRQBreakEnabled = enabled;
  }  //rbpSetIRQEnabled(boolean)

  //rbpCheckIRQ ()
  //  IRQを確認する
  public static void rbpCheckIRQ () {
    int irq = CRTC.crtRasterNumber == CRTC.crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
    if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
      if (irq == 0) {  //IRQ信号が0になったとき
        if (RBP_ON) {
          if (rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
            rbpFire ();  //ラスタブレークをかける
          }
        }
        MC68901.mfpRintFall ();  //IRQ開始
      } else {  //IRQ信号が0でなくなったとき
        MC68901.mfpRintRise ();  //IRQ終了
      }
    }
  }  //rbpCheckIRQ()

  //rbpFire ()
  //  ラスタブレークをかける
  //  ブレークラスタの水平フロントポーチで呼び出す
  public static void rbpFire () {
    if (XEiJ.mpuTask == null) {  //既に停止している(ブレークラスタとIRQラスタが同じとき2回呼び出されることがある)
      return;
    }
    //回数を数える
    rbpCountValue++;
    //ウインドウが表示されていたら更新する
    if ((XEiJ.dbgVisibleMask & XEiJ.DBG_RBP_VISIBLE_MASK) != 0) {
      rbpUpdateFrame ();
      rbpCountModel.setValue (new Integer (rbpCountValue));
    }
    //回数が閾値に達していたらラスタブレークをかける
    if (rbpThresholdValue <= rbpCountValue) {
      //スクリーンを更新する
      if (CRTC.crtDirtyY0 >= 0) {  //更新されたラスタがある
        int dirtyY0 = CRTC.crtDirtyY0;
        VideoController.vcnUpdate ();  //スクリーンを更新する
        CRTC.crtDirtyY0 = dirtyY0;  //CRTC.crtDirtyY0を更新しない
      }
      //逆アセンブルリストウインドウが開いていなかったら開く
      if ((XEiJ.dbgVisibleMask & XEiJ.DBG_DDP_VISIBLE_MASK) == 0) {
        DisassembleList.ddpOpen (-1, -1, false);
      }
      //ラスタブレークをかける
      XEiJ.mpuStop (null);
    }
  }  //rbpFire()

  public static void rbpTrace () {
    System.out.println (new StringBuilder ().
                        append ("backEnd=").append (CRTC.crtR06VBackEndCurr).append (",").
                        append ("dispEnd=").append (CRTC.crtR07VDispEndCurr).append (",").
                        append ("frontEnd=").append (CRTC.crtR04VFrontEndCurr).append (",").
                        append ("IRQ=").append (CRTC.crtR09IRQRasterCurr).append (",").
                        append ("break=").append (rbpBreakRaster).append (",").
                        append ("raster=").append (CRTC.crtRasterNumber).toString ());
  }  //rbpTrace()

}  //class RasterBreakPoint



