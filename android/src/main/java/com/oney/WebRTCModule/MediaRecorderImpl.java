package com.oney.WebRTCModule;

import org.webrtc.VideoTrack;

import java.io.File;

public class MediaRecorderImpl {
    private final String id;
    private final VideoTrack videoTrack;
    private VideoAudioFileRenderer videoAudioFileRenderer;
    private File file;

    public MediaRecorderImpl(String id, VideoTrack videoTrack) {
        this.id = id;
        this.videoTrack = videoTrack;
    }

    public void start(File file) throws Exception {
        this.file = file;

        file.getParentFile().mkdirs();
        videoAudioFileRenderer = new VideoAudioFileRenderer(
                file.getAbsolutePath(),
                EglUtils.getRootEglBaseContext()
        );
        videoTrack.addSink(videoAudioFileRenderer);
    }

    public File stop() {
        videoTrack.removeSink(videoAudioFileRenderer);
        videoAudioFileRenderer.release();;
        videoAudioFileRenderer = null;

       return file;
    }
}
