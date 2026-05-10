package com.music.player.manager;

import com.music.player.model.*;
import com.music.player.utils.*;

import android.content.Context;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;

public class ConfigManager {

    private static final String TAG = "ConfigManager";

    // JSON Keys
    private static final String KEY_MUSIC_DIR = "music_dir";
    private static final String KEY_AUTO_SCAN = "auto_scan";
    private static final String KEY_LOG_LEVEL = "log_level";
    private static final String KEY_VIS_NOISE_FLOOR = "vis_noise_floor";
    private static final String KEY_VIS_SMOOTHING = "vis_smoothing";
    private static final String KEY_VIS_BAR_COUNT = "vis_bar_count";
    private static final String KEY_VIS_INNER_RADIUS = "vis_inner_radius";
    private static final String KEY_VIS_MAX_BAR_LEN = "vis_max_bar_len";
    private static final String KEY_VIS_DECAY_SPEED = "vis_decay_speed";
    private static final String KEY_VIS_BAR_WIDTH = "vis_bar_width";

    private String musicDir;
    private boolean autoScan;
    private String logLevel;
    private float visNoiseFloor;
    private float visSmoothing;
    private int visBarCount;
    private float visInnerRadius;
    private float visMaxBarLen;
    private float visDecaySpeed;
    private float visBarWidth;

    private FileLogger fileLogger;
    private Context context;
    private File configDirFile;
    private File configFile;

    public ConfigManager(Context context) {
        fileLogger = FileLogger.getInstance(context);
        this.context = context;

        configDirFile = context.getExternalFilesDir(null);
        if (configDirFile == null) {
            configDirFile = context.getFilesDir();
        }
        configFile = new File(configDirFile, Constant.CONFIG_FILE_NAME);

        loadConfig();
    }

    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                fileLogger.w(TAG, "Config file not found, using default.");
                setDefaults();
                createDefaultConfig();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            JSONArray configArray = new JSONArray(json.toString());
            if (configArray.length() > 0) {
                JSONObject config = configArray.getJSONObject(0);

                musicDir = config.optString(KEY_MUSIC_DIR, Constant.DEFAULT_MUSIC_DIR);
                autoScan = config.optBoolean(KEY_AUTO_SCAN, false);
                logLevel = config.optString(KEY_LOG_LEVEL, "INFO");

                visNoiseFloor = (float) config.optDouble(KEY_VIS_NOISE_FLOOR, 5.0f);
                visSmoothing = (float) config.optDouble(KEY_VIS_SMOOTHING, 0.60f);
                visBarCount = config.optInt(KEY_VIS_BAR_COUNT, 60);
                visInnerRadius = (float) config.optDouble(KEY_VIS_INNER_RADIUS, 0.53f);
                visMaxBarLen = (float) config.optDouble(KEY_VIS_MAX_BAR_LEN, 0.60f);
                visDecaySpeed = (float) config.optDouble(KEY_VIS_DECAY_SPEED, 0.15f);
                visBarWidth = (float) config.optDouble(KEY_VIS_BAR_WIDTH, 0.45f);

                fileLogger.i(TAG, "Config loaded.");
                CrashHandler.install(context);
            } else {
                setDefaults();
            }

        } catch (Exception e) {
            fileLogger.e(TAG, "Error loading config: " + e.getMessage());
            setDefaults();
        }
    }

    public void saveConfig() {
        try {
            if (!configDirFile.exists()) configDirFile.mkdirs();

            JSONArray configArray = new JSONArray();
            JSONObject config = new JSONObject();
            config.put(KEY_AUTO_SCAN, autoScan);
            config.put(KEY_MUSIC_DIR, musicDir);
            config.put(KEY_LOG_LEVEL, logLevel);
            config.put(KEY_VIS_NOISE_FLOOR, visNoiseFloor);
            config.put(KEY_VIS_SMOOTHING, visSmoothing);
            config.put(KEY_VIS_BAR_COUNT, visBarCount);
            config.put(KEY_VIS_INNER_RADIUS, visInnerRadius);
            config.put(KEY_VIS_MAX_BAR_LEN, visMaxBarLen);
            config.put(KEY_VIS_DECAY_SPEED, visDecaySpeed);
            config.put(KEY_VIS_BAR_WIDTH, visBarWidth);
            configArray.put(config);

            FileWriter writer = new FileWriter(configFile);
            writer.write(configArray.toString(2));
            writer.close();
            fileLogger.i(TAG, "Config saved.");
        } catch (Exception e) {
            fileLogger.e(TAG, "Error saving config: " + e.getMessage());
        }
    }

    private void createDefaultConfig() {
        setDefaults();
        saveConfig();
    }

    private void setDefaults() {
        musicDir = Constant.DEFAULT_MUSIC_DIR;
        autoScan = false;
        logLevel = "INFO";
        visNoiseFloor = 5.0f;
        visSmoothing = 0.60f;
        visBarCount = 60;
        visInnerRadius = 0.53f;
        visMaxBarLen = 0.60f;
        visDecaySpeed = 0.15f;
        visBarWidth = 0.45f;
    }

    // Getters and Setters
    public String getMusicDir() { return musicDir; }
    public void setMusicDir(String dir) { this.musicDir = dir; }

    public boolean isAutoScan() { return autoScan; }
    public void setAutoScan(boolean autoScan) { this.autoScan = autoScan; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public float getVisNoiseFloor() { return visNoiseFloor; }
    public void setVisNoiseFloor(float val) { this.visNoiseFloor = val; }

    public float getVisSmoothing() { return visSmoothing; }
    public void setVisSmoothing(float val) { this.visSmoothing = val; }

    public int getVisBarCount() { return visBarCount; }
    public void setVisBarCount(int val) { this.visBarCount = val; }

    public float getVisInnerRadius() { return visInnerRadius; }
    public void setVisInnerRadius(float val) { this.visInnerRadius = val; }

    public float getVisMaxBarLen() { return visMaxBarLen; }
    public void setVisMaxBarLen(float val) { this.visMaxBarLen = val; }

    public float getVisDecaySpeed() { return visDecaySpeed; }
    public void setVisDecaySpeed(float val) { this.visDecaySpeed = val; }

    public float getVisBarWidth() { return visBarWidth; }
    public void setVisBarWidth(float val) { this.visBarWidth = val; }
}
