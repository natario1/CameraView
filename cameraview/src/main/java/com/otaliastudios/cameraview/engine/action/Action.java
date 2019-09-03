package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface Action {

    int STATE_COMPLETED = Integer.MAX_VALUE;

    int getState();

    void start(@NonNull ActionHolder holder);

    void addCallback(@NonNull ActionCallback callback);

    void removeCallback(@NonNull ActionCallback callback);

    void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request);

    void onCaptureProgressed(@NonNull ActionHolder holder, @NonNull CaptureRequest request, @NonNull CaptureResult result);

    void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result);
}
