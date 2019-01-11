package com.otaliastudios.cameraview;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * Wraps the result of a video recording started by {@link CameraView#takeVideo(File)}.
 */
public class VideoResult {

    @SuppressWarnings({"WeakerAccess", "unused"})
    public static final int REASON_USER = 0;

    @SuppressWarnings("WeakerAccess")
    public static final int REASON_MAX_SIZE_REACHED = 1;

    @SuppressWarnings("WeakerAccess")
    public static final int REASON_MAX_DURATION_REACHED = 2;

    boolean isSnapshot;
    Location location;
    int rotation;
    Size size;
    File file;
    Facing facing;
    VideoCodec codec;
    Audio audio;
    long maxSize;
    int maxDuration;
    int endReason;
    int videoBitRate;
    int videoFrameRate;
    int audioBitRate;

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
     * Returns the facing value with which this video was recorded.
     *
     * @return the Facing of this video
     */
    @NonNull
    public Facing getFacing() {
        return facing;
    }

    /**
     * Returns the codec that was used to encode the video frames.
     *
     * @return the video codec
     */
    @NonNull
    public VideoCodec getVideoCodec() {
        return codec;
    }

    /**
     * Returns the max file size in bytes that was set before recording,
     * or 0 if no constraint was set.
     *
     * @return the max file size in bytes
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the max video duration in milliseconds that was set before recording,
     * or 0 if no constraint was set.
     *
     * @return the max duration in milliseconds
     */
    public int getMaxDuration() {
        return maxDuration;
    }

    /**
     * Returns the {@link Audio} setting for this video.
     *
     * @return the audio setting for this video
     */
    @NonNull
    public Audio getAudio() {
        return audio;
    }

    /**
     * Returns the reason why the recording was stopped.
     * @return one of {@link #REASON_USER}, {@link #REASON_MAX_DURATION_REACHED} or {@link #REASON_MAX_SIZE_REACHED}.
     */
    public int getTerminationReason() {
        return endReason;
    }

    /**
     * Returns the bit rate used for video encoding.
     *
     * @return the video bit rate
     */
    public int getVideoBitRate() {
        return videoBitRate;
    }

    /**
     * Returns the frame rate used for video encoding
     * in frames per second.
     *
     * @return the video frame rate
     */
    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    /**
     * Returns the bit rate used for audio encoding.
     *
     * @return the audio bit rate
     */
    public int getAudioBitRate() {
        return audioBitRate;
    }
}
