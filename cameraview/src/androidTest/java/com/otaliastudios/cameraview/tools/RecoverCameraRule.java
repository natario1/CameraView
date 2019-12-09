package com.otaliastudios.cameraview.tools;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraView;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RecoverCameraRule implements TestRule {

    public interface Callback {
        @NonNull
        CameraView getCamera();
        @NonNull
        CameraLogger getLogger();
    }

    private final Callback mCallback;
    private final CameraListener mListener = new CameraListener() {
        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            if (exception.isUnrecoverable()) {
                mException = exception;
            }
        }
    };
    private CameraException mException;

    public RecoverCameraRule(@NonNull Callback callback) {
        mCallback = callback;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                mException = null;
                mCallback.getCamera().addCameraListener(mListener);
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    if (mException != null) {
                        mCallback.getLogger().e("**************************************");
                        mCallback.getLogger().e("!!! TEST FAILED, TRYING TO RECOVER !!!");
                        mCallback.getLogger().e("**************************************");
                        mException = null;
                        mCallback.getCamera().destroy();
                        base.evaluate();
                    }
                } finally {
                    mCallback.getCamera().removeCameraListener(mListener);
                }
            }
        };
    }
}
