package com.music.player;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
    private EditText etDirPath;
    private ListView lvMusicFiles;
    private TextView tvStatus, tvSongTitle;
    private Button btnPlayPause, btnStop, btnScan;
    private Button btnLoop, btnPrev, btnNext;
    private boolean isLoopEnabled = false;

    private MusicService musicService;
    private boolean isBound = false;
    
    private LogHelper logger;
    private FileLogger fileLogger;
    private Handler mainHandler;

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

        // Inisialisasi FileLogger
        fileLogger = FileLogger.getInstance(this);
        fileLogger.i("MainActivity", "MainActivity.onCreate() started");

        initViews();
        setupLogger();
        setupListView();
        setupButtons();
        
        checkPermissions();
        etDirPath.setText("/sdcard/Download/YouTubeDownload/music1/");
        
        // Bind ke service
        bindMusicService();
    }

    private void initViews() {
        fileLogger.d("MainActivity", "Initializing views...");
        
        etDirPath = findViewById(R.id.etDirPath);
        lvMusicFiles = findViewById(R.id.lvMusicFiles);
        tvStatus = findViewById(R.id.tvStatus);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        btnLoop = findViewById(R.id.btnLoop);
        
        btnScan = findViewById(R.id.btnScan);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        
        fileLogger.d("MainActivity", "Views initialized");
    }

    private void setupLogger() {
        LinearLayout logSection = findViewById(R.id.logSection);
        ScrollView logContainer = findViewById(R.id.logContainer);
        TextView tvLog = findViewById(R.id.tvLog);
        
        logger = new LogHelper(this, tvLog, logContainer, logSection);
        logger.log("Music Player started");
        logger.log("Log file: " + fileLogger.getLogPath());
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
        if (!PermissionHelper.hasAudioPermission(this)) {
            PermissionHelper.request(this);
            logger.log("Requesting audio permission...");
            fileLogger.w("MainActivity", "Requesting audio permission");
        } else {
            logger.log("Audio permission granted");
            fileLogger.i("MainActivity", "Audio permission already granted");
        }
    }
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(MainActivity.this);
            isBound = true;
            
            logger.log("Connected to MusicService");
            fileLogger.i("MainActivity", "Connected to MusicService");
            
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            logger.log("Disconnected from MusicService");
            fileLogger.w("MainActivity", "Disconnected from MusicService");
        }
    };
    
    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        fileLogger.i("MainActivity", "Binding to MusicService...");
    }
    
    private void updateUIFromService() {
        if (!isBound || musicService == null) return;
        
        MusicFile current = musicService.getCurrentMusic();
        if (current != null) {
            currentMusic = current;
            currentMusicIndex = musicService.getCurrentIndex();
            tvSongTitle.setText(current.getName());
            
            enableControls(true);
        }
        
        isLoopEnabled = musicService.isLoopEnabled();
        updateLoopButton();
    }

    private void scanDirectory() {
        String dirPath = etDirPath.getText().toString().trim();
        
        if (dirPath.isEmpty()) {
            toast("Please enter directory path");
            logger.logError("Empty directory path");
            return;
        }
        
        File dir = new File(dirPath);
        if (!dir.exists()) {
            toast("Directory not found");
            logger.logError("Directory not found: " + dirPath);
            return;
        }
        
        if (!dir.isDirectory()) {
            toast("Path is not a directory");
            logger.logError("Not a directory: " + dirPath);
            return;
        }

        btnScan.setEnabled(false);
        btnScan.setText("Scanning...");
        logger.log("Scanning directory: " + dirPath);
        fileLogger.i("MainActivity", "Scanning directory: " + dirPath);

        ScanResultHandler handler = new ScanResultHandler(
            this, mainHandler,
            musicFiles, adapter,
            btnScan, logger
        );
        MusicScanner.scanDirectoryAsync(dirPath, handler);
        
        if (isBound && musicService != null) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    musicService.setPlaylist(musicFiles);
                    logger.log("Playlist updated: " + musicFiles.size() + " songs");
                    fileLogger.i("MainActivity", "Playlist updated: " + musicFiles.size() + " songs");
                }
            }, 1000);
        }
    }

    public void loadMusic(MusicFile musicFile) {
        if (!isBound || musicService == null) {
            toast("Service not ready");
            logger.logError("Service not ready");
            return;
        }
        
        try {
            currentMusicIndex = musicFiles.indexOf(musicFile);
            
            logger.log("Loading: " + musicFile.getName());
            logger.log("Path: " + musicFile.getPath());
            
            fileLogger.i("MainActivity", "Loading: " + musicFile.getName());
            
            musicService.loadAndPlay(musicFile);
            currentMusic = musicFile;

        } catch (Exception e) {
            logger.logError("Failed to load music", e);
            toast("Failed to load music");
        }
    }

    private void playPause() {
        if (!isBound || musicService == null || !musicService.isReady()) {
            logger.logError("No music loaded");
            return;
        }
        
        musicService.togglePlayPause();
    }

    private void stop() {
        if (!isBound || musicService == null || !musicService.isReady()) return;
        
        musicService.stop();
        logger.log("Stopped");
        fileLogger.i("MainActivity", "Music stopped");
    }
    
    private void playNext() {
        if (!isBound || musicService == null) return;
        
        musicService.playNext();
        logger.log("Playing next song");
        fileLogger.i("MainActivity", "Playing next song");
    }
    
    private void playPrevious() {
        if (!isBound || musicService == null) return;
        
        musicService.playPrevious();
        logger.log("Playing previous song");
        fileLogger.i("MainActivity", "Playing previous song");
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
    protected void onDestroy() {
        super.onDestroy();
        
        fileLogger.i("MainActivity", "MainActivity.onDestroy() called");
        
        if (isBound) {
            musicService.setListener(null);
            unbindService(serviceConnection);
            isBound = false;
        }
        
        logger.log("Activity destroyed");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        fileLogger.d("MainActivity", "onResume() called");
        
        if (isBound && musicService != null) {
            updateUIFromService();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PermissionHelper.REQ) {
            boolean granted = results.length > 0 && 
                results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            
            if (granted) {
                logger.log("Permission GRANTED");
                fileLogger.i("MainActivity", "Permission GRANTED");
            } else {
                logger.logError("Permission DENIED");
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
                tvSongTitle.setText(musicFile.getName());
                tvStatus.setText("Ready");
                enableControls(true);
                logger.log("Now playing: " + musicFile.getName());
                fileLogger.i("MainActivity", "Now playing: " + musicFile.getName());
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
                logger.log("Music finished");
                fileLogger.i("MainActivity", "Music finished");
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
        
        logger.log("Loop: " + (isLoopEnabled ? "ON":"OFF"));
        fileLogger.i("MainActivity", "Loop: " + (isLoopEnabled ? "ON":"OFF"));
    }

    private void updateLoopButton() {
        if (isLoopEnabled) {
            btnLoop.setText("↻ Loop");
            btnLoop.setBackgroundColor(0xFFBB86FC);
            btnLoop.setTextColor(0xFF121212);
        } else {
            btnLoop.setText("↻ Loop");
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
            }
        }
    }
}
