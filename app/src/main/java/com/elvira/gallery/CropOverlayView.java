package com.elvira.gallery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Transparent view drawn on top of the photo being cropped. Shows a
 * draggable, resizable rectangle (dim scrim outside it, corner handles) that
 * the user drags into place over the part of the photo they want to keep -
 * exactly like a standard photo-editor crop tool.
 *
 * This view only knows about its own pixel space; {@link CropActivity} is
 * responsible for translating {@link #getNormalizedCropRect()} (a 0..1
 * fraction of the displayed image, independent of any letterboxing) into
 * real image pixel coordinates.
 */
public class CropOverlayView extends View {

    private static final float MIN_SIZE_DP = 48f;
    private static final float HANDLE_TOUCH_RADIUS_DP = 28f;
    private static final float HANDLE_DRAW_LEN_DP = 18f;

    private enum DragMode { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private final RectF imageBounds = new RectF();
    private final RectF cropRect = new RectF();
    private boolean squareLocked = false;

    private DragMode dragMode = DragMode.NONE;
    private float lastTouchX, lastTouchY;

    private final Paint scrimPaint = new Paint();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path scrimPath = new Path();

    private float minSizePx;
    private float handleTouchRadiusPx;
    private float handleDrawLenPx;

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        minSizePx = dp(MIN_SIZE_DP);
        handleTouchRadiusPx = dp(HANDLE_TOUCH_RADIUS_DP);
        handleDrawLenPx = dp(HANDLE_DRAW_LEN_DP);

        scrimPaint.setColor(Color.parseColor("#99000000"));
        scrimPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1.5f));

        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeWidth(dp(3.5f));
        handlePaint.setStrokeCap(Paint.Cap.ROUND);

        gridPaint.setColor(Color.parseColor("#66FFFFFF"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    /** Called once the real image has been decoded and laid out, with the
     *  rectangle (in this view's own coordinates) that the photo actually
     *  occupies inside the ImageView (accounting for fitCenter letterboxing). */
    public void setImageBounds(RectF bounds) {
        imageBounds.set(bounds);
        resetCropRect();
    }

    /** Locks/unlocks a 1:1 aspect ratio and immediately re-centers the crop
     *  rectangle to the largest square (or, when unlocked, the default
     *  inset rectangle) that fits inside the current image bounds. */
    public void setSquareLocked(boolean locked) {
        squareLocked = locked;
        resetCropRect();
    }

    private void resetCropRect() {
        if (imageBounds.isEmpty()) return;
        float inset = Math.min(imageBounds.width(), imageBounds.height()) * 0.12f;
        if (squareLocked) {
            float side = Math.min(imageBounds.width(), imageBounds.height()) - inset * 2f;
            float cx = imageBounds.centerX();
            float cy = imageBounds.centerY();
            cropRect.set(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f);
        } else {
            cropRect.set(imageBounds.left + inset, imageBounds.top + inset,
                    imageBounds.right - inset, imageBounds.bottom - inset);
        }
        invalidate();
    }

    /** The current crop rectangle as fractions (0..1) of the displayed
     *  image, e.g. left=0 means the left edge of the photo itself. */
    public RectF getNormalizedCropRect() {
        float w = imageBounds.width();
        float h = imageBounds.height();
        return new RectF(
                (cropRect.left - imageBounds.left) / w,
                (cropRect.top - imageBounds.top) / h,
                (cropRect.right - imageBounds.left) / w,
                (cropRect.bottom - imageBounds.top) / h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageBounds.isEmpty()) return;

        // Dim everything outside the crop rectangle.
        scrimPath.reset();
        scrimPath.addRect(imageBounds.left, imageBounds.top, imageBounds.right, imageBounds.bottom, Path.Direction.CW);
        scrimPath.addRect(cropRect, Path.Direction.CCW);
        canvas.drawPath(scrimPath, scrimPaint);

        canvas.drawRect(cropRect, borderPaint);

        // Rule-of-thirds guide lines, purely visual.
        float thirdW = cropRect.width() / 3f;
        float thirdH = cropRect.height() / 3f;
        for (int i = 1; i <= 2; i++) {
            canvas.drawLine(cropRect.left + thirdW * i, cropRect.top, cropRect.left + thirdW * i, cropRect.bottom, gridPaint);
            canvas.drawLine(cropRect.left, cropRect.top + thirdH * i, cropRect.right, cropRect.top + thirdH * i, gridPaint);
        }

        drawCorner(canvas, cropRect.left, cropRect.top, 1, 1);
        drawCorner(canvas, cropRect.right, cropRect.top, -1, 1);
        drawCorner(canvas, cropRect.left, cropRect.bottom, 1, -1);
        drawCorner(canvas, cropRect.right, cropRect.bottom, -1, -1);
    }

    private void drawCorner(Canvas canvas, float x, float y, int dirX, int dirY) {
        canvas.drawLine(x, y, x + handleDrawLenPx * dirX, y, handlePaint);
        canvas.drawLine(x, y, x, y + handleDrawLenPx * dirY, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragMode = detectDragMode(x, y);
                lastTouchX = x;
                lastTouchY = y;
                return dragMode != DragMode.NONE;

            case MotionEvent.ACTION_MOVE:
                if (dragMode == DragMode.NONE) return false;
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                applyDrag(dx, dy);
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragMode = DragMode.NONE;
                return true;
        }
        return false;
    }

    private DragMode detectDragMode(float x, float y) {
        if (near(x, y, cropRect.left, cropRect.top)) return DragMode.TOP_LEFT;
        if (near(x, y, cropRect.right, cropRect.top)) return DragMode.TOP_RIGHT;
        if (near(x, y, cropRect.left, cropRect.bottom)) return DragMode.BOTTOM_LEFT;
        if (near(x, y, cropRect.right, cropRect.bottom)) return DragMode.BOTTOM_RIGHT;
        if (cropRect.contains(x, y)) return DragMode.MOVE;
        return DragMode.NONE;
    }

    private boolean near(float x, float y, float px, float py) {
        float ddx = x - px;
        float ddy = y - py;
        return (ddx * ddx + ddy * ddy) <= handleTouchRadiusPx * handleTouchRadiusPx;
    }

    private void applyDrag(float dx, float dy) {
        switch (dragMode) {
            case MOVE: {
                float clampedDx = clampMoveDelta(dx, cropRect.left, cropRect.right, imageBounds.left, imageBounds.right);
                float clampedDy = clampMoveDelta(dy, cropRect.top, cropRect.bottom, imageBounds.top, imageBounds.bottom);
                cropRect.offset(clampedDx, clampedDy);
                break;
            }
            case TOP_LEFT:
                if (squareLocked) {
                    float delta = (dx + dy) / 2f;
                    resizeCorner(delta, delta, true, true);
                } else {
                    resizeCorner(dx, dy, true, true);
                }
                break;
            case TOP_RIGHT:
                if (squareLocked) {
                    float delta = (dx - dy) / 2f;
                    resizeCorner(delta, -delta, false, true);
                } else {
                    resizeCorner(dx, dy, false, true);
                }
                break;
            case BOTTOM_LEFT:
                if (squareLocked) {
                    float delta = (dx - dy) / 2f;
                    resizeCorner(delta, -delta, true, false);
                } else {
                    resizeCorner(dx, dy, true, false);
                }
                break;
            case BOTTOM_RIGHT:
                if (squareLocked) {
                    float delta = (dx + dy) / 2f;
                    resizeCorner(delta, delta, false, false);
                } else {
                    resizeCorner(dx, dy, false, false);
                }
                break;
            default:
                break;
        }
    }

    private float clampMoveDelta(float delta, float rectMin, float rectMax, float boundMin, float boundMax) {
        if (rectMin + delta < boundMin) delta = boundMin - rectMin;
        if (rectMax + delta > boundMax) delta = boundMax - rectMax;
        return delta;
    }

    /** Moves whichever edges belong to the dragged corner (left and/or top
     *  and/or right and/or bottom), clamped so the rectangle can't shrink
     *  below the minimum size or escape the image bounds. */
    private void resizeCorner(float dx, float dy, boolean isLeftEdge, boolean isTopEdge) {
        float newLeft = cropRect.left;
        float newTop = cropRect.top;
        float newRight = cropRect.right;
        float newBottom = cropRect.bottom;

        if (isLeftEdge) {
            newLeft = clamp(cropRect.left + dx, imageBounds.left, cropRect.right - minSizePx);
        } else {
            newRight = clamp(cropRect.right + dx, cropRect.left + minSizePx, imageBounds.right);
        }
        if (isTopEdge) {
            newTop = clamp(cropRect.top + dy, imageBounds.top, cropRect.bottom - minSizePx);
        } else {
            newBottom = clamp(cropRect.bottom + dy, cropRect.top + minSizePx, imageBounds.bottom);
        }

        cropRect.set(newLeft, newTop, newRight, newBottom);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
