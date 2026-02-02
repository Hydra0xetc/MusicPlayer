package com.music.player;

import android.app.Application;

/**
 * Application class for global initialization
 * This will be executed FIRST before any Activity
 */
public class MusicPlayerApp extends Application {
    
    private static final String TAG = "MusicPlayerApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Install FileLogger and CrashHandler FIRST
        FileLogger logger = FileLogger.getInstance(this);
        
        logger.i(TAG, "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        logger.i(TAG, "Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        logger.i(TAG, "Package: " + getPackageName());
        logger.i(TAG, "Log path: " + logger.getLogPath());
        
        CrashHandler.install(this);
    }
}
