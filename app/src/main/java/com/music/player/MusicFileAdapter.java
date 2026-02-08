package com.music.player;

import android.util.LruCache;
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
    private LruCache<String, Bitmap> bitmapCache;
    
    private ExecutorService executorService;
    private Handler mainHandler;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
        this.inflater = LayoutInflater.from(context);

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        bitmapCache = new LruCache<>(cacheSize);
        
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    static class ViewHolder {
        TextView tvMusicTitle;
        TextView tvMusicArtistAndAlbum;
        TextView tvMusicInfo;
        ImageView imgAlbumArt;
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

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MusicFile music = musicFiles.get(position);
        final ViewHolder finalHolder = holder;

        holder.tvMusicTitle.setText(music.getTitle());
        holder.tvMusicArtistAndAlbum.setText(
                music.getArtist() + " - " + music.getAlbum()
        );

        holder.tvMusicInfo.setText(
                music.getSizeFormatted() + " • " + music.getDurationFormatted()
        );

        holder.path = music.getPath();

        Bitmap cachedBitmap = bitmapCache.get(music.getPath());
        
        if (cachedBitmap != null) {
            holder.imgAlbumArt.setImageBitmap(cachedBitmap);
        } else {
            holder.imgAlbumArt.setImageResource(R.mipmap.ic_launcher);
            
            final byte[] art = music.getAlbumArt();
            if (art != null) {
                loadAlbumArtAsync(art, music.getPath(), finalHolder);
            }
        }

        return convertView;
    }
    
    private void loadAlbumArtAsync(final byte[] albumArt, final String path, final ViewHolder holder) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                    
                    if (bitmap != null) {
                        bitmapCache.put(path, bitmap);
                        
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
                    // Ignore decoding errors
                }
            }
        });
    }
}
