package com.oney.WebRTCModule;

import android.util.Log;
import org.webrtc.VideoTrack;
import java.io.File;

public class MediaRecorderImpl {
    static final String TAG = MediaRecorderImpl.class.getCanonicalName();

    private final String id;
    private final VideoTrack videoTrack;
    // private VideoAudioFileRenderer videoAudioFileRenderer;
    private File file;

    /**
     * The actual implementation for recording to a file
     * 
     * @param id Id
     * @param videoTrack Video track
     * @param interceptor Eventually we'll add this param as an AudioSamplesInterceptor to pipe audio
     */
    protected MediaRecorderImpl(String id, VideoTrack videoTrack) {
        this.id = id;
        this.videoTrack = videoTrack;
    }

    protected void start(File file) throws Exception {
        if (!file.exists()) {
            Log.e(TAG, "start() file: " + file.getAbsolutePath() + " does not exist!");
            return;
        }

        this.file = file;

        // Make sure videoTrack exists before adding sink
        if (videoTrack != null) {
            // Create a new VideoAudioFileRenderer using the file path and EglUtils.getRootEglBaseContext()
            // VideoAudioFileRenderer will write to the file
            // Add sink to videoTrack (https://chromium.googlesource.com/external/webrtc/+/HEAD/sdk/android/api/org/webrtc/VideoTrack.java#31)
            // Log.i(TAG, "Started media recorder! " + videoAudioFileRenderer.toString());
        }
    }

    protected File stop() {
        // Remove sink from videoTrack (https://chromium.googlesource.com/external/webrtc/+/HEAD/sdk/android/api/org/webrtc/VideoTrack.java#49)
        // Release videoAudioFileRenderer, and set to null

       return file;
    }
}