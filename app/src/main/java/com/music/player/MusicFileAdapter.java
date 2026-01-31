package com.music.player;

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
    
    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
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
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_music, parent, false);
        }
        
        MusicFile music = musicFiles.get(position);
        
        TextView tvMusicTitle = convertView.findViewById(R.id.tvMusicTitle);
        TextView tvMusicArtistAndAlbum = convertView.findViewById(R.id.tvMusicArtistAndAlbum);
        TextView tvMusicSize = convertView.findViewById(R.id.tvMusicSize);
        TextView tvMusicDuration = convertView.findViewById(R.id.tvMusicDuration);
        
        tvMusicTitle.setText(music.getTitle());
        tvMusicArtistAndAlbum.setText(music.getArtist() + " - " + music.getAlbum());
        tvMusicSize.setText(music.getSizeFormatted());
        tvMusicDuration.setText(music.getDurationFormatted());
        
        return convertView;
    }
}
