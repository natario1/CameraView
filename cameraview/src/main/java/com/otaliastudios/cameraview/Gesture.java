package com.otaliastudios.cameraview;


import java.util.Arrays;
import java.util.List;

import static com.otaliastudios.cameraview.CameraConstants.*;

public enum Gesture {

    PINCH(GESTURE_ACTION_ZOOM, GESTURE_ACTION_AE_CORRECTION),
    TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE),
    // DOUBLE_TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE),
    LONG_TAP(GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER, GESTURE_ACTION_CAPTURE);

    Gesture(@GestureAction Integer... controls) {
        mControls = Arrays.asList(controls);
    }

    private List<Integer> mControls;

    boolean isAssignableTo(@GestureAction Integer control) {
        return control == GESTURE_ACTION_NONE || mControls.contains(control);
    }

}
