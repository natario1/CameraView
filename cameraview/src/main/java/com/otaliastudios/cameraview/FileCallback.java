package com.otaliastudios.cameraview;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * Receives callbacks about a file saving operation.
 */
public interface FileCallback {

    /**
     * Notifies that the data was succesfully written to file.
     * This is run on the UI thread.
     * Returns a null object if an exception was encountered, for example
     * if you don't have permissions to write to file.
     *
     * @param file the written file, or null
     */
    @UiThread
    void onFileReady(@Nullable File file);
}
