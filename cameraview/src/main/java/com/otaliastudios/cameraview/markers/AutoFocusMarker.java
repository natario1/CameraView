package com.otaliastudios.cameraview.markers;

import android.graphics.PointF;

import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;

/**
 * A marker for the autofocus operations. Receives callback when focus starts,
 * ends successfully or failed, and can be used to draw on screen.
 *
 * The point coordinates are meant with respect to {@link CameraView} width and height,
 * so a 0, 0 point means that focus is happening on the top-left visible corner.
 */
public interface AutoFocusMarker extends Marker {

    /**
     * Called when the autofocus process has started.
     *
     * @param trigger the autofocus trigger
     * @param point coordinates
     */
    void onAutoFocusStart(@NonNull AutoFocusTrigger trigger, @NonNull PointF point);


    /**
     * Called when the autofocus process has ended, and the camera converged
     * to a new focus or failed while trying to do so.
     *
     * @param trigger the autofocus trigger
     * @param successful whether the operation succeeded
     * @param point coordinates
     */
    void onAutoFocusEnd(@NonNull AutoFocusTrigger trigger,
                        boolean successful,
                        @NonNull PointF point);


}
