package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 */
public class CameraException extends RuntimeException {

    CameraException(String message, Throwable cause) {
        super(message, cause);
    }
}