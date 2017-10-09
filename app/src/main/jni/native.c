#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <setjmp.h>
#include "trs.h"
#include "trs_disk.h"
#include "trs_mkdisk.h"
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
#define ERR_GET_METHOD_XLOG -5
#define ERR_GET_METHOD_NOT_IMPLEMENTED -10

int isRunning = 0;

static JavaVM *jvm;
static jclass clazzXTRS = NULL;
static jmethodID xlogMethodId;
static jmethodID notImplementedMethodId;

static jmp_buf ex_buf;

static int reset_required = 0;

// Defined in trs_memory.c
extern Uchar memory[];

// Note: Further down, upon initialization, we set this pointer to a direct
//       buffer address that we share with the Java-side. This way, all writes
//       by the emulator inot this address space will be immediately visible
//       on the java side by the render thread.
unsigned char* trs_screen;


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

extern int trs_paste_started();
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
            return(FALSE);
        }
    }
    else {
        return(FALSE);
    }
}

static int ends_with(const char *str, const char *suffix)
{
    if (!str || !suffix)
        return 0;
    size_t lenstr = strlen(str);
    size_t lensuffix = strlen(suffix);
    if (lensuffix >  lenstr)
        return 0;
    return strncmp(str + lenstr - lensuffix, suffix, lensuffix) == 0;
}

static void init_emulator() {
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

static void init_xtrs(JNIEnv* env, jint model, jstring romFile, Ushort entryAddr, jstring xtrsCassette,
                      jstring xtrsDisk0, jstring xtrsDisk1, jstring xtrsDisk2, jstring xtrsDisk3) {
    int debug = FALSE;

    program_name = "xtrs";
    check_endian();
    trs_model = model;
    init_emulator();

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
        if (ends_with(path, ".cmd")) {
            FILE* f = fopen(path, "rb");
            load_cmd(f, memory, NULL, 0, NULL, -1, NULL, &entryAddr, 1);
            fclose(f);
        } else {
            trs_disk_insert(0, (char *) path);
        }
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
    clear_paste_string();
}

JNIEXPORT jint JNICALL
Java_org_puder_trs80_XTRS_initNative(JNIEnv *env, jclass cls) {
    int status = (*env)->GetJavaVM(env, &jvm);
    if(status != 0) {
        return ERR_GET_JVM;
    }

    if (clazzXTRS == NULL) {
        clazzXTRS = (*env)->NewGlobalRef(env, cls);
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

    jfieldID xtrsModelID = (*env)->GetStaticFieldID(env, cls, "xtrsModel", "I");
    jfieldID xtrsRomFileID = (*env)->GetStaticFieldID(env, cls, "xtrsRomFile", "Ljava/lang/String;");
    jfieldID xtrsEntryAddrID = (*env)->GetStaticFieldID(env, cls, "xtrsEntryAddr", "I");
    jfieldID xtrsCassetteID = (*env)->GetStaticFieldID(env, cls, "xtrsCassette", "Ljava/lang/String;");
    jfieldID xtrsDisk0ID = (*env)->GetStaticFieldID(env, cls, "xtrsDisk0", "Ljava/lang/String;");
    jfieldID xtrsDisk1ID = (*env)->GetStaticFieldID(env, cls, "xtrsDisk1", "Ljava/lang/String;");
    jfieldID xtrsDisk2ID = (*env)->GetStaticFieldID(env, cls, "xtrsDisk2", "Ljava/lang/String;");
    jfieldID xtrsDisk3ID = (*env)->GetStaticFieldID(env, cls, "xtrsDisk3", "Ljava/lang/String;");
    jint xtrsModel = (*env)->GetStaticIntField(env, cls, xtrsModelID);
    jstring xtrsRomFile = (*env)->GetStaticObjectField(env, cls, xtrsRomFileID);

    // Get the direct buffer for writing screen updates into.
    jfieldID xtrsScreenBufferID = (*env)->GetStaticFieldID(env, cls, "xtrsScreenBuffer", "Ljava/nio/ByteBuffer;");
    jobject screenBufferObject = (*env)->GetStaticObjectField(env, cls, xtrsScreenBufferID);
    trs_screen = (*env)->GetDirectBufferAddress(env, screenBufferObject);

    jint xtrsEntryAddr = (*env)->GetStaticIntField(env, cls, xtrsEntryAddrID);
    jstring xtrsCassette = (*env)->GetStaticObjectField(env, cls, xtrsCassetteID);
    jstring xtrsDisk0 = (*env)->GetStaticObjectField(env, cls, xtrsDisk0ID);
    jstring xtrsDisk1 = (*env)->GetStaticObjectField(env, cls, xtrsDisk1ID);
    jstring xtrsDisk2 = (*env)->GetStaticObjectField(env, cls, xtrsDisk2ID);
    jstring xtrsDisk3 = (*env)->GetStaticObjectField(env, cls, xtrsDisk3ID);

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

void Java_org_puder_trs80_XTRS_paste(JNIEnv* env, jclass cls, jstring clipboard) {
    clear_paste_string();
    const char* cb = (*env)->GetStringUTFChars(env, clipboard, NULL);
    charCount = pasteStringLength = (*env)->GetStringUTFLength(env, clipboard);
    pasteString = (unsigned char*) malloc(pasteStringLength);
    memcpy(pasteString, cb, pasteStringLength);
    (*env)->ReleaseStringUTFChars(env, clipboard, cb);
    trs_paste_started();
}

void Java_org_puder_trs80_XTRS_run(JNIEnv* env, jclass clazz) {
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
        // Got not implemented exception
    }
    OpenSLWrap_Shutdown();
}

void Java_org_puder_trs80_XTRS_reset(JNIEnv* env, jclass cls) {
    reset_required = 1;
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

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_isExpandedMode(JNIEnv *env, jclass type) {
    return is_expanded_mode() ? JNI_TRUE : JNI_FALSE;
}

jboolean Java_org_puder_trs80_XTRS_createBlankJV1(JNIEnv* env, jclass cls, jstring fileName) {
    const char* fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    int rc = trs_create_blank_jv1(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return (rc == 0) ? JNI_TRUE : JNI_FALSE;
}

jboolean Java_org_puder_trs80_XTRS_createBlankJV3(JNIEnv* env, jclass cls, jstring fileName) {
    const char* fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    int rc = trs_create_blank_jv3(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return (rc == 0) ? JNI_TRUE : JNI_FALSE;
}

jboolean Java_org_puder_trs80_XTRS_createBlankDMK(JNIEnv* env, jclass cls, jstring fileName,
    jint sides, jint density, jint eight, jint ignden) {
    const char* fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    int rc = trs_create_blank_dmk(fn, sides, density, eight, ignden);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return (rc == 0) ? JNI_TRUE : JNI_FALSE;
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
