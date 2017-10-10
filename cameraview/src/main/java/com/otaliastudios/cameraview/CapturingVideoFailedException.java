package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started video capturing failed, but the camera should be still available.
 */
public class CapturingVideoFailedException extends CapturingFailedException {

    CapturingVideoFailedException(String message) {
        super(message);
    }

    CapturingVideoFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}