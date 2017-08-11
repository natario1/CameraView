package com.otaliastudios.cameraview;


public class CameraConstants {

    public static final int PERMISSION_REQUEST_CODE = 16;

    public static final int GRID_OFF = 0;
    public static final int GRID_3X3 = 1;
    public static final int GRID_4X4 = 2;
    public static final int GRID_PHI = 3;

    public static final int SESSION_TYPE_PICTURE = 0;
    public static final int SESSION_TYPE_VIDEO = 1;

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

        // TODO: Move to enum classes

        static final int DEFAULT_VIDEO_QUALITY = VIDEO_QUALITY_480P;
        static final int DEFAULT_WHITE_BALANCE = WHITE_BALANCE_AUTO;
        static final int DEFAULT_SESSION_TYPE = SESSION_TYPE_PICTURE;
        static final int DEFAULT_JPEG_QUALITY = 100;
        static final int DEFAULT_GRID = GRID_OFF;
        static final boolean DEFAULT_CROP_OUTPUT = false;


    }
}
