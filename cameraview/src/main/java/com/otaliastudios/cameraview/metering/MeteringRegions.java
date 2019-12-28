package com.otaliastudios.cameraview.metering;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeteringRegions {
    private final static float POINT_AREA = 0.05F;

    @VisibleForTesting
    final static float BLUR_FACTOR_WEIGHT = 0.1F;
    private final static float BLUR_FACTOR_SIZE = 1.5F;

    @VisibleForTesting
    final List<MeteringRegion> mRegions;

    private MeteringRegions(@NonNull List<MeteringRegion> regions) {
        mRegions = regions;
    }

    @NonNull
    public MeteringRegions transform(@NonNull MeteringTransform transform) {
        List<MeteringRegion> regions = new ArrayList<>();
        for (MeteringRegion region : mRegions) {
            regions.add(region.transform(transform));
        }
        return new MeteringRegions(regions);
    }

    @NonNull
    public <T> List<T> get(int atMost, @NonNull MeteringTransform<T> transform) {
        List<T> result = new ArrayList<>();
        Collections.sort(mRegions);
        for (MeteringRegion region : mRegions) {
            result.add(transform.transformMeteringRegion(region.mRegion, region.mWeight));
        }
        atMost = Math.min(atMost, result.size());
        return result.subList(0, atMost);
    }

    @NonNull
    public static MeteringRegions fromPoint(@NonNull Size bounds,
                                            @NonNull PointF point) {
        return fromPoint(bounds, point, MeteringRegion.MAX_WEIGHT);
    }

    @NonNull
    public static MeteringRegions fromPoint(@NonNull Size bounds,
                                            @NonNull PointF point,
                                            int weight) {
        float width = POINT_AREA * bounds.getWidth();
        float height = POINT_AREA * bounds.getHeight();
        RectF rectF = expand(point, width, height);
        return fromArea(bounds, rectF, weight, true);
    }

    @NonNull
    public static MeteringRegions fromArea(@NonNull Size bounds,
                                           @NonNull RectF area) {
        return fromArea(bounds, area, MeteringRegion.MAX_WEIGHT);
    }

    @NonNull
    public static MeteringRegions fromArea(@NonNull Size bounds,
                                           @NonNull RectF area,
                                           int weight) {
        return fromArea(bounds, area, weight, false);
    }

    @NonNull
    public static MeteringRegions fromArea(@NonNull Size bounds,
                                           @NonNull RectF area,
                                           int weight,
                                           boolean blur) {
        List<MeteringRegion> regions = new ArrayList<>();
        final PointF center = new PointF(area.centerX(), area.centerY());
        final float width = area.width();
        final float height = area.height();
        regions.add(new MeteringRegion(area, weight));
        if (blur) {
            RectF background = expand(center,
                    BLUR_FACTOR_SIZE * width,
                    BLUR_FACTOR_SIZE * height);
            regions.add(new MeteringRegion(background,
                    Math.round(BLUR_FACTOR_WEIGHT * weight)));
        }
        List<MeteringRegion> clipped = new ArrayList<>();
        for (MeteringRegion region : regions) {
            clipped.add(region.clip(bounds));
        }
        return new MeteringRegions(clipped);
    }

    @NonNull
    private static RectF expand(@NonNull PointF center, float width, float height) {
        return new RectF(
                center.x - width / 2F,
                center.y - height / 2F,
                center.x + width / 2F,
                center.y + height / 2F
        );
    }

}
