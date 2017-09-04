package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Using Views to address the need to draw stuff, but think about it.
 * Simple classes could be enough.
 */
abstract class GestureLayout extends FrameLayout {

    protected boolean mEnabled;
    protected Gesture mType;
    protected PointF[] mPoints;

    public GestureLayout(Context context) {
        super(context);
        onInitialize(context);
    }

    protected void onInitialize(Context context) {
    }

    public void enable(boolean enable) {
        mEnabled = enable;
    }

    public boolean enabled() {
        return mEnabled;
    }

    public abstract boolean onTouchEvent(MotionEvent event);

    public final Gesture getGestureType() {
        return mType;
    }

    // For tests.
    void setGestureType(Gesture type) {
        mType = type;
    }

    public final PointF[] getPoints() {
        return mPoints;
    }

    public abstract float scaleValue(float currValue, float minValue, float maxValue);
}
