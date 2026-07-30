/*
 * Copyright 2025, Arno Puder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Rasterizes the emulated screen into a coverage mask the host can upload as a
 * single image, replacing the per-character blitting each host used to do.
 *
 * Two properties of the machine shape this file.
 *
 * There are no frames. A TRS-80 program writes into video RAM whenever it likes
 * and the display reflects it immediately; there is no vsync and no moment at
 * which the screen is "complete". So there is nothing to synchronise with, and
 * the host simply samples the screen at its own rate.
 *
 * Consequently the sampling has to stay where it already is. Video RAM is
 * written by the CPU thread with no synchronisation, and read here from the
 * host's render thread. Rasterizing on the CPU thread instead would turn a
 * benign race into an ugly one: today a torn read yields the old character or
 * the new one, never anything invalid, whereas a half-written *glyph* would be
 * visibly corrupt. So trs80_render() snapshots video RAM first and rasterizes
 * from the snapshot. The race is confined to that memcpy and stays exactly as
 * coarse as it has always been, and the mask itself is only ever touched by the
 * caller's thread.
 *
 * Rasterizing here also costs the guest nothing, which matters because input is
 * polled by the guest on its own schedule: stealing CPU-thread time would show
 * up directly as input latency.
 */

#include <string.h>

#include "trs80_core.h"
#include "trs_iodefs.h"

/* The character generator ROMs, in trs_chars.c. */
extern char trs_char_data[][MAXCHARS][TRS_CHAR_HEIGHT];

/* Set by trs_sdl_interface.c from the emulated model. */
extern int trs_charset;
extern int trs_model;

/* Video RAM, one byte per cell. Written by the CPU thread. */
extern unsigned char *trs_screen;

int trs80_is_expanded_mode(void);

#define CELLS (TRS80_SCREEN_COLS * TRS80_SCREEN_ROWS)

/*
 * Codes 128..191 are the 2x3 block graphics. The Model I generated them in
 * hardware rather than storing them, so those entries of its character ROM are
 * empty; they are drawn here for every model so the result does not depend on
 * which ROM happens to be selected.
 */
#define FIRST_GRAPHICS_CHAR 128
#define LAST_GRAPHICS_CHAR 191

#define FOREGROUND 0xff
#define BACKGROUND 0x00

static unsigned char pixels[TRS80_PIXEL_WIDTH * TRS80_PIXEL_HEIGHT];

/* The cells the mask was last built from, for diffing. */
static unsigned char rendered_cells[CELLS];
static int rendered_expanded = -1;

/* Forces a full rasterize on the first call and after a mode change. */
static int mask_valid = 0;

unsigned char *trs80_pixel_buffer(void)
{
    return pixels;
}

void trs80_invalidate_render(void)
{
    mask_valid = 0;
}

/* Fills the rectangle [x, x + w) x [y, y + h) of the mask with value. */
static void fill(int x, int y, int w, int h, unsigned char value)
{
    for (int row = y; row < y + h; row++) {
        memset(pixels + row * TRS80_PIXEL_WIDTH + x, value, (size_t) w);
    }
}

/*
 * Draws the block-graphics character code at the given pixel origin.
 *
 * Each of the low six bits lights one cell of a 2x3 grid, in the order
 * top-left, top-right, middle-left, middle-right, bottom-left, bottom-right.
 * That is the same mapping the Android host used, so the graphics look
 * unchanged.
 */
static void draw_graphics_char(int code, int x, int y, int cell_width)
{
    const int mid_x = cell_width / 2;
    const int upper_y = TRS80_CELL_HEIGHT / 3;
    const int lower_y = TRS80_CELL_HEIGHT / 3 * 2;

    fill(x, y, cell_width, TRS80_CELL_HEIGHT, BACKGROUND);

    if (code & 0x01) fill(x, y, mid_x, upper_y, FOREGROUND);
    if (code & 0x02) fill(x + mid_x, y, cell_width - mid_x, upper_y, FOREGROUND);
    if (code & 0x04) fill(x, y + upper_y, mid_x, lower_y - upper_y, FOREGROUND);
    if (code & 0x08) fill(x + mid_x, y + upper_y, cell_width - mid_x, lower_y - upper_y, FOREGROUND);
    if (code & 0x10) fill(x, y + lower_y, mid_x, TRS80_CELL_HEIGHT - lower_y, FOREGROUND);
    if (code & 0x20)
        fill(x + mid_x, y + lower_y, cell_width - mid_x, TRS80_CELL_HEIGHT - lower_y, FOREGROUND);
}

/*
 * Draws a text character from the selected character ROM.
 *
 * In the ROM data each row is one byte and bit n is column n, so bit 0 is the
 * leftmost pixel. Only the first eight rows carry glyph data; the remaining
 * four are the leading between text lines.
 *
 * @param scale 1 normally, 2 in expanded mode, where each column is doubled.
 */
static void draw_text_char(int code, int x, int y, int scale)
{
    const char *glyph = trs_char_data[trs_charset][code];

    for (int row = 0; row < TRS80_CELL_HEIGHT; row++) {
        unsigned char bits = (row < TRS_CHAR_HEIGHT) ? (unsigned char) glyph[row] : 0;
        unsigned char *out = pixels + (y + row) * TRS80_PIXEL_WIDTH + x;
        for (int col = 0; col < TRS80_CELL_WIDTH; col++) {
            unsigned char value = ((bits >> col) & 1) ? FOREGROUND : BACKGROUND;
            for (int repeat = 0; repeat < scale; repeat++) {
                *out++ = value;
            }
        }
    }
}

static void draw_cell(int code, int x, int y, int scale)
{
    if (code >= FIRST_GRAPHICS_CHAR && code <= LAST_GRAPHICS_CHAR) {
        draw_graphics_char(code, x, y, TRS80_CELL_WIDTH * scale);
    } else {
        draw_text_char(code, x, y, scale);
    }
}

/*
 * The Radio Shack lowercase modification: on a Model I the codes below 0x20
 * displayed as uppercase letters.
 */
static int adjust_for_model(int code)
{
    if (trs_model == 1 && code < 0x20) {
        return code + 0x40;
    }
    return code;
}

int trs80_render(void)
{
    unsigned char snapshot[CELLS];

    /*
     * The only place the CPU thread is raced, and deliberately so: a cell read
     * here is either the character that was there before the guest's write or
     * the one after it, never a mixture. Everything below works from this copy,
     * so a write landing mid-rasterize cannot produce a broken glyph.
     */
    memcpy(snapshot, trs_screen, sizeof(snapshot));

    const int expanded = trs80_is_expanded_mode() ? 1 : 0;
    const int scale = expanded ? 2 : 1;
    /* Expanded mode shows 32 double-width characters, taking every other cell. */
    const int columns = TRS80_SCREEN_COLS / scale;

    /* A mode change moves every character, so nothing can be reused. */
    if (expanded != rendered_expanded) {
        mask_valid = 0;
        rendered_expanded = expanded;
    }

    int changed = 0;
    for (int row = 0; row < TRS80_SCREEN_ROWS; row++) {
        for (int column = 0; column < columns; column++) {
            const int index = row * TRS80_SCREEN_COLS + column * scale;
            const unsigned char code = snapshot[index];
            if (mask_valid && rendered_cells[index] == code) {
                continue;
            }
            draw_cell(adjust_for_model(code),
                      column * TRS80_CELL_WIDTH * scale,
                      row * TRS80_CELL_HEIGHT,
                      scale);
            rendered_cells[index] = code;
            changed = 1;
        }
    }
    mask_valid = 1;
    return changed;
}
