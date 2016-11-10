//========================================================================================
//  ExpressionEvaluator.java
//    en:Expression evaluator
//    ja:式評価
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  内部クラスExpressionElementのインスタンスは共通のレジスタやメモリにアクセスする
//
//  値の型
//    値の型は浮動小数点数と文字列の2種類。数値はすべて浮動小数点数
//    原則として値の型はパーサで確認する。print()などの任意の引数を受け取るものを除いて、エバリュエータは引数の型をチェックしない
//  整数演算
//    以下の演算子は浮動小数点数を符号あり64bit整数に飽和変換してから演算を行う
//      x<<y  x>>y  x>>>y  x&y  x^y  x|y  x<<=y  x>>=y  x>>>=y  x&=y  x^=y  x|=y
//    浮動小数点数から符号あり64bit整数への飽和変換
//      符号あり64bit整数の範囲内の値は小数点以下を切り捨てる
//      符号あり64bit整数の範囲外の値は符号あり64bit整数で表現できる最小の値または最大の値に変換する
//      NaNは0に変換する
//    シフトカウント
//      シフトカウントは符号あり64bit整数の下位6bitを使用する
//    符号なし右シフトの結果
//      符号なし右シフトの結果も符号あり64bit整数とみなす
//      -1>>>1は2**63-1だが、-1>>>0は2**64-1にならず-1のままである
//    アドレス
//      アドレスは符号あり64bit整数の下位32bitを使用する
//    ファンクションコード
//      ファンクションコードは符号あり64bit整数の下位3bitを使用する
//  右辺のx.bとx.wとx.lとx.q
//    x.bはxを符号あり64bit整数に飽和変換してから下位8bitを符号あり8bit整数とみなして符号拡張する。xがアドレスレジスタの場合も同じ
//    x.wはxを符号あり64bit整数に飽和変換してから下位16bitを符号あり16bit整数とみなして符号拡張する。xがアドレスレジスタの場合も同じ
//    x.lはxを符号あり64bit整数に飽和変換してから下位32bitを符号あり32bit整数とみなして符号拡張する
//    x.qはxを符号あり64bit整数に飽和変換する
//  左辺のr0.bとr0.wとr0.l
//    r0.b=yはr0の下位8bitだけを書き換える。r0がアドレスレジスタの場合も同じ
//    r0.w=yはr0の下位16bitだけを書き換える。r0がアドレスレジスタの場合も同じ
//    r0.l=yはr0の下位32bitすなわち全体を書き換える
//  代入演算子
//    代入演算子は左辺を右辺として返す
//      d0.b=yはyを符号あり64bit整数に飽和変換して下位8bitをd0の下位8bitに代入し、代入した値を符号あり8bit整数とみなして符号拡張して返す
//  複合代入演算子
//    複合代入演算子が返す値は2つの演算子に分けた場合と常に一致する
//      d0.b+=yのd0.bは右辺として読まれて左辺として代入されてから再び右辺として読まれる
//      2回目の読み出しは省略できるが符号拡張は省略できない
//
//  ブレークポイントで使える特殊変数
//    count      命令ブレークポイントの通過回数。変更できる
//    threshold  命令ブレークポイントの閾値。変更できる
//    size       データブレークポイントのサイズ。1=バイト,2=ワード,4=ロング。オペレーションサイズと一致しているとは限らない。変更できない
//    data       データブレークポイントで書き込もうとしている値または読み出した値。変更できる
//    usersuper  0=ユーザモード,1=スーパーバイザモード。変更できない
//    readwrite  0=リード,1=ライト。変更できない
//
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class ExpressionEvaluator extends EFPBox {

  //------------------------------------------------------------------------
  //変数
  protected static HashMap<String,ExpressionElement> evxVariableMap;

  //------------------------------------------------------------------------
  //コンストラクタ
  public ExpressionEvaluator () {
    evxVariableMap = new HashMap<String,ExpressionElement> ();
  }



  //========================================================================================
  //$$EPY 要素の優先順位
  //  ElementPriority
  protected static final int EPY_PRIORITY_PRIMITIVE      = 20;  //基本要素
  protected static final int EPY_PRIORITY_FUNCTION       = 19;  //関数呼び出し      右から
  protected static final int EPY_PRIORITY_AT             = 18;  //＠演算子          左から
  protected static final int EPY_PRIORITY_POSTFIX        = 17;  //後置演算子        左から
  protected static final int EPY_PRIORITY_PREFIX         = 16;  //前置演算子        右から
  protected static final int EPY_PRIORITY_EXPONENTIATION = 15;  //累乗演算子        右から
  protected static final int EPY_PRIORITY_MULTIPLICATION = 14;  //乗除算演算子      左から
  protected static final int EPY_PRIORITY_ADDITION       = 13;  //加減算演算子      左から
  protected static final int EPY_PRIORITY_SHIFT          = 12;  //シフト演算子      左から
  protected static final int EPY_PRIORITY_COMPARISON     = 11;  //比較演算子        左から
  protected static final int EPY_PRIORITY_EQUALITY       = 10;  //等価演算子        左から
  protected static final int EPY_PRIORITY_BITWISE_AND    =  9;  //ビットAND演算子   左から
  protected static final int EPY_PRIORITY_BITWISE_XOR    =  8;  //ビットXOR演算子   左から
  protected static final int EPY_PRIORITY_BITWISE_OR     =  7;  //ビットOR演算子    左から
  protected static final int EPY_PRIORITY_LOGICAL_AND    =  6;  //論理AND演算子     左から
  protected static final int EPY_PRIORITY_LOGICAL_OR     =  5;  //論理OR演算子      左から
  protected static final int EPY_PRIORITY_CONDITIONAL    =  4;  //条件演算子        右から
  protected static final int EPY_PRIORITY_ASSIGNMENT     =  3;  //代入演算子        右から
  protected static final int EPY_PRIORITY_COMMA          =  2;  //コンマ演算子      左から
  protected static final int EPY_PRIORITY_COMMAND        =  1;  //コマンド          右から
  protected static final int EPY_PRIORITY_SEPARATOR      =  0;  //セパレータ        左から

  //ElementControlRegister
  //  制御レジスタ
  protected static final int EYG_PC    =  0;
  protected static final int EYG_SR    =  1;
  protected static final int EYG_CCR   =  2;
  protected static final int EYG_SFC   =  3;
  protected static final int EYG_DFC   =  4;
  protected static final int EYG_CACR  =  5;
  protected static final int EYG_TC    =  6;
  protected static final int EYG_ITT0  =  7;
  protected static final int EYG_ITT1  =  8;
  protected static final int EYG_DTT0  =  9;
  protected static final int EYG_DTT1  = 10;
  protected static final int EYG_BUSCR = 11;
  protected static final int EYG_USP   = 12;
  protected static final int EYG_VBR   = 13;
  protected static final int EYG_CAAR  = 14;
  protected static final int EYG_SSP   = 15;
  protected static final int EYG_MSP   = 16;
  protected static final int EYG_ISP   = 17;
  protected static final int EYG_URP   = 18;
  protected static final int EYG_SRP   = 19;
  protected static final int EYG_PCR   = 20;
  protected static final int EYG_FPIAR = 21;
  protected static final int EYG_FPSR  = 22;
  protected static final int EYG_FPCR  = 23;
  protected static final String[] EYG_NAME = {
    "pc",
    "sr",
    "ccr",
    "sfc",
    "dfc",
    "cacr",
    "tc",
    "itt0",
    "itt1",
    "dtt0",
    "dtt1",
    "buscr",
    "usp",
    "vbr",
    "caar",
    "ssp",
    "msp",
    "isp",
    "urp",
    "srp",
    "pcr",
    "fpiar",
    "fpsr",
    "fpcr",
  };



  //========================================================================================
  //$$ETY 要素の型
  //  enum ElementType
  protected enum ElementType {

    //基本要素

    //UNDEF 未定義
    //  関数名や演算子に変換する前のトークンなどの単独では評価できない要素の値の型に使う
    //  パーサが終了した時点で最上位の要素が未定義のときはエラー
    ETY_UNDEF {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("undefined");
      }
    },

    //VOID 値なし
    //  コマンドの返却値
    //  引数としては使用できない
    ETY_VOID {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb;
      }
    },

    //VARIABLE_FLOAT
    //  数値変数
    //  フィールド
    //    paramX  変数の本体
    ETY_VARIABLE_FLOAT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sete (elem.exlParamX.exlFloatValue);  //変数の本体の値
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);  //変数名
      }
    },

    //VARIABLE_STRING
    //  文字列変数
    //  フィールド
    //    paramX  変数の本体
    ETY_VARIABLE_STRING {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlStringValue;  //変数の本体の値
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);  //変数名
      }
    },

    //CONST_FLOAT 浮動小数点数
    //  NaNとInfinityを含む
    //  フィールド
    //    floatValue  数値
    ETY_FLOAT {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlFloatValue.toString ());
      }
    },

    //CONST_STRING 文字列
    //  フィールド
    //    stringValue  文字列
    ETY_STRING {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        String str = elem.exlStringValue;
        sb.append ('"');
        for (int i = 0, l = str.length (); i < l; i++) {
          char c = str.charAt (i);
          if (c == '\b') {
            sb.append ("\\b");
          } else if (c == '\f') {
            sb.append ("\\f");
          } else if (c == '\t') {
            sb.append ("\\t");
          } else if (c == '\n') {
            sb.append ("\\n");
          } else if (c == '\r') {
            sb.append ("\\r");
          } else if (0x00 <= c && c <= 0x1f) {
            String.format ("\\x%02x", c);
          } else if (c == '"') {
            sb.append ("\\\"");
          } else if (c == '\\') {
            sb.append ("\\\\");
          } else {
            sb.append (c);
          }
        }
        return sb.append ('"');
      }
    },

    //MATH_* 数学定数
    //  数学的には定数だがInfinityやNaNと違って丸めの影響を受けるので実行時に値が変化する場合がある
    //  フィールド
    //    floatValue  数値
    ETY_MATH_APERY {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setapery ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Apery");
      }
    },
    ETY_MATH_CATALAN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setcatalan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Catalan");
      }
    },
    ETY_MATH_EULER {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seteuler ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Euler");
      }
    },
    ETY_MATH_NAPIER {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setnapier ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("e");
      }
    },
    ETY_MATH_PI {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setpi ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("pi");
      }
    },

    //REGISTER_* レジスタ
    //  フィールド
    //    subscript  レジスタの番号
    ETY_REGISTER_RN {  //整数レジスタ
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (XEiJ.regRn[elem.exlSubscript]);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        if (elem.exlSubscript <= 7) {  //d0～d7
          return sb.append ('d').append (elem.exlSubscript);
        } else if (elem.exlSubscript <= 14) {  //a0～a6
          return sb.append ('a').append (elem.exlSubscript - 8);
        } else {
          return sb.append ("sp");
        }
      }
    },

    ETY_REGISTER_FPN {  //浮動小数点レジスタ
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlSubscript));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("fp").append (elem.exlSubscript);
      }
    },

    ETY_REGISTER_CRN {  //制御レジスタ
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlReadCRn (elem.exlSubscript));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (EYG_NAME[elem.exlSubscript]);
      }
    },

    //FUNCTION_* 関数
    //  フィールド
    //    valueType    結果の型
    //    floatValue   数値の結果
    //    stringValue  文字列の結果
    //    paramX       1番目の引数
    //    paramY       2番目の引数
    //    paramZ       3番目の引数
    ETY_FUNCTION_ABS {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.abs (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "abs");
      }
    },

    ETY_FUNCTION_ACOS {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acos (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acos");
      }
    },

    ETY_FUNCTION_ACOSH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acosh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acosh");
      }
    },

    ETY_FUNCTION_ACOT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acot (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acot");
      }
    },

    ETY_FUNCTION_ACOTH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acoth (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acoth");
      }
    },

    ETY_FUNCTION_ACSC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acsc (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acsc");
      }
    },

    ETY_FUNCTION_ACSCH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.acsch (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acsch");
      }
    },

    ETY_FUNCTION_AGI {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.agi (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "agi");
      }
    },

    ETY_FUNCTION_AGM {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.agi (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "agm");
      }
    },

    ETY_FUNCTION_ASC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlStringValue.charAt (0));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asc");
      }
    },

    ETY_FUNCTION_ASEC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.asec (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asec");
      }
    },

    ETY_FUNCTION_ASECH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.asech (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asech");
      }
    },

    ETY_FUNCTION_ASIN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.asin (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asin");
      }
    },

    ETY_FUNCTION_ASINH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.asinh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asinh");
      }
    },

    ETY_FUNCTION_ATAN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.atan (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atan");
      }
    },

    ETY_FUNCTION_ATAN2 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.atan2 (elem.exlParamX.exlEval ().exlFloatValue,
                                  elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atan2");
      }
    },

    ETY_FUNCTION_ATANH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.atanh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atanh");
      }
    },

    ETY_FUNCTION_BIN_DOLLAR {
      @Override protected void etyEval (ExpressionElement elem) {
        long x = elem.exlParamX.exlEval ().exlFloatValue.getl ();
        int m = Math.max (0, 63 - Long.numberOfLeadingZeros (x));  //桁数-1=最上位の桁位置
        char[] w = new char[64];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> k) & 1;
          w[m - k] = (char) (48 + t);
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "bin$");
      }
    },

    ETY_FUNCTION_CBRT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.cbrt (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cbrt");
      }
    },

    ETY_FUNCTION_CEIL {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.ceil (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ceil");
      }
    },

    ETY_FUNCTION_CHR_DOLLAR {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = String.valueOf ((char) elem.exlParamX.exlEval ().exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "chr$");
      }
    },

    ETY_FUNCTION_CMP {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.cmp (elem.exlParamY.exlEval ().exlFloatValue));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp");
      }
    },

    ETY_FUNCTION_CMP0 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.cmp0 ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp0");
      }
    },

    ETY_FUNCTION_CMP1 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.cmp1 ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp1");
      }
    },

    ETY_FUNCTION_CMP1ABS {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.cmp1abs ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp1abs");
      }
    },

    ETY_FUNCTION_CMPABS {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.cmpabs (elem.exlParamY.exlEval ().exlFloatValue));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmpabs");
      }
    },

    ETY_FUNCTION_COS {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.cos (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cos");
      }
    },

    ETY_FUNCTION_COSH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.cosh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cosh");
      }
    },

    ETY_FUNCTION_COT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.cot (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cot");
      }
    },

    ETY_FUNCTION_COTH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.coth (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "coth");
      }
    },

    ETY_FUNCTION_CSC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.csc (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "csc");
      }
    },

    ETY_FUNCTION_CSCH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.csch (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "csch");
      }
    },

    ETY_FUNCTION_CUB {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.cub (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cub");
      }
    },

    ETY_FUNCTION_DEC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.dec (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "dec");
      }
    },

    ETY_FUNCTION_DEG {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.deg (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "deg");
      }
    },

    ETY_FUNCTION_DIV2 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.div2 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "div2");
      }
    },

    ETY_FUNCTION_DIV3 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.div3 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "div3");
      }
    },

    ETY_FUNCTION_DIVPI {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.divpi (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "divpi");
      }
    },

    ETY_FUNCTION_DIVRZ {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.divrz (elem.exlParamX.exlEval ().exlFloatValue,
                                  elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "divrz");
      }
    },

    ETY_FUNCTION_EXP {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.exp (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp");
      }
    },

    ETY_FUNCTION_EXP10 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.exp10 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp10");
      }
    },

    ETY_FUNCTION_EXP2 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.exp2 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp2");
      }
    },

    ETY_FUNCTION_EXP2M1 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.exp2m1 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp2m1");
      }
    },

    ETY_FUNCTION_EXPM1 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.expm1 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "expm1");
      }
    },

    ETY_FUNCTION_FLOOR {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.floor (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "floor");
      }
    },

    ETY_FUNCTION_FRAC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.frac (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "frac");
      }
    },

    ETY_FUNCTION_GAMMA {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.gamma (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "gamma");
      }
    },

    ETY_FUNCTION_GETEXP {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.getexp (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "getexp");
      }
    },

    ETY_FUNCTION_GETMAN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.getman (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "getman");
      }
    },

    ETY_FUNCTION_HEX_DOLLAR {
      @Override protected void etyEval (ExpressionElement elem) {
        long x = elem.exlParamX.exlEval ().exlFloatValue.getl ();
        int m = Math.max (0, 63 - Long.numberOfLeadingZeros (x) >> 2);  //桁数-1=最上位の桁位置
        char[] w = new char[16];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> (k << 2)) & 15;
          w[m - k] = (char) ((9 - t >> 4 & 7) + 48 + t);  //大文字
          //w[m - k] = (char) ((9 - t >> 4 & 39) + 48 + t);  //小文字
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "hex$");
      }
    },

    ETY_FUNCTION_IEEEREM {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.ieeerem (elem.exlParamX.exlEval ().exlFloatValue,
                                    elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ieeerem");
      }
    },

    ETY_FUNCTION_INC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.inc (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "inc");
      }
    },

    ETY_FUNCTION_ISEVEN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.iseven () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "iseven");
      }
    },

    ETY_FUNCTION_ISINF {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.isinf () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isinf");
      }
    },

    ETY_FUNCTION_ISINT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.isint () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isint");
      }
    },

    ETY_FUNCTION_ISNAN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.isnan () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isnan");
      }
    },

    ETY_FUNCTION_ISODD {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.isodd () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isodd");
      }
    },

    ETY_FUNCTION_ISONE {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.isone () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isone");
      }
    },

    ETY_FUNCTION_ISZERO {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "iszero");
      }
    },

    ETY_FUNCTION_LOG {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.log (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log");
      }
    },

    ETY_FUNCTION_LOG10 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.log10 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log10");
      }
    },

    ETY_FUNCTION_LOG1P {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.log1p (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log1p");
      }
    },

    ETY_FUNCTION_LOG2 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.log2 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log2");
      }
    },

    ETY_FUNCTION_LOGGAMMA {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.loggamma (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "loggamma");
      }
    },

    ETY_FUNCTION_MAX {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.max (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "max");
      }
    },

    ETY_FUNCTION_MIN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.min (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "min");
      }
    },

    ETY_FUNCTION_MUL2 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.mul2 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mul2");
      }
    },

    ETY_FUNCTION_MUL3 {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.mul3 (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mul3");
      }
    },

    ETY_FUNCTION_MULPI {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.mulpi (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mulpi");
      }
    },

    ETY_FUNCTION_OCT_DOLLAR {
      @Override protected void etyEval (ExpressionElement elem) {
        long x = elem.exlParamX.exlEval ().exlFloatValue.getl ();
        int m = Math.max (0, (63 - Long.numberOfLeadingZeros (x)) / 3);  //桁数-1=最上位の桁位置
        char[] w = new char[22];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> k * 3) & 7;
          w[m - k] = (char) (48 + t);
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "oct$");
      }
    },

    ETY_FUNCTION_POW {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.pow (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "pow");
      }
    },

    ETY_FUNCTION_QUO {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.quo (elem.exlParamX.exlEval ().exlFloatValue,
                                elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "quo");
      }
    },

    ETY_FUNCTION_RAD {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.rad (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rad");
      }
    },

    ETY_FUNCTION_RANDOM {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.random ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "random");
      }
    },

    ETY_FUNCTION_RCP {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.rcp (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rcp");
      }
    },

    ETY_FUNCTION_RINT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.rint (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rint");
      }
    },

    ETY_FUNCTION_RMODE {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlSetRoundingMode (elem.exlParamX.exlEval ().exlFloatValue.geti ());
        elem.exlFloatValue.setnan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rmode");
      }
    },

    ETY_FUNCTION_ROUND {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.round (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "round");
      }
    },

    ETY_FUNCTION_RPREC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlSetRoundingPrec (elem.exlParamX.exlEval ().exlFloatValue.geti ());
        elem.exlFloatValue.setnan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rprec");
      }
    },

    ETY_FUNCTION_SEC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sec (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sec");
      }
    },

    ETY_FUNCTION_SECH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sech (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sech");
      }
    },

    ETY_FUNCTION_SGN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sgn (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sgn");
      }
    },

    ETY_FUNCTION_SIN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sin (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sin");
      }
    },

    ETY_FUNCTION_SINH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sinh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sinh");
      }
    },

    ETY_FUNCTION_SQRT {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sqrt (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sqrt");
      }
    },

    ETY_FUNCTION_SQU {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.squ (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "squ");
      }
    },

    ETY_FUNCTION_STR_DOLLAR {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlEval ().exlFloatValue.toString ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "str$");
      }
    },

    ETY_FUNCTION_TAN {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.tan (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "tan");
      }
    },

    ETY_FUNCTION_TANH {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.tanh (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "tanh");
      }
    },

    ETY_FUNCTION_TRUNC {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.trunc (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "trunc");
      }
    },

    ETY_FUNCTION_ULP {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.ulp (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ulp");
      }
    },

    ETY_FUNCTION_VAL {
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.parse (elem.exlParamX.exlEval ().exlStringValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "val");
      }
    },

    //  メモリ参照
    ETY_OPERATOR_MEMORY {  // [x]
      @Override protected void etyEval (ExpressionElement elem) {
        int a, f;
        if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]
          a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
          f = elem.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
        } else {  //[x]
          a = elem.exlParamX.exlEval ().exlFloatValue.geti ();
          f = -1;
        }
        elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamX.exlAppendTo (sb.append ('[')).append (']');
      }
    },

    //＠演算子
    ETY_OPERATOR_AT {  // x@y
      @Override protected void etyEval (ExpressionElement elem) {
        //xとyを評価してxを返す
        elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue);
        elem.exlParamY.exlEval ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "@");
      }
    },

    //後置演算子
    ETY_OPERATOR_POSTINCREMENT {  // x++
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_REGISTER_RN:  // d0++
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n);
            elem.exlWriteRegLong (n, x + 1);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0++
          {
            int n = elem.exlParamX.exlSubscript;
            EFP x = elem.exlGetFPn (n);
            elem.exlFloatValue.sete (x);
            x.inc ();
          }
          break;
        case ETY_REGISTER_CRN:  // pc++
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadCRn (n);
            elem.exlWriteCRn (n, x + 1);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]++
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]++
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x]++
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x + 1, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.b++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x + 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].b++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].b++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].b++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x + 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.w++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x + 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].w++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].w++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].w++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x + 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.l++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x + 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].l++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].l++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].l++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x + 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].q++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].q++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].q++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f);
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x + 1L, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.s++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n));
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x + 1.0F));
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].s++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].s++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].s++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f));
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x + 1.0F), f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].d++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].d++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].d++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f));
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x + 1.0), f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].x++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].x++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].x++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setx012 (b, 0));
              x.inc ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].y++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].y++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].y++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.sety012 (b, 0));
              x.inc ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].p++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].p++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].p++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setp012 (b, 0));
              x.inc ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, "++");
      }
    },

    ETY_OPERATOR_POSTDECREMENT {  // x--
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_REGISTER_RN:  // d0--
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x - 1);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0--
          {
            int n = elem.exlParamX.exlSubscript;
            EFP x = elem.exlGetFPn (n);
            elem.exlFloatValue.sete (x);
            x.dec ();
          }
          break;
        case ETY_REGISTER_CRN:  // pc--
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadCRn (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x - 1);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]--
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]--
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x]--
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x - 1, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.b--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x - 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].b--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].b--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].b--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x - 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.w--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x - 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].w--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].w--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].w--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x - 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.l--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x - 1);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].l--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].l--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].l--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x - 1, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].q--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].q--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].q--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f);
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x - 1L, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.s--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n));
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x - 1.0F));
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].s--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].s--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].s--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f));
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x - 1.0F), f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].d--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].d--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].d--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f));
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x - 1.0), f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].x--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].x--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].x--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setx012 (b, 0));
              x.dec ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].y--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].y--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].y--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.sety012 (b, 0));
              x.dec ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].p--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].p--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].p--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setp012 (b, 0));
              x.dec ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, "--");
      }
    },

    ETY_OPERATOR_SIZE_BYTE {  // x.b
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].b
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].b
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].b
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f));
          }
          break;
        default:
          elem.exlFloatValue.seti ((byte) elem.exlParamX.exlEval ().exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".b");
      }
    },

    ETY_OPERATOR_SIZE_WORD {  // x.w
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].w
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].w
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].w
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f));
          }
          break;
        default:
          elem.exlFloatValue.seti ((short) elem.exlParamX.exlEval ().exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".w");
      }
    },

    ETY_OPERATOR_SIZE_LONG {  // x.l
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].l
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].l
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].l
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f));
          }
          break;
        default:
          elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".l");
      }
    },

    ETY_OPERATOR_SIZE_QUAD {  // x.q
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].q
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].q
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].q
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f));
          }
          break;
        default:
          elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".q");
      }
    },

    ETY_OPERATOR_SIZE_SINGLE {  // x.s
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].s
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].s
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].s
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f));
          }
          break;
        default:
          elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue).roundf ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".s");
      }
    },

    ETY_OPERATOR_SIZE_DOUBLE {  // x.d
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].d
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].d
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].d
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f));
          }
          break;
        default:
          elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue).roundd ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".d");
      }
    },

    ETY_OPERATOR_SIZE_EXTENDED {  // x.x
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].x
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].x
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].x
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.setx012 (b, 0);
          }
          break;
        default:
          elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue).roundx ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".x");
      }
    },

    ETY_OPERATOR_SIZE_TRIPLE {  // x.y
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].y
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].y
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.sety012 (b, 0);
          }
          break;
        default:
          elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue).roundy ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".y");
      }
    },

    ETY_OPERATOR_SIZE_PACKED {  // x.p
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_OPERATOR_MEMORY:  // [x].p
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].p
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].p
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.setp012 (b, 0);
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".p");
      }
    },

    //前置演算子
    ETY_OPERATOR_PREINCREMENT {  // ++x
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_REGISTER_RN:  // ++d0
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n) + 1;
            elem.exlWriteRegLong (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_REGISTER_FPN:  // ++fp0
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).inc ());
          break;
        case ETY_REGISTER_CRN:  // ++pc
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadCRn (n) + 1;
            elem.exlWriteCRn (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // ++[x]
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y]
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //++[x]
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f) + 1;
            MC68060.mmuPokeByte (a, x, f);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // ++x.b
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // ++d0.b
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n) + 1;
              elem.exlWriteRegByte (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // ++[x].b
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].b
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].b
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f) + 1;
              MC68060.mmuPokeByte (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // ++x.w
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // ++d0.w
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n) + 1;
              elem.exlWriteRegWord (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // ++[x].w
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].w
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].w
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f) + 1;
              MC68060.mmuPokeWord (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // ++x.l
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // ++d0.l
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n) + 1;
              elem.exlWriteRegLong (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // ++[x].l
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].l
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].l
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f) + 1;
              MC68060.mmuPokeLong (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // ++x.q
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // ++[x].q
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].q
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].q
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f) + 1L;
              MC68060.mmuPokeQuad (a, x, f);
              elem.exlFloatValue.setl (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // ++x.s
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // ++d0.s
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n)) + 1.0F;
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
              elem.exlFloatValue.setf (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // ++[x].s
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].s
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].s
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) + 1.0F;
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
              elem.exlFloatValue.setf (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // ++x.d
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // ++[x].d
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].d
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].d
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) + 1.0;
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
              elem.exlFloatValue.setd (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // ++x.x
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // ++[x].x
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].x
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].x
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0).inc ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // ++x.y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // ++[x].y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0).inc ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // ++x.p
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // ++[x].p
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //++[x@y].p
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //++[x].p
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0).inc ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "++");
      }
    },

    ETY_OPERATOR_PREDECREMENT {  // --x
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_REGISTER_RN:  // --d0
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n) - 1;
            elem.exlWriteRegLong (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_REGISTER_FPN:  // --fp0
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).dec ());
          break;
        case ETY_REGISTER_CRN:  // --pc
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadCRn (n) - 1;
            elem.exlWriteCRn (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // --[x]
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y]
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //--[x]
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f) - 1;
            MC68060.mmuPokeByte (a, x, f);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // --x.b
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // --d0.b
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n) - 1;
              elem.exlWriteRegByte (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // --[x].b
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].b
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].b
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f) - 1;
              MC68060.mmuPokeByte (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // --x.w
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // --d0.w
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n) - 1;
              elem.exlWriteRegWord (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // --[x].w
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].w
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].w
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f) - 1;
              MC68060.mmuPokeWord (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // --x.l
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // --d0.l
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n) - 1;
              elem.exlWriteRegLong (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // --[x].l
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].l
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].l
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f) - 1;
              MC68060.mmuPokeLong (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // --x.q
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // --[x].q
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].q
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].q
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f) - 1L;
              MC68060.mmuPokeQuad (a, x, f);
              elem.exlFloatValue.setl (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // --x.s
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // --d0.s
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n)) - 1.0F;
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
              elem.exlFloatValue.setf (x);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // --[x].s
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].s
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].s
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) - 1.0F;
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
              elem.exlFloatValue.setf (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // --x.d
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // --[x].d
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].d
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].d
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) - 1.0;
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
              elem.exlFloatValue.setd (x);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // --x.x
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // --[x].x
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].x
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].x
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0).dec ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // --x.y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // --[x].y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0).dec ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // --x.p
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // --[x].p
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //--[x@y].p
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //--[x].p
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0).dec ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "--");
      }
    },

    ETY_OPERATOR_NOTHING {  // +x
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sete (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_NEGATION {  // -x
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.neg (elem.exlParamX.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "-");
      }
    },

    ETY_OPERATOR_BITWISE_NOT {  // ~x
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (~elem.exlParamX.exlEval ().exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "~");
      }
    },

    ETY_OPERATOR_LOGICAL_NOT {  // !x
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "!");
      }
    },

    //累乗演算子
    ETY_OPERATOR_POWER {  // x**y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.pow (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "**");
      }
    },

    //乗除算演算子
    ETY_OPERATOR_MULTIPLICATION {  // x*y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.mul (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "*");
      }
    },

    ETY_OPERATOR_DIVISION {  // x/y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.div (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "/");
      }
    },

    ETY_OPERATOR_MODULUS {  //x%y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.rem (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "%");
      }
    },

    //加減算演算子
    ETY_OPERATOR_ADDITION_FLOAT_FLOAT {  // x+y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.add (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_FLOAT_STRING {  // x+y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlEval ().exlFloatValue.toString () + elem.exlParamY.exlEval ().exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_STRING_FLOAT {  // x+y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlEval ().exlStringValue + elem.exlParamY.exlEval ().exlFloatValue.toString ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_STRING_STRING {  // x+y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlEval ().exlStringValue + elem.exlParamY.exlEval ().exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_SUBTRACTION {  // x-y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sub (elem.exlParamX.exlEval ().exlFloatValue, elem.exlParamY.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "-");
      }
    },

    //シフト演算子
    ETY_OPERATOR_LEFT_SHIFT {  // x<<y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () << elem.exlParamY.exlEval ().exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<<");
      }
    },

    ETY_OPERATOR_RIGHT_SHIFT {  // x>>y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () >> elem.exlParamY.exlEval ().exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">>");
      }
    },

    ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT {  // x>>>y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () >>> elem.exlParamY.exlEval ().exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">>>");
      }
    },

    //比較演算子
    ETY_OPERATOR_LESS_THAN {  // x<y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.lt (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<");
      }
    },

    ETY_OPERATOR_LESS_OR_EQUAL {  // x<=y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.le (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<=");
      }
    },

    ETY_OPERATOR_GREATER_THAN {  // x>y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.gt (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">");
      }
    },

    ETY_OPERATOR_GREATER_OR_EQUAL {  // x>=y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.ge (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">=");
      }
    },

    //等価演算子
    ETY_OPERATOR_EQUAL {  // x==y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.eq (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "==");
      }
    },

    ETY_OPERATOR_NOT_EQUAL {  // x!=y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval ().exlFloatValue.ne (elem.exlParamY.exlEval ().exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "!=");
      }
    },

    //ビットAND演算子
    ETY_OPERATOR_BITWISE_AND {  // x&y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () &
                                 elem.exlParamY.exlEval ().exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "&");
      }
    },

    //ビットXOR演算子
    ETY_OPERATOR_BITWISE_XOR {  // x^y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () ^
                                 elem.exlParamY.exlEval ().exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "^");
      }
    },

    //ビットOR演算子
    ETY_OPERATOR_BITWISE_OR {  // x|y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval ().exlFloatValue.getl () |
                                 elem.exlParamY.exlEval ().exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "|");
      }
    },

    //論理AND演算子
    ETY_OPERATOR_LOGICAL_AND {  // x&&y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (!elem.exlParamX.exlEval ().exlFloatValue.iszero () &&
                                 !elem.exlParamY.exlEval ().exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "&&");
      }
    },

    //論理OR演算子
    ETY_OPERATOR_LOGICAL_OR {  // x||y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.seti (!elem.exlParamX.exlEval ().exlFloatValue.iszero () ||
                                 !elem.exlParamY.exlEval ().exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "||");
      }
    },

    //条件演算子
    ETY_OPERATOR_CONDITIONAL {  // x?y:z
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlFloatValue.sete (!elem.exlParamX.exlEval ().exlFloatValue.iszero () ?
                                 elem.exlParamY.exlEval ().exlFloatValue :
                                 elem.exlParamZ.exlEval ().exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendConditionalOperatorTo (sb, "?", ":");
      }
    },

    //代入演算子
    ETY_OPERATOR_ASSIGNMENT {  // x=y
      @Override protected void etyEval (ExpressionElement elem) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.sete (elem.exlParamY.exlEval ().exlFloatValue));
          break;
        case ETY_REGISTER_RN:  // d0=y
          {
            int y = (int) elem.exlParamY.exlEval ().exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteRegLong (elem.exlParamX.exlSubscript, y);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0=y
          {
            EFP y = elem.exlParamY.exlEval ().exlFloatValue;
            elem.exlFloatValue.sete (y);
            elem.exlGetFPn (elem.exlParamX.exlSubscript).sete (y);
          }
          break;
        case ETY_REGISTER_CRN:  // pc=y
          {
            int y = (int) elem.exlParamY.exlEval ().exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteCRn (elem.exlParamX.exlSubscript, y);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]=y
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x]=y
              a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamY.exlValueType == ElementType.ETY_FLOAT) {  //浮動小数点数
              int y = (byte) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              elem.exlFloatValue.seti (y);
              MC68060.mmuPokeByte (a, y, f);
            } else {  //文字列
              MC68060.mmuPokeStringZ (a, elem.exlStringValue = elem.exlParamY.exlEval ().exlStringValue, f);
            }
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.b=y
            {
              int y = (byte) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              elem.exlWriteRegByte (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].b=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].b=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].b=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int y = (byte) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              MC68060.mmuPokeByte (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.w=y
            {
              int y = (short) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              elem.exlWriteRegWord (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].w=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].w=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].w=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int y = (short) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              MC68060.mmuPokeWord (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.l=y
            {
              int y = (int) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              elem.exlWriteRegLong (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].l=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].l=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].l=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int y = (int) elem.exlParamY.exlEval ().exlFloatValue.getl ();
              MC68060.mmuPokeLong (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].q=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].q=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].q=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long y = elem.exlParamY.exlEval ().exlFloatValue.getl ();
              MC68060.mmuPokeQuad (a, y, f);
              elem.exlFloatValue.setl (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_REGISTER_RN:  // d0.s=y
            {
              int y = elem.exlParamY.exlEval ().exlFloatValue.getf0 ();
              elem.exlWriteRegLong (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.setf0 (y);
            }
            break;
          case ETY_OPERATOR_MEMORY:  // [x].s=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].s=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].s=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              int y = elem.exlParamY.exlEval ().exlFloatValue.getf0 ();
              MC68060.mmuPokeLong (a, y, f);
              elem.exlFloatValue.setf0 (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].d=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].d=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].d=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              long y = elem.exlParamY.exlEval ().exlFloatValue.getd01 ();
              MC68060.mmuPokeQuad (a, y, f);
              elem.exlFloatValue.setd01 (y);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].x=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].x=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].x=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval ().exlFloatValue.getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].y=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].y=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].y=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval ().exlFloatValue.gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_OPERATOR_MEMORY:  // [x].p=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].p=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
              } else {  //[x].p=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval ().exlFloatValue.getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0);
            }
            break;
          default:
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_SELF_POWER {  // x**=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.pow (y));
          break;
        case ETY_REGISTER_RN:  // d0**=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).pow (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0**=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).pow (y));
          break;
        case ETY_REGISTER_CRN:  // pc**=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).pow (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]**=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]**=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]**=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).pow (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b**=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w**=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l**=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q**=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s**=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d**=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x**=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y**=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p**=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?**=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b**=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w**=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l**=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s**=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).pow (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?**=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?**=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?**=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b**=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w**=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l**=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q**=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).pow (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s**=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).pow (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d**=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).pow (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x**=y
                elem.exlFloatValue.setx012 (b, 0).pow (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y**=y
                elem.exlFloatValue.sety012 (b, 0).pow (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p**=y
                elem.exlFloatValue.setp012 (b, 0).pow (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "**=");
      }
    },

    ETY_OPERATOR_SELF_MULTIPLICATION {  // x*=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.mul (y));
          break;
        case ETY_REGISTER_RN:  // d0*=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).mul (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0*=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).mul (y));
          break;
        case ETY_REGISTER_CRN:  // pc*=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).mul (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]*=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]*=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]*=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).mul (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b*=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w*=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l*=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q*=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s*=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d*=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x*=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y*=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p*=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?*=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b*=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w*=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l*=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s*=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).mul (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?*=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?*=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?*=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b*=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w*=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l*=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q*=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).mul (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s*=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).mul (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d*=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).mul (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x*=y
                elem.exlFloatValue.setx012 (b, 0).mul (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y*=y
                elem.exlFloatValue.sety012 (b, 0).mul (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p*=y
                elem.exlFloatValue.setp012 (b, 0).mul (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "*=");
      }
    },

    ETY_OPERATOR_SELF_DIVISION {  // x/=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.div (y));
          break;
        case ETY_REGISTER_RN:  // d0/=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).div (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0/=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).div (y));
          break;
        case ETY_REGISTER_CRN:  // pc/=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).div (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]/=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]/=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]/=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).div (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b/=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w/=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l/=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q/=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s/=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d/=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x/=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y/=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p/=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?/=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b/=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w/=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l/=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s/=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).div (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?/=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?/=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?/=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b/=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w/=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l/=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q/=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).div (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s/=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).div (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d/=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).div (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x/=y
                elem.exlFloatValue.setx012 (b, 0).div (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y/=y
                elem.exlFloatValue.sety012 (b, 0).div (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p/=y
                elem.exlFloatValue.setp012 (b, 0).div (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "/=");
      }
    },

    ETY_OPERATOR_SELF_MODULUS {  // x%=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.rem (y));
          break;
        case ETY_REGISTER_RN:  // d0%=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).rem (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0%=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).rem (y));
          break;
        case ETY_REGISTER_CRN:  // pc%=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).rem (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]%=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]%=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]%=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).rem (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b%=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w%=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l%=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q%=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s%=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d%=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x%=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y%=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p%=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?%=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b%=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w%=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l%=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s%=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).rem (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?%=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?%=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?%=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b%=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w%=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l%=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q%=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).rem (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s%=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).rem (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d%=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).rem (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x%=y
                elem.exlFloatValue.setx012 (b, 0).rem (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y%=y
                elem.exlFloatValue.sety012 (b, 0).rem (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p%=y
                elem.exlFloatValue.setp012 (b, 0).rem (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "%=");
      }
    },

    ETY_OPERATOR_SELF_ADDITION {  // x+=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.add (y));
          break;
        case ETY_REGISTER_RN:  // d0+=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).add (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0+=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).add (y));
          break;
        case ETY_REGISTER_CRN:  // pc+=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).add (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]+=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]+=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]+=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).add (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b+=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w+=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l+=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q+=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s+=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d+=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x+=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y+=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p+=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?+=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b+=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w+=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l+=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s+=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).add (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?+=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?+=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?+=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b+=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w+=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l+=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q+=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).add (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s+=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).add (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d+=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).add (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x+=y
                elem.exlFloatValue.setx012 (b, 0).add (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y+=y
                elem.exlFloatValue.sety012 (b, 0).add (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p+=y
                elem.exlFloatValue.setp012 (b, 0).add (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    ETY_OPERATOR_SELF_SUBTRACTION {  // x-=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval ().exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.sub (y));
          break;
        case ETY_REGISTER_RN:  // d0-=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).sub (y).geti ());
          break;
        case ETY_REGISTER_FPN:  // fp0-=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).sub (y));
          break;
        case ETY_REGISTER_CRN:  // pc-=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteCRn (n, elem.exlFloatValue.seti (elem.exlReadCRn (n)).sub (y).geti ());
          break;
        case ETY_OPERATOR_MEMORY:  // [x]-=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]-=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]-=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).sub (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b-=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w-=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l-=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q-=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s-=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d-=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x-=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y-=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p-=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?-=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b-=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w-=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l-=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s-=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).sub (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?-=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?-=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x]./-=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b-=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w-=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l-=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q-=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).sub (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s-=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).sub (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d-=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).sub (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x-=y
                elem.exlFloatValue.setx012 (b, 0).sub (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y-=y
                elem.exlFloatValue.sety012 (b, 0).sub (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p-=y
                elem.exlFloatValue.setp012 (b, 0).sub (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "-=");
      }
    },

    ETY_OPERATOR_SELF_LEFT_SHIFT {  // x<<=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        int y = elem.exlParamY.exlEval ().exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () << y));
          break;
        case ETY_REGISTER_RN:  // d0<<=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0<<=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () << y));
          break;
        case ETY_REGISTER_CRN:  // pc<<=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]<<=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]<<=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]<<=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) << y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b<<=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w<<=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l<<=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q<<=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s<<=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d<<=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x<<=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y<<=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p<<=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?<<=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b<<=y
              int x = (byte) ((long) elem.exlReadRegByte (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w<<=y
              int x = (short) ((long) elem.exlReadRegWord (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l<<=y
              int x = (int) ((long) elem.exlReadRegLong (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s<<=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) << y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?<<=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?<<=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?<<=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b<<=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w<<=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l<<=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q<<=y
              long x = MC68060.mmuPeekQuad (a, f) << y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s<<=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) << y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d<<=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) << y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () << y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () << y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () << y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "<<=");
      }
    },

    ETY_OPERATOR_SELF_RIGHT_SHIFT {  // x>>=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        int y = elem.exlParamY.exlEval ().exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () >> y));
          break;
        case ETY_REGISTER_RN:  // d0>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0>>=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () >> y));
          break;
        case ETY_REGISTER_CRN:  // pc>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]>>=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]>>=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]>>=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >> y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b>>=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w>>=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l>>=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q>>=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s>>=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d>>=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x>>=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y>>=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p>>=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?>>=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b>>=y
              int x = (byte) ((long) elem.exlReadRegByte (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w>>=y
              int x = (short) ((long) elem.exlReadRegWord (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l>>=y
              int x = (int) ((long) elem.exlReadRegLong (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s>>=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) >> y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?>>=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b>>=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w>>=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l>>=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q>>=y
              long x = MC68060.mmuPeekQuad (a, f) >> y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s>>=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) >> y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d>>=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) >> y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () >> y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () >> y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () >> y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, ">>=");
      }
    },

    ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT {  // x>>>=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        int y = elem.exlParamY.exlEval ().exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () >>> y));
          break;
        case ETY_REGISTER_RN:  // d0>>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0>>>=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () >>> y));
          break;
        case ETY_REGISTER_CRN:  // pc>>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]>>>=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]>>>=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]>>>=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >>> y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b>>>=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w>>>=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l>>>=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q>>>=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s>>>=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d>>>=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x>>>=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y>>>=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p>>>=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?>>>=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b>>>=y
              int x = (byte) ((long) elem.exlReadRegByte (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w>>>=y
              int x = (short) ((long) elem.exlReadRegWord (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l>>>=y
              int x = (int) ((long) elem.exlReadRegLong (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s>>>=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) >>> y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?>>>=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?>>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?>>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b>>>=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w>>>=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l>>>=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q>>>=y
              long x = MC68060.mmuPeekQuad (a, f) >>> y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s>>>=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) >>> y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d>>>=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) >>> y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () >>> y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () >>> y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () >>> y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, ">>>=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_AND {  // x&=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        long y = elem.exlParamY.exlEval ().exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () & y));
          break;
        case ETY_REGISTER_RN:  // d0&=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0&=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () & y));
          break;
        case ETY_REGISTER_CRN:  // pc&=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]&=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]&=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]&=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) & y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b&=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w&=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l&=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q&=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s&=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d&=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x&=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y&=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p&=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?&=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b&=y
              int x = (byte) ((long) elem.exlReadRegByte (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w&=y
              int x = (short) ((long) elem.exlReadRegWord (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l&=y
              int x = (int) ((long) elem.exlReadRegLong (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s&=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) & y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?&=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?&=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?&=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b&=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w&=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l&=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q&=y
              long x = MC68060.mmuPeekQuad (a, f) & y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s&=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) & y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d&=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) & y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () & y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () & y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () & y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "&=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_XOR {  // x^=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        long y = elem.exlParamY.exlEval ().exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () ^ y));
          break;
        case ETY_REGISTER_RN:  // d0^=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0^=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () ^ y));
          break;
        case ETY_REGISTER_CRN:  // pc^=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]^=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]^=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]^=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) ^ y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b^=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w^=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l^=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q^=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s^=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d^=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x^=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y^=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p^=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?^=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b^=y
              int x = (byte) ((long) elem.exlReadRegByte (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w^=y
              int x = (short) ((long) elem.exlReadRegWord (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l^=y
              int x = (int) ((long) elem.exlReadRegLong (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s^=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) ^ y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?^=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?^=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?^=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b^=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w^=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l^=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q^=y
              long x = MC68060.mmuPeekQuad (a, f) ^ y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s^=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) ^ y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d^=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) ^ y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () ^ y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () ^ y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () ^ y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "^=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_OR {  // x|=y
      @Override protected void etyEval (ExpressionElement elem) {
        int n, a, f;
        long y = elem.exlParamY.exlEval ().exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () | y));
          break;
        case ETY_REGISTER_RN:  // d0|=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_REGISTER_FPN:  // fp0|=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () | y));
          break;
        case ETY_REGISTER_CRN:  // pc|=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadCRn (n) | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCRn (n, x);
          }
          break;
        case ETY_OPERATOR_MEMORY:  // [x]|=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y]|=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
          } else {  //[x]|=y
            a = elem.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) | y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b|=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w|=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l|=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q|=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s|=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d|=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x|=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.y|=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p|=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_REGISTER_RN) {  // d0.?|=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b|=y
              int x = (byte) ((long) elem.exlReadRegByte (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w|=y
              int x = (short) ((long) elem.exlReadRegWord (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l|=y
              int x = (int) ((long) elem.exlReadRegLong (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s|=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) | y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY) {  // [x].?|=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  //[x@y].?|=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval ().exlFloatValue.geti ();
            } else {  //[x].?|=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval ().exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b|=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w|=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l|=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q|=y
              long x = MC68060.mmuPeekQuad (a, f) | y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s|=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) | y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d|=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) | y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () | y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].y|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () | y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () | y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "|=");
      }
    },

    //  文字列代入演算子
    ETY_OPERATOR_ASSIGN_STRING_TO_VARIABLE {  // v=y 変数への文字列単純代入
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlParamX.exlStringValue = elem.exlParamY.exlEval ().exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_CONCAT_STRING_TO_VARIABLE {  // v+=y 変数への文字列連結複合代入
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlStringValue = elem.exlParamX.exlParamX.exlStringValue += elem.exlParamY.exlEval ().exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    ETY_OPERATOR_ASSIGN_STRING_TO_MEMORY {  // [a]=y メモリへの文字列単純代入
      @Override protected void etyEval (ExpressionElement elem) {
        ExpressionElement valueA = elem.exlParamX.exlParamX.exlEval ();
        ExpressionElement valueY = elem.exlParamY.exlEval ();
        int a = valueA.exlFloatValue.geti ();
        int f = valueA.exlType == ElementType.ETY_OPERATOR_AT ? valueA.exlParamY.exlFloatValue.geti () : -1;
        elem.exlStringValue = MC68060.mmuPokeStringZ (a, valueY.exlStringValue, f);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_CONCAT_STRING_TO_MEMORY {  // [a]+=y メモリへの文字列連結複合代入
      @Override protected void etyEval (ExpressionElement elem) {
        ExpressionElement valueA = elem.exlParamX.exlParamX.exlEval ();
        ExpressionElement valueY = elem.exlParamY.exlEval ();
        int a = valueA.exlFloatValue.geti ();
        int f = valueA.exlType == ElementType.ETY_OPERATOR_AT ? valueA.exlParamY.exlFloatValue.geti () : -1;
        elem.exlStringValue = MC68060.mmuPokeStringZ (a, MC68060.mmuPeekStringZ (a, f) + valueY.exlStringValue, f);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    //コンマ演算子
    ETY_OPERATOR_COMMA {  // x,y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlParamX.exlEval ();
        elem.exlFloatValue.sete (elem.exlParamY.exlEval ().exlFloatValue);
        elem.exlStringValue = elem.exlParamY.exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ",");
      }
    },

    //コマンド
    ETY_COMMAND_DUMP {
      @Override protected void etyEval (ExpressionElement elem) {
        int size = elem.exlSubscript;
        //1行のサイズを決める
        int lineSize = 16;
        EFPBox.EFP tmpF = null;
        switch (size) {
          //case 'b':
          //case 'w':
          //case 'l':
          //case 'q':
        case 's':
          lineSize = 4;
          tmpF = XEiJ.fpuBox.new EFP ();
          break;
        case 'd':
          lineSize = 8;
          tmpF = XEiJ.fpuBox.new EFP ();
          break;
        case 'x':
        case 'y':
        case 'p':
          lineSize = 12;
          tmpF = XEiJ.fpuBox.new EFP ();
          break;
        }
        //開始アドレスと終了アドレスとファンクションコードを設定する
        int pageAddress = XEiJ.dgtDumpAddress;
        int pageSize = lineSize * 16;
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList ();
          if (0 < list.length) {
            ExpressionElement param = list[0];  //開始アドレス
            if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //x@y
              pageAddress = param.exlParamX.exlFloatValue.geti ();
              XEiJ.dgtDumpFunctionCode = param.exlParamY.exlFloatValue.geti ();
            } else if (param.exlValueType == ElementType.ETY_FLOAT) {  //x
              pageAddress = param.exlFloatValue.geti ();
              XEiJ.dgtDumpFunctionCode = XEiJ.regSRS == 0 ? 1 : 5;
            }
            if (1 < list.length) {
              param = list[1];  //終了アドレス
              if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //x@y
                pageSize = param.exlParamX.exlFloatValue.geti () - pageAddress;
              } else if (param.exlValueType == ElementType.ETY_FLOAT) {  //x
                pageSize = param.exlFloatValue.geti () - pageAddress;
              }
            }
          }
        }
        if ((pageSize & -65536) != 0) {  //符号なしのサイズで64KB以上
          pageSize = lineSize * 16;
        }
        pageSize = Math.max (1, (pageSize + lineSize - 1) / lineSize) * lineSize;  //全体のサイズを1行のサイズの倍数に切り上げる
        XEiJ.dgtDumpAddress = pageAddress + pageSize;  //次回の開始アドレス
        int supervisor = XEiJ.dgtDumpFunctionCode & 4;
        //行ループ
        StringBuilder sb = new StringBuilder ();
        for (int lineOffset = 0; lineOffset < pageSize; lineOffset += lineSize) {  //0x00000000と0x80000000の両方を跨げるようにする
          int lineAddress = pageAddress + lineOffset;
          //アドレス
          XEiJ.fmtHex8 (sb, lineAddress);
          //データ
          switch (size) {
          case 'b':
            for (int o = 0; o < lineSize; o++) {
              XEiJ.fmtHex2 (sb.append ((o & 3) == 0 ? "  " : " "), MC68060.mmuPeekByteZeroData (lineAddress + o, supervisor));
            }
            break;
          case 'w':
            for (int o = 0; o < lineSize; o += 2) {
              XEiJ.fmtHex4 (sb.append ((o & 7) == 0 ? "  " : " "), MC68060.mmuPeekWordZeroData (lineAddress + o, supervisor));
            }
            break;
          case 'l':
            for (int o = 0; o < lineSize; o += 4) {
              XEiJ.fmtHex8 (sb.append (o == 0 ? "  " : " "), MC68060.mmuPeekLongData (lineAddress + o, supervisor));
            }
            break;
          case 'q':
            for (int o = 0; o < lineSize; o += 8) {
              XEiJ.fmtHex16 (sb.append (o == 0 ? "  " : " "), MC68060.mmuPeekQuadData (lineAddress + o, supervisor));
            }
            break;
          case 's':
            {
              String s = tmpF.setf0 (MC68060.mmuPeekLongData (lineAddress, supervisor)).toString ();
              sb.append ("  ").append (s).append (XEiJ.DBG_SPACES, 0, Math.max (0, 35 - s.length ()));
            }
            break;
          case 'd':
            {
              String s = tmpF.setd01 (MC68060.mmuPeekQuadData (lineAddress, supervisor)).toString ();
              sb.append ("  ").append (s).append (XEiJ.DBG_SPACES, 0, Math.max (0, 35 - s.length ()));
            }
            break;
          case 'x':
            {
              String s = tmpF.setx012 (MC68060.mmuPeekLongData (lineAddress, supervisor),
                                       MC68060.mmuPeekQuadData (lineAddress + 4, supervisor)).toString ();
              sb.append ("  ").append (s).append (XEiJ.DBG_SPACES, 0, Math.max (0, 35 - s.length ()));
            }
            break;
          case 'y':
            {
              String s = tmpF.sety012 (MC68060.mmuPeekLongData (lineAddress, supervisor),
                                       MC68060.mmuPeekQuadData (lineAddress + 4, supervisor)).toString ();
              sb.append ("  ").append (s).append (XEiJ.DBG_SPACES, 0, Math.max (0, 35 - s.length ()));
            }
            break;
          case 'p':
            {
              String s = tmpF.setp012 (MC68060.mmuPeekLongData (lineAddress, supervisor),
                                       MC68060.mmuPeekQuadData (lineAddress + 4, supervisor)).toString ();
              sb.append ("  ").append (s).append (XEiJ.DBG_SPACES, 0, Math.max (0, 35 - s.length ()));
            }
            break;
          }
          //キャラクタ
          sb.append ("  ");
          for (int o = 0; o < lineSize; o++) {
            int a = lineAddress + o;
            int h = MC68060.mmuPeekByteZeroData (a, supervisor);
            int c;
            if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
              int l = MC68060.mmuPeekByteZeroData (a + 1, supervisor);  //これは範囲外になる場合がある
              if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
                c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                if (c == 0) {  //対応する文字がない
                  c = '※';
                }
                o++;
              } else {  //SJISの2バイトコードの2バイト目ではない
                c = '.';  //SJISの2バイトコードの1バイト目ではなかった
              }
            } else {  //SJISの2バイトコードの1バイト目ではない
              c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
              if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                c = '.';
              }
            }
            sb.append ((char) c);
          }  //for o
          sb.append ('\n');
        }  //for lineOffset
        XEiJ.dgtPrint (sb.toString ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("d").append ((char) elem.exlSubscript);
      }
    },

    ETY_COMMAND_FILL {
      @Override protected void etyEval (ExpressionElement elem) {
        int size = elem.exlSubscript;
        //引数を評価する
        //  引数が3個以上あることはパーサで確認済み
        ExpressionElement[] list = elem.exlParamX.exlEvalCommaList ();
        //開始アドレス
        ExpressionElement param = list[0];
        if (param.exlValueType != ElementType.ETY_FLOAT) {
          XEiJ.dgtPrintln ("引数の型が違います\n");
          return;
        }
        int startAddress;
        int functionCode;
        if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //x@y
          startAddress = param.exlParamX.exlFloatValue.geti ();
          functionCode = param.exlParamY.exlFloatValue.geti () & 7;
        } else {  //x
          startAddress = param.exlFloatValue.geti ();
          functionCode = XEiJ.regSRS == 0 ? 1 : 5;
        }
        //終了アドレス
        param = list[1];
        if (param.exlValueType != ElementType.ETY_FLOAT) {
          XEiJ.dgtPrintln ("引数の型が違います\n");
          return;
        }
        int endAddress;
        if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //x@y
          endAddress = param.exlParamX.exlFloatValue.geti ();
        } else {  //x
          endAddress = param.exlFloatValue.geti ();
        }
        //内容
        int dataSize;
        switch (size) {
        case 'b':
          dataSize = 1;
          break;
        case 'w':
          dataSize = 2;
          break;
        case 'l':
        case 's':
          dataSize = 4;
          break;
        case 'q':
        case 'd':
          dataSize = 8;
          break;
        default:
          dataSize = 12;
        }
        int dataCount = list.length - 2;
        EFPBox.EFP[] dataArray = new EFPBox.EFP[dataCount];
        for (int i = 0; i < dataCount; i++) {
          param = list[2 + i];
          if (param.exlValueType != ElementType.ETY_FLOAT) {
            XEiJ.dgtPrintln ("引数の型が違います\n");
            return;
          }
          dataArray[i] = param.exlFloatValue;
        }
        int repeatCount = (endAddress + 1 - startAddress) / (dataSize * dataCount);
        //フィル
        int a = startAddress;
        switch (size) {
        case 'b':
          {
            int[] x = new int[dataCount];
            for (int i = 0; i < dataCount; i++) {
              x[i] = dataArray[i].getb ();  //符号あり8bit整数に飽和変換
            }
            for (int n = 0; n < repeatCount; n++) {
              for (int i = 0; i < dataCount; i++) {
                MC68060.mmuPokeByte (a++, x[i], functionCode);
              }
            }
          }
          break;
        case 'w':
          {
            int[] x = new int[dataCount];
            for (int i = 0; i < dataCount; i++) {
              x[i] = dataArray[i].gets ();  //符号あり16bit整数に飽和変換
            }
            for (int n = 0; n < repeatCount; n++) {
              for (int i = 0; i < dataCount; i++) {
                MC68060.mmuPokeWord (a, x[i], functionCode);
                a += 2;
              }
            }
          }
          break;
        case 'l':
        case 's':
          {
            int[] x = new int[dataCount];
            for (int i = 0; i < dataCount; i++) {
              if (size == 'l') {
                x[i] = dataArray[i].geti ();  //符号あり32bit整数に飽和変換
              } else {
                x[i] = dataArray[i].getf0 ();  //32bit単精度浮動小数点数に変換
              }
            }
            for (int n = 0; n < repeatCount; n++) {
              for (int i = 0; i < dataCount; i++) {
                MC68060.mmuPokeLong (a, x[i], functionCode);
                a += 4;
              }
            }
          }
          break;
        case 'q':
        case 'd':
          {
            long[] x = new long[dataCount];
            for (int i = 0; i < dataCount; i++) {
              if (size == 'q') {
                x[i] = dataArray[i].getl ();  //符号あり64bit整数に飽和変換
              } else {
                x[i] = dataArray[i].getd01 ();  //倍精度浮動小数点数に変換
              }
            }
            for (int n = 0; n < repeatCount; n++) {
              for (int i = 0; i < dataCount; i++) {
                MC68060.mmuPokeQuad (a, x[i], functionCode);
                a += 8;
              }
            }
          }
          break;
        case 'x':
        case 'y':
        case 'p':
          {
            int[] xi = new int[dataCount];
            long[] xl = new long[dataCount];
            byte[] b = new byte[12];
            for (int i = 0; i < dataCount; i++) {
              if (size == 'x') {
                dataArray[i].getx012 (b, 0);  //拡張精度浮動小数点数に変換
              } else if (size == 'y') {
                dataArray[i].gety012 (b, 0);  //三倍精度浮動小数点数に変換
              } else {
                dataArray[i].getp012 (b, 0);  //パックトデシマルに変換
              }
              xi[i] = b[0] << 24 | (b[1] & 255) << 16 | (char) (b[2] << 8 | b[3] & 255);
              xl[i] = ((long) (b[4] << 24 | (b[5] & 255) << 16 | (char) (b[ 6] << 8 | b[ 7] & 255)) << 32 |
                       (long) (b[8] << 24 | (b[9] & 255) << 16 | (char) (b[10] << 8 | b[11] & 255)) & 0xffffffffL);
            }
            for (int n = 0; n < repeatCount; n++) {
              for (int i = 0; i < dataCount; i++) {
                MC68060.mmuPokeLong (a    , xi[i], functionCode);
                MC68060.mmuPokeQuad (a + 4, xl[i], functionCode);
                a += 12;
              }
            }
          }
          break;
        }  //switch size
        //範囲がデータのサイズで割り切れなかったときは残りを0で充填する
        while (a != endAddress + 1) {
          MC68060.mmuPokeByte (a++, 0, functionCode);
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("f").append ((char) elem.exlSubscript);
      }
    },

    ETY_COMMAND_HELP {
      @Override protected void etyEval (ExpressionElement elem) {
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  コマンド\n" +
                       "	d<サイズ> <開始アドレス>,<終了アドレス¹>	ダンプ\n" +
                       "	f<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>,…	フィル\n" +
                       "	g			実行\n" +
                       "	h			ヘルプ\n" +
                       "	i			停止\n" +
                       "	l <開始アドレス>	逆アセンブル\n" +
                       //"	me<サイズ> <開始アドレス>,<データ>,…\n" +
                       //"				メモリ編集\n" +
                       //"	mm<サイズ> <開始アドレス>,<終了アドレス¹>,<移動先アドレス>\n" +
                       //"				メモリ移動\n" +
                       //"	ms<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>\n" +
                       //"				メモリ検索\n" +
                       "	p <式>,…		計算結果表示\n" +
                       "	x			レジスタ一覧\n" +
                       "	s			スキップ\n" +
                       "	t			トレース\n" +
                       "	<式>			計算\n" +
                       "	<コマンド>;…		逐次実行\n" +
                       "    ¹終了アドレスは範囲に含まれます\n" :
                       "  Command\n" +
                       "	d<size> <start-address>,<end-address¹>	dump\n" +
                       "	f<size> <start-address>,<end-address¹>,<data>,…	fill\n" +
                       "	g			run\n" +
                       "	h			help\n" +
                       "	i			stop\n" +
                       "	l <start-address>	disassemble\n" +
                       //"	me<size> <start-address>,<data>,…\n" +
                       //"				edit memory\n" +
                       //"	mm<size> <start-address>,<end-address¹>,<destination-address>\n" +
                       //"				move memory\n" +
                       //"	ms<size> <start-address>,<end-address¹>,<data>\n" +
                       //"				search memory\n" +
                       "	p <expression>,…	calculate and print\n" +
                       "	x			register list\n" +
                       "	s			skip\n" +
                       "	t			trace\n" +
                       "	<expression>		calculate\n" +
                       "	<command>;…		sequential execution\n" +
                       "    ¹The end address is within the range.\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  サイズ\n" +
                       "	b			バイト (8bit)\n" +
                       "	w			ワード (16bit)\n" +
                       "	l			ロング (32bit)\n" +
                       "	q			クワッド (64bit)\n" +
                       "	s			シングル (32bit)\n" +
                       "	d			ダブル (64bit)\n" +
                       "	x			エクステンデッド (80bit)\n" +
                       "	y			トリプル (96bit)\n" +
                       "	p			パックトデシマル (96bit)\n" :
                       "  Size\n" +
                       "	b			byte (8bit)\n" +
                       "	w			word (16bit)\n" +
                       "	l			long (32bit)\n" +
                       "	q			quad (64bit)\n" +
                       "	s			single (32bit)\n" +
                       "	d			double (64bit)\n" +
                       "	x			extended (80bit)\n" +
                       "	y			triple (96bit)\n" +
                       "	p			packed decimal (96bit)\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  浮動小数点数\n" +
                       "	1.0e+2			10進数\n" +
                       "	0b1.1001p+6		2進数\n" +
                       "	0o1.44p+6		8進数\n" +
                       "	0x1.9p+6 $64		16進数\n" +
                       "	Infinity		無限大\n" +
                       "	NaN			非数\n" +
                       "    数学定数\n" +
                       "	Apery		ζ(3)	アペリーの定数\n" +
                       "	Catalan		G	カタランの定数\n" +
                       "	Eular		γ	オイラー・マスケローニ定数\n" +
                       "	e		e	ネイピア数\n" +
                       "	pi		π	円周率\n" +
                       "    文字コード\n" +
                       "	'A'\n" +
                       "  文字列\n" +
                       "	\"ABC\"\n" :
                       "  Floating point number\n" +
                       "	1.0e+2			decimal number\n" +
                       "	0b1.1001p+6		binary number\n" +
                       "	0o1.44p+6		octal number\n" +
                       "	0x1.9p+6 $64		hexadecimal number\n" +
                       "	Infinity		infinity\n" +
                       "	NaN			not a number\n" +
                       "    Mathematical constant\n" +
                       "	Apery		ζ(3)	Apéry's constant\n" +
                       "	Catalan		G	Catalan's constant\n" +
                       "	Eular		γ	Euler-Mascheroni constant\n" +
                       "	e		e	Napier's constant\n" +
                       "	pi		π	circular constant\n" +
                       "    Character code\n" +
                       "	'A'\n" +
                       "  String\n" +
                       "	\"ABC\"\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  レジスタ\n" +
                       "    汎用レジスタ\n" +
                       "	d0 … d7 r0 … r7	データレジスタ\n" +
                       "	a0 … a7 r8 … r15 sp	アドレスレジスタ\n" +
                       "	fp0 … fp7		浮動小数点レジスタ\n" +
                       "    制御レジスタ\n" +
                       "	pc sr ccr sfc dfc cacr tc itt0 itt1 dtt0 dtt1 buscr\n" +
                       "	usp vbr caar ssp msp isp urp srp pcr fpiar fpsr fpcr\n" :
                       "  Register\n" +
                       "    General register\n" +
                       "	d0 … d7 r0 … r7	data register\n" +
                       "	a0 … a7 r8 … r15 sp	address register\n" +
                       "	fp0 … fp7		floating point register\n" +
                       "    Control register\n" +
                       "	pc sr ccr sfc dfc cacr tc itt0 itt1 dtt0 dtt1 buscr\n" +
                       "	usp vbr caar ssp msp isp urp srp pcr fpiar fpsr fpcr\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  変数\n" +
                       "	r16 … r63		整数変数 (32bit)\n" +
                       "	fp8 … fp31		浮動小数点変数\n" :
                       "  Variable\n" +
                       "	r16 … r63		integer variable (32bit)\n" +
                       "	fp8 … fp31		floating point variable\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  アドレス\n" +
                       "	<アドレス>		現在のアドレス空間\n" +
                       "	<物理アドレス>@0	物理アドレス空間\n" +
                       "	<論理アドレス>@1	ユーザデータ空間\n" +
                       "	<論理アドレス>@2	ユーザコード空間\n" +
                       "	<論理アドレス>@5	スーパーバイザデータ空間\n" +
                       "	<論理アドレス>@6	スーパーバイザコード空間\n" :
                       "  Address\n" +
                       "	<address>		current address space\n" +
                       "	<physical-address>@0	physical address space\n" +
                       "	<logical-address>@1	user data space\n" +
                       "	<logical-address>@2	user code space\n" +
                       "	<logical-address>@5	supervisor data space\n" +
                       "	<logical-address>@6	supervisor code space\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  演算子\n" +
                       "	<汎用レジスタ>.<サイズ>	汎用レジスタアクセス\n" +
                       "	<変数>.<サイズ>		変数アクセス\n" +
                       "	[<アドレス>].<サイズ>	メモリアクセス\n" +
                       "	x.<サイズ>		キャスト\n" +
                       "	x(y)			関数呼び出し\n" +
                       "	x++ ++x x-- --x		インクリメント,デクリメント\n" +
                       "	+x -x ~x !x		符号,ビットNOT,論理NOT\n" +
                       "	x**y			累乗\n" +
                       "	x*y x/y x%y		乗除算\n" +
                       "	x+y x-y			加減算,連結\n" +
                       "	x<<y x>>y x>>>y		シフト\n" +
                       "	x<y x<=y x>y x>=y	比較\n" +
                       "	x==y x!=y		等価\n" +
                       "	x&y x^y x|y		ビットAND,XOR,OR\n" +
                       "	x&&y x||y		論理AND,OR\n" +
                       "	x?y:z			条件\n" +
                       "	x=y			代入\n" +
                       "	x**=y			累乗複合代入\n" +
                       "	x*=y x/=y x%=y		乗除算複合代入\n" +
                       "	x+=y x-=y		加減算複合代入\n" +
                       "	x<<=y x>>=y x>>>=y	シフト複合代入\n" +
                       "	x&=y x^=y x|=y		ビットAND,XOR,OR複合代入\n" +
                       "	x,y			逐次評価\n" :
                       "  Operator\n" +
                       "	<general-register>.<size>	general register access\n" +
                       "	<variable>.<size>	variable access\n" +
                       "	[<address>].<size>	memory access\n" +
                       "	x.<size>		cast\n" +
                       "	x(y)			function call\n" +
                       "	x++ ++x x-- --x		increment,decrement\n" +
                       "	+x -x ~x !x		signum,bitwise NOT,logical NOT\n" +
                       "	x**y			exponentiation\n" +
                       "	x*y x/y x%y		multiplication\n" +
                       "	x+y x-y			addition,concatenation\n" +
                       "	x<<y x>>y x>>>y		shift\n" +
                       "	x<y x<=y x>y x>=y	comparison\n" +
                       "	x==y x!=y		equality\n" +
                       "	x&y x^y x|y		bitwise AND,XOR,OR\n" +
                       "	x&&y x||y		logical AND,OR\n" +
                       "	x?y:z			conditional\n" +
                       "	x=y			simple assignment\n" +
                       "	x**=y			exponentiation compound assignment\n" +
                       "	x*=y x/=y x%=y		multiplication compound assignment\n" +
                       "	x+=y x-=y		addition compound assignment\n" +
                       "	x<<=y x>>=y x>>>=y	shift compound assignment\n" +
                       "	x&=y x^=y x|=y		bitwise AND,XOR,OR compound assignment\n" +
                       "	x,y			sequential evaluation\n");
        XEiJ.dgtPrint (Multilingual.mlnJapanese ?
                       "  関数\n" +
                       "	abs acos acosh acot acoth acsc acsch agi agm\n" +
                       "	asc asec asech asin asinh atan atan2 atanh\n" +
                       "	bin$ cbrt ceil chr$ cmp cmp0 cmp1 cmp1abs cmpabs\n" +
                       "	cos cosh cot coth csc csch cub dec deg div2 div3 divpi divrz\n" +
                       "	exp exp10 exp2 exp2m1 expm1 floor frac gamma getexp getman\n" +
                       "	hex$ ieeerem inc iseven isinf isint isnan isodd isone iszero\n" +
                       "	log log10 log1p log2 loggamma max min mul2 mul3 mulpi\n" +
                       "	oct$ pow quo rad random rcp rint rmode round rprec\n" +
                       "	sec sech sgn sin sinh sqrt squ str$ tan tanh trunc ulp val\n" :
                       "  Function\n" +
                       "	abs acos acosh acot acoth acsc acsch agi agm\n" +
                       "	asc asec asech asin asinh atan atan2 atanh\n" +
                       "	bin$ cbrt ceil chr$ cmp cmp0 cmp1 cmp1abs cmpabs\n" +
                       "	cos cosh cot coth csc csch cub dec deg div2 div3 divpi divrz\n" +
                       "	exp exp10 exp2 exp2m1 expm1 floor frac gamma getexp getman\n" +
                       "	hex$ ieeerem inc iseven isinf isint isnan isodd isone iszero\n" +
                       "	log log10 log1p log2 loggamma max min mul2 mul3 mulpi\n" +
                       "	oct$ pow quo rad random rcp rint rmode round rprec\n" +
                       "	sec sech sgn sin sinh sqrt squ str$ tan tanh trunc ulp val\n");
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("h");
      }
    },

    ETY_COMMAND_LIST {
      @Override protected void etyEval (ExpressionElement elem) {
        if (XEiJ.dgtDisassemblePC == 0) {
          XEiJ.dgtDisassemblePC = XEiJ.regPC;
          XEiJ.dgtDisassembleLastTail = XEiJ.regPC + 31 & -32;
        }
        int headAddress = XEiJ.dgtDisassemblePC;
        int tailAddress = XEiJ.dgtDisassembleLastTail + 32;  //前回の終了位置+32
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList ();
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //list x@y
              headAddress = XEiJ.dgtDisassemblePC = param.exlParamX.exlFloatValue.geti ();
              tailAddress = headAddress + 63 & -32;  //32バイト以上先の32バイト境界
              XEiJ.dgtDisassembleFC = param.exlParamY.exlFloatValue.geti ();
            } else if (param.exlValueType == ElementType.ETY_FLOAT) {  //list x
              headAddress = XEiJ.dgtDisassemblePC = param.exlFloatValue.geti ();
              tailAddress = headAddress + 63 & -32;  //32バイト以上先の32バイト境界
              XEiJ.dgtDisassembleFC = XEiJ.regSRS == 0 ? 2 : 6;
            }
          }
        }
        int supervisor = XEiJ.dgtDisassembleFC & 4;
        //ラベルの準備
        LabeledAddress.lblUpdate ();
        boolean prevBranchFlag = false;  //true=直前が完全分岐命令だった
        //命令ループ
        StringBuilder sb = new StringBuilder ();
        int itemAddress = headAddress;
        int itemEndAddress;
        do {
          //完全分岐命令の下に隙間を空けて読みやすくする
          if (prevBranchFlag) {
            sb.append ('\n');
            //ラベル
            int l = sb.length ();
            LabeledAddress.lblSearch (sb, itemAddress);
            if (l < sb.length ()) {
              sb.append ('\n');
            }
          }
          //逆アセンブルする
          String code = Disassembler.disDisassemble (new StringBuilder (), itemAddress, supervisor).toString ();
          itemEndAddress = Disassembler.disPC;
          //1行目
          int lineAddress = itemAddress;  //行の開始アドレス
          int lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
          //アドレス
          XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
          //データ
          for (int a = lineAddress; a < lineEndAddress; a += 2) {
            XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
          }
          sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + 2);
          //逆アセンブル結果
          sb.append (code).append (XEiJ.DBG_SPACES, 0, Math.max (1, 68 - 32 - code.length ()));
          //キャラクタ
          for (int a = lineAddress; a < lineEndAddress; a++) {
            int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
            int c;
            if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
              int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
              if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
                c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                if (c == 0) {  //対応する文字がない
                  c = '※';
                }
                a++;
              } else {  //SJISの2バイトコードの2バイト目ではない
                c = '.';  //SJISの2バイトコードの1バイト目ではなかった
              }
            } else {  //SJISの2バイトコードの1バイト目ではない
              c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
              if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                c = '.';
              }
            }
            sb.append ((char) c);
          }  //for a
          sb.append ('\n');
          //2行目以降
          while (lineEndAddress < itemEndAddress) {
            lineAddress = lineEndAddress;  //行の開始アドレス
            lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
            //アドレス
            XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
            //データ
            for (int a = lineAddress; a < lineEndAddress; a += 2) {
              XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
            }
            sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + (2 + 68 - 32));
            //キャラクタ
            for (int a = lineAddress; a < lineEndAddress; a++) {
              int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
              int c;
              if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
                int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
                if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
                  c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                  if (c == 0) {  //対応する文字がない
                    c = '※';
                  }
                  a++;
                } else {  //SJISの2バイトコードの2バイト目ではない
                  c = '.';  //SJISの2バイトコードの1バイト目ではなかった
                }
              } else {  //SJISの2バイトコードの1バイト目ではない
                c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
                if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                  c = '.';
                }
              }
              sb.append ((char) c);
            }  //for a
            sb.append ('\n');
          }  //while
          //完全分岐命令の下に隙間を空けて読みやすくする
          prevBranchFlag = (Disassembler.disStatus & Disassembler.DIS_ALWAYS_BRANCH) != 0;
          itemAddress = itemEndAddress;
        } while (itemAddress - headAddress < tailAddress - headAddress);
        XEiJ.dgtPrint (sb.toString ());
        XEiJ.dgtDisassemblePC = itemEndAddress;
        XEiJ.dgtDisassembleLastTail = tailAddress;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("l");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    ETY_COMMAND_PRINT {
      @Override protected void etyEval (ExpressionElement elem) {
        if (elem.exlParamX != null) {
          for (ExpressionElement param : elem.exlParamX.exlEvalCommaList ()) {
            param.exlPrint ();
          }
        }
        XEiJ.dgtPrintChar ('\n');
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("p");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    ETY_COMMAND_REGS {
      @Override protected void etyEval (ExpressionElement elem) {
        //コアの動作中はレジスタの値が刻々と変化するのでpcとsrsはコピーしてから使う
        int pc = XEiJ.regPC;
        int srs = XEiJ.regSRS;
        StringBuilder sb = new StringBuilder ();
        //1行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "PC:xxxxxxxx USP:xxxxxxxx SSP:xxxxxxxx SR:xxxx  X:b N:b Z:b V:b C:b\n"
        //  または
        //  "PC:xxxxxxxx USP:xxxxxxxx ISP:xxxxxxxx MSP:xxxxxxxx SR:xxxx  X:b N:b Z:b V:b C:b\n"
        XEiJ.fmtHex8 (sb.append ("PC:"), pc);  //PC
        XEiJ.fmtHex8 (sb.append (" USP:"), srs != 0 ? XEiJ.mpuUSP : XEiJ.regRn[15]);  //USP
        if (XEiJ.mpuCoreType == 0) {  //000
          XEiJ.fmtHex8 (sb.append (" SSP:"), srs != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | srs | XEiJ.regSRI | XEiJ.regCCR);  //SR
        } else if (XEiJ.mpuCoreType == 3) {  //030
          XEiJ.fmtHex8 (sb.append (" ISP:"), srs == 0 || XEiJ.regSRM != 0 ? XEiJ.mpuISP : XEiJ.regRn[15]);  //ISP
          XEiJ.fmtHex8 (sb.append (" MSP:"), srs == 0 || XEiJ.regSRM == 0 ? XEiJ.mpuMSP : XEiJ.regRn[15]);  //MSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | XEiJ.regSRT0 | srs | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
        } else {  //060
          XEiJ.fmtHex8 (sb.append (" SSP:"), srs != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | srs | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
        }
        sb.append ("  X:").append (XEiJ.REG_CCRXMAP[XEiJ.regCCR]);  //X
        sb.append (" N:").append (XEiJ.REG_CCRNMAP[XEiJ.regCCR]);  //N
        sb.append (" Z:").append (XEiJ.REG_CCRZMAP[XEiJ.regCCR]);  //Z
        sb.append (" V:").append (XEiJ.REG_CCRVMAP[XEiJ.regCCR]);  //V
        sb.append (" C:").append (XEiJ.REG_CCRCMAP[XEiJ.regCCR]);  //C
        sb.append ('\n');
        //2行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b\n"
        sb.append ("HI:").append (XEiJ.MPU_CCCMAP[ 2 << 5 | XEiJ.regCCR]);  //HI
        sb.append (" LS:").append (XEiJ.MPU_CCCMAP[ 3 << 5 | XEiJ.regCCR]);  //LS
        sb.append (" CC(HS):").append (XEiJ.MPU_CCCMAP[ 4 << 5 | XEiJ.regCCR]);  //CC(HS)
        sb.append (" CS(LO):").append (XEiJ.MPU_CCCMAP[ 5 << 5 | XEiJ.regCCR]);  //CS(LO)
        sb.append (" NE:").append (XEiJ.MPU_CCCMAP[ 6 << 5 | XEiJ.regCCR]);  //NE
        sb.append (" EQ:").append (XEiJ.MPU_CCCMAP[ 7 << 5 | XEiJ.regCCR]);  //EQ
        sb.append (" VC:").append (XEiJ.MPU_CCCMAP[ 8 << 5 | XEiJ.regCCR]);  //VC
        sb.append (" VS:").append (XEiJ.MPU_CCCMAP[ 9 << 5 | XEiJ.regCCR]);  //VS
        sb.append (" PL:").append (XEiJ.MPU_CCCMAP[10 << 5 | XEiJ.regCCR]);  //PL
        sb.append (" MI:").append (XEiJ.MPU_CCCMAP[11 << 5 | XEiJ.regCCR]);  //MI
        sb.append (" GE:").append (XEiJ.MPU_CCCMAP[12 << 5 | XEiJ.regCCR]);  //GE
        sb.append (" LT:").append (XEiJ.MPU_CCCMAP[13 << 5 | XEiJ.regCCR]);  //LT
        sb.append (" GT:").append (XEiJ.MPU_CCCMAP[14 << 5 | XEiJ.regCCR]);  //GT
        sb.append (" LE:").append (XEiJ.MPU_CCCMAP[15 << 5 | XEiJ.regCCR]);  //LE
        sb.append ('\n');
        //3～5行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "D0:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx D4:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n"
        //  "A0:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx A4:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n"
        XEiJ.fmtHex8 (sb.append ("D0:") , XEiJ.regRn[ 0]);  //D0
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 1]);  //D1
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 2]);  //D2
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 3]);  //D3
        XEiJ.fmtHex8 (sb.append (" D4:"), XEiJ.regRn[ 4]);  //D4
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 5]);  //D5
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 6]);  //D6
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 7]);  //D7
        sb.append ('\n');
        XEiJ.fmtHex8 (sb.append ("A0:") , XEiJ.regRn[ 8]);  //A0
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 9]);  //A1
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[10]);  //A2
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[11]);  //A3
        XEiJ.fmtHex8 (sb.append (" A4:"), XEiJ.regRn[12]);  //A4
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[13]);  //A5
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[14]);  //A6
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[15]);  //A7
        sb.append ('\n');
        if (3 <= XEiJ.mpuCoreType) {
          //6行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "SFC:x DFC:x VBR:xxxxxxxx CACR:xxxxxxxx  TCR:xxxxxxxx URP:xxxxxxxx SRP:xxxxxxxx\n"
          sb.append ("SFC:").append ((char) ('0' + XEiJ.mpuSFC));  //SFC
          sb.append (" DFC:").append ((char) ('0' + XEiJ.mpuDFC));  //DFC
          XEiJ.fmtHex8 (sb.append (" VBR:"), XEiJ.mpuVBR);  //VBR
          XEiJ.fmtHex8 (sb.append (" CACR:"), XEiJ.mpuCACR);  //CACR
          if (XEiJ.mpuCoreType == 6) {
            XEiJ.fmtHex8 (sb.append ("  TCR:"), MC68060.mmuTCR);  //TCR
            XEiJ.fmtHex8 (sb.append (" URP:"), MC68060.mmuURP);  //URP
            XEiJ.fmtHex8 (sb.append (" SRP:"), MC68060.mmuSRP);  //SRP
          }
          sb.append ('\n');
          //7行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "ITT0:xxxxxxxx ITT1:xxxxxxxx DTT0:xxxxxxxx DTT1:xxxxxxxx  PCR:xxxxxxxx\n"
          if (XEiJ.mpuCoreType == 6) {
            XEiJ.fmtHex8 (sb.append ("ITT0:"), MC68060.mmuITT0);  //ITT0
            XEiJ.fmtHex8 (sb.append (" ITT1:"), MC68060.mmuITT1);  //ITT1
            XEiJ.fmtHex8 (sb.append (" DTT0:"), MC68060.mmuDTT0);  //DTT0
            XEiJ.fmtHex8 (sb.append (" DTT1:"), MC68060.mmuDTT1);  //DTT1
            XEiJ.fmtHex8 (sb.append ("  PCR:"), XEiJ.mpuPCR);  //PCR
            sb.append ('\n');
          }
          //8行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "FPCR:xxxxxxxx FPSR:xxxxxxxx  M:b Z:b I:b N:b  B:b S:b E:b O:b U:b D:b X:b P:b\n"
          XEiJ.fmtHex8 (sb.append ("FPCR:"), XEiJ.fpuBox.epbFpcr);  //FPCR
          XEiJ.fmtHex8 (sb.append (" FPSR:"), XEiJ.fpuBox.epbFpsr);  //FPSR
          sb.append ("  M:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 27 & 1)));  //FPSR M
          sb.append (" Z:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 26 & 1)));  //FPSR Z
          sb.append (" I:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 25 & 1)));  //FPSR I
          sb.append (" N:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 24 & 1)));  //FPSR N
          sb.append ("  B:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 15 & 1)));  //FPSR BSUN
          sb.append (" S:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 14 & 1)));  //FPSR SNAN
          sb.append (" E:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 13 & 1)));  //FPSR OPERR
          sb.append (" O:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 12 & 1)));  //FPSR OVFL
          sb.append (" U:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 11 & 1)));  //FPSR UNFL
          sb.append (" D:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 10 & 1)));  //FPSR DZ
          sb.append (" X:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >>  9 & 1)));  //FPSR INEX2
          sb.append (" P:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >>  8 & 1)));  //FPSR INEX1
          sb.append ('\n');
          //9～12行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "FP0:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP1:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
          //  "FP2:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP3:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
          //  "FP4:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP5:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
          //  "FP6:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP7:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
          for (int n = 0; n <= 7; n++) {
            String s = XEiJ.fpuFPn[n].toString ();
            sb.append ("FP").append (n).append (':').append (s);
            if ((n & 1) == 0) {
              sb.append (XEiJ.DBG_SPACES, 0, Math.max (0, 36 - s.length ()));
            } else {
              sb.append ('\n');
            }
          }
        }  //if XEiJ.mpuCoreType>=3
        //ラベル
        LabeledAddress.lblUpdate ();
        {
          int l = sb.length ();
          LabeledAddress.lblSearch (sb, pc);
          if (l < sb.length ()) {
            sb.append ('\n');
          }
        }
        //逆アセンブルする
        String code = Disassembler.disDisassemble (new StringBuilder (), pc, srs).toString ();
        //アドレス
        XEiJ.fmtHex8 (sb, pc).append ("  ");
        //データ
        for (int a = pc; a < Disassembler.disPC; a += 2) {
          XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, srs));
        }
        for (int a = Disassembler.disPC; a < pc + 10; a += 2) {
          sb.append ("    ");
        }
        //コード
        sb.append ("  ").append (code).append ('\n');
        XEiJ.dgtPrint (sb.toString ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("x");
      }
    },

    ETY_COMMAND_RUN {
      @Override protected void etyEval (ExpressionElement elem) {
        XEiJ.mpuStart ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("g");
      }
    },

    ETY_COMMAND_SKIP {
      @Override protected void etyEval (ExpressionElement elem) {
        if (XEiJ.mpuTask == null) {
          XEiJ.dgtRequestRegs = true;
          XEiJ.mpuSkip ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("s");
      }
    },

    ETY_COMMAND_STOP {
      @Override protected void etyEval (ExpressionElement elem) {
        if (XEiJ.mpuTask != null) {
          if (RootPointerList.RTL_ON) {
            if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
                RootPointerList.rtlCurrentUserTaskIsStoppable) {
              XEiJ.dgtRequestRegs = true;
              XEiJ.mpuStop (null);
            }
          } else {
            XEiJ.dgtRequestRegs = true;
            XEiJ.mpuStop (null);
          }
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("i");
      }
    },

    ETY_COMMAND_TRACE {
      @Override protected void etyEval (ExpressionElement elem) {
        if (XEiJ.mpuTask == null) {
          XEiJ.dgtRequestRegs = true;
          XEiJ.mpuAdvance ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("t");
      }
    },

    //セパレータ
    ETY_SEPARATOR {  // x;y
      @Override protected void etyEval (ExpressionElement elem) {
        elem.exlParamX.exlEval ();
        elem.exlFloatValue.sete (elem.exlParamY.exlEval ().exlFloatValue);
        elem.exlStringValue = elem.exlParamY.exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ";");
      }
    },

    //仮トークン
    //  パス1で機能を確定できないか、パス2で捨てられるトークン
    //ETY_TOKEN_EXCLAMATION_MARK,  // !
    //ETY_TOKEN_QUOTATION_MARK,  // "
    //ETY_TOKEN_NUMBER_SIGN,  // #
    //ETY_TOKEN_DOLLAR_SIGN,  // $
    //ETY_TOKEN_PERCENT_SIGN,  // %
    //ETY_TOKEN_AMPERSAND,  // &
    //ETY_TOKEN_APOSTROPHE,  // '
    ETY_TOKEN_LEFT_PARENTHESIS,  // (
    ETY_TOKEN_RIGHT_PARENTHESIS,  // )
    //ETY_TOKEN_ASTERISK,  // *
    ETY_TOKEN_PLUS_SIGN,  // +
    ETY_TOKEN_PLUS_PLUS,  // ++
    //ETY_TOKEN_COMMA,  // ,
    ETY_TOKEN_HYPHEN_MINUS,  // -
    ETY_TOKEN_MINUS_MINUS,  // --
    //ETY_TOKEN_FULL_STOP,  // .
    //ETY_TOKEN_SOLIDUS,  // /
    //0-9
    ETY_TOKEN_COLON,  // :
    //ETY_TOKEN_SEMICOLON,  // ;
    //ETY_TOKEN_LESS_THAN_SIGN,  // <
    //ETY_TOKEN_EQUALS_SIGN,  // =
    //ETY_TOKEN_GREATER_THAN_SIGN,  // >
    //ETY_TOKEN_QUESTION_MARK,  // ?
    //ETY_TOKEN_COMMERCIAL_AT,  // @
    //A-Z
    ETY_TOKEN_LEFT_SQUARE_BRACKET,  // [
    //ETY_TOKEN_REVERSE_SOLIDUS,  // \\
    ETY_TOKEN_RIGHT_SQUARE_BRACKET,  // ]
    //ETY_TOKEN_CIRCUMFLEX_ACCENT,  // ^
    //_
    //ETY_TOKEN_GRAVE_ACCENT,  // `
    //a-z
    //ETY_TOKEN_LEFT_CURLY_BRACKET,  // {
    //ETY_TOKEN_VERTICAL_LINE,  // |
    //ETY_TOKEN_RIGHT_CURLY_BRACKET,  // }
    //ETY_TOKEN_TILDE,  // ~

    ETY_DUMMY;

    protected void etyEval (ExpressionElement elem) {
    }

    protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
      return sb.append ('<').append (name ()).append ('>');
    }

  };  //enum ElementType



  //========================================================================================
  //$$EXL 式の要素
  //  class ExpressionElement
  protected class ExpressionElement {
    
    protected ElementType exlType;  //要素の種類
    protected int exlSubscript;  //レジスタの番号
    protected int exlPriority;  //評価の優先順位
    protected ElementType exlValueType;  //結果の型。ETY_FLOATまたはETY_STRING
    protected EFP exlFloatValue;  //ETY_FLOATの値
    protected String exlStringValue;  //ETY_STRINGの値
    protected ExpressionElement exlParamX;  //関数と演算子の引数
    protected ExpressionElement exlParamY;
    protected ExpressionElement exlParamZ;
    protected String exlSource;  //ソース
    protected int exlOffset;  //ソースの該当部分の開始位置
    protected int exlLength;  //ソースの該当部分の長さ

    protected ExpressionElement (ElementType type, int subscript,
                                 ElementType valueType, EFP floatValue, String stringValue,
                                 String source, int offset, int length) {
      exlType = type;
      exlSubscript = subscript;
      //exlPriority = EPY_PRIORITY_PRIMITIVE;
      exlValueType = valueType;
      exlFloatValue = floatValue == null ? new EFP () : new EFP (floatValue);
      exlStringValue = stringValue;
      exlSource = source;
      exlOffset = offset;
      exlLength = length;
    }

    //------------------------------------------------------------------------
    //丸め桁数
    protected void exlSetRoundingPrec (int prec) {
      if (0 <= prec && prec <= 4) {
        epbRoundingPrec = prec;
      }
    }

    //------------------------------------------------------------------------
    //丸めモード
    protected void exlSetRoundingMode (int mode) {
      if (0 <= mode && mode <= 3) {
        epbRoundingMode = mode;
      }
    }

    //------------------------------------------------------------------------
    //整数レジスタ
    protected int exlReadRegByte (int n) {
      if (0 <= n && n <= 15) {
        return (byte) XEiJ.regRn[n];
      }
      return 0;
    }
    protected int exlReadRegWord (int n) {
      if (0 <= n && n <= 15) {
        return (short) XEiJ.regRn[n];
      }
      return 0;
    }
    protected int exlReadRegLong (int n) {
      if (0 <= n && n <= 15) {
        return XEiJ.regRn[n];
      }
      return 0;
    }
    protected void exlWriteRegByte (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~255 | x & 255;
      }
    }
    protected void exlWriteRegWord (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~65535 | x & 65535;
      }
    }
    protected void exlWriteRegLong (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = x;
      }
    }

    //------------------------------------------------------------------------
    //浮動小数点レジスタ
    protected EFP exlGetFPn (int n) {
      return ExpressionEvaluator.this.epbFPn[n];
    }
    protected void exlSetFPn (int n, EFPBox.EFP x) {
      ExpressionEvaluator.this.epbFPn[n].sete (x);
    }

    //------------------------------------------------------------------------
    //制御レジスタ
    protected int exlReadCRn (int n) {
      switch (n) {
      case EYG_PC:
        return XEiJ.regPC0;
      case EYG_SR:
        return XEiJ.regSRT1 | XEiJ.regSRT0 | XEiJ.regSRS | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR;
      case EYG_CCR:
        return XEiJ.regCCR;
      case EYG_SFC:
        return XEiJ.mpuSFC;
      case EYG_DFC:
        return XEiJ.mpuDFC;
      case EYG_CACR:
        return XEiJ.mpuCACR;
      case EYG_TC:
        return MC68060.mmuTCR;
      case EYG_ITT0:
        return MC68060.mmuITT0;
      case EYG_ITT1:
        return MC68060.mmuITT1;
      case EYG_DTT0:
        return MC68060.mmuDTT0;
      case EYG_DTT1:
        return MC68060.mmuDTT1;
      case EYG_BUSCR:
        return XEiJ.mpuBUSCR;
      case EYG_USP:
        return XEiJ.regSRS != 0 ? XEiJ.mpuUSP : XEiJ.regRn[15];
      case EYG_VBR:
        return XEiJ.mpuVBR;
      case EYG_CAAR:
        return XEiJ.mpuCAAR;
      case EYG_SSP:
        return XEiJ.regSRS != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP;
      case EYG_MSP:
        return XEiJ.regSRS == 0 || XEiJ.regSRM == 0 ? XEiJ.mpuMSP : XEiJ.regRn[15];
      case EYG_ISP:
        return XEiJ.regSRS == 0 || XEiJ.regSRM != 0 ? XEiJ.mpuISP : XEiJ.regRn[15];
      case EYG_URP:
        return MC68060.mmuURP;
      case EYG_SRP:
        return MC68060.mmuSRP;
      case EYG_PCR:
        return XEiJ.mpuPCR;
      case EYG_FPIAR:
        return XEiJ.fpuBox.epbFpiar;
      case EYG_FPSR:
        return XEiJ.fpuBox.epbFpsr;
      case EYG_FPCR:
        return XEiJ.fpuBox.epbFpcr;
      }
      return 0;
    }  //exlReadCRn(int)
    protected int exlWriteCRn (int n, int x) {
      switch (n) {
      case EYG_PC:
        XEiJ.regPC0 = x;
        break;
      case EYG_SR:
        XEiJ.regSRT1 = XEiJ.REG_SR_T1 & x;
        XEiJ.regSRT0 = XEiJ.REG_SR_T0 & x;
        //XEiJ.regSRS = XEiJ.REG_SR_S & x;
        XEiJ.regSRM = XEiJ.REG_SR_M & x;
        XEiJ.regSRI = XEiJ.REG_SR_I & x;
        XEiJ.regCCR = XEiJ.REG_CCR_MASK & x;
        x &= XEiJ.REG_SR_T1 | XEiJ.REG_SR_T0 | XEiJ.REG_SR_M | XEiJ.REG_SR_I | XEiJ.REG_CCR_MASK;
        break;
      case EYG_CCR:
        XEiJ.regCCR = x &= XEiJ.REG_CCR_MASK;
        break;
      case EYG_SFC:
        XEiJ.mpuSFC = x &= 0x00000007;
        break;
      case EYG_DFC:
        XEiJ.mpuDFC = x &= 0x00000007;
        break;
      case EYG_CACR:
        XEiJ.mpuCACR = x &= (XEiJ.mpuCoreType <= 3 ? 0x00003f1f : 0xf8e0e000);
        break;
      case EYG_TC:
        MC68060.mmuTCR = x;
        break;
      case EYG_ITT0:
        MC68060.mmuITT0 = x;
        break;
      case EYG_ITT1:
        MC68060.mmuITT1 = x;
        break;
      case EYG_DTT0:
        MC68060.mmuDTT0 = x;
        break;
      case EYG_DTT1:
        MC68060.mmuDTT1 = x;
        break;
      case EYG_BUSCR:
        XEiJ.mpuBUSCR = x &= 0xf0000000;
        break;
      case EYG_USP:
        if (XEiJ.regSRS != 0) {
          XEiJ.mpuUSP = x;
        } else {
          XEiJ.regRn[15] = x;
        }
        break;
      case EYG_VBR:
        XEiJ.mpuVBR = x &= -4;
        break;
      case EYG_CAAR:
        XEiJ.mpuCAAR = x;
        break;
      case EYG_SSP:
        if (XEiJ.regSRS != 0) {
          XEiJ.regRn[15] = x;
        } else {
          XEiJ.mpuISP = x;
        }
        break;
      case EYG_MSP:
        if (XEiJ.regSRS == 0 || XEiJ.regSRM == 0) {
          XEiJ.mpuMSP = x;
        } else {
          XEiJ.regRn[15] = x;
        }
        break;
      case EYG_ISP:
        if (XEiJ.regSRS == 0 || XEiJ.regSRM != 0) {
          XEiJ.mpuISP = x;
        } else {
          XEiJ.regRn[15] = x;
        }
        break;
      case EYG_URP:
        MC68060.mmuURP = x;
        break;
      case EYG_SRP:
        MC68060.mmuSRP = x;
        break;
      case EYG_PCR:
        XEiJ.mpuPCR = x = 0x04300500 | XEiJ.MPU_060_REV << 8 | x & 0x00000083;
        break;
      case EYG_FPIAR:
        XEiJ.fpuBox.epbFpiar = x;
        break;
      case EYG_FPSR:
        XEiJ.fpuBox.epbFpsr = x;
        break;
      case EYG_FPCR:
        XEiJ.fpuBox.epbFpcr = x;
        break;
      }
      return x;
    }  //exlWriteCRn(int,int)

    //------------------------------------------------------------------------
    //  式エバリュエータ
    //  式の内部表現を評価する
    protected ExpressionElement exlEval () {
      exlType.etyEval (this);
      return this;
    }  //exlEval()


    //  コンマリストの長さを数える
    protected int exlLengthOfCommaList () {
      return exlType == ElementType.ETY_OPERATOR_COMMA ? exlParamX.exlLengthOfCommaList () + 1 : 1;
    }

    //  コンマ演算子で連結された式を左から順に評価して結果を配列にして返す
    protected ExpressionElement[] exlEvalCommaList () {
      return exlEvalCommaListSub (new ArrayList<ExpressionElement> ()).toArray (new ExpressionElement[0]);
    }

    protected ArrayList<ExpressionElement> exlEvalCommaListSub (ArrayList<ExpressionElement> list) {
      if (exlType == ElementType.ETY_OPERATOR_COMMA) {
        exlParamX.exlEvalCommaListSub (list);
        list.add (exlParamY.exlEval ());
      } else {
        list.add (exlEval ());
      }
      return list;
    }

    protected void exlPrint () {
      switch (exlValueType) {
      case ETY_FLOAT:
        XEiJ.dgtPrint (exlFloatValue.toString ());
        break;
      case ETY_STRING:
        XEiJ.dgtPrint (exlStringValue);
        break;
      }
    }

    //  式の内部表現を文字列に変換する
    protected StringBuilder exlAppendTo (StringBuilder sb) {
      return exlType.etyAppendTo (sb, this);
    }

    //  関数呼び出し
    protected StringBuilder exlAppendFunctionTo (StringBuilder sb, String funcName) {
      sb.append (funcName).append ('(');
      if (exlParamX != null) {
        exlParamX.exlAppendTo (sb);
        if (exlParamY != null) {
          exlParamY.exlAppendTo (sb.append (','));
          if (exlParamZ != null) {
            exlParamZ.exlAppendTo (sb.append (','));
          }
        }
      }
      return sb.append (')');
    }

    //  後置演算子
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    protected StringBuilder exlAppendPostfixOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlPriority <= exlPriority) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return sb.append (text);
    }

    //  前置演算子
    //    右辺の優先順位が自分と同じか自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendPrefixOperatorTo (StringBuilder sb, String text) {
      sb.append (text);
      if (exlParamX.exlPriority <= exlPriority) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return sb;
    }

    //  二項演算子
    //    左から結合する
    //    左辺の優先順位が自分より低いとき左辺を括弧で囲む
    //    右辺の優先順位が自分と同じか自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendBinaryOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlPriority < exlPriority) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      sb.append (text);
      if (exlParamY.exlPriority <= exlPriority) {
        exlParamY.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamY.exlAppendTo (sb);
      }
      return sb;
    }

    //  条件演算子
    //    右から結合する
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    //    中辺と右辺は括弧で囲まない
    protected StringBuilder exlAppendConditionalOperatorTo (StringBuilder sb, String text1, String text2) {
      if (exlParamX.exlPriority <= exlPriority) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return exlParamZ.exlAppendTo (exlParamY.exlAppendTo (sb.append (text1)).append (text2));
    }

    //  代入演算子
    //    右から結合する
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    //    右辺の優先順位が自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendAssignmentOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlPriority <= exlPriority) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      sb.append (text);
      if (exlParamY.exlPriority < exlPriority) {
        exlParamY.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamY.exlAppendTo (sb);
      }
      return sb;
    }

    //  式の内部表現を文字列に変換する
    @Override public String toString () {
      return exlAppendTo (new StringBuilder ()).toString ();
    }

  }  //class ExpressionElement



  //  式パーサのエラー表示
  protected void evxPrintError (String message, String source, int offset, int length) {
    StringBuilder sb = new StringBuilder ();
    sb.append (message).append ('\n');
    if (source != null) {
      if (offset == -1) {
        offset = source.length ();
      }
      int head = Math.max (0, offset - 20);
      int tail = Math.min (source.length (), offset + length + 20);
      sb.append (source.substring (head, tail)).append ('\n');
      for (int i = head; i < offset; i++) {
        sb.append (' ');
      }
      for (int i = 0; i < length; i++) {
        sb.append ('^');
      }
      XEiJ.dgtPrintln (sb.toString ());
    }
  }

  //code = evxParse (source)
  //  式パーサ
  //  文字列を式の内部表現に変換する
  protected ExpressionElement evxParse (String source) {
    LinkedList<ExpressionElement> tokenList = evxParseLexically (source);
    if (tokenList == null) {
      return null;
    }
    return evxParseSyntactically (tokenList);
  }

  //evxParseLexically (source)
  //  文字列をトークンリストに変換する
  protected LinkedList<ExpressionElement> evxParseLexically (String source) {
    LinkedList<ExpressionElement> tokenList = new LinkedList<ExpressionElement> ();
    char[] a = source.toCharArray ();  //文字の配列
    int l = a.length;  //文字の配列の長さ
    int p = 0;  //次の文字の位置
    int c = p < l ? a[p++] : -1;
    while (0 <= c) {
      //          0x20        0x08         0x09         0x0a         0x0c         0x0d
      while (c == ' ' || c == '\b' || c == '\t' || c == '\n' || c == '\f' || c == '\r') {  //空白
        c = p < l ? a[p++] : -1;
      }
      if (c < 0) {
        break;
      }
      int p0 = p - 1;  //1文字目の位置
      ElementType type = ElementType.ETY_FLOAT;
      int subscript = 0;
      ElementType valueType = ElementType.ETY_UNDEF;
      EFP floatValue = null;
      String stringValue = "";
    token:
      {

        //----------------------------------------
        //浮動小数点数
        if ('0' <= c && c <= '9' || c == '$') {  //浮動小数点数
          int radix = 10;  //10進数
          int check = 0;  //1=整数部に数字がない,2=小数部に数字がない,4=指数部に数字がない,8=不明な接尾辞がある。10進数のときは既に整数部に数字がある
          if (c == '$') {  //$で始まる16進数
            radix = 16;  //16進数
            check = 1;  //整数部に数字がない
          } else if (c == '0' && p < l) {  //0で始まって2文字以上ある
            int d = a[p];  //2文字目
            if (d == 'B' || d == 'b') {  //0bで始まる2進数
              p++;
              radix = 2;  //2進数
              check = 1;  //整数部に数字がない
            } else if (d == 'O' || d == 'o') {  //0oで始まる8進数
              p++;
              radix = 8;  //8進数
              check = 1;  //整数部に数字がない
            } else if (d == 'X' || d == 'x') {  //0xで始まる16進数
              p++;
              radix = 16;  //16進数
              check = 1;  //整数部に数字がない
            }
          }
          //整数部
          //  10進数のときは2桁目以降、それ以外は1桁目から
          c = p < l ? a[p++] : -1;
          while ((radix <= 10 ? '0' <= c && c < '0' + radix :  //2進数,8進数,10進数
                  '0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f') ||  //16進数
                 c == '_') {
            if (c != '_') {
              check &= ~1;  //整数部に数字がある
            }
            c = p < l ? a[p++] : -1;
          }
          //小数部
          if (c == '.') {  //小数部がある
            check |= 2;  //小数部に数字がない
            c = p < l ? a[p++] : -1;
            while ((radix <= 10 ? '0' <= c && c < '0' + radix :  //2進数,8進数,10進数
                    '0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f') ||  //16進数
                   c == '_') {
              if (c != '_') {
                check &= ~2;  //小数部に数字がある
              }
              c = p < l ? a[p++] : -1;
            }
          }
          //指数部
          if (radix == 10 ? c == 'E' || c == 'e' :  //10進数
              c == 'P' || c == 'p') {  //2進数,8進数,16進数。指数部がある
            check |= 4;  //指数部に数字がない
            c = p < l ? a[p++] : -1;
            if (c == '+' || c == '-') {
              c = p < l ? a[p++] : -1;
            }
            while ('0' <= c && c <= '9') {
              check &= ~4;  //指数部に数字がある
              c = p < l ? a[p++] : -1;
            }
          }
          //接尾辞
          while ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z') {  //接尾辞がある
            check |= 8;  //不明な接尾辞がある
            c = p < l ? a[p++] : -1;
          }
          if (check != 0) {
            evxPrintError ((check & 1) != 0 ? "数値の整数部に数字がありません" :
                           (check & 2) != 0 ? "数値の小数部に数字がありません" :
                           (check & 4) != 0 ? "数値の指数部に数字がありません" :
                           (check & 8) != 0 ? "数値の接尾辞が間違っています" :
                           "数値の書き方が間違っています", source, p0, p - p0);
            return null;
          }
          type = ElementType.ETY_FLOAT;
          floatValue = new EFP (String.valueOf (a, p0, p - p0));
          break token;
        }  //浮動小数点数

        //----------------------------------------
        //文字
        else if (c == '\'') {  //文字
          c = p < l ? a[p++] : -1;
          if (c < 0) {
            evxPrintError ("文字がありません", source, p0, p - p0);
            return null;
          }
          type = ElementType.ETY_FLOAT;
          floatValue = new EFP ((char) c);
          c = p < l ? a[p++] : -1;
          if (c != '\'') {
            evxPrintError ("'～' が閉じていません", source, p0, p - p0);
            return null;
          }
          c = p < l ? a[p++] : -1;
          break token;
        }  //文字

        //----------------------------------------
        //文字列
        else if (c == '"') {  //文字列
          StringBuilder sb = new StringBuilder ();
          c = p < l ? a[p++] : -1;
          while (0 <= c && c != '"' && c != '\n') {
            if (c == '\\') {  //エスケープ文字
              c = p < l ? a[p++] : -1;
              if (c == '\n') {  //改行を跨ぐ
                c = p < l ? a[p++] : -1;
                continue;
              }
              if ('0' <= c && c <= '3') {  //8進数1～3桁
                c -= '0';
                int d = p < l ? a[p] : -1;
                if ('0' <= d && d <= '7') {
                  p++;
                  c = (c << 3) + (d - '0');
                  d = p < l ? a[p] : -1;
                  if ('0' <= d && d <= '7') {
                    p++;
                    c = (c << 3) + (d - '0');
                  }
                }
              } else if ('4' <= c && c <= '7') {  //8進数1～2桁
                c -= '0';
                int d = p < l ? a[p] : -1;
                if ('0' <= d && d <= '7') {
                  p++;
                  c = (c << 3) + (d - '0');
                }
              } else if (c == 'b') {
                c = '\b';
              } else if (c == 'f') {
                c = '\f';
              } else if (c == 'n') {
                c = '\n';
              } else if (c == 'r') {
                c = '\r';
              } else if (c == 't') {
                c = '\t';
              } else if (c == 'x') {  //16進数2桁
                c = 0;
                for (int i = 0; i < 2; i++) {
                  int d = p < l ? a[p++] : -1;
                  if ('0' <= d && d <= '9' || 'A' <= d && d <= 'F' || 'a' <= d && d <= 'f') {
                    evxPrintError ("不正なエスケープシーケンスです", source, p - i - 3, i + 2);
                    return null;
                  }
                  c = (c << 4) + (d <= '9' ? d - '0' : (d | 0x20) - ('a' - 10));
                }
              } else if (c == 'u') {  //16進数4桁
                c = 0;
                for (int i = 0; i < 4; i++) {
                  int d = p < l ? a[p++] : -1;
                  if ('0' <= d && d <= '9' || 'A' <= d && d <= 'F' || 'a' <= d && d <= 'f') {
                    evxPrintError ("不正なエスケープシーケンスです", source, p - i - 3, i + 2);
                    return null;
                  }
                  c = (c << 4) + (d <= '9' ? d - '0' : (d | 0x20) - ('a' - 10));
                }
              } else if (c == '\"') {
              } else if (c == '\'') {
              } else if (c == '\\') {
              } else {
                evxPrintError ("不正なエスケープシーケンスです", source, p - 3, 2);
                return null;
              }
            }  //エスケープ文字
            sb.append ((char) c);
            c = p < l ? a[p++] : -1;
          }
          if (c != '"') {
            evxPrintError ("\"～\" が閉じていません", source, p0, p - p0);
            return null;
          }
          c = p < l ? a[p++] : -1;
          type = ElementType.ETY_STRING;
          stringValue = sb.toString ();
          break token;
        }  //文字列

        //----------------------------------------
        //識別子
        else if ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z' || c == '_') {  //識別子
          c = p < l ? a[p++] : -1;
          while ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z' || c == '_' || '0' <= c && c <= '9' || c == '$') {
            c = p < l ? a[p++] : -1;
          }
          String identifier = String.valueOf (a, p0, (c < 0 ? p : p - 1) - p0);
          String lowerIdentifier = identifier.toLowerCase ();
          switch (lowerIdentifier) {
            //浮動小数点数
          case "infinity":
            type = ElementType.ETY_FLOAT;
            floatValue = INF;
            break token;
          case "nan":
            type = ElementType.ETY_FLOAT;
            floatValue = NAN;
            break token;
            //数学定数
          case "apery":
            type = ElementType.ETY_MATH_APERY;
            break token;
          case "catalan":
            type = ElementType.ETY_MATH_CATALAN;
            break token;
          case "euler":
            type = ElementType.ETY_MATH_EULER;
            break token;
          case "e":
            type = ElementType.ETY_MATH_NAPIER;
            break token;
          case "pi":
            type = ElementType.ETY_MATH_PI;
            break token;
            //レジスタ
          case "d0":
          case "d1":
          case "d2":
          case "d3":
          case "d4":
          case "d5":
          case "d6":
          case "d7":
            type = ElementType.ETY_REGISTER_RN;
            subscript = Integer.parseInt (lowerIdentifier.substring (1));
            break token;
          case "a0":
          case "a1":
          case "a2":
          case "a3":
          case "a4":
          case "a5":
          case "a6":
          case "a7":
            type = ElementType.ETY_REGISTER_RN;
            subscript = 8 + Integer.parseInt (lowerIdentifier.substring (1));
            break token;
          case "sp":
            type = ElementType.ETY_REGISTER_RN;
            subscript = 15;
            break token;
          case "r0":
          case "r1":
          case "r2":
          case "r3":
          case "r4":
          case "r5":
          case "r6":
          case "r7":
          case "r8":
          case "r9":
          case "r10":
          case "r11":
          case "r12":
          case "r13":
          case "r14":
          case "r15":
            type = ElementType.ETY_REGISTER_RN;
            subscript = Integer.parseInt (lowerIdentifier.substring (1));
            break token;
          case "fp0":
          case "fp1":
          case "fp2":
          case "fp3":
          case "fp4":
          case "fp5":
          case "fp6":
          case "fp7":
            type = ElementType.ETY_REGISTER_FPN;
            subscript = Integer.parseInt (lowerIdentifier.substring (2));
            break token;
          case "pc":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_PC;
            break token;
          case "sr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_SR;
            break token;
          case "ccr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_CCR;
            break token;
          case "sfc":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_SFC;
            break token;
          case "dfc":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_DFC;
            break token;
          case "cacr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_CACR;
            break token;
          case "tc":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_TC;
            break token;
          case "itt0":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_ITT0;
            break token;
          case "itt1":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_ITT1;
            break token;
          case "dtt0":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_DTT0;
            break token;
          case "dtt1":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_DTT1;
            break token;
          case "buscr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_BUSCR;
            break token;
          case "usp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_USP;
            break token;
          case "vbr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_VBR;
            break token;
          case "caar":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_CAAR;
            break token;
          case "ssp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_SSP;
            break token;
          case "msp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_MSP;
            break token;
          case "isp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_ISP;
            break token;
          case "urp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_URP;
            break token;
          case "srp":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_SRP;
            break token;
          case "pcr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_PCR;
            break token;
          case "fpiar":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_FPIAR;
            break token;
          case "fpsr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_FPSR;
            break token;
          case "fpcr":
            type = ElementType.ETY_REGISTER_CRN;
            subscript = EYG_FPCR;
            break token;
            //関数
          case "abs":
            type = ElementType.ETY_FUNCTION_ABS;
            break token;
          case "acos":
            type = ElementType.ETY_FUNCTION_ACOS;
            break token;
          case "acosh":
            type = ElementType.ETY_FUNCTION_ACOSH;
            break token;
          case "acot":
            type = ElementType.ETY_FUNCTION_ACOT;
            break token;
          case "acoth":
            type = ElementType.ETY_FUNCTION_ACOTH;
            break token;
          case "acsc":
            type = ElementType.ETY_FUNCTION_ACSC;
            break token;
          case "acsch":
            type = ElementType.ETY_FUNCTION_ACSCH;
            break token;
          case "agi":
            type = ElementType.ETY_FUNCTION_AGI;
            break token;
          case "agm":
            type = ElementType.ETY_FUNCTION_AGM;
            break token;
          case "asc":
            type = ElementType.ETY_FUNCTION_ASC;
            break token;
          case "asec":
            type = ElementType.ETY_FUNCTION_ASEC;
            break token;
          case "asech":
            type = ElementType.ETY_FUNCTION_ASECH;
            break token;
          case "asin":
            type = ElementType.ETY_FUNCTION_ASIN;
            break token;
          case "asinh":
            type = ElementType.ETY_FUNCTION_ASINH;
            break token;
          case "atan":
            type = ElementType.ETY_FUNCTION_ATAN;
            break token;
          case "atan2":
            type = ElementType.ETY_FUNCTION_ATAN2;
            break token;
          case "atanh":
            type = ElementType.ETY_FUNCTION_ATANH;
            break token;
          case "bin$":
            type = ElementType.ETY_FUNCTION_BIN_DOLLAR;
            break token;
          case "cbrt":
            type = ElementType.ETY_FUNCTION_CBRT;
            break token;
          case "ceil":
            type = ElementType.ETY_FUNCTION_CEIL;
            break token;
          case "chr$":
            type = ElementType.ETY_FUNCTION_CHR_DOLLAR;
            break token;
          case "cmp":
            type = ElementType.ETY_FUNCTION_CMP;
            break token;
          case "cmp0":
            type = ElementType.ETY_FUNCTION_CMP0;
            break token;
          case "cmp1":
            type = ElementType.ETY_FUNCTION_CMP1;
            break token;
          case "cmp1abs":
            type = ElementType.ETY_FUNCTION_CMP1ABS;
            break token;
          case "cmpabs":
            type = ElementType.ETY_FUNCTION_CMPABS;
            break token;
          case "cos":
            type = ElementType.ETY_FUNCTION_COS;
            break token;
          case "cosh":
            type = ElementType.ETY_FUNCTION_COSH;
            break token;
          case "cot":
            type = ElementType.ETY_FUNCTION_COT;
            break token;
          case "coth":
            type = ElementType.ETY_FUNCTION_COTH;
            break token;
          case "csc":
            type = ElementType.ETY_FUNCTION_CSC;
            break token;
          case "csch":
            type = ElementType.ETY_FUNCTION_CSCH;
            break token;
          case "cub":
            type = ElementType.ETY_FUNCTION_CUB;
            break token;
          case "dec":
            type = ElementType.ETY_FUNCTION_DEC;
            break token;
          case "deg":
            type = ElementType.ETY_FUNCTION_DEG;
            break token;
          case "div2":
            type = ElementType.ETY_FUNCTION_DIV2;
            break token;
          case "div3":
            type = ElementType.ETY_FUNCTION_DIV3;
            break token;
          case "divpi":
            type = ElementType.ETY_FUNCTION_DIVPI;
            break token;
          case "divrz":
            type = ElementType.ETY_FUNCTION_DIVRZ;
            break token;
          case "exp":
            type = ElementType.ETY_FUNCTION_EXP;
            break token;
          case "exp10":
            type = ElementType.ETY_FUNCTION_EXP10;
            break token;
          case "exp2":
            type = ElementType.ETY_FUNCTION_EXP2;
            break token;
          case "exp2m1":
            type = ElementType.ETY_FUNCTION_EXP2M1;
            break token;
          case "expm1":
            type = ElementType.ETY_FUNCTION_EXPM1;
            break token;
          case "floor":
            type = ElementType.ETY_FUNCTION_FLOOR;
            break token;
          case "frac":
            type = ElementType.ETY_FUNCTION_FRAC;
            break token;
          case "gamma":
            type = ElementType.ETY_FUNCTION_GAMMA;
            break token;
          case "getexp":
            type = ElementType.ETY_FUNCTION_GETEXP;
            break token;
          case "getman":
            type = ElementType.ETY_FUNCTION_GETMAN;
            break token;
          case "hex$":
            type = ElementType.ETY_FUNCTION_HEX_DOLLAR;
            break token;
          case "ieeerem":
            type = ElementType.ETY_FUNCTION_IEEEREM;
            break token;
          case "inc":
            type = ElementType.ETY_FUNCTION_INC;
            break token;
          case "iseven":
            type = ElementType.ETY_FUNCTION_ISEVEN;
            break token;
          case "isinf":
            type = ElementType.ETY_FUNCTION_ISINF;
            break token;
          case "isint":
            type = ElementType.ETY_FUNCTION_ISINT;
            break token;
          case "isnan":
            type = ElementType.ETY_FUNCTION_ISNAN;
            break token;
          case "isodd":
            type = ElementType.ETY_FUNCTION_ISODD;
            break token;
          case "isone":
            type = ElementType.ETY_FUNCTION_ISONE;
            break token;
          case "iszero":
            type = ElementType.ETY_FUNCTION_ISZERO;
            break token;
          case "log":
            type = ElementType.ETY_FUNCTION_LOG;
            break token;
          case "log10":
            type = ElementType.ETY_FUNCTION_LOG10;
            break token;
          case "log1p":
            type = ElementType.ETY_FUNCTION_LOG1P;
            break token;
          case "log2":
            type = ElementType.ETY_FUNCTION_LOG2;
            break token;
          case "loggamma":
            type = ElementType.ETY_FUNCTION_LOGGAMMA;
            break token;
          case "max":
            type = ElementType.ETY_FUNCTION_MAX;
            break token;
          case "min":
            type = ElementType.ETY_FUNCTION_MIN;
            break token;
          case "mul2":
            type = ElementType.ETY_FUNCTION_MUL2;
            break token;
          case "mul3":
            type = ElementType.ETY_FUNCTION_MUL3;
            break token;
          case "mulpi":
            type = ElementType.ETY_FUNCTION_MULPI;
            break token;
          case "oct$":
            type = ElementType.ETY_FUNCTION_OCT_DOLLAR;
            break token;
          case "pow":
            type = ElementType.ETY_FUNCTION_POW;
            break token;
          case "quo":
            type = ElementType.ETY_FUNCTION_QUO;
            break token;
          case "rad":
            type = ElementType.ETY_FUNCTION_RAD;
            break token;
          case "random":
            type = ElementType.ETY_FUNCTION_RANDOM;
            break token;
          case "rcp":
            type = ElementType.ETY_FUNCTION_RCP;
            break token;
          case "rint":
            type = ElementType.ETY_FUNCTION_RINT;
            break token;
          case "rmode":
            type = ElementType.ETY_FUNCTION_RMODE;
            break token;
          case "round":
            type = ElementType.ETY_FUNCTION_ROUND;
            break token;
          case "rprec":
            type = ElementType.ETY_FUNCTION_RPREC;
            break token;
          case "sec":
            type = ElementType.ETY_FUNCTION_SEC;
            break token;
          case "sech":
            type = ElementType.ETY_FUNCTION_SECH;
            break token;
          case "sgn":
            type = ElementType.ETY_FUNCTION_SGN;
            break token;
          case "sin":
            type = ElementType.ETY_FUNCTION_SIN;
            break token;
          case "sinh":
            type = ElementType.ETY_FUNCTION_SINH;
            break token;
          case "sqrt":
            type = ElementType.ETY_FUNCTION_SQRT;
            break token;
          case "squ":
            type = ElementType.ETY_FUNCTION_SQU;
            break token;
          case "str$":
            type = ElementType.ETY_FUNCTION_STR_DOLLAR;
            break token;
          case "tan":
            type = ElementType.ETY_FUNCTION_TAN;
            break token;
          case "tanh":
            type = ElementType.ETY_FUNCTION_TANH;
            break token;
          case "trunc":
            type = ElementType.ETY_FUNCTION_TRUNC;
            break token;
          case "ulp":
            type = ElementType.ETY_FUNCTION_ULP;
            break token;
          case "val":
            type = ElementType.ETY_FUNCTION_VAL;
            break token;
            //コマンド
          case "d":
          case "db":
          case "dw":
          case "dl":
          case "dq":
          case "ds":
          case "dd":
          case "dx":
          case "dy":
          case "dp":
            type = ElementType.ETY_COMMAND_DUMP;
            subscript = lowerIdentifier.length () < 2 ? 'b' : lowerIdentifier.charAt (1);
            break token;
          case "f":
          case "fb":
          case "fw":
          case "fl":
          case "fq":
          case "fs":
          case "fd":
          case "fx":
          case "fy":
          case "fp":
            type = ElementType.ETY_COMMAND_FILL;
            subscript = lowerIdentifier.length () < 2 ? 'b' : lowerIdentifier.charAt (1);
            break token;
          case "h":
            type = ElementType.ETY_COMMAND_HELP;
            break token;
          case "l":
            type = ElementType.ETY_COMMAND_LIST;
            break token;
          case "p":
            type = ElementType.ETY_COMMAND_PRINT;
            break token;
          case "x":
            type = ElementType.ETY_COMMAND_REGS;
            break token;
          case "g":
            type = ElementType.ETY_COMMAND_RUN;
            break token;
          case "s":
            type = ElementType.ETY_COMMAND_SKIP;
            break token;
          case "i":
            type = ElementType.ETY_COMMAND_STOP;
            break token;
          case "t":
            type = ElementType.ETY_COMMAND_TRACE;
            break token;
          default:
            {
              Integer value = epbConstLongMap.get (lowerIdentifier);  //ロング定数
              if (value != null) {
                type = ElementType.ETY_FLOAT;
                floatValue = new EFP (value.intValue ());
                break token;
              }
            }
            type = (lowerIdentifier.endsWith ("$") ? ElementType.ETY_VARIABLE_STRING :  //"$"で終わっていたら文字列変数
                    ElementType.ETY_VARIABLE_FLOAT);  //それ以外は数値変数
            stringValue = identifier;  //小文字化されていない方
            break token;
          }
        }  //識別子

        //----------------------------------------
        //演算子
        else {  //演算子
          int d = p < l ? a[p] : -1;
          int e = p + 1 < l ? a[p + 1] : -1;
          int f = p + 2 < l ? a[p + 2] : -1;
          if (c == '!') {
            if (d == '=') {  // !=
              p++;
              type = ElementType.ETY_OPERATOR_NOT_EQUAL;
            } else {  // !
              type = ElementType.ETY_OPERATOR_LOGICAL_NOT;
            }
          } else if (c == '%') {
            if (d == '=') {  // %=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_MODULUS;
            } else {  // %
              type = ElementType.ETY_OPERATOR_MODULUS;
            }
          } else if (c == '&') {
            if (d == '&') {  // &&
              p++;
              type = ElementType.ETY_OPERATOR_LOGICAL_AND;
            } else if (d == '=') {  // &=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_AND;
            } else {  // &
              type = ElementType.ETY_OPERATOR_BITWISE_AND;
            }
          } else if (c == '(') {
            type = ElementType.ETY_TOKEN_LEFT_PARENTHESIS;
          } else if (c == ')') {
            type = ElementType.ETY_TOKEN_RIGHT_PARENTHESIS;
          } else if (c == '*') {
            if (d == '*') {  // **
              if (e == '=') {  // **=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_POWER;
              } else {  // **
                p++;
                type = ElementType.ETY_OPERATOR_POWER;
              }
            } else if (d == '=') {  // *=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_MULTIPLICATION;
            } else {  // *
              type = ElementType.ETY_OPERATOR_MULTIPLICATION;
            }
          } else if (c == '+') {
            if (d == '+') {  // ++
              p++;
              type = ElementType.ETY_TOKEN_PLUS_PLUS;
            } else if (d == '=') {  // +=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_ADDITION;
            } else {  // +
              type = ElementType.ETY_TOKEN_PLUS_SIGN;
            }
          } else if (c == ',') {
            type = ElementType.ETY_OPERATOR_COMMA;
          } else if (c == '-') {
            if (d == '-') {  // --
              p++;
              type = ElementType.ETY_TOKEN_MINUS_MINUS;
            } else if (d == '=') {  // -=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_SUBTRACTION;
            } else {  // -
              type = ElementType.ETY_TOKEN_HYPHEN_MINUS;
            }
          } else if (c == '.') {
            if (('A' <= d && d <= 'Z' || d == '_' || 'a' <= d & d <= 'z') &&  //直後が英字で
                !('0' <= e && e <= '9' || 'A' <= e && e <= 'Z' || e == '_' || 'a' <= e & e <= 'z')) {  //その次が英数字でない
              if (d == 'B' || d == 'b') {  // .b
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_BYTE;
              } else if (d == 'W' || d == 'w') {  // .w
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_WORD;
              } else if (d == 'L' || d == 'l') {  // .l
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_LONG;
              } else if (d == 'Q' || d == 'q') {  // .q
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_QUAD;
              } else if (d == 'S' || d == 's') {  // .s
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_SINGLE;
              } else if (d == 'D' || d == 'd') {  // .d
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_DOUBLE;
              } else if (d == 'X' || d == 'x') {  // .x
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_EXTENDED;
              } else if (d == 'Y' || d == 'y') {  // .y
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_TRIPLE;
              } else if (d == 'P' || d == 'p') {  // .p
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_PACKED;
              } else {
                evxPrintError ("文法エラー", source, p0, 1);
                return null;
              }
            } else {
              evxPrintError ("文法エラー", source, p0, 1);
              return null;
            }
          } else if (c == '/') {
            if (d == '=') {  // /=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_DIVISION;
            } else {  // /
              type = ElementType.ETY_OPERATOR_DIVISION;
            }
          } else if (c == ':') {
            type = ElementType.ETY_TOKEN_COLON;
          } else if (c == ';') {
            type = ElementType.ETY_SEPARATOR;
          } else if (c == '<') {
            if (d == '<') {
              if (e == '=') {  // <<=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_LEFT_SHIFT;
              } else {  // <<
                p++;
                type = ElementType.ETY_OPERATOR_LEFT_SHIFT;
              }
            } else if (d == '=') {  // <=
              p++;
              type = ElementType.ETY_OPERATOR_LESS_OR_EQUAL;
            } else {  // <
              type = ElementType.ETY_OPERATOR_LESS_THAN;
            }
          } else if (c == '=') {
            if (d == '=') {  // ==
              p++;
              type = ElementType.ETY_OPERATOR_EQUAL;
            } else {  // =
              type = ElementType.ETY_OPERATOR_ASSIGNMENT;
            }
          } else if (c == '>') {
            if (d == '>') {
              if (e == '>') {
                if (f == '=') {  // >>>=
                  p += 3;
                  type = ElementType.ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT;
                } else {  // >>>
                  p += 2;
                  type = ElementType.ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT;
                }
              } else if (e == '=') {  // >>=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_RIGHT_SHIFT;
              } else {  // >>
                p++;
                type = ElementType.ETY_OPERATOR_RIGHT_SHIFT;
              }
            } else if (d == '=') {  // >=
              p++;
              type = ElementType.ETY_OPERATOR_GREATER_OR_EQUAL;
            } else {  // >
              type = ElementType.ETY_OPERATOR_GREATER_THAN;
            }
          } else if (c == '?') {
            type = ElementType.ETY_OPERATOR_CONDITIONAL;
          } else if (c == '@') {
            type = ElementType.ETY_OPERATOR_AT;
          } else if (c == '[') {
            type = ElementType.ETY_OPERATOR_MEMORY;
          } else if (c == ']') {
            type = ElementType.ETY_TOKEN_RIGHT_SQUARE_BRACKET;
          } else if (c == '^') {
            if (d == '=') {  // ^=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_XOR;
            } else {  // ^
              type = ElementType.ETY_OPERATOR_BITWISE_XOR;
            }
          } else if (c == '|') {
            if (d == '|') {  // ||
              p++;
              type = ElementType.ETY_OPERATOR_LOGICAL_OR;
            } else if (d == '=') {  // |=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_OR;
            } else {  // |
              type = ElementType.ETY_OPERATOR_BITWISE_OR;
            }
          } else if (c == '~') {
            type = ElementType.ETY_OPERATOR_BITWISE_NOT;
          } else {
            evxPrintError ("使用できない文字です", source, p0, 1);
            return null;
          }
          c = p < l ? a[p++] : -1;
        }  //演算子

      }  //token:
      tokenList.add (new ExpressionElement (type, subscript, valueType, floatValue, stringValue, source, p0, (c < 0 ? p : p - 1) - p0));
    }  //for
    if (false) {
      for (ExpressionElement elem : tokenList) {
        XEiJ.dgtPrintln (elem.exlType.name ());
      }
    }
    return tokenList;
  }  //evxParseLexically

  //code = evxParseSyntactically (tokenList)
  //  トークンリストを式の内部表現に変換する
  //  呼び出す側が左辺と右辺のどちらをパースしようとしているかによって呼び出される側の動作を変えてもよい
  protected ExpressionElement evxParseSyntactically (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement codeTree = evxParseSeparator (tokenList);
    if (codeTree == null) {
      return null;
    }
    if (codeTree.exlValueType == ElementType.ETY_UNDEF) {
      evxPrintError ("文法エラー", codeTree.exlSource, codeTree.exlOffset, codeTree.exlLength);
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      evxPrintError ("; がありません", elem.exlSource, elem.exlOffset, 1);
      return null;
    }
    return codeTree;
  }  //evxParseSyntactically

  //  基本要素
  protected ExpressionElement evxParsePrimitive (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement elem = tokenList.pollFirst ();
    if (elem == null) {
      return null;
    }
    switch (elem.exlType) {

      //数値変数
    case ETY_VARIABLE_FLOAT:
      {
        String variableName = elem.exlStringValue;
        ExpressionElement variableBody = evxVariableMap.get (variableName);  //探す
        if (variableBody == null) {
          variableBody = new ExpressionElement (ElementType.ETY_VARIABLE_FLOAT, 0,
                                                ElementType.ETY_FLOAT, new EFP (), "",
                                                elem.exlSource, elem.exlOffset, elem.exlLength);  //作る
          evxVariableMap.put (variableName, variableBody);  //登録する
        }
        elem.exlType = ElementType.ETY_VARIABLE_FLOAT;
        elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
        elem.exlValueType = ElementType.ETY_FLOAT;
        elem.exlParamX = variableBody;  //変数の本体
        return elem;
      }

      //文字列変数
    case ETY_VARIABLE_STRING:
      {
        String variableName = elem.exlStringValue;
        ExpressionElement variableBody = evxVariableMap.get (variableName);  //探す
        if (variableBody == null) {
          variableBody = new ExpressionElement (ElementType.ETY_VARIABLE_STRING, 0,
                                                ElementType.ETY_STRING, new EFP (), "",
                                                elem.exlSource, elem.exlOffset, elem.exlLength);  //作る
          evxVariableMap.put (variableName, variableBody);  //登録する
        }
        elem.exlType = ElementType.ETY_VARIABLE_STRING;
        elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
        elem.exlValueType = ElementType.ETY_STRING;
        elem.exlParamX = variableBody;  //変数の本体
        return elem;
      }

      //浮動小数点数
    case ETY_FLOAT:
      elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
      elem.exlValueType = ElementType.ETY_FLOAT;
      return elem;

      //文字列
    case ETY_STRING:
      elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
      elem.exlValueType = ElementType.ETY_STRING;
      return elem;

      //数学定数
    case ETY_MATH_APERY:
    case ETY_MATH_CATALAN:
    case ETY_MATH_EULER:
    case ETY_MATH_NAPIER:
    case ETY_MATH_PI:
      elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
      elem.exlValueType = ElementType.ETY_FLOAT;
      return elem;

      //  レジスタ
    case ETY_REGISTER_RN:
    case ETY_REGISTER_FPN:
    case ETY_REGISTER_CRN:
      elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
      elem.exlValueType = ElementType.ETY_FLOAT;
      return elem;

      //関数
    case ETY_FUNCTION_ABS:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACOS:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACOSH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACOT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACOTH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACSC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ACSCH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_AGI:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_AGM:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ASC:
      return evxParseFunctionFloatString (elem, tokenList);
    case ETY_FUNCTION_ASEC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ASECH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ASIN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ASINH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ATAN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ATAN2:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ATANH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_BIN_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList);
    case ETY_FUNCTION_CBRT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CEIL:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CHR_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList);
    case ETY_FUNCTION_CMP:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CMP0:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CMP1:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CMP1ABS:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CMPABS:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_COS:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_COSH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_COT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_COTH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CSC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CSCH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_CUB:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DEC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DEG:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DIV2:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DIV3:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DIVPI:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_DIVRZ:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_EXP:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_EXP10:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_EXP2:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_EXP2M1:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_EXPM1:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_FLOOR:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_FRAC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_GAMMA:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_GETEXP:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_GETMAN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_HEX_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList);
    case ETY_FUNCTION_IEEEREM:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_INC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISEVEN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISINF:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISINT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISNAN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISODD:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISONE:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ISZERO:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_LOG:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_LOG10:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_LOG1P:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_LOG2:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_LOGGAMMA:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_MAX:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_MIN:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_MUL2:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_MUL3:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_MULPI:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_OCT_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList);
    case ETY_FUNCTION_POW:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_QUO:
      return evxParseFunctionFloatFloatFloat (elem, tokenList);
    case ETY_FUNCTION_RAD:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_RANDOM:
      return evxParseFunctionFloat (elem, tokenList);
    case ETY_FUNCTION_RCP:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_RINT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_RMODE:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ROUND:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_RPREC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SEC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SECH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SGN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SIN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SINH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SQRT:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_SQU:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_STR_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList);
    case ETY_FUNCTION_TAN:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_TANH:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_TRUNC:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_ULP:
      return evxParseFunctionFloatFloat (elem, tokenList);
    case ETY_FUNCTION_VAL:
      return evxParseFunctionFloatString (elem, tokenList);

      //メモリ参照
    case ETY_OPERATOR_MEMORY:  // [x]
      ExpressionElement paramX = evxParseAssignment (tokenList);  //コンマ演算子の次に優先順位の低い演算子
      if (paramX == null) {
        return null;
      }
      ExpressionElement bracket = tokenList.pollFirst ();  // ]
      if (bracket == null) {  //]がない
        evxPrintError ("] がありません", elem.exlSource, -1, 1);
        return null;
      }
      if (bracket.exlType != ElementType.ETY_TOKEN_RIGHT_SQUARE_BRACKET) {  //]がない
        evxPrintError ("] がありません", bracket.exlSource, bracket.exlOffset, 1);
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {  //1番目の引数の型が違う
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = bracket.exlOffset + bracket.exlLength - elem.exlOffset;
      return elem;

      //括弧
    case ETY_TOKEN_LEFT_PARENTHESIS:  // (x)
      elem = evxParseComma (tokenList);  //最も優先順位の低い演算子
      if (elem == null) {
        return null;
      }
      ExpressionElement paren = tokenList.pollFirst ();  // )
      if (paren == null) {  //)がない
        evxPrintError (") がありません", elem.exlSource, elem.exlOffset, 1);
        return null;
      }
      if (paren.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
        evxPrintError (") がありません", paren.exlSource, paren.exlOffset, 1);
        return null;
      }
      return elem;

    }
    return null;
  }  //evxParsePrimitive

  //  0引数関数
  protected ExpressionElement evxParseFunctionFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction0 (elem, tokenList,
                              ElementType.ETY_FLOAT);
  }  //evxParseFunctionFloat
  protected ExpressionElement evxParseFunctionString (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction0 (elem, tokenList,
                              ElementType.ETY_STRING);
  }  //evxParseFunctionString
  protected ExpressionElement evxParseFunction0 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError ("( がありません", elem.exlSource, -1, 1);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (") がありません", elem.exlSource, -1, 1);
      return null;
    }
    elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
    elem.exlValueType = valueType;
    elem.exlParamX = null;
    elem.exlParamY = null;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction0

  //  1引数関数
  protected ExpressionElement evxParseFunctionFloatFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_FLOAT);
  }  //evxParseFunctionFloatFloat
  protected ExpressionElement evxParseFunctionFloatString (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_STRING);
  }  //evxParseFunctionFloatString
  protected ExpressionElement evxParseFunctionStringFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_STRING, ElementType.ETY_FLOAT);
  }  //evxParseFunctionStringFloat
  protected ExpressionElement evxParseFunction1 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType, ElementType paramTypeX) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError ("( がありません", elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramX = evxParseAssignment (tokenList);  //コンマ演算子の次に優先順位の低い演算子
    if (paramX == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeX) {  //1番目の引数の型が違う
      evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (") がありません", elem.exlSource, -1, 1);
      return null;
    }
    elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
    elem.exlValueType = valueType;
    elem.exlParamX = paramX;
    elem.exlParamY = null;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction1

  //  2引数関数
  protected ExpressionElement evxParseFunctionFloatFloatFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList) {
    return evxParseFunction2 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_FLOAT, ElementType.ETY_FLOAT);
  }  //evxParseFunctionFloatFloatFloat
  protected ExpressionElement evxParseFunction2 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType, ElementType paramTypeX, ElementType paramTypeY) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError ("( がありません", elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramX = evxParseAssignment (tokenList);  //コンマ演算子の次に優先順位の低い演算子
    if (paramX == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeX) {  //1番目の引数の型が違う
      evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //,
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_OPERATOR_COMMA) {  //,がない
      evxPrintError (", がありません", elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramY = evxParseAssignment (tokenList);  //コンマ演算子の次に優先順位の低い演算子
    if (paramY == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeY) {  //2番目の引数の型が違う
      evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (") がありません", elem.exlSource, -1, 1);
      return null;
    }
    elem.exlPriority = EPY_PRIORITY_PRIMITIVE;
    elem.exlValueType = valueType;
    elem.exlParamX = paramX;
    elem.exlParamY = paramY;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction2

  //  ＠演算子
  protected ExpressionElement evxParseAt (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParsePrimitive (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_AT:  // x@y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParsePrimitive (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_AT;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseAt

  //  後置演算子
  protected ExpressionElement evxParsePostfix (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseAt (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_TOKEN_PLUS_PLUS:  // x++
        elem.exlType = ElementType.ETY_OPERATOR_POSTINCREMENT;
        elem.exlPriority = EPY_PRIORITY_POSTFIX;
        break;
      case ETY_TOKEN_MINUS_MINUS:  // x--
        elem.exlType = ElementType.ETY_OPERATOR_POSTDECREMENT;
        elem.exlPriority = EPY_PRIORITY_POSTFIX;
        break;
      case ETY_OPERATOR_SIZE_BYTE:  // x.b
      case ETY_OPERATOR_SIZE_WORD:  // x.w
      case ETY_OPERATOR_SIZE_LONG:  // x.l
      case ETY_OPERATOR_SIZE_QUAD:  // x.q
      case ETY_OPERATOR_SIZE_SINGLE:  // x.s
      case ETY_OPERATOR_SIZE_DOUBLE:  // x.d
      case ETY_OPERATOR_SIZE_EXTENDED:  // x.x
      case ETY_OPERATOR_SIZE_TRIPLE:  // x.y
      case ETY_OPERATOR_SIZE_PACKED:  // x.p
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_POSTFIX;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      int t = elem.exlOffset + elem.exlLength;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = t - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParsePostfix

  //  前置演算子
  protected ExpressionElement evxParsePrefix (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem == null) {
      return null;
    }
    switch (elem.exlType) {
    case ETY_TOKEN_PLUS_PLUS:  // ++x
      elem.exlType = ElementType.ETY_OPERATOR_PREINCREMENT;
      elem.exlPriority = EPY_PRIORITY_PREFIX;
      break;
    case ETY_TOKEN_MINUS_MINUS:  // --x
      elem.exlType = ElementType.ETY_OPERATOR_PREDECREMENT;
      elem.exlPriority = EPY_PRIORITY_PREFIX;
      break;
    case ETY_TOKEN_PLUS_SIGN:  // +x
      elem.exlType = ElementType.ETY_OPERATOR_NOTHING;
      elem.exlPriority = EPY_PRIORITY_PREFIX;
      break;
    case ETY_TOKEN_HYPHEN_MINUS:  //-x
      elem.exlType = ElementType.ETY_OPERATOR_NEGATION;
      elem.exlPriority = EPY_PRIORITY_PREFIX;
      break;
    case ETY_OPERATOR_BITWISE_NOT:  // ~x
    case ETY_OPERATOR_LOGICAL_NOT:  // !x
      break;
    default:
      return evxParsePostfix (tokenList);
    }
    tokenList.pollFirst ();
    ExpressionElement paramX = evxParsePrefix (tokenList);  //右から結合するので自分を呼ぶ
    if (paramX == null) {
      return null;
    }
    if (paramX.exlValueType != ElementType.ETY_FLOAT) {
      evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      return null;
    }
    elem.exlPriority = EPY_PRIORITY_PREFIX;
    elem.exlValueType = ElementType.ETY_FLOAT;
    elem.exlParamX = paramX;
    elem.exlLength = paramX.exlOffset + paramX.exlLength - elem.exlOffset;
    return elem;
  }  //evxParsePrefix

  //  累乗演算子
  protected ExpressionElement evxParseExponentiation (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParsePrefix (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_POWER:  // x**y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseExponentiation (tokenList);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_EXPONENTIATION;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseExponentiation

  //  乗除算演算子
  protected ExpressionElement evxParseMultiplication (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseExponentiation (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_MULTIPLICATION:  // x*y
      case ETY_OPERATOR_DIVISION:  // x/y
      case ETY_OPERATOR_MODULUS:  //x%y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseExponentiation (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_MULTIPLICATION;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseMultiplication

  //  加減算演算子
  protected ExpressionElement evxParseAddition (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseMultiplication (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      ExpressionElement paramY;
      switch (elem.exlType) {
      case ETY_TOKEN_PLUS_SIGN:  // x+y
        tokenList.pollFirst ();
        paramY = evxParseMultiplication (tokenList);
        if (paramY == null) {
          return null;
        }
        if (paramX.exlValueType == ElementType.ETY_FLOAT) {
          if (paramY.exlValueType == ElementType.ETY_FLOAT) {  //浮動小数点数+浮動小数点数
            elem.exlType = ElementType.ETY_OPERATOR_ADDITION_FLOAT_FLOAT;
            elem.exlPriority = EPY_PRIORITY_ADDITION;
            elem.exlValueType = ElementType.ETY_FLOAT;
          } else {  //浮動小数点数+文字列
            elem.exlType = ElementType.ETY_OPERATOR_ADDITION_FLOAT_STRING;
            elem.exlPriority = EPY_PRIORITY_ADDITION;
            elem.exlValueType = ElementType.ETY_STRING;
          }
        } else {
          if (paramY.exlValueType == ElementType.ETY_FLOAT) {  //文字列+浮動小数点数
            elem.exlType = ElementType.ETY_OPERATOR_ADDITION_STRING_FLOAT;
            elem.exlPriority = EPY_PRIORITY_ADDITION;
            elem.exlValueType = ElementType.ETY_STRING;
          } else {  //文字列+文字列
            elem.exlType = ElementType.ETY_OPERATOR_ADDITION_STRING_STRING;
            elem.exlPriority = EPY_PRIORITY_ADDITION;
            elem.exlValueType = ElementType.ETY_STRING;
          }
        }
        break;
      case ETY_TOKEN_HYPHEN_MINUS:  // x-y
        tokenList.pollFirst ();
        paramY = evxParseMultiplication (tokenList);
        if (paramY == null) {
          return null;
        }
        if (paramX.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
          return null;
        }
        if (paramY.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
          return null;
        }
        elem.exlType = ElementType.ETY_OPERATOR_SUBTRACTION;
        elem.exlPriority = EPY_PRIORITY_ADDITION;
        elem.exlValueType = ElementType.ETY_FLOAT;
        break;
      default:
        return paramX;
      }
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseAddition

  //  シフト演算子
  protected ExpressionElement evxParseShift (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseAddition (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LEFT_SHIFT:  // x<<y
      case ETY_OPERATOR_RIGHT_SHIFT:  // x>>y
      case ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT:  // x>>>y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseAddition (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_SHIFT;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseShift

  //  比較演算子
  protected ExpressionElement evxParseComparison (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseShift (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LESS_THAN:  // x<y
      case ETY_OPERATOR_LESS_OR_EQUAL:  // x<=y
      case ETY_OPERATOR_GREATER_THAN:  // x>y
      case ETY_OPERATOR_GREATER_OR_EQUAL:  // x>=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseShift (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_COMPARISON;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxComparison

  //  等価演算子
  protected ExpressionElement evxParseEquality (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseComparison (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_EQUAL:  // x==y
      case ETY_OPERATOR_NOT_EQUAL:  // x!=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseComparison (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_EQUALITY;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseEquality

  //  ビットAND演算子
  protected ExpressionElement evxParseBitwiseAnd (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseEquality (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_AND:  // x&y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseEquality (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_BITWISE_AND;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseAnd

  //  ビットXOR演算子
  protected ExpressionElement evxParseBitwiseXor (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseBitwiseAnd (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_XOR:  // x^y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseAnd (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_BITWISE_XOR;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseXor

  //  ビットOR演算子
  protected ExpressionElement evxParseBitwiseOr (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseBitwiseXor (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_OR:  // x|y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseXor (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_BITWISE_OR;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseOr

  //  論理AND演算子
  protected ExpressionElement evxParseLogicalAnd (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseBitwiseOr (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LOGICAL_AND:  // x&&y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseOr (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_LOGICAL_AND;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseLogicalAnd

  //  論理OR演算子
  protected ExpressionElement evxParseLogicalOr (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseLogicalAnd (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LOGICAL_OR:  // x||y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseLogicalAnd (tokenList);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_LOGICAL_OR;
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseLogicalOr

  //  条件演算子
  protected ExpressionElement evxParseConditional (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseLogicalOr (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_CONDITIONAL:  // x?y:z
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseConditional (tokenList);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      ExpressionElement colon = tokenList.pollFirst ();
      if (colon == null) {  //?があるのに:がない
        evxPrintError (": がありません", elem.exlSource, -1, 1);
        return null;
      }
      if (colon.exlType != ElementType.ETY_TOKEN_COLON) {  //?があるのに:がない
        evxPrintError (": がありません", colon.exlSource, colon.exlOffset, 1);
        return null;
      }
      ExpressionElement paramZ = evxParseConditional (tokenList);  //右から結合するので自分を呼ぶ
      if (paramZ == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError ("引数の型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_CONDITIONAL;
      elem.exlValueType = paramY.exlValueType;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlParamZ = paramZ;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramZ.exlOffset + paramZ.exlLength - paramX.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseConditional

  //  代入演算子
  protected ExpressionElement evxParseAssignment (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseConditional (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_ASSIGNMENT:  // x=y
      case ETY_OPERATOR_SELF_POWER:  // x**=y
      case ETY_OPERATOR_SELF_MULTIPLICATION:  // x*=y
      case ETY_OPERATOR_SELF_DIVISION:  // x/=y
      case ETY_OPERATOR_SELF_MODULUS:  // x%=y
      case ETY_OPERATOR_SELF_ADDITION:  // x+=y
      case ETY_OPERATOR_SELF_SUBTRACTION:  // x-=y
      case ETY_OPERATOR_SELF_LEFT_SHIFT:  // x<<=y
      case ETY_OPERATOR_SELF_RIGHT_SHIFT:  // x>>=y
      case ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT:  // x>>>=y
      case ETY_OPERATOR_SELF_BITWISE_AND:  // x&=y
      case ETY_OPERATOR_SELF_BITWISE_XOR:  // x^=y
      case ETY_OPERATOR_SELF_BITWISE_OR:  // x|=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseAssignment (tokenList);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      if (!(paramX.exlType == ElementType.ETY_VARIABLE_FLOAT ||  // 数値変数
            paramX.exlType == ElementType.ETY_VARIABLE_STRING ||  // 文字列変数
            paramX.exlType == ElementType.ETY_REGISTER_RN ||  // d0
            paramX.exlType == ElementType.ETY_REGISTER_FPN ||  // fp0
            paramX.exlType == ElementType.ETY_REGISTER_CRN ||  // pc
            paramX.exlType == ElementType.ETY_OPERATOR_MEMORY ||  //[x]
            ((paramX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) &&
             (paramX.exlParamX.exlType == ElementType.ETY_REGISTER_RN ||  // d0.b,d0.w,d0.l,d0.s
              paramX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY)) ||  // [x].b,[x].w,[x].l,[x].s
            ((paramX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE ||
              paramX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) &&
             paramX.exlParamX.exlType == ElementType.ETY_OPERATOR_MEMORY))) {  // [x].q,[x].d,[x].x,[x].y,[x].p
        evxPrintError ("左辺が場所ではありません", paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (elem.exlType == ElementType.ETY_OPERATOR_ASSIGNMENT &&  //単純代入
          paramX.exlType == ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数
          paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_ASSIGN_STRING_TO_VARIABLE;  //文字列変数への文字列単純代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_SELF_ADDITION &&  //加算・連結複合代入
                 paramX.exlType == ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_CONCAT_STRING_TO_VARIABLE;  //文字列変数への文字列連結複合代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_ASSIGNMENT &&  //単純代入
                 paramX.exlType == ElementType.ETY_OPERATOR_MEMORY &&  //左辺がメモリ
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_ASSIGN_STRING_TO_MEMORY;  //メモリへの文字列単純代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_SELF_ADDITION &&  //加算・連結複合代入
                 paramX.exlType == ElementType.ETY_OPERATOR_MEMORY &&  //左辺がメモリ
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_CONCAT_STRING_TO_MEMORY;  //メモリへの文字列連結複合代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (paramX.exlType != ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数ではない
                 paramY.exlValueType == ElementType.ETY_FLOAT) {  //右辺が数値
        elem.exlValueType = ElementType.ETY_FLOAT;
      } else {
        evxPrintError ("型が違います", paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_ASSIGNMENT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseAssignment

  //  コンマ演算子
  protected ExpressionElement evxParseComma (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseAssignment (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_COMMA:  // x,y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseAssignment (tokenList);
      if (paramY == null) {
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_COMMA;
      elem.exlValueType = paramY.exlValueType;  //右辺の値の型
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseComma

  //  コマンド
  protected ExpressionElement evxParseCommand (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem == null) {
      return null;
    }
    int minParamCount = 0;
    int maxParamCount = 0;
    switch (elem.exlType) {
    case ETY_COMMAND_FILL:
      minParamCount = 2 + 1;
      maxParamCount = 2 + 65536;
      break;
    case ETY_COMMAND_PRINT:
      maxParamCount = 65536;
      break;
    case ETY_COMMAND_DUMP:
      maxParamCount = 2;
      break;
    case ETY_COMMAND_LIST:
      maxParamCount = 1;
      break;
    case ETY_COMMAND_HELP:
      maxParamCount = 65536;  //ヘルプの引数は何を書いてもよいが全部無視する
      break;
    case ETY_COMMAND_REGS:
    case ETY_COMMAND_RUN:
    case ETY_COMMAND_SKIP:
    case ETY_COMMAND_STOP:
    case ETY_COMMAND_TRACE:
      break;
    default:
      return evxParseComma (tokenList);
    }
    tokenList.pollFirst ();
    ExpressionElement semicolon = tokenList.peekFirst ();
    ExpressionElement paramX = null;
    int paramCount = 0;
    if (semicolon != null && semicolon.exlType != ElementType.ETY_SEPARATOR) {  //引数がある
      paramX = evxParseCommand (tokenList);  //右から結合するので自分を呼ぶ
      if (paramX == null) {
        return null;
      }
      elem.exlParamX = paramX;
      elem.exlLength = paramX.exlOffset + paramX.exlLength - elem.exlOffset;
      paramCount = paramX.exlLengthOfCommaList ();  //コンマリストの長さ
    }
    if (paramCount < minParamCount) {
      evxPrintError ("引数が足りません", elem.exlSource, elem.exlOffset, elem.exlLength);
      return null;
    } else if (maxParamCount < paramCount) {
      evxPrintError ("引数が多すぎます", elem.exlSource, elem.exlOffset, elem.exlLength);
      return null;
    }
    elem.exlPriority = EPY_PRIORITY_COMMAND;
    elem.exlValueType = ElementType.ETY_VOID;
    return elem;
  }  //evxParseCommand

  //  セパレータ
  protected ExpressionElement evxParseSeparator (LinkedList<ExpressionElement> tokenList) {
    ExpressionElement paramX = evxParseCommand (tokenList);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_SEPARATOR:  // x;y
        break;
      default:
        return paramX;
      }
      do {
        tokenList.pollFirst ();
        if (tokenList.size () == 0) {  //右辺がなければ左辺を返す
          return paramX;
        }
      } while (tokenList.peekFirst ().exlType == ElementType.ETY_SEPARATOR);  //x;;yはx;yと同じ
      ExpressionElement paramY = evxParseCommand (tokenList);
      if (paramY == null) {
        return null;
      }
      elem.exlPriority = EPY_PRIORITY_SEPARATOR;
      elem.exlValueType = paramY.exlValueType;  //右辺の値の型
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseSeparator

}  //class ExpressionEvaluator



