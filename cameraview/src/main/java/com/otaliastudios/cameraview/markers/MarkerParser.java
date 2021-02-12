package com.otaliastudios.cameraview.markers;

import android.content.Context;
import android.content.res.TypedArray;

import com.otaliastudios.cameraview.R;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parses markers from XML attributes.
 */
public class MarkerParser {

    private AutoFocusMarker autoFocusMarker = null;

    public MarkerParser(@NonNull TypedArray array) {
        String autoFocusName = array.getString(R.styleable.CameraView_cameraAutoFocusMarker);
        if (autoFocusName != null) {
            try {
                Class<?> autoFocusClass = Class.forName(autoFocusName);
                autoFocusMarker = (AutoFocusMarker) autoFocusClass.newInstance();
            } catch (Exception ignore) { }
        }
    }

    @Nullable
    public AutoFocusMarker getAutoFocusMarker() {
        return autoFocusMarker;
    }
}
