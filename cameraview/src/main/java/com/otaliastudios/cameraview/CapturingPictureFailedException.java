package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started picture capturing failed, but the camera should be still available.
 * This exception does not handle failed snapshots.
 */
public class CapturingPictureFailedException extends CapturingImageFailedException {

    CapturingPictureFailedException(String message) {
        super(message);
    }

    CapturingPictureFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}