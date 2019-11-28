package com.otaliastudios.cameraview.controls;


import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

/**
 * Format of the picture results for pictures that are taken with {@link CameraView#takePicture()}.
 * This does not apply to picture snapshots.
 *
 * @see CameraView#setPictureFormat(PictureFormat)
 */
public enum PictureFormat implements Control {

    /**
     * The picture result data will be a JPEG file.
     * This value is always supported.
     */
    JPEG(0),

    /**
     * The picture result data will be a DNG file.
     * This is only supported with the {@link Engine#CAMERA2} engine and only on
     * specific devices. Please check {@link CameraOptions#getSupportedPictureFormats()}.
     */
    DNG(1);

    static final PictureFormat DEFAULT = JPEG;

    private int value;

    PictureFormat(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @NonNull
    static PictureFormat fromValue(int value) {
        PictureFormat[] list = PictureFormat.values();
        for (PictureFormat action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return DEFAULT;
    }
}
