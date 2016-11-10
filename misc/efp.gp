\\========================================================================================
\\  efp.gp
\\  Copyright (C) 2003-2016 Makoto Kamada
\\
\\  This file is part of the XEiJ (X68000 Emulator in Java).
\\  You can use, modify and redistribute the XEiJ if the conditions are met.
\\  Read the XEiJ License for more details.
\\  http://stdkmd.com/xeij/
\\========================================================================================

\\----------------------------------------------------------------------------------------
\\  class EFPの定数データを作るためのPARI/GPスクリプト
\\    PARI/GP
\\      http://pari.math.u-bordeaux.fr/
\\  基本形
\\    echo read("efp.gp");efppub2("PI",Pi,0,"pi") | gp-2.7 -q
\\    public final EFP       PI = new EFP (P,  1,0xc90fdaa22168c234L,0xc4c6629L<<36);  //>       pi=3.1415926535897932384626433833
\\    public final EFP       PIA= new EFP (M,-92,0x8fe47c65dadfb63eL,0xeeb3067L<<36);  //>         -2.2702237215044402196569865277 e-28
\\  メモ
\\    Windowsのコマンドプロンプトでは^がエスケープ文字なので、累乗演算子を使うときはeval("2^n")のように文字列を経由する
\\      エスケープ文字なのだから2^^nと書けばよさそうに思えるが、エスケープ文字が展開される回数が環境によって異なることが問題になる
\\      xyzzyの*Shell*では2回展開されるので、2^^^^nと書かなければならない
\\----------------------------------------------------------------------------------------

\p 400  \\デフォルトの桁数では足りないので増やす。大きくしすぎると時間がかかり、スタックサイズも増やさなければならなくなる



\\----------------------------------------------------------------------------------------
\\  数学定数
\\----------------------------------------------------------------------------------------

pi=Pi;
napier=exp(1);

LN2=log(2);
LN10=log(10);
LOG10_2=LN2/LN10;
LOG2_E=1/LN2;
LOG10_E=1/LN10;

LOG_ZERO=1e-99999;  \\abs(x)<=LOG_ZEROのときx==0と見なしてlog(x)の計算を避ける



\\----------------------------------------------------------------------------------------
\\  汎用関数
\\----------------------------------------------------------------------------------------

\\y=log2(x)
\\  二進対数
\\  2^n-1は有理数で正確に計算されるがlog2(2^n-1)は浮動小数点数に変換されるので精度が落ちる
\\  nが大きすぎるとlog2(2^n-1)==log2(2^n)すなわちfloor(log2(2^n-1))==nになってしまう場合があることに注意する
log2(x)={
  my(y,n);
  y=log(x)/LN2;  \\このままだとfloor(log2(2^63))が62になってしまう
  n=floor(y+1/2);
  if(x==2^n,n,y)  \\x==2^nのとき誤差が残らないようにする
  }

\\y=log10(x)
\\  常用対数
\\  10^n-1は有理数で正確に計算されるがlog10(10^n-1)は浮動小数点数に変換されるので精度が落ちる
\\  nが大きすぎるとlog10(10^n-1)==log10(10^n)すなわちfloor(log10(10^n-1))==nになってしまう場合があることに注意する
log10(x)={
  my(y,n);
  y=log(x)/LN10;
  n=floor(y+1/2);
  if(x==10^n,n,y)  \\x==10^nのとき誤差が残らないようにする
  }

rint(x)={
  if(frac(x)==1/2,  \\frac(x)=x-floor(x)
     if(x>=0,
        floor(x)+(floor(x)%2),  \\x%y=x-floor(x/y)*y
        ceil(x)-(ceil(x)%2)),
     floor(x+1/2))
  }

trunc(x)={
  if(x>=0,floor(x),ceil(x))  \\truncate(x)
  }

intrm(x,rm)={
  if(rm==RN,rint(x),
     rm==RZ,trunc(x),
     rm==RM,floor(x),
     rm==RP,ceil(x),
     x)
  }

\\h=hex(s)
\\  文字列を16進数と見なして符号なし整数に変換する
\\  "_"を読み飛ばす
hex(s)={
  my(v,h,i,c);
  v=Vecsmall(s);
  h=0;
  for(i=1,#v,
      c=v[i];
      if(c!=95,  \\_
         h=(h<<4)+if((48<=c)&&(c<=57),c-48,  \\0-9
                     if((65<=c)&&(c<=70),c-65+10,  \\A-F
                        if((97<=c)&&(c<=102),c-97+10,  \\a-f
                           error())))));
  h
}

\\s=hexstr(x,n)
\\  整数をn桁の16進数の文字列に変換する
HEXCHR=["0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"];
hexstr(x,n)={
  my(m,s);
  x=trunc(x);
  if(x<0,x=-x;m="-",m="");
  if(n<1,n=1);
  s="";
  while((x!=0)||(n>0),
        s=concat(HEXCHR[bitand(x,15)+1],s);
        x>>=4;
        n--);
  concat(m,s)
  }

\\v=append(a,b,c,...)
\\  多数のベクトルを連結する
\\  文字列も連結できるがStr(a,b,c,...)と同じ
append(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z)={
  if(length(b)==0,a,concat(a,append(b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z)))
  }

\\s=formatg(x,n)
\\  数値を有効桁数を指定して文字列に変換する
formatg(x,n)={
  my(s,g,v);
  if(x>=0,
     s="",
     s="-";
     x=-x);  \\x=abs(x)
  if(n<1,n=1);
  if(x<=LOG_ZERO,  \\x==0のとき。log10(x)を計算できない
     g=0;
     v=vector(n,i,48),  \\0のときは0を並べる
     g=floor(log10(x));  \\x!=0のときの指数
     v=Vecsmall(Str(floor(10^(n-1-g)*x+0.5)));  \\先頭のn桁を整数で取り出して1桁ずつ分解する
     if(#v==n+1,g++));  \\丸めで1桁増えたとき指数部を増やす。vの要素がn+1個あるのでStrchr(v)ではなくStrchr(v[1..n])と書くこと
  if((-3<=g)&&(g<=-2),
     Str(s,"0.",Strchr(vector(-1-g,i,48)),Strchr(v[1..n])),  \\すべて小数部。小数点以下n-g-1桁。先頭に0.0または0.00を付ける。指数形式にしても.e-3で4文字増えることに変わりないので0.00までは指数形式にしない。有効桁数は0以外の数字から数えればよい
     g==-1,
     Str(s,"0.",Strchr(v[1..n])),  \\すべて小数部。小数点以下n桁。先頭に0.を付ける
     (0<=g)&&(g<=n-2),
     Str(s,Strchr(v[1..g+1]),".",Strchr(v[g+2..n])),  \\g+1桁の整数部と小数点とn-g-1桁の小数部
     g==n-1,
     Str(s,Strchr(v[1..n])),  \\すべて整数部
     \\(n<=g)&&(g<=n+3),
     \\Str(s,Strchr(v[1..n]),Strchr(vector(g+1-n,i,48))),  \\すべて整数部。g+1桁。末尾のg+1-n桁は0。0の数が指数部よりも少なければ指数形式と比較して文字数は多くならないが有効桁数がわからなくなるので不採用
     n==1,
     Str(s,Strchr(v[1]),if(g>0,"e+","e"),g),  \\1桁の整数部と指数部
     Str(s,Strchr(v[1]),".",Strchr(v[2..n]),if(g>0,"e+","e"),g))  \\1桁の整数部と小数点とn-1桁の小数部と指数部
  }



\\----------------------------------------------------------------------------------------

LEN=92;  \\65<=LEN<=95

QFP_GETA_SIZE=512;
QFP_GETA_BASE=-QFP_GETA_SIZE/2;

EXCOLS=9;  \\式のカラム数

\\[s,e,q]=efpseq(x,rm,len)
\\  浮動小数点数への分解
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  s   符号。-1=-,0=0,1=+
\\  e   指数部
\\  q   仮数部。0<=q<2^LEN
efpseq(x,rm,len)={
  my(a,e,q,r);
  a=abs(x);  \\絶対値
  e=0;  \\指数部
  q=0;  \\仮数部の先頭lenbit
  r=0;  \\仮数部の先頭lenbitに続くlenbit
  if(LOG_ZERO<a,  \\0でないとき。a==0だとlog2(a)が計算できない
    e=floor(log2(a));  \\指数部
    r=floor(a*2^(2*len-1-e));  \\仮数部の先頭2*lenbit
    q=floor(r/2^len);  \\仮数部の先頭lenbit
    r=r-q*2^len);  \\仮数部の先頭lenbitに続くlenbit
  if(((rm==RN)&&(r>=2^(len-1))&&((q%2)!=0||r>2^(len-1)))||  \\RNでgbm==1&&(lsb|rbm|sbm)!=0
     ((rm==RM)&&(x<0)&&(r!=0))||  \\RMでx<0&&(gbm|rbm|sbm)!=0
     ((rm==RP)&&(x>=0)&&(r!=0)),  \\RPでx>=0&&(gbm|rbm|sbm)!=0
     \\RZのときは常に切り捨てる
     q++);  \\切り上げる
  if(q==2^len,  \\溢れているとき
     q=2^(len-1);  \\右にシフトして
     e++);  \\指数部をインクリメントする
  if(q>0,  \\0でないとき
     [if(x<0,-1,1),e,q],  \\符号,指数部,仮数部
     [0,0,0])
  }

\\v=efpval(x,rm)
\\  実際に出力する値を求める
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  v   実際に出力する値
efpval(x,rm)={
  my(w,s,e,q);
  w=efpseq(x,rm,LEN);
  s=w[1];
  e=w[2];
  q=w[3];
  s*q/2^(LEN-1-e)  \\実際に出力する値
  }

\\v=efpnew(x,rm)
\\  new EFP(～)
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  v   実際に出力した値
efpnew(x,rm)={
  my(d,c,e,q,s,t,w);
  w=efpseq(x,rm,LEN);
  s=w[1];
  e=w[2];
  q=w[3];
  print1("new EFP (");  \\コンストラクタの開始
  if(q>0,  \\0でないとき
     d=floor(q/2^(LEN-64));  \\仮数部の上位64bit
     c=(q-d*2^(LEN-64))*2^(128-LEN);  \\仮数部の下位LEN-64bitを64bit左詰めにした値
     t=floor((128-LEN)/4);  \\下位の末尾を省く桁数
     printf(Str("%s,%3d,0x%016xL,0x%0",16-t,"xL<<%d"),if(s<0,"M","P"),e,d,floor(c/2^(4*t)),4*t));  \\符号,指数部,仮数部の上位,仮数部の下位
  print1(")");  \\コンストラクタの終了
  s*q/2^(LEN-1-e)  \\実際に出力した値
  }

\\efprem(x,ex)
\\  式=値
\\  x   値
\\  ex  式の文字列
efprem(x,ex)={
  my(a);
  if(ex=="+",  \\下位のとき
     printf(Str("%",EXCOLS,"s"),"");  \\式
     if(x<0,print1("-"),print1("+")),  \\符号
     \\上位のとき
     if(#ex>0,printf(Str("%",EXCOLS,"s="),ex));  \\式=
     if(x<0,print1("-")));  \\符号
  a=abs(x);
  if((a<=2^LEN)&&(floor(a)==a),  \\整数かどうか
     print1(a),  \\整数のときは小数部を出力しない
     printf(Str("%.",ceil(LEN*LOG10_2)+1,"g"),a));  \\整数でないときは真の値との比較を明確にするため1桁余分に出力する
  print()  \\改行
  }

\\v=efpmem(x,rm,ex)
\\  new EFP(～),  //<=>式=値
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値
efpmem(x,rm,ex)={
  my(v);
  print1("    ");  \\インデント
  v=efpnew(x,rm);  \\new EFP(～)
  print1(",  //",if(v==x,"=",if(v<x,"<",">")));  \\区切りのコンマ,注釈開始,真の値との比較
  efprem(x,ex);  \\式=値
  v  \\実際に出力した値
  }

\\v=efpmem2(x,rm,ex)
\\  new EFP(～),  //<=>式=値(上位)
\\  new EFP(～),  //<=>  +値(下位)
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値(上位)
efpmem2(x,rm,ex)={
  my(v);
  v=efpmem(x,rm,ex);
  efpmem(x-v,rm,"+");
  v  \\実際に出力した値(上位)
  }

\\efpmems([x,rm,ex,...])
\\  new EFP(～),  //<=>式=値
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efpmems(p)={
  my(i);
  forstep(i=1,#p,3,
          efpmem(p[i],p[i+1],p[i+2]))
  }

\\efpmem2s([x,rm,ex,...])
\\  new EFP(～),  //<=>式=値(上位)
\\  new EFP(～),  //<=>  +値(下位)
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efpmem2s(p)={
  my(i);
  forstep(i=1,#p,3,
          efpmem2(p[i],p[i+1],p[i+2]))
  }

\\v=efppub(id,x,rm,ex)
\\  public final ～ = new EFP(～);  //<=>式=値
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値
efppub(id,x,rm,ex)={
  my(v);
  printf("  public final EFP %8s%s= ",id,if(ex=="+","A"," "));  \\インデント,public定数宣言,識別子=
  v=efpnew(x,rm);  \\new EFP(～)
  print1(";  //",if(v==x,"=",if(v<x,"<",">")));  \\行末のセミコロン,注釈開始,真の値との比較
  efprem(x,ex);  \\式=値
  v  \\実際に出力した値
  }

\\v=efppub2(id,x,rm,ex)
\\  public final ～ = new EFP(～);  //<=>式=値(上位)
\\  public final ～ = new EFP(～);  //<=>  +値(下位)
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値
efppub2(id,x,rm,ex)={
  my(v);
  v=efppub(id,x,rm,ex);
  efppub(id,x-v,rm,"+");
  v  \\実際に出力した値(上位)
  }

\\efppubs([id,x,rm,ex,...])
\\  public final ～ = new EFP(～);  //<=>式=値
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efppubs(p)={
  my(i);
  forstep(i=1,#p,4,
          efppub(p[i],p[i+1],p[i+2],p[i+3]))
  }

\\efppub2s([id,x,rm,ex,...])
\\  public final ～ = new EFP(～);  //<=>式=値(上位)
\\  public final ～ = new EFP(～);  //<=>  +値(下位)
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efppub2s(p)={
  my(i);
  forstep(i=1,#p,4,
          efppub2(p[i],p[i+1],p[i+2],p[i+3]))
  }

\\v=efppri(id,x,rm,ex)
\\  private final ～ = new EFP(～);  //<=>式=値
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値
efppri(id,x,rm,ex)={
  my(v);
  printf("  private final EFP %8s%s= ",id,if(ex=="+","A"," "));  \\インデント,private定数宣言,識別子=
  v=efpnew(x,rm);  \\new EFP(～)
  print1(";  //",if(v==x,"=",if(v<x,"<",">")));  \\行末のセミコロン,注釈開始,真の値との比較
  efprem(x,ex);  \\式=値
  v  \\実際に出力した値
  }

\\v=efppri2(id,x,rm,ex)
\\  private final ～ = new EFP(～);  //<=>式=値(上位)
\\  private final ～ = new EFP(～);  //<=>  +値(下位)
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
\\  v   実際に出力した値
efppri2(id,x,rm,ex)={
  my(v);
  v=efppri(id,x,rm,ex);
  efppri(id,x-v,rm,"+");
  v  \\実際に出力した値(上位)
  }

\\efppris([id,x,rm,ex,...])
\\  private final ～ = new EFP(～);  //<=>式=値
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efppris(p)={
  my(i);
  forstep(i=1,#p,4,
          efppri(p[i],p[i+1],p[i+2],p[i+3]))
  }

\\efppri2s([id,x,rm,ex,...])
\\  private final ～ = new EFP(～);  //<=>式=値(上位)
\\  private final ～ = new EFP(～);  //<=>  +値(下位)
\\  id  識別子
\\  x   値
\\  rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\  ex  式の文字列
efppri2s(p)={
  my(i);
  forstep(i=1,#p,4,
          efppri2(p[i],p[i+1],p[i+2],p[i+3]))
  }

\\g=enunit(f,x,a,b)
\\  変数変換
\\  定義域[a,b]の関数f(x)を定義域[-1,1]の関数g(x)に写す
enunit(f,x,a,b)={
  f((b-a)/2*x+(a+b)/2)
}

\\q=deunit(p,x,a,b)
\\  変数変換
\\  定義域[-1,1]の多項式p(x)を定義域[a,b]の多項式q(x)に写す
deunit(p,x,a,b)={
  subst(p,x,2/(b-a)*x-(a+b)/(b-a))
}

\\p=chebyshev(f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開した多項式p(x)を作る
chebyshev(f,a,b,n)={
  my(k,t);
  deunit(sum(k=0,n,if(k==0,1,2)/Pi*intnum(t=0,Pi,cos(k*t)*enunit(f,cos(t),a,b))*polchebyshev(k)),x,a,b)
}

\\p=efpch(f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開した多項式p(x)を作り、EFPの精度で出力する
efpch(f,a,b,n)={
  my(p);
  p=chebyshev(f,a,b,n);
  sum(k=0,poldegree(p),
      c=polcoeff(p,k);
      if(abs(c)>1e-200,efpval(c)*x^k,0))
  }

\\dblchebyshev(f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開した多項式p(x)を作り、doubleの精度で出力する
dblchebyshev(f,a,b,n)={
  my(p,pp,k,c,w,s,e,q,l);
  p=chebyshev(f,a,b,n);
  print1("  //  ");
  l=-1;  \\最後に出力した次数
  pp=0;
  forstep(k=poldegree(p),0,-1,
          c=polcoeff(p,k);
          if(abs(c)>1e-200,
             w=efpseq(c,0,53);
             s=w[1];
             if(s,
                e=w[2];
                q=w[3];
                pp+=s*q*2^(e-52)*x^k;
                if(l>=0||s<0,print1(if(l<0,"-",s>0," + "," - ")));  \\2番目以降または1番目で負のとき符号を出力する
                l=k;
                printf("0x1.%013xp%+d",q-2^52,e);
                if(k>0,
                   print1(" * x");
                   if(k>1,
                      printf("^%d",k)
                      )
                   )
                )
             )
          );
  print();
  printf("  //  %.4fbit",closeness(f,pp,a,b,10000));
  print()
}

\\m=closeness(f,p,a,b,n)
\\  関数f(x)と多項式p(x)を定義域[a,b]をn等分したn+1箇所すべてで比較し、一致しているbit数の最小値を返す
\\  1e-200のところを1e-99999にするとatanの連分数展開のn=23の値が111.17…から63.00…に変わってしまう
\\  closeness(atan,(565460191391535*x^23+53341142793851055*x^21+1478642826578514597*x^19+18830144848679609445*x^17+132684287947990969190*x^15+569150603397110037030*x^13+1562895514439656905690*x^11+2805272998676350873242*x^9+3274637089068144148635*x^7+2394753672097538146395*x^5+996219568503343609425*x^3+179855261407133737425*x)/(15081948074193*x^24+4524584422257900*x^22+223966928901766050*x^20+4330027292100810300*x^18+43145629089147359775*x^16+253121023989664510680*x^14+939615922385875835100*x^12+2292249832633675114200*x^10+3724905978029722060575*x^8+3992709675796564823100*x^6+2710839727251351906210*x^4+1056171322305721521900*x^2+179855261407133737425),1-sqrt(2),sqrt(2)-1,10000)
closeness(f,p,a,b,n)={
  my(m,k,x,t,r);
  m=999;
  for(k=0,n,
      x=a+(b-a)*k/n;
      t=f(x);
      if(abs(t)>1e-200,
         r=abs((eval(p)-t)/t);
         if(r>1e-200,
            m=min(m,-log2(r))
            )
         )
      );
  m
}

\\q=efppol(p)
\\  多項式pの係数をLENbitに丸めた多項式qを返す
\\  丸めモードはRNに固定
\\  p   多項式
\\  q   係数をLENbitに丸めた多項式
efppol(p)={
  my(k,c);
  sum(k=0,poldegree(p),
      c=polcoeff(p,k);
      if(abs(c)>1e-200,  \\1e-200以下は切り捨てる
         efpval(c,0)*x^k,
         0))
  }

\\efpchebyshev(id,f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開して多項式を作り、係数の定数宣言と真の値と一致しているbit数の最小値を出力する
efpchebyshev(id,f,a,b,n)={
  my(p,q,k,c);
  p=chebyshev(f,a,b,n);
  q=sum(k=0,poldegree(p),
        c=polcoeff(p,k);
        if(abs(c)>1e-200,
           efppub(Str(id,k),c,0,Str("c",k))*x^k,
           0));
  print("  //  ",q);
  printf("  //  %.4fbit",closeness(f,q,a,b,10000));
  print();
  q
}

\\efpmemch(f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開して多項式を作り、配列要素として出力する
efpmemch(f,a,b,n)={
  my(p,q,k,c);
  p=chebyshev(f,a,b,n);
  q=sum(k=0,poldegree(p),
        c=polcoeff(p,k);
        if(abs(c)>1e-200,
           efpmem(c,0,Str("c",k))*x^k,
           0));
  print("    //  ",q);
  printf("    //  %.4fbit",closeness(f,q,a,b,10000));
  print();
  q
}

\\graph(f)
\\  関数fのグラフを表示する
graph(f)={
  my(reso=10000,m,r,c,i,x,y);
  m=matrix(41,81,r,c,
           if(r%20==1,
              if(c%10==1,"+","-"),
              c%40==1,
              if(r%5==1,"+","|"),
              " "
              )
           );
  for(i=-4*reso,4*reso,
      x=i/reso;
      iferr(y=f(x);
            if(imag(y)==0,
               r=21-round(y*5);
               if(1<=r&&r<=41,
                  c=41+round(x*10);
                  m[r,c]="*"
                  )
               ),
            ERR,
            0
            )
      );
  for(r=1,41,
      print1("    //    ");
      for(c=1,81,
          print1(m[r,c])
          );
      print()
      )
}

\\fmax(f,a,b)
\\  区間[a,b]における関数fの最大値(簡易計算)
fmaxsub(n,f,a,b)={
  my(v,m,l,t);
  v=vector(101,i,f((a*(101-i)+b*(i-1))/100));  \\100分割する
  m=vecmax(v);
  if(n==0,return(m));  \\これ以上分割しない
  l=vecmin(v);
  if(l==m,return(m));  \\101箇所すべて同じ値なのでこれ以上分割しない
  t=vector(100,i,v[i+1]-v[i]);
  t=if((vecmin(t)>=0)||(vecmax(t)<=0),m,(l+m*9)/10);  \\単調増加または単調減少のときは最大値を含む区間、それ以外は最大値から10%までの区間
  for(i=1,100,
      if((v[i]>=t)||(v[i+1]>=t),  \\該当する区間
         m=max(m,fmaxsub(n-1,f,(a*(101-i)+b*(i-1))/100,(a*(100-i)+b*i)/100))  \\さらに分割する
         )
      );
  m
  }
fmax(f,a,b)=fmaxsub(10,f,a,b);

\\closeness2(f,p,a,b)
\\  関数fの近似多項式p(x)が区間[a,b]で何bit一致しているか
closeness2sub(f,p,a,b,d)={
  my(v,m,l,t);
  v=vector(d+1,i,my(x1,y);x1=(a*(d+1-i)+b*(i-1))/d;y=f(x1);if(abs(y)==0,0,abs(subst(p,x,x1)-y)/abs(y)));  \\d分割する
  m=vecmax(v);  \\d+1箇所の中の最大値
  if(d==1,return(m));  \\これ以上分割しない
  l=vecmin(v);  \\d+1箇所の中の最小値
  if(l==m,return(m));  \\d+1箇所すべて同じ値なのでこれ以上分割しない
  t=vector(d,i,v[i+1]-v[i]);  \\d区間それぞれの増分
  t=if((vecmin(t)>0)||(vecmax(t)<0),  \\単調増加または単調減少のときは
       m,  \\最大値を含む区間
       \\vecsort(t)[d-d/10+1];  \\それ以外は大きい方から10%まで
       t=List(vector(d/10,i,l));
       for(i=1,d+1,
           for(j=1,d/10,
               if(v[i]>t[j],
                  listinsert(t,v[i],j);
                  listpop(t)
                  )
               )
           );
       t[d/10]
       );
  for(i=1,d,
      if((v[i]>=t)||(v[i+1]>=t),  \\該当する区間を
         m=max(m,closeness2sub(f,p,(a*(d+1-i)+b*(i-1))/d,(a*(d-i)+b*i)/d,d/10))  \\さらに分割する
         )
      );
  m
  }
closeness2(f,p,a,b)=-log2(closeness2sub(f,p,a,b,1000));

\\[x,y]=fmaxp(f,a,b)
\\x=fmaxx(f,a,b)
\\y=fmaxy(f,a,b)
\\  関数fの区間[a,b]における最大値を求める
\\  f  関数(t_CLOSURE)
\\  a  左端
\\  b  右端
fmaxp(f,a,b)={
  my(d=1000,df,l,m,u,v,x1,t1,x2,t2);
  df=deriv(f);  \\fの導関数
  l=a;
  m=f(l);
  u=b;
  v=f(u);
  if(m<v,l=u;m=v);  \\最大値の初期値は両端の値の大きい方
  x1=a;  \\区間の左端
  t1=df(x1);  \\区間の左端の微分係数
  for(i=1,d,
      x2=(a*(d-i)+b*i)/d;  \\区間の右端
      t2=df(x2);  \\区間の右端の微分係数
      if((t1>=0)&&(t2<=0),  \\左端が増加で右端が減少
         u=solve(x=x1,x2,df(x));  \\微分係数が0になる位置
         v=f(u);
         if(m<v,l=u;m=v)
         );
      x1=x2;
      t1=t2;
      );
  [l,m]
}
fmaxx(f,a,b)={
  fmaxp(f,a,b)[1];
}
fmaxy(f,a,b)={
  fmaxp(f,a,b)[2];
}

\\closeness3(f,p,a,b)
\\  関数f(x)の近似多項式p(x)が区間[a,b]で何bit一致しているか
closeness3(f,p,a,b)={
  my(d=1000,df,dp,l,m,u,v,x1,t1,x2,t2);
  df=deriv(f);  \\fの導関数
  dp=deriv(p);  \\pの導関数
  l=a;
  m=(subst(p,x,l)/f(l)-1)^2;
  u=b;
  v=(subst(p,x,u)/f(u)-1)^2;
  if(m<v,l=u;m=v);  \\最大値の初期値は両端の距離の大きい方
  x1=a;  \\区間の左端
  \\  d(f(x)+g(x))=df(x)+dg(x)
  \\  d(f(x)-g(x))=df(x)-dg(x)
  \\  d(f(x)*g(x))=df(x)*g(x)+f(x)*dg(x), d(f(x)^2)=2*df(x)*f(x)
  \\  d(f(x)/g(x))=(df(x)*g(x)-f(x)*dg(x))/g(x)^2
  \\  d((p(x)/f(x)-1)^2)=2*d(p(x)/f(x)-1)*(p(x)/f(x)-1)
  \\                    =2*d((p(x)/f(x))*(p(x)/f(x)-1)
  \\                    =2*(dp(x)*f(x)-p(x)*df(x))/f(x)^2*(p(x)/f(x)-1)
  \\                    =2*(dp(x)*f(x)-p(x)*df(x))*(p(x)-f(x))/f(x)^3
  t1=2*(subst(dp,x,x1)*f(x1)-subst(p,x,x1)*df(x1))*(subst(p,x,x1)-f(x1))/f(x1)^3;  \\区間の左端の微分係数
  for(i=1,d,
      x2=(a*(d-i)+b*i)/d;  \\区間の右端
      t2=2*(subst(dp,x,x2)*f(x2)-subst(p,x,x2)*df(x2))*(subst(p,x,x2)-f(x2))/f(x2)^3;  \\区間の右端の微分係数
      if((t1>=0)&&(t2<=0),  \\左端が増加で右端が減少
         u=solve(xx=x1,x2,2*(subst(dp,x,xx)*f(xx)-subst(p,x,xx)*df(xx))*(subst(p,x,xx)-f(xx))/f(xx)^3);  \\微分係数が0になる位置
         v=(subst(p,x,u)/f(u)-1)^2;
         if(m<v,l=u;m=v)
         );
      x1=x2;
      t1=t2;
      );
  -log2(m)/2
}

\\distance(f,p,a,b)
\\  関数f(x)と近似多項式p(x)が区間[a,b]で小数点以下何bitまで一致しているか
distance(f,p,a,b)={
  my(d=1000,df,dp,l,m,u,v,x1,t1,x2,t2);
  df=deriv(f);  \\fの導関数
  dp=deriv(p);  \\pの導関数
  l=a;
  m=(subst(p,x,l)-f(l))^2;
  u=b;
  v=(subst(p,x,u)-f(u))^2;
  if(m<v,l=u;m=v);  \\最大値の初期値は両端の距離の大きい方
  x1=a;  \\区間の左端
  \\  d(f(x)+g(x))=df(x)+dg(x)
  \\  d(f(x)-g(x))=df(x)-dg(x)
  \\  d(f(x)*g(x))=df(x)*g(x)+f(x)*dg(x), d(f(x)^2)=2*df(x)*f(x)
  \\  d(f(x)/g(x))=(df(x)*g(x)-f(x)*dg(x))/g(x)^2
  \\  d((p(x)-f(x))^2)=2*d(p(x)-f(x))*(p(x)-f(x))
  \\                  =2*(dp(x)-df(x))*(p(x)-f(x))
  t1=2*(subst(dp,x,x1)-df(x1))*(subst(p,x,x1)-f(x1));  \\区間の左端の微分係数
  for(i=1,d,
      x2=(a*(d-i)+b*i)/d;  \\区間の右端
      t2=2*(subst(dp,x,x2)-df(x2))*(subst(p,x,x2)-f(x2));  \\区間の右端の微分係数
      if((t1>=0)&&(t2<=0),  \\左端が増加で右端が減少
         u=solve(xx=x1,x2,2*(subst(dp,x,xx)-df(xx))*(subst(p,x,xx)-f(xx)));  \\微分係数が0になる位置
         v=(subst(p,x,u)-f(u))^2;
         if(m<v,l=u;m=v)
         );
      x1=x2;
      t1=t2;
      );
  -log2(m)/2
}



GFP_PRC=256;

gfpnew(x)={
  my(f,e);
  if(x==0,
     f="P | Z";
     e=0;
     x="null",
     if(x>0,
        f="P",
        f="M";
        x=-x);
     e=floor(log2(x));
     x=floor(x*2^(GFP_PRC-1-e)+0.5);
     if(x>=2^GFP_PRC,
        x=x/2;
        e=e+1);
     printf("new GFP (%s, %d, new BigInteger (\"%x\", 16))",f,e,x)
     )
  }

gfpmem(x)={
  print1("      ");
  gfpnew(x);
  print(",")
  }

gfppri(id,x)={
  printf("  private final GFP %s = ",id);
  gfpnew(x);
  print(";")
  }

gfppris(a)={
  my(i);
  forstep(i=1,#a,2,
          gfppri(a[i],a[i+1])
          )
  }



\\dblbits(x)
\\  xに最も近いdouble値を符号なし64bit整数で返す
dblbits(x)={
  my(ne=11,nf=52,me=(1<<(ne-1))-1,mf=(1<<nf)-1,s,e,f,g);
  if(x<0,s=-1;x=-x,s=1);  \\符号
  if(x<LOG_ZERO,x=0);
  if(x==0,e=-me;f=0,  \\0
     e=floor(log2(x));  \\指数部
     if(me<e,e=me+1;f=0,  \\オーバーフロー。±Inf
        e<-me-nf,e=-me;f=0,  \\アンダーフロー。±0
        g=2^max(-me-nf,e-nf-1);  \\guard bit。g<1の場合があるので1<<max(～)は不可
        f=floor(x/(2*g));  \\仮数部の先頭nf+1bit
        x-=f*(2*g);  \\仮数部の先頭nf+1bitを除いた残り
        if((g<x)||((g==x)&&(bitand(f,1)!=0)),  \\nearest even
           f++;  \\切り上げる
           if(e<=-me,if(mf<f,e++),  \\非正規化数が正規化数になった
              if(2*mf+1<f,f>>=1;e++)));  \\正規化数が溢れた
        e=max(-me,e)));
  (((if(s<0,1,0)<<ne)+(e+me))<<nf)+bitand(f,mf)
  }

\\bitsdbl(l)
\\  符号なし64bit整数をdouble値に変換する
bitsdbl(l)={
  my(ne=11,nf=52,me=(1<<(ne-1))-1,mf=(1<<nf)-1,s,e,f);
  s=if((l>>(ne+nf))==0,1,-1);  \\符号
  e=bitand(l>>nf,2*me+1)-me;  \\指数部
  f=bitand(l,mf);  \\小数部
  if(-me<e,f+=mf+1,  \\正規化数
     f!=0,e=1-me,  \\非正規化数
     e=0);  \\0
  s*2^(e-nf)*f
  }

\\bitsdblhex(l)
\\  符号なし64bit整数をdouble値と見なして16進数で出力する
bitsdblhex(l)={
  my(ne=11,nf=52,me=(1<<(ne-1))-1,mf=(1<<nf)-1,s,e,f,g,k);
  s=if((l>>(ne+nf))==0,1,-1);  \\符号
  e=bitand(l>>nf,2*me+1)-me;  \\指数部
  f=bitand(l,mf);  \\仮数部
  if(me<e,print(if(s<0,"Double.NEGATIVE_INFINITY","Double.POSITIVE_INFINITY")),  \\±Inf
     if(-me<e,f+=mf+1,  \\正規化数
        f!=0,e=1-me;while(f<=mf,f<<=1;e--),  \\非正規化数。正規化する
        e=0);  \\0
     g=bitand(f,mf);
     if(g!=0,
        k=13;
        while(bitand(g,15)==0,g>>=4;k--));
     if(f==0,
        print1("0.0"),
        printf(Str("%s0x%x.%0",k,"xp%d"),if(s<0,"-",""),f>>nf,g,e)));
  s*2^(e-nf)*f
  }

\\dblhex(x)
\\dblhexmem(x)
\\  xに最も近いdouble値を16進数で出力する
dblhex(x)={
  bitsdblhex(dblbits(x))
  }
dblhexmem(x)={
  my(v);
  print1("      ");
  v=bitsdblhex(dblbits(x));
  print(",");
  v
  }



\\dbldbl(x)
\\  xに最も近いDouble-Double値を求める
dbldbl(x)={
  my(v);
  v=dbl(x);
  v+dbl(x-v);
  }



\\qfpnew(x)
\\  xに最も近いQFP値を出力する
qfpnew(x)={
  my(sh=0,eh=0,mh=0,sl=0,el=0,ml=0,g,b,epp);
  if(abs(x)<=LOG_ZERO,x=0);  \\絶対値が小さすぎるときlogの計算に失敗するので0にする
  if(x!=0,
     \\全体が0以外
     \\上位53bit
     if(x<0,x=-x;sh=-1,sh=1);  \\上位の符号
     eh=floor(log2(x))-52;  \\上位の指数部-52。上位の右端の指数部
     b=2^eh;  \\LSB。右端のbit
     g=b/2;  \\guard bit。LSBの次のbit。丸める位置
     mh=floor(x/b);  \\上位53bit
     x-=mh*b;  \\上位53bitを除いた残り
     if((g<x)||((x==g)&&(bitand(mh,1)!=0)),  \\to nearest evenで丸める
        mh++;  \\切り上げる。誤差で2^52-1になっていたときはここで2^52になるのでbit数が足りなくなることはない
        x-=b;  \\残りが負になる
        if(mh==2^53,  \\2^53-1を切り上げて2^53になった
           mh=2^52;
           eh++));
     \\下位53bit
     if(abs(x)<=LOG_ZERO,x=0);  \\絶対値が小さすぎるときlogの計算に失敗するので0にする
     if(x!=0,
        \\下位が0以外
        if(x<0,x=-x;sl=-sh,sl=sh);  \\下位の符号。xが負になっていたときは上位と下位の符号が逆
        el=floor(log2(x))-52;  \\下位の指数部-52。下位の右端の指数部
        b=2^el;  \\LSB。右端のbit
        g=b/2;  \\guard bit。LSBの次のbit。丸める位置
        ml=floor(x/b);  \\下位53bit
        x-=ml*b;  \\下位53bitを除いた残り
        if((g<x)||((x==g)&&(bitand(ml,1)!=0)),  \\to nearest evenで丸める
           ml++;  \\切り上げる。誤差で2^52-1になっていたときはここで2^52になるのでbit数が足りなくなることはない
           x-=b;  \\残りが負になる
           if(ml==2^53,  \\2^53-1を切り上げて2^53になった
              ml=2^52;
              el++))));
  if(sh==0,
     print("new QFP ()"),  \\0
     epp=floor((eh+52-QFP_GETA_BASE)/QFP_GETA_SIZE)*QFP_GETA_SIZE;  \\指数部の下駄。上位の指数部を-QFP_GETA_SIZE/2..QFP_GETA_SIZE/2-1にする
     print1("new QFP (",if(sh<0,"QFP_M, ","QFP_P, "),epp,", ");
     dblhex(sh*2^(eh-epp)*mh);  \\上位53bit
     print1(", ");
     dblhex(sl*2^(el-epp)*ml));  \\下位53bit。上位と下位の指数部が極端に離れていると下位がアンダーフローして0になる可能性がある
     print1(")");
  sh*2^eh*mh+sl*2^el*ml  \\値
  }

qfpmem(x,ex)={
  my(v);
  print1("      ");
  v=qfpnew(x);  \\new QFP(～)
  print1(",  //",if(v==x,"=",if(v<x,"<",">")));  \\区切りのコンマ,注釈開始,真の値との比較
  if(0<#ex,print1(ex,"="));
  if(v==floor(v)&&v<10^32,
     print(v),  \\整数で32桁以内のときはそのまま出力する
     printf("%.32g\n",v));
  v
  }

qfppub(id,x,ex)={
  my(v);
  printf("  public final QFP %s = ",id);  \\インデント,public定数宣言,識別子=
  v=qfpnew(x);  \\new QFP(～)
  print1(";  //",if(v==x,"=",if(v<x,"<",">")));  \\行末のセミコロン,注釈開始,真の値との比較
  if(0<#ex,print1(ex,"="));
  if(v==floor(v)&&v<10^32,
     print(v),  \\整数で32桁以内のときはそのまま出力する
     printf("%.32g\n",v));
  v  \\実際に出力した値
  }

qfppri(id,x,ex)={
  my(v);
  printf("  private final QFP %s = ",id);  \\インデント,private定数宣言,識別子=
  v=qfpnew(x);  \\new QFP(～)
  print1(";  //",if(v==x,"=",if(v<x,"<",">")));  \\行末のセミコロン,注釈開始,真の値との比較
  if(0<#ex,print1(ex,"="));
  if(v==floor(v)&&v<10^32,
     print(v),  \\整数で32桁以内のときはそのまま出力する
     printf("%.32g\n",v));
  v  \\実際に出力した値
  }

\\qfpchebyshev(id,f,a,b,n)
\\  定義域[a,b]の関数f(x)をn次チェビシェフ展開して多項式を作り、係数の定数宣言と真の値と一致しているbit数の最小値を出力する
qfpchebyshev(id,f,a,b,n)={
  my(p,q,k,c);
  p=chebyshev(f,a,b,n);
  q=sum(k=0,poldegree(p),
        c=polcoeff(p,k);
        if(abs(c)>1e-200,
           qfppri(Str(id,k),c,Str("c",k))*x^k,
           0));
  print("  //  ",q);
  printf("  //  %.4fbit",closeness(f,q,a,b,10000));
  print();
  q
}



1;
