package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started snapshot capturing failed, but the camera should be still available.
 * This exception does not handle failed "real picture" capturing.
 */
public class CapturingSnapshotFailedException extends CapturingImageFailedException {

    CapturingSnapshotFailedException(String message) {
        super(message);
    }

    CapturingSnapshotFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}