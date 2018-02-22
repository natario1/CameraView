package com.otaliastudios.cameraview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

class TapGestureLayout extends GestureLayout {


    private GestureDetector mDetector;
    private boolean mNotify;

    private FrameLayout mFocusMarkerContainer;
    private ImageView mFocusMarkerFill;

    public TapGestureLayout(Context context) {
        super(context);
    }


    @Override
    protected void onInitialize(Context context) {
        super.onInitialize(context);
        mPoints = new PointF[]{ new PointF(0, 0) };
        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mNotify = true;
                mType = Gesture.TAP;
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
                mType = Gesture.LONG_TAP;
            }
        });

        mDetector.setIsLongpressEnabled(true);


        // Views to draw the focus marker.
        LayoutInflater.from(getContext()).inflate(R.layout.cameraview_layout_focus_marker, this);
        mFocusMarkerContainer = findViewById(R.id.focusMarkerContainer);
        mFocusMarkerFill = findViewById(R.id.fill);
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

        // Let's see if we detect something.
        mDetector.onTouchEvent(event);

        // Keep notifying CameraView as long as the gesture goes.
        if (mNotify) {
            mPoints[0].x = event.getX();
            mPoints[0].y = event.getY();
            return true;
        }
        return false;
    }

    @Override
    public float scaleValue(float currValue, float minValue, float maxValue) {
        return 0;
    }

// Draw

    private final Runnable mFocusMarkerHideRunnable = new Runnable() {
        @Override
        public void run() {
            onFocusEnd(false);
        }
    };

    public void onFocusStart(PointF point) {
        removeCallbacks(mFocusMarkerHideRunnable);
        mFocusMarkerContainer.clearAnimation(); // animate().setListener(null).cancel();
        mFocusMarkerFill.clearAnimation(); // animate().setListener(null).cancel();

        float x = (int) (point.x - mFocusMarkerContainer.getWidth() / 2);
        float y = (int) (point.y - mFocusMarkerContainer.getWidth() / 2);
        mFocusMarkerContainer.setTranslationX(x);
        mFocusMarkerContainer.setTranslationY(y);

        mFocusMarkerContainer.setScaleX(1.36f);
        mFocusMarkerContainer.setScaleY(1.36f);
        mFocusMarkerContainer.setAlpha(1f);
        mFocusMarkerFill.setScaleX(0);
        mFocusMarkerFill.setScaleY(0);
        mFocusMarkerFill.setAlpha(1f);

        // Since onFocusEnd is not guaranteed to be called, we post a hide runnable just in case.
        animate(mFocusMarkerContainer, 1, 1, 300, 0, null);
        animate(mFocusMarkerFill, 1, 1, 300, 0, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                postDelayed(mFocusMarkerHideRunnable, 2000);
            }
        });
    }


    public void onFocusEnd(boolean success) {
        if (success) {
            animate(mFocusMarkerContainer, 1, 0, 500, 0, null);
            animate(mFocusMarkerFill, 1, 0, 500, 0, null);
        } else {
            animate(mFocusMarkerFill, 0, 0, 500, 0, null);
            animate(mFocusMarkerContainer, 1.36f, 1, 500, 0, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animate(mFocusMarkerContainer, 1.36f, 0, 200, 1000, null);
                }
            });
        }
    }


    private static void animate(View view, float scale, float alpha, long duration, long delay,
                                Animator.AnimatorListener listener) {
        view.animate().scaleX(scale).scaleY(scale)
                .alpha(alpha)
                .setDuration(duration)
                .setStartDelay(delay)
                .setListener(listener)
                .start();
    }
}
