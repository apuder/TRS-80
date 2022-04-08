/*
 * trs_scrn.c -- system independent screen routines.
 */

#include <stdio.h>
#include <stdlib.h>

#include "trs_scrn.h"
#include "run.h"


/* Shadow of actual image */

unsigned char trs_scrn[1024];

int	curpos, curon, curchar, curunder;
int screenmode = 0; /* 0 = normal, 1 = chr$(23) */
int comp_spec = 0; /* 0 = compression, 1 = special characters */

void scrn_init(void)
{
	curpos = 0;
	curon = 0;
	curchar = '_';
}

void cls(void)
{
	trputc(28);
	trputc(31);
}

int get_char(int pos)
{
	if (pos < 0 || pos > 1024) return -1;
	return trs_scrn[pos];
}

void clr_eol(int cp)
{
	do {
		trs_scrn[cp] = ' ';
                put_char(cp++, ' ');
	} while (cp & 63);
}

void clr_eos(int cp)
{
	clr_eol(cp);
	cp = (cp + 64) & ~63;
	while (cp < 1024) {
		clr_eol(cp);
		cp += 64;
	}
}

/* Does the TAB statement of BASIC.  Needs chr$(23) support. */
void tabcursor(int pos)
{
	int	i;
	int	num_char;

	num_char = pos - (curpos & 63);
	for (i = 0; i < num_char; i++)
		trputc(32);
}

/* This does "," for PRINT.   Needs chr$(23) support added. */
void commacursor(void)
{
	int i;
	int	num_char;

	num_char = 16 - (curpos & 15);
	for (i = 0; i < num_char; i++)
		trputc(' ');
}

void setcursor(int pos)
{
	if (pos < 0 || pos > 1023) return; /* should be run-time error */
	if (curon)
		put_char(curpos, curunder);
	curpos = pos;
	if (curon) {
		curunder = trs_scrn[curpos];
		put_char(curpos, curchar);
	}
}

void rewrite23(void);
void rewrite64(void);

void trputc(unsigned char ch)
{
	if (curon)
		put_char(curpos, curunder);

	if (ch < 32) {
		switch (ch) {
		case 8:
			curpos = (curpos - 1 - screenmode) & 1023;
			put_char(curpos, ' ');
			break;
		case 10:
		case 13:
			curpos = (curpos & ~63) + 64;
			if (curpos >= 1024) {
				scroll();
				curpos = 960;
			}
			else
				clr_eol(curpos);
			break;
		case 14:
		case 15:
			curon = 15 - ch;	/* i.e. 14 = on, 15 = off */
			break;
		case 21:
			comp_spec = !comp_spec;
			break;
		case 23:
			if (screenmode == 0)
				rewrite23();
			screenmode = 1;
			curpos = (curpos + 1) & 0x3fe;
			break;
		case 24:
			curpos = (curpos & 0x3c0) | ((curpos - 1 - screenmode) & 0x3f);
			break;
		case 25:
			curpos = (curpos & 0x3c0) | ((curpos + 1 + screenmode) & 0x3f);
			break;
		case 26:
			curpos = (curpos + 64) & 0x3ff;
			break;
		case 27:
			curpos = (curpos - 64) & 0x3ff;
			break;
		case 28:
			if (screenmode == 1)
				rewrite64();
			screenmode = 0;
			curpos = 0;
			break;
		case 29:
			curpos &= ~63;
		case 30:
			clr_eol(curpos);
			break;
		case 31:
			clr_eos(curpos);
			break;
		}
	}
	else if (ch > 191 && !comp_spec) { /* space compression code */
		while (ch-- > 192)
			trputc(' ');
	}
	else {
		put_char(curpos++, ch);
		if (screenmode == 1)
			curpos++;

		if (curpos >= 1024) {
			scroll();
			curpos = 960;
		}
	}

	if (curon) {
		curunder = trs_scrn[curpos];
		put_char(curpos, curchar);
	}
}

void rewrite23(void)
{
	int	i;

	for (i = 0; i < 1024; i += 2)
		put_char(i, trs_scrn[i]);
}

void rewrite64(void)
{
	int	i;

	for (i = 0; i < 1024; i++)
		put_char(i, trs_scrn[i]);
}

void trprint(char *s)
{
	while (*s != 0) trputc(*s++);
}

int tpxl(int, int, int);

void trs_set(int x, int y)
{
	tpxl(x, y, 1);
}

void trs_reset(int x, int y)
{
	tpxl(x, y, 0);
}

int trs_point(int x, int y)
{
	return tpxl(x, y, 2);
}

static unsigned char tbits[] = { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20 } ;

/* GWP: changed to return -1 for point set (instead of the bit) */
/* GWP: returns 0 when point off screen */
int tpxl(x, y, mode)
int	x, y, mode;
{
	int	pos, ch, bit;

	if (x < 0 || x > 127 || y < 0 || y > 47) return(0);
	pos = x / 2 + (y / 3) * 64;
	ch = trs_scrn[pos];
	if (ch < 128 || ch > 191)
		ch = 128;
	bit = x % 2 + 2 * (y % 3);

	switch (mode) {
	case 0:
		put_char(pos, ch & ~tbits[bit]);
		break;
	case 1:
		put_char(pos, ch | tbits[bit]);
		break;
	case 2:
		return (ch & tbits[bit]) ? -1 : 0;
	}
	return(0); /* doesn't matter */
}
