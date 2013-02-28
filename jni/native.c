#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "z80.h"

#define DEBUG_TAG "NDK"

static jobject obj;
static JNIEnv* env;
static jmethodID pokeMethodId;
static jmethodID peekMethodId;
static jmethodID getMemMethodId;
static jboolean isRunning = 0;
static jbyte* mem;


static byte context_mem_read_callback(int param, ushort address)
{
    return mem[address];
//    return (*env)->CallByteMethod(env, obj, peekMethodId, address);
}

static void context_mem_write_callback(int param, ushort address, byte data)
{
    mem[address] = data;
    if (address >= 0x3c00 && address <= 0x3fff) {
        (*env)->CallVoidMethod(env, obj, pokeMethodId, address, data);
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


void Java_org_puder_trs80_Z80ExecutionThread_bootTRS80(JNIEnv* e, jobject this, jint entryAddr)
{
    obj = this;
    env = e;
    jclass cls = (*env)->GetObjectClass(env, obj);
    pokeMethodId = (*env)->GetMethodID(env, cls, "pokeRAM", "(IB)V");
    if (pokeMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: pokeRAM not found");
        return;
    }
    peekMethodId = (*env)->GetMethodID(env, cls, "peekRAM", "(I)B");
    if (peekMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: peekRAM not found");
        return;
    }
    getMemMethodId = (*env)->GetMethodID(env, cls, "getMem", "()[B");
    if (getMemMethodId == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: getMem not found");
        return;
    }
    
    jobject memArray = (*env)->CallObjectMethod(env, obj, getMemMethodId);
    jboolean isCopy;
    mem = (*env)->GetByteArrayElements(env, memArray, &isCopy);
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
    
    while (isRunning) {
        Z80Execute(&ctx);
    }
    
    (*env)->ReleaseByteArrayElements(env, memArray, mem, JNI_COMMIT);
}

void Java_org_puder_trs80_Z80ExecutionThread_setRunning(JNIEnv* e, jobject this, jboolean run)
{
    isRunning = run;
}