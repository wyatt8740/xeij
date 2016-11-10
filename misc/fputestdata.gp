\\========================================================================================
\\  fputestdata.gp
\\  Copyright (C) 2003-2016 Makoto Kamada
\\
\\  This file is part of the XEiJ (X68000 Emulator in Java).
\\  You can use, modify and redistribute the XEiJ if the conditions are met.
\\  Read the XEiJ License for more details.
\\  http://stdkmd.com/xeij/
\\========================================================================================

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

\\y=roundfman882(x,rm,ofuf)
\\y=roundfman060(x,rm,ofuf)
\\  xの仮数部を丸めモードrmで24bitに丸める
roundfman882(x,rm,ofuf)={
  my(n=24,s,a,e,t,m);
  if(type(x)=="t_POL",return(x));
  s=sign(x);  \\符号
  a=abs(x);  \\絶対値
  if(a<=LOG_ZERO,  \\0に近すぎる。0に近すぎるとlog2(a)の計算に失敗する
     return(s*Rei));
  e=floor(log2(a));  \\指数
  if(a<2^e,e--);  \\補正する
  t=a*2^(n-1-e);
  m=floor(t);  \\仮数部の先頭(n)bit
  t-=m;  \\仮数部の端数
  if(t!=0,  \\端数が0ではない
     fpsr=bitor(fpsr,X2);  \\不正確な結果
     if(((rm==RN)&&(((1/2)<t)||
                    ((t==(1/2))&&(bitand(m,1)==1))))||  \\RNで端数が1/2より大きいか端数が1/2と等しくて1の位が1
        ((rm==RM)&&(x<0))||  \\端数が0ではなくてRMで-または
        ((rm==RP)&&(0<x)),  \\端数が0ではなくてRPで+のとき
        m++;  \\繰り上げる
        if(m==2^n,  \\溢れた
           m=2^(n-1);
           e++)));
  if(ofuf,  \\オーバーフローとアンダーフローを処理するとき
     if(16383<e,  \\オーバーフローした
        fpsr=bitor(fpsr,OF);  \\オーバーフロー
        return(if(0<s,
                  if((rm==RZ)||(rm==RM),2^16384-2^16320,Inf),
                  if((rm==RZ)||(rm==RP),-2^16384+2^16320,-Inf))),
        e<-16383,  \\アンダーフローした
        fpsr=bitor(fpsr,UF);  \\アンダーフロー
        return(if(0<s,
                  if(rm==RP,2^-16406,Rei),
                  if(rm==RM,-2^-16406,-Rei)))
        )
     );
  s*m*2^(e+1-n)
  }
roundfman060(x,rm,ofuf)={
  my(n=24,s,a,e,t,m);
  if(type(x)=="t_POL",return(x));
  s=sign(x);  \\符号
  a=abs(x);  \\絶対値
  if(a<=LOG_ZERO,  \\0に近すぎる。0に近すぎるとlog2(a)の計算に失敗する
     return(s*Rei));
  e=floor(log2(a));  \\指数
  if(a<2^e,e--);  \\補正する
  t=a*2^(n-1-e);
  m=floor(t);  \\仮数部の先頭(n)bit
  t-=m;  \\仮数部の端数
  if(t!=0,  \\端数が0ではない
     fpsr=bitor(fpsr,X2);  \\不正確な結果
     if(((rm==RN)&&(((1/2)<t)||
                    ((t==(1/2))&&(bitand(m,1)==1))))||  \\RNで端数が1/2より大きいか端数が1/2と等しくて1の位が1
        ((rm==RM)&&(x<0))||  \\端数が0ではなくてRMで-または
        ((rm==RP)&&(0<x)),  \\端数が0ではなくてRPで+のとき
        m++;  \\繰り上げる
        if(m==2^n,  \\溢れた
           m=2^(n-1);
           e++)));
  if(ofuf,  \\オーバーフローとアンダーフローを処理するとき
     if(16383<e,  \\オーバーフローした
        fpsr=bitor(fpsr,OF);  \\オーバーフロー
        return(if(0<s,
                  if((rm==RZ)||(rm==RM),2^16384-2^16320,Inf),
                  if((rm==RZ)||(rm==RP),-2^16384+2^16320,-Inf))),
        e<-16383,  \\アンダーフローした
        fpsr=bitor(fpsr,UF+X2);  \\アンダーフロー、不正確な結果
        return(if(0<s,
                  if(rm==RP,2^-16406,Rei),
                  if(rm==RM,-2^-16406,-Rei)))
        )
     );
  s*m*2^(e+1-n)
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
\\  FPCR
\\----------------------------------------------------------------------------------------

EXD=0;  \\extended
SGL=1;  \\single
DBL=2;  \\double
DBL3=3;  \\double

RN=0;  \\to nearest
RZ=1;  \\toward zero
RM=2;  \\toward minus infinity
RP=3;  \\toward plus infinity

fpcr=(EXD<<6)+(RN<<4);



\\----------------------------------------------------------------------------------------
\\  FPSR
\\----------------------------------------------------------------------------------------

\\コンディションコードバイト
MI=1<<27;
ZE=1<<26;
IN=1<<25;
NA=1<<24;
\\エクセプションバイト
BS=1<<15;
SN=1<<14;
OE=1<<13;
OF=1<<12;
UF=1<<11;
DZ=1<<10;
X2=1<<9;
X1=1<<8;
\\アクルードエクセプションバイト
AV=1<<7;
AO=1<<6;
AU=1<<5;
AZ=1<<4;
AX=1<<3;

FPSR_MASK_1=[MI,ZE,IN,NA];
FPSR_NAME_1=["MI","ZE","IN","NA"];
FPSR_MASK_2=[BS,SN,OE,OF,UF,DZ,X2,X1,AV,AO,AU,AZ,AX];
FPSR_NAME_2=["BS","SN","OE","OF","UF","DZ","X2","X1","AV","AO","AU","AZ","AX"];

fpsr=0;

\\s=fpsrtostr(sr)
\\  fpsrを文字列に変換する
fpsrtostr(sr)={
  my(s,n);
  if(sr==0,"0",
     s="";
     for(i=1,#FPSR_MASK_1,
         if(bitand(sr,FPSR_MASK_1[i])!=0,
            if(s!="",s=concat(s,"+"));
            s=concat(s,FPSR_NAME_1[i])));
     if(bitand(sr>>23,1)!=0,
        if(s!="",s=concat(s,"+"));
        s=concat(s,Str("(",bitand(sr>>23,1),"<<23)")));
     if(bitand(sr>>16,127)!=0,
        if(s!="",s=concat(s,"+"));
        s=concat(s,Str("(",bitand(sr>>16,127),"<<16)")));
     for(i=1,#FPSR_MASK_2,
         if(bitand(sr,FPSR_MASK_2[i])!=0,
            if(s!="",s=concat(s,"+"));
            s=concat(s,FPSR_NAME_2[i]))));
  s
}



\\----------------------------------------------------------------------------------------
\\  特別な数値
\\----------------------------------------------------------------------------------------

\\    Rei   +0
\\    -Rei  -0
\\    Inf   +Inf
\\    -Inf  -Inf
\\    NaN   NaN
\\
\\  type(x)=="t_POL"とx==Reiなどで判別できる。Rei<xは不可
\\  単なる変数なので間違って値を代入しないように注意すること

isnegative(x)={
  if((x==Rei)||(x==Inf)||(x==NaN),0,
     (x==-Rei)||(x==-Inf)||(x<0),1,
     0)
}



\\----------------------------------------------------------------------------------------
\\  型変換
\\----------------------------------------------------------------------------------------

\\  perl -e "printf qq@%c%c    %9s%5s%5s%4s%4s%4s%5s%7s%8s%8s%8s%7s\n@,92,92,'name','nam','bit','sw','ew','iw','fw','bias','demin','demax','nomin','nomax';for my$i(['single','sgl',8,0,23],['double','dbl',11,0,52],['extended','exd',15,1,63],['triple','trp',15,1,79],['quadruple','qrp',15,0,112],['sextuple','sxt',15,0,176],['octuple','otp',15,0,240]){my($name,$nam,$ew,$iw,$fw)=@$i;my$bias=(1<<($ew-1))-1;my$demin=1-$iw-$bias-$fw;my$demax=-$iw-$bias;my$nomin=1-$iw-$bias;my$nomax=$bias;printf qq@%c%c    %9s%5s%5d%4d%4d%4d%5d%7d%8d%8d%8d%7d\n@,92,92,$name,$nam,1+$ew+$iw+$fw,1,$ew,$iw,$fw,$bias,$demin,$demax,$nomin,$nomax;}"
\\         name  nam  bit  sw  ew  iw   fw   bias   demin   demax   nomin  nomax
\\       single  sgl   32   1   8   0   23    127    -149    -127    -126    127
\\       double  dbl   64   1  11   0   52   1023   -1074   -1023   -1022   1023
\\     extended  exd   80   1  15   1   63  16383  -16446  -16384  -16383  16383
\\       triple  trp   96   1  15   1   79  16383  -16462  -16384  -16383  16383
\\    quadruple  qrp  128   1  15   0  112  16383  -16494  -16383  -16382  16383
\\     sextuple  sxt  192   1  15   0  176  16383  -16558  -16383  -16382  16383
\\      octuple  otp  256   1  15   0  240  16383  -16622  -16383  -16382  16383

TPR=10;  \\triple
QRP=11;  \\quadruple
SXT=12;  \\sextuple
OTP=13;  \\octuple

\\x=bitstosgl(u)  32bit符号なし整数をsingleの内部表現と見なして数値に変換する
\\x=bitstodbl(u)  64bit符号なし整数をdoubleの内部表現と見なして数値に変換する
\\x=bitstoexd(u)  80bit符号なし整数をextendedの内部表現と見なして数値に変換する
\\x=bitstotrp(u)  96bit符号なし整数をtripleの内部表現と見なして数値に変換する
\\x=bitstoqrp(u)  128bit符号なし整数をquadrupleの内部表現と見なして数値に変換する
\\x=bitstosxt(u)  192bit符号なし整数をsextupleの内部表現と見なして数値に変換する
\\x=bitstootp(u)  256bit符号なし整数をoctupleの内部表現と見なして数値に変換する
\\x=bitstoxxx(u,ew,iw,fw)
\\  符号なし整数を浮動小数点数の内部表現と見なして数値に変換する
\\     x  数値
\\     s  16進数の文字列
\\     u  (1+ew+iw+fw)bit符号なし整数
\\    ew  浮動小数点数の内部表現の指数部のbit数
\\    iw  浮動小数点数の内部表現の整数部のbit数
\\    fw  浮動小数点数の内部表現の小数部のbit数
bitstosgl(u)={ bitstoxxx(u,8,0,23) }
bitstodbl(u)={ bitstoxxx(u,11,0,52) }
bitstoexd(u)={ bitstoxxx(u,15,1,63) }
bitstotrp(u)={ bitstoxxx(u,15,1,79) }
bitstoqrp(u)={ bitstoxxx(u,15,0,112) }
bitstosxt(u)={ bitstoxxx(u,15,0,176) }
bitstootp(u)={ bitstoxxx(u,15,0,240) }
bitstoxxx(u,ew,iw,fw)={
  my(eb,sp,ep,ip,fp,sv);
  eb=(1<<(ew-1))-1;  \\指数のバイアス
  sp=u>>(ew+iw+fw);  \\符号部
  ep=bitand(u>>(iw+fw),(1<<ew)-1);  \\指数部
  ip=bitand(u>>fw,(1<<iw)-1);  \\整数部
  fp=bitand(u,(1<<fw)-1);  \\小数部
  sv=if(sp==0,1,-1);  \\符号
  if(ep==((1<<ew)-1),  \\指数部がすべて1
     if (fp==0,sv*Inf,  \\指数部がすべて1で小数部がすべて0ならば±Inf
         NaN),  \\指数部がすべて1で小数部が0でなければNaN
     iw==0,
     \\整数部がないとき(single,double)
     \\  指数部が0でなければ正規化数、指数部が0で小数部が0でなければ非正規化数、指数部が0で小数部も0ならば0
     \\  指数部が1の正規化数と指数部が0の非正規化数は小数点の位置が同じで同じ指数
     if(ep!=0,sv*2^(ep-eb-fw)*((1<<fw)+fp),  \\指数部が0でなければ正規化数
        fp!=0,sv*2^(1-eb-fw)*fp,  \\指数部が0で小数部が0でなければ非正規化数
        sv*Rei),  \\指数部が0で小数部も0ならば±0
     \\整数部があるとき(extended)
     \\  整数部が0でなければ正規化数、指数部と整数部が0で小数部が0でなければ非正規化数、指数部と整数部と小数部が0ならば±0、それ以外はNaN
     \\  指数部が0の正規化数と指数部が0の非正規化数は小数点の位置が同じで同じ指数
     if((ip!=0),sv*2^(ep-eb-fw)*((ip<<fw)+fp),  \\整数部が0でなければ正規化数
        (ep==0)&&(ip==0)&&(fp!=0),sv*2^(0-eb-fw)*fp,  \\指数部と整数部が0で小数部が0でなければ非正規化数
        (ep==0)&&(ip==0)&&(fp==0),sv*Rei,  \\指数部と整数部と小数部が0ならば±0
        NaN))  \\それ以外はNaN
  }

\\x=membitstoexd(u)  96bit符号なし整数をextendedのメモリ内部表現と見なして数値に変換する
\\x=membitstotrp(u)  96bit符号なし整数をtripleのメモリ内部表現と見なして数値に変換する
membitstoexd(u)={
  \\  |符号部と指数部(16bit)|空き(16bit)|仮数部(64bit)|
  \\                   ↓
  \\  |符号部と指数部(16bit)|仮数部(64bit)|
  bitstoexd((bitand(u,(1<<96)-(1<<80))>>16)+  \\符号部と指数部(16bit)
            bitand(u,(1<<64)-(1<<0)))  \\仮数部(64bit)
  }
membitstotrp(u)={
  \\  |符号部と指数部(16bit)|仮数部の下位(16bit)|仮数部の上位(64bit)|
  \\                                ↓
  \\  |符号部と指数部(16bit)|仮数部の上位(64bit)|仮数部の下位(16bit)|
  bitstotrp(bitand(u,(1<<96)-(1<<80))+  \\符号部と指数部(16bit)
            (bitand(u,(1<<64)-(1<<0))<<16)+  \\仮数部の上位(64bit)
            (bitand(u,(1<<80)-(1<<64))>>64))  \\仮数部の下位(16bit)
  }

\\x=hextosgl(s)  8桁の16進数の文字列をsingleの内部表現と見なして数値に変換する
\\x=hextodbl(s)  16桁の16進数の文字列をdoubleの内部表現と見なして数値に変換する
\\x=hextoexd(s)  20桁の16進数の文字列をextendedの内部表現と見なして数値に変換する
\\x=hextotrp(s)  24桁の16進数の文字列をtripleの内部表現と見なして数値に変換する
\\x=hextoqrp(s)  32桁の16進数の文字列をquadrupleの内部表現と見なして数値に変換する
\\x=hextosxt(s)  48桁の16進数の文字列をsextupleの内部表現と見なして数値に変換する
\\x=hextootp(s)  64桁の16進数の文字列をoctupleの内部表現と見なして数値に変換する
hextosgl(s)={ bitstosgl(hex(s)) }
hextodbl(s)={ bitstodbl(hex(s)) }
hextoexd(s)={ bitstoexd(hex(s)) }
hextotrp(s)={ bitstotrp(hex(s)) }
hextoqrp(s)={ bitstoqrp(hex(s)) }
hextosxt(s)={ bitstosxt(hex(s)) }
hextootp(s)={ bitstootp(hex(s)) }

\\x=memhextoexd(s)  24桁の16進数の文字列をextendedのメモリ内部表現と見なして数値に変換する
\\x=memhextotrp(s)  24桁の16進数の文字列をtripleのメモリ内部表現と見なして数値に変換する
memhextoexd(s)={ membitstoexd(hex(s)) }
memhextotrp(s)={ membitstotrp(hex(s)) }

\\u=sgltobits(x,rm,sg)  数値をsingleの内部表現の32bit符号なし整数に変換する
\\u=dbltobits(x,rm,sg)  数値をdoubleの内部表現の64bit符号なし整数に変換する
\\u=exdtobits(x,rm,sg)  数値をextendedの内部表現の80bit符号なし整数に変換する
\\u=trptobits(x,rm,sg)  数値をtripleの内部表現の96bit符号なし整数に変換する
\\u=qrptobits(x,rm,sg)  数値をquadrupleの内部表現の128bit符号なし整数に変換する
\\u=sxttobits(x,rm,sg)  数値をsextupleの内部表現の192bit符号なし整数に変換する
\\u=otptobits(x,rm,sg)  数値をoctupleの内部表現の256bit符号なし整数に変換する
\\u=sgltobitssr(x,rm,sg)  数値をsingleの内部表現の32bit符号なし整数に変換してfpsrを更新する
\\u=dbltobitssr(x,rm,sg)  数値をdoubleの内部表現の64bit符号なし整数に変換してfpsrを更新する
\\u=exdtobitssr(x,rm,sg)  数値をextendedの内部表現の80bit符号なし整数に変換してfpsrを更新する
\\u=trptobitssr(x,rm,sg)  数値をtripleの内部表現の96bit符号なし整数に変換してfpsrを更新する
\\u=qrptobitssr(x,rm,sg)  数値をquadrupleの内部表現の128bit符号なし整数に変換してfpsrを更新する
\\u=sxttobitssr(x,rm,sg)  数値をsextupleの内部表現の192bit符号なし整数に変換してfpsrを更新する
\\u=otptobitssr(x,rm,sg)  数値をoctupleの内部表現の256bit符号なし整数に変換してfpsrを更新する
\\u=xxxtobits(x,rm,sg,sf,ew,iw,fw)
\\  数値を浮動小数点数の内部表現の符号なし整数に変換する
\\    絶対値が小さすぎるときは±Rei、絶対値が大きすぎるときは±Infになる
\\    必要に応じてfpsrのオーバーフロー、アンダーフロー、不正確な結果がセットされる
\\     u  (1+ew+iw+fw)bit符号なし整数
\\     x  数値または±Reiまたは±InfまたはNaN
\\    rm  丸めモード。0=RN,1=RZ,2=RM,3=RP
\\    sg  符号の強制指定。-1=負,0=指定なし,1=正
\\    sf  fpsrの更新の有無。0=fpsrを更新しない,1=fpsrを更新する
\\    ew  浮動小数点数の内部表現の指数部のbit数
\\    iw  浮動小数点数の内部表現の整数部のbit数
\\    fw  浮動小数点数の内部表現の小数部のbit数
sgltobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,8,0,23) }
dbltobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,11,0,52) }
exdtobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,15,1,63) }
trptobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,15,1,79) }
qrptobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,15,0,112) }
sxttobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,15,0,176) }
otptobits(x,rm,sg)={ xxxtobits(x,rm,sg,0,15,0,240) }
sgltobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,8,0,23) }
dbltobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,11,0,52) }
exdtobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,15,1,63) }
trptobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,15,1,79) }
qrptobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,15,0,112) }
sxttobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,15,0,176) }
otptobitssr(x,rm,sg)={ xxxtobits(x,rm,sg,1,15,0,240) }
xxxtobits(x,rm,sg,sf,ew,iw,fw)={
  my(bias,demin,demax,nomin,nomax,z,a,e,o,t,m);
  bias=(1<<(ew-1))-1;  \\指数のバイアス
  demin=1-iw-bias-fw;  \\非正規化数の指数の下限
  demax=-iw-bias;  \\非正規化数の指数の上限
  nomin=1-iw-bias;  \\正規化数の指数の下限
  nomax=bias;  \\正規化数の指数の上限
  if(x==0,x=if(rm==RM,-Rei,Rei));
  if(x==NaN,return((1<<(ew+iw+fw))-1));  \\NaN
  if(sg==0,if(x==Rei,sg=1,x==-Rei,sg=-1,x==Inf,sg=1,x==-Inf,sg=-1,x>0,sg=1,x<0,sg=-1));  \\符号
  z=if(sg<0,1<<(ew+iw+fw),0);  \\±0
  a=abs(x);  \\絶対値
  if(a==Rei,return(z));  \\±0
  if(a==Inf,  \\±Inf
     return(z+(((1<<ew)-1)<<(iw+fw))-if(rm==if(sg<0,RP,RM),1,0)));
  if(a<=LOG_ZERO,  \\0に近すぎる。0に近すぎるとlog2(a)の計算に失敗する
     if(sf,fpsr=bitor(fpsr,UF+X2));  \\アンダーフロー、不正確な結果
     return(z));  \\±0
  e=floor(log2(a));  \\指数
  if(a<2^e,e--);  \\補正する
  if(e<demin-1,  \\指数部が小さすぎる。丸めで繰り上がる場合があるので一旦非正規化数の指数の下限-1まで受け入れる
     if(sf,fpsr=bitor(fpsr,UF+X2));  \\アンダーフロー、不正確な結果
     \\符号を跨がず±0から遠ざかる方向に丸めるときは±0ではなく絶対値が最小の非正規化数を返す
     return(z+if(((sg<0)&&(rm==RM))||((sg>0)&&(rm==RP)),1,0)));
  o=if(demax<e,1+fw,e-demax+fw);  \\1+小数部のbit数。正規化数のときo==1+fw、非正規化数のときo<1+fw
  t=a*2^(o-1-e);
  m=floor(t);  \\仮数部の先頭(o)bit
  t-=m;  \\仮数部の端数
  if(if(o==0,m!=0,(m<2^(o-1))||(2^o<=m)),
     print("a=",a);
     print("e=",e);
     print("o=",o);
     print("t=",t);
     print("m=",m);
     error());
  if(nomax<e,  \\指数部が大きすぎる
     if(sf,fpsr=bitor(fpsr,OF));  \\オーバーフロー
     if(sf&&(t!=0),fpsr=bitor(fpsr,X2));  \\不正確な結果
     \\±0に近付く方向に丸めるときは±Infではなく絶対値が最大の正規化数を返す
     return(z+(((1<<ew)-1)<<(iw+fw))-if(((sg<0)&&((rm==RZ)||(rm==RP)))||((sg>0)&&((rm==RZ)||(rm==RM))),1,0)));
  if(o<1+fw,  \\非正規化数
     if(sf,fpsr=bitor(fpsr,UF)));  \\アンダーフロー
  if(t!=0,  \\端数が0ではない
     if(sf,fpsr=bitor(fpsr,X2));  \\不正確な結果
     if(((rm==RN)&&(((1/2)<t)||
                    ((t==(1/2))&&(bitand(m,1)==1))))||  \\RNで端数が1/2より大きいか端数が1/2と等しくて1の位が1
        ((rm==RM)&&(x<0))||  \\端数が0ではなくてRMで-または
        ((rm==RP)&&(0<x)),  \\端数が0ではなくてRPで+のとき
        m++;  \\繰り上げる
        if(m==(1<<o),  \\1桁増えた
           if(o==1+fw,  \\正規化数が溢れた
              m>>=1;
              e++;  \\指数部をインクリメントする
              if(nomax<e,  \\指数部が溢れた
                 if(sf,fpsr=bitor(fpsr,OF));  \\オーバーフロー
                 \\±0に近付く方向に丸めるときは±Infではなく絶対値が最大の正規化数を返す
                 return(z+(((1<<ew)-1)<<(iw+fw))-if(((sg<0)&&((rm==RZ)||(rm==RP)))||((sg>0)&&((rm==RZ)||(rm==RM))),1,0))),
              m==(1<<(iw+fw)),  \\非正規化数が正規化数になった
              e+=1-iw))));
  if(m==0,  \\非正規化数が指数の下限-1から繰り上がらなかった
     \\符号を跨がず±0から遠ざかる方向に丸めるときは±0ではなく絶対値が最小の非正規化数を返す
     return(z+if(((sg<0)&&(rm==RM))||((sg>0)&&(rm==RP)),1,0)));
  z+(if((bias+e)>=0,bias+e,0)<<(iw+fw))+bitand(m,(1<<(iw+fw))-1)
  }

\\u=exdtomembits(x,rm,sg)  数値をextendedのメモリ内部表現の96bit符号なし整数に変換する
\\u=trptomembits(x,rm,sg)  数値をtripleのメモリ内部表現の96bit符号なし整数に変換する
exdtomembits(x,rm,sg)={
  my(u);
  u=exdtobits(x,rm,sg);
  \\  |符号部と指数部(16bit)|仮数部(64bit)|
  \\                   ↓
  \\  |符号部と指数部(16bit)|空き(16bit)|仮数部(64bit)|
  ((bitand(u,(1<<80)-(1<<64))<<16)+  \\符号部と指数部(16bit)
   bitand(u,(1<<64)-(1<<0)))  \\仮数部(64bit)
  }
trptomembits(x,rm,sg)={
  my(u);
  u=trptobits(x,rm,sg);
  \\  |符号部と指数部(16bit)|仮数部の上位(64bit)|仮数部の下位(16bit)|
  \\                                ↓
  \\  |符号部と指数部(16bit)|仮数部の下位(16bit)|仮数部の上位(64bit)|
  (bitand(u,(1<<96)-(1<<80))+  \\符号部と指数部(16bit)
   (bitand(u,(1<<16)-(1<<0))<<64)+  \\仮数部の下位(16bit)
   (bitand(u,(1<<80)-(1<<16))>>16))  \\仮数部の上位(64bit)
  }

\\s=sgltohex(x,rm,sg)  数値をsingleの内部表現を表す8桁の16進数の文字列に変換する
\\s=dbltohex(x,rm,sg)  数値をdoubleの内部表現を表す16桁の16進数の文字列に変換する
\\s=exdtohex(x,rm,sg)  数値をextendedの内部表現を表す20桁の16進数の文字列に変換する
\\s=trptohex(x,rm,sg)  数値をtripleの内部表現を表す24桁の16進数の文字列に変換する
\\s=qrptohex(x,rm,sg)  数値をquadrupleの内部表現を表す32桁の16進数の文字列に変換する
\\s=sxttohex(x,rm,sg)  数値をsextupleの内部表現を表す48桁の16進数の文字列に変換する
\\s=otptohex(x,rm,sg)  数値をoctupleの内部表現を表す64桁の16進数の文字列に変換する
sgltohex(x,rm,sg)={ hexstr(sgltobits(x,rm,sg),8) }
dbltohex(x,rm,sg)={ hexstr(dbltobits(x,rm,sg),16) }
exdtohex(x,rm,sg)={ hexstr(exdtobits(x,rm,sg),20) }
trptohex(x,rm,sg)={ hexstr(trptobits(x,rm,sg),24) }
qrptohex(x,rm,sg)={ hexstr(qrptobits(x,rm,sg),32) }
sxttohex(x,rm,sg)={ hexstr(sxttobits(x,rm,sg),48) }
otptohex(x,rm,sg)={ hexstr(otptobits(x,rm,sg),64) }

\\s=exdtomemhex(x,rm,sg)  数値をextendedのメモリ内部表現を表す24桁の16進数の文字列に変換する
\\s=trptomemhex(x,rm,sg)  数値をtripleのメモリ内部表現を表す24桁の16進数の文字列に変換する
exdtomemhex(x,rm,sg)={ hexstr(exdtomembits(x,rm,sg),24) }
trptomemhex(x,rm,sg)={ hexstr(trptomembits(x,rm,sg),24) }

\\sgl(x,rm)  数値をsingleで表現できる値に変換する
\\dbl(x,rm)  数値をdoubleで表現できる値に変換する
\\exd(x,rm)  数値をextendedで表現できる値に変換する
\\trp(x,rm)  数値をtripleで表現できる値に変換する
\\qrp(x,rm)  数値をquadrupleで表現できる値に変換する
\\sxt(x,rm)  数値をsextupleで表現できる値に変換する
\\otp(x,rm)  数値をoctupleで表現できる値に変換する
sgl(x,rm)={ bitstosgl(sgltobits(x,rm)) }
dbl(x,rm)={ bitstodbl(dbltobits(x,rm)) }
exd(x,rm)={ bitstoexd(exdtobits(x,rm)) }
trp(x,rm)={ bitstotrp(trptobits(x,rm)) }
qrp(x,rm)={ bitstoqrp(qrptobits(x,rm)) }
sxt(x,rm)={ bitstosxt(sxttobits(x,rm)) }
otp(x,rm)={ bitstootp(otptobits(x,rm)) }

\\s=sgltoimm(x,rm,sg)  数値をsingleの内部表現を表す1個の8桁の16進数の文字列に変換する
\\s=dbltoimm(x,rm,sg)  数値をdoubleの内部表現を表す2個の8桁の16進数の文字列に変換する
\\s=exdtoimm(x,rm,sg)  数値をextendedのメモリ内部表現を表す3個の8桁の16進数の文字列に変換する
\\s=trptoimm(x,rm,sg)  数値をtripleのメモリ内部表現を表す3個の8桁の16進数の文字列に変換する
\\s=qrptoimm(x,rm,sg)  数値をquadrupleの内部表現を表す4個の8桁の16進数の文字列に変換する
\\s=sxttoimm(x,rm,sg)  数値をsextupleの内部表現を表す6個の8桁の16進数の文字列に変換する
\\s=otptoimm(x,rm,sg)  数値をoctupleの内部表現を表す8個の8桁の16進数の文字列に変換する
sgltoimm(x,rm,sg)={
  Str("$",hexstr(sgltobits(x,rm,sg),8))
  }
dbltoimm(x,rm,sg)={
  my(u);
  u=dbltobits(x,rm,sg);
  Str("$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }
exdtoimm(x,rm,sg)={
  my(u);
  u=exdtomembits(x,rm,sg);
  Str("$",hexstr(u>>64,8),
      ",$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }
trptoimm(x,rm,sg)={
  my(u);
  u=trptomembits(x,rm,sg);
  Str("$",hexstr(u>>64,8),
      ",$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }
qrptoimm(x,rm,sg)={
  my(u);
  u=qrptobits(x,rm,sg);
  Str("$",hexstr(u>>96,8),
      ",$",hexstr(bitand(u>>64,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }
sxttoimm(x,rm,sg)={
  my(u);
  u=sxttobits(x,rm,sg);
  Str("$",hexstr(u>>160,8),
      ",$",hexstr(bitand(u>>128,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>96,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>64,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }
otptoimm(x,rm,sg)={
  my(u);
  u=otptobits(x,rm,sg);
  Str("$",hexstr(u>>224,8),
      ",$",hexstr(bitand(u>>192,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>160,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>128,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>96,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>64,(1<<32)-1),8),
      ",$",hexstr(bitand(u>>32,(1<<32)-1),8),
      ",$",hexstr(bitand(u,(1<<32)-1),8))
  }

\\y=sglnextdown(x)  xよりも小さい最大のsingleで表現できる値を返す
\\y=dblnextdown(x)  xよりも小さい最大のdoubleで表現できる値を返す
\\y=exdnextdown(x)  xよりも小さい最大のextendedで表現できる値を返す
\\y=trpnextdown(x)  xよりも小さい最大のtripleで表現できる値を返す
\\y=qrpnextdown(x)  xよりも小さい最大のquadrupleで表現できる値を返す
\\y=sxtnextdown(x)  xよりも小さい最大のsextupleで表現できる値を返す
\\y=otpnextdown(x)  xよりも小さい最大のoctupleで表現できる値を返す
sglnextdown(x)={ xxxnextdown(x,8,0,23) }
dblnextdown(x)={ xxxnextdown(x,11,0,52) }
exdnextdown(x)={ xxxnextdown(x,15,1,63) }
trpnextdown(x)={ xxxnextdown(x,15,1,79) }
qrpnextdown(x)={ xxxnextdown(x,15,0,112) }
sxtnextdown(x)={ xxxnextdown(x,15,0,176) }
otpnextdown(x)={ xxxnextdown(x,15,0,240) }
xxxnextdown(x,ew,iw,fw)={
  my(bias,demin,demax,nomin,nomax,y);
  bias=(1<<(ew-1))-1;  \\指数のバイアス
  demin=1-iw-bias-fw;  \\非正規化数の指数の下限
  demax=-iw-bias;  \\非正規化数の指数の上限
  nomin=1-iw-bias;  \\正規化数の指数の下限
  nomax=bias;  \\正規化数の指数の上限
  if(x==NaN,NaN,
     x==-Inf,-Inf,  \\負の無限大で飽和する
     (x==0)||(x==-Rei)||(x==Rei),-2^demin,  \\負の非正規化数の最大値
     x==Inf,2^nomax,  \\正の正規化数の最大値
     y=bitstoxxx(xxxtobits(x,RM,0,0,ew,iw,fw),ew,iw,fw);
     if(y==x,y=bitstoxxx(xxxtobits(y-abs(y)*2^-(2+fw),RM,0,0,ew,iw,fw),ew,iw,fw));
     y)
  }

\\y=nextdown(x,rp)
nextdown(x,rp)={
  if(rp==SGL,sglnextdown(x),
     (rp==DBL)||(rp==DBL3),dblnextdown(x),
     rp==EXD,exdnextdown(x),
     rp==TRP,trpnextdown(x),
     rp==QRP,qrpnextdown(x),
     rp==SXT,sxtnextdown(x),
     rp==OTP,otpnextdown(x),
     x)
  }

\\y=sglnextup(x)  xよりも大きい最小のsingleで表現できる値を返す
\\y=dblnextup(x)  xよりも大きい最小のdoubleで表現できる値を返す
\\y=exdnextup(x)  xよりも大きい最小のextendedで表現できる値を返す
\\y=trpnextup(x)  xよりも大きい最小のtripleで表現できる値を返す
\\y=qrpnextup(x)  xよりも大きい最小のquadrupleで表現できる値を返す
\\y=sxtnextup(x)  xよりも大きい最小のsextupleで表現できる値を返す
\\y=otpnextup(x)  xよりも大きい最小のoctupleで表現できる値を返す
sglnextup(x)={ xxxnextup(x,8,0,23) }
dblnextup(x)={ xxxnextup(x,11,0,52) }
exdnextup(x)={ xxxnextup(x,15,1,63) }
trpnextup(x)={ xxxnextup(x,15,1,79) }
qrpnextup(x)={ xxxnextup(x,15,0,112) }
sxtnextup(x)={ xxxnextup(x,15,0,176) }
otpnextup(x)={ xxxnextup(x,15,0,240) }
xxxnextup(x,ew,iw,fw)={
  my(bias,demin,demax,nomin,nomax,y);
  bias=(1<<(ew-1))-1;  \\指数のバイアス
  demin=1-iw-bias-fw;  \\非正規化数の指数の下限
  demax=-iw-bias;  \\非正規化数の指数の上限
  nomin=1-iw-bias;  \\正規化数の指数の下限
  nomax=bias;  \\正規化数の指数の上限
  if(x==NaN,NaN,
     x==-Inf,-2^nomax,  \\負の正規化数の最小値
     (x==0)||(x==-Rei)||(x==Rei),2^demin,  \\正の非正規化数の最小値
     x==Inf,Inf,  \\正の無限大で飽和する
     y=bitstoxxx(xxxtobits(x,RP,0,0,ew,iw,fw),ew,iw,fw);
     if(y==x,y=bitstoxxx(xxxtobits(y+abs(y)*2^-(2+fw),RP,0,0,ew,iw,fw),ew,iw,fw));
     y)
  }

\\y=nextup(x,rp)
nextup(x,rp)={
  if(rp==SGL,sglnextup(x),
     (rp==DBL)||(rp==DBL3),dblnextup(x),
     rp==EXD,exdnextup(x),
     rp==TRP,trpnextup(x),
     rp==QRP,qrpnextup(x),
     rp==SXT,sxtnextup(x),
     rp==OTP,otpnextup(x),
     x)
  }



\\----------------------------------------------------------------------------------------
\\  FPUのテストプログラムで使うデータを作る
\\----------------------------------------------------------------------------------------

MC68882ROM={[
  bitstoexd(hex("4001fe00068200000000")),  \\1
  bitstoexd(hex("4001ffc0050380000000")),  \\2
  bitstoexd(hex("20007fffffff00000000")),  \\3
  bitstoexd(hex("0000ffffffffffffffff")),  \\4
  bitstoexd(hex("3c00fffffffffffff800")),  \\5
  bitstoexd(hex("3f80ffffff0000000000")),  \\6
  bitstoexd(hex("0001f65d8d9c00000000")),  \\7
  bitstoexd(hex("7fff401e000000000000")),  \\8
  bitstoexd(hex("43f3e000000000000000")),  \\9
  bitstoexd(hex("4072c000000000000000"))  \\10
  ]}

FMOVECR_DATA={[
  "pi",
  "MC68882ROM[1]",
  "MC68882ROM[2]",
  "MC68882ROM[3]",
  "MC68882ROM[4]",
  "MC68882ROM[5]",
  "MC68882ROM[6]",
  "MC68882ROM[7]",
  "MC68882ROM[8]",
  "MC68882ROM[9]",
  "MC68882ROM[10]",
  "log10(2)",
  "exp(1)",
  "log2(exp(1))",
  "log10(exp(1))",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "0",
  "log(2)",
  "log(10)",
  "1",
  "10",
  "10^2",
  "10^4",
  "10^8",
  "10^16",
  "10^32",
  "10^64",
  "10^128",
  "10^256",
  "10^512",
  "10^1024",
  "10^2048",
  "10^4096"
  ]}

bothsign(a)={
  my(b,x);
  b=[];
  for(i=1,#a,
      x=a[i];
      b=concat(b,[x]);
      if(type(x)=="t_STR",
         if(x!="NaN",b=concat(b,[Str("-(",x,")")])),
         if(x!=NaN,b=concat(b,[-x]))));
  b
  }

FOP_DATA_SPECIAL=[Rei, -Rei, Inf, -Inf, NaN];

FOP_DATA_ROUNDING={
  bothsign([
    \\  extended
    "2^-16446",  \\2^-16446 exddemin
    "2^-16383-2^-16446-2^-16447*2",
    "2^-16383-2^-16446-2^-16447",
    "2^-16383-2^-16446",  \\2^-16383-2^-16446 exddemax
    "2^-16383-2^-16446+2^-16447",
    "2^-16383-2^-16446+2^-16447*2",
    "2^16384-2^16320-2^16320*2",
    "2^16384-2^16320-2^16320",
    "2^16384-2^16320",  \\2^16384-2^16320 exdnomax
    "2^-16383-2^-16447*2",
    "2^-16383-2^-16447",
    "2^-16383",  \\2^-16383 exdnomin
    "2^-16383+2^-16446",
    "2^-16383+2^-16446*2",
    "2^63-1-2^-1", "2^63-1", "2^63-1+2^-1",  \\2^63-1
    "2^63-1/2-2^-1", "2^63-1/2", "2^63-1/2+2^-1",  \\2^63-1/2
    "2^63-2^-1", "2^63", "2^63+2^0",  \\2^63
    "2^63+1/2-2^0", "2^63+1/2", "2^63+1/2+2^0",  \\2^63+1/2
    "2^63+1-2^0", "2^63+1", "2^63+1+2^0",  \\2^63+1
    "2^64-1-2^0", "2^64-1", "2^64-1+2^0",  \\2^64-1
    "2^64-1/2-2^0", "2^64-1/2", "2^64-1/2+2^0",  \\2^64-1/2
    "2^64-2^0", "2^64", "2^64+2^1",  \\2^64
    "2^64+1/2-2^1", "2^64+1/2", "2^64+1/2+2^1",  \\2^64+1/2
    "2^64+1-2^1", "2^64+1", "2^64+1+2^1",  \\2^64+1
    \\  single
    "2^-149-2^-213*2",
    "2^-149-2^-213",
    "2^-149",  \\2^-149 sgldemin
    "2^-149+2^-212",
    "2^-149+2^-212*2",
    "2^-126-2^-149-2^-190*2",
    "2^-126-2^-149-2^-190",
    "2^-126-2^-149",  \\2^-126-2^-149 sgldemax
    "2^-126-2^-149+2^-190",
    "2^-126-2^-149+2^-190*2",
    "2^-126-2^-190*2",
    "2^-126-2^-190",
    "2^-126",  \\2^-126 sglnomin
    "2^-126+2^-189",
    "2^-126+2^-189*2",
    "2^128-2^104-2^64*2",
    "2^128-2^104-2^64",
    "2^128-2^104",  \\2^128-2^104 sglnomax
    "2^128-2^104+2^64",
    "2^128-2^104+2^64*2",
    "2^23-1-2^-41", "2^23-1", "2^23-1+2^-41",  \\2^23-1
    "2^23-1/2-2^-41", "2^23-1/2", "2^23-1/2+2^-41",  \\2^23-1/2
    "2^23-2^-41", "2^23", "2^23+2^-40",  \\2^23
    "2^23+1/2-2^-40", "2^23+1/2", "2^23+1/2+2^-40",  \\2^23+1/2
    "2^23+1-2^-40", "2^23+1", "2^23+1+2^-40",  \\2^23+1
    "2^24-1-2^-40", "2^24-1", "2^24-1+2^-40",  \\2^24-1
    "2^24-1/2-2^-40", "2^24-1/2", "2^24-1/2+2^-40",  \\2^24-1/2
    "2^24-2^-40", "2^24", "2^24+2^-39",  \\2^24
    "2^24+1/2-2^-39", "2^24+1/2", "2^24+1/2+2^-39",  \\2^24+1/2
    "2^24+1-2^-39", "2^24+1", "2^24+1+2^-39",  \\2^24+1
    \\  double
    "2^-1074-2^-1138*2",
    "2^-1074-2^-1138",
    "2^-1074",  \\2^-1074 dbldemin
    "2^-1074+2^-1137",
    "2^-1074+2^-1137*2",
    "2^-1022-2^-1074-2^-1086*2",
    "2^-1022-2^-1074-2^-1086",
    "2^-1022-2^-1074",  \\2^-1022-2^-1074 dbldemax
    "2^-1022-2^-1074+2^-1086",
    "2^-1022-2^-1074+2^-1086*2",
    "2^-1022-2^-1086*2",
    "2^-1022-2^-1086",
    "2^-1022",  \\2^-1022 dblnomin
    "2^-1022+2^-1085",
    "2^-1022+2^-1085*2",
    "2^1024-2^971-2^960*2",
    "2^1024-2^971-2^960",
    "2^1024-2^971",  \\2^1024-2^971 dblnomax
    "2^1024-2^971+2^960",
    "2^1024-2^971+2^960*2",
    "2^52-1-2^-12", "2^52-1", "2^52-1+2^-12",  \\2^52-1
    "2^52-1/2-2^-12", "2^52-1/2", "2^52-1/2+2^-12",  \\2^52-1/2
    "2^52-2^-12", "2^52", "2^52+2^-11",  \\2^52
    "2^52+1/2-2^-11", "2^52+1/2", "2^52+1/2+2^-11",  \\2^52+1/2
    "2^52+1-2^-11", "2^52+1", "2^52+1+2^-11",  \\2^52+1
    "2^53-1-2^-11", "2^53-1", "2^53-1+2^-11",  \\2^53-1
    "2^53-1/2-2^-11", "2^53-1/2", "2^53-1/2+2^-11",  \\2^53-1/2
    "2^53-2^-11", "2^53", "2^53+2^-10",  \\2^53
    "2^53+1/2-2^-10", "2^53+1/2", "2^53+1/2+2^-10",  \\2^53+1/2
    "2^53+1-2^-10", "2^53+1", "2^53+1+2^-10"  \\2^53+1
    ])
  }  \\FOP_DATA_ROUNDING

FOP_DATA_MINUSBIG={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<1050*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{-$a->[1]*log($a->[0])<=>-$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22-$m^$n\x22,\n@}"
    "-2^1029",
    "-3^388",
    "-2^588",
    "-3^222",
    "-2^336",
    "-3^127",
    "-2^192",
    "-3^73",
    "-2^110",
    "-3^42",
    "-2^63",
    "-3^24",
    "-2^36",
    "-3^14",
    "-2^21",
    "-3^8",
    "-2^12",
    "-3^5",
    "-2^7",
    "-3^3",
    "-2^4"
    ]
  }  \\FOP_DATA_MINUSBIG

FOP_DATA_MINUSTWO={
  [
    -2
    ]
  }  \\FOP_DATA_MINUSTWO

FOP_DATA_MINUSTWOTOMINUSONE={
  [
    \\  perl -e "sub floor{my($x)=@_;my$y=int$x;$y<=$x?$y:$y-1}sub log2{my($x)=@_;my$y=log($x)/log(2);my$n=floor($y+0.5);2**$n==$x?$n:$y}sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$d(2,3,4,5,8){for my$n(-$d*2+1..-$d-1){gcd($n,$d)==1 and push@a,[$n,$d]}}for my$s(map{my($n,$d)=@$_;my$x=abs($n/$d);my$q=floor(log2($x));$p=2**$q==$x?$q-1:$q;$p-=63;$q-=63;$d==1?qq@\x22$n-2^$p\x22, $n, \x22$n+2^$q\x22,@:$d==2||$d==4||$d==8?qq@\x22$n/$d-2^$p\x22, $n/$d, \x22$n/$d+2^$q\x22,@:qq@\x22exd($n/$d,RM)\x22, \x22exd($n/$d,RP)\x22,@}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    $s\n@}"
    "-15/8-2^-63", -15/8, "-15/8+2^-63",
    "exd(-9/5,RM)", "exd(-9/5,RP)",
    "-7/4-2^-63", -7/4, "-7/4+2^-63",
    "exd(-5/3,RM)", "exd(-5/3,RP)",
    "-13/8-2^-63", -13/8, "-13/8+2^-63",
    "exd(-8/5,RM)", "exd(-8/5,RP)",
    "-3/2-2^-63", -3/2, "-3/2+2^-63",
    "exd(-7/5,RM)", "exd(-7/5,RP)",
    "-11/8-2^-63", -11/8, "-11/8+2^-63",
    "exd(-4/3,RM)", "exd(-4/3,RP)",
    "-5/4-2^-63", -5/4, "-5/4+2^-63",
    "exd(-6/5,RM)", "exd(-6/5,RP)",
    "-9/8-2^-63", -9/8, "-9/8+2^-63"
    ]
  }  \\FOP_DATA_MINUSTWOTOMINUSONE

FOP_DATA MINUSONEMINUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<64*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{$a->[1]*log($a->[0])<=>$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22-1-$m^-$n\x22,\n@}"
    "-1-2^-4",
    "-1-3^-3",
    "-1-2^-7",
    "-1-3^-5",
    "-1-2^-12",
    "-1-3^-8",
    "-1-2^-21",
    "-1-3^-14",
    "-1-2^-36",
    "-1-3^-24",
    "-1-2^-63"
    ]
  }  \\FOP_DATA_MINUSONEMINUSEPS

FOP_DATA_MINUSONE={
  [
    -1
    ]
  }  \\FOP_DATA_MINUSONE

FOP_DATA_MINUSONEPLUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<64*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{-$a->[1]*log($a->[0])<=>-$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22-1+$m^-$n\x22,\n@}"
    "-1+2^-63",
    "-1+3^-24",
    "-1+2^-36",
    "-1+3^-14",
    "-1+2^-21",
    "-1+3^-8",
    "-1+2^-12",
    "-1+3^-5",
    "-1+2^-7",
    "-1+3^-3",
    "-1+2^-4"
    ]
  }  \\FOP_DATA_MINUSONEPLUSEPS

FOP_DATA_MINUSONETOZERO={
  [
    \\  perl -e "sub floor{my($x)=@_;my$y=int$x;$y<=$x?$y:$y-1}sub log2{my($x)=@_;my$y=log($x)/log(2);my$n=floor($y+0.5);2**$n==$x?$n:$y}sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$d(2,3,4,5,8){for my$n(-$d+1..-1){gcd($n,$d)==1 and push@a,[$n,$d]}}for my$s(map{my($n,$d)=@$_;my$x=abs($n/$d);my$q=floor(log2($x));$p=2**$q==$x?$q-1:$q;$p-=63;$q-=63;$d==1?qq@\x22$n-2^$p\x22, $n, \x22$n+2^$q\x22,@:$d==2||$d==4||$d==8?qq@\x22$n/$d-2^$p\x22, $n/$d, \x22$n/$d+2^$q\x22,@:qq@\x22exd($n/$d,RM)\x22, \x22exd($n/$d,RP)\x22,@}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    $s\n@}"
    "-7/8-2^-64", -7/8, "-7/8+2^-64",
    "exd(-4/5,RM)", "exd(-4/5,RP)",
    "-3/4-2^-64", -3/4, "-3/4+2^-64",
    "exd(-2/3,RM)", "exd(-2/3,RP)",
    "-5/8-2^-64", -5/8, "-5/8+2^-64",
    "exd(-3/5,RM)", "exd(-3/5,RP)",
    "-1/2-2^-65", -1/2, "-1/2+2^-64",
    "exd(-2/5,RM)", "exd(-2/5,RP)",
    "-3/8-2^-65", -3/8, "-3/8+2^-65",
    "exd(-1/3,RM)", "exd(-1/3,RP)",
    "-1/4-2^-66", -1/4, "-1/4+2^-65",
    "exd(-1/5,RM)", "exd(-1/5,RP)",
    "-1/8-2^-67", -1/8, "-1/8+2^-66"
    ]
  }  \\FOP_DATA_MINUSONETOZERO

FOP_DATA_ZEROMINUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<1050*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{$a->[1]*log($a->[0])<=>$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22-$m^-$n\x22,\n@}"
    "-2^-4",
    "-3^-3",
    "-2^-7",
    "-3^-5",
    "-2^-12",
    "-3^-8",
    "-2^-21",
    "-3^-14",
    "-2^-36",
    "-3^-24",
    "-2^-63",
    "-3^-42",
    "-2^-110",
    "-3^-73",
    "-2^-192",
    "-3^-127",
    "-2^-336",
    "-3^-222",
    "-2^-588",
    "-3^-388",
    "-2^-1029"
    ]
  }  \\ZEROMINUSEPS

FOP_DATA_ZEROPLUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<1050*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{-$a->[1]*log($a->[0])<=>-$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22$m^-$n\x22,\n@}"
    "2^-1029",
    "3^-388",
    "2^-588",
    "3^-222",
    "2^-336",
    "3^-127",
    "2^-192",
    "3^-73",
    "2^-110",
    "3^-42",
    "2^-63",
    "3^-24",
    "2^-36",
    "3^-14",
    "2^-21",
    "3^-8",
    "2^-12",
    "3^-5",
    "2^-7",
    "3^-3",
    "2^-4"
    ]
  }  \\FOP_DATA_ZEROPLUSEPS

FOP_DATA_ZEROTOPLUSONE={
  [
    \\  perl -e "sub floor{my($x)=@_;my$y=int$x;$y<=$x?$y:$y-1}sub log2{my($x)=@_;my$y=log($x)/log(2);my$n=floor($y+0.5);2**$n==$x?$n:$y}sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$d(2,3,4,5,8){for my$n(1..$d-1){gcd($n,$d)==1 and push@a,[$n,$d]}}for my$s(map{my($n,$d)=@$_;my$x=abs($n/$d);my$q=floor(log2($x));$p=2**$q==$x?$q-1:$q;$p-=63;$q-=63;$d==1?qq@\x22$n-2^$p\x22, $n, \x22$n+2^$q\x22,@:$d==2||$d==4||$d==8?qq@\x22$n/$d-2^$p\x22, $n/$d, \x22$n/$d+2^$q\x22,@:qq@\x22exd($n/$d,RM)\x22, \x22exd($n/$d,RP)\x22,@}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    $s\n@}"
    "1/8-2^-67", 1/8, "1/8+2^-66",
    "exd(1/5,RM)", "exd(1/5,RP)",
    "1/4-2^-66", 1/4, "1/4+2^-65",
    "exd(1/3,RM)", "exd(1/3,RP)",
    "3/8-2^-65", 3/8, "3/8+2^-65",
    "exd(2/5,RM)", "exd(2/5,RP)",
    "1/2-2^-65", 1/2, "1/2+2^-64",
    "exd(3/5,RM)", "exd(3/5,RP)",
    "5/8-2^-64", 5/8, "5/8+2^-64",
    "exd(2/3,RM)", "exd(2/3,RP)",
    "3/4-2^-64", 3/4, "3/4+2^-64",
    "exd(4/5,RM)", "exd(4/5,RP)",
    "7/8-2^-64", 7/8, "7/8+2^-64"
    ]
  }  \\FOP_DATA_ZEROTOPLUSONE

FOP_DATA_PLUSONEMINUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<64*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{$a->[1]*log($a->[0])<=>$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x221-$m^-$n\x22,\n@}"
    "1-2^-4",
    "1-3^-3",
    "1-2^-7",
    "1-3^-5",
    "1-2^-12",
    "1-3^-8",
    "1-2^-21",
    "1-3^-14",
    "1-2^-36",
    "1-3^-24",
    "1-2^-63"
    ]
  }  \\FOP_DATA_PLUSONEMINUSEPS

FOP_DATA_PLUSONE={
  [
    1
    ]
  }  \\FOP_DATA_PLUSONE

FOP_DATA_PLUSONEPLUSEPS={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<64*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{-$a->[1]*log($a->[0])<=>-$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x221+$m^-$n\x22,\n@}"
    "1+2^-63",
    "1+3^-24",
    "1+2^-36",
    "1+3^-14",
    "1+2^-21",
    "1+3^-8",
    "1+2^-12",
    "1+3^-5",
    "1+2^-7",
    "1+3^-3",
    "1+2^-4"
    ]
  }  \\FOP_DATA_PLUSONEPLUSEPS

FOP_DATA_PLUSONETOPLUSTWO={
  [
    \\  perl -e "sub floor{my($x)=@_;my$y=int$x;$y<=$x?$y:$y-1}sub log2{my($x)=@_;my$y=log($x)/log(2);my$n=floor($y+0.5);2**$n==$x?$n:$y}sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$d(2,3,4,5,8){for my$n($d+1..2*$d-1){gcd($n,$d)==1 and push@a,[$n,$d]}}for my$s(map{my($n,$d)=@$_;my$x=abs($n/$d);my$q=floor(log2($x));$p=2**$q==$x?$q-1:$q;$p-=63;$q-=63;$d==1?qq@\x22$n-2^$p\x22, $n, \x22$n+2^$q\x22,@:$d==2||$d==4||$d==8?qq@\x22$n/$d-2^$p\x22, $n/$d, \x22$n/$d+2^$q\x22,@:qq@\x22exd($n/$d,RM)\x22, \x22exd($n/$d,RP)\x22,@}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    $s\n@}"
    "9/8-2^-63", 9/8, "9/8+2^-63",
    "exd(6/5,RM)", "exd(6/5,RP)",
    "5/4-2^-63", 5/4, "5/4+2^-63",
    "exd(4/3,RM)", "exd(4/3,RP)",
    "11/8-2^-63", 11/8, "11/8+2^-63",
    "exd(7/5,RM)", "exd(7/5,RP)",
    "3/2-2^-63", 3/2, "3/2+2^-63",
    "exd(8/5,RM)", "exd(8/5,RP)",
    "13/8-2^-63", 13/8, "13/8+2^-63",
    "exd(5/3,RM)", "exd(5/3,RP)",
    "7/4-2^-63", 7/4, "7/4+2^-63",
    "exd(9/5,RM)", "exd(9/5,RP)",
    "15/8-2^-63", 15/8, "15/8+2^-63"
    ]
  }  \\FOP_DATA_PLUSONETOPLUSTWO

FOP_DATA_PLUSTWO={
  [
    2
    ]
  }  \\FOP_DATA_PLUSTWO

FOP_DATA_PLUSBIG={
  [
    \\  perl -e "my@a=();for my$m(2,3){for(my$n=$m==2?4:3;$n*log($m)<1050*log(2);$n=int(7*$n/4)){push@a,[$m,$n]}}for my$t(sort{$a->[1]*log($a->[0])<=>$b->[1]*log($b->[0])}@a){my($m,$n)=@$t;print qq@    \x22$m^$n\x22,\n@}"
    "2^4",
    "3^3",
    "2^7",
    "3^5",
    "2^12",
    "3^8",
    "2^21",
    "3^14",
    "2^36",
    "3^24",
    "2^63",
    "3^42",
    "2^110",
    "3^73",
    "2^192",
    "3^127",
    "2^336",
    "3^222",
    "2^588",
    "3^388",
    "2^1029"
    ]
  }  \\FOP_DATA_PLUSBIG

FOP_DATA_EXP={
  [
    \\  log(n/8) (1<=n<=63,n!=8)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..7,9..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'log('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "log(1/8)",
    "log(1/4)",
    "log(3/8)",
    "log(1/2)",
    "log(5/8)",
    "log(3/4)",
    "log(7/8)",
    "log(9/8)",
    "log(5/4)",
    "log(11/8)",
    "log(3/2)",
    "log(13/8)",
    "log(7/4)",
    "log(15/8)",
    "log(2)",
    "log(17/8)",
    "log(9/4)",
    "log(19/8)",
    "log(5/2)",
    "log(21/8)",
    "log(11/4)",
    "log(23/8)",
    "log(3)",
    "log(25/8)",
    "log(13/4)",
    "log(27/8)",
    "log(7/2)",
    "log(29/8)",
    "log(15/4)",
    "log(31/8)",
    "log(4)",
    "log(33/8)",
    "log(17/4)",
    "log(35/8)",
    "log(9/2)",
    "log(37/8)",
    "log(19/4)",
    "log(39/8)",
    "log(5)",
    "log(41/8)",
    "log(21/4)",
    "log(43/8)",
    "log(11/2)",
    "log(45/8)",
    "log(23/4)",
    "log(47/8)",
    "log(6)",
    "log(49/8)",
    "log(25/4)",
    "log(51/8)",
    "log(13/2)",
    "log(53/8)",
    "log(27/4)",
    "log(55/8)",
    "log(7)",
    "log(57/8)",
    "log(29/4)",
    "log(59/8)",
    "log(15/2)",
    "log(61/8)",
    "log(31/4)",
    "log(63/8)",
    \\  log(2^(-2^n)) (10<=n<=13)
    "log(2^(-2^10))",
    "log(2^(-2^11))",
    "log(2^(-2^12))",
    "log(2^(-2^13))",
    \\  log(2^(2^n)) (10<=n<=13)
    "log(2^(2^10))",
    "log(2^(2^11))",
    "log(2^(2^12))",
    "log(2^(2^13))"
    ]
  }  \\FOP_DATA_EXP

FOP_DATA_EXPM1={
  [
    \\  log(n/8)-1 (1<=n<=63,n!=8)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..7,9..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'log('.$n.($d==1?'':'/'.$d).')-1'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "log(1/8)-1",
    "log(1/4)-1",
    "log(3/8)-1",
    "log(1/2)-1",
    "log(5/8)-1",
    "log(3/4)-1",
    "log(7/8)-1",
    "log(9/8)-1",
    "log(5/4)-1",
    "log(11/8)-1",
    "log(3/2)-1",
    "log(13/8)-1",
    "log(7/4)-1",
    "log(15/8)-1",
    "log(2)-1",
    "log(17/8)-1",
    "log(9/4)-1",
    "log(19/8)-1",
    "log(5/2)-1",
    "log(21/8)-1",
    "log(11/4)-1",
    "log(23/8)-1",
    "log(3)-1",
    "log(25/8)-1",
    "log(13/4)-1",
    "log(27/8)-1",
    "log(7/2)-1",
    "log(29/8)-1",
    "log(15/4)-1",
    "log(31/8)-1",
    "log(4)-1",
    "log(33/8)-1",
    "log(17/4)-1",
    "log(35/8)-1",
    "log(9/2)-1",
    "log(37/8)-1",
    "log(19/4)-1",
    "log(39/8)-1",
    "log(5)-1",
    "log(41/8)-1",
    "log(21/4)-1",
    "log(43/8)-1",
    "log(11/2)-1",
    "log(45/8)-1",
    "log(23/4)-1",
    "log(47/8)-1",
    "log(6)-1",
    "log(49/8)-1",
    "log(25/4)-1",
    "log(51/8)-1",
    "log(13/2)-1",
    "log(53/8)-1",
    "log(27/4)-1",
    "log(55/8)-1",
    "log(7)-1",
    "log(57/8)-1",
    "log(29/4)-1",
    "log(59/8)-1",
    "log(15/2)-1",
    "log(61/8)-1",
    "log(31/4)-1",
    "log(63/8)-1"
    ]
  }  \\FOP_DATA_EXPM1

FOP_DATA_EXP2={
  [
    \\  log2(n/8) (1<=n<=63,n!=8,n!=16,n!=32)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..7,9..15,17..31,33..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'log2('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "log2(1/8)",
    "log2(1/4)",
    "log2(3/8)",
    "log2(1/2)",
    "log2(5/8)",
    "log2(3/4)",
    "log2(7/8)",
    "log2(9/8)",
    "log2(5/4)",
    "log2(11/8)",
    "log2(3/2)",
    "log2(13/8)",
    "log2(7/4)",
    "log2(15/8)",
    "log2(17/8)",
    "log2(9/4)",
    "log2(19/8)",
    "log2(5/2)",
    "log2(21/8)",
    "log2(11/4)",
    "log2(23/8)",
    "log2(3)",
    "log2(25/8)",
    "log2(13/4)",
    "log2(27/8)",
    "log2(7/2)",
    "log2(29/8)",
    "log2(15/4)",
    "log2(31/8)",
    "log2(33/8)",
    "log2(17/4)",
    "log2(35/8)",
    "log2(9/2)",
    "log2(37/8)",
    "log2(19/4)",
    "log2(39/8)",
    "log2(5)",
    "log2(41/8)",
    "log2(21/4)",
    "log2(43/8)",
    "log2(11/2)",
    "log2(45/8)",
    "log2(23/4)",
    "log2(47/8)",
    "log2(6)",
    "log2(49/8)",
    "log2(25/4)",
    "log2(51/8)",
    "log2(13/2)",
    "log2(53/8)",
    "log2(27/4)",
    "log2(55/8)",
    "log2(7)",
    "log2(57/8)",
    "log2(29/4)",
    "log2(59/8)",
    "log2(15/2)",
    "log2(61/8)",
    "log2(31/4)",
    "log2(63/8)",
    \\  -2^n (10<=n<=13)
    -2^10,
    -2^11,
    -2^12,
    -2^13,
    \\  2^n (10<=n<=13)
    2^10,
    2^11,
    2^12,
    2^13
    ]
  }  \\FOP_DATA_EXP2

FOP_DATA_EXP10={
  [
    \\  log10(n/16) (1<=n<=255,n!=16)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..15,17..255){for my$d(16..16){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'log10('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "log10(1/16)",
    "log10(1/8)",
    "log10(3/16)",
    "log10(1/4)",
    "log10(5/16)",
    "log10(3/8)",
    "log10(7/16)",
    "log10(1/2)",
    "log10(9/16)",
    "log10(5/8)",
    "log10(11/16)",
    "log10(3/4)",
    "log10(13/16)",
    "log10(7/8)",
    "log10(15/16)",
    "log10(17/16)",
    "log10(9/8)",
    "log10(19/16)",
    "log10(5/4)",
    "log10(21/16)",
    "log10(11/8)",
    "log10(23/16)",
    "log10(3/2)",
    "log10(25/16)",
    "log10(13/8)",
    "log10(27/16)",
    "log10(7/4)",
    "log10(29/16)",
    "log10(15/8)",
    "log10(31/16)",
    "log10(2)",
    "log10(33/16)",
    "log10(17/8)",
    "log10(35/16)",
    "log10(9/4)",
    "log10(37/16)",
    "log10(19/8)",
    "log10(39/16)",
    "log10(5/2)",
    "log10(41/16)",
    "log10(21/8)",
    "log10(43/16)",
    "log10(11/4)",
    "log10(45/16)",
    "log10(23/8)",
    "log10(47/16)",
    "log10(3)",
    "log10(49/16)",
    "log10(25/8)",
    "log10(51/16)",
    "log10(13/4)",
    "log10(53/16)",
    "log10(27/8)",
    "log10(55/16)",
    "log10(7/2)",
    "log10(57/16)",
    "log10(29/8)",
    "log10(59/16)",
    "log10(15/4)",
    "log10(61/16)",
    "log10(31/8)",
    "log10(63/16)",
    "log10(4)",
    "log10(65/16)",
    "log10(33/8)",
    "log10(67/16)",
    "log10(17/4)",
    "log10(69/16)",
    "log10(35/8)",
    "log10(71/16)",
    "log10(9/2)",
    "log10(73/16)",
    "log10(37/8)",
    "log10(75/16)",
    "log10(19/4)",
    "log10(77/16)",
    "log10(39/8)",
    "log10(79/16)",
    "log10(5)",
    "log10(81/16)",
    "log10(41/8)",
    "log10(83/16)",
    "log10(21/4)",
    "log10(85/16)",
    "log10(43/8)",
    "log10(87/16)",
    "log10(11/2)",
    "log10(89/16)",
    "log10(45/8)",
    "log10(91/16)",
    "log10(23/4)",
    "log10(93/16)",
    "log10(47/8)",
    "log10(95/16)",
    "log10(6)",
    "log10(97/16)",
    "log10(49/8)",
    "log10(99/16)",
    "log10(25/4)",
    "log10(101/16)",
    "log10(51/8)",
    "log10(103/16)",
    "log10(13/2)",
    "log10(105/16)",
    "log10(53/8)",
    "log10(107/16)",
    "log10(27/4)",
    "log10(109/16)",
    "log10(55/8)",
    "log10(111/16)",
    "log10(7)",
    "log10(113/16)",
    "log10(57/8)",
    "log10(115/16)",
    "log10(29/4)",
    "log10(117/16)",
    "log10(59/8)",
    "log10(119/16)",
    "log10(15/2)",
    "log10(121/16)",
    "log10(61/8)",
    "log10(123/16)",
    "log10(31/4)",
    "log10(125/16)",
    "log10(63/8)",
    "log10(127/16)",
    "log10(8)",
    "log10(129/16)",
    "log10(65/8)",
    "log10(131/16)",
    "log10(33/4)",
    "log10(133/16)",
    "log10(67/8)",
    "log10(135/16)",
    "log10(17/2)",
    "log10(137/16)",
    "log10(69/8)",
    "log10(139/16)",
    "log10(35/4)",
    "log10(141/16)",
    "log10(71/8)",
    "log10(143/16)",
    "log10(9)",
    "log10(145/16)",
    "log10(73/8)",
    "log10(147/16)",
    "log10(37/4)",
    "log10(149/16)",
    "log10(75/8)",
    "log10(151/16)",
    "log10(19/2)",
    "log10(153/16)",
    "log10(77/8)",
    "log10(155/16)",
    "log10(39/4)",
    "log10(157/16)",
    "log10(79/8)",
    "log10(159/16)",
    "log10(10)",
    "log10(161/16)",
    "log10(81/8)",
    "log10(163/16)",
    "log10(41/4)",
    "log10(165/16)",
    "log10(83/8)",
    "log10(167/16)",
    "log10(21/2)",
    "log10(169/16)",
    "log10(85/8)",
    "log10(171/16)",
    "log10(43/4)",
    "log10(173/16)",
    "log10(87/8)",
    "log10(175/16)",
    "log10(11)",
    "log10(177/16)",
    "log10(89/8)",
    "log10(179/16)",
    "log10(45/4)",
    "log10(181/16)",
    "log10(91/8)",
    "log10(183/16)",
    "log10(23/2)",
    "log10(185/16)",
    "log10(93/8)",
    "log10(187/16)",
    "log10(47/4)",
    "log10(189/16)",
    "log10(95/8)",
    "log10(191/16)",
    "log10(12)",
    "log10(193/16)",
    "log10(97/8)",
    "log10(195/16)",
    "log10(49/4)",
    "log10(197/16)",
    "log10(99/8)",
    "log10(199/16)",
    "log10(25/2)",
    "log10(201/16)",
    "log10(101/8)",
    "log10(203/16)",
    "log10(51/4)",
    "log10(205/16)",
    "log10(103/8)",
    "log10(207/16)",
    "log10(13)",
    "log10(209/16)",
    "log10(105/8)",
    "log10(211/16)",
    "log10(53/4)",
    "log10(213/16)",
    "log10(107/8)",
    "log10(215/16)",
    "log10(27/2)",
    "log10(217/16)",
    "log10(109/8)",
    "log10(219/16)",
    "log10(55/4)",
    "log10(221/16)",
    "log10(111/8)",
    "log10(223/16)",
    "log10(14)",
    "log10(225/16)",
    "log10(113/8)",
    "log10(227/16)",
    "log10(57/4)",
    "log10(229/16)",
    "log10(115/8)",
    "log10(231/16)",
    "log10(29/2)",
    "log10(233/16)",
    "log10(117/8)",
    "log10(235/16)",
    "log10(59/4)",
    "log10(237/16)",
    "log10(119/8)",
    "log10(239/16)",
    "log10(15)",
    "log10(241/16)",
    "log10(121/8)",
    "log10(243/16)",
    "log10(61/4)",
    "log10(245/16)",
    "log10(123/8)",
    "log10(247/16)",
    "log10(31/2)",
    "log10(249/16)",
    "log10(125/8)",
    "log10(251/16)",
    "log10(63/4)",
    "log10(253/16)",
    "log10(127/8)",
    "log10(255/16)",
    \\  log10(2^(-2^n)) (10<=n<=13)
    "log10(2^(-2^10))",
    "log10(2^(-2^11))",
    "log10(2^(-2^12))",
    "log10(2^(-2^13))",
    \\  log10(2^(2^n)) (10<=n<=13)
    "log10(2^(2^10))",
    "log10(2^(2^11))",
    "log10(2^(2^12))",
    "log10(2^(2^13))"
    ]
  }  \\FOP_DATA_EXP10

FOP_DATA_LOG={
  [
    \\  exp(n/8) (1<=n<=63)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'exp('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "exp(1/8)",
    "exp(1/4)",
    "exp(3/8)",
    "exp(1/2)",
    "exp(5/8)",
    "exp(3/4)",
    "exp(7/8)",
    "exp(1)",
    "exp(9/8)",
    "exp(5/4)",
    "exp(11/8)",
    "exp(3/2)",
    "exp(13/8)",
    "exp(7/4)",
    "exp(15/8)",
    "exp(2)",
    "exp(17/8)",
    "exp(9/4)",
    "exp(19/8)",
    "exp(5/2)",
    "exp(21/8)",
    "exp(11/4)",
    "exp(23/8)",
    "exp(3)",
    "exp(25/8)",
    "exp(13/4)",
    "exp(27/8)",
    "exp(7/2)",
    "exp(29/8)",
    "exp(15/4)",
    "exp(31/8)",
    "exp(4)",
    "exp(33/8)",
    "exp(17/4)",
    "exp(35/8)",
    "exp(9/2)",
    "exp(37/8)",
    "exp(19/4)",
    "exp(39/8)",
    "exp(5)",
    "exp(41/8)",
    "exp(21/4)",
    "exp(43/8)",
    "exp(11/2)",
    "exp(45/8)",
    "exp(23/4)",
    "exp(47/8)",
    "exp(6)",
    "exp(49/8)",
    "exp(25/4)",
    "exp(51/8)",
    "exp(13/2)",
    "exp(53/8)",
    "exp(27/4)",
    "exp(55/8)",
    "exp(7)",
    "exp(57/8)",
    "exp(29/4)",
    "exp(59/8)",
    "exp(15/2)",
    "exp(61/8)",
    "exp(31/4)",
    "exp(63/8)"
    ]
  }  \\FOP_DATA_LOG

FOP_DATA_LOG1P={
  [
    \\  exp(n/8)-1 (1<=n<=63)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'exp('.$n.($d==1?'':'/'.$d).')-1'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "exp(1/8)-1",
    "exp(1/4)-1",
    "exp(3/8)-1",
    "exp(1/2)-1",
    "exp(5/8)-1",
    "exp(3/4)-1",
    "exp(7/8)-1",
    "exp(1)-1",
    "exp(9/8)-1",
    "exp(5/4)-1",
    "exp(11/8)-1",
    "exp(3/2)-1",
    "exp(13/8)-1",
    "exp(7/4)-1",
    "exp(15/8)-1",
    "exp(2)-1",
    "exp(17/8)-1",
    "exp(9/4)-1",
    "exp(19/8)-1",
    "exp(5/2)-1",
    "exp(21/8)-1",
    "exp(11/4)-1",
    "exp(23/8)-1",
    "exp(3)-1",
    "exp(25/8)-1",
    "exp(13/4)-1",
    "exp(27/8)-1",
    "exp(7/2)-1",
    "exp(29/8)-1",
    "exp(15/4)-1",
    "exp(31/8)-1",
    "exp(4)-1",
    "exp(33/8)-1",
    "exp(17/4)-1",
    "exp(35/8)-1",
    "exp(9/2)-1",
    "exp(37/8)-1",
    "exp(19/4)-1",
    "exp(39/8)-1",
    "exp(5)-1",
    "exp(41/8)-1",
    "exp(21/4)-1",
    "exp(43/8)-1",
    "exp(11/2)-1",
    "exp(45/8)-1",
    "exp(23/4)-1",
    "exp(47/8)-1",
    "exp(6)-1",
    "exp(49/8)-1",
    "exp(25/4)-1",
    "exp(51/8)-1",
    "exp(13/2)-1",
    "exp(53/8)-1",
    "exp(27/4)-1",
    "exp(55/8)-1",
    "exp(7)-1",
    "exp(57/8)-1",
    "exp(29/4)-1",
    "exp(59/8)-1",
    "exp(15/2)-1",
    "exp(61/8)-1",
    "exp(31/4)-1",
    "exp(63/8)-1"
    ]
  }  \\FOP_DATA_LOG1P

FOP_DATA_LOG10={
  [
    \\  10^(n/8) (1<=n<=63)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'10^('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "10^(1/8)",
    "10^(1/4)",
    "10^(3/8)",
    "10^(1/2)",
    "10^(5/8)",
    "10^(3/4)",
    "10^(7/8)",
    "10^(1)",
    "10^(9/8)",
    "10^(5/4)",
    "10^(11/8)",
    "10^(3/2)",
    "10^(13/8)",
    "10^(7/4)",
    "10^(15/8)",
    "10^(2)",
    "10^(17/8)",
    "10^(9/4)",
    "10^(19/8)",
    "10^(5/2)",
    "10^(21/8)",
    "10^(11/4)",
    "10^(23/8)",
    "10^(3)",
    "10^(25/8)",
    "10^(13/4)",
    "10^(27/8)",
    "10^(7/2)",
    "10^(29/8)",
    "10^(15/4)",
    "10^(31/8)",
    "10^(4)",
    "10^(33/8)",
    "10^(17/4)",
    "10^(35/8)",
    "10^(9/2)",
    "10^(37/8)",
    "10^(19/4)",
    "10^(39/8)",
    "10^(5)",
    "10^(41/8)",
    "10^(21/4)",
    "10^(43/8)",
    "10^(11/2)",
    "10^(45/8)",
    "10^(23/4)",
    "10^(47/8)",
    "10^(6)",
    "10^(49/8)",
    "10^(25/4)",
    "10^(51/8)",
    "10^(13/2)",
    "10^(53/8)",
    "10^(27/4)",
    "10^(55/8)",
    "10^(7)",
    "10^(57/8)",
    "10^(29/4)",
    "10^(59/8)",
    "10^(15/2)",
    "10^(61/8)",
    "10^(31/4)",
    "10^(63/8)"
    ]
  }  \\FOP_DATA_LOG10

FOP_DATA_LOG2={
  [
    \\  2^(n/8) (1<=n<=63,n!=8,n!=16,n!=32)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my@a=();for my$n(1..7,9..15,17..31,33..63){for my$d(8..8){my$g=gcd($n,$d);push@a,[$n/$g,$d/$g]}}for my$s(map{my($n,$d)=@$_;'2^('.$n.($d==1?'':'/'.$d).')'}sort{$a->[0]/$a->[1]<=>$b->[0]/$b->[1]}@a){print qq@    \x22$s\x22,\n@}"
    "2^(1/8)",
    "2^(1/4)",
    "2^(3/8)",
    "2^(1/2)",
    "2^(5/8)",
    "2^(3/4)",
    "2^(7/8)",
    "2^(9/8)",
    "2^(5/4)",
    "2^(11/8)",
    "2^(3/2)",
    "2^(13/8)",
    "2^(7/4)",
    "2^(15/8)",
    "2^(17/8)",
    "2^(9/4)",
    "2^(19/8)",
    "2^(5/2)",
    "2^(21/8)",
    "2^(11/4)",
    "2^(23/8)",
    "2^(3)",
    "2^(25/8)",
    "2^(13/4)",
    "2^(27/8)",
    "2^(7/2)",
    "2^(29/8)",
    "2^(15/4)",
    "2^(31/8)",
    "2^(33/8)",
    "2^(17/4)",
    "2^(35/8)",
    "2^(9/2)",
    "2^(37/8)",
    "2^(19/4)",
    "2^(39/8)",
    "2^(5)",
    "2^(41/8)",
    "2^(21/4)",
    "2^(43/8)",
    "2^(11/2)",
    "2^(45/8)",
    "2^(23/4)",
    "2^(47/8)",
    "2^(6)",
    "2^(49/8)",
    "2^(25/4)",
    "2^(51/8)",
    "2^(13/2)",
    "2^(53/8)",
    "2^(27/4)",
    "2^(55/8)",
    "2^(7)",
    "2^(57/8)",
    "2^(29/4)",
    "2^(59/8)",
    "2^(15/2)",
    "2^(61/8)",
    "2^(31/4)",
    "2^(63/8)"
    ]
  }  \\FOP_DATA_LOG2

FOP_DATA_TRIGONOMETRIC={
  bothsign([
    \\  n*pi/8 (1<=n<=32,10000<=n<=10032)
    \\  perl -e "sub gcd{my($x,$y)=@_;while($y){my$t=$x%$y;$x=$y;$y=$t;}$x}my$dd=8;for my$nn(1..32,10000..10032){my$g=gcd($nn,$dd);my$n=$nn/$g;my$d=$dd/$g;my$s=($n==1?'':$n.'*').'pi'.($d==1?'':'/'.$d);print qq@    \x22exd($s,RM)\x22, \x22exd($s,RP)\x22,\n@}"
    "exd(pi/8,RM)", "exd(pi/8,RP)",
    "exd(pi/4,RM)", "exd(pi/4,RP)",
    "exd(3*pi/8,RM)", "exd(3*pi/8,RP)",
    "exd(pi/2,RM)", "exd(pi/2,RP)",
    "exd(5*pi/8,RM)", "exd(5*pi/8,RP)",
    "exd(3*pi/4,RM)", "exd(3*pi/4,RP)",
    "exd(7*pi/8,RM)", "exd(7*pi/8,RP)",
    "exd(pi,RM)", "exd(pi,RP)",
    "exd(9*pi/8,RM)", "exd(9*pi/8,RP)",
    "exd(5*pi/4,RM)", "exd(5*pi/4,RP)",
    "exd(11*pi/8,RM)", "exd(11*pi/8,RP)",
    "exd(3*pi/2,RM)", "exd(3*pi/2,RP)",
    "exd(13*pi/8,RM)", "exd(13*pi/8,RP)",
    "exd(7*pi/4,RM)", "exd(7*pi/4,RP)",
    "exd(15*pi/8,RM)", "exd(15*pi/8,RP)",
    "exd(2*pi,RM)", "exd(2*pi,RP)",
    "exd(17*pi/8,RM)", "exd(17*pi/8,RP)",
    "exd(9*pi/4,RM)", "exd(9*pi/4,RP)",
    "exd(19*pi/8,RM)", "exd(19*pi/8,RP)",
    "exd(5*pi/2,RM)", "exd(5*pi/2,RP)",
    "exd(21*pi/8,RM)", "exd(21*pi/8,RP)",
    "exd(11*pi/4,RM)", "exd(11*pi/4,RP)",
    "exd(23*pi/8,RM)", "exd(23*pi/8,RP)",
    "exd(3*pi,RM)", "exd(3*pi,RP)",
    "exd(25*pi/8,RM)", "exd(25*pi/8,RP)",
    "exd(13*pi/4,RM)", "exd(13*pi/4,RP)",
    "exd(27*pi/8,RM)", "exd(27*pi/8,RP)",
    "exd(7*pi/2,RM)", "exd(7*pi/2,RP)",
    "exd(29*pi/8,RM)", "exd(29*pi/8,RP)",
    "exd(15*pi/4,RM)", "exd(15*pi/4,RP)",
    "exd(31*pi/8,RM)", "exd(31*pi/8,RP)",
    "exd(4*pi,RM)", "exd(4*pi,RP)",
    "exd(1250*pi,RM)", "exd(1250*pi,RP)",
    "exd(10001*pi/8,RM)", "exd(10001*pi/8,RP)",
    "exd(5001*pi/4,RM)", "exd(5001*pi/4,RP)",
    "exd(10003*pi/8,RM)", "exd(10003*pi/8,RP)",
    "exd(2501*pi/2,RM)", "exd(2501*pi/2,RP)",
    "exd(10005*pi/8,RM)", "exd(10005*pi/8,RP)",
    "exd(5003*pi/4,RM)", "exd(5003*pi/4,RP)",
    "exd(10007*pi/8,RM)", "exd(10007*pi/8,RP)",
    "exd(1251*pi,RM)", "exd(1251*pi,RP)",
    "exd(10009*pi/8,RM)", "exd(10009*pi/8,RP)",
    "exd(5005*pi/4,RM)", "exd(5005*pi/4,RP)",
    "exd(10011*pi/8,RM)", "exd(10011*pi/8,RP)",
    "exd(2503*pi/2,RM)", "exd(2503*pi/2,RP)",
    "exd(10013*pi/8,RM)", "exd(10013*pi/8,RP)",
    "exd(5007*pi/4,RM)", "exd(5007*pi/4,RP)",
    "exd(10015*pi/8,RM)", "exd(10015*pi/8,RP)",
    "exd(1252*pi,RM)", "exd(1252*pi,RP)",
    "exd(10017*pi/8,RM)", "exd(10017*pi/8,RP)",
    "exd(5009*pi/4,RM)", "exd(5009*pi/4,RP)",
    "exd(10019*pi/8,RM)", "exd(10019*pi/8,RP)",
    "exd(2505*pi/2,RM)", "exd(2505*pi/2,RP)",
    "exd(10021*pi/8,RM)", "exd(10021*pi/8,RP)",
    "exd(5011*pi/4,RM)", "exd(5011*pi/4,RP)",
    "exd(10023*pi/8,RM)", "exd(10023*pi/8,RP)",
    "exd(1253*pi,RM)", "exd(1253*pi,RP)",
    "exd(10025*pi/8,RM)", "exd(10025*pi/8,RP)",
    "exd(5013*pi/4,RM)", "exd(5013*pi/4,RP)",
    "exd(10027*pi/8,RM)", "exd(10027*pi/8,RP)",
    "exd(2507*pi/2,RM)", "exd(2507*pi/2,RP)",
    "exd(10029*pi/8,RM)", "exd(10029*pi/8,RP)",
    "exd(5015*pi/4,RM)", "exd(5015*pi/4,RP)",
    "exd(10031*pi/8,RM)", "exd(10031*pi/8,RP)",
    "exd(1254*pi,RM)", "exd(1254*pi,RP)"
    ])
  }  \\FOP_DATA_TRIGONOMETRIC

FOP_DATA_MULTIPARAM={
  bothsign([
    "2^-16446",  \\extendedの非正規化数の最小値
    "2^-1074",  \\doubleの非正規化数の最小値
    "2^-149",  \\singleの非正規化数の最小値
    "1/4",
    "exd(1/3,RN)",
    "1/2",
    "exd(2/3,RN)",
    "3/4",
    1,
    "exd(4/3,RN)",
    "3/2",
    2,
    3,
    4,
    "2^128-2^104",  \\singleの正規化数の最大値
    "2^1024-2^971",  \\doubleの正規化数の最大値
    "2^16384-2^16320"  \\extendedの正規化数の最大値
    ])
  }

\\make_test_clear()
\\  fpsrのコンディションコードバイトとエクセプションバイトをクリアする
make_test_clear(x)={
  fpsr=bitand(fpsr,(1<<24)-(1<<16)+(1<<8)-1)
  }

\\x=make_test_round(x)
\\  fpcrの丸め桁数と丸めモードに従って数値を丸める
make_test_round(x)={
  my(rp,rm);
  \\丸め処理
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  if(x==0,x=if(rm==RM,-Rei,Rei));
  if(rp==EXD,bitstoexd(exdtobitssr(x,rm)),
     rp==SGL,bitstosgl(sgltobitssr(x,rm)),
     bitstodbl(dbltobitssr(x,rm)))
  }

\\x=make_test_flag(x)
\\  fpsrのコンディションコードバイトとアクルードエクセプションバイトを更新する
make_test_flag(x)={
  my(rm);
  rm=bitand(fpcr>>4,3);  \\丸めモード
  if(x==0,x=if(rm==RM,-Rei,Rei));
  \\コンディションコードバイト
  fpsr=bitand(fpsr,(1<<24)-1);
  if(x==Rei,fpsr=bitor(fpsr,ZE),
     x==-Rei,fpsr=bitor(fpsr,MI+ZE),
     x==Inf,fpsr=bitor(fpsr,IN),
     x==-Inf,fpsr=bitor(fpsr,MI+IN),
     x==NaN,fpsr=bitor(fpsr,NA),
     x<0,fpsr=bitor(fpsr,MI));
  \\アクルードエクセプションバイト
  if(bitand(fpsr,BS+SN+OE)!=0,fpsr=bitor(fpsr,AV));
  if(bitand(fpsr,OF)!=0,fpsr=bitor(fpsr,AO));
  if(bitand(fpsr,UF+X2)==(UF+X2),fpsr=bitor(fpsr,AU));
  if(bitand(fpsr,DZ)!=0,fpsr=bitor(fpsr,AZ));
  if(bitand(fpsr,OF+X2+X1)!=0,fpsr=bitor(fpsr,AX));
  x
  }

make_test_fmovecr(s1)={
  if(s1=="MC68882ROM[3]","\t.dc.l\t$20000000,$7FFFFFFF,$00000000,NA",
     s1=="MC68882ROM[7]","\t.dc.l\t$00010000,$F65D8D9C,$00000000,NA",
     s1=="MC68882ROM[8]","\t.dc.l\t$7FFF0000,$401E0000,$00000000,NA",
     x=eval(s1);
     make_test_clear();
     if(x==0,x=Rei);  \\rm==ここではRMでも-Reiにしない
     x=make_test_round(x);
     make_test_flag(x))
  }

make_test_fmove(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       make_test_round(x));
  make_test_flag(x)
  }

make_test_fint(s1)={
  my(x,rm,y);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       y=if(rm==RN,
            if(frac(x)==1/2,  \\frac(x)=x-floor(x)
               if(x>=0,
                  floor(x)+(floor(x)%2),  \\x%y=x-floor(x/y)*y
                  ceil(x)-(ceil(x)%2)),
               floor(x+1/2)),
            rm==RZ,
            if(x>=0,floor(x),ceil(x)),
            rm==RM,
            floor(x),
            ceil(x));
       if(x!=y,fpsr=bitor(fpsr,X2));
       if(y==0,y=if(x<0,-Rei,Rei));
       bitstoexd(exdtobitssr(y,rm)));
  make_test_flag(x)
  }

make_test_fsinh(s1)={
  my(x,rp,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<-100000,fpsr=bitor(fpsr,OF);-Inf,
       100000<x,fpsr=bitor(fpsr,OF);Inf,
       fpsr=bitor(fpsr,X2);
       r=make_test_round(sinh(x));
       if(abs(x)<2^-16,
          if(x<0,
             if((rm==RM)&&(x<=r),r=nextdown(x,rp)),
             if((rm==RP)&&(r<=x),r=nextup(x,rp))));
       r);
  make_test_flag(x)
  }

make_test_fintrz(s1)={
  my(x,rm,y);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       y=if(x>=0,floor(x),ceil(x));
       if(x!=y,fpsr=bitor(fpsr,X2));
       if(y==0,y=if(x<0,-Rei,Rei));
       bitstoexd(exdtobitssr(y,rm)));
  make_test_flag(x)
  }

make_test_fsqrt(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       make_test_round(sqrt(x)));
  make_test_flag(x)
  }

make_test_flognp1(s1)={
  my(x,rp,rm,y,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  y=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<-1,fpsr=bitor(fpsr,OE);NaN,
       x==-1,fpsr=bitor(fpsr,DZ);-Inf,
       fpsr=bitor(fpsr,X2);
       r=make_test_round(log(1+x));
       if(abs(x)<2^-16,
          if(x<0,
             if((rm==RM)&&(x<=r),r=nextdown(x,rp)),
             if(((rm==RZ)||(rm==RM))&&(x<=r),r=nextdown(x,rp))));
       r);
  make_test_flag(y)
  }

make_test_fetoxm1(s1)={
  my(x,rp,rm,y,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  y=if(x==-Inf,-1,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<-100,fpsr=bitor(fpsr,X2);if((rm==RZ)||(rm==RP),nextup(-1,rp),-1),
       100<x,fpsr=bitor(fpsr,OF);make_test_round(Inf),
       fpsr=bitor(fpsr,X2);
       r=make_test_round(expm1(x));
       if(abs(x)<2^-16,
          if(x<0,
             if(((rm==RZ)||(rm==RP))&&(r<=x),r=nextup(x,rp)),
             if((rm==RP)&&(r<=x),r=nextup(x,rp))));
       r);
  make_test_flag(y)
  }

make_test_ftanh(s1)={
  my(x,rp,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,-1,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,1,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       if(x<=-2^6,if((rm==RZ)||(rm==RP),nextup(-1,rp),-1),
          2^6<=x,if((rm==RZ)||(rm==RM),nextdown(1,rp),1),
          make_test_round(tanh(x))));
  make_test_flag(x)
  }

make_test_fatan(s1)={
  my(x,rp,rm,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,X2);make_test_round(-pi/2),
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==NaN,NaN,
       x==Inf,fpsr=bitor(fpsr,X2);make_test_round(pi/2),
       fpsr=bitor(fpsr,X2);
       r=make_test_round(atan(x));
       if(abs(x)<2^-16,
          if(x<0,
             if(((rm==RZ)||(rm==RP))&&(r<=x),r=nextup(x,rp)),
             if(((rm==RZ)||(rm==RM))&&(x<=r),r=nextdown(x,rp))));
       r);
  make_test_flag(x)
  }

make_test_fasin(s1)={
  my(x,rp,rm,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       x<-1,fpsr=bitor(fpsr,OE);NaN,
       x==-1,fpsr=bitor(fpsr,X2);make_test_round(-pi/2),
       x==1,fpsr=bitor(fpsr,X2);make_test_round(pi/2),
       1<x,fpsr=bitor(fpsr,OE);NaN,
       fpsr=bitor(fpsr,X2);
       r=make_test_round(asin(x));
       if(abs(x)<2^-16,
          if(x<0,
             if((rm==RM)&&(x<=r),r=nextdown(x,rp)),
             if((rm==RP)&&(r<=x),r=nextup(x,rp))));
       r);
  make_test_flag(x)
  }

make_test_fatanh(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       x<-1,fpsr=bitor(fpsr,OE);NaN,
       x==-1,fpsr=bitor(fpsr,DZ);-Inf,
       x==1,fpsr=bitor(fpsr,DZ);Inf,
       1<x,fpsr=bitor(fpsr,OE);NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(atanh(x)));
  make_test_flag(x)
  }

make_test_fsin(s1)={
  my(x,rp,rm,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       r=make_test_round(sin(x));
       if(abs(x)<2^-16,
          if(x<0,
             if(((rm==RZ)||(rm==RP))&&(r<=x),r=nextup(x,rp)),
             if(((rm==RZ)||(rm==RM))&&(x<=r),r=nextdown(x,rp))));
       r);
  make_test_flag(x)
  }

make_test_ftan(s1)={
  my(x,rp,rm,r);
  x=exd(eval(s1),RN);
  make_test_clear();
  rp=bitand(fpcr>>6,3);  \\丸め桁数
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       r=make_test_round(tan(x));
       if(abs(x)<2^-16,
          if(x<0,
             if((rm==RM)&&(x<=r),r=nextdown(x,rp)),
             if((rm==RP)&&(r<=x),r=nextup(x,rp))));
       r);
  make_test_flag(x)
  }

make_test_fetox(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Rei,
       x==-Rei,1,
       x==Rei,1,
       x==Inf,Inf,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(if(x<-100000,fpsr=bitor(fpsr,UF);Rei,
                          100000<x,fpsr=bitor(fpsr,OF);Inf,
                          exp(x))));
  make_test_flag(x)
  }

make_test_ftwotox(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Rei,
       x==-Rei,1,
       x==Rei,1,
       x==Inf,Inf,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(if(x<-100000,fpsr=bitor(fpsr,UF);Rei,
                          100000<x,fpsr=bitor(fpsr,OF);Inf,
                          2^x)));
  make_test_flag(x)
  }

make_test_ftentox(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Rei,
       x==-Rei,1,
       x==Rei,1,
       x==Inf,Inf,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(if(x<-100000,fpsr=bitor(fpsr,UF);Rei,
                          100000<x,fpsr=bitor(fpsr,OF);Inf,
                          10^x)));
  make_test_flag(x)
  }

make_test_flogn(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       x==1,if(rm==RM,-Rei,Rei),
       fpsr=bitor(fpsr,X2);
       make_test_round(log(x)));
  make_test_flag(x)
  }

make_test_flog10(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       x==1,if(rm==RM,-Rei,Rei),
       fpsr=bitor(fpsr,X2);
       make_test_round(log10(x)));
  make_test_flag(x)
  }

make_test_flog2(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Rei,fpsr=bitor(fpsr,DZ);-Inf,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       x==1,if(rm==RM,-Rei,Rei),
       fpsr=bitor(fpsr,X2);
       make_test_round(log2(x)));
  make_test_flag(x)
  }

make_test_fabs(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       make_test_round(abs(x)));
  make_test_flag(x)
  }

make_test_fcosh(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Inf,
       x==-Rei,1,
       x==Rei,1,
       x==Inf,Inf,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(if(x<-100000,fpsr=bitor(fpsr,OF);Inf,
                          100000<x,fpsr=bitor(fpsr,OF);Inf,
                          cosh(x))));
  make_test_flag(x)
  }

make_test_fneg(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,-Rei,
       x==Inf,-Inf,
       x==NaN,NaN,
       make_test_round(-x));
  make_test_flag(x)
  }

make_test_facos(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,fpsr=bitor(fpsr,X2);make_test_round(pi/2),
       x==Rei,fpsr=bitor(fpsr,X2);make_test_round(pi/2),
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       x<-1,fpsr=bitor(fpsr,OE);NaN,
       x==-1,fpsr=bitor(fpsr,X2);make_test_round(pi),
       x==1,Rei,
       1<x,fpsr=bitor(fpsr,OE);NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(acos(x)));
  make_test_flag(x)
  }

make_test_fcos(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,1,
       x==Rei,1,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       fpsr=bitor(fpsr,X2);
       make_test_round(cos(x)));
  make_test_flag(x)
  }

make_test_fgetexp(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       make_test_round(floor(log2(abs(x)))));
  make_test_flag(x)
  }

make_test_fgetman(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,fpsr=bitor(fpsr,OE);NaN,
       x==NaN,NaN,
       make_test_round(2^-floor(log2(abs(x)))*x));
  make_test_flag(x)
  }

make_test_fdiv(s1,s2)={
  my(x,y,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±0/±0=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf/±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),if(x==Inf,1,-1)*if(y==Rei,1,-1)*Inf,  \\±Inf/±0=±Inf,non-DZ
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),if(x==Rei,1,-1)*if(y==Inf,1,-1)*Rei,  \\±0/±Inf=±0
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,DZ);sign(x)*if(y==Rei,1,-1)*Inf,  \\±x/±0=±Inf,DZ
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Rei,  \\±x/±Inf=±0
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0/±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf/±y=±Inf
       make_test_round(x/y));
  make_test_flag(z)
}

make_test_fmod(s1,s2)={
  my(x,y,z,q,r);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  fpsr=bitand(fpsr,(1<<32)-(1<<24)+(1<<16)-(1<<0));  \\quotient byteをクリアする
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)||(x==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\±Inf%±y=NaN,OE
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,OE);NaN,  \\±x%±0=NaN,OE
       fpsr=bitor(fpsr,bitxor(isnegative(x),isnegative(y))<<23);  \\商の符号
       if((x==Rei)||(x==-Rei),x,  \\±0%±y=±0,商は±0
          (y==Inf)||(y==-Inf),make_test_round(x),  \\±x%±Inf=±x,商は±0。xが非正規化数のときUFをセットする
          q=trunc(x/y);
          fpsr=bitor(fpsr,bitand(abs(q),127)<<16);  \\商の絶対値の下位7bit
          r=make_test_round(x-q*y);
          if((r==Rei)||(r==-Rei),r=sign(x)*Rei);  \\余りが0のときは0にxの符号を付ける
          r));
  make_test_flag(z)
}

make_test_fadd(s1,s2)={
  my(x,y,rm,z,r);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf+-Inf=NaN,OE
       (x==-Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf+Inf=NaN,OE
       (x==Rei)&&(y==Rei),Rei,  \\0+0=0
       (x==-Rei)&&(y==-Rei),-Rei,  \\-0+-0=-0
       (x==Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\0+-0=±0
       (x==-Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\-0+0=±0
       (y==Inf)||(y==-Inf),y,  \\±x+±Inf=±Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf+±y=±Inf
       r=make_test_round(if((x==Rei)||(x==-Rei),0,x)+if((y==Rei)||(y==-Rei),0,y));  \\±x+±0=±x, ±0+±y=±y
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei),r));
  make_test_flag(z)
}

make_test_fmul(s1,s2)={
  my(x,y,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±0*±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf*±0=NaN,OE
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),if(x==Rei,1,-1)*if(y==Rei,1,-1)*Rei,  \\±0*±0=±0
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),if(x==Inf,1,-1)*if(y==Inf,1,-1)*Inf,  \\±Inf*±Inf=±Inf
       (y==Rei)||(y==-Rei),sign(x)*if(y==Rei,1,-1)*Rei,  \\±x*±0=±0
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Inf,  \\±x*±Inf=±Inf
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0*±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf*±y=±Inf
       make_test_round(x*y));
  make_test_flag(z)
}

make_test_fsgldiv882(s1,s2)={
  my(x,y,rm,z,t);
  x=eval(s1);  \\destination
  y=eval(s2);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±0/±0=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf/±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),if(x==Inf,1,-1)*if(y==Rei,1,-1)*Inf,  \\±Inf/±0=±Inf,non-DZ
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),if(x==Rei,1,-1)*if(y==Inf,1,-1)*Rei,  \\±0/±Inf=±0
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,DZ);sign(x)*if(y==Rei,1,-1)*Inf,  \\±x/±0=±Inf,DZ
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Rei,  \\±x/±Inf=±0
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0/±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf/±y=±Inf
       x=roundfman882(x,RN,0);  \\destination
       y=roundfman882(y,RN,0);  \\source
       roundfman882(x/y,rm,1));
  make_test_flag(z)
}
make_test_fsgldiv060(s1,s2)={
  my(x,y,rm,z,t);
  x=eval(s1);  \\destination
  y=eval(s2);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±0/±0=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf/±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),if(x==Inf,1,-1)*if(y==Rei,1,-1)*Inf,  \\±Inf/±0=±Inf,non-DZ
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),if(x==Rei,1,-1)*if(y==Inf,1,-1)*Rei,  \\±0/±Inf=±0
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,DZ);sign(x)*if(y==Rei,1,-1)*Inf,  \\±x/±0=±Inf,DZ
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Rei,  \\±x/±Inf=±0
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0/±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf/±y=±Inf
       x=roundfman060(x,RN,0);  \\destination
       y=roundfman060(y,RN,0);  \\source
       roundfman060(x/y,rm,1));
  make_test_flag(z)
}

make_test_frem(s1,s2)={
  my(x,y,z,q,r);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  fpsr=bitand(fpsr,(1<<32)-(1<<24)+(1<<16)-(1<<0));  \\quotient byteをクリアする
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)||(x==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\±Inf%±y=NaN,OE
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,OE);NaN,  \\±x%±0=NaN,OE
       fpsr=bitor(fpsr,bitxor(isnegative(x),isnegative(y))<<23);  \\商の符号
       if((x==Rei)||(x==-Rei),x,  \\±0%±y=±0,商は±0
          (y==Inf)||(y==-Inf),make_test_round(x),  \\±x%±Inf=±x,商は±0。xが非正規化数のときUFをセットする
          q=rint(x/y);
          fpsr=bitor(fpsr,bitand(abs(q),127)<<16);  \\商の絶対値の下位7bit
          r=make_test_round(x-q*y);
          if((r==Rei)||(r==-Rei),r=sign(x)*Rei);  \\余りが0のときは0にxの符号を付ける
          r));
  make_test_flag(z)
}

make_test_fscale(s1,s2)={
  my(x,y,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  z=if((x==NaN)||(y==NaN),NaN,  \\NaNと±InfのときOEはセットされない
       (y==Inf)||(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\scale(±x,±Inf)=NaN,OE
       (x==Inf)||(x==-Inf),x,  \\scale(±Inf,±y)=±Inf
       (x==Rei)||(x==-Rei),x,  \\scale(±0,±y)=±0
       (y==Rei)||(y==-Rei),make_test_round(x),  \\scale(±x,±0)=±x
       y<-2^14,make_test_round(sign(x)*2^-20000),  \\scale(±x,small)=±0,UF+X2
       2^14<=y,make_test_round(sign(x)*2^20000),  \\scale(±x,big)=±0,OF
       make_test_round(x*2^trunc(y)));
  make_test_flag(z)
}

make_test_fsglmul882(s1,s2)={
  my(x,y,rm,z);
  x=roundfman882(eval(s1),RZ,0);  \\destination
  y=roundfman882(eval(s2),RZ,0);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±0*±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf*±0=NaN,OE
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),if(x==Rei,1,-1)*if(y==Rei,1,-1)*Rei,  \\±0*±0=±0
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),if(x==Inf,1,-1)*if(y==Inf,1,-1)*Inf,  \\±Inf*±Inf=±Inf
       (y==Rei)||(y==-Rei),sign(x)*if(y==Rei,1,-1)*Rei,  \\±x*±0=±0
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Inf,  \\±x*±Inf=±Inf
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0*±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf*±y=±Inf
       roundfman882(x*y,rm,1));
  make_test_flag(z)
}
make_test_fsglmul060(s1,s2)={
  my(x,y,rm,z);
  x=roundfman060(eval(s1),RZ,0);  \\destination
  y=roundfman060(eval(s2),RZ,0);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±0*±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf*±0=NaN,OE
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),if(x==Rei,1,-1)*if(y==Rei,1,-1)*Rei,  \\±0*±0=±0
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),if(x==Inf,1,-1)*if(y==Inf,1,-1)*Inf,  \\±Inf*±Inf=±Inf
       (y==Rei)||(y==-Rei),sign(x)*if(y==Rei,1,-1)*Rei,  \\±x*±0=±0
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Inf,  \\±x*±Inf=±Inf
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0*±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf*±y=±Inf
       roundfman060(x*y,rm,1));
  make_test_flag(z)
}

make_test_fsub(s1,s2)={
  my(x,y,rm,z,r);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf-Inf=NaN,OE
       (x==-Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf--Inf=NaN,OE
       (x==Rei)&&(y==-Rei),Rei,  \\0--0=0
       (x==-Rei)&&(y==Rei),-Rei,  \\-0-0=-0
       (x==Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\0-0=±0
       (x==-Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\-0--0=±0
       (y==Inf)||(y==-Inf),-y,  \\±x-±Inf=∓Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf-±y=±Inf
       r=make_test_round(if((x==Rei)||(x==-Rei),0,x)-if((y==Rei)||(y==-Rei),0,y));  \\±x+±0=±x, ±0+±y=±y
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei),r));
  make_test_flag(z)
}

make_test_fsincos(s1)={
  my(x,yz);
  x=exd(eval(s1),RN);
  make_test_clear();
  yz=if(x==-Inf,fpsr=bitor(fpsr,OE);[NaN,NaN],
        x==-Rei,[-Rei,1],
        x==Rei,[Rei,1],
        x==Inf,fpsr=bitor(fpsr,OE);[NaN,NaN],
        x==NaN,[NaN,NaN],
        [make_test_round(sin(x)),make_test_round(cos(x))]);
  make_test_flag(yz[1]);
  yz
  }

make_test_fcmp(s1,s2)={
  my(x,y);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  fpsr=bitor(fpsr,if((x==NaN)||(y==NaN),NA,  \\どちらかがNaN
                     x==y,if((x==-Rei)||(x==-Inf),MI+ZE,(x==Rei)||(x==Inf),ZE,x<0,MI+ZE,ZE),  \\-0==-0, +0==+0, -Inf==-Inf, +Inf==+Inf, ±x==±y
                     (x==-Rei)&&(y==Rei),MI+ZE,  \\-0==+0
                     (x==Rei)&&(y==-Rei),ZE,  \\+0==-0
                     (x==-Inf)||(y==Inf),MI,  \\-Inf<±y, ±x<Inf
                     (x==Inf)||(y==-Inf),0,  \\+Inf>±y, ±x>-Inf
                     if((x==Rei)||(x==-Rei),0,x)<if((y==Rei)||(y==-Rei),0,y),MI,  \\±x<±y
                     0));  \\±x>±y
  x  \\destination
}

make_test_ftst(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  fpsr=bitor(fpsr,if(x==-Inf,MI+IN,
                     x==-Rei,MI+ZE,
                     x==Rei,ZE,
                     x==Inf,IN,
                     x==NaN,NA,
                     x<0,MI,
                     0));
  x
  }

make_test_fsmove(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       bitstosgl(sgltobitssr(x,rm)));
  make_test_flag(x)
  }

make_test_fssqrt(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       bitstosgl(sgltobitssr(sqrt(x),rm)));
  make_test_flag(x)
  }

make_test_fdmove(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,-Inf,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       bitstodbl(dbltobitssr(x,bitand(fpcr>>4,3))));
  make_test_flag(x)
  }

make_test_fdsqrt(s1)={
  my(x);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,fpsr=bitor(fpsr,OE);NaN,
       x==-Rei,-Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       x<0,fpsr=bitor(fpsr,OE);NaN,
       bitstodbl(dbltobitssr(sqrt(x),bitand(fpcr>>4,3))));
  make_test_flag(x)
  }

make_test_fsabs(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       bitstosgl(sgltobitssr(abs(x),rm)));
  make_test_flag(x)
  }

make_test_fsneg(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,-Rei,
       x==Inf,-Inf,
       x==NaN,NaN,
       bitstosgl(sgltobitssr(-x,rm)));
  make_test_flag(x)
  }

make_test_fdabs(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,Rei,
       x==Inf,Inf,
       x==NaN,NaN,
       bitstodbl(dbltobitssr(abs(x),bitand(fpcr>>4,3))));
  make_test_flag(x)
  }

make_test_fdneg(s1)={
  my(x,rm);
  x=exd(eval(s1),RN);
  make_test_clear();
  x=if(x==-Inf,Inf,
       x==-Rei,Rei,
       x==Rei,-Rei,
       x==Inf,-Inf,
       x==NaN,NaN,
       bitstodbl(dbltobitssr(-x,bitand(fpcr>>4,3))));
  make_test_flag(x)
  }

make_test_fsdiv(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±0/±0=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf/±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),if(x==Inf,1,-1)*if(y==Rei,1,-1)*Inf,  \\±Inf/±0=±Inf,non-DZ
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),if(x==Rei,1,-1)*if(y==Inf,1,-1)*Rei,  \\±0/±Inf=±0
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,DZ);sign(x)*if(y==Rei,1,-1)*Inf,  \\±x/±0=±Inf,DZ
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Rei,  \\±x/±Inf=±0
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0/±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf/±y=±Inf
       bitstosgl(sgltobitssr(x/y,rm)));
  make_test_flag(z)
}

make_test_fsadd(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf+-Inf=NaN,OE
       (x==-Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf+Inf=NaN,OE
       (x==Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\0+-0=±0
       (x==-Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\-0+0=±0
       (y==Inf)||(y==-Inf),y,  \\±x+±Inf=±Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf+±y=±Inf
       r=bitstosgl(sgltobitssr(if((y==Rei)||(y==-Rei),x,  \\±x+±0=±x
                                  (x==Rei)||(x==-Rei),y,  \\±0+±y=±y
                                  x+y),
                               rm));
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei));
       r);
  make_test_flag(z)
}

make_test_fsmul(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±0*±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf*±0=NaN,OE
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),if(x==Rei,1,-1)*if(y==Rei,1,-1)*Rei,  \\±0*±0=±0
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),if(x==Inf,1,-1)*if(y==Inf,1,-1)*Inf,  \\±Inf*±Inf=±Inf
       (y==Rei)||(y==-Rei),sign(x)*if(y==Rei,1,-1)*Rei,  \\±x*±0=±0
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Inf,  \\±x*±Inf=±Inf
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0*±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf*±y=±Inf
       bitstosgl(sgltobitssr(x*y,rm)));
  make_test_flag(z)
}

make_test_fddiv(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±0/±0=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf/±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),if(x==Inf,1,-1)*if(y==Rei,1,-1)*Inf,  \\±Inf/±0=±Inf,non-DZ
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),if(x==Rei,1,-1)*if(y==Inf,1,-1)*Rei,  \\±0/±Inf=±0
       (y==Rei)||(y==-Rei),fpsr=bitor(fpsr,DZ);sign(x)*if(y==Rei,1,-1)*Inf,  \\±x/±0=±Inf,DZ
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Rei,  \\±x/±Inf=±0
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0/±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf/±y=±Inf
       bitstodbl(dbltobitssr(x/y,rm)));
  make_test_flag(z)
}

make_test_fdadd(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf+-Inf=NaN,OE
       (x==-Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf+Inf=NaN,OE
       (x==Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\0+-0=±0
       (x==-Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\-0+0=±0
       (y==Inf)||(y==-Inf),y,  \\±x+±Inf=±Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf+±y=±Inf
       r=bitstodbl(dbltobitssr(if((y==Rei)||(y==-Rei),x,  \\±x+±0=±x
                                  (x==Rei)||(x==-Rei),y,  \\±0+±y=±y
                                  x+y),
                               rm));
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei));
       r);
  make_test_flag(z)
}

make_test_fdmul(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       ((x==Rei)||(x==-Rei))&&((y==Inf)||(y==-Inf)),fpsr=bitor(fpsr,OE);NaN,  \\±0*±Inf=NaN,OE
       ((x==Inf)||(x==-Inf))&&((y==Rei)||(y==-Rei)),fpsr=bitor(fpsr,OE);NaN,  \\±Inf*±0=NaN,OE
       ((x==Rei)||(x==-Rei))&&((y==Rei)||(y==-Rei)),if(x==Rei,1,-1)*if(y==Rei,1,-1)*Rei,  \\±0*±0=±0
       ((x==Inf)||(x==-Inf))&&((y==Inf)||(y==-Inf)),if(x==Inf,1,-1)*if(y==Inf,1,-1)*Inf,  \\±Inf*±Inf=±Inf
       (y==Rei)||(y==-Rei),sign(x)*if(y==Rei,1,-1)*Rei,  \\±x*±0=±0
       (y==Inf)||(y==-Inf),sign(x)*if(y==Inf,1,-1)*Inf,  \\±x*±Inf=±Inf
       (x==Rei)||(x==-Rei),if(x==Rei,1,-1)*sign(y)*Rei,  \\±0*±y=±0
       (x==Inf)||(x==-Inf),if(x==Inf,1,-1)*sign(y)*Inf,  \\±Inf*±y=±Inf
       bitstodbl(dbltobitssr(x*y,rm)));
  make_test_flag(z)
}

make_test_fssub(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf-Inf=NaN,OE
       (x==-Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf--Inf=NaN,OE
       (x==Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\0-0=±0
       (x==-Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\-0--0=±0
       (y==Inf)||(y==-Inf),-y,  \\±x-±Inf=∓Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf-±y=±Inf
       r=bitstosgl(sgltobitssr(if((y==Rei)||(y==-Rei),x,  \\±x-±0=±x
                                  (x==Rei)||(x==-Rei),-y,  \\±0-±y=∓y
                                  x-y),
                               rm));
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei));
       r);
  make_test_flag(z)
}

make_test_fdsub(s1,s2)={
  my(x,y,rm,z);
  x=exd(eval(s1),RN);  \\destination
  y=exd(eval(s2),RN);  \\source
  make_test_clear();
  rm=bitand(fpcr>>4,3);  \\丸めモード
  z=if((x==NaN)||(y==NaN),NaN,
       (x==Inf)&&(y==Inf),fpsr=bitor(fpsr,OE);NaN,  \\Inf-Inf=NaN,OE
       (x==-Inf)&&(y==-Inf),fpsr=bitor(fpsr,OE);NaN,  \\-Inf--Inf=NaN,OE
       (x==Rei)&&(y==Rei),if(rm==RM,-Rei,Rei),  \\0-0=±0
       (x==-Rei)&&(y==-Rei),if(rm==RM,-Rei,Rei),  \\-0--0=±0
       (y==Inf)||(y==-Inf),-y,  \\±x-±Inf=∓Inf
       (x==Inf)||(x==-Inf),x,  \\±Inf-±y=±Inf
       r=bitstodbl(dbltobitssr(if((y==Rei)||(y==-Rei),x,  \\±x-±0=±x
                                  (x==Rei)||(x==-Rei),-y,  \\±0-±y=∓y
                                  x-y),
                               rm));
       if((r==Rei)||(r==-Rei),if(rm==RM,-Rei,Rei));
       r);
  make_test_flag(z)
}

\\make_test_1to1(id,mnemonic,fop,data,prec)
\\  命令のテストデータを生成する(ソースオペランドが1個の場合)
\\  id         識別子。"fmove"など
\\  mnemonic   ニモニック。"fmove"など
\\  x=fop(s1)  テストする命令の演算を行う関数
\\             fpcrの丸め桁数と丸めモードに従って演算を行う
\\             fpsrを更新する
\\               コンディションコードバイトとエクセプションバイトはクリアして作り直す
\\               アクルードエクセプションバイトはORで重ねる
\\    s1       ソースオペランド1を表す文字列
\\    x        演算結果の数値または"\t.dc.l\t$7FFFFFFF,$FFFFFFFF,$FFFFFFFF,NA"のような文字列
\\  data       s1を列挙したベクタ
\\  prec       singleとdoubleの有無。0=expected resultはextendedの4個,1=expected resultはextended,single,doubleの12個
make_test_1to1(id,mnemonic,fop,data,prec)={
  my(s1,x,y,sr);
  print();
  if(mnemonic=="fmovecr",
     \\fmovecr
     for(i=0,#data-1,
         print("test_",id,"_func_",i,":");
         print("\t",mnemonic,".x\t#",i,",fp7");
         print("\trts")),
     mnemonic=="ftst",
     \\ftst
     print("test_",id,"_func::");
     print("\t",mnemonic,".x\tfp7");
     print("\trts"),
     \\その他
     print("test_",id,"_func::");
     print("\t",mnemonic,".x\t(a3),fp7");
     print("\trts"));
  print();
  print("\t.align\t4");
  print("test_",id,"_data::");
  for(i=0,#data-1,
      s1=data[i+1];
      fpcr=0;  \\extended RN
      x=exd(eval(s1),RN);
      print("\t.dc.l\t",exdtoimm(x),"\t;",s1,if(type(x)=="t_POL",if(x==s1,"",Str("=",x)),Str("=",formatg(x,30))));
      print("\tdclstr\t'",id,"[",i,"](",s1,")'");
      if(mnemonic=="fmovecr",
         print("\t.dc.l\ttest_",id,"_func_",i));
      for(rp=0,if(prec,2,0),
          for(rm=0,3,
              fpcr=(rp<<6)+(rm<<4);
              fpsr=0;  \\アクルードエクセプションバイトをクリアする
              y=fop(s1);
              sr=fpsr;
              print(if(type(y)=="t_STR",y,Str("\t.dc.l\t",exdtoimm(y),",",fpsrtostr(sr))),"\t;",
                    if(rp==0,"extended",
                       rp==1,"single",
                       rp==2,"double",
                       "undefined"),
                    " ",
                    if(rm==0,"RN",
                       rm==1,"RZ",
                       rm==2,"RM",
                       "RP")))));
  print("\t.dc.l\t$FFFFFFFF")
}

\\make_test_2to1(id,mnemonic,fop,data1,data2,prec)
\\  命令のテストデータを生成する(ソースオペランドが2個の場合)
\\  id            識別子。"fsglmul882"など
\\  mnemonic      ニモニック。"fsglmul"など
\\  x=fop(s1,s2)  テストする命令の演算を行う関数
\\                fpcrの丸め桁数と丸めモードに従って演算を行う
\\                fpsrを更新する
\\                  コンディションコードバイトとエクセプションバイトはクリアして作り直す
\\                  アクルードエクセプションバイトはORで重ねる
\\    s1          ソースオペランド1を表す文字列
\\    s2          ソースオペランド2を表す文字列
\\    x           演算結果の数値または"\t.dc.l\t$7FFFFFFF,$FFFFFFFF,$FFFFFFFF,NA"のような文字列
\\  data1         s1を列挙したベクタ
\\  data2         s2を列挙したベクタ
\\  prec          singleとdoubleの有無。0=expected resultはextendedの4個,1=expected resultはextended,single,doubleの12個
make_test_2to1(id,mnemonic,fop,data1,data2,prec)={
  my(s1,s2,x,y,z,sr);
  print();
  print("test_",id,"_func::");
  print("\t",mnemonic,".x\t12(a3),fp7");
  print("\trts");
  print();
  print("\t.align\t4");
  print("test_",id,"_data::");
  for(i=0,#data1-1,
      s1=data1[i+1];
      fpcr=0;  \\extended RN
      x=exd(eval(s1),RN);
      for(j=0,#data2-1,
          s2=data2[j+1];
          fpcr=0;  \\extended RN
          y=exd(eval(s2),RN);
          print("\t.dc.l\t",exdtoimm(x),"\t;",s1,if(type(x)=="t_POL",if(x==s1,"",Str("=",x)),Str("=",formatg(x,30))));
          print("\t.dc.l\t",exdtoimm(y),"\t;",s2,if(type(y)=="t_POL",if(y==s2,"",Str("=",y)),Str("=",formatg(y,30))));
          print("\tdclstr\t'",id,"[",#data2*i+j,"](",s1,",",s2,")'");
          for(rp=0,if(prec,2,0),
              for(rm=0,3,
                  fpcr=(rp<<6)+(rm<<4);
                  fpsr=0;  \\アクルードエクセプションバイトをクリアする
                  z=fop(s1,s2);
                  sr=fpsr;
                  print(if(type(z)=="t_STR",z,Str("\t.dc.l\t",exdtoimm(z),",",fpsrtostr(sr))),"\t;",
                        if(rp==0,"extended",
                           rp==1,"single",
                           rp==2,"double",
                           "undefined"),
                        " ",
                        if(rm==0,"RN",
                           rm==1,"RZ",
                           rm==2,"RM",
                           "RP"))))));
  print("\t.dc.l\t$FFFFFFFF")
}

\\make_test_1to2(id,mnemonic,fop,data,prec)
\\  命令のテストデータを生成する(デスティネーションオペランドが2個の場合)
\\  id             識別子。"fsincos"など
\\  mnemonic       ニモニック。"fsincos"など
\\  [y,z]=fop(s1)  テストする命令の演算を行う関数
\\                 fpcrの丸め桁数と丸めモードに従って演算を行う
\\                 fpsrを更新する
\\                   コンディションコードバイトとエクセプションバイトはクリアして作り直す
\\                   アクルードエクセプションバイトはORで重ねる
\\    s1           ソースオペランド1を表す文字列
\\    y            destinationの数値
\\    z            co-destinationの数値
\\  data       s1を列挙したベクタ
\\  prec       singleとdoubleの有無。0=expected resultはextendedの4個,1=expected resultはextended,single,doubleの12個
make_test_1to2(id,mnemonic,fop,data,prec)={
  my(s1,x,yz,y,z,sr);
  print();
  print("test_",id,"_func::");
  print("\t",mnemonic,".x\t(a3),fp6:fp7");
  print("\trts");
  print();
  print("\t.align\t4");
  print("test_",id,"_data::");
  for(i=0,#data-1,
      s1=data[i+1];
      fpcr=0;  \\extended RN
      x=exd(eval(s1),RN);
      print("\t.dc.l\t",exdtoimm(x),"\t;",s1,if(type(x)=="t_POL",if(x==s1,"",Str("=",x)),Str("=",formatg(x,30))));
      print("\tdclstr\t'",id,"[",i,"](",s1,")'");
      for(rp=0,if(prec,2,0),
          for(rm=0,3,
              fpcr=(rp<<6)+(rm<<4);
              fpsr=0;  \\アクルードエクセプションバイトをクリアする
              yz=fop(s1);
              sr=fpsr;
              y=yz[1];
              z=yz[2];
              print("\t.dc.l\t",exdtoimm(y),",",exdtoimm(z),",",fpsrtostr(sr),"\t;",
                    if(rp==0,"extended",
                       rp==1,"single",
                       rp==2,"double",
                       "undefined"),
                    " ",
                    if(rm==0,"RN",
                       rm==1,"RZ",
                       rm==2,"RM",
                       "RP")))));
  print("\t.dc.l\t$FFFFFFFF")
}

make_test()={
  print("dclstr\t.macro\tstr");
  print("\t.data");
  print("@@:\t.dc.b\tstr,0");
  print("\t.text");
  print("\t.dc.l\t@b");
  print("\t.endm");
  print();
  print("MI\tequ\t$08000000");
  print("ZE\tequ\t$04000000");
  print("IN\tequ\t$02000000");
  print("NA\tequ\t$01000000");
  print("BS\tequ\t$00008000");
  print("SN\tequ\t$00004000");
  print("OE\tequ\t$00002000");
  print("OF\tequ\t$00001000");
  print("UF\tequ\t$00000800");
  print("DZ\tequ\t$00000400");
  print("X2\tequ\t$00000200");
  print("X1\tequ\t$00000100");
  print("AV\tequ\t$00000080");
  print("AO\tequ\t$00000040");
  print("AU\tequ\t$00000020");
  print("AZ\tequ\t$00000010");
  print("AX\tequ\t$00000008");
  print();
  print("\t.text");
  print();
  print("\t.cpu\t68030");
  make_test_1to1("fmovecr",
                 "fmovecr",
                 make_test_fmovecr,
                 FMOVECR_DATA,
                 0);
  make_test_1to1("fmove",
                 "fmove",
                 make_test_fmove,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 1);
  make_test_1to1("fint",
                 "fint",
                 make_test_fint,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 1);
  make_test_1to1("fsinh",
                 "fsinh",
                 make_test_fsinh,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXP,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS),
                 0);
  make_test_1to1("fintrz",
                 "fintrz",
                 make_test_fintrz,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 1);
  make_test_1to1("fsqrt",
                 "fsqrt",
                 make_test_fsqrt,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO,
                        FOP_DATA_PLUSTWO,
                        FOP_DATA_PLUSBIG),
                 0);
  make_test_1to1("flognp1",
                 "flognp1",
                 make_test_flognp1,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_LOG1P,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS),
                 0);
  make_test_1to1("fetoxm1",
                 "fetoxm1",
                 make_test_fetoxm1,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXPM1,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS),
                 0);
  make_test_1to1("ftanh",
                 "ftanh",
                 make_test_ftanh,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSBIG,
                        FOP_DATA_MINUSTWO,
                        FOP_DATA_MINUSTWOTOMINUSONE,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_MINUSONETOZERO,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO,
                        FOP_DATA_PLUSTWO,
                        FOP_DATA_PLUSBIG),
                 0);
  make_test_1to1("fatan",
                 "fatan",
                 make_test_fatan,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSBIG,
                        FOP_DATA_MINUSTWO,
                        FOP_DATA_MINUSTWOTOMINUSONE,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_MINUSONETOZERO,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO,
                        FOP_DATA_PLUSTWO,
                        FOP_DATA_PLUSBIG),
                 0);
  make_test_1to1("fasin",
                 "fasin",
                 make_test_fasin,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSTWO,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_MINUSONEPLUSEPS,
                        FOP_DATA_MINUSONETOZERO,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONEMINUSEPS,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSTWO),
                 0);
  make_test_1to1("fatanh",
                 "fatanh",
                 make_test_fatanh,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSTWO,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_MINUSONEPLUSEPS,
                        FOP_DATA_MINUSONETOZERO,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONEMINUSEPS,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSTWO),
                 0);
  make_test_1to1("fsin",
                 "fsin",
                 make_test_fsin,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_TRIGONOMETRIC,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS),
                 0);
  make_test_1to1("ftan",
                 "ftan",
                 make_test_ftan,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_TRIGONOMETRIC,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS),
                 0);
  make_test_1to1("fetox",
                 "fetox",
                 make_test_fetox,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXP),
                 0);
  make_test_1to1("ftwotox",
                 "ftwotox",
                 make_test_ftwotox,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXP2),
                 0);
  make_test_1to1("ftentox",
                 "ftentox",
                 make_test_ftentox,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXP10),
                 0);
  make_test_1to1("flogn",
                 "flogn",
                 make_test_flogn,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_LOG),
                 0);
  make_test_1to1("flog10",
                 "flog10",
                 make_test_flog10,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_LOG10),
                 0);
  make_test_1to1("flog2",
                 "flog2",
                 make_test_flog2,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_LOG2),
                 0);
  make_test_1to1("fabs",
                 "fabs",
                 make_test_fabs,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fcosh",
                 "fcosh",
                 make_test_fcosh,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_EXP),
                 0);
  make_test_1to1("fneg",
                 "fneg",
                 make_test_fneg,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("facos",
                 "facos",
                 make_test_facos,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSTWOTOMINUSONE,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_MINUSONETOZERO,
                        FOP_DATA_ZEROMINUSEPS,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO),
                 0);
  make_test_1to1("fcos",
                 "fcos",
                 make_test_fcos,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_TRIGONOMETRIC),
                 0);
  make_test_1to1("fgetexp",
                 "fgetexp",
                 make_test_fgetexp,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fgetman",
                 "fgetman",
                 make_test_fgetman,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_2to1("fdiv",
                 "fdiv",
                 make_test_fdiv,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fmod",
                 "fmod",
                 make_test_fmod,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fadd",
                 "fadd",
                 make_test_fadd,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fmul",
                 "fmul",
                 make_test_fmul,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsgldiv882",
                 "fsgldiv",
                 make_test_fsgldiv882,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsgldiv060",
                 "fsgldiv",
                 make_test_fsgldiv060,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("frem",
                 "frem",
                 make_test_frem,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fscale",
                 "fscale",
                 make_test_fscale,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsglmul882",
                 "fsglmul",
                 make_test_fsglmul882,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsglmul060",
                 "fsglmul",
                 make_test_fsglmul060,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsub",
                 "fsub",
                 make_test_fsub,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_1to2("fsincos",
                 "fsincos",
                 make_test_fsincos,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_TRIGONOMETRIC),
                 0);
  make_test_2to1("fcmp",
                 "fcmp",
                 make_test_fcmp,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_1to1("ftst",
                 "ftst",
                 make_test_ftst,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  print();
  print("\t.cpu\t68040");
  make_test_1to1("fsmove",
                 "fsmove",
                 make_test_fsmove,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fssqrt",
                 "fssqrt",
                 make_test_fssqrt,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO,
                        FOP_DATA_PLUSTWO,
                        FOP_DATA_PLUSBIG),
                 0);
  make_test_1to1("fdmove",
                 "fdmove",
                 make_test_fdmove,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fdsqrt",
                 "fdsqrt",
                 make_test_fdsqrt,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MINUSONE,
                        FOP_DATA_ZEROPLUSEPS,
                        FOP_DATA_ZEROTOPLUSONE,
                        FOP_DATA_PLUSONE,
                        FOP_DATA_PLUSONETOPLUSTWO,
                        FOP_DATA_PLUSTWO,
                        FOP_DATA_PLUSBIG),
                 0);
  make_test_1to1("fsabs",
                 "fsabs",
                 make_test_fsabs,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fsneg",
                 "fsneg",
                 make_test_fsneg,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fdabs",
                 "fdabs",
                 make_test_fdabs,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_1to1("fdneg",
                 "fdneg",
                 make_test_fdneg,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_ROUNDING),
                 0);
  make_test_2to1("fsdiv",
                 "fsdiv",
                 make_test_fsdiv,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsadd",
                 "fsadd",
                 make_test_fsadd,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fsmul",
                 "fsmul",
                 make_test_fsmul,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fddiv",
                 "fddiv",
                 make_test_fddiv,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fdadd",
                 "fdadd",
                 make_test_fdadd,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fdmul",
                 "fdmul",
                 make_test_fdmul,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fssub",
                 "fssub",
                 make_test_fssub,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  make_test_2to1("fdsub",
                 "fdsub",
                 make_test_fdsub,
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 append(FOP_DATA_SPECIAL,
                        FOP_DATA_MULTIPARAM),
                 0);
  print();
  print("\t.end");
  }



1;
