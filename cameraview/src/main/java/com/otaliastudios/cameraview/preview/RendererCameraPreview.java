package com.otaliastudios.cameraview.preview;

import androidx.annotation.NonNull;

/**
 * Base interface for previews that support renderer frame callbacks,
 * see {@link RendererFrameCallback}.
 */
public interface RendererCameraPreview {

    /**
     * Adds a {@link RendererFrameCallback} to receive renderer frame events.
     * @param callback a callback
     */
    void addRendererFrameCallback(@NonNull final RendererFrameCallback callback);

    /**
     * Removes a {@link RendererFrameCallback} that was previously added to receive renderer
     * frame events.
     * @param callback a callback
     */
    void removeRendererFrameCallback(@NonNull final RendererFrameCallback callback);
}
