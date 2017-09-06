package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;

@TargetApi(17)
public abstract class GestureLayoutTest<T extends GestureLayout> extends BaseTest {

    protected abstract T create(Context context);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    protected T layout;
    protected Task<Gesture> touch;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                layout = create(a);
                layout.enable(true);
                a.inflate(layout);

                touch = new Task<>();
                layout.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        boolean found = layout.onTouchEvent(motionEvent);
                        if (found) touch.end(layout.getGestureType());
                        return true;
                    }
                });
                layout.setId(View.generateViewId());
            }
        });
    }
}
