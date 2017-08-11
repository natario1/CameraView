package com.otaliastudios.cameraview;


import android.hardware.Camera;

public class CameraConstants {

    public static final int PERMISSION_REQUEST_CODE = 16;

    public static final int SESSION_TYPE_PICTURE = 0;
    public static final int SESSION_TYPE_VIDEO = 1;

    public static final int VIDEO_QUALITY_480P = 0;
    public static final int VIDEO_QUALITY_720P = 1;
    public static final int VIDEO_QUALITY_1080P = 2;
    public static final int VIDEO_QUALITY_2160P = 3;
    public static final int VIDEO_QUALITY_HIGHEST = 4;
    public static final int VIDEO_QUALITY_LOWEST = 5;
    public static final int VIDEO_QUALITY_QVGA = 6;

    static class Defaults {

        static final int DEFAULT_VIDEO_QUALITY = VIDEO_QUALITY_480P;
        static final int DEFAULT_SESSION_TYPE = SESSION_TYPE_PICTURE;
        static final int DEFAULT_JPEG_QUALITY = 100;
        static final boolean DEFAULT_CROP_OUTPUT = false;


    }
}
