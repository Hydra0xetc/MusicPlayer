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
import android.view.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements MusicService.MusicServiceListener {
    final static String TAG = "MainActivity";
    private ListView lvMusicFiles;
    private TextView tvStatus, tvSongTitle;
    private ImageView ivMainAlbumArt;
    private ImageButton btnSettings;
    private Button btnPlayPause, btnStop, btnScan;
    private Button btnLoop, btnPrev, btnNext;
    private boolean isLoopEnabled = false;

    private MusicService musicService;
    private boolean isBound = false;
    
    private FileLogger fileLogger;
    private Handler mainHandler;
    private ConfigManager configManager;

    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private MusicFile currentMusic;
    private int currentMusicIndex = -1;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        mainHandler = new Handler(Looper.getMainLooper());
        musicFiles = new ArrayList<MusicFile>();

        fileLogger = FileLogger.getInstance(this);
        fileLogger.i(TAG, "MainActivity.onCreate() started");

        configManager = new ConfigManager(this);

        initViews();
        setupListView();
        setupButtons();
        
        checkPermissions();
        
        updateUIBasedOnConfig();
        
        bindMusicService();
        
        if (configManager.isAutoScan()) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fileLogger.i(TAG, "Auto-scan enabled, scanning...");
                    scanDirectory();
                }
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        fileLogger.i(TAG, "MainActivity.onDestroy() called");
        
        if (isBound) {
            musicService.setListener(null);
            unbindService(serviceConnection);
            isBound = false;
        }
        
        configManager.stopWatching();
        fileLogger.i(TAG, "Activity destroyed");
    }

    private void initViews() {
        fileLogger.d(TAG, "Initializing views...");
        
        lvMusicFiles = findViewById(R.id.lvMusicFiles);
        tvStatus = findViewById(R.id.tvStatus);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        ivMainAlbumArt = findViewById(R.id.ivMainAlbumArt);
        btnSettings = findViewById(R.id.btnSettings);
        btnLoop = findViewById(R.id.btnLoop);
        
        btnScan = findViewById(R.id.btnScan);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        
        fileLogger.d(TAG, "Views initialized");
    }

    private void setupListView() {
        adapter = new MusicFileAdapter(this, musicFiles);
        lvMusicFiles.setAdapter(adapter);
        lvMusicFiles.setOnItemClickListener(new ListItemClick());
    }

    private void setupButtons() {
        ButtonClick listener = new ButtonClick();
        btnScan.setOnClickListener(listener);
        btnPlayPause.setOnClickListener(listener);
        btnStop.setOnClickListener(listener);

        if (btnSettings != null) {
            btnSettings.setOnClickListener(listener);
        }
        
        if (btnPrev != null) {
            btnPrev.setOnClickListener(listener);
        }
        
        if (btnNext != null) {
            btnNext.setOnClickListener(listener);
        }
        
        btnLoop.setOnClickListener(new View.OnClickListener() {
           @Override 
           public void onClick(View v) {
               toggleLoop();
           }
        });
    }

    private void checkPermissions() {
        if (!PermissionHelper.hasAllNecessaryPermissions(this)) {
            fileLogger.w(TAG, "Not all necessary permissions granted. Requesting...");
            PermissionHelper.request(this);
        } else {
            fileLogger.i(TAG, "All necessary permissions already granted");
        }
    }
    
    private void updateUIBasedOnConfig() {
        if (configManager.isAutoScan()) {
            btnScan.setVisibility(View.GONE);
            fileLogger.i(TAG, "Auto-scan enabled, hiding scan button");
        } else {
            btnScan.setVisibility(View.VISIBLE);
        }
    }
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(MainActivity.this);
            isBound = true;
            
            fileLogger.i(TAG, "Connected to MusicService");
            
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            fileLogger.w(TAG, "Disconnected from MusicService");
        }
    };
    
    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        fileLogger.i(TAG, "Binding to MusicService...");
    }
    
    private void updateUIFromService() {
        if (!isBound || musicService == null) return;
        
        MusicFile current = musicService.getCurrentMusic();
        if (current != null) {
            currentMusic = current;
            currentMusicIndex = musicService.getCurrentIndex();
            tvSongTitle.setText(current.getName());
            onMusicChanged(current, currentMusicIndex);
        } else {
            String no_song = "No song playing";
            tvSongTitle.setText(no_song);
            fileLogger.d(TAG, no_song);
            currentMusic = null;
            currentMusicIndex = -1;
        }

        if (musicService.isReady()) {
            onPlayStateChanged(musicService.isPlaying());
            enableControls(true);
        } else {
            onPlayStateChanged(false);
            enableControls(false);
        }
        
        isLoopEnabled = musicService.isLoopEnabled();
        updateLoopButton();
    }

    private void scanDirectory() {
        String dirPath = configManager.getMusicDir();
        
        if (dirPath.isEmpty()) {
            toast("Config not found! Please check config.json");
            return;
        }
        
        File dir = new File(dirPath);
        if (!dir.exists()) {
            toast("Directory not found: " + dirPath);
            return;
        }
        
        if (!dir.isDirectory()) {
            toast("Path is not a directory");
            return;
        }

        btnScan.setEnabled(false);
        btnScan.setText("Scanning...");
        fileLogger.i(TAG, "Scanning directory: " + dirPath);

        ScanResultHandler handler = new ScanResultHandler(
            this, mainHandler,
            musicFiles, adapter,
            btnScan
        );
        MusicScanner.scanDirectoryAsync(this, dirPath, handler);
        
    }

    public void updatePlaylist() {
        if (isBound && musicService != null) {
            musicService.setPlaylist(musicFiles);
            fileLogger.i(TAG, "Playlist updated: " + musicFiles.size() + " songs");
        }
    }

    public void loadMusic(MusicFile musicFile) {
        if (!isBound || musicService == null) {
            toast("Service not ready");
            fileLogger.e(TAG, "Service not ready");
            return;
        }
        
        try {
            currentMusicIndex = musicFiles.indexOf(musicFile);
            
            fileLogger.i(TAG, "Path: " + musicFile.getPath());
            fileLogger.i(TAG, "Loading: " + musicFile.getName());
            
            musicService.loadAndPlay(musicFile);
            currentMusic = musicFile;

        } catch (Exception e) {
            fileLogger.i(TAG, "Failed to load music: " + e);
            toast("Failed to load music");
        }
    }

    private void playPause() {
        if (!isBound || musicService == null || !musicService.isReady()) {
            fileLogger.i(TAG, "No music loaded");
            return;
        }
        
        musicService.togglePlayPause();
    }

    private void stop() {
        if (!isBound || musicService == null || !musicService.isReady()) return;
        
        musicService.stop();
        fileLogger.i(TAG, "Music stopped");
    }
    
    private void playNext() {
        if (!isBound || musicService == null) return;
        
        musicService.playNext();
        fileLogger.i(TAG, "Playing next song");
    }
    
    private void playPrevious() {
        if (!isBound || musicService == null) return;
        
        musicService.playPrevious();
        fileLogger.i(TAG, "Playing previous song");
    }
    
    private void enableControls(boolean enable) {
        btnPlayPause.setEnabled(enable);
        btnStop.setEnabled(enable);
        btnLoop.setEnabled(enable);
        
        if (btnPrev != null) btnPrev.setEnabled(enable);
        if (btnNext != null) btnNext.setEnabled(enable);
        
        if (!enable) {
            btnLoop.setBackgroundColor(0xFF1A1A1A);
            btnLoop.setTextColor(0xFF666666);
        } else {
            updateLoopButton();
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        fileLogger.d(TAG, "onResume() called");
        
        configManager.loadConfig();
        updateUIBasedOnConfig();
        
        // Check for auto-scan here, similar to onCreate
        if (configManager.isAutoScan()) {
            fileLogger.i(TAG, "Auto-scan enabled, scanning on resume...");
            // Add a slight delay to ensure UI is ready or to prevent race conditions
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanDirectory();
                }
            }, 500); // 500ms delay
        }
        
        if (isBound && musicService != null) {
            updateUIFromService();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PermissionHelper.REQ) {
            boolean allGranted = true;
            for (int i = 0; i < perms.length; i++) {
                if (results[i] == PackageManager.PERMISSION_GRANTED) {
                    fileLogger.i(TAG, "Permission GRANTED: " + perms[i]);
                } else {
                    fileLogger.e(TAG, "Permission DENIED: " + perms[i]);
                    allGranted = false;
                }
            }
            if (!allGranted) {
                toast("Please grant all necessary permissions for the app to function properly.");
            }
        }
    }

    @Override
    public void onMusicChanged(MusicFile musicFile, int index) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                currentMusic = musicFile;
                currentMusicIndex = index;
                tvSongTitle.setText(musicFile.getTitle()); // Display title, artist
                tvStatus.setText("Ready");
                enableControls(true);
                fileLogger.i(TAG, "Now playing: " + musicFile.getName());

                byte[] albumArt = musicFile.getAlbumArt();
                if (albumArt != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                    ivMainAlbumArt.setImageBitmap(bitmap);
                } else {
                    ivMainAlbumArt.setImageResource(R.mipmap.ic_launcher);
                }
            }
        });
    }
    
    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    tvStatus.setText("▶ Playing");
                    btnPlayPause.setText("❚❚ PAUSE");
                    btnPlayPause.setBackgroundColor(0xFFFF9800);
                } else {
                    tvStatus.setText("❚❚ Paused");
                    btnPlayPause.setText("▶ PLAY");
                    btnPlayPause.setBackgroundColor(0xFF03DAC6);
                }
            }
        });
    }
    
    @Override
    public void onMusicFinished() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                fileLogger.i(TAG, "Music finished");
            }
        });
    }

    private class ListItemClick implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            loadMusic(musicFiles.get(position));
        }
    }

    private void toggleLoop() {
        if (!isBound || musicService == null) return;
        
        isLoopEnabled = !isLoopEnabled;
        musicService.setLoop(isLoopEnabled);
        updateLoopButton();
        
        fileLogger.i(TAG, "Loop: " + (isLoopEnabled ? "ON":"OFF"));
    }

    private void updateLoopButton() {
        if (isLoopEnabled) {
            btnLoop.setBackgroundColor(0xFFBB86FC);
            btnLoop.setTextColor(0xFF121212);
        } else {
            btnLoop.setBackgroundColor(0xFF333333);
            btnLoop.setTextColor(0xFFFFFFFF);
        }
    }

    private class ButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            int id = v.getId();
            
            if (id == R.id.btnScan) {
                scanDirectory();
            } else if (id == R.id.btnPlayPause) {
                playPause();
            } else if (id == R.id.btnStop) {
                stop();
            } else if (id == R.id.btnPrev) {
                playPrevious();
            } else if (id == R.id.btnNext) {
                playNext();
            } else if (id == R.id.btnSettings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }
    }
}
