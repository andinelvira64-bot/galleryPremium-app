package com.elvira.gallery;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

/**
 * Scans a root folder and ALL of its nested subfolders (any depth) for
 * photo and video files. Works for structures like:
 *   Download/fkdos/djdiejd/foto.jpg
 *   Download/anyFolder/anyDeeperFolder/video.mp4
 *
 * Uses an explicit stack instead of recursion so it can handle very deep
 * or very large folder trees without a StackOverflowError.
 */
public class MediaScanner {

    private static final Set<String> IMAGE_EXT = new HashSet<>();
    private static final Set<String> VIDEO_EXT = new HashSet<>();

    static {
        IMAGE_EXT.add("jpg");
        IMAGE_EXT.add("jpeg");
        IMAGE_EXT.add("png");
        IMAGE_EXT.add("webp");
        IMAGE_EXT.add("heic");
        IMAGE_EXT.add("bmp");
        IMAGE_EXT.add("gif");

        VIDEO_EXT.add("mp4");
        VIDEO_EXT.add("3gp");
        VIDEO_EXT.add("mkv");
        VIDEO_EXT.add("webm");
        VIDEO_EXT.add("mov");
        VIDEO_EXT.add("avi");
        VIDEO_EXT.add("m4v");
    }

    public interface ScanCallback {
        /** Called on the main thread once scanning finishes. */
        void onScanComplete(List<MediaItem> items);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Scans rootDir and every nested subfolder beneath it, no matter how
     * deep, and returns every jpg/png/etc photo and mp4/etc video it finds.
     */
    public void scanAsync(final File rootDir, final ScanCallback callback) {
        EXECUTOR.execute(() -> {
            final List<MediaItem> results = new ArrayList<>();
            scanRecursively(rootDir, results);

            // Newest first
            Collections.sort(results, (a, b) -> Long.compare(b.dateModified, a.dateModified));

            mainHandler.post(() -> callback.onScanComplete(results));
        });
    }

    /** Synchronous version, call only from a background thread. */
    public List<MediaItem> scanSync(File rootDir) {
        List<MediaItem> results = new ArrayList<>();
        scanRecursively(rootDir, results);
        Collections.sort(results, (a, b) -> Long.compare(b.dateModified, a.dateModified));
        return results;
    }

    private void scanRecursively(File rootDir, List<MediaItem> out) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) return;

        // Explicit stack -> handles arbitrarily nested folders
        // (Download/fkdos/djdiejd/... as deep as it goes) without recursion limits.
        Deque<File> stack = new ArrayDeque<>();
        stack.push(rootDir);

        while (!stack.isEmpty()) {
            File dir = stack.pop();
            File[] children = dir.listFiles();
            if (children == null) continue;

            for (File child : children) {
                if (child.isDirectory()) {
                    // Skip hidden folders (e.g. .thumbnails, .trashed) to avoid junk
                    if (!child.getName().startsWith(".")) {
                        stack.push(child);
                    }
                } else if (child.isFile()) {
                    String ext = getExtension(child.getName());
                    if (IMAGE_EXT.contains(ext)) {
                        out.add(new MediaItem(child, MediaItem.TYPE_PHOTO, child.lastModified()));
                    } else if (VIDEO_EXT.contains(ext)) {
                        out.add(new MediaItem(child, MediaItem.TYPE_VIDEO, child.lastModified()));
                    }
                }
            }
        }
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
