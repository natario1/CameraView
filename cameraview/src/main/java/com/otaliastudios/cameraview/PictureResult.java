package com.otaliastudios.cameraview;

import android.graphics.BitmapFactory;
import android.location.Location;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wraps the picture captured by {@link CameraView#takePicture()} or
 * {@link CameraView#takePictureSnapshot()}.
 */
public class PictureResult {

    public final static int FORMAT_JPEG = 0;
    // public final static int FORMAT_PNG = 1;

    boolean isSnapshot;
    Location location;
    int rotation;
    Size size;
    Facing facing;
    byte[] data;
    int format;

    PictureResult() {}

    /**
     * Returns whether this result comes from a snapshot.
     *
     * @return whether this is a snapshot
     */
    public boolean isSnapshot() {
        return isSnapshot;
    }

    /**
     * Returns geographic information for this picture, if any.
     * If it was set, it is also present in the file metadata.
     *
     * @return a nullable Location
     */
    @Nullable
    public Location getLocation() {
        return location;
    }

    /**
     * Returns the clock-wise rotation that should be applied to the
     * picture before displaying. If it is non-zero, it is also present
     * in the EXIF metadata.
     *
     * @return the clock-wise rotation
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Returns the size of the picture after the rotation is applied.
     *
     * @return the Size of this picture
     */
    @NonNull
    public Size getSize() {
        return size;
    }

    /**
     * Returns the facing value with which this video was recorded.
     *
     * @return the Facing of this video
     */
    @NonNull
    public Facing getFacing() {
        return facing;
    }

    /**
     * Returns the raw compressed, ready to be saved to file,
     * in the given format.
     *
     * @return the compressed data stream
     */
    @NonNull
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the image format. At the moment this will always be
     * {@link #FORMAT_JPEG}.
     *
     * @return the current format
     */
    public int getFormat() {
        return format;
    }

    /**
     * Shorthand for {@link CameraUtils#decodeBitmap(byte[], int, int, BitmapCallback)}.
     * Decodes this picture on a background thread and posts the result in the UI thread using
     * the given callback.
     *
     * @param maxWidth the max. width of final bitmap
     * @param maxHeight the max. height of final bitmap
     * @param callback a callback to be notified of image decoding
     */
    public void toBitmap(int maxWidth, int maxHeight, @NonNull BitmapCallback callback) {
        CameraUtils.decodeBitmap(getData(), maxWidth, maxHeight, new BitmapFactory.Options(), rotation, callback);
    }

    /**
     * Shorthand for {@link CameraUtils#decodeBitmap(byte[], BitmapCallback)}.
     * Decodes this picture on a background thread and posts the result in the UI thread using
     * the given callback.
     *
     * @param callback a callback to be notified of image decoding
     */
    public void toBitmap(@NonNull BitmapCallback callback) {
        toBitmap(-1, -1, callback);
    }

    /**
     * Shorthand for {@link CameraUtils#writeToFile(byte[], File, FileCallback)}.
     * This writes this picture to file on a background thread and posts the result in the UI
     * thread using the given callback.
     *
     * @param file the file to write into
     * @param callback a callback
     */
    public void toFile(@NonNull File file, @NonNull FileCallback callback) {
        CameraUtils.writeToFile(getData(), file, callback);
    }
}
