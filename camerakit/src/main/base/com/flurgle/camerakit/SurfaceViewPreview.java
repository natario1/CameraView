package com.flurgle.camerakit;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

// This is not used.
class SurfaceViewPreview extends PreviewImpl {

    private final SurfaceView mSurfaceView;

    SurfaceViewPreview(Context context, ViewGroup parent) {
        super(context, parent);
        final View view = View.inflate(context, R.layout.surface_view, parent); // MATCH_PARENT
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        final SurfaceHolder holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                onSurfaceAvailable(mSurfaceView.getWidth(), mSurfaceView.getHeight());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                onSurfaceSizeChanged(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                onSurfaceDestroyed();
            }
        });
    }

    @Override
    Surface getSurface() {
        return getSurfaceHolder().getSurface();
    }

    @Override
    View getView() {
        return mSurfaceView;
    }

    @Override
    SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    @Override
    Class getOutputClass() {
        return SurfaceHolder.class;
    }

    @Override
    boolean isReady() {
        return mSurfaceView.getWidth() != 0 && mSurfaceView.getHeight() != 0;
    }

}
