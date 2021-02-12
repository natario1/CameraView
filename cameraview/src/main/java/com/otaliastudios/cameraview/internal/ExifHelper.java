package com.otaliastudios.cameraview.internal;

import androidx.exifinterface.media.ExifInterface;

/**
 * Super basic exif utilities.
 */
public class ExifHelper {

    /**
     * Maps an {@link ExifInterface} orientation value
     * to the actual degrees.
     */
    public static int getOrientation(int exifOrientation) {
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

    /**
     * Maps a degree value to {@link ExifInterface} constant.
     */
    public static int getExifOrientation(int orientation) {
        switch ((orientation + 360) % 360) {
            case 0: return ExifInterface.ORIENTATION_NORMAL;
            case 90: return ExifInterface.ORIENTATION_ROTATE_90;
            case 180: return ExifInterface.ORIENTATION_ROTATE_180;
            case 270: return ExifInterface.ORIENTATION_ROTATE_270;
            default: throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }
}

