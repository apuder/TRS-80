/* SDLTRS version Copyright (c): 2006, Mark Grebe */

/* Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
*/
/*
 * Copyright (C) 1992 Clarendon Hill Software.
 *
 * Permission is granted to any individual or institution to use, copy,
 * or redistribute this software, provided this copyright notice is retained. 
 *
 * This software is provided "as is" without any expressed or implied
 * warranty.  If this software brings on any sort of damage -- physical,
 * monetary, emotional, or brain -- too bad.  You've got no one to blame
 * but yourself. 
 *
 * The software may be modified for your own purposes, but modified versions
 * must retain this notice.
 */

/*
   Modified by Timothy Mann, 1996
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

/*#define KBDEBUG 1*/
/*#define JOYDEBUG 1*/
#define KP_JOYSTICK 1         /* emulate joystick with keypad */
/*#define KPNUM_JOYSTICK 1*/  /* emulate joystick with keypad + NumLock */
#define SHIFT_F1_IS_F13 1     /* use if X reports Shift+F1..F8 as F13..F20 */
/*#define SHIFT_F1_IS_F11 1*/ /* use if X reports Shift+F1..F10 as F11..F20 */

#include <SDL/SDL.h>
#include "z80.h"
#include "trs.h"
#include "trs_sdl_keyboard.h"

/*
 * Key event queue
 */
#define KEY_QUEUE_SIZE	(32)
static int key_queue[KEY_QUEUE_SIZE];
static int key_queue_head;
static int key_queue_entries;

/*
 * TRS-80 key matrix
 */
#define TK(a, b) (((a)<<4)+(b))
#define TK_ADDR(tk) (((tk) >> 4)&0xf)
#define TK_DATA(tk) ((tk)&0xf)
#define TK_DOWN(tk) (((tk)&0x10000) == 0)

#define TK_AtSign       TK(0, 0)  /* @   */
#define TK_A            TK(0, 1)
#define TK_B            TK(0, 2)
#define TK_C            TK(0, 3)
#define TK_D            TK(0, 4)
#define TK_E            TK(0, 5)
#define TK_F            TK(0, 6)
#define TK_G            TK(0, 7)
#define TK_H            TK(1, 0)
#define TK_I            TK(1, 1)
#define TK_J            TK(1, 2)
#define TK_K            TK(1, 3)
#define TK_L            TK(1, 4)
#define TK_M            TK(1, 5)
#define TK_N            TK(1, 6)
#define TK_O            TK(1, 7)
#define TK_P            TK(2, 0)
#define TK_Q            TK(2, 1)
#define TK_R            TK(2, 2)
#define TK_S            TK(2, 3)
#define TK_T            TK(2, 4)
#define TK_U            TK(2, 5)
#define TK_V            TK(2, 6)
#define TK_W            TK(2, 7)
#define TK_X            TK(3, 0)
#define TK_Y            TK(3, 1)
#define TK_Z            TK(3, 2)
#define TK_LeftBracket  TK(3, 3)  /* [ { */ /* not really on keyboard */
#define TK_Backslash    TK(3, 4)  /* \ | */ /* not really on keyboard */
#define TK_RightBracket TK(3, 5)  /* ] } */ /* not really on keyboard */
#define TK_Caret        TK(3, 6)  /* ^ ~ */ /* not really on keyboard */
#define TK_Underscore   TK(3, 7)  /* _   */ /* not really on keyboard */
#define TK_0            TK(4, 0)  /* 0   */
#define TK_1            TK(4, 1)  /* 1 ! */
#define TK_2            TK(4, 2)  /* 2 " */
#define TK_3            TK(4, 3)  /* 3 # */
#define TK_4            TK(4, 4)  /* 4 $ */
#define TK_5            TK(4, 5)  /* 5 % */
#define TK_6            TK(4, 6)  /* 6 & */
#define TK_7            TK(4, 7)  /* 7 ' */
#define TK_8            TK(5, 0)  /* 8 ( */
#define TK_9            TK(5, 1)  /* 9 ) */
#define TK_Colon        TK(5, 2)  /* : * */
#define TK_Semicolon    TK(5, 3)  /* ; + */
#define TK_Comma        TK(5, 4)  /* , < */
#define TK_Minus        TK(5, 5)  /* - = */
#define TK_Period       TK(5, 6)  /* . > */
#define TK_Slash        TK(5, 7)  /* / ? */
#define TK_Enter        TK(6, 0)
#define TK_Clear        TK(6, 1)
#define TK_Break        TK(6, 2)
#define TK_Up           TK(6, 3)
#define TK_Down         TK(6, 4)
#define TK_Left         TK(6, 5)
#define TK_Right        TK(6, 6)
#define TK_Space        TK(6, 7)
#define TK_LeftShift    TK(7, 0)
#define TK_RightShift   TK(7, 1)  /* M3/4 only; both shifts are 7, 0 on M1 */
#define TK_Ctrl         TK(7, 2)  /* M4 only */
#define TK_CapsLock     TK(7, 3)  /* M4 only */
#define TK_F1           TK(7, 4)  /* M4 only */
#define TK_F2           TK(7, 5)  /* M4 only */
#define TK_F3           TK(7, 6)  /* M4 only */
#define TK_Unused       TK(7, 7)

/* Fake keycodes with special meanings */
#define TK_NULL                 TK(8, 0)
#define TK_Neutral              TK(8, 1)
#define TK_ForceShift           TK(8, 2)
#define TK_ForceNoShift         TK(8, 3)
#define TK_ForceShiftPersistent TK(8, 4)
#define TK_AllKeysUp            TK(8, 5)
#define TK_Joystick             TK(10,  0)
#define TK_North                TK(10,  1)
#define TK_Northeast            TK(10,  9)
#define TK_East                 TK(10,  8)
#define TK_Southeast            TK(10, 10)
#define TK_South                TK(10,  2)
#define TK_Southwest            TK(10,  5)
#define TK_West                 TK(10,  4)
#define TK_Northwest            TK(10,  6)
#define TK_Fire                 TK(10, 16)

#define JOY_BOUNCE 20000

typedef struct
{
    int bit_action;
    int shift_action;
} KeyTable;

/* Keysyms in the extended ASCII range 0x0000 - 0x00ff */

KeyTable ascii_key_table[] = {
/* 0x0 */     { TK_NULL, TK_Neutral }, /* undefined keysyms... */
/* 0x1 */     { TK_NULL, TK_Neutral },
/* 0x2 */     { TK_NULL, TK_Neutral },
/* 0x3 */     { TK_NULL, TK_Neutral },
/* 0x4 */     { TK_NULL, TK_Neutral },
/* 0x5 */     { TK_NULL, TK_Neutral },
/* 0x6 */     { TK_NULL, TK_Neutral },
/* 0x7 */     { TK_NULL, TK_Neutral },
/* 0x8 */     { TK_Left, TK_Neutral },
/* 0x9 */     { TK_Right, TK_Neutral },
/* 0xa */     { TK_NULL, TK_Neutral },
/* 0xb */     { TK_NULL, TK_Neutral },
/* 0xc */     { TK_Clear, TK_Neutral },
/* 0xd */     { TK_Enter, TK_Neutral },
/* 0xe */     { TK_NULL, TK_Neutral },
/* 0xf */     { TK_NULL, TK_Neutral },
/* 0x10 */    { TK_NULL, TK_Neutral },
/* 0x11 */    { TK_NULL, TK_Neutral },
/* 0x12 */    { TK_NULL, TK_Neutral },
/* 0x13 */    { TK_NULL, TK_Neutral },
/* 0x14 */    { TK_NULL, TK_Neutral },
/* 0x15 */    { TK_NULL, TK_Neutral },
/* 0x16 */    { TK_NULL, TK_Neutral },
/* 0x17 */    { TK_NULL, TK_Neutral },
/* 0x18 */    { TK_NULL, TK_Neutral },
/* 0x19 */    { TK_NULL, TK_Neutral },
/* 0x1a */    { TK_NULL, TK_Neutral },
/* 0x1b */    { TK_Break, TK_Neutral },
/* 0x1c */    { TK_NULL, TK_Neutral },
/* 0x1d */    { TK_NULL, TK_Neutral },
/* 0x1e */    { TK_NULL, TK_Neutral },
/* 0x1f */    { TK_NULL, TK_Neutral }, 
/* 0x20 */    { TK_Space, TK_Neutral },
/* 0x21 */    { TK_1, TK_ForceShift },
/* 0x22 */    { TK_2, TK_ForceShift },
/* 0x23 */    { TK_3, TK_ForceShift },
/* 0x24 */    { TK_4, TK_ForceShift },
/* 0x25 */    { TK_5, TK_ForceShift },
/* 0x26 */    { TK_6, TK_ForceShift },
/* 0x27 */    { TK_7, TK_ForceShift },
/* 0x28 */    { TK_8, TK_ForceShift },
/* 0x29 */    { TK_9, TK_ForceShift },
/* 0x2a */    { TK_Colon, TK_ForceShift },
/* 0x2b */    { TK_Semicolon, TK_ForceShift },
/* 0x2c */    { TK_Comma, TK_ForceNoShift },
/* 0x2d */    { TK_Minus, TK_ForceNoShift },
/* 0x2e */    { TK_Period, TK_ForceNoShift },
/* 0x2f */    { TK_Slash, TK_ForceNoShift },
/* 0x30 */    { TK_0, TK_ForceNoShift },
/* 0x31 */    { TK_1, TK_ForceNoShift },
/* 0x32 */    { TK_2, TK_ForceNoShift },
/* 0x33 */    { TK_3, TK_ForceNoShift },
/* 0x34 */    { TK_4, TK_ForceNoShift },
/* 0x35 */    { TK_5, TK_ForceNoShift },
/* 0x36 */    { TK_6, TK_ForceNoShift },
/* 0x37 */    { TK_7, TK_ForceNoShift },
/* 0x38 */    { TK_8, TK_ForceNoShift },
/* 0x39 */    { TK_9, TK_ForceNoShift },
/* 0x3a */    { TK_Colon, TK_ForceNoShift },
/* 0x3b */    { TK_Semicolon, TK_ForceNoShift },
/* 0x3c */    { TK_Comma, TK_ForceShift },
/* 0x3d */    { TK_Minus, TK_ForceShift },
/* 0x3e */    { TK_Period, TK_ForceShift },
/* 0x3f */    { TK_Slash, TK_ForceShift },
/* 0x40 */    { TK_AtSign, TK_ForceNoShift },
/* 0x41 */    { TK_A,  TK_ForceShift },
/* 0x42 */    { TK_B,  TK_ForceShift },
/* 0x43 */    { TK_C,  TK_ForceShift },
/* 0x44 */    { TK_D,  TK_ForceShift },
/* 0x45 */    { TK_E,  TK_ForceShift },
/* 0x46 */    { TK_F,  TK_ForceShift },
/* 0x47 */    { TK_G,  TK_ForceShift },
/* 0x48 */    { TK_H,  TK_ForceShift },
/* 0x49 */    { TK_I,  TK_ForceShift },
/* 0x4a */    { TK_J,  TK_ForceShift },
/* 0x4b */    { TK_K,  TK_ForceShift },
/* 0x4c */    { TK_L,  TK_ForceShift },
/* 0x4d */    { TK_M,  TK_ForceShift },
/* 0x4e */    { TK_N,  TK_ForceShift },
/* 0x4f */    { TK_O,  TK_ForceShift },
/* 0x50 */    { TK_P,  TK_ForceShift },
/* 0x51 */    { TK_Q,  TK_ForceShift },
/* 0x52 */    { TK_R,  TK_ForceShift },
/* 0x53 */    { TK_S,  TK_ForceShift },
/* 0x54 */    { TK_T,  TK_ForceShift },
/* 0x55 */    { TK_U,  TK_ForceShift },
/* 0x56 */    { TK_V,  TK_ForceShift },
/* 0x57 */    { TK_W,  TK_ForceShift },
/* 0x58 */    { TK_X,  TK_ForceShift },
/* 0x59 */    { TK_Y,  TK_ForceShift },
/* 0x5a */    { TK_Z,  TK_ForceShift },
/* 0x5b */    { TK_LeftBracket, TK_ForceNoShift },
/* 0x5c */    { TK_Backslash, TK_ForceNoShift },
/* 0x5d */    { TK_RightBracket, TK_ForceNoShift },
/* 0x5e */    { TK_Caret, TK_ForceNoShift },
/* 0x5f */    { TK_Underscore, TK_ForceNoShift },
/* 0x60 */    { TK_AtSign,  TK_ForceShift },
/* 0x61 */    { TK_A, TK_ForceNoShift },
/* 0x62 */    { TK_B, TK_ForceNoShift },
/* 0x63 */    { TK_C, TK_ForceNoShift },
/* 0x64 */    { TK_D, TK_ForceNoShift },
/* 0x65 */    { TK_E, TK_ForceNoShift },
/* 0x66 */    { TK_F, TK_ForceNoShift },
/* 0x67 */    { TK_G, TK_ForceNoShift },
/* 0x68 */    { TK_H, TK_ForceNoShift },
/* 0x69 */    { TK_I, TK_ForceNoShift },
/* 0x6a */    { TK_J, TK_ForceNoShift },
/* 0x6b */    { TK_K, TK_ForceNoShift },
/* 0x6c */    { TK_L, TK_ForceNoShift },
/* 0x6d */    { TK_M, TK_ForceNoShift },
/* 0x6e */    { TK_N, TK_ForceNoShift },
/* 0x6f */    { TK_O, TK_ForceNoShift },
/* 0x70 */    { TK_P, TK_ForceNoShift },
/* 0x71 */    { TK_Q, TK_ForceNoShift },
/* 0x72 */    { TK_R, TK_ForceNoShift },
/* 0x73 */    { TK_S, TK_ForceNoShift },
/* 0x74 */    { TK_T, TK_ForceNoShift },
/* 0x75 */    { TK_U, TK_ForceNoShift },
/* 0x76 */    { TK_V, TK_ForceNoShift },
/* 0x77 */    { TK_W, TK_ForceNoShift },
/* 0x78 */    { TK_X, TK_ForceNoShift },
/* 0x79 */    { TK_Y, TK_ForceNoShift },
/* 0x7a */    { TK_Z, TK_ForceNoShift },
/* 0x7b */    { TK_LeftBracket, TK_ForceShift },
/* 0x7c */    { TK_Backslash, TK_ForceShift },
/* 0x7d */    { TK_RightBracket, TK_ForceShift },
/* 0x7e */    { TK_Caret, TK_ForceShift },
/* 0x7f */    { TK_Left, TK_Neutral },
/* 0x80 */    { TK_NULL, TK_Neutral },
/* 0x81 */    { TK_NULL, TK_Neutral },
/* 0x82 */    { TK_NULL, TK_Neutral },
/* 0x83 */    { TK_NULL, TK_Neutral },
/* 0x84 */    { TK_NULL, TK_Neutral },
/* 0x85 */    { TK_NULL, TK_Neutral },
/* 0x86 */    { TK_NULL, TK_Neutral },
/* 0x87 */    { TK_NULL, TK_Neutral },
/* 0x88 */    { TK_NULL, TK_Neutral },
/* 0x89 */    { TK_NULL, TK_Neutral },
/* 0x8a */    { TK_NULL, TK_Neutral },
/* 0x8b */    { TK_NULL, TK_Neutral },
/* 0x8c */    { TK_NULL, TK_Neutral },
/* 0x8d */    { TK_NULL, TK_Neutral },
/* 0x8e */    { TK_NULL, TK_Neutral },
/* 0x8f */    { TK_NULL, TK_Neutral },
/* 0x90 */    { TK_NULL, TK_Neutral },
/* 0x91 */    { TK_NULL, TK_Neutral },
/* 0x92 */    { TK_NULL, TK_Neutral },
/* 0x93 */    { TK_NULL, TK_Neutral },
/* 0x94 */    { TK_NULL, TK_Neutral },
/* 0x95 */    { TK_NULL, TK_Neutral },
/* 0x96 */    { TK_NULL, TK_Neutral },
/* 0x97 */    { TK_NULL, TK_Neutral },
/* 0x98 */    { TK_NULL, TK_Neutral },
/* 0x99 */    { TK_NULL, TK_Neutral },
/* 0x9a */    { TK_NULL, TK_Neutral },
/* 0x9b */    { TK_NULL, TK_Neutral },
/* 0x9c */    { TK_NULL, TK_Neutral },
/* 0x9d */    { TK_NULL, TK_Neutral },
/* 0x9e */    { TK_NULL, TK_Neutral },
/* 0x9f */    { TK_NULL, TK_Neutral },
/* 0xa0 */    { TK_NULL, TK_Neutral },
/* 0xa1 */    { TK_NULL, TK_Neutral },
/* 0xa2 */    { TK_NULL, TK_Neutral },
/* 0xa3 */    { TK_NULL, TK_Neutral },
/* 0xa4 */    { TK_NULL, TK_Neutral },
/* 0xa5 */    { TK_NULL, TK_Neutral },
/* 0xa6 */    { TK_NULL, TK_Neutral },
/* 0xa7 */    { TK_NULL, TK_Neutral },
/* 0xa8 */    { TK_NULL, TK_Neutral },
/* 0xa9 */    { TK_NULL, TK_Neutral },
/* 0xaa */    { TK_NULL, TK_Neutral },
/* 0xab */    { TK_NULL, TK_Neutral },
/* 0xac */    { TK_NULL, TK_Neutral },
/* 0xad */    { TK_NULL, TK_Neutral },
/* 0xae */    { TK_NULL, TK_Neutral },
/* 0xaf */    { TK_NULL, TK_Neutral },
/* 0xb0 */    { TK_NULL, TK_Neutral },
/* 0xb1 */    { TK_NULL, TK_Neutral },
/* 0xb2 */    { TK_NULL, TK_Neutral },
/* 0xb3 */    { TK_NULL, TK_Neutral },
/* 0xb4 */    { TK_NULL, TK_Neutral },
/* 0xb5 */    { TK_NULL, TK_Neutral },
/* 0xb6 */    { TK_NULL, TK_Neutral },
/* 0xb7 */    { TK_NULL, TK_Neutral },
/* 0xb8 */    { TK_NULL, TK_Neutral },
/* 0xb9 */    { TK_NULL, TK_Neutral },
/* 0xba */    { TK_NULL, TK_Neutral },
/* 0xbb */    { TK_NULL, TK_Neutral },
/* 0xbc */    { TK_NULL, TK_Neutral },
/* 0xbd */    { TK_NULL, TK_Neutral },
/* 0xbe */    { TK_NULL, TK_Neutral },
/* 0xbf */    { TK_NULL, TK_Neutral },
/* 0xc0 */    { TK_NULL, TK_Neutral },
/* 0xc1 */    { TK_NULL, TK_Neutral },
/* 0xc2 */    { TK_NULL, TK_Neutral },
/* 0xc3 */    { TK_NULL, TK_Neutral },
/* 0xc4 */    { TK_LeftBracket, TK_ForceShift },    /* Ä */
/* 0xc5 */    { TK_NULL, TK_Neutral },
/* 0xc6 */    { TK_NULL, TK_Neutral },
/* 0xc7 */    { TK_NULL, TK_Neutral },
/* 0xc8 */    { TK_NULL, TK_Neutral },
/* 0xc9 */    { TK_NULL, TK_Neutral },
/* 0xca */    { TK_NULL, TK_Neutral },
/* 0xcb */    { TK_NULL, TK_Neutral },
/* 0xcc */    { TK_NULL, TK_Neutral },
/* 0xcd */    { TK_NULL, TK_Neutral },
/* 0xce */    { TK_NULL, TK_Neutral },
/* 0xcf */    { TK_NULL, TK_Neutral },
/* 0xd0 */    { TK_NULL, TK_Neutral },
/* 0xd1 */    { TK_NULL, TK_Neutral },
/* 0xd2 */    { TK_NULL, TK_Neutral },
/* 0xd3 */    { TK_NULL, TK_Neutral },
/* 0xd4 */    { TK_NULL, TK_Neutral },
/* 0xd5 */    { TK_NULL, TK_Neutral },
/* 0xd6 */    { TK_Backslash, TK_ForceShift },      /* Ö */
/* 0xd7 */    { TK_NULL, TK_Neutral },
/* 0xd8 */    { TK_NULL, TK_Neutral },
/* 0xd9 */    { TK_NULL, TK_Neutral },
/* 0xda */    { TK_NULL, TK_Neutral },
/* 0xdb */    { TK_NULL, TK_Neutral },
/* 0xdc */    { TK_RightBracket, TK_ForceShift },   /* Ü */
/* 0xdd */    { TK_NULL, TK_Neutral },
/* 0xde */    { TK_NULL, TK_Neutral },
/* 0xdf */    { TK_Caret, TK_ForceNoShift },        /* ß */
/* 0xe0 */    { TK_NULL, TK_Neutral },
/* 0xe1 */    { TK_NULL, TK_Neutral },
/* 0xe2 */    { TK_NULL, TK_Neutral },
/* 0xe3 */    { TK_NULL, TK_Neutral },
/* 0xe4 */    { TK_LeftBracket, TK_ForceNoShift },  /* ä */
/* 0xe5 */    { TK_NULL, TK_Neutral },
/* 0xe6 */    { TK_NULL, TK_Neutral },
/* 0xe7 */    { TK_NULL, TK_Neutral },
/* 0xe8 */    { TK_NULL, TK_Neutral },
/* 0xe9 */    { TK_NULL, TK_Neutral },
/* 0xea */    { TK_NULL, TK_Neutral },
/* 0xeb */    { TK_NULL, TK_Neutral },
/* 0xec */    { TK_NULL, TK_Neutral },
/* 0xed */    { TK_NULL, TK_Neutral },
/* 0xee */    { TK_NULL, TK_Neutral },
/* 0xef */    { TK_NULL, TK_Neutral },
/* 0xf0 */    { TK_NULL, TK_Neutral },
/* 0xf1 */    { TK_NULL, TK_Neutral },
/* 0xf2 */    { TK_NULL, TK_Neutral },
/* 0xf3 */    { TK_NULL, TK_Neutral },
/* 0xf4 */    { TK_NULL, TK_Neutral },
/* 0xf5 */    { TK_NULL, TK_Neutral },
/* 0xf6 */    { TK_Backslash, TK_ForceNoShift },    /* ö */
/* 0xf7 */    { TK_NULL, TK_Neutral },
/* 0xf8 */    { TK_NULL, TK_Neutral },
/* 0xf9 */    { TK_NULL, TK_Neutral },
/* 0xfa */    { TK_NULL, TK_Neutral },
/* 0xfb */    { TK_NULL, TK_Neutral },
/* 0xfc */    { TK_RightBracket, TK_ForceNoShift }, /* ü */
/* 0xfd */    { TK_NULL, TK_Neutral },
/* 0xfe */    { TK_NULL, TK_Neutral },
/* 0xff */    { TK_NULL, TK_Neutral },
/* 0x100 */    { TK_Fire, TK_Neutral },
/* 0x101 */    { TK_Northwest, TK_Neutral },
/* 0x102 */    { TK_South, TK_Neutral },
/* 0x103 */    { TK_Southeast, TK_Neutral },
/* 0x104 */    { TK_West, TK_Neutral },
/* 0x105 */    { TK_NULL, TK_Neutral },
/* 0x106 */    { TK_East, TK_Neutral },
/* 0x107 */    { TK_Southwest, TK_Neutral },
/* 0x108 */    { TK_North, TK_Neutral },
/* 0x109 */    { TK_Northeast, TK_Neutral },
/* 0x10a */    { TK_Left, TK_Neutral },
/* 0x10b */    { TK_Slash, TK_Neutral },
/* 0x10c */    { TK_Colon, TK_ForceShift },
/* 0x10d */    { TK_Minus, TK_Neutral },
/* 0x10e */    { TK_Semicolon, TK_ForceShift },
/* 0x10f */    { TK_Enter, TK_Neutral },
/* 0x110 */    { TK_Minus,  TK_ForceShift },
/* 0x111 */    { TK_Up, TK_Neutral },
/* 0x112 */    { TK_Down, TK_Neutral },
/* 0x113 */    { TK_Right, TK_Neutral },
/* 0x114 */    { TK_Left, TK_Neutral },
/* 0x115 */    { TK_Underscore, TK_Neutral },
/* 0x116 */    { TK_Clear, TK_Neutral },
/* 0x117 */    { TK_Unused, TK_Neutral },
/* 0x118 */    { TK_LeftShift, TK_Neutral },
/* 0x119 */    { TK_RightShift, TK_Neutral },
/* 0x11a */    { TK_F1, TK_Neutral },
/* 0x11b */    { TK_F2, TK_Neutral },
/* 0x11c */    { TK_F3, TK_Neutral },
/* 0x11d */    { TK_CapsLock, TK_Neutral },
/* 0x11e */    { TK_AtSign, TK_Neutral },
/* 0x11f */    { TK_0, TK_Neutral },
/* 0x120 */    { TK_NULL, TK_Neutral },
/* 0x121 */    { TK_NULL, TK_Neutral },
/* 0x122 */    { TK_NULL, TK_Neutral },
/* 0x123 */    { TK_NULL, TK_Neutral },
/* 0x124 */    { TK_NULL, TK_Neutral },
/* 0x125 */    { TK_NULL, TK_Neutral },
/* 0x126 */    { TK_NULL, TK_Neutral },
/* 0x127 */    { TK_NULL, TK_Neutral },
/* 0x128 */    { TK_NULL, TK_Neutral },
/* 0x129 */    { TK_NULL, TK_Neutral },
/* 0x12a */    { TK_NULL, TK_Neutral },
/* 0x12b */    { TK_NULL, TK_Neutral },
/* 0x12c */    { TK_NULL, TK_Neutral },
/* 0x12d */    { TK_NULL, TK_Neutral },
/* 0x12e */    { TK_AtSign, TK_Neutral },
/* 0x12f */    { TK_RightShift, TK_Neutral },
/* 0x130 */    { TK_LeftShift, TK_Neutral },
/* 0x131 */    { TK_Ctrl, TK_Neutral },
/* 0x132 */    { TK_Ctrl, TK_Neutral },
#ifdef MACOSX
/* 0x133 */    { TK_NULL, TK_Neutral },
#else
/* 0x133 */    { TK_Down, TK_ForceShiftPersistent },
#endif
/* 0x134 */    { TK_NULL, TK_Neutral },
#ifdef MACOSX
/* 0x135 */    { TK_Down, TK_ForceShiftPersistent },
#else
/* 0x135 */    { TK_NULL, TK_Neutral },
#endif
/* 0x136 */    { TK_NULL, TK_Neutral },
/* 0x137 */    { TK_NULL, TK_Neutral },
/* 0x138 */    { TK_NULL, TK_Neutral },
/* 0x139 */    { TK_NULL, TK_Neutral },
/* 0x13a */    { TK_NULL, TK_Neutral },
/* 0x13b */    { TK_NULL, TK_Neutral },
/* 0x13c */    { TK_NULL, TK_Neutral },
/* 0x13d */    { TK_NULL, TK_Neutral },
/* 0x13e */    { TK_Break, TK_Neutral },
/* 0x13f */    { TK_NULL, TK_Neutral },
/* 0x140 */    { TK_NULL, TK_Neutral },
/* 0x141 */    { TK_NULL, TK_Neutral },
/* 0x142 */    { TK_NULL, TK_Neutral },
};

static int keystate[8] = { 0, };
static int force_shift = TK_Neutral;
static int joystate = 0;
int trs_joystick_num = 0;
int trs_keypad_joystick = TRUE;

/* Avoid changing state too fast so keystrokes aren't lost. */
static tstate_t key_stretch_timeout;
int stretch_amount = STRETCH_AMOUNT;
int trs_kb_bracket_state = 0;

void trs_keyboard_save(FILE *file)
{
  fwrite(&keystate,8,sizeof(int),file);
  fwrite(&force_shift,1,sizeof(int),file);
  fwrite(&joystate,1,sizeof(int),file);
  fwrite(&key_stretch_timeout,1,sizeof(long long),file);
  fwrite(&stretch_amount,1,sizeof(int),file);
  fwrite(&trs_kb_bracket_state,1,sizeof(int),file);
}

void trs_keyboard_load(FILE *file)
{
  fread(&keystate,8,sizeof(int),file);
  fread(&force_shift,1,sizeof(int),file);
  fread(&joystate,1,sizeof(int),file);
  fread(&key_stretch_timeout,1,sizeof(long long),file);
  fread(&stretch_amount,1,sizeof(int),file);
  fread(&trs_kb_bracket_state,1,sizeof(int),file);
}

void trs_kb_reset()
{
  key_stretch_timeout = z80_state.t_count;
}

int key_heartbeat = 0;
void trs_kb_heartbeat()
{
  /* Don't hold keys in queue too long */
  key_heartbeat++;
}

void trs_kb_bracket(int shifted)
{
  /* Set the shift state for the emulation of the "[ {", "\ |", 
     "] }", "^ ~", and "_ DEL" keys.  Some Model 4 keyboard drivers
     decode these with [ shifted and { unshifted, etc., while most
     other keyboard drivers either ignore them or decode them with
     [ unshifted and { shifted.  We default to the latter.  Note that
     these keys didn't exist on real machines anyway.
  */
  int i;
  trs_kb_bracket_state = shifted;
  for (i=0x5b; i<=0x5f; i++) {
    ascii_key_table[i].shift_action =
      shifted ? TK_ForceShift : TK_ForceNoShift;
  }
  for (i=0x7b; i<0x7f; i++) {
    ascii_key_table[i].shift_action =
      shifted ? TK_ForceNoShift : TK_ForceShift;
  }
}

/* Emulate joystick with the keypad */
int trs_emulate_joystick(int key_down, int bit_action)
{
  if (bit_action < TK_Joystick) return 0;
  if (key_down) {
    joystate |= (bit_action & 0x1f);
  } else {
    joystate &= ~(bit_action & 0x1f);
  }
  return 1;
}

/* Joystick functions called when SDL Joystick events occur */
void trs_joy_button_down(void)
{
  joystate |= (TK_Fire & 0x1f);
}

void trs_joy_button_up(void)
{
  joystate &= ~(TK_Fire & 0x1f);
}

void trs_joy_hat(unsigned char value)
{
  joystate &= (TK_Fire & 0x1f);
  
  switch(value) {
    case SDL_HAT_CENTERED:
      break;
    case SDL_HAT_UP:
      joystate |= (TK_North & 0x1f);
      break;
    case SDL_HAT_RIGHT:
      joystate |= (TK_East & 0x1f);
      break;
    case SDL_HAT_DOWN:
      joystate |= (TK_South & 0x1f);
      break;
    case SDL_HAT_LEFT:
      joystate |= (TK_West & 0x1f);
      break;
    case SDL_HAT_RIGHTUP:
      joystate |= (TK_Northeast & 0x1f);
      break;
    case SDL_HAT_RIGHTDOWN:
      joystate |= (TK_Southeast & 0x1f);
      break;
    case SDL_HAT_LEFTUP:
      joystate |= (TK_Southwest & 0x1f);
      break;
    case SDL_HAT_LEFTDOWN:
      joystate |= (TK_Northwest & 0x1f);
      break;
    }
}

void trs_set_keypad_joystick(void)
{
  if (trs_keypad_joystick) {
    ascii_key_table[0x100].bit_action = TK_Fire;
    ascii_key_table[0x101].bit_action = TK_Northwest;
    ascii_key_table[0x102].bit_action = TK_South;
    ascii_key_table[0x103].bit_action = TK_Southeast;
    ascii_key_table[0x104].bit_action = TK_West;
    ascii_key_table[0x105].bit_action = TK_NULL;
    ascii_key_table[0x106].bit_action = TK_East;
    ascii_key_table[0x107].bit_action = TK_Southwest;
    ascii_key_table[0x108].bit_action = TK_North;
    ascii_key_table[0x109].bit_action = TK_Northeast;
  } else {
    ascii_key_table[0x100].bit_action = TK_0;
    ascii_key_table[0x101].bit_action = TK_1;
    ascii_key_table[0x102].bit_action = TK_2;
    ascii_key_table[0x103].bit_action = TK_3;
    ascii_key_table[0x104].bit_action = TK_4;
    ascii_key_table[0x105].bit_action = TK_5;
    ascii_key_table[0x106].bit_action = TK_6;
    ascii_key_table[0x107].bit_action = TK_7;
    ascii_key_table[0x108].bit_action = TK_8;
    ascii_key_table[0x109].bit_action = TK_9;
  }
}

void trs_open_joystick(void)
{
  static SDL_Joystick *open_joy = NULL;
  int num_joysticks = SDL_NumJoysticks();
  
  if (open_joy != NULL) {
    SDL_JoystickClose(open_joy);
    open_joy = NULL;
 }

  if ((trs_joystick_num != -1) &&
      (trs_joystick_num <= (num_joysticks -1))) {
      open_joy = SDL_JoystickOpen(trs_joystick_num);
  }
  else
    trs_joystick_num = -1;
}

void trs_joy_axis(unsigned char axis, short value)
{
  int dir;
  
  if (value < -JOY_BOUNCE)
    dir = -1;
  else if (value > JOY_BOUNCE)
    dir = 1;
  else
    dir = 0;
    
  if (axis == 0) {
    switch (dir) {
      case -1:
        joystate |= (TK_West & 0x1f);
        joystate &= ~(TK_East & 0x1f);
        break;
      case 0:
        joystate &= ~((TK_West | TK_East) & 0x1f);
        break;
      case 1:
        joystate |= (TK_East & 0x1f);
        joystate &= ~(TK_West & 0x1f);
        break;
    }
  }
  else if (axis == 1) {
    switch (dir) {
      case -1:
        joystate |= (TK_North & 0x1f);
        joystate &= ~(TK_South & 0x1f);
        break;
      case 0:
        joystate &= ~((TK_North | TK_South) & 0x1f);
        break;
      case 1:
        joystate |= (TK_South & 0x1f);
        joystate &= ~(TK_North & 0x1f);
        break;
    }
  }
}

int trs_joystick_in()
{
#if JOYDEBUG
  debug("joy %02x ", joystate);
#endif
  return ~joystate;
}

void trs_xlate_keysym(int keysym)
{
    int key_down;
    KeyTable* kt;
    static int shift_action = TK_Neutral;

    if (keysym == 0x10000) {
	/* force all keys up */
	queue_key(TK_AllKeysUp);
	shift_action = TK_Neutral;
	return;
    }

    key_down = (keysym & 0x10000) == 0;
    kt = &ascii_key_table[keysym & 0xFFFF];

    if (kt->bit_action == TK_NULL) return;
    if (trs_emulate_joystick(key_down, kt->bit_action)) return;

    if (key_down) {
      if (shift_action != TK_ForceShiftPersistent &&
	  shift_action != kt->shift_action) {
	shift_action = kt->shift_action;
	queue_key(shift_action);
      }
      queue_key(kt->bit_action);
    } else {
      queue_key(kt->bit_action | 0x10000);
      if (shift_action != TK_Neutral &&
	  shift_action == kt->shift_action) {
	shift_action = TK_Neutral;
	queue_key(shift_action);
      }
    }
}

static void change_keystate(int action)
{
    int key_down;
    int i;
#ifdef KBDEBUG
    debug("change_keystate: action 0x%x\n", action);
#endif

    switch (action) {
      case TK_AllKeysUp:
	/* force all keys up */
	for (i=0; i<7; i++) {
	    keystate[i] = 0;
	}
	force_shift = TK_Neutral;
	break;

      case TK_Neutral:
      case TK_ForceShift:
      case TK_ForceNoShift:
      case TK_ForceShiftPersistent:
	force_shift = action;
	break;

      default:
	key_down = TK_DOWN(action);
	if (key_down) {
	    keystate[TK_ADDR(action)] |= (1 << TK_DATA(action));
	} else {
	    keystate[TK_ADDR(action)] &= ~(1 << TK_DATA(action));
	}
    }
}

static int kb_mem_value(int address)
{
    int i, bitpos, data = 0;

    for (i=0, bitpos=1; i<7; i++, bitpos<<=1) {
	if (address & bitpos) {
	    data |= keystate[i];
	}
    }
    if (address & 0x80) {
	int tmp = keystate[7];
	if (trs_model == 1) {
	    if (force_shift == TK_ForceNoShift) {
		/* deactivate shift key */
		tmp &= ~1;
	    } else if (force_shift != TK_Neutral) {
		/* activate shift key */
		tmp |= 1;
	    }
	} else {
	    if (force_shift == TK_ForceNoShift) {
		/* deactivate both shift keys */
		tmp &= ~3;
	    } else if (force_shift != TK_Neutral) {
		/* if no shift keys are down, activate left shift key */
		if ((tmp & 3) == 0) tmp |= 1;
	    }
	}
	data |= tmp;
    }
    return data;
}

int trs_kb_mem_read(int address)
{
    int key = -1;
    int i, wait;
    static int recursion = 0;
    static int timesseen;

    /* Prevent endless recursive calls to this routine (by mem_read_word
       below) if REG_SP happens to point to keyboard memory. */
    if (recursion) return 0;

    /* Avoid delaying key state changes in queue for too long */
    if (key_heartbeat > 2) {
      do {
	key = trs_next_key(0);
	if (key >= 0) {
	  change_keystate(key);
	  timesseen = 1;
	}
      } while (key >= 0);
    }

    /* After each key state change, impose a timeout before the next one
       so that the Z-80 program doesn't miss any by polling too rarely,
       and so that we don't tickle the bugs in some common TRS-80 keyboard
       drivers that strike if two keys change simultaneously */
    if (key_stretch_timeout - z80_state.t_count > TSTATE_T_MID) {

	/* Check if we are in the system keyboard driver, called from
	   the wait-for-input routine.  If so, and there are no
	   keystrokes queued, and the current state has been seen by
	   at least 16 such reads, then trs_next_key will pause the
	   process to avoid burning host CPU needlessly.

	   The test below works on both Model I and III and is
	   insensitive to what keyboard driver is being used, as long
	   as it is called through the wait-for-key routine at ROM
	   address 0x0049 and has not pushed too much on the stack yet
	   when it first reads from the key matrix.  The search is
	   needed (at least) for NEWDOS80, which pushes 2 extra bytes
	   on the stack.  */
	wait = 0;
	if (timesseen++ >= 16) {
	  recursion = 1;
	  for (i=0; i<=4; i+=2) {
	    if (mem_read_word(REG_SP + 2 + i) == 0x4015) {
	      wait = mem_read_word(REG_SP + 10 + i) == 0x004c;
	      break;
	    }
	  }
	  recursion = 0;
	}
	/* Get the next key */
	key = trs_next_key(wait);
	key_stretch_timeout = z80_state.t_count + stretch_amount;
    }

    if (key >= 0) {
      change_keystate(key);
      timesseen = 1;
    }
    key_heartbeat = 0;
    return kb_mem_value(address);
}

void clear_key_queue()
{
  key_queue_head = 0;
  key_queue_entries = 0;
#if QDEBUG
    debug("clear_key_queue\n");
#endif
}

void queue_key(int state)
{
  key_queue[(key_queue_head + key_queue_entries) % KEY_QUEUE_SIZE] = state;
#if QDEBUG
  debug("queue_key 0x%x\n", state);
#endif
  if (key_queue_entries < KEY_QUEUE_SIZE) {
    key_queue_entries++;
  } else {
#if QDEBUG
    debug("queue_key overflow\n");
#endif
  }
}

int dequeue_key()
{
  int rval = -1;

  if(key_queue_entries > 0)
    {
      rval = key_queue[key_queue_head];
      key_queue_head = (key_queue_head + 1) % KEY_QUEUE_SIZE;
      key_queue_entries--;
#if QDEBUG
      debug("dequeue_key 0x%x\n", rval);
#endif
    }
  return rval;
}

int trs_next_key(int wait)
{
  return dequeue_key();

}
