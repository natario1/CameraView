package com.otaliastudios.cameraview;


import android.hardware.Camera;

public class CameraConstants {

    public static final int PERMISSION_REQUEST_CODE = 16;

    public static final int SESSION_TYPE_PICTURE = 0;
    public static final int SESSION_TYPE_VIDEO = 1;


    static class Defaults {

        static final int DEFAULT_SESSION_TYPE = SESSION_TYPE_PICTURE;
        static final int DEFAULT_JPEG_QUALITY = 100;
        static final boolean DEFAULT_CROP_OUTPUT = false;


    }
}
