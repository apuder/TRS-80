#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "trs.h"
#include "atrs.h"

#define DEBUG_TAG "Z80"


static jobject obj;
static JNIEnv* env;
static jmethodID isRenderingMethodId;
static jmethodID updateScreenMethodId;
static jboolean isRunning = 0;
static jbyte* screenBuffer;

unsigned char trs_screen[2048];
int instructionsSinceLastScreenAccess;
int screenWasUpdated;

void check_for_screen_updates()
{
    instructionsSinceLastScreenAccess++;
    if (instructionsSinceLastScreenAccess > 2000) {
	       // __android_log_print(ANDROID_LOG_DEBUG, "TRS80", "NDK: instructionsSinceLastScreenAccess > 2000");
    	if (screenWasUpdated) {
    		jboolean isRendering = (*env)->CallBooleanMethod(env, obj, isRenderingMethodId);
    		if (!isRendering) {
    	       // __android_log_print(ANDROID_LOG_DEBUG, "TRS80", "NDK: !isRendering");
    			memcpy(screenBuffer, trs_screen, 0x3fff - 0x3c00 + 1);
    			(*env)->CallVoidMethod(env, obj, updateScreenMethodId);
    			screenWasUpdated = 0;
    		}
    		else {
    	        //__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: skipping screen update");
    		}
    	}
        instructionsSinceLastScreenAccess = 0;
    }
}


#if 0
static byte context_mem_read_callback(int param, ushort address)
{
    return memBuffer[address];
}

static void context_mem_write_callback(int param, ushort address, byte data)
{
    memBuffer[address] = data;
    if (address >= 0x3c00 && address <= 0x3fff) {
    	instructionsSinceLastScreenAccess = 0;
    	screenWasUpdated = 1;
    }
}

static byte context_io_read_callback(int param, ushort address)
{
    byte data = address >> 8;
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: PR %04x %02x", address, data);
    return data;
}

static void context_io_write_callback(int param, ushort address, byte data)
{
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: PW %04x %02x", address, data);
}
#endif

int android_main(Ushort entryAddr);

extern Uchar memory[0x20001];

void Java_org_puder_trs80_Z80ExecutionThread_bootTRS80(JNIEnv* e, jobject this, jint entryAddr, jbyteArray mem, jbyteArray screen)
{
    obj = this;
    env = e;
    jclass cls = (*env)->GetObjectClass(env, obj);
    isRenderingMethodId = (*env)->GetMethodID(env, cls, "isRendering", "()Z");
    if (isRenderingMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: isRendering not found");
        return;
    }

    updateScreenMethodId = (*env)->GetMethodID(env, cls, "updateScreen", "()V");
    if (updateScreenMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: updateScreen not found");
        return;
    }

    jboolean isCopy;
    jbyte* memBuffer = (*env)->GetByteArrayElements(env, mem, &isCopy);
    int i;
    for (i = 0; i < 0x20000; i++) {
    	memory[i] = memBuffer[i];
    }
    (*env)->ReleaseByteArrayElements(env, mem, memBuffer, JNI_COMMIT);

    screenBuffer = (*env)->GetByteArrayElements(env, screen, &isCopy);
    if (isCopy) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: didn't get copy of array");
        return;
    }

    android_main(entryAddr);
#if 0
    Z80Context ctx;
    Z80RESET(&ctx);
    ctx.PC = entryAddr;
    ctx.memRead = context_mem_read_callback;
    ctx.memWrite = context_mem_write_callback;
    ctx.ioRead = context_io_read_callback;
    ctx.ioWrite = context_io_write_callback;
    instructionsSinceLastScreenAccess = 0;
    screenWasUpdated = 0;
    
    while (isRunning) {
    	//ctx.tstates = 0;
        Z80Execute(&ctx);
        instructionsSinceLastScreenAccess++;
        if (instructionsSinceLastScreenAccess > 2000) {
	        //__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: PC=0x%x, islsa=%d, swu=%d", ctx.PC, instructionsSinceLastScreenAccess, screenWasUpdated);
        	if (screenWasUpdated) {
        		jboolean isRendering = (*env)->CallBooleanMethod(env, obj, isRenderingMethodId);
        		if (!isRendering) {
        	       // __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: !isRendering");
        			memcpy(screenBuffer, memBuffer + 0x3c00, 0x3fff - 0x3c00 + 1);
        			(*env)->CallVoidMethod(env, obj, updateScreenMethodId);
        			screenWasUpdated = 0;
        		}
        		else {
        	        //__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: skipping screen update");
        		}
        	}
            instructionsSinceLastScreenAccess = 0;
        }
        if(ctx.tstates > 120) {
        	usleep(5);
        	ctx.tstates -= 120;
        }
    }
#endif

    (*env)->ReleaseByteArrayElements(env, mem, memBuffer, JNI_COMMIT);
    (*env)->ReleaseByteArrayElements(env, mem, screenBuffer, JNI_COMMIT);
}

void Java_org_puder_trs80_Z80ExecutionThread_setRunning(JNIEnv* e, jobject this, jboolean run)
{
    isRunning = run;
}
