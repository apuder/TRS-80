char* data[] = {
	0
};
#include <math.h>
#include <stdlib.h>
#include "basic.h"
#include "trs_str.h"
#include "trs_io.h"
#include "func.h"
#include "key.h"
#include "trs_scrn.h"
#include "peekpoke.h"
#include "fornext.h"
struct string	ZZz;
struct string	Yz;
float		Zs;
struct string	Xz;
float		R4s;
float		XPs;
struct string	F1z;
float		Xs;
float		Ws;
float		R3s;
float		R2s;
float		R1s;
float		Rs;
float		Q1s;
float		R8s;
float		Gs;
float		X9s;
float		X2s;
float		X1s;
struct string	Cz;
float		Vs;
float		H1s;
float		Hs;
float		V1s;
float		Cs;
float		FAs;
float		LCs;
float		MAs;
float		FCs;
struct array	L_z;
float		L1s;
float		ZRs;
float		Ss;
float		S1s;
float		S5s;
float		D2s;
struct string	Iz;
float		OFs;
float		PLs;
float		Ns;
float		I1s;
float		Is;
float		D1s;
struct array	S_s;
struct array	S_z;
float		N1s;
float		N3s;
float		N2s;
float		I2s;
float		XZs;
struct string	Qz;
float		T1s;
float		Ls;
float		Ms;
struct string	Fz;
float		MLs;
struct string	INz;
float		ZZs;
float		Qs;
struct array	V_s;
struct array	R_s;
struct array	Y_s;
struct array	PP_z;
struct array	T_s;
struct array	A_z;
struct array	W_s;
struct array	T_z;
struct array	P_s;
struct array	PL_z;
struct array	C_s;
struct array	B_s;
struct array	Z_s;
struct array	U_s;
struct array	A_s;

void initialize_vars(void)
{
int i;
ZZz.str_len  = 0;
Yz.str_len  = 0;
Zs = 0;
Xz.str_len  = 0;
R4s = 0;
XPs = 0;
F1z.str_len  = 0;
Xs = 0;
Ws = 0;
R3s = 0;
R2s = 0;
R1s = 0;
Rs = 0;
Q1s = 0;
R8s = 0;
Gs = 0;
X9s = 0;
X2s = 0;
X1s = 0;
Cz.str_len  = 0;
Vs = 0;
H1s = 0;
Hs = 0;
V1s = 0;
Cs = 0;
FAs = 0;
LCs = 0;
MAs = 0;
FCs = 0;
arrflat(&L_z);
L1s = 0;
ZRs = 0;
Ss = 0;
S1s = 0;
S5s = 0;
D2s = 0;
Iz.str_len  = 0;
OFs = 0;
PLs = 0;
Ns = 0;
I1s = 0;
Is = 0;
D1s = 0;
arrflat(&S_s);
arrflat(&S_z);
N1s = 0;
N3s = 0;
N2s = 0;
I2s = 0;
XZs = 0;
Qz.str_len  = 0;
T1s = 0;
Ls = 0;
Ms = 0;
Fz.str_len  = 0;
MLs = 0;
INz.str_len  = 0;
ZZs = 0;
Qs = 0;
arrflat(&V_s);
arrflat(&R_s);
arrflat(&Y_s);
arrflat(&PP_z);
arrflat(&T_s);
arrflat(&A_z);
arrflat(&W_s);
arrflat(&T_z);
arrflat(&P_s);
arrflat(&PL_z);
arrflat(&C_s);
arrflat(&B_s);
arrflat(&Z_s);
arrflat(&U_s);
arrflat(&A_s);
}

int ogo;
int must_exit;
void tr_exit(void)
{
    lpush("READY\n\016>");
    print();
    lnum = -10000;
    must_exit = 1;
}

int lnum = -1;

/*---cut here---*/
void main0()
{
switch (lnum) {
case -1:
	initialize_vars();
	L = 1;
	cls();
	L = 5;
	RESTORE;
	initialize_vars();
	L = 10;
	RANDOM;
	L = 20;
	dim(&A_s, 1, sizeof(float), (5), 0, 0);
	dim(&U_s, 1, sizeof(float), (5), 0, 0);
	dim(&Z_s, 1, sizeof(float), (48), 0, 0);
	dim(&B_s, 1, sizeof(float), (48), 0, 0);
	dim(&C_s, 1, sizeof(float), (48), 0, 0);
	dim(&PL_z, 1, sizeof(struct string), (24), 0, 0);
	dim(&P_s, 1, sizeof(float), (24), 0, 0);
	dim(&T_z, 1, sizeof(struct string), (48), 0, 0);
	dim(&W_s, 1, sizeof(float), (48), 0, 0);
	dim(&A_z, 1, sizeof(struct string), (10), 0, 0);
	dim(&T_s, 1, sizeof(float), (48), 0, 0);
	L = 25;
	dim(&PP_z, 1, sizeof(struct string), (3), 0, 0);
	dim(&Y_s, 1, sizeof(float), (24), 0, 0);
	dim(&R_s, 1, sizeof(float), (24), 0, 0);
	dim(&V_s, 1, sizeof(float), (24), 0, 0);
	L = 30;
	GOSUB(230 /* 30000 */, 1);
	L = 35;
	GOSUB(239 /* 40250 */, 2);
	L = 40;
	FOR(Qs, (1), (2000), (1), 3, "Qs", &Qs, NULL);
	lnum = -2; return; case -2: ;
	NEXT("Qs");
	L = 42;
	lpush("\n");print();lnum = -3; return; case -3: ;

	lpush("\n");print();lnum = -4; return; case -4: ;

	L = 45;
	GOSUB(238 /* 40200 */, 4);
	L = 50;
	FOR(Qs, (1), (3500), (1), 5, "Qs", &Qs, NULL);
	lnum = -5; return; case -5: ;
	NEXT("Qs");
	L = 61;
	FOR(Qs, (1), (20), (1), 6, "Qs", &Qs, NULL);
	L = 62;
	
	setcursor(rnd((900))); lpush("*");lpush("\n");cat();print();lnum = -6; return; case -6: ;

	L = 63;
	trs_out(255,1);lnum = -7; return; case -7: ;

	L = 64;
	
	setcursor(rnd((950))); lpush("!");lpush("\n");cat();print();lnum = -8; return; case -8: ;

	L = 65;
	trs_out(255,2);lnum = -9; return; case -9: ;

	L = 66;
	
	setcursor(rnd((950))); lpush("GOAL!");lpush("\n");cat();print();lnum = -10; return; case -10: ;

	L = 67;
	trs_out(255,0);lnum = -11; return; case -11: ;

	L = 68;
	NEXT("Qs");
	L = 69;
	cls();
	L = 70;
	cpush((23));lpush("\n");cat();print();lnum = -12; return; case -12: ;

	lpush("\n");print();lnum = -13; return; case -13: ;

	L = 71;
	trs_out(255,1);lnum = -14; return; case -14: ;

	L = 73;
	trs_out(255,2);lnum = -15; return; case -15: ;

	L = 75;
	trs_out(255,1);lnum = -16; return; case -16: ;

	L = 76;
	lpush("\n");print();lnum = -17; return; case -17: ;

	lpush("\n");print();lnum = -18; return; case -18: ;

	lpush("*DREAMLAND SOFTWARE 2021*");lpush("\n");cat();print();lnum = -19; return; case -19: ;

	L = 77;
	trs_out(255,1);lnum = -20; return; case -20: ;

	trs_out(255,0);lnum = -21; return; case -21: ;

	L = 80;
	FOR(ZZs, (1), (2000), (1), 7, "ZZs", &ZZs, NULL);
	lnum = -22; return; case -22: ;
	NEXT("ZZs");
	L = 90;
	GOSUB(219 /* 29000 */, 8);
case 9 /* 100 */:
	L = 100;
	GOSUB(35 /* 2000 */, 10);
	L = 110;
	if ((push(&INz),left((4)),lpush("SAVE"),sEQ())) {
		GOSUB(231 /* 40000 */, 11);
		tr_exit();
	}
	L = 120;
	GOSUB(81 /* 4000 */, 12);
	L = 220;
	GOSUB(177 /* 14000 */, 13);
	L = 240;
	GOSUB(182 /* 16000 */, 14);
	L = 260;
	if (-(MLs > 0)) {
		GOSUB(200 /* 18000 */, 15);
	}
	L = 499;
	GOTO(9 /* 100 */);
	L = 990;
	FOR(Qs, (1), (2000), (1), 16, "Qs", &Qs, NULL);
	lnum = -23; return; case -23: ;
	NEXT("Qs");
	RETURN;
case 17 /* 1000 */:
	L = 1000;
	lpush("YOU HAVE :$");print();lnum = -24; return; case -24: ;

	
	push(&Fz);setusing();fusing(Ms);lpush("");susing();lpush("\n");print();lnum = -25; return; case -25: ;

	lpush("    YOU NOW OWE :$");print();lnum = -26; return; case -26: ;

	
	push(&Fz);setusing();fusing(Ls);lpush("\n");print();lnum = -27; return; case -27: ;

	RETURN;
case 18 /* 1010 */:
	L = 1010;
	lpush("YOU HAVE INSUFFICIENT MONEY");lpush("\n");cat();print();lnum = -28; return; case -28: ;

	RETURN;
case 19 /* 1020 */:
	L = 1020;
	lpush("YOUR LEAGUE POSITION IS :");fpush(*Far(&T_s, 1, (T1s), 0, 0));cat();print();lnum = -29; return; case -29: ;

	commacursor();lpush(" MATCHES PLAYED :");fpush(MLs);cat();lpush("\n");cat();print();lnum = -30; return; case -30: ;

	RETURN;
case 20 /* 1030 */:
	L = 1030;
	GOSUB(30 /* 1110 */, 21);
	
	setcursor(960); lpush("---- HIT ENTER TO CONTINUE ");print();lnum = -31; return; case -31: ;

	L = 1031;
	start_input("");sinput(&Qz);lnum = -32; return; case -32: ;

	L = 1032;
	trs_out(255,2);lnum = -33; return; case -33: ;

	trs_out(255,0);lnum = -34; return; case -34: ;

	L = 1035;
	RETURN;
case 22 /* 1040 */:
	L = 1040;
	if (-(XZs < 1)) {
		XZs = 1;
	}
	L = 1042;
	if (-(XZs > 20)) {
		XZs = 20;
	}
	L = 1044;
	RETURN;
case 23 /* 1050 */:
	L = 1050;
	cls();
	push(Sar(&A_z, 1, (((float)(I2s)+(float)(2))), 0, 0));lpush("(++ MEANS PICKED TO PLAY,-INJ- MEANS INJURED)");cat();lpush("\n");cat();print();lnum = -35; return; case -35: ;

	RETURN;
case 24 /* 1060 */:
	L = 1060;
	lpush("TYPE 0 TO SELECT NONE");lpush("\n");cat();print();lnum = -36; return; case -36: ;

	RETURN;
case 25 /* 1070 */:
	L = 1070;
	GOSUB(32 /* 1120 */, 26);
	lpush("\n");print();lnum = -37; return; case -37: ;

	lpush("PLAYERS PICKED =");fpush(N2s);cat();lpush("PLAYERS INJURED =");cat();fpush(N3s);cat();lpush("PLAYERS IN SQUAD =");cat();fpush(N1s);cat();lpush("\n");cat();print();lnum = -38; return; case -38: ;

	RETURN;
case 27 /* 1080 */:
	L = 1080;
	lpush("\n");print();lnum = -39; return; case -39: ;

	lpush("OR 	TYPE 99 TO CONTINUE GAME");lpush("\n");cat();print();lnum = -40; return; case -40: ;

	RETURN;
case 28 /* 1090 */:
	L = 1090;
	push(Sar(&S_z, 1, (1), 0, 0));print();lnum = -41; return; case -41: ;

	commacursor();fpush(*Far(&S_s, 1, (1), 0, 0));print();lnum = -42; return; case -42: ;

	commacursor();push(Sar(&S_z, 1, (2), 0, 0));print();lnum = -43; return; case -43: ;

	commacursor();fpush(*Far(&S_s, 1, (2), 0, 0));lpush("\n");cat();print();lnum = -44; return; case -44: ;

	RETURN;
case 29 /* 1100 */:
	L = 1100;
	trs_out(255,2);lnum = -45; return; case -45: ;

	trs_out(255,0);lnum = -46; return; case -46: ;

	
	commacursor();push(Sar(&A_z, 1, (6), 0, 0));fpush(D1s);cat();lpush("\n");cat();print();lnum = -47; return; case -47: ;

	RETURN;
case 30 /* 1110 */:
	L = 1110;
	FOR(Qs, (1), (100), (1), 31, "Qs", &Qs, NULL);
	lnum = -48; return; case -48: ;
	NEXT("Qs");
	trs_out(255,1);lnum = -49; return; case -49: ;

	trs_out(255,0);lnum = -50; return; case -50: ;

	RETURN;
case 32 /* 1120 */:
	L = 1120;
	N1s = 0;
	N2s = 0;
	N3s = 0;
	L = 1130;
	FOR(Qs, (1), (24), (1), 33, "Qs", &Qs, NULL);
	L = 1140;
	if (-(*Far(&P_s, 1, (Qs), 0, 0) > 0)) {
		N1s = ((float)(N1s)+(float)(1));
	}
	L = 1150;
	if (-(*Far(&P_s, 1, (Qs), 0, 0) == 2)) {
		N2s = ((float)(N2s)+(float)(1));
	}
	L = 1160;
	if (-(*Far(&P_s, 1, (Qs), 0, 0) == 3)) {
		N3s = ((float)(N3s)+(float)(1));
	}
	L = 1170;
	NEXT("Qs");
	L = 1180;
	RETURN;
case 34 /* 1200 */:
	L = 1200;
	lpush("---NOW REMOVE A PLAYER ----");lpush("\n");cat();print();lnum = -51; return; case -51: ;

	RETURN;
case 35 /* 2000 */:
	L = 2000;
	cls();
	L = 2020;
	lpush("TO DO THE FOLLOWING --");print();lnum = -52; return; case -52: ;

	commacursor();
	commacursor();lpush("TYPE:");lpush("\n");cat();print();lnum = -53; return; case -53: ;

	L = 2025;
	FOR(Is, (1), (11), (1), 36, "Is", &Is, NULL);
	lpush("-"),strng((5));print();lnum = -54; return; case -54: ;

	NEXT("Is");
	lpush("");lpush("\n");cat();print();lnum = -55; return; case -55: ;

	L = 2040;
	lpush("SELL OR LIST YOUR PLAYERS");print();lnum = -56; return; case -56: ;

	commacursor();
	commacursor();lpush("-- A");lpush("\n");cat();print();lnum = -57; return; case -57: ;

	L = 2080;
	lpush("OBTAIN A LOAN");print();lnum = -58; return; case -58: ;

	commacursor();
	commacursor();
	commacursor();lpush("-- L");lpush("\n");cat();print();lnum = -59; return; case -59: ;

	L = 2100;
	lpush("DISPLAY YOUR STATUS");print();lnum = -60; return; case -60: ;

	commacursor();
	commacursor();lpush("-- S");lpush("\n");cat();print();lnum = -61; return; case -61: ;

	L = 2120;
	lpush("PAY OFF LOAN");print();lnum = -62; return; case -62: ;

	commacursor();
	commacursor();
	commacursor();lpush("-- P");lpush("\n");cat();print();lnum = -63; return; case -63: ;

	L = 2140;
	lpush("CHECK LEAGUE TABLE");print();lnum = -64; return; case -64: ;

	commacursor();
	commacursor();lpush("-- T");lpush("\n");cat();print();lnum = -65; return; case -65: ;

	L = 2150;
	lpush("RESIGN");print();lnum = -66; return; case -66: ;

	commacursor();
	commacursor();
	commacursor();lpush("-- R");lpush("\n");cat();print();lnum = -67; return; case -67: ;

	L = 2160;
	lpush("DO NOTHING");print();lnum = -68; return; case -68: ;

	commacursor();
	commacursor();
	commacursor();lpush("-- 99");lpush("\n");cat();print();lnum = -69; return; case -69: ;

	L = 2165;
	/*  PRINT"LOAD GAME",,,"-- LOAD" */
	L = 2170;
	/*  PRINT"SAVE GAME",,,"-- SAVE" */
	L = 2175;
	trs_out(255,1);lnum = -70; return; case -70: ;

	trs_out(255,0);lnum = -71; return; case -71: ;

	L = 2180;
	start_input("");sinput(&INz);lnum = -72; return; case -72: ;

	L = 2185;
	trs_out(255,2);lnum = -73; return; case -73: ;

	trs_out(255,0);lnum = -74; return; case -74: ;

	L = 2200;
	if ((push(&INz),lpush("A"),sEQ())) {
		GOSUB(46 /* 2400 */, 37);
	}
	else {
		if ((push(&INz),lpush("L"),sEQ())) {
			GOSUB(62 /* 2800 */, 38);
		}
		else {
			if ((push(&INz),lpush("S"),sEQ())) {
				GOSUB(67 /* 3000 */, 39);
			}
			else {
				if ((push(&INz),lpush("P"),sEQ())) {
					GOSUB(75 /* 3200 */, 40);
				}
				else {
					if ((push(&INz),lpush("T"),sEQ())) {
						GOSUB(170 /* 13710 */, 41);
					}
					else {
						if ((push(&INz),lpush("99"),sEQ())) {
							GOTO(45 /* 2299 */);
						}
					}
				}
			}
		}
	}
	L = 2210;
	if ((push(&INz),lpush("R"),sEQ())) {
		GOSUB(242 /* 50000 */, 42);
	}
	L = 2220;
	if ((push(&INz),lpush("LOAD"),sEQ())) {
		GOSUB(215 /* 27000 */, 43);
	}
	L = 2230;
	if ((push(&INz),lpush("SAVE"),sEQ())) {
		GOSUB(231 /* 40000 */, 44);
	}
	L = 2250;
	GOTO(35 /* 2000 */);
case 45 /* 2299 */:
	L = 2299;
	RETURN;
case 46 /* 2400 */:
	L = 2400;
	cls();
	L = 2410;
	start_input("TYPE 1 FOR DEFENCE,  2 FOR MIDFIELD,  3 FOR ATTACK");finput(&Is);lnum = -75; return; case -75: ;

	L = 2420;
	if (-(Is < 1) | -(Is > 3)) {
		GOTO(46 /* 2400 */);
	}
	L = 2423;
	I2s = Is;
	L = 2425;
	cls();
	L = 2430;
	I1s = ((float)(1)+(float)((((float)((((float)(Is)-(float)(1))))*(float)(8)))));
	L = 2433;
	GOSUB(23 /* 1050 */, 47);
	L = 2435;
	GOSUB(60 /* 2540 */, 48);
	L = 2440;
	FOR(Ns, (I1s), (((float)(I1s)+(float)(7))), (1), 49, "Ns", &Ns, NULL);
	L = 2443;
	if (-(*Far(&P_s, 1, (Ns), 0, 0) == 0)) {
		GOTO(51 /* 2460 */);
	}
	L = 2445;
	PLs = Ns;
	L = 2450;
	GOSUB(61 /* 2550 */, 50);
case 51 /* 2460 */:
	L = 2460;
	NEXT("Ns");
case 52 /* 2470 */:
	L = 2470;
	lpush("TYPE THE NUMBER OF THE PLAYER YOU WANT TO SELL");lpush("\n");cat();print();lnum = -76; return; case -76: ;

	GOSUB(24 /* 1060 */, 53);
	start_input("");finput(&Is);lnum = -77; return; case -77: ;

	L = 2472;
	if (-(Is == 0)) {
		GOTO(59 /* 2539 */);
	}
	L = 2475;
	if (-(Is < I1s) | -(Is > ((float)(I1s)+(float)(7)))) {
		GOTO(52 /* 2470 */);
	}
	L = 2476;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 0)) {
		GOTO(52 /* 2470 */);
	}
	L = 2480;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 3)) {
		
		setcursor(788); lpush("NOBODY WANTS HIM!");print();lnum = -78; return; case -78: ;

		GOSUB(20 /* 1030 */, 54);
		GOTO(59 /* 2539 */);
	}
	L = 2483;
	OFs = floor((((float)((((float)((((float)(rnd((4)))+(float)(8))))*(float)(*Far(&V_s, 1, (Is), 0, 0)))))/(float)(10))));
	L = 2485;
	push(Sar(&T_z, 1, (rnd((48))), 0, 0));lpush(" HAVE OFFERED $");cat();print();lnum = -79; return; case -79: ;

	
	lpush("###,###");setusing();fusing(OFs);
	lpush(" FOR ");push(Sar(&PL_z, 1, (Is), 0, 0));cat();lpush("\n");cat();print();lnum = -80; return; case -80: ;

case 55 /* 2490 */:
	L = 2490;
	start_input("ACCEPT THE OFFER");sinput(&Iz);lnum = -81; return; case -81: ;

	L = 2492;
	if ((push(&Iz),lpush("YES"),sEQ()) | (push(&Iz),lpush("Y"),sEQ())) {
		GOTO(56 /* 2500 */);
	}
	L = 2494;
	if ((push(&Iz),lpush("N"),sEQ()) | (push(&Iz),lpush("NO"),sEQ())) {
		GOTO(58 /* 2535 */);
	}
	L = 2495;
	GOTO(55 /* 2490 */);
case 56 /* 2500 */:
	L = 2500;
	*Far(&P_s, 1, (Is), 0, 0) = 0;
	N1s = ((float)(N1s)-(float)(1));
	L = 2505;
	Ms = ((float)(Ms)+(float)(OFs));
	L = 2510;
	push(Sar(&PL_z, 1, (Is), 0, 0));lpush(" HAS BEEN SOLD");cat();lpush("\n");cat();print();lnum = -82; return; case -82: ;

	GOSUB(20 /* 1030 */, 57);
	GOTO(59 /* 2539 */);
case 58 /* 2535 */:
	L = 2535;
	if (-(rnd((3)) > 1)) {
		*Far(&P_s, 1, (Is), 0, 0) = 3;
		N3s = ((float)(N3s)+(float)(1));
		N2s = ((float)(N1s)-(float)(N3s));
	}
case 59 /* 2539 */:
	L = 2539;
	RETURN;
case 60 /* 2540 */:
	L = 2540;
	lpush("NAME            NUMBER   SKILL    ENERGY    VALUE(POUNDS)");lpush("\n");cat();print();lnum = -83; return; case -83: ;

	L = 2545;
	RETURN;
case 61 /* 2550 */:
	L = 2550;
	push(Sar(&PL_z, 1, (PLs), 0, 0));print();lnum = -84; return; case -84: ;

	tabcursor(17); lpush(" ");print();lnum = -85; return; case -85: ;

	
	lpush("##");setusing();fusing(PLs);
	
	tabcursor(25); lpush(" ");print();lnum = -86; return; case -86: ;

	
	lpush("##");setusing();fusing(*Far(&R_s, 1, (PLs), 0, 0));
	
	tabcursor(35); lpush(" ");print();lnum = -87; return; case -87: ;

	
	lpush("##");setusing();fusing(*Far(&Y_s, 1, (PLs), 0, 0));
	
	tabcursor(45); lpush(" ");print();lnum = -88; return; case -88: ;

	
	push(&Fz);setusing();fusing(*Far(&V_s, 1, (PLs), 0, 0));
	push(Sar(&PP_z, 1, (*Far(&P_s, 1, (PLs), 0, 0)), 0, 0));lpush("\n");cat();print();lnum = -89; return; case -89: ;

	L = 2555;
	RETURN;
case 62 /* 2800 */:
	L = 2800;
	cls();
	GOSUB(17 /* 1000 */, 63);
	start_input("TYPE AMOUNT OF LOAN REQUIRED");finput(&Is);lnum = -90; return; case -90: ;

	L = 2810;
	if (-(Is < 0)) {
		GOTO(62 /* 2800 */);
	}
	L = 2820;
	if (-(((float)(Is)+(float)(Ls)) > (((float)(250000)*(float)(D2s))))) {
		lpush("SORRY - YOUR CREDIT LIMIT IS $");print();lnum = -91; return; case -91: ;

		
		push(&Fz);setusing();fusing((((float)(250000)*(float)(D2s))));lpush("\n");print();lnum = -92; return; case -92: ;

		GOSUB(20 /* 1030 */, 64);
		GOTO(62 /* 2800 */);
	}
	L = 2830;
	Ms = ((float)(Ms)+(float)(Is));
	Ls = ((float)(Ls)+(float)(Is));
	GOSUB(17 /* 1000 */, 65);
	GOSUB(20 /* 1030 */, 66);
	L = 2840;
	RETURN;
case 67 /* 3000 */:
	L = 3000;
	cls();
	
	commacursor();push(Sar(&T_z, 1, (T1s), 0, 0));lpush("\n");cat();print();lnum = -93; return; case -93: ;

	lpush("\n");print();lnum = -94; return; case -94: ;

	L = 3004;
	if (-(S5s == 0)) {
		GOTO(68 /* 3010 */);
	}
	L = 3005;
	S1s = ((float)(floor((((float)(Ss)/(float)(S5s)))))*(float)(2));
	if (-(S1s > 100)) {
		S1s = 100;
	}
case 68 /* 3010 */:
	L = 3010;
	lpush("MANAGERIAL RATING (MAX 100):");fpush(S1s);cat();lpush("   SUCCESS POINTS :");cat();fpush(Ss);cat();lpush("\n");cat();print();lnum = -95; return; case -95: ;

	lpush("FA CUP TROPHIES WON:");fpush(ZRs);cat();lpush("\n");cat();print();lnum = -96; return; case -96: ;

	L = 3012;
	lpush("LEVEL :");fpush(L1s);cat();print();lnum = -97; return; case -97: ;

	commacursor();push(Sar(&L_z, 1, (L1s), 0, 0));print();lnum = -98; return; case -98: ;

	commacursor();lpush("SEASONS :");fpush(S5s);cat();lpush("\n");cat();print();lnum = -99; return; case -99: ;

	lpush("\n");print();lnum = -100; return; case -100: ;

	L = 3016;
	GOSUB(17 /* 1000 */, 69);
	lpush("\n");print();lnum = -101; return; case -101: ;

	L = 3020;
	push(Sar(&A_z, 1, (2), 0, 0));print();lnum = -102; return; case -102: ;

	commacursor();fpush(*Far(&A_s, 1, (2), 0, 0));lpush("\n");cat();print();lnum = -103; return; case -103: ;

	L = 3030;
	GOSUB(19 /* 1020 */, 70);
	GOSUB(25 /* 1070 */, 71);
	L = 3031;
	FOR(ZZs, (1), (150), (1), 72, "ZZs", &ZZs, NULL);
	lnum = -104; return; case -104: ;
	NEXT("ZZs");
	trs_out(255,1);lnum = -105; return; case -105: ;

	trs_out(255,0);lnum = -106; return; case -106: ;

	L = 3032;
	FCs = 0;
	FOR(ZZs, (1), (5), (1), 73, "ZZs", &ZZs, NULL);
	L = 3033;
	FCs = ((float)(FCs)+(float)(*Far(&A_s, 1, (ZZs), 0, 0)));
	L = 3034;
	NEXT("ZZs");
	L = 3035;
	lpush("CURRENT FOOTBALL FOCUS TEAM RATING:");fpush(FCs);cat();lpush("%");cat();lpush("\n");cat();print();lnum = -107; return; case -107: ;

	L = 3036;
	trs_out(255,2);lnum = -108; return; case -108: ;

	lpush("\n");print();lnum = -109; return; case -109: ;

	L = 3038;
	GOSUB(20 /* 1030 */, 74);
	L = 3040;
	RETURN;
case 75 /* 3200 */:
	L = 3200;
	cls();
	L = 3210;
	GOSUB(17 /* 1000 */, 76);
case 77 /* 3220 */:
	L = 3220;
	start_input("TYPE AMOUNT YOU WANT TO PAY OFF ($)");finput(&Is);lnum = -110; return; case -110: ;

	L = 3230;
	if (-(Is < 0) | -(Is > Ls)) {
		GOTO(77 /* 3220 */);
	}
	L = 3240;
	if (-(Ms < Is)) {
		GOSUB(18 /* 1010 */, 78);
		GOTO(77 /* 3220 */);
	}
	L = 3250;
	Ms = ((float)(Ms)-(float)(Is));
	Ls = ((float)(Ls)-(float)(Is));
	GOSUB(17 /* 1000 */, 79);
	GOSUB(20 /* 1030 */, 80);
	L = 3260;
	RETURN;
case 81 /* 4000 */:
	L = 4000;
	MAs = ((float)(MAs)+(float)(1));
	if (-(MAs == 3)) {
		LCs = 1;
	}
	else {
		LCs = 2;
	}
	L = 4010;
	if (-(FAs == 1)) {
		LCs = 2;
	}
	L = 4020;
	if (-(MAs == 3)) {
		MAs = 0;
	}
	L = 4025;
	GOSUB(83 /* 4100 */, 82);
	L = 4029;
	RETURN;
case 83 /* 4100 */:
	L = 4100;
	cls();
	L = 4110;
	if (-(LCs == 1)) {
		Cs = ((float)(Cs)+(float)(1));
	}
	else {
		MLs = ((float)(MLs)+(float)(1));
	}
case 84 /* 4120 */:
	L = 4120;
	if (-(LCs == 1)) {
		V1s = floor((((float)(((float)(((float)((((float)(9)-(float)(Cs))))*(float)(5)))+(float)(1)))-(float)(rnd((5))))));
	}
	else {
		V1s = floor((((float)(rnd((12)))+(float)((((float)((((float)(D1s)-(float)(1))))*(float)(12)))))));
	}
	L = 4130;
	if (-(V1s == T1s)) {
		GOTO(84 /* 4120 */);
	}
	L = 4140;
	FOR(Is, (1), (5), (1), 85, "Is", &Is, NULL);
	L = 4150;
	if (-(LCs == 1)) {
		*Far(&U_s, 1, (Is), 0, 0) = floor((((float)(((float)(((float)(rnd((16)))+(float)(L1s)))+(float)((((float)((((float)(T1s)-(float)(1))))/(float)(12))))))-(float)((((float)(((float)((((float)(V1s)-(float)(1))))/(float)(12)))+(float)(1)))))));
	}
	else {
		*Far(&U_s, 1, (Is), 0, 0) = floor((((float)(((float)(rnd((14)))+(float)(L1s)))+(float)((((float)(*Far(&Z_s, 1, (V1s), 0, 0))/(float)(MLs)))))));
	}
	L = 4155;
	XZs = *Far(&U_s, 1, (Is), 0, 0);
	GOSUB(22 /* 1040 */, 86);
	*Far(&U_s, 1, (Is), 0, 0) = XZs;
	L = 4160;
	NEXT("Is");
	L = 4190;
	if (-(LCs == 1)) {
		GOSUB(106 /* 4500 */, 87);
	}
	L = 4200;
	if (-(LCs == 1)) {
		Hs = rnd((2));
	}
	else {
		Hs = floor((((float)((((float)(1.5)/(float)(H1s))))+(float)(1))));
	}
	L = 4201;
	H1s = Hs;
	L = 4205;
	Vs = floor((((float)((((float)(1.5)/(float)(Hs))))+(float)(1))));
case 88 /* 4210 */:
	L = 4210;
	if (-(LCs == 1)) {
		GOSUB(107 /* 4600 */, 89);
	}
	else {
		GOSUB(108 /* 4700 */, 90);
	}
	L = 4220;
	push(Sar(&T_z, 1, (T1s), 0, 0));pop(Sar(&S_z, 1, (Hs), 0, 0));
	push(Sar(&T_z, 1, (V1s), 0, 0));pop(Sar(&S_z, 1, (Vs), 0, 0));
	*Far(&S_s, 1, (1), 0, 0) = 0;
	*Far(&S_s, 1, (2), 0, 0) = 0;
	L = 4230;
	if (-(LCs == 1)) {
		
		setcursor(9); lpush("F.A. CUP MATCH  -  ROUND ");push(&Cz);cat();lpush("\n");cat();print();lnum = -111; return; case -111: ;

	}
	else {
		
		setcursor(9); lpush("LEAGUE MATCH  -  DIVISION");fpush(D1s);cat();lpush("\n");cat();print();lnum = -112; return; case -112: ;

	}
	L = 4235;
	GOSUB(92 /* 4240 */, 91);
	GOTO(93 /* 4245 */);
case 92 /* 4240 */:
	L = 4240;
	push(Sar(&A_z, 1, (((float)(5)+(float)(LCs))), 0, 0));fpush(X1s);cat();push(Sar(&S_z, 1, (1), 0, 0));cat();lpush(" V ");cat();push(Sar(&S_z, 1, (2), 0, 0));cat();lpush(" ");cat();push(Sar(&A_z, 1, (((float)(5)+(float)(LCs))), 0, 0));cat();fpush(X2s);cat();lpush("\n");cat();print();lnum = -113; return; case -113: ;

	RETURN;
case 93 /* 4245 */:
	L = 4245;
	GOSUB(20 /* 1030 */, 94);
	L = 4250;
	GOSUB(109 /* 6000 */, 95);
	cls();
	GOSUB(141 /* 6605 */, 96);
	L = 4280;
	GOSUB(143 /* 10000 */, 97);
	L = 4300;
	lpush("***** FINAL SCORE *****");lpush("\n");cat();print();lnum = -114; return; case -114: ;

	L = 4310;
	push(Sar(&S_z, 1, (1), 0, 0));print();lnum = -115; return; case -115: ;

	commacursor();fpush(*Far(&S_s, 1, (1), 0, 0));print();lnum = -116; return; case -116: ;

	commacursor();push(Sar(&S_z, 1, (2), 0, 0));print();lnum = -117; return; case -117: ;

	commacursor();fpush(*Far(&S_s, 1, (2), 0, 0));lpush("\n");cat();print();lnum = -118; return; case -118: ;

	L = 4320;
	lpush("\n");print();lnum = -119; return; case -119: ;

	lpush("YOUR GATE RECEIPTS WERE :$");fpush(X9s);cat();lpush("\n");cat();print();lnum = -120; return; case -120: ;

	L = 4325;
	Ms = ((float)(Ms)+(float)(X9s));
	GOSUB(20 /* 1030 */, 98);
	L = 4329;
	if (-(LCs == 2)) {
		GOTO(100 /* 4340 */);
	}
	L = 4330;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) == *Far(&S_s, 1, (Vs), 0, 0))) {
		cls();
		lpush("REPLAY-----");lpush("\n");cat();print();lnum = -121; return; case -121: ;

		GOSUB(20 /* 1030 */, 99);
		cls();
		Hs = floor((((float)((((float)(1.5)/(float)(Hs))))+(float)(1))));
		Vs = floor((((float)((((float)(1.5)/(float)(Vs))))+(float)(1))));
		GOTO(88 /* 4210 */);
	}
case 100 /* 4340 */:
	L = 4340;
	if (-(LCs == 1)) {
		GOSUB(155 /* 12000 */, 101);
	}
	else {
		GOSUB(160 /* 13000 */, 102);
	}
	L = 4350;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) > *Far(&S_s, 1, (Vs), 0, 0))) {
		*Far(&A_s, 1, (2), 0, 0) = ((float)(*Far(&A_s, 1, (2), 0, 0))+(float)(floor((((float)((((float)(20)-(float)(*Far(&A_s, 1, (2), 0, 0)))))/(float)(2))))));
	}
	L = 4360;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) < *Far(&S_s, 1, (Vs), 0, 0))) {
		*Far(&A_s, 1, (2), 0, 0) = ((float)(*Far(&A_s, 1, (2), 0, 0))-(float)(floor((((float)(*Far(&A_s, 1, (2), 0, 0))/(float)(2))))));
	}
	L = 4370;
	XZs = *Far(&A_s, 1, (2), 0, 0);
	GOSUB(22 /* 1040 */, 103);
	*Far(&A_s, 1, (2), 0, 0) = XZs;
	L = 4380;
	if (-(LCs == 2)) {
		GOSUB(163 /* 13500 */, 104);
		GOSUB(170 /* 13710 */, 105);
	}
	L = 4499;
	RETURN;
case 106 /* 4500 */:
	L = 4500;
	if (-(Cs < 7)) {
		fstr((Cs));pop(&Cz);
	}
	else {
		if (-(Cs == 7)) {
			lpush("SEMI-FINAL");pop(&Cz);
		}
		else {
			if (-(Cs == 8)) {
				lpush("FINAL");pop(&Cz);
				RETURN;
			}
		}
	}
case 107 /* 4600 */:
	L = 4600;
	if (-(Hs == 1)) {
		X1s = D1s;
		X2s = ((float)(floor((((float)((((float)(V1s)-(float)(1))))/(float)(12)))))+(float)(1));
		X9s = Gs;
	}
	L = 4610;
	if (-(Hs == 2)) {
		X1s = ((float)(floor((((float)((((float)(V1s)-(float)(1))))/(float)(12)))))+(float)(1));
		X2s = D1s;
		X9s = ((float)((((float)(5)-(float)(X1s))))*(float)(10000));
	}
	L = 4620;
	if (-(Cs == 7)) {
		X9s = 50000;
	}
	else {
		if (-(Cs == 8)) {
			X9s = 100000;
		}
	}
	L = 4630;
	RETURN;
case 108 /* 4700 */:
	L = 4700;
	if (-(Hs == 1)) {
		X1s = *Far(&T_s, 1, (T1s), 0, 0);
		X2s = *Far(&T_s, 1, (V1s), 0, 0);
		X9s = Gs;
	}
	L = 4710;
	if (-(Hs == 2)) {
		X1s = *Far(&T_s, 1, (V1s), 0, 0);
		X2s = *Far(&T_s, 1, (T1s), 0, 0);
		X9s = ((float)(((float)((((float)(13)-(float)(*Far(&T_s, 1, (V1s), 0, 0)))))*(float)(D2s)))*(float)(1000));
	}
	L = 4720;
	RETURN;
case 109 /* 6000 */:
	L = 6000;
	cls();
	L = 6010;
	if (-(MLs == 1)) {
		GOTO(113 /* 6110 */);
	}
	L = 6020;
	FOR(Is, (1), (24), (1), 110, "Is", &Is, NULL);
	L = 6030;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 1)) {
		*Far(&Y_s, 1, (Is), 0, 0) = ((float)(*Far(&Y_s, 1, (Is), 0, 0))+(float)(10));
	}
	L = 6040;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 2)) {
		*Far(&Y_s, 1, (Is), 0, 0) = ((float)(*Far(&Y_s, 1, (Is), 0, 0))-(float)(1));
	}
	L = 6050;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 3)) {
		*Far(&Y_s, 1, (Is), 0, 0) = ((float)(*Far(&Y_s, 1, (Is), 0, 0))+(float)(10));
		*Far(&P_s, 1, (Is), 0, 0) = 1;
		N3s = ((float)(N3s)-(float)(1));
	}
	L = 6060;
	XZs = *Far(&Y_s, 1, (Is), 0, 0);
	GOSUB(22 /* 1040 */, 111);
	*Far(&Y_s, 1, (Is), 0, 0) = XZs;
	L = 6070;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 0)) {
		GOTO(112 /* 6100 */);
	}
	L = 6080;
	R8s = rnd((20));
	L = 6090;
	if (-(R8s == 20)) {
		*Far(&P_s, 1, (Is), 0, 0) = 3;
	}
case 112 /* 6100 */:
	L = 6100;
	NEXT("Is");
case 113 /* 6110 */:
	L = 6110;
	GOSUB(135 /* 6500 */, 114);
	GOSUB(140 /* 6600 */, 115);
	GOSUB(141 /* 6605 */, 116);
	L = 6115;
	GOSUB(25 /* 1070 */, 117);
	L = 6117;
	if (-(N2s > 11)) {
		GOSUB(34 /* 1200 */, 118);
	}
case 119 /* 6120 */:
	L = 6120;
	lpush("\n");print();lnum = -122; return; case -122: ;

	lpush("TYPE 3 FOR DEFENCE,4 FOR MIDFIELD,5 FOR ATTACK");lpush("\n");cat();print();lnum = -123; return; case -123: ;

	L = 6123;
	if (-(N2s < 12)) {
		GOSUB(27 /* 1080 */, 120);
	}
	L = 6125;
	start_input("");finput(&I1s);lnum = -124; return; case -124: ;

	L = 6126;
	trs_out(255,2);lnum = -125; return; case -125: ;

	trs_out(255,0);lnum = -126; return; case -126: ;

	L = 6127;
	if (-(N2s < 12)) {
		if (-(I1s == 99)) {
			GOTO(133 /* 6300 */);
		}
	}
	L = 6130;
	if (-(I1s < 3) | -(I1s > 5)) {
		GOTO(119 /* 6120 */);
	}
	L = 6135;
	I1s = ((float)(I1s)-(float)(2));
case 121 /* 6140 */:
	L = 6140;
	I2s = I1s;
	GOSUB(23 /* 1050 */, 122);
	GOSUB(60 /* 2540 */, 123);
	L = 6150;
	FOR(PLs, (((float)((((float)((((float)(I1s)-(float)(1))))*(float)(8))))+(float)(1))), (((float)((((float)((((float)(I1s)-(float)(1))))*(float)(8))))+(float)(8))), (1), 124, "PLs", &PLs, NULL);
	L = 6160;
	if (-(*Far(&P_s, 1, (PLs), 0, 0) == 0)) {
		GOTO(126 /* 6180 */);
	}
	L = 6170;
	GOSUB(61 /* 2550 */, 125);
case 126 /* 6180 */:
	L = 6180;
	NEXT("PLs");
	L = 6190;
	GOSUB(25 /* 1070 */, 127);
case 128 /* 6200 */:
	L = 6200;
	lpush("\n");print();lnum = -127; return; case -127: ;

	if (-(N2s < 12)) {
		lpush("TYPE NO. OF PLAYER YOU WANT TO ADD TO TEAM");lpush("\n");cat();print();lnum = -128; return; case -128: ;

	}
	else {
		lpush("TYPE NO. OF PLAYER YOU WANT TO REMOVE");lpush("\n");cat();print();lnum = -129; return; case -129: ;

	}
	L = 6205;
	GOSUB(24 /* 1060 */, 129);
	L = 6208;
	start_input("");finput(&Ns);lnum = -130; return; case -130: ;

	L = 6210;
	trs_out(255,2);lnum = -131; return; case -131: ;

	trs_out(255,0);lnum = -132; return; case -132: ;

	L = 6215;
	if (-(Ns == 0)) {
		GOTO(113 /* 6110 */);
	}
	L = 6220;
	if (-(Ns < ((float)((((float)((((float)(I1s)-(float)(1))))*(float)(8))))+(float)(1))) | -(Ns > ((float)((((float)((((float)(I1s)-(float)(1))))*(float)(8))))+(float)(8)))) {
		GOTO(128 /* 6200 */);
	}
	L = 6230;
	if (-(N2s < 12)) {
		if (-(*Far(&P_s, 1, (Ns), 0, 0) != 1)) {
			GOTO(128 /* 6200 */);
		}
	}
	L = 6235;
	if (-(N2s == 12)) {
		if (-(*Far(&P_s, 1, (Ns), 0, 0) != 2)) {
			GOTO(128 /* 6200 */);
		}
	}
	L = 6240;
	if (-(N2s < 12)) {
		*Far(&P_s, 1, (Ns), 0, 0) = 2;
	}
	L = 6245;
	if (-(N2s == 12)) {
		*Far(&P_s, 1, (Ns), 0, 0) = 1;
	}
	L = 6250;
	GOSUB(25 /* 1070 */, 130);
	L = 6260;
	if (-(N2s == 12)) {
		GOSUB(34 /* 1200 */, 131);
	}
	L = 6262;
	GOSUB(20 /* 1030 */, 132);
	L = 6270;
	if (-(N2s == 12)) {
		GOTO(113 /* 6110 */);
	}
	else {
		GOTO(121 /* 6140 */);
	}
case 133 /* 6300 */:
	L = 6300;
	FOR(Is, (1), (24), (1), 134, "Is", &Is, NULL);
	L = 6310;
	if (-(*Far(&P_s, 1, (Is), 0, 0) == 3)) {
		*Far(&P_s, 1, (Is), 0, 0) = 1;
		N3s = ((float)(N3s)-(float)(1));
	}
	L = 6320;
	NEXT("Is");
	L = 6499;
	RETURN;
case 135 /* 6500 */:
	L = 6500;
	*Far(&A_s, 1, (1), 0, 0) = 0;
	*Far(&A_s, 1, (3), 0, 0) = 0;
	*Far(&A_s, 1, (4), 0, 0) = 0;
	*Far(&A_s, 1, (5), 0, 0) = 0;
	L = 6510;
	FOR(Is, (1), (24), (1), 136, "Is", &Is, NULL);
	L = 6520;
	if (-(*Far(&P_s, 1, (Is), 0, 0) != 2)) {
		GOTO(137 /* 6550 */);
	}
	L = 6530;
	*Far(&A_s, 1, (((float)(floor((((float)((((float)(Is)-(float)(1))))/(float)(8)))))+(float)(3))), 0, 0) = ((float)(*Far(&A_s, 1, (((float)(floor((((float)((((float)(Is)-(float)(1))))/(float)(8)))))+(float)(3))), 0, 0))+(float)(*Far(&R_s, 1, (Is), 0, 0)));
	L = 6540;
	*Far(&A_s, 1, (1), 0, 0) = ((float)(*Far(&A_s, 1, (1), 0, 0))+(float)(*Far(&Y_s, 1, (Is), 0, 0)));
case 137 /* 6550 */:
	L = 6550;
	NEXT("Is");
	L = 6560;
	*Far(&A_s, 1, (1), 0, 0) = floor((((float)(*Far(&A_s, 1, (1), 0, 0))/(float)(11))));
	L = 6570;
	FOR(Is, (1), (5), (1), 138, "Is", &Is, NULL);
	XZs = *Far(&A_s, 1, (Is), 0, 0);
	GOSUB(22 /* 1040 */, 139);
	*Far(&A_s, 1, (Is), 0, 0) = XZs;
	NEXT("Is");
	L = 6580;
	RETURN;
case 140 /* 6600 */:
	L = 6600;
	cls();
	lpush("***********************   PICK TEAM   **********************");lpush("\n");cat();print();lnum = -133; return; case -133: ;

	L = 6601;
	RETURN;
case 141 /* 6605 */:
	L = 6605;
	
	commacursor();push(Sar(&T_z, 1, (T1s), 0, 0));lpush(" V ");cat();push(Sar(&T_z, 1, (V1s), 0, 0));cat();lpush("\n");cat();print();lnum = -134; return; case -134: ;

	lpush("\n");print();lnum = -135; return; case -135: ;

	L = 6610;
	lpush("ATTRIBUTE");print();lnum = -136; return; case -136: ;

	commacursor();lpush("----TEAM RATINGS----");lpush("\n");cat();print();lnum = -137; return; case -137: ;

	L = 6630;
	FOR(Is, (1), (5), (1), 142, "Is", &Is, NULL);
	fpush(Is);print();lnum = -138; return; case -138: ;

	commacursor();push(Sar(&A_z, 1, (Is), 0, 0));print();lnum = -139; return; case -139: ;

	commacursor();fpush(*Far(&A_s, 1, (Is), 0, 0));lpush(" V ");cat();fpush(*Far(&U_s, 1, (Is), 0, 0));cat();lpush("\n");cat();print();lnum = -140; return; case -140: ;

	NEXT("Is");
	RETURN;
case 143 /* 10000 */:
	L = 10000;
	
	commacursor();lpush("** GAME IN PROGRESS **");lpush("\n");cat();print();lnum = -141; return; case -141: ;

	L = 10002;
	FOR(Is, (1), (5), (1), 144, "Is", &Is, NULL);
	L = 10005;
	Ns = ((float)(rnd((100)))+(float)(((float)((((float)(*Far(&A_s, 1, (Is), 0, 0))-(float)(*Far(&U_s, 1, (Is), 0, 0)))))*(float)(5))));
	L = 10010;
	if (-(Ns < 75)) {
		GOTO(148 /* 10050 */);
	}
	L = 10020;
	*Far(&S_s, 1, (Hs), 0, 0) = ((float)(*Far(&S_s, 1, (Hs), 0, 0))+(float)(1));
	L = 10030;
	FOR(Q1s, (1), (rnd((14))), (1), 145, "Q1s", &Q1s, NULL);
	GOSUB(30 /* 1110 */, 146);
	NEXT("Q1s");
	L = 10040;
	GOSUB(28 /* 1090 */, 147);
case 148 /* 10050 */:
	L = 10050;
	Ns = ((float)(rnd((100)))+(float)(((float)((((float)(*Far(&U_s, 1, (Is), 0, 0))-(float)(*Far(&A_s, 1, (Is), 0, 0)))))*(float)(5))));
	L = 10060;
	if (-(Ns < 75)) {
		GOTO(152 /* 10100 */);
	}
	L = 10070;
	*Far(&S_s, 1, (Vs), 0, 0) = ((float)(*Far(&S_s, 1, (Vs), 0, 0))+(float)(1));
	L = 10080;
	FOR(Q1s, (1), (rnd((14))), (1), 149, "Q1s", &Q1s, NULL);
	GOSUB(30 /* 1110 */, 150);
	NEXT("Q1s");
	L = 10090;
	GOSUB(28 /* 1090 */, 151);
case 152 /* 10100 */:
	L = 10100;
	NEXT("Is");
	L = 10105;
	FOR(Q1s, (1), (rnd((14))), (1), 153, "Q1s", &Q1s, NULL);
	GOSUB(30 /* 1110 */, 154);
	NEXT("Q1s");
	L = 10110;
	RETURN;
case 155 /* 12000 */:
	L = 12000;
	cls();
	if (-(*Far(&S_s, 1, (Hs), 0, 0) < *Far(&S_s, 1, (Vs), 0, 0))) {
		FAs = 1;
		lpush("++++ YOU'RE OUT OF THE CUP ++++");lpush("\n");cat();print();lnum = -142; return; case -142: ;

		*Far(&A_s, 1, (2), 0, 0) = ((float)(*Far(&A_s, 1, (2), 0, 0))-(float)(floor((((float)(*Far(&A_s, 1, (2), 0, 0))/(float)(2))))));
		GOTO(157 /* 12030 */);
	}
	L = 12010;
	if (-(Cs < 8)) {
		lpush("***** YOU'RE THROUGH TO THE NEXT ROUND *****");lpush("\n");cat();print();lnum = -143; return; case -143: ;

	}
	else {
		GOSUB(232 /* 40100 */, 156);
	}
	L = 12020;
	*Far(&A_s, 1, (2), 0, 0) = ((float)(*Far(&A_s, 1, (2), 0, 0))+(float)(floor((((float)((((float)(20)-(float)(*Far(&A_s, 1, (2), 0, 0)))))/(float)(2))))));
case 157 /* 12030 */:
	L = 12030;
	XZs = *Far(&A_s, 1, (2), 0, 0);
	GOSUB(22 /* 1040 */, 158);
	*Far(&A_s, 1, (2), 0, 0) = XZs;
	L = 12040;
	GOSUB(20 /* 1030 */, 159);
	L = 12099;
	RETURN;
case 160 /* 13000 */:
	L = 13000;
	*Far(&B_s, 1, (T1s), 0, 0) = ((float)(*Far(&B_s, 1, (T1s), 0, 0))+(float)(*Far(&S_s, 1, (Hs), 0, 0)));
	*Far(&C_s, 1, (T1s), 0, 0) = ((float)(*Far(&C_s, 1, (T1s), 0, 0))+(float)(*Far(&S_s, 1, (Vs), 0, 0)));
	L = 13010;
	*Far(&B_s, 1, (V1s), 0, 0) = ((float)(*Far(&B_s, 1, (V1s), 0, 0))+(float)(*Far(&S_s, 1, (Vs), 0, 0)));
	*Far(&C_s, 1, (V1s), 0, 0) = ((float)(*Far(&C_s, 1, (V1s), 0, 0))+(float)(*Far(&S_s, 1, (Hs), 0, 0)));
	L = 13020;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) == *Far(&S_s, 1, (Vs), 0, 0))) {
		*Far(&Z_s, 1, (T1s), 0, 0) = ((float)(*Far(&Z_s, 1, (T1s), 0, 0))+(float)(1));
		*Far(&Z_s, 1, (V1s), 0, 0) = ((float)(*Far(&Z_s, 1, (V1s), 0, 0))+(float)(1));
		GOTO(162 /* 13499 */);
	}
	L = 13030;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) < *Far(&S_s, 1, (Vs), 0, 0))) {
		Gs = ((float)(Gs)-(float)(floor((((float)(Gs)/(float)(10))))));
	}
	L = 13040;
	if (-(*Far(&S_s, 1, (Hs), 0, 0) > *Far(&S_s, 1, (Vs), 0, 0))) {
		GOTO(161 /* 13070 */);
	}
	L = 13050;
	if (-(Gs < 1000)) {
		Gs = 1000;
	}
	L = 13063;
	*Far(&Z_s, 1, (V1s), 0, 0) = ((float)(*Far(&Z_s, 1, (V1s), 0, 0))+(float)(3));
	L = 13065;
	GOTO(162 /* 13499 */);
case 161 /* 13070 */:
	L = 13070;
	Gs = ((float)(Gs)+(float)(floor((((float)((((float)((((float)(15000)*(float)(D2s))))-(float)(Gs))))/(float)(10))))));
	L = 13090;
	*Far(&Z_s, 1, (T1s), 0, 0) = ((float)(*Far(&Z_s, 1, (T1s), 0, 0))+(float)(3));
case 162 /* 13499 */:
	L = 13499;
	RETURN;
case 163 /* 13500 */:
	L = 13500;
	cls();
	L = 13510;
	lpush("*OTHER MATCHES*");lpush("\n");cat();print();lnum = -144; return; case -144: ;

	L = 13515;
	trs_out(255,1);lnum = -145; return; case -145: ;

	trs_out(255,2);lnum = -146; return; case -146: ;

	trs_out(255,0);lnum = -147; return; case -147: ;

	L = 13520;
	GOSUB(29 /* 1100 */, 164);
	L = 13530;
	FOR(Is, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 165, "Is", &Is, NULL);
	*Far(&T_s, 1, (Is), 0, 0) = 0;
	NEXT("Is");
	L = 13540;
	*Far(&T_s, 1, (T1s), 0, 0) = 1;
	*Far(&T_s, 1, (V1s), 0, 0) = 1;
	L = 13550;
	FOR(Is, (1), (5), (1), 166, "Is", &Is, NULL);
case 167 /* 13560 */:
	L = 13560;
	Rs = ((float)(rnd((12)))+(float)(((float)((((float)(D1s)-(float)(1))))*(float)(12))));
	L = 13570;
	if (-(*Far(&T_s, 1, (Rs), 0, 0) == 1)) {
		GOTO(167 /* 13560 */);
	}
case 168 /* 13580 */:
	L = 13580;
	*Far(&T_s, 1, (Rs), 0, 0) = 1;
	R1s = ((float)(rnd((12)))+(float)(((float)((((float)(D1s)-(float)(1))))*(float)(12))));
	L = 13590;
	if (-(*Far(&T_s, 1, (R1s), 0, 0) == 1)) {
		GOTO(168 /* 13580 */);
	}
	L = 13600;
	*Far(&T_s, 1, (R1s), 0, 0) = 1;
	L = 13610;
	R2s = ((float)(((float)(floor((((float)(*Far(&Z_s, 1, (Rs), 0, 0))/(float)(MLs)))))+(float)(rnd((4)))))-(float)(1));
	L = 13620;
	R3s = ((float)(((float)(floor((((float)(*Far(&Z_s, 1, (R1s), 0, 0))/(float)(MLs)))))+(float)(rnd((4)))))-(float)(1));
	L = 13630;
	push(Sar(&T_z, 1, (Rs), 0, 0));print();lnum = -148; return; case -148: ;

	commacursor();fpush(R2s);print();lnum = -149; return; case -149: ;

	commacursor();push(Sar(&T_z, 1, (R1s), 0, 0));print();lnum = -150; return; case -150: ;

	commacursor();fpush(R3s);lpush("\n");cat();print();lnum = -151; return; case -151: ;

	L = 13640;
	*Far(&B_s, 1, (Rs), 0, 0) = ((float)(*Far(&B_s, 1, (Rs), 0, 0))+(float)(R2s));
	*Far(&C_s, 1, (Rs), 0, 0) = ((float)(*Far(&C_s, 1, (Rs), 0, 0))+(float)(R3s));
	L = 13650;
	*Far(&B_s, 1, (R1s), 0, 0) = ((float)(*Far(&B_s, 1, (R1s), 0, 0))+(float)(R3s));
	*Far(&C_s, 1, (R1s), 0, 0) = ((float)(*Far(&C_s, 1, (R1s), 0, 0))+(float)(R2s));
	L = 13660;
	if (-(R2s > R3s)) {
		*Far(&Z_s, 1, (Rs), 0, 0) = ((float)(*Far(&Z_s, 1, (Rs), 0, 0))+(float)(3));
	}
	L = 13670;
	if (-(R2s < R3s)) {
		*Far(&Z_s, 1, (R1s), 0, 0) = ((float)(*Far(&Z_s, 1, (R1s), 0, 0))+(float)(3));
	}
	L = 13680;
	if (-(R2s == R3s)) {
		*Far(&Z_s, 1, (R1s), 0, 0) = ((float)(*Far(&Z_s, 1, (R1s), 0, 0))+(float)(1));
		*Far(&Z_s, 1, (Rs), 0, 0) = ((float)(*Far(&Z_s, 1, (Rs), 0, 0))+(float)(1));
	}
	L = 13690;
	NEXT("Is");
	L = 13700;
	GOSUB(20 /* 1030 */, 169);
	L = 13705;
	RETURN;
case 170 /* 13710 */:
	L = 13710;
	cls();
	L = 13720;
	lpush("   TEAM");print();lnum = -152; return; case -152: ;

	commacursor();lpush("F");print();lnum = -153; return; case -153: ;

	commacursor();lpush("A");print();lnum = -154; return; case -154: ;

	commacursor();lpush("PTS");lpush("\n");cat();print();lnum = -155; return; case -155: ;

	L = 13730;
	FOR(Qs, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 171, "Qs", &Qs, NULL);
	*Far(&T_s, 1, (Qs), 0, 0) = 0;
	NEXT("Qs");
	L = 13740;
	FOR(Is, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 172, "Is", &Is, NULL);
	L = 13750;
	*Far(&Z_s, 1, (0), 0, 0) = -1;
	Q1s = 0;
	L = 13760;
	FOR(Qs, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 173, "Qs", &Qs, NULL);
	L = 13770;
	if (-(*Far(&T_s, 1, (Qs), 0, 0) != 0)) {
		GOTO(174 /* 13800 */);
	}
	L = 13780;
	if (-(*Far(&Z_s, 1, (Qs), 0, 0) > *Far(&Z_s, 1, (Q1s), 0, 0))) {
		Q1s = Qs;
	}
	L = 13790;
	if (-(*Far(&Z_s, 1, (Qs), 0, 0) == *Far(&Z_s, 1, (Q1s), 0, 0))) {
		if (-(((float)(*Far(&B_s, 1, (Qs), 0, 0))-(float)(*Far(&C_s, 1, (Qs), 0, 0))) > ((float)(*Far(&B_s, 1, (Q1s), 0, 0))-(float)(*Far(&C_s, 1, (Q1s), 0, 0))))) {
			Q1s = Qs;
		}
	}
case 174 /* 13800 */:
	L = 13800;
	NEXT("Qs");
	L = 13810;
	push(Sar(&T_z, 1, (Q1s), 0, 0));print();lnum = -156; return; case -156: ;

	commacursor();fpush(*Far(&B_s, 1, (Q1s), 0, 0));print();lnum = -157; return; case -157: ;

	commacursor();fpush(*Far(&C_s, 1, (Q1s), 0, 0));print();lnum = -158; return; case -158: ;

	commacursor();fpush(*Far(&Z_s, 1, (Q1s), 0, 0));lpush("\n");cat();print();lnum = -159; return; case -159: ;

	L = 13820;
	*Far(&T_s, 1, (Q1s), 0, 0) = ((float)(Is)-(float)(((float)((((float)(D1s)-(float)(1))))*(float)(12))));
	L = 13830;
	NEXT("Is");
	L = 13840;
	GOSUB(19 /* 1020 */, 175);
	GOSUB(20 /* 1030 */, 176);
	L = 13850;
	RETURN;
case 177 /* 14000 */:
	L = 14000;
	cls();
	L = 14010;
	lpush("****       BILLS TO PAY AT END OF WEEK       ****");lpush("\n");cat();print();lnum = -160; return; case -160: ;

	L = 14019;
	Ws = 0;
	L = 14020;
	FOR(Is, (1), (24), (1), 178, "Is", &Is, NULL);
	L = 14022;
	if (-(*Far(&P_s, 1, (Is), 0, 0) > 0)) {
		Ws = ((float)(Ws)+(float)((((float)(((float)(*Far(&R_s, 1, (Is), 0, 0))*(float)(100)))*(float)(D2s)))));
	}
	L = 14024;
	NEXT("Is");
	L = 14030;
	lpush("\n");print();lnum = -161; return; case -161: ;

	lpush("WAGE BILL = $");fpush(Ws);cat();lpush("\n");cat();print();lnum = -162; return; case -162: ;

	lpush("GROUND RENT = $");fpush(Xs);cat();lpush("\n");cat();print();lnum = -163; return; case -163: ;

	lpush("LOAN INTEREST = $");fpush(floor((((float)(Ls)/(float)(100)))));cat();lpush("\n");cat();print();lnum = -164; return; case -164: ;

	L = 14035;
	Ms = ((float)(((float)(((float)(Ms)-(float)(Ws)))-(float)(Xs)))-(float)(floor((((float)(Ls)/(float)(100))))));
	L = 14040;
	lpush("THIS WEEK'S BALANCE = $");print();lnum = -165; return; case -165: ;

	
	push(&F1z);setusing();fusing(((float)(((float)(Ms)-(float)(Ls)))-(float)(XPs)));lpush("\n");print();lnum = -166; return; case -166: ;

	XPs = ((float)(Ms)-(float)(Ls));
	L = 14050;
	if (-(Ms < 0)) {
		Ls = ((float)(Ls)-(float)(Ms));
		Ms = 0;
		lpush("YOU HAD TO BORROW TO PAY THE BILLS!!!");lpush("\n");cat();print();lnum = -167; return; case -167: ;

	}
	L = 14060;
	if (-(Ls > ((float)(250000)*(float)(D2s)))) {
		lpush("YOU OWE OVER $");fpush(((float)(250000)*(float)(D2s)));cat();lpush("-THE BANK IS DEMANDING REPAYMENT!");cat();lpush("\n");cat();print();lnum = -168; return; case -168: ;

		lpush("YOU'RE SACKED FOR INCOMPETENCE!!!");lpush("\n");cat();print();lnum = -169; return; case -169: ;

		FOR(Qs, (1), (999), (1), 179, "Qs", &Qs, NULL);
		lnum = -170; return; case -170: ;
		NEXT("Qs");
		RESTORE;
		initialize_vars();
		lpush("\n");print();lnum = -171; return; case -171: ;

		lpush("** RESTARTING **");lpush("\n");cat();print();lnum = -172; return; case -172: ;

		FOR(Qs, (1), (1000), (1), 180, "Qs", &Qs, NULL);
		lnum = -173; return; case -173: ;
		NEXT("Qs");
		initialize_vars();
 GOTO(-1);
	}
	L = 14070;
	GOSUB(20 /* 1030 */, 181);
	L = 14099;
	RETURN;
case 182 /* 16000 */:
	L = 16000;
	if (-(MLs < 16)) {
		GOTO(193 /* 16499 */);
	}
	L = 16010;
	if (-(FAs == 0)) {
		if (-(Cs < 8)) {
			GOTO(193 /* 16499 */);
		}
	}
	L = 16020;
	cls();
	L = 16030;
	
	commacursor();lpush("***END OF SEASON***");lpush("\n");cat();print();lnum = -174; return; case -174: ;

	GOSUB(20 /* 1030 */, 183);
	L = 16040;
	GOSUB(170 /* 13710 */, 184);
	L = 16050;
	R1s = ((float)(13)-(float)(*Far(&T_s, 1, (T1s), 0, 0)));
	L = 16060;
	cls();
	push(Sar(&A_z, 1, (8), 0, 0));print();lnum = -175; return; case -175: ;

	commacursor();fpush(R1s);lpush("\n");cat();print();lnum = -176; return; case -176: ;

	Ss = ((float)(Ss)+(float)(R1s));
	L = 16070;
	if (-(FAs == 1)) {
		R3s = ((float)(((float)(Cs)*(float)(2)))*(float)(D1s));
	}
	else {
		R3s = ((float)(16)*(float)(D1s));
	}
	L = 16080;
	push(Sar(&A_z, 1, (9), 0, 0));print();lnum = -177; return; case -177: ;

	commacursor();fpush(R3s);lpush("\n");cat();print();lnum = -178; return; case -178: ;

	L = 16090;
	Ss = ((float)(Ss)+(float)(R3s));
	R4s = floor((((float)((((float)(Ms)-(float)(Ls))))/(float)(10000))));
	L = 16100;
	if (-(R4s < 0)) {
		R4s = 0;
	}
	L = 16110;
	push(Sar(&A_z, 1, (10), 0, 0));print();lnum = -179; return; case -179: ;

	commacursor();fpush(R4s);lpush("\n");cat();print();lnum = -180; return; case -180: ;

	Ss = ((float)(Ss)+(float)(R4s));
	L = 16120;
	Ms = ((float)(Ms)+(float)(((float)(((float)(R1s)*(float)(5000)))*(float)(D2s))));
	S5s = ((float)(S5s)+(float)(1));
	L = 16125;
	GOSUB(20 /* 1030 */, 185);
	L = 16126;
	cls();
	L = 16130;
	FOR(Is, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 186, "Is", &Is, NULL);
	L = 16140;
	GOSUB(194 /* 16500 */, 187);
	GOSUB(198 /* 16700 */, 188);
	L = 16142;
	NEXT("Is");
	L = 16145;
	GOSUB(20 /* 1030 */, 189);
	L = 16150;
	GOSUB(216 /* 28000 */, 190);
	L = 16160;
	cls();
	
	commacursor();lpush("**** NEW SEASON ****");lpush("\n");cat();print();lnum = -181; return; case -181: ;

	GOSUB(29 /* 1100 */, 191);
	GOSUB(20 /* 1030 */, 192);
case 193 /* 16499 */:
	L = 16499;
	RETURN;
case 194 /* 16500 */:
	L = 16500;
	if (-(*Far(&T_s, 1, (Is), 0, 0) > 2)) {
		GOTO(197 /* 16699 */);
	}
	L = 16505;
	if (-(D1s == 1)) {
		GOTO(195 /* 16650 */);
	}
	L = 16510;
	push(Sar(&T_z, 1, (Is), 0, 0));print();lnum = -182; return; case -182: ;

	commacursor();lpush("\n");print();lnum = -183; return; case -183: ;

	L = 16520;
	if (-(*Far(&T_s, 1, (Is), 0, 0) == 2)) {
		lpush("ARE PROMOTED ****");lpush("\n");cat();print();lnum = -184; return; case -184: ;

	}
	else {
		lpush("ARE CHAMPIONS AND PROMOTED ******");lpush("\n");cat();print();lnum = -185; return; case -185: ;

	}
	L = 16530;
	push(Sar(&T_z, 1, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))-(float)(*Far(&T_s, 1, (Is), 0, 0)))), 0, 0));pop(&Xz);
	L = 16540;
	push(Sar(&T_z, 1, (Is), 0, 0));pop(Sar(&T_z, 1, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))-(float)(*Far(&T_s, 1, (Is), 0, 0)))), 0, 0));
	L = 16550;
	push(&Xz);pop(Sar(&T_z, 1, (Is), 0, 0));
	if (-(Is == T1s)) {
		T1s = (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))-(float)(*Far(&T_s, 1, (Is), 0, 0))));
	}
	L = 16560;
	GOTO(196 /* 16698 */);
case 195 /* 16650 */:
	L = 16650;
	if (-(*Far(&T_s, 1, (Is), 0, 0) == 1)) {
		push(Sar(&T_z, 1, (Is), 0, 0));print();lnum = -186; return; case -186: ;

		commacursor();lpush("ARE LEAGUE CHAMPIONS *********");lpush("\n");cat();print();lnum = -187; return; case -187: ;

	}
case 196 /* 16698 */:
	L = 16698;
	/*  */
case 197 /* 16699 */:
	L = 16699;
	RETURN;
case 198 /* 16700 */:
	L = 16700;
	if (-(D1s == 4)) {
		GOTO(199 /* 16899 */);
	}
	L = 16710;
	if (-(*Far(&T_s, 1, (Is), 0, 0) < 11)) {
		GOTO(199 /* 16899 */);
	}
	L = 16720;
	push(Sar(&T_z, 1, (Is), 0, 0));print();lnum = -188; return; case -188: ;

	commacursor();lpush("ARE RELEGATED +++++");lpush("\n");cat();print();lnum = -189; return; case -189: ;

	L = 16730;
	push(Sar(&T_z, 1, (((float)(((float)(((float)(D1s)*(float)(12)))+(float)(13)))-(float)(*Far(&T_s, 1, (Is), 0, 0)))), 0, 0));pop(&Xz);
	L = 16740;
	push(Sar(&T_z, 1, (Is), 0, 0));pop(Sar(&T_z, 1, (((float)(((float)(((float)(D1s)*(float)(12)))+(float)(13)))-(float)(*Far(&T_s, 1, (Is), 0, 0)))), 0, 0));
	push(&Xz);pop(Sar(&T_z, 1, (Is), 0, 0));
	L = 16750;
	if (-(Is == T1s)) {
		T1s = ((float)(((float)(((float)(D1s)*(float)(12)))+(float)(13)))-(float)(*Far(&T_s, 1, (Is), 0, 0)));
	}
case 199 /* 16899 */:
	L = 16899;
	RETURN;
case 200 /* 18000 */:
	L = 18000;
	cls();
	L = 18005;
	if (-(N1s > 15)) {
		lpush("*SQUAD FULL!*");lpush("\n");cat();print();lnum = -190; return; case -190: ;

		GOSUB(20 /* 1030 */, 201);
		GOTO(213 /* 18499 */);
	}
case 202 /* 18010 */:
	L = 18010;
	R1s = rnd((24));
	if (-(*Far(&P_s, 1, (R1s), 0, 0) > 0)) {
		GOTO(202 /* 18010 */);
	}
case 203 /* 18020 */:
	L = 18020;
	
	commacursor();lpush("** BUY PLAYER **");lpush("\n");cat();print();lnum = -191; return; case -191: ;

	lpush("\n");print();lnum = -192; return; case -192: ;

	GOSUB(17 /* 1000 */, 204);
	lpush("\n");print();lnum = -193; return; case -193: ;

	push(Sar(&A_z, 1, (((float)(floor((((float)((((float)(R1s)-(float)(1))))/(float)(8)))))+(float)(3))), 0, 0));lpush("\n");cat();print();lnum = -194; return; case -194: ;

	lpush("\n");print();lnum = -195; return; case -195: ;

	GOSUB(60 /* 2540 */, 205);
	PLs = R1s;
	GOSUB(61 /* 2550 */, 206);
	lpush("\n");print();lnum = -196; return; case -196: ;

	L = 18030;
	lpush("TYPE YOUR BID($)");lpush("\n");cat();print();lnum = -197; return; case -197: ;

	GOSUB(27 /* 1080 */, 207);
	start_input("");finput(&Ns);lnum = -198; return; case -198: ;

	L = 18035;
	trs_out(255,2);lnum = -199; return; case -199: ;

	trs_out(255,0);lnum = -200; return; case -200: ;

	L = 18040;
	if (-(Ns == 99)) {
		GOTO(213 /* 18499 */);
	}
	L = 18050;
	if (-(Ms < Ns)) {
		GOSUB(18 /* 1010 */, 208);
		GOSUB(20 /* 1030 */, 209);
		cls();
		GOTO(203 /* 18020 */);
	}
	L = 18060;
	Rs = ((float)(((float)(rnd((10)))*(float)(Ns)))/(float)(*Far(&V_s, 1, (R1s), 0, 0)));
	L = 18070;
	if (-(Rs < 5)) {
		GOSUB(214 /* 18500 */, 210);
		GOSUB(20 /* 1030 */, 211);
		cls();
		GOTO(203 /* 18020 */);
	}
	L = 18080;
	push(Sar(&PL_z, 1, (R1s), 0, 0));print();lnum = -201; return; case -201: ;

	commacursor();lpush("HAS JOINED YOUR TEAM");lpush("\n");cat();print();lnum = -202; return; case -202: ;

	L = 18090;
	*Far(&V_s, 1, (R1s), 0, 0) = ((float)(((float)(*Far(&R_s, 1, (R1s), 0, 0))*(float)(5000)))*(float)(D2s));
	Ms = ((float)(Ms)-(float)(Ns));
	*Far(&P_s, 1, (R1s), 0, 0) = 1;
	N1s = ((float)(N1s)+(float)(1));
	GOSUB(20 /* 1030 */, 212);
case 213 /* 18499 */:
	L = 18499;
	RETURN;
case 214 /* 18500 */:
	L = 18500;
	*Far(&V_s, 1, (R1s), 0, 0) = floor((((float)(*Far(&V_s, 1, (R1s), 0, 0))+(float)(((float)(*Far(&V_s, 1, (R1s), 0, 0))/(float)(5))))));
	lpush("YOUR BID IS REFUSED-");lpush("\n");cat();print();lnum = -203; return; case -203: ;

	RETURN;
case 215 /* 27000 */:
	L = 27000;
	start_input("READY TAPE TO INPUT DATA THEN HIT ENTER");sinput(&Iz);lnum = -204; return; case -204: ;

	L = 27010;
	/* INPUT#-1,M,L,ML,N2,N3,N1,D1,A(2),D2,S1,L1 */
	L = 27020;
	/* INPUT#-1,S,MA,FA,C,H,V,H1,G,W,X,XP,S5,T1 */
	L = 27030;
	/* FORI=1TO48:INPUT#-1,T(I),T$(I),B(I),C(I),Z(I):NEXTI */
	L = 27040;
	/* FORI=1TO24:INPUT#-1,P(I),R(I),Y(I),V(I):NEXTI */
	L = 27070;
	RETURN;
case 216 /* 28000 */:
	L = 28000;
	D1s = floor((((float)((((float)((((float)(T1s)-(float)(1))))/(float)(12))))+(float)(1))));
	L = 28005;
	D2s = ((float)(5)-(float)(D1s));
	Zs = 0;
	FAs = 0;
	MAs = 0;
	MLs = 0;
	L = 28010;
	FOR(Is, (1), (48), (1), 217, "Is", &Is, NULL);
	*Far(&Z_s, 1, (Is), 0, 0) = 0;
	*Far(&T_s, 1, (Is), 0, 0) = 0;
	*Far(&B_s, 1, (Is), 0, 0) = 0;
	*Far(&C_s, 1, (Is), 0, 0) = 0;
	NEXT("Is");
	L = 28020;
	FOR(Is, (1), (24), (1), 218, "Is", &Is, NULL);
	L = 28030;
	*Far(&V_s, 1, (Is), 0, 0) = ((float)(((float)(5000)*(float)(D2s)))*(float)(rnd((5))));
	*Far(&R_s, 1, (Is), 0, 0) = ((float)(*Far(&V_s, 1, (Is), 0, 0))/(float)((((float)(5000)*(float)(D2s)))));
	L = 28040;
	*Far(&Y_s, 1, (Is), 0, 0) = rnd((20));
	NEXT("Is");
	L = 28050;
	*Far(&A_s, 1, (2), 0, 0) = 10;
	Gs = ((float)(5000)*(float)(D2s));
	Cs = 0;
	Xs = ((float)(500)*(float)(D2s));
	H1s = 2;
	L = 28060;
	RETURN;
case 219 /* 29000 */:
	L = 29000;
	/*  */
case 220 /* 29010 */:
	L = 29010;
	D1s = 0;
case 221 /* 29020 */:
	L = 29020;
	D1s = ((float)(D1s)+(float)(1));
	L = 29030;
	if (-(D1s > 4)) {
		GOTO(220 /* 29010 */);
	}
	L = 29035;
	cls();
	lpush("NUMBER");print();lnum = -205; return; case -205: ;

	commacursor();lpush("NAME");lpush("\n");cat();print();lnum = -206; return; case -206: ;

	L = 29040;
	FOR(Is, (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))), (((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12))), (1), 222, "Is", &Is, NULL);
	L = 29050;
	fpush(Is);print();lnum = -207; return; case -207: ;

	commacursor();push(Sar(&T_z, 1, (Is), 0, 0));lpush("\n");cat();print();lnum = -208; return; case -208: ;

	NEXT("Is");
case 223 /* 29060 */:
	L = 29060;
	start_input("TYPE TEAM NUMBER (*OR 99 FOR MORE CHOICE*)");finput(&T1s);lnum = -209; return; case -209: ;

	L = 29065;
	trs_out(255,1);lnum = -210; return; case -210: ;

	trs_out(255,0);lnum = -211; return; case -211: ;

	L = 29070;
	if (-(T1s == 99)) {
		GOTO(221 /* 29020 */);
	}
	L = 29080;
	if (-(T1s < ((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(1))) | -(T1s > ((float)(((float)((((float)(D1s)-(float)(1))))*(float)(12)))+(float)(12)))) {
		GOTO(223 /* 29060 */);
	}
	L = 29090;
	D1s = 4;
	push(Sar(&T_z, 1, (48), 0, 0));pop(&Yz);
	push(Sar(&T_z, 1, (T1s), 0, 0));pop(Sar(&T_z, 1, (48), 0, 0));
	push(&Yz);pop(Sar(&T_z, 1, (T1s), 0, 0));
	T1s = 48;
	L = 29100;
	cls();
	FOR(Is, (1), (7), (1), 224, "Is", &Is, NULL);
	push(Sar(&L_z, 1, (Is), 0, 0));print();lnum = -212; return; case -212: ;

	commacursor();fpush(Is);lpush("\n");cat();print();lnum = -213; return; case -213: ;

	NEXT("Is");
case 225 /* 29110 */:
	L = 29110;
	start_input("TYPE YOUR LEVEL (1-7)");finput(&L1s);lnum = -214; return; case -214: ;

	L = 29120;
	if (-(L1s < 1) | -(L1s > 7)) {
		GOTO(225 /* 29110 */);
	}
	L = 29130;
	FOR(Is, (1), (12), (1), 226, "Is", &Is, NULL);
case 227 /* 29140 */:
	L = 29140;
	Rs = rnd((24));
	L = 29150;
	if (-(*Far(&P_s, 1, (Rs), 0, 0) > 0)) {
		GOTO(227 /* 29140 */);
	}
	L = 29160;
	*Far(&P_s, 1, (Rs), 0, 0) = 2;
	L = 29170;
	if (-(Is == 12)) {
		*Far(&P_s, 1, (Rs), 0, 0) = 1;
	}
	L = 29180;
	NEXT("Is");
	L = 29190;
	Ms = 100000;
	GOSUB(29 /* 1100 */, 228);
	GOSUB(216 /* 28000 */, 229);
	L = 29200;
	RETURN;
case 230 /* 30000 */:
	L = 30000;
	lpush("P.SCHMEICHEL");pop(Sar(&PL_z, 1, (1), 0, 0));
	L = 30010;
	lpush("S.STAUNTON");pop(Sar(&PL_z, 1, (2), 0, 0));
	L = 30020;
	lpush("T.ADAMS");pop(Sar(&PL_z, 1, (3), 0, 0));
	L = 30030;
	lpush("S.PEARCE");pop(Sar(&PL_z, 1, (4), 0, 0));
	L = 30040;
	lpush("P.MCGRATH");pop(Sar(&PL_z, 1, (5), 0, 0));
	L = 30050;
	lpush("D.WALKER");pop(Sar(&PL_z, 1, (6), 0, 0));
	L = 30060;
	lpush("T.BUTCHER");pop(Sar(&PL_z, 1, (7), 0, 0));
	L = 30070;
	lpush("L.MATTHAUS");pop(Sar(&PL_z, 1, (8), 0, 0));
	L = 30080;
	lpush("Z.ZIDANE");pop(Sar(&PL_z, 1, (9), 0, 0));
	L = 30090;
	lpush("G.ZOLA");pop(Sar(&PL_z, 1, (10), 0, 0));
	L = 30100;
	lpush("R.GIGGS");pop(Sar(&PL_z, 1, (11), 0, 0));
	L = 30110;
	lpush("M.VAN BASTEN");pop(Sar(&PL_z, 1, (12), 0, 0));
	L = 30120;
	lpush("D.BECKHAM");pop(Sar(&PL_z, 1, (13), 0, 0));
	L = 30130;
	lpush("J.BARNES");pop(Sar(&PL_z, 1, (14), 0, 0));
	L = 30140;
	lpush("P.GASCOIGNE");pop(Sar(&PL_z, 1, (15), 0, 0));
	L = 30150;
	lpush("J.GREALISH");pop(Sar(&PL_z, 1, (16), 0, 0));
	L = 30160;
	lpush("G.LINEKER");pop(Sar(&PL_z, 1, (17), 0, 0));
	L = 30170;
	lpush("R.MILLA");pop(Sar(&PL_z, 1, (18), 0, 0));
	L = 30180;
	lpush("D.MARADONA");pop(Sar(&PL_z, 1, (19), 0, 0));
	L = 30190;
	lpush("I.WRIGHT");pop(Sar(&PL_z, 1, (20), 0, 0));
	L = 30200;
	lpush("A.SHEARER");pop(Sar(&PL_z, 1, (21), 0, 0));
	L = 30210;
	lpush("H.KANE");pop(Sar(&PL_z, 1, (22), 0, 0));
	L = 30220;
	lpush("R.BAGGIO");pop(Sar(&PL_z, 1, (23), 0, 0));
	L = 30230;
	lpush("M.SALAH");pop(Sar(&PL_z, 1, (24), 0, 0));
	L = 30300;
	lpush("BEGINNER (I CAN BE CHRIS KAMARA.)");pop(Sar(&L_z, 1, (1), 0, 0));
	L = 30310;
	lpush("NOVICE (A DUDE WITH A BROLLY.)   ");pop(Sar(&L_z, 1, (2), 0, 0));
	L = 30320;
	lpush("MEDIOCRE (ROY HODGSON ROCKS!)   ");pop(Sar(&L_z, 1, (3), 0, 0));
	L = 30330;
	lpush("GOOD (I'LL NEVER 'TURNIP' IN.)   ");pop(Sar(&L_z, 1, (4), 0, 0));
	L = 30340;
	lpush("EXPERT (MOVE OVER KLOPP & PEP.)   ");pop(Sar(&L_z, 1, (5), 0, 0));
	L = 30350;
	lpush("SUPER EXPERT (I THINK I'M FERGIE!)");pop(Sar(&L_z, 1, (6), 0, 0));
	L = 30360;
	lpush("GENIUS (I THINK I'M BRIAN CLOUGH!)");pop(Sar(&L_z, 1, (7), 0, 0));
	L = 30400;
	lpush("ENERGY   ");pop(Sar(&A_z, 1, (1), 0, 0));
	L = 30410;
	lpush("MORALE   ");pop(Sar(&A_z, 1, (2), 0, 0));
	L = 30420;
	lpush("DEFENCE  ");pop(Sar(&A_z, 1, (3), 0, 0));
	L = 30430;
	lpush("MIDFIELD ");pop(Sar(&A_z, 1, (4), 0, 0));
	L = 30440;
	lpush("ATTACK   ");pop(Sar(&A_z, 1, (5), 0, 0));
	L = 30450;
	lpush("LEAGUE SUCCESS PTS.");pop(Sar(&A_z, 1, (8), 0, 0));
	L = 30460;
	lpush("CUP SUCCESS PTS.");pop(Sar(&A_z, 1, (9), 0, 0));
	L = 30470;
	lpush("FINANCIAL SUCCESS PTS.");pop(Sar(&A_z, 1, (10), 0, 0));
	L = 30520;
	lpush("CHELSEA ");pop(Sar(&T_z, 1, (1), 0, 0));
	L = 30530;
	lpush("WEST HAM ");pop(Sar(&T_z, 1, (2), 0, 0));
	L = 30540;
	lpush("LIVERPOOL ");pop(Sar(&T_z, 1, (3), 0, 0));
	L = 30550;
	lpush("MAN UTD ");pop(Sar(&T_z, 1, (4), 0, 0));
	L = 30560;
	lpush("ARSENAL ");pop(Sar(&T_z, 1, (5), 0, 0));
	L = 30570;
	lpush("NOTTM FOREST ");pop(Sar(&T_z, 1, (6), 0, 0));
	L = 30580;
	lpush("SOUTHAMPTON ");pop(Sar(&T_z, 1, (7), 0, 0));
	L = 30590;
	lpush("ASTON VILLA ");pop(Sar(&T_z, 1, (8), 0, 0));
	L = 30600;
	lpush("WEST BROM ");pop(Sar(&T_z, 1, (9), 0, 0));
	L = 30610;
	lpush("EVERTON ");pop(Sar(&T_z, 1, (10), 0, 0));
	L = 30620;
	lpush("COVENTRY ");pop(Sar(&T_z, 1, (11), 0, 0));
	L = 30630;
	lpush("TOTTENHAM ");pop(Sar(&T_z, 1, (12), 0, 0));
	L = 30640;
	lpush("NORWICH ");pop(Sar(&T_z, 1, (13), 0, 0));
	L = 30650;
	lpush("MAN CITY ");pop(Sar(&T_z, 1, (14), 0, 0));
	L = 30660;
	lpush("LEEDS UTD ");pop(Sar(&T_z, 1, (15), 0, 0));
	L = 30670;
	lpush("NEWCASTLE ");pop(Sar(&T_z, 1, (16), 0, 0));
	L = 30680;
	lpush("LEICESTER ");pop(Sar(&T_z, 1, (17), 0, 0));
	L = 30690;
	lpush("BLACKBURN ");pop(Sar(&T_z, 1, (18), 0, 0));
	L = 30700;
	lpush("SHEFF UTD ");pop(Sar(&T_z, 1, (19), 0, 0));
	L = 30710;
	lpush("C PALACE ");pop(Sar(&T_z, 1, (20), 0, 0));
	L = 30720;
	lpush("WOLVES ");pop(Sar(&T_z, 1, (21), 0, 0));
	L = 30730;
	lpush("CHARLTON ");pop(Sar(&T_z, 1, (22), 0, 0));
	L = 30740;
	lpush("DERBY ");pop(Sar(&T_z, 1, (23), 0, 0));
	L = 30750;
	lpush("BOURNEMOUTH ");pop(Sar(&T_z, 1, (24), 0, 0));
	L = 30760;
	lpush("BHAM CITY ");pop(Sar(&T_z, 1, (25), 0, 0));
	L = 30770;
	lpush("FULHAM ");pop(Sar(&T_z, 1, (26), 0, 0));
	L = 30780;
	lpush("PORTSMOUTH ");pop(Sar(&T_z, 1, (27), 0, 0));
	L = 30790;
	lpush("CRYSTAL PALACE ");pop(Sar(&T_z, 1, (28), 0, 0));
	L = 30800;
	lpush("SALFORD CITY ");pop(Sar(&T_z, 1, (29), 0, 0));
	L = 30810;
	lpush("SHEFF WEDS ");pop(Sar(&T_z, 1, (30), 0, 0));
	L = 30820;
	lpush("AFC WIMBLEDON ");pop(Sar(&T_z, 1, (31), 0, 0));
	L = 30830;
	lpush("IPSWICH TOWN ");pop(Sar(&T_z, 1, (32), 0, 0));
	L = 30840;
	lpush("DERBY COUNTY ");pop(Sar(&T_z, 1, (33), 0, 0));
	L = 30850;
	lpush("BURNLEY ");pop(Sar(&T_z, 1, (34), 0, 0));
	L = 30860;
	lpush("NEWPORT CTY ");pop(Sar(&T_z, 1, (35), 0, 0));
	L = 30870;
	lpush("BRENTFORD ");pop(Sar(&T_z, 1, (36), 0, 0));
	L = 30880;
	lpush("IPSWICH ");pop(Sar(&T_z, 1, (37), 0, 0));
	L = 30890;
	lpush("FG ROVERS ");pop(Sar(&T_z, 1, (38), 0, 0));
	L = 30900;
	lpush("SHREWSBURY ");pop(Sar(&T_z, 1, (39), 0, 0));
	L = 30910;
	lpush("STOKE CITY ");pop(Sar(&T_z, 1, (40), 0, 0));
	L = 30920;
	lpush("BRIGHTON ");pop(Sar(&T_z, 1, (41), 0, 0));
	L = 30930;
	lpush("TRANMERE ");pop(Sar(&T_z, 1, (42), 0, 0));
	L = 30940;
	lpush("TORQUAY ");pop(Sar(&T_z, 1, (43), 0, 0));
	L = 30950;
	lpush("HULL ");pop(Sar(&T_z, 1, (44), 0, 0));
	L = 30960;
	lpush("PRESTON NE ");pop(Sar(&T_z, 1, (45), 0, 0));
	L = 30970;
	lpush("MK DONS ");pop(Sar(&T_z, 1, (46), 0, 0));
	L = 30980;
	lpush("WATFORD ");pop(Sar(&T_z, 1, (47), 0, 0));
	L = 30990;
	lpush("WALSALL ");pop(Sar(&T_z, 1, (48), 0, 0));
	L = 31000;
	lpush("   ");pop(Sar(&PP_z, 1, (1), 0, 0));
	L = 31010;
	lpush("  ++");pop(Sar(&PP_z, 1, (2), 0, 0));
	L = 31020;
	lpush("  -INJ-");pop(Sar(&PP_z, 1, (3), 0, 0));
	L = 31025;
	lpush("##,###,###");pop(&Fz);
	L = 31026;
	lpush("+##,###,###");pop(&F1z);
	L = 31030;
	lpush("DIVISION:");pop(Sar(&A_z, 1, (6), 0, 0));
	lpush("LEAGUE POS.:");pop(Sar(&A_z, 1, (7), 0, 0));
	L = 31040;
	RETURN;
case 231 /* 40000 */:
	L = 40000;
	start_input("READY DATA TAPE TO RECORD THEN HIT ENTER");sinput(&Iz);lnum = -215; return; case -215: ;

	L = 40010;
	/* PRINT#-1,M,L,ML,N2,N3,N1,D1,A(2),D2,S1,L1 */
	L = 40015;
	/* PRINT#-1,S,MA,FA,C,H,V,H1,G,W,X,XP,S5,T1 */
	L = 40020;
	/* FORI=1TO48:PRINT#-1,T(I),T$(I),B(I),C(I),Z(I):NEXTI */
	L = 40030;
	/* FORI=1TO24:PRINT#-1,P(I),R(I),Y(I),V(I):NEXTI */
	L = 40060;
	RETURN;
case 232 /* 40100 */:
	L = 40100;
	FOR(ZZs, (1), (500), (1), 233, "ZZs", &ZZs, NULL);
	lnum = -216; return; case -216: ;
	NEXT("ZZs");
	L = 40110;
	cls();
	L = 40115;
	/*  CODING BY CAT MANTRA AND KEVIN TOMS */
	L = 40117;
	/*  GRAPHICS BY GEORGE PHILLIPS */
	L = 40120;
	FOR(ZZs, (1), (75), (1), 234, "ZZs", &ZZs, NULL);
	L = 40130;
	lpush("******** YOU'VE WON THE F.A.CUP!!!!! ********");lpush("\n");cat();print();lnum = -217; return; case -217: ;

	L = 40140;
	trs_out(255,1);lnum = -218; return; case -218: ;

	trs_out(255,2);lnum = -219; return; case -219: ;

	trs_out(255,0);lnum = -220; return; case -220: ;

	L = 40150;
	NEXT("ZZs");
	L = 40152;
	FOR(ZZs, (1), (750), (1), 235, "ZZs", &ZZs, NULL);
	lnum = -221; return; case -221: ;
	NEXT("ZZs");
	cls();
	cpush((23));lpush("\n");cat();print();lnum = -222; return; case -222: ;

	lpush("\n");print();lnum = -223; return; case -223: ;

	lpush("\n");print();lnum = -224; return; case -224: ;

	lpush("*CHAMPIONS*");lpush("\n");cat();print();lnum = -225; return; case -225: ;

	trs_out(255,1);lnum = -226; return; case -226: ;

	trs_out(255,0);lnum = -227; return; case -227: ;

	FOR(ZZs, (1), (1500), (1), 236, "ZZs", &ZZs, NULL);
	lnum = -228; return; case -228: ;
	NEXT("ZZs");
	L = 40155;
	ZRs = ((float)(ZRs)+(float)(1));
	L = 40160;
	cls();
	GOSUB(240 /* 40300 */, 237);
	L = 40170;
	RETURN;
case 238 /* 40200 */:
	L = 40200;
	lpush("");lpush("\n");cat();print();lnum = -229; return; case -229: ;

	L = 40201;
	lpush("Ð¼¼¼¼¼¼Á¼¼œŒŒ„ˆŒ¼¼œŒÁ¸¼¼¼¼¼´Á ¸¼Œ¼¼´");lpush("\n");cat();print();lnum = -230; return; case -230: ;

	L = 40202;
	lpush("Ïª¿ŸÁ°¿¿…ª¿¿Åª¿ŸÂ¨¿¿ ¿¿…¨¿¿Áº¿¿");lpush("\n");cat();print();lnum = -231; return; case -231: ;

	L = 40203;
	lpush("Î ¿¿…ª¿¿Â¿¿‡ƒƒÁ ¿¿…Â¾¿•ª¿¿Â¿¿•Á ¿¿…");lpush("\n");cat();print();lnum = -232; return; case -232: ;

	L = 40204;
	lpush("Îª¿—ÁŠ¿¿”ª¿¿¼¼œÂ¾¿—Âª¿ŸÂ¿¿•Á¿½¼¿");lpush("\n");cat();print();lnum = -233; return; case -233: ;

	L = 40205;
	lpush("");lpush("\n");cat();print();lnum = -234; return; case -234: ;

	L = 40206;
	lpush("Ä¨¼¼ŒŒŒÁ°¼œŒ¼¼Á ¸¼œ¬¼´ÁˆŒ¬¼¼Œ„ ¼¼Œ¼¼Â¸¼œŒ¼¼”Á¨¼¼Ä¼¼„");lpush("\n");cat();print();lnum = -235; return; case -235: ;

	L = 40207;
	lpush("Ä¿¿•Ãº¿ŸÂº¿• ¿¿…Á ¿¿Â ¿¿…Â¾¿µ°º¿ŸÁº¿¿°°¿¿…Á¿¿…Ãº¿Ÿ");lpush("\n");cat();print();lnum = -236; return; case -236: ;

	L = 40208;
	lpush("Ãª¿Ÿƒƒƒ ¿¿Á¨¿ŸÁº¿—Â¾¿—Âº¿ŸÂª¿Ÿƒƒ¿¿” ¿¿ƒƒ«¿ŸÁº¿ŸÃ ¿¿");lpush("\n");cat();print();lnum = -237; return; case -237: ;

	L = 40209;
	lpush("Ã¿¿…Ã‚¿¼¼ŸÁ‹¯½¼¾‡Â¨¿¿Â¿¿½¼¾‡Á¾¿—Â¿¿Á¿¿½¼¼Áª¿¿¼¼”");lpush("\n");cat();print();lnum = -238; return; case -238: ;

	L = 40210;
	lpush("");lpush("\n");cat();print();lnum = -239; return; case -239: ;

	L = 40211;
	lpush("Ã¼¼œ¬¼¼œ¬¼¼Â¸¼œŒ¼¼”Á¨¼¼Œ¬¼¼Â°¼œŒ¼¼´Á ¸¼œŒŒŒÂ¼¼œŒŒ„¨¼¼¼¼¼´");lpush("\n");cat();print();lnum = -240; return; case -240: ;

	L = 40212;
	lpush("Â¨¿¿¨¿¿Áª¿¿Áª¿¿Á ¿¿•Á¿¿…Á¨¿¿Á¨¿¿Â¾¿• ¿¿…Åª¿¿Ä¾¿•Áº¿Ÿ");lpush("\n");cat();print();lnum = -241; return; case -241: ;

	L = 40213;
	lpush("Â¾¿•Á¿¿•Á¿¿… ¿¿‡ƒ«¿ŸÁª¿ŸÂ¿¿…Á¿¿‡ƒ«¿¿Áº¿ŸÁ ¿¿… ¿¿‡ƒƒÁ¨¿¿Á¿¿•");lpush("\n");cat();print();lnum = -242; return; case -242: ;

	L = 40214;
	lpush("Áª¿¿Áª¿ŸÁª¿ŸÁº¿—Â¿¿…Á¿¿…Áª¿ŸÁª¿ŸÂ¿¿…Á‹¿½¼¾¿‡Áª¿¿¼¼”Á¿¿…Á«¿¿");lpush("\n");cat();print();lnum = -243; return; case -243: ;

	L = 40215;
	lpush("");print();lnum = -244; return; case -244: ;

	L = 40216;
	RETURN;
case 239 /* 40250 */:
	L = 40250;
	lpush("Ò °¼Œ£“ƒÊ‚ƒ“Œ¬´°");lpush("\n");cat();print();lnum = -245; return; case -245: ;

	L = 40251;
	lpush("Î ¸œ£±¼¾¿¿¿”Ìª¿¿¿¼¼²“¬´");lpush("\n");cat();print();lnum = -246; return; case -246: ;

	L = 40252;
	lpush("Ì¸ž‡±¼¿¿¿¿¿¿Ÿ‡¸¿¿¿¿¿¿¿¿¿¿¿¿´ƒ¯¿¿¿¿¿½¼²‹­´");lpush("\n");cat();print();lnum = -247; return; case -247: ;

	L = 40253;
	lpush("Ê¸ŸƒÁ¯¿¿Ÿ‡ƒ¨¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿„£ƒ¯¿Á‹­´");lpush("\n");cat();print();lnum = -248; return; case -248: ;

	L = 40254;
	lpush("É¾‡Å¸¼¼¼¿¿¿´’¯¿¿¿¿¿¿¿¿¿¿¿¿‡±¾¿¿¿¿¿¼”Å‹½");lpush("\n");cat();print();lnum = -249; return; case -249: ;

	L = 40255;
	lpush("È¾Å¸¿¿¿¿¿¿¿¿¿„Ì®¿¿¿¿¿¿¿¿¿”Å‹½");lpush("\n");cat();print();lnum = -250; return; case -250: ;

	L = 40256;
	lpush("Çº•Å¸¿¿¿¿¿¿¿¿¿‡Î¯¿¿¿¿¿¿¿¿¿”Å«”");lpush("\n");cat();print();lnum = -251; return; case -251: ;

	L = 40257;
	lpush("Ç¿Å¸¿¿¿¿¿¿¿¿¿‡Ð¯¿¿¿¿¿¿¿¿¿”Äª•");lpush("\n");cat();print();lnum = -252; return; case -252: ;

	L = 40258;
	lpush("Ç¿¿¿¿½°‹¯¿¿¿¿¿‡Ò¿¿¿¿¿¿Ÿ¡¸¿¿¿•ª•");lpush("\n");cat();print();lnum = -253; return; case -253: ;

	L = 40259;
	lpush("Çªµ«¿¿¿¿¿´ƒ³°¼¼¼½¼°Ì°¸¼½¼¼´²³¼¿¿¿¿¿¾…");lpush("\n");cat();print();lnum = -254; return; case -254: ;

	L = 40260;
	lpush("È¯´«¿¿¿¿¿Á¿¿¿¿¿¿¿¿¿¿¼°Ä°¸¼¿¿¿¿¿¿¿¿¿¿Á¿¿¿¿¿…º‡");lpush("\n");cat();print();lnum = -255; return; case -255: ;

	L = 40261;
	lpush("É‹´‹¿¿¿Á¿¿¿¿¿¿¿¿¿¿¿¿¿”¨¿¿¿¿¿¿¿¿¿¿¿¿¿Á¿¿Ÿ¡ž");lpush("\n");cat();print();lnum = -256; return; case -256: ;

	L = 40262;
	lpush("Ê‚¯´Åƒ‹¿¿¿¿¿¿¿¿¿•ª¿¿¿¿¿¿¿¿¿ƒÅ¡¸‡");lpush("\n");cat();print();lnum = -257; return; case -257: ;

	L = 40263;
	lpush("Ìƒ¼°Ç‚‹ƒ£³°°³ƒ‹ƒÇ°œ‡");lpush("\n");cat();print();lnum = -258; return; case -258: ;

	L = 40264;
	lpush("Ïƒ‹¬°°Å ¾¿¿¿¿¿¿¿¿¿¿¼Æ°¸Œ‡");lpush("\n");cat();print();lnum = -259; return; case -259: ;

	L = 40265;
	lpush("ÓƒƒŒŒ´²³³“‡³³±¸ŒŒ‡ƒ");print();lnum = -260; return; case -260: ;

	L = 40266;
	RETURN;
case 240 /* 40300 */:
	L = 40300;
	lpush("Þˆ¿€");lpush("\n");cat();print();lnum = -261; return; case -261: ;

	L = 40301;
	lpush("Ýˆ¯¿¿");lpush("\n");cat();print();lnum = -262; return; case -262: ;

	L = 40302;
	lpush("Û ¸¾½½½¿½´");lpush("\n");cat();print();lnum = -263; return; case -263: ;

	L = 40303;
	lpush("Ó°°°Â Ž³³³³›Â°°°");lpush("\n");cat();print();lnum = -264; return; case -264: ;

	L = 40304;
	lpush("Ò¾¯¿¿Ÿ¯¼½ŸŸŸ›››››››ŸŸ¾¿Ÿ¯¿¿¯”");lpush("\n");cat();print();lnum = -265; return; case -265: ;

	L = 40305;
	lpush("Ò¿º¿¿Áƒ«¾¾¾¿¿¿¿¿¿¿¾¾žƒª¿¿º…");lpush("\n");cat();print();lnum = -266; return; case -266: ;

	L = 40306;
	lpush("Ò‚¿¿¿µÃ«¿¿¯­­­¯¯¿¿Â°¾¿¿•");lpush("\n");cat();print();lnum = -267; return; case -267: ;

	L = 40307;
	lpush("Ó¿¿¿—‹¬°¾¿¶··Ÿ···¾¿´¸Ž¿¿¿•");lpush("\n");cat();print();lnum = -268; return; case -268: ;

	L = 40308;
	lpush("Ò¨¿¿¿… ž¿¯›»»›»·§··§Ÿ¿¬«¿¿½");lpush("\n");cat();print();lnum = -269; return; case -269: ;

	L = 40309;
	lpush("Òª¿¿¿€¿›µ¯š¿¿ª¿¿•¿¿¥Ÿº§¿ª¿¿¿");lpush("\n");cat();print();lnum = -270; return; case -270: ;

	L = 40310;
	lpush("Òº¿¿¿€‚‹Ž½»¶Ÿ­›§ž¯¹·ž‡ª¿¿¿");lpush("\n");cat();print();lnum = -271; return; case -271: ;

	L = 40311;
	lpush("Ñ ¿¿¿¿Â ¸¼¾»·¿»·¿»»¼¼´Âª¿¿¿•");lpush("\n");cat();print();lnum = -272; return; case -272: ;

	L = 40312;
	lpush("Ñª¿¿¿¿Á¶³°°°°°°°°°°°°°²³”ª¿Ÿ¿¿");lpush("\n");cat();print();lnum = -273; return; case -273: ;

	L = 40313;
	lpush("Ñ¾Ÿ‚¯‡ ˜¤’¯Á‚¯");lpush("\n");cat();print();lnum = -274; return; case -274: ;

	L = 40314;
	lpush("Ñ‡Â¸ž¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿Ÿ½");lpush("\n");cat();print();lnum = -275; return; case -275: ;

	L = 40315;
	lpush("Ô¯´°°°°°°°°°°°°°°°°°°°°¾…");print();lnum = -276; return; case -276: ;

	L = 40320;
	FOR(ZZs, (1), (2000), (1), 241, "ZZs", &ZZs, NULL);
	lnum = -277; return; case -277: ;
	NEXT("ZZs");
	L = 40330;
	RETURN;
case 242 /* 50000 */:
	L = 50000;
	trs_out(255,2);lnum = -278; return; case -278: ;

	trs_out(255,0);lnum = -279; return; case -279: ;

	L = 50010;
	FOR(ZZs, (1), (500), (1), 243, "ZZs", &ZZs, NULL);
	lnum = -280; return; case -280: ;
	NEXT("ZZs");
	L = 50020;
	lpush("ARE YOU SURE? (Y/N)");print();lnum = -281; return; case -281: ;

	start_input("");sinput(&ZZz);lnum = -282; return; case -282: ;

	L = 50030;
	if ((push(&ZZz),lpush("N"),sEQ())) {
		GOTO(248 /* 50100 */);
	}
	L = 50040;
	FOR(ZZs, (1), (500), (1), 244, "ZZs", &ZZs, NULL);
	lnum = -283; return; case -283: ;
	NEXT("ZZs");
	L = 50050;
	if (-(FAs > 75)) {
		GOTO(246 /* 50080 */);
	}
	L = 50060;
	lpush("I DON'T BLAME YOU. YOU WEREN'T REALLY ON THE RADAR FOR THE      ENGLAND JOB.");lpush("\n");cat();print();lnum = -284; return; case -284: ;

	lpush("\n");print();lnum = -285; return; case -285: ;

	L = 50070;
	FOR(Qs, (1), (750), (1), 245, "Qs", &Qs, NULL);
	lnum = -286; return; case -286: ;
	NEXT("Qs");
case 246 /* 50080 */:
	L = 50080;
	lpush("*RESETTING GAME ENGINE*");lpush("\n");cat();print();lnum = -287; return; case -287: ;

	FOR(Qs, (1), (1000), (1), 247, "Qs", &Qs, NULL);
	lnum = -288; return; case -288: ;
	NEXT("Qs");
	L = 50090;
	RESTORE;
	initialize_vars();
	initialize_vars();
 GOTO(-1);
case 248 /* 50100 */:
	L = 50100;
	FOR(ZZs, (1), (500), (1), 249, "ZZs", &ZZs, NULL);
	lnum = -289; return; case -289: ;
	NEXT("ZZs");
	L = 50110;
	lpush("RETURNING TO MAIN SELECTION.");lpush("\n");cat();print();lnum = -290; return; case -290: ;

	L = 50120;
	FOR(ZZs, (1), (500), (1), 250, "ZZs", &ZZs, NULL);
	lnum = -291; return; case -291: ;
	NEXT("ZZs");
	RETURN;
	tr_exit();
} /* end switch */
}
