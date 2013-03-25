#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "trs.h"
#include "trs_disk.h"
#include "trs_iodefs.h"
#include "trs_uart.h"

#include "atrs.h"

#define DEBUG_TAG "Z80"


Uchar* memory;
int isRunning = 0;

static jclass clazz;
static JNIEnv* env;
static jmethodID isRenderingMethodId;
static jmethodID updateScreenMethodId;
static jbyte* screenBuffer;
static jbyteArray memoryArray;
static jbyteArray screenArray;


unsigned char trs_screen[2048];
int instructionsSinceLastScreenAccess;
int screenWasUpdated;

extern char *program_name;

static void check_endian()
{
    wordregister x;
    x.byte.low = 1;
    x.byte.high = 0;
    if(x.word != 1) {
    	fatal("Program compiled with wrong ENDIAN value -- adjust the Makefile.local, type \"rm *.o\", recompile, and try again.");
    }
}

static void init_xtrs(Ushort entryAddr)
{
    int debug = FALSE;

    /* program_name must be set first because the error
     * printing routines use it. */
    program_name = "xtrs";
    check_endian();
    trs_autodelay = 1;
    trs_model = 3;
    trs_disk_dir = "/sdcard";
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
}


static void check_for_screen_updates()
{
    instructionsSinceLastScreenAccess++;
    if (instructionsSinceLastScreenAccess >= SCREEN_UPDATE_THRESHOLD) {
    	if (screenWasUpdated) {
    		jboolean isRendering = (*env)->CallStaticBooleanMethod(env, clazz, isRenderingMethodId);
    		if (!isRendering) {
    			memcpy(screenBuffer, trs_screen, 0x3fff - 0x3c00 + 1);
    			(*env)->CallStaticVoidMethod(env, clazz, updateScreenMethodId);
    			screenWasUpdated = 0;
    		}
    	}
        instructionsSinceLastScreenAccess = 0;
    }
}


void Java_org_puder_trs80_XTRS_setROMSize(JNIEnv* e, jclass clazz, jint size)
{
	trs_rom_size = size;
}


void Java_org_puder_trs80_XTRS_init(JNIEnv* e, jclass cls, jint entryAddr, jbyteArray mem, jbyteArray screen)
{
    env = e;
    clazz = cls;
    isRenderingMethodId = (*env)->GetStaticMethodID(env, cls, "isRendering", "()Z");
    if (isRenderingMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: isRendering not found");
        return;
    }

    updateScreenMethodId = (*env)->GetStaticMethodID(env, cls, "updateScreen", "()V");
    if (updateScreenMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: updateScreen not found");
        return;
    }

    jboolean isCopy;
    memoryArray = (*env)->NewGlobalRef(env, mem);
    memory = (Uchar*) (*env)->GetByteArrayElements(env, mem, &isCopy);
    if (isCopy) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: didn't get copy of array");
        return;
    }

    screenArray = (*env)->NewGlobalRef(env, screen);
    screenBuffer = (*env)->GetByteArrayElements(env, screen, &isCopy);
    if (isCopy) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: didn't get copy of array");
        return;
    }

    init_xtrs(entryAddr);
}

void Java_org_puder_trs80_XTRS_run(JNIEnv* e, jclass cls)
{
	env = e;
	clazz = cls;
	while (isRunning) {
		z80_run(0);
		check_for_screen_updates();
	}
}

void Java_org_puder_trs80_XTRS_cleanup(JNIEnv* env, jclass cls)
{
	(*env)->ReleaseByteArrayElements(env, memoryArray, (jbyte*) memory, JNI_COMMIT);
	(*env)->DeleteGlobalRef(env, memoryArray);
	(*env)->ReleaseByteArrayElements(env, screenArray, screenBuffer, JNI_COMMIT);
	(*env)->DeleteGlobalRef(env, screenArray);
}

void Java_org_puder_trs80_XTRS_setRunning(JNIEnv* e, jclass clazz, jboolean run)
{
    isRunning = run;
}
