package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started capturing failed, but the camera should be still available.
 */
public abstract class CapturingFailedException extends CameraException {
    CapturingFailedException(String message) {
        super(message);
    }

    CapturingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}