package com.otaliastudios.cameraview;

import android.content.Context;
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

    public GestureLayout(Context context) {
        super(context);
        onInitialize(context);
    }

    protected void onInitialize(Context context) {
    }

    public void enable(boolean enable) {
        mEnabled = enable;
    }

    public abstract boolean onTouchEvent(MotionEvent event);
}
