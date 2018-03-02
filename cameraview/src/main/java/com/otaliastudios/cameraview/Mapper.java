package com.otaliastudios.cameraview;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;

import java.util.HashMap;

abstract class Mapper {

    abstract <T> T map(Flash flash);
    abstract <T> T map(Facing facing);
    abstract <T> T map(WhiteBalance whiteBalance);
    abstract <T> T map(Hdr hdr);
    abstract <T> Flash unmapFlash(T cameraConstant);
    abstract <T> Facing unmapFacing(T cameraConstant);
    abstract <T> WhiteBalance unmapWhiteBalance(T cameraConstant);
    abstract <T> Hdr unmapHdr(T cameraConstant);

    int map(VideoCodec codec) {
        switch (codec) {
            case DEVICE_DEFAULT: return MediaRecorder.VideoEncoder.DEFAULT;
            case H_263: return MediaRecorder.VideoEncoder.H263;
            case H_264: return MediaRecorder.VideoEncoder.H264;
            default: return MediaRecorder.VideoEncoder.DEFAULT;
        }
    }
}
