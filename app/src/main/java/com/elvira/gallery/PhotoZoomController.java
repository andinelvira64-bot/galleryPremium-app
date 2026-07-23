package com.elvira.gallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

/**
 * Adds two-finger pinch-to-zoom and (once zoomed in) one-finger panning to a
 * photo page - no on-screen zoom buttons needed, exactly like a stock
 * Photos app. A plain single tap still toggles the shared back/menu
 * buttons, same as before.
 *
 * The touch listener is attached to the transparent tapCatcher view that
 * already sits above the photo purely to receive touches; the resulting
 * transform is applied to the real ImageView underneath via
 * {@link ImageView#setImageMatrix}, so this class never needs to know
 * anything about Glide or how the bitmap got there.
 */
public class PhotoZoomController {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;
    private static final float PAN_SLOP_PX = 8f;

    public interface Callbacks {
        /** Fired whenever the zoomed-in/out state changes, so the host can
         *  disable ViewPager2 swiping while the user pans a zoomed photo
         *  (otherwise a horizontal pan gets stolen as a page-swipe). */
        void onZoomChanged(boolean zoomedIn);

        /** Fired on a genuine single tap (no drag, no pinch) so the host can
         *  toggle the shared back/menu buttons exactly like before. */
        void onSingleTap();
    }

    private final ImageView imageView;
    private final Callbacks callbacks;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private final Matrix matrix = new Matrix();
    private final Matrix baseMatrix = new Matrix();
    private float currentScale = 1f;
    private boolean zoomedIn = false;

    private float lastPanX, lastPanY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isPanning = false;

    public PhotoZoomController(View tapCatcher, ImageView imageView, Callbacks callbacks) {
        this.imageView = imageView;
        this.callbacks = callbacks;
        Context context = tapCatcher.getContext();

        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                callbacks.onSingleTap();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > MIN_SCALE + 0.01f) {
                    animateToScale(MIN_SCALE, imageView.getWidth() / 2f, imageView.getHeight() / 2f);
                } else {
                    animateToScale(DOUBLE_TAP_SCALE, e.getX(), e.getY());
                }
                return true;
            }
        });

        tapCatcher.setOnTouchListener(this::handleTouch);
    }

    /** Call whenever a new (or newly recycled) photo finishes loading into
     *  the ImageView, so the base "fit" matrix and zoom state reset for it. */
    public void resetForNewImage() {
        imageView.post(() -> {
            Drawable d = imageView.getDrawable();
            int viewW = imageView.getWidth();
            int viewH = imageView.getHeight();
            if (d == null || viewW == 0 || viewH == 0) return;

            int drawableW = d.getIntrinsicWidth();
            int drawableH = d.getIntrinsicHeight();
            if (drawableW <= 0 || drawableH <= 0) return;

            baseMatrix.reset();
            float scale = Math.min((float) viewW / drawableW, (float) viewH / drawableH);
            float dx = (viewW - drawableW * scale) / 2f;
            float dy = (viewH - drawableH * scale) / 2f;
            baseMatrix.setScale(scale, scale);
            baseMatrix.postTranslate(dx, dy);

            matrix.set(baseMatrix);
            currentScale = 1f;
            setZoomedIn(false);
            imageView.setImageMatrix(matrix);
        });
    }

    private boolean handleTouch(View v, MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        handlePanning(event);
        return true;
    }

    private void handlePanning(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastPanX = event.getX();
                lastPanY = event.getY();
                isPanning = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // A second finger just went down - ScaleGestureDetector owns
                // the gesture now; panning resumes with whichever finger stays.
                isPanning = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                if (currentScale <= MIN_SCALE + 0.01f) break; // nothing to pan when fully zoomed out
                if (event.getPointerCount() > 1) break; // mid-pinch, ScaleGestureDetector handles it
                int idx = event.findPointerIndex(activePointerId);
                if (idx < 0) break;
                float x = event.getX(idx);
                float y = event.getY(idx);
                float dx = x - lastPanX;
                float dy = y - lastPanY;
                if (!isPanning && (Math.abs(dx) > PAN_SLOP_PX || Math.abs(dy) > PAN_SLOP_PX)) {
                    isPanning = true;
                }
                if (isPanning) {
                    matrix.postTranslate(dx, dy);
                    clampTranslation();
                    imageView.setImageMatrix(matrix);
                }
                lastPanX = x;
                lastPanY = y;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newIndex = pointerIndex == 0 ? 1 : 0;
                    if (newIndex < event.getPointerCount()) {
                        activePointerId = event.getPointerId(newIndex);
                        lastPanX = event.getX(newIndex);
                        lastPanY = event.getY(newIndex);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPanning = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
        }
    }

    private void animateToScale(float targetScale, float focusX, float focusY) {
        final float startScale = currentScale;
        final Matrix startMatrix = new Matrix(matrix);
        final float totalFactor = targetScale / startScale;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(220);
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float stepFactor = 1f + (totalFactor - 1f) * t;
            matrix.set(startMatrix);
            matrix.postScale(stepFactor, stepFactor, focusX, focusY);
            currentScale = startScale * stepFactor;
            clampTranslation();
            imageView.setImageMatrix(matrix);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentScale = targetScale;
                clampTranslation();
                imageView.setImageMatrix(matrix);
                setZoomedIn(currentScale > MIN_SCALE + 0.01f);
            }
        });
        animator.start();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newScale = currentScale * factor;
            newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
            float appliedFactor = newScale / currentScale;

            matrix.postScale(appliedFactor, appliedFactor, detector.getFocusX(), detector.getFocusY());
            currentScale = newScale;
            clampTranslation();
            imageView.setImageMatrix(matrix);
            setZoomedIn(currentScale > MIN_SCALE + 0.01f);
            return true;
        }
    }

    /** Keeps the image from being dragged/zoomed away until there's empty
     *  space showing where the photo used to be - the panned/zoomed image
     *  always still covers the whole view, like a standard photo viewer. */
    private void clampTranslation() {
        Drawable d = imageView.getDrawable();
        if (d == null) return;
        int viewW = imageView.getWidth();
        int viewH = imageView.getHeight();

        RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rect);

        float dx = 0, dy = 0;
        if (rect.width() <= viewW) {
            dx = (viewW - rect.width()) / 2f - rect.left;
        } else {
            if (rect.left > 0) dx = -rect.left;
            else if (rect.right < viewW) dx = viewW - rect.right;
        }
        if (rect.height() <= viewH) {
            dy = (viewH - rect.height()) / 2f - rect.top;
        } else {
            if (rect.top > 0) dy = -rect.top;
            else if (rect.bottom < viewH) dy = viewH - rect.bottom;
        }
        matrix.postTranslate(dx, dy);
    }

    private void setZoomedIn(boolean value) {
        if (zoomedIn != value) {
            zoomedIn = value;
            if (callbacks != null) callbacks.onZoomChanged(zoomedIn);
        }
    }

    public boolean isZoomedIn() {
        return zoomedIn;
    }
}
