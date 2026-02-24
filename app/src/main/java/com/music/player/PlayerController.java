package com.music.player;

import android.content.Context;
import java.io.File;

public class PlayerController {

    static {
        try {
            System.loadLibrary("audioplayer");
        } catch (UnsatisfiedLinkError e) {
            throw e; // Re-throw to crash the app and get a stack trace
        }
    }

    private long playerPtr = 0;

    private native long createPlayer(String filePath);
    private native void setupPlayer(long ptr, String path);
    private native void play(long ptr);
    private native void pause(long ptr);
    private native void stop(long ptr);
    private native boolean isPlaying(long ptr);
    private native boolean isFinished(long ptr);
    private native void setLoop(long ptr, boolean loop);
    private native long getCurrentPosition(long ptr);
    private native void seekTo(long ptr, int position);
    private native void destroyPlayer(long ptr);
    
    public boolean isReady() {
        return playerPtr != 0;
    }

    public void load(String path) {
        release();
        playerPtr = createPlayer(path);
        if (playerPtr != 0) {
            setupPlayer(playerPtr, path);
        }
    }

    public void play() { play(playerPtr); }
    public void pause() { pause(playerPtr); }
    public void stop() { stop(playerPtr); }
    public boolean isPlaying() { return isPlaying(playerPtr); }
    public boolean isFinished() { return isFinished(playerPtr); }
    public void setLoop(boolean loop) { setLoop(playerPtr, loop); }

    public long getCurrentPosition() { return getCurrentPosition(playerPtr); }
    public void seekTo(int position) { seekTo(playerPtr, position); }

    public void release() {
        if (playerPtr != 0) {
            destroyPlayer(playerPtr);
            playerPtr = 0;
        }
    }
}
