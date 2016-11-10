;========================================================================================
;  rompat13.s
;  Copyright (C) 2003-2016 Makoto Kamada
;
;  This file is part of the XEiJ (X68000 Emulator in Java).
;  You can use, modify and redistribute the XEiJ if the conditions are met.
;  Read the XEiJ License for more details.
;  http://stdkmd.com/xeij/
;========================================================================================

;----------------------------------------------------------------
;
;	ROM 1.3のパッチ
;
;----------------------------------------------------------------

	.include	iocscall.mac

	.include	def_M.equ
	.include	rompat.mac

ROM_VERSION	equ	$15160816	;ROMのバージョン



;----------------------------------------------------------------
;
;	パッチデータの先頭
;
;----------------------------------------------------------------

	.cpu	68000

	.text



;----------------------------------------------------------------
;
;	IOCS _ROMVER
;
;----------------------------------------------------------------

	PATCH_DATA	iocsRomver,$00FF0030

	move.l	#ROM_VERSION,d0

	PATCH_END



;----------------------------------------------------------------
;
;	制御レジスタの初期化とMPU/MMU/FPUのチェック
;
;----------------------------------------------------------------

	PATCH_DATA	mpuCheck,$00FF005A

;サブルーチンがアドレス変換を無効にするとスタックエリアが移動して戻ってこれなくなるのでjsrではなくjmpを使う
	jmp	mpuCheck

	PATCH_EXTRA

;----------------------------------------------------------------
;MPUチェック
;	起動直後に割り込み禁止、スタック未使用の状態でジャンプしてくる
;	ROMのTRAP#10はホットスタートするときMMUのアドレス変換の状態を確認せずに$00FF0038にジャンプしている
;	060turbo.sysがTRAP#10をフックしているのでホットスタートした場合もアドレス変換は既に無効化されているはずだが、
;	念のためアドレス変換が有効になっていた場合を考慮する
;	ROMのコードは変化しないので問題ないが、
;	ベクタテーブルとスタックエリアとワークエリアの内容はアドレス変換を無効にした瞬間に失われる
;	ホットスタートしたらスタックエリアやワークエリアを使う前にアドレス変換を無効にしなければならない
;<d5.l:0=コールドスタート(d0d1!='HotStart'),-1=ホットスタート(d0d1=='HotStart')
;<d6.l:エミュレータ割り込みベクタ([$0030.w].l)。コールドスタートのとき未定義例外処理ルーチンを指していなければ電源ON、指していればリセット
;>d7.l:MPU/MMU/FPUのチェックの結果
;	bit31:0=MMUなし,1=MMUあり
;	bit15:0=FPU/FPCPなし,1=FPU/FPCPあり
;	bit7-0:0=MC68000,1=MC68010,2=MC68020,3=MC68030,4=MC68040,6=MC68060
;>a0.l:$00000400
;>a1.l:$00FF0770
;?d0
mpuCheck:
	lea.l	dummyTrap(pc),a1
	move.l	a1,$0010.w		;[$0010.w].l:例外ベクタ$04 不当命令
	move.l	a1,$002C.w		;[$002C.w].l:例外ベクタ$0B ライン1111エミュレータ
	movea.l	sp,a1
;----------------------------------------------------------------
;MOVEC to VBR(-12346)がなければMC68000
;	MC68000かどうかのテストとVBRのクリアを同時に行う
;	不当命令例外ベクタが$0010.wにあるのはMC68000の場合とMC68010以上でVBRが0の場合
;	MC68010以上でVBRが0でないとき$0010.wを書き換えても不当命令例外を捉えられないので、
;	MC68010以上のときは最初にVBRをクリアする必要がある
;	VBRをクリアする命令はMC68000で不当命令なのでMC68000かどうかのテストを兼ねる
	.cpu	68010
	lea.l	mpuCheckDone(pc),a0
	moveq.l	#0,d7			;MC68000
	moveq.l	#0,d0
	movec.l	d0,vbr
	.cpu	68000
;----------------------------------------------------------------
;MOVEC to VBR(-12346)があってスケールファクタ(--2346)がなければMC68010
	.cpu	68020
	moveq.l	#1,d7			;MC68010
@@:	moveq.l	#1,d0			;$70,$01
	and.b	(@b-1,pc,d0.w*2),d0	;スケールファクタなし(([@b-1+1].b==$70)&1)==0,スケールファクタあり(([@b-1+1*2].b=$01)&1)==1
	beq	mpuCheckDone
	.cpu	68000
;----------------------------------------------------------------
;CALLM(--2---)があればMC68020
	.cpu	68020
	lea.l	9f(pc),a0
	callm	#0,1f(pc)
	moveq.l	#2,d7			;MC68020
;		  3  2 1 0
;		  C CE F E
	move.l	#%1__0_0_0,d0
	movec.l	d0,cacr			;命令キャッシュクリア、命令キャッシュOFF
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPUあり
@@:
;MC68851のチェックは省略
	bra	mpuCheckDone
;モジュールデスクリプタ
1:	.dc.l	%0<<13|0<<24|0<<16	;option=0,type=0,accesslevel=0
	.dc.l	2f			;モジュールエントリポインタ
	.dc.l	0			;モジュールデータ領域ポインタ
	.dc.l	0
;モジュールエントリ
2:	.dc.w	15<<12			;Rn=sp
	rtm	sp
9:	movea.l	a1,sp
	.cpu	68000
;----------------------------------------------------------------
;CALLM(--2---)がなくてMOVEC from CAAR(--23--)があればMC68030
	.cpu	68030
	lea.l	9f(pc),a0
	movec.l	caar,d0
	moveq.l	#3,d7			;MC68030
;		   D   C  B   A  9  8 765   4  3   2  1  0
;		  WA DBE CD CED FD ED     IBE CI CEI FI EI
	move.l	#%_0___0__1___0__0__0_000___0__1___0__0__0,d0
	movec.l	d0,cacr			;データキャッシュクリア、データキャッシュOFF、命令キャッシュクリア、命令キャッシュOFF
;		  F ECDBA   9   8 7654 3210 FEDC BA98 7654 3210
;		  E       SRE FCL   PS   IS  TIA  TIB  TIC  TID
	move.l	#%0_00000___0___0_1101_1000_0011_0100_0100_0000,-(sp)
	pmove.l	(sp),tc			;アドレス変換OFF
;		  FEDCBA98 76543210 F EDCB  A   9   8 7    654 3    210
;		      BASE     MASK E      CI R/W RWM   FCBASE   FCMASK
	move.l	#%00000000_00000000_0_0000__0___0___0_0____000_0____000,(sp)
	pmove.l	(sp),tt0		;透過変換OFF
	pmove.l	(sp),tt1
	addq.l	#4,sp
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPUあり
@@:	bsr	mmuCheck3		;MMUチェック(MC68030)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;MOVEC from MMUSR(----4-)があればMC68040
	.cpu	68040
	lea.l	9f(pc),a0
	movec.l	mmusr,d0
	moveq.l	#4,d7			;MC68040
;		   F EDCBA9876543210  F EDCBA9876543210
;		  DE 000000000000000 IE 000000000000000
	move.l	#%_0_000000000000000__0_000000000000000,d0
	movec.l	d0,cacr			;データキャッシュOFF、命令キャッシュOFF
	cinva	bc			;データキャッシュクリア、命令キャッシュクリア
;		  F E DCBA9876543210
;		  E P 00000000000000
	move.l	#%0_0_00000000000000,d0
	movec.l	d0,tc			;アドレス変換無効
;		  FEDCBA98 76543210 F     ED CBA  9  8 7 65 43 2 10
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	move.l	#%00000000_00000000_0_____00_000__0__0_0_00_00_0_00,d0
	movec.l	d0,itt0			;透過変換OFF
	movec.l	d0,itt1
	movec.l	d0,dtt0
	movec.l	d0,dtt1
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPUあり
@@:	bsr	mmuCheck46		;MMUチェック(MC68040/MC68060)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;MOVEC from PCR(-----6)があればMC68060
	.cpu	68060
	lea.l	9f(pc),a0
	movec.l	pcr,d1
	moveq.l	#6,d7			;MC68060
;		    F   E   D   C   B A98   7    6    5 43210   F   E   D CBA9876543210
;		  EDC NAD ESB DPI FOC     EBC CABC CUBC       EIC NAI FIC
	move.l	#%__0___0___0___0___0_000___1____0____0_00000___0___0___0_0000000000000,d0
	movec.l	d0,cacr			;データキャッシュOFF、ストアバッファOFF、分岐キャッシュON、命令キャッシュOFF
	cinva	bc			;データキャッシュクリア、命令キャッシュクリア
;		  F E   D   C    B    A  98  76   5  43  21 0
;		  E P NAD NAI FOTC FITC DCO DUO DWO DCI DUI
	move.l	#%0_0___0___0____0____0__10__00___0__10__00_0,d0
	movec.l	d0,tc			;アドレス変換OFF、データキャッシュOFFプリサイスモード、命令キャッシュOFFプリサイスモード
;		  FEDCBA98 76543210 F     ED CBA  9  8 7 65 43 2 10
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	move.l	#%00000000_00000000_0_____00_000__0__0_0_00_00_0_00,d0
	movec.l	d0,itt0			;透過変換OFF
	movec.l	d0,itt1
	movec.l	d0,dtt0
	movec.l	d0,dtt1
;		  FEDCBA9876543210 FEDCBA98      7 65432   1   0
;		  0000010000110000 REVISION EDEBUG       DFP ESS
	move.l	#%0000000000000000_00000000______0_00000___0___1,d0
	movec.l	d0,pcr			;FPU ON,スーパースカラON
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPUあり
@@:	bsr	mmuCheck46		;MMUチェック(MC68040/MC68060)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;不明
	moveq.l	#0,d7
;----------------------------------------------------------------
;終了
mpuCheckDone:

;例外ベクタテーブルを初期化する
;	$0010.wと$002C.wはここで上書きされる
	suba.l	a0,a0
	lea.l	$00FF0770.l,a1		;未定義例外処理
	moveq.l	#1,d1
	ror.l	#8,d1
	moveq.l	#256/2-1,d0
@@:
  .rept 2
	move.l	a1,(a0)+
	adda.l	d1,a1
  .endm
	dbra.w	d0,@b
	move.l	#unimplementedIntegerInstruction,$00F4.w	;[$00F4.w].l:例外ベクタ$3D 未実装整数命令
	jmp	$00FF007E

;----------------------------------------------------------------
;ダミーの例外処理
;	a1をspにコピーしてa0にジャンプする
dummyTrap:
	movea.l	a1,sp
	jmp	(a0)

;----------------------------------------------------------------
;MMUチェック(MC68030)
mmuCheck3:
	.cpu	68030
	lea.l	-128(sp),sp
	movea.l	sp,a0
;		L/U           LIMIT                DT
	move.l	#%0_111111111111111_00000000000000_10,(a0)+
	move.l	sp,d0
	lsr.l	#4,d0
	addq.l	#2,d0
	lsl.l	#4,d0
	move.l	d0,(a0)
;			     CI   M U WP DT
	lea.l	$00000000|%0__0_0_0_0__0_01.w,a0
	movea.l	d0,a1
	moveq.l	#8-1,d0
@@:	move.l	a0,(a1)+
	adda.l	#$00200000,a0
	dbra	d0,@b
;アドレス変換を有効にする
;			      CI   M U WP DT
	move.l	#$00F00000|%0__0_0_0_0__0_01,-4*8+4(a1)	;$00200000を$00F00000に変換する
	pflusha				;MMUがなくてもエラーにならない
	pmove.q	(sp),crp
;		  E       SRE FCL   PS   IS  TIA  TIB  TIC  TID
	move.l	#%1_00000___0___0_1101_1000_0011_0100_0100_0000,-(sp)
	pmove.l	(sp),tc
;8KB比較する
	lea.l	$00F00000,a1
	lea.l	$00200000,a0
	move.w	#2048-1,d0
@@:	cmpm.l	(a1)+,(a0)+
	dbne	d0,@b
	bne	@f
	bset.l	#31,d7			;MMUあり
@@:
;アドレス変換を解除する
	bclr.b	#7,(sp)			;E=0
	pmove.l	(sp),tc			;アドレス変換無効
	lea.l	4+128(sp),sp
	.cpu	68000
	rts

;----------------------------------------------------------------
;MMUチェック(MC68040/MC68060)
mmuCheck46:
	.cpu	68040
	lea.l	$2000.w,a1
;ルートテーブルを作る
;			   U W UDT
	lea.l	512|%00000_0_0__10(a1),a0
	moveq.l	#128-1,d0
@@:	move.l	a0,(a1)+
	dbra	d0,@b
;ポインタテーブルを作る
;			   U W UDT
	lea.l	512|%00000_0_0__10(a1),a0
	moveq.l	#128-1,d0
@@:	move.l	a0,(a1)+
	dbra	d0,@b
;ページテーブルを作る
	moveq.l	#32-1,d0
;			    UR G U1 U0 S CM M U W PDT
@@:	move.l	#$00FF0000|%00_1__0__0_0_10_0_0_0__01,(a1)+
	dbra	d0,@b
;$80000000～$FFFFFFFFを透過変換にする
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	movea.l	#%00000000_01111111_1_____10_000__0__0_0_10_00_0_00,a1
	movec.l	a1,itt0
	movec.l	a1,dtt0
	movec.l	a1,itt1
	movec.l	a1,dtt1
;アドレス変換を有効にする
	lea.l	$2000.w,a1
	movec.l	a1,srp			;MMUがなくてもエラーにならない(APPENDIX B-1)
	movec.l	a1,urp
;		  E P NAD NAI FOTC FITC DCO DUO DWO DCI DUI
	movea.l	#%1_1___0___0____0____0__00__00___0__00__00_0,a1
	movec.l	a1,tc
	pflusha
	cinva	bc
;8KB比較する
	lea.l	$80FF0000,a0
	lea.l	$80F00000,a1
	move.w	#2048-1,d0
@@:	cmpm.l	(a0)+,(a1)+
	dbne	d0,@b
	bne	@f
	bset.l	#31,d7			;MMUあり
@@:
;アドレス変換とトランスペアレント変換を解除する
;		  E P NAD NAI FOTC FITC DCO DUO DWO DCI DUI
	movea.l	#%0_0___0___0____0____0__10__00___0__00__00_0,a1
	movec.l	a1,tc
	suba.l	a1,a1
	pflusha
	movec.l	a1,itt0
	movec.l	a1,dtt0
	movec.l	a1,itt1
	movec.l	a1,dtt1
	.cpu	68000
	rts

;----------------------------------------------------------------
;未実装整数命令例外処理ルーチン
	.cpu	68060
unimplementedIntegerInstruction:
	pea.l	(8,sp)			;例外発生時のssp
	movem.l	d0-d7/a0-a6,-(sp)
	moveq.l	#5,d2			;スーパーバイザデータアクセスのファンクションコード
	btst.b	#5,(4*16,sp)		;例外発生時のsrのS
	bne	1f			;スーパーバイザモード
;ユーザモード
	moveq.l	#1,d2			;ユーザデータアクセスのファンクションコード
	move.l	usp,a0			;例外発生時のusp
	move.l	a0,(4*15,sp)		;例外発生時のsp
;命令コードを読み取る
1:	movea.l	(4*16+2,sp),a0		;a0=例外発生時のpc
	move.w	(a0)+,d1		;d1=命令コード,a1=pc+2
;MOVEPかどうか調べる
	move.w	d1,d0
;		  0000qqq1ws001rrr
	and.w	#%1111000100111000,d0
	cmp.w	#%0000000100001000,d0
	beq	2f			;MOVEP
;MOVEP以外
	movem.l	(sp),d0-d2		;d0/d1/d2を復元
	movea.l	(4*8,sp),a0		;a0を復元
	lea.l	(4*16,sp),sp		;破壊されていないレジスタの復元を省略する
	jmp	$00FF0770		;未定義例外処理

;MOVEP
;実効アドレスを求める
2:	moveq.l	#7,d0			;d0=0000000000000111
	and.w	d1,d0			;d0=0000000000000rrr
	movea.l	(4*8,sp,d0.w*4),a1	;a1=Ar
	adda.w	(a0)+,a1		;a0=pc+4,a1=d16+Ar=実効アドレス
;復帰アドレスを更新する
	move.l	a0,(4*16+2,sp)		;pc=pc+4
;Dqのアドレスを求める
	move.w	d1,d0			;d0=0000qqq1ws001rrr
	lsr.w	#8,d0			;d0=000000000000qqq1
	lea.l	(-2,sp,d0.w*2),a0	;d0*2=00000000000qqq10,a0=Dqのアドレス
;リード/ライト,ワード/ロングで分岐する
	add.b	d1,d1			;c=w,d1=s001rrr0
	bcs	5f			;ライト
;リード
	movec.l	sfc,d1			;ファンクションコードを保存
	movec.l	d2,sfc			;ファンクションコードを変更
	bmi	3f			;リードロング
;リードワード
;MOVEP.W (d16,Ar),Dq
	moves.b	(a1),d0			;メモリから上位バイトをリード
	lsl.w	#8,d0
	moves.b	(2,a1),d0		;メモリから下位バイトをリード
	move.w	d0,(2,a0)		;データレジスタの下位ワードへライト
	bra	4f

;リードロング
;MOVEP.L (d16,Ar),Dq
3:	moves.b	(a1),d0			;メモリから上位ワードの上位バイトをリード
	lsl.l	#8,d0
	moves.b	(2,a1),d0		;メモリから上位ワードの下位バイトをリード
	lsl.l	#8,d0
	moves.b	(4,a1),d0		;メモリから下位ワードの上位バイトをリード
	lsl.l	#8,d0
	moves.b	(6,a1),d0		;メモリから下位ワードの下位バイトをリード
	move.l	d0,(a0)			;データレジスタへライト
4:	movec.l	d1,sfc			;ファンクションコードを復元
	movem.l	(sp),d0-d7		;データレジスタのどれか1個が更新されている
	bra	8f

;ライト
5:	movec.l	dfc,d1			;ファンクションコードを保存
	movec.l	d2,dfc			;ファンクションコードを変更
	bmi	6f			;ライトロング
;ライドワード
;MOVEP.W Dq,(d16,Ar)
	move.w	(2,a0),d0		;データレジスタの下位ワードからリード
	rol.w	#8,d0
	moves.b	d0,(a1)			;メモリへ上位バイトをライト
	rol.w	#8,d0
	moves.b	d0,(2,a1)		;メモリへ下位バイトをライト
	bra	7f

;ライトロング
;MOVEP.L Dq,(d16,Ar)
6:	move.l	(a0),d0			;データレジスタからリード
	rol.l	#8,d0
	moves.b	d0,(a1)			;メモリへ上位ワードの上位バイトをライト
	rol.l	#8,d0
	moves.b	d0,(2,a1)		;メモリへ上位ワードの下位バイトをライト
	rol.l	#8,d0
	moves.b	d0,(4,a1)		;メモリへ下位ワードの上位バイトをライト
	rol.l	#8,d0
	moves.b	d0,(6,a1)		;メモリへ下位ワードの下位バイトをライト
7:	movec.l	d1,dfc			;ファンクションコードを復元
	movem.l	(sp),d0-d2		;d0/d1/d2を復元
8:	movem.l	(4*8,sp),a0-a1		;a0/a1を復元
	lea.l	(4*16,sp),sp		;破壊されていないレジスタの復元を省略する
	tst.b	(sp)			;例外発生時のsrのT
	bpl	9f			;トレースなし
;トレース
	ori.w	#$8000,sr		;RTEの前にsrのTをセットしてMOVEPをトレースしたように振る舞う
9:	rte
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	クロック計測
;
;----------------------------------------------------------------

	PATCH_DATA	clockCheck,$00FF013C

	jmp	clockCheck

	PATCH_EXTRA

;----------------------------------------------------------------
;クロック計測
clockCheck:

;システムクロックの確認
	moveq.l	#0,d0
	move.b	$00E8E00B,d0		;[$00E8E00B].b:SYS 機種判別($DC=X68030,$FE=XVIで16MHz,$FF=XVI以前で10MHz)
					;$00FF=10MHz,$00FE=16MHz,$00DC=25MHz
	not.b	d0			;$0000=10MHz,$0001=16MHz,$0023=25MHz
	lsl.w	#4,d0			;$0000=10MHz,$0010=16MHz,$0230=25MHz
	lsr.b	#4,d0			;$0000=10MHz,$0001=16MHz,$0203=25MHz
	move.w	d0,$0CB6.w		;[$0CB6.w].w:[11,12,13]システムクロック($0000=10MHz,$0001=16MHz,$0203=25MHz)

;クロック計測のためのキャッシュON
;	MC68030のときは命令キャッシュとデータキャッシュをONにする
;	MC68040/MC68060のデータキャッシュとMC68060のストアバッファはMMUを有効にするまでONにできないので命令キャッシュだけONにする
;	SRAMのキャッシュの設定はこれより後で反映される
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	3f			;MC68000/MC68010はキャッシュなし
	move.l	#$00002101,d0		;MC68020は命令キャッシュON(bit0)、MC68030はデータキャッシュON(bit8),命令キャッシュON(bit0)
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	2f			;MC68020/MC68030
	beq	1f			;MC68040
	.cpu	68060
	movec.l	pcr,d0
	bset.l	#0,d0			;MC68060はスーパースカラON(bit0)
	movec.l	d0,pcr
	.cpu	68000
	move.l	#$00800000,d0		;MC68060はストアバッファOFF(bit29),分岐キャッシュON(bit23)
1:	move.w	#$8000,d0		;MC68040/MC68060はデータキャッシュOFF(bit31),命令キャッシュON(bit15)
2:
	.cpu	68030
	movec.l	d0,cacr
	.cpu	68000
	clr.b	$00E8E009		;[$00E8E009].b:[X68030]アクセスウェイト制御
3:

;クロック計測
;ROM計測
	lea.l	clockLoop(pc),a0	;計測ループ
	bsr	clockSub		;計測サブ
	move.w	d0,$0CB8.w		;[$0CB8.w].w:[11,12,13]ROM計測クロック($0342≒10*250/3,$056E≒16.67*250/3,$104D≒25*500/3)
;RAM計測
	lea.l	-30(sp),sp
	move.l	sp,d0
	add.w	#14,d0
	and.w	#-16,d0			;16バイト境界から始まる16バイトのワークエリア
	addq.w	#4,d0
	movea.l	d0,a0			;計測ループ
	move.l	clockLoop-4(pc),-4(a0)	;計測ループをワークエリアにコピーする
	move.l	clockLoop(pc),(a0)
	move.l	clockLoop+4(pc),4(a0)
	move.l	clockLoop+8(pc),8(a0)	;スタックエリアが命令キャッシュに乗っていることはないのでキャッシュフラッシュは省略する
	bsr	clockSub		;計測サブ
	lea.l	30(sp),sp
	move.w	d0,$0CBA.w		;[$0CBA.w].w:[11,12,13]RAM計測クロック($03D3≒10*250/3*1.2/(1+0.22/10),$066D≒16.67*250/3*1.2/(1+0.22/16.67),$104D≒25*500/3)
	jmp	$00FF019C

IERB	equ	$00E88009
IMRB	equ	$00E88015
TCDCR	equ	$00E8801D
TCDR	equ	$00E88023
TDDR	equ	$00E88025

;計測サブ
;>d0.w:計測値。0=失敗
;?d1-d2/a1
clockSub:
;回数初期化
	moveq.l	#22-8,d2
clockRetry:
;割り込み禁止
	move.w	sr,-(sp)
	ori.w	#$0700,sr
;タイマ保存
	lea.l	TCDCR,a1
	move.b	IERB-TCDCR(a1),-(sp)
	move.b	IMRB-TCDCR(a1),-(sp)
	move.b	TCDCR-TCDCR(a1),-(sp)
;タイマ設定
	andi.b	#%11001111,IERB-TCDCR(a1)	;Timer-C/D割り込み停止
	andi.b	#%11001111,IMRB-TCDCR(a1)	;Timer-C/D割り込み禁止
	sf.b	TCDCR-TCDCR(a1)		;Timer-C/Dカウント停止
@@:	tst.b	TCDCR-TCDCR(a1)		;完全に停止するまで待つ
	bne	@b
	sf.b	TCDR-TCDCR(a1)		;Timer-Cカウンタクリア
	sf.b	TDDR-TCDCR(a1)		;Timer-Dカウンタクリア
;準備
	move.l	#1<<22,d0
	lsr.l	d2,d0
	subq.l	#1,d0
;カウント開始
;		  TCCR TDCR
	move.b	#%0111_0001,TCDCR-TCDCR(a1)	;Timer-C/Dカウント開始
					;	Timer-Cは1/200プリスケール(50μs)
					;	Timer-Dは1/4プリスケール(1μs)
;計測
	jsr	(a0)
;カウント停止
	sf.b	TCDCR-TCDCR(a1)		;Timer-C/Dカウント停止
@@:	tst.b	TCDCR-TCDCR(a1)		;完全に停止するまで待つ
	bne	@b
;タイマ取得
	moveq.l	#0,d0
	sub.b	TCDR-TCDCR(a1),d0	;Timer-Cカウント数
	moveq.l	#0,d1
	sub.b	TDDR-TCDCR(a1),d1	;Timer-Dカウント数(オーバーフローあり)
;タイマ復元
	move.b	#200,TCDR-TCDCR(a1)	;Timer-Cカウンタ復元
	sf.b	TDDR-TCDCR(a1)		;Timer-Dカウンタクリア
	move.b	(sp)+,TCDCR-TCDCR(a1)
	move.b	(sp)+,IMRB-TCDCR(a1)
	move.b	(sp)+,IERB-TCDCR(a1)
;割り込み許可
	move.w	(sp)+,sr
;カウンタ合成
	mulu.w	#50,d0
	cmp.b	d1,d0
	bls	@f
	add.w	#256,d0
@@:	move.b	d1,d0
	subq.w	#1,d0
;回数更新
	tst.w	d2
	beq	@f
	cmp.w	#5000,d0
	bcc	@f
	subq.w	#1,d2			;5000μsに満たなければ回数を2倍にする
	bra	clockRetry
@@:
;補正
;	000/010のときMHz値*250/3をワードで保存するので786.42MHzが上限
;	020/030/040/060のときMHz値*500/3をワードで保存するので393.21MHzが上限
;	000/010	12clk	1MHz	12μs	2^9回	6144μs	10*250/3*1.2/(6144/2^9)=83.3333		83*3/250=0.996
;							(4194304000>>(22-9))/6144=83.3333
;			10MHz	1.2μs	2^13回	9830μs	10*250/3*1.2/(9830/2^13)=833.367	833*3/250=9.996
;							(4194304000>>(22-13))/9830=833.367
;			100MHz	0.12μs	2^16回	7864μs	10*250/3*1.2/(7864/2^16)=8333.67	8334*3/250=100.008
;							(4194304000>>(22-16))/7864=8333.67
;			600MHz	0.02μs	2^18回	5243μs	10*250/3*1.2/(5243/2^18)=49998.9	49999*3/250=599.988
;							(4194304000>>(22-18))/5243=49998.9
;			750MHz	0.016μs	2^19回	8389μs	10*250/3*1.2/(8389/2^19)=62497.1	62497*3/250=749.964
;							(4194304000>>(22-19))/8389=62497.1
;	020/030	6clk	25MHz	0.24μs	2^15回	7864μs	25*500/3*0.24/(7864/2^15)=4166.84	4167*3/500=25.002
;							(4194304000>>(22-15))/7864=4166.84
;	040	3clk	25MHz	0.12μs	2^16回	7864μs	25*500/3*0.12/(7864/2^16)=4166.84	4167*3/500=25.002
;							(2097152000>>(22-16))/7864=4166.84
;	060	1clk	50MHz	0.02μs	2^18回	5243μs	50*500/3*0.02/(5243/2^18)=8333.14	8333*3/500=49.998
;							(699050667>>(22-18))/5243=8333.14
	lea.l	clockScale(pc),a1
	moveq.l	#0,d1
	move.b	$0CBC.w,d1		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	cmp.w	#6,d1
	bhi	@f			;念のため
	lsl.w	#2,d1
	adda.w	d1,a1
@@:	move.l	(a1),d1
	lsr.l	d2,d1			;699050667>>(22-18)
	tst.l	d0
	beq	9f
	cmp.l	#$FFFF,d0
	bhi	9f
	divu.w	d0,d1			;(699050667>>(22-18))/5243
	bvc	@f
	moveq.l	#-1,d1			;オーバーフロー
@@:	moveq.l	#0,d0
	move.w	d1,d0
	rts
9:	moveq.l	#0,d0			;失敗
	rts

clockScale:
	.dc.l	4194304000	;000
	.dc.l	4194304000	;010
	.dc.l	4194304000	;020
	.dc.l	4194304000	;030
	.dc.l	2097152000	;040
	.dc.l	0
	.dc.l	699050667	;060

;計測ループ
;	000/010のとき
;		dbra(分岐あり)は10clk。ROMのときはウェイトがかかるので12clk。10MHzで1.2μs
;		RAM計測のときは基本は10clkだがDRAMのリフレッシュの影響で(1+0.22/MHz値)倍くらいになる
;	020/030のとき
;		dbra(命令キャッシュON,分岐あり)は6clk。25MHzで0.24μs
;	040のとき
;		dbra(命令キャッシュON,分岐あり)は3clk。25MHzで0.12μs
;	060のとき
;		dbra(命令キャッシュON,分岐キャッシュON,分岐あり)は1clk。50MHzで0.02μs
	.align	4		;dbraを4バイト境界に合わせる
	nop
@@:	swap.w	d0
clockLoop:
	dbra	d0,clockLoop
	swap.w	d0
	dbra	d0,@b
	rts			;上位ワードと下位ワードが入れ替わったままだが両方-1なので問題ない

	PATCH_END



;----------------------------------------------------------------
;
;	XF3,XF4,XF5を押しながら起動したときのキャッシュOFF
;
;----------------------------------------------------------------

	PATCH_DATA	xf345CacheOff,$00FF0336

	moveq.l	#0,d0
	.cpu	68030
	movec.l	d0,cacr			;キャッシュOFF
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	XF1,XF2を押しながら起動したときのキャッシュOFF
;
;----------------------------------------------------------------

	PATCH_DATA	xf12CacheOff,$00FF0380

	moveq.l	#0,d0
	.cpu	68030
	movec.l	d0,cacr			;キャッシュOFF
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	起動メッセージ
;
;----------------------------------------------------------------

IPL_MESSAGE_PROPORTIONAL	equ	1

	PATCH_DATA	iplMessage,$00FF0E88

	jsr	iplMessage
	bra	($00FF0E9A)PatchZL

	PATCH_EXTRA

;----------------------------------------------------------------
;起動メッセージを表示する
iplMessage:
	movem.l	d0-d7/a0-a6,-(sp)
	lea.l	-64(sp),sp
	movea.l	sp,a6			;文字列バッファ
;<a6.l:文字列バッファ
;メッセージ
	bsr	iplMessageRomver	;ROMのバージョンを表示する
	bsr	iplMessageMpu		;MPUの種類と動作周波数を表示する
	bsr	iplMessageFpu		;FPU/FPCPの有無と種類を表示する
	bsr	iplMessageMmu		;MMUの有無を表示する
	bsr	iplMessageMemory	;メインメモリの範囲と容量を表示する
	bsr	iplMessageExmemory	;拡張メモリの範囲と容量を表示する
	bsr	iplMessageCoprocessor	;コプロセッサの有無と種類を表示する
	lea.l	64(sp),sp
	movem.l	(sp)+,d0-d7/a0-a6
	rts

;----------------------------------------------------------------
;ROMのバージョンを表示する
;<a6.l:文字列バッファ
;?d0-d1/a0-a1
iplMessageRomver:
	movea.l	a6,a0			;文字列バッファ
	lea.l	100f(pc),a1		;$1B,'[1mBIOS ROM version: '
	bsr	strCopy
	IOCS	_ROMVER
;BCD変換
	move.l	d0,d1			;abcdefgh
	clr.w	d0			;abcd0000
	sub.l	d0,d1			;0000efgh
	swap.w	d0			;0000abcd
	lsl.l	#4,d0			;000abcd0
	lsl.l	#4,d1			;000efgh0
	lsr.w	#4,d0			;000a0bcd
	lsr.w	#4,d1			;000e0fgh
	lsl.l	#8,d0			;0a0bcd00
	lsl.l	#8,d1			;0e0fgh00
	lsr.w	#4,d0			;0a0b0cd0
	lsr.w	#4,d1			;0e0f0gh0
	lsr.b	#4,d0			;0a0b0c0d
	lsr.b	#4,d1			;0e0f0g0h
	or.l	#$30303030,d0		;3a3b3c3d
	or.l	#$30303030,d1		;3e3f3g3h
;バージョン
	rol.l	#8,d0
	move.b	d0,(a0)+
	move.b	#'.',(a0)+
	rol.l	#8,d0
	move.b	d0,(a0)+
;日付
	lea.l	101f(pc),a1		;' (20'
	bsr	strCopy
	rol.l	#8,d0
	move.b	d0,(a0)+
	rol.l	#8,d0
	move.b	d0,(a0)+
	move.b	#'-',(a0)+
	rol.l	#8,d1
	move.b	d1,(a0)+
	rol.l	#8,d1
	move.b	d1,(a0)+
	move.b	#'-',(a0)+
	rol.l	#8,d1
	move.b	d1,(a0)+
	rol.l	#8,d1
	move.b	d1,(a0)+
	lea.l	102f(pc),a1		;')',$1B,'[1m',13,10
	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
	rts

100:	.dc.b	$1B,'[1mBIOS ROM version: ',0
101:	.dc.b	' (20',0
102:	.dc.b	')',$1B,'[1m',13,10,0
	.even

;----------------------------------------------------------------
;MPUの種類と動作周波数を表示する
;<a6.l:文字列バッファ
;>d5.l:_SYS_STAT(0)の結果
;	bit0～7		MPUの種類(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	動作周波数(MHz)*10
;?d0-d1/a0-a1
iplMessageMpu:
	moveq.l	#0,d1
	IOCS	_SYS_STAT
	move.l	d0,d5
;<d5.l:_SYS_STAT(0)の結果
;	bit0～7		MPUの種類(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	動作周波数(MHz)*10
	movea.l	a6,a0			;文字列バッファ
	lea.l	100f(pc),a1		;'Micro-Processing Unit (MPU): '
	bsr	strCopy
;MPUの種類
;	MCとXC
;		MC68000/MC68010/MC68020/MC68030/MC68040はMC
;		MC68060はリビジョン6からMC、リビジョン5までXC
;			http://www.ppa.pl/forum/amiga/29981/68060-pcr
;			?	リビジョン0
;			F43G	リビジョン1
;			G65V	リビジョン5
;			G59Y	?
;			E41J	リビジョン6
;	無印とLCとEC
;		MC68000/MC68010/MC68020は無印
;		MC68030はMMUがあるとき無印、MMUがないときEC
;		MC68040/MC68060はFPUがあるとき無印、FPUがなくてMMUがあるときLC、MMUがないときEC
;	リビジョンナンバー
;		MC68060のとき末尾に-0XXでリビジョンナンバーを表示
;	メモ
;		MC68EC020はMC68020のアドレスバスを24bitにしてECS,OCS,DBEN,IPEND,BGACKを省略した組み込み用。コプロセッサインタフェイスは共通
	moveq.l	#-1,d2			;リビジョンナンバー。-1=リビジョンナンバーを表示しない
	moveq.l	#'M',d1
	cmp.b	#6,d5
	bne	1f			;MC68000/MC68030/MC68040はMC
	.cpu	68060
	movec.l	pcr,d0
	.cpu	68000
	lsr.w	#8,d0
	moveq.l	#0,d2
	move.b	d0,d2			;リビジョンナンバー
	cmp.b	#6,d2
	bcc	1f			;MC68060のリビジョン6以上はMC
	moveq.l	#'X',d1			;MC68060のリビジョン5以下はXC
1:	move.b	d1,(a0)+
	lea.l	101f(pc),a1		;'C68',0
	bsr	strCopy
	cmp.b	#3,d5
	blo	5f			;MC68000/MC68010/MC68020は無印
	bhi	2f
	btst.l	#14,d5
	bne	5f			;MC68030でMMUがあるとき無印
	bra	3f			;MC68030でMMUがないときEC
2:	tst.w	d5
	bmi	5f			;MC68040/MC68060でFPUがあるとき無印
	moveq.l	#'L',d1
	btst.l	#14,d5
	bne	4f			;MC68040/MC68060でFPUがなくてMMUがあるときLC
3:	move.l	#'E',d1			;MC68040/MC68060でMMUがないときEC
4:	move.b	d1,(a0)+
	move.b	#'C',(a0)+
5:	move.b	#'0',(a0)+
	moveq.l	#'0',d1
	add.b	d5,d1
	move.b	d1,(a0)+
	move.b	#'0',(a0)+
	move.l	d2,d0			;リビジョンナンバー
	bmi	6f
	move.b	#'-',(a0)+
	moveq.l	#3,d1
	bsr	strDecN
6:
;動作周波数
	move.b	#' ',(a0)+
	move.b	#'(',(a0)+
	move.l	d5,d0
	clr.w	d0
	swap.w	d0			;動作周波数(MHz)*10
	bsr	strDec
	move.b	-1(a0),(a0)+		;小数点以下1桁目を後ろにずらす
	move.b	#'.',-2(a0)		;小数点を押し込む
	lea.l	102f(pc),a1		;'MHz)',13,10
	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
	rts

100:	.dc.b	'Micro-Processing Unit (MPU): ',0
101:	.dc.b	'C68',0
102:	.dc.b	'MHz)',13,10,0
	.even

;----------------------------------------------------------------
;FPU/FPCPの有無と種類を表示する
;	MC68030
;	  fnopがある
;	    fmovecr.x #1,fp0は0		Floating-Point Coprocessor (FPCP): MC68881
;	    fmovecr.x #1,fp0は0以外	Floating-Point Coprocessor (FPCP): MC68882
;	MC68040,MC68060
;	  fnopがある			Floating-Point Unit (FPU): on MPU
;<d5.l:_SYS_STAT(0)の結果
;	bit0～7		MPUの種類(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	動作周波数(MHz)*10
;<a6.l:文字列バッファ
;?d0/a0-a1
iplMessageFpu:
;FPU/FPCP
	tst.w	d5
	bpl	3f			;FPU/FPCPなし
	movea.l	a6,a0			;文字列バッファ
	lea.l	100f(pc),a1		;'Floating-Point Unit (FPU): '
	cmp.b	#4,d5
	bhs	1f
	lea.l	101f(pc),a1		;'Floating-Point Coprocessor (FPCP): '
1:	bsr	strCopy
	lea.l	102f(pc),a1		;'on MPU',13,10
	cmp.b	#4,d5
	bhs	2f			;MC68040/MC68060
	.cpu	68030
	fmovecr.x	#1,fp0		;0=MC68881,0以外=MC68882
	fmove.x	fp0,-(sp)
	.cpu	68000
	move.l	(sp)+,d0
	or.l	(sp)+,d0
	or.l	(sp)+,d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	2f
	lea.l	104f(pc),a1		;'MC68882',13,10
2:	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
3:
	rts

100:	.dc.b	'Floating-Point Unit (FPU): ',0
101:	.dc.b	'Floating-Point Coprocessor (FPCP): ',0
102:	.dc.b	'on MPU',13,10,0
103:	.dc.b	'MC68881',13,10,0
104:	.dc.b	'MC68882',13,10,0
	.even

;----------------------------------------------------------------
;コプロセッサの有無と種類を表示する
;	MC68040,MC68060
;	  FC=7の$00022000にCIRがある
;	    fmovecr.x #1,fp0は0		Motherboard Coprocessor: MC68881
;	    fmovecr.x #1,fp0は0以外	Motherboard Coprocessor: MC68882
;	MC68000,MC68030,MC68040,MC68060
;	  $00E9E000にCIRがある
;	    fmovecr.x #1,fp0は0		Extension Board Coprocessor 1: MC68881
;	    fmovecr.x #1,fp0は0以外	Extension Board Coprocessor 1: MC68882
;	  $00E9E080にCIRがある
;	    fmovecr.x #1,fp0は0		Extension Board Coprocessor 2: MC68881
;	    fmovecr.x #1,fp0は0以外	Extension Board Coprocessor 2: MC68882
iplMessageCoprocessor:
;マザーボードコプロセッサ
	cmp.b	#4,d5
	blo	2f
	bsr	coproCheck1		;マザーボードコプロセッサの有無と種類を調べる
	bmi	2f
	movea.l	a6,a0			;文字列バッファ
	lea.l	105f(pc),a1		;'Motherboard Coprocessor: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
2:
;数値演算プロセッサボード1
	moveq.l	#0,d0
	bsr	coproCheck2
	bmi	2f
	movea.l	a6,a0			;文字列バッファ
	lea.l	106f(pc),a1		;'Extension Board Coprocessor 1: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
2:
;数値演算プロセッサボード2
	moveq.l	#1,d0
	bsr	coproCheck2
	bmi	2f
	movea.l	a6,a0			;文字列バッファ
	lea.l	107f(pc),a1		;'Extension Board Coprocessor 2: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
2:
	rts

103:	.dc.b	'MC68881',13,10,0
104:	.dc.b	'MC68882',13,10,0
105:	.dc.b	'Motherboard Coprocessor: ',0
106:	.dc.b	'Extension Board Coprocessor 1: ',0
107:	.dc.b	'Extension Board Coprocessor 2: ',0
	.even

;マザーボードコプロセッサの有無と種類を調べる
;>d0.l:-1=なし,0=MC68881,1=MC68882
	.cpu	68060
coproCheck1:
	movem.l	d1-d4/a0-a2,-(sp)
@@:	moveq.l	#1,d0
	and.b	@b-1(pc,d0.w*2),d0	;0=000,1=020以上
	subq.l	#1,d0			;-1=000,0=020以上
	bmi	9f			;000
	lea.l	$00022000,a0
	moveq.l	#-1,d0
	move.w	sr,d2
	ori.w	#$0700,sr
	movec.l	dfc,d4
	movec.l	sfc,d3
	moveq.l	#7,d1			;CPU空間
	movec.l	d1,dfc
	movec.l	d1,sfc
	lea.l	5f(pc),a1
	movea.l	$0008.w,a2
	move.l	a1,$0008.w
	movea.l	sp,a1
	clr.w	d1			;null
	moves.w	d1,$06(a0)		;restore
	moves.w	$06(a0),d1		;restore
@@:	moves.w	(a0),d1			;response
	cmp.w	#$0802,d1		;idle
	bne	@b
	move.w	#$5C01,d1		;fmovecr.x #$01,fp0
	moves.w	d1,$0A(a0)		;command
@@:	moves.w	(a0),d1			;response
	cmp.w	#$0802,d1		;idle
	bne	@b
	move.w	#$6800,d1		;fmove.x fp0,<mem>
	moves.w	d1,$0A(a0)		;command
@@:	moves.w	(a0),d1			;response
	cmp.w	#$320C,d1		;extended to mem
	bne	@b
	moves.l	$10(a0),d0		;operand
	moves.l	$10(a0),d1		;operand
	or.l	d1,d0
	moves.l	$10(a0),d1		;operand
	or.l	d1,d0
	beq	@f			;ROMの$01が0なのでMC68881
	moveq.l	#1,d0			;ROMの$01が0でないのでMC68882
@@:
5:	movea.l	a1,sp
	move.l	a2,$0008.w
	movec.l	d3,sfc
	movec.l	d4,dfc
	move.w	d2,sr
9:	movem.l	(sp)+,d1-d4/a0-a2
	tst.l	d0
	rts
	.cpu	68000

;数値演算プロセッサボードの有無と種類を調べる
;<d0.l:0=数値演算プロセッサボード1,1=数値演算プロセッサボード2
;>d0.l:-1=なし,0=MC68881,1=MC68882
coproCheck2:
	movem.l	d1-d2/a0-a2,-(sp)
	lea.l	$00E9E000,a0
	tst.l	d0
	beq	@f
	lea.l	$80(a0),a0
@@:	moveq.l	#-1,d0
	move.w	sr,d2
	ori.w	#$0700,sr
	lea.l	5f(pc),a1
	movea.l	$0008.w,a2
	move.l	a1,$0008.w
	movea.l	sp,a1
	clr.w	$06(a0)			;restore,null
	tst.w	$06(a0)			;restore
@@:	cmpi.w	#$0802,(a0)		;response,idle
	bne	@b
	move.w	#$5C01,$0A(a0)		;command,fmovecr.x #$01,fp0
@@:	cmpi.w	#$0802,(a0)		;response,idle
	bne	@b
	move.w	#$6800,$0A(a0)		;command,fmove.x fp0,<mem>
@@:	cmpi.w	#$320C,(a0)		;response,extended to mem
	bne	@b
	move.l	$10(a0),d0		;operand
	move.l	$10(a0),d1		;operand
	or.l	d1,d0
	move.l	$10(a0),d1		;operand
	or.l	d1,d0
	beq	@f			;ROMの$01が0なのでMC68881
	moveq.l	#1,d0			;ROMの$01が0でないのでMC68882
@@:
5:	movea.l	a1,sp
	move.l	a2,$0008.w
	move.w	d2,sr
	movem.l	(sp)+,d1-d2/a0-a2
	tst.l	d0
	rts

;----------------------------------------------------------------
;MMUの有無を表示する
;<d5.l:_SYS_STAT(0)の結果
;	bit0～7		MPUの種類(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	動作周波数(MHz)*10
;<a6.l:文字列バッファ
;?d0/a0-a1
iplMessageMmu:
	btst.l	#14,d5
	beq	9f			;MMUなし
	movea.l	a6,a0			;文字列バッファ
	lea.l	100f(pc),a1		;'Memory Management Unit (MMU): '
	bsr	strCopy
	cmp.b	#3,d5
	bhs	1f
	lea.l	101f(pc),a1		;'MC68851',13,10
	bsr	strCopy
	bra	2f

1:	lea.l	102f(pc),a1		;'on MPU',13,10
	bsr	strCopy
2:	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
9:	rts

100:	.dc.b	'Memory Management Unit (MMU): ',0
101:	.dc.b	'MC68851',13,10,0
102:	.dc.b	'on MPU',13,10,0
	.even

;----------------------------------------------------------------
;メインメモリの範囲と容量を表示する
;	$00100000-$00BFFFFFについて1MB単位でメインメモリの有無を確認し、メインメモリが存在する範囲を表示する
;	手順
;		下位のページから順にリードしてみてバスエラーが出ないことを確認する
;----------------------------------------------------------------
;<d5.l:_SYS_STAT(0)の結果
;	bit0～7		MPUの種類(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	動作周波数(MHz)*10
;<a6.l:文字列バッファ
;?d0-d1/a0-a3/a5
iplMessageMemory:
	move.w	sr,-(sp)		;sr保存
	ori.w	#$0700,sr		;割り込み禁止
	move.l	$0008.w,-(sp)		;バスエラーベクタ保存
	movea.l	sp,a5			;sp保存
	move.l	#2f,$0008.w		;バスエラーでループを終了する
	suba.l	a2,a2			;a2=$00000000
	move.l	a2,d0			;開始アドレス
1:	tst.b	(a2)			;テスト
	adda.l	#$00100000,a2		;次のページ
	cmpa.l	#$00C00000,a2		;メインメモリの末尾
	blo	1b
2:	movea.l	a5,sp			;sp復元
	move.l	(sp)+,$0008.w		;バスエラーベクタ復元
	move.w	(sp)+,sr		;sr復元(割り込み許可レベル復元)
	move.l	a2,d1			;終了アドレス(これを含まない)
	movea.l	a6,a0			;文字列バッファ
	lea.l	100f(pc),a1		;'Motherboard Memory: $'
	bsr	strCopy
	bsr	strMemory		;メモリの範囲と容量の文字列を作る
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
	rts

100:	.dc.b	'Motherboard Memory: $',0
	.even

;----------------------------------------------------------------
;拡張メモリの範囲と容量を表示する
;	$01000000-$FFFFFFFFについて16MB単位で独立した拡張メモリの有無を確認し、拡張メモリが存在する範囲を表示する
;	手順
;		各ページの同じオフセットに上位のページから順に異なるデータを書き込む
;		下位のページに書き込んだことで上位のページのデータが変化した場合は上位のページは存在しないと判断する
;		$2000-$21FFをワークとして使用する
;	メモ
;		MC68000/MC68010/MC68EC020はアドレスバスが24bitなのでここで表示される拡張メモリは存在しない
;<a6.l:文字列バッファ
;?d0-d1/a0-a3/a5
iplMessageExmemory:
	move.w	sr,-(sp)		;sr保存
	ori.w	#$0700,sr		;割り込み禁止
	move.l	$0008.w,-(sp)		;バスエラーベクタ保存
	movea.l	sp,a5			;sp保存
;----------------------------------------------------------------
;	page=$FF..$00について
;		[$2100+page].b=[page<<24].b	保存
;		バスエラーでなければ
;			[page<<24].b=page	テストデータ書き込み
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;バスエラーでスキップ
	suba.l	a2,a2			;a2=$FF000000,$FE000000,…,$00000000
	lea.l	$2200.w,a1		;a1=$000021FF,$000021FE,…,$00002100
	move.w	#$00FF,d1		;d1=$00FF,$00FE,…,$0000=page
1:	suba.l	#$01000000,a2
	subq.l	#1,a1			;バスエラーでずれると困るので-(a1)は使わない
	move.b	(a2),(a1)		;保存
	move.b	d1,(a2)			;テストデータ書き込み
2:	movea.l	a5,sp			;sp復元
	dbra	d1,1b
;----------------------------------------------------------------
;	page=$FF..$00について
;		[$2000+page].b=~page	;テストデータと異なるダミーデータ。バスエラーで書き込めなかったときに参照される
;		[$2000+page].b=[page<<24].b	テストデータ読み出し
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;バスエラーでスキップ
	suba.l	a2,a2			;a2=$FF000000,$FE000000,…,$00000000
	lea.l	$2100.w,a1		;a1=$000020FF,$000020FE,…,$00002000
	move.w	#$00FF,d1		;d1=$00FF,$00FE,…,$0000=page
	move.b	d1,d0			;d0=$00,$01,…,$FF=~page
1:	addq.b	#1,d0
	suba.l	#$01000000,a2
	move.b	d0,-(a1)		;テストデータと異なるダミーデータ。バスエラーで書き込めなかったときに参照される
	move.b	(a2),(a1)		;テストデータ読み出し
2:	movea.l	a5,sp			;sp復元
	dbra	d1,1b
;----------------------------------------------------------------
;	page=$FF..$00について
;		([$2000+page].b-=page)==0ならば
;			[page<<24].b=[$2100+].b	復元
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;バスエラーでスキップ
	suba.l	a2,a2			;a2=$FF000000,$FE000000,…,$00000000
	lea.l	$2100.w,a1		;a1=$000020FF,$000020FE,…,$00002000
	move.w	#$00FF,d1		;d1=$00FF,$00FE,…,$0000=page
1:	suba.l	#$01000000,a2
	sub.b	d1,-(a1)		;比較。一致したとき0
	bne	2f
	move.b	$2200-$2100(a1),(a2)	;復元
2:	movea.l	a5,sp			;sp復元
	dbra	d1,1b
;----------------------------------------------------------------
	move.l	(sp)+,$0008.w		;バスエラーベクタ復元
	move.w	(sp)+,sr		;sr復元(割り込み許可レベル復元)
;----------------------------------------------------------------
;	page=$01..$FFについて
;		[$2000+page].b==0である範囲を表示
;----------------------------------------------------------------
	suba.l	a2,a2			;拡張メモリの開始位置。0=page-1に拡張メモリはない
	lea.l	$2001.w,a3		;a3=$00002001,$00002002,…,$000020FF=$2000+page
1:	tst.b	(a3)
	bne	2f			;pageに拡張メモリはない
;pageに拡張メモリがある
	move.l	a2,d0			;拡張メモリの開始位置
	bne	3f			;page-1にも拡張メモリがあった
;page-1には拡張メモリはなかった
;pageから拡張メモリが始まった
	movea.l	a3,a2			;拡張メモリの開始位置
	bra	3f
;pageに拡張メモリはない
2:	move.l	a2,d0			;拡張メモリの開始位置
	beq	3f			;page-1にも拡張メモリはなかった
;page-1には拡張メモリがあった
;pageで拡張メモリが終わった
	movea.l	a6,a0			;文字列バッファ
	bsr	strExmemory		;拡張メモリの範囲を表示する
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
	suba.l	a2,a2			;拡張メモリの開始位置
3:	addq.l	#1,a3
	cmpa.w	#$20FF,a3
	bls	1b
	move.l	a2,d0			;拡張メモリの開始位置
	beq	4f			;page-1には拡張メモリはなかった
;page-1には拡張メモリがあった
	movea.l	a6,a0			;文字列バッファ
	bsr	strExmemory		;メモリの範囲と容量の文字列を作る
	movea.l	a6,a1			;文字列バッファ
	IOCS	_B_PRINT
4:	rts

;----------------------------------------------------------------
;拡張メモリの範囲と容量の文字列を作る
;<a0.l:文字列バッファ
;<a2.l:$2000+page。拡張メモリの開始位置
;<a3.l:$2000+page。拡張メモリの終了位置(これを含まない)
;?d0-d1/a0-a1
strExmemory:
	move.l	a2,d0
	swap.w	d0
	lsl.l	#8,d0			;開始アドレス
	move.l	a3,d1
	swap.w	d1
	lsl.l	#8,d1			;終了アドレス(これを含まない)
	lea.l	100f(pc),a1		;'Daughterboard Memory: $'
	bsr	strCopy
;メモリの範囲と容量の文字列を作る
;<d0.l:開始アドレス
;<d1.l:終了アドレス(これを含まない)
;<a0.l:文字列バッファ
;?d0/a0-a1
strMemory:
	bsr	strHex8			;開始アドレス
	move.b	#'-',(a0)+
	move.b	#'$',(a0)+
	move.l	d0,-(sp)		;開始アドレス
	move.l	d1,d0			;終了アドレス(これを含まない)
	subq.l	#1,d0			;終了アドレス(これを含む)
	bsr	strHex8
	move.b	#' ',(a0)+
	move.b	#'(',(a0)+
	move.l	d1,d0			;終了アドレス(これを含まない)
	sub.l	(sp)+,d0		;終了アドレス(これを含まない)-開始アドレス=容量
	swap.w	d0
	lsr.w	#4,d0			;容量(16MB単位)
	bsr	strDec
	lea.l	101f(pc),a1		;'MB)',13,10
	bsr	strCopy
	rts

100:	.dc.b	'Daughterboard Memory: $',0
101:	.dc.b	'MB)',13,10,0
	.even

;----------------------------------------------------------------
;32bit符号あり整数を10進数の文字列に変換する(ゼロサプレスあり)
;<d0.l:32bit符号あり整数
;<a0.l:文字列バッファ
;>a0.l:文字列の直後
strDec:
	movem.l	d0-d2/a1,-(sp)
	tst.l	d0
	bne	1f
	move.b	#'0',(a0)+
	bra	6f

1:	bpl	2f
	move.b	#'-',(a0)+
	neg.l	d0
2:	lea.l	10f(pc),a1
3:	cmp.l	(a1)+,d0		;ゼロサプレスする
	blo	3b
	subq.l	#4,a1
	bra	5f

4:	addq.b	#1,d1
	sub.l	d2,d0			;10の累乗を何回引けるか数える
	bhs	4b
	add.l	d2,d0
	move.b	d1,(a0)+
5:	moveq.l	#'0'-1,d1
	move.l	(a1)+,d2
	bne	4b
6:	clr.b	(a0)
	movem.l	(sp)+,d0-d2/a1
	rts

;----------------------------------------------------------------
;32bit符号なし整数を10進数の文字列に変換する(桁数指定,飽和変換あり,ゼロサプレスなし)
;<d0.l:32bit符号なし整数
;<d1.l:桁数
;<a0.l:文字列バッファ
;>a0.l:文字列の直後
strDecN:
	movem.l	d0-d2/a1,-(sp)
	moveq.l	#10,d2
	sub.l	d1,d2			;10-桁数
	bhs	7f
	moveq.l	#0,d2
7:	lsl.w	#2,d2
	lea.l	10f(pc,d2.w),a1
	beq	5b			;10桁のときは飽和変換できない
	cmp.l	-4(a1),d0
	blo	5b			;範囲内
	move.l	-4(a1),d0
	subq.l	#1,d0			;最大値
	bra	5b

;10の累乗のテーブル
10:	.dc.l	1000000000
	.dc.l	100000000
	.dc.l	10000000
	.dc.l	1000000
	.dc.l	100000
	.dc.l	10000
	.dc.l	1000
	.dc.l	100
	.dc.l	10
	.dc.l	1
	.dc.l	0

;----------------------------------------------------------------
;32bit符号なし整数を16進数8桁の文字列に変換する
;<d0.l:32bit符号なし整数
;<a0.l:文字列バッファ
;>a0.l:文字列の直後
strHex8:
	movem.l	d0-d2,-(sp)
	moveq.l	#8-1,d2
1:	rol.l	#4,d0
	moveq.l	#$0F,d1
	and.b	d0,d1
	move.b	10f(pc,d1.w),(a0)+
	dbra	d2,1b
	clr.b	(a0)
	movem.l	(sp)+,d0-d2
	rts

10:	.dc.b	'0123456789ABCDEF'

;----------------------------------------------------------------
;文字列コピー
;<a0.l:コピー先
;<a1.l:コピー元
;>a0.l:コピー先の末尾の0の位置
;>a1.l:コピー元の末尾の0の直後の位置
strCopy:
@@:	move.b	(a1)+,(a0)+
	bne	@b
	subq.l	#1,a0
	rts

	PATCH_END



;----------------------------------------------------------------
;
;	クロック表示ルーチンでA6レジスタの最上位バイトが破壊される
;	http://stdkmd.com/bugsx68k/#rom_clocka6
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	メインメモリが9MBのとき起動時に19MBと表示される
;	http://stdkmd.com/bugsx68k/#rom_9mb
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	起動音を鳴らすとFM音源ドライバが誤動作することがある
;	http://stdkmd.com/bugsx68k/#rom_chime
;
;----------------------------------------------------------------

	PATCH_DATA	chime,$00FF10E0

	moveq.l	#1,d1
	moveq.l	#2,d2
	bsr	($00FF1138)PatchZL
	moveq.l	#1,d1
	moveq.l	#0,d2
	bsr	($00FF1138)PatchZL
	moveq.l	#8,d1

	PATCH_END



;----------------------------------------------------------------
;
;	起動メッセージのMemory Managiment Unitのスペルが間違っている
;	http://stdkmd.com/bugsx68k/#rom_mmu
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	電卓を使うと実行中のプログラムが誤動作することがある
;	http://stdkmd.com/bugsx68k/#rom_dentakud3
;
;----------------------------------------------------------------

	PATCH_DATA	dentakuD3_1,$00FF3BFC

	movem.l	d1-d3,-(sp)

	PATCH_END

	PATCH_DATA	dentakuD3_2,$00FF3C16

	movem.l	(sp)+,d1-d3

	PATCH_END

	PATCH_DATA	dentakuD3_3,$00FF3C1C

	movem.l	(sp)+,d1-d3

	PATCH_END

	PATCH_DATA	dentakuD3_4,$00FF3C5A

	movem.l	d1-d3,-(sp)

	PATCH_END

	PATCH_DATA	dentakuD3_5,$00FF3C76

	movem.l	(sp)+,d1-d3

	PATCH_END



;----------------------------------------------------------------
;
;	カーソルが画面の最下行にあると電卓が画面外に表示される
;	http://stdkmd.com/bugsx68k/#rom_dentaku64
;
;	電卓の表示位置を決める処理にY座標を調整するコードを追加する
;	直前にある電卓OFFルーチンを詰めてできた隙間にY座標を調整するコードを押し込んでそれを呼び出す
;
;----------------------------------------------------------------

	PATCH_DATA	dentaku64_1,$00FF4444

	move.w	d0,-(sp)
	move.l	#184<<16|16,-(sp)
	move.l	$0BFC.w,-(sp)		;[$0BFE.w].w:電卓表示左上Yドット座標
					;[$0BFC.w].w:電卓表示左上Xドット座標
	move.w	#2,-(sp)
	bsr	($00FF67C4)PatchZL	;_TXFILL実行
	clr.w	10(sp)
	addq.w	#1,(sp)			;3
	bsr	($00FF67C4)PatchZL	;_TXFILL実行
	lea.l	12(sp),sp
	move.w	(sp)+,d0
	beq	@f
	bsr	($00FFAA5C)PatchZL	;_MS_CURON
@@:	movem.l	(sp)+,d1-d7/a0-a6
	rts

dentaku64:
	addq.w	#1,d1			;電卓のテキストY座標
	cmp.w	#31,d1			;カーソルの次の行のY座標が31以下のときはカーソルの次の行
	bls	@f
	cmp.w	$0972.w,d1		;[$0972.w].w:テキストの行数-1
	bls	@f			;カーソルの次の行がコンソールの範囲内のときはカーソルの次の行
	move.w	$0972.w,d1		;[$0972.w].w:テキストの行数-1
	beq	@f			;コンソールが1行しかないときはコンソールの最下行
	subq.w	#1,d1			;それ以外はコンソールの下から2番目の行
@@:	rts

	PATCH_END

	PATCH_DATA	dentaku64_2,$00FF44A6

	patchbsr.s	dentaku64,dentaku64_1

	PATCH_END



;----------------------------------------------------------------
;
;	ソフトキーボードの↑キーの袋文字が閉じていない
;	http://stdkmd.com/bugsx68k/#rom_softkeyboard
;
;----------------------------------------------------------------

	PATCH_DATA	softkeyboard,$00FF5AA8

	.dc.b	__MMM_MM,M_______

	PATCH_END



;----------------------------------------------------------------
;
;	_DEFCHRでフォントサイズに0が指定されたとき8に読み替える処理が文字コードも0のときしか機能していない
;	_DEFCHRでフォントサイズに6を指定できない
;	_DEFCHRで_FNTADRがフォントパターンを$0C46.wに作成して返したときそこに上書きしても保存されないのにエラーにならない
;	_DEFCHRでフォントアドレスがX68030のハイメモリや060turboのローカルメモリを指しているとROMと誤認してエラーになる
;
;----------------------------------------------------------------

	PATCH_DATA	defchr,$00FF6ADA

	movem.l	d1-d2/a1,-(sp)
	move.l	d1,d2
	swap.w	d2
	movea.l	$0458.w,a0		;[$0458.w].l:[$0116]_FNTADR
	jsr	(a0)
;<d0.l:フォントアドレス
;<d1.w:横方向のバイト数-1
;<d2.w:縦方向のドット数-1
	movea.l	d0,a0			;フォントアドレス
	cmpa.w	#$0C46.w,a0		;[$0C46.w].b[72]:_FNTADRのフォント作成バッファ
	beq.s	4f			;フォントパターンが$0C46.wに作成された。ROMではないがそこに上書きしても保存されないので失敗
	swap.w	d0
	cmp.w	#$00F0,d0
	blo.s	1f			;フォントアドレスがROMを指していない
	cmp.w	#$0100,d0
	blo.s	4f			;フォントアドレスがROMを指している。上書きできないので失敗
;フォントアドレスがROMを指していない
;フォントパターンをコピーする
1:	move.w	d1,d0			;(d1+1)*(d2+1)-1=d1*d2+d1+d2
	mulu.w	d2,d0
	add.w	d1,d0
	add.w	d2,d0
2:	move.b	(a1)+,(a0)+
	dbra.w	d0,2b
	moveq.l	#0,d0
3:	movem.l	(sp)+,d1-d2/a1
	rts

4:	moveq.l	#-1,d0
	bra.s	3b

	PATCH_END



;----------------------------------------------------------------
;
;	_CRTMODが指定された画面モードと異なる色数でグラフィックパレットを初期化する
;	http://stdkmd.com/bugsx68k/#rom_crtmod_gpalet
;
;----------------------------------------------------------------

	PATCH_DATA	crtmodGpalet,$00FF6D70

	moveq.l	#%1100,d0
	and.b	$093C.w,d0		;[$093C.w].b:画面モード
	lsr.w	#1,d0
	move.w	@f(pc,d0.w),d0
	jsr	@f(pc,d0.w)
	bset.b	#3,$00E80028		;[$00E80028].w:CRTC R20 メモリモード/解像度(高解像度|垂直解像度|水平解像度)
	rts

@@:
	.dc.w	($00FFB3F4)PatchZL-@b	;CRTMOD(0～3)→グラフィック16色標準パレット設定
	.dc.w	($00FFB3F4)PatchZL-@b	;CRTMOD(4～7)→グラフィック16色標準パレット設定
	.dc.w	($00FFB408)PatchZL-@b	;CRTMOD(8～11)→グラフィック256色標準パレット設定
	.dc.w	($00FFB41E)PatchZL-@b	;CRTMOD(12～15)→グラフィック65536色標準パレット設定

	PATCH_END



;----------------------------------------------------------------
;
;	DMA転送開始直前のキャッシュフラッシュ
;
;----------------------------------------------------------------

	PATCH_DATA	dmaCacheFlush,$00FF8284

	jmp	cacheFlush

	PATCH_END



;----------------------------------------------------------------
;
;	特定のSCSI機器が接続されていると起動できない
;	http://stdkmd.com/bugsx68k/#rom_fds120
;
;	_S_INQUIRYと_S_REQUESTのアロケーション長が足りない
;	InquiryはEVPDが0のときアロケーション長が5バイト以上でなければならないのに1バイトになっている
;	Request Senseのセンスデータはエラークラスによって4バイトまたは8バイト以上だがアロケーション長が3バイトになっている
;	起動時にアロケーション長よりも多くのデータを返そうとするSCSI機器が接続されて電源が入っているとハングアップする
;	Inquiryは1回目に5バイト要求して2回目に追加データ長+5バイト要求するのが正しい
;	最初から36バイト要求しても良いが2回に分ける方が無難
;	参考:FDS021.LZH/FDS120T.DOC
;
;----------------------------------------------------------------

	PATCH_DATA	srequest3_1,$00FF93A2

	moveq.l	#8,d3

	PATCH_END

	PATCH_DATA	sinquiry1,$00FF93F2

	moveq.l	#5,d3

	PATCH_END

	PATCH_DATA	srequest3_2,$00FF9522

	moveq.l	#8,d3

	PATCH_END



;----------------------------------------------------------------
;
;	ブロック長が2048バイト以上のSCSI機器(CD-ROMなど)から起動できるようにする
;
;	FORMAT.XによってSCSI HDに書き込まれたSCSI-BIOSは対応していないが、
;	差し替えられるのは組み込み済みのSCSI-BIOSのレベルが3以下のときだけなので問題ない
;
;	SCSIINROM/SCSIEXROMを作るときリロケートするブロックの中でパッチデータを完結させるためFF1を使用していることに注意
;	実機では動作しない
;
;----------------------------------------------------------------

	PATCH_DATA	scsi2048_1,$00FF9436

	move.l	4(a1),d0
;!!! XEiJ以外では動作しない
	.dc.w	$04C0			;ff1.l d0
	moveq.l	#23,d5
	sub.l	d0,d5
;セクタ0を読み込む
	moveq.l	#0,d2			;0x00000000から
	moveq.l	#4,d3			;0x00000400バイト
	patchbsr.w	scsi2048Sub,scsi2048_3	;1ブロックのサイズに応じてブロック番号とブロック数を調整する
;	moveq.l	#_SCSIDRV,d0

	PATCH_END

	PATCH_DATA	scsi2048_2,$00FF9570

	move.l	4(a1),d0
;!!! XEiJ以外では動作しない
	.dc.w	$04C0			;ff1.l d0
	moveq.l	#23,d5
	sub.l	d0,d5
	moveq.l	#8,d2			;0x00000800から
	moveq.l	#4,d3			;0x00000400バイト
	patchbsr.w	scsi2048Sub,scsi2048_3	;1ブロックのサイズに応じてブロック番号とブロック数を調整する
;	moveq.l	#_SCSIDRV,d0

	PATCH_END

	PATCH_DATA	scsi2048_3,$00FF969C

scsi2048Sub:
	moveq.l	#-1,d0
	lsl.l	d5,d0
	not.l	d0
	add.l	d0,d2
	add.l	d0,d3
	lsr.l	d5,d2
	lsr.l	d5,d3
	moveq.l	#_SCSIDRV,d0
	rts

	PATCH_END



;----------------------------------------------------------------
;
;	_MS_LIMITでY方向の範囲を1007までしか設定できない
;	http://stdkmd.com/bugsx68k/#rom_mslimit
;
;----------------------------------------------------------------

	PATCH_DATA	mslimit_1,$00FFABA4

	cmp.w	#$0400,d1

	PATCH_END

	PATCH_DATA	mslimit_2,$00FFABB4

	cmp.w	#$0400,d2

	PATCH_END



;----------------------------------------------------------------
;
;	_MS_ONTMのキャッシュ制御
;
;----------------------------------------------------------------

	PATCH_DATA	msOntmCache_1,$00FFAC72

	jsr	cacheOnI	;データキャッシュOFF,命令キャッシュON
	move.l	d0,-(sp)
	bra	($00FFAC86)PatchZL

	PATCH_END

	PATCH_DATA	msOntmCache_2,$00FFACE8

	move.l	(sp)+,d2
	jsr	cacheSet	;キャッシュ設定
	bra	($00FFACF6)PatchZL

	PATCH_END



;----------------------------------------------------------------
;
;	_GPALETで65536色モードのパレットを正しく取得できない
;	http://stdkmd.com/bugsx68k/#rom_gpalet
;
;----------------------------------------------------------------

	PATCH_DATA	gpalet,$00FFB740

	move.b	2(a0,d0.w),d3

	PATCH_END



;----------------------------------------------------------------
;
;	IOCS _SYS_STAT
;
;	_SYS_STATのコードが間違っている
;	http://stdkmd.com/bugsx68k/#rom_sysstat
;
;----------------------------------------------------------------

	PATCH_DATA	sysstat,$00FFC75A

	jmp	iocsSysStat

	PATCH_EXTRA

;----------------------------------------------------------------
;IOCS _SYS_STAT
;$AC システム環境の取得と設定
;<d1.w:モード
;	0	MPUステータスの取得
;		>d0.l:MPUステータス
;			bit0～7		MPUの種類(0=68000,3=68030,4=68040,6=68060)
;			bit14		MMUの有無(0=なし,1=あり)
;			bit15		FPU/FPCPの有無(0=なし,1=あり)
;			bit16～31	クロックスピード*10
;	1	キャッシュ状態の取得
;		>d0.l:現在のキャッシュ状態
;			bit0	命令キャッシュの状態(0=無効,1=有効)
;			bit1	データキャッシュの状態(0=無効,1=有効)
;	2	キャッシュの状態をSRAM設定値にする
;		>d0.l:設定後のキャッシュ状態
;			bit0	命令キャッシュの状態(0=無効,1=有効)
;			bit1	データキャッシュの状態(0=無効,1=有効)
;	3	キャッシュフラッシュ
;	4	キャッシュ設定
;		<d2.w:キャッシュの設定
;			bit0	命令キャッシュの状態(0=無効,1=有効)
;			bit1	データキャッシュの状態(0=無効,1=有効)
;		>d0.l:設定前のキャッシュ状態
;			bit0	命令キャッシュの状態(0=無効,1=有効)
;			bit1	データキャッシュの状態(0=無効,1=有効)
iocsSysStat:
	moveq.l	#-1,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	bhs	2f			;020/030/040/060
;000/010
	tst.w	d1
	bne	9f			;000/010かつd1!=0のときはエラー
2:	cmp.w	#4,d1
	bhi	9f
	move.w	d1,-(sp)
	add.w	d1,d1
	move.w	10f(pc,d1.w),d1
	jsr	10f(pc,d1.w)
	move.w	(sp)+,d1
9:	rts

10:
	.dc.w	mpuStat-10b		;0:MPUステータスの取得
	.dc.w	cacheGet-10b		;1:キャッシュ状態の取得
	.dc.w	cacheLoad-10b		;2:キャッシュの状態をSRAM設定値にする
	.dc.w	cacheFlush-10b		;3:キャッシュフラッシュ
	.dc.w	cacheSet-10b		;4:キャッシュ設定

;----------------------------------------------------------------
;IOCS _SYS_STAT		0
;MPUステータスの取得
;>d0.l:MPUステータス
;	bit0～7		MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
;	bit14		MMUの有無(0=なし,1=あり)
;	bit15		FPU/FPCPの有無(0=なし,1=あり)
;	bit16～31	クロックスピード*10
mpuStat:
	moveq.l	#12,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	@f
;020/030/040/060
	moveq.l	#6,d0
@@:	mulu.w	$0CB8.w,d0		;[$0CB8.w].w:[11,12,13]ROM計測クロック($0342≒10*250/3,$056E≒16.67*250/3,$104D≒25*500/3)
					;MHz値*250/3*12=MHz値*1000、または、MHz値*500/3*6=MHz値*1000
	add.l	#50,d0
	divu.w	#100,d0			;MHz値*10
	swap.w	d0			;ssssssss ssssssss ........ ........
	clr.w	d0			;ssssssss ssssssss 00000000 00000000
	tst.b	$0CBE.w			;[$0CBE.w].b:MMUの有無(0=なし,-1=あり)
	sne.b	d0			;ssssssss ssssssss 00000000 mmmmmmmm
	ror.w	#1,d0			;ssssssss ssssssss m0000000 0mmmmmmm
	tst.b	$0CBD.w			;[$0CBD.w].b:FPU/FPCPの有無(0=なし,-1=あり)
	sne.b	d0			;ssssssss ssssssss m0000000 ffffffff
	ror.w	#1,d0			;ssssssss ssssssss fm000000 0fffffff
	move.b	$0CBC.w,d0		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
					;ssssssss ssssssss fm000000 pppppppp
	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		1
;キャッシュ状態の取得
;>d0.l:現在キャッシュ状態
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは0を返す
cacheGet:
	moveq.l	#0,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	.cpu	68030
	movec.l	cacr,d0
	.cpu	68000
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	bhs	4f
;020/030				;........ ........ .......d .......i
	ror.l	#1,d0			;i....... ........ ........ d.......
	rol.b	#1,d0			;i....... ........ ........ .......d
	bra	8f
;040/060				;d....... ........ i....... ........
4:	swap.w	d0			;i....... ........ d....... ........
	rol.w	#1,d0			;i....... ........ ........ .......d
8:	rol.l	#1,d0			;........ ........ ........ ......di
	and.l	#3,d0			;00000000 00000000 00000000 000000di
9:	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		2
;キャッシュの状態をSRAM設定値にする
;>d0.l:設定後のキャッシュ状態
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは0を返す
cacheLoad:
	move.l	d2,-(sp)
	moveq.l	#0,d2			;念のため
	move.b	$00ED0090,d2		;[$00ED0090].b:SRAM [13]キャッシュ(------|データ|命令)
	bsr	cacheSet		;キャッシュ設定
	move.l	(sp)+,d2
	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		3
;キャッシュフラッシュ
cacheFlush:
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	bhs	4f
;020/030
	move.l	d0,-(sp)
	.cpu	68030
	movec.l	cacr,d0
	or.w	#$0808,d0
	movec.l	d0,cacr
	and.w	#$F7F7,d0
	movec.l	d0,cacr
	.cpu	68000
	move.l	(sp)+,d0
9:	rts
;040/060
4:	.cpu	68040
	cpusha	bc
	.cpu	68000
	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		4
;キャッシュ設定
;<d2.l:キャッシュの設定
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは無意味
;>d0.l:設定前のキャッシュ状態
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは0を返す
cacheSet:
	bsr	cacheGet		;キャッシュ状態の取得
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	move.l	d1,-(sp)
	move.w	d2,-(sp)		;順序とサイズに注意
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	bhs	4f
;020/030
	moveq.l	#1,d1
	and.w	d2,d1			;00000000 00000000 00000000 0000000i
	and.w	#2,d2			;........ ........ 00000000 000000d0
	neg.w	d2			;........ ........ dddddddd ddddddd0
	and.w	#$2100,d2		;........ ........ 00d0000d 00000000
	or.w	d2,d1			;........ ........ 00d0000d 0000000i
	.cpu	68030
	movec.l	d1,cacr
	.cpu	68000
	bra	8f
;040/060
4:	.cpu	68040
	movec.l	cacr,d1
	lsr.w	#1,d2			;FEDCBA98 76543210 FEDCBA98 76543210 i
	addx.w	d1,d1			;FEDCBA98 76543210 EDCBA987 6543210i .
	ror.w	#1,d1			;FEDCBA98 76543210 iEDCBA98 76543210 .
	lsr.w	#1,d2			;FEDCBA98 76543210 iEDCBA98 76543210 d
	addx.l	d1,d1			;EDCBA987 6543210i EDCBA987 6543210d .
	ror.l	#1,d1			;dEDCBA98 76543210 iEDCBA98 76543210 .
	movec.l	d1,cacr
	move.w	(sp),d2
	not.w	d2
	and.w	d0,d2			;bit1=データキャッシュON→OFF,bit0=命令キャッシュON→OFF
	beq	8f
	subq.w	#2,d2
	bhi	3f
	blo	1f
	cpusha	dc			;2:データキャッシュをOFFにしたときプッシュしてから無効化する
	bra	8f
3:	cpusha	dc			;3:データキャッシュをOFFにしたときプッシュしてから無効化する
1:	cinva	ic			;1:3:命令キャッシュをOFFにしたとき無効化する
	.cpu	68000
8:	move.w	(sp)+,d2
	move.l	(sp)+,d1
9:	rts

;----------------------------------------------------------------
;キャッシュOFF
;>d0.l:設定前のキャッシュ状態
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは0を返す
cacheOff:
	move.l	d2,-(sp)
	moveq.l	#0,d2
	bsr	cacheSet		;キャッシュ設定
	move.l	(sp)+,d2
	rts

;----------------------------------------------------------------
;データキャッシュOFF,命令キャッシュON
;>d0.l:設定前のキャッシュ状態
;	bit0	命令キャッシュの状態(0=無効,1=有効)
;	bit1	データキャッシュの状態(0=無効,1=有効)
;	000/010のときは0を返す
cacheOnI:
	move.l	d2,-(sp)
	moveq.l	#1,d2			;データキャッシュOFF,命令キャッシュON
	bsr	cacheSet		;キャッシュ設定
	move.l	(sp)+,d2
	rts

	PATCH_END



;----------------------------------------------------------------
;
;	MC68060のときSPCのTEMP,TCH,TCM,TCLへのライトで使われているMOVEPを展開する
;
;	IOCS _SCSIDRVを詰めてMOVEPの展開コードを押し込む
;	展開しなくてもMOVEPエミュレーションで問題なく動作するが、デバッグ中のコードと関係のない例外は邪魔なので展開しておく
;
;	SCSIINROM/SCSIEXROMを作るときA6レジスタにSCSIポートベースアドレスを設定する命令の位置が変わっていることに注意
;
;----------------------------------------------------------------

	PATCH_DATA	scsiMovep_1,$00FFCCB8

;----------------------------------------------------------------
;IOCS _SCSIDRVルーチン
;<d0.l:$F5
;<d1.l:SCSIコール番号
iocsScsidrv:
	movem.l	d1/d3/a1-a2/a6,-(sp)
	movea.l	($00FF933C)PatchZL(pc),a6	;SCSIポートベースアドレス
	moveq.l	#(iocsSJmptblEnd-iocsSJmptbl)/2,d0
	cmp.l	d0,d1
	blo	@f
	moveq.l	#-1,d1			;未定義
@@:	add.w	d1,d1
	move.w	iocsSJmptbl(pc,d1.w),d1
	jsr	iocsSJmptbl(pc,d1.w)
	movem.l	(sp)+,d1/d3/a1-a2/a6
	rts

iocsSReserved:
	moveq.l	#-1,d0
	rts

	.dc.w	iocsSReserved-iocsSJmptbl	;未定義
iocsSJmptbl:
	.dc.w	($00FFCE0E)PatchZL-iocsSJmptbl	;SCSIコール$00 _S_RESET
	.dc.w	($00FFCEFC)PatchZL-iocsSJmptbl	;SCSIコール$01 _S_SELECT
	.dc.w	($00FFCED6)PatchZL-iocsSJmptbl	;SCSIコール$02 _S_SELECTA
	.dc.w	($00FFCFCA)PatchZL-iocsSJmptbl	;SCSIコール$03 _S_CMDOUT
	.dc.w	($00FFD5C0)PatchZL-iocsSJmptbl	;SCSIコール$04 _S_DATAIN
	.dc.w	($00FFD578)PatchZL-iocsSJmptbl	;SCSIコール$05 _S_DATAOUT
	.dc.w	($00FFD0AA)PatchZL-iocsSJmptbl	;SCSIコール$06 _S_STSIN
	.dc.w	($00FFD0E8)PatchZL-iocsSJmptbl	;SCSIコール$07 _S_MSGIN
	.dc.w	($00FFD126)PatchZL-iocsSJmptbl	;SCSIコール$08 _S_MSGOUT
	.dc.w	($00FFD192)PatchZL-iocsSJmptbl	;SCSIコール$09 _S_PHASE
	.dc.w	($00FFD1A6)PatchZL-iocsSJmptbl	;SCSIコール$0A _S_LEVEL
	.dc.w	($00FFD066)PatchZL-iocsSJmptbl	;SCSIコール$0B _S_DATAINI
	.dc.w	($00FFD026)PatchZL-iocsSJmptbl	;SCSIコール$0C _S_DATAOUTI
	.dc.w	($00FFD162)PatchZL-iocsSJmptbl	;SCSIコール$0D _S_MSGOUTEXT
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$0E
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$0F
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$10
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$11
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$12
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$13
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$14
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$15
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$16
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$17
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$18
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$19
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1A
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1B
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1C
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1D
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1E
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$1F
	.dc.w	($00FFD612)PatchZL-iocsSJmptbl	;SCSIコール$20 _S_INQUIRY
	.dc.w	($00FFD742)PatchZL-iocsSJmptbl	;SCSIコール$21 _S_READ
	.dc.w	($00FFD79C)PatchZL-iocsSJmptbl	;SCSIコール$22 _S_WRITE
	.dc.w	($00FFD92C)PatchZL-iocsSJmptbl	;SCSIコール$23 _S_FORMAT
	.dc.w	($00FFD3F0)PatchZL-iocsSJmptbl	;SCSIコール$24 _S_TESTUNIT
	.dc.w	($00FFD49C)PatchZL-iocsSJmptbl	;SCSIコール$25 _S_READCAP
	.dc.w	($00FFD7FA)PatchZL-iocsSJmptbl	;SCSIコール$26 _S_READEXT
	.dc.w	($00FFD860)PatchZL-iocsSJmptbl	;SCSIコール$27 _S_WRITEEXT
	.dc.w	($00FFD900)PatchZL-iocsSJmptbl	;SCSIコール$28 _S_VERIFYEXT
	.dc.w	($00FFD68A)PatchZL-iocsSJmptbl	;SCSIコール$29 _S_MODESENSE
	.dc.w	($00FFD6CA)PatchZL-iocsSJmptbl	;SCSIコール$2A _S_MODESELECT
	.dc.w	($00FFD40E)PatchZL-iocsSJmptbl	;SCSIコール$2B _S_REZEROUNIT
	.dc.w	($00FFD64E)PatchZL-iocsSJmptbl	;SCSIコール$2C _S_REQUEST
	.dc.w	($00FFD9F8)PatchZL-iocsSJmptbl	;SCSIコール$2D _S_SEEK
	.dc.w	($00FFD42C)PatchZL-iocsSJmptbl	;SCSIコール$2E _S_READI
	.dc.w	($00FFD992)PatchZL-iocsSJmptbl	;SCSIコール$2F _S_STARTSTOP
	.dc.w	($00FFD9C4)PatchZL-iocsSJmptbl	;SCSIコール$30 _S_SEJECT
	.dc.w	($00FFD70A)PatchZL-iocsSJmptbl	;SCSIコール$31 _S_REASSIGN
	.dc.w	($00FFD960)PatchZL-iocsSJmptbl	;SCSIコール$32 _S_PAMEDIUM
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$33
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$34
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSIコール$35
	.dc.w	($00FFDA2C)PatchZL-iocsSJmptbl	;SCSIコール$36 _S_DSKINI
	.dc.w	($00FFDA6E)PatchZL-iocsSJmptbl	;SCSIコール$37 _S_FORMATB
	.dc.w	($00FFDAA6)PatchZL-iocsSJmptbl	;SCSIコール$38 _S_BADFMT
	.dc.w	($00FFDADE)PatchZL-iocsSJmptbl	;SCSIコール$39 _S_ASSIGN
iocsSJmptblEnd:

;MOVEP.L D0,$0017(A6)
;<d0.l:データ
;<a6.l:SCSIポートベースアドレス
scsiMovep:
	cmpi.b	#6,$0CBC.w		;[$0CBC.w].b:[13]MPUの種類(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	beq	@f
;MC68060でないときMOVEP命令を使う
	movep.l	d0,$0017(a6)		;[$00E96037].b:SPC TEMP Temporary Register
					;[$00E96039].b:SPC TCH Transfer Counter High
					;[$00E9603B].b:SPC TCM Transfer Counter Mid
					;[$00E9603D].b:SPC TCL Transfer Counter Low
	rts

;MC68060のときMOVEP命令を使わない
@@:	rol.l	#8,d0
	move.b	d0,$0017(a6)		;[$00E96037].b:SPC TEMP Temporary Register
	rol.l	#8,d0
	move.b	d0,$0019(a6)		;[$00E96039].b:SPC TCH Transfer Counter High
	rol.l	#8,d0
	move.b	d0,$001B(a6)		;[$00E9603B].b:SPC TCM Transfer Counter Mid
	rol.l	#8,d0
	move.b	d0,$001D(a6)		;[$00E9603D].b:SPC TCL Transfer Counter Low
	rts

;IOCS _SCSIDRVの転送バイト数の計算
;	d5=3とd5=4のどちらも2048バイト/ブロックとみなす
;<d3.l:転送ブロック数
;<d5.b:ブロック長(0=256,1=512,2=1024,3=2048,4=2048)
;<a3.l:バッファのアドレス
;>d3.l:転送バイト数
;>a1.l:バッファのアドレス
scsidrv2048Sub:
	lsl.l	#8,d3
	cmp.b	#3,d5
	bcc	1f
	lsl.l	d5,d3
	bra	2f
1:	lsl.l	#3,d3
2:	rts

	PATCH_END

	PATCH_DATA	scsiMovep_2,$00FFCF46

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsiMovep_3,$00FFCF96

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsiMovep_4,$00FFD1CA

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsiMovep_5,$00FFD28C

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsiMovep_6,$00FFDB34

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsiMovep_7,$00FFDB96

	patchbsr.w	scsiMovep,scsiMovep_1

	PATCH_END



;----------------------------------------------------------------
;
;	IOCS _SCSIDRVの転送バイト数の計算を2048バイト/ブロックに対応させる
;
;	d5=3とd5=4のどちらも2048バイト/ブロックとみなす
;	4096バイト/ブロック以上は非対応とする
;
;----------------------------------------------------------------

	PATCH_DATA	scsidrv2048_readi,$00FFD460

	patchbsr.w	scsidrv2048Sub,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsidrv2048_read,$00FFD778

	patchbsr.w	scsidrv2048Sub,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsidrv2048_write,$00FFD7D2

	patchbsr.w	scsidrv2048Sub,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsidrv2048_readext,$00FFD83A

	patchbsr.w	scsidrv2048Sub,scsiMovep_1

	PATCH_END

	PATCH_DATA	scsidrv2048_writeext,$00FFD89C

	patchbsr.w	scsidrv2048Sub,scsiMovep_1

	PATCH_END



;----------------------------------------------------------------
;
;	グラフィック関係のIOCSコールがリバースモードになったままになる
;	http://stdkmd.com/bugsx68k/#rom_drawmode
;
;----------------------------------------------------------------

	PATCH_DATA	drawmode,$00FFDCEA

	cmp.w	#-1,d1

	PATCH_END



;----------------------------------------------------------------
;
;	パッチデータの末尾
;
;----------------------------------------------------------------

	PATCH_TAIL



	.end
