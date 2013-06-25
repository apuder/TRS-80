
#include <jni.h>
#include <android/log.h>
#include <stdlib.h>

#include "trs_iodefs.h"
#include "trs.h"
#include "z80.h"
#include "trs_disk.h"
#include "trs_uart.h"

#include "atrs.h"


#define DEBUG_TAG "TRS80"

#define NOT_IMPLEMENTED() \
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NOT_IMPLEMENTED: %s:%d", __FILE__, __LINE__); \
    exit(-1);

extern char trs_char_data[][MAXCHARS][TRS_CHAR_HEIGHT];

static int screen_chars = 1024;
static int row_chars = 64;
static int col_chars = 16;
static int resize = 0;
static int top_margin = 0;
static int left_margin = 0;
static int border_width = 2;

static int hrg_enable = 0;

unsigned char grafyx_microlabs = 0;
unsigned char grafyx_x = 0, grafyx_y = 0, grafyx_mode = 0;
unsigned char grafyx_enable = 0;
unsigned char grafyx_overlay = 0;
unsigned char grafyx_xoffset = 0, grafyx_yoffset = 0;

/* Port 0xFF (grafyx_m3_mode) bits */
#define G3_COORD    0x80
#define G3_ENABLE   0x40
#define G3_COMMAND  0x20
#define G3_YLOW(v)  (((v)&0x1e)>>1)


int trs_screen_batched = 0;

void trs_screen_batch()
{
#if BATCH
  /* Defer screen updates until trs_screen_unbatch, then redraw screen
     if anything changed.  Unfortunately, this seems to slow things
     down, so it's disabled.  Probably what we should really be doing
     is rendering into an offscreen buffer when trs_screen_batched is
     set, then copying to the real screen in trs_screen_unbatch.  Also
     (and orthogonally) we should probably be keeping track of what
     part of the screen changed and only redrawing that part. */
  trs_screen_batched = 1;
#endif
}

void trs_screen_unbatch()
{
#if BATCH
  if (trs_screen_batched > 1) {
    trs_screen_batched = 0;
    trs_screen_refresh();
  } else {
    trs_screen_batched = 0;
  }
#endif
}

/*
 * show help
 */

void trs_show_help()
{
	NOT_IMPLEMENTED();
}

/* exits if something really bad happens */
void trs_screen_init()
{
}


/*
 * Flush output to X server
 */
inline void trs_x_flush()
{
	NOT_IMPLEMENTED();
}

/* 
 * Get and process X event(s).
 *   If wait is true, process one event, blocking until one is available.
 *   If wait is false, process as many events as are available, returning
 *     when none are left.
 * Handle interrupt-driven uart input here too.
 */ 
void trs_get_event(int wait)
{
}

void trs_screen_expanded(int flag)
{
}

void trs_screen_inverse(int flag)
{
	NOT_IMPLEMENTED();
}

void trs_screen_alternate(int flag)
{
}

void trs_screen_80x24(int flag)
{
	NOT_IMPLEMENTED();
}

void screen_init()
{
	NOT_IMPLEMENTED();
}

void
boxes_init(int foreground, int background, int width, int height, int expanded)
{
	NOT_IMPLEMENTED();
}


void trs_screen_refresh()
{
	NOT_IMPLEMENTED();
}

void trs_screen_write_char(int position, int char_index)
{
	trs_screen[position] = char_index;
#ifdef ANDROID_BATCHED_SCREEN_UPDATE
	instructionsSinceLastScreenAccess = 0;
	screenWasUpdated = 1;
#endif
}

 /* Copy lines 1 through col_chars-1 to lines 0 through col_chars-2.
    Doesn't need to clear line col_chars-1. */
void trs_screen_scroll()
{
	  int i = 0;

	  for (i = row_chars; i < screen_chars; i++)
	    trs_screen[i-row_chars] = trs_screen[i];

	  if (trs_screen_batched) {
	    trs_screen_batched++;
	    return;
	  }
	  if (grafyx_enable) {
	    if (grafyx_overlay) {
	      trs_screen_refresh();
	    }
	  } else if (hrg_enable) {
	    trs_screen_refresh();
	  } else {
#ifdef ANDROID
#ifdef ANDROID_BATCHED_SCREEN_UPDATE
		  screenWasUpdated = 1;
		  instructionsSinceLastScreenAccess = SCREEN_UPDATE_THRESHOLD;
#endif
#else
	    XCopyArea(display,window,window,gc,
	              left_margin,cur_char_height+top_margin,
	              (cur_char_width*row_chars),(cur_char_height*col_chars),
	              left_margin,top_margin);
#endif
	  }
}

void grafyx_write_byte(int x, int y, char byte)
{
}

void grafyx_write_x(int value)
{
	NOT_IMPLEMENTED();
}

void grafyx_write_y(int value)
{
	NOT_IMPLEMENTED();
}

void grafyx_write_data(int value)
{
	NOT_IMPLEMENTED();
}

int grafyx_read_data()
{
	NOT_IMPLEMENTED();
	return 0;
}

void grafyx_write_mode(int value)
{
}

void grafyx_write_xoffset(int value)
{
	NOT_IMPLEMENTED();
}

void grafyx_write_yoffset(int value)
{
	NOT_IMPLEMENTED();
}

void grafyx_write_overlay(int value)
{
	NOT_IMPLEMENTED();
}

int grafyx_get_microlabs()
{
  return 0;
}

void grafyx_set_microlabs(int on_off)
{
}

/* Model III MicroLabs support */
void grafyx_m3_reset()
{
}

void grafyx_m3_write_mode(int value)
{
	NOT_IMPLEMENTED();
}

int grafyx_m3_write_byte(int position, int byte)
{
#if 0
	  if (grafyx_microlabs && (grafyx_mode & G3_COORD)) {
	    int x = (position % 64);
	    int y = (position / 64) * 12 + grafyx_y;
	    grafyx_write_byte(x, y, byte);
	    return 1;
	  } else {
#endif
	    return 0;
#if 0
	  }
#endif
}

unsigned char grafyx_m3_read_byte(int position)
{
#if 0
	  if (grafyx_microlabs && (grafyx_mode & G3_COORD)) {
	    int x = (position % 64);
	    int y = (position / 64) * 12 + grafyx_y;
	    return grafyx_unscaled[y][x];
	  } else {
#endif
	    return trs_screen[position];
#if 0
	  }
#endif
}

int grafyx_m3_active()
{
	return (trs_model == 3 && grafyx_microlabs && (grafyx_mode & G3_COORD));
}

/* Switch HRG on (1) or off (0). */
void
hrg_onoff(int enable)
{
	NOT_IMPLEMENTED();
}

/* Write address to latch. */
void
hrg_write_addr(int addr, int mask)
{
	NOT_IMPLEMENTED();
}

/* Write byte to HRG memory. */
void
hrg_write_data(int data)
{
	NOT_IMPLEMENTED();
}

/* Read byte from HRG memory. */
int
hrg_read_data()
{
	NOT_IMPLEMENTED();
  return 0;
}

void trs_get_mouse_pos(int *x, int *y, unsigned int *buttons)
{
	NOT_IMPLEMENTED();
}

void trs_set_mouse_pos(int x, int y)
{
	NOT_IMPLEMENTED();
}

void trs_get_mouse_max(int *x, int *y, unsigned int *sens)
{
	NOT_IMPLEMENTED();
}

void trs_set_mouse_max(int x, int y, unsigned int sens)
{
	NOT_IMPLEMENTED();
}

int trs_get_mouse_type()
{
	NOT_IMPLEMENTED();
  /* !!Note: assuming 3-button mouse */
  return 1;
}
