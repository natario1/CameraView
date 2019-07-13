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
     *
     * @param target target
     * @param canvas target canvas
     */
    void drawOn(@NonNull Target target, @NonNull Canvas canvas);

    /**
     * Called to understand if this overlay would like to draw onto the given
     * target or not. If true is returned, {@link #drawOn(Target, Canvas)} can be
     * called at a future time.
     *
     * @param target the target
     * @return true to draw on it
     */
    boolean drawsOn(@NonNull Target target);
}
