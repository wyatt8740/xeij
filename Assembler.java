//========================================================================================
//  Assembler.java
//    en:Assembler -- (under construction)
//    ja:アセンブラ -- (工事中)
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//
//  機能
//    開始アドレスと複数行の文字列を受け取ってバイト配列を返す
//    全体を2回以上評価することでディスプレースメントのサイズを最適化する
//    行頭ラベルは変数として保存する
//    イミディエイトオペランドやディスプレースメントなどの数値を書く場所に式を書くことができる
//
//  アセンブル手順
//    パス1(パース)
//      文字列を'\n'で行に分割する
//      行ごとに
//        ラベル、ニモニック、サイズ、オペランドに分ける
//        オペランドをトークンリストに変換する
//        オペランドをツリーに変換する
//        ニモニックとオペランドの種類から命令の種類を確定する
//          該当する命令がなければエラー
//        相対分岐命令を含むディスプレースメントについて
//          命令の制約による最小長と最大長を設定する
//            分岐命令のディスプレースメントはサプレスできない
//            MOVEP命令のディスプレースメントはワード固定
//            など
//          サイズが指定されているとき
//            最小長と最大長の範囲内でなければエラー
//            指定されたサイズ(確定)にする
//          サイズが指定されていないとき
//            最小長==最大長のとき
//              最大長(確定)にする
//            最小長<最大長のとき
//              最大長(未確定)にする
//        命令長を計算する
//          ディスプレースメントのサイズによってアドレッシングモードが変化する場合も考慮して正確に計算すること
//        行のアドレスを計算する
//        行頭ラベルに現在の行のアドレスを代入する
//    パス2(前方参照の解決と最適化)
//      変化フラグをクリアする
//      行ごとに
//        オペランドを評価する
//          ロング(未確定)のディスプレースメントがワードで入り切り最小長がワード以下のとき
//            ワード(未確定)に変更する
//            変化フラグをセットする
//          ワード(未確定)のディスプレースメントが0で最小長がサプレスのとき
//            サプレス(未確定)に変更する
//            変化フラグをセットする
//          サプレス(未確定)のディスプレースメントが0でないとき
//            (0だが削除すると0でなくなってしまうため削除できないディスプレースメントがある)
//            最小長をワードに変更する
//            ワード(未確定)に変更する
//            変化フラグをセットする
//          ワード(未確定)のディスプレースメントがワードに入り切らないとき
//            最小長をロングに変更する
//            ロング(確定)に変更する
//            変化フラグをセットする
//        行頭ラベルに現在の行のアドレスを代入する
//      変化フラグがセットされなくなるまで繰り返す
//    パス3(出力)
//      バイナリを生成する
//
//  オペランド
//    イミディエイトオペランド
//    コンディションコードレジスタ
//    ステータスレジスタ
//    制御レジスタ
//    メモリ管理レジスタ
//    データレジスタペア
//    汎用レジスタリスト
//    汎用レジスタ
//    浮動小数点レジスタリスト
//    浮動小数点レジスタ
//    浮動小数点制御レジスタリスト
//    浮動小数点制御レジスタ
//    レジスタ間接ペア
//    キャッシュ選択
//    ビットフィールド付きオペランド
//    k-factor付きオペランド
//    メモリ実効アドレス
//
//  イミディエイトオペランド
//    #<data>
//    '#'で始まっているとき数値をパースする
//
//  データレジスタペア
//    D1:D0など
//    データレジスタの直後に':'が続くときはデータレジスタペア
//
//  汎用レジスタリスト
//    D0-D7/A0-A6など
//    汎用レジスタの直後に'-'または'/'が続くときは汎用レジスタリスト
//    汎用レジスタを'/'で区切って昇順に列挙する
//    連続するデータレジスタまたは連続するアドレスレジスタの途中を'-'で省略できる
//    汎用レジスタはメモリに配置される順序通りに重複なく記述しなければならない
//
//  浮動小数点レジスタリスト
//    FP0-FP7など
//    浮動小数点レジスタの直後に'-'または'/'が続くときは浮動小数点レジスタリスト
//    浮動小数点レジスタを'/'で区切って昇順に列挙する
//    連続するレジスタの途中を'-'で省略できる
//    浮動小数点レジスタはメモリに配置される順序通りに重複なく記述しなければならない
//
//  浮動小数点制御レジスタリスト
//    FPCR/FPSR/FPIARなど
//    浮動小数点制御レジスタの直後に'/'が続くときは浮動小数点制御レジスタリスト
//    浮動小数点制御レジスタを'/'で区切って昇順に列挙する
//    '-'は使わない
//    浮動小数点制御レジスタはメモリに配置される順序通りに重複なく記述しなければならない
//
//  汎用レジスタ間接ペア
//    (D0):(A0)など
//
//  キャッシュ選択
//    NC DC IC BC
//
//  ビットフィールド付きオペランド
//    データレジスタ{#o:#w}
//    データレジスタ{#o:Dw}
//    データレジスタ{Do:#w}
//    データレジスタ{Do:Dw}
//    メモリ実効アドレス{#o:#w}
//    メモリ実効アドレス{#o:Dw}
//    メモリ実効アドレス{Do:#w}
//    メモリ実効アドレス{Do:Dw}
//
//  k-factor付きオペランド
//    メモリ実効アドレス{#k}
//    メモリ実効アドレス{Dl}
//
//  メモリ実効アドレス
//    アドレスレジスタ間接
//    アドレスレジスタ間接ポストインクリメント付き
//    アドレスレジスタ間接プレデクリメント付き
//    アドレスレジスタ間接ディスプレースメント付き
//    アドレスレジスタ間接インデックス付き
//    絶対ショート
//    絶対ロング
//    プログラムカウンタ間接ディスプレースメント付き
//    プログラムカウンタ間接インデックス付き
//
//  アドレスレジスタ間接
//    (Ar)
//    メモリ制御実効アドレスとしてパースする
//
//  アドレスレジスタ間接ポストインクリメント付き
//    (Ar)+
//    直接パースする
//
//  アドレスレジスタ間接プレデクリメント付き
//    -(Ar)
//    直接パースする
//
//  アドレスレジスタ間接ディスプレースメント付き
//    (d16,Ar)
//    メモリ制御実効アドレスとしてパースする
//
//  アドレスレジスタ間接インデックス付き
//    (d8,Ar,Rn.wl)
//    メモリ制御実効アドレスとしてパースする
//
//  絶対ショート
//    (xxx).W
//    '('で始まっていないとき
//      式をパースして数値(0～16bit)または数値.Wならば絶対ショートとみなす
//    '('で始まっているとき
//      メモリ制御実効アドレスとしてパースする
//
//  絶対ロング
//    (xxx).L
//    '('で始まっていないとき
//      式をパースして数値(17～32bit)または数値.Lならば絶対ロングとみなす
//    '('で始まっているとき
//      メモリ制御実効アドレスとしてパースする
//
//  プログラムカウンタ間接ディスプレースメント付き
//    (d16,PC)
//    メモリ制御実効アドレスとしてパースする
//
//  プログラムカウンタ間接インデックス付き
//    (d8,PC,Rn.wl)
//    メモリ制御実効アドレスとしてパースする
//
//  メモリ制御実効アドレス
//    (x) (x,y) (x,y,z) x(y) x(y,z)
//    x(～)のxを除く残りのどれかがベースレジスタまたはベースメモリ間接
//      左から調べる
//      ベースレジスタまたはベースメモリ間接がないとき
//        ZA0が省略されているものとみなす
//    ベースレジスタまたはベースメモリ間接のプリインデックスがZD0.W*1のとき
//      x(～)のxを除く残りのどれかがポストインデックス
//      ポストインデックスがないとき
//        残りが2個のとき
//          メモリ制御実効アドレスではない
//        残りが1個以下のとき
//          ZD0.W*1が省略されているものとみなす
//      ベースレジスタとポストインデックスを入れ換えても同じときは左に書いた方がベースレジスタになる
//    残り1個以下がアウタディスプレースメント
//      アウタディスプレースメントがないとき
//        0.L(可変長)が省略されているものとみなす
//    インデックスサプレス
//      プリインデックスがZD0.W*1かつポストインデックスがZD0.W*1
//    フルフォーマット以外への変更
//      最適化の過程でフルフォーマットに戻さなければならなくなる場合があるので、
//      最適化中はサイズの計算だけフルフォーマット以外に変更する
//      (Ar,ZD0.W*1) → (Ar)
//        アドレスレジスタ間接
//        条件
//          ベースレジスタがアドレスレジスタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが0bit
//      (Ar,ZD0.W*1,od.W) → (d16,Ar)
//        アドレスレジスタ間接ディスプレースメント付き
//        条件
//          ベースレジスタがアドレスレジスタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが1～16bit
//      (Ar,Rn.wl*s,od.W) → (d8,Ar,Rn.wl)
//        アドレスレジスタ間接インデックス付き
//        条件
//          ベースレジスタがアドレスレジスタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスなし
//          アウタディスプレースメントが0～8bit
//      (ZA0,ZD0.W*1,od.W) → (xxx).W
//        絶対ショート
//        条件
//          ベースレジスタサプレスあり
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが1～16bit
//      (ZA0,ZD0.W*1,od.L) → (xxx).L
//        絶対ロング
//        条件
//          ベースレジスタサプレスあり
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが17～32bit
//      (PC,ZD0.W*1,od.W) → (d16,PC)
//      (OPC,ZD0.W*1,od.W) → (d16,PC)
//        プログラムカウンタ間接ディスプレースメント付き
//        条件
//          ベースレジスタがプログラムカウンタまたはオプショナルプログラムカウンタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが1～16bit
//      (OPC,ZD0.W*1,od.L) → (xxx).L
//        絶対ロング
//        条件
//          ベースレジスタがオプショナルプログラムカウンタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスあり
//          アウタディスプレースメントが17～32bit
//      (PC,Rn.wl*s,od.W) → (d8,PC,Rn.wl)
//        プログラムカウンタ間接インデックス付き
//        条件
//          ベースレジスタがプログラムカウンタ
//          ベースレジスタサプレスなし
//          メモリ間接なし
//          インデックスサプレスなし
//          アウタディスプレースメントが0～8bit
//
//  ベースメモリ間接
//    [x] [x,y] [x,y,z]
//    残り3個以下のどれかがベースレジスタ
//      左から調べる
//      ベースレジスタがないとき
//        残りが3個のとき
//          ベースメモリ間接ではない
//        残りが2個以下のとき
//          ZA0が省略されているものとみなす
//    残り2個以下のどれかがプリインデックス
//      左から調べる
//      プリインデックスがないとき
//        残りが2個のとき
//          ベースメモリ間接ではない
//        残りが1個以下のとき
//          ZD0.W*1が省略されているものとみなす
//      ベースレジスタとプリインデックスを入れ換えても同じときは左に書いた方がベースレジスタになる
//    残り1個以下がベースディスプレースメント
//      ベースディスプレースメントがないとき
//        0.L(可変長)が省略されているものとみなす
//
//  ベースディスプレースメントとアウタディスプレースメント
//    数値    ロング(可変長)
//    数値.W  ワード(固定長)
//    数値.L  ロング(固定長)
//
//  プリインデックスとポストインデックス
//    インデックスレジスタ      インデックスレジスタ.W*1とみなす
//    インデックスレジスタ.W    インデックスレジスタ.W*1とみなす
//    インデックスレジスタ.W*1
//    インデックスレジスタ.W*2
//    インデックスレジスタ.W*4
//    インデックスレジスタ.W*8
//    インデックスレジスタ.L    インデックスレジスタ.L*1とみなす
//    インデックスレジスタ.L*1
//    インデックスレジスタ.L*2
//    インデックスレジスタ.L*4
//    インデックスレジスタ.L*8
//
//  ベースレジスタ
//    アドレスレジスタ
//    サプレスされたアドレスレジスタ
//    プログラムカウンタ
//    サプレスされたプログラムカウンタ
//    オプショナルなプログラムカウンタ
//
//  インデックスレジスタ
//    データレジスタ
//    サプレスされたデータレジスタ
//    アドレスレジスタ
//    サプレスされたアドレスレジスタ
//
//  レジスタ
//    汎用レジスタ
//    プログラムカウンタ
//    コンディションコードレジスタ
//    ステータスレジスタ
//    制御レジスタ
//    メモリ管理レジスタ
//    浮動小数点レジスタ
//    サプレスされたデータレジスタ
//    サプレスされたアドレスレジスタ
//    サプレスされたプログラムカウンタ
//    オプショナルなプログラムカウンタ
//
//  汎用レジスタ
//    データレジスタ
//    アドレスレジスタ
//
//  データレジスタ
//                   012346  D0     Data Register 0
//                   012346  D1     Data Register 1
//                   012346  D2     Data Register 2
//                   012346  D3     Data Register 3
//                   012346  D4     Data Register 4
//                   012346  D5     Data Register 5
//                   012346  D6     Data Register 6
//                   012346  D7     Data Register 7
//
//  アドレスレジスタ
//                   012346  A0     Address Register 0
//                   012346  A1     Address Register 1
//                   012346  A2     Address Register 2
//                   012346  A3     Address Register 3
//                   012346  A4     Address Register 4
//                   012346  A5     Address Register 5
//                   012346  A6     Address Register 6
//                   012346  A7     Address Register 7
//                   012346  SP     Stack Pointer
//
//  プログラムカウンタ
//                   012346  PC     Program Counter
//
//  コンディションコードレジスタ
//                   012346  CCR    Condition Code Register
//
//  ステータスレジスタ
//                   012346  SR     Status Register
//
//  制御レジスタ
//    MOVEC.L  0000  -12346  SFC    Source Function Code Register
//    MOVEC.L  0001  -12346  DFC    Destination Function Code Register
//    MOVEC.L  0002  --2346  CACR   Cache Control Register
//    MOVEC.L  0003  ----46  TC     Translation Control Register (TCR)
//    MOVEC.L  0004  ----46  ITT0   Instruction Transparent Translation Register 0
//    MOVEC.L  0004  ----4-  IACR0  Instruction Access Control Register 0
//    MOVEC.L  0005  ----46  ITT1   Instruction Transparent Translation Register 1
//    MOVEC.L  0005  ----4-  IACR1  Instruction Access Control Register 1
//    MOVEC.L  0006  ----46  DTT0   Data Transparent Translation Register 0
//    MOVEC.L  0006  ----4-  DACR0  Data Access Control Register 0
//    MOVEC.L  0007  ----46  DTT1   Data Transparent Translation Register 1
//    MOVEC.L  0007  ----4-  DACR1  Data Access Control Register 1
//    MOVEC.L  0008  -----6  BUSCR  Bus Control Register
//    MOVEC.L  0800  -12346  USP    User Stack Pointer
//    MOVEC.L  0801  -12346  VBR    Vector Base Register
//    MOVEC.L  0802  --23--  CAAR   Cache Address Register
//    MOVEC.L  0803  --234-  MSP    Master Stack Pointer Register
//    MOVEC.L  0804  --2346  ISP    Interrupt Stack Pointer
//    MOVEC.L  0805  ----4-  MMUSR  Memory Management Unit Status Register
//    MOVEC.L  0806  ----46  URP    User Root Pointer
//    MOVEC.L  0807  ----46  SRP    Supervisor Root Pointer
//    MOVEC.L  0808  -----6  PCR    Processor Configuration Register
//
//  メモリ管理レジスタ
//    PMOVE.L  0800  ---3--  TT0    Transparent Translation Register 0
//    PMOVE.L  0800  ---E--  AC0    Access Control Register 0       (M68000PRMでは0400になっているが0800の間違い。ACUSRのInstruction FieldのNOTEにMC68851ではACUSRをPMMUSRと書きMC68030ではAC0とAC1をTT0とTT1と書くと書かれている)
//    PMOVE.L  0c00  ---3--  TT1    Transparent Translation Register 1
//    PMOVE.L  0c00  ---E--  AC1    Access Control Register 1
//    PMOVE.L  4000  --M3--  TC     Translation Control Register
//    PMOVE.Q  4400  --M---  DRP    DMA Root Pointer
//    PMOVE.Q  4800  --M3--  SRP    Supervisor Root Pointer
//    PMOVE.Q  4c00  --M3--  CRP    CPU Root Pointer
//    PMOVE.B  5000  --M---  CAL    Current Access Level Register
//    PMOVE.B  5400  --M---  VAL    Valid Access Level Register
//    PMOVE.B  5800  --M---  SCC    Stack Change Control Register
//    PMOVE.W  5c00  --M---  AC     Access Control Register
//    PMOVE.W  6000  --M---  PSR    PMMU Status Register (PMMUSR)
//    PMOVE.W  6000  ---3--  MMUSR  Memory Management Unit Status Register
//    PMOVE.W  6000  ---E--  ACUSR  Access Control Unit Status Register
//    PMOVE.W  6400  --M---  PCSR   PMMU Cache Status Register
//    PMOVE.W  7000  --M---  BAD0   Breakpoint Acknowledge Data 0
//    PMOVE.W  7004  --M---  BAD1   Breakpoint Acknowledge Data 1
//    PMOVE.W  7008  --M---  BAD2   Breakpoint Acknowledge Data 2
//    PMOVE.W  700c  --M---  BAD3   Breakpoint Acknowledge Data 3
//    PMOVE.W  7010  --M---  BAD4   Breakpoint Acknowledge Data 4
//    PMOVE.W  7014  --M---  BAD5   Breakpoint Acknowledge Data 5
//    PMOVE.W  7018  --M---  BAD6   Breakpoint Acknowledge Data 6
//    PMOVE.W  701c  --M---  BAD7   Breakpoint Acknowledge Data 7
//    PMOVE.W  7400  --M---  BAC0   Breakpoint Acknowledge Control 0
//    PMOVE.W  7404  --M---  BAC1   Breakpoint Acknowledge Control 1
//    PMOVE.W  7408  --M---  BAC2   Breakpoint Acknowledge Control 2
//    PMOVE.W  740c  --M---  BAC3   Breakpoint Acknowledge Control 3
//    PMOVE.W  7410  --M---  BAC4   Breakpoint Acknowledge Control 4
//    PMOVE.W  7414  --M---  BAC5   Breakpoint Acknowledge Control 5
//    PMOVE.W  7418  --M---  BAC6   Breakpoint Acknowledge Control 6
//    PMOVE.W  741c  --M---  BAC7   Breakpoint Acknowledge Control 7
//
//  浮動小数点レジスタ
//                   --CC46  FP0    Floating-Point Register 0
//                   --CC46  FP1    Floating-Point Register 1
//                   --CC46  FP2    Floating-Point Register 2
//                   --CC46  FP3    Floating-Point Register 3
//                   --CC46  FP4    Floating-Point Register 4
//                   --CC46  FP5    Floating-Point Register 5
//                   --CC46  FP6    Floating-Point Register 6
//                   --CC46  FP7    Floating-Point Register 7
//    FMOVE.L 8400   --CC46  FPIAR  Floating-Point Instruction Address Register
//    FMOVE.L 8800   --CC46  FPSR   Floating-Point Status Register
//    FMOVE.L 9000   --CC46  FPCR   Floating-Point Control Register
//
//  サプレスされたデータレジスタ
//    ZD0 ZD1 ZD2 ZD3 ZD4 ZD5 ZD6 ZD7
//    データレジスタの先頭に'Z'を付ける
//
//  サプレスされたアドレスレジスタ
//    ZA0 ZA1 ZA2 ZA3 ZA4 ZA5 ZA6 ZA7 ZSP
//    アドレスレジスタの先頭に'Z'を付ける
//
//  サプレスされたプログラムカウンタ
//    ZPC
//    プログラムカウンタの先頭に'Z'を付ける
//
//  オプショナルなプログラムカウンタ
//    OPC
//    プログラムカウンタの先頭に'O'を付ける
//    ディスプレースメントが17～32bitのとき絶対ロングに変換される
//      (OPC,ZD0.W*1,od.W) → (d16,PC)
//      (OPC,ZD0.W*1,od.L) → (xxx).L
//
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class Assembler {

/*

!!!
!!! 工事中
!!!

  public static byte[] asmAssemble (int address, String source) {
    String mnemonic = "";
    switch (mnemonic) {



      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //ABCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_000_rrr
      //ABCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_001_rrr
    case "abcd":
      break;

      //ADD.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1101_qqq_000_mmm_rrr
      //ADD.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_001_mmm_rrr
      //ADD.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_010_mmm_rrr
      //ADD.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr [ADDA.W <ea>,Aq]
      //ADD.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_100_mmm_rrr
      //ADD.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_101_mmm_rrr
      //ADD.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_110_mmm_rrr
      //ADD.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr [ADDA.L <ea>,Aq]
    case "add":
      break;

      //ADDA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr
      //ADDA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr
    case "adda":
      break;

      //ADDI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_000_mmm_rrr-{data}
      //ADDI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_001_mmm_rrr-{data}
      //ADDI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_010_mmm_rrr-{data}
    case "addi":
      break;

      //ADDQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_000_mmm_rrr
      //ADDQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_001_mmm_rrr
      //ADDQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_001_001_rrr
      //ADDQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_010_mmm_rrr
      //ADDQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_010_001_rrr
    case "addq":
      break;

      //ADDX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_100_000_rrr
      //ADDX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_100_001_rrr
      //ADDX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_101_000_rrr
      //ADDX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_101_001_rrr
      //ADDX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_110_000_rrr
      //ADDX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_110_001_rrr
    case "addx":
      break;

      //ALINE #<data>                                   |-|012346|-|UUUUU|*****|          |1010_ddd_ddd_ddd_ddd (line 1010 emulator)
    case "aline":
      break;

      //AND.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_000_mmm_rrr-{data}  [ANDI.B #<data>,<ea>]
      //AND.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_001_mmm_rrr-{data}  [ANDI.W #<data>,<ea>]
      //AND.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_010_mmm_rrr-{data}  [ANDI.L #<data>,<ea>]
      //AND.B <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_000_mmm_rrr
      //AND.W <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_001_mmm_rrr
      //AND.L <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_010_mmm_rrr
      //AND.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_100_mmm_rrr
      //AND.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_101_mmm_rrr
      //AND.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_110_mmm_rrr
    case "and":
      break;

      //ANDI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_000_mmm_rrr-{data}
      //ANDI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_001_000_111_100-{data}
      //ANDI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_001_mmm_rrr-{data}
      //ANDI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_001_001_111_100-{data}
      //ANDI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_010_mmm_rrr-{data}
    case "andi":
      break;

      //ASL.B #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_100_000_rrr
      //ASL.B Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_100_100_rrr
      //ASL.W #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_101_000_rrr
      //ASL.W Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_101_100_rrr
      //ASL.L #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_110_000_rrr
      //ASL.L Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_110_100_rrr
      //ASL.W <ea>                                      |-|012346|-|UUUUU|*****|  M+-WXZ  |1110_000_111_mmm_rrr
      //ASL.B Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_100_000_rrr [ASL.B #1,Dr]
      //ASL.W Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_101_000_rrr [ASL.W #1,Dr]
      //ASL.L Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_110_000_rrr [ASL.L #1,Dr]
    case "asl":
      break;

      //ASR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_000_rrr
      //ASR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_100_rrr
      //ASR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_000_rrr
      //ASR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_100_rrr
      //ASR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_000_rrr
      //ASR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_100_rrr
      //ASR.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_000_011_mmm_rrr
      //ASR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_000_rrr [ASR.B #1,Dr]
      //ASR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_000_rrr [ASR.W #1,Dr]
      //ASR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_000_rrr [ASR.L #1,Dr]
    case "asr":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //BCC.W <label>                                   |-|012346|-|----*|-----|          |0110_010_000_000_000-{offset}
      //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)
      //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_001_sss_sss
      //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_010_sss_sss
      //BCC.S <label>                                   |-|01----|-|----*|-----|          |0110_010_011_sss_sss
      //BCC.S <label>                                   |-|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)
      //BCC.L <label>                                   |-|--2346|-|----*|-----|          |0110_010_011_111_111-{offset}
    case "bcc":
      break;

      //BCHG.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_101_000_rrr
      //BCHG.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_101_mmm_rrr
      //BCHG.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_001_000_rrr-{data}
      //BCHG.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_001_mmm_rrr-{data}
    case "bchg":
      break;

      //BCLR.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_110_000_rrr
      //BCLR.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_110_mmm_rrr
      //BCLR.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_010_000_rrr-{data}
      //BCLR.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_010_mmm_rrr-{data}
    case "bclr":
      break;

      //BCS.W <label>                                   |-|012346|-|----*|-----|          |0110_010_100_000_000-{offset}
      //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)
      //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_101_sss_sss
      //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_110_sss_sss
      //BCS.S <label>                                   |-|01----|-|----*|-----|          |0110_010_111_sss_sss
      //BCS.S <label>                                   |-|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)
      //BCS.L <label>                                   |-|--2346|-|----*|-----|          |0110_010_111_111_111-{offset}
    case "bcs":
      break;

      //BEQ.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}
      //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)
      //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_101_sss_sss
      //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_110_sss_sss
      //BEQ.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_111_sss_sss
      //BEQ.S <label>                                   |-|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)
      //BEQ.L <label>                                   |-|--2346|-|--*--|-----|          |0110_011_111_111_111-{offset}
    case "beq":
      break;

      //BFCHG <ea>{#o:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-00000ooooo0wwwww
      //BFCHG <ea>{#o:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-00000ooooo100www
      //BFCHG <ea>{Do:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000100ooo0wwwww
      //BFCHG <ea>{Do:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000100ooo100www
    case "bfchg":
      break;

      //BFCLR <ea>{#o:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-00000ooooo0wwwww
      //BFCLR <ea>{#o:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-00000ooooo100www
      //BFCLR <ea>{Do:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000100ooo0wwwww
      //BFCLR <ea>{Do:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000100ooo100www
    case "bfclr":
      break;

      //BFEXTS <ea>{#o:#w},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn0ooooo0wwwww
      //BFEXTS <ea>{#o:Dw},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn0ooooo100www
      //BFEXTS <ea>{Do:#w},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn100ooo0wwwww
      //BFEXTS <ea>{Do:Dw},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn100ooo100www
    case "bfexts":
      break;

      //BFEXTU <ea>{#o:#w},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn0ooooo0wwwww
      //BFEXTU <ea>{#o:Dw},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn0ooooo100www
      //BFEXTU <ea>{Do:#w},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn100ooo0wwwww
      //BFEXTU <ea>{Do:Dw},Dn                           |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn100ooo100www
    case "bfextu":
      break;

      //BFFFO <ea>{#o:#w},Dn                            |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn0ooooo0wwwww
      //BFFFO <ea>{#o:Dw},Dn                            |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn0ooooo100www
      //BFFFO <ea>{Do:#w},Dn                            |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn100ooo0wwwww
      //BFFFO <ea>{Do:Dw},Dn                            |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn100ooo100www
    case "bfffo":
      break;

      //BFINS Dn,<ea>{#o:#w}                            |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn0ooooo0wwwww
      //BFINS Dn,<ea>{#o:Dw}                            |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn0ooooo100www
      //BFINS Dn,<ea>{Do:#w}                            |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn100ooo0wwwww
      //BFINS Dn,<ea>{Do:Dw}                            |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn100ooo100www
    case "bfins":
      break;

      //BFSET <ea>{#o:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-00000ooooo0wwwww
      //BFSET <ea>{#o:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-00000ooooo100www
      //BFSET <ea>{Do:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000100ooo0wwwww
      //BFSET <ea>{Do:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000100ooo100www
    case "bfset":
      break;

      //BFTST <ea>{#o:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-00000ooooo0wwwww
      //BFTST <ea>{#o:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-00000ooooo100www
      //BFTST <ea>{Do:#w}                               |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000100ooo0wwwww
      //BFTST <ea>{Do:Dw}                               |-|--2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000100ooo100www
    case "bftst":
      break;

      //BGE.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}
      //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)
      //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_001_sss_sss
      //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_010_sss_sss
      //BGE.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_011_sss_sss
      //BGE.S <label>                                   |-|--2346|-|-*-*-|-----|          |0110_110_011_sss_sss (s is not equal to 63)
      //BGE.L <label>                                   |-|--2346|-|-*-*-|-----|          |0110_110_011_111_111-{offset}
    case "bge":
      break;

      //BGT.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}
      //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)
      //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_001_sss_sss
      //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_010_sss_sss
      //BGT.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_011_sss_sss
      //BGT.S <label>                                   |-|--2346|-|-***-|-----|          |0110_111_011_sss_sss (s is not equal to 63)
      //BGT.L <label>                                   |-|--2346|-|-***-|-----|          |0110_111_011_111_111-{offset}
    case "bgt":
      break;

      //BHI.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}
      //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)
      //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_001_sss_sss
      //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_010_sss_sss
      //BHI.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_011_sss_sss
      //BHI.S <label>                                   |-|--2346|-|--*-*|-----|          |0110_001_011_sss_sss (s is not equal to 63)
      //BHI.L <label>                                   |-|--2346|-|--*-*|-----|          |0110_001_011_111_111-{offset}
    case "bhi":
      break;

      //BHS.W <label>                                   |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //BHS.S <label>                                   |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //BHS.S <label>                                   |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //BHS.L <label>                                   |A|--2346|-|----*|-----|          |0110_010_011_111_111-{offset}        [BCC.L <label>]
    case "bhs":
      break;

      //BITREV.L Dr                                     |-|------|-|-----|-----|D         |0000_000_011_000_rrr (ISA_C)
    case "bitrev":
      break;

      //BKPT #<data>                                    |-|-12346|-|-----|-----|          |0100_100_001_001_ddd
    case "bkpt":
      break;

      //BLE.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}
      //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)
      //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_101_sss_sss
      //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_110_sss_sss
      //BLE.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_111_sss_sss
      //BLE.S <label>                                   |-|--2346|-|-***-|-----|          |0110_111_111_sss_sss (s is not equal to 63)
      //BLE.L <label>                                   |-|--2346|-|-***-|-----|          |0110_111_111_111_111-{offset}
    case "ble":
      break;

      //BLO.W <label>                                   |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //BLO.S <label>                                   |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //BLO.S <label>                                   |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
      //BLO.L <label>                                   |A|--2346|-|----*|-----|          |0110_010_111_111_111-{offset}        [BCS.L <label>]
    case "blo":
      break;

      //BLS.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}
      //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)
      //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_101_sss_sss
      //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_110_sss_sss
      //BLS.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_111_sss_sss
      //BLS.S <label>                                   |-|--2346|-|--*-*|-----|          |0110_001_111_sss_sss (s is not equal to 63)
      //BLS.L <label>                                   |-|--2346|-|--*-*|-----|          |0110_001_111_111_111-{offset}
    case "bls":
      break;

      //BLT.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}
      //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)
      //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_101_sss_sss
      //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_110_sss_sss
      //BLT.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_111_sss_sss
      //BLT.S <label>                                   |-|--2346|-|-*-*-|-----|          |0110_110_111_sss_sss (s is not equal to 63)
      //BLT.L <label>                                   |-|--2346|-|-*-*-|-----|          |0110_110_111_111_111-{offset}
    case "blt":
      break;

      //BMI.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}
      //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)
      //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_101_sss_sss
      //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_110_sss_sss
      //BMI.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_111_sss_sss
      //BMI.S <label>                                   |-|--2346|-|-*---|-----|          |0110_101_111_sss_sss (s is not equal to 63)
      //BMI.L <label>                                   |-|--2346|-|-*---|-----|          |0110_101_111_111_111-{offset}
    case "bmi":
      break;

      //BNCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //BNCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //BNCC.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
      //BNCC.L <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_111_111-{offset}        [BCS.L <label>]
    case "bncc":
      break;

      //BNCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //BNCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //BNCS.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //BNCS.L <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_111_111-{offset}        [BCC.L <label>]
    case "bncs":
      break;

      //BNE.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}
      //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)
      //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_001_sss_sss
      //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_010_sss_sss
      //BNE.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_011_sss_sss
      //BNE.S <label>                                   |-|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)
      //BNE.L <label>                                   |-|--2346|-|--*--|-----|          |0110_011_011_111_111-{offset}
    case "bne":
      break;

      //BNEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //BNEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //BNEQ.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
      //BNEQ.L <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_111_111-{offset}        [BNE.L <label>]
    case "bneq":
      break;

      //BNGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
      //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
      //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
      //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
      //BNGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
      //BNGE.S <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_111_sss_sss (s is not equal to 63)  [BLT.S <label>]
      //BNGE.L <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_111_111_111-{offset}        [BLT.L <label>]
    case "bnge":
      break;

      //BNGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
      //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
      //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
      //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
      //BNGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
      //BNGT.S <label>                                  |A|--2346|-|-***-|-----|          |0110_111_111_sss_sss (s is not equal to 63)  [BLE.S <label>]
      //BNGT.L <label>                                  |A|--2346|-|-***-|-----|          |0110_111_111_111_111-{offset}        [BLE.L <label>]
    case "bngt":
      break;

      //BNHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
      //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
      //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
      //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
      //BNHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
      //BNHI.S <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_111_sss_sss (s is not equal to 63)  [BLS.S <label>]
      //BNHI.L <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_111_111_111-{offset}        [BLS.L <label>]
    case "bnhi":
      break;

      //BNHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //BNHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //BNHS.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
      //BNHS.L <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_111_111-{offset}        [BCS.L <label>]
    case "bnhs":
      break;

      //BNLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
      //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
      //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
      //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
      //BNLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
      //BNLE.S <label>                                  |A|--2346|-|-***-|-----|          |0110_111_011_sss_sss (s is not equal to 63)  [BGT.S <label>]
      //BNLE.L <label>                                  |A|--2346|-|-***-|-----|          |0110_111_011_111_111-{offset}        [BGT.L <label>]
    case "bnle":
      break;

      //BNLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //BNLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //BNLO.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //BNLO.L <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_111_111-{offset}        [BCC.L <label>]
    case "bnlo":
      break;

      //BNLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
      //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
      //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
      //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
      //BNLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
      //BNLS.S <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_011_sss_sss (s is not equal to 63)  [BHI.S <label>]
      //BNLS.L <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_011_111_111-{offset}        [BHI.L <label>]
    case "bnls":
      break;

      //BNLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
      //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
      //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
      //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
      //BNLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
      //BNLT.S <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_011_sss_sss (s is not equal to 63)  [BGE.S <label>]
      //BNLT.L <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_011_111_111-{offset}        [BGE.L <label>]
    case "bnlt":
      break;

      //BNMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
      //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
      //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
      //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
      //BNMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
      //BNMI.S <label>                                  |A|--2346|-|-*---|-----|          |0110_101_011_sss_sss (s is not equal to 63)  [BPL.S <label>]
      //BNMI.L <label>                                  |A|--2346|-|-*---|-----|          |0110_101_011_111_111-{offset}        [BPL.L <label>]
    case "bnmi":
      break;

      //BNNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //BNNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //BNNE.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
      //BNNE.L <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_111_111-{offset}        [BEQ.L <label>]
    case "bnne":
      break;

      //BNNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //BNNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //BNNZ.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
      //BNNZ.L <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_111_111-{offset}        [BEQ.L <label>]
    case "bnnz":
      break;

      //BNPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
      //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
      //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
      //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
      //BNPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
      //BNPL.S <label>                                  |A|--2346|-|-*---|-----|          |0110_101_111_sss_sss (s is not equal to 63)  [BMI.S <label>]
      //BNPL.L <label>                                  |A|--2346|-|-*---|-----|          |0110_101_111_111_111-{offset}        [BMI.L <label>]
    case "bnpl":
      break;

      //BNVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
      //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
      //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
      //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
      //BNVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
      //BNVC.S <label>                                  |A|--2346|-|---*-|-----|          |0110_100_111_sss_sss (s is not equal to 63)  [BVS.S <label>]
      //BNVC.L <label>                                  |A|--2346|-|---*-|-----|          |0110_100_111_111_111-{offset}        [BVS.L <label>]
    case "bnvc":
      break;

      //BNVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
      //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
      //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
      //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
      //BNVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
      //BNVS.S <label>                                  |A|--2346|-|---*-|-----|          |0110_100_011_sss_sss (s is not equal to 63)  [BVC.S <label>]
      //BNVS.L <label>                                  |A|--2346|-|---*-|-----|          |0110_100_011_111_111-{offset}        [BVC.L <label>]
    case "bnvs":
      break;

      //BNZ.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //BNZ.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //BNZ.S <label>                                   |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
      //BNZ.L <label>                                   |A|--2346|-|--*--|-----|          |0110_011_011_111_111-{offset}        [BNE.L <label>]
    case "bnz":
      break;

      //BNZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //BNZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //BNZE.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
      //BNZE.L <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_111_111-{offset}        [BNE.L <label>]
    case "bnze":
      break;

      //BPL.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}
      //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)
      //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_001_sss_sss
      //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_010_sss_sss
      //BPL.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_011_sss_sss
      //BPL.S <label>                                   |-|--2346|-|-*---|-----|          |0110_101_011_sss_sss (s is not equal to 63)
      //BPL.L <label>                                   |-|--2346|-|-*---|-----|          |0110_101_011_111_111-{offset}
    case "bpl":
      break;

      //BRA.W <label>                                   |-|012346|-|-----|-----|          |0110_000_000_000_000-{offset}
      //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)
      //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_001_sss_sss
      //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_010_sss_sss
      //BRA.S <label>                                   |-|01----|-|-----|-----|          |0110_000_011_sss_sss
      //BRA.S <label>                                   |-|--2346|-|-----|-----|          |0110_000_011_sss_sss (s is not equal to 63)
      //BRA.L <label>                                   |-|--2346|-|-----|-----|          |0110_000_011_111_111-{offset}
    case "bra":
      break;

      //BSET.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_111_000_rrr
      //BSET.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_111_mmm_rrr
      //BSET.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_011_000_rrr-{data}
      //BSET.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_011_mmm_rrr-{data}
    case "bset":
      break;

      //BSR.W <label>                                   |-|012346|-|-----|-----|          |0110_000_100_000_000-{offset}
      //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)
      //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_101_sss_sss
      //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_110_sss_sss
      //BSR.S <label>                                   |-|01----|-|-----|-----|          |0110_000_111_sss_sss
      //BSR.S <label>                                   |-|--2346|-|-----|-----|          |0110_000_111_sss_sss (s is not equal to 63)
      //BSR.L <label>                                   |-|--2346|-|-----|-----|          |0110_000_111_111_111-{offset}
    case "bsr":
      break;

      //BTST.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_100_000_rrr
      //BTST.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZPI|0000_qqq_100_mmm_rrr
      //BTST.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_000_000_rrr-{data}
      //BTST.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZP |0000_100_000_mmm_rrr-{data}
    case "btst":
      break;

      //BVC.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}
      //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)
      //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_001_sss_sss
      //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_010_sss_sss
      //BVC.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_011_sss_sss
      //BVC.S <label>                                   |-|--2346|-|---*-|-----|          |0110_100_011_sss_sss (s is not equal to 63)
      //BVC.L <label>                                   |-|--2346|-|---*-|-----|          |0110_100_011_111_111-{offset}
    case "bvc":
      break;

      //BVS.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}
      //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)
      //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_101_sss_sss
      //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_110_sss_sss
      //BVS.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_111_sss_sss
      //BVS.S <label>                                   |-|--2346|-|---*-|-----|          |0110_100_111_sss_sss (s is not equal to 63)
      //BVS.L <label>                                   |-|--2346|-|---*-|-----|          |0110_100_111_111_111-{offset}
    case "bvs":
      break;

      //BYTEREV.L Dr                                    |-|------|-|-----|-----|D         |0000_001_011_000_rrr (ISA_C)
    case "byterev":
      break;

      //BZE.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //BZE.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //BZE.S <label>                                   |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
      //BZE.L <label>                                   |A|--2346|-|--*--|-----|          |0110_011_111_111_111-{offset}        [BEQ.L <label>]
    case "bze":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //CALLM #<data>,<ea>                              |-|--2---|-|-----|-----|  M  WXZP |0000_011_011_mmm_rrr-00000000dddddddd
    case "callm":
      break;

      //CAS.B Dc,Du,<ea>                                |-|--2346|-|-UUUU|-****|  M+-WXZ  |0000_101_011_mmm_rrr-0000000uuu000ccc
      //CAS.W Dc,Du,<ea>                                |-|--2346|-|-UUUU|-****|  M+-WXZ  |0000_110_011_mmm_rrr-0000000uuu000ccc        (68060 software emulate misaligned <ea>)
      //CAS.L Dc,Du,<ea>                                |-|--2346|-|-UUUU|-****|  M+-WXZ  |0000_111_011_mmm_rrr-0000000uuu000ccc        (68060 software emulate misaligned <ea>)
    case "cas":
      break;

      //CAS2.W Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)              |-|--234S|-|-UUUU|-****|          |0000_110_011_111_100-rnnn000uuu000ccc(1)-rnnn_000_uuu_000_ccc(2)
      //CAS2.L Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)              |-|--234S|-|-UUUU|-****|          |0000_111_011_111_100-rnnn000uuu000ccc(1)-rnnn_000_uuu_000_ccc(2)
    case "cas2":
      break;

      //CHK.L <ea>,Dq                                   |-|--2346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_100_mmm_rrr
      //CHK.W <ea>,Dq                                   |-|012346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_110_mmm_rrr
    case "chk":
      break;

      //CHK2.B <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn100000000000
      //CHK2.W <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn100000000000
      //CHK2.L <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn100000000000
    case "chk2":
      break;

      //CINVA NC                                        |-|----46|P|-----|-----|          |1111_010_000_011_000
      //CINVA DC                                        |-|----46|P|-----|-----|          |1111_010_001_011_000
      //CINVA IC                                        |-|----46|P|-----|-----|          |1111_010_010_011_000
      //CINVA BC                                        |-|----46|P|-----|-----|          |1111_010_011_011_000
    case "cinva":
      break;

      //CINVL NC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_000_001_rrr
      //CINVL DC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_001_001_rrr
      //CINVL IC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_010_001_rrr
      //CINVL BC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_011_001_rrr
    case "cinvl":
      break;

      //CINVP NC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_000_010_rrr
      //CINVP DC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_001_010_rrr
      //CINVP IC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_010_010_rrr
      //CINVP BC,(Ar)                                   |-|----46|P|-----|-----|          |1111_010_011_010_rrr
    case "cinvp":
      break;

      //CLR.B <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_000_mmm_rrr (68000 and 68008 read before clear)
      //CLR.W <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_001_mmm_rrr (68000 and 68008 read before clear)
      //CLR.L <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_010_mmm_rrr (68000 and 68008 read before clear)
      //CLR.W Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_011_001_rrr [SUBA.W Ar,Ar]
      //CLR.L Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_111_001_rrr [SUBA.L Ar,Ar]
    case "clr":
      break;

      //CMP.B #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_000_mmm_rrr-{data}  [CMPI.B #<data>,<ea>]
      //CMP.B #<data>,<ea>                              |A|--2346|-|-UUUU|-****|  M+-WXZP |0000_110_000_mmm_rrr-{data}  [CMPI.B #<data>,<ea>]
      //CMP.W #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_001_mmm_rrr-{data}  [CMPI.W #<data>,<ea>]
      //CMP.W #<data>,<ea>                              |A|--2346|-|-UUUU|-****|  M+-WXZP |0000_110_001_mmm_rrr-{data}  [CMPI.W #<data>,<ea>]
      //CMP.L #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_010_mmm_rrr-{data}  [CMPI.L #<data>,<ea>]
      //CMP.L #<data>,<ea>                              |A|--2346|-|-UUUU|-****|  M+-WXZP |0000_110_010_mmm_rrr-{data}  [CMPI.L #<data>,<ea>]
      //CMP.B <ea>,Dq                                   |-|012346|-|-UUUU|-****|D M+-WXZPI|1011_qqq_000_mmm_rrr
      //CMP.W <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_001_mmm_rrr
      //CMP.L <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_010_mmm_rrr
      //CMP.W <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr [CMPA.W <ea>,Aq]
      //CMP.L <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr [CMPA.L <ea>,Aq]
    case "cmp":
      break;

      //CMP2.B <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn000000000000
      //CMP2.W <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn000000000000
      //CMP2.L <ea>,Rn                                  |-|--234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn000000000000
    case "cmp2":
      break;

      //CMPA.W <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr
      //CMPA.L <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr
    case "cmpa":
      break;

      //CMPI.B #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_000_mmm_rrr-{data}
      //CMPI.B #<data>,<ea>                             |-|--2346|-|-UUUU|-****|D M+-WXZP |0000_110_000_mmm_rrr-{data}
      //CMPI.W #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_001_mmm_rrr-{data}
      //CMPI.W #<data>,<ea>                             |-|--2346|-|-UUUU|-****|D M+-WXZP |0000_110_001_mmm_rrr-{data}
      //CMPI.L #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_010_mmm_rrr-{data}
      //CMPI.L #<data>,<ea>                             |-|--2346|-|-UUUU|-****|D M+-WXZP |0000_110_010_mmm_rrr-{data}
    case "cmpi":
      break;

      //CMPM.B (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_100_001_rrr
      //CMPM.W (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_101_001_rrr
      //CMPM.L (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_110_001_rrr
    case "cmpm":
      break;

      //CPUSHA NC                                       |-|----46|P|-----|-----|          |1111_010_000_111_000
      //CPUSHA DC                                       |-|----46|P|-----|-----|          |1111_010_001_111_000
      //CPUSHA IC                                       |-|----46|P|-----|-----|          |1111_010_010_111_000
      //CPUSHA BC                                       |-|----46|P|-----|-----|          |1111_010_011_111_000
    case "cpusha":
      break;

      //CPUSHL NC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_000_101_rrr
      //CPUSHL DC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_001_101_rrr
      //CPUSHL IC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_010_101_rrr
      //CPUSHL BC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_011_101_rrr
    case "cpushl":
      break;

      //CPUSHP NC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_000_110_rrr
      //CPUSHP DC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_001_110_rrr
      //CPUSHP IC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_010_110_rrr
      //CPUSHP BC,(Ar)                                  |-|----46|P|-----|-----|          |1111_010_011_110_rrr
    case "cpushp":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //DBCC.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}
    case "dbcc":
      break;

      //DBCS.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}
    case "dbcs":
      break;

      //DBEQ.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}
    case "dbeq":
      break;

      //DBF.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}
    case "dbf":
      break;

      //DBGE.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}
    case "dbge":
      break;

      //DBGT.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}
    case "dbgt":
      break;

      //DBHI.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}
    case "dbhi":
      break;

      //DBHS.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
    case "dbhs":
      break;

      //DBLE.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}
    case "dble":
      break;

      //DBLO.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
    case "dblo":
      break;

      //DBLS.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}
    case "dbls":
      break;

      //DBLT.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}
    case "dblt":
      break;

      //DBMI.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}
    case "dbmi":
      break;

      //DBNCC.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
    case "dbncc":
      break;

      //DBNCS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
    case "dbncs":
      break;

      //DBNE.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}
    case "dbne":
      break;

      //DBNEQ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
    case "dbneq":
      break;

      //DBNF.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}        [DBT.W Dr,<label>]
    case "dbnf":
      break;

      //DBNGE.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}        [DBLT.W Dr,<label>]
    case "dbnge":
      break;

      //DBNGT.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}        [DBLE.W Dr,<label>]
    case "dbngt":
      break;

      //DBNHI.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}        [DBLS.W Dr,<label>]
    case "dbnhi":
      break;

      //DBNHS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
    case "dbnhs":
      break;

      //DBNLE.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}        [DBGT.W Dr,<label>]
    case "dbnle":
      break;

      //DBNLO.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
    case "dbnlo":
      break;

      //DBNLS.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}        [DBHI.W Dr,<label>]
    case "dbnls":
      break;

      //DBNLT.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}        [DBGE.W Dr,<label>]
    case "dbnlt":
      break;

      //DBNMI.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}        [DBPL.W Dr,<label>]
    case "dbnmi":
      break;

      //DBNNE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
    case "dbnne":
      break;

      //DBNNZ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
    case "dbnnz":
      break;

      //DBNPL.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}        [DBMI.W Dr,<label>]
    case "dbnpl":
      break;

      //DBNT.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
    case "dbnt":
      break;

      //DBNVC.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}        [DBVS.W Dr,<label>]
    case "dbnvc":
      break;

      //DBNVS.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}        [DBVC.W Dr,<label>]
    case "dbnvs":
      break;

      //DBNZ.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
    case "dbnz":
      break;

      //DBNZE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
    case "dbnze":
      break;

      //DBPL.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}
    case "dbpl":
      break;

      //DBRA.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
    case "dbra":
      break;

      //DBT.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}
    case "dbt":
      break;

      //DBVC.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}
    case "dbvc":
      break;

      //DBVS.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}
    case "dbvs":
      break;

      //DBZE.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
    case "dbze":
      break;

      //DEC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_100_mmm_rrr [SUBQ.B #1,<ea>]
      //DEC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_101_mmm_rrr [SUBQ.W #1,<ea>]
      //DEC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_101_001_rrr [SUBQ.W #1,Ar]
      //DEC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_110_mmm_rrr [SUBQ.L #1,<ea>]
      //DEC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_110_001_rrr [SUBQ.L #1,Ar]
    case "dec":
      break;

      //DIVS.L <ea>,Dq                                  |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq100000000qqq
      //DIVS.L <ea>,Dr:Dq                               |-|--234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq110000000rrr        (q is not equal to r)
      //DIVS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_111_mmm_rrr
    case "divs":
      break;

      //DIVSL.L <ea>,Dr:Dq                              |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq100000000rrr        (q is not equal to r)
    case "divsl":
      break;

      //DIVU.L <ea>,Dq                                  |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq000000000qqq
      //DIVU.L <ea>,Dr:Dq                               |-|--234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq010000000rrr        (q is not equal to r)
      //DIVU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_011_mmm_rrr
    case "divu":
      break;

      //DIVUL.L <ea>,Dr:Dq                              |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq000000000rrr        (q is not equal to r)
    case "divul":
      break;

      //DOS <data>                                      |A|012346|-|UUUUU|UUUUU|          |1111_111_1dd_ddd_ddd [FLINE #<data>]
    case "dos":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //EOR.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}  [EORI.B #<data>,<ea>]
      //EOR.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}  [EORI.W #<data>,<ea>]
      //EOR.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}  [EORI.L #<data>,<ea>]
      //EOR.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_100_mmm_rrr
      //EOR.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_101_mmm_rrr
      //EOR.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_110_mmm_rrr
    case "eor":
      break;

      //EORI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}
      //EORI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_101_000_111_100-{data}
      //EORI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}
      //EORI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_101_001_111_100-{data}
      //EORI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}
    case "eori":
      break;

      //EXG.L Dq,Dr                                     |-|012346|-|-----|-----|          |1100_qqq_101_000_rrr
      //EXG.L Aq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_101_001_rrr
      //EXG.L Dq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_110_001_rrr
    case "exg":
      break;

      //EXT.W Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_010_000_rrr
      //EXT.L Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_011_000_rrr
    case "ext":
      break;

      //EXTB.L Dr                                       |-|--2346|-|-UUUU|-**00|D         |0100_100_111_000_rrr
    case "extb":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //FABS.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011000
      //FABS.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011000
      //FABS.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011000
      //FABS.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011000
      //FABS.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011000
      //FABS.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011000
      //FABS.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011000
      //FABS.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011000
    case "fabs":
      break;

      //FACOS.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011100
      //FACOS.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011100
      //FACOS.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011100
      //FACOS.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011100
      //FACOS.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011100
      //FACOS.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011100
      //FACOS.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011100
      //FACOS.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011100
    case "facos":
      break;

      //FADD.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100010
      //FADD.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100010
      //FADD.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100010
      //FADD.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100010
      //FADD.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100010
      //FADD.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100010
      //FADD.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100010
      //FADD.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100010
    case "fadd":
      break;

      //FASIN.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001100
      //FASIN.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001100
      //FASIN.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001100
      //FASIN.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001100
      //FASIN.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001100
      //FASIN.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001100
      //FASIN.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001100
      //FASIN.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001100
    case "fasin":
      break;

      //FATAN.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001010
      //FATAN.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001010
      //FATAN.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001010
      //FATAN.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001010
      //FATAN.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001010
      //FATAN.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001010
      //FATAN.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001010
      //FATAN.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001010
    case "fatan":
      break;

      //FATANH.X FPm,FPn                                |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001101
      //FATANH.L <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001101
      //FATANH.S <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001101
      //FATANH.W <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001101
      //FATANH.B <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001101
      //FATANH.X <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001101
      //FATANH.P <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001101
      //FATANH.D <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001101
    case "fatanh":
      break;

      //FBEQ.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_000_001-{offset}
      //FBEQ.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_000_001-{offset}
    case "fbeq":
      break;

      //FBF.W <label>                                   |-|--CC46|-|-----|-----|          |1111_001_010_000_000-{offset}
      //FBF.L <label>                                   |-|--CC46|-|-----|-----|          |1111_001_011_000_000-{offset}
    case "fbf":
      break;

      //FBGE.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_011-{offset}
      //FBGE.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_011-{offset}
    case "fbge":
      break;

      //FBGL.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_110-{offset}
      //FBGL.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_110-{offset}
    case "fbgl":
      break;

      //FBGLE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_010_111-{offset}
      //FBGLE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_010_111-{offset}
    case "fbgle":
      break;

      //FBGT.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_010-{offset}
      //FBGT.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_010-{offset}
    case "fbgt":
      break;

      //FBLE.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_101-{offset}
      //FBLE.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_101-{offset}
    case "fble":
      break;

      //FBLT.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_100-{offset}
      //FBLT.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_100-{offset}
    case "fblt":
      break;

      //FBNE.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_001_110-{offset}
      //FBNE.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_001_110-{offset}
    case "fbne":
      break;

      //FBNGE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_100-{offset}
      //FBNGE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_100-{offset}
    case "fbnge":
      break;

      //FBNGL.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_001-{offset}
      //FBNGL.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_001-{offset}
    case "fbngl":
      break;

      //FBNGLE.W <label>                                |-|--CC46|-|-----|-----|          |1111_001_010_011_000-{offset}
      //FBNGLE.L <label>                                |-|--CC46|-|-----|-----|          |1111_001_011_011_000-{offset}
    case "fbngle":
      break;

      //FBNGT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_101-{offset}
      //FBNGT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_101-{offset}
    case "fbngt":
      break;

      //FBNLE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_010-{offset}
      //FBNLE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_010-{offset}
    case "fbnle":
      break;

      //FBNLT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_011-{offset}
      //FBNLT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_011-{offset}
    case "fbnlt":
      break;

      //FBOGE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_000_011-{offset}
      //FBOGE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_000_011-{offset}
    case "fboge":
      break;

      //FBOGL.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_000_110-{offset}
      //FBOGL.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_000_110-{offset}
    case "fbogl":
      break;

      //FBOGT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_000_010-{offset}
      //FBOGT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_000_010-{offset}
    case "fbogt":
      break;

      //FBOLE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_000_101-{offset}
      //FBOLE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_000_101-{offset}
    case "fbole":
      break;

      //FBOLT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_000_100-{offset}
      //FBOLT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_000_100-{offset}
    case "fbolt":
      break;

      //FBOR.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_000_111-{offset}
      //FBOR.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_000_111-{offset}
    case "fbor":
      break;

      //FBRA.W <label>                                  |A|--CC46|-|-----|-----|          |1111_001_010_001_111-{offset}        [FBT.W <label>]
      //FBRA.L <label>                                  |A|--CC46|-|-----|-----|          |1111_001_011_001_111-{offset}        [FBT.L <label>]
    case "fbra":
      break;

      //FBSEQ.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_010_001-{offset}
      //FBSEQ.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_010_001-{offset}
    case "fbseq":
      break;

      //FBSF.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_010_000-{offset}
      //FBSF.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_010_000-{offset}
    case "fbsf":
      break;

      //FBSNE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_011_110-{offset}
      //FBSNE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_011_110-{offset}
    case "fbsne":
      break;

      //FBST.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_011_111-{offset}
      //FBST.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_011_111-{offset}
    case "fbst":
      break;

      //FBT.W <label>                                   |-|--CC46|-|-----|-----|          |1111_001_010_001_111-{offset}
      //FBT.L <label>                                   |-|--CC46|-|-----|-----|          |1111_001_011_001_111-{offset}
    case "fbt":
      break;

      //FBUEQ.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_001_001-{offset}
      //FBUEQ.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_001_001-{offset}
    case "fbueq":
      break;

      //FBUGE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_001_011-{offset}
      //FBUGE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_001_011-{offset}
    case "fbuge":
      break;

      //FBUGT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_001_010-{offset}
      //FBUGT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_001_010-{offset}
    case "fbugt":
      break;

      //FBULE.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_001_101-{offset}
      //FBULE.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_001_101-{offset}
    case "fbule":
      break;

      //FBULT.W <label>                                 |-|--CC46|-|-----|-----|          |1111_001_010_001_100-{offset}
      //FBULT.L <label>                                 |-|--CC46|-|-----|-----|          |1111_001_011_001_100-{offset}
    case "fbult":
      break;

      //FBUN.W <label>                                  |-|--CC46|-|-----|-----|          |1111_001_010_001_000-{offset}
      //FBUN.L <label>                                  |-|--CC46|-|-----|-----|          |1111_001_011_001_000-{offset}
    case "fbun":
      break;

      //FCMP.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0111000
      //FCMP.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0111000
      //FCMP.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0111000
      //FCMP.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0111000
      //FCMP.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0111000
      //FCMP.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0111000
      //FCMP.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0111000
      //FCMP.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0111000
    case "fcmp":
      break;

      //FCOS.X FPm,FPn                                  |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011101
      //FCOS.L <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011101
      //FCOS.S <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011101
      //FCOS.W <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011101
      //FCOS.B <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011101
      //FCOS.X <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011101
      //FCOS.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011101
      //FCOS.D <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011101
    case "fcos":
      break;

      //FCOSH.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011001
      //FCOSH.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011001
      //FCOSH.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011001
      //FCOSH.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011001
      //FCOSH.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011001
      //FCOSH.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011001
      //FCOSH.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011001
      //FCOSH.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011001
    case "fcosh":
      break;

      //FDABS.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1011100
      //FDABS.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1011100
      //FDABS.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1011100
      //FDABS.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1011100
      //FDABS.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1011100
      //FDABS.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1011100
      //FDABS.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1011100
      //FDABS.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1011100
    case "fdabs":
      break;

      //FDADD.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100110
      //FDADD.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100110
      //FDADD.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100110
      //FDADD.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100110
      //FDADD.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100110
      //FDADD.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100110
      //FDADD.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100110
      //FDADD.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100110
    case "fdadd":
      break;

      //FDBEQ Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000001-{offset}
    case "fdbeq":
      break;

      //FDBF Dr,<label>                                 |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000000-{offset}
    case "fdbf":
      break;

      //FDBGE Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010011-{offset}
    case "fdbge":
      break;

      //FDBGL Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010110-{offset}
    case "fdbgl":
      break;

      //FDBGLE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010111-{offset}
    case "fdbgle":
      break;

      //FDBGT Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010010-{offset}
    case "fdbgt":
      break;

      //FDBLE Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010101-{offset}
    case "fdble":
      break;

      //FDBLT Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010100-{offset}
    case "fdblt":
      break;

      //FDBNE Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001110-{offset}
    case "fdbne":
      break;

      //FDBNGE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011100-{offset}
    case "fdbnge":
      break;

      //FDBNGL Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011001-{offset}
    case "fdbngl":
      break;

      //FDBNGLE Dr,<label>                              |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011000-{offset}
    case "fdbngle":
      break;

      //FDBNGT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011101-{offset}
    case "fdbngt":
      break;

      //FDBNLE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011010-{offset}
    case "fdbnle":
      break;

      //FDBNLT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011011-{offset}
    case "fdbnlt":
      break;

      //FDBOGE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000011-{offset}
    case "fdboge":
      break;

      //FDBOGL Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000110-{offset}
    case "fdbogl":
      break;

      //FDBOGT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000010-{offset}
    case "fdbogt":
      break;

      //FDBOLE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000101-{offset}
    case "fdbole":
      break;

      //FDBOLT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000100-{offset}
    case "fdbolt":
      break;

      //FDBOR Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000111-{offset}
    case "fdbor":
      break;

      //FDBRA Dr,<label>                                |A|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000000000-{offset}       [FDBF Dr,<label>]
    case "fdbra":
      break;

      //FDBSEQ Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010001-{offset}
    case "fdbseq":
      break;

      //FDBSF Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000010000-{offset}
    case "fdbsf":
      break;

      //FDBSNE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011110-{offset}
    case "fdbsne":
      break;

      //FDBST Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000011111-{offset}
    case "fdbst":
      break;

      //FDBT Dr,<label>                                 |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001111-{offset}
    case "fdbt":
      break;

      //FDBUEQ Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001001-{offset}
    case "fdbueq":
      break;

      //FDBUGE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001011-{offset}
    case "fdbuge":
      break;

      //FDBUGT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001010-{offset}
    case "fdbugt":
      break;

      //FDBULE Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001101-{offset}
    case "fdbule":
      break;

      //FDBULT Dr,<label>                               |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001100-{offset}
    case "fdbult":
      break;

      //FDBUN Dr,<label>                                |-|--CC4S|-|-----|-----|          |1111_001_001_001_rrr-0000000000001000-{offset}
    case "fdbun":
      break;

      //FDDIV.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100100
      //FDDIV.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100100
      //FDDIV.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100100
      //FDDIV.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100100
      //FDDIV.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100100
      //FDDIV.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100100
      //FDDIV.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100100
      //FDDIV.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100100
    case "fddiv":
      break;

      //FDIV.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100000
      //FDIV.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100000
      //FDIV.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100000
      //FDIV.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100000
      //FDIV.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100000
      //FDIV.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100000
      //FDIV.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100000
      //FDIV.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100000
    case "fdiv":
      break;

      //FDMOVE.X FPm,FPn                                |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1000100
      //FDMOVE.L <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1000100
      //FDMOVE.S <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1000100
      //FDMOVE.W <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1000100
      //FDMOVE.B <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1000100
      //FDMOVE.X <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1000100
      //FDMOVE.P <ea>,FPn                               |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1000100
      //FDMOVE.D <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1000100
    case "fdmove":
      break;

      //FDMUL.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100111
      //FDMUL.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100111
      //FDMUL.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100111
      //FDMUL.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100111
      //FDMUL.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100111
      //FDMUL.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100111
      //FDMUL.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100111
      //FDMUL.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100111
    case "fdmul":
      break;

      //FDNEG.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1011110
      //FDNEG.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1011110
      //FDNEG.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1011110
      //FDNEG.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1011110
      //FDNEG.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1011110
      //FDNEG.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1011110
      //FDNEG.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1011110
      //FDNEG.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1011110
    case "fdneg":
      break;

      //FDSQRT.X FPm,FPn                                |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1000101
      //FDSQRT.L <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1000101
      //FDSQRT.S <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1000101
      //FDSQRT.W <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1000101
      //FDSQRT.B <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1000101
      //FDSQRT.X <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1000101
      //FDSQRT.P <ea>,FPn                               |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1000101
      //FDSQRT.D <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1000101
    case "fdsqrt":
      break;

      //FDSUB.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1101100
      //FDSUB.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1101100
      //FDSUB.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1101100
      //FDSUB.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1101100
      //FDSUB.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1101100
      //FDSUB.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1101100
      //FDSUB.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1101100
      //FDSUB.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1101100
    case "fdsub":
      break;

      //FETOX.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010000
      //FETOX.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010000
      //FETOX.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010000
      //FETOX.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010000
      //FETOX.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010000
      //FETOX.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010000
      //FETOX.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010000
      //FETOX.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010000
    case "fetox":
      break;

      //FETOXM1.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001000
      //FETOXM1.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001000
      //FETOXM1.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001000
      //FETOXM1.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001000
      //FETOXM1.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001000
      //FETOXM1.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001000
      //FETOXM1.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001000
      //FETOXM1.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001000
    case "fetoxm1":
      break;

      //FF1.L Dr                                        |-|------|-|-UUUU|-**00|D         |0000_010_011_000_rrr (ISA_C)
    case "ff1":
      break;

      //FGETEXP.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011110
      //FGETEXP.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011110
      //FGETEXP.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011110
      //FGETEXP.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011110
      //FGETEXP.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011110
      //FGETEXP.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011110
      //FGETEXP.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011110
      //FGETEXP.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011110
    case "fgetexp":
      break;

      //FGETMAN.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011111
      //FGETMAN.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011111
      //FGETMAN.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011111
      //FGETMAN.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011111
      //FGETMAN.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011111
      //FGETMAN.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011111
      //FGETMAN.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011111
      //FGETMAN.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011111
    case "fgetman":
      break;

      //FINT.X FPm,FPn                                  |-|--CCS6|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000001
      //FINT.L <ea>,FPn                                 |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000001
      //FINT.S <ea>,FPn                                 |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000001
      //FINT.W <ea>,FPn                                 |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000001
      //FINT.B <ea>,FPn                                 |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000001
      //FINT.X <ea>,FPn                                 |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000001
      //FINT.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000001
      //FINT.D <ea>,FPn                                 |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000001
    case "fint":
      break;

      //FINTRZ.X FPm,FPn                                |-|--CCS6|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000011
      //FINTRZ.L <ea>,FPn                               |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000011
      //FINTRZ.S <ea>,FPn                               |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000011
      //FINTRZ.W <ea>,FPn                               |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000011
      //FINTRZ.B <ea>,FPn                               |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000011
      //FINTRZ.X <ea>,FPn                               |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000011
      //FINTRZ.P <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000011
      //FINTRZ.D <ea>,FPn                               |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000011
    case "fintrz":
      break;

      //FLINE #<data>                                   |-|012346|-|UUUUU|UUUUU|          |1111_ddd_ddd_ddd_ddd (line 1111 emulator)
    case "fline":
      break;

      //FLOG10.X FPm,FPn                                |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010101
      //FLOG10.L <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010101
      //FLOG10.S <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010101
      //FLOG10.W <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010101
      //FLOG10.B <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010101
      //FLOG10.X <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010101
      //FLOG10.P <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010101
      //FLOG10.D <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010101
    case "flog10":
      break;

      //FLOG2.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010110
      //FLOG2.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010110
      //FLOG2.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010110
      //FLOG2.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010110
      //FLOG2.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010110
      //FLOG2.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010110
      //FLOG2.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010110
      //FLOG2.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010110
    case "flog2":
      break;

      //FLOGN.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010100
      //FLOGN.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010100
      //FLOGN.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010100
      //FLOGN.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010100
      //FLOGN.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010100
      //FLOGN.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010100
      //FLOGN.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010100
      //FLOGN.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010100
    case "flogn":
      break;

      //FLOGNP1.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000110
      //FLOGNP1.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000110
      //FLOGNP1.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000110
      //FLOGNP1.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000110
      //FLOGNP1.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000110
      //FLOGNP1.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000110
      //FLOGNP1.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000110
      //FLOGNP1.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000110
    case "flognp1":
      break;

      //FMOD.X FPm,FPn                                  |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100001
      //FMOD.L <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100001
      //FMOD.S <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100001
      //FMOD.W <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100001
      //FMOD.B <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100001
      //FMOD.X <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100001
      //FMOD.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100001
      //FMOD.D <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100001
    case "fmod":
      break;

      //FMOVE.X FPm,FPn                                 |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000000
      //FMOVE.L FPn,<ea>                                |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011000nnn0000000
      //FMOVE.S FPn,<ea>                                |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011001nnn0000000
      //FMOVE.W FPn,<ea>                                |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011100nnn0000000
      //FMOVE.B FPn,<ea>                                |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011110nnn0000000
      //FMOVE.L FPIAR,<ea>                              |-|--CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-1010010000000000
      //FMOVE.L FPSR,<ea>                               |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-1010100000000000
      //FMOVE.L FPCR,<ea>                               |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-1011000000000000
      //FMOVE.L <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000000
      //FMOVE.S <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000000
      //FMOVE.W <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000000
      //FMOVE.B <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000000
      //FMOVE.L <ea>,FPIAR                              |-|--CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-1000010000000000
      //FMOVE.L <ea>,FPSR                               |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-1000100000000000
      //FMOVE.L <ea>,FPCR                               |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-1001000000000000
      //FMOVE.X FPn,<ea>                                |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011010nnn0000000
      //FMOVE.P FPn,<ea>{#k}                            |-|--CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011011nnnkkkkkkk
      //FMOVE.D FPn,<ea>                                |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011101nnn0000000
      //FMOVE.P FPn,<ea>{Dk}                            |-|--CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011111nnnkkk0000
      //FMOVE.X <ea>,FPn                                |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000000
      //FMOVE.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000000
      //FMOVE.D <ea>,FPn                                |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000000
    case "fmove":
      break;

      //FMOVECR.X #ccc,FPn                              |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-010111nnn0cccccc
    case "fmovecr":
      break;

      //FMOVEM.L FPIAR,<ea>                             |-|--CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-1010010000000000
      //FMOVEM.L FPSR,<ea>                              |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-1010100000000000
      //FMOVEM.L FPCR,<ea>                              |-|--CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-1011000000000000
      //FMOVEM.L <ea>,FPIAR                             |-|--CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-1000010000000000
      //FMOVEM.L <ea>,FPSR                              |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-1000100000000000
      //FMOVEM.L <ea>,FPCR                              |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-1001000000000000
      //FMOVEM.L FPSR/FPIAR,<ea>                        |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-1010110000000000
      //FMOVEM.L FPCR/FPIAR,<ea>                        |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-1011010000000000
      //FMOVEM.L FPCR/FPSR,<ea>                         |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-1011100000000000
      //FMOVEM.L FPCR/FPSR/FPIAR,<ea>                   |-|--CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-1011110000000000
      //FMOVEM.X #<data>,<ea>                           |-|--CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-11110000dddddddd
      //FMOVEM.X <list>,<ea>                            |-|--CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-11110000llllllll
      //FMOVEM.X Dl,<ea>                                |-|--CC4S|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111110000lll0000
      //FMOVEM.L <ea>,FPSR/FPIAR                        |-|--CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-1000110000000000
      //FMOVEM.L <ea>,FPCR/FPIAR                        |-|--CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-1001010000000000
      //FMOVEM.L <ea>,FPCR/FPSR                         |-|--CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-1001100000000000
      //FMOVEM.L <ea>,FPCR/FPSR/FPIAR                   |-|--CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-1001110000000000
      //FMOVEM.X <ea>,#<data>                           |-|--CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-11010000dddddddd
      //FMOVEM.X <ea>,<list>                            |-|--CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-11010000llllllll
      //FMOVEM.X <ea>,Dl                                |-|--CC4S|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110110000lll0000
      //FMOVEM.X #<data>,-(Ar)                          |-|--CC46|-|-----|-----|    -     |1111_001_000_100_rrr-11100000dddddddd
      //FMOVEM.X <list>,-(Ar)                           |-|--CC46|-|-----|-----|    -     |1111_001_000_100_rrr-11100000llllllll
      //FMOVEM.X Dl,-(Ar)                               |-|--CC4S|-|-----|-----|    -     |1111_001_000_100_rrr-111010000lll0000
      //FMOVEM.L #<data>,#<data>,FPSR/FPIAR             |-|--CC4S|-|-----|-----|         I|1111_001_000_111_100-1000110000000000-{data}
      //FMOVEM.L #<data>,#<data>,FPCR/FPIAR             |-|--CC4S|-|-----|-----|         I|1111_001_000_111_100-1001010000000000-{data}
      //FMOVEM.L #<data>,#<data>,FPCR/FPSR              |-|--CC4S|-|-----|-----|         I|1111_001_000_111_100-1001100000000000-{data}
      //FMOVEM.L #<data>,#<data>,#<data>,FPCR/FPSR/FPIAR|-|--CC4S|-|-----|-----|         I|1111_001_000_111_100-1001110000000000-{data}
    case "fmovem":
      break;

      //FMUL.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100011
      //FMUL.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100011
      //FMUL.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100011
      //FMUL.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100011
      //FMUL.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100011
      //FMUL.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100011
      //FMUL.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100011
      //FMUL.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100011
    case "fmul":
      break;

      //FNEG.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0011010
      //FNEG.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0011010
      //FNEG.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0011010
      //FNEG.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0011010
      //FNEG.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0011010
      //FNEG.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0011010
      //FNEG.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0011010
      //FNEG.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0011010
    case "fneg":
      break;

      //FNOP                                            |A|--CC46|-|-----|-----|          |1111_001_010_000_000-0000000000000000        [FBF.W (*)+2]
    case "fnop":
      break;

      //FPACK <data>                                    |A|012346|-|UUUUU|*****|          |1111_111_0dd_ddd_ddd [FLINE #<data>]
    case "fpack":
      break;

      //FREM.X FPm,FPn                                  |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100101
      //FREM.L <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100101
      //FREM.S <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100101
      //FREM.W <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100101
      //FREM.B <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100101
      //FREM.X <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100101
      //FREM.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100101
      //FREM.D <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100101
    case "frem":
      break;

      //FRESTORE <ea>                                   |-|--CC46|P|-----|-----|  M+ WXZP |1111_001_101_mmm_rrr
    case "frestore":
      break;

      //FSABS.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1011000
      //FSABS.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1011000
      //FSABS.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1011000
      //FSABS.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1011000
      //FSABS.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1011000
      //FSABS.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1011000
      //FSABS.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1011000
      //FSABS.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1011000
    case "fsabs":
      break;

      //FSADD.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100010
      //FSADD.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100010
      //FSADD.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100010
      //FSADD.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100010
      //FSADD.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100010
      //FSADD.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100010
      //FSADD.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100010
      //FSADD.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100010
    case "fsadd":
      break;

      //FSAVE <ea>                                      |-|--CC46|P|-----|-----|  M -WXZ  |1111_001_100_mmm_rrr
    case "fsave":
      break;

      //FSCALE.X FPm,FPn                                |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100110
      //FSCALE.L <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100110
      //FSCALE.S <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100110
      //FSCALE.W <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100110
      //FSCALE.B <ea>,FPn                               |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100110
      //FSCALE.X <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100110
      //FSCALE.P <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100110
      //FSCALE.D <ea>,FPn                               |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100110
    case "fscale":
      break;

      //FSDIV.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100000
      //FSDIV.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100000
      //FSDIV.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100000
      //FSDIV.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100000
      //FSDIV.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100000
      //FSDIV.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100000
      //FSDIV.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100000
      //FSDIV.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100000
    case "fsdiv":
      break;

      //FSEQ.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000001
    case "fseq":
      break;

      //FSF.B <ea>                                      |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000000
    case "fsf":
      break;

      //FSGE.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010011
    case "fsge":
      break;

      //FSGL.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010110
    case "fsgl":
      break;

      //FSGLDIV.X FPm,FPn                               |-|--CCS6|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100100
      //FSGLDIV.L <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100100
      //FSGLDIV.S <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100100
      //FSGLDIV.W <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100100
      //FSGLDIV.B <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100100
      //FSGLDIV.X <ea>,FPn                              |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100100
      //FSGLDIV.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100100
      //FSGLDIV.D <ea>,FPn                              |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100100
    case "fsgldiv":
      break;

      //FSGLE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010111
    case "fsgle":
      break;

      //FSGLMUL.X FPm,FPn                               |-|--CCS6|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0100111
      //FSGLMUL.L <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0100111
      //FSGLMUL.S <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0100111
      //FSGLMUL.W <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0100111
      //FSGLMUL.B <ea>,FPn                              |-|--CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0100111
      //FSGLMUL.X <ea>,FPn                              |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0100111
      //FSGLMUL.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0100111
      //FSGLMUL.D <ea>,FPn                              |-|--CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0100111
    case "fsglmul":
      break;

      //FSGT.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010010
    case "fsgt":
      break;

      //FSIN.X FPm,FPn                                  |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001110
      //FSIN.L <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001110
      //FSIN.S <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001110
      //FSIN.W <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001110
      //FSIN.B <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001110
      //FSIN.X <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001110
      //FSIN.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001110
      //FSIN.D <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001110
    case "fsin":
      break;

      //FSINCOS.X FPm,FPc:FPs                           |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmsss0110ccc
      //FSINCOS.L <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000sss0110ccc
      //FSINCOS.S <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001sss0110ccc
      //FSINCOS.W <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100sss0110ccc
      //FSINCOS.B <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110sss0110ccc
      //FSINCOS.X <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010sss0110ccc
      //FSINCOS.P <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011sss0110ccc
      //FSINCOS.D <ea>,FPc:FPs                          |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101sss0110ccc
    case "fsincos":
      break;

      //FSINH.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000010
      //FSINH.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000010
      //FSINH.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000010
      //FSINH.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000010
      //FSINH.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000010
      //FSINH.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000010
      //FSINH.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000010
      //FSINH.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000010
    case "fsinh":
      break;

      //FSLE.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010101
    case "fsle":
      break;

      //FSLT.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010100
    case "fslt":
      break;

      //FSMOVE.X FPm,FPn                                |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1000000
      //FSMOVE.L <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1000000
      //FSMOVE.S <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1000000
      //FSMOVE.W <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1000000
      //FSMOVE.B <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1000000
      //FSMOVE.X <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1000000
      //FSMOVE.P <ea>,FPn                               |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1000000
      //FSMOVE.D <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1000000
    case "fsmove":
      break;

      //FSMUL.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1100011
      //FSMUL.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1100011
      //FSMUL.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1100011
      //FSMUL.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1100011
      //FSMUL.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1100011
      //FSMUL.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1100011
      //FSMUL.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1100011
      //FSMUL.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1100011
    case "fsmul":
      break;

      //FSNE.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001110
    case "fsne":
      break;

      //FSNEG.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1011010
      //FSNEG.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1011010
      //FSNEG.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1011010
      //FSNEG.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1011010
      //FSNEG.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1011010
      //FSNEG.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1011010
      //FSNEG.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1011010
      //FSNEG.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1011010
    case "fsneg":
      break;

      //FSNGE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011100
    case "fsnge":
      break;

      //FSNGL.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011001
    case "fsngl":
      break;

      //FSNGLE.B <ea>                                   |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011000
    case "fsngle":
      break;

      //FSNGT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011101
    case "fsngt":
      break;

      //FSNLE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011010
    case "fsnle":
      break;

      //FSNLT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011011
    case "fsnlt":
      break;

      //FSOGE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000011
    case "fsoge":
      break;

      //FSOGL.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000110
    case "fsogl":
      break;

      //FSOGT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000010
    case "fsogt":
      break;

      //FSOLE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000101
    case "fsole":
      break;

      //FSOLT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000100
    case "fsolt":
      break;

      //FSOR.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000000111
    case "fsor":
      break;

      //FSQRT.X FPm,FPn                                 |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0000100
      //FSQRT.L <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0000100
      //FSQRT.S <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0000100
      //FSQRT.W <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0000100
      //FSQRT.B <ea>,FPn                                |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0000100
      //FSQRT.X <ea>,FPn                                |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0000100
      //FSQRT.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0000100
      //FSQRT.D <ea>,FPn                                |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0000100
    case "fsqrt":
      break;

      //FSSEQ.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010001
    case "fsseq":
      break;

      //FSSF.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000010000
    case "fssf":
      break;

      //FSSNE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011110
    case "fssne":
      break;

      //FSSQRT.X FPm,FPn                                |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1000001
      //FSSQRT.L <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1000001
      //FSSQRT.S <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1000001
      //FSSQRT.W <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1000001
      //FSSQRT.B <ea>,FPn                               |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1000001
      //FSSQRT.X <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1000001
      //FSSQRT.P <ea>,FPn                               |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1000001
      //FSSQRT.D <ea>,FPn                               |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1000001
    case "fssqrt":
      break;

      //FSST.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000011111
    case "fsst":
      break;

      //FSSUB.X FPm,FPn                                 |-|----46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn1101000
      //FSSUB.L <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn1101000
      //FSSUB.S <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn1101000
      //FSSUB.W <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn1101000
      //FSSUB.B <ea>,FPn                                |-|----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn1101000
      //FSSUB.X <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn1101000
      //FSSUB.P <ea>,FPn                                |-|----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn1101000
      //FSSUB.D <ea>,FPn                                |-|----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn1101000
    case "fssub":
      break;

      //FST.B <ea>                                      |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001111
    case "fst":
      break;

      //FSUB.X FPm,FPn                                  |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0101000
      //FSUB.L <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0101000
      //FSUB.S <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0101000
      //FSUB.W <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0101000
      //FSUB.B <ea>,FPn                                 |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0101000
      //FSUB.X <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0101000
      //FSUB.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0101000
      //FSUB.D <ea>,FPn                                 |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0101000
    case "fsub":
      break;

      //FSUEQ.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001001
    case "fsueq":
      break;

      //FSUGE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001011
    case "fsuge":
      break;

      //FSUGT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001010
    case "fsugt":
      break;

      //FSULE.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001101
    case "fsule":
      break;

      //FSULT.B <ea>                                    |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001100
    case "fsult":
      break;

      //FSUN.B <ea>                                     |-|--CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-0000000000001000
    case "fsun":
      break;

      //FTAN.X FPm,FPn                                  |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001111
      //FTAN.L <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001111
      //FTAN.S <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001111
      //FTAN.W <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001111
      //FTAN.B <ea>,FPn                                 |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001111
      //FTAN.X <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001111
      //FTAN.P <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001111
      //FTAN.D <ea>,FPn                                 |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001111
    case "ftan":
      break;

      //FTANH.X FPm,FPn                                 |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0001001
      //FTANH.L <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0001001
      //FTANH.S <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0001001
      //FTANH.W <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0001001
      //FTANH.B <ea>,FPn                                |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0001001
      //FTANH.X <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0001001
      //FTANH.P <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0001001
      //FTANH.D <ea>,FPn                                |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0001001
    case "ftanh":
      break;

      //FTENTOX.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010010
      //FTENTOX.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010010
      //FTENTOX.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010010
      //FTENTOX.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010010
      //FTENTOX.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010010
      //FTENTOX.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010010
      //FTENTOX.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010010
      //FTENTOX.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010010
    case "ftentox":
      break;

      //FTRAPEQ.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000001-{data}
      //FTRAPEQ.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000001-{data}
      //FTRAPEQ                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000001
    case "ftrapeq":
      break;

      //FTRAPF.W #<data>                                |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000000-{data}
      //FTRAPF.L #<data>                                |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000000-{data}
      //FTRAPF                                          |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000000
    case "ftrapf":
      break;

      //FTRAPGE.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010011-{data}
      //FTRAPGE.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010011-{data}
      //FTRAPGE                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010011
    case "ftrapge":
      break;

      //FTRAPGL.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010110-{data}
      //FTRAPGL.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010110-{data}
      //FTRAPGL                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010110
    case "ftrapgl":
      break;

      //FTRAPGLE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010111-{data}
      //FTRAPGLE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010111-{data}
      //FTRAPGLE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010111
    case "ftrapgle":
      break;

      //FTRAPGT.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010010-{data}
      //FTRAPGT.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010010-{data}
      //FTRAPGT                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010010
    case "ftrapgt":
      break;

      //FTRAPLE.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010101-{data}
      //FTRAPLE.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010101-{data}
      //FTRAPLE                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010101
    case "ftraple":
      break;

      //FTRAPLT.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010100-{data}
      //FTRAPLT.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010100-{data}
      //FTRAPLT                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010100
    case "ftraplt":
      break;

      //FTRAPNE.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001110-{data}
      //FTRAPNE.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001110-{data}
      //FTRAPNE                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001110
    case "ftrapne":
      break;

      //FTRAPNGE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011100-{data}
      //FTRAPNGE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011100-{data}
      //FTRAPNGE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011100
    case "ftrapnge":
      break;

      //FTRAPNGL.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011001-{data}
      //FTRAPNGL.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011001-{data}
      //FTRAPNGL                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011001
    case "ftrapngl":
      break;

      //FTRAPNGLE.W #<data>                             |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011000-{data}
      //FTRAPNGLE.L #<data>                             |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011000-{data}
      //FTRAPNGLE                                       |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011000
    case "ftrapngle":
      break;

      //FTRAPNGT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011101-{data}
      //FTRAPNGT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011101-{data}
      //FTRAPNGT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011101
    case "ftrapngt":
      break;

      //FTRAPNLE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011010-{data}
      //FTRAPNLE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011010-{data}
      //FTRAPNLE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011010
    case "ftrapnle":
      break;

      //FTRAPNLT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011011-{data}
      //FTRAPNLT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011011-{data}
      //FTRAPNLT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011011
    case "ftrapnlt":
      break;

      //FTRAPOGE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000011-{data}
      //FTRAPOGE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000011-{data}
      //FTRAPOGE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000011
    case "ftrapoge":
      break;

      //FTRAPOGL.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000110-{data}
      //FTRAPOGL.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000110-{data}
      //FTRAPOGL                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000110
    case "ftrapogl":
      break;

      //FTRAPOGT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000010-{data}
      //FTRAPOGT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000010-{data}
      //FTRAPOGT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000010
    case "ftrapogt":
      break;

      //FTRAPOLE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000101-{data}
      //FTRAPOLE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000101-{data}
      //FTRAPOLE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000101
    case "ftrapole":
      break;

      //FTRAPOLT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000100-{data}
      //FTRAPOLT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000100-{data}
      //FTRAPOLT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000100
    case "ftrapolt":
      break;

      //FTRAPOR.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000000111-{data}
      //FTRAPOR.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000000111-{data}
      //FTRAPOR                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000000111
    case "ftrapor":
      break;

      //FTRAPSEQ.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010001-{data}
      //FTRAPSEQ.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010001-{data}
      //FTRAPSEQ                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010001
    case "ftrapseq":
      break;

      //FTRAPSF.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000010000-{data}
      //FTRAPSF.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000010000-{data}
      //FTRAPSF                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000010000
    case "ftrapsf":
      break;

      //FTRAPSNE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011110-{data}
      //FTRAPSNE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011110-{data}
      //FTRAPSNE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011110
    case "ftrapsne":
      break;

      //FTRAPST.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000011111-{data}
      //FTRAPST.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000011111-{data}
      //FTRAPST                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000011111
    case "ftrapst":
      break;

      //FTRAPT.W #<data>                                |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001111-{data}
      //FTRAPT.L #<data>                                |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001111-{data}
      //FTRAPT                                          |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001111
    case "ftrapt":
      break;

      //FTRAPUEQ.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001001-{data}
      //FTRAPUEQ.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001001-{data}
      //FTRAPUEQ                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001001
    case "ftrapueq":
      break;

      //FTRAPUGE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001011-{data}
      //FTRAPUGE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001011-{data}
      //FTRAPUGE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001011
    case "ftrapuge":
      break;

      //FTRAPUGT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001010-{data}
      //FTRAPUGT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001010-{data}
      //FTRAPUGT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001010
    case "ftrapugt":
      break;

      //FTRAPULE.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001101-{data}
      //FTRAPULE.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001101-{data}
      //FTRAPULE                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001101
    case "ftrapule":
      break;

      //FTRAPULT.W #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001100-{data}
      //FTRAPULT.L #<data>                              |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001100-{data}
      //FTRAPULT                                        |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001100
    case "ftrapult":
      break;

      //FTRAPUN.W #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_010-0000000000001000-{data}
      //FTRAPUN.L #<data>                               |-|--CC4S|-|-----|-----|          |1111_001_001_111_011-0000000000001000-{data}
      //FTRAPUN                                         |-|--CC4S|-|-----|-----|          |1111_001_001_111_100-0000000000001000
    case "ftrapun":
      break;

      //FTST.X FPm                                      |-|--CC46|-|-----|-----|          |1111_001_000_000_000-000mmm0000111010
      //FTST.L <ea>                                     |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-0100000000111010
      //FTST.S <ea>                                     |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-0100010000111010
      //FTST.W <ea>                                     |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-0101000000111010
      //FTST.B <ea>                                     |-|--CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-0101100000111010
      //FTST.X <ea>                                     |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-0100100000111010
      //FTST.P <ea>                                     |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-0100110000111010
      //FTST.D <ea>                                     |-|--CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-0101010000111010
    case "ftst":
      break;

      //FTWOTOX.X FPm,FPn                               |-|--CCSS|-|-----|-----|          |1111_001_000_000_000-000mmmnnn0010001
      //FTWOTOX.L <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010000nnn0010001
      //FTWOTOX.S <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010001nnn0010001
      //FTWOTOX.W <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010100nnn0010001
      //FTWOTOX.B <ea>,FPn                              |-|--CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010110nnn0010001
      //FTWOTOX.X <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010010nnn0010001
      //FTWOTOX.P <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010011nnn0010001
      //FTWOTOX.D <ea>,FPn                              |-|--CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010101nnn0010001
    case "ftwotox":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //ILLEGAL                                         |-|012346|-|-----|-----|          |0100_101_011_111_100
    case "illegal":
      break;

      //INC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_000_mmm_rrr [ADDQ.B #1,<ea>]
      //INC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_001_mmm_rrr [ADDQ.W #1,<ea>]
      //INC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_001_001_rrr [ADDQ.W #1,Ar]
      //INC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_010_mmm_rrr [ADDQ.L #1,<ea>]
      //INC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_010_001_rrr [ADDQ.L #1,Ar]
    case "inc":
      break;

      //IOCS <name>                                     |A|012346|-|UUUUU|UUUUU|          |0111_000_0dd_ddd_ddd-0100111001001111        [MOVEQ.L #<data>,D0;TRAP #15]
    case "iocs":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //JBCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //JBCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //JBCC.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //JBCC.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
    case "jbcc":
      break;

      //JBCS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
      //JBCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //JBCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //JBCS.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
    case "jbcs":
      break;

      //JBEQ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //JBEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //JBEQ.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
    case "jbeq":
      break;

      //JBGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
      //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
      //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
      //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
      //JBGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
      //JBGE.S <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_011_sss_sss (s is not equal to 63)  [BGE.S <label>]
      //JBGE.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
    case "jbge":
      break;

      //JBGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
      //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
      //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
      //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
      //JBGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
      //JBGT.S <label>                                  |A|--2346|-|-***-|-----|          |0110_111_011_sss_sss (s is not equal to 63)  [BGT.S <label>]
      //JBGT.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
    case "jbgt":
      break;

      //JBHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
      //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
      //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
      //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
      //JBHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
      //JBHI.S <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_011_sss_sss (s is not equal to 63)  [BHI.S <label>]
      //JBHI.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
    case "jbhi":
      break;

      //JBHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //JBHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //JBHS.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //JBHS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
    case "jbhs":
      break;

      //JBLE.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
      //JBLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
      //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
      //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
      //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
      //JBLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
      //JBLE.S <label>                                  |A|--2346|-|-***-|-----|          |0110_111_111_sss_sss (s is not equal to 63)  [BLE.S <label>]
    case "jble":
      break;

      //JBLO.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
      //JBLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //JBLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //JBLO.S <label>                                  |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
    case "jblo":
      break;

      //JBLS.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
      //JBLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
      //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
      //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
      //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
      //JBLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
      //JBLS.S <label>                                  |A|--2346|-|--*-*|-----|          |0110_001_111_sss_sss (s is not equal to 63)  [BLS.S <label>]
    case "jbls":
      break;

      //JBLT.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
      //JBLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
      //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
      //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
      //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
      //JBLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
      //JBLT.S <label>                                  |A|--2346|-|-*-*-|-----|          |0110_110_111_sss_sss (s is not equal to 63)  [BLT.S <label>]
    case "jblt":
      break;

      //JBMI.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
      //JBMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
      //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
      //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
      //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
      //JBMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
      //JBMI.S <label>                                  |A|--2346|-|-*---|-----|          |0110_101_111_sss_sss (s is not equal to 63)  [BMI.S <label>]
    case "jbmi":
      break;

      //JBNCC.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
      //JBNCC.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //JBNCC.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //JBNCC.S <label>                                 |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
    case "jbncc":
      break;

      //JBNCS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //JBNCS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //JBNCS.S <label>                                 |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //JBNCS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
    case "jbncs":
      break;

      //JBNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //JBNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //JBNE.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
      //JBNE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_110-0100111011111001-{address}      [BEQ.S (*)+8;JMP <label>]
    case "jbne":
      break;

      //JBNEQ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //JBNEQ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //JBNEQ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //JBNEQ.S <label>                                 |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
    case "jbneq":
      break;

      //JBNGE.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
      //JBNGE.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
      //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
      //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
      //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
      //JBNGE.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
      //JBNGE.S <label>                                 |A|--2346|-|-*-*-|-----|          |0110_110_111_sss_sss (s is not equal to 63)  [BLT.S <label>]
    case "jbnge":
      break;

      //JBNGT.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
      //JBNGT.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
      //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
      //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
      //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
      //JBNGT.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
      //JBNGT.S <label>                                 |A|--2346|-|-***-|-----|          |0110_111_111_sss_sss (s is not equal to 63)  [BLE.S <label>]
    case "jbngt":
      break;

      //JBNHI.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
      //JBNHI.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
      //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
      //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
      //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
      //JBNHI.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
      //JBNHI.S <label>                                 |A|--2346|-|--*-*|-----|          |0110_001_111_sss_sss (s is not equal to 63)  [BLS.S <label>]
    case "jbnhi":
      break;

      //JBNHS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
      //JBNHS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
      //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
      //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
      //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
      //JBNHS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
      //JBNHS.S <label>                                 |A|--2346|-|----*|-----|          |0110_010_111_sss_sss (s is not equal to 63)  [BCS.S <label>]
    case "jbnhs":
      break;

      //JBNLE.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
      //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
      //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
      //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
      //JBNLE.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
      //JBNLE.S <label>                                 |A|--2346|-|-***-|-----|          |0110_111_011_sss_sss (s is not equal to 63)  [BGT.S <label>]
      //JBNLE.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
    case "jbnle":
      break;

      //JBNLO.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
      //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
      //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
      //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
      //JBNLO.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
      //JBNLO.S <label>                                 |A|--2346|-|----*|-----|          |0110_010_011_sss_sss (s is not equal to 63)  [BCC.S <label>]
      //JBNLO.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
    case "jbnlo":
      break;

      //JBNLS.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
      //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
      //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
      //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
      //JBNLS.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
      //JBNLS.S <label>                                 |A|--2346|-|--*-*|-----|          |0110_001_011_sss_sss (s is not equal to 63)  [BHI.S <label>]
      //JBNLS.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
    case "jbnls":
      break;

      //JBNLT.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
      //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
      //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
      //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
      //JBNLT.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
      //JBNLT.S <label>                                 |A|--2346|-|-*-*-|-----|          |0110_110_011_sss_sss (s is not equal to 63)  [BGE.S <label>]
      //JBNLT.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
    case "jbnlt":
      break;

      //JBNMI.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
      //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
      //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
      //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
      //JBNMI.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
      //JBNMI.S <label>                                 |A|--2346|-|-*---|-----|          |0110_101_011_sss_sss (s is not equal to 63)  [BPL.S <label>]
      //JBNMI.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
    case "jbnmi":
      break;

      //JBNNE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBNNE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //JBNNE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //JBNNE.S <label>                                 |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
    case "jbnne":
      break;

      //JBNNZ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBNNZ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //JBNNZ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //JBNNZ.S <label>                                 |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
    case "jbnnz":
      break;

      //JBNPL.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
      //JBNPL.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
      //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
      //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
      //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
      //JBNPL.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
      //JBNPL.S <label>                                 |A|--2346|-|-*---|-----|          |0110_101_111_sss_sss (s is not equal to 63)  [BMI.S <label>]
    case "jbnpl":
      break;

      //JBNVC.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
      //JBNVC.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
      //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
      //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
      //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
      //JBNVC.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
      //JBNVC.S <label>                                 |A|--2346|-|---*-|-----|          |0110_100_111_sss_sss (s is not equal to 63)  [BVS.S <label>]
    case "jbnvc":
      break;

      //JBNVS.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
      //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
      //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
      //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
      //JBNVS.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
      //JBNVS.S <label>                                 |A|--2346|-|---*-|-----|          |0110_100_011_sss_sss (s is not equal to 63)  [BVC.S <label>]
      //JBNVS.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
    case "jbnvs":
      break;

      //JBNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //JBNZ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //JBNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //JBNZ.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
    case "jbnz":
      break;

      //JBNZE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
      //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
      //JBNZE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
      //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
      //JBNZE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
      //JBNZE.S <label>                                 |A|--2346|-|--*--|-----|          |0110_011_011_sss_sss (s is not equal to 63)  [BNE.S <label>]
    case "jbnze":
      break;

      //JBPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
      //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
      //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
      //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
      //JBPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
      //JBPL.S <label>                                  |A|--2346|-|-*---|-----|          |0110_101_011_sss_sss (s is not equal to 63)  [BPL.S <label>]
      //JBPL.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
    case "jbpl":
      break;

      //JBRA.L <label>                                  |A|012346|-|-----|-----|          |0100_111_011_111_001-{address}       [JMP <label>]
      //JBRA.W <label>                                  |A|012346|-|-----|-----|          |0110_000_000_000_000-{offset}        [BRA.W <label>]
      //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)   [BRA.S <label>]
      //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_001_sss_sss [BRA.S <label>]
      //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_010_sss_sss [BRA.S <label>]
      //JBRA.S <label>                                  |A|01----|-|-----|-----|          |0110_000_011_sss_sss [BRA.S <label>]
      //JBRA.S <label>                                  |A|--2346|-|-----|-----|          |0110_000_011_sss_sss (s is not equal to 63)  [BRA.S <label>]
    case "jbra":
      break;

      //JBSR.L <label>                                  |A|012346|-|-----|-----|          |0100_111_010_111_001-{address}       [JSR <label>]
      //JBSR.W <label>                                  |A|012346|-|-----|-----|          |0110_000_100_000_000-{offset}        [BSR.W <label>]
      //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)   [BSR.S <label>]
      //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_101_sss_sss [BSR.S <label>]
      //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_110_sss_sss [BSR.S <label>]
      //JBSR.S <label>                                  |A|01----|-|-----|-----|          |0110_000_111_sss_sss [BSR.S <label>]
      //JBSR.S <label>                                  |A|--2346|-|-----|-----|          |0110_000_111_sss_sss (s is not equal to 63)  [BSR.S <label>]
    case "jbsr":
      break;

      //JBVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
      //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
      //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
      //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
      //JBVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
      //JBVC.S <label>                                  |A|--2346|-|---*-|-----|          |0110_100_011_sss_sss (s is not equal to 63)  [BVC.S <label>]
      //JBVC.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
    case "jbvc":
      break;

      //JBVS.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
      //JBVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
      //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
      //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
      //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
      //JBVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
      //JBVS.S <label>                                  |A|--2346|-|---*-|-----|          |0110_100_111_sss_sss (s is not equal to 63)  [BVS.S <label>]
    case "jbvs":
      break;

      //JBZE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
      //JBZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
      //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
      //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
      //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
      //JBZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
      //JBZE.S <label>                                  |A|--2346|-|--*--|-----|          |0110_011_111_sss_sss (s is not equal to 63)  [BEQ.S <label>]
    case "jbze":
      break;

      //JMP <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_011_mmm_rrr
    case "jmp":
      break;

      //JSR <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_010_mmm_rrr
    case "jsr":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //LEA.L <ea>,Aq                                   |-|012346|-|-----|-----|  M  WXZP |0100_qqq_111_mmm_rrr
    case "lea":
      break;

      //LINK.L Ar,#<data>                               |-|--2346|-|-----|-----|          |0100_100_000_001_rrr-{data}
      //LINK.W Ar,#<data>                               |-|012346|-|-----|-----|          |0100_111_001_010_rrr-{data}
    case "link":
      break;

      //LPSTOP.W #<data>                                |-|-----6|P|-----|-----|          |1111_100_000_000_000-0000000111000000-{data}
    case "lpstop":
      break;

      //LSL.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_100_001_rrr
      //LSL.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_100_101_rrr
      //LSL.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_101_001_rrr
      //LSL.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_101_101_rrr
      //LSL.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_110_001_rrr
      //LSL.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_110_101_rrr
      //LSL.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_100_001_rrr [LSL.B #1,Dr]
      //LSL.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_101_001_rrr [LSL.W #1,Dr]
      //LSL.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_110_001_rrr [LSL.L #1,Dr]
      //LSL.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_001_111_mmm_rrr
    case "lsl":
      break;

      //LSR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_001_rrr
      //LSR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_101_rrr
      //LSR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_001_rrr
      //LSR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_101_rrr
      //LSR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_001_rrr
      //LSR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_101_rrr
      //LSR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_001_rrr [LSR.B #1,Dr]
      //LSR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_001_rrr [LSR.W #1,Dr]
      //LSR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_001_rrr [LSR.L #1,Dr]
      //LSR.W <ea>                                      |-|012346|-|UUUUU|*0*0*|  M+-WXZ  |1110_001_011_mmm_rrr
    case "lsr":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //MOVE.B <ea>,Dq                                  |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_000_mmm_rrr
      //MOVE.B <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_010_mmm_rrr
      //MOVE.B <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_011_mmm_rrr
      //MOVE.B <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_100_mmm_rrr
      //MOVE.B <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_101_mmm_rrr
      //MOVE.B <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_110_mmm_rrr
      //MOVE.B <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_000_111_mmm_rrr
      //MOVE.B <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_001_111_mmm_rrr
      //MOVE.L <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_000_mmm_rrr
      //MOVE.L <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr [MOVEA.L <ea>,Aq]
      //MOVE.L <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_010_mmm_rrr
      //MOVE.L <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_011_mmm_rrr
      //MOVE.L <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_100_mmm_rrr
      //MOVE.L <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_101_mmm_rrr
      //MOVE.L <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_110_mmm_rrr
      //MOVE.L <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_000_111_mmm_rrr
      //MOVE.L <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_001_111_mmm_rrr
      //MOVE.W <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_000_mmm_rrr
      //MOVE.W <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr [MOVEA.W <ea>,Aq]
      //MOVE.W <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_010_mmm_rrr
      //MOVE.W <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_011_mmm_rrr
      //MOVE.W <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_100_mmm_rrr
      //MOVE.W <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_101_mmm_rrr
      //MOVE.W <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_110_mmm_rrr
      //MOVE.W <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_000_111_mmm_rrr
      //MOVE.W <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_001_111_mmm_rrr
      //MOVE.W SR,<ea>                                  |-|0-----|-|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr (68000 and 68008 read before move)
      //MOVE.W SR,<ea>                                  |-|-12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
      //MOVE.W CCR,<ea>                                 |-|-12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr
      //MOVE.W <ea>,CCR                                 |-|012346|-|UUUUU|*****|D M+-WXZPI|0100_010_011_mmm_rrr
      //MOVE.W <ea>,SR                                  |-|012346|P|UUUUU|*****|D M+-WXZPI|0100_011_011_mmm_rrr
      //MOVE.L Ar,USP                                   |-|012346|P|-----|-----|          |0100_111_001_100_rrr
      //MOVE.L USP,Ar                                   |-|012346|P|-----|-----|          |0100_111_001_101_rrr
    case "move":
      break;

      //MOVE16 (Ar)+,xxx.L                              |-|----46|-|-----|-----|          |1111_011_000_000_rrr-{address}
      //MOVE16 xxx.L,(Ar)+                              |-|----46|-|-----|-----|          |1111_011_000_001_rrr-{address}
      //MOVE16 (Ar),xxx.L                               |-|----46|-|-----|-----|          |1111_011_000_010_rrr-{address}
      //MOVE16 xxx.L,(Ar)                               |-|----46|-|-----|-----|          |1111_011_000_011_rrr-{address}
      //MOVE16 (Ar)+,(An)+                              |-|----46|-|-----|-----|          |1111_011_000_100_rrr-1nnn000000000000
    case "move16":
      break;

      //MOVEA.L <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr
      //MOVEA.W <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr
    case "movea":
      break;

      //MOVEC.L Rc,Rn                                   |-|-12346|P|-----|-----|          |0100_111_001_111_010-rnnncccccccccccc
      //MOVEC.L Rn,Rc                                   |-|-12346|P|-----|-----|          |0100_111_001_111_011-rnnncccccccccccc
    case "movec":
      break;

      //MOVEM.W <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_010_mmm_rrr-llllllllllllllll
      //MOVEM.L <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_011_mmm_rrr-llllllllllllllll
      //MOVEM.W <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_010_mmm_rrr-llllllllllllllll
      //MOVEM.L <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_011_mmm_rrr-llllllllllllllll
    case "movem":
      break;

      //MOVEP.W (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_100_001_rrr-{data}
      //MOVEP.L (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_101_001_rrr-{data}
      //MOVEP.W Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_110_001_rrr-{data}
      //MOVEP.L Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_111_001_rrr-{data}
    case "movep":
      break;

      //MOVEQ.L #<data>,Dq                              |-|012346|-|-UUUU|-**00|          |0111_qqq_0dd_ddd_ddd
    case "moveq":
      break;

      //MOVES.B <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn000000000000
      //MOVES.B Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn100000000000
      //MOVES.W <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn000000000000
      //MOVES.W Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn100000000000
      //MOVES.L <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn000000000000
      //MOVES.L Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn100000000000
    case "moves":
      break;

      //MULS.L <ea>,Dl                                  |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll100000000hhh        (h is not used)
      //MULS.L <ea>,Dh:Dl                               |-|--234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll110000000hhh        (if h=l then result is not defined)
      //MULS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_111_mmm_rrr
    case "muls":
      break;

      //MULU.L <ea>,Dl                                  |-|--2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll000000000hhh        (h is not used)
      //MULU.L <ea>,Dh:Dl                               |-|--234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll010000000hhh        (if h=l then result is not defined)
      //MULU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_011_mmm_rrr
    case "mulu":
      break;

      //MVS.B <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr (ISA_B)
      //MVS.W <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr (ISA_B)
    case "mvs":
      break;

      //MVZ.B <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr (ISA_B)
      //MVZ.W <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr (ISA_B)
    case "mvz":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //NBCD.B <ea>                                     |-|012346|-|UUUUU|*U*U*|D M+-WXZ  |0100_100_000_mmm_rrr
    case "nbcd":
      break;

      //NEG.B <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_000_mmm_rrr
      //NEG.W <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_001_mmm_rrr
      //NEG.L <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_010_mmm_rrr
    case "neg":
      break;

      //NEGX.B <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_000_mmm_rrr
      //NEGX.W <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_001_mmm_rrr
      //NEGX.L <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_010_mmm_rrr
    case "negx":
      break;

      //NOP                                             |-|012346|-|-----|-----|          |0100_111_001_110_001
    case "nop":
      break;

      //NOT.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_000_mmm_rrr
      //NOT.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_001_mmm_rrr
      //NOT.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_010_mmm_rrr
    case "not":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //OR.B #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_000_mmm_rrr-{data}  [ORI.B #<data>,<ea>]
      //OR.W #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_001_mmm_rrr-{data}  [ORI.W #<data>,<ea>]
      //OR.L #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_010_mmm_rrr-{data}  [ORI.L #<data>,<ea>]
      //OR.B <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_000_mmm_rrr
      //OR.W <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_001_mmm_rrr
      //OR.L <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_010_mmm_rrr
      //OR.B Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_100_mmm_rrr
      //OR.W Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_101_mmm_rrr
      //OR.L Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_110_mmm_rrr
    case "or":
      break;

      //ORI.B #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_000_mmm_rrr-{data}
      //ORI.B #<data>,CCR                               |-|012346|-|*****|*****|          |0000_000_000_111_100-{data}
      //ORI.W #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_001_mmm_rrr-{data}
      //ORI.W #<data>,SR                                |-|012346|P|*****|*****|          |0000_000_001_111_100-{data}
      //ORI.L #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_010_mmm_rrr-{data}
    case "ori":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //PACK Dr,Dq,#<data>                              |-|--2346|-|-----|-----|          |1000_qqq_101_000_rrr-{data}
      //PACK -(Ar),-(Aq),#<data>                        |-|--2346|-|-----|-----|          |1000_qqq_101_001_rrr-{data}
    case "pack":
      break;

      //PBAC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_111-{offset}
      //PBAC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_111-{offset}
    case "pbac":
      break;

      //PBAS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_110-{offset}
      //PBAS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_110-{offset}
    case "pbas":
      break;

      //PBBC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_001-{offset}
      //PBBC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_001-{offset}
    case "pbbc":
      break;

      //PBBS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_000-{offset}
      //PBBS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_000-{offset}
    case "pbbs":
      break;

      //PBCC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_111-{offset}
      //PBCC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_111-{offset}
    case "pbcc":
      break;

      //PBCS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_110-{offset}
      //PBCS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_110-{offset}
    case "pbcs":
      break;

      //PBGC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_101-{offset}
      //PBGC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_101-{offset}
    case "pbgc":
      break;

      //PBGS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_100-{offset}
      //PBGS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_100-{offset}
    case "pbgs":
      break;

      //PBIC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_011-{offset}
      //PBIC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_011-{offset}
    case "pbic":
      break;

      //PBIS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_010-{offset}
      //PBIS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_010-{offset}
    case "pbis":
      break;

      //PBLC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_011-{offset}
      //PBLC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_011-{offset}
    case "pblc":
      break;

      //PBLS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_010-{offset}
      //PBLS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_010-{offset}
    case "pbls":
      break;

      //PBSC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_101-{offset}
      //PBSC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_101-{offset}
    case "pbsc":
      break;

      //PBSS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_000_100-{offset}
      //PBSS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_000_100-{offset}
    case "pbss":
      break;

      //PBWC.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_001-{offset}
      //PBWC.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_001-{offset}
    case "pbwc":
      break;

      //PBWS.W <label>                                  |-|--M---|P|-----|-----|          |1111_000_010_001_000-{offset}
      //PBWS.L <label>                                  |-|--M---|P|-----|-----|          |1111_000_011_001_000-{offset}
    case "pbws":
      break;

      //PDBAC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000111-{offset}
    case "pdbac":
      break;

      //PDBAS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000110-{offset}
    case "pdbas":
      break;

      //PDBBC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000001-{offset}
    case "pdbbc":
      break;

      //PDBBS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000000-{offset}
    case "pdbbs":
      break;

      //PDBCC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001111-{offset}
    case "pdbcc":
      break;

      //PDBCS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001110-{offset}
    case "pdbcs":
      break;

      //PDBGC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001101-{offset}
    case "pdbgc":
      break;

      //PDBGS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001100-{offset}
    case "pdbgs":
      break;

      //PDBIC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001011-{offset}
    case "pdbic":
      break;

      //PDBIS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001010-{offset}
    case "pdbis":
      break;

      //PDBLC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000011-{offset}
    case "pdblc":
      break;

      //PDBLS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000010-{offset}
    case "pdbls":
      break;

      //PDBSC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000101-{offset}
    case "pdbsc":
      break;

      //PDBSS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000000100-{offset}
    case "pdbss":
      break;

      //PDBWC.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001001-{offset}
    case "pdbwc":
      break;

      //PDBWS.W Dr,<label>                              |-|--M---|P|-----|-----|          |1111_000_001_001_rrr-0000000000001000-{offset}
    case "pdbws":
      break;

      //PEA.L <ea>                                      |-|012346|-|-----|-----|  M  WXZP |0100_100_001_mmm_rrr
    case "pea":
      break;

      //PFLUSH SFC,#<mask>                              |-|---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00000
      //PFLUSH DFC,#<mask>                              |-|---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00001
      //PFLUSH Dn,#<mask>                               |-|---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm01nnn
      //PFLUSH #<data>,#<mask>                          |-|---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm10ddd
      //PFLUSH SFC,#<mask>                              |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00000
      //PFLUSH DFC,#<mask>                              |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00001
      //PFLUSH Dn,#<mask>                               |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm01nnn
      //PFLUSH #<data>,#<mask>                          |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm1dddd
      //PFLUSH SFC,#<mask>,<ea>                         |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00000
      //PFLUSH DFC,#<mask>,<ea>                         |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00001
      //PFLUSH Dn,#<mask>,<ea>                          |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm01nnn
      //PFLUSH #<data>,#<mask>,<ea>                     |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm10ddd
      //PFLUSH SFC,#<mask>,<ea>                         |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00000
      //PFLUSH DFC,#<mask>,<ea>                         |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00001
      //PFLUSH Dn,#<mask>,<ea>                          |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm01nnn
      //PFLUSH #<data>,#<mask>,<ea>                     |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm1dddd
      //PFLUSH (Ar)                                     |-|----46|P|-----|-----|          |1111_010_100_001_rrr
    case "pflush":
      break;

      //PFLUSHA                                         |-|---3--|P|-----|-----|          |1111_000_000_000_000-0010010000000000
      //PFLUSHA                                         |-|--M---|P|-----|-----|          |1111_000_000_000_000-0010010000000000
      //PFLUSHA                                         |-|----46|P|-----|-----|          |1111_010_100_011_000
    case "pflusha":
      break;

      //PFLUSHAN                                        |-|----46|P|-----|-----|          |1111_010_100_010_000
    case "pflushan":
      break;

      //PFLUSHN (Ar)                                    |-|----46|P|-----|-----|          |1111_010_100_000_rrr
    case "pflushn":
      break;

      //PFLUSHR <ea>                                    |-|--M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-1010000000000000
    case "pflushr":
      break;

      //PFLUSHS SFC,#<mask>                             |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00000
      //PFLUSHS DFC,#<mask>                             |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00001
      //PFLUSHS Dn,#<mask>                              |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm01nnn
      //PFLUSHS #<data>,#<mask>                         |-|--M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm1dddd
      //PFLUSHS SFC,#<mask>,<ea>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00000
      //PFLUSHS DFC,#<mask>,<ea>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00001
      //PFLUSHS Dn,#<mask>,<ea>                         |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm01nnn
      //PFLUSHS #<data>,#<mask>,<ea>                    |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm1dddd
    case "pflushs":
      break;

      //PLOADR SFC,<ea>                                 |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000000
      //PLOADR DFC,<ea>                                 |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000001
      //PLOADR Dn,<ea>                                  |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000001nnn
      //PLOADR #<data>,<ea>                             |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000010ddd
      //PLOADR #<data>,<ea>                             |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000100001dddd
    case "ploadr":
      break;

      //PLOADW SFC,<ea>                                 |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000000
      //PLOADW DFC,<ea>                                 |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000001
      //PLOADW Dn,<ea>                                  |-|--M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000001nnn
      //PLOADW #<data>,<ea>                             |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000010ddd
      //PLOADW #<data>,<ea>                             |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000000001dddd
    case "ploadw":
      break;

      //PLPAR (Ar)                                      |-|-----6|P|-----|-----|          |1111_010_111_001_rrr
    case "plpar":
      break;

      //PLPAW (Ar)                                      |-|-----6|P|-----|-----|          |1111_010_110_001_rrr
    case "plpaw":
      break;

      //PMOVE.L TC,<ea>                                 |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0100001000000000
      //PMOVE.B CAL,<ea>                                |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101001000000000
      //PMOVE.B VAL,<ea>                                |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101011000000000
      //PMOVE.B SCC,<ea>                                |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101101000000000
      //PMOVE.W AC,<ea>                                 |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101111000000000
      //PMOVE.W PSR,<ea>                                |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110001000000000
      //PMOVE.W PCSR,<ea>                               |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110011000000000
      //PMOVE.W BADn,<ea>                               |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110010000nnn00
      //PMOVE.W BACn,<ea>                               |-|--M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110110000nnn00
      //PMOVE.L <ea>,TC                                 |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0100000000000000
      //PMOVE.B <ea>,CAL                                |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101000000000000
      //PMOVE.B <ea>,VAL                                |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101010000000000
      //PMOVE.B <ea>,SCC                                |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101100000000000
      //PMOVE.W <ea>,AC                                 |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101110000000000
      //PMOVE.W <ea>,PSR                                |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110000000000000
      //PMOVE.W <ea>,PCSR                               |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110010000000000
      //PMOVE.W <ea>,BADn                               |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110000000nnn00
      //PMOVE.W <ea>,BACn                               |-|--M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110100000nnn00
      //PMOVE.L <ea>,TTn                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0000000000
      //PMOVE.L TTn,<ea>                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n1000000000
      //PMOVE.L <ea>,TC                                 |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000000000000
      //PMOVE.L TC,<ea>                                 |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100001000000000
      //PMOVE.Q DRP,<ea>                                |-|--M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100011000000000
      //PMOVE.Q <ea>,SRP                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100000000000
      //PMOVE.Q SRP,<ea>                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100101000000000
      //PMOVE.Q SRP,<ea>                                |-|--M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100101000000000
      //PMOVE.Q <ea>,CRP                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110000000000
      //PMOVE.Q CRP,<ea>                                |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100111000000000
      //PMOVE.Q CRP,<ea>                                |-|--M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100111000000000
      //PMOVE.W <ea>,MMUSR                              |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110000000000000
      //PMOVE.W MMUSR,<ea>                              |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110001000000000
      //PMOVE.Q <ea>,DRP                                |-|--M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100010000000000
      //PMOVE.Q <ea>,SRP                                |-|--M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100100000000000
      //PMOVE.Q <ea>,CRP                                |-|--M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100110000000000
    case "pmove":
      break;

      //PMOVEFD.L <ea>,TTn                              |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0100000000
      //PMOVEFD.L <ea>,TC                               |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000100000000
      //PMOVEFD.Q <ea>,SRP                              |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100100000000
      //PMOVEFD.Q <ea>,CRP                              |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110100000000
    case "pmovefd":
      break;

      //PRESTORE <ea>                                   |-|--M---|P|-----|-----|  M+ WXZP |1111_000_101_mmm_rrr
    case "prestore":
      break;

      //PSAC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000111
    case "psac":
      break;

      //PSAS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000110
    case "psas":
      break;

      //PSAVE <ea>                                      |-|--M---|P|-----|-----|  M -WXZ  |1111_000_100_mmm_rrr
    case "psave":
      break;

      //PSBC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000001
    case "psbc":
      break;

      //PSBS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000000
    case "psbs":
      break;

      //PSCC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001111
    case "pscc":
      break;

      //PSCS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001110
    case "pscs":
      break;

      //PSGC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001101
    case "psgc":
      break;

      //PSGS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001100
    case "psgs":
      break;

      //PSIC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001011
    case "psic":
      break;

      //PSIS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001010
    case "psis":
      break;

      //PSLC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000011
    case "pslc":
      break;

      //PSLS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000010
    case "psls":
      break;

      //PSSC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000101
    case "pssc":
      break;

      //PSSS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000000100
    case "psss":
      break;

      //PSWC.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001001
    case "pswc":
      break;

      //PSWS.B <ea>                                     |-|--M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000001000
    case "psws":
      break;

      //PTESTR SFC,<ea>,#<level>                        |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
      //PTESTR SFC,<ea>,#<level>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
      //PTESTR DFC,<ea>,#<level>                        |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
      //PTESTR DFC,<ea>,#<level>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
      //PTESTR Dn,<ea>,#<level>                         |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
      //PTESTR Dn,<ea>,#<level>                         |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
      //PTESTR #<data>,<ea>,#<level>                    |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000010ddd
      //PTESTR #<data>,<ea>,#<level>                    |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll100001dddd
      //PTESTR SFC,<ea>,#<level>,An                     |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
      //PTESTR SFC,<ea>,#<level>,An                     |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
      //PTESTR DFC,<ea>,#<level>,An                     |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
      //PTESTR DFC,<ea>,#<level>,An                     |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
      //PTESTR Dn,<ea>,#<level>,An                      |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
      //PTESTR Dn,<ea>,#<level>,An                      |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
      //PTESTR #<data>,<ea>,#<level>,An                 |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn10ddd
      //PTESTR #<data>,<ea>,#<level>,An                 |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn1dddd
      //PTESTR (Ar)                                     |-|----4-|P|-----|-----|          |1111_010_101_101_rrr
    case "ptestr":
      break;

      //PTESTW SFC,<ea>,#<level>                        |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
      //PTESTW SFC,<ea>,#<level>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
      //PTESTW DFC,<ea>,#<level>                        |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
      //PTESTW DFC,<ea>,#<level>                        |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
      //PTESTW Dn,<ea>,#<level>                         |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
      //PTESTW Dn,<ea>,#<level>                         |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
      //PTESTW #<data>,<ea>,#<level>                    |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000010ddd
      //PTESTW #<data>,<ea>,#<level>                    |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll000001dddd
      //PTESTW SFC,<ea>,#<level>,An                     |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
      //PTESTW SFC,<ea>,#<level>,An                     |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
      //PTESTW DFC,<ea>,#<level>,An                     |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
      //PTESTW DFC,<ea>,#<level>,An                     |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
      //PTESTW Dn,<ea>,#<level>,An                      |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
      //PTESTW Dn,<ea>,#<level>,An                      |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
      //PTESTW #<data>,<ea>,#<level>,An                 |-|---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn10ddd
      //PTESTW #<data>,<ea>,#<level>,An                 |-|--M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn1dddd
      //PTESTW (Ar)                                     |-|----4-|P|-----|-----|          |1111_010_101_001_rrr
    case "ptestw":
      break;

      //PTRAPAC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000111-{data}
      //PTRAPAC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000111-{data}
      //PTRAPAC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000111
    case "ptrapac":
      break;

      //PTRAPAS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000110-{data}
      //PTRAPAS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000110-{data}
      //PTRAPAS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000110
    case "ptrapas":
      break;

      //PTRAPBC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000001-{data}
      //PTRAPBC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000001-{data}
      //PTRAPBC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000001
    case "ptrapbc":
      break;

      //PTRAPBS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000000-{data}
      //PTRAPBS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000000-{data}
      //PTRAPBS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000000
    case "ptrapbs":
      break;

      //PTRAPCC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001111-{data}
      //PTRAPCC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001111-{data}
      //PTRAPCC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001111
    case "ptrapcc":
      break;

      //PTRAPCS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001110-{data}
      //PTRAPCS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001110-{data}
      //PTRAPCS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001110
    case "ptrapcs":
      break;

      //PTRAPGC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001101-{data}
      //PTRAPGC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001101-{data}
      //PTRAPGC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001101
    case "ptrapgc":
      break;

      //PTRAPGS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001100-{data}
      //PTRAPGS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001100-{data}
      //PTRAPGS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001100
    case "ptrapgs":
      break;

      //PTRAPIC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001011-{data}
      //PTRAPIC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001011-{data}
      //PTRAPIC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001011
    case "ptrapic":
      break;

      //PTRAPIS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001010-{data}
      //PTRAPIS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001010-{data}
      //PTRAPIS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001010
    case "ptrapis":
      break;

      //PTRAPLC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000011-{data}
      //PTRAPLC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000011-{data}
      //PTRAPLC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000011
    case "ptraplc":
      break;

      //PTRAPLS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000010-{data}
      //PTRAPLS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000010-{data}
      //PTRAPLS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000010
    case "ptrapls":
      break;

      //PTRAPSC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000101-{data}
      //PTRAPSC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000101-{data}
      //PTRAPSC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000101
    case "ptrapsc":
      break;

      //PTRAPSS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000000100-{data}
      //PTRAPSS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000000100-{data}
      //PTRAPSS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000000100
    case "ptrapss":
      break;

      //PTRAPWC.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001001-{data}
      //PTRAPWC.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001001-{data}
      //PTRAPWC                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001001
    case "ptrapwc":
      break;

      //PTRAPWS.W #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_010-0000000000001000-{data}
      //PTRAPWS.L #<data>                               |-|--M---|P|-----|-----|          |1111_000_001_111_011-0000000000001000-{data}
      //PTRAPWS                                         |-|--M---|P|-----|-----|          |1111_000_001_111_100-0000000000001000
    case "ptrapws":
      break;

      //PVALID.L VAL,<ea>                               |-|--M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010100000000000
      //PVALID.L An,<ea>                                |-|--M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010110000000nnn
    case "pvalid":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //RESET                                           |-|012346|P|-----|-----|          |0100_111_001_110_000
    case "reset":
      break;

      //ROL.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_011_rrr
      //ROL.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_111_rrr
      //ROL.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_011_rrr
      //ROL.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_111_rrr
      //ROL.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_011_rrr
      //ROL.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_111_rrr
      //ROL.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_100_011_rrr [ROL.B #1,Dr]
      //ROL.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_101_011_rrr [ROL.W #1,Dr]
      //ROL.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_110_011_rrr [ROL.L #1,Dr]
      //ROL.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_111_mmm_rrr
    case "rol":
      break;

      //ROR.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_011_rrr
      //ROR.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_111_rrr
      //ROR.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_011_rrr
      //ROR.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_111_rrr
      //ROR.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_011_rrr
      //ROR.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_111_rrr
      //ROR.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_000_011_rrr [ROR.B #1,Dr]
      //ROR.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_001_011_rrr [ROR.W #1,Dr]
      //ROR.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_010_011_rrr [ROR.L #1,Dr]
      //ROR.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_011_mmm_rrr
    case "ror":
      break;

      //ROXL.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_100_010_rrr
      //ROXL.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_100_110_rrr
      //ROXL.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_101_010_rrr
      //ROXL.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_101_110_rrr
      //ROXL.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_110_010_rrr
      //ROXL.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_110_110_rrr
      //ROXL.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_100_010_rrr [ROXL.B #1,Dr]
      //ROXL.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_101_010_rrr [ROXL.W #1,Dr]
      //ROXL.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_110_010_rrr [ROXL.L #1,Dr]
      //ROXL.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_111_mmm_rrr
    case "roxl":
      break;

      //ROXR.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_000_010_rrr
      //ROXR.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_000_110_rrr
      //ROXR.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_001_010_rrr
      //ROXR.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_001_110_rrr
      //ROXR.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_010_010_rrr
      //ROXR.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_010_110_rrr
      //ROXR.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_000_010_rrr [ROXR.B #1,Dr]
      //ROXR.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_001_010_rrr [ROXR.W #1,Dr]
      //ROXR.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_010_010_rrr [ROXR.L #1,Dr]
      //ROXR.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_011_mmm_rrr
    case "roxr":
      break;

      //RTD #<data>                                     |-|-12346|-|-----|-----|          |0100_111_001_110_100-{data}
    case "rtd":
      break;

      //RTE                                             |-|012346|P|UUUUU|*****|          |0100_111_001_110_011
    case "rte":
      break;

      //RTM Rn                                          |-|--2---|-|UUUUU|*****|          |0000_011_011_00n_nnn
    case "rtm":
      break;

      //RTR                                             |-|012346|-|UUUUU|*****|          |0100_111_001_110_111
    case "rtr":
      break;

      //RTS                                             |-|012346|-|-----|-----|          |0100_111_001_110_101
    case "rts":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //SATS.L Dr                                       |-|------|-|-UUUU|-**00|D         |0100_110_010_000_rrr (ISA_B)
    case "sats":
      break;

      //SBCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_000_rrr
      //SBCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_001_rrr
    case "sbcd":
      break;

      //SCC.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr
    case "scc":
      break;

      //SCS.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr
    case "scs":
      break;

      //SEQ.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr
    case "seq":
      break;

      //SF.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr
    case "sf":
      break;

      //SGE.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr
    case "sge":
      break;

      //SGT.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr
    case "sgt":
      break;

      //SHI.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr
    case "shi":
      break;

      //SHS.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
    case "shs":
      break;

      //SLE.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr
    case "sle":
      break;

      //SLO.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
    case "slo":
      break;

      //SLS.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr
    case "sls":
      break;

      //SLT.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr
    case "slt":
      break;

      //SMI.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr
    case "smi":
      break;

      //SNCC.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
    case "sncc":
      break;

      //SNCS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
    case "sncs":
      break;

      //SNE.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr
    case "sne":
      break;

      //SNEQ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
    case "sneq":
      break;

      //SNF.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr [ST.B <ea>]
    case "snf":
      break;

      //SNGE.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr [SLT.B <ea>]
    case "snge":
      break;

      //SNGT.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr [SLE.B <ea>]
    case "sngt":
      break;

      //SNHI.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr [SLS.B <ea>]
    case "snhi":
      break;

      //SNHS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
    case "snhs":
      break;

      //SNLE.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr [SGT.B <ea>]
    case "snle":
      break;

      //SNLO.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
    case "snlo":
      break;

      //SNLS.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr [SHI.B <ea>]
    case "snls":
      break;

      //SNLT.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr [SGE.B <ea>]
    case "snlt":
      break;

      //SNMI.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr [SPL.B <ea>]
    case "snmi":
      break;

      //SNNE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
    case "snne":
      break;

      //SNNZ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
    case "snnz":
      break;

      //SNPL.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr [SMI.B <ea>]
    case "snpl":
      break;

      //SNT.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr [SF.B <ea>]
    case "snt":
      break;

      //SNVC.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr [SVS.B <ea>]
    case "snvc":
      break;

      //SNVS.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr [SVC.B <ea>]
    case "snvs":
      break;

      //SNZ.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
    case "snz":
      break;

      //SNZE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
    case "snze":
      break;

      //SPL.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr
    case "spl":
      break;

      //ST.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr
    case "st":
      break;

      //STOP #<data>                                    |-|012346|P|UUUUU|*****|          |0100_111_001_110_010-{data}
    case "stop":
      break;

      //SUB.B #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_000_mmm_rrr-{data}  [SUBI.B #<data>,<ea>]
      //SUB.W #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_001_mmm_rrr-{data}  [SUBI.W #<data>,<ea>]
      //SUB.L #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_010_mmm_rrr-{data}  [SUBI.L #<data>,<ea>]
      //SUB.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1001_qqq_000_mmm_rrr
      //SUB.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_001_mmm_rrr
      //SUB.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_010_mmm_rrr
      //SUB.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr [SUBA.W <ea>,Aq]
      //SUB.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_100_mmm_rrr
      //SUB.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_101_mmm_rrr
      //SUB.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_110_mmm_rrr
      //SUB.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr [SUBA.L <ea>,Aq]
    case "sub":
      break;

      //SUBA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr
      //SUBA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr
    case "suba":
      break;

      //SUBI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_000_mmm_rrr-{data}
      //SUBI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_001_mmm_rrr-{data}
      //SUBI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_010_mmm_rrr-{data}
    case "subi":
      break;

      //SUBQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_100_mmm_rrr
      //SUBQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_101_mmm_rrr
      //SUBQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_101_001_rrr
      //SUBQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_110_mmm_rrr
      //SUBQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_110_001_rrr
    case "subq":
      break;

      //SUBX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_100_000_rrr
      //SUBX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_100_001_rrr
      //SUBX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_101_000_rrr
      //SUBX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_101_001_rrr
      //SUBX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_110_000_rrr
      //SUBX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_110_001_rrr
    case "subx":
      break;

      //SVC.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr
    case "svc":
      break;

      //SVS.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr
    case "svs":
      break;

      //SWAP.W Dr                                       |-|012346|-|-UUUU|-**00|D         |0100_100_001_000_rrr
    case "swap":
      break;

      //SXCALL <name>                                   |A|012346|-|UUUUU|*****|          |1010_0dd_ddd_ddd_ddd [ALINE #<data>]
    case "sxcall":
      break;

      //SZE.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
    case "sze":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //TAS.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_011_mmm_rrr
    case "tas":
      break;

      //TPCC.W #<data>                                  |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TPCC.L #<data>                                  |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TPCC                                            |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "tpcc":
      break;

      //TPCS.W #<data>                                  |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TPCS.L #<data>                                  |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TPCS                                            |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "tpcs":
      break;

      //TPEQ.W #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TPEQ.L #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TPEQ                                            |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "tpeq":
      break;

      //TPF.W #<data>                                   |A|--2346|-|-----|-----|          |0101_000_111_111_010-{data}  [TRAPF.W #<data>]
      //TPF.L #<data>                                   |A|--2346|-|-----|-----|          |0101_000_111_111_011-{data}  [TRAPF.L #<data>]
      //TPF                                             |A|--2346|-|-----|-----|          |0101_000_111_111_100 [TRAPF]
    case "tpf":
      break;

      //TPGE.W #<data>                                  |A|--2346|-|-*-*-|-----|          |0101_110_011_111_010-{data}  [TRAPGE.W #<data>]
      //TPGE.L #<data>                                  |A|--2346|-|-*-*-|-----|          |0101_110_011_111_011-{data}  [TRAPGE.L #<data>]
      //TPGE                                            |A|--2346|-|-*-*-|-----|          |0101_110_011_111_100 [TRAPGE]
    case "tpge":
      break;

      //TPGT.W #<data>                                  |A|--2346|-|-***-|-----|          |0101_111_011_111_010-{data}  [TRAPGT.W #<data>]
      //TPGT.L #<data>                                  |A|--2346|-|-***-|-----|          |0101_111_011_111_011-{data}  [TRAPGT.L #<data>]
      //TPGT                                            |A|--2346|-|-***-|-----|          |0101_111_011_111_100 [TRAPGT]
    case "tpgt":
      break;

      //TPHI.W #<data>                                  |A|--2346|-|--*-*|-----|          |0101_001_011_111_010-{data}  [TRAPHI.W #<data>]
      //TPHI.L #<data>                                  |A|--2346|-|--*-*|-----|          |0101_001_011_111_011-{data}  [TRAPHI.L #<data>]
      //TPHI                                            |A|--2346|-|--*-*|-----|          |0101_001_011_111_100 [TRAPHI]
    case "tphi":
      break;

      //TPHS.W #<data>                                  |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TPHS.L #<data>                                  |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TPHS                                            |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "tphs":
      break;

      //TPLE.W #<data>                                  |A|--2346|-|-***-|-----|          |0101_111_111_111_010-{data}  [TRAPLE.W #<data>]
      //TPLE.L #<data>                                  |A|--2346|-|-***-|-----|          |0101_111_111_111_011-{data}  [TRAPLE.L #<data>]
      //TPLE                                            |A|--2346|-|-***-|-----|          |0101_111_111_111_100 [TRAPLE]
    case "tple":
      break;

      //TPLO.W #<data>                                  |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TPLO.L #<data>                                  |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TPLO                                            |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "tplo":
      break;

      //TPLS.W #<data>                                  |A|--2346|-|--*-*|-----|          |0101_001_111_111_010-{data}  [TRAPLS.W #<data>]
      //TPLS.L #<data>                                  |A|--2346|-|--*-*|-----|          |0101_001_111_111_011-{data}  [TRAPLS.W #<data>]
      //TPLS                                            |A|--2346|-|--*-*|-----|          |0101_001_111_111_100 [TRAPLS]
    case "tpls":
      break;

      //TPLT.W #<data>                                  |A|--2346|-|-*-*-|-----|          |0101_110_111_111_010-{data}  [TRAPLT.W #<data>]
      //TPLT.L #<data>                                  |A|--2346|-|-*-*-|-----|          |0101_110_111_111_011-{data}  [TRAPLT.L #<data>]
      //TPLT                                            |A|--2346|-|-*-*-|-----|          |0101_110_111_111_100 [TRAPLT]
    case "tplt":
      break;

      //TPMI.W #<data>                                  |A|--2346|-|-*---|-----|          |0101_101_111_111_010-{data}  [TRAPMI.W #<data>]
      //TPMI.L #<data>                                  |A|--2346|-|-*---|-----|          |0101_101_111_111_011-{data}  [TRAPMI.L #<data>]
      //TPMI                                            |A|--2346|-|-*---|-----|          |0101_101_111_111_100 [TRAPMI]
    case "tpmi":
      break;

      //TPNCC.W #<data>                                 |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TPNCC.L #<data>                                 |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TPNCC                                           |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "tpncc":
      break;

      //TPNCS.W #<data>                                 |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TPNCS.L #<data>                                 |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TPNCS                                           |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "tpncs":
      break;

      //TPNE.W #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TPNE.L #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TPNE                                            |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "tpne":
      break;

      //TPNEQ.W #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TPNEQ.L #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TPNEQ                                           |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "tpneq":
      break;

      //TPNF.W #<data>                                  |A|--2346|-|-----|-----|          |0101_000_011_111_010-{data}  [TRAPT.W #<data>]
      //TPNF.L #<data>                                  |A|--2346|-|-----|-----|          |0101_000_011_111_011-{data}  [TRAPT.L #<data>]
      //TPNF                                            |A|--2346|-|-----|-----|          |0101_000_011_111_100 [TRAPT]
    case "tpnf":
      break;

      //TPNGE.W #<data>                                 |A|--2346|-|-*-*-|-----|          |0101_110_111_111_010-{data}  [TRAPLT.W #<data>]
      //TPNGE.L #<data>                                 |A|--2346|-|-*-*-|-----|          |0101_110_111_111_011-{data}  [TRAPLT.L #<data>]
      //TPNGE                                           |A|--2346|-|-*-*-|-----|          |0101_110_111_111_100 [TRAPLT]
    case "tpnge":
      break;

      //TPNGT.W #<data>                                 |A|--2346|-|-***-|-----|          |0101_111_111_111_010-{data}  [TRAPLE.W #<data>]
      //TPNGT.L #<data>                                 |A|--2346|-|-***-|-----|          |0101_111_111_111_011-{data}  [TRAPLE.L #<data>]
      //TPNGT                                           |A|--2346|-|-***-|-----|          |0101_111_111_111_100 [TRAPLE]
    case "tpngt":
      break;

      //TPNHI.W #<data>                                 |A|--2346|-|--*-*|-----|          |0101_001_111_111_010-{data}  [TRAPLS.W #<data>]
      //TPNHI.L #<data>                                 |A|--2346|-|--*-*|-----|          |0101_001_111_111_011-{data}  [TRAPLS.L #<data>]
      //TPNHI                                           |A|--2346|-|--*-*|-----|          |0101_001_111_111_100 [TRAPLS]
    case "tpnhi":
      break;

      //TPNHS.W #<data>                                 |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TPNHS.L #<data>                                 |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TPNHS                                           |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "tpnhs":
      break;

      //TPNLE.W #<data>                                 |A|--2346|-|-***-|-----|          |0101_111_011_111_010-{data}  [TRAPGT.W #<data>]
      //TPNLE.L #<data>                                 |A|--2346|-|-***-|-----|          |0101_111_011_111_011-{data}  [TRAPGT.L #<data>]
      //TPNLE                                           |A|--2346|-|-***-|-----|          |0101_111_011_111_100 [TRAPGT]
    case "tpnle":
      break;

      //TPNLO.W #<data>                                 |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TPNLO.L #<data>                                 |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TPNLO                                           |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "tpnlo":
      break;

      //TPNLS.W #<data>                                 |A|--2346|-|--*-*|-----|          |0101_001_011_111_010-{data}  [TRAPHI.W #<data>]
      //TPNLS.L #<data>                                 |A|--2346|-|--*-*|-----|          |0101_001_011_111_011-{data}  [TRAPHI.L #<data>]
      //TPNLS                                           |A|--2346|-|--*-*|-----|          |0101_001_011_111_100 [TRAPHI]
    case "tpnls":
      break;

      //TPNLT.W #<data>                                 |A|--2346|-|-*-*-|-----|          |0101_110_011_111_010-{data}  [TRAPGE.W #<data>]
      //TPNLT.L #<data>                                 |A|--2346|-|-*-*-|-----|          |0101_110_011_111_011-{data}  [TRAPGE.L #<data>]
      //TPNLT                                           |A|--2346|-|-*-*-|-----|          |0101_110_011_111_100 [TRAPGE]
    case "tpnlt":
      break;

      //TPNMI.W #<data>                                 |A|--2346|-|-*---|-----|          |0101_101_011_111_010-{data}  [TRAPPL.W #<data>]
      //TPNMI.L #<data>                                 |A|--2346|-|-*---|-----|          |0101_101_011_111_011-{data}  [TRAPPL.L #<data>]
      //TPNMI                                           |A|--2346|-|-*---|-----|          |0101_101_011_111_100 [TRAPPL]
    case "tpnmi":
      break;

      //TPNNE.W #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TPNNE.L #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TPNNE                                           |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "tpnne":
      break;

      //TPNNZ.W #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TPNNZ.L #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TPNNZ                                           |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "tpnnz":
      break;

      //TPNPL.W #<data>                                 |A|--2346|-|-*---|-----|          |0101_101_111_111_010-{data}  [TRAPMI.W #<data>]
      //TPNPL.L #<data>                                 |A|--2346|-|-*---|-----|          |0101_101_111_111_011-{data}  [TRAPMI.L #<data>]
      //TPNPL                                           |A|--2346|-|-*---|-----|          |0101_101_111_111_100 [TRAPMI]
    case "tpnpl":
      break;

      //TPNT.W #<data>                                  |A|--2346|-|-----|-----|          |0101_000_111_111_010-{data}  [TRAPF.W #<data>]
      //TPNT.L #<data>                                  |A|--2346|-|-----|-----|          |0101_000_111_111_011-{data}  [TRAPF.L #<data>]
      //TPNT                                            |A|--2346|-|-----|-----|          |0101_000_111_111_100 [TRAPF]
    case "tpnt":
      break;

      //TPNVC.W #<data>                                 |A|--2346|-|---*-|-----|          |0101_100_111_111_010-{data}  [TRAPVS.W #<data>]
      //TPNVC.L #<data>                                 |A|--2346|-|---*-|-----|          |0101_100_111_111_011-{data}  [TRAPVS.L #<data>]
      //TPNVC                                           |A|--2346|-|---*-|-----|          |0101_100_111_111_100 [TRAPVS]
    case "tpnvc":
      break;

      //TPNVS.W #<data>                                 |A|--2346|-|---*-|-----|          |0101_100_011_111_010-{data}  [TRAPVC.W #<data>]
      //TPNVS.L #<data>                                 |A|--2346|-|---*-|-----|          |0101_100_011_111_011-{data}  [TRAPVC.L #<data>]
      //TPNVS                                           |A|--2346|-|---*-|-----|          |0101_100_011_111_100 [TRAPVC]
    case "tpnvs":
      break;

      //TPNZ.W #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TPNZ.L #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TPNZ                                            |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "tpnz":
      break;

      //TPNZE.W #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TPNZE.L #<data>                                 |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TPNZE                                           |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "tpnze":
      break;

      //TPPL.W #<data>                                  |A|--2346|-|-*---|-----|          |0101_101_011_111_010-{data}  [TRAPPL.W #<data>]
      //TPPL.L #<data>                                  |A|--2346|-|-*---|-----|          |0101_101_011_111_011-{data}  [TRAPPL.L #<data>]
      //TPPL                                            |A|--2346|-|-*---|-----|          |0101_101_011_111_100 [TRAPPL]
    case "tppl":
      break;

      //TPT.W #<data>                                   |A|--2346|-|-----|-----|          |0101_000_011_111_010-{data}  [TRAPT.W #<data>]
      //TPT.L #<data>                                   |A|--2346|-|-----|-----|          |0101_000_011_111_011-{data}  [TRAPT.L #<data>]
      //TPT                                             |A|--2346|-|-----|-----|          |0101_000_011_111_100 [TRAPT]
    case "tpt":
      break;

      //TPVC.W #<data>                                  |A|--2346|-|---*-|-----|          |0101_100_011_111_010-{data}  [TRAPVC.W #<data>]
      //TPVC.L #<data>                                  |A|--2346|-|---*-|-----|          |0101_100_011_111_011-{data}  [TRAPVC.L #<data>]
      //TPVC                                            |A|--2346|-|---*-|-----|          |0101_100_011_111_100 [TRAPVC]
    case "tpvc":
      break;

      //TPVS.W #<data>                                  |A|--2346|-|---*-|-----|          |0101_100_111_111_010-{data}  [TRAPVS.W #<data>]
      //TPVS.L #<data>                                  |A|--2346|-|---*-|-----|          |0101_100_111_111_011-{data}  [TRAPVS.L #<data>]
      //TPVS                                            |A|--2346|-|---*-|-----|          |0101_100_111_111_100 [TRAPVS]
    case "tpvs":
      break;

      //TPZE.W #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TPZE.L #<data>                                  |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TPZE                                            |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "tpze":
      break;

      //TRAP #<vector>                                  |-|012346|-|-----|-----|          |0100_111_001_00v_vvv
    case "trap":
      break;

      //TRAPCC.W #<data>                                |-|--2346|-|----*|-----|          |0101_010_011_111_010-{data}
      //TRAPCC.L #<data>                                |-|--2346|-|----*|-----|          |0101_010_011_111_011-{data}
      //TRAPCC                                          |-|--2346|-|----*|-----|          |0101_010_011_111_100
    case "trapcc":
      break;

      //TRAPCS.W #<data>                                |-|--2346|-|----*|-----|          |0101_010_111_111_010-{data}
      //TRAPCS.L #<data>                                |-|--2346|-|----*|-----|          |0101_010_111_111_011-{data}
      //TRAPCS                                          |-|--2346|-|----*|-----|          |0101_010_111_111_100
    case "trapcs":
      break;

      //TRAPEQ.W #<data>                                |-|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}
      //TRAPEQ.L #<data>                                |-|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}
      //TRAPEQ                                          |-|--2346|-|--*--|-----|          |0101_011_111_111_100
    case "trapeq":
      break;

      //TRAPF.W #<data>                                 |-|--2346|-|-----|-----|          |0101_000_111_111_010-{data}
      //TRAPF.L #<data>                                 |-|--2346|-|-----|-----|          |0101_000_111_111_011-{data}
      //TRAPF                                           |-|--2346|-|-----|-----|          |0101_000_111_111_100
    case "trapf":
      break;

      //TRAPGE.W #<data>                                |-|--2346|-|-*-*-|-----|          |0101_110_011_111_010-{data}
      //TRAPGE.L #<data>                                |-|--2346|-|-*-*-|-----|          |0101_110_011_111_011-{data}
      //TRAPGE                                          |-|--2346|-|-*-*-|-----|          |0101_110_011_111_100
    case "trapge":
      break;

      //TRAPGT.W #<data>                                |-|--2346|-|-***-|-----|          |0101_111_011_111_010-{data}
      //TRAPGT.L #<data>                                |-|--2346|-|-***-|-----|          |0101_111_011_111_011-{data}
      //TRAPGT                                          |-|--2346|-|-***-|-----|          |0101_111_011_111_100
    case "trapgt":
      break;

      //TRAPHI.W #<data>                                |-|--2346|-|--*-*|-----|          |0101_001_011_111_010-{data}
      //TRAPHI.L #<data>                                |-|--2346|-|--*-*|-----|          |0101_001_011_111_011-{data}
      //TRAPHI                                          |-|--2346|-|--*-*|-----|          |0101_001_011_111_100
    case "traphi":
      break;

      //TRAPHS.W #<data>                                |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TRAPHS.L #<data>                                |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TRAPHS                                          |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "traphs":
      break;

      //TRAPLE.W #<data>                                |-|--2346|-|-***-|-----|          |0101_111_111_111_010-{data}
      //TRAPLE.L #<data>                                |-|--2346|-|-***-|-----|          |0101_111_111_111_011-{data}
      //TRAPLE                                          |-|--2346|-|-***-|-----|          |0101_111_111_111_100
    case "traple":
      break;

      //TRAPLO.W #<data>                                |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TRAPLO.L #<data>                                |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TRAPLO                                          |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "traplo":
      break;

      //TRAPLS.W #<data>                                |-|--2346|-|--*-*|-----|          |0101_001_111_111_010-{data}
      //TRAPLS.L #<data>                                |-|--2346|-|--*-*|-----|          |0101_001_111_111_011-{data}
      //TRAPLS                                          |-|--2346|-|--*-*|-----|          |0101_001_111_111_100
    case "trapls":
      break;

      //TRAPLT.W #<data>                                |-|--2346|-|-*-*-|-----|          |0101_110_111_111_010-{data}
      //TRAPLT.L #<data>                                |-|--2346|-|-*-*-|-----|          |0101_110_111_111_011-{data}
      //TRAPLT                                          |-|--2346|-|-*-*-|-----|          |0101_110_111_111_100
    case "traplt":
      break;

      //TRAPMI.W #<data>                                |-|--2346|-|-*---|-----|          |0101_101_111_111_010-{data}
      //TRAPMI.L #<data>                                |-|--2346|-|-*---|-----|          |0101_101_111_111_011-{data}
      //TRAPMI                                          |-|--2346|-|-*---|-----|          |0101_101_111_111_100
    case "trapmi":
      break;

      //TRAPNCC.W #<data>                               |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TRAPNCC.L #<data>                               |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TRAPNCC                                         |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "trapncc":
      break;

      //TRAPNCS.W #<data>                               |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TRAPNCS.L #<data>                               |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TRAPNCS                                         |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "trapncs":
      break;

      //TRAPNE.W #<data>                                |-|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}
      //TRAPNE.L #<data>                                |-|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}
      //TRAPNE                                          |-|--2346|-|--*--|-----|          |0101_011_011_111_100
    case "trapne":
      break;

      //TRAPNEQ.W #<data>                               |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TRAPNEQ.L #<data>                               |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TRAPNEQ                                         |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "trapneq":
      break;

      //TRAPNF.W #<data>                                |A|--2346|-|-----|-----|          |0101_000_011_111_010-{data}  [TRAPT.W #<data>]
      //TRAPNF.L #<data>                                |A|--2346|-|-----|-----|          |0101_000_011_111_011-{data}  [TRAPT.L #<data>]
      //TRAPNF                                          |A|--2346|-|-----|-----|          |0101_000_011_111_100 [TRAPT]
    case "trapnf":
      break;

      //TRAPNGE.W #<data>                               |A|--2346|-|-*-*-|-----|          |0101_110_111_111_010-{data}  [TRAPLT.W #<data>]
      //TRAPNGE.L #<data>                               |A|--2346|-|-*-*-|-----|          |0101_110_111_111_011-{data}  [TRAPLT.L #<data>]
      //TRAPNGE                                         |A|--2346|-|-*-*-|-----|          |0101_110_111_111_100 [TRAPLT]
    case "trapnge":
      break;

      //TRAPNGT.W #<data>                               |A|--2346|-|-***-|-----|          |0101_111_111_111_010-{data}  [TRAPLE.W #<data>]
      //TRAPNGT.L #<data>                               |A|--2346|-|-***-|-----|          |0101_111_111_111_011-{data}  [TRAPLE.L #<data>]
      //TRAPNGT                                         |A|--2346|-|-***-|-----|          |0101_111_111_111_100 [TRAPLE]
    case "trapngt":
      break;

      //TRAPNHI.W #<data>                               |A|--2346|-|--*-*|-----|          |0101_001_111_111_010-{data}  [TRAPLS.W #<data>]
      //TRAPNHI.L #<data>                               |A|--2346|-|--*-*|-----|          |0101_001_111_111_011-{data}  [TRAPLS.L #<data>]
      //TRAPNHI                                         |A|--2346|-|--*-*|-----|          |0101_001_111_111_100 [TRAPLS]
    case "trapnhi":
      break;

      //TRAPNHS.W #<data>                               |A|--2346|-|----*|-----|          |0101_010_111_111_010-{data}  [TRAPCS.W #<data>]
      //TRAPNHS.L #<data>                               |A|--2346|-|----*|-----|          |0101_010_111_111_011-{data}  [TRAPCS.L #<data>]
      //TRAPNHS                                         |A|--2346|-|----*|-----|          |0101_010_111_111_100 [TRAPCS]
    case "trapnhs":
      break;

      //TRAPNLE.W #<data>                               |A|--2346|-|-***-|-----|          |0101_111_011_111_010-{data}  [TRAPGT.W #<data>]
      //TRAPNLE.L #<data>                               |A|--2346|-|-***-|-----|          |0101_111_011_111_011-{data}  [TRAPGT.L #<data>]
      //TRAPNLE                                         |A|--2346|-|-***-|-----|          |0101_111_011_111_100 [TRAPGT]
    case "trapnle":
      break;

      //TRAPNLO.W #<data>                               |A|--2346|-|----*|-----|          |0101_010_011_111_010-{data}  [TRAPCC.W #<data>]
      //TRAPNLO.L #<data>                               |A|--2346|-|----*|-----|          |0101_010_011_111_011-{data}  [TRAPCC.L #<data>]
      //TRAPNLO                                         |A|--2346|-|----*|-----|          |0101_010_011_111_100 [TRAPCC]
    case "trapnlo":
      break;

      //TRAPNLS.W #<data>                               |A|--2346|-|--*-*|-----|          |0101_001_011_111_010-{data}  [TRAPHI.W #<data>]
      //TRAPNLS.L #<data>                               |A|--2346|-|--*-*|-----|          |0101_001_011_111_011-{data}  [TRAPHI.L #<data>]
      //TRAPNLS                                         |A|--2346|-|--*-*|-----|          |0101_001_011_111_100 [TRAPHI]
    case "trapnls":
      break;

      //TRAPNLT.W #<data>                               |A|--2346|-|-*-*-|-----|          |0101_110_011_111_010-{data}  [TRAPGE.W #<data>]
      //TRAPNLT.L #<data>                               |A|--2346|-|-*-*-|-----|          |0101_110_011_111_011-{data}  [TRAPGE.L #<data>]
      //TRAPNLT                                         |A|--2346|-|-*-*-|-----|          |0101_110_011_111_100 [TRAPGE]
    case "trapnlt":
      break;

      //TRAPNMI.W #<data>                               |A|--2346|-|-*---|-----|          |0101_101_011_111_010-{data}  [TRAPPL.W #<data>]
      //TRAPNMI.L #<data>                               |A|--2346|-|-*---|-----|          |0101_101_011_111_011-{data}  [TRAPPL.L #<data>]
      //TRAPNMI                                         |A|--2346|-|-*---|-----|          |0101_101_011_111_100 [TRAPPL]
    case "trapnmi":
      break;

      //TRAPNNE.W #<data>                               |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TRAPNNE.L #<data>                               |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TRAPNNE                                         |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "trapnne":
      break;

      //TRAPNNZ.W #<data>                               |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TRAPNNZ.L #<data>                               |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TRAPNNZ                                         |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "trapnnz":
      break;

      //TRAPNPL.W #<data>                               |A|--2346|-|-*---|-----|          |0101_101_111_111_010-{data}  [TRAPMI.W #<data>]
      //TRAPNPL.L #<data>                               |A|--2346|-|-*---|-----|          |0101_101_111_111_011-{data}  [TRAPMI.L #<data>]
      //TRAPNPL                                         |A|--2346|-|-*---|-----|          |0101_101_111_111_100 [TRAPMI]
    case "trapnpl":
      break;

      //TRAPNT.W #<data>                                |A|--2346|-|-----|-----|          |0101_000_111_111_010-{data}  [TRAPF.W #<data>]
      //TRAPNT.L #<data>                                |A|--2346|-|-----|-----|          |0101_000_111_111_011-{data}  [TRAPF.L #<data>]
      //TRAPNT                                          |A|--2346|-|-----|-----|          |0101_000_111_111_100 [TRAPF]
    case "trapnt":
      break;

      //TRAPNVC.W #<data>                               |A|--2346|-|---*-|-----|          |0101_100_111_111_010-{data}  [TRAPVS.W #<data>]
      //TRAPNVC.L #<data>                               |A|--2346|-|---*-|-----|          |0101_100_111_111_011-{data}  [TRAPVS.L #<data>]
      //TRAPNVC                                         |A|--2346|-|---*-|-----|          |0101_100_111_111_100 [TRAPVS]
    case "trapnvc":
      break;

      //TRAPNVS.W #<data>                               |A|--2346|-|---*-|-----|          |0101_100_011_111_010-{data}  [TRAPVC.W #<data>]
      //TRAPNVS.L #<data>                               |A|--2346|-|---*-|-----|          |0101_100_011_111_011-{data}  [TRAPVC.L #<data>]
      //TRAPNVS                                         |A|--2346|-|---*-|-----|          |0101_100_011_111_100 [TRAPVC]
    case "trapnvs":
      break;

      //TRAPNZ.W #<data>                                |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TRAPNZ.L #<data>                                |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TRAPNZ                                          |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "trapnz":
      break;

      //TRAPNZE.W #<data>                               |A|--2346|-|--*--|-----|          |0101_011_011_111_010-{data}  [TRAPNE.W #<data>]
      //TRAPNZE.L #<data>                               |A|--2346|-|--*--|-----|          |0101_011_011_111_011-{data}  [TRAPNE.L #<data>]
      //TRAPNZE                                         |A|--2346|-|--*--|-----|          |0101_011_011_111_100 [TRAPNE]
    case "trapnze":
      break;

      //TRAPPL.W #<data>                                |-|--2346|-|-*---|-----|          |0101_101_011_111_010-{data}
      //TRAPPL.L #<data>                                |-|--2346|-|-*---|-----|          |0101_101_011_111_011-{data}
      //TRAPPL                                          |-|--2346|-|-*---|-----|          |0101_101_011_111_100
    case "trappl":
      break;

      //TRAPT.W #<data>                                 |-|--2346|-|-----|-----|          |0101_000_011_111_010-{data}
      //TRAPT.L #<data>                                 |-|--2346|-|-----|-----|          |0101_000_011_111_011-{data}
      //TRAPT                                           |-|--2346|-|-----|-----|          |0101_000_011_111_100
    case "trapt":
      break;

      //TRAPV                                           |-|012346|-|---*-|-----|          |0100_111_001_110_110
    case "trapv":
      break;

      //TRAPVC.W #<data>                                |-|--2346|-|---*-|-----|          |0101_100_011_111_010-{data}
      //TRAPVC.L #<data>                                |-|--2346|-|---*-|-----|          |0101_100_011_111_011-{data}
      //TRAPVC                                          |-|--2346|-|---*-|-----|          |0101_100_011_111_100
    case "trapvc":
      break;

      //TRAPVS.W #<data>                                |-|--2346|-|---*-|-----|          |0101_100_111_111_010-{data}
      //TRAPVS.L #<data>                                |-|--2346|-|---*-|-----|          |0101_100_111_111_011-{data}
      //TRAPVS                                          |-|--2346|-|---*-|-----|          |0101_100_111_111_100
    case "trapvs":
      break;

      //TRAPZE.W #<data>                                |A|--2346|-|--*--|-----|          |0101_011_111_111_010-{data}  [TRAPEQ.W #<data>]
      //TRAPZE.L #<data>                                |A|--2346|-|--*--|-----|          |0101_011_111_111_011-{data}  [TRAPEQ.L #<data>]
      //TRAPZE                                          |A|--2346|-|--*--|-----|          |0101_011_111_111_100 [TRAPEQ]
    case "trapze":
      break;

      //TST.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_000_mmm_rrr
      //TST.B <ea>                                      |-|--2346|-|-UUUU|-**00|        PI|0100_101_000_mmm_rrr
      //TST.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_001_mmm_rrr
      //TST.W <ea>                                      |-|--2346|-|-UUUU|-**00| A      PI|0100_101_001_mmm_rrr
      //TST.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_010_mmm_rrr
      //TST.L <ea>                                      |-|--2346|-|-UUUU|-**00| A      PI|0100_101_010_mmm_rrr
    case "tst":
      break;

      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
      //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
      //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
      //UNLK Ar                                         |-|012346|-|-----|-----|          |0100_111_001_011_rrr
    case "unlk":
      break;

      //UNPK Dr,Dq,#<data>                              |-|--2346|-|-----|-----|          |1000_qqq_110_000_rrr-{data}
      //UNPK -(Ar),-(Aq),#<data>                        |-|--2346|-|-----|-----|          |1000_qqq_110_001_rrr-{data}
    case "unpk":
      break;



      //------------------------------------------------------------------------
      //エミュレータ拡張命令

      //HFSBOOT                                         |-|012346|-|-----|-----|          |0100_111_000_000_000
    case "hfsboot":
      break;

      //HFSINST                                         |-|012346|-|-----|-----|          |0100_111_000_000_001
    case "hfsinst":
      break;

      //HFSSTR                                          |-|012346|-|-----|-----|          |0100_111_000_000_010
    case "hfsstr":
      break;

      //HFSINT                                          |-|012346|-|-----|-----|          |0100_111_000_000_011
    case "hfsint":
      break;



      //------------------------------------------------------------------------
      //擬似命令

    case "dc":
      break;

    case "dcb":
      break;

    case "ds":
      break;



    default:

    }
  }

*/

}  //class Assembler



