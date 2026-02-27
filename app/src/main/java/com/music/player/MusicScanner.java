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

    public static List<MusicFile> scanDirectory(Context context, String dirPath) {
        FileLogger fileLogger = FileLogger.getInstance(context);
        MusicMetadataCache metaCache = MusicMetadataCache.getInstance(context);
        AlbumArtManager artManager = AlbumArtManager.getInstance(context);

        List<MusicFile> musicFiles = new ArrayList<>();

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            fileLogger.w(TAG, "Directory not found or not a directory: " + dirPath);
            return musicFiles;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            fileLogger.w(TAG, "Could not list files in directory: " + dirPath);
            return musicFiles;
        }

        List<String> currentPaths = new ArrayList<>();

        for (File file : files) {
            if (!file.isFile() || !isAudioFile(file.getName())) {
                continue;
            }

            String path = file.getAbsolutePath();
            long fileSize = file.length();
            long lastModified = file.lastModified();

            currentPaths.add(path);

            MusicFile cached = metaCache.getCached(path, fileSize, lastModified);
            if (cached != null) {
                musicFiles.add(cached);
                continue; // cache hit — skip MMR entirely
            }
            MusicFile fresh = extractMetadata(context, file, artManager);
            if (fresh != null) {
                boolean hasArt = artManager.hasAlbumArt(path);
                metaCache.putCache(fresh, lastModified, hasArt);
                musicFiles.add(fresh);
            }
        }

        metaCache.removeStaleEntries(currentPaths);
        artManager.removeStaleArt(currentPaths);

        Collections.sort(musicFiles, new MusicComparator());
        return musicFiles;
    }

    private static MusicFile extractMetadata(Context context, File file, AlbumArtManager artManager) {
        FileLogger fileLogger = FileLogger.getInstance(context);

        String title = file.getName();
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        long duration = 0;

        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {

            // Fresh instance per file: avoids stale values from previous file
            mmr.setDataSource(context, Uri.fromFile(file));

            String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (t != null && !t.isEmpty()) {
                title = t;
            }

            String ar = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (ar != null && !ar.isEmpty()) {
                artist = cleanupArtistString(ar);
            }

            String al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (al != null && !al.isEmpty()) {
                album = al;
            }

            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null && !dur.isEmpty()) {
                try {
                    duration = Long.parseLong(dur);
                } catch (NumberFormatException e) {
                    fileLogger.e(TAG, "NumberFormatException error: " + e);
                }
            }

            if (!artManager.hasAlbumArt(file.getAbsolutePath())) {
                byte[] artBytes = mmr.getEmbeddedPicture();
                if (artBytes != null) {
                    artManager.saveAlbumArt(file.getAbsolutePath(), artBytes);
                }
            }
        } catch (Exception e) {
            fileLogger.e(TAG, "Failed to get metadata for: " + file.getName() + ": " + e.getMessage());
        }

        return new MusicFile(
                file.getName(),
                file.getAbsolutePath(),
                file.length(),
                title, artist, album, duration);
    }

    public static void scanDirectoryAsync(Context context, String dirPath, ScanListener listener) {
        new Thread(() -> {
            try {
                if (listener != null) {
                    listener.onScanStarted();
                }
                List<MusicFile> files = scanDirectory(context, dirPath);
                if (listener != null) {
                    listener.onScanCompleted(files);
                }
            } catch (Exception e) {
                FileLogger.getInstance(context).e(TAG, "Unexpected error: " + e);
                if (listener != null) {
                    listener.onScanError(e.toString());
                }
            }
        }).start();
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanupArtistString(String s) {
        if (s == null) {
            return "Unknown Artist";
        }
        String[] parts = s.split(",\\s*");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (!out.toString().contains(p)) {
                if (out.length() > 0) {
                    out.append(", ");
                }
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
