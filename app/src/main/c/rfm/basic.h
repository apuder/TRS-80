/*
 * basic.h -- runtime #defines and stuff for BASIC simulator.
 */

typedef unsigned int line_number;

extern char** data_ptr;
extern char* data[];
extern int lnum;
extern line_number L;

#define RESTORE	data_ptr = data

#ifdef __TURBOC__
#include <stdlib.h>
#endif

#include <stdio.h>
#include <time.h>

#ifdef __TURBOC__
#define RANDOM randomize()
#else
#define RANDOM (srand(getpid() + time(0)))
#endif

extern void main0(void);
