package com.otaliastudios.cameraview;


/**
 * Gestures actions are actions over camera controls that can be mapped to certain gestures over
 * the screen, using XML attributes or {@link CameraView#mapGesture(Gesture, GestureAction)}.
 *
 * Not every gesture can control a certain action. For example, pinch gestures can only control
 * continuous values, such as zoom or AE correction. Single point gestures, on the other hand,
 * can only control point actions such as focusing or capturing a picture.
 */
public enum GestureAction {

    /**
     * No action. This can be mapped to any gesture to disable it.
     */
    NONE(0),

    /**
     * Auto focus control, typically assigned to the tap gesture.
     * This action can be mapped to:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     */
    FOCUS(1),

    /**
     * Auto focus control, typically assigned to the tap gesture.
     * On top of {@link #FOCUS}, this will draw a default marker on screen.
     * This action can be mapped to:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     */
    FOCUS_WITH_MARKER(2),

    /**
     * When triggered, this action will fire a picture shoot.
     * This action can be mapped to:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     */
    CAPTURE(3),

    /**
     * Zoom control, typically assigned to the pinch gesture.
     * This action can be mapped to:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    ZOOM(4),

    /**
     * Exposure correction control.
     * This action can be mapped to:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    EXPOSURE_CORRECTION(5);


    final static GestureAction DEFAULT_PINCH = NONE;
    final static GestureAction DEFAULT_TAP = NONE;
    final static GestureAction DEFAULT_LONG_TAP = NONE;
    final static GestureAction DEFAULT_SCROLL_HORIZONTAL = NONE;
    final static GestureAction DEFAULT_SCROLL_VERTICAL = NONE;

    private int value;

    GestureAction(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    static GestureAction fromValue(int value) {
        GestureAction[] list = GestureAction.values();
        for (GestureAction action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
