package com.otaliastudios.cameraview.video.encoding;

import androidx.annotation.NonNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Random;

/**
 * An AudioNoise instance offers buffers of noise that we can use when recording
 * some samples failed for some reason.
 *
 * Since we can't create noise anytime it's needed - that would be expensive and
 * slow down the recording thread - we create a big noise buffer at start time.
 *
 * We'd like to work with {@link ShortBuffer}s, but this requires converting the
 * input buffer to ShortBuffer each time, and this can be expensive.
 */
class AudioNoise {

    private final static int FRAMES = 1; // After testing, it looks like this is the best setup
    private final static Random RANDOM = new Random();

    private final ByteBuffer mNoiseBuffer;

    AudioNoise(@NonNull AudioConfig config) {
        //noinspection ConstantConditions
        if (config.sampleSizePerChannel != 2) {
            throw new IllegalArgumentException("AudioNoise expects 2bytes-1short samples.");
        }
        mNoiseBuffer = ByteBuffer
                .allocateDirect(config.frameSize() * FRAMES)
                .order(ByteOrder.nativeOrder());
        double i = 0;
        double frequency = config.frameSize() / 2D; // each X samples, the signal repeats
        double step = Math.PI / frequency; // the increase in radians
        double max = 10; // might choose this from 0 to Short.MAX_VALUE
        while (mNoiseBuffer.hasRemaining()) {
            short noise = (short) (Math.sin(++i * step) * max);
            mNoiseBuffer.put((byte) noise);
            mNoiseBuffer.put((byte) (noise >> 8));
        }
        mNoiseBuffer.rewind();
    }

    void fill(@NonNull ByteBuffer outBuffer) {
        mNoiseBuffer.clear();
        if (mNoiseBuffer.capacity() == outBuffer.remaining()) {
            mNoiseBuffer.position(0); // Happens if FRAMES = 1.
        } else {
            mNoiseBuffer.position(RANDOM.nextInt(mNoiseBuffer.capacity()
                    - outBuffer.remaining()));
        }
        mNoiseBuffer.limit(mNoiseBuffer.position() + outBuffer.remaining());
        outBuffer.put(mNoiseBuffer);
    }
}
