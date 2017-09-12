package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.content.Context;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.Root;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.View;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;

import static android.support.test.espresso.Espresso.onView;
import static org.hamcrest.Matchers.any;

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
            }
        });
    }

    protected final ViewInteraction onLayout() {
        return onView(Matchers.<View>is(layout))
                .inRoot(RootMatchers.withDecorView(
                        Matchers.is(rule.getActivity().getWindow().getDecorView())));
    }
}
