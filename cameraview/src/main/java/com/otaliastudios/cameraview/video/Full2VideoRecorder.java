package com.otaliastudios.cameraview.video;

import android.annotation.SuppressLint;
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

    private final String mCameraId;
    private Surface mInputSurface;

    public Full2VideoRecorder(@NonNull Camera2Engine engine,
                              @NonNull String cameraId) {
        super(engine);
        mCameraId = cameraId;
    }

    @SuppressLint("NewApi")
    @Override
    protected boolean onPrepareMediaRecorder(@NonNull VideoResult.Stub stub, @NonNull MediaRecorder mediaRecorder) {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        Size size = stub.rotation % 180 != 0 ? stub.size.flip() : stub.size;
        mProfile = CamcorderProfiles.get(mCameraId, size);
        // This was an option: get the surface from outside this class, using MediaCodec.createPersistentInputSurface()
        // But it doesn't really help since the Camera2 engine refuses a surface that has not been configured,
        // so even with that trick we would have to attach the surface to this recorder before creating the CameraSession.
        // mediaRecorder.setInputSurface(mInputSurface);
        return super.onPrepareMediaRecorder(stub, mediaRecorder);
    }

    /**
     * This method should be called just once.
     *
     * @param stub the video stub
     * @return a surface
     * @throws PrepareException if prepare went wrong
     */
    @NonNull
    public Surface createInputSurface(@NonNull VideoResult.Stub stub) throws PrepareException {
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

    public class PrepareException extends Exception {
        private PrepareException(Throwable cause) {
            super(cause);
        }
    }

}
