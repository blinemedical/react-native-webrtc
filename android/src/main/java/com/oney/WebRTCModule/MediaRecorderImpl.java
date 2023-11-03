package com.oney.WebRTCModule;

import android.util.Log;

import org.webrtc.VideoTrack;

import java.io.File;

public class MediaRecorderImpl {
    static final String TAG = MediaRecorderImpl.class.getCanonicalName();

    private final String id;
    private final VideoTrack videoTrack;
    private VideoAudioFileRenderer videoAudioFileRenderer;
    private AudioSamplesInterceptor audioSamplesInterceptor;
    private File file;

    public MediaRecorderImpl(String id, VideoTrack videoTrack, AudioSamplesInterceptor interceptor) {
        this.id = id;
        this.videoTrack = videoTrack;
        this.audioSamplesInterceptor = interceptor;
    }

    public void start(File file) throws Exception {
        this.file = file;

        // Not really needed probably but was putting this here as a test
        if (file.exists()) {
            Log.w(TAG, "File: " + file.getAbsolutePath() + " Already exists!");
            file.delete();
        }

        // Make sure directories exists before writing a file
        file.getParentFile().mkdirs();
        videoAudioFileRenderer = new VideoAudioFileRenderer(
                file.getAbsolutePath(),
                EglUtils.getRootEglBaseContext()
        );
        videoTrack.addSink(videoAudioFileRenderer);

        audioSamplesInterceptor.attachCallback(id, videoAudioFileRenderer);

        Log.i(TAG, "Started media recorder! " + videoAudioFileRenderer.toString());
    }

    public File stop() {
        videoTrack.removeSink(videoAudioFileRenderer);
        videoAudioFileRenderer.release();;
        videoAudioFileRenderer = null;

       return file;
    }
}
