/*
 * peekpoke.c -- the PEEK and POKE routines.  Naturally, this has a lot
 * of tie-ins to other modules and is generally full of weirdness.
 */

#include "trs_scrn.h"
#include "key.h"
#include "peekpoke.h"

int trs_peek(int addr)
{
	if (addr >= 15360 && addr <= 16383)
		return(get_char(addr-15360));

	if (addr >= 0x3800 && addr <= 0x3880)
		return(keybits[addr - 0x3800]);

	/* BASIC copies of the keyboard matrix */
	switch (addr) {
	case 16438: return(keybits[0x01]);
	case 16439: return(keybits[0x02]);
	case 16440: return(keybits[0x04]);
	case 16441: return(keybits[0x08]);
	case 16442: return(keybits[0x10]);
	case 16443: return(keybits[0x20]);
	case 16444: return(keybits[0x40]);
	}

	return(0);
}

void trs_poke(int addr, int val)
{
	if (addr >= 15360 && addr <= 16383) {
		put_char(addr - 15360, val);
		return;
	}
}

void trs_out(int port, int val)
{
	outlog[outidx].port = port;
	outlog[outidx].value = val;
	outidx++;
}
