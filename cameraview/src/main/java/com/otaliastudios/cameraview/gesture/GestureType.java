package com.otaliastudios.cameraview.gesture;


/**
 * Gestures and gesture actions can both have a type. For a gesture to be able to be mapped to
 * a certain {@link GestureAction}, both of them might be of the same type.
 */
public enum GestureType {

    /**
     * Defines gestures or gesture actions that consist of a single operation.
     * Gesture example: a tap.
     * Gesture action example: taking a picture.
     */
    ONE_SHOT,

    /**
     * Defines gestures or gesture actions that consist of a continuous operation.
     * Gesture example: pinching.
     * Gesture action example: controlling zoom.
     */
    CONTINUOUS
}
