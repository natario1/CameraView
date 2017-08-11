package com.otaliastudios.cameraview;


/**
 * Facing values indicates which camera sensor should be used for the current session.
 *
 * @see CameraView#setFacing(int)
 */
public enum Facing {

    /**
     * Back-facing camera sensor.
     */
    BACK(0),

    /**
     * Front-facing camera sensor.
     */
    FRONT(1);

    private int value;

    Facing(int value) {
        this.value = value;
    }

    private int value() {
        return value;
    }

    static Facing fromValue(int value) {
        Facing[] list = Facing.values();
        for (Facing action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
