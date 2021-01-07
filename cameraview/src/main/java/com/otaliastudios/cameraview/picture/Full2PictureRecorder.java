package com.otaliastudios.cameraview.picture;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Log;

import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.internal.ExifHelper;
import com.otaliastudios.cameraview.internal.WorkerHandler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Full2PictureRecorder extends FullPictureRecorder
        implements ImageReader.OnImageAvailableListener {

    private final ActionHolder mHolder;
    private final Action mAction;
    private final ImageReader mPictureReader;
    private final CaptureRequest.Builder mPictureBuilder;

    private DngCreator mDngCreator;

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
                if (mResult.format == PictureFormat.JPEG) {
                    mPictureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mResult.rotation);
                }
                mPictureBuilder.setTag(CameraDevice.TEMPLATE_STILL_CAPTURE);
                try {
                    holder.applyBuilder(this, mPictureBuilder);
                } catch (CameraAccessException e) {
                    mResult = null;
                    mError = e;
                    dispatchResult();
                    setState(STATE_COMPLETED);
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

            @Override
            public void onCaptureCompleted(@NonNull ActionHolder holder,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                try {
                    super.onCaptureCompleted(holder, request, result);
                } catch (Exception e) {
                    mError = e;
                    dispatchResult();
                }

                if (mResult.format == PictureFormat.DNG) {
                    mDngCreator = new DngCreator(holder.getCharacteristics(this), result);
                    mDngCreator.setOrientation(ExifHelper.getExifOrientation(mResult.rotation));
                    if (mResult.location != null) {
                        mDngCreator.setLocation(mResult.location);
                    }
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
        Image image = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            image = reader.acquireNextImage();
            switch (mResult.format) {
                case JPEG: readJpegImage(image); break;
                case DNG: readRawImage(image); break;
                default: throw new IllegalStateException("Unknown format: " + mResult.format);
            }
        } catch (Exception e) {
            mResult = null;
            mError = e;
            dispatchResult();
            return;
        } finally {
            if (image != null) {
                image.close();
            }
        }

        // Leave.
        LOG.i("onImageAvailable ended.");
        dispatchResult();
    }

    private void readJpegImage(@NonNull Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        mResult.data = bytes;

        // Just like Camera1, unfortunately, the camera might rotate the image
        // and put EXIF=0 instead of respecting our EXIF and leave the image unaltered.
        mResult.rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(mResult.data));
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            mResult.rotation = ExifHelper.getOrientation(exifOrientation);
        } catch (IOException ignore) {
            // Should not happen
        }
    }

    private void readRawImage(@NonNull Image image) {
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        BufferedOutputStream stream = new BufferedOutputStream(array);
        try {
            mDngCreator.writeImage(stream, image);
            stream.flush();
            mResult.data = array.toByteArray();
        } catch (IOException e) {
            mDngCreator.close();
            try { stream.close(); } catch (IOException ignore) {}
            throw new RuntimeException(e);
        }
    }
}
