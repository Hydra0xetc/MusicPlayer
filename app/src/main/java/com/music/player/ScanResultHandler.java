package com.music.player;

import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import java.util.List;

public class ScanResultHandler implements MusicScanner.ScanListener {
    private MainActivity activity;
    private Handler handler;
    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private Button btnScan;
    private LogHelper logger;

    public ScanResultHandler(MainActivity activity, Handler handler, 
                            List<MusicFile> musicFiles, MusicFileAdapter adapter,
                            Button btnScan, LogHelper logger) {
        this.activity = activity;
        this.handler = handler;
        this.musicFiles = musicFiles;
        this.adapter = adapter;
        this.btnScan = btnScan;
        this.logger = logger;
    }

    public void onScanStarted() {
        ClearListTask task = new ClearListTask(musicFiles, adapter);
        handler.post(task);
    }

    public void onFileFound(MusicFile file) {
        LogFileTask task = new LogFileTask(logger, file);
        handler.post(task);
    }

    public void onScanCompleted(List<MusicFile> files) {
        CompleteTask task = new CompleteTask(activity, musicFiles, adapter, btnScan, logger, files);
        handler.post(task);
    }

    public void onScanError(String error) {
        ErrorTask task = new ErrorTask(activity, btnScan, logger, error);
        handler.post(task);
    }
}
