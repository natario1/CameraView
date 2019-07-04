package com.otaliastudios.cameraview.controls;


import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The engine to be used.
 *
 * @see CameraView#setEngine(Engine)
 */
public enum Engine implements Control {

    /**
     * Camera1 based engine.
     */
    CAMERA1(0),

    /**
     * Camera2 based engine. For API versions older than 21,
     * the system falls back to {@link #CAMERA1}.
     */
    CAMERA2(1);

    final static Engine DEFAULT = CAMERA1;

    private int value;

    Engine(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @NonNull
    static Engine fromValue(int value) {
        Engine[] list = Engine.values();
        for (Engine action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return DEFAULT;
    }
}
