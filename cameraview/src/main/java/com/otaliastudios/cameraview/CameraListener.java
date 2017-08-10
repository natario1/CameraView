package com.otaliastudios.cameraview;

import android.graphics.PointF;

import java.io.File;

public abstract class CameraListener {


    /**
     * Notifies that the camera was opened.
     * The {@link CameraOptions} object collects all supported options by the current camera.
     *
     * @param options camera supported options
     */
    public void onCameraOpened(CameraOptions options) {

    }


    /**
     * Notifies that the camera session was closed.
     */
    public void onCameraClosed() {

    }


    /**
     * Notifies that a picture previously captured with {@link CameraView#capturePicture()}
     * or {@link CameraView#captureSnapshot()} is ready to be shown or saved.
     *
     * If planning to get a bitmap, you can use {@link CameraUtils#decodeBitmap(byte[], CameraUtils.BitmapCallback)}
     * to decode the byte array taking care about orientation.
     *
     * @param jpeg captured picture
     */
    public void onPictureTaken(byte[] jpeg) {

    }


    /**
     * Notifies that a video capture has just ended. The file parameter is the one that
     * was passed to {@link CameraView#startCapturingVideo(File)}, if any.
     * If not, the camera fallsback to:
     * <code>
     *     new File(getContext().getExternalFilesDir(null), "video.mp4");
     * </code>
     *
     * @param video file hosting the mp4 video
     */
    public void onVideoTaken(File video) {

    }


    /**
     * Notifies that user interacted with the screen and started focus with a gesture,
     * and the autofocus is trying to focus around that area.
     * This can be used to draw things on screen.
     *
     * @param point coordinates with respect to CameraView.getWidth() and CameraView.getHeight()
     */
    public void onFocusStart(PointF point) {

    }


    /**
     * Notifies that a gesture focus event just ended, and the camera converged
     * to a new focus (and possibly exposure and white balance).
     * This might succeed or not.
     *
     * @param successful whether camera succeeded
     * @param point coordinates with respect to CameraView.getWidth() and CameraView.getHeight()
     */
    public void onFocusEnd(boolean successful, PointF point) {

    }


    /**
     * Noitifies that a finger gesture just caused the camera zoom
     * to be changed. This can be used to draw, for example, a seek bar.
     *
     * @param newValue the new zoom value
     * @param bounds min and max bounds for newValue (fixed to 0 ... 1)
     * @param fingers finger positions that caused the event
     */
    public void onZoomChanged(float newValue, float[] bounds, PointF[] fingers) {

    }


    /**
     * Noitifies that a finger gesture just caused the camera exposure correction
     * to be changed. This can be used to draw, for example, a seek bar.
     *
     * @param newValue the new correction value
     * @param bounds min and max bounds for newValue, as returned by {@link CameraOptions}
     * @param fingers finger positions that caused the event
     */
    public void onExposureCorrectionChanged(float newValue, float[] bounds, PointF[] fingers) {

    }

}