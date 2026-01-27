package com.example.musicplayer;

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
    private Button btnPlayPause, btnStop, btnScan, btnToggleLog;
    private Button btnLoop, btnPrev, btnNext;
    private boolean isLoopEnabled = false;

    private MusicService musicService;
    private boolean isBound = false;
    
    private LogHelper logger;
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
        etDirPath = findViewById(R.id.etDirPath);
        lvMusicFiles = findViewById(R.id.lvMusicFiles);
        tvStatus = findViewById(R.id.tvStatus);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        btnLoop = findViewById(R.id.btnLoop);
        
        btnScan = findViewById(R.id.btnScan);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        btnToggleLog = findViewById(R.id.btnToggleLog);
        
        // Tambahan tombol prev/next (jika ada di layout)
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupLogger() {
        LinearLayout logSection = findViewById(R.id.logSection);
        ScrollView logContainer = findViewById(R.id.logContainer);
        TextView tvLog = findViewById(R.id.tvLog);
        
        File logFile = new File(getExternalFilesDir(null), "log.txt");
        logger = new LogHelper(tvLog, logContainer, logSection, logFile);
        logger.log("Log file: " + logger.getLogFilePath());
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
        btnToggleLog.setOnClickListener(listener);
        
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
        } else {
            logger.log("Audio permission granted");
        }
    }
    
    // Service binding
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(MainActivity.this);
            isBound = true;
            
            logger.log("Connected to MusicService");
            
            // Update UI jika service sudah memiliki musik yang sedang diputar
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            logger.log("Disconnected from MusicService");
        }
    };
    
    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent); // Start service terlebih dahulu
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void updateUIFromService() {
        if (!isBound || musicService == null) return;
        
        MusicFile current = musicService.getCurrentMusic();
        if (current != null) {
            currentMusic = current;
            currentMusicIndex = musicService.getCurrentIndex();
            tvSongTitle.setText(current.getName());
            
            if (musicService.isPlaying()) {
                tvStatus.setText("▶ Playing");
                btnPlayPause.setText("▶ PLAY");
                btnPlayPause.setBackgroundColor(0xFF03DAC6);
            } else {
                tvStatus.setText("❚❚ Paused");
                btnPlayPause.setText("❚❚ PAUSE");
                btnPlayPause.setBackgroundColor(0xFFFF9800);
            }
            
            enableControls(true);
        }
        
        isLoopEnabled = musicService.isLoopEnabled();
        updateLoopButton();
    }

    private void scanDirectory() {
        String dirPath = etDirPath.getText().toString().trim();
        
        if (dirPath.isEmpty()) {
            toast("Please enter directory path");
            logger.log("ERROR: Empty directory path");
            return;
        }
        
        File dir = new File(dirPath);
        if (!dir.exists()) {
            toast("Directory not found");
            logger.log("ERROR: Directory not found: " + dirPath);
            return;
        }
        
        if (!dir.isDirectory()) {
            toast("Path is not a directory");
            logger.log("ERROR: Not a directory: " + dirPath);
            return;
        }

        btnScan.setEnabled(false);
        btnScan.setText("Scanning...");
        logger.log("Scanning directory: " + dirPath);

        ScanResultHandler handler = new ScanResultHandler(
            this, mainHandler,
            musicFiles, adapter,
            btnScan, logger
        );
        MusicScanner.scanDirectoryAsync(dirPath, handler);
        
        // Update playlist di service setelah scan
        if (isBound && musicService != null) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    musicService.setPlaylist(musicFiles);
                    logger.log("Playlist updated in service: " + musicFiles.size() + " songs");
                }
            }, 1000);
        }
    }

    public void loadMusic(MusicFile musicFile) {
        if (!isBound || musicService == null) {
            toast("Service not ready");
            return;
        }
        
        try {
            currentMusicIndex = musicFiles.indexOf(musicFile);
            
            logger.log("Loading: " + musicFile.getName());
            logger.log("Path: " + musicFile.getPath());
            logger.log("Size: " + musicFile.getSizeFormatted());
            logger.log("Index: " + (currentMusicIndex + 1) + "/" + musicFiles.size());
            
            musicService.loadAndPlay(musicFile);
            currentMusic = musicFile;

        } catch (Exception e) {
            logger.log("ERROR: Failed to load - " + e.getMessage());
            toast("Failed to load music");
        }
    }

    private void playPause() {
        if (!isBound || musicService == null || !musicService.isReady()) {
            logger.log("ERROR: No music loaded");
            return;
        }
        
        musicService.togglePlayPause();
    }

    private void stop() {
        if (!isBound || musicService == null || !musicService.isReady()) return;
        
        musicService.stop();
        logger.log("Stopped");
    }
    
    private void playNext() {
        if (!isBound || musicService == null) return;
        
        musicService.playNext();
        logger.log("Playing next song");
    }
    
    private void playPrevious() {
        if (!isBound || musicService == null) return;
        
        musicService.playPrevious();
        logger.log("Playing previous song");
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
        
        // Unbind dari service, tapi service tetap jalan
        if (isBound) {
            musicService.setListener(null);
            unbindService(serviceConnection);
            isBound = false;
        }
        
        logger.log("Activity destroyed, but service continues running");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Update UI ketika kembali ke activity
        if (isBound && musicService != null) {
            updateUIFromService();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ) {
            boolean granted = grantResults.length > 0 && 
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            
            if (granted) {
                logger.log("Permission GRANTED");
            } else {
                logger.log("Permission DENIED");
            }
        }
    }

    // Callbacks dari MusicService
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
                    btnPlayPause.setText("▶ PLAY");
                    btnPlayPause.setBackgroundColor(0xFF03DAC6);
                } else {
                    tvStatus.setText("❚❚ Paused");
                    btnPlayPause.setText("❚❚ PAUSE");
                    btnPlayPause.setBackgroundColor(0xFFFF9800);
                }
            }
        });
    }
    
    @Override
    public void onMusicFinished() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                logger.log("Music finished, auto-playing next...");
            }
        });
    }

    // Simple inner classes
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
    }

    private void updateLoopButton() {
        if (isLoopEnabled) {
            btnLoop.setText("↻ Loop");
            btnLoop.setBackgroundColor(0xFFBB86FC);
            btnLoop.setTextColor(0xFF121212);
        } else {
            btnLoop.setText("Loop");
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
            } else if (id == R.id.btnToggleLog) {
                logger.toggle();
                btnToggleLog.setText(logger.isVisible() ? "Hide Log" : "Show Log");
            } else if (id == R.id.btnPrev) {
                playPrevious();
            } else if (id == R.id.btnNext) {
                playNext();
            }
        }
    }
}
