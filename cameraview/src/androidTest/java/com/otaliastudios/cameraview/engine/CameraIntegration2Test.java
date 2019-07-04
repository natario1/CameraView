package com.otaliastudios.cameraview.engine;

import com.otaliastudios.cameraview.controls.Engine;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CameraIntegration2Test extends CameraIntegrationTest {

    @NonNull
    @Override
    protected Engine getEngine() {
        return Engine.CAMERA2;
    }
}
