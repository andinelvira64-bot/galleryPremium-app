package com.elvira.gallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

/**
 * Remembers, per storage volume root (e.g. an SD card's mount path), the
 * tree Uri the user granted via the Storage Access Framework folder picker.
 * Deleting a file that plain File.delete() can't remove (which mostly
 * happens on SD cards on some devices) falls back to this saved permission
 * instead of asking the user again every single time.
 */
public class VolumePermissions {

    private static final String PREFS_NAME = "volume_permissions";

    private VolumePermissions() {}

    public static Uri get(Context context, String volumeRootPath) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(volumeRootPath, null);
        return value != null ? Uri.parse(value) : null;
    }

    public static void save(Context context, String volumeRootPath, Uri treeUri) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(volumeRootPath, treeUri.toString()).apply();
    }
}
