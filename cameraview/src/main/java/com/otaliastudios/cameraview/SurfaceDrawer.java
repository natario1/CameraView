package com.otaliastudios.cameraview;

import android.graphics.Canvas;

public interface SurfaceDrawer {
    void drawOnSurfaceForPictureSnapshot(Canvas surfaceCanvas);
    void drawOnSurfaceForVideoSnapshot(Canvas surfaceCanvas);
}
