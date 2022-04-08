/*
 * trs_io.h -- external declarations of routines found in trs_io.c
 */

extern char* READ(void);
extern void err(int);
extern void fpush(double);
extern void ipush(int);
extern void fformat_push(double, int);
extern void iformat_push(int, int);

extern void start_input(char*);
extern void update_input(int ch);
extern void iinput(int*);
extern void finput(float*);
extern void sinput(struct string*);
extern int need_input;

extern void setusing(void);
extern void fusing(double);
extern void iusing(int);
extern void susing(void);

extern void break_hit(void);
