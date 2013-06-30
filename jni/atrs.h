
#ifndef __ATRS_H__
#define __ATRS_H__

#define SCREEN_UPDATE_THRESHOLD 5000

extern Uchar* memory;
extern unsigned char trs_screen[2048];

#ifdef ANDROID_BATCHED_SCREEN_UPDATE
#define SCREEN_FORCED_UPDATE_INTERVAL 1000000

extern int instructionsSinceLastScreenAccess;
extern int screenWasUpdated;
#endif

extern int trs_rom_size;

char* get_disk_path(int disk);

void android_cassette_out(int value);

void xlog(const char* msg);

#ifdef SETITIMER_FIX
#include <time.h>
extern suseconds_t next_timer;
#endif

#endif
