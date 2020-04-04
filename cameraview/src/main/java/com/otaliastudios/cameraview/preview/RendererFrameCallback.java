package com.otaliastudios.cameraview.preview;

import android.graphics.SurfaceTexture;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.Filter;

/**
 * Callback for renderer frames.
 */
public interface RendererFrameCallback {

    /**
     * Called on the renderer thread, hopefully only once, to notify that
     * the texture was created (or to inform a new callback of the old texture).
     *
     * @param textureId the GL texture linked to the image stream
     */
    @RendererThread
    void onRendererTextureCreated(int textureId);

    /**
     * Called on the renderer thread after each frame was drawn.
     * You are not supposed to hold for too long onto this thread, because
     * well, it is the rendering thread.
     * @param surfaceTexture the texture to get transformation
     * @param rotation the rotation (to reach REF_VIEW)
     * @param scaleX the scaleX (in REF_VIEW) value
     * @param scaleY the scaleY (in REF_VIEW) value
     */
    @RendererThread
    void onRendererFrame(@NonNull SurfaceTexture surfaceTexture, int rotation, float scaleX, float scaleY);

    /**
     * Called when the renderer filter changes. This is guaranteed to be called at least once
     * before the first {@link #onRendererFrame(SurfaceTexture, int, float, float)}.
     *
     * @param filter the new filter
     */
    @RendererThread
    void onRendererFilterChanged(@NonNull Filter filter);
}
