package com.otaliastudios.cameraview.internal;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Checks the capabilities of device encoders and adjust parameters to ensure
 * that they'll be supported by the final encoder.
 */
public class DeviceEncoders {

    private final static String TAG = DeviceEncoders.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final boolean mEnabled;
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaCodecInfo mVideoEncoder;
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaCodecInfo mAudioEncoder;
    private final MediaCodecInfo.VideoCapabilities mVideoCapabilities;
    private final MediaCodecInfo.AudioCapabilities mAudioCapabilities;

    @SuppressLint("NewApi")
    public DeviceEncoders(@NonNull String videoType, @NonNull String audioType) {
        mEnabled = Build.VERSION.SDK_INT >= 21;
        // We could still get a list of MediaCodecInfo for API >= 16, but it seems that the APIs
        // for querying the availability of a specified MediaFormat were only added in 21 anyway.
        if (mEnabled) {
            List<MediaCodecInfo> encoders = getDeviceEncoders();
            mVideoEncoder = findDeviceEncoder(encoders, videoType);
            LOG.i("Enabled. Found video encoder:", mVideoEncoder.getName());
            mAudioEncoder = findDeviceEncoder(encoders, audioType);
            LOG.i("Enabled. Found audio encoder:", mAudioEncoder.getName());
            mVideoCapabilities = mVideoEncoder.getCapabilitiesForType(videoType).getVideoCapabilities();
            mAudioCapabilities = mAudioEncoder.getCapabilitiesForType(audioType).getAudioCapabilities();
        } else {
            mVideoEncoder = null;
            mAudioEncoder = null;
            mVideoCapabilities = null;
            mAudioCapabilities = null;
            LOG.i("Disabled.");
        }
    }

    /**
     * Collects all the device encoders, which means excluding decoders.
     * @return encoders
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    private List<MediaCodecInfo> getDeviceEncoders() {
        ArrayList<MediaCodecInfo> results = new ArrayList<>();
        MediaCodecInfo[] array = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
        for (MediaCodecInfo info : array) {
            if (info.isEncoder()) results.add(info);
        }
        return results;
    }

    /**
     * Whether an encoder is a hardware encoder or not. We don't have an API to check this,
     * but it seems that we can assume that all OMX.google encoders are software encoders.
     *
     * @param encoder encoder
     * @return true if hardware
     */
    @SuppressLint("NewApi")
    private boolean isHardwareEncoder(@NonNull MediaCodecInfo encoder) {
        return !encoder.getName().startsWith("OMX.google");
    }

    /**
     * Finds the encoder we'll be using.
     * The way we do this is to list all possible encoders and prefer the hardware encoders
     * over software encoders. Throws if we find no encoder for this type.
     *
     * @param mimeType mime type
     * @return encoder
     */
    @SuppressLint("NewApi")
    @NonNull
    private MediaCodecInfo findDeviceEncoder(@NonNull List<MediaCodecInfo> encoders, @NonNull String mimeType) {
        ArrayList<MediaCodecInfo> results = new ArrayList<>();
        for (MediaCodecInfo encoder : encoders) {
            String[] types = encoder.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    results.add(encoder);
                    break;
                }
            }
        }
        LOG.i("findDeviceEncoder -", "type:", mimeType, "encoders:", results.size());
        Collections.sort(results, new Comparator<MediaCodecInfo>() {
            @Override
            public int compare(MediaCodecInfo o1, MediaCodecInfo o2) {
                boolean hw1 = isHardwareEncoder(o1);
                boolean hw2 = isHardwareEncoder(o2);
                if (hw1 && hw2) return 0;
                if (hw1) return 1;
                if (hw2) return -1;
                return 0;
            }
        });
        if (results.isEmpty()) {
            throw new RuntimeException("No encoders for type:" + mimeType);
        }
        return results.get(0);
    }

    /**
     * Returns a video size supported by the device encoders.
     * Throws if input width or height are out of the supported boundaries.
     *
     * @param size input size
     * @return adjusted size
     */
    @SuppressLint("NewApi")
    @NonNull
    public Size getSupportedVideoSize(@NonNull Size size) {
        if (!mEnabled) return size;
        int width = size.getWidth();
        int height = size.getHeight();
        while (width % mVideoCapabilities.getWidthAlignment() != 0) width++;
        while (height % mVideoCapabilities.getHeightAlignment() != 0) height++;
        if (!mVideoCapabilities.getSupportedWidths().contains(width)) {
            throw new RuntimeException("Width not supported after adjustment." +
                    " Desired:" + width +
                    " Range:" + mVideoCapabilities.getSupportedWidths());
        }
        if (!mVideoCapabilities.getSupportedHeights().contains(height)) {
            throw new RuntimeException("Height not supported after adjustment." +
                    " Desired:" + height +
                    " Range:" + mVideoCapabilities.getSupportedHeights());
        }
        Size adjusted = new Size(width, height);
        LOG.i("getSupportedVideoSize -", "inputSize:", size, "adjustedSize:", adjusted);
        return adjusted;
    }

    /**
     * Returns a video bit rate supported by the device encoders.
     * This means adjusting the input bit rate if needed, to match encoder constraints.
     *
     * @param bitRate input rate
     * @return adjusted rate
     */
    @SuppressLint("NewApi")
    public int getSupportedVideoBitRate(int bitRate) {
        if (!mEnabled) return bitRate;
        int newBitRate = mVideoCapabilities.getBitrateRange().clamp(bitRate);
        LOG.i("getSupportedVideoBitRate -", "inputRate:", bitRate, "adjustedRate:", newBitRate);
        return newBitRate;
    }

    /**
     * Returns a video frame rate supported by the device encoders.
     * This means adjusting the input frame rate if needed, to match encoder constraints.
     *
     * @param frameRate input rate
     * @return adjusted rate
     */
    @SuppressLint("NewApi")
    public int getSupportedVideoFrameRate(@NonNull Size size, int frameRate) {
        if (!mEnabled) return frameRate;
        int newFrameRate = (int) (double) mVideoCapabilities
                .getSupportedFrameRatesFor(size.getWidth(), size.getHeight())
                .clamp((double) frameRate);
        LOG.i("getSupportedVideoFrameRate -", "inputRate:", frameRate, "adjustedRate:", newFrameRate);
        return newFrameRate;
    }

    /**
     * Returns an audio bit rate supported by the device encoders.
     * This means adjusting the input bit rate if needed, to match encoder constraints.
     *
     * @param bitRate input rate
     * @return adjusted rate
     */
    @SuppressLint("NewApi")
    public int getSupportedAudioBitRate(int bitRate) {
        if (!mEnabled) return bitRate;
        int newBitRate = mAudioCapabilities.getBitrateRange().clamp(bitRate);
        LOG.i("getSupportedAudioBitRate -", "inputRate:", bitRate, "adjustedRate:", newBitRate);
        return newBitRate;
    }


    // Won't do this for audio sample rate. As far as I remember, the value we're using,
    // 44.1kHz, is guaranteed to be available, and it's not configurable.

    /**
     * Returns the name of the video encoder if we were able to determine one.
     * @return encoder name
     */
    @SuppressLint("NewApi")
    @Nullable
    public String getVideoEncoder() {
        if (mVideoEncoder != null) {
            return mVideoEncoder.getName();
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the audio encoder if we were able to determine one.
     * @return encoder name
     */
    @SuppressLint("NewApi")
    @Nullable
    public String getAudioEncoder() {
        if (mAudioEncoder != null) {
            return mAudioEncoder.getName();
        } else {
            return null;
        }
    }

}
