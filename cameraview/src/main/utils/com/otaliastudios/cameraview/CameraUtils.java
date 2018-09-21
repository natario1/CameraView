package com.otaliastudios.cameraview;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Static utilities for dealing with camera I/O, orientations, etc.
 */
public class CameraUtils {


    /**
     * Determines whether the device has valid camera sensors, so the library
     * can be used.
     *
     * @param context a valid Context
     * @return whether device has cameras
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean hasCameras(Context context) {
        PackageManager manager = context.getPackageManager();
        // There's also FEATURE_CAMERA_EXTERNAL , should we support it?
        return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }


    /**
     * Determines whether the device has a valid camera sensor with the given
     * Facing value, so that a session can be started.
     *
     * @param context a valid context
     * @param facing either {@link Facing#BACK} or {@link Facing#FRONT}
     * @return true if such sensor exists
     */
    public static boolean hasCameraFacing(Context context, Facing facing) {
        int internal = new Mapper1().map(facing);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == internal) return true;
        }
        return false;
    }


    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     *
     * @param source a JPEG byte array
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public static void decodeBitmap(final byte[] source) {
        decodeBitmap(source, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     * This is executed in a background thread, and returns the result to the original thread.
     *
     * @param source a JPEG byte array
     * @param callback a callback to be notified
     */
    @SuppressWarnings("WeakerAccess")
    public static void decodeBitmap(final byte[] source, final BitmapCallback callback) {
        decodeBitmap(source, Integer.MAX_VALUE, Integer.MAX_VALUE, callback);
    }
    
    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     * This is executed in a background thread, and returns the result to the original thread.
     *
     * The image is also downscaled taking care of the maxWidth and maxHeight arguments.
     *
     * @param source a JPEG byte array
     * @param maxWidth the max allowed width
     * @param maxHeight the max allowed height
     * @param callback a callback to be notified
     */
    @SuppressWarnings("WeakerAccess")
    public static void decodeBitmap(final byte[] source, final int maxWidth, final int maxHeight, final BitmapCallback callback) {
        decodeBitmap(source, maxWidth, maxHeight, new BitmapFactory.Options(), callback);
    }

    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     * This is executed in a background thread, and returns the result to the original thread.
     *
     * The image is also downscaled taking care of the maxWidth and maxHeight arguments.
     *
     * @param source a JPEG byte array
     * @param maxWidth the max allowed width
     * @param maxHeight the max allowed height
     * @param options the options to be passed to decodeByteArray
     * @param callback a callback to be notified
     */
    @SuppressWarnings("WeakerAccess")
    public static void decodeBitmap(final byte[] source, final int maxWidth, final int maxHeight, @NonNull final BitmapFactory.Options options, final BitmapCallback callback) {
        decodeBitmap(source, maxWidth, maxHeight, options, -1, callback);
    }

    @SuppressWarnings("WeakerAccess")
    static void decodeBitmap(final byte[] source, final int maxWidth, final int maxHeight, @NonNull final BitmapFactory.Options options, final int rotation, final BitmapCallback callback) {
        final Handler ui = new Handler();
        WorkerHandler.run(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodeBitmap(source, maxWidth, maxHeight, options, rotation);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onBitmapReady(bitmap);
                    }
                });
            }
        });
    }

    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     *
     * The image is also downscaled taking care of the maxWidth and maxHeight arguments.
     *
     * @param source a JPEG byte array
     * @param maxWidth the max allowed width
     * @param maxHeight the max allowed height
     */
    static Bitmap decodeBitmap(byte[] source, int maxWidth, int maxHeight) {
        return decodeBitmap(source, maxWidth, maxHeight, new BitmapFactory.Options());
    }

    /**
     * Decodes an input byte array and outputs a Bitmap that is ready to be displayed.
     * The difference with {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}
     * is that this cares about orientation, reading it from the EXIF header.
     *
     * The image is also downscaled taking care of the maxWidth and maxHeight arguments.
     *
     * @param source a JPEG byte array
     * @param maxWidth the max allowed width
     * @param maxHeight the max allowed height
     * @param options the options to be passed to decodeByteArray
     */
    @SuppressWarnings({"SuspiciousNameCombination", "WeakerAccess"})
    @Nullable
    @WorkerThread
    public static Bitmap decodeBitmap(byte[] source, int maxWidth, int maxHeight, @NonNull BitmapFactory.Options options) {
        return decodeBitmap(source, maxWidth, maxHeight, options, -1);
    }

    // Null: got OOM
    // TODO ignores flipping. but it should be super rare.
    @Nullable
    static Bitmap decodeBitmap(byte[] source, int maxWidth, int maxHeight, @NonNull BitmapFactory.Options options, int rotation) {
        if (maxWidth <= 0) maxWidth = Integer.MAX_VALUE;
        if (maxHeight <= 0) maxHeight = Integer.MAX_VALUE;
        int orientation;
        boolean flip;
        if (rotation == -1) {
            InputStream stream = null;
            try {
                // http://sylvana.net/jpegcrop/exif_orientation.html
                stream = new ByteArrayInputStream(source);
                ExifInterface exif = new ExifInterface(stream);
                int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                orientation = readExifOrientation(exifOrientation);
                flip = exifOrientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
                        exifOrientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
                        exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                        exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE;

            } catch (IOException e) {
                e.printStackTrace();
                orientation = 0;
                flip = false;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignored) { }
                }
            }
        } else {
            orientation = rotation;
            flip = false;
        }

        Bitmap bitmap;
        try {
            if (maxWidth < Integer.MAX_VALUE || maxHeight < Integer.MAX_VALUE) {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(source, 0, source.length, options);

                int outHeight = options.outHeight;
                int outWidth = options.outWidth;
                if (orientation % 180 != 0) {
                    //noinspection SuspiciousNameCombination
                    outHeight = options.outWidth;
                    outWidth = options.outHeight;
                }

                options.inSampleSize = computeSampleSize(outWidth, outHeight, maxWidth, maxHeight);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeByteArray(source, 0, source.length, options);
            } else {
                bitmap = BitmapFactory.decodeByteArray(source, 0, source.length);
            }

            if (orientation != 0 || flip) {
                Matrix matrix = new Matrix();
                matrix.setRotate(orientation);
                Bitmap temp = bitmap;
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                temp.recycle();
            }
        } catch (OutOfMemoryError e) {
            bitmap = null;
        }
        return bitmap;
    }

    static int readExifOrientation(int exifOrientation) {
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


    private static int computeSampleSize(int width, int height, int maxWidth, int maxHeight) {
        // https://developer.android.com/topic/performance/graphics/load-bitmap.html
        int inSampleSize = 1;
        if (height > maxHeight || width > maxWidth) {
            while ((height / inSampleSize) >= maxHeight
                    || (width / inSampleSize) >= maxWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


}
