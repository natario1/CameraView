package com.otaliastudios.cameraview;


import com.otaliastudios.cameraview.controls.Facing;

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

    /**
     * Could not take a picture or a picture snapshot,
     * for some not specified reason.
     */
    public static final int REASON_PICTURE_FAILED = 4;

    /**
     * Could not take a video or a video snapshot,
     * for some not specified reason.
     */
    public static final int REASON_VIDEO_FAILED = 5;

    /**
     * Indicates that we could not find a camera for the current {@link Facing}
     * value.
     * This can be solved by changing the facing value and starting again.
     */
    public static final int REASON_NO_CAMERA = 6;

    private int reason = REASON_UNKNOWN;

    @SuppressWarnings("WeakerAccess")
    public CameraException(Throwable cause) {
        super(cause);
    }

    public CameraException(Throwable cause, int reason) {
        super(cause);
        this.reason = reason;
    }

    public CameraException(int reason) {
        super();
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }

    /**
     * Whether this error is unrecoverable. If this function returns true,
     * the Camera has been closed (or will be soon) and it is likely showing a black preview.
     * This is the right moment to show an error dialog to the user.
     *
     * @return true if this error is unrecoverable
     */
    @SuppressWarnings("unused")
    public boolean isUnrecoverable() {
        switch (getReason()) {
            case REASON_FAILED_TO_CONNECT: return true;
            case REASON_FAILED_TO_START_PREVIEW: return true;
            case REASON_DISCONNECTED: return true;
            default: return false;
        }
    }
}
