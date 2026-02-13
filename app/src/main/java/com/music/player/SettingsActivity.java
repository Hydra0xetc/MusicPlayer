package com.music.player;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SettingsActivity extends Activity {

    private EditText etMusicDir;
    private Switch swAutoScan;
    private Spinner spLogLevel;
    private Button btnSaveSettings;
    private Button btnBack;

    private ConfigManager configManager;
    private FileLogger fileLogger;
    private Animation blinkAnimation;

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        fileLogger = FileLogger.getInstance(this);
        configManager = new ConfigManager(this);
        
        fileLogger.setLogLevel(configManager.getLogLevel());

        initViews();
        loadSettings();
        setupListeners();
        updateAutoScanSwitchColor();
    }

    private void initViews() {
        etMusicDir = findViewById(R.id.etMusicDir);
        swAutoScan = findViewById(R.id.swAutoScan);
        spLogLevel = findViewById(R.id.spLogLevel);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnBack = findViewById(R.id.btnBack);
        blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.log_levels,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLogLevel.setAdapter(adapter);
    }

    private void loadSettings() {
        configManager.loadConfig();
        etMusicDir.setText(configManager.getMusicDir());
        swAutoScan.setChecked(configManager.isAutoScan());
        
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spLogLevel.getAdapter();
        if (adapter != null) {
            int spinnerPosition = adapter.getPosition(configManager.getLogLevel());
            spLogLevel.setSelection(spinnerPosition);
        }
        
        fileLogger.i(TAG, "Settings loaded and displayed.");
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(blinkAnimation);
                saveSettings();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(blinkAnimation);
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
        String newLogLevel = spLogLevel.getSelectedItem().toString(); // Get selected log level

        configManager.setMusicDir(newMusicDir);
        configManager.setAutoScan(newAutoScan);
        configManager.setLogLevel(newLogLevel); // Save new log level
        configManager.saveConfig();
        
        // Apply new log level to FileLogger immediately
        FileLogger.getInstance(this).setLogLevel(newLogLevel);

        fileLogger.i(TAG, "Settings saved: MusicDir=" + newMusicDir + ", AutoScan=" + newAutoScan + ", LogLevel=" + newLogLevel);
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
