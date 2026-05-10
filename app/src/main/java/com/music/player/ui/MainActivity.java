package com.music.player.ui;

import com.music.player.R;
import com.music.player.model.*;
import com.music.player.manager.*;
import com.music.player.service.*;
import com.music.player.player.*;
import com.music.player.utils.*;
import com.music.player.scanner.*;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.os.Bundle;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.text.style.ForegroundColorSpan;
import android.text.TextWatcher;
import android.text.Editable;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.text.SpannableStringBuilder;
import android.app.AlertDialog;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
        implements MusicService.MusicServiceListener, PlaybackUIController.MusicServiceWrapper {
    final static String TAG = "MainActivity";
    private ListView lvMusicFiles;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View llEmptySearch;

    private MusicService musicService;
    private boolean isBound = false;

    private FileLogger fileLogger;
    private Handler mainHandler;
    private ConfigManager configManager;

    private List<MusicFile> musicFiles;
    private MusicFileAdapter adapter;
    private MusicFile currentMusic;

    private PlaybackUIController uiController;

    @Override
    public MusicService getService() {
        return musicService;
    }

    @Override
    public boolean isBound() {
        return isBound;
    }

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
        llEmptySearch = findViewById(R.id.llEmptySearch);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            scanDirectory();
        });

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        EditText etSearch = findViewById(R.id.etSearch);

        btnSearch.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if (etSearch.getVisibility() == View.VISIBLE) {
                // Toggle OFF: Hide search bar
                etSearch.setVisibility(View.GONE);
                etSearch.setText(Constant.EMPTY_STRING);
                adapter.filter(Constant.EMPTY_STRING);
                checkEmptyState();
                if (imm != null)
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            } else {
                // Toggle ON: Show search bar
                // First, collapse player to show more list
                uiController.expandToTop();

                // Then show search bar and keyboard
                etSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
                if (imm != null) {
                    etSearch.postDelayed(() -> imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT), 300);
                }
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                checkEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void checkEmptyState() {
        if (llEmptySearch == null || adapter == null)
            return;
        if (adapter.getCount() == 0) {
            llEmptySearch.setVisibility(View.VISIBLE);
        } else {
            llEmptySearch.setVisibility(View.GONE);
        }
    }

    private void setupListView() {
        adapter = new MusicFileAdapter(this, musicFiles);
        lvMusicFiles.setAdapter(adapter);

        lvMusicFiles.setOnItemClickListener((parent, view, position, id) -> {
            loadMusic((MusicFile) adapter.getItem(position));
        });

        lvMusicFiles.setOnItemLongClickListener((parent, view, position, id) -> {
            showMusicInfoDialog((MusicFile) adapter.getItem(position));
            return true;
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
        if (!isBound || musicService == null)
            return;
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

        ScanResultHandler handler = new ScanResultHandler(this, mainHandler, musicFiles,
                adapter, swipeRefreshLayout);
        MusicScanner.scanDirectoryAsync(this, dirPath, handler);
    }

    public void updatePlaylist() {
        if (isBound) {
            musicService.setPlaylist(musicFiles);
        }
    }

    public void loadMusic(MusicFile musicFile) {
        if (!isBound) {
            return;
        }
        try {
            musicService.loadAndPlay(musicFile);
            currentMusic = musicFile;
            adapter.setPlayingPath(musicFile.getPath());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load music", Toast.LENGTH_SHORT).show();
            fileLogger.e(TAG, "Failed to load music: " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configManager.loadConfig();
        if (isBound) {
            updateUIFromService();
        }
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

    private void showMusicInfoDialog(MusicFile music) {
        new MusicInfoDialog(this).show(music);
    }
}
