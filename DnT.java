//========================================================================================
//  DnT.java
//    en:Date and time
//    ja:日時
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  データの種類
//    dttm     long               日時        日付<<32|時刻=西暦年<<42|月<<38|月通日<<32|時<<22|分<<16|秒<<10|ミリ秒
//    csec     int                暦通秒      1970年1月1日0時0分0.000秒から数えた秒数
//    cmil     long               暦通ミリ秒  1970年1月1日0時0分0.000秒から数えたミリ秒数
//    now                         現在
//    date     int                日付        西暦年<<10|月<<6|月通日
//    year     int                西暦年      -1=BC(紀元前)2年,0=BC(紀元前)1年,1=AD(紀元後)1年
//    mont     int   1～12        月          1=1月,…,12=12月
//    mday     int   0～32        月通日      1=1日,…,31=31日。1月0日と12月32日が必要になることがある
//    cday     int                暦通日      1970年1月1日0時0分0.000秒から数えた日数
//    wday     int   0～6         曜日        0=日曜日,1=月曜日,2=火曜日,3=水曜日,4=木曜日,5=金曜日,6=土曜日
//    time     int                時刻        時<<22|分<<16|秒<<10|ミリ秒
//    hour     int   0～23        時          0=0時,…,23=23時
//    minu     int   0～59        分          0=0分,…,59=59分
//    seco     int   0～61        秒          0=0秒,…,59=59秒。うるう秒として60と61が必要になることがある
//    mill     int   0～999       ミリ秒      0=0ミリ秒,…,999=999ミリ秒
//    dsec     int   0～86400     日通秒      0時0分0.000秒から数えた秒数
//    dmil     int   0～86400000  日通ミリ秒  0時0分0.000秒から数えたミリ秒数
//
//    jstdttm  long               日時(日本時間)
//    jstdate  int                日付(日本時間)
//    jstyear  int                西暦年(日本時間)
//    jstmont  int   1～12        月(日本時間)
//    jstmday  int   0～32        月通日(日本時間)
//    lcmont   int   1～12        旧暦の月
//
//    enmont3  String             月の名前(英語3文字)
//    enmont   String             月の名前(英語)
//    enwday3  String             曜日の名前(英語3文字)
//    enwday   String             曜日の名前(英語)
//    jawday1  String             曜日の名前(日本語1文字)
//    jawday   String             曜日の名前(日本語)
//    jaholi   String             日本の祝日の名前(日本語)
//    jalcmont String             旧暦の月の名前(日本語)
//
//  組み合わせ
//      x  実装する
//         dntDateCday,dntCdayYearMontMday,dntTimeDsec,dntTimeDmil以外はインライン展開する
//      .  あってもよいが省略する。最小単位がsecoのものとmillのものを混在させない
//      -  実装しない
//                                  デスティネーション                                         ソース
//    dttm csec cmil date year mont mday cday wday time hour minu seco mill dsec dmil
//      -    x    x    x    x    x    x    x    x    x    x    x    x    x    x    x  Dttm
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  DateTime
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  DateHourMinuSeco
//      x    -    x    -    -    -    -    -    -    -    -    -    -    -    -    -  DateHourMinuSecoMill
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  DateDsec
//      x    .    x    -    -    -    -    -    -    -    -    -    -    -    -    -  DateDmil
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  YearMontMdayTime
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  YearMontMdayHourMinuSeco
//      x    .    x    -    -    -    -    -    -    -    -    -    -    -    -    -  YearMontMdayHourMinuSecoMill
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  YearMontMdayDsec
//      x    .    x    -    -    -    -    -    -    -    -    -    -    -    -    -  YearMontMdayDmil
//      x    x    x    -    -    -    -    -    -    -    -    -    -    -    -    -  CdayTime
//      x    x    .    -    -    -    -    -    -    -    -    -    -    -    -    -  CdayHourMinuSeco
//      x    .    x    -    -    -    -    -    -    -    -    -    -    -    -    -  CdayHourMinuSecoMill
//      x    x    .    -    -    -    -    -    -    -    -    -    -    -    -    -  CdayDsec
//      x    .    x    -    -    -    -    -    -    -    -    -    -    -    -    -  CdayDmil
//      x    -    x    x    x    x    x    x    x    x    x    x    x    -    x    x  Csec
//      x    x    -    x    x    x    x    x    x    x    x    x    x    x    x    x  Cmil
//      x    x    x    x    x    x    x    x    x    x    x    x    x    x    x    x  Now
//      x    x    x    -    x    x    x    x    x    -    -    -    -    -    -    -  Date
//      x    x    x    x    -    -    -    x    x    -    -    -    -    -    -    -  YearMontMday
//      x    x    x    x    x    x    x    -    x    -    -    -    -    -    -    -  Cday
//      -    -    -    -    -    -    -    -    -    -    x    x    x    x    x    x  Time
//      -    -    -    -    -    -    -    -    -    x    -    -    -    -    x    x  HourMinuSeco
//      -    -    -    -    -    -    -    -    -    x    -    -    -    -    x    x  HourMinuSecoMill
//      -    -    -    -    -    -    -    -    -    x    x    x    x    -    -    x  Dsec
//      -    -    -    -    -    -    -    -    -    x    x    x    x    x    x    -  Dmil
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class DnT {

  protected static final String[] DNT_ENMON3_MONT = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
  protected static final String[] DNT_ENMON_MONT = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
  protected static final String[] DNT_ENWDAY3_WDAY = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
  protected static final String[] DNT_ENWDAY_WDAY = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
  protected static final String[] DNT_JAWDAY1_WDAY = { "日", "月", "火", "水", "木", "金", "土" };
  protected static final String[] DNT_JAWDAY_WDAY = { "日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日" };
  protected static final String[] DNT_JALCMONT_LCMONT = { "睦月", "如月", "弥生", "卯月", "皐月", "水無月", "文月", "葉月", "長月", "神無月", "霜月", "師走" };

  //q = dntFdiv (x, y)
  //  床除算
  //  q=floor(x/y)
  //  xとyの符号が同じまたはx/yが割り切れるとき  q=x/y
  //  xとyの符号が違いかつx/yが割り切れないとき  q=x/y-1
  public static int dntFdiv (int x, int y) {
    if (true) {
      return (int) Math.floor ((double) x / (double) y);  //0.026ns。20倍くらい速い
    } else {
      int q = x / y;
      return (x ^ y) >= 0 || x - q * y == 0 ? q : q - 1;  //0.540ns。x*y>=0だと溢れる場合がある。x==0&&y<0のとき(x^y)<0だがx-q*y==0なので問題ない
    }
  }  //dntFdiv(int,int)
  public static long dntFdiv (long x, long y) {
    //return (long) Math.floor ((double) x / (double) y);  //bit数が足りない
    long q = x / y;
    return (x ^ y) >= 0L || x - q * y == 0L ? q : q - 1L;  //x*y>=0Lだと溢れる場合がある。x==0L&&y<0Lのとき(x^y)<0Lだがx-q*y==0Lなので問題ない
  }  //dntFdiv(long,long)
  public static double dntFdiv (double x, double y) {
    return Math.floor (x / y);
  }  //dntFdiv(double,double)

  //r = dntFrem (x, y)
  //  床剰余
  //  r=x-floor(x/y)*y=mod(x,y)
  //  xとyの符号が同じまたはx/yが割り切れるとき  r=x%y
  //  xとyの符号が違いかつx/yが割り切れないとき  r=x%y+y
  public static int dntFrem (int x, int y) {
    if (false) {
      return (int) ((double) x - Math.floor ((double) x / (double) y) * (double) y);  //0.026ns。20倍くらい速い
    } else if (false) {
      double u = (double) x;
      double v = (double) y;
      return (int) (u - Math.floor (u / v) * v);  //0.026ns
    } else {
      int r = x % y;
      return (x ^ y) >= 0 || r == 0 ? r : r + y;  //0.540ns。x*y>=0だと溢れる場合がある。x==0&&y<0のとき(x^y)<0だがr==0なので問題ない
    }
  }  //dntFrem(int,int)
  public static long dntFrem (long x, long y) {
    //return (long) ((double) x - Math.floor ((double) x / (double) y) * (double) y);  //bit数が足りない
    long r = x % y;
    return (x ^ y) >= 0L || r == 0L ? r : r + y;  //x*y>=0Lだと溢れる場合がある。x==0L&&y<0Lのとき(x^y)<0Lだがr==0Lなので問題ない
  }  //dntFrem(long,long)
  public static double dntFrem (double x, double y) {
    return x - Math.floor (x / y) * y;
  }  //dntFrem(double,double)

  //--------------------------------------------------------------------------------
  //dttm 日時

  //dttm = dntDttmDateTime (date, time)
  //  日付と時刻から日時を作る
  public static long dntDttmDateTime (int date, int time) {
    return (long) date << 32 | (long) time & 0xffffffffL;
  }  //dntDttmDateTime(int,int)

  //dttm = dntDttmDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から日時を作る
  public static long dntDttmDateHourMinuSeco (int date, int hour, int minu, int seco) {
    //return dntDttmDateTime (date, dntTimeHourMinuSeco (hour, minu, seco));
    return (long) date << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL;
  }  //dntDttmDateHourMinuSeco(int,int,int,int)

  //dttm = dntDttmDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒とミリ秒から日時を作る
  public static long dntDttmDateHourMinuSecoMill (int date, int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (date, dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return (long) date << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL;
  }  //dntDttmDateHourMinuSecoMill(int,int,int,int,int)

  //dttm = dntDttmDateDsec (date, dsec)
  //  日付と日通秒から日時を作る
  public static long dntDttmDateDsec (int date, int dsec) {
    //return dntDttmDateTime (date, dntTimeDsec (dsec));
    return (long) date << 32 | (long) dntTimeDsec (dsec) & 0xffffffffL;
  }  //dntDttmDateDsec(int,int)

  //dttm = dntDttmDateDmil (date, dmil)
  //  日付と日通ミリ秒から日時を作る
  public static long dntDttmDateDmil (int date, int dmil) {
    //return dntDttmDateTime (date, dntTimeDmil (dmil));
    return (long) date << 32 | (long) dntTimeDmil (dmil) & 0xffffffffL;
  }  //dntDttmDateDmil(int,int)

  //dttm = dntDttmYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から日時を作る
  public static long dntDttmYearMontMdayTime (int year, int mont, int mday, int time) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), time);
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 | (long) time & 0xffffffffL;
  }  //dntDttmYearMontMdayTime(int,int,int,int)

  //dttm = dntDttmYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から日時を作る
  public static long dntDttmYearMontMdayHourMinuSeco (int year, int mont, int mday, int hour, int minu, int seco) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), dntTimeHourMinuSeco (hour, minu, seco));
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL;
  }  //dntDttmYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //dttm = dntDttmYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒とミリ秒から日時を作る
  public static long dntDttmYearMontMdayHourMinuSecoMill (int year, int mont, int mday, int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL;
  }  //dntDttmYearMontMdayHourMinuSecoMill(int,int,int,int,int,int,int)

  //dttm = dntDttmYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から日時を作る
  public static long dntDttmYearMontMdayDsec (int year, int mont, int mday, int dsec) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), dntTimeDsec (dsec));
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 | (long) dntTimeDsec (dsec) & 0xffffffffL;
  }  //dntDttmYearMontMdayDsec(int,int,int,int)

  //dttm = dntDttmYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒から日時を作る
  public static long dntDttmYearMontMdayDmil (int year, int mont, int mday, int dmil) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), dntTimeDmil (dmil));
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 | (long) dntTimeDmil (dmil) & 0xffffffffL;
  }  //dntDttmYearMontMdayDmil(int,int,int,int)

  //dttm = dntDttmCdayTime (cday, time)
  //  歴通日と時刻から日時を作る
  public static long dntDttmCdayTime (int cday, int time) {
    //return dntDttmDateTime (dntDateCday (cday), time);
    return (long) dntDateCday (cday) << 32 | (long) time & 0xffffffffL;
  }  //dntDttmCdayTime(int,int)

  //dttm = dntDttmCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から日時を作る
  public static long dntDttmCdayHourMinuSeco (int cday, int hour, int minu, int seco) {
    //return dntDttmDateTime (dntDateCday (cday), dntTimeHourMinuSeco (hour, minu, seco));
    return (long) dntDateCday (cday) << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL;
  }  //dntDttmCdayHourMinuSeco(int,int,int,int)

  //dttm = dntDttmCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  歴通日と時と分と秒とミリ秒から日時を作る
  public static long dntDttmCdayHourMinuSecoMill (int cday, int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (dntDateCday (cday), dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return (long) dntDateCday (cday) << 32 | (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL;
  }  //dntDttmCdayHourMinuSecoMill(int,int,int,int,int)

  //dttm = dntDttmCdayDsec (cday, dsec)
  //  歴通日と日通秒から日時を作る
  public static long dntDttmCdayDsec (int cday, int dsec) {
    //return dntDttmDateTime (dntDateCday (cday), dntTimeDsec (dsec));
    return (long) dntDateCday (cday) << 32 | (long) dntTimeDsec (dsec) & 0xffffffffL;
  }  //dntDttmCdayDsec(int,int)

  //dttm = dntDttmCdayDmil (cday, dmil)
  //  歴通日と日通ミリ秒から日時を作る
  public static long dntDttmCdayDmil (int cday, int dmil) {
    //return dntDttmDateTime (dntDateCday (cday), dntTimeDmil (dmil));
    return (long) dntDateCday (cday) << 32 | (long) dntTimeDmil (dmil) & 0xffffffffL;
  }  //dntDttmCdayDmil(int,int)

  //dttm = dntDttmCsec (csec)
  //  暦通秒を日時に変換する
  public static long dntDttmCsec (int csec) {
    //return dntDttmDateTime (dntDateCday (dntCdayCsec (csec)), dntTimeDsec (dntDsecCsec (csec)));
    return (long) dntDateCday (dntFdiv (csec, 86400)) << 32 | (long) dntTimeDsec (dntFrem (csec, 86400)) & 0xffffffffL;
  }  //dntDttmCsec(int)

  //dttm = dntDttmCmil (cmil)
  //  暦通ミリ秒を日時に変換する
  public static long dntDttmCmil (long cmil) {
    //return dntDttmDateTime (dntDateCday (dntCdayCmil (cmil)), dntTimeDmil (dntDmilCmil (cmil)));
    return (long) dntDateCday ((int) dntFdiv (cmil, 86400000L)) << 32 | (long) dntTimeDmil ((int) dntFrem (cmil, 86400000L)) & 0xffffffffL;
  }  //dntDttmCmil(long)

  //dttm = dntDttmNow ()
  //  現在の日時を返す
  public static long dntDttmNow () {
    //return dntDttmCmil (System.currentTimeMillis ());
    long cmil = System.currentTimeMillis ();
    return (long) dntDateCday ((int) dntFdiv (cmil, 86400000L)) << 32 | (long) dntTimeDsec ((int) dntFrem (cmil, 86400000L)) & 0xffffffffL;
  }  //dntDttmNow()

  //dttm = dntDttmDate (date)
  //  日付から日時を作る
  public static long dntDttmDate (int date) {
    //return dntDttmDateTime (date, 0);
    return (long) date << 32;
  }  //dntDttmDate(int)

  //dttm = dntDttmYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から日時を作る
  public static long dntDttmYearMontMday (int year, int mont, int mday) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday), 0);
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32;
  }  //dntDttmYearMontMday(int,int,int)

  //dttm = dntDttmCday (cday)
  //  歴通日から日時を作る
  public static long dntDttmCday (int cday) {
    //return dntDttmDateTime (dntDateCday (cday), 0);
    return (long) dntDateCday (cday) << 32;
  }  //dntDttmCday(int)

  //--------------------------------------------------------------------------------
  //csec 暦通秒

  //csec = dntCsecDttm (dttm)
  //  日時から暦通秒を求める
  public static int dntCsecDttm (long dttm) {
    //return dntCsecDateTime (dntDateDttm (dttm), dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400 + (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntCsecDttm(long)

  //csec = dntCsecDateTime (date, time)
  //  日付と時刻から暦通秒を求める
  public static int dntCsecDateTime (int date, int time) {
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400 + (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntCsecDateTime(int,int)

  //csec = dntCsecDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から暦通秒を求める
  public static int dntCsecDateHourMinuSeco (int date, int hour, int minu, int seco) {
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + dntDsecHourMinuSeco (hour, minu, seco);
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400 + hour * 3600 + minu * 60 + seco;
  }  //dntCsecDateHourMinuSeco(int,int,int,int)

  //csec = dntCsecDateDsec (date, dsec)
  //  日付と日通秒から暦通秒を求める
  public static int dntCsecDateDsec (int date, int dsec) {
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + dsec;
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400 + dsec;
  }  //dntCsecDateDsec(int,int)

  //csec = dntCsecYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から暦通秒を求める
  public static int dntCsecYearMontMdayTime (int year, int mont, int mday, int time) {
    //return dntCsecCday (dntCdayYearMontMday (year, mont, mday)) + dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntCdayYearMontMday (year, mont, mday) * 86400 + (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntCsecYearMontMdayTime(int,int,int,int)

  //csec = dntCsecYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から暦通秒を求める
  public static int dntCsecYearMontMdayHourMinuSeco (int year, int mont, int mday, int hour, int minu, int seco) {
    //return dntCsecCday (dntCdayYearMontMday (year, mont, mday)) + dntDsecHourMinuSeco (hour, minu, seco);
    return dntCdayYearMontMday (year, mont, mday) * 86400 + hour * 3600 + minu * 60 + seco;
  }  //dntCsecYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //csec = dntCsecYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から暦通秒を求める
  public static int dntCsecYearMontMdayDsec (int year, int mont, int mday, int dsec) {
    //return dntCsecCday (dntCdayYearMontMday (year, mont, mday)) + dsec;
    return dntCdayYearMontMday (year, mont, mday) * 86400 + dsec;
  }  //dntCsecYearMontMdayDsec(int,int,int,int)

  //csec = dntCsecCdayTime (cday, time)
  //  歴通日と時刻から暦通秒を求める
  public static int dntCsecCdayTime (int cday, int time) {
    //return dntCsecCday (cday) + dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return cday * 86400 + (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntCsecCdayTime(int,int)

  //csec = dntCsecCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から暦通秒を求める
  public static int dntCsecCdayHourMinuSeco (int cday, int hour, int minu, int seco) {
    //return dntCsecCday (cday) + dntDsecHourMinuSeco (hour, minu, seco);
    return cday * 86400 + hour * 3600 + minu * 60 + seco;
  }  //dntCsecCdayHourMinuSeco(int,int,int,int)

  //csec = dntCsecCdayDsec (cday, dsec)
  //  暦通日と日通秒を暦通秒に変換する
  public static int dntCsecCdayDsec (int cday, int dsec) {
    //return dntCsecCday (cday) + dsec;
    return cday * 86400 + dsec;
  }  //dntCsecCdayDsec(int,int)

  //csec = dntCsecCmil (cmil)
  //  暦通ミリ秒を暦通秒に変換する
  public static int dntCsecCmil (long cmil) {
    return (int) dntFdiv (cmil, 1000L);
  }  //dntCsecCmil(long)

  //csec = dntCsecNow ()
  //  現在の暦通秒を返す
  public static int dntCsecNow () {
    //return dntCsecCmil (dntCmilNow ());
    return (int) dntFdiv (System.currentTimeMillis (), 1000L);
  }  //dntCsecNow()

  //csec = dntCsecDate (date)
  //  日付から暦通秒を求める
  public static int dntCsecDate (int date) {
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400;
  }  //dntCsecDate(int)

  //csec = dntCsecYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通秒を求める
  public static int dntCsecYearMontMday (int year, int mont, int mday) {
    //return dntCsecCday (dntCdayYearMontMday (year, mont, mday));
    return dntCdayYearMontMday (year, mont, mday) * 86400;
  }  //dntCsecYearMontMday(int,int,int)

  //csec = dntCsecCday (cday)
  //  暦通日を暦通秒に変換する
  public static int dntCsecCday (int cday) {
    return cday * 86400;
  }  //dntCsecCday(int)

  //--------------------------------------------------------------------------------
  //cmil 暦通ミリ秒

  //cmil = dntCmilDttm (dttm)
  //  日時から暦通ミリ秒を求める
  public static long dntCmilDttm (long dttm) {
    //return dntCmilDateTime (dntDateDttm (dttm), dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023));
  }  //dntCmilDttm(long)

  //cmil = dntCmilDateTime (date, time)
  //  日付と時刻から暦通ミリ秒を求める
  public static long dntCmilDateTime (int date, int time) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023));
  }  //dntCmilDateTime(int,int)

  //cmil = dntCmilDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilDateHourMinuSeco (int date, int hour, int minu, int seco) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dmilHourMinuSeco (hour, minu, seco);
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000);
  }  //dntCmilDateHourMinuSeco(int,int,int,int)

  //cmil = dntCmilDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilDateHourMinuSecoMill (int date, int hour, int minu, int seco, int mill) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill);
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill);
  }  //dntCmilDateHourMinuSecoMill(int,int,int,int,int)

  //cmil = dntCmilDateDsec (date, dsec)
  //  日付と日通秒から暦通ミリ秒を求める
  public static long dntCmilDateDsec (int date, int dsec) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dntDmilDsec (dsec);
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) (dsec * 1000);
  }  //dntCmilDateDsec(int,int)

  //cmil = dntCmilDateDmil (date, dmil)
  //  日付と日通ミリ秒から暦通ミリ秒を求める
  public static long dntCmilDateDmil (int date, int dmil) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) + (long) dmil;
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L + (long) dmil;
  }  //dntCmilDateDmil(int,int)

  //cmil = dntCmilYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayTime (int year, int mont, int mday, int time) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday)) + (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L + (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023));
  }  //dntCmilYearMontMdayTime(int,int,int,int)

  //cmil = dntCmilYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayHourMinuSeco (int year, int mont, int mday, int hour, int minu, int seco) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday)) + (long) dmilHourMinuSeco (hour, minu, seco);
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000);
  }  //dntCmilYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //cmil = dntCmilYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayHourMinuSecoMill (int year, int mont, int mday, int hour, int minu, int seco, int mill) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday)) + (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill);
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill);
  }  //dntCmilYearMontMdayHourMinuSecoMill(int,int,int,int,int,int,int)

  //cmil = dntCmilYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayDsec (int year, int mont, int mday, int dsec) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday)) + (long) dntDmilDsec (dsec);
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L + (long) (dsec * 1000);
  }  //dntCmilYearMontMdayDsec(int,int,int,int)

  //cmil = dntCmilYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayDmil (int year, int mont, int mday, int dmil) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday)) + (long) dmil;
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L + (long) dmil;
  }  //dntCmilYearMontMdayDmil(int,int,int,int)

  //cmil = dntCmilCdayTime (cday, time)
  //  歴通日と時刻から暦通ミリ秒を求める
  public static long dntCmilCdayTime (int cday, int time) {
    //return dntCmilCday (cday) + (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (long) cday * 86400000L + (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023));
  }  //dntCmilCdayTime(int,int)

  //cmil = dntCmilCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilCdayHourMinuSeco (int cday, int hour, int minu, int seco) {
    //return dntCmilCday (cday) + (long) dntDmilHourMinuSecoMill (hour, minu, seco);
    return (long) cday * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000);
  }  //dntCmilCdayHourMinuSeco(int,int,int,int)

  //cmil = dntCmilCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  歴通日と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilCdayHourMinuSecoMill (int cday, int hour, int minu, int seco, int mill) {
    //return dntCmilCday (cday) + (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill);
    return (long) cday * 86400000L + (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill);
  }  //dntCmilCdayHourMinuSecoMill(int,int,int,int,int)

  //cmil = dntCmilCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を暦通ミリ秒に変換する
  public static long dntCmilCdayDmil (int cday, int dmil) {
    //return dntCmilCday (cday) + (long) dmil;
    return (long) cday * 86400000L + (long) dmil;
  }  //dntCmilCdayDmil(int,int)

  //cmil = dntCmilCsec (csec)
  //  暦通秒を暦通ミリ秒に変換する
  public static long dntCmilCsec (int csec) {
    return (long) csec * 1000L;
  }  //dntCmilCsec(int)

  //cmil = dntCmilNow ()
  //  現在の暦通ミリ秒を返す
  public static long dntCmilNow () {
    return System.currentTimeMillis ();
  }  //dntCmilNow()

  //cmil = dntCmilDate (date)
  //  日付から暦通ミリ秒を求める
  public static long dntCmilDate (int date) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L;
  }  //dntCmilDate(int)

  //cmil = dntCmilYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通ミリ秒を求める
  public static long dntCmilYearMontMday (int year, int mont, int mday) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday));
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L;
  }  //dntCmilYearMontMday(int,int,int)

  //cmil = dntCmilCday (cday)
  //  暦通日を暦通ミリ秒に変換する
  public static long dntCmilCday (int cday) {
    return (long) cday * 86400000L;
  }  //dntCmilCday(int)

  //--------------------------------------------------------------------------------
  //date 日付

  //date = dntDateDttm (dttm)
  //  日時から日付を取り出す
  public static int dntDateDttm (long dttm) {
    return (int) (dttm >> 32);
  }  //dntDateDttm(long)

  //date = dntDateCsec (csec)
  //  歴通秒から日付を求める
  public static int dntDateCsec (int csec) {
    //return dntDateCday (dntCdayCsec (csec));
    return dntDateCday (dntFdiv (csec, 86400));
  }  //dntDateCsec(int)

  //date = dntDateCmil (cmil)
  //  歴通ミリ秒から日付を求める
  public static int dntDateCmil (long cmil) {
    //return dntDateCday (dntCdayCmil (cmil));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L));
  }  //dntDateCmil(long)

  //date = dntDateNow ()
  //  現在の日付を返す
  public static int dntDateNow () {
    //return dntDateCday (dntCdayCmil (dntCmilNow ()));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L));
  }  //dntDateNow()

  //date = dntDateYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から日付を作る
  public static int dntDateYearMontMday (int year, int mont, int mday) {
    return year << 10 | (mont & 15) << 6 | mday & 63;
  }  //dntDateYearMontMday(int,int,int)

  //date = dntDateCday (cday)
  //  暦通日から日付を求める
  public static int dntDateCday (int cday) {
    int y400;
    int y100;
    if (cday < -141427) {  //1582年10月4日までユリウス暦
      cday += 719470;
      y400 = 0;
      y100 = 0;
    } else {  //1582年10月15日からグレゴリオ暦
      cday += 719468;
      y400 = dntFdiv (cday, 146097);
      cday -= 146097 * y400;
      y100 = dntFdiv (cday, 36524);
      if (y100 > 3) {  //2000年2月29日の処理に必要
        y100 = 3;
      }
      cday -= 36524 * y100;
    }
    int y4 = dntFdiv (cday, 1461);
    cday -= 1461 * y4;
    int y1 = dntFdiv (cday, 365);
    if (y1 > 3) {  //2004年2月29日の処理に必要
      y1 = 3;
    }
    cday = 10 * cday - 3650 * y1 + 922;
    int year = 400 * y400 + 100 * y100 + 4 * y4 + y1;
    int mont = dntFdiv (cday, 306);
    int mday = dntFdiv (cday - 306 * mont, 10) + 1;
    if (mont > 12) {
      year++;
      mont -= 12;
    }
    return dntDateYearMontMday (year, mont, mday);
  }  //dntDateCday(int)

  //--------------------------------------------------------------------------------
  //year 西暦年

  //year = dntYearDttm (dttm)
  //  日時から西暦年を取り出す
  public static int dntYearDttm (long dttm) {
    return (int) (dttm >> 42);
  }  //dntYearDttm(long)

  //year = dntYearCsec (csec)
  //  歴通秒から西暦年を求める
  public static int dntYearCsec (int csec) {
    //return dntYearDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday (dntFdiv (csec, 86400)) >> 10;
  }  //dntYearCsec(int)

  //year = dntYearCmil (cmil)
  //  歴通ミリ秒から西暦年を求める
  public static int dntYearCmil (long cmil) {
    //return dntYearDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) >> 10;
  }  //dntYearCmil(long)

  //year = dntYearNow ()
  //  現在の西暦年を返す
  public static int dntYearNow () {
    //return dntYearDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) >> 10;
  }  //dntYearNow()

  //year = dntYearDate (date)
  //  日付から西暦年を取り出す
  public static int dntYearDate (int date) {
    return date >> 10;
  }  //dntYearDate(int)

  //year = dntYearCday (cday)
  //  歴通日から西暦年を求める
  public static int dntYearCday (int cday) {
    //return dntYearDate (dntDateCday (cday));
    return dntDateCday (cday) >> 10;
  }  //dntYearCday(int)

  //--------------------------------------------------------------------------------
  //mont 月

  //mont = dntMontDttm (dttm)
  //  日時から月を取り出す
  public static int dntMontDttm (long dttm) {
    return (int) (dttm >> 38) & 15;
  }  //dntMontDttm(long)

  //mont = dntMontCsec (csec)
  //  歴通秒から月を求める
  public static int dntMontCsec (int csec) {
    //return dntMontDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday (dntFdiv (csec, 86400)) >> 6 & 15;
  }  //dntMontCsec(int)

  //mont = dntMontCmil (cmil)
  //  歴通ミリ秒から月を求める
  public static int dntMontCmil (long cmil) {
    //return dntMontDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) >> 6 & 15;
  }  //dntMontCmil(long)

  //mont = dntMontNow ()
  //  現在の月を返す
  public static int dntMontNow () {
    //return dntMontDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) >> 6 & 15;
  }  //dntMontNow()

  //mont = dntMontDate (date)
  //  日付から月を取り出す
  public static int dntMontDate (int date) {
    return date >> 6 & 15;
  }  //dntMontDate(int)

  //mont = dntMontCday (cday)
  //  歴通日から月を求める
  public static int dntMontCday (int cday) {
    //return dntMontDate (dntDateCday (cday));
    return dntDateCday (cday) >> 6 & 15;
  }  //dntMontCday(int)

  //--------------------------------------------------------------------------------
  //mday 月通日

  //mday = dntMdayDttm (dttm)
  //  日時から月通日を取り出す
  public static int dntMdayDttm (long dttm) {
    return (int) (dttm >> 32) & 63;
  }  //dntMdayDttm(long)

  //mday = dntMdayCsec (csec)
  //  歴通秒から月通日を求める
  public static int dntMdayCsec (int csec) {
    //return dntMdayDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday (dntFdiv (csec, 86400)) & 63;
  }  //dntMdayCsec(int)

  //mday = dntMdayCmil (cmil)
  //  歴通ミリ秒から月通日を求める
  public static int dntMdayCmil (long cmil) {
    //return dntMdayDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) & 63;
  }  //dntMdayCmil(long)

  //mday = dntMdayNow ()
  //  現在の月通日を返す
  public static int dntMdayNow () {
    //return dntMdayDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) & 63;
  }  //dntMdayNow()

  //mday = dntMdayDate (date)
  //  日付から月通日を取り出す
  public static int dntMdayDate (int date) {
    return date & 63;
  }  //dntMdayDate(int)

  //mday = dntMdayCday (cday)
  //  歴通日から月通日を求める
  public static int dntMdayCday (int cday) {
    //return dntMdayDate (dntDateCday (cday));
    return dntDateCday (cday) & 63;
  }  //dntMdayCday(int)

  //--------------------------------------------------------------------------------
  //cday 暦通日

  //cday = dntCdayDttm (dttm)
  //  日時から暦通日を求める
  public static int dntCdayDttm (long dttm) {
    //return dntCdayDate (dntDateDttm (dttm));
    int date = (int) (dttm >> 32);
    //return dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntCdayDttm(long)

  //cday = dntCdayCsec (csec)
  //  暦通秒を暦通日に変換する
  public static int dntCdayCsec (int csec) {
    return dntFdiv (csec, 86400);
  }  //dntCdayCsec(int)

  //cday = dntCdayCmil (cmil)
  //  暦通ミリ秒を暦通日に変換する
  public static int dntCdayCmil (long cmil) {
    return (int) dntFdiv (cmil, 86400000L);
  }  //dntCdayCmil(long)

  //cday = dntCdayNow ()
  //  現在の暦通日を返す
  public static int dntCdayNow () {
    //return dntCdayCmil (dntCmilNow ());
    return (int) dntFdiv (dntCmilNow (), 86400000L);
  }  //dntCdayNow()

  //cday = dntCdayDate (date)
  //  日付から暦通日を求める
  public static int dntCdayDate (int date) {
    //return dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntCdayDate(int)

  //cday = dntCdayYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通日を求める
  //  日付から年通日を求めるときに1月0日の暦通日を使うので0日の入力をエラーにしないこと
  public static int dntCdayYearMontMday (int year, int mont, int mday) {
    if (mont < 3) {  //1月と2月を前年の13月と14月として処理する
      year--;
      mont += 12;
    }
    int cday = (int) Math.floor (365.25 * (double) year) + (int) Math.floor (30.59 * (double) (mont - 2)) + mday - 719501;  //ユリウス暦と見なして暦通日に変換する
    return cday < -141417 ? cday : cday + dntFdiv (year, 400) - dntFdiv (year, 100) + 2;  //グレゴリオ暦のときは補正を加える
  }  //dntCdayYearMontMday(int,int,int)

  //--------------------------------------------------------------------------------
  //wday 曜日

  //wday = dntWdayDttm (dttm)
  //  日時から曜日を求める
  public static int dntWdayDttm (long dttm) {
    //return dntWdayDate (dntDateDttm (dttm));
    int date = (int) (dttm >> 32);
    //return dntWdayCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return dntFrem (dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) + 4, 7);
  }  //dntWdayDttm(long)

  //wday = dntWdayCsec (csec)
  //  暦通秒から曜日を求める
  public static int dntWdayCsec (int csec) {
    //return dntWdayCday (dntCdayCsec (csec));
    return dntFrem (dntFdiv (csec, 86400) + 4, 7);
  }  //dntWdayCsec(int)

  //wday = dntWdayCmil (cmil)
  //  暦通ミリ秒から曜日を求める
  public static int dntWdayCmil (long cmil) {
    //return dntWdayCday (dntCdayCmil (cmil));
    return dntFrem ((int) dntFdiv (cmil, 86400000L) + 4, 7);
  }  //dntWdayCmil(long)

  //wday = dntWdayNow ()
  //  現在の曜日を返す
  public static int dntWdayNow () {
    //return dntWdayCday (dntCdayCmil (dntCmilNow ()));
    return dntFrem ((int) dntFdiv (System.currentTimeMillis (), 86400000L) + 4, 7);
  }  //dntWdayNow()

  //wday = dntWdayDate (date)
  //  日付から曜日を求める
  public static int dntWdayDate (int date) {
    //return dntWdayCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return dntFrem (dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) + 4, 7);
  }  //dntWdayDate(int)

  //wday = dntWdayYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から曜日を求める
  public static int dntWdayYearMontMday (int year, int mont, int mday) {
    return dntFrem (dntCdayYearMontMday (year, mont, mday) + 4, 7);
  }  //dntWdayYearMontMday(int,int,int)

  //wday = dntWdayCday (cday)
  //  暦通日から曜日を求める
  public static int dntWdayCday (int cday) {
    return dntFrem (cday + 4, 7);
  }  //dntWdayCday(int)

  //--------------------------------------------------------------------------------
  //time 時

  //time = dntTimeDttm (dttm)
  //  日時から時刻を取り出す
  public static int dntTimeDttm (long dttm) {
    return (int) dttm;
  }  //dntTimeDttm(long)

  //time = dntTimeCsec (csec)
  //  歴通秒を時刻に変換する
  public static int dntTimeCsec (int csec) {
    //return dntTimeDsec (dntDsecCsec (csec));
    return dntTimeDsec (dntFrem (csec, 86400));
  }  //dntTimeCsec(int)

  //time = dntTimeCmil (cmil)
  //  歴通ミリ秒を時刻に変換する
  public static int dntTimeCmil (long cmil) {
    //return dntTimeDmil (dntDmilCmil (cmil));
    return dntTimeDmil ((int) dntFrem (cmil, 86400000L));
  }  //dntTimeCmil(long)

  //time = dntTimeNow ()
  //  現在の時刻を返す
  public static int dntTimeNow () {
    //return dntTimeDmil (dntDmilCmil (dntCmilNow ()));
    return dntTimeDmil ((int) dntFrem (System.currentTimeMillis (), 86400000L));
  }  //dntTimeNow()

  //time = dntTimeHourMinuSeco (hour, minu, seco)
  //  時と分と秒から時刻を作る
  public static int dntTimeHourMinuSeco (int hour, int minu, int seco) {
    return hour << 22 | (minu & 63) << 16 | (seco & 63) << 10;
  }  //dntTimeHourMinuSeco(int,int,int)

  //time = dntTimeHourMinuSecoMill (hour, minu, seco, mill)
  //  時と分と秒とミリ秒から時刻を作る
  public static int dntTimeHourMinuSecoMill (int hour, int minu, int seco, int mill) {
    return hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023;
  }  //dntTimeHourMinuSecoMill(int,int,int,int)

  //time = dntTimeDsec (dsec)
  //  日通秒を時刻に変換する
  public static int dntTimeDsec (int dsec) {
    int hour = dntFdiv (dsec, 3600);
    dsec -= hour * 3600;
    int minu = dntFdiv (dsec, 60);
    dsec -= minu * 60;
    return dntTimeHourMinuSeco (hour, minu, dsec);
  }  //dntTimeDsec(int)

  //time = dntTimeDmil (dmil)
  //  日通ミリ秒を時刻に変換する
  public static int dntTimeDmil (int dmil) {
    int hour = dntFdiv (dmil, 3600000);
    dmil -= hour * 3600000;
    int minu = dntFdiv (dmil, 60000);
    dmil -= minu * 60000;
    int seco = dntFdiv (dmil, 1000);
    dmil -= seco * 1000;
    return dntTimeHourMinuSecoMill (hour, minu, seco, dmil);
  }  //dntTimeDmil(int)

  //--------------------------------------------------------------------------------
  //hour 時

  //hour = dntHourDttm (dttm)
  //  日時から時を取り出す
  public static int dntHourDttm (long dttm) {
    return (int) dttm >> 22;
  }  //dntHourDttm(long)

  //hour = dntHourCsec (csec)
  //  歴通秒から時を求める
  public static int dntHourCsec (int csec) {
    //return dntFrem (csec, 86400) / 3600;
    //perl optdiv.pl 86399 3600
    //  x/3600==x*37283>>>27 (0<=x<=125998) [86399*37283==3221213917]
    return dntFrem (csec, 86400) * 37283 >>> 27;
  }  //dntHourCsec(int)

  //hour = dntHourCmil (cmil)
  //  歴通ミリ秒から時を求める
  public static int dntHourCmil (long cmil) {
    //return (int) dntFrem (cmil, 86400000L) / 3600000;
    //perl optdiv.pl 86399999 3600000
    //  x/3600000==x*39093747>>>47 (0<=x<=169199998) [86399999*39093747==3377699701706253]
    return (int) (dntFrem (cmil, 86400000L) * 39093747L >>> 47);
  }  //dntHourCmil(long)

  //hour = dntHourNow ()
  //  現在の時を返す
  public static int dntHourNow () {
    //return dntHourCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 86400000L) / 3600000;
    //perl optdiv.pl 86399999 3600000
    //  x/3600000==x*39093747>>>47 (0<=x<=169199998) [86399999*39093747==3377699701706253]
    return (int) (dntFrem (System.currentTimeMillis (), 86400000L) * 39093747L >>> 47);
  }  //dntHourNow()

  //hour = dntHourTime (time)
  //  時刻から時を取り出す
  public static int dntHourTime (int time) {
    return time >> 22;
  }  //dntHourTime(int)

  //hour = dntHourDsec (dsec)
  //  日通秒から時を求める
  public static int dntHourDsec (int dsec) {
    return dntFdiv (dsec, 3600);
  }  //dntHourDsec(int)

  //hour = dntHourDmil (dmil)
  //  日通ミリ秒から時を求める
  public static int dntHourDmil (int dmil) {
    return dntFdiv (dmil, 3600000);
  }  //dntHourDmil(int)

  //--------------------------------------------------------------------------------
  //minu 分

  //minu = dntMinuDttm (dttm)
  //  日時から分を取り出す
  public static int dntMinuDttm (long dttm) {
    return (int) dttm >> 16 & 63;
  }  //dntMinuDttm(long)

  //minu = dntMinuCsec (csec)
  //  歴通秒から分を求める
  public static int dntMinuCsec (int csec) {
    //return dntFrem (csec, 3600) / 60;
    //perl optdiv.pl 3599 60
    //  x/60==x*2185>>>17 (0<=x<=4738) [3599*2185==7863815]
    return dntFrem (csec, 3600) * 2185 >>> 17;
  }  //dntMinuCsec(int)

  //minu = dntMinuCmil (cmil)
  //  歴通ミリ秒から分を求める
  public static int dntMinuCmil (long cmil) {
    //return (int) dntFrem (cmil, 3600000L) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) (dntFrem (cmil, 3600000L) * 4581299L >>> 38);
  }  //dntMinuCmil(long)

  //minu = dntMinuNow ()
  //  現在の分を返す
  public static int dntMinuNow () {
    //return dntMinuCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 3600000L) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) (dntFrem (System.currentTimeMillis (), 3600000L) * 4581299L >>> 38);
  }  //dntMinuNow()

  //minu = dntMinuTime (time)
  //  時刻から分を取り出す
  public static int dntMinuTime (int time) {
    return time >> 16 & 31;
  }  //dntMinuTime(int)

  //minu = dntMinuDsec (dsec)
  //  日通秒から分を求める
  public static int dntMinuDsec (int dsec) {
    //perl optdiv.pl 3599 60
    //  x/60==x*2185>>>17 (0<=x<=4738) [3599*2185==7863815]
    //return dntFrem (dsec, 3600) / 60;
    return dntFrem (dsec, 3600) * 2185 >>> 17;
  }  //dntMinuDsec(int)

  //minu = dntMinuDmil (dmil)
  //  日通ミリ秒から分を求める
  public static int dntMinuDmil (int dmil) {
    //return dntFrem (dmil, 3600000) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) ((long) dntFrem (dmil, 3600000) * 4581299L >>> 38);
  }  //dntMinuDmil(int)

  //--------------------------------------------------------------------------------
  //seco 秒

  //seco = dntSecoDttm (dttm)
  //  日時から秒を取り出す
  public static int dntSecoDttm (long dttm) {
    return (int) dttm >> 10 & 63;
  }  //dntSecoDttm(long)

  //seco = dntSecoCsec (csec)
  //  歴通秒から秒を求める
  public static int dntSecoCsec (int csec) {
    return dntFrem (csec, 60);
  }  //dntSecoCsec(int)

  //seco = dntSecoCmil (cmil)
  //  歴通ミリ秒から秒を求める
  public static int dntSecoCmil (long cmil) {
    //return (int) dntFrem (cmil, 60000L) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return (int) dntFrem (cmil, 60000L) * 67109 >>> 26;
  }  //dntSecoCmil(long)

  //seco = dntSecoNow ()
  //  現在の秒を返す
  public static int dntSecoNow () {
    //return dntSecoCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 60000L) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return (int) dntFrem (System.currentTimeMillis (), 60000L) * 67109 >>> 26;
  }  //dntSecoNow()

  //seco = dntSecoTime (time)
  //  時刻から秒を取り出す
  public static int dntSecoTime (int time) {
    return time >> 10 & 63;
  }  //dntSecoTime(int)

  //seco = dntSecoDsec (dsec)
  //  日通秒から秒を求める
  public static int dntSecoDsec (int dsec) {
    return dntFrem (dsec, 60);
  }  //dntSecoDsec(int)

  //seco = dntSecoDmil (dmil)
  //  日通ミリ秒から秒を求める
  public static int dntSecoDmil (int dmil) {
    //return dntFrem (dmil, 60000) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return dntFrem (dmil, 60000) * 67109 >>> 26;
  }  //dntSecoDmil(int)

  //--------------------------------------------------------------------------------
  //mill ミリ秒

  //mill = dntMillDttm (dttm)
  //  日時からミリ秒を取り出す
  public static int dntMillDttm (long dttm) {
    return (int) dttm & 1023;
  }  //dntMillDttm(long)

  //mill = dntMillCmil (cmil)
  //  歴通ミリ秒からミリ秒を求める
  public static int dntMillCmil (long cmil) {
    return (int) dntFrem (cmil, 1000L);
  }  //dntMillCmil(long)

  //mill = dntMillNow ()
  //  現在のミリ秒を返す
  public static int dntMillNow () {
    //return dntMillCmil (dntCmilNow ());
    return (int) dntFrem (System.currentTimeMillis (), 1000L);
  }  //dntMillNow()

  //mill = dntMillTime (time)
  //  時刻からミリ秒を取り出す
  public static int dntMillTime (int time) {
    return time & 1023;
  }  //dntMillTime(int)

  //mill = dntMillDmil (dmil)
  //  日通ミリ秒からミリ秒を求める
  public static int dntMillDmil (int dmil) {
    return dntFrem (dmil, 1000);
  }  //dntMillDmil(int)

  //--------------------------------------------------------------------------------
  //dsec 日通秒

  //dsec = dntDsecDttm (dttm)
  //  日時から日通秒を求める
  public static int dntDsecDttm (long dttm) {
    //return dntDsecHourMinuSeco (dntHourDttm (dttm), dntMinuDttm (dttm), dntSecoDttm (dttm));
    return ((int) dttm >> 22) * 3600 + ((int) dttm >> 16 & 63) * 60 + ((int) dttm >> 10 & 63);
  }  //dntDsecDttm(long)

  //dsec = dntDsecCsec (csec)
  //  暦通秒から日通秒を求める
  public static int dntDsecCsec (int csec) {
    return dntFrem (csec, 86400);
  }  //dntDsecCsec(int)

  //dsec = dntDsecCmil (cmil)
  //  暦通ミリ秒から日通秒を求める
  public static int dntDsecCmil (long cmil) {
    //return dntDsecCsec (dntCsecCmil (cmil));
    return dntFrem ((int) dntFdiv (cmil, 1000L), 86400);
  }  //dntDsecCmil(long)

  //dsec = dntDsecNow ()
  //  現在の日通秒を返す
  public static int dntDsecNow () {
    //return dntDsecCsec (dntCsecCmil (dntCmilNow ()));
    return dntFrem ((int) dntFdiv (System.currentTimeMillis (), 1000L), 86400);
  }  //dntDsecNow()

  //dsec = dntDsecTime (time)
  //  時刻を日通秒に変換する
  public static int dntDsecTime (int time) {
    //return dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntDsecTime(int)

  //dsec = dntDsecHourMinuSeco (hour, minu, seco)
  //  時と分と秒を日通秒に変換する
  public static int dntDsecHourMinuSeco (int hour, int minu, int seco) {
    return hour * 3600 + minu * 60 + seco;
  }  //dntDsecHourMinuSeco(int,int,int)

  //dsec = dntDsecDmil (dmil)
  //  日通ミリ秒を日通秒に変換する
  public static int dntDsecDmil (int dmil) {
    return dntFdiv (dmil, 1000);
  }  //dntDsecDmil(int)

  //--------------------------------------------------------------------------------
  //dmil 日通ミリ秒

  //dmil = dntDmilDttm (dttm)
  //  日時から日通ミリ秒を求める
  public static int dntDmilDttm (long dttm) {
    //return dntDmilHourMinuSecoMill (dntHourDttm (dttm), dntMinuDttm (dttm), dntSecoDttm (dttm), dntMillDttm (dttm));
    return ((int) dttm >> 22) * 3600 + ((int) dttm >> 16 & 63) * 60 + ((int) dttm >> 10 & 63) + ((int) dttm & 1023);
  }  //dntDmilDttm(long)

  //dmil = dntDmilCsec (csec)
  //  暦通秒から日通ミリ秒を求める
  public static int dntDmilCsec (int csec) {
    //return dntDmilDsec (dntDsecCsec (csec));
    return dntFrem (csec, 86400) * 1000;
  }  //dntDmilCsec(int)

  //dmil = dntDmilCmil (cmil)
  //  暦通ミリ秒から日通ミリ秒を取り出す
  public static int dntDmilCmil (long cmil) {
    return (int) dntFrem (cmil, 86400000L);
  }  //dntDmilCmil(long)

  //dmil = dntDmilTime (time)
  //  時刻を日通ミリ秒に変換する
  public static int dntDmilTime (int time) {
    //return dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63) + (time & 1023);
  }  //dntDmilTime(int)

  //dmil = dntDmilHourMinuSecoMill (hour, minu, seco, mill)
  //  時と分と秒とミリ秒を日通ミリ秒に変換する
  public static int dntDmilHourMinuSecoMill (int hour, int minu, int seco, int mill) {
    return hour * 3600000 + minu * 60000 + seco * 1000 + mill;
  }  //dntDmilHourMinuSecoMill(int,int,int,int)

  //dmil = dntDmilDsec (dsec)
  //  日通秒を日通ミリ秒に変換する
  public static int dntDmilDsec (int dsec) {
    return dsec * 1000;
  }  //dntDmilDsec(int)

  //--------------------------------------------------------------------------------
  //jaholi 日本の祝日の名前(日本語)
  //
  //    国民の祝日  国民の祝日に関する法律で定められた日
  //    国民の休日  前日が国民の祝日で翌日も国民の祝日である国民の祝日ではない日
  //    振替休日    直前の日曜日から前日まで国民の祝日が続いている国民の祝日ではない日
  //
  //    1月1日            1949年～        元日 (New Year's Day)
  //    1月2日(月)        1974年～        振替休日 (transfer holiday) (元日が日曜日のとき)
  //    1月第2月曜日      2000年～        成人の日 (Coming of Age Day)
  //    1月15日           1949年～1999年  成人の日 (Coming of Age Day)
  //    1月16日(月)       1974年～1999年  振替休日 (transfer holiday) (成人の日が日曜日のとき)
  //    2月11日           1967年～        建国記念の日 (National Foundation Day)
  //    2月12日(月)       1974年～        振替休日 (transfer holiday) (建国記念の日が日曜日のとき)
  //    3月21日頃         1949年～        春分の日 (Vernal Equinox Day)
  //    3月22日頃(月)     1974年～        振替休日 (transfer holiday) (春分の日が日曜日のとき)
  //    4月29日           1949年～1988年  天皇誕生日 (The Emperor's Birthday)
  //                      1989年～2006年  みどりの日 (Greenery Day)
  //                      2007年～        昭和の日 (Showa Day)
  //    4月30日(月)       1973年～        振替休日 (transfer holiday) (天皇誕生日,みどりの日,昭和の日が日曜日のとき)
  //    5月3日            1949年～        憲法記念日 (Constitution Memorial Day)
  //    5月4日(月)        1973年～1987年  振替休日 (transfer holiday) (憲法記念日が日曜日のとき)
  //    5月4日            1988年～2006年  国民の休日 (national day of rest) (憲法記念日とこどもの日に挟まれた平日)
  //                      2007年～        みどりの日 (Greenery Day)
  //    5月5日            1949年～        こどもの日 (Children's Day)
  //    5月6日(月)        1973年～2006年  振替休日 (transfer holiday) (こどもの日が日曜日のとき)
  //    5月6日(月,火,水)  2007年～        振替休日 (transfer holiday) (憲法記念日,みどりの日,こどもの日が日曜日のとき)
  //    7月第3月曜日      2003年～        海の日 (Marine Day)
  //    7月20日           1996年～2002年  海の日 (Marine Day)
  //    8月11日           2016年～        山の日 (Mountain Day)
  //    7月21日(月)       1996年～2002年  振替休日 (transfer holiday) (海の日が日曜日のとき)
  //    9月15日           1966年～2002年  敬老の日 (Respect for the Aged Day)
  //    9月第3月曜日      2003年～        敬老の日 (Respect for the Aged Day)
  //    9月16日(月)       1973年～2002年  振替休日 (transfer holiday) (敬老の日が日曜日のとき)
  //    9月22日頃(火)     2003年～        国民の休日 (national day of rest) (敬老の日と秋分の日に挟まれた平日)
  //    9月23日頃         1948年～        秋分の日 (Autumnal Equinox Day)
  //    9月24日頃(月)     1973年～        振替休日 (transfer holiday) (秋分の日が日曜日のとき)
  //    10月10日          1966年～1999年  体育の日 (Health and Sports Day)
  //    10月第2月曜日     2000年～        体育の日 (Health and Sports Day)
  //    10月11日(月)      1973年～1999年  振替休日 (transfer holiday) (体育の日が日曜日のとき)
  //    11月3日           1948年～        文化の日 (Culture Day)
  //    11月4日(月)       1973年～        振替休日 (transfer holiday) (文化の日が日曜日のとき)
  //    11月23日          1948年～        勤労感謝の日 (Labor Thanksgiving Day)
  //    11月24日(月)      1973年～        振替休日 (transfer holiday) (勤労感謝の日が日曜日のとき)
  //    12月23日          1989年～        天皇誕生日 (The Emperor's Birthday)
  //    12月24日(月)      1989年～        振替休日 (transfer holiday) (天皇誕生日が日曜日のとき)
  //                      1959年4月10日   皇太子明仁親王の結婚の儀 (The Rite of Wedding of HIH Crown Prince Akihito)
  //                      1989年2月24日   昭和天皇の大喪の礼 (The Funeral Ceremony of Emperor Showa.)
  //                      1990年11月12日  即位礼正殿の儀 (The Ceremony of the Enthronement of His Majesty the Emperor (at the Seiden))
  //                      1993年6月9日    皇太子徳仁親王の結婚の儀 (The Rite of Wedding of HIH Crown Prince Naruhito)
  //                                      (HIH: His/Her Imperial Highness; 殿下/妃殿下)
  //    参考
  //      http://www8.cao.go.jp/chosei/shukujitsu/gaiyou.html
  //      http://eco.mtk.nao.ac.jp/koyomi/yoko/
  //      http://www.nao.ac.jp/faq/a0301.html
  //      https://ja.wikipedia.org/wiki/%E5%9B%BD%E6%B0%91%E3%81%AE%E7%A5%9D%E6%97%A5
  //      https://en.wikipedia.org/wiki/Public_holidays_in_Japan

  //jaholi = jaholiDttm (jstdttm)
  //  日時(日本時間)から日本の祝日の名前(日本語)を求める
  public static String dntJaholiJstdttm (long jstdttm) {
    //return dntJaholiJstdate (jstdateJstdttm (jstdttm));
    int jstdate = (int) (jstdttm >> 32);
    //return dntJaholiJstyearJstmontJstmday (jstyearJstdate (jstdate), jstmontJstdate (jstdate), jstmdayJstdate (jstdate));
    return dntJaholiJstyearJstmontJstmday (jstdate >> 10, jstdate >> 6 & 15, jstdate & 63);
  }  //dntJaholiJstdttm(long)

  //jaholi = dntJaholiJstdate (jstdate)
  //  日付(日本時間)から日本の祝日の名前(日本語)を求める
  public static String dntJaholiJstdate (int jstdate) {
    //return dntJaholiJstyearJstmontJstmday (jstyearJstdate (jstdate), jstmontJstdate (jstdate), jstmdayJstdate (jstdate));
    return dntJaholiJstyearJstmontJstmday (jstdate >> 10, jstdate >> 6 & 15, jstdate & 63);
  }  //dntJaholiJstdate(int)

  //jaholi = dntJaholiJstyearJstmontJstmday (jstyear, jstmont, jstmday)
  //  西暦年(日本時間)と月(日本時間)と月通日(日本時間)から日本の祝日の名前(日本語)を求める
  public static String dntJaholiJstyearJstmontJstmday (int jstyear, int jstmont, int jstmday) {
    //int jstwday = jstwdayJstyearJstmontJstmday (jstyear, jstmont, jstmday);
    int jstwday = dntWdayYearMontMday (jstyear, jstmont, jstmday);
    int jstwnum = dntFdiv (jstmday + 6, 7);  //第何jstwday曜日か (1～)
    int jstmdayVernal = ((jstyear & 3) == 0 ? jstyear <= 1956 ? 21 : jstyear <= 2088 ? 20 : 19 :
                        (jstyear & 3) == 1 ? jstyear <= 1989 ? 21 : 20 :
                        (jstyear & 3) == 2 ? jstyear <= 2022 ? 21 : 20 :
                        jstyear <= 1923 ? 22 : jstyear <= 2055 ? 21 : 20);  //春分の日の月通日(日本時間)
    int jstmdayAutumnal = ((jstyear & 3) == 0 ? jstyear <= 2008 ? 23 : 22 :
                          (jstyear & 3) == 1 ? jstyear <= 1917 ? 24 : jstyear <= 2041 ? 23 : 22 :
                          (jstyear & 3) == 2 ? jstyear <= 1946 ? 24 : jstyear <= 2074 ? 23 : 22 :
                          jstyear <= 1979 ? 24 : 23);  //秋分の日の月通日(日本時間)
    return (jstmont == 1 ?  //1月
            jstyear >= 1949 && jstmday == 1 ? "元日" :  //1949年～ 1月1日
            jstyear >= 1974 && jstmday == 2 && jstwday == 1 ? "振替休日" :  //1974年～ 1月2日(月)
            jstyear >= 2000 && jstwnum == 2 && jstwday == 1 ? "成人の日" :  //2000年～ 1月第2月曜日
            jstyear >= 1949 && jstyear <= 1999 && jstmday == 15 ? "成人の日" :  //1949年～1999年 1月15日
            jstyear >= 1974 && jstyear <= 1999 && jstmday == 16 && jstwday == 1 ? "振替休日" : null : //1974年～1999年 1月16日(月)
            jstmont == 2 ?  //2月
            jstyear == 1989 && jstmday == 24 ? "昭和天皇の大喪の礼" :  //1989年2月24日
            jstyear >= 1967 && jstmday == 11 ? "建国記念の日" :  //1967年～ 2月11日
            jstyear >= 1974 && jstmday == 12 && jstwday == 1 ? "振替休日" : null :  //1974年～ 2月12日(月)
            jstmont == 3 ?  //3月
            jstyear >= 1949 && jstmday == jstmdayVernal ? "春分の日" :  //1949年～ 3月21日頃
            jstyear >= 1974 && jstmday == jstmdayVernal + 1 && jstwday == 1 ? "振替休日" : null :  //1974年～ 3月22日頃(月)
            jstmont == 4 ?  //4月
            jstyear == 1959 && jstmday == 10 ? "皇太子明仁親王の結婚の儀" :  //1959年4月10日
            jstyear >= 1949 && jstyear <= 1988 && jstmday == 29 ? "天皇誕生日" :  //1949年～1988年 4月29日
            jstyear >= 1989 && jstyear <= 2006 && jstmday == 29 ? "みどりの日" :  //1989年～2006年 4月29日
            jstyear >= 2007 && jstmday == 29 ? "昭和の日" :  //2007年～ 4月29日
            jstyear >= 1973 && jstmday == 30 && jstwday == 1 ? "振替休日" : null :  //1973年～ 4月30日(月)
            jstmont == 5 ?  //5月
            jstyear >= 1949 && jstmday == 3 ? "憲法記念日" :  //1949年～ 5月3日
            jstyear >= 1973 && jstyear <= 1987 && jstmday == 4 && jstwday == 1 ? "振替休日" :  //1973年～1987年 5月4日(月)
            jstyear >= 1988 && jstyear <= 2006 && jstmday == 4 ? "国民の休日" :  //1988年～2006年 5月4日
            jstyear >= 2007 && jstmday == 4 ? "みどりの日" :  //2007年～ 5月4日
            jstyear >= 1949 && jstmday == 5 ? "こどもの日" :  //1949年～ 5月5日
            jstyear >= 1973 && jstyear <= 2006 && jstmday == 6 && jstwday == 1 ? "振替休日" :  //1973年～2006年 5月6日(月)
            jstyear >= 2007 && jstmday == 6 && (jstwday == 1 || jstwday == 2 || jstwday == 3) ? "振替休日" : null :  //2007年～ 5月6日(月,火,水)
            jstmont == 6 ?  //6月
            jstyear == 1993 && jstmday == 9 ? "皇太子徳仁親王の結婚の儀" : null :  //1993年6月9日
            jstmont == 7 ?  //7月
            jstyear >= 2003 && jstwnum == 3 && jstwday == 1 ? "海の日" :  //2003年～ 7月第3月曜日
            jstyear >= 1996 && jstyear <= 2002 && jstmday == 20 ? "海の日" :  //1996年～2002年 7月20日
            jstyear >= 1996 && jstyear <= 2002 && jstmday == 21 && jstwday == 1 ? "振替休日" : null :  //1996年～2002年 7月21日(月)
            jstmont == 8 ?  //8月
            jstyear >= 2016 && jstmday == 11 ? "山の日" : null :  //2016年～ 8月11日
            jstmont == 9 ?  //9月
            jstyear >= 1966 && jstyear <= 2002 && jstmday == 15 ? "敬老の日" :  //1966年～2002年 9月15日
            jstyear >= 2003 && jstwnum == 3 && jstwday == 1 ? "敬老の日" :  //2003年～ 9月第3月曜日
            jstyear >= 1973 && jstyear <= 2002 && jstmday == 16 && jstwday == 1 ? "振替休日" :  //1973年～2002年 9月16日(月)
            jstyear >= 2003 && dntFdiv (jstmday + 5, 7) == 3 && jstwday == 2 && jstmday == jstmdayAutumnal - 1 ? "国民の休日" :  //2003年～ 9月22日頃(火)
            jstyear >= 1948 && jstmday == jstmdayAutumnal ? "秋分の日" :  //1948年～ 9月23日頃
            jstyear >= 1973 && jstmday == jstmdayAutumnal + 1 && jstwday == 1 ? "振替休日" : null :  //1973年～ 9月24日頃(月)
            jstmont == 10 ?  //10月
            jstyear >= 1966 && jstyear <= 1999 && jstmday == 10 ? "体育の日" :  //1966年～1999年 10月10日
            jstyear >= 2000 && jstwnum == 2 && jstwday == 1 ? "体育の日" :  //2000年～ 10月第2月曜日
            jstyear >= 1973 && jstyear <= 1999 && jstmday == 11 && jstwday == 1 ? "振替休日" : null :  //1973年～1999年 10月11日(月)
            jstmont == 11 ?  //11月
            jstyear == 1990 && jstmday == 12 ? "即位礼正殿の儀" :  //1990年11月12日
            jstyear >= 1948 && jstmday == 3 ? "文化の日" :  //1948年～ 11月3日
            jstyear >= 1973 && jstmday == 4 && jstwday == 1 ? "振替休日" :  //1973年～ 11月4日(月)
            jstyear >= 1948 && jstmday == 23 ? "勤労感謝の日" :  //1948年～ 11月23日
            jstyear >= 1973 && jstmday == 24 && jstwday == 1 ? "振替休日" : null :  //1973年～ 11月24日(月)
            jstmont == 12 ?  //12月
            jstyear >= 1989 && jstmday == 23 ? "天皇誕生日" :  //1989年～ 12月23日
            jstyear >= 1989 && jstmday == 24 && jstwday == 1 ? "振替休日" : null :  //1989年～ 12月24日(月)
            null);
  }  //dntJaholiJstyearJstmontJstmday(int,int,int)

}  //class DnT



