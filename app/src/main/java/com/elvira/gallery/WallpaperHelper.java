package com.elvira.gallery;

import android.app.WallpaperManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperHelper {

    public static final int TARGET_HOME = 1;
    public static final int TARGET_LOCK = 2;
    public static final int TARGET_BOTH = 3;

    public interface Callback {
        void onDone(boolean success);
    }

    /** Applies the given photo file as wallpaper. Runs the heavy work off the main thread. */
    public static void apply(Context context, MediaItem photoItem, int target, Callback callback) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            boolean success = true;
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(appContext);

            try {
                if (target == TARGET_HOME || target == TARGET_BOTH) {
                    try (InputStream in = new FileInputStream(photoItem.file)) {
                        wallpaperManager.setStream(in, null, true, flagFor(TARGET_HOME));
                    }
                }
                if ((target == TARGET_LOCK || target == TARGET_BOTH)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try (InputStream in = new FileInputStream(photoItem.file)) {
                        wallpaperManager.setStream(in, null, true, flagFor(TARGET_LOCK));
                    }
                }
            } catch (IOException e) {
                success = false;
            }

            boolean finalSuccess = success;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onDone(finalSuccess);
            });
        }).start();
    }

    private static int flagFor(int target) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return WallpaperManager.FLAG_SYSTEM;
        }
        if (target == TARGET_HOME) return WallpaperManager.FLAG_SYSTEM;
        if (target == TARGET_LOCK) return WallpaperManager.FLAG_LOCK;
        return WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
    }
}
