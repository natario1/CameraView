package com.otaliastudios.cameraview.demo;

import android.graphics.Color;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.otaliastudios.cameraview.Grid;
import com.otaliastudios.cameraview.Hdr;
import com.otaliastudios.cameraview.Mode;
import com.otaliastudios.cameraview.VideoCodec;
import com.otaliastudios.cameraview.WhiteBalance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Controls that we want to display in a ControlView.
 */
public enum Control {

    WIDTH("Width", false),
    HEIGHT("Height", true),

    MODE("Mode", false),
    FLASH("Flash", false),
    WHITE_BALANCE("White balance", false),
    HDR("Hdr", true),

    GRID("Grid lines", false),
    GRID_COLOR("Grid color", true),

    // TODO audio bitRate
    // TODO video bitRate
    // THey are a bit annoying because it's not clear what the default should be.
    VIDEO_CODEC("Video codec", false),
    AUDIO("Audio", true),

    PINCH("Pinch", false),
    HSCROLL("Horizontal scroll", false),
    VSCROLL("Vertical scroll", false),
    TAP("Single tap", false),
    LONG_TAP("Long tap", true);

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

    public Collection<?> getValues(CameraView view, @NonNull CameraOptions options) {
        switch (this) {
            case WIDTH:
            case HEIGHT:
                View root = (View) view.getParent();
                ArrayList<Integer> list = new ArrayList<>();
                int boundary = this == WIDTH ? root.getWidth() : root.getHeight();
                if (boundary == 0) boundary = 1000;
                int step = boundary / 10;
                // list.add(this == WIDTH ? 300 : 700);
                list.add(ViewGroup.LayoutParams.WRAP_CONTENT);
                list.add(ViewGroup.LayoutParams.MATCH_PARENT);
                for (int i = step; i < boundary; i += step) {
                    list.add(i);
                }
                return list;
            case MODE: return options.getSupportedControls(Mode.class);
            case FLASH: return options.getSupportedControls(Flash.class);
            case WHITE_BALANCE: return options.getSupportedControls(WhiteBalance.class);
            case HDR: return options.getSupportedControls(Hdr.class);
            case GRID: return options.getSupportedControls(Grid.class);
            case AUDIO: return options.getSupportedControls(Audio.class);
            case VIDEO_CODEC: return options.getSupportedControls(VideoCodec.class);
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
            case GRID_COLOR:
                ArrayList<GridColor> list3 = new ArrayList<>();
                list3.add(new GridColor(Color.argb(160, 255, 255, 255), "default"));
                list3.add(new GridColor(Color.WHITE, "white"));
                list3.add(new GridColor(Color.BLACK, "black"));
                list3.add(new GridColor(Color.YELLOW, "yellow"));
                return list3;
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
            case MODE: return view.getMode();
            case FLASH: return view.getFlash();
            case WHITE_BALANCE: return view.getWhiteBalance();
            case GRID: return view.getGrid();
            case GRID_COLOR: return new GridColor(view.getGridColor(), "color");
            case AUDIO: return view.getAudio();
            case VIDEO_CODEC: return view.getVideoCodec();
            case HDR: return view.getHdr();
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
            case MODE:
            case FLASH:
            case WHITE_BALANCE:
            case GRID:
            case AUDIO:
            case VIDEO_CODEC:
            case HDR:
                camera.set((com.otaliastudios.cameraview.Control) value);
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
            case GRID_COLOR:
                camera.setGridColor(((GridColor) value).color);
        }
    }


    static class GridColor {
        int color;
        String name;

        GridColor(int color, String name) {
            this.color = color;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof GridColor && color == ((GridColor) obj).color;
        }
    }
}
