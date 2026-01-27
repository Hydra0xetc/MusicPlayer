package com.example.musicplayer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.view.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private EditText etDirPath;
    private ListView lvMusicFiles;
    private TextView tvStatus, tvSongTitle;
    private Button btnPlayPause, btnStop, btnScan, btnToggleLog;
    private Button btnLoop;
    private boolean isLoopEnabled = false;

    private PlayerController player;
    private LogHelper logger;
    private Handler mainHandler;
    private Handler autoNextHandler;

    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private MusicFile currentMusic;
    private int currentMusicIndex = -1;

    public void onCreate(Bundle b) {
        super.onCreate(b);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // Hilangkan title

        setContentView(R.layout.main);
    
        player = new PlayerController();
        mainHandler = new Handler(Looper.getMainLooper());
        autoNextHandler = new Handler(Looper.getMainLooper());
        musicFiles = new ArrayList<MusicFile>();

        initViews();
        setupLogger();
        setupListView();
        setupButtons();
        
        checkPermissions();
        etDirPath.setText("/sdcard/Download/YouTubeDownload/music1/");
        
        // Start monitoring untuk auto-next
        startAutoNextMonitoring();
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
    }

    public void loadMusic(MusicFile musicFile) {
        try {
            // Cari index lagu yang dipilih
            currentMusicIndex = musicFiles.indexOf(musicFile);
            
            logger.log("Loading: " + musicFile.getName());
            logger.log("Path: " + musicFile.getPath());
            logger.log("Size: " + musicFile.getSizeFormatted());
            logger.log("Index: " + (currentMusicIndex + 1) + "/" + musicFiles.size());
            
            player.load(musicFile.getPath());
            currentMusic = musicFile;
            
            tvSongTitle.setText(musicFile.getName());
            tvStatus.setText("Ready");
            
            enableControls(true);
            
            if (!player.isPlaying()) {
                tvStatus.setText("▶ Playing");
                btnPlayPause.setText("▶ PLAY");
                btnPlayPause.setBackgroundColor(0xFF03DAC6); // Cyan
            }
            player.play();

        } catch (Exception e) {
            logger.log("ERROR: Failed to load - " + e.getMessage());
            toast("Failed to load music");
        }
    }

    private void playPause() {
        if (!player.isReady()) {
            logger.log("ERROR: No music loaded");
            return;
        }
        
        if (player.isPlaying()) {
            player.pause();
            tvStatus.setText("Paused");
            btnPlayPause.setText("❚❚ PAUSE");
            btnPlayPause.setBackgroundColor(0xFFFF9800); // Orange
            logger.log("Paused");
        } else {
            player.play();
            tvStatus.setText("▶ Playing");
            btnPlayPause.setText("▶ PLAY");
            btnPlayPause.setBackgroundColor(0xFF03DAC6); // Cyan
            String name = currentMusic != null ? currentMusic.getName() : "";
            logger.log("▶ Playing: " + name);
        }
    }

    private void stop() {
        if (!player.isReady()) return;
        
        player.stop();
        tvStatus.setText("Stopped");
        btnPlayPause.setText("❚❚ PAUSE");
        btnPlayPause.setBackgroundColor(0xFFFF9800); // Reset ke Orange
    }
    
    private void enableControls(boolean enable) {
        btnPlayPause.setEnabled(enable);
        btnStop.setEnabled(enable);
        btnLoop.setEnabled(enable);
        
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

    protected void onDestroy() {
        super.onDestroy();
        logger.log("Releasing player and closing app...");
        autoNextHandler.removeCallbacksAndMessages(null);
        player.release();
    }
    
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ) {
            boolean granted = grantResults.length > 0 && 
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            
            if (granted) {
                logger.log("Permission GRANTED by user");
            } else {
                logger.log("Permission DENIED by user");
            }
        }
    }

    // Simple inner classes
    private class ListItemClick implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            loadMusic(musicFiles.get(position));
        }
    }

    private void toggleLoop() {
        isLoopEnabled = !isLoopEnabled;
        updateLoopButton();
        
        if (player.isReady()) {
            player.setLoop(isLoopEnabled);
            logger.log("Loop: " + (isLoopEnabled ? "ON":"OFF"));
        }
    }

    private void updateLoopButton() {
        if (isLoopEnabled) {
            btnLoop.setText("↻ Loop");
            btnLoop.setBackgroundColor(0xFFBB86FC); // Purple ketika aktif
            btnLoop.setTextColor(0xFF121212); // Dark text
        } else {
            btnLoop.setText("Loop");
            btnLoop.setBackgroundColor(0xFF333333); // Dark grey ketika non-aktif
            btnLoop.setTextColor(0xFFFFFFFF); // White text
        }
    }
    
    private void startAutoNextMonitoring() {
        autoNextHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndPlayNext();
                // Cek setiap 500ms
                autoNextHandler.postDelayed(this, 500);
            }
        }, 500);
    }
    
    private void checkAndPlayNext() {
        if (!player.isReady()) {
            return;
        }
        
        // Cek apakah lagu sudah selesai
        if (player.isFinished() && !player.isPlaying()) {
            playNextSong();
        }
    }
    
    private void playNextSong() {
        if (musicFiles.isEmpty()) {
            logger.log("AutoNext: No songs in playlist");
            return;
        }
        
        // Jika loop aktif, jangan auto-next
        if (isLoopEnabled) {
            return;
        }
        
        int nextIndex = currentMusicIndex + 1;
        
        // Jika sudah di akhir playlist, kembali ke awal
        if (nextIndex >= musicFiles.size()) {
            nextIndex = 0;
            logger.log("AutoNext: Reached end, restarting from first song");
        }
        
        currentMusicIndex = nextIndex;
        MusicFile nextSong = musicFiles.get(nextIndex);
        
        logger.log("AutoNext: Playing next song (" + (nextIndex + 1) + "/" + musicFiles.size() + ")");
        loadMusic(nextSong);
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
            }
        }
    }

}
