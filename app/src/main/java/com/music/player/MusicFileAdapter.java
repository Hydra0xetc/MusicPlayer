package com.music.player;

import android.util.LruCache;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MusicFileAdapter extends BaseAdapter {

    private Context context;
    private List<MusicFile> musicFiles;
    private LayoutInflater inflater;
    private LruCache<String, Bitmap> bitmapCache;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
        this.inflater = LayoutInflater.from(context);

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        bitmapCache = new LruCache<>(cacheSize);
    }

    static class ViewHolder {
        TextView tvMusicTitle;
        TextView tvMusicArtistAndAlbum;
        TextView tvMusicInfo;
        ImageView imgAlbumArt;
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

        MusicFile music = musicFiles.get(position);

        holder.tvMusicTitle.setText(music.getTitle());
        holder.tvMusicArtistAndAlbum.setText(
                music.getArtist() + " - " + music.getAlbum()
        );

        holder.tvMusicInfo.setText(
                music.getSizeFormatted() + " • " + music.getDurationFormatted()
        );

        byte[] art = music.getAlbumArt();
        Bitmap bmp = bitmapCache.get(music.getPath());

        if (bmp == null && art != null) {
            bmp = BitmapFactory.decodeByteArray(art, 0, art.length);
            bitmapCache.put(music.getPath(), bmp);
        }

        if (bmp != null) {
            holder.imgAlbumArt.setImageBitmap(bmp);
        } else {
            holder.imgAlbumArt.setImageResource(R.mipmap.ic_launcher);
        }

        return convertView;
    }
}
