package com.otaliastudios.cameraview.video.encoding;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
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
    private static final int BUFFER_POOL_MAX_SIZE = 80;

    private boolean mRequestStop = false;
    private AudioEncodingHandler mEncoder;
    private AudioRecordingThread mRecorder;
    private ByteBufferPool mByteBufferPool;
    private final AudioTimestamp mTimestamp;
    private Config mConfig;

    public static class Config {
        public int bitRate;

        @NonNull
        private Config copy() {
            Config config = new Config();
            config.bitRate = this.bitRate;
            return config;
        }
    }

    public AudioMediaEncoder(@NonNull Config config) {
        super("AudioEncoder");
        mConfig = config.copy();
        mTimestamp = new AudioTimestamp();
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
    void onStop() {
        mRequestStop = true;
    }

    @Override
    protected void onStopped() {
        super.onStopped();
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

    private class AudioRecordingThread extends Thread {

        private AudioRecord mAudioRecord;
        private ByteBuffer mCurrentBuffer;
        private int mReadBytes;
        private long mLastTimeUs;
        private long mFirstTimeUs = Long.MIN_VALUE;

        private AudioRecordingThread() {
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
                // This can happen and it means that encoding is slow with respect to recording.
                // One might be tempted to fix precisely the next frame presentation time when this happens,
                // but this is not needed because the current increaseTime() algorithm will consider delays
                // when they get large.
                // Sleeping before returning is a good way of balancing the two operations.
                // However, if endOfStream, we CAN'T lose this frame!
                if (endOfStream) {
                    LOG.v("read thread - eos: true - No buffer, retrying.");
                    read(true); // try again
                } else {
                    LOG.w("read thread - eos: false - Skipping audio frame, encoding is too slow.");
                    sleep(); // sleep a bit
                }
            } else {
                mCurrentBuffer.clear();
                mReadBytes = mAudioRecord.read(mCurrentBuffer, FRAME_SIZE);
                LOG.i("read thread - eos:", endOfStream, "- Read new audio frame. Bytes:", mReadBytes);
                if (mReadBytes > 0) { // Good read: increase PTS.
                    increaseTime(mReadBytes, endOfStream);
                    LOG.i("read thread - eos:", endOfStream, "- mLastTimeUs:", mLastTimeUs);
                    mCurrentBuffer.limit(mReadBytes);
                    mEncoder.sendInputBuffer(mCurrentBuffer, mLastTimeUs, endOfStream);
                } else if (mReadBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                    LOG.e("read thread - eos:", endOfStream, "- Got AudioRecord.ERROR_INVALID_OPERATION");
                } else if (mReadBytes == AudioRecord.ERROR_BAD_VALUE) {
                    LOG.e("read thread - eos:", endOfStream, "- Got AudioRecord.ERROR_BAD_VALUE");
                }
            }
        }

        /**
         * Sleeps for a frame duration, to skip it. This can be used to slow down
         * the recording operation to balance it with encoding.
         */
        private void sleep() {
            try {
                Thread.sleep(AudioTimestamp.bytesToUs(FRAME_SIZE, BYTE_RATE) / 1000);
            } catch (InterruptedException ignore) {}
        }

        /**
         * Increases presentation time and checks for max length constraint. This is much faster
         * then waiting for the encoder to check it during {@link #drainOutput(boolean)}. We
         * want to catch this as soon as possible so we stop recording useless frames and bother
         * all the threads involved.
         * @param readBytes bytes read in last reading
         * @param endOfStream end of stream?
         */
        private void increaseTime(int readBytes, boolean endOfStream) {
            mLastTimeUs = mTimestamp.increaseUs(readBytes, BYTE_RATE);
            if (mFirstTimeUs == Long.MIN_VALUE) {
                mFirstTimeUs = mLastTimeUs;
            }
            boolean didReachMaxLength = (mLastTimeUs - mFirstTimeUs) > getMaxLengthMillis() * 1000L;
            if (didReachMaxLength && !endOfStream) {
                LOG.w("read thread - this frame reached the maxLength! deltaUs:", mLastTimeUs - mFirstTimeUs);
                notifyMaxLengthReached();
            }
        }

    }

    /**
     * This will be a super busy thread. It's important for it to be:
     * - different than the recording thread: or we would miss a lot of audio
     * - different than the 'encoder' thread: we want that to be reactive.
     *   For example, a stop() must become onStop() soon, can't wait for all this draining.
     */
    @SuppressLint("HandlerLeak")
    private class AudioEncodingHandler extends Handler {

        private InputBufferPool mInputBufferPool = new InputBufferPool();
        private LinkedBlockingQueue<InputBuffer> mPendingOps = new LinkedBlockingQueue<>();

        private AudioEncodingHandler() {
            super(WorkerHandler.get("AudioEncodingHandler").getLooper());
        }

        private void sendInputBuffer(ByteBuffer buffer, long presentationTimeUs, boolean endOfStream) {
            sendMessage(obtainMessage(
                    endOfStream ? 1 : 0,
                    (int) (presentationTimeUs >> 32),
                    (int) (presentationTimeUs),
                    buffer));
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

            LOG.i("encoding thread - performing", mPendingOps.size(), "pending operations.");
            while ((inputBuffer = mPendingOps.peek()) != null) {
                if (endOfStream) {
                    acquireInputBuffer(inputBuffer);
                    performPendingOp(inputBuffer);
                } else if (tryAcquireInputBuffer(inputBuffer)) {
                    performPendingOp(inputBuffer);
                } else {
                    break; // Will try later.
                }
            }
        }

        private void performPendingOp(InputBuffer buffer) {
            LOG.i("encoding thread - performing pending operation for timestamp:", buffer.timestamp, "- encoding.");
            buffer.data.put(buffer.source); // NOTE: this copy is prob. the worst part here for performance
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
            drainOutput(eos);
            if (eos) WorkerHandler.get("AudioEncodingHandler").getThread().interrupt();
        }
    }
}
