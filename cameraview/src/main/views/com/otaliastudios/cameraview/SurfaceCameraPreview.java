package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

// Fallback preview when hardware acceleration is off.
// Currently this does NOT support cropping (e. g. the crop inside behavior),
// so we return false in supportsCropping() in order to have proper measuring.
// This means that CameraView is forced to be wrap_content.
class SurfaceCameraPreview extends CameraPreview<View, SurfaceHolder> {

    private final static CameraLogger LOG = CameraLogger.create(SurfaceCameraPreview.class.getSimpleName());

    SurfaceCameraPreview(Context context, ViewGroup parent, SurfaceCallback callback) {
        super(context, parent, callback);
    }

    private SurfaceView mSurfaceView;

    @NonNull
    @Override
    protected View onCreateView(Context context, ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_surface_view, parent, false);
        parent.addView(root, 0);
        mSurfaceView = root.findViewById(R.id.surface_view);
        final SurfaceHolder holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {

            private boolean mFirstTime = true;

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Looks like this is too early to call anything.
                // surfaceChanged is guaranteed to be called after, with exact dimensions.
                LOG.i("callback:", "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LOG.i("callback:", "surfaceChanged", "w:", width, "h:", height, "firstTime:", mFirstTime);
                if (mFirstTime) {
                    onSurfaceAvailable(width, height);
                    mFirstTime = false;
                } else {
                    onSurfaceSizeChanged(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LOG.i("callback:", "surfaceDestroyed");
                onSurfaceDestroyed();
                mFirstTime = true;
            }
        });
        return root.findViewById(R.id.surface_view_root);
    }

    @Override
    Surface getSurface() {
        return getOutput().getSurface();
    }

    @Override
    SurfaceHolder getOutput() {
        return mSurfaceView.getHolder();
    }

    @Override
    Class<SurfaceHolder> getOutputClass() {
        return SurfaceHolder.class;
    }

    @Override
    boolean supportsCropping() {
        return false;
    }

    @Override
    protected void applyCrop(float scaleX, float scaleY) {
        /* float currWidth = getView().getWidth();
        float currHeight = getView().getHeight();
        float cropX = currWidth * (scaleX - 1) / 2f;
        float cropY = currHeight * (scaleX - 1) / 2f;
        LOG.e("applyCrop:", "currWidth:", currWidth, "currHeight:", currHeight);
        LOG.e("applyCrop:", "scaleX:", scaleX, "scaleY:", scaleY);
        mSurfaceView.setTop((int) -cropY);
        mSurfaceView.setLeft((int) -cropX);
        mSurfaceView.setBottom((int) (currHeight + cropY));
        mSurfaceView.setRight((int) (currWidth + cropX));
        LOG.e("applyCrop:", "top:", -cropY, "left:", -cropX, "bottom:", currHeight+cropY, "right:", currWidth+cropX);
        mSurfaceView.requestLayout(); */
        // mSurfaceView.getLayoutParams().width = (int) (currWidth * scaleX);
        // mSurfaceView.getLayoutParams().height = (int) (currHeight * scaleY);
        // mSurfaceView.setTranslationY(-cropY / 2f);
        // mSurfaceView.setTranslationX(-cropX / 2f);
        // getView().setScrollX((int) (cropX / 2f));
        // getView().setScrollY((int) (cropY / 2f));
        // mSurfaceView.requestLayout();
        // super.applyCrop(scaleX, scaleY);
    }
}
