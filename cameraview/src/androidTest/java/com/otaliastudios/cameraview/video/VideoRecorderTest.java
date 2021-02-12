package com.otaliastudios.cameraview.video;


import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.VideoResult;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class VideoRecorderTest extends BaseTest {

    @Test
    public void testRecorder() throws Exception {
        VideoResult.Stub result = createStub();
        VideoRecorder.VideoResultListener listener = Mockito.mock(VideoRecorder.VideoResultListener.class);
        VideoRecorder recorder = new VideoRecorder(listener) {
            @Override
            protected void onStart() {
                dispatchVideoRecordingStart();
            }

            @Override
            protected void onStop(boolean isCameraShutdown) {
                dispatchVideoRecordingEnd();
                dispatchResult();
            }
        };
        recorder.start(result);
        Mockito.verify(listener,Mockito.times(1) )
                .onVideoRecordingStart();
        recorder.stop(false);
        Mockito.verify(listener, Mockito.times(1))
                .onVideoRecordingEnd();
        Mockito.verify(listener, Mockito.times(1))
                .onVideoResult(result, null);
    }

    private VideoResult.Stub createStub() throws Exception {
        Constructor<VideoResult.Stub> constructor = VideoResult.Stub.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
