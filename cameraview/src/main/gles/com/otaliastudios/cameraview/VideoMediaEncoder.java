package com.otaliastudios.cameraview;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.view.Surface;

import java.io.IOException;

/**
 * This alone does nothing.
 * Subclasses must make sure they write each frame onto the given Surface {@link #mSurface}.
 *
 * @param <C> the config object.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
abstract class VideoMediaEncoder<C extends VideoMediaEncoder.Config> extends MediaEncoder {

    @SuppressWarnings("WeakerAccess")
    protected C mConfig;

    @SuppressWarnings("WeakerAccess")
    protected Surface mSurface;

    @SuppressWarnings("WeakerAccess")
    protected int mFrameNum = -1;

    static class Config {
        int width;
        int height;
        int bitRate;
        int frameRate;
        int rotation;
        String mimeType;

        Config(int width, int height, int bitRate, int frameRate, int rotation, String mimeType) {
            this.width = width;
            this.height = height;
            this.bitRate = bitRate;
            this.frameRate = frameRate;
            this.rotation = rotation;
            this.mimeType = mimeType;
        }
    }

    VideoMediaEncoder(@NonNull C config) {
        mConfig = config;
    }

    @NonNull
    @Override
    String getName() {
        return "VideoEncoder";
    }

    @EncoderThread
    @Override
    void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        MediaFormat format = MediaFormat.createVideoFormat(mConfig.mimeType, mConfig.width, mConfig.height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.frameRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 6); // TODO
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        format.setInteger("rotation-degrees", mConfig.rotation);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        try {
            mMediaCodec = MediaCodec.createEncoderByType(mConfig.mimeType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
    }

    @EncoderThread
    @Override
    void onStart() {
        // Nothing to do here. Waiting for the first frame.
        mFrameNum = 0;
    }

    @EncoderThread
    @Override
    void onStop() {
        mFrameNum = -1;
        signalEndOfInputStream();
        drainOutput(true);
    }

    @Override
    int getEncodedBitRate() {
        return mConfig.bitRate;
    }
}
