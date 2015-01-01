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
/* $Id: trs_imp_exp.c,v 1.19 2008/06/26 04:39:56 mann Exp $ */

/* This software may be copied, modified, and used for any purpose
 * without fee, provided that (1) the above copyright notice is
 * retained, and (2) modified versions are clearly marked as having
 * been modified, with the modifier's name and the date included.  */

/*
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

/*
 * trs_imp_exp.c
 *
 * Features to make transferring files into and out of the emulator
 *  easier.  
 */

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <time.h>
#include <dirent.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include "trs_imp_exp.h"
#include "z80.h"
#include "trs.h"
#include "trs_disk.h"
#include "trs_hard.h"
#include "trs_state_save.h"

/*
   If the following option is set, potentially dangerous emulator traps
   will be blocked, including file writes to the host filesystem and shell
   command execution.
 */
int trs_emtsafe = 0;

/* New emulator traps */
typedef struct {
  DIR *dir;
  char pathname[FILENAME_MAX];
} OpenDir;

#define MAX_OPENDIR 32
OpenDir dir[MAX_OPENDIR];

typedef struct {
  int fd;
  int inuse;
  int oflag;
  int xtrshard;
  int xtrshard_unit;
  char filename[FILENAME_MAX];
} OpenDisk;

#define MAX_OPENDISK 32
OpenDisk od[MAX_OPENDISK];
int xtrshard_fd[4] = {-1,-1,-1,-1};

void do_emt_system()
{
  int res;
  if (trs_emtsafe) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
  res = system((char *)mem_pointer(REG_HL, 0));
  if (res == -1) {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  } else {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  }
  REG_BC = res;
}

void do_emt_mouse()
{
  int x, y;
  unsigned int buttons, sens;

  trs_emu_mouse = TRUE;

  switch (REG_B) {
  case 1:
    trs_get_mouse_pos(&x, &y, &buttons);
    REG_HL = x;
    REG_DE = y;
    REG_A = buttons;
    if (REG_A) {
      REG_F &= ~ZERO_MASK;
    } else {
      REG_F |= ZERO_MASK;
    }
    break;
  case 2:
    trs_set_mouse_pos(REG_HL, REG_DE);
    REG_A = 0;
    REG_F |= ZERO_MASK;
    break;
  case 3:
    trs_get_mouse_max(&x, &y, &sens);
    REG_HL = x;
    REG_DE = y;
    REG_A = sens;
    if (REG_A) {
      REG_F &= ~ZERO_MASK;
    } else {
      REG_F |= ZERO_MASK;
    }
    break;
  case 4:
    trs_set_mouse_max(REG_HL, REG_DE, REG_C);
    REG_A = 0;
    REG_F |= ZERO_MASK;
    break;
  case 5:
    REG_A = trs_get_mouse_type();
    if (REG_A) {
      REG_F &= ~ZERO_MASK;
    } else {
      REG_F |= ZERO_MASK;
    }
    break;
  default:
    error("undefined emt_mouse function code");
    break;
  }
}

void do_emt_getddir()
{
  if (REG_HL + REG_BC > 0x10000 ||
      REG_HL + strlen(trs_disk_dir) + 1 > REG_HL + REG_BC) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  strcpy((char *)mem_pointer(REG_HL, 1), trs_disk_dir);
  REG_A = 0;
  REG_F |= ZERO_MASK;
  REG_BC = strlen(trs_disk_dir);
}

void do_emt_setddir()
{
  if (trs_emtsafe) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
  strcpy(trs_disk_dir,(char *)mem_pointer(REG_HL, 0));
  if (trs_disk_dir[0] == '~' &&
#ifdef _WIN32  
      (trs_disk_dir[1] == '\\' || trs_disk_dir[1] == '\0')) {
#else
      (trs_disk_dir[1] == '/' || trs_disk_dir[1] == '\0')) {
#endif                       
    char* home = getenv("HOME");
    if (home) {
#ifdef _WIN32              
      sprintf(trs_disk_dir, "%s\\%s", home, trs_disk_dir+1);
#else
      sprintf(trs_disk_dir, "%s/%s", home, trs_disk_dir+1);
#endif      
    }
  }
  REG_A = 0;
  REG_F |= ZERO_MASK;
}

void do_emt_open()
{
  int fd, oflag, eoflag;
  eoflag = REG_BC;
  switch (eoflag & EO_ACCMODE) {
  case EO_RDONLY:
  default:
    oflag = O_RDONLY;
    break;
  case EO_WRONLY:
    oflag = O_WRONLY;
    break;
  case EO_RDWR:
    oflag = O_RDWR;
    break;
  }
  if (eoflag & EO_CREAT)  oflag |= O_CREAT;
  if (eoflag & EO_EXCL)   oflag |= O_EXCL;
  if (eoflag & EO_TRUNC)  oflag |= O_TRUNC;
  if (eoflag & EO_APPEND) oflag |= O_APPEND;

  if (trs_emtsafe && oflag != O_RDONLY) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
  fd = open((char *)mem_pointer(REG_HL, 0), oflag, REG_DE);
  if (fd >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
  REG_DE = fd;
}

void do_emt_close()
{
  int res;
  res = close(REG_DE);
  if (res >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
}

void do_emt_read()
{
  int size;
  int i;
  
  if (REG_HL + REG_BC > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  for (i=0;i<3;i++) {
    if (REG_DE == xtrshard_fd[i])
      trs_hard_led(i,1);
  }
  size = read(REG_DE, mem_pointer(REG_HL, 1), REG_BC);
  if (size >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
  REG_BC = size;
}


void do_emt_write()
{
  int size;
  int i;
  
  if (trs_emtsafe) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
 if (REG_HL + REG_BC > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  for (i=0;i<3;i++) {
    if (REG_DE == xtrshard_fd[i])
      trs_hard_led(i,1);
  }
  size = write(REG_DE, mem_pointer(REG_HL, 0), REG_BC);
  if (size >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
  REG_BC = size;
}

void do_emt_lseek()
{
  int i;
  off_t offset;
  if (REG_HL + 8 > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    return;
  }
  offset = 0;
  for (i=0; i<8; i++) {
    offset = offset + (mem_read(REG_HL + i) << i*8);
  }
  offset = lseek(REG_DE, offset, REG_BC);
  if (offset != (off_t) -1) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
  for (i=REG_HL; i<8; i++) {
    mem_write(REG_HL + i, offset & 0xff);
    offset >>= 8;
  }
}

void do_emt_strerror()
{
  char *msg;
  int size;
  if (REG_HL + REG_BC > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  errno = 0;
  msg = strerror(REG_A);
  size = strlen(msg);
  if (errno != 0) {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  } else if (REG_BC < size + 2) {
    REG_A = ERANGE;
    REG_F &= ~ZERO_MASK;
    size = REG_BC - 1;
  } else {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  }
  memcpy(mem_pointer(REG_HL, 1), msg, size);
  mem_write(REG_HL + size++, '\r');
  mem_write(REG_HL + size, '\0');
  if (errno == 0) {
    REG_BC = size;
  } else {
    REG_BC = 0xFFFF;
  }
}

void do_emt_time()
{
  time_t now = time(0);
  if (REG_A == 1) {
#if __alpha
    struct tm *loctm = localtime(&now);
    now += loctm->tm_gmtoff;
#else
    struct tm loctm = *(localtime(&now));
    struct tm gmtm = *(gmtime(&now));
    int daydiff = loctm.tm_mday - gmtm.tm_mday;
    now += (loctm.tm_sec - gmtm.tm_sec)
      + (loctm.tm_min - gmtm.tm_min) * 60
      + (loctm.tm_hour - gmtm.tm_hour) * 3600;
    switch (daydiff) {
    case 0:
    case 1:
    case -1:
      now += 24*3600 * daydiff;
      break;
    case 30:
    case 29:
    case 28:
    case 27:
      now -= 24*3600;
      break;
    case -30:
    case -29:
    case -28:
    case -27:
      now += 24*3600;
      break;
    default:
      error("trouble computing local time in emt_time");
    }
#endif
  } else if (REG_A != 0) {
    error("unsupported function code to emt_time");
  }
  REG_BC = (now >> 16) & 0xffff;
  REG_DE = now & 0xffff;
}

void do_emt_opendir()
{
  int i;
  char *dirname;
  for (i = 0; i < MAX_OPENDIR; i++) {
    if (dir[i].dir == NULL) break;
   }
  if (i == MAX_OPENDIR) {
    REG_DE = 0xffff;
    REG_A = EMFILE;
    return;
  }
  dirname = (char *)mem_pointer(REG_HL, 0);
  dir[i].dir = opendir(dirname);
  if (dir[i].dir == NULL) {
    REG_DE = 0xffff;
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  } else {
    strncpy(dir[i].pathname,dirname,FILENAME_MAX);
    REG_DE = i;
    REG_A = 0;
    REG_F |= ZERO_MASK;
  }
}

void do_emt_closedir()
{
  int i = REG_DE;
  int ok;
  if (i < 0 || i >= MAX_OPENDIR || dir[i].dir == NULL) {
    REG_A = EBADF;
    REG_F &= ~ZERO_MASK;
    return;
  }	
  ok = closedir(dir[i].dir);
  dir[i].dir = NULL;
  if (ok >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
}

void do_emt_readdir()
{
  int size, i = REG_DE;
  struct dirent *result;

  if (i < 0 || i >= MAX_OPENDIR || dir[i].dir == NULL) {
    REG_A = EBADF;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }	
  if (REG_HL + REG_BC > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  result = readdir(dir[i].dir);
  if (result == NULL) {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  size = strlen(result->d_name);
  if (size + 1 > REG_BC) {
    REG_A = ERANGE;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  strcpy((char *)mem_pointer(REG_HL, 1), result->d_name);
  REG_A = 0;
  REG_F |= ZERO_MASK;
  REG_BC = size;
}

void do_emt_chdir()
{
  int ok = chdir((char *)mem_pointer(REG_HL, 0));
  if (trs_emtsafe) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
  if (ok < 0) {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  } else {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  }
}

void do_emt_getcwd()
{
  char *result;
  if (REG_HL + REG_BC > 0x10000) {
    printf("Here 1\n");
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  result = getcwd((char *)mem_pointer(REG_HL, 1), REG_BC);
  if (result == NULL) {
    printf("Here 3\n");
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
    REG_BC = 0xFFFF;
    return;
  }
  REG_A = 0;
  REG_F |= ZERO_MASK;
  REG_BC = strlen(result);
}

// fixme - document codes that were removed.
void do_emt_misc()
{
  switch (REG_A) {
  case 0:
// Removed for sdltrs - mdg */
    REG_HL = 0;
    break;
  case 1:
    trs_exit();
    break;
  case 2:
    trs_debug();
    break;
  case 3:
    trs_reset(0);
    break;
  case 4:
    REG_HL = 0;
    break;
  case 5:
    REG_HL = trs_model;
    break;
  case 6:
    REG_HL = trs_disk_getsize(REG_BC);
    break;
  case 7:
    trs_disk_setsize(REG_BC, REG_HL);
    break;
#ifdef __linux    
  case 8:
    REG_HL = trs_disk_getstep(REG_BC);
    break;
  case 9:
    trs_disk_setstep(REG_BC, REG_HL);
    break;
#endif   
  case 10:
    REG_HL = grafyx_get_microlabs();
    break;
  case 11:
    grafyx_set_microlabs(REG_HL);
    break;
  case 12:
// Removed for sdltrs - mdg */
    REG_HL = 0;
    REG_BC = 0;
    break;
  case 13:
// Removed for sdltrs - mdg */
    break;
  case 14:
    REG_HL = stretch_amount;
    break;
  case 15:
    stretch_amount = REG_HL;
    break;
  case 16:
    REG_HL = trs_disk_doubler;
    break;
  case 17:
    trs_disk_doubler = REG_HL;
    break;
  case 18:
    REG_HL = 0; 
// Removed for sdltrs - mdg */
    break;
  case 19:
// Removed for sdltrs - mdg */
    break;
  case 20:
    REG_HL = trs_disk_truedam;
    break;
  case 21:
    trs_disk_truedam = REG_HL;
    break;
  default:
    error("unsupported function code to emt_misc");
    break;
  }
}

void do_emt_ftruncate()
{
  int i, result;
  off_t offset;
  if (trs_emtsafe) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }
  if (REG_HL + 8 > 0x10000) {
    REG_A = EFAULT;
    REG_F &= ~ZERO_MASK;
    return;
  }
  offset = 0;
  for (i=0; i<8; i++) {
    offset = offset + (mem_read(REG_HL + i) << i*8);
  }
#ifdef _WIN32  
  result = chsize(REG_DE, offset);
#else
  result = ftruncate(REG_DE, offset);
#endif  
  if (result == 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
}

void do_emt_opendisk()
{
  char *name = (char *)mem_pointer(REG_HL, 0);
  char *qname;
  int i;
  int oflag, eoflag;

  eoflag = REG_BC;
  switch (eoflag & EO_ACCMODE) {
  case EO_RDONLY:
  default:
    oflag = O_RDONLY;
    break;
  case EO_WRONLY:
    oflag = O_WRONLY;
    break;
  case EO_RDWR:
    oflag = O_RDWR;
    break;
  }
  if (eoflag & EO_CREAT)  oflag |= O_CREAT;
  if (eoflag & EO_EXCL)   oflag |= O_EXCL;
  if (eoflag & EO_TRUNC)  oflag |= O_TRUNC;
  if (eoflag & EO_APPEND) oflag |= O_APPEND;

  if (trs_emtsafe && oflag != O_RDONLY) {
    error("potentially dangerous emulator trap blocked");
    REG_A = EACCES;
    REG_F &= ~ZERO_MASK;
    return;
  }

#ifdef _WIN32
  if (*name == '\\' || *trs_disk_dir == '\0') {
#else
  if (*name == '/' || *trs_disk_dir == '\0') {
#endif            
    qname = strdup(name);
  } else {
    qname = (char *)malloc(strlen(trs_disk_dir) + 1 + strlen(name) + 1);
    strcpy(qname, trs_disk_dir);
#ifdef _WIN32    
    strcat(qname, "\\");
#else
    strcat(qname, "/");
#endif    
    strcat(qname, name);
  }
  for (i = 0; i < MAX_OPENDISK; i++) {
    if (!od[i].inuse) break;
  }
  if (i == MAX_OPENDISK) {
    REG_DE = 0xffff;
    REG_A = EMFILE;
    REG_F &= ~ZERO_MASK;
    free(qname);
    return;
  }
  /* Check if this is a XTRSHARD open request, and if so, redirect
     to the hardisk files in trs_hard.c */
  if ((((strncmp(name,"hard1-",6) == 0) ||
        (strncmp(name,"hard3-",6) == 0) ||
        (strncmp(name,"hard4-",6) == 0)) &&
        (strlen(name) == 7)) ||
      ((strncmp(name,"hard4p-",7) == 0) &&
        (strlen(name) == 8))) {
    int hard_unit = name[strlen(name) -1] - '0';
    if (hard_unit >=0 && hard_unit <= 3) {
      strcpy(od[i].filename,trs_hard_getfilename(hard_unit));
      od[i].fd = open(od[i].filename, oflag, REG_DE);
      od[i].oflag = oflag;
      if (od[i].fd >= 0)
        od[i].xtrshard = 1;
        xtrshard_fd[hard_unit] = od[i].fd;
        od[i].xtrshard_unit = hard_unit;
    } else {
      od[i].fd = -1;
    }
  } else {
    od[i].fd = open(qname, oflag, REG_DE);
    strcpy(od[i].filename,qname);
    od[i].xtrshard = 0;
  }
  free(qname);
  if (od[i].fd >= 0) {
    od[i].inuse = 1;
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
  REG_DE = od[i].fd;
}

int do_emt_closefd(int odindex)
{
  int i;
  if (od[odindex].xtrshard) {
    for (i=0;i<4;i++) {
      if (xtrshard_fd[i] == od[odindex].fd)
        xtrshard_fd[i] = -1;
    }
  } 
  return(close(od[odindex].fd));                         
}

void do_emt_closedisk()
{
  int i;
  int res;
  if (REG_DE == 0xffff) {
    for (i = 0; i < MAX_OPENDISK; i++) {
      if (od[i].inuse) {
    do_emt_closefd(i);
	od[i].inuse = 0;
    od[i].xtrshard = 0;
    od[i].filename[0] = 0;
      }
    }
    REG_A = 0;
    REG_F |= ZERO_MASK;
    return;
  }

  for (i = 0; i < MAX_OPENDISK; i++) {
    if (od[i].inuse && od[i].fd == REG_DE) break;
  }
  if (i == MAX_OPENDISK) {
    REG_A = EBADF;
    REG_F &= ~ZERO_MASK;
    return;
  }
  od[i].inuse = 0;
  od[i].xtrshard = 0;
  od[i].filename[0] = 0;
  res = do_emt_closefd(i);
  if (res >= 0) {
    REG_A = 0;
    REG_F |= ZERO_MASK;
  } else {
    REG_A = errno;
    REG_F &= ~ZERO_MASK;
  }
}

void do_emt_resetdisk()
{
  int i;
  
  for (i = 0; i < MAX_OPENDISK; i++) {
    if (od[i].inuse) {
      do_emt_closefd(i);
      od[i].inuse = 0;
      od[i].xtrshard = 0;
      od[i].filename[0] = 0;
    }
  } 
}

void trs_imp_exp_save(FILE *file)
{
  int i;
  int one = 1;
  int zero = 0;
  
  for (i=0;i<MAX_OPENDIR;i++) {
    if (dir[i].dir == NULL)
      trs_save_int(file, &zero, 1);
    else
      trs_save_int(file, &one, 1);
    trs_save_filename(file, dir[i].pathname);
  }
  for (i=0;i<MAX_OPENDISK;i++) {
    trs_save_int(file, &od[i].fd, 1);
    trs_save_int(file, &od[i].inuse, 1);
    trs_save_int(file, &od[i].oflag, 1);
    trs_save_int(file, &od[i].xtrshard, 1);
    trs_save_int(file, &od[i].xtrshard_unit, 1);
    trs_save_filename(file, od[i].filename);
  }
}

void trs_imp_exp_load(FILE *file)
{
  int i, dir_present;

  /* Close any open dirs and files */
  for (i=0;i<MAX_OPENDIR;i++) {
    if (dir[i].dir)
      closedir(dir[i].dir);
  }
  for (i=0;i<MAX_OPENDISK;i++) {
    if (od[i].inuse)
      close(od[i].fd);
  }
  /* Load the state */
  for (i=0;i<MAX_OPENDIR;i++) {
    trs_load_int(file, &dir_present, 1);
    trs_load_filename(file, dir[i].pathname);
    if (dir_present)
      dir[i].dir = opendir(dir[i].pathname);
    else
      dir[i].dir = NULL;
  }
  for (i=0;i<MAX_OPENDISK;i++) {
    trs_load_int(file, &od[i].fd, 1);
    trs_load_int(file, &od[i].inuse, 1);
    trs_load_int(file, &od[i].oflag, 1);
    trs_load_int(file, &od[i].xtrshard, 1);
    trs_load_int(file, &od[i].xtrshard_unit, 1);
    trs_load_filename(file, od[i].filename);
  }
  /* Reopen the files */
  for (i=0;i<4;i++)
    xtrshard_fd[i] = -1;
  for (i=0;i<MAX_OPENDIR;i++) {
    if (dir[i].dir)
      dir[i].dir = opendir(dir[i].pathname);
  }
  for (i=0;i<MAX_OPENDISK;i++) {
    if (od[i].inuse) {
      od[i].fd = open(od[i].filename, od[i].oflag);
      if (od[i].xtrshard) 
        xtrshard_fd[od[i].xtrshard_unit] = od[i].fd;
    }
  }
}

void
trs_impexp_xtrshard_attach(int drive, char *filename)
{
  int i;
  for (i=0;i<MAX_OPENDISK;i++) {
    if (od[i].inuse && od[i].xtrshard && (od[i].xtrshard_unit == drive)) {
      close(od[i].fd);
      strcpy(od[i].filename, filename);
      od[i].fd = open(filename, od[i].oflag);
      xtrshard_fd[od[i].xtrshard_unit] = od[i].fd;
    }
  }
}

void
trs_impexp_xtrshard_remove(int drive)
{
  int i;
  for (i=0;i<MAX_OPENDISK;i++) {
    if (od[i].inuse && od[i].xtrshard && (od[i].xtrshard_unit == drive)) {
      close(od[i].fd);
      od[i].fd = -1;
      xtrshard_fd[od[i].xtrshard_unit] = -1;
    }
  }
}
