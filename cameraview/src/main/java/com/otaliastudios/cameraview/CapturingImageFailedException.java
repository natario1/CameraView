package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started image capturing failed (snapshot or "real picture"), but the camera should
 * be still available.
 */
public abstract class CapturingImageFailedException extends CapturingFailedException {

    CapturingImageFailedException(String message) {
        super(message);
    }

    CapturingImageFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}