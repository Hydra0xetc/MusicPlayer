package com.music.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicFileAdapter extends BaseAdapter {

    private Context context;
    private List<MusicFile> musicFiles;
    private LayoutInflater inflater;
    private BitmapCache bitmapCache;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    private FileLogger fileLogger;
    private final String TAG = "MusicFileAdapter";
    private String playingPath = Constant.EMPTY_STRING;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
        this.inflater = LayoutInflater.from(context);
        this.bitmapCache = BitmapCache.getInstance();
        
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        fileLogger = FileLogger.getInstance(context);
    }

    public void setPlayingPath(String path) {
        this.playingPath = path != null ? path : Constant.EMPTY_STRING;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView tvMusicTitle;
        TextView tvMusicArtistAndAlbum;
        TextView tvMusicInfo;
        ImageView imgAlbumArt;
        View llSoundWave;
        View wave1, wave2, wave3;
        String path;
    }

    @Override
    public int getCount() {
        return musicFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return musicFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_music, parent, false);

            holder = new ViewHolder();
            holder.tvMusicTitle = convertView.findViewById(R.id.tvMusicTitle);
            holder.tvMusicArtistAndAlbum = convertView.findViewById(R.id.tvMusicArtistAndAlbum);
            holder.tvMusicInfo = convertView.findViewById(R.id.tvMusicInfo);
            holder.imgAlbumArt = convertView.findViewById(R.id.imgAlbumArt);
            holder.llSoundWave = convertView.findViewById(R.id.llSoundWave);
            holder.wave1 = convertView.findViewById(R.id.wave1);
            holder.wave2 = convertView.findViewById(R.id.wave2);
            holder.wave3 = convertView.findViewById(R.id.wave3);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MusicFile music = musicFiles.get(position);
        final ViewHolder finalHolder = holder;

        boolean isPlaying = music.getPath().equals(playingPath);
        
        holder.tvMusicTitle.setText(music.getTitle());
        holder.tvMusicTitle.setTextColor(isPlaying ? context.getResources().getColor(R.color.turqoise) : context.getResources().getColor(R.color.white));
        
        holder.tvMusicArtistAndAlbum.setText(
                music.getArtist() + " - " + music.getAlbum()
        );

        holder.tvMusicInfo.setText(
                music.getSizeFormatted() + " • " + music.getDurationFormatted()
        );

        // Smart Rebinding to prevent flickering
        String oldPath = holder.path;
        holder.path = music.getPath();

        // Wave Animation
        if (isPlaying) {
            holder.llSoundWave.setVisibility(View.VISIBLE);
            startWaveAnimation(holder);
        } else {
            holder.llSoundWave.setVisibility(View.GONE);
            holder.wave1.clearAnimation();
            holder.wave2.clearAnimation();
            holder.wave3.clearAnimation();
        }

        Bitmap cachedBitmap = bitmapCache.getBitmapFromMemCache(music.getPath());
        
        if (cachedBitmap != null) {
            holder.imgAlbumArt.setImageBitmap(cachedBitmap);
        } else {
            // Only set default/placeholder if the path has actually changed
            // This prevents the "flash" to default when the same item is re-bound (e.g. during overscroll)
            if (oldPath == null || !oldPath.equals(music.getPath())) {
                holder.imgAlbumArt.setImageResource(R.mipmap.ic_launcher);
            }
            
            final byte[] art = music.getAlbumArt();
            if (art != null) {
                loadAlbumArtAsync(art, music.getPath(), finalHolder);
            }
        }

        return convertView;
    }

    private void startWaveAnimation(ViewHolder holder) {
        android.view.animation.Animation anim1 = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.wave_anim);
        android.view.animation.Animation anim2 = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.wave_anim);
        android.view.animation.Animation anim3 = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.wave_anim);

        anim2.setStartOffset(150);
        anim3.setStartOffset(300);

        holder.wave1.startAnimation(anim1);
        holder.wave2.startAnimation(anim2);
        holder.wave3.startAnimation(anim3);
    }
    
    private void loadAlbumArtAsync(final byte[] albumArt, final String path, final ViewHolder holder) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                    
                    if (bitmap != null) {
                        bitmapCache.addBitmapToMemoryCache(path, bitmap);
                        
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (holder.path != null && holder.path.equals(path)) {
                                    holder.imgAlbumArt.setImageBitmap(bitmap);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    fileLogger.e(TAG, "Unexpected error: " + e);
                }
            }
        });
    }
}
