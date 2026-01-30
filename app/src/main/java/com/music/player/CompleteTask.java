package com.music.player;

import android.widget.Button;
import android.widget.Toast;
import java.util.List;

public class CompleteTask implements Runnable {
    private MainActivity activity;
    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private Button btnScan;
    private LogHelper logger;
    private List<MusicFile> files;

    public CompleteTask(
        MainActivity activity,
        List<MusicFile> musicFiles, 
        MusicFileAdapter adapter,
        Button btnScan, 
        LogHelper logger,
        List<MusicFile> files
) {
        this.activity = activity;
        this.musicFiles = musicFiles;
        this.adapter = adapter;
        this.btnScan = btnScan;
        this.logger = logger;
        this.files = files;
    }

    public void run() {
        musicFiles.clear();
        musicFiles.addAll(files);
        adapter.notifyDataSetChanged();
        
        btnScan.setEnabled(true);
        btnScan.setText("Scan");
        
        int count = files.size();
        logger.log("Scan completed: " + count + " file(s) found");
        // BUG: Toast triggered twice
        Toast.makeText(activity, count + " music file(s) found", Toast.LENGTH_SHORT).show();
        
        if (files.isEmpty()) {
            logger.log("No audio files found in directory");
        }
    }
}
