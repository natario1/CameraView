package com.otaliastudios.cameraview.gesture;

import android.os.Build;
import androidx.annotation.NonNull;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * A {@link GestureFinder} that detects {@link Gesture#PINCH} gestures.
 */
public class PinchGestureFinder extends GestureFinder {

    private final static float ADD_SENSITIVITY = 2f;

    private ScaleGestureDetector mDetector;
    private boolean mNotify;
    private float mFactor = 0;

    public PinchGestureFinder(@NonNull Controller controller) {
        super(controller, 2);
        setGesture(Gesture.PINCH);
        mDetector = new ScaleGestureDetector(controller.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
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
    }

    @Override
    protected boolean handleTouchEvent(@NonNull MotionEvent event) {
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
            getPoint(0).x = event.getX(0);
            getPoint(0).y = event.getY(0);
            if (event.getPointerCount() > 1) {
                getPoint(1).x = event.getX(1);
                getPoint(1).y = event.getY(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public float getValue(float currValue, float minValue, float maxValue) {
        float add = getFactor();
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
        return currValue + add;
    }


    /* for tests */ protected float getFactor() {
        return mFactor;
    }
}
