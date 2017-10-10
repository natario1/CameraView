package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * It prevents the camera from being used. The cause may be temporary or permanent. You should
 * restart the camera or deactivate any user interaction with the camera.
 */
public class CameraUnavailableException extends CameraException {

    CameraUnavailableException(String message) {
        super(message);
    }

    CameraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}