package com.otaliastudios.cameraview.video.encoding;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.view.Surface;

import com.otaliastudios.cameraview.CameraLogger;

import java.io.IOException;

/**
 * Base class for video encoding.
 *
 * This uses {@link MediaCodec#createInputSurface()} to create an input {@link Surface}
 * into which we can write and that MediaCodec itself can read.
 *
 * This makes everything easier with respect to the process explained in {@link MediaEncoder}
 * docs. We can skip the whole input part of acquiring an InputBuffer, filling it with data
 * and returning it to the encoder with {@link #encodeInputBuffer(InputBuffer)}.
 *
 * All of this is automatically done by MediaCodec as long as we keep writing data into the
 * given {@link Surface}. This class alone does not do this - subclasses are required to do so.
 *
 * @param <C> the config object.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
abstract class VideoMediaEncoder<C extends VideoMediaEncoder.Config> extends MediaEncoder {

    private static final String TAG = VideoMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    protected C mConfig;

    @SuppressWarnings("WeakerAccess")
    protected Surface mSurface;

    @SuppressWarnings("WeakerAccess")
    protected int mFrameNumber = -1;

    protected static class Config {
        public int width;
        public int height;
        public int bitRate;
        public int frameRate;
        public int rotation;
        public String mimeType;

        protected <C extends Config> void copy(@NonNull C output) {
            output.width = this.width;
            output.height = this.height;
            output.bitRate = this.bitRate;
            output.frameRate = this.frameRate;
            output.rotation = this.rotation;
            output.mimeType = this.mimeType;
        }
    }

    VideoMediaEncoder(@NonNull C config) {
        super("VideoEncoder");
        mConfig = config;
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
        mFrameNumber = 0;
    }

    @EncoderThread
    @Override
    void onStop() {
        LOG.i("onStop", "setting mFrameNumber to 1 and signaling the end of input stream.");
        mFrameNumber = -1;
        // Signals the end of input stream. This is a Video only API, as in the normal case,
        // we use input buffers to signal the end. In the video case, we don't have input buffers
        // because we use an input surface instead.
        mMediaCodec.signalEndOfInputStream();
        drainOutput(true);
    }

    @Override
    int getEncodedBitRate() {
        return mConfig.bitRate;
    }
}
