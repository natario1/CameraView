package com.otaliastudios.cameraview.picture;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.internal.utils.ExifHelper;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Full2PictureRecorder extends PictureRecorder implements ImageReader.OnImageAvailableListener {

    private static final String TAG = Full2PictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int STATE_IDLE = 0;
    private static final int STATE_WAITING_CAPTURE = 1;
    private static final int STATE_WAITING_IMAGE = 2;

    private CameraCaptureSession mSession;
    private CameraCaptureSession.CaptureCallback mCallback;
    private ImageReader mPictureReader;
    private CaptureRequest.Builder mPictureBuilder;
    private boolean mStopPreviewBeforeCapture;
    private int mState = STATE_IDLE;
    private int mSequenceId;

    public Full2PictureRecorder(@NonNull PictureResult.Stub stub,
                                @Nullable PictureResultListener listener,
                                @NonNull CameraCaptureSession session,
                                @NonNull CameraCaptureSession.CaptureCallback callback,
                                @NonNull CaptureRequest.Builder pictureBuilder,
                                @NonNull ImageReader pictureReader,
                                boolean stopPreviewBeforeCapture) {
        super(stub, listener);
        mSession = session;
        mCallback = callback;
        mPictureBuilder = pictureBuilder;
        mStopPreviewBeforeCapture = stopPreviewBeforeCapture;
        mPictureReader = pictureReader;
        mPictureReader.setOnImageAvailableListener(this, WorkerHandler.get().getHandler());
    }

    @Override
    public void take() {
        try {
            mState = STATE_WAITING_CAPTURE;
            mPictureBuilder.addTarget(mPictureReader.getSurface());
            mPictureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mResult.rotation);
            if (mStopPreviewBeforeCapture) {
                // These two are present in official samples and are probably meant to speed things up?
                // But from my tests, they actually make everything slower. So this is disabled by default
                // with a boolean coming from the engine. Maybe in the future we can make this configurable
                // as some people might want to stop the preview while picture is being taken even if it
                // increases the latency.
                mSession.stopRepeating();
                mSession.abortCaptures();
            }
            mSequenceId = mSession.capture(mPictureBuilder.build(), mCallback, null);
        } catch (CameraAccessException e) {
            mResult = null;
            mError = e;
            dispatchResult();
        }
    }

    public void onCaptureStarted(@NonNull CaptureRequest request) {
        if (mState == STATE_WAITING_CAPTURE) {
            if (request.getTag() == mPictureBuilder.build().getTag()) {
                dispatchOnShutter(false);
            }
        }
    }

    public void onCaptureCompleted(@NonNull TotalCaptureResult result) {
        if (mState == STATE_WAITING_CAPTURE) {
            if (result.getSequenceId() == mSequenceId) {
                // This has no real use for now other than logging.
                LOG.i("onCaptureCompleted:", "Got result, moving to STATE_WAITING_IMAGE");
                mState = STATE_WAITING_IMAGE;
            }
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        LOG.i("onImageAvailable started.");
        mState = STATE_IDLE;

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
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            mResult.rotation = ExifHelper.readExifOrientation(exifOrientation);
        } catch (IOException ignore) { }

        // Leave.
        LOG.i("onImageAvailable ended.");
        dispatchResult();
    }


    @Override
    protected void dispatchResult() {
        mState = STATE_IDLE;
        super.dispatchResult();
    }
}
