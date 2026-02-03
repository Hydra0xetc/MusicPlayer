package com.music.player;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private EditText etMusicDir;
    private Switch swAutoReload;
    private Switch swAutoScan;
    private Button btnSaveSettings;
    private Button btnBack;

    private ConfigManager configManager;
    private FileLogger fileLogger;

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        fileLogger = FileLogger.getInstance(this);
        configManager = new ConfigManager(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        etMusicDir = findViewById(R.id.etMusicDir);
        swAutoReload = findViewById(R.id.swAutoReload);
        swAutoScan = findViewById(R.id.swAutoScan);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadSettings() {
        configManager.loadConfig(); // Ensure the latest config is loaded
        etMusicDir.setText(configManager.getMusicDir());
        swAutoReload.setChecked(configManager.isAutoReload());
        swAutoScan.setChecked(configManager.isAutoScan());
        fileLogger.i(TAG, "Settings loaded and displayed.");
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity
            }
        });
    }

    private void saveSettings() {
        String newMusicDir = etMusicDir.getText().toString();
        boolean newAutoReload = swAutoReload.isChecked();
        boolean newAutoScan = swAutoScan.isChecked();

        configManager.setMusicDir(newMusicDir);
        configManager.setAutoReload(newAutoReload);
        configManager.setAutoScan(newAutoScan);
        configManager.saveConfig();

        fileLogger.i(TAG, "Settings saved: MusicDir=" + newMusicDir + ", AutoReload=" + newAutoReload + ", AutoScan=" + newAutoScan);
        finish(); // Go back to MainActivity after saving
    }
}
