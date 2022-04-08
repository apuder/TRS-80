/*
 * trs_scrn.h -- routines of model III screen simulator
 */

extern void put_char(int, unsigned char);
extern void trputc(unsigned char);
extern void trs_set(int, int);
extern void trs_reset(int, int);
extern int trs_point(int, int);
extern void cls(void);
extern void setcursor(int);
extern void tabcursor(int);
extern int get_char(int);
extern void trprint(char*);

extern void scrn_init(void);

extern unsigned char trs_scrn[1024];
extern int screenmode;
