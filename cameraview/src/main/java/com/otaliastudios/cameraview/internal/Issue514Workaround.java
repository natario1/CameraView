package com.otaliastudios.cameraview.internal;

import android.graphics.SurfaceTexture;


/**
 * Fixes an issue for some devices with snapshot picture and video recording.
 * This is so dirty and totally unclear that I wanted to have a separate class.
 */
public class Issue514Workaround {

    private SurfaceTexture surfaceTexture = null;
    private final boolean hasOverlay;

    public Issue514Workaround(int cameraTextureId, boolean hasOverlay) {
        this.hasOverlay = hasOverlay;
        if (this.hasOverlay) {
            try {
                surfaceTexture = new SurfaceTexture(cameraTextureId);
            } catch (Exception ignore) { }
        }
    }

    public void onStart() {
        if (hasOverlay) {
            try {
                surfaceTexture.updateTexImage();
            } catch (Exception ignore) {}
        }
    }

    public void onEnd() {
        if (hasOverlay) {
            try {
                surfaceTexture.release();
            } catch (Exception ignore) {}
        }
    }
}
