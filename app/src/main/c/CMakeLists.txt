cmake_minimum_required(VERSION 3.10.2)
project("musicplayer")

add_library(
    audioplayer
    SHARED
    audio_player.c
    jni_wrapper.c
)

find_library(log-lib log)
find_library(OpenSLES-lib OpenSLES)

target_link_libraries(audioplayer ${log-lib} ${OpenSLES-lib})
