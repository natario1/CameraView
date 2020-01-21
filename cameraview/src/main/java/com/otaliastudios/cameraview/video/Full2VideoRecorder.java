package com.otaliastudios.cameraview.video;

import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.Surface;

import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.engine.action.CompletionCallback;
import com.otaliastudios.cameraview.internal.CamcorderProfiles;
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

    private ActionHolder mHolder;
    private final String mCameraId;
    private Surface mInputSurface;

    public Full2VideoRecorder(@NonNull Camera2Engine engine, @NonNull String cameraId) {
        super(engine);
        mHolder = engine;
        mCameraId = cameraId;
    }

    @Override
    protected void onStart() {
        // Do not start now. Instead, wait for the first frame.
        // Check that the request is the correct one, using the request tag.
        // The engine might have been changing the request to add our surface lately,
        // and we don't want to start on an old frame.
        Action action = new BaseAction() {
            @Override
            public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
                super.onCaptureStarted(holder, request);
                Object tag = holder.getBuilder(this).build().getTag();
                Object currentTag = request.getTag();
                if (tag == null ? currentTag == null : tag.equals(currentTag)) {
                    setState(STATE_COMPLETED);
                }
            }
        };
        action.addCallback(new CompletionCallback() {
            @Override
            protected void onActionCompleted(@NonNull Action action) {
                Full2VideoRecorder.super.onStart();
            }
        });
        action.start(mHolder);
    }

    @Override
    protected void applyVideoSource(@NonNull VideoResult.Stub stub,
                                    @NonNull MediaRecorder mediaRecorder) {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    }

    @NonNull
    @Override
    protected CamcorderProfile getCamcorderProfile(@NonNull VideoResult.Stub stub) {
        // This was an option: get the surface from outside this class, using
        // MediaCodec.createPersistentInputSurface(). But it doesn't really help since the
        // Camera2 engine refuses a surface that has not been configured, so even with that trick
        // we would have to attach the surface to this recorder before creating the CameraSession.
        // mediaRecorder.setInputSurface(mInputSurface);
        Size size = stub.rotation % 180 != 0 ? stub.size.flip() : stub.size;
        return CamcorderProfiles.get(mCameraId, size);
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
