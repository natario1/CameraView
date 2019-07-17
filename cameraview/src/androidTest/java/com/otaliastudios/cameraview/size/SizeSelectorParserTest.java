package com.otaliastudios.cameraview.size;


import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.arch.core.util.Function;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class SizeSelectorParserTest extends BaseTest {

    private MockTypedArray input;
    private List<Size> sizes = Arrays.asList(
            new Size(100, 200),
            new Size(150, 300),
            new Size(600, 900),
            new Size(600, 600),
            new Size(1600, 900),
            new Size(30, 40),
            new Size(40, 30),
            new Size(2000, 4000)
    );

    @Before
    public void setUp() {
        input = new MockTypedArray();
    }

    @After
    public void tearDown() {
        input = null;
    }

    private void doAssert(@NonNull Function<List<Size>, Void> assertions) {
        SizeSelectorParser parser = new SizeSelectorParser(input.array);
        assertions.apply(parser.getPictureSizeSelector().select(sizes));
        assertions.apply(parser.getVideoSizeSelector().select(sizes));
    }

    @Test
    public void testWidth() {
        input.setMinWidth(1500);
        input.setMaxWidth(1700);
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(1, input.size());
                assertEquals(new Size(1600, 900), input.get(0));
                return null;
            }
        });
    }

    @Test
    public void testHeight() {
        input.setMinHeight(25);
        input.setMaxHeight(35);
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(1, input.size());
                assertEquals(new Size(40, 30), input.get(0));
                return null;
            }
        });
    }

    @Test
    public void testArea() {
        input.setMinArea(30 * 30);
        input.setMaxArea(40 * 40);
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(2, input.size());
                assertTrue(input.contains(new Size(40, 30)));
                assertTrue(input.contains(new Size(30, 40)));
                return null;
            }
        });
    }

    @Test
    public void testSmallest() {
        input.setSmallest(true);
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(sizes.size(), input.size());
                Size first = input.get(0);
                assertEquals(30 * 40, first.getWidth() * first.getHeight());
                return null;
            }
        });
    }

    @Test
    public void testBiggest() {
        input.setBiggest(true);
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(sizes.size(), input.size());
                assertEquals(new Size(2000, 4000), input.get(0));
                return null;
            }
        });
    }

    @Test
    public void testAspectRatio() {
        input.setAspectRatio("16:9");
        doAssert(new Function<List<Size>, Void>() {
            @Override
            public Void apply(List<Size> input) {
                assertEquals(1, input.size());
                assertEquals(new Size(1600, 900), input.get(0));
                return null;
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private class MockTypedArray {
        private TypedArray array = mock(TypedArray.class);

        private void setIntValue(@StyleableRes int index, int value) {
            when(array.hasValue(index)).thenReturn(true);
            when(array.getInteger(eq(index), anyInt())).thenReturn(value);
        }

        private void setBooleanValue(@StyleableRes int index, boolean value) {
            when(array.hasValue(index)).thenReturn(true);
            when(array.getBoolean(eq(index), anyBoolean())).thenReturn(value);
        }

        private void setStringValue(@StyleableRes int index, @NonNull String value) {
            when(array.hasValue(index)).thenReturn(true);
            when(array.getString(index)).thenReturn(value);
        }

        private void setMinWidth(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMinWidth, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMinWidth, value);
        }

        private void setMaxWidth(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMaxWidth, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMaxWidth, value);
        }

        private void setMinHeight(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMinHeight, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMinHeight, value);
        }

        private void setMaxHeight(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMaxHeight, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMaxHeight, value);
        }

        private void setMinArea(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMinArea, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMinArea, value);
        }

        private void setMaxArea(int value) {
            setIntValue(R.styleable.CameraView_cameraPictureSizeMaxArea, value);
            setIntValue(R.styleable.CameraView_cameraVideoSizeMaxArea, value);
        }

        private void setSmallest(boolean value) {
            setBooleanValue(R.styleable.CameraView_cameraPictureSizeSmallest, value);
            setBooleanValue(R.styleable.CameraView_cameraVideoSizeSmallest, value);
        }

        private void setBiggest(boolean value) {
            setBooleanValue(R.styleable.CameraView_cameraPictureSizeBiggest, value);
            setBooleanValue(R.styleable.CameraView_cameraVideoSizeBiggest, value);
        }

        private void setAspectRatio(@NonNull String value) {
            setStringValue(R.styleable.CameraView_cameraPictureSizeAspectRatio, value);
            setStringValue(R.styleable.CameraView_cameraVideoSizeAspectRatio, value);
        }
    }
}
