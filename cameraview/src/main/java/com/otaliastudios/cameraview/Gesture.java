package com.otaliastudios.cameraview;


import java.util.Arrays;
import java.util.List;

import static com.otaliastudios.cameraview.CameraConstants.*;


/**
 * Gestures listen to finger gestures over the {@link CameraView} bounds and can be mapped
 * to one or more camera controls using XML attributes or {@link CameraView#mapGesture(Gesture, int)}.
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
     * - {@link CameraConstants#GESTURE_ACTION_ZOOM}
     * - {@link CameraConstants#GESTURE_ACTION_AE_CORRECTION}
     * - {@link CameraConstants#GESTURE_ACTION_NONE}
     */
    PINCH(GESTURE_ACTION_ZOOM, GESTURE_ACTION_AE_CORRECTION),

    /**
     * Single tap gesture, typically assigned to the focus control.
     * This gesture can be mapped to:
     *
     * - {@link CameraConstants#GESTURE_ACTION_FOCUS}
     * - {@link CameraConstants#GESTURE_ACTION_FOCUS_WITH_MARKER}
     * - {@link CameraConstants#GESTURE_ACTION_CAPTURE}
     * - {@link CameraConstants#GESTURE_ACTION_NONE}
     */
    TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE),
    // DOUBLE_TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE),

    /**
     * Long tap gesture.
     * This gesture can be mapped to:
     *
     * - {@link CameraConstants#GESTURE_ACTION_FOCUS}
     * - {@link CameraConstants#GESTURE_ACTION_FOCUS_WITH_MARKER}
     * - {@link CameraConstants#GESTURE_ACTION_CAPTURE}
     * - {@link CameraConstants#GESTURE_ACTION_NONE}
     */
    LONG_TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE);


    Gesture(@GestureAction Integer... controls) {
        mControls = Arrays.asList(controls);
    }

    private List<Integer> mControls;

    boolean isAssignableTo(@GestureAction Integer control) {
        return control == GESTURE_ACTION_NONE || mControls.contains(control);
    }

}
