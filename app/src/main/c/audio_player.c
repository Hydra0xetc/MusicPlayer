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
// Untuk non-Android
#define LOGI(...) printf("[INFO] " __VA_ARGS__); printf("\n")
#define LOGE(...) printf("[ERROR] " __VA_ARGS__); printf("\n")
#endif

// Buffer queue callback (tandai sebagai digunakan jika diperlukan)
void SLAPIENTRY bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void* context) {
    (void)bq; // Mark parameter as unused
    (void)context; // Mark parameter as unused
    // Callback untuk buffer queue (untuk streaming)
    // Diimplementasikan sesuai kebutuhan
    LOGI("Buffer queue callback called");
}

AudioPlayer* createAudioPlayer(const char* filePath) {
    (void)filePath; // Mark parameter as unused
    
    AudioPlayer* player = (AudioPlayer*)malloc(sizeof(AudioPlayer));
    if (!player) {
        LOGE("Gagal mengalokasikan memori untuk AudioPlayer");
        return NULL;
    }
    
    memset(player, 0, sizeof(AudioPlayer));
    
    // Inisialisasi engine OpenSL ES
    SLresult result;
    
    // Buat engine
    result = slCreateEngine(&player->engineObject, 0, NULL, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal membuat engine: %d", result);
        free(player);
        return NULL;
    }
    
    // Realisasikan engine
    result = (*player->engineObject)->Realize(player->engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal merealisasikan engine: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Dapatkan interface engine
    result = (*player->engineObject)->GetInterface(
        player->engineObject, SL_IID_ENGINE, &player->engineEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal mendapatkan interface engine: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Buat output mix
    result = (*player->engineEngine)->CreateOutputMix(
        player->engineEngine, &player->outputMixObject, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal membuat output mix: %d", result);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    // Realisasikan output mix
    result = (*player->outputMixObject)->Realize(
        player->outputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal merealisasikan output mix: %d", result);
        (*player->outputMixObject)->Destroy(player->outputMixObject);
        (*player->engineObject)->Destroy(player->engineObject);
        free(player);
        return NULL;
    }
    
    LOGI("Engine OpenSL ES berhasil diinisialisasi");
    return player;
}

void setupUriAudioPlayer(AudioPlayer* player, const char* filePath) {
    if (!player || !filePath) {
        LOGE("Player atau filePath NULL");
        return;
    }
    
    SLresult result;
    
    // Konfigurasi data source untuk URI
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
    
    // Konfigurasi data sink
    SLDataLocator_OutputMix loc_outmix = {
        SL_DATALOCATOR_OUTPUTMIX,
        player->outputMixObject
    };
    
    SLDataSink audioSnk = {&loc_outmix, NULL};
    
    // Buat audio player
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
        LOGE("Gagal membuat audio player: %d", result);
        return;
    }
    
    // Realisasikan player
    result = (*player->playerObject)->Realize(
        player->playerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal merealisasikan player: %d", result);
        return;
    }
    
    // Dapatkan interface play
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_PLAY, &player->playerPlay);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal mendapatkan interface play: %d", result);
        return;
    }
    
    // Dapatkan interface seek
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_SEEK, &player->playerSeek);
    if (result != SL_RESULT_SUCCESS) {
        LOGI("Interface seek tidak tersedia, melanjutkan tanpa seek");
        // Bukan error fatal, lanjutkan
    }
    
    // Dapatkan interface volume
    result = (*player->playerObject)->GetInterface(
        player->playerObject, SL_IID_VOLUME, &player->playerVolume);
    if (result != SL_RESULT_SUCCESS) {
        LOGI("Interface volume tidak tersedia, melanjutkan tanpa kontrol volume");
        // Bukan error fatal, lanjutkan
    }
    
    // Set callback untuk events
    result = (*player->playerPlay)->RegisterCallback(
        player->playerPlay, playbackCallback, player);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal mendaftarkan callback: %d", result);
    }
    
    // Enable event callback
    result = (*player->playerPlay)->SetCallbackEventsMask(
        player->playerPlay, SL_PLAYEVENT_HEADATEND);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal mengatur event mask: %d", result);
    }
    
    player->isPrepared = true;
    LOGI("Audio player berhasil disiapkan untuk: %s", filePath);
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
        LOGI("Audio selesai (callback)");
    }
}

void playAudio(AudioPlayer* player) {
    if (!player || !player->isPrepared) {
        LOGE("Player tidak siap");
        return;
    }

    player->finished = false;
    
    SLresult result = (*player->playerPlay)->SetPlayState(
        player->playerPlay, SL_PLAYSTATE_PLAYING);
    
    if (result == SL_RESULT_SUCCESS) {
        player->isPlaying = true;
        LOGI("Memulai pemutaran");
    } else {
        LOGE("Gagal memulai pemutaran: %d", result);
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
        LOGI("Dijeda");
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
        
        // Kembali ke awal
        if (player->playerSeek) {
            (*player->playerSeek)->SetPosition(
                player->playerSeek, 0, SL_SEEKMODE_FAST);
        }
        LOGI("Dihentikan");
    }
}

void setLooping(AudioPlayer* player, bool loop) {
    if (!player || !player->playerSeek) {
        LOGI("Seek interface tidak tersedia, tidak bisa mengatur looping");
        return;
    }
    
    SLresult result = (*player->playerSeek)->SetLoop(
        player->playerSeek, 
        loop ? SL_BOOLEAN_TRUE : SL_BOOLEAN_FALSE,
        0, SL_TIME_UNKNOWN);
    
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Gagal mengatur looping: %d", result);
    } else {
        LOGI("Looping diatur ke: %s", loop ? "true" : "false");
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
    
    LOGI("Membersihkan audio player...");
    
    // Hentikan pemutaran
    if (player->playerPlay) {
        (*player->playerPlay)->SetPlayState(
            player->playerPlay, SL_PLAYSTATE_STOPPED);
    }
    
    // Hancurkan player
    if (player->playerObject) {
        (*player->playerObject)->Destroy(player->playerObject);
        player->playerObject = NULL;
    }
    
    // Hancurkan output mix
    if (player->outputMixObject) {
        (*player->outputMixObject)->Destroy(player->outputMixObject);
        player->outputMixObject = NULL;
    }
    
    // Hancurkan engine
    if (player->engineObject) {
        (*player->engineObject)->Destroy(player->engineObject);
        player->engineObject = NULL;
    }
    
    free(player);
    LOGI("Audio player dihancurkan");
}
