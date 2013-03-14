
#ifndef __ATRS_H__
#define __ATRS_H__

extern Uchar* memory;
extern unsigned char trs_screen[2048];
extern int instructionsSinceLastScreenAccess;
extern int screenWasUpdated;
extern int trs_rom_size;

int android_main(Ushort entryAddr);
void check_for_screen_updates();

#endif
