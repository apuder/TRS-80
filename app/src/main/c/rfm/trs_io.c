/*
 * trs_io.c -- some I/O routines for the runtime environment of the
 * BASIC compiler.
 */

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>

#include "basic.h"
#include "trs_str.h"
#include "trs_io.h"
#include "trs_scrn.h"
#include "key.h"
#include "err.h"

extern void tr_exit(void);

char** data_ptr = data;
line_number L;

/* Routine to read a data element */
char* READ(void)
{
	if (*data_ptr == 0)
		err(OUT_OF_DATA);
	return(*data_ptr++);
}

/* routine to exit with a run-time error */
void err(int i)
{
	trs_str_init();	/* so we don't "blow up" */
	lpush("\nStopped at line");
	/* should use unsigned_int_push or some such */
	fpush((float)L); lpush(".\n"); cat(); cat(); print();

	switch (i) {
	case RET_WO_GOSUB:
		lpush("Return without gosub\n"); print();
		break;
	case NEXT_WO_FOR:
		lpush("Next without for\n"); print();
		break;
	case NEXT_NM_FOR:
		lpush("Next var does not match any current for\n"); print();
		break;
	case OUT_OF_DATA:
		lpush("Out of data error\n"); print();
		break;
	case UNDEF_LINE:
		lpush("Undefined line number "); print();
		ipush(lnum); print();
		lpush("\n"); print();
		break;
	case BAD_ONGO:
		lpush("Bad ON GOTO/GOSUB index\n"); print();
		break;
	case STRING2LONG:
		lpush("String too long\n"); print();
		break;
	case ARRAY_LIMIT:
		lpush("Sorry, max array dimension is 4\n"); print();
		break;
	case ARRAY_MEM:
		lpush("No memory for some array\n"); print();
		break;
	case GOSUB_OV:
		lpush("Gosub stack overflow\n"); print();
		break;
	case FOR_OV:
		lpush("For stack overflow\n"); print();
		break;
	case STRING_OV:
		lpush("String stack overflow\n"); print();
		break;
	case ARRAY_BOUND:
		lpush("Array out of bounds\n"); print();
		break;
	default:
		lpush("unknown error (internal fault)\n"); print();
		break;
	}
	tr_exit();
}

void break_hit(void)
{
	lpush("Break at line");
	/* see note above */
	fpush((float)L); lpush("\n"); cat(); cat(); print();
	tr_exit();
}

/* some support routines for PRINT */

void fpush(double f)
{
	fformat_push(f, 1);
}

/* not correct format, but getting better.  Misses out on E notation */
void fformat_push(double f, int trailspace)
{
	static char buf[32];
	char*		p;

	sprintf(buf, " %f", f);
	for (p = buf; *p; p++)
		;
	for (p--; *p == '0'; p--)
		;
	if (*p == '.')
		p--;

	if (trailspace)
		*(++p) = ' ';	/* put a trailing blank */
	*(p+1) = '\0';		/* terminate string */

	if (buf[1] == '0' && buf[2] == '.')
		lpush(buf+1);
	else
		lpush(buf);
}

void ipush(int i)
{
	iformat_push(i, 1);
}

void iformat_push(int i, int trailspace)
{
	static char buf[32];

	sprintf(buf, " %d%s", i, trailspace ? " " : "");
	lpush(buf);
}

/*
 * Routines for dealing with INPUT.
 *
 * BUGS:
 *	uses C stdio instead of model III i/o
 *  only will input integers and strings, no negative numbers.
 */

static char input_buffer[256];
static char* inp_ptr;
static char* istr;
static int	 inum;
static int	 ret_prev;
static int   inp_i;

void start_input(char* s)
{
	lpush(s);
	lpush("? ");
	cat();
	print();

	inp_i = 0;
	trputc('\016');
}

// Returns 1 when input ready.

static int ch_input(int ch)
{
	if (ch == '\1') {
		break_hit();
	}
	else if (ch == '\010') {
		if (inp_i != 0) {
			inp_i--;
			trputc(ch);
		}
	}
	else if (ch != '\015') {
		if (inp_i < 255) {
			input_buffer[inp_i++] = ch;
			trputc(ch);
		}
	}
	else if (ch == '\015') {
		trputc('\017');
		trputc('\015');
		input_buffer[inp_i] = '\0';

		inp_ptr = input_buffer;
		if (*inp_ptr == '\0')
			ret_prev = 1;
		else
			ret_prev = 0;

		return 1;
	}

	return 0;
}

#define I_PREV	1
#define I_NUM	2
#define I_STR	3
#define I_CONT	4

static int grab_input(void)
{
	char*	p;
	char*	q;
	int		done;
	int		numeric;

	if (ret_prev)
		return(I_PREV);

	do {
		for (p = inp_ptr; *p && (*p == ' ' || *p == '\t'); p++)
			;
		if (*p == '\0')  {
			done = 0;
			start_input("?");
			return I_CONT;
		}
		else
			done = 1;
	} while (!done);

	if (*p == '"') { /* aha, a string */
		inp_ptr = ++p;
		while (*p != '"' && *p)
			p++;
		if (*p == '"')
			*p++ = '\0';
		istr = inp_ptr;
		inp_ptr = p;
		return(I_STR);
	}
	inp_ptr = p;
	while (*p != ',' && *p)
		p++;
	if (*p == ',')
		*p++ = '\0';
	/* string or numeric value */
	numeric = 1;
	for (q = inp_ptr; *q; q++)
		/* Turbo C, not me!! (no &&=) */
		numeric = numeric && (isdigit(*q) || *q == ' ' || *q == '\n'
							  || *q == '\t' || *q == '-');
	if (numeric) {
		inum = atoi(inp_ptr);
		istr = inp_ptr;
		inp_ptr = p;
		return(I_NUM);
	}
	else {
		istr = inp_ptr;
		inp_ptr = p;
		return(I_STR);
	}
}

static void redo(void)
{
	start_input("REDO");
}

static int *int_input_var;
static float *float_input_var;
static struct string *string_input_var;

int need_input;

void iinput(int* ivar)
{
	int_input_var = ivar;
	float_input_var = 0;
	string_input_var = 0;
	need_input = 1;
}

void finput(float* fvar)
{
	int_input_var = 0;
	float_input_var = fvar;
	string_input_var = 0;
	need_input = 1;
}

void sinput(struct string* svar)
{
	int_input_var = 0;
	float_input_var = 0;
	string_input_var = svar;
	need_input = 1;
}

void update_input(int ch)
{
	int ret;

	if (ch_input(ch) == 0)
		return;

	ret = grab_input();
	if (ret == I_CONT)
		return;

	if (string_input_var) {
		if (ret != I_PREV) {
			lpush(istr);
			pop(string_input_var);
			need_input = 0;
			return;
		}
	}

	if (ret != I_NUM && ret != I_PREV) {
		redo();
		return;
	}

	if (ret == I_NUM) {
		if (int_input_var)
			*int_input_var = inum;

		if (float_input_var)
			*float_input_var = (float)inum;
	}

	need_input = 0;
}

/*
 * A start of the routines necessary for implementing PRINT USING.
 * The amount to which these will be completed is very dependent on
 * what programs use them.
 *
 * Currently, I will only do integers and floating point numbers which
 * are printed in formats only requiring "#.," to indicate them.
 *
 * (and I may not have the perfectly well done either...)
 *
 * And a bug:  PRINT USING of a string with a 0 character in it will
 * fail since I only use C-strings.  This is likely not a problem for
 * any program.
 *
 * Some other issues.  When fields are too small, BASIC would output a %
 * to indicate this.  I will not do this.  Seems to me that a "," means
 * a comma every 3 fields, but hey, I could be wrong.
 */

static char* using;

/*
 * setusing() pop the USING string off the stack
 */
void setusing(void)
{
	static struct string	u;

	pop(&u);
	using = u.str_char;
	using[u.str_len] = '\0';
}

/*
 * outusing -- print any characters in the using string which are
 * not formatting things.  This routine will change as the using
 * routines are brought more in line with BASIC's PRINT USING.
 * This routine is bogglingly inefficient!
 */
static void outusing(void)
{
	char out[2];

	while (*using && *using != '#' && *using != '.' && *using != ',') {
		out[0] = *using++;
		out[1] = '\0';
		lpush(out);
		print();
	}
}

/*
 * nusing -- the real heart of the print using routines for numbers.
 * It takes a buf in which it expects a number with a decimal place.
 */
static void nusing(char* num)
{
	char*	decimal;
	char*	lastbefore;
	char*	p;
	int		docomma;
	int		numplace[2];
	int		whichplace;
	int		donecheck;
	int		ocnt;
	static char outbuf[64];

	outusing();

	/* for now, we may assume we have a ',', '#' or '.' here */
	for (decimal = num; *decimal != '.'; decimal++)
		;
	lastbefore = decimal - 1;
	*decimal++ = '\0';

	numplace[0] = 0;
	numplace[1] = 0;
	whichplace = 0;
	donecheck = 0;
	docomma = 0;
	for (; *using && !donecheck; using++)
		switch (*using) {
		case '.':
			if (++whichplace == 2)
				donecheck = 1;
			break;
		case '#':
			numplace[whichplace]++;
			break;
		case ',':
			docomma = 1;
			break;
		default:
			donecheck = 1;
		}

    /* output the first part into a buffer backwards */
	if (docomma)
		numplace[0] += numplace[0] / 3;

	ocnt = 0;
	outbuf[63] = '\0';
	for (p = outbuf + 62; lastbefore >= num && isdigit(*lastbefore); lastbefore--) {
		if (ocnt == 3 && docomma) {
			*p-- = ',';
			numplace[0]--;
			ocnt = 0;
		}
		*p-- = *lastbefore;
		ocnt++;
		numplace[0]--;
	}
	while (numplace[0] > 0) {
		*p-- = ' ';
		numplace[0]--;
	}
	lpush(p + 1);
	print();

	if (numplace[1] == 0) {
		outusing();
		return;
	}

	lpush(".");
	print();

	/* now dump stuff after the decimal place */
	for (p = outbuf; *decimal && isdigit(*decimal) && numplace[1] > 0; decimal++) {
		*p++ = *decimal;
		numplace[1]--;
	}
	while (numplace[1] > 0) {
		*p++ = '0';
		numplace[1]--;
	}
	*p++ = '\0';
	lpush(outbuf);
	print();
	outusing();
}

/*
 * fusing -- output the given floating point # based on what is
 * in the using string.
 */
void fusing(double f)
{
	static char buf[32];

	sprintf(buf, "%f", f);
	nusing(buf);
}

/*
 * iusing -- just like fusing except that we are given an integer.
 */
void iusing(int i)
{
	static char	buf[32];

	sprintf(buf, "%d.0", i);
	nusing(buf);
}

/*
 * susing -- a fake for now...
 */
void susing(void)
{
	static struct string dummy;

	pop(&dummy);
}
