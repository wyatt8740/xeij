//========================================================================================
//  MemoryMappedDevice.java
//    en:Memory mapped device
//    ja:メモリマップトデバイス
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  デバイスにアクセスするためのメソッドをenum bodyに記述する
//    mmdPbs,mmdPbz,mmdPws,mmdPwz,mmdPls  ピーク
//    mmdRbs,mmdRbz,mmdRws,mmdRwz,mmdRls  リード
//    mmdWb,mmdWw,mmdWl                   ライト
//  ピーク、リード、ライトの命名規則
//    4文字目  P=ピーク,R=リード,W=ライト
//    5文字目  b=バイト,w=ワード,l=ロング
//    6文字目  s=符号拡張,z=ゼロ拡張
//  ピークとリードの返却値の型はmmdPbsとmmdRbsだけbyte、他はint
//  ピークはSRAMスイッチの読み取りやデバッガなどで使用する
//  ピークはMPUやデバイスの状態を変化させず、例外もスローしない
//  リードとライトはMPUやDMAによる通常のアクセスで使用する
//  リードとライトはバスエラーをスローする場合がある
//  アドレスの未使用ビットはデバイスに渡る前にすべてクリアされていなければならない
//    バスエラーは未使用ビットがクリアされたアドレスで通知されることになる
//  異なるデバイスに跨るアクセスはデバイスに渡る前に分割されていなければならない
//  奇数アドレスに対するワードアクセスはデバイスに渡る前に分割または排除されていなければならない
//  4の倍数でないアドレスに対するロングアクセスはデバイスに渡る前に分割または排除されていなければならない
//  デバイスのメソッドを直接呼び出すときはアドレスのマスクや分割を忘れないこと
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public enum MemoryMappedDevice {

  //--------------------------------------------------------------------------------
  //MMD_MMR メインメモリ
  MMD_MMR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "メインメモリ" : "Main Memory";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putShort (a, (short) d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 1] = (byte)  d;
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putInt (a, d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 24);
         MainMemory.mmrM8[a + 1] = (byte) (d >> 16);
         MainMemory.mmrM8[a + 2] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 3] = (byte)  d;
       }
     }
  },  //MMD_MMR

  //--------------------------------------------------------------------------------
  //MMD_XMM 拡張メモリ
  MMD_XMM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張メモリ" : "Expansion Memory";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a];
    }
    @Override protected int mmdPbz (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 8 | XEiJ.busExMemoryArray[a + 1] & 255;
    }
    @Override protected int mmdPwz (int a) {
      a -= XEiJ.busExMemoryStart;
      return (char) (XEiJ.busExMemoryArray[a] << 8 | XEiJ.busExMemoryArray[a + 1] & 255);
    }
    @Override protected int mmdPls (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 255) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | XEiJ.busExMemoryArray[a + 3] & 255);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 8 | XEiJ.busExMemoryArray[a + 1] & 255;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return (char) (XEiJ.busExMemoryArray[a] << 8 | XEiJ.busExMemoryArray[a + 1] & 255);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 255) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | XEiJ.busExMemoryArray[a + 3] & 255);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 1] = (byte)  d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 24);
       XEiJ.busExMemoryArray[a + 1] = (byte) (d >> 16);
       XEiJ.busExMemoryArray[a + 2] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 3] = (byte)  d;
     }
  },  //MMD_XMM

  //--------------------------------------------------------------------------------
  //MMD_GE0 グラフィックス画面(512ドット16色ページ0)
  //
  //  512ドット16色
  //                アドレス             アクセス                  格納
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  ............3210
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  ............7654
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  ............ba98
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  ............fedc
  //
  MMD_GE0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 0)" : "Graphics Screen (512 dots 16 colors page 0)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a] = (byte) (d          & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 3] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GE0

  //--------------------------------------------------------------------------------
  //MMD_GE1 グラフィックス画面(512ドット16色ページ1)
  //
  //  512ドット16色
  //                アドレス             アクセス                  格納
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  ............3210
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  ............7654
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  ............ba98
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  ............fedc
  //
  MMD_GE1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 1)" : "Graphics Screen (512 dots 16 colors page 1)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
      a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
      a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
      a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
      a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a] = (byte) (d          & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 3] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GE1

  //--------------------------------------------------------------------------------
  //MMD_GE2 グラフィックス画面(512ドット16色ページ2)
  //
  //  512ドット16色
  //                アドレス             アクセス                  格納
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  ............3210
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  ............7654
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  ............ba98
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  ............fedc
  //
  MMD_GE2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 2)" : "Graphics Screen (512 dots 16 colors page 2)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a] = (byte) (d          & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 3] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GE2

  //--------------------------------------------------------------------------------
  //MMD_GE3 グラフィックス画面(512ドット16色ページ3)
  //
  //  512ドット16色
  //                アドレス             アクセス                  格納
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  ............3210
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  ............7654
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  ............ba98
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  ............fedc
  //
  MMD_GE3 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 3)" : "Graphics Screen (512 dots 16 colors page 3)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 3];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a] = (byte) (d          & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 1] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 3] = (byte) (d          & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GE3

  //--------------------------------------------------------------------------------
  //MMD_GF0 グラフィックス画面(512ドット256色ページ0)
  //
  //  512ドット256色
  //                アドレス             アクセス                  格納
  //    GF0  0x00c00000～0x00c7ffff  ........76543210  ─┬  ............3210
  //    GF1  0x00c80000～0x00cfffff  ........fedcba98  ┐└  ............7654
  //         0x00d00000～0x00d7ffff                    ├─  ............ba98
  //         0x00d80000～0x00dfffff                    └─  ............fedc
  //
  MMD_GF0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 256 色 ページ 0)" : "Graphics Screen (512 dots 256 colors page 0)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (byte) (MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a]);
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00080001] << 20 | MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 0x00080003] << 4 | MainMemory.mmrM8[a + 3];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (byte) (MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00080001] << 20 | MainMemory.mmrM8[a + 1] << 16 | MainMemory.mmrM8[a + 0x00080003] << 4 | MainMemory.mmrM8[a + 3];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a + 0x00080000] = (byte) (d << 24  >>> 28);
         MainMemory.mmrM8[a             ] = (byte) (d          & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00000001] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d <<  8  >>> 28);
       MainMemory.mmrM8[a + 0x00000001] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 0x00080003] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00000003] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GF0

  //--------------------------------------------------------------------------------
  //MMD_GF1 グラフィックス画面(512ドット256色ページ1)
  //
  //  512ドット256色
  //                アドレス             アクセス                  格納
  //    GF0  0x00c00000～0x00c7ffff  ........76543210  ─┬  ............3210
  //    GF1  0x00c80000～0x00cfffff  ........fedcba98  ┐└  ............7654
  //         0x00d00000～0x00d7ffff                    ├─  ............ba98
  //         0x00d80000～0x00dfffff                    └─  ............fedc
  //
  MMD_GF1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 256 色 ページ 1)" : "Graphics Screen (512 dots 256 colors page 1)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (byte) (MainMemory.mmrM8[a + 0x00100000] << 4 | MainMemory.mmrM8[a + 0x00080000]);
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00100000] << 4 | MainMemory.mmrM8[a + 0x00080000];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00100001] << 4 | MainMemory.mmrM8[a + 0x00080001];
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00100001] << 4 | MainMemory.mmrM8[a + 0x00080001];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00100001] << 20 | MainMemory.mmrM8[a + 0x00080001] << 16 | MainMemory.mmrM8[a + 0x00100003] << 4 | MainMemory.mmrM8[a + 0x00080003];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (byte) (MainMemory.mmrM8[a + 0x00100000] << 4 | MainMemory.mmrM8[a + 0x00080000]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00100000] << 4 | MainMemory.mmrM8[a + 0x00080000];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00100001] << 4 | MainMemory.mmrM8[a + 0x00080001];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00100001] << 4 | MainMemory.mmrM8[a + 0x00080001];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00100001] << 20 | MainMemory.mmrM8[a + 0x00080001] << 16 | MainMemory.mmrM8[a + 0x00100003] << 4 | MainMemory.mmrM8[a + 0x00080003];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
         MainMemory.mmrM8[a + 0x00100000] = (byte) (d << 24  >>> 28);
         MainMemory.mmrM8[a + 0x00080000] = (byte) (d          & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00100001] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00100001] = (byte) (d <<  8  >>> 28);
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 0x00100003] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00080003] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GF1

  //--------------------------------------------------------------------------------
  //MMD_GG0 グラフィックス画面(512ドット65536色)
  //
  //  512ドット65536色
  //                アドレス             アクセス                  格納
  //    GG0  0x00c00000～0x00c7ffff  fedcba9876543210  ─┬  ............3210
  //         0x00c80000～0x00cfffff                      ├  ............7654
  //         0x00d00000～0x00d7ffff                      ├  ............ba98
  //         0x00d80000～0x00dfffff                      └  ............fedc
  //
  MMD_GG0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 65536 色)" : "Graphics Screen (512 dots 65536 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      a += -(~a & 1) & 0x00100001;
      return (byte) (MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a]);
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      a += -(~a & 1) & 0x00100001;
      return MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (short) (MainMemory.mmrM8[a + 0x00180001] << 12 | MainMemory.mmrM8[a + 0x00100001] << 8 | MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1]);
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a + 0x00180001] << 12 | MainMemory.mmrM8[a + 0x00100001] << 8 | MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (MainMemory.mmrM8[a + 0x00180001] << 28 | MainMemory.mmrM8[a + 0x00100001] << 24 | MainMemory.mmrM8[a + 0x00080001] << 20 | MainMemory.mmrM8[a + 1] << 16 |
              MainMemory.mmrM8[a + 0x00180003] << 12 | MainMemory.mmrM8[a + 0x00100003] << 8 | MainMemory.mmrM8[a + 0x00080003] << 4 | MainMemory.mmrM8[a + 3]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       a += -(~a & 1) & 0x00100001;
       return (byte) (MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       a += -(~a & 1) & 0x00100001;
       return MainMemory.mmrM8[a + 0x00080000] << 4 | MainMemory.mmrM8[a];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (short) (MainMemory.mmrM8[a + 0x00180001] << 12 | MainMemory.mmrM8[a + 0x00100001] << 8 | MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a + 0x00180001] << 12 | MainMemory.mmrM8[a + 0x00100001] << 8 | MainMemory.mmrM8[a + 0x00080001] << 4 | MainMemory.mmrM8[a + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (MainMemory.mmrM8[a + 0x00180001] << 28 | MainMemory.mmrM8[a + 0x00100001] << 24 | MainMemory.mmrM8[a + 0x00080001] << 20 | MainMemory.mmrM8[a + 1] << 16 |
               MainMemory.mmrM8[a + 0x00180003] << 12 | MainMemory.mmrM8[a + 0x00100003] << 8 | MainMemory.mmrM8[a + 0x00080003] << 4 | MainMemory.mmrM8[a + 3]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {
         MainMemory.mmrM8[a + 0x00180001] = (byte) (d << 24  >>> 28);
         MainMemory.mmrM8[a + 0x00100001] = (byte) (d          & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       } else {
         MainMemory.mmrM8[a + 0x00080000] = (byte) (d << 24  >>> 28);
         MainMemory.mmrM8[a             ] = (byte) (d          & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00180001] = (byte) ((char) d >>> 12);
       MainMemory.mmrM8[a + 0x00100001] = (byte) (d << 20  >>> 28);
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00000001] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a + 0x00180001] = (byte) (d        >>> 28);
       MainMemory.mmrM8[a + 0x00100001] = (byte) (d <<  4  >>> 28);
       MainMemory.mmrM8[a + 0x00080001] = (byte) (d <<  8  >>> 28);
       MainMemory.mmrM8[a + 0x00000001] = (byte) (d << 12  >>> 28);
       MainMemory.mmrM8[a + 0x00180003] = (byte) ((char) d >>> 12);
       MainMemory.mmrM8[a + 0x00100003] = (byte) (d << 20  >>> 28);
       MainMemory.mmrM8[a + 0x00080003] = (byte) (d << 24  >>> 28);
       MainMemory.mmrM8[a + 0x00000003] = (byte) (d          & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
  },  //MMD_GG0

  //--------------------------------------------------------------------------------
  //MMD_GH0 グラフィックス画面(1024ドット16色)
  //
  //  参照アドレス
  //    0b00000000_110Yyyyy_yyyyyXxx_xxxxxxx?
  //    +--------------------------+--------------------------+
  //    | 0x00c00000 .. 0x00c003ff | 0x00c00400 .. 0x00c007ff |
  //    | 0x00c00800    0x00c00bff | 0x00c00c00    0x00c00fff |
  //    |      :             :     |      :             :     |
  //    | 0x00cff000    0x00cff3ff | 0x00cff400    0x00cff7ff |
  //    | 0x00cff800 .. 0x00cffbff | 0x00cffc00 .. 0x00cfffff |
  //    +--------------------------+--------------------------+
  //    | 0x00d00000 .. 0x00d003ff | 0x00d00400 .. 0x00d007ff |
  //    | 0x00d00800    0x00d00bff | 0x00d00c00    0x00d00fff |
  //    |      :             :     |      :             :     |
  //    | 0x00dff000    0x00dff3ff | 0x00dff400    0x00dff7ff |
  //    | 0x00dff800 .. 0x00dffbff | 0x00dffc00 .. 0x00dfffff |
  //    +--------------------------+--------------------------+
  //
  //  格納アドレス
  //    0b00000000_110YXyyy_yyyyyyxx_xxxxxxx?
  //    +--------------------------+--------------------------+
  //    | 0x00c00000 .. 0x00c003ff | 0x00c80000 .. 0x00c803ff |
  //    | 0x00c00400    0x00c007ff | 0x00c80400    0x00c807ff |
  //    |      :     G0      :     |      :     G1      :     |
  //    | 0x00c7f800    0x00c7fbff | 0x00cff800    0x00cffbff |
  //    | 0x00c7fc00 .. 0x00c7ffff | 0x00cffc00 .. 0x00cfffff |
  //    +--------------------------+--------------------------+
  //    | 0x00d00000 .. 0x00d003ff | 0x00d80000 .. 0x00d803ff |
  //    | 0x00d00400    0x00d007ff | 0x00d80400    0x00d807ff |
  //    |      :     G2      :     |      :     G3      :     |
  //    | 0x00d7f800    0x00d7fbff | 0x00dff800    0x00dffbff |
  //    | 0x00d7fc00 .. 0x00d7ffff | 0x00dffc00 .. 0x00dfffff |
  //    +--------------------------+--------------------------+
  //
  //    perl -e "@m=(0,1,2,3);sub f{my($a)=@_;my$x=$a>>1&1023;my$y=$a>>11&1023;($a&0x00c00001)+($m[($y>>8&2)+($x>>9&1)]<<19)+(($y&511)<<10)+(($x&511)<<1);}my@a=(0xc00000,0xc003ff,0xc00400,0xc007ff,0xc00800,0xc00bff,0xc00c00,0xc00fff,0xcff000,0xcff3ff,0xcff400,0xcff7ff,0xcff800,0xcffbff,0xcffc00,0xcfffff,0xd00000,0xd003ff,0xd00400,0xd007ff,0xd00800,0xd00bff,0xd00c00,0xd00fff,0xdff000,0xdff3ff,0xdff400,0xdff7ff,0xdff800,0xdffbff,0xdffc00,0xdfffff);for$v(0..7){print'  //    ';for$u(0..3){$a=$a[4*$v+$u];$b=f($a);printf'  0x%08x',$b;}print qq@\n@;}"
  //    perl -e "sub f{my($a)=@_;($a&0x00c00000+(1<<20)+1023)+($a<<9&1<<19)+($a>>1&511<<10);}my@a=(0xc00000,0xc003ff,0xc00400,0xc007ff,0xc00800,0xc00bff,0xc00c00,0xc00fff,0xcff000,0xcff3ff,0xcff400,0xcff7ff,0xcff800,0xcffbff,0xcffc00,0xcfffff,0xd00000,0xd003ff,0xd00400,0xd007ff,0xd00800,0xd00bff,0xd00c00,0xd00fff,0xdff000,0xdff3ff,0xdff400,0xdff7ff,0xdff800,0xdffbff,0xdffc00,0xdfffff);for$v(0..7){print'  //    ';for$u(0..3){$a=$a[4*$v+$u];$b=f($a);printf'  0x%08x',$b;}print qq@\n@;}"
  //      0x00c00000  0x00c003ff  0x00c80000  0x00c803ff
  //      0x00c00400  0x00c007ff  0x00c80400  0x00c807ff
  //      0x00c7f800  0x00c7fbff  0x00cff800  0x00cffbff
  //      0x00c7fc00  0x00c7ffff  0x00cffc00  0x00cfffff
  //      0x00d00000  0x00d003ff  0x00d80000  0x00d803ff
  //      0x00d00400  0x00d007ff  0x00d80400  0x00d807ff
  //      0x00d7f800  0x00d7fbff  0x00dff800  0x00dffbff
  //      0x00d7fc00  0x00d7ffff  0x00dffc00  0x00dfffff
  //
  MMD_GH0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 16 色)" : "Graphics Screen (1024 dots 16 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0];
    }
    @Override protected int mmdPbz (int a) {
      int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0];
    }
    @Override protected int mmdPws (int a) {
      int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0 + 1];
    }
    @Override protected int mmdPwz (int a) {
      int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0 + 1];
    }
    @Override protected int mmdPls (int a) {
      //4bitページを跨ぐ場合があることに注意
      int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      a += 2;
      int b2 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0 + 1] << 16 | MainMemory.mmrM8[b2 + 1];
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0 + 1];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0 + 1];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       //4bitページを跨ぐ場合があることに注意
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0 + 1] << 16 | MainMemory.mmrM8[b2 + 1];
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & 1) != 0) {
         CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
         int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
         MainMemory.mmrM8[b0] = (byte) (d & 15);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0 + 1] = (byte) (d & 15);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       //4bitページを跨ぐ場合があることに注意
       int b0 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = (a & 0x00c00000 + (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0 + 1] = (byte) (d >>> 16 & 15);
       MainMemory.mmrM8[b2 + 1] = (byte) (d & 15);
     }
  },  //MMD_GH0

  //--------------------------------------------------------------------------------
  //MMD_GI0 グラフィックス画面(1024ドット256色)
  //
  //  参照アドレスと格納アドレスはGH0と同じ
  //
  MMD_GI0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 256 色)" : "Graphics Screen (1024 dots 256 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0];
    }
    @Override protected int mmdPbz (int a) {
      int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0] & 255;
    }
    @Override protected int mmdPws (int a) {
      int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0 + 1] & 255;
    }
    @Override protected int mmdPwz (int a) {
      int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0 + 1] & 255;
    }
    @Override protected int mmdPls (int a) {
      //4bitページを跨ぐ場合があることに注意
      int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      a += 2;
      int b2 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return (MainMemory.mmrM8[b0 + 1] & 255) << 16 | MainMemory.mmrM8[b2 + 1] & 255;
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0 + 1] & 255;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0 + 1] & 255;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       //4bitページを跨ぐ場合があることに注意
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return (MainMemory.mmrM8[b0 + 1] & 255) << 16 | MainMemory.mmrM8[b2 + 1] & 255;
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & 1) != 0) {
         CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
         int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
         MainMemory.mmrM8[b0] = (byte) d;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0 + 1] = (byte) d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       //4bitページを跨ぐ場合があることに注意
       int b0 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = XEiJ.BUS_MODIFIED_256_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0 + 1] = (byte) (d >>> 16);
       MainMemory.mmrM8[b2 + 1] = (byte) d;
     }
  },  //MMD_GI0

  //--------------------------------------------------------------------------------
  //MMD_GJ0 グラフィックス画面(1024ドット65536色)
  //
  //  参照アドレスと格納アドレスはGH0と同じ
  //
  MMD_GJ0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 65536 色)" : "Graphics Screen (1024 dots 65536 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0];
    }
    @Override protected int mmdPbz (int a) {
      int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0] & 255;
    }
    @Override protected int mmdPws (int a) {
      int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return MainMemory.mmrM8[b0] << 8 | MainMemory.mmrM8[b0 + 1] & 255;
    }
    @Override protected int mmdPwz (int a) {
      int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return (char) (MainMemory.mmrM8[b0] << 8 | MainMemory.mmrM8[b0 + 1] & 255);
    }
    @Override protected int mmdPls (int a) {
       //4bitページを跨ぐ場合があることに注意
      int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      a += 2;
      int b2 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
      return (MainMemory.mmrM8[b0] << 24 | (MainMemory.mmrM8[b0 + 1] & 255) << 16 |
              (char) (MainMemory.mmrM8[b2] << 8 | MainMemory.mmrM8[b2 + 1] & 255));
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       return MainMemory.mmrM8[XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10)];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       return MainMemory.mmrM8[XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10)] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return MainMemory.mmrM8[b0] << 8 | MainMemory.mmrM8[b0 + 1] & 255;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return (char) (MainMemory.mmrM8[b0] << 8 | MainMemory.mmrM8[b0 + 1] & 255);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       //4bitページを跨ぐ場合があることに注意
       int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       return (MainMemory.mmrM8[b0] << 24 | (MainMemory.mmrM8[b0 + 1] & 255) << 16 |
               (char) (MainMemory.mmrM8[b2] << 8 | MainMemory.mmrM8[b2 + 1] & 255));
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & 1) != 0) {
         CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
         MainMemory.mmrM8[XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10)] = (byte) d;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0] = (byte) (d >> 8);
       MainMemory.mmrM8[b0 + 1] = (byte) d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       //4bitページを跨ぐ場合があることに注意
       int b0 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       a += 2;
       int b2 = XEiJ.BUS_MODIFIED_65536_PAGE + (a & (1 << 20) + 1023) + (a << 9 & 1 << 19) + (a >>> 1 & 511 << 10);
       MainMemory.mmrM8[b0] = (byte) (d >>> 24);
       MainMemory.mmrM8[b0 + 1] = (byte) (d >>> 16);
       MainMemory.mmrM8[b2] = (byte) (d >>> 8);
       MainMemory.mmrM8[b2 + 1] = (byte) d;
     }
  },  //MMD_GJ0

  //--------------------------------------------------------------------------------
  //MMD_TXT テキスト画面
  MMD_TXT {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "テキスト画面" : "Text Screen";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int x;  //マスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           d &= ~(x = CRTC.crtR23Mask >> ((~a & 1) << 3));
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         x = CRTC.crtR23Mask >> ((~a & 1) << 3);
         MainMemory.mmrM8[a] = (byte) (MainMemory.mmrM8[a] & x | d & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         MainMemory.mmrM8[a] = (byte) d;
       }
       CRTC.crtRasterStamp[((a & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1023] = 0;  //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
     }  //mmdWb
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int e;  //上位バイトのデータ
       int x;  //下位バイトのマスク
       int y;  //上位バイトのマスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           e = d >> 8 & ~(y = (x = CRTC.crtR23Mask) >> 8);
           d &= ~x;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & y | e);
             MainMemory.mmrM8[a + 0x00000001] = (byte) (MainMemory.mmrM8[a + 0x00000001] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & y | e);
             MainMemory.mmrM8[a + 0x00020001] = (byte) (MainMemory.mmrM8[a + 0x00020001] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & y | e);
             MainMemory.mmrM8[a + 0x00040001] = (byte) (MainMemory.mmrM8[a + 0x00040001] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & y | e);
             MainMemory.mmrM8[a + 0x00060001] = (byte) (MainMemory.mmrM8[a + 0x00060001] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           e = d >> 8;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) e;
             MainMemory.mmrM8[a + 0x00000001] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) e;
             MainMemory.mmrM8[a + 0x00020001] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) e;
             MainMemory.mmrM8[a + 0x00040001] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) e;
             MainMemory.mmrM8[a + 0x00060001] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         y = (x = CRTC.crtR23Mask) >> 8;
         MainMemory.mmrM8[a    ] = (byte) (MainMemory.mmrM8[a    ] & y | (d >> 8) & ~y);
         MainMemory.mmrM8[a + 1] = (byte) (MainMemory.mmrM8[a + 1] & x |  d       & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         if (MainMemory.MMR_USE_BYTE_BUFFER) {
           MainMemory.mmrBuffer.putShort (a, (short) d);
         } else {
           MainMemory.mmrM8[a    ] = (byte) (d >> 8);
           MainMemory.mmrM8[a + 1] = (byte)  d;
         }
       }
       CRTC.crtRasterStamp[((a & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1023] = 0;  //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
     }  //mmdWw
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int e;  //下位ワードの上位バイトのデータ
       int f;  //上位ワードの下位バイトのデータ
       int g;  //上位ワードの上位バイトのデータ
       int x;  //下位バイトのマスク
       int y;  //上位バイトのマスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           g = d >> 24 & ~(y = (x = CRTC.crtR23Mask) >> 8);
           f = d >> 16 & ~x;
           e = d >>  8 & ~y;
           d &= ~x;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & y | g);
             MainMemory.mmrM8[a + 0x00000001] = (byte) (MainMemory.mmrM8[a + 0x00000001] & x | f);
             MainMemory.mmrM8[a + 0x00000002] = (byte) (MainMemory.mmrM8[a + 0x00000002] & y | e);
             MainMemory.mmrM8[a + 0x00000003] = (byte) (MainMemory.mmrM8[a + 0x00000003] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & y | g);
             MainMemory.mmrM8[a + 0x00020001] = (byte) (MainMemory.mmrM8[a + 0x00020001] & x | f);
             MainMemory.mmrM8[a + 0x00020002] = (byte) (MainMemory.mmrM8[a + 0x00020002] & y | e);
             MainMemory.mmrM8[a + 0x00020003] = (byte) (MainMemory.mmrM8[a + 0x00020003] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & y | g);
             MainMemory.mmrM8[a + 0x00040001] = (byte) (MainMemory.mmrM8[a + 0x00040001] & x | f);
             MainMemory.mmrM8[a + 0x00040002] = (byte) (MainMemory.mmrM8[a + 0x00040002] & y | e);
             MainMemory.mmrM8[a + 0x00040003] = (byte) (MainMemory.mmrM8[a + 0x00040003] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & y | g);
             MainMemory.mmrM8[a + 0x00060001] = (byte) (MainMemory.mmrM8[a + 0x00060001] & x | f);
             MainMemory.mmrM8[a + 0x00060002] = (byte) (MainMemory.mmrM8[a + 0x00060002] & y | e);
             MainMemory.mmrM8[a + 0x00060003] = (byte) (MainMemory.mmrM8[a + 0x00060003] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           g = d >> 24;
           f = d >> 16;
           e = d >>  8;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) g;
             MainMemory.mmrM8[a + 0x00000001] = (byte) f;
             MainMemory.mmrM8[a + 0x00000002] = (byte) e;
             MainMemory.mmrM8[a + 0x00000003] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) g;
             MainMemory.mmrM8[a + 0x00020001] = (byte) f;
             MainMemory.mmrM8[a + 0x00020002] = (byte) e;
             MainMemory.mmrM8[a + 0x00020003] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) g;
             MainMemory.mmrM8[a + 0x00040001] = (byte) f;
             MainMemory.mmrM8[a + 0x00040002] = (byte) e;
             MainMemory.mmrM8[a + 0x00040003] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) g;
             MainMemory.mmrM8[a + 0x00060001] = (byte) f;
             MainMemory.mmrM8[a + 0x00060002] = (byte) e;
             MainMemory.mmrM8[a + 0x00060003] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         y = (x = CRTC.crtR23Mask) >> 8;
         MainMemory.mmrM8[a    ] = (byte) (MainMemory.mmrM8[a    ] & y | (d >> 24) & ~y);
         MainMemory.mmrM8[a + 1] = (byte) (MainMemory.mmrM8[a + 1] & x | (d >> 16) & ~x);
         MainMemory.mmrM8[a + 2] = (byte) (MainMemory.mmrM8[a + 1] & y | (d >>  8) & ~y);
         MainMemory.mmrM8[a + 3] = (byte) (MainMemory.mmrM8[a + 1] & x |  d        & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         if (MainMemory.MMR_USE_BYTE_BUFFER) {
           MainMemory.mmrBuffer.putInt (a, d);
         } else {
           MainMemory.mmrM8[a    ] = (byte) (d >> 24);
           MainMemory.mmrM8[a + 1] = (byte) (d >> 16);
           MainMemory.mmrM8[a + 2] = (byte) (d >>  8);
           MainMemory.mmrM8[a + 3] = (byte)  d;
         }
       }
       CRTC.crtRasterStamp[((a     & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1023] = 0;  //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
       CRTC.crtRasterStamp[((a + 2 & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1023] = 0;  //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
     }  //mmdWl
  },  //MMD_TXT

  //--------------------------------------------------------------------------------
  //MMD_CRT CRTコントローラ
  MMD_CRT {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "CRT コントローラ" : "CRT Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case CRTC.CRT_R00_HFRONT_END:  //R00の上位
       case CRTC.CRT_R01_HSYNC_END:  //R01の上位
       case CRTC.CRT_R02_HBACK_END:  //R02の上位
       case CRTC.CRT_R03_HDISP_END:  //R03の上位
       case CRTC.CRT_R08_ADJUST:  //R08の上位
       case CRTC.CRT_R24:  //R24の上位
       case CRTC.CRT_R24 + 1:  //R24の下位
       case CRTC.CRT_ACTION:  //動作ポートの上位
         return 0;
       case CRTC.CRT_R00_HFRONT_END + 1:  //R00の下位
         return CRTC.crtR00HFrontEndPort;
       case CRTC.CRT_R01_HSYNC_END + 1:  //R01の下位
         return CRTC.crtR01HSyncEndPort;
       case CRTC.CRT_R02_HBACK_END + 1:  //R02の下位
         return CRTC.crtR02HBackEndPort;
       case CRTC.CRT_R03_HDISP_END + 1:  //R03の下位
         return CRTC.crtR03HDispEndPort;
       case CRTC.CRT_R04_VFRONT_END:  //R04の上位
         return CRTC.crtR04VFrontEndPort >> 8;
       case CRTC.CRT_R04_VFRONT_END + 1:  //R04の下位
         return CRTC.crtR04VFrontEndPort & 255;
       case CRTC.CRT_R05_VSYNC_END:  //R05の上位
         return CRTC.crtR05VSyncEndPort >> 8;
       case CRTC.CRT_R05_VSYNC_END + 1:  //R05の下位
         return CRTC.crtR05VSyncEndPort & 255;
       case CRTC.CRT_R06_VBACK_END:  //R06の上位
         return CRTC.crtR06VBackEndPort >> 8;
       case CRTC.CRT_R06_VBACK_END + 1:  //R06の下位
         return CRTC.crtR06VBackEndPort & 255;
       case CRTC.CRT_R07_VDISP_END:  //R07の上位
         return CRTC.crtR07VDispEndPort >> 8;
       case CRTC.CRT_R07_VDISP_END + 1:  //R07の下位
         return CRTC.crtR07VDispEndPort & 255;
       case CRTC.CRT_R08_ADJUST + 1:  //R08の下位
         return CRTC.crtR08Adjust;
       case CRTC.CRT_R09_IRQ_RASTER:  //R09の上位
         return CRTC.crtR09IRQRasterPort >> 8;
       case CRTC.CRT_R09_IRQ_RASTER + 1:  //R09の下位
         return CRTC.crtR09IRQRasterPort & 255;
       case CRTC.CRT_R10_TX_X:  //R10の上位
         return CRTC.crtR10TxXPort >> 8;
       case CRTC.CRT_R10_TX_X + 1:  //R10の下位
         return CRTC.crtR10TxXPort & 255;
       case CRTC.CRT_R11_TX_Y:  //R11の上位
         return CRTC.crtR11TxYCurr >> 8;
       case CRTC.CRT_R11_TX_Y + 1:  //R11の下位
         return CRTC.crtR11TxYCurr & 255;
       case CRTC.CRT_R12_GR_X_0:  //R12の上位
         return CRTC.crtR12GrXPort[0] >> 8;
       case CRTC.CRT_R12_GR_X_0 + 1:  //R12の下位
         return CRTC.crtR12GrXPort[0] & 255;
       case CRTC.CRT_R13_GR_Y_0:  //R13の上位
         return CRTC.crtR13GrYPort[0] >> 8;
       case CRTC.CRT_R13_GR_Y_0 + 1:  //R13の下位
         return CRTC.crtR13GrYPort[0] & 255;
       case CRTC.CRT_R14_GR_X_1:  //R14の上位
         return CRTC.crtR12GrXPort[1] >> 8;
       case CRTC.CRT_R14_GR_X_1 + 1:  //R14の下位
         return CRTC.crtR12GrXPort[1] & 255;
       case CRTC.CRT_R15_GR_Y_1:  //R15の上位
         return CRTC.crtR13GrYPort[1] >> 8;
       case CRTC.CRT_R15_GR_Y_1 + 1:  //R15の下位
         return CRTC.crtR13GrYPort[1] & 255;
       case CRTC.CRT_R16_GR_X_2:  //R16の上位
         return CRTC.crtR12GrXPort[2] >> 8;
       case CRTC.CRT_R16_GR_X_2 + 1:  //R16の下位
         return CRTC.crtR12GrXPort[2] & 255;
       case CRTC.CRT_R17_GR_Y_2:  //R17の上位
         return CRTC.crtR13GrYPort[2] >> 8;
       case CRTC.CRT_R17_GR_Y_2 + 1:  //R17の下位
         return CRTC.crtR13GrYPort[2] & 255;
       case CRTC.CRT_R18_GR_X_3:  //R18の上位
         return CRTC.crtR12GrXPort[3] >> 8;
       case CRTC.CRT_R18_GR_X_3 + 1:  //R18の下位
         return CRTC.crtR12GrXPort[3] & 255;
       case CRTC.CRT_R19_GR_Y_3:  //R19の上位
         return CRTC.crtR13GrYPort[3] >> 8;
       case CRTC.CRT_R19_GR_Y_3 + 1:  //R19の下位
         return CRTC.crtR13GrYPort[3] & 255;
       case CRTC.CRT_R20_MODE:  //R20の上位
         return CRTC.crtMemoryModePort;
       case CRTC.CRT_R20_MODE + 1:  //R20の下位
         return CRTC.crtHighResoPort << 4 | CRTC.crtVResoPort << 2 | CRTC.crtHResoPort;
       case CRTC.CRT_R21_SELECT:  //R21の上位
         return ((CRTC.crtBitMask   ? 0b00000010 : 0) |
                 (CRTC.crtSimAccess ? 0b00000001 : 0));
       case CRTC.CRT_R21_SELECT + 1:  //R21の下位
         return ((CRTC.crtSimPlane3 ? 0b10000000 : 0) |
                 (CRTC.crtSimPlane2 ? 0b01000000 : 0) |
                 (CRTC.crtSimPlane1 ? 0b00100000 : 0) |
                 (CRTC.crtSimPlane0 ? 0b00010000 : 0) |
                 (CRTC.crtCCPlane3  ? 0b00001000 : 0) |
                 (CRTC.crtCCPlane2  ? 0b00000100 : 0) |
                 (CRTC.crtCCPlane1  ? 0b00000010 : 0) |
                 (CRTC.crtCCPlane0  ? 0b00000001 : 0));
       case CRTC.CRT_R22_BLOCK:  //R22の上位
         return CRTC.crtR22SrcBlock;
       case CRTC.CRT_R22_BLOCK + 1:  //R22の下位
         return CRTC.crtR22DstBlock;
       case CRTC.CRT_R23_MASK:  //R23の上位
         return CRTC.crtR23Mask >> 8;
       case CRTC.CRT_R23_MASK + 1:  //R23の下位
         return CRTC.crtR23Mask & 255;
       case CRTC.CRT_ACTION + 1:  //動作ポートの下位
         return ((CRTC.crtRasterCopyOn ? 8 : 0) |  //ラスタコピー
                 (CRTC.crtClearStandby || CRTC.crtClearFrames != 0 ? 2 : 0));  //高速クリア
       case 0x00e8003c:
         return VideoController.vcnMode.ordinal () >> 8;
       case 0x00e8003d:
         return VideoController.vcnMode.ordinal () & 255;
       }
       return super.mmdRbz (a);  //バスエラー
     }  //mmdRbz
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case CRTC.CRT_R00_HFRONT_END:  //R00
         return CRTC.crtR00HFrontEndPort;
       case CRTC.CRT_R01_HSYNC_END:  //R01
         return CRTC.crtR01HSyncEndPort;
       case CRTC.CRT_R02_HBACK_END:  //R02
         return CRTC.crtR02HBackEndPort;
       case CRTC.CRT_R03_HDISP_END:  //R03
         return CRTC.crtR03HDispEndPort;
       case CRTC.CRT_R04_VFRONT_END:  //R04
         return CRTC.crtR04VFrontEndPort;
       case CRTC.CRT_R05_VSYNC_END:  //R05
         return CRTC.crtR05VSyncEndPort;
       case CRTC.CRT_R06_VBACK_END:  //R06
         return CRTC.crtR06VBackEndPort;
       case CRTC.CRT_R07_VDISP_END:  //R07
         return CRTC.crtR07VDispEndPort;
       case CRTC.CRT_R08_ADJUST:  //R08
         return CRTC.crtR08Adjust;
       case CRTC.CRT_R09_IRQ_RASTER:  //R09
         return CRTC.crtR09IRQRasterPort;
       case CRTC.CRT_R10_TX_X:  //R10
         return CRTC.crtR10TxXPort;
       case CRTC.CRT_R11_TX_Y:  //R11
         return CRTC.crtR11TxYPort;
       case CRTC.CRT_R12_GR_X_0:  //R12
         return CRTC.crtR12GrXPort[0];
       case CRTC.CRT_R13_GR_Y_0:  //R13
         return CRTC.crtR13GrYPort[0];
       case CRTC.CRT_R14_GR_X_1:  //R14
         return CRTC.crtR12GrXPort[1];
       case CRTC.CRT_R15_GR_Y_1:  //R15
         return CRTC.crtR13GrYPort[1];
       case CRTC.CRT_R16_GR_X_2:  //R16
         return CRTC.crtR12GrXPort[2];
       case CRTC.CRT_R17_GR_Y_2:  //R17
         return CRTC.crtR13GrYPort[2];
       case CRTC.CRT_R18_GR_X_3:  //R18
         return CRTC.crtR12GrXPort[3];
       case CRTC.CRT_R19_GR_Y_3:  //R19
         return CRTC.crtR13GrYPort[3];
       case CRTC.CRT_R20_MODE:  //R20
         return CRTC.crtMemoryModePort << 8 | CRTC.crtHighResoPort << 4 | CRTC.crtVResoPort << 2 | CRTC.crtHResoPort;
       case CRTC.CRT_R21_SELECT:  //R21
         return ((CRTC.crtBitMask   ? 0b00000010_00000000 : 0) |
                 (CRTC.crtSimAccess ? 0b00000001_00000000 : 0) |
                 (CRTC.crtSimPlane3 ? 0b00000000_10000000 : 0) |
                 (CRTC.crtSimPlane2 ? 0b00000000_01000000 : 0) |
                 (CRTC.crtSimPlane1 ? 0b00000000_00100000 : 0) |
                 (CRTC.crtSimPlane0 ? 0b00000000_00010000 : 0) |
                 (CRTC.crtCCPlane3  ? 0b00000000_00001000 : 0) |
                 (CRTC.crtCCPlane2  ? 0b00000000_00000100 : 0) |
                 (CRTC.crtCCPlane1  ? 0b00000000_00000010 : 0) |
                 (CRTC.crtCCPlane0  ? 0b00000000_00000001 : 0));
       case CRTC.CRT_R22_BLOCK:  //R22
         return CRTC.crtR22SrcBlock << 8 | CRTC.crtR22DstBlock;
       case CRTC.CRT_R23_MASK:  //R23
         return CRTC.crtR23Mask;
       case CRTC.CRT_R24:  //R24
         return 0;
       case CRTC.CRT_ACTION:  //動作ポート
         return ((CRTC.crtRasterCopyOn ? 8 : 0) |  //ラスタコピー
                 (CRTC.crtClearStandby || CRTC.crtClearFrames != 0 ? 2 : 0));  //高速クリア
       }
       return super.mmdRwz (a);  //バスエラー
     }  //mmdRwz
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case CRTC.CRT_R00_HFRONT_END:  //R00の上位
         return;
       case CRTC.CRT_R00_HFRONT_END + 1:  //R00の下位
         CRTC.crtR00HFrontEndPort = d & 255;
         {
           int curr = CRTC.crtR00HFrontEndMask == 0 ? CRTC.crtR00HFrontEndPort : CRTC.crtR00HFrontEndTest;
           if (CRTC.crtR00HFrontEndCurr != curr) {
             CRTC.crtR00HFrontEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R01_HSYNC_END:  //R01の上位
         return;
       case CRTC.CRT_R01_HSYNC_END + 1:  //R01の下位
         CRTC.crtR01HSyncEndPort = d & 255;
         {
           int curr = CRTC.crtR01HSyncEndMask == 0 ? CRTC.crtR01HSyncEndPort : CRTC.crtR01HSyncEndTest;
           if (CRTC.crtR01HSyncEndCurr != curr) {
             CRTC.crtR01HSyncEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R02_HBACK_END:  //R02の上位
         return;
       case CRTC.CRT_R02_HBACK_END + 1:  //R02の下位
         CRTC.crtR02HBackEndPort = d & 255;
         {
           int curr = CRTC.crtR02HBackEndMask == 0 ? CRTC.crtR02HBackEndPort : CRTC.crtR02HBackEndTest;
           if (CRTC.crtR02HBackEndCurr != curr) {
             CRTC.crtR02HBackEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R03_HDISP_END:  //R03の上位
         return;
       case CRTC.CRT_R03_HDISP_END + 1:  //R03の下位
         CRTC.crtR03HDispEndPort = d & 255;
         {
           int curr = CRTC.crtR03HDispEndMask == 0 ? CRTC.crtR03HDispEndPort : CRTC.crtR03HDispEndTest;
           if (CRTC.crtR03HDispEndCurr != curr) {
             CRTC.crtR03HDispEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R04_VFRONT_END:  //R04の上位
         CRTC.crtR04VFrontEndPort = (d & 3) << 8 | CRTC.crtR04VFrontEndPort & 255;
         {
           int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
           if (CRTC.crtR04VFrontEndCurr != curr) {
             CRTC.crtR04VFrontEndCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R04_VFRONT_END + 1:  //R04の下位
         CRTC.crtR04VFrontEndPort = CRTC.crtR04VFrontEndPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
           if (CRTC.crtR04VFrontEndCurr != curr) {
             CRTC.crtR04VFrontEndCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R05_VSYNC_END:  //R05の上位
         CRTC.crtR05VSyncEndPort = (d & 3) << 8 | CRTC.crtR05VSyncEndPort & 255;
         {
           int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
           if (CRTC.crtR05VSyncEndCurr != curr) {
             CRTC.crtR05VSyncEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R05_VSYNC_END + 1:  //R05の下位
         CRTC.crtR05VSyncEndPort = CRTC.crtR05VSyncEndPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
           if (CRTC.crtR05VSyncEndCurr != curr) {
             CRTC.crtR05VSyncEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R06_VBACK_END:  //R06の上位
         CRTC.crtR06VBackEndPort = (d & 3) << 8 | CRTC.crtR06VBackEndPort & 255;
         {
           int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
           if (CRTC.crtR06VBackEndCurr != curr) {
             CRTC.crtR06VBackEndCurr = curr;
             CRTC.crtVDispStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R06_VBACK_END + 1:  //R06の下位
         CRTC.crtR06VBackEndPort = CRTC.crtR06VBackEndPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
           if (CRTC.crtR06VBackEndCurr != curr) {
             CRTC.crtR06VBackEndCurr = curr;
             CRTC.crtVDispStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R07_VDISP_END:  //R07の上位
         CRTC.crtR07VDispEndPort = (d & 3) << 8 | CRTC.crtR07VDispEndPort & 255;
         {
           int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
           if (CRTC.crtR07VDispEndCurr != curr) {
             CRTC.crtR07VDispEndCurr = curr;
             CRTC.crtVIdleStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R07_VDISP_END + 1:  //R07の下位
         CRTC.crtR07VDispEndPort = CRTC.crtR07VDispEndPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
           if (CRTC.crtR07VDispEndCurr != curr) {
             CRTC.crtR07VDispEndCurr = curr;
             CRTC.crtVIdleStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R08_ADJUST:  //R08の上位
         return;
       case CRTC.CRT_R08_ADJUST + 1:  //R08の下位
         d &= 0xff;
         if (CRTC.crtR08Adjust != d) {
           CRTC.crtR08Adjust = d;
           CRTC.crtRestart ();
         }
         return;
       case CRTC.CRT_R09_IRQ_RASTER:  //R09の上位
         CRTC.crtR09IRQRasterPort = (d & 3) << 8 | CRTC.crtR09IRQRasterPort & 255;
         {
           int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
           if (CRTC.crtR09IRQRasterCurr != curr) {
             CRTC.crtR09IRQRasterCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             if (RasterBreakPoint.RBP_ON) {
               RasterBreakPoint.rbpCheckIRQ ();
             }
           }
         }
         return;
       case CRTC.CRT_R09_IRQ_RASTER + 1:  //R09の下位
         CRTC.crtR09IRQRasterPort = CRTC.crtR09IRQRasterPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
           if (CRTC.crtR09IRQRasterCurr != curr) {
             CRTC.crtR09IRQRasterCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             if (RasterBreakPoint.RBP_ON) {
               RasterBreakPoint.rbpCheckIRQ ();
             }
           }
         }
         return;
       case CRTC.CRT_R10_TX_X:  //R10の上位
         CRTC.crtR10TxXPort = (d & 3) << 8 | CRTC.crtR10TxXPort & 255;
         {
           int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
           if (CRTC.crtR10TxXCurr != curr) {
             CRTC.crtR10TxXCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R10_TX_X + 1:  //R10の下位
         CRTC.crtR10TxXPort = CRTC.crtR10TxXPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
           if (CRTC.crtR10TxXCurr != curr) {
             CRTC.crtR10TxXCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R11_TX_Y:  //R11の上位
         CRTC.crtR11TxYPort = (d & 3) << 8 | CRTC.crtR11TxYPort & 255;
         {
           int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
           if (CRTC.crtR11TxYCurr != curr) {
             CRTC.crtR11TxYCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R11_TX_Y + 1:  //R11の下位
         CRTC.crtR11TxYPort = CRTC.crtR11TxYPort & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
           if (CRTC.crtR11TxYCurr != curr) {
             CRTC.crtR11TxYCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R12_GR_X_0:  //R12の上位
         CRTC.crtR12GrXPort[0] = (d & 3) << 8 | CRTC.crtR12GrXPort[0] & 255;
         {
           int curr = CRTC.crtR12GrXMask[0] == 0 ? CRTC.crtR12GrXPort[0] : CRTC.crtR12GrXTest[0];
           if (CRTC.crtR12GrXCurr[0] != curr) {
             CRTC.crtR12GrXCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R12_GR_X_0 + 1:  //R12の下位
         CRTC.crtR12GrXPort[0] = CRTC.crtR12GrXPort[0] & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR12GrXMask[0] == 0 ? CRTC.crtR12GrXPort[0] : CRTC.crtR12GrXTest[0];
           if (CRTC.crtR12GrXCurr[0] != curr) {
             CRTC.crtR12GrXCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R13_GR_Y_0:  //R13の上位
         CRTC.crtR13GrYPort[0] = (d & 3) << 8 | CRTC.crtR13GrYPort[0] & 255;
         {
           int curr = CRTC.crtR13GrYMask[0] == 0 ? CRTC.crtR13GrYPort[0] : CRTC.crtR13GrYTest[0];
           if (CRTC.crtR13GrYCurr[0] != curr) {
             CRTC.crtR13GrYCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R13_GR_Y_0 + 1:  //R13の下位
         CRTC.crtR13GrYPort[0] = CRTC.crtR13GrYPort[0] & 3 << 8 | d & 255;
         {
           int curr = CRTC.crtR13GrYMask[0] == 0 ? CRTC.crtR13GrYPort[0] : CRTC.crtR13GrYTest[0];
           if (CRTC.crtR13GrYCurr[0] != curr) {
             CRTC.crtR13GrYCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R14_GR_X_1:  //R14の上位
         CRTC.crtR12GrXPort[1] = (d & 1) << 8 | CRTC.crtR12GrXPort[1] & 255;
         {
           int curr = CRTC.crtR12GrXMask[1] == 0 ? CRTC.crtR12GrXPort[1] : CRTC.crtR12GrXTest[1];
           if (CRTC.crtR12GrXCurr[1] != curr) {
             CRTC.crtR12GrXCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R14_GR_X_1 + 1:  //R14の下位
         CRTC.crtR12GrXPort[1] = CRTC.crtR12GrXPort[1] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR12GrXMask[1] == 0 ? CRTC.crtR12GrXPort[1] : CRTC.crtR12GrXTest[1];
           if (CRTC.crtR12GrXCurr[1] != curr) {
             CRTC.crtR12GrXCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R15_GR_Y_1:  //R15の上位
         CRTC.crtR13GrYPort[1] = (d & 1) << 8 | CRTC.crtR13GrYPort[1] & 255;
         {
           int curr = CRTC.crtR13GrYMask[1] == 0 ? CRTC.crtR13GrYPort[1] : CRTC.crtR13GrYTest[1];
           if (CRTC.crtR13GrYCurr[1] != curr) {
             CRTC.crtR13GrYCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R15_GR_Y_1 + 1:  //R15の下位
         CRTC.crtR13GrYPort[1] = CRTC.crtR13GrYPort[1] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR13GrYMask[1] == 0 ? CRTC.crtR13GrYPort[1] : CRTC.crtR13GrYTest[1];
           if (CRTC.crtR13GrYCurr[1] != curr) {
             CRTC.crtR13GrYCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R16_GR_X_2:  //R16の上位
         CRTC.crtR12GrXPort[2] = (d & 1) << 8 | CRTC.crtR12GrXPort[2] & 255;
         {
           int curr = CRTC.crtR12GrXMask[2] == 0 ? CRTC.crtR12GrXPort[2] : CRTC.crtR12GrXTest[2];
           if (CRTC.crtR12GrXCurr[2] != curr) {
             CRTC.crtR12GrXCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R16_GR_X_2 + 1:  //R16の下位
         CRTC.crtR12GrXPort[2] = CRTC.crtR12GrXPort[2] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR12GrXMask[2] == 0 ? CRTC.crtR12GrXPort[2] : CRTC.crtR12GrXTest[2];
           if (CRTC.crtR12GrXCurr[2] != curr) {
             CRTC.crtR12GrXCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R17_GR_Y_2:  //R17の上位
         CRTC.crtR13GrYPort[2] = (d & 1) << 8 | CRTC.crtR13GrYPort[2] & 255;
         {
           int curr = CRTC.crtR13GrYMask[2] == 0 ? CRTC.crtR13GrYPort[2] : CRTC.crtR13GrYTest[2];
           if (CRTC.crtR13GrYCurr[2] != curr) {
             CRTC.crtR13GrYCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R17_GR_Y_2 + 1:  //R17の下位
         CRTC.crtR13GrYPort[2] = CRTC.crtR13GrYPort[2] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR13GrYMask[2] == 0 ? CRTC.crtR13GrYPort[2] : CRTC.crtR13GrYTest[2];
           if (CRTC.crtR13GrYCurr[2] != curr) {
             CRTC.crtR13GrYCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R18_GR_X_3:  //R18の上位
         CRTC.crtR12GrXPort[3] = (d & 1) << 8 | CRTC.crtR12GrXPort[3] & 255;
         {
           int curr = CRTC.crtR12GrXMask[3] == 0 ? CRTC.crtR12GrXPort[3] : CRTC.crtR12GrXTest[3];
           if (CRTC.crtR12GrXCurr[3] != curr) {
             CRTC.crtR12GrXCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R18_GR_X_3 + 1:  //R18の下位
         CRTC.crtR12GrXPort[3] = CRTC.crtR12GrXPort[3] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR12GrXMask[3] == 0 ? CRTC.crtR12GrXPort[3] : CRTC.crtR12GrXTest[3];
           if (CRTC.crtR12GrXCurr[3] != curr) {
             CRTC.crtR12GrXCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R19_GR_Y_3:  //R19の上位
         CRTC.crtR13GrYPort[3] = (d & 1) << 8 | CRTC.crtR13GrYPort[3] & 255;
         {
           int curr = CRTC.crtR13GrYMask[3] == 0 ? CRTC.crtR13GrYPort[3] : CRTC.crtR13GrYTest[3];
           if (CRTC.crtR13GrYCurr[3] != curr) {
             CRTC.crtR13GrYCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R19_GR_Y_3 + 1:  //R19の下位
         CRTC.crtR13GrYPort[3] = CRTC.crtR13GrYPort[3] & 1 << 8 | d & 255;
         {
           int curr = CRTC.crtR13GrYMask[3] == 0 ? CRTC.crtR13GrYPort[3] : CRTC.crtR13GrYTest[3];
           if (CRTC.crtR13GrYCurr[3] != curr) {
             CRTC.crtR13GrYCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R20_MODE:  //R20の上位
         CRTC.crtSetMemoryMode (d & 7);
         return;
       case CRTC.CRT_R20_MODE + 1:  //R20の下位
         CRTC.crtHighResoPort = d >>> 4 & 1;
         CRTC.crtVResoPort    = d >>> 2 & 3;
         CRTC.crtHResoPort    = d       & 3;
         int highResoCurr = CRTC.crtHighResoMask == 0 ? CRTC.crtHighResoPort : CRTC.crtHighResoTest;
         int vResoCurr = CRTC.crtVResoMask == 0 ? CRTC.crtVResoPort : CRTC.crtVResoTest;
         int hResoCurr = CRTC.crtHResoMask == 0 ? CRTC.crtHResoPort : CRTC.crtHResoTest;
         if (CRTC.crtHighResoCurr != highResoCurr ||
             CRTC.crtVResoCurr != vResoCurr ||
             CRTC.crtHResoCurr != hResoCurr) {
           CRTC.crtHighResoCurr = highResoCurr;
           CRTC.crtVResoCurr = vResoCurr;
           CRTC.crtHResoCurr = hResoCurr;
           CRTC.crtRestart ();
         }
         return;
       case CRTC.CRT_R21_SELECT:  //R21の上位
         CRTC.crtBitMask   = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
         CRTC.crtSimAccess = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
         return;
       case CRTC.CRT_R21_SELECT + 1:  //R21の下位
         CRTC.crtSimPlane3 = (byte) d < 0;  //(d & 128) != 0。d << 24 < 0
         CRTC.crtSimPlane2 = d << 25 < 0;  //(d & 64) != 0
         CRTC.crtSimPlane1 = d << 26 < 0;  //(d & 32) != 0
         CRTC.crtSimPlane0 = d << 27 < 0;  //(d & 16) != 0
         CRTC.crtCCPlane3  = XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0;
         CRTC.crtCCPlane2  = XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0;
         CRTC.crtCCPlane1  = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
         CRTC.crtCCPlane0  = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
         return;
       case CRTC.CRT_R22_BLOCK:  //R22の上位
         CRTC.crtR22SrcBlock = d & 255;
         return;
       case CRTC.CRT_R22_BLOCK + 1:  //R22の下位
         CRTC.crtR22DstBlock = d & 255;
         return;
       case CRTC.CRT_R23_MASK:  //R23の上位
         CRTC.crtR23Mask = (0xff & d) << 8 | 0xff & CRTC.crtR23Mask;
         return;
       case CRTC.CRT_R23_MASK + 1:  //R23の下位
         CRTC.crtR23Mask = 0xff00 & CRTC.crtR23Mask | 0xff & d;
         return;
       case CRTC.CRT_R24:  //R24の上位
       case CRTC.CRT_R24 + 1:  //R24の下位
       case CRTC.CRT_ACTION:  //動作ポートの上位
         return;
       case CRTC.CRT_ACTION + 1:  //動作ポートの下位
         {
           boolean rasterCopyOn = (d & 8) != 0;
           if (CRTC.crtRasterCopyOn != rasterCopyOn) {
             CRTC.crtRasterCopyOn = rasterCopyOn;  //ラスタコピー
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
           }
         }
         CRTC.crtClearStandby = (d & 2) != 0;  //高速クリア
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case CRTC.CRT_R00_HFRONT_END:  //R00
         CRTC.crtR00HFrontEndPort = d & 255;
         {
           int curr = CRTC.crtR00HFrontEndMask == 0 ? CRTC.crtR00HFrontEndPort : CRTC.crtR00HFrontEndTest;
           if (CRTC.crtR00HFrontEndCurr != curr) {
             CRTC.crtR00HFrontEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R01_HSYNC_END:  //R01
         CRTC.crtR01HSyncEndPort = d & 255;
         {
           int curr = CRTC.crtR01HSyncEndMask == 0 ? CRTC.crtR01HSyncEndPort : CRTC.crtR01HSyncEndTest;
           if (CRTC.crtR01HSyncEndCurr != curr) {
             CRTC.crtR01HSyncEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R02_HBACK_END:  //R02
         CRTC.crtR02HBackEndPort = d & 255;
         {
           int curr = CRTC.crtR02HBackEndMask == 0 ? CRTC.crtR02HBackEndPort : CRTC.crtR02HBackEndTest;
           if (CRTC.crtR02HBackEndCurr != curr) {
             CRTC.crtR02HBackEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R03_HDISP_END:  //R03
         CRTC.crtR03HDispEndPort = d & 255;
         {
           int curr = CRTC.crtR03HDispEndMask == 0 ? CRTC.crtR03HDispEndPort : CRTC.crtR03HDispEndTest;
           if (CRTC.crtR03HDispEndCurr != curr) {
             CRTC.crtR03HDispEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R04_VFRONT_END:  //R04
         CRTC.crtR04VFrontEndPort = d & 1023;
         {
           int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
           if (CRTC.crtR04VFrontEndCurr != curr) {
             CRTC.crtR04VFrontEndCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R05_VSYNC_END:  //R05
         CRTC.crtR05VSyncEndPort = d & 1023;
         {
           int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
           if (CRTC.crtR05VSyncEndCurr != curr) {
             CRTC.crtR05VSyncEndCurr = curr;
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R06_VBACK_END:  //R06
         CRTC.crtR06VBackEndPort = d & 1023;
         {
           int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
           if (CRTC.crtR06VBackEndCurr != curr) {
             CRTC.crtR06VBackEndCurr = curr;
             CRTC.crtVDispStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R07_VDISP_END:  //R07
         CRTC.crtR07VDispEndPort = d & 1023;
         {
           int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
           if (CRTC.crtR07VDispEndCurr != curr) {
             CRTC.crtR07VDispEndCurr = curr;
             CRTC.crtVIdleStart = curr + 1;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             CRTC.crtRestart ();
           }
         }
         return;
       case CRTC.CRT_R08_ADJUST:  //R08
         d &= 0x00ff;
         if (CRTC.crtR08Adjust != d) {
           CRTC.crtR08Adjust = d;
           CRTC.crtRestart ();
         }
         return;
       case CRTC.CRT_R09_IRQ_RASTER:  //R09
         CRTC.crtR09IRQRasterPort = d & 1023;
         {
           int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
           if (CRTC.crtR09IRQRasterCurr != curr) {
             CRTC.crtR09IRQRasterCurr = curr;
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
             if (RasterBreakPoint.RBP_ON) {
               RasterBreakPoint.rbpCheckIRQ ();
             }
           }
         }
         return;
       case CRTC.CRT_R10_TX_X:  //R10
         CRTC.crtR10TxXPort = d & 1023;
         {
           int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
           if (CRTC.crtR10TxXCurr != curr) {
             CRTC.crtR10TxXCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R11_TX_Y:  //R11
         CRTC.crtR11TxYPort = d & 1023;
         {
           int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
           if (CRTC.crtR11TxYCurr != curr) {
             CRTC.crtR11TxYCurr = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R12_GR_X_0:  //R12
         CRTC.crtR12GrXPort[0] = d & 1023;
         {
           int curr = CRTC.crtR12GrXMask[0] == 0 ? CRTC.crtR12GrXPort[0] : CRTC.crtR12GrXTest[0];
           if (CRTC.crtR12GrXCurr[0] != curr) {
             CRTC.crtR12GrXCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R13_GR_Y_0:  //R13
         CRTC.crtR13GrYPort[0] = d & 1023;
         {
           int curr = CRTC.crtR13GrYMask[0] == 0 ? CRTC.crtR13GrYPort[0] : CRTC.crtR13GrYTest[0];
           if (CRTC.crtR13GrYCurr[0] != curr) {
             CRTC.crtR13GrYCurr[0] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R14_GR_X_1:  //R14
         CRTC.crtR12GrXPort[1] = d & 511;
         {
           int curr = CRTC.crtR12GrXMask[1] == 0 ? CRTC.crtR12GrXPort[1] : CRTC.crtR12GrXTest[1];
           if (CRTC.crtR12GrXCurr[1] != curr) {
             CRTC.crtR12GrXCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R15_GR_Y_1:  //R15
         CRTC.crtR13GrYPort[1] = d & 511;
         {
           int curr = CRTC.crtR13GrYMask[1] == 0 ? CRTC.crtR13GrYPort[1] : CRTC.crtR13GrYTest[1];
           if (CRTC.crtR13GrYCurr[1] != curr) {
             CRTC.crtR13GrYCurr[1] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R16_GR_X_2:  //R16
         CRTC.crtR12GrXPort[2] = d & 511;
         {
           int curr = CRTC.crtR12GrXMask[2] == 0 ? CRTC.crtR12GrXPort[2] : CRTC.crtR12GrXTest[2];
           if (CRTC.crtR12GrXCurr[2] != curr) {
             CRTC.crtR12GrXCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R17_GR_Y_2:  //R17
         CRTC.crtR13GrYPort[2] = d & 511;
         {
           int curr = CRTC.crtR13GrYMask[2] == 0 ? CRTC.crtR13GrYPort[2] : CRTC.crtR13GrYTest[2];
           if (CRTC.crtR13GrYCurr[2] != curr) {
             CRTC.crtR13GrYCurr[2] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R18_GR_X_3:  //R18
         CRTC.crtR12GrXPort[3] = d & 511;
         {
           int curr = CRTC.crtR12GrXMask[3] == 0 ? CRTC.crtR12GrXPort[3] : CRTC.crtR12GrXTest[3];
           if (CRTC.crtR12GrXCurr[3] != curr) {
             CRTC.crtR12GrXCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R19_GR_Y_3:  //R19
         CRTC.crtR13GrYPort[3] = d & 511;
         {
           int curr = CRTC.crtR13GrYMask[3] == 0 ? CRTC.crtR13GrYPort[3] : CRTC.crtR13GrYTest[3];
           if (CRTC.crtR13GrYCurr[3] != curr) {
             CRTC.crtR13GrYCurr[3] = curr;
             CRTC.crtAllStamp += 2;
           }
         }
         return;
       case CRTC.CRT_R20_MODE:  //R20
         CRTC.crtSetMemoryMode (d >> 8);
         CRTC.crtHighResoPort = d >>> 4 & 1;
         CRTC.crtVResoPort    = d >>> 2 & 3;
         CRTC.crtHResoPort    = d       & 3;
         int highResoCurr = CRTC.crtHighResoMask == 0 ? CRTC.crtHighResoPort : CRTC.crtHighResoTest;
         int vResoCurr = CRTC.crtVResoMask == 0 ? CRTC.crtVResoPort : CRTC.crtVResoTest;
         int hResoCurr = CRTC.crtHResoMask == 0 ? CRTC.crtHResoPort : CRTC.crtHResoTest;
         if (CRTC.crtHighResoCurr != highResoCurr ||
             CRTC.crtVResoCurr != vResoCurr ||
             CRTC.crtHResoCurr != hResoCurr) {
           CRTC.crtHighResoCurr = highResoCurr;
           CRTC.crtVResoCurr = vResoCurr;
           CRTC.crtHResoCurr = hResoCurr;
           CRTC.crtRestart ();
         }
         return;
       case CRTC.CRT_R21_SELECT:  //R21
         CRTC.crtBitMask   = d << 22 < 0;  //(d & 512) != 0
         CRTC.crtSimAccess = d << 23 < 0;  //(d & 256) != 0
         CRTC.crtSimPlane3 = (byte) d < 0;  //(d & 128) != 0。d << 24 < 0
         CRTC.crtSimPlane2 = d << 25 < 0;  //(d & 64) != 0
         CRTC.crtSimPlane1 = d << 26 < 0;  //(d & 32) != 0
         CRTC.crtSimPlane0 = d << 27 < 0;  //(d & 16) != 0
         CRTC.crtCCPlane3  = XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0;
         CRTC.crtCCPlane2  = XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0;
         CRTC.crtCCPlane1  = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
         CRTC.crtCCPlane0  = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
         return;
       case CRTC.CRT_R22_BLOCK:  //R22
         CRTC.crtR22SrcBlock = d >> 8 & 255;
         CRTC.crtR22DstBlock = d      & 255;
         return;
       case CRTC.CRT_R23_MASK:  //R23
         CRTC.crtR23Mask = (char) d;
         return;
       case CRTC.CRT_R24:  //R24
         return;
       case CRTC.CRT_ACTION :  //動作ポート
         {
           boolean rasterCopyOn = (d & 8) != 0;
           if (CRTC.crtRasterCopyOn != rasterCopyOn) {
             CRTC.crtRasterCopyOn = rasterCopyOn;  //ラスタコピー
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
           }
         }
         CRTC.crtClearStandby = (d & 2) != 0;  //高速クリア
         return;
       }
       super.mmdWw (a, d);  //バスエラー
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_CRT

  //--------------------------------------------------------------------------------
  //MMD_VCN ビデオコントローラ
  MMD_VCN {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ビデオコントローラ" : "Video Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int ah = a & ~0xff;
       int al = a & 0x01;
       return (ah < 0x00e82200 ? al == 0 ? VideoController.vcnPal16G8[a >> 1 & 255] >> 8 : VideoController.vcnPal16G8[a >> 1 & 255] & 255 :  //グラフィックスパレット
               ah < 0x00e82400 ? al == 0 ? VideoController.vcnPal16TS[a >> 1 & 255] >> 8 : VideoController.vcnPal16TS[a >> 1 & 255] & 255 :  //テキストスプライトパレット
               ah == VideoController.VCN_REG1 ? al == 0 ? VideoController.vcnReg1Port >> 8 : VideoController.vcnReg1Port & 255 :
               ah == VideoController.VCN_REG2 ? al == 0 ? VideoController.vcnReg2Port >> 8 : VideoController.vcnReg2Port & 255 :
               ah == VideoController.VCN_REG3 ? al == 0 ? VideoController.vcnReg3Port >> 8 : VideoController.vcnReg3Port & 255 : 0);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int ah = a & ~0xff;
       return (ah < 0x00e82200 ? VideoController.vcnPal16G8[a >> 1 & 255] :  //グラフィックスパレット
               ah < 0x00e82400 ? VideoController.vcnPal16TS[a >> 1 & 255] :  //テキストスプライトパレット
               ah == VideoController.VCN_REG1 ? VideoController.vcnReg1Port :
               ah == VideoController.VCN_REG2 ? VideoController.vcnReg2Port :
               ah == VideoController.VCN_REG3 ? VideoController.vcnReg3Port : 0);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int ah = a & ~0xff;
       int al = a & 0x01;
       if (ah < 0x00e82200) {  //グラフィックスパレット
         d &= 0xff;
         int n = a >> 1 & 255;
         if (al == 0) {
           VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[VideoController.vcnPal16G8[n] = d << 8 | 0xff & VideoController.vcnPal16G8[n]];
           if ((n & 1) == 0) {
             VideoController.vcnPal8G16L[n] = d;
           } else {
             VideoController.vcnPal8G16H[n - 1] = d << 8;
           }
         } else {
           VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[VideoController.vcnPal16G8[n] = 0xff00 & VideoController.vcnPal16G8[n] | d];
           if ((n & 1) == 0) {
             VideoController.vcnPal8G16L[n + 1] = d;
           } else {
             VideoController.vcnPal8G16H[n] = d << 8;
           }
         }
         if ((0x001f & VideoController.vcnReg3Curr) != 0) {  //グラフィックス画面が表示されている
           CRTC.crtAllStamp += 2;
         }
       } else if (ah < 0x00e82400) {  //テキストスプライトパレット
         d &= 0xff;
         int n = a >> 1 & 255;
         VideoController.vcnPal16TS[n] = d = al == 0 ? d << 8 | 0xff & VideoController.vcnPal16TS[n] : 0xff00 & VideoController.vcnPal16TS[n] | d;
         VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[d];
         if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
             SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0 ||  //スプライト画面が表示されている
             VideoController.vcnReg3Curr << 31 - 5 < 0 &&  //テキスト画面が表示されている
             n < 16) {  //テキストパレット
           CRTC.crtAllStamp += 2;
         }
       } else if (ah == VideoController.VCN_REG1) {
         d = al == 0 ? 0x00 : 0x07 & d;
         if (VideoController.vcnReg1Port != d) {
           VideoController.vcnReg1Port = d;
           VideoController.vcnReg1Curr = VideoController.vcnReg1Port & ~VideoController.vcnReg1Mask | VideoController.vcnReg1Test & VideoController.vcnReg1Mask;
           VideoController.vcnUpdateMode ();
         }
       } else if (ah == VideoController.VCN_REG2) {
         d = al == 0 ? (0x3f & d) << 8 | 0xff & VideoController.vcnReg2Port : 0xff00 & VideoController.vcnReg2Port | 0xff & d;
         if (VideoController.vcnReg2Port != d) {
           VideoController.vcnReg2Port = d;
           VideoController.vcnReg2Curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
           VideoController.vcnUpdateMode ();
         }
       } else if (ah == VideoController.VCN_REG3) {
         d = al == 0 ? (0xff & d) << 8 | 0xff & VideoController.vcnReg3Port : 0xff00 & VideoController.vcnReg3Port | 0xff & d;
         if (VideoController.vcnReg3Port != d) {
           VideoController.vcnReg3Port = d;
           VideoController.vcnReg3Curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
           VideoController.vcnUpdateMode ();
         }
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int ah = a & ~0xff;
       if (ah < 0x00e82200) {  //グラフィックスパレット
         int n = a >> 1 & 255;
         VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[VideoController.vcnPal16G8[n] = (char) d];
         if ((n & 1) == 0) {
           VideoController.vcnPal8G16L[n] = 0xff & d >> 8;
           VideoController.vcnPal8G16L[n + 1] = 0xff & d;
         } else {
           VideoController.vcnPal8G16H[n - 1] = 0xff00 & d;
           VideoController.vcnPal8G16H[n] = 0xff00 & d << 8;
         }
         if ((0x001f & VideoController.vcnReg3Curr) != 0) {  //グラフィックス画面が表示されている
           CRTC.crtAllStamp += 2;
         }
       } else if (ah < 0x00e82400) {  //テキストスプライトパレット
         int n = a >> 1 & 255;
         VideoController.vcnPal16TS[n] = d = (char) d;
         VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[d];
         if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
             SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0 ||  //スプライト画面が表示されている
             VideoController.vcnReg3Curr << 31 - 5 < 0 &&  //テキスト画面が表示されている
             n < 16) {  //テキストパレット
           CRTC.crtAllStamp += 2;
         }
       } else if (ah == VideoController.VCN_REG1) {
         d &= 0x0007;
         if (VideoController.vcnReg1Port != d) {
           VideoController.vcnReg1Port = d;
           VideoController.vcnReg1Curr = VideoController.vcnReg1Port & ~VideoController.vcnReg1Mask | VideoController.vcnReg1Test & VideoController.vcnReg1Mask;
           VideoController.vcnUpdateMode ();
         }
       } else if (ah == VideoController.VCN_REG2) {
         d &= 0x3fff;
         if (VideoController.vcnReg2Port != d) {
           VideoController.vcnReg2Port = d;
           VideoController.vcnReg2Curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
           VideoController.vcnUpdateMode ();
         }
       } else if (ah == VideoController.VCN_REG3) {
         d &= 0xffff;
         if (VideoController.vcnReg3Port != d) {
           VideoController.vcnReg3Port = d;
           VideoController.vcnReg3Curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
           VideoController.vcnUpdateMode ();
         }
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_VCN

  //--------------------------------------------------------------------------------
  //MMD_DMA DMAコントローラ
  MMD_DMA {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "DMA コントローラ" : "DMA Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int d;
       int al = a & 255;
       if (al == HD63450.DMA_GCR) {
         d = HD63450.dmaBT | HD63450.dmaBR;
         if (HD63450.DMA_DEBUG_TRACE != 0) {
           System.out.printf ("%d %08x HD63450.dmaRbz(0x%08x)=0x%02x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
         }
       } else {
         int i = al >> 6;  //チャンネル
         switch (al & 0x3f) {
         case HD63450.DMA_CSR:
           d = HD63450.dmaCOC[i] | HD63450.dmaBLC[i] | HD63450.dmaNDT[i] | HD63450.dmaERR[i] | HD63450.dmaACT[i] | HD63450.dmaDIT[i] | HD63450.dmaPCT[i] | HD63450.dmaPCS[i];
           break;
         case HD63450.DMA_CER:
           d = HD63450.dmaErrorCode[i];
           break;
         case HD63450.DMA_DCR:
           d = HD63450.dmaXRM[i] | HD63450.dmaDTYP[i] | HD63450.dmaDPS[i] | HD63450.dmaPCL[i];
           break;
         case HD63450.DMA_OCR:
           d = HD63450.dmaDIR[i] | HD63450.dmaBTD[i] | HD63450.dmaSIZE[i] | HD63450.dmaCHAIN[i] | HD63450.dmaREQG[i];
           break;
         case HD63450.DMA_SCR:
           d = HD63450.dmaMAC[i] | HD63450.dmaDAC[i];
           break;
         case HD63450.DMA_CCR:
           d = HD63450.dmaSTR[i] | HD63450.dmaCNT[i] | HD63450.dmaHLT[i] | HD63450.dmaSAB[i] | HD63450.dmaITE[i];
           break;
         case HD63450.DMA_MTC:
           d = HD63450.dmaMTC[i] >> 8;
           break;
         case HD63450.DMA_MTC + 1:
           d = HD63450.dmaMTC[i] & 255;
           break;
         case HD63450.DMA_MAR:
           d = HD63450.dmaMAR[i] >>> 24;
           break;
         case HD63450.DMA_MAR + 1:
           d = HD63450.dmaMAR[i] >> 16 & 255;
           break;
         case HD63450.DMA_MAR + 2:
           d = (char) HD63450.dmaMAR[i] >> 8;
           break;
         case HD63450.DMA_MAR + 3:
           d = HD63450.dmaMAR[i] & 255;
           break;
         case HD63450.DMA_DAR:
           d = HD63450.dmaDAR[i] >>> 24;
           break;
         case HD63450.DMA_DAR + 1:
           d = HD63450.dmaDAR[i] >> 16 & 255;
           break;
         case HD63450.DMA_DAR + 2:
           d = (char) HD63450.dmaDAR[i] >> 8;
           break;
         case HD63450.DMA_DAR + 3:
           d = HD63450.dmaDAR[i] & 255;
           break;
         case HD63450.DMA_BTC:
           d = HD63450.dmaBTC[i] >> 8;
           break;
         case HD63450.DMA_BTC + 1:
           d = HD63450.dmaBTC[i] & 255;
           break;
         case HD63450.DMA_BAR:
           d = HD63450.dmaBAR[i] >>> 24;
           break;
         case HD63450.DMA_BAR + 1:
           d = HD63450.dmaBAR[i] >> 16 & 255;
           break;
         case HD63450.DMA_BAR + 2:
           d = (char) HD63450.dmaBAR[i] >> 8;
           break;
         case HD63450.DMA_BAR + 3:
           d = HD63450.dmaBAR[i] & 255;
           break;
         case HD63450.DMA_NIV:
           d = HD63450.dmaNIV[i];
           break;
         case HD63450.DMA_EIV:
           d = HD63450.dmaEIV[i];
           break;
         case HD63450.DMA_MFC:
           d = HD63450.dmaMFC[i];
           break;
         case HD63450.DMA_CPR:
           d = HD63450.dmaCP[i];
           break;
         case HD63450.DMA_DFC:
           d = HD63450.dmaDFC[i];
           break;
         case HD63450.DMA_BFC:
           d = HD63450.dmaBFC[i];
           break;
         default:
           d = 0;
         }
         if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
           System.out.printf ("%d %08x HD63450.dmaRbz(0x%08x)=0x%02x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
         }
       }
       return d;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int d;
       int al = a & 255;
       int i = al >> 6;  //チャンネル
       switch (al & 0x3f) {
       case HD63450.DMA_MTC:
         d = HD63450.dmaMTC[i];
         break;
       case HD63450.DMA_MAR:
         d = HD63450.dmaMAR[i] >>> 16;
         break;
       case HD63450.DMA_MAR + 2:
         d = (char) HD63450.dmaMAR[i];
         break;
       case HD63450.DMA_DAR:
         d = HD63450.dmaDAR[i] >>> 16;
         break;
       case HD63450.DMA_DAR + 2:
         d = (char) HD63450.dmaDAR[i];
         break;
       case HD63450.DMA_BTC:
         d = HD63450.dmaBTC[i];
         break;
       case HD63450.DMA_BAR:
         d = HD63450.dmaBAR[i] >>> 16;
         break;
       case HD63450.DMA_BAR + 2:
         d = (char) HD63450.dmaBAR[i];
         break;
       default:
         d = mmdRbz (a) << 8 | mmdRbz (a + 1);
       }
       if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
         System.out.printf ("%d %08x HD63450.dmaRwz(0x%08x)=0x%04x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
       }
       return d;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int d;
       int al = a & 255;
       int i = al >> 6;  //チャンネル
       switch (al & 0x3f) {
       case HD63450.DMA_MAR:
         d = HD63450.dmaMAR[i];
         break;
       case HD63450.DMA_DAR:
         d = HD63450.dmaDAR[i];
         break;
       case HD63450.DMA_BAR:
         d = HD63450.dmaBAR[i];
         break;
       default:
         d = mmdRwz (a) << 16 | mmdRwz (a + 2);
       }
       if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
         System.out.printf ("%d %08x HD63450.dmaRls(0x%08x)=0x%08x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
       }
       return d;
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int al = a & 255;
       if (al == HD63450.DMA_GCR) {
         if (HD63450.DMA_DEBUG_TRACE != 0) {
           System.out.printf ("%d %08x HD63450.dmaWb(0x%08x,0x%02x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
         }
         HD63450.dmaBT = d & HD63450.DMA_BT;
         HD63450.dmaBR = d & HD63450.DMA_BR;
         HD63450.dmaBurstInterval = HD63450.DMA_CLOCK_UNIT << 4 + (HD63450.dmaBT >> 2);
         HD63450.dmaBurstSpan = HD63450.dmaBurstInterval >> 1 + (HD63450.dmaBR & 3);
         return;
       }
       int i = al >> 6;  //チャンネル
       if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
         System.out.printf ("%d %08x HD63450.dmaWb(0x%08x,0x%02x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
       }
       switch (al & 0x3f) {
       case HD63450.DMA_CSR:
         if ((byte) d == -1) {  //HD63450.DMA_CSRは-1を書き込むとクリアされる。それ以外は無視
           HD63450.dmaCOC[i] = 0;
           HD63450.dmaBLC[i] = 0;
           HD63450.dmaNDT[i] = 0;
           HD63450.dmaERR[i] = 0;
         }
         return;
       case HD63450.DMA_CER:
         //HD63450.DMA_CERはread-only
         return;
       case HD63450.DMA_DCR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaXRM[i] = d & HD63450.DMA_XRM;
         HD63450.dmaDTYP[i] = d & HD63450.DMA_DTYP;
         HD63450.dmaDPS[i] = d & HD63450.DMA_DPS;
         HD63450.dmaPCL[i] = d & HD63450.DMA_PCL;
         return;
       case HD63450.DMA_OCR:
         HD63450.dmaDIR[i] = d & HD63450.DMA_DIR;
         HD63450.dmaBTD[i] = d & HD63450.DMA_BTD;
         HD63450.dmaSIZE[i] = d & HD63450.DMA_SIZE;
         HD63450.dmaCHAIN[i] = d & HD63450.DMA_CHAIN;
         HD63450.dmaREQG[i] = d & HD63450.DMA_REQG;
         return;
       case HD63450.DMA_SCR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAC[i] = d & HD63450.DMA_MAC;
         HD63450.dmaMACValue[i] = HD63450.dmaMAC[i] == HD63450.DMA_INC_MAR ? 1 : HD63450.dmaMAC[i] == HD63450.DMA_DEC_MAR ? -1 : 0;
         HD63450.dmaDAC[i] = d & HD63450.DMA_DAC;
         HD63450.dmaDACValue[i] = HD63450.dmaDAC[i] == HD63450.DMA_INC_DAR ? 1 : HD63450.dmaDAC[i] == HD63450.DMA_DEC_DAR ? -1 : 0;
         return;
       case HD63450.DMA_CCR:
         HD63450.dmaHLT[i] = d & HD63450.DMA_HLT;
         HD63450.dmaITE[i] = d & HD63450.DMA_ITE;
         //HD63450.DMA_CNT
         if ((d & HD63450.DMA_CNT) != 0) {
           if ((HD63450.dmaACT[i] == 0 && (d & HD63450.DMA_STR) == 0) || HD63450.dmaBLC[i] != 0) {  //動作中でないかブロック転送完了済みの状態でHD63450.DMA_CNTをセットしようとした
             HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
             return;
           }
           if (HD63450.dmaCHAIN[i] != HD63450.DMA_NO_CHAINING) {  //チェインモードのときHD63450.DMA_CNTをセットしようとした
             HD63450.dmaErrorExit (i, HD63450.DMA_CONFIGURATION_ERROR);
             return;
           }
           HD63450.dmaCNT[i] = HD63450.DMA_CNT;  //HD63450.DMA_CNTに0を書き込んでもクリアされない
         }
         //HD63450.DMA_SAB
         if ((d & HD63450.DMA_SAB) != 0) {
           //HD63450.dmaSABには書き込まない。SABは読み出すと常に0
           HD63450.dmaCOC[i] = 0;
           HD63450.dmaBLC[i] = 0;
           HD63450.dmaNDT[i] = 0;
           HD63450.dmaHLT[i] = 0;
           HD63450.dmaCNT[i] = 0;
           if (HD63450.dmaACT[i] != 0 || (d & HD63450.DMA_STR) != 0) {  //動作中
             HD63450.dmaErrorExit (i, HD63450.DMA_SOFTWARE_ABORT);
           }
           return;
         }
         //HD63450.DMA_STR
         if ((d & HD63450.DMA_STR) != 0) {
           HD63450.dmaStart (i);
         }
         return;
       case HD63450.DMA_MTC:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMTC[i] = (0xff & d) << 8 | 0xff & HD63450.dmaMTC[i];
         return;
       case HD63450.DMA_MTC + 1:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMTC[i] = 0xff00 & HD63450.dmaMTC[i] | 0xff & d;
         return;
       case HD63450.DMA_MAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = d << 24 | 0xffffff & HD63450.dmaMAR[i];
         return;
       case HD63450.DMA_MAR + 1:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = (0xff & d) << 16 | 0xff00ffff & HD63450.dmaMAR[i];
         return;
       case HD63450.DMA_MAR + 2:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = (0xff & d) << 8 | 0xffff00ff & HD63450.dmaMAR[i];
         return;
       case HD63450.DMA_MAR + 3:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = ~0xff & HD63450.dmaMAR[i] | 0xff & d;
         return;
       case HD63450.DMA_DAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = d << 24 | 0xffffff & HD63450.dmaDAR[i];
         return;
       case HD63450.DMA_DAR + 1:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = (0xff & d) << 16 | 0xff00ffff & HD63450.dmaDAR[i];
         return;
       case HD63450.DMA_DAR + 2:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = (0xff & d) << 8 | 0xffff00ff & HD63450.dmaDAR[i];
         return;
       case HD63450.DMA_DAR + 3:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = ~0xff & HD63450.dmaDAR[i] | 0xff & d;
         return;
       case HD63450.DMA_BTC:
         HD63450.dmaBTC[i] = (0xff & d) << 8 | 0xff & HD63450.dmaBTC[i];
         return;
       case HD63450.DMA_BTC + 1:
         HD63450.dmaBTC[i] = 0xff00 & HD63450.dmaBTC[i] | 0xff & d;
         return;
       case HD63450.DMA_BAR:
         HD63450.dmaBAR[i] = d << 24 | 0xffffff & HD63450.dmaBAR[i];
         return;
       case HD63450.DMA_BAR + 1:
         HD63450.dmaBAR[i] = (0xff & d) << 16 | 0xff00ffff & HD63450.dmaBAR[i];
         return;
       case HD63450.DMA_BAR + 2:
         HD63450.dmaBAR[i] = (0xff & d) << 8 | 0xffff00ff & HD63450.dmaBAR[i];
         return;
       case HD63450.DMA_BAR + 3:
         HD63450.dmaBAR[i] = ~0xff & HD63450.dmaBAR[i] | 0xff & d;
         return;
       case HD63450.DMA_NIV:
         HD63450.dmaNIV[i] = 0xff & d;
         return;
       case HD63450.DMA_EIV:
         HD63450.dmaEIV[i] = 0xff & d;
         return;
       case HD63450.DMA_MFC:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMFC[i] = d & HD63450.DMA_FC2;
         if (DataBreakPoint.DBP_ON) {
           HD63450.dmaMFCMap[i] = HD63450.dmaMFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
         } else {
           HD63450.dmaMFCMap[i] = HD63450.dmaMFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
         }
         return;
       case HD63450.DMA_CPR:
         HD63450.dmaCP[i] = d & HD63450.DMA_CP;
         return;
       case HD63450.DMA_DFC:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDFC[i] = d & HD63450.DMA_FC2;
         if (DataBreakPoint.DBP_ON) {
           HD63450.dmaDFCMap[i] = HD63450.dmaDFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
         } else {
           HD63450.dmaDFCMap[i] = HD63450.dmaDFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
         }
         return;
       case HD63450.DMA_BFC:
         HD63450.dmaBFC[i] = d & HD63450.DMA_FC2;
         if (DataBreakPoint.DBP_ON) {
           HD63450.dmaBFCMap[i] = HD63450.dmaBFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
         } else {
           HD63450.dmaBFCMap[i] = HD63450.dmaBFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
         }
         return;
       default:
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int al = a & 255;
       int i = al >> 6;  //チャンネル
       if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
         System.out.printf ("%d %08x HD63450.dmaWw(0x%08x,0x%04x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
       }
       switch (al & 0x3f) {
       case HD63450.DMA_MTC:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMTC[i] = (char) d;
         return;
       case HD63450.DMA_MAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = d << 16 | (char) HD63450.dmaMAR[i];
         return;
       case HD63450.DMA_MAR + 2:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = ~0xffff & HD63450.dmaMAR[i] | (char) d;
         return;
       case HD63450.DMA_DAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = d << 16 | (char) HD63450.dmaDAR[i];
         return;
       case HD63450.DMA_DAR + 2:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = ~0xffff & HD63450.dmaDAR[i] | (char) d;
         return;
       case HD63450.DMA_BTC:
         HD63450.dmaBTC[i] = (char) d;
         return;
       case HD63450.DMA_BAR:
         HD63450.dmaBAR[i] = d << 16 | (char) HD63450.dmaBAR[i];
         return;
       case HD63450.DMA_BAR + 2:
         HD63450.dmaBAR[i] = ~0xffff & HD63450.dmaBAR[i] | (char) d;
         return;
       }
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int al = a & 255;
       int i = al >> 6;  //チャンネル
       if (HD63450.DMA_DEBUG_TRACE != 0 && (HD63450.DMA_DEBUG_TRACE & 1 << i) != 0) {
         System.out.printf ("%d %08x HD63450.dmaWl(0x%08x,0x%08x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
       }
       switch (al & 0x3f) {
       case HD63450.DMA_MAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaMAR[i] = d;
         return;
       case HD63450.DMA_DAR:
         if (HD63450.dmaACT[i] != 0) {
           HD63450.dmaErrorExit (i, HD63450.DMA_TIMING_ERROR);
           return;
         }
         HD63450.dmaDAR[i] = d;
         return;
       case HD63450.DMA_BAR:
         HD63450.dmaBAR[i] = d;
         return;
       }
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_DMA

  //--------------------------------------------------------------------------------
  //MMD_SVS スーパーバイザ領域設定
  MMD_SVS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "スーパーバイザ領域設定" : "Supervisor Area Setting";
    }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == XEiJ.SVS_AREASET) {
         a = (0xff & d) + 1 << 13;
         XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, a);
         XEiJ.busUser ( MemoryMappedDevice.MMD_MMR, a, Math.min (0x00200000, MainMemory.mmrMemorySizeCurrent));
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_SVS

  //--------------------------------------------------------------------------------
  //MMD_MFP MFP
  MMD_MFP {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "MFP" : "MFP";  //Multi Function Peripheral
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (byte) mmdPbz (a);
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      try {
        return a == MC68901.MFP_UDR ? MC68901.mfpUdrQueueArray[MC68901.mfpUdrQueueRead] : mmdRbz (a);
      } catch (M68kException e) {
      }
      return 0;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (short) (mmdPbz (a) << 8 | mmdPbz (a + 1));
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return mmdPbz (a) << 8 | mmdPbz (a + 1);
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return mmdPwz (a) << 16 | mmdPwz (a + 2);
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case MC68901.MFP_GPIP_DATA:
         return MC68901.mfpGpipHsync | MC68901.mfpGpipRint | 0b00100000 | MC68901.mfpGpipVdisp | MC68901.mfpGpipOpmirq | MC68901.mfpGpipPower | MC68901.mfpGpipExpwon | MC68901.mfpGpipAlarm;  //GPIP5は常に1
       case MC68901.MFP_AER:
         return MC68901.mfpAer;
       case MC68901.MFP_DDR:
         return 0x00;
       case MC68901.MFP_IERA:
         return MC68901.mfpIer >>> 8;
       case MC68901.MFP_IERB:
         return 0xff & MC68901.mfpIer;
       case MC68901.MFP_IPRA:
         return ((MC68901.mfpInnerRequest[15] != MC68901.mfpInnerAcknowledged[15] ? 0b10000000 : 0) |
                 (MC68901.mfpInnerRequest[14] != MC68901.mfpInnerAcknowledged[14] ? 0b01000000 : 0) |
                 (MC68901.mfpInnerRequest[13] != MC68901.mfpInnerAcknowledged[13] ? 0b00100000 : 0) |
                 (MC68901.mfpInnerRequest[12] != MC68901.mfpInnerAcknowledged[12] ? 0b00010000 : 0) |
                 (MC68901.mfpInnerRequest[11] != MC68901.mfpInnerAcknowledged[11] ? 0b00001000 : 0) |
                 (MC68901.mfpInnerRequest[10] != MC68901.mfpInnerAcknowledged[10] ? 0b00000100 : 0) |
                 (MC68901.mfpInnerRequest[ 9] != MC68901.mfpInnerAcknowledged[ 9] ? 0b00000010 : 0) |
                 (MC68901.mfpInnerRequest[ 8] != MC68901.mfpInnerAcknowledged[ 8] ? 0b00000001 : 0));
       case MC68901.MFP_IPRB:
         return ((MC68901.mfpInnerRequest[ 7] != MC68901.mfpInnerAcknowledged[ 7] ? 0b10000000 : 0) |
                 (MC68901.mfpInnerRequest[ 6] != MC68901.mfpInnerAcknowledged[ 6] ? 0b01000000 : 0) |
                 (MC68901.mfpInnerRequest[ 5] != MC68901.mfpInnerAcknowledged[ 5] ? 0b00100000 : 0) |
                 (MC68901.mfpInnerRequest[ 4] != MC68901.mfpInnerAcknowledged[ 4] ? 0b00010000 : 0) |
                 (MC68901.mfpInnerRequest[ 3] != MC68901.mfpInnerAcknowledged[ 3] ? 0b00001000 : 0) |
                 (MC68901.mfpInnerRequest[ 2] != MC68901.mfpInnerAcknowledged[ 2] ? 0b00000100 : 0) |
                 (MC68901.mfpInnerRequest[ 1] != MC68901.mfpInnerAcknowledged[ 1] ? 0b00000010 : 0) |
                 (MC68901.mfpInnerRequest[ 0] != MC68901.mfpInnerAcknowledged[ 0] ? 0b00000001 : 0));
       case MC68901.MFP_ISRA:
         return ((MC68901.mfpInnerInService[15] ? 0b10000000 : 0) |
                 (MC68901.mfpInnerInService[14] ? 0b01000000 : 0) |
                 (MC68901.mfpInnerInService[13] ? 0b00100000 : 0) |
                 (MC68901.mfpInnerInService[12] ? 0b00010000 : 0) |
                 (MC68901.mfpInnerInService[11] ? 0b00001000 : 0) |
                 (MC68901.mfpInnerInService[10] ? 0b00000100 : 0) |
                 (MC68901.mfpInnerInService[ 9] ? 0b00000010 : 0) |
                 (MC68901.mfpInnerInService[ 8] ? 0b00000001 : 0));
       case MC68901.MFP_ISRB:
         return ((MC68901.mfpInnerInService[ 7] ? 0b10000000 : 0) |
                 (MC68901.mfpInnerInService[ 6] ? 0b01000000 : 0) |
                 (MC68901.mfpInnerInService[ 5] ? 0b00100000 : 0) |
                 (MC68901.mfpInnerInService[ 4] ? 0b00010000 : 0) |
                 (MC68901.mfpInnerInService[ 3] ? 0b00001000 : 0) |
                 (MC68901.mfpInnerInService[ 2] ? 0b00000100 : 0) |
                 (MC68901.mfpInnerInService[ 1] ? 0b00000010 : 0) |
                 (MC68901.mfpInnerInService[ 0] ? 0b00000001 : 0));
       case MC68901.MFP_IMRA:
         return MC68901.mfpImr >>> 8;
       case MC68901.MFP_IMRB:
         return MC68901.mfpImr & 255;
       case MC68901.MFP_VECTOR:
         return MC68901.mfpVectorHigh;
       case MC68901.MFP_TACR:
         return (MC68901.mfpTaEventcount ? 0x08 : 0) | MC68901.mfpTaPrescale;
       case MC68901.MFP_TBCR:
         return 0x01;  //固定
       case MC68901.MFP_TCDCR:
         return MC68901.mfpTcPrescale << 4 | MC68901.mfpTdPrescale;
       case MC68901.MFP_TADR:
         if (MC68901.mfpTaEventcount || MC68901.mfpTaPrescale == 0) {
           return 0xff & MC68901.mfpTaCurrent;
         }
         return MC68901.mfpTaInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTaStart) / MC68901.mfpTaDelta) % MC68901.mfpTaInitial) & 255;
       case MC68901.MFP_TBDR:
         return 0;  //乱数にしたほうがいいかも
       case MC68901.MFP_TCDR:
         if (MC68901.mfpTcPrescale == 0) {
           return MC68901.mfpTcCurrent & 255;
         }
         return MC68901.mfpTcInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTcStart) / MC68901.mfpTcDelta) % MC68901.mfpTcInitial) & 255;
       case MC68901.MFP_TDDR:
         if (MC68901.mfpTdPrescale == 0) {
           return MC68901.mfpTdCurrent & 255;
         }
         return MC68901.mfpTdInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTdStart) / MC68901.mfpTdDelta) % MC68901.mfpTdInitial) & 255;
       case MC68901.MFP_SYNC_CHAR:
         return 0;
       case MC68901.MFP_UCR:
         return 0;
       case MC68901.MFP_RSR:
         if (MC68901.MFP_KBD_ON) {
           return MC68901.mfpKbdReadPointer != MC68901.mfpKbdWritePointer ? 0x80 : 0;
         } else {
           return MC68901.mfpUdrQueueRead != MC68901.mfpUdrQueueWrite ? 0x80 : 0;
         }
       case MC68901.MFP_TSR:
         return 0x80;  //バッファエンプティ
       case MC68901.MFP_UDR:
         if (MC68901.MFP_KBD_ON) {
           return MC68901.mfpKbdReadData ();
         } else {
           if (MC68901.mfpUdrQueueRead != MC68901.mfpUdrQueueWrite) {  //キューが空でないとき
             MC68901.mfpUdrQueueRead = MC68901.mfpUdrQueueRead + 1 & MC68901.MFP_UDR_QUEUE_MASK;
             if (MC68901.mfpUdrQueueRead != MC68901.mfpUdrQueueWrite) {  //キューが空にならなければ再度割り込み要求を出す
               if ((MC68901.mfpIer & MC68901.MFP_INPUT_FULL_MASK) != 0) {
                 MC68901.mfpInnerRequest[MC68901.MFP_INPUT_FULL_LEVEL]++;
                 if ((MC68901.mfpImr & MC68901.MFP_INPUT_FULL_MASK) != 0) {
                   if (MC68901.MFP_DELAYED_INTERRUPT) {
                     XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                   } else {
                     XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                   }
                 }
               }
             }
           }
           return MC68901.mfpUdrQueueArray[MC68901.mfpUdrQueueRead];  //最後に押されたまたは離されたキー
         }
       }
       return 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case MC68901.MFP_GPIP_DATA:
         return;
       case MC68901.MFP_AER:
         MC68901.mfpAer = 0xff & d;
         return;
       case MC68901.MFP_DDR:
         return;
       case MC68901.MFP_IERA:
         d = (char) (d << 8);
         int oldIera = MC68901.mfpIer;
         MC68901.mfpIer = d | 0x00ff & MC68901.mfpIer;
         //MC68901.MFP_IERAのビットに0を書き込むとMC68901.MFP_IPRAの該当ビットも0になる
         if ((short) d >= 0) {  //(0b10000000_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[15] = MC68901.mfpInnerRequest[15];
         }
         if (d << 31 - 14 >= 0) {  //(0b01000000_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[14] = MC68901.mfpInnerRequest[14];
         }
         if (d << 31 - 13 >= 0) {  //(0b00100000_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[13] = MC68901.mfpInnerRequest[13];
         }
         if (d << 31 - 12 >= 0) {  //(0b00010000_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[12] = MC68901.mfpInnerRequest[12];
         } else if (oldIera << 31 - 12 >= 0 &&  //MC68901.MFP_INPUT_FULL_MASKが0→1
                    (MC68901.MFP_KBD_ON ?
                     MC68901.mfpKbdReadPointer != MC68901.mfpKbdWritePointer :  //バッファが空でないとき
                     MC68901.mfpUdrQueueRead != MC68901.mfpUdrQueueWrite)) {  //キューが空でないとき
           MC68901.mfpInnerRequest[MC68901.MFP_INPUT_FULL_LEVEL]++;
           if ((MC68901.mfpImr & MC68901.MFP_INPUT_FULL_MASK) != 0) {
             //MFPのキー入力割り込みを要求する
             if (MC68901.MFP_DELAYED_INTERRUPT) {
               XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
             } else {
               XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
             }
           }
         }
         if (d << 31 - 11 >= 0) {  //(0b00001000_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[11] = MC68901.mfpInnerRequest[11];
         }
         if (d << 31 - 10 >= 0) {  //(0b00000100_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[10] = MC68901.mfpInnerRequest[10];
         }
         if (d << 31 - 9 >= 0) {  //(0b00000010_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 9] = MC68901.mfpInnerRequest[ 9];
         }
         if (d << 31 - 8 >= 0) {  //(0b00000001_00000000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 8] = MC68901.mfpInnerRequest[ 8];
         }
         return;
       case MC68901.MFP_IERB:
         MC68901.mfpIer = ~0xff & MC68901.mfpIer | 0xff & d;
         //MC68901.MFP_IERBのビットに0を書き込むとMC68901.MFP_IPRBの該当ビットも0になる
         if ((byte) d >= 0) {  //(0b10000000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 7] = MC68901.mfpInnerRequest[ 7];
         }
         if (d << 31 - 6 >= 0) {  //(0b01000000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 6] = MC68901.mfpInnerRequest[ 6];
         }
         if (d << 31 - 5 >= 0) {  //(0b00100000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 5] = MC68901.mfpInnerRequest[ 5];
         }
         if (d << 31 - 4 >= 0) {  //(0b00010000 & d) == 0
           MC68901.mfpInnerAcknowledged[ 4] = MC68901.mfpInnerRequest[ 4];
         }
         if (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 >= 0 : (d & 8) == 0) {
           MC68901.mfpInnerAcknowledged[ 3] = MC68901.mfpInnerRequest[ 3];
         }
         if (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 >= 0 : (d & 4) == 0) {
           MC68901.mfpInnerAcknowledged[ 2] = MC68901.mfpInnerRequest[ 2];
         }
         if (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 >= 0 : (d & 2) == 0) {
           MC68901.mfpInnerAcknowledged[ 1] = MC68901.mfpInnerRequest[ 1];
         }
         if (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 >= 0 : (d & 1) == 0) {
           MC68901.mfpInnerAcknowledged[ 0] = MC68901.mfpInnerRequest[ 0];
         }
         return;
       case MC68901.MFP_IPRA:
         //MC68901.MFP_IPRAのビットは他のすべてのビットに1を書き込むことで直接クリアできる
         switch (0xff & d) {
         case 0b01111111:
           MC68901.mfpInnerAcknowledged[15] = MC68901.mfpInnerRequest[15];
           break;
         case 0b10111111:
           MC68901.mfpInnerAcknowledged[14] = MC68901.mfpInnerRequest[14];
           break;
         case 0b11011111:
           MC68901.mfpInnerAcknowledged[13] = MC68901.mfpInnerRequest[13];
           break;
         case 0b11101111:
           MC68901.mfpInnerAcknowledged[12] = MC68901.mfpInnerRequest[12];
           break;
         case 0b11110111:
           MC68901.mfpInnerAcknowledged[11] = MC68901.mfpInnerRequest[11];
           break;
         case 0b11111011:
           MC68901.mfpInnerAcknowledged[10] = MC68901.mfpInnerRequest[10];
           break;
         case 0b11111101:
           MC68901.mfpInnerAcknowledged[ 9] = MC68901.mfpInnerRequest[ 9];
           break;
         case 0b11111110:
           MC68901.mfpInnerAcknowledged[ 8] = MC68901.mfpInnerRequest[ 8];
           break;
         }
         return;
       case MC68901.MFP_IPRB:
         //MC68901.MFP_IPRBのビットは他のすべてのビットに1を書き込むことで直接クリアできる
         switch (0xff & d) {
         case 0b01111111:
           MC68901.mfpInnerAcknowledged[ 7] = MC68901.mfpInnerRequest[ 7];
           break;
         case 0b10111111:
           MC68901.mfpInnerAcknowledged[ 6] = MC68901.mfpInnerRequest[ 6];
           break;
         case 0b11011111:
           MC68901.mfpInnerAcknowledged[ 5] = MC68901.mfpInnerRequest[ 5];
           break;
         case 0b11101111:
           MC68901.mfpInnerAcknowledged[ 4] = MC68901.mfpInnerRequest[ 4];
           break;
         case 0b11110111:
           MC68901.mfpInnerAcknowledged[ 3] = MC68901.mfpInnerRequest[ 3];
           break;
         case 0b11111011:
           MC68901.mfpInnerAcknowledged[ 2] = MC68901.mfpInnerRequest[ 2];
           break;
         case 0b11111101:
           MC68901.mfpInnerAcknowledged[ 1] = MC68901.mfpInnerRequest[ 1];
           break;
         case 0b11111110:
           MC68901.mfpInnerAcknowledged[ 0] = MC68901.mfpInnerRequest[ 0];
           break;
         }
         return;
       case MC68901.MFP_ISRA:
         //MC68901.MFP_ISRAのビットは他のすべてのビットに1を書き込むことで直接クリアできる
         switch (0xff & d) {
         case 0b01111111:
           MC68901.mfpInnerInService[15] = false;
           break;
         case 0b10111111:
           MC68901.mfpInnerInService[14] = false;
           break;
         case 0b11011111:
           MC68901.mfpInnerInService[13] = false;
           break;
         case 0b11101111:
           MC68901.mfpInnerInService[12] = false;
           break;
         case 0b11110111:
           MC68901.mfpInnerInService[11] = false;
           break;
         case 0b11111011:
           MC68901.mfpInnerInService[10] = false;
           break;
         case 0b11111101:
           MC68901.mfpInnerInService[ 9] = false;
           break;
         case 0b11111110:
           MC68901.mfpInnerInService[ 8] = false;
           break;
         }
         return;
       case MC68901.MFP_ISRB:
         //MC68901.MFP_ISRBのビットは他のすべてのビットに1を書き込むことで直接クリアできる
         switch (0xff & d) {
         case 0b01111111:
           MC68901.mfpInnerInService[ 7] = false;
           break;
         case 0b10111111:
           MC68901.mfpInnerInService[ 6] = false;
           break;
         case 0b11011111:
           MC68901.mfpInnerInService[ 5] = false;
           break;
         case 0b11101111:
           MC68901.mfpInnerInService[ 4] = false;
           break;
         case 0b11110111:
           MC68901.mfpInnerInService[ 3] = false;
           break;
         case 0b11111011:
           MC68901.mfpInnerInService[ 2] = false;
           break;
         case 0b11111101:
           MC68901.mfpInnerInService[ 1] = false;
           break;
         case 0b11111110:
           MC68901.mfpInnerInService[ 0] = false;
           break;
         }
         return;
       case MC68901.MFP_IMRA:
         MC68901.mfpImr = (0xff & d) << 8 | 0xff & MC68901.mfpImr;
         //MC68901.MFP_IMRAのビットに1を書き込んだときMC68901.MFP_IPRAの該当ビットが1ならば割り込み発生
         if ((byte) d < 0 && MC68901.mfpInnerRequest[15] != MC68901.mfpInnerAcknowledged[15] ||  //(0b10000000 & d) != 0
             d << 31 - 6 < 0 && MC68901.mfpInnerRequest[14] != MC68901.mfpInnerAcknowledged[14] ||  //(0b01000000 & d) != 0
             d << 31 - 5 < 0 && MC68901.mfpInnerRequest[13] != MC68901.mfpInnerAcknowledged[13] ||  //(0b00100000 & d) != 0
             d << 31 - 4 < 0 && MC68901.mfpInnerRequest[12] != MC68901.mfpInnerAcknowledged[12] ||  //(0b00010000 & d) != 0
             (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0) && MC68901.mfpInnerRequest[11] != MC68901.mfpInnerAcknowledged[11] ||
             (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0) && MC68901.mfpInnerRequest[10] != MC68901.mfpInnerAcknowledged[10] ||
             (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0) && MC68901.mfpInnerRequest[ 9] != MC68901.mfpInnerAcknowledged[ 9] ||
             (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0) && MC68901.mfpInnerRequest[ 8] != MC68901.mfpInnerAcknowledged[ 8]) {
           if (MC68901.MFP_DELAYED_INTERRUPT) {
             XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
           } else {
             XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
           }
         }
         return;
       case MC68901.MFP_IMRB:
         MC68901.mfpImr = ~0xff & MC68901.mfpImr | 0xff & d;
         //MC68901.MFP_IMRBのビットに1を書き込んだときMC68901.MFP_IPRBの該当ビットが1ならば割り込み発生
         if ((byte) d < 0 && MC68901.mfpInnerRequest[ 7] != MC68901.mfpInnerAcknowledged[ 7] ||  //(0b10000000 & d) != 0
             d << 31 - 6 < 0 && MC68901.mfpInnerRequest[ 6] != MC68901.mfpInnerAcknowledged[ 6] ||  //(0b01000000 & d) != 0
             d << 31 - 5 < 0 && MC68901.mfpInnerRequest[ 5] != MC68901.mfpInnerAcknowledged[ 5] ||  //(0b00100000 & d) != 0
             d << 31 - 4 < 0 && MC68901.mfpInnerRequest[ 4] != MC68901.mfpInnerAcknowledged[ 4] ||  //(0b00010000 & d) != 0
             (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0) && MC68901.mfpInnerRequest[ 3] != MC68901.mfpInnerAcknowledged[ 3] ||
             (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0) && MC68901.mfpInnerRequest[ 2] != MC68901.mfpInnerAcknowledged[ 2] ||
             (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0) && MC68901.mfpInnerRequest[ 1] != MC68901.mfpInnerAcknowledged[ 1] ||
             (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0) && MC68901.mfpInnerRequest[ 0] != MC68901.mfpInnerAcknowledged[ 0]) {
           if (MC68901.MFP_DELAYED_INTERRUPT) {
             XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
           } else {
             XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
           }
         }
         return;
       case MC68901.MFP_VECTOR:
         MC68901.mfpVectorHigh = 0xf0 & d;  //ビット3は0(全チャンネル自動割込み終了モード)に固定
         return;
       case MC68901.MFP_TACR:
         {
           int prevPrescale = MC68901.mfpTaPrescale;
           MC68901.mfpTaEventcount = (d & 0x08) != 0;
           MC68901.mfpTaPrescale = d & 0x07;
           if (MC68901.mfpTaEventcount && MC68901.mfpTaPrescale != 0) {  //パルス幅計測モードはキャンセル
             MC68901.mfpTaEventcount = false;
             MC68901.mfpTaPrescale = 0;
           }
           if (MC68901.mfpTaEventcount) {  //イベントカウントモード
             if (prevPrescale != 0) {  //ディレイモードで動作中だったとき
               MC68901.mfpTaCurrent = MC68901.mfpTaInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTaStart) / MC68901.mfpTaDelta) % MC68901.mfpTaInitial);
             }
             MC68901.mfpTaClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (MC68901.mfpTaTicker);
           } else if (MC68901.mfpTaPrescale != 0) {  //ディレイモード
             //! ディレイモードで動作中にプリスケールを変更されるとカウンタが即座に初期値に戻ってしまう
             MC68901.mfpTaClock = (MC68901.mfpTaStart = XEiJ.mpuClockTime) + (MC68901.mfpTaDelta = MC68901.MFP_DELTA[MC68901.mfpTaPrescale]) * MC68901.mfpTaInitial;
             TickerQueue.tkqAdd (MC68901.mfpTaTicker, MC68901.mfpTaClock);
           } else {  //カウント停止
             if (prevPrescale != 0) {  //ディレイモードで動作中だったとき
               MC68901.mfpTaCurrent = MC68901.mfpTaInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTaStart) / MC68901.mfpTaDelta) % MC68901.mfpTaInitial);
             }
             MC68901.mfpTaClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (MC68901.mfpTaTicker);
           }
         }
         return;
       case MC68901.MFP_TBCR:
         //何もしない
         return;
       case MC68901.MFP_TCDCR:
         {
           int prevPrescale = MC68901.mfpTcPrescale;
           MC68901.mfpTcPrescale = d >> 4 & 0x07;
           if (MC68901.mfpTcPrescale != 0) {  //ディレイモード
             //! ディレイモードで動作中にプリスケールを変更されるとカウンタが即座に初期値に戻ってしまう
             MC68901.mfpTcClock = (MC68901.mfpTcStart = XEiJ.mpuClockTime) + (MC68901.mfpTcDelta = MC68901.MFP_DELTA[MC68901.mfpTcPrescale]) * MC68901.mfpTcInitial;
             TickerQueue.tkqAdd (MC68901.mfpTcTicker, MC68901.mfpTcClock);
           } else {  //カウント停止
             if (prevPrescale != 0) {  //ディレイモードで動作中だったとき
               MC68901.mfpTcCurrent = MC68901.mfpTcInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTcStart) / MC68901.mfpTcDelta) % MC68901.mfpTcInitial);
             }
             MC68901.mfpTcClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (MC68901.mfpTcTicker);
           }
         }
         {
           int prevPrescale = MC68901.mfpTdPrescale;
           MC68901.mfpTdPrescale = d & 0x07;
           if (MC68901.mfpTdPrescale != 0) {  //ディレイモード
             //! ディレイモードで動作中にプリスケールを変更されるとカウンタが即座に初期値に戻ってしまう
             MC68901.mfpTdClock = (MC68901.mfpTdStart = XEiJ.mpuClockTime) + (MC68901.mfpTdDelta = MC68901.MFP_DELTA[MC68901.mfpTdPrescale]) * MC68901.mfpTdInitial;
             TickerQueue.tkqAdd (MC68901.mfpTdTicker, MC68901.mfpTdClock);
           } else {  //カウント停止
             if (prevPrescale != 0) {  //ディレイモードで動作中だったとき
               MC68901.mfpTdCurrent = MC68901.mfpTdInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - MC68901.mfpTdStart) / MC68901.mfpTdDelta) % MC68901.mfpTdInitial);
             }
             MC68901.mfpTdClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (MC68901.mfpTdTicker);
           }
         }
         return;
       case MC68901.MFP_TADR:
         //! ディレイモードで動作中に初期値を変更するとオーバーフローするまでMC68901.MFP_TADRの値がずれてしまう
         d &= 0xff;
         MC68901.mfpTaInitial = d == 0 ? 256 : d;  //初期値
         if (!MC68901.mfpTaEventcount && MC68901.mfpTaPrescale == 0) {  //動作中でなければカウンタの値も変更される
           MC68901.mfpTaCurrent = MC68901.mfpTaInitial;
         }
         return;
       case MC68901.MFP_TBDR:
         //何もしない
         return;
       case MC68901.MFP_TCDR:
         //! ディレイモードで動作中に初期値を変更するとオーバーフローするまでMC68901.MFP_TADRの値がずれてしまう
         d &= 0xff;
         MC68901.mfpTcInitial = d == 0 ? 256 : d;  //初期値
         if (MC68901.mfpTcPrescale == 0) {  //動作中でなければカウンタの値も変更される
           MC68901.mfpTcCurrent = MC68901.mfpTcInitial;
         }
         return;
       case MC68901.MFP_TDDR:
         //! ディレイモードで動作中に初期値を変更するとオーバーフローするまでMC68901.MFP_TADRの値がずれてしまう
         d &= 0xff;
         MC68901.mfpTdInitial = d == 0 ? 256 : d;  //初期値
         if (MC68901.mfpTdPrescale == 0) {  //動作中でなければカウンタの値も変更される
           MC68901.mfpTdCurrent = MC68901.mfpTdInitial;
         }
         return;
       case MC68901.MFP_SYNC_CHAR:
         return;
       case MC68901.MFP_UCR:
         return;
       case MC68901.MFP_RSR:
         return;
       case MC68901.MFP_TSR:
         return;
       case MC68901.MFP_UDR:
         if ((byte) d < 0) {  //LEDの状態
           Keyboard.kbdSetLedStatus (d);
         } else if ((d & 0xf8) == 0x40) {  //MSCTRL
         } else if ((d & 0xf8) == 0x48) {  //ロック
         } else if ((d & 0xfc) == 0x54) {  //LEDの明るさ
         } else if ((d & 0xfc) == 0x58) {  //テレビコントロール
         } else if ((d & 0xfc) == 0x5c) {  //OPT.2キーによるテレビコントロール
         } else if ((d & 0xf0) == 0x60) {  //リピート開始時間
           Keyboard.kbdSetRepeatDelay (0x0f & d);
         } else if ((d & 0xf0) == 0x70) {  //リピート間隔
           Keyboard.kbdSetRepeatInterval (0x0f & d);
         }
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_MFP

  //--------------------------------------------------------------------------------
  //MMD_RTC_FIRST RTC
  MMD_RTC_FIRST {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "RTC" : "RTC";  //Real Time Clock
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 24 | RP5C15.rtcRegCurrent[a + 1 & 31] << 16 | RP5C15.rtcRegCurrent[a + 2 & 31] << 8 | RP5C15.rtcRegCurrent[a + 3 & 31];  //&255を省略
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       return RP5C15.rtcRegCurrent[a & 31];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       return RP5C15.rtcRegCurrent[a & 31];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       return RP5C15.rtcRegCurrent[a & 31] << 24 | RP5C15.rtcRegCurrent[a + 1 & 31] << 16 | RP5C15.rtcRegCurrent[a + 2 & 31] << 8 | RP5C15.rtcRegCurrent[a + 3 & 31];  //&255を省略
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       MMD_RTC_NEXT.mmdWb (a, d);
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       MMD_RTC_NEXT.mmdWw (a, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT, 0x00e8a000, 0x00e8c000);  //RTC RTC
       //XEiJ.busSuperMap[0x00e8a000 >>> XEiJ.BUS_PAGE_BITS] = MMD_RTC_NEXT;
       if (RP5C15.rtcMove != 0) {
         RP5C15.rtcUpdate ();
       }
       MMD_RTC_NEXT.mmdWl (a, d);
     }
  },  //MMD_RTC_FIRST

  //--------------------------------------------------------------------------------
  //MMD_RTC_NEXT RTC
  MMD_RTC_NEXT {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "RTC" : "RTC";  //Real Time Clock
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31];
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return RP5C15.rtcRegCurrent[a & 31] << 24 | RP5C15.rtcRegCurrent[a + 1 & 31] << 16 | RP5C15.rtcRegCurrent[a + 2 & 31] << 8 | RP5C15.rtcRegCurrent[a + 3 & 31];  //&255を省略
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return RP5C15.rtcRegCurrent[a & 31];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return RP5C15.rtcRegCurrent[a & 31];
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       //aはページを跨がないがRP5C15.rtcRegCurrentが32バイトしかないのでa&=31;return RP5C15.rtcRegCurrent[a]<<8|RP5C15.rtcRegCurrent[a+1]とするのは不可
       return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return RP5C15.rtcRegCurrent[a & 31] << 8 | RP5C15.rtcRegCurrent[a + 1 & 31];  //&255を省略
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return RP5C15.rtcRegCurrent[a & 31] << 24 | RP5C15.rtcRegCurrent[a + 1 & 31] << 16 | RP5C15.rtcRegCurrent[a + 2 & 31] << 8 | RP5C15.rtcRegCurrent[a + 3 & 31];  //&255を省略
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       int seco, day, t;
       long mill;
       switch (RP5C15.rtcBank << 5 | a & 31) {
         //バンク0
       case 0 << 5 | RP5C15.RTC_0_SECO0:  //0x00e8a001  bit0-3  1秒カウンタ
         d &= 15;  //0..15
         seco = d - RP5C15.rtcRegBank0[RP5C15.RTC_0_SECO0];
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_SECO0] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_SECO1:  //0x00e8a003  bit0-2  10秒カウンタ
         d &= 7;  //0..7
         seco = 10 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_SECO1]);
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_SECO1] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MINU0:  //0x00e8a005  bit0-3  1分カウンタ
         d &= 15;  //0..15
         seco = 60 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_MINU0]);
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MINU0] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MINU1:  //0x00e8a007  bit0-2  10分カウンタ
         d &= 7;  //0..7
         seco = 600 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_MINU1]);
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MINU1] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_HOUR0:  //0x00e8a009  bit0-3  1時間カウンタ
         d &= 15;  //0..15
         seco = 3600 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR0]);
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR0] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_HOUR1:  //0x00e8a00b  bit0-1  10時間カウンタ
         d &= 3;  //0..3
         seco = (RP5C15.rtcRule == 0 ?
                 12 * 3600 * ((d >> 1) - (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] >> 1)) +
                 10 * 3600 * ((d & 1) - (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] & 1)) :  //12時間制
                 10 * 3600 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1]));  //24時間制
         RP5C15.rtcDsec += seco;
         mill = (long) (1000 * seco);
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_WDAY:  //0x00e8a00d  bit0-2  曜日カウンタ
         d &= 7;  //0..7
         t = RP5C15.rtcWeekGap + d - RP5C15.rtcRegBank0[RP5C15.RTC_0_WDAY] + 7;  //1..20
         //perl optdiv.pl 20 7
         //  x/7==x*19>>>7 (0<=x<=26) [20*19==380]
         RP5C15.rtcWeekGap = t - (t * 19 >>> 7) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_WDAY] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MDAY0:  //0x00e8a00f  bit0-3  1日カウンタ
         d &= 15;  //0..15
         day = d - RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0];
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 21;  //6..27
         //perl optdiv.pl 27 7
         //  x/7==x*37>>>8 (0<=x<=89) [27*37==999]
         RP5C15.rtcWeekGap = t - (t * 37 >>> 8) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MDAY1:  //0x00e8a011  bit0-1  10日カウンタ
         d &= 3;  //0..3
         day = 10 * (d - RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1]);
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 35;  //5..41
         //perl optdiv.pl 41 7
         //  x/7==x*37>>>8 (0<=x<=89) [41*37==1517]
         RP5C15.rtcWeekGap = t - (t * 37 >>> 8) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MONT0:  //0x00e8a013  bit0-3  1月カウンタ
         d &= 15;  //0..15
         day = DnT.dntCdayYearMontMday (10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR0],
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT1] + d,
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0]) - RP5C15.rtcCday;
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 469;  //0..940くらい。-31*15<=day<=31*15
         //perl optdiv.pl 940 7
         //  x/7==x*1171>>>13 (0<=x<=1643) [940*1171==1100740]
         RP5C15.rtcWeekGap = t - (t * 1171 >>> 13) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT0] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_MONT1:  //0x00e8a015  bit0  10月カウンタ
         d &= 1;  //0..1
         day = DnT.dntCdayYearMontMday (10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR0],
                                        10 * d + RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT0],
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0]) - RP5C15.rtcCday;
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 315;  //0..631くらい。-31*10<=day<=31*10
         //perl optdiv.pl 631 7
         //  x/7==x*293>>>11 (0<=x<=684) [631*293==184883]
         RP5C15.rtcWeekGap = t - (t * 293 >>> 11) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT1] = (byte) d;
         return;
       case 0 << 5 | RP5C15.RTC_0_YEAR0:  //0x00e8a017  bit0-3  1年カウンタ
         d &= 15;  //0..15
         day = DnT.dntCdayYearMontMday (10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] + d,
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT0],
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0]) - RP5C15.rtcCday;
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 5495;  //0..10991くらい。-366*15<=day<=366*15
         //perl optdiv.pl 10991 7
         //  x/7==x*9363>>>16 (0<=x<=13109) [10991*9363==102908733]
         RP5C15.rtcWeekGap = t - (t * 9363 >>> 16) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR0] = (byte) d;
         RP5C15.rtcRegBank0[RP5C15.RTC_1_LEAP] = (byte) (10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] + d & 3);
         return;
       case 0 << 5 | RP5C15.RTC_0_YEAR1:  //0x00e8a019  bit0-3  10年カウンタ
         d &= 15;  //0..15
         day = DnT.dntCdayYearMontMday (10 * d + RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR0],
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MONT0],
                                        10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY1] + RP5C15.rtcRegBank0[RP5C15.RTC_0_MDAY0]) - RP5C15.rtcCday;
         RP5C15.rtcCday += day;
         mill = 86400000L * (long) day;
         RP5C15.rtcCmil += mill;
         RP5C15.rtcCmilGap += mill;
         t = RP5C15.rtcWeekGap - day + 54901;  //0..109807くらい。-366*150<=day<=366*150
         //perl optdiv.pl 109807 7
         //  x/7==x*149797>>>20 (0<=x<=349529) [109807*149797==16448759179]
         RP5C15.rtcWeekGap = t - (int) ((long) t * 149797L >>> 20) * 7;  //0..6
         RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] = (byte) d;
         RP5C15.rtcRegBank0[RP5C15.RTC_1_LEAP] = (byte) (10 * RP5C15.rtcRegBank0[RP5C15.RTC_0_YEAR1] + d & 3);
         return;
         //バンク1
       case 1 << 5 | RP5C15.RTC_1_CLKOUT:  //0x00e8a001  bit0-2  CLKOUTセレクタ
         //! 未対応。TIMER-LED
         return;
       case 1 << 5 | RP5C15.RTC_1_ADJUST:  //0x00e8a003  bit0    アジャスト
         //! 未対応。アジャスト
         return;
         //case 1 << 5 | RP5C15.RTC_1_MINU0:  //0x00e8a005  bit0-3  アラーム1分レジスタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_MINU1:  //0x00e8a007  bit0-2  アラーム10分レジスタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_HOUR0:  //0x00e8a009  bit0-3  アラーム1時間レジスタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_HOUR1:  //0x00e8a00b  bit0-2  アラーム10時間レジスタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_WDAY:  //0x00e8a00d  bit0-2  アラーム曜日レジスタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_MDAY0:  //0x00e8a00f  bit0-3  アラーム1日カウンタ
         //  return;
         //case 1 << 5 | RP5C15.RTC_1_MDAY1:  //0x00e8a011  bit0-1  アラーム10日カウンタ
         //  return;
       case 1 << 5 | RP5C15.RTC_1_RULE:  //0x00e8a015  bit0    12時間制/24時間制セレクタ
         d &= 1;
         if (RP5C15.rtcRule != d) {
           //10時間カウンタの値を変えずに解釈を変更する
           seco = ((d == 0 ?
                    12 * 3600 * (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] >> 1) + 10 * (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] & 1) :  //12時間制
                    10 * 3600 * RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1]) -  //24時間制
                   (RP5C15.rtcRule == 0 ?
                    12 * 3600 * (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] >> 1) + 10 * (RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1] & 1) :  //12時間制
                    10 * 3600 * RP5C15.rtcRegBank0[RP5C15.RTC_0_HOUR1]));  //24時間制
           RP5C15.rtcRule = d;
           RP5C15.rtcDsec += seco;
           mill = (long) (1000 * seco);
           RP5C15.rtcCmil += mill;
           RP5C15.rtcCmilGap += mill;
           RP5C15.rtcRegBank1[RP5C15.RTC_1_RULE] = (byte) d;
         }
         return;
       case 1 << 5 | RP5C15.RTC_1_LEAP:  //0x00e8a017  bit0-1  閏年カウンタ
         //何もしない
         return;
         //共通
       case 0 << 5 | RP5C15.RTC_MODE:  //0x00e8a01b  モードレジスタ
       case 1 << 5 | RP5C15.RTC_MODE:  //0x00e8a01b  モードレジスタ
         d &= 13;
         RP5C15.rtcBank = d & 1;
         RP5C15.rtcRegCurrent = RP5C15.rtcBank == 0 ? RP5C15.rtcRegBank0 : RP5C15.rtcRegBank1;
         if (RP5C15.rtcMove < (d & 8)) {  //停止→動作
           RP5C15.rtcCmilGap = RP5C15.rtcCmil - System.currentTimeMillis ();
         }
         RP5C15.rtcMove = d & 8;
         RP5C15.rtcRegBank0[RP5C15.RTC_MODE] = RP5C15.rtcRegBank1[RP5C15.RTC_MODE] = (byte) d;
         return;
         //case 0 << 5 | RP5C15.RTC_TEST:  //0x00e8a01d  テストレジスタ
         //case 1 << 5 | RP5C15.RTC_TEST:  //0x00e8a01d  テストレジスタ
         //  return 0;
         //case 0 << 5 | RP5C15.RTC_RESET:  //0x00e8a01f  リセットコントローラ
         //case 1 << 5 | RP5C15.RTC_RESET:  //0x00e8a01f  リセットコントローラ
         //  return 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if ((a & 1) == 0) {
         mmdWb (a + 1, d);
       } else {
         mmdWb (a, d >> 8);
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if ((a & 1) == 0) {
         mmdWb (a + 1, d >> 16);
         mmdWb (a + 3, d);
       } else {
         mmdWb (a, d >> 24);
         mmdWb (a + 2, d >> 8);
       }
     }
  },  //MMD_RTC_NEXT

  //--------------------------------------------------------------------------------
  //MMD_PRN プリンタポート
  MMD_PRN {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "プリンタポート" : "Printer Port";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (a == PrinterPort.PRN_DATA ? PrinterPort.prnReadData () :
               a == PrinterPort.PRN_STROBE ? PrinterPort.prnReadStrobe () :
               0);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == PrinterPort.PRN_DATA) {
         PrinterPort.prnWriteData (d);
       } else if (a == PrinterPort.PRN_STROBE) {
         PrinterPort.prnWriteStrobe (d);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_PRN

  //--------------------------------------------------------------------------------
  //MMD_SYS システムポート
  MMD_SYS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "システムポート" : "System Port";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       switch (a & 15) {
       case 0x01:
         return 0b11110000 | VideoController.vcnTargetContrastPort;
       case 0x03:
         return 0b11111000;
       case 0x07:
         return 0b11111100 | CRTC.crtHRLPort << 1;
       case 0x0b:
         return XEiJ.mpuCoreType == 0 ? XEiJ.mpuClockMHz <= 10.0 ? 0xff : 0xfe : 0xdc;
       }
       return 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       switch (a & 15) {
       case 0x01:
         VideoController.vcnTargetContrastPort = d & 15;
         {
           int curr = VideoController.vcnTargetContrastMask == 0 ? VideoController.vcnTargetContrastPort : VideoController.vcnTargetContrastTest;
           if (VideoController.vcnTargetContrastCurr != curr) {
             VideoController.vcnTargetContrastCurr = curr;
             VideoController.vcnTargetScaledContrast = VideoController.VCN_CONTRAST_SCALE * VideoController.vcnTargetContrastCurr;
             CRTC.crtContrastClock = XEiJ.mpuClockTime;
             CRTC.crtFrameTaskClock = Math.min (CRTC.crtContrastClock, CRTC.crtCaptureClock);
           }
         }
         return;
       case 0x07:
         {
           CRTC.crtHRLPort = d >> 1 & 1;
           int curr = CRTC.crtHRLMask == 0 ? CRTC.crtHRLPort : CRTC.crtHRLTest;
           if (CRTC.crtHRLCurr != curr) {
             CRTC.crtHRLCurr = curr;
             CRTC.crtRestart ();
           }
           if ((d & 1 << 2) != 0) {
             XEiJ.sysResetNMI ();  //NMIリセット
           }
         }
         return;
       case 0x0d:
         XEiJ.smrWriteEnableOn = d == 0x31;
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_SYS

  //--------------------------------------------------------------------------------
  //MMD_OPM FM音源
  MMD_OPM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "FM 音源" : "FM Sound Generator";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (a & 0x03) == 3 ? (XEiJ.mpuClockTime < YM2151.opmBusyClock ? 0x80 : 0x00) | YM2151.opmISTB | YM2151.opmISTA : 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       a &= 3;
       if (a == 3) {  //データレジスタ
         //ビジーフラグをセットする
         //  データレジスタに書き込んでからビジーフラグがセットされるまでのタイムラグを再現することもできなくはないが、
         //  実機をクロックアップすると動かなくなるソフトが実機と同じように動かなくなるだけのような気がするのでやめておく
         YM2151.opmBusyClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / YM2151.OPM_OSC_FREQ * 68;
         //除算の負荷を軽減するためYM2151.opmPointerを進める必要があるかどうかを先に確認する
         if (SoundSource.sndBlockClock - SoundSource.SND_BLOCK_TIME + YM2151.OPM_SAMPLE_TIME / SoundSource.SND_CHANNELS * YM2151.opmPointer < XEiJ.mpuClockTime) {
           //現在のブロックの残り時間が1サンプルの時間の倍数になるように切り上げる
           YM2151.opmUpdate (SoundSource.SND_CHANNELS * (YM2151.OPM_BLOCK_SAMPLES - Math.max (0, (int) ((double) (SoundSource.sndBlockClock - XEiJ.mpuClockTime) / (double) YM2151.OPM_SAMPLE_TIME))));
         }
         YM2151.opmSetData (YM2151.opmAddress, d);
       } else if (a == 1) {  //アドレスレジスタ
         //アドレスレジスタへの書き込みではビジーフラグをセットしない
         //! 未対応。X68030のときアドレスレジスタへの書き込みに限って約1.2μsのウェイトを追加する
         //  http://retropc.net/x68000/software/hardware/060turbo/060opmp/060opmp.htm
         YM2151.opmAddress = 0xff & d;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_OPM

  //--------------------------------------------------------------------------------
  //MMD_PCM ADPCM音源
  MMD_PCM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ADPCM 音源" : "ADPCM Sound Generator";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (a & 0x03) == 1 ? (ADPCM.pcmActive ? 0b10000000 : 0) | 0x40 : 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       a &= 3;
       if (a == 1) {  //コマンド
         if ((d & 0b00000001) != 0) {  //動作終了
           if (ADPCM.pcmActive) {  //動作中
             ADPCM.pcmClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (SoundSource.sndPcmTicker);
             ADPCM.pcmActive = false;
             ADPCM.pcmEncodedData = -1;
             ADPCM.pcmDecoderPointer = 0;
             HD63450.dmaRisePCL (3);
           }
         } else if ((d & 0b00000010) != 0) {  //動作開始
           if (!ADPCM.pcmActive) {  //停止中
             //現在のブロックの残り時間が1サンプルの時間の倍数になるように切り上げる
             int remainingSamples = Math.max (0, (int) ((double) (SoundSource.sndBlockClock - XEiJ.mpuClockTime) / (double) ADPCM.PCM_SAMPLE_TIME));  //現在のブロックの残りサンプル数
             ADPCM.pcmClock = SoundSource.sndBlockClock - ADPCM.PCM_SAMPLE_TIME * (long) remainingSamples;  //書き込み開始時刻
             TickerQueue.tkqAdd (SoundSource.sndPcmTicker, ADPCM.pcmClock);
             ADPCM.pcmActive = true;
             int newPointer = SoundSource.SND_CHANNELS * (ADPCM.PCM_BLOCK_SAMPLES - remainingSamples);  //書き込み開始位置
             if (ADPCM.pcmPointer < newPointer) {
               ADPCM.pcmFillBuffer (newPointer);
             } else {
               ADPCM.pcmPointer = newPointer;  //少し戻る場合がある
             }
             //DMAに最初のデータを要求する
             HD63450.dmaFallPCL (3);
           }
           //} else if ((d & 0b00000100) != 0) {  //録音開始
           //! 非対応
         }
       } else if (a == 3) {  //データ
         if (ADPCM.pcmActive) {
           ADPCM.pcmEncodedData = d & 255;
           HD63450.dmaRisePCL (3);
         }
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_PCM

  //--------------------------------------------------------------------------------
  //MMD_FDC FDコントローラ
  MMD_FDC {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "FD コントローラ" : "FD Controller";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return (byte) mmdPbz (a);
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK & ~1;  //bit0を無視する
      switch (a) {
      case FDC.FDC_STATUS_PORT & ~1:  //0x00e94001
        return FDC.fdcPeekStatus ();
      case FDC.FDC_DATA_PORT & ~1:  //0x00e94003
        return FDC.fdcPeekData ();
      case FDC.FDC_DRIVE_STATUS & ~1:  //0x00e94005
        return FDC.fdcPeekDriveStatus ();
      }
      return 0xff;
    }
    @Override protected int mmdPws (int a) {
      return (short) (mmdPbz (a) << 8 | mmdPbz (a + 1));
    }
    @Override protected int mmdPwz (int a) {
      return mmdPbz (a) << 8 | mmdPbz (a + 1);
    }
    @Override protected int mmdPls (int a) {
      return mmdPwz (a) << 16 | mmdPwz (a + 2);
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK & ~1;  //bit0を無視する
       switch (a) {
       case FDC.FDC_STATUS_PORT & ~1:  //0x00e94001
         return FDC.fdcReadStatus ();
       case FDC.FDC_DATA_PORT & ~1:  //0x00e94003
         return FDC.fdcReadData ();
       case FDC.FDC_DRIVE_STATUS & ~1:  //0x00e94005
         return FDC.fdcReadDriveStatus ();
       }
       return 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK & ~1;  //bit0を無視する
       switch (a) {
       case FDC.FDC_STATUS_PORT & ~1:  //0x00e94001
         FDC.fdcWriteCommand (d);
         break;
       case FDC.FDC_DATA_PORT & ~1:  //0x00e94003
         FDC.fdcWriteData (d);
         break;
       case FDC.FDC_DRIVE_STATUS & ~1:  //0x00e94005
         FDC.fdcWriteDriveControl (d);
         break;
       case FDC.FDC_DRIVE_SELECT & ~1:  //0x00e94007
         FDC.fdcWriteDriveSelect (d);
         break;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_FDC

  //--------------------------------------------------------------------------------
  //MMD_HDC SASI HDコントローラ
  MMD_HDC {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "内蔵 SASI/SCSI ポート" : "Internal SASI/SCSI Port";  //SCSIのIはInterfaceなのだからSCSIインタフェイスはおかしい
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      //内蔵SCSIを持つ機種でもSASIポートがないと起動できない
      switch (a) {
      case HDC.HDC_DATA_PORT:  //0x00e96001
        return (byte) HDC.hdcPeekData ();
      case HDC.HDC_STATUS_PORT:  //0x00e96003
        return (byte) HDC.hdcPeekStatus ();
      case HDC.HDC_RESET_PORT:  //0x00e96005
        return 0;
      case HDC.HDC_SELECTION_PORT:  //0x00e96007
        return 0;
      }
      if (SPC.spcSCSIINOn) {
        if ((a & -32) == SPC.SPC_BASE_IN) {
          return (byte) SPC.spcSCSIINChip.spiPeek (a);
        }
      }
      return 0;
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      //内蔵SCSIを持つ機種でもSASIポートがないと起動できない
      switch (a) {
      case HDC.HDC_DATA_PORT:  //0x00e96001
        return HDC.hdcPeekData ();
      case HDC.HDC_STATUS_PORT:  //0x00e96003
        return HDC.hdcPeekStatus ();
      case HDC.HDC_RESET_PORT:  //0x00e96005
        return 0;
      case HDC.HDC_SELECTION_PORT:  //0x00e96007
        return 0;
      }
      if (SPC.spcSCSIINOn) {
        if ((a & -32) == SPC.SPC_BASE_IN) {
          return SPC.spcSCSIINChip.spiPeek (a);
        }
      }
      return 0;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return (short) (mmdPbz (a) << 8 | mmdPbz (a + 1));
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return mmdPbz (a) << 8 | mmdPbz (a + 1);
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return mmdPwz (a) << 16 | mmdPwz (a + 2);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       //内蔵SCSIを持つ機種でもSASIポートがないと起動できない
       switch (a) {
       case HDC.HDC_DATA_PORT:  //0x00e96001
         return (byte) HDC.hdcReadData ();
       case HDC.HDC_STATUS_PORT:  //0x00e96003
         return (byte) HDC.hdcReadStatus ();
       case HDC.HDC_RESET_PORT:  //0x00e96005
         return 0;
       case HDC.HDC_SELECTION_PORT:  //0x00e96007
         return 0;
       }
       if (SPC.spcSCSIINOn) {
         if ((a & -32) == SPC.SPC_BASE_IN) {
           return (byte) SPC.spcSCSIINChip.spiRead (a);
         }
       }
       return super.mmdRbs (a);  //バスエラー
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       //内蔵SCSIを持つ機種でもSASIポートがないと起動できない
       switch (a) {
       case HDC.HDC_DATA_PORT:  //0x00e96001
         return HDC.hdcReadData ();
       case HDC.HDC_STATUS_PORT:  //0x00e96003
         return HDC.hdcReadStatus ();
       case HDC.HDC_RESET_PORT:  //0x00e96005
         return 0;
       case HDC.HDC_SELECTION_PORT:  //0x00e96007
         return 0;
       }
       if (SPC.spcSCSIINOn) {
         if ((a & -32) == SPC.SPC_BASE_IN) {
           return SPC.spcSCSIINChip.spiRead (a);
         }
       }
       return super.mmdRbz (a);  //バスエラー
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       //内蔵SCSIを持つ機種でもSASIポートがないと起動できない
       switch (a) {
       case HDC.HDC_DATA_PORT:  //0x00e96001
         HDC.hdcWriteData (d);
         return;
       case HDC.HDC_STATUS_PORT:  //0x00e96003
         HDC.hdcWriteCommand (d);
         return;
       case HDC.HDC_RESET_PORT:  //0x00e96005
         HDC.hdcWriteReset (d);
         return;
       case HDC.HDC_SELECTION_PORT:  //0x00e96007
         HDC.hdcWriteSelect (d);
         return;
       }
       if (SPC.spcSCSIINOn) {
         if ((a & -32) == SPC.SPC_BASE_IN) {
           SPC.spcSCSIINChip.spiWrite (a, d);
           return;
         }
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_HDC

  //--------------------------------------------------------------------------------
  //MMD_SCC SCC
  //  アドレスは下位3bitだけデコードされる。0x00e98008以降は0x00e98000～0x00e98007の繰り返し
  //  偶数アドレスをバイトサイズでアクセスするとバスエラーになる
  //  ワードリードはバスエラーにならず0xffxxが返る
  //  ロングリードは0xffxxffxxが返る
  MMD_SCC {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "SCC" : "SCC";  //Serial Communication Controller
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      try {
        //! 未対応
        return mmdRbs (a);
      } catch (M68kException e) {
      }
      return -1;
    }
    @Override protected int mmdPbz (int a) {
      try {
        //! 未対応
        return mmdRbz (a);
      } catch (M68kException e) {
      }
      return 255;
    }
    @Override protected int mmdPws (int a) {
      return 0xffffff00 | mmdPbz (a + 1);
    }
    @Override protected int mmdPwz (int a) {
      return 0xff00 | mmdPbz (a + 1);
    }
    @Override protected int mmdPls (int a) {
      return 0xff00ff00 | mmdPbz (a + 1) << 16 | mmdPbz (a + 3);
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       int d = 0;
       switch (a & 7) {
         //------------------------------------------------
       case Z8530.SCC_0_COMMAND & 7:  //ポートBコマンド読み出し
         switch (Z8530.scc0RegisterNumber) {
         case 0:  //RR0
           //  0x80  ブレークまたはアボート
           //  0x40  送信アンダーラン
           //  0x20  CTS(0=送信禁止,1=送信許可)
           //  0x10  SYNC
           //  0x08  DCD
           //  0x04  送信バッファ空
           //  0x02  ボーレートカウント0
           //  0x01  受信バッファフル
           d = Z8530.scc0InputCounter < 3 ? 0x25 : 0x24;
           break;
         case 2:  //RR2
           //修飾割り込みベクタ
           //  ポートBのRR2はWR2に設定したベクタを割り込み要求で加工して返す
           d = (Z8530.scc1aReceiveRequest != 0 ? Z8530.scc1aReceiveVector :  //1A受信バッファフル(RS-232C受信)
                Z8530.scc1aSendRequest != 0 ? Z8530.scc1aSendVector :  //1A送信バッファ空(RS-232C送信)
                Z8530.scc0bReceiveRequest != 0 ? Z8530.scc0bReceiveVector :  //0B受信バッファフル(マウス受信)
                Z8530.sccInterruptVector);
           break;
         case 3:  //RR3
           //ポートBのRR3は常に0
           //  ポートBの割り込みペンディングを見るときはポートAのRR3を参照する
           //d = 0;
           break;
         case 12:  //RR12
           d = Z8530.scc0BaudRateGen & 255;
           break;
         case 13:  //RR13
           d = Z8530.scc0BaudRateGen >> 8 & 255;
           break;
         default:
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.println ("unimplemented register");
           }
         }
         Z8530.scc0RegisterNumber = 0;
         break;
         //------------------------------------------------
       case Z8530.SCC_0_DATA & 7:  //ポートBデータ読み出し(マウス受信)
         if (Z8530.scc0InputCounter == 0) {  //1バイト目
           d = XEiJ.musDataRight | XEiJ.musDataLeft;
           if (!XEiJ.musOnScreen) {  //ホストのマウスカーソルがスクリーン上にない
             //XEiJ.musShow ();
             if (XEiJ.musCursorNumber != 1 && XEiJ.musCursorAvailable) {
               XEiJ.musCursorNumber = 1;
               XEiJ.pnlPanel.setCursor (XEiJ.musCursorArray[1]);  //ホストのマウスカーソルを表示する
             }
             Z8530.scc0Data1 = Z8530.scc0Data2 = 0;
           } else if (XEiJ.musSeamlessOn) {  //シームレス
             int on, dx, dy, coeff = 256;
             if (XEiJ.mpuCoreType < 6) {  //MMUなし
               if (Z8530.SCC_FSX_MOUSE &&
                   Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
                   MainMemory.mmrRls (0x0938) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
                 on = MainMemory.mmrRws (Z8530.sccFSXMouseWork + 0x2e) == 0 ? 1 : 0;  //SX-Windowのマウスカーソルの表示状態。Obscureのときは表示されているとみなす
                 int xy = MainMemory.mmrRls (Z8530.sccFSXMouseWork + 0x0a);
                 dx = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
                 dy = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
                 coeff = MainMemory.mmrRwz (Z8530.sccFSXMouseWork + 0x04);  //ポインタの移動量。係数*256
               } else {  //SX-Windowが動作中ではない
                 on = MainMemory.mmrRbs (0x0aa2);  //IOCSのマウスカーソルの表示状態
                 int xy = MainMemory.mmrRls (0x0ace);
                 dx = xy >> 16;  //IOCSのマウスカーソルのX座標
                 dy = (short) xy;  //IOCSのマウスカーソルのY座標
               }
             } else {  //MMUあり
               if (Z8530.SCC_FSX_MOUSE &&
                   Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
                   MC68060.mmuPeekLongData (0x0938, 1) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
                 on = MC68060.mmuPeekWordSignData (Z8530.sccFSXMouseWork + 0x2e, 1) == 0 ? 1 : 0;  //SX-Windowのマウスカーソルの表示状態。Obscureのときは表示されているとみなす
                 int xy = MC68060.mmuPeekLongData (Z8530.sccFSXMouseWork + 0x0a, 1);
                 dx = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
                 dy = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
                 coeff = MC68060.mmuPeekWordZeroData (Z8530.sccFSXMouseWork + 0x04, 1);  //ポインタの移動量。係数*256
               } else {  //SX-Windowが動作中ではない
                 on = MC68060.mmuPeekByteSignData (0x0aa2, 1);  //IOCSのマウスカーソルの表示状態
                 int xy = MC68060.mmuPeekLongData (0x0ace, 1);
                 dx = xy >> 16;  //IOCSのマウスカーソルのX座標
                 dy = (short) xy;  //IOCSのマウスカーソルのY座標
               }
             }
             dx = XEiJ.musScreenX - dx;  //X方向の移動量
             dy = XEiJ.musScreenY - dy;  //Y方向の移動量
             if (XEiJ.musEdgeAccelerationOn) {  //縁部加速を行う
               final int range = 10;  //加速領域の幅
               final int speed = 10;  //移動速度
               if (XEiJ.musScreenX < range) {
                 dx = -speed;  //左へ
               } else if (XEiJ.pnlScreenWidth - range <= XEiJ.musScreenX) {
                 dx = speed;  //右へ
               }
               if (XEiJ.musScreenY < range) {
                 dy = -speed;  //上へ
               } else if (XEiJ.pnlScreenHeight - range <= XEiJ.musScreenY) {
                 dy = speed;  //下へ
               }
             }
             if (on != 0) {  //X68000のマウスカーソルが表示されている
               //XEiJ.musHide ();
               if (XEiJ.musCursorNumber != 0 && XEiJ.musCursorAvailable) {
                 XEiJ.musCursorNumber = 0;
                 XEiJ.pnlPanel.setCursor (XEiJ.musCursorArray[0]);  //ホストのマウスカーソルを消す
               }
             } else {  //X68000のマウスカーソルが表示されていない
               //XEiJ.musShow ();
               if (XEiJ.musCursorNumber != 1 && XEiJ.musCursorAvailable) {
                 XEiJ.musCursorNumber = 1;
                 XEiJ.pnlPanel.setCursor (XEiJ.musCursorArray[1]);  //ホストのマウスカーソルを表示する
               }
             }
             if (coeff != 256 && coeff != 0) {
               //SX-Windowのポインタの移動量の補正
               dx = (dx << 8) / coeff;
               dy = (dy << 8) / coeff;
             }
             //  XEiJ.MUS_DEACCELERATION_TABLEの値が127を越えることはないのでシームレスでオーバーフローフラグがセットされることはない
             //  rbzで返すので負数のときのゼロ拡張を忘れないこと
             Z8530.scc0Data1 = (dx == 0 ? 0 : dx >= 0 ?
                          XEiJ.MUS_DEACCELERATION_TABLE[Math.min (1024, dx)] :
                          -XEiJ.MUS_DEACCELERATION_TABLE[Math.min (1024, -dx)] & 255);
             Z8530.scc0Data2 = (dy == 0 ? 0 : dy >= 0 ?
                          XEiJ.MUS_DEACCELERATION_TABLE[Math.min (1024, dy)] :
                          -XEiJ.MUS_DEACCELERATION_TABLE[Math.min (1024, -dy)] & 255);
           } else {  //エクスクルーシブ
             //XEiJ.musHide ();
             if (XEiJ.musCursorNumber != 0 && XEiJ.musCursorAvailable) {
               XEiJ.musCursorNumber = 0;
               XEiJ.pnlPanel.setCursor (XEiJ.musCursorArray[0]);  //ホストのマウスカーソルを消す
             }
             int ox = XEiJ.pnlScreenX + (XEiJ.pnlZoomWidth >> 1);  //画面の中央
             int oy = XEiJ.pnlScreenY + (XEiJ.pnlZoomHeight >> 1);
             XEiJ.rbtRobot.mouseMove (XEiJ.pnlGlobalX + ox, XEiJ.pnlGlobalY + oy);  //マウスカーソルを画面の中央に戻す
             int dx = XEiJ.musPanelX - ox;
             int dy = XEiJ.musPanelY - oy;
             if (XEiJ.musExclusiveStart) {  //エクスクルーシブに切り替えた直後
               //エクスクルーシブに切り替えた直後の1回だけ相対位置を無視する
               //  エクスクルーシブに切り替える直前にマウスカーソルが画面の中央から離れていると切り替えた瞬間に画面の端に飛んでしまう
               dx = 0;
               dy = 0;
               XEiJ.musExclusiveStart = false;
             }
             //  上下左右のレスポンスが非対称になると気持ち悪いので冗長に書く
             //  rbzで返すので負数のときのゼロ拡張を忘れないこと
             if (dx != 0) {
               if (dx >= 0) {
                 //dx = dx * Z8530.scc0RatioX + 32768 >> 16;
                 dx = dx * Z8530.scc0RatioX >> 16;
                 if (dx > 127) {
                   d |= 0x10;
                   dx = 127;
                 }
               } else {
                 //dx = -(-dx * Z8530.scc0RatioX + 32768 >> 16);
                 dx = -(-dx * Z8530.scc0RatioX >> 16);
                 if (dx < -128) {
                   d |= 0x20;
                   dx = -128;
                 }
                 dx &= 255;
               }
             }
             if (dy != 0) {
               if (dy >= 0) {
                 //dy = dy * Z8530.scc0RatioY + 32768 >> 16;
                 dy = dy * Z8530.scc0RatioY >> 16;
                 if (dy > 127) {
                   d |= 0x40;
                   dy = 127;
                 }
               } else {
                 //dy = -(-dy * Z8530.scc0RatioY + 32768 >> 16);
                 dy = -(-dy * Z8530.scc0RatioY >> 16);
                 if (dy < -128) {
                   d |= 0x80;
                   dy = -128;
                 }
                 dy &= 255;
               }
             }
             Z8530.scc0Data1 = dx;
             Z8530.scc0Data2 = dy;
           }  //if シームレス else エクスクルーシブ
           Z8530.scc0InputCounter = 1;
           //d = d;
         } else if (Z8530.scc0InputCounter == 1) {  //2バイト目
           Z8530.scc0InputCounter = 2;
           d = Z8530.scc0Data1;
         } else if (Z8530.scc0InputCounter == 2) {  //3バイト目
           Z8530.scc0InputCounter = 3;
           d = Z8530.scc0Data2;
         }
         break;
         //------------------------------------------------
       case Z8530.SCC_1_COMMAND & 7:  //ポートAコマンド読み出し
         switch (Z8530.scc1RegisterNumber) {
         case 0:  //RR0
           //  0x80  ブレークまたはアボート
           //  0x40  送信アンダーラン
           //  0x20  CTS(0=送信禁止,1=送信許可)
           //  0x10  SYNC
           //  0x08  DCD
           //  0x04  送信バッファ空
           //  0x02  ボーレートカウント0
           //  0x01  受信バッファフル
           d = Z8530.scc1InputRead != Z8530.scc1InputWrite && Z8530.scc1InputClock <= XEiJ.mpuClockTime ? 0x25 : 0x24;
           break;
         case 2:  //RR2
           //非修飾割り込みベクタ
           //  ポートAのRR2はWR2に設定したベクタをそのまま返す
           d = Z8530.sccInterruptVector;
           break;
         case 3:  //RR3
           //割り込みペンディング
           //  RR3リクエストからインサービスまでの間セットされている
           //  許可されていない割り込みのビットはセットされない
           d = Z8530.scc1aReceiveRR3 | Z8530.scc1aSendRR3 | Z8530.scc0bReceiveRR3;
           break;
         case 12:  //RR12
           d = Z8530.scc1BaudRateGen & 255;
           break;
         case 13:  //RR13
           d = Z8530.scc1BaudRateGen >> 8 & 255;
           break;
         default:
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.println ("unimplemented register");
           }
         }
         Z8530.scc1RegisterNumber = 0;
         break;
         //------------------------------------------------
       case Z8530.SCC_1_DATA & 7:  //ポートAデータ読み出し(RS-232C受信)
         if (Z8530.scc1InputRead != Z8530.scc1InputWrite && Z8530.scc1InputClock <= XEiJ.mpuClockTime) {  //受信バッファが空ではなく、受信予定時刻になっているとき
           d = Z8530.scc1InputBuffer[Z8530.scc1InputRead];  //ゼロ拡張済み
           Z8530.scc1InputRead = Z8530.scc1InputRead + 1 & Z8530.SCC_1_INPUT_MASK;
           if (Z8530.scc1InputRead != Z8530.scc1InputWrite) {  //受信バッファが空にならなかったとき
             Z8530.scc1InputClock += Z8530.scc1Interval;  //受信バッファの先頭のデータの受信予定時刻
             TickerQueue.tkqAdd (Z8530.sccTicker, Z8530.scc1InputClock);  //割り込みを発生させる
           } else {  //受信バッファが空になったとき
             Z8530.scc1InputClock = XEiJ.FAR_FUTURE;
             TickerQueue.tkqRemove (Z8530.sccTicker);
           }
         }
         break;
         //------------------------------------------------
       default:
         return super.mmdRbz (a);  //バスエラー
       }
       if (Z8530.SCC_DEBUG_TRACE && Z8530.sccTraceOn) {
         System.out.printf ("%08x Z8530.sccRead(0x%08x)=0x%02x\n", XEiJ.regPC0, a, d);
       }
       return d;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       return 0xffffff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       d &= 0xff;
       if (Z8530.SCC_DEBUG_TRACE && Z8530.sccTraceOn) {
         System.out.printf ("%08x Z8530.sccWrite(0x%08x,0x%02x)\n", XEiJ.regPC0, a, d);
       }
       switch (a & 7) {
         //------------------------------------------------
       case Z8530.SCC_0_COMMAND & 7:  //ポートBコマンド書き込み
         switch (Z8530.scc0RegisterNumber) {
         case 0:  //WR0
           if ((d & 0xf0) == 0) {  //レジスタ選択
             Z8530.scc0RegisterNumber = d;
           } else if (d == 0x38) {  //IUSリセット。割り込み処理が終了し、次の割り込みを受け付ける
             if (Z8530.scc0bReceiveRR3 != 0) {
               Z8530.scc0bReceiveRR3 = 0;
               if (Z8530.scc0InputCounter < 3) {  //3バイト受信するまで割り込み要求を続ける
                 if (Z8530.scc0bReceiveMask != 0) {
                   Z8530.scc0bReceiveRR3 = Z8530.SCC_0B_RECEIVE_MASK;
                   Z8530.scc0bReceiveRequest = Z8530.SCC_0B_RECEIVE_MASK;
                   XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
                 }
               }
             }
           } else if (d == 0x10) {  //ステータス割り込みリセット
           } else if (d == 0x30) {  //エラーリセット
           } else if (d == 0x80) {  //送信CRCジェネレータリセット
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented command");
             }
           }
           return;
         case 1:  //WR1
           Z8530.scc0bReceiveMask = (d & 0x18) != 0 ? Z8530.SCC_0B_RECEIVE_MASK : 0;
           if ((d & 0xec) != 0x00) {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented interrupt mode");
             }
           }
           break;
         case 2:  //WR2
           Z8530.sccInterruptVector = d;  //割り込みベクタ
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.printf ("Z8530.sccInterruptVector=0x%02x\n", Z8530.sccInterruptVector);
           }
           Z8530.sccUpdateVector ();
           break;
         case 3:  //WR3
           if (d == 0xc0) {  //受信禁止
           } else if (d == 0xc1) {  //受信許可
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented receiver configuration");
             }
           }
           break;
         case 4:  //WR4
           break;
         case 5:  //WR5
           //  0x80  DTR
           //  0x60  ビット長(0x00=5bit,0x20=7bit,0x40=6bit,0x60=8bit)
           //  0x10  ブレーク
           //  0x08  送信許可
           //  0x04  CRC-16
           //  0x02  RTS
           //  0x01  送信CRC
           {
             int rts = d >> 1 & 1;
             if ((~Z8530.scc0Rts & rts) != 0) {  //RTS=0→1。MSCTRL=H→Lとなってマウスに送信要求が出される
               Z8530.scc0InputCounter = 0;
               //マウスデータ受信開始
               if (Z8530.scc0bReceiveMask != 0) {
                 Z8530.scc0bReceiveRR3 = Z8530.SCC_0B_RECEIVE_MASK;
                 Z8530.scc0bReceiveRequest = Z8530.SCC_0B_RECEIVE_MASK;
                 XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
               }
             }
             Z8530.scc0Rts = rts;
             if ((d & 0x75) == 0x60) {
             } else {
               if (Z8530.SCC_DEBUG_TRACE) {
                 System.out.println ("unimplemented sender configuration");
               }
             }
           }
           break;
         case 6:  //WR6
           break;
         case 7:  //WR7
           break;
         case 9:  //WR9
           if ((d & 0xc0) == 0x40) {  //ポートBリセット
             Z8530.scc0Rts = 0;
           } else if ((d & 0xc0) == 0x80) {  //ポートAリセット
           } else if ((d & 0xc0) == 0xc0) {  //ハードウェアリセット
             Z8530.scc0Rts = 0;
           }
           Z8530.sccVectorInclude = d & 0x11;
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.printf ("Z8530.sccVectorInclude=0x%02x\n", Z8530.sccVectorInclude);
           }
           Z8530.sccUpdateVector ();
           if ((d & 0x26) != 0x00) {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented interrupt configuration");
             }
           }
           break;
         case 10:  //WR10
           if (d == 0x00) {
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented SDLC configuration");
             }
           }
           break;
         case 11:  //WR11
           if (d == 0x50) {  //TRxCは入力
           } else if (d == 0x56) {  //TRxCからボーレートジェネレータを出力
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented clock control");
             }
           }
           break;
         case 12:  //WR12
           Z8530.scc0BaudRateGen = Z8530.scc0BaudRateGen & 255 << 8 | d;
           break;
         case 13:  //WR13
           Z8530.scc0BaudRateGen = d << 8 | Z8530.scc0BaudRateGen & 255;
           break;
         case 14:  //WR14
           if (d == 0x02) {  //ボーレートジェネレータ停止
           } else if (d == 0x03) {  //ボーレートジェネレータ動作
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented DPLL configuration");
             }
           }
           break;
         case 15:  //WR15
           if (d == 0x00) {
           } else if (d == 0x80) {
           } else if (d == 0x88) {
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented status interrupt configuration");
             }
           }
           break;
         default:
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.println ("unimplemented register");
           }
         }
         Z8530.scc0RegisterNumber = 0;
         return;
         //------------------------------------------------
       case Z8530.SCC_0_DATA & 7:  //ポートBデータ書き込み(マウス送信)
         return;
         //------------------------------------------------
       case Z8530.SCC_1_COMMAND & 7:  //ポートAコマンド書き込み
         switch (Z8530.scc1RegisterNumber) {
         case 0:  //WR0
           if ((d & 0xf0) == 0) {  //レジスタ選択
             Z8530.scc1RegisterNumber = d;
           } else if (d == 0x38) {  //IUSリセット。割り込み処理が終了し、次の割り込みを受け付ける
             if (Z8530.scc1aReceiveRR3 != 0) {
               Z8530.scc1aReceiveRR3 = 0;
               if (Z8530.scc1InputRead != Z8530.scc1InputWrite) {  //バッファが空になるまで割り込み要求を続ける
                 TickerQueue.tkqAdd (Z8530.sccTicker, Z8530.scc1InputClock);  //受信バッファの先頭のデータの受信予定時刻
               }
             }
           } else if (d == 0x10) {  //ステータス割り込みリセット
           } else if (d == 0x30) {  //エラーリセット
           } else if (d == 0x80) {  //送信CRCジェネレータリセット
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented command");
             }
           }
           return;
         case 1:  //WR1
           Z8530.scc1aReceiveMask = (d & 0x18) != 0 ? Z8530.SCC_1A_RECEIVE_MASK : 0;
           Z8530.scc1aSendMask = (d & 0x02) != 0 ? Z8530.SCC_1A_SEND_MASK : 0;
           if ((d & 0xec) != 0x00) {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented interrupt mode");
             }
           }
           break;
         case 2:  //WR2
           Z8530.sccInterruptVector = d;  //割り込みベクタ
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.printf ("Z8530.sccInterruptVector=0x%02x\n", Z8530.sccInterruptVector);
           }
           Z8530.sccUpdateVector ();
           break;
         case 3:  //WR3
           if (d == 0xc0) {  //受信禁止
           } else if (d == 0xc1) {  //受信許可
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented receiver configuration");
             }
           }
           break;
         case 4:  //WR4
           Z8530.scc1ClockModeShift = d >> 6 == 0 ? 0 : (d >> 6) + 3;  //0=2^0,1=2^4,2=2^5,3=2^6
           Z8530.scc1Interval = XEiJ.TMR_FREQ / ((Z8530.SCC_FREQ / 2 >> Z8530.scc1ClockModeShift) / (Z8530.scc1BaudRateGen + 2));
           break;
         case 5:  //WR5
           //  0x80  DTR
           //  0x60  ビット長(0x00=5bit,0x20=7bit,0x40=6bit,0x60=8bit)
           //  0x10  ブレーク
           //  0x08  送信許可
           //  0x04  CRC-16
           //  0x02  RTS
           //  0x01  送信CRC
           {
             if ((d & 0x75) == 0x60) {
             } else {
               if (Z8530.SCC_DEBUG_TRACE) {
                 System.out.println ("unimplemented sender configuration");
               }
             }
           }
           break;
         case 6:  //WR6
           break;
         case 7:  //WR7
           break;
         case 9:  //WR9
           if ((d & 0xc0) == 0x40) {  //ポートBリセット
             Z8530.scc0Rts = 0;
           } else if ((d & 0xc0) == 0x80) {  //ポートAリセット
           } else if ((d & 0xc0) == 0xc0) {  //ハードウェアリセット
             Z8530.scc0Rts = 0;
           }
           Z8530.sccVectorInclude = d & 0x11;
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.printf ("Z8530.sccVectorInclude=0x%02x\n", Z8530.sccVectorInclude);
           }
           Z8530.sccUpdateVector ();
           if ((d & 0x2e) != 0x08) {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented interrupt configuration");
             }
           }
           break;
         case 10:  //WR10
           if (d == 0x00) {
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented SDLC configuration");
             }
           }
           break;
         case 11:  //WR11
           if (d == 0x50) {  //TRxCは入力
           } else if (d == 0x56) {  //TRxCからボーレートジェネレータを出力
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented clock control");
             }
           }
           break;
         case 12:  //WR12
           Z8530.scc1BaudRateGen = Z8530.scc1BaudRateGen & 255 << 8 | d;
           Z8530.scc1Interval = XEiJ.TMR_FREQ / ((Z8530.SCC_FREQ / 2 >> Z8530.scc1ClockModeShift) / (Z8530.scc1BaudRateGen + 2));
           break;
         case 13:  //WR13
           Z8530.scc1BaudRateGen = d << 8 | Z8530.scc1BaudRateGen & 255;
           Z8530.scc1Interval = XEiJ.TMR_FREQ / ((Z8530.SCC_FREQ / 2 >> Z8530.scc1ClockModeShift) / (Z8530.scc1BaudRateGen + 2));
           break;
         case 14:  //WR14
           if (d == 0x02) {  //ボーレートジェネレータ停止
           } else if (d == 0x03) {  //ボーレートジェネレータ動作
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented DPLL configuration");
             }
           }
           break;
         case 15:  //WR15
           if (d == 0x00) {
           } else if (d == 0x80) {
           } else if (d == 0x88) {
           } else {
             if (Z8530.SCC_DEBUG_TRACE) {
               System.out.println ("unimplemented status interrupt configuration");
             }
           }
           break;
         default:
           if (Z8530.SCC_DEBUG_TRACE) {
             System.out.println ("unimplemented register");
           }
         }
         Z8530.scc1RegisterNumber = 0;
         return;
         //------------------------------------------------
       case Z8530.SCC_1_DATA & 7:  //ポートAデータ書き込み(RS-232C送信)
         RS232CTerminal.trmPrintSJIS (d);
         return;
         //------------------------------------------------
       default:
         super.mmdWb (a, d);  //バスエラー
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_SCC

  //--------------------------------------------------------------------------------
  //MMD_PPI PPI
  MMD_PPI {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "PPI" : "PPI";  //Programmable Peripheral Interface
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       switch (a) {
       case 0x00e9a001:  //PPI PORT-A ジョイスティック1
         //return (~(XEiJ.ppiPortA >> 1 | XEiJ.ppiPortA) & 0b0101) * 0b11 | XEiJ.ppiPortA;  //上下と左右がそれぞれ同時に押されていたらキャンセルする
         if (XEiJ.regOC >> 6 != 0b0100_101_000) {  //TST.B以外。FM音源レジスタのアクセスウエイトのためのPPIの空読みはジョイスティックのデータを得ることが目的ではない
           XEiJ.ppiJoyTimeLimit = XEiJ.mpuClockTime + XEiJ.PPI_JOY_TIME_SPAN;
         }
         return XEiJ.ppiPortA;
       case 0x00e9a003:  //PPI PORT-B ジョイスティック2
         //return (~(XEiJ.ppiPortB >> 1 | XEiJ.ppiPortB) & 0b0101) * 0b11 | XEiJ.ppiPortB;  //上下と左右がそれぞれ同時に押されていたらキャンセルする
         if (XEiJ.regOC >> 6 != 0b0100_101_000) {  //TST.B以外。FM音源レジスタのアクセスウエイトのためのPPIの空読みはジョイスティックのデータを得ることが目的ではない
           XEiJ.ppiJoyTimeLimit = XEiJ.mpuClockTime + XEiJ.PPI_JOY_TIME_SPAN;
         }
         return XEiJ.ppiPortB;
       case 0x00e9a005:  //PPI PORT-C ADPCM音源,ジョイスティックコントロール
         return XEiJ.ppiPortC;
       case 0x00e9a007:  //PPIコントロール
         return 0;
       }
       return 0;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       d &= 0xff;
       switch (a) {
       case 0x00e9a001:  //PPI PORT-A ジョイスティック1
         return;
       case 0x00e9a003:  //PPI PORT-B ジョイスティック2
         return;
       case 0x00e9a005:  //PPI PORT-C ADPCM音源,ジョイスティックコントロール
         XEiJ.ppiPortC = d;
         ADPCM.pcmSetPan (d);  //パン
         ADPCM.pcmDivider = d >> 2 & 3;  //分周比。0=1/1024,1=1/768,2=1/512,3=1/768
         ADPCM.pcmUpdateRepeatInterval ();
         return;
       case 0x00e9a007:  //PPIコントロール
         if ((d & 0x80) == 0) {
           int n = (d >> 1) & 0x07;  //ビット番号
           XEiJ.ppiPortC = XEiJ.ppiPortC & ~(1 << n) | (d & 1) << n;
           if (n <= 1) {
             ADPCM.pcmSetPan (XEiJ.ppiPortC);  //パン
           } else if (n <= 3) {
             ADPCM.pcmDivider = XEiJ.ppiPortC >> 2 & 3;  //分周比。0=1/1024,1=1/768,2=1/512,3=1/768
             ADPCM.pcmUpdateRepeatInterval ();
           }
         }
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_PPI

  //--------------------------------------------------------------------------------
  //MMD_IOI I/O割り込み
  MMD_IOI {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "I/O 割り込み" : "I/O Interrupt";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == XEiJ.IOI_STATUS) {
         return XEiJ.ioiReadStatus ();
       } else if (a == XEiJ.IOI_VECTOR) {  //read only?
         return XEiJ.ioiReadVector ();
       } else {
         return super.mmdRbz (a);  //バスエラー
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == XEiJ.IOI_STATUS) {
         XEiJ.ioiWriteMask (d);
       } else if (a == XEiJ.IOI_VECTOR) {
         XEiJ.ioiWriteVector (d);
       } else {
         super.mmdWb (a, d);  //バスエラー
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_IOI

  //--------------------------------------------------------------------------------
  //MMD_XB1 拡張ボード領域1
  MMD_XB1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 1" : "Expansion Board Area 1";
    }
    //ピーク
    @Override protected int mmdPbz (int a) {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirPeekByteZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirPeekByteZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return MainMemory.mmrM8[a] & 255;
       }
       return 255;
     }
    @Override protected int mmdPwz (int a) {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirPeekWordZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirPeekWordZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
       return 65535;
     }
    @Override protected int mmdPls (int a) {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirPeekLong (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirPeekLong (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
       return -1;
     }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirReadByteZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirReadByteZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return MainMemory.mmrM8[a] & 255;
       }
       return super.mmdRbz (a);  //バスエラー
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirReadWordZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirReadWordZero (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
       return super.mmdRwz (a);  //バスエラー
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         return XEiJ.fpuCoproboard1.cirReadLong (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         return XEiJ.fpuCoproboard2.cirReadLong (a);
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
       return super.mmdRls (a);  //バスエラー
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         XEiJ.fpuCoproboard1.cirWriteByte (a, d);
         return;
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         XEiJ.fpuCoproboard2.cirWriteByte (a, d);
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         XEiJ.fpuCoproboard1.cirWriteWord (a, d);
         return;
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         XEiJ.fpuCoproboard2.cirWriteWord (a, d);
         return;
       }
       super.mmdWw (a, d);  //バスエラー
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         XEiJ.fpuCoproboard1.cirWriteLong (a, d);
         return;
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         XEiJ.fpuCoproboard2.cirWriteLong (a, d);
         return;
       }
       super.mmdWl (a, d);  //バスエラー
     }
  },  //MMD_XB1

  //--------------------------------------------------------------------------------
  //MMD_EXS 拡張SCSI
  //  必要なときだけ接続される
  //  拡張SCSIのROMのサイズは8KBなのでリードのときのバスエラーのチェックは不要
  //  ライトのときはROMには書き込めないのでSPCのレジスタでなければバスエラー
  MMD_EXS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張 SCSI ポート" : "Expansion SCSI Port";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
       return (a & -32) == SPC.SPC_BASE_EX ? (byte) SPC.spcSCSIEXChip.spiPeek (a) : MainMemory.mmrM8[a];
     }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
       return (a & -32) == SPC.SPC_BASE_EX ? SPC.spcSCSIEXChip.spiPeek (a) : MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
       return mmdPbs (a) << 8 | mmdPbz (a + 1);
     }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
       return mmdPbz (a) << 8 | mmdPbz (a + 1);
     }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
       return mmdPbs (a) << 24 | mmdPbz (a + 1) << 16 | mmdPbz (a + 2) << 8 | mmdPbz (a + 3);
     }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (a & -32) == SPC.SPC_BASE_EX ? (byte) SPC.spcSCSIEXChip.spiRead (a) : MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (a & -32) == SPC.SPC_BASE_EX ? SPC.spcSCSIEXChip.spiRead (a) : MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbs (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbz (a) << 8 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRbs (a) << 24 | mmdRbz (a + 1) << 16 | mmdRbz (a + 2) << 8 | mmdRbz (a + 3);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if ((a & -32) == SPC.SPC_BASE_EX) {
         SPC.spcSCSIEXChip.spiWrite (a, d);
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_EXS

  //--------------------------------------------------------------------------------
  //MMD_XB2 拡張ボード領域2
  MMD_XB2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 2" : "Expansion Board Area 2";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (0x00eaff81 <= a && a <= 0x00eaff89 && (a & 1) != 0) {  //スーパーバイザエリア設定ポート
         return MainMemory.mmrM8[a] & 255;  //読み出せるようにしておく(本来はライトオンリー)
       }
       return super.mmdRbz (a);  //バスエラー
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (0x00eaff81 <= a && a <= 0x00eaff89 && (a & 1) != 0) {  //スーパーバイザエリア設定ポート
         MainMemory.mmrM8[a] = (byte) d;  //読み出せるようにしておく(本来はライトオンリー)
         a = (a & 14) + 2 << 20;  //1,3,5,7,9→2,4,6,8,a
         for (int m = 1; m <= 128; m <<= 1) {
           if ((d & m) == 0) {  //ユーザエリア
             XEiJ.busUser ( MemoryMappedDevice.MMD_MMR, a, a + 0x00040000);
           } else {  //スーパーバイザエリア
             XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, a, a + 0x00040000);
           }
           a += 0x00040000;
         }
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
  },  //MMD_XB2

  //--------------------------------------------------------------------------------
  //MMD_SPR スプライト画面
  MMD_SPR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "スプライト画面" : "Sprite Screen";
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (byte) ((XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) ? mmdRwz (a) >> 8 : mmdRwz (a - 1) & 255);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) ? mmdRwz (a) >> 8 : mmdRwz (a - 1) & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return (short) mmdRwz (a);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a < 0x00eb0400) {  //スプライトスクロールレジスタ
         int n = (a >> 3) & 0x7f;
         switch (a & 0x06) {
         case 0:  //x座標
           return SpriteScreen.sprX[n];
         case 2:  //y座標
           return SpriteScreen.sprY[n];
         case 4:  //パターン番号、パレットブロック、水平反転、垂直反転
           return (SpriteScreen.sprV[n] ? 0x8000 : 0) | (SpriteScreen.sprH[n] ? 0x4000 : 0) | SpriteScreen.sprColPort[n] << 4 | SpriteScreen.sprNum[n];
         case 6:  //プライオリティ
           return SpriteScreen.sprPrw[n];
         }
       } else if (a < 0x00eb8000) {  //各種レジスタ
         switch (a) {
         case SpriteScreen.SPR_REG0_BG0_X:
           return SpriteScreen.sprReg0Bg0XPort;
         case SpriteScreen.SPR_REG1_BG0_Y:
           return SpriteScreen.sprReg1Bg0YPort;
         case SpriteScreen.SPR_REG2_BG1_X:
           return SpriteScreen.sprReg2Bg1XPort;
         case SpriteScreen.SPR_REG3_BG1_Y:
           return SpriteScreen.sprReg3Bg1YPort;
         case SpriteScreen.SPR_REG4_BG_CTRL:
           return SpriteScreen.sprReg4BgCtrlPort;
         case SpriteScreen.SPR_REG5_H_TOTAL:
           return SpriteScreen.sprReg5HTotalPort;
         case SpriteScreen.SPR_REG6_H_START:
           return SpriteScreen.sprReg6HStartPort;
         case SpriteScreen.SPR_REG7_V_START:
           return SpriteScreen.sprReg7VStartPort;
         case SpriteScreen.SPR_REG8_RESO:
           return SpriteScreen.sprReg8ResoPort;
         }
         return 0;
       } else {  //PCGエリアとテキストエリア
         int t = a >> 2 & 0x1fff;
         return (a & 0x02) == 0 ? SpriteScreen.sprPatPort[t] >>> 16 : (char) SpriteScreen.sprPatPort[t];
       }
       return 0;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {
         mmdWw (a, d << 8 | mmdRwz (a) & 255);
       } else {
         mmdWw (a - 1, mmdRwz (a - 1) & 0xff00 | d & 255);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a < 0x00eb0400) {  //スプライトスクロールレジスタ
         int n = a >> 3 & 0x7f;  //スプライト番号
         switch (a & 0x06) {
         case 0:  //x座標
           SpriteScreen.sprX[n] = (short) (d & 1023);
/*
           if (SpriteScreen.sprPrw[n] != 0 &&
               VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
               SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
             int y = SpriteScreen.sprY[n];
             if (true) {
               if (y < 16) {
                 for (int i = 0; i < y; i++) {
                   CRTC.crtRasterStamp[i] = 0;
                 }
               } else {
                 CRTC.crtRasterStamp[y - 16] = 0;
                 CRTC.crtRasterStamp[y - 15] = 0;
                 CRTC.crtRasterStamp[y - 14] = 0;
                 CRTC.crtRasterStamp[y - 13] = 0;
                 CRTC.crtRasterStamp[y - 12] = 0;
                 CRTC.crtRasterStamp[y - 11] = 0;
                 CRTC.crtRasterStamp[y - 10] = 0;
                 CRTC.crtRasterStamp[y -  9] = 0;
                 CRTC.crtRasterStamp[y -  8] = 0;
                 CRTC.crtRasterStamp[y -  7] = 0;
                 CRTC.crtRasterStamp[y -  6] = 0;
                 CRTC.crtRasterStamp[y -  5] = 0;
                 CRTC.crtRasterStamp[y -  4] = 0;
                 CRTC.crtRasterStamp[y -  3] = 0;
                 CRTC.crtRasterStamp[y -  2] = 0;
                 CRTC.crtRasterStamp[y -  1] = 0;
               }
             } else {
               Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
             }
           }
*/
           break;
         case 2:  //y座標
           d &= 1023;
           if (SpriteScreen.sprY[n] != d) {
             int y = SpriteScreen.sprY[n];
             SpriteScreen.sprY[n] = (short) d;
             if (SpriteScreen.sprPrw[n] != 0) {
               if (SpriteScreen.SPR_RRMAP) {
                 int mask = ~(0x80000000 >>> n);  //intのシフトカウントは5bitでマスクされる
                 int i = y << 2 | n >> 5;
                 SpriteScreen.sprRRmap[i            ] &= mask;
                 SpriteScreen.sprRRmap[i + ( 1 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 2 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 3 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 4 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 5 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 6 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 7 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 8 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 9 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (10 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (11 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (12 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (13 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (14 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (15 << 2)] &= mask;
                 mask = ~mask;
                 i = d << 2 | n >> 5;
                 SpriteScreen.sprRRmap[i            ] |= mask;
                 SpriteScreen.sprRRmap[i + ( 1 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 2 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 3 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 4 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 5 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 6 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 7 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 8 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 9 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (10 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (11 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (12 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (13 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (14 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (15 << 2)] |= mask;
               } else {  //!SpriteScreen.SPR_RRMAP
                 int[] map = SpriteScreen.sprRmap[n >> 5];
                 int mask = ~(1 << n);  //intのシフトカウントは5bitでマスクされる
                 map[y     ] &= mask;
                 map[y +  1] &= mask;
                 map[y +  2] &= mask;
                 map[y +  3] &= mask;
                 map[y +  4] &= mask;
                 map[y +  5] &= mask;
                 map[y +  6] &= mask;
                 map[y +  7] &= mask;
                 map[y +  8] &= mask;
                 map[y +  9] &= mask;
                 map[y + 10] &= mask;
                 map[y + 11] &= mask;
                 map[y + 12] &= mask;
                 map[y + 13] &= mask;
                 map[y + 14] &= mask;
                 map[y + 15] &= mask;
                 mask = ~mask;
                 map[d     ] |= mask;
                 map[d +  1] |= mask;
                 map[d +  2] |= mask;
                 map[d +  3] |= mask;
                 map[d +  4] |= mask;
                 map[d +  5] |= mask;
                 map[d +  6] |= mask;
                 map[d +  7] |= mask;
                 map[d +  8] |= mask;
                 map[d +  9] |= mask;
                 map[d + 10] |= mask;
                 map[d + 11] |= mask;
                 map[d + 12] |= mask;
                 map[d + 13] |= mask;
                 map[d + 14] |= mask;
                 map[d + 15] |= mask;
               }  //if SpriteScreen.SPR_RRMAP/!SpriteScreen.SPR_RRMAP
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 if (true) {
                   if (y < 16) {
                     for (int i = 0; i < y; i++) {
                       CRTC.crtRasterStamp[i] = 0;
                     }
                   } else {
                     CRTC.crtRasterStamp[y - 16] = 0;
                     CRTC.crtRasterStamp[y - 15] = 0;
                     CRTC.crtRasterStamp[y - 14] = 0;
                     CRTC.crtRasterStamp[y - 13] = 0;
                     CRTC.crtRasterStamp[y - 12] = 0;
                     CRTC.crtRasterStamp[y - 11] = 0;
                     CRTC.crtRasterStamp[y - 10] = 0;
                     CRTC.crtRasterStamp[y -  9] = 0;
                     CRTC.crtRasterStamp[y -  8] = 0;
                     CRTC.crtRasterStamp[y -  7] = 0;
                     CRTC.crtRasterStamp[y -  6] = 0;
                     CRTC.crtRasterStamp[y -  5] = 0;
                     CRTC.crtRasterStamp[y -  4] = 0;
                     CRTC.crtRasterStamp[y -  3] = 0;
                     CRTC.crtRasterStamp[y -  2] = 0;
                     CRTC.crtRasterStamp[y -  1] = 0;
                   }
                 } else {
                   Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
                 }
                 if (true) {
                   if (d < 16) {
                     for (int i = 0; i < d; i++) {
                       CRTC.crtRasterStamp[i] = 0;
                     }
                   } else {
                     CRTC.crtRasterStamp[d - 16] = 0;
                     CRTC.crtRasterStamp[d - 15] = 0;
                     CRTC.crtRasterStamp[d - 14] = 0;
                     CRTC.crtRasterStamp[d - 13] = 0;
                     CRTC.crtRasterStamp[d - 12] = 0;
                     CRTC.crtRasterStamp[d - 11] = 0;
                     CRTC.crtRasterStamp[d - 10] = 0;
                     CRTC.crtRasterStamp[d -  9] = 0;
                     CRTC.crtRasterStamp[d -  8] = 0;
                     CRTC.crtRasterStamp[d -  7] = 0;
                     CRTC.crtRasterStamp[d -  6] = 0;
                     CRTC.crtRasterStamp[d -  5] = 0;
                     CRTC.crtRasterStamp[d -  4] = 0;
                     CRTC.crtRasterStamp[d -  3] = 0;
                     CRTC.crtRasterStamp[d -  2] = 0;
                     CRTC.crtRasterStamp[d -  1] = 0;
                   }
                 } else {
                   Arrays.fill (CRTC.crtRasterStamp, d < 16 ? 0 : d - 16, d, 0);
                 }
               }
*/
             }
           }
           break;
         case 4:  //パターン番号、パレットブロック、水平反転、垂直反転
           {
             int num = SpriteScreen.sprNum[n];
             SpriteScreen.sprNum[n] = (short) (d & 255);
             SpriteScreen.sprColPort[n] = (short) (d >> 4 & 0x0f << 4);
             SpriteScreen.sprH[n] = (short) (d << 1) < 0;
             SpriteScreen.sprV[n] = (short) d < 0;
             if (SpriteScreen.sprPrw[n] != 0) {
               if (SpriteScreen.SPR_RRMAP) {
                 int mask = 0x80000000 >>> n;  //intのシフトカウントは5bitでマスクされる
                 SpriteScreen.sprPPmap[num << 2 | n >> 5] -= mask;
                 SpriteScreen.sprPPmap[(d & 255) << 2 | n >> 5] += mask;
               } else {
                 int[] map = SpriteScreen.sprPmap[n >> 5];
                 int mask = 1 << n;  //intのシフトカウントは5bitでマスクされる
                 map[num] -= mask;
                 map[d & 255] += mask;
               }
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 int y = SpriteScreen.sprY[n];
                 if (true) {
                   if (y < 16) {
                     for (int i = 0; i < y; i++) {
                       CRTC.crtRasterStamp[i] = 0;
                     }
                   } else {
                     CRTC.crtRasterStamp[y - 16] = 0;
                     CRTC.crtRasterStamp[y - 15] = 0;
                     CRTC.crtRasterStamp[y - 14] = 0;
                     CRTC.crtRasterStamp[y - 13] = 0;
                     CRTC.crtRasterStamp[y - 12] = 0;
                     CRTC.crtRasterStamp[y - 11] = 0;
                     CRTC.crtRasterStamp[y - 10] = 0;
                     CRTC.crtRasterStamp[y -  9] = 0;
                     CRTC.crtRasterStamp[y -  8] = 0;
                     CRTC.crtRasterStamp[y -  7] = 0;
                     CRTC.crtRasterStamp[y -  6] = 0;
                     CRTC.crtRasterStamp[y -  5] = 0;
                     CRTC.crtRasterStamp[y -  4] = 0;
                     CRTC.crtRasterStamp[y -  3] = 0;
                     CRTC.crtRasterStamp[y -  2] = 0;
                     CRTC.crtRasterStamp[y -  1] = 0;
                   }
                 } else {
                   Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
                 }
               }
*/
             }
           }
           break;
         case 6:  //プライオリティ
           d &= 0x0003;
           int prw = SpriteScreen.sprPrw[n];
           SpriteScreen.sprPrw[n] = (byte) d;
           if (prw != d) {
             if (prw == 0) {  //出現
               int y = SpriteScreen.sprY[n];
               if (SpriteScreen.SPR_RRMAP) {
                 int mask = 0x80000000 >>> n;  //intのシフトカウントは5bitでマスクされる
                 int i = y << 2 | n >> 5;
                 SpriteScreen.sprRRmap[i            ] |= mask;
                 SpriteScreen.sprRRmap[i + ( 1 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 2 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 3 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 4 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 5 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 6 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 7 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 8 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + ( 9 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (10 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (11 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (12 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (13 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (14 << 2)] |= mask;
                 SpriteScreen.sprRRmap[i + (15 << 2)] |= mask;
                 SpriteScreen.sprPPmap[SpriteScreen.sprNum[n] << 2 | n >> 5] |= mask;
               } else {  //!SpriteScreen.SPR_RRMAP
                 int map[] = SpriteScreen.sprRmap[n >> 5];
                 int mask = 1 << n;  //intのシフトカウントは5bitでマスクされる
                 map[y     ] |= mask;
                 map[y +  1] |= mask;
                 map[y +  2] |= mask;
                 map[y +  3] |= mask;
                 map[y +  4] |= mask;
                 map[y +  5] |= mask;
                 map[y +  6] |= mask;
                 map[y +  7] |= mask;
                 map[y +  8] |= mask;
                 map[y +  9] |= mask;
                 map[y + 10] |= mask;
                 map[y + 11] |= mask;
                 map[y + 12] |= mask;
                 map[y + 13] |= mask;
                 map[y + 14] |= mask;
                 map[y + 15] |= mask;
                 SpriteScreen.sprPmap[n >> 5][SpriteScreen.sprNum[n]] |= mask;
               }  //if SpriteScreen.SPR_RRMAP/!SpriteScreen.SPR_RRMAP
             } else if (d == 0) {  //消滅
               int y = SpriteScreen.sprY[n];
               if (SpriteScreen.SPR_RRMAP) {
                 int mask = ~(0x80000000 >>> n);  //intのシフトカウントは5bitでマスクされる
                 int i = y << 2 | n >> 5;
                 SpriteScreen.sprRRmap[i            ] &= mask;
                 SpriteScreen.sprRRmap[i + ( 1 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 2 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 3 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 4 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 5 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 6 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 7 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 8 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + ( 9 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (10 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (11 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (12 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (13 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (14 << 2)] &= mask;
                 SpriteScreen.sprRRmap[i + (15 << 2)] &= mask;
                 SpriteScreen.sprPPmap[SpriteScreen.sprNum[n] << 2 | n >> 5] &= mask;
               } else {  //!SpriteScreen.SPR_RRMAP
                 int map[] = SpriteScreen.sprRmap[n >> 5];
                 int mask = ~(1 << n);  //intのシフトカウントは5bitでマスクされる
                 map[y     ] &= mask;
                 map[y +  1] &= mask;
                 map[y +  2] &= mask;
                 map[y +  3] &= mask;
                 map[y +  4] &= mask;
                 map[y +  5] &= mask;
                 map[y +  6] &= mask;
                 map[y +  7] &= mask;
                 map[y +  8] &= mask;
                 map[y +  9] &= mask;
                 map[y + 10] &= mask;
                 map[y + 11] &= mask;
                 map[y + 12] &= mask;
                 map[y + 13] &= mask;
                 map[y + 14] &= mask;
                 map[y + 15] &= mask;
                 SpriteScreen.sprPmap[n >> 5][SpriteScreen.sprNum[n]] &= mask;
               }  //if SpriteScreen.SPR_RRMAP/!SpriteScreen.SPR_RRMAP
             }
/*
             if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                 SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
               int y = SpriteScreen.sprY[n];
               if (true) {
                 if (y < 16) {
                   for (int i = 0; i < y; i++) {
                     CRTC.crtRasterStamp[i] = 0;
                   }
                 } else {
                   CRTC.crtRasterStamp[y - 16] = 0;
                   CRTC.crtRasterStamp[y - 15] = 0;
                   CRTC.crtRasterStamp[y - 14] = 0;
                   CRTC.crtRasterStamp[y - 13] = 0;
                   CRTC.crtRasterStamp[y - 12] = 0;
                   CRTC.crtRasterStamp[y - 11] = 0;
                   CRTC.crtRasterStamp[y - 10] = 0;
                   CRTC.crtRasterStamp[y -  9] = 0;
                   CRTC.crtRasterStamp[y -  8] = 0;
                   CRTC.crtRasterStamp[y -  7] = 0;
                   CRTC.crtRasterStamp[y -  6] = 0;
                   CRTC.crtRasterStamp[y -  5] = 0;
                   CRTC.crtRasterStamp[y -  4] = 0;
                   CRTC.crtRasterStamp[y -  3] = 0;
                   CRTC.crtRasterStamp[y -  2] = 0;
                   CRTC.crtRasterStamp[y -  1] = 0;
                 }
               } else {
                 Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
               }
             }
*/
           }
           break;
         }  //スプライトスクロールレジスタのswitch
         return;
       } else if (a < 0x00eb8000) {  //各種レジスタ
         switch (a) {
         case SpriteScreen.SPR_REG0_BG0_X:
           SpriteScreen.sprReg0Bg0XPort = d & 1023;
           {
             int curr = SpriteScreen.sprReg0Bg0XMask == 0 ? SpriteScreen.sprReg0Bg0XPort : SpriteScreen.sprReg0Bg0XTest;
             if (SpriteScreen.sprReg0Bg0XCurr != curr) {
               SpriteScreen.sprReg0Bg0XCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   (SpriteScreen.sprReg4BgCtrlCurr & (1 << 9 | 1 << 0)) == (1 << 9 | 1 << 0)) {  //BG0が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG1_BG0_Y:
           SpriteScreen.sprReg1Bg0YPort = d & 1023;
           {
             int curr = SpriteScreen.sprReg1Bg0YMask == 0 ? SpriteScreen.sprReg1Bg0YPort : SpriteScreen.sprReg1Bg0YTest;
             if (SpriteScreen.sprReg1Bg0YCurr != curr) {
               SpriteScreen.sprReg1Bg0YCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   (SpriteScreen.sprReg4BgCtrlCurr & (1 << 9 | 1 << 0)) == (1 << 9 | 1 << 0)) {  //BG0が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG2_BG1_X:
           SpriteScreen.sprReg2Bg1XPort = d & 1023;
           {
             int curr = SpriteScreen.sprReg2Bg1XMask == 0 ? SpriteScreen.sprReg2Bg1XPort : SpriteScreen.sprReg2Bg1XTest;
             if (SpriteScreen.sprReg2Bg1XCurr != curr) {
               SpriteScreen.sprReg2Bg1XCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   (SpriteScreen.sprReg4BgCtrlCurr & (1 << 9 | 1 << 3)) == (1 << 9 | 1 << 3)) {  //BG1が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG3_BG1_Y:
           SpriteScreen.sprReg3Bg1YPort = d & 1023;
           {
             int curr = SpriteScreen.sprReg3Bg1YMask == 0 ? SpriteScreen.sprReg3Bg1YPort : SpriteScreen.sprReg3Bg1YTest;
             if (SpriteScreen.sprReg3Bg1YCurr != curr) {
               SpriteScreen.sprReg3Bg1YCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   (SpriteScreen.sprReg4BgCtrlCurr & (1 << 9 | 1 << 3)) == (1 << 9 | 1 << 3)) {  //BG1が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG4_BG_CTRL:
           SpriteScreen.sprReg4BgCtrlPort = d & 2047;
           {
             int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
             if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
               SpriteScreen.sprReg4BgCtrlCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0) {  //スプライト画面が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG5_H_TOTAL:
           SpriteScreen.sprReg5HTotalPort = d & 255;
           {
             int curr = SpriteScreen.sprReg5HTotalMask == 0 ? SpriteScreen.sprReg5HTotalPort : SpriteScreen.sprReg5HTotalTest;
             if (SpriteScreen.sprReg5HTotalCurr != curr) {
               SpriteScreen.sprReg5HTotalCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG6_H_START:
           SpriteScreen.sprReg6HStartPort = d & 63;
           {
             int curr = SpriteScreen.sprReg6HStartMask == 0 ? SpriteScreen.sprReg6HStartPort : SpriteScreen.sprReg6HStartTest;
             if (SpriteScreen.sprReg6HStartCurr != curr) {
               SpriteScreen.sprReg6HStartCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG7_V_START:
           SpriteScreen.sprReg7VStartPort = d & 255;
           {
             int curr = SpriteScreen.sprReg7VStartMask == 0 ? SpriteScreen.sprReg7VStartPort : SpriteScreen.sprReg7VStartTest;
             if (SpriteScreen.sprReg7VStartCurr != curr) {
               SpriteScreen.sprReg7VStartCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         case SpriteScreen.SPR_REG8_RESO:
           SpriteScreen.sprReg8ResoPort = d & 31;
           {
             int curr = SpriteScreen.sprReg8ResoPort & ~SpriteScreen.sprReg8ResoMask | SpriteScreen.sprReg8ResoTest & SpriteScreen.sprReg8ResoMask;
             if (SpriteScreen.sprReg8ResoCurr != curr) {
               SpriteScreen.sprReg8ResoCurr = curr;
/*
               if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
                   SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
                 CRTC.crtAllStamp += 2;
               }
*/
             }
           }
           return;
         }
         return;
       } else {  //PCGエリアとテキストエリア
         if (a >= 0x00ebe000) {  //テキストエリア1
           int n = a >> 1 & 0x0fff;
           SpriteScreen.sprT1Num[n] = (short) ((d & 0x00ff) << 3);
           SpriteScreen.sprT1ColPort[n] = (short) ((d & 0x0f00) >> 4);
           SpriteScreen.sprT1H[n] = (short) (d << 1) < 0;
           SpriteScreen.sprT1V[n] = (short) d < 0;
/*
           if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
               SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
             if ((SpriteScreen.sprReg4BgCtrlCurr & 0b111) == 0b011) {  //BG0が表示されていてBG0にTEXT1が割り当てられている
               if ((SpriteScreen.sprReg8ResoCurr & 0x0003) == 0) {  //水平256ドット→8x8
                 int y = (n >> 6 << 3) - SpriteScreen.sprReg1Bg0YCurr;
                 CRTC.crtRasterStamp[y     & 511] = 0;
                 CRTC.crtRasterStamp[y + 1 & 511] = 0;
                 CRTC.crtRasterStamp[y + 2 & 511] = 0;
                 CRTC.crtRasterStamp[y + 3 & 511] = 0;
                 CRTC.crtRasterStamp[y + 4 & 511] = 0;
                 CRTC.crtRasterStamp[y + 5 & 511] = 0;
                 CRTC.crtRasterStamp[y + 6 & 511] = 0;
                 CRTC.crtRasterStamp[y + 7 & 511] = 0;
               } else {  //水平512ドット→16x16
                 int y = (n >> 6 << 4) - SpriteScreen.sprReg1Bg0YCurr;
                 CRTC.crtRasterStamp[y      & 1023] = 0;
                 CRTC.crtRasterStamp[y +  1 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  2 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  3 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  4 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  5 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  6 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  7 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  8 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  9 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 10 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 11 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 12 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 13 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 14 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 15 & 1023] = 0;
               }
             }
             if ((SpriteScreen.sprReg4BgCtrlCurr & 0b111_000) == 0b011_000) {  //BG1が表示されていてBG1にTEXT1が割り当てられている
               int y = (n >> 6 << 3) - SpriteScreen.sprReg3Bg1YCurr;
               CRTC.crtRasterStamp[y     & 511] = 0;
               CRTC.crtRasterStamp[y + 1 & 511] = 0;
               CRTC.crtRasterStamp[y + 2 & 511] = 0;
               CRTC.crtRasterStamp[y + 3 & 511] = 0;
               CRTC.crtRasterStamp[y + 4 & 511] = 0;
               CRTC.crtRasterStamp[y + 5 & 511] = 0;
               CRTC.crtRasterStamp[y + 6 & 511] = 0;
               CRTC.crtRasterStamp[y + 7 & 511] = 0;
             }
           }
*/
         } else if (a >= 0x00ebc000) {  //テキストエリア0
           int n = a >> 1 & 0x0fff;
           SpriteScreen.sprT0Num[n] = (short) ((d & 0x00ff) << 3);
           SpriteScreen.sprT0ColPort[n] = (short) ((d & 0x0f00) >> 4);
           SpriteScreen.sprT0H[n] = (short) (d << 1) < 0;
           SpriteScreen.sprT0V[n] = (short) d < 0;
/*
           if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
               SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
             if ((SpriteScreen.sprReg4BgCtrlCurr & 0b111) == 0b001) {  //BG0が表示されていてBG0にTEXT0が割り当てられている
               if ((SpriteScreen.sprReg8ResoCurr & 0x0003) == 0) {  //水平256ドット→8x8
                 int y = (n >> 6 << 3) - SpriteScreen.sprReg1Bg0YCurr;
                 CRTC.crtRasterStamp[y     & 511] = 0;
                 CRTC.crtRasterStamp[y + 1 & 511] = 0;
                 CRTC.crtRasterStamp[y + 2 & 511] = 0;
                 CRTC.crtRasterStamp[y + 3 & 511] = 0;
                 CRTC.crtRasterStamp[y + 4 & 511] = 0;
                 CRTC.crtRasterStamp[y + 5 & 511] = 0;
                 CRTC.crtRasterStamp[y + 6 & 511] = 0;
                 CRTC.crtRasterStamp[y + 7 & 511] = 0;
               } else {  //水平512ドット→16x16
                 int y = (n >> 6 << 4) - SpriteScreen.sprReg1Bg0YCurr;
                 CRTC.crtRasterStamp[y      & 1023] = 0;
                 CRTC.crtRasterStamp[y +  1 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  2 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  3 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  4 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  5 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  6 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  7 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  8 & 1023] = 0;
                 CRTC.crtRasterStamp[y +  9 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 10 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 11 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 12 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 13 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 14 & 1023] = 0;
                 CRTC.crtRasterStamp[y + 15 & 1023] = 0;
               }
             }
             if ((SpriteScreen.sprReg4BgCtrlCurr & 0b111_000) == 0b001_000) {  //BG1が表示されていてBG1にTEXT0が割り当てられている
               int y = (n >> 6 << 3) - SpriteScreen.sprReg3Bg1YCurr;
               CRTC.crtRasterStamp[y     & 511] = 0;
               CRTC.crtRasterStamp[y + 1 & 511] = 0;
               CRTC.crtRasterStamp[y + 2 & 511] = 0;
               CRTC.crtRasterStamp[y + 3 & 511] = 0;
               CRTC.crtRasterStamp[y + 4 & 511] = 0;
               CRTC.crtRasterStamp[y + 5 & 511] = 0;
               CRTC.crtRasterStamp[y + 6 & 511] = 0;
               CRTC.crtRasterStamp[y + 7 & 511] = 0;
             }
           }
*/
         }
         int t = a >> 2 & 0x1fff;
         SpriteScreen.sprPatPort[t] = (a & 0x02) == 0 ? d << 16 | (char) SpriteScreen.sprPatPort[t] : ~0xffff & SpriteScreen.sprPatPort[t] | (char) d;
/*
         if (VideoController.vcnReg3Curr << 31 - 6 < 0 &&  //スプライト画面が表示されている
             SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
           int num = a >> 7 & 255;  //num=パターン番号
           if (SpriteScreen.SPR_RRMAP) {
             for (int i = num << 2, nn = 0; nn <= 128 - 32; nn += 32) {
               for (int map = SpriteScreen.sprPPmap[i++], n = nn; map != 0; map <<= 1, n++) {
                 if (map >= 0) {  //このスプライトには割り当てられていない
                   continue;
                 }
                 int y = SpriteScreen.sprY[n];
                 if (true) {
                   if (y < 16) {
                     for (int k = 0; k < y; k++) {
                       CRTC.crtRasterStamp[k] = 0;
                     }
                   } else {
                     CRTC.crtRasterStamp[y - 16] = 0;
                     CRTC.crtRasterStamp[y - 15] = 0;
                     CRTC.crtRasterStamp[y - 14] = 0;
                     CRTC.crtRasterStamp[y - 13] = 0;
                     CRTC.crtRasterStamp[y - 12] = 0;
                     CRTC.crtRasterStamp[y - 11] = 0;
                     CRTC.crtRasterStamp[y - 10] = 0;
                     CRTC.crtRasterStamp[y -  9] = 0;
                     CRTC.crtRasterStamp[y -  8] = 0;
                     CRTC.crtRasterStamp[y -  7] = 0;
                     CRTC.crtRasterStamp[y -  6] = 0;
                     CRTC.crtRasterStamp[y -  5] = 0;
                     CRTC.crtRasterStamp[y -  4] = 0;
                     CRTC.crtRasterStamp[y -  3] = 0;
                     CRTC.crtRasterStamp[y -  2] = 0;
                     CRTC.crtRasterStamp[y -  1] = 0;
                   }
                 } else {
                   Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
                 }
               }  //for map,n
             }  //for i,nn
           } else {  //!SpriteScreen.SPR_RRMAP
             for (int i = 96; i >= 0; i -= 32) {
               int map = SpriteScreen.sprPmap[i >> 5][num];
               if (map == 0) {
                 continue;
               }
               for (int n = i + 31; n >= i; n--) {  //n=スプライト番号
                 if (map < 0) {
                   int y = SpriteScreen.sprY[n];
                   if (true) {
                     if (y < 16) {
                       for (int k = 0; k < y; k++) {
                         CRTC.crtRasterStamp[k] = 0;
                       }
                     } else {
                       CRTC.crtRasterStamp[y - 16] = 0;
                       CRTC.crtRasterStamp[y - 15] = 0;
                       CRTC.crtRasterStamp[y - 14] = 0;
                       CRTC.crtRasterStamp[y - 13] = 0;
                       CRTC.crtRasterStamp[y - 12] = 0;
                       CRTC.crtRasterStamp[y - 11] = 0;
                       CRTC.crtRasterStamp[y - 10] = 0;
                       CRTC.crtRasterStamp[y -  9] = 0;
                       CRTC.crtRasterStamp[y -  8] = 0;
                       CRTC.crtRasterStamp[y -  7] = 0;
                       CRTC.crtRasterStamp[y -  6] = 0;
                       CRTC.crtRasterStamp[y -  5] = 0;
                       CRTC.crtRasterStamp[y -  4] = 0;
                       CRTC.crtRasterStamp[y -  3] = 0;
                       CRTC.crtRasterStamp[y -  2] = 0;
                       CRTC.crtRasterStamp[y -  1] = 0;
                     }
                   } else {
                     Arrays.fill (CRTC.crtRasterStamp, y < 16 ? 0 : y - 16, y, 0);
                   }
                 }  //if map<0
                 map <<= 1;
               }  //for n
             }  //for i
           }  //if SpriteScreen.SPR_RRMAP/!SpriteScreen.SPR_RRMAP
         }  //(VideoController.vcnReg3Curr&0x40)!=0
*/
       }  //PCGエリアとテキストエリア
     }  //mmdWw
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_SPR

  //--------------------------------------------------------------------------------
  //MMD_SMR SRAM
  MMD_SMR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "SRAM" : "SRAM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == 0x00ed0018 &&  //起動デバイス
           XEiJ.mpuBootDevice >= 0 &&  //「ここから再起動」
           XEiJ.regPC0 == XEiJ.romBootDeviceRead) {  //起動デバイス
         return (short) XEiJ.mpuBootDevice;
       }
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == 0x00ed0018 &&  //起動デバイス
           XEiJ.mpuBootDevice >= 0 &&  //「ここから再起動」
           XEiJ.regPC0 == XEiJ.romBootDeviceRead) {  //起動デバイス
         return XEiJ.mpuBootDevice;
       }
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (a == 0x00ed000c) {  //ROM起動ハンドル
         if (XEiJ.mpuBootDevice == 0xa000) {  //「ここから再起動」でROM起動
           if (XEiJ.regPC0 == XEiJ.romBootROMAddressRead ||  //起動シーケンス
               XEiJ.regPC0 == XEiJ.romBootinfROMAddressRead) {  //IOCS _BOOTINF
             return XEiJ.mpuBootAddress;
           }
         }
       } else if (a == 0x00ed0010) {  //RAM起動アドレス
         if (XEiJ.mpuBootDevice == 0xb000) {  //「ここから再起動」でRAM起動
           if (XEiJ.regPC0 == XEiJ.romBootRAMAddressRead ||  //起動シーケンス
               XEiJ.regPC0 == XEiJ.romBootinfRAMAddressRead) {  //IOCS _BOOTINF
             return XEiJ.mpuBootAddress;
           }
         }
       }
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.smrWriteEnableOn) {
         MainMemory.mmrM8[a] = (byte) d;
         if (a == 0x00ed002b || a == 0x00ed0059) {  //キーボードの配列または字体が変化した
           Keyboard.kbdRepaint ();  //キーボードが表示されているときkbdImageを作り直して再描画する
         }
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWb (a    , d >> 8);
       mmdWb (a + 1, d     );
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_SMR

  //--------------------------------------------------------------------------------
  //MMD_CG1 CGROM1
  MMD_CG1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "CGROM" : "CGROM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG1 ();
      }
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG1 ();
      }
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG1 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG1 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG1 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG1 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG1 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG1 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG1 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG1 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait << 1;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
  },  //MMD_CG1

  //--------------------------------------------------------------------------------
  //MMD_CG2 CGROM2
  MMD_CG2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "CGROM" : "CGROM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG2 ();
      }
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG2 ();
      }
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG2 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG2 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (XEiJ.ROM_CREATE_CGROM_LATER) {
        XEiJ.romCreateCG2 ();
      }
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG2 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG2 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG2 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG2 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.ROM_CREATE_CGROM_LATER) {
         XEiJ.romCreateCG2 ();
       }
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait << 1;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
  },  //MMD_CG2

  //--------------------------------------------------------------------------------
  //MMD_ROM ROM
  MMD_ROM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ROM" : "ROM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 255;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       return MainMemory.mmrM8[a] & 255;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuCycleCount += XEiJ.mpuRomWait << 1;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
       }
     }
  },  //MMD_ROM

  //--------------------------------------------------------------------------------
  //MMD_IBP 命令ブレークポイント
  MMD_IBP {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "命令ブレークポイント" : "Instruction Break Point";
    }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (InstructionBreakPoint.IBP_ON) {
         InstructionBreakPoint.InstructionBreakRecord[] hashTable = XEiJ.regSRS != 0 ? InstructionBreakPoint.ibpSuperHashTable : InstructionBreakPoint.ibpUserHashTable;
         for (InstructionBreakPoint.InstructionBreakRecord r = hashTable[a >> 1 & InstructionBreakPoint.IBP_HASH_MASK]; r != null; r = r.ibrNext) {  //同じ物理ハッシュコードを持つ命令ブレークポイントについて
           if (r.ibrPhysicalAddress == a) {  //命令ブレークポイントが設定されているとき
             if (r.ibrValue == r.ibrTarget) {  //現在値が目標値と一致しているとき
               if (r.ibrThreshold < 0) {  //インスタントのとき
                 InstructionBreakPoint.ibpRemove (r.ibrLogicalAddress, XEiJ.regSRS);  //取り除く
               } else if (r.ibrTarget < r.ibrThreshold) {  //インスタント化しているとき
                 r.ibrTarget = r.ibrThreshold;  //目標値を閾値に戻す
               } else {  //インスタントでなくインスタント化していないとき
                 if (r.ibrScriptElement != null &&  //スクリプトが指定されていて
                     r.ibrScriptElement.exlEval ().exlFloatValue.iszero ()) {  //条件が成立していないとき
                   break;  //続行する
                 }
                 r.ibrTarget++;  //目標値を増やす
               }
               M68kException.m6eNumber = -1;  //停止する
               throw M68kException.m6eSignal;
             } else {  //現在値が目標値と一致していないとき
               r.ibrValue++;  //現在値を増やす
               break;  //続行する
             }
           }
         }
       }
       if (DataBreakPoint.DBP_ON) {
         return DataBreakPoint.dbpMemoryMap[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       } else {
         return XEiJ.busMemoryMap[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       }
     }
  },  //MMD_IBP

  //--------------------------------------------------------------------------------
  //MMD_DBP データブレークポイント
  MMD_DBP {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "データブレークポイント" : "Data Break Point";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPbs (a);
    }
    @Override protected int mmdPbz (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPbz (a);
    }
    @Override protected int mmdPws (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPws (a);
    }
    @Override protected int mmdPwz (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPwz (a);
    }
    @Override protected int mmdPls (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPls (a);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRbs (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d);
       return (byte) d;
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d);
       return d;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d);
       return d;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d);
       return d;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRls (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_LONG, a, d);
       return d;
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d);
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWl (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_LONG, a, d);
     }
  },  //MMD_DBP

  //--------------------------------------------------------------------------------
  //MMD_NUL ヌルデバイス
  MMD_NUL {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ヌルデバイス" : "Null Device";
    }
  };  //MMD_NUL

  //--------------------------------------------------------------------------------
  //ピークのデフォルト
  //  エラーや副作用なしでリードする
  //  バスエラーのときは-1をキャストした値を返す
  //  リードがデバイスの状態を変化させる可能性がある場合は個別に処理すること
  protected byte mmdPbs (int a) {
    try {
      return (byte) mmdRbz (a);
    } catch (M68kException e) {
    }
    return -1;
  }
  protected int mmdPbz (int a) {
    try {
      return mmdRbz (a);
    } catch (M68kException e) {
    }
    return 255;
  }
  protected int mmdPws (int a) {
    try {
      return (short) mmdRwz (a);
    } catch (M68kException e) {
    }
    return -1;
  }
  protected int mmdPwz (int a) {
    try {
      return mmdRwz (a);
    } catch (M68kException e) {
    }
    return 65535;
  }
  protected int mmdPls (int a) {
    try {
      return mmdRls (a);
    } catch (M68kException e) {
    }
    return -1;
  }
  //リードのデフォルト
  //  バイトとワードの符号拡張はゼロ拡張を呼び出す
  //  符号なしとロングはバスエラー
  protected byte mmdRbs (int a) throws M68kException {
    return (byte) mmdRbz (a);
  }
  protected int mmdRbz (int a) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_READ;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
    throw M68kException.m6eSignal;
  }
  protected int mmdRws (int a) throws M68kException {
    return (short) mmdRwz (a);
  }
  protected int mmdRwz (int a) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_READ;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_WORD;
    throw M68kException.m6eSignal;
  }
  protected int mmdRls (int a) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_READ;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_LONG;
    throw M68kException.m6eSignal;
  }
  //ポークのデフォルト
  //  エラーや副作用なしでライトする
  //  ライトがデバイスの状態を変化させる可能性がある場合は個別に処理すること
  protected void mmdVb (int a, int d) {
    try {
      mmdWb (a, d);
    } catch (M68kException e) {
    }
  }
  protected void mmdVw (int a, int d) {
    try {
      mmdWw (a, d);
    } catch (M68kException e) {
    }
  }
  protected void mmdVl (int a, int d) {
    try {
      mmdWl (a, d);
    } catch (M68kException e) {
    }
  }
  //ライトのデフォルト
  //  すべてバスエラー
  protected void mmdWb (int a, int d) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_WRITE;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
    throw M68kException.m6eSignal;
  }
  protected void mmdWw (int a, int d) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_WRITE;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_WORD;
    throw M68kException.m6eSignal;
  }
  protected void mmdWl (int a, int d) throws M68kException {
    M68kException.m6eFSLW |= M68kException.M6E_FSLW_BUS_ERROR_ON_WRITE;  //サイズは設定済み。分割されている場合があることに注意
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_LONG;
    throw M68kException.m6eSignal;
  }
}  //enum MemoryMappedDevice



