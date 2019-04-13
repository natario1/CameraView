package com.otaliastudios.cameraview.controls;

import android.content.Context;
import android.content.res.TypedArray;

import com.otaliastudios.cameraview.R;

import androidx.annotation.NonNull;

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
    }

    public Preview getPreview() {
        return Preview.fromValue(preview);
    }

    public Facing getFacing() {
        return Facing.fromValue(facing);
    }

    public Flash getFlash() {
        return Flash.fromValue(flash);
    }

    public Grid getGrid() {
        return Grid.fromValue(grid);
    }

    public Mode getMode() {
        return Mode.fromValue(mode);
    }

    public WhiteBalance getWhiteBalance() {
        return WhiteBalance.fromValue(whiteBalance);
    }

    public Hdr getHdr() {
        return Hdr.fromValue(hdr);
    }

    public Audio getAudio() {
        return Audio.fromValue(audio);
    }

    public VideoCodec getVideoCodec() {
        return VideoCodec.fromValue(videoCodec);
    }
}
