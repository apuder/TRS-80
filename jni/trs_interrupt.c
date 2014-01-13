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
/* Copyright (c) 1996, Timothy Mann */
/* $Id: trs_interrupt.c,v 1.27 2008/06/26 04:39:56 mann Exp $ */

/* This software may be copied, modified, and used for any purpose
 * without fee, provided that (1) the above copyright notice is
 * retained, and (2) modified versions are clearly marked as having
 * been modified, with the modifier's name and the date included.  */

/*
 * Emulate interrupts
 */

#include "z80.h"
#include "trs.h"
#include "trs_state_save.h"
#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include <SDL/SDL.h>

/*#define IDEBUG 1*/
/*#define IDEBUG2 1*/

/* IRQs */
#define M1_TIMER_BIT    0x80
#define M1_DISK_BIT     0x40
#define M3_UART_ERR_BIT 0x40
#define M3_UART_RCV_BIT 0x20
#define M3_UART_SND_BIT 0x10
#define M3_IOBUS_BIT    0x80 /* not emulated */
#define M3_TIMER_BIT    0x04
#define M3_CASSFALL_BIT 0x02
#define M3_CASSRISE_BIT 0x01
static unsigned char interrupt_latch = 0;
static unsigned char interrupt_mask = 0;

/* NMIs (M3/4/4P only) */
#define M3_INTRQ_BIT    0x80  /* FDC chip INTRQ line */
#define M3_MOTOROFF_BIT 0x40  /* FDC motor timed out (stopped) */
#define M3_RESET_BIT    0x20  /* User pressed Reset button */
static unsigned char nmi_latch = 1; /* ?? One diagnostic program needs this */
static unsigned char nmi_mask = M3_RESET_BIT;

#define TIMER_HZ_1 40
#define TIMER_HZ_3 30
#define TIMER_HZ_4 60
int timer_hz;
int timer_overclock = 0;
int timer_overclock_rate = 5;
unsigned int cycles_per_timer;

#define CLOCK_MHZ_1 1.77408
#define CLOCK_MHZ_3 2.02752
#define CLOCK_MHZ_4 4.05504

/* Kludge: LDOS hides the date (not time) in a memory area across reboots. */
/* We put it there on powerup, so LDOS magically knows the date! */
#define LDOS_MONTH 0x4306
#define LDOS_DAY   0x4307
#define LDOS_YEAR  0x4466
#define LDOS3_MONTH 0x442f
#define LDOS3_DAY   0x4457
#define LDOS3_YEAR  0x4413
#define LDOS4_MONTH 0x0035
#define LDOS4_DAY   0x0034
#define LDOS4_YEAR  0x0033

/* Kludge, continued: On NEWDOS/80, both date and time are stored in memory
   across reboots, but a test is done on boot to decide whether to use the
   stored values.  Here's how it works: NEWDOS/80 writes a special byte value
   to the memory address right before the stored date and time.  On reboot,
   this address is checked, and if it contains that special byte, the stored
   date and time are considered valid and are therefore used.

   By putting this info in memory on powerup, NEWDOS/80 gets initialized
   with the system date and time.
 */
#define NEWDOS_DATETIME_VALID_BYTE  0xa5
// Model 1
#define NEWDOS_DATETIME_VALID_ADDR  0x43ab
#define NEWDOS_MONTH                0x43b1
#define NEWDOS_DAY                  0x43b0
#define NEWDOS_YEAR                 0x43af
#define NEWDOS_HOUR                 0x43ae
#define NEWDOS_MIN                  0x43ad
#define NEWDOS_SEC                  0x43ac
// Model 3
#define NEWDOS3_DATETIME_VALID_ADDR 0x42cb
#define NEWDOS3_MONTH               0x42d1
#define NEWDOS3_DAY                 0x42d0
#define NEWDOS3_YEAR                0x42cf
#define NEWDOS3_HOUR                0x42ce
#define NEWDOS3_MIN                 0x42cd
#define NEWDOS3_SEC                 0x42cc

static int timer_on = 1;
#ifdef IDEBUG
long lost_timer_interrupts = 0;
#endif

/* Note: the independent interrupt latch and mask model is not correct
   for all interrupts.  The cassette rise/fall interrupt enable is
   clocked into the interrupt latch when the event occurs (we get this
   right), and is *not* masked against the latch output (we get this
   wrong, but it doesn't really matter). */

void
trs_cassette_rise_interrupt(int dummy)
{
  interrupt_latch = (interrupt_latch & ~M3_CASSRISE_BIT) |
    (interrupt_mask & M3_CASSRISE_BIT);
  z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  trs_cassette_update(0);
}

void
trs_cassette_fall_interrupt(int dummy)
{
  interrupt_latch = (interrupt_latch & ~M3_CASSFALL_BIT) |
    (interrupt_mask & M3_CASSFALL_BIT);
  z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  trs_cassette_update(0);
}

void
trs_cassette_clear_interrupts()
{
  interrupt_latch &= ~(M3_CASSRISE_BIT|M3_CASSFALL_BIT);
  z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
}

int
trs_cassette_interrupts_enabled()
{
  return interrupt_mask & (M3_CASSRISE_BIT|M3_CASSFALL_BIT);
}

int
trs_timer_is_turbo()
{
    return(timer_overclock);
}

int
trs_timer_switch_turbo()
{
    timer_overclock = !timer_overclock;
#ifdef MACOSX	
	SetControlManagerTurboMode(timer_overclock);
#endif
    return(timer_overclock);
}

void
trs_timer_interrupt(int state)
{
  if (trs_model == 1) {
    if (state) {
#ifdef IDEBUG
      if (interrupt_latch & M1_TIMER_BIT) lost_timer_interrupts++;
#endif
      interrupt_latch |= M1_TIMER_BIT;
      z80_state.irq = 1;
    } else {
      interrupt_latch &= ~M1_TIMER_BIT;
    }
  } else {
    if (state) {
#ifdef IDEBUG
      if (interrupt_latch & M3_TIMER_BIT) lost_timer_interrupts++;
#endif
      interrupt_latch |= M3_TIMER_BIT;
    } else {
      interrupt_latch &= ~M3_TIMER_BIT;
    }
    z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  }
}

void
trs_disk_intrq_interrupt(int state)
{
  if (trs_model == 1) {
    if (state) {
      interrupt_latch |= M1_DISK_BIT;
      z80_state.irq = 1;
    } else {
      interrupt_latch &= ~M1_DISK_BIT;
    }
  } else {
    if (state) {
      nmi_latch |= M3_INTRQ_BIT;
    } else {
      nmi_latch &= ~M3_INTRQ_BIT;
    }
    z80_state.nmi = (nmi_latch & nmi_mask) != 0;
    if (!z80_state.nmi) z80_state.nmi_seen = 0;
  }
}

void
trs_disk_motoroff_interrupt(int state)
{
  /* Drive motor timed out (stopped). */
  if (trs_model == 1) {
    /* no such interrupt */
  } else {
    if (state) {
      nmi_latch |= M3_MOTOROFF_BIT;
    } else {
      nmi_latch &= ~M3_MOTOROFF_BIT;
    }
    z80_state.nmi = (nmi_latch & nmi_mask) != 0;
    if (!z80_state.nmi) z80_state.nmi_seen = 0;
  }
}

void
trs_disk_drq_interrupt(int state)
{
  /* no effect */
}

void
trs_uart_err_interrupt(int state)
{
  if (trs_model > 1) {
    if (state) {
      interrupt_latch |= M3_UART_ERR_BIT;
    } else {
      interrupt_latch &= ~M3_UART_ERR_BIT;
    }
    z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  }
}

void
trs_uart_rcv_interrupt(int state)
{
  if (trs_model > 1) {
    if (state) {
      interrupt_latch |= M3_UART_RCV_BIT;
    } else {
      interrupt_latch &= ~M3_UART_RCV_BIT;
    }
    z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  }
}

void
trs_uart_snd_interrupt(int state)
{
  if (trs_model > 1) {
    if (state) {
      interrupt_latch |= M3_UART_SND_BIT;
    } else {
      interrupt_latch &= ~M3_UART_SND_BIT;
    }
    z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
  }
}

void
trs_reset_button_interrupt(int state)
{
  if (trs_model == 1) {
    z80_state.nmi = state;
  } else {  
    if (state) {
      nmi_latch |= M3_RESET_BIT;
    } else {
      nmi_latch &= ~M3_RESET_BIT;
    }
    z80_state.nmi = (nmi_latch & nmi_mask) != 0;
  }
  if (!z80_state.nmi) z80_state.nmi_seen = 0;
}

unsigned char
trs_interrupt_latch_read()
{
  unsigned char tmp = interrupt_latch;
  if (trs_model == 1) {
    trs_timer_interrupt(0); /* acknowledge this one (only) */
    z80_state.irq = (interrupt_latch != 0);
    return tmp;
  } else {
    return ~tmp;
  }
}

void
trs_interrupt_mask_write(unsigned char value)
{
  interrupt_mask = value;
  z80_state.irq = (interrupt_latch & interrupt_mask) != 0;
}

/* M3 only */
unsigned char
trs_nmi_latch_read()
{
  return ~nmi_latch;
}

void
trs_nmi_mask_write(unsigned char value)
{
  nmi_mask = value | M3_RESET_BIT;
  z80_state.nmi = (nmi_latch & nmi_mask) != 0;
#if IDEBUG2
  if (z80_state.nmi && !z80_state.nmi_seen) {
    debug("mask write caused nmi, mask %02x latch %02x\n",
	  nmi_mask, nmi_latch);
  }
#endif
  if (!z80_state.nmi) z80_state.nmi_seen = 0;
}

static int saved_delay;

/* Temporarily reduce the delay, until trs_restore_delay is called.
   Useful if we know we're about to do something that's emulated more
   slowly than most instructions, such as video or real-time sound.
   In case the boost is too big or too small, we allow the normal
   autodelay algorithm to continue to run and adjust the new delay. */
void
trs_suspend_delay()
{
  if (!saved_delay) {
    saved_delay = z80_state.delay;
    z80_state.delay /= 2;  /* dividing by 2 is arbitrary */
  }
}

/* Put back the saved delay */
void
trs_restore_delay()
{
  if (saved_delay) {
    z80_state.delay = saved_delay;
    saved_delay = 0;
  }
}

#define UP_F   1.50
#define DOWN_F 0.50 

void
trs_timer_event(void)
{
  if (timer_on) {
    trs_timer_interrupt(1); /* generate */
    trs_disk_motoroff_interrupt(trs_disk_motoroff());
    trs_kb_heartbeat(); /* part of keyboard stretch kludge */
  }
}

void trs_timer_sync_with_host(void)
{
	Uint32 curtime;
	Uint32 deltatime;
    static Uint32 lasttime = 0;

    if (timer_overclock) {
        deltatime = 1000 / (timer_overclock_rate * timer_hz);
    } else {
        deltatime = 1000 / timer_hz;
    }

	curtime = SDL_GetTicks();

	if (lasttime + deltatime > curtime) {
		SDL_Delay(lasttime + deltatime - curtime);
    }
	curtime = SDL_GetTicks();

	lasttime += deltatime;
	if ((lasttime + deltatime) < curtime)
		lasttime = curtime;
    
    trs_disk_led(0,0);
    trs_hard_led(0,0);
    trs_timer_event();
}

void
trs_timer_init()
{
  struct tm *lt;
  time_t tt;

  if (trs_model == 1) {
      timer_hz = TIMER_HZ_1;
      z80_state.clockMHz = CLOCK_MHZ_1;
  } else {
      /* initially... */
      timer_hz = TIMER_HZ_3;  
      z80_state.clockMHz = CLOCK_MHZ_3;
  }
  cycles_per_timer = z80_state.clockMHz * 1000000 / timer_hz;
  
  trs_timer_event();

  /* Also initialize the clock in memory - hack */
  tt = time(NULL);
  lt = localtime(&tt);
  if (trs_model == 1) {
      mem_write(LDOS_MONTH, (lt->tm_mon + 1) ^ 0x50);
      mem_write(LDOS_DAY, lt->tm_mday);
      mem_write(LDOS_YEAR, lt->tm_year - 80);

      mem_write(NEWDOS_DATETIME_VALID_ADDR, NEWDOS_DATETIME_VALID_BYTE);
      mem_write(NEWDOS_MONTH, lt->tm_mon + 1);
      mem_write(NEWDOS_DAY, lt->tm_mday);
      mem_write(NEWDOS_YEAR, lt->tm_year % 100);
      mem_write(NEWDOS_HOUR, lt->tm_hour);
      mem_write(NEWDOS_MIN, lt->tm_min);
      mem_write(NEWDOS_SEC, lt->tm_sec);
  } else {
      mem_write(LDOS3_MONTH, (lt->tm_mon + 1) ^ 0x50);
      mem_write(LDOS3_DAY, lt->tm_mday);
      mem_write(LDOS3_YEAR, lt->tm_year - 80);

      mem_write(NEWDOS3_DATETIME_VALID_ADDR, NEWDOS_DATETIME_VALID_BYTE);
      mem_write(NEWDOS3_MONTH, lt->tm_mon + 1);
      mem_write(NEWDOS3_DAY, lt->tm_mday);
      mem_write(NEWDOS3_YEAR, lt->tm_year % 100);
      mem_write(NEWDOS3_HOUR, lt->tm_hour);
      mem_write(NEWDOS3_MIN, lt->tm_min);
      mem_write(NEWDOS3_SEC, lt->tm_sec);

      if (trs_model >= 4) {
        extern Uchar memory[];
	memory[LDOS4_MONTH] = lt->tm_mon + 1;
	memory[LDOS4_DAY] = lt->tm_mday;
	memory[LDOS4_YEAR] = lt->tm_year;
      }
  }
}

void
trs_timer_off()
{
  timer_on = 0;
}

void
trs_timer_on()
{
  if (!timer_on) {
    timer_on = 1;
    trs_timer_event();
  }
}

void
trs_timer_speed(int fast)
{
    if (trs_model >= 4) {
	timer_hz = fast ? TIMER_HZ_4 : TIMER_HZ_3;
	z80_state.clockMHz = fast ? CLOCK_MHZ_4 : CLOCK_MHZ_3;
    } else if (trs_model == 1) {
        /* Typical 2x clock speedup kit */
        z80_state.clockMHz = CLOCK_MHZ_1 * ((fast&1) + 1);
    }
    cycles_per_timer = z80_state.clockMHz * 1000000 / timer_hz;
}

static trs_event_func event_func = NULL;
static int event_arg;

/* Schedule an event to occur after "countdown" more t-states have
 *  executed.  0 makes the event happen immediately -- that is, at
 *  the end of the current instruction, but before the emulator checks
 *  for interrupts.  It is legal for an event function to call 
 *  trs_schedule_event.  
 *
 * Only one event can be buffered.  If you try to schedule a second
 *  event while one is still pending, the pending event (along with
 *  any further events that it schedules) is executed immediately.
 */
void
trs_schedule_event(trs_event_func f, int arg, int countdown)
{
    while (event_func) {
#if EDEBUG	
	error("warning: trying to schedule two events");
#endif
	trs_do_event();
    }
    event_func = f;
    event_arg = arg;
    z80_state.sched = z80_state.t_count + (tstate_t) countdown;
    if (z80_state.sched == 0) z80_state.sched--;
}

/*
 * If an event is scheduled, do it now.  (If the event function
 * schedules a new event, however, leave that one pending.)
 */
void
trs_do_event()
{
    trs_event_func f = event_func;
    if (f) {
	event_func = NULL;
	z80_state.sched = 0;
	f(event_arg);    
    }
}

/*
 * Cancel scheduled event, if any.
 */
void
trs_cancel_event()
{
    event_func = NULL;
    z80_state.sched = 0;
}

/*
 * Check event scheduled
 */
trs_event_func
trs_event_scheduled()
{
    return event_func;
}

void trs_interrupt_save(FILE *file)
{
  int event;
  
  trs_save_uchar(file, &interrupt_latch, 1);
  trs_save_uchar(file, &interrupt_mask, 1);
  trs_save_uchar(file, &nmi_latch, 1);
  trs_save_int(file, &timer_hz, 1);
  trs_save_uint32(file, &cycles_per_timer, 1);
  trs_save_int(file, &timer_on, 1);
  trs_save_int(file, &saved_delay, 1);
  if (event_func == (trs_event_func) assert_state)
    event = 1;
  else if (event_func == transition_out)
    event = 2;
  else if (event_func == trs_cassette_kickoff)
    event = 3;
  else if (event_func == orch90_flush)
    event = 4;
  else if (event_func == trs_cassette_fall_interrupt)
    event = 5;
  else if (event_func == trs_cassette_rise_interrupt)
    event = 6;
  else if (event_func == trs_cassette_update)
    event = 7;
  else if (event_func == trs_disk_lostdata)
    event = 8;
  else if (event_func == trs_disk_done)
    event = 9;
  else if (event_func == trs_disk_firstdrq)
    event = 10;
  else if (event_func == trs_reset_button_interrupt)
    event = 11;
  else if (event_func == trs_uart_set_avail)
    event = 12;
  else if (event_func == trs_uart_set_empty)
    event = 13;
  else
    event = 0;
  trs_save_int(file, &event, 1);
  trs_save_int(file, &event_arg, 1);
}

void trs_interrupt_load(FILE *file)
{
  int event;
  
  trs_load_uchar(file, &interrupt_latch, 1);
  trs_load_uchar(file, &interrupt_mask, 1);
  trs_load_uchar(file, &nmi_latch, 1);
  trs_load_int(file, &timer_hz, 1);
  trs_load_uint32(file, &cycles_per_timer, 1);
  trs_load_int(file, &timer_on, 1);
  trs_load_int(file, &saved_delay, 1);
  trs_load_int(file, &event, 1);
  switch(event) {
  case 1:
    event_func = (trs_event_func) assert_state;
    break;
  case 2:
    event_func = transition_out;
    break;
  case 3:
    event_func = trs_cassette_kickoff;
    break;
  case 4:
    event_func = orch90_flush;
    break;
  case 5:
    event_func = trs_cassette_fall_interrupt;
    break;
  case 6:
    event_func = trs_cassette_rise_interrupt;
    break;
  case 7:
    event_func = trs_cassette_update;
    break;
  case 8:
    event_func = trs_disk_lostdata;
    break;
  case 9:
    event_func = trs_disk_done;
    break;
  case 10:
    event_func = trs_disk_firstdrq;
    break;
  case 11:
    event_func = trs_reset_button_interrupt;
    break;
  case 12:
    event_func = trs_uart_set_avail;
    break;
  case 13:
    event_func = trs_uart_set_empty;
    break;
  default:
    event_func = NULL;
    break;
  }
  trs_load_int(file, &event_arg, 1);
}

