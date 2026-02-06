package com.music.player;

import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import java.util.List;

public class ScanResultHandler implements MusicScanner.ScanListener {
    final static String TAG = "ScanResultHandler";
    private MainActivity activity;
    private Handler handler;
    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private Button btnScan;
    private FileLogger fileLogger;

    public ScanResultHandler(
        MainActivity activity, Handler handler, List<MusicFile> musicFiles,
        MusicFileAdapter adapter, Button btnScan) {

        this.activity = activity;
        this.handler = handler;
        this.musicFiles = musicFiles;
        this.adapter = adapter;
        this.btnScan = btnScan;
    }

    public void onScanStarted() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                musicFiles.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    // this may better if i can delete this
    public void onFileFound(MusicFile file) { }

    public void onScanCompleted(List<MusicFile> files) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handleScanCompletion(files);
            }
        });
    }

    private void handleScanCompletion(List<MusicFile> files) {
        musicFiles.clear();
        musicFiles.addAll(files);
        adapter.notifyDataSetChanged();

        btnScan.setEnabled(true);
        btnScan.setText("Scan");

        int count = files.size();
        Toast.makeText(activity, count + " music file(s) found", Toast.LENGTH_SHORT).show();

        if (files.isEmpty()) {
            fileLogger.e(TAG, "No audio files found in directory");
        }

        activity.updatePlaylist();
    }

    public void onScanError(String error) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handleScanError(error);
            }
        });
    }

    private void handleScanError(String error) {
        btnScan.setEnabled(true);
        btnScan.setText("Ready");
        fileLogger.e(TAG, "ERROR: Scan failed - " + error);
        Toast.makeText(activity, "Scan failed: " + error, Toast.LENGTH_SHORT).show();
    }
}
