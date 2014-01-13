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
/* Copyright (c) 2000, Timothy Mann */
/* $Id: trs_hard.c,v 1.8 2009/06/15 23:40:47 mann Exp $ */

/* This software may be copied, modified, and used for any purpose
 * without fee, provided that (1) the above copyright notice is
 * retained, and (2) modified versions are clearly marked as having
 * been modified, with the modifier's name and the date included.  */

/*
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

/*
 * Emulation of the Radio Shack TRS-80 Model I/III/4/4P
 * hard disk controller.  This is a Western Digital WD1000/WD1010
 * mapped at ports 0xc8-0xcf, plus control registers at 0xc0-0xc1.
 */

#include <errno.h>
#include <string.h>
#include "trs.h"
#include "trs_hard.h"
#include "trs_state_save.h"
#include "trs_imp_exp.h"
#include "reed.h"

/*#define HARDDEBUG1 1*/  /* show detail on all port i/o */
/*#define HARDDEBUG2 1*/  /* show all commands */
/*#define HARDDEBUG3 1*/  /* show failure to open a drive */

/* Private types and data */

/* Structure describing one drive */
typedef struct {
  FILE* file;
  char filename[FILENAME_MAX];
  /* Values decoded from rhh */
  int writeprot;
  int cyls;  /* cyls per drive */
  int heads; /* tracks per cyl */
  int secs;  /* secs per track */
} Drive;

/* Structure describing controller state */
typedef struct {
  /* Controller present?  Yes if we have any drives, no if none */
  int present;

  /* Controller register images */
  Uchar control;
  Uchar data;
  Uchar error;
  Uchar seccnt;
  Uchar secnum;
  Ushort cyl;
  Uchar drive;
  Uchar head;
  Uchar status;
  Uchar command;

  /* Number of bytes already done in current read/write */
  int bytesdone;

  /* Drive geometries and files */
  Drive d[TRS_HARD_MAXDRIVES];
} State;

static State state;

/* Forward */
static int hard_data_in();
static void hard_data_out(int value);
static void hard_restore(int cmd);
static void hard_read(int cmd);
static void hard_write(int cmd);
static void hard_verify(int cmd);
static void hard_format(int cmd);
static void hard_init(int cmd);
static void hard_seek(int cmd);
static int open_drive(int drive);
static int find_sector(int newstatus);
static int open_drive(int n);
static void set_dir_cyl(int cyl);

/* Powerup or reset button */
void trs_hard_init(void)
{
  int i;
  state.control = 0;
  state.data = 0;
  state.error = 0;
  state.seccnt = 0;
  state.secnum = 0;
  state.cyl = 0;
  state.drive = 0;
  state.head = 0;
  state.status = 0;
  state.command = 0;
  for (i=0; i<TRS_HARD_MAXDRIVES; i++) {
    if (state.d[i].file == NULL) {
      state.d[i].writeprot = 0;
      state.d[i].cyls = 0;
      state.d[i].heads = 0;
      state.d[i].secs = 0;
    }
  }
}

void trs_hard_attach(int drive, char *diskname)
{
  if (state.d[drive].file != NULL)
    fclose(state.d[drive].file);
  strcpy(state.d[drive].filename, diskname);
  if (open_drive(drive) == 0) {
    trs_hard_remove(drive); 
  }
  trs_impexp_xtrshard_attach(drive, diskname);
}

void trs_hard_remove(int drive)
{
  if (state.d[drive].file != NULL)
    fclose(state.d[drive].file);
  trs_impexp_xtrshard_remove(drive);
  state.d[drive].filename[0] = 0;
  state.d[drive].file = NULL;
}

char* 
trs_hard_getfilename(int unit)
{
  return state.d[unit].filename;
}

int 
trs_hard_getwriteprotect(int unit)
{
  return state.d[unit].writeprot;
}

/* Read from an I/O port mapped to the controller */
int trs_hard_in(int port)
{
  int v;
  if (state.present) {
    switch (port) {
    case TRS_HARD_WP: {
      int i;
      v = 0;
      for (i=0; i<TRS_HARD_MAXDRIVES; i++) {
	open_drive(i);
	if (state.d[i].writeprot) {
	  v |= TRS_HARD_WPBIT(i) | TRS_HARD_WPSOME;
	}
      }
      break; }
    case TRS_HARD_CONTROL:
      v = state.control;
      break;
    case TRS_HARD_DATA:
      v = hard_data_in();
      break;
    case TRS_HARD_ERROR:
      v = state.error;
      break;
    case TRS_HARD_SECCNT:
      v = state.seccnt;
      break;
    case TRS_HARD_SECNUM:
      v = state.secnum;
      break;
    case TRS_HARD_CYLLO:
      v = state.cyl & 0xff;
      break;
    case TRS_HARD_CYLHI:
      v = (state.cyl >> 8) & 0xff;
      break;
    case TRS_HARD_SDH:
      v = (state.drive << 3) | state.head;
      break;
    case TRS_HARD_STATUS:
      v = state.status;
      break;
    default:
      v = 0xff;
      break;
    }
  } else {
    v = 0xff;
  }
#if HARDDEBUG1
  debug("%02x -> %02x\n", port, v);
#endif  
  return v;
}

/* Write to an I/O port mapped to the controller */
void trs_hard_out(int port, int value)
{
#if HARDDEBUG1
  debug("%02x <- %02x\n", port, value);
#endif  
  switch (port) {
  case TRS_HARD_WP:
    break;
  case TRS_HARD_CONTROL:
    if (value & TRS_HARD_SOFTWARE_RESET) {
      trs_hard_init();
    }
    if ((value & TRS_HARD_DEVICE_ENABLE) && state.present == 0) {
      int i;
      for (i=0; i<TRS_HARD_MAXDRIVES; i++) {
	if (open_drive(i)) state.present = 1;
      }
    }
    state.control = value;
    break;
  case TRS_HARD_DATA:
    hard_data_out(value);
    break;
  case TRS_HARD_PRECOMP:
    break;
  case TRS_HARD_SECCNT:
    state.seccnt = value;
    break;
  case TRS_HARD_SECNUM:
    state.secnum = value;
    break;
  case TRS_HARD_CYLLO:
    state.cyl = (state.cyl & 0xff00) | (value & 0x00ff);
    break;
  case TRS_HARD_CYLHI:
    state.cyl = (state.cyl & 0x00ff) | ((value << 8) & 0xff00);
    break;
  case TRS_HARD_SDH:
    if (value & TRS_HARD_SIZEMASK) {
      error("trs_hard: size bits set to nonzero value (0x%02x)",
	    value & TRS_HARD_SIZEMASK);
    }
    state.drive = (value & TRS_HARD_DRIVEMASK) >> TRS_HARD_DRIVESHIFT;
    state.head = (value & TRS_HARD_HEADMASK) >> TRS_HARD_HEADSHIFT;
#if 0
    if (!open_drive(state.drive)) state.status &= ~TRS_HARD_READY;
#else
    /* Ready, but perhaps not able!  This way seems to work better; it
     * avoids a long delay in the Model 4P boot ROM when there is no
     * unit 0. */
    state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE;
#endif
    break;

  case TRS_HARD_COMMAND:
    state.bytesdone = 0;
    state.command = value;
    switch (value & TRS_HARD_CMDMASK) {
    default:
      error("trs_hard: unknown command 0x%02x", value);
      break;
    case TRS_HARD_RESTORE:
      hard_restore(value);
      break;
    case TRS_HARD_READ:
      hard_read(value);
      break;
    case TRS_HARD_WRITE:
      hard_write(value);
      break;
    case TRS_HARD_VERIFY:
      hard_verify(value);
      break;
    case TRS_HARD_FORMAT:
      hard_format(value);
      break;
    case TRS_HARD_INIT:
      hard_init(value);
      break;
    case TRS_HARD_SEEK:
      hard_seek(value);
      break;
    }
    break;

  default:
    break;
  }
}

static void hard_restore(int cmd)
{
#if HARDDEBUG2
  debug("hard_restore drive %d\n", state.drive);
#endif
  state.cyl = 0;
  /*!! should anything else be zeroed? */
  state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE;
}

static void hard_read(int cmd)
{
#if HARDDEBUG2
  debug("hard_read drive %d cyl %d hd %d sec %d\n",
	state.drive, state.cyl, state.head, state.secnum);
#endif
  if (cmd & TRS_HARD_MULTI) {
    error("trs_hard: multi-sector read not supported (0x%02x)", cmd);
    state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_ERR;
    state.error = TRS_HARD_ABRTERR;
    return;
  }
  find_sector(TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_DRQ);
}

static void hard_write(int cmd)
{
#if HARDDEBUG2
  debug("hard_write drive %d cyl %d hd %d sec %d\n",
	state.drive, state.cyl, state.head, state.secnum);
#endif
  if (cmd & TRS_HARD_MULTI) {
    error("trs_hard: multi-sector write not supported (0x%02x)", cmd);
    state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_ERR;
    state.error = TRS_HARD_ABRTERR;
    return;
  }
  find_sector(TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_DRQ);
}

static void hard_verify(int cmd)
{
#if HARDDEBUG2
  debug("hard_verify drive %d cyl %d hd %d sec %d\n",
	state.drive, state.cyl, state.head, state.secnum);
#endif
  find_sector(TRS_HARD_READY | TRS_HARD_SEEKDONE);
}

static void hard_format(int cmd)
{
#if HARDDEBUG2
  debug("hard_format drive %d cyl %d hd %d\n",
	state.drive, state.cyl, state.head);
#endif
  if (state.seccnt != TRS_HARD_SEC_PER_TRK) {
    error("trs_hard: can only do %d sectors/track, not %d",
	  TRS_HARD_SEC_PER_TRK, state.seccnt);
  }
  if (state.secnum != TRS_HARD_SECSIZE_CODE) {
    error("trs_hard: can only do %d bytes/sectors (code %d), not code %d",
	  TRS_HARD_SECSIZE, TRS_HARD_SECSIZE_CODE, state.secnum);
  }
  /* !!should probably set up to read skew table here */
  state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE;
}

static void hard_init(int cmd)
{
#if HARDDEBUG2
  debug("hard_init drive %d cyl %d hd %d sec %d\n",
	state.drive, state.cyl, state.head, state.secnum);
#endif
  /* I don't know what this command does */
  error("trs_hard: init command (0x%02x) not implemented", cmd);
  state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE;
}

static void hard_seek(int cmd)
{
#if HARDDEBUG2
  debug("hard_seek drive %d cyl %d hd %d sec %d\n",
	state.drive, state.cyl, state.head, state.secnum);
#endif
  find_sector(TRS_HARD_READY | TRS_HARD_SEEKDONE);
}

/* 
 * 1) Make sure the file for the current drive is open.  If the file
 * cannot be opened, return 0 and set the controller error status.
 *
 * 2) If newly opening the file, establish the hardware write protect
 * status and geometry in the Drive structure.
 *
 * 3) Return 1 if all OK.
 */
static int open_drive(int drive)
{
  Drive *d = &state.d[drive];
  char diskname[1024];
  ReedHardHeader rhh;
  size_t res;

  if (d->file != NULL)
   return 1;
  if (d->filename[0] == 0)
    goto fail;
	
  /* First try opening for reading and writing */
  d->file = fopen(d->filename, "rb+");
  if (d->file == NULL) {
    /* No luck, try for reading only */
    d->file = fopen(d->filename, "rb");
    if (d->file == NULL) {
#if HARDDEBUG3
      error("trs_hard: could not open hard drive image %s: %s",
	    diskname, strerror(errno));
#endif
      goto fail;
    }
    d->writeprot = 1;
  } else {
    d->writeprot = 0;
  }

  /* Read in the Reed header and check some basic magic numbers (not all) */
  res = fread(&rhh, sizeof(rhh), 1, d->file);
  if (res != 1 || rhh.id1 != 0x56 || rhh.id2 != 0xcb || rhh.ver != 0x10) {
    error("trs_hard: unrecognized hard drive image %s", diskname);
    goto fail;
  }
  if (rhh.flag1 & 0x80) d->writeprot = 1;

  /* Use the number of cylinders specified in the header */
  d->cyls = rhh.cyl ? rhh.cyl : 256;

  /* Use the number of secs/track that RSHARD requires */
  d->secs = TRS_HARD_SEC_PER_TRK;

  /* Header gives only secs/cyl.  Compute number of heads from 
     this and the assumed number of secs/track. */
  d->heads = (rhh.sec ? rhh.sec : 256) / d->secs;

  if ((rhh.sec % d->secs) != 0 ||
      d->heads <= 0 || d->heads > TRS_HARD_MAXHEADS) {
    error("trs_hard: unusable geometry in image %s", diskname);
    goto fail;
  }

  state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE;
  return 1;

 fail:
  if (d->file) fclose(d->file);
  d->file = NULL;
  state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_ERR;
  state.error = TRS_HARD_NFERR;
  return 0;
}    

/*
 * Check whether the current position is in bounds for the geometry.
 * If not, return 0 and set the controller error status.  If so, fseek
 * the file to the start of the current sector, return 1, and set
 * the controller status to newstatus.
 */
static int find_sector(int newstatus)
{
  Drive *d = &state.d[state.drive];
  if (open_drive(state.drive) == 0) return 0;
  if (/**state.cyl >= d->cyls ||**/ /* ignore this limit */
      state.head >= d->heads ||
      state.secnum > d->secs /* allow 0-origin or 1-origin */ ) {
    error("trs_hard: requested cyl %d hd %d sec %d; max cyl %d hd %d sec %d\n",
	  state.cyl, state.head, state.secnum, d->cyls, d->heads, d->secs);
    state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_ERR;
    state.error = TRS_HARD_NFERR;
    return 0;
  }
  fseek(d->file,
	sizeof(ReedHardHeader) +
	TRS_HARD_SECSIZE * (state.cyl * d->heads * d->secs +
			    state.head * d->secs +
			    (state.secnum % d->secs)),
	0);
  state.status = newstatus;
  return 1;
}

static int hard_data_in()
{
  Drive *d = &state.d[state.drive];
  trs_hard_led(state.drive,1);
  if ((state.command & TRS_HARD_CMDMASK) == TRS_HARD_READ &&
      (state.status & TRS_HARD_ERR) == 0) {
    if (state.bytesdone < TRS_HARD_SECSIZE) {
      state.data = getc(d->file);
      state.bytesdone++;
    }
  }
  return state.data;
}

static void hard_data_out(int value)
{
  Drive *d = &state.d[state.drive];
  int res = 0;
  trs_hard_led(state.drive,1);
  state.data = value;
  if ((state.command & TRS_HARD_CMDMASK) == TRS_HARD_WRITE &&
      (state.status & TRS_HARD_ERR) == 0) {
    if (state.bytesdone < TRS_HARD_SECSIZE) {
      if (state.cyl == 0 && state.head == 0 &&
	  state.secnum == 0 && state.bytesdone == 2) {
	set_dir_cyl(value);
      }
      res = putc(state.data, d->file);
      state.bytesdone++;
      if (res != EOF && state.bytesdone == TRS_HARD_SECSIZE) {
	res = fflush(d->file);
      }
    }
  }
  if (res == EOF) {
    error("trs_hard: errno %d while writing drive %d", errno, state.drive);
    state.status = TRS_HARD_READY | TRS_HARD_SEEKDONE | TRS_HARD_ERR;
    state.error = TRS_HARD_DATAERR; /* arbitrary choice */
  }
}

/* Sleazy trick to update the "directory cylinder" byte in the Reed
   header.  This value is only needed by the Reed emulator itself, and
   we would like xtrs to set it automatically so that the user doesn't
   have to know about it. */
static void set_dir_cyl(int cyl)
{
  Drive *d = &state.d[state.drive];
  long where = ftell(d->file);
  fseek(d->file, 31, 0);
  putc(cyl, d->file);
  fseek(d->file, where, 0);
}

static void trs_save_harddrive(FILE *file, Drive *d)
{
  int one = 1;
  int zero = 0;

  if (d->file == NULL)
     trs_save_int(file, &zero, 1);
  else
     trs_save_int(file, &one, 1);
  trs_save_filename(file, d->filename);
  trs_save_int(file, &d->writeprot,1);
  trs_save_int(file, &d->cyls,1);
  trs_save_int(file, &d->heads,1);
  trs_save_int(file, &d->secs,1);
}

static void trs_load_harddrive(FILE *file, Drive *d)
{
  int file_not_null;
  
  trs_load_int(file,&file_not_null,1);
  if (file_not_null)
    d->file = (FILE *) 1;
  else
    d->file = NULL;
  trs_load_filename(file, d->filename);
  trs_load_int(file, &d->writeprot,1);
  trs_load_int(file, &d->cyls,1);
  trs_load_int(file, &d->heads,1);
  trs_load_int(file, &d->secs,1);
}

void trs_hard_save(FILE *file)
{
  int i;
  
  trs_save_int(file, &state.present, 1);
  trs_save_uchar(file, &state.control, 1);
  trs_save_uchar(file, &state.data, 1);
  trs_save_uchar(file, &state.error, 1);
  trs_save_uchar(file, &state.seccnt, 1);
  trs_save_uchar(file, &state.secnum, 1);
  trs_save_uint16(file, &state.cyl, 1);
  trs_save_uchar(file, &state.drive, 1);
  trs_save_uchar(file, &state.head, 1);
  trs_save_uchar(file, &state.status, 1);
  trs_save_uchar(file, &state.command, 1);
  trs_save_int(file, &state.bytesdone, 1);
  for (i=0;i<TRS_HARD_MAXDRIVES;i++)
    trs_save_harddrive(file, &state.d[i]);
}

void trs_hard_load(FILE *file)
{
  int i;
    
  for (i=0;i<TRS_HARD_MAXDRIVES;i++) {
    if (state.d[i].file != NULL) 
      fclose(state.d[i].file);
  }
  trs_load_int(file, &state.present, 1);
  trs_load_uchar(file, &state.control, 1);
  trs_load_uchar(file, &state.data, 1);
  trs_load_uchar(file, &state.error, 1);
  trs_load_uchar(file, &state.seccnt, 1);
  trs_load_uchar(file, &state.secnum, 1);
  trs_load_uint16(file, &state.cyl, 1);
  trs_load_uchar(file, &state.drive, 1);
  trs_load_uchar(file, &state.head, 1);
  trs_load_uchar(file, &state.status, 1);
  trs_load_uchar(file, &state.command, 1);
  trs_load_int(file, &state.bytesdone, 1);
  for (i=0;i<TRS_HARD_MAXDRIVES;i++) {
    trs_load_harddrive(file, &state.d[i]);
    if (state.d[i].file != NULL) {
      state.d[i].file = fopen(state.d[i].filename,"rb+");
      if (state.d[i].file == NULL) {
        state.d[i].file = fopen(state.d[i].filename,"rb");
        state.d[i].writeprot = 1;
      } else {
        state.d[i].writeprot = 0;
      }
    }
  }
}

