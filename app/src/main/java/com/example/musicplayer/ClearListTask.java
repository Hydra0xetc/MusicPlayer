package com.example.musicplayer;

import java.util.List;

public class ClearListTask implements Runnable {
    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;

    public ClearListTask(List<MusicFile> musicFiles, MusicFileAdapter adapter) {
        this.musicFiles = musicFiles;
        this.adapter = adapter;
    }

    public void run() {
        musicFiles.clear();
        adapter.notifyDataSetChanged();
    }
}
