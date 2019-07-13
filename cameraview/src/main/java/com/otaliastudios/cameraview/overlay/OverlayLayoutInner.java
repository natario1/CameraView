package com.otaliastudios.cameraview.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OverlayLayoutInner extends FrameLayout {

    public OverlayLayoutInner(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public OverlayLayoutInner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void drawOverlay(Canvas canvas) {
        super.draw(canvas);
    }

}
