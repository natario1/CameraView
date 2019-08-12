package com.otaliastudios.cameraview.internal;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the capabilities of device encoders.
 */
public class DeviceEncoders {

    private final List<MediaCodecInfo> mEncoders;

    public DeviceEncoders() {
        mEncoders = getDeviceEncoders();
    }

    @Nullable
    private List<MediaCodecInfo> getDeviceEncoders() {
        if (Build.VERSION.SDK_INT >= 21) {
            ArrayList<MediaCodecInfo> results = new ArrayList<>();
            MediaCodecInfo[] array = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
            for (MediaCodecInfo info : array) {
                if (info.isEncoder()) results.add(info);
            }
            return results;
        } else {
            // We could still get a list of MediaCodecInfo for API >= 16, but it seems that the APIs
            // for querying the availability of a specified MediaFormat were only added in 21 anyway.
            return null;
        }
    }

    @SuppressLint("NewApi")
    @Nullable
    private List<MediaCodecInfo.CodecCapabilities> getCapabilities(@NonNull String mimeType) {
        if (mEncoders == null) return null;
        ArrayList<MediaCodecInfo.CodecCapabilities> results = new ArrayList<>();
        for (MediaCodecInfo encoder : mEncoders) {
            String[] types = encoder.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    results.add(encoder.getCapabilitiesForType(type));
                    break;
                }
            }
        }
        return results;
    }

    @SuppressLint("NewApi")
    public boolean supportsVideo(@NonNull String mimeType, int width, int height, int bitRate) {
        List<MediaCodecInfo.CodecCapabilities> capabilities = getCapabilities(mimeType);
        if (capabilities == null) return true;
        if (capabilities.isEmpty()) return false;
        for (MediaCodecInfo.CodecCapabilities cap : capabilities) {
            MediaCodecInfo.VideoCapabilities videoCap = cap.getVideoCapabilities();
            if (videoCap.isSizeSupported(width, height)
                    && videoCap.getBitrateRange().contains(bitRate)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    public boolean supportsAudio(@NonNull String mimeType, int sampleRate, int bitRate) {
        List<MediaCodecInfo.CodecCapabilities> capabilities = getCapabilities(mimeType);
        if (capabilities == null) return true;
        if (capabilities.isEmpty()) return false;
        for (MediaCodecInfo.CodecCapabilities cap : capabilities) {
            MediaCodecInfo.AudioCapabilities audioCap = cap.getAudioCapabilities();
            if (audioCap.isSampleRateSupported(sampleRate)
                    && audioCap.getBitrateRange().contains(bitRate)) {
                return true;
            }
        }
        return false;
    }

}
