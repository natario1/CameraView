package com.otaliastudios.cameraview;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

/**
 * Receives callbacks about a bitmap decoding operation.
 */
public interface BitmapCallback {

    /**
     * Notifies that the bitmap was succesfully decoded.
     * This is run on the UI thread.
     * Returns a null object if a {@link OutOfMemoryError} was encountered.
     *
     * @param bitmap decoded bitmap, or null
     */
    @UiThread
    void onBitmapReady(@Nullable Bitmap bitmap);
}
