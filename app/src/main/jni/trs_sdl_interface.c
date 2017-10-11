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
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:56:00 MST 2006 by markgrebe
*/

/*#define MOUSEDEBUG 1*/
/*#define XDEBUG 1*/
/*#define QDEBUG 1*/

/*
 * trs_sdl_interface.c
 *
 * SDL interface for TRS-80 simulator
 */

#include <stdio.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/file.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

#include "trs_iodefs.h"
#include "trs.h"
#include "z80.h"
#include "trs_disk.h"
#include "trs_uart.h"
#include "trs_state_save.h"
#ifndef ANDROID
#include "trs_sdl_gui.h"
#else
#include "atrs.h"
#endif
#include "trs_cassette.h"
#include "trs_sdl_keyboard.h"

#include "SDL/SDL.h"
#include "blit.h"

extern int trs_timer_is_turbo();
extern int trs_timer_switch_turbo();

extern char trs_char_data[][MAXCHARS][TRS_CHAR_HEIGHT];

#define MAX_RECTS 2048
#define WHITE 0xffffff
#define BLACK 0
#define GREEN 0x008010

#ifdef MACOSX
#include "macosx/trs_mac_interface.h"
#define MENU_MOD KMOD_LMETA
void restoreAppWindowPrefs();
#else
#define MENU_MOD KMOD_LALT
#endif


/* Public data */
int screen_chars = 1024;
int row_chars = 64;
int col_chars = 16;
int border_width = 2;
int window_border_width = 2;
unsigned int foreground = WHITE;
unsigned int background = 0;
unsigned int gui_foreground = WHITE;
unsigned int gui_background = GREEN;
int trs_show_led = 1;
int text80x24 = 0, screen640x240 = 0;
int scale_x = 1;
int scale_y = 2;
char romfile[FILENAME_MAX];
char romfile3[FILENAME_MAX];
char romfile4p[FILENAME_MAX];
int fullscreen = 0;
int drawnRectCount = 0;
int resize = 0;
int resize3 = 1;
int resize4 = 0;
int trs_paused = 0;

char trs_disk_dir[FILENAME_MAX] = "disks";
char trs_hard_dir[FILENAME_MAX] = "harddisks";
char trs_cass_dir[FILENAME_MAX] = "cassettes";
char trs_disk_set_dir[FILENAME_MAX] = "disksets";
char trs_state_dir[FILENAME_MAX] = "savedstates";
char trs_printer_dir[FILENAME_MAX] = "printer";
char trs_config_file[FILENAME_MAX];
char init_state_file[FILENAME_MAX];
char trs_printer_command[256];
int trs_charset = 3;
int trs_charset1 = 3;
int trs_charset3 = 4;
int trs_charset4 = 8;

int trs_emu_mouse = FALSE;

/* Private data */
#ifdef ANDROID
extern
#else
static
#endif
unsigned char* trs_screen;
static unsigned char trs_gui_screen[2048];
static unsigned char trs_gui_screen_invert[2048];
static int top_margin = 0;
static int left_margin = 0;
static int led_width = 0;
static int currentmode = NORMAL;
static int OrigHeight,OrigWidth;
static int cur_char_width = TRS_CHAR_WIDTH;
static int cur_char_height = TRS_CHAR_HEIGHT * 2;
static int disksizes[8] = {5,5,5,5,8,8,8,8};
#ifdef MACOSX
static int disksizesLoaded;
#endif
static int disksteps[8] = {1,1,1,1,2,2,2,2};
static SDL_Surface *trs_char[6][MAXCHARS];
static SDL_Surface *trs_box[3][64];
static SDL_Surface *image;
static SDL_Surface *screen;
static SDL_Rect drawnRects[MAX_RECTS];
static Uint32 light_red;
static Uint32 bright_red;

#define PASTE_IDLE    0
#define PASTE_GETNEXT 1
#define PASTE_KEYDOWN 2
#define PASTE_KEYUP   3
static int paste_state = PASTE_IDLE;
static int paste_lastkey = FALSE;
extern int  PasteManagerStartPaste(void);
extern void PasteManagerStartCopy(unsigned char *string);
extern int PasteManagerGetChar(unsigned short *character);

#define COPY_OFF       0
#define COPY_IDLE      1
#define COPY_STARTED   2
#define COPY_DEFINED   3
#define COPY_CLEAR     4
static int copyStatus = COPY_IDLE;
static int selectionStartX = 0;
static int selectionStartY = 0;
static int selectionEndX = 0;
static int selectionEndY = 0;
int requestSelectAll = FALSE;

/* Support for Micro Labs Grafyx Solution and Radio Shack hi-res card */

/* True size of graphics memory -- some is offscreen */
#define G_XSIZE 128
#define G_YSIZE 256
char grafyx[(2*G_YSIZE*MAX_SCALE) * (G_XSIZE*MAX_SCALE)];
unsigned char grafyx_unscaled[G_YSIZE][G_XSIZE];

unsigned char grafyx_microlabs = 0;
unsigned char grafyx_x = 0, grafyx_y = 0, grafyx_mode = 0;
unsigned char grafyx_enable = 0;
unsigned char grafyx_overlay = 0;
unsigned char grafyx_xoffset = 0, grafyx_yoffset = 0;

/* Port 0x83 (grafyx_mode) bits */
#define G_ENABLE    1
#define G_UL_NOTEXT 2   /* Micro Labs only */
#define G_RS_WAIT   2   /* Radio Shack only */
#define G_XDEC      4
#define G_YDEC      8
#define G_XNOCLKR   16
#define G_YNOCLKR   32
#define G_XNOCLKW   64
#define G_YNOCLKW   128

/* Port 0xFF (grafyx_m3_mode) bits */
#define G3_COORD    0x80
#define G3_ENABLE   0x40
#define G3_COMMAND  0x20
#define G3_YLOW(v)  (((v)&0x1e)>>1)
     
typedef struct image_size_type {
    unsigned int width;
    unsigned int height;
    unsigned int bytes_per_line;
} IMAGE_SIZE_TYPE;

IMAGE_SIZE_TYPE imageSize = {
  /*width, height*/    8*G_XSIZE, 2*G_YSIZE,  /* if scale_x=1, scale_y=2 */
  /*bytes_per_line*/   G_XSIZE,  /* if scale = 1 */
};

#define HRG_MEMSIZE (1024 * 12)	/* 12k * 8 bit graphics memory */
static unsigned char hrg_screen[HRG_MEMSIZE];
static int hrg_pixel_x[2][6+1];
static int hrg_pixel_y[12+1];
static int hrg_pixel_width[2][6];
static int hrg_pixel_height[12];
static int hrg_enable = 0;
static int hrg_addr = 0;
static void hrg_update_char(int position);

/*
 * Key event queueing routines
 */
#define KEY_QUEUE_SIZE	(32)
static int key_queue[KEY_QUEUE_SIZE];
static int key_queue_head;
static int key_queue_entries;

/* dummy buffer for stat() call */
struct stat statbuf;

/* Option handling */
typedef struct trs_opt_struct {
  char *name;
  void (*handler)(char *,int, char *);
  int hasArg;
  int intArg;
  char *strArg;
} trs_opt;

static void trs_opt_scale(char *arg, int intarg, char *stringarg);
static void trs_opt_scale2(char *arg, int intarg, char *stringarg);
static void trs_opt_resize3(char *arg, int intarg, char *stringarg);
static void trs_opt_resize4(char *arg, int intarg, char *stringarg);
static void trs_opt_fullscreen(char *arg, int intarg, char *stringarg);
static void trs_opt_model(char *arg, int intarg, char *stringarg);
static void trs_opt_model2(char *arg, int intarg, char *stringarg);
static void trs_opt_charset1(char *arg, int intarg, char *stringarg);
static void trs_opt_charset3(char *arg, int intarg, char *stringarg);
static void trs_opt_charset4(char *arg, int intarg, char *stringarg);
static void trs_opt_string(char *arg, int intarg, char *stringarg);
static void trs_opt_disk(char *arg, int intarg, char *stringarg);
static void trs_opt_hard(char *arg, int intarg, char *stringarg);
static void trs_opt_cass(char *arg, int intarg, char *stringarg);
static void trs_opt_keystretch(char *arg, int intarg, char *stringarg);
static void trs_opt_borderwidth(char *arg, int intarg, char *stringarg);
static void trs_opt_microlabs(char *arg, int intarg, char *stringarg);
static void trs_opt_led(char *arg, int intarg, char *stringarg);
static void trs_opt_doubler(char *arg, int intarg, char *stringarg);
static void trs_opt_sizemap(char *arg, int intarg, char *stringarg);
#ifdef __linux
static void trs_opt_stepmap(char *arg, int intarg, char *stringarg);
#endif
static void trs_opt_truedam(char *arg, int intarg, char *stringarg);
static void trs_opt_samplerate(char *arg, int intarg, char *stringarg);
static void trs_opt_switches(char *arg, int intarg, char *stringarg);
static void trs_opt_shiftbracket(char *arg, int intarg, char *stringarg);
static void trs_opt_keypadjoy(char *arg, int intarg, char *stringarg);
static void trs_opt_joysticknum(char *arg, int intarg, char *stringarg);
static void trs_opt_foreground(char *arg, int intarg, char *stringarg);
static void trs_opt_background(char *arg, int intarg, char *stringarg);
static void trs_opt_guiforeground(char *arg, int intarg, char *stringarg);
static void trs_opt_guibackground(char *arg, int intarg, char *stringarg);
static void trs_opt_emtsafe(char *arg, int intarg, char *stringarg);
static void trs_opt_turbo(char *arg, int intarg, char *stringarg);
static void trs_opt_turborate(char *arg, int intarg, char *stringarg);

trs_opt options[] = {
{"scale",trs_opt_scale,1,0,NULL},
{"scale1",trs_opt_scale2,0,1,NULL},
{"scale2",trs_opt_scale2,0,2,NULL},
{"scale3",trs_opt_scale2,0,3,NULL},
{"scale4",trs_opt_scale2,0,4,NULL},
{"resize3",trs_opt_resize3,0,1,NULL},
{"resize4",trs_opt_resize4,0,1,NULL},
{"noresize3",trs_opt_resize3,0,0,NULL},
{"noresize4",trs_opt_resize4,0,0,NULL},
{"fullscreen",trs_opt_fullscreen,0,1,NULL},
{"nofullscreen",trs_opt_fullscreen,0,0,NULL},
{"model",trs_opt_model,1,0,NULL},
{"model1",trs_opt_model2,0,1,NULL},
{"model3",trs_opt_model2,0,3,NULL},
{"model4",trs_opt_model2,0,4,NULL},
{"model4p",trs_opt_model2,0,5,NULL},
{"charset1",trs_opt_charset1,1,0,NULL},
{"charset3",trs_opt_charset3,1,0,NULL},
{"charset4",trs_opt_charset4,1,0,NULL},
{"romfile",trs_opt_string,1,0,romfile},
{"romfile3",trs_opt_string,1,0,romfile3},
{"romfile4p",trs_opt_string,1,0,romfile4p},
{"disk0",trs_opt_disk,1,0,NULL},
{"disk1",trs_opt_disk,1,1,NULL},
{"disk2",trs_opt_disk,1,2,NULL},
{"disk3",trs_opt_disk,1,3,NULL},
{"disk4",trs_opt_disk,1,4,NULL},
{"disk5",trs_opt_disk,1,5,NULL},
{"disk6",trs_opt_disk,1,6,NULL},
{"disk7",trs_opt_disk,1,7,NULL},
{"hard0",trs_opt_hard,1,0,NULL},
{"hard1",trs_opt_hard,1,1,NULL},
{"hard2",trs_opt_hard,1,2,NULL},
{"hard3",trs_opt_hard,1,3,NULL},
{"cassette",trs_opt_cass,1,0,NULL},
{"diskdir",trs_opt_string,1,0,trs_disk_dir},
{"harddir",trs_opt_string,1,0,trs_hard_dir},
{"cassdir",trs_opt_string,1,0,trs_cass_dir},
{"disksetdir",trs_opt_string,1,0,trs_disk_set_dir},
{"statedir",trs_opt_string,1,0,trs_state_dir},
{"printerdir",trs_opt_string,1,0,trs_printer_dir},
{"printercmd",trs_opt_string,1,0,trs_printer_command},
{"keystretch",trs_opt_keystretch,1,0,NULL},
{"borderwidth",trs_opt_borderwidth,1,0,NULL},
{"microlabs",trs_opt_microlabs,0,1,NULL},
{"nomicrolabs",trs_opt_microlabs,0,0,NULL},
{"showled",trs_opt_led,0,1,NULL},
{"hideled",trs_opt_led,0,0,NULL},
{"doubler",trs_opt_doubler,1,0,NULL},
{"sizemap",trs_opt_sizemap,1,0,NULL},
#ifdef __linux      
{"stepmap",trs_opt_stepmap,1,0,NULL},
#endif      
{"truedam",trs_opt_truedam,0,1,NULL},
{"notruedam",trs_opt_truedam,0,0,NULL},
{"samplerate",trs_opt_samplerate,1,0,NULL},
#ifndef ANDROID
{"serial",trs_opt_string,1,0,trs_uart_name},
#endif
{"switches",trs_opt_switches,1,0,NULL},
{"shiftbracket",trs_opt_shiftbracket,0,1,NULL},
{"noshiftbracket",trs_opt_shiftbracket,0,0,NULL},
{"keypadjoy",trs_opt_keypadjoy,0,1,NULL},
{"nokeypadjoy",trs_opt_keypadjoy,0,0,NULL},
{"joysticknum",trs_opt_joysticknum,1,0,NULL},
{"foreground",trs_opt_foreground,1,0,NULL},
{"background",trs_opt_background,1,0,NULL},
{"guiforeground",trs_opt_guiforeground,1,0,NULL},
{"guibackground",trs_opt_guibackground,1,0,NULL},
{"emtsafe",trs_opt_emtsafe,0,1,NULL},
{"noemtsafe",trs_opt_emtsafe,0,0,NULL},
{"turbo",trs_opt_turbo,0,1,NULL},
{"noturbo",trs_opt_turbo,0,0,NULL},
{"turborate",trs_opt_turborate,1,0,NULL},
};

static int num_options = sizeof(options)/sizeof(trs_opt);
  
/* Private routines */
void bitmap_init();
void trs_event_init();
void trs_event();

extern char *program_name;
char *title;

static void stripWhitespace (char *inputStr)
  {
  char *start, *end;
  
  start = inputStr;
  while (*start && (*start == ' '|| *start == '\t' || *start == '\r' || *start == '\n')) 
    start++;
  memmove(inputStr, start, strlen(start) + 1);
  end = inputStr + strlen(inputStr) - 1;
  while (*end && (*end == ' ' || *end == '\t' || *end == '\r' || *end == '\n')) 
    end--;
  *(end + 1) = '\0';
  }
  
int trs_write_config_file(char *filename)
{
  FILE *config_file;
  int i;

  config_file = fopen(filename,"w");
  if (config_file == NULL)
    return -1;

  fprintf(config_file,"scale=%d\n",scale_x);
  if (resize3)
    fprintf(config_file,"resize3\n");
  else
    fprintf(config_file,"noresize3\n");
  if (resize4)
    fprintf(config_file,"resize4\n");
  else
    fprintf(config_file,"noresize4\n");
  switch(trs_model) {
    case 1:
      fprintf(config_file,"model=1\n");
      break;
    case 3:
      fprintf(config_file,"model=3\n");
      break;
    case 4:
      fprintf(config_file,"model=4\n");
      break;
    case 5:
      fprintf(config_file,"model=4P\n");
      break;
    } 
  fprintf(config_file,"charset1=%d\n",trs_charset1);
  fprintf(config_file,"charset3=%d\n",trs_charset3);
  fprintf(config_file,"charset4=%d\n",trs_charset4);
  fprintf(config_file,"romfile=%s\n",romfile);
  fprintf(config_file,"romfile3=%s\n",romfile3);
  fprintf(config_file,"romfile4p=%s\n",romfile4p);
  fprintf(config_file,"diskdir=%s\n",trs_disk_dir);
  fprintf(config_file,"harddir=%s\n",trs_hard_dir);
  fprintf(config_file,"cassdir=%s\n",trs_cass_dir);
  fprintf(config_file,"disksetdir=%s\n",trs_disk_set_dir);
  fprintf(config_file,"statedir=%s\n",trs_state_dir);
  fprintf(config_file,"printerdir=%s\n",trs_printer_dir);
  fprintf(config_file,"printercmd=%s\n",trs_printer_command);
  fprintf(config_file,"keystretch=%d\n",stretch_amount);
  fprintf(config_file,"borderwidth=%d\n",window_border_width);
  if (fullscreen)
    fprintf(config_file,"fullscreen\n");
  else
    fprintf(config_file,"nofullscreen\n");
  if (grafyx_microlabs)
    fprintf(config_file,"microlabs\n");
  else
    fprintf(config_file,"nomicrolabs\n");
  if (trs_show_led)
    fprintf(config_file,"showled\n");
  else
    fprintf(config_file,"hideled\n");
  switch(trs_disk_doubler) {
    case TRSDISK_PERCOM:
      fprintf(config_file,"doubler=percom\n");
      break;
    case TRSDISK_TANDY:
      fprintf(config_file,"doubler=tandy\n");
      break;
    case TRSDISK_BOTH:
      fprintf(config_file,"doubler=both\n");
      break;
    case TRSDISK_NODOUBLER:
      fprintf(config_file,"doubler=nodoubler\n");
      break;
  } 
  fprintf(config_file, "sizemap=%d,%d,%d,%d,%d,%d,%d,%d\n",
          trs_disk_getsize(0),
          trs_disk_getsize(1),
          trs_disk_getsize(2),
          trs_disk_getsize(3),
          trs_disk_getsize(4),
          trs_disk_getsize(5),
          trs_disk_getsize(6),
          trs_disk_getsize(7));
#ifdef __linux          
  fprintf(config_file, "stepmap=%d,%d,%d,%d,%d,%d,%d,%d\n",
          trs_disk_getstep(0),            /* Corrected to trs_disk_getstep vs getsize  */
          trs_disk_getstep(1),            /* Corrected by Larry Kraemer 08-01-2011 */
          trs_disk_getstep(2),
          trs_disk_getstep(3),
          trs_disk_getstep(4),
          trs_disk_getstep(5),
          trs_disk_getstep(6),
          trs_disk_getstep(7));
#endif          
  if (trs_disk_truedam)
    fprintf(config_file,"truedam\n");
  else
    fprintf(config_file,"notruedam\n");
  fprintf(config_file,"samplerate=%d\n",cassette_default_sample_rate);
  fprintf(config_file,"serial=%s\n",trs_uart_name);
  fprintf(config_file,"switches=%d\n",trs_uart_switches);
  if (trs_kb_bracket_state)
    fprintf(config_file,"shiftbracket\n");
  else
    fprintf(config_file,"noshiftbracket\n");
  if (trs_joystick_num == -1)
    fprintf(config_file,"joysticknum=none\n");
  else
    fprintf(config_file,"joysticknum=%d\n",trs_joystick_num);
  if (trs_keypad_joystick)
    fprintf(config_file,"keypadjoy\n");
  else
    fprintf(config_file,"nokeypadjoy\n");
  fprintf(config_file,"foreground=0x%x\n",foreground);
  fprintf(config_file,"background=0x%x\n",background);
  fprintf(config_file,"guiforeground=0x%x\n",gui_foreground);
  fprintf(config_file,"guibackground=0x%x\n",gui_background);
  if (timer_overclock)
    fprintf(config_file,"turbo\n");
  else
    fprintf(config_file,"noturbo\n");
  fprintf(config_file,"turborate=%d\n", timer_overclock_rate);

  for (i=0;i<8;i++) {
    char *diskname;
    
    diskname = trs_disk_getfilename(i); 
    if (diskname[0] != 0)
      fprintf(config_file,"disk%d=%s\n",i,diskname);
  }
  for (i=0;i<4;i++) {
    char *diskname;
    
    diskname = trs_hard_getfilename(i); 
    if (diskname[0] != 0)
      fprintf(config_file,"hard%d=%s\n",i,diskname);
  }
  
  {
    char *cassname;
    
    cassname = trs_cassette_getfilename();
    if (cassname[0] != 0)
      fprintf(config_file,"cassette=%s\n",cassname);
  }

  fclose(config_file);
  return 0;
}

void trs_set_to_defaults(void)
{
  int i;

  for (i=0;i<8;i++)
    trs_disk_remove(i);
  for (i=0;i<4;i++)
    trs_hard_remove(i);
  trs_cassette_remove();
  
  scale_x = 1;
  scale_y = 2;
  resize3 = 1;
  resize4 = 0;
  fullscreen = 0;
  trs_model = 1;
  trs_charset = 3;
  trs_charset1 = 3;
  trs_charset3 = 4;
  trs_charset4 = 8;
  strcpy(romfile,"level2.rom");
  strcpy(romfile3,"model3.rom");
  strcpy(romfile4p,"model4p.rom");
  strcpy(trs_disk_dir,"disks");
  strcpy(trs_hard_dir,"harddisks");
  strcpy(trs_cass_dir,"cassettes");
  strcpy(trs_disk_set_dir,"disksets");
  strcpy(trs_state_dir,"savedstates");
  strcpy(trs_printer_dir,"printer");
  stretch_amount = STRETCH_AMOUNT;
  window_border_width = 2;
  grafyx_set_microlabs(FALSE);
  trs_show_led = TRUE;
  trs_disk_doubler = TRSDISK_BOTH;
  disksizes[0] = 5;            /* Disk Sizes are 5" or 8" for all Eight Default Drives */
  disksizes[1] = 5;            /* Corrected by Larry Kraemer 08-01-2011 */
  disksizes[2] = 5;
  disksizes[3] = 5;
  disksizes[4] = 8;
  disksizes[5] = 8;
  disksizes[6] = 8;
  disksizes[7] = 8;
  disksteps[0] = 1;            /* Disk Steps are 1 for Single Step, 2 for Double Step for all Eight Default Drives */
  disksteps[1] = 1;            /* Corrected by Larry Kraemer 08-01-2011 */
  disksteps[2] = 1;
  disksteps[3] = 1;
  disksteps[4] = 2;
  disksteps[5] = 2;
  disksteps[6] = 2;
  disksteps[7] = 2;
  trs_disk_truedam = 0;
  cassette_default_sample_rate = DEFAULT_SAMPLE_RATE;
  trs_uart_switches = 0x7 | TRS_UART_NOPAR | TRS_UART_WORD8;
  trs_kb_bracket(FALSE);
  trs_keypad_joystick = TRUE;
  trs_joystick_num = 0;
  foreground = WHITE;
  background = BLACK;
  gui_foreground = WHITE;
  gui_background = GREEN;
#ifdef __linux
  strcpy(trs_printer_command,"lpr %s");
#endif
#ifdef _WIN32
  strcpy(trs_printer_command,"notepad %s");
#endif
#ifdef MACOSX
  strcpy(trs_printer_command,"open %s");
#endif
  trs_emtsafe = 0;
}

static void trs_opt_scale(char *arg, int intarg, char *stringarg)
{
  scale_x = atoi(arg);
  if (scale_x <= 0) scale_x = 1;
  if (scale_x > MAX_SCALE) scale_x = MAX_SCALE;
  scale_y = scale_x * 2;
}
static void trs_opt_scale2(char *arg, int intarg, char *stringarg)
{
  scale_x = intarg;
  scale_y = scale_x * 2;
}
static void trs_opt_resize3(char *arg, int intarg, char *stringarg)
{
  resize3 = intarg;
}
static void trs_opt_resize4(char *arg, int intarg, char *stringarg)
{
  resize4 = intarg;
}
static void trs_opt_fullscreen(char *arg, int intarg, char *stringarg)
{
  fullscreen = intarg;
}
static void trs_opt_model(char *arg, int intarg, char *stringarg)
{
  if (strcmp(arg, "1") == 0 ||
      strcasecmp(arg, "I") == 0) {
    trs_model = 1;
  } else if (strcmp(arg, "3") == 0 ||
             strcasecmp(arg, "III") == 0) {
    trs_model = 3;
  } else if (strcmp(arg, "4") == 0 ||
             strcasecmp(arg, "IV") == 0) {
    trs_model = 4;
  } else if (strcasecmp(arg, "4P") == 0 ||
             strcasecmp(arg, "IVp") == 0) {
    trs_model = 5;
  } else 
    trs_model = 1;
}
static void trs_opt_model2(char *arg, int intarg, char *stringarg)
{
	trs_model = intarg;
}
static void trs_opt_charset1(char *arg, int intarg, char *stringarg)
{
  if (isdigit(*arg)) {
    trs_charset1 = atoi(arg);
    if (trs_charset1 < 0 || (trs_charset1 > 3  && trs_charset != 10))
      trs_charset1 = 3;
  } else if (arg[0] == 'e'/*early*/) {
     trs_charset1 = 0;
  } else if (arg[0] == 's'/*stock*/) {
     trs_charset1 = 1;
  } else if (arg[0] == 'l'/*lcmod*/) {
     trs_charset1 = 2;
  } else if (arg[0] == 'w'/*wider*/) {
     trs_charset1 = 3;
  } else if (arg[0] == 'g'/*genie or german*/) {
     trs_charset1 = 10;
  } else {
     trs_charset1 = 3;
  }
}
static void trs_opt_charset3(char *arg, int intarg, char *stringarg)
{
  if (isdigit(*arg)) {
    trs_charset3 = atoi(arg);
    if (trs_charset3 < 4 || trs_charset3 > 6)
      trs_charset3 = 4;
  } else if (arg[0] == 'k'/*katakana*/) {
    trs_charset3 = 4;
  } else if (arg[0] == 'i'/*international*/) {
    trs_charset3 = 5;
  } else if (arg[0] == 'b'/*bold*/) {
    trs_charset3 = 6;
  } else {
    trs_charset3 = 5;
  }
}
static void trs_opt_charset4(char *arg, int intarg, char *stringarg)
{
  if (isdigit(*arg)) {
    trs_charset4 = atoi(arg);
    if (trs_charset4 < 7 || trs_charset4 > 9)
      trs_charset4 = 8;
  } else if (arg[0] == 'k'/*katakana*/) {
    trs_charset4 = 7;
  } else if (arg[0] == 'i'/*international*/) {
    trs_charset4 = 8;
  } else if (arg[0] == 'b'/*bold*/) {
    trs_charset4 = 9;
  } else {
    trs_charset4 = 8;
  }
}
static void trs_opt_string(char *arg, int intarg, char *stringarg)
{
	strcpy(stringarg, arg);
}
static void trs_opt_disk(char *arg, int intarg, char *stringarg)
{
	trs_disk_insert(intarg, arg);
}
static void trs_opt_hard(char *arg, int intarg, char *stringarg)
{
	trs_hard_attach(intarg, arg);
}
static void trs_opt_cass(char *arg, int intarg, char *stringarg)
{
	trs_cassette_insert(arg);
}
static void trs_opt_keystretch(char *arg, int intarg, char *stringarg)
{
  stretch_amount = strtol(arg, NULL, 0);
}
static void trs_opt_borderwidth(char *arg, int intarg, char *stringarg)
{
  window_border_width = strtol(arg, NULL, 0);
}
static void trs_opt_microlabs(char *arg, int intarg, char *stringarg)
{
  grafyx_set_microlabs(intarg);
}
static void trs_opt_led(char *arg, int intarg, char *stringarg)
{
  trs_show_led = intarg;
}
static void trs_opt_doubler(char *arg, int intarg, char *stringarg)
{
  switch (*arg) {
    case 'p':
    case 'P':
      trs_disk_doubler = TRSDISK_PERCOM;
      break;
    case 'r':
    case 'R':
    case 't':
    case 'T':
      trs_disk_doubler = TRSDISK_TANDY;
      break;
    case 'b':
    case 'B':
    default:
      trs_disk_doubler = TRSDISK_BOTH;
      break;
    case 'n':
    case 'N':
      trs_disk_doubler = TRSDISK_NODOUBLER;
      break;
    }
}
static void trs_opt_sizemap(char *arg, int intarg, char *stringarg)
{
  sscanf(arg, "%d,%d,%d,%d,%d,%d,%d,%d",
         &disksizes[0], &disksizes[1], &disksizes[2], &disksizes[3],
         &disksizes[4], &disksizes[5], &disksizes[6], &disksizes[7]);
#ifdef MACOSX		 
  disksizesLoaded = TRUE;
#endif  
}
#ifdef __linux
static void trs_opt_stepmap(char *arg, int intarg, char *stringarg)
{
  sscanf(arg, "%d,%d,%d,%d,%d,%d,%d,%d",
         &disksteps[0], &disksteps[1], &disksteps[2], &disksteps[3],
         &disksteps[4], &disksteps[5], &disksteps[6], &disksteps[7]);
}
#endif
static void trs_opt_truedam(char *arg, int intarg, char *stringarg)
{
  trs_disk_truedam = intarg;
}
static void trs_opt_samplerate(char *arg, int intarg, char *stringarg)
{
  cassette_default_sample_rate = strtol(arg, NULL, 0);
}
static void trs_opt_switches(char *arg, int intarg, char *stringarg)
{
  trs_uart_switches = strtol(arg, NULL, 0);
}
static void trs_opt_shiftbracket(char *arg, int intarg, char *stringarg)
{
  trs_kb_bracket(intarg);
}
static void trs_opt_keypadjoy(char *arg, int intarg, char *stringarg)
{
  trs_keypad_joystick = intarg;
}
static void trs_opt_joysticknum(char *arg, int intarg, char *stringarg)
{
  if (strcasecmp(arg,"none") == 0)
    trs_joystick_num = -1;
  else
    trs_joystick_num = atoi(arg);
}
static void trs_opt_foreground(char *arg, int intarg, char *stringarg)
{
  foreground = strtol(arg, NULL, 16);
}
static void trs_opt_background(char *arg, int intarg, char *stringarg)
{
  background = strtol(arg, NULL, 16);
}
static void trs_opt_guiforeground(char *arg, int intarg, char *stringarg)
{
  gui_foreground = strtol(arg, NULL, 16);
}
static void trs_opt_guibackground(char *arg, int intarg, char *stringarg)
{
  gui_background = strtol(arg, NULL, 16);
}
static void trs_opt_emtsafe(char *arg, int intarg, char *stringarg)
{
  trs_emtsafe = intarg;
}
static void trs_opt_turbo(char *arg, int intarg, char *stringarg)
{
  timer_overclock = intarg;
}
static void trs_opt_turborate(char *arg, int intarg, char *stringarg)
{
  timer_overclock_rate = atoi(arg);
  if (timer_overclock_rate <= 0)
	  timer_overclock_rate = 1;
}

int trs_load_config_file(char *alternate_file)
{
  char line[FILENAME_MAX + 512];
  char *arg;
  FILE *config_file;
  int i;

#ifdef MACOSX
  if (alternate_file == NULL) {
	return(0);
	}
#endif

  if (alternate_file) 
    strcpy(trs_config_file,alternate_file);
  else
#ifdef __linux
    {
      char *home = getenv("HOME");
      strcpy(trs_config_file,home);
      strcat(trs_config_file,"/sdltrs.t8c");
    }
#else
    strcpy(trs_config_file,"./sdltrs.t8c");
#endif
  
  trs_set_to_defaults();
  
  config_file = fopen(trs_config_file,"r");
  if (config_file == NULL) {
    trs_write_config_file(trs_config_file);
    return(0);
  }
  
  while (fgets(line, sizeof(line), config_file)) {
	arg = strchr(line, '=');
	if (arg != NULL) {
	  *arg++ = '\0';
      stripWhitespace(arg);
    }

    stripWhitespace(line);
	
	for (i=0;i<num_options;i++) {
	  if (strcasecmp(line, options[i].name) == 0) {
	    if (options[i].hasArg) {
		  if (arg) {
			(*options[i].handler)(arg,options[i].intArg,options[i].strArg);  
		  }
		} else
			(*options[i].handler)(NULL,options[i].intArg,options[i].strArg); 
		break;
      }
    }
  }
  
  trs_disk_setsizes();
  trs_disk_setsteps();
  return 1;
}

int trs_parse_command_line(int argc, char **argv, int *debug)
{
  int i,j;
  char alt_config_file[FILENAME_MAX];

  title = program_name; /* default */

  /* Check for config or state files on the command line */
  alt_config_file[0] = 0;
  init_state_file[0] = 0;
  for (i = 1; i < argc; i++) {
	if (argv[i][0] == '-') {
	  for (j=0;j<num_options;j++) {
	    if (strcasecmp(&argv[i][1], options[j].name) == 0) {
		  if (options[j].hasArg)
	        i++;
		  break;
		}
	  }
	}
	else if (strlen(argv[i]) < 4) {
	}
	else if (strcmp(&argv[i][strlen(argv[i])-4],".t8c") == 0) {
        strcpy(alt_config_file,argv[i]);
	}
	else if (strcmp(&argv[i][strlen(argv[i])-4],".t8s") == 0) {
        strcpy(init_state_file,argv[i]);
	}
  }

#ifdef MACOSX
  trs_get_mac_prefs();
  disksizesLoaded = FALSE;
#endif  

  if (alt_config_file[0] == 0)  
    trs_load_config_file(NULL);
  else
    trs_load_config_file(alt_config_file);

  for (i = 1; i < argc; i++) {
	int argAvail = ((i + 1) < argc);		/* is argument available? */
	int argMissing = FALSE;

	for (j=0;j<num_options;j++) {
      if (argv[i][0] == '-') {
        if (strcasecmp(&argv[i][1], options[j].name) == 0) {
	      if (options[j].hasArg) {
		    if (argAvail) {
		      (*options[j].handler)(argv[++i],options[j].intArg,options[j].strArg);  
		    } else {
			  argMissing = TRUE;
  		    }
	  	  } else
		      (*options[j].handler)(NULL,options[j].intArg,options[j].strArg); 
		  break;
        }
      }
	}
  }

#ifdef MACOSX
  if (disksizesLoaded)
#endif
  trs_disk_setsizes();
  trs_disk_setsteps();
  
  return 1;
}

void trs_disk_setsizes(void)
{
  int j;
  
  for (j=0; j<=7; j++) {
    if (disksizes[j] == 5 || disksizes[j] == 8) {
            trs_disk_setsize(j, disksizes[j]);
    }
  }
}

void trs_disk_setsteps(void)
{            /* Disk Steps are 1 for Single Step or 2 for Double Step for all Eight Default Drives */
  int j;
  
  for (j=0; j<=7; j++) {
    if (disksteps[j] == 1 || disksteps[j] == 2) {
            trs_disk_setstep(j, disksteps[j]);
    }
  }
}

void trs_flip_fullscreen(void)
{
  static int window_scale_x = 1;
  static int window_scale_y = 2;
  
  copyStatus = COPY_IDLE;
  fullscreen = !fullscreen;
  if (fullscreen) {
#ifdef MACOSX
    TrsOriginSave();
#endif
    window_scale_x = scale_x;
    window_scale_y = scale_y;
    if (scale_x != 1) {
      scale_x = 1;
      scale_y = 2;
      trs_screen_init();
      grafyx_redraw();
      trs_screen_refresh();
    }
    else {
      screen = SDL_SetVideoMode(OrigWidth, OrigHeight, 0, 
                                SDL_ANYFORMAT | SDL_FULLSCREEN);
      SDL_ShowCursor(SDL_DISABLE);
      }
    }
  else {
#ifdef MACOSX	  
	TrsWindowCreate(OrigWidth, OrigHeight);
    TrsOriginRestore();
	if (1) {
#else	  
    if (window_scale_x != 1) {
#endif		
      scale_x = window_scale_x;
      scale_y = window_scale_y;
      trs_screen_init();
      grafyx_redraw();
      trs_screen_refresh();
    }
    else {
      screen = SDL_SetVideoMode(OrigWidth, OrigHeight, 0, 
                                SDL_ANYFORMAT);
      SDL_ShowCursor(SDL_ENABLE);
	 }
#ifdef MACOSX	  
	 TrsOriginRestore();
#endif
  }
  if (trs_show_led) {
    trs_disk_led(-1,0);
    trs_hard_led(-1,0);
  }
}

void trs_rom_init(void)
{
  switch(trs_model) {
    case 1:
      if (romfile[0] != 0) {
        if (trs_load_rom(romfile) == 0) 
          break;
      } 
      if (trs_rom1_size > 0) 
        trs_load_compiled_rom(trs_rom1_size, trs_rom1);
      break;
    case 3:
    case 4:
      if (romfile3[0] != 0){
        if (trs_load_rom(romfile3) == 0) 
          break;
      } 
      if (trs_rom3_size > 0) 
        trs_load_compiled_rom(trs_rom3_size, trs_rom3);
      break;
    case 5:
      if (romfile4p[0] != 0){
        if (trs_load_rom(romfile4p) == 0) 
          break;
      } 
      if (trs_rom4p_size > 0) 
        trs_load_compiled_rom(trs_rom4p_size, trs_rom4p);
      break;
  }
}

void trs_screen_var_reset()
{
  text80x24 = 0;
  screen640x240 = 0;
  screen_chars = 1024;
  row_chars = 64;
  col_chars = 16;
}

void trs_screen_caption(int turbo)
{
    char title[80];

    if (trs_model == 5) {
        sprintf(title,"TRS80 Model 4p %s", turbo ? "Turbo" : "");
    }
    else {
      sprintf(title,"TRS80 Model %d %s",trs_model, turbo ? "Turbo" : "");
    }
    SDL_WM_SetCaption(title,NULL);
}

void trs_screen_init()
{
  SDL_Color colors[2];

  copyStatus = COPY_IDLE;
  if (trs_model == 1)
    trs_charset = trs_charset1;
  else if (trs_model == 3)
    trs_charset = trs_charset3;
  else
    trs_charset = trs_charset4;

  if (trs_model == 4)
	resize = resize4;
  else
	resize = resize3;


  if (trs_model == 1) {
    if (trs_charset < 3)
      cur_char_width = 6 * scale_x;
    else
      cur_char_width = 8 * scale_x;
    cur_char_height = TRS_CHAR_HEIGHT * scale_y;
  } else {
    cur_char_width = TRS_CHAR_WIDTH * scale_x;
    cur_char_height = TRS_CHAR_HEIGHT4 * scale_y;
  }

  imageSize.width = 8*G_XSIZE*scale_x;
  imageSize.height = 2*G_YSIZE * scale_y/2;
  imageSize.bytes_per_line = G_XSIZE * scale_x;

  if (fullscreen)
    border_width = 0;
  else
    border_width = window_border_width;

  if (trs_show_led)
    led_width = 8;
  else
    led_width = 0;

  clear_key_queue();		/* init the key queue */

  if (trs_model >= 3  && !resize) {
    OrigWidth = cur_char_width * 80 + 2 * border_width;
    left_margin = cur_char_width * (80 - row_chars)/2 + border_width;
    OrigHeight = TRS_CHAR_HEIGHT4 * scale_y * 24 + 2 * border_width + led_width;
    top_margin = (TRS_CHAR_HEIGHT4 * scale_y * 24 -
                 cur_char_height * col_chars)/2 + border_width;
  } else {
    OrigWidth = cur_char_width * row_chars + 2 * border_width;
    left_margin = border_width;
    OrigHeight = cur_char_height * col_chars + 2 * border_width + led_width;
    top_margin = border_width;
  }

#ifndef ANDROID
  if (fullscreen) {
     screen = SDL_SetVideoMode(OrigWidth, OrigHeight, 0, 
                               SDL_ANYFORMAT | SDL_FULLSCREEN);
     SDL_ShowCursor(SDL_DISABLE);
	}
  else {
#ifdef MACOSX	  
     TrsWindowResize(OrigWidth, OrigHeight);
     TrsWindowDisplay();
#endif	  
     screen = SDL_SetVideoMode(OrigWidth, OrigHeight, 0, 
                               SDL_ANYFORMAT);
     SDL_ShowCursor(SDL_ENABLE);
    }
  
  trs_screen_caption(trs_timer_is_turbo());

  light_red = SDL_MapRGB(screen->format, 0x40,0x00,0x00);
  bright_red = SDL_MapRGB(screen->format, 0xff,0x00,0x00);

#ifdef MACOSX
  if (!fullscreen) {
    centerAppWindow();
    SetControlManagerModel(trs_model, grafyx_get_microlabs());
    SetControlManagerTurboMode(trs_timer_is_turbo());
    UpdateMediaManagerInfo();
    if (mediaStatusWindowOpen)
        MediaManagerStatusWindowShow();
    }
	TrsMakeKeyWindow();
#endif  

  if (image)
    SDL_FreeSurface(image);
  memset(grafyx,0,(2*G_YSIZE*MAX_SCALE) * (G_XSIZE*MAX_SCALE));
  image = SDL_CreateRGBSurfaceFrom(grafyx, imageSize.width, imageSize.height, 1, 
                                   imageSize.bytes_per_line, 1, 1, 1, 0);
  colors[0].r = (background >> 16) & 0xFF;
  colors[0].g = (background >> 8) & 0xFF;
  colors[0].b = (background) & 0xFF;
  colors[1].r = (foreground >> 16) & 0xFF;
  colors[1].g = (foreground >> 8) & 0xFF;
  colors[1].b = (foreground) & 0xFF;
  SDL_SetPalette(image,SDL_LOGPAL, colors,0,2);
  
  TrsBlitMap(image->format->palette, screen->format);

  bitmap_init(foreground, background);

  trs_disk_led(-1,0);
  trs_hard_led(-1,0);
#endif
}

Uint32 last_key[256];

void addToDrawList(SDL_Rect *rect)
{
	if (drawnRectCount < MAX_RECTS) {
		drawnRects[drawnRectCount++] = *rect;
		}
}

void DrawSelectionRectangle(int orig_x, int orig_y, int copy_x, int copy_y)
{
	int i,y,scale;
	
	if (copy_x < orig_x) {
		int swap_x;
		
		swap_x = copy_x;
		copy_x = orig_x;
		orig_x = swap_x;
	}
	if (copy_y < orig_y) {
		int swap_y;
		
		swap_y = copy_y;
		copy_y = orig_y;
		orig_y = swap_y;
	}
	
	scale = scale_x	;
	
	if (screen->format->BitsPerPixel == 8) {
		register int pitch;
		register Uint8 *start8;
		
		SDL_LockSurface(screen);
		
		pitch = screen->pitch;
		
		for (y=orig_y;y < orig_y+scale;y++) {
			start8 = (Uint8 *) screen->pixels + 
			(y * pitch) + orig_x;
			for (i=0;i<(copy_x-orig_x+scale);i++,start8++)
				*start8 ^= 0xFF;
		}
		if (copy_y > orig_y) {
			for (y=copy_y;y < copy_y+scale;y++) {
				start8 = (Uint8 *) screen->pixels + 
				(y * pitch) + orig_x;
				for (i=0;i<(copy_x-orig_x+scale);i++,start8++)
					*start8 ^= 0xFF;
			}
		}
		for (y=orig_y+scale;y < copy_y;y++) {
			start8 = (Uint8 *) screen->pixels + 
			(y * pitch) + orig_x;
			for (i=0;i<scale;i++)
				*start8++ ^= 0xFF;
		}
		if (copy_x > orig_x) {
			for (y=orig_y+scale;y < copy_y;y++) {
				start8 = (Uint8 *) screen->pixels + 
				(y * pitch) + copy_x;
				for (i=0;i<scale;i++)
					*start8++ ^= 0xFF;
			}
		}
		SDL_UnlockSurface(screen);		
	}
	else if (screen->format->BitsPerPixel == 16) {
		register int pitch2;
		register Uint16 *start16;
		
		SDL_LockSurface(screen);
		
		pitch2 = screen->pitch / 2;
		
		for (y=orig_y;y < orig_y+scale;y++) {
			start16 = (Uint16 *) screen->pixels + 
			(y * pitch2) + orig_x;
			for (i=0;i<(copy_x-orig_x+scale);i++,start16++)
				*start16 ^= 0xFFFF;
		}
		if (copy_y > orig_y) {
			for (y=copy_y;y < copy_y+scale;y++) {
				start16 = (Uint16 *) screen->pixels + 
				(y * pitch2) + orig_x;
				for (i=0;i<(copy_x-orig_x+scale);i++,start16++)
					*start16 ^= 0xFFFF;
			}
		}
		for (y=orig_y+scale;y < copy_y;y++) {
			start16 = (Uint16 *) screen->pixels + 
			(y * pitch2) + orig_x;
			for (i=0;i<scale;i++)
				*start16++ ^= 0xFFFF;
		}
		if (copy_x > orig_x) {
			for (y=orig_y+scale;y < copy_y;y++) {
				start16 = (Uint16 *) screen->pixels + 
				(y * pitch2) + copy_x;
				for (i=0;i<scale;i++)
					*start16++ ^= 0xFFFF;
			}
		}
		SDL_UnlockSurface(screen);		
	}
	else if (screen->format->BitsPerPixel == 32) {
		register int pitch4;
		register Uint32 *start32;
		
		SDL_LockSurface(screen);
		
		pitch4 = screen->pitch / 4;
		
		for (y=orig_y;y<orig_y+scale;y++) {
			start32 = (Uint32 *) screen->pixels + 
			(y * pitch4) + orig_x;
			for (i=0;i<(copy_x-orig_x+scale);i++,start32++)
				*start32 ^= 0xFFFFFFFF;
		}
		if (copy_y > orig_y) {
			for (y=copy_y;y<copy_y+scale;y++) {
				start32 = (Uint32 *) screen->pixels + 
				(y * pitch4) + orig_x;
				for (i=0;i<(copy_x-orig_x+scale);i++,start32++)
					*start32 ^= 0xFFFFFFFF;
			}
		}
		for (y=orig_y+scale;y < copy_y;y++) {
			start32 = (Uint32 *) screen->pixels + 
			(y * pitch4) + orig_x;
			for (i=0;i<scale;i++)
				*start32++ ^= 0xFFFFFFFF;
		}
		if (copy_x > orig_x) {
			for (y=orig_y+scale;y < copy_y;y++) {
				start32 = (Uint32 *) screen->pixels + 
				(y * pitch4) + copy_x;
				for (i=0;i<scale;i++)
					*start32++ ^= 0xFFFFFFFF;
			}
		}
		SDL_UnlockSurface(screen);		
	}
}

void ProcessCopySelection(int selectAll)
{
	static int orig_x = 0;
	static int orig_y = 0;
	static int end_x = 0;
	static int end_y = 0;
	static int copy_x = 0;
	static int copy_y = 0;
	static Uint8 mouse = 0;
	
	if (selectAll) {
		if (copyStatus == COPY_STARTED)
			return;
		if (copyStatus == COPY_DEFINED || copyStatus == COPY_CLEAR) {
			DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
		}
		orig_x = 0;
		orig_y = 0;
		copy_x = end_x = screen->w - scale_x;
		copy_y = end_y = screen->h - scale_x;
		DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
		drawnRectCount = MAX_RECTS;
		copyStatus = COPY_DEFINED;
		selectionStartX = orig_x - left_margin; 
		selectionStartY = orig_y - top_margin;
		selectionEndX = copy_x - left_margin;
		selectionEndY = copy_y - top_margin;
	} else {
#ifdef MACOSX		
		if (TrsIsKeyWindow()) {
#endif			
			mouse = SDL_GetMouseState(&copy_x, &copy_y);
#ifdef MACOSX			
			if ((copyStatus == COPY_IDLE) && !TrsWindowMouseInside())
				return;
#endif			
			if ((copyStatus == COPY_IDLE) &&
				(mouse & SDL_BUTTON(1) == 0)) {
				return;		
			}
#ifdef MACOSX			
		}
		else {
			mouse = 0;
			copyStatus = COPY_IDLE;
			return;		
		}
#endif		
	}
	
	switch(copyStatus) {
		case COPY_IDLE:
			if (selectAll) {
				copyStatus = COPY_DEFINED;
				orig_x = 0;
				orig_y = 0;
                selectionStartX = orig_x - left_margin; 
                selectionStartY = orig_y - top_margin;
                selectionEndX = copy_x - left_margin;
                selectionEndY = copy_y - top_margin;
				DrawSelectionRectangle(orig_x, orig_y, copy_x, copy_y);
				drawnRectCount = MAX_RECTS;
			}
			else if (mouse & SDL_BUTTON(1) ) {
				copyStatus = COPY_STARTED;
				orig_x = copy_x;
				orig_y = copy_y;
				DrawSelectionRectangle(orig_x, orig_y, copy_x, copy_y);
				drawnRectCount = MAX_RECTS;
			}
			end_x = copy_x;
			end_y = copy_y;
			break;
		case COPY_STARTED:
			DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
			if (mouse & SDL_BUTTON(1))
				DrawSelectionRectangle(orig_x, orig_y, copy_x, copy_y);
			drawnRectCount = MAX_RECTS;
			end_x = copy_x;
			end_y = copy_y;
			if ((mouse & SDL_BUTTON(1)) == 0) {
				if (orig_x == copy_x && orig_y == copy_y) {
					copyStatus = COPY_IDLE;
				} else {
					DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
					copyStatus = COPY_DEFINED;
                    selectionStartX = orig_x - left_margin; 
                    selectionStartY = orig_y - top_margin;
                    selectionEndX = copy_x - left_margin;
                    selectionEndY = copy_y - top_margin;
				}
			}
			break;
		case COPY_DEFINED:
			if (mouse & SDL_BUTTON(1)) {
				copyStatus = COPY_STARTED;
				DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
				orig_x = end_x = copy_x;
				orig_y = end_y = copy_y;
				DrawSelectionRectangle(orig_x, orig_y, copy_x, copy_y);
				drawnRectCount = MAX_RECTS;
			}
			break;
		case COPY_CLEAR:
			DrawSelectionRectangle(orig_x, orig_y, end_x, end_y);
			drawnRectCount = MAX_RECTS;
			copyStatus = COPY_IDLE;
	}
}

void trs_end_copy() 
{
	copyStatus = COPY_CLEAR;
}

void trs_paste_started()
{
  // engage turbo
  trs_timer_switch_turbo();
  paste_state = PASTE_GETNEXT;
}

void trs_paste_ended()
{
  // stop turbo
  trs_timer_switch_turbo();
  paste_state = PASTE_IDLE;
}

void trs_select_all()
{
	requestSelectAll = TRUE;
}

/*
 * Flush output to X server
 */
inline void trs_x_flush()
{
  if (!trs_emu_mouse) 
      {
      ProcessCopySelection(requestSelectAll);
      }
  requestSelectAll = FALSE;
  if (drawnRectCount == 0)
    return;
  if (drawnRectCount == MAX_RECTS)
	SDL_UpdateRect(screen,0,0,0,0);
  else
    SDL_UpdateRects(screen,drawnRectCount,drawnRects);
  drawnRectCount = 0;
}

char *trs_get_copy_data()
{
  static char copy_data[2500];
  char data;
  char *curr_data = copy_data;
  char *screen_ptr;
  int col, line;
  int start_col, end_col, start_line, end_line;

  if (grafyx_enable && !grafyx_overlay) {
      copy_data[0] = 0;
      return(copy_data);
  }

  if (selectionStartX < 0) 
    selectionStartX = 0;
  if (selectionStartY < 0) 
    selectionStartY = 0;

  if (selectionStartX % cur_char_width == 0)
    start_col = selectionStartX / cur_char_width;
  else
    start_col = selectionStartX / cur_char_width + 1;

  if (selectionEndX % cur_char_width == cur_char_width - 1)
    end_col = selectionEndX / cur_char_width;
  else
    end_col = selectionEndX / cur_char_width - 1;

  if (selectionStartY % cur_char_height == 0)
    start_line = selectionStartY / cur_char_height;
  else
    start_line = selectionStartY / cur_char_height + 1;

  if (selectionEndY % cur_char_height >= cur_char_height / 2)
    end_line = selectionEndY / cur_char_height;
  else
    end_line = selectionEndY / cur_char_height - 1;

  if (end_col >= row_chars) 
      end_col = row_chars - 1;
  if (end_line >= col_chars) 
      end_line = col_chars - 1;

  for (line = start_line; line <= end_line; line++) {
    screen_ptr = (char*) &trs_screen[line * row_chars + start_col];
    for (col = start_col; col <= end_col; col++, screen_ptr++) {
      data = *screen_ptr;
      if (data < 0x20) 
        data += 0x40;
      if (data >= 0x20 && data <= 0x7e) 
        *curr_data++ = data;
      else
        *curr_data++ = ' ';
      }
    if (line != end_line) {
#ifdef _WIN32
      *curr_data++ = 0xd;
#endif
      *curr_data++ = 0xa;
    }
  }
  *curr_data = 0;
  return copy_data;
}

/*
 * Get and process SDL event(s).
 *   If wait is true, process one event, blocking until one is available.
 *   If wait is false, process as many events as are available, returning
 *     when none are left.
 * Handle interrupt-driven uart input here too.
 */ 
void trs_get_event(int wait)
{
  SDL_Event event;
  SDL_keysym keysym;
  Uint32 keyup;
  int ret;

  if (trs_model > 1) {
    (void)trs_uart_check_avail();
  }

  trs_x_flush();

  do {
    if (paste_state != PASTE_IDLE) {
		static unsigned short paste_key_uni;
		
  	    if (SDL_PollEvent(&event)) {
			if (event.type == SDL_KEYDOWN) {
				if (paste_state == PASTE_KEYUP) {
					trs_xlate_keysym(0x10000 | paste_key_uni);
				}
				trs_paste_ended();
				return;
			}
		}

      if (paste_state == PASTE_GETNEXT) {
            if (!trs_waiting_for_key())
              return;
			if (!PasteManagerGetChar(&paste_key_uni))
				paste_lastkey = TRUE;
			else
				paste_lastkey = FALSE;
			trs_xlate_keysym(paste_key_uni);
			paste_state = PASTE_KEYDOWN;
			return;
		} else	if (paste_state == PASTE_KEYDOWN) {
			trs_xlate_keysym(0x10000 | paste_key_uni);
			paste_state = PASTE_KEYUP;
			return;
		} else if (paste_state == PASTE_KEYUP) {
			if (paste_lastkey)
				trs_paste_ended();
			else
				paste_state = PASTE_GETNEXT;
		}
    }
		  
    if (wait) {
      SDL_WaitEvent(&event);
    } else {
      if (!SDL_PollEvent(&event)) return;
    }
    switch(event.type) {
#ifndef ANDROID
    case SDL_QUIT:
     trs_exit();
     break;
    case SDL_ACTIVEEVENT:
      if (event.active.state & SDL_APPACTIVE) {
        if (event.active.gain) {
#if XDEBUG
          debug("Active\n");
#endif
          trs_screen_refresh();
          }
      }
      break;
#endif

    case SDL_KEYDOWN:
      keysym  = event.key.keysym;
#ifndef ANDROID
#if XDEBUG
        debug("KeyDown: mod 0x%x, scancode 0x%x keycode 0x%x, unicode 0x%x\n",
	        keysym.mod, keysym.scancode, keysym.sym, keysym.unicode);
#endif
#ifdef MACOSX
	  if (keysym.mod & MENU_MOD == 0) {
#else
	  if (keysym.mod & KMOD_CTRL == 0) {
#endif
	    if (copyStatus != COPY_IDLE)
		  copyStatus = COPY_CLEAR;
	  }
#ifdef MACOSX
	  else if (keysym.sym != SDLK_c && 
		  keysym.sym != SDLK_LMETA && 
		  keysym.sym != SDLK_RMETA) {
#else
	  else if (keysym.sym != SDLK_c &&
		  keysym.sym != SDLK_LCTRL &&
		  keysym.sym != SDLK_RCTRL) {
#endif
	    if (copyStatus != COPY_IDLE)
		  copyStatus = COPY_CLEAR;
	  }
		  
      switch (keysym.sym) {
        /* Trap some function keys here */
      case SDLK_F10:
        if (keysym.mod & KMOD_SHIFT) 
		  {
          trs_reset(1);
		  trs_disk_led(-1,0);
		  trs_hard_led(-1,0);
		  }
        else
          trs_reset(0);
        keysym.unicode = 0;
        keysym.sym = 0;
	    break;
      case SDLK_F11:
        trs_screen_caption(trs_timer_switch_turbo());
        keysym.unicode = 0;
        keysym.sym = 0;
        break;
      case SDLK_F9:
        if (!fullscreen)
          trs_debug();
        keysym.unicode = 0;
        keysym.sym = 0;
        break;
      case SDLK_F8:
        trs_exit();
        keysym.unicode = 0;
        keysym.sym = 0;
        break;
      case SDLK_F7:
#ifdef MACOSX
        if (fullscreen) 
#endif		
	    {
        SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
        trs_pause_audio(1);
        trs_gui();
        trs_pause_audio(0);
        SDL_EnableKeyRepeat(0,0);
        trs_screen_refresh();
        trs_x_flush();
        keysym.unicode = 0;
        keysym.sym = 0;
		}
#ifdef MACOSX
        if (fullscreen) {
          SetControlManagerModel(trs_model, grafyx_get_microlabs());
          SetControlManagerTurboMode(trs_timer_is_turbo());
          UpdateMediaManagerInfo();
        }
#endif  
        break;
      default:
        break;
      }

#if !defined(MACOSX)
      if (keysym.mod & KMOD_CTRL) {
        char *string;

        switch (keysym.sym) {
        case SDLK_c:
          string = trs_get_copy_data();
          PasteManagerStartCopy(string);
          keysym.unicode = 0;
          keysym.sym = 0;
          break;
        case SDLK_v:
          PasteManagerStartPaste();
          keysym.unicode = 0;
          keysym.sym = 0;
          break;
        case SDLK_a:
          requestSelectAll = TRUE;
          keysym.unicode = 0;
          keysym.sym = 0;
          break;
        default:
          break;
        }
      }
#endif
      
      /* Trap the menu keys here */
      if (keysym.mod & MENU_MOD) {
        switch (keysym.sym) {
#ifdef MACOSX        
        case SDLK_q:
          trs_exit();
          break;
        case SDLK_COMMA:
          trs_run_mac_prefs();
          trs_screen_refresh();
          trs_x_flush();
          break;
#if 0				
        case SDLK_a:
          ControlManagerAboutApp();
          break;
#endif				
        case SDLK_h: 
          ControlManagerHideApp();
          break;
        case SDLK_m:
          ControlManagerMiniturize();
          break;
        case SDLK_SLASH:
          ControlManagerShowHelp();
          break;
#endif          
#ifdef _WIN32        
        case SDLK_F4:
          trs_exit();
          break;
#endif          
        case SDLK_RETURN:
          trs_flip_fullscreen();
          trs_screen_refresh();
          break;
        case SDLK_d:
          if (keysym.mod & KMOD_SHIFT) {
#ifdef MACOSX
            if (!fullscreen) {
              MediaManagerRunHardManagement();
            } else
#endif
            {
              SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
              trs_pause_audio(1);
              trs_gui_hard_management();
              trs_pause_audio(0);
              SDL_EnableKeyRepeat(0,0);
              trs_screen_refresh();
              trs_x_flush();
            }
          } else {
#ifdef MACOSX
            if (!fullscreen) {
              MediaManagerRunDiskManagement();
            } else
#endif
            {
              SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
              trs_pause_audio(1);
              trs_gui_disk_management();
              trs_pause_audio(0);
              SDL_EnableKeyRepeat(0,0);
              trs_screen_refresh();
              trs_x_flush();
            }
          }
          break;
        case SDLK_t:
#ifdef MACOSX
          if (!fullscreen) {
            MediaManagerRunCassManagement();
          } else
#endif
          {
            SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
            trs_pause_audio(1);
            trs_gui_cassette_management();
            trs_pause_audio(0);
            SDL_EnableKeyRepeat(0,0);
            trs_screen_refresh();
            trs_x_flush();
          }
          break;
        case SDLK_s:
#ifdef MACOSX
          if (!fullscreen) {
            ControlManagerSaveState();
          } else
#endif
          {
            SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
            trs_pause_audio(1);
            trs_gui_save_state();   
            trs_pause_audio(0);
            SDL_EnableKeyRepeat(0,0);
            trs_screen_refresh();
            trs_x_flush();
          }
          break;
        case SDLK_l:
#ifdef MACOSX
          if (!fullscreen) {
            ControlManagerLoadState();
          } else
#endif
          {
            SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
            trs_pause_audio(1);
            trs_gui_load_state();   
            trs_pause_audio(0);
            SDL_EnableKeyRepeat(0,0);
            trs_screen_init();
            grafyx_redraw();
            trs_screen_refresh();
            trs_x_flush();
          }
          break;
        case SDLK_w:
#ifdef MACOSX
          if (!fullscreen) {
            ControlManagerWriteConfig();
          } else
#endif
          {
            SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
            trs_pause_audio(1);
            trs_gui_write_config();   
            trs_pause_audio(0);
            SDL_EnableKeyRepeat(0,0);
            trs_screen_refresh();
            trs_x_flush();
          }
          break;
        case SDLK_r:
#ifdef MACOSX
          if (!fullscreen) {
            ControlManagerReadConfig();
          } else
#endif
          {
            SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, SDL_DEFAULT_REPEAT_INTERVAL);
            trs_pause_audio(1);
            ret = trs_gui_read_config();   
            trs_pause_audio(0);
            SDL_EnableKeyRepeat(0,0);
            if (!ret) {
              trs_screen_init();
              grafyx_redraw();
              trs_screen_refresh();
              trs_x_flush();
            }
          }
          break;
        case SDLK_EQUALS:
          scale_x++;
          if (scale_x > MAX_SCALE)
            scale_x = 1;
          scale_y = scale_x * 2;
          trs_screen_init();
          grafyx_redraw();
          trs_screen_refresh();
          trs_x_flush();
          break;
        case SDLK_MINUS:
          scale_x--;
          if (scale_x < 1)
            scale_x = MAX_SCALE;
          scale_y = scale_x * 2;
          trs_screen_init();
          grafyx_redraw();
          trs_screen_refresh();
          trs_x_flush();
          break;
        case SDLK_p:
          trs_paused = !trs_paused;
          if (!trs_paused)
            trs_screen_refresh();
          break;
#ifdef MACOSX				
		case SDLK_v:
			SDLMainPaste();
			break;
		case SDLK_c:
			SDLMainCopy();
			break;
		case SDLK_a:
			SDLMainSelectAll();
			break;
#endif				
        case SDLK_0:
		case SDLK_1:
        case SDLK_2:
        case SDLK_3:
        case SDLK_4:
        case SDLK_5:
        case SDLK_6:
        case SDLK_7:
#ifdef MACOSX
          if (!fullscreen) {
            if (keysym.mod & KMOD_SHIFT) 
               MediaManagerRemoveDisk(keysym.sym-SDLK_0);
            else
               MediaManagerInsertDisk(keysym.sym-SDLK_0);
          } else
#endif
          {
            char filename[FILENAME_MAX];
            char browse_dir[FILENAME_MAX];

            if (keysym.mod & KMOD_SHIFT) {
              trs_disk_remove(keysym.sym-SDLK_0);
            } else {
              SDL_EnableKeyRepeat(SDL_DEFAULT_REPEAT_DELAY, 
                                  SDL_DEFAULT_REPEAT_INTERVAL);
              trs_expand_dir(trs_disk_dir, browse_dir);
              if (trs_gui_file_browse(browse_dir, filename,0,
                                      " Floppy Disk Image ") != -1)
                trs_disk_insert(keysym.sym-SDLK_0, filename);
              SDL_EnableKeyRepeat(0,0);
              trs_screen_refresh();
              trs_x_flush();
            }
          }
          break;
        default:
          break;
        }
        if (trs_paused)
          trs_gui_display_pause();
#ifdef MACOSX
        if (!fullscreen) {
          SetControlManagerModel(trs_model, grafyx_get_microlabs());
          SetControlManagerTurboMode(trs_timer_is_turbo());
          UpdateMediaManagerInfo();
        }
#endif  
        break;
      }

      if (trs_paused) {
        trs_gui_display_pause();
        break;
      }
#endif
              
      if ( ((keysym.mod & (KMOD_CAPS|KMOD_LSHIFT))
	    	== (KMOD_CAPS|KMOD_LSHIFT) ||
           ((keysym.mod & (KMOD_CAPS|KMOD_RSHIFT))
	    	== (KMOD_CAPS|KMOD_RSHIFT)))
	       && keysym.unicode >= 'A' && keysym.unicode <= 'Z')  {
	  /* Make Shift + CapsLock give lower case */
         keysym.unicode = (int) keysym.unicode + 0x20;
      }
      if (keysym.sym == SDLK_RSHIFT && trs_model == 1) {
        keysym.sym = SDLK_LSHIFT;
      }
     
      if (last_key[keysym.scancode] != 0) {
        trs_xlate_keysym(0x10000 | last_key[keysym.scancode]);
       }
      if (keysym.sym < 0x100 && keysym.unicode >= 0x20 && keysym.unicode <= 0xFF) {
         last_key[keysym.scancode] = keysym.unicode;
         trs_xlate_keysym(keysym.unicode);
         }
      else if (keysym.sym != 0) {
        last_key[keysym.scancode] = keysym.sym;
        trs_xlate_keysym(keysym.sym);
        }
      break;

    case SDL_KEYUP:
      keysym  = event.key.keysym;
#if XDEBUG
      debug("KeyUp: mod 0x%x, scancode 0x%x keycode 0x%x, unicode 0x%x\n",
	      keysym.mod, keysym.scancode, keysym.sym, keysym.unicode);
#endif
      if (keysym.mod & MENU_MOD)
        break;
      keyup = last_key[event.key.keysym.scancode];
      last_key[event.key.keysym.scancode] = 0;
      trs_xlate_keysym(0x10000 | keyup);
      break;

    case SDL_JOYAXISMOTION:
      trs_joy_axis(event.jaxis.axis, event.jaxis.value);
      break;

    case SDL_JOYHATMOTION:
      trs_joy_hat(event.jhat.value);
      break;

    case SDL_JOYBUTTONUP:
      trs_joy_button_up();
      break;

    case SDL_JOYBUTTONDOWN:
      trs_joy_button_down();
      break;

#ifdef MACOSX
    case SDL_USEREVENT:
      trs_handle_mac_events(&event);
      break;
#endif

    default:
#if XDEBUG	    
//      debug("Unhandled event: type %d\n", event.type);
#endif
      break;
    }
  } while (!wait);
}

void trs_screen_expanded(int flag)
{
  int bit = flag ? EXPANDED : 0;
  if ((currentmode ^ bit) & EXPANDED) {
    currentmode ^= EXPANDED;
#ifndef ANDROID
	SDL_FillRect(screen,NULL,background);
    trs_screen_refresh();
#endif
  }
}

#ifdef ANDROID
int is_expanded_mode()
{
  return currentmode & EXPANDED;
}
#endif

void trs_screen_inverse(int flag)
{
  int bit = flag ? INVERSE : 0;
  int i;
  if ((currentmode ^ bit) & INVERSE) {
    currentmode ^= INVERSE;
    for (i = 0; i < screen_chars; i++) {
      if (trs_screen[i] & 0x80)
	trs_screen_write_char(i, trs_screen[i]);
    }
  }
}

void trs_screen_alternate(int flag)
{
  int bit = flag ? ALTERNATE : 0;
  int i;
  if ((currentmode ^ bit) & ALTERNATE) {
    currentmode ^= ALTERNATE;
    for (i = 0; i < screen_chars; i++) {
      if (trs_screen[i] >= 0xc0)
	trs_screen_write_char(i, trs_screen[i]);
    }
  }
}

void trs_screen_640x240(int flag)
{
  if (flag == screen640x240) return;
  screen640x240 = flag;
  if (flag) {
    row_chars = 80;
    col_chars = 24;
    cur_char_height = TRS_CHAR_HEIGHT4 * scale_y;
  } else {
    row_chars = 64;
    col_chars = 16;
    cur_char_height = TRS_CHAR_HEIGHT * scale_y;
  }
  screen_chars = row_chars * col_chars;
  if (resize) {
    trs_screen_init();
  } else {
    left_margin = cur_char_width * (80 - row_chars)/2 + border_width;
    top_margin = (TRS_CHAR_HEIGHT4 * scale_y * 24 -
	  	      cur_char_height * col_chars)/2 + border_width;
    if (left_margin > border_width || top_margin > border_width) 
      SDL_FillRect(screen,NULL,background);
  }
  trs_screen_refresh();
}

void trs_screen_80x24(int flag)
{
  if (!grafyx_enable || grafyx_overlay) {
    trs_screen_640x240(flag);
  }
  text80x24 = flag;
}

void screen_init()
{
  int i;

  /* initially, screen is blank (i.e. full of spaces) */
  for (i = 0; i < sizeof(trs_screen); i++)
    trs_screen[i] = ' ';
}

void
boxes_init(int foreground, int background, int width, int height, int expanded)
{
  SDL_Rect fullrect;
  int graphics_char, bit, p;
  SDL_Rect bits[6];

  /*
   * Calculate what the 2x3 boxes look like.
   */
  bits[0].x = bits[2].x = bits[4].x = 0;
  bits[0].w = bits[2].w = bits[4].w =
    bits[1].x = bits[3].x = bits[5].x =  width / 2;
  bits[1].w = bits[3].w = bits[5].w = width - bits[1].x;

  bits[0].y = bits[1].y = 0;
  bits[0].h = bits[1].h =
    bits[2].y = bits[3].y = height / 3;
  bits[4].y = bits[5].y = (height * 2) / 3;
  bits[2].h = bits[3].h = bits[4].y - bits[2].y;
  bits[4].h = bits[5].h = height - bits[4].y;
  
  fullrect.x = 0;
  fullrect.y = 0;
  fullrect.h = height;
  fullrect.w = width;

  for (graphics_char = 0; graphics_char < 64; ++graphics_char) {
    if (trs_box[expanded][graphics_char])
      SDL_FreeSurface(trs_box[expanded][graphics_char]);
    trs_box[expanded][graphics_char] =
	  SDL_CreateRGBSurface(SDL_SWSURFACE, width, height, 32, 
                           0x00ff0000, 0x0000ff00,0x000000ff,0);

    /* Clear everything */
    SDL_FillRect(trs_box[expanded][graphics_char], &fullrect, background);
    
    for (bit = 0, p = 0; bit < 6; ++bit) {
      if (graphics_char & (1 << bit)) {
      	SDL_FillRect(trs_box[expanded][graphics_char], &bits[bit], foreground);
      }
    }
  }
}

SDL_Surface *CreateSurfaceFromDataScale(char *data,
                     unsigned int foreground,
                     unsigned int background,
                     unsigned int width, 
				     unsigned int height,
				     unsigned int scale_x,
				     unsigned int scale_y)
{
  static unsigned int *mydata, *currdata;
  static unsigned char *mypixels, *currpixel;
  int i, j, w;

  /* 
   * Allocate a bit more room than necessary - There shouldn't be 
   * any proportional characters, but just in case...             
   * These arrays never get released, but they are really not     
   * too big, so we should be OK.
   */
  mydata = (unsigned int *)malloc(width * height *
		     scale_x * scale_y * sizeof(unsigned int));
  mypixels= (unsigned char *)malloc(width * height * 8);
  
  /* Read the character data */ 
  for (j= 0; j< width * height; j += 8)
  {
    for (i= j + 7; i >= j; i--)
    {
      *(mypixels + i)= (*(data + (j >> 3)) >> (i - j)) & 1;
    }
  }

  currdata = mydata;
  /* And prepare our rescaled character. */
  for (j= 0; j< height * scale_y; j++)
  {
    currpixel = mypixels + ((j/scale_y) * width);
    for (w= 0; w< width ; w++)
    {
    if (*currpixel++ == 0) {
      for (i=0;i<scale_x;i++)
 	    *currdata++ = background;
      }
    else {
      for (i=0;i<scale_x;i++)
 	    *currdata++ = foreground;
      }
    }
  }
  
  free(mypixels);
  
  return(SDL_CreateRGBSurfaceFrom(mydata, width*scale_x, height*scale_y, 32, width*scale_x*4,
                                  0x00ff0000, 0x0000ff00,
                                  0x000000ff,0));
}

void bitmap_init(unsigned long foreground, unsigned long background)
{
    /* Initialize from built-in font bitmaps. */
    int i;
	
    for (i = 0; i < MAXCHARS; i++) {
      if (trs_char[0][i]) {
         free(trs_char[0][i]->pixels);
         SDL_FreeSurface(trs_char[0][i]);
         }
      trs_char[0][i] =
	    CreateSurfaceFromDataScale(trs_char_data[trs_charset][i],
	               foreground, background,
				   TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				   scale_x,scale_y);
      if (trs_char[1][i]) {
         free(trs_char[1][i]->pixels);
         SDL_FreeSurface(trs_char[1][i]);
         }
      trs_char[1][i] =
	    CreateSurfaceFromDataScale(trs_char_data[trs_charset][i],
	               foreground, background,
				   TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				   scale_x*2,scale_y);
      if (trs_char[2][i]) {
         free(trs_char[2][i]->pixels);
         SDL_FreeSurface(trs_char[2][i]);
         }
      trs_char[2][i] =
	    CreateSurfaceFromDataScale(trs_char_data[trs_charset][i],
	               background, foreground,
				   TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				   scale_x,scale_y);
      if (trs_char[3][i]) {
         free(trs_char[3][i]->pixels);
         SDL_FreeSurface(trs_char[3][i]);
         }
      trs_char[3][i] =
	    CreateSurfaceFromDataScale(trs_char_data[trs_charset][i],
	               background, foreground,
				   TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				   scale_x*2,scale_y);
      if (trs_char[4][i]) {
         free(trs_char[4][i]->pixels);
         SDL_FreeSurface(trs_char[4][i]);
         }
      /* For the GUI, make sure we have a backslash , not arrow */
      if (i=='\\')
        trs_char[4][i] =
	      CreateSurfaceFromDataScale(trs_char_data[0][i],
	                 gui_foreground, gui_background,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
      else if (trs_charset<3)
        trs_char[4][i] =
	      CreateSurfaceFromDataScale(trs_char_data[2][i],
	                 gui_foreground, gui_background,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
      else
        trs_char[4][i] =
	      CreateSurfaceFromDataScale(trs_char_data[3][i],
	                 gui_foreground, gui_background,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
      if (trs_char[5][i]) {
         free(trs_char[5][i]->pixels);
         SDL_FreeSurface(trs_char[5][i]);
         }
      if (i=='\\')
        trs_char[5][i] =
	      CreateSurfaceFromDataScale(trs_char_data[0][i],
	                 gui_background, gui_foreground,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
      else if (trs_charset<3)
        trs_char[5][i] =
		CreateSurfaceFromDataScale(trs_char_data[2][i],
	                 gui_background, gui_foreground,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
      else
        trs_char[5][i] =
	      CreateSurfaceFromDataScale(trs_char_data[3][i],
	                 gui_background, gui_foreground,
				     TRS_CHAR_WIDTH,TRS_CHAR_HEIGHT,
				     scale_x,scale_y);
    }
    boxes_init(foreground, background,
	       cur_char_width, TRS_CHAR_HEIGHT * scale_y, 0);
    boxes_init(foreground, background,
	       cur_char_width*2, TRS_CHAR_HEIGHT * scale_y, 1);  
    boxes_init(gui_foreground, gui_background,
	       cur_char_width, TRS_CHAR_HEIGHT * scale_y, 2);  
}

void trs_screen_refresh()
{
  int i, srcx, srcy, dunx, duny;
  SDL_Rect srcRect, destRect;

#if XDEBUG
  debug("trs_screen_refresh\n");
#endif
  if (grafyx_enable && !grafyx_overlay) {
    srcx = cur_char_width * grafyx_xoffset;
    srcy = scale_y * grafyx_yoffset;
    srcRect.x = srcx;
    srcRect.y = srcy;
    srcRect.w = cur_char_width*row_chars;
    srcRect.h = cur_char_height*col_chars;
    destRect.x = left_margin;
    destRect.y = top_margin;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    SDL_BlitSurface(image, &srcRect, screen, &destRect);
	addToDrawList(&destRect);
    /* Draw wrapped portions if any */
    dunx = imageSize.width - srcx;
    if (dunx < cur_char_width*row_chars) {
      srcRect.x = 0;
      srcRect.y = srcy;
      srcRect.w = cur_char_width*row_chars - dunx;
      srcRect.h = cur_char_height*col_chars;
      destRect.x = left_margin + dunx;
      destRect.y = top_margin;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      SDL_BlitSurface(image, &srcRect, screen, &destRect);
	  addToDrawList(&destRect);
    }
    duny = imageSize.height - srcy;
    if (duny < cur_char_height*col_chars) {
      srcRect.x = srcx;
      srcRect.y = 0;
      srcRect.w = cur_char_width*row_chars;
      srcRect.h = cur_char_height*col_chars - duny;
      destRect.x = left_margin;
      destRect.y = top_margin + duny;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
	  addToDrawList(&destRect);
      SDL_BlitSurface(image, &srcRect, screen, &destRect);
      if (dunx < cur_char_width*row_chars) {
        srcRect.x = 0;
        srcRect.y = 0;
        srcRect.w = cur_char_width*row_chars - dunx;
        srcRect.h = cur_char_height*col_chars - duny;
        destRect.x = left_margin + dunx;
        destRect.y = top_margin + duny;
        destRect.w = srcRect.w;
        destRect.h = srcRect.h;
	    addToDrawList(&destRect);
        SDL_BlitSurface(image, &srcRect, screen, &destRect);
      }
    }
  } else {
    for (i = 0; i < screen_chars; i++) {
      trs_screen_write_char(i, trs_screen[i]);
    }
  }
  drawnRectCount = MAX_RECTS; // Will force redraw of whole screen
}

void trs_disk_led(int drive, int on_off)
{
  SDL_Rect rect;
  static int countdown[8] = {0,0,0,0,0,0,0,0};
  int i;

  if (trs_show_led) {
    int drive0_led_x = border_width;
    rect.w = 16*scale_x;
    rect.h = 2*scale_y;
    rect.y = OrigHeight - led_width/2;
  
    if (drive == -1) {
      for (i=0;i<8;i++) {
        rect.x = drive0_led_x + 24*scale_x*i;
        SDL_FillRect(screen, &rect, light_red);
        addToDrawList(&rect);
#ifdef MACOSX
        MediaManagerStatusLed(i,0);
#endif  
      }
    }
    if (on_off) {
      if (countdown[drive] == 0) {
        rect.x = drive0_led_x + 24*scale_x*drive;
        SDL_FillRect(screen, &rect, bright_red);
        addToDrawList(&rect);
#ifdef MACOSX
        MediaManagerStatusLed(drive,1);
#endif  
        }
      countdown[drive] = 2*timer_hz;
      }
    else {
      for (i=0;i<8;i++) {
        if (countdown[i]) {
          countdown[i]--;
          if (countdown[i] == 0) {
            rect.x = drive0_led_x + 24*scale_x*i;
            SDL_FillRect(screen, &rect, light_red);
            addToDrawList(&rect);
#ifdef MACOSX
            MediaManagerStatusLed(i,0);
#endif  
          }
        }
      }
    }
  }
}

void trs_hard_led(int drive, int on_off)
{
  SDL_Rect rect;
  static int countdown[4] = {0,0,0,0};
  int i;

  if (trs_show_led) {
    int drive0_led_x = OrigWidth - border_width - 88*scale_x;
    rect.w = 16*scale_x;
    rect.h = 2*scale_y;
    rect.y = OrigHeight - led_width/2;
  
    if (drive == -1) {
      for (i=0;i<4;i++) {
        rect.x = drive0_led_x + 24*scale_x*i;
        SDL_FillRect(screen, &rect, light_red);
        addToDrawList(&rect);
#ifdef MACOSX
        MediaManagerStatusLed(i+8, 0);
#endif  
      }
    }
    if (on_off) {
      if (countdown[drive] == 0) {
        rect.x = drive0_led_x + 24*scale_x*drive;
        SDL_FillRect(screen, &rect, bright_red);
        addToDrawList(&rect);
#ifdef MACOSX
        MediaManagerStatusLed(drive+8, 1);
#endif  
        }
      countdown[drive] = timer_hz/2;
      }
    else {
      for (i=0;i<4;i++) {
        if (countdown[i]) {
          countdown[i]--;
          if (countdown[i] == 0) {
            rect.x = drive0_led_x + 24*scale_x*i;
            SDL_FillRect(screen, &rect, light_red);
            addToDrawList(&rect);
#ifdef MACOSX
            MediaManagerStatusLed(i+8, 0);
#endif  
          }
        }
      }
    }
  }
}

void trs_screen_write_char(int position, int char_index)
{
#ifdef ANDROID
  trs_screen[position] = char_index;
#else
  int row,col,destx,desty;
  int plane;
  SDL_Rect srcRect, destRect;

  trs_screen[position] = char_index;
  if (position >= screen_chars) {
    return;
  }
  if ((currentmode & EXPANDED) && (position & 1)) {
    return;
  }
  if (grafyx_enable && !grafyx_overlay) {
    return;
  }
  row = position / row_chars;
  col = position - (row * row_chars);
  destx = col * cur_char_width + left_margin;
  desty = row * cur_char_height + top_margin;

  if (trs_model == 1 && char_index >= 0xc0) {
    /* On Model I, 0xc0-0xff is another copy of 0x80-0xbf */
    char_index -= 0x40;
  }
  if (char_index >= 0x80 && char_index <= 0xbf && !(currentmode & INVERSE)) {
    /* Use graphics character bitmap instead of font */
    switch (currentmode & EXPANDED) {
    case NORMAL:
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      SDL_BlitSurface(trs_box[0][char_index-0x80], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    case EXPANDED:
      /* use expanded graphics character bitmap instead of font */
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width*2;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      SDL_BlitSurface(trs_box[1][char_index-0x80], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    } 
  } else {
    /* Draw character using a builtin bitmap */
    if (trs_model > 1 && char_index >= 0xc0 &&
	(currentmode & (ALTERNATE+INVERSE)) == 0) {
      char_index -= 0x40;
    }
    plane = 1;
    switch (currentmode & ~ALTERNATE) {
    case NORMAL:
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      SDL_BlitSurface(trs_char[0][char_index], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    case EXPANDED:
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width*2;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      SDL_BlitSurface(trs_char[1][char_index], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    case INVERSE:
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      if (char_index & 0x80) 
        SDL_BlitSurface(trs_char[2][char_index & 0x7f], &srcRect, screen, &destRect);
      else
        SDL_BlitSurface(trs_char[0][char_index & 0x7f], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    case EXPANDED+INVERSE:
      srcRect.x = 0;
      srcRect.y = 0;
      srcRect.w = cur_char_width*2;
      srcRect.h = cur_char_height;
      destRect.x = destx;
      destRect.y = desty;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      if (char_index & 0x80) 
        SDL_BlitSurface(trs_char[3][char_index & 0x7f], &srcRect, screen, &destRect);
      else
        SDL_BlitSurface(trs_char[1][char_index & 0x7f], &srcRect, screen, &destRect);
      addToDrawList(&destRect);
      break;
    }
  }
  if (grafyx_enable) {
    /* assert(grafyx_overlay); */
    int srcx, srcy, duny;
    srcx = ((col+grafyx_xoffset) % G_XSIZE)*cur_char_width;
    srcy = (row*cur_char_height + grafyx_yoffset*scale_y)
	   % (G_YSIZE*scale_y); 
    srcRect.x = srcx;
    srcRect.y = srcy;
    srcRect.w = cur_char_width;
    srcRect.h = cur_char_height;
    destRect.x = destx;
    destRect.y = desty;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    addToDrawList(&destRect);
    TrsSoftBlit(image, &srcRect, screen, &destRect,1);
    /* Draw wrapped portion if any */
    duny = imageSize.height - srcy;
    if (duny < cur_char_height) {
      srcRect.x = srcx;
      srcRect.y = 0;
      srcRect.w = cur_char_width;
      srcRect.h = cur_char_height - duny;
      destRect.x = destx;
      destRect.y = desty + duny;
      destRect.w = srcRect.w;
      destRect.h = srcRect.h;
      addToDrawList(&destRect);
      TrsSoftBlit(image, &srcRect, screen, &destRect,1);
    }
  }
  if (hrg_enable) {
    hrg_update_char(position);
  }
#endif
}

#ifdef ANDROID
void trs_gui_write_char(int position, int char_index, int invert);
#endif

void trs_gui_refresh()
{
  int i;

  for (i = 0; i < screen_chars; i++) 
      trs_gui_write_char(i, trs_gui_screen[i], trs_gui_screen_invert[i]);

  drawnRectCount = MAX_RECTS; // Will force redraw of whole screen
}

void trs_gui_write_char(int position, int char_index, int invert)
{
  int row,col,destx,desty;
  SDL_Rect srcRect, destRect;

  if (position >= screen_chars) {
    return;
  }

  trs_gui_screen[position] = char_index;
  trs_gui_screen_invert[position] = invert;
  
  /* Add offsets if we are in 80x24 mode */
  if (row_chars != 64) {
    row = position / 64;
    col = position - (row * 64);
    position = (row + (col_chars-16)/2) * row_chars +
               col + (row_chars-64)/2;
  }

  row = position / row_chars;
  col = position - (row * row_chars);
  destx = col * cur_char_width + left_margin;
  desty = row * cur_char_height + top_margin;

  if (trs_model == 1 && char_index >= 0xc0) {
    /* On Model I, 0xc0-0xff is another copy of 0x80-0xbf */
    char_index -= 0x40;
  }
  if (char_index >= 0x80 && char_index <= 0xbf && !(currentmode & INVERSE)) {
    /* Use graphics character bitmap instead of font */
    srcRect.x = 0;
    srcRect.y = 0;
    srcRect.w = cur_char_width;
    srcRect.h = cur_char_height;
    destRect.x = destx;
    destRect.y = desty;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    SDL_BlitSurface(trs_box[2][char_index-0x80], &srcRect, screen, &destRect);
    addToDrawList(&destRect);
  } else {
    /* Draw character using a builtin bitmap */
    if (trs_model > 1 && char_index >= 0xc0 &&
	(currentmode & (ALTERNATE+INVERSE)) == 0) {
      char_index -= 0x40;
    }
    srcRect.x = 0;
    srcRect.y = 0;
    srcRect.w = cur_char_width;
    srcRect.h = cur_char_height;
    destRect.x = destx;
    destRect.y = desty;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    if (invert)
      SDL_BlitSurface(trs_char[5][char_index], &srcRect, screen, &destRect);
    else
      SDL_BlitSurface(trs_char[4][char_index], &srcRect, screen, &destRect);
    addToDrawList(&destRect);
  }
}
void trs_gui_clear_screen(void)
{
    int i;
    for (i=0;i<1024;i++)
       trs_gui_write_char(i,' ',0);
}


 /* Copy lines 1 through col_chars-1 to lines 0 through col_chars-2.
    Doesn't need to clear line col_chars-1. */
void trs_screen_scroll()
{
  int i = 0;
  SDL_Rect srcRect, destRect;

  for (i = row_chars; i < screen_chars; i++)
    trs_screen[i-row_chars] = trs_screen[i];

#ifndef ANDROID
  if (grafyx_enable) {
    if (grafyx_overlay) {
      trs_screen_refresh();
    }
  } else if (hrg_enable) {
    trs_screen_refresh();
  } else {
    srcRect.x = left_margin;
    srcRect.y = cur_char_height+top_margin;
    srcRect.w = cur_char_width*row_chars;
    srcRect.h = cur_char_height*(col_chars-1);
    destRect.x = left_margin;
    destRect.y = top_margin;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    SDL_BlitSurface(screen, &srcRect, screen, &destRect);
    addToDrawList(&destRect);
  }
#endif
}

void grafyx_write_byte(int x, int y, char byte)
{
  int i, j;
  char exp[MAX_SCALE];
  int screen_x = ((x - grafyx_xoffset + G_XSIZE) % G_XSIZE);
  int screen_y = ((y - grafyx_yoffset + G_YSIZE) % G_YSIZE);
  int on_screen = screen_x < row_chars &&
    screen_y < col_chars*cur_char_height/scale_y;
  SDL_Rect srcRect, destRect;
  
  if (grafyx_enable && grafyx_overlay && on_screen) {
    srcRect.x = x*cur_char_width;
    srcRect.y = y*scale_y;
    srcRect.w = cur_char_width;
    srcRect.h = scale_y;
    destRect.x = left_margin + screen_x*cur_char_width;
    destRect.y = top_margin + screen_y*scale_y;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    /* Erase old byte, preserving text */
    TrsSoftBlit(image, &srcRect, screen, &destRect,1);
  }

  /* Save new byte in local memory */
  grafyx_unscaled[y][x] = byte;
  switch (scale_x) {
  case 1:
  default:
    exp[0] = byte;
    break;
  case 2:
    exp[1] = ((byte & 0x01) + ((byte & 0x02) << 1)
	      + ((byte & 0x04) << 2) + ((byte & 0x08) << 3)) * 3;
    exp[0] = (((byte & 0x10) >> 4) + ((byte & 0x20) >> 3)
	      + ((byte & 0x40) >> 2) + ((byte & 0x80) >> 1)) * 3;
    break;
  case 3:
    exp[2] = ((byte & 0x01) + ((byte & 0x02) << 2)
	      + ((byte & 0x04) << 4)) * 7;
    exp[1] = (((byte & 0x08) >> 2) + (byte & 0x10)
	      + ((byte & 0x20) << 2)) * 7 + ((byte & 0x04) >> 2);
    exp[0] = (((byte & 0x40) >> 4) + ((byte & 0x80) >> 2)) * 7
           + ((byte & 0x20) >> 5) * 3;
    break;
  case 4:
    exp[3] = ((byte & 0x01) + ((byte & 0x02) << 3)) * 15;
    exp[2] = (((byte & 0x04) >> 2) + ((byte & 0x08) << 1)) * 15;
    exp[1] = (((byte & 0x10) >> 4) + ((byte & 0x20) >> 1)) * 15;
    exp[0] = (((byte & 0x40) >> 6) + ((byte & 0x80) >> 3)) * 15;
    break;
  }
  for (j=0; j<scale_y; j++) {
    for (i=0; i<scale_x; i++) {
      grafyx[(y*scale_y + j)*imageSize.bytes_per_line + x*scale_x + i] = exp[i];
    }
  }

  if (grafyx_enable && on_screen) {
    /* Draw new byte */
    srcRect.x = x*cur_char_width;
    srcRect.y = y*scale_y;
    srcRect.w = cur_char_width;
    srcRect.h = scale_y;
    destRect.x = left_margin + screen_x*cur_char_width;
    destRect.y = top_margin + screen_y*scale_y;
    destRect.w = srcRect.w;
    destRect.h = srcRect.h;
    addToDrawList(&destRect);
    if (grafyx_overlay) {
      TrsSoftBlit(image, &srcRect, screen, &destRect,1);
    } else {
      TrsSoftBlit(image, &srcRect, screen, &destRect,0);
    }
  }
}

void grafyx_redraw(void)
{
  int i, j;
  char exp[MAX_SCALE];
  int screen_x, screen_y, on_screen;
  int x,y;
  char byte;
  
  for (y=0;y<G_YSIZE;y++) {
    for (x=0;x<G_XSIZE;x++) {
    screen_x = ((x - grafyx_xoffset + G_XSIZE) % G_XSIZE);
    screen_y = ((y - grafyx_yoffset + G_YSIZE) % G_YSIZE);
    on_screen = screen_x < row_chars &&
                screen_y < col_chars*cur_char_height/scale_y;
      byte = grafyx_unscaled[y][x];
      switch (scale_x) {
      default:
      case 1:
        exp[0] = byte;
        break;
      case 2:
        exp[1] = ((byte & 0x01) + ((byte & 0x02) << 1)
    	      + ((byte & 0x04) << 2) + ((byte & 0x08) << 3)) * 3;
        exp[0] = (((byte & 0x10) >> 4) + ((byte & 0x20) >> 3)
    	      + ((byte & 0x40) >> 2) + ((byte & 0x80) >> 1)) * 3;
        break;
      case 3:
        exp[2] = ((byte & 0x01) + ((byte & 0x02) << 2)
    	      + ((byte & 0x04) << 4)) * 7;
        exp[1] = (((byte & 0x08) >> 2) + (byte & 0x10)
    	      + ((byte & 0x20) << 2)) * 7 + ((byte & 0x04) >> 2);
        exp[0] = (((byte & 0x40) >> 4) + ((byte & 0x80) >> 2)) * 7
               + ((byte & 0x20) >> 5) * 3;
        break;
      case 4:
        exp[3] = ((byte & 0x01) + ((byte & 0x02) << 3)) * 15;
        exp[2] = (((byte & 0x04) >> 2) + ((byte & 0x08) << 1)) * 15;
        exp[1] = (((byte & 0x10) >> 4) + ((byte & 0x20) >> 1)) * 15;
        exp[0] = (((byte & 0x40) >> 6) + ((byte & 0x80) >> 3)) * 15;
        break;
      }
      for (j=0; j<scale_y; j++) {
        for (i=0; i<scale_x; i++) {
          grafyx[(y*scale_y + j)*imageSize.bytes_per_line + x*scale_x + i] = exp[i];
        }
      }
    }
  }
}

void grafyx_write_x(int value)
{
  grafyx_x = value;
}

void grafyx_write_y(int value)
{
  grafyx_y = value;
}

void grafyx_write_data(int value)
{
  grafyx_write_byte(grafyx_x % G_XSIZE, grafyx_y, value);
  if (!(grafyx_mode & G_XNOCLKW)) {
    if (grafyx_mode & G_XDEC) {
      grafyx_x--;
    } else {
      grafyx_x++;
    }
  }
  if (!(grafyx_mode & G_YNOCLKW)) {
    if (grafyx_mode & G_YDEC) {
      grafyx_y--;
    } else {
      grafyx_y++;
    }
  }
}

int grafyx_read_data()
{
  int value = grafyx_unscaled[grafyx_y][grafyx_x % G_XSIZE];
  if (!(grafyx_mode & G_XNOCLKR)) {
    if (grafyx_mode & G_XDEC) {
      grafyx_x--;
    } else {
      grafyx_x++;
    }
  }
  if (!(grafyx_mode & G_YNOCLKR)) {
    if (grafyx_mode & G_YDEC) {
      grafyx_y--;
    } else {
      grafyx_y++;
    }
  }
  return value;
}

void grafyx_write_mode(int value)
{
  unsigned char old_enable = grafyx_enable;
  unsigned char old_overlay = grafyx_overlay;

  grafyx_enable = value & G_ENABLE;
  if (grafyx_microlabs) {
    grafyx_overlay = (value & G_UL_NOTEXT) == 0;
  }
  grafyx_mode = value;
  trs_screen_640x240((grafyx_enable && !grafyx_overlay) || text80x24);
  if (old_enable != grafyx_enable || 
      (grafyx_enable && old_overlay != grafyx_overlay)) {
    
    trs_screen_refresh();
  }
}

void grafyx_write_xoffset(int value)
{
  unsigned char old_xoffset = grafyx_xoffset;
  grafyx_xoffset = value % G_XSIZE;
  if (grafyx_enable && old_xoffset != grafyx_xoffset) {
    trs_screen_refresh();
  }
}

void grafyx_write_yoffset(int value)
{
  unsigned char old_yoffset = grafyx_yoffset;
  grafyx_yoffset = value;
  if (grafyx_enable && old_yoffset != grafyx_yoffset) {
    trs_screen_refresh();
  }
}

void grafyx_write_overlay(int value)
{
  unsigned char old_overlay = grafyx_overlay;
  grafyx_overlay = value & 1;
  if (grafyx_enable && old_overlay != grafyx_overlay) {
    trs_screen_640x240((grafyx_enable && !grafyx_overlay) || text80x24);
    trs_screen_refresh();
  }
}

int grafyx_get_microlabs()
{
  return grafyx_microlabs;
}

void grafyx_set_microlabs(int on_off)
{
  grafyx_microlabs = on_off;
}

/* Model III MicroLabs support */
void grafyx_m3_reset()
{
  if (grafyx_microlabs) grafyx_m3_write_mode(0);
}

void grafyx_m3_write_mode(int value)
{
  int enable = (value & G3_ENABLE) != 0;
  int changed = (enable != grafyx_enable);
  grafyx_enable = enable;
  grafyx_overlay = enable;
  grafyx_mode = value;
  grafyx_y = G3_YLOW(value);
  if (changed) trs_screen_refresh();
}

int grafyx_m3_write_byte(int position, int byte)
{
  if (grafyx_microlabs && (grafyx_mode & G3_COORD)) {
    int x = (position % 64);
    int y = (position / 64) * 12 + grafyx_y;
    grafyx_write_byte(x, y, byte);
    return 1;
  } else {
    return 0;
  }
}

unsigned char grafyx_m3_read_byte(int position)
{
  if (grafyx_microlabs && (grafyx_mode & G3_COORD)) {
    int x = (position % 64);
    int y = (position / 64) * 12 + grafyx_y;
    return grafyx_unscaled[y][x];
  } else {
    return trs_screen[position];
  }
}

int grafyx_m3_active()
{
  return (trs_model == 3 && grafyx_microlabs && (grafyx_mode & G3_COORD));
}

/*
 * Support for Model I HRG1B 384*192 graphics card
 * (sold in Germany for Model I and Video Genie by RB-Elektronik).
 *
 * Assignment of ports is as follows:
 *    Port 0x00 (out): switch graphics screen off (value ignored).
 *    Port 0x01 (out): switch graphics screen on (value ignored).
 *    Port 0x02 (out): select screen memory address (LSB).
 *    Port 0x03 (out): select screen memory address (MSB).
 *    Port 0x04 (in):  read byte from screen memory.
 *    Port 0x05 (out): write byte to screen memory.
 * (The real hardware decodes only address lines A0-A2 and A7, so
 * that there are several "shadow" ports in the region 0x08-0x7d.
 * However, these undocumented ports are not implemented here.)
 *
 * The 16-bit memory address (port 2 and 3) is used for subsequent
 * read or write operations. It corresponds to a position on the
 * graphics screen, in the following way:
 *    Bits 0-5:   character column address (0-63)
 *    Bits 6-9:   character row address (0-15)
 *                (i.e. bits 0-9 are the "PRINT @" position.)
 *    Bits 10-13: address of line within character cell (0-11)
 *    Bits 14-15: not used
 *
 *      <----port 2 (LSB)---->  <-------port 3 (MSB)------->
 * Bit: 0  1  2  3  4  5  6  7  8  9  10  11  12  13  14  15
 *      <-column addr.->  <row addr>  <-line addr.->  <n.u.>
 *
 * Reading from port 4 or writing to port 5 will access six
 * neighbouring pixels corresponding (from left to right) to bits
 * 0-5 of the data byte. Bits 6 and 7 are present in memory, but
 * are ignored.
 *
 * In expanded mode (32 chars per line), the graphics screen has
 * only 192*192 pixels. Pixels with an odd column address (i.e.
 * every second group of 6 pixels) are suppressed.
 */

/* Initialize HRG. */
static void
hrg_init()
{
  int i;

  /* Precompute arrays of pixel sizes and offsets. */
  for (i = 0; i <= 6; i++) {
    hrg_pixel_x[0][i] = cur_char_width * i / 6;
    hrg_pixel_x[1][i] = cur_char_width*2 * i / 6;
    if (i != 0) {
      hrg_pixel_width[0][i-1] = hrg_pixel_x[0][i] - hrg_pixel_x[0][i-1];
      hrg_pixel_width[1][i-1] = hrg_pixel_x[1][i] - hrg_pixel_x[1][i-1];
    }
  }
  for (i = 0; i <= 12; i++) {
    hrg_pixel_y[i] = cur_char_height * i / 12;
    if (i != 0)
      hrg_pixel_height[i-1] = hrg_pixel_y[i] - hrg_pixel_y[i-1];
  }
  if (cur_char_width % 6 != 0 || cur_char_height % 12 != 0)
    error("character size %d*%d not a multiple of 6*12 HRG raster",
	  cur_char_width, cur_char_height);
}

/* Switch HRG on (1) or off (0). */
void
hrg_onoff(int enable)
{
  static int init = 0;

  if ((hrg_enable!=0) == (enable!=0)) return; /* State does not change. */

  if (!init) {
    hrg_init();
    init = 1;
  }
  hrg_enable = enable;
  trs_screen_refresh();
}

/* Write address to latch. */
void
hrg_write_addr(int addr, int mask)
{
  hrg_addr = (hrg_addr & ~mask) | (addr & mask);
}

/* Write byte to HRG memory. */
void
hrg_write_data(int data)
{
  int old_data;
  int position, line;
  int bits0, bits1;

  if (hrg_addr >= HRG_MEMSIZE) return; /* nonexistent address */
  old_data = hrg_screen[hrg_addr];
  hrg_screen[hrg_addr] = data;

  if (!hrg_enable) return;
  if ((currentmode & EXPANDED) && (hrg_addr & 1)) return;
  if ((data &= 0x3f) == (old_data &= 0x3f)) return;

  position = hrg_addr & 0x3ff;	/* bits 0-9: "PRINT @" screen position */
  line = hrg_addr >> 10;	/* vertical offset inside character cell */
  bits0 = ~data & old_data;	/* pattern to clear */
  bits1 = data & ~old_data;	/* pattern to set */

  if (bits0 == 0
      || trs_screen[position] == 0x20
      || trs_screen[position] == 0x80
      /*|| (trs_screen[position] < 0x80 && line >= 8 && !usefont)*/
      ) {
    /* Only additional bits set, or blank text character.
       No need for update of text. */
    int destx = (position % row_chars) * cur_char_width + left_margin;
    int desty = (position / row_chars) * cur_char_height + top_margin
      + hrg_pixel_y[line];
    int *x = hrg_pixel_x[(currentmode&EXPANDED)!=0];
    int *w = hrg_pixel_width[(currentmode&EXPANDED)!=0];
    int h = hrg_pixel_height[line];
    SDL_Rect rect0[3];    /* 6 bits => max. 3 groups of adjacent "0" bits */
    SDL_Rect rect1[3];
    int n0 = 0;
    int n1 = 0;
    int flag = 0;
    int i, j, b;

    /* Compute arrays of rectangles to clear and to set. */
    for (j = 0, b = 1; j < 6; j++, b <<= 1) {
      if (bits0 & b) {
	if (flag >= 0) {	/* Start new rectangle. */
	  rect0[n0].x = destx + x[j];
	  rect0[n0].y = desty;
	  rect0[n0].w = w[j];
	  rect0[n0].h = h;
	  n0++;
	  flag = -1;
	}
	else {			/* Increase width of rectangle. */
	  rect0[n0-1].w += w[j];
	}
      }
      else if (bits1 & b) {
	if (flag <= 0) {
	  rect1[n1].x = destx + x[j];
	  rect1[n1].y = desty;
	  rect1[n1].w = w[j];
	  rect1[n1].h = h;
	  n1++;
	  flag = 1;
	}
	else {
	  rect1[n1-1].w += w[j];
	}
      }
      else {
	flag = 0;
      }
    }
    for (i=0;i<n0;i++)
       SDL_FillRect(screen, &rect0[i], background);
    for (i=0;i<n1;i++)
       SDL_FillRect(screen, &rect0[i], foreground);
  }
  else {
    /* Unfortunately, HRG1B combines text and graphics with an
       (inclusive) OR. Thus, in the general case, we cannot erase
       the old graphics byte without losing the text information.
       Call trs_screen_write_char to restore the text character
       (erasing the graphics). This function will in turn call
       hrg_update_char and restore 6*12 graphics pixels. Sigh. */
    trs_screen_write_char(position, trs_screen[position]);
  }
}

/* Read byte from HRG memory. */
int
hrg_read_data()
{
  if (hrg_addr >= HRG_MEMSIZE) return 0xff; /* nonexistent address */
  return hrg_screen[hrg_addr];
}

/* Update graphics at given screen position.
   Called by trs_screen_write_char. */
static void
hrg_update_char(int position)
{
  int destx = (position % row_chars) * cur_char_width + left_margin;
  int desty = (position / row_chars) * cur_char_height + top_margin;
  int *x = hrg_pixel_x[(currentmode&EXPANDED)!=0];
  int *w = hrg_pixel_width[(currentmode&EXPANDED)!=0];
  SDL_Rect rect[3*12];
  int byte;
  int prev_byte = 0;
  int n = 0;
  int np = 0;
  int i, j, flag;

  /* Compute array of rectangles. */
  for (i = 0; i < 12; i++) {
    if ((byte = hrg_screen[position+(i<<10)] & 0x3f) == 0) {
    }
    else if (byte != prev_byte) {
      np = n;
      flag = 0;
      for (j = 0; j < 6; j++) {
	if (!(byte & 1<<j)) {
	  flag = 0;
	}
	else if (!flag) {	/* New rectangle. */
	  rect[n].x = destx + x[j];
	  rect[n].y = desty + hrg_pixel_y[i];
	  rect[n].w = w[j];
	  rect[n].h = hrg_pixel_height[i];
	  n++;
	  flag = 1;
	}
	else {			/* Increase width. */
	  rect[n-1].w += w[j];
	}
      }
    }
    else {			/* Increase heights. */
      for (j = np; j < n; j++)
	rect[j].h += hrg_pixel_height[i];
    }
    prev_byte = byte;
  }
  for (i=0;i<n;i++)
  	SDL_FillRect(screen, &rect[i], foreground);
}


/*---------- X mouse support --------------*/

int mouse_x_size = 640, mouse_y_size = 240;
int mouse_sens = 3;
int mouse_last_x = -1, mouse_last_y = -1;
unsigned int mouse_last_buttons;
int mouse_old_style = 0;

void trs_get_mouse_pos(int *x, int *y, unsigned int *buttons)
{
  int win_x, win_y;
  Uint8 mask;
  
  mask = SDL_GetMouseState(&win_x, &win_y);
#if MOUSEDEBUG
  debug("get_mouse %d %d 0x%x ->", win_x, win_y, mask);
#endif
  if (win_x >= 0 && win_x < OrigWidth &&
      win_y >= 0 && win_y < OrigHeight) {
    /* Mouse is within emulator window */
    if (win_x < left_margin) win_x = left_margin;
    if (win_x >= OrigWidth - left_margin) win_x = OrigWidth - left_margin - 1;
    if (win_y < top_margin) win_y = top_margin;
    if (win_y >= OrigHeight - top_margin) win_y = OrigHeight - top_margin - 1;
    *x = mouse_last_x = (win_x - left_margin)
                        * mouse_x_size
                        / (OrigWidth - 2*left_margin);
    *y = mouse_last_y = (win_y - top_margin) 
                        * mouse_y_size
                        / (OrigHeight - 2*top_margin);
    mouse_last_buttons = 7;
    /* !!Note: assuming 3-button mouse */
    if (mask & SDL_BUTTON(1)) mouse_last_buttons &= ~4;
    if (mask & SDL_BUTTON(2)) mouse_last_buttons &= ~2;
    if (mask & SDL_BUTTON(3)) mouse_last_buttons &= ~1;
  }
  *x = mouse_last_x;
  *y = mouse_last_y;
  *buttons = mouse_last_buttons;
#if MOUSEDEBUG
  debug("%d %d 0x%x\n",
	  mouse_last_x, mouse_last_y, mouse_last_buttons);
#endif
}

void trs_set_mouse_pos(int x, int y)
{
  int dest_x, dest_y;
  if (x == mouse_last_x && y == mouse_last_y) {
    /* Kludge: Ignore warp if it says to move the mouse to where we
       last said it was. In general someone could really want to do that,
       but with MDRAW, gratuitous warps to the last location occur frequently.
    */
    return;
  }
  dest_x = left_margin + x * (OrigWidth - 2*left_margin) / mouse_x_size;
  dest_y = top_margin  + y * (OrigHeight - 2*top_margin) / mouse_y_size;

#if MOUSEDEBUG
  debug("set_mouse %d %d -> %d %d\n", x, y, dest_x, dest_y);
#endif
  SDL_WarpMouse(dest_x, dest_y);
}

void trs_get_mouse_max(int *x, int *y, unsigned int *sens)
{
  *x = mouse_x_size - (mouse_old_style ? 0 : 1);
  *y = mouse_y_size - (mouse_old_style ? 0 : 1);
  *sens = mouse_sens;
}

void trs_set_mouse_max(int x, int y, unsigned int sens)
{
  if ((x & 1) == 0 && (y & 1) == 0) {
    /* "Old style" mouse drivers took the size here; new style take
       the maximum. As a heuristic kludge, we assume old style if
       the values are even, new style if not. */
    mouse_old_style = 1;
  }
  mouse_x_size = x + (mouse_old_style ? 0 : 1);
  mouse_y_size = y + (mouse_old_style ? 0 : 1);
  mouse_sens = sens;
}

int trs_get_mouse_type()
{
  /* !!Note: assuming 3-button mouse */
  return 1;
}

void trs_main_save(FILE *file)
{
  int i;
  trs_save_int(file,&trs_model,1);
  trs_save_uchar(file,trs_screen,2048);
  trs_save_int(file,&screen_chars,1);
  trs_save_int(file,&col_chars,1);
  trs_save_int(file,&row_chars,1);
  trs_save_int(file,&currentmode,1);
  trs_save_int(file,&text80x24,1);
  trs_save_int(file,&screen640x240,1);
 trs_save_int(file,&trs_charset,1);
  for (i=0;i<G_YSIZE;i++)
    trs_save_uchar(file,grafyx_unscaled[i],G_XSIZE);
  trs_save_uchar(file,&grafyx_x,1);
  trs_save_uchar(file,&grafyx_y,1);
  trs_save_uchar(file,&grafyx_enable,1);
  trs_save_uchar(file,&grafyx_overlay,1);
  trs_save_uchar(file,&grafyx_xoffset,1);
  trs_save_uchar(file,&grafyx_yoffset,1);
  trs_save_uchar(file,&grafyx_x,1);
  trs_save_int(file,key_queue,KEY_QUEUE_SIZE);
  trs_save_int(file,&key_queue_head,1);
  trs_save_int(file,&key_queue_entries,1);
}

void trs_main_load(FILE *file)
{
  int i;
                    
  trs_load_int(file,&trs_model,1);
  trs_load_uchar(file,trs_screen,2048);
  trs_load_int(file,&screen_chars,1);
  trs_load_int(file,&col_chars,1);
  trs_load_int(file,&row_chars,1);
  trs_load_int(file,&currentmode,1);
  trs_load_int(file,&text80x24,1);
  trs_load_int(file,&screen640x240,1);
  trs_load_int(file,&trs_charset,1);
  for (i=0;i<G_YSIZE;i++)
    trs_load_uchar(file,grafyx_unscaled[i],G_XSIZE);
  trs_load_uchar(file,&grafyx_x,1);
  trs_load_uchar(file,&grafyx_y,1);
  trs_load_uchar(file,&grafyx_enable,1);
  trs_load_uchar(file,&grafyx_overlay,1);
  trs_load_uchar(file,&grafyx_xoffset,1);
  trs_load_uchar(file,&grafyx_yoffset,1);
  trs_load_uchar(file,&grafyx_x,1);
  trs_load_int(file,key_queue,KEY_QUEUE_SIZE);
  trs_load_int(file,&key_queue_head,1);
  trs_load_int(file,&key_queue_entries,1);
}

#ifdef ANDROID
void trs_main_init()
{
  int i;

  for (i = 0; i < 2048; i++) {
    trs_screen[i] = 0;
  };
  screen_chars = 1024;
  col_chars = 16;
  row_chars = 64;
  currentmode = NORMAL;
  text80x24 = 0;
  screen640x240 = 0;
  trs_charset = 3;
  key_queue_head = 0;
  key_queue_entries = 0;
}
#endif
