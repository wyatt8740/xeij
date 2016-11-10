//========================================================================================
//  FDC.java
//    en:Floppy disk controller
//    ja:フロッピーディスクコントローラ
//  Copyright (C) 2003-2016 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  http://stdkmd.com/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.filechooser.*;  //FileFilter,FileNameExtensionFilter

public class FDC {

  public static final boolean FDC_DEBUG_TRACE = false;
  public static final boolean FDC_DEBUG_DEFAULT = true;  //true=起動時からデバッグログを有効にする
  public static final boolean FDC_DEBUG_COMMAND = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_CONTROL = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_INTERRUPT = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_MEDIA = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_MOTOR = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_PHASE = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_SEEK = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_STATUS = FDC_DEBUG_TRACE && true;
  public static final boolean FDC_DEBUG_TRANSFER = FDC_DEBUG_TRACE && false;
  public static boolean fdcDebugLogOn;  //true=デバッグログを出力する

  //ポート
  public static final int FDC_STATUS_PORT  = 0x00e94001;  //FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static final int FDC_DATA_PORT    = 0x00e94003;  //FDC データ/コマンド
  public static final int FDC_DRIVE_STATUS = 0x00e94005;  //FDD 状態(挿入|誤挿入|------)/機能(点滅|排出禁止|排出|-|選択####)
  public static final int FDC_DRIVE_SELECT = 0x00e94007;  //FDD $FF/選択(モータON|--|2DD|--|ドライブ##)

  //ユニット数
  public static final int FDC_MIN_UNITS = 2;  //最小ユニット数
  public static final int FDC_MAX_UNITS = 4;  //最大ユニット数

  public static FDUnit[] fdcUnitArray;  //ユニットの配列

  public static File fdcLastFile;  //最後にアクセスしたファイル＝次にファイルチューザーを開いたときに表示するディレクトリ。コマンドラインのみ

  //メニュー
  public static JMenu fdcMenu;

  //開くダイアログ
  public static JDialog fdcOpenDialog;  //ダイアログ
  public static JFileChooser2 fdcOpenFileChooser;  //ファイルチューザー
  public static int fdcOpenUnit;  //開くユニットの番号
  public static boolean fdcOpenWriteProtect;  //true=ライトプロテクトモードで開く
  public static javax.swing.filechooser.FileFilter fdcOpenFileFilter;  //java.io.FileFilterと紛らわしい。フロッピーディスクイメージファイルかどうかを調べるファイルフィルタ。ファイルチューザーとドロップターゲットで使う。コマンドラインのみ

  //フォーマットダイアログ
  public static JDialog fdcFormatDialog;  //ダイアログ
  public static JCheckBox fdcFormatX86SafeCheckBox;  //x86セーフチェックボックス
  public static JFileChooser2 fdcFormatFileChooser;  //ファイルチューザー
  public static FDMedia fdcFormatMedia;  //フォーマットするメディアの種類
  //public static boolean fdcFormatCopySystemFiles;  //true=システムファイルを転送する
  public static boolean fdcFormatX86SafeOn;  //true=x86セーフ
  public static JCheckBox fdcFormatCopyHumanSysCheckBox;  //HUMAN.SYSチェックボックス
  public static JCheckBox fdcFormatCopyCommandXCheckBox;  //COMMAND.Xチェックボックス
  public static boolean fdcFormatCopyHumanSysOn;  //true=HUMAN.SYSを書き込む
  public static boolean fdcFormatCopyCommandXOn;  //true=COMMAND.Xを書き込む
  public static javax.swing.filechooser.FileFilter fdcFormatFileFilter;  //java.io.FileFilterと紛らわしい。フロッピーディスクイメージファイルかどうかを調べるファイルフィルタ

  //FDCステータス
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  //    bit7  RQM  Request for Master  転送準備完了
  //                                     DIO=0(MPU→FDC)のときMPUはRQM=1を待ってから書き込む
  //                                     DIO=1(FDC→MPU)のときMPUはRQM=1を待ってから読み出す
  //    bit6  DIO  Data Input/Output   転送方向
  //                                     0  MPU→FDC。C-PhaseまたはE-Phase
  //                                     1  FDC→MPU。R-PhaseまたはE-Phase
  //    bit5  NDM  Non-DMA Mode        Non-DMAモード
  //                                     1  E-Phase(Non-DMA Modeで転送中)
  //    bit4  CB   FDC Busy            FDCビジー
  //                                     1  C-Phaseの2バイト目以降またはE-Phase(転送中)またはR-Phase。コマンド入力不可
  //    bit3  D3B  FD3 Busy            ユニットビジー
  //    bit2  D2B  FD2 Busy              1  C-Phase(シークのリザルトステータスの引き取り待ち)またはE-Phase(シーク中)。転送コマンド不可
  //    bit1  D1B  FD1 Busy
  //    bit0  D0B  FD0 Busy
  //
  //  C-Phaseの1バイト目
  //    0x00e94001=0x80(RQM=1,DIO=0(MPU→FDC),NDM=0,CB=0)を待って
  //    0x00e94003に出力する
  //
  //  C-Phaseの2バイト目
  //    0x00e94001=0x00(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=0)ではなくて
  //    0x00e94001=0x10(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=1)ではなくて
  //    0x00e94001=0x90(RQM=1,DIO=0(MPU→FDC),NDM=0,CB=1)を待って
  //    0x00e94003に出力する
  //
  //  R-Phase
  //    0x00e94001=0x10(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=1)ではなくて
  //    0x00e94001=0x50(RQM=0,DIO=1(FDC→MPU),NDM=0,CB=1)ではなくて
  //    0x00e94001=0xd0(RQM=1,DIO=1(FDC→MPU),NDM=0,CB=1)を待って
  //    0x00e94003から入力する
  //
  public static final int FDC_RQM        = 0x80;  //RQM
  public static final int FDC_MPU_TO_FDC = 0x00;  //DIO=0 OUT(MPU→FDC)
  public static final int FDC_FDC_TO_MPU = 0x40;  //DIO=1 IN(FDC→MPU)
  public static final int FDC_NDM        = 0x20;  //NDM
  public static final int FDC_CB         = 0x10;  //CB
  public static final int FDC_D3B        = 0x08;  //D3B
  public static final int FDC_D2B        = 0x04;  //D2B
  public static final int FDC_D1B        = 0x02;  //D1B
  public static final int FDC_D0B        = 0x01;  //D0B
  public static int fdcStatus;  //FDCステータス(RQM|DIO(OUT/IN)|NDM|CB)。D3B|D2B|D1B|D0BはReadでfduBusyを合わせる

  //リザルトステータス
  //  リザルトステータスとデバイスエラーの関係はHuman302の0x00010ceeを参照
  //    FDC_ST0_NR  「ディスクが入っていません、入れてください」
  //    FDC_ST1_NW  「プロテクトをはずして、同じディスクを入れてください」
  //    FDC_ST1_DE  「ＣＲＣエラー」
  //    FDC_ST2_DD  「ＣＲＣエラー」
  //    FDC_ST2_SN  「読み込みエラー」
  //    FDC_ST0_AT  「無効なメディアを使用しました」
  //  ST0
  //    bit6-7  IC   Interrupt Code 割り込みの発生要因
  //                 00  NT  Normal Terminate コマンドの正常終了
  //                 01  AT  Abnormal Terminate コマンドの異常終了
  //                         「無効なメディアを使用しました」
  //                 10  IC  Invalid Command 無効なコマンド
  //                 11  AI  Attention Interrupt デバイスに状態遷移があった
  //    bit5    SE   Seek End
  //                 1  SEEKコマンドまたはRECALIBRATEコマンドのシーク動作が正常終了または異常終了した
  //                    ディスクがないときもセット
  //    bit4    EC   Equipment Check
  //                 1  デバイスからFault信号を受け取った
  //                    RECALIBRATEコマンドでTrack 0信号を一定時間検出できなかった
  //                    _B_RECALIの強制レディチェックでドライブがないときセット
  //    bit3    NR   Not Ready
  //                 1  デバイスがReady状態でない
  //                    「ディスクが入っていません、入れてください」
  //    bit2    HD   Head Address 割り込み発生時のヘッドの状態
  //                 Sense Interrupt Statusコマンドでは常に0
  //    bit1    US1  Unit Select 1
  //    bit0    US0  Unit Select 0
  //                 割り込み発生時のデバイス番号
  public static final int FDC_ST0_NT = 0x00 << 24;
  public static final int FDC_ST0_AT = 0x40 << 24;
  public static final int FDC_ST0_IC = 0x80 << 24;
  public static final int FDC_ST0_AI = 0xc0 << 24;
  public static final int FDC_ST0_SE = 0x20 << 24;
  public static final int FDC_ST0_EC = 0x10 << 24;
  public static final int FDC_ST0_NR = 0x08 << 24;
  //  ST1
  //    bit7    EN   End of Cylinder
  //                 1  EOTで指定した最終セクタを超えてリードまたはライトを続けようとした
  //    bit6    -    常に0
  //    bit5    DE   Data Error
  //                 1  ディスク上のIDまたはデータのCRCエラーを検出した(READ IDコマンドを除く)
  //                    (IDとデータの区別はST2のDDによる)
  //                    「ＣＲＣエラー」
  //    bit4    OR   Overrun
  //                 1  MPUまたはDMAが規定時間内にデータ転送を行わなかった
  //    bit3    -    常に0
  //    bit2    ND   No Data
  //                 1  以下のコマンドでIDRで指定したセクタがトラック上に検出できなかった(このときST2のNCもセット)
  //                      READ DATA
  //                      READ DELETED DATA
  //                      WRITE DATA
  //                      WRITE DELETED DATA
  //                      SCAN
  //                    READ IDコマンドでトラック上にCRCエラーのないIDが見つからない
  //                    READ DIAGNOSTICコマンドでセクタIDと指定されたIDRの内容が一致しない
  //    bit1    NW   Not Writable
  //                 1  ライト系コマンドでライトプロテクト信号を検出した
  //                    「プロテクトをはずして、同じディスクを入れてください」
  //    bit0    MA   Missing Address Mark
  //                 1  IDをアクセスするコマンドでインデックスパルスを2回検出するまでにIDAMが見つからなかった
  //                    IDAMが見つかった後、DAMまたはDDAMが見つからなかった(このときST2のMDもセット)
  public static final int FDC_ST1_EN = 0x80 << 16;
  public static final int FDC_ST1_DE = 0x20 << 16;
  public static final int FDC_ST1_OR = 0x10 << 16;
  public static final int FDC_ST1_ND = 0x04 << 16;
  public static final int FDC_ST1_NW = 0x02 << 16;
  public static final int FDC_ST1_MA = 0x01 << 16;
  //  ST2
  //    bit7    -    常に0
  //    bit6    CM   Control Mark
  //                 1  READ DATAコマンドまたはREAD DIAGNOSTICコマンドまたはSCANコマンドでDDAMを検出した
  //                    READ DELETED DATAコマンドでDAMを検出した
  //                    削除データ読み込み時に通常データを読み込もうとしたまたはその逆のときセット
  //    bit5    DD   Data Error in Data Field
  //                 1  CRCエラーが検出された
  //                    「ＣＲＣエラー」
  //    bit4    NC   No Cylinder
  //                 1  ST1のNDに付帯して、IDのCバイトが一致せず0xffでもない(READ DIAGNOSTICを除く)
  //                    シリンダが見つからなかったときにセット
  //    bit3    SH   Scan Equal Hit
  //                 1  SCANコマンドでEqual条件を満足した
  //                    ベリファイコマンドで一致したときにセット
  //    bit2    SN   Scan Not Satisfied
  //                 1  SCANコマンドで最終セクタまで条件を満足しなかった
  //                    ベリファイコマンドで不一致があったときにセット
  //                    「読み込みエラー」
  //    bit1    BC   Bad Cylinder
  //                 1  ST1のNDに付帯して、IDのCバイトが0xff(READ DIAGNOSTICを除く)
  //                    シリンダの番号が規定外のときにセット
  //    bit0    MD   Missing Address Mark in Data Field
  //                 1   ST1のMAに付帯して、IDAMが見つかった後、DAMまたはDDAMが見つからなかった
  //                     データフィールドがないときにセット
  public static final int FDC_ST2_CM = 0x40 << 8;
  public static final int FDC_ST2_DD = 0x20 << 8;
  public static final int FDC_ST2_NC = 0x10 << 8;
  public static final int FDC_ST2_SH = 0x08 << 8;
  public static final int FDC_ST2_SN = 0x04 << 8;
  public static final int FDC_ST2_BC = 0x02 << 8;
  public static final int FDC_ST2_MD = 0x01 << 8;
  //  ST3
  //    bit7    FT   Fault
  //                 デバイスからのFault信号の状態
  //    bit6    WP   Write Protected
  //                 デバイスからのWrite Protected信号の状態
  //    bit5    RY   Ready
  //                 デバイスからのReady信号の状態
  //                 モータONから372ms後くらいに0→1
  //    bit4    T0   Track 0
  //                 デバイスからのTrack 0信号の状態
  //                 モータONまで0、モータONで0→1
  //    bit3    TS   Tow Side
  //                 デバイスからのTow Side信号の状態
  //                 常に0
  //    bit2    HD   Head Address
  //                 デバイスへのSide Select信号の状態
  //    bit1    US1  Unit Select 1
  //                 デバイスへのUnit Select 1信号の状態
  //    bit0    US0  Unit Select 0
  //                 デバイスへのUnit Select 0信号の状態
  public static final int FDC_ST3_FT = 0x80;
  public static final int FDC_ST3_WP = 0x40;
  public static final int FDC_ST3_RY = 0x20;
  public static final int FDC_ST3_T0 = 0x10;
  public static final int FDC_ST3_TS = 0x08;
  public static int fdcResultStatus;  //リザルトステータス。ST0<<24|ST1<<16|ST2<<8|fduPCN

  //コマンド
  public static final String[] FDC_COMMAND_NAME = {  //コマンド名(デバッグ用)
    "0x00  INVALID",
    "0x01  INVALID",
    "0x02  READ DIAGNOSTIC",  //トラックのフォーマットを調べる
    "0x03  SPECIFY",  //FDCの動作モードを設定する
    "0x04  SENSE DEVICE STATUS",  //FDDの状態を読み出す
    "0x05  WRITE DATA",  //セクタを指定してデータを書き込む
    "0x06  READ DATA",  //セクタを指定してデータを読み出す
    "0x07  RECALIBRATE",  //ヘッドをシリンダ0(最外周)へ移動させる
    "0x08  SENSE INTERRUPT STATUS",  //FDCの割り込み要因を読み出す
    "0x09  WRITE DELETED DATA",  //セクタを指定して削除データを書き込む
    "0x0a  READ ID",  //セクタのIDを読み出す
    "0x0b  INVALID",
    "0x0c  READ DELETED DATA",  //セクタを指定して削除データを読み出す
    "0x0d  WRITE ID",  //トラックをフォーマットする
    "0x0e  INVALID",
    "0x0f  SEEK",  //シリンダを指定してヘッドを移動させる
    "0x10  INVALID",
    "0x11  SCAN EQUAL",  //条件に合うセクタを探す
    "0x12  INVALID",
    "0x13  INVALID",
    "0x14  RESET STANDBY",  //FDCのスタンバイ状態を解除する
    "0x15  SET STANDBY",  //FDCをスタンバイ状態にする
    "0x16  SOFTWARE RESET",  //FDCを初期状態にする
    "0x17  INVALID",
    "0x18  INVALID",
    "0x19  SCAN LOW OR EQUAL",  //条件に合うセクタを探す
    "0x1a  INVALID",
    "0x1b  INVALID",
    "0x1c  INVALID",
    "0x1d  SCAN HIGH OR EQUAL",  //条件に合うセクタを探す
    "0x1e  INVALID",
    "0x1f  INVALID",
  };
/*
  public static final int[] FDC_COMMAND_LENGTH = {  //コマンドの長さ。INVALIDも含めて1以上
    1,  //0x00  INVALID
    1,  //0x01  INVALID
    9,  //0x02  READ DIAGNOSTIC
    3,  //0x03  SPECIFY
    2,  //0x04  SENSE DEVICE STATUS
    9,  //0x05  WRITE DATA
    9,  //0x06  READ DATA
    2,  //0x07  RECALIBRATE
    1,  //0x08  SENSE INTERRUPT STATUS
    9,  //0x09  WRITE DELETED DATA
    2,  //0x0a  READ ID
    1,  //0x0b  INVALID
    9,  //0x0c  READ DELETED DATA
    6,  //0x0d  WRITE ID
    1,  //0x0e  INVALID
    3,  //0x0f  SEEK
    1,  //0x10  INVALID
    9,  //0x11  SCAN EQUAL
    1,  //0x12  INVALID
    1,  //0x13  INVALID
    1,  //0x14  RESET STANDBY
    1,  //0x15  SET STANDBY
    1,  //0x16  SOFTWARE RESET
    1,  //0x17  INVALID
    1,  //0x18  INVALID
    9,  //0x19  SCAN LOW OR EQUAL
    1,  //0x1a  INVALID
    1,  //0x1b  INVALID
    1,  //0x1c  INVALID
    9,  //0x1d  SCAN HIGH OR EQUAL
    1,  //0x1e  INVALID
    1,  //0x1f  INVALID
  };
*/
  //  perl misc/itob.pl xeij/FDC.java FDC_COMMAND_LENGTH
  public static final byte[] FDC_COMMAND_LENGTH = "\1\1\t\3\2\t\t\2\1\t\2\1\t\6\1\3\1\t\1\1\1\1\1\1\1\t\1\1\1\t\1\1".getBytes (XEiJ.ISO_8859_1);
  public static int fdcCommandNumber;  //処理中のコマンド番号。C-Phaseの1バイト目まで-1、2バイト目からR-PhaseまでfdcCommandBuffer[0]&31

  //バッファ
  public static final byte[] fdcCommandBuffer = new byte[256];  //コマンドバッファ
  public static final byte[] fdcResultBuffer = new byte[256];  //リザルトバッファ
  public static final byte[] fdcTempBuffer = new byte[16384];  //WRITE IDまたはSCANで使用するバッファ
  public static byte[] fdcReadHandle;  //E-Phase(Read)のときfduImageまたはfdcIdBuffer、R-PhaseのときfdcResultBuffer、それ以外はnull
  public static byte[] fdcWriteHandle;  //C-Phase(Write)のときfdcCommandBuffer、E-Phase(Write)のときfduImageまたはfdcIdBuffer、それ以外はnull
  public static int fdcIndex;  //fdcReadHandleまたはfdcWriteHandleの次に読み書きするインデックス
  public static int fdcStart;  //fdcReadHandleまたはfdcWriteHandleの読み書きを開始するインデックス。デバッグ表示用
  public static int fdcLimit;  //fdcReadHandleまたはfdcWriteHandleの読み書きを終了するインデックス

  //  強制レディフラグ
  public static boolean fdcEnforcedReady;  //true=強制レディ状態(YM2151のCT2が1)

  //  ユニット選択
  //    ドライブコントロール(0x00e94005)のWriteのbit1-0
  //    ドライブステータス(0x00e94005)を読み出すユニットを選択する
  public static FDUnit fdcDriveLastSelected;  //ドライブコントロールで選択されているユニット

  //  転送終了割り込み
  //    転送命令のE-Phaseが終了したときR-Phase(RQM=1,DIO=1(FDC→MPU))の先頭で割り込む
  //    MPUはそのままリザルトステータス(ST0,ST1,ST2,C,H,R,N)を受け取る

  //  シーク終了割り込み
  //    SEEKまたはRECALIBRATEが終了したときC-Phase(RQM=1,DIO=0(MPU→FDC))で割り込む
  //    MPUはSENSE INTERRUPT STATUSでR-Phase(RQM=1,DIO=1(FDC→MPU))にしてからリザルトステータス(ST0,PCN)を受け取る
  //    複数のユニットでシーク終了割り込みが発生したときは番号が小さいユニットのリザルトステータスが優先される
  //    SENSE INTERRUPT STATUSが終わったとき番号の大きいユニットのシーク終了割り込みが残っていたら再度割り込みが要求される
  public static int fdcStepRateTime;  //SPECIFYコマンドのSRT

  //  状態遷移割り込み

  //  割り込み要求フラグ
  //    FDC割り込みハンドラはSENSE INTERRUPT STATUSをInvalid Commandが返るまで繰り返してすべての割り込み要因を取り除かなければならない
  //    bit11  1=ユニット3状態遷移割り込み要求あり
  //    bit10  1=ユニット2状態遷移割り込み要求あり
  //    bit9   1=ユニット1状態遷移割り込み要求あり
  //    bit8   1=ユニット0状態遷移割り込み要求あり
  //    bit7   1=ユニット3シーク終了割り込み要求あり
  //    bit6   1=ユニット2シーク終了割り込み要求あり
  //    bit5   1=ユニット1シーク終了割り込み要求あり
  //    bit4   1=ユニット0シーク終了割り込み要求あり
  //    bit3   1=ユニット3転送終了割り込み要求あり
  //    bit2   1=ユニット2転送終了割り込み要求あり
  //    bit1   1=ユニット1転送終了割り込み要求あり
  //    bit0   1=ユニット0転送終了割り込み要求あり
  public static final int FDC_MOTOR_REQUEST    = 0x0100;  //状態遷移割り込み要求
  public static final int FDC_SEEK_REQUEST     = 0x0010;  //シーク終了割り込み要求
  public static final int FDC_TRANSFER_REQUEST = 0x0001;  //転送終了割り込み要求
  public static int fdcInterruptRequest;  //割り込み要求フラグ

  //  ポーリング
  //    開始
  //      CBが0になったとき(C-Phaseに戻ったとき)
  //        ポーリングティッカーをキューに入れて1ms後に呼び出す
  //    継続
  //      ポーリングティッカーが呼び出されたとき
  //        ポーリングティッカーをキューに入れて1ms後に呼び出す
  //    終了
  //      CBが1になったとき(コマンドの1バイト目が入力されたとき)
  //        ポーリングティッカーをキューから取り除く
  public static final long FDC_POLLING_DELAY = XEiJ.TMR_FREQ * 1000 / 1000000;  //1ms。C-Phaseに戻ってからポーリングを開始するまでの時間。10MHzのとき50μsだとFDDEVICE.Xの組み込みで止まる
  public static final long FDC_POLLING_INTERVAL = XEiJ.TMR_FREQ * 1 / 1000;  //1ms。ポーリングの間隔

  //  ポーリングティッカー
  //    ユニット0..3について
  //      ユニットがシーク中のとき(ターゲットシリンダ番号が0以上のとき)
  //        強制レディ状態で接続されていないとき
  //          ターゲットシリンダ番号を-1にする
  //          ユニットビジーフラグをOFFにする
  //          シーク終了割り込み要求フラグをONにする
  //          FDC割り込みを要求する
  //        ノットレディのとき
  //          ターゲットシリンダ番号を-1にする
  //          ユニットビジーフラグをOFFにする
  //          シーク終了割り込み要求フラグをONにする
  //          FDC割り込みを要求する
  //        レディのとき
  //          シリンダ番号とターゲットシリンダ番号が一致していないとき
  //            ステップレートカウンタをデクリメントする
  //            ステップレートカウンタが0になったとき
  //              ステップレートカウンタを16-SRTにする
  //              シリンダ番号がターゲットシリンダ番号よりも小さいとき
  //                シリンダ番号をインクリメントする
  //              シリンダ番号がターゲットシリンダ番号よりも大きいとき
  //                シリンダ番号をデクリメントする
  //        改めて、シリンダ番号とターゲットシリンダ番号が一致しているとき
  //          ターゲットシリンダ番号を-1にする
  //          ユニットビジーフラグをOFFにする
  //          シーク終了割り込み要求フラグをONにする
  //          FDC割り込みを要求する
  //      ユニットがシーク中でないとき(ターゲットシリンダ番号が0未満のとき)
  //        レディフラグが前回と異なるとき
  //          レディフラグを保存する
  //          状態遷移割り込みを要求フラグをONにする
  //          FDC割り込みを要求する
  //    ポーリングティッカーをキューに入れて1ms後に呼び出す
  public static final TickerQueue.Ticker fdcPollingTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      for (int u = 0; u < 4; u++) {  //ユニット0..3について
        FDUnit unit = fdcUnitArray[u];  //ユニット
        if (0 <= unit.fduTargetCylinder) {  //ユニットがシーク中のとき(ターゲットシリンダ番号が0以上のとき)
          if (fdcEnforcedReady && !unit.fduIsConnected ()) {  //強制レディ状態で接続されていないとき
            unit.fduTargetCylinder = -1;  //ターゲットシリンダ番号を-1にする
            if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduTargetCylinder);
            }
            unit.fduBusy = false;  //ユニットビジーフラグをOFFにする
            if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduBusy);
            }
            fdcInterruptRequest |= FDC_SEEK_REQUEST << u;  //シーク終了割り込み要求フラグをONにする
            if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
              System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
            }
            XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
          } else if (!unit.fduIsReady ()) {  //ノットレディのとき
            unit.fduTargetCylinder = -1;  //ターゲットシリンダ番号を-1にする
            if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduTargetCylinder);
            }
            unit.fduBusy = false;  //ユニットビジーフラグをOFFにする
            if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduBusy);
            }
            fdcInterruptRequest |= FDC_SEEK_REQUEST << u;  //シーク終了割り込み要求フラグをONにする
            if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
              System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
            }
            XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
          } else {  //レディのとき
            if (unit.fduPCN != unit.fduTargetCylinder) {  //シリンダ番号とターゲットシリンダ番号が一致していないとき
              unit.fduStepRateCounter--;  //ステップレートカウンタをデクリメントする
              if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
                System.out.printf ("%08x:%d unit=%d,fduStepRateCounter=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduStepRateCounter);
              }
              if (unit.fduStepRateCounter == 0) {  //ステップレートカウンタが0になったとき
                unit.fduStepRateCounter = 16 - fdcStepRateTime;  //ステップレートカウンタを16-SRTにする
                if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
                  System.out.printf ("%08x:%d unit=%d,fduStepRateCounter=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduStepRateCounter);
                }
                if (unit.fduPCN < unit.fduTargetCylinder) {  //シリンダ番号がターゲットシリンダ番号よりも小さいとき
                  unit.fduPCN++;  //シリンダ番号をインクリメントする
                  if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
                    System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduPCN);
                  }
                } else {  //シリンダ番号がターゲットシリンダ番号よりも大きいとき
                  unit.fduPCN--;  //シリンダ番号をデクリメントする
                  if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
                    System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduPCN);
                  }
                }
              }
            }
            if (unit.fduPCN == unit.fduTargetCylinder) {  //改めて、シリンダ番号とターゲットシリンダ番号が一致しているとき
              unit.fduTargetCylinder = -1;  //ターゲットシリンダ番号を-1にする
              if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
                System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduTargetCylinder);
              }
              unit.fduBusy = false;  //ユニットビジーフラグをOFFにする
              if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
                System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, u, unit.fduBusy);
              }
              fdcInterruptRequest |= FDC_SEEK_REQUEST << u;  //シーク終了割り込み要求フラグをONにする
              if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
                System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
              }
              XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
            }
          }
        } else {  //ユニットがシーク中でないとき(ターゲットシリンダ番号が0未満のとき)
          boolean ready = unit.fduIsReady ();
          if (unit.fduLastReady != ready) {  //レディフラグが前回と異なるとき
            unit.fduLastReady = ready;  //レディフラグを保存する
            //!!! X68030実機で2HDディスクを入れたFDD0のモータをONにしたら、FDD0だけでなくメディアが入っていないFDD1と接続されていないFDD2とFDD3もレディ状態になったことを知らせる4個の状態遷移割り込みステータスが出力された。OFFも同じ。
            fdcInterruptRequest |= FDC_MOTOR_REQUEST << u;  //状態遷移割り込み要求フラグをONにする
            if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
              System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
            }
            XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
          }
        }
      }  //for u
      TickerQueue.tkqAdd (fdcPollingTicker, XEiJ.mpuClockTime + FDC_POLLING_INTERVAL);  //ポーリングティッカーをキューに入れて1ms後に呼び出す
    }
  };


  //fdcInit ()
  //  FDCを初期化する
  public static void fdcInit () {

    if (FDC_DEBUG_TRACE) {
      fdcDebugLogOn = FDC_DEBUG_DEFAULT;
    }

    if (XEiJ.prgIsLocal) {  //ローカルのとき

      //最後にアクセスしたファイルの初期値＝最初にファイルチューザーを開いたときに表示するディレクトリ
      fdcLastFile = new File (".");  //カレントディレクトリ

      fdcOpenDialog = null;
      fdcOpenFileChooser = null;
      fdcOpenUnit = 0;
      fdcOpenWriteProtect = false;

      fdcFormatDialog = null;
      fdcFormatX86SafeCheckBox = null;
      fdcFormatFileChooser = null;
      fdcFormatMedia = FDMedia.FDM_2HD;
      //fdcFormatCopySystemFiles = false;
      fdcFormatX86SafeOn = true;
      fdcFormatCopyHumanSysCheckBox = null;
      fdcFormatCopyCommandXCheckBox = null;
      fdcFormatCopyHumanSysOn = true;
      fdcFormatCopyCommandXOn = true;

      //ファイルフィルタ
      //  フロッピーディスクイメージファイルかどうかを調べる
      //  ファイルチューザーとドロップターゲットで使う
      fdcOpenFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
        @Override public boolean accept (File file) {
          if (file.isDirectory ()) {
            return true;
          }
          String path = file.getPath ();
          if (fdcIsInsertedPath (path)) {  //既に挿入されている
            return false;
          }
          return FDMedia.fdmPathToMedia (path, null) != null;  //ファイルサイズと拡張子を確認する
        }
        @Override public String getDescription () {
          return Multilingual.mlnJapanese ? "フロッピーディスクイメージ (*.XDF,*.2HD,*.2HC,*.DIM,etc.)" : "Floppy Disk Image (*.XDF,*.2HD,*.2HC,*.DIM,etc.)";
        }
      };
      fdcFormatFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
        @Override public boolean accept (File file) {
          if (file.isDirectory ()) {
            return true;
          }
          String path = file.getPath ();
          if (fdcIsInsertedPath (path)) {  //既に挿入されている
            return false;
          }
          return FDMedia.fdmExtToMedia (FDMedia.fdmPathToExt (path)) != null;  //拡張子だけ確認する。*.DIMは不可
        }
        @Override public String getDescription () {
          return Multilingual.mlnJapanese ? "フロッピーディスクイメージ (*.XDF,*.2HD,*.2HC,etc.)" : "Floppy Disk Image (*.XDF,*.2HD,*.2HC,etc.)";
        }
      };

    }

    fdcUnitArray = new FDUnit[FDC_MAX_UNITS];
    for (int u = 0; u < FDC_MAX_UNITS; u++) {
      FDUnit unit = fdcUnitArray[u] = new FDUnit (u);
      if (u < FDC_MIN_UNITS) {
        unit.connect (false);  //ドライブ0とドライブ1は最初から接続されていて切り離せない
      }
      String path = Settings.sgsCurrentMap.get ("fd" + u);
      boolean userWriteProtect = false;
      if (path.toUpperCase ().endsWith (":R")) {  //書き込み禁止モードで開く
        path = path.substring (0, path.length () - 2);
        userWriteProtect = true;
      }
      //boolean hostWriteProtect = !XEiJ.prgIsLocal;  //ローカルでないときは書き込み禁止
      boolean hostWriteProtect = false;  //アプレットのとき男弾のハイスコアを書き込もうとしてエラーが出てしまうので書き込みを禁止しない
      if (XEiJ.prgIsLocal) {  //ローカルのとき
        hostWriteProtect = !new File (path).canWrite ();
      }
      if (path.length () != 0) {
        unit.connect (true);  //接続されていなければ接続する
        if (unit.insert (path)) {  //挿入できた
          if (userWriteProtect || hostWriteProtect) {  //書き込みを禁止する
            unit.protect (false);  //開くときに書き込みを禁止した場合はイジェクトするまで書き込みを許可できない
          }
          if (XEiJ.prgIsLocal) {  //ローカルのとき
            fdcLastFile = new File (path);
          }
        }
      }
    }

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Create New Floppy Disk Images":  //フロッピーディスクイメージの新規作成
          fdcOpenFormatDialog ();
          break;
        case "FDC Debug Log":  //FDC デバッグログ
          if (FDC_DEBUG_TRACE) {
            fdcDebugLogOn = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        }
      }
    };

    //FDメニュー
    fdcMenu = XEiJ.createMenu ("FD");  //横に長いとサブメニューを開きにくいので短くする
    XEiJ.addComponents (
      fdcMenu,
      XEiJ.createHorizontalBox (Multilingual.mlnText (XEiJ.createLabel ("Floppy Disk"), "ja", "フロッピーディスク")),
      XEiJ.createHorizontalSeparator ()
      );
    for (FDUnit unit : fdcUnitArray) {
      fdcMenu.add (unit.getMenuBox ());
    }
    XEiJ.addComponents (
      fdcMenu,
      XEiJ.createHorizontalSeparator (),
      XEiJ.setEnabled (
        Multilingual.mlnText (XEiJ.createMenuItem ("Create New Floppy Disk Images", listener), "ja", "フロッピーディスクイメージの新規作成"),
        XEiJ.prgIsLocal),  //ローカルのとき
      FDC_DEBUG_TRACE ? Multilingual.mlnText (XEiJ.createCheckBoxMenuItem (fdcDebugLogOn, "FDC Debug Log", listener), "ja", "FDC デバッグログ") : null
      );

    //fdcCommandBuffer = new byte[256];
    //fdcResultBuffer = new byte[256];
    //fdcTempBuffer = new byte[16384];
    fdcResultStatus = FDC_ST0_IC;
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      fdcPrintResultStatus (fdcResultStatus);
    }
    if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
      System.out.printf ("%08x:%d FDC C-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
    }
    fdcStatus = FDC_RQM | FDC_MPU_TO_FDC;
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                         fdcStatus >> 7,
                         fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                         fdcStatus >> 5 & 1,
                         fdcStatus >> 4 & 1);
    }
    fdcReadHandle = null;
    fdcWriteHandle = fdcCommandBuffer;  //C-Phase
    fdcIndex = fdcStart = 0;
    fdcLimit = 1;  //C-Phaseの1バイト目
    fdcCommandNumber = -1;  //C-Phaseの1バイト目

    fdcEnforcedReady = false;  //強制レディOFF
    fdcDriveLastSelected = null;  //ドライブ選択なし

    fdcStepRateTime = 3;  //SRT
    fdcInterruptRequest = 0;  //割り込み要求なし
    if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
    }

  }  //fdcInit()

  //fdcReset ()
  public static void fdcReset () {
    TickerQueue.tkqAdd (fdcPollingTicker, XEiJ.mpuClockTime + FDC_POLLING_DELAY);  //ポーリングティッカーをキューに入れて1ms後に呼び出す
  }  //fdcReset()

  //fdcTini ()
  //  後始末
  //  イメージファイルに書き出す
  public static void fdcTini () {
    for (FDUnit unit : fdcUnitArray) {
      unit.fduTini ();
    }
  }  //fdcTini()

  //inserted = fdcIsInsertedPath (path)
  //  パスで指定したファイルが既に挿入されているか調べる
  public static boolean fdcIsInsertedPath (String path) {
    for (FDUnit unit : fdcUnitArray) {
      if (unit != null &&
          unit.fduIsConnected () &&  //接続されている
          unit.fduIsInserted () &&  //挿入されている
          unit.abuPath.equals (path)) {  //パスが一致している
        return true;  //既に挿入されている
      }
    }
    return false;  //まだ挿入されていない
  }  //fdcIsInsertedPath(String)

  //fdcMakeOpenDialog ()
  //  開くダイアログを作る
  //  コマンドラインのみ
  public static void fdcMakeOpenDialog () {
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case JFileChooser.APPROVE_SELECTION:
        case "Reboot from it":  //ここから再起動
          {
            File[] list = fdcOpenFileChooser.getSelectedFiles2 ();
            if (list.length > 0) {
              fdcOpenFiles (list, true);
              fdcOpenDialog.setVisible (false);
            }
          }
          break;
        case "Open":  //開く
          {
            File[] list = fdcOpenFileChooser.getSelectedFiles2 ();
            if (list.length > 0) {
              fdcOpenFiles (list, false);
              fdcOpenDialog.setVisible (false);
            }
          }
          break;
        case JFileChooser.CANCEL_SELECTION:
        case "Cancel":  //キャンセル
          fdcOpenDialog.setVisible (false);
          break;
        case "Write-Protect":  //書き込み禁止
          fdcOpenWriteProtect = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        }
      }
    };
    fdcOpenFileChooser = new JFileChooser2 (fdcLastFile);
    fdcOpenFileChooser.setFileFilter (fdcOpenFileFilter);
    fdcOpenFileChooser.setMultiSelectionEnabled (true);  //複数選択可能
    fdcOpenFileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
    fdcOpenFileChooser.addActionListener (listener);
    fdcOpenDialog = Multilingual.mlnTitle (
      XEiJ.createModalDialog (
        XEiJ.frmFrame,
        "Open Floppy Disk Images",
        XEiJ.createBorderPanel (
          0, 0,
          XEiJ.createVerticalBox (
            fdcOpenFileChooser,
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              Multilingual.mlnText (XEiJ.createCheckBox (fdcOpenWriteProtect, "Write-Protect", listener), "ja", "書き込み禁止"),
              Box.createHorizontalGlue (),
              Multilingual.mlnText (XEiJ.createButton ("Reboot from it", KeyEvent.VK_R, listener), "ja", "ここから再起動"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Open", KeyEvent.VK_O, listener), "ja", "開く"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Cancel", KeyEvent.VK_C, listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "フロッピーディスクイメージを開く");
  }  //fdcMakeOpenDialog()

  //fdcOpenFiles (list, reset)
  //  開くダイアログで選択されたファイルを開く
  //  コマンドラインのみ
  public static void fdcOpenFiles (File[] list, boolean reset) {
    for (int u = fdcOpenUnit, k = 0; k < list.length; ) {
      if (u >= FDC_MAX_UNITS) {
        reset = false;  //ユニットが足りないときはリセットをキャンセルする
        break;
      }
      FDUnit unit = fdcUnitArray[u];  //ユニット
      if (!unit.fduIsConnected ()) {  //接続されていない
        u++;
        continue;
      }
      File file = list[k++];  //イメージファイル
      if (!file.isFile ()) {  //ファイルが存在しない
        reset = false;  //ファイルが存在しないときはリセットをキャンセルする
        continue;
      }
      if (unit.insert (file.getPath ())) {  //挿入できた
        if (fdcOpenWriteProtect || !file.canWrite ()) {  //書き込みを禁止する
          unit.protect (false);  //開くときに書き込みを禁止した場合はイジェクトするまで書き込みを許可できない
        }
        fdcLastFile = file;  //最後にアクセスしたファイル
        u++;
      } else {
        reset = false;  //挿入できないファイルがあったときはリセットをキャンセルする
      }
    }
    if (reset) {  //ここから再起動
      XEiJ.mpuReset (0x9070 | fdcOpenUnit << 8, -1);
    }

  }  //fdcOpenFiles

  //fdcMakeFormatDialog ()
  //  フォーマットダイアログを作る
  //  コマンドラインのみ
  public static void fdcMakeFormatDialog () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case JFileChooser.APPROVE_SELECTION:
        case "Start Formatting":  //フォーマットを開始する
          {
            File[] list = fdcFormatFileChooser.getSelectedFiles2 ();
            if (list.length > 0) {
              fdcFormatDialog.setVisible (false);
              if (!fdcFormatFiles (list)) {
                //!!! 失敗
              }
            }
          }
          break;
        case JFileChooser.CANCEL_SELECTION:
        case "Cancel":  //キャンセル
          fdcFormatDialog.setVisible (false);
          break;
        case "2HD (1232KB)":
          fdcFormatMedia = FDMedia.FDM_2HD;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HC (1200KB)":
          fdcFormatMedia = FDMedia.FDM_2HC;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2DD (640KB)":
          fdcFormatMedia = FDMedia.FDM_2DD8;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2DD (720KB)":
          fdcFormatMedia = FDMedia.FDM_2DD9;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HQ (1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HQ;
          fdcFormatX86SafeCheckBox.setEnabled (true);
          fdcFormatX86SafeCheckBox.setSelected (fdcFormatX86SafeOn);
          break;
        case "2DD (800KB)":
          fdcFormatMedia = FDMedia.FDM_2DD10;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HDE(1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HDE;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HS (1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HS;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
          //case "Copy System Files":  //システムファイルを転送する
          //  fdcFormatCopySystemFiles = ((JCheckBox) ae.getSource ()).isSelected ();
          //  break;
        case "HUMAN.SYS":
          fdcFormatCopyHumanSysOn = fdcFormatCopyHumanSysCheckBox.isSelected ();  //HUMAN.SYSを書き込む/書き込まない
          if (fdcFormatCopyHumanSysOn) {  //HUMAN.SYSを書き込む
            fdcFormatCopyCommandXCheckBox.setEnabled (true);  //COMMAND.Xを書き込むかどうか選択できる
            fdcFormatCopyCommandXCheckBox.setSelected (fdcFormatCopyCommandXOn);  //COMMAND.Xを書き込む/書き込まない
          } else {  //HUMAN.SYSを書き込まない
            fdcFormatCopyCommandXCheckBox.setEnabled (false);  //COMMAND.Xを書き込むかどうか選択できない
            fdcFormatCopyCommandXCheckBox.setSelected (false);  //COMMAND.Xを書き込まない
          }
          break;
        case "COMMAND.X":
          fdcFormatCopyCommandXOn = fdcFormatCopyCommandXCheckBox.isSelected ();  //COMMAND.Xを書き込む/書き込まない
          break;
        case "x86-safe":  //x86 セーフ
          fdcFormatX86SafeOn = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        }
      }
    };
    //ファイルチューザー
    fdcFormatFileChooser = new JFileChooser2 (fdcLastFile);
    fdcFormatFileChooser.setFileFilter (fdcFormatFileFilter);
    //fdcFormatFileChooser.setMultiSelectionEnabled (true);  //複数選択可能
    fdcFormatFileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
    fdcFormatFileChooser.addActionListener (listener);
    //ダイアログ
    ButtonGroup mediaGroup = new ButtonGroup ();
    fdcFormatDialog = Multilingual.mlnTitle (
      XEiJ.createModalDialog (
        XEiJ.frmFrame,
        "Create New Floppy Disk Images",
        XEiJ.createBorderPanel (
          0, 0,
          XEiJ.createVerticalBox (
            fdcFormatFileChooser,
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              XEiJ.createVerticalBox (
                XEiJ.createHorizontalBox (
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HD,   "2HD (1232KB)", listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HC,   "2HC (1200KB)", listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD8,  "2DD (640KB)",  listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD9,  "2DD (720KB)",  listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HQ,   "2HQ (1440KB)", listener)
                  ),
                XEiJ.createHorizontalBox (
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD10, "2DD (800KB)",  listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HDE,  "2HDE(1440KB)", listener),
                  XEiJ.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HS,   "2HS (1440KB)", listener),
                  Box.createHorizontalGlue (),
                  fdcFormatX86SafeCheckBox = XEiJ.setEnabled (
                    Multilingual.mlnText (XEiJ.createCheckBox (fdcFormatMedia == FDMedia.FDM_2HQ && fdcFormatX86SafeOn, "x86-safe", listener), "ja", "x86 セーフ"),
                    fdcFormatMedia == FDMedia.FDM_2HQ),
                  Box.createHorizontalStrut (12)
                  )
                ),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            XEiJ.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              //Multilingual.mlnText (XEiJ.createCheckBox (fdcFormatCopySystemFiles, "Copy System Files", listener), "ja", "システムファイルを転送する"),
              fdcFormatCopyHumanSysCheckBox = XEiJ.createCheckBox (fdcFormatCopyHumanSysOn, "HUMAN.SYS", listener),
              Box.createHorizontalStrut (12),
              fdcFormatCopyCommandXCheckBox = XEiJ.setEnabled (
                XEiJ.createCheckBox (fdcFormatCopyHumanSysOn && fdcFormatCopyCommandXOn, "COMMAND.X", listener),
                fdcFormatCopyHumanSysOn),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Start Formatting", KeyEvent.VK_F, listener), "ja", "フォーマットを開始する"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (XEiJ.createButton ("Cancel", KeyEvent.VK_C, listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "フロッピーディスクイメージの新規作成");
  }  //fdcMakeFormatDialog()

  //fdcOpenFormatDialog ()
  //  フォーマットダイアログを開く
  public static void fdcOpenFormatDialog () {
    if (fdcFormatDialog == null) {
      fdcMakeFormatDialog ();
    }
    fdcFormatFileChooser.setSelectedFile (fdcLastFile);  //最後にアクセスしたファイル
    fdcFormatFileChooser.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
    fdcFormatDialog.setVisible (true);
  }  //fdcOpenFormatDialog()

  //success = fdcFormatFiles (list)
  //  フロッピーディスクをフォーマットする
  //  コマンドラインのみ
  public static boolean fdcFormatFiles (File[] list) {
    //フローピーディスクのフォーマットデータを作る
    byte[] bb = new byte[fdcFormatMedia.fdmBytesPerDisk];
    //if (!fdcFormatMedia.fdmMakeFormatData (bb, fdcFormatCopySystemFiles, fdcFormatX86SafeOn)) {
    if (!fdcFormatMedia.fdmMakeFormatData (bb, fdcFormatCopyHumanSysOn, fdcFormatCopyCommandXOn, fdcFormatX86SafeOn)) {
      return false;
    }
    //書き出す
    int u = 0;
    for (File file : list) {
      String path = file.getPath ();
      if (true) {
        String upperPath = path.toUpperCase ();
        boolean extNotSpecified = true;
        for (String mediaExt : fdcFormatMedia.fdmExtensionArray) {
          if (upperPath.endsWith (mediaExt)) {  //適切な拡張子が指定されている
            extNotSpecified = false;
            break;
          }
        }
        if (extNotSpecified) {  //適切な拡張子が指定されていない
          if (!path.endsWith (".")) {
            path += ".";
          }
          path += fdcFormatMedia.fdmExtensionArray[0].toLowerCase ();
        }
      }
      if (fdcIsInsertedPath (path)) {  //既に挿入されている
        return false;
      }
      if (!XEiJ.ismSave (bb, 0, (long) bb.length, path, true)) {
        return false;
      }
      fdcLastFile = file;
      //空いているユニットがあれば挿入する
      while (u < FDC_MAX_UNITS) {
        FDUnit unit = fdcUnitArray[u++];  //ユニット
        if (unit.fduIsConnected () && !unit.fduIsInserted ()) {  //接続されていて挿入されていない
          unit.insert (path);  //挿入する
          break;  //挿入に失敗しても終了する
        }
      }
    }
    return true;
  }  //fdcFormatFiles(File[])

  //d = fdcPeekStatus ()
  //  pbz (0x00e94001)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static int fdcPeekStatus () {
    return (fdcStatus |
            (fdcUnitArray[3].fduBusy ? FDC_D3B : 0) |
            (fdcUnitArray[2].fduBusy ? FDC_D2B : 0) |
            (fdcUnitArray[1].fduBusy ? FDC_D1B : 0) |
            (fdcUnitArray[0].fduBusy ? FDC_D0B : 0));
  }  //fdcPeekStatus()

  //d = fdcReadStatus ()
  //  rbz (0x00e94001)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static int fdcPrevReadStatus;
  public static int fdcReadStatus () {
    int d = (fdcStatus |
             (fdcUnitArray[3].fduBusy ? FDC_D3B : 0) |
             (fdcUnitArray[2].fduBusy ? FDC_D2B : 0) |
             (fdcUnitArray[1].fduBusy ? FDC_D1B : 0) |
             (fdcUnitArray[0].fduBusy ? FDC_D0B : 0));
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      if (fdcPrevReadStatus == d) {
        System.out.print ('=');
      } else {
        fdcPrevReadStatus = d;
        System.out.printf ("%08x:%d fdcReadStatus(0x00e94001)=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d,D3B=%d,D2B=%d,D1B=%d,D0B=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8, d,
                           d >> 7,
                           d >> 6 & 1, (d >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                           d >> 5 & 1,
                           d >> 4 & 1,
                           d >> 3 & 1,
                           d >> 2 & 1,
                           d >> 1 & 1,
                           d      & 1);
      }
    }
    return d;
  }  //fdcReadStatus

  //d = fdcPeekData ()
  //  pbz (0x00e94003)
  //  FDC データ/コマンド
  public static int fdcPeekData () {
    return (fdcReadHandle == null ? 0 :  //Read中でない
            fdcReadHandle[fdcIndex] & 255);
  }  //fdcPeekData

  //d = fdcReadData ()
  //  rbz (0x00e94003)
  //  FDC データ/コマンド
  public static int fdcReadData () {
    if (fdcReadHandle == null) {  //Read中でない
      if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcReadData(0x00e94003=???)=0x00\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      return 0;
    }
    int d = fdcReadHandle[fdcIndex] & 255;
    if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x:%d fdcReadData(0x00e94003=%s[0x%08x])=0x%02x\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           fdcReadHandle == fdcResultBuffer ? "fdcResultBuffer" :
                           fdcReadHandle == fdcTempBuffer ? "fdcTempBuffer" :
                           "fduImage",
                           fdcIndex, d);
      }
    }
    fdcIndex++;
    if (fdcIndex < fdcLimit) {  //継続
      if (fdcReadHandle != fdcResultBuffer) {  //E-Phaseのとき
        HD63450.dmaFallPCL (0);  //DMA転送継続
      }
    } else if (fdcReadHandle != fdcResultBuffer) {  //E-Phaseが終了した
      FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];
      switch (fdcCommandNumber) {
      case 0x06:  //0x06  READ DATA
        unit.fduFinishReadData ();
        break;
      }
    } else {  //R-Phaseが終了した
      int commandNumber = fdcCommandNumber;
      fdcCPhase ();  //C-Phaseに戻る
      fdcInterruptRequest &= ~(FDC_TRANSFER_REQUEST * 15);  //転送終了割り込み要求を終了する
      if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
      }
      XEiJ.ioiFdcFall ();  //FDC割り込み要求を終了する
      if (fdcInterruptRequest != 0) {  //割り込み要求が残っているとき
        XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
      }
    }
    return d;
  }  //fdcReadData

  //d = fdcPeekDriveStatus ()
  //  pbz (0x00e94005)
  //  FDD 状態(挿入|誤挿入|000000)/機能(点滅|排出禁止|排出|-|選択####)
  public static int fdcPeekDriveStatus () {
    return (fdcDriveLastSelected == null ? 0 :  //ドライブが選択されていない
            fdcDriveLastSelected.fduDriveStatus ());
  }  //fdcPeekDriveStatus

  //d = fdcReadDriveStatus ()
  //  rbz (0x00e94005)
  //  FDD 状態(挿入|誤挿入|000000)/機能(点滅|排出禁止|排出|-|選択####)
  public static int fdcReadDriveStatus () {
    int d = (fdcDriveLastSelected == null ? 0 :  //ドライブが選択されていない
             fdcDriveLastSelected.fduDriveStatus ());
    if (FDC_DEBUG_CONTROL && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcReadDriveStatus(0x00e94005)=0x%02x(挿入=%d,誤挿入=%d)\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, d,
                         d >> 7,
                         d >> 6 & 1);
    }
    return d;
  }  //fdcReadDriveStatus

  //fdcWriteCommand (d)
  //  wb (0x00e94001, d)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static void fdcWriteCommand (int d) {
    d &= 255;
    if (fdcWriteHandle != fdcCommandBuffer) {  //C-Phaseでない
      if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcWriteCommand(0x00e94001=???,0x%02x)\n", XEiJ.regPC0, XEiJ.regSRI >> 8, d);
      }
      return;
    }
    fdcWriteHandle[fdcIndex] = (byte) d;
    if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x:%d fdcWriteCommand(0x00e94001=%s[0x%08x],0x%02x)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           "fdcCommandBuffer",
                           fdcIndex, d);
      }
    }
    fdcIndex++;
    if (fdcLimit <= fdcIndex) {  //C-Phaseが終了した
      if (fdcCommandNumber < 0) {  //C-Phaseの1バイト目のとき
        //コマンドが入力されたらポーリングティッカーを停止
        TickerQueue.tkqRemove (fdcPollingTicker);
        fdcCommandNumber = fdcCommandBuffer[0] & 31;
        fdcLimit = FDC_COMMAND_LENGTH[fdcCommandNumber];  //コマンドの長さ
        if (1 < fdcLimit) {  //2バイト以上のコマンドなのでC-Phaseを継続
          fdcStatus |= FDC_CB;  //2バイト目からCBをセットする
          if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
            System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                               XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                               fdcStatus >> 7,
                               fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                               fdcStatus >> 5 & 1,
                               fdcStatus >> 4 & 1);
          }
          //HD63450.dmaFallPCL (0);  //DMA転送継続
          return;
        }
        //1バイトのコマンドなのでC-Phaseが終了した
      }
      //HD63450.dmaRisePCL (0);
      if (FDC_DEBUG_COMMAND && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC %s(", XEiJ.regPC0, XEiJ.regSRI >> 8, FDC_COMMAND_NAME[fdcCommandNumber]);
        for (int i = 0; i < fdcLimit; i++) {
          if (i > 0) {
            System.out.print (",");
          }
          System.out.printf ("0x%02x", fdcCommandBuffer[i] & 255);
        }
        System.out.println (")");
      }
      FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];  //ユニットを指定しないコマンドでは無意味だがエラーになることはないので問題ない
      switch (fdcCommandNumber) {
        //case 0x02:  //0x02  READ DIAGNOSTIC
        //  unit.fduCommandReadDiagnostic ();
        //  break;
      case 0x03:  //0x03  SPECIFY
        fdcCommandSpecify ();
        break;
      case 0x04:  //0x04  SENSE DEVICE STATUS
        unit.fduCommandSenseDeviceStatus ();
        break;
      case 0x05:  //0x05  WRITE DATA
        unit.fduCommandWriteData ();
        break;
      case 0x06:  //0x06  READ DATA
        unit.fduCommandReadData ();
        break;
      case 0x07:  //0x07  RECALIBRATE
        unit.fduCommandRecalibrate ();
        break;
      case 0x08:  //0x08  SENSE INTERRUPT STATUS
        fdcCommandSenseInterruptStatus ();
        break;
        //case 0x09:  //0x09  WRITE DELETED DATA
        //  unit.fduCommandWriteDeletedData ();
        //  break;
      case 0x0a:  //0x0a  READ ID
        unit.fduCommandReadId ();
        break;
        //case 0x0c:  //0x0c  READ DELETED DATA
        //  unit.fduCommandReadDeletedData ();
        //  break;
      case 0x0d:  //0x0d  WRITE ID
        unit.fduCommandWriteId ();
        break;
      case 0x0f:  //0x0f  SEEK
        unit.fduCommandSeek ();
        break;
      case 0x11:  //0x11  SCAN EQUAL
        unit.fduCommandScan ();
        break;
        //case 0x14:  //0x14  RESET STANDBY
        //  fdcCommandResetStandby ();
        //  break;
        //case 0x15:  //0x15  SET STANDBY
        //  fdcCommandSetStandby ();
        //  break;
      case 0x16:  //0x16  SOFTWARE RESET
        fdcCommandSoftwareReset ();
        break;
      case 0x19:  //0x19  SCAN LOW OR EQUAL
        unit.fduCommandScan ();
        break;
      case 0x1d:  //0x1d  SCAN HIGH OR EQUAL
        unit.fduCommandScan ();
        break;
        //case 0x00:  //0x00  INVALID
        //case 0x01:  //0x01  INVALID
        //case 0x0b:  //0x0b  INVALID
        //case 0x0e:  //0x0e  INVALID
        //case 0x10:  //0x10  INVALID
        //case 0x12:  //0x12  INVALID
        //case 0x13:  //0x13  INVALID
        //case 0x17:  //0x17  INVALID
        //case 0x18:  //0x18  INVALID
        //case 0x1a:  //0x1a  INVALID
        //case 0x1b:  //0x1b  INVALID
        //case 0x1c:  //0x1c  INVALID
        //case 0x1e:  //0x1e  INVALID
        //case 0x1f:  //0x1f  INVALID
      default:  //INVALID
        fdcCommandInvalid ();
      }
    }  //C-Phaseが終了した
  }  //fdcWriteCommand(int)

  //fdcWriteData (d)
  //  wb (0x00e94003, d)
  //  FDC データ/コマンド
  public static void fdcWriteData (int d) {
    d &= 255;
    if (fdcWriteHandle == null) {  //Write中でない
      if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcWriteData(0x00e94003=???,0x%02x)\n", XEiJ.regPC0, XEiJ.regSRI >> 8, d);
      }
      return;
    }
    fdcWriteHandle[fdcIndex] = (byte) d;
    if (FDC_DEBUG_TRANSFER && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x:%d fdcWriteData(0x00e94003=%s[0x%08x],0x%02x)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           fdcWriteHandle == fdcCommandBuffer ? "fdcCommandBuffer" :
                           fdcWriteHandle == fdcTempBuffer ? "fdcTempBuffer" :
                           "fduImage",
                           fdcIndex, d);
      }
    }
    fdcIndex++;
    if (fdcIndex < fdcLimit) {  //継続
      if (fdcWriteHandle != fdcCommandBuffer) {  //E-Phaseのとき
        //外部転送要求モードでDMAの動作を開始してからFDCにコマンドが送られてくるので、
        //C-PhaseのときDMAに転送要求を出してはならない
        HD63450.dmaFallPCL (0);  //DMA転送継続
      }
    } else if (fdcWriteHandle == fdcCommandBuffer) {  //C-Phaseが終了した
      if (fdcCommandNumber < 0) {  //C-Phaseの1バイト目のとき
        //コマンドが入力されたらポーリングティッカーを停止
        TickerQueue.tkqRemove (fdcPollingTicker);
        fdcCommandNumber = fdcCommandBuffer[0] & 31;
        fdcLimit = FDC_COMMAND_LENGTH[fdcCommandNumber];  //コマンドの長さ
        if (1 < fdcLimit) {  //2バイト以上のコマンドなのでC-Phaseを継続
          fdcStatus |= FDC_CB;  //2バイト目からCBをセットする
          if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
            System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                               XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                               fdcStatus >> 7,
                               fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                               fdcStatus >> 5 & 1,
                               fdcStatus >> 4 & 1);
          }
          //HD63450.dmaFallPCL (0);  //DMA転送継続
          return;
        }
        //1バイトのコマンドなのでC-Phaseが終了した
      }
      //HD63450.dmaRisePCL (0);
      if (FDC_DEBUG_COMMAND && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC %s(", XEiJ.regPC0, XEiJ.regSRI >> 8, FDC_COMMAND_NAME[fdcCommandNumber]);
        for (int i = 0; i < fdcLimit; i++) {
          if (i > 0) {
            System.out.print (",");
          }
          System.out.printf ("0x%02x", fdcCommandBuffer[i] & 255);
        }
        System.out.println (")");
      }
      FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];  //ユニットを指定しないコマンドでは無意味だがエラーになることはないので問題ない
      switch (fdcCommandNumber) {
        //case 0x02:  //0x02  READ DIAGNOSTIC
        //  unit.fduCommandReadDiagnostic ();
        //  break;
      case 0x03:  //0x03  SPECIFY
        fdcCommandSpecify ();
        break;
      case 0x04:  //0x04  SENSE DEVICE STATUS
        unit.fduCommandSenseDeviceStatus ();
        break;
      case 0x05:  //0x05  WRITE DATA
        unit.fduCommandWriteData ();
        break;
      case 0x06:  //0x06  READ DATA
        unit.fduCommandReadData ();
        break;
      case 0x07:  //0x07  RECALIBRATE
        unit.fduCommandRecalibrate ();
        break;
      case 0x08:  //0x08  SENSE INTERRUPT STATUS
        fdcCommandSenseInterruptStatus ();
        break;
        //case 0x09:  //0x09  WRITE DELETED DATA
        //  unit.fduCommandWriteDeletedData ();
        //  break;
      case 0x0a:  //0x0a  READ ID
        unit.fduCommandReadId ();
        break;
        //case 0x0c:  //0x0c  READ DELETED DATA
        //  unit.fduCommandReadDeletedData ();
        //  break;
      case 0x0d:  //0x0d  WRITE ID
        unit.fduCommandWriteId ();
        break;
      case 0x0f:  //0x0f  SEEK
        unit.fduCommandSeek ();
        break;
      case 0x11:  //0x11  SCAN EQUAL
        unit.fduCommandScan ();
        break;
        //case 0x14:  //0x14  RESET STANDBY
        //  fdcCommandResetStandby ();
        //  break;
        //case 0x15:  //0x15  SET STANDBY
        //  fdcCommandSetStandby ();
        //  break;
      case 0x16:  //0x16  SOFTWARE RESET
        fdcCommandSoftwareReset ();
        break;
      case 0x19:  //0x19  SCAN LOW OR EQUAL
        unit.fduCommandScan ();
        break;
      case 0x1d:  //0x1d  SCAN HIGH OR EQUAL
        unit.fduCommandScan ();
        break;
        //case 0x00:  //0x00  INVALID
        //case 0x01:  //0x01  INVALID
        //case 0x0b:  //0x0b  INVALID
        //case 0x0e:  //0x0e  INVALID
        //case 0x10:  //0x10  INVALID
        //case 0x12:  //0x12  INVALID
        //case 0x13:  //0x13  INVALID
        //case 0x17:  //0x17  INVALID
        //case 0x18:  //0x18  INVALID
        //case 0x1a:  //0x1a  INVALID
        //case 0x1b:  //0x1b  INVALID
        //case 0x1c:  //0x1c  INVALID
        //case 0x1e:  //0x1e  INVALID
        //case 0x1f:  //0x1f  INVALID
      default:  //INVALID
        fdcCommandInvalid ();
      }
    } else {  //E-Phaseが終了した
      FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];
      switch (fdcCommandNumber) {
      case 0x05:  //0x05  WRITE DATA
        unit.fduFinishWriteData ();
        break;
      case 0x0d:  //0x0d  WRITE ID
        unit.fduFinishWriteId ();
        break;
      case 0x11:  //0x11  SCAN EQUAL
        unit.fduFinishScanEqual ();
        break;
      case 0x19:  //0x19  SCAN LOW OR EQUAL
        unit.fduFinishScanLowOrEqual ();
        break;
      case 0x1d:  //0x1d  SCAN HIGH OR EQUAL
        unit.fduFinishScanHighOrEqual ();
        break;
      }
    }
  }  //fdcWriteData(int)

  //fdcWriteDriveControl (d)
  //  wb (0x00e94005, d)
  //  FDD 状態(挿入|誤挿入|------)/機能(点滅|排出禁止|排出|-|選択####)
  public static void fdcWriteDriveControl (int d) {
    //0x00e94005にライトすると0x00e9c001のFDD割り込みステータスがクリアされる
    XEiJ.ioiFddFall ();
    d &= 255;
    if (FDC_DEBUG_CONTROL && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcWriteDriveControl(0x00e94005,0x%02x(点滅=%d,排出禁止=%d,排出=%d,選択3=%d,選択2=%d,選択1=%d,選択0=%d))\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, d,
                         d >> 7,
                         d >> 6 & 1,
                         d >> 5 & 1,
                         d >> 3 & 1,
                         d >> 2 & 1,
                         d >> 1 & 1,
                         d      & 1);
    }
    //bit0-3で選択されたドライブについてbit7=点滅,bit6=排出禁止,bit5=排出
    int u = Integer.numberOfTrailingZeros (d & 15);  //選択されたドライブの番号。なければ32
    if (u < 4) {
      FDUnit unit = fdcUnitArray[u];  //ユニット
      if (unit.fduIsConnected ()) {  //接続されている
        unit.fduDriveControl (d);
      }
      fdcDriveLastSelected = unit;
    } else {
      fdcDriveLastSelected = null;
    }
  }  //fdcWriteDriveControl

  //fdcWriteDriveSelect (d)
  //  wb (0x00e94007, d)
  //  FDD 選択(モータON|--|2DD|--|ドライブ##)
  public static void fdcWriteDriveSelect (int d) {
    XEiJ.ioiFddFall ();
    d &= 255;
    if (FDC_DEBUG_MOTOR && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcWriteDriveSelect(0x00e94007,0x%02x(モータON=%d,2DD=%d,ドライブ=%d))\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, d,
                         d >> 7,
                         d >> 4 & 1,
                         d      & 3);
    }
    FDUnit unit = fdcUnitArray[d & 3];  //ユニット
    if (unit.fduIsConnected () &&  //接続されている
        unit.fduIsInserted ()) {  //挿入されている
      unit.fduDoubleDensity = d << 31 - 4 < 0;
      unit.fduSetMotorOn ((byte) d < 0);
    }
  }  //fdcWriteDriveSelect

  //fdcSetEnforcedReady (enforcedReady)
  //  強制レディ状態(YM2151のCT2が1)の設定
  public static void fdcSetEnforcedReady (boolean enforcedReady) {
    if (fdcEnforcedReady != enforcedReady) {
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcEnforcedReady=%b->%b\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           fdcEnforcedReady, enforcedReady);
      }
      fdcEnforcedReady = enforcedReady;
    }
  }  //fdcSetEnforcedReady(boolean)

  //fdcCommandSpecify ()
  //  0x03  SPECIFY
  //    タイマとNon-DMAモードの設定
  //  C-Phase
  //    [0]          CMD   Command           0x03=SPECIFY
  //    [1] bit7-4   SRT   Step Rate Time    Seekコマンドのステップパルス(シリンダ移動)の間隔。標準1ms単位、ミニ2ms単位。16から引く
  //        bit3-0   HUT   Head Unload Time  Read/Writeコマンド終了後のヘッドアンロードまでの時間。標準16ms単位、ミニ32ms単位。16倍する
  //    [2] bit7-1   HLT   Head Load Time    ヘッドロード後の安定待ち時間。標準2ms単位、ミニ4ms単位。2倍する
  //        bit0     ND    Non-DMA Mode      Read/WriteコマンドのE-Phaseについて0=DMAモード,1=Non-DMAモード
  public static void fdcCommandSpecify () {
    int srt = fdcCommandBuffer[1] >> 4 & 15;  //SRT
    int hut = fdcCommandBuffer[1] & 15;  //HUT
    int hlt = fdcCommandBuffer[2] >> 1 & 127;  //HLT
    int nd = fdcCommandBuffer[2] & 1;  //ND
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      System.out.printf ("%08x:%d   SRT=%d,HUT=%d,HLT=%d,ND=%d\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8,
                         srt, hut, hlt, nd);
    }
    fdcStepRateTime = srt;  //SRT
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSpecify

  //fdcCommandSenseInterruptStatus ()
  //  0x08  SENSE INTERRUPT STATUS
  //    リザルトステータス0(ST0)の引き取り
  //  C-Phase
  //    [0]          CMD   Command          0x08=SENSE INTERRUPT STATUS
  //    [1]  bit2    HD    Head Address     サイド
  //         bit1-0  US    Unit Select      ユニット
  //  R-Phase
  //    [0]          ST0   Status 0         リザルトステータス0
  //    [1]          PCN   Present Cylinder Number  現在のシリンダ
  //
  //  SENSE INTERRUPT STATUS
  //    R-Phaseを開始するとき
  //      リザルトステータスの割り込み要因がInvalid Commandのとき
  //        シーク終了/状態遷移割り込み要求フラグが1つでもONのとき
  //          シーク終了/状態遷移割り込み要求フラグがONの割り込みを1つ選ぶ
  //            (ユニット番号が小さい方が優先順位が高い)
  //          選んだ割り込みの割り込み要求フラグをOFFにする
  //          選んだ割り込みがシーク終了割り込みのとき
  //            強制レディ状態で接続されていないとき
  //              リザルトステータスに異常終了とシーク終了とトラック0非検出を設定する
  //            ノットレディのとき
  //              リザルトステータスに異常終了とシーク終了とノットレディを設定する
  //            レディのとき
  //              リザルトステータスに正常終了とシーク終了を設定する
  //          選んだ割り込みが状態遷移割り込みのとき
  //            ノットレディのとき
  //              リザルトステータスに状態遷移とノットレディを設定する
  //            レディのとき
  //              リザルトステータスに状態遷移を設定する
  //
  public static void fdcCommandSenseInterruptStatus () {
    if (fdcResultStatus == FDC_ST0_IC) {  //リザルトステータスの割り込み要因がInvalid Commandのとき
      if (fdcInterruptRequest != 0) {  //割り込み要求があるとき
        fdcInterruptRequest &= ~(FDC_TRANSFER_REQUEST * 15);  //転送終了割り込みをキャンセルする
        if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
          System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
        }
        int i = Integer.numberOfTrailingZeros (fdcInterruptRequest);  //割り込みを選ぶ
        fdcInterruptRequest &= ~(1 << i);  //選んだ割り込みの割り込み要求フラグをOFFにする
        if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
          System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
        }
        int u = i & 3;  //ユニット番号
        FDUnit unit = fdcUnitArray[u];  //ユニット
        if ((FDC_SEEK_REQUEST * 15 & 1 << i) != 0) {  //選んだ割り込みがシーク終了割り込みのとき
          if (fdcEnforcedReady && !unit.fduIsConnected ()) {  //強制レディ状態で接続されていないとき
            fdcResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_EC | u << 24;  //リザルトステータスに異常終了とシーク終了とトラック0非検出を設定する
          } else if (!unit.fduIsReady ()) {  //ノットレディのとき
            fdcResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_NR | u << 24;  //リザルトステータスに異常終了とシーク終了とノットレディを設定する
          } else {  //レディのとき
            fdcResultStatus = FDC_ST0_NT | FDC_ST0_SE | u << 24;  //リザルトステータスに正常終了とシーク終了を設定する
          }
        } else if ((FDC_MOTOR_REQUEST * 15 & 1 << i) != 0) {  //選んだ割り込みが状態遷移割り込みのとき
          if (!unit.fduIsReady ()) {  //ノットレディのとき
            fdcResultStatus = FDC_ST0_AI | FDC_ST0_NR | u << 24;  //リザルトステータスに状態遷移とノットレディを設定する
          } else {  //レディのとき
            fdcResultStatus = FDC_ST0_AI | u << 24;  //リザルトステータスに状態遷移を設定する
          }
        }
      }
    }
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      fdcPrintResultStatus (fdcResultStatus);
    }
    fdcResultBuffer[0] = (byte) (fdcResultStatus >> 24);  //ST0
    if (fdcResultStatus == FDC_ST0_IC) {  //Invalid Command
      fdcRPhase (1);
    } else {  //Invalid Command以外
      fdcResultBuffer[1] = (byte) fdcUnitArray[fdcResultStatus >> 24 & 3].fduPCN;  //PCN
      fdcRPhase (2);
      fdcResultStatus = FDC_ST0_IC;  //2回目以降はInvalid Commandと同じ動作になる
    }
  }  //fdcCommandSenseInterruptStatus()

  //fdcCommandResetStandby ()
  //  0x14  RESET STANDBY
  //    スタンバイ状態の解除
  public static void fdcCommandResetStandby () {
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandResetStandby()

  //fdcCommandSetStandby ()
  //  0x15  SET STANDBY
  //    スタンバイ状態への移行
  public static void fdcCommandSetStandby () {
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSetStandby()

  //fdcCommandSoftwareReset ()
  //  0x16  SOFTWARE RESET
  //    FDCの初期化
  public static void fdcCommandSoftwareReset () {
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSoftwareReset()

  //fdcCommandInvalid ()
  //  0x00  INVALID
  //  0x01  INVALID
  //  0x0b  INVALID
  //  0x0e  INVALID
  //  0x10  INVALID
  //  0x12  INVALID
  //  0x13  INVALID
  //  0x17  INVALID
  //  0x18  INVALID
  //  0x1a  INVALID
  //  0x1b  INVALID
  //  0x1c  INVALID
  //  0x1e  INVALID
  //  0x1f  INVALID
  //    不正なコマンドまたはSENSE INTERRUPT STATUSの不正使用
  public static void fdcCommandInvalid () {
    fdcResultStatus = FDC_ST0_IC;  //Invalid Command
    fdcResultBuffer[0] = (byte) (fdcResultStatus >> 24);  //ST0
    fdcRPhase (1);
  }  //fdcCommandInvalid

  //fdcCPhase ()
  //  C-Phaseに戻る
  public static void fdcCPhase () {
    if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
      System.out.printf ("%08x:%d FDC C-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
    }
    fdcStatus = FDC_RQM | FDC_MPU_TO_FDC;  //2バイト目からCBをセットする
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                         fdcStatus >> 7,
                         fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                         fdcStatus >> 5 & 1,
                         fdcStatus >> 4 & 1);
    }
    fdcReadHandle = null;
    fdcWriteHandle = fdcCommandBuffer;  //C-Phase
    fdcIndex = fdcStart = 0;
    fdcLimit = 1;  //C-Phaseの1バイト目
    fdcCommandNumber = -1;  //C-Phaseの1バイト目
    //C-Phaseに戻ったらポーリングティッカーを停止して50μs後に再開する
    TickerQueue.tkqAdd (fdcPollingTicker, XEiJ.mpuClockTime + FDC_POLLING_DELAY);  //ポーリングティッカーをキューに入れて1ms後に呼び出す
  }  //fdcCPhase()

  //fdcRPhase (limit)
  //  R-Phaseに移行する
  public static void fdcRPhase (int limit) {
    if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
      System.out.printf ("%08x:%d FDC R-Phase (", XEiJ.regPC0, XEiJ.regSRI >> 8);
      for (int i = 0; i < limit; i++) {
        if (i != 0) {
          System.out.print (",");
        }
        System.out.printf ("0x%02x", fdcResultBuffer[i] & 255);
      }
      System.out.println (")");
    }
    fdcStatus = FDC_RQM | FDC_FDC_TO_MPU | FDC_CB;
    if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
      System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                         XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                         fdcStatus >> 7,
                         fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                         fdcStatus >> 5 & 1,
                         fdcStatus >> 4 & 1);
    }
    fdcReadHandle = fdcResultBuffer;  //R-Phase
    fdcWriteHandle = null;
    fdcIndex = fdcStart = 0;
    fdcLimit = limit;
  }  //fdcRPhase(int)

  public static void fdcPrintResultStatus (int status) {
    int ic = status >> 24 + 6 & 3;
    System.out.printf ("%08x:%d fdcResultStatus=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, status);
    System.out.printf ("%08x:%d   ST0=0x%02x(IC=%d(%s),SE=%d,EC=%d,NR=%d,HD=%d,US=%d)\n",
                       XEiJ.regPC0, XEiJ.regSRI >> 8,
                       status >> 24 & 255,
                       ic, ic == 0 ? "NT" : ic == 1 ? "AT" : ic == 2 ? "IC" : "AI",  //IC
                       status >> 24 + 5 & 1,
                       status >> 24 + 4 & 1,
                       status >> 24 + 3 & 1,
                       status >> 24 + 2 & 1,
                       status >> 24 + 0 & 3);
    System.out.printf ("%08x:%d   ST1=0x%02x(EN=%d,DE=%d,OR=%d,ND=%d,NW=%d,MA=%d)\n",
                       XEiJ.regPC0, XEiJ.regSRI >> 8,
                       status >> 16 & 255,
                       status >> 16 + 7 & 1,
                       status >> 16 + 5 & 1,
                       status >> 16 + 4 & 1,
                       status >> 16 + 2 & 1,
                       status >> 16 + 1 & 1,
                       status >> 16 + 0 & 1);
    System.out.printf ("%08x:%d   ST2=0x%02x(CM=%d,DD=%d,NC=%d,SH=%d,SN=%d,BC=%d,MD=%d)\n",
                       XEiJ.regPC0, XEiJ.regSRI >> 8,
                       status >> 8 & 255,
                       status >> 8 + 6 & 1,
                       status >> 8 + 5 & 1,
                       status >> 8 + 4 & 1,
                       status >> 8 + 3 & 1,
                       status >> 8 + 2 & 1,
                       status >> 8 + 1 & 1,
                       status >> 8 + 0 & 1);
  }  //fdcPrintResultStatus(int)



  //========================================================================================
  //$$FDU FDユニット
  //  フロッピーディスクのユニット
  //
  public static class FDUnit extends AbstractUnit {

    public FDMedia fduMedia;  //メディアの種類
    public byte[] fduImage;  //イメージ
    public boolean fduWritten;  //true=書き込みがあった
    public int fduPCN;  //PCN Present Cylinder Number 現在のシリンダ。Track0信号=fduPCN==0?1:0
    public int fduPHN;  //PHN Present Head Number 現在のサイド。0～1。2HDEは最初のセクタを除いて128～129
    public int fduPRN;  //PRN Present Record Number 現在のセクタ。1～1トラックあたりのセクタ数。2HSは最初のセクタを除いて10～18
    public int fduPNN;  //PNN Present Record Length 現在のセクタ長。0～7
    public int fduEOT;  //End of Track。終了セクタ
    public int fduSTP;  //Step。1=R++,2=R+=2


    //  (接続フラグ)
    //    ユニットが接続されているときON、接続されていないときOFF
    public boolean fduIsConnected () {
      return abuConnected;
    }  //fduIsConnected()

    //  (挿入フラグ)
    //    ドライブステータス(0x00e94005)のReadのbit7。0=OFF,1=ON
    //    接続フラグがON、かつ、
    //    ディスクイメージファイルの読み込みが完了している、かつ、
    //    メディアの種類の判別が完了している
    public boolean fduIsInserted () {
      return fduIsConnected () && abuInserted && fduMedia != null;
    }  //fduIsInserted()

    //  (誤挿入フラグ)
    //    ドライブステータス(0x00e94005)のReadのbit6。0=OFF,1=ON
    //    使用しない。常にOFF
    //
    //  点滅設定フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit7。0=OFF,1=ON
    public boolean fduBlinking;  //true=点滅が設定されている

    //  排出禁止設定フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit6。0=OFF,1=ON
    public boolean fduPrevented;  //true=排出禁止が設定されている

    //  排出要求フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit5。0=OFF,1=ON

    //  モータON設定フラグ
    //    ドライブセレクト(0x00e94007)のWriteのbit7。0=OFF,1=ON
    public static final long FDU_MOTOR_ON_DELAY = XEiJ.TMR_FREQ * 10 / 1000;  //10ms。モータONからレディまでの時間
    public boolean fduMotorOn;  //true=モータ動作中
    public long fduMotorTime;  //最後にモータONまたはモータOFFされた時刻

    //  2DD設定フラグ
    //    ドライブセレクト(0x00e94007)のWriteのbit5。0=OFF,1=ON
    public boolean fduDoubleDensity;  //true=2DD設定

    //  書き込み禁止フラグ
    //    デバイスステータス(ST3)のbit6。0=書き込み許可,1=書き込み禁止
    public boolean fduIsWriteProtected () {  //true=書き込み禁止
      return abuWriteProtected;
    }  //fduIsWriteProtected()

    //  レディフラグ
    //    デバイスステータス(ST3)のbit5。0=ノットレディ,1=レディ
    public boolean fduLastReady;  //ポーリングで最後にチェックしたときのレディフラグの状態
    public boolean fduIsReady () {
      return (fduIsInserted () &&  //挿入されている
              fduMotorOn &&  //モータ動作中
              FDU_MOTOR_ON_DELAY <= XEiJ.mpuClockTime - fduMotorTime);  //モータ安定
    }  //fduIsReady()

    //  排出禁止フラグ
    public boolean fduIsPrevented () {
      return (fduIsInserted () &&  //挿入されている
              fduMotorOn);  //モータ動作中
    }  //fduIsPrevented()

    //  ユニットビジーフラグ
    //    SEEKまたはRECALIBRATEが開始されてからSENSE INTERRUPT STATUSでリザルトステータスが引き取られるまでON、それ以外はOFF
    public boolean fduBusy;  //true=ユニットビジー

    //  ターゲットシリンダ番号
    //    シークするシリンダ番号
    //  ターゲットコマンド番号
    //    シリンダ番号がターゲットシリンダ番号と一致したとき実行するコマンド
    public int fduTargetCylinder;  //ターゲットシリンダ番号
    public int fduStepRateCounter;  //ステップレートカウンタ。初期値は16-SRT

    //  アクセスランプ
    //    消灯    挿入フラグがOFF、かつ、点滅設定フラグがOFF
    //    緑点滅  挿入フラグがOFF、かつ、点滅設定フラグがON
    //    緑点灯  挿入フラグがON、かつ、ユニットビジーフラグがOFF
    //    赤点灯  挿入フラグがON、かつ、ユニットビジーフラグがON
    public static final int FDU_ACCESS_OFF            = 0;
    public static final int FDU_ACCESS_GREEN_BLINKING = 1;
    public static final int FDU_ACCESS_GREEN_ON       = 2;
    public static final int FDU_ACCESS_RED_ON         = 3;

    //  イジェクトランプ
    //    消灯    挿入フラグがOFF
    //    消灯    挿入フラグがON、かつ、排出禁止フラグがON
    //    緑点灯  挿入フラグがON、かつ、排出禁止フラグがOFF
    public static final int FDU_EJECT_OFF      = 0;
    public static final int FDU_EJECT_GREEN_ON = 1;

    //new FDUnit (number)
    //  コンストラクタ
    public FDUnit (int number) {
      super (number);
      fduMedia = null;
      fduImage = null;
      fduWritten = false;
      fduPCN = 0;
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
      }
      fduPHN = 0;
      fduPRN = 1;
      fduPNN = 3;
      fduEOT = 1;
      fduSTP = 1;

      fduBlinking = false;  //消灯する
      fduPrevented = false;  //排出を禁止しない
      fduDoubleDensity = false;  //2DD設定OFF
      fduMotorOn = false;  //モータ停止中
      if (FDC_DEBUG_MOTOR && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduMotorOn=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduMotorOn);
      }
      fduMotorTime = 0L;
      fduLastReady = false;
      fduBusy = false;  //ユニットビジーOFF
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduBusy);
      }
      fduTargetCylinder = -1;  //ターゲットシリンダ番号なし
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduTargetCylinder);
      }
      fduStepRateCounter = 16 - fdcStepRateTime;  //ステップレートカウンタ
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduStepRateCounter=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduStepRateCounter);
      }
    }

    //fduTini ()
    //  後始末
    //  イメージファイルに書き出す
    public void fduTini () {
      if (fduIsInserted ()) {
        fduFlush ();
      }
    }  //fduTini()

    //success = unit.fduFlush ()
    //  イメージファイルに書き出す
    public boolean fduFlush () {
      if (!fduIsConnected () ||  //接続されていない
          !fduIsInserted () ||  //挿入されていない
          !fduWritten) {  //書き込みがない
        return true;
      }
      if (fduIsWriteProtected ()) {  //書き込みが許可されていない
        return false;
      }
      if (!XEiJ.ismSave (fduImage, 0, (long) fduMedia.fdmBytesPerDisk, abuPath, true)) {
        return false;
      }
      fduWritten = false;
      return true;
    }  //fduFlush()

    //unit.connect (disconnectable)
    //  接続する
    @Override protected void connect (boolean disconnectable) {
      super.connect (disconnectable);
      fduImage = new byte[FDMedia.FDM_BUFFER_SIZE];
    }

    //unit.disconnect ()
    //  切り離す
    @Override protected void disconnect () {
      super.disconnect ();
      fduImage = null;
    }

    //unit.protect (unprotectable)
    //  書き込みを禁止する
    @Override protected void protect (boolean unprotectable) {
      super.protect (unprotectable);
      if (!unprotectable) {  //書き込みが許可されることはない
        fduMedia.fdmReviveFiles (fduImage);  //削除ファイルを復元する
      }
    }  //unit.protect(boolean)

    //unit.blink ()
    //  挿入されていないときLEDを点滅させる
    public void blink () {
      if (!fduIsConnected () ||  //接続されていない
          fduBlinking) {  //既にLEDが点滅している
        return;
      }
      fduBlinking = true;
      //! 表示なし
    }

    //unit.darken ()
    //  挿入されていないときLEDを消す
    public void darken () {
      if (!fduIsConnected () ||  //接続されていない
          !fduBlinking) {  //LEDが点滅していない
        return;
      }
      fduBlinking = false;
      //! 表示なし
    }

    //success = unit.eject ()
    //  イジェクトする
    @Override protected boolean eject () {
      if (!fduFlush ()) {  //イメージファイルに書き出す
        return false;
      }
      boolean inserted = fduIsInserted ();
      String path = abuPath;  //イジェクトされたイメージファイルのパス。super.eject()を呼び出す前にコピーすること
      if (!super.eject ()) {  //イジェクトする
        return false;
      }
      if (XEiJ.prgIsLocal) {  //ローカルのとき
        if (path.length () != 0) {  //挿入されていたとき
          fdcLastFile = new File (path);  //最後にアクセスしたファイルを設定する
        }
      }
      if (inserted) {
        XEiJ.ioiFddRise ();
      }
      fduMedia = null;
      fduPCN = 0;
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
      }
      fduPHN = 0;
      fduPRN = 1;
      fduPNN = 3;
      return true;
    }

    //success = unit.open ()
    //  openダイアログを開く
    @Override protected boolean open () {
      if (!super.open ()) {
        return false;
      }
      if (XEiJ.prgIsLocal) {  //ローカルのとき
        fdcOpenUnit = abuNumber;
        if (fdcOpenDialog == null) {
          fdcMakeOpenDialog ();
        }
        fdcOpenFileChooser.setSelectedFile (fdcLastFile);  //最後にアクセスしたファイル
        fdcOpenFileChooser.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
        fdcOpenDialog.setVisible (true);
      }
      return true;
    }  //unit.open()

    //success = unit.insert (path)
    //  挿入する
    public boolean insert (String path) {
      if (fdcIsInsertedPath (path)) {  //既に挿入されている
        return false;
      }
      if (!super.insert (path)) {  //挿入できなかった
        return false;
      }
      XEiJ.ioiFddRise ();
      return true;
    }  //unit.insert(String)

    //loaded = unit.load (path)
    //  読み込む
    //  挿入されていない状態で呼び出すこと
    @Override protected boolean load (String path) {
      fduMedia = FDMedia.fdmPathToMedia (path, fduImage);  //メディアの種類
      if (fduMedia == null) {  //不明
        return false;
      }
      if (FDC_DEBUG_MEDIA && fdcDebugLogOn) {
        System.out.println ("media = " + fduMedia.fdmName);
        System.out.println ("------------------------------------------------------------------------");
        fduMedia.fdmPrintInfo ();
        System.out.println ("------------------------------------------------------------------------");
      }
      if (FDMedia.fdmPathToExt (path).equals ("DIM")) {  //*.DIM
        protect (false);  //書き込み禁止
      }
      fduWritten = false;
      if (XEiJ.prgIsLocal) {  //ローカルのとき
        fdcLastFile = new File (path);  //最後にアクセスしたファイルを更新する
      }
      //XEiJ.prgMessage ("FD" + abuNumber + ": " + path);
      return true;
    }  //unit.load(String)

    //name = unit.getName ()
    //  pathからnameを作る
    public String getName () {
      return abuPath.substring (abuPath.lastIndexOf (File.separatorChar) + 1);
    }

    //d = unit.fduDriveStatus ()
    //  bit7  1=挿入
    //  bit6  1=誤挿入
    public int fduDriveStatus () {
      return fduIsInserted () ? 0x80 : 0;
    }

    //unit.fduDriveControl (d)
    //  bit7  0=消灯する,1=点滅する
    //  bit6  0=排出を許可する,1=排出を禁止する
    //  bit5  0=排出しない,1=排出する
    public void fduDriveControl (int d) {
      if (d << 31 - 5 < 0) {  //排出する
        eject ();
      }
      if (d << 31 - 6 < 0) {  //排出を禁止する
        fduPrevented = true;
        prevent ();
      } else {  //排出を許可する
        fduPrevented = false;
        allow ();
      }
      if ((byte) d < 0) {  //点滅する
        fduBlinking = true;
        blink ();
      } else {  //消灯する
        fduBlinking = false;
        darken ();
      }
    }

    //unit.fduCommandReadDiagnostic ()
    //  0x02  READ DIAGNOSTIC
    //    診断のための読み出し
    //    セクタ1から開始し、1トラック分のエラーを累積して正常終了する
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x02=READ DIAGNOSTIC
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    無意味
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックするがMPUにはDTLバイトだけ転送する
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(終了せず)  NT|ND
    //      H不一致(終了せず)  NT|ND
    //      R不一致(終了せず)  NT|ND
    //      N不一致(終了せず)  NT|ND
    //      CRC不一致(終了せず)  NT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出(終了せず)  NT|CM
    //      CRC不一致(終了せず)  NT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadDiagnostic () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d READ DIAGNOSTIC is not implemented\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcCommandInvalid ();
    }

    //unit.fduCommandSenseDeviceStatus ()
    //  0x04  SENSE DEVICE STATUS
    //    状態信号(ST3)の読み出し
    //  C-Phase
    //    [0]          CMD   Command          0x04=SENSE DEVICE STATUS
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //  R-Phase
    //    [0]          ST3   Status 3         リザルトステータス3
    public void fduCommandSenseDeviceStatus () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      int st3 = ((fduIsWriteProtected () ? FDC_ST3_WP : 0) |  //Write Protect
                 (fduIsReady () ? FDC_ST3_RY : 0) |  //Ready
                 (fduIsInserted () && fduMotorOn && fduPCN == 0 ? FDC_ST3_T0 : 0) |  //Track 0
                 //(fduIsInserted () ? fduMedia.fdmTwoSide : 0) |  //Two Side
                 (fduPHN & 1) << 2 |  //HD
                 abuNumber);  //US1,US0
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d   ST3=0x%02x(FT=%d,WP=%d,RY=%d,T0=%d,TS=%d,HD=%d,US=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           st3,
                           st3 >> 7,
                           st3 >> 6 & 1,
                           st3 >> 5 & 1,
                           st3 >> 4 & 1,
                           st3 >> 3 & 1,
                           st3 >> 2 & 1,
                           st3      & 3);
      }
      fdcResultBuffer[0] = (byte) st3;
      fdcRPhase (1);
    }

    //unit.fduCommandWriteData ()
    //  0x05  WRITE DATA
    //    データ(DAM)の書き込み
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x05=WRITE DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のときMPUからDTLバイトだけ受け取って128バイト書き込む
    //  E-Phase(MPU→FDC)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      フォールト  AT|EC
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandWriteData () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is not ready\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduIsWriteProtected ()) {  //書き込めない
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is write protected\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_NW | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduPCN = fdcCommandBuffer[2] & 255;  //C
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
      }
      fduPHN = fdcCommandBuffer[3] & 255;  //H
      fduPRN = fdcCommandBuffer[4] & 255;  //R
      fduPNN = fdcCommandBuffer[5] & 255;  //N
      fduEOT = fdcCommandBuffer[6] & 255;  //EOT
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      fduWritten = true;
      if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC E-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcStatus = FDC_RQM | FDC_MPU_TO_FDC | FDC_CB;
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                           fdcStatus >> 7,
                           fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                           fdcStatus >> 5 & 1,
                           fdcStatus >> 4 & 1);
      }
      fdcReadHandle = null;
      fdcWriteHandle = fduImage;
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
      HD63450.dmaFallPCL (0);  //DMA転送開始
    }

    //unit.fduFinishWriteData ()
    public void fduFinishWriteData () {
      if (fduPRN == fduEOT) {  //終了
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      if (fduMedia == FDMedia.FDM_2HS && fduPCN == 0 && fduPHN == 0 && fduPRN == 1) {
        fduPRN = 11;
      } else {
        fduPRN++;
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | (fduPHN << 2 | abuNumber << 24));
        return;
      }
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
    }  //unit.fduFinishWriteData()

    //unit.fduCommandReadData ()
    //  0x06  READ DATA
    //    データ(DAM)の読み出し
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DDAMをスキップ
    //         bit4-0  CMD   Command          0x06=READ DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックするがMPUにはDTLバイトだけ転送する
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadData () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is not ready\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduPCN = fdcCommandBuffer[2] & 255;  //C
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
      }
      fduPHN = fdcCommandBuffer[3] & 255;  //H
      fduPRN = fdcCommandBuffer[4] & 255;  //R
      fduPNN = fdcCommandBuffer[5] & 255;  //N
      fduEOT = fdcCommandBuffer[6] & 255;  //EOT
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC E-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcStatus = FDC_RQM | FDC_FDC_TO_MPU | FDC_CB;
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                           fdcStatus >> 7,
                           fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                           fdcStatus >> 5 & 1,
                           fdcStatus >> 4 & 1);
      }
      fdcReadHandle = fduImage;
      fdcWriteHandle = null;
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
      HD63450.dmaFallPCL (0);  //DMA転送開始
    }  //unit.fduCommandReadData

    //unit.fduFinishReadData ()
    public void fduFinishReadData () {
      if (fduPRN == fduEOT) {  //終了
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      if (fduMedia == FDMedia.FDM_2HS && fduPCN == 0 && fduPHN == 0 && fduPRN == 1) {
        fduPRN = 11;
      } else {
        fduPRN++;
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
    }  //unit.fduFinishReadData()

    //unit.fduCommandRecalibrate ()
    //  0x07  RECALIBRATE
    //    トラック0(一番外側)へのシーク
    //  C-Phase
    //    [0]          CMD   Command          0x07=RECALIBRATE
    //    [1]  bit1-0  US    Unit Select      ユニット
    //  リザルトステータス(SENSE INTERRUPT STATUSで引き取る)
    //    正常終了  NT|SE
    //    ノットレディ  AT|SE|NR
    //    トラック0非検出  AT|SE|EC
    public void fduCommandRecalibrate () {
      fduBusy = true;  //ユニットビジー
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduBusy);
      }
      fduTargetCylinder = 0;  //ターゲットシリンダ番号を設定する。既に設定されているときは上書きする
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduTargetCylinder);
      }
      fdcCPhase ();  //C-Phaseに戻る
    }  //fduCommandRecalibrate()

    //unit.fduCommandWriteDeletedData ()
    //  0x09  WRITE DELETED DATA
    //    削除データ(DDAM)の書き込み
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x09=WRITE DELETED DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のときMPUからDTLバイトだけ受け取って128バイト書き込む
    //  E-Phase(MPU→FDC)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      フォールト  AT|EC
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandWriteDeletedData () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d WRITE DELETED DATA is not implemented\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcCommandInvalid ();
    }

    //unit.fduCommandReadId ()
    //  0x0a  READ ID
    //    現在のシリンダの指定されたサイドにあるトラックでヘッドロード後最初に見つけたDEエラーやMAエラーのないセクタのIDを返す
    //    トラック上のどのセクタのIDが返るかは不定
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x0a=READ ID
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA  インデックスパルスを2回検出するまでにIDAMが見つからない
    //      CRC不一致  AT|ND  IDAMを見つけたがインデックスパルスを2回検出するまでにCRCエラーのないIDが見つからない
    public void fduCommandReadId () {
      fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is not ready\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //セクタ番号
      //  常に1を返す方法だとREAD IDを繰り返してセクタ数を確認することができない
      if (fduMedia == FDMedia.FDM_2HS) {  //2HS
        if (fduPCN == 0 && fduPHN == 0) {  //最初のトラック
          if (fduPRN == 1) {  //最初のセクタ
            fduPRN = 11;  //2番目のセクタ
          } else if (fduPRN == 18) {  //最後のセクタ
            fduPRN = 1;  //最初のセクタ
          } else {  //その他のセクタ
            fduPRN++;  //次のセクタ
          }
        } else {  //最初のトラック以外
          if (fduPRN == 18) {  //最後のセクタ
            fduPRN = 10;  //最初のセクタ
          } else {  //その他のセクタ
            fduPRN++;  //次のセクタ
          }
        }
      } else {  //2HS以外
        if (fduPRN == fduMedia.fdmSectorsPerTrack) {  //最後のセクタ
          fduPRN = 1;  //最初のセクタ
        } else {  //その他のセクタ
          fduPRN++;  //次のセクタ
        }
      }
      //サイド番号
      if (fduMedia == FDMedia.FDM_2HDE) {
        if (!(fduPCN == 0 && fduPHN == 0 && fduPRN == 1)) {  //最初のトラックの最初のセクタ以外
          fduPHN |= 128;
        }
      }
      //セクタスケール
      fduPNN = fduMedia.fdmSectorScale;
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
    }  //unit.fduCommandReadId()

    //unit.fduCommandReadDeletedData ()
    //  0x0c  READ DELETED DATA
    //    削除データ(DDAM)の読み出し
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DAMをスキップ
    //         bit4-0  CMD   Command          0x0c=READ DELETED DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックしてMPUにDTLバイトだけ渡す
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DDAM非検出  AT|MA|MD
    //      DAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadDeletedData () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d READ DELETED DATA is not implemented\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcCommandInvalid ();
    }

    //unit.fduCommandWriteId ()
    //  0x0d  WRITE ID
    //    1トラックフォーマットする。別名Format Write
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x0d=WRITE ID
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          N     Record Length    セクタ長=128<<N
    //    [3]          SC    Sector           1トラックあたりのセクタ数
    //    [4]          GPL   Gap Length       Gap3に書き込む長さ
    //    [5]          D     Data             データ部に書き込むデータ
    //  E-Phase
    //    1トラック分のID情報。4*SCバイト
    //    [0]          C     Cylinder Number  シリンダ
    //    [1]          H     Head Number      サイド
    //    [2]          R     Record Number    開始セクタ
    //    [3]          N     Record Length    セクタ長=128<<N
    //    これをSC回繰り返す
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  無意味
    //    [4]          H     Head Number      無意味
    //    [5]          R     Record Number    無意味
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    フォールト  AT|EC
    //    オーバーラン  AT|OR
    public void fduCommandWriteId () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is not ready\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduIsWriteProtected ()) {  //書き込めない
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is write protected\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_NW | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC E-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcStatus = FDC_RQM | FDC_MPU_TO_FDC | FDC_CB;
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                           fdcStatus >> 7,
                           fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                           fdcStatus >> 5 & 1,
                           fdcStatus >> 4 & 1);
      }
      fdcReadHandle = null;
      fdcWriteHandle = fdcTempBuffer;
      fdcIndex = fdcStart = 0;
      fdcLimit = (fdcCommandBuffer[3] & 255) << 2;
      HD63450.dmaFallPCL (0);  //DMA転送開始
    }  //fduCommandWriteId()

    //unit.fduFinishWriteId ()
    public void fduFinishWriteId () {
      HD63450.dmaRisePCL (0);  //DMA転送終了
      //トラック0のフォーマットでメディアを判別する
      if (fdcTempBuffer[0] == 0 && fdcTempBuffer[1] == 0) {  //pcn1==0&&phn1==0。トラック0
        int prn1 = fdcTempBuffer[2] & 255;
        int pnn1 = fdcTempBuffer[3] & 255;
        int pcn2 = fdcTempBuffer[4] & 255;
        int phn2 = fdcTempBuffer[5] & 255;
        int prn2 = fdcTempBuffer[6] & 255;
        int pnn2 = fdcTempBuffer[7] & 255;
        int sectors = fdcLimit >> 2;  //1トラックあたりのセクタ数。fdcCommandBuffer[3]&255
        FDMedia media = null;
        if (phn2 == 0 && prn1 == 1 && prn2 == 2) {
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 8) {  //8セクタ/トラック
                media = FDMedia.FDM_2HD;
              }
            } else if (pnn1 == 2 && pnn2 == 2) {  //512バイト/セクタ
              if (sectors == 15) {  //15セクタ/トラック
                media = FDMedia.FDM_2HC;
              } else if (sectors == 18) {  //18セクタ/トラック
                media = FDMedia.FDM_2HQ;
              }
            }
          } else {  //倍密度
            if (pnn1 == 2 && pnn2 == 2) {  //512バイト/セクタ
              if (sectors == 8) {  //8セクタ/トラック
                media = FDMedia.FDM_2DD8;
              } else if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2DD9;
              } else if (sectors == 10) {  //10セクタ/トラック
                media = FDMedia.FDM_2DD10;
              }
            }
          }
        } else if (phn2 == 128 && prn1 == 1 && prn2 == 2) {  //2番目のセクタのサイド番号が128
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2HDE;
              }
            }
          }
        } else if (phn2 == 0 && prn1 == 1 && prn2 == 11) {  //2番目のセクタのレコード番号が11
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2HS;
              }
            }
          }
        }
        if (media != null) {
          fduMedia = media;
        } else {
          fduMedia = FDMedia.FDM_2HD;
        }
        if (FDC_DEBUG_MEDIA && fdcDebugLogOn) {
          System.out.println ("media = " + fduMedia.fdmName);
          System.out.println ("------------------------------------------------------------------------");
          fduMedia.fdmPrintInfo ();
          System.out.println ("------------------------------------------------------------------------");
        }
      }  //if トラック0
      for (int i = 0; i < fdcLimit; i += 4) {
        fduPCN = fdcTempBuffer[i    ] & 255;
        if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
        }
        fduPHN = fdcTempBuffer[i + 1] & 255;
        fduPRN = fdcTempBuffer[i + 2] & 255;
        fduPNN = fdcTempBuffer[i + 3] & 255;
        int o = fduCalcOffset ();
        if (o < 0) {  //セクタが存在しない
          //FORMAT.Xが1トラック余分にフォーマットしようとするので、シリンダが上限+1のときはエラーにせず無視する
          if (0 < fduPCN) {
            fduPCN--;
            if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
            }
            o = fduCalcOffset ();
            fduPCN++;
            if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
              System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
            }
          }
          if (o < 0) {  //シリンダを1つ減らしたセクタも存在しない
            fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_EC | ((fduPHN & 1) << 2 | abuNumber) << 24);
            return;
          }
        } else {  //セクタが存在する
          Arrays.fill (fduImage, o, o + (128 << fduPNN), fdcCommandBuffer[5]);  //初期化データで充填する
          fduWritten = true;
        }
      }  //for i
      fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
    }  //fduFinishWriteId()

    //unit.fduCommandSeek ()
    //  0x0f  SEEK
    //    NCNがPCNと異なるときSPECIFYコマンドで指定されたStep Rate Time間隔でPCNをインクリメントまたはデクリメントすることを繰り返す
    //    NCNが範囲外でもエラーを出さない
    //  C-Phase
    //    [0]          CMD   Command          0x0f=SEEK
    //    [1]  bit1-0  US    Unit Select      ユニット
    //    [2]          NCN   New Cylinder Number
    //  E-Phase
    //    終了待ち
    //  リザルトステータス(SENSE INTERRUPT STATUSで引き取る)
    //    正常終了  NT|SE
    //    ノットレディ  AT|SE|NR
    public void fduCommandSeek () {
      int ncn = fdcCommandBuffer[2] & 255;  //New Cylinder Number
      fduBusy = true;  //ユニットビジー
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduBusy=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduBusy);
      }
      fduTargetCylinder = ncn;  //ターゲットシリンダ番号を設定する。既に設定されているときは上書きする
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduTargetCylinder=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduTargetCylinder);
      }
      fdcCPhase ();  //C-Phaseに戻る
    }  //fduCommandSeek()

    //unit.fduCommandScan ()
    //  0x11  SCAN EQUAL
    //  0x19  SCAN LOW OR EQUAL
    //  0x1d  SCAN HIGH OR EQUAL
    //    記録データと目的データを1バイトずつ8bit符号なし数値とみなして比較する(ベリファイ)
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DDAMをスキップ
    //         bit4-0  CMD   Command          0x11=SCAN EQUAL,0x19=SCAN LOW OR EQUAL,0x1d=SCAN HIGH OR EQUAL
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ。STP==2のときはRとEOTの奇偶が同じであること
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          STP   Step             条件不成立のとき1=次のセクタに進む(R++),2=次の次のセクタに進む(R+=2)
    //  E-Phase(MPU→FDC)
    //    (EOT-R)/STP+1セクタ分の目的データ
    //    条件
    //      EQUAL          目的データ==0xff||記録データ==目的データ
    //      LOW OR EQUAL   目的データ==0xff||記録データ<=目的データ
    //      HIGH OR EQUAL  目的データ==0xff||記録データ>=目的データ
    //    1セクタ分のデータがすべて条件成立したとき
    //      条件成立で終了
    //    1セクタの中に条件成立しないデータがあったとき
    //      終了セクタではないとき
    //        STPに従って次または次の次のセクタに進む
    //      終了セクタまでに1セクタ分のデータがすべて条件成立するセクタが見つからなかったとき
    //        条件不成立で終了
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    最後に比較したセクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    条件成立  EQUAL          NT|SH
    //              LOW OR EQUAL   NT
    //              HIGH OR EQUAL  NT
    //    条件不成立  NT|SN
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    public void fduCommandScan () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,is not ready\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduPCN = fdcCommandBuffer[2] & 255;  //C
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduPCN=%d\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduPCN);
      }
      fduPHN = fdcCommandBuffer[3] & 255;  //H
      fduPRN = fdcCommandBuffer[4] & 255;  //R
      fduPNN = fdcCommandBuffer[5] & 255;  //N
      fduEOT = fdcCommandBuffer[6] & 255;  //EOT
      fduSTP = fdcCommandBuffer[8] & 3;  //STP
      //E-Phase
      if (FDC_DEBUG_PHASE && fdcDebugLogOn) {
        System.out.printf ("%08x:%d FDC E-Phase\n", XEiJ.regPC0, XEiJ.regSRI >> 8);
      }
      fdcStatus = FDC_RQM | FDC_MPU_TO_FDC | FDC_CB;
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcStatus=0x%02x(RQM=%d,DIO=%d(%s),NDM=%d,CB=%d)\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8, fdcStatus,
                           fdcStatus >> 7,
                           fdcStatus >> 6 & 1, (fdcStatus >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                           fdcStatus >> 5 & 1,
                           fdcStatus >> 4 & 1);
      }
      fdcReadHandle = null;
      fdcWriteHandle = fdcTempBuffer;
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
      HD63450.dmaFallPCL (0);  //DMA転送開始
    }  //fduCommandScanEqual()

    //fduFinishScanEqual ()
    public void fduFinishScanEqual () {
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //比較
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) != d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SH | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      fdcIndex = fdcStart = 0;
      //fdcLimit = 128 << fduPNN;
    }  //fduFinishScanEqual()

    //fduFinishScanLowOrEqual ()
    public void fduFinishScanLowOrEqual () {
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) > d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
    }  //fduFinishScanLowOrEqual()

    //fduFinishScanHighOrEqual ()
    public void fduFinishScanHighOrEqual () {
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) < d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRisePCL (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
    }  //fduFinishScanHighOrEqual()

    //offset = fduCalcOffset ()
    //  指定されたセクタのオフセットを計算する。-1=範囲外
    public int fduCalcOffset () {
      int o = (fduMedia == null ? -1 :
               fduDoubleDensity == fduMedia.fdmDoubleDensity &&
               fduPNN == fduMedia.fdmSectorScale &&
               fduPCN < fduMedia.fdmCylindersPerDisk &&
               (fduMedia == FDMedia.FDM_2HDE ?
                fduPHN == (fduPCN == 0 && fduPRN == 1 ? 0 : 128) || fduPHN == 129 :
                fduPHN < fduMedia.fdmTracksPerCylinder) &&
               (fduMedia == FDMedia.FDM_2HS ?
                fduPCN == 0 && fduPHN == 0 ? fduPRN == 1 || (11 <= fduPRN && fduPRN <= 18) : 10 <= fduPRN && fduPRN <= 18 :
                1 <= fduPRN && fduPRN <= fduMedia.fdmSectorsPerTrack) ?
               fduMedia.fdmBytesPerSector * (
                 (fduPRN <= fduMedia.fdmSectorsPerTrack ? fduPRN : fduPRN - fduMedia.fdmSectorsPerTrack) - 1 +
                 fduMedia.fdmSectorsPerTrack * (
                   ((fduPHN & 1) + fduMedia.fdmTracksPerCylinder * fduPCN))) :
               -1);
      if (FDC_DEBUG_SEEK && fdcDebugLogOn) {
        System.out.printf ("%08x:%d   PCN=%d,PHN=%d,PRN=%d,PNN=%d,offset=%d\n",
                           XEiJ.regPC0, XEiJ.regSRI >> 8,
                           fduPCN, fduPHN, fduPRN, fduPNN, o);
      }
      return o;
    }  //fduCalcOffset()

    //fduEPhaseEnd (status)
    //  E-Phaseを終了してR-Phaseに移行する
    public void fduEPhaseEnd (int status) {
      if (FDC_DEBUG_STATUS && fdcDebugLogOn) {
        fdcPrintResultStatus (status);
      }
      fdcResultBuffer[0] = (byte) (status >> 24);  //ST0
      fdcResultBuffer[1] = (byte) (status >> 16);  //ST1
      fdcResultBuffer[2] = (byte) (status >> 8);  //ST2
      fdcResultBuffer[3] = (byte) fduPCN;  //C
      fdcResultBuffer[4] = (byte) fduPHN;  //H
      fdcResultBuffer[5] = (byte) fduPRN;  //R
      fdcResultBuffer[6] = (byte) fduPNN;  //N
      fdcRPhase (7);
      fdcInterruptRequest |= FDC_TRANSFER_REQUEST << abuNumber;  //転送終了割り込み
      if (FDC_DEBUG_INTERRUPT && fdcDebugLogOn) {
        System.out.printf ("%08x:%d fdcInterruptRequest=0x%08x\n", XEiJ.regPC0, XEiJ.regSRI >> 8, fdcInterruptRequest);
      }
      XEiJ.ioiFdcFall ();  //FDC割り込み要求を終了する
      XEiJ.ioiFdcRise ();  //FDC割り込みを要求する
      //!!! ここでfdcResultStatusは操作せずInvalid Commandのままにしておくこと
      //  IOCS _B_READはREAD DATAの後に割り込みがなくてもSENSE INTERRUPT STATUSを発行する
      //  そのときST0とPCNを返すとST0とST1として回収されてしまい、
      //  Human302の0x00010ceeがPCN=2をST1=2(NW)と誤認して「プロテクトをはずして、同じディスクを入れてください」が出る
    }  //fduEPhaseEnd(int)

    //fduSetMotorOn (motorOn)
    //  モータON/OFF
    //
    //  モータON/OFFのFDC割り込み
    //    0x00e94001==0x80
    //    RQM=1なので0x00e94003=0x08(SENSE INTERRUPT STATUS)
    //    0x00e94001==0x00  RQM=0,DIO=0(OUT),NDM=0,CB=0,D3B=0,D2B=0,D1B=0,D0B=0 x 2回～3回
    //    0x00e94001==0x10  RQM=0,DIO=0(OUT),NDM=0,CB=1,D3B=0,D2B=0,D1B=0,D0B=0 x 2回
    //    0x00e94001==0xd0  RQM=1,DIO=1(IN),NDM=0,CB=1,D3B=0,D2B=0,D1B=0,D0B=0
    //    0x00e94003==0xc0(モータ動作中のとき)  IC=3(AI状態遷移)
    //                0xc8(モータ停止中のとき)  IC=3(AI状態遷移),NR=1(ノットレディ)
    //    0x00e94003==0x00  PCN
    //    0x00e94001==0x80
    //
    public void fduSetMotorOn (boolean motorOn) {
      if (FDC_DEBUG_MOTOR && fdcDebugLogOn) {
        System.out.printf ("%08x:%d unit=%d,fduSetMotorOn(%b)\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, motorOn);
      }
      if (fduIsInserted () &&  //挿入されている
          fduMotorOn != motorOn) {  //モータONまたはモータOFF
        fduMotorOn = motorOn;
        if (FDC_DEBUG_MOTOR && fdcDebugLogOn) {
          System.out.printf ("%08x:%d unit=%d,fduMotorOn=%b\n", XEiJ.regPC0, XEiJ.regSRI >> 8, abuNumber, fduMotorOn);
        }
        fduMotorTime = XEiJ.mpuClockTime;
        if (motorOn && fduPrevented) {  //モータONで排出禁止が設定されているとき
          prevent ();  //排出を禁止する
        }
      }
    }  //fduSetMotorOn(boolean)


  }  //class FDUnit



}  //class FDC



