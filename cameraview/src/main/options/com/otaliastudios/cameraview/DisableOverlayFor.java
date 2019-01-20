package com.otaliastudios.cameraview;


import androidx.annotation.Nullable;

/**
 * DisableOverlayFor value allow the user to prevent the overlay from being recorded.
 *
 * @see CameraView#setDisableOverlayFor(DisableOverlayFor)
 */
public enum DisableOverlayFor implements Control {

    /**
     * Record the overlay both in picture and video snapshots.
     */
    NONE(0),

    /**
     * The picture snapshots will not contain the overlay.
     *
     * @see CameraOptions#getSupportedDisableOverlayFor()
     */
    PICTURE(1),


    /**
     * The picture snapshots will not contain the overlay.
     *
     * @see CameraOptions#getSupportedDisableOverlayFor()
     */
    VIDEO(2);

    static final DisableOverlayFor DEFAULT = NONE;

    private int value;

    DisableOverlayFor(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @Nullable
    static DisableOverlayFor fromValue(int value) {
        DisableOverlayFor[] list = DisableOverlayFor.values();
        for (DisableOverlayFor action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
