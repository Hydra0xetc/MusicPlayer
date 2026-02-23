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

    private EditText      etMusicDir;
    private Switch        swAutoScan;
    private Spinner       spLogLevel;
    private Button        btnSaveSettings;
    private Button        btnBack;
    private Button        btnBrowse;
    private ConfigManager configManager;
    private FileLogger    fileLogger;
    private Animation     blinkAnimation;

    private static final int REQUEST_CODE_PICK_DIR = 1001;
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
        btnBrowse = findViewById(R.id.btnBrowse);
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

        btnBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(blinkAnimation);
                openDirectoryPicker();
            }
        });
        
        swAutoScan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAutoScanSwitchColor();
            }
        });
    }

    private void openDirectoryPicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                android.net.Uri uri = data.getData();
                if (uri != null) {
                    String path = getPathFromUri(uri);
                    if (path != null) {
                        etMusicDir.setText(path);
                    } else {
                        Toast.makeText(this, "Could not resolve path", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(android.net.Uri uri) {
        String path = null;
        try {
            if ("com.android.externalstorage.documents".equals(uri.getHost())) {
                String docId = android.provider.DocumentsContract.getTreeDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    path = android.os.Environment.getExternalStorageDirectory() + "/" + (split.length > 1 ? split[1] : "");
                } else {
                    path = "/storage/" + type + "/" + (split.length > 1 ? split[1] : "");
                }
            } else if ("com.android.providers.downloads.documents".equals(uri.getHost())) {
                String id = android.provider.DocumentsContract.getTreeDocumentId(uri);
                if (id.startsWith("raw:")) {
                    path = id.substring(4);
                } else if (id.startsWith("msf:")) {
                    // Handle msf: (Media Storage Framework) IDs if necessary, though tricky for directories
                    fileLogger.w(TAG, "MSF ID found, may not resolve to absolute path: " + id);
                }
            }
            
            // Cleanup double slashes if any
            if (path != null) {
                path = path.replace("//", "/");
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
            }
        } catch (Exception e) {
            fileLogger.e(TAG, "Error resolving path: " + e.getMessage());
        }
        return path;
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
