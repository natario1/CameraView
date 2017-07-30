package com.flurgle.camerakit;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

@TargetApi(14)
class TextureViewPreview extends PreviewImpl {

    private final TextureView mTextureView;

    TextureViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.texture_view, parent); // MATCH_PARENT
        mTextureView = (TextureView) view.findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setSurfaceSize(width, height);
                dispatchSurfaceChanged();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                setSurfaceSize(width, height);
                dispatchSurfaceChanged();
                // refreshScale();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setSurfaceSize(0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @Override
    Surface getSurface() {
        return new Surface(mTextureView.getSurfaceTexture());
    }

    @Override
    View getView() {
        return mTextureView;
    }

    @Override
    Class getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    boolean isReady() {
        return mTextureView.getSurfaceTexture() != null;
    }

    @Override
    SurfaceTexture getSurfaceTexture() {
        return mTextureView.getSurfaceTexture();
    }

    @TargetApi(15)
    @Override
    void setDesiredSize(int width, int height) {
        super.setDesiredSize(width, height);
        if (mTextureView.getSurfaceTexture() != null) {
            mTextureView.getSurfaceTexture().setDefaultBufferSize(width, height);
        }
    }

}
