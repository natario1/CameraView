package com.otaliastudios.cameraview.internal;

import android.graphics.SurfaceTexture;


/**
 * Fixes an issue for some devices with snapshot picture and video recording.
 * This is so dirty and totally unclear that I wanted to have a separate class.
 */
public class Issue514Workaround {

    private final SurfaceTexture surfaceTexture;

    public Issue514Workaround(int cameraTextureId) {
        surfaceTexture = new SurfaceTexture(cameraTextureId);
    }

    public void onStart() {
        surfaceTexture.updateTexImage();
    }

    public void onEnd() {
        surfaceTexture.release();
    }
}
