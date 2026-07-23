package com.elvira.gallery;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Figures out which physical storage volume (internal storage vs. an SD
 * card / other removable volume) a given file lives on, and builds the
 * right document-tree picker intent to ask permission for that specific
 * volume. This is what lets the delete feature fall back to the Storage
 * Access Framework only when it's actually needed (SD card on a device
 * that refuses plain File.delete() there), instead of always asking.
 */
public class StorageVolumes {

    private StorageVolumes() {}

    /** Root folder of the volume containing {@code file}: either the primary
     *  shared storage root, or a secondary volume root such as /storage/1234-5678. */
    public static File resolveVolumeRoot(Context context, File file) {
        String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = file.getAbsolutePath();

        List<File> secondaryRoots = listSecondaryVolumeRoots(context, primaryPath);
        for (File root : secondaryRoots) {
            if (filePath.startsWith(root.getAbsolutePath() + File.separator)
                    || filePath.equals(root.getAbsolutePath())) {
                return root;
            }
        }
        return new File(primaryPath);
    }

    public static boolean isPrimaryVolume(Context context, File volumeRoot) {
        String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        return volumeRoot.getAbsolutePath().equals(primaryPath);
    }

    /** Same technique MainActivity uses to find every mounted storage root
     *  (internal + any SD card), reused here so a single file's path can be
     *  matched back to the volume it belongs to. */
    private static List<File> listSecondaryVolumeRoots(Context context, String primaryPath) {
        List<File> roots = new ArrayList<>();
        File[] externalDirs = ContextCompat.getExternalFilesDirs(context, null);
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir == null) continue;
                String path = dir.getAbsolutePath();
                int idx = path.indexOf("/Android/data/");
                if (idx <= 0) continue;
                String volumeRoot = path.substring(0, idx);
                if (!volumeRoot.equals(primaryPath)) {
                    roots.add(new File(volumeRoot));
                }
            }
        }
        return roots;
    }

    /**
     * Builds an ACTION_OPEN_DOCUMENT_TREE intent already scoped to
     * {@code volumeRoot}, so the folder picker opens directly on that SD
     * card (instead of a generic picker the user has to navigate manually).
     * Falls back to a plain, unscoped picker if the OS can't identify the
     * volume for some reason.
     */
    public static Intent createTreeIntentForVolume(Context context, File volumeRoot) {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (sm != null) {
                StorageVolume volume = sm.getStorageVolume(volumeRoot);
                if (volume != null) {
                    return volume.createOpenDocumentTreeIntent();
                }
            }
        } catch (Exception ignored) {
            // Fall through to the generic picker below.
        }
        return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    }

    /** Path segments to walk down from {@code volumeRoot} to reach
     *  {@code file}'s containing folder (excluding the file name itself). */
    public static String[] relativeFolderSegments(File volumeRoot, File file) {
        File parent = file.getParentFile();
        String rootPath = volumeRoot.getAbsolutePath();
        if (parent == null) return new String[0];

        String parentPath = parent.getAbsolutePath();
        if (!parentPath.startsWith(rootPath)) return new String[0];

        String relative = parentPath.substring(rootPath.length());
        if (relative.startsWith(File.separator)) relative = relative.substring(1);
        if (relative.isEmpty()) return new String[0];
        return relative.split(File.separator);
    }
}
