package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
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

    SurfaceCameraPreview(Context context, ViewGroup parent, SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected SurfaceView onCreateView(Context context, ViewGroup parent) {
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
                    dispatchOnOutputSurfaceAvailable(width, height);
                    mDispatched = true;
                } else {
                    dispatchOnOutputSurfaceSizeChanged(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LOG.i("callback:", "surfaceDestroyed");
                dispatchOnOutputSurfaceDestroyed();
                mDispatched = false;
            }
        });
        return surfaceView;
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
