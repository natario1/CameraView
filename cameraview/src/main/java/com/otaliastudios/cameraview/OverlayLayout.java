package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class OverlayLayout extends FrameLayout {

    public OverlayLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void drawOverlay(Canvas canvas) {
        super.draw(canvas);
    }

}
