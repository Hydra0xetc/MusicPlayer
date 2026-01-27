package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
        
        TextView tvMusicName = convertView.findViewById(R.id.tvMusicName);
        TextView tvMusicSize = convertView.findViewById(R.id.tvMusicSize);
        
        tvMusicName.setText(music.getName());
        tvMusicSize.setText(music.getSizeFormatted());
        
        return convertView;
    }
}
