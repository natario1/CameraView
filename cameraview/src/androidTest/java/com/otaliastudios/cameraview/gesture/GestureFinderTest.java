package com.otaliastudios.cameraview.gesture;


import android.annotation.TargetApi;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.tools.Op;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;

import static androidx.test.espresso.Espresso.onView;

@TargetApi(17)
public abstract class GestureFinderTest<T extends GestureFinder> extends BaseTest {

    protected abstract T createFinder(@NonNull GestureFinder.Controller controller);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @SuppressWarnings("WeakerAccess")
    protected T finder;
    @SuppressWarnings("WeakerAccess")
    protected Op<Gesture> touchOp;
    @SuppressWarnings("WeakerAccess")
    protected ViewGroup layout;

    @Before
    public void setUp() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                layout = new FrameLayout(a);
                finder = createFinder(new Controller());
                finder.setActive(true);
                a.inflate(layout);

                touchOp = new Op<>(false);
                layout.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        boolean found = finder.onTouchEvent(motionEvent);
                        if (found) touchOp.controller().end(finder.getGesture());
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

    private class Controller implements GestureFinder.Controller {

        @NonNull
        @Override
        public Context getContext() {
            return layout.getContext();
        }

        @Override
        public int getWidth() {
            return layout.getWidth();
        }

        @Override
        public int getHeight() {
            return layout.getHeight();
        }
    }
}
