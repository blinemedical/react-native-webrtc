package com.oney.WebRTCModule;

import android.util.Log;
import org.webrtc.VideoTrack;
import java.io.File;
import java.util.HashMap;

public class MediaRecorderImpl {
    static final String TAG = MediaRecorderImpl.class.getCanonicalName();

    private final String id;
    private final VideoTrack videoTrack;
    private final HashMap videoTrackInfo;
    private File file;

    /**
     * VideoAudioFileRenderer is heavily influenced by Flutter's webRTC implementation
     * https://github.com/flutter-webrtc/flutter-webrtc/blob/main/android/src/main/java/com/cloudwebrtc/webrtc/record/VideoFileRenderer.java
     */
    private VideoAudioFileRenderer videoAudioFileRenderer;

    /**
     * The actual implementation for recording to a file
     * 
     * @param id Id
     * @param videoTrack Video track
     * @param interceptor Eventually we'll add this param as an AudioSamplesInterceptor to pipe audio
     */
    protected MediaRecorderImpl(String id, VideoTrack videoTrack, HashMap<String, Integer> videoTrackInfo) {
        this.id = id;
        this.videoTrack = videoTrack;
        this.videoTrackInfo = videoTrackInfo;
    }

    protected void start(File file) throws Exception {
        if (!file.exists()) {
            Log.e(TAG, "start() file: " + file.getAbsolutePath() + " does not exist!");
            return;
        }

        this.file = file;

        // Make sure videoTrack exists before adding sink
        if (videoTrack != null) {
            // Make sure directories exists before writing a file
            file.getParentFile().mkdirs();
            videoAudioFileRenderer = new VideoAudioFileRenderer(
                    file.getAbsolutePath(),
                    EglUtils.getRootEglBaseContext(),
                    videoTrackInfo
            );
            videoTrack.addSink(videoAudioFileRenderer);

            Log.i(TAG, "Started media recorder! " + videoAudioFileRenderer.toString());
        }
    }

    protected File stop() {
        // Remove sink from videoTrack (https://chromium.googlesource.com/external/webrtc/+/HEAD/sdk/android/api/org/webrtc/VideoTrack.java#49)
        // Release videoAudioFileRenderer, and set to null

       return file;
    }
}