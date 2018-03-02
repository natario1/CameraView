package com.otaliastudios.cameraview;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Wraps the picture captured by {@link CameraView#takePicture()} or
 * {@link CameraView#takePictureSnapshot()}.
 */
public class PictureResult {

    boolean isSnapshot;
    Location location;
    int rotation;
    Size size;
    byte[] jpeg;

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
     * in the jpeg EXIF metadata.
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
     * Returns the raw jpeg, ready to be saved to file.
     *
     * @return the jpeg stream
     */
    @NonNull
    public byte[] getJpeg() {
        return jpeg;
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
    public void asBitmap(int maxWidth, int maxHeight, BitmapCallback callback) {
        CameraUtils.decodeBitmap(getJpeg(), maxWidth, maxHeight, callback);
    }
}
