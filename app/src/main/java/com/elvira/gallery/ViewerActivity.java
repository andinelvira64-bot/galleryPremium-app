package com.elvira.gallery;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewerActivity extends AppCompatActivity implements SensorEventListener {

    public static final String EXTRA_PATHS = "extra_paths";
    public static final String EXTRA_TYPES = "extra_types";
    public static final String EXTRA_START_POSITION = "extra_start_position";
    /** Set true on the result Intent when a delete happened here, so
     *  MainActivity knows to rescan the grid when the user comes back. */
    public static final String EXTRA_DELETED_OCCURRED = "extra_deleted_occurred";

    private static final int DEFAULT_DARK_COLOR = 0xFF141110;
    private static final float PARALLAX_MAX_DP = 22f;

    private ViewPager2 pager;
    private MediaPagerAdapter pagerAdapter;
    private ImageView backgroundLayer;
    private android.view.View glowOverlay;
    private ImageButton btnClose;
    private ImageButton btnMenu;
    private boolean controlsButtonsVisible = true;
    private boolean deletedOccurred = false;
    /** Whether a video is currently being watched forced into landscape,
     *  with the status/navigation bars hidden for a true fullscreen feel. */
    private boolean isVideoFullscreenLandscape = false;

    private final ActivityResultLauncher<Intent> deletePermissionTreeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::handleDeleteTreePicked);
    private final DeleteFlow deleteFlow = new DeleteFlow(this, deletePermissionTreeLauncher);

    private void handleDeleteTreePicked(androidx.activity.result.ActivityResult result) {
        deleteFlow.onTreePicked(result.getData());
    }

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::handleCropResult);

    private void handleCropResult(androidx.activity.result.ActivityResult result) {
        boolean succeeded = result.getData() != null
                && result.getData().getBooleanExtra(CropActivity.EXTRA_CROP_SUCCEEDED, false);
        if (!succeeded) return;

        int position = pager.getCurrentItem();
        // The file on disk changed in place (same path); this refreshes both
        // the pager's photo (Glide reloads via its last-modified signature)
        // and the blurred/colored backdrop derived from it.
        pagerAdapter.notifyItemChanged(position);
        applyEffectsForPosition(position);
    }

    private ArrayList<String> paths;
    private ArrayList<Integer> types;

    private SettingsPrefs prefs;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private final ExecutorService effectsExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private float parallaxMaxPx;
    private int currentDominantColor = DEFAULT_DARK_COLOR;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        prefs = new SettingsPrefs(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        parallaxMaxPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PARALLAX_MAX_DP,
                getResources().getDisplayMetrics());

        pager = findViewById(R.id.pager);
        backgroundLayer = findViewById(R.id.backgroundLayer);
        glowOverlay = findViewById(R.id.glowOverlay);
        btnClose = findViewById(R.id.btnClose);
        btnMenu = findViewById(R.id.btnMenu);
        btnClose.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(this::showViewerMenu);
        setResult(Activity.RESULT_CANCELED, buildResultIntent());

        paths = getIntent().getStringArrayListExtra(EXTRA_PATHS);
        types = getIntent().getIntegerArrayListExtra(EXTRA_TYPES);
        int startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (paths == null && Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            String resolvedPath = ExternalUriResolver.resolveAbsolutePath(this, getIntent().getData());
            if (resolvedPath == null) {
                Toast.makeText(this, R.string.open_external_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            paths = new ArrayList<>();
            paths.add(resolvedPath);
            types = new ArrayList<>();
            types.add(MediaScanner.isVideoExtension(resolvedPath) ? MediaItem.TYPE_VIDEO : MediaItem.TYPE_PHOTO);
            startPosition = 0;
        }

        if (paths == null) paths = new ArrayList<>();
        if (types == null) types = new ArrayList<>();

        MediaPagerAdapter adapter = new MediaPagerAdapter(paths, types);
        pagerAdapter = adapter;
        // Keep the back and menu buttons in lockstep with the current page's
        // own controls: a single tap on either a photo or a video hides (or
        // reveals) all of them together, exactly like the play/pause and
        // skip buttons on a video.
        pagerAdapter.setControlsVisibilityListener((position, visible) -> {
            if (position == pager.getCurrentItem()) {
                setControlsButtonsVisible(visible);
            }
        });
        // When a video finishes, auto-advance to the next photo/video (if any).
        // The next page only truly starts playing once ViewPager2 makes it the
        // active page (see MediaPagerAdapter.setActivePosition), so it can't
        // have been silently playing/finishing in the background already.
        pagerAdapter.setVideoCompletionListener(position -> {
            if (position != pager.getCurrentItem()) return; // stale callback from a recycled/backgrounded page
            int next = position + 1;
            if (next < pagerAdapter.getItemCount()) {
                pager.setCurrentItem(next, true);
            }
        });
        // While a photo is pinch-zoomed in, a one-finger pan needs to move the
        // photo around rather than get stolen as a page-swipe by ViewPager2.
        pagerAdapter.setZoomStateListener((position, zoomedIn) -> {
            if (position == pager.getCurrentItem()) {
                pager.setUserInputEnabled(!zoomedIn);
            }
        });
        // The tap-to-reveal fullscreen/landscape button on a video page has no
        // window access of its own, so it just asks the activity to actually
        // rotate the screen and hide the system bars.
        pagerAdapter.setFullscreenToggleListener(this::toggleVideoFullscreenLandscape);
        pager.setAdapter(adapter);
        pager.setCurrentItem(startPosition, false);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pagerAdapter.setActivePosition(position);
                applyEffectsForPosition(position);
                // A freshly landed-on page (photo or video) starts out with
                // its controls, and the shared back/menu buttons, visible.
                // Videos auto-hide theirs a few seconds after playback
                // starts; photos only hide theirs on a tap.
                setControlsButtonsVisible(true);
                // A newly-landed-on page always starts at 1x zoom, so paging
                // (which the previous page may have disabled while zoomed) is
                // always allowed again here.
                pager.setUserInputEnabled(true);
            }
        });

        // onPageSelected is only fired for changes after this point, so the very
        // first page opened needs its "active" state set explicitly here.
        pagerAdapter.setActivePosition(startPosition);

        applyEffectsForPosition(startPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeRegisterSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void maybeRegisterSensor() {
        if (prefs.isParallaxEnabled() && sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            backgroundLayer.animate().translationX(0).translationY(0).setDuration(200).start();
        }
    }

    private void setControlsButtonsVisible(boolean visible) {
        if (controlsButtonsVisible == visible) return;
        controlsButtonsVisible = visible;
        animateButton(btnClose, visible);
        animateButton(btnMenu, visible);
    }

    private void animateButton(ImageButton button, boolean visible) {
        button.animate().cancel();
        if (visible) {
            button.setVisibility(android.view.View.VISIBLE);
            button.animate().alpha(1f).setDuration(150).start();
        } else {
            button.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> button.setVisibility(android.view.View.GONE)).start();
        }
    }

    // ---------------------------------------------------------------
    // Video landscape / fullscreen toggle
    // ---------------------------------------------------------------

    /** Flips between the video's normal orientation and a forced landscape,
     *  fully-immersive (status/nav bars hidden) view, then tells the adapter
     *  so the on-screen button swaps to the matching enter/exit icon. The
     *  activity declares configChanges="orientation|screenSize" in the
     *  manifest, so this rotation never recreates the activity or interrupts
     *  playback. */
    private void toggleVideoFullscreenLandscape() {
        isVideoFullscreenLandscape = !isVideoFullscreenLandscape;
        setRequestedOrientation(isVideoFullscreenLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        applyImmersiveMode(isVideoFullscreenLandscape);
        pagerAdapter.setFullscreenState(isVideoFullscreenLandscape);
    }

    private void applyImmersiveMode(boolean immersive) {
        if (immersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Swiping in the status bar temporarily clears the immersive flags;
        // re-apply them once the window regains focus so it doesn't get
        // stuck showing the system bars while still in landscape mode.
        if (hasFocus && isVideoFullscreenLandscape) {
            applyImmersiveMode(true);
        }
    }

    // ---------------------------------------------------------------
    // Overflow menu ("...") - currently just permanent delete of the
    // photo/video that's actually open right now.
    // ---------------------------------------------------------------

    private void showViewerMenu(android.view.View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.viewer_menu, popup.getMenu());

        int position = pager.getCurrentItem();
        boolean isPhoto = position >= 0 && position < types.size()
                && types.get(position) != MediaItem.TYPE_VIDEO;
        MenuItem cropItem = popup.getMenu().findItem(R.id.action_crop_current);
        if (cropItem != null) {
            cropItem.setVisible(isPhoto);
        }
        MenuItem captureFrameItem = popup.getMenu().findItem(R.id.action_capture_frame_current);
        if (captureFrameItem != null) {
            captureFrameItem.setVisible(!isPhoto);
        }

        popup.setOnMenuItemClickListener(this::onViewerMenuItemClick);
        popup.show();
    }

    private boolean onViewerMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_current) {
            confirmDeleteCurrent();
            return true;
        }
        if (item.getItemId() == R.id.action_crop_current) {
            openCropForCurrent();
            return true;
        }
        if (item.getItemId() == R.id.action_capture_frame_current) {
            openFrameCaptureForCurrent();
            return true;
        }
        return false;
    }

    private void openFrameCaptureForCurrent() {
        int position = pager.getCurrentItem();
        if (position < 0 || position >= paths.size()) return;

        Intent intent = new Intent(this, FrameCaptureActivity.class);
        intent.putExtra(FrameCaptureActivity.EXTRA_VIDEO_PATH, paths.get(position));
        startActivity(intent);
    }

    private void openCropForCurrent() {
        int position = pager.getCurrentItem();
        if (position < 0 || position >= paths.size()) return;

        Intent intent = new Intent(this, CropActivity.class);
        intent.putExtra(CropActivity.EXTRA_PATH, paths.get(position));
        cropLauncher.launch(intent);
    }

    private void confirmDeleteCurrent() {
        int position = pager.getCurrentItem();
        if (position < 0 || position >= paths.size()) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message_single)
                .setNegativeButton(R.string.delete_cancel, null)
                .setPositiveButton(R.string.delete_yes, (dialog, which) -> deleteCurrent(position))
                .show();
    }

    private void deleteCurrent(int position) {
        String path = paths.get(position);
        int type = types.get(position);
        MediaItem item = new MediaItem(new java.io.File(path), type, 0L);

        deleteFlow.start(java.util.Collections.singletonList(item), (deletedCount, failedCount) -> {
            if (deletedCount > 0) {
                deletedOccurred = true;
                Toast.makeText(this, getString(R.string.delete_success, deletedCount), Toast.LENGTH_SHORT).show();
                // Simplest safe behavior: go back to the grid, which will
                // rescan and no longer show the file that's now gone. Trying
                // to keep the pager open and reshuffle its internal video
                // playback bookkeeping in place is fragile; returning to the
                // grid avoids that risk entirely.
                setResult(Activity.RESULT_OK, buildResultIntent());
                finish();
            } else {
                Toast.makeText(this, R.string.delete_partial_fail_single, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Intent buildResultIntent() {
        Intent data = new Intent();
        data.putExtra(EXTRA_DELETED_OCCURRED, deletedOccurred);
        return data;
    }

    // ---------------------------------------------------------------
    // Effects: auto coloring, blur backdrop, glow
    // ---------------------------------------------------------------

    private void applyEffectsForPosition(int position) {
        if (position < 0 || position >= paths.size()) return;
        String path = paths.get(position);
        boolean isVideo = types.get(position) == MediaItem.TYPE_VIDEO;

        boolean blurOn = prefs.isBlurEnabled();
        boolean glowOn = prefs.isGlowEnabled();
        boolean colorOn = prefs.isAutoColoringEnabled();

        effectsExecutor.execute(() -> {
            Bitmap sample = isVideo ? extractVideoFrame(path) : decodeSampledBitmap(path, 200, 200);
            if (sample == null) {
                mainHandler.post(() -> applyFallbackBackground(blurOn, glowOn));
                return;
            }

            int dominant = DEFAULT_DARK_COLOR;
            if (colorOn) {
                Palette palette = Palette.from(sample).generate();
                dominant = palette.getDominantColor(DEFAULT_DARK_COLOR);
            }

            Bitmap backdrop = blurOn ? BlurUtils.createBlurredBackdrop(sample, 12) : sample;
            int finalDominant = dominant;

            mainHandler.post(() -> {
                currentDominantColor = finalDominant;
                backgroundLayer.setAlpha(blurOn ? 0.55f : 0.35f);
                backgroundLayer.setImageBitmap(backdrop);
                applyGlow(glowOn, finalDominant);
            });
        });
    }

    private void applyFallbackBackground(boolean blurOn, boolean glowOn) {
        backgroundLayer.setImageBitmap(null);
        backgroundLayer.setImageDrawable(null);
        backgroundLayer.setBackgroundColor(DEFAULT_DARK_COLOR);
        applyGlow(glowOn, DEFAULT_DARK_COLOR);
    }

    private void applyGlow(boolean glowOn, int color) {
        if (!glowOn) {
            glowOverlay.animate().alpha(0f).setDuration(250).start();
            return;
        }
        int glowColor = Color.argb(140, Color.red(color), Color.green(color), Color.blue(color));
        int transparent = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{glowColor, transparent, transparent, glowColor});
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        glowOverlay.setBackground(gradient);
        glowOverlay.animate().alpha(0.5f).setDuration(250).start();
    }

    // ---------------------------------------------------------------
    // Bitmap helpers
    // ---------------------------------------------------------------

    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            int inSampleSize = 1;
            int halfWidth = opts.outWidth / 2;
            int halfHeight = opts.outHeight / 2;
            while ((halfWidth / inSampleSize) >= reqWidth && (halfHeight / inSampleSize) >= reqHeight) {
                inSampleSize *= 2;
            }
            opts.inSampleSize = Math.max(1, inSampleSize);
            opts.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap extractVideoFrame(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                return retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 200, 200);
            }
            return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    // ---------------------------------------------------------------
    // Parallax via accelerometer
    // ---------------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!prefs.isParallaxEnabled()) return;
        // event.values[0] = tilt left/right, event.values[1] = tilt front/back
        float tiltX = -event.values[0];
        float tiltY = event.values[1];

        float dx = clamp(tiltX / 9.8f, -1f, 1f) * parallaxMaxPx;
        float dy = clamp(tiltY / 9.8f, -1f, 1f) * parallaxMaxPx;

        backgroundLayer.setTranslationX(dx);
        backgroundLayer.setTranslationY(dy);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        effectsExecutor.shutdownNow();
    }
}
