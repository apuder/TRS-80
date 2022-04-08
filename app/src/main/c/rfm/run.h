//
// run.h -- interface to execution of a compiled BASIC program.
//

#include "trs_str.h"
#include "trs_io.h"
#include "peekpoke.h"

// Maximum number of statements allowed in basic_frame()
// Needed because OUT statements put values into a fixed size buffer.
#define MAX_STATEMENTS_PER_FRAME (100)

// Call once before basic_frame().
// Or whenever you want to re-run the program.

extern void basic_init(void);

// Return vales from basic_frame()

#define FRAME_DONE (0) // all statements done; process frame
#define FRAME_KEY  (1) // pass an input key to update_input()
#define FRAME_EXIT (2) // BASIC program has finished

// Call to execute stcount BASIC statements.  Some
// experimentation will be needed to determine a good number
// but MAX_STATEMENTS_PER_FRAME is a good starting point.

extern int basic_frame(int stcount);

// Callouts when basic_frame() is executing.

// Draw character val at screen address addr (0 .. 1023).
extern void put_char(int addr, unsigned char val);

// Scroll the screen up one line.
extern void scroll(void);

// Variables to check after each basic_frame() call.

extern int screenmode; // One if screen is in 32 character/line mode.
extern int outidx; // number of OUT statements that happened
extern struct outrec outlog[]; // .port and .value for each OUT from 0 .. outidx - 1
