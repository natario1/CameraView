package com.otaliastudios.cameraview;

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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

// TODO create onVideoRecordingStart/onVideoRecordingEnd callbacks
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class AudioMediaEncoder extends MediaEncoder {

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

    static final int BIT_RATE = BYTE_RATE * 8; // bit/sec

    // We call FRAME here the chunk of data that we want to read at each loop cycle
    private static final int FRAME_SIZE_PER_CHANNEL = 1024; // bytes/frame/channel [AAC constant]
    private static final int FRAME_SIZE = FRAME_SIZE_PER_CHANNEL * CHANNELS_COUNT; // bytes/frame

    // We allocate buffers of 1KB each, which is not so much. I would say that allocating
    // at most 200 of them is a reasonable value. With the current setup, in device tests,
    // we manage to use 50 at most.
    private static final int BUFFER_POOL_MAX_SIZE = 200;

    private boolean mRequestStop = false;
    private AudioEncodingHandler mEncoder;
    private AudioRecordingThread mRecorder;
    private ByteBufferPool mByteBufferPool;
    private Config mConfig;

    static class Config {
        int bitRate;
        Config(int bitRate) {
            this.bitRate = bitRate;
        }
    }

    AudioMediaEncoder(@NonNull Config config) {
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
            LOG.w("RECORDER: Stop was requested. We're out of the loop. Will post an endOfStream.");
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
                LOG.e("Skipping audio frame, encoding is too slow.");
                // TODO should fix the next presentation time here. However this is
                // extremely unlikely based on my tests. The mByteBufferPool should be big enough.
            } else {
                mCurrentBuffer.clear();
                mReadBytes = mAudioRecord.read(mCurrentBuffer, FRAME_SIZE);
                if (mReadBytes > 0) { // Good read: increase PTS.
                    increaseTime(mReadBytes);
                    mCurrentBuffer.limit(mReadBytes);
                    onBuffer(endOfStream);
                } else if (mReadBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                    LOG.e("Got AudioRecord.ERROR_INVALID_OPERATION");
                } else if (mReadBytes == AudioRecord.ERROR_BAD_VALUE) {
                    LOG.e("Got AudioRecord.ERROR_BAD_VALUE");
                }
            }
        }

        /**
         * New data at position buffer.position() of size buffer.remaining()
         * has been written into this buffer. This method should pass the data
         * to the consumer.
         */
        private void onBuffer(boolean endOfStream) {
            mEncoder.sendInputBuffer(mCurrentBuffer, mLastTimeUs, endOfStream);
        }

        private void increaseTime(int readBytes) {
            increaseTime3(readBytes);
            LOG.v("Read", readBytes, "bytes, increasing PTS to", mLastTimeUs);
        }

        /**
         * This method simply assumes that we read everything without losing a single US.
         * It will use System.nanoTime() just once, as the starting point.
         * Of course we don't as there are things going on in this thread.
         */
        private void increaseTime1(int readBytes) {
            mLastTimeUs += (1000000L * readBytes) / BYTE_RATE;
        }

        /**
         * Just for testing, this method will use Api 24 method to retrieve the timestamp.
         * This way we let the platform choose instead of making assumptions.
         */
        @RequiresApi(24)
        private void increaseTime2(int readBytes) {
            if (mApi24Timestamp == null) {
                mApi24Timestamp = new AudioTimestamp();
            }
            mAudioRecord.getTimestamp(mApi24Timestamp, AudioTimestamp.TIMEBASE_MONOTONIC);
            mLastTimeUs = mApi24Timestamp.nanoTime / 1000;
        }
        private AudioTimestamp mApi24Timestamp;

        /**
         * This method looks like an improvement over {@link #increaseTime1(int)} as it
         * accounts for the current time as well. Adapted & improved. from Kickflip.
         */
        private void increaseTime3(int readBytes) {
            long currentTime = System.nanoTime() / 1000;
            long correctedTime;
            long bufferDuration = (1000000 * readBytes) / BYTE_RATE;
            long bufferTime = currentTime - bufferDuration; // delay of acquiring the audio buffer
            if (mTotalReadBytes == 0) {
                mStartTimeUs = bufferTime;
            }
            // Recompute time assuming that we are respecting the sampling frequency.
            // However, if the correction is too big (> 2*bufferDuration), reset to this point.
            correctedTime = mStartTimeUs + (1000000 * mTotalReadBytes) / BYTE_RATE;
            if(bufferTime - correctedTime >= 2 * bufferDuration) {
                mStartTimeUs = bufferTime;
                mTotalReadBytes = 0;
                correctedTime = mStartTimeUs;
            }
            mTotalReadBytes += readBytes;
            mLastTimeUs = correctedTime;
        }
        private long mStartTimeUs;
        private long mTotalReadBytes;
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
            ByteBuffer buffer = (ByteBuffer) msg.obj;
            int readBytes = buffer.remaining();
            InputBuffer inputBuffer = mInputBufferPool.get();
            inputBuffer.source = buffer;
            inputBuffer.timestamp = timestamp;
            inputBuffer.length = readBytes;
            inputBuffer.isEndOfStream = endOfStream;
            mPendingOps.add(inputBuffer);
            performPendingOps(endOfStream);
        }

        private void performPendingOps(boolean force) {
            LOG.v("Performing", mPendingOps.size(), "Pending operations.");
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
            buffer.data.put(buffer.source);
            mByteBufferPool.recycle(buffer.source);
            mPendingOps.remove(buffer);
            encodeInputBuffer(buffer);
            boolean eos = buffer.isEndOfStream;
            mInputBufferPool.recycle(buffer);
            drainOutput(eos);
            if (eos) {
                mInputBufferPool.clear();
                WorkerHandler.get("AudioEncodingHandler").getThread().interrupt();
            }
        }
    }
}
