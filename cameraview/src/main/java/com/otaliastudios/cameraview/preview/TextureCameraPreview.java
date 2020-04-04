package com.otaliastudios.cameraview.preview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.R;
import com.otaliastudios.cameraview.size.AspectRatio;

import java.util.concurrent.ExecutionException;

/**
 * A preview implementation based on {@link TextureView}.
 * Better than {@link SurfaceCameraPreview} but much less powerful than {@link GlCameraPreview}.
 */
public class TextureCameraPreview extends CameraPreview<TextureView, SurfaceTexture> {

    private View mRootView;

    public TextureCameraPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        super(context, parent);
    }

    @NonNull
    @Override
    protected TextureView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_texture_view, parent,
                false);
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
    public View getRootView() {
        return mRootView;
    }

    @NonNull
    @Override
    public Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @NonNull
    @Override
    public SurfaceTexture getOutput() {
        return getView().getSurfaceTexture();
    }

    @Override
    public boolean supportsCropping() {
        return true;
    }

    @Override
    protected void crop(@Nullable final CropCallback callback) {
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (mInputStreamHeight == 0 || mInputStreamWidth == 0 ||
                        mOutputSurfaceHeight == 0 || mOutputSurfaceWidth == 0) {
                    if (callback != null) callback.onCrop();
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
                if (callback != null) callback.onCrop();
            }
        });
    }

    @Override
    public void setDrawRotation(final int drawRotation) {
        super.setDrawRotation(drawRotation);
        final TaskCompletionSource<Void> task = new TaskCompletionSource<>();
        getView().post(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                // Output surface coordinates
                float outputCenterX = mOutputSurfaceWidth / 2F;
                float outputCenterY = mOutputSurfaceHeight / 2F;
                boolean flip = drawRotation % 180 != 0;
                // If dimensions are swapped, we must also do extra work to flip
                // the two dimensions, using the view width and height (to support cropping).
                if (flip) {
                    float scaleX = (float) mOutputSurfaceHeight / mOutputSurfaceWidth;
                    matrix.postScale(scaleX, 1F / scaleX, outputCenterX, outputCenterY);
                }
                matrix.postRotate((float) drawRotation, outputCenterX, outputCenterY);
                getView().setTransform(matrix);
                task.setResult(null);
            }
        });
        try {
            Tasks.await(task.getTask());
        } catch (InterruptedException | ExecutionException ignore) { }
    }
}
