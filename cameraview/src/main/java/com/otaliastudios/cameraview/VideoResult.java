package com.otaliastudios.cameraview;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

/**
 * Wraps the result of a video recording started by {@link CameraView#takeVideo(File)}.
 */
public class VideoResult {

    boolean isSnapshot;
    Location location;
    int rotation;
    Size size;
    File file;
    VideoCodec codec;

    VideoResult() {}

    /**
     * Returns whether this result comes from a snapshot.
     *
     * @return whether this is a snapshot
     */
    public boolean isSnapshot() {
        return isSnapshot;
    }

    /**
     * Returns geographic information for this video, if any.
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
     * video frames before displaying. If it is non-zero, it is also present
     * in the video metadata, so most reader will take care of it.
     *
     * @return the clock-wise rotation
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Returns the size of the frames after the rotation is applied.
     *
     * @return the Size of this video
     */
    @NonNull
    public Size getSize() {
        return size;
    }

    /**
     * Returns the file where the video was saved.
     *
     * @return the File of this video
     */
    @NonNull
    public File getFile() {
        return file;
    }

    /**
     * Returns the codec that was used to encode the video frames.
     *
     * @return the video codec
     */
    @NonNull
    public VideoCodec getCodec() {
        return codec;
    }
}
