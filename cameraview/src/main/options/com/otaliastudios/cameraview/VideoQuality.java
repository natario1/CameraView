package com.otaliastudios.cameraview;


/**
 * Constants for selecting the quality of video recordings.
 *
 * @see CameraView#setVideoQuality(VideoQuality)
 */
public enum VideoQuality {


    /**
     * Quality level corresponding to the lowest available resolution.
     */
    LOWEST(0),

    /**
     * Quality level corresponding to the highest available resolution.
     */
    HIGHEST(1),

    /**
     * Quality level corresponding to the QVGA (320x240) resolution.
     */
    MAX_QVGA(2),

    /**
     * Quality level corresponding to the 480p (720 x 480) resolution.
     * Note that the horizontal resolution for 480p can also be other
     * values, such as 640 or 704, instead of 720.
     */
    MAX_480P(3),

    /**
     * Quality level corresponding to the 720p (1280 x 720) resolution.
     */
    MAX_720P(4),

    /**
     * Quality level corresponding to the 1080p (1920 x 1080) resolution.
     * Note that the vertical resolution for 1080p can also be 1088,
     * instead of 1080 (used by some vendors to avoid cropping during
     * video playback).
     */
    MAX_1080P(5),

    /**
     * Quality level corresponding to the 2160p (3840x2160) resolution.
     */
    MAX_2160P(6);

    static final VideoQuality DEFAULT = MAX_480P;

    private int value;

    VideoQuality(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    static VideoQuality fromValue(int value) {
        VideoQuality[] list = VideoQuality.values();
        for (VideoQuality action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
