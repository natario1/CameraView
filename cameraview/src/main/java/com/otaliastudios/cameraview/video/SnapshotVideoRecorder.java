package com.otaliastudios.cameraview.video;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.controls.AudioCodec;
import com.otaliastudios.cameraview.internal.DeviceEncoders;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.overlay.OverlayDrawer;
import com.otaliastudios.cameraview.preview.RendererCameraPreview;
import com.otaliastudios.cameraview.preview.RendererFrameCallback;
import com.otaliastudios.cameraview.preview.RendererThread;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.video.encoding.AudioConfig;
import com.otaliastudios.cameraview.video.encoding.AudioMediaEncoder;
import com.otaliastudios.cameraview.video.encoding.EncoderThread;
import com.otaliastudios.cameraview.video.encoding.MediaEncoderEngine;
import com.otaliastudios.cameraview.video.encoding.TextureConfig;
import com.otaliastudios.cameraview.video.encoding.TextureMediaEncoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaCodec} APIs.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SnapshotVideoRecorder extends VideoRecorder implements RendererFrameCallback,
        MediaEncoderEngine.Listener {

    private static final String TAG = SnapshotVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int DEFAULT_VIDEO_FRAMERATE = 30;
    private static final int DEFAULT_AUDIO_BITRATE = 64000;

    // https://stackoverflow.com/a/5220554/4288782
    // Assuming low motion, we don't want to put this too high for default usage,
    // advanced users are still free to change this for each video.
    private static int estimateVideoBitRate(@NonNull Size size, int frameRate) {
        return (int) (0.07F * 1F * size.getWidth() * size.getHeight() * frameRate);
    }

    private static final int STATE_RECORDING = 0;
    private static final int STATE_NOT_RECORDING = 1;

    private MediaEncoderEngine mEncoderEngine;
    private final Object mEncoderEngineLock = new Object();
    private RendererCameraPreview mPreview;

    private int mCurrentState = STATE_NOT_RECORDING;
    private int mDesiredState = STATE_NOT_RECORDING;
    private int mTextureId = 0;

    private Overlay mOverlay;
    private OverlayDrawer mOverlayDrawer;
    private boolean mHasOverlay;

    private Filter mCurrentFilter;

    public SnapshotVideoRecorder(@NonNull CameraEngine engine,
                                 @NonNull RendererCameraPreview preview,
                                 @Nullable Overlay overlay) {
        super(engine);
        mPreview = preview;
        mOverlay = overlay;
        mHasOverlay = overlay != null && overlay.drawsOn(Overlay.Target.VIDEO_SNAPSHOT);
    }

    @Override
    protected void onStart() {
        mPreview.addRendererFrameCallback(this);
        mDesiredState = STATE_RECORDING;
        dispatchVideoRecordingStart();
    }

    // Can be called different threads
    @Override
    protected void onStop(boolean isCameraShutdown) {
        if (isCameraShutdown) {
            // The renderer callback might never be called. From my tests, it's not,
            // so we can't wait for that callback to stop the encoder engine.
            LOG.i("Stopping the encoder engine from isCameraShutdown.");
            mDesiredState = STATE_NOT_RECORDING;
            mCurrentState = STATE_NOT_RECORDING;
            synchronized (mEncoderEngineLock) {
                if (mEncoderEngine != null) {
                    mEncoderEngine.stop();
                    mEncoderEngine = null;
                }
            }
        } else {
            mDesiredState = STATE_NOT_RECORDING;
        }
    }

    @RendererThread
    @Override
    public void onRendererTextureCreated(int textureId) {
        mTextureId = textureId;
        if (mHasOverlay) {
            mOverlayDrawer = new OverlayDrawer(mOverlay, mResult.size);
        }
    }

    @RendererThread
    @Override
    public void onRendererFilterChanged(@NonNull Filter filter) {
        mCurrentFilter = filter.copy();
        mCurrentFilter.setSize(mResult.size.getWidth(), mResult.size.getHeight());
        synchronized (mEncoderEngineLock) {
            if (mEncoderEngine != null) {
                mEncoderEngine.notify(TextureMediaEncoder.FILTER_EVENT, mCurrentFilter);
            }
        }
    }

    @RendererThread
    @Override
    public void onRendererFrame(@NonNull SurfaceTexture surfaceTexture, int rotation,
                                float scaleX, float scaleY) {
        if (mCurrentState == STATE_NOT_RECORDING && mDesiredState == STATE_RECORDING) {
            LOG.i("Starting the encoder engine.");

            // Set default options
            if (mResult.videoFrameRate <= 0) mResult.videoFrameRate = DEFAULT_VIDEO_FRAMERATE;
            if (mResult.videoBitRate <= 0) mResult.videoBitRate
                    = estimateVideoBitRate(mResult.size, mResult.videoFrameRate);
            if (mResult.audioBitRate <= 0) mResult.audioBitRate = DEFAULT_AUDIO_BITRATE;

            // Define mime types
            String videoType = "";
            switch (mResult.videoCodec) {
                case H_263: videoType = "video/3gpp"; break; // MediaFormat.MIMETYPE_VIDEO_H263;
                case H_264: videoType = "video/avc"; break; // MediaFormat.MIMETYPE_VIDEO_AVC:
                case DEVICE_DEFAULT: videoType = "video/avc"; break;
            }
            String audioType = "";
            switch (mResult.audioCodec) {
                case AAC:
                case HE_AAC:
                case AAC_ELD: audioType = "audio/mp4a-latm"; break; // MediaFormat.MIMETYPE_AUDIO_AAC:
                case DEVICE_DEFAULT: audioType = "audio/mp4a-latm"; break;

            }
            TextureConfig videoConfig = new TextureConfig();
            AudioConfig audioConfig = new AudioConfig();

            // See if we have audio
            int audioChannels = 0;
            if (mResult.audio == Audio.ON) {
                audioChannels = audioConfig.channels;
            } else if (mResult.audio == Audio.MONO) {
                audioChannels = 1;
            } else if (mResult.audio == Audio.STEREO) {
                audioChannels = 2;
            }
            boolean hasAudio = audioChannels > 0;

            // Check the availability of values
            Size newVideoSize = null;
            int newVideoBitRate = 0;
            int newAudioBitRate = 0;
            int newVideoFrameRate = 0;
            int videoEncoderOffset = 0;
            int audioEncoderOffset = 0;
            boolean encodersFound = false;
            DeviceEncoders deviceEncoders = null;
            while (!encodersFound) {
                LOG.i("Checking DeviceEncoders...",
                        "videoOffset:", videoEncoderOffset,
                        "audioOffset:", audioEncoderOffset);
                try {
                    deviceEncoders = new DeviceEncoders(DeviceEncoders.MODE_RESPECT_ORDER,
                            videoType, audioType, videoEncoderOffset, audioEncoderOffset);
                } catch (RuntimeException e) {
                    LOG.w("Could not respect encoders parameters.",
                            "Going on again without checking encoders, possibly failing.");
                    newVideoSize = mResult.size;
                    newVideoBitRate = mResult.videoBitRate;
                    newVideoFrameRate = mResult.videoFrameRate;
                    newAudioBitRate = mResult.audioBitRate;
                    break;
                }
                deviceEncoders = new DeviceEncoders(DeviceEncoders.MODE_PREFER_HARDWARE,
                        videoType, audioType, videoEncoderOffset, audioEncoderOffset);
                try {
                    newVideoSize = deviceEncoders.getSupportedVideoSize(mResult.size);
                    newVideoBitRate = deviceEncoders.getSupportedVideoBitRate(mResult.videoBitRate);
                    newVideoFrameRate = deviceEncoders.getSupportedVideoFrameRate(newVideoSize,
                            mResult.videoFrameRate);
                    deviceEncoders.tryConfigureVideo(videoType, newVideoSize, newVideoFrameRate,
                            newVideoBitRate);
                    if (hasAudio) {
                        newAudioBitRate = deviceEncoders
                                .getSupportedAudioBitRate(mResult.audioBitRate);
                        deviceEncoders.tryConfigureAudio(audioType, newAudioBitRate,
                                audioConfig.samplingFrequency, audioChannels);
                    }
                    encodersFound = true;
                } catch (DeviceEncoders.VideoException videoException) {
                    LOG.i("Got VideoException:", videoException.getMessage());
                    videoEncoderOffset++;
                } catch (DeviceEncoders.AudioException audioException) {
                    LOG.i("Got AudioException:", audioException.getMessage());
                    audioEncoderOffset++;
                }
            }
            mResult.size = newVideoSize;
            mResult.videoBitRate = newVideoBitRate;
            mResult.audioBitRate = newAudioBitRate;
            mResult.videoFrameRate = newVideoFrameRate;

            // Video
            videoConfig.width = mResult.size.getWidth();
            videoConfig.height = mResult.size.getHeight();
            videoConfig.bitRate = mResult.videoBitRate;
            videoConfig.frameRate = mResult.videoFrameRate;
            videoConfig.rotation = rotation + mResult.rotation;
            videoConfig.mimeType = videoType;
            videoConfig.encoder = deviceEncoders.getVideoEncoder();
            videoConfig.textureId = mTextureId;
            videoConfig.scaleX = scaleX;
            videoConfig.scaleY = scaleY;
            // Get egl context from the RendererThread, which is the one in which we have created
            // the textureId and the overlayTextureId, managed by the GlSurfaceView.
            // Next operations can then be performed on different threads using this handle.
            videoConfig.eglContext = EGL14.eglGetCurrentContext();
            if (mHasOverlay) {
                videoConfig.overlayTarget = Overlay.Target.VIDEO_SNAPSHOT;
                videoConfig.overlayDrawer = mOverlayDrawer;
                videoConfig.overlayRotation = mResult.rotation;
                // ^ no "rotation" here! Overlays are already in VIEW ref.
            }
            TextureMediaEncoder videoEncoder = new TextureMediaEncoder(videoConfig);

            // Adjustment
            mResult.rotation = 0; // We will rotate the result instead.
            mCurrentFilter.setSize(mResult.size.getWidth(), mResult.size.getWidth());

            // Audio
            AudioMediaEncoder audioEncoder = null;
            if (hasAudio) {
                audioConfig.bitRate = mResult.audioBitRate;
                audioConfig.channels = audioChannels;
                audioConfig.encoder = deviceEncoders.getAudioEncoder();
                audioEncoder = new AudioMediaEncoder(audioConfig);
            }

            // Engine
            synchronized (mEncoderEngineLock) {
                mEncoderEngine = new MediaEncoderEngine(mResult.file,
                        videoEncoder,
                        audioEncoder,
                        mResult.maxDuration,
                        mResult.maxSize,
                        SnapshotVideoRecorder.this);
                mEncoderEngine.notify(TextureMediaEncoder.FILTER_EVENT, mCurrentFilter);
                mEncoderEngine.start();
            }
            mCurrentState = STATE_RECORDING;
        }

        if (mCurrentState == STATE_RECORDING) {
            LOG.i("scheduling frame.");
            synchronized (mEncoderEngineLock) {
                if (mEncoderEngine != null) { // Can be null on teardown.
                    LOG.i("dispatching frame.");
                    TextureMediaEncoder textureEncoder
                            = (TextureMediaEncoder) mEncoderEngine.getVideoEncoder();
                    TextureMediaEncoder.Frame frame = textureEncoder.acquireFrame();
                    frame.timestampNanos = surfaceTexture.getTimestamp();
                    // NOTE: this is an approximation but it seems to work:
                    frame.timestampMillis = System.currentTimeMillis();
                    surfaceTexture.getTransformMatrix(frame.transform);
                    mEncoderEngine.notify(TextureMediaEncoder.FRAME_EVENT, frame);
                }
            }
        }

        if (mCurrentState == STATE_RECORDING && mDesiredState == STATE_NOT_RECORDING) {
            LOG.i("Stopping the encoder engine.");
            mCurrentState = STATE_NOT_RECORDING;
            synchronized (mEncoderEngineLock) {
                if (mEncoderEngine != null) {
                    mEncoderEngine.stop();
                    mEncoderEngine = null;
                }
            }
        }

    }

    @Override
    public void onEncodingStart() {
        // This would be the most correct place to call dispatchVideoRecordingStart. However,
        // after this we'll post the call on the UI thread which can take some time. To compensate
        // this, we call dispatchVideoRecordingStart() a bit earlier in this class (onStart()).
    }

    @Override
    public void onEncodingStop() {
        dispatchVideoRecordingEnd();
    }

    @EncoderThread
    @Override
    public void onEncodingEnd(int stopReason, @Nullable Exception e) {
        // If something failed, undo the result, since this is the mechanism
        // to notify Camera1Engine about this.
        if (e != null) {
            LOG.e("Error onEncodingEnd", e);
            mResult = null;
            mError = e;
        } else {
            if (stopReason == MediaEncoderEngine.END_BY_MAX_DURATION) {
                LOG.i("onEncodingEnd because of max duration.");
                mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
            } else if (stopReason == MediaEncoderEngine.END_BY_MAX_SIZE) {
                LOG.i("onEncodingEnd because of max size.");
                mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
            } else {
                LOG.i("onEncodingEnd because of user.");
            }
        }
        // Cleanup
        mCurrentState = STATE_NOT_RECORDING;
        mDesiredState = STATE_NOT_RECORDING;
        mPreview.removeRendererFrameCallback(SnapshotVideoRecorder.this);
        mPreview = null;
        if (mOverlayDrawer != null) {
            mOverlayDrawer.release();
            mOverlayDrawer = null;
        }
        synchronized (mEncoderEngineLock) {
            mEncoderEngine = null;
        }
        dispatchResult();
    }
}
