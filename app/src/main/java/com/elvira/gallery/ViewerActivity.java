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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
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

    private static final int DEFAULT_DARK_COLOR = 0xFF141110;
    private static final float PARALLAX_MAX_DP = 22f;

    private ViewPager2 pager;
    private ImageView backgroundLayer;
    private android.view.View glowOverlay;

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
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        paths = getIntent().getStringArrayListExtra(EXTRA_PATHS);
        types = getIntent().getIntegerArrayListExtra(EXTRA_TYPES);
        int startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (paths == null) paths = new ArrayList<>();
        if (types == null) types = new ArrayList<>();

        MediaPagerAdapter adapter = new MediaPagerAdapter(paths, types);
        pager.setAdapter(adapter);
        pager.setCurrentItem(startPosition, false);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                applyEffectsForPosition(position);
            }
        });

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
