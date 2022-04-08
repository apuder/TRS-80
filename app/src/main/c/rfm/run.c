//
// run.c -- high level interface routines
//

#include "basic.h"
#include "trs_scrn.h"
#include "fornext.h"
#include "run.h"

int outidx;
struct outrec outlog[MAX_STATEMENTS_PER_FRAME];

extern int must_exit;

void basic_init(void)
{
	// Move into basic_init();
	trs_str_init();
	scrn_init();
	fornext_init();
	RESTORE;

	need_input = 0;
	must_exit = 0;
}

extern int lnum;

int basic_frame(int stcount)
{
	int statements = 0;

	if (stcount > MAX_STATEMENTS_PER_FRAME)
		stcount = MAX_STATEMENTS_PER_FRAME;

	outidx = 0;

	while (lnum > -10000) {
#if defined(ALLOW_BREAK)
		if (trs_peek(14400) & 4)
			break_hit();
#endif
		if (need_input)
			return FRAME_KEY;

		main0();

		if (must_exit)
			break;

		statements++;
		if (statements >= stcount) {
			return 0;
		}
	}

	return FRAME_EXIT;
}
