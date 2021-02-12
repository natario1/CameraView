package com.otaliastudios.cameraview;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * Receives callbacks about a bitmap decoding operation.
 */
public interface BitmapCallback {

    /**
     * Notifies that the bitmap was successfully decoded.
     * This is run on the UI thread.
     * Returns a null object if a {@link OutOfMemoryError} was encountered.
     *
     * @param bitmap decoded bitmap, or null
     */
    @UiThread
    void onBitmapReady(@Nullable Bitmap bitmap);
}
