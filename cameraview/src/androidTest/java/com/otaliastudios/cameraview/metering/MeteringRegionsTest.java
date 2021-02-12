package com.otaliastudios.cameraview.metering;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.R;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;
import com.otaliastudios.cameraview.markers.AutoFocusTrigger;
import com.otaliastudios.cameraview.markers.MarkerParser;
import com.otaliastudios.cameraview.size.Size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class MeteringRegionsTest extends BaseTest {

    private final Size bounds = new Size(1000, 1000);

    private void checkRegion(@NonNull MeteringRegion region, @NonNull PointF center, int weight) {
        assertEquals(center.x, region.mRegion.centerX(), 0.01F);
        assertEquals(center.y, region.mRegion.centerY(), 0.01F);
        assertEquals(weight, region.mWeight);
    }

    @Test
    public void testFromPoint() {
        PointF center = new PointF(500, 500);
        MeteringRegions regions = MeteringRegions.fromPoint(bounds, center);
        assertEquals(2, regions.mRegions.size());
        MeteringRegion first = regions.mRegions.get(0);
        MeteringRegion second = regions.mRegions.get(1);
        checkRegion(first, center, MeteringRegion.MAX_WEIGHT);
        checkRegion(second, center,
                Math.round(MeteringRegions.BLUR_FACTOR_WEIGHT * MeteringRegion.MAX_WEIGHT));
    }

    @Test
    public void testFromArea() {
        RectF area = new RectF(400, 400, 600, 600);
        MeteringRegions regions = MeteringRegions.fromArea(bounds, area);
        assertEquals(1, regions.mRegions.size());
        MeteringRegion region = regions.mRegions.get(0);
        checkRegion(region, new PointF(area.centerX(), area.centerY()), MeteringRegion.MAX_WEIGHT);
    }

    @Test
    public void testFromArea_withBlur() {
        RectF area = new RectF(400, 400, 600, 600);
        MeteringRegions regions = MeteringRegions.fromArea(bounds, area,
                MeteringRegion.MAX_WEIGHT, true);
        assertEquals(2, regions.mRegions.size());
        MeteringRegion first = regions.mRegions.get(0);
        MeteringRegion second = regions.mRegions.get(1);
        PointF center = new PointF(area.centerX(), area.centerY());
        checkRegion(first, center, MeteringRegion.MAX_WEIGHT);
        checkRegion(second, center,
                Math.round(MeteringRegions.BLUR_FACTOR_WEIGHT * MeteringRegion.MAX_WEIGHT));
    }

    @Test
    public void testTransform() {
        MeteringTransform transform = mock(MeteringTransform.class);
        when(transform.transformMeteringPoint(any(PointF.class))).then(new Answer<PointF>() {
            @Override
            public PointF answer(InvocationOnMock invocation) {
                PointF in = invocation.getArgument(0);
                // This will swap x and y coordinates
                //noinspection SuspiciousNameCombination
                return new PointF(in.y, in.x);
            }
        });
        RectF area = new RectF(0, 0, 100, 500); // tall area
        RectF expected = new RectF(0, 0, 500, 100); // wide area
        MeteringRegions regions = MeteringRegions.fromArea(bounds, area);
        MeteringRegions transformed = regions.transform(transform);
        verify(transform, times(4)).transformMeteringPoint(any(PointF.class));
        assertEquals(1, transformed.mRegions.size());
        assertEquals(expected, transformed.mRegions.get(0).mRegion);
    }


    @Test
    public void testGet() {
        MeteringTransform<Integer> transform = new MeteringTransform<Integer>() {
            @NonNull
            @Override
            public PointF transformMeteringPoint(@NonNull PointF point) {
                return point;
            }

            @NonNull
            @Override
            public Integer transformMeteringRegion(@NonNull RectF region, int weight) {
                return weight;
            }
        };
        MeteringRegions regions = MeteringRegions.fromArea(bounds,
                new RectF(400, 400, 600, 600),
                900,
                true);
        assertEquals(2, regions.mRegions.size());
        List<Integer> result = regions.get(1, transform);
        assertEquals(1, result.size());
        assertEquals(900, (int) result.get(0));
    }

}
