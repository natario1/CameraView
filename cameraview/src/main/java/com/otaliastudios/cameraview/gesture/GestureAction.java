package com.otaliastudios.cameraview.gesture;


import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    NONE(0, GestureType.ONE_SHOT),

    /**
     * Touch metering control, typically assigned to the tap gesture.
     * This action can be mapped to one shot gestures:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     *
     * To control marker drawing, please see {@link CameraView#setAutoFocusMarker(AutoFocusMarker)}
     */
    AUTO_FOCUS(1, GestureType.ONE_SHOT),

    /**
     * When triggered, this action will fire a picture shoot.
     * This action can be mapped to one shot gestures:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     */
    TAKE_PICTURE(2, GestureType.ONE_SHOT),

    /**
     * When triggered, this action will fire a picture snapshot.
     * This action can be mapped to one shot gestures:
     *
     * - {@link Gesture#TAP}
     * - {@link Gesture#LONG_TAP}
     */
    TAKE_PICTURE_SNAPSHOT(3, GestureType.ONE_SHOT),

    /**
     * Zoom control, typically assigned to the pinch gesture.
     * This action can be mapped to continuous gestures:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    ZOOM(4, GestureType.CONTINUOUS),

    /**
     * Exposure correction control.
     * This action can be mapped to continuous gestures:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    EXPOSURE_CORRECTION(5, GestureType.CONTINUOUS),

    /**
     * Controls the first parameter of a real-time {@link Filter},
     * if it accepts one. This action can be mapped to continuous gestures:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    FILTER_CONTROL_1(6, GestureType.CONTINUOUS),

    /**
     * Controls the second parameter of a real-time {@link Filter},
     * if it accepts one. This action can be mapped to continuous gestures:
     *
     * - {@link Gesture#PINCH}
     * - {@link Gesture#SCROLL_HORIZONTAL}
     * - {@link Gesture#SCROLL_VERTICAL}
     */
    FILTER_CONTROL_2(7, GestureType.CONTINUOUS);

    final static GestureAction DEFAULT_PINCH = NONE;
    final static GestureAction DEFAULT_TAP = NONE;
    final static GestureAction DEFAULT_LONG_TAP = NONE;
    final static GestureAction DEFAULT_SCROLL_HORIZONTAL = NONE;
    final static GestureAction DEFAULT_SCROLL_VERTICAL = NONE;

    private int value;
    private GestureType type;

    GestureAction(int value, @NonNull GestureType type) {
        this.value = value;
        this.type = type;
    }

    int value() {
        return value;
    }

    @NonNull
    GestureType type() {
        return type;
    }

    @Nullable
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
