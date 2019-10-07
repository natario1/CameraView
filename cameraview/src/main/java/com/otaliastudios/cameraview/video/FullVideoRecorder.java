package com.otaliastudios.cameraview.video;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.internal.DeviceEncoders;
import com.otaliastudios.cameraview.internal.utils.CamcorderProfiles;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaRecorder} APIs.
 *
 * When started, the media recorder will be prepared in
 * {@link #prepareMediaRecorder(VideoResult.Stub)}. This will call two abstract methods:
 * - {@link #getCamcorderProfile(VideoResult.Stub)}
 * - {@link #applyVideoSource(VideoResult.Stub, MediaRecorder)}
 *
 * Subclasses can also call {@link #prepareMediaRecorder(VideoResult.Stub)} before start happens,
 * in which case it will not be prepared twice. This can be used for example to test some
 * configurations.
 */
public abstract class FullVideoRecorder extends VideoRecorder {

    private static final String TAG = FullVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess") protected MediaRecorder mMediaRecorder;
    private CamcorderProfile mProfile;
    private boolean mMediaRecorderPrepared;


    FullVideoRecorder(@Nullable VideoResultListener listener) {
        super(listener);
    }

    /**
     * Subclasses should return an appropriate CamcorderProfile.
     * This could be taken from the {@link CamcorderProfiles} utility class based on the
     * stub declared size, for instance.
     *
     * @param stub the stub
     * @return the profile
     */
    @NonNull
    protected abstract CamcorderProfile getCamcorderProfile(@NonNull VideoResult.Stub stub);

    /**
     * Subclasses should apply a video source to the given recorder.
     *
     * @param stub the stub
     * @param mediaRecorder the recorder
     */
    protected abstract void applyVideoSource(@NonNull VideoResult.Stub stub,
                                             @NonNull MediaRecorder mediaRecorder);

    @SuppressWarnings("WeakerAccess")
    protected final boolean prepareMediaRecorder(@NonNull VideoResult.Stub stub) {
        if (mMediaRecorderPrepared) return true;
        // We kind of trust the stub size at this point. It's coming from CameraOptions sizes
        // and it's clipped to be less than CamcorderProfile's highest available profile.
        // However, we still can't trust the developer parameters (e.g. bit rates), and even
        // without them, the camera declared sizes can cause crashes in MediaRecorder (#467, #602).
        // A possible solution was to prepare without checking DeviceEncoders first, and should it
        // fail, prepare again checking them. However, when parameters are wrong, MediaRecorder
        // fails on start() instead of prepare() (start failed -19), so this wouldn't be effective.
        return prepareMediaRecorder(stub, true);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean prepareMediaRecorder(@NonNull VideoResult.Stub stub,
                                         boolean applyEncodersConstraints) {
        // 1. Create reference and ask for the CamcorderProfile
        mMediaRecorder = new MediaRecorder();
        mProfile = getCamcorderProfile(stub);

        // 2. Set the video and audio sources.
        applyVideoSource(stub, mMediaRecorder);
        boolean hasAudio = stub.audio == Audio.ON
                || stub.audio == Audio.MONO
                || stub.audio == Audio.STEREO;
        if (hasAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        // 3. Set the output format. Before, change the profile data if the user
        // has specified a specific codec.
        if (stub.videoCodec == VideoCodec.H_264) {
            mProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
            mProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        } else if (stub.videoCodec == VideoCodec.H_263) {
            mProfile.videoCodec = MediaRecorder.VideoEncoder.H263;
            mProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4; // should work
        }
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);

        // 4. Update the VideoResult stub with information from the profile, if the
        // stub values are absent or incomplete
        if (stub.videoFrameRate <= 0) stub.videoFrameRate = mProfile.videoFrameRate;
        if (stub.videoBitRate <= 0) stub.videoBitRate = mProfile.videoBitRate;
        if (stub.audioBitRate <= 0 && hasAudio) stub.audioBitRate = mProfile.audioBitRate;

        // 5. Update the VideoResult stub with DeviceEncoders constraints
        if (applyEncodersConstraints) {
            // A. Get the audio mime type
            // https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/StagefrightRecorder.cpp#1096
            // https://github.com/MrAlex94/Waterfox-Old/blob/master/media/libstagefright/frameworks/av/media/libstagefright/MediaDefs.cpp
            String audioType;
            switch (mProfile.audioCodec) {
                case MediaRecorder.AudioEncoder.AMR_NB: audioType = "audio/3gpp"; break;
                case MediaRecorder.AudioEncoder.AMR_WB: audioType = "audio/amr-wb"; break;
                case MediaRecorder.AudioEncoder.AAC:
                case MediaRecorder.AudioEncoder.HE_AAC:
                case MediaRecorder.AudioEncoder.AAC_ELD: audioType = "audio/mp4a-latm"; break;
                case MediaRecorder.AudioEncoder.VORBIS: audioType = "audio/vorbis"; break;
                case MediaRecorder.AudioEncoder.DEFAULT:
                default: audioType = "audio/3gpp";
            }
            // B. Get the video mime type
            // https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/StagefrightRecorder.cpp#1650
            // https://github.com/MrAlex94/Waterfox-Old/blob/master/media/libstagefright/frameworks/av/media/libstagefright/MediaDefs.cpp
            String videoType;
            switch (mProfile.videoCodec) {
                case MediaRecorder.VideoEncoder.H263: videoType = "video/3gpp"; break;
                case MediaRecorder.VideoEncoder.H264: videoType = "video/avc"; break;
                case MediaRecorder.VideoEncoder.MPEG_4_SP: videoType = "video/mp4v-es"; break;
                case MediaRecorder.VideoEncoder.VP8: videoType = "video/x-vnd.on2.vp8"; break;
                case MediaRecorder.VideoEncoder.HEVC: videoType = "video/hevc"; break;
                case MediaRecorder.VideoEncoder.DEFAULT:
                default: videoType = "video/avc";
            }
            // C. Check DeviceEncoders support
            boolean flip = stub.rotation % 180 != 0;
            if (flip) stub.size = stub.size.flip();
            Size newVideoSize = null;
            int newVideoBitRate = 0;
            int newAudioBitRate = 0;
            int newVideoFrameRate = 0;
            int videoEncoderOffset = 0;
            int audioEncoderOffset = 0;
            boolean encodersFound = false;
            while (!encodersFound) {
                LOG.i("prepareMediaRecorder:", "Checking DeviceEncoders...",
                        "videoOffset:", videoEncoderOffset,
                        "audioOffset:", audioEncoderOffset);
                DeviceEncoders encoders = new DeviceEncoders(DeviceEncoders.MODE_RESPECT_ORDER,
                        videoType, audioType, videoEncoderOffset, audioEncoderOffset);
                try {
                    newVideoSize = encoders.getSupportedVideoSize(stub.size);
                    newVideoBitRate = encoders.getSupportedVideoBitRate(stub.videoBitRate);
                    newAudioBitRate = encoders.getSupportedAudioBitRate(stub.audioBitRate);
                    newVideoFrameRate = encoders.getSupportedVideoFrameRate(newVideoSize,
                            stub.videoFrameRate);
                    encodersFound = true;
                } catch (DeviceEncoders.VideoException videoException) {
                    videoEncoderOffset++;
                } catch (DeviceEncoders.AudioException audioException) {
                    audioEncoderOffset++;
                }
            }
            // D. Apply results
            stub.size = newVideoSize;
            stub.videoBitRate = newVideoBitRate;
            stub.audioBitRate = newAudioBitRate;
            stub.videoFrameRate = newVideoFrameRate;
            if (flip) stub.size = stub.size.flip();
        }

        // 6A. Configure MediaRecorder from stub and from profile (video)
        boolean flip = stub.rotation % 180 != 0;
        mMediaRecorder.setVideoSize(
                flip ? stub.size.getHeight() : stub.size.getWidth(),
                flip ? stub.size.getWidth() : stub.size.getHeight());
        mMediaRecorder.setVideoFrameRate(stub.videoFrameRate);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        mMediaRecorder.setVideoEncodingBitRate(stub.videoBitRate);

        // 6B. Configure MediaRecorder from stub and from profile (audio)
        if (hasAudio) {
            if (stub.audio == Audio.ON) {
                mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            } else if (stub.audio == Audio.MONO) {
                mMediaRecorder.setAudioChannels(1);
            } else if (stub.audio == Audio.STEREO) {
                mMediaRecorder.setAudioChannels(2);
            }
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(stub.audioBitRate);
        }

        // 7. Set other params
        if (stub.location != null) {
            mMediaRecorder.setLocation(
                    (float) stub.location.getLatitude(),
                    (float) stub.location.getLongitude());
        }
        mMediaRecorder.setOutputFile(stub.file.getAbsolutePath());
        mMediaRecorder.setOrientationHint(stub.rotation);
        mMediaRecorder.setMaxFileSize(stub.maxSize);
        mMediaRecorder.setMaxDuration(stub.maxDuration);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
                        stop(false);
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
                        stop(false);
                        break;
                }
            }
        });

        // 8. Prepare the Recorder
        try {
            mMediaRecorder.prepare();
            mMediaRecorderPrepared = true;
            mError = null;
            return true;
        } catch (Exception e) {
            LOG.w("prepareMediaRecorder:", "Error while preparing media recorder.", e);
            mMediaRecorderPrepared = false;
            mError = e;
            return false;
        }
    }

    @Override
    protected void onStart() {
        if (!prepareMediaRecorder(mResult)) {
            mResult = null;
            stop(false);
            return;
        }

        try {
            mMediaRecorder.start();
            dispatchVideoRecordingStart();
        } catch (Exception e) {
            LOG.w("start:", "Error while starting media recorder.", e);
            mResult = null;
            mError = e;
            stop(false);
        }
    }

    @Override
    protected void onStop(boolean isCameraShutdown) {
        if (mMediaRecorder != null) {
            dispatchVideoRecordingEnd();
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                // This can happen if stopVideo() is called right after takeVideo()
                // (in which case we don't care). Or when prepare()/start() have failed for
                // some reason and we are not allowed to call stop.
                // Make sure we don't override the error if one exists already.
                mResult = null;
                if (mError == null) {
                    LOG.w("stop:", "Error while closing media recorder.", e);
                    mError = e;
                }
            }
            mMediaRecorder.release();
        }
        mProfile = null;
        mMediaRecorder = null;
        mMediaRecorderPrepared = false;
        dispatchResult();
    }

}
