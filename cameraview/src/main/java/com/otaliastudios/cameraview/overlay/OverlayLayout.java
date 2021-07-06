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
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.R;


@SuppressLint("CustomViewStyleable")
public class OverlayLayout extends FrameLayout implements Overlay {

    private static final String TAG = OverlayLayout.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @VisibleForTesting Target currentTarget = Target.PREVIEW;

    private boolean mHardwareCanvasEnabled;

    /**
     * We set {@link #setWillNotDraw(boolean)} to false even if we don't draw anything.
     * This ensures that the View system will call {@link #draw(Canvas)} on us instead
     * of short-circuiting to {@link #dispatchDraw(Canvas)}.
     *
     * That would be a problem for us since we use {@link #draw(Canvas)} to understand if
     * we are currently drawing on the preview or not.
     *
     * @param context a context
     */
    public OverlayLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    /**
     * Returns true if this {@link AttributeSet} belongs to an overlay.
     * @param set an attribute set
     * @return true if overlay
     */
    public boolean isOverlay(@Nullable AttributeSet set) {
        if (set == null) return false;
        TypedArray a = getContext().obtainStyledAttributes(set, R.styleable.CameraView_Layout);
        boolean isOverlay =
                a.hasValue(R.styleable.CameraView_Layout_layout_drawOnPreview)
                || a.hasValue(R.styleable.CameraView_Layout_layout_drawOnPictureSnapshot)
                || a.hasValue(R.styleable.CameraView_Layout_layout_drawOnVideoSnapshot);
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
    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
        LOG.i("normal draw called.");
        if (drawsOn(Target.PREVIEW)) {
            drawOn(Target.PREVIEW, canvas);
        }
    }

    @Override
    public boolean drawsOn(@NonNull Target target) {
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            if (params.drawsOn(target)) return true;
        }
        return false;
    }

    @Override
    public void setHardwareCanvasEnabled(boolean on) {
        mHardwareCanvasEnabled = on;
    }

    @Override
    public boolean getHardwareCanvasEnabled() {
        return mHardwareCanvasEnabled;
    }

    /**
     * For {@link Target#PREVIEW}, this method is called by the View hierarchy. We will
     * just forward the call to super.
     *
     * For {@link Target#PICTURE_SNAPSHOT} and {@link Target#VIDEO_SNAPSHOT},
     * this method is called by the overlay drawer. We call {@link #dispatchDraw(Canvas)}
     * to draw our children only.
     *
     * @param target the draw target
     * @param canvas the canvas
     */
    @Override
    public void drawOn(@NonNull Target target, @NonNull Canvas canvas) {
        synchronized (this) {
            currentTarget = target;
            switch (target) {
                case PREVIEW:
                    super.draw(canvas);
                    break;
                case VIDEO_SNAPSHOT:
                case PICTURE_SNAPSHOT:
                    canvas.save();
                    // The input canvas size is that of the preview stream, cropped to match
                    // the view aspect ratio (this op is done by picture & video recorder).
                    // So the aspect ratio is guaranteed to be the same, but we might have
                    // to apply some scale (typically > 1).
                    float widthScale = canvas.getWidth() / (float) getWidth();
                    float heightScale = canvas.getHeight() / (float) getHeight();
                    LOG.v("draw",
                            "target:", target,
                            "canvas:", canvas.getWidth() + "x" + canvas.getHeight(),
                            "view:", getWidth() + "x" + getHeight(),
                            "widthScale:", widthScale,
                            "heightScale:", heightScale,
                            "hardwareCanvasMode:", mHardwareCanvasEnabled
                    );
                    canvas.scale(widthScale, heightScale);
                    dispatchDraw(canvas);
                    canvas.restore();
                    break;
            }
        }
    }

    /**
     * We end up here in all three cases, and should filter out
     * views that are not meant to be drawn on that specific surface.
     */
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        if (params.drawsOn(currentTarget)) {
            LOG.v("Performing drawing for view:", child.getClass().getSimpleName(),
                    "target:", currentTarget,
                    "params:", params);
            return doDrawChild(canvas, child, drawingTime);
        } else {
            LOG.v("Skipping drawing for view:", child.getClass().getSimpleName(),
                    "target:", currentTarget,
                    "params:", params);
            return false;
        }
    }

    @VisibleForTesting
    boolean doDrawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    @SuppressWarnings("WeakerAccess")
    public static class LayoutParams extends FrameLayout.LayoutParams {

        @SuppressWarnings("unused")
        public boolean drawOnPreview = false;
        public boolean drawOnPictureSnapshot = false;
        public boolean drawOnVideoSnapshot = false;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull Context context, @NonNull AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView_Layout);
            try {
                drawOnPreview = a.getBoolean(R.styleable.CameraView_Layout_layout_drawOnPreview,
                        false);
                drawOnPictureSnapshot = a.getBoolean(
                        R.styleable.CameraView_Layout_layout_drawOnPictureSnapshot, false);
                drawOnVideoSnapshot = a.getBoolean(
                        R.styleable.CameraView_Layout_layout_drawOnVideoSnapshot, false);
            } finally {
                a.recycle();
            }
        }

        @VisibleForTesting
        boolean drawsOn(@NonNull Target target) {
            return ((target == Target.PREVIEW && drawOnPreview)
                    || (target == Target.VIDEO_SNAPSHOT && drawOnVideoSnapshot)
                    || (target == Target.PICTURE_SNAPSHOT && drawOnPictureSnapshot));
        }

        @NonNull
        @Override
        public String toString() {
            return getClass().getName() + "["
                    + "drawOnPreview:" + drawOnPreview
                    + ",drawOnPictureSnapshot:" + drawOnPictureSnapshot
                    + ",drawOnVideoSnapshot:" + drawOnVideoSnapshot
                    + "]";
        }
    }
}
