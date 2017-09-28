package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

// Fallback preview when hardware acceleration is off.
class SurfaceViewPreview extends Preview<SurfaceView, SurfaceHolder> {


    SurfaceViewPreview(Context context, ViewGroup parent, SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected SurfaceView onCreateView(Context context, ViewGroup parent) {
        final View root = View.inflate(context, R.layout.surface_view, parent); // MATCH_PARENT
        SurfaceView surface = root.findViewById(R.id.surface_view);
        final SurfaceHolder holder = surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {

            private boolean mFirstTime = true;

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Looks like this is too early to call anything.
                // onSurfaceAvailable(getView().getWidth(), getView().getHeight());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mFirstTime) {
                    onSurfaceAvailable(width, height);
                    mFirstTime = false;
                } else {
                    onSurfaceSizeChanged(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                onSurfaceDestroyed();
                mFirstTime = true;
            }
        });
        return surface;
    }

    @Override
    Surface getSurface() {
        return getOutput().getSurface();
    }

    @Override
    SurfaceHolder getOutput() {
        return getView().getHolder();
    }

    @Override
    Class<SurfaceHolder> getOutputClass() {
        return SurfaceHolder.class;
    }

}
