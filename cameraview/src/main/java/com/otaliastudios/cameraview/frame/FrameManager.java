package com.otaliastudios.cameraview.frame;


import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the allocation of buffers and Frame objects.
 * We are interested in both recycling byte[] buffers so they are not allocated for each
 * preview frame, and in recycling Frame instances (so we don't instantiate a lot).
 *
 * For this, we keep a mPoolSize integer that defines the size of instances to keep.
 * Whether this does make sense, it depends on how slow the frame processors are.
 * If they are very slow, it is possible that some frames will be skipped.
 *
 * - byte[] buffer pool:
 *     this is not kept here, because Camera1 internals already have one that we can't control, but
 *     it should be OK. The only thing we do is allocate mPoolSize buffers when requested.
 * - Frame pool:
 *     We keep a list of mPoolSize recycled instances, to be reused when a new buffer is available.
 */
public class FrameManager {

    /**
     * Receives callbacks on buffer availability
     * (when a Frame is released, we reuse its buffer).
     */
    public interface BufferCallback {
        void onBufferAvailable(@NonNull byte[] buffer);
    }

    private int mPoolSize;
    private int mBufferSize;
    private BufferCallback mCallback;
    private LinkedBlockingQueue<Frame> mQueue;

    /**
     * Construct a new frame manager.
     * The construction must be followed by an {@link #allocateBuffers(int, Size)} call
     * as soon as the parameters are known.
     *
     * @param poolSize the size of the backing pool.
     * @param callback a callback
     */
    public FrameManager(int poolSize, @Nullable BufferCallback callback) {
        mPoolSize = poolSize;
        mCallback = callback;
        mQueue = new LinkedBlockingQueue<>(mPoolSize);
        mBufferSize = -1;
    }

    /**
     * Allocates a {@link #mPoolSize} number of buffers. Should be called once
     * the preview size and the bitsPerPixel value are known.
     *
     * This method can be called again after {@link #release()} has been called.
     *
     * @param bitsPerPixel bits per pixel, depends on image format
     * @param previewSize the preview size
     * @return the buffer size
     */
    public int allocateBuffers(int bitsPerPixel, @NonNull Size previewSize) {
        // TODO throw if called twice without release?
        mBufferSize = getBufferSize(bitsPerPixel, previewSize);
        for (int i = 0; i < mPoolSize; i++) {
            mCallback.onBufferAvailable(new byte[mBufferSize]);
        }
        return mBufferSize;
    }

    /**
     * Releases all frames controlled by this manager and
     * clears the pool.
     */
    public void release() {
        for (Frame frame : mQueue) {
            frame.releaseManager();
            frame.release();
        }
        mQueue.clear();
        mBufferSize = -1;
    }

    void onFrameReleased(Frame frame) {
        byte[] buffer = frame.getData();
        boolean willRecycle = mQueue.offer(frame);
        if (!willRecycle) {
            // If frame queue is full, let's drop everything.
            frame.releaseManager();
        } else {
            // If frame will be recycled, let's recycle the buffer as well.
            int currSize = buffer.length;
            int reqSize = mBufferSize;
            if (currSize == reqSize && mCallback != null) {
                mCallback.onBufferAvailable(buffer);
            }
        }

    }

    /**
     * Returns a new Frame for the given data. This must be called
     * - after {@link #allocateBuffers(int, Size)}, which sets the buffer size
     * - after the byte buffer given by allocateBuffers() has been filled.
     *   If this is called X times in a row without releasing frames, it will allocate
     *   X frames and that's bad. Callers must wait for the preview buffer to be available.
     *
     * In Camera1, this is always respected thanks to its internals.
     *
     * @return a new frame
     */
    public Frame getFrame(@NonNull byte[] data, long time, int rotation, @NonNull Size previewSize, int previewFormat) {
        Frame frame = mQueue.poll();
        if (frame == null) frame = new Frame(this);
        frame.set(data, time, rotation, previewSize, previewFormat);
        return frame;
    }

    private int getBufferSize(int bitsPerPixel, @NonNull Size previewSize) {
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        return (int) Math.ceil(sizeInBits / 8.0d);
    }
}
