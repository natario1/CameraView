package com.otaliastudios.cameraview.controls;


import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraView;

/**
 * Constants for selecting the encoder of audio recordings.
 * https://developer.android.com/guide/topics/media/media-formats.html#audio-formats
 *
 * @see CameraView#setAudioCodec(AudioCodec)
 */
public enum AudioCodec implements Control {

    /**
     * Let the device choose its codec.
     */
    DEVICE_DEFAULT(0),

    /**
     * The AAC codec.
     */
    AAC(1),

    /**
     * The HE_AAC codec.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    HE_AAC(2),

    /**
     * The AAC_ELD codec.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    AAC_ELD(3);

    static final AudioCodec DEFAULT = DEVICE_DEFAULT;

    private int value;

    AudioCodec(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    @NonNull
    static AudioCodec fromValue(int value) {
        AudioCodec[] list = AudioCodec.values();
        for (AudioCodec action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return DEFAULT;
    }
}
