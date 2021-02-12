package com.otaliastudios.cameraview.gesture;


import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;


/**
 * Gestures listen to finger gestures over the {@link CameraView} bounds and can be mapped
 * to one or more camera controls using XML attributes or {@link CameraView#mapGesture(Gesture,
 * GestureAction)}.
 *
 * Not every gesture can control a certain action. For example, pinch gestures can only control
 * continuous values, such as zoom or AE correction. Single point gestures, on the other hand,
 * can only control point actions such as focusing or capturing a picture.
 */
public enum Gesture {

    /**
     * Pinch gesture, typically assigned to the zoom control.
     * This gesture can be mapped to continuous actions:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#FILTER_CONTROL_1}
     * - {@link GestureAction#FILTER_CONTROL_2}
     * - {@link GestureAction#NONE}
     */
    PINCH(GestureType.CONTINUOUS),

    /**
     * Single tap gesture, typically assigned to the focus control.
     * This gesture can be mapped to one shot actions:
     *
     * - {@link GestureAction#AUTO_FOCUS}
     * - {@link GestureAction#TAKE_PICTURE}
     * - {@link GestureAction#NONE}
     */
    TAP(GestureType.ONE_SHOT),

    /**
     * Long tap gesture.
     * This gesture can be mapped to one shot actions:
     *
     * - {@link GestureAction#AUTO_FOCUS}
     * - {@link GestureAction#TAKE_PICTURE}
     * - {@link GestureAction#NONE}
     */
    LONG_TAP(GestureType.ONE_SHOT),

    /**
     * Horizontal scroll gesture.
     * This gesture can be mapped to continuous actions:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#FILTER_CONTROL_1}
     * - {@link GestureAction#FILTER_CONTROL_2}
     * - {@link GestureAction#NONE}
     */
    SCROLL_HORIZONTAL(GestureType.CONTINUOUS),

    /**
     * Vertical scroll gesture.
     * This gesture can be mapped to continuous actions:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#FILTER_CONTROL_1}
     * - {@link GestureAction#FILTER_CONTROL_2}
     * - {@link GestureAction#NONE}
     */
    SCROLL_VERTICAL(GestureType.CONTINUOUS);

    Gesture(@NonNull GestureType type) {
        this.type = type;
    }

    private GestureType type;

    /**
     * Whether this gesture can be assigned to the given {@link GestureAction}.
     * @param action the action to be checked
     * @return true if assignable
     */
    public boolean isAssignableTo(@NonNull GestureAction action) {
        return action == GestureAction.NONE || action.type() == type;
    }

}
