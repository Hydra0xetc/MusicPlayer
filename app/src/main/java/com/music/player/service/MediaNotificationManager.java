package com.music.player.service;

import com.music.player.R;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.music.player.ui.MainActivity;
import com.music.player.model.MusicFile;
import com.music.player.manager.AlbumArtManager;
import com.music.player.utils.BitmapCache;
import com.music.player.utils.Constant;
import com.music.player.R;

public class MediaNotificationManager {
    public static final String CHANNEL_ID = "MusicPlayerChannel";
    public static final int NOTIFICATION_ID = 1;

    private final MusicService service;
    private final NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;

    public MediaNotificationManager(MusicService service) {
        this.service = service;
        this.notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        setupMediaSession();
        createNotificationChannel();
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(service, "MusicService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { service.play(); }
            @Override
            public void onPause() { service.pause(); }
            @Override
            public void onSkipToNext() { service.playNext(); }
            @Override
            public void onSkipToPrevious() { service.playPrevious(); }
            @Override
            public void onStop() { service.stop(); }
            @Override
            public void onSeekTo(long pos) { service.seekTo((int) pos); }
        });
        mediaSession.setActive(true);
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void updateNotification(MusicFile music, boolean isPlaying) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(music, isPlaying));
    }

    public Notification buildNotification(MusicFile music, boolean isPlaying) {
        String title = music != null ? music.getTitle() : Constant.NO_SONG;
        String artist = music != null ? music.getArtist() : Constant.EMPTY_STRING;

        Bitmap art = null;
        if (music != null) {
            art = BitmapCache.getInstance().getBitmapFromMemCache(music.getPath());
            if (art == null) {
                art = AlbumArtManager.getInstance(service).loadAlbumArt(music.getPath());
                if (art != null) BitmapCache.getInstance().addBitmapToMemoryCache(music.getPath(), art);
            }
        }
        if (art == null) art = getDefaultAlbumArt();

        Intent intent = new Intent(service, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentIntent)
                .setLargeIcon(art != null ? zoomIn(art, Constant.NOTIFICATION_IMG_ZOOM) : null)
                .addAction(R.drawable.ic_notification_prev_black, "Prev", getActionIntent(MusicService.ACTION_PREV))
                .addAction(isPlaying ? R.drawable.ic_notification_pause_black : R.drawable.ic_notification_play_black,
                        isPlaying ? "Pause" : "Play", getActionIntent(isPlaying ? MusicService.ACTION_PAUSE : MusicService.ACTION_PLAY))
                .addAction(R.drawable.ic_notification_next_black, "Next", getActionIntent(MusicService.ACTION_NEXT))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying);

        return builder.build();
    }

    public void updateMetadata(MusicFile music) {
        if (music == null) return;
        Bitmap art = BitmapCache.getInstance().getBitmapFromMemCache(music.getPath());
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, music.getAlbum())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, music.getDuration());
        if (art != null) builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
        mediaSession.setMetadata(builder.build());
    }

    public void updatePlaybackState(boolean isPlaying, long position) {
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                           PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                           PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, position, 1.0f).build());
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(service, MusicService.class).setAction(action);
        return PendingIntent.getService(service, action.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Bitmap getDefaultAlbumArt() {
        Drawable d = ContextCompat.getDrawable(service, R.mipmap.ic_launcher);
        if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
        if (d == null) return null;
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        return b;
    }

    private Bitmap zoomIn(Bitmap src, float zoom) {
        int w = src.getWidth(), h = src.getHeight();
        int cw = (int) (w / zoom), ch = (int) (h / zoom);
        Bitmap cropped = Bitmap.createBitmap(src, (w - cw) / 2, (h - ch) / 2, cw, ch);
        return Bitmap.createScaledBitmap(cropped, w, h, true);
    }

    public void release() {
        mediaSession.release();
    }
}
