package com.otaliastudios.cameraview;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class AudioMediaEncoder extends MediaEncoder {

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100; // 44.1[KHz] is only setting guaranteed to be available on all devices.
    public static final int SAMPLES_PER_FRAME = 1024; // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25; // AAC, frame/buffer/sec

    private final Object mLock = new Object();
    private boolean mRequestStop = false;
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

    @EncoderThread
    @Override
    void prepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        super.prepare(controller, maxLengthMillis);
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
    }

    @EncoderThread
    @Override
    void start() {
        mRequestStop = false;
        new AudioThread().start();
    }

    @EncoderThread
    @Override
    void notify(@NonNull String event, @Nullable Object data) { }

    @EncoderThread
    @Override
    void stop() {
        mRequestStop = true;
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    @Override
    void release() {
        super.release();
        mRequestStop = false;
    }

    class AudioThread extends Thread {

        private AudioRecord mAudioRecord;

        AudioThread() {
            final int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
            if (bufferSize < minBufferSize) {
                bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
            }
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }

        @Override
        public void run() {
            super.run();
            mAudioRecord.startRecording();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
            int readBytes;
            while (!mRequestStop) {
                buffer.clear();
                readBytes = mAudioRecord.read(buffer, SAMPLES_PER_FRAME);
                if (readBytes > 0) {
                    // set audio data to encoder
                    buffer.position(readBytes);
                    buffer.flip();
                    encode(buffer, readBytes, getPresentationTime());
                    drain(false);
                }
            }
            // This will signal the endOfStream.
            // Can't use drain(true); it is only available when writing to the codec InputSurface.
            encode(null, 0, getPresentationTime());
            drain(false);
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    @Override
    int getBitRate() {
        return mConfig.bitRate;
    }
}
