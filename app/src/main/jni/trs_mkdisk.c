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

/* Copyright (c) 1996-98, Timothy Mann */

/* This software may be copied, modified, and used for any purpose
 * without fee, provided that (1) the above copyright notice is
 * retained, and (2) modified versions are clearly marked as having
 * been modified, with the modifier's name and the date included.  */

/*
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

/*
 * mkdisk.c
 * Make a blank (unformatted) emulated floppy or hard drive in a file,
 * or write protect/unprotect an existing one.
 */

#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include "trs_disk.h"
#include "trs_hard.h"
#include "trs_mkdisk.h"

typedef unsigned char Uchar;
#include "reed.h"

#ifdef _WIN32
#include "wtypes.h"
#include "winnt.h"

static int win_set_readonly(char *filename, int readonly)
{
  DWORD attr;
  attr = GetFileAttributes(filename);
  SetFileAttributes(filename, readonly
		? (attr | FILE_ATTRIBUTE_READONLY)
		: (attr & ~FILE_ATTRIBUTE_READONLY)) != 0;
}
#endif

void trs_protect_disk(int drive, int writeprot)
{
  char prot_filename[FILENAME_MAX];
  struct stat st;
  int newmode;
  FILE *f;
  char *diskname;
  int emutype = trs_disk_getdisktype(drive);
  
  diskname = trs_disk_getfilename(drive);
  if (diskname[0] == 0)
    return;
    
  strcpy(prot_filename, diskname);
#ifndef _WIN32  
  if (stat(prot_filename, &st) < 0)
    return;
#endif    
  trs_disk_remove(drive);
  
  if (emutype == JV3 || emutype == DMK) {
#ifdef _WIN32  
    win_set_readonly(prot_filename,0);
#else
    chmod(prot_filename, st.st_mode | (S_IWUSR|S_IWGRP|S_IWOTH));
#endif
    f=fopen(prot_filename,"r+");
    if (f!=NULL) {
      if (emutype == JV3) {
        /* Set the magic byte */
        fseek(f, 256*34-1, 0);
        putc(writeprot ? 0 : 0xff, f);
      } else {
        /* Set the magic byte */
        putc(writeprot ? 0xff : 0, f);
      }
      fclose(f);  
    }
  }
    
#ifdef _WIN32  
  win_set_readonly(prot_filename,writeprot);
#else
  if (writeprot) 
    newmode = st.st_mode & ~(S_IWUSR|S_IWGRP|S_IWOTH);
  else 
    newmode = st.st_mode | (S_IWUSR|S_IWGRP|S_IWOTH);
  chmod(prot_filename, newmode);
#endif  
  trs_disk_insert(drive,prot_filename);
}

void trs_protect_hard(int drive, int writeprot)
{
  char prot_filename[FILENAME_MAX];
  struct stat st;
  int newmode;
  char *diskname;
  FILE *f;
  
  diskname = trs_hard_getfilename(drive);
  if (diskname[0] == 0)
    return;
    
  strcpy(prot_filename, diskname);

#ifndef _WIN32  
  if (stat(prot_filename, &st) < 0)
    return;
#endif    
  trs_hard_remove(drive);
  
#ifdef _WIN32  
  win_set_readonly(prot_filename,0);
#else
  chmod(prot_filename, st.st_mode | (S_IWUSR|S_IWGRP|S_IWOTH));
#endif
  f=fopen(prot_filename,"r+");
  if (f!=NULL) {
    fseek(f, 7, 0);
    newmode = getc(f);
    if (newmode != EOF) {
      newmode = (newmode & 0x7f) | (writeprot ? 0x80 : 0);
      fseek(f, 7, 0);
      putc(newmode, f);
    }
    fclose(f);  
  }
    
#ifdef _WIN32  
  win_set_readonly(prot_filename,writeprot);
#else
  if (writeprot) 
    newmode = st.st_mode & ~(S_IWUSR);
  else 
    newmode = st.st_mode | (S_IWUSR);
  chmod(prot_filename, newmode);
#endif  
  trs_hard_attach(drive,prot_filename);
}

int trs_create_blank_jv1(char *fname)
{
  FILE *f;

  /* Unformatted JV1 disk - just an empty file! */
  f = fopen(fname, "wb");
  if (f == NULL) 
	return -1;
  fclose(f);
  return 0;
}

int trs_create_blank_jv3(char *fname)
{
  FILE *f;
  int i;
  
  /* Unformatted JV3 disk. */
  f = fopen(fname, "wb");
  if (f == NULL) 
	return -1;
  for (i=0; i<(256*34); i++) 
    putc(0xff, f);
  fclose(f);
  return 0;
}

int trs_create_blank_dmk(char *fname, int sides, int density, 
                         int eight, int ignden)
{
  FILE *f;
  int i;
  /* Unformatted DMK disk */

  if (sides == -1) sides = 2;
  if (density == -1) density = 2;

  if (sides != 1 && sides != 2) 
    return -1;
	
  if (density < 1 || density > 2)
    return -1;
    
  f = fopen(fname, "wb");
  if (f == NULL) 
	return -1;
  putc(0, f);           /* 0: not write protected */
  putc(0, f);           /* 1: initially zero tracks */
  if (eight) {
    if (density == 1)
       i = 0x14e0;
    else
       i = 0x2940;
  } else {
    if (density == 1)
      i = 0x0cc0;
    else
      i = 0x1900;
  }
  putc(i & 0xff, f);    /* 2: LSB of track length */
  putc(i >> 8, f);      /* 3: MSB of track length */
  i = 0;
  if (sides == 1)   i |= 0x10;
  if (density == 1) i |= 0x40;
  if (ignden)       i |= 0x80;
  putc(i, f);           /* 4: options */
  putc(0, f);           /* 5: reserved */
  putc(0, f);           /* 6: reserved */
  putc(0, f);           /* 7: reserved */
  putc(0, f);           /* 8: reserved */
  putc(0, f);           /* 9: reserved */
  putc(0, f);           /* a: reserved */
  putc(0, f);           /* b: reserved */
  putc(0, f);           /* c: MBZ */
  putc(0, f);           /* d: MBZ */
  putc(0, f);           /* e: MBZ */
  putc(0, f);           /* f: MBZ */
  fclose(f);
  return 0;
}

int trs_create_blank_hard(char *fname, int cyl, int sec, 
                          int gran, int dir)
{
  FILE *f;
  int i;
    /* Unformatted hard disk */
    /* We don't care about most of this header, but we generate
       it just in case some user wants to exchange hard drives with
       Matthew Reed's emulator or with Pete Cervasio's port of
       xtrshard/dct to Jeff Vavasour's Model III/4 emulator.
    */
  time_t tt = time(0);
  struct tm *lt = localtime(&tt);
  ReedHardHeader rhh;
  Uchar *rhhp;
  int cksum;

  if (cyl == -1) cyl = 202;
  if (sec == -1) sec = 256;
  if (gran == -1) gran = 8;
  if (dir == -1) dir = 1;

  if (cyl < 3) {
    printf( " error: cyl < 3\n");
    return(-1);
  }
  if (cyl > 256) {
    printf( " error: cyl > 256\n");
    return(-1);
  }
  if (cyl > 203) {
    printf(
     " warning: cyl > 203 is incompatible with XTRSHARD/DCT\n");
  }
  if (sec < 4) {
    printf( " error: sec < 4\n");
    return(-1);
  }
  if (sec > 256) {
    printf( " error: sec > 256\n");
    return(-1);
  }
  if (gran < 1) {
    return(-1);
  }
  if (gran > 8) {
    return(-1);
  }
  if (sec < gran) {
    return(-1);
  }
  if (sec % gran != 0) {
    return(-1);
  }
  if (sec / gran > 32) {
    return(-1);
  }
  if (dir < 1) {
    return(-1);
  }
  if (dir >= cyl) {
    return(-1);
  }
  
  memset(&rhh,0,sizeof(rhh));

  rhh.id1 = 0x56;
  rhh.id2 = 0xcb;
  rhh.ver = 0x10;
  rhh.cksum = 0;  /* init for cksum computation */
  rhh.blks = 1;
  rhh.mb4 = 4;
  rhh.media = 0;
  rhh.flag1 = 0;
  rhh.flag2 = rhh.flag3 = 0;
  rhh.crtr = 0x42;
  rhh.dfmt = 0;
  rhh.mm = lt->tm_mon + 1;
  rhh.dd = lt->tm_mday;
  rhh.yy = lt->tm_year;
  rhh.dparm = 0;
  rhh.cyl = cyl;
  rhh.sec = sec;
  rhh.gran = gran;
  rhh.dcyl = dir;
  strcpy(rhh.label, "xtrshard");
  strcpy(rhh.filename, fname); /* note we don't limit to 8 chars */

  cksum = 0;
  rhhp = (Uchar *) &rhh;
  for (i=0; i<=31; i++) {
    cksum += rhhp[i];
  }
  rhh.cksum = ((Uchar) cksum) ^ 0x4c;

  f = fopen(fname, "wb");
  if (f == NULL) 
    return(1);
  fwrite(&rhh, sizeof(rhh), 1, f);
  fclose(f);
  return 0;
}


