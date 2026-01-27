#ifndef AUDIO_PLAYER_H
#define AUDIO_PLAYER_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <stdbool.h>

// Struktur AudioPlayer
typedef struct {
    // OpenSL ES objects
    SLObjectItf engineObject;
    SLEngineItf engineEngine;
    SLObjectItf outputMixObject;
    SLObjectItf playerObject;
    
    // Interfaces
    SLPlayItf playerPlay;
    SLSeekItf playerSeek;
    SLVolumeItf playerVolume;
    
    // State
    bool isPlaying;
    bool isPrepared;
    bool finished;
} AudioPlayer;

// Function declarations
AudioPlayer* createAudioPlayer(const char* filePath);
void setupUriAudioPlayer(AudioPlayer* player, const char* filePath);
void playAudio(AudioPlayer* player);
void pauseAudio(AudioPlayer* player);
void stopAudio(AudioPlayer* player);
void setLooping(AudioPlayer* player, bool loop);
bool isAudioPlaying(AudioPlayer* player);
bool isAudioFinished(AudioPlayer* player);
void destroyAudioPlayer(AudioPlayer* player);

// Callback
void SLAPIENTRY playbackCallback(SLPlayItf caller, void* context, SLuint32 event);

#endif // AUDIO_PLAYER_H
