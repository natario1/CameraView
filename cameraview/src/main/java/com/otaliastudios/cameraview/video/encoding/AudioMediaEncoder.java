package com.otaliastudios.cameraview.video.encoding;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation for audio encoding.
 */
// TODO create onVideoRecordingStart/onVideoRecordingEnd callbacks
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioMediaEncoder extends MediaEncoder {

    private static final String TAG = AudioMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT; // Determines the SAMPLE_SIZE
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO; // AudioFormat.CHANNEL_IN_STEREO;

    // The 44.1KHz frequency is the only setting guaranteed to be available on all devices.
    private static final int SAMPLING_FREQUENCY = 44100; // samples/sec
    private static final int CHANNELS_COUNT = 1; // 2;

    private static final int SAMPLE_SIZE = 2; // byte/sample/channel
    private static final int BYTE_RATE_PER_CHANNEL = SAMPLING_FREQUENCY * SAMPLE_SIZE; // byte/sec/channel
    private static final int BYTE_RATE = BYTE_RATE_PER_CHANNEL * CHANNELS_COUNT; // byte/sec
    @SuppressWarnings("unused")
    private static final int BIT_RATE = BYTE_RATE * 8; // bit/sec

    // We call FRAME here the chunk of data that we want to read at each loop cycle
    private static final int FRAME_SIZE_PER_CHANNEL = 1024; // bytes/frame/channel [AAC constant]
    private static final int FRAME_SIZE = FRAME_SIZE_PER_CHANNEL * CHANNELS_COUNT; // bytes/frame

    // We allocate buffers of 1KB each, which is not so much. This value indicates the maximum
    // number of these buffers that we can allocate at a given instant.
    // This value is the number of runnables that the encoder thread is allowed to be 'behind'
    // the recorder thread. It's not safe to have it very large or we can end encoding A LOT AFTER
    // the actual recording. It's better to reduce this and skip recording at all.
    private static final int BUFFER_POOL_MAX_SIZE = 60;

    private static long bytesToUs(int bytes) {
        return (1000000L * bytes) / BYTE_RATE;
    }

    private static long bytesToUs(long bytes) {
        return (1000000L * bytes) / BYTE_RATE;
    }

    private boolean mRequestStop = false;
    private AudioEncodingHandler mEncoder;
    private AudioRecordingThread mRecorder;
    private ByteBufferPool mByteBufferPool;
    private Config mConfig;

    public static class Config {
        int bitRate;
        public Config(int bitRate) {
            this.bitRate = bitRate;
        }
    }

    public AudioMediaEncoder(@NonNull Config config) {
        mConfig = config;
    }

    @NonNull
    @Override
    String getName() {
        return "AudioEncoder";
    }

    @EncoderThread
    @Override
    void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLING_FREQUENCY, CHANNELS_COUNT);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, CHANNELS);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNELS_COUNT);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        mByteBufferPool = new ByteBufferPool(FRAME_SIZE, BUFFER_POOL_MAX_SIZE);
        mEncoder = new AudioEncodingHandler();
        mRecorder = new AudioRecordingThread();
    }

    @EncoderThread
    @Override
    void onStart() {
        mRequestStop = false;
        mRecorder.start();
    }

    @EncoderThread
    @Override
    void onEvent(@NonNull String event, @Nullable Object data) { }

    @EncoderThread
    @Override
    void onStop() {
        mRequestStop = true;
    }

    @Override
    void onRelease() {
        mRequestStop = false;
        mEncoder = null;
        mRecorder = null;
        if (mByteBufferPool != null) {
            mByteBufferPool.clear();
            mByteBufferPool = null;
        }
    }

    @Override
    int getEncodedBitRate() {
        return mConfig.bitRate;
    }

    class AudioRecordingThread extends Thread {

        private AudioRecord mAudioRecord;
        private ByteBuffer mCurrentBuffer;
        private int mReadBytes;
        private long mLastTimeUs;

        AudioRecordingThread() {
            final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLING_FREQUENCY, CHANNELS, ENCODING);
            int bufferSize = FRAME_SIZE * 25; // Make this bigger so we don't skip frames.
            while (bufferSize < minBufferSize) {
                bufferSize += FRAME_SIZE; // Unlikely I think.
            }
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                    SAMPLING_FREQUENCY, CHANNELS, ENCODING, bufferSize);
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            mLastTimeUs = System.nanoTime() / 1000L;
            mAudioRecord.startRecording();
            while (!mRequestStop) {
                read(false);
            }
            LOG.w("Stop was requested. We're out of the loop. Will post an endOfStream.");
            // Last input with 0 length. This will signal the endOfStream.
            // Can't use drain(true); it is only available when writing to the codec InputSurface.
            read(true);
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        private void read(boolean endOfStream) {
            mCurrentBuffer = mByteBufferPool.get();
            if (mCurrentBuffer == null) {
                LOG.e("read thread - eos:", endOfStream, "- Skipping audio frame, encoding is too slow.");
                // Should fix the next presentation time here, but
            } else {
                mCurrentBuffer.clear();
                mReadBytes = mAudioRecord.read(mCurrentBuffer, FRAME_SIZE);
                LOG.i("read thread - eos:", endOfStream, "- Read new audio frame. Bytes:", mReadBytes);
                if (mReadBytes > 0) { // Good read: increase PTS.
                    mLastTimeUs = increaseTime(mReadBytes);
                    LOG.i("read thread - eos:", endOfStream, "- Frame PTS:", mLastTimeUs);
                    mCurrentBuffer.limit(mReadBytes);
                    onBuffer(endOfStream);
                } else if (mReadBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                    LOG.e("read thread - eos:", endOfStream, "- Got AudioRecord.ERROR_INVALID_OPERATION");
                } else if (mReadBytes == AudioRecord.ERROR_BAD_VALUE) {
                    LOG.e("read thread - eos:", endOfStream, "- Got AudioRecord.ERROR_BAD_VALUE");
                }
            }
        }

        /**
         * New data at position buffer.position() of size buffer.remaining()
         * has been written into this buffer. This method should pass the data
         * to the consumer.
         */
        private void onBuffer(boolean endOfStream) {
            LOG.v("read thread - Sending buffer to encoder thread.");
            mEncoder.sendInputBuffer(mCurrentBuffer, mLastTimeUs, endOfStream);
        }

        private long increaseTime(int readBytes) {
            return increaseTime3(readBytes);
        }

        /**
         * This method simply assumes that we read everything without losing a single US.
         * It will use System.nanoTime() just once, as the starting point.
         * Of course we don't as there are things going on in this thread.
         */
        @SuppressWarnings("unused")
        private long increaseTime1(int readBytes) {
            return mLastTimeUs + bytesToUs(readBytes);
        }

        /**
         * Just for testing, this method will use Api 24 method to retrieve the timestamp.
         * This way we let the platform choose instead of making assumptions.
         */
        @SuppressWarnings("unused")
        @RequiresApi(24)
        private long increaseTime2(int readBytes) {
            if (mApi24Timestamp == null) {
                mApi24Timestamp = new AudioTimestamp();
            }
            mAudioRecord.getTimestamp(mApi24Timestamp, AudioTimestamp.TIMEBASE_MONOTONIC);
            return mApi24Timestamp.nanoTime / 1000;
        }
        private AudioTimestamp mApi24Timestamp;

        /**
         * This method looks like an improvement over {@link #increaseTime1(int)} as it
         * accounts for the current time as well. Adapted & improved. from Kickflip.
         *
         * This creates regular timestamps unless we accumulate a lot of delay (greater than
         * twice the buffer duration), in which case it creates a gap and starts again trying
         * to be regular from the new point.
         */
        private long increaseTime3(int readBytes) {
            long bufferDurationUs = bytesToUs(readBytes);
            long bufferEndTimeUs = System.nanoTime() / 1000; // now
            long bufferStartTimeUs = bufferEndTimeUs - bufferDurationUs;

            // If this is the first time, the base time is the buffer start time.
            if (mBytesSinceBaseTime == 0) mBaseTimeUs = bufferStartTimeUs;

            // Recompute time assuming that we are respecting the sampling frequency.
            // This puts the time at the end of last read buffer, which means, where we
            // should be if we had no delay / missed buffers.
            long correctedTimeUs = mBaseTimeUs + bytesToUs(mBytesSinceBaseTime);
            long correctionUs = bufferStartTimeUs - correctedTimeUs;

            // However, if the correction is too big (> 2*bufferDurationUs), reset to this point.
            // This is triggered if we lose buffers and are recording/encoding at a slower rate.
            if (correctionUs >= 2L * bufferDurationUs) {
                mBaseTimeUs = bufferStartTimeUs;
                mBytesSinceBaseTime = readBytes;
                return mBaseTimeUs;
            } else {
                mBytesSinceBaseTime += readBytes;
                return correctedTimeUs;
            }
        }
        private long mBaseTimeUs;
        private long mBytesSinceBaseTime;
    }

    /**
     * This will be a super busy thread. It's important for it to be:
     * - different than the recording thread: or we would miss a lot of audio
     * - different than the 'encoder' thread: we want that to be reactive.
     *   For example, a stop() must become onStop() soon, can't wait for all this draining.
     */
    @SuppressLint("HandlerLeak")
    class AudioEncodingHandler extends Handler {

        InputBufferPool mInputBufferPool = new InputBufferPool();
        LinkedBlockingQueue<InputBuffer> mPendingOps = new LinkedBlockingQueue<>();

        AudioEncodingHandler() {
            super(WorkerHandler.get("AudioEncodingHandler").getLooper());
        }

        void sendInputBuffer(ByteBuffer buffer, long presentationTimeUs, boolean endOfStream) {
            int presentation1 = (int) (presentationTimeUs >> 32);
            int presentation2 = (int) (presentationTimeUs);
            sendMessage(obtainMessage(endOfStream ? 1 : 0, presentation1, presentation2, buffer));
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean endOfStream = msg.what == 1;
            long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
            LOG.i("encoding thread - got buffer. timestamp:", timestamp, "eos:", endOfStream);
            ByteBuffer buffer = (ByteBuffer) msg.obj;
            int readBytes = buffer.remaining();
            InputBuffer inputBuffer = mInputBufferPool.get();
            //noinspection ConstantConditions
            inputBuffer.source = buffer;
            inputBuffer.timestamp = timestamp;
            inputBuffer.length = readBytes;
            inputBuffer.isEndOfStream = endOfStream;
            mPendingOps.add(inputBuffer);
            performPendingOps(endOfStream);
        }

        private void performPendingOps(boolean force) {
            LOG.i("encoding thread - performing", mPendingOps.size(), "pending operations. force:", force);
            InputBuffer buffer;
            while ((buffer = mPendingOps.peek()) != null) {
                if (force) {
                    acquireInputBuffer(buffer);
                    performPendingOp(buffer);
                } else if (tryAcquireInputBuffer(buffer)) {
                    performPendingOp(buffer);
                } else {
                    break; // Will try later.
                }
            }
        }

        private void performPendingOp(InputBuffer buffer) {
            LOG.i("encoding thread - performing pending operation for timestamp:", buffer.timestamp, "- encoding.");
            buffer.data.put(buffer.source); // TODO this copy is prob. the worst part here for performance
            mByteBufferPool.recycle(buffer.source);
            mPendingOps.remove(buffer);
            encodeInputBuffer(buffer);
            boolean eos = buffer.isEndOfStream;
            mInputBufferPool.recycle(buffer);
            if (eos) mInputBufferPool.clear();
            LOG.i("encoding thread - performing pending operation for timestamp:", buffer.timestamp, "- draining.");
            // NOTE: can consider calling this drainOutput on yet another thread, which would let us
            // use an even smaller BUFFER_POOL_MAX_SIZE without losing audio frames. But this way
            // we can accumulate delay on this new thread without noticing (no pool getting empty).
            if (true) {
                drainOutput(eos);
                if (eos) WorkerHandler.get("AudioEncodingHandler").getThread().interrupt();
            } else {
                // Testing the option above.
                WorkerHandler.get("AudioEncodingDrainer").remove(drainRunnable);
                WorkerHandler.get("AudioEncodingDrainer").remove(drainRunnableEos);
                WorkerHandler.get("AudioEncodingDrainer").post(eos ? drainRunnableEos : drainRunnable);
            }
        }

        private final Runnable drainRunnable = new Runnable() {
            @Override
            public void run() {
                drainOutput(false);
            }
        };

        private final Runnable drainRunnableEos = new Runnable() {
            @Override
            public void run() {
                drainOutput(true);
                WorkerHandler.get("AudioEncodingHandler").getThread().interrupt();
                WorkerHandler.get("AudioEncodingDrainer").getThread().interrupt();
            }
        };
    }
}
