package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

class PinchGestureLayout extends GestureLayout {

    private final static float ADD_SENSITIVITY = 2f;

    ScaleGestureDetector mDetector;
    private boolean mNotify;
    /* tests */ float mFactor = 0;


    public PinchGestureLayout(Context context) {
        super(context);
    }


    @Override
    protected void onInitialize(Context context) {
        super.onInitialize(context);
        mPoints = new PointF[]{ new PointF(0, 0), new PointF(0, 0) };
        mDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mNotify = true;
                mFactor = ((detector.getScaleFactor() - 1) * ADD_SENSITIVITY);
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= 19) {
            mDetector.setQuickScaleEnabled(false);
        }

        // We listen only to the pinch type.
        mType = Gesture.PINCH;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnabled) return false;

        // Reset the mNotify flag on a new gesture.
        // This is to ensure that the mNotify flag stays on until the
        // previous gesture ends.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mNotify = false;
        }

        // Let's see if we detect something. This will call onScale().
        mDetector.onTouchEvent(event);

        // Keep notifying CameraView as long as the gesture goes.
        if (mNotify) {
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

    @Override
    public float scaleValue(float currValue, float minValue, float maxValue) {
        float add = mFactor;
        // ^ This works well if minValue = 0, maxValue = 1.
        // Account for the different range:
        add *= (maxValue - minValue);

        // ^ This works well if currValue = 0.
        // Account for a different starting point:
        /* if (add > 0) {
            add *= (maxValue - currValue);
        } else if (add < 0) {
            add *= (currValue - minValue);
        } Nope, I don't like this, it slows everything down. */
        return capValue(currValue, currValue + add, minValue, maxValue);
    }
}
