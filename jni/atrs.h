
#ifndef __ATRS_H__
#define __ATRS_H__

#define SCREEN_UPDATE_THRESHOLD 2000

extern Uchar* memory;
extern unsigned char trs_screen[2048];
extern int instructionsSinceLastScreenAccess;
extern int screenWasUpdated;
extern int trs_rom_size;

char* get_disk_path(int disk);

void xlog(const char* msg);

#endif
