package com.example.musicplayer;

public class PlayerController {

    static {
        try {
            System.loadLibrary("audioplayer");
        } catch (UnsatisfiedLinkError ignored) {}
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

    public void release() {
        if (playerPtr != 0) {
            destroyPlayer(playerPtr);
            playerPtr = 0;
        }
    }
}
