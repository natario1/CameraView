package com.otaliastudios.cameraview;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This alone does nothing.
 * Subclasses must make sure they write each frame onto the given Surface {@link #mSurface}.
 *
 * @param <C> the config object.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
abstract class VideoMediaEncoder<C extends VideoMediaEncoder.Config> extends MediaEncoder {

    protected C mConfig;
    protected Surface mSurface;
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

    @EncoderThread
    @Override
    void prepare(MediaEncoderEngine.Controller controller) {
        super.prepare(controller);
        MediaFormat format = MediaFormat.createVideoFormat(mConfig.mimeType, mConfig.width, mConfig.height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
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
    void start() {
        // Nothing to do here. Waiting for the first frame.
        mFrameNum = 0;
    }

    @EncoderThread
    @Override
    void stop() {
        mFrameNum = -1;
        drain(true);
    }

    @EncoderThread
    @Override
    void release() {
        super.release();
    }
}
