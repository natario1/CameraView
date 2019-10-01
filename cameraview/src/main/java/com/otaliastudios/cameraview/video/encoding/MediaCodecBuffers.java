package com.otaliastudios.cameraview.video.encoding;

import android.media.MediaCodec;
import android.os.Build;

import java.nio.ByteBuffer;

import androidx.annotation.RequiresApi;

/**
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer
 * methods, in order to prevent: http://stackoverflow.com/q/30646885
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaCodecBuffers {

    private final MediaCodec mMediaCodec;
    private final ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    MediaCodecBuffers(MediaCodec mediaCodec) {
        mMediaCodec = mediaCodec;

        if (Build.VERSION.SDK_INT < 21) {
            mInputBuffers = mediaCodec.getInputBuffers();
            mOutputBuffers = mediaCodec.getOutputBuffers();
        } else {
            mInputBuffers = mOutputBuffers = null;
        }
    }

    ByteBuffer getInputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getInputBuffer(index);
        }
        ByteBuffer buffer = mInputBuffers[index];
        buffer.clear();
        return buffer;
    }

    ByteBuffer getOutputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getOutputBuffer(index);
        }
        return mOutputBuffers[index];
    }

    void onOutputBuffersChanged() {
        if (Build.VERSION.SDK_INT < 21) {
            mOutputBuffers = mMediaCodec.getOutputBuffers();
        }
    }
}
