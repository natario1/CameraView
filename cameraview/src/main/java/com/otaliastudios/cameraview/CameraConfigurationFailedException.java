package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started setting change failed, but the camera should be still available.
 */
public class CameraConfigurationFailedException extends CameraException {

    CameraConfigurationFailedException(String message) {
        super(message);
    }

    CameraConfigurationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}