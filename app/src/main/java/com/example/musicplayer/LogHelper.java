package com.example.musicplayer;

import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogHelper {

    private final TextView tvLog;
    private final ScrollView scrollView;
    private final View container;
    private boolean visible = false;
    private File logFile;

    public LogHelper(TextView tvLog, ScrollView scrollView, View container, File logFile) {
        this.tvLog = tvLog;
        this.scrollView = scrollView;
        this.container = container;
        this.logFile = logFile;
        setVisible(false);
        
        // Create log file directory if needed
        if (logFile != null) {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            // Create or clear log file
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public void log(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] " + message;
        
        // Add to TextView
        tvLog.append("\n" + logEntry);
        
        // Scroll to bottom
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
        // Write to file
        writeToFile(logEntry);
    }

    private void writeToFile(String logEntry) {
        if (logFile == null) return;
        
        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(logEntry).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Can't log this error to avoid infinite loop
            e.printStackTrace();
        }
    }
    
    public String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "No log file";
    }
    
    public void clearLogFile() {
        if (logFile != null && logFile.exists()) {
            try {
                FileWriter writer = new FileWriter(logFile, false);
                writer.write("");
                writer.close();
                log("Log file cleared");
            } catch (IOException e) {
                log("ERROR: Failed to clear log file");
            }
        }
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
