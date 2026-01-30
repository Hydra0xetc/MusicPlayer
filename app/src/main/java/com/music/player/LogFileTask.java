package com.music.player;

public class LogFileTask implements Runnable {
    private LogHelper logger;
    private MusicFile file;

    public LogFileTask(LogHelper logger, MusicFile file) {
        this.logger = logger;
        this.file = file;
    }

    public void run() {
        /* Do Nothing */
    }
}
