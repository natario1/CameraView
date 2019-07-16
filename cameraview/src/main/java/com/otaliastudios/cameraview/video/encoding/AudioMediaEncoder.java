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
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation for audio encoding.
 */
// TODO create onVideoRecordingEnd callbacks
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioMediaEncoder extends MediaEncoder {

    private static final String TAG = AudioMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    // We allocate buffers of 1KB each, which is not so much. This value indicates the maximum
    // number of these buffers that we can allocate at a given instant.
    // This value is the number of runnables that the encoder thread is allowed to be 'behind'
    // the recorder thread. It's not safe to have it very large or we can end encoding A LOT AFTER
    // the actual recording. It's better to reduce this and skip recording at all.
    private static final int BUFFER_POOL_MAX_SIZE = 80;
    private static final boolean PERFORMANCE_DEBUG = true;
    private static final boolean PERFORMANCE_SEPARATE_ENCODING_THREAD = true; // not clear how much benefit this brings
    private static final boolean PERFORMANCE_FILL_GAPS = false;

    private boolean mRequestStop = false;
    private AudioEncodingHandler mEncoder;
    private AudioRecordingThread mRecorder;
    private ByteBufferPool mByteBufferPool;
    private ByteBuffer mZeroBuffer;
    private final AudioTimestamp mTimestamp;
    private Config mConfig;

    /**
     * Audio configuration to be passed as input to the constructor.
     */
    public static class Config {

        // Configurable options
        public int bitRate; // ENCODED bit rate
        public int channels = 1;

        // Not configurable options (for now)
        private final String mimeType = "audio/mp4a-latm";
        private final int encoding = AudioFormat.ENCODING_PCM_16BIT; // Determines the sampleSizePerChannel
        // The 44.1KHz frequency is the only setting guaranteed to be available on all devices.
        private final int samplingFrequency = 44100; // samples/sec
        private final int sampleSizePerChannel = 2; // byte/sample/channel [16bit]
        private final int byteRatePerChannel = samplingFrequency * sampleSizePerChannel; // byte/sec/channel
        private final int frameSizePerChannel = 1024; // bytes/frame/channel [AAC constant]

        @NonNull
        private Config copy() {
            Config config = new Config();
            config.bitRate = this.bitRate;
            config.channels = this.channels;
            return config;
        }

        private int byteRate() { // RAW byte rate
            return byteRatePerChannel * channels; // byte/sec
        }

        @SuppressWarnings("unused")
        private int bitRate() { // RAW bit rate
            return byteRate() * 8; // bit/sec
        }

        private int frameSize() {
            // We call FRAME here the chunk of data that we want to read at each loop cycle
            return frameSizePerChannel * channels; // bytes/frame
        }

        private int audioFormatChannels() {
            if (channels == 1) {
                return AudioFormat.CHANNEL_IN_MONO;
            } else if (channels == 2) {
                return AudioFormat.CHANNEL_IN_STEREO;
            }
            throw new RuntimeException("Invalid number of channels: " + channels);
        }
    }

    public AudioMediaEncoder(@NonNull Config config) {
        super("AudioEncoder");
        mConfig = config.copy();
        mTimestamp = new AudioTimestamp(mConfig.byteRate());
        // These two were in onPrepare() but it's better to do warm-up here
        // since thread and looper creation is expensive.
        mEncoder = new AudioEncodingHandler();
        mRecorder = new AudioRecordingThread();
    }

    @EncoderThread
    @Override
    protected void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(mConfig.mimeType, mConfig.samplingFrequency, mConfig.channels);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, mConfig.audioFormatChannels());
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate); // TODO multiply by channels?
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mConfig.channels);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(mConfig.mimeType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        mByteBufferPool = new ByteBufferPool(mConfig.frameSize(), BUFFER_POOL_MAX_SIZE);
        mZeroBuffer = ByteBuffer.allocateDirect(mConfig.frameSize());
    }

    @EncoderThread
    @Override
    protected void onStart() {
        mRequestStop = false;
        mRecorder.start();
    }

    @EncoderThread
    @Override
    protected void onStop() {
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
    protected int getEncodedBitRate() {
        return mConfig.bitRate;
    }

    private class AudioRecordingThread extends Thread {

        private AudioRecord mAudioRecord;
        private ByteBuffer mCurrentBuffer;
        private int mReadBytes;
        private long mLastTimeUs;
        private long mFirstTimeUs = Long.MIN_VALUE;

        private AudioRecordingThread() {
            final int minBufferSize = AudioRecord.getMinBufferSize(mConfig.samplingFrequency,
                    mConfig.channels, mConfig.encoding);
            // Make this bigger so we don't skip frames. Stereo: 51200. Mono: 25600
            int bufferSize = mConfig.frameSize() * 25;
            while (bufferSize < minBufferSize) {
                bufferSize += mConfig.frameSize(); // Unlikely.
            }
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                    mConfig.samplingFrequency,
                    mConfig.channels,
                    mConfig.encoding,
                    bufferSize);
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
                // When stereo, we read twice the data here and AudioRecord will fill the buffer
                // with left and right bytes. https://stackoverflow.com/q/20594750/4288782
                if (PERFORMANCE_DEBUG) {
                    float before = System.nanoTime() / 1000000F;
                    mReadBytes = mAudioRecord.read(mCurrentBuffer, mConfig.frameSize());
                    float after = System.nanoTime() / 1000000F;
                    float delayMillis = after - before;
                    float durationMillis = AudioTimestamp.bytesToUs(mReadBytes, mConfig.byteRate()) / 1000F;
                    LOG.v("read thread - reading took:", delayMillis,
                            "should be:", durationMillis,
                            "delta:", delayMillis - durationMillis);
                } else {
                    mReadBytes = mAudioRecord.read(mCurrentBuffer, mConfig.frameSize());
                }
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
         * Sleeps for some frames duration, to skip them. This can be used to slow down
         * the recording operation to balance it with encoding.
         */
        private void sleep() {
            try {
                Thread.sleep(AudioTimestamp.bytesToMillis(
                        mConfig.frameSize() * 6,
                        mConfig.byteRate()));
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
            // Get the latest frame timestamp.
            mLastTimeUs = mTimestamp.increaseUs(readBytes);
            if (mFirstTimeUs == Long.MIN_VALUE) {
                mFirstTimeUs = mLastTimeUs;
                // Compute the first frame milliseconds as well.
                notifyFirstFrameMillis(System.currentTimeMillis()
                        - AudioTimestamp.bytesToMillis(readBytes, mConfig.byteRate()));
            }

            // See if we reached the max length value.
            boolean didReachMaxLength = (mLastTimeUs - mFirstTimeUs) > getMaxLengthMillis() * 1000L;
            if (didReachMaxLength && !endOfStream) {
                LOG.w("read thread - this frame reached the maxLength! deltaUs:", mLastTimeUs - mFirstTimeUs);
                notifyMaxLengthReached();
            }

            // Add zeroes if we have huge gaps. Even if timestamps are correct, if we have gaps between
            // them, the encoder might shrink all timestamps to have a continuous audio. This results
            // in a video that is fast-forwarded.
            // Adding zeroes does not solve the gaps issue - audio will still be distorted. But at
            // least we get a video that has the correct playback speed.
            if (PERFORMANCE_FILL_GAPS) {
                int gaps = mTimestamp.getGapCount(mConfig.frameSize());
                if (gaps > 0) {
                    long gapStart = mTimestamp.getGapStartUs(mLastTimeUs);
                    long frameUs = AudioTimestamp.bytesToUs(mConfig.frameSize(), mConfig.byteRate());
                    LOG.w("read thread - GAPS: trying to add", gaps, "zeroed buffers");
                    for (int i = 0; i < gaps; i++) {
                        ByteBuffer zeroBuffer = mByteBufferPool.get();
                        if (zeroBuffer == null) {
                            LOG.e("read thread - GAPS: aborting because we have no free buffer.");
                            break;
                        }
                        ;
                        zeroBuffer.position(0);
                        zeroBuffer.put(mZeroBuffer);
                        zeroBuffer.clear();
                        mEncoder.sendInputBuffer(zeroBuffer, gapStart, false);
                        gapStart += frameUs;
                    }
                }
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
            // TODO this thread has warmup issues, the first recording is always messy. Investigate.
            // getLooper().getThread().setPriority(Thread.MAX_PRIORITY);
        }

        // Just to debug performance.
        private int mSendCount = 0;
        private int mExecuteCount = 0;
        private long mAvgSendDelay = 0;
        private long mAvgExecuteDelay = 0;
        private Map<Long, Long> mSendStartMap = new HashMap<>();

        private void sendInputBuffer(ByteBuffer buffer, long presentationTimeUs, boolean endOfStream) {
            if (PERFORMANCE_DEBUG) {
                mSendStartMap.put(presentationTimeUs, System.nanoTime() / 1000000);
            }
            Message message = obtainMessage(
                    endOfStream ? 1 : 0,
                    (int) (presentationTimeUs >> 32),
                    (int) (presentationTimeUs),
                    buffer);
            if (PERFORMANCE_SEPARATE_ENCODING_THREAD) {
                sendMessage(message);
            } else {
                handleMessage(message);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
            boolean endOfStream = msg.what == 1;
            LOG.i("encoding thread - got buffer. timestamp:", timestamp, "eos:", endOfStream);

            // Performance logging
            long executeStart;
            if (PERFORMANCE_DEBUG) {
                long sendEnd = System.nanoTime() / 1000000;
                //noinspection ConstantConditions
                long sendStart = mSendStartMap.remove(timestamp);
                mAvgSendDelay = ((mAvgSendDelay * mSendCount) + (sendEnd - sendStart)) / (++mSendCount);
                LOG.v("send delay millis:", sendEnd - sendStart, "average:", mAvgSendDelay);
                executeStart = System.nanoTime() / 1000000;
            }

            // Actual work
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

            if (PERFORMANCE_DEBUG) {
                long executeEnd = System.nanoTime() / 1000000;
                mAvgExecuteDelay = ((mAvgExecuteDelay * mExecuteCount) + (executeEnd - executeStart)) / (++mExecuteCount);
                LOG.v("execute delay millis:", executeEnd - executeStart, "average:", mAvgExecuteDelay);
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
            if (eos) {
                // Not sure we want this: WorkerHandler.get("AudioEncodingHandler").getThread().interrupt();
                if (PERFORMANCE_DEBUG) {
                    // With MONO, the count is about 370. With STEREO, the count is about 220.
                    // This reflects the count difference that we see in engine.write (MONO:200 STEREO:100).
                    LOG.e("EXECUTE DELAY MILLIS:", mAvgExecuteDelay, "COUNT:", mExecuteCount);
                    LOG.e("SEND DELAY MILLIS:", mAvgSendDelay, "COUNT:", mSendCount);
                }
            }
        }
    }
}
