package com.otaliastudios.cameraview;


/**
 * Holds an error with the camera configuration.
 */
public class CameraException extends RuntimeException {

    /**
     * Unknown error. No further info available.
     */
    public static final int REASON_UNKNOWN = 0;

    /**
     * We failed to connect to the camera service.
     * The camera might be in use by another app.
     */
    public static final int REASON_FAILED_TO_CONNECT = 1;

    /**
     * Failed to start the camera preview.
     * Again, the camera might be in use by another app.
     */
    public static final int REASON_FAILED_TO_START_PREVIEW = 2;

    /**
     * Camera was forced to disconnect.
     * In Camera1, this is thrown when android.hardware.Camera.CAMERA_ERROR_EVICTED
     * is caught.
     */
    public static final int REASON_DISCONNECTED = 3;

    private int reason = REASON_UNKNOWN;

    CameraException(Throwable cause) {
        super(cause);
    }

    CameraException(Throwable cause, int reason) {
        super(cause);
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }
}
