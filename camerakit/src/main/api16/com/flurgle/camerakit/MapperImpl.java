package com.flurgle.camerakit;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseIntArray;

import java.util.Map;

abstract class MapperImpl {

    abstract <T> T mapFlash(@Flash int internalConstant);
    abstract <T> T mapFacing(@Facing int internalConstant);
    abstract <T> T mapWhiteBalance(@WhiteBalance int internalConstant);
    abstract <T> T mapFocus(@Focus int internalConstant);
    @Flash abstract <T> int unmapFlash(T cameraConstant);
    @Facing abstract <T> int unmapFacing(T cameraConstant);
    @WhiteBalance abstract <T> int unmapWhiteBalance(T cameraConstant);
    @Focus abstract <T> int unmapFocus(T cameraConstant);

    static class Mapper1 extends MapperImpl {
        private static final SparseArrayCompat<String> FLASH = new SparseArrayCompat<>();
        private static final SparseArrayCompat<String> WB = new SparseArrayCompat<>();
        private static final SparseArrayCompat<Integer> FACING = new SparseArrayCompat<>();
        private static final SparseArrayCompat<String> FOCUS = new SparseArrayCompat<>();

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
            FOCUS.put(CameraKit.Constants.FOCUS_FIXED, Camera.Parameters.FOCUS_MODE_FIXED);
            FOCUS.put(CameraKit.Constants.FOCUS_CONTINUOUS, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            FOCUS.put(CameraKit.Constants.FOCUS_TAP, Camera.Parameters.FOCUS_MODE_AUTO);
            FOCUS.put(CameraKit.Constants.FOCUS_TAP_WITH_MARKER, Camera.Parameters.FOCUS_MODE_AUTO);
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

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return (T) FOCUS.get(internalConstant, null);
        }

        @Override
        <T> int unmapFlash(T cameraConstant) {
            return FLASH.keyAt(FLASH.indexOfValue((String) cameraConstant));
        }

        @Override
        <T> int unmapFacing(T cameraConstant) {
            return FACING.keyAt(FACING.indexOfValue((Integer) cameraConstant));
        }

        @Override
        <T> int unmapWhiteBalance(T cameraConstant) {
            return WB.keyAt(WB.indexOfValue((String) cameraConstant));
        }

        // This will ignore FOCUS_TAP_WITH_MARKER but it's fine
        @Override
        <T> int unmapFocus(T cameraConstant) {
            return FOCUS.keyAt(FOCUS.indexOfValue((String) cameraConstant));
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

        @Override
        <T> int unmapFlash(T cameraConstant) {
            return 0;
        }

        @Override
        <T> int unmapFacing(T cameraConstant) {
            return 0;
        }

        @Override
        <T> int unmapWhiteBalance(T cameraConstant) {
            return 0;
        }

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return null;
        }

        @Override
        <T> int unmapFocus(T cameraConstant) {
            return 0;
        }
    }

}
