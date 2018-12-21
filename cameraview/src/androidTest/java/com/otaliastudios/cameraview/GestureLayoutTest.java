package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.content.Context;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.Root;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.View;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;

import static androidx.test.espresso.Espresso.onView;

@TargetApi(17)
public abstract class GestureLayoutTest<T extends GestureLayout> extends BaseTest {

    protected abstract T create(Context context);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @SuppressWarnings("WeakerAccess")
    protected T layout;
    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    protected final ViewInteraction onLayout() {
        return onView(Matchers.<View>is(layout))
                .inRoot(RootMatchers.withDecorView(
                        Matchers.is(rule.getActivity().getWindow().getDecorView())));
    }
}
