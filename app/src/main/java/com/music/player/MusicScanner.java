package com.music.player;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MusicScanner {

    public static final String[] AUDIO_EXTENSIONS = {
            ".mp3", ".wav", ".ogg", ".m4a", ".aac",
            ".flac", ".wma", ".opus", ".3gp"
    };
    private static final String TAG = "MusicScanner";

    public interface ScanListener {
        void onScanStarted();
        void onFileFound(MusicFile file);
        void onScanCompleted(List<MusicFile> files);
        void onScanError(String error);
    }

    public static List<MusicFile> scanDirectory(Context context, String dirPath, boolean loadAlbumArt) {

        FileLogger fileLogger = FileLogger.getInstance(context);
        List<MusicFile> musicFiles = new ArrayList<>();

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return musicFiles;

        File[] files = dir.listFiles();
        if (files == null) return musicFiles;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        for (File file : files) {

            if (!file.isFile() || !isAudioFile(file.getName())) continue;

            String title = file.getName();
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            long duration = 0;
            byte[] albumArt = null;

            try {
                mmr.setDataSource(context, Uri.fromFile(file));

                String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if (t != null) title = t;

                String a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (a != null) artist = cleanupArtistString(a);

                String al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (al != null) album = al;

                String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (d != null) duration = Long.parseLong(d);

                if (loadAlbumArt) {
                    albumArt = mmr.getEmbeddedPicture();
                }

            } catch (Exception ignored) {}

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

        try {
            mmr.release();
        } catch (Exception e) {
            fileLogger.e(TAG, "Unexpected error: " + e);
        }

        Collections.sort(musicFiles, new MusicComparator());

        return musicFiles;
    }
    
    public static List<MusicFile> scanDirectory(Context context, String dirPath) {
        return scanDirectory(context, dirPath, true);
    }

    public static void scanDirectoryAsync(
            Context context,
            String dirPath,
            ScanListener listener
    ) {
        new Thread(() -> {
            try {
                if (listener != null) listener.onScanStarted();
                
                List<MusicFile> files = scanDirectory(context, dirPath, true);
                
                if (listener != null) listener.onScanCompleted(files);
            } catch (Exception e) {
                if (listener != null) listener.onScanError(e.toString());
            }
        }).start();
    }
    
    public static byte[] loadAlbumArt(Context context, String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        byte[] albumArt = null;
        
        try {
            mmr.setDataSource(context, Uri.fromFile(new File(filePath)));
            albumArt = mmr.getEmbeddedPicture();
        } catch (Exception e) {
            FileLogger fileLogger = FileLogger.getInstance(context);
            fileLogger.e(TAG, "Unexpected error: " + e);
        } finally {
            try {
                mmr.release();
            } catch (Exception e) {
                FileLogger fileLogger = FileLogger.getInstance(context);
                fileLogger.e(TAG, "Unexpected error: " + e);
            }
        }
        
        return albumArt;
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private static String cleanupArtistString(String s) {
        if (s == null) return "Unknown Artist";
        String[] parts = s.split(",\\s*");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (!out.toString().contains(p)) {
                if (out.length() > 0) out.append(", ");
                out.append(p);
            }
        }
        return out.toString();
    }

    private static class MusicComparator implements Comparator<MusicFile> {
        @Override
        public int compare(MusicFile a, MusicFile b) {
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        }
    }
}
