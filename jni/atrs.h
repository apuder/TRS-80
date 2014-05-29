
#ifndef __ATRS_H__
#define __ATRS_H__

#include "z80.h"

#define SCREEN_UPDATE_THRESHOLD 5000

extern unsigned char trs_screen[2048];

#ifdef ANDROID_BATCHED_SCREEN_UPDATE
#define SCREEN_FORCED_UPDATE_INTERVAL 1000000

extern int instructionsSinceLastScreenAccess;
extern int screenWasUpdated;
#endif

char* get_disk_path(int disk);

void init_audio(int rate, int channels, int encoding, int bufSize);
void close_audio();
void pause_audio(int pause_on);
void flush_audio_queue();

void xlog(const char* msg);
void not_implemented(const char* msg);

#ifdef SETITIMER_FIX
#include <time.h>
extern suseconds_t next_timer;
#endif

#endif
