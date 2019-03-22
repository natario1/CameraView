package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OverlayLayout extends FrameLayout implements SurfaceDrawer {

    public OverlayLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    public void drawOnSurface(Surface outputSurface) {
        try {
            final Canvas surfaceCanvas = outputSurface.lockCanvas(null);

            float xScale = surfaceCanvas.getWidth() / (float) getWidth();
            float yScale = surfaceCanvas.getHeight() / (float) getHeight();
            surfaceCanvas.scale(xScale, yScale);
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            super.draw(surfaceCanvas);

            outputSurface.unlockCanvasAndPost(surfaceCanvas);
        } catch (Surface.OutOfResourcesException e) {
            e.printStackTrace();
        }
    }

    // invalidates children (and nested children) recursively
    private void postInvalidateRecursive(ViewGroup layout) {
        int count = layout.getChildCount();
        View child;
        for (int i = 0; i < count; i++) {
            child = layout.getChildAt(i);
            if(child instanceof ViewGroup)
                postInvalidateRecursive((ViewGroup) child);
            else
                child.postInvalidate();
        }
    }
}
