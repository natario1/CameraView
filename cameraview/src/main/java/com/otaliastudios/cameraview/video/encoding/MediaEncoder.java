package com.otaliastudios.cameraview.video.encoding;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;

import java.nio.ByteBuffer;

/**
 * Base class for single-track encoders, coordinated by a {@link MediaEncoderEngine}.
 * For the lifecycle of this class, read comments in the engine class.
 *
 * This class manages a background thread and streamlines events on this thread
 * which we call the {@link EncoderThread}:
 *
 * 1. When {@link #prepare(MediaEncoderEngine.Controller, long)} is called, we call
 *    {@link #onPrepare(MediaEncoderEngine.Controller, long)} on the encoder thread.
 *
 * 2. When {@link #start()} is called, we call {@link #onStart()} on the encoder thread.
 *
 * 3. When {@link #notify(String, Object)} is called, we call {@link #onEvent(String, Object)}
 *    on the encoder thread.
 *
 * 4. After starting, encoders are free to acquire an input buffer with
 *    {@link #tryAcquireInputBuffer(InputBuffer)} or {@link #acquireInputBuffer(InputBuffer)}.
 *
 * 5. After getting the input buffer, they are free to fill it with data.
 *
 * 6. After filling it with data, they are required to call {@link #encodeInputBuffer(InputBuffer)}
 *    for encoding to take place.
 *
 * 7. After this happens, or at regular intervals, or whenever they want, encoders can then
 *    call {@link #drainOutput(boolean)} with a false parameter to fetch the encoded data
 *    and pass it to the engine (so it can be written to the muxer).
 *
 * 8. When {@link #stop()} is called - either by the engine user, or as a consequence of having
 *    called {@link MediaEncoderEngine.Controller#requestStop(int)} - we call
 *    {@link #onStop()} on the encoder thread.
 *
 * 9. The {@link #onStop()} implementation should, as fast as possible, stop reading, signal the
 *    end of input stream (there are two ways to do so), and finally call
 *    {@link #drainOutput(boolean)} for the last time, with a true parameter.
 *
 * 10. Once everything is drained, we will call {@link #onStopped()}, on a unspecified thread.
 *     There, subclasses can perform extra cleanup of their own resources.
 *
 * For VIDEO encoders, things are much easier because we skip the whole input part.
 * See description in {@link VideoMediaEncoder}.
 */
// https://github.com/saki4510t/AudioVideoRecordingSample/blob/master/app/src/main/java/com/serenegiant/encoder/MediaEncoder.java
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
abstract class MediaEncoder {

    private final static String TAG = MediaEncoder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    // Did some test to see which value would maximize our performance in the current setup (infinite audio pool).
    // Measured the time it would take to write a 30 seconds video. Based on this, we'll go with TIMEOUT=0 for now.
    // INPUT_TIMEOUT_US 10000: 46 seconds
    // INPUT_TIMEOUT_US 1000: 37 seconds
    // INPUT_TIMEOUT_US 100: 33 seconds
    // INPUT_TIMEOUT_US 0: 32 seconds
    private final static int INPUT_TIMEOUT_US = 0;

    // 0 also seems to be the best, although it does not change so much.
    // Can't go too high or this is a bottleneck for the audio encoder.
    private final static int OUTPUT_TIMEOUT_US = 0;

    private final static int STATE_NONE = 0;
    private final static int STATE_PREPARING = 1;
    private final static int STATE_PREPARED = 2;
    private final static int STATE_STARTING = 3;
    private final static int STATE_STARTED = 4;
    // max timestamp was reached. we will keep draining but have asked the engine to stop us.
    // this step can be skipped in case stop() is called from outside before a limit is reached.
    private final static int STATE_LIMIT_REACHED = 5;
    private final static int STATE_STOPPING = 6;
    private final static int STATE_STOPPED = 7;

    private int mState = STATE_NONE;
    private final String mName;

    @SuppressWarnings("WeakerAccess")
    protected MediaCodec mMediaCodec;

    @SuppressWarnings("WeakerAccess")
    protected WorkerHandler mWorker;

    private MediaEncoderEngine.Controller mController;
    private int mTrackIndex;
    private OutputBufferPool mOutputBufferPool;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodecBuffers mBuffers;
    private long mMaxLengthMillis;
    private boolean mMaxLengthReached;

    /**
     * Needs a readable name for the thread and for logging.
     * @param name a name
     */
    MediaEncoder(@NonNull String name) {
        mName = name;
    }

    private void setState(int newState) {
        String newStateName = null;
        switch (newState) {
            case STATE_NONE: newStateName = "NONE"; break;
            case STATE_PREPARING: newStateName = "PREPARING"; break;
            case STATE_PREPARED: newStateName = "PREPARED"; break;
            case STATE_STARTING: newStateName = "STARTING"; break;
            case STATE_STARTED: newStateName = "STARTED"; break;
            case STATE_LIMIT_REACHED: newStateName = "LIMIT_REACHED"; break;
            case STATE_STOPPING: newStateName = "STOPPING"; break;
            case STATE_STOPPED: newStateName = "STOPPED"; break;
        }
        LOG.w(mName, "setState:", newStateName);
        mState = newState;
    }

    /**
     * This encoder was attached to the engine. Keep the controller
     * and run the internal thread.
     *
     * NOTE: it's important to call {@link WorkerHandler#post(Runnable)} instead of run()!
     * The internal actions can cause a stop, and due to how {@link WorkerHandler#run(Runnable)}
     * works, we might have {@link #onStop()} or {@link #onStopped()} to be executed before
     * the previous step has completed.
     */
    final void prepare(@NonNull final MediaEncoderEngine.Controller controller, final long maxLengthMillis) {
        if (mState >= STATE_PREPARING) {
            LOG.e("Wrong state while preparing. Aborting.", mState);
            return;
        }
        setState(STATE_PREPARING);
        mController = controller;
        mBufferInfo = new MediaCodec.BufferInfo();
        mMaxLengthMillis = maxLengthMillis;
        mWorker = WorkerHandler.get(mName);
        LOG.i(mName, "Prepare was called. Posting.");
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                LOG.i(mName, "Prepare was called. Executing.");
                onPrepare(controller, maxLengthMillis);
                setState(STATE_PREPARED);
            }
        });
    }

    /**
     * Start recording. This might be a lightweight operation
     * in case the encoder needs to wait for a certain event
     * like a "frame available".
     *
     * The {@link #STATE_STARTED} state will be set when draining for the
     * first time (not when onStart ends).
     *
     * NOTE: it's important to call {@link WorkerHandler#post(Runnable)} instead of run()!
     */
    final void start() {
        if (mState < STATE_PREPARED || mState >= STATE_STARTING) {
            LOG.e("Wrong state while starting. Aborting.", mState);
            return;
        }
        setState(STATE_STARTING);
        LOG.w(mName, "Start was called. Posting.");
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                LOG.w(mName, "Start was called. Executing.");
                onStart();
            }
        });
    }

    /**
     * The caller notifying of a certain event occurring.
     * Should analyze the string and see if the event is important.
     *
     * NOTE: it's important to call {@link WorkerHandler#post(Runnable)} instead of run()!
     *
     * @param event what happened
     * @param data object
     */
    final void notify(final @NonNull String event, final @Nullable Object data) {
        LOG.v(mName, "Notify was called. Posting.");
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                LOG.v(mName, "Notify was called. Executing.");
                onEvent(event, data);
            }
        });
    }

    /**
     * Stop recording. This involves signaling the end of stream and draining
     * all output left.
     *
     * The {@link #STATE_STOPPED} state will be set when draining for the
     * last time (not when onStart ends).
     *
     * NOTE: it's important to call {@link WorkerHandler#post(Runnable)} instead of run()!
     */
    final void stop() {
        if (mState >= STATE_LIMIT_REACHED) {
            LOG.e("Wrong state while stopping. Aborting.", mState);
            return;
        }
        setState(STATE_STOPPING);
        LOG.w(mName, "Stop was called. Posting.");
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                LOG.w(mName, "Stop was called. Executing.");
                onStop();
            }
        });
    }

    /**
     * Called to prepare this encoder before starting.
     * Any initialization should be done here as it does not interfere with the original
     * thread (that, generally, is the rendering thread).
     *
     * At this point subclasses MUST create the {@link #mMediaCodec} object.
     *
     * @param controller the muxer controller
     * @param maxLengthMillis the maxLength in millis
     */
    @EncoderThread
    abstract void onPrepare(@NonNull final MediaEncoderEngine.Controller controller, final long maxLengthMillis);

    /**
     * Start recording. This might be a lightweight operation
     * in case the encoder needs to wait for a certain event
     * like a "frame available".
     */
    @EncoderThread
    abstract void onStart();

    /**
     * The caller notifying of a certain event occurring.
     * Should analyze the string and see if the event is important.
     * @param event what happened
     * @param data object
     */
    @EncoderThread
    abstract void onEvent(@NonNull String event, @Nullable Object data);

    /**
     * Stop recording. This involves signaling the end of stream and draining
     * all output left.
     */
    @EncoderThread
    abstract void onStop();

    /**
     * Called by {@link #drainOutput(boolean)} when we get an EOS signal (not necessarily in the
     * parameters, might also be through an input buffer flag).
     *
     * This is a good moment to release all resources, although the muxer might still
     * be alive (we wait for the other Encoder, see MediaEncoderEngine.Controller).
     */
    @CallSuper
    protected void onStopped() {
        LOG.w(mName, "is being released. Notifying controller and releasing codecs.");
        // TODO should we notify after this method?
        mController.notifyStopped(mTrackIndex);
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
        mOutputBufferPool.clear();
        mOutputBufferPool = null;
        mBuffers = null;
        setState(STATE_STOPPED);
    }

    /**
     * Returns a new input buffer and index, waiting at most {@link #INPUT_TIMEOUT_US} if none is available.
     * Callers should check the boolean result - true if the buffer was filled.
     *
     * @param holder the input buffer holder
     * @return true if acquired
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean tryAcquireInputBuffer(@NonNull InputBuffer holder) {
        if (mBuffers == null) {
            mBuffers = new MediaCodecBuffers(mMediaCodec);
        }
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(INPUT_TIMEOUT_US);
        if (inputBufferIndex < 0) {
            return false;
        } else {
            holder.index = inputBufferIndex;
            holder.data = mBuffers.getInputBuffer(inputBufferIndex);
            return true;
        }
    }

    /**
     * Returns a new input buffer and index, waiting indefinitely if none is available.
     * The buffer should be written into, then be passed to {@link #encodeInputBuffer(InputBuffer)}.
     *
     * @param holder the input buffer holder
     */
    @SuppressWarnings({"StatementWithEmptyBody", "WeakerAccess"})
    protected void acquireInputBuffer(@NonNull InputBuffer holder) {
        while (!tryAcquireInputBuffer(holder)) {}
    }

    /**
     * Encode data into the {@link #mMediaCodec}.
     *
     * @param buffer the input buffer
     */
    @SuppressWarnings("WeakerAccess")
    protected void encodeInputBuffer(InputBuffer buffer) {
        LOG.v(mName, "ENCODING - Buffer:", buffer.index, "Bytes:", buffer.length, "Presentation:", buffer.timestamp);
        if (buffer.isEndOfStream) { // send EOS
            mMediaCodec.queueInputBuffer(buffer.index, 0, 0,
                    buffer.timestamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        } else {
            mMediaCodec.queueInputBuffer(buffer.index, 0, buffer.length,
                    buffer.timestamp, 0);
        }
    }

    /**
     * Extracts all pending data that was written and encoded into {@link #mMediaCodec},
     * and forwards it to the muxer.
     *
     * If drainAll is not set, this returns after TIMEOUT_USEC if there is no more data to drain.
     * If drainAll is set, we wait until we see EOS on the output.
     * Calling this with drainAll set should be done once, right before stopping the muxer.
     *
     * @param drainAll whether to drain all
     */
    @SuppressLint("LogNotTimber")
    @SuppressWarnings("WeakerAccess")
    protected void drainOutput(boolean drainAll) {
        LOG.v(mName, "DRAINING - EOS:", drainAll);
        if (mMediaCodec == null) {
            LOG.e("drain() was called before prepare() or after releasing.");
            return;
        }
        if (mBuffers == null) {
            mBuffers = new MediaCodecBuffers(mMediaCodec);
        }
        while (true) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, OUTPUT_TIMEOUT_US);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!drainAll) break; // out of while

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                mBuffers.onOutputBuffersChanged();

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mController.isStarted()) throw new RuntimeException("MediaFormat changed twice.");
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                mTrackIndex = mController.notifyStarted(newFormat);
                setState(STATE_STARTED);
                mOutputBufferPool = new OutputBufferPool(mTrackIndex);
            } else if (encoderStatus < 0) {
                LOG.e("Unexpected result from dequeueOutputBuffer: " + encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = mBuffers.getOutputBuffer(encoderStatus);

                // Codec config means that config data was pulled out and fed to the muxer when we got
                // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                boolean isCodecConfig = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                if (!isCodecConfig && mController.isStarted() && mBufferInfo.size != 0) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    // Store startPresentationTime and lastPresentationTime, useful for example to
                    // detect the mMaxLengthReached and stop recording.
                    if (mStartPresentationTimeUs == Long.MIN_VALUE) {
                        mStartPresentationTimeUs = mBufferInfo.presentationTimeUs;
                    }
                    mLastPresentationTimeUs = mBufferInfo.presentationTimeUs;
                    // Pass presentation times as offets with respect to the mStartPresentationTimeUs.
                    // This ensures consistency between audio pts (coming from System.nanoTime()) and
                    // video pts (coming from SurfaceTexture) both of which have no meaningful time-base
                    // and should be used for offsets only.
                    // TODO find a better way, this causes sync issues. (+ note: this sends pts=0 at first)
                    // mBufferInfo.presentationTimeUs = mLastPresentationTimeUs - mStartPresentationTimeUs;
                    LOG.v(mName, "DRAINING - About to write(). Presentation:", mBufferInfo.presentationTimeUs);

                    // TODO fix the mBufferInfo being the same, then implement delayed writing in Controller
                    // and remove the isStarted() check here.
                    OutputBuffer buffer = mOutputBufferPool.get();
                    //noinspection ConstantConditions
                    buffer.info = mBufferInfo;
                    buffer.trackIndex = mTrackIndex;
                    buffer.data = encodedData;
                    mController.write(mOutputBufferPool, buffer);
                }
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                // Check for the maxLength constraint (with appropriate conditions)
                // Not needed if drainAll because we already were asked to stop
                if (!drainAll
                        && !mMaxLengthReached
                        && mStartPresentationTimeUs != Long.MIN_VALUE
                        && mLastPresentationTimeUs - mStartPresentationTimeUs > mMaxLengthMillis * 1000) {
                    LOG.w(mName, "DRAINING - Reached maxLength! mLastPresentationTimeUs:", mLastPresentationTimeUs,
                            "mStartPresentationTimeUs:", mStartPresentationTimeUs,
                            "mMaxLengthUs:", mMaxLengthMillis * 1000);
                    mMaxLengthReached = true;
                    LOG.w(mName, "DRAINING - Requesting a stop.");
                    setState(STATE_LIMIT_REACHED);
                    mController.requestStop(mTrackIndex);
                    break;
                }

                // Check for the EOS flag so we can call onStopped.
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    LOG.w(mName, "DRAINING - Got EOS. Releasing the codec.");
                    onStopped();
                    break;
                }
            }
        }
    }

    private long mStartPresentationTimeUs = Long.MIN_VALUE;
    private long mLastPresentationTimeUs = 0;

    abstract int getEncodedBitRate();
}
