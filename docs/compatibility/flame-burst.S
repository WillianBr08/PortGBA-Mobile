.syntax unified
.cpu arm7tdmi
.thumb
.section .entry,"ax",%progbits
.global flame_entry
flame_entry:
    ldr r3, =flame_fix + 1
    bx r3
    .align 2
    .ltorg
.section .payload,"ax",%progbits
.global flame_fix
flame_fix:
    push {r0-r3}
    ldr r3, =0x0201c6e8
    ldr r2, [r3]
    adds r2, #1
    str r2, [r3]
    ldr r0, =0x082881f6
    ldr r3, =0x080c4ef5
    bl call_r3
    pop {r0-r3}
    ldr r3, =0x0809ca45
    bx r3
call_r3:
    bx r3
    .align 2
    .ltorg
