#include "audio_player.h"
#include <SLES/OpenSLES.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "AudioPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
// For non-Android
#define LOGI(...) printf("[INFO] " __VA_ARGS__); printf("\n")
#define LOGE(...) printf("[ERROR] " __VA_ARGS__); printf("\n")
#endif

AudioPlayer* createAudioPlayer(const char* filePath) {
    (void)filePath; // Mark parameter as unused
    
    AudioPlayer* player = (AudioPlayer*)malloc(sizeof(AudioPlayer));
    if (!player) {
        LOGE("Failed to allocate memory for AudioPlayer");
        return NULL;
    }
    
    memset(player, 0, sizeof(AudioPlayer));
    
    // Initialize OpenSL ES engine
    SLresult result;
    
    // Create engine
    result = slCreateEngine(&player->engineObject, 0, NULL, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create engine: %d", result);
        free(player);
        return NULL;
    }
    
    // Realize engine
    result = (*player->engineObject)->Realize(player->engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize engine: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Get engine interface
    result = (*player->engineObject)->GetInterface(
        player->engineObject, SL_IID_ENGINE, &player->engineEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get engine interface: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Create output mix
    result = (*player->engineEngine)->CreateOutputMix(
        player->engineEngine, &player->outputMixObject, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create output mix: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Realize output mix
    result = (*player->outputMixObject)->Realize(
        player->outputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize output mix: %d", result);
        (*player->outputMixObject)->Destroy(player->outputMixObject);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    LOGI("OpenSL ES engine initialized successfully");
    return player;
}

void setupUriAudioPlayer(AudioPlayer* player, const char* filePath) {
    if (!player || !filePath) {
        LOGE("Player or filePath is NULL");
        return;
    }
    
    SLresult result;
    
    // Configure data source for URI
    SLDataLocator_URI loc_uri = {
        SL_DATALOCATOR_URI,
        (SLchar*)filePath
    };
    
    SLDataFormat_MIME format_mime = {
        SL_DATAFORMAT_MIME,
        NULL,
        SL_CONTAINERTYPE_UNSPECIFIED
    };
    
    SLDataSource audioSrc = {&loc_uri, &format_mime};
    
    // Configure data sink
    SLDataLocator_OutputMix loc_outmix = {
        SL_DATALOCATOR_OUTPUTMIX,
        player->outputMixObject
    };
    
    SLDataSink audioSnk = {&loc_outmix, NULL};
    
    // Create audio player
    const SLInterfaceID ids[3] = {
        SL_IID_PLAY,
        SL_IID_SEEK,
        SL_IID_VOLUME
    };
    
    const SLboolean req[3] = {
        SL_BOOLEAN_TRUE,
        SL_BOOLEAN_TRUE,
        SL_BOOLEAN_TRUE
    };
    
    result = (*player->engineEngine)->CreateAudioPlayer(
        player->engineEngine,
        &player->playerObject,
        &audioSrc,
        &audioSnk,
        3,
        ids,
        req
    );
    
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio player: %d", result);
        return;
    }
    
    // Realize player
    result = (*player->playerObject)->Realize(
        player->playerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize player: %d", result);
        return;
    }
    
    // Get play interface
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_PLAY, &player->playerPlay);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get play interface: %d", result);
        return;
    }
    
    // Get seek interface
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_SEEK, &player->playerSeek);
    if (result != SL_RESULT_SUCCESS) {
        // Seek interface not available, continuing without seek
    }
    
    // Get volume interface
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_VOLUME, &player->playerVolume);
    if (result != SL_RESULT_SUCCESS) {
        // Volume interface not available, continuing without volume control
    }
    
    // Set callback for events
    result = (*player->playerPlay)->RegisterCallback(
        player->playerPlay, playbackCallback, player);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to register callback: %d", result);
    }
    
    // Enable event callback
    result = (*player->playerPlay)->SetCallbackEventsMask(
        player->playerPlay, SL_PLAYEVENT_HEADATEND);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to set event mask: %d", result);
    }
    
    player->isPrepared = true;
    LOGI("Audio player successfully prepared for: %s", filePath);
}

void SLAPIENTRY playbackCallback(
    SLPlayItf caller,
    void* context,
    SLuint32 event
) {
    
    (void)caller;
    AudioPlayer *player = context;

    if (event & SL_PLAYEVENT_HEADATEND) {
        player->isPlaying = false;
        player->finished = true;
        LOGI("Audio finished (callback)");
    }
}

void playAudio(AudioPlayer* player) {
    if (!player || !player->isPrepared) {
        LOGE("Player not ready");
        return;
    }

    player->finished = false;
    
    SLresult result = (*player->playerPlay)->SetPlayState(
        player->playerPlay, SL_PLAYSTATE_PLAYING);
    
    if (result == SL_RESULT_SUCCESS) {
        player->isPlaying = true;
        LOGI("Starting playback");
    } else {
        LOGE("Failed to start playback: %d", result);
    }
}

void pauseAudio(AudioPlayer* player) {
    if (!player || !player->playerPlay) {
        return;
    }
    
    SLresult result = (*player->playerPlay)->SetPlayState(
        player->playerPlay, SL_PLAYSTATE_PAUSED);
    
    if (result == SL_RESULT_SUCCESS) {
        player->isPlaying = false;
        LOGI("Paused");
    }
}

void stopAudio(AudioPlayer* player) {
    if (!player || !player->playerPlay) {
        return;
    }
    
    SLresult result = (*player->playerPlay)->SetPlayState(
        player->playerPlay, SL_PLAYSTATE_STOPPED);
    
    if (result == SL_RESULT_SUCCESS) {
        player->isPlaying = false;
        player->finished = false;
        
        // Back to start
        if (player->playerSeek) {
            (*player->playerSeek)->SetPosition(
                player->playerSeek, 0, SL_SEEKMODE_FAST);
        }
        LOGI("Stopped");
    }
}

void setLooping(AudioPlayer* player, bool loop) {
    if (!player || !player->playerSeek) {
        LOGI("Seek interface not available, cannot set looping");
        return;
    }
    
    SLresult result = (*player->playerSeek)->SetLoop(
        player->playerSeek, 
        loop ? SL_BOOLEAN_TRUE : SL_BOOLEAN_FALSE,
        0, SL_TIME_UNKNOWN);
    
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to set looping: %d", result);
    } else {
        LOGI("Looping set to: %s", loop ? "true" : "false");
    }
}

SLmillisecond getCurrentPosition(AudioPlayer* player) {
    if (!player || !player->playerPlay) {
        LOGE("Player or playerPlay interface is NULL");
        return 0;
    }
    SLmillisecond currentPos;
    SLresult result = (*player->playerPlay)->GetPosition(player->playerPlay, &currentPos);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get current position: %d", result);
        return 0;
    }
    return currentPos;
}

SLmillisecond getDuration(AudioPlayer* player) {
    if (!player || !player->playerPlay) {
        LOGE("Player or playerPlay interface is NULL");
        return 0;
    }
    SLmillisecond duration;
    SLresult result = (*player->playerPlay)->GetDuration(player->playerPlay, &duration);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get duration: %d", result);
        return 0;
    }
    return duration;
}

void seekToPosition(AudioPlayer* player, SLmillisecond position) {
    if (!player || !player->playerSeek) {
        LOGE("Seek interface not available, cannot seek");
        return;
    }
    SLresult result = (*player->playerSeek)->SetPosition(
        player->playerSeek, position, SL_SEEKMODE_ACCURATE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to seek to position %lu: %d", (unsigned long)position, result);
    } else {
        LOGI("Seeked to position: %lu", (unsigned long)position);
        player->finished = false; // Reset finished flag after seeking
    }
}

bool isAudioPlaying(AudioPlayer* player) {
    if (!player) return false;
    
    if (player->playerPlay) {
        SLuint32 state;
        SLresult result = (*player->playerPlay)->GetPlayState(
            player->playerPlay, &state);
        
        if (result == SL_RESULT_SUCCESS) {
            player->isPlaying = (state == SL_PLAYSTATE_PLAYING);
        }
    }
    
    return player->isPlaying;
}

bool isAudioFinished(AudioPlayer* player) {
    if (!player) {
        return false;
    }

    return player->finished;
}

void destroyAudioPlayer(AudioPlayer* player) {
    if (!player) return;
    
    LOGI("Cleaning up audio player...");
    
    // Stop playback
    if (player->playerPlay) {
        (*player->playerPlay)->SetPlayState(
            player->playerPlay, SL_PLAYSTATE_STOPPED);
    }
    
    // Destroy player
    if (player->playerObject) {
        (*player->playerObject)->Destroy(player->playerObject);
        player->playerObject = NULL;
    }
    
    // Destroy output mix
    if (player->outputMixObject) {
        (*player->outputMixObject)->Destroy(player->outputMixObject);
        player->outputMixObject = NULL;
    }
    
    // Destroy engine
    if (player->engineObject) {
        (*player->engineObject)->Destroy(player->engineObject);
        player->engineObject = NULL;
    }
    
    free(player);
    LOGI("Audio player destroyed");
}
