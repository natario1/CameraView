package com.otaliastudios.cameraview.controls;

import android.content.Context;
import android.content.res.TypedArray;

import com.otaliastudios.cameraview.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parses controls from XML attributes.
 */
public class ControlParser {

    private int preview;
    private int facing;
    private int flash;
    private int grid;
    private int whiteBalance;
    private int mode;
    private int hdr;
    private int audio;
    private int videoCodec;
    private int engine;

    public ControlParser(@NonNull Context context, @NonNull TypedArray array) {
        this.preview = array.getInteger(R.styleable.CameraView_cameraPreview, Preview.DEFAULT.value());
        this.facing = array.getInteger(R.styleable.CameraView_cameraFacing, Facing.DEFAULT(context).value());
        this.flash = array.getInteger(R.styleable.CameraView_cameraFlash, Flash.DEFAULT.value());
        this.grid = array.getInteger(R.styleable.CameraView_cameraGrid, Grid.DEFAULT.value());
        this.whiteBalance = array.getInteger(R.styleable.CameraView_cameraWhiteBalance, WhiteBalance.DEFAULT.value());
        this.mode = array.getInteger(R.styleable.CameraView_cameraMode, Mode.DEFAULT.value());
        this.hdr = array.getInteger(R.styleable.CameraView_cameraHdr, Hdr.DEFAULT.value());
        this.audio = array.getInteger(R.styleable.CameraView_cameraAudio, Audio.DEFAULT.value());
        this.videoCodec = array.getInteger(R.styleable.CameraView_cameraVideoCodec, VideoCodec.DEFAULT.value());
        this.engine = array.getInteger(R.styleable.CameraView_cameraEngine, Engine.DEFAULT.value());
    }

    @NonNull
    public Preview getPreview() {
        return Preview.fromValue(preview);
    }

    @NonNull
    public Facing getFacing() {
        //noinspection ConstantConditions
        return Facing.fromValue(facing);
    }

    @NonNull
    public Flash getFlash() {
        return Flash.fromValue(flash);
    }

    @NonNull
    public Grid getGrid() {
        return Grid.fromValue(grid);
    }

    @NonNull
    public Mode getMode() {
        return Mode.fromValue(mode);
    }

    @NonNull
    public WhiteBalance getWhiteBalance() {
        return WhiteBalance.fromValue(whiteBalance);
    }

    @NonNull
    public Hdr getHdr() {
        return Hdr.fromValue(hdr);
    }

    @NonNull
    public Audio getAudio() {
        return Audio.fromValue(audio);
    }

    @NonNull
    public VideoCodec getVideoCodec() {
        return VideoCodec.fromValue(videoCodec);
    }

    @NonNull
    public Engine getEngine() {
        return Engine.fromValue(engine);
    }
}
