package com.otaliastudios.cameraview.engine.orchestrator;

import androidx.annotation.NonNull;

public enum CameraState {
    OFF(0), ENGINE(1), BIND(2), PREVIEW(3);

    private int mState;

    CameraState(int state) {
        mState = state;
    }

    public boolean isAtLeast(@NonNull CameraState reference) {
        return mState >= reference.mState;
    }
}
