#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "z80.h"

#define DEBUG_TAG "Z80"

static jobject obj;
static JNIEnv* env;
static jmethodID isRenderingMethodId;
static jmethodID updateScreenMethodId;
static jboolean isRunning = 0;
static jbyte* memBuffer;
static jbyte* screenBuffer;

static int instructionsSinceLastScreenAccess;
static int screenWasUpdated;

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
    memBuffer = (*env)->GetByteArrayElements(env, mem, &isCopy);
    if (isCopy) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: didn't get copy of array");
        return;
    }

    screenBuffer = (*env)->GetByteArrayElements(env, screen, &isCopy);
    if (isCopy) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: didn't get copy of array");
        return;
    }

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
    
    (*env)->ReleaseByteArrayElements(env, mem, memBuffer, JNI_COMMIT);
    (*env)->ReleaseByteArrayElements(env, mem, screenBuffer, JNI_COMMIT);
}

void Java_org_puder_trs80_Z80ExecutionThread_setRunning(JNIEnv* e, jobject this, jboolean run)
{
    isRunning = run;
}
