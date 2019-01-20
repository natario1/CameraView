package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OverlayLayout extends FrameLayout {

    private Surface outputSurface = null;

    public OverlayLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        if (outputSurface != null ) {
            // Requires a try/catch for .lockCanvas( null )
            try {
                final Canvas surfaceCanvas = outputSurface.lockCanvas(null);

                float xScale = surfaceCanvas.getWidth() / (float) canvas.getWidth();
                surfaceCanvas.scale(xScale, xScale);
                super.draw(surfaceCanvas);

                outputSurface.unlockCanvasAndPost(surfaceCanvas);
            } catch (Surface.OutOfResourcesException e) {
                e.printStackTrace();
            }
        }

        // draw the view as always, since we are already drawing on the preview this step can be
        // skipped
        //super.draw(canvas);
    }



    public void setOutputSurface(Surface outputSurface) {
        this.outputSurface = outputSurface;

        // force redrawing
        postInvalidate();
        postInvalidateRecursive(this);
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
