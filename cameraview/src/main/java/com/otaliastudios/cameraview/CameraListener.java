package com.otaliastudios.cameraview;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * The base class for receiving updates from a {@link CameraView} instance.
 * You can add and remove listeners using {@link CameraView#addCameraListener(CameraListener)}
 * and {@link CameraView#removeCameraListener(CameraListener)}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class CameraListener {


    /**
     * Notifies that the camera was opened.
     * The {@link CameraOptions} object collects all supported options by the current camera.
     *
     * @param options camera supported options
     */
    @UiThread
    public void onCameraOpened(@NonNull CameraOptions options) { }


    /**
     * Notifies that the camera session was closed.
     */
    @UiThread
    public void onCameraClosed() { }


    /**
     * Notifies about an error during the camera setup or configuration.
     *
     * At this point you should inspect the {@link CameraException} reason using
     * {@link CameraException#getReason()} and see what should be done, if anything.
     * If the error is unrecoverable, this is the right moment to show an error dialog, for example.
     *
     * @param exception the error
     */
    @UiThread
    public void onCameraError(@NonNull CameraException exception) { }


    /**
     * Notifies that a picture previously captured with {@link CameraView#takePicture()}
     * or {@link CameraView#takePictureSnapshot()} is ready to be shown or saved to file.
     *
     * If planning to show a bitmap, you can use
     * {@link PictureResult#toBitmap(int, int, BitmapCallback)} to decode the byte array
     * taking care about orientation and threading.
     *
     * @param result captured picture
     */
    @UiThread
    public void onPictureTaken(@NonNull PictureResult result) { }


    /**
     * Notifies that a video capture has just ended.
     *
     * @param result the video result
     */
    @UiThread
    public void onVideoTaken(@NonNull VideoResult result) { }


    /**
     * Notifies that the device was tilted or the window offset changed.
     * The orientation passed is exactly the counter-clockwise rotation that a View should have,
     * in order to appear correctly oriented to the user, considering the way she is
     * holding the device, and the native activity orientation.
     *
     * This is meant to be used for aligning views (e.g. buttons) to the current camera viewport.
     *
     * @param orientation either 0, 90, 180 or 270
     */
    @UiThread
    public void onOrientationChanged(int orientation) { }


    /**
     * Notifies that user interacted with the screen and started metering with a gesture,
     * and touch metering routine is trying to focus around that area.
     * This callback can be used to draw things on screen.
     * Can also be triggered by {@link CameraView#startAutoFocus(float, float)}.
     *
     * @param point coordinates with respect to CameraView.getWidth() and CameraView.getHeight()
     */
    @UiThread
    public void onAutoFocusStart(@NonNull PointF point) { }


    /**
     * Notifies that a touch metering event just ended, and the camera converged
     * to a new focus, exposure and possibly white balance.
     * This might succeed or not.
     * Can also be triggered by {@link CameraView#startAutoFocus(float, float)}.
     *
     * @param successful whether metering succeeded
     * @param point coordinates with respect to CameraView.getWidth() and CameraView.getHeight()
     */
    @UiThread
    public void onAutoFocusEnd(boolean successful, @NonNull PointF point) { }


    /**
     * Notifies that a finger gesture just caused the camera zoom
     * to be changed. This can be used to draw, for example, a seek bar.
     *
     * @param newValue the new zoom value
     * @param bounds min and max bounds for newValue (fixed to 0 ... 1)
     * @param fingers finger positions that caused the event, null if not caused by touch
     */
    @UiThread
    public void onZoomChanged(float newValue,
                              @NonNull float[] bounds,
                              @Nullable PointF[] fingers) { }


    /**
     * Noitifies that a finger gesture just caused the camera exposure correction
     * to be changed. This can be used to draw, for example, a seek bar.
     *
     * @param newValue the new correction value
     * @param bounds min and max bounds for newValue, as returned by {@link CameraOptions}
     * @param fingers finger positions that caused the event, null if not caused by touch
     */
    @UiThread
    public void onExposureCorrectionChanged(float newValue,
                                            @NonNull float[] bounds,
                                            @Nullable PointF[] fingers) { }


    /**
     * Notifies that the actual video recording has started.
     * This is the time when actual frames recording starts.
     *
     * This can be used to show some UI indicator for video recording or counting time.
     *
     * @see #onVideoRecordingEnd()
     */
    @UiThread
    public void onVideoRecordingStart() {

    }

    /**
     * Notifies that the actual video recording has ended.
     * At this point recording has ended, though the file might still be processed.
     * The {@link #onVideoTaken(VideoResult)} callback will be called soon.
     *
     * This can be used to remove UI indicators for video recording.
     *
     * @see #onVideoRecordingStart()
     */
    @UiThread
    public void onVideoRecordingEnd() {

    }

    /**
     * Notifies that the picture capture has started. Can be used to update the UI for visual
     * confirmation or sound effects.
     */
    @UiThread
    public void onPictureShutter() {

    }
    
}
