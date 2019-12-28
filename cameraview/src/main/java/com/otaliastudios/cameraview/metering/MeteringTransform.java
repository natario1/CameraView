package com.otaliastudios.cameraview.metering;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;

public interface MeteringTransform<T> {
    @NonNull
    PointF transformMeteringPoint(@NonNull PointF point);

    @NonNull
    T transformMeteringRegion(@NonNull RectF region, int weight);
}
