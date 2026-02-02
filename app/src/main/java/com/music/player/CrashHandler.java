package com.music.player;

import android.content.Context;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Global exception handler that catches all crashes
 * and writes the full stack trace to log.txt
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
     * Install crash handler as the default exception handler
     */
    public static void install(Context context) {
        CrashHandler handler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        
        FileLogger logger = FileLogger.getInstance(context);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            // Log crash to file with clear format
            logger.e(TAG, "");
            logger.e(TAG, "APPLICATION CRASH! ");
            logger.e(TAG, "Thread: " + thread.getName());
            logger.e(TAG, "Exception: " + throwable.getClass().getName());
            logger.e(TAG, "Message: " + throwable.getMessage());
            logger.e(TAG, "");
            logger.e(TAG, "Stack Trace:");
            logger.e(TAG, "────────────────────────────────────────────────────────");
            
            // Get full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // Log each line of the stack trace
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                logger.e(TAG, line);
            }
            
            // Allow time to write to file
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            
        } catch (Exception e) {
            // If logging fails, print to logcat
            android.util.Log.e(TAG, "Failed to log crash", e);
        } finally {
            // Call the default handler to crash the app as usual
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(1);
            }
        }
    }
}
