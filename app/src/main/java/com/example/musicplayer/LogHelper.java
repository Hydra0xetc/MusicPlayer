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

    public LogHelper(TextView tvLog, ScrollView scrollView, View container) {
        this.tvLog = tvLog;
        this.scrollView = scrollView;
        this.container = container;
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

    public void log(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = "[" + time + "] " + message;
        
        // Add to TextView
        tvLog.append("\n" + logEntry);
        
        // Scroll to bottom
        scrollView.post(new ScrollToBottomRunnable(scrollView));
        
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
