package com.otaliastudios.cameraview.internal;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Checks the capabilities of device encoders and adjust parameters to ensure
 * that they'll be supported by the final encoder.
 *
 * Methods in this class might throw either a {@link VideoException} or a {@link AudioException}.
 * Throwing this exception means that the given parameters will not be supported by the encoder
 * for that type, and cannot be tweaked to be.
 *
 * When this happens, users should retry with a new {@link DeviceEncoders} instance, but with
 * the audio or video encoder offset incremented. This offset is the position in the encoder list
 * from which we'll choose the potential encoder.
 *
 * This class will inspect the encoders list in two ways, based on the mode flag:
 *
 * 1. {@link #MODE_RESPECT_ORDER}
 *
 * Chooses the encoder as the first one that matches the given mime type.
 * This is what {@link android.media.MediaCodec#createEncoderByType(String)} does,
 * and what {@link android.media.MediaRecorder} also does when recording.
 *
 * The list is ordered based on the encoder definitions in system/etc/media_codecs.xml,
 * as explained here: https://source.android.com/devices/media , for example.
 * So taking the first means respecting the vendor priorities and should generally be
 * a good idea.
 *
 * About {@link android.media.MediaRecorder}, we know it uses this option from here:
 * https://stackoverflow.com/q/57479564/4288782 where all links to source code are shown.
 * - StagefrightRecorder (https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/StagefrightRecorder.cpp#1782)
 * - MediaCodecSource (https://android.googlesource.com/platform/frameworks/av/+/master/media/libstagefright/MediaCodecSource.cpp#515)
 * - MediaCodecList (https://android.googlesource.com/platform/frameworks/av/+/master/media/libstagefright/MediaCodecList.cpp#322)
 *
 * To be fair, what {@link android.media.MediaRecorder} does is actually choose the first one
 * that configures itself without errors. We offer this option through
 * {@link #tryConfigureVideo(String, Size, int, int)} and
 * {@link #tryConfigureAudio(String, int, int, int)}.
 *
 * 2. {@link #MODE_PREFER_HARDWARE}
 *
 * This takes the list - as ordered by the vendor - and just sorts it so that hardware encoders
 * are preferred over software ones. It's questionable whether this is good or not. Some vendors
 * might forget to put hardware encoders first in the list, some others might put poor hardware
 * encoders on the bottom of the list on purpose.
 */
public class DeviceEncoders {

    private final static String TAG = DeviceEncoders.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @VisibleForTesting static boolean ENABLED = Build.VERSION.SDK_INT >= 21;

    public final static int MODE_RESPECT_ORDER = 0;
    public final static int MODE_PREFER_HARDWARE = 1;

    /**
     * Exception thrown when trying to find appropriate values
     * for a video encoder.
     */
    public class VideoException extends RuntimeException {
        private VideoException(@NonNull String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when trying to find appropriate values
     * for an audio encoder. Currently never thrown.
     */
    public class AudioException extends RuntimeException {
        private AudioException(@NonNull String message) {
            super(message);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final MediaCodecInfo mVideoEncoder;
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaCodecInfo mAudioEncoder;
    private final MediaCodecInfo.VideoCapabilities mVideoCapabilities;
    private final MediaCodecInfo.AudioCapabilities mAudioCapabilities;

    @SuppressLint("NewApi")
    public DeviceEncoders(int mode,
                          @NonNull String videoType,
                          @NonNull String audioType,
                          int videoOffset,
                          int audioOffset) {
        // We could still get a list of MediaCodecInfo for API >= 16, but it seems that the APIs
        // for querying the availability of a specified MediaFormat were only added in 21 anyway.
        if (ENABLED) {
            List<MediaCodecInfo> encoders = getDeviceEncoders();
            mVideoEncoder = findDeviceEncoder(encoders, videoType, mode, videoOffset);
            LOG.i("Enabled. Found video encoder:", mVideoEncoder.getName());
            mAudioEncoder = findDeviceEncoder(encoders, audioType, mode, audioOffset);
            LOG.i("Enabled. Found audio encoder:", mAudioEncoder.getName());
            mVideoCapabilities = mVideoEncoder.getCapabilitiesForType(videoType)
                    .getVideoCapabilities();
            mAudioCapabilities = mAudioEncoder.getCapabilitiesForType(audioType)
                    .getAudioCapabilities();
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
    @NonNull
    @SuppressLint("NewApi")
    @VisibleForTesting
    List<MediaCodecInfo> getDeviceEncoders() {
        ArrayList<MediaCodecInfo> results = new ArrayList<>();
        MediaCodecInfo[] array = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
        for (MediaCodecInfo info : array) {
            if (info.isEncoder()) results.add(info);
        }
        return results;
    }

    /**
     * Whether an encoder is a hardware encoder or not. We don't have an API to check this,
     * but we can follow what libstagefright does:
     * https://android.googlesource.com/platform/frameworks/av/+/master/media/libstagefright/MediaCodecList.cpp#293
     *
     * @param encoder encoder
     * @return true if hardware
     */
    @SuppressLint("NewApi")
    @VisibleForTesting
    boolean isHardwareEncoder(@NonNull String encoder) {
        encoder = encoder.toLowerCase();
        boolean isSoftwareEncoder = encoder.startsWith("omx.google.")
                || encoder.startsWith("c2.android.")
                || (!encoder.startsWith("omx.") && !encoder.startsWith("c2."));
        return !isSoftwareEncoder;
    }

    /**
     * Finds the encoder we'll be using, depending on the given mode flag:
     * - {@link #MODE_RESPECT_ORDER} will just take the first of the list
     * - {@link #MODE_PREFER_HARDWARE} will prefer hardware encoders
     * Throws if we find no encoder for this type.
     *
     * @param encoders encoders
     * @param mimeType mime type
     * @param mode mode
     * @return encoder
     */
    @SuppressLint("NewApi")
    @NonNull
    @VisibleForTesting
    MediaCodecInfo findDeviceEncoder(@NonNull List<MediaCodecInfo> encoders,
                                     @NonNull String mimeType,
                                     int mode,
                                     int offset) {
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
        if (mode == MODE_PREFER_HARDWARE) {
            Collections.sort(results, new Comparator<MediaCodecInfo>() {
                @Override
                public int compare(MediaCodecInfo o1, MediaCodecInfo o2) {
                    boolean hw1 = isHardwareEncoder(o1.getName());
                    boolean hw2 = isHardwareEncoder(o2.getName());
                    return Boolean.compare(hw2, hw1);
                }
            });
        }
        if (results.size() < offset + 1) {
            // This should not be a VideoException or AudioException - we want the process
            // to crash here.
            throw new RuntimeException("No encoders for type:" + mimeType);
        }
        return results.get(offset);
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
        if (!ENABLED) return size;
        int width = size.getWidth();
        int height = size.getHeight();
        double aspect = (double) width / height;
        LOG.i("getSupportedVideoSize - started. width:", width, "height:", height);

        // If width is too large, scale down, but keep aspect ratio.
        if (mVideoCapabilities.getSupportedWidths().getUpper() < width) {
            width = mVideoCapabilities.getSupportedWidths().getUpper();
            height = (int) Math.round(width / aspect);
            LOG.i("getSupportedVideoSize - exceeds maxWidth! width:", width,
                    "height:", height);
        }

        // If height is too large, scale down, but keep aspect ratio.
        if (mVideoCapabilities.getSupportedHeights().getUpper() < height) {
            height = mVideoCapabilities.getSupportedHeights().getUpper();
            width = (int) Math.round(aspect * height);
            LOG.i("getSupportedVideoSize - exceeds maxHeight! width:", width,
                    "height:", height);
        }

        // Adjust the alignment.
        while (width % mVideoCapabilities.getWidthAlignment() != 0) width--;
        while (height % mVideoCapabilities.getHeightAlignment() != 0) height--;
        LOG.i("getSupportedVideoSize - aligned. width:", width, "height:", height);

        // It's still possible that we're BELOW the lower.
        if (!mVideoCapabilities.getSupportedWidths().contains(width)) {
            throw new VideoException("Width not supported after adjustment." +
                    " Desired:" + width +
                    " Range:" + mVideoCapabilities.getSupportedWidths());
        }
        if (!mVideoCapabilities.getSupportedHeights().contains(height)) {
            throw new VideoException("Height not supported after adjustment." +
                    " Desired:" + height +
                    " Range:" + mVideoCapabilities.getSupportedHeights());
        }

        // We cannot change the aspect ratio, but the max block count might also be the
        // issue. Try to find a width that contains a height that would accept our AR.
        try {
            if (!mVideoCapabilities.getSupportedHeightsFor(width).contains(height)) {
                int candidateWidth = width;
                int minWidth = mVideoCapabilities.getSupportedWidths().getLower();
                int widthAlignment = mVideoCapabilities.getWidthAlignment();
                while (candidateWidth >= minWidth) {
                    // Reduce by 32 and realign just in case, then check if our AR is now
                    // supported. If it is, restart from scratch to go through the other checks.
                    candidateWidth -= 32;
                    while (candidateWidth % widthAlignment != 0) candidateWidth--;
                    int candidateHeight = (int) Math.round(candidateWidth / aspect);
                    if (mVideoCapabilities.getSupportedHeightsFor(candidateWidth)
                            .contains(candidateHeight)) {
                        LOG.w("getSupportedVideoSize - restarting with smaller size.");
                        return getSupportedVideoSize(new Size(candidateWidth, candidateHeight));
                    }
                }
            }
        } catch (IllegalArgumentException ignore) {}

        // It's still possible that we're unsupported for other reasons.
        if (!mVideoCapabilities.isSizeSupported(width, height)) {
            throw new VideoException("Size not supported for unknown reason." +
                    " Might be an aspect ratio issue." +
                    " Desired size:" + new Size(width, height));
        }
        return new Size(width, height);
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
        if (!ENABLED) return bitRate;
        int newBitRate = mVideoCapabilities.getBitrateRange().clamp(bitRate);
        LOG.i("getSupportedVideoBitRate -",
                "inputRate:", bitRate,
                "adjustedRate:", newBitRate);
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
        if (!ENABLED) return frameRate;
        int newFrameRate = (int) (double) mVideoCapabilities
                .getSupportedFrameRatesFor(size.getWidth(), size.getHeight())
                .clamp((double) frameRate);
        LOG.i("getSupportedVideoFrameRate -",
                "inputRate:", frameRate,
                "adjustedRate:", newFrameRate);
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
        if (!ENABLED) return bitRate;
        int newBitRate = mAudioCapabilities.getBitrateRange().clamp(bitRate);
        LOG.i("getSupportedAudioBitRate -",
                "inputRate:", bitRate,
                "adjustedRate:", newBitRate);
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

    @SuppressLint("NewApi")
    public void tryConfigureVideo(@NonNull String mimeType,
                                  @NonNull Size size,
                                  int frameRate,
                                  int bitRate) {
        if (mVideoEncoder != null) {
            MediaCodec codec = null;
            try {
                MediaFormat format = MediaFormat.createVideoFormat(mimeType, size.getWidth(),
                        size.getHeight());
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                codec = MediaCodec.createByCodecName(mVideoEncoder.getName());
                codec.configure(format, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception e) {
                throw new VideoException("Failed to configure video codec: " + e.getMessage());
            } finally {
                if (codec != null) {
                    try {
                        codec.release();
                    } catch (Exception ignore) {}
                }
            }
        }
    }

    @SuppressLint("NewApi")
    public void tryConfigureAudio(@NonNull String mimeType,
                                  int bitRate,
                                  int sampleRate,
                                  int channels) {
        if (mAudioEncoder != null) {
            MediaCodec codec = null;
            try {
                final MediaFormat format = MediaFormat.createAudioFormat(mimeType, sampleRate,
                        channels);
                int channelMask = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO
                        : AudioFormat.CHANNEL_IN_MONO;
                format.setInteger(MediaFormat.KEY_CHANNEL_MASK, channelMask);
                format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);

                codec = MediaCodec.createByCodecName(mAudioEncoder.getName());
                codec.configure(format, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception e) {
                throw new AudioException("Failed to configure video audio: " + e.getMessage());
            } finally {
                if (codec != null) {
                    try {
                        codec.release();
                    } catch (Exception ignore) {}
                }
            }
        }
    }

}
