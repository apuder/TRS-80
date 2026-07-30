/*
 * trs80_core.h - the complete host-facing API of the TRS-80 emulator core.
 *
 * Nothing below depends on Android, the JVM, or any UI toolkit. A host binds
 * to this header via JNI, Kotlin/Native cinterop, Dart FFI or plain C, supplies
 * the callbacks in trs80_host, and drives the emulator with trs80_run().
 *
 * The core is a singleton: all emulator state lives in file-scope globals, so
 * there is exactly one machine per process.
 */

#ifndef TRS80_CORE_H
#define TRS80_CORE_H

#ifdef __cplusplus
extern "C" {
#endif

/* Return values of trs80_init(). */
#define TRS80_OK             0
#define TRS80_ERR_ENDIAN    -1

/* Size of the shared character buffer; see trs80_screen_buffer(). */
#define TRS80_SCREEN_BUFFER_SIZE 2048

/* The emulated screen, in character cells. */
#define TRS80_SCREEN_COLS 64
#define TRS80_SCREEN_ROWS 16

/*
 * One character cell at the character ROM's own resolution. The glyphs occupy
 * the top eight rows; the remaining four are the leading between text lines.
 */
#define TRS80_CELL_WIDTH  8
#define TRS80_CELL_HEIGHT 12

/* The rasterized screen; see trs80_pixel_buffer(). */
#define TRS80_PIXEL_WIDTH  (TRS80_SCREEN_COLS * TRS80_CELL_WIDTH)
#define TRS80_PIXEL_HEIGHT (TRS80_SCREEN_ROWS * TRS80_CELL_HEIGHT)

/* Key event kinds accepted by trs80_add_key_event(), matching SDL 1.2. */
#define TRS80_KEY_DOWN 2
#define TRS80_KEY_UP   3

/*
 * The machine to emulate. Any path may be NULL to leave that drive empty.
 * If disk_path[0] names a ".cmd" file it is loaded directly into memory
 * instead of being mounted, and its entry point overrides entry_addr.
 */
typedef struct {
    int model;                  /* 1 = Model I, 3 = Model III, 4 = Model 4, 5 = Model 4P */
    const char *rom_path;
    const char *cassette_path;
    const char *disk_path[4];
    unsigned short entry_addr;
} trs80_config;

/*
 * Services the core calls back into. Members may be NULL.
 *
 * not_implemented is invoked when the emulator reaches a code path that this
 * build does not support (notably the hi-res graphics modes). The core
 * unwinds out of trs80_run() immediately afterwards, so the callback should
 * record or report the message rather than attempt to continue.
 */
typedef struct {
    void (*not_implemented)(const char *msg);
} trs80_host;

/* Install host callbacks. Call before trs80_init(). */
void trs80_set_host(const trs80_host *host);

/* Boot a machine. Returns TRS80_OK or a TRS80_ERR_* value. */
int trs80_init(const trs80_config *config);

/*
 * The shared screen buffer: one byte per character cell, written by the
 * emulator as the emulated machine updates its video RAM. The host reads it
 * directly - there is no copy and no change notification, so hosts poll it.
 * The pointer is valid for the lifetime of the process.
 */
unsigned char *trs80_screen_buffer(void);

/*
 * The rasterized screen: one byte of coverage per pixel, 0 for background and
 * 255 for foreground, row-major with TRS80_PIXEL_WIDTH bytes per row. The host
 * tints and scales it, so the emulated picture keeps its exact pixel geometry
 * however large it is drawn.
 *
 * Only ever written by trs80_render(). The pointer is valid for the lifetime of
 * the process.
 */
unsigned char *trs80_pixel_buffer(void);

/* The mask's current size, which follows the cell size set below. */
int trs80_pixel_width(void);
int trs80_pixel_height(void);

/*
 * Sets the size one character cell is drawn at, and rebuilds the mask for it.
 *
 * Pass the size the host actually draws a cell at, so that nothing needs scaling
 * afterwards. That is the point: the character ROM's glyphs are 8x12 one-bit
 * bitmaps, and scaling those up by a fraction leaves each one-pixel stem on
 * either one or two output pixels depending where it falls, so stems come out
 * uneven and thin ones vanish. Scaling once here, with area coverage, gives every
 * stem the same weight wherever it lands.
 *
 * Passing 0 for either falls back to the ROM's own 8x12. Call before reading
 * trs80_pixel_buffer(), and again whenever the cell size changes; it rasterizes
 * 256 glyphs, so it belongs on rotation rather than in a frame.
 */
void trs80_set_cell_size(int width, int height);

/*
 * Rasterizes whatever the emulated machine currently has in video RAM into
 * trs80_pixel_buffer(), redrawing only the cells that changed.
 *
 * Call this from the thread that reads the pixel buffer, and never from the
 * thread running trs80_run(). It snapshots video RAM before rasterizing, so the
 * only contention with the CPU thread is that copy - which can see a cell
 * mid-change, exactly as reading trs80_screen_buffer() always could, and never
 * a half-drawn character. Rasterizing on the CPU thread would both invert that
 * property and steal time from the guest, which shows up as input latency.
 *
 * @return 1 if any pixels changed, 0 if the screen is identical to last time,
 * in which case the host can skip its upload entirely.
 */
int trs80_render(void);

/*
 * Discards what trs80_render() believes is already on screen, so the next call
 * redraws everything. Needed after anything that invalidates the host's copy,
 * such as reattaching a surface.
 */
void trs80_invalidate_render(void);

/*
 * Run the CPU. Blocks until trs80_set_running(0) is called from another
 * thread, so the host must call this on a dedicated thread.
 */
void trs80_run(void);
void trs80_set_running(int running);

/* Reset the emulated machine. Takes effect on the next run-loop iteration. */
void trs80_reset(void);

/* True while the machine is in 32-column expanded mode. */
int trs80_is_expanded_mode(void);

/* Queue a key event; event is TRS80_KEY_DOWN or TRS80_KEY_UP. */
void trs80_add_key_event(int event, int sym, int key);

/* Type text into the emulated keyboard. */
void trs80_paste(const char *text, int length);

/* Machine state, in the legacy xtrs format. */
void trs80_save_state(const char *path);
void trs80_load_state(const char *path);

void trs80_rewind_cassette(void);
float trs80_cassette_position(void);   /* 0.0 - 1.0 */
void trs80_set_sound_muted(int muted);

/* Blank disk image creation. All return non-zero on success. */
int trs80_create_blank_jv1(const char *path);
int trs80_create_blank_jv3(const char *path);
int trs80_create_blank_dmk(const char *path, int sides, int density,
                           int eight, int ignden);

#ifdef __cplusplus
}
#endif

#endif /* TRS80_CORE_H */
