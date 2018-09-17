package com.otaliastudios.cameraview;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
class SnapshotPictureRecorder extends PictureRecorder {

    private static final String TAG = SnapshotPictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera1 mController;
    private Camera mCamera;
    private AspectRatio mOutputRatio;
    private Size mSensorPreviewSize;
    private int mFormat;

    SnapshotPictureRecorder(PictureResult stub, Camera1 controller, Camera camera, AspectRatio viewRatio) {
        super(stub, controller);
        mController = controller;
        mCamera = camera;
        mOutputRatio = viewRatio;
        mFormat = mController.mPreviewFormat;
        mSensorPreviewSize = mController.mPreviewSize;
    }

    @Override
    void take() {
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] yuv, Camera camera) {
                dispatchOnShutter(false);

                // Got to rotate the preview frame, since byte[] data here does not include
                // EXIF tags automatically set by camera. So either we add EXIF, or we rotate.
                // Adding EXIF to a byte array, unfortunately, is hard.
                final int sensorToOutput = mResult.rotation;
                final Size outputSize = mResult.size;
                WorkerHandler.run(new Runnable() {
                    @Override
                    public void run() {
                        // Rotate the picture, because no one will write EXIF data,
                        // then crop if needed. In both cases, transform yuv to jpeg.
                        byte[] data = RotationHelper.rotate(yuv, mSensorPreviewSize, sensorToOutput);
                        YuvImage yuv = new YuvImage(data, mFormat, outputSize.getWidth(), outputSize.getHeight(), null);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Rect outputRect = CropHelper.computeCrop(outputSize, mOutputRatio);
                        yuv.compressToJpeg(outputRect, 90, stream);
                        data = stream.toByteArray();

                        mResult.jpeg = data;
                        mResult.size = new Size(outputRect.width(), outputRect.height());
                        mResult.rotation = 0;
                        dispatchResult();
                    }
                });

                // It seems that the buffers are already cleared here, so we need to allocate again.
                camera.setPreviewCallbackWithBuffer(null); // Release anything left
                camera.setPreviewCallbackWithBuffer(mController); // Add ourselves
                mController.mFrameManager.allocate(ImageFormat.getBitsPerPixel(mFormat), mController.mPreviewSize);
            }
        });
    }

    @Override
    protected void dispatchResult() {
        mController = null;
        mCamera = null;
        mOutputRatio = null;
        mFormat = 0;
        mSensorPreviewSize = null;
        super.dispatchResult();
    }
}
