package com.music.player;

import android.content.Context;
import android.os.FileObserver;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;

public class ConfigManager {
    // Keep CONFIG_DIR as /sdcard/MusicPlayer/ as per user's request
    private static final String CONFIG_ROOT_DIR = "/sdcard/MusicPlayer/";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String DEFAULT_MUSIC_DIR = "/sdcard/Music/";
    
    private String               musicDir;
    private boolean              autoScan;
    private FileLogger           fileLogger;
    private Context              context;
    private File                 configDirFile; // Resolved config directory File object
    private File                 configFile;    // Resolved config file File object
    
    public ConfigManager(Context context) {
        fileLogger = FileLogger.getInstance(context);
        this.context = context;
        
        configDirFile = new File(CONFIG_ROOT_DIR);
        configFile = new File(configDirFile, CONFIG_FILE_NAME);
        
        loadConfig();
    }
    
    public void startWatching() {
        // No longer watching config file automatically, as autoReload is removed.
        // This method can be kept if other watching functionality is added later.
    }
    
    public void stopWatching() {
        // No longer watching config file automatically.
    }
    
    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                fileLogger.w("ConfigManager", "Config file not found at " + configFile.getAbsolutePath() + ", using default.");
                setDefaults();
                createDefaultConfig(); // createDefaultConfig calls saveConfig() which has its own error handling
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
                
                musicDir = config.optString("music_dir", DEFAULT_MUSIC_DIR);
                autoScan = config.optBoolean("auto_scan", false);
                
                // Validate path
                File dir = new File(musicDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    fileLogger.w("ConfigManager", "Invalid music dir in config: " + musicDir + ", defaulting to " + DEFAULT_MUSIC_DIR);
                    musicDir = DEFAULT_MUSIC_DIR;
                }
                
                fileLogger.i("ConfigManager", "Config loaded from: " + configFile.getAbsolutePath() + ", MusicDir: " + musicDir + ", AutoScan: " + autoScan);
            } else {
                fileLogger.w("ConfigManager", "Empty config, using default");
                setDefaults();
            }
            
        } catch (IOException e) { // Catch IOException specifically for file operations
            fileLogger.e("ConfigManager", "I/O error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage());
            Toast.makeText(
                    context,
                    "Error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage() + ". Check permissions.",
                    Toast.LENGTH_LONG // Use LONG for more visibility
            ).show();
            setDefaults();
            createDefaultConfig();
        } catch (Exception e) { // General catch for JSON parsing or other errors
            fileLogger.e("ConfigManager", "Error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage());
            Toast.makeText(
                    context,
                    "Error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage(),
                    Toast.LENGTH_LONG // Use LONG for more visibility
            ).show();
            setDefaults();
            createDefaultConfig();
        }
    }
    
    public void saveConfig() {
        try {
            if (!configDirFile.exists()) {
                configDirFile.mkdirs();
                if (!configDirFile.exists()) { // Check again if mkdirs failed
                    throw new IOException("Failed to create config directory: " + configDirFile.getAbsolutePath());
                }
            }
            
            JSONArray configArray = new JSONArray();
            JSONObject config = new JSONObject();
            config.put("auto_scan", autoScan);
            config.put("music_dir", musicDir);
            configArray.put(config);
            
            FileWriter writer = new FileWriter(configFile); // Write to the resolved configFile
            writer.write(configArray.toString(2));
            writer.close();
            
            fileLogger.i("ConfigManager", "Config saved to: " + configFile.getAbsolutePath());
            Toast.makeText(context, "Settings saved to " + configFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) { // Catch IOException specifically for file operations
            fileLogger.e("ConfigManager", "I/O error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage());
            Toast.makeText(
                    context,
                    "Error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage() + ". Check app permissions.",
                    Toast.LENGTH_LONG // Use LONG for more visibility
            ).show();
        } catch (Exception e) { // General catch for other errors
            fileLogger.e("ConfigManager", "Error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage());
            Toast.makeText(
                    context,
                    "Error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage(),
                    Toast.LENGTH_LONG // Use LONG for more visibility
            ).show();
        }
    }
    
    private void createDefaultConfig() {
        setDefaults();
        // saveConfig will be called here, which includes error handling
        saveConfig(); 
        fileLogger.i("ConfigManager", "Default config created at " + configFile.getAbsolutePath());
    }
    
    private void setDefaults() {
        musicDir = DEFAULT_MUSIC_DIR;
        autoScan = false;
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
    
    public boolean isValid() {
        File dir = new File(musicDir);
        return dir.exists() && dir.isDirectory();
    }
}
