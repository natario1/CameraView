package com.otaliastudios.cameraview;


import android.location.Location;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class VideoRecorderTest extends BaseTest {

    @Test
    public void testRecorder() {
        VideoResult result = new VideoResult();
        VideoRecorder.VideoResultListener listener = Mockito.mock(VideoRecorder.VideoResultListener.class);
        VideoRecorder recorder = new VideoRecorder(result, listener) {
            void start() {}
        };
        recorder.start();
        recorder.stop();
        Mockito.verify(listener, Mockito.times(1)).onVideoResult(result);
        assertNull(recorder.mListener);
        assertNull(recorder.mResult);
    }
}
