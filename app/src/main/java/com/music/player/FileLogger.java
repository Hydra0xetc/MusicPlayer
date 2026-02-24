package com.music.player;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FileLogger {
    private final static String TAG = "FileLogger";
    private static FileLogger instance;
    private File logFile;
    private final SimpleDateFormat dateFormat;
    
    private int logLevelThreshold;
    private static final Map<String, Integer> LOG_LEVEL_MAP = new HashMap<>();
    static {
        LOG_LEVEL_MAP.put("DEBUG", 0);
        LOG_LEVEL_MAP.put("INFO", 1);
        LOG_LEVEL_MAP.put("WARN", 2);
        LOG_LEVEL_MAP.put("ERROR", 3);
    }
    
    private FileLogger(Context context) {
        File logDir = context.getExternalFilesDir(null);
        if (logDir != null) {
            logFile = new File(logDir, "log.txt");
 
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to create log file", e);
            }
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        setLogLevel("INFO");
    }
    
    public static synchronized FileLogger getInstance(Context context) {
        if (instance == null) {
            instance = new FileLogger(context.getApplicationContext());
        }
        return instance;
    }
    
    public synchronized void setLogLevel(String level) {
        Integer newThreshold = LOG_LEVEL_MAP.get(level.toUpperCase());
        if (newThreshold != null) {
            logLevelThreshold = newThreshold;
            android.util.Log.i(TAG, "File logger level set to: " + level);
        } else {
            android.util.Log.e(TAG, "Invalid log level: " + level + ". Keeping current level.");
        }
    }
    
    private void rotateLogs() {
        if (logFile == null || !logFile.exists()) {
            return;
        }
        
        if (logFile.length() > Constant.MAX_LOG_SIZE) {
            File oldLog = new File(logFile.getParent(), "log.old.txt");
            if (oldLog.exists()) {
                oldLog.delete();
            }
            logFile.renameTo(oldLog);
            
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to create new log file", e);
                logFile = null; 
            }
        }
    }
    
    private synchronized void writeToFile(String level, String tag, String message) {
        if (logFile == null) {
            return;
        }
        
        if (LOG_LEVEL_MAP.get(level) < logLevelThreshold) {
            return; 
        }
        
        
        rotateLogs();
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("%s [%s] %s: %s\n", 
                timestamp, level, tag, message);
            
            pw.print(logEntry);
            pw.flush();
            
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to write log", e);
        }
    }
    
    private synchronized void writeToFile(String level, String tag, String message, Throwable throwable) {
        if (logFile == null) {
            return;
        }
        
        if (LOG_LEVEL_MAP.get(level) < logLevelThreshold) {
            return; 
        }
        
        rotateLogs();
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("%s [%s] %s: %s\n", 
                timestamp, level, tag, message);
            
            pw.print(logEntry);
            
            
            if (throwable != null) {
                throwable.printStackTrace(pw);
            }
            
            pw.flush();
            
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to write log with throwable", e);
        }
    }
    
    public void i(String tag, String message) {
        writeToFile("INFO", tag, message);
        android.util.Log.i(tag, message);
    }

    public void d(String tag, String message) {
        writeToFile("DEBUG", tag, message);
        android.util.Log.d(tag, message);
    }

    public void w(String tag, String message) {
        writeToFile("WARN", tag, message);
        android.util.Log.w(tag, message);
    }
    
   public void e(String tag, String message) {
        writeToFile("ERROR", tag, message);
        android.util.Log.e(tag, message);
    }
    
    public void e(String tag, String message, Throwable throwable) {
        writeToFile("ERROR", tag, message, throwable);
        android.util.Log.e(tag, message, throwable);
    }
    
    public String getLogPath() {
        return logFile != null ? logFile.getAbsolutePath() : "Log file not available";
    }
    
    public void clearLogs() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to recreate log file", e);
            }
        }
    }
    
    public boolean exists() {
        return logFile != null && logFile.exists();
    }
    
    public long getSize() {
        return logFile != null && logFile.exists() ? logFile.length() : 0;
    }
}
