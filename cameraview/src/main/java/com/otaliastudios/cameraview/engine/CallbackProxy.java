package com.otaliastudios.cameraview.engine;

import android.content.Context;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.gesture.Gesture;

public class CallbackProxy implements CameraEngine.Callback {
    private CameraEngine.Callback mCallbacks;

    public void setCallbacks(CameraEngine.Callback callbacks) {
        mCallbacks = callbacks;
    }

    @NonNull
    @Override
    public Context getContext() {
        return mCallbacks.getContext();
    }

    @Override
    public void dispatchOnCameraOpened(@NonNull CameraOptions options) {
        mCallbacks.dispatchOnCameraOpened(options);
    }

    @Override
    public void dispatchOnCameraClosed() {
        mCallbacks.dispatchOnCameraClosed();
    }

    @Override
    public void onCameraPreviewStreamSizeChanged() {
        mCallbacks.onCameraPreviewStreamSizeChanged();
    }

    @Override
    public void dispatchOnPictureShutter(boolean shouldPlaySound) {
        mCallbacks.dispatchOnPictureShutter(shouldPlaySound);
    }

    @Override
    public void dispatchOnVideoTaken(@NonNull VideoResult.Stub stub) {
        mCallbacks.dispatchOnVideoTaken(stub);
    }

    @Override
    public void dispatchOnPictureTaken(@NonNull PictureResult.Stub stub) {
        mCallbacks.dispatchOnPictureTaken(stub);
    }

    @Override
    public void dispatchOnFocusStart(@Nullable Gesture trigger, @NonNull PointF where) {
        mCallbacks.dispatchOnFocusStart(trigger, where);
    }

    @Override
    public void dispatchOnFocusEnd(@Nullable Gesture trigger, boolean success, @NonNull PointF where) {
        mCallbacks.dispatchOnFocusEnd(trigger, success, where);
    }

    @Override
    public void dispatchOnZoomChanged(float newValue, @Nullable PointF[] fingers) {
        mCallbacks.dispatchOnZoomChanged(newValue, fingers);
    }

    @Override
    public void dispatchOnExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
        mCallbacks.dispatchOnExposureCorrectionChanged(newValue, bounds, fingers);
    }

    @Override
    public void dispatchFrame(@NonNull Frame frame) {
        mCallbacks.dispatchFrame(frame);
    }

    @Override
    public void dispatchError(CameraException exception) {
        mCallbacks.dispatchError(exception);
    }

    @Override
    public void dispatchOnVideoRecordingStart() {
        mCallbacks.dispatchOnVideoRecordingStart();
    }

    @Override
    public void dispatchOnVideoRecordingEnd() {
        mCallbacks.dispatchOnVideoRecordingEnd();
    }
}