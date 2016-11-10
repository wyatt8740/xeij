//========================================================================================
//  FontPage.java
//    en:Font page
//    ja:フォントページ
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.font.*;  //FontRenderContext,LineMetrics,TextLayout
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class FontPage {

  //色
  //  1dotあたり2bit使用する
  //  bit0は背景の市松模様
  //  bit1は文字の有無
  private static final int FNP_COLOR_0 = 0x00;  //背景=黒,文字=なし
  private static final int FNP_COLOR_1 = 0x33;  //背景=灰,文字=なし
  private static final int FNP_COLOR_2 = 0xff;  //背景=黒,文字=あり
  private static final int FNP_COLOR_3 = 0xff;  //背景=灰,文字=あり
  private static final byte[] FNP_COLOR_BASE = new byte[] {
    (byte) FNP_COLOR_0,
    (byte) FNP_COLOR_1,
    (byte) FNP_COLOR_2,
    (byte) FNP_COLOR_3,
  };
  private static final Color[] FNP_COLOR_ARRAY = new Color[] {
    new Color (FNP_COLOR_0, FNP_COLOR_0, FNP_COLOR_0),
    new Color (FNP_COLOR_1, FNP_COLOR_1, FNP_COLOR_1),
    new Color (FNP_COLOR_2, FNP_COLOR_2, FNP_COLOR_2),
    new Color (FNP_COLOR_3, FNP_COLOR_3, FNP_COLOR_3),
  };
  //4bitのフォントデータ→4dotのパレットコード
  private static final byte[][] FNP_PALET = {
    //背景=黒黒黒黒
    {
      0b00_00_00_00,
      0b00_00_00_10,
      0b00_00_10_00,
      0b00_00_10_10,
      0b00_10_00_00,
      0b00_10_00_10,
      0b00_10_10_00,
      0b00_10_10_10,
      (byte) 0b10_00_00_00,
      (byte) 0b10_00_00_10,
      (byte) 0b10_00_10_00,
      (byte) 0b10_00_10_10,
      (byte) 0b10_10_00_00,
      (byte) 0b10_10_00_10,
      (byte) 0b10_10_10_00,
      (byte) 0b10_10_10_10,
    },
    //背景=灰灰灰灰
    {
      0b01_01_01_01,
      0b01_01_01_11,
      0b01_01_11_01,
      0b01_01_11_11,
      0b01_11_01_01,
      0b01_11_01_11,
      0b01_11_11_01,
      0b01_11_11_11,
      (byte) 0b11_01_01_01,
      (byte) 0b11_01_01_11,
      (byte) 0b11_01_11_01,
      (byte) 0b11_01_11_11,
      (byte) 0b11_11_01_01,
      (byte) 0b11_11_01_11,
      (byte) 0b11_11_11_01,
      (byte) 0b11_11_11_11,
    },
    //背景=黒黒灰灰
    {
      0b00_00_01_01,
      0b00_00_01_11,
      0b00_00_11_01,
      0b00_00_11_11,
      0b00_10_01_01,
      0b00_10_01_11,
      0b00_10_11_01,
      0b00_10_11_11,
      (byte) 0b10_00_01_01,
      (byte) 0b10_00_01_11,
      (byte) 0b10_00_11_01,
      (byte) 0b10_00_11_11,
      (byte) 0b10_10_01_01,
      (byte) 0b10_10_01_11,
      (byte) 0b10_10_11_01,
      (byte) 0b10_10_11_11,
    },
    //背景=灰灰黒黒
    {
      0b01_01_00_00,
      0b01_01_00_10,
      0b01_01_10_00,
      0b01_01_10_10,
      0b01_11_00_00,
      0b01_11_00_10,
      0b01_11_10_00,
      0b01_11_10_10,
      (byte) 0b11_01_00_00,
      (byte) 0b11_01_00_10,
      (byte) 0b11_01_10_00,
      (byte) 0b11_01_10_10,
      (byte) 0b11_11_00_00,
      (byte) 0b11_11_00_10,
      (byte) 0b11_11_10_00,
      (byte) 0b11_11_10_10,
    },
  };
  //4dotのパレットコード→4bitのフォントデータ
  private static final byte[] FNP_INV_PALET = new byte[256];
  static {
    for (int i = 0; i < 256; i++) {
      FNP_INV_PALET[i] = (byte) (i >> 4 & 8 | i >> 3 & 4 | i >> 2 & 2 | i >> 1 & 1);  //0bP.Q.R.S. → 0b0000PQRS
    }
  }

  //インスタンスフィールド
  public int fnpMask;  //マスク
  public byte[] fnpDataMemory;  //フォントデータのメモリ
  public int fnpDataAddress;  //フォントデータのアドレス
  public int fnpFontWidth;  //フォントの幅
  public int fnpFontHeight;  //フォントの高さ
  public int fnpType;  //種類。FNT_TYPE_ANKまたはFNT_TYPE_KNJ
  public String fnpEn;  //ページの名前(英語)
  public String fnpJa;  //ページの名前(日本語)
  public String fnpImageName;  //イメージファイル名
  public String fnpFontName;  //自動生成に使用したホストのフォント名
  public int fnpCols;  //横方向の文字数
  public int fnpRows;  //縦方向の文字数
  public int fnpImageWidth;  //イメージの幅
  public int fnpImageHeight;  //イメージの高さ
  public int fnpImageOffset;  //イメージの1ラスタのバイト数
  public int fnpDataOffset;  //フォントデータの1ラスタのバイト数
  public BufferedImage fnpImage;  //イメージ
  public byte[] fnpBitmap;  //ビットマップ

  //コンストラクタ
  public FontPage (int mask, byte[] dataMemory, int dataAddress, int fontWidth, int fontHeight, int type, String en, String ja, String imageName) {
    fnpMask = mask;
    fnpDataMemory = dataMemory;
    fnpDataAddress = dataAddress;
    fnpFontWidth = fontWidth;
    fnpFontHeight = fontHeight;
    fnpType = type;
    fnpEn = en;
    fnpJa = ja;
    fnpImageName = imageName;
    fnpFontName = null;
    fnpCols = type == XEiJ.FNT_TYPE_ANK ? 16 : 94;
    fnpRows = type == XEiJ.FNT_TYPE_ANK ? 16 : 77;
    fnpImageWidth = fontWidth * fnpCols;
    fnpImageHeight = fontHeight * fnpRows;
    fnpImageOffset = fnpImageWidth + 3 >> 2;
    fnpDataOffset = fontWidth + 7 >> 3;
    fnpImage = null;
    fnpBitmap = null;
  }

  public BufferedImage fnpGetImage () {
    if (fnpImage == null) {
      fnpMakeImage ();
      fnpMemoryToImage ();
    }
    return fnpImage;
  }

  private void fnpMakeImage () {
    IndexColorModel m = new IndexColorModel (2, 4, FNP_COLOR_BASE, FNP_COLOR_BASE, FNP_COLOR_BASE);
    fnpImage = new BufferedImage (fnpImageWidth, fnpImageHeight, BufferedImage.TYPE_BYTE_BINARY, m);
    fnpBitmap = ((DataBufferByte) fnpImage.getRaster ().getDataBuffer ()).getData ();
  }

  //fnpMemoryToImage ()
  //  メモリからイメージへコピーする
  public void fnpMemoryToImage () {
    byte[] bitmap = fnpBitmap;
    if (bitmap == null) {  //イメージがないときは何もしない
      return;
    }
    byte[] m = fnpDataMemory;
    int a = fnpDataAddress;
    //ROM1.0～ROM1.2のときはIPLROMにあるANK 6x12フォントをCGROMにコピーする
    if (a == XEiJ.FNT_ADDRESS_ANK6X12 && XEiJ.romANK6X12 != 0) {
      System.arraycopy (m, XEiJ.romANK6X12, m, XEiJ.FNT_ADDRESS_ANK6X12, 1 * 12 * 254);
    }
    int o = fnpImageOffset;
    int h = fnpFontHeight;
    switch (fnpFontWidth) {
    case 6:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col += 2) {  //2文字ずつ変換する
          //                                          偶数行   奇数行
          byte[] palet0 = FNP_PALET[    row & 1];  //黒黒黒黒 灰灰灰灰
          byte[] palet1 = FNP_PALET[2 | row & 1];  //黒黒灰灰 灰灰黒黒
          byte[] palet2 = FNP_PALET[   ~row & 1];  //灰灰灰灰 黒黒黒黒
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);
            //    m[a]    m[a+h]
            //  ABCDEF00 GHIJKL00
            //          t
            //  0000ABCD EFGHIJKL
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            int t = (m[a] & 253) << 4 | (m[a + h] & 253) >> 2;
            bitmap[i    ] = palet0[t >>  8     ];  //0000ABCD → A.B.C.D.
            bitmap[i + 1] = palet1[t >>  4 & 15];  //0000EFGH → E.F.G.H.
            bitmap[i + 2] = palet2[t       & 15];  //0000IJKL → I.J.K.L.
            a++;
          }
          a += h;
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 * col >> 2);
            //    m[a]
            //  ABCDEFGH
            //     t
            //  ABCDEFGH
            //    b[i]    b[i+1]
            //  A.B.C.D. E.F.G.H.
            int t = m[a] & 255;
            bitmap[i    ] = palet[t >>  4     ];
            bitmap[i + 1] = palet[t       & 15];
            a++;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 * col >> 2);
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKL0000
            //          t
            //  ABCDEFGH IJKL0000
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            int t = (char) (m[a] << 8 | m[a + 1] & 255);
            bitmap[i    ] = palet[t >> 12     ];
            bitmap[i + 1] = palet[t >>  8 & 15];
            bitmap[i + 2] = palet[t >>  4 & 15];
            a += 2;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 * col >> 2);
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKLMNOP
            //          t
            //  ABCDEFGH IJKLMNOP
            //    b[i]    b[i+1]   b[i+2]   b[i+3]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P.
            int t = (char) (m[a] << 8 | m[a + 1] & 255);
            bitmap[i    ] = palet[t >> 12     ];
            bitmap[i + 1] = palet[t >>  8 & 15];
            bitmap[i + 2] = palet[t >>  4 & 15];
            bitmap[i + 3] = palet[t       & 15];
            a += 2;
          }
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 * col >> 2);
            //    m[a]    m[a+1]   m[a+2]
            //  ABCDEFGH IJKLMNOP RSTUVWX
            //              t
            //  ABCDEFGH IJKLMNOP RSTUVWX
            //    b[i]    b[i+1]   b[i+2]   b[i+3]   b[i+4]   b[i+5]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P. Q.R.S.T. U.V.W.X.
            int t = (char) (m[a] << 8 | m[a + 1] & 255) << 8 | m[a + 2] & 255;
            bitmap[i    ] = palet[t >> 20     ];
            bitmap[i + 1] = palet[t >> 16 & 15];
            bitmap[i + 2] = palet[t >> 12 & 15];
            bitmap[i + 3] = palet[t >>  8 & 15];
            bitmap[i + 4] = palet[t >>  4 & 15];
            bitmap[i + 5] = palet[t       & 15];
            a += 3;
          }
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 * col >> 2);
            int t = m[a] << 24 | (m[a + 1] & 255) << 16 | (char) (m[a + 2] << 8 | m[a + 3] & 255);
            bitmap[i    ] = palet[t >>> 28     ];
            bitmap[i + 1] = palet[t >>> 24 & 15];
            bitmap[i + 2] = palet[t >>> 20 & 15];
            bitmap[i + 3] = palet[t >>> 16 & 15];
            bitmap[i + 4] = palet[t >>> 12 & 15];
            bitmap[i + 5] = palet[t >>>  8 & 15];
            bitmap[i + 6] = palet[t >>>  4 & 15];
            bitmap[i + 7] = palet[t        & 15];
            a += 4;
          }
        }
      }
      break;
    }
  }

  //fnpImageToMemory ()
  //  イメージからメモリへコピーする
  private void fnpImageToMemory () {
    byte[] bitmap = fnpBitmap;
    if (bitmap == null) {  //イメージがないときは何もしない
      return;
    }
    byte[] m = fnpDataMemory;
    int a = fnpDataAddress;
    int o = fnpImageOffset;
    int h = fnpFontHeight;
    switch (fnpFontWidth) {
    case 6:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col += 2) {  //2文字ずつ変換する
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //              t
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //    m[a]    m[a+h]
            //  ABCDEF00 GHIJKL00
            int t = (char) (bitmap[i] << 8 | bitmap[i + 1] & 255) << 8 | bitmap[i + 2] & 255;
            m[a    ] = (byte) (FNP_INV_PALET[t >> 16      ] << 4 |  //0000ABCD → ABCD0000
                               FNP_INV_PALET[t >>  8 & 240]);       //E.F.0000 → 0000EF00
            m[a + h] = (byte) (FNP_INV_PALET[t >>  4 & 255] << 4 |  //G.H.I.J. → GHIJ0000
                               FNP_INV_PALET[t <<  4 & 240]);       //K.L.0000 → 0000KL00
            a++;
          }
          a += h;
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 >> 2) * col;
            //    b[i]    b[i+1]
            //  A.B.C.D. E.F.G.H.
            //    m[a]
            //  ABCDEFGH
            m[a] = (byte) (FNP_INV_PALET[bitmap[i] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            a++;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKL0000
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4);
            a += 2;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]   b[i+3]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P.
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKLMNOP
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            a += 2;
          }
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]   b[i+3]   b[i+4]   b[i+5]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P. Q.R.S.T. U.V.W.X.
            //    m[a]    m[a+1]   m[a+2]
            //  ABCDEFGH IJKLMNOP RSTUVWX
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            a += 3;
          }
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 >> 2) * col;
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            m[a + 3] = (byte) (FNP_INV_PALET[bitmap[i + 6] & 255] << 4 | FNP_INV_PALET[bitmap[i + 7] & 255]);
            a += 4;
          }
        }
      }
      break;
    }
    //ROM1.0～ROM1.2のときはCGROMにあるANK 6x12フォントをIPLROMにコピーする
    if (fnpDataAddress == XEiJ.FNT_ADDRESS_ANK6X12 && XEiJ.romANK6X12 != 0) {
      System.arraycopy (m, XEiJ.FNT_ADDRESS_ANK6X12, m, XEiJ.romANK6X12, 1 * 12 * 254);
    }
  }

  //fnpGenerate (fontName)
  //  ホストのフォントを使ってCGROMを作る
  public void fnpGenerate (String fontName) {
    fnpFontName = fontName;
    Font font = new Font (fontName, Font.PLAIN, fnpFontHeight);
    Graphics2D g2 = (Graphics2D) fnpImage.getGraphics ();
    FontRenderContext frc = g2.getFontRenderContext ();
    //背景を黒で塗り潰す
    byte[] bitmap = fnpBitmap;
    int o = fnpImageOffset;
    int h = fnpFontHeight;
    Arrays.fill (bitmap, 0, o * fnpImageHeight, (byte) 0);
    //白い文字を描く
    g2.setColor (FNP_COLOR_ARRAY[2]);
    g2.setFont (font);
    AffineTransform savedTransform = g2.getTransform ();
    boolean transformed = false;
    double fw = (double) fnpFontWidth;
    double fh = (double) fnpFontHeight;
    //文字コードセット
    //  ANKは正方形のときは全角フォントを優先し、縦長のときは半角フォントを優先する
    String[][] bases = (fnpType == XEiJ.FNT_TYPE_ANK ?
                        fnpFontWidth == fnpFontHeight ?
                        new String[][] { XEiJ.ROM_ANK_FULLWIDTH_BASE, XEiJ.ROM_ANK_HALFWIDTH_BASE } :  //1/4角ANKは全角フォントを優先
                        new String[][] { XEiJ.ROM_ANK_HALFWIDTH_BASE, XEiJ.ROM_ANK_FULLWIDTH_BASE } :  //半角ANKは半角フォントを優先
                        new String[][] { CharacterCode.CHR_KANJI_BASE });  //JIS
    for (int row = 0; row < fnpRows; row++) {
      for (int col = 0; col < fnpCols; col++) {
        for (String[] base : bases) {
          char c = base[row].charAt (col);
          if (c != ' ' && c != '　' && font.canDisplay (c)) {
            String s = String.valueOf (c);  //描く文字
            Rectangle2D r = font.getStringBounds (s, frc);  //文字のレクタングル
            double rx = r.getX ();
            double ry = r.getY ();
            double rw = r.getWidth ();
            double rh = r.getHeight ();
            //文字のレクタングルのサイズを偶数に切り上げる
            //  ANK 12x24のときArialのWの幅は23.0、Xの幅は15.0だが、そのまま縮小すると右側がはみ出してしまう
            //  Arialのサイズを調整したいが等幅フォントに影響が出るのは困るので、レクタングルのサイズを偶数に切り上げることにする
            //  偶数にすることでセンタリング位置の(fw-rw)/2,(fh-rh)/2が整数になり、描画座標の端数が常に-rx,-ryと一致する
            //  Arial Italicやメイリオ イタリックなどはこれでもはみ出す
            rw = Math.ceil (rw * 0.5) * 2.0;
            rh = Math.ceil (rh * 0.5) * 2.0;
            if (rw <= fw && rh <= fh) {  //枠に入り切るとき
              if (transformed) {
                g2.setTransform (savedTransform);  //1x1倍に戻す
                transformed = false;
              }
              //枠の中央に描く
              g2.drawString (s,
                             (float) (fw * col + (fw - rw) * 0.5 - rx),
                             (float) (fh * row + (fh - rh) * 0.5 - ry));
            } else {  //枠に入り切らないとき
              if (transformed) {
                g2.setTransform (savedTransform);  //1x1倍に戻す。戻さないとスケーリングが連結されてしまう
              } else {
                transformed = true;
              }
              //枠を拡大する
              double cw = Math.max (fw, rw);
              double ch = Math.max (fh, rh);
              //枠の中央に描いて縮小する
              g2.scale (fw / cw, fh / ch);  //スケーリング
              g2.drawString (s,
                             (float) (cw * col + (cw - rw) * 0.5 - rx),
                             (float) (ch * row + (ch - rh) * 0.5 - ry));
            }
            break;
          }
        }  //for base
      }  //for col
    }  //for row
    if (transformed) {
      g2.setTransform (savedTransform);
    }
    //背景を市松模様に塗る
    //  フォントが枠からはみ出していないか確認できる
    switch (fnpFontWidth) {
    case 6:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col += 2) {  //2文字ずつ変換する
          //                                       偶数行   奇数行
          byte p0 = FNP_PALET[    row & 1][0];  //黒黒黒黒 灰灰灰灰
          byte p1 = FNP_PALET[2 | row & 1][0];  //黒黒灰灰 灰灰黒黒
          byte p2 = FNP_PALET[   ~row & 1][0];  //灰灰灰灰 黒黒黒黒
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);
            bitmap[i    ] |= p0;
            bitmap[i + 1] |= p1;
            bitmap[i + 2] |= p2;
          }
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 * col >> 2);
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 * col >> 2);
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 * col >> 2);
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
          }
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 * col >> 2);
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
            bitmap[i + 4] |= p;
            bitmap[i + 5] |= p;
          }
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpRows; row++) {
        for (int col = 0; col < fnpCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 * col >> 2);
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
            bitmap[i + 4] |= p;
            bitmap[i + 5] |= p;
            bitmap[i + 6] |= p;
            bitmap[i + 7] |= p;
          }
        }
      }
      break;
    }
    fnpImageToMemory ();
  }

  //fnpInputImage ()
  //  イメージファイルを読み込む
  //  画像ファイルをROMのデータに変換するだけでここでは表示および出力用のイメージは作らない
  //  画像ファイルのフォーマットの違いを吸収する必要がある
  //  初期化のときにも使う

  //fnpEditPixel (x, y, mode)
  //  イメージの指定されたピクセルを編集する。CGROMも更新する
  public void fnpEditPixel (int x, int y, int mode) {
    if (XEiJ.fntEditX == x && XEiJ.fntEditY == y) {
      return;
    }
    XEiJ.fntEditX = x;
    XEiJ.fntEditY = y;
    if (x < 0 || fnpImageWidth <= x ||
        y < 0 || fnpImageHeight <= y) {
      return;
    }
    int i = fnpImageOffset * y + (x >> 2);  //ビットマップのインデックス
    int m = 2 << ((~x & 3) << 1);  //ビットマップのマスク
    int col = x / fnpFontWidth;  //桁
    int row = y / fnpFontHeight;  //行
    x -= fnpFontWidth * col;  //フォント内のx座標
    y -= fnpFontHeight * row;  //フォント内のy座標
    int a = fnpDataAddress + fnpDataOffset * fnpFontHeight * (fnpCols * row + col) + fnpDataOffset * y + (x >> 3);  //フォントデータのアドレス
    int d = 1 << (~x & 7);  //フォントデータのマスク
    if (mode == XEiJ.FNT_MODE_PENCIL) {
      fnpBitmap[i] |= m;  //セット
      fnpDataMemory[a] |= d;  //セット
    } else if (mode == XEiJ.FNT_MODE_ERASER) {
      fnpBitmap[i] &= ~m;  //クリア
      fnpDataMemory[a] &= ~d;  //クリア
    } else {
      fnpBitmap[i] ^= m;  //反転
      fnpDataMemory[a] ^= d;  //反転
    }
    XEiJ.fntCanvas.repaint ();
  }

  //string = fnpStatusText (x, y)
  //  イメージの指定されたピクセルの情報を文字列で返す
  public String fnpStatusText (int x, int y) {
    if (x < 0 || fnpImageWidth <= x || y < 0 || fnpImageHeight <= y) {
      return "";
    }
    StringBuilder sb = new StringBuilder ();
    int col = x / fnpFontWidth;  //桁
    int row = y / fnpFontHeight;  //行
    x -= fnpFontWidth * col;  //フォント内のx座標
    y -= fnpFontHeight * row;  //フォント内のy座標
    int a = fnpDataAddress + fnpDataOffset * fnpFontHeight * (fnpCols * row + col) + fnpDataOffset * y + (x >> 3);  //フォントデータのアドレス
    int b = ~x & 7;  //フォントデータのビット位置
    XEiJ.fmtHex8 (sb.append (' '), a).append (':').append (b).append (' ').append (fnpDataMemory[a] >> b & 1);
    return sb.toString ();
  }

}  //class FontPage



