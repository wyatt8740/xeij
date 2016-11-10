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
;	ROM 1.3�̃p�b�`
;
;----------------------------------------------------------------

	.include	iocscall.mac

	.include	def_M.equ
	.include	rompat.mac

ROM_VERSION	equ	$15160816	;ROM�̃o�[�W����



;----------------------------------------------------------------
;
;	�p�b�`�f�[�^�̐擪
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
;	���䃌�W�X�^�̏�������MPU/MMU/FPU�̃`�F�b�N
;
;----------------------------------------------------------------

	PATCH_DATA	mpuCheck,$00FF005A

;�T�u���[�`�����A�h���X�ϊ��𖳌��ɂ���ƃX�^�b�N�G���A���ړ����Ė߂��Ă���Ȃ��Ȃ�̂�jsr�ł͂Ȃ�jmp���g��
	jmp	mpuCheck

	PATCH_EXTRA

;----------------------------------------------------------------
;MPU�`�F�b�N
;	�N������Ɋ��荞�݋֎~�A�X�^�b�N���g�p�̏�ԂŃW�����v���Ă���
;	ROM��TRAP#10�̓z�b�g�X�^�[�g����Ƃ�MMU�̃A�h���X�ϊ��̏�Ԃ��m�F������$00FF0038�ɃW�����v���Ă���
;	060turbo.sys��TRAP#10���t�b�N���Ă���̂Ńz�b�g�X�^�[�g�����ꍇ���A�h���X�ϊ��͊��ɖ���������Ă���͂������A
;	�O�̂��߃A�h���X�ϊ����L���ɂȂ��Ă����ꍇ���l������
;	ROM�̃R�[�h�͕ω����Ȃ��̂Ŗ��Ȃ����A
;	�x�N�^�e�[�u���ƃX�^�b�N�G���A�ƃ��[�N�G���A�̓��e�̓A�h���X�ϊ��𖳌��ɂ����u�ԂɎ�����
;	�z�b�g�X�^�[�g������X�^�b�N�G���A�⃏�[�N�G���A���g���O�ɃA�h���X�ϊ��𖳌��ɂ��Ȃ���΂Ȃ�Ȃ�
;<d5.l:0=�R�[���h�X�^�[�g(d0d1!='HotStart'),-1=�z�b�g�X�^�[�g(d0d1=='HotStart')
;<d6.l:�G�~�����[�^���荞�݃x�N�^([$0030.w].l)�B�R�[���h�X�^�[�g�̂Ƃ�����`��O�������[�`�����w���Ă��Ȃ���Γd��ON�A�w���Ă���΃��Z�b�g
;>d7.l:MPU/MMU/FPU�̃`�F�b�N�̌���
;	bit31:0=MMU�Ȃ�,1=MMU����
;	bit15:0=FPU/FPCP�Ȃ�,1=FPU/FPCP����
;	bit7-0:0=MC68000,1=MC68010,2=MC68020,3=MC68030,4=MC68040,6=MC68060
;>a0.l:$00000400
;>a1.l:$00FF0770
;?d0
mpuCheck:
	lea.l	dummyTrap(pc),a1
	move.l	a1,$0010.w		;[$0010.w].l:��O�x�N�^$04 �s������
	move.l	a1,$002C.w		;[$002C.w].l:��O�x�N�^$0B ���C��1111�G�~�����[�^
	movea.l	sp,a1
;----------------------------------------------------------------
;MOVEC to VBR(-12346)���Ȃ����MC68000
;	MC68000���ǂ����̃e�X�g��VBR�̃N���A�𓯎��ɍs��
;	�s�����ߗ�O�x�N�^��$0010.w�ɂ���̂�MC68000�̏ꍇ��MC68010�ȏ��VBR��0�̏ꍇ
;	MC68010�ȏ��VBR��0�łȂ��Ƃ�$0010.w�����������Ă��s�����ߗ�O�𑨂����Ȃ��̂ŁA
;	MC68010�ȏ�̂Ƃ��͍ŏ���VBR���N���A����K�v������
;	VBR���N���A���閽�߂�MC68000�ŕs�����߂Ȃ̂�MC68000���ǂ����̃e�X�g�����˂�
	.cpu	68010
	lea.l	mpuCheckDone(pc),a0
	moveq.l	#0,d7			;MC68000
	moveq.l	#0,d0
	movec.l	d0,vbr
	.cpu	68000
;----------------------------------------------------------------
;MOVEC to VBR(-12346)�������ăX�P�[���t�@�N�^(--2346)���Ȃ����MC68010
	.cpu	68020
	moveq.l	#1,d7			;MC68010
@@:	moveq.l	#1,d0			;$70,$01
	and.b	(@b-1,pc,d0.w*2),d0	;�X�P�[���t�@�N�^�Ȃ�(([@b-1+1].b==$70)&1)==0,�X�P�[���t�@�N�^����(([@b-1+1*2].b=$01)&1)==1
	beq	mpuCheckDone
	.cpu	68000
;----------------------------------------------------------------
;CALLM(--2---)�������MC68020
	.cpu	68020
	lea.l	9f(pc),a0
	callm	#0,1f(pc)
	moveq.l	#2,d7			;MC68020
;		  3  2 1 0
;		  C CE F E
	move.l	#%1__0_0_0,d0
	movec.l	d0,cacr			;���߃L���b�V���N���A�A���߃L���b�V��OFF
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPU����
@@:
;MC68851�̃`�F�b�N�͏ȗ�
	bra	mpuCheckDone
;���W���[���f�X�N���v�^
1:	.dc.l	%0<<13|0<<24|0<<16	;option=0,type=0,accesslevel=0
	.dc.l	2f			;���W���[���G���g���|�C���^
	.dc.l	0			;���W���[���f�[�^�̈�|�C���^
	.dc.l	0
;���W���[���G���g��
2:	.dc.w	15<<12			;Rn=sp
	rtm	sp
9:	movea.l	a1,sp
	.cpu	68000
;----------------------------------------------------------------
;CALLM(--2---)���Ȃ���MOVEC from CAAR(--23--)�������MC68030
	.cpu	68030
	lea.l	9f(pc),a0
	movec.l	caar,d0
	moveq.l	#3,d7			;MC68030
;		   D   C  B   A  9  8 765   4  3   2  1  0
;		  WA DBE CD CED FD ED     IBE CI CEI FI EI
	move.l	#%_0___0__1___0__0__0_000___0__1___0__0__0,d0
	movec.l	d0,cacr			;�f�[�^�L���b�V���N���A�A�f�[�^�L���b�V��OFF�A���߃L���b�V���N���A�A���߃L���b�V��OFF
;		  F ECDBA   9   8 7654 3210 FEDC BA98 7654 3210
;		  E       SRE FCL   PS   IS  TIA  TIB  TIC  TID
	move.l	#%0_00000___0___0_1101_1000_0011_0100_0100_0000,-(sp)
	pmove.l	(sp),tc			;�A�h���X�ϊ�OFF
;		  FEDCBA98 76543210 F EDCB  A   9   8 7    654 3    210
;		      BASE     MASK E      CI R/W RWM   FCBASE   FCMASK
	move.l	#%00000000_00000000_0_0000__0___0___0_0____000_0____000,(sp)
	pmove.l	(sp),tt0		;���ߕϊ�OFF
	pmove.l	(sp),tt1
	addq.l	#4,sp
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPU����
@@:	bsr	mmuCheck3		;MMU�`�F�b�N(MC68030)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;MOVEC from MMUSR(----4-)�������MC68040
	.cpu	68040
	lea.l	9f(pc),a0
	movec.l	mmusr,d0
	moveq.l	#4,d7			;MC68040
;		   F EDCBA9876543210  F EDCBA9876543210
;		  DE 000000000000000 IE 000000000000000
	move.l	#%_0_000000000000000__0_000000000000000,d0
	movec.l	d0,cacr			;�f�[�^�L���b�V��OFF�A���߃L���b�V��OFF
	cinva	bc			;�f�[�^�L���b�V���N���A�A���߃L���b�V���N���A
;		  F E DCBA9876543210
;		  E P 00000000000000
	move.l	#%0_0_00000000000000,d0
	movec.l	d0,tc			;�A�h���X�ϊ�����
;		  FEDCBA98 76543210 F     ED CBA  9  8 7 65 43 2 10
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	move.l	#%00000000_00000000_0_____00_000__0__0_0_00_00_0_00,d0
	movec.l	d0,itt0			;���ߕϊ�OFF
	movec.l	d0,itt1
	movec.l	d0,dtt0
	movec.l	d0,dtt1
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPU����
@@:	bsr	mmuCheck46		;MMU�`�F�b�N(MC68040/MC68060)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;MOVEC from PCR(-----6)�������MC68060
	.cpu	68060
	lea.l	9f(pc),a0
	movec.l	pcr,d1
	moveq.l	#6,d7			;MC68060
;		    F   E   D   C   B A98   7    6    5 43210   F   E   D CBA9876543210
;		  EDC NAD ESB DPI FOC     EBC CABC CUBC       EIC NAI FIC
	move.l	#%__0___0___0___0___0_000___1____0____0_00000___0___0___0_0000000000000,d0
	movec.l	d0,cacr			;�f�[�^�L���b�V��OFF�A�X�g�A�o�b�t�@OFF�A����L���b�V��ON�A���߃L���b�V��OFF
	cinva	bc			;�f�[�^�L���b�V���N���A�A���߃L���b�V���N���A
;		  F E   D   C    B    A  98  76   5  43  21 0
;		  E P NAD NAI FOTC FITC DCO DUO DWO DCI DUI
	move.l	#%0_0___0___0____0____0__10__00___0__10__00_0,d0
	movec.l	d0,tc			;�A�h���X�ϊ�OFF�A�f�[�^�L���b�V��OFF�v���T�C�X���[�h�A���߃L���b�V��OFF�v���T�C�X���[�h
;		  FEDCBA98 76543210 F     ED CBA  9  8 7 65 43 2 10
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	move.l	#%00000000_00000000_0_____00_000__0__0_0_00_00_0_00,d0
	movec.l	d0,itt0			;���ߕϊ�OFF
	movec.l	d0,itt1
	movec.l	d0,dtt0
	movec.l	d0,dtt1
;		  FEDCBA9876543210 FEDCBA98      7 65432   1   0
;		  0000010000110000 REVISION EDEBUG       DFP ESS
	move.l	#%0000000000000000_00000000______0_00000___0___1,d0
	movec.l	d0,pcr			;FPU ON,�X�[�p�[�X�J��ON
	lea.l	@f(pc),a0
	fnop
	bset.l	#15,d7			;FPU����
@@:	bsr	mmuCheck46		;MMU�`�F�b�N(MC68040/MC68060)
	bra	mpuCheckDone
9:
	.cpu	68000
;----------------------------------------------------------------
;�s��
	moveq.l	#0,d7
;----------------------------------------------------------------
;�I��
mpuCheckDone:

;��O�x�N�^�e�[�u��������������
;	$0010.w��$002C.w�͂����ŏ㏑�������
	suba.l	a0,a0
	lea.l	$00FF0770.l,a1		;����`��O����
	moveq.l	#1,d1
	ror.l	#8,d1
	moveq.l	#256/2-1,d0
@@:
  .rept 2
	move.l	a1,(a0)+
	adda.l	d1,a1
  .endm
	dbra.w	d0,@b
	move.l	#unimplementedIntegerInstruction,$00F4.w	;[$00F4.w].l:��O�x�N�^$3D ��������������
	jmp	$00FF007E

;----------------------------------------------------------------
;�_�~�[�̗�O����
;	a1��sp�ɃR�s�[����a0�ɃW�����v����
dummyTrap:
	movea.l	a1,sp
	jmp	(a0)

;----------------------------------------------------------------
;MMU�`�F�b�N(MC68030)
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
;�A�h���X�ϊ���L���ɂ���
;			      CI   M U WP DT
	move.l	#$00F00000|%0__0_0_0_0__0_01,-4*8+4(a1)	;$00200000��$00F00000�ɕϊ�����
	pflusha				;MMU���Ȃ��Ă��G���[�ɂȂ�Ȃ�
	pmove.q	(sp),crp
;		  E       SRE FCL   PS   IS  TIA  TIB  TIC  TID
	move.l	#%1_00000___0___0_1101_1000_0011_0100_0100_0000,-(sp)
	pmove.l	(sp),tc
;8KB��r����
	lea.l	$00F00000,a1
	lea.l	$00200000,a0
	move.w	#2048-1,d0
@@:	cmpm.l	(a1)+,(a0)+
	dbne	d0,@b
	bne	@f
	bset.l	#31,d7			;MMU����
@@:
;�A�h���X�ϊ�����������
	bclr.b	#7,(sp)			;E=0
	pmove.l	(sp),tc			;�A�h���X�ϊ�����
	lea.l	4+128(sp),sp
	.cpu	68000
	rts

;----------------------------------------------------------------
;MMU�`�F�b�N(MC68040/MC68060)
mmuCheck46:
	.cpu	68040
	lea.l	$2000.w,a1
;���[�g�e�[�u�������
;			   U W UDT
	lea.l	512|%00000_0_0__10(a1),a0
	moveq.l	#128-1,d0
@@:	move.l	a0,(a1)+
	dbra	d0,@b
;�|�C���^�e�[�u�������
;			   U W UDT
	lea.l	512|%00000_0_0__10(a1),a0
	moveq.l	#128-1,d0
@@:	move.l	a0,(a1)+
	dbra	d0,@b
;�y�[�W�e�[�u�������
	moveq.l	#32-1,d0
;			    UR G U1 U0 S CM M U W PDT
@@:	move.l	#$00FF0000|%00_1__0__0_0_10_0_0_0__01,(a1)+
	dbra	d0,@b
;$80000000�`$FFFFFFFF�𓧉ߕϊ��ɂ���
;		      BASE     MASK E SFIELD     U1 U0   CM    W
	movea.l	#%00000000_01111111_1_____10_000__0__0_0_10_00_0_00,a1
	movec.l	a1,itt0
	movec.l	a1,dtt0
	movec.l	a1,itt1
	movec.l	a1,dtt1
;�A�h���X�ϊ���L���ɂ���
	lea.l	$2000.w,a1
	movec.l	a1,srp			;MMU���Ȃ��Ă��G���[�ɂȂ�Ȃ�(APPENDIX B-1)
	movec.l	a1,urp
;		  E P NAD NAI FOTC FITC DCO DUO DWO DCI DUI
	movea.l	#%1_1___0___0____0____0__00__00___0__00__00_0,a1
	movec.l	a1,tc
	pflusha
	cinva	bc
;8KB��r����
	lea.l	$80FF0000,a0
	lea.l	$80F00000,a1
	move.w	#2048-1,d0
@@:	cmpm.l	(a0)+,(a1)+
	dbne	d0,@b
	bne	@f
	bset.l	#31,d7			;MMU����
@@:
;�A�h���X�ϊ��ƃg�����X�y�A�����g�ϊ�����������
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
;�������������ߗ�O�������[�`��
	.cpu	68060
unimplementedIntegerInstruction:
	pea.l	(8,sp)			;��O��������ssp
	movem.l	d0-d7/a0-a6,-(sp)
	moveq.l	#5,d2			;�X�[�p�[�o�C�U�f�[�^�A�N�Z�X�̃t�@���N�V�����R�[�h
	btst.b	#5,(4*16,sp)		;��O��������sr��S
	bne	1f			;�X�[�p�[�o�C�U���[�h
;���[�U���[�h
	moveq.l	#1,d2			;���[�U�f�[�^�A�N�Z�X�̃t�@���N�V�����R�[�h
	move.l	usp,a0			;��O��������usp
	move.l	a0,(4*15,sp)		;��O��������sp
;���߃R�[�h��ǂݎ��
1:	movea.l	(4*16+2,sp),a0		;a0=��O��������pc
	move.w	(a0)+,d1		;d1=���߃R�[�h,a1=pc+2
;MOVEP���ǂ������ׂ�
	move.w	d1,d0
;		  0000qqq1ws001rrr
	and.w	#%1111000100111000,d0
	cmp.w	#%0000000100001000,d0
	beq	2f			;MOVEP
;MOVEP�ȊO
	movem.l	(sp),d0-d2		;d0/d1/d2�𕜌�
	movea.l	(4*8,sp),a0		;a0�𕜌�
	lea.l	(4*16,sp),sp		;�j�󂳂�Ă��Ȃ����W�X�^�̕������ȗ�����
	jmp	$00FF0770		;����`��O����

;MOVEP
;�����A�h���X�����߂�
2:	moveq.l	#7,d0			;d0=0000000000000111
	and.w	d1,d0			;d0=0000000000000rrr
	movea.l	(4*8,sp,d0.w*4),a1	;a1=Ar
	adda.w	(a0)+,a1		;a0=pc+4,a1=d16+Ar=�����A�h���X
;���A�A�h���X���X�V����
	move.l	a0,(4*16+2,sp)		;pc=pc+4
;Dq�̃A�h���X�����߂�
	move.w	d1,d0			;d0=0000qqq1ws001rrr
	lsr.w	#8,d0			;d0=000000000000qqq1
	lea.l	(-2,sp,d0.w*2),a0	;d0*2=00000000000qqq10,a0=Dq�̃A�h���X
;���[�h/���C�g,���[�h/�����O�ŕ��򂷂�
	add.b	d1,d1			;c=w,d1=s001rrr0
	bcs	5f			;���C�g
;���[�h
	movec.l	sfc,d1			;�t�@���N�V�����R�[�h��ۑ�
	movec.l	d2,sfc			;�t�@���N�V�����R�[�h��ύX
	bmi	3f			;���[�h�����O
;���[�h���[�h
;MOVEP.W (d16,Ar),Dq
	moves.b	(a1),d0			;�����������ʃo�C�g�����[�h
	lsl.w	#8,d0
	moves.b	(2,a1),d0		;���������牺�ʃo�C�g�����[�h
	move.w	d0,(2,a0)		;�f�[�^���W�X�^�̉��ʃ��[�h�փ��C�g
	bra	4f

;���[�h�����O
;MOVEP.L (d16,Ar),Dq
3:	moves.b	(a1),d0			;�����������ʃ��[�h�̏�ʃo�C�g�����[�h
	lsl.l	#8,d0
	moves.b	(2,a1),d0		;�����������ʃ��[�h�̉��ʃo�C�g�����[�h
	lsl.l	#8,d0
	moves.b	(4,a1),d0		;���������牺�ʃ��[�h�̏�ʃo�C�g�����[�h
	lsl.l	#8,d0
	moves.b	(6,a1),d0		;���������牺�ʃ��[�h�̉��ʃo�C�g�����[�h
	move.l	d0,(a0)			;�f�[�^���W�X�^�փ��C�g
4:	movec.l	d1,sfc			;�t�@���N�V�����R�[�h�𕜌�
	movem.l	(sp),d0-d7		;�f�[�^���W�X�^�̂ǂꂩ1���X�V����Ă���
	bra	8f

;���C�g
5:	movec.l	dfc,d1			;�t�@���N�V�����R�[�h��ۑ�
	movec.l	d2,dfc			;�t�@���N�V�����R�[�h��ύX
	bmi	6f			;���C�g�����O
;���C�h���[�h
;MOVEP.W Dq,(d16,Ar)
	move.w	(2,a0),d0		;�f�[�^���W�X�^�̉��ʃ��[�h���烊�[�h
	rol.w	#8,d0
	moves.b	d0,(a1)			;�������֏�ʃo�C�g�����C�g
	rol.w	#8,d0
	moves.b	d0,(2,a1)		;�������։��ʃo�C�g�����C�g
	bra	7f

;���C�g�����O
;MOVEP.L Dq,(d16,Ar)
6:	move.l	(a0),d0			;�f�[�^���W�X�^���烊�[�h
	rol.l	#8,d0
	moves.b	d0,(a1)			;�������֏�ʃ��[�h�̏�ʃo�C�g�����C�g
	rol.l	#8,d0
	moves.b	d0,(2,a1)		;�������֏�ʃ��[�h�̉��ʃo�C�g�����C�g
	rol.l	#8,d0
	moves.b	d0,(4,a1)		;�������։��ʃ��[�h�̏�ʃo�C�g�����C�g
	rol.l	#8,d0
	moves.b	d0,(6,a1)		;�������։��ʃ��[�h�̉��ʃo�C�g�����C�g
7:	movec.l	d1,dfc			;�t�@���N�V�����R�[�h�𕜌�
	movem.l	(sp),d0-d2		;d0/d1/d2�𕜌�
8:	movem.l	(4*8,sp),a0-a1		;a0/a1�𕜌�
	lea.l	(4*16,sp),sp		;�j�󂳂�Ă��Ȃ����W�X�^�̕������ȗ�����
	tst.b	(sp)			;��O��������sr��T
	bpl	9f			;�g���[�X�Ȃ�
;�g���[�X
	ori.w	#$8000,sr		;RTE�̑O��sr��T���Z�b�g����MOVEP���g���[�X�����悤�ɐU�镑��
9:	rte
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	�N���b�N�v��
;
;----------------------------------------------------------------

	PATCH_DATA	clockCheck,$00FF013C

	jmp	clockCheck

	PATCH_EXTRA

;----------------------------------------------------------------
;�N���b�N�v��
clockCheck:

;�V�X�e���N���b�N�̊m�F
	moveq.l	#0,d0
	move.b	$00E8E00B,d0		;[$00E8E00B].b:SYS �@�픻��($DC=X68030,$FE=XVI��16MHz,$FF=XVI�ȑO��10MHz)
					;$00FF=10MHz,$00FE=16MHz,$00DC=25MHz
	not.b	d0			;$0000=10MHz,$0001=16MHz,$0023=25MHz
	lsl.w	#4,d0			;$0000=10MHz,$0010=16MHz,$0230=25MHz
	lsr.b	#4,d0			;$0000=10MHz,$0001=16MHz,$0203=25MHz
	move.w	d0,$0CB6.w		;[$0CB6.w].w:[11,12,13]�V�X�e���N���b�N($0000=10MHz,$0001=16MHz,$0203=25MHz)

;�N���b�N�v���̂��߂̃L���b�V��ON
;	MC68030�̂Ƃ��͖��߃L���b�V���ƃf�[�^�L���b�V����ON�ɂ���
;	MC68040/MC68060�̃f�[�^�L���b�V����MC68060�̃X�g�A�o�b�t�@��MMU��L���ɂ���܂�ON�ɂł��Ȃ��̂Ŗ��߃L���b�V������ON�ɂ���
;	SRAM�̃L���b�V���̐ݒ�͂������Ŕ��f�����
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	3f			;MC68000/MC68010�̓L���b�V���Ȃ�
	move.l	#$00002101,d0		;MC68020�͖��߃L���b�V��ON(bit0)�AMC68030�̓f�[�^�L���b�V��ON(bit8),���߃L���b�V��ON(bit0)
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	2f			;MC68020/MC68030
	beq	1f			;MC68040
	.cpu	68060
	movec.l	pcr,d0
	bset.l	#0,d0			;MC68060�̓X�[�p�[�X�J��ON(bit0)
	movec.l	d0,pcr
	.cpu	68000
	move.l	#$00800000,d0		;MC68060�̓X�g�A�o�b�t�@OFF(bit29),����L���b�V��ON(bit23)
1:	move.w	#$8000,d0		;MC68040/MC68060�̓f�[�^�L���b�V��OFF(bit31),���߃L���b�V��ON(bit15)
2:
	.cpu	68030
	movec.l	d0,cacr
	.cpu	68000
	clr.b	$00E8E009		;[$00E8E009].b:[X68030]�A�N�Z�X�E�F�C�g����
3:

;�N���b�N�v��
;ROM�v��
	lea.l	clockLoop(pc),a0	;�v�����[�v
	bsr	clockSub		;�v���T�u
	move.w	d0,$0CB8.w		;[$0CB8.w].w:[11,12,13]ROM�v���N���b�N($0342��10*250/3,$056E��16.67*250/3,$104D��25*500/3)
;RAM�v��
	lea.l	-30(sp),sp
	move.l	sp,d0
	add.w	#14,d0
	and.w	#-16,d0			;16�o�C�g���E����n�܂�16�o�C�g�̃��[�N�G���A
	addq.w	#4,d0
	movea.l	d0,a0			;�v�����[�v
	move.l	clockLoop-4(pc),-4(a0)	;�v�����[�v�����[�N�G���A�ɃR�s�[����
	move.l	clockLoop(pc),(a0)
	move.l	clockLoop+4(pc),4(a0)
	move.l	clockLoop+8(pc),8(a0)	;�X�^�b�N�G���A�����߃L���b�V���ɏ���Ă��邱�Ƃ͂Ȃ��̂ŃL���b�V���t���b�V���͏ȗ�����
	bsr	clockSub		;�v���T�u
	lea.l	30(sp),sp
	move.w	d0,$0CBA.w		;[$0CBA.w].w:[11,12,13]RAM�v���N���b�N($03D3��10*250/3*1.2/(1+0.22/10),$066D��16.67*250/3*1.2/(1+0.22/16.67),$104D��25*500/3)
	jmp	$00FF019C

IERB	equ	$00E88009
IMRB	equ	$00E88015
TCDCR	equ	$00E8801D
TCDR	equ	$00E88023
TDDR	equ	$00E88025

;�v���T�u
;>d0.w:�v���l�B0=���s
;?d1-d2/a1
clockSub:
;�񐔏�����
	moveq.l	#22-8,d2
clockRetry:
;���荞�݋֎~
	move.w	sr,-(sp)
	ori.w	#$0700,sr
;�^�C�}�ۑ�
	lea.l	TCDCR,a1
	move.b	IERB-TCDCR(a1),-(sp)
	move.b	IMRB-TCDCR(a1),-(sp)
	move.b	TCDCR-TCDCR(a1),-(sp)
;�^�C�}�ݒ�
	andi.b	#%11001111,IERB-TCDCR(a1)	;Timer-C/D���荞�ݒ�~
	andi.b	#%11001111,IMRB-TCDCR(a1)	;Timer-C/D���荞�݋֎~
	sf.b	TCDCR-TCDCR(a1)		;Timer-C/D�J�E���g��~
@@:	tst.b	TCDCR-TCDCR(a1)		;���S�ɒ�~����܂ő҂�
	bne	@b
	sf.b	TCDR-TCDCR(a1)		;Timer-C�J�E���^�N���A
	sf.b	TDDR-TCDCR(a1)		;Timer-D�J�E���^�N���A
;����
	move.l	#1<<22,d0
	lsr.l	d2,d0
	subq.l	#1,d0
;�J�E���g�J�n
;		  TCCR TDCR
	move.b	#%0111_0001,TCDCR-TCDCR(a1)	;Timer-C/D�J�E���g�J�n
					;	Timer-C��1/200�v���X�P�[��(50��s)
					;	Timer-D��1/4�v���X�P�[��(1��s)
;�v��
	jsr	(a0)
;�J�E���g��~
	sf.b	TCDCR-TCDCR(a1)		;Timer-C/D�J�E���g��~
@@:	tst.b	TCDCR-TCDCR(a1)		;���S�ɒ�~����܂ő҂�
	bne	@b
;�^�C�}�擾
	moveq.l	#0,d0
	sub.b	TCDR-TCDCR(a1),d0	;Timer-C�J�E���g��
	moveq.l	#0,d1
	sub.b	TDDR-TCDCR(a1),d1	;Timer-D�J�E���g��(�I�[�o�[�t���[����)
;�^�C�}����
	move.b	#200,TCDR-TCDCR(a1)	;Timer-C�J�E���^����
	sf.b	TDDR-TCDCR(a1)		;Timer-D�J�E���^�N���A
	move.b	(sp)+,TCDCR-TCDCR(a1)
	move.b	(sp)+,IMRB-TCDCR(a1)
	move.b	(sp)+,IERB-TCDCR(a1)
;���荞�݋���
	move.w	(sp)+,sr
;�J�E���^����
	mulu.w	#50,d0
	cmp.b	d1,d0
	bls	@f
	add.w	#256,d0
@@:	move.b	d1,d0
	subq.w	#1,d0
;�񐔍X�V
	tst.w	d2
	beq	@f
	cmp.w	#5000,d0
	bcc	@f
	subq.w	#1,d2			;5000��s�ɖ����Ȃ���Ή񐔂�2�{�ɂ���
	bra	clockRetry
@@:
;�␳
;	000/010�̂Ƃ�MHz�l*250/3�����[�h�ŕۑ�����̂�786.42MHz�����
;	020/030/040/060�̂Ƃ�MHz�l*500/3�����[�h�ŕۑ�����̂�393.21MHz�����
;	000/010	12clk	1MHz	12��s	2^9��	6144��s	10*250/3*1.2/(6144/2^9)=83.3333		83*3/250=0.996
;							(4194304000>>(22-9))/6144=83.3333
;			10MHz	1.2��s	2^13��	9830��s	10*250/3*1.2/(9830/2^13)=833.367	833*3/250=9.996
;							(4194304000>>(22-13))/9830=833.367
;			100MHz	0.12��s	2^16��	7864��s	10*250/3*1.2/(7864/2^16)=8333.67	8334*3/250=100.008
;							(4194304000>>(22-16))/7864=8333.67
;			600MHz	0.02��s	2^18��	5243��s	10*250/3*1.2/(5243/2^18)=49998.9	49999*3/250=599.988
;							(4194304000>>(22-18))/5243=49998.9
;			750MHz	0.016��s	2^19��	8389��s	10*250/3*1.2/(8389/2^19)=62497.1	62497*3/250=749.964
;							(4194304000>>(22-19))/8389=62497.1
;	020/030	6clk	25MHz	0.24��s	2^15��	7864��s	25*500/3*0.24/(7864/2^15)=4166.84	4167*3/500=25.002
;							(4194304000>>(22-15))/7864=4166.84
;	040	3clk	25MHz	0.12��s	2^16��	7864��s	25*500/3*0.12/(7864/2^16)=4166.84	4167*3/500=25.002
;							(2097152000>>(22-16))/7864=4166.84
;	060	1clk	50MHz	0.02��s	2^18��	5243��s	50*500/3*0.02/(5243/2^18)=8333.14	8333*3/500=49.998
;							(699050667>>(22-18))/5243=8333.14
	lea.l	clockScale(pc),a1
	moveq.l	#0,d1
	move.b	$0CBC.w,d1		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	cmp.w	#6,d1
	bhi	@f			;�O�̂���
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
	moveq.l	#-1,d1			;�I�[�o�[�t���[
@@:	moveq.l	#0,d0
	move.w	d1,d0
	rts
9:	moveq.l	#0,d0			;���s
	rts

clockScale:
	.dc.l	4194304000	;000
	.dc.l	4194304000	;010
	.dc.l	4194304000	;020
	.dc.l	4194304000	;030
	.dc.l	2097152000	;040
	.dc.l	0
	.dc.l	699050667	;060

;�v�����[�v
;	000/010�̂Ƃ�
;		dbra(���򂠂�)��10clk�BROM�̂Ƃ��̓E�F�C�g��������̂�12clk�B10MHz��1.2��s
;		RAM�v���̂Ƃ��͊�{��10clk����DRAM�̃��t���b�V���̉e����(1+0.22/MHz�l)�{���炢�ɂȂ�
;	020/030�̂Ƃ�
;		dbra(���߃L���b�V��ON,���򂠂�)��6clk�B25MHz��0.24��s
;	040�̂Ƃ�
;		dbra(���߃L���b�V��ON,���򂠂�)��3clk�B25MHz��0.12��s
;	060�̂Ƃ�
;		dbra(���߃L���b�V��ON,����L���b�V��ON,���򂠂�)��1clk�B50MHz��0.02��s
	.align	4		;dbra��4�o�C�g���E�ɍ��킹��
	nop
@@:	swap.w	d0
clockLoop:
	dbra	d0,clockLoop
	swap.w	d0
	dbra	d0,@b
	rts			;��ʃ��[�h�Ɖ��ʃ��[�h������ւ�����܂܂�������-1�Ȃ̂Ŗ��Ȃ�

	PATCH_END



;----------------------------------------------------------------
;
;	XF3,XF4,XF5�������Ȃ���N�������Ƃ��̃L���b�V��OFF
;
;----------------------------------------------------------------

	PATCH_DATA	xf345CacheOff,$00FF0336

	moveq.l	#0,d0
	.cpu	68030
	movec.l	d0,cacr			;�L���b�V��OFF
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	XF1,XF2�������Ȃ���N�������Ƃ��̃L���b�V��OFF
;
;----------------------------------------------------------------

	PATCH_DATA	xf12CacheOff,$00FF0380

	moveq.l	#0,d0
	.cpu	68030
	movec.l	d0,cacr			;�L���b�V��OFF
	.cpu	68000

	PATCH_END



;----------------------------------------------------------------
;
;	�N�����b�Z�[�W
;
;----------------------------------------------------------------

IPL_MESSAGE_PROPORTIONAL	equ	1

	PATCH_DATA	iplMessage,$00FF0E88

	jsr	iplMessage
	bra	($00FF0E9A)PatchZL

	PATCH_EXTRA

;----------------------------------------------------------------
;�N�����b�Z�[�W��\������
iplMessage:
	movem.l	d0-d7/a0-a6,-(sp)
	lea.l	-64(sp),sp
	movea.l	sp,a6			;������o�b�t�@
;<a6.l:������o�b�t�@
;���b�Z�[�W
	bsr	iplMessageRomver	;ROM�̃o�[�W������\������
	bsr	iplMessageMpu		;MPU�̎�ނƓ�����g����\������
	bsr	iplMessageFpu		;FPU/FPCP�̗L���Ǝ�ނ�\������
	bsr	iplMessageMmu		;MMU�̗L����\������
	bsr	iplMessageMemory	;���C���������͈̔͂Ɨe�ʂ�\������
	bsr	iplMessageExmemory	;�g���������͈̔͂Ɨe�ʂ�\������
	bsr	iplMessageCoprocessor	;�R�v���Z�b�T�̗L���Ǝ�ނ�\������
	lea.l	64(sp),sp
	movem.l	(sp)+,d0-d7/a0-a6
	rts

;----------------------------------------------------------------
;ROM�̃o�[�W������\������
;<a6.l:������o�b�t�@
;?d0-d1/a0-a1
iplMessageRomver:
	movea.l	a6,a0			;������o�b�t�@
	lea.l	100f(pc),a1		;$1B,'[1mBIOS ROM version: '
	bsr	strCopy
	IOCS	_ROMVER
;BCD�ϊ�
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
;�o�[�W����
	rol.l	#8,d0
	move.b	d0,(a0)+
	move.b	#'.',(a0)+
	rol.l	#8,d0
	move.b	d0,(a0)+
;���t
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
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
	rts

100:	.dc.b	$1B,'[1mBIOS ROM version: ',0
101:	.dc.b	' (20',0
102:	.dc.b	')',$1B,'[1m',13,10,0
	.even

;----------------------------------------------------------------
;MPU�̎�ނƓ�����g����\������
;<a6.l:������o�b�t�@
;>d5.l:_SYS_STAT(0)�̌���
;	bit0�`7		MPU�̎��(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	������g��(MHz)*10
;?d0-d1/a0-a1
iplMessageMpu:
	moveq.l	#0,d1
	IOCS	_SYS_STAT
	move.l	d0,d5
;<d5.l:_SYS_STAT(0)�̌���
;	bit0�`7		MPU�̎��(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	������g��(MHz)*10
	movea.l	a6,a0			;������o�b�t�@
	lea.l	100f(pc),a1		;'Micro-Processing Unit (MPU): '
	bsr	strCopy
;MPU�̎��
;	MC��XC
;		MC68000/MC68010/MC68020/MC68030/MC68040��MC
;		MC68060�̓��r�W����6����MC�A���r�W����5�܂�XC
;			http://www.ppa.pl/forum/amiga/29981/68060-pcr
;			?	���r�W����0
;			F43G	���r�W����1
;			G65V	���r�W����5
;			G59Y	?
;			E41J	���r�W����6
;	�����LC��EC
;		MC68000/MC68010/MC68020�͖���
;		MC68030��MMU������Ƃ�����AMMU���Ȃ��Ƃ�EC
;		MC68040/MC68060��FPU������Ƃ�����AFPU���Ȃ���MMU������Ƃ�LC�AMMU���Ȃ��Ƃ�EC
;	���r�W�����i���o�[
;		MC68060�̂Ƃ�������-0XX�Ń��r�W�����i���o�[��\��
;	����
;		MC68EC020��MC68020�̃A�h���X�o�X��24bit�ɂ���ECS,OCS,DBEN,IPEND,BGACK���ȗ������g�ݍ��ݗp�B�R�v���Z�b�T�C���^�t�F�C�X�͋���
	moveq.l	#-1,d2			;���r�W�����i���o�[�B-1=���r�W�����i���o�[��\�����Ȃ�
	moveq.l	#'M',d1
	cmp.b	#6,d5
	bne	1f			;MC68000/MC68030/MC68040��MC
	.cpu	68060
	movec.l	pcr,d0
	.cpu	68000
	lsr.w	#8,d0
	moveq.l	#0,d2
	move.b	d0,d2			;���r�W�����i���o�[
	cmp.b	#6,d2
	bcc	1f			;MC68060�̃��r�W����6�ȏ��MC
	moveq.l	#'X',d1			;MC68060�̃��r�W����5�ȉ���XC
1:	move.b	d1,(a0)+
	lea.l	101f(pc),a1		;'C68',0
	bsr	strCopy
	cmp.b	#3,d5
	blo	5f			;MC68000/MC68010/MC68020�͖���
	bhi	2f
	btst.l	#14,d5
	bne	5f			;MC68030��MMU������Ƃ�����
	bra	3f			;MC68030��MMU���Ȃ��Ƃ�EC
2:	tst.w	d5
	bmi	5f			;MC68040/MC68060��FPU������Ƃ�����
	moveq.l	#'L',d1
	btst.l	#14,d5
	bne	4f			;MC68040/MC68060��FPU���Ȃ���MMU������Ƃ�LC
3:	move.l	#'E',d1			;MC68040/MC68060��MMU���Ȃ��Ƃ�EC
4:	move.b	d1,(a0)+
	move.b	#'C',(a0)+
5:	move.b	#'0',(a0)+
	moveq.l	#'0',d1
	add.b	d5,d1
	move.b	d1,(a0)+
	move.b	#'0',(a0)+
	move.l	d2,d0			;���r�W�����i���o�[
	bmi	6f
	move.b	#'-',(a0)+
	moveq.l	#3,d1
	bsr	strDecN
6:
;������g��
	move.b	#' ',(a0)+
	move.b	#'(',(a0)+
	move.l	d5,d0
	clr.w	d0
	swap.w	d0			;������g��(MHz)*10
	bsr	strDec
	move.b	-1(a0),(a0)+		;�����_�ȉ�1���ڂ����ɂ��炷
	move.b	#'.',-2(a0)		;�����_����������
	lea.l	102f(pc),a1		;'MHz)',13,10
	bsr	strCopy
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
	rts

100:	.dc.b	'Micro-Processing Unit (MPU): ',0
101:	.dc.b	'C68',0
102:	.dc.b	'MHz)',13,10,0
	.even

;----------------------------------------------------------------
;FPU/FPCP�̗L���Ǝ�ނ�\������
;	MC68030
;	  fnop������
;	    fmovecr.x #1,fp0��0		Floating-Point Coprocessor (FPCP): MC68881
;	    fmovecr.x #1,fp0��0�ȊO	Floating-Point Coprocessor (FPCP): MC68882
;	MC68040,MC68060
;	  fnop������			Floating-Point Unit (FPU): on MPU
;<d5.l:_SYS_STAT(0)�̌���
;	bit0�`7		MPU�̎��(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	������g��(MHz)*10
;<a6.l:������o�b�t�@
;?d0/a0-a1
iplMessageFpu:
;FPU/FPCP
	tst.w	d5
	bpl	3f			;FPU/FPCP�Ȃ�
	movea.l	a6,a0			;������o�b�t�@
	lea.l	100f(pc),a1		;'Floating-Point Unit (FPU): '
	cmp.b	#4,d5
	bhs	1f
	lea.l	101f(pc),a1		;'Floating-Point Coprocessor (FPCP): '
1:	bsr	strCopy
	lea.l	102f(pc),a1		;'on MPU',13,10
	cmp.b	#4,d5
	bhs	2f			;MC68040/MC68060
	.cpu	68030
	fmovecr.x	#1,fp0		;0=MC68881,0�ȊO=MC68882
	fmove.x	fp0,-(sp)
	.cpu	68000
	move.l	(sp)+,d0
	or.l	(sp)+,d0
	or.l	(sp)+,d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	2f
	lea.l	104f(pc),a1		;'MC68882',13,10
2:	bsr	strCopy
	movea.l	a6,a1			;������o�b�t�@
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
;�R�v���Z�b�T�̗L���Ǝ�ނ�\������
;	MC68040,MC68060
;	  FC=7��$00022000��CIR������
;	    fmovecr.x #1,fp0��0		Motherboard Coprocessor: MC68881
;	    fmovecr.x #1,fp0��0�ȊO	Motherboard Coprocessor: MC68882
;	MC68000,MC68030,MC68040,MC68060
;	  $00E9E000��CIR������
;	    fmovecr.x #1,fp0��0		Extension Board Coprocessor 1: MC68881
;	    fmovecr.x #1,fp0��0�ȊO	Extension Board Coprocessor 1: MC68882
;	  $00E9E080��CIR������
;	    fmovecr.x #1,fp0��0		Extension Board Coprocessor 2: MC68881
;	    fmovecr.x #1,fp0��0�ȊO	Extension Board Coprocessor 2: MC68882
iplMessageCoprocessor:
;�}�U�[�{�[�h�R�v���Z�b�T
	cmp.b	#4,d5
	blo	2f
	bsr	coproCheck1		;�}�U�[�{�[�h�R�v���Z�b�T�̗L���Ǝ�ނ𒲂ׂ�
	bmi	2f
	movea.l	a6,a0			;������o�b�t�@
	lea.l	105f(pc),a1		;'Motherboard Coprocessor: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
2:
;���l���Z�v���Z�b�T�{�[�h1
	moveq.l	#0,d0
	bsr	coproCheck2
	bmi	2f
	movea.l	a6,a0			;������o�b�t�@
	lea.l	106f(pc),a1		;'Extension Board Coprocessor 1: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
2:
;���l���Z�v���Z�b�T�{�[�h2
	moveq.l	#1,d0
	bsr	coproCheck2
	bmi	2f
	movea.l	a6,a0			;������o�b�t�@
	lea.l	107f(pc),a1		;'Extension Board Coprocessor 2: '
	bsr	strCopy
	tst.l	d0
	lea.l	103f(pc),a1		;'MC68881',13,10
	beq	1f
	lea.l	104f(pc),a1		;'MC68882',13,10
1:	bsr	strCopy
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
2:
	rts

103:	.dc.b	'MC68881',13,10,0
104:	.dc.b	'MC68882',13,10,0
105:	.dc.b	'Motherboard Coprocessor: ',0
106:	.dc.b	'Extension Board Coprocessor 1: ',0
107:	.dc.b	'Extension Board Coprocessor 2: ',0
	.even

;�}�U�[�{�[�h�R�v���Z�b�T�̗L���Ǝ�ނ𒲂ׂ�
;>d0.l:-1=�Ȃ�,0=MC68881,1=MC68882
	.cpu	68060
coproCheck1:
	movem.l	d1-d4/a0-a2,-(sp)
@@:	moveq.l	#1,d0
	and.b	@b-1(pc,d0.w*2),d0	;0=000,1=020�ȏ�
	subq.l	#1,d0			;-1=000,0=020�ȏ�
	bmi	9f			;000
	lea.l	$00022000,a0
	moveq.l	#-1,d0
	move.w	sr,d2
	ori.w	#$0700,sr
	movec.l	dfc,d4
	movec.l	sfc,d3
	moveq.l	#7,d1			;CPU���
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
	beq	@f			;ROM��$01��0�Ȃ̂�MC68881
	moveq.l	#1,d0			;ROM��$01��0�łȂ��̂�MC68882
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

;���l���Z�v���Z�b�T�{�[�h�̗L���Ǝ�ނ𒲂ׂ�
;<d0.l:0=���l���Z�v���Z�b�T�{�[�h1,1=���l���Z�v���Z�b�T�{�[�h2
;>d0.l:-1=�Ȃ�,0=MC68881,1=MC68882
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
	beq	@f			;ROM��$01��0�Ȃ̂�MC68881
	moveq.l	#1,d0			;ROM��$01��0�łȂ��̂�MC68882
@@:
5:	movea.l	a1,sp
	move.l	a2,$0008.w
	move.w	d2,sr
	movem.l	(sp)+,d1-d2/a0-a2
	tst.l	d0
	rts

;----------------------------------------------------------------
;MMU�̗L����\������
;<d5.l:_SYS_STAT(0)�̌���
;	bit0�`7		MPU�̎��(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	������g��(MHz)*10
;<a6.l:������o�b�t�@
;?d0/a0-a1
iplMessageMmu:
	btst.l	#14,d5
	beq	9f			;MMU�Ȃ�
	movea.l	a6,a0			;������o�b�t�@
	lea.l	100f(pc),a1		;'Memory Management Unit (MMU): '
	bsr	strCopy
	cmp.b	#3,d5
	bhs	1f
	lea.l	101f(pc),a1		;'MC68851',13,10
	bsr	strCopy
	bra	2f

1:	lea.l	102f(pc),a1		;'on MPU',13,10
	bsr	strCopy
2:	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
9:	rts

100:	.dc.b	'Memory Management Unit (MMU): ',0
101:	.dc.b	'MC68851',13,10,0
102:	.dc.b	'on MPU',13,10,0
	.even

;----------------------------------------------------------------
;���C���������͈̔͂Ɨe�ʂ�\������
;	$00100000-$00BFFFFF�ɂ���1MB�P�ʂŃ��C���������̗L�����m�F���A���C�������������݂���͈͂�\������
;	�菇
;		���ʂ̃y�[�W���珇�Ƀ��[�h���Ă݂ăo�X�G���[���o�Ȃ����Ƃ��m�F����
;----------------------------------------------------------------
;<d5.l:_SYS_STAT(0)�̌���
;	bit0�`7		MPU�̎��(0=MC68000,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	������g��(MHz)*10
;<a6.l:������o�b�t�@
;?d0-d1/a0-a3/a5
iplMessageMemory:
	move.w	sr,-(sp)		;sr�ۑ�
	ori.w	#$0700,sr		;���荞�݋֎~
	move.l	$0008.w,-(sp)		;�o�X�G���[�x�N�^�ۑ�
	movea.l	sp,a5			;sp�ۑ�
	move.l	#2f,$0008.w		;�o�X�G���[�Ń��[�v���I������
	suba.l	a2,a2			;a2=$00000000
	move.l	a2,d0			;�J�n�A�h���X
1:	tst.b	(a2)			;�e�X�g
	adda.l	#$00100000,a2		;���̃y�[�W
	cmpa.l	#$00C00000,a2		;���C���������̖���
	blo	1b
2:	movea.l	a5,sp			;sp����
	move.l	(sp)+,$0008.w		;�o�X�G���[�x�N�^����
	move.w	(sp)+,sr		;sr����(���荞�݋����x������)
	move.l	a2,d1			;�I���A�h���X(������܂܂Ȃ�)
	movea.l	a6,a0			;������o�b�t�@
	lea.l	100f(pc),a1		;'Motherboard Memory: $'
	bsr	strCopy
	bsr	strMemory		;�������͈̔͂Ɨe�ʂ̕���������
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
	rts

100:	.dc.b	'Motherboard Memory: $',0
	.even

;----------------------------------------------------------------
;�g���������͈̔͂Ɨe�ʂ�\������
;	$01000000-$FFFFFFFF�ɂ���16MB�P�ʂœƗ������g���������̗L�����m�F���A�g�������������݂���͈͂�\������
;	�菇
;		�e�y�[�W�̓����I�t�Z�b�g�ɏ�ʂ̃y�[�W���珇�ɈقȂ�f�[�^����������
;		���ʂ̃y�[�W�ɏ������񂾂��Ƃŏ�ʂ̃y�[�W�̃f�[�^���ω������ꍇ�͏�ʂ̃y�[�W�͑��݂��Ȃ��Ɣ��f����
;		$2000-$21FF�����[�N�Ƃ��Ďg�p����
;	����
;		MC68000/MC68010/MC68EC020�̓A�h���X�o�X��24bit�Ȃ̂ł����ŕ\�������g���������͑��݂��Ȃ�
;<a6.l:������o�b�t�@
;?d0-d1/a0-a3/a5
iplMessageExmemory:
	move.w	sr,-(sp)		;sr�ۑ�
	ori.w	#$0700,sr		;���荞�݋֎~
	move.l	$0008.w,-(sp)		;�o�X�G���[�x�N�^�ۑ�
	movea.l	sp,a5			;sp�ۑ�
;----------------------------------------------------------------
;	page=$FF..$00�ɂ���
;		[$2100+page].b=[page<<24].b	�ۑ�
;		�o�X�G���[�łȂ����
;			[page<<24].b=page	�e�X�g�f�[�^��������
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;�o�X�G���[�ŃX�L�b�v
	suba.l	a2,a2			;a2=$FF000000,$FE000000,�c,$00000000
	lea.l	$2200.w,a1		;a1=$000021FF,$000021FE,�c,$00002100
	move.w	#$00FF,d1		;d1=$00FF,$00FE,�c,$0000=page
1:	suba.l	#$01000000,a2
	subq.l	#1,a1			;�o�X�G���[�ł����ƍ���̂�-(a1)�͎g��Ȃ�
	move.b	(a2),(a1)		;�ۑ�
	move.b	d1,(a2)			;�e�X�g�f�[�^��������
2:	movea.l	a5,sp			;sp����
	dbra	d1,1b
;----------------------------------------------------------------
;	page=$FF..$00�ɂ���
;		[$2000+page].b=~page	;�e�X�g�f�[�^�ƈقȂ�_�~�[�f�[�^�B�o�X�G���[�ŏ������߂Ȃ������Ƃ��ɎQ�Ƃ����
;		[$2000+page].b=[page<<24].b	�e�X�g�f�[�^�ǂݏo��
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;�o�X�G���[�ŃX�L�b�v
	suba.l	a2,a2			;a2=$FF000000,$FE000000,�c,$00000000
	lea.l	$2100.w,a1		;a1=$000020FF,$000020FE,�c,$00002000
	move.w	#$00FF,d1		;d1=$00FF,$00FE,�c,$0000=page
	move.b	d1,d0			;d0=$00,$01,�c,$FF=~page
1:	addq.b	#1,d0
	suba.l	#$01000000,a2
	move.b	d0,-(a1)		;�e�X�g�f�[�^�ƈقȂ�_�~�[�f�[�^�B�o�X�G���[�ŏ������߂Ȃ������Ƃ��ɎQ�Ƃ����
	move.b	(a2),(a1)		;�e�X�g�f�[�^�ǂݏo��
2:	movea.l	a5,sp			;sp����
	dbra	d1,1b
;----------------------------------------------------------------
;	page=$FF..$00�ɂ���
;		([$2000+page].b-=page)==0�Ȃ��
;			[page<<24].b=[$2100+].b	����
;----------------------------------------------------------------
	move.l	#2f,$0008.w		;�o�X�G���[�ŃX�L�b�v
	suba.l	a2,a2			;a2=$FF000000,$FE000000,�c,$00000000
	lea.l	$2100.w,a1		;a1=$000020FF,$000020FE,�c,$00002000
	move.w	#$00FF,d1		;d1=$00FF,$00FE,�c,$0000=page
1:	suba.l	#$01000000,a2
	sub.b	d1,-(a1)		;��r�B��v�����Ƃ�0
	bne	2f
	move.b	$2200-$2100(a1),(a2)	;����
2:	movea.l	a5,sp			;sp����
	dbra	d1,1b
;----------------------------------------------------------------
	move.l	(sp)+,$0008.w		;�o�X�G���[�x�N�^����
	move.w	(sp)+,sr		;sr����(���荞�݋����x������)
;----------------------------------------------------------------
;	page=$01..$FF�ɂ���
;		[$2000+page].b==0�ł���͈͂�\��
;----------------------------------------------------------------
	suba.l	a2,a2			;�g���������̊J�n�ʒu�B0=page-1�Ɋg���������͂Ȃ�
	lea.l	$2001.w,a3		;a3=$00002001,$00002002,�c,$000020FF=$2000+page
1:	tst.b	(a3)
	bne	2f			;page�Ɋg���������͂Ȃ�
;page�Ɋg��������������
	move.l	a2,d0			;�g���������̊J�n�ʒu
	bne	3f			;page-1�ɂ��g����������������
;page-1�ɂ͊g���������͂Ȃ�����
;page����g�����������n�܂���
	movea.l	a3,a2			;�g���������̊J�n�ʒu
	bra	3f
;page�Ɋg���������͂Ȃ�
2:	move.l	a2,d0			;�g���������̊J�n�ʒu
	beq	3f			;page-1�ɂ��g���������͂Ȃ�����
;page-1�ɂ͊g����������������
;page�Ŋg�����������I�����
	movea.l	a6,a0			;������o�b�t�@
	bsr	strExmemory		;�g���������͈̔͂�\������
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
	suba.l	a2,a2			;�g���������̊J�n�ʒu
3:	addq.l	#1,a3
	cmpa.w	#$20FF,a3
	bls	1b
	move.l	a2,d0			;�g���������̊J�n�ʒu
	beq	4f			;page-1�ɂ͊g���������͂Ȃ�����
;page-1�ɂ͊g����������������
	movea.l	a6,a0			;������o�b�t�@
	bsr	strExmemory		;�������͈̔͂Ɨe�ʂ̕���������
	movea.l	a6,a1			;������o�b�t�@
	IOCS	_B_PRINT
4:	rts

;----------------------------------------------------------------
;�g���������͈̔͂Ɨe�ʂ̕���������
;<a0.l:������o�b�t�@
;<a2.l:$2000+page�B�g���������̊J�n�ʒu
;<a3.l:$2000+page�B�g���������̏I���ʒu(������܂܂Ȃ�)
;?d0-d1/a0-a1
strExmemory:
	move.l	a2,d0
	swap.w	d0
	lsl.l	#8,d0			;�J�n�A�h���X
	move.l	a3,d1
	swap.w	d1
	lsl.l	#8,d1			;�I���A�h���X(������܂܂Ȃ�)
	lea.l	100f(pc),a1		;'Daughterboard Memory: $'
	bsr	strCopy
;�������͈̔͂Ɨe�ʂ̕���������
;<d0.l:�J�n�A�h���X
;<d1.l:�I���A�h���X(������܂܂Ȃ�)
;<a0.l:������o�b�t�@
;?d0/a0-a1
strMemory:
	bsr	strHex8			;�J�n�A�h���X
	move.b	#'-',(a0)+
	move.b	#'$',(a0)+
	move.l	d0,-(sp)		;�J�n�A�h���X
	move.l	d1,d0			;�I���A�h���X(������܂܂Ȃ�)
	subq.l	#1,d0			;�I���A�h���X(������܂�)
	bsr	strHex8
	move.b	#' ',(a0)+
	move.b	#'(',(a0)+
	move.l	d1,d0			;�I���A�h���X(������܂܂Ȃ�)
	sub.l	(sp)+,d0		;�I���A�h���X(������܂܂Ȃ�)-�J�n�A�h���X=�e��
	swap.w	d0
	lsr.w	#4,d0			;�e��(16MB�P��)
	bsr	strDec
	lea.l	101f(pc),a1		;'MB)',13,10
	bsr	strCopy
	rts

100:	.dc.b	'Daughterboard Memory: $',0
101:	.dc.b	'MB)',13,10,0
	.even

;----------------------------------------------------------------
;32bit�������萮����10�i���̕�����ɕϊ�����(�[���T�v���X����)
;<d0.l:32bit�������萮��
;<a0.l:������o�b�t�@
;>a0.l:������̒���
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
3:	cmp.l	(a1)+,d0		;�[���T�v���X����
	blo	3b
	subq.l	#4,a1
	bra	5f

4:	addq.b	#1,d1
	sub.l	d2,d0			;10�̗ݏ����������邩������
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
;32bit�����Ȃ�������10�i���̕�����ɕϊ�����(�����w��,�O�a�ϊ�����,�[���T�v���X�Ȃ�)
;<d0.l:32bit�����Ȃ�����
;<d1.l:����
;<a0.l:������o�b�t�@
;>a0.l:������̒���
strDecN:
	movem.l	d0-d2/a1,-(sp)
	moveq.l	#10,d2
	sub.l	d1,d2			;10-����
	bhs	7f
	moveq.l	#0,d2
7:	lsl.w	#2,d2
	lea.l	10f(pc,d2.w),a1
	beq	5b			;10���̂Ƃ��͖O�a�ϊ��ł��Ȃ�
	cmp.l	-4(a1),d0
	blo	5b			;�͈͓�
	move.l	-4(a1),d0
	subq.l	#1,d0			;�ő�l
	bra	5b

;10�̗ݏ�̃e�[�u��
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
;32bit�����Ȃ�������16�i��8���̕�����ɕϊ�����
;<d0.l:32bit�����Ȃ�����
;<a0.l:������o�b�t�@
;>a0.l:������̒���
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
;������R�s�[
;<a0.l:�R�s�[��
;<a1.l:�R�s�[��
;>a0.l:�R�s�[��̖�����0�̈ʒu
;>a1.l:�R�s�[���̖�����0�̒���̈ʒu
strCopy:
@@:	move.b	(a1)+,(a0)+
	bne	@b
	subq.l	#1,a0
	rts

	PATCH_END



;----------------------------------------------------------------
;
;	�N���b�N�\�����[�`����A6���W�X�^�̍ŏ�ʃo�C�g���j�󂳂��
;	http://stdkmd.com/bugsx68k/#rom_clocka6
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	���C����������9MB�̂Ƃ��N������19MB�ƕ\�������
;	http://stdkmd.com/bugsx68k/#rom_9mb
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	�N������炷��FM�����h���C�o���듮�삷�邱�Ƃ�����
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
;	�N�����b�Z�[�W��Memory Managiment Unit�̃X�y�����Ԉ���Ă���
;	http://stdkmd.com/bugsx68k/#rom_mmu
;
;----------------------------------------------------------------



;----------------------------------------------------------------
;
;	�d����g���Ǝ��s���̃v���O�������듮�삷�邱�Ƃ�����
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
;	�J�[�\������ʂ̍ŉ��s�ɂ���Ɠd�삪��ʊO�ɕ\�������
;	http://stdkmd.com/bugsx68k/#rom_dentaku64
;
;	�d��̕\���ʒu�����߂鏈����Y���W�𒲐�����R�[�h��ǉ�����
;	���O�ɂ���d��OFF���[�`�����l�߂Ăł������Ԃ�Y���W�𒲐�����R�[�h����������ł�����Ăяo��
;
;----------------------------------------------------------------

	PATCH_DATA	dentaku64_1,$00FF4444

	move.w	d0,-(sp)
	move.l	#184<<16|16,-(sp)
	move.l	$0BFC.w,-(sp)		;[$0BFE.w].w:�d��\������Y�h�b�g���W
					;[$0BFC.w].w:�d��\������X�h�b�g���W
	move.w	#2,-(sp)
	bsr	($00FF67C4)PatchZL	;_TXFILL���s
	clr.w	10(sp)
	addq.w	#1,(sp)			;3
	bsr	($00FF67C4)PatchZL	;_TXFILL���s
	lea.l	12(sp),sp
	move.w	(sp)+,d0
	beq	@f
	bsr	($00FFAA5C)PatchZL	;_MS_CURON
@@:	movem.l	(sp)+,d1-d7/a0-a6
	rts

dentaku64:
	addq.w	#1,d1			;�d��̃e�L�X�gY���W
	cmp.w	#31,d1			;�J�[�\���̎��̍s��Y���W��31�ȉ��̂Ƃ��̓J�[�\���̎��̍s
	bls	@f
	cmp.w	$0972.w,d1		;[$0972.w].w:�e�L�X�g�̍s��-1
	bls	@f			;�J�[�\���̎��̍s���R���\�[���͈͓̔��̂Ƃ��̓J�[�\���̎��̍s
	move.w	$0972.w,d1		;[$0972.w].w:�e�L�X�g�̍s��-1
	beq	@f			;�R���\�[����1�s�����Ȃ��Ƃ��̓R���\�[���̍ŉ��s
	subq.w	#1,d1			;����ȊO�̓R���\�[���̉�����2�Ԗڂ̍s
@@:	rts

	PATCH_END

	PATCH_DATA	dentaku64_2,$00FF44A6

	patchbsr.s	dentaku64,dentaku64_1

	PATCH_END



;----------------------------------------------------------------
;
;	�\�t�g�L�[�{�[�h�́��L�[�̑ܕ��������Ă��Ȃ�
;	http://stdkmd.com/bugsx68k/#rom_softkeyboard
;
;----------------------------------------------------------------

	PATCH_DATA	softkeyboard,$00FF5AA8

	.dc.b	__MMM_MM,M_______

	PATCH_END



;----------------------------------------------------------------
;
;	_DEFCHR�Ńt�H���g�T�C�Y��0���w�肳�ꂽ�Ƃ�8�ɓǂݑւ��鏈���������R�[�h��0�̂Ƃ������@�\���Ă��Ȃ�
;	_DEFCHR�Ńt�H���g�T�C�Y��6���w��ł��Ȃ�
;	_DEFCHR��_FNTADR���t�H���g�p�^�[����$0C46.w�ɍ쐬���ĕԂ����Ƃ������ɏ㏑�����Ă��ۑ�����Ȃ��̂ɃG���[�ɂȂ�Ȃ�
;	_DEFCHR�Ńt�H���g�A�h���X��X68030�̃n�C��������060turbo�̃��[�J�����������w���Ă����ROM�ƌ�F���ăG���[�ɂȂ�
;
;----------------------------------------------------------------

	PATCH_DATA	defchr,$00FF6ADA

	movem.l	d1-d2/a1,-(sp)
	move.l	d1,d2
	swap.w	d2
	movea.l	$0458.w,a0		;[$0458.w].l:[$0116]_FNTADR
	jsr	(a0)
;<d0.l:�t�H���g�A�h���X
;<d1.w:�������̃o�C�g��-1
;<d2.w:�c�����̃h�b�g��-1
	movea.l	d0,a0			;�t�H���g�A�h���X
	cmpa.w	#$0C46.w,a0		;[$0C46.w].b[72]:_FNTADR�̃t�H���g�쐬�o�b�t�@
	beq.s	4f			;�t�H���g�p�^�[����$0C46.w�ɍ쐬���ꂽ�BROM�ł͂Ȃ��������ɏ㏑�����Ă��ۑ�����Ȃ��̂Ŏ��s
	swap.w	d0
	cmp.w	#$00F0,d0
	blo.s	1f			;�t�H���g�A�h���X��ROM���w���Ă��Ȃ�
	cmp.w	#$0100,d0
	blo.s	4f			;�t�H���g�A�h���X��ROM���w���Ă���B�㏑���ł��Ȃ��̂Ŏ��s
;�t�H���g�A�h���X��ROM���w���Ă��Ȃ�
;�t�H���g�p�^�[�����R�s�[����
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
;	_CRTMOD���w�肳�ꂽ��ʃ��[�h�ƈقȂ�F���ŃO���t�B�b�N�p���b�g������������
;	http://stdkmd.com/bugsx68k/#rom_crtmod_gpalet
;
;----------------------------------------------------------------

	PATCH_DATA	crtmodGpalet,$00FF6D70

	moveq.l	#%1100,d0
	and.b	$093C.w,d0		;[$093C.w].b:��ʃ��[�h
	lsr.w	#1,d0
	move.w	@f(pc,d0.w),d0
	jsr	@f(pc,d0.w)
	bset.b	#3,$00E80028		;[$00E80028].w:CRTC R20 ���������[�h/�𑜓x(���𑜓x|�����𑜓x|�����𑜓x)
	rts

@@:
	.dc.w	($00FFB3F4)PatchZL-@b	;CRTMOD(0�`3)���O���t�B�b�N16�F�W���p���b�g�ݒ�
	.dc.w	($00FFB3F4)PatchZL-@b	;CRTMOD(4�`7)���O���t�B�b�N16�F�W���p���b�g�ݒ�
	.dc.w	($00FFB408)PatchZL-@b	;CRTMOD(8�`11)���O���t�B�b�N256�F�W���p���b�g�ݒ�
	.dc.w	($00FFB41E)PatchZL-@b	;CRTMOD(12�`15)���O���t�B�b�N65536�F�W���p���b�g�ݒ�

	PATCH_END



;----------------------------------------------------------------
;
;	DMA�]���J�n���O�̃L���b�V���t���b�V��
;
;----------------------------------------------------------------

	PATCH_DATA	dmaCacheFlush,$00FF8284

	jmp	cacheFlush

	PATCH_END



;----------------------------------------------------------------
;
;	�����SCSI�@�킪�ڑ�����Ă���ƋN���ł��Ȃ�
;	http://stdkmd.com/bugsx68k/#rom_fds120
;
;	_S_INQUIRY��_S_REQUEST�̃A���P�[�V������������Ȃ�
;	Inquiry��EVPD��0�̂Ƃ��A���P�[�V��������5�o�C�g�ȏ�łȂ���΂Ȃ�Ȃ��̂�1�o�C�g�ɂȂ��Ă���
;	Request Sense�̃Z���X�f�[�^�̓G���[�N���X�ɂ����4�o�C�g�܂���8�o�C�g�ȏゾ���A���P�[�V��������3�o�C�g�ɂȂ��Ă���
;	�N�����ɃA���P�[�V���������������̃f�[�^��Ԃ����Ƃ���SCSI�@�킪�ڑ�����ēd���������Ă���ƃn���O�A�b�v����
;	Inquiry��1��ڂ�5�o�C�g�v������2��ڂɒǉ��f�[�^��+5�o�C�g�v������̂�������
;	�ŏ�����36�o�C�g�v�����Ă��ǂ���2��ɕ������������
;	�Q�l:FDS021.LZH/FDS120T.DOC
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
;	�u���b�N����2048�o�C�g�ȏ��SCSI�@��(CD-ROM�Ȃ�)����N���ł���悤�ɂ���
;
;	FORMAT.X�ɂ����SCSI HD�ɏ������܂ꂽSCSI-BIOS�͑Ή����Ă��Ȃ����A
;	�����ւ�����̂͑g�ݍ��ݍς݂�SCSI-BIOS�̃��x����3�ȉ��̂Ƃ������Ȃ̂Ŗ��Ȃ�
;
;	SCSIINROM/SCSIEXROM�����Ƃ������P�[�g����u���b�N�̒��Ńp�b�`�f�[�^�����������邽��FF1���g�p���Ă��邱�Ƃɒ���
;	���@�ł͓��삵�Ȃ�
;
;----------------------------------------------------------------

	PATCH_DATA	scsi2048_1,$00FF9436

	move.l	4(a1),d0
;!!! XEiJ�ȊO�ł͓��삵�Ȃ�
	.dc.w	$04C0			;ff1.l d0
	moveq.l	#23,d5
	sub.l	d0,d5
;�Z�N�^0��ǂݍ���
	moveq.l	#0,d2			;0x00000000����
	moveq.l	#4,d3			;0x00000400�o�C�g
	patchbsr.w	scsi2048Sub,scsi2048_3	;1�u���b�N�̃T�C�Y�ɉ����ău���b�N�ԍ��ƃu���b�N���𒲐�����
;	moveq.l	#_SCSIDRV,d0

	PATCH_END

	PATCH_DATA	scsi2048_2,$00FF9570

	move.l	4(a1),d0
;!!! XEiJ�ȊO�ł͓��삵�Ȃ�
	.dc.w	$04C0			;ff1.l d0
	moveq.l	#23,d5
	sub.l	d0,d5
	moveq.l	#8,d2			;0x00000800����
	moveq.l	#4,d3			;0x00000400�o�C�g
	patchbsr.w	scsi2048Sub,scsi2048_3	;1�u���b�N�̃T�C�Y�ɉ����ău���b�N�ԍ��ƃu���b�N���𒲐�����
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
;	_MS_LIMIT��Y�����͈̔͂�1007�܂ł����ݒ�ł��Ȃ�
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
;	_MS_ONTM�̃L���b�V������
;
;----------------------------------------------------------------

	PATCH_DATA	msOntmCache_1,$00FFAC72

	jsr	cacheOnI	;�f�[�^�L���b�V��OFF,���߃L���b�V��ON
	move.l	d0,-(sp)
	bra	($00FFAC86)PatchZL

	PATCH_END

	PATCH_DATA	msOntmCache_2,$00FFACE8

	move.l	(sp)+,d2
	jsr	cacheSet	;�L���b�V���ݒ�
	bra	($00FFACF6)PatchZL

	PATCH_END



;----------------------------------------------------------------
;
;	_GPALET��65536�F���[�h�̃p���b�g�𐳂����擾�ł��Ȃ�
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
;	_SYS_STAT�̃R�[�h���Ԉ���Ă���
;	http://stdkmd.com/bugsx68k/#rom_sysstat
;
;----------------------------------------------------------------

	PATCH_DATA	sysstat,$00FFC75A

	jmp	iocsSysStat

	PATCH_EXTRA

;----------------------------------------------------------------
;IOCS _SYS_STAT
;$AC �V�X�e�����̎擾�Ɛݒ�
;<d1.w:���[�h
;	0	MPU�X�e�[�^�X�̎擾
;		>d0.l:MPU�X�e�[�^�X
;			bit0�`7		MPU�̎��(0=68000,3=68030,4=68040,6=68060)
;			bit14		MMU�̗L��(0=�Ȃ�,1=����)
;			bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;			bit16�`31	�N���b�N�X�s�[�h*10
;	1	�L���b�V����Ԃ̎擾
;		>d0.l:���݂̃L���b�V�����
;			bit0	���߃L���b�V���̏��(0=����,1=�L��)
;			bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	2	�L���b�V���̏�Ԃ�SRAM�ݒ�l�ɂ���
;		>d0.l:�ݒ��̃L���b�V�����
;			bit0	���߃L���b�V���̏��(0=����,1=�L��)
;			bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	3	�L���b�V���t���b�V��
;	4	�L���b�V���ݒ�
;		<d2.w:�L���b�V���̐ݒ�
;			bit0	���߃L���b�V���̏��(0=����,1=�L��)
;			bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;		>d0.l:�ݒ�O�̃L���b�V�����
;			bit0	���߃L���b�V���̏��(0=����,1=�L��)
;			bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
iocsSysStat:
	moveq.l	#-1,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	bhs	2f			;020/030/040/060
;000/010
	tst.w	d1
	bne	9f			;000/010����d1!=0�̂Ƃ��̓G���[
2:	cmp.w	#4,d1
	bhi	9f
	move.w	d1,-(sp)
	add.w	d1,d1
	move.w	10f(pc,d1.w),d1
	jsr	10f(pc,d1.w)
	move.w	(sp)+,d1
9:	rts

10:
	.dc.w	mpuStat-10b		;0:MPU�X�e�[�^�X�̎擾
	.dc.w	cacheGet-10b		;1:�L���b�V����Ԃ̎擾
	.dc.w	cacheLoad-10b		;2:�L���b�V���̏�Ԃ�SRAM�ݒ�l�ɂ���
	.dc.w	cacheFlush-10b		;3:�L���b�V���t���b�V��
	.dc.w	cacheSet-10b		;4:�L���b�V���ݒ�

;----------------------------------------------------------------
;IOCS _SYS_STAT		0
;MPU�X�e�[�^�X�̎擾
;>d0.l:MPU�X�e�[�^�X
;	bit0�`7		MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
;	bit14		MMU�̗L��(0=�Ȃ�,1=����)
;	bit15		FPU/FPCP�̗L��(0=�Ȃ�,1=����)
;	bit16�`31	�N���b�N�X�s�[�h*10
mpuStat:
	moveq.l	#12,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	@f
;020/030/040/060
	moveq.l	#6,d0
@@:	mulu.w	$0CB8.w,d0		;[$0CB8.w].w:[11,12,13]ROM�v���N���b�N($0342��10*250/3,$056E��16.67*250/3,$104D��25*500/3)
					;MHz�l*250/3*12=MHz�l*1000�A�܂��́AMHz�l*500/3*6=MHz�l*1000
	add.l	#50,d0
	divu.w	#100,d0			;MHz�l*10
	swap.w	d0			;ssssssss ssssssss ........ ........
	clr.w	d0			;ssssssss ssssssss 00000000 00000000
	tst.b	$0CBE.w			;[$0CBE.w].b:MMU�̗L��(0=�Ȃ�,-1=����)
	sne.b	d0			;ssssssss ssssssss 00000000 mmmmmmmm
	ror.w	#1,d0			;ssssssss ssssssss m0000000 0mmmmmmm
	tst.b	$0CBD.w			;[$0CBD.w].b:FPU/FPCP�̗L��(0=�Ȃ�,-1=����)
	sne.b	d0			;ssssssss ssssssss m0000000 ffffffff
	ror.w	#1,d0			;ssssssss ssssssss fm000000 0fffffff
	move.b	$0CBC.w,d0		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
					;ssssssss ssssssss fm000000 pppppppp
	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		1
;�L���b�V����Ԃ̎擾
;>d0.l:���݃L���b�V�����
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ���0��Ԃ�
cacheGet:
	moveq.l	#0,d0
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	.cpu	68030
	movec.l	cacr,d0
	.cpu	68000
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
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
;�L���b�V���̏�Ԃ�SRAM�ݒ�l�ɂ���
;>d0.l:�ݒ��̃L���b�V�����
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ���0��Ԃ�
cacheLoad:
	move.l	d2,-(sp)
	moveq.l	#0,d2			;�O�̂���
	move.b	$00ED0090,d2		;[$00ED0090].b:SRAM [13]�L���b�V��(------|�f�[�^|����)
	bsr	cacheSet		;�L���b�V���ݒ�
	move.l	(sp)+,d2
	rts

;----------------------------------------------------------------
;IOCS _SYS_STAT		3
;�L���b�V���t���b�V��
cacheFlush:
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
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
;�L���b�V���ݒ�
;<d2.l:�L���b�V���̐ݒ�
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ��͖��Ӗ�
;>d0.l:�ݒ�O�̃L���b�V�����
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ���0��Ԃ�
cacheSet:
	bsr	cacheGet		;�L���b�V����Ԃ̎擾
	cmpi.b	#2,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	blo	9f
;020/030/040/060
	move.l	d1,-(sp)
	move.w	d2,-(sp)		;�����ƃT�C�Y�ɒ���
	cmpi.b	#4,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
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
	and.w	d0,d2			;bit1=�f�[�^�L���b�V��ON��OFF,bit0=���߃L���b�V��ON��OFF
	beq	8f
	subq.w	#2,d2
	bhi	3f
	blo	1f
	cpusha	dc			;2:�f�[�^�L���b�V����OFF�ɂ����Ƃ��v�b�V�����Ă��疳��������
	bra	8f
3:	cpusha	dc			;3:�f�[�^�L���b�V����OFF�ɂ����Ƃ��v�b�V�����Ă��疳��������
1:	cinva	ic			;1:3:���߃L���b�V����OFF�ɂ����Ƃ�����������
	.cpu	68000
8:	move.w	(sp)+,d2
	move.l	(sp)+,d1
9:	rts

;----------------------------------------------------------------
;�L���b�V��OFF
;>d0.l:�ݒ�O�̃L���b�V�����
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ���0��Ԃ�
cacheOff:
	move.l	d2,-(sp)
	moveq.l	#0,d2
	bsr	cacheSet		;�L���b�V���ݒ�
	move.l	(sp)+,d2
	rts

;----------------------------------------------------------------
;�f�[�^�L���b�V��OFF,���߃L���b�V��ON
;>d0.l:�ݒ�O�̃L���b�V�����
;	bit0	���߃L���b�V���̏��(0=����,1=�L��)
;	bit1	�f�[�^�L���b�V���̏��(0=����,1=�L��)
;	000/010�̂Ƃ���0��Ԃ�
cacheOnI:
	move.l	d2,-(sp)
	moveq.l	#1,d2			;�f�[�^�L���b�V��OFF,���߃L���b�V��ON
	bsr	cacheSet		;�L���b�V���ݒ�
	move.l	(sp)+,d2
	rts

	PATCH_END



;----------------------------------------------------------------
;
;	MC68060�̂Ƃ�SPC��TEMP,TCH,TCM,TCL�ւ̃��C�g�Ŏg���Ă���MOVEP��W�J����
;
;	IOCS _SCSIDRV���l�߂�MOVEP�̓W�J�R�[�h����������
;	�W�J���Ȃ��Ă�MOVEP�G�~�����[�V�����Ŗ��Ȃ����삷�邪�A�f�o�b�O���̃R�[�h�Ɗ֌W�̂Ȃ���O�͎ז��Ȃ̂œW�J���Ă���
;
;	SCSIINROM/SCSIEXROM�����Ƃ�A6���W�X�^��SCSI�|�[�g�x�[�X�A�h���X��ݒ肷�閽�߂̈ʒu���ς���Ă��邱�Ƃɒ���
;
;----------------------------------------------------------------

	PATCH_DATA	scsiMovep_1,$00FFCCB8

;----------------------------------------------------------------
;IOCS _SCSIDRV���[�`��
;<d0.l:$F5
;<d1.l:SCSI�R�[���ԍ�
iocsScsidrv:
	movem.l	d1/d3/a1-a2/a6,-(sp)
	movea.l	($00FF933C)PatchZL(pc),a6	;SCSI�|�[�g�x�[�X�A�h���X
	moveq.l	#(iocsSJmptblEnd-iocsSJmptbl)/2,d0
	cmp.l	d0,d1
	blo	@f
	moveq.l	#-1,d1			;����`
@@:	add.w	d1,d1
	move.w	iocsSJmptbl(pc,d1.w),d1
	jsr	iocsSJmptbl(pc,d1.w)
	movem.l	(sp)+,d1/d3/a1-a2/a6
	rts

iocsSReserved:
	moveq.l	#-1,d0
	rts

	.dc.w	iocsSReserved-iocsSJmptbl	;����`
iocsSJmptbl:
	.dc.w	($00FFCE0E)PatchZL-iocsSJmptbl	;SCSI�R�[��$00 _S_RESET
	.dc.w	($00FFCEFC)PatchZL-iocsSJmptbl	;SCSI�R�[��$01 _S_SELECT
	.dc.w	($00FFCED6)PatchZL-iocsSJmptbl	;SCSI�R�[��$02 _S_SELECTA
	.dc.w	($00FFCFCA)PatchZL-iocsSJmptbl	;SCSI�R�[��$03 _S_CMDOUT
	.dc.w	($00FFD5C0)PatchZL-iocsSJmptbl	;SCSI�R�[��$04 _S_DATAIN
	.dc.w	($00FFD578)PatchZL-iocsSJmptbl	;SCSI�R�[��$05 _S_DATAOUT
	.dc.w	($00FFD0AA)PatchZL-iocsSJmptbl	;SCSI�R�[��$06 _S_STSIN
	.dc.w	($00FFD0E8)PatchZL-iocsSJmptbl	;SCSI�R�[��$07 _S_MSGIN
	.dc.w	($00FFD126)PatchZL-iocsSJmptbl	;SCSI�R�[��$08 _S_MSGOUT
	.dc.w	($00FFD192)PatchZL-iocsSJmptbl	;SCSI�R�[��$09 _S_PHASE
	.dc.w	($00FFD1A6)PatchZL-iocsSJmptbl	;SCSI�R�[��$0A _S_LEVEL
	.dc.w	($00FFD066)PatchZL-iocsSJmptbl	;SCSI�R�[��$0B _S_DATAINI
	.dc.w	($00FFD026)PatchZL-iocsSJmptbl	;SCSI�R�[��$0C _S_DATAOUTI
	.dc.w	($00FFD162)PatchZL-iocsSJmptbl	;SCSI�R�[��$0D _S_MSGOUTEXT
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$0E
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$0F
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$10
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$11
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$12
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$13
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$14
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$15
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$16
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$17
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$18
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$19
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1A
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1B
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1C
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1D
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1E
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$1F
	.dc.w	($00FFD612)PatchZL-iocsSJmptbl	;SCSI�R�[��$20 _S_INQUIRY
	.dc.w	($00FFD742)PatchZL-iocsSJmptbl	;SCSI�R�[��$21 _S_READ
	.dc.w	($00FFD79C)PatchZL-iocsSJmptbl	;SCSI�R�[��$22 _S_WRITE
	.dc.w	($00FFD92C)PatchZL-iocsSJmptbl	;SCSI�R�[��$23 _S_FORMAT
	.dc.w	($00FFD3F0)PatchZL-iocsSJmptbl	;SCSI�R�[��$24 _S_TESTUNIT
	.dc.w	($00FFD49C)PatchZL-iocsSJmptbl	;SCSI�R�[��$25 _S_READCAP
	.dc.w	($00FFD7FA)PatchZL-iocsSJmptbl	;SCSI�R�[��$26 _S_READEXT
	.dc.w	($00FFD860)PatchZL-iocsSJmptbl	;SCSI�R�[��$27 _S_WRITEEXT
	.dc.w	($00FFD900)PatchZL-iocsSJmptbl	;SCSI�R�[��$28 _S_VERIFYEXT
	.dc.w	($00FFD68A)PatchZL-iocsSJmptbl	;SCSI�R�[��$29 _S_MODESENSE
	.dc.w	($00FFD6CA)PatchZL-iocsSJmptbl	;SCSI�R�[��$2A _S_MODESELECT
	.dc.w	($00FFD40E)PatchZL-iocsSJmptbl	;SCSI�R�[��$2B _S_REZEROUNIT
	.dc.w	($00FFD64E)PatchZL-iocsSJmptbl	;SCSI�R�[��$2C _S_REQUEST
	.dc.w	($00FFD9F8)PatchZL-iocsSJmptbl	;SCSI�R�[��$2D _S_SEEK
	.dc.w	($00FFD42C)PatchZL-iocsSJmptbl	;SCSI�R�[��$2E _S_READI
	.dc.w	($00FFD992)PatchZL-iocsSJmptbl	;SCSI�R�[��$2F _S_STARTSTOP
	.dc.w	($00FFD9C4)PatchZL-iocsSJmptbl	;SCSI�R�[��$30 _S_SEJECT
	.dc.w	($00FFD70A)PatchZL-iocsSJmptbl	;SCSI�R�[��$31 _S_REASSIGN
	.dc.w	($00FFD960)PatchZL-iocsSJmptbl	;SCSI�R�[��$32 _S_PAMEDIUM
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$33
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$34
	.dc.w	iocsSReserved-iocsSJmptbl	;SCSI�R�[��$35
	.dc.w	($00FFDA2C)PatchZL-iocsSJmptbl	;SCSI�R�[��$36 _S_DSKINI
	.dc.w	($00FFDA6E)PatchZL-iocsSJmptbl	;SCSI�R�[��$37 _S_FORMATB
	.dc.w	($00FFDAA6)PatchZL-iocsSJmptbl	;SCSI�R�[��$38 _S_BADFMT
	.dc.w	($00FFDADE)PatchZL-iocsSJmptbl	;SCSI�R�[��$39 _S_ASSIGN
iocsSJmptblEnd:

;MOVEP.L D0,$0017(A6)
;<d0.l:�f�[�^
;<a6.l:SCSI�|�[�g�x�[�X�A�h���X
scsiMovep:
	cmpi.b	#6,$0CBC.w		;[$0CBC.w].b:[13]MPU�̎��(0=68000,1=68010,2=68020,3=68030,4=68040,6=68060)
	beq	@f
;MC68060�łȂ��Ƃ�MOVEP���߂��g��
	movep.l	d0,$0017(a6)		;[$00E96037].b:SPC TEMP Temporary Register
					;[$00E96039].b:SPC TCH Transfer Counter High
					;[$00E9603B].b:SPC TCM Transfer Counter Mid
					;[$00E9603D].b:SPC TCL Transfer Counter Low
	rts

;MC68060�̂Ƃ�MOVEP���߂��g��Ȃ�
@@:	rol.l	#8,d0
	move.b	d0,$0017(a6)		;[$00E96037].b:SPC TEMP Temporary Register
	rol.l	#8,d0
	move.b	d0,$0019(a6)		;[$00E96039].b:SPC TCH Transfer Counter High
	rol.l	#8,d0
	move.b	d0,$001B(a6)		;[$00E9603B].b:SPC TCM Transfer Counter Mid
	rol.l	#8,d0
	move.b	d0,$001D(a6)		;[$00E9603D].b:SPC TCL Transfer Counter Low
	rts

;IOCS _SCSIDRV�̓]���o�C�g���̌v�Z
;	d5=3��d5=4�̂ǂ����2048�o�C�g/�u���b�N�Ƃ݂Ȃ�
;<d3.l:�]���u���b�N��
;<d5.b:�u���b�N��(0=256,1=512,2=1024,3=2048,4=2048)
;<a3.l:�o�b�t�@�̃A�h���X
;>d3.l:�]���o�C�g��
;>a1.l:�o�b�t�@�̃A�h���X
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
;	IOCS _SCSIDRV�̓]���o�C�g���̌v�Z��2048�o�C�g/�u���b�N�ɑΉ�������
;
;	d5=3��d5=4�̂ǂ����2048�o�C�g/�u���b�N�Ƃ݂Ȃ�
;	4096�o�C�g/�u���b�N�ȏ�͔�Ή��Ƃ���
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
;	�O���t�B�b�N�֌W��IOCS�R�[�������o�[�X���[�h�ɂȂ����܂܂ɂȂ�
;	http://stdkmd.com/bugsx68k/#rom_drawmode
;
;----------------------------------------------------------------

	PATCH_DATA	drawmode,$00FFDCEA

	cmp.w	#-1,d1

	PATCH_END



;----------------------------------------------------------------
;
;	�p�b�`�f�[�^�̖���
;
;----------------------------------------------------------------

	PATCH_TAIL



	.end
