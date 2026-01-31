package com.music.player;

import android.content.Context;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Global exception handler yang menangkap semua crash
 * dan menulis stack trace lengkap ke log.txt
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    
    private static final String TAG = "CrashHandler";
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final FileLogger logger;
    
    private CrashHandler(Context context) {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.logger = FileLogger.getInstance(context);
    }
    
    /**
     * Install crash handler sebagai default exception handler
     */
    public static void install(Context context) {
        CrashHandler handler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        
        FileLogger logger = FileLogger.getInstance(context);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            // Log crash ke file dengan format yang jelas
            logger.e(TAG, "");
            logger.e(TAG, "APLIKASI CRASH! ");
            logger.e(TAG, "Thread: " + thread.getName());
            logger.e(TAG, "Exception: " + throwable.getClass().getName());
            logger.e(TAG, "Message: " + throwable.getMessage());
            logger.e(TAG, "");
            logger.e(TAG, "Stack Trace:");
            logger.e(TAG, "────────────────────────────────────────────────────────");
            
            // Dapatkan full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // Log setiap baris stack trace
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                logger.e(TAG, line);
            }
            
            // Beri waktu untuk write ke file
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            
        } catch (Exception e) {
            // Jika logging gagal, print ke logcat
            android.util.Log.e(TAG, "Failed to log crash", e);
        } finally {
            // Panggil default handler untuk crash app seperti biasa
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(1);
            }
        }
    }
}
