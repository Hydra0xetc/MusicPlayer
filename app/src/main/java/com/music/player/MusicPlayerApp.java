package com.music.player;

import android.app.Application;

/**
 * Application class untuk inisialisasi global
 * Ini akan dijalankan PERTAMA sebelum Activity apapun
 */
public class MusicPlayerApp extends Application {
    
    private static final String TAG = "MusicPlayerApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Install FileLogger dan CrashHandler PERTAMA KALI
        FileLogger logger = FileLogger.getInstance(this);
        
        logger.i(TAG, "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        logger.i(TAG, "Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        logger.i(TAG, "Package: " + getPackageName());
        logger.i(TAG, "Log path: " + logger.getLogPath());
        
        // Install CrashHandler untuk menangkap SEMUA crash
        CrashHandler.install(this);
    }
}
