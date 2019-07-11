package com.otaliastudios.cameraview.frame;


import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the allocation of byte buffers and {@link Frame} objects.
 * We are interested in recycling both of them, especially byte[] buffers which can create a lot
 * of overhead.
 *
 * The FrameManager keeps a {@link #mPoolSize} integer that defines the number of instances to keep.
 * The pool size applies to both the {@link Frame} pool and the byte[] pool - it makes sense to use
 * the same number since they are consumed at the same time.
 *
 * Main methods are:
 * - {@link #setUp(int, Size)}: to set up with size and allocate buffers
 * - {@link #release()}: to release. After release, a manager can be setUp again.
 * - {@link #getFrame(byte[], long, int, Size, int)}: gets a new {@link Frame}.
 *
 * For both byte buffers and frames to get back to the FrameManager pool, all you have to do
 * is call {@link Frame#release()} when done.
 *
 * Other than this, the FrameManager can work in two modes, depending on whether a {@link BufferCallback}
 * is passed to the constructor. The modes changes the buffer behavior.
 *
 * 1. {@link #BUFFER_MODE_DISPATCH}: in this mode, as soon as we have a buffer, it is dispatched to
 *    the {@link BufferCallback}. The callback should then fill the buffer, and finally call
 *    {@link #getFrame(byte[], long, int, Size, int)} to receive a frame.
 *    This is used for Camera1.
 *
 * 2. {@link #BUFFER_MODE_ENQUEUE}: in this mode, the manager internally keeps a queue of byte buffers,
 *    instead of handing them to the callback. The users can ask for buffers through {@link #getBuffer()}.
 *    This buffer can be filled with data and used to get a frame {@link #getFrame(byte[], long, int, Size, int)},
 *    or, in case it was not filled, returned to the queue using {@link #onBufferUnused(byte[])}.
 *    This is used for Camera2.
 */
public class FrameManager {

    private static final String TAG = FrameManager.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    /**
     * Receives callbacks on buffer availability
     * (when a Frame is released, we reuse its buffer).
     */
    public interface BufferCallback {
        void onBufferAvailable(@NonNull byte[] buffer);
    }

    private final int mPoolSize;
    private int mBufferSize = -1;
    private LinkedBlockingQueue<Frame> mFrameQueue;
    private LinkedBlockingQueue<byte[]> mBufferQueue;
    private BufferCallback mBufferCallback;
    private final int mBufferMode;

    /**
     * In this mode, we have a {@link #mBufferCallback} and dispatch
     * new buffers to the callback.
     */
    private final static int BUFFER_MODE_DISPATCH = 0;

    /**
     * In this mode, we have a {@link #mBufferQueue} where we store
     * buffers and only dispatch when requested.
     */
    private final static int BUFFER_MODE_ENQUEUE = 1;

    /**
     * Construct a new frame manager.
     * The construction must be followed by an {@link #setUp(int, Size)} call
     * as soon as the parameters are known.
     *
     * @param poolSize the size of the backing pool.
     * @param callback a callback
     */
    public FrameManager(int poolSize, @Nullable BufferCallback callback) {
        mPoolSize = poolSize;
        mFrameQueue = new LinkedBlockingQueue<>(mPoolSize);
        if (callback != null) {
            mBufferCallback = callback;
            mBufferMode = BUFFER_MODE_DISPATCH;
        } else {
            mBufferQueue = new LinkedBlockingQueue<>(mPoolSize);
            mBufferMode = BUFFER_MODE_ENQUEUE;
        }
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
    public int setUp(int bitsPerPixel, @NonNull Size previewSize) {
        // TODO throw if called twice without release?
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        mBufferSize = (int) Math.ceil(sizeInBits / 8.0d);
        for (int i = 0; i < mPoolSize; i++) {
            if (mBufferMode == BUFFER_MODE_DISPATCH) {
                mBufferCallback.onBufferAvailable(new byte[mBufferSize]);
            } else {
                mBufferQueue.offer(new byte[mBufferSize]);
            }
        }
        return mBufferSize;
    }

    /**
     * Returns a new byte buffer than can be filled.
     * This can only be called in {@link #BUFFER_MODE_ENQUEUE} mode! Where the frame
     * manager also holds a queue of the byte buffers.
     *
     * If not null, the buffer returned by this method can be filled and used to get
     * a new frame through {@link #getFrame(byte[], long, int, Size, int)}.
     *
     * @return a buffer, or null
     */
    @Nullable
    public byte[] getBuffer() {
        if (mBufferMode != BUFFER_MODE_ENQUEUE) {
            throw new IllegalStateException("Can't call getBuffer() when not in BUFFER_MODE_ENQUEUE.");
        }
        return mBufferQueue.poll();
    }

    /**
     * Can be called if the buffer obtained by {@link #getBuffer()}
     * was not used to construct a frame, so it can be put back into the queue.
     * @param buffer a buffer
     */
    public void onBufferUnused(@NonNull byte[] buffer) {
        if (mBufferMode != BUFFER_MODE_ENQUEUE) {
            throw new IllegalStateException("Can't call onBufferUnused() when not in BUFFER_MODE_ENQUEUE.");
        }
        mBufferQueue.offer(buffer);
    }

    /**
     * Returns a new Frame for the given data. This must be called
     * - after {@link #setUp(int, Size)}, which sets the buffer size
     * - after the byte buffer given by setUp() has been filled.
     *   If this is called X times in a row without releasing frames, it will allocate
     *   X frames and that's bad. Callers must wait for the preview buffer to be available.
     *
     * In Camera1, this is always respected thanks to its internals.
     *
     * @param data data
     * @param time timestamp
     * @param rotation rotation
     * @param previewSize preview size
     * @param previewFormat format
     * @return a new frame
     */
    @NonNull
    public Frame getFrame(@NonNull byte[] data, long time, int rotation, @NonNull Size previewSize, int previewFormat) {
        Frame frame = mFrameQueue.poll();
        if (frame != null) {
            LOG.v("getFrame for time:", time, "RECYCLING.", "Data:", data != null);
        } else {
            LOG.v("getFrame for time:", time, "CREATING.", "Data:", data != null);
            frame = new Frame(this);
        }
        frame.set(data, time, rotation, previewSize, previewFormat);
        return frame;
    }

    /**
     * Releases all frames controlled by this manager and
     * clears the pool.
     * In BUFFER_MODE_ENQUEUE, releases also all the buffers.
     */
    public void release() {
        LOG.w("Releasing all frames!");
        for (Frame frame : mFrameQueue) {
            frame.releaseManager();
            frame.release();
        }
        mFrameQueue.clear();
        if (mBufferMode == BUFFER_MODE_ENQUEUE) {
            mBufferQueue.clear();
        }
        mBufferSize = -1;
    }

    /**
     * Called by child frames when they are released.
     * @param frame the released frame
     */
    void onFrameReleased(@NonNull Frame frame) {
        byte[] buffer = frame.getData();
        boolean willRecycle = mFrameQueue.offer(frame);
        if (!willRecycle) {
            // If frame queue is full, let's drop everything.
            frame.releaseManager();
        } else {
            // If frame will be recycled, let's recycle the buffer as well.
            int currSize = buffer.length;
            int reqSize = mBufferSize;
            if (currSize == reqSize) {
                if (mBufferMode == BUFFER_MODE_DISPATCH) {
                    mBufferCallback.onBufferAvailable(buffer);
                } else {
                    mBufferQueue.offer(buffer);
                }
            }
        }
    }
}
