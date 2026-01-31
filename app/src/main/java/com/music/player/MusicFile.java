package com.music.player;

import java.io.File;

public class MusicFile {
    private String name;
    private String path;
    private long size;
    private String title;
    private String artist;
    private String album;
    private long duration; // in milliseconds
    private byte[] albumArt; // raw byte data for album art

    // Primary constructor with full metadata
    public MusicFile(
        String name,
        String path,
        long size,
        String title,
        String artist,
        String album,
        long duration,
        byte[] albumArt
) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.albumArt = albumArt;
    }

    // Secondary constructor for backward compatibility or when metadata is not yet available
    public MusicFile(String name, String path, long size) {
        this(name, path, size, name, "Unknown Artist", "Unknown Album", 0, null);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getSizeFormatted() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public String getDurationFormatted() {
        long minutes = (duration / 1000) / 60;
        long seconds = (duration / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public byte[] getAlbumArt() {
        return albumArt;
    }

    @Override
    public String toString() {
        return (artist != null && !artist.isEmpty() ? artist + " - " : "") +
               (title != null && !title.isEmpty() ? title : name);
    }
}
