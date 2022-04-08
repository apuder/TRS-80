/*
 * fornext.h -- stuff for for-next and GOSUB
 */

struct for_elem {
	int	l;			/* line # */
	char* varname;	/* variable name */
	float	step;
	float	limit;
	float*	fvar;
	int*	ivar;
};

extern void fornext_init(void);

#define FORSTKSIZE	100
#define GSBSTKSIZE	100

extern struct for_elem fstk[FORSTKSIZE];
extern int gstk[GSBSTKSIZE];

extern struct for_elem* fsp;
extern int* gsp;

#define GOTO(x)		lnum = x; return

#define GOSUB(x,r)	gosub(x, r); return; case r:
#define RETURN		ret(); return;

/*#define FOR(v, init, tst, num, vstr)	 */
#define FOR(v, init, limit, step, num, vstr, fptr, iptr)	\
v = init;													\
rof(num, vstr, limit, step, fptr, iptr);					\
case num:

#define NEXT_	if (next(NULL)) return

#define NEXT(i)	if (next(i)) return

extern void gosub(int, int);
extern void rof(int, char*, double, double, float*, int*);
extern int next(char*);
