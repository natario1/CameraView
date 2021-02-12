package com.otaliastudios.cameraview;


import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.otaliastudios.cameraview.size.Size;

import static android.view.ViewGroup.LayoutParams.*;

public class TestActivity extends Activity {

    private ViewGroup content;
    private Size contentSize = new Size(1000, 1000);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        wakeScreen();

        // Match parent decor view.
        FrameLayout root = new FrameLayout(this);
        root.setKeepScreenOn(true);
        root.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // Inner content view with fixed size.
        // We want it to be fully visible or expresso will crash.
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int width = Math.min(size.x, size.y);
        int height = Math.min(size.x, size.y);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height, Gravity.CENTER);
        content = new FrameLayout(this);
        content.setLayoutParams(params);
        contentSize = new Size(width, height);

        // Add.
        root.addView(content);
        setContentView(root);
    }

    public void wakeScreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public Size getContentSize() {
        return contentSize;
    }

    public ViewGroup getContentView() {
        return content;
    }

    public void inflate(View child) {
        inflate(child, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    public void inflate(View child, ViewGroup.LayoutParams params) {
        content.addView(child, params);
        content.requestLayout();
    }
}
