/*
 * func.h -- external declarations for builtin functions defined in func.c
 */

extern int sign(int);
extern int fsign(float);
extern float rnd(int);
extern void time_string(void);
extern void fstr(double);
extern void istr(int);

struct array {
	char*	data;
	int		ndim;
	int		d1;
	int		d2;
	int		d3;
};

extern struct string*	Sar(struct array*, int, int, int, int);
extern int*				Iar(struct array*, int, int, int, int);
extern float*			Far(struct array*, int, int, int, int);
extern void				arrflat(struct array*);
extern void				dim(struct array*, int, int, int, int, int);
