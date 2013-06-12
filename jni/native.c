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
#define ERR_GET_METHOD_XLOG -5
#define ERR_MEMORY_IS_NO_COPY -6
#define ERR_SCREEN_IS_NO_COPY -7

Uchar* memory;
int isRunning = 0;

#define EMULATOR_STATUS_NOT_INITIALIZED 0
#define EMULATOR_STATUS_INITIALIZED     1
static int emulator_status = EMULATOR_STATUS_NOT_INITIALIZED;

static JavaVM *jvm;
static jclass clazzXTRS;
static jmethodID isRenderingMethodId;
static jmethodID updateScreenMethodId;
static jmethodID getDiskPathMethodId;
static jmethodID xlogMethodId;
static jbyte* screenBuffer;
static jbyteArray memoryArray;
static jbyteArray screenArray;

unsigned char trs_screen[2048];
int instructionsSinceLastScreenAccess;
int screenWasUpdated;

extern char *program_name;

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
    grafyx_set_microlabs(0);
    trs_disk_doubler = TRSDISK_BOTH;
    trs_disk_truedam = 0;
    cassette_default_sample_rate = 0;
    trs_uart_name = "UART";
    trs_uart_switches = 0;
    trs_kb_bracket(0);
    mem_init();
    trs_screen_init();
    trs_timer_init();
    trs_reset(1);
    z80_state.pc.word = entryAddr;
    instructionsSinceLastScreenAccess = 0;
    screenWasUpdated = 0;

    emulator_status = EMULATOR_STATUS_INITIALIZED;
}

static void check_for_screen_updates() {
    instructionsSinceLastScreenAccess++;
    if (instructionsSinceLastScreenAccess >= SCREEN_UPDATE_THRESHOLD) {
        if (screenWasUpdated) {
            JNIEnv *env = getEnv();
            jboolean isRendering = (*env)->CallStaticBooleanMethod(env, clazzXTRS,
                    isRenderingMethodId);
            if (!isRendering) {
                memcpy(screenBuffer, trs_screen, 0x3fff - 0x3c00 + 1);
                (*env)->CallStaticVoidMethod(env, clazzXTRS, updateScreenMethodId);
                screenWasUpdated = 0;
            }
        }
        instructionsSinceLastScreenAccess = 0;
    }
}

char* get_disk_path(int disk) {
    JNIEnv *env = getEnv();
    jstring jpath = (*env)->CallStaticObjectMethod(env, clazzXTRS,
            getDiskPathMethodId, disk);
    if (jpath == NULL) {
        return strdup("");
    }
    const char* path = (*env)->GetStringUTFChars(env, jpath, NULL);
    char* str = strdup(path);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return str;
}

int Java_org_puder_trs80_XTRS_init(JNIEnv* env, jclass cls, jint model, jint sizeROM,
        jint entryAddr, jbyteArray mem, jbyteArray screen) {
    clazzXTRS = cls;
    int status = (*env)->GetJavaVM(env, &jvm);
    if(status != 0) {
        return ERR_GET_JVM;
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

void Java_org_puder_trs80_XTRS_run(JNIEnv* env, jclass clazz) {
    if (emulator_status == EMULATOR_STATUS_NOT_INITIALIZED) {
        return;
    }
    while (isRunning) {
        z80_run(0);
        check_for_screen_updates();
    }
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
