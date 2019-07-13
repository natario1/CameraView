package com.otaliastudios.cameraview.overlay;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

public interface Overlay {
    void drawOnPicture(@NonNull Canvas canvas);
    void drawOnVideo(@NonNull Canvas canvas);
}
