package com.otaliastudios.cameraview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

class TextureViewPreview extends Preview<TextureView, SurfaceTexture> {

    private Surface mSurface;

    TextureViewPreview(Context context, ViewGroup parent) {
        super(context, parent);
    }

    @NonNull
    @Override
    protected TextureView onCreateView(Context context, ViewGroup parent) {
        final View root = View.inflate(context, R.layout.texture_view, parent); // MATCH_PARENT
        TextureView texture = root.findViewById(R.id.texture_view);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                onSurfaceAvailable(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                onSurfaceSizeChanged(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                onSurfaceDestroyed();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        return texture;
    }

    @Override
    Surface getSurface() {
        if (mSurface == null) { // Check if valid?
            mSurface = new Surface(getOutput());
        }
        return mSurface;
    }

    @Override
    Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    boolean isReady() {
        return getOutput() != null;
    }

    @Override
    SurfaceTexture getOutput() {
        return getView().getSurfaceTexture();
    }

    @TargetApi(15)
    @Override
    void setDesiredSize(int width, int height) {
        super.setDesiredSize(width, height);
        if (getView().getSurfaceTexture() != null) {
            getView().getSurfaceTexture().setDefaultBufferSize(width, height);
        }
    }

}
