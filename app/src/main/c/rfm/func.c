/*
 * func.c -- definitions for builtin functions of BASIC
 */

#include <string.h>
#include <stdlib.h>

#include "trs_str.h"
#include "func.h"
#include "err.h"
#include "trs_io.h"

#include <stdio.h>
#include <time.h>

int sign(int i)
{
	if (i < 0)
		return(-1);
	else if (i > 0)
		return(1);
	return(0);
}

int fsign(float f)
{
	if (f < 0)
		return(-1);
	else if (f > 0)
		return(1);
	return(0);
}

float rnd(int x)
{
	if (x == 0)
		return((float)(rand() % 1000) / 1000.0);
	if (x == 1)
		return(1);
	else
		return((float)(rand() % x + 1));
}

void time_string(void)
{
	time_t		t;
	struct tm*	tm;
	static char	buf[32];

	time(&t);
	tm = localtime(&t);

	/* haven't checked this... */
	sprintf(buf, "%02d/%02d/%02d %02d:%02d:%02d", tm->tm_mday, tm->tm_mon,
		tm->tm_year, tm->tm_hour, tm->tm_min, tm->tm_sec);
	lpush(buf);
}

/*
 * istr -- STR$ of an integer
 */
void istr(int i)
{
	iformat_push(i, 0);
}

/*
 * fstr -- STR$ of a float
 */
void fstr(double f)
{
	fformat_push(f, 0);
}

void arrflat(struct array* a)
{
	if (a->data != NULL)
		free(a->data);
	a->data = NULL;
}

void dim(struct array* a, int ndim, int size, int d1, int d2, int d3)
{
    if ((a->data = malloc(size * (d1 + 1) * (d2 + 1) * (d3 + 1))) == NULL)
		err(ARRAY_MEM);

	memset(a->data, 0, size * (d1 + 1) * (d2 + 1) * (d3 + 1));

	a->d1 = d1;
	a->d2 = d2;
	a->d3 = d3;
	a->ndim = ndim;
}

void* arr(struct array* a, int ndim, int size, int d1, int d2, int d3)
{
	if (a->data == NULL)
		switch (ndim) {
		case 1:	dim(a, 1, size, 10, 0, 0); break;
		case 2: dim(a, 2, size, 10, 10, 0); break;
		case 3: dim(a, 3, size, 10, 10, 10); break;
		default:
			err(ARRAY_LIMIT);
		}

	if (d1 < 0 || d2 < 0 || d3 < 0 || d1 > a->d1 || d2 > a->d2 || d3 > a->d3)
		err(ARRAY_BOUND);

	return((void*)(a->data + size * (d1 + (a->d1+1) * (d2 + (a->d2+1) * d3))) );
}

struct string* Sar(struct array* a, int ndim, int d1, int d2, int d3)
{
	return((struct string*)arr(a, ndim, sizeof(struct string), d1, d2, d3));
}

int* Iar(struct array* a, int ndim, int d1, int d2, int d3)
{
	return((int*)arr(a, ndim, sizeof(int), d1, d2, d3));
}

float* Far(struct array* a, int ndim, int d1, int d2, int d3)
{
	return((float*)arr(a, ndim, sizeof(float), d1, d2, d3));
}
