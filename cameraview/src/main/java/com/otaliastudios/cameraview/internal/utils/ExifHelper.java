package com.otaliastudios.cameraview.internal.utils;

import androidx.exifinterface.media.ExifInterface;

/**
 * Super basic exif utilities.
 */
public class ExifHelper {

    /**
     * Maps an {@link ExifInterface} orientation value
     * to the actual degrees.
     */
    public static int readExifOrientation(int exifOrientation) {
        int orientation;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                orientation = 0; break;

            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                orientation = 180; break;

            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
                orientation = 90; break;

            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                orientation = 270; break;

            default: orientation = 0;
        }
        return orientation;
    }
}

