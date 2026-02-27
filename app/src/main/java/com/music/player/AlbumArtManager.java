package com.music.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public class AlbumArtManager {

    private static final String TAG = "AlbumArtManager";
    private static final String ART_DIR = "albumart";
    private static final int MAX_ART_PX = 256; // Max dimension when saving, saves disk space

    private static AlbumArtManager instance;
    private final File artDir;
    private final FileLogger fileLogger;

    private AlbumArtManager(Context context) {
        artDir = new File(context.getFilesDir(), ART_DIR);
        if (!artDir.exists()) {
            artDir.mkdirs();
        }
        fileLogger = FileLogger.getInstance(context);
    }

    public static synchronized AlbumArtManager getInstance(Context context) {
        if (instance == null) {
            instance = new AlbumArtManager(context.getApplicationContext());
        }
        return instance;
    }

    public File getAlbumArtFile(String musicPath) {
        String hash = Integer.toHexString(musicPath.hashCode());
        return new File(artDir, hash + ".png");
    }

    public boolean hasAlbumArt(String musicPath) {
        return getAlbumArtFile(musicPath).exists();
    }

    public boolean saveAlbumArt(String musicPath, byte[] artBytes) {
        if (artBytes == null || artBytes.length == 0)
            return false;

        File out = getAlbumArtFile(musicPath);
        FileOutputStream fos = null;
        try {
            // Decode with inJustDecodeBounds first to compute sample size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length, opts);

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, MAX_ART_PX, MAX_ART_PX);

            Bitmap bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length, opts);
            if (bitmap == null)
                return false;

            fos = new FileOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            bitmap.recycle();
            return true;
        } catch (Exception e) {
            fileLogger.e(TAG, "saveAlbumArt error for " + musicPath + ": " + e.getMessage());
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public Bitmap loadAlbumArt(String musicPath) {
        File artFile = getAlbumArtFile(musicPath);
        if (!artFile.exists())
            return null;
        try {
            return BitmapFactory.decodeFile(artFile.getAbsolutePath());
        } catch (Exception e) {
            fileLogger.e(TAG, "loadAlbumArt error for " + musicPath + ": " + e.getMessage());
            return null;
        }
    }

    public void deleteAlbumArt(String musicPath) {
        File f = getAlbumArtFile(musicPath);
        if (f.exists())
            f.delete();
    }

    public void removeStaleArt(java.util.List<String> currentPaths) {
        java.util.Set<String> validHashes = new java.util.HashSet<>();
        for (String p : currentPaths) {
            validHashes.add(Integer.toHexString(p.hashCode()) + ".png");
        }
        File[] files = artDir.listFiles();
        if (files == null)
            return;
        int removed = 0;
        for (File f : files) {
            if (!validHashes.contains(f.getName())) {
                f.delete();
                removed++;
            }
        }
        if (removed > 0) {
            fileLogger.w(TAG, "Removed " + removed + " stale album art files");
        }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
