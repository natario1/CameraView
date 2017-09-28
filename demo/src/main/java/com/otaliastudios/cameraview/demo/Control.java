package com.otaliastudios.cameraview.demo;

import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Grid;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.VideoQuality;
import com.otaliastudios.cameraview.WhiteBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Controls that we want to display in a ControlView.
 */
public enum Control {

    WIDTH("Width", Integer.class),
    HEIGHT("Height", Integer.class),
    SESSION("Session type", SessionType.class),
    CROP_OUTPUT("Crop output", Boolean.class),
    FLASH("Flash", Flash.class),
    WHITE_BALANCE("White balance", WhiteBalance.class),
    GRID("Grid", Grid.class),
    VIDEO_QUALITY("Video quality", VideoQuality.class),
    AUDIO("Audio", Audio.class);

    private String name;
    private Class<?> cla;

    Control(String n, Class<?> cl) {
        name = n;
        cla = cl;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return cla;
    }

    public Collection<?> getValues(CameraView view) {
        CameraOptions options = view.getCameraOptions();
        switch (this) {
            case WIDTH:
            case HEIGHT:
                View root = (View) view.getParent();
                ArrayList<Integer> list = new ArrayList<>();
                int boundary = this == WIDTH ? root.getWidth() : root.getHeight();
                if (boundary == 0) boundary = 1000;
                int step = boundary / 10;
                list.add(ViewGroup.LayoutParams.WRAP_CONTENT);
                list.add(ViewGroup.LayoutParams.MATCH_PARENT);
                for (int i = step; i < boundary; i += step) {
                    list.add(i);
                }
                return list;
            case SESSION: return Arrays.asList(SessionType.values());
            case CROP_OUTPUT: return Arrays.asList(true, false);
            case FLASH: return options.getSupportedFlash();
            case WHITE_BALANCE: return options.getSupportedWhiteBalance();
            case GRID: return Arrays.asList(Grid.values());
            case VIDEO_QUALITY: return Arrays.asList(VideoQuality.values());
            case AUDIO: return Arrays.asList(Audio.values());
        }
        return null;
    }

    public Object getCurrentValue(CameraView view) {
        switch (this) {
            case WIDTH: return view.getLayoutParams().width;
            case HEIGHT: return view.getLayoutParams().height;
            case SESSION: return view.getSessionType();
            case CROP_OUTPUT: return view.getCropOutput();
            case FLASH: return view.getFlash();
            case WHITE_BALANCE: return view.getWhiteBalance();
            case GRID: return view.getGrid();
            case VIDEO_QUALITY: return view.getVideoQuality();
            case AUDIO: return view.getAudio();
        }
        return null;
    }


}
