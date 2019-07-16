package com.otaliastudios.cameraview.video.encoding;

import android.media.AudioFormat;

import androidx.annotation.NonNull;

/**
 * Audio configuration to be passed as input to the constructor
 * of an {@link AudioMediaEncoder}.
 */
@SuppressWarnings("WeakerAccess")
public class AudioConfig {

    // Configurable options
    public int bitRate; // ENCODED bit rate
    public int channels = 1;

    // Not configurable options (for now)
    final String mimeType = "audio/mp4a-latm";
    final int encoding = AudioFormat.ENCODING_PCM_16BIT; // Determines the sampleSizePerChannel
    // The 44.1KHz frequency is the only setting guaranteed to be available on all devices.
    final int samplingFrequency = 44100; // samples/sec
    final int sampleSizePerChannel = 2; // byte/sample/channel [16bit]
    final int byteRatePerChannel = samplingFrequency * sampleSizePerChannel; // byte/sec/channel
    final int frameSizePerChannel = 1024; // bytes/frame/channel [AAC constant]

    @NonNull
    AudioConfig copy() {
        AudioConfig config = new AudioConfig();
        config.bitRate = this.bitRate;
        config.channels = this.channels;
        return config;
    }

    int byteRate() { // RAW byte rate
        return byteRatePerChannel * channels; // byte/sec
    }

    @SuppressWarnings("unused")
    int bitRate() { // RAW bit rate
        return byteRate() * 8; // bit/sec
    }

    int frameSize() {
        // We call FRAME here the chunk of data that we want to read at each loop cycle
        return frameSizePerChannel * channels; // bytes/frame
    }

    int audioFormatChannels() {
        if (channels == 1) {
            return AudioFormat.CHANNEL_IN_MONO;
        } else if (channels == 2) {
            return AudioFormat.CHANNEL_IN_STEREO;
        }
        throw new RuntimeException("Invalid number of channels: " + channels);
    }
}
