package com.otaliastudios.cameraview;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaRecorder} APIs.
 */
class MediaRecorderVideoRecorder extends VideoRecorder {

    private static final String TAG = MediaRecorderVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private MediaRecorder mMediaRecorder;
    private CamcorderProfile mProfile;
    private Mapper mMapper;

    MediaRecorderVideoRecorder(VideoResult stub, VideoResultListener listener, Camera camera, int cameraId) {
        super(stub, listener);
        mMapper = new Mapper1();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(camera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        // TODO: should get a profile of a quality compatible with the chosen size.
    }

    // Camera2 constructor here...

    @Override
    void start() {
        if (mResult.getAudio() == Audio.ON) {
            // Must be called before setOutputFormat.
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        Size size = mResult.getRotation() % 180 != 0 ? mResult.getSize().flip() : mResult.getSize();
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        if (mResult.getCodec() == VideoCodec.DEFAULT) {
            mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        } else {
            mMediaRecorder.setVideoEncoder(mMapper.map(mResult.getCodec()));
        }
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        if (mResult.getAudio() == Audio.ON) {
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
        }
        if (mResult.getLocation() != null) {
            mMediaRecorder.setLocation(
                    (float) mResult.getLocation().getLatitude(),
                    (float) mResult.getLocation().getLongitude());
        }
        mMediaRecorder.setOutputFile(mResult.getFile().getAbsolutePath());
        mMediaRecorder.setOrientationHint(mResult.getRotation());
        mMediaRecorder.setMaxFileSize(mResult.getMaxSize());
        mMediaRecorder.setMaxDuration(mResult.getMaxDuration());
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
                        stop();
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
                        stop();
                        break;
                }
            }
        });
        // Not needed. mMediaRecorder.setPreviewDisplay(mPreview.getSurface());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            mResult = null;
            stop();
        }
    }

    @Override
    void close() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                // This can happen if stopVideo() is called right after takeVideo(). We don't care.
                LOG.w("stop:", "Error while closing media recorder. Swallowing", e);
            }
            mMediaRecorder.release();
        }
        mProfile = null;
        mMediaRecorder = null;
        mMapper = null;
    }
}
