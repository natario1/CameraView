package com.otaliastudios.cameraview;

import android.annotation.TargetApi;
import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

@TargetApi(21)
class Camera2 extends CameraController {

    public Camera2(CameraView.CameraCallbacks callback) {
        super(callback);
    }

    @Override
    public void onSurfaceAvailable() {

    }

    @Override
    public void onSurfaceChanged() {

    }

    @Override
    void onStart() {

    }

    @Override
    void onStop() {

    }

    @Override
    void setSessionType(SessionType sessionType) {

    }

    @Override
    void setFacing(Facing facing) {

    }

    @Override
    void setZoom(float zoom, PointF[] points, boolean notify) {

    }

    @Override
    void setExposureCorrection(float EVvalue, float[] bounds, PointF[] points, boolean notify) {

    }

    @Override
    void setFlash(Flash flash) {

    }

    @Override
    void setWhiteBalance(WhiteBalance whiteBalance) {

    }

    @Override
    void setHdr(Hdr hdr) {

    }

    @Override
    void setAudio(Audio audio) {

    }

    @Override
    void setLocation(Location location) {

    }

    @Override
    void setVideoQuality(VideoQuality videoQuality) {

    }

    @Override
    void takePicture() {

    }

    @Override
    void takePictureSnapshot(boolean shouldCrop, AspectRatio viewAspectRatio) {

    }

    @Override
    void takeVideo(@NonNull File file) {

    }

    @Override
    void stopVideo() {

    }

    @Override
    void startAutoFocus(@Nullable Gesture gesture, PointF point) {

    }

    @Override
    public void onBufferAvailable(byte[] buffer) {

    }

    @Override
    void setPlaySounds(boolean playSounds) {

    }
}
