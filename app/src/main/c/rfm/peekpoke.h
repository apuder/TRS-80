/*
 * peekpoke.h
 */

extern int trs_peek(int);
extern void trs_poke(int, int);
extern void trs_out(int, int);

struct outrec {
	int port;
	int value;
};

extern int outidx;
extern struct outrec outlog[];
