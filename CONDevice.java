//========================================================================================
//  CONDevice.java
//    en:CON device control -- It pastes the text that was copied to the clipboard of the host machine to the console of Human68k.
//    ja:CONデバイス制御 -- ホストマシンのクリップボードにコピーされたテキストをHuman68kのコンソールに貼り付けます。
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.datatransfer.*;  //Clipboard,DataFlavor,FlavorEvent,FlavorListener,Transferable,UnsupportedFlavorException
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class CONDevice {

  public static TimerTask conPasteTask;  //貼り付け中の文字列をコンソール入力バッファに書き込むタスク。null=貼り付け中ではない

  //conInit ()
  //  CONデバイス制御を初期化する
  public static void conInit () {
    conPasteTask = null;
  }  //conInit()

  //conDoPaste ()
  //  貼り付け
  //  クリップボードの文字列をCONデバイスのコンソール入力バッファに書き込む
  public static void conDoPaste () {
    //クリップボードから文字列を取り出す
    String string = null;
    try {
      string = (String) XEiJ.clpClipboard.getData (DataFlavor.stringFlavor);
    } catch (Exception e) {
      return;
    }
    //ASK68K version 3.02を探す
    int con = MainMemory.mmrHumanDev ('C' << 24 | 'O' << 16 | 'N' << 8 | ' ', ' ' << 24 | ' ' << 16 | ' ' << 8 | ' ');
    if (con < 0 ||  //CONデバイスが見つからない
        !(//MC68060.mmuPeekLongData (con + 0x000168, 1) == 0x93fa967b &&  //"日本"
          //MC68060.mmuPeekLongData (con + 0x00016c, 1) == 0x8cea8374 &&  //"語フ"
          //MC68060.mmuPeekLongData (con + 0x000170, 1) == 0x838d8393 &&  //"ロン"
          //MC68060.mmuPeekLongData (con + 0x000174, 1) == 0x83678376 &&  //"トプ"
          //MC68060.mmuPeekLongData (con + 0x000178, 1) == 0x838d835a &&  //"ロセ"
          //MC68060.mmuPeekLongData (con + 0x00017c, 1) == 0x83628354 &&  //"ッサ"
          MC68060.mmuPeekLongData (con + 0x000180, 1) == 0x20826082 &&  //" ＡＳ"
          MC68060.mmuPeekLongData (con + 0x000184, 1) == 0x72826a82 &&  //"ＳＫ６"
          MC68060.mmuPeekLongData (con + 0x000188, 1) == 0x55825782 &&  //"６８Ｋ"
          MC68060.mmuPeekLongData (con + 0x00018c, 1) == 0x6a20666f &&  //"Ｋ fo"
          MC68060.mmuPeekLongData (con + 0x000190, 1) == 0x72205836 &&  //"r X6"
          MC68060.mmuPeekLongData (con + 0x000194, 1) == 0x38303030 &&  //"8000"
          MC68060.mmuPeekLongData (con + 0x000198, 1) == 0x20766572 &&  //" ver"
          MC68060.mmuPeekLongData (con + 0x00019c, 1) == 0x73696f6e &&  //"sion"
          MC68060.mmuPeekLongData (con + 0x0001a0, 1) == 0x20332e30 &&  //" 3.0"
          MC68060.mmuPeekLongData (con + 0x0001a4, 1) == 0x320d0a43// &&  //"2\r\nC"
          //MC68060.mmuPeekLongData (con + 0x0001a8, 1) == 0x6f707972 &&  //"opyr"
          //MC68060.mmuPeekLongData (con + 0x0001ac, 1) == 0x69676874 &&  //"ight"
          //MC68060.mmuPeekLongData (con + 0x0001b0, 1) == 0x20313938 &&  //" 198"
          //MC68060.mmuPeekLongData (con + 0x0001b4, 1) == 0x372d3934 &&  //"7-94"
          //MC68060.mmuPeekLongData (con + 0x0001b8, 1) == 0x20534841 &&  //" SHA"
          //MC68060.mmuPeekLongData (con + 0x0001bc, 1) == 0x52502043 &&  //"RP C"
          //MC68060.mmuPeekLongData (con + 0x0001c0, 1) == 0x6f72702e &&  //"orp."
          //MC68060.mmuPeekLongData (con + 0x0001c4, 1) == 0x2f414343 &&  //"/ACC"
          //MC68060.mmuPeekLongData (con + 0x0001c8, 1) == 0x45535320 &&  //"ESS "
          //MC68060.mmuPeekLongData (con + 0x0001cc, 1) == 0x434f2e2c &&  //"CO.,"
          //MC68060.mmuPeekLongData (con + 0x0001d0, 1) == 0x4c54442e &&  //"LTD."
          //MC68060.mmuPeekLongData (con + 0x0001d4, 1) == 0x0d0a0000  //"\r\n\0\0"
          )) {  //ASK68K version 3.02が見つからない
      return;
    }
    //文字列をコンソール入力バッファに書き込む
    //  ASK68K version 3.02のコンソール入力バッファは200バイトしかない
    //  大きいテキストも貼り付けられるようにタイマーを使う
    if (conPasteTask == null) {
      XEiJ.tmrTimer.schedule (conPasteTask = new CONPasteTask (con, string), XEiJ.TMR_DELAY, XEiJ.TMR_INTERVAL);
    }
  }  //conDoPaste



  //$$CPT 貼り付けタスク
  //  貼り付け中の文字列をコンソール入力バッファに書き込むタスク
  public static class CONPasteTask extends TimerTask {
    private int con;  //CONデバイスヘッダ
    private String string;  //貼り付ける文字列
    private int length;  //貼り付ける文字列の長さ
    private int index;  //次に書き込む文字のインデックス
    public CONPasteTask (int con, String string) {
      this.con = con;
      this.string = string;
      length = string.length ();
      index = 0;
    }  //CONPasteTask(int,String)
    @Override public void run () {
      int read = MC68060.mmuPeekLongData (con + 0x00e460, 1);  //コンソール入力バッファから最後に読み出した位置、または、これから読み出そうとしている位置
      int write = MC68060.mmuPeekLongData (con + 0x00e464, 1);  //コンソール入力バッファへ最後に書き込んだ位置。入力中はこの後に書き込み始めている場合がある
      if (read != write) {  //空になるまで待つ
        return;
      }
      //!!! 入力と貼り付けが競合するとデータが混ざったり欠落したりする可能性がある
      //  MPUのスレッドでMPUが動いていないときに貼り付けているので命令の実行中にコンソール入力バッファを書き換えることはないが、
      //  割り込みルーチンでコンソール入力バッファを書き換えた場合と同様の壊れ方をする可能性はある
      int head = con + 0x010504;  //コンソール入力バッファの先頭
      int tail = head + 200;  //コンソール入力バッファの末尾
      for (; index < length; index++) {
        int c = CharacterCode.chrCharToSJIS[string.charAt (index)];  //UTF16→SJIS変換
        if (c == 0) {  //変換できない
          continue;  //無視する
        }
        if (c == '\r' && index + 1 < length && string.charAt (index + 1) == '\n') {  //CRLF
          index++;  //CRにする
        } else if (c == '\n') {  //LF
          c = '\r';  //CRにする
        }
        if (!(c >= ' ' || c == '\t' || c == '\r')) {  //タブと改行以外の制御コード
          continue;  //無視する
        }
        int write1 = write + 1 == tail ? head : write + 1;  //1バイト目を書き込む位置
        int write2 = write1 + 1 == tail ? head : write1 + 1;  //2バイト目を書き込む位置
        int write3 = write2 + 1 == tail ? head : write2 + 1;  //3バイト目を書き込む位置。予備
        if (write1 == read || write2 == read || write3 == read) {  //コンソール入力バッファフル。readの位置はまだ読み出されていない場合がある
          break;  //書き込みを延期する
        }
        if (c < 0x0100) {  //1バイトのとき
          MC68060.mmuPokeByteData (write1, c, 1);
          write = write1;
        } else {  //2バイトのとき
          MC68060.mmuPokeByteData (write1, c >> 8, 1);
          MC68060.mmuPokeByteData (write2, c, 1);
          write = write2;
        }
      }
      MC68060.mmuPokeLongData (con + 0x00e464, write, 1);  //コンソール入力バッファへ最後に書き込んだ位置
      if (index == length) {  //全部貼り付け終わった
        cancel ();
        conPasteTask = null;
      }
    }  //run()
  }  //class CONPasteTask



}  //class CONDevice



