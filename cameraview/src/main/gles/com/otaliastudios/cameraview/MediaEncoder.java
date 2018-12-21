package com.otaliastudios.cameraview;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;

import java.nio.ByteBuffer;

// https://github.com/saki4510t/AudioVideoRecordingSample/blob/master/app/src/main/java/com/serenegiant/encoder/MediaEncoder.java
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
abstract class MediaEncoder {

    private final static int TIMEOUT_USEC = 10000; // 10 msec

    @SuppressWarnings("WeakerAccess")
    protected MediaCodec mMediaCodec;

    private MediaCodec.BufferInfo mBufferInfo;
    private MediaEncoderEngine.Controller mController;
    private int mTrackIndex;
    private long mMaxLengthMillis;
    private boolean mMaxLengthReached;

    /**
     * Called to prepare this encoder before starting.
     * Any initialization should be done here as it does not interfere with the original
     * thread (that, generally, is the rendering thread).
     *
     * At this point subclasses MUST create the {@link #mMediaCodec} object.
     *
     * @param controller the muxer controller
     */
    @EncoderThread
    void prepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        mController = controller;
        mBufferInfo = new MediaCodec.BufferInfo();
        mMaxLengthMillis = maxLengthMillis;
    }

    /**
     * Start recording. This might be a lightweight operation
     * in case the encoder needs to wait for a certain event
     * like a "frame available".
     */
    @EncoderThread
    abstract void start();

    /**
     * The caller notifying of a certain event occurring.
     * Should analyze the string and see if the event is important.
     * @param event what happened
     * @param data object
     */
    @EncoderThread
    abstract void notify(@NonNull String event, @Nullable Object data);

    /**
     * Stop recording.
     * This MUST happen SYNCHRONOUSLY!
     */
    @EncoderThread
    abstract void stop();

    /**
     * Release resources here.
     */
    @EncoderThread
    void release() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    /**
     * Encode data into the {@link #mMediaCodec}.
     */
    @SuppressWarnings("WeakerAccess")
    protected void encode(@Nullable final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (true) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (length <= 0) { // send EOS
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * Extracts all pending data that was written and encoded into {@link #mMediaCodec},
     * and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    @SuppressWarnings("WeakerAccess")
    protected void drain(boolean endOfStream) {
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) break; // out of while

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mController.isStarted()) throw new RuntimeException("format changed twice");
                MediaFormat newFormat = mMediaCodec.getOutputFormat();

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mController.start(newFormat);
            } else if (encoderStatus < 0) {
                Log.w("VideoMediaEncoder", "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                // Codec config means that config data was pulled out and fed to the muxer when we got
                // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                boolean isCodecConfig = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                if (!isCodecConfig && mController.isStarted() && mBufferInfo.size != 0) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mController.write(mTrackIndex, encodedData, mBufferInfo);
                    mLastPresentationTime = mBufferInfo.presentationTimeUs;
                    if (mStartPresentationTime == 0) {
                        mStartPresentationTime = mLastPresentationTime;
                    }
                }
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if (!mMaxLengthReached) {
                    if (mLastPresentationTime / 1000 - mStartPresentationTime / 1000 > mMaxLengthMillis) {
                        mMaxLengthReached = true;
                        // Log.e("MediaEncoder", this.getClass().getSimpleName() + " requested stop at " + (mLastPresentationTime * 1000 * 1000));
                        mController.requestStop();
                        break;
                    }
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break; // out of while
                }
            }
        }
    }

    private long mStartPresentationTime = 0;
    private long mLastPresentationTime = 0;

    long getPresentationTime() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < mLastPresentationTime) {
            result = (mLastPresentationTime - result) + result;
        }
        return result;
    }

    abstract int getBitRate();
}
