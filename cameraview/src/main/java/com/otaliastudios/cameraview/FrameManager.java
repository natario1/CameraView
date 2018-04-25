package com.otaliastudios.cameraview;


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
class FrameManager {

    interface BufferCallback {
        void onBufferAvailable(byte[] buffer);
    }

    private int mPoolSize;
    private int mBufferSize;
    private BufferCallback mCallback;
    private LinkedBlockingQueue<Frame> mQueue;

    FrameManager(int poolSize, BufferCallback callback) {
        mPoolSize = poolSize;
        mCallback = callback;
        mQueue = new LinkedBlockingQueue<>(mPoolSize);
        mBufferSize = -1;
    }

    void release() {
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
            frame.releaseManager();
        }
        if (buffer != null && mCallback != null) {
            int currSize = buffer.length;
            int reqSize = mBufferSize;
            if (currSize == reqSize) {
                mCallback.onBufferAvailable(buffer);
            }
        }
    }

    /**
     * Returns a new Frame for the given data. This must be called
     * - after {@link #allocate(int, Size)}, which sets the buffer size
     * - after the byte buffer given by allocate() has been filled.
     *   If this is called X times in a row without releasing frames, it will allocate
     *   X frames and that's bad. Callers must wait for the preview buffer to be available.
     *
     * In Camera1, this is always respected thanks to its internals.
     *
     * @return a new frame
     */
    Frame getFrame(byte[] data, long time, int rotation, Size previewSize, int previewFormat) {
        Frame frame = mQueue.poll();
        if (frame == null) frame = new Frame(this);
        frame.set(data, time, rotation, previewSize, previewFormat);
        return frame;
    }

    int allocate(int bitsPerPixel, Size previewSize) {
        mBufferSize = getBufferSize(bitsPerPixel, previewSize);
        for (int i = 0; i < mPoolSize; i++) {
            mCallback.onBufferAvailable(new byte[mBufferSize]);
        }
        return mBufferSize;
    }

    private int getBufferSize(int bitsPerPixel, Size previewSize) {
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        return (int) Math.ceil(sizeInBits / 8.0d);
    }
}
