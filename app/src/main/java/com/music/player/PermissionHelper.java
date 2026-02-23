package com.music.player;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static final int REQ = 100;

    private static final String PERM_READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO";
    private static final String PERM_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

    /**
     * Checks if all necessary permissions are granted.
     * For API 33+, only READ_MEDIA_AUDIO is checked.
     * For API 23-32, READ_EXTERNAL_STORAGE
     */
    public static boolean hasAllNecessaryPermissions(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) {
            return a.checkSelfPermission(PERM_READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            // On older APIs, check both read and write external storage
            return a.checkSelfPermission(PERM_READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests all necessary permissions.
     * For API 33+, requests READ_MEDIA_AUDIO.
     * For API 23-32, requests READ_EXTERNAL_STORAGE
     */
    public static void request(Activity a) {
        List<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 33) {
            permissionsToRequest.add(PERM_READ_MEDIA_AUDIO);
        } else {
            permissionsToRequest.add(PERM_READ_EXTERNAL_STORAGE);
        }

        if (!permissionsToRequest.isEmpty()) {
            String[] perms = permissionsToRequest.toArray(new String[0]);
            a.requestPermissions(perms, REQ);
        }
    }
}
