package com.music.player;

public class MusicFile {
    private String name;
    private String path;
    private long size;
    private String title;
    private String artist;
    private String album;
    private long duration;

    public MusicFile(String name, String path, long size, String title, String artist, String album, long duration) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getSizeFormatted() {
        if (size < Constant.ONE_KB) {
            return size + " B";
        } else if (size < Constant.ONE_MB) {
            return String.format("%.1f KB", size / Constant.ONE_KB);
        } else {
            return String.format("%.1f MB", size / Constant.ONE_MB);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MusicFile that = (MusicFile) o;
        return path != null ? path.equals(that.path) : that.path == null;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return (artist != null && !artist.isEmpty() ? artist + " - " : Constant.EMPTY_STRING) +
                (title != null && !title.isEmpty() ? title : name);
    }
}
