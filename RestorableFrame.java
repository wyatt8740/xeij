//========================================================================================
//  RestorableFrame.java
//    en:Restorable frame -- It is a frame that you can easily save and restore the position, size and state.
//    ja:リストアラブルフレーム -- 位置とサイズと状態の保存と復元が簡単にできるフレームです。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  コンストラクタでインスタンスのキーを指定する
//    キーに対応するインスタンスの位置とサイズと状態が指定されていなければダミーの位置とサイズと状態を記憶する
//  クラスメソッドでキーに対応するインスタンスの位置とサイズと状態を指定する
//    キーに対応するインスタンスが既に存在して表示されていたら位置とサイズと状態を復元する
//  コンポーネントリスナーとウインドウステートリスナーでインスタンスの位置とサイズと状態を復元または記憶する
//    表示されていないときは何もしない
//    初めて表示されたときインスタンスの位置とサイズと状態を復元する
//      サイズは表示された後に復元しなければならない
//    2回目以降はインスタンスの位置とサイズと状態を記憶する
//  クラスメソッドでキーに対応するインスタンスの位置とサイズと状態を取り出して保存する
//  インスタンスメソッドは変更しない
//    JFrameをRestorableFrameに置き換えるとき変数宣言の型を変更しなくて済む
//  最大化しているときは位置とサイズを保存しない
//    最大化している状態で位置とサイズを保存してしまうと復元した後に元の大きさに戻せなくなる
//    最大化した状態を復元するときは最大化する前の位置とサイズを復元してから最大化させる
//  水平方向だけ最大化または垂直方向だけ最大化
//    Windows7の場合、通常のウインドウはウインドウの枠を左ダブルクリックするとウインドウを水平方向だけ最大化または垂直方向だけ最大化できる
//    しかし、JFrameには水平方向だけ最大化または垂直方向だけ最大化する機能がないらしい
//    枠を左ダブルクリックしてもウインドウのサイズが変わらない
//    Frame.getExtendedState()はMAXIMIZED_HORIZまたはFrame.MAXIMIZED_VERTを返さない
//    Frame.setExtendedState()にMAXIMIZED_HORIZまたはFrame.MAXIMIZED_VERTを与えてもウインドウのサイズが変わらない
//  Frame.getExtendedState()では全画面表示を判別できない
//    GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getFullScreenWindow () == frameで判別する
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class RestorableFrame extends JFrame {

  //クラス変数
  protected static HashMap<String,RestorableFrame> rfmKeyToFrame;
  protected static HashMap<String,Rectangle> rfmKeyToBounds;
  protected static HashMap<String,Integer> rfmKeyToState;
  protected static HashMap<String,Boolean> rfmKeyToOpened;  //ウインドウが開いているかどうか。これはまとめて保存しやすくするための情報。そもそもインスタンスを作らなければ開けないのだから実際に開く処理はインスタンスを作る側で行う

  //クラスメソッド

  //rfmInit ()
  //  初期化
  public static void rfmInit () {
    rfmKeyToFrame = new HashMap<String,RestorableFrame> ();
    rfmKeyToBounds = new HashMap<String,Rectangle> ();
    rfmKeyToState = new HashMap<String,Integer> ();
    rfmKeyToOpened = new HashMap<String,Boolean> ();
  }  //rfmInit()

  //bounds = rfmGetBounds (key)
  //  インスタンスの位置とサイズを読み出す
  public static Rectangle rfmGetBounds (String key) {
    return rfmKeyToBounds.containsKey (key) ? rfmKeyToBounds.get (key) : new Rectangle ();
  }  //rfmGetBounds(String)

  //rfmSetBounds (key, bounds)
  //  インスタンスの位置とサイズを指定する
  public static void rfmSetBounds (String key, Rectangle bounds) {
    rfmKeyToBounds.put (key, bounds);  //新しい位置とサイズ
    if (rfmKeyToFrame.containsKey (key)) {  //インスタンスが既にあるとき
      RestorableFrame frame = rfmKeyToFrame.get (key);  //インスタンス
      if (frame.isShowing ()) {  //表示されている
        frame.setLocation (bounds.x, bounds.y);  //位置を復元する
        if (bounds.width > 0 && bounds.height > 0) {  //サイズが保存されているとき
          frame.setSize (bounds.width, bounds.height);  //サイズを復元する
        }
      }
    }
  }  //rfmSetBounds(String,Rectangle)

  //state = rfmGetState (key)
  //  インスタンスの状態を読み出す
  public static int rfmGetState (String key) {
    return rfmKeyToState.containsKey (key) ? rfmKeyToState.get (key) : Frame.NORMAL;
  }  //rfmGetState(String)

  //rfmSetState (key, state)
  //  インスタンスの状態を指定する
  public static void rfmSetState (String key, int state) {
    rfmKeyToState.put (key, state);  //新しい状態
    if (rfmKeyToFrame.containsKey (key)) {  //インスタンスが既にあるとき
      RestorableFrame frame = rfmKeyToFrame.get (key);  //インスタンス
      if (frame.isShowing ()) {  //表示されている
        frame.setExtendedState (state);  //状態を復元する
      }
    }
  }  //rfmSetState(String,int)

  //opened = rfmGetOpened (key)
  //  ウインドウが開いているかどうかを読み出す
  public static boolean rfmGetOpened (String key) {
    return rfmKeyToOpened.containsKey (key) && rfmKeyToOpened.get (key);
  }  //rfmGetOpened(String)

  //rfmSetOpened (key, opened)
  //  ウインドウが開いているかどうかを指定する
  public static void rfmSetOpened (String key, boolean opened) {
    rfmKeyToOpened.put (key, opened);
    //フレームは操作しない
  }  //rfmSetOpened(String,boolean)

  //image = rfmCapture (key)
  //  ウインドウをキャプチャする
  public static BufferedImage rfmCapture (String key) {
    if (rfmGetOpened (key)) {  //存在して開いている
      rfmKeyToFrame.get (key).rfmUpdate ();  //レクタングルを更新する
      Rectangle rect = rfmGetBounds (key);
      if (!rect.isEmpty ()) {  //空ではない
        try {
          return new Robot().createScreenCapture (rect);
        } catch (Exception e) {
        }
      }
    }
    return null;
  }  //rfmCapture(String)

  //インスタンス変数
  private String rfmKey;  //キー
  private boolean rfmRestored;  //false=まだ復元されていない,true=復元済み

  //コンストラクタ
  public RestorableFrame (String key) {
    this (key, "", GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getDefaultConfiguration ());
  }
  public RestorableFrame (String key, GraphicsConfiguration gc) {
    this (key, "", gc);
  }
  public RestorableFrame (String key, String title) {
    this (key, title, GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getDefaultConfiguration ());
  }
  public RestorableFrame (String key, String title, GraphicsConfiguration gc) {
    super (title, gc);
    rfmKey = key;  //キー
    rfmRestored = false;  //まだ復元されていない
    if (rfmKeyToFrame.containsKey (key)) {  //同じキーを持つインスタンスが既にあるとき
      throw new IllegalArgumentException ("RestorableFrame: Key " + key + " is already used.");
    }
    rfmKeyToFrame.put (key, this);  //インスタンス
    if (rfmKeyToBounds.containsKey (key)) {  //位置とサイズが既に指定されているとき
      rfmRestoreBounds (rfmKeyToBounds.get (key));  //位置とサイズを復元する
    } else {  //位置とサイズがまだ指定されていないとき
      rfmKeyToBounds.put (key, new Rectangle ());  //ダミーの位置とサイズ
    }
    if (rfmKeyToState.containsKey (key)) {  //状態が既に指定されているとき
      setExtendedState (rfmKeyToState.get (key));  //状態を復元する
    } else {  //状態がまだ指定されていないとき
      rfmKeyToState.put (key, Frame.NORMAL);  //ダミーの状態
    }
    //コンポーネントリスナー
    addComponentListener (new ComponentAdapter () {
      @Override public void componentMoved (ComponentEvent ce) {
        rfmUpdate ();  //位置とサイズと状態を復元または記憶する
      }
      @Override public void componentResized (ComponentEvent ce) {
        rfmUpdate ();  //位置とサイズと状態を復元または記憶する
      }
    });
    //ウインドリスナー
    addWindowListener (new WindowAdapter () {
      @Override public void windowClosing (WindowEvent we) {
        //HIDE_ON_CLOSEのときclosedは呼び出されないがclosingは呼び出される
        rfmKeyToOpened.put (rfmKey, false);
      }
      @Override public void windowOpened (WindowEvent we) {
        rfmKeyToOpened.put (rfmKey, true);
      }
    });
    //ウインドウステートリスナー
    addWindowStateListener (new WindowStateListener () {
      @Override public void windowStateChanged (WindowEvent we) {
        rfmUpdate ();  //位置とサイズと状態を復元または記憶する
      }
    });
  }  //new RestorableFrame(String,String,GraphicsConfiguration)

  //rfmUpdate ()
  //  位置とサイズと状態を復元または記憶する
  private void rfmUpdate () {
    if (!isShowing ()) {  //表示されていない
      return;
    }
    for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment ().getScreenDevices ()) {
      if (gd.getFullScreenWindow () == this) {  //全画面表示
        return;
      }
    }
    if (!rfmRestored) {  //まだ復元されていない
      rfmRestored = true;  //復元済み
      rfmRestoreBounds (rfmKeyToBounds.get (rfmKey));  //位置とサイズを復元する
      setExtendedState (rfmKeyToState.get (rfmKey));  //状態を復元する
    } else {  //復元済み
      Point p = getLocationOnScreen ();  //位置
      Dimension d = getSize ();  //サイズ
      int state = getExtendedState ();  //状態
      //位置とサイズを記憶する
      Rectangle bounds = rfmKeyToBounds.get (rfmKey);  //位置とサイズ
      if ((state & (Frame.ICONIFIED | Frame.MAXIMIZED_HORIZ)) == 0) {  //アイコン化または水平方向に最大化されていない
        //水平方向の要素を記憶する
        bounds.x = p.x;
        bounds.width = d.width;
      }
      if ((state & (Frame.ICONIFIED | Frame.MAXIMIZED_VERT)) == 0) {  //アイコン化または垂直方向に最大化されていない
        //垂直方向の要素を記憶する
        bounds.y = p.y;
        bounds.height = d.height;
      }
      //状態を記憶する
      rfmKeyToState.put (rfmKey, state);  //状態
    }  //if まだ復元されていない/復元済み
  }  //rfmUpdate()

  //rfmRestoreBounds (bounds)
  //  位置とサイズを復元する
  //  保存後にマルチスクリーン環境が変化して画面外に復元されると困るので、
  //  ウインドウの上端の64x16ピクセルを含む画面がなければデフォルト画面の左上に復元する
  private void rfmRestoreBounds (Rectangle bounds) {
    Rectangle location = bounds;  //復元する位置
  test:
    {
      Rectangle testBounds = new Rectangle (bounds.x, bounds.y, bounds.width, 16);
      for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment ().getScreenDevices ()) {
        for (GraphicsConfiguration gc : gd.getConfigurations ()) {
          Rectangle intersectionBounds = testBounds.intersection (gc.getBounds ());
          if (intersectionBounds.width >= 64 && intersectionBounds.height >= 16) {
            //ウインドウの上端の64x16ピクセルを含む画面が見つかった
            break test;
          }
        }  //for gc
      }  //for gd
      //ウインドウの上端の64x16ピクセルを含む画面が見つからなかった
      location = GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getDefaultConfiguration ().getBounds ();
    }
    setLocation (location.x, location.y);  //位置を復元する
    if (bounds.width > 0 && bounds.height > 0) {  //サイズが保存されているとき
      setSize (bounds.width, bounds.height);  //サイズを復元する
    }
  }  //rfmRestoreBounds(Rectangle)

}  //class RestorableFrame



