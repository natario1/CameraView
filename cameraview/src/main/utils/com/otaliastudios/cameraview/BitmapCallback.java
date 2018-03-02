package com.otaliastudios.cameraview;

import android.graphics.Bitmap;
import android.support.annotation.UiThread;

/**
 * Receives callbacks about a bitmap decoding operation.
 */
public interface BitmapCallback {

    /**
     * Notifies that the bitmap was succesfully decoded.
     * This is run on the UI thread.
     *
     * @param bitmap decoded bitmap
     */
    @UiThread
    void onBitmapReady(Bitmap bitmap);
}
