package com.otaliastudios.cameraview;

import android.view.Surface;

interface SurfaceDrawer {
    void drawOnSurfaceForPictureSnapshot(Surface surface);
    void drawOnSurfaceForVideoSnapshot(Surface surface);
}
