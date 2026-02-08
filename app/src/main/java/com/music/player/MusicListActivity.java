package com.music.player;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends Activity {
    private static final String TAG = "MusicListActivity";
    
    private ListView lvAllMusic;
    private TextView tvLibraryTitle, tvSongCount;
    private Button btnScanLibrary, btnSettings;
    private ProgressBar progressBar;
    private View emptyView;
    
    private FileLogger fileLogger;
    private ConfigManager configManager;
    private Handler mainHandler;
    
    private List<MusicFile> allMusicFiles;
    private MusicFileAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_music_list);
        
        mainHandler = new Handler(Looper.getMainLooper());
        allMusicFiles = new ArrayList<>();
        
        fileLogger = FileLogger.getInstance(this);
        fileLogger.i(TAG, "MusicListActivity.onCreate() started");
        
        configManager = new ConfigManager(this);
        
        initViews();
        setupListView();
        setupButtons();
        checkPermissions();
        
        // Auto-scan if enabled
        if (configManager.isAutoScan()) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fileLogger.i(TAG, "Auto-scan enabled, scanning music library...");
                    scanMusicLibrary();
                }
            }, 500);
        }
    }
    
    private void initViews() {
        lvAllMusic = findViewById(R.id.lvAllMusic);
        tvLibraryTitle = findViewById(R.id.tvLibraryTitle);
        tvSongCount = findViewById(R.id.tvSongCount);
        btnScanLibrary = findViewById(R.id.btnScanLibrary);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        
        updateSongCount();
    }
    
    private void setupListView() {
        adapter = new MusicFileAdapter(this, allMusicFiles);
        lvAllMusic.setAdapter(adapter);
        lvAllMusic.setEmptyView(emptyView);
        
        lvAllMusic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicFile selectedMusic = allMusicFiles.get(position);
                openPlayerActivity(selectedMusic, position);
            }
        });
    }
    
    private void setupButtons() {
        btnScanLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanMusicLibrary();
            }
        });
        
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicListActivity.this, SettingsActivity.class);
                startActivity(intent);
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
    
    private void scanMusicLibrary() {
        if (!PermissionHelper.hasAllNecessaryPermissions(this)) {
            Toast.makeText(this, "Please grant storage permissions first", Toast.LENGTH_SHORT).show();
            PermissionHelper.request(this);
            return;
        }
        
        showLoading(true);
        fileLogger.i(TAG, "Starting music library scan...");
        
        MusicScanner scanner = new MusicScanner(this);
        scanner.scanAsync(new ScanResultHandler() {
            @Override
            public void onScanComplete(final List<MusicFile> files) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        allMusicFiles.clear();
                        allMusicFiles.addAll(files);
                        adapter.notifyDataSetChanged();
                        updateSongCount();
                        showLoading(false);
                        
                        fileLogger.i(TAG, "Music library scan complete. Found " + files.size() + " songs");
                        Toast.makeText(MusicListActivity.this, 
                            "Found " + files.size() + " songs", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onScanError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLoading(false);
                        fileLogger.e(TAG, "Scan error: " + error);
                        Toast.makeText(MusicListActivity.this, 
                            "Scan error: " + error, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void openPlayerActivity(MusicFile music, int position) {
        fileLogger.i(TAG, "Opening player for: " + music.getName());
        
        Intent intent = new Intent(this, MainActivity.class);
        
        // Pass the selected music and entire playlist
        intent.putExtra("SELECTED_MUSIC_PATH", music.getPath());
        intent.putExtra("SELECTED_MUSIC_INDEX", position);
        
        // Pass the entire music list as paths
        ArrayList<String> musicPaths = new ArrayList<>();
        for (MusicFile file : allMusicFiles) {
            musicPaths.add(file.getPath());
        }
        intent.putStringArrayListExtra("PLAYLIST_PATHS", musicPaths);
        
        startActivity(intent);
    }
    
    private void updateSongCount() {
        int count = allMusicFiles.size();
        if (count == 0) {
            tvSongCount.setText("No songs in library");
        } else if (count == 1) {
            tvSongCount.setText("1 song");
        } else {
            tvSongCount.setText(count + " songs");
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnScanLibrary.setEnabled(!show);
        lvAllMusic.setEnabled(!show);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        fileLogger.d(TAG, "onResume() called");
        
        configManager.loadConfig();
        
        // Refresh if auto-scan enabled
        if (configManager.isAutoScan() && allMusicFiles.isEmpty()) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanMusicLibrary();
                }
            }, 300);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionHelper.REQ) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    fileLogger.i(TAG, "Permission GRANTED: " + permissions[i]);
                } else {
                    fileLogger.e(TAG, "Permission DENIED: " + permissions[i]);
                    allGranted = false;
                }
            }
            
            if (allGranted) {
                // Auto-scan after permissions granted
                scanMusicLibrary();
            } else {
                Toast.makeText(this, 
                    "Storage permissions are required to access your music", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
