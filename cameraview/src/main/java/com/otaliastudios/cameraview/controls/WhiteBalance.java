package com.otaliastudios.cameraview.controls;


import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * White balance values control the white balance settings.
 *
 * @see CameraView#setWhiteBalance(WhiteBalance)
 */
public enum WhiteBalance implements Control {

    /**
     * Automatic white balance selection (AWB).
     * This is not guaranteed to be supported.
     *
     * @see CameraOptions#getSupportedWhiteBalance()
     */
    AUTO(0),

    /**
     * White balance appropriate for incandescent light.
     * This is not guaranteed to be supported.
     *
     * @see CameraOptions#getSupportedWhiteBalance()
     */
    INCANDESCENT(1),

    /**
     * White balance appropriate for fluorescent light.
     * This is not guaranteed to be supported.
     *
     * @see CameraOptions#getSupportedWhiteBalance()
     */
    FLUORESCENT(2),

    /**
     * White balance appropriate for daylight captures.
     * This is not guaranteed to be supported.
     *
     * @see CameraOptions#getSupportedWhiteBalance()
     */
    DAYLIGHT(3),

    /**
     * White balance appropriate for pictures in cloudy conditions.
     * This is not guaranteed to be supported.
     *
     * @see CameraOptions#getSupportedWhiteBalance()
     */
    CLOUDY(4);

    static final WhiteBalance DEFAULT = AUTO;

    private int value;

    WhiteBalance(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @NonNull
    static WhiteBalance fromValue(int value) {
        WhiteBalance[] list = WhiteBalance.values();
        for (WhiteBalance action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return DEFAULT;
    }
}