package com.otaliastudios.cameraview.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.otaliastudios.cameraview.R;

/**
 * A {@link GestureLayout} that detects {@link Gesture#TAP}
 * and {@link Gesture#LONG_TAP} gestures.
 */
public class TapGestureLayout extends GestureLayout {

    private GestureDetector mDetector;
    private boolean mNotify;

    public TapGestureLayout(@NonNull Context context) {
        super(context, 1);
        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

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
