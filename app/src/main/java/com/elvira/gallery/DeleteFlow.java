package com.elvira.gallery;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives a permanent-delete request through to completion, including the
 * one-time SD card permission prompt when needed:
 *
 *   1. Try to delete everything (plain delete; SAF delete for volumes
 *      already granted).
 *   2. If some items are stuck on a volume with no saved permission yet,
 *      ask the user once to pick that folder.
 *   3. Retry with the newly granted permission. Repeat if more than one
 *      such volume is involved (rare - e.g. two SD cards).
 *   4. Report final counts back to the caller.
 *
 * One instance is meant to be created once per host Activity and reused
 * across delete requests; the host must forward its tree-picker launcher's
 * result to {@link #onTreePicked(Intent)}.
 */
public class DeleteFlow {

    public interface Callback {
        void onFinished(int deletedCount, int failedCount);
    }

    private final Activity activity;
    private final ActivityResultLauncher<Intent> treeLauncher;

    private int totalDeleted;
    private int totalFailed;
    private Callback callback;
    private String pendingVolumeRoot;
    private List<MediaItem> pendingVolumeItems;
    private Map<String, List<MediaItem>> otherPendingVolumes;

    public DeleteFlow(Activity activity, ActivityResultLauncher<Intent> treeLauncher) {
        this.activity = activity;
        this.treeLauncher = treeLauncher;
    }

    public void start(List<MediaItem> items, Callback callback) {
        this.callback = callback;
        totalDeleted = 0;
        totalFailed = 0;
        runRound(items);
    }

    private void runRound(List<MediaItem> items) {
        FileOps.deleteFiles(activity, items, result -> {
            totalDeleted += result.deletedCount;
            totalFailed += result.hardFailures.size();

            if (result.needsPermission.isEmpty()) {
                callback.onFinished(totalDeleted, totalFailed);
                return;
            }

            Map.Entry<String, List<MediaItem>> first = result.needsPermission.entrySet().iterator().next();
            otherPendingVolumes = new HashMap<>(result.needsPermission);
            otherPendingVolumes.remove(first.getKey());
            askForVolumePermission(first.getKey(), first.getValue());
        });
    }

    private void askForVolumePermission(String volumeRoot, List<MediaItem> items) {
        pendingVolumeRoot = volumeRoot;
        pendingVolumeItems = items;

        new AlertDialog.Builder(activity)
                .setTitle(R.string.sdcard_permission_title)
                .setMessage(activity.getString(R.string.sdcard_permission_message, items.size()))
                .setNegativeButton(R.string.delete_cancel, (dialog, which) -> giveUpOnPendingVolumes())
                .setPositiveButton(R.string.sdcard_permission_grant, (dialog, which) -> {
                    Intent treeIntent = StorageVolumes.createTreeIntentForVolume(activity, new File(volumeRoot));
                    treeLauncher.launch(treeIntent);
                })
                .setCancelable(false)
                .show();
    }

    /** Call this from the host Activity's ActivityResultLauncher callback
     *  for the tree-picker launcher passed into the constructor. */
    public void onTreePicked(Intent resultData) {
        Uri treeUri = resultData != null ? resultData.getData() : null;
        if (treeUri == null) {
            giveUpOnPendingVolumes();
            return;
        }

        activity.getContentResolver().takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        VolumePermissions.save(activity, pendingVolumeRoot, treeUri);

        List<MediaItem> retryBatch = new ArrayList<>(pendingVolumeItems);
        for (List<MediaItem> more : otherPendingVolumes.values()) {
            retryBatch.addAll(more);
        }
        runRound(retryBatch);
    }

    private void giveUpOnPendingVolumes() {
        totalFailed += pendingVolumeItems.size();
        for (List<MediaItem> more : otherPendingVolumes.values()) {
            totalFailed += more.size();
        }
        callback.onFinished(totalDeleted, totalFailed);
    }
}
