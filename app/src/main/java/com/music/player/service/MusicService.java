package com.music.player.service;
import com.music.player.R;

import com.music.player.model.*;
import com.music.player.manager.*;
import com.music.player.player.*;
import com.music.player.utils.*;
import com.music.player.scanner.*;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.List;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    public static final String ACTION_PLAY = "com.music.player.PLAY";
    public static final String ACTION_PAUSE = "com.music.player.PAUSE";
    public static final String ACTION_NEXT = "com.music.player.NEXT";
    public static final String ACTION_PREV = "com.music.player.PREV";
    public static final String ACTION_STOP = "com.music.player.STOP";
    public static final String ACTION_SEEK = "com.music.player.SEEK";
    public static final String EXTRA_SEEK_POSITION = "seek_position";

    private final IBinder binder = new MusicBinder();
    private PlayerController player;
    private Handler autoNextHandler;
    private Handler notificationUpdateHandler;

    private FileLogger fileLogger;
    private PlaylistManager playlistManager;
    private MusicFile currentPlayingMusic = null;
    private MusicServiceListener listener;
    private MediaNotificationManager notificationManager;

    public interface MusicServiceListener {
        void onMusicChanged(MusicFile musicFile, int index);
        void onPlayStateChanged(boolean isPlaying);
        void onMusicFinished();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fileLogger = FileLogger.getInstance(this);
        player = new PlayerController();
        playlistManager = new PlaylistManager();
        autoNextHandler = new Handler(Looper.getMainLooper());
        notificationUpdateHandler = new Handler(Looper.getMainLooper());
        notificationManager = new MediaNotificationManager(this);

        startAutoNextMonitoring();
        startNotificationUpdater();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction(), intent);
        }
        return START_STICKY;
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_PLAY: play(); break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_NEXT: playNext(); break;
            case ACTION_PREV: playPrevious(); break;
            case ACTION_STOP: stop(); break;
            case ACTION_SEEK:
                if (intent.hasExtra(EXTRA_SEEK_POSITION)) {
                    seekTo(intent.getIntExtra(EXTRA_SEEK_POSITION, 0));
                }
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    private void startNotificationUpdater() {
        notificationUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    notificationManager.updatePlaybackState(true, player.getCurrentPosition());
                    notificationManager.updateNotification(currentPlayingMusic, true);
                }
                notificationUpdateHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void setPlaylist(List<MusicFile> files) {
        playlistManager.setPlaylist(files);
        if (currentPlayingMusic != null) playlistManager.setCurrentMusic(currentPlayingMusic);
    }

    public void loadAndPlay(MusicFile musicFile) {
        playlistManager.setCurrentMusic(musicFile);
        if (playlistManager.getCurrentIndex() != -1) {
            loadMusic(musicFile);
            play();
        }
    }

    private void loadMusic(MusicFile musicFile) {
        currentPlayingMusic = musicFile;
        player.load(musicFile.getPath());
        syncPlayerLoopMode();

        notificationManager.updateMetadata(musicFile);
        startForeground(MediaNotificationManager.NOTIFICATION_ID, 
                notificationManager.buildNotification(musicFile, true));

        if (listener != null) {
            listener.onMusicChanged(musicFile, playlistManager.getCurrentIndex());
        }
    }

    public void play() {
        if (player.isReady()) {
            player.play();
            notificationManager.updatePlaybackState(true, player.getCurrentPosition());
            notificationManager.updateNotification(currentPlayingMusic, true);
            if (listener != null) listener.onPlayStateChanged(true);
        }
    }

    public void pause() {
        if (player.isReady()) {
            player.pause();
            notificationManager.updatePlaybackState(false, player.getCurrentPosition());
            notificationManager.updateNotification(currentPlayingMusic, false);
            if (listener != null) listener.onPlayStateChanged(false);
        }
    }

    public void stop() {
        if (player.isReady()) {
            player.stop();
            notificationManager.updatePlaybackState(false, 0);
            notificationManager.updateNotification(currentPlayingMusic, false);
            if (listener != null) listener.onPlayStateChanged(false);
        }
    }

    public void playNext() {
        MusicFile next = playlistManager.getNextMusic();
        if (next != null) loadAndPlay(next);
    }

    public void playPrevious() {
        MusicFile prev = playlistManager.getPreviousMusic();
        if (prev != null) loadAndPlay(prev);
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    private void syncPlayerLoopMode() {
        if (player.isReady()) {
            player.setLoop(playlistManager.getRepeatMode() == PlaylistManager.RepeatMode.ONE);
        }
    }

    public void toggleShuffle() {
        playlistManager.toggleShuffle();
        if (currentPlayingMusic != null) playlistManager.setCurrentMusic(currentPlayingMusic);
    }

    public void cycleRepeatMode() {
        playlistManager.cycleRepeatMode();
        syncPlayerLoopMode();
    }

    public boolean isShuffleEnabled() { return playlistManager.isShuffleEnabled(); }
    public PlaylistManager.RepeatMode getRepeatMode() { return playlistManager.getRepeatMode(); }
    public boolean isPlaying() { return player.isPlaying(); }
    public boolean isReady() { return player.isReady(); }
    public MusicFile getCurrentMusic() { return currentPlayingMusic; }
    public int getCurrentIndex() { return playlistManager.getCurrentIndex(); }
    public long getCurrentPosition() { return player.getCurrentPosition(); }
    public void setListener(MusicServiceListener listener) { this.listener = listener; }

    public void seekTo(int position) {
        player.seekTo(position);
        notificationManager.updatePlaybackState(player.isPlaying(), position);
        notificationManager.updateNotification(currentPlayingMusic, player.isPlaying());
    }

    private void startAutoNextMonitoring() {
        autoNextHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndPlayNext();
                autoNextHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void checkAndPlayNext() {
        if (!player.isReady() || player.isPlaying() || !player.isFinished()) return;
        if (listener != null) listener.onMusicFinished();
        if (playlistManager.getRepeatMode() == PlaylistManager.RepeatMode.ALL) playNext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        autoNextHandler.removeCallbacksAndMessages(null);
        notificationUpdateHandler.removeCallbacksAndMessages(null);
        player.release();
        notificationManager.release();
        stopForeground(true);
    }
}
