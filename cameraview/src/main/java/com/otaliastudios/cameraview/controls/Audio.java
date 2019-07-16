package com.otaliastudios.cameraview.controls;


import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Audio values indicate whether to record audio stream when record video.
 *
 * @see CameraView#setAudio(Audio)
 */
public enum Audio implements Control {

    /**
     * No audio.
     */
    OFF(0),

    /**
     * Audio on. The number of channels depends on the video configuration,
     * on the device capabilities and on the video type (e.g. we default to
     * mono for snapshots).
     */
    ON(1),

    /**
     * Force mono channel audio.
     */
    MONO(2),

    /**
     * Force stereo audio.
     */
    STEREO(3);

    final static Audio DEFAULT = ON;

    private int value;

    Audio(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @NonNull
    static Audio fromValue(int value) {
        Audio[] list = Audio.values();
        for (Audio action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return DEFAULT;
    }
}
