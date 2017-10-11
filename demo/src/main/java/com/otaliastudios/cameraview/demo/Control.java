package com.otaliastudios.cameraview.demo;

import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.otaliastudios.cameraview.Grid;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.VideoQuality;
import com.otaliastudios.cameraview.WhiteBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Controls that we want to display in a ControlView.
 */
public enum Control {

    WIDTH("Width", false),
    HEIGHT("Height", true),
    SESSION("Session type", false),
    CROP_OUTPUT("Crop output", true),
    FLASH("Flash", false),
    WHITE_BALANCE("White balance", false),
    GRID("Grid", true),
    VIDEO_QUALITY("Video quality", false),
    AUDIO("Audio", true),
    PINCH("Pinch gesture", false),
    HSCROLL("Horizontal scroll gesture", false),
    VSCROLL("Vertical scroll gesture", false),
    TAP("Single tap gesture", false),
    LONG_TAP("Long tap gesture", true);

    private String name;
    private boolean last;

    Control(String n, boolean l) {
        name = n;
        last = l;
    }

    public String getName() {
        return name;
    }

    public boolean isSectionLast() {
        return last;
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
            case PINCH:
            case HSCROLL:
            case VSCROLL:
                ArrayList<GestureAction> list1 = new ArrayList<>();
                addIfSupported(options, list1, GestureAction.NONE);
                addIfSupported(options, list1, GestureAction.ZOOM);
                addIfSupported(options, list1, GestureAction.EXPOSURE_CORRECTION);
                return list1;
            case TAP:
            case LONG_TAP:
                ArrayList<GestureAction> list2 = new ArrayList<>();
                addIfSupported(options, list2, GestureAction.NONE);
                addIfSupported(options, list2, GestureAction.CAPTURE);
                addIfSupported(options, list2, GestureAction.FOCUS);
                addIfSupported(options, list2, GestureAction.FOCUS_WITH_MARKER);
                return list2;

        }
        return null;
    }

    private void addIfSupported(CameraOptions options, List<GestureAction> list, GestureAction value) {
        if (options.supports(value)) list.add(value);
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
            case PINCH: return view.getGestureAction(Gesture.PINCH);
            case HSCROLL: return view.getGestureAction(Gesture.SCROLL_HORIZONTAL);
            case VSCROLL: return view.getGestureAction(Gesture.SCROLL_VERTICAL);
            case TAP: return view.getGestureAction(Gesture.TAP);
            case LONG_TAP: return view.getGestureAction(Gesture.LONG_TAP);
        }
        return null;
    }

    public void applyValue(CameraView camera, Object value) {
        switch (this) {
            case WIDTH:
                camera.getLayoutParams().width = (int) value;
                camera.setLayoutParams(camera.getLayoutParams());
                break;
            case HEIGHT:
                camera.getLayoutParams().height = (int) value;
                camera.setLayoutParams(camera.getLayoutParams());
                break;
            case SESSION:
                camera.setSessionType((SessionType) value);
                break;
            case CROP_OUTPUT:
                camera.setCropOutput((boolean) value);
                break;
            case FLASH:
                camera.setFlash((Flash) value);
                break;
            case WHITE_BALANCE:
                camera.setWhiteBalance((WhiteBalance) value);
                break;
            case GRID:
                camera.setGrid((Grid) value);
                break;
            case VIDEO_QUALITY:
                camera.setVideoQuality((VideoQuality) value);
                break;
            case AUDIO:
                camera.setAudio((Audio) value);
                break;
            case PINCH:
                camera.mapGesture(Gesture.PINCH, (GestureAction) value);
                break;
            case HSCROLL:
                camera.mapGesture(Gesture.SCROLL_HORIZONTAL, (GestureAction) value);
                break;
            case VSCROLL:
                camera.mapGesture(Gesture.SCROLL_VERTICAL, (GestureAction) value);
                break;
            case TAP:
                camera.mapGesture(Gesture.TAP, (GestureAction) value);
                break;
            case LONG_TAP:
                camera.mapGesture(Gesture.LONG_TAP, (GestureAction) value);
                break;
        }
    }


}
