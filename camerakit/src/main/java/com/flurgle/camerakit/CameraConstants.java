package com.flurgle.camerakit;

/**
 * Created by Mattia on 06/08/17.
 */
public class CameraConstants {

    public static final int PERMISSION_REQUEST_CODE = 16;

    public static final int FACING_BACK = 0;
    public static final int FACING_FRONT = 1;

    public static final int FLASH_OFF = 0;
    public static final int FLASH_ON = 1;
    public static final int FLASH_AUTO = 2;
    public static final int FLASH_TORCH = 3;

    public static final int FOCUS_FIXED = 0;
    public static final int FOCUS_CONTINUOUS = 1;
    public static final int FOCUS_TAP = 2;
    public static final int FOCUS_TAP_WITH_MARKER = 3;

    public static final int ZOOM_OFF = 0;
    public static final int ZOOM_PINCH = 1;

    public static final int GRID_OFF = 0;
    public static final int GRID_3X3 = 1;
    public static final int GRID_4X4 = 2;
    public static final int GRID_PHI = 3;

    public static final int SESSION_TYPE_PICTURE = 0;
    public static final int SESSION_TYPE_VIDEO = 1;

    @Deprecated
    public static final int CAPTURE_METHOD_STANDARD = 0;
    @Deprecated
    public static final int CAPTURE_METHOD_FRAME = 1;


    /**
     * This means the app will be requesting Camera and Audio permissions on Marshmallow+.
     * With this configuration, it is mandatory that the developer adds
     * <uses-permission android:name="android.permission.RECORD_AUDIO" />
     * to its manifest, or the app will crash.
     *
     * @deprecated this does nothing.
     */
    @Deprecated
    public static final int PERMISSIONS_VIDEO = 0;

    /**
     * This means the app will be requesting the Camera permissions on Marshmallow+.
     * On older APIs, this is not important, since the Camera permission is already
     * declared in the manifest by the library.
     *
     * @deprecated this does nothing.
     */
    @Deprecated
    public static final int PERMISSIONS_PICTURE = 1;

    public static final int VIDEO_QUALITY_480P = 0;
    public static final int VIDEO_QUALITY_720P = 1;
    public static final int VIDEO_QUALITY_1080P = 2;
    public static final int VIDEO_QUALITY_2160P = 3;
    public static final int VIDEO_QUALITY_HIGHEST = 4;
    public static final int VIDEO_QUALITY_LOWEST = 5;
    public static final int VIDEO_QUALITY_QVGA = 6;

    public static final int WHITE_BALANCE_AUTO = 0;
    public static final int WHITE_BALANCE_INCANDESCENT = 1;
    public static final int WHITE_BALANCE_FLUORESCENT = 2;
    public static final int WHITE_BALANCE_DAYLIGHT = 3;
    public static final int WHITE_BALANCE_CLOUDY = 4;


    static class Defaults {

        static final int DEFAULT_FACING = FACING_BACK;
        static final int DEFAULT_FLASH = FLASH_OFF;
        static final int DEFAULT_FOCUS = FOCUS_CONTINUOUS;
        static final int DEFAULT_ZOOM = ZOOM_OFF;
        @Deprecated static final int DEFAULT_METHOD = CAPTURE_METHOD_STANDARD;
        @Deprecated static final int DEFAULT_PERMISSIONS = PERMISSIONS_PICTURE;
        static final int DEFAULT_VIDEO_QUALITY = VIDEO_QUALITY_480P;
        static final int DEFAULT_WHITE_BALANCE = WHITE_BALANCE_AUTO;
        static final int DEFAULT_SESSION_TYPE = SESSION_TYPE_PICTURE;
        static final int DEFAULT_JPEG_QUALITY = 100;
        static final int DEFAULT_GRID = GRID_OFF;
        static final boolean DEFAULT_CROP_OUTPUT = false;

    }
}
