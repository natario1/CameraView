package com.otaliastudios.cameraview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

class TextureCameraPreview extends CameraPreview<TextureView, SurfaceTexture> {

    private View mRootView;

    TextureCameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected TextureView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_texture_view, parent, false);
        parent.addView(root, 0);
        TextureView texture = root.findViewById(R.id.texture_view);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                dispatchOnSurfaceAvailable(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                dispatchOnSurfaceSizeChanged(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                dispatchOnSurfaceDestroyed();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        mRootView = root;
        return texture;
    }

    @NonNull
    @Override
    View getRootView() {
        return mRootView;
    }

    @NonNull
    @Override
    Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @NonNull
    @Override
    SurfaceTexture getOutput() {
        return getView().getSurfaceTexture();
    }

    @TargetApi(15)
    @Override
    void setStreamSize(int width, int height, boolean wasFlipped) {
        super.setStreamSize(width, height, wasFlipped);
        if (getView().getSurfaceTexture() != null) {
            getView().getSurfaceTexture().setDefaultBufferSize(width, height);
        }
    }

    @Override
    boolean supportsCropping() {
        return true;
    }

    @Override
    protected void crop() {
        mCropTask.start();
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (mInputStreamHeight == 0 || mInputStreamWidth == 0 ||
                        mOutputSurfaceHeight == 0 || mOutputSurfaceWidth == 0) {
                    mCropTask.end(null);
                    return;
                }
                float scaleX = 1f, scaleY = 1f;
                AspectRatio current = AspectRatio.of(mOutputSurfaceWidth, mOutputSurfaceHeight);
                AspectRatio target = AspectRatio.of(mInputStreamWidth, mInputStreamHeight);
                if (current.toFloat() >= target.toFloat()) {
                    // We are too short. Must increase height.
                    scaleY = current.toFloat() / target.toFloat();
                } else {
                    // We must increase width.
                    scaleX = target.toFloat() / current.toFloat();
                }

                getView().setScaleX(scaleX);
                getView().setScaleY(scaleY);

                mCropping = scaleX > 1.02f || scaleY > 1.02f;
                LOG.i("crop:", "applied scaleX=", scaleX);
                LOG.i("crop:", "applied scaleY=", scaleY);
                mCropTask.end(null);
            }
        });
    }
}
