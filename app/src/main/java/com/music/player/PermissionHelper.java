package com.music.player;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionHelper {

    public static final int REQ = 100;

    private static final String PERM_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO";

    public static boolean hasAudioPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) {
            return a.checkSelfPermission(
                    PERM_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= 23) {
            return a.checkSelfPermission(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void request(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) {
            a.requestPermissions( new String[]{ PERM_MEDIA_AUDIO }, REQ);
        } else {
            a.requestPermissions(
            new String[] {
                android.Manifest.permission.READ_EXTERNAL_STORAGE 
            }, REQ);
        }
    }
}
