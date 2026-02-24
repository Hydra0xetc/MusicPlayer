package com.music.player;

import android.os.Handler;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;

public class ScanResultHandler implements MusicScanner.ScanListener {
    final static String TAG = "ScanResultHandler";
    private MainActivity activity;
    private Handler handler;
    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FileLogger fileLogger;

    public ScanResultHandler(MainActivity activity, Handler handler, List<MusicFile> musicFiles, MusicFileAdapter adapter, SwipeRefreshLayout swipeRefresh) {
        this.activity = activity;
        this.handler = handler;
        this.musicFiles = musicFiles;
        this.adapter = adapter;
        this.swipeRefresh = swipeRefresh;
        this.fileLogger = FileLogger.getInstance(activity);
    }

    public void onScanStarted() {
        handler.post(() -> {
            musicFiles.clear();
            adapter.updateList(musicFiles);
            activity.checkEmptyState();
            if (!swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(true);
            }
        });
    }

    public void onFileFound(MusicFile file) { }

    public void onScanCompleted(List<MusicFile> files) {
        handler.post(() -> handleScanCompletion(files));
    }

    private void handleScanCompletion(List<MusicFile> files) {
        musicFiles.clear();
        musicFiles.addAll(files);
        adapter.updateList(musicFiles);
        activity.checkEmptyState();

        swipeRefresh.setRefreshing(false);

        int count = files.size();
        Toast.makeText(activity, count + " music file(s) found", Toast.LENGTH_SHORT).show();

        if (files.isEmpty()) {
            fileLogger.e(TAG, "No audio files found in directory");
        }

        activity.updatePlaylist();
    }

    public void onScanError(String error) {
        handler.post(() -> handleScanError(error));
    }

    private void handleScanError(String error) {
        swipeRefresh.setRefreshing(false);
        fileLogger.e(TAG, "ERROR: Scan failed - " + error);
        Toast.makeText(activity, "Scan failed: " + error, Toast.LENGTH_SHORT).show();
    }
}
