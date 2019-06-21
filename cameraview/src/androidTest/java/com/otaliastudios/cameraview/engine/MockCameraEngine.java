package com.otaliastudios.cameraview.engine;


import android.graphics.PointF;
import android.location.Location;

import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;

public class MockCameraEngine extends CameraEngine {

    public boolean mPictureCaptured;
    public boolean mFocusStarted;
    public boolean mZoomChanged;
    public boolean mExposureCorrectionChanged;

    public MockCameraEngine(CameraEngine.Callback callback) {
        super(callback);
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {
    }

    public void setMockCameraOptions(CameraOptions options) {
        mCameraOptions = options;
    }

    public void setMockPreviewStreamSize(Size size) {
        mPreviewStreamSize = size;
    }

    public void mockStarted(boolean started) {
        mState = started ? STATE_STARTED : STATE_STOPPED;
    }

    public int getSnapshotMaxWidth() {
        return mSnapshotMaxWidth;
    }

    public int getSnapshotMaxHeight() {
        return mSnapshotMaxHeight;
    }

    public SizeSelector getInternalPreviewStreamSizeSelector() {
        return super.getPreviewStreamSizeSelector();
    }

    public SizeSelector getInternalPictureSizeSelector() {
        return super.getPictureSizeSelector();
    }

    public SizeSelector getInternalVideoSizeSelector() {
        return super.getVideoSizeSelector();
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
    public void setFacing(@NonNull Facing facing) {
        mFacing = facing;
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
    public void setMode(@NonNull Mode mode) {
        mMode = mode;
    }

    @Override
    public void setHdr(@NonNull Hdr hdr) {
        mHdr = hdr;
    }

    @Override
    public void setAudio(@NonNull Audio audio) {
        mAudio = audio;
    }

    @Override
    public void setLocation(@Nullable Location location) {
        mLocation = location;
    }

    @Override
    public void takePicture(@NonNull PictureResult.Stub stub) {
        mPictureCaptured = true;
    }

    @Override
    public void takePictureSnapshot(@NonNull PictureResult.Stub stub, @NonNull AspectRatio viewAspectRatio) {
    }

    @Override
    public void takeVideo(@NonNull VideoResult.Stub stub, @NonNull File file) {
    }

    @Override
    public void takeVideoSnapshot(@NonNull VideoResult.Stub stub, @NonNull File file, @NonNull AspectRatio viewAspectRatio) {
    }


    @Override
    public void stopVideo() {
    }

    @Override
    public void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point) {
        mFocusStarted = true;
    }

    @Override
    public void onSurfaceChanged() {
    }

    @Override
    public void onSurfaceAvailable() {
    }

    @Override
    public void onSurfaceDestroyed() {

    }

    @Override
    public void onBufferAvailable(@NonNull byte[] buffer) {
    }

    @Override
    public void setPlaySounds(boolean playSounds) {

    }
}
