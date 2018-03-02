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


    static class Mapper1 extends Mapper {

        private static final HashMap<Flash, String> FLASH = new HashMap<>();
        private static final HashMap<WhiteBalance, String> WB = new HashMap<>();
        private static final HashMap<Facing, Integer> FACING = new HashMap<>();
        private static final HashMap<Hdr, String> HDR = new HashMap<>();

        static {
            FLASH.put(Flash.OFF, Camera.Parameters.FLASH_MODE_OFF);
            FLASH.put(Flash.ON, Camera.Parameters.FLASH_MODE_ON);
            FLASH.put(Flash.AUTO, Camera.Parameters.FLASH_MODE_AUTO);
            FLASH.put(Flash.TORCH, Camera.Parameters.FLASH_MODE_TORCH);
            FACING.put(Facing.BACK, Camera.CameraInfo.CAMERA_FACING_BACK);
            FACING.put(Facing.FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT);
            WB.put(WhiteBalance.AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
            WB.put(WhiteBalance.INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
            WB.put(WhiteBalance.FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
            WB.put(WhiteBalance.DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
            WB.put(WhiteBalance.CLOUDY, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            HDR.put(Hdr.OFF, Camera.Parameters.SCENE_MODE_AUTO);
            if (Build.VERSION.SDK_INT >= 17) {
                HDR.put(Hdr.ON, Camera.Parameters.SCENE_MODE_HDR);
            } else {
                HDR.put(Hdr.ON, "hdr");
            }
        }

        @Override
        <T> T map(Flash flash) {
            return (T) FLASH.get(flash);
        }

        @Override
        <T> T map(Facing facing) {
            return (T) FACING.get(facing);
        }

        @Override
        <T> T map(WhiteBalance whiteBalance) {
            return (T) WB.get(whiteBalance);
        }

        @Override
        <T> T map(Hdr hdr) {
            return (T) HDR.get(hdr);
        }

        private <T> T reverseLookup(HashMap<T, ?> map, Object object) {
            for (T value : map.keySet()) {
                if (map.get(value).equals(object)) {
                    return value;
                }
            }
            return null;
        }

        @Override
        <T> Flash unmapFlash(T cameraConstant) {
            return reverseLookup(FLASH, cameraConstant);
        }

        @Override
        <T> Facing unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, cameraConstant);
        }

        @Override
        <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, cameraConstant);
        }

        @Override
        <T> Hdr unmapHdr(T cameraConstant) {
            return reverseLookup(HDR, cameraConstant);
        }
    }

    static class Mapper2 extends Mapper {

        @Override
        <T> T map(WhiteBalance whiteBalance) {
            return null;
        }

        @Override
        <T> T map(Flash flash) {
            return null;
        }

        @Override
        <T> Flash unmapFlash(T cameraConstant) {
            return null;
        }

        @Override
        <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return null;
        }

        @Override
        <T> T map(Facing facing) {
            return null;
        }

        @Override
        <T> Facing unmapFacing(T cameraConstant) {
            return null;
        }

        @Override
        <T> T map(Hdr hdr) {
            return null;
        }

        @Override
        <T> Hdr unmapHdr(T cameraConstant) {
            return null;
        }
    }

}
