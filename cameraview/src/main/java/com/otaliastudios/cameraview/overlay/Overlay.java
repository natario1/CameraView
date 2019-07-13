package com.otaliastudios.cameraview.overlay;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

/**
 * Base interface for overlays.
 */
public interface Overlay {

    enum Target {
        PREVIEW, PICTURE_SNAPSHOT, VIDEO_SNAPSHOT
    }

    /**
     * Called for this overlay to draw itself on the specified target and canvas.
     * @param target target
     * @param canvas target canvas
     */
    void draw(@NonNull Target target, @NonNull Canvas canvas);
}
