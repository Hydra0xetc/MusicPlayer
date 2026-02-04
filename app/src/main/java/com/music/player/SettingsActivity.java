package com.music.player;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.graphics.PorterDuff;

public class SettingsActivity extends Activity {

    private EditText etMusicDir;
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
        updateAutoScanSwitchColor();
    }

    private void initViews() {
        etMusicDir = findViewById(R.id.etMusicDir);
        swAutoScan = findViewById(R.id.swAutoScan);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadSettings() {
        configManager.loadConfig(); // Ensure the latest config is loaded
        etMusicDir.setText(configManager.getMusicDir());
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
        
        swAutoScan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAutoScanSwitchColor();
            }
        });
    }

    private void saveSettings() {
        String newMusicDir = etMusicDir.getText().toString();
        boolean newAutoScan = swAutoScan.isChecked();

        configManager.setMusicDir(newMusicDir);
        configManager.setAutoScan(newAutoScan);
        configManager.saveConfig();

        fileLogger.i(TAG, "Settings saved: MusicDir=" + newMusicDir + ", AutoScan=" + newAutoScan);
        finish(); // Go back to MainActivity after saving
    }

    private void updateAutoScanSwitchColor() {
        if (swAutoScan.isChecked()) {
            swAutoScan.getTrackDrawable().setColorFilter(getResources().getColor(R.color.switch_on), PorterDuff.Mode.SRC_IN);
            swAutoScan.getThumbDrawable().setColorFilter(getResources().getColor(R.color.switch_on), PorterDuff.Mode.SRC_IN);
        } else {
            swAutoScan.getTrackDrawable().setColorFilter(getResources().getColor(R.color.switch_off), PorterDuff.Mode.SRC_IN);
            swAutoScan.getThumbDrawable().setColorFilter(getResources().getColor(R.color.switch_off), PorterDuff.Mode.SRC_IN);
        }
    }
}
