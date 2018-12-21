package com.otaliastudios.cameraview;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class PictureRecorderTest extends BaseTest {

    @Test
    public void testRecorder() {
        PictureResult result = new PictureResult();
        PictureRecorder.PictureResultListener listener = Mockito.mock(PictureRecorder.PictureResultListener.class);
        PictureRecorder recorder = new PictureRecorder(result, listener) {
            void take() {
                dispatchResult();
            }
        };
        recorder.take();
        Mockito.verify(listener, Mockito.times(1)).onPictureResult(result);
        assertNull(recorder.mListener);
        assertNull(recorder.mResult);
    }
}
