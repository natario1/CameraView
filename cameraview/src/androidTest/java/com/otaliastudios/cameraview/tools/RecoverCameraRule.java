package com.otaliastudios.cameraview.tools;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public RecoverCameraRule(@NonNull Callback callback) {
        mCallback = callback;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    mCallback.getLogger().w("**************************************");
                    mCallback.getLogger().w("!!! TEST FAILED? THROWABLE FOUND:", throwable);
                    mCallback.getLogger().w("**************************************");
                    CameraException exception = findCameraException(throwable);
                    if (exception != null) {
                        mCallback.getLogger().e("**************************************");
                        mCallback.getLogger().e("!!! TEST FAILED, TRYING TO RECOVER !!!");
                        mCallback.getLogger().e("**************************************");
                        mCallback.getCamera().destroy();
                        base.evaluate();
                    }
                }
            }
        };
    }

    @Nullable
    private CameraException findCameraException(@NonNull Throwable throwable) {
        if (throwable instanceof CameraException
                && ((CameraException) throwable).isUnrecoverable()) {
            return (CameraException) throwable;
        }
        if (throwable.getCause() == null) return null;
        if (throwable == throwable.getCause()) return null;
        return findCameraException(throwable.getCause());
    }
}
