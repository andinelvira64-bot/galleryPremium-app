package com.elvira.gallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lets the user pick a rectangular region out of the photo currently open in
 * ViewerActivity and permanently crops the file to that region, overwriting
 * it in place (same path, so the pager/grid just show the new content).
 *
 * The screen only ever displays a downsampled preview (so very large photos
 * don't need to be fully decoded into memory just to be looked at), but the
 * actual crop is performed against the original full-resolution file via
 * BitmapRegionDecoder, so the saved result isn't limited to preview quality.
 */
public class CropActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "extra_crop_path";
    public static final String EXTRA_CROP_SUCCEEDED = "extra_crop_succeeded";

    private static final int MAX_PREVIEW_DIMENSION = 1600;

    private ImageView imageView;
    private CropOverlayView overlay;
    private ProgressBar progressBar;
    private TextView btnAspectFree;
    private TextView btnAspectSquare;

    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String sourcePath;
    /** Degrees the raw file needs to be rotated clockwise to display upright,
     *  read from EXIF once up front and reused for every coordinate mapping. */
    private int rotationDegrees = 0;
    private int rawWidth;
    private int rawHeight;
    private Bitmap previewBitmap;

    private String pendingVolumeRoot;
    private byte[] pendingSaveBytes;
    private final ActivityResultLauncher<Intent> treePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onTreePicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        imageView = findViewById(R.id.cropImageView);
        overlay = findViewById(R.id.cropOverlay);
        progressBar = findViewById(R.id.cropProgress);
        btnAspectFree = findViewById(R.id.btnAspectFree);
        btnAspectSquare = findViewById(R.id.btnAspectSquare);
        ImageButton btnCancel = findViewById(R.id.btnCropCancel);
        ImageButton btnConfirm = findViewById(R.id.btnCropConfirm);

        sourcePath = getIntent().getStringExtra(EXTRA_PATH);
        setResult(Activity.RESULT_CANCELED);

        btnCancel.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> performCrop());
        btnAspectFree.setOnClickListener(v -> selectAspect(false));
        btnAspectSquare.setOnClickListener(v -> selectAspect(true));

        if (sourcePath == null) {
            Toast.makeText(this, R.string.crop_load_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPreview();
    }

    private void selectAspect(boolean square) {
        overlay.setSquareLocked(square);
        btnAspectFree.setTextColor(square ? 0xFFB8ACA5 : 0xFFFFFFFF);
        btnAspectSquare.setTextColor(square ? 0xFFFFFFFF : 0xFFB8ACA5);
    }

    // ---------------------------------------------------------------
    // Loading a downsampled, upright preview
    // ---------------------------------------------------------------

    private void loadPreview() {
        setBusy(true);
        bgExecutor.execute(() -> {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(sourcePath, bounds);
                rawWidth = bounds.outWidth;
                rawHeight = bounds.outHeight;

                if (rawWidth <= 0 || rawHeight <= 0) {
                    mainHandler.post(this::showLoadFailedAndFinish);
                    return;
                }

                rotationDegrees = readExifRotation(sourcePath);

                int uprightW = (rotationDegrees == 90 || rotationDegrees == 270) ? rawHeight : rawWidth;
                int uprightH = (rotationDegrees == 90 || rotationDegrees == 270) ? rawWidth : rawHeight;

                int sampleSize = 1;
                while ((uprightW / sampleSize) > MAX_PREVIEW_DIMENSION || (uprightH / sampleSize) > MAX_PREVIEW_DIMENSION) {
                    sampleSize *= 2;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sampleSize;
                Bitmap sampledRaw = BitmapFactory.decodeFile(sourcePath, opts);
                if (sampledRaw == null) {
                    mainHandler.post(this::showLoadFailedAndFinish);
                    return;
                }

                Bitmap upright;
                if (rotationDegrees != 0) {
                    Matrix rotate = new Matrix();
                    rotate.postRotate(rotationDegrees);
                    upright = Bitmap.createBitmap(sampledRaw, 0, 0, sampledRaw.getWidth(), sampledRaw.getHeight(), rotate, true);
                    if (upright != sampledRaw) sampledRaw.recycle();
                } else {
                    upright = sampledRaw;
                }

                Bitmap finalUpright = upright;
                mainHandler.post(() -> onPreviewReady(finalUpright));
            } catch (Exception e) {
                mainHandler.post(this::showLoadFailedAndFinish);
            }
        });
    }

    private int readExifRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void onPreviewReady(Bitmap upright) {
        previewBitmap = upright;
        imageView.setImageBitmap(upright);
        setBusy(false);

        imageView.post(() -> {
            int viewW = imageView.getWidth();
            int viewH = imageView.getHeight();
            if (viewW == 0 || viewH == 0 || upright == null) return;

            float scale = Math.min((float) viewW / upright.getWidth(), (float) viewH / upright.getHeight());
            float displayW = upright.getWidth() * scale;
            float displayH = upright.getHeight() * scale;
            float left = (viewW - displayW) / 2f;
            float top = (viewH - displayH) / 2f;

            overlay.setImageBounds(new RectF(left, top, left + displayW, top + displayH));
        });
    }

    private void showLoadFailedAndFinish() {
        setBusy(false);
        Toast.makeText(this, R.string.crop_load_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------
    // Performing the actual full-resolution crop
    // ---------------------------------------------------------------

    private void performCrop() {
        RectF normalized = overlay.getNormalizedCropRect();
        setBusy(true);

        bgExecutor.execute(() -> {
            try {
                int uprightW = (rotationDegrees == 90 || rotationDegrees == 270) ? rawHeight : rawWidth;
                int uprightH = (rotationDegrees == 90 || rotationDegrees == 270) ? rawWidth : rawHeight;

                RectF uprightCropPx = new RectF(
                        normalized.left * uprightW,
                        normalized.top * uprightH,
                        normalized.right * uprightW,
                        normalized.bottom * uprightH);

                // Map the crop rectangle (picked in upright space, matching what
                // the user actually saw) back into the raw file's own pixel
                // space, using the exact inverse of the same rotation transform
                // used to build the upright preview above.
                Matrix rawToUpright = new Matrix();
                rawToUpright.setRotate(rotationDegrees);
                RectF rawBoundsMapped = new RectF(0, 0, rawWidth, rawHeight);
                rawToUpright.mapRect(rawBoundsMapped);
                rawToUpright.postTranslate(-rawBoundsMapped.left, -rawBoundsMapped.top);

                Matrix uprightToRaw = new Matrix();
                if (!rawToUpright.invert(uprightToRaw)) {
                    mainHandler.post(this::showSaveFailed);
                    return;
                }

                RectF rawCropRectF = new RectF(uprightCropPx);
                uprightToRaw.mapRect(rawCropRectF);

                Rect rawCropRect = new Rect(
                        clampInt(Math.round(rawCropRectF.left), 0, rawWidth),
                        clampInt(Math.round(rawCropRectF.top), 0, rawHeight),
                        clampInt(Math.round(rawCropRectF.right), 0, rawWidth),
                        clampInt(Math.round(rawCropRectF.bottom), 0, rawHeight));

                if (rawCropRect.width() < 1 || rawCropRect.height() < 1) {
                    mainHandler.post(this::showSaveFailed);
                    return;
                }

                @SuppressWarnings("deprecation")
                BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(sourcePath, false);
                if (regionDecoder == null) {
                    mainHandler.post(this::showSaveFailed);
                    return;
                }
                Bitmap rawCropped = regionDecoder.decodeRegion(rawCropRect, null);
                regionDecoder.recycle();
                if (rawCropped == null) {
                    mainHandler.post(this::showSaveFailed);
                    return;
                }

                Bitmap finalCropped;
                if (rotationDegrees != 0) {
                    Matrix rotate = new Matrix();
                    rotate.postRotate(rotationDegrees);
                    finalCropped = Bitmap.createBitmap(rawCropped, 0, 0, rawCropped.getWidth(), rawCropped.getHeight(), rotate, true);
                    if (finalCropped != rawCropped) rawCropped.recycle();
                } else {
                    finalCropped = rawCropped;
                }

                boolean isPng = sourcePath.toLowerCase(Locale.ROOT).endsWith(".png");
                Bitmap.CompressFormat format = isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                finalCropped.compress(format, 92, baos);
                finalCropped.recycle();
                byte[] bytes = baos.toByteArray();

                saveBytesToSource(bytes);
            } catch (Exception e) {
                mainHandler.post(this::showSaveFailed);
            }
        });
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ---------------------------------------------------------------
    // Writing the result back to disk (direct write, with an SD-card /
    // Storage-Access-Framework fallback matching the rest of the app).
    // ---------------------------------------------------------------

    private void saveBytesToSource(byte[] bytes) {
        File file = new File(sourcePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
            onSaveSucceeded();
            return;
        } catch (Exception ignored) {
            // Falls through to the SAF path below - typically only needed on
            // an SD card the app doesn't have direct File-API write access to.
        }

        File volumeRoot = StorageVolumes.resolveVolumeRoot(this, file);
        if (StorageVolumes.isPrimaryVolume(this, volumeRoot)) {
            mainHandler.post(this::showSaveFailed);
            return;
        }

        Uri savedTree = VolumePermissions.get(this, volumeRoot.getAbsolutePath());
        if (savedTree != null && writeViaSaf(savedTree, volumeRoot, file, bytes)) {
            onSaveSucceeded();
            return;
        }

        // No saved permission (or it no longer works) - ask the user to pick
        // that volume's folder once, then retry, exactly like DeleteFlow does.
        pendingVolumeRoot = volumeRoot.getAbsolutePath();
        pendingSaveBytes = bytes;
        mainHandler.post(() -> new AlertDialog.Builder(this)
                .setTitle(R.string.crop_permission_needed_title)
                .setMessage(R.string.crop_permission_needed_message)
                .setCancelable(false)
                .setNegativeButton(R.string.delete_cancel, (d, w) -> showSaveFailed())
                .setPositiveButton(R.string.crop_permission_grant, (d, w) -> {
                    Intent treeIntent = StorageVolumes.createTreeIntentForVolume(this, volumeRoot);
                    treePickerLauncher.launch(treeIntent);
                })
                .show());
    }

    private void onTreePicked(androidx.activity.result.ActivityResult result) {
        Uri treeUri = result.getData() != null ? result.getData().getData() : null;
        if (treeUri == null || pendingSaveBytes == null) {
            showSaveFailed();
            return;
        }
        getContentResolver().takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        VolumePermissions.save(this, pendingVolumeRoot, treeUri);

        File file = new File(sourcePath);
        File volumeRoot = new File(pendingVolumeRoot);
        byte[] bytes = pendingSaveBytes;
        setBusy(true);
        bgExecutor.execute(() -> {
            if (writeViaSaf(treeUri, volumeRoot, file, bytes)) {
                onSaveSucceeded();
            } else {
                mainHandler.post(this::showSaveFailed);
            }
        });
    }

    /** Walks the granted document tree down to the file's folder and
     *  overwrites the matching document's content in place. */
    private boolean writeViaSaf(Uri treeUri, File volumeRoot, File file, byte[] bytes) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(this, treeUri);
            if (dir == null) return false;
            for (String segment : StorageVolumes.relativeFolderSegments(volumeRoot, file)) {
                dir = dir.findFile(segment);
                if (dir == null) return false;
            }
            DocumentFile target = dir.findFile(file.getName());
            if (target == null) return false;

            try (OutputStream out = getContentResolver().openOutputStream(target.getUri(), "wt")) {
                if (out == null) return false;
                out.write(bytes);
                out.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onSaveSucceeded() {
        try {
            android.media.MediaScannerConnection.scanFile(getApplicationContext(), new String[]{sourcePath}, null, null);
        } catch (Exception ignored) {
        }
        mainHandler.post(() -> {
            setBusy(false);
            Toast.makeText(this, R.string.crop_success, Toast.LENGTH_SHORT).show();
            Intent data = new Intent();
            data.putExtra(EXTRA_CROP_SUCCEEDED, true);
            setResult(Activity.RESULT_OK, data);
            finish();
        });
    }

    private void showSaveFailed() {
        setBusy(false);
        Toast.makeText(this, R.string.crop_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
        if (previewBitmap != null && !previewBitmap.isRecycled()) {
            previewBitmap.recycle();
        }
    }
}
