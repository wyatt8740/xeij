//========================================================================================
//  OPMTest.java
//    en:OPM test
//    ja:OPMテスト
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.zip.*;  //CRC32,Deflater,GZIPInputStream,GZIPOutputStream,ZipEntry,ZipInputStream

public class OPMTest {

  public static final int[] OTS_TEST_DATA = {
/*
    0x89e0dbe7,  // 1 OK
    0x8a3bef50,  // 2 OK
    0xe49c49fb,  // 3 OK
    0x0c652382,  // 4 OK
    0x18f668b3,  // 5 OK
    0x91a0b0b1,  // 6 OK
    0x6f5d10da,  // 7 OK
    0x434e3774,  // 8 OK
    0xc38b40a9,  // 9 OK
    0xe261d5a4,  //10 OK
    0x2b2e2def,  //11 OK
    0x7522981f,  //12 OK
    0xb4acff1b,  //13 OK
    0x70b068d3,  //14 OK
    0xd5e52aba,  //15 OK
    0x9a23468c,  //16 OK
    0x67ae24b0,  //17 OK
    0xa2a3489a,  //18 OK
    0x296bd7cf,  //19 OK
    0x074b6a56,  //20 OK
    0x0bdf079b,  //21 OK
    0xb537944d,  //22 OK
    0x48a0e27d,  //23 OK
    0x3eef47e5,  //24 OK
    0x1c2ba97c,  //25 OK
    0x5efe4c43,  //26 OK
    0xa842c70a,  //27 OK
    0xb132e05f,  //28 OK
    0x3b3a60f7,  //29 OK
    0x3802de14,  //30 OK
    0xca1ac0d6,  //31 OK
    0x4157a63d,  //32 OK
    0xd9f73d3d,  //33 OK
    0x02241d79,  //34 OK
    0xc4e8c518,  //35 OK
    0xa85c0f8c,  //36 OK
    0x446b4cb4,  //37 OK
    0x75a9fe48,  //38 OK
    0xad12a819,  //39 OK
    0x6d9e3e3a,  //40 OK
    0xf7b11596,  //41 OK
    0xe043a19d,  //42 OK
    0x0dc122c2,  //43 OK
    0x901c5c3b,  //44 OK
    0x1c9da5be,  //45 OK
    0x613640a9,  //46 OK
    0xb5b389bc,  //47 OK
    0xfc894e09,  //48 OK
    0xe6bfe1de,  //49 OK
    0x446be244,  //50 OK
    0x28a4892d,  //51 OK
    0xd898514c,  //52 OK
    0x90cb64eb,  //53 OK
    0x81d05b52,  //54 OK
    0xc8b56bcf,  //55 OK
    0xa2f21f4d,  //56 OK
    0xdc422744,  //57 OK
    0xa0557229,  //58 OK
    0x15aabb3a,  //59 OK
    0xf2c973a7,  //60 OK
    0x8a9be7a4,  //61 OK
    0x678c6ee2,  //62 OK
    0x37fb72b5,  //63 OK
    0xf340013a,  //64 OK
    0xba1dd942,  //65 OK
    0x78a8c84c,  //66 OK
    0x3c95559f,  //67 OK
    0x374fa017,  //68 OK
*/
    0x97e44107,  // 1 OK
    0xef3507bb,  // 2 OK
    0x9f6f473e,  // 3 OK
    0xdf7c67dc,  // 4 OK
    0xffd7dc0b,  // 5 OK
    0x62e427d1,  // 6 OK
    0x4d01655a,  // 7 OK
    0x9d606efb,  // 8 OK
    0xa2c36a12,  // 9 OK
    0x954c9cd2,  //10 OK
    0x2b2e2def,  //11 OK
    0x03273adb,  //12 OK
    0xa503da51,  //13 OK
    0x02914a27,  //14 OK
    0x48763db2,  //15 OK
    0xd2ff4720,  //16 OK
    0xf0fcff11,  //17 OK
    0xd4dcd235,  //18 OK
    0x5da3a360,  //19 OK
    0x202fd9bb,  //20 OK
    0xa4e7b164,  //21 OK
    0x57f85c09,  //22 OK
    0x570a7ee1,  //23 OK
    0xf4338481,  //24 OK
    0x925c2414,  //25 OK
    0x1a01014f,  //26 OK
    0xf8cafee7,  //27 OK
    0x807e1b31,  //28 OK
    0xee4689d3,  //29 OK
    0x6c3cdfb8,  //30 OK
    0xa6af20cc,  //31 OK
    0xd8a0b0b4,  //32 OK
    0x7d638b3e,  //33 OK
    0x52cefaa3,  //34 OK
    0x92c1a95d,  //35 OK
    0xaf556f47,  //36 OK
    0x99b34548,  //37 OK
    0x970814ff,  //38 OK
    0xe6027515,  //39 OK
    0x31a58b27,  //40 OK
    0x5c8b7370,  //41 OK
    0xd7660cc1,  //42 OK
    0x5480c2d7,  //43 OK
    0x9453eefa,  //44 OK
    0x1c9da5be,  //45 OK
    0x613640a9,  //46 OK
    0xb5b389bc,  //47 OK
    0x93526d65,  //48 OK
    0x11fb99cd,  //49 OK
    0x446be244,  //50 OK
    0x4187069a,  //51 OK
    0x5178f328,  //52 OK
    0x90cb64eb,  //53 OK
    0x05f0b918,  //54 OK
    0xc746ab53,  //55 OK
    0xa2f21f4d,  //56 OK
    0xc474259f,  //57 OK
    0x35c5ff7f,  //58 OK
    0x15aabb3a,  //59 OK
    0x18d15838,  //60 OK
    0xaf635730,  //61 OK
    0xa875b2e8,  //62 OK
    0x5633294d,  //63 OK
    0x54819ef9,  //64 OK
    0x07981323,  //65 OK
    0xbed1bd16,  //66 OK
    0x3c95559f,  //67 OK
    0x374fa017,  //68 OK
  };

  //otsTest ()
  //  OPMのテスト
  public static void otsTest () {
    if (!SoundSource.SND_FREQ_TABLE) {
      return;
    }
    InputStream in = XEiJ.ismOpen ("HUMAN302.XDF");
    if (in == null) {
      return;
    }
    XEiJ.ismSkip (in, 1024 * 349 + 64 + 0x787c);
    byte[][] na = new byte[68][];
    for (int n = 0; n < 68; n++) {
      byte[] b = na[n] = new byte[10];
      XEiJ.ismRead (in, b, 0, 10);
    }
    XEiJ.ismSkip (in, 0x7b38 - (0x787c + 10 * 68));
    byte[][] va = new byte[68][];
    for (int n = 0; n < 68; n++) {
      byte[] a = va[n] = new byte[55];
      XEiJ.ismRead (in, a, 0, 55);
    }
    XEiJ.ismClose (in);
    if (false) {
      System.out.println ("int YM2151.OPM_DEFAULT_TONE[68][55] = {");
      for (int n = 0; n < 68; n++) {
        System.out.printf ("  //%02d:", n + 1);
        byte[] b = na[n];
        for (int i = 0; i < 10; i++) {
          System.out.print (CharacterCode.chrSJISToChar[b[i] & 255]);
        }
        System.out.println ();
        byte[] a = va[n];
        for (int row = 0; row < 5; row++) {
          System.out.print (row == 0 ? "  { " : "    ");
          for (int col = 0; col < 11; col++) {
            System.out.printf ("0x%02x", a[11 * row + col] & 255);
            System.out.print (col < 10 ? ", " : row < 4 ? "," : " },");
          }
          System.out.println ();
        }
      }
      System.out.println ("};");
    }
    final int seconds = 2;
    final int freq = 48000;
    byte[] outputBuffer = new byte[2 * 2 * freq * seconds];
    CRC32 crc32 = new CRC32 ();
    final int ch = 0;
    final int kc = 72;
    final int kf = 0;
    final int vol = 16;
    SoundSource.sndPlayOn = true;
    int err = 0;
    long t = System.nanoTime ();
    for (int n = 0; n < 68; n++) {
      byte[] a = va[n];
      //音色を設定する
      YM2151.opmReset ();
      YM2151.opmSetData (0x1b, a[2] & 3);  //WAVE
      YM2151.opmSetData (0x18, a[4] & 255);  //SPEED
      YM2151.opmSetData (0x19, 1 << 7 | (a[5] & 127));  //PMD
      YM2151.opmSetData (0x19, 0 << 7 | (a[6] & 127));  //AMD
      YM2151.opmSetData (0x38 + ch, (a[7] & 7) << 4 | (a[8] & 3));  //PMS<<4|AMS
      YM2151.opmSetData (0x0f, 0);  //NE,NFRQ
      YM2151.opmSetData (0x20 + ch, (a[9] & 3) << 6 | (a[0] & 63));  //RLPAN<<6|FL<<3|CON
      YM2151.opmSetData (0x40 + ch, (a[11 + 8] & 7) << 4 | (a[11 + 7] & 15));  //M1 DT1<<4|MUL
      YM2151.opmSetData (0x50 + ch, (a[22 + 8] & 7) << 4 | (a[22 + 7] & 15));  //C1 DT1<<4|MUL
      YM2151.opmSetData (0x48 + ch, (a[33 + 8] & 7) << 4 | (a[33 + 7] & 15));  //M2 DT1<<4|MUL
      YM2151.opmSetData (0x58 + ch, (a[44 + 8] & 7) << 4 | (a[44 + 7] & 15));  //C2 DT1<<4|MUL
      YM2151.opmSetData (0x60 + ch, a[11 + 5] & 127);  //M1 TL
      YM2151.opmSetData (0x70 + ch, a[22 + 5] & 127);  //C1 TL
      YM2151.opmSetData (0x68 + ch, a[33 + 5] & 127);  //M2 TL
      YM2151.opmSetData (0x78 + ch, a[44 + 5] & 127);  //C2 TL
      YM2151.opmSetData (0x80 + ch, (a[11 + 6] & 3) << 6 | (a[11 + 0] & 31));  //M1 KS<<6|AR
      YM2151.opmSetData (0x90 + ch, (a[22 + 6] & 3) << 6 | (a[22 + 0] & 31));  //C1 KS<<6|AR
      YM2151.opmSetData (0x88 + ch, (a[33 + 6] & 3) << 6 | (a[33 + 0] & 31));  //M2 KS<<6|AR
      YM2151.opmSetData (0x98 + ch, (a[44 + 6] & 3) << 6 | (a[44 + 0] & 31));  //C2 KS<<6|AR
      YM2151.opmSetData (0xa0 + ch, (a[11 + 10] & 1) << 7 | (a[11 + 1] & 31));  //M1 AMSEN<<7|D1R
      YM2151.opmSetData (0xb0 + ch, (a[22 + 10] & 1) << 7 | (a[22 + 1] & 31));  //C1 AMSEN<<7|D1R
      YM2151.opmSetData (0xa8 + ch, (a[33 + 10] & 1) << 7 | (a[33 + 1] & 31));  //M2 AMSEN<<7|D1R
      YM2151.opmSetData (0xb8 + ch, (a[44 + 10] & 1) << 7 | (a[44 + 1] & 31));  //C2 AMSEN<<7|D1R
      YM2151.opmSetData (0xc0 + ch, (a[11 + 9] & 3) << 6 | (a[11 + 2] & 31));  //M1 DT2<<6|D2R
      YM2151.opmSetData (0xd0 + ch, (a[22 + 9] & 3) << 6 | (a[22 + 2] & 31));  //C1 DT2<<6|D2R
      YM2151.opmSetData (0xc8 + ch, (a[33 + 9] & 3) << 6 | (a[33 + 2] & 31));  //M2 DT2<<6|D2R
      YM2151.opmSetData (0xd8 + ch, (a[44 + 9] & 3) << 6 | (a[44 + 2] & 31));  //C2 DT2<<6|D2R
      YM2151.opmSetData (0xe0 + ch, (a[11 + 4] & 15) << 4 | (a[11 + 3] & 15));  //M1 D1L<<4|RR
      YM2151.opmSetData (0xf0 + ch, (a[22 + 4] & 15) << 4 | (a[22 + 3] & 15));  //C1 D1L<<4|RR
      YM2151.opmSetData (0xe8 + ch, (a[33 + 4] & 15) << 4 | (a[33 + 3] & 15));  //M2 D1L<<4|RR
      YM2151.opmSetData (0xf8 + ch, (a[44 + 4] & 15) << 4 | (a[44 + 3] & 15));  //C2 D1L<<4|RR
      switch (a[0] & 7) {
      case 7:
        if ((a[0] & 7 << 3) == 0) {
          YM2151.opmSetData (0x60 + ch, vol);  //M1 TL
        }
      case 6:
      case 5:
        YM2151.opmSetData (0x68 + ch, vol);  //M2 TL
      case 4:
        YM2151.opmSetData (0x70 + ch, vol);  //C1 TL
      case 3:
      case 2:
      case 1:
      case 0:
        YM2151.opmSetData (0x78 + ch, vol);  //C2 TL
      }
      YM2151.opmSetData (0x28 + ch, kc);  //KC
      YM2151.opmSetData (0x30 + ch, kf);  //KF
      //keyon
      YM2151.opmSetData (0x08, (a[1] & 15) << 3 | ch);  //KON SLOT<<3|CH
      //if ((a[3] & 1) != 0) {  //SYNC
      //  YM2151.opmSetData (0x01, 2);  //LFOリセット
      //  YM2151.opmSetData (0x01, 0);
      //}
      int outputPointer = 0;
      //0秒～1秒
      while (outputPointer < 2 * 2 * freq * (seconds >> 1)) {
        //1/25秒間出力する
        YM2151.opmPointer = 0;
        YM2151.opmUpdate (2 * YM2151.OPM_BLOCK_SAMPLES);
        //62.5kHzから48kHzに変換する
        for (int dst = 0; dst < SoundSource.SND_BLOCK_SAMPLES; dst++) {
          int src2 = SoundSource.sndFreqConvIndex[dst];
          int l = Math.max (-32768, Math.min (32767, YM2151.opmBuffer[src2    ]));
          int r = Math.max (-32768, Math.min (32767, YM2151.opmBuffer[src2 + 1]));
          int dst4 = dst << 2;
          outputBuffer[outputPointer    ] = (byte) l;
          outputBuffer[outputPointer + 1] = (byte) (l >> 8);
          outputBuffer[outputPointer + 2] = (byte) r;
          outputBuffer[outputPointer + 3] = (byte) (r >> 8);
          outputPointer += 4;
        }
      }
      //keyoff
      YM2151.opmSetData (0x08, 0 << 3 | ch);  //KON SLOT<<3|CH
      //1秒～2秒
      while (outputPointer < 2 * 2 * freq * seconds) {
        //1/25秒間出力する
        YM2151.opmPointer = 0;
        YM2151.opmUpdate (2 * YM2151.OPM_BLOCK_SAMPLES);
        //62.5kHzから48kHzに変換する
        for (int dst = 0; dst < SoundSource.SND_BLOCK_SAMPLES; dst++) {
          int src2 = SoundSource.sndFreqConvIndex[dst];
          int l = Math.max (-32768, Math.min (32767, YM2151.opmBuffer[src2    ]));
          int r = Math.max (-32768, Math.min (32767, YM2151.opmBuffer[src2 + 1]));
          int dst4 = dst << 2;
          outputBuffer[outputPointer    ] = (byte) l;
          outputBuffer[outputPointer + 1] = (byte) (l >> 8);
          outputBuffer[outputPointer + 2] = (byte) r;
          outputBuffer[outputPointer + 3] = (byte) (r >> 8);
          outputPointer += 4;
        }
      }
      //MAMEの出力と比較する
      crc32.reset ();
      crc32.update (outputBuffer);
      int result = (int) crc32.getValue ();
      if (true) {
        System.out.printf ("    0x%08x,  //%2d %s\n", result, n + 1, result == OTS_TEST_DATA[n] ? "OK" : "ERROR");
      }
      if (result != OTS_TEST_DATA[n]) {
        err++;
      }
    }
    System.out.printf ("%8.3fms\n", (double) (System.nanoTime () - t) / 1000000.0);
    System.out.println (err + " errors");
  }  //otsTest()

}  //class OPMTest



