;========================================================================================
;  instructiontest.s
;  Copyright (C) 2003-2016 Makoto Kamada
;
;  This file is part of the XEiJ (X68000 Emulator in Java).
;  You can use, modify and redistribute the XEiJ if the conditions are met.
;  Read the XEiJ License for more details.
;  http://stdkmd.com/xeij/
;========================================================================================

	.include	doscall.mac
	.include	iocscall.mac


	.cpu	68000


CRC32_REG	reg	d7


;--------------------------------------------------------------------------------
;constants

IM_BYTE		reg	$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f
IM_WORD		reg	$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff
IM_LONG		reg	$00000000,$ffffffff,$00000001,$fffffffe,$00000080,$ffffff7f,$00000100,$fffffeff,$00008000,$ffff7fff,$00010000,$fffeffff,$00800000,$ff7fffff,$01000000,$feffffff,$80000000,$7fffffff
IM_QUICK	reg	1,2,3,4,5,6,7,8
IM_3BIT		reg	0,1,2,3,4,5,6,7
IM_5BIT		reg	0,1,7,8,9,15,16,17,23,24,25,31
IM_OFFSET	reg	0,1,7,8,9,15,16,17,23,24,25,31
IM_WIDTH	reg	1,7,8,9,15,16,17,23,24,25,31,32

MC68000		equ	1
MC68010		equ	2
MC68020		equ	4
MC68030		equ	8
MC68040		equ	16
MC68060		equ	64


;--------------------------------------------------------------------------------
;macros

leastr	.macro	s0,an
	.data
@str:
	.dc.b	s0,0
	.text
	lea.l	@str,an
	.endm

peastr	.macro	s0,s1,s2,s3,s4,s5,s6,s7,s8,s9
	.data
@str:
	.dc.b	s0,s1,s2,s3,s4,s5,s6,s7,s8,s9,0
	.text
	pea.l	@str
	.endm

print	.macro	s0,s1,s2,s3,s4,s5,s6,s7,s8,s9
	peastr	s0,s1,s2,s3,s4,s5,s6,s7,s8,s9
	jsr	print_by_write
	addq.l	#4,sp
	.endm

putchar	.macro	c0
	move.w	c0,-(sp)
	jsr	pubchar_by_write
	addq.l	#2,sp
	.endm

SPC1	reg	' '
SPC2	reg	'  '
SPC3	reg	'   '
SPC4	reg	'    '
SPC5	reg	'     '
SPC6	reg	'      '
SPC7	reg	'       '
SPC8	reg	'        '
SPC9	reg	'         '
SPC10	reg	'          '
SPC11	reg	'           '
SPC12	reg	'            '
SPC13	reg	'             '
SPC14	reg	'              '
SPC15	reg	'               '
SPC16	reg	'                '
SPC17	reg	'                 '
SPC18	reg	'                  '
SPC19	reg	'                   '
SPC20	reg	'                    '
SPC21	reg	'                     '
SPC22	reg	'                      '
SPC23	reg	'                       '
SPC24	reg	'                        '
SPC25	reg	'                         '
SPC26	reg	'                          '
SPC27	reg	'                           '
SPC28	reg	'                            '
SPC29	reg	'                             '
SPC30	reg	'                              '
SPC31	reg	'                               '
SPC32	reg	'                                '
SPC33	reg	'                                 '
SPC34	reg	'                                  '
SPC35	reg	'                                   '
SPC36	reg	'                                    '
SPC37	reg	'                                     '
SPC38	reg	'                                      '
SPC39	reg	'                                       '
SPC40	reg	'                                        '

fillto	.macro	v,c,s0
~fillto = c-.sizeof.(s0)
  .if ~fillto<=0
v	reg	s0
  .else
v	reg	s0,SPC%~fillto
  .endif
	.endm


;--------------------------------------------------------------------------------
;illegal instruction

try_illegal_instruction	.macro
	move.l	#798f,abort_illegal_instruction
	.endm

catch_illegal_instruction	.macro
	tst.l	d0			;clear carry flag
798:	jsr	print_illegal_instruction
	.endm


;--------------------------------------------------------------------------------
;no operand

xxx_no_operand	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.w	d6,ccr
	op
	move.w	sr,d0
	jsr	crc32_byte
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_im?

xxx_imb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.w	d6,ccr
	op	#im
	move.w	sr,d0
	jsr	crc32_byte
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.w	d6,ccr
	op	#im
	move.w	sr,d0
	jsr	crc32_byte
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_iml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00000000,$ffffffff,$00000001,$fffffffe,$00000080,$ffffff7f,$00000100,$fffffeff,$00008000,$ffff7fff,$00010000,$fffeffff,$00800000,$ff7fffff,$01000000,$feffffff,$80000000,$7fffffff	;IM_LONG
	move.w	d6,ccr
	op	#im
	move.w	sr,d0
	jsr	crc32_byte
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?

xxx_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	d2			;Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	d2			;Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	d2			;Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_ar?

xxx_arw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	movea.l	d1,a2			;Ar'
	move.w	d6,ccr
	op	a2			;Ar'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a2,d0			;Ar'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	movea.l	d1,a2			;Ar'
	move.w	d6,ccr
	op	a2			;Ar'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a2,d0			;Ar'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mm?

xxx_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	(a0)			;(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	(a0)			;(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	(a0)			;(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?_dr?

xxx_drb_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drb_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drb_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drw_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drw_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4			;Dr',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl_drq	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dh:Dl'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	quad_data,a2
	movem.l	(a2)+,d2/a3		;Dh:Dl
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dh'
	move.l	a3,d5			;Dl'
	move.w	d6,ccr
	op	d3,d4:d5		;Dr',Dh':Dl'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dh'
	jsr	crc32_long
	move.l	d5,d0			;Dl'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	movem.l	(a2)+,d2/a3		;Dh:Dl
	move.l	a3,d0
	or.l	d2,d0
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?_ar?

xxx_drw_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	d3,a4			;Dr',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	d3,a4			;Dr',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?_im?

xxx_drb_imb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	d2,#im			;Dr',#<data>
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_ar?_ar?

xxx_arl_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar,Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movea.l	d1,a3			;Ar'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	a3,a4			;Ar',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a3,d0			;Ar'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mm?_dr?

xxx_mmb_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmw_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmw_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmw_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml_drq	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dh:Dl'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	quad_data,a2
	movem.l	(a2)+,d2-d3		;Dh:Dl
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	move.l	d2,d4			;Dh'
	move.l	d3,d5			;Dl'
	move.w	d6,ccr
	op	(a0),d4:d5		;(Ar)',Dh':Dl'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dh'
	jsr	crc32_long
	move.l	d5,d0			;Dl'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	movem.l	(a2)+,d2-d3		;Dh:Dl
	move.l	d2,d0
	or.l	d3,d0
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmq_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	quad_data,a1
	movem.l	(a1)+,d1-d2		;(Ar)
101:
	lea.l	long_data,a3
	move.l	(a3)+,d3		;Dq
103:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movem.l	d1-d2,(a0)		;(Ar)'
	move.l	d3,d4			;Dq'
	move.w	d6,ccr
	op	(a0),d4			;(Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'h
	jsr	crc32_long
	move.l	4(a0),d0		;(Ar)'l
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;Dq
	bne	103b
	movem.l	(a1)+,d1-d2		;(Ar)
	move.l	d1,d0
	or.l	d2,d0
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmq_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	quad_data,a1
	movem.l	(a1)+,d1-d2		;(Ar)
101:
	lea.l	long_data,a3
	move.l	(a3)+,d3		;Aq
103:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movem.l	d1-d2,(a0)		;(Ar)'
	movea.l	d3,a4			;Aq'
	move.w	d6,ccr
	op	(a0),a4			;(Ar)',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'h
	jsr	crc32_long
	move.l	4(a0),d0		;(Ar)'l
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;Aq
	bne	103b
	movem.l	(a1)+,d1-d2		;(Ar)
	move.l	d1,d0
	or.l	d2,d0
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mm?_ar?

xxx_mmw_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	(a0),a4			;(Ar)',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(Ar)'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	(a0),a4			;(Ar)',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?_mm?

xxx_drb_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dq,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dq
101:
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;(Ar)
102:
	ror.l	#8,d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dq'
	move.l	d2,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d3,(a0)			;Dq',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dq'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Ar)
	bne	102b
	move.l	(a1)+,d1		;Dq
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drw_mmw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dq,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dq
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;(Ar)
102:
	swap.w	d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dq'
	move.l	d2,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d3,(a0)			;Dq',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dq'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Ar)
	bne	102b
	move.l	(a1)+,d1		;Dq
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dq,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dq
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;(Ar)
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dq'
	move.l	d2,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d3,(a0)			;Dq',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dq'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Ar)
	bne	102b
	move.l	(a1)+,d1		;Dq
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mm?_mm?

xxx_mmb_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)'
	lea.l	work_area+4,a4		;(Aq)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;(Aq)
102:
	ror.l	#8,d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)'
	move.l	d2,(a4)			;(Aq)'
	move.w	d6,ccr
	op	(a3),(a4)		;(Ar)',(Aq)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mmw_mmw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)'
	lea.l	work_area+4,a4		;(Aq)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	word_data,a2
	move.l	(a2)+,d2		;(Aq)
102:
	swap.w	d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)'
	move.l	d2,(a4)			;(Aq)'
	move.w	d6,ccr
	op	(a3),(a4)		;(Ar)',(Aq)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mml_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)'
	lea.l	work_area+4,a4		;(Aq)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;(Aq)
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)'
	move.l	d2,(a4)			;(Aq)'
	move.w	d6,ccr
	op	(a3),(a4)		;(Ar)',(Aq)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)
	bne	102b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mp?_mp?

xxx_mpb_mpb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)+,(Aq)+'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)+'
	lea.l	work_area+4,a4		;(Aq)+'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)+
101:
	ror.l	#8,d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;(Aq)+
102:
	ror.l	#8,d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)+'
	move.l	d2,(a4)			;(Aq)+'
	movea.l	a3,a5			;Ar'
	movea.l	a4,a0			;Aq'
	move.w	d6,ccr
	op	(a5)+,(a0)+		;(Ar')+',(Aq')+'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)+'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)+'
	jsr	crc32_long
	subq.l	#1,a5
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	subq.l	#1,a0
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)+
	bne	102b
	move.l	(a1)+,d1		;(Ar)+
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mpw_mpw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)+,(Aq)+'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)+'
	lea.l	work_area+4,a4		;(Aq)+'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)+
101:
	swap.w	d1
	lea.l	word_data,a2
	move.l	(a2)+,d2		;(Aq)+
102:
	swap.w	d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)+'
	move.l	d2,(a4)			;(Aq)+'
	movea.l	a3,a5			;Ar'
	movea.l	a4,a0			;Aq'
	move.w	d6,ccr
	op	(a5)+,(a0)+		;(Ar')+',(Aq')+'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)+'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)+'
	jsr	crc32_long
	subq.l	#2,a5
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	subq.l	#2,a0
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)+
	bne	102b
	move.l	(a1)+,d1		;(Ar)+
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mpl_mpl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)+,(Aq)+'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;(Ar)+'
	lea.l	work_area+4,a4		;(Aq)+'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)+
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;(Aq)+
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;(Ar)+'
	move.l	d2,(a4)			;(Aq)+'
	movea.l	a3,a5			;Ar'
	movea.l	a4,a0			;Aq'
	move.w	d6,ccr
	op	(a5)+,(a0)+		;(Ar')+',(Aq')+'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;(Ar)+'
	jsr	crc32_long
	move.l	(a4),d0			;(Aq)+'
	jsr	crc32_long
	subq.l	#4,a5
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	subq.l	#4,a0
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(Aq)+
	bne	102b
	move.l	(a1)+,d1		;(Ar)+
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mn?_mn?

xxx_mnb_mnb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op -(Ar),-(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;-(Ar)'
	lea.l	work_area+4,a4		;-(Aq)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;-(Ar)
101:
	ror.l	#8,d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;-(Aq)
102:
	ror.l	#8,d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;-(Ar)'
	move.l	d2,(a4)			;-(Aq)'
	lea.l	1(a3),a5		;Ar'+1
	lea.l	1(a4),a0		;Aq'+1
	move.w	d6,ccr
	op	-(a5),-(a0)		;-(Ar')',-(Aq')'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;-(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;-(Aq)'
	jsr	crc32_long
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;-(Aq)
	bne	102b
	move.l	(a1)+,d1		;-(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mnw_mnw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op -(Ar),-(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;-(Ar)'
	lea.l	work_area+4,a4		;-(Aq)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;-(Ar)
101:
	swap.w	d1
	lea.l	word_data,a2
	move.l	(a2)+,d2		;-(Aq)
102:
	swap.w	d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;-(Ar)'
	move.l	d2,(a4)			;-(Aq)'
	lea.l	2(a3),a5		;Ar'+2
	lea.l	2(a4),a0		;Aq'+2
	move.w	d6,ccr
	op	-(a5),-(a0)		;-(Ar')',-(Aq')'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;-(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;-(Aq)'
	jsr	crc32_long
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;-(Aq)
	bne	102b
	move.l	(a1)+,d1		;-(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mnl_mnl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op -(Ar),-(Aq)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;-(Ar)'
	lea.l	work_area+4,a4		;-(Aq)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;-(Ar)
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;-(Aq)
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a3)			;-(Ar)'
	move.l	d2,(a4)			;-(Aq)'
	lea.l	4(a3),a5		;Ar'+4
	lea.l	4(a4),a0		;Aq'+4
	move.w	d6,ccr
	op	-(a5),-(a0)		;-(Ar')',-(Aq')'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;-(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;-(Aq)'
	jsr	crc32_long
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;-(Aq)
	bne	102b
	move.l	(a1)+,d1		;-(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_im?_dr?

xxx_imb_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imb_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imw_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_iml_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00000000,$ffffffff,$00000001,$fffffffe,$00000080,$ffffff7f,$00000100,$fffffeff,$00008000,$ffff7fff,$00010000,$fffeffff,$00800000,$ff7fffff,$01000000,$feffffff,$80000000,$7fffffff	;IM_LONG
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_drb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_im5_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Dr'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,0,1,7,8,9,15,16,17,23,24,25,31	;IM_5BIT
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	#im,d2			;#<data>,Dr'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_im?_ar?

xxx_imq_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,Ar'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	movea.l	d1,a2			;Ar'
	move.w	d6,ccr
	op	#im,a2			;#<data>,Ar'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a2,d0			;Ar'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_im?_mm?

xxx_imb_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imw_mmw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_iml_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00000000,$ffffffff,$00000001,$fffffffe,$00000080,$ffffff7f,$00000100,$fffffeff,$00008000,$ffff7fff,$00010000,$fffeffff,$00800000,$ff7fffff,$01000000,$feffffff,$80000000,$7fffffff	;IM_LONG
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_mmw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	swap.w	d1
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imq_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,1,2,3,4,5,6,7,8	;IM_QUICK
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_im3_mmb	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;(Ar)
101:
	ror.l	#8,d1
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,0,1,2,3,4,5,6,7	;IM_3BIT
	move.l	d1,(a0)			;(Ar)'
	move.w	d6,ccr
	op	#im,(a0)		;#<data>,(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_im?_ccr

xxx_imb_ccr	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,CCR'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;CCR
101:
  .irp im,$00,$ff,$01,$fe,$02,$fd,$04,$fb,$08,$f7,$10,$ef,$20,$df,$40,$bf,$80,$7f	;IM_BYTE
	move.w	d1,ccr			;CCR
	op	#im,ccr			;#<data>,CCR
	move.w	sr,d0
	jsr	crc32_byte
  .endm
	move.l	(a1)+,d1		;CCR
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_imw_ccr	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op #<data>,CCR'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;CCR
101:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.w	d1,ccr			;CCR
	op	#im,ccr			;#<data>,CCR
	move.w	sr,d0
	jsr	crc32_byte
  .endm
	move.l	(a1)+,d1		;CCR
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_mw?_dr?

xxx_mwl_drw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (d16,Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(d16,Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;(d16,Ar)
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;(d16,Ar)'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	(0.w,a0),d4		;(d16,Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(d16,Ar)'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;(d16,Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mwq_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (d16,Ar),Dq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(d16,Ar)'
	lea.l	quad_data,a1
	movem.l	(a1)+,d1-d2		;(d16,Ar)
101:
	lea.l	word_data,a3
	move.l	(a3)+,d3		;Dq
103:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movem.l	d1-d2,(a0)		;(d16,Ar)'
	move.l	d3,d5			;Dq'
	move.w	d6,ccr
	op	(0.w,a0),d5		;(d16,Ar)',Dq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a0),d0			;(d16,Ar)'
	jsr	crc32_long
	move.l	4(a0),d0		;(d16,Ar)'
	jsr	crc32_long
	move.l	d5,d0			;Dq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;Dq
	bne	103b
	movem.l	(a1)+,d1-d2		;(d16,Ar)
	move.l	d1,d0
	or.l	d2,d0
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;xxx_dr?_mw?

xxx_drw_mwl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dq,(d16,Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(d16,Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dq
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;(d16,Ar)
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dq'
	move.l	d2,(a0)			;(d16,Ar)'
	move.w	d6,ccr
	op	d3,(0.w,a0)		;Dq',(d16,Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dq'
	jsr	crc32_long
	move.l	(a0),d0			;(d16,Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;(d16,Ar)
	bne	102b
	move.l	(a1)+,d1		;Dq
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drl_mwq	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dq,(d16,Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(d16,Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dq
101:
	lea.l	quad_data,a2
	movem.l	(a2)+,d2-d3		;(d16,Ar)
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4			;Dq'
	movem.l	d2-d3,(a0)		;(d16,Ar)'
	move.w	d6,ccr
	op	d4,(0.w,a0)		;Dq',(d16,Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0			;Dq'
	jsr	crc32_long
	move.l	(a0),d0			;(d16,Ar)'
	jsr	crc32_long
	move.l	4(a0),d0		;(d16,Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	movem.l	(a2)+,d2-d3		;(d16,Ar)
	move.l	d2,d0
	or.l	d3,d0
	bne	102b
	move.l	(a1)+,d1		;Dq
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;bit field

bfxxx_drss	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{#o:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	d1,d2			;Dr'
	move.w	d6,ccr
	op	d2{#of:#wi}		;Dr'{#o:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dr'
	jsr	crc32_long
    .endm
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drsd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{#o:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{#of:d5}		;Dr'{#o:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drds	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{Do:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{d4:#wi}		;Dr'{Do:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drdd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{Do:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{d4:d5}		;Dr'{Do:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmss	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){#o:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){#of:#wi}		;(Ar)'{#o:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
    .endm
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmsd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){#o:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){#of:d5}		;(Ar)'{#o:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmds	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){Do:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	ccr_short_data,a6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){d4:#wi}		;(Ar)'{Do:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmdd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){Do:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
106:
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){d4:d5}		;(Ar)'{Do:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_drss	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,Dr{#o:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	long_short_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d2,d3{#of:#wi}		;Dn',Dr'{#o:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dn'
	jsr	crc32_long
	move.l	d3,d0			;Dr'
	jsr	crc32_long
    .endm
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_drsd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,Dr{#o:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	long_short_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d2,d3{#of:d5}		;Dn',Dr'{#o:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dn'
	jsr	crc32_long
	move.l	d3,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_drds	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,Dr{Do:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	long_short_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d2,d3{d4:#wi}		;Dn',Dr'{Do:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dn'
	jsr	crc32_long
	move.l	d3,d0			;Dr'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_drdd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,Dr{Do:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	long_short_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d2,d3{d4:d5}		;Dn',Dr'{Do:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d2,d0			;Dn'
	jsr	crc32_long
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_mmss	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,(Ar){#o:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	long_data,a0		;Dn
100:
	lea.l	oct_short_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	d1,(a3){#of:#wi}	;Dn',(Ar)'{#o:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0			;Dn'
	jsr	crc32_long
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
    .endm
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_mmsd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,(Ar){#o:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	long_data,a0		;Dn
100:
	lea.l	oct_short_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	d1,(a3){#of:d5}		;Dn',(Ar)'{#o:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0			;Dn'
	jsr	crc32_long
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_mmds	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,(Ar){Do:#w}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	long_data,a0		;Dn
100:
	lea.l	oct_short_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	ccr_short_data,a6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	d1,(a3){d4:#wi}		;Dn',(Ar)'{Do:#w}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0			;Dn'
	jsr	crc32_long
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drl_mmdd	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dn,(Ar){Do:Dw}'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	long_data,a0		;Dn
100:
	lea.l	oct_short_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	ccr_short_data,a6
106:
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	d1,(a3){d4:d5}		;Dn',(Ar)'{Do:Dw}
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0			;Dn'
	jsr	crc32_long
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drss_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{#o:#w},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	long_short_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{#of:#wi},d2		;Dr'{#o:#w},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d2,d0			;Dn'
	jsr	crc32_long
    .endm
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drsd_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{#o:Dw},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	long_short_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{#of:d5},d2		;Dr'{#o:Dw},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d2,d0			;Dn'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drds_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{Do:#w},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	long_short_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{d4:#wi},d2		;Dr'{Do:#w},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d2,d0			;Dn'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_drdd_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr{Do:Dw},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	long_short_data,a0
	move.l	(a0)+,d0		;Dn
100:
	movea.l	d0,a2
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	a2,d2			;Dn'
	move.l	d1,d3			;Dr'
	move.w	d6,ccr
	op	d3{d4:d5},d2		;Dr'{Do:Dw},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d2,d0			;Dn'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a0)+,d0		;Dn
	bne	100b
	move.l	(a5)+,d5		;Do
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmss_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){#o:#w},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	long_short_data,a0	;Dn
100:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
    .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){#of:#wi},d1	;(Ar)'{#o:#w},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	move.l	d1,d0			;Dn'
	jsr	crc32_long
    .endm
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmsd_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){#o:Dw},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	long_short_data,a0	;Dn
100:
	lea.l	ccr_short_data,a6
106:
  .irp of,0,1,7,8,9,15,16,17,23,24,25,31	;IM_OFFSET
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){#of:d5},d1		;(Ar)'{#o:Dw},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	move.l	d1,d0			;Dn'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	(a5)+,d5		;Dw
	bne	105b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmds_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){Do:#w},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	long_short_data,a0	;Dn
100:
	lea.l	ccr_short_data,a6
106:
  .irp wi,1,7,8,9,15,16,17,23,24,25,31,32	;IM_WIDTH
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){d4:#wi},d1		;(Ar)'{Do:#w},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	move.l	d1,d0			;Dn'
	jsr	crc32_long
  .endm
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bfxxx_mmdd_drl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar){Do:Dw},Dn'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area+4,a3		;(Ar)'
	lea.l	oct_data,a2
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
102:
	lea.l	bf_offset_data,a4
	move.l	(a4)+,d4		;Do
104:
	lea.l	bf_width_data,a5
	move.l	(a5)+,d5		;Dw
105:
	lea.l	long_short_data,a0	;Dn
100:
	lea.l	ccr_short_data,a6
106:
	move.l	(a0),d1			;Dn'
	movem.l	d2-d3/d6/a1,-4(a3)	;(Ar-4)',(Ar)',(Ar+4)',(Ar-8)'
	move.w	(a6),ccr
	op	(a3){d4:d5},d1		;(Ar)'{Do:Dw},Dn'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	-4(a3),d0		;(Ar-4)'
	jsr	crc32_long
	move.l	(a3),d0			;(Ar)'
	jsr	crc32_long
	move.l	4(a3),d0		;(Ar+4)'
	jsr	crc32_long
	move.l	8(a3),d0		;(Ar+8)'
	jsr	crc32_long
	move.l	d1,d0			;Dn'
	jsr	crc32_long
	addq.l	#2,a6
	tst.w	(a6)
	bne	106b
	addq.l	#4,a0			;Dn
	tst.l	(a0)
	bne	100b
	move.l	(a5)+,d5		;Dw
	bne	105b
	move.l	(a4)+,d4		;Do
	bne	104b
	movem.l	(a2)+,d2-d3/d6/a1	;(Ar-4),(Ar),(Ar+4),(Ar+8)
	move.l	a1,d0
	or.l	d2,d0
	or.l	d3,d0
	or.l	d6,d0
	bne	102b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;pack/unpk

xxx_drw_drb_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq,#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4,#im		;Dr',Dq',#<data>
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mnw_mnb_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op -(Ar),-(Aq),#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;-(Ar)'
	lea.l	work_area+4,a4		;-(Aq)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;-(Ar)
101:
	swap.w	d1
	lea.l	byte_data,a2
	move.l	(a2)+,d2		;-(Aq)
102:
	ror.l	#8,d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,(a3)			;-(Ar)'
	move.l	d2,(a4)			;-(Aq)'
	lea.l	2(a3),a5		;Ar'+2
	lea.l	1(a4),a0		;Aq'+1
	move.w	d6,ccr
	op	-(a5),-(a0),#im		;-(Ar')',-(Aq')',#<data>
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;-(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;-(Aq)'
	jsr	crc32_long
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;-(Aq)
	bne	102b
	move.l	(a1)+,d1		;-(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_drb_drw_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,Dq,#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dr
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2		;Dq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,d3			;Dr'
	move.l	d2,d4			;Dq'
	move.w	d6,ccr
	op	d3,d4,#im		;Dr',Dq',#<data>
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d3,d0			;Dr'
	jsr	crc32_long
	move.l	d4,d0			;Dq'
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Dq
	bne	102b
	move.l	(a1)+,d1		;Dr
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

xxx_mnb_mnw_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op -(Ar),-(Aq),#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a3		;-(Ar)'
	lea.l	work_area+4,a4		;-(Aq)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;-(Ar)
101:
	ror.l	#8,d1
	lea.l	word_data,a2
	move.l	(a2)+,d2		;-(Aq)
102:
	swap.w	d2
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	move.l	d1,(a3)			;-(Ar)'
	move.l	d2,(a4)			;-(Aq)'
	lea.l	1(a3),a5		;Ar'+1
	lea.l	2(a4),a0		;Aq'+2
	move.w	d6,ccr
	op	-(a5),-(a0),#im		;-(Ar')',-(Aq')',#<data>
	move.w	sr,d0
	jsr	crc32_byte
	move.l	(a3),d0			;-(Ar)'
	jsr	crc32_long
	move.l	(a4),d0			;-(Aq)'
	jsr	crc32_long
	suba.l	a3,a5
	move.l	a5,d0			;Ar'-Ar
	jsr	crc32_long
	suba.l	a4,a0
	move.l	a0,d0			;Aq'-Aq
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;-(Aq)
	bne	102b
	move.l	(a1)+,d1		;-(Ar)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;bcc/bsr/jmp/jsr/dbcc

bcc_label	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <label>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	suba.l	a0,a0			;flags
	move.w	d6,ccr
	op	101f
	addq.l	#1,a0			;15=not taken
	addq.l	#2,a0			;14=too short
101:
	addq.l	#4,a0			;12=taken
	addq.l	#8,a0			;8=too long
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a0,d0			;flags
	jsr	crc32_byte
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

bsr_label	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <label>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	suba.l	a0,a0			;flags
	move.w	d6,ccr
	op	101f
100:
	addq.l	#1,a0			;15=not taken
	addq.l	#2,a0			;14=too short
101:
	addq.l	#4,a0			;12=taken
	addq.l	#8,a0			;8=too long
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a0,d0			;flags
	jsr	crc32_byte
	move.l	(sp)+,d0		;pc
	sub.l	#100b,d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

jmp_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	101f,a1			;Ar
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
	suba.l	a0,a0			;flags
	move.w	d6,ccr
	op	(a1)			;(Ar)
	addq.l	#1,a0			;15=not taken
	addq.l	#2,a0			;14=too short
101:
	addq.l	#4,a0			;12=taken
	addq.l	#8,a0			;8=too long
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a0,d0			;flags
	jsr	crc32_byte
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

jsr_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	lea.l	101f,a1			;Ar
	move.w	(a6)+,d6
106:
	suba.l	a0,a0			;flags
	move.w	d6,ccr
	op	(a1)			;(Ar)
100:
	addq.l	#1,a0			;15=not taken
	addq.l	#2,a0			;14=too short
101:
	addq.l	#4,a0			;12=taken
	addq.l	#8,a0			;8=too long
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a0,d0			;flags
	jsr	crc32_byte
	move.l	(sp)+,d0		;pc
	sub.l	#100b,d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

dbcc_drw_label	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dr,<label>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	ccr_long_data,a6
	move.w	(a6)+,d6
106:
  .irp n1,$00000000,$00000001,$00008000,$00010000,$80000000
	move.l	#n1,d1			;Dr
	suba.l	a0,a0			;flags
	move.w	d6,ccr
	op	d1,101f
	addq.l	#1,a0			;15=not taken
	addq.l	#2,a0			;14=too short
101:
	addq.l	#4,a0			;12=taken
	addq.l	#8,a0			;8=too long
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0			;Dr
	jsr	crc32_long
	move.l	a0,d0			;flags
	jsr	crc32_byte
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;lea/pea

lea_mml_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar),Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2		;Aq
102:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movea.l	d1,a3			;Ar'
	movea.l	d2,a4			;Aq'
	move.w	d6,ccr
	op	(a3),a4			;(Ar)',Aq'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a3,d0			;Ar'
	jsr	crc32_long
	move.l	a4,d0			;Aq'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2		;Aq
	bne	102b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

pea_mml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Ar
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	movea.l	d1,a3			;Ar'
	move.w	d6,ccr
	op	(a3)			;(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	a3,d0			;Ar'
	jsr	crc32_long
	move.l	(sp)+,d0		;Ar''
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;link/unlk

link_arl_imw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar,#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	movea.l	sp,a5			;old A7
	lea.l	long_data,a1
	move.l	(a1)+,d1		;old Ar
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$0000,$ffff,$0001,$fffe,$0008,$fff7,$0010,$ffef,$0080,$ff7f,$0100,$feff,$0800,$f7ff,$1000,$efff,$8000,$7fff	;IM_WORD
	movea.l	d1,a2			;old Ar'
	move.w	d6,ccr
;A7 must be the user stack pointer.
	op	a2,#im			;LINK.W Ar,#<data> == PEA.L (Ar);MOVEA.L A7,Ar;ADDA.W #<data>,A7
;A7 is broken. Don't use it as a stack pointer.
	move.w	sr,d0
	move.l	-4(a5),d2		;old Ar'
	movea.l	sp,a4			;new A7
	movea.l	a5,sp
;A7 is restored.
	jsr	crc32_byte
	move.l	d2,d0			;old Ar'
	jsr	crc32_long
	move.l	a2,d0			;new Ar'
	sub.l	a5,d0
	jsr	crc32_long
	move.l	a4,d0			;new A7
	sub.l	a5,d0
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;old Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

link_arl_iml	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar,#<data>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	movea.l	sp,a5			;old A7
	lea.l	long_data,a1
	move.l	(a1)+,d1		;old Ar
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
  .irp im,$00000000,$ffffffff,$00000001,$fffffffe,$00000080,$ffffff7f,$00000100,$fffffeff,$00008000,$ffff7fff,$00010000,$fffeffff,$00800000,$ff7fffff,$01000000,$feffffff,$80000000,$7fffffff	;IM_LONG
	movea.l	d1,a2			;old Ar'
	move.w	d6,ccr
;A7 must be the user stack pointer.
	op	a2,#im			;LINK.L Ar,#<data> == PEA.L (Ar);MOVEA.L A7,Ar;ADDA.L #<data>,A7
;A7 is broken. Don't use it as a stack pointer.
	move.w	sr,d0
	move.l	-4(a5),d2		;old Ar'
	movea.l	sp,a4			;new A7
	movea.l	a5,sp
;A7 is restored.
	jsr	crc32_byte
	move.l	d2,d0			;old Ar'
	jsr	crc32_long
	move.l	a2,d0			;new Ar'
	sub.l	a5,d0
	jsr	crc32_long
	move.l	a4,d0			;new A7
	sub.l	a5,d0
	jsr	crc32_long
  .endm
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;old Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

unlk_arl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Ar'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	movea.l	sp,a5			;old A7
	lea.l	work_area,a0		;Ar
	lea.l	long_data,a1
	move.l	(a1)+,d1		;new A7
101:
	lea.l	ccr_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,(a0)			;new A7
	movea.l	a0,a2			;Ar'
	move.w	d6,ccr
;A7 must be the user stack pointer.
	op	a2			;UNLK Ar == MOVEA.L Ar,A7;MOVEA.L (A7)+,Ar
;A7 is broken. Don't use it as a stack pointer.
	move.w	sr,d0
	movea.l	sp,a4			;new A7
	movea.l	a5,sp
;A7 is restored.
	jsr	crc32_byte
	move.l	(a0),d0			;new A7
	jsr	crc32_long
	move.l	a2,d0			;Ar'
	jsr	crc32_long
	move.l	a4,d0			;new A7
	sub.l	a0,d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a1)+,d1		;Ar
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;movem

movem_list_mnw	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <list>,-(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	movea.l	#$55555555,a5
	lea.l	long_data,a1
	move.l	(a1)+,d1
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2
102:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4
	movea.l	a0,a3
	movea.l	d2,a4
	move.w	a5,(a3)+
	move.w	a5,(a3)+
	move.w	a5,(a3)+
	move.w	d6,ccr
	movem.w	d4/a3-a4,-(a3)
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0
	jsr	crc32_long
	move.l	a3,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a4,d0
	jsr	crc32_long
	move.w	(a0),d0
	jsr	crc32_word
	move.w	2(a0),d0
	sub.w	a0,d0
	jsr	crc32_word
	move.w	4(a0),d0
	jsr	crc32_word
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2
	bne	102b
	move.l	(a1)+,d1
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

movem_list_mnl	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <list>,-(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	movea.l	#$55555555,a5
	lea.l	long_data,a1
	move.l	(a1)+,d1
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2
102:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4
	movea.l	a0,a3
	movea.l	d2,a4
	move.l	a5,(a3)+
	move.l	a5,(a3)+
	move.l	a5,(a3)+
	move.w	d6,ccr
	movem.l	d4/a3-a4,-(a3)
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0
	jsr	crc32_long
	move.l	a3,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a4,d0
	jsr	crc32_long
	move.l	(a0),d0
	jsr	crc32_long
	move.l	4(a0),d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	8(a0),d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2
	bne	102b
	move.l	(a1)+,d1
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

movem_mpw_list	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)+,<list>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	movea.l	#$55555555,a5
	lea.l	word_data,a1
	move.l	(a1)+,d1
101:
	lea.l	word_data,a2
	move.l	(a2)+,d2
102:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	movea.l	a0,a3
	move.w	d1,(a3)+
	move.w	a5,(a3)+
	move.w	d2,(a3)+
	move.l	a5,d4
	movea.l	a0,a3
	movea.l	a5,a4
	move.w	d6,ccr
	movem.w	(a3)+,d4/a3-a4
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0
	jsr	crc32_long
	move.l	a3,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a4,d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2
	bne	102b
	move.l	(a1)+,d1
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

movem_mpl_list	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op (Ar)+,<list>'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	movea.l	#$55555555,a5
	lea.l	long_data,a1
	move.l	(a1)+,d1
101:
	lea.l	long_data,a2
	move.l	(a2)+,d2
102:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	movea.l	a0,a3
	move.l	d1,(a3)+
	move.l	a5,(a3)+
	move.l	d2,(a3)+
	move.l	a5,d4
	movea.l	a0,a3
	movea.l	a5,a4
	move.w	d6,ccr
	movem.l	(a3)+,d4/a3-a4
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0
	jsr	crc32_long
	move.l	a3,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a4,d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a2)+,d2
	bne	102b
	move.l	(a1)+,d1
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

;--------------------------------------------------------------------------------
;cas/cas2

cas_byte	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dc,Du,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	byte_data,a1
	move.l	(a1)+,d1		;Dc
101:
	lea.l	byte_short_data,a2
	move.l	(a2)+,d2		;Du
102:
	lea.l	byte_data,a3
	move.l	(a3)+,d3		;(Ar)
103:
	ror.l	#8,d3
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4			;Dc'
	move.l	d2,d5			;Du'
	move.l	d3,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d4,d5,(a0)		;Dc',Du',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0			;Dc'
	jsr	crc32_long
	move.l	d5,d0			;Du'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;(Ar)
	bne	103b
	move.l	(a2)+,d2		;Du
	bne	102b
	move.l	(a1)+,d1		;Dc
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

cas_word	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dc,Du,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	word_data,a1
	move.l	(a1)+,d1		;Dc
101:
	lea.l	word_short_data,a2
	move.l	(a2)+,d2		;Du
102:
	lea.l	word_data,a3
	move.l	(a3)+,d3		;(Ar)
103:
	swap.w	d3
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4			;Dc'
	move.l	d2,d5			;Du'
	move.l	d3,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d4,d5,(a0)		;Dc',Du',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0			;Dc'
	jsr	crc32_long
	move.l	d5,d0			;Du'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;(Ar)
	bne	103b
	move.l	(a2)+,d2		;Du
	bne	102b
	move.l	(a1)+,d1		;Dc
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

cas_long	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dc,Du,(Ar)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0		;(Ar)'
	lea.l	long_data,a1
	move.l	(a1)+,d1		;Dc
101:
	lea.l	long_short_data,a2
	move.l	(a2)+,d2		;Du
102:
	lea.l	long_data,a3
	move.l	(a3)+,d3		;(Ar)
103:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	move.l	d1,d4			;Dc'
	move.l	d2,d5			;Du'
	move.l	d3,(a0)			;(Ar)'
	move.w	d6,ccr
	op	d4,d5,(a0)		;Dc',Du',(Ar)'
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d4,d0			;Dc'
	jsr	crc32_long
	move.l	d5,d0			;Du'
	jsr	crc32_long
	move.l	(a0),d0			;(Ar)'
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	move.l	(a3)+,d3		;(Ar)
	bne	103b
	move.l	(a2)+,d2		;Du
	bne	102b
	move.l	(a1)+,d1		;Dc
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

cas2_word	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	lea.l	long_data,a1		;Dc1:Dc2
101:
	lea.l	long_short_data,a2	;Du1:Du2
102:
	lea.l	long_data,a3		;(Rn1):(Rn2)
103:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	lea.l	(a0),a4
	lea.l	2(a0),a5
	movem.w	(a1),d1-d2
	movem.w	(a2),d3-d4
	move.w	(a3),(a4)
	move.w	2(a3),(a5)
	move.w	d6,ccr
	op	d1:d2,d3:d4,(a4):(a5)
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0
	jsr	crc32_long
	move.l	d2,d0
	jsr	crc32_long
	move.l	d3,d0
	jsr	crc32_long
	move.l	d4,d0
	jsr	crc32_long
	move.l	a4,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a5,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.w	(a0),d0
	jsr	crc32_word
	move.w	2(a0),d0
	jsr	crc32_word
	move.w	(a6)+,d6
	bne	106b
	addq.l	#4,a3
	tst.l	(a3)
	bne	103b
	addq.l	#4,a2
	tst.l	(a2)
	bne	102b
	addq.l	#4,a1
	tst.l	(a1)
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

cas2_long	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a0
	lea.l	quad_data,a1		;Dc1:Dc2
101:
	lea.l	quad_short_data,a2	;Du1:Du2
102:
	lea.l	quad_data,a3		;(Rn1):(Rn2)
103:
	lea.l	ccr_short_data,a6
	move.w	(a6)+,d6
106:
	lea.l	(a0),a4
	lea.l	4(a0),a5
	movem.l	(a1),d1-d2
	movem.l	(a2),d3-d4
	move.l	(a3),(a4)
	move.l	4(a3),(a5)
	move.w	d6,ccr
	op	d1:d2,d3:d4,(a4):(a5)
	move.w	sr,d0
	jsr	crc32_byte
	move.l	d1,d0
	jsr	crc32_long
	move.l	d2,d0
	jsr	crc32_long
	move.l	d3,d0
	jsr	crc32_long
	move.l	d4,d0
	jsr	crc32_long
	move.l	a4,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	a5,d0
	sub.l	a0,d0
	jsr	crc32_long
	move.l	(a0),d0
	jsr	crc32_long
	move.l	4(a0),d0
	jsr	crc32_long
	move.w	(a6)+,d6
	bne	106b
	addq.l	#8,a3
	move.l	(a3),d0
	or.l	4(a3),d0
	bne	103b
	addq.l	#8,a2
	move.l	(a2),d0
	or.l	4(a2),d0
	bne	102b
	addq.l	#8,a1
	move.l	(a1),d0
	or.l	4(a1),d0
	bne	101b
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm


;--------------------------------------------------------------------------------
;effective address

eatest	.macro	ea
	lea.l	ea,a0
	move.l	a0,d0
	sub.l	a4,d0
	jsr	crc32_long
	.endm

zeatest	.macro	ea
	lea.l	ea,a0
	move.l	a0,d0
	jsr	crc32_long
	.endm

lea_brief	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <brief-format>,Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a4
	move.l	#$00008000,d5
  .irp d8,$ffffff80,$0000007c
	eatest	(d8,a4,d5.w)			;nnnn0000dddddddd
	eatest	(d8,a4,d5.w*2)			;nnnn0010dddddddd
	eatest	(d8,a4,d5.w*4)			;nnnn0100dddddddd
	eatest	(d8,a4,d5.w*8)			;nnnn0110dddddddd
	eatest	(d8,a4,d5.l)			;nnnn1000dddddddd
	eatest	(d8,a4,d5.l*2)			;nnnn1010dddddddd
	eatest	(d8,a4,d5.l*4)			;nnnn1100dddddddd
	eatest	(d8,a4,d5.l*8)			;nnnn1110dddddddd
  .endm
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm

lea_full	.macro	op,crc
	leastr	'&op',a0
	jsr	mnemonic_check
	beq	@skip
	fillto	@v,36,'&op <full-format>,Aq'
	print	@v
	jsr	crc32_reset
	try_illegal_instruction
	lea.l	work_area,a4
	move.l	#$00008000,d5
	lea.l	work_area_start,a0
	lea.l	work_area_end,a1
	lea.l	256(a0),a2
100:	move.l	a2,(a0)+
	addq.l	#4,a2
	cmpa.l	a1,a0
	blo	100b
	eatest	(a4,d5.w)			;nnnn000100010000
	eatest	(a4,d5.w*2)			;nnnn001100010000
	eatest	(a4,d5.w*4)			;nnnn010100010000
	eatest	(a4,d5.w*8)			;nnnn011100010000
	eatest	(a4,zd5.w)			;nnnn000101010000
	eatest	(a4,zd5.w*2)			;nnnn001101010000
	eatest	(a4,zd5.w*4)			;nnnn010101010000
	eatest	(a4,zd5.w*8)			;nnnn011101010000
	eatest	(a4,d5.l)			;nnnn100100010000
	eatest	(a4,d5.l*2)			;nnnn101100010000
	eatest	(a4,d5.l*4)			;nnnn110100010000
	eatest	(a4,d5.l*8)			;nnnn111100010000
	eatest	(a4,zd5.l)			;nnnn100101010000
	eatest	(a4,zd5.l*2)			;nnnn101101010000
	eatest	(a4,zd5.l*4)			;nnnn110101010000
	eatest	(a4,zd5.l*8)			;nnnn111101010000
	eatest	([a4,d5.w])			;nnnn000100010001
	eatest	([a4,d5.w*2])			;nnnn001100010001
	eatest	([a4,d5.w*4])			;nnnn010100010001
	eatest	([a4,d5.w*8])			;nnnn011100010001
	eatest	([a4,zd5.w])			;nnnn000101010001
	eatest	([a4,zd5.w*2])			;nnnn001101010001
	eatest	([a4,zd5.w*4])			;nnnn010101010001
	eatest	([a4,zd5.w*8])			;nnnn011101010001
	eatest	([a4,d5.l])			;nnnn100100010001
	eatest	([a4,d5.l*2])			;nnnn101100010001
	eatest	([a4,d5.l*4])			;nnnn110100010001
	eatest	([a4,d5.l*8])			;nnnn111100010001
	eatest	([a4,zd5.l])			;nnnn100101010001
	eatest	([a4,zd5.l*2])			;nnnn101101010001
	eatest	([a4,zd5.l*4])			;nnnn110101010001
	eatest	([a4,zd5.l*8])			;nnnn111101010001
	eatest	([a4],d5.w)			;nnnn000100010101
	eatest	([a4],d5.w*2)			;nnnn001100010101
	eatest	([a4],d5.w*4)			;nnnn010100010101
	eatest	([a4],d5.w*8)			;nnnn011100010101
	eatest	([a4],zd5.w)			;nnnn000101010101
	eatest	([a4],zd5.w*2)			;nnnn001101010101
	eatest	([a4],zd5.w*4)			;nnnn010101010101
	eatest	([a4],zd5.w*8)			;nnnn011101010101
	eatest	([a4],d5.l)			;nnnn100100010101
	eatest	([a4],d5.l*2)			;nnnn101100010101
	eatest	([a4],d5.l*4)			;nnnn110100010101
	eatest	([a4],d5.l*8)			;nnnn111100010101
	eatest	([a4],zd5.l)			;nnnn100101010101
	eatest	([a4],zd5.l*2)			;nnnn101101010101
	eatest	([a4],zd5.l*4)			;nnnn110101010101
	eatest	([a4],zd5.l*8)			;nnnn111101010101
  .irp od,$ffff8000,$00007ffc
	eatest	([a4,d5.w],od.w)		;nnnn000100010010
	eatest	([a4,d5.w*2],od.w)		;nnnn001100010010
	eatest	([a4,d5.w*4],od.w)		;nnnn010100010010
	eatest	([a4,d5.w*8],od.w)		;nnnn011100010010
	eatest	([a4,zd5.w],od.w)		;nnnn000101010010
	eatest	([a4,zd5.w*2],od.w)		;nnnn001101010010
	eatest	([a4,zd5.w*4],od.w)		;nnnn010101010010
	eatest	([a4,zd5.w*8],od.w)		;nnnn011101010010
	eatest	([a4,d5.l],od.w)		;nnnn100100010010
	eatest	([a4,d5.l*2],od.w)		;nnnn101100010010
	eatest	([a4,d5.l*4],od.w)		;nnnn110100010010
	eatest	([a4,d5.l*8],od.w)		;nnnn111100010010
	eatest	([a4,zd5.l],od.w)		;nnnn100101010010
	eatest	([a4,zd5.l*2],od.w)		;nnnn101101010010
	eatest	([a4,zd5.l*4],od.w)		;nnnn110101010010
	eatest	([a4,zd5.l*8],od.w)		;nnnn111101010010
	eatest	([a4],d5.w,od.w)		;nnnn000100010110
	eatest	([a4],d5.w*2,od.w)		;nnnn001100010110
	eatest	([a4],d5.w*4,od.w)		;nnnn010100010110
	eatest	([a4],d5.w*8,od.w)		;nnnn011100010110
	eatest	([a4],zd5.w,od.w)		;nnnn000101010110
	eatest	([a4],zd5.w*2,od.w)		;nnnn001101010110
	eatest	([a4],zd5.w*4,od.w)		;nnnn010101010110
	eatest	([a4],zd5.w*8,od.w)		;nnnn011101010110
	eatest	([a4],d5.l,od.w)		;nnnn100100010110
	eatest	([a4],d5.l*2,od.w)		;nnnn101100010110
	eatest	([a4],d5.l*4,od.w)		;nnnn110100010110
	eatest	([a4],d5.l*8,od.w)		;nnnn111100010110
	eatest	([a4],zd5.l,od.w)		;nnnn100101010110
	eatest	([a4],zd5.l*2,od.w)		;nnnn101101010110
	eatest	([a4],zd5.l*4,od.w)		;nnnn110101010110
	eatest	([a4],zd5.l*8,od.w)		;nnnn111101010110
  .endm
  .irp od,$ffff7ffc,$00008000
	eatest	([a4,d5.w],od.l)		;nnnn000100010011
	eatest	([a4,d5.w*2],od.l)		;nnnn001100010011
	eatest	([a4,d5.w*4],od.l)		;nnnn010100010011
	eatest	([a4,d5.w*8],od.l)		;nnnn011100010011
	eatest	([a4,zd5.w],od.l)		;nnnn000101010011
	eatest	([a4,zd5.w*2],od.l)		;nnnn001101010011
	eatest	([a4,zd5.w*4],od.l)		;nnnn010101010011
	eatest	([a4,zd5.w*8],od.l)		;nnnn011101010011
	eatest	([a4,d5.l],od.l)		;nnnn100100010011
	eatest	([a4,d5.l*2],od.l)		;nnnn101100010011
	eatest	([a4,d5.l*4],od.l)		;nnnn110100010011
	eatest	([a4,d5.l*8],od.l)		;nnnn111100010011
	eatest	([a4,zd5.l],od.l)		;nnnn100101010011
	eatest	([a4,zd5.l*2],od.l)		;nnnn101101010011
	eatest	([a4,zd5.l*4],od.l)		;nnnn110101010011
	eatest	([a4,zd5.l*8],od.l)		;nnnn111101010011
	eatest	([a4],d5.w,od.l)		;nnnn000100010111
	eatest	([a4],d5.w*2,od.l)		;nnnn001100010111
	eatest	([a4],d5.w*4,od.l)		;nnnn010100010111
	eatest	([a4],d5.w*8,od.l)		;nnnn011100010111
	eatest	([a4],zd5.w,od.l)		;nnnn000101010111
	eatest	([a4],zd5.w*2,od.l)		;nnnn001101010111
	eatest	([a4],zd5.w*4,od.l)		;nnnn010101010111
	eatest	([a4],zd5.w*8,od.l)		;nnnn011101010111
	eatest	([a4],d5.l,od.l)		;nnnn100100010111
	eatest	([a4],d5.l*2,od.l)		;nnnn101100010111
	eatest	([a4],d5.l*4,od.l)		;nnnn110100010111
	eatest	([a4],d5.l*8,od.l)		;nnnn111100010111
	eatest	([a4],zd5.l,od.l)		;nnnn100101010111
	eatest	([a4],zd5.l*2,od.l)		;nnnn101101010111
	eatest	([a4],zd5.l*4,od.l)		;nnnn110101010111
	eatest	([a4],zd5.l*8,od.l)		;nnnn111101010111
  .endm
  .irp bd,$ffff8000,$00007ffc
	eatest	(bd.w,a4,d5.w)			;nnnn000100100000
	eatest	(bd.w,a4,d5.w*2)		;nnnn001100100000
	eatest	(bd.w,a4,d5.w*4)		;nnnn010100100000
	eatest	(bd.w,a4,d5.w*8)		;nnnn011100100000
	eatest	(bd.w,a4,zd5.w)			;nnnn000101100000
	eatest	(bd.w,a4,zd5.w*2)		;nnnn001101100000
	eatest	(bd.w,a4,zd5.w*4)		;nnnn010101100000
	eatest	(bd.w,a4,zd5.w*8)		;nnnn011101100000
	eatest	(bd.w,a4,d5.l)			;nnnn100100100000
	eatest	(bd.w,a4,d5.l*2)		;nnnn101100100000
	eatest	(bd.w,a4,d5.l*4)		;nnnn110100100000
	eatest	(bd.w,a4,d5.l*8)		;nnnn111100100000
	eatest	(bd.w,a4,zd5.l)			;nnnn100101100000
	eatest	(bd.w,a4,zd5.l*2)		;nnnn101101100000
	eatest	(bd.w,a4,zd5.l*4)		;nnnn110101100000
	eatest	(bd.w,a4,zd5.l*8)		;nnnn111101100000
  .endm
  .irp bd,$ffff8000,$00007ffc
	eatest	([bd.w,a4,d5.w])		;nnnn000100100001
	eatest	([bd.w,a4,d5.w*2])		;nnnn001100100001
	eatest	([bd.w,a4,d5.w*4])		;nnnn010100100001
	eatest	([bd.w,a4,d5.w*8])		;nnnn011100100001
	eatest	([bd.w,a4,zd5.w])		;nnnn000101100001
	eatest	([bd.w,a4,zd5.w*2])		;nnnn001101100001
	eatest	([bd.w,a4,zd5.w*4])		;nnnn010101100001
	eatest	([bd.w,a4,zd5.w*8])		;nnnn011101100001
	eatest	([bd.w,a4,d5.l])		;nnnn100100100001
	eatest	([bd.w,a4,d5.l*2])		;nnnn101100100001
	eatest	([bd.w,a4,d5.l*4])		;nnnn110100100001
	eatest	([bd.w,a4,d5.l*8])		;nnnn111100100001
	eatest	([bd.w,a4,zd5.l])		;nnnn100101100001
	eatest	([bd.w,a4,zd5.l*2])		;nnnn101101100001
	eatest	([bd.w,a4,zd5.l*4])		;nnnn110101100001
	eatest	([bd.w,a4,zd5.l*8])		;nnnn111101100001
	eatest	([bd.w,a4],d5.w)		;nnnn000100100101
	eatest	([bd.w,a4],d5.w*2)		;nnnn001100100101
	eatest	([bd.w,a4],d5.w*4)		;nnnn010100100101
	eatest	([bd.w,a4],d5.w*8)		;nnnn011100100101
	eatest	([bd.w,a4],zd5.w)		;nnnn000101100101
	eatest	([bd.w,a4],zd5.w*2)		;nnnn001101100101
	eatest	([bd.w,a4],zd5.w*4)		;nnnn010101100101
	eatest	([bd.w,a4],zd5.w*8)		;nnnn011101100101
	eatest	([bd.w,a4],d5.l)		;nnnn100100100101
	eatest	([bd.w,a4],d5.l*2)		;nnnn101100100101
	eatest	([bd.w,a4],d5.l*4)		;nnnn110100100101
	eatest	([bd.w,a4],d5.l*8)		;nnnn111100100101
	eatest	([bd.w,a4],zd5.l)		;nnnn100101100101
	eatest	([bd.w,a4],zd5.l*2)		;nnnn101101100101
	eatest	([bd.w,a4],zd5.l*4)		;nnnn110101100101
	eatest	([bd.w,a4],zd5.l*8)		;nnnn111101100101
    .irp od,$ffff8000,$00007ffc
	eatest	([bd.w,a4,d5.w],od.w)		;nnnn000100100010
	eatest	([bd.w,a4,d5.w*2],od.w)		;nnnn001100100010
	eatest	([bd.w,a4,d5.w*4],od.w)		;nnnn010100100010
	eatest	([bd.w,a4,d5.w*8],od.w)		;nnnn011100100010
	eatest	([bd.w,a4,zd5.w],od.w)		;nnnn000101100010
	eatest	([bd.w,a4,zd5.w*2],od.w)	;nnnn001101100010
	eatest	([bd.w,a4,zd5.w*4],od.w)	;nnnn010101100010
	eatest	([bd.w,a4,zd5.w*8],od.w)	;nnnn011101100010
	eatest	([bd.w,a4,d5.l],od.w)		;nnnn100100100010
	eatest	([bd.w,a4,d5.l*2],od.w)		;nnnn101100100010
	eatest	([bd.w,a4,d5.l*4],od.w)		;nnnn110100100010
	eatest	([bd.w,a4,d5.l*8],od.w)		;nnnn111100100010
	eatest	([bd.w,a4,zd5.l],od.w)		;nnnn100101100010
	eatest	([bd.w,a4,zd5.l*2],od.w)	;nnnn101101100010
	eatest	([bd.w,a4,zd5.l*4],od.w)	;nnnn110101100010
	eatest	([bd.w,a4,zd5.l*8],od.w)	;nnnn111101100010
	eatest	([bd.w,a4],d5.w,od.w)		;nnnn000100100110
	eatest	([bd.w,a4],d5.w*2,od.w)		;nnnn001100100110
	eatest	([bd.w,a4],d5.w*4,od.w)		;nnnn010100100110
	eatest	([bd.w,a4],d5.w*8,od.w)		;nnnn011100100110
	eatest	([bd.w,a4],zd5.w,od.w)		;nnnn000101100110
	eatest	([bd.w,a4],zd5.w*2,od.w)	;nnnn001101100110
	eatest	([bd.w,a4],zd5.w*4,od.w)	;nnnn010101100110
	eatest	([bd.w,a4],zd5.w*8,od.w)	;nnnn011101100110
	eatest	([bd.w,a4],d5.l,od.w)		;nnnn100100100110
	eatest	([bd.w,a4],d5.l*2,od.w)		;nnnn101100100110
	eatest	([bd.w,a4],d5.l*4,od.w)		;nnnn110100100110
	eatest	([bd.w,a4],d5.l*8,od.w)		;nnnn111100100110
	eatest	([bd.w,a4],zd5.l,od.w)		;nnnn100101100110
	eatest	([bd.w,a4],zd5.l*2,od.w)	;nnnn101101100110
	eatest	([bd.w,a4],zd5.l*4,od.w)	;nnnn110101100110
	eatest	([bd.w,a4],zd5.l*8,od.w)	;nnnn111101100110
    .endm
    .irp od,$ffff7ffc,$00008000
	eatest	([bd.w,a4,d5.w],od.l)		;nnnn000100100011
	eatest	([bd.w,a4,d5.w*2],od.l)		;nnnn001100100011
	eatest	([bd.w,a4,d5.w*4],od.l)		;nnnn010100100011
	eatest	([bd.w,a4,d5.w*8],od.l)		;nnnn011100100011
	eatest	([bd.w,a4,zd5.w],od.l)		;nnnn000101100011
	eatest	([bd.w,a4,zd5.w*2],od.l)	;nnnn001101100011
	eatest	([bd.w,a4,zd5.w*4],od.l)	;nnnn010101100011
	eatest	([bd.w,a4,zd5.w*8],od.l)	;nnnn011101100011
	eatest	([bd.w,a4,d5.l],od.l)		;nnnn100100100011
	eatest	([bd.w,a4,d5.l*2],od.l)		;nnnn101100100011
	eatest	([bd.w,a4,d5.l*4],od.l)		;nnnn110100100011
	eatest	([bd.w,a4,d5.l*8],od.l)		;nnnn111100100011
	eatest	([bd.w,a4,zd5.l],od.l)		;nnnn100101100011
	eatest	([bd.w,a4,zd5.l*2],od.l)	;nnnn101101100011
	eatest	([bd.w,a4,zd5.l*4],od.l)	;nnnn110101100011
	eatest	([bd.w,a4,zd5.l*8],od.l)	;nnnn111101100011
	eatest	([bd.w,a4],d5.w,od.l)		;nnnn000100100111
	eatest	([bd.w,a4],d5.w*2,od.l)		;nnnn001100100111
	eatest	([bd.w,a4],d5.w*4,od.l)		;nnnn010100100111
	eatest	([bd.w,a4],d5.w*8,od.l)		;nnnn011100100111
	eatest	([bd.w,a4],zd5.w,od.l)		;nnnn000101100111
	eatest	([bd.w,a4],zd5.w*2,od.l)	;nnnn001101100111
	eatest	([bd.w,a4],zd5.w*4,od.l)	;nnnn010101100111
	eatest	([bd.w,a4],zd5.w*8,od.l)	;nnnn011101100111
	eatest	([bd.w,a4],d5.l,od.l)		;nnnn100100100111
	eatest	([bd.w,a4],d5.l*2,od.l)		;nnnn101100100111
	eatest	([bd.w,a4],d5.l*4,od.l)		;nnnn110100100111
	eatest	([bd.w,a4],d5.l*8,od.l)		;nnnn111100100111
	eatest	([bd.w,a4],zd5.l,od.l)		;nnnn100101100111
	eatest	([bd.w,a4],zd5.l*2,od.l)	;nnnn101101100111
	eatest	([bd.w,a4],zd5.l*4,od.l)	;nnnn110101100111
	eatest	([bd.w,a4],zd5.l*8,od.l)	;nnnn111101100111
    .endm
  .endm
  .irp bd,$ffff7ffc,$00008000
	eatest	(bd.l,a4,d5.w)			;nnnn000100110000
	eatest	(bd.l,a4,d5.w*2)		;nnnn001100110000
	eatest	(bd.l,a4,d5.w*4)		;nnnn010100110000
	eatest	(bd.l,a4,d5.w*8)		;nnnn011100110000
	eatest	(bd.l,a4,zd5.w)			;nnnn000101110000
	eatest	(bd.l,a4,zd5.w*2)		;nnnn001101110000
	eatest	(bd.l,a4,zd5.w*4)		;nnnn010101110000
	eatest	(bd.l,a4,zd5.w*8)		;nnnn011101110000
	eatest	(bd.l,a4,d5.l)			;nnnn100100110000
	eatest	(bd.l,a4,d5.l*2)		;nnnn101100110000
	eatest	(bd.l,a4,d5.l*4)		;nnnn110100110000
	eatest	(bd.l,a4,d5.l*8)		;nnnn111100110000
	eatest	(bd.l,a4,zd5.l)			;nnnn100101110000
	eatest	(bd.l,a4,zd5.l*2)		;nnnn101101110000
	eatest	(bd.l,a4,zd5.l*4)		;nnnn110101110000
	eatest	(bd.l,a4,zd5.l*8)		;nnnn111101110000
  .endm
  .irp bd,$ffff7ffc,$00008000
	eatest	([bd.l,a4,d5.w])		;nnnn000100110001
	eatest	([bd.l,a4,d5.w*2])		;nnnn001100110001
	eatest	([bd.l,a4,d5.w*4])		;nnnn010100110001
	eatest	([bd.l,a4,d5.w*8])		;nnnn011100110001
	eatest	([bd.l,a4,zd5.w])		;nnnn000101110001
	eatest	([bd.l,a4,zd5.w*2])		;nnnn001101110001
	eatest	([bd.l,a4,zd5.w*4])		;nnnn010101110001
	eatest	([bd.l,a4,zd5.w*8])		;nnnn011101110001
	eatest	([bd.l,a4,d5.l])		;nnnn100100110001
	eatest	([bd.l,a4,d5.l*2])		;nnnn101100110001
	eatest	([bd.l,a4,d5.l*4])		;nnnn110100110001
	eatest	([bd.l,a4,d5.l*8])		;nnnn111100110001
	eatest	([bd.l,a4,zd5.l])		;nnnn100101110001
	eatest	([bd.l,a4,zd5.l*2])		;nnnn101101110001
	eatest	([bd.l,a4,zd5.l*4])		;nnnn110101110001
	eatest	([bd.l,a4,zd5.l*8])		;nnnn111101110001
	eatest	([bd.l,a4],d5.w)		;nnnn000100110101
	eatest	([bd.l,a4],d5.w*2)		;nnnn001100110101
	eatest	([bd.l,a4],d5.w*4)		;nnnn010100110101
	eatest	([bd.l,a4],d5.w*8)		;nnnn011100110101
	eatest	([bd.l,a4],zd5.w)		;nnnn000101110101
	eatest	([bd.l,a4],zd5.w*2)		;nnnn001101110101
	eatest	([bd.l,a4],zd5.w*4)		;nnnn010101110101
	eatest	([bd.l,a4],zd5.w*8)		;nnnn011101110101
	eatest	([bd.l,a4],d5.l)		;nnnn100100110101
	eatest	([bd.l,a4],d5.l*2)		;nnnn101100110101
	eatest	([bd.l,a4],d5.l*4)		;nnnn110100110101
	eatest	([bd.l,a4],d5.l*8)		;nnnn111100110101
	eatest	([bd.l,a4],zd5.l)		;nnnn100101110101
	eatest	([bd.l,a4],zd5.l*2)		;nnnn101101110101
	eatest	([bd.l,a4],zd5.l*4)		;nnnn110101110101
	eatest	([bd.l,a4],zd5.l*8)		;nnnn111101110101
    .irp od,$ffff8000,$00007ffc
	eatest	([bd.l,a4,d5.w],od.w)		;nnnn000100110010
	eatest	([bd.l,a4,d5.w*2],od.w)		;nnnn001100110010
	eatest	([bd.l,a4,d5.w*4],od.w)		;nnnn010100110010
	eatest	([bd.l,a4,d5.w*8],od.w)		;nnnn011100110010
	eatest	([bd.l,a4,zd5.w],od.w)		;nnnn000101110010
	eatest	([bd.l,a4,zd5.w*2],od.w)	;nnnn001101110010
	eatest	([bd.l,a4,zd5.w*4],od.w)	;nnnn010101110010
	eatest	([bd.l,a4,zd5.w*8],od.w)	;nnnn011101110010
	eatest	([bd.l,a4,d5.l],od.w)		;nnnn100100110010
	eatest	([bd.l,a4,d5.l*2],od.w)		;nnnn101100110010
	eatest	([bd.l,a4,d5.l*4],od.w)		;nnnn110100110010
	eatest	([bd.l,a4,d5.l*8],od.w)		;nnnn111100110010
	eatest	([bd.l,a4,zd5.l],od.w)		;nnnn100101110010
	eatest	([bd.l,a4,zd5.l*2],od.w)	;nnnn101101110010
	eatest	([bd.l,a4,zd5.l*4],od.w)	;nnnn110101110010
	eatest	([bd.l,a4,zd5.l*8],od.w)	;nnnn111101110010
	eatest	([bd.l,a4],d5.w,od.w)		;nnnn000100110110
	eatest	([bd.l,a4],d5.w*2,od.w)		;nnnn001100110110
	eatest	([bd.l,a4],d5.w*4,od.w)		;nnnn010100110110
	eatest	([bd.l,a4],d5.w*8,od.w)		;nnnn011100110110
	eatest	([bd.l,a4],zd5.w,od.w)		;nnnn000101110110
	eatest	([bd.l,a4],zd5.w*2,od.w)	;nnnn001101110110
	eatest	([bd.l,a4],zd5.w*4,od.w)	;nnnn010101110110
	eatest	([bd.l,a4],zd5.w*8,od.w)	;nnnn011101110110
	eatest	([bd.l,a4],d5.l,od.w)		;nnnn100100110110
	eatest	([bd.l,a4],d5.l*2,od.w)		;nnnn101100110110
	eatest	([bd.l,a4],d5.l*4,od.w)		;nnnn110100110110
	eatest	([bd.l,a4],d5.l*8,od.w)		;nnnn111100110110
	eatest	([bd.l,a4],zd5.l,od.w)		;nnnn100101110110
	eatest	([bd.l,a4],zd5.l*2,od.w)	;nnnn101101110110
	eatest	([bd.l,a4],zd5.l*4,od.w)	;nnnn110101110110
	eatest	([bd.l,a4],zd5.l*8,od.w)	;nnnn111101110110
    .endm
    .irp od,$ffff7ffc,$00008000
	eatest	([bd.l,a4,d5.w],od.l)		;nnnn000100110011
	eatest	([bd.l,a4,d5.w*2],od.l)		;nnnn001100110011
	eatest	([bd.l,a4,d5.w*4],od.l)		;nnnn010100110011
	eatest	([bd.l,a4,d5.w*8],od.l)		;nnnn011100110011
	eatest	([bd.l,a4,zd5.w],od.l)		;nnnn000101110011
	eatest	([bd.l,a4,zd5.w*2],od.l)	;nnnn001101110011
	eatest	([bd.l,a4,zd5.w*4],od.l)	;nnnn010101110011
	eatest	([bd.l,a4,zd5.w*8],od.l)	;nnnn011101110011
	eatest	([bd.l,a4,d5.l],od.l)		;nnnn100100110011
	eatest	([bd.l,a4,d5.l*2],od.l)		;nnnn101100110011
	eatest	([bd.l,a4,d5.l*4],od.l)		;nnnn110100110011
	eatest	([bd.l,a4,d5.l*8],od.l)		;nnnn111100110011
	eatest	([bd.l,a4,zd5.l],od.l)		;nnnn100101110011
	eatest	([bd.l,a4,zd5.l*2],od.l)	;nnnn101101110011
	eatest	([bd.l,a4,zd5.l*4],od.l)	;nnnn110101110011
	eatest	([bd.l,a4,zd5.l*8],od.l)	;nnnn111101110011
	eatest	([bd.l,a4],d5.w,od.l)		;nnnn000100110111
	eatest	([bd.l,a4],d5.w*2,od.l)		;nnnn001100110111
	eatest	([bd.l,a4],d5.w*4,od.l)		;nnnn010100110111
	eatest	([bd.l,a4],d5.w*8,od.l)		;nnnn011100110111
	eatest	([bd.l,a4],zd5.w,od.l)		;nnnn000101110111
	eatest	([bd.l,a4],zd5.w*2,od.l)	;nnnn001101110111
	eatest	([bd.l,a4],zd5.w*4,od.l)	;nnnn010101110111
	eatest	([bd.l,a4],zd5.w*8,od.l)	;nnnn011101110111
	eatest	([bd.l,a4],d5.l,od.l)		;nnnn100100110111
	eatest	([bd.l,a4],d5.l*2,od.l)		;nnnn101100110111
	eatest	([bd.l,a4],d5.l*4,od.l)		;nnnn110100110111
	eatest	([bd.l,a4],d5.l*8,od.l)		;nnnn111100110111
	eatest	([bd.l,a4],zd5.l,od.l)		;nnnn100101110111
	eatest	([bd.l,a4],zd5.l*2,od.l)	;nnnn101101110111
	eatest	([bd.l,a4],zd5.l*4,od.l)	;nnnn110101110111
	eatest	([bd.l,a4],zd5.l*8,od.l)	;nnnn111101110111
    .endm
  .endm
	zeatest	(za4,d5.w)			;nnnn000110010000
	zeatest	(za4,d5.w*2)			;nnnn001110010000
	zeatest	(za4,d5.w*4)			;nnnn010110010000
	zeatest	(za4,d5.w*8)			;nnnn011110010000
	zeatest	(za4,zd5.w)			;nnnn000111010000
	zeatest	(za4,zd5.w*2)			;nnnn001111010000
	zeatest	(za4,zd5.w*4)			;nnnn010111010000
	zeatest	(za4,zd5.w*8)			;nnnn011111010000
	zeatest	(za4,d5.l)			;nnnn100110010000
	zeatest	(za4,d5.l*2)			;nnnn101110010000
	zeatest	(za4,d5.l*4)			;nnnn110110010000
	zeatest	(za4,d5.l*8)			;nnnn111110010000
	zeatest	(za4,zd5.l)			;nnnn100111010000
	zeatest	(za4,zd5.l*2)			;nnnn101111010000
	zeatest	(za4,zd5.l*4)			;nnnn110111010000
	zeatest	(za4,zd5.l*8)			;nnnn111111010000
;	eatest	([za4,d5.w])			;nnnn000110010001
;	eatest	([za4,d5.w*2])			;nnnn001110010001
;	eatest	([za4,d5.w*4])			;nnnn010110010001
;	eatest	([za4,d5.w*8])			;nnnn011110010001
;	eatest	([za4,zd5.w])			;nnnn000111010001
;	eatest	([za4,zd5.w*2])			;nnnn001111010001
;	eatest	([za4,zd5.w*4])			;nnnn010111010001
;	eatest	([za4,zd5.w*8])			;nnnn011111010001
;	eatest	([za4,d5.l])			;nnnn100110010001
;	eatest	([za4,d5.l*2])			;nnnn101110010001
;	eatest	([za4,d5.l*4])			;nnnn110110010001
;	eatest	([za4,d5.l*8])			;nnnn111110010001
;	eatest	([za4,zd5.l])			;nnnn100111010001
;	eatest	([za4,zd5.l*2])			;nnnn101111010001
;	eatest	([za4,zd5.l*4])			;nnnn110111010001
;	eatest	([za4,zd5.l*8])			;nnnn111111010001
;	eatest	([za4],d5.w)			;nnnn000110010101
;	eatest	([za4],d5.w*2)			;nnnn001110010101
;	eatest	([za4],d5.w*4)			;nnnn010110010101
;	eatest	([za4],d5.w*8)			;nnnn011110010101
;	eatest	([za4],zd5.w)			;nnnn000111010101
;	eatest	([za4],zd5.w*2)			;nnnn001111010101
;	eatest	([za4],zd5.w*4)			;nnnn010111010101
;	eatest	([za4],zd5.w*8)			;nnnn011111010101
;	eatest	([za4],d5.l)			;nnnn100110010101
;	eatest	([za4],d5.l*2)			;nnnn101110010101
;	eatest	([za4],d5.l*4)			;nnnn110110010101
;	eatest	([za4],d5.l*8)			;nnnn111110010101
;	eatest	([za4],zd5.l)			;nnnn100111010101
;	eatest	([za4],zd5.l*2)			;nnnn101111010101
;	eatest	([za4],zd5.l*4)			;nnnn110111010101
;	eatest	([za4],zd5.l*8)			;nnnn111111010101
  .irp od,$ffff8000,$00007ffc
;	eatest	([za4,d5.w],od.w)		;nnnn000110010010
;	eatest	([za4,d5.w*2],od.w)		;nnnn001110010010
;	eatest	([za4,d5.w*4],od.w)		;nnnn010110010010
;	eatest	([za4,d5.w*8],od.w)		;nnnn011110010010
;	eatest	([za4,zd5.w],od.w)		;nnnn000111010010
;	eatest	([za4,zd5.w*2],od.w)		;nnnn001111010010
;	eatest	([za4,zd5.w*4],od.w)		;nnnn010111010010
;	eatest	([za4,zd5.w*8],od.w)		;nnnn011111010010
;	eatest	([za4,d5.l],od.w)		;nnnn100110010010
;	eatest	([za4,d5.l*2],od.w)		;nnnn101110010010
;	eatest	([za4,d5.l*4],od.w)		;nnnn110110010010
;	eatest	([za4,d5.l*8],od.w)		;nnnn111110010010
;	eatest	([za4,zd5.l],od.w)		;nnnn100111010010
;	eatest	([za4,zd5.l*2],od.w)		;nnnn101111010010
;	eatest	([za4,zd5.l*4],od.w)		;nnnn110111010010
;	eatest	([za4,zd5.l*8],od.w)		;nnnn111111010010
;	eatest	([za4],d5.w,od.w)		;nnnn000110010110
;	eatest	([za4],d5.w*2,od.w)		;nnnn001110010110
;	eatest	([za4],d5.w*4,od.w)		;nnnn010110010110
;	eatest	([za4],d5.w*8,od.w)		;nnnn011110010110
;	eatest	([za4],zd5.w,od.w)		;nnnn000111010110
;	eatest	([za4],zd5.w*2,od.w)		;nnnn001111010110
;	eatest	([za4],zd5.w*4,od.w)		;nnnn010111010110
;	eatest	([za4],zd5.w*8,od.w)		;nnnn011111010110
;	eatest	([za4],d5.l,od.w)		;nnnn100110010110
;	eatest	([za4],d5.l*2,od.w)		;nnnn101110010110
;	eatest	([za4],d5.l*4,od.w)		;nnnn110110010110
;	eatest	([za4],d5.l*8,od.w)		;nnnn111110010110
;	eatest	([za4],zd5.l,od.w)		;nnnn100111010110
;	eatest	([za4],zd5.l*2,od.w)		;nnnn101111010110
;	eatest	([za4],zd5.l*4,od.w)		;nnnn110111010110
;	eatest	([za4],zd5.l*8,od.w)		;nnnn111111010110
  .endm
  .irp od,$ffff7ffc,$00008000
;	eatest	([za4,d5.w],od.l)		;nnnn000110010011
;	eatest	([za4,d5.w*2],od.l)		;nnnn001110010011
;	eatest	([za4,d5.w*4],od.l)		;nnnn010110010011
;	eatest	([za4,d5.w*8],od.l)		;nnnn011110010011
;	eatest	([za4,zd5.w],od.l)		;nnnn000111010011
;	eatest	([za4,zd5.w*2],od.l)		;nnnn001111010011
;	eatest	([za4,zd5.w*4],od.l)		;nnnn010111010011
;	eatest	([za4,zd5.w*8],od.l)		;nnnn011111010011
;	eatest	([za4,d5.l],od.l)		;nnnn100110010011
;	eatest	([za4,d5.l*2],od.l)		;nnnn101110010011
;	eatest	([za4,d5.l*4],od.l)		;nnnn110110010011
;	eatest	([za4,d5.l*8],od.l)		;nnnn111110010011
;	eatest	([za4,zd5.l],od.l)		;nnnn100111010011
;	eatest	([za4,zd5.l*2],od.l)		;nnnn101111010011
;	eatest	([za4,zd5.l*4],od.l)		;nnnn110111010011
;	eatest	([za4,zd5.l*8],od.l)		;nnnn111111010011
;	eatest	([za4],d5.w,od.l)		;nnnn000110010111
;	eatest	([za4],d5.w*2,od.l)		;nnnn001110010111
;	eatest	([za4],d5.w*4,od.l)		;nnnn010110010111
;	eatest	([za4],d5.w*8,od.l)		;nnnn011110010111
;	eatest	([za4],zd5.w,od.l)		;nnnn000111010111
;	eatest	([za4],zd5.w*2,od.l)		;nnnn001111010111
;	eatest	([za4],zd5.w*4,od.l)		;nnnn010111010111
;	eatest	([za4],zd5.w*8,od.l)		;nnnn011111010111
;	eatest	([za4],d5.l,od.l)		;nnnn100110010111
;	eatest	([za4],d5.l*2,od.l)		;nnnn101110010111
;	eatest	([za4],d5.l*4,od.l)		;nnnn110110010111
;	eatest	([za4],d5.l*8,od.l)		;nnnn111110010111
;	eatest	([za4],zd5.l,od.l)		;nnnn100111010111
;	eatest	([za4],zd5.l*2,od.l)		;nnnn101111010111
;	eatest	([za4],zd5.l*4,od.l)		;nnnn110111010111
;	eatest	([za4],zd5.l*8,od.l)		;nnnn111111010111
  .endm
  .irp bd,$ffff8000,$00007ffc
	zeatest	(bd.w,za4,d5.w)			;nnnn000110100000
	zeatest	(bd.w,za4,d5.w*2)		;nnnn001110100000
	zeatest	(bd.w,za4,d5.w*4)		;nnnn010110100000
	zeatest	(bd.w,za4,d5.w*8)		;nnnn011110100000
	zeatest	(bd.w,za4,zd5.w)		;nnnn000111100000
	zeatest	(bd.w,za4,zd5.w*2)		;nnnn001111100000
	zeatest	(bd.w,za4,zd5.w*4)		;nnnn010111100000
	zeatest	(bd.w,za4,zd5.w*8)		;nnnn011111100000
	zeatest	(bd.w,za4,d5.l)			;nnnn100110100000
	zeatest	(bd.w,za4,d5.l*2)		;nnnn101110100000
	zeatest	(bd.w,za4,d5.l*4)		;nnnn110110100000
	zeatest	(bd.w,za4,d5.l*8)		;nnnn111110100000
	zeatest	(bd.w,za4,zd5.l)		;nnnn100111100000
	zeatest	(bd.w,za4,zd5.l*2)		;nnnn101111100000
	zeatest	(bd.w,za4,zd5.l*4)		;nnnn110111100000
	zeatest	(bd.w,za4,zd5.l*8)		;nnnn111111100000
  .endm
  .irp bd,$ffff8000,$00007ffc
;	eatest	([bd.w,za4,d5.w])		;nnnn000110100001
;	eatest	([bd.w,za4,d5.w*2])		;nnnn001110100001
;	eatest	([bd.w,za4,d5.w*4])		;nnnn010110100001
;	eatest	([bd.w,za4,d5.w*8])		;nnnn011110100001
;	eatest	([bd.w,za4,zd5.w])		;nnnn000111100001
;	eatest	([bd.w,za4,zd5.w*2])		;nnnn001111100001
;	eatest	([bd.w,za4,zd5.w*4])		;nnnn010111100001
;	eatest	([bd.w,za4,zd5.w*8])		;nnnn011111100001
;	eatest	([bd.w,za4,d5.l])		;nnnn100110100001
;	eatest	([bd.w,za4,d5.l*2])		;nnnn101110100001
;	eatest	([bd.w,za4,d5.l*4])		;nnnn110110100001
;	eatest	([bd.w,za4,d5.l*8])		;nnnn111110100001
;	eatest	([bd.w,za4,zd5.l])		;nnnn100111100001
;	eatest	([bd.w,za4,zd5.l*2])		;nnnn101111100001
;	eatest	([bd.w,za4,zd5.l*4])		;nnnn110111100001
;	eatest	([bd.w,za4,zd5.l*8])		;nnnn111111100001
;	eatest	([bd.w,za4],d5.w)		;nnnn000110100101
;	eatest	([bd.w,za4],d5.w*2)		;nnnn001110100101
;	eatest	([bd.w,za4],d5.w*4)		;nnnn010110100101
;	eatest	([bd.w,za4],d5.w*8)		;nnnn011110100101
;	eatest	([bd.w,za4],zd5.w)		;nnnn000111100101
;	eatest	([bd.w,za4],zd5.w*2)		;nnnn001111100101
;	eatest	([bd.w,za4],zd5.w*4)		;nnnn010111100101
;	eatest	([bd.w,za4],zd5.w*8)		;nnnn011111100101
;	eatest	([bd.w,za4],d5.l)		;nnnn100110100101
;	eatest	([bd.w,za4],d5.l*2)		;nnnn101110100101
;	eatest	([bd.w,za4],d5.l*4)		;nnnn110110100101
;	eatest	([bd.w,za4],d5.l*8)		;nnnn111110100101
;	eatest	([bd.w,za4],zd5.l)		;nnnn100111100101
;	eatest	([bd.w,za4],zd5.l*2)		;nnnn101111100101
;	eatest	([bd.w,za4],zd5.l*4)		;nnnn110111100101
;	eatest	([bd.w,za4],zd5.l*8)		;nnnn111111100101
    .irp od,$ffff8000,$00007ffc
;	eatest	([bd.w,za4,d5.w],od.w)		;nnnn000110100010
;	eatest	([bd.w,za4,d5.w*2],od.w)	;nnnn001110100010
;	eatest	([bd.w,za4,d5.w*4],od.w)	;nnnn010110100010
;	eatest	([bd.w,za4,d5.w*8],od.w)	;nnnn011110100010
;	eatest	([bd.w,za4,zd5.w],od.w)		;nnnn000111100010
;	eatest	([bd.w,za4,zd5.w*2],od.w)	;nnnn001111100010
;	eatest	([bd.w,za4,zd5.w*4],od.w)	;nnnn010111100010
;	eatest	([bd.w,za4,zd5.w*8],od.w)	;nnnn011111100010
;	eatest	([bd.w,za4,d5.l],od.w)		;nnnn100110100010
;	eatest	([bd.w,za4,d5.l*2],od.w)	;nnnn101110100010
;	eatest	([bd.w,za4,d5.l*4],od.w)	;nnnn110110100010
;	eatest	([bd.w,za4,d5.l*8],od.w)	;nnnn111110100010
;	eatest	([bd.w,za4,zd5.l],od.w)		;nnnn100111100010
;	eatest	([bd.w,za4,zd5.l*2],od.w)	;nnnn101111100010
;	eatest	([bd.w,za4,zd5.l*4],od.w)	;nnnn110111100010
;	eatest	([bd.w,za4,zd5.l*8],od.w)	;nnnn111111100010
;	eatest	([bd.w,za4],d5.w,od.w)		;nnnn000110100110
;	eatest	([bd.w,za4],d5.w*2,od.w)	;nnnn001110100110
;	eatest	([bd.w,za4],d5.w*4,od.w)	;nnnn010110100110
;	eatest	([bd.w,za4],d5.w*8,od.w)	;nnnn011110100110
;	eatest	([bd.w,za4],zd5.w,od.w)		;nnnn000111100110
;	eatest	([bd.w,za4],zd5.w*2,od.w)	;nnnn001111100110
;	eatest	([bd.w,za4],zd5.w*4,od.w)	;nnnn010111100110
;	eatest	([bd.w,za4],zd5.w*8,od.w)	;nnnn011111100110
;	eatest	([bd.w,za4],d5.l,od.w)		;nnnn100110100110
;	eatest	([bd.w,za4],d5.l*2,od.w)	;nnnn101110100110
;	eatest	([bd.w,za4],d5.l*4,od.w)	;nnnn110110100110
;	eatest	([bd.w,za4],d5.l*8,od.w)	;nnnn111110100110
;	eatest	([bd.w,za4],zd5.l,od.w)		;nnnn100111100110
;	eatest	([bd.w,za4],zd5.l*2,od.w)	;nnnn101111100110
;	eatest	([bd.w,za4],zd5.l*4,od.w)	;nnnn110111100110
;	eatest	([bd.w,za4],zd5.l*8,od.w)	;nnnn111111100110
    .endm
    .irp od,$ffff7ffc,$00008000
;	eatest	([bd.w,za4,d5.w],od.l)		;nnnn000110100011
;	eatest	([bd.w,za4,d5.w*2],od.l)	;nnnn001110100011
;	eatest	([bd.w,za4,d5.w*4],od.l)	;nnnn010110100011
;	eatest	([bd.w,za4,d5.w*8],od.l)	;nnnn011110100011
;	eatest	([bd.w,za4,zd5.w],od.l)		;nnnn000111100011
;	eatest	([bd.w,za4,zd5.w*2],od.l)	;nnnn001111100011
;	eatest	([bd.w,za4,zd5.w*4],od.l)	;nnnn010111100011
;	eatest	([bd.w,za4,zd5.w*8],od.l)	;nnnn011111100011
;	eatest	([bd.w,za4,d5.l],od.l)		;nnnn100110100011
;	eatest	([bd.w,za4,d5.l*2],od.l)	;nnnn101110100011
;	eatest	([bd.w,za4,d5.l*4],od.l)	;nnnn110110100011
;	eatest	([bd.w,za4,d5.l*8],od.l)	;nnnn111110100011
;	eatest	([bd.w,za4,zd5.l],od.l)		;nnnn100111100011
;	eatest	([bd.w,za4,zd5.l*2],od.l)	;nnnn101111100011
;	eatest	([bd.w,za4,zd5.l*4],od.l)	;nnnn110111100011
;	eatest	([bd.w,za4,zd5.l*8],od.l)	;nnnn111111100011
;	eatest	([bd.w,za4],d5.w,od.l)		;nnnn000110100111
;	eatest	([bd.w,za4],d5.w*2,od.l)	;nnnn001110100111
;	eatest	([bd.w,za4],d5.w*4,od.l)	;nnnn010110100111
;	eatest	([bd.w,za4],d5.w*8,od.l)	;nnnn011110100111
;	eatest	([bd.w,za4],zd5.w,od.l)		;nnnn000111100111
;	eatest	([bd.w,za4],zd5.w*2,od.l)	;nnnn001111100111
;	eatest	([bd.w,za4],zd5.w*4,od.l)	;nnnn010111100111
;	eatest	([bd.w,za4],zd5.w*8,od.l)	;nnnn011111100111
;	eatest	([bd.w,za4],d5.l,od.l)		;nnnn100110100111
;	eatest	([bd.w,za4],d5.l*2,od.l)	;nnnn101110100111
;	eatest	([bd.w,za4],d5.l*4,od.l)	;nnnn110110100111
;	eatest	([bd.w,za4],d5.l*8,od.l)	;nnnn111110100111
;	eatest	([bd.w,za4],zd5.l,od.l)		;nnnn100111100111
;	eatest	([bd.w,za4],zd5.l*2,od.l)	;nnnn101111100111
;	eatest	([bd.w,za4],zd5.l*4,od.l)	;nnnn110111100111
;	eatest	([bd.w,za4],zd5.l*8,od.l)	;nnnn111111100111
    .endm
  .endm
  .irp bd,$ffff8000,$00007ffc
	zeatest	(bd.l,za4,d5.w)			;nnnn000110110000
	zeatest	(bd.l,za4,d5.w*2)		;nnnn001110110000
	zeatest	(bd.l,za4,d5.w*4)		;nnnn010110110000
	zeatest	(bd.l,za4,d5.w*8)		;nnnn011110110000
	zeatest	(bd.l,za4,zd5.w)		;nnnn000111110000
	zeatest	(bd.l,za4,zd5.w*2)		;nnnn001111110000
	zeatest	(bd.l,za4,zd5.w*4)		;nnnn010111110000
	zeatest	(bd.l,za4,zd5.w*8)		;nnnn011111110000
	zeatest	(bd.l,za4,d5.l)			;nnnn100110110000
	zeatest	(bd.l,za4,d5.l*2)		;nnnn101110110000
	zeatest	(bd.l,za4,d5.l*4)		;nnnn110110110000
	zeatest	(bd.l,za4,d5.l*8)		;nnnn111110110000
	zeatest	(bd.l,za4,zd5.l)		;nnnn100111110000
	zeatest	(bd.l,za4,zd5.l*2)		;nnnn101111110000
	zeatest	(bd.l,za4,zd5.l*4)		;nnnn110111110000
	zeatest	(bd.l,za4,zd5.l*8)		;nnnn111111110000
  .endm
  .irp bd,work_area+4096
	eatest	([bd.l,za4,d5.w])		;nnnn000110110001
	eatest	([bd.l,za4,d5.w*2])		;nnnn001110110001
	eatest	([bd.l,za4,d5.w*4])		;nnnn010110110001
	eatest	([bd.l,za4,d5.w*8])		;nnnn011110110001
	eatest	([bd.l,za4,zd5.w])		;nnnn000111110001
	eatest	([bd.l,za4,zd5.w*2])		;nnnn001111110001
	eatest	([bd.l,za4,zd5.w*4])		;nnnn010111110001
	eatest	([bd.l,za4,zd5.w*8])		;nnnn011111110001
	eatest	([bd.l,za4,d5.l])		;nnnn100110110001
	eatest	([bd.l,za4,d5.l*2])		;nnnn101110110001
	eatest	([bd.l,za4,d5.l*4])		;nnnn110110110001
	eatest	([bd.l,za4,d5.l*8])		;nnnn111110110001
	eatest	([bd.l,za4,zd5.l])		;nnnn100111110001
	eatest	([bd.l,za4,zd5.l*2])		;nnnn101111110001
	eatest	([bd.l,za4,zd5.l*4])		;nnnn110111110001
	eatest	([bd.l,za4,zd5.l*8])		;nnnn111111110001
	eatest	([bd.l,za4],d5.w)		;nnnn000110110101
	eatest	([bd.l,za4],d5.w*2)		;nnnn001110110101
	eatest	([bd.l,za4],d5.w*4)		;nnnn010110110101
	eatest	([bd.l,za4],d5.w*8)		;nnnn011110110101
	eatest	([bd.l,za4],zd5.w)		;nnnn000111110101
	eatest	([bd.l,za4],zd5.w*2)		;nnnn001111110101
	eatest	([bd.l,za4],zd5.w*4)		;nnnn010111110101
	eatest	([bd.l,za4],zd5.w*8)		;nnnn011111110101
	eatest	([bd.l,za4],d5.l)		;nnnn100110110101
	eatest	([bd.l,za4],d5.l*2)		;nnnn101110110101
	eatest	([bd.l,za4],d5.l*4)		;nnnn110110110101
	eatest	([bd.l,za4],d5.l*8)		;nnnn111110110101
	eatest	([bd.l,za4],zd5.l)		;nnnn100111110101
	eatest	([bd.l,za4],zd5.l*2)		;nnnn101111110101
	eatest	([bd.l,za4],zd5.l*4)		;nnnn110111110101
	eatest	([bd.l,za4],zd5.l*8)		;nnnn111111110101
    .irp od,$ffff8000,$00007ffc
	eatest	([bd.l,za4,d5.w],od.w)		;nnnn000110110010
	eatest	([bd.l,za4,d5.w*2],od.w)	;nnnn001110110010
	eatest	([bd.l,za4,d5.w*4],od.w)	;nnnn010110110010
	eatest	([bd.l,za4,d5.w*8],od.w)	;nnnn011110110010
	eatest	([bd.l,za4,zd5.w],od.w)		;nnnn000111110010
	eatest	([bd.l,za4,zd5.w*2],od.w)	;nnnn001111110010
	eatest	([bd.l,za4,zd5.w*4],od.w)	;nnnn010111110010
	eatest	([bd.l,za4,zd5.w*8],od.w)	;nnnn011111110010
	eatest	([bd.l,za4,d5.l],od.w)		;nnnn100110110010
	eatest	([bd.l,za4,d5.l*2],od.w)	;nnnn101110110010
	eatest	([bd.l,za4,d5.l*4],od.w)	;nnnn110110110010
	eatest	([bd.l,za4,d5.l*8],od.w)	;nnnn111110110010
	eatest	([bd.l,za4,zd5.l],od.w)		;nnnn100111110010
	eatest	([bd.l,za4,zd5.l*2],od.w)	;nnnn101111110010
	eatest	([bd.l,za4,zd5.l*4],od.w)	;nnnn110111110010
	eatest	([bd.l,za4,zd5.l*8],od.w)	;nnnn111111110010
	eatest	([bd.l,za4],d5.w,od.w)		;nnnn000110110110
	eatest	([bd.l,za4],d5.w*2,od.w)	;nnnn001110110110
	eatest	([bd.l,za4],d5.w*4,od.w)	;nnnn010110110110
	eatest	([bd.l,za4],d5.w*8,od.w)	;nnnn011110110110
	eatest	([bd.l,za4],zd5.w,od.w)		;nnnn000111110110
	eatest	([bd.l,za4],zd5.w*2,od.w)	;nnnn001111110110
	eatest	([bd.l,za4],zd5.w*4,od.w)	;nnnn010111110110
	eatest	([bd.l,za4],zd5.w*8,od.w)	;nnnn011111110110
	eatest	([bd.l,za4],d5.l,od.w)		;nnnn100110110110
	eatest	([bd.l,za4],d5.l*2,od.w)	;nnnn101110110110
	eatest	([bd.l,za4],d5.l*4,od.w)	;nnnn110110110110
	eatest	([bd.l,za4],d5.l*8,od.w)	;nnnn111110110110
	eatest	([bd.l,za4],zd5.l,od.w)		;nnnn100111110110
	eatest	([bd.l,za4],zd5.l*2,od.w)	;nnnn101111110110
	eatest	([bd.l,za4],zd5.l*4,od.w)	;nnnn110111110110
	eatest	([bd.l,za4],zd5.l*8,od.w)	;nnnn111111110110
    .endm
    .irp od,$ffff7ffc,$00008000
	eatest	([bd.l,za4,d5.w],od.l)		;nnnn000110110011
	eatest	([bd.l,za4,d5.w*2],od.l)	;nnnn001110110011
	eatest	([bd.l,za4,d5.w*4],od.l)	;nnnn010110110011
	eatest	([bd.l,za4,d5.w*8],od.l)	;nnnn011110110011
	eatest	([bd.l,za4,zd5.w],od.l)		;nnnn000111110011
	eatest	([bd.l,za4,zd5.w*2],od.l)	;nnnn001111110011
	eatest	([bd.l,za4,zd5.w*4],od.l)	;nnnn010111110011
	eatest	([bd.l,za4,zd5.w*8],od.l)	;nnnn011111110011
	eatest	([bd.l,za4,d5.l],od.l)		;nnnn100110110011
	eatest	([bd.l,za4,d5.l*2],od.l)	;nnnn101110110011
	eatest	([bd.l,za4,d5.l*4],od.l)	;nnnn110110110011
	eatest	([bd.l,za4,d5.l*8],od.l)	;nnnn111110110011
	eatest	([bd.l,za4,zd5.l],od.l)		;nnnn100111110011
	eatest	([bd.l,za4,zd5.l*2],od.l)	;nnnn101111110011
	eatest	([bd.l,za4,zd5.l*4],od.l)	;nnnn110111110011
	eatest	([bd.l,za4,zd5.l*8],od.l)	;nnnn111111110011
	eatest	([bd.l,za4],d5.w,od.l)		;nnnn000110110111
	eatest	([bd.l,za4],d5.w*2,od.l)	;nnnn001110110111
	eatest	([bd.l,za4],d5.w*4,od.l)	;nnnn010110110111
	eatest	([bd.l,za4],d5.w*8,od.l)	;nnnn011110110111
	eatest	([bd.l,za4],zd5.w,od.l)		;nnnn000111110111
	eatest	([bd.l,za4],zd5.w*2,od.l)	;nnnn001111110111
	eatest	([bd.l,za4],zd5.w*4,od.l)	;nnnn010111110111
	eatest	([bd.l,za4],zd5.w*8,od.l)	;nnnn011111110111
	eatest	([bd.l,za4],d5.l,od.l)		;nnnn100110110111
	eatest	([bd.l,za4],d5.l*2,od.l)	;nnnn101110110111
	eatest	([bd.l,za4],d5.l*4,od.l)	;nnnn110110110111
	eatest	([bd.l,za4],d5.l*8,od.l)	;nnnn111110110111
	eatest	([bd.l,za4],zd5.l,od.l)		;nnnn100111110111
	eatest	([bd.l,za4],zd5.l*2,od.l)	;nnnn101111110111
	eatest	([bd.l,za4],zd5.l*4,od.l)	;nnnn110111110111
	eatest	([bd.l,za4],zd5.l*8,od.l)	;nnnn111111110111
    .endm
  .endm
	move.l	#crc,d1
	jsr	test_check
	catch_illegal_instruction
@skip:
	.endm


;--------------------------------------------------------------------------------
;main

	.text
	.even

main:
	lea.l	(16,a0),a0
	suba.l	a0,a1
	movem.l	a0-a1,-(sp)
	DOS	_SETBLOCK
	addq.l	#8,sp

	lea.l	stack_area_end,sp

	lea.l	1(a2),a0
1:	move.b	(a0)+,d0
	beq	2f
	cmp.b	#' ',d0
	bls	1b
2:	subq.l	#1,a0
	tst.b	(a0)
	bne	3f
	print	'usage: instructiontest <leading letters of a mnemonic | all>',13,10
	jmp	exit
3:	lea.l	mnemonic_buffer,a1
	move.w	#256,d1
	bra	2f
1:	or.b	#$20,d0
	move.b	d0,(a1)+
2:	move.b	(a0)+,d0
	cmp.b	#' ',d0
	dbls	d1,1b
	sf.b	(a1)

	lea.l	$0CBC.w,a1
	IOCS	_B_BPEEK
	move.l	d0,d1			;0=MC68000,1=MC68010,2=MC68020,3=MC68030,4=MC68040,6=MC68060
	moveq.l	#1,d0
	lsl.b	d1,d0
	move.b	d0,mpu_type		;1=MC68000,2=MC68010,4=MC68020,8=MC68030,16=MC68040,64=MC68060
	print	'processor:MC680'
	moveq.l	#'0',d0
	add.b	d1,d0
	move.w	d0,-(sp)
	jsr	putchar_by_write
	addq.l	#2,sp
	print	'0',13,10

;		           1111111111222222222233333333334444444444555555555566666666667777777777
;		 01234567890123456789012345678901234567890123456789012345678901234567890123456789
;		                                     $xxxxxxxx   $xxxxxxxx   OK
	print	'           instruction               expected     actual',13,10

	jsr	test_start

	move.w	#_CTRLVC,-(sp)
	DOS	_INTVCG
	addq.l	#2,sp
	move.l	d0,old_ctrlvc
	move.w	#_ERRJVC,-(sp)
	DOS	_INTVCG
	addq.l	#2,sp
	move.l	d0,old_errjvc
	pea.l	new_ctrlvc
	move.w	#_CTRLVC,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	pea.l	new_errjvc
	move.w	#_ERRJVC,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp

	pea.l	new_illegal_instruction
	move.w	#4,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	d0,old_illegal_instruction

	pea.l	new_divide_by_zero
	move.w	#5,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	d0,old_divide_by_zero

	pea.l	new_chk_instruction
	move.w	#6,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	d0,old_chk_instruction

	pea.l	new_trapv_instruction
	move.w	#7,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	d0,old_trapv_instruction

	pea.l	new_privilege_instruction
	move.w	#8,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	d0,old_privilege_instruction

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb_drb	abcd.b,$5548412c	;ABCD.B Dr,Dq
	xxx_mnb_mnb	abcd.b,$8952ee82	;ABCD.B -(Ar),-(Aq)
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb_drb	abcd.b,$40d21e5c	;ABCD.B Dr,Dq
	xxx_mnb_mnb	abcd.b,$68f24245	;ABCD.B -(Ar),-(Aq)
@@:

	xxx_drb_drb	add.b,$dfa4e2b8		;ADD.B Dr,Dq
	xxx_drw_drw	add.w,$02b9a679		;ADD.W Dr,Dq
	xxx_drl_drl	add.l,$0f056fd1		;ADD.L Dr,Dq
	xxx_mmb_drb	add.b,$21cae1be		;ADD.B (Ar),Dq
	xxx_mmw_drw	add.w,$a7ff428e		;ADD.W (Ar),Dq
	xxx_mml_drl	add.l,$0f056fd1		;ADD.L (Ar),Dq
	xxx_drb_mmb	add.b,$f061d262		;ADD.B Dq,(Ar)
	xxx_drw_mmw	add.w,$5a3bec22		;ADD.W Dq,(Ar)
	xxx_drl_mml	add.l,$0f056fd1		;ADD.L Dq,(Ar)

	xxx_drw_arl	adda.w,$53d6a361	;ADDA.W Dr,Aq
	xxx_drl_arl	adda.l,$32de9b3a	;ADDA.L Dr,Aq
	xxx_mmw_arl	adda.w,$210772d4	;ADDA.W (Ar),Aq
	xxx_mml_arl	adda.l,$32de9b3a	;ADDA.L (Ar),Aq

	xxx_imb_drb	addi.b,$b9a23342	;ADDI.B #<data>,Dr
	xxx_imw_drw	addi.w,$afc2ebb3	;ADDI.W #<data>,Dr
	xxx_iml_drl	addi.l,$1dcef17c	;ADDI.L #<data>,Dr
	xxx_imb_mmb	addi.b,$c23be1e4	;ADDI.B #<data>,(Ar)
	xxx_imw_mmw	addi.w,$981da70c	;ADDI.W #<data>,(Ar)
	xxx_iml_mml	addi.l,$1dcef17c	;ADDI.L #<data>,(Ar)

	xxx_imq_drb	addq.b,$df01d53c	;ADDQ.B #<data>,Dr
	xxx_imq_drw	addq.w,$bd621acf	;ADDQ.W #<data>,Dr
	xxx_imq_drl	addq.l,$7594c89b	;ADDQ.L #<data>,Dr
	xxx_imq_arl	addq.w,$d5efcf93	;ADDQ.W #<data>,Ar
	xxx_imq_arl	addq.l,$d5efcf93	;ADDQ.L #<data>,Ar
	xxx_imq_mmb	addq.b,$0f2d303e	;ADDQ.B #<data>,(Ar)
	xxx_imq_mmw	addq.w,$5618fc06	;ADDQ.W #<data>,(Ar)
	xxx_imq_mml	addq.l,$7594c89b	;ADDQ.L #<data>,(Ar)

	xxx_drb_drb	addx.b,$9b8170ad	;ADDX.B Dr,Dq
	xxx_drw_drw	addx.w,$a11e33fe	;ADDX.W Dr,Dq
	xxx_drl_drl	addx.l,$965544f9	;ADDX.L Dr,Dq
	xxx_mnb_mnb	addx.b,$3b8e0e8c	;ADDX.B -(Ar),-(Aq)
	xxx_mnw_mnw	addx.w,$cdbfd621	;ADDX.W -(Ar),-(Aq)
	xxx_mnl_mnl	addx.l,$46394e22	;ADDX.L -(Ar),-(Aq)

	xxx_drb_drb	and.b,$cd73abd8		;AND.B Dr,Dq
	xxx_drw_drw	and.w,$c5dfd024		;AND.W Dr,Dq
	xxx_drl_drl	and.l,$df324e0b		;AND.L Dr,Dq
	xxx_mmb_drb	and.b,$331da8de		;AND.B (Ar),Dq
	xxx_mmw_drw	and.w,$609934d3		;AND.W (Ar),Dq
	xxx_mml_drl	and.l,$df324e0b		;AND.L (Ar),Dq
	xxx_drb_mmb	and.b,$435e51a9		;AND.B Dq,(Ar)
	xxx_drw_mmw	and.w,$0df892c5		;AND.W Dq,(Ar)
	xxx_drl_mml	and.l,$df324e0b		;AND.L Dq,(Ar)

	xxx_imb_drb	andi.b,$db4e3c46	;ANDI.B #<data>,Dr
	xxx_imw_drw	andi.w,$51df8be3	;ANDI.W #<data>,Dr
	xxx_iml_drl	andi.l,$ee8417c0	;ANDI.L #<data>,Dr
	xxx_imb_mmb	andi.b,$73060ca3	;ANDI.B #<data>,(Ar)
	xxx_imw_mmw	andi.w,$a3ecbd85	;ANDI.W #<data>,(Ar)
	xxx_iml_mml	andi.l,$ee8417c0	;ANDI.L #<data>,(Ar)
	xxx_imb_ccr	andi.b,$0657e338	;ANDI.B #<data>,CCR
						;ANDI.W #<data>,SR

	xxx_drb_drb	asl.b,$55a55ff9		;ASL.B Dq,Dr
	xxx_drb_drw	asl.w,$60032ae6		;ASL.W Dq,Dr
	xxx_drb_drl	asl.l,$7cca4f57		;ASL.L Dq,Dr
	xxx_imq_drb	asl.b,$939c2ed4		;ASL.B #<data>,Dr
	xxx_imq_drw	asl.w,$578bed09		;ASL.W #<data>,Dr
	xxx_imq_drl	asl.l,$ef3d1a62		;ASL.L #<data>,Dr
	xxx_mmw		asl.w,$2bb6720f		;ASL.W (Ar)

	xxx_drb_drb	asr.b,$51be907b		;ASR.B Dq,Dr
	xxx_drb_drw	asr.w,$eea90c31		;ASR.W Dq,Dr
	xxx_drb_drl	asr.l,$5237b420		;ASR.L Dq,Dr
	xxx_imq_drb	asr.b,$5615106d		;ASR.B #<data>,Dr
	xxx_imq_drw	asr.w,$62607697		;ASR.W #<data>,Dr
	xxx_imq_drl	asr.l,$cc13b46b		;ASR.L #<data>,Dr
	xxx_mmw		asr.w,$967415e7		;ASR.W (Ar)

	bcc_label	bcc.s,$f30675e0		;BCC.S <label>
	bcc_label	bcc.w,$f30675e0		;BCC.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bcc.l,$f30675e0		;BCC.L <label>
	.cpu	68000
@@:

	xxx_drb_drl	bchg.l,$2ae2a803	;BCHG.L Dq,Dr
	xxx_im5_drl	bchg.l,$19467d95	;BCHG.L #<data>,Dr
	xxx_drb_mmb	bchg.b,$f1f29660	;BCHG.B Dq,(Ar)
	xxx_im3_mmb	bchg.b,$40339966	;BCHG.B #<data>,(Ar)

	xxx_drb_drl	bclr.l,$43785dce	;BCLR.L Dq,Dr
	xxx_im5_drl	bclr.l,$21153c05	;BCLR.L #<data>,Dr
	xxx_drb_mmb	bclr.b,$e70b1c92	;BCLR.B Dq,(Ar)
	xxx_im3_mmb	bclr.b,$c0931996	;BCLR.B #<data>,(Ar)

	bcc_label	bcs.s,$e7db1aaa		;BCS.S <label>
	bcc_label	bcs.w,$e7db1aaa		;BCS.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bcs.l,$e7db1aaa		;BCS.L <label>
	.cpu	68000
@@:

	bcc_label	beq.s,$dd300132		;BEQ.S <label>
	bcc_label	beq.w,$dd300132		;BEQ.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	beq.l,$dd300132		;BEQ.L <label>
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss	bfchg,$66e7a75c		;BFCHG Dr{#o:#w}
	bfxxx_drsd	bfchg,$16b71ced		;BFCHG Dr{#o:Dw}
	bfxxx_drds	bfchg,$133cba57		;BFCHG Dr{Do:#w}
	bfxxx_drdd	bfchg,$2b584550		;BFCHG Dr{Do:Dw}
	bfxxx_mmss	bfchg,$687e4b69		;BFCHG (Ar){#o:#w}
	bfxxx_mmsd	bfchg,$e9070777		;BFCHG (Ar){#o:Dw}
	bfxxx_mmds	bfchg,$f1865f2b		;BFCHG (Ar){Do:#w}
	bfxxx_mmdd	bfchg,$e660cfd7		;BFCHG (Ar){Do:Dw}
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss	bfclr,$b935f4f0		;BFCLR Dr{#o:#w}
	bfxxx_drsd	bfclr,$d012933a		;BFCLR Dr{#o:Dw}
	bfxxx_drds	bfclr,$bdeb16fe		;BFCLR Dr{Do:#w}
	bfxxx_drdd	bfclr,$35fd889f		;BFCLR Dr{Do:Dw}
	bfxxx_mmss	bfclr,$d05197dc		;BFCLR (Ar){#o:#w}
	bfxxx_mmsd	bfclr,$905aaa98		;BFCLR (Ar){#o:Dw}
	bfxxx_mmds	bfclr,$a2486f74		;BFCLR (Ar){Do:#w}
	bfxxx_mmdd	bfclr,$74bda607		;BFCLR (Ar){Do:Dw}
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss_drl	bfexts,$1c77c5e1	;BFEXTS Dr{#o:#w},Dn
	bfxxx_drsd_drl	bfexts,$2b26466f	;BFEXTS Dr{#o:Dw},Dn
	bfxxx_drds_drl	bfexts,$45b81e9f	;BFEXTS Dr{Do:#w},Dn
	bfxxx_drdd_drl	bfexts,$f6e20650	;BFEXTS Dr{Do:Dw},Dn
	bfxxx_mmss_drl	bfexts,$8e806a24	;BFEXTS (Ar){#o:#w},Dn
	bfxxx_mmsd_drl	bfexts,$bba9e2ac	;BFEXTS (Ar){#o:Dw},Dn
	bfxxx_mmds_drl	bfexts,$34c1e59a	;BFEXTS (Ar){Do:#w},Dn
	bfxxx_mmdd_drl	bfexts,$49234fca	;BFEXTS (Ar){Do:Dw},Dn
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss_drl	bfextu,$0e9d12e6	;BFEXTU Dr{#o:#w},Dn
	bfxxx_drsd_drl	bfextu,$11156b6e	;BFEXTU Dr{#o:Dw},Dn
	bfxxx_drds_drl	bfextu,$a10caf8a	;BFEXTU Dr{Do:#w},Dn
	bfxxx_drdd_drl	bfextu,$60fe94b3	;BFEXTU Dr{Do:Dw},Dn
	bfxxx_mmss_drl	bfextu,$4cc25cf3	;BFEXTU (Ar){#o:#w},Dn
	bfxxx_mmsd_drl	bfextu,$0a06ee35	;BFEXTU (Ar){#o:Dw},Dn
	bfxxx_mmds_drl	bfextu,$fdf4106c	;BFEXTU (Ar){Do:#w},Dn
	bfxxx_mmdd_drl	bfextu,$0f3ce482	;BFEXTU (Ar){Do:Dw},Dn
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss_drl	bfffo,$03f2d1a4		;BFFFO Dr{#o:#w},Dn
	bfxxx_drsd_drl	bfffo,$f567dc6b		;BFFFO Dr{#o:Dw},Dn
	bfxxx_drds_drl	bfffo,$3ce523b5		;BFFFO Dr{Do:#w},Dn
	bfxxx_drdd_drl	bfffo,$14de92a5		;BFFFO Dr{Do:Dw},Dn
	bfxxx_mmss_drl	bfffo,$0d2aa0d7		;BFFFO (Ar){#o:#w},Dn
	bfxxx_mmsd_drl	bfffo,$328cb96c		;BFFFO (Ar){#o:Dw},Dn
	bfxxx_mmds_drl	bfffo,$02611cc9		;BFFFO (Ar){Do:#w},Dn
	bfxxx_mmdd_drl	bfffo,$0aa0e90d		;BFFFO (Ar){Do:Dw},Dn
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drl_drss	bfins,$321bbc83		;BFINS Dn,Dr{#o:#w}
	bfxxx_drl_drsd	bfins,$9422855a		;BFINS Dn,Dr{#o:Dw}
	bfxxx_drl_drds	bfins,$5049cba9		;BFINS Dn,Dr{Do:#w}
	bfxxx_drl_drdd	bfins,$781e830f		;BFINS Dn,Dr{Do:Dw}
	bfxxx_drl_mmss	bfins,$b8186ff2		;BFINS Dn,(Ar){#o:#w}
	bfxxx_drl_mmsd	bfins,$c17aab89		;BFINS Dn,(Ar){#o:Dw}
	bfxxx_drl_mmds	bfins,$9a1b6e56		;BFINS Dn,(Ar){Do:#w}
	bfxxx_drl_mmdd	bfins,$d9bfce25		;BFINS Dn,(Ar){Do:Dw}
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss	bfset,$2d1f28c1		;BFSET Dr{#o:#w}
	bfxxx_drsd	bfset,$28b30833		;BFSET Dr{#o:Dw}
	bfxxx_drds	bfset,$7e5bac5a		;BFSET Dr{Do:#w}
	bfxxx_drdd	bfset,$f54705c7		;BFSET Dr{Do:Dw}
	bfxxx_mmss	bfset,$b44e72e5		;BFSET (Ar){#o:#w}
	bfxxx_mmsd	bfset,$2cdfc077		;BFSET (Ar){#o:Dw}
	bfxxx_mmds	bfset,$71b6e9ce		;BFSET (Ar){Do:#w}
	bfxxx_mmdd	bfset,$7d75539e		;BFSET (Ar){Do:Dw}
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bfxxx_drss	bftst,$f2cd7b6d		;BFTST Dr{#o:#w}
	bfxxx_drsd	bftst,$ee1687e4		;BFTST Dr{#o:Dw}
	bfxxx_drds	bftst,$d08c00f3		;BFTST Dr{Do:#w}
	bfxxx_drdd	bftst,$ebe2c808		;BFTST Dr{Do:Dw}
	bfxxx_mmss	bftst,$0c61ae50		;BFTST (Ar){#o:#w}
	bfxxx_mmsd	bftst,$55826d98		;BFTST (Ar){#o:Dw}
	bfxxx_mmds	bftst,$2278d991		;BFTST (Ar){Do:#w}
	bfxxx_mmdd	bftst,$efa83a4e		;BFTST (Ar){Do:Dw}
	.cpu	68000
@@:

	bcc_label	bge.s,$56553618		;BGE.S <label>
	bcc_label	bge.w,$56553618		;BGE.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bge.l,$56553618		;BGE.L <label>
	.cpu	68000
@@:

	bcc_label	bgt.s,$f548e8da		;BGT.S <label>
	bcc_label	bgt.w,$f548e8da		;BGT.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bgt.l,$f548e8da		;BGT.L <label>
	.cpu	68000
@@:

	bcc_label	bhi.s,$ae658b96		;BHI.S <label>
	bcc_label	bhi.w,$ae658b96		;BHI.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bhi.l,$ae658b96		;BHI.L <label>
	.cpu	68000
@@:

						;BITREV.L Dr

						;BKPT #<data>

	bcc_label	ble.s,$e1958790		;BLE.S <label>
	bcc_label	ble.w,$e1958790		;BLE.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	ble.l,$e1958790		;BLE.L <label>
	.cpu	68000
@@:

	bcc_label	bls.s,$bab8e4dc		;BLS.S <label>
	bcc_label	bls.w,$bab8e4dc		;BLS.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bls.l,$bab8e4dc		;BLS.L <label>
	.cpu	68000
@@:

	bcc_label	blt.s,$42885952		;BLT.S <label>
	bcc_label	blt.w,$42885952		;BLT.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	blt.l,$42885952		;BLT.L <label>
	.cpu	68000
@@:

	bcc_label	bmi.s,$a8249454		;BMI.S <label>
	bcc_label	bmi.w,$a8249454		;BMI.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bmi.l,$a8249454		;BMI.L <label>
	.cpu	68000
@@:

	bcc_label	bne.s,$c9ed6e78		;BNE.S <label>
	bcc_label	bne.w,$c9ed6e78		;BNE.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bne.l,$c9ed6e78		;BNE.L <label>
	.cpu	68000
@@:

	bcc_label	bpl.s,$bcf9fb1e		;BPL.S <label>
	bcc_label	bpl.w,$bcf9fb1e		;BPL.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bpl.l,$bcf9fb1e		;BPL.L <label>
	.cpu	68000
@@:

	bcc_label	bra.s,$aa254bc1		;BRA.S <label>
	bcc_label	bra.w,$aa254bc1		;BRA.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bra.l,$aa254bc1		;BRA.L <label>
	.cpu	68000
@@:

	xxx_drb_drl	bset.l,$5eca10ad	;BSET.L Dq,Dr
	xxx_im5_drl	bset.l,$1c9f0d13	;BSET.L #<data>,Dr
	xxx_drb_mmb	bset.b,$45f44fc9	;BSET.B Dq,(Ar)
	xxx_im3_mmb	bset.b,$8c73dd9a	;BSET.B #<data>,(Ar)

	bsr_label	bsr.s,$67feb070		;BSR.S <label>
	bsr_label	bsr.w,$67feb070		;BSR.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bsr_label	bsr.l,$67feb070		;BSR.L <label>
	.cpu	68000
@@:

	xxx_drb_drl	btst.l,$3750e560	;BTST.L Dq,Dr
	xxx_im5_drl	btst.l,$24cc4c83	;BTST.L #<data>,Dr
	xxx_drb_mmb	btst.b,$530dc53b	;BTST.B Dq,(Ar)
	xxx_im3_mmb	btst.b,$0cd35d6a	;BTST.B #<data>,(Ar)
	xxx_drb_imb	btst.b,$79b29194	;BTST.B Dq,#<data>

	bcc_label	bvc.s,$408986c7		;BVC.S <label>
	bcc_label	bvc.w,$408986c7		;BVC.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bvc.l,$408986c7		;BVC.L <label>
	.cpu	68000
@@:

	bcc_label	bvs.s,$5454e98d		;BVS.S <label>
	bcc_label	bvs.w,$5454e98d		;BVS.W <label>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	bcc_label	bvs.l,$5454e98d		;BVS.L <label>
	.cpu	68000
@@:

						;BYTEREV.L Dr

						;CALLM #<data>,(Ar)

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	cas_byte	cas.b,$265cc072		;CAS.B Dc,Du,<ea>
	cas_word	cas.w,$4a8dc694		;CAS.W Dc,Du,<ea>
	cas_long	cas.l,$a7388115		;CAS.L Dc,Du,<ea>
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	cas2_word	cas2.w,$f7040931	;CAS2.W Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)
	cas2_long	cas2.l,$32be8a52	;CAS2.L Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)
	.cpu	68000
@@:

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_mmw_drw	chk.w,$6fc015eb		;CHK.W (Ar),Dq
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_mmw_drw	chk.w,$98db9e5b		;CHK.W (Ar),Dq
@@:

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_mml_drl	chk.l,$48d9e9de		;CHK.L (Ar),Dq
	.cpu	68000
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_mml_drl	chk.l,$a8f4f758		;CHK.L (Ar),Dq
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_mmw_drb	chk2.b,$e3ce12a2	;CHK2.B (Ar),Dn
	xxx_mml_drw	chk2.w,$306f7caa	;CHK2.W (Ar),Dn
	xxx_mmq_drl	chk2.l,$1ead6442	;CHK2.L (Ar),Dn
	xxx_mmw_arl	chk2.b,$3388f45f	;CHK2.B (Ar),An
	xxx_mml_arl	chk2.w,$9e27300e	;CHK2.W (Ar),An
	xxx_mmq_arl	chk2.l,$1ead6442	;CHK2.L (Ar),An
	.cpu	68000
@@:

						;CINVA *C

						;CINVL *C,(Ar)

						;CINVP *C,(Ar)

	xxx_drb		clr.b,$45ff0c0e		;CLR.B Dr
	xxx_drw		clr.w,$32f3b058		;CLR.W Dr
	xxx_drl		clr.l,$1a04544e		;CLR.L Dr
	xxx_mmb		clr.b,$15914b53		;CLR.B (Ar)
	xxx_mmw		clr.w,$3984922c		;CLR.W (Ar)
	xxx_mml		clr.l,$1a04544e		;CLR.L (Ar)

	xxx_drb_drb	cmp.b,$aad1b753		;CMP.B Dr,Dq
	xxx_drw_drw	cmp.w,$87111837		;CMP.W Dr,Dq
	xxx_drl_drl	cmp.l,$cd504ae1		;CMP.L Dr,Dq
	xxx_mmb_drb	cmp.b,$54bfb455		;CMP.B (Ar),Dq
	xxx_mmw_drw	cmp.w,$2257fcc0		;CMP.W (Ar),Dq
	xxx_mml_drl	cmp.l,$cd504ae1		;CMP.L (Ar),Dq

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_mmw_drb	cmp2.b,$0f519c55	;CMP2.B (An),Dn
	xxx_mml_drw	cmp2.w,$1f671d3a	;CMP2.W (An),Dn
	xxx_mmq_drl	cmp2.l,$ac60dff6	;CMP2.L (An),Dn
	xxx_mmw_arl	cmp2.b,$c6a2a566	;CMP2.B (An),An
	xxx_mml_arl	cmp2.w,$f818b60c	;CMP2.W (An),An
	xxx_mmq_arl	cmp2.l,$ac60dff6	;CMP2.L (An),An
	.cpu	68000
@@:

	xxx_drw_arl	cmpa.w,$be8c7dea	;CMPA.W Dr,Aq
	xxx_drl_arl	cmpa.l,$cd504ae1	;CMPA.L Dr,Aq
	xxx_mmw_arl	cmpa.w,$cc5dac5f	;CMPA.W (Ar),Aq
	xxx_mml_arl	cmpa.l,$cd504ae1	;CMPA.L (Ar),Aq

	xxx_imb_drb	cmpi.b,$dd5ac082	;CMPI.B #<data>,Dr
	xxx_imw_drw	cmpi.w,$7fd56696	;CMPI.W #<data>,Dr
	xxx_iml_drl	cmpi.l,$af99c75c	;CMPI.L #<data>,Dr
	xxx_imb_mmb	cmpi.b,$d9720d27	;CMPI.B #<data>,(Ar)
	xxx_imw_mmw	cmpi.w,$fcdd44e8	;CMPI.W #<data>,(Ar)
	xxx_iml_mml	cmpi.l,$af99c75c	;CMPI.L #<data>,(Ar)

	xxx_mpb_mpb	cmpm.b,$d312254d	;CMPM.B (Ar)+,(Aq)+
	xxx_mpw_mpw	cmpm.w,$cc9efc22	;CMPM.W (Ar)+,(Aq)+
	xxx_mpl_mpl	cmpm.l,$10db5911	;CMPM.L (Ar)+,(Aq)+

						;CPUSHA *C

						;CPUSHL *C,(Ar)

						;CPUSHP *C,(Ar)

	dbcc_drw_label	dbcc.w,$2ef9c63f	;DBCC.W Dr,<label>

	dbcc_drw_label	dbcs.w,$d6367901	;DBCS.W Dr,<label>

	dbcc_drw_label	dbeq.w,$5837e090	;DBEQ.W Dr,<label>

	dbcc_drw_label	dbf.w,$d8a3dfc4		;DBF.W Dr,<label>

	dbcc_drw_label	dbge.w,$11bdab9f	;DBGE.W Dr,<label>

	dbcc_drw_label	dbgt.w,$0bb0fde3	;DBGT.W Dr,<label>

	dbcc_drw_label	dbhi.w,$4468ce33	;DBHI.W Dr,<label>

	dbcc_drw_label	dble.w,$f37f42dd	;DBLE.W Dr,<label>

	dbcc_drw_label	dbls.w,$bca7710d	;DBLS.W Dr,<label>

	dbcc_drw_label	dblt.w,$e97214a1	;DBLT.W Dr,<label>

	dbcc_drw_label	dbmi.w,$b472c6bd	;DBMI.W Dr,<label>

	dbcc_drw_label	dbpl.w,$4cbd7983	;DBPL.W Dr,<label>

	dbcc_drw_label	dbt.w,$206c60fa		;DBT.W Dr,<label>

	dbcc_drw_label	dbvc.w,$7d6cb2e6	;DBVC.W Dr,<label>

	dbcc_drw_label	dbvs.w,$85a30dd8	;DBVS.W Dr,<label>

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drw_drl	divs.w,$582e4721	;DIVS.W Dr,Dq
	xxx_mmw_drl	divs.w,$2aff9694	;DIVS.W (Ar),Dq
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drw_drl	divs.w,$0df89afb	;DIVS.W Dr,Dq
	xxx_mmw_drl	divs.w,$7f294b4e	;DIVS.W (Ar),Dq
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	divs.l,$24e8a786	;DIVS.L Dr,Dq
	xxx_mml_drl	divs.l,$24e8a786	;DIVS.L (Ar),Dq
	.cpu	68000
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	divs.l,$fe788660	;DIVS.L Dr,Dq
	xxx_mml_drl	divs.l,$fe788660	;DIVS.L (Ar),Dq
	.cpu	68000
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divs.l,$98ceff6a	;DIVS.L Dr,Dh:Dl
	xxx_mml_drq	divs.l,$98ceff6a	;DIVS.L (Ar),Dh:Dl
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divsl.l,$7f5578e0	;DIVSL.L Dr,Dh:Dl
	xxx_mml_drq	divsl.l,$7f5578e0	;DIVSL.L (Ar),Dh:Dl
	.cpu	68000
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divsl.l,$06985a9c	;DIVSL.L Dr,Dh:Dl
	xxx_mml_drq	divsl.l,$06985a9c	;DIVSL.L (Ar),Dh:Dl
	.cpu	68000
@@:

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drw_drl	divu.w,$09ac1c45	;DIVU.W Dr,Dq
	xxx_mmw_drl	divu.w,$7b7dcdf0	;DIVU.W (Ar),Dq
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drw_drl	divu.w,$ded94f76	;DIVU.W Dr,Dq
	xxx_mmw_drl	divu.w,$ac089ec3	;DIVU.W (Ar),Dq
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	divu.l,$947d5f9d	;DIVU.L Dr,Dq
	xxx_mml_drl	divu.l,$947d5f9d	;DIVU.L (Ar),Dq
	.cpu	68000
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	divu.l,$48cd5d48	;DIVU.L Dr,Dq
	xxx_mml_drl	divu.l,$48cd5d48	;DIVU.L (Ar),Dq
	.cpu	68000
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divu.l,$c53edcf3	;DIVU.L Dr,Dh:Dl
	xxx_mml_drq	divu.l,$c53edcf3	;DIVU.L (Ar),Dh:Dl
	.cpu	68000
@@:

	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divul.l,$d4f37612	;DIVUL.L Dr,Dh:Dl
	xxx_mml_drq	divul.l,$d4f37612	;DIVUL.L (Ar),Dh:Dl
	.cpu	68000
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	divul.l,$f9467912	;DIVUL.L Dr,Dh:Dl
	xxx_mml_drq	divul.l,$f9467912	;DIVUL.L (Ar),Dh:Dl
	.cpu	68000
@@:

	xxx_drb_drb	eor.b,$47768d81		;EOR.B Dq,Dr
	xxx_drw_drw	eor.w,$32953267		;EOR.W Dq,Dr
	xxx_drl_drl	eor.l,$6327714f		;EOR.L Dq,Dr
	xxx_drb_mmb	eor.b,$51e4c2ea		;EOR.B Dq,(Ar)
	xxx_drw_mmw	eor.w,$1df0cd48		;EOR.W Dq,(Ar)
	xxx_drl_mml	eor.l,$6327714f		;EOR.L Dq,(Ar)

	xxx_imb_drb	eori.b,$1c6237ec	;EORI.B #<data>,Dr
	xxx_imw_drw	eori.w,$ae1c1ea3	;EORI.W #<data>,Dr
	xxx_iml_drl	eori.l,$db9043f5	;EORI.L #<data>,Dr
	xxx_imb_mmb	eori.b,$bfd41bc6	;EORI.B #<data>,(Ar)
	xxx_imw_mmw	eori.w,$8ebd5762	;EORI.W #<data>,(Ar)
	xxx_iml_mml	eori.l,$db9043f5	;EORI.L #<data>,(Ar)
	xxx_imb_ccr	eori.b,$64979cf3	;EORI.B #<data>,CCR
						;EORI.W #<data>,SR

	xxx_drl_drl	exg.l,$a7c04ba6		;EXG.L Dq,Dr
	xxx_arl_arl	exg.l,$a7c04ba6		;EXG.L Aq,Ar
	xxx_drl_arl	exg.l,$a7c04ba6		;EXG.L Dq,Ar

	xxx_drb		ext.w,$d2b9e70a		;EXT.W Dr
	xxx_drw		ext.l,$79e91538		;EXT.L Dr

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drb		extb.l,$80a53eb5	;EXTB.L Dr
	.cpu	68000
@@:

						;FABS
						;FACOS
						;FADD
						;FASIN
						;FATAN
						;FBEQ
						;FBF
						;FBGE
						;FBGL
						;FBGLE
						;FBGT
						;FBLE
						;FBLT
						;FBNE
						;FBNGE
						;FBNGL
						;FBNGLE
						;FBNGT
						;FBNLE
						;FBNLT
						;FBOGE
						;FBOGL
						;FBOGT
						;FBOLE
						;FBOLT
						;FBOR
						;FBSEQ
						;FBSF
						;FBSNE
						;FBST
						;FBT
						;FBUEQ
						;FBUGE
						;FBUGT
						;FBULE
						;FBULT
						;FBUN
						;FCMP
						;FCOS
						;FCOSH
						;FDABS
						;FDADD
						;FDBEQ
						;FDBF
						;FDBGE
						;FDBGL
						;FDBGLE
						;FDBGT
						;FDBLE
						;FDBLT
						;FDBNE
						;FDBNGE
						;FDBNGL
						;FDBNGLE
						;FDBNGT
						;FDBNLE
						;FDBNLT
						;FDBOGE
						;FDBOGL
						;FDBOGT
						;FDBOLE
						;FDBOLT
						;FDBOR
						;FDBSEQ
						;FDBSF
						;FDBSNE
						;FDBST
						;FDBT
						;FDBUEQ
						;FDBUGE
						;FDBUGT
						;FDBULE
						;FDBULT
						;FDBUN
						;FDDIV
						;FDIV
						;FDMOVE
						;FDMUL
						;FDNEG
						;FDSQRT
						;FDSUB
						;FETOX
						;FETOXM1

						;FF1.L Dr

						;FGETEXP
						;FGETMAN
						;FINT
						;FINTRZ
						;FLOG10
						;FLOG2
						;FLOGN
						;FLOGNP1
						;FMOD
						;FMOVE
						;FMOVECR
						;FMOVEM
						;FMUL
						;FNEG
						;FREM
						;FRESTORE
						;FSABS
						;FSADD
						;FSAVE
						;FSCALE
						;FSDIV
						;FSEQ
						;FSF
						;FSGE
						;FSGL
						;FSGLDIV
						;FSGLE
						;FSGLMUL
						;FSGT
						;FSIN
						;FSINCOS
						;FSINH
						;FSLE
						;FSLT
						;FSMOVE
						;FSMUL
						;FSNE
						;FSNEG
						;FSNGE
						;FSNGL
						;FSNGLE
						;FSNGT
						;FSNLE
						;FSNLT
						;FSOGE
						;FSOGL
						;FSOGT
						;FSOLE
						;FSOLT
						;FSOR
						;FSQRT
						;FSSEQ
						;FSSF
						;FSSNE
						;FSSQRT
						;FSST
						;FSSUB
						;FST
						;FSUB
						;FSUEQ
						;FSUGE
						;FSUGT
						;FSULE
						;FSULT
						;FSUN
						;FTAN
						;FTANH
						;FTENTOX
						;FTRAPEQ
						;FTRAPF
						;FTRAPGE
						;FTRAPGL
						;FTRAPGLE
						;FTRAPGT
						;FTRAPLE
						;FTRAPLT
						;FTRAPNE
						;FTRAPNGE
						;FTRAPNGL
						;FTRAPNGLE
						;FTRAPNGT
						;FTRAPNLE
						;FTRAPNLT
						;FTRAPOGE
						;FTRAPOGL
						;FTRAPOGT
						;FTRAPOLE
						;FTRAPOLT
						;FTRAPOR
						;FTRAPSEQ
						;FTRAPSF
						;FTRAPSNE
						;FTRAPST
						;FTRAPT
						;FTRAPUEQ
						;FTRAPUGE
						;FTRAPUGT
						;FTRAPULE
						;FTRAPULT
						;FTRAPUN
						;FTST
						;FTWOTOX

						;ILLEGAL

	jmp_mml		jmp,$aa254bc1		;JMP <ea>

	jsr_mml		jsr,$67feb070		;JSR <ea>

	lea_mml_arl	lea.l,$8420a456		;LEA.L (Ar),Aq
	moveq.l	#MC68000|MC68010,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	lea_brief	lea.l,$b1d175d7		;LEA.L <brief-format>,Aq
	.cpu	68000
@@:
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	lea_brief	lea.l,$68b62ff6		;LEA.L <brief-format>,Aq
	lea_full	lea.l,$dc4c31af		;LEA.L <full-format>,Aq
	.cpu	68000
@@:

	link_arl_imw	link.w,$1e4e47e0	;LINK.W Ar,#<data>
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	link_arl_iml	link.l,$1c66adcc	;LINK.L Ar,#<data>
	.cpu	68000
@@:

						;LPSTOP.W #<data>

	xxx_drb_drb	lsl.b,$2c016afc		;LSL.B Dq,Dr
	xxx_drb_drw	lsl.w,$05475ff7		;LSL.W Dq,Dr
	xxx_drb_drl	lsl.l,$20c05bd9		;LSL.L Dq,Dr
	xxx_imq_drb	lsl.b,$739bc4d8		;LSL.B #<data>,Dr
	xxx_imq_drw	lsl.w,$06ba1484		;LSL.W #<data>,Dr
	xxx_imq_drl	lsl.l,$0a1a0cf4		;LSL.L #<data>,Dr
	xxx_mmw		lsl.w,$753dad40		;LSL.W (Ar)

	xxx_drb_drb	lsr.b,$1dac497f		;LSR.B Dq,Dr
	xxx_drb_drw	lsr.w,$bcac3d1d		;LSR.W Dq,Dr
	xxx_drb_drl	lsr.l,$dbd000cb		;LSR.L Dq,Dr
	xxx_imq_drb	lsr.b,$c4bfca04		;LSR.B #<data>,Dr
	xxx_imq_drw	lsr.w,$3415b091		;LSR.W #<data>,Dr
	xxx_imq_drl	lsr.l,$cee4e06f		;LSR.L #<data>,Dr
	xxx_mmw		lsr.w,$ba20612f		;LSR.W (Ar)

	xxx_drb_drb	move.b,$87baf59a	;MOVE.B Dr,Dq
	xxx_drw_drw	move.w,$3fbb660e	;MOVE.W Dr,Dq
	xxx_drl_drl	move.l,$b3880bef	;MOVE.L Dr,Dq
	xxx_mmb_drb	move.b,$79d4f69c	;MOVE.B (Ar),Dq
	xxx_mmw_drw	move.w,$9afd82f9	;MOVE.W (Ar),Dq
	xxx_mml_drl	move.l,$b3880bef	;MOVE.L (Ar),Dq
	xxx_drb_mmb	move.b,$7e4e6fbd	;MOVE.B Dr,(Aq)
	xxx_drw_mmw	move.w,$8546cb24	;MOVE.W Dr,(Aq)
	xxx_drl_mml	move.l,$b3880bef	;MOVE.L Dr,(Aq)
	xxx_mmb_mmb	move.b,$80206cbb	;MOVE.B (Ar),(Aq)
	xxx_mmw_mmw	move.w,$20002fd3	;MOVE.W (Ar),(Aq)
	xxx_mml_mml	move.l,$b3880bef	;MOVE.L (Ar),(Aq)
	xxx_imw_ccr	move.w,$eda1718a	;MOVE.W #<data>,CCR

						;MOVE16 (Ar)+,xxx.L
						;MOVE16 xxx.L,(Ar)+
						;MOVE16 (Ar),xxx.L
						;MOVE16 xxx.L,(Ar)
						;MOVE16 (Ar)+,(An)+

	xxx_drw_arl	movea.w,$99a33617	;MOVEA.W Dr,Aq
	xxx_drl_arl	movea.l,$8420a456	;MOVEA.L Dr,Aq
	xxx_mmw_arl	movea.w,$eb72e7a2	;MOVEA.W (Ar),Aq
	xxx_mml_arl	movea.l,$8420a456	;MOVEA.L (Ar),Aq

						;MOVEC.L Rc,Rn
						;MOVEC.L Rn,Rc

	moveq.l	#MC68000|MC68010,d0
	and.b	mpu_type,d0
	beq	@f
	movem_list_mnw	movem.w,$6dc26be8	;MOVEM.W <list>,-(Ar)
	movem_list_mnl	movem.l,$bf5543ec	;MOVEM.L <list>,-(Ar)
@@:
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	movem_list_mnw	movem.w,$9f9ae375	;MOVEM.W <list>,-(Ar)
	movem_list_mnl	movem.l,$c8771aa2	;MOVEM.L <list>,-(Ar)
@@:
	movem_mpw_list	movem.w,$f7e73f39	;MOVEM.W (Ar)+,<list>
	movem_mpl_list	movem.l,$82fc7345	;MOVEM.L (Ar)+,<list>

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_mwl_drw	movep.w,$52c6ba88	;MOVEP.W (d16,Ar),Dq
	xxx_mwq_drl	movep.l,$475c2eca	;MOVEP.L (d16,Ar),Dq
	xxx_drw_mwl	movep.w,$8125a495	;MOVEP.W Dq,(d16,Ar)
	xxx_drl_mwq	movep.l,$310c61cf	;MOVEP.L Dq,(d16,Ar)
@@:

	xxx_imb_drl	moveq.l,$bbbe919d	;MOVEQ.L #<data>,Dq

						;MOVES.B <ea>,Rn
						;MOVES.W <ea>,Rn
						;MOVES.L <ea>,Rn
						;MOVES.B Rn,<ea>
						;MOVES.W Rn,<ea>
						;MOVES.L Rn,<ea>

	xxx_drw_drw	muls.w,$c190876d	;MULS.W Dr,Dq
	xxx_mmw_drw	muls.w,$64d6639a	;MULS.W (Ar),Dq
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	muls.l,$249a2645	;MULS.L Dr,Dl
	xxx_mml_drl	muls.l,$249a2645	;MULS.L (Ar),Dl
	.cpu	68000
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	muls.l,$802d97fa	;MULS.L Dr,Dh:Dl
	xxx_mml_drq	muls.l,$802d97fa	;MULS.L (Ar),Dh:Dl
	.cpu	68000
@@:

	xxx_drw_drw	mulu.w,$4e50704f	;MULU.W Dr,Dq
	xxx_mmw_drw	mulu.w,$eb1694b8	;MULU.W (Ar),Dq
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drl	mulu.l,$69fdd568	;MULU.L Dr,Dl
	xxx_mml_drl	mulu.l,$69fdd568	;MULU.L (Ar),Dl
	.cpu	68000
@@:
	moveq.l	#MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drl_drq	mulu.l,$6f2f64e3	;MULU.L Dr,Dh:Dl
	xxx_mml_drq	mulu.l,$6f2f64e3	;MULU.L (Ar),Dh:Dl
	.cpu	68000
@@:

						;MVS.B <ea>,Dq
						;MVS.W <ea>,Dq

						;MVZ.B <ea>,Dq
						;MVZ.W <ea>,Dq

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb		nbcd.b,$c115e5cf	;NBCD.B Dr
	xxx_mmb		nbcd.b,$892429a2	;NBCD.B (Ar)
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb		nbcd.b,$da5d7b7e	;NBCD.B Dr
	xxx_mmb		nbcd.b,$926cb713	;NBCD.B (Ar)
@@:

	xxx_drb		neg.b,$a53acbbc		;NEG.B Dr
	xxx_drw		neg.w,$60c508c9		;NEG.W Dr
	xxx_drl		neg.l,$317e339c		;NEG.L Dr
	xxx_mmb		neg.b,$6f12da63		;NEG.B (Ar)
	xxx_mmw		neg.w,$2496659f		;NEG.W (Ar)
	xxx_mml		neg.l,$317e339c		;NEG.L (Ar)

	xxx_drb		negx.b,$fd8e1821	;NEGX.B Dr
	xxx_drw		negx.w,$63462f2d	;NEGX.W Dr
	xxx_drl		negx.l,$dbdf823f	;NEGX.L Dr
	xxx_mmb		negx.b,$416caf60	;NEGX.B (Ar)
	xxx_mmw		negx.w,$85b796c5	;NEGX.W (Ar)
	xxx_mml		negx.l,$dbdf823f	;NEGX.L (Ar)

	xxx_no_operand	nop,$91267e8a		;NOP

	xxx_drb		not.b,$810ec23b		;NOT.B Dr
	xxx_drw		not.w,$889e8207		;NOT.W Dr
	xxx_drl		not.l,$d497fc58		;NOT.L Dr
	xxx_mmb		not.b,$646565cc		;NOT.B (Ar)
	xxx_mmw		not.w,$d52476b2		;NOT.W (Ar)
	xxx_mml		not.l,$d497fc58		;NOT.L (Ar)

	xxx_drb_drb	or.b,$460e5a32		;OR.B Dr,Dq
	xxx_drw_drw	or.w,$525fbd72		;OR.W Dr,Dq
	xxx_drl_drl	or.l,$51048d0c		;OR.L Dr,Dq
	xxx_mmb_drb	or.b,$b8605934		;OR.B (Ar),Dq
	xxx_mmw_drw	or.w,$f7195985		;OR.W (Ar),Dq
	xxx_mml_drl	or.l,$51048d0c		;OR.L (Ar),Dq
	xxx_drb_mmb	or.b,$cd062fc1		;OR.B Dq,(Ar)
	xxx_drw_mmw	or.w,$22b42602		;OR.W Dq,(Ar)
	xxx_drl_mml	or.l,$51048d0c		;OR.L Dq,(Ar)

	xxx_imb_drb	ori.b,$10daec76		;ORI.B #<data>,Dr
	xxx_imw_drw	ori.w,$0320d3d8		;ORI.W #<data>,Dr
	xxx_iml_drl	ori.l,$57012e6f		;ORI.L #<data>,Dr
	xxx_imb_mmb	ori.b,$6bfa6287		;ORI.B #<data>,(Ar)
	xxx_imw_mmw	ori.w,$ede4c58f		;ORI.W #<data>,(Ar)
	xxx_iml_mml	ori.l,$57012e6f		;ORI.L #<data>,(Ar)
	xxx_imb_ccr	ori.b,$5287f186		;ORI.B #<data>,CCR
						;ORI.W #<data>,SR

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drw_drb_imw	pack,$72c87df9		;PACK Dr,Dq,#<data>
	xxx_mnw_mnb_imw	pack,$6315321d		;PACK -(Ar),-(Aq),#<data>
	.cpu	68000
@@:

	pea_mml		pea.l,$870f6c34		;PEA.L <ea>

						;PFLUSH (Ar)

						;PFLUSHA

						;PFLUSHAN

						;PFLUSHN (Ar)

						;PLPAR (Ar)

						;PLPAW (Ar)

						;PTESTR

						;PTESTW

						;RESET

	xxx_drb_drb	rol.b,$30bbd844		;ROL.B Dq,Dr
	xxx_drb_drw	rol.w,$052fcba0		;ROL.W Dq,Dr
	xxx_drb_drl	rol.l,$9a8f36e0		;ROL.L Dq,Dr
	xxx_imq_drb	rol.b,$d23c61c5		;ROL.B #<data>,Dr
	xxx_imq_drw	rol.w,$f8dc2a58		;ROL.W #<data>,Dr
	xxx_imq_drl	rol.l,$54f29d06		;ROL.L #<data>,Dr
	xxx_mmw		rol.w,$8c39bab5		;ROL.W (Ar)

	xxx_drb_drb	ror.b,$171a07b9		;ROR.B Dq,Dr
	xxx_drb_drw	ror.w,$8f2014d9		;ROR.W Dq,Dr
	xxx_drb_drl	ror.l,$5b9d07c6		;ROR.L Dq,Dr
	xxx_imq_drb	ror.b,$a464c903		;ROR.B #<data>,Dr
	xxx_imq_drw	ror.w,$695ab28b		;ROR.W #<data>,Dr
	xxx_imq_drl	ror.l,$40776d2b		;ROR.L #<data>,Dr
	xxx_mmw		ror.w,$504ff522		;ROR.W (Ar)

	xxx_drb_drb	roxl.b,$dbb70c26	;ROXL.B Dq,Dr
	xxx_drb_drw	roxl.w,$0a70e80b	;ROXL.W Dq,Dr
	xxx_drb_drl	roxl.l,$47fdf570	;ROXL.L Dq,Dr
	xxx_imq_drb	roxl.b,$a7af236f	;ROXL.B #<data>,Dr
	xxx_imq_drw	roxl.w,$4186fb12	;ROXL.W #<data>,Dr
	xxx_imq_drl	roxl.l,$a0d44bf9	;ROXL.L #<data>,Dr
	xxx_mmw		roxl.w,$c08bcfc9	;ROXL.W (Ar)

	xxx_drb_drb	roxr.b,$1bb83019	;ROXR.B Dq,Dr
	xxx_drb_drw	roxr.w,$90f5976a	;ROXR.W Dq,Dr
	xxx_drb_drl	roxr.l,$53661ce6	;ROXR.L Dq,Dr
	xxx_imq_drb	roxr.b,$8b6477e2	;ROXR.B #<data>,Dr
	xxx_imq_drw	roxr.w,$15910aba	;ROXR.W #<data>,Dr
	xxx_imq_drl	roxr.l,$6530217b	;ROXR.L #<data>,Dr
	xxx_mmw		roxr.w,$4bf937c1	;ROXR.W (Ar)

						;RTD #<data>

						;RTE

						;RTM Rn

						;RTR

						;RTS

						;SATS.L Dr

	moveq.l	#MC68000|MC68010|MC68020|MC68030|MC68040,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb_drb	sbcd.b,$5be32b1a	;SBCD.B Dr,Dq
	xxx_mnb_mnb	sbcd.b,$6deaf0ef	;SBCD.B -(Ar),-(Aq)
@@:
	moveq.l	#MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	xxx_drb_drb	sbcd.b,$c3f1c658	;SBCD.B Dr,Dq
	xxx_mnb_mnb	sbcd.b,$bbbfb9b0	;SBCD.B -(Ar),-(Aq)
@@:

	xxx_drb		scc.b,$1896fbfe		;SCC.B Dr
	xxx_mmb		scc.b,$8d4dd5ea		;SCC.B (Ar)

	xxx_drb		scs.b,$f4c6fe17		;SCS.B Dr
	xxx_mmb		scs.b,$3d629b98		;SCS.B (Ar)

	xxx_drb		seq.b,$77c64350		;SEQ.B Dr
	xxx_mmb		seq.b,$19dcd0ab		;SEQ.B (Ar)

	xxx_drb		sf.b,$3bfa1a78		;SF.B Dr
	xxx_mmb		sf.b,$6b945d25		;SF.B (Ar)

	xxx_drb		sge.b,$0ea16591		;SGE.B Dr
	xxx_mmb		sge.b,$99b95b7a		;SGE.B (Ar)

	xxx_drb		sgt.b,$a0cd2b5a		;SGT.B Dr
	xxx_mmb		sgt.b,$2ebd6d8d		;SGT.B (Ar)

	xxx_drb		shi.b,$ad890137		;SHI.B Dr
	xxx_mmb		shi.b,$70687c52		;SHI.B (Ar)

	xxx_drb		sle.b,$4c9d2eb3		;SLE.B Dr
	xxx_mmb		sle.b,$9e9223ff		;SLE.B (Ar)

	xxx_drb		sls.b,$41d904de		;SLS.B Dr
	xxx_mmb		sls.b,$c0473220		;SLS.B (Ar)

	xxx_drb		slt.b,$e2f16078		;SLT.B Dr
	xxx_mmb		slt.b,$29961508		;SLT.B (Ar)

	xxx_drb		smi.b,$7d7fd466		;SMI.B Dr
	xxx_mmb		smi.b,$b5875e76		;SMI.B (Ar)

	xxx_drb		sne.b,$9b9646b9		;SNE.B Dr
	xxx_mmb		sne.b,$a9f39ed9		;SNE.B (Ar)

	xxx_drb		spl.b,$912fd18f		;SPL.B Dr
	xxx_mmb		spl.b,$05a81004		;SPL.B (Ar)

	xxx_drb		st.b,$d7aa1f91		;ST.B Dr
	xxx_mmb		st.b,$dbbb1357		;ST.B (Ar)

						;STOP #<data>

	xxx_drb_drb	sub.b,$e026d093		;SUB.B Dr,Dq
	xxx_drw_drw	sub.w,$f15cec4e		;SUB.W Dr,Dq
	xxx_drl_drl	sub.l,$85f764b5		;SUB.L Dr,Dq
	xxx_mmb_drb	sub.b,$1e48d395		;SUB.B (Ar),Dq
	xxx_mmw_drw	sub.w,$541a08b9		;SUB.W (Ar),Dq
	xxx_mml_drl	sub.l,$85f764b5		;SUB.L (Ar),Dq
	xxx_drb_mmb	sub.b,$85b7479b		;SUB.B Dq,(Ar)
	xxx_drw_mmw	sub.w,$1dd5201d		;SUB.W Dq,(Ar)
	xxx_drl_mml	sub.l,$85f764b5		;SUB.L Dq,(Ar)

	xxx_drw_arl	suba.w,$0810b638	;SUBA.W Dr,Aq
	xxx_drl_arl	suba.l,$27dd8548	;SUBA.L Dr,Aq
	xxx_mmw_arl	suba.w,$7ac1678d	;SUBA.W (Ar),Aq
	xxx_mml_arl	suba.l,$27dd8548	;SUBA.L (Ar),Aq

	xxx_imb_drb	subi.b,$d65133e8	;SUBI.B #<data>,Dr
	xxx_imw_drw	subi.w,$88156b4f	;SUBI.W #<data>,Dr
	xxx_iml_drl	subi.l,$37615ac6	;SUBI.L #<data>,Dr
	xxx_imb_mmb	subi.b,$efb5c6ae	;SUBI.B #<data>,(Ar)
	xxx_imw_mmw	subi.w,$3b31b243	;SUBI.W #<data>,(Ar)
	xxx_iml_mml	subi.l,$37615ac6	;SUBI.L #<data>,(Ar)

	xxx_imq_drb	subq.b,$9e9c1dce	;SUBQ.B #<data>,Dr
	xxx_imq_drw	subq.w,$3ea3f7bf	;SUBQ.W #<data>,Dr
	xxx_imq_drl	subq.l,$b606b45a	;SUBQ.L #<data>,Dr
	xxx_imq_arl	subq.w,$56d056e8	;SUBQ.W #<data>,Ar
	xxx_imq_arl	subq.l,$56d056e8	;SUBQ.L #<data>,Ar
	xxx_imq_mmb	subq.b,$4a079011	;SUBQ.B #<data>,(Ar)
	xxx_imq_mmw	subq.w,$bcc68d2f	;SUBQ.W #<data>,(Ar)
	xxx_imq_mml	subq.l,$b606b45a	;SUBQ.L #<data>,(Ar)

	xxx_drb_drb	subx.b,$4cea2db2	;SUBX.B Dr,Dq
	xxx_drw_drw	subx.w,$da10b7f5	;SUBX.W Dr,Dq
	xxx_drl_drl	subx.l,$e6850984	;SUBX.L Dr,Dq
	xxx_mnb_mnb	subx.b,$db4838f8	;SUBX.B -(Ar),-(Aq)
	xxx_mnw_mnw	subx.w,$73e47d63	;SUBX.W -(Ar),-(Aq)
	xxx_mnl_mnl	subx.l,$ddb76b81	;SUBX.L -(Ar),-(Aq)

	xxx_drb		svc.b,$4824ab8f		;SVC.B Dr
	xxx_mmb		svc.b,$47aa5829		;SVC.B (Ar)

	xxx_drb		svs.b,$a474ae66		;SVS.B Dr
	xxx_mmb		svs.b,$f785165b		;SVS.B (Ar)

	xxx_drl		swap.w,$cb6a3f12	;SWAP.W Dr

	xxx_drb		tas.b,$a93a3195		;TAS.B Dr
	xxx_mmb		tas.b,$439aa61b		;TAS.B (Ar)

						;TRAP #<vector>

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_no_operand	trapcc,$d068dcd9	;TRAPCC
	xxx_imw		trapcc.w,$b8f6ca00	;TRAPCC.W #<data>
	xxx_iml		trapcc.l,$b8f6ca00	;TRAPCC.L #<data>

	xxx_no_operand	trapcs,$dbfe4d21	;TRAPCS
	xxx_imw		trapcs.w,$1089e161	;TRAPCS.W #<data>
	xxx_iml		trapcs.l,$1089e161	;TRAPCS.L #<data>

	xxx_no_operand	trapeq,$2c8f5b99	;TRAPEQ
	xxx_imw		trapeq.w,$871107f7	;TRAPEQ.W #<data>
	xxx_iml		trapeq.l,$871107f7	;TRAPEQ.L #<data>

	xxx_no_operand	trapf,$91267e8a		;TRAPF
	xxx_imw		trapf.w,$103e182a	;TRAPF.W #<data>
	xxx_iml		trapf.l,$103e182a	;TRAPF.L #<data>

	xxx_no_operand	trapge,$2a86ffa4	;TRAPGE
	xxx_imw		trapge.w,$93f3826b	;TRAPGE.W #<data>
	xxx_iml		trapge.l,$93f3826b	;TRAPGE.L #<data>

	xxx_no_operand	trapgt,$7c831da5	;TRAPGT
	xxx_imw		trapgt.w,$846d51af	;TRAPGT.W #<data>
	xxx_iml		trapgt.l,$846d51af	;TRAPGT.L #<data>

	xxx_no_operand	traphi,$cca5d99b	;TRAPHI
	xxx_imw		traphi.w,$7c9b2e43	;TRAPHI.W #<data>
	xxx_iml		traphi.l,$7c9b2e43	;TRAPHI.L #<data>

	xxx_no_operand	traple,$77158c5d	;TRAPLE
	xxx_imw		traple.w,$2c127ace	;TRAPLE.W #<data>
	xxx_iml		traple.l,$2c127ace	;TRAPLE.L #<data>

	xxx_no_operand	trapls,$c7334863	;TRAPLS
	xxx_imw		trapls.w,$d4e40522	;TRAPLS.W #<data>
	xxx_iml		trapls.l,$d4e40522	;TRAPLS.L #<data>

	xxx_no_operand	traplt,$21106e5c	;TRAPLT
	xxx_imw		traplt.w,$3b8ca90a	;TRAPLT.W #<data>
	xxx_iml		traplt.l,$3b8ca90a	;TRAPLT.L #<data>

	xxx_no_operand	trapmi,$b6526333	;TRAPMI
	xxx_imw		trapmi.w,$28c717cf	;TRAPMI.W #<data>
	xxx_iml		trapmi.l,$28c717cf	;TRAPMI.L #<data>

	xxx_no_operand	trapne,$2719ca61	;TRAPNE
	xxx_imw		trapne.w,$2f6e2c96	;TRAPNE.W #<data>
	xxx_iml		trapne.l,$2f6e2c96	;TRAPNE.L #<data>

	xxx_no_operand	trappl,$bdc4f2cb	;TRAPPL
	xxx_imw		trappl.w,$80b83cae	;TRAPPL.W #<data>
	xxx_iml		trappl.l,$80b83cae	;TRAPPL.L #<data>

	xxx_no_operand	trapt,$9ab0ef72		;TRAPT
	xxx_imw		trapt.w,$b841334b	;TRAPT.W #<data>
	xxx_iml		trapt.l,$b841334b	;TRAPT.L #<data>
	.cpu	68000
@@:

	xxx_no_operand	trapv,$066473e5		;TRAPV

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_no_operand	trapvc,$0df2e21d	;TRAPVC
	xxx_imw		trapvc.w,$ab0a8d8e	;TRAPVC.W #<data>
	xxx_iml		trapvc.l,$ab0a8d8e	;TRAPVC.L #<data>

	xxx_no_operand	trapvs,$066473e5	;TRAPVS
	xxx_imw		trapvs.w,$0375a6ef	;TRAPVS.W #<data>
	xxx_iml		trapvs.l,$0375a6ef	;TRAPVS.L #<data>
	.cpu	68000
@@:

	xxx_drb		tst.b,$356c4487		;TST.B Dr
	xxx_drw		tst.w,$e407829d		;TST.W Dr
	xxx_drl		tst.l,$7f977b3a		;TST.L Dr
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_arw		tst.w,$e407829d		;TST.W Ar
	xxx_arl		tst.l,$7f977b3a		;TST.L Ar
	.cpu	68000
@@:
	xxx_mmb		tst.b,$8c78a8eb		;TST.B (Ar)
	xxx_mmw		tst.w,$ea178555		;TST.W (Ar)
	xxx_mml		tst.l,$7f977b3a		;TST.L (Ar)
	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_imb		tst.b,$30e1c1dc		;TST.B #<data>
	xxx_imw		tst.w,$30e1c1dc		;TST.W #<data>
	xxx_iml		tst.l,$30e1c1dc		;TST.L #<data>
	.cpu	68000
@@:

	unlk_arl	unlk,$7fcc9098		;UNLK Ar

	moveq.l	#MC68020|MC68030|MC68040|MC68060,d0
	and.b	mpu_type,d0
	beq	@f
	.cpu	68020
	xxx_drb_drw_imw	unpk,$8697e611		;UNPK Dr,Dq,#<data>
	xxx_mnb_mnw_imw	unpk,$b242ac11		;UNPK -(Ar),-(Aq),#<data>
	.cpu	68000
@@:

new_ctrlvc:
new_errjvc:
	clr.l	abort_illegal_instruction

	move.l	old_privilege_instruction,d0
	beq	@f
	move.l	d0,-(sp)
	move.w	#8,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
@@:

	move.l	old_trapv_instruction,d0
	beq	@f
	move.l	d0,-(sp)
	move.w	#7,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
@@:

	move.l	old_chk_instruction,d0
	beq	@f
	move.l	d0,-(sp)
	move.w	#6,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
@@:

	move.l	old_divide_by_zero,d0
	beq	@f
	move.l	d0,-(sp)
	move.w	#5,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
@@:

	move.l	old_illegal_instruction,d0
	beq	@f
	move.l	d0,-(sp)
	move.w	#4,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
@@:

	move.l	old_ctrlvc,-(sp)
	move.w	#_CTRLVC,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp
	move.l	old_errjvc,-(sp)
	move.w	#_ERRJVC,-(sp)
	DOS	_INTVCS
	addq.l	#6,sp

	jsr	test_end

exit:
	DOS	_EXIT

	.align	4
old_ctrlvc:
	.dc.l	0
old_errjvc:
	.dc.l	0

	.bss

mpu_type:
	.ds.b	1	;1=MC68000,2=MC68010,4=MC68020,8=MC68030,16=MC68040,64=MC68060

	.align	4
work_area_start:
	.ds.b	$8000*10
work_area:
	.ds.b	$8000*10
work_area_end:

	.align	4
	.ds.b	1024*64
stack_area_end:


;--------------------------------------------------------------------------------
;illegal instruction

	.text

	.align	4
abort_illegal_instruction:
	.dc.l	0
old_illegal_instruction:
	.dc.l	0
new_illegal_instruction:
	tst.l	abort_illegal_instruction
	bne	10f			;abort
	move.l	old_illegal_instruction,-(sp)
	rts
;abort
10:	move.l	abort_illegal_instruction,2(sp)	;catch
	ori.w	#%0001,(sp)		;set carry flag
	rte

;<carry flag:1=illegal instruction
print_illegal_instruction:
	bcc	@f
	pea.l	10f(pc)
	jsr	print_by_write
	addq.l	#4,sp
	addq.l	#1,test_failed
@@:	rts
10:	.dc.b	'illegal instruction',13,10,0
	.even


;--------------------------------------------------------------------------------
;divide by zero

	.text

	.align	4
old_divide_by_zero:
	.dc.l	0
new_divide_by_zero:
	andi.w	#%11001,(sp)		;clear zero flag and overflow flag
	eori.w	#%11111,(sp)		;invert ccr
	rte


;--------------------------------------------------------------------------------
;chk instruction

	.text

	.align	4
old_chk_instruction:
	.dc.l	0
new_chk_instruction:
	eori.w	#%11111,(sp)		;invert ccr
	rte


;--------------------------------------------------------------------------------
;trapv instruction

	.text

	.align	4
old_trapv_instruction:
	.dc.l	0
new_trapv_instruction:
	eori.w	#%11111,(sp)		;invert ccr
	rte


;--------------------------------------------------------------------------------
;privilege instruction
;	MOVE.W SR,<ea>					|-|-12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
;	MOVE.W CCR,<ea>					|-|-12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr

	.text

	.align	4
old_privilege_instruction:
	.dc.l	0
new_privilege_instruction:
	movem.l	d0-d1/a0,-(sp)
	movea.l	14(sp),a0		;pc
	moveq.l	#$ffffffc0,d0
	and.w	(a0),d0			;oc
	cmp.w	#$40c0,d0
	beq	10f			;move-from-sr
	movem.l	(sp)+,d0-d1/a0
	move.l	old_privilege_instruction,-(sp)
	rts

;move-from-sr
10:	ori.w	#$0200,(a0)		;modify move-from-sr to move-from-ccr
	tst.b	$0CBC.w
	beq	@f
	moveq.l	#3,d1			;cache flush
	IOCS	_SYS_STAT
@@:	movem.l	(sp)+,d0-d1/a0
	rte


;--------------------------------------------------------------------------------
;mnemonic

	.text
	.even

;<a0.l:mnemonic
;>d0.l:0=skip,1=test
;?a0-a1
mnemonic_check:
	movem.l	a0-a1,-(sp)
	lea.l	mnemonic_buffer,a1
	cmpi.l	#'all'<<8,(a1)
	beq	2f			;all
1:	tst.b	(a1)
	beq	2f
	cmpm.b	(a0)+,(a1)+
	beq	1b
	moveq.l	#0,d0			;skip
	bra	3f
2:	moveq.l	#1,d0			;test
3:	movem.l	(sp)+,a0-a1
	rts

	.bss

	.align	4
mnemonic_buffer:
	.ds.b	256+4


;--------------------------------------------------------------------------------
;result

	.text
	.even

test_start:
	clr.l	test_passed
	clr.l	test_failed
	rts

;<d1.l:expected crc32
;<CRC32_REG.l:actual crc32
test_check:
	move.l	d1,d0			;expected crc32
	jsr	print_hex8
	print	'   '
	move.l	CRC32_REG,d0		;actual crc32
	jsr	print_hex8
	cmp.l	CRC32_REG,d1
	bne	1f
	print	'   OK',13,10
	addq.l	#1,test_passed
	bra	2f
1:	print	'   ERROR',13,10
	addq.l	#1,test_failed
2:	rts

test_end:
	movem.l	d0-d3,-(sp)
	move.l	test_passed,d1		;passed
	move.l	test_failed,d2		;failed
	move.l	d1,d3
	add.l	d2,d3			;tested
	print	'tested:'
	move.l	d3,d0			;tested
	bsr	print_dec
	tst.l	d3
	beq	8f			;no tests were performed
	print	', passed:'
	move.l	d1,d0			;passed
	bsr	print_dec
	print	'('
	mulu.w	#100,d0
	divu.w	d3,d0			;100*passed/tested
	ext.l	d0
	bsr	print_dec
	print	'%), failed:'
	move.l	d2,d0			;failed
	bsr	print_dec
	print	'('
	mulu.w	#100,d0
	divu.w	d3,d0			;100*failed/tested
	ext.l	d0
	bsr	print_dec
	print	'%)'
8:	print	13,10
	movem.l	(sp)+,d0-d3
	rts

	.bss
	.align	4
test_passed:
	.ds.l	1
test_failed:
	.ds.l	1


;--------------------------------------------------------------------------------
;crc32

	.text
	.even

  .if 0
crc32_test:
	move.l	d0,-(sp)
;crc32('A')=$d3d99e8b
	jsr	crc32_reset
	moveq.l	#'A',d0
	jsr	crc32_byte
	jsr	crc32_print
	jsr	print_crlf
;crc32('ABCD')=$db1720a5
	jsr	crc32_reset
	move.l	#'ABCD',d0
	jsr	crc32_long
	jsr	crc32_print
	jsr	print_crlf
	move.l	(sp)+,d0
	rts

crc32_print:
	move.l	d0,-(sp)
	move.l	CRC32_REG,d0
	jsr	print_hex8
	move.l	(sp)+,d0
	rts
  .endif

crc32_reset:
	moveq.l	#0,CRC32_REG
	rts

;<d0.b:data
;<CRC32_REG.l:crc32
;>CRC32_REG.l:crc32
;?d0
crc32_byte:
	move.l	d0,-(sp)
	not.l	CRC32_REG
	eor.b	d0,CRC32_REG
	moveq.l	#0,d0
	move.b	CRC32_REG,d0
	lsr.l	#8,CRC32_REG
	lsl.w	#2,d0
	move.l	crc32_table(pc,d0.w),d0
	eor.l	d0,CRC32_REG
	not.l	CRC32_REG
	move.l	(sp)+,d0
	rts

;<d0.w:data
;<CRC32_REG.l:crc32
;>CRC32_REG.l:crc32
crc32_word:
	movem.l	d0-d1,-(sp)
	not.l	CRC32_REG
	moveq.l	#-2,d1
1:	moveq.l	#0,d0
	move.b	4(sp,d1.w),d0
	eor.b	CRC32_REG,d0
	lsr.l	#8,CRC32_REG
	lsl.w	#2,d0
	move.l	crc32_table(pc,d0.w),d0
	eor.l	d0,CRC32_REG
	addq.w	#1,d1
	bne	1b
	not.l	CRC32_REG
	movem.l	(sp)+,d0-d1
	rts

;<d0.l:data
;<CRC32_REG.l:crc32
;>CRC32_REG.l:crc32
crc32_long:
	movem.l	d0-d1,-(sp)
	not.l	CRC32_REG
	moveq.l	#-4,d1
1:	moveq.l	#0,d0
	move.b	4(sp,d1.w),d0
	eor.b	CRC32_REG,d0
	lsr.l	#8,CRC32_REG
	lsl.w	#2,d0
	move.l	crc32_table(pc,d0.w),d0
	eor.l	d0,CRC32_REG
	addq.w	#1,d1
	bne	1b
	not.l	CRC32_REG
	movem.l	(sp)+,d0-d1
	rts

	.align	4
crc32_table:
	.dc.l	$00000000,$77073096,$ee0e612c,$990951ba,$076dc419,$706af48f,$e963a535,$9e6495a3
	.dc.l	$0edb8832,$79dcb8a4,$e0d5e91e,$97d2d988,$09b64c2b,$7eb17cbd,$e7b82d07,$90bf1d91
	.dc.l	$1db71064,$6ab020f2,$f3b97148,$84be41de,$1adad47d,$6ddde4eb,$f4d4b551,$83d385c7
	.dc.l	$136c9856,$646ba8c0,$fd62f97a,$8a65c9ec,$14015c4f,$63066cd9,$fa0f3d63,$8d080df5
	.dc.l	$3b6e20c8,$4c69105e,$d56041e4,$a2677172,$3c03e4d1,$4b04d447,$d20d85fd,$a50ab56b
	.dc.l	$35b5a8fa,$42b2986c,$dbbbc9d6,$acbcf940,$32d86ce3,$45df5c75,$dcd60dcf,$abd13d59
	.dc.l	$26d930ac,$51de003a,$c8d75180,$bfd06116,$21b4f4b5,$56b3c423,$cfba9599,$b8bda50f
	.dc.l	$2802b89e,$5f058808,$c60cd9b2,$b10be924,$2f6f7c87,$58684c11,$c1611dab,$b6662d3d
	.dc.l	$76dc4190,$01db7106,$98d220bc,$efd5102a,$71b18589,$06b6b51f,$9fbfe4a5,$e8b8d433
	.dc.l	$7807c9a2,$0f00f934,$9609a88e,$e10e9818,$7f6a0dbb,$086d3d2d,$91646c97,$e6635c01
	.dc.l	$6b6b51f4,$1c6c6162,$856530d8,$f262004e,$6c0695ed,$1b01a57b,$8208f4c1,$f50fc457
	.dc.l	$65b0d9c6,$12b7e950,$8bbeb8ea,$fcb9887c,$62dd1ddf,$15da2d49,$8cd37cf3,$fbd44c65
	.dc.l	$4db26158,$3ab551ce,$a3bc0074,$d4bb30e2,$4adfa541,$3dd895d7,$a4d1c46d,$d3d6f4fb
	.dc.l	$4369e96a,$346ed9fc,$ad678846,$da60b8d0,$44042d73,$33031de5,$aa0a4c5f,$dd0d7cc9
	.dc.l	$5005713c,$270241aa,$be0b1010,$c90c2086,$5768b525,$206f85b3,$b966d409,$ce61e49f
	.dc.l	$5edef90e,$29d9c998,$b0d09822,$c7d7a8b4,$59b33d17,$2eb40d81,$b7bd5c3b,$c0ba6cad
	.dc.l	$edb88320,$9abfb3b6,$03b6e20c,$74b1d29a,$ead54739,$9dd277af,$04db2615,$73dc1683
	.dc.l	$e3630b12,$94643b84,$0d6d6a3e,$7a6a5aa8,$e40ecf0b,$9309ff9d,$0a00ae27,$7d079eb1
	.dc.l	$f00f9344,$8708a3d2,$1e01f268,$6906c2fe,$f762575d,$806567cb,$196c3671,$6e6b06e7
	.dc.l	$fed41b76,$89d32be0,$10da7a5a,$67dd4acc,$f9b9df6f,$8ebeeff9,$17b7be43,$60b08ed5
	.dc.l	$d6d6a3e8,$a1d1937e,$38d8c2c4,$4fdff252,$d1bb67f1,$a6bc5767,$3fb506dd,$48b2364b
	.dc.l	$d80d2bda,$af0a1b4c,$36034af6,$41047a60,$df60efc3,$a867df55,$316e8eef,$4669be79
	.dc.l	$cb61b38c,$bc66831a,$256fd2a0,$5268e236,$cc0c7795,$bb0b4703,$220216b9,$5505262f
	.dc.l	$c5ba3bbe,$b2bd0b28,$2bb45a92,$5cb36a04,$c2d7ffa7,$b5d0cf31,$2cd99e8b,$5bdeae1d
	.dc.l	$9b64c2b0,$ec63f226,$756aa39c,$026d930a,$9c0906a9,$eb0e363f,$72076785,$05005713
	.dc.l	$95bf4a82,$e2b87a14,$7bb12bae,$0cb61b38,$92d28e9b,$e5d5be0d,$7cdcefb7,$0bdbdf21
	.dc.l	$86d3d2d4,$f1d4e242,$68ddb3f8,$1fda836e,$81be16cd,$f6b9265b,$6fb077e1,$18b74777
	.dc.l	$88085ae6,$ff0f6a70,$66063bca,$11010b5c,$8f659eff,$f862ae69,$616bffd3,$166ccf45
	.dc.l	$a00ae278,$d70dd2ee,$4e048354,$3903b3c2,$a7672661,$d06016f7,$4969474d,$3e6e77db
	.dc.l	$aed16a4a,$d9d65adc,$40df0b66,$37d83bf0,$a9bcae53,$debb9ec5,$47b2cf7f,$30b5ffe9
	.dc.l	$bdbdf21c,$cabac28a,$53b39330,$24b4a3a6,$bad03605,$cdd70693,$54de5729,$23d967bf
	.dc.l	$b3667a2e,$c4614ab8,$5d681b02,$2a6f2b94,$b40bbe37,$c30c8ea1,$5a05df1b,$2d02ef8d


;--------------------------------------------------------------------------------
;output

	.text
	.even

print_dec:
	movem.l	d0-d2/a0-a1,-(sp)
	lea.l	-12(sp),sp
	movea.l	sp,a0
	tst.l	d0
	bne	1f
	move.b	#'0',(a0)+
	bra	5f
1:	lea.l	10f(pc),a1
2:	move.l	(a1)+,d1
	cmp.l	d1,d0
	blo	2b
3:	moveq.l	#'0'-1,d2
4:	addq.b	#1,d2
	sub.l	d1,d0
	bcc	4b
	add.l	d1,d0
	move.b	d2,(a0)+
	move.l	(a1)+,d1
	bne	3b
5:	suba.l	sp,a0
	move.l	a0,-(sp)
	pea.l	4(sp)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	10+12(sp),sp
	movem.l	(sp)+,d0-d2/a0-a1
	rts
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

print_hex8:
	movem.l	d0-d2/a0,-(sp)
	lea.l	-10(sp),sp
	movea.l	sp,a0
	move.b	#'$',(a0)+
	moveq.l	#8-1,d2
2:	rol.l	#4,d0
	moveq.l	#15,d1
	and.w	d0,d1
	move.b	10f(pc,d1.w),(a0)+
	dbra	d2,2b
	pea.l	1+8.w
	pea.l	4(sp)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	10+10(sp),sp
	movem.l	(sp)+,d0-d2/a0
	rts
10:	.dc.b	'0123456789abcdef'

print_crlf:
	move.l	d0,-(sp)
	pea.l	2.w
	pea.l	10f(pc)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	10(sp),sp
	move.l	(sp)+,d0
	rts
10:	.dc.b	13,10
	.even

putchar_by_write:
	move.l	d0,-(sp)
	pea.l	1.w
	pea.l	12+1(sp)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	10(sp),sp
	move.l	(sp)+,d0
	rts

print_by_write:
	movem.l	d0/a0-a1,-(sp)
	movea.l	16(sp),a1
	movea.l	a1,a0
@@:	tst.b	(a1)+
	bne	@b
	subq.l	#1,a1
	suba.l	a0,a1
	movem.l	a0-a1,-(sp)
	move.w	#1,-(sp)
	DOS	_WRITE
	lea.l	10(sp),sp
	movem.l	(sp)+,d0/a0-a1
	rts


;--------------------------------------------------------------------------------
;ccr data

	.data

	.align	4
ccr_short_data:
	.dc.w	0,31
;sentry
	.dc.w	0

	.align	4
ccr_long_data:
	.dc.w	0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
;sentry
	.dc.w	0

	.align	4
ccr_data:
	.dc.w	%00000,%11111,%00001,%11110,%00010,%11101,%00100,%11011,%01000,%10111,%10000,%01111
;sentry
	.dc.w	0


;--------------------------------------------------------------------------------
;bit field data

	.data

	.align	4
bf_offset_data:
	.dc.l	0,1,7,8,9,15,16,17,23,24,25,31	;zero must appear first
	.dc.l	32,33,39,40,41,47,48,49,55,56,57,63
	.dc.l	-32,-31,-25,-24,-23,-17,-16,-15,-9,-8,-7,-1
;sentry
	.dc.l	0

bf_width_data:
	.dc.l	1,7,8,9,15,16,17,23,24,25,31,32
;sentry
	.dc.l	0


;--------------------------------------------------------------------------------
;byte data

put_byte	.macro	x
	.dc.l	x
	.dc.l	.notb.(x)
	.dc.l	$ffffff00+(x)
	.dc.l	$ffffff00+.notb.(x)
	.endm

	.data

	.align	4
byte_short_data:
;no one
	put_byte	0
;sentry
	.dc.l	0

	.align	4
byte_data:
;no one
	put_byte	0
;one one
  .irp i,0,1,7
	put_byte	1<<i
  .endm
;two ones
  .irp i,0,1,7
    .irp j,0,1,7
      .if i>j
	put_byte	(1<<i)+(1<<j)
      .endif
    .endm
  .endm
;three or more consecutive ones
  .irp i,0,1,7
    .irp j,0,1,7
      .if i>j+2
	put_byte	(2<<i)-(1<<j)
      .endif
    .endm
  .endm
;sentry
	.dc.l	0


;--------------------------------------------------------------------------------
;word data

put_word	.macro	x
	.dc.l	x
	.dc.l	.notw.(x)
	.dc.l	$ffff0000+(x)
	.dc.l	$ffff0000+.notw.(x)
	.endm

	.data

	.align	4
word_short_data:
;no one
	put_word	0
;sentry
	.dc.l	0

	.align	4
word_data:
;no one
	put_word	0
;one one
  .irp i,0,1,7,8,9,15
	put_word	1<<i
  .endm
;two ones
  .irp i,0,1,7,8,9,15
    .irp j,0,1,7,8,9,15
      .if i>j
	put_word	(1<<i)+(1<<j)
      .endif
    .endm
  .endm
;three or more consecutive ones
  .irp i,0,1,7,8,9,15
    .irp j,0,1,7,8,9,15
      .if i>j+2
	put_word	(2<<i)-(1<<j)
      .endif
    .endm
  .endm
;sentry
	.dc.l	0


;--------------------------------------------------------------------------------
;long data

put_long	.macro	x
	.dc.l	x
	.dc.l	.not.(x)
	.endm

	.data

	.align	4
long_short_data:
;no one
	put_long	0
;sentry
	.dc.l	0

	.align	4
long_data:
;no one
	put_long	0
;one one
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31
	put_long	1<<i
  .endm
;two ones
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31
    .irp j,0,1,7,8,9,15,16,17,23,24,25,31
      .if i>j
	put_long	(1<<i)+(1<<j)
      .endif
    .endm
  .endm
;three or more consecutive ones
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31
    .irp j,0,1,7,8,9,15,16,17,23,24,25,31
      .if i>j+2
	put_long	(2<<i)-(1<<j)
      .endif
    .endm
  .endm
;sentry
	.dc.l	0


;--------------------------------------------------------------------------------
;quad data

put_quad	.macro	xh,xl
	.dc.l	xh,xl
	.dc.l	.not.(xh),.not.(xl)
	.endm

	.data

	.align	4
quad_short_data:
;no one
	put_quad	0,0
;sentry
	.dc.l	0,0

	.align	4
quad_data:
;no one
	put_quad	0,0
;one one
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31,32,33,39,40,41,47,48,49,55,56,57,63
    .if i<32
	put_quad	0,1<<i
    .else
	put_quad	1<<(i-32),0
    .endif
  .endm
  .if 0
;two ones
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31,32,33,39,40,41,47,48,49,55,56,57,63
    .irp j,0,1,7,8,9,15,16,17,23,24,25,31,32,33,39,40,41,47,48,49,55,56,57,63
      .if i>j
        .if i<32
	put_long	0,(1<<i)+(1<<j)
        .elif j<32
	put_long	1<<(i-32),1<<j
        .else
	put_long	(1<<(i-32))+(1<<(j-32)),0
        .endif
      .endif
    .endm
  .endm
  .endif
;sentry
	.dc.l	0,0


;--------------------------------------------------------------------------------
;sext data

put_sext	.macro	xh,xm,xl
	.dc.l	xh,xm,xl
	.dc.l	.not.(xh),.not.(xm),.not.(xl)
	.endm

	.data

	.align	4
sext_short_data:
;no one
	put_sext	0,0,0
;sentry
	.dc.l	0,0,0

	.align	4
sext_data:
;no one
	put_sext	0,0,0
;one one
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31,32,33,39,40,41,47,48,49,55,56,57,63,64,65,71,72,73,79,80,81,87,88,89,95
    .if i<32
	put_sext	0,0,1<<i
    .elif i<64
	put_sext	0,1<<(i-32),0
    .else
	put_sext	1<<(i-64),0,0
    .endif
  .endm
;sentry
	.dc.l	0,0,0


;--------------------------------------------------------------------------------
;oct data

put_oct	.macro	x0,x1,x2,x3
	.dc.l	x0,x1,x2,x3
	.dc.l	.not.(x0),.not.(x1),.not.(x2),.not.(x3)
	.endm

	.data

	.align	4
oct_short_data:
;no one
	put_oct	0,0,0,0
;sentry
	.dc.l	0,0,0,0

	.align	4
oct_data:
;no one
	put_oct	0,0,0,0
;one one
  .irp i,0,1,7,8,9,15,16,17,23,24,25,31,32,33,39,40,41,47,48,49,55,56,57,63,64,65,71,72,73,79,80,81,87,88,89,95,96,97,103,104,105,111,112,113,119,120,121,127
    .if i<32
	put_oct	0,0,0,1<<i
    .elif i<64
	put_oct	0,0,1<<(i-32),0
    .elif i<96
	put_oct	0,1<<(i-64),0,0
    .else
	put_oct	1<<(i-96),0,0,0
    .endif
  .endm
;sentry
	.dc.l	0,0,0,0


;--------------------------------------------------------------------------------

	.end	main

