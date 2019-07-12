package com.otaliastudios.cameraview.demo;

import android.graphics.Color;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Controls that we want to display in a ControlView.
 */
public enum Control {

    // Layout
    WIDTH("Width", false),
    HEIGHT("Height", true),

    // Some controls
    MODE("Mode", false),
    FLASH("Flash", false),
    WHITE_BALANCE("White balance", false),
    HDR("Hdr", true),

    // Engine and preview
    ENGINE("Engine", false),
    PREVIEW("Preview Surface", true),

    // Video recording
    VIDEO_CODEC("Video codec", false),
    AUDIO("Audio", true),
    // TODO audio bitRate
    // TODO video bitRate
    // They are a bit annoying because it's not clear what the default should be.

    // Gestures
    PINCH("Pinch", false),
    HSCROLL("Horizontal scroll", false),
    VSCROLL("Vertical scroll", false),
    TAP("Single tap", false),
    LONG_TAP("Long tap", true),

    // Others
    GRID("Grid lines", false),
    GRID_COLOR("Grid color", false),
    USE_DEVICE_ORIENTATION("Use device orientation", true);

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
            case ENGINE: return options.getSupportedControls(Engine.class);
            case PREVIEW: return options.getSupportedControls(Preview.class);
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
                addIfSupported(options, list2, GestureAction.TAKE_PICTURE);
                addIfSupported(options, list2, GestureAction.AUTO_FOCUS);
                return list2;
            case GRID_COLOR:
                ArrayList<GridColor> list3 = new ArrayList<>();
                list3.add(new GridColor(Color.argb(160, 255, 255, 255), "default"));
                list3.add(new GridColor(Color.WHITE, "white"));
                list3.add(new GridColor(Color.BLACK, "black"));
                list3.add(new GridColor(Color.YELLOW, "yellow"));
                return list3;
            case USE_DEVICE_ORIENTATION:
                return Arrays.asList(true, false);
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
            case USE_DEVICE_ORIENTATION: return view.getUseDeviceOrientation();
            case ENGINE: return view.getEngine();
            case PREVIEW: return view.getPreview();
        }
        return null;
    }

    public void applyValue(final CameraView camera, final Object value) {
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
                camera.set((com.otaliastudios.cameraview.controls.Control) value);
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
                break;
            case USE_DEVICE_ORIENTATION:
                camera.setUseDeviceOrientation((Boolean) value);
                break;
            case ENGINE:
                boolean started = camera.isOpened();
                if (started) {
                    camera.addCameraListener(new CameraListener() {
                        @Override
                        public void onCameraClosed() {
                            super.onCameraClosed();
                            camera.removeCameraListener(this);
                            camera.setEngine((Engine) value);
                            camera.open();
                        }
                    });
                    camera.close();
                } else {
                    camera.setEngine((Engine) value);
                }
                break;
            case PREVIEW:
                boolean opened = camera.isOpened();
                if (opened) {
                    camera.addCameraListener(new CameraListener() {
                        @Override
                        public void onCameraClosed() {
                            super.onCameraClosed();
                            camera.removeCameraListener(this);
                            applyPreview(camera, (Preview) value, true);
                        }
                    });
                    camera.close();
                } else {
                    applyPreview(camera, (Preview) value, false);
                }
        }
    }

    // This is really tricky since the preview can only be changed when not attached to window.
    private void applyPreview(@NonNull CameraView cameraView, @NonNull Preview newPreview, boolean openWhenDone) {
        ViewGroup.LayoutParams params = cameraView.getLayoutParams();
        ViewGroup parent = (ViewGroup) cameraView.getParent();
        int index = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) == cameraView) {
                index = i;
                break;
            }
        }
        parent.removeView(cameraView);
        cameraView.setPreview(newPreview);
        parent.addView(cameraView, index, params);
        if (openWhenDone) cameraView.open();
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
