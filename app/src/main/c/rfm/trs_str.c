/* Basic string routines
 * A stack is provided for evaluating string expressions
 *
 * GWP:  Added an initialization routine for the string routines!!!!!
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "trs_str.h"
#include "trs_scrn.h"
#include "trs_io.h"
#include "key.h"
#include "err.h"

#define SSIZE		256		/* Max depth of stack		*/
#define SCHARS		2048	/* Max # of chars on stack	*/

struct	string foo, bar;

char	stkchar[SCHARS];
int	stklen[SSIZE];
int	stktop;
char	*stkptr;

int stkdiff(void) { return(stkptr - stkchar); }

void trs_str_init(void)
{
	stkptr = stkchar;
	stktop = -1;
}

#ifdef TEST
main()
{
	stkptr = stkchar;
	stktop = -1;
	lpush("hello, ");
	lpush("world!");
	cat();
	print();
	lpush("Foo");
	pop(&foo);
	lpush("bar");
	pop(&bar);
	push(&bar);
	push(&foo);
	cat();
	print();
	lpush("hello");
	lpush("world!");
	printf("%d", _compare());
}

#endif /* TEST */

/*
 * Make sure the stack has not blown up!
 */
static void str_sanity(void)
{
	if (stktop < -1 || stktop >= SSIZE || stkptr < stkchar || stkptr >= stkchar + SCHARS) {
		trs_str_init(); /* so that error message will work */
		err(STRING_OV);
	}
}

/* Push a chunk of memory onto the stack */

void _push(char *src, int len)
{
	memcpy(stkptr, src, len);
	stkptr += len;
	stktop++;
	stklen[stktop] = len;

	str_sanity();
}

/* Push a string variable onto the stack */

void push(struct string *s)
{
	_push(s->str_char, s->str_len);
}

/* Push a literal string onto the stack */

void lpush(char *s)
{
	_push(s, strlen(s));
}

/* Push a character onto the stack */

void cpush(int ch)
{
	char	c;

	c = ch;
	_push(&c, 1);
}

/* Pop string into a string variable */

void pop(struct string *s)
{
	int	len;

	if ((len = stklen[stktop--]) > 255)
		err(STRING2LONG);

	stkptr -= len;
	s->str_len = len;
	memcpy(s->str_char, stkptr, len);

	str_sanity();
}

/* concatenate the two strings on top of the stack */
/* due to the nature of PRINT (it concatenates all its strings
 * before calling print), we should allow a long string length.
 */
void cat(void)
{
	int	len;

	len = stklen[stktop--];
	if ((stklen[stktop] += len) > 255) {
		/*stkptr -= stklen[stktop--];
		err(7);*/
	}

	str_sanity();
}

/* Print the string on top of the stack */

void print(void)
{
	int	i;
	char	*p;

	p = stkptr - stklen[stktop];
	for (i = stklen[stktop]; i > 0; i--)
		trputc(*p++);
	stkptr -= stklen[stktop--];

	str_sanity();
}

static void midstr(int, int);

/* take the left n characters of the top string */

void left(int n)
{
	midstr(0, n - 1);
}

/* take the right n characters of the top string */

void right(int n)
{
	int	len;

	len = stklen[stktop];
	midstr(len - n, len - 1);
}

/* take the middle string of length n characters from the top string */

void mid3(int p, int n)
{
	midstr(p - 1, p + n - 2);
}

/* take the characters from p to the end of the string */

void mid2(int p)
{
	midstr(p - 1, stklen[stktop] - 1);
}

/* General purpose string extractor -- characters n1 to n2 of string */

static void midstr(int n1, int n2)
{
	int	len;
	char	*p;

	if (n1 < 0) n1 = 0;
	if (n2 >= stklen[stktop]) n2 = stklen[stktop] - 1;
	if ((len = 1 + n2 - n1) <= 0) {
		stkptr -= stklen[stktop];
		stklen[stktop] = 0;
		str_sanity();
		return;
	}
	p = stkptr - stklen[stktop];
	if (n1 != 0) memcpy(p, p + n1, len);
	stklen[stktop] = len;
	stkptr = p + len;

	str_sanity();
}

/* inkey$ */

void inkey(void)
{
	stktop++;
	if (currentkey == NUL)
		stklen[stktop] = 0;
	else {
		*stkptr++ = currentkey;
		currentkey = NUL;
		stklen[stktop] = 1;
	}

	str_sanity();
}

void strng(int n)
{
	int i;
	static struct string foo;

    left(1);
	pop(&foo);
	push(&foo);

	for (i = 0; i < n - 1; i++) {
		push(&foo);
		cat();
	}

	str_sanity();
}

/* len -- return the length of the string on the stack.  String is popped
 */
int len(void)
{
	int length;

	length = stklen[stktop--];
	stkptr -= length;
	str_sanity();
	return(length);
}

/* asc -- return the ASCII value of the first character of the string
 * on top of the stack.  String is popped.
 *
 * ASC("") is an FC error, but we will return 0 for now
 */
int asc(void)
{
	if (stklen[stktop] == 0) {
		stktop--;
		return(0);
	}

	stkptr -= stklen[stktop--];
	str_sanity();
	return(*stkptr);
}

/* val -- return the integer value to the string on top of the stack and pop
 * should be able to do floating point, etc, but simple for now.
 */
int val(void)
{
	if (stklen[stktop] == 0) {
		stktop--;
		return(0);
	}

	*stkptr = '\0';	/* is this ok to do? */

	stkptr -= stklen[stktop--];

	str_sanity();
	return(atoi(stkptr));
}

/* String comparisons -- all rely on _compare */

int _compare(void)
{
	char	*s1, *s2;
	int	len1, len2;
	int cmpret;

	len2 = stklen[stktop--];
	len1 = stklen[stktop--];
	s2 = stkptr -= len2;
	s1 = stkptr -= len1;

	str_sanity();

	if (len1 > 0 || len2 > 0)
		cmpret = memcmp(s1, s2, (len1 < len2) ? len1 : len2);
	else
		cmpret = 0;

	if (cmpret == 0) {
		if (len1 > len2)
			return(1);
		else if (len1 < len2)
			return(-1);
	}
	return(cmpret);
}

/* oops, fixed these to return -1 or 0 instead of 1 or 0 */

int sEQ(void)
{
	return -(_compare() == 0);
}

int sLT(void)
{
	return -(_compare() < 0);
}

int sGT(void)
{
	return -(_compare() > 0);
}

int sLE(void)
{
	return -(_compare() <= 0);
}

int sGE(void)
{
	return -(_compare() >= 0);
}

int sNE(void)
{
	return -(_compare() != 0);
}

