#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <setjmp.h>
#include "trs.h"
#include "trs_disk.h"
#include "trs_cassette.h"
#include "trs_iodefs.h"
#include "trs_uart.h"
#include "trs_state_save.h"


#include <SDL/SDL.h>

#include "atrs.h"
#include "opensl.h"

#define DEBUG_TAG "Z80"

#define NO_ERROR 0
#define ERR_GET_JVM -1
#define ERR_GET_METHOD_IS_RENDERING -2
#define ERR_GET_METHOD_UPDATE_SCREEN -3
#define ERR_GET_METHOD_SET_EXPANDED_SCREEN_MODE -4
#define ERR_GET_METHOD_XLOG -5
#define ERR_GET_METHOD_NOT_IMPLEMENTED -10

int isRunning = 0;

static JavaVM *jvm;
static jclass clazzXTRS = NULL;
static jmethodID rendererIsReadyMethodId;
static jmethodID updateScreenMethodId;
static jmethodID setExpandedScreenModeMethodId;
static jmethodID xlogMethodId;
static jmethodID notImplementedMethodId;

static jbyteArray screenArray = NULL;
static jbyte* screenBuffer;
static jboolean screenBufferIsCopy;

static jmp_buf ex_buf;

unsigned char trs_screen[2048];
static int screenUpdateRequired = 0;


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

static void init_xtrs(JNIEnv* env, jint model, jstring romFile, Ushort entryAddr, jstring xtrsCassette,
                      jstring xtrsDisk0, jstring xtrsDisk1, jstring xtrsDisk2, jstring xtrsDisk3) {
    int debug = FALSE;

    /* program_name must be set first because the error
     * printing routines use it. */
    program_name = "xtrs";
    check_endian();
    trs_model = model;
    const char* path = (*env)->GetStringUTFChars(env, romFile, NULL);
    char* dest = NULL;
    switch(model) {
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
    strncpy(dest, path, FILENAME_MAX);
    (*env)->ReleaseStringUTFChars(env, romFile, path);
    trs_autodelay = 1;
    trs_emtsafe = 1;
    trs_show_led = 0;
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
    // Cassette
    trs_cassette_remove();
    if (xtrsCassette != NULL) {
        path = (*env)->GetStringUTFChars(env, xtrsCassette, NULL);
        trs_cassette_insert((char*) path);
        (*env)->ReleaseStringUTFChars(env, xtrsCassette, path);
    }
    // Disk 0
    trs_disk_remove(0);
    if (xtrsDisk0 != NULL) {
        path = (*env)->GetStringUTFChars(env, xtrsDisk0, NULL);
        trs_disk_insert(0, (char*) path);
        (*env)->ReleaseStringUTFChars(env, xtrsDisk0, path);
    }
    // Disk 1
    trs_disk_remove(1);
    if (xtrsDisk1 != NULL) {
        path = (*env)->GetStringUTFChars(env, xtrsDisk1, NULL);
        trs_disk_insert(1, (char*) path);
        (*env)->ReleaseStringUTFChars(env, xtrsDisk1, path);
    }
    // Disk 2
    trs_disk_remove(2);
    if (xtrsDisk2 != NULL) {
        path = (*env)->GetStringUTFChars(env, xtrsDisk2, NULL);
        trs_disk_insert(2, (char*) path);
        (*env)->ReleaseStringUTFChars(env, xtrsDisk2, path);
    }
    // Disk 3
    trs_disk_remove(3);
    if (xtrsDisk3 != NULL) {
        path = (*env)->GetStringUTFChars(env, xtrsDisk3, NULL);
        trs_disk_insert(3, (char*) path);
        (*env)->ReleaseStringUTFChars(env, xtrsDisk3, path);
    }

    trs_disk_init(1);
    z80_state.pc.word = entryAddr;
}

void trigger_screen_update() {
    screenUpdateRequired = 1;
    JNIEnv *env = getEnv();
    jboolean isReady = (*env)->CallStaticBooleanMethod(env, clazzXTRS,
            rendererIsReadyMethodId);
    if (!isReady) {
        return;
    }
    memcpy(screenBuffer, trs_screen, 0x3fff - 0x3c00 + 1);
    if (screenBufferIsCopy) {
        (*env)->ReleaseByteArrayElements(env, screenArray, screenBuffer, JNI_COMMIT);
    }
    (*env)->CallStaticVoidMethod(env, clazzXTRS, updateScreenMethodId);
    screenUpdateRequired = 0;
}

int Java_org_puder_trs80_XTRS_init(JNIEnv* env, jclass cls, jobject hardware) {
    int status = (*env)->GetJavaVM(env, &jvm);
    if(status != 0) {
        return ERR_GET_JVM;
    }

    if (clazzXTRS == NULL) {
        clazzXTRS = (*env)->NewGlobalRef(env, cls);
    }

    rendererIsReadyMethodId = (*env)->GetStaticMethodID(env, cls, "rendererIsReady",
            "()Z");
    if (rendererIsReadyMethodId == 0) {
        return ERR_GET_METHOD_IS_RENDERING;
    }

    updateScreenMethodId = (*env)->GetStaticMethodID(env, cls, "updateScreen",
            "()V");
    if (updateScreenMethodId == 0) {
        return ERR_GET_METHOD_UPDATE_SCREEN;
    }

    setExpandedScreenModeMethodId = (*env)->GetStaticMethodID(env, cls, "setExpandedScreenMode", "(Z)V");
    if (setExpandedScreenModeMethodId == 0) {
        return ERR_GET_METHOD_SET_EXPANDED_SCREEN_MODE;
    }

    xlogMethodId = (*env)->GetStaticMethodID(env, cls, "xlog",
            "(Ljava/lang/String;)V");
    if (xlogMethodId == 0) {
        return ERR_GET_METHOD_XLOG;
    }

    notImplementedMethodId = (*env)->GetStaticMethodID(env, cls, "notImplemented",
            "(Ljava/lang/String;)V");
    if (notImplementedMethodId == 0) {
        return ERR_GET_METHOD_NOT_IMPLEMENTED;
    }

    jclass hardwareClass = (*env)->GetObjectClass(env, hardware);
    jfieldID xtrsModelID = (*env)->GetFieldID(env, hardwareClass, "xtrsModel", "I");
    jfieldID xtrsRomFileID = (*env)->GetFieldID(env, hardwareClass, "xtrsRomFile", "Ljava/lang/String;");
    jfieldID xtrsScreenBufferID = (*env)->GetFieldID(env, hardwareClass, "xtrsScreenBuffer", "[B");
    jfieldID xtrsEntryAddrID = (*env)->GetFieldID(env, hardwareClass, "xtrsEntryAddr", "I");
    jfieldID xtrsCassetteID = (*env)->GetFieldID(env, hardwareClass, "xtrsCassette", "Ljava/lang/String;");
    jfieldID xtrsDisk0ID = (*env)->GetFieldID(env, hardwareClass, "xtrsDisk0", "Ljava/lang/String;");
    jfieldID xtrsDisk1ID = (*env)->GetFieldID(env, hardwareClass, "xtrsDisk1", "Ljava/lang/String;");
    jfieldID xtrsDisk2ID = (*env)->GetFieldID(env, hardwareClass, "xtrsDisk2", "Ljava/lang/String;");
    jfieldID xtrsDisk3ID = (*env)->GetFieldID(env, hardwareClass, "xtrsDisk3", "Ljava/lang/String;");
    jint xtrsModel = (*env)->GetIntField(env, hardware, xtrsModelID);
    jstring xtrsRomFile = (*env)->GetObjectField(env, hardware, xtrsRomFileID);
    jbyteArray xtrsScreenBuffer = (*env)->GetObjectField(env, hardware, xtrsScreenBufferID);
    jint xtrsEntryAddr = (*env)->GetIntField(env, hardware, xtrsEntryAddrID);
    jstring xtrsCassette = (*env)->GetObjectField(env, hardware, xtrsCassetteID);
    jstring xtrsDisk0 = (*env)->GetObjectField(env, hardware, xtrsDisk0ID);
    jstring xtrsDisk1 = (*env)->GetObjectField(env, hardware, xtrsDisk1ID);
    jstring xtrsDisk2 = (*env)->GetObjectField(env, hardware, xtrsDisk2ID);
    jstring xtrsDisk3 = (*env)->GetObjectField(env, hardware, xtrsDisk3ID);

    if (screenArray != NULL) {
        (*env)->ReleaseByteArrayElements(env, screenArray, screenBuffer, JNI_ABORT);
        (*env)->DeleteGlobalRef(env, screenArray);
    }
    screenArray = (*env)->NewGlobalRef(env, xtrsScreenBuffer);
    screenBuffer = (*env)->GetByteArrayElements(env, screenArray, &screenBufferIsCopy);

    init_xtrs(env, xtrsModel, xtrsRomFile, xtrsEntryAddr, xtrsCassette, xtrsDisk0, xtrsDisk1, xtrsDisk2, xtrsDisk3);
    return NO_ERROR;
}

void Java_org_puder_trs80_XTRS_saveState(JNIEnv* env, jclass cls, jstring fileName) {
    const char* fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    trs_cassette_reset();
    trs_state_save(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
}

void Java_org_puder_trs80_XTRS_loadState(JNIEnv* env, jclass cls, jstring fileName) {
    const char* fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    trs_state_load(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
}

extern void add_key_event(Uint16 event, Uint16 sym, Uint16 key);
void Java_org_puder_trs80_XTRS_addKeyEvent(JNIEnv* env, jclass cls, jint event, jint sym, jint key) {
    add_key_event(event, sym, key);
}

void Java_org_puder_trs80_XTRS_run(JNIEnv* env, jclass clazz) {
    if (!setjmp(ex_buf)) {
        screenUpdateRequired = 1;
        while (isRunning) {
            z80_run(0);
            if (screenUpdateRequired) {
                trigger_screen_update();
            }
        }
    } else {
        // Got not implemented exception
    }
    OpenSLWrap_Shutdown();
}

void Java_org_puder_trs80_XTRS_reset(JNIEnv* env, jclass cls) {
    trs_timer_init();
    trs_reset(0);
}

void Java_org_puder_trs80_XTRS_rewindCassette(JNIEnv* env, jclass cls) {
    trs_set_cassette_position(0);
}

void Java_org_puder_trs80_XTRS_setSoundMuted(JNIEnv* e, jclass clazz, jboolean muted) {
    if (muted) {
        sdl_audio_muted = 1;
        SDL_CloseAudio();
    }
    flush_audio_queue();
    sdl_audio_muted = muted;
}

void Java_org_puder_trs80_XTRS_setRunning(JNIEnv* e, jclass clazz, jboolean run) {
    isRunning = run;
}

jfloat Java_org_puder_trs80_XTRS_getCassettePosition(JNIEnv* e, jclass clazz) {
    return (jfloat) trs_get_cassette_position() / (jfloat) trs_get_cassette_length();
}

void set_expanded_screen_mode(int flag) {
    JNIEnv *env = getEnv();
    (*env)->CallStaticVoidMethod(env, clazzXTRS, setExpandedScreenModeMethodId, (jboolean) flag);
}

void xlog(const char* msg) {
    JNIEnv *env = getEnv();
    jstring jmsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallStaticVoidMethod(env, clazzXTRS, xlogMethodId, jmsg);
    (*env)->DeleteLocalRef(env, jmsg);
}

void not_implemented(const char* msg) {
    JNIEnv *env = getEnv();
    jstring jmsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallStaticVoidMethod(env, clazzXTRS, notImplementedMethodId, jmsg);
    (*env)->DeleteLocalRef(env, jmsg);
    longjmp(ex_buf, 1);
}
