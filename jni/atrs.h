
#ifndef __ATRS_H__
#define __ATRS_H__

#include "z80.h"

#define SCREEN_UPDATE_THRESHOLD 5000

extern unsigned char trs_screen[2048];

char* get_disk_path(int disk);

void init_audio(int rate, int channels, int encoding, int bufSize);
void close_audio();
void pause_audio(int pause_on);
void flush_audio_queue();
void set_expanded_screen_mode(int flag);

void xlog(const char* msg);
void not_implemented(const char* msg);

#endif
