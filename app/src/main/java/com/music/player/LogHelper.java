package com.music.player;

import android.content.Context;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogHelper {

    private static final String TAG = "LogHelper";
    private final TextView tvLog;
    private final ScrollView scrollView;
    private final View container;
    private final FileLogger fileLogger;
    private boolean visible = false;

    public LogHelper(Context context, TextView tvLog, ScrollView scrollView, View container) {
        this.tvLog = tvLog;
        this.scrollView = scrollView;
        this.container = container;
        this.fileLogger = FileLogger.getInstance(context);
        setVisible(false);
    }

    public void toggle() {
        setVisible(!visible);
    }

    public void setVisible(boolean show) {
        visible = show;
        container.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Log normal (INFO level)
     */
    public void log(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] " + message;
        
        // Add to TextView
        tvLog.append("\n" + logEntry);
        
        // Scroll to bottom
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        // Write to file juga
        fileLogger.i(TAG, message);
    }
    
    /**
     * Log dengan level DEBUG
     */
    public void logDebug(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] [DEBUG] " + message;
        
        tvLog.append("\n" + logEntry);
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        fileLogger.d(TAG, message);
    }
    
    /**
     * Log dengan level WARNING
     */
    public void logWarning(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] [WARN] " + message;
        
        tvLog.append("\n" + logEntry);
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        fileLogger.w(TAG, message);
    }
    
    /**
     * Log dengan level ERROR
     */
    public void logError(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] [ERROR] " + message;
        
        tvLog.append("\n" + logEntry);
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        fileLogger.e(TAG, message);
    }
    
    /**
     * Log error dengan Exception (akan menulis stack trace ke file)
     */
    public void logError(String message, Exception e) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] [ERROR] " + message + ": " + e.getMessage();
        
        tvLog.append("\n" + logEntry);
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        // Write to file dengan full stack trace
        fileLogger.e(TAG, message, e);
    }
    
    /**
     * Dapatkan path file log
     */
    public String getLogPath() {
        return fileLogger.getLogPath();
    }

    // Named inner class instead of lambda
    private static class ScrollToBottomRunnable implements Runnable {
        private ScrollView scrollView;

        public ScrollToBottomRunnable(ScrollView scrollView) {
            this.scrollView = scrollView;
        }

        @Override
        public void run() {
            scrollView.fullScroll(View.FOCUS_DOWN);
        }
    }
}
