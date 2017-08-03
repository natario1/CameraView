package com.flurgle.camerakit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.support.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Static utilities for dealing with camera I/O, orientation, etc.
 */
public class CameraUtils {


    /**
     * Determines whether the device has valid camera sensors, so the library
     * can be used.
     *
     * @param context a valid Context
     * @return whether device has cameras
     */
    public static boolean hasCameras(Context context) {
        PackageManager manager = context.getPackageManager();
        // There's also FEATURE_CAMERA_EXTERNAL , should we support it?
        return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }


    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     * This is executed in a background thread, and returns the result to the original thread.
     *
     * This ignores flipping at the moment.
     * TODO care about flipping using Matrix.scale()
     *
     * @param source a JPEG byte array
     * @param callback a callback to be notified
     */
    public static void decodeBitmap(final byte[] source, final BitmapCallback callback) {
        final Handler ui = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {

                int orientation = 0;
                boolean flip = false;
                try {
                    // http://sylvana.net/jpegcrop/exif_orientation.html
                    ExifInterface exif = new ExifInterface(new ByteArrayInputStream(source));
                    Integer exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
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

                    flip = exifOrientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
                            exifOrientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
                            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE;

                } catch (IOException e) {
                    e.printStackTrace();
                    orientation = 0;
                    flip = false;
                }


                Bitmap bitmap = BitmapFactory.decodeByteArray(source, 0, source.length);
                if (orientation != 0 || flip) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(orientation);
                    // matrix.postScale(1, -1) Flip... needs testing.
                    Bitmap temp = bitmap;
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    temp.recycle();
                }
                final Bitmap result = bitmap;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onBitmapReady(result);
                    }
                });
            }
        }).start();
    }


    public interface BitmapCallback {
        void onBitmapReady(Bitmap bitmap);
    }
}
