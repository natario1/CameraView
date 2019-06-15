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

class OverlayLayout extends FrameLayout implements SurfaceDrawer {

    public OverlayLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    public void drawOnSurfaceForPictureSnapshot(Surface outputSurface) {
        try {
            final Canvas surfaceCanvas = outputSurface.lockCanvas(null);

            float xScale = surfaceCanvas.getWidth() / (float) getWidth();
            float yScale = surfaceCanvas.getHeight() / (float) getHeight();
            surfaceCanvas.scale(xScale, yScale);
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (((CameraView.OverlayLayoutParams) child.getLayoutParams()).isDrawInPictureSnapshot()) {
                    drawChild(surfaceCanvas, child, getDrawingTime());
                }
            }

            outputSurface.unlockCanvasAndPost(surfaceCanvas);
        } catch (Surface.OutOfResourcesException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawOnSurfaceForVideoSnapshot(Surface outputSurface) {
        try {
            final Canvas surfaceCanvas = outputSurface.lockCanvas(null);

            // scale factor between canvas width and this View's width
            float widthScale = surfaceCanvas.getWidth() / (float) getWidth();
            // scale factor between canvas height and this View's height
            float heightScale = surfaceCanvas.getHeight() / (float) getHeight();
            surfaceCanvas.scale(widthScale, heightScale);
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i); if (((CameraView.OverlayLayoutParams) child.getLayoutParams()).isDrawInVideoSnapshot()) {
                    drawChild(surfaceCanvas, child, getDrawingTime());
                }
            }

            outputSurface.unlockCanvasAndPost(surfaceCanvas);
        } catch (Surface.OutOfResourcesException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i); if (((CameraView.OverlayLayoutParams) child.getLayoutParams()).isDrawInPreview()) {
                drawChild(canvas, child, getDrawingTime());
            }
        }
    }

}
