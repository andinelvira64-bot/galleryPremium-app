package com.elvira.gallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lets the user scrub through a video, grab one or more exact frames, and
 * save each captured frame as a brand-new photo file sitting right next to
 * the original video. The source video itself is never modified or deleted
 * - this only ever creates new files.
 */
public class FrameCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_CAPTURE_SUCCEEDED = "extra_capture_succeeded";

    private ImageView ivFramePreview;
    private SeekBar seekBarFrame;
    private TextView tvFrameTime;
    private TextView tvFrameCount;
    private TextView btnCaptureFrame;
    private ImageButton btnFrameCaptureClose;
    private ImageButton btnFrameCaptureSave;
    private LinearLayout capturedFramesStrip;
    private ProgressBar progressBar;

    private String videoPath;
    private File videoFile;
    private MediaMetadataRetriever retriever;
    private long durationMs;

    private volatile Bitmap currentFrameBitmap;
    private final List<Bitmap> capturedFrames = new ArrayList<>();
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long latestSeekRequestId = 0;

    // Pending SAF (SD-card) save state, mirrors CropActivity's pattern.
    private String pendingVolumeRoot;
    private List<String> pendingFilenames;
    private List<byte[]> pendingByteList;
    private final ActivityResultLauncher<Intent> treePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onTreePicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_capture);

        ivFramePreview = findViewById(R.id.ivFramePreview);
        seekBarFrame = findViewById(R.id.seekBarFrame);
        tvFrameTime = findViewById(R.id.tvFrameTime);
        tvFrameCount = findViewById(R.id.tvFrameCount);
        btnCaptureFrame = findViewById(R.id.btnCaptureFrame);
        btnFrameCaptureClose = findViewById(R.id.btnFrameCaptureClose);
        btnFrameCaptureSave = findViewById(R.id.btnFrameCaptureSave);
        capturedFramesStrip = findViewById(R.id.capturedFramesStrip);
        progressBar = findViewById(R.id.frameCaptureProgress);

        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        setResult(Activity.RESULT_CANCELED);

        if (videoPath == null) {
            Toast.makeText(this, R.string.frame_capture_load_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        videoFile = new File(videoPath);

        btnFrameCaptureClose.setOnClickListener(v -> finish());
        btnFrameCaptureSave.setOnClickListener(v -> saveCapturedFrames());
        btnCaptureFrame.setOnClickListener(v -> captureCurrentFrame());

        loadVideoMetadata();
        refreshCapturedFramesUI();
    }

    private void loadVideoMetadata() {
        setBusy(true);
        retriever = new MediaMetadataRetriever();
        bgExecutor.execute(() -> {
            try {
                retriever.setDataSource(videoPath);
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                durationMs = durationStr != null ? Long.parseLong(durationStr) : 0L;
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setBusy(false);
                    Toast.makeText(this, R.string.frame_capture_load_failed, Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            mainHandler.post(() -> {
                seekBarFrame.setMax((int) Math.max(durationMs, 1));
                setupSeekBarListener();
                updateTimeLabel(0);
                setBusy(false);
                requestFrameAt(0);
            });
        });
    }

    private void setupSeekBarListener() {
        seekBarFrame.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeLabel(progress);
                if (fromUser) requestFrameAt(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void requestFrameAt(int ms) {
        long myRequestId = ++latestSeekRequestId;
        bgExecutor.execute(() -> {
            Bitmap frame;
            try {
                frame = retriever.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST);
            } catch (Exception e) {
                frame = null;
            }
            Bitmap finalFrame = frame;
            mainHandler.post(() -> {
                // Discard stale results from a seek position the user has
                // already scrubbed past.
                if (myRequestId != latestSeekRequestId || finalFrame == null) return;
                currentFrameBitmap = finalFrame;
                ivFramePreview.setImageBitmap(finalFrame);
            });
        });
    }

    private void updateTimeLabel(int currentMs) {
        tvFrameTime.setText(String.format(Locale.getDefault(), "%s / %s",
                formatTime(currentMs), formatTime((int) durationMs)));
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // ---------------------------------------------------------------
    // Capturing frames into the on-screen list
    // ---------------------------------------------------------------

    private void captureCurrentFrame() {
        if (currentFrameBitmap == null) return;
        capturedFrames.add(currentFrameBitmap);
        refreshCapturedFramesUI();
    }

    private void refreshCapturedFramesUI() {
        capturedFramesStrip.removeAllViews();

        for (int i = 0; i < capturedFrames.size(); i++) {
            final int index = i;
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_captured_frame, capturedFramesStrip, false);
            ImageView ivThumb = itemView.findViewById(R.id.ivCapturedThumb);
            TextView tvNumber = itemView.findViewById(R.id.tvCapturedNumber);
            ImageButton btnRemove = itemView.findViewById(R.id.btnRemoveCapturedFrame);

            ivThumb.setImageBitmap(capturedFrames.get(index));
            tvNumber.setText(String.valueOf(index + 1));
            btnRemove.setOnClickListener(v -> {
                capturedFrames.remove(index);
                refreshCapturedFramesUI();
            });

            capturedFramesStrip.addView(itemView);
        }

        tvFrameCount.setText(capturedFrames.isEmpty()
                ? getString(R.string.frame_capture_none_yet)
                : getString(R.string.frame_capture_count, capturedFrames.size()));
    }

    // ---------------------------------------------------------------
    // Saving: each captured frame becomes its own new JPEG file next to the
    // video. The video file itself is never opened for writing, so it's
    // completely safe regardless of how this save goes.
    // ---------------------------------------------------------------

    private void saveCapturedFrames() {
        if (capturedFrames.isEmpty()) return;

        setBusy(true);
        List<Bitmap> toSave = new ArrayList<>(capturedFrames);
        String videoName = videoFile.getName();
        String baseName = videoName;
        int dot = videoName.lastIndexOf('.');
        if (dot > 0) baseName = videoName.substring(0, dot);
        final String finalBaseName = baseName;
        long timestamp = System.currentTimeMillis();

        bgExecutor.execute(() -> {
            List<byte[]> byteList = new ArrayList<>();
            List<String> filenames = new ArrayList<>();
            for (int i = 0; i < toSave.size(); i++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                toSave.get(i).compress(Bitmap.CompressFormat.JPEG, 92, baos);
                byteList.add(baos.toByteArray());
                filenames.add(finalBaseName + "_frame" + (i + 1) + "_" + timestamp + ".jpg");
            }

            File parentDir = videoFile.getParentFile();
            List<String> savedPaths = new ArrayList<>();
            boolean allDirectOk = parentDir != null;

            if (allDirectOk) {
                for (int i = 0; i < filenames.size(); i++) {
                    File outFile = new File(parentDir, filenames.get(i));
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(byteList.get(i));
                        fos.flush();
                        savedPaths.add(outFile.getAbsolutePath());
                    } catch (Exception e) {
                        allDirectOk = false;
                        break;
                    }
                }
            }

            if (allDirectOk) {
                onSaveSucceeded(savedPaths, toSave.size(), 0);
                return;
            }

            // Direct write failed partway through - almost always an SD card
            // Android won't let plain File APIs write to. Fall back to the
            // Storage Access Framework, same approach as crop/delete.
            File volumeRoot = StorageVolumes.resolveVolumeRoot(this, parentDir != null ? parentDir : videoFile);
            if (StorageVolumes.isPrimaryVolume(this, volumeRoot)) {
                mainHandler.post(this::showSaveFailed);
                return;
            }

            Uri savedTree = VolumePermissions.get(this, volumeRoot.getAbsolutePath());
            if (savedTree != null) {
                int[] result = writeAllViaSaf(savedTree, volumeRoot, parentDir, filenames, byteList);
                if (result[0] > 0 || result[1] == 0) {
                    onSaveSucceeded(null, result[0], result[1]);
                    return;
                }
            }

            // No usable saved permission - ask the user to pick that volume's
            // folder once, then retry.
            pendingVolumeRoot = volumeRoot.getAbsolutePath();
            pendingFilenames = filenames;
            pendingByteList = byteList;
            mainHandler.post(() -> new AlertDialog.Builder(this)
                    .setTitle(R.string.frame_capture_permission_needed_title)
                    .setMessage(R.string.frame_capture_permission_needed_message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.delete_cancel, (d, w) -> showSaveFailed())
                    .setPositiveButton(R.string.frame_capture_permission_grant, (d, w) -> {
                        Intent treeIntent = StorageVolumes.createTreeIntentForVolume(this, volumeRoot);
                        treePickerLauncher.launch(treeIntent);
                    })
                    .show());
        });
    }

    private void onTreePicked(androidx.activity.result.ActivityResult result) {
        Uri treeUri = result.getData() != null ? result.getData().getData() : null;
        if (treeUri == null || pendingByteList == null) {
            showSaveFailed();
            return;
        }
        getContentResolver().takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        VolumePermissions.save(this, pendingVolumeRoot, treeUri);

        File volumeRoot = new File(pendingVolumeRoot);
        File parentDir = videoFile.getParentFile();
        List<String> filenames = pendingFilenames;
        List<byte[]> byteList = pendingByteList;
        setBusy(true);

        bgExecutor.execute(() -> {
            int[] outcome = writeAllViaSaf(treeUri, volumeRoot, parentDir, filenames, byteList);
            if (outcome[0] > 0 || outcome[1] == 0) {
                onSaveSucceeded(null, outcome[0], outcome[1]);
            } else {
                mainHandler.post(this::showSaveFailed);
            }
        });
    }

    /** Walks the granted document tree down to the video's folder and
     *  creates each new frame file there. Returns {successCount, failCount}. */
    private int[] writeAllViaSaf(Uri treeUri, File volumeRoot, File parentDir, List<String> filenames, List<byte[]> byteList) {
        int success = 0;
        int fail = 0;
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(this, treeUri);
            if (dir == null || parentDir == null) return new int[]{0, filenames.size()};

            for (String segment : StorageVolumes.relativeFolderSegments(volumeRoot, new File(parentDir, "placeholder"))) {
                dir = dir.findFile(segment);
                if (dir == null) return new int[]{0, filenames.size()};
            }

            for (int i = 0; i < filenames.size(); i++) {
                try {
                    DocumentFile newDoc = dir.createFile("image/jpeg", filenames.get(i));
                    if (newDoc == null) {
                        fail++;
                        continue;
                    }
                    try (OutputStream out = getContentResolver().openOutputStream(newDoc.getUri())) {
                        if (out == null) {
                            fail++;
                            continue;
                        }
                        out.write(byteList.get(i));
                        out.flush();
                    }
                    success++;
                } catch (Exception e) {
                    fail++;
                }
            }
        } catch (Exception e) {
            return new int[]{success, filenames.size() - success};
        }
        return new int[]{success, fail};
    }

    private void onSaveSucceeded(@Nullable List<String> directPaths, int successCount, int failCount) {
        if (directPaths != null && !directPaths.isEmpty()) {
            try {
                MediaScannerConnection.scanFile(getApplicationContext(), directPaths.toArray(new String[0]), null, null);
            } catch (Exception ignored) {
            }
        }

        mainHandler.post(() -> {
            setBusy(false);
            if (failCount == 0) {
                Toast.makeText(this, getString(R.string.frame_capture_success, successCount), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.frame_capture_partial_fail, successCount, failCount), Toast.LENGTH_LONG).show();
            }
            Intent data = new Intent();
            data.putExtra(EXTRA_CAPTURE_SUCCEEDED, successCount > 0);
            setResult(Activity.RESULT_OK, data);
            finish();
        });
    }

    private void showSaveFailed() {
        setBusy(false);
        Toast.makeText(this, R.string.frame_capture_failed, Toast.LENGTH_SHORT).show();
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
        if (retriever != null) {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }
}
