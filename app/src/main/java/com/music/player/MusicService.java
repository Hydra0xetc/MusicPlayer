package com.music.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final String TAG = "MusicService";
    public static final String ACTION_PLAY = "com.music.player.PLAY";
    public static final String ACTION_PAUSE = "com.music.player.PAUSE";
    public static final String ACTION_NEXT = "com.music.player.NEXT";
    public static final String ACTION_PREV = "com.music.player.PREV";
    public static final String ACTION_STOP = "com.music.player.STOP";

    private final IBinder binder = new MusicBinder();
    private PlayerController player;
    private Handler autoNextHandler;

    private FileLogger fileLogger;
    private List<MusicFile> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isLoopEnabled = false;
    private MusicServiceListener listener;
    private MediaSessionCompat mediaSession;
    private final float ZOOM = 1.4f;

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
        fileLogger = FileLogger.getInstance(this);

        player = new PlayerController();
        autoNextHandler = new Handler(Looper.getMainLooper());
        mediaSession = new MediaSessionCompat(this, TAG);
        createNotificationChannel();
        startAutoNextMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_STICKY;
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
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Bitmap getDefaultAlbumArt() {
        Bitmap bitmap = null;
        
        try {
            Drawable drawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
            if (drawable != null) {
                bitmap = drawableToBitmap(drawable);
                if (bitmap != null) {
                    return bitmap;
                }
            }
        } catch (Exception e) {
            fileLogger.e(TAG, "Unexpected error: " + e);
        }
        
        return null;
    }
    
    // Convert Drawable to Bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
            );
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Notification buildNotification() {
        String songTitle = "No song playing";
        MusicFile currentSong = null;
        Bitmap albumArtBitmap = null;
        
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            currentSong = playlist.get(currentIndex);
            songTitle = currentSong.getName();
            
            albumArtBitmap = BitmapCache.getInstance().getBitmapFromMemCache(currentSong.getPath());
        }
        
        if (albumArtBitmap == null) {
            albumArtBitmap = getDefaultAlbumArt();
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Action prevAction = new NotificationCompat.Action(
            R.drawable.ic_notification_prev_black,
            "Previous",
            getPendingIntent(ACTION_PREV)
        );

        NotificationCompat.Action playPauseAction;
        if (player.isPlaying()) {
            playPauseAction = new NotificationCompat.Action(
                R.drawable.ic_notification_pause_black,
                "Pause",
                getPendingIntent(ACTION_PAUSE)
            );
        } else {
            playPauseAction = new NotificationCompat.Action(
                R.drawable.ic_notification_play_black,
                "Play",
                getPendingIntent(ACTION_PLAY)
            );
        }

        NotificationCompat.Action nextAction = new NotificationCompat.Action(
            R.drawable.ic_notification_next_black,
            "Next",
            getPendingIntent(ACTION_NEXT)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText(songTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Set large icon if exist
        if (albumArtBitmap != null) {
            builder.setLargeIcon(zoomIn(albumArtBitmap, ZOOM));
        }

        return builder.build();
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

    private long getAvailableActions() {
        return PlaybackStateCompat.ACTION_PLAY |
               PlaybackStateCompat.ACTION_PAUSE |
               PlaybackStateCompat.ACTION_PLAY_PAUSE |
               PlaybackStateCompat.ACTION_STOP |
               PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
               PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
    }

    private void updatePlaybackState() {
        if (player == null) {
            return;
        }

        int state = player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(getAvailableActions())
            .setState(state, player.getCurrentPosition(), 1.0f);

        mediaSession.setPlaybackState(stateBuilder.build());
    }
    
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

        Bitmap albumArtBitmap = BitmapCache.getInstance()
            .getBitmapFromMemCache(musicFile.getPath());
        // TODO: scale the image
        if (albumArtBitmap == null) {
            byte[] albumArt = musicFile.getAlbumArt();
            if (albumArt != null) {
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                if (decodedBitmap != null) {

                    BitmapCache.getInstance()
                        .addBitmapToMemoryCache(musicFile.getPath(), decodedBitmap);

                }
            }
        }
        
        if (albumArtBitmap == null) {
            albumArtBitmap = getDefaultAlbumArt();
        }

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicFile.getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicFile.getArtist());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicFile.getAlbum());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicFile.getDuration());
        if (albumArtBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap);
        }
        mediaSession.setMetadata(metadataBuilder.build());

        startForeground(NOTIFICATION_ID, buildNotification());

        if (listener != null) {
            listener.onMusicChanged(musicFile, currentIndex);
        }
    }

    private Bitmap zoomIn(Bitmap src, float zoom) {
        int w = src.getWidth();
        int h = src.getHeight();

        int cropW = (int) (w / zoom);
        int cropH = (int) (h / zoom);

        int x = (w - cropW) / 2;
        int y = (h - cropH) / 2;

        Bitmap cropped = Bitmap.createBitmap(src, x, y, cropW, cropH);

        return Bitmap.createScaledBitmap(cropped, w, h, true);
    }

    public void play() {
        if (player.isReady()) {
            player.play();
            mediaSession.setActive(true);
            updatePlaybackState();
            updateNotification();

            if (listener != null) {
                listener.onPlayStateChanged(true);
            }
        }
    }

    public void pause() {
        if (player.isReady()) {
            player.pause();
            mediaSession.setActive(false);
            updatePlaybackState();
            updateNotification();

            if (listener != null) {
                listener.onPlayStateChanged(false);
            }
        }
    }

    public void stop() {
        if (player.isReady()) {
            player.stop();
            mediaSession.setActive(false);
            updatePlaybackState();
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
            nextIndex = 0;
        }

        loadAndPlay(nextIndex);
    }

    public void playPrevious() {
        if (playlist.isEmpty()) return;

        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1;
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

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public void seekTo(int position) {
        player.seekTo(position);
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
        mediaSession.release();
        stopForeground(true);
    }
}
