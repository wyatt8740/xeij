//========================================================================================
//  AnchorAdapter.java
//    en:Anchor adapter -- It is a mouse adapter which passes the predetermined URI to a browser when it is clicked.
//    ja:アンカーアダプタ -- クリックされたとき所定のURIをブラウザに渡すマウスアダプタです。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.net.*;  //MalformedURLException,URI,URL

public class AnchorAdapter extends MouseAdapter {
  private URI uri;
  public AnchorAdapter (String str) {
    try {
      uri = new URI (str);
    } catch (Exception e) {
      uri = null;
    }
  }
  @Override public void mouseClicked (MouseEvent me) {
    if (uri != null) {
      try {
        if (XEiJ.prgIsApplet) {  //アプレットのとき
          //Desktop.getDesktop().browse(uri)でもAppletContext.showDocument(uri)に渡ってページが開くが、
          //targetが_selfになるため自分自身を閉じてしまう。これは困る
          //_blankを指定したら今度はタブではなくてポップアップとみなされてブロックされてしまった
          //名前を指定してもポップアップとみなされる
          //他に方法が見当たらないのでとりあえず_blankにしておく
          XEiJApplet.appApplet.getAppletContext ().showDocument (uri.toURL (), "_blank");
        } else {  //ローカルまたはJNLPのとき
          Desktop.getDesktop ().browse (uri);  //URIをブラウザに渡す
        }
      } catch (Exception e) {
        //e.printStackTrace ();
      }
    }
  }
}  //class AnchorAdapter



