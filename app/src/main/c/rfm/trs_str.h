/* Header file for string routines      */

struct	string {
	int	str_len;
	char	str_char[256];
} ;

extern void trs_str_init(void);
extern void push(struct string *s);
extern void lpush(char *s);
extern void cpush(int ch);
extern void pop(struct string *s);
extern void cat(void);
extern void print(void);
extern void left(int n);
extern void right(int n);
extern void mid3(int p, int n);
extern void mid2(int p);
extern void inkey(void);
extern void strng(int n);
extern int len(void);
extern int asc(void);

extern int sEQ(void);
extern int sLT(void);
extern int sGT(void);
extern int sLE(void);
extern int sGE(void);
extern int sNE(void);
