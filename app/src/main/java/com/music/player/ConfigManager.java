package com.music.player;

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

    private String musicDir;
    private boolean autoScan;
    private String logLevel;
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
                fileLogger.w(TAG, "Config file not found at " + configFile.getAbsolutePath() + ", using default.");
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

                File dir = new File(musicDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    Toast.makeText(context, "Folder not found: " + musicDir, Toast.LENGTH_SHORT).show();
                }

                fileLogger.i(TAG, "Config loaded from: " + configFile.getAbsolutePath() + ", MusicDir: " + musicDir
                        + ", AutoScan: " + autoScan + ", LogLevel: " + logLevel);
                CrashHandler.install(context);
            } else {
                fileLogger.w(TAG, "Empty config, using default");
                setDefaults();
            }

        } catch (IOException e) {
            fileLogger.e(TAG, "I/O error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage());
            setDefaults();
            createDefaultConfig();
        } catch (Exception e) {
            fileLogger.e(TAG, "Error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage());
            setDefaults();
            createDefaultConfig();
        }
    }

    public void saveConfig() {
        try {
            if (!configDirFile.exists()) {
                configDirFile.mkdirs();
                if (!configDirFile.exists()) {
                    throw new IOException("Failed to create config directory: " + configDirFile.getAbsolutePath());
                }
            }

            JSONArray configArray = new JSONArray();
            JSONObject config = new JSONObject();
            config.put(KEY_AUTO_SCAN, autoScan);
            config.put(KEY_MUSIC_DIR, musicDir);
            config.put(KEY_LOG_LEVEL, logLevel);
            configArray.put(config);

            FileWriter writer = new FileWriter(configFile);
            writer.write(configArray.toString(2));
            writer.close();

            fileLogger.i(TAG, "Config saved to: " + configFile.getAbsolutePath());

        } catch (IOException e) {
            fileLogger.e(TAG, "I/O error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage());
        } catch (Exception e) {
            fileLogger.e(TAG, "Error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private void createDefaultConfig() {
        setDefaults();
        saveConfig();
        fileLogger.i(TAG, "Default config created at " + configFile.getAbsolutePath());
    }

    private void setDefaults() {
        musicDir = Constant.DEFAULT_MUSIC_DIR;
        autoScan = false;
        logLevel = "INFO";
    }

    public String getMusicDir() {
        return musicDir;
    }

    public void setMusicDir(String dir) {
        this.musicDir = dir;
    }

    public boolean isAutoScan() {
        return autoScan;
    }

    public void setAutoScan(boolean autoScan) {
        this.autoScan = autoScan;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}
