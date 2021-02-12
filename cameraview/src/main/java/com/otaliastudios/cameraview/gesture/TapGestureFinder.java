package com.otaliastudios.cameraview.gesture;

import androidx.annotation.NonNull;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * A {@link GestureFinder} that detects {@link Gesture#TAP}
 * and {@link Gesture#LONG_TAP} gestures.
 */
public class TapGestureFinder extends GestureFinder {

    private GestureDetector mDetector;
    private boolean mNotify;

    public TapGestureFinder(@NonNull Controller controller) {
        super(controller, 1);
        mDetector = new GestureDetector(controller.getContext(),
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mNotify = true;
                setGesture(Gesture.TAP);
                return true;
            }

            /*
            TODO should use onSingleTapConfirmed and enable this.
            public boolean onDoubleTap(MotionEvent e) {
                mNotify = true;
                mType = Gesture.DOUBLE_TAP;
                return true;
            } */

            @Override
            public void onLongPress(MotionEvent e) {
                mNotify = true;
                setGesture(Gesture.LONG_TAP);
            }
        });

        mDetector.setIsLongpressEnabled(true);
    }

    @Override
    protected boolean handleTouchEvent(@NonNull MotionEvent event) {
        // Reset the mNotify flag on a new gesture.
        // This is to ensure that the mNotify flag stays on until the
        // previous gesture ends.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mNotify = false;
        }

        // Let's see if we detect something.
        mDetector.onTouchEvent(event);

        // Keep notifying CameraView as long as the gesture goes.
        if (mNotify) {
            getPoint(0).x = event.getX();
            getPoint(0).y = event.getY();
            return true;
        }
        return false;
    }

    @Override
    public float getValue(float currValue, float minValue, float maxValue) {
        return 0;
    }

}
