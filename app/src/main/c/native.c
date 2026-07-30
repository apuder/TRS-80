/*
 * native.c - JNI binding for the TRS-80 emulator core.
 *
 * This file is the Android host's adapter and nothing more: it converts JNI
 * types to plain C, forwards to trs80_core.h, and routes the core's callbacks
 * back up into Java. All emulator logic lives in trs80_core.c.
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>

#include "trs80_core.h"

#define NO_ERROR 0
#define ERR_GET_JVM -1
#define ERR_GET_METHOD_NOT_IMPLEMENTED -10

static JavaVM *jvm;
static jclass clazzXTRS = NULL;
static jmethodID notImplementedMethodId;

static JNIEnv *getEnv()
{
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, (JNIEnv **) &env, NULL);
    return env;
}

/* Called by the core when it hits an unsupported code path. */
static void on_not_implemented(const char *msg)
{
    JNIEnv *env = getEnv();
    jstring jmsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallStaticVoidMethod(env, clazzXTRS, notImplementedMethodId, jmsg);
    (*env)->DeleteLocalRef(env, jmsg);
}

/*
 * Copies a Java string into a caller-supplied buffer. Returns NULL for a null
 * jstring so it maps directly onto the core's "NULL means empty drive" contract.
 */
static const char *copy_string(JNIEnv *env, jstring str, char *buf, size_t size)
{
    if (str == NULL) {
        return NULL;
    }
    const char *chars = (*env)->GetStringUTFChars(env, str, NULL);
    strncpy(buf, chars, size - 1);
    buf[size - 1] = '\0';
    (*env)->ReleaseStringUTFChars(env, str, chars);
    return buf;
}

JNIEXPORT jint JNICALL
Java_org_puder_trs80_XTRS_initNative(JNIEnv *env, jclass cls, jint model,
                                     jstring romFile, jint entryAddr,
                                     jstring cassette, jstring disk0,
                                     jstring disk1, jstring disk2, jstring disk3)
{
    char romBuf[FILENAME_MAX];
    char cassetteBuf[FILENAME_MAX];
    char diskBuf[4][FILENAME_MAX];
    trs80_config config;
    trs80_host host;

    if ((*env)->GetJavaVM(env, &jvm) != 0) {
        return ERR_GET_JVM;
    }

    if (clazzXTRS == NULL) {
        clazzXTRS = (*env)->NewGlobalRef(env, cls);
    }

    notImplementedMethodId = (*env)->GetStaticMethodID(env, cls, "notImplemented",
                                                       "(Ljava/lang/String;)V");
    if (notImplementedMethodId == 0) {
        return ERR_GET_METHOD_NOT_IMPLEMENTED;
    }

    host.not_implemented = on_not_implemented;
    trs80_set_host(&host);

    memset(&config, 0, sizeof(config));
    config.model = model;
    config.entry_addr = (unsigned short) entryAddr;
    config.rom_path = copy_string(env, romFile, romBuf, sizeof(romBuf));
    config.cassette_path = copy_string(env, cassette, cassetteBuf, sizeof(cassetteBuf));
    config.disk_path[0] = copy_string(env, disk0, diskBuf[0], sizeof(diskBuf[0]));
    config.disk_path[1] = copy_string(env, disk1, diskBuf[1], sizeof(diskBuf[1]));
    config.disk_path[2] = copy_string(env, disk2, diskBuf[2], sizeof(diskBuf[2]));
    config.disk_path[3] = copy_string(env, disk3, diskBuf[3], sizeof(diskBuf[3]));

    int rc = trs80_init(&config);
    return rc == TRS80_OK ? NO_ERROR : rc;
}

JNIEXPORT jobject JNICALL
Java_org_puder_trs80_XTRS_getScreenBufferNative(JNIEnv *env, jclass cls)
{
    return (*env)->NewDirectByteBuffer(env, trs80_screen_buffer(),
                                       TRS80_SCREEN_BUFFER_SIZE);
}

JNIEXPORT jobject JNICALL
Java_org_puder_trs80_XTRS_getPixelBufferNative(JNIEnv *env, jclass cls)
{
    return (*env)->NewDirectByteBuffer(env, trs80_pixel_buffer(),
                                       (jlong) trs80_pixel_width() * trs80_pixel_height());
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_setCellSize(JNIEnv *env, jclass cls, jint width, jint height)
{
    trs80_set_cell_size(width, height);
}

JNIEXPORT jint JNICALL
Java_org_puder_trs80_XTRS_pixelWidth(JNIEnv *env, jclass cls)
{
    return trs80_pixel_width();
}

JNIEXPORT jint JNICALL
Java_org_puder_trs80_XTRS_pixelHeight(JNIEnv *env, jclass cls)
{
    return trs80_pixel_height();
}

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_render(JNIEnv *env, jclass cls)
{
    return trs80_render() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_invalidateRender(JNIEnv *env, jclass cls)
{
    trs80_invalidate_render();
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_run(JNIEnv *env, jclass cls)
{
    trs80_run();
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_setRunning(JNIEnv *env, jclass cls, jboolean run)
{
    trs80_set_running(run);
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_reset(JNIEnv *env, jclass cls)
{
    trs80_reset();
}

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_isExpandedMode(JNIEnv *env, jclass cls)
{
    return trs80_is_expanded_mode() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_addKeyEvent(JNIEnv *env, jclass cls, jint event,
                                      jint sym, jint key)
{
    trs80_add_key_event(event, sym, key);
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_paste(JNIEnv *env, jclass cls, jstring clipboard)
{
    const char *cb = (*env)->GetStringUTFChars(env, clipboard, NULL);
    trs80_paste(cb, (*env)->GetStringUTFLength(env, clipboard));
    (*env)->ReleaseStringUTFChars(env, clipboard, cb);
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_saveState(JNIEnv *env, jclass cls, jstring fileName)
{
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    trs80_save_state(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_loadState(JNIEnv *env, jclass cls, jstring fileName)
{
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    trs80_load_state(fn);
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_rewindCassette(JNIEnv *env, jclass cls)
{
    trs80_rewind_cassette();
}

JNIEXPORT jfloat JNICALL
Java_org_puder_trs80_XTRS_getCassettePosition(JNIEnv *env, jclass cls)
{
    return (jfloat) trs80_cassette_position();
}

JNIEXPORT void JNICALL
Java_org_puder_trs80_XTRS_setSoundMuted(JNIEnv *env, jclass cls, jboolean muted)
{
    trs80_set_sound_muted(muted);
}

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_createBlankJV1(JNIEnv *env, jclass cls, jstring fileName)
{
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    jboolean ok = trs80_create_blank_jv1(fn) ? JNI_TRUE : JNI_FALSE;
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return ok;
}

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_createBlankJV3(JNIEnv *env, jclass cls, jstring fileName)
{
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    jboolean ok = trs80_create_blank_jv3(fn) ? JNI_TRUE : JNI_FALSE;
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return ok;
}

JNIEXPORT jboolean JNICALL
Java_org_puder_trs80_XTRS_createBlankDMK(JNIEnv *env, jclass cls, jstring fileName,
                                         jint sides, jint density, jint eight,
                                         jint ignden)
{
    const char *fn = (*env)->GetStringUTFChars(env, fileName, NULL);
    jboolean ok = trs80_create_blank_dmk(fn, sides, density, eight, ignden)
                  ? JNI_TRUE : JNI_FALSE;
    (*env)->ReleaseStringUTFChars(env, fileName, fn);
    return ok;
}
