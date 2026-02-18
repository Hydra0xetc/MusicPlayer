package com.music.player;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.widget.AbsListView;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.MotionEvent;
import android.animation.ValueAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements MusicService.MusicServiceListener, PlaybackUIController.MusicServiceWrapper {
    final static String TAG = "MainActivity";
    private ListView lvMusicFiles;
    private SwipeRefreshLayout swipeRefreshLayout;

    private MusicService musicService;
    private boolean isBound = false;
    
    private FileLogger fileLogger;
    private Handler mainHandler;
    private ConfigManager configManager;

    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private MusicFile currentMusic;
    private int currentMusicIndex = -1;
    
    private PlaybackUIController uiController;
    private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

    @Override
    public MusicService getService() { return musicService; }
    @Override
    public boolean isBound() { return isBound; }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        musicFiles = new ArrayList<MusicFile>();
        fileLogger = FileLogger.getInstance(this);
        configManager = new ConfigManager(this);

        uiController = new PlaybackUIController(this, this);
        initViews();
        setupListView();
        checkPermissions();
        bindMusicService();
        
        if (configManager.isAutoScan()) {
            mainHandler.postDelayed(() -> {
                scanDirectory();
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            musicService.setListener(null);
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void initViews() {
        lvMusicFiles = findViewById(R.id.lvMusicFiles);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            scanDirectory();
        });
    }

    private void setupListView() {
        adapter = new MusicFileAdapter(this, musicFiles);
        lvMusicFiles.setAdapter(adapter);
        
        lvMusicFiles.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                scrollState = state;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        lvMusicFiles.setOnItemClickListener((parent, view, position, id) -> {
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                loadMusic(musicFiles.get(position));
            }
        });
    }

    private void checkPermissions() {
        if (!PermissionHelper.hasAllNecessaryPermissions(this)) {
            PermissionHelper.request(this);
        }
    }
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicBinder) service).getService();
            musicService.setListener(MainActivity.this);
            isBound = true;
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    
    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void updateUIFromService() {
        if (!isBound || musicService == null) return;
        currentMusic = musicService.getCurrentMusic();
        if (currentMusic != null) {
            adapter.setPlayingPath(currentMusic.getPath());
        }
        uiController.updateUI(currentMusic, musicService.isPlaying());
    }

    private void scanDirectory() {
        String dirPath = configManager.getMusicDir();
        if (dirPath.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        ScanResultHandler handler = new ScanResultHandler(this, mainHandler, musicFiles, adapter, swipeRefreshLayout);
        MusicScanner.scanDirectoryAsync(this, dirPath, handler);
    }

    public void updatePlaylist() {
        if (isBound) musicService.setPlaylist(musicFiles);
    }

    public void loadMusic(MusicFile musicFile) {
        if (!isBound) return;
        try {
            currentMusicIndex = musicFiles.indexOf(musicFile);
            musicService.loadAndPlay(musicFile);
            currentMusic = musicFile;
            adapter.setPlayingPath(musicFile.getPath());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load music", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configManager.loadConfig();
        if (isBound) updateUIFromService();
    }
    
    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PermissionHelper.REQ) {
            for (int res : results) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    public void onMusicChanged(MusicFile musicFile, int index) {
        mainHandler.post(() -> {
            currentMusic = musicFile;
            currentMusicIndex = index;
            adapter.setPlayingPath(musicFile.getPath());
            uiController.updateUI(musicFile, true);
        });
    }
    
    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        mainHandler.post(() -> uiController.updatePlayState(isPlaying));
    }
    
    @Override
    public void onMusicFinished() {
        mainHandler.post(() -> uiController.onMusicFinished());
    }
}
