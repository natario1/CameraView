package com.otaliastudios.cameraview.gesture;

import androidx.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.otaliastudios.cameraview.CameraLogger;

/**
 * A {@link GestureFinder} that detects {@link Gesture#SCROLL_HORIZONTAL}
 * and {@link Gesture#SCROLL_VERTICAL} gestures.
 */
public class ScrollGestureFinder extends GestureFinder {

    private static final String TAG = ScrollGestureFinder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private GestureDetector mDetector;
    private boolean mNotify;
    private float mFactor;

    public ScrollGestureFinder(final @NonNull Controller controller) {
        super(controller, 2);
        mDetector = new GestureDetector(controller.getContext(),
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1,
                                    MotionEvent e2,
                                    float distanceX,
                                    float distanceY) {
                boolean horizontal;
                LOG.i("onScroll:", "distanceX="+distanceX, "distanceY="+distanceY);
                if (e1 == null || e2 == null) return false; // Got some crashes about this.
                if (e1.getX() != getPoint(0).x || e1.getY() != getPoint(0).y) {
                    // First step. We choose now if it's a vertical or horizontal scroll, and
                    // stick to it for the whole gesture.
                    horizontal = Math.abs(distanceX) >= Math.abs(distanceY);
                    setGesture(horizontal ? Gesture.SCROLL_HORIZONTAL : Gesture.SCROLL_VERTICAL);
                    getPoint(0).set(e1.getX(), e1.getY());
                } else {
                    // Not the first step. We already defined the type.
                    horizontal = getGesture() == Gesture.SCROLL_HORIZONTAL;
                }
                getPoint(1).set(e2.getX(), e2.getY());
                mFactor = horizontal ? (distanceX / controller.getWidth())
                        : (distanceY / controller.getHeight());
                mFactor = horizontal ? -mFactor : mFactor; // When vertical, up = positive
                mNotify = true;
                return true;
            }
        });

        mDetector.setIsLongpressEnabled(false); // Looks important.
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
        if (mNotify) LOG.i("Notifying a gesture of type", getGesture().name());
        return mNotify;
    }

    @Override
    public float getValue(float currValue, float minValue, float maxValue) {
        float delta = getFactor(); // -1 ... 1

        // ^ This works well if minValue = 0, maxValue = 1.
        // Account for the different range:
        delta *= (maxValue - minValue); // -(max-min) ... (max-min)
        delta *= 2; // Add some sensitivity.

        return currValue + delta;
    }

    /* for tests */ protected float getFactor() {
        return mFactor;
    }
}
