package com.otaliastudios.cameraview;


import android.support.media.ExifInterface;

// TODO unused.
class ExifUtils {

    static int getOrientationTag(int rotation, boolean flip) {
        switch (rotation) {
            case 90:
                return flip ? ExifInterface.ORIENTATION_TRANSPOSE :
                        ExifInterface.ORIENTATION_ROTATE_90;

            case 180:
                return flip ? ExifInterface.ORIENTATION_FLIP_VERTICAL :
                        ExifInterface.ORIENTATION_ROTATE_180;

            case 270:
                return flip ? ExifInterface.ORIENTATION_TRANSVERSE :
                        ExifInterface.ORIENTATION_ROTATE_270;

            case 0:
            default:
                return flip ? ExifInterface.ORIENTATION_FLIP_HORIZONTAL :
                        ExifInterface.ORIENTATION_NORMAL;
        }
    }
}
