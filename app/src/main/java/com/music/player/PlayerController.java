package com.music.player;

import java.io.File;

public class PlayerController {

    private final static String TAG = "PlayerController";

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
    private native long getDuration(long ptr);
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

    // Add null pointer checks before calling native methods
    public void play() { 
        if (playerPtr != 0) {
            play(playerPtr); 
        }
    }
    
    public void pause() { 
        if (playerPtr != 0) {
            pause(playerPtr); 
        }
    }
    
    public void stop() { 
        if (playerPtr != 0) {
            stop(playerPtr); 
        }
    }
    
    public boolean isPlaying() { 
        if (playerPtr != 0) {
            return isPlaying(playerPtr); 
        }
        return false;
    }
    
    public boolean isFinished() { 
        if (playerPtr != 0) {
            return isFinished(playerPtr); 
        }
        return false;
    }
    
    public void setLoop(boolean loop) { 
        if (playerPtr != 0) {
            setLoop(playerPtr, loop); 
        }
    }

    public long getCurrentPosition() { 
        if (playerPtr != 0) {
            long pos = getCurrentPosition(playerPtr);
            return (pos < 0) ? 0 : pos;
        }
        return 0;
    }
    
    public long getDuration() { 
        if (playerPtr != 0) {
            return getDuration(playerPtr); 
        }
        return 0;
    }
    
    public void seekTo(int position) { 
        if (playerPtr != 0) {
            seekTo(playerPtr, position); 
        }
    }

    public void release() {
        if (playerPtr != 0) {
            destroyPlayer(playerPtr);
            playerPtr = 0;
        }
    }
}
