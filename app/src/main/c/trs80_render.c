/*
 * Copyright The TRS-80 App Authors
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

#include <stdlib.h>
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

/* In trs_sdl_interface.c: whether port 0xec has switched in the alternate set. */
int is_alternate_mode(void);

#define CELLS (TRS80_SCREEN_COLS * TRS80_SCREEN_ROWS)

/*
 * Codes 128..191 are the 2x3 block graphics. The Model I generated them in
 * hardware rather than storing them, so those entries of its character ROM are
 * empty; they are drawn here for every model so the result does not depend on
 * which ROM happens to be selected.
 */
#define FIRST_GRAPHICS_CHAR 128
#define LAST_GRAPHICS_CHAR 191

/*
 * The Model III and 4 ROMs keep 64 more glyphs at 128..191, behind the block
 * graphics: the symbols -- card suits, faces, Greek letters -- that codes
 * 192..255 show. What the ROM stores at 192..255 itself is the alternate set,
 * Katakana or accented letters depending on the ROM, which a program has to
 * switch in through port 0xec. Games draw with the symbols, so getting this
 * backwards turns their sprites into Katakana. The cache holds the symbols as a
 * bank of their own after the 256 codes.
 */
#define SYMBOLS MAXCHARS
#define GLYPHS (MAXCHARS + 64)

#define FOREGROUND 0xff
#define BACKGROUND 0x00

/*
 * The mask, and the size a character cell is drawn at.
 *
 * The cell size is the host's, not the character ROM's. Rasterizing straight to
 * it is what keeps the strokes even: the ROM's glyphs are 8x12 one-bit bitmaps,
 * and a host scaling those up by a fraction - 1.75 on a 1008px-wide phone - lands
 * each one-pixel stem on either one or two output pixels depending on where it
 * falls, so stems come out visibly uneven and thin ones disappear. No filtering
 * at draw time fixes that; nearest-neighbour drops columns and bilinear turns the
 * same stems into fringes of differing weight. Scaling once, here, with area
 * coverage gives every stem the same ink wherever it lands, which is what the
 * host's own font rasterizer used to do before the core took the job over.
 */
static unsigned char *pixels = NULL;
static int pixel_width = TRS80_PIXEL_WIDTH;
static int pixel_height = TRS80_PIXEL_HEIGHT;
static int cell_width = TRS80_CELL_WIDTH;
static int cell_height = TRS80_CELL_HEIGHT;

/*
 * Every glyph pre-scaled to the cell size, so drawing one is a row-by-row copy.
 * Rebuilt only when the cell size or the character ROM changes, which is on
 * rotation or a new session rather than per frame.
 */
static unsigned char *glyph_cache = NULL;

/* The character ROM the cache was built from, or -1 before the first build. */
static int cached_charset = -1;

/* The cells the mask was last built from, for diffing. */
static unsigned char rendered_cells[CELLS];
static int rendered_expanded = -1;
static int rendered_alternate = -1;

/* Forces a full rasterize on the first call and after a mode change. */
static int mask_valid = 0;

unsigned char *trs80_pixel_buffer(void)
{
    if (pixels == NULL) {
        trs80_set_cell_size(TRS80_CELL_WIDTH, TRS80_CELL_HEIGHT);
    }
    return pixels;
}

int trs80_pixel_width(void)
{
    return pixel_width;
}

int trs80_pixel_height(void)
{
    return pixel_height;
}

/*
 * The coverage of one destination pixel of a glyph, as the fraction of it that
 * the source glyph fills.
 *
 * A box filter: the destination pixel maps back to a rectangle of source pixels,
 * and each source pixel contributes in proportion to how much of that rectangle
 * it occupies. Partial overlaps at the edges are what make a stem that falls
 * between output pixels weigh the same as one that lands squarely on it.
 */
static unsigned char glyph_coverage(const char *glyph, int dx, int dy)
{
    /*
     * Integer arithmetic throughout, in units of 1/cell_width of a source pixel
     * horizontally and 1/cell_height vertically, so no rounding creeps in. It
     * matters: at 1.75x a stem's edge pixel is covered exactly half, and in
     * floating point that lands either side of the threshold depending on the
     * glyph, which is precisely the uneven-stem problem this is here to avoid.
     */
    const int x_lo = dx * TRS80_CELL_WIDTH;
    const int x_hi = (dx + 1) * TRS80_CELL_WIDTH;
    const int y_lo = dy * TRS80_CELL_HEIGHT;
    const int y_hi = (dy + 1) * TRS80_CELL_HEIGHT;

    long covered = 0;
    for (int sy = 0; sy < TRS80_CELL_HEIGHT; sy++) {
        const int top = (sy * cell_height > y_lo) ? sy * cell_height : y_lo;
        const int bottom = ((sy + 1) * cell_height < y_hi) ? (sy + 1) * cell_height : y_hi;
        if (bottom <= top) {
            continue;
        }
        const unsigned char bits = (sy < TRS_CHAR_HEIGHT) ? (unsigned char) glyph[sy] : 0;
        if (bits == 0) {
            continue;
        }
        for (int sx = 0; sx < TRS80_CELL_WIDTH; sx++) {
            if (((bits >> sx) & 1) == 0) {
                continue;
            }
            const int left = (sx * cell_width > x_lo) ? sx * cell_width : x_lo;
            const int right = ((sx + 1) * cell_width < x_hi) ? (sx + 1) * cell_width : x_hi;
            if (right > left) {
                covered += (long) (right - left) * (bottom - top);
            }
        }
    }

    /*
     * Majority coverage rather than the fraction itself, which is what keeps the
     * strokes even and hard-edged the way the host's font rasterizer used to make
     * them. Taking the fraction would be the faithful resample, but a one-pixel
     * stem then lands as one solid pixel between two half-lit ones and reads as
     * thinner than a stem falling squarely. Asking whether each output pixel is
     * at least half covered gives that stem the same width wherever it falls --
     * two pixels at 1.75x, at every one of the eight phases -- which is what font
     * hinting does. Ties round up, hence >=.
     */
    /* One destination pixel spans this much, in the units used above. */
    const long area = (long) TRS80_CELL_WIDTH * TRS80_CELL_HEIGHT;
    return (covered * 2 >= area) ? FOREGROUND : BACKGROUND;
}

/* Draws the 2x3 block graphics of code straight into the cache, at cell size. */
static void cache_graphics_char(unsigned char *dest, int code)
{
    const int mid_x = cell_width / 2;
    const int upper_y = cell_height / 3;
    const int lower_y = cell_height / 3 * 2;

    memset(dest, BACKGROUND, (size_t) cell_width * cell_height);
    for (int y = 0; y < cell_height; y++) {
        const int band = (y < upper_y) ? 0 : (y < lower_y ? 1 : 2);
        for (int x = 0; x < cell_width; x++) {
            const int half = (x < mid_x) ? 0 : 1;
            if (code & (1 << (band * 2 + half))) {
                dest[y * cell_width + x] = FOREGROUND;
            }
        }
    }
}

/* Rasterizes every glyph of the current character ROM at the current cell size. */
static void build_glyph_cache(void)
{
    for (int glyph = 0; glyph < GLYPHS; glyph++) {
        unsigned char *dest = glyph_cache + (size_t) glyph * cell_width * cell_height;
        if (glyph >= FIRST_GRAPHICS_CHAR && glyph <= LAST_GRAPHICS_CHAR) {
            cache_graphics_char(dest, glyph);
            continue;
        }
        const int code = (glyph < SYMBOLS) ? glyph : glyph - SYMBOLS + FIRST_GRAPHICS_CHAR;
        const char *rom = trs_char_data[trs_charset][code];
        for (int dy = 0; dy < cell_height; dy++) {
            for (int dx = 0; dx < cell_width; dx++) {
                dest[dy * cell_width + dx] = glyph_coverage(rom, dx, dy);
            }
        }
    }
    cached_charset = trs_charset;
    mask_valid = 0;
}

/*
 * Sets the size a character cell is drawn at, rebuilding the glyph cache and the
 * mask for it. Pass the host's cell size so nothing has to be scaled afterwards.
 *
 * Call before reading trs80_pixel_buffer(), and again whenever the cell size
 * changes. Cheap enough to call on rotation: it rasterizes 320 glyphs once.
 */
void trs80_set_cell_size(int width, int height)
{
    if (width <= 0 || height <= 0) {
        width = TRS80_CELL_WIDTH;
        height = TRS80_CELL_HEIGHT;
    }
    if (pixels != NULL && width == cell_width && height == cell_height) {
        return;
    }
    cell_width = width;
    cell_height = height;
    pixel_width = TRS80_SCREEN_COLS * cell_width;
    pixel_height = TRS80_SCREEN_ROWS * cell_height;

    free(pixels);
    free(glyph_cache);
    pixels = calloc((size_t) pixel_width * pixel_height, 1);
    glyph_cache = calloc((size_t) GLYPHS * cell_width * cell_height, 1);
    build_glyph_cache();
}

void trs80_invalidate_render(void)
{
    mask_valid = 0;
}

/*
 * Draws one character into the mask at the given cell position.
 *
 * Everything expensive already happened when the cache was built, so this is a
 * row-by-row copy. In expanded mode each column is doubled, which is an exact
 * 2x and so introduces none of the unevenness a fractional scale would.
 */
static void draw_cell(int glyph_index, int column, int row, int scale)
{
    const unsigned char *glyph =
            glyph_cache + (size_t) glyph_index * cell_width * cell_height;
    const int x = column * cell_width * scale;
    const int y = row * cell_height;

    for (int line = 0; line < cell_height; line++) {
        unsigned char *out = pixels + (size_t) (y + line) * pixel_width + x;
        const unsigned char *in = glyph + (size_t) line * cell_width;
        if (scale == 1) {
            memcpy(out, in, (size_t) cell_width);
            continue;
        }
        for (int dx = 0; dx < cell_width; dx++) {
            for (int repeat = 0; repeat < scale; repeat++) {
                *out++ = in[dx];
            }
        }
    }
}

/*
 * The glyph a code in video RAM shows, which is not always the one stored at
 * that code. This follows trs_screen_write_char in trs_sdl_interface.c, which
 * drew the screen before the core did.
 */
static int glyph_for_code(int code, int alternate)
{
    if (trs_model == 1) {
        /*
         * The Radio Shack lowercase modification: codes below 0x20 displayed
         * as uppercase letters.
         */
        if (code < 0x20) {
            return code + 0x40;
        }
        /*
         * The Model I's graphics ignore bit 6, so 192..255 repeat 128..191. Its
         * ROM stops at 127, so reading 192..255 from it draws nothing.
         */
        if (code > LAST_GRAPHICS_CHAR) {
            return code - 0x40;
        }
        return code;
    }
    if (code > LAST_GRAPHICS_CHAR && !alternate) {
        return SYMBOLS + code - (LAST_GRAPHICS_CHAR + 1);
    }
    return code;
}

int trs80_render(void)
{
    unsigned char snapshot[CELLS];

    if (pixels == NULL) {
        trs80_set_cell_size(cell_width, cell_height);
    }
    /* A new session can be another model, and so another character ROM. */
    if (trs_charset != cached_charset) {
        build_glyph_cache();
    }

    /*
     * The only place the CPU thread is raced, and deliberately so: a cell read
     * here is either the character that was there before the guest's write or
     * the one after it, never a mixture. Everything below works from this copy,
     * so a write landing mid-rasterize cannot produce a broken glyph.
     */
    memcpy(snapshot, trs_screen, sizeof(snapshot));

    const int expanded = trs80_is_expanded_mode() ? 1 : 0;
    const int alternate = is_alternate_mode() ? 1 : 0;
    const int scale = expanded ? 2 : 1;
    /* Expanded mode shows 32 double-width characters, taking every other cell. */
    const int columns = TRS80_SCREEN_COLS / scale;

    /* A mode change moves every character, so nothing can be reused. */
    if (expanded != rendered_expanded) {
        mask_valid = 0;
        rendered_expanded = expanded;
    }
    /* Switching sets redraws 192..255 without a byte of video RAM changing. */
    if (alternate != rendered_alternate) {
        mask_valid = 0;
        rendered_alternate = alternate;
    }

    int changed = 0;
    for (int row = 0; row < TRS80_SCREEN_ROWS; row++) {
        for (int column = 0; column < columns; column++) {
            const int index = row * TRS80_SCREEN_COLS + column * scale;
            const unsigned char code = snapshot[index];
            if (mask_valid && rendered_cells[index] == code) {
                continue;
            }
            draw_cell(glyph_for_code(code, alternate), column, row, scale);
            rendered_cells[index] = code;
            changed = 1;
        }
    }
    mask_valid = 1;
    return changed;
}
