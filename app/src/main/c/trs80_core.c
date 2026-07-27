/*
 * trs80_core.c - implementation of the host-facing emulator API.
 *
 * This file owns everything that used to live in native.c apart from the JNI
 * glue itself. It must not include jni.h or any android/ header.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <setjmp.h>

#include "load_cmd.h"
#include "trs.h"
#include "trs_disk.h"
#include "trs_mkdisk.h"
#include "trs_cassette.h"
#include "trs_iodefs.h"
#include "trs_uart.h"
#include "trs_state_save.h"
#include "trs_xray_state_save.h"

#include <SDL/SDL.h>

#include "atrs.h"
#include "trs80_audio.h"
#include "trs80_core.h"

/* Polled by the CPU thread, written by the host thread. */
static volatile int isRunning = 0;

static jmp_buf ex_buf;
static int reset_required = 0;

static trs80_host host = { 0 };

/*
 * The screen buffer is owned by the core and shared with the host. Making the
 * core the owner is what lets a host map it with JNI's NewDirectByteBuffer,
 * Kotlin/Native's CPointer or Dart's asTypedList without any copying.
 */
static unsigned char screen_buffer[TRS80_SCREEN_BUFFER_SIZE];
unsigned char *trs_screen = screen_buffer;

/* Defined in trs_memory.c */
extern Uchar memory[];

extern char *program_name;
extern int trs_paste_started();
extern void add_key_event(Uint16 event, Uint16 sym, Uint16 key);

void trs_debug()
{
    // Do nothing
}

static int is_little_endian()
{
    wordregister x;
    x.byte.low = 1;
    x.byte.high = 0;
    return x.word == 1;
}

/* ------------------------------------------------------------------ paste */

static int charCount = 0;
static unsigned char *pasteString = NULL;
static int pasteStringLength = 0;

static void clear_paste_string()
{
    if (pasteString != NULL) {
        free(pasteString);
        pasteString = NULL;
    }
    charCount = pasteStringLength = 0;
}

int PasteManagerGetChar(unsigned short *character)
{
    if (charCount) {
        *character = pasteString[pasteStringLength - charCount];
        charCount--;
        if (charCount) {
            return (TRUE);
        } else {
            clear_paste_string();
            return (FALSE);
        }
    } else {
        return (FALSE);
    }
}

/* ------------------------------------------------------------------- init */

static int ends_with(const char *str, const char *suffix)
{
    if (!str || !suffix)
        return 0;
    size_t lenstr = strlen(str);
    size_t lensuffix = strlen(suffix);
    if (lensuffix > lenstr)
        return 0;
    return strncmp(str + lenstr - lensuffix, suffix, lensuffix) == 0;
}

static void init_emulator()
{
    trs_main_init();
    trs_cassette_init();
    trs_disk__init();
    trs_hard__init();
    trs_interrupt_init();
    trs_io_init();
    trs_mem_init();
    trs_keyboard_init();
#ifndef ANDROID
    trs_uart_init();
#endif
    trs_z80_init();
}

void trs80_set_host(const trs80_host *h)
{
    if (h != NULL) {
        host = *h;
    } else {
        memset(&host, 0, sizeof(host));
    }
}

unsigned char *trs80_screen_buffer(void)
{
    return screen_buffer;
}

int trs80_init(const trs80_config *config)
{
    int i;
    /* load_cmd() writes through an int*, so this must be an int and not the
     * unsigned short it used to be - the old code corrupted the stack slot
     * next to it whenever a .cmd file was loaded. */
    int entry_addr = config->entry_addr;

    if (!is_little_endian()) {
        return TRS80_ERR_ENDIAN;
    }

    program_name = "xtrs";
    trs_model = config->model;
    init_emulator();

    char *dest = NULL;
    switch (config->model) {
    case 1:
        dest = romfile;
        break;
    case 3:
        dest = romfile3;
        break;
    case 4:
    case 5:
        dest = romfile4p;
        break;
    }
    if (dest != NULL && config->rom_path != NULL) {
        strncpy(dest, config->rom_path, FILENAME_MAX);
    }

    trs_autodelay = 1;
    trs_emtsafe = 1;
    trs_show_led = 0;
    timer_overclock = 0;
    grafyx_set_microlabs(0);
    trs_disk_doubler = TRSDISK_BOTH;
    trs_disk_truedam = 0;
    trs_uart_name = "UART";
    trs_uart_switches = 0;
    trs_kb_bracket(0);
    mem_init();
    trs_rom_init();
    trs_screen_init();
    trs_timer_init();
    trs_reset(1);

    trs_cassette_remove();
    if (config->cassette_path != NULL) {
        trs_cassette_insert((char *) config->cassette_path);
    }

    for (i = 0; i < 4; i++) {
        const char *path = config->disk_path[i];
        trs_disk_remove(i);
        if (path == NULL) {
            continue;
        }
        /* A .cmd file in the first slot is loaded straight into memory and
         * supplies its own entry point. */
        if (i == 0 && ends_with(path, ".cmd")) {
            FILE *f = fopen(path, "rb");
            if (f != NULL) {
                load_cmd(f, memory, NULL, 0, NULL, -1, NULL, &entry_addr, 1);
                fclose(f);
            }
        } else {
            trs_disk_insert(i, (char *) path);
        }
    }

    trs_disk_init(1);
    z80_state.pc.word = (unsigned short) entry_addr;
    clear_paste_string();
    return TRS80_OK;
}

/* -------------------------------------------------------------- execution */

void trs80_run(void)
{
    clear_paste_string();
    if (!setjmp(ex_buf)) {
        reset_required = 0;
        while (isRunning) {
            z80_run(0);
            if (reset_required) {
                reset_required = 0;
                clear_paste_string();
                trs_timer_init();
                trs_reset(0);
            }
        }
    } else {
        // Unwound by not_implemented().
    }
    trs80_audio_shutdown();
}

void trs80_set_running(int running)
{
    isRunning = running;
}

void trs80_reset(void)
{
    reset_required = 1;
}

int trs80_is_expanded_mode(void)
{
    return is_expanded_mode();
}

/* ------------------------------------------------------------------ input */

void trs80_add_key_event(int event, int sym, int key)
{
    add_key_event((Uint16) event, (Uint16) sym, (Uint16) key);
}

void trs80_paste(const char *text, int length)
{
    clear_paste_string();
    if (text == NULL || length <= 0) {
        return;
    }
    charCount = pasteStringLength = length;
    pasteString = (unsigned char *) malloc(length);
    if (pasteString == NULL) {
        charCount = pasteStringLength = 0;
        return;
    }
    memcpy(pasteString, text, length);
    trs_paste_started();
}

/* ------------------------------------------------------------------ state */

void trs80_save_state(const char *path)
{
    char xray_state_filename[FILENAME_MAX];

    trs_cassette_reset();
    trs_state_save((char *) path);

    snprintf(xray_state_filename, sizeof(xray_state_filename), "%s-xray.pb", path);
    trs_xray_save_system_state(xray_state_filename);
}

void trs80_load_state(const char *path)
{
    trs_state_load((char *) path);
}

/* ------------------------------------------------------ cassette and sound */

void trs80_rewind_cassette(void)
{
    trs_set_cassette_position(0);
}

float trs80_cassette_position(void)
{
    return (float) trs_get_cassette_position() / (float) trs_get_cassette_length();
}

void trs80_set_sound_muted(int muted)
{
    if (muted) {
        sdl_audio_muted = 1;
        SDL_CloseAudio();
    }
    flush_audio_queue();
    sdl_audio_muted = muted;
}

/* ------------------------------------------------------------ disk images */

int trs80_create_blank_jv1(const char *path)
{
    return trs_create_blank_jv1((char *) path) == 0;
}

int trs80_create_blank_jv3(const char *path)
{
    return trs_create_blank_jv3((char *) path) == 0;
}

int trs80_create_blank_dmk(const char *path, int sides, int density,
                           int eight, int ignden)
{
    return trs_create_blank_dmk((char *) path, sides, density, eight, ignden) == 0;
}

/* ---------------------------------------------------------- host callback */

void not_implemented(const char *msg)
{
    if (host.not_implemented != NULL) {
        host.not_implemented(msg);
    }
    longjmp(ex_buf, 1);
}
