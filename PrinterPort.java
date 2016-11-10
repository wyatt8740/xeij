//========================================================================================
//  Printer.java
//    en:Printer Port
//    ja:プリンタポート
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//
//!!! 工事中
//
//  プリンタ出力
//    割り込みベクタ番号
//      0x00e9c003に割り込みベクタ番号を設定する
//    割り込みマスク
//      0x00e9c001のbit0を1にすると0x00e9c001のbit5が0→1でプリンタ割り込みがかかる
//    出力
//      プリンタ割り込みを待つか、0x00e9c001のbit5が1になるまで待つ
//        0x00e9c001のbit5は0=ビジー,1=レディ。逆に書かれている資料があるので注意
//      0x00e8c001にデータをセットする
//      0x00e8c003のbit0を0にする
//      0x00e8c003のbit0を1にするとプリンタがデータを受け取る
//      プリンタがビジーのとき0x00e9c001のbit5が0になり、レディで1に戻る
//
//  CZ-8PC4
//    48ドット熱転写カラー漢字プリンタ
//
//  CZ-8PC5
//    CZ-8PC4の後継機
//    明朝体とゴシック体の切り替えができる
//    Oh!X 1991年3月号に紹介記事
//
//  用紙サイズ
//    A4縦
//    B4縦
//    B5縦
//    B5横
//    はがき縦
//    はがき横
//
//  分解能
//    水平
//      1/360in/dot
//      1/360in/dot*25.4mm/in=0.070556mm/dot
//    垂直
//      1/360in/dot
//      1/360in/dot*25.4mm/in=0.070556mm/dot
//
//  印字領域
//    幅
//      8in/行
//      25.4mm/in*8in/行=203.2mm/行
//      8in/行/(1/360in/dot)=2880dot/行
//      A4縦
//        210-14-9=187mm/行
//        187mm/行/25.4mm/in/(1/360)in/dot=2650.3937dot
//      B4縦
//        257-24-30=203mm/行
//    高さ
//      11in/頁
//      25.4mm/in*11in/頁=279.4mm/頁
//      11in/頁/(1/360in/dot)=3960dot/頁
//
//  文字種
//    パイカ
//      10文字/in
//      36dot/文字
//      floor(2880dot/行/36dot/文字)=80文字/行
//      A4縦
//        (210-14-9)/(25.4/10)=73文字/行
//      B5縦
//        (182-13-9)/(25.4/10)=63文字/行
//    エリート
//      12文字/in
//      30dot/文字
//      floor(2880dot/行/30dot/文字)=96文字/行
//      A4縦
//        (210-14-9)/(25.4/12))=88文字/行
//      B5縦
//        (182-13-9)/(25.4/12)=75文字/行
//    縮小
//      17文字/in
//      21dot/文字
//      floor(2880dot/行/21dot/文字)=137文字/行
//      A4縦
//        126文字/行
//      B5縦
//        108文字/行
//    漢字
//      2+48+6=56dot/文字
//      floor(2880dot/行/56dot/文字)=51文字/行
//    半角
//      0+24+4=28dot/文字
//      floor(2880dot/行/28dot/文字)=102文字/行
//      A4縦
//        94文字/行
//      B5縦
//        81文字/行
//    スーパースクリプト、サブスクリプト
//      15文字/in
//      24dot/文字
//      floor(2880dot/行/24dot/文字)=120文字/行
//
package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class PrinterPort {

  public static boolean PRN_CONNECTED_ON = false;  //true=プリンタが繋がっている(ように見える)
  public static boolean prnConnectedOn;  //true=プリンタが繋がっている(ように見える)

  public static final int PRN_DATA   = 0x00e8c001;
  public static final int PRN_STROBE = 0x00e8c003;

  public static int prnData;
  public static int prnStrobe;

  public static void prnInit () {
    prnConnectedOn = PRN_CONNECTED_ON;
    prnReset ();
  }  //prnInit()

  public static void prnReset () {
    prnData = 0;
    prnStrobe = 1;
  }  //prnReset()

  public static int prnReadData () {
    return prnData;
  }  //prnReadData(int)

  public static int prnReadStrobe () {
    return prnStrobe;
  }  //prnReadStrobe(int)

  public static void prnWriteData (int d) {
    prnData = d & 255;
  }  //prnWriteData(int,int)

  public static void prnWriteStrobe (int d) {
    d &= 1;
    if (prnStrobe != d) {
      prnStrobe = d;
      if (d != 0) {  //0→1
        //System.out.printf ("[%02X]", prnData);
        XEiJ.ioiPrnFall ();
        XEiJ.ioiPrnRise ();
      }
    }
  }  //prnWriteStrobe(int,int)

}  //class PrinterPort



