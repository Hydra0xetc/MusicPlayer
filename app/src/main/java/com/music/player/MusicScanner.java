package com.music.player;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
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

    public static List<MusicFile> scanDirectory(Context context, String dirPath) {
        FileLogger fileLogger = FileLogger.getInstance(context);
        final String TAG = "MusicScanner";

        List<MusicFile> musicFiles = new ArrayList<MusicFile>();
        
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            fileLogger.e(TAG, "Directory not found: " + dirPath);
            return musicFiles;
        }
        
        if (!dir.isDirectory()) {
            fileLogger.e(TAG, "Path is not a directory: " + dirPath);
            return musicFiles;
        }
        
        File[] files = dir.listFiles();
        
        if (files == null) {
            fileLogger.e(TAG, "No files found in directory: " + dirPath);
            return musicFiles;
        }
        
        MediaMetadataRetriever mmr = null; // Declare outside try block for finally
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile() && isAudioFile(file.getName())) {
                String title = file.getName(); // Default to filename
                String artist = "Unknown Artist";
                String album = "Unknown Album";
                long duration = 0; // milliseconds
                byte[] albumArt = null;

                try {
                    mmr = new MediaMetadataRetriever(); // Create a new instance for EACH file
                    mmr.setDataSource(context, Uri.fromFile(file));

                    String retrievedTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (retrievedTitle != null && !retrievedTitle.isEmpty()) {
                        title = retrievedTitle;
                    }

                    String retrievedArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    if (retrievedArtist != null && !retrievedArtist.isEmpty()) {
                        artist = retrievedArtist;
                    }

                    String retrievedAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    if (retrievedAlbum != null && !retrievedAlbum.isEmpty()) {
                        album = retrievedAlbum;
                    }

                    String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durationStr != null && !durationStr.isEmpty()) {
                        duration = Long.parseLong(durationStr);
                    }

                    albumArt = mmr.getEmbeddedPicture();

                } catch (Exception e) {
                    fileLogger.e(TAG, "Error processing media file " + file.getAbsolutePath(), e);
                } finally {
                    if (mmr != null) {
                        try {
                            mmr.release(); // Release this specific instance
                        } catch (Exception releaseException) {
                            fileLogger.e(TAG, "Error releasing MediaMetadataRetriever for " + file.getAbsolutePath(), releaseException);
                        }
                    }
                }

                musicFiles.add(new MusicFile(
                    file.getName(),
                    file.getAbsolutePath(),
                    file.length(),
                    title,
                    artist,
                    album,
                    duration,
                    albumArt
                ));
            }
        }

        // Sort alphabetically
        Collections.sort(musicFiles, new MusicFileComparator());
        
        return musicFiles;
    }

    public static void scanDirectoryAsync(Context context, String dirPath, ScanListener listener) {
        ScanThread thread = new ScanThread(context, dirPath, listener);
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
        private Context context;

        public ScanThread(Context context, String dirPath, ScanListener listener) {
            this.context = context;
            this.dirPath = dirPath;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                if (listener != null) {
                    listener.onScanStarted();
                }
                
                List<MusicFile> files = scanDirectory(context, dirPath);
                
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
