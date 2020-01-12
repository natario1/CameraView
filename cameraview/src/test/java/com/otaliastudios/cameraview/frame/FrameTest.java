package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FrameTest {

    private FrameManager<String> manager;

    @Before
    public void setUp() {
        manager = spy(new FrameManager<String>(1, String.class) {
            @Override
            protected void onFrameDataReleased(@NonNull String data, boolean recycled) { }

            @NonNull
            @Override
            protected String onCloneFrameData(@NonNull String data) {
                return data;
            }
        });
    }

    @After
    public void tearDown() {
        manager = null;
    }

    @Test
    public void testEquals() {
        // Only time should count.
        Frame f1 = new Frame(manager);
        long time = 1000;
        f1.setContent("foo", time, 90, 180, new Size(5, 5), ImageFormat.NV21);
        Frame f2 = new Frame(manager);
        f2.setContent("bar", time, 0, 90, new Size(10, 10), ImageFormat.NV21);
        assertEquals(f1, f2);

        f2.setContent("foo", time + 1, 0, 90, new Size(10, 10), ImageFormat.NV21);
        assertNotEquals(f1, f2);
    }

    @Test
    public void testReleaseThrows() {
        final Frame frame = new Frame(manager);
        frame.setContent("foo", 1000, 90, 90, new Size(10, 10), ImageFormat.NV21);
        frame.release();
        verify(manager, times(1)).onFrameReleased(frame, "foo");

        assertThrows(new Runnable() { public void run() { frame.getTime(); }});
        assertThrows(new Runnable() { public void run() { frame.getFormat(); }});
        assertThrows(new Runnable() { public void run() { frame.getRotation(); }});
        assertThrows(new Runnable() { public void run() { frame.getData(); }});
        assertThrows(new Runnable() { public void run() { frame.getSize(); }});
    }

    private void assertThrows(Runnable runnable) {
        try {
            runnable.run();
            throw new IllegalStateException("Expected an exception but found none.");
        } catch (Exception e) {
            // All good
        }
    }

    @Test
    public void testFreeze() {
        Frame frame = new Frame(manager);
        String data = "test data";
        long time = 1000;
        int userRotation = 90;
        int viewRotation = 90;
        Size size = new Size(10, 10);
        int format = ImageFormat.NV21;
        frame.setContent(data, time, userRotation, viewRotation, size, format);

        Frame frozen = frame.freeze();
        assertEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(userRotation, frozen.getRotationToUser());
        assertEquals(viewRotation, frozen.getRotationToView());
        assertEquals(size, frozen.getSize());

        // Mutate the first, ensure that frozen is not affected
        frame.setContent("new data", 50, 180, 180, new Size(1, 1), ImageFormat.JPEG);
        assertEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(userRotation, frozen.getRotationToUser());
        assertEquals(viewRotation, frozen.getRotationToView());
        assertEquals(size, frozen.getSize());
        assertEquals(format, frozen.getFormat());
    }

}
