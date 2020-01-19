package com.otaliastudios.cameraview.engine;


import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.engine.orchestrator.CameraState;
import com.otaliastudios.cameraview.frame.ByteBufferFrameManager;
import com.otaliastudios.cameraview.frame.FrameManager;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.metering.MeteringRegions;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MockCameraEngine extends CameraBaseEngine {

    public boolean mPictureCaptured;
    public boolean mFocusStarted;
    public boolean mZoomChanged;
    public boolean mExposureCorrectionChanged;

    public MockCameraEngine(CameraEngine.Callback callback) {
        super(callback);
    }

    @NonNull
    @Override
    protected Task<CameraOptions> onStartEngine() {
        return Tasks.forResult(mCameraOptions);
    }

    @NonNull
    @Override
    protected Task<Void> onStopEngine() {
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    protected Task<Void> onStartBind() {
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    protected Task<Void> onStopBind() {
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    protected Task<Void> onStartPreview() {
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    protected Task<Void> onStopPreview() {
        return Tasks.forResult(null);
    }

    public void setMockCameraOptions(CameraOptions options) {
        mCameraOptions = options;
    }

    public void setMockPreviewStreamSize(Size size) {
        mPreviewStreamSize = size;
    }

    public void setMockState(@NonNull CameraState state) {
        Task<Void> change = getOrchestrator().scheduleStateChange(getState(),
                state,
                false,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                return Tasks.forResult(null);
            }
        });
        try {
            Tasks.await(change);
        } catch (Exception ignore) {}
    }

    @Override
    public void setZoom(float zoom, @Nullable PointF[] points, boolean notify) {
        mZoomValue = zoom;
        mZoomChanged = true;
    }

    @Override
    public void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify) {
        mExposureCorrectionValue = EVvalue;
        mExposureCorrectionChanged = true;
    }

    @Override
    public void setFlash(@NonNull Flash flash) {
        mFlash = flash;
    }

    @Override
    public void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        mWhiteBalance = whiteBalance;
    }

    @Override
    public void setHdr(@NonNull Hdr hdr) {
        mHdr = hdr;
    }

    @Override
    public void setLocation(@Nullable Location location) {
        mLocation = location;
    }

    @Override
    public void setPictureFormat(@NonNull PictureFormat pictureFormat) {
        mPictureFormat = pictureFormat;
    }

    @Override
    public void setHasFrameProcessors(boolean hasFrameProcessors) {
        mHasFrameProcessors = hasFrameProcessors;
    }

    @Override
    public void setFrameProcessingFormat(int format) {
        mFrameProcessingFormat = format;
    }

    @Override
    public void takePicture(@NonNull PictureResult.Stub stub) {
        super.takePicture(stub);
        mPictureCaptured = true;
    }

    @Override
    protected void onTakePicture(@NonNull PictureResult.Stub stub, boolean doMetering) {

    }

    @Override
    protected void onTakePictureSnapshot(@NonNull PictureResult.Stub stub, @NonNull AspectRatio outputRatio, boolean doMetering) {

    }

    @Override
    protected void onTakeVideo(@NonNull VideoResult.Stub stub) {

    }

    @Override
    protected void onTakeVideoSnapshot(@NonNull VideoResult.Stub stub, @NonNull AspectRatio outputRatio) {

    }

    @Override
    protected void onPreviewStreamSizeChanged() {

    }

    @NonNull
    @Override
    protected List<Size> getPreviewStreamAvailableSizes() {
        return new ArrayList<>();
    }

    @NonNull
    @Override
    protected List<Size> getFrameProcessingAvailableSizes() {
        return new ArrayList<>();
    }

    @Override
    public void startAutoFocus(@Nullable Gesture gesture, @NonNull MeteringRegions regions, @NonNull PointF legacyPoint) {
        mFocusStarted = true;
    }

    @NonNull
    @Override
    protected FrameManager instantiateFrameManager(int poolSize) {
        return new ByteBufferFrameManager(poolSize, null);
    }

    @Override
    public void setPlaySounds(boolean playSounds) { }

    @Override
    protected boolean collectCameraInfo(@NonNull Facing facing) {
        return true;
    }

    @Override public void setPreviewFrameRate(float previewFrameRate) {
        mPreviewFrameRate = previewFrameRate;
    }
}
