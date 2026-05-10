package com.music.player.ui;
import com.music.player.R;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.music.player.R;
import com.music.player.model.MusicFile;
import com.music.player.utils.FileLogger;

import java.io.File;

public class MusicInfoDialog {

    private final Context context;
    private final FileLogger fileLogger;

    public MusicInfoDialog(Context context) {
        this.context = context;
        this.fileLogger = FileLogger.getInstance(context);
    }

    public void show(MusicFile music) {
        if (music == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_music_info, null);
        TextView tvFullInfo = view.findViewById(R.id.tvMusicFullInfo);
        Button btnClose = view.findViewById(R.id.btnDialogClose);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        appendInfo(ssb, "TITLE", music.getTitle());
        appendInfo(ssb, "ARTIST", music.getArtist());
        appendInfo(ssb, "ALBUM", music.getAlbum());
        appendInfo(ssb, "DURATION", music.getDurationFormatted());
        appendInfo(ssb, "SIZE", music.getSizeFormatted());
        appendInfo(ssb, "PATH", music.getPath());

        try {
            ContentInfo info = new ContentInfoUtil().findMatch(new File(music.getPath()));
            String formatDescription = (info != null) ? info.getMessage() : 
                "Unknown " + music.getPath().substring(music.getPath().lastIndexOf(".")).toUpperCase() + " Audio";
            appendInfo(ssb, "FORMAT INFO", formatDescription);
        } catch (Exception e) {
            appendInfo(ssb, "FORMAT INFO", "Unable to detect file header");
            fileLogger.e("MusicInfoDialog", "SimpleMagic error: " + e);
        }

        tvFullInfo.setText(ssb);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Adjust window size
        if (dialog.getWindow() != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            dialog.getWindow().setLayout((int) (metrics.widthPixels * 0.85), (int) (metrics.heightPixels * 0.60));
        }
    }

    private void appendInfo(SpannableStringBuilder ssb, String label, String value) {
        int start = ssb.length();
        ssb.append(label).append("\n");
        ssb.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.turqoise)), 
                start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int valStart = ssb.length();
        ssb.append(value != null ? value : "-").append("\n\n");
        ssb.setSpan(new ForegroundColorSpan(Color.WHITE), valStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
