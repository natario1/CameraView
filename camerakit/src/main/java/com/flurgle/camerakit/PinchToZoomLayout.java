package com.flurgle.camerakit;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

class PinchToZoomLayout extends View {


    private ScaleGestureDetector detector;
    private boolean notify;
    @ZoomMode private int mZoomMode;
    private float mZoom = 0;
    private PointF[] mPoints = new PointF[]{
            new PointF(0, 0),
            new PointF(0, 0)
    };

    public PinchToZoomLayout(@NonNull Context context) {
        this(context, null);
    }

    public PinchToZoomLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                notify = true;
                mZoom += ((detector.getScaleFactor() - 1) * 2);
                if (mZoom < 0) mZoom = 0;
                if (mZoom > 1) mZoom = 1;
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= 19) {
            detector.setQuickScaleEnabled(false);
        }
    }

    @ZoomMode
    public int getZoomMode() {
        return mZoomMode;
    }

    public void setZoomMode(@ZoomMode int zoomMode) {
        mZoomMode = zoomMode;
    }

    public float getZoom() {
        return mZoom;
    }

    public void onExternalZoom(float zoom) {
        mZoom = zoom;
    }

    public PointF[] getPoints() {
        return mPoints;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mZoomMode != CameraConstants.ZOOM_PINCH) return false;

        // Reset the notify flag on a new gesture.
        // This is to ensure that the notify flag stays on until the
        // previous gesture ends.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            notify = false;
        }

        // Let's see if we detect something. This will call onScale().
        detector.onTouchEvent(event);

        // Keep notifying CameraView as long as the gesture goes.
        if (notify) {
            mPoints[0].x = event.getX(0);
            mPoints[0].y = event.getY(0);
            if (event.getPointerCount() > 1) {
                mPoints[1].x = event.getX(1);
                mPoints[1].y = event.getY(1);
            }
            return true;
        }
        return false;
    }
}
