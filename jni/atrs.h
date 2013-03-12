
#ifndef __ATRS_H__
#define __ATRS_H__

extern unsigned char trs_screen[2048];
extern int instructionsSinceLastScreenAccess;
extern int screenWasUpdated;

void check_for_screen_updates();

#endif
