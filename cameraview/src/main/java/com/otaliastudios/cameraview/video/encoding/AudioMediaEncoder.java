package com.otaliastudios.cameraview.video.encoding;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation for audio encoding.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioMediaEncoder extends MediaEncoder {

    private static final String TAG = AudioMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final boolean PERFORMANCE_DEBUG = false;
    private static final boolean PERFORMANCE_FILL_GAPS = true;
    private static final int PERFORMANCE_MAX_GAPS = 8;

    private boolean mRequestStop = false;
    private AudioEncodingThread mEncoder;
    private AudioRecordingThread mRecorder;
    private ByteBufferPool mByteBufferPool;
    private final AudioTimestamp mTimestamp;
    private AudioConfig mConfig;
    private InputBufferPool mInputBufferPool = new InputBufferPool();
    private final LinkedBlockingQueue<InputBuffer> mInputBufferQueue = new LinkedBlockingQueue<>();
    private AudioNoise mAudioNoise;

    // Just to debug performance.
    private int mDebugSendCount = 0;
    private int mDebugExecuteCount = 0;
    private long mDebugSendAvgDelay = 0;
    private long mDebugExecuteAvgDelay = 0;
    private Map<Long, Long> mDebugSendStartMap = new HashMap<>();

    public AudioMediaEncoder(@NonNull AudioConfig config) {
        super("AudioEncoder");
        mConfig = config.copy();
        mTimestamp = new AudioTimestamp(mConfig.byteRate());
        // These two were in onPrepare() but it's better to do warm-up here
        // since thread and looper creation is expensive.
        mEncoder = new AudioEncodingThread();
        mRecorder = new AudioRecordingThread();
    }

    @EncoderThread
    @Override
    protected void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthUs) {
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(
                mConfig.mimeType,
                mConfig.samplingFrequency,
                mConfig.channels);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, mConfig.audioFormatChannels());
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        try {
            if (mConfig.encoder != null) {
                mMediaCodec = MediaCodec.createByCodecName(mConfig.encoder);
            } else {
                mMediaCodec = MediaCodec.createEncoderByType(mConfig.mimeType);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mMediaCodec.configure(audioFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        mByteBufferPool = new ByteBufferPool(mConfig.frameSize(), mConfig.bufferPoolMaxSize());
        mAudioNoise = new AudioNoise(mConfig);
    }

    @EncoderThread
    @Override
    protected void onStart() {
        mRequestStop = false;
        mRecorder.start();
        mEncoder.start();
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

    /**
     * Sleeps for some frames duration, to skip them. This can be used to slow down
     * the recording operation to balance it with encoding.
     */
    private void skipFrames(int frames) {
        try {
            Thread.sleep(AudioTimestamp.bytesToMillis(
                    mConfig.frameSize() * frames,
                    mConfig.byteRate()));
        } catch (InterruptedException ignore) {}
    }

    /**
     * A thread recording from microphone using {@link AudioRecord} class.
     * Communicates with {@link AudioEncodingThread} using {@link #mInputBufferQueue}.
     */
    private class AudioRecordingThread extends Thread {

        private AudioRecord mAudioRecord;
        private ByteBuffer mCurrentBuffer;
        private int mCurrentReadBytes;

        private long mLastTimeUs;
        private long mFirstTimeUs = Long.MIN_VALUE;

        private AudioRecordingThread() {
            setPriority(Thread.MAX_PRIORITY);
            final int minBufferSize = AudioRecord.getMinBufferSize(
                    mConfig.samplingFrequency,
                    mConfig.audioFormatChannels(),
                    mConfig.encoding);
            // Make this bigger so we don't skip frames. 25: Stereo: 51200. Mono: 25600
            // 25 is quite big already. Tried to make it bigger to solve the read() delay
            // but it just makes things worse (ruins MONO as well).
            // Tried to make it smaller and things change as well.
            int bufferSize = mConfig.frameSize() * mConfig.audioRecordBufferFrames();
            while (bufferSize < minBufferSize) {
                bufferSize += mConfig.frameSize(); // Unlikely.
            }
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                    mConfig.samplingFrequency,
                    mConfig.audioFormatChannels(),
                    mConfig.encoding,
                    bufferSize);
        }

        @Override
        public void run() {
            mAudioRecord.startRecording();
            while (!mRequestStop) {
                if (!hasReachedMaxLength()) {
                    read(false);
                } else {
                    // We have reached the max length, so stop reading.
                    // However, do not get out of the loop - the controller
                    // will call stop() on us soon. It's not our responsibility
                    // to stop ourselves.
                    //noinspection UnnecessaryContinue
                    continue;
                }
            }
            LOG.w("Stop was requested. We're out of the loop. Will post an endOfStream.");
            // Last input with 0 length. This will signal the endOfStream.
            // Can't use drain(true); it is only available when writing to the codec InputSurface.
            boolean didReadEos = false;
            while (!didReadEos) {
                didReadEos = read(true);
            }
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        /**
         * Returns true if we found a buffer and could proceed, false if we found no buffer
         * so the operation should be performed again by the caller.
         * @param endOfStream true if last read
         * @return true if proceeded
         */
        private boolean read(boolean endOfStream) {
            mCurrentBuffer = mByteBufferPool.get();
            if (mCurrentBuffer == null) {
                // This can happen and it means that encoding is slow with respect to recording.
                // One might be tempted to fix precisely the next frame presentation time when
                // this happens, but this is not needed because the current increaseTime()
                // algorithm will consider delays when they get large.
                // Sleeping before returning is a good way of balancing the two operations.
                // However, if endOfStream, we CAN'T lose this frame!
                if (endOfStream) {
                    LOG.v("read thread - eos: true - No buffer, retrying.");
                } else {
                    LOG.w("read thread - eos: false - Skipping audio frame,",
                            "encoding is too slow.");
                    skipFrames(6); // sleep a bit
                }
                return false;
            } else {
                mCurrentBuffer.clear();
                // When stereo, we read twice the data here and AudioRecord will fill the buffer
                // with left and right bytes. https://stackoverflow.com/q/20594750/4288782
                if (PERFORMANCE_DEBUG) {
                    long before = System.nanoTime();
                    mCurrentReadBytes = mAudioRecord.read(mCurrentBuffer, mConfig.frameSize());
                    long after = System.nanoTime();
                    float delayMillis = (after - before) / 1000000F;
                    float durationMillis = AudioTimestamp.bytesToMillis(mCurrentReadBytes,
                            mConfig.byteRate());
                    LOG.v("read thread - reading took:", delayMillis,
                            "should be:", durationMillis,
                            "delay:", delayMillis - durationMillis);
                } else {
                    mCurrentReadBytes = mAudioRecord.read(mCurrentBuffer, mConfig.frameSize());
                }
                LOG.v("read thread - eos:", endOfStream, "- Read new audio frame. Bytes:",
                        mCurrentReadBytes);
                if (mCurrentReadBytes > 0) { // Good read: increase PTS.
                    increaseTime(mCurrentReadBytes, endOfStream);
                    LOG.v("read thread - eos:", endOfStream, "- mLastTimeUs:", mLastTimeUs);
                    mCurrentBuffer.limit(mCurrentReadBytes);
                    enqueue(mCurrentBuffer, mLastTimeUs, endOfStream);
                } else if (mCurrentReadBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                    LOG.e("read thread - eos:", endOfStream,
                            "- Got AudioRecord.ERROR_INVALID_OPERATION");
                } else if (mCurrentReadBytes == AudioRecord.ERROR_BAD_VALUE) {
                    LOG.e("read thread - eos:", endOfStream,
                            "- Got AudioRecord.ERROR_BAD_VALUE");
                }
                return true;
            }
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
            if (!hasReachedMaxLength()) {
                boolean didReachMaxLength = (mLastTimeUs - mFirstTimeUs) > getMaxLengthUs();
                if (didReachMaxLength && !endOfStream) {
                    LOG.w("read thread - this frame reached the maxLength! deltaUs:",
                            mLastTimeUs - mFirstTimeUs);
                    notifyMaxLengthReached();
                }
            }

            // Maybe add noise.
            maybeAddNoise();
        }

        private void enqueue(@NonNull ByteBuffer byteBuffer,
                             long timestamp,
                             boolean isEndOfStream) {
            if (PERFORMANCE_DEBUG) {
                mDebugSendStartMap.put(timestamp, System.nanoTime() / 1000000);
            }
            int readBytes = byteBuffer.remaining();
            InputBuffer inputBuffer = mInputBufferPool.get();
            //noinspection ConstantConditions
            inputBuffer.source = byteBuffer;
            inputBuffer.timestamp = timestamp;
            inputBuffer.length = readBytes;
            inputBuffer.isEndOfStream = isEndOfStream;
            mInputBufferQueue.add(inputBuffer);
        }

        /**
         * If our {@link AudioTimestamp} detected huge gap, and the performance flag is enabled,
         * we can add noise to fill them.
         *
         * Even if we always pass the correct timestamps, if there are big gaps between the frames,
         * the encoder implementation might shrink all timestamps to have a continuous audio.
         * This results in a video that is fast-forwarded.
         *
         * Adding noise does not solve the gaps issue, we'll still have distorted audio, but
         * at least we get a video that has the correct playback speed.
         *
         * NOTE: this MUST be fast!
         * If this operation is slow, we make the {@link AudioRecordingThread} busy, so we'll
         * read the next frame with a delay, so we'll have even more gaps at the next call
         * and spend even more time here. The result might be recording no audio at all - just
         * random noise.
         * This is the reason why we have a {@link #PERFORMANCE_MAX_GAPS} number.
         */
        private void maybeAddNoise() {
            if (!PERFORMANCE_FILL_GAPS) return;
            int gaps = mTimestamp.getGapCount(mConfig.frameSize());
            if (gaps <= 0) return;

            long gapStart = mTimestamp.getGapStartUs(mLastTimeUs);
            long frameUs = AudioTimestamp.bytesToUs(mConfig.frameSize(), mConfig.byteRate());
            LOG.w("read thread - GAPS: trying to add", gaps,
                    "noise buffers. PERFORMANCE_MAX_GAPS:", PERFORMANCE_MAX_GAPS);
            for (int i = 0; i < Math.min(gaps, PERFORMANCE_MAX_GAPS); i++) {
                ByteBuffer noiseBuffer = mByteBufferPool.get();
                if (noiseBuffer == null) {
                    LOG.e("read thread - GAPS: aborting because we have no free buffer.");
                    break;
                }
                noiseBuffer.clear();
                mAudioNoise.fill(noiseBuffer);
                noiseBuffer.rewind();
                enqueue(noiseBuffer, gapStart, false);
                gapStart += frameUs;
            }
        }
    }

    /**
     * A thread encoding the microphone data using the media encoder APIs.
     * Communicates with {@link AudioRecordingThread} using {@link #mInputBufferQueue}.
     *
     * We want to do this operation on a different thread than the recording one (to avoid
     * losing frames while we're working here), and different than the {@link MediaEncoder}
     * own thread (we want that to be reactive - stop() must become onStop() soon).
     */
    private class AudioEncodingThread extends Thread {
        private AudioEncodingThread() {
            // Not sure about this... This thread can do VERY time consuming operations,
            // and slowing down the preview/camera threads can break them e.g. hit internal
            // timeouts for camera requests to be consumed.
            // setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            encoding: while (true) {
                if (mInputBufferQueue.isEmpty()) {
                    skipFrames(3);
                } else {
                    LOG.v("encoding thread - performing", mInputBufferQueue.size(),
                            "pending operations.");
                    InputBuffer inputBuffer;
                    while ((inputBuffer = mInputBufferQueue.peek()) != null) {

                        // Performance logging
                        if (PERFORMANCE_DEBUG) {
                            long sendEnd = System.nanoTime() / 1000000;
                            Long sendStart = mDebugSendStartMap.remove(inputBuffer.timestamp);
                            //noinspection StatementWithEmptyBody
                            if (sendStart != null) {
                                mDebugSendAvgDelay = ((mDebugSendAvgDelay * mDebugSendCount)
                                        + (sendEnd - sendStart)) / (++mDebugSendCount);
                                LOG.v("send delay millis:", sendEnd - sendStart,
                                        "average:", mDebugSendAvgDelay);
                            } else {
                                // This input buffer was already processed
                                // (but tryAcquire failed for now).
                            }
                        }

                        // Actual work
                        if (inputBuffer.isEndOfStream) {
                            acquireInputBuffer(inputBuffer);
                            encode(inputBuffer);
                            break encoding;
                        } else if (tryAcquireInputBuffer(inputBuffer)) {
                            encode(inputBuffer);
                        } else {
                            skipFrames(3);
                        }
                    }
                }
            }
            // We got an end of stream.
            mInputBufferPool.clear();
            if (PERFORMANCE_DEBUG) {
                // After latest changes, the count here is not so different between MONO and STEREO.
                // We get about 400 frames in both cases (430 for MONO, but doesn't seem like
                // a big issue).
                LOG.e("EXECUTE DELAY MILLIS:", mDebugExecuteAvgDelay,
                        "COUNT:", mDebugExecuteCount);
                LOG.e("SEND DELAY MILLIS:", mDebugSendAvgDelay,
                        "COUNT:", mDebugSendCount);
            }
        }

        private void encode(@NonNull InputBuffer buffer) {
            long executeStart = System.nanoTime() / 1000000;

            LOG.v("encoding thread - performing pending operation for timestamp:",
                    buffer.timestamp, "- encoding.");
            // NOTE: this copy is prob. the worst part here for performance
            buffer.data.put(buffer.source);
            mByteBufferPool.recycle(buffer.source);
            mInputBufferQueue.remove(buffer);
            encodeInputBuffer(buffer);
            boolean eos = buffer.isEndOfStream;
            mInputBufferPool.recycle(buffer);
            LOG.v("encoding thread - performing pending operation for timestamp:",
                    buffer.timestamp, "- draining.");
            // NOTE: can consider calling this drainOutput on yet another thread, which would let us
            // use an even smaller BUFFER_POOL_MAX_SIZE without losing audio frames. But this way
            // we can accumulate delay on this new thread without noticing (no pool getting empty).
            drainOutput(eos);

            if (PERFORMANCE_DEBUG) {
                long executeEnd = System.nanoTime() / 1000000;
                mDebugExecuteAvgDelay = ((mDebugExecuteAvgDelay * mDebugExecuteCount)
                        + (executeEnd - executeStart)) / (++mDebugExecuteCount);
                LOG.v("execute delay millis:", executeEnd - executeStart,
                        "average:", mDebugExecuteAvgDelay);
            }
        }
    }
}
