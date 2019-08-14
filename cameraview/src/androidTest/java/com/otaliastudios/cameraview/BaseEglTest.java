package com.otaliastudios.cameraview;


import android.graphics.Canvas;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.internal.egl.EglBaseSurface;
import com.otaliastudios.cameraview.internal.egl.EglCore;
import com.otaliastudios.cameraview.internal.egl.EglViewport;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.overlay.OverlayDrawer;
import com.otaliastudios.cameraview.size.Size;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SuppressWarnings("WeakerAccess")
public abstract class BaseEglTest extends BaseTest {

    protected final static int WIDTH = 100;
    protected final static int HEIGHT = 100;

    protected EglCore eglCore;
    protected EglBaseSurface eglSurface;

    @Before
    public void setUp() {
        eglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
        eglSurface = new EglBaseSurface(eglCore);
        eglSurface.createOffscreenSurface(WIDTH, HEIGHT);
        eglSurface.makeCurrent();
    }

    @After
    public void tearDown() {
        eglSurface.releaseEglSurface();
        eglSurface = null;
        eglCore.release();
        eglCore = null;
    }
}
