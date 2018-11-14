package com.otaliastudios.cameraview;

/**
 * This exception is thrown when a third party application is currently using the camera of the device and thus the current app cannot launch the camera view
 * <p></p>
 * This exception is made to extend {@link CameraException} signifying that it is an expected exception and should not crash the app.
 *<p></p>
 * The implementation of the {@link CameraListener} supplied should check if the {@link CameraException} in the implementation of the
 * {@link CameraListener#onCameraError(CameraException)} method is an instance of {@link CameraLaunchException}. If it is an instance of
 * {@link CameraLaunchException}, an appropriate message should be displayed to the user indicating that a third party app is currently
 * using the camera and that needs to be turned off before the camera can be launched from the current app
 *
 */
public class CameraLaunchException extends CameraException {

    public CameraLaunchException(Throwable cause) {
        super(cause);
    }
}
