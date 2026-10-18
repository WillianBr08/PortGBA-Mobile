#include "engine.h"
#include <jni.h>
#include <android/native_window_jni.h>
#include <string.h>
#include <stdint.h>

static ANativeWindow* window;
static void present_frame(struct EliteEngine* e) {
    if (window && e) {
        ANativeWindow_Buffer b;
        if (ANativeWindow_lock(window, &b, NULL) == 0) {
            for (int y = 0; y < 160; y++) {
                uint32_t* row = (uint32_t*)b.bits + y * b.stride;
                for (int x = 0; x < 240; x++) row[x] = e->pixels[y * 240 + x] | 0xFF000000;
            }
            ANativeWindow_unlockAndPost(window);
        }
    }
}
#define FN(name) Java_dev_privado_eliteredux_Native_##name
JNIEXPORT jlong JNICALL FN(open)(JNIEnv* env, jclass cls, jstring rom, jstring save) {
    const char* r = (*env)->GetStringUTFChars(env, rom, NULL);
    const char* s = (*env)->GetStringUTFChars(env, save, NULL);
    struct EliteEngine* e = elite_open(r, s);
    (*env)->ReleaseStringUTFChars(env, rom, r); (*env)->ReleaseStringUTFChars(env, save, s);
    return (jlong)(intptr_t)e;
}
JNIEXPORT void JNICALL FN(surface)(JNIEnv* env, jclass cls, jobject surface) {
    if (window) { ANativeWindow_release(window); window = NULL; }
    if (surface) { window = ANativeWindow_fromSurface(env, surface); ANativeWindow_setBuffersGeometry(window, 240, 160, WINDOW_FORMAT_RGBA_8888); }
}
JNIEXPORT jint JNICALL FN(step)(JNIEnv* env, jclass cls, jlong ptr, jint keys, jint frames, jint skip, jshortArray samples, jboolean present) {
    struct EliteEngine* e = (void*)(intptr_t)ptr;
    if(e) e->frameskip = skip >= 0 && skip <= 7 ? skip : 0;
    short audio[4096];
    int count = elite_step(e, keys, frames, audio, 4096);
    if (count && samples) (*env)->SetShortArrayRegion(env, samples, 0, count, audio);
    if (present) present_frame(e);
    return count;
}
JNIEXPORT jboolean JNICALL FN(state)(JNIEnv* env, jclass cls, jlong ptr, jstring path, jboolean save) {
    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    int ok = elite_state((void*)(intptr_t)ptr, p, save);
    if (ok && !save) present_frame((void*)(intptr_t)ptr);
    (*env)->ReleaseStringUTFChars(env, path, p); return ok;
}
JNIEXPORT jboolean JNICALL FN(flush)(JNIEnv* env, jclass cls, jlong ptr) { return elite_flush((void*)(intptr_t)ptr); }
JNIEXPORT jboolean JNICALL FN(importBattery)(JNIEnv* env, jclass cls, jlong ptr, jstring path) {
    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    int ok = elite_import_battery((void*)(intptr_t)ptr, p);
    (*env)->ReleaseStringUTFChars(env, path, p); return ok;
}
JNIEXPORT void JNICALL FN(close)(JNIEnv* env, jclass cls, jlong ptr) { elite_close((void*)(intptr_t)ptr); }
