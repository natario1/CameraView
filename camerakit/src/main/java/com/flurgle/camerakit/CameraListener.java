package com.flurgle.camerakit;

import android.graphics.YuvImage;

import java.io.File;

public abstract class CameraListener {

    public void onCameraOpened() {

    }

    public void onCameraClosed() {

    }

    public void onPictureTaken(byte[] jpeg) {

    }

    public void onVideoTaken(File video) {

    }


    /**
     * Notifies that user tapped on screen at position given by x and y,
     * and the autofocus is trying to focus around that area.
     * This can be used to draw things on screen.
     *
     * @param x coordinate with respect to CameraView.getWidth()
     * @param y coordinate with respect to CameraView.getHeight()
     */
    public void onFocusStart(float x, float y) {

    }


    /**
     * Notifies that a tap-to-focus event just ended, and the camera converged
     * to a new focus (and possibly exposure and white balance).
     * This might succeed or not.
     *
     * @param successful whether camera succeeded
     * @param x coordinate with respect to CameraView.getWidth()
     * @param y coordinate with respect to CameraView.getHeight()
     */
    public void onFocusEnd(boolean successful, float x, float y) {

    }

}