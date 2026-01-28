package com.example.musicplayer;

import android.widget.Button;
import android.widget.Toast;

public class ErrorTask implements Runnable {
    private MainActivity activity;
    private Button btnScan;
    private LogHelper logger;
    private String error;

    public ErrorTask(
        MainActivity activity,
        Button btnScan,
        LogHelper logger,
        String error
) {
        this.activity = activity;
        this.btnScan = btnScan;
        this.logger = logger;
        this.error = error;
    }

    public void run() {
        btnScan.setEnabled(true);
        btnScan.setText("Ready");
        logger.log("ERROR: Scan failed - " + error);
        Toast.makeText(activity, "Scan failed: " + error, Toast.LENGTH_SHORT).show();
    }
}
