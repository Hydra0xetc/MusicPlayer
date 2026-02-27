package com.music.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicFileAdapter extends BaseAdapter {

    private Context context;
    private List<MusicFile> allMusicFiles;
    private List<MusicFile> filteredMusicFiles;
    private LayoutInflater inflater;
    private BitmapCache bitmapCache;
    private AlbumArtManager artManager;

    private ExecutorService executorService;
    private Handler mainHandler;
    private FileLogger fileLogger;
    private final String TAG = "MusicFileAdapter";
    private String playingPath = Constant.EMPTY_STRING;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.allMusicFiles = musicFiles;
        this.filteredMusicFiles = musicFiles;
        this.inflater = LayoutInflater.from(context);
        this.bitmapCache = BitmapCache.getInstance();
        this.artManager = AlbumArtManager.getInstance(context);

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        fileLogger = FileLogger.getInstance(context);
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredMusicFiles = allMusicFiles;
        } else {
            String lowerQuery = query.toLowerCase().trim();
            java.util.List<MusicFile> filtered = new ArrayList<>();
            for (MusicFile file : allMusicFiles) {
                if (isFuzzyMatch(file.getTitle().toLowerCase(), lowerQuery) ||
                        isFuzzyMatch(file.getArtist().toLowerCase(), lowerQuery) ||
                        isFuzzyMatch(file.getAlbum().toLowerCase(), lowerQuery)) {
                    filtered.add(file);
                }
            }
            filteredMusicFiles = filtered;
        }
        notifyDataSetChanged();
    }

    private boolean isFuzzyMatch(String text, String query) {
        if (query.length() == 0) {
            return true;
        }
        if (query.length() > text.length()) {
            return false;
        }
        int textIdx = 0, queryIdx = 0;
        while (textIdx < text.length() && queryIdx < query.length()) {
            if (text.charAt(textIdx) == query.charAt(queryIdx)) {
                queryIdx++;
            }
            textIdx++;
        }
        return queryIdx == query.length();
    }

    public void updateList(List<MusicFile> newList) {
        this.allMusicFiles = newList;
        this.filteredMusicFiles = newList;
        notifyDataSetChanged();
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
        return filteredMusicFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredMusicFiles.get(position);
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

        final MusicFile music = filteredMusicFiles.get(position);
        final ViewHolder finalHolder = holder;
        boolean isPlaying = music.getPath().equals(playingPath);

        holder.tvMusicTitle.setText(music.getTitle());
        holder.tvMusicTitle.setTextColor(
                isPlaying ? context.getResources().getColor(R.color.turqoise)
                        : context.getResources().getColor(R.color.white));

        holder.tvMusicArtistAndAlbum.setText(music.getArtist() + " - " + music.getAlbum());
        holder.tvMusicInfo.setText(music.getSizeFormatted() + " • " + music.getDurationFormatted());

        // Wave animation
        if (isPlaying) {
            holder.llSoundWave.setVisibility(View.VISIBLE);
            startWaveAnimation(holder);
        } else {
            holder.llSoundWave.setVisibility(View.GONE);
            holder.wave1.clearAnimation();
            holder.wave2.clearAnimation();
            holder.wave3.clearAnimation();
        }

        String oldPath = holder.path;
        holder.path = music.getPath();

        Bitmap cachedBitmap = bitmapCache.getBitmapFromMemCache(music.getPath());
        if (cachedBitmap != null) {
            holder.imgAlbumArt.setImageBitmap(cachedBitmap);
        } else {
            // Only flash to placeholder when view is truly reused for a different song
            if (oldPath == null || !oldPath.equals(music.getPath())) {
                holder.imgAlbumArt.setImageResource(R.mipmap.ic_launcher);
            }
            // Load from disk on background thread
            loadAlbumArtAsync(music.getPath(), finalHolder);
        }

        return convertView;
    }

    private void startWaveAnimation(ViewHolder holder) {
        Animation a1 = AnimationUtils.loadAnimation(context, R.anim.wave_anim);
        Animation a2 = AnimationUtils.loadAnimation(context, R.anim.wave_anim);
        Animation a3 = AnimationUtils.loadAnimation(context, R.anim.wave_anim);
        a2.setStartOffset(150);
        a3.setStartOffset(300);
        holder.wave1.startAnimation(a1);
        holder.wave2.startAnimation(a2);
        holder.wave3.startAnimation(a3);
    }

    private void loadAlbumArtAsync(final String path, final ViewHolder holder) {
        executorService.execute(() -> {
            try {
                // Load from internal disk cache
                Bitmap bitmap = artManager.loadAlbumArt(path);

                if (bitmap != null) {
                    bitmapCache.addBitmapToMemoryCache(path, bitmap);

                    mainHandler.post(() -> {
                        if (holder.path != null && holder.path.equals(path)) {
                            holder.imgAlbumArt.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                fileLogger.e(TAG, "loadAlbumArtAsync error: " + e.getMessage());
            }
        });
    }
}
