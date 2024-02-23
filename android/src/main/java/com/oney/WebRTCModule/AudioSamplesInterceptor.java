package com.oney.WebRTCModule;

import android.annotation.SuppressLint;

import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples;

import java.util.HashMap;

// Note: This is a happy path implementation and heavily based off of Flutters webrtc implementation
// https://github.com/flutter-webrtc/flutter-webrtc/blob/main/android/src/main/java/com/cloudwebrtc/webrtc/record/AudioSamplesInterceptor.java

/** JavaAudioDeviceModule allows attaching samples callback only on building
 *  We don't want to instantiate VideoFileRenderer and codecs at this step
 *  It's simple dummy class, it does nothing until samples are necessary */
@SuppressWarnings("WeakerAccess")
public class AudioSamplesInterceptor implements SamplesReadyCallback {

    @SuppressLint("UseSparseArrays")
    protected final HashMap<String, SamplesReadyCallback> callbacks = new HashMap<>();

    @Override
    public void onWebRtcAudioRecordSamplesReady(AudioSamples audioSamples) {
        for (SamplesReadyCallback callback : callbacks.values()) {
            callback.onWebRtcAudioRecordSamplesReady(audioSamples);
        }
    }

    public void attachCallback(String id, SamplesReadyCallback callback) throws Exception {
        callbacks.put(id, callback);
    }

    public void detachCallback(String id) {
        callbacks.remove(id);
    }

}