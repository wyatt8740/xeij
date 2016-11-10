//========================================================================================
//  XEiJApplet.java
//    en:XEiJ applet
//    ja:XEiJアプレット
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  LiveConnectを使用する
//  参考
//    http://docs.oracle.com/javase/jp/6/technotes/guides/jweb/applet/liveconnect_support.html
//    https://developer.mozilla.org/ja/docs/Web/JavaScript/Guide/LiveConnect_Overview
//    https://developer.mozilla.org/en-US/docs/Archive/Web/LiveConnect/LiveConnect_Reference
//    http://docs.oracle.com/javase/jp/7/technotes/guides/jweb/index.html
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import netscape.javascript.*;  //JSException,JSObject。jfxrt.jarではなくplugin.jarを使うこと

//$$XEA XEiJアプレット
//  アプレットクラス
public class XEiJApplet extends JApplet {

  public static final boolean APP_DEBUG_TRACE = false;

  public static XEiJApplet appApplet;  //アプレット
  public static boolean appLiveConnectSupported;  //true=LiveConnectが動作する
  public static JSObject appWindow;  //window
  public static JSObject appDocument;  //document
  public static JSObject appDocumentBody;  //document.body
  public static JSObject appDocumentElement;  //document.documentElement
  public static String appElementId;  //<object>または<applet>のid
  public static int appWidth;  //アプレットの要素の現在のサイズ
  public static int appHeight;
  public static int appMinWidth;  //アプレットの要素の最小サイズ。ウインドウに合わせるときこれより小さくしない
  public static int appMinHeight;
  public static boolean appFullscreenOn;  //true=全画面表示
  public static boolean appFitInWindowOn;  //true=ウインドウに合わせる
  public static JSObject appDeckElement;  //デッキ。外側のdiv要素。document.bodyと一緒に動く
  public static JSObject appLiftElement;  //リフト。内側のdiv要素。ウインドウに合わせるときデッキからはみ出してウインドウ一杯に広がる。全画面表示のときフルスクリーンになる
  public static JSObject appElement;  //アプレットの要素。object要素またはapplet要素またはembed要素
  public static JSObject appRequestDialog;  //全画面表示のダイアログ
  public static JSObject appExitDialog;  //全画面解除のダイアログ
  public static JSObject appYesButton;  //Yesボタン
  public static JSObject appNoButton;  //Noボタン
  public static JSObject appOkButton;  //Okボタン
  public static String appRequestFullscreenMethod;  //lift.requestFullscreen相当のメソッド名
  public static String appExitFullscreenMethod;  //document.exitFullscreen相当のメソッド名
  public static String appFullscreenElementField;  //document.fullscreenElement相当のフィールド名
  public static boolean appIsMSIE;  //true=IE11
  public static boolean appIsChrome;  //true=Chrome
  public static int appLiftLeft;  //appLiftElementの相対位置
  public static int appLiftTop;
  public static int appPrevLiftLeft;  //全画面表示に移行する前のappLiftElementの相対位置
  public static int appPrevLiftTop;

  //appInit ()
  //  初期化
  public static void appInit () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appInit()");
    }
    appLiveConnectSupported = false;
    XEiJ.pnlIsFullscreenSupported = false;
    XEiJ.pnlIsFitInWindowSupported = false;
    //LiveConnect
    try {
      appWindow = JSObject.getWindow (appApplet);  //windowオブジェクト
      appDocument = appGetJSObject (appWindow, "document");
      if (appDocument != null) {
        appElementId = appApplet.getParameter ("elementid");
        if (appElementId == null) {
          appElementId = "XEiJApplet";
        }
        if (APP_DEBUG_TRACE) {
          System.out.println ("appElementId = " + appElementId);
        }
        appElement = appCallJSObject (appDocument, "getElementById", appElementId);
        if (APP_DEBUG_TRACE) {
          System.out.println ("appElement = " + appElement);
        }
        if (appElement != null) {
          //appAttr (appElement, "mayscript", true);
          appLiftElement = appGetJSObject (appElement, "parentNode");
          if (APP_DEBUG_TRACE) {
            System.out.println ("appLiftElement = " + appLiftElement);
          }
          if (appLiftElement != null && "div".equalsIgnoreCase (appGetString (appLiftElement, "tagName"))) {
            appDeckElement = appGetJSObject (appLiftElement, "parentNode");
            if (APP_DEBUG_TRACE) {
              System.out.println ("appDeckElement = " + appDeckElement);
            }
            if (appDeckElement != null && "div".equalsIgnoreCase (appGetString (appDeckElement, "tagName"))) {
              appLiveConnectSupported = "XEiJAppletOuter".equals (appElementId);
            }
          }
        }
      }
    } catch (Exception e) {
      //Operaのときローカルでは
      //  netscape.javascript.JSException: baseURI and docbase host DO NOT match: localhost
      //というエラーが出る
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("appLiveConnectSupported = " + appLiveConnectSupported);
    }
    if (appLiveConnectSupported) {
      appDocumentBody = appGetJSObject (appDocument, "body");
      appDocumentElement = appGetJSObject (appDocument, "documentElement");
      appWidth = 768;
      appHeight = 639;
      appMinWidth = 200;
      appMinHeight = 200;
      appFullscreenOn = false;
      appFitInWindowOn = false;
      appStyle (appDeckElement,
                "left", "0px",
                "position", "relative",
                "top", "0px");
      appLiftLeft = 0;
      appLiftTop = 0;
      String color2 = XEiJ.fmtHex6 (new StringBuilder ().append ('#'), LnF.LNF_RGB[2]).toString ();
      appStyle (appLiftElement,
                //"backgroundColor", color2,  //ウインドウに合わせるときリフトがデッキからはみ出した部分にダイアログを表示したとき奥のテキストが見えないようにする
                "left", "0px",
                "position", "relative",
                //"textAlign", "center",  //HTMLに書いておかないと起動するまで左に寄ったままになる
                "top", "0px",
                "zIndex", "65535");
      appStyle (appElement,
                "height", appHeight + "px",
                //"margin", "0px 0px",  //auto 0px
                "position", "relative",
                "width", appWidth + "px");
      XEiJ.pnlIsFullscreenSupported = false;
      String userAgent = appGetString (appGetJSObject (appWindow, "navigator"), "userAgent");
      if (userAgent != null) {
        appIsMSIE = userAgent.indexOf ("Trident") >= 0;  //IE11
        appIsChrome = userAgent.indexOf ("Chrome") >= 0;  //Chrome
        //Chromeのバグ対策
        //  ウインドウの上部に表示されるJavaアプレットの実行の許可を求めるバーが引っ込む前にアプレットが表示されたとき、
        //  アプレットの位置が下にずれたまま起動してしまう
        //  ずれているだけならまだよいのだが、マウスに反応しないのでメニューを操作できない
        //  ウインドウの枠を摘んでちょっとリサイズしてやるだけでアプレットがガクンと跳ね上がって本来の位置に収まって操作できるようになるが、
        //  分かりにくいのでデッキを1pxだけずらすことで人間が操作しなくても本来の位置(+1px)に戻るようにする
        //  すぐに0pxに戻してしまうと効果がない
        //  時間を開ければよいのだろうが面倒なので1pxずれたままにする
        //if (appIsChrome) {  //Chromeのとき → Chrome以外でもずらしてみる
        appStyle (appDeckElement,
                  "top", "1px");
        //}
        if (true) {
          //全画面API
          //  https://msdn.microsoft.com/library/dn265028(v=vs.85).aspx
          appRequestFullscreenMethod = null;
          appExitFullscreenMethod = null;
          appFullscreenElementField = null;
          for (String candidate : new String[] {
            "msRequestFullscreen",  //IE。screenのsは小文字
            "webkitRequestFullScreen",  //Chrome。ScreenのSは大文字と小文字の両方
            "mozRequestFullScreen",  //Firefox,Waterfox。ScreenのSは大文字
            "requestFullscreen",  //HTML5,Chrome,Firefox,Opera,Waterfox。screenは小文字
          }) {
            if (appIsDefined (appLiftElement, candidate)) {
              appRequestFullscreenMethod = candidate;
              if (APP_DEBUG_TRACE) {
                System.out.println ("requestFullscreen = " + candidate);
              }
              break;
            }
          }
          for (String candidate : new String[] {
            "msExitFullscreen",  //IE。screenのsは小文字
            "webkitCancelFullScreen",  //Chrome。ScreenのSは大文字
            "mozCancelFullScreen",  //Firefox,Waterfox。ScreenのSは大文字
            "exitFullscreen",  //HTML5,Chrome,Opera,Waterfox。screenは小文字
          }) {
            if (appIsDefined (appDocument, candidate)) {
              appExitFullscreenMethod = candidate;
              if (APP_DEBUG_TRACE) {
                System.out.println ("exitFullscreen = " + candidate);
              }
              break;
            }
          }
          for (String candidate : new String[] {
            "msFullscreenElement",  //IE。screenは小文字
            "webkitFullscreenElement",  //Chrome。screenは小文字
            "mozFullScreenElement",  //Firefox。ScreenのSは大文字
            //"webkitCurrentFullScreenElement",  //Chrome。ScreenのSは大文字
            "fullscreenElement",  //HTML5,Opera。screenは小文字
          }) {
            if (appIsDefined (appDocument, candidate)) {
              appFullscreenElementField = candidate;
              if (APP_DEBUG_TRACE) {
                System.out.println ("fullscreenElement = " + candidate);
              }
              break;
            }
          }
          XEiJ.pnlIsFullscreenSupported = appRequestFullscreenMethod != null && appExitFullscreenMethod != null;  //全画面表示に移行できる
        }  //IE11またはChromeのとき
      }  //if userAgent!=null
      XEiJ.pnlIsFitInWindowSupported = true;  //LiveConnectが動作しているのでウインドウに合わせられる
      appEvent (appWindow, "resize", appElementId + ".appWindowOnresize ();");
      appRequestDialog = null;  //使うときに作る
    }  //if appLiveConnectSupported
  }  //appInit()

  //appMake ()
  //  アプレットのメニューとコンテントペインを設定する
  public static void appMake () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appMake()");
    }
    appApplet.setJMenuBar (XEiJ.mnbMenuBar);
    appApplet.getContentPane ().add (XEiJ.pnlPanel, BorderLayout.CENTER);
  }  //appMake()

  //appMakeRequestDialog ()
  //  全画面表示のダイアログを作る
  public static void appMakeRequestDialog () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appMakeRequestDialog()");
    }
    if (appRequestDialog == null) {
      appRequestDialog = appCreate ("div");  //ダイアログ
      String color2 = XEiJ.fmtHex6 (new StringBuilder ().append ('#'), LnF.LNF_RGB[2]).toString ();
      if (false) {
        appStyle (appRequestDialog,
                  "backgroundColor", color2,
                  "border", "#ff0 solid 0.125em",
                  "borderRadius", "0.5em",
                  "display", "none",  //まだ表示しない
                  "margin", "0.5em auto",
                  "padding", "0.5em",
                  "textAlign", "center",
                  "width", "400px");
        appAppend (appRequestDialog,
                   appText (Multilingual.mlnJapanese ? "全画面表示に切り替えますか？ " : "Do you want to switch to full screen? "),
                   appAppend (appEvent (appAttr (appYesButton = appCreate ("button"),
                                                 "tabIndex", 1),
                                        "click", appElementId + ".appRequestOnclick (true);"),
                              appText (Multilingual.mlnJapanese ? " はい " : " Yes ")),  //Chromeのとき末尾の文字が化けるので空白を追加
                   appText (" "),
                   appAppend (appEvent (appAttr (appNoButton = appCreate ("button"),
                                                 "tabIndex", 2),
                                        "click", appElementId + ".appRequestOnclick (false);"),
                              appText (Multilingual.mlnJapanese ? " いいえ " : " No ")));  //Chromeのとき末尾の文字が化けるので空白を追加
      } else {
        appAppend (appStyle (appRequestDialog,
                             "backgroundColor", color2,
                             "display", "none",  //まだ表示しない
                             "margin", "0px",
                             "padding", "0.5em 0px"),
                   appAppend (appStyle (appCreate ("div"),
                                        "border", "#ff0 solid 0.125em",
                                        "borderRadius", "0.5em",
                                        "margin", "0px auto",
                                        "padding", "0.5em",
                                        "textAlign", "center",
                                        "width", "400px"),
                              appText (Multilingual.mlnJapanese ? "全画面表示に切り替えますか？ " : "Do you want to switch to full screen? "),
                              appAppend (appEvent (appAttr (appYesButton = appCreate ("button"),
                                                            "tabIndex", 1),
                                                   "click", appElementId + ".appRequestOnclick (true);"),
                                         appText (Multilingual.mlnJapanese ? " はい " : " Yes ")),  //Chromeのとき末尾の文字が化けるので空白を追加
                              appText (" "),
                              appAppend (appEvent (appAttr (appNoButton = appCreate ("button"),
                                                            "tabIndex", 2),
                                                   "click", appElementId + ".appRequestOnclick (false);"),
                                         appText (Multilingual.mlnJapanese ? " いいえ " : " No "))));  //Chromeのとき末尾の文字が化けるので空白を追加
      }
      appCall (appLiftElement, "insertBefore", appRequestDialog, appElement);  //アプレットの上に押し込む
    }
  }  //appMakeRequestDialog()

  //appMakeExitDialog ()
  //  全画面解除のダイアログを作る
  public static void appMakeExitDialog () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appMakeExitDialog()");
    }
    if (appExitDialog == null) {
      appExitDialog = appCreate ("div");  //ダイアログ
      String color2 = XEiJ.fmtHex6 (new StringBuilder ().append ('#'), LnF.LNF_RGB[2]).toString ();
      if (false) {
        appStyle (appExitDialog,
                  "backgroundColor", color2,
                  "border", "#ff0 solid 0.125em",
                  "borderRadius", "0.5em",
                  "display", "none",  //まだ表示しない
                  "margin", "0.5em auto",
                  "padding", "0.5em",
                  "textAlign", "center",
                  "width", "400px");
        appAppend (appExitDialog,
                   appText (Multilingual.mlnJapanese ? "全画面表示を終了します " : "Exiting full screen mode "),
                   appAppend (appEvent (appAttr (appOkButton = appCreate ("button"),
                                                 "tabIndex", 1),
                                        "click", appElementId + ".appExitOnclick (true);"),
                              appText (" OK ")));  //Chromeのとき末尾の文字が化けるので空白を追加
      } else {
        appAppend (appStyle (appExitDialog,
                             "backgroundColor", color2,
                             "display", "none",  //まだ表示しない
                             "margin", "0px",
                             "padding", "0.5em 0px"),
                   appAppend (appStyle (appCreate ("div"),
                                        "border", "#ff0 solid 0.125em",
                                        "borderRadius", "0.5em",
                                        "margin", "0px auto",
                                        "padding", "0.5em",
                                        "textAlign", "center",
                                        "width", "400px"),
                              appText (Multilingual.mlnJapanese ? "全画面表示を終了します " : "Exiting full screen mode "),
                              appAppend (appEvent (appAttr (appOkButton = appCreate ("button"),
                                                            "tabIndex", 1),
                                                   "click", appElementId + ".appExitOnclick (true);"),
                                         appText (" OK "))));  //Chromeのとき末尾の文字が化けるので空白を追加
      }
      appCall (appLiftElement, "insertBefore", appExitDialog, appElement);  //アプレットの上に押し込む
    }
  }  //appMakeExitDialog()

  //appReload ()
  //  設定に従ってコンポーネントを更新する
  public static void appReload () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appReload()");
    }
    String color2 = XEiJ.fmtHex6 (new StringBuilder ().append ('#'), LnF.LNF_RGB[2]).toString ();
    appStyle (appDocumentBody,
              "backgroundColor", color2);
    appStyle (appLiftElement,
              "backgroundColor", color2);
  }  //appReload()

  //appStandbyFullscreen (on)
  //  全画面表示の設定の準備
  //  OFF→ONのとき
  //    requestFullscreenはマウスイベントまたはキーイベントの中でなければ動作しないので確認ダイアログを出してボタンを押してもらう
  //    全画面表示の切り替えのインタフェイスをJavaScriptに渡す
  //    キャンセルされる場合があるのでここではまだ全画面表示に移行しない
  //    全画面表示メニューを無効にする
  //      F11キーは全画面表示メニューが有効なときdoClick()しているだけなので全画面表示メニューを無効にすればF11キーも効かなくなる
  //  ON→OFFのとき
  //    MSIEまたはChromeのとき
  //      全画面表示を解除する
  //    それ以外
  //      exitFullscreenはマウスイベントまたはキーイベントの中でなければ動作しないので確認ダイアログを出してボタンを押してもらう
  //      全画面表示の切り替えのインタフェイスをJavaScriptに渡す
  //      全画面表示メニューを無効にする
  //        F11キーは全画面表示メニューが有効なときdoClick()しているだけなので全画面表示メニューを無効にすればF11キーも効かなくなる
  public static void appStandbyFullscreen (boolean on) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appStandbyFullscreen(" + on + ")");
    }
    if (XEiJ.pnlIsFullscreenSupported && appFullscreenOn != on) {
      if (on) {  //OFF→ON
        XEiJ.mnbFullscreenMenuItem.setEnabled (false);  //全画面表示メニューを無効にする
        appMakeRequestDialog ();  //appRequestDialogを作る
        appStyle (appRequestDialog,
                  "display", "block");  //ダイアログを表示する
        appCall (appYesButton, "focus");  //Yesボタンにフォーカスを移す
      } else if (appIsMSIE || appIsChrome) {  //ON→OFFかつMSIEまたはChrome
        appSetFullscreenOn (false);  //全画面表示を解除する
      } else {  //ON→OFFかつMSIEまたはChrome以外
        XEiJ.mnbFullscreenMenuItem.setEnabled (false);  //全画面表示メニューを無効にする
        appMakeExitDialog ();  //appExitDialogを作る
        appStyle (appExitDialog,
                  "display", "block");  //ダイアログを表示する
        appCall (appOkButton, "focus");  //Okボタンにフォーカスを移す
      }
    }
  }  //appStandbyFullscreen(boolean)

  //appSetFullscreenOn (on)
  //  全画面表示を設定する
  public static void appSetFullscreenOn (boolean on) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appSetFullscreenOn(" + on + ")");
    }
    if (XEiJ.pnlIsFullscreenSupported && appFullscreenOn != on) {
      if (on) {  //OFF→ON
        XEiJ.pnlFullscreenOn = true;
        appFullscreenOn = true;
        XEiJ.mnbFullscreenMenuItem.setSelected (true);  //全画面表示メニューを有効にする
        if (false) {
          appCall (appLiftElement, appRequestFullscreenMethod);  //全画面表示に移行する
          XEiJ.pnlPrevFitInWindowOn = XEiJ.pnlFitInWindowOn;  //全画面表示に移行する前にウインドウに合わせていたか
          XEiJ.pnlSetFitInWindowOn (true);  //ウインドウに合わせる
        } else {
          XEiJ.pnlPrevFitInWindowOn = XEiJ.pnlFitInWindowOn;  //全画面表示に移行する前にウインドウに合わせていたか
          XEiJ.pnlSetFitInWindowOn (true);  //ウインドウに合わせる
          appCall (appLiftElement, appRequestFullscreenMethod);  //全画面表示に移行する
        }
        appPrevLiftLeft = appLiftLeft;
        appPrevLiftTop = appLiftTop;
        appLiftLeft = 0;
        appLiftTop = 0;
        appStyle (appLiftElement,
                  "left", "0px",
                  "top", "0px");
      } else {  //ON→OFF
        XEiJ.pnlFullscreenOn = false;
        appFullscreenOn = false;
        appCall (appDocument, appExitFullscreenMethod);  //全画面表示を解除する
        if (XEiJ.pnlPrevFitInWindowOn) {  //全画面表示に移行する前はウインドウに合わせていた
          XEiJ.mnbFitInWindowMenuItem.setSelected (true);  //メニューだけ戻す
          appLiftLeft = appPrevLiftLeft;
          appLiftTop = appPrevLiftTop;
          appStyle (appLiftElement,
                    "left", appLiftLeft + "px",
                    "top", appLiftTop + "px");
        } else {  //全画面表示に移行する前はウインドウに合わせていなかった
          XEiJ.pnlSetFitInWindowOn (false);  //ウインドウに合わせない
        }
      }
    }
  }  //appSetFullscreenOn(boolean)

  //appSetFitInWindowOn (on, width, height)
  //  アプレットをウインドウに合わせるかどうかを設定する
  //  一度documentに接続したアプレットは一瞬でもdocumentから切り離してはならない
  //    appendChildなどでノードを繋ぎ直すのもダメ
  //    style.positionをrelativeからabsoluteに変更するのもダメ
  //  style.left,style.top,style.width,style.heightでレクタングルを変更するだけなら大丈夫
  //    style.width,style.heightを0にするのはダメ
  public static void appSetFitInWindowOn (boolean on, int width, int height) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appSetFitInWindowOn(" + on + "," + width + "," + height + ")");
    }
    if (XEiJ.pnlIsFitInWindowSupported && appFitInWindowOn != on) {
      appFitInWindowOn = on;
      if (on) {  //ウインドウに合わせる
        appStyle (appDocumentBody,
                  "overflow", "hidden");
        appStyle (appElement,
                  "margin", "0px 0px");
        appLiftLeft = appGetDocumentScrollLeft () - appGetInt (appDeckElement, "offsetLeft");
        appLiftTop = appGetDocumentScrollTop () - appGetInt (appDeckElement, "offsetTop");
        appStyle (appLiftElement,
                  "left", appLiftLeft + "px",
                  "top", appLiftTop + "px");
        appSetSize (appGetWindowInnerWidth (width),
                    appGetWindowInnerHeight (height));
      } else {  //ウインドウに合わせない
        appSetSize (width, height);
        //appStyle (appElement,
        //          "margin", "0px 0px");  //0px auto
        appLiftLeft = 0;
        appLiftTop = 0;
        appStyle (appLiftElement,
                  "left", "0px",
                  "top", "0px");
        appStyle (appDocumentBody,
                  "overflow", "auto");
      }
    }
  }  //appSetFitInWindowOn(boolean,int,int)

  //appSetSize (width, height)
  //  アプレットのサイズを設定する
  //  最小サイズの制限は受けない
  public static void appSetSize (int width, int height) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appSetSize(" + width + "," + height + ")");
    }
    if (appWidth != width || appHeight != height) {
      appWidth = width;
      appHeight = height;
      appAttr (appElement,
               "height", height + "",
               "width", width + "");
      appStyle (appElement,
                "height", height + "px",
                "width", width + "px");
    }
  }  //appSetSize(int,int)

  //appSetMinSize (minWidth, minHeight)
  //  アプレットの最小サイズを設定する
  //  現在のサイズが小さすぎるときは大きくする
  //  ウインドウに合わせるときはウインドウを手動でリサイズしたときに小さくなり過ぎないようにする
  public static void appSetMinSize (int minWidth, int minHeight) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appSetMinSize(" + minWidth + "," + minHeight + ")");
    }
    if (appMinWidth != minWidth || appMinHeight != minHeight) {
      appMinWidth = minWidth;
      appMinHeight = minHeight;
      if (appWidth < minWidth || appHeight < minHeight) {
        appSetSize (Math.max (minWidth, appWidth),
                    Math.max (minHeight, appHeight));
      }
    }
  }  //appSetMinSize(int,int)

  //i = appGetWindowInnerWidth (i0)
  //  ウインドウの内側の幅を求める
  //  取り出せないか0以下のときはi0を返す
  public static int appGetWindowInnerWidth (int i0) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetWindowInnerWidth(" + i0 + ")");
    }
    if (true) {
      int i = appGetInt (appWindow, "innerWidth");
      if (i <= 0) {
        i = appGetInt (appDocumentElement, "clientWidth");
        if (i <= 0) {
          i = appGetInt (appDocumentBody, "clientWidth");
          if (i <= 0) {
            i = i0;
          }
        }
      }
      return i;
    } else {
      return (appIsDefined (appWindow, "innerWidth") ? appGetInt (appWindow, "innerWidth") :
              appIsDefined (appDocumentElement, "clientWidth") ? appGetInt (appDocumentElement, "clientWidth") :
              appIsDefined (appDocumentBody, "clientWidth") ? appGetInt (appDocumentBody, "clientWidth") :
              i0);
    }
  }  //appGetWindowInnerWidth(int)

  //i = appGetWindowInnerHeight (i0)
  //  ウインドウの内側の高さを求める
  //  取り出せないか0以下のときはi0を返す
  public static int appGetWindowInnerHeight (int i0) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetWindowInnerHeight(" + i0 + ")");
    }
    if (true) {
      int i = appGetInt (appWindow, "innerHeight");
      if (i <= 0) {
        i = appGetInt (appDocumentElement, "clientHeight");
        if (i <= 0) {
          i = appGetInt (appDocumentBody, "clientHeight");
          if (i <= 0) {
            i = i0;
          }
        }
      }
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    } else {
      int i = (appIsDefined (appWindow, "innerHeight") ? appGetInt (appWindow, "innerHeight") :
               appIsDefined (appDocumentElement, "clientHeight") ? appGetInt (appDocumentElement, "clientHeight") :
               appIsDefined (appDocumentBody, "clientHeight") ? appGetInt (appDocumentBody, "clientHeight") :
               i0);
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    }
  }  //appGetWindowInnerHeight(int)

  //i = appGetDocumentScrollLeft ()
  //  ドキュメントのx方向のスクロール位置を求める
  public static int appGetDocumentScrollLeft () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetDocumentScrollLeft()");
    }
    if (true) {
      int i = appGetInt (appDocumentElement, "scrollLeft");
      if (i == 0) {
        i = appGetInt (appDocumentBody, "scrollLeft");
      }
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    } else {
      int i = (appIsDefined (appDocumentElement, "scrollLeft") ? appGetInt (appDocumentElement, "scrollLeft") :
               appIsDefined (appDocumentBody, "scrollLeft") ? appGetInt (appDocumentBody, "scrollLeft") :
               0);
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    }
  }  //appGetDocumentScrollLeft()

  //i = appGetDocumentScrollTop ()
  //  ドキュメントのy方向のスクロール位置を求める
  public static int appGetDocumentScrollTop () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetDocumentScrollTop()");
    }
    if (true) {
      int i = appGetInt (appDocumentElement, "scrollTop");
      if (i == 0) {
        i = appGetInt (appDocumentBody, "scrollTop");
      }
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    } else {
      int i = (appIsDefined (appDocumentElement, "scrollTop") ? appGetInt (appDocumentElement, "scrollTop") :
               appIsDefined (appDocumentBody, "scrollTop") ? appGetInt (appDocumentBody, "scrollTop") :
               0);
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + i);
      }
      return i;
    }
  }  //appGetDocumentScrollTop()

  //r = appGetInt (o, m)
  //  r=o.m
  //  結果をintで返す
  //  失敗したときは0を返す
  //  try～catch～と(Number)とintValue()を書く手間を省く
  public static int appGetInt (JSObject o, String m) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetInt(" + o + "," + m + ")");
    }
    int r = 0;
    try {
      r = ((Number) o.getMember (m)).intValue ();  //ChromeはDouble,IE11はInteger
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appGetInt(JSObject,String)

  //r = appGetString (o, m)
  //  r=o.m
  //  結果をStringで返す
  //  失敗したときはnullを返す
  //  try～catch～と(String)を書く手間を省く
  public static String appGetString (JSObject o, String m) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetString(" + o + "," + m + ")");
    }
    String r = null;
    try {
      r = (String) o.getMember (m);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appGetString(JSObject,String)

  //r = appGetJSObject (o, m)
  //  r=o.m
  //  結果をJSObjectで返す
  //  失敗したときはnullを返す
  //  try～catch～と(JSObject)を書く手間を省く
  public static JSObject appGetJSObject (JSObject o, String m) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appGetJSObject(" + o + "," + m + ")");
    }
    JSObject r = null;
    try {
      r = (JSObject) o.getMember (m);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appGetJSObject(JSObject,String)

  //r = appIsDefined (o, m)
  //  r=typeof(o.m)!="undefined"
  //  結果をbooleanで返す
  //  try～catch～を書く手間を省く
  public static boolean appIsDefined (JSObject o, String m) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appIsDefined(" + o + "," + m + ")");
    }
    try {
      o.getMember (m);
      if (APP_DEBUG_TRACE) {
        System.out.println ("=true");
      }
      return true;
    } catch (Exception e) {  //NullPointerException | JSException
      if (APP_DEBUG_TRACE) {
        System.out.println ("=false");
      }
      return false;
    }
  }  //appIsDefined(JSObject,String)

  //appCall (o, m, p1, p2, ...)
  //  o.m(p1,p2,...)
  //  結果は捨てる
  //  try～catch～とnew Object[] { p1, p2, ... }を書く手間を省く
  public static void appCall (JSObject o, String m, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appCall(" + o + "," + m + ",...)");
    }
    try {
      Object r = o.call (m, pa);
      if (APP_DEBUG_TRACE) {
        System.out.println ("=" + r);
      }
    } catch (Exception e) {  //NullPointerException | JSException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
  }  //appCall(JSObject,String,Object...)

  //r = appCallBoolean (o, m, p1, p2, ...)
  //  r=o.m(p1,p2,...)
  //  結果をbooleanで返す
  //  失敗したときはfalseを返す
  //  try～catch～とnew Object[] { p1, p2, ... }と(boolean)を書く手間を省く
  public static boolean appCallBoolean (JSObject o, String m, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appCallBoolean(" + o + "," + m + ",...)");
    }
    boolean r = false;
    try {
      r = (boolean) o.call (m, pa);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appCallBoolean(JSObject,String,Object...)

  //r = appCallJSObject (o, m, p1, p2, ...)
  //  r=o.m(p1,p2,...)
  //  結果をJSObjectで返す
  //  失敗したときはnullを返す
  //  try～catch～とnew Object[] { p1, p2, ... }と(JSObject)を書く手間を省く
  public static JSObject appCallJSObject (JSObject o, String m, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appCallJSObject(" + o + "," + m + ",...)");
    }
    JSObject r = null;
    try {
      r = (JSObject) o.call (m, pa);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appCallJSObject(JSObject,String,Object)

  //r = appCallString (o, m, p1, p2, ...)
  //  r=o.m(p1,p2,...)
  //  結果をStringで返す
  //  失敗したときはnullを返す
  //  try～catch～とnew Object[] { p1, p2, ... }と(String)を書く手間を省く
  public static String appCallString (JSObject o, String m, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appCallString(" + o + "," + m + ",...)");
    }
    String r = null;
    try {
      r = (String) o.call (m, pa);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appCallJSObject(JSObject,String,Object)

  //r = appEvalJSObject (s)
  //  r=eval(s)
  //  結果をJSObjectで返す
  //  失敗したときはnullを返す
  //  try～catch～とappWindowと(JSObject)を書く手間を省く
  public static JSObject appEvalJSObject (String s) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appEvalJSObject(" + s + ")");
    }
    JSObject r = null;
    try {
      r = (JSObject) appWindow.eval (s);
    } catch (Exception e) {  //NullPointerException | JSException | ClassCastException
      if (APP_DEBUG_TRACE) {
        e.printStackTrace ();
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appEvalJSObject(String)

  //o = appCreate (s)
  //  要素を作る
  public static JSObject appCreate (String s) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appCreate(" + s + ")");
    }
    JSObject r = appCallJSObject (appDocument, "createElement", s);
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appCreate(String)

  //o = appText (s)
  //  テキストノードを作る
  public static JSObject appText (String s) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appText(" + s + ")");
    }
    JSObject r = appCallJSObject (appDocument, "createTextNode", s);
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + r);
    }
    return r;
  }  //appText(String)

  //o = appAppend (o, p1, p2, ...)
  //  要素oに要素p1,p2,...を追加する
  public static JSObject appAppend (JSObject o, JSObject... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appAppend(" + o.toString () + ",...)");
    }
    for (JSObject p : pa) {
      if (p != null) {
        appCall (o, "appendChild", p);
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + o);
    }
    return o;
  }  //appAppend(JSObject,JSObject...)

  //o = appAttr (o, k1, v1, k2, v2, ...)
  //  o.k1=v1,o.k2=v2,...
  //  要素の属性を設定する
  public static JSObject appAttr (JSObject o, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appAttr(" + o.toString () + ",...)");
    }
    if (o != null) {
      for (int i = 0; i + 1 < pa.length; i += 2) {
        try {
          String k = (String) pa[i];
          if (k != null) {
            o.setMember (k, pa[i + 1]);
          }
        } catch (Exception e) {
          if (APP_DEBUG_TRACE) {
            e.printStackTrace ();
          }
        }
      }
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + o);
    }
    return o;
  }  //appAttr(JSObject,Object...)

  //o = appStyle (o, k1, v1, k2, v2, ...)
  //  o.style.k1=v1,o.style.k2=v2,...
  //  要素のスタイルを設定する
  public static JSObject appStyle (JSObject o, Object... pa) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appStyle(" + o.toString () + ",...)");
    }
    appAttr (appGetJSObject (o, "style"), pa);  //返却値はoではなくてo.styleなのでこれを返してはならない
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + o);
    }
    return o;
  }  //appStyle(JSObject,Object...)

  //o = appEvent (o, s, p)
  //  要素にイベントリスナーを追加する
  public static JSObject appEvent (JSObject o, String s, String t) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appEvent(" + o.toString () + "," + s + "," + t + ")");
    }
    JSObject p = appEvalJSObject ("(function (event) { " + t + " })");  //function(){}は括弧で括らないとエラーになる
    if (appIsDefined (o, "addEventListener")) {
      appCall (o, "addEventListener", s, p, false);  //IE11はon～にsetMemberしても動かない
    } else {
      o.setMember ("on" + s, p);
    }
    if (APP_DEBUG_TRACE) {
      System.out.println ("=" + o);
    }
    return o;
  }  //appEvent(JSObject,String,String)



  //init ()
  //  アプレットの初期化
  @Override public void init () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("init()");
    }
    XEiJ.prgIsApplet = true;  //アプレット
    appApplet = this;
    XEiJ.prgIsJnlp = false;  //JNLPでない
    XEiJ.prgIsLocal = false;  //ローカルでない
    XEiJ.prgArgs = null;
    //起動する
    SwingUtilities.invokeLater (new Runnable () {
      @Override public void run () {
        new XEiJ ();
      }
    });
  }  //init()

  //stop ()
  //  アプレットの停止
  @Override public void stop () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("stop()");
    }
    XEiJ.prgTini ();
  }  //stop()

  //appWindowOnresize ()
  //  windowオブジェクトのresizeイベント
  public void appWindowOnresize () {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appWindowOnresize()");
    }
    if (appFitInWindowOn) {  //ウインドウに合わせるとき。全画面表示を含む
      appSetSize (Math.max (appMinWidth, appGetWindowInnerWidth (768)),
                  Math.max (appMinHeight, appGetWindowInnerHeight (639)));
    }
    if (appFullscreenOn &&
        appFullscreenElementField != null &&
        appGetJSObject (appDocument, appFullscreenElementField) == null) {  //全画面表示だったはずだが全画面表示になっていない
      appSetFullscreenOn (false);
    }
  }

  //appRequestOnclick (on)
  //  全画面表示のYes/Noボタンのclickイベント
  public void appRequestOnclick (boolean on) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appRequestOnclick()");
    }
    appStyle (appRequestDialog,
              "display", "none");  //ダイアログを消す
    XEiJ.pnlPanel.requestFocusInWindow ();  //パネルにフォーカスを戻す
    XEiJ.mnbFullscreenMenuItem.setEnabled (true);  //全画面表示メニューを有効にする
    if (on) {
      appSetFullscreenOn (true);  //全画面表示に移行する
    }
  }

  //appExitOnclick (on)
  //  全画面解除のOkボタンのclickイベント
  public void appExitOnclick (boolean on) {
    if (APP_DEBUG_TRACE) {
      System.out.println ("appExitOnclick()");
    }
    appStyle (appExitDialog,
              "display", "none");  //ダイアログを消す
    XEiJ.pnlPanel.requestFocusInWindow ();  //パネルにフォーカスを戻す
    XEiJ.mnbFullscreenMenuItem.setEnabled (true);  //全画面表示メニューを有効にする
    if (on) {
      appSetFullscreenOn (false);  //全画面表示を解除する
    }
  }

}  //class XEiJApplet



