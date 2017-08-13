package com.otaliastudios.cameraview;


import java.util.Arrays;
import java.util.List;


/**
 * Gestures listen to finger gestures over the {@link CameraView} bounds and can be mapped
 * to one or more camera controls using XML attributes or {@link CameraView#mapGesture(Gesture, GestureAction)}.
 *
 * Not every gesture can control a certain action. For example, pinch gestures can only control
 * continuous values, such as zoom or AE correction. Single point gestures, on the other hand,
 * can only control point actions such as focusing or capturing a picture.
 */
public enum Gesture {

    /**
     * Pinch gesture, typically assigned to the zoom control.
     * This gesture can be mapped to:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#NONE}
     */
    PINCH(GestureAction.ZOOM, GestureAction.EXPOSURE_CORRECTION),

    /**
     * Single tap gesture, typically assigned to the focus control.
     * This gesture can be mapped to:
     *
     * - {@link GestureAction#FOCUS}
     * - {@link GestureAction#FOCUS_WITH_MARKER}
     * - {@link GestureAction#CAPTURE}
     * - {@link GestureAction#NONE}
     */
    TAP(GestureAction.FOCUS, GestureAction.FOCUS_WITH_MARKER, GestureAction.CAPTURE),
    // DOUBLE_TAP(GestureAction.FOCUS, GestureAction.FOCUS_WITH_MARKER, GestureAction.CAPTURE),

    /**
     * Long tap gesture.
     * This gesture can be mapped to:
     *
     * - {@link GestureAction#FOCUS}
     * - {@link GestureAction#FOCUS_WITH_MARKER}
     * - {@link GestureAction#CAPTURE}
     * - {@link GestureAction#NONE}
     */
    LONG_TAP(GestureAction.FOCUS, GestureAction.FOCUS_WITH_MARKER, GestureAction.CAPTURE),

    /**
     * Horizontal scroll gesture.
     * This gesture can be mapped to:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#NONE}
     */
    SCROLL_HORIZONTAL(GestureAction.ZOOM, GestureAction.EXPOSURE_CORRECTION),

    /**
     * Vertical scroll gesture.
     * This gesture can be mapped to:
     *
     * - {@link GestureAction#ZOOM}
     * - {@link GestureAction#EXPOSURE_CORRECTION}
     * - {@link GestureAction#NONE}
     */
    SCROLL_VERTICAL(GestureAction.ZOOM, GestureAction.EXPOSURE_CORRECTION);

    Gesture(GestureAction... controls) {
        mControls = Arrays.asList(controls);
    }

    private List<GestureAction> mControls;

    boolean isAssignableTo(GestureAction control) {
        return control == GestureAction.NONE || mControls.contains(control);
    }

}
