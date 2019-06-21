package com.otaliastudios.cameraview;


import android.graphics.PointF;
import android.location.Location;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class MockCameraEngine extends CameraEngine {

    boolean mPictureCaptured;
    boolean mFocusStarted;
    boolean mZoomChanged;
    boolean mExposureCorrectionChanged;

    MockCameraEngine(CameraView.CameraCallbacks callback) {
        super(callback);
    }

    void setMockCameraOptions(CameraOptions options) {
        mCameraOptions = options;
    }

    void setMockPreviewStreamSize(Size size) {
        mPreviewStreamSize = size;
    }

    void mockStarted(boolean started) {
        mState = started ? STATE_STARTED : STATE_STOPPED;
    }

    @Override
    void onStart() {
    }

    @Override
    void onStop() {
    }

    @Override
    void setZoom(float zoom, @Nullable PointF[] points, boolean notify) {
        mZoomValue = zoom;
        mZoomChanged = true;
    }

    @Override
    void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify) {
        mExposureCorrectionValue = EVvalue;
        mExposureCorrectionChanged = true;
    }

    @Override
    void setFacing(@NonNull Facing facing) {
        mFacing = facing;
    }

    @Override
    void setFlash(@NonNull Flash flash) {
        mFlash = flash;
    }

    @Override
    void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        mWhiteBalance = whiteBalance;
    }

    @Override
    void setMode(@NonNull Mode mode) {
        mMode = mode;
    }

    @Override
    void setHdr(@NonNull Hdr hdr) {
        mHdr = hdr;
    }

    @Override
    void setAudio(@NonNull Audio audio) {
        mAudio = audio;
    }

    @Override
    void setLocation(@Nullable Location location) {
        mLocation = location;
    }

    @Override
    void takePicture() {
        mPictureCaptured = true;
    }

    @Override
    void takePictureSnapshot(@NonNull AspectRatio viewAspectRatio) {
    }

    @Override
    void takeVideo(@NonNull File file) {
    }

    @Override
    void takeVideoSnapshot(@NonNull File file, @NonNull AspectRatio viewAspectRatio) {

    }

    @Override
    void stopVideo() {
    }

    @Override
    void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point) {
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
    void setPlaySounds(boolean playSounds) {

    }
}