package com.otaliastudios.cameraview;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * An object of this class describes an error that occurred during the normal runtime of the camera.
 * The previously started video capturing failed, but the camera should be still available.
 */
public class CapturingVideoFailedException extends CapturingFailedException {

    @NonNull
    private File video;

    /**
     *
     * @param message
     * @param video The video file that was meant to store the captured video.
     */
    CapturingVideoFailedException(String message, @NonNull File video) {
        super(message);
        this.video = video;
    }

    /**
     *
     * @param message
     * @param video The video file that was meant to store the captured video.
     * @param cause
     */
    CapturingVideoFailedException(String message, @NonNull File video, Throwable cause) {
        super(message, cause);
        this.video = video;
    }

    /**
     * Get the video file that was meant to store the captured video.
     * The physical file itself will usually not exist (anymore), but you can use the file object
     * information for further processing.
     * @return
     */
    @NonNull
    public File getVideo() {
        return video;
    }
}