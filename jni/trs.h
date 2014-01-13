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
   Modified by Timothy Mann, 1996-2008
   $Id: trs.h,v 1.30 2009/06/15 23:36:54 mann Exp $
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

/*
 * trs.h
 */
#ifndef _TRS_H
#define _TRS_H

#include "z80.h"

#define NORMAL 0
#define EXPANDED 1
#define INVERSE 2
#define ALTERNATE 4

#define NO_PRINTER   0
#define TEXT_PRINTER 1
#ifdef MACOSX
#define EPSON_PRINTER 2
#define CGP_115_PRINTER 3
#endif


#define MAX_SCALE 4
#define STRETCH_AMOUNT 4000
#define DEFAULT_SAMPLE_RATE 44100  /* samples/sec to use for .wav files */
#define MAX_JOYSTICKS 8

extern char romfile[FILENAME_MAX];
extern char romfile3[FILENAME_MAX];
extern char romfile4p[FILENAME_MAX];
extern char trs_hard_dir[FILENAME_MAX];
extern char trs_cass_dir[FILENAME_MAX];
extern char trs_disk_set_dir[FILENAME_MAX];
extern char trs_state_dir[FILENAME_MAX];
extern char trs_printer_dir[FILENAME_MAX];
extern char trs_printer_command[256];
extern char trs_config_file[FILENAME_MAX];
extern char init_state_file[FILENAME_MAX];

extern int trs_model; /* 1, 3, 4, 5(=4p) */
extern int trs_autodelay;
extern unsigned int foreground;
extern unsigned int background;
extern unsigned int gui_foreground;
extern unsigned int gui_background;
extern int fullscreen;
extern int trs_emu_mouse;

void trs_suspend_delay(void);
void trs_restore_delay(void);
extern int trs_continuous; /* 1= run continuously,
			      0= enter debugger after instruction,
			     -1= suppress interrupt and enter debugger */
extern int trs_disk_debug_flags;
extern int trs_emtsafe;

extern int trs_parse_command_line(int argc, char **argv, int *debug);
extern int trs_write_config_file(char *filename);
extern int trs_load_config_file(char *alternate_file);

extern void trs_screen_init(void);
extern void screen_init();
extern void trs_flip_fullscreen(void);
extern void trs_rom_init(void);
extern void trs_disk_setsizes(void);
extern void trs_disk_setsteps(void);
extern void trs_screen_write_char(int position, int char_index);
extern void trs_screen_expanded(int flag);
extern void trs_screen_alternate(int flag);
extern void trs_screen_80x24(int flag);
extern void trs_screen_inverse(int flag);
extern void trs_screen_scroll(void);
extern void trs_screen_refresh(void);
extern void trs_screen_var_reset(void);

extern void trs_disk_led(int drive, int on_off);
extern void trs_hard_led(int drive, int on_off);

extern void trs_reset(int poweron);
extern void trs_exit(void);

extern void trs_kb_reset(void);
extern void trs_kb_bracket(int shifted);
extern int trs_kb_mem_read(int address);
extern int trs_next_key(int wait);
extern void trs_kb_heartbeat(void);
extern void trs_xlate_keysym(int keysym);
extern void queue_key(int key);
extern int dequeue_key(void);
extern void clear_key_queue(void);
extern void trs_skip_next_kbwait(void);
extern int stretch_amount;
extern int trs_kb_bracket_state;

extern int romin;
extern int scale_x;
extern int scale_y;
extern int resize;
extern int resize3;
extern int resize4;
extern int stretch_amount;
extern unsigned char grafyx_microlabs;
extern int trs_uart_switches;
extern int trs_show_led;
extern int window_border_width;
extern int trs_joystick_num;
extern int trs_keypad_joystick;
extern int trs_charset1;
extern int trs_charset3;
extern int trs_charset4;
extern int trs_paused;
extern int trs_printer;

extern void trs_get_event(int wait);
extern void trs_x_flush(void);

extern void trs_printer_write(int value);
extern int trs_printer_read(void);
extern int trs_printer_reset(void);

extern void trs_cassette_motor(int value);
extern void trs_cassette_out(int value);
extern int trs_cassette_in(void);
extern void trs_sound_out(int value);

extern int trs_joystick_in(void);

extern int trs_rom_size;
extern int trs_rom1_size;
extern int trs_rom3_size;
extern int trs_rom4p_size;
extern unsigned char trs_rom1[];
extern unsigned char trs_rom3[];
extern unsigned char trs_rom4p[];

extern void trs_load_compiled_rom(int size, unsigned char rom[]);
extern int trs_load_rom(char *filename);

extern unsigned char trs_interrupt_latch_read(void);
extern unsigned char trs_nmi_latch_read(void);
extern void trs_interrupt_mask_write(unsigned char);
extern void trs_nmi_mask_write(unsigned char);
extern void trs_reset_button_interrupt(int state);
extern void trs_disk_intrq_interrupt(int state);
extern void trs_disk_drq_interrupt(int state);
extern void trs_disk_motoroff_interrupt(int state);
extern void trs_uart_err_interrupt(int state);
extern void trs_uart_rcv_interrupt(int state);
extern void trs_uart_snd_interrupt(int state);
extern void trs_timer_interrupt(int state);
extern void trs_timer_init(void);
extern void trs_timer_off(void);
extern void trs_timer_on(void);
extern void trs_timer_speed(int flag);
extern void trs_cassette_rise_interrupt(int dummy);
extern void trs_cassette_fall_interrupt(int dummy);
extern void trs_cassette_clear_interrupts(void);
extern int trs_cassette_interrupts_enabled(void);
extern void trs_cassette_update(int dummy);
extern int cassette_default_sample_rate;
extern void trs_orch90_out(int chan, int value);
extern void trs_cassette_reset(void);
extern int assert_state(int dummy);
extern void transition_out(int dummy);
extern void trs_cassette_kickoff(int dummy);
extern void orch90_flush(int dummy);
extern void trs_disk_lostdata(int dummy);
extern void trs_disk_done(int dummy);
extern void trs_disk_firstdrq(int dummy);
extern void trs_uart_set_avail(int dummy);
extern void trs_uart_set_empty(int dummy);

extern void trs_disk_debug(void);
extern int trs_disk_motoroff(void);

extern void mem_video_page(int which);
extern void mem_bank(int which);
extern void mem_map(int which);
extern void mem_romin(int state);

extern void trs_debug(void);

typedef void (*trs_event_func)(int arg);
void trs_schedule_event(trs_event_func f, int arg, int tstates);
void trs_schedule_event_us(trs_event_func f, int arg, int us);
void trs_do_event(void);
void trs_cancel_event(void);
trs_event_func trs_event_scheduled(void);

void grafyx_redraw(void);
void grafyx_write_x(int value);
void grafyx_write_y(int value);
void grafyx_write_data(int value);
int grafyx_read_data(void);
void grafyx_write_mode(int value);
void grafyx_write_xoffset(int value);
void grafyx_write_yoffset(int value);
void grafyx_write_overlay(int value);
void grafyx_set_microlabs(int on_off);
int grafyx_get_microlabs(void);
void grafyx_m3_reset();
int grafyx_m3_active(); /* true if currently intercepting video i/o */
void grafyx_m3_write_mode(int value);
unsigned char grafyx_m3_read_byte(int position);
int grafyx_m3_write_byte(int position, int value);
void hrg_onoff(int enable);
void hrg_write_addr(int addr, int mask);
void hrg_write_data(int data);
int hrg_read_data(void);

void trs_get_mouse_pos(int *x, int *y, unsigned int *buttons);
void trs_set_mouse_pos(int x, int y);
void trs_get_mouse_max(int *x, int *y, unsigned int *sens);
void trs_set_mouse_max(int x, int y, unsigned int sens);
int trs_get_mouse_type(void);

extern int timer_hz;
extern int timer_overclock_rate;
extern int timer_overclock;

#endif /*_TRS_H*/
