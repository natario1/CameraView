package com.otaliastudios.cameraview;


import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import static android.view.ViewGroup.LayoutParams.*;

public class TestActivity extends Activity {

    private ViewGroup content;
    private Size contentSize = new Size(1000, 1000);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match parent decor view.
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // Inner content view with fixed size.
        content = new FrameLayout(this);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                contentSize.getWidth(), contentSize.getHeight()));

        // Add.
        root.addView(content);
        setContentView(root);
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
