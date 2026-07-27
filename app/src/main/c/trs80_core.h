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
