#include <stdio.h>

#include "run.h"

extern unsigned char* trs_screen;
int trs_next_key(int wait);
void trs_get_event(int wait);

static int current_key = -1;

void add_key_event_basic(uint16_t event, uint16_t sym, uint16_t key)
{
	if (event == 3 /*KEY_UP*/) {
		current_key = -1;
		return;
	}
	if (key == 15) {
		key = '\r';
	}
	current_key = key;
}

int run_rfm()
{
	int ex;

	basic_init();

	ex = 0;
	while (!ex) {
		switch (basic_frame(100))
		{
		case FRAME_DONE:
			// process outlog[] up to outidx
			// delay for until next 1/60th of a second.
			break;
		case FRAME_KEY:
			{
			    /*
				trs_get_event(0);
				int ch = trs_next_key(0);
				if (ch == -1) {
					break;
				}
				if (ch == '\n')
					ch = '\r';
			     */
			    if (current_key == -1) {
			        break;
			    }
				update_input(current_key);
			    current_key = -1;
			}
			break;
		case FRAME_EXIT:
			ex = 1;
			break;
		}
	}

	return 0;
}


void put_char(int addr, unsigned char val)
{
	if (addr < 0 || addr > 1023)
		return;

	trs_screen[addr] = val;
}

void scroll()
{
	for (int i = 0; i < 1024 - 64; i++) {
		trs_screen[i] = trs_screen[i + 64];
	}
	for (int i = 0; i < 64; i++) {
		trs_screen[15 * 64 + i] = 0;
	}
}
