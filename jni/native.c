#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "trs.h"
#include "trs_disk.h"
#include "trs_iodefs.h"
#include "trs_uart.h"

#include "atrs.h"

#define DEBUG_TAG "Z80"

#define NO_ERROR 0
#define ERR_GET_JVM -1
#define ERR_GET_METHOD_IS_RENDERING -2
#define ERR_GET_METHOD_UPDATE_SCREEN -3
#define ERR_GET_METHOD_GET_DISK_PATH -4
#define ERR_GET_METHOD_INIT_AUDIO -5
#define ERR_GET_METHOD_CLOSE_AUDIO -6
#define ERR_GET_METHOD_PAUSE_AUDIO -7
#define ERR_GET_METHOD_XLOG -8
#define ERR_MEMORY_IS_NO_COPY -9
#define ERR_SCREEN_IS_NO_COPY -10

Uchar* memory;
int isRunning = 0;

#define EMULATOR_STATUS_NOT_INITIALIZED 0
#define EMULATOR_STATUS_INITIALIZED     1
static int emulator_status = EMULATOR_STATUS_NOT_INITIALIZED;

static JavaVM *jvm;
static jclass clazzXTRS = NULL;
static jmethodID isRenderingMethodId;
static jmethodID updateScreenMethodId;
static jmethodID getDiskPathMethodId;
static jmethodID initAudioMethodId;
static jmethodID closeAudioMethodId;
static jmethodID pauseAudioMethodId;
static jmethodID xlogMethodId;
static jbyte* screenBuffer;
static jbyteArray memoryArray;
static jbyteArray screenArray;
static jbyteArray audioBufferArray;
static jbyte* audioBuffer;
static jint audioBufferSize;

unsigned char trs_screen[2048];
#ifdef ANDROID_BATCHED_SCREEN_UPDATE
int instructionsSinceLastScreenAccess;
int screenWasUpdated;
#endif
static int instructionsSinceLastScreenUpdate;


extern char *program_name;

void trs_debug()
{
    // Do nothing
}

static void check_endian() {
    wordregister x;
    x.byte.low = 1;
    x.byte.high = 0;
    if (x.word != 1) {
        fatal(
                "Program compiled with wrong ENDIAN value -- adjust the Makefile.local, type \"rm *.o\", recompile, and try again.");
    }
}

static JNIEnv* getEnv() {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, (JNIEnv **) &env, NULL);
    return env;
}

static void cleanup_xtrs() {
    if (emulator_status == EMULATOR_STATUS_NOT_INITIALIZED) {
        return;
    }
    JNIEnv *env = getEnv();
    (*env)->ReleaseByteArrayElements(env, memoryArray, (jbyte*) memory,
            JNI_COMMIT);
    (*env)->DeleteGlobalRef(env, memoryArray);
    (*env)->ReleaseByteArrayElements(env, screenArray, screenBuffer,
            JNI_COMMIT);
    (*env)->DeleteGlobalRef(env, screenArray);
    emulator_status = EMULATOR_STATUS_NOT_INITIALIZED;
}

static void init_xtrs(jint model, Ushort sizeROM, Ushort entryAddr) {
    int debug = FALSE;

    /* program_name must be set first because the error
     * printing routines use it. */
    program_name = "xtrs";
    check_endian();
    trs_model = model;
    trs_rom_size = sizeROM;
    trs_autodelay = 1;
    trs_show_led = 0;
    grafyx_set_microlabs(0);
    trs_disk_doubler = TRSDISK_BOTH;
    trs_disk_truedam = 0;
//    cassette_default_sample_rate = 0;
    trs_uart_name = "UART";
    trs_uart_switches = 0;
    trs_kb_bracket(0);
    mem_init();
    trs_screen_init();
    trs_timer_init();
    trs_reset(1);
    int i;
    for (i = 0; i < 4; i++) {
        char* path = get_disk_path(i);
        if (path != NULL) {
            trs_disk_insert(i, path);
            free(path);
        }
    }
    trs_disk_init(1);
    z80_state.pc.word = entryAddr;
#ifdef ANDROID_BATCHED_SCREEN_UPDATE
    instructionsSinceLastScreenAccess = 0;
    screenWasUpdated = 0;
#else
    instructionsSinceLastScreenUpdate = 0;
#endif

    emulator_status = EMULATOR_STATUS_INITIALIZED;
}

static int trigger_screen_update() {
    JNIEnv *env = getEnv();
    jboolean isRendering = (*env)->CallStaticBooleanMethod(env, clazzXTRS,
            isRenderingMethodId);
    if (isRendering) {
        return 0;
    }
    memcpy(screenBuffer, trs_screen, 0x3fff - 0x3c00 + 1);
    (*env)->CallStaticVoidMethod(env, clazzXTRS, updateScreenMethodId);
    return 1;
}

/*
 * This function gets called after each Z80 instruction. The original idea
 * for screen updates was to wait until a certain number of Z80 instructions
 * had passed during which no screen updates occurred (SCREEN_UPDATE_THRESHOLD).
 * This worked reasonably well when only few screen updates happened, but it
 * didn't work so well for arcade games that frequently updated the screen.
 * For that reason the screen update is now triggered after a certain number
 * of Z80 instructions, irrespective whether the screen was updated or not.
 */
static void check_for_screen_updates() {
#ifdef ANDROID_BATCHED_SCREEN_UPDATE
    instructionsSinceLastScreenAccess++;
    if ((instructionsSinceLastScreenAccess % SCREEN_FORCED_UPDATE_INTERVAL) == 0) {
        if (trigger_screen_update()) {
            screenWasUpdated = 0;
        }
    }
    if (instructionsSinceLastScreenAccess >= SCREEN_UPDATE_THRESHOLD) {
        if (screenWasUpdated) {
            if (trigger_screen_update()) {
                screenWasUpdated = 0;
            }
        }
    }
#else
    if (instructionsSinceLastScreenUpdate++ > SCREEN_UPDATE_THRESHOLD) {
        instructionsSinceLastScreenUpdate = 0;
        trigger_screen_update();
    }
#endif
}

char* get_disk_path(int disk) {
    JNIEnv *env = getEnv();
    jstring jpath = (*env)->CallStaticObjectMethod(env, clazzXTRS,
            getDiskPathMethodId, disk);
    if (jpath == NULL) {
        return NULL;
    }
    const char* path = (*env)->GetStringUTFChars(env, jpath, NULL);
    char* str = strdup(path);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return str;
}

void init_audio(int rate, int channels, int encoding, int bufSize) {
    JNIEnv *env = getEnv();
    (*env)->CallStaticVoidMethod(env, clazzXTRS, initAudioMethodId, rate, channels, encoding, bufSize);
}

void close_audio() {
    JNIEnv *env = getEnv();
    (*env)->CallStaticVoidMethod(env, clazzXTRS, closeAudioMethodId);
}

void pause_audio(int pause_on) {
    JNIEnv *env = getEnv();
    (*env)->CallStaticVoidMethod(env, clazzXTRS, pauseAudioMethodId, pause_on);
}

int Java_org_puder_trs80_XTRS_init(JNIEnv* env, jclass cls, jint model, jint sizeROM,
        jint entryAddr, jbyteArray mem, jbyteArray screen) {
    int status = (*env)->GetJavaVM(env, &jvm);
    if(status != 0) {
        return ERR_GET_JVM;
    }

    if (clazzXTRS == NULL) {
        clazzXTRS = (*env)->NewGlobalRef(env, cls);
    }

    cleanup_xtrs();

    isRenderingMethodId = (*env)->GetStaticMethodID(env, cls, "isRendering",
            "()Z");
    if (isRenderingMethodId == 0) {
        return ERR_GET_METHOD_IS_RENDERING;
    }

    updateScreenMethodId = (*env)->GetStaticMethodID(env, cls, "updateScreen",
            "()V");
    if (updateScreenMethodId == 0) {
        return ERR_GET_METHOD_UPDATE_SCREEN;
    }

    getDiskPathMethodId = (*env)->GetStaticMethodID(env, cls, "getDiskPath",
            "(I)Ljava/lang/String;");
    if (getDiskPathMethodId == 0) {
        return ERR_GET_METHOD_GET_DISK_PATH;
    }

    initAudioMethodId = (*env)->GetStaticMethodID(env, cls, "initAudio", "(IIII)V");
    if (initAudioMethodId == 0) {
        return ERR_GET_METHOD_INIT_AUDIO;
    }

    closeAudioMethodId = (*env)->GetStaticMethodID(env, cls, "closeAudio", "()V");
    if (closeAudioMethodId == 0) {
        return ERR_GET_METHOD_CLOSE_AUDIO;
    }

    pauseAudioMethodId = (*env)->GetStaticMethodID(env, cls, "pauseAudio", "(I)V");
    if (pauseAudioMethodId == 0) {
        return ERR_GET_METHOD_PAUSE_AUDIO;
    }

    xlogMethodId = (*env)->GetStaticMethodID(env, cls, "xlog",
            "(Ljava/lang/String;)V");
    if (xlogMethodId == 0) {
        return ERR_GET_METHOD_XLOG;
    }

    jboolean isCopy;
    memoryArray = (*env)->NewGlobalRef(env, mem);
    memory = (Uchar*) (*env)->GetByteArrayElements(env, mem, &isCopy);
    if (isCopy) {
        return ERR_MEMORY_IS_NO_COPY;
    }

    screenArray = (*env)->NewGlobalRef(env, screen);
    screenBuffer = (*env)->GetByteArrayElements(env, screen, &isCopy);
    if (isCopy) {
        return ERR_SCREEN_IS_NO_COPY;
    }

    init_xtrs(model, sizeROM, entryAddr);
    return NO_ERROR;
}

void Java_org_puder_trs80_XTRS_setAudioBuffer(JNIEnv* env, jclass cls, jbyteArray buffer) {
    jboolean isCopy;
    audioBufferArray = (*env)->NewGlobalRef(env, buffer);
    audioBuffer = (Uchar*) (*env)->GetByteArrayElements(env, buffer, &isCopy);
    audioBufferSize = (*env)->GetArrayLength(env, buffer);
    if (isCopy) {
        // TODO error
    }
}

void Java_org_puder_trs80_XTRS_flushAudioQueue(JNIEnv* env, jclass cls) {
    flush_audio_queue();
}

typedef unsigned char Uint8;
typedef unsigned short Uint16;
typedef unsigned int Uint32;
extern void fillBuffer(Uint8* stream, int len);
void Java_org_puder_trs80_XTRS_fillAudioBuffer(JNIEnv* env, jclass cls) {
    fillBuffer(audioBuffer, audioBufferSize);
}

extern void add_key_event(Uint16 event, Uint16 sym, Uint16 key);
void Java_org_puder_trs80_XTRS_addKeyEvent(JNIEnv* env, jclass cls, jint event, jint sym, jint key) {
    add_key_event(event, sym, key);
}

void Java_org_puder_trs80_XTRS_run(JNIEnv* env, jclass clazz) {
    if (emulator_status == EMULATOR_STATUS_NOT_INITIALIZED) {
        return;
    }
    while (isRunning) {
        z80_run(0);
        check_for_screen_updates();
#ifdef SETITIMER_FIX
        struct timeval tv;

        gettimeofday(&tv, NULL);
        if ((tv.tv_sec*1000000 + tv.tv_usec) >= next_timer) {
            trs_timer_event(0);
        }
#endif
    }
}

void Java_org_puder_trs80_XTRS_reset(JNIEnv* env, jclass cls) {
    z80_reset();
}

void Java_org_puder_trs80_XTRS_cleanup(JNIEnv* env, jclass clazz) {
    cleanup_xtrs();
}

void Java_org_puder_trs80_XTRS_setRunning(JNIEnv* e, jclass clazz, jboolean run) {
    isRunning = run;
}

void xlog(const char* msg) {
    JNIEnv *env = getEnv();
    jstring jmsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallStaticVoidMethod(env, clazzXTRS, xlogMethodId, jmsg);
    (*env)->DeleteLocalRef(env, jmsg);
}
