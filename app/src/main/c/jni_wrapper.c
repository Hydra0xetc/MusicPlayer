#include <jni.h>
#include <android/log.h>
#include "audio_player.h"

#define LOG_TAG "MusicPlayerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Create AudioPlayer
JNIEXPORT jlong JNICALL
Java_com_example_musicplayer_PlayerController_createPlayer(JNIEnv *env, jobject thiz, jstring filePath) {
    (void)thiz;
    
    const char *path = (*env)->GetStringUTFChars(env, filePath, NULL);
    if (path == NULL) {
        LOGE("Failed to get file path string");
        return 0;
    }
    
    LOGI("Creating player for: %s", path);
    
    AudioPlayer* player = createAudioPlayer(path);
    
    (*env)->ReleaseStringUTFChars(env, filePath, path);
    
    if (player == NULL) {
        LOGE("Failed to create AudioPlayer");
        return 0;
    }
    
    LOGI("Player created successfully at address: %p", (void*)player);
    return (jlong)player;
}

// Setup player with URI
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_setupPlayer(JNIEnv *env, jobject thiz, jlong playerPtr, jstring filePath) {
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in setupPlayer");
        return;
    }
    
    const char *path = (*env)->GetStringUTFChars(env, filePath, NULL);
    if (path == NULL) {
        LOGE("Failed to get file path string in setupPlayer");
        return;
    }
    
    LOGI("Setting up player with file: %s", path);
    
    setupUriAudioPlayer(player, path);
    
    (*env)->ReleaseStringUTFChars(env, filePath, path);
    
    LOGI("Player setup completed");
}

// Play audio
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_play(JNIEnv *env, jobject thiz, jlong playerPtr) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in play");
        return;
    }
    
    LOGI("Playing audio");
    playAudio(player);
}

// Pause audio
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_pause(JNIEnv *env, jobject thiz, jlong playerPtr) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in pause");
        return;
    }
    
    LOGI("Pausing audio");
    pauseAudio(player);
}

// Stop audio
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_stop(JNIEnv *env, jobject thiz, jlong playerPtr) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in stop");
        return;
    }
    
    LOGI("Stopping audio");
    stopAudio(player);
}

// Set looping
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_setLoop(JNIEnv *env, jobject thiz, jlong playerPtr, jboolean loop) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in setLoop");
        return;
    }
    
    LOGI("Setting loop to: %d", loop);
    setLooping(player, loop == JNI_TRUE);
}

// Check if playing
JNIEXPORT jboolean JNICALL
Java_com_example_musicplayer_PlayerController_isPlaying(
        JNIEnv *env, jobject thiz, jlong playerPtr
) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in isPlaying");
        return JNI_FALSE;
    }
    
    bool playing = isAudioPlaying(player);
    return playing ? JNI_TRUE : JNI_FALSE;
}

// Check if finished
JNIEXPORT jboolean JNICALL
Java_com_example_musicplayer_PlayerController_isFinished(JNIEnv *env, jobject thiz, jlong playerPtr) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in isFinished");
        return JNI_FALSE;
    }
    
    bool finished = isAudioFinished(player);
    return finished ? JNI_TRUE : JNI_FALSE;
}

// Destroy player
JNIEXPORT void JNICALL
Java_com_example_musicplayer_PlayerController_destroyPlayer(JNIEnv *env, jobject thiz, jlong playerPtr) {
    (void)env;
    (void)thiz;
    
    AudioPlayer* player = (AudioPlayer*)playerPtr;
    if (player == NULL) {
        LOGE("Player pointer is NULL in destroyPlayer");
        return;
    }
    
    LOGI("Destroying player at address: %p", (void*)player);
    destroyAudioPlayer(player);
}
