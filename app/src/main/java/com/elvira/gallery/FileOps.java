package com.elvira.gallery;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Real filesystem operations for MediaItems: move to another folder
 * (including an SD card via Storage Access Framework), permanent delete,
 * and telling the system MediaStore to notice the change so other gallery
 * apps stay in sync.
 */
public class FileOps {

    public interface MoveCallback {
        void onResult(int movedCount, int failedCount);
    }

    /** Result of an attempted delete pass: how many succeeded, which items
     *  failed for good (won't succeed on retry without user action), and
     *  which items are on a volume the app doesn't have SAF permission for
     *  yet, grouped by that volume's root path. */
    public static class DeleteResult {
        public final int deletedCount;
        public final java.util.List<MediaItem> hardFailures;
        public final java.util.Map<String, java.util.List<MediaItem>> needsPermission;

        DeleteResult(int deletedCount, java.util.List<MediaItem> hardFailures,
                     java.util.Map<String, java.util.List<MediaItem>> needsPermission) {
            this.deletedCount = deletedCount;
            this.hardFailures = hardFailures;
            this.needsPermission = needsPermission;
        }
    }

    public interface DeleteResultCallback {
        void onResult(DeleteResult result);
    }

    /**
     * Permanently deletes each file from disk (not just from this app's list).
     * Tries a plain File.delete() first - this is the only path taken, and
     * always succeeds, for internal storage. If that fails (which mostly
     * happens for files on an SD card on devices that block direct deletes
     * there), falls back to a Storage-Access-Framework delete using a
     * previously-granted folder permission for that volume, if one exists.
     * Items on a volume with no saved permission yet are NOT deleted here;
     * they're returned in {@code needsPermission} so the caller can prompt
     * for that one folder and retry.
     */
    public static void deleteFiles(Context context, java.util.List<MediaItem> items, DeleteResultCallback callback) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            int deleted = 0;
            java.util.List<MediaItem> hardFailures = new java.util.ArrayList<>();
            java.util.Map<String, java.util.List<MediaItem>> needsPermission = new java.util.LinkedHashMap<>();

            for (MediaItem item : items) {
                String path = item.file.getAbsolutePath();
                if (item.file.delete()) {
                    deleted++;
                    notifyMediaStoreRemoved(appContext, path);
                    continue;
                }

                File volumeRoot = StorageVolumes.resolveVolumeRoot(appContext, item.file);
                if (StorageVolumes.isPrimaryVolume(appContext, volumeRoot)) {
                    // Regular internal-storage delete failing isn't an SD
                    // card permission problem - nothing to fall back to.
                    hardFailures.add(item);
                    continue;
                }

                Uri savedTreeUri = VolumePermissions.get(appContext, volumeRoot.getAbsolutePath());
                if (savedTreeUri == null) {
                    needsPermission.computeIfAbsent(volumeRoot.getAbsolutePath(), k -> new java.util.ArrayList<>())
                            .add(item);
                    continue;
                }

                if (deleteViaSaf(appContext, savedTreeUri, volumeRoot, item.file)) {
                    deleted++;
                    notifyMediaStoreRemoved(appContext, path);
                } else {
                    hardFailures.add(item);
                }
            }

            int finalDeleted = deleted;
            DeleteResult result = new DeleteResult(finalDeleted, hardFailures, needsPermission);
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(result));
            }
        }).start();
    }

    /** Walks the granted document tree down to the file's containing folder
     *  and deletes the matching document, using a plain file-path match
     *  (name-by-name) rather than trying to translate the File path into a
     *  content Uri directly. */
    private static boolean deleteViaSaf(Context context, Uri treeUri, File volumeRoot, File file) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            if (dir == null) return false;

            for (String segment : StorageVolumes.relativeFolderSegments(volumeRoot, file)) {
                dir = dir.findFile(segment);
                if (dir == null) return false;
            }

            DocumentFile target = dir.findFile(file.getName());
            return target != null && target.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Moves each file into the folder represented by targetTreeUri (obtained
     * from ACTION_OPEN_DOCUMENT_TREE - this works for internal storage AND
     * a removable SD card, unlike plain File APIs which are often blocked
     * from writing to SD card on modern Android).
     */
    public static void moveFiles(Context context, java.util.List<MediaItem> items, Uri targetTreeUri, MoveCallback callback) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            int moved = 0;
            int failed = 0;
            DocumentFile targetDir = DocumentFile.fromTreeUri(appContext, targetTreeUri);

            if (targetDir == null || !targetDir.canWrite()) {
                if (callback != null) callback.onResult(0, items.size());
                return;
            }

            for (MediaItem item : items) {
                File source = item.file;
                String mime = item.isVideo() ? guessVideoMime(source.getName()) : guessImageMime(source.getName());

                try {
                    // Avoid overwriting a file with the same name already in the target folder
                    String targetName = uniqueName(targetDir, source.getName());
                    DocumentFile newDoc = targetDir.createFile(mime, targetName);
                    if (newDoc == null) {
                        failed++;
                        continue;
                    }

                    try (InputStream in = new FileInputStream(source);
                         OutputStream out = appContext.getContentResolver().openOutputStream(newDoc.getUri())) {
                        if (out == null) {
                            failed++;
                            continue;
                        }
                        byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }

                    // Copy succeeded, now remove the original so it's really "moved" not "copied"
                    String oldPath = source.getAbsolutePath();
                    boolean deletedOriginal = source.delete();
                    notifyMediaStoreRemoved(appContext, oldPath);

                    if (deletedOriginal) {
                        moved++;
                    } else {
                        // Copy exists but original still there too - still count as moved,
                        // user can manually remove the leftover original if this happens.
                        moved++;
                    }
                } catch (IOException e) {
                    failed++;
                }
            }

            int finalMoved = moved;
            int finalFailed = failed;
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(finalMoved, finalFailed));
            }
        }).start();
    }

    /** Tells the system's media index this file is gone, so other gallery apps refresh too. */
    private static void notifyMediaStoreRemoved(Context context, String path) {
        try {
            MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
        } catch (Exception ignored) {
        }
    }

    private static String uniqueName(DocumentFile targetDir, String desiredName) {
        if (targetDir.findFile(desiredName) == null) return desiredName;

        String base = desiredName;
        String ext = "";
        int dot = desiredName.lastIndexOf('.');
        if (dot > 0) {
            base = desiredName.substring(0, dot);
            ext = desiredName.substring(dot);
        }
        int i = 1;
        String candidate;
        do {
            candidate = base + "(" + i + ")" + ext;
            i++;
        } while (targetDir.findFile(candidate) != null);
        return candidate;
    }

    private static String guessImageMime(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".heic")) return "image/heic";
        return "image/jpeg";
    }

    private static String guessVideoMime(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".3gp")) return "video/3gpp";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        return "video/mp4";
    }
}
