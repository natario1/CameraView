package com.otaliastudios.cameraview;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started setting change failed, but the camera should be still available.
 */
public class CameraConfigurationFailedException extends CameraException {

    // possible values for this.configuration
    public final static int CONFIGURATION_UNKNOWN = -1;
    public final static int CONFIGURATION_OTHER = 0;
    public final static int CONFIGURATION_FACING = 1;
    public final static int CONFIGURATION_FLASH = 2;
    public final static int CONFIGURATION_FOCUS = 3;
    public final static int CONFIGURATION_ZOOM = 4;
    public final static int CONFIGURATION_VIDEO_QUALITY = 5;
    public final static int CONFIGURATION_JPEG_QUALITY = 6;
    public final static int CONFIGURATION_HDR = 7;
    public final static int CONFIGURATION_LOCATION = 8;
    public final static int CONFIGURATION_EXPOSURE_CORRECTION = 9;
    public final static int CONFIGURATION_WHITE_BALANCE = 10;

    private int configuration;

    CameraConfigurationFailedException(String message, int configuration) {
        super(message);
        this.configuration = configuration;
    }

    CameraConfigurationFailedException(String message, int configuration, Throwable cause) {
        super(message, cause);
        this.configuration = configuration;
    }

    /**
     * Get the type of configuration that failed.
     * @return
     */
    public int getConfiguration() {
        return configuration;
    }
}