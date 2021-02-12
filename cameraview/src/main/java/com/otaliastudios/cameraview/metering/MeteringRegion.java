package com.otaliastudios.cameraview.metering;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.size.Size;

class MeteringRegion implements Comparable<MeteringRegion> {

    final static int MAX_WEIGHT = 1000;

    final RectF mRegion;
    final int mWeight;

    MeteringRegion(@NonNull RectF region, int weight) {
        mRegion = region;
        mWeight = weight;
    }

    @NonNull
    MeteringRegion transform(@NonNull MeteringTransform transform) {
        RectF result = new RectF(Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE);
        PointF point = new PointF();
        // top-left
        point.set(mRegion.left, mRegion.top);
        point = transform.transformMeteringPoint(point);
        updateRect(result, point);
        // top-right
        point.set(mRegion.right, mRegion.top);
        point = transform.transformMeteringPoint(point);
        updateRect(result, point);
        // bottom-right
        point.set(mRegion.right, mRegion.bottom);
        point = transform.transformMeteringPoint(point);
        updateRect(result, point);
        // bottom-left
        point.set(mRegion.left, mRegion.bottom);
        point = transform.transformMeteringPoint(point);
        updateRect(result, point);
        return new MeteringRegion(result, mWeight);
    }

    private void updateRect(@NonNull RectF rect, @NonNull PointF point) {
        rect.left = Math.min(rect.left, point.x);
        rect.top = Math.min(rect.top, point.y);
        rect.right = Math.max(rect.right, point.x);
        rect.bottom = Math.max(rect.bottom, point.y);
    }

    @NonNull
    MeteringRegion clip(@NonNull Size bounds) {
        return clip(new RectF(0, 0, bounds.getWidth(), bounds.getHeight()));
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    MeteringRegion clip(@NonNull RectF bounds) {
        RectF region = new RectF();
        region.set(
                Math.max(bounds.left, mRegion.left),
                Math.max(bounds.top, mRegion.top),
                Math.min(bounds.right, mRegion.right),
                Math.min(bounds.bottom, mRegion.bottom)
        );
        return new MeteringRegion(region, mWeight);
    }

    @Override
    public int compareTo(@NonNull MeteringRegion o) {
        //noinspection UseCompareMethod
        return -Integer.valueOf(mWeight).compareTo(o.mWeight);
    }
}
