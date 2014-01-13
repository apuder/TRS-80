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
   Modified by Timothy Mann, 1996 and later
   $Id: main.c,v 1.16 2009/06/15 23:33:53 mann Exp $
   Modified by Mark Grebe, 2006
   Last modified on Wed May 07 09:12:00 MST 2006 by markgrebe
*/

#include <string.h>
#include <stdlib.h>
#include <SDL/SDL.h>
#include <sys/stat.h>

#include "z80.h"
#include "trs.h"
#include "trs_disk.h"
#include "load_cmd.h"
#include "trs_sdl_keyboard.h"
#include "trs_state_save.h"

#ifdef MACOSX
#include "macosx/trs_mac_interface.h"
#endif

extern int fullscreen;

int trs_model = 1;
int trs_autodelay = 0;
char *program_name;

static void check_endian()
{
    wordregister x;
    x.byte.low = 1;
    x.byte.high = 0;
    if(x.word != 1)
    {
	printf("Program compiled with wrong ENDIAN value -- adjust the Makefile.local, type \"rm *.o\", recompile, and try again.");
    }
}

int trs_load_rom(char *filename)
{
    FILE *program;
    int c;

    if((program = fopen(filename, "rb")) == NULL)
    {
    return(-1);
    }
    c = getc(program);
    if (c == ':') {
        /* Assume Intel hex format */
        rewind(program);
        trs_rom_size = load_hex(program);
	fclose(program);
	if (trs_rom_size == -1)
	  return(-1);
    else
	  return(0);
    } else if (c == 1 || c == 5) {
	/* Assume MODELA/III file */
	int res;
	extern Uchar *rom; /*!! fixme*/
	Uchar loadmap[Z80_ADDRESS_LIMIT];
	rewind(program);
	res = load_cmd(program, rom, loadmap, 0, NULL, -1, NULL, NULL, 1);
	if (res == LOAD_CMD_OK) {
	    trs_rom_size = Z80_ADDRESS_LIMIT;
	    while (trs_rom_size > 0) {
		if (loadmap[--trs_rom_size] != 0) {
		    trs_rom_size++;
		    break;
		}
	    }
	    fclose(program);
	    return(0);
	} else {
	    /* Guess it wasn't one */
	    rewind(program);
	    c = getc(program);
	}
    }
    trs_rom_size = 0;
    while (c != EOF) {
        mem_write_rom(trs_rom_size++, c);
	c = getc(program);
    }
    return(0);
}

void trs_load_compiled_rom(int size, unsigned char rom[])
{
    int i;
    
    trs_rom_size = size;
    for(i = 0; i < size; ++i)
    {
	mem_write_rom(i, rom[i]);
    }
}

#ifndef ANDROID
int SDLmain(int argc, char *argv[])
{
    int debug = FALSE;
    struct stat st;

    /* program_name must be set first because the error
     * printing routines use it. */
    program_name = strrchr(argv[0], '/');
    if (program_name == NULL) {
      program_name = argv[0];
    } else {
      program_name++;
    }

    check_endian();

#ifndef MACOSX
    putenv("SDL_VIDEO_CENTERED=1");
#endif	
    
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK | SDL_INIT_AUDIO | SDL_INIT_TIMER) != 0) { 
        fprintf(stderr, "Failed to initialize SDL library");
  	    exit(1);
    }
        
    /* Enable Unicode key translations */
    SDL_EnableUNICODE(TRUE); 

    argc = trs_parse_command_line(argc, argv, &debug);
    if (argc > 1) {
      fprintf(stderr, "%s: erroneous argument %s\n", program_name, argv[1]);
      exit(1);
    }
    
    trs_set_keypad_joystick();    
    trs_open_joystick();
    
    if (stat(trs_disk_dir, &st) < 0) {
      strcpy(trs_disk_dir,".");
    }                   
    if (stat(trs_hard_dir, &st) < 0) {
      strcpy(trs_hard_dir,".");
    }                   
    if (stat(trs_cass_dir, &st) < 0) {
      strcpy(trs_cass_dir,".");
    }                   
    if (stat(trs_state_dir, &st) < 0) {
      strcpy(trs_state_dir,".");
    }                   
    if (stat(trs_disk_set_dir, &st) < 0) {
      strcpy(trs_disk_set_dir,".");
    }                   
    if (stat(trs_printer_dir, &st) < 0) {
      strcpy(trs_printer_dir,".");
    }                   
 
    mem_init();
    trs_disk_init(0);
    trs_rom_init();
    trs_screen_init();
    screen_init();
    trs_timer_init();

    trs_reset(1);
    if (init_state_file[0] != 0) {
      trs_state_load(init_state_file);
      trs_screen_init();
      trs_screen_refresh();
      }
#ifdef MACOSX
	TrsOriginSet();
#endif
	
    if (!debug || fullscreen) {
      /* Run continuously until exit or request to enter debugger */
      z80_run(TRUE);
    }
    printf("Entering debugger.\n");
    debug_init();
    debug_shell();
    printf("Quitting.\n");
#ifdef MACOSX
    trs_mac_save_defaults();
#endif
    exit(0);
}
#endif

