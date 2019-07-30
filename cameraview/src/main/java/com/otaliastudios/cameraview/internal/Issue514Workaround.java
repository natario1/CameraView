package com.otaliastudios.cameraview.internal;

import android.graphics.SurfaceTexture;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.size.Size;

/**
 * Fixes an issue for some devices with snapshot picture and video recording.
 * This is so dirty and totally unclear that I wanted to have a separate class.
 */
public class Issue514Workaround {

    private final SurfaceTexture surfaceTexture;

    public Issue514Workaround(int cameraTextureId, @NonNull Size size) {
        surfaceTexture = new SurfaceTexture(cameraTextureId);
        surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
    }

    public void onStart() {
        surfaceTexture.updateTexImage();
    }

    public void onEnd() {
        surfaceTexture.release();
    }
}
