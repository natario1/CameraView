package com.flurgle.camerakit;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseIntArray;

import java.util.Map;

public class ConstantMapper {

    static abstract class MapperImpl {

        MapperImpl() {}

        abstract <T> T mapFlash(@Flash int internalConstant);
        abstract <T> T mapFacing(@Facing int internalConstant);
        abstract <T> T mapWhiteBalance(@WhiteBalance int internalConstant);
    }

    static class Mapper1 extends MapperImpl {
        private static final SparseArrayCompat<String> FLASH = new SparseArrayCompat<>();
        private static final SparseArrayCompat<String> WB = new SparseArrayCompat<>();
        private static final SparseArrayCompat<Integer> FACING = new SparseArrayCompat<>();

        static {
            FLASH.put(CameraKit.Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
            FLASH.put(CameraKit.Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
            FLASH.put(CameraKit.Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
            FLASH.put(CameraKit.Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
            FACING.put(CameraKit.Constants.FACING_BACK, Camera.CameraInfo.CAMERA_FACING_BACK);
            FACING.put(CameraKit.Constants.FACING_FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT);
            WB.put(CameraKit.Constants.WHITE_BALANCE_AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
            WB.put(CameraKit.Constants.WHITE_BALANCE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
            WB.put(CameraKit.Constants.WHITE_BALANCE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
            WB.put(CameraKit.Constants.WHITE_BALANCE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
            WB.put(CameraKit.Constants.WHITE_BALANCE_CLOUDY, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        }

        @Override
        <T> T mapFlash(int internalConstant) {
            return (T) FLASH.get(internalConstant, null);
        }

        @Override
        <T> T mapFacing(int internalConstant) {
            return (T) FACING.get(internalConstant, null);
        }

        @Override
        <T> T mapWhiteBalance(int internalConstant) {
            return (T) WB.get(internalConstant, null);
        }
    }

    static class Mapper2 extends MapperImpl {

        @Override
        <T> T mapWhiteBalance(@WhiteBalance int internalConstant) {
            return null;
        }

        @Override
        <T> T mapFacing(@Facing int internalConstant) {
            return null;
        }

        @Override
        <T> T mapFlash(@Flash int internalConstant) {
            return null;
        }
    }

}
