package com.example.musicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MusicScanner {

    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".wav", ".ogg", ".m4a", ".aac", 
        ".flac", ".wma", ".opus", ".3gp"
    };

    public interface ScanListener {
        void onScanStarted();
        void onFileFound(MusicFile file);
        void onScanCompleted(List<MusicFile> files);
        void onScanError(String error);
    }

    public static List<MusicFile> scanDirectory(String dirPath) {
        List<MusicFile> musicFiles = new ArrayList<MusicFile>();
        
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            return musicFiles;
        }
        
        if (!dir.isDirectory()) {
            return musicFiles;
        }
        
        File[] files = dir.listFiles();
        
        if (files == null) {
            return musicFiles;
        }
        
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile() && isAudioFile(file.getName())) {
                musicFiles.add(new MusicFile(
                    file.getName(),
                    file.getAbsolutePath(),
                    file.length()
                ));
            }
        }
        
        // Sort alphabetically
        Collections.sort(musicFiles, new MusicFileComparator());
        
        return musicFiles;
    }

    public static void scanDirectoryAsync(String dirPath, ScanListener listener) {
        ScanThread thread = new ScanThread(dirPath, listener);
        thread.start();
    }

    private static boolean isAudioFile(String filename) {
        String lowerName = filename.toLowerCase();
        for (int i = 0; i < AUDIO_EXTENSIONS.length; i++) {
            if (lowerName.endsWith(AUDIO_EXTENSIONS[i])) {
                return true;
            }
        }
        return false;
    }

    public static String[] getSupportedExtensions() {
        String[] copy = new String[AUDIO_EXTENSIONS.length];
        System.arraycopy(AUDIO_EXTENSIONS, 0, copy, 0, AUDIO_EXTENSIONS.length);
        return copy;
    }

    // Named inner class instead of anonymous Comparator
    private static class MusicFileComparator implements Comparator<MusicFile> {
        @Override
        public int compare(MusicFile f1, MusicFile f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    // Named inner class instead of anonymous Runnable
    private static class ScanThread extends Thread {
        private String dirPath;
        private ScanListener listener;

        public ScanThread(String dirPath, ScanListener listener) {
            this.dirPath = dirPath;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                if (listener != null) {
                    listener.onScanStarted();
                }
                
                List<MusicFile> files = scanDirectory(dirPath);
                
                if (listener != null) {
                    for (int i = 0; i < files.size(); i++) {
                        listener.onFileFound(files.get(i));
                    }
                    listener.onScanCompleted(files);
                }
            } catch (Exception e) {
                if (listener != null) {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null) {
                        errorMsg = "Unknown error";
                    }
                    listener.onScanError(errorMsg);
                }
            }
        }
    }
}
