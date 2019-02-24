package com.otaliastudios.cameraview;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

// Fallback preview when hardware acceleration is off.
// Currently this does NOT support cropping (e. g. the crop inside behavior),
// so we return false in supportsCropping() in order to have proper measuring.
// This means that CameraView is forced to be wrap_content.
class SurfaceCameraPreview extends CameraPreview<SurfaceView, SurfaceHolder> {

    private final static CameraLogger LOG = CameraLogger.create(SurfaceCameraPreview.class.getSimpleName());

    private boolean mDispatched;
    private View mRootView;

    SurfaceCameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected SurfaceView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_surface_view, parent, false);
        parent.addView(root, 0);
        SurfaceView surfaceView = root.findViewById(R.id.surface_view);
        final SurfaceHolder holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // This is too early to call anything.
                // surfaceChanged is guaranteed to be called after, with exact dimensions.
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LOG.i("callback:", "surfaceChanged", "w:", width, "h:", height, "dispatched:", mDispatched);
                if (!mDispatched) {
                    dispatchOnSurfaceAvailable(width, height);
                    mDispatched = true;
                } else {
                    dispatchOnSurfaceSizeChanged(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LOG.i("callback:", "surfaceDestroyed");
                dispatchOnSurfaceDestroyed();
                mDispatched = false;
            }
        });
        mRootView = root;
        return surfaceView;
    }

    @NonNull
    @Override
    View getRootView() {
        return mRootView;
    }

    @NonNull
    @Override
    SurfaceHolder getOutput() {
        return getView().getHolder();
    }

    @NonNull
    @Override
    Class<SurfaceHolder> getOutputClass() {
        return SurfaceHolder.class;
    }


}
