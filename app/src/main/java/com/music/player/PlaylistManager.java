package com.music.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistManager {

    public enum RepeatMode {
        OFF,
        ALL,
        ONE
    }

    private List<MusicFile> playlist = new ArrayList<>();
    private List<MusicFile> originalPlaylist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isShuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;

    public void setPlaylist(List<MusicFile> files) {
        this.originalPlaylist = new ArrayList<>(files);
        this.playlist = new ArrayList<>(files);
        this.currentIndex = -1; // Reset index when a new playlist is set

        if (isShuffleEnabled) {
            shufflePlaylist();
        }
    }

    public MusicFile getCurrentMusic() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public MusicFile getNextMusic() {
        if (playlist.isEmpty()) {
            return null;
        }

        int nextIndex = currentIndex;
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentMusic();
        }

        nextIndex++;

        if (nextIndex >= playlist.size()) {
            if (repeatMode == RepeatMode.ALL) {
                nextIndex = 0;
            } else {
                return null; // End of playlist
            }
        }
        
        currentIndex = nextIndex;
        return playlist.get(currentIndex);
    }
    
    public MusicFile getMusicAt(int index) {
        if (index >= 0 && index < playlist.size()) {
            currentIndex = index;
            return playlist.get(index);
        }
        return null;
    }

    public MusicFile getPreviousMusic() {
        if (playlist.isEmpty()) {
            return null;
        }

        int prevIndex = currentIndex - 1;

        if (prevIndex < 0) {
            if (repeatMode == RepeatMode.ALL) {
                prevIndex = playlist.size() - 1;
            } else {
                return null; // Start of playlist
            }
        }
        
        currentIndex = prevIndex;
        return playlist.get(currentIndex);
    }

    public void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        if (isShuffleEnabled) {
            shufflePlaylist();
        } else {
            restoreOriginalPlaylist();
        }
    }

    private void shufflePlaylist() {
        MusicFile currentMusic = getCurrentMusic();
        Collections.shuffle(playlist);
        if (currentMusic != null) {
            currentIndex = playlist.indexOf(currentMusic);
        }
    }

    private void restoreOriginalPlaylist() {
        MusicFile currentMusic = getCurrentMusic();
        playlist = new ArrayList<>(originalPlaylist);
        if (currentMusic != null) {
            currentIndex = playlist.indexOf(currentMusic);
        }
    }
    
    public void setCurrentMusic(MusicFile musicFile) {
        currentIndex = playlist.indexOf(musicFile);
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void cycleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = RepeatMode.ONE;
                break;
            case ONE:
                repeatMode = RepeatMode.OFF;
                break;
        }
    }
}
