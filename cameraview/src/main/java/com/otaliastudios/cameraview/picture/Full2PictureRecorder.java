package com.otaliastudios.cameraview.picture;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.internal.utils.ExifHelper;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Full2PictureRecorder extends PictureRecorder
        implements ImageReader.OnImageAvailableListener {

    private static final String TAG = Full2PictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private final ActionHolder mHolder;
    private final Action mAction;
    private final ImageReader mPictureReader;
    private final CaptureRequest.Builder mPictureBuilder;

    public Full2PictureRecorder(@NonNull PictureResult.Stub stub,
                                @NonNull Camera2Engine engine,
                                @NonNull CaptureRequest.Builder pictureBuilder,
                                @NonNull ImageReader pictureReader) {
        super(stub, engine);
        mHolder = engine;
        mPictureBuilder = pictureBuilder;
        mPictureReader = pictureReader;
        mPictureReader.setOnImageAvailableListener(this, WorkerHandler.get().getHandler());
        mAction = new BaseAction() {

            @Override
            protected void onStart(@NonNull ActionHolder holder) {
                super.onStart(holder);
                mPictureBuilder.addTarget(mPictureReader.getSurface());
                mPictureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mResult.rotation);
                mPictureBuilder.setTag(CameraDevice.TEMPLATE_STILL_CAPTURE);
                try {
                    holder.applyBuilder(this, mPictureBuilder);
                } catch (CameraAccessException e) {
                    mResult = null;
                    mError = e;
                    dispatchResult();
                }
            }

            @Override
            public void onCaptureStarted(@NonNull ActionHolder holder,
                                         @NonNull CaptureRequest request) {
                super.onCaptureStarted(holder, request);
                if (request.getTag() == (Integer) CameraDevice.TEMPLATE_STILL_CAPTURE) {
                    LOG.i("onCaptureStarted:", "Dispatching picture shutter.");
                    dispatchOnShutter(false);
                    setState(STATE_COMPLETED);
                }
            }
        };
    }

    @Override
    public void take() {
        mAction.start(mHolder);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        LOG.i("onImageAvailable started.");
        // Read the JPEG.
        Image image = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            mResult.data = bytes;
        } catch (Exception e) {
            mResult = null;
            mError = e;
            dispatchResult();
            return;
        } finally {
            if (image != null) image.close();
        }

        // Just like Camera1, unfortunately, the camera might rotate the image
        // and put EXIF=0 instead of respecting our EXIF and leave the image unaltered.
        mResult.format = PictureResult.FORMAT_JPEG;
        mResult.rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(mResult.data));
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            mResult.rotation = ExifHelper.readExifOrientation(exifOrientation);
        } catch (IOException ignore) { }

        // Leave.
        LOG.i("onImageAvailable ended.");
        dispatchResult();
    }
}
