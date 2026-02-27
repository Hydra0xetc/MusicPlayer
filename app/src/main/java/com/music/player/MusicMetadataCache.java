package com.music.player;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MusicMetadataCache {

    private static final String TAG = "MusicMetadataCache";
    private static final String DB_NAME = "music_metadata_cache.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "music_cache";
    private static final String COL_PATH = "path";
    private static final String COL_FILE_SIZE = "file_size";
    private static final String COL_LAST_MODIFIED = "last_modified";
    private static final String COL_TITLE = "title";
    private static final String COL_ARTIST = "artist";
    private static final String COL_ALBUM = "album";
    private static final String COL_DURATION = "duration";
    private static final String COL_HAS_ART = "has_album_art";

    private static MusicMetadataCache instance;
    private final DbHelper dbHelper;
    private final FileLogger logger;

    private MusicMetadataCache(Context context) {
        dbHelper = new DbHelper(context.getApplicationContext());
        logger = FileLogger.getInstance(context);
    }

    public static synchronized MusicMetadataCache getInstance(Context context) {
        if (instance == null) {
            instance = new MusicMetadataCache(context);
        }
        return instance;
    }

    public MusicFile getCached(String path, long fileSize, long lastModified) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE,
                    null,
                    COL_PATH + "=? AND " + COL_FILE_SIZE + "=? AND " + COL_LAST_MODIFIED + "=?",
                    new String[] { path, String.valueOf(fileSize), String.valueOf(lastModified) },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALBUM));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION));
                String fileName = new java.io.File(path).getName();

                return new MusicFile(fileName, path, fileSize, title, artist, album, duration);
            }
        } catch (Exception e) {
            logger.e(TAG, "getCached error for " + path + ": " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public void putCache(MusicFile music, long lastModified, boolean hasAlbumArt) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_PATH, music.getPath());
            cv.put(COL_FILE_SIZE, music.getSize());
            cv.put(COL_LAST_MODIFIED, lastModified);
            cv.put(COL_TITLE, music.getTitle());
            cv.put(COL_ARTIST, music.getArtist());
            cv.put(COL_ALBUM, music.getAlbum());
            cv.put(COL_DURATION, music.getDuration());
            cv.put(COL_HAS_ART, hasAlbumArt ? 1 : 0);
            db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            logger.e(TAG, "putCache error for " + music.getPath() + ": " + e.getMessage());
        }
    }

    public boolean cachedHasAlbumArt(String path) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE,
                    new String[] { COL_HAS_ART },
                    COL_PATH + "=?",
                    new String[] { path },
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0) == 1;
            }
        } catch (Exception e) {
            logger.e(TAG, "cachedHasAlbumArt error: " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }

    public void removeStaleEntries(List<String> currentPaths) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE, new String[] { COL_PATH }, null, null, null, null, null);

            List<String> toDelete = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String cachedPath = cursor.getString(0);
                    if (!currentPaths.contains(cachedPath)) {
                        toDelete.add(cachedPath);
                    }
                }
            }
            cursor.close();
            cursor = null;

            if (!toDelete.isEmpty()) {
                db = dbHelper.getWritableDatabase();
                for (String p : toDelete) {
                    db.delete(TABLE, COL_PATH + "=?", new String[] { p });
                }
                logger.w(TAG, "Removed " + toDelete.size() + " stale cache entries");
            }
        } catch (Exception e) {
            logger.e(TAG, "removeStaleEntries error: " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private static class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                            COL_PATH + " TEXT PRIMARY KEY, " +
                            COL_FILE_SIZE + " INTEGER NOT NULL, " +
                            COL_LAST_MODIFIED + " INTEGER NOT NULL, " +
                            COL_TITLE + " TEXT, " +
                            COL_ARTIST + " TEXT, " +
                            COL_ALBUM + " TEXT, " +
                            COL_DURATION + " INTEGER DEFAULT 0, " +
                            COL_HAS_ART + " INTEGER DEFAULT 0" +
                            ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }
}
