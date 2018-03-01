package com.otaliastudios.cameraview;


import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public class MockCameraController extends CameraController {

    boolean mPictureCaptured;
    boolean mFocusStarted;
    boolean mZoomChanged;
    boolean mExposureCorrectionChanged;

    MockCameraController(CameraView.CameraCallbacks callback) {
        super(callback);
    }

    void setMockCameraOptions(CameraOptions options) {
        mCameraOptions = options;
    }

    void setMockPreviewSize(Size size) {
        mPreviewSize = size;
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
    void setZoom(float zoom, PointF[] points, boolean notify) {
        mZoomValue = zoom;
        mZoomChanged = true;
    }

    @Override
    void setExposureCorrection(float EVvalue, float[] bounds, PointF[] points, boolean notify) {
        mExposureCorrectionValue = EVvalue;
        mExposureCorrectionChanged = true;
    }

    @Override
    void setFacing(Facing facing) {
        mFacing = facing;
    }

    @Override
    void setFlash(Flash flash) {
        mFlash = flash;
    }

    @Override
    void setWhiteBalance(WhiteBalance whiteBalance) {
        mWhiteBalance = whiteBalance;
    }

    @Override
    void setVideoQuality(VideoQuality videoQuality) {
        mVideoQuality = videoQuality;
    }

    @Override
    void setSessionType(SessionType sessionType) {
        mSessionType = sessionType;
    }

    @Override
    void setHdr(Hdr hdr) {
        mHdr = hdr;
    }

    @Override
    void setAudio(Audio audio) {
        mAudio = audio;
    }

    @Override
    void setLocation(Location location) {
        mLocation = location;
    }

    @Override
    void capturePicture() {
        mPictureCaptured = true;
    }

    @Override
    void captureSnapshot() {
    }

    @Override
    void startVideo(@NonNull File file) {
    }

    @Override
    void endVideo() {
    }

    @Override
    void startAutoFocus(@Nullable Gesture gesture, PointF point) {
        mFocusStarted = true;
    }

    @Override
    public void onSurfaceChanged() {
    }

    @Override
    public void onSurfaceAvailable() {
    }

    @Override
    public void onBufferAvailable(byte[] buffer) {
    }

    @Override
    void setPlaySounds(boolean playSounds) {

    }
}
