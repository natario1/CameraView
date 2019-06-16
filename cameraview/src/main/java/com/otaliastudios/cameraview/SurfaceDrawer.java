package com.otaliastudios.cameraview;

import android.graphics.Canvas;

interface SurfaceDrawer {
    void drawOnSurfaceForPictureSnapshot(Canvas surfaceCanvas);
    void drawOnSurfaceForVideoSnapshot(Canvas surfaceCanvas);
}
