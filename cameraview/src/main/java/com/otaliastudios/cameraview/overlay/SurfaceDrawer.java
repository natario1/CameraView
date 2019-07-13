package com.otaliastudios.cameraview.overlay;

import android.graphics.Canvas;

public interface SurfaceDrawer {
    void drawOnSurfaceForPictureSnapshot(Canvas surfaceCanvas);
    void drawOnSurfaceForVideoSnapshot(Canvas surfaceCanvas);
}
