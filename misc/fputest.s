;========================================================================================
;  fputest.s
;  Copyright (C) 2003-2016 Makoto Kamada
;
;  This file is part of the XEiJ (X68000 Emulator in Java).
;  You can use, modify and redistribute the XEiJ if the conditions are met.
;  Read the XEiJ License for more details.
;  http://stdkmd.com/xeij/
;========================================================================================

;----------------------------------------------------------------
;
;	浮動小数点命令の動作テスト
;
;----------------------------------------------------------------

	.include	doscall.mac
	.include	iocscall.mac

	.include	stresc.mac

	.cpu	68060

;----------------------------------------------------------------
	.text
	.align	4,$2048
entry:

;check FPU
	lea.l	$0CBD.w,a1
	IOCS	_B_BPEEK
	tst.b	d0
	bne	@f
	leastr	'ERROR: FPU is not available\r\n',a0
	bra	abort
@@:

;open log file
	move.w	#$0020,-(sp)
	peastr	'fputest.log'
	DOS	_CREATE
	addq.l	#6,sp
	tst.l	d0
	bpl	@f
	leastr	'ERROR: cannot create fputest.log\r\n',a0
	bra	abort
@@:	move.w	d0,log_handle

;test
	clr.l	total_tested
	clr.l	total_failed

	lea.l	$0CBC.w,a1
	IOCS	_B_BPEEK
	cmp.b	#3,d0
	bne	1f
;MC68882
	bsr	test_fmovecr
	bsr	test_fmove
	bsr	test_fint
	bsr	test_fsinh
	bsr	test_fintrz
	bsr	test_fsqrt
	bsr	test_flognp1
	bsr	test_fetoxm1
	bsr	test_ftanh
	bsr	test_fatan
	bsr	test_fasin
	bsr	test_fatanh
	bsr	test_fsin
	bsr	test_ftan
	bsr	test_fetox
	bsr	test_ftwotox
	bsr	test_ftentox
	bsr	test_flogn
	bsr	test_flog10
	bsr	test_flog2
	bsr	test_fabs
	bsr	test_fcosh
	bsr	test_fneg
	bsr	test_facos
	bsr	test_fcos
	bsr	test_fgetexp
	bsr	test_fgetman
	bsr	test_fdiv
	bsr	test_fmod
	bsr	test_fadd
	bsr	test_fmul
	bsr	test_fsgldiv882		;MC68882
	bsr	test_frem
	bsr	test_fscale
	bsr	test_fsglmul882		;MC68882
	bsr	test_fsub
	bsr	test_fsincos
	bsr	test_fcmp
	bsr	test_ftst
	bsr	test_fbcc882		;MC68882
	bra	2f

1:	cmp.b	#6,d0
	bne	2f
;MC68060
	bsr	test_fmovecr
	bsr	test_fmove
	bsr	test_fint
	bsr	test_fsinh
	bsr	test_fintrz
	bsr	test_fsqrt
	bsr	test_flognp1
	bsr	test_fetoxm1
	bsr	test_ftanh
	bsr	test_fatan
	bsr	test_fasin
	bsr	test_fatanh
	bsr	test_fsin
	bsr	test_ftan
	bsr	test_fetox
	bsr	test_ftwotox
	bsr	test_ftentox
	bsr	test_flogn
	bsr	test_flog10
	bsr	test_flog2
	bsr	test_fabs
	bsr	test_fcosh
	bsr	test_fneg
	bsr	test_facos
	bsr	test_fcos
	bsr	test_fgetexp
	bsr	test_fgetman
	bsr	test_fdiv
	bsr	test_fmod
	bsr	test_fadd
	bsr	test_fmul
	bsr	test_fsgldiv060		;MC68060
	bsr	test_frem
	bsr	test_fscale
	bsr	test_fsglmul060		;MC68060
	bsr	test_fsub
	bsr	test_fsincos
	bsr	test_fcmp
	bsr	test_ftst
	bsr	test_fsmove
	bsr	test_fssqrt
	bsr	test_fdmove
	bsr	test_fdsqrt
	bsr	test_fsabs
	bsr	test_fsneg
	bsr	test_fdabs
	bsr	test_fdneg
	bsr	test_fsdiv
	bsr	test_fsadd
	bsr	test_fsmul
	bsr	test_fddiv
	bsr	test_fdadd
	bsr	test_fdmul
	bsr	test_fssub
	bsr	test_fdsub
	bsr	test_fbcc060		;MC68060
2:

	lea.l	buffer,a0
	leastr	'\r\nTotal\r\n',a1
	bsr	strcpy
;tested
	leastr	'tested:',a1
	bsr	strcpy
	move.l	total_tested,d0		;tested
	bsr	utos
;passed
	move.l	total_tested,d0		;tested
	sub.l	total_failed,d0		;tested-failed=passed
	beq	@f
	leastr	', passed:',a1
	bsr	strcpy
	bsr	utos
	move.l	total_tested,d1
	bsr	put_ratio
@@:
;failed
	move.l	total_failed,d0		;failed
	beq	@f
	leastr	', failed:',a1
	bsr	strcpy
	bsr	utos
	move.l	total_tested,d1
	bsr	put_ratio
@@:
	move.b	#13,(a0)+
	move.b	#10,(a0)+
	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print

;close log file
	move.w	log_handle,-(sp)
	DOS	_CLOSE
	addq.l	#2,sp

;exit
exit:
	DOS	_EXIT

abort:
	move.w	#2,-(sp)		;stderr
	move.l	a0,-(sp)
	DOS	_FPUTS
	addq.l	#6,sp
	bra	exit

	.bss
	.align	4
total_tested:
	.ds.l	1
total_failed:
	.ds.l	1



;----------------------------------------------------------------
;<a0.l:message
	.text
	.align	4,$2048
log_print:
	move.l	d0,-(sp)
	move.w	log_handle,-(sp)	;log file
	move.l	a0,-(sp)		;message
	DOS	_FPUTS
;	move.w	#1,4(sp)		;stdout
;	DOS	_FPUTS
	addq.l	#6,sp
	move.l	(sp)+,d0
	rts

	.bss
	.even
log_handle:
	.ds.w	1



MI	equ	$08000000
ZE	equ	$04000000
IN	equ	$02000000
NA	equ	$01000000
BS	equ	$00008000
SN	equ	$00004000
OE	equ	$00002000
OF	equ	$00001000
UF	equ	$00000800
DZ	equ	$00000400
X2	equ	$00000200
X1	equ	$00000100
AV	equ	$00000080
AO	equ	$00000040
AU	equ	$00000020
AZ	equ	$00000010
AX	equ	$00000008



;----------------------------------------------------------------
;<a1.l:extended
	.text
	.align	4,$2048
put_extended:
	movem.l	d0/a1,-(sp)
	move.b	#'$',(a0)+
	move.l	(a1)+,d0
	bsr	h8tos
	move.b	#',',(a0)+
	move.b	#'$',(a0)+
	move.l	(a1)+,d0
	bsr	h8tos
	move.b	#',',(a0)+
	move.b	#'$',(a0)+
	move.l	(a1)+,d0
	bsr	h8tos
	movem.l	(sp)+,d0/a1
	rts

;----------------------------------------------------------------
;<d0.l:fpsr
	.text
	.align	4,$2048
put_fpsr:
	movem.l	d0-d5/a1,-(sp)
	move.l	d0,d5
	sf.b	d4
;condition code byte
	lea.l	10f(pc),a1
	moveq.l	#27,d1
1:	move.b	(a1)+,d2
	move.b	(a1)+,d3
	btst.l	d1,d5
	beq	2f
	move.b	d2,(a0)+
	move.b	d3,(a0)+
	move.b	#'+',(a0)+
	st.b	d4
2:	subq.w	#1,d1
	cmp.w	#24,d1
	bhs	1b
;quotient byte
	btst.l	#23,d5
	beq	1f
	leastr	'(1<<23)+',a1
	bsr	strcpy
	st.b	d4
1:
	move.l	d5,d0
	and.l	#$007F0000,d0
	beq	2f
	swap.w	d0
	move.b	#'(',(a0)+
	bsr	utos
	leastr	'<<16)+',a1
	bsr	strcpy
	st.b	d4
2:
;exception byte, accrued exception byte
	lea.l	11f(pc),a1
	moveq.l	#15,d1
1:	move.b	(a1)+,d2
	move.b	(a1)+,d3
	btst.l	d1,d5
	beq	2f
	move.b	d2,(a0)+
	move.b	d3,(a0)+
	move.b	#'+',(a0)+
	st.b	d4
2:	subq.w	#1,d1
	cmp.w	#3,d1
	bhs	1b
	tst.b	d4
	beq	3f
	sf.b	-(a0)
	bra	9f

3:	move.b	#'0',(a0)+
	sf.b	(a0)
9:	movem.l	(sp)+,d0-d5/a1
	rts

;		 27   26   25   24
10:	.dc.b	'MI','ZE','IN','NA'
;		 15   14   13   12   11   10    9    8    7    6    5    4    3
11:	.dc.b	'BS','SN','OE','OF','UF','DZ','X2','X1','AV','AO','AU','AZ','AX'
	.even



;----------------------------------------------------------------
	.offset	0
record_tested:	.ds.l	1
record_failed:	.ds.l	1
  .irp n,63,62,61,60,59,58,57,56,55,54
record_level&n:	.ds.l	1
  .endm
record_size:

;----------------------------------------------------------------
	.text
	.align	4,$2048
;<a6.l:record
record_clear:
	clr.l	(record_tested,a6)
	clr.l	(record_failed,a6)
  .irp n,63,62,61,60,59,58,57,56,55,54
	clr.l	(record_level&n,a6)
  .endm
	rts

;----------------------------------------------------------------
	.text
	.align	4,$2048
;<a1.l:actual destination, fpsr
;<a2.l:expected destination, fpsr
;<a6.l:record
;>d0.l:0=passed,-1=failed
record_check_1:
	movem.l	a1-a2,-(sp)
;	move.l	#0,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#8,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#8,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#8,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lea.l	-16(a1),a1
	lea.l	-16(a2),a2
	addq.l	#1,(record_tested,a6)
	tst.l	d0
	beq	9f
	addq.l	#1,(record_failed,a6)
	tst.b	d0
	bne	8f			;wrong fpsr
	move.l	12(a2),d0		;fpsr
	and.l	#ZE+IN+NA,d0
	bne	8f			;±0,±Inf,NaN but the extended data does not match

;check accuracy
	fmove.l	#0,fpcr			;fpcr
	fmove.x	(a1),fp0		;actual destination
	fabs.x	fp0,fp0			;abs(actual)
	flog2.x	fp0,fp0			;log2(abs(actual))
	fmove.x	(a2),fp1		;expected destination
	fabs.x	fp1,fp1			;abs(expected)
	flog2.x	fp1,fp1			;log2(abs(expected))
	fcmp.x	fp1,fp0
	fblt	@f
	fmove.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))
@@:	fmove.x	(a1),fp1		;actual destination
	fsub.x	(a2),fp1		;actual-expected
	fabs.x	fp1,fp1			;abs(actual-expected)
	flog2.x	fp1,fp1			;log2(abs(actual-expected))
	fsub.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))-log2(abs(actual-expected)), number of correct bits
  .irp n,63,62,61,60,59,58,57,56,55,54
	fcmp.s	#&n.0,fp0
	fblt	1f
	addq.l	#1,(record_level&n,a6)
	bra	2f
1:
  .endm
2:

8:	moveq.l	#-1,d0
9:	movem.l	(sp)+,a1-a2
	rts

;----------------------------------------------------------------
	.text
	.align	4,$2048
;<a1.l:actual destination, co-destination, fpsr
;<a2.l:expected destination, co-destination, fpsr
;<a6.l:record
;>d0.l:0=passed,-1=failed
record_check_2:
	movem.l	a1-a2,-(sp)
;	moveq.l	#0,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d1
	lsl.l	#4,d1
	cmpm.l	(a1)+,(a2)+
	sne.b	d1
	lsl.l	#4,d1
	cmpm.l	(a1)+,(a2)+
	sne.b	d1
	lsl.l	#4,d1
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#4,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#4,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lsl.l	#4,d0
	cmpm.l	(a1)+,(a2)+
	sne.b	d0
	lea.l	-28(a1),a1		;actual
	lea.l	-28(a2),a2		;expected
	addq.l	#2,(record_tested,a6)
	tst.l	d0
	beq	9f
	addq.l	#2,(record_failed,a6)
	tst.b	d0
	bne	8f			;wrong fpsr
	move.l	24(a2),d0		;fpsr
	and.l	#ZE+IN+NA,d0
	bne	8f			;±0,±Inf,NaN but the extended data does not match

;check accuracy (destination)
	fmove.l	#0,fpcr			;fpcr
	fmove.x	(a1),fp0		;actual destination
	fabs.x	fp0,fp0			;abs(actual)
	flog2.x	fp0,fp0			;log2(abs(actual))
	fmove.x	(a2),fp1		;expected destination
	fabs.x	fp1,fp1			;abs(expected)
	flog2.x	fp1,fp1			;log2(abs(expected))
	fcmp.x	fp1,fp0
	fblt	@f
	fmove.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))
@@:	fmove.x	(a1),fp1		;actual destination
	fsub.x	(a2),fp1		;actual-expected
	fabs.x	fp1,fp1			;abs(actual-expected)
	flog2.x	fp1,fp1			;log2(abs(actual-expected))
	fsub.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))-log2(abs(actual-expected)), number of correct bits
  .irp n,63,62,61,60,59,58,57,56,55,54
	fcmp.s	#&n.0,fp0
	fblt	1f
	addq.l	#1,(record_level&n,a6)
	bra	2f
1:
  .endm
2:

;check accuracy (co-destination)
	fmove.l	#0,fpcr			;fpcr
	fmove.x	12(a1),fp0		;actual co-destination
	fabs.x	fp0,fp0			;abs(actual)
	flog2.x	fp0,fp0			;log2(abs(actual))
	fmove.x	12(a2),fp1		;expected co-destination
	fabs.x	fp1,fp1			;abs(expected)
	flog2.x	fp1,fp1			;log2(abs(expected))
	fcmp.x	fp1,fp0
	fblt	@f
	fmove.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))
@@:	fmove.x	12(a1),fp1		;actual co-destination
	fsub.x	12(a2),fp1		;actual-expected
	fabs.x	fp1,fp1			;abs(actual-expected)
	flog2.x	fp1,fp1			;log2(abs(actual-expected))
	fsub.x	fp1,fp0			;min(log2(abs(actual)),log2(abs(expected)))-log2(abs(actual-expected)), number of correct bits
  .irp n,63,62,61,60,59,58,57,56,55,54
	fcmp.s	#&n.0,fp0
	fblt	1f
	addq.l	#1,(record_level&n,a6)
	bra	2f
1:
  .endm
2:

8:	moveq.l	#-1,d0
9:	movem.l	(sp)+,a1-a2
	rts

;----------------------------------------------------------------
	.text
	.align	4,$2048
;<a6.l:record
record_print:
	movem.l	d0-d2/a0-a1,-(sp)

	lea.l	buffer,a0

	move.l	(record_tested,a6),d0
	leastr	'tested:',a1
	bsr	strcpy
	bsr	utos

	move.l	(record_tested,a6),d0
	sub.l	(record_failed,a6),d0
	beq	@f
	leastr	', passed:',a1
	bsr	strcpy
	bsr	utos			;tested-failed
	move.l	(record_tested,a6),d1
	bsr	put_ratio		;(tested-failed)/tested
@@:

	move.l	(record_failed,a6),d0
	beq	@f
	leastr	', failed:',a1
	bsr	strcpy
	bsr	utos
	move.l	(record_tested,a6),d1
	bsr	put_ratio		;failed/tested
@@:

	move.l	(record_failed,a6),d2
	beq	9f

	leastr	' [ ',a1
	bsr	strcpy
  .irp n,63,62,61,60,59,58,57,56,55,54
	move.l	(record_level&n,a6),d0
	beq	@f
	sub.l	d0,d2
	leastr	'&n:',a1
	bsr	strcpy
	bsr	utos
	move.l	(record_tested,a6),d1
	bsr	put_ratio
	move.b	#',',(a0)+
	move.b	#' ',(a0)+
@@:
  .endm
	move.l	d2,d0
	beq	@f
	leastr	'wrong:',a1
	bsr	strcpy
	bsr	utos
	move.l	(record_tested,a6),d1
	bsr	put_ratio
	move.b	#',',(a0)+
	move.b	#' ',(a0)+
@@:
	subq.l	#2,a0
	move.b	#' ',(a0)+
	move.b	#']',(a0)+

9:	move.b	#13,(a0)+
	move.b	#10,(a0)+
	sf.b	(a0)

	lea.l	buffer,a0
	bsr	log_print

	move.l	(record_tested,a6),d0
	add.l	d0,total_tested
	move.l	(record_failed,a6),d0
	add.l	d0,total_failed

	movem.l	(sp)+,d0-d2/a0-a1
	rts

;----------------------------------------------------------------
;<d0.l:分子
;<d1.l:分母。0<d1かつ0<=d0<=d1であること
;<a0.l:buffer
put_ratio:
	movem.l	d0-d1,-(sp)
;d1<=65535にする
	bra	2f
1:	lsr.l	#1,d0
	lsr.l	#1,d1
2:	cmp.l	#65535,d1
	bhi	1b
	move.b	#'(',(a0)+
	mulu.w	#10000,d0	;d0を10000倍してから
	divu.w	d1,d0		;d1で割る。0～9999になる。これを0%～100%と表示する
	and.l	#$0000FFFF,d0
	divu.w	#100,d0		;100で割った余り(小数点以下2桁)と商(整数部分)に分ける
	move.l	d0,d1
	clr.w	d1
	swap.w	d1
	divu.w	#10,d1		;100で割った余り(小数点以下2桁)をさらに10で割った余り(小数点以下2桁目)と商(小数点以下1桁目)に分ける
	cmp.l	#5<<16,d1	;小数点以下2桁目を四捨五入する
	blo	@f
	add.w	#1,d1		;小数点以下1桁目に繰り上がる
	cmp.w	#10,d1
	blo	@f
	clr.w	d1
	addq.w	#1,d0		;整数部分に繰り上がる
@@:	and.l	#$0000FFFF,d0	;整数部分
	bsr	utos
	move.b	#'.',(a0)+
	moveq.l	#'0',d0
	add.w	d1,d0		;小数点以下1桁目
	move.b	d0,(a0)+
	move.b	#'%',(a0)+
	move.b	#')',(a0)+
	sf.b	(a0)
	movem.l	(sp)+,d0-d1
	rts



;----------------------------------------------------------------
;test_1to1
;<d6.l:number of expected results. 4 or 12
;<a4.l:data
;		.align	4
;		.dc.l	H,M,L		;input source
;		dclstr	'remark'
;		.dc.l	function	;(optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RN)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RZ)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RM)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RP)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RN) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RZ) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RM) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RP) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RN) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RZ) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RM) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RP) (optional)
;<a5.l:function. 0=function specified indivisually
;	(a3).x		input source
;	fp7		output destination
	.text
	.align	4,$2048
test_1to1:
	movem.l	d0-d7/a0-a6,-(sp)
	lea.l	(-record_size,sp),sp
	movea.l	sp,a6			;record
	bsr	record_clear

	move.l	a5,d5			;function
	lsl.l	#4,d6			;number of expected results<<4. upper limit of fpcr

	moveq.l	#0,d3			;number
31:
	sf.b	d4			;number was printed
	lea.l	buffer,a0
	movea.l	a4,a3			;input
	lea.l	16(a4),a4		;function or expected
	tst.l	d5
	bne	@f
	movea.l	(a4)+,a5		;a5=function,a4=expected
@@:

	moveq.l	#$00000000,d7		;fpcr
71:

	fmove.l	#0,fpcr
	fmove.x	(a3),fp7		;default destination (input source is needed for FTST)
	fmove.l	d7,fpcr			;input fpcr
	fmove.l	#0,fpsr			;input fpsr
	jsr	(a5)			;function. Fop.x (a3),fp7
	fmove.l	fpsr,-(sp)		;actual fpsr
	fmove.x	fp7,-(sp)		;actual destination
	movea.l	sp,a1			;actual
	movea.l	a4,a2			;expected
	bsr	record_check_1
	beq	77f

;number
	tst.b	d4			;number was printed
	bne	1f
	st.b	d4
	move.b	#'#',(a0)+
	move.l	d3,d0			;number
	bsr	utos
	move.b	#' ',(a0)+
;remark
	movea.l	12(a3),a1		;remark
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+
1:

;input
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a3,a1			;input source
	bsr	put_extended
	move.b	#9,(a0)+
	move.b	#';',(a0)+
;rounding precision
	move.l	d7,d0
	lsr.l	#6,d0
	and.l	#3,d0
	movea.l	(rounding_prec,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#' ',(a0)+
;rounding mode
	move.l	d7,d0
	lsr.l	#4,d0
	and.l	#3,d0
	movea.l	(rounding_mode,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+

;expected result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a4,a1			;expected destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	12(a4),d0		;expected fpsr
	bsr	put_fpsr
	leastr	'\t;expected result\r\n',a1
	bsr	strcpy

;actual result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	sp,a1			;actual destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	12(sp),d0		;actual fpsr
	bsr	put_fpsr
	leastr	'\t;actual result\r\n',a1
	bsr	strcpy

77:	lea.l	16(sp),sp		;actual
	lea.l	16(a4),a4		;expected

	add.l	#$00000010,d7		;(prec|mode)++
	cmp.l	d6,d7			;upper limit of fpcr
	blo	71b

	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print

	addq.l	#1,d3			;number
	cmpi.l	#$FFFFFFFF,(a4)		;input
	bne	31b

	bsr	record_print
	lea.l	(record_size,sp),sp
	movem.l	(sp)+,d0-d7/a0-a6
	rts

;----------------------------------------------------------------
;test_2to1
;<d6.l:number of expected results. 4 or 12
;<a4.l:data
;		.align	4
;		.dc.l	H,M,L,H,M,L	;input destination, source
;		dclstr	'remark'
;		.dc.l	function	;(optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RN)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RZ)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RM)
;		.dc.l	H,M,L,S		;expected destination, fpsr (extended RP)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RN) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RZ) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RM) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (single RP) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RN) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RZ) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RM) (optional)
;		.dc.l	H,M,L,S		;expected destination, fpsr (double RP) (optional)
;<a5.l:function. 0=function specified indivisually
;	(a3).x		input destination
;	12(a3).x	input source
;	fp7		output destination
	.text
	.align	4,$2048
test_2to1:
	movem.l	d0-d7/a0-a6,-(sp)
	lea.l	(-record_size,sp),sp
	movea.l	sp,a6			;record
	bsr	record_clear

	move.l	a5,d5			;function
	lsl.l	#4,d6			;number of expected results<<4. upper limit of fpcr

	moveq.l	#0,d3			;number
31:
	sf.b	d4			;number was printed
	lea.l	buffer,a0
	movea.l	a4,a3			;input
	lea.l	28(a4),a4		;function or expected
	tst.l	d5
	bne	@f
	movea.l	(a4)+,a5		;a5=function,a4=expected
@@:

	moveq.l	#$00000000,d7		;fpcr
71:

	fmove.l	#0,fpcr
	fmove.x	(a3),fp7		;input destination
	fmove.l	d7,fpcr			;input fpcr
	fmove.l	#0,fpsr			;input fpsr
	jsr	(a5)			;function. Fop.x 12(a3),fp7
	fmove.l	fpsr,-(sp)		;actual fpsr
	fmove.x	fp7,-(sp)		;actual destination
	movea.l	sp,a1			;actual
	movea.l	a4,a2			;expected
	bsr	record_check_1
	beq	77f

;number
	tst.b	d4			;number was printed
	bne	1f
	st.b	d4
	move.b	#'#',(a0)+
	move.l	d3,d0			;number
	bsr	utos
	move.b	#' ',(a0)+
;remark
	movea.l	24(a3),a1		;remark
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+
1:

;input
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a3,a1			;input destination
	bsr	put_extended
	move.b	#',',(a0)+
	lea.l	12(a3),a1		;input source
	bsr	put_extended
	move.b	#9,(a0)+
	move.b	#';',(a0)+
;rounding precision
	move.l	d7,d0
	lsr.l	#6,d0
	and.l	#3,d0
	movea.l	(rounding_prec,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#' ',(a0)+
;rounding mode
	move.l	d7,d0
	lsr.l	#4,d0
	and.l	#3,d0
	movea.l	(rounding_mode,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+

;expected result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a4,a1			;expected destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	12(a4),d0		;expected fpsr
	bsr	put_fpsr
	leastr	'\t;expected result\r\n',a1
	bsr	strcpy

;actual result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	sp,a1			;actual destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	12(sp),d0		;actual fpsr
	bsr	put_fpsr
	leastr	'\t;actual result\r\n',a1
	bsr	strcpy

77:	lea.l	16(sp),sp		;actual
	lea.l	16(a4),a4		;expected

	add.l	#$00000010,d7		;(prec|mode)++
	cmp.l	d6,d7			;upper limit of fpcr
	blo	71b

	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print

	addq.l	#1,d3			;number
	cmpi.l	#$FFFFFFFF,(a4)		;input
	bne	31b

	bsr	record_print
	lea.l	(record_size,sp),sp
	movem.l	(sp)+,d0-d7/a0-a6
	rts

;----------------------------------------------------------------
;test_1to2
;<d6.l:number of expected results. 4 or 12
;<a4.l:data
;		.align	4
;		.dc.l	H,M,L		;input source
;		dclstr	'remark'
;		.dc.l	function	;(optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (extended RN)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (extended RZ)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (extended RM)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (extended RP)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (single RN) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (single RZ) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (single RM) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (single RP) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (double RN) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (double RZ) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (double RM) (optional)
;		.dc.l	H,M,L,H,M,L,S	;expected destination, co-destination, fpsr (double RP) (optional)
;<a5.l:function. 0=function specified indivisually
;	(a3).x		input source
;	fp6		output co-destination
;	fp7		output destination
	.text
	.align	4,$2048
test_1to2:
	movem.l	d0-d7/a0-a6,-(sp)
	lea.l	(-record_size,sp),sp
	movea.l	sp,a6			;record
	bsr	record_clear

	move.l	a5,d5			;function
	lsl.l	#4,d6			;number of expected results<<4. upper limit of fpcr

	moveq.l	#0,d3			;number
31:
	sf.b	d4			;number was printed
	lea.l	buffer,a0
	movea.l	a4,a3			;input
	lea.l	16(a4),a4		;function or expected
	tst.l	d5
	bne	@f
	movea.l	(a4)+,a5		;a5=function,a4=expected
@@:

	moveq.l	#$00000000,d7		;fpcr
71:

	fmove.l	#0,fpcr
	fmove.s	#$7FFFFFFF,fp7		;default destination
	fmove.s	#$7FFFFFFF,fp6		;default co-destination
	fmove.l	d7,fpcr			;input fpcr
	fmove.l	#0,fpsr			;input fpsr
	jsr	(a5)			;function. Fop.x (a3),fp6:fp7
	fmove.l	fpsr,-(sp)		;actual fpsr
	fmove.x	fp6,-(sp)		;actual co-destination
	fmove.x	fp7,-(sp)		;actual destination
	movea.l	sp,a1			;actual
	movea.l	a4,a2			;expected
	bsr	record_check_2
	beq	77f

;number
	tst.b	d4			;number was printed
	bne	1f
	st.b	d4
	move.b	#'#',(a0)+
	move.l	d3,d0			;number
	bsr	utos
	move.b	#' ',(a0)+
;remark
	movea.l	12(a3),a1		;remark
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+
1:

;input
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a3,a1			;input source
	bsr	put_extended
	move.b	#9,(a0)+
	move.b	#';',(a0)+
;rounding precision
	move.l	d7,d0
	lsr.l	#6,d0
	and.l	#3,d0
	movea.l	(rounding_prec,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#' ',(a0)+
;rounding mode
	move.l	d7,d0
	lsr.l	#4,d0
	and.l	#3,d0
	movea.l	(rounding_mode,za0,d0.l*4),a1
	bsr	strcpy
	move.b	#13,(a0)+
	move.b	#10,(a0)+

;expected result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	a4,a1			;expected destination
	bsr	put_extended
	move.b	#',',(a0)+
	lea.l	12(a4),a1		;expected co-destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	24(a4),d0		;expected fpsr
	bsr	put_fpsr
	leastr	'\t;expected result\r\n',a1
	bsr	strcpy

;actual result
	leastr	'\t.dc.l\t',a1
	bsr	strcpy
	movea.l	sp,a1			;actual destination
	bsr	put_extended
	move.b	#',',(a0)+
	lea.l	12(sp),a1		;actual co-destination
	bsr	put_extended
	move.b	#',',(a0)+
	move.l	24(sp),d0		;actual fpsr
	bsr	put_fpsr
	leastr	'\t;actual result\r\n',a1
	bsr	strcpy

77:	lea.l	28(sp),sp		;actual
	lea.l	28(a4),a4		;expected

	add.l	#$00000010,d7		;(prec|mode)++
	cmp.l	d6,d7			;upper limit of fpcr
	blo	71b

	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print

	addq.l	#1,d3			;number
	cmpi.l	#$FFFFFFFF,(a4)		;input
	bne	31b

	bsr	record_print
	lea.l	(record_size,sp),sp
	movem.l	(sp)+,d0-d7/a0-a6
	rts



	.align	4
rounding_prec:
	dclstr	'extended'
	dclstr	'single'
	dclstr	'double'
	dclstr	'undefined'

	.align	4
rounding_mode:
	dclstr	'RN'			;round to nearest
	dclstr	'RZ'			;round toward zero
	dclstr	'RM'			;round toward minus infinity
	dclstr	'RP'			;round toward plus infinity



;----------------------------------------------------------------
	.bss
	.align	4
buffer:
	.ds.b	1024*64



;----------------------------------------------------------------
	.xref	test_fmovecr_data
	.text
	.align	4,$2048
test_fmovecr:
	leastr	'\r\nTesting FMOVECR\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fmovecr_data,a4	;data
	suba.l	a5,a5			;function specified indivisually
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fmove_data
	.xref	test_fmove_func
	.text
	.align	4,$2048
test_fmove:
	leastr	'\r\nTesting FMOVE\r\n',a0
	bsr	log_print
	moveq.l	#12,d6			;extended,single,double
	lea.l	test_fmove_data,a4	;data
	lea.l	test_fmove_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fint_data
	.xref	test_fint_func
	.text
	.align	4,$2048
test_fint:
	leastr	'\r\nTesting FINT\r\n',a0
	bsr	log_print
	moveq.l	#12,d6			;extended,single,double
	lea.l	test_fint_data,a4	;data
	lea.l	test_fint_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsinh_data
	.xref	test_fsinh_func
	.text
	.align	4,$2048
test_fsinh:
	leastr	'\r\nTesting FSINH\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsinh_data,a4	;data
	lea.l	test_fsinh_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fintrz_data
	.xref	test_fintrz_func
	.text
	.align	4,$2048
test_fintrz:
	leastr	'\r\nTesting FINTRZ\r\n',a0
	bsr	log_print
	moveq.l	#12,d6			;extended,single,double
	lea.l	test_fintrz_data,a4	;data
	lea.l	test_fintrz_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsqrt_data
	.xref	test_fsqrt_func
	.text
	.align	4,$2048
test_fsqrt:
	leastr	'\r\nTesting FSQRT\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsqrt_data,a4	;data
	lea.l	test_fsqrt_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_flognp1_data
	.xref	test_flognp1_func
	.text
	.align	4,$2048
test_flognp1:
	leastr	'\r\nTesting FLOGNP1\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_flognp1_data,a4	;data
	lea.l	test_flognp1_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fetoxm1_data
	.xref	test_fetoxm1_func
	.text
	.align	4,$2048
test_fetoxm1:
	leastr	'\r\nTesting FETOXM1\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fetoxm1_data,a4	;data
	lea.l	test_fetoxm1_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_ftanh_data
	.xref	test_ftanh_func
	.text
	.align	4,$2048
test_ftanh:
	leastr	'\r\nTesting FTANH\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_ftanh_data,a4	;data
	lea.l	test_ftanh_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fatan_data
	.xref	test_fatan_func
	.text
	.align	4,$2048
test_fatan:
	leastr	'\r\nTesting FATAN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fatan_data,a4	;data
	lea.l	test_fatan_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fasin_data
	.xref	test_fasin_func
	.text
	.align	4,$2048
test_fasin:
	leastr	'\r\nTesting FASIN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fasin_data,a4	;data
	lea.l	test_fasin_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fatanh_data
	.xref	test_fatanh_func
	.text
	.align	4,$2048
test_fatanh:
	leastr	'\r\nTesting FATANH\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fatanh_data,a4	;data
	lea.l	test_fatanh_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsin_data
	.xref	test_fsin_func
	.text
	.align	4,$2048
test_fsin:
	leastr	'\r\nTesting FSIN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsin_data,a4	;data
	lea.l	test_fsin_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_ftan_data
	.xref	test_ftan_func
	.text
	.align	4,$2048
test_ftan:
	leastr	'\r\nTesting FTAN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_ftan_data,a4	;data
	lea.l	test_ftan_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fetox_data
	.xref	test_fetox_func
	.text
	.align	4,$2048
test_fetox:
	leastr	'\r\nTesting FETOX\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fetox_data,a4	;data
	lea.l	test_fetox_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_ftwotox_data
	.xref	test_ftwotox_func
	.text
	.align	4,$2048
test_ftwotox:
	leastr	'\r\nTesting FTWOTOX\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_ftwotox_data,a4	;data
	lea.l	test_ftwotox_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_ftentox_data
	.xref	test_ftentox_func
	.text
	.align	4,$2048
test_ftentox:
	leastr	'\r\nTesting FTENTOX\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_ftentox_data,a4	;data
	lea.l	test_ftentox_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_flogn_data
	.xref	test_flogn_func
	.text
	.align	4,$2048
test_flogn:
	leastr	'\r\nTesting FLOGN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_flogn_data,a4	;data
	lea.l	test_flogn_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_flog10_data
	.xref	test_flog10_func
	.text
	.align	4,$2048
test_flog10:
	leastr	'\r\nTesting FLOG10\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_flog10_data,a4	;data
	lea.l	test_flog10_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_flog2_data
	.xref	test_flog2_func
	.text
	.align	4,$2048
test_flog2:
	leastr	'\r\nTesting FLOG2\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_flog2_data,a4	;data
	lea.l	test_flog2_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fabs_data
	.xref	test_fabs_func
	.text
	.align	4,$2048
test_fabs:
	leastr	'\r\nTesting FABS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fabs_data,a4	;data
	lea.l	test_fabs_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fcosh_data
	.xref	test_fcosh_func
	.text
	.align	4,$2048
test_fcosh:
	leastr	'\r\nTesting FCOSH\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fcosh_data,a4	;data
	lea.l	test_fcosh_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fneg_data
	.xref	test_fneg_func
	.text
	.align	4,$2048
test_fneg:
	leastr	'\r\nTesting FNEG\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fneg_data,a4	;data
	lea.l	test_fneg_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_facos_data
	.xref	test_facos_func
	.text
	.align	4,$2048
test_facos:
	leastr	'\r\nTesting FACOS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_facos_data,a4	;data
	lea.l	test_facos_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fcos_data
	.xref	test_fcos_func
	.text
	.align	4,$2048
test_fcos:
	leastr	'\r\nTesting FCOS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fcos_data,a4	;data
	lea.l	test_fcos_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fgetexp_data
	.xref	test_fgetexp_func
	.text
	.align	4,$2048
test_fgetexp:
	leastr	'\r\nTesting FGETEXP\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fgetexp_data,a4	;data
	lea.l	test_fgetexp_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fgetman_data
	.xref	test_fgetman_func
	.text
	.align	4,$2048
test_fgetman:
	leastr	'\r\nTesting FGETMAN\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fgetman_data,a4	;data
	lea.l	test_fgetman_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fdiv_data
	.xref	test_fdiv_func
	.text
	.align	4,$2048
test_fdiv:
	leastr	'\r\nTesting FDIV\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdiv_data,a4	;data
	lea.l	test_fdiv_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fmod_data
	.xref	test_fmod_func
	.text
	.align	4,$2048
test_fmod:
	leastr	'\r\nTesting FMOD\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fmod_data,a4	;data
	lea.l	test_fmod_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fadd_data
	.xref	test_fadd_func
	.text
	.align	4,$2048
test_fadd:
	leastr	'\r\nTesting FADD\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fadd_data,a4	;data
	lea.l	test_fadd_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fmul_data
	.xref	test_fmul_func
	.text
	.align	4,$2048
test_fmul:
	leastr	'\r\nTesting FMUL\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fmul_data,a4	;data
	lea.l	test_fmul_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsgldiv882_data
	.xref	test_fsgldiv882_func
	.text
	.align	4,$2048
test_fsgldiv882:
	leastr	'\r\nTesting FSGLDIV (MC68882)\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsgldiv882_data,a4	;data
	lea.l	test_fsgldiv882_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsgldiv060_data
	.xref	test_fsgldiv060_func
	.text
	.align	4,$2048
test_fsgldiv060:
	leastr	'\r\nTesting FSGLDIV (MC68060)\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsgldiv060_data,a4	;data
	lea.l	test_fsgldiv060_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_frem_data
	.xref	test_frem_func
	.text
	.align	4,$2048
test_frem:
	leastr	'\r\nTesting FREM\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_frem_data,a4	;data
	lea.l	test_frem_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fscale_data
	.xref	test_fscale_func
	.text
	.align	4,$2048
test_fscale:
	leastr	'\r\nTesting FSCALE\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fscale_data,a4	;data
	lea.l	test_fscale_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsglmul882_data
	.xref	test_fsglmul882_func
	.text
	.align	4,$2048
test_fsglmul882:
	leastr	'\r\nTesting FSGLMUL (MC68882)\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsglmul882_data,a4	;data
	lea.l	test_fsglmul882_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsglmul060_data
	.xref	test_fsglmul060_func
	.text
	.align	4,$2048
test_fsglmul060:
	leastr	'\r\nTesting FSGLMUL (MC68060)\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsglmul060_data,a4	;data
	lea.l	test_fsglmul060_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsub_data
	.xref	test_fsub_func
	.text
	.align	4,$2048
test_fsub:
	leastr	'\r\nTesting FSUB\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsub_data,a4	;data
	lea.l	test_fsub_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsincos_data
	.xref	test_fsincos_func
	.text
	.align	4,$2048
test_fsincos:
	leastr	'\r\nTesting FSINCOS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsincos_data,a4	;data
	lea.l	test_fsincos_func,a5	;function
	bra	test_1to2

;----------------------------------------------------------------
	.xref	test_fcmp_data
	.xref	test_fcmp_func
	.text
	.align	4,$2048
test_fcmp:
	leastr	'\r\nTesting FCMP\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fcmp_data,a4	;data
	lea.l	test_fcmp_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_ftst_data
	.xref	test_ftst_func
	.text
	.align	4,$2048
test_ftst:
	leastr	'\r\nTesting FTST\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_ftst_data,a4	;data
	lea.l	test_ftst_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsmove_data
	.xref	test_fsmove_func
	.text
	.align	4,$2048
test_fsmove:
	leastr	'\r\nTesting FSMOVE\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsmove_data,a4	;data
	lea.l	test_fsmove_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fssqrt_data
	.xref	test_fssqrt_func
	.text
	.align	4,$2048
test_fssqrt:
	leastr	'\r\nTesting FSSQRT\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fssqrt_data,a4	;data
	lea.l	test_fssqrt_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fdmove_data
	.xref	test_fdmove_func
	.text
	.align	4,$2048
test_fdmove:
	leastr	'\r\nTesting FDMOVE\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdmove_data,a4	;data
	lea.l	test_fdmove_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fdsqrt_data
	.xref	test_fdsqrt_func
	.text
	.align	4,$2048
test_fdsqrt:
	leastr	'\r\nTesting FDSQRT\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdsqrt_data,a4	;data
	lea.l	test_fdsqrt_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsabs_data
	.xref	test_fsabs_func
	.text
	.align	4,$2048
test_fsabs:
	leastr	'\r\nTesting FSABS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsabs_data,a4	;data
	lea.l	test_fsabs_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsneg_data
	.xref	test_fsneg_func
	.text
	.align	4,$2048
test_fsneg:
	leastr	'\r\nTesting FSNEG\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsneg_data,a4	;data
	lea.l	test_fsneg_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fdabs_data
	.xref	test_fdabs_func
	.text
	.align	4,$2048
test_fdabs:
	leastr	'\r\nTesting FDABS\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdabs_data,a4	;data
	lea.l	test_fdabs_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fdneg_data
	.xref	test_fdneg_func
	.text
	.align	4,$2048
test_fdneg:
	leastr	'\r\nTesting FDNEG\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdneg_data,a4	;data
	lea.l	test_fdneg_func,a5	;function
	bra	test_1to1

;----------------------------------------------------------------
	.xref	test_fsdiv_data
	.xref	test_fsdiv_func
	.text
	.align	4,$2048
test_fsdiv:
	leastr	'\r\nTesting FSDIV\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsdiv_data,a4	;data
	lea.l	test_fsdiv_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsadd_data
	.xref	test_fsadd_func
	.text
	.align	4,$2048
test_fsadd:
	leastr	'\r\nTesting FSADD\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsadd_data,a4	;data
	lea.l	test_fsadd_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fsmul_data
	.xref	test_fsmul_func
	.text
	.align	4,$2048
test_fsmul:
	leastr	'\r\nTesting FSMUL\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fsmul_data,a4	;data
	lea.l	test_fsmul_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fddiv_data
	.xref	test_fddiv_func
	.text
	.align	4,$2048
test_fddiv:
	leastr	'\r\nTesting FDDIV\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fddiv_data,a4	;data
	lea.l	test_fddiv_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fdadd_data
	.xref	test_fdadd_func
	.text
	.align	4,$2048
test_fdadd:
	leastr	'\r\nTesting FDADD\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdadd_data,a4	;data
	lea.l	test_fdadd_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fdmul_data
	.xref	test_fdmul_func
	.text
	.align	4,$2048
test_fdmul:
	leastr	'\r\nTesting FDMUL\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdmul_data,a4	;data
	lea.l	test_fdmul_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fssub_data
	.xref	test_fssub_func
	.text
	.align	4,$2048
test_fssub:
	leastr	'\r\nTesting FSSUB\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fssub_data,a4	;data
	lea.l	test_fssub_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
	.xref	test_fdsub_data
	.xref	test_fdsub_func
	.text
	.align	4,$2048
test_fdsub:
	leastr	'\r\nTesting FDSUB\r\n',a0
	bsr	log_print
	moveq.l	#4,d6			;extended
	lea.l	test_fdsub_data,a4	;data
	lea.l	test_fdsub_func,a5	;function
	bra	test_2to1

;----------------------------------------------------------------
test_fbcc882:
	leastr	'\r\nTesting FBcc\r\n',a0
	bsr	log_print
	lea.l	-2*32(sp),sp
	lea.l	buffer,a0
	lea.l	test_fbcc882_data,a2	;expected
	movea.l	sp,a3			;actual
	moveq.l	#0,d4			;tested
	moveq.l	#0,d5			;failed
	fmove.l	#0,fpcr
  .irp cc,f,eq,ogt,oge,olt,ole,ogl,or,un,ueq,ugt,uge,ult,ule,ne,t,sf,seq,gt,ge,lt,le,gl,gle,ngle,ngl,nle,nlt,nge,ngt,sne,st
	move.w	(a2)+,d2		;expected
	clr.w	d3			;actual
	moveq.l	#0<<24,d7		;fpsr
7:	add.w	d2,d2
	clr.w	d0
	addx.w	d0,d0			;expected
	fmove.l	d7,fpsr
	fb&cc	1f
	moveq.l	#0,d1			;actual
	bra	2f
1:	moveq.l	#1,d1			;actual
2:	add.w	d3,d3
	or.w	d1,d3			;actual
	addq.w	#1,d4			;tested
	eor.w	d1,d0			;failed
	add.w	d0,d5			;failed
	add.l	#1<<24,d7		;fpsr
	cmp.l	#15<<24,d7
	bls	7b
	move.w	d3,(a3)+		;actual
  .endm
	tst.l	d5			;failed
	beq	7f
;expected
	lea.l	test_fbcc882_data,a2	;expected
	moveq.l	#2-1,d2
2:	leastr	'\t.dc.w',a1
	bsr	strcpy
	moveq.l	#9,d0
	moveq.l	#16-1,d1
1:	move.b	d0,(a0)+
	move.b	#'$',(a0)+
	move.w	(a2)+,d0
	bsr	h4tos
	moveq.l	#',',d0
	dbra	d1,1b
	leastr	'\t;expected result\r\n',a1
	bsr	strcpy
	dbra	d2,2b
;actual
	movea.l	sp,a2			;actual
	moveq.l	#2-1,d2
2:	leastr	'\t.dc.w',a1
	bsr	strcpy
	moveq.l	#9,d0
	moveq.l	#16-1,d1
1:	move.b	d0,(a0)+
	move.b	#'$',(a0)+
	move.w	(a2)+,d0
	bsr	h4tos
	moveq.l	#',',d0
	dbra	d1,1b
	leastr	'\t;actual result\r\n',a1
	bsr	strcpy
	dbra	d2,2b
7:
;tested
	leastr	'tested:',a1
	bsr	strcpy
	move.l	d4,d0			;tested
	bsr	utos
;passed
	move.l	d4,d0			;tested
	sub.l	d5,d0			;tested-failed=passed
	beq	@f
	leastr	', passed:',a1
	bsr	strcpy
	bsr	utos
	move.l	d4,d1
	bsr	put_ratio
@@:
;failed
	move.l	d5,d0			;failed
	beq	@f
	leastr	', failed:',a1
	bsr	strcpy
	bsr	utos
	move.l	d4,d1
	bsr	put_ratio
@@:
	move.b	#13,(a0)+
	move.b	#10,(a0)+
	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print
	add.l	d4,total_tested
	add.l	d5,total_failed
	lea.l	2*32(sp),sp
	rts

test_fbcc882_data:
	.dc.w	$0000,$0F0F,$A000,$AF0F,$00A0,$0FAF,$A0A0,$AFAF,$5555,$5F5F,$F555,$FF5F,$55F5,$5FFF,$F5F5,$FFFF
	.dc.w	$0000,$0F0F,$A000,$AF0F,$00A0,$0FAF,$A0A0,$AFAF,$5555,$5F5F,$F555,$FF5F,$55F5,$5FFF,$F5F5,$FFFF

;----------------------------------------------------------------
test_fbcc060:
	leastr	'\r\nTesting FBcc\r\n',a0
	bsr	log_print
	lea.l	-2*32(sp),sp
	lea.l	buffer,a0
	lea.l	test_fbcc060_data,a2	;expected
	movea.l	sp,a3			;actual
	moveq.l	#0,d4			;tested
	moveq.l	#0,d5			;failed
	fmove.l	#0,fpcr
  .irp cc,f,eq,ogt,oge,olt,ole,ogl,or,un,ueq,ugt,uge,ult,ule,ne,t,sf,seq,gt,ge,lt,le,gl,gle,ngle,ngl,nle,nlt,nge,ngt,sne,st
	move.w	(a2)+,d2		;expected
	clr.w	d3			;actual
	moveq.l	#0<<24,d7		;fpsr
7:	add.w	d2,d2
	clr.w	d0
	addx.w	d0,d0			;expected
	fmove.l	d7,fpsr
	fb&cc	1f
	moveq.l	#0,d1			;actual
	bra	2f
1:	moveq.l	#1,d1			;actual
2:	add.w	d3,d3
	or.w	d1,d3			;actual
	addq.w	#1,d4			;tested
	eor.w	d1,d0			;failed
	add.w	d0,d5			;failed
	add.l	#1<<24,d7		;fpsr
	cmp.l	#15<<24,d7
	bls	7b
	move.w	d3,(a3)+		;actual
  .endm
	tst.l	d5			;failed
	beq	7f
;expected
	lea.l	test_fbcc882_data,a2	;expected
	moveq.l	#2-1,d2
2:	leastr	'\t.dc.w',a1
	bsr	strcpy
	moveq.l	#9,d0
	moveq.l	#16-1,d1
1:	move.b	d0,(a0)+
	move.b	#'$',(a0)+
	move.w	(a2)+,d0
	bsr	h4tos
	moveq.l	#',',d0
	dbra	d1,1b
	leastr	'\t;expected result\r\n',a1
	bsr	strcpy
	dbra	d2,2b
;actual
	movea.l	sp,a2			;actual
	moveq.l	#2-1,d2
2:	leastr	'\t.dc.w',a1
	bsr	strcpy
	moveq.l	#9,d0
	moveq.l	#16-1,d1
1:	move.b	d0,(a0)+
	move.b	#'$',(a0)+
	move.w	(a2)+,d0
	bsr	h4tos
	moveq.l	#',',d0
	dbra	d1,1b
	leastr	'\t;actual result\r\n',a1
	bsr	strcpy
	dbra	d2,2b
7:
;tested
	leastr	'tested:',a1
	bsr	strcpy
	move.l	d4,d0			;tested
	bsr	utos
;passed
	move.l	d4,d0			;tested
	sub.l	d5,d0			;tested-failed=passed
	beq	@f
	leastr	', passed:',a1
	bsr	strcpy
	bsr	utos
	move.l	d4,d1
	bsr	put_ratio
@@:
;failed
	move.l	d5,d0			;failed
	beq	@f
	leastr	', failed:',a1
	bsr	strcpy
	bsr	utos
	move.l	d4,d1
	bsr	put_ratio
@@:
	move.b	#13,(a0)+
	move.b	#10,(a0)+
	sf.b	(a0)
	lea.l	buffer,a0
	bsr	log_print
	add.l	d4,total_tested
	add.l	d5,total_failed
	lea.l	2*32(sp),sp
	rts

test_fbcc060_data:
	.dc.w	$0000,$0F0F,$A000,$AF0F,$00A0,$0FAF,$A0A0,$AAAA,$5555,$5F5F,$F555,$FF5F,$55F5,$5FFF,$F0F0,$FFFF
	.dc.w	$0000,$0F0F,$A000,$AF0F,$00A0,$0FAF,$A0A0,$AAAA,$5555,$5F5F,$F555,$FF5F,$55F5,$5FFF,$F0F0,$FFFF

;----------------------------------------------------------------
;16ビット整数→16進数4桁の文字列
;<d0.w:16ビット整数
;<a0.l:16進数4桁の文字列を格納するバッファの先頭アドレス
;>a0.l:文字列の末尾の0の位置
	.text
	.align	4,$2048
h4tos::
	move.l	d1,-(sp)
  .rept 4
	rol.w	#4,d0
	moveq.l	#$0F,d1
	and.b	d0,d1
	move.b	(50f,pc,d1.l),(a0)+
  .endm
	sf.b	(a0)
	move.l	(sp)+,d1
	rts

50:	.dc.b	'0123456789ABCDEF'

;----------------------------------------------------------------
;32ビット整数→16進数8桁の文字列
;<d0.l:32ビット整数
;<a0.l:16進数8桁の文字列を格納するバッファの先頭アドレス
;>a0.l:文字列の末尾の0の位置
	.text
	.align	4,$2048
h8tos::
	move.l	d1,-(sp)
  .rept 8
	rol.l	#4,d0
	moveq.l	#$0F,d1
	and.b	d0,d1
	move.b	(50f,pc,d1.l),(a0)+
  .endm
	sf.b	(a0)
	move.l	(sp)+,d1
	rts

50:	.dc.b	'0123456789ABCDEF'

;----------------------------------------------------------------
;文字列表示
;<a0.l:文字列の先頭
;>a0.l:文字列の先頭
	.text
	.align	4,$2048
print::
	move.l	d0,-(sp)
	bsr	strlen
	move.l	d0,-(sp)
	move.l	a0,-(sp)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	(10,sp),sp
	move.l	(sp)+,d0
	rts

;----------------------------------------------------------------
;文字列の末尾の0までスキップする
;<a0.l:文字列の先頭
;>a0.l:文字列の末尾の0の位置
	.text
	.align	4,$2048
strchr0::
@@:	tst.b	(a0)+
	bne	@b
	subq.l	#1,a0
	rts

;----------------------------------------------------------------
;文字列をコピーする
;<a0.l:コピー先
;<a1.l:コピーする文字列
;>a0.l:コピー先の文字列の末尾の0の位置
;>a1.l:コピーした文字列の末尾の0の次の位置
	.text
	.align	4,$2048
strcpy::
@@:	move.b	(a1)+,(a0)+
	bne	@b
	subq.l	#1,a0
	rts

;----------------------------------------------------------------
;文字列の長さを数える
;<a0.l:文字列の先頭
;>d0.l:文字列の長さ
;>a0.l:文字列の先頭
;z-flag:eq=NULL
	.text
	.align	4,$2048
strlen::
	move.l	a0,d0
	bsr	strchr0
	exg.l	d0,a0
	sub.l	a0,d0
	rts

;----------------------------------------------------------------
;符号なし整数→10進数文字列
;<d0.l:符号なし整数
;<a0.l:文字列を格納するバッファの先頭
;>(a0).b[]:10進数文字列
	.text
	.align	4,$2048
utos::
	tst.l	d0
	beq	utos_zero
	movem.l	d0-d2/a1,-(sp)
	lea.l	utos_table,a1
@@:	cmp.l	(a1)+,d0
	bcs	@b
	move.l	(-4,a1),d1
utos_loop:
	moveq.l	#'0'-1,d2
@@:	addq.b	#1,d2
	sub.l	d1,d0
	bcc	@b
	move.b	d2,(a0)+
	add.l	d1,d0
	move.l	(a1)+,d1
	bne	utos_loop
	sf.b	(a0)
	movem.l	(sp)+,d0-d2/a1
	rts

utos_zero:
	move.b	#'0',(a0)+
	sf.b	(a0)
	rts

;----------------------------------------------------------------
;utos,iusing,zusingで使う10のべき乗のテーブル
	.text
	.align	4,$2048
utos_table::
iusing_table::
zusing_table::
	.dc.l	1000000000
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



	.end
