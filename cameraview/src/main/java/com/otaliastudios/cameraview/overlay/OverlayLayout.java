package com.otaliastudios.cameraview.overlay;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.R;


@SuppressLint("CustomViewStyleable")
public class OverlayLayout extends FrameLayout implements Overlay {

    private static final String TAG = OverlayLayout.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int DRAWING_PREVIEW = 0;
    private static final int DRAWING_PICTURE = 1;
    private static final int DRAWING_VIDEO = 2;

    private int target = DRAWING_PREVIEW;

    public OverlayLayout(@NonNull Context context) {
        super(context);
    }

    /**
     * Returns true if this {@link AttributeSet} belongs to an overlay.
     * @param set an attribute set
     * @return true if overlay
     */
    public boolean isOverlay(@Nullable AttributeSet set) {
        if (set == null) return false;
        TypedArray a = getContext().obtainStyledAttributes(set, R.styleable.CameraView_Layout);
        boolean isOverlay = a.getBoolean(R.styleable.CameraView_Layout_layout_isOverlay, false);
        a.recycle();
        return isOverlay;
    }

    /**
     * Returns true if this {@link ViewGroup.LayoutParams} belongs to an overlay.
     * @param params a layout params
     * @return true if overlay
     */
    public boolean isOverlay(@NonNull ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    /**
     * Generates our own overlay layout params.
     * @param attrs input attrs
     * @return our params
     */
    @Override
    public OverlayLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * This is called by the View hierarchy, so at this point we are
     * likely drawing on the preview.
     * @param canvas View canvas
     */
    @Override
    public void draw(Canvas canvas) {
        synchronized (this) {
            target = DRAWING_PREVIEW;
            super.draw(canvas);
        }
    }

    /**
     * This is called by the overlay drawer.
     * We call {@link #dispatchDraw(Canvas)} to draw our children.
     *
     * The input canvas has the Surface dimensions which means it is guaranteed
     * to have our own aspect ratio. But we might still have to apply some scale.
     *
     * @param canvas the overlay canvas
     */
    @Override
    public void drawOnPicture(@NonNull Canvas canvas) {
        canvas.save();
        float widthScale = canvas.getWidth() / (float) getWidth();
        float heightScale = canvas.getHeight() / (float) getHeight();
        LOG.i("drawOnPicture",
                "widthScale:", widthScale,
                "heightScale:", heightScale,
                "canvasWidth:", canvas.getWidth(),
                "canvasHeight:", canvas.getHeight());
        canvas.scale(widthScale, heightScale);
        synchronized (this) {
            target = DRAWING_PICTURE;
            dispatchDraw(canvas);
        }
        canvas.restore();
    }

    /**
     * This is called by the overlay drawer.
     * We call {@link #dispatchDraw(Canvas)} to draw our children.
     * @param canvas the overlay canvas
     */
    @Override
    public void drawOnVideo(@NonNull Canvas canvas) {
        canvas.save();
        float widthScale = canvas.getWidth() / (float) getWidth();
        float heightScale = canvas.getHeight() / (float) getHeight();
        LOG.i("drawOnVideo",
                "widthScale:", widthScale,
                "heightScale:", heightScale,
                "canvasWidth:", canvas.getWidth(),
                "canvasHeight:", canvas.getHeight());
        canvas.scale(widthScale, heightScale);
        synchronized (this) {
            target = DRAWING_VIDEO;
            dispatchDraw(canvas);
        }
        canvas.restore();
    }

    /**
     * We end up here in all three cases, and should filter out
     * views that are not meant to be drawn on that specific surface.
     */
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        boolean draw = ((target == DRAWING_PREVIEW && params.drawOnPreview)
                || (target == DRAWING_VIDEO && params.drawOnVideoSnapshot)
                || (target == DRAWING_PICTURE && params.drawOnPictureSnapshot)
        );
        if (draw) {
            return super.drawChild(canvas, child, drawingTime);
        } else {
            LOG.v("Skipping drawing for view:", child.getClass().getSimpleName(),
                    "target:", target,
                    "params:", params);
            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class LayoutParams extends FrameLayout.LayoutParams {

        @SuppressWarnings("unused")
        private boolean isOverlay;
        public boolean drawOnPreview;
        public boolean drawOnPictureSnapshot;
        public boolean drawOnVideoSnapshot;

        public LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView_Layout);
            try {
                this.isOverlay = a.getBoolean(R.styleable.CameraView_Layout_layout_isOverlay, false);
                this.drawOnPreview = a.getBoolean(R.styleable.CameraView_Layout_layout_drawOnPreview, false);
                this.drawOnPictureSnapshot = a.getBoolean(R.styleable.CameraView_Layout_layout_drawOnPictureSnapshot, false);
                this.drawOnVideoSnapshot = a.getBoolean(R.styleable.CameraView_Layout_layout_drawOnVideoSnapshot, false);
            } finally {
                a.recycle();
            }
        }

        @NonNull
        @Override
        public String toString() {
            return getClass().getName() + "[isOverlay:" + isOverlay
                    + ",drawOnPreview:" + drawOnPreview
                    + ",drawOnPictureSnapshot:" + drawOnPictureSnapshot
                    + ",drawOnVideoSnapshot:" + drawOnVideoSnapshot
                    + "]";
        }
    }
}
