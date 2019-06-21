package com.otaliastudios.cameraview.video;


import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.video.VideoRecorder;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class VideoRecorderTest extends BaseTest {

    @Test
    public void testRecorder() throws Exception {
        VideoResult.Stub result = createStub();
        VideoRecorder.VideoResultListener listener = Mockito.mock(VideoRecorder.VideoResultListener.class);
        VideoRecorder recorder = new VideoRecorder(result, listener) {
            public void start() {}
            public void stop() {
                dispatchResult();
            }
        };
        recorder.start();
        recorder.stop();
        Mockito.verify(listener, Mockito.times(1)).onVideoResult(result, null);
        assertNull(recorder.mListener);
        assertNull(recorder.mResult);
    }

    private VideoResult.Stub createStub() throws Exception {
        Constructor<VideoResult.Stub> constructor = VideoResult.Stub.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
