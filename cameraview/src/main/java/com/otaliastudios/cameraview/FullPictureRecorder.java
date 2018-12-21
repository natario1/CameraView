package com.otaliastudios.cameraview;

import android.hardware.Camera;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
class FullPictureRecorder extends PictureRecorder {

    private static final String TAG = FullPictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera mCamera;

    FullPictureRecorder(@NonNull PictureResult stub, @Nullable PictureResultListener listener, @NonNull Camera camera) {
        super(stub, listener);
        mCamera = camera;

        // We set the rotation to the camera parameters, but we don't know if the result will be
        // already rotated with 0 exif, or original with non zero exif. we will have to read EXIF.
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(mResult.rotation);
        mCamera.setParameters(params);
    }

    // Camera2 constructor here...

    @Override
    void take() {
        mCamera.takePicture(
                new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        dispatchOnShutter(true);
                    }
                },
                null,
                null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {
                        int exifRotation;
                        try {
                            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(data));
                            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            exifRotation = CameraUtils.readExifOrientation(exifOrientation);
                        } catch (IOException e) {
                            exifRotation = 0;
                        }
                        mResult.format = PictureResult.FORMAT_JPEG;
                        mResult.data = data;
                        mResult.rotation = exifRotation;
                        camera.startPreview(); // This is needed, read somewhere in the docs.
                        dispatchResult();
                    }
                }
        );
    }

    @Override
    protected void dispatchResult() {
        mCamera = null;
        super.dispatchResult();
    }
}
