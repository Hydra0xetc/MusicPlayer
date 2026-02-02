package com.music.player;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logger that saves logs to a file in getExternalFilesDir
 * Thread-safe and automatically rotates files when too large
 */
public class FileLogger {
    private static FileLogger instance;
    private File logFile;
    private final SimpleDateFormat dateFormat;
    private static final int MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB
    
    private FileLogger(Context context) {
        File logDir = context.getExternalFilesDir(null);
        if (logDir != null) {
            logFile = new File(logDir, "log.txt");
            
            // Create file if it doesn't exist yet
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                android.util.Log.e("FileLogger", "Failed to create log file", e);
            }
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }
    
    /**
     * Get the FileLogger instance (Singleton)
     */
    public static synchronized FileLogger getInstance(Context context) {
        if (instance == null) {
            instance = new FileLogger(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Rotate logs if the file is too large
     */
    private void rotateLogs() {
        if (logFile == null || !logFile.exists()) {
            return;
        }
        
        if (logFile.length() > MAX_LOG_SIZE) {
            File oldLog = new File(logFile.getParent(), "log.old.txt");
            if (oldLog.exists()) {
                oldLog.delete();
            }
            logFile.renameTo(oldLog);
            
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                android.util.Log.e("FileLogger", "Failed to create new log file", e);
                logFile = null; // Set logFile to null if creation fails
            }
        }
    }
    
    /**
     * Write log to file
     */
    private synchronized void writeToFile(String level, String tag, String message) {
        if (logFile == null) {
            return;
        }
        
        // Rotate if necessary
        rotateLogs();
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("%s [%s] %s: %s\n", 
                timestamp, level, tag, message);
            
            pw.print(logEntry);
            pw.flush();
            
        } catch (IOException e) {
            android.util.Log.e("FileLogger", "Failed to write log", e);
        }
    }
    
    /**
     * Write log with stack trace
     */
    private synchronized void writeToFile(String level, String tag, String message, Throwable throwable) {
        if (logFile == null) {
            return;
        }
        
        rotateLogs();
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("%s [%s] %s: %s\n", 
                timestamp, level, tag, message);
            
            pw.print(logEntry);
            
            // Write stack trace
            if (throwable != null) {
                throwable.printStackTrace(pw);
            }
            
            pw.flush();
            
        } catch (IOException e) {
            android.util.Log.e("FileLogger", "Failed to write log with throwable", e);
        }
    }
    
    /**
     * Log level INFO
     */
    public void i(String tag, String message) {
        writeToFile("INFO", tag, message);
        android.util.Log.i(tag, message);
    }
    
    /**
     * Log level DEBUG
     */
    public void d(String tag, String message) {
        writeToFile("DEBUG", tag, message);
        android.util.Log.d(tag, message);
    }
    
    /**
     * Log level WARNING
     */
    public void w(String tag, String message) {
        writeToFile("WARN", tag, message);
        android.util.Log.w(tag, message);
    }
    
    /**
     * Log level ERROR
     */
    public void e(String tag, String message) {
        writeToFile("ERROR", tag, message);
        android.util.Log.e(tag, message);
    }
    
    /**
     * Log level ERROR with Exception
     */
    public void e(String tag, String message, Throwable throwable) {
        writeToFile("ERROR", tag, message, throwable);
        android.util.Log.e(tag, message, throwable);
    }
    
    /**
     * Get the log file path
     */
    public String getLogPath() {
        return logFile != null ? logFile.getAbsolutePath() : "Log file not available";
    }
    
    /**
     * Clear all logs
     */
    public void clearLogs() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                android.util.Log.e("FileLogger", "Failed to recreate log file", e);
            }
        }
    }
    
    /**
     * Check if the log file exists
     */
    public boolean exists() {
        return logFile != null && logFile.exists();
    }
    
    /**
     * Get log file size in bytes
     */
    public long getSize() {
        return logFile != null && logFile.exists() ? logFile.length() : 0;
    }
}
