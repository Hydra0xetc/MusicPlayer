package com.music.player.ui;
import com.music.player.R;
import com.music.player.model.*;
import com.music.player.manager.*;
import com.music.player.service.*;
import com.music.player.player.*;
import com.music.player.utils.*;
import com.music.player.scanner.*;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SettingsActivity extends Activity {

    private EditText etMusicDir;
    private Switch swAutoScan;
    private Spinner spLogLevel;
    private Button btnSaveSettings, btnBack, btnBrowse;
    
    private SeekBar seekSensitivity, seekSmoothing, seekBarCount;
    private SeekBar seekInnerRadius, seekMaxBarLen, seekDecaySpeed, seekBarWidth;
    
    private TextView labelSensitivity, labelSmoothing, labelBarCount;
    private TextView labelInnerRadius, labelMaxBarLen, labelDecaySpeed, labelBarWidth;
    
    private ConfigManager configManager;
    private FileLogger fileLogger;
    private Animation blinkAnimation;
    private ArrayAdapter<CharSequence> logLevelAdapter;

    private static final int REQUEST_CODE_PICK_DIR = 1001;
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
        spLogLevel = findViewById(R.id.spLogLevel);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnBack = findViewById(R.id.btnBack);
        btnBrowse = findViewById(R.id.btnBrowse);
        
        seekSensitivity = findViewById(R.id.seek_sensitivity);
        seekSmoothing = findViewById(R.id.seek_smoothing);
        seekBarCount = findViewById(R.id.seek_bar_count);
        seekInnerRadius = findViewById(R.id.seek_inner_radius);
        seekMaxBarLen = findViewById(R.id.seek_max_bar_len);
        seekDecaySpeed = findViewById(R.id.seek_decay_speed);
        seekBarWidth = findViewById(R.id.seek_bar_width);

        labelSensitivity = findViewById(R.id.label_sensitivity);
        labelSmoothing = findViewById(R.id.label_smoothing);
        labelBarCount = findViewById(R.id.label_bar_count);
        labelInnerRadius = findViewById(R.id.label_inner_radius);
        labelMaxBarLen = findViewById(R.id.label_max_bar_len);
        labelDecaySpeed = findViewById(R.id.label_decay_speed);
        labelBarWidth = findViewById(R.id.label_bar_width);

        blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);

        logLevelAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.log_levels,
                android.R.layout.simple_spinner_item);
        logLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLogLevel.setAdapter(logLevelAdapter);
    }

    private void loadSettings() {
        configManager.loadConfig();
        etMusicDir.setText(configManager.getMusicDir());
        swAutoScan.setChecked(configManager.isAutoScan());

        if (logLevelAdapter != null) {
            int pos = logLevelAdapter.getPosition(configManager.getLogLevel());
            spLogLevel.setSelection(pos);
        }

        // Visualizer
        float sens = configManager.getVisNoiseFloor();
        seekSensitivity.setProgress((int)(sens * 10));
        updateSensitivityLabel(sens);

        float smooth = configManager.getVisSmoothing();
        seekSmoothing.setProgress((int)((smooth - 0.10f) * 100));
        updateSmoothingLabel(smooth);

        int bars = configManager.getVisBarCount();
        seekBarCount.setProgress(bars - 20);
        updateBarCountLabel(bars);

        float inner = configManager.getVisInnerRadius();
        seekInnerRadius.setProgress((int)((inner - 0.10f) * 100));
        updateInnerRadiusLabel(inner);

        float maxLen = configManager.getVisMaxBarLen();
        seekMaxBarLen.setProgress((int)(maxLen * 100));
        updateMaxBarLenLabel(maxLen);

        float decay = configManager.getVisDecaySpeed();
        seekDecaySpeed.setProgress((int)((decay - 0.05f) * 100));
        updateDecaySpeedLabel(decay);

        float width = configManager.getVisBarWidth();
        seekBarWidth.setProgress((int)((width - 0.10f) * 100));
        updateBarWidthLabel(width);
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            saveSettings();
        });

        btnBack.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            finish();
        });

        btnBrowse.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            openDirectoryPicker();
        });

        swAutoScan.setOnCheckedChangeListener((btn, isChecked) -> updateAutoScanSwitchColor());

        seekSensitivity.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateSensitivityLabel(Math.max(0.1f, p / 10.0f));
            }
        });
        seekSmoothing.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateSmoothingLabel(0.10f + (p / 100.0f));
            }
        });
        seekBarCount.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                int val = 20 + p;
                if (val % 2 != 0) val++;
                updateBarCountLabel(val);
            }
        });
        seekInnerRadius.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateInnerRadiusLabel(0.10f + (p / 100.0f));
            }
        });
        seekMaxBarLen.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateMaxBarLenLabel(p / 100.0f);
            }
        });
        seekDecaySpeed.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateDecaySpeedLabel(0.05f + (p / 100.0f));
            }
        });
        seekBarWidth.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                updateBarWidthLabel(0.10f + (p / 100.0f));
            }
        });
    }

    private void updateSensitivityLabel(float v) { labelSensitivity.setText(String.format("Sensitivity: %.1f", v)); }
    private void updateSmoothingLabel(float v) { labelSmoothing.setText(String.format("Smoothing: %.2f", v)); }
    private void updateBarCountLabel(int v) { labelBarCount.setText(String.format("Bar Count: %d", v)); }
    private void updateInnerRadiusLabel(float v) { labelInnerRadius.setText(String.format("Inner Radius: %.2f", v)); }
    private void updateMaxBarLenLabel(float v) { labelMaxBarLen.setText(String.format("Max Bar Len: %.2f", v)); }
    private void updateDecaySpeedLabel(float v) { labelDecaySpeed.setText(String.format("Decay Speed: %.2f", v)); }
    private void updateBarWidthLabel(float v) { labelBarWidth.setText(String.format("Bar Thickness: %.2f", v)); }

    private void saveSettings() {
        configManager.setMusicDir(etMusicDir.getText().toString());
        configManager.setAutoScan(swAutoScan.isChecked());
        configManager.setLogLevel(spLogLevel.getSelectedItem().toString());
        
        configManager.setVisNoiseFloor(Math.max(0.1f, seekSensitivity.getProgress() / 10.0f));
        configManager.setVisSmoothing(0.10f + (seekSmoothing.getProgress() / 100.0f));
        int bars = 20 + seekBarCount.getProgress();
        if (bars % 2 != 0) bars++;
        configManager.setVisBarCount(bars);
        configManager.setVisInnerRadius(0.10f + (seekInnerRadius.getProgress() / 100.0f));
        configManager.setVisMaxBarLen(seekMaxBarLen.getProgress() / 100.0f);
        configManager.setVisDecaySpeed(0.05f + (seekDecaySpeed.getProgress() / 100.0f));
        configManager.setVisBarWidth(0.10f + (seekBarWidth.getProgress() / 100.0f));
        
        configManager.saveConfig();
        FileLogger.getInstance(this).setLogLevel(configManager.getLogLevel());
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void openDirectoryPicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                String path = getPathFromUri(uri);
                if (path != null) etMusicDir.setText(path);
            }
        }
    }

    private String getPathFromUri(android.net.Uri uri) {
        String path = null;
        try {
            if ("com.android.externalstorage.documents".equals(uri.getHost())) {
                String docId = android.provider.DocumentsContract.getTreeDocumentId(uri);
                String[] split = docId.split(":");
                if ("primary".equalsIgnoreCase(split[0])) {
                    path = android.os.Environment.getExternalStorageDirectory() + "/" + (split.length > 1 ? split[1] : "");
                } else {
                    path = "/storage/" + split[0] + "/" + (split.length > 1 ? split[1] : "");
                }
            }
            if (path != null) path = path.replace("//", "/");
        } catch (Exception e) { fileLogger.e(TAG, "Path error: " + e); }
        return path;
    }

    private void updateAutoScanSwitchColor() {
        int color = swAutoScan.isChecked() ? getResources().getColor(R.color.switch_on) : getResources().getColor(R.color.switch_off);
        swAutoScan.getTrackDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        swAutoScan.getThumbDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private abstract static class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {
            // No action needed
        }
    }
}
