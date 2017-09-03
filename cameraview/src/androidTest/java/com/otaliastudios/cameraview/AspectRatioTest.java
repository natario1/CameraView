package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Parcel;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.SizeF;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AspectRatioTest {

    @Test
    public void testConstructor() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        assertEquals(ratio.getX(), 5f, 0);
        assertEquals(ratio.getY(), 1f, 0);
        assertEquals(ratio.toString(), "5:1");
    }

    @Test
    public void testEquals() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        AspectRatio ratio1 = AspectRatio.of(5, 1);
        assertTrue(ratio.equals(ratio1));

        Size size = new Size(500, 100);
        assertTrue(ratio.matches(size));
    }

    @Test
    public void testInverse() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        AspectRatio inverse = ratio.inverse();
        assertEquals(inverse.getX(), 1f, 0);
        assertEquals(inverse.getY(), 5f, 0);
    }

    @Test
    public void testParcelable() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        Parcel parcel = Parcel.obtain();
        ratio.writeToParcel(parcel, ratio.describeContents());

        parcel.setDataPosition(0);
        AspectRatio other = AspectRatio.CREATOR.createFromParcel(parcel);
        assertEquals(ratio, other);
    }

}
