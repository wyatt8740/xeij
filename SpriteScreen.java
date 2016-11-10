//========================================================================================
//  SpriteScreen.java
//    en:Sprite screen
//    ja:スプライト画面
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class SpriteScreen {

  //スイッチ
  public static final int SPR_HORIZONTAL_LIMIT = 0;  //スプライトの水平表示限界。0=無制限,32=標準

  //レジスタ
  public static final int SPR_REG0_BG0_X   = 0x00eb0800;  //9-0 BG0スクロールX座標
  public static final int SPR_REG1_BG0_Y   = 0x00eb0802;  //9-0 BG0スクロールY座標
  public static final int SPR_REG2_BG1_X   = 0x00eb0804;  //9-0 BG1スクロールX座標
  public static final int SPR_REG3_BG1_Y   = 0x00eb0806;  //9-0 BG1スクロールY座標
  public static final int SPR_REG4_BG_CTRL = 0x00eb0808;  //9   0=スプライト画面表示OFF,1=スプライト画面表示ON
  //                                                        5-4 BG1 00=BG1にTEXT0を割り当てる,01=BG1にTEXT1を割り当てる
  //                                                        3   BG1 0=BG1表示OFF,1=BG1表示ON
  //                                                        2-1 BG0 00=BG0にTEXT0を割り当てる,01=BG0にTEXT1を割り当てる
  //                                                        0   BG0 0=BG0表示OFF,1=BG0表示ON
  public static final int SPR_REG5_H_TOTAL = 0x00eb080a;  //7-0 スプライト画面水平表示終了位置(水平トータル)
  public static final int SPR_REG6_H_START = 0x00eb080c;  //5-0 スプライト画面水平映像開始位置
  public static final int SPR_REG7_V_START = 0x00eb080e;  //7-0 スプライト画面垂直映像開始位置
  public static final int SPR_REG8_RESO    = 0x00eb0810;  //4   スプライト画面解像度 0=低解像度,1=高解像度
  //                                                        3-2 スプライト画面垂直サイズ 00=256,01=512
  //                                                        1-0 スプライト画面水平サイズ
  //                                                            00=256(BGパターンは8x8,BG仮想画面は512x512)
  //                                                            01=512(BGパターンは16x16,BG仮想画面は1024x1024,BG0のみ)

  //レジスタ
  //  ゼロ拡張
  public static int sprReg0Bg0XPort;
  public static int sprReg0Bg0XMask;
  public static int sprReg0Bg0XTest;
  public static int sprReg0Bg0XCurr;
  public static int sprReg1Bg0YPort;
  public static int sprReg1Bg0YMask;
  public static int sprReg1Bg0YTest;
  public static int sprReg1Bg0YCurr;
  public static int sprReg2Bg1XPort;
  public static int sprReg2Bg1XMask;
  public static int sprReg2Bg1XTest;
  public static int sprReg2Bg1XCurr;
  public static int sprReg3Bg1YPort;
  public static int sprReg3Bg1YMask;
  public static int sprReg3Bg1YTest;
  public static int sprReg3Bg1YCurr;
  public static int sprReg4BgCtrlPort;  //ポートの読み書きに使われる値
  public static int sprReg4BgCtrlMask;  //マスク。0=ポート,1=テスト
  public static int sprReg4BgCtrlTest;  //テストデータ
  public static int sprReg4BgCtrlCurr;  //使用されている値。sprReg4BgCtrlPort & ~sprReg4BgCtrlMask | sprReg4BgCtrlTest & sprReg4BgCtrlMask
  public static int sprReg5HTotalPort;
  public static int sprReg5HTotalMask;
  public static int sprReg5HTotalTest;
  public static int sprReg5HTotalCurr;
  public static int sprReg6HStartPort;
  public static int sprReg6HStartMask;
  public static int sprReg6HStartTest;
  public static int sprReg6HStartCurr;
  public static int sprReg7VStartPort;
  public static int sprReg7VStartMask;
  public static int sprReg7VStartTest;
  public static int sprReg7VStartCurr;
  public static int sprReg8ResoPort;  //ポートの読み書きに使われる値
  public static int sprReg8ResoMask;  //マスク。0=ポート,1=テスト
  public static int sprReg8ResoTest;  //テストデータ
  public static int sprReg8ResoCurr;  //使用されている値。sprReg8ResoPort & ~sprReg8ResoMask | sprReg8ResoTest & sprReg8ResoMask

  //スプライトスクロールレジスタ
  public static final short[] sprX = new short[128];  //x座標(0～1023)
  public static final short[] sprY = new short[128];  //y座標(0～1023)
  public static final short[] sprNum = new short[128];  //パターン番号(0～255)
  public static final short[] sprColPort = new short[128];  //パレットブロック(0～15)<<4
  public static final byte[] sprPrw = new byte[128];  //プライオリティ(0～3)
  public static final boolean[] sprH = new boolean[128];  //水平反転
  public static final boolean[] sprV = new boolean[128];  //垂直反転

  //ラスタ毎にどのスプライトが含まれているかを示すテーブル
  //  1ラスタあたりint*4=128ビット
  //  プライオリティが0のスプライトは含まれない
  //  スプライト座標は1023までで縦16ビットなので1038まで1039ラスタ必要
  //  下からはみ出したスプライトが上から出てくるということはない
  //  [16]が画面上の最初のラスタになるので描画の際に注意すること
  public static final int[] sprRmap0 = new int[1039];
  public static final int[] sprRmap1 = new int[1039];
  public static final int[] sprRmap2 = new int[1039];
  public static final int[] sprRmap3 = new int[1039];
  public static final int[][] sprRmap = { sprRmap0, sprRmap1, sprRmap2, sprRmap3 };
  public static final boolean SPR_RRMAP = true;
  public static final int[] sprRRmap = new int[1039 << 2];

  //パターン
  //  1要素に8ピクセル(4*8=32ビット)ずつ入れる
  //  上位が左側のピクセル
  public static final int[] sprPatPort = new int[8192];

  //パターン毎にどのスプライトが使用されているかを示すテーブル
  //  1パターンあたりint*4=128ビット
  //  プライオリティが0のスプライトは含まれない
  public static final int[] sprPmap0 = new int[256];
  public static final int[] sprPmap1 = new int[256];
  public static final int[] sprPmap2 = new int[256];
  public static final int[] sprPmap3 = new int[256];
  public static final int[][] sprPmap = { sprPmap0, sprPmap1, sprPmap2, sprPmap3 };
  public static final int[] sprPPmap = new int[256 << 2];

  //テキストエリア
  public static final short[] sprT0Num = new short[4096];  //テキストエリア0 パターン番号<<3
  public static final short[] sprT0ColPort = new short[4096];  //テキストエリア0 パレットブロック<<4
  public static short[] sprT0ColCurr;
  public static final boolean[] sprT0H = new boolean[4096];  //テキストエリア0 水平反転
  public static final boolean[] sprT0V = new boolean[4096];  //テキストエリア0 垂直反転。0=しない,15=する
  public static final short[] sprT1Num = new short[4096];  //テキストエリア1 パターン番号<<3
  public static final short[] sprT1ColPort = new short[4096];  //テキストエリア1 パレットブロック<<4
  public static short[] sprT1ColCurr;
  public static final boolean[] sprT1H = new boolean[4096];  //テキストエリア1 水平反転
  public static final boolean[] sprT1V = new boolean[4096];  //テキストエリア1 垂直反転。0=しない,15=する

  public static final boolean SPR_THREE_STEPS = true;
  public static int[] sprBuffer;  //表バッファ
  public static int[] sprShadowBuffer;  //裏バッファ
  public static boolean sprActive;  //垂直映像期間の先頭で(sprReg8ResoCurr&10)==0のとき、そのフレームはスプライト画面が構築され、すべてのラスタが描画される

  //パターンテスト
  public static final boolean SPR_PATTEST_ON = true;
  public static final int SPR_PATTEST_MARK = '˙';  //上下左右の反転を示す印。'^'(U+005E;CIRCUMFLEX ACCENT),'~'(U+007E;TILDE),'¨'(U+00A8;DIAERESIS),'˙'(U+02D9;DOT ABOVE)
  public static final int[] sprPatTest = new int[8192];
  public static int[] sprPatCurr;
  public static final short[] sprColTest = new short[128];  //パターンテスト用のパレットブロック(Sp)。(16～19)<<4
  public static final short[] sprT0ColTest = new short[4096];  //パターンテスト用のパレットブロック(T0)。20<<4
  public static final short[] sprT1ColTest = new short[4096];  //パターンテスト用のパレットブロック(T1)。21<<4
  public static short[] sprColCurr;  //現在のパレットブロック(0～16)<<4

  //sprInit ()
  //  スプライト画面を初期化する
  public static void sprInit () {
    sprReg0Bg0XPort = 0;
    sprReg0Bg0XMask = 0;
    sprReg0Bg0XTest = 0;
    sprReg0Bg0XCurr = 0;
    sprReg1Bg0YPort = 0;
    sprReg1Bg0YMask = 0;
    sprReg1Bg0YTest = 0;
    sprReg1Bg0YCurr = 0;
    sprReg2Bg1XPort = 0;
    sprReg2Bg1XMask = 0;
    sprReg2Bg1XTest = 0;
    sprReg2Bg1XCurr = 0;
    sprReg3Bg1YPort = 0;
    sprReg3Bg1YMask = 0;
    sprReg3Bg1YTest = 0;
    sprReg3Bg1YCurr = 0;
    sprReg4BgCtrlPort = 0;
    sprReg4BgCtrlMask = 0;
    sprReg4BgCtrlTest = 0;
    sprReg4BgCtrlCurr = 0;
    sprReg5HTotalPort = 0;
    sprReg5HTotalMask = 0;
    sprReg5HTotalTest = 0;
    sprReg5HTotalCurr = 0;
    sprReg6HStartPort = 0;
    sprReg6HStartMask = 0;
    sprReg6HStartTest = 0;
    sprReg6HStartCurr = 0;
    sprReg7VStartPort = 0;
    sprReg7VStartMask = 0;
    sprReg7VStartTest = 0;
    sprReg7VStartCurr = 0;
    sprReg8ResoPort = 0;
    sprReg8ResoMask = 0;
    sprReg8ResoTest = 0;
    sprReg8ResoCurr = 0;
    //sprX = new short[128];
    //sprY = new short[128];
    //sprNum = new short[128];
    //sprColPort = new short[128];
    //sprPrw = new byte[128];
    //sprH = new boolean[128];
    //sprV = new boolean[128];
    if (SPR_RRMAP) {
      //sprRRmap = new int[1039 << 2];
      //sprPPmap = new int[256 << 2];
    } else {
      //sprRmap = new int[4][];
      //sprRmap[0] = sprRmap0 = new int[1039];
      //sprRmap[1] = sprRmap1 = new int[1039];
      //sprRmap[2] = sprRmap2 = new int[1039];
      //sprRmap[3] = sprRmap3 = new int[1039];
      //sprPmap = new int[4][];
      //sprPmap[0] = sprPmap0 = new int[256];
      //sprPmap[1] = sprPmap1 = new int[256];
      //sprPmap[2] = sprPmap2 = new int[256];
      //sprPmap[3] = sprPmap3 = new int[256];
    }
    //sprT0Num = new short[4096];
    //sprT0ColPort = new short[4096];
    //sprT0H = new boolean[4096];
    //sprT0V = new boolean[4096];
    //sprT1Num = new short[4096];
    //sprT1ColPort = new short[4096];
    //sprT1H = new boolean[4096];
    //sprT1V = new boolean[4096];

    if (SPR_THREE_STEPS) {
      sprBuffer = new int[1056 * 2];
      sprShadowBuffer = new int[1056 * 2];
    }

    //sprPatPort = new int[8192];
    //パターンテスト
    if (SPR_PATTEST_ON) {
      //スプライトパターン
      //sprPatTest = new int[8192];
      Arrays.fill (sprPatTest, 0x00000000);
      //  BGに0番が並んでいるときBGが手前にあるとスプライトが見えにくくなるので0番は上下左右の反転を示す印だけにする
      if (SPR_PATTEST_MARK == '^') {
        sprPatTest[ 5] = 0x01000000;
        sprPatTest[ 6] = 0x10100000;
        sprPatTest[13] = 0x02000000;
        sprPatTest[14] = 0x20200000;
        sprPatTest[21] = 0x03000000;
        sprPatTest[22] = 0x30300000;
        sprPatTest[29] = 0x04000000;
        sprPatTest[30] = 0x40400000;
      } else if (SPR_PATTEST_MARK == '~') {
        sprPatTest[ 6] = 0x11100000;
        sprPatTest[14] = 0x22200000;
        sprPatTest[22] = 0x33300000;
        sprPatTest[30] = 0x44400000;
      } else if (SPR_PATTEST_MARK == '¨') {
        sprPatTest[ 6] = 0x10100000;
        sprPatTest[14] = 0x20200000;
        sprPatTest[22] = 0x30300000;
        sprPatTest[30] = 0x40400000;
      } else if (SPR_PATTEST_MARK == '˙') {
        sprPatTest[ 6] = 0x01000000;
        sprPatTest[14] = 0x02000000;
        sprPatTest[22] = 0x03000000;
        sprPatTest[30] = 0x04000000;
      }
      for (int i = 32; i < 8192; i += 32) {
        int x1 = i >> 9 & 15;  //上位4bit
        int x0 = i >> 5 & 15;  //下位4bit
        x1 = Indicator.IND_ASCII_3X5[(9 - x1 >> 4 & 7 | 48) + x1];  //上位3x5dot
        x0 = Indicator.IND_ASCII_3X5[(9 - x0 >> 4 & 7 | 48) + x0];  //下位3x5dot
        int p0 = VideoController.VCN_TXP0[x1 >> 12 - 5 & 0b11100000 | x0 >> 12 - 1 & 0b00001110];
        int p1 = VideoController.VCN_TXP0[x1 >>  9 - 5 & 0b11100000 | x0 >>  9 - 1 & 0b00001110];
        int p2 = VideoController.VCN_TXP0[x1 >>  6 - 5 & 0b11100000 | x0 >>  6 - 1 & 0b00001110];
        int p3 = VideoController.VCN_TXP0[x1 <<  5 - 3 & 0b11100000 | x0 >>  3 - 1 & 0b00001110];
        int p4 = VideoController.VCN_TXP0[x1 <<  5 - 0 & 0b11100000 | x0 <<  1 - 0 & 0b00001110];
        //左上
        sprPatTest[i     ] = p0;
        sprPatTest[i +  1] = p1;
        sprPatTest[i +  2] = p2;
        sprPatTest[i +  3] = p3;
        sprPatTest[i +  4] = p4;
        //左下
        sprPatTest[i +  8] = p0 << 1;
        sprPatTest[i +  9] = p1 << 1;
        sprPatTest[i + 10] = p2 << 1;
        sprPatTest[i + 11] = p3 << 1;
        sprPatTest[i + 12] = p4 << 1;
        //右上
        sprPatTest[i + 16] = p0 * 3;
        sprPatTest[i + 17] = p1 * 3;
        sprPatTest[i + 18] = p2 * 3;
        sprPatTest[i + 19] = p3 * 3;
        sprPatTest[i + 20] = p4 * 3;
        //右下
        sprPatTest[i + 24] = p0 << 2;
        sprPatTest[i + 25] = p1 << 2;
        sprPatTest[i + 26] = p2 << 2;
        sprPatTest[i + 27] = p3 << 2;
        sprPatTest[i + 28] = p4 << 2;
        //上下左右の反転を示す印
        if (SPR_PATTEST_MARK == '^') {
          sprPatTest[i +  5] = 0x01000000;
          sprPatTest[i +  6] = 0x10100000;
          sprPatTest[i + 13] = 0x02000000;
          sprPatTest[i + 14] = 0x20200000;
          sprPatTest[i + 21] = 0x03000000;
          sprPatTest[i + 22] = 0x30300000;
          sprPatTest[i + 29] = 0x04000000;
          sprPatTest[i + 30] = 0x40400000;
        } else if (SPR_PATTEST_MARK == '~') {
          sprPatTest[i +  6] = 0x11100000;
          sprPatTest[i + 14] = 0x22200000;
          sprPatTest[i + 22] = 0x33300000;
          sprPatTest[i + 30] = 0x44400000;
        } else if (SPR_PATTEST_MARK == '¨') {
          sprPatTest[i +  6] = 0x10100000;
          sprPatTest[i + 14] = 0x20200000;
          sprPatTest[i + 22] = 0x30300000;
          sprPatTest[i + 30] = 0x40400000;
        } else if (SPR_PATTEST_MARK == '˙') {
          sprPatTest[i +  6] = 0x01000000;
          sprPatTest[i + 14] = 0x02000000;
          sprPatTest[i + 22] = 0x03000000;
          sprPatTest[i + 30] = 0x04000000;
        }
      }
      //パレットブロック
      //sprColTest = new short[128];
      //sprT0ColTest = new short[4096];
      //sprT1ColTest = new short[4096];
      for (int i = 0; i < 128; i++) {
        sprColTest[i] = (short) (16 + (i * (VideoController.VCN_PATTEST_BLOCKS - 2) >> 7) << 4);  //0..127 -> 0..VideoController.VCN_PATTEST_BLOCKS-3
      }
      Arrays.fill (sprT0ColTest, (short) (16 + VideoController.VCN_PATTEST_BLOCKS - 2 << 4));
      Arrays.fill (sprT1ColTest, (short) (16 + VideoController.VCN_PATTEST_BLOCKS - 1 << 4));
    }  //if SPR_PATTEST_ON
    sprPatCurr = sprPatPort;
    sprColCurr = sprColPort;
    sprT0ColCurr = sprT0ColPort;
    sprT1ColCurr = sprT1ColPort;

    sprReset ();
  }  //sprInit()

  //sprReset ()
  //  リセット
  public static void sprReset () {
    Arrays.fill (sprX, (short) 0);
    Arrays.fill (sprY, (short) 0);
    Arrays.fill (sprNum, (short) 0);
    Arrays.fill (sprColPort, (short) 0);
    Arrays.fill (sprPrw, (byte) 0);
    Arrays.fill (sprH, false);
    Arrays.fill (sprV, false);
    if (SPR_RRMAP) {
      Arrays.fill (sprRRmap, 0);
    } else {
      Arrays.fill (sprRmap0, 0);
      Arrays.fill (sprRmap1, 0);
      Arrays.fill (sprRmap2, 0);
      Arrays.fill (sprRmap3, 0);
    }
    Arrays.fill (sprPatPort, 0);
    if (SPR_RRMAP) {
      Arrays.fill (sprPPmap, 0);
    } else {
      Arrays.fill (sprPmap0, 0);
      Arrays.fill (sprPmap1, 0);
      Arrays.fill (sprPmap2, 0);
      Arrays.fill (sprPmap3, 0);
    }
    Arrays.fill (sprT0Num, (short) 0);
    Arrays.fill (sprT0ColPort, (short) 0);
    Arrays.fill (sprT0H, false);
    Arrays.fill (sprT0V, false);
    Arrays.fill (sprT1Num, (short) 0);
    Arrays.fill (sprT1ColPort, (short) 0);
    Arrays.fill (sprT1H, false);
    Arrays.fill (sprT1V, false);
  }  //sprReset()


  //
  //  ノーマル
  //    ラスタ(dst=-2,src=-2)
  //      表(0)にスプライト(0)を並べる
  //      表(0)と裏(-1)を入れ換える
  //    ラスタ(dst=-1,src=-1)
  //      表(-1)を表(1)として再利用する
  //      表(1)にスプライト(1)を並べる
  //      表(1)と裏(0)を入れ換える
  //      表(0)にバックグラウンド(0)を並べる
  //    ラスタ(dst=src)
  //      表(dst)のスプライト(src)とバックグラウンド(src)を重ねる
  //      表(dst)を描画する
  //      表(dst)を表(dst+2)として再利用する
  //      表(dst+2)にスプライト(src+2)を並べる
  //      表(dst+2)と裏(dst+1)を入れ換える
  //      表(dst+1)にバックグラウンド(src+1)を並べる
  //
  //  ラスタ2度読み
  //    偶数ラスタ(dst=-2,src=-1)
  //      表(0)にスプライト(0)を並べる
  //      表(0)と裏(-1)を入れ換える
  //    奇数ラスタ(dst=-1,src=-1)
  //      表(-1)を表(1)として再利用する
  //      表(1)にスプライト(0)を並べる
  //      表(1)と裏(0)を入れ換える
  //      表(0)にバックグラウンド(0)を並べる
  //    偶数ラスタ(dst=src*2)
  //      表(dst)のスプライト(src)とバックグラウンド(src)を重ねる
  //      表(dst)を描画する
  //      表(dst)を表(dst+2)として再利用する
  //      表(dst+2)にスプライト(src+1)を並べる
  //      表(dst+2)と裏(dst+1)を入れ換える
  //      表(dst+1)にバックグラウンド(src)を並べる
  //    奇数ラスタ(dst=src*2+1)
  //      表(dst)のスプライト(src)とバックグラウンド(src)を重ねる
  //      表(dst)を描画する
  //      表(dst)を表(dst+2)として再利用する
  //      表(dst+2)にスプライト(src+1)を並べる
  //      表(dst+2)と裏(dst+1)を入れ換える
  //      表(dst+1)にバックグラウンド(src+1)を並べる
  //
  //  インタレース
  //    ラスタ(dst=-4,src=-4)
  //      表(0)にスプライト(0)を並べる
  //      表(0)と裏(-2)を入れ換える
  //    ラスタ(dst=-2,src=-2)
  //      表(-2)を表(2)として再利用する
  //      表(2)にスプライト(2)を並べる
  //      表(2)と裏(0)を入れ換える
  //      表(0)にバックグラウンド(0)を並べる
  //    ラスタ(dst=src)
  //      表(dst)のスプライト(src)とバックグラウンド(src)を重ねる
  //      表(dst)を描画する
  //      表(dst)を表(dst+4)として再利用する
  //      表(dst+4)にスプライト(src+4)を並べる
  //      表(dst+4)と裏(dst+2)を入れ換える
  //      表(dst+2)にバックグラウンド(src+2)を並べる
  //
  //  スリット
  //    ラスタ(dst=-4,src=-2)
  //      表(0)にスプライト(0)を並べる
  //      表(0)と裏(-2)を入れ換える
  //    ラスタ(dst=-2,src=-1)
  //      表(-2)を表(2)として再利用する
  //      表(2)にスプライト(1)を並べる
  //      表(2)と裏(0)を入れ換える
  //      表(0)にバックグラウンド(0)を並べる
  //    ラスタ(dst=src*2)
  //      表(dst)のスプライト(src)とバックグラウンド(src)を重ねる
  //      表(dst)を描画する
  //      表(dst)を表(dst+4)として再利用する
  //      表(dst+4)にスプライト(src+2)を並べる
  //      表(dst+4)と裏(dst+2)を入れ換える
  //      表(dst+2)にバックグラウンド(src+1)を並べる
  //

  //sprSwap ()
  //  表と裏を入れ換える
  //
  //!!! if (SPR_THREE_STEPS)
  public static void sprSwap () {
    int[] t = sprBuffer;
    sprBuffer = sprShadowBuffer;
    sprShadowBuffer = t;
  }  //sprSwap()

  //sprStep1 (src)
  //  スプライトを並べる
  //
  //  sprBuffer[x]
  //    4bitパレット
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //  sprBuffer[1056 + x]
  //    パレットブロック
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //!!! if (SPR_THREE_STEPS)
  //!!! if (SPR_RRMAP)
  public static void sprStep1 (int src) {
    //バッファをクリアする
    //  4bitパレットとパレットブロックの両方をクリアすること
    Arrays.fill (sprBuffer, 0);
    //垂直映像開始位置の指定に伴う補正
    src += sprReg7VStartCurr - CRTC.crtR06VBackEndCurr;
    if (src < 0 || 1023 < src) {
      return;
    }
    //水平映像開始位置の指定に伴う補正
    int hStart = (sprReg6HStartCurr - CRTC.crtR02HBackEndCurr - 4) << 3;
    int width16 = 16 + XEiJ.pnlScreenWidth;
    int cnt = 0;  //ラスタにかかっているスプライトの数
  nn:
    for (int i = 16 + src << 2, nn = 0; nn <= 128 - 32; nn += 32) {
      for (int map = sprRRmap[i++], n = nn; map != 0; map <<= 1, n++) {  //nは昇順
        if (map >= 0) {  //このスプライトはラスタにかかっていない
          continue;
        }
        int x = hStart + sprX[n];  //X座標。画面左端は16
        if (x <= 0 || width16 <= x) {  //画面外。画面外のスプライトは水平表示限界に影響しない
          continue;
        }
        //  8x8のパターンを
        //    +---+---+
        //    | 0 | 2 |
        //    +---+---+
        //    | 1 | 3 |
        //    +---+---+
        //  の順序で並べる
        int a = (sprNum[n] << 5) + (sprV[n] ? sprY[n] - src - 1 : 16 + src - sprY[n]);
        int prw = sprPrw[n] << 3;  //プライオリティ*8。表示されていることがわかっているのでプライオリティは1～3のいずれかであるはず
        int col = sprColCurr[n] << prw >>> 4;  //パレットブロック
        int s, t;
        if ((t = sprPatCurr[a]) != 0) {  //左半分のパターンあり
          if (sprH[n]) {  //水平反転あり。左半分→右半分
            if ((s = 15       & t) != 0 && sprBuffer[ 8 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        8 + x] = s        << prw;
              sprBuffer[1056 +  8 + x] = col;
            }
            if ((s = 15 <<  4 & t) != 0 && sprBuffer[ 9 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        9 + x] = s >>>  4 << prw;
              sprBuffer[1056 +  9 + x] = col;
            }
            if ((s = 15 <<  8 & t) != 0 && sprBuffer[10 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       10 + x] = s >>>  8 << prw;
              sprBuffer[1056 + 10 + x] = col;
            }
            if ((s = 15 << 12 & t) != 0 && sprBuffer[11 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       11 + x] = s >>> 12 << prw;
              sprBuffer[1056 + 11 + x] = col;
            }
            if ((s = 15 << 16 & t) != 0 && sprBuffer[12 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       12 + x] = s >>> 16 << prw;
              sprBuffer[1056 + 12 + x] = col;
            }
            if ((s = 15 << 20 & t) != 0 && sprBuffer[13 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       13 + x] = s >>> 20 << prw;
              sprBuffer[1056 + 13 + x] = col;
            }
            if ((s = 15 << 24 & t) != 0 && sprBuffer[14 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       14 + x] = s >>> 24 << prw;
              sprBuffer[1056 + 14 + x] = col;
            }
            if ((s = t >>> 28    ) != 0 && sprBuffer[15 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       15 + x] = s        << prw;
              sprBuffer[1056 + 15 + x] = col;
            }
          } else {  //水平反転なし。左半分→左半分
            if ((s = t >>> 28    ) != 0 && sprBuffer[     x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[            x] = s        << prw;
              sprBuffer[1056      + x] = col;
            }
            if ((s = 15 << 24 & t) != 0 && sprBuffer[ 1 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        1 + x] = s >>> 24 << prw;
              sprBuffer[1056 +  1 + x] = col;
            }
            if ((s = 15 << 20 & t) != 0 && sprBuffer[ 2 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        2 + x] = s >>> 20 << prw;
              sprBuffer[1056 +  2 + x] = col;
            }
            if ((s = 15 << 16 & t) != 0 && sprBuffer[ 3 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        3 + x] = s >>> 16 << prw;
              sprBuffer[1056 +  3 + x] = col;
            }
            if ((s = 15 << 12 & t) != 0 && sprBuffer[ 4 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        4 + x] = s >>> 12 << prw;
              sprBuffer[1056 +  4 + x] = col;
            }
            if ((s = 15 <<  8 & t) != 0 && sprBuffer[ 5 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        5 + x] = s >>>  8 << prw;
              sprBuffer[1056 +  5 + x] = col;
            }
            if ((s = 15 <<  4 & t) != 0 && sprBuffer[ 6 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        6 + x] = s >>>  4 << prw;
              sprBuffer[1056 +  6 + x] = col;
            }
            if ((s = 15       & t) != 0 && sprBuffer[ 7 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        7 + x] = s        << prw;
              sprBuffer[1056 +  7 + x] = col;
            }
          }  //if 水平反転あり/水平反転なし
        }  //if 左半分のパターンあり
        if ((t = sprPatCurr[16 + a]) != 0) {  //右半分のパターンあり
          if (sprH[n]) {  //水平反転あり。右半分→左半分
            if ((s = 15       & t) != 0 && sprBuffer[     x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[            x] = s        << prw;
              sprBuffer[1056      + x] = col;
            }
            if ((s = 15 <<  4 & t) != 0 && sprBuffer[ 1 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        1 + x] = s >>>  4 << prw;
              sprBuffer[1056 +  1 + x] = col;
            }
            if ((s = 15 <<  8 & t) != 0 && sprBuffer[ 2 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        2 + x] = s >>>  8 << prw;
              sprBuffer[1056 +  2 + x] = col;
            }
            if ((s = 15 << 12 & t) != 0 && sprBuffer[ 3 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        3 + x] = s >>> 12 << prw;
              sprBuffer[1056 +  3 + x] = col;
            }
            if ((s = 15 << 16 & t) != 0 && sprBuffer[ 4 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        4 + x] = s >>> 16 << prw;
              sprBuffer[1056 +  4 + x] = col;
            }
            if ((s = 15 << 20 & t) != 0 && sprBuffer[ 5 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        5 + x] = s >>> 20 << prw;
              sprBuffer[1056 +  5 + x] = col;
            }
            if ((s = 15 << 24 & t) != 0 && sprBuffer[ 6 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        6 + x] = s >>> 24 << prw;
              sprBuffer[1056 +  6 + x] = col;
            }
            if ((s = t >>> 28    ) != 0 && sprBuffer[ 7 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        7 + x] = s        << prw;
              sprBuffer[1056 +  7 + x] = col;
            }
          } else {  //水平反転なし。右半分→右半分
            if ((s = t >>> 28    ) != 0 && sprBuffer[ 8 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        8 + x] = s        << prw;
              sprBuffer[1056  + 8 + x] = col;
            }
            if ((s = 15 << 24 & t) != 0 && sprBuffer[ 9 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[        9 + x] = s >>> 24 << prw;
              sprBuffer[1056 +  9 + x] = col;
            }
            if ((s = 15 << 20 & t) != 0 && sprBuffer[10 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       10 + x] = s >>> 20 << prw;
              sprBuffer[1056 + 10 + x] = col;
            }
            if ((s = 15 << 16 & t) != 0 && sprBuffer[11 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       11 + x] = s >>> 16 << prw;
              sprBuffer[1056 + 11 + x] = col;
            }
            if ((s = 15 << 12 & t) != 0 && sprBuffer[12 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       12 + x] = s >>> 12 << prw;
              sprBuffer[1056 + 12 + x] = col;
            }
            if ((s = 15 <<  8 & t) != 0 && sprBuffer[13 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       13 + x] = s >>>  8 << prw;
              sprBuffer[1056 + 13 + x] = col;
            }
            if ((s = 15 <<  4 & t) != 0 && sprBuffer[14 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       14 + x] = s >>>  4 << prw;
              sprBuffer[1056 + 14 + x] = col;
            }
            if ((s = 15       & t) != 0 && sprBuffer[15 + x] >>> prw == 0) {  //ドットがありかつ若い番号のスプライトよりも優先順位が高い
              sprBuffer[       15 + x] = s        << prw;
              sprBuffer[1056 + 15 + x] = col;
            }
          }  //if 水平反転あり/水平反転なし
        }  //if 右半分のパターンあり
        if (SPR_HORIZONTAL_LIMIT != 0) {
          if (++cnt == SPR_HORIZONTAL_LIMIT) {  //今回のスプライトで終わりにする
            break nn;
          }
        }
      }  //for map,n
    }  // for i,nn
  }  //sprStep1(int)

  //sprStep2 (src)
  //  バックグラウンドを並べる
  //
  //  sprBuffer[x]
  //    4bitパレット
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //  sprBuffer[1056 + x]
  //    パレットブロック
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //!!! if (SPR_THREE_STEPS)
  public static void sprStep2 (int src) {
    //垂直映像開始位置の指定に伴う補正
    src += sprReg7VStartCurr - CRTC.crtR06VBackEndCurr;
    if (src < 0 || 1023 < src) {
      return;
    }
    //水平映像開始位置の指定に伴う補正
    int hStart = (sprReg6HStartCurr - CRTC.crtR02HBackEndCurr - 4) << 3;
    int width16 = 16 + XEiJ.pnlScreenWidth;
    if ((sprReg8ResoCurr & 3) == 0) {  //水平256ドット、BGパターンは8x8、BG仮想画面は512x512、BG0とBG1
      short[] tnum, tcol;
      boolean[] th, tv;
      int x, y, sx, sy;
      //BG0
      //  BG0の有無は表示ラスタまで分からないので1ラスタ手前では常に展開しておなかければならない
      if ((sprReg4BgCtrlCurr & 3 << 1) == 0) {  //BG0にTEXT0が割り当てられている
        tnum = sprT0Num;  //パターン番号
        tcol = sprT0ColCurr;  //パレットブロック<<4
        th = sprT0H;  //水平反転
        tv = sprT0V;  //垂直反転
      } else {  //BG0にTEXT1が割り当てられている
        tnum = sprT1Num;  //パターン番号
        tcol = sprT1ColCurr;  //パレットブロック<<4
        th = sprT1H;  //水平反転
        tv = sprT1V;  //垂直反転
      }
      x = 16 + hStart - sprReg0Bg0XCurr;  //X座標。画面左端は16
      y = src + sprReg1Bg0YCurr & 511;
      sx = ((x & 7) - x >> 3) & 63;  //テキストX座標
      sy = y >> 3 << 6;  //テキストY座標*64
      x &= 7;
      y &= 7;
      while (x < width16) {
        int t;
        if ((t = sprPatCurr[tnum[sy + sx] + (tv[sy + sx] ? 7 - y : y)]) != 0) {  //パターンあり
          if (th[sy + sx]) {  //水平反転あり
            sprBuffer[     x] |= (t        & 15) << 20;
            sprBuffer[ 1 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[ 2 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[ 3 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[ 4 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[ 5 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[ 6 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[ 7 + x] |= (t >>> 28     ) << 20;
          } else {  //水平反転なし
            sprBuffer[     x] |= (t >>> 28     ) << 20;
            sprBuffer[ 1 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[ 2 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[ 3 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[ 4 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[ 5 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[ 6 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[ 7 + x] |= (t        & 15) << 20;
          }  //if 水平反転あり/水平反転なし
        }  //if パターンあり
        if ((t = tcol[sy + sx]) != 0) {  //パレットブロックが0でないとき。バックグラウンドは4bitパレットが0でもパレットブロックが必要
          t <<= 20 - 4;  //tcolはパレットブロック<<4。ここでは4bitパレットと同じ位置に置く
          sprBuffer[1056      + x] |= t;
          sprBuffer[1056 +  1 + x] |= t;
          sprBuffer[1056 +  2 + x] |= t;
          sprBuffer[1056 +  3 + x] |= t;
          sprBuffer[1056 +  4 + x] |= t;
          sprBuffer[1056 +  5 + x] |= t;
          sprBuffer[1056 +  6 + x] |= t;
          sprBuffer[1056 +  7 + x] |= t;
        }
        x += 8;
        sx = sx + 1 & 63;
      }  //while x<width16
      //BG1
      //  BG1の有無は表示ラスタまで分からないので1ラスタ手前では常に展開しておなかければならない
      if ((sprReg4BgCtrlCurr & 3 << 4) == 0) {  //BG1にTEXT0が割り当てられている
        tnum = sprT0Num;  //パターン番号
        tcol = sprT0ColCurr;  //パレットブロック<<4
        th = sprT0H;  //水平反転
        tv = sprT0V;  //垂直反転
      } else {  //BG1にTEXT1が割り当てられている
        tnum = sprT1Num;  //パターン番号
        tcol = sprT1ColCurr;  //パレットブロック<<4
        th = sprT1H;  //水平反転
        tv = sprT1V;  //垂直反転
      }
      x = 16 + hStart - sprReg2Bg1XCurr;  //X座標。画面左端は16
      y = src + sprReg3Bg1YCurr & 511;
      sx = ((x & 7) - x >> 3) & 63;  //テキストX座標
      sy = y >> 3 << 6;  //テキストY座標*64
      x &= 7;
      y &= 7;
      while (x < width16) {
        int t;
        if ((t = sprPatCurr[tnum[sy + sx] + (tv[sy + sx] ? 7 - y : y)]) != 0) {  //パターンあり
          if (th[sy + sx]) {  //水平反転あり
            sprBuffer[     x] |= (t        & 15) << 12;
            sprBuffer[ 1 + x] |= (t >>>  4 & 15) << 12;
            sprBuffer[ 2 + x] |= (t >>>  8 & 15) << 12;
            sprBuffer[ 3 + x] |= (t >>> 12 & 15) << 12;
            sprBuffer[ 4 + x] |= (t >>> 16 & 15) << 12;
            sprBuffer[ 5 + x] |= (t >>> 20 & 15) << 12;
            sprBuffer[ 6 + x] |= (t >>> 24 & 15) << 12;
            sprBuffer[ 7 + x] |= (t >>> 28     ) << 12;
          } else {  //水平反転なし
            sprBuffer[     x] |= (t >>> 28     ) << 12;
            sprBuffer[ 1 + x] |= (t >>> 24 & 15) << 12;
            sprBuffer[ 2 + x] |= (t >>> 20 & 15) << 12;
            sprBuffer[ 3 + x] |= (t >>> 16 & 15) << 12;
            sprBuffer[ 4 + x] |= (t >>> 12 & 15) << 12;
            sprBuffer[ 5 + x] |= (t >>>  8 & 15) << 12;
            sprBuffer[ 6 + x] |= (t >>>  4 & 15) << 12;
            sprBuffer[ 7 + x] |= (t        & 15) << 12;
          }  //if 水平反転あり/水平反転なし
        }  //if パターンあり
        if ((t = tcol[sy + sx]) != 0) {  //パレットブロックが0でないとき。バックグラウンドは4bitパレットが0でもパレットブロックが必要
          t <<= 12 - 4;  //tcolはパレットブロック<<4。ここでは4bitパレットと同じ位置に置く
          sprBuffer[1056      + x] |= t;
          sprBuffer[1056 +  1 + x] |= t;
          sprBuffer[1056 +  2 + x] |= t;
          sprBuffer[1056 +  3 + x] |= t;
          sprBuffer[1056 +  4 + x] |= t;
          sprBuffer[1056 +  5 + x] |= t;
          sprBuffer[1056 +  6 + x] |= t;
          sprBuffer[1056 +  7 + x] |= t;
        }
        x += 8;
        sx = sx + 1 & 63;
      }  //while x<width16
    } else {  //水平512ドット、BGパターンは16x16、BG仮想画面は1024x1024、BG0のみ
      short[] tnum, tcol;
      boolean[] th, tv;
      int x, y, sx, sy;
      //BG0
      //  BG0の有無は表示ラスタまで分からないので1ラスタ手前では常に展開しておなかければならない
      if ((sprReg4BgCtrlCurr & 6) == 0) {  //BG0にTEXT0が割り当てられている
        tnum = sprT0Num;  //パターン番号
        tcol = sprT0ColCurr;  //パレットブロック<<4
        th = sprT0H;  //水平反転
        tv = sprT0V;  //垂直反転
      } else {  //BG0にTEXT1が割り当てられている
        tnum = sprT1Num;  //パターン番号
        tcol = sprT1ColCurr;  //パレットブロック<<4
        th = sprT1H;  //水平反転
        tv = sprT1V;  //垂直反転
      }
      x = 16 + hStart - sprReg0Bg0XCurr;  //X座標。画面左端は16
      y = src + sprReg1Bg0YCurr & 1023;
      sx = ((x & 15) - x >> 4) & 63;  //テキストX座標
      sy = y >> 4 << 6;  //テキストY座標*64
      x &= 15;
      y &= 15;
      while (x < width16) {
        int a = (tnum[sy + sx] << 2) + (tv[sy + sx] ? 15 - y : y);
        int t;
        if ((t = sprPatCurr[a]) != 0) {  //左半分のパターンあり
          if (th[sy + sx]) {  //水平反転あり。左半分→右半分
            sprBuffer[ 8 + x] |= (t        & 15) << 20;
            sprBuffer[ 9 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[10 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[11 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[12 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[13 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[14 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[15 + x] |= (t >>> 28     ) << 20;
          } else {  //水平反転なし。左半分→左半分
            sprBuffer[     x] |= (t >>> 28     ) << 20;
            sprBuffer[ 1 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[ 2 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[ 3 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[ 4 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[ 5 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[ 6 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[ 7 + x] |= (t        & 15) << 20;
          }  //if 水平反転あり/水平反転なし
        }  //if 左半分のパターンあり
        if ((t = sprPatCurr[16 + a]) != 0) {  //右半分のパターンあり
          if (th[sy + sx]) {  //水平反転あり。右半分→左半分
            sprBuffer[     x] |= (t        & 15) << 20;
            sprBuffer[ 1 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[ 2 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[ 3 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[ 4 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[ 5 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[ 6 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[ 7 + x] |= (t >>> 28     ) << 20;
          } else {  //水平反転なし。右半分→右半分
            sprBuffer[ 8 + x] |= (t >>> 28     ) << 20;
            sprBuffer[ 9 + x] |= (t >>> 24 & 15) << 20;
            sprBuffer[10 + x] |= (t >>> 20 & 15) << 20;
            sprBuffer[11 + x] |= (t >>> 16 & 15) << 20;
            sprBuffer[12 + x] |= (t >>> 12 & 15) << 20;
            sprBuffer[13 + x] |= (t >>>  8 & 15) << 20;
            sprBuffer[14 + x] |= (t >>>  4 & 15) << 20;
            sprBuffer[15 + x] |= (t        & 15) << 20;
          }  //if 水平反転あり/水平反転なし
        }  //if 右半分のパターンあり
        if ((t = tcol[sy + sx]) != 0) {  //パレットブロックが0でないとき。バックグラウンドは4bitパレットが0でもパレットブロックが必要
          t <<= 20 - 4;  //tcolはパレットブロック<<4。ここでは4bitパレットと同じ位置に置く
          sprBuffer[1056      + x] |= t;
          sprBuffer[1056 +  1 + x] |= t;
          sprBuffer[1056 +  2 + x] |= t;
          sprBuffer[1056 +  3 + x] |= t;
          sprBuffer[1056 +  4 + x] |= t;
          sprBuffer[1056 +  5 + x] |= t;
          sprBuffer[1056 +  6 + x] |= t;
          sprBuffer[1056 +  7 + x] |= t;
          sprBuffer[1056 +  8 + x] |= t;
          sprBuffer[1056 +  9 + x] |= t;
          sprBuffer[1056 + 10 + x] |= t;
          sprBuffer[1056 + 11 + x] |= t;
          sprBuffer[1056 + 12 + x] |= t;
          sprBuffer[1056 + 13 + x] |= t;
          sprBuffer[1056 + 14 + x] |= t;
          sprBuffer[1056 + 15 + x] |= t;
        }  //if パレットブロックが0でないとき
        x += 16;
        sx = sx + 1 & 63;
      }  //while x<width16
    }  //if 水平256ドット/水平512ドット
  }  //sprStep2(int)

  //sprStep3 ()
  //  スプライトとバックグラウンドを重ねる
  //
  //  sprBuffer[x]
  //    4bitパレット
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //  sprBuffer[1056 + x]
  //    パレットブロック
  //             28  24  20  16  12   8   4   0
  //    手前← |---|SP3|BG0|SP2|BG1|SP1|---|---| →奥
  //
  //!!! if (SPR_THREE_STEPS)
  public static void sprStep3 () {
    int width16 = 16 + XEiJ.pnlScreenWidth;
    if (!sprActive ||
        (sprReg4BgCtrlCurr & 512) == 0) {  //スプライト画面が表示されていない
      Arrays.fill (sprBuffer, 16, width16, 0);
    } else {  //スプライト画面が表示されている
      int mask = (15 << 24 |  //スプライト(プライオリティ3)
                  15 << 16 |  //スプライト(プライオリティ2)
                  15 << 8 |  //スプライト(プライオリティ1)
                  15 << 20 & -(sprReg4BgCtrlCurr & 1) |  //BG0。スプライト(プライオリティ3)とスプライト(プライオリティ2)の間
                  15 << 12 & -(sprReg4BgCtrlCurr & 8));  //BG1。スプライト(プライオリティ2)とスプライト(プライオリティ1)の間
      for (int x = 16; x < width16; x++) {  //X座標。画面左端は16
        int l = sprBuffer[x];  //4bitパレット
        int h = sprBuffer[1056 + x];  //パレットブロック
        if ((l &= mask) != 0) {  //4bitパレットが0でないプレーンがある
          int i = Integer.numberOfLeadingZeros (l) & -4;  //一番手前にあるものを選ぶ
          sprBuffer[x] = h << i >>> 28 << 4 | l << i >>> 28;  //パレットブロックと4bitパレットを合わせて8bitパレットを作る
        } else if ((h &= mask & (15 << 20 | 15 << 12)) != 0) {  //パレットブロックが0でないバックグラウンドプレーンがある
          int i = Integer.numberOfTrailingZeros (h) & -4;  //一番奥にあるものを選ぶ
          sprBuffer[x] = (h >> i & 15) << 4 | l >> i & 15;  //パレットブロックと4bitパレットを合わせて8bitパレットを作る
        } else {  //4bitパレットとパレットブロックがすべて0
          sprBuffer[x] = 0;
        }
      }  //for x
    }
  }  //sprStep3()


}  //class SpriteScreen



