package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

abstract class GestureLayout extends FrameLayout {

    // The number of possible values between minValue and maxValue, for the scaleValue method.
    // We could make this non-static (e.g. larger granularity for exposure correction).
    private final static int GRANULARITY = 50;

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

    // Implementors should call capValue at the end.
    public abstract float scaleValue(float currValue, float minValue, float maxValue);

    // Checks for newValue to be between minValue and maxValue,
    // and checks that it is 'far enough' from the oldValue, in order
    // to reduce useless updates.
    protected static float capValue(float oldValue, float newValue, float minValue, float maxValue) {
        if (newValue < minValue) newValue = minValue;
        if (newValue > maxValue) newValue = maxValue;

        float distance = (maxValue - minValue) / (float) GRANULARITY;
        float half = distance / 2;
        if (newValue >= oldValue - half && newValue <= oldValue + half) {
            // Too close! Return the oldValue.
            return oldValue;
        }
        return newValue;
    }
}
