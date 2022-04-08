/*
 * fornext.c -- functions for FOR, NEXT and GOSUB
 */

#include <string.h>

#include "basic.h"
#include "fornext.h"
#include "err.h"
#include "trs_str.h"
#include "trs_io.h"

struct for_elem fstk[FORSTKSIZE];
int gstk[GSBSTKSIZE];

struct for_elem* fsp = fstk;
int* gsp = gstk;

void fornext_init(void)
{
	fsp = fstk;
	gsp = gstk;
}

void gosub(int line, int ret)
{
	if (gsp >= gstk + GSBSTKSIZE)
		err(GOSUB_OV);

	*gsp++ = ret;
	lnum = line;
}

void ret(void)
{
	if (gsp == gstk)
		err(RET_WO_GOSUB);
	lnum = *--gsp;
}

/*
 * This is it! This really takes the cake.  NEXT, NEXT I could understand
 * doing what it does, but FOR??  Boy oh boy, you gotta see this one.  What
 * is the result of the following?
 *
 * 10 FORI=1TO10
 * 20 FORI=1TO5
 * 30 PRINT I
 * 40 NEXT
 *
 * or how about this gem?
 *
 * 10 FORI=1TO10
 * 20 FORJ=1TO5
 * 30 FORI=1TO5
 * 40 PRINT I,J
 * 50 NEXT
 * 60 NEXT
 *
 * Go ahead, try it.  My compiler can take it, my brain can't.
 */
void rof(int strt_num, char* vstr, double limit, double step, float* fvar, int* ivar)
{
	struct for_elem*	f;

	if (fsp >= fstk + FORSTKSIZE)
		err(FOR_OV);

	for (f = fsp; f > fstk; f--)
		if (!strcmp(f->varname, vstr))
			fsp = f - 1;

	fsp++;
	fsp->l = strt_num;
	fsp->limit = limit;
	fsp->step = step;
	fsp->varname = vstr;
	fsp->fvar = fvar;
	fsp->ivar = ivar;
}

/*
 * I can't believe it, I just can't!!!  When a NEXT X is encountered,
 * if the current FOR NEXT loop does not have X as the loop variable,
 * the stack must be popped and the next FOR NEXT loop tried.
 *
 * Got that?  So what does this BASIC program print?
 *
 * 10 FORI=1TO3:FORJ=1TO3:IFJ=2THEN40
 * 20 PRINTI;J
 * 30 NEXTJ
 * 40 NEXTI
 *
 * This routine will return 1 if control is to go back to the head
 * of the FOR loop, 0 otherwise.
 */
int next(char* lvar)
{
	while (fsp > fstk) {
		if (lvar == NULL || !strcmp(fsp->varname, lvar)) {
			if (fsp->fvar == NULL) {
				*(fsp->ivar) += fsp->step;
				if (fsp->step < 0 ? *(fsp->ivar) >= fsp->limit
								  : *(fsp->ivar) <= fsp->limit) {
					lnum = fsp->l;
					return(1);
				}
				else {
					fsp--;
					return(0);
				}
			}
			else {
				*(fsp->fvar) += fsp->step;
				if (fsp->step < 0 ? *(fsp->fvar) >= fsp->limit
								  : *(fsp->fvar) <= fsp->limit) {
					lnum = fsp->l;
					return(1);
				}
				else {
					fsp--;
					return(0);
				}
			}
		}
		fsp--;
	}
	if (lvar != NULL)
		err(NEXT_NM_FOR);
	else
		err(NEXT_WO_FOR);
	/*NOTREACHED*/
	return 0; // to keep compilers happy.
}
