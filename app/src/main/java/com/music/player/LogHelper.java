package com.music.player;

import android.content.Context;

public class LogHelper {

    private static final String TAG = "LogHelper";
    private final FileLogger fileLogger;

    public LogHelper(Context context) {
        this.fileLogger = FileLogger.getInstance(context);
    }

    /**
     * Log normal (INFO level)
     */
    public void log(String message) {
        fileLogger.i(TAG, message);
    }
    
    /**
     * Log level DEBUG
     */
    public void logDebug(String message) {
        fileLogger.d(TAG, message);
    }
    
    /**
     * Log level WARNING
     */
    public void logWarning(String message) {
        fileLogger.w(TAG, message);
    }
    
    /**
     * Log level ERROR
     */
    public void logError(String message) {
        fileLogger.e(TAG, message);
    }
    
    /**
     * Log error Exception (akan menulis stack trace ke file)
     */
    public void logError(String message, Exception e) {
        fileLogger.e(TAG, message, e);
    }
    
    /**
     * Dapatkan path file log
     */
    public String getLogPath() {
        return fileLogger.getLogPath();
    }
}
