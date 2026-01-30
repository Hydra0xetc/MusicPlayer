package com.music.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;
    
    public static final String ACTION_PLAY = "com.music.player.PLAY";
    public static final String ACTION_PAUSE = "com.music.player.PAUSE";
    public static final String ACTION_NEXT = "com.music.player.NEXT";
    public static final String ACTION_PREV = "com.music.player.PREV";
    public static final String ACTION_STOP = "com.music.player.STOP";
    
    private final IBinder binder = new MusicBinder();
    private PlayerController player;
    private Handler autoNextHandler;
    
    private List<MusicFile> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isLoopEnabled = false;
    private MusicServiceListener listener;
    
    public interface MusicServiceListener {
        void onMusicChanged(MusicFile musicFile, int index);
        void onPlayStateChanged(boolean isPlaying);
        void onMusicFinished();
    }
    
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        player = new PlayerController();
        autoNextHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startAutoNextMonitoring();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_STICKY; // Service akan restart jika di-kill oleh system
    }
    
    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                play();
                break;
            case ACTION_PAUSE:
                pause();
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PREV:
                playPrevious();
                break;
            case ACTION_STOP:
                stop();
                break;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");
            channel.setSound(null, null); // No sound untuk notification
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification buildNotification() {
        String songTitle = "No song playing";
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            songTitle = playlist.get(currentIndex).getName();
        }
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Action buttons
        NotificationCompat.Action prevAction = new NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            getPendingIntent(ACTION_PREV)
        );
        
        NotificationCompat.Action playPauseAction;
        if (player.isPlaying()) {
            playPauseAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                getPendingIntent(ACTION_PAUSE)
            );
        } else {
            playPauseAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                getPendingIntent(ACTION_PLAY)
            );
        }
        
        NotificationCompat.Action nextAction = new NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            getPendingIntent(ACTION_NEXT)
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText(songTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(true) // Membuat notifikasi tidak bisa di-swipe
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();
    }
    
    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            this, 
            action.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private void updateNotification() {
        Notification notification = buildNotification();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    // Public methods untuk kontrol dari Activity
    public void setPlaylist(List<MusicFile> files) {
        this.playlist = new ArrayList<>(files);
    }
    
    public void loadAndPlay(MusicFile musicFile) {
        currentIndex = playlist.indexOf(musicFile);
        if (currentIndex != -1) {
            loadMusic(musicFile);
            play();
        }
    }
    
    public void loadAndPlay(int index) {
        if (index >= 0 && index < playlist.size()) {
            currentIndex = index;
            loadMusic(playlist.get(index));
            play();
        }
    }
    
    private void loadMusic(MusicFile musicFile) {
        player.load(musicFile.getPath());
        startForeground(NOTIFICATION_ID, buildNotification());
        
        if (listener != null) {
            listener.onMusicChanged(musicFile, currentIndex);
        }
    }
    
    public void play() {
        if (player.isReady()) {
            player.play();
            updateNotification();
            
            if (listener != null) {
                listener.onPlayStateChanged(true);
            }
        }
    }
    
    public void pause() {
        if (player.isReady()) {
            player.pause();
            updateNotification();
            
            if (listener != null) {
                listener.onPlayStateChanged(false);
            }
        }
    }
    
    public void stop() {
        if (player.isReady()) {
            player.stop();
            updateNotification();
            
            if (listener != null) {
                listener.onPlayStateChanged(false);
            }
        }
    }
    
    public void playNext() {
        if (playlist.isEmpty()) return;
        
        int nextIndex = currentIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0; // Kembali ke awal
        }
        
        loadAndPlay(nextIndex);
    }
    
    public void playPrevious() {
        if (playlist.isEmpty()) return;
        
        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1; // Ke akhir
        }
        
        loadAndPlay(prevIndex);
    }
    
    public void togglePlayPause() {
        if (player.isPlaying()) {
            pause();
        } else {
            play();
        }
    }
    
    public void setLoop(boolean enabled) {
        isLoopEnabled = enabled;
        if (player.isReady()) {
            player.setLoop(enabled);
        }
    }
    
    public boolean isLoopEnabled() {
        return isLoopEnabled;
    }
    
    public boolean isPlaying() {
        return player.isPlaying();
    }
    
    public boolean isReady() {
        return player.isReady();
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
    
    public void setListener(MusicServiceListener listener) {
        this.listener = listener;
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
        if (!player.isReady()) {
            return;
        }
        
        if (player.isFinished() && !player.isPlaying()) {
            if (listener != null) {
                listener.onMusicFinished();
            }
            
            // Jika loop tidak aktif, play next
            if (!isLoopEnabled) {
                playNext();
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        autoNextHandler.removeCallbacksAndMessages(null);
        player.release();
        stopForeground(true);
    }
}
