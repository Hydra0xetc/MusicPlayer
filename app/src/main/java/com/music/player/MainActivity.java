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
    private TextView tvStatus, tvSongTitle, tvCurrentTime, tvTotalTime;
    private ImageView ivMainAlbumArt;
    private ImageButton btnSettings;
    private ImageButton btnPlayPause, btnStop, btnPrev, btnNext;
    private Button btnScan;
    private SeekBar seekBar;
    private boolean isLoopEnabled = false;

    private MusicService musicService;
    private boolean isBound = false;
    
    private FileLogger fileLogger;
    private Handler mainHandler;
    private final Handler seekbarUpdateHandler = new Handler(Looper.getMainLooper());
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
        setupSeekBar();
        
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
        seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
    }

    private void initViews() {
        fileLogger.d(TAG, "Initializing views...");
        
        lvMusicFiles = findViewById(R.id.lvMusicFiles);
        tvStatus = findViewById(R.id.tvStatus);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        ivMainAlbumArt = findViewById(R.id.ivMainAlbumArt);
        btnSettings = findViewById(R.id.btnSettings);
        
        btnScan = findViewById(R.id.btnScan);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        
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
        
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    // Update current time display immediately
                    tvCurrentTime.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Pause updates from service while user is seeking
                seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && musicService != null) {
                    musicService.seekTo(seekBar.getProgress());
                }
                seekbarUpdateHandler.post(updateSeekBarRunnable);
            }
        });
    }

    private String formatDuration(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
            long currentPos = musicService.getCurrentPosition();
            seekBar.setProgress((int)currentPos);
            tvCurrentTime.setText(formatDuration(currentPos));
            onPlayStateChanged(musicService.isPlaying());
            enableControls(true);
        } else {
            onPlayStateChanged(false);
            enableControls(false);
        }
        
        isLoopEnabled = musicService.isLoopEnabled();
    }

    private void scanDirectory() {
        String dirPath = configManager.getMusicDir();
        
        if (dirPath.isEmpty()) {
            Toast.makeText(this, "Config not found! Please check config.json", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File dir = new File(dirPath);
        if (!dir.exists()) {
            Toast.makeText(this, "Directory not found: " + dirPath, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!dir.isDirectory()) {
            Toast.makeText(this, "Path is not a directory", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Service not ready", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Failed to load music", Toast.LENGTH_SHORT).show();
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
        
        if (btnPrev != null) btnPrev.setEnabled(enable);
        if (btnNext != null) btnNext.setEnabled(enable);
        
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
                Toast.makeText(this, "Please grant all necessary permissions for the app to function properly.", Toast.LENGTH_SHORT).show();
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
                tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Clear icons
                enableControls(true);
                fileLogger.i(TAG, "Now playing: " + musicFile.getName());

                byte[] albumArt = musicFile.getAlbumArt();
                if (albumArt != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                    ivMainAlbumArt.setImageBitmap(bitmap);
                } else {
                    ivMainAlbumArt.setImageResource(R.mipmap.ic_launcher);
                }

                // Initialize SeekBar and total time
                long duration = musicFile.getDuration();
                seekBar.setMax((int) duration);
                tvTotalTime.setText(formatDuration(duration));
                if (isBound && musicService != null && musicService.isReady()) {
                    long currentPos = musicService.getCurrentPosition();
                    tvCurrentTime.setText(formatDuration(currentPos));
                    seekBar.setProgress((int)currentPos);
                } else {
                    tvCurrentTime.setText(formatDuration(0));
                    seekBar.setProgress(0);
                }
            }
        });
    }
    
    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        mainHandler.post(new Runnable() {

            @Override
            public void run() {
                int color;
                if (isPlaying) {
                    tvStatus.setText("Playing");
                    tvStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_small, 0, 0, 0);
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    color = 0xFFFF9800; // Orange
                    seekbarUpdateHandler.post(updateSeekBarRunnable);
                } else {
                    tvStatus.setText("Paused");
                    tvStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_small, 0, 0, 0);
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    color = 0xFF03DAC6; // Teal
                    seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
                }

                android.graphics.drawable.Drawable background = btnPlayPause.getBackground();
                if (background instanceof android.graphics.drawable.GradientDrawable) {
                    // Cast and mutate the drawable to change its color
                    ((android.graphics.drawable.GradientDrawable) background.mutate()).setColor(color);
                } else {
                    // If not a shape, fallback to the old way
                    btnPlayPause.setBackgroundColor(color);
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
                seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
                seekBar.setProgress(0);
                tvCurrentTime.setText(formatDuration(0));
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
        
        fileLogger.i(TAG, "Loop: " + (isLoopEnabled ? "ON":"OFF"));
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

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null && musicService.isPlaying()) {
                long currentPosition = musicService.getCurrentPosition();
                seekBar.setProgress((int) currentPosition);
                tvCurrentTime.setText(formatDuration(currentPosition));
                seekbarUpdateHandler.postDelayed(this, 1000); // Update every second
            }
        }
    };
}
