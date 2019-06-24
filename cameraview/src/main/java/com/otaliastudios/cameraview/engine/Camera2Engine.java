package com.otaliastudios.cameraview.engine;

import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.view.SurfaceHolder;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.size.AspectRatio;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine extends CameraEngine {

    private static final String TAG = Camera2Engine.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public Camera2Engine(Callback callback) {
        super(callback);
        mMapper = Mapper.get(Engine.CAMERA2);
    }

    @Override
    public void onSurfaceAvailable() {

    }

    @Override
    public void onSurfaceChanged() {

    }

    @Override
    public void onSurfaceDestroyed() {

    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onStop() {

    }

    @Override
    public void onBufferAvailable(@NonNull byte[] buffer) {

    }

    @Override
    public void setMode(@NonNull Mode mode) {

    }

    @Override
    public void setFacing(@NonNull Facing facing) {

    }

    @Override
    public void setZoom(float zoom, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void setFlash(@NonNull Flash flash) {

    }

    @Override
    public void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {

    }

    @Override
    public void setHdr(@NonNull Hdr hdr) {

    }

    @Override
    public void setLocation(@Nullable Location location) {

    }

    @Override
    public void setAudio(@NonNull Audio audio) {

    }

    @Override
    public void takePicture(@NonNull PictureResult.Stub stub) {

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

    }

    @Override
    public void setPlaySounds(boolean playSounds) {

    }
}

