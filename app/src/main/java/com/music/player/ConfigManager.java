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

public class ConfigManager {
    private static final String CONFIG_DIR = "/sdcard/MusicPlayer/";
    private static final String CONFIG_FILE = "config.json";
    private static final String DEFAULT_MUSIC_DIR = "/sdcard/Music/";
    
    private String               musicDir;
    private boolean              autoReload;
    private boolean              autoScan;
    private FileLogger           fileLogger;
    private ConfigFileObserver   fileObserver;
    private ConfigChangeListener changeListener;
    private Context              context;
    
    public interface ConfigChangeListener {
        void onConfigChanged();
    }
    
    public ConfigManager(Context context) {
        fileLogger = FileLogger.getInstance(context);
        this.context = context;
        loadConfig();
    }
    
    public void startWatching(ConfigChangeListener listener) {
        this.changeListener = listener;
        
        // Always start the FileObserver to detect changes
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // Ensure only one observer is active
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        
        fileObserver = new ConfigFileObserver(CONFIG_DIR);
        fileObserver.startWatching();
        fileLogger.i("ConfigManager", "Started watching config file for changes");
    }
    
    public void stopWatching() {
        if (fileObserver != null) {
            fileObserver.stopWatching();
            fileObserver = null;
            fileLogger.i("ConfigManager", "Stopped watching config file");
        }
    }
    
    private class ConfigFileObserver extends FileObserver {
        private String path;
        
        public ConfigFileObserver(String path) {
            super(path, FileObserver.MODIFY | FileObserver.CLOSE_WRITE);
            this.path = path;
        }
        
        @Override
        public void onEvent(int event, String filename) {
            if (filename != null && filename.equals(CONFIG_FILE)) {
                fileLogger.i("ConfigManager", "Config file changed, reloading...");
                
                // Delay briefly to ensure the file has finished writing
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                loadConfig();
                
                if (changeListener != null) {
                    changeListener.onConfigChanged();
                }
            }
        }
    }
    
    public void loadConfig() {
        try {
            File configFile = new File(CONFIG_DIR + CONFIG_FILE);
            
            if (!configFile.exists()) {
                fileLogger.w("ConfigManager", "Config file not found, using default");
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
                
                musicDir = config.optString("music_dir", DEFAULT_MUSIC_DIR);
                autoReload = config.optBoolean("auto_reload", false);
                autoScan = config.optBoolean("auto_scan", false);
                
                // Validate path
                File dir = new File(musicDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    fileLogger.w("ConfigManager", "Invalid music dir in config: " + musicDir);
                    musicDir = DEFAULT_MUSIC_DIR;
                }
                
                fileLogger.i("ConfigManager", "Config loaded: " + musicDir);
                fileLogger.i("ConfigManager", "auto_reload: " + autoReload + ", auto_scan: " + autoScan);
            } else {
                fileLogger.w("ConfigManager", "Empty config, using default");
                setDefaults();
            }
            
        } catch (Exception e) {
            fileLogger.e("ConfigManager", "Error loading config: " + e.getMessage());
            Toast.makeText(
                    context,
                    "Error loading config: " + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
            setDefaults();
            createDefaultConfig();
        }
    }
    
    public void saveConfig() {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            JSONArray configArray = new JSONArray();
            JSONObject config = new JSONObject();
            config.put("auto_reload", autoReload);
            config.put("auto_scan", autoScan);
            config.put("music_dir", musicDir);
            configArray.put(config);
            
            FileWriter writer = new FileWriter(CONFIG_DIR + CONFIG_FILE);
            writer.write(configArray.toString(2));
            writer.close();
            
            fileLogger.i("ConfigManager", "Config saved: " + musicDir);
            
        } catch (Exception e) {
            fileLogger.e("ConfigManager", "Error saving config: " + e.getMessage());
        }
    }
    
    private void createDefaultConfig() {
        setDefaults();
        saveConfig();
        fileLogger.i("ConfigManager", "Default config created");
    }
    
    private void setDefaults() {
        musicDir = DEFAULT_MUSIC_DIR;
        autoReload = false;
        autoScan = false;
    }
    
    public String getMusicDir() {
        return musicDir;
    }
    
    public void setMusicDir(String dir) {
        this.musicDir = dir;
    }
    
    public boolean isAutoReload() {
        return autoReload;
    }
    
    public void setAutoReload(boolean autoReload) {
        this.autoReload = autoReload;
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
