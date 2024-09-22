	.text
_sqr:
	sw    $ra, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	sw    $fp, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#This is Setting the FP
	addu  $fp, $sp, 8
#This is pushing space for locals
	subu  $sp, $sp, 0
	lw    $t0, 4($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	mult  $t0, $t1
	mflo  $t0
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $v0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
#Loading the return address
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
	.text
_sqrt:
	sw    $ra, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	sw    $fp, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#This is Setting the FP
	addu  $fp, $sp, 8
#This is pushing space for locals
	subu  $sp, $sp, 20
#Generate intval
	li    $t0, 46341
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -8($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
#Generate intval
	li    $t0, 0
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -12($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, -8($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -16($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
.L0:		# begin while
	lw    $t0, -12($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, -16($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	sle   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	beq   $t0, 0, .L1
	lw    $t0, -12($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, -16($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	add   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generate intval
	li    $t0, 2
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	div   $t0, $t1
	mflo  $t0
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	jal   _sqr
	add   $sp, 4
	sw    $v0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -24($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	seq   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	li    $t1, 0
	beq   $t0, $t1, .L2
	lw    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $v0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
	b     .L3
.L2:
	lw    $t0, -24($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	sgt   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	li    $t1, 0
	beq   $t0, $t1, .L4
	lw    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generate intval
	li    $t0, 1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	sub   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -16($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	b     .L5
.L4:
	lw    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generate intval
	li    $t0, 1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	add   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generating Indexed
	la    $t0, -12($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	sw    $t1, 0($t0)
	sw    $t1, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
.L5:
.L3:
	b     .L0
.L1:		# end while
	lw    $t0, -24($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	sgt   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	li    $t1, 0
	beq   $t0, $t1, .L6
	lw    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#Generate intval
	li    $t0, 1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	# POP
	addu  $sp, $sp, 4
	sub   $t0, $t0, $t1
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $v0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
	b     .L7
.L6:
	lw    $t0, -20($fp)
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $v0, 4($sp)	# POP
	addu  $sp, $sp, 4
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
.L7:
#Loading the return address
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	jr    $ra
	.text
	.globl main
main:
	sw    $ra, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	sw    $fp, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#This is Setting the FP
	addu  $fp, $sp, 8
#This is pushing space for locals
	subu  $sp, $sp, 0
#Generate intval
	li    $t0, 22
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	jal   _sqrt
	add   $sp, 4
	sw    $v0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
	lw    $a0, 4($sp)	# POP
	addu  $sp, $sp, 4
	li    $v0, 1
	syscall
	.data
.L8:	.asciiz "\n"	# string literal
	.text
	la    $t0, .L8
	sw    $t0, 0($sp)	# PUSH
	subu  $sp, $sp, 4
#WRITE STRING
	lw    $a0, 4($sp)	# POP
	addu  $sp, $sp, 4
	li    $v0, 4
	syscall
#Loading the return address
	lw    $ra, 0($fp)
	move  $t0, $fp
#Restoring FP
	lw    $fp, -4($fp)
#Restoring SP
	move  $sp, $t0
	li    $v0, 10
	syscall
