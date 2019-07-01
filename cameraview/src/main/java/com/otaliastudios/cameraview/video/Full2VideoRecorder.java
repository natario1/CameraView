package com.otaliastudios.cameraview.video;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.Surface;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.internal.utils.CamcorderProfiles;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A {@link VideoRecorder} that uses {@link MediaRecorder} APIs
 * for the Camera2 engine.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Full2VideoRecorder extends FullVideoRecorder {

    private static final String TAG = Full2VideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    // This actually didn't work as I expected, we're never using this. Should remove.
    @SuppressWarnings("PointlessBooleanExpression") // TODO
    public static final boolean SUPPORTS_PERSISTENT_SURFACE = false && Build.VERSION.SDK_INT >= 23;

    private final String mCameraId;
    private Surface mInputSurface;
    private final boolean mUseInputSurface;

    public Full2VideoRecorder(@NonNull Camera2Engine engine,
                              @NonNull String cameraId,
                              @Nullable Surface surface) {
        super(engine);
        mCameraId = cameraId;
        mUseInputSurface = surface != null && SUPPORTS_PERSISTENT_SURFACE;
        mInputSurface = mUseInputSurface ? surface : null;
    }

    @SuppressLint("NewApi")
    @Override
    protected boolean onPrepareMediaRecorder(@NonNull VideoResult.Stub stub, @NonNull MediaRecorder mediaRecorder) {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        Size size = stub.rotation % 180 != 0 ? stub.size.flip() : stub.size;
        mProfile = CamcorderProfiles.get(mCameraId, size);
        if (mUseInputSurface) {
            //noinspection ConstantConditions
            mediaRecorder.setInputSurface(mInputSurface);
        }
        return super.onPrepareMediaRecorder(stub, mediaRecorder);
    }

    /**
     * This method should be called just once.
     * @return a surface
     * @throws PrepareException if prepare went wrong
     */
    @NonNull
    public Surface createInputSurface(@NonNull VideoResult.Stub stub) throws PrepareException {
        if (mUseInputSurface) {
            throw new IllegalStateException("We are using the input surface (API23+), can't createInputSurface here.");
        }
        if (!prepareMediaRecorder(stub)) {
            throw new PrepareException(mError);
        }
        mInputSurface = mMediaRecorder.getSurface();
        return mInputSurface;
    }

    @Nullable
    public Surface getInputSurface() {
        return mInputSurface;
    }


    @SuppressWarnings("WeakerAccess")
    public class PrepareException extends Exception {
        private PrepareException(Throwable cause) {
            super(cause);
        }
    }

}
