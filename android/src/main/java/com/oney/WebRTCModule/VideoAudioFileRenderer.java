package com.oney.WebRTCModule;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.VideoSink;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;

import java.io.IOException;
import java.nio.ByteBuffer;

// Note: This is a happy path implementation and heavily based off of Flutters webrtc implementation
// If we use this we should attribute back to them in the code for all parts of the media recorder impl on
// the android side
// https://github.com/flutter-webrtc/flutter-webrtc/blob/main/android/src/main/java/com/cloudwebrtc/webrtc/record/VideoFileRenderer.java

public class VideoAudioFileRenderer implements VideoSink, SamplesReadyCallback {
    static final String TAG = VideoAudioFileRenderer.class.getCanonicalName();

    // Following are used to render the frames as they come in
    private EglBase eglBase;
    private EglBase.Context eglContext;
    private Surface surface;
    private GlRectDrawer rectDrawer;
    private VideoFrameDrawer videoFrameDrawer;

    // Following are used to encode the video and audio and mux them together
    private MediaMuxer mediaMuxer;
    private MediaCodec videoEncoder = null;
    private MediaCodec audioEncoder = null;

    // Buffer info for the separate tracks
    private MediaCodec.BufferInfo videoBufferInfo;
    private MediaCodec.BufferInfo audioBufferInfo;

    // Threads and handlers to do the actual work on so we don't lock up
    // the main thread
    private final HandlerThread videoRenderThread;
    private final Handler videoRenderThreadHandler;
    private final HandlerThread audioRenderThread;
    private final Handler audioRenderThreadHandler;

    // Used to determine if it's the first frame and get the proper incoming width and height
    // These are then used when rendering the frame out with EGL
    private int outputFileWidth = -1;
    private int outputFileHeight = -1;

    // Used to help sync the video with the audio... likely could handle this better but it works
    // well enough
    private long presentationTime = 0L;
    private long videoFrameStart = 0L;

    // Just used to know if we are currently rendering and if we have started muxing
    private boolean isRunning = true;
    private boolean muxerStarted = false;

    // Keep track of the currently selected audio and video track index. This is needed as the
    // proper index is required when writing out the sample data
    private int audioTrackIndex = -1;
    private int videoTrackIndex = -1;

    public VideoAudioFileRenderer(String outputFilePath, final EglBase.Context eglContext) throws IOException {
        videoBufferInfo = new MediaCodec.BufferInfo();
        audioBufferInfo = new MediaCodec.BufferInfo();

        videoRenderThread = new HandlerThread(TAG + "VideoRenderThread");
        videoRenderThread.start();
        videoRenderThreadHandler = new Handler(videoRenderThread.getLooper());

        audioRenderThread = new HandlerThread(TAG + "AudioRenderThread");
        audioRenderThread.start();
        audioRenderThreadHandler = new Handler(audioRenderThread.getLooper());

        this.eglContext = eglContext;

        mediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        videoFrameDrawer = new VideoFrameDrawer();

        Log.i(TAG, "Finished setting up muxer: " + mediaMuxer.toString());
    }

    public void release() {
        isRunning = false;
        audioRenderThreadHandler.post(() -> {
            audioEncoder.stop();
            audioEncoder.release();
            audioRenderThread.quit();
        });

        videoRenderThreadHandler.post(() -> {
            videoEncoder.stop();
            videoEncoder.release();
            eglBase.release();
            mediaMuxer.stop();
            mediaMuxer.release();
            videoRenderThread.quit();
        });
    }

    // NOTE: Can break this up in the future
    @Override
    public void onFrame(VideoFrame videoFrame) {
        videoFrame.retain();

//        Log.i(TAG, "FRAME RECEIVED!");

        // Initialize video encoding stuff
        if (outputFileWidth == -1) {
            outputFileWidth = videoFrame.getRotatedWidth();
            outputFileHeight = videoFrame.getRotatedHeight();

            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_MPEG4,
                    outputFileWidth,
                    outputFileHeight
            );

            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);

            // TODO: Temporary(happy path) but really should use the value we set when requesting user media
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); // 5 seconds between I-frames

            try {
                videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_MPEG4);
                videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

                videoRenderThreadHandler.post(() -> {
                    eglBase = EglBase.create(eglContext, EglBase.CONFIG_RECORDABLE);
                    surface = videoEncoder.createInputSurface();

                    videoEncoder.start();

                    eglBase.createSurface(surface);
                    eglBase.makeCurrent();
                    rectDrawer = new GlRectDrawer();
                });
            } catch (Exception e) {
                Log.wtf(TAG, e);
            }
        }

        videoRenderThreadHandler.post(() -> {
            videoFrameDrawer.drawFrame(
                    videoFrame,
                    rectDrawer,
                    null,
                    0,
                    0,
                    outputFileWidth,
                    outputFileHeight
            );
            videoFrame.release();
            muxVideo();
            eglBase.swapBuffers();
        });
    }

    private void muxVideo() {
        // Keep dequeueing until nothing left
        boolean shouldMux = true;
        while(shouldMux) {
           int encoderStatus = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 10000);

           switch(encoderStatus) {
               case MediaCodec.INFO_TRY_AGAIN_LATER:
                   shouldMux = false;
                   break;
               case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                   MediaFormat newFormat = videoEncoder.getOutputFormat();
                   Log.w(TAG, "video encoder format changed: " + newFormat);

                   // In theory we should handle if this changes although was running into some crashing
                   // if I saw any. So for now since we lock a lot of params this is likely fine but
                   // may need to find some time to figure out why it was crashing and address it
                   if (videoTrackIndex == -1) {
                       videoTrackIndex = mediaMuxer.addTrack(newFormat);
                       Log.i(TAG, "ADDING video track to muxer " + videoTrackIndex);
                   }

                   if (audioTrackIndex != -1 && !muxerStarted) {
                       Log.i(TAG, "muxVideo STARTING MUXER");
                       mediaMuxer.start();
                       muxerStarted = true;
                   }

                   if (!muxerStarted) {
                       shouldMux = false;
                   }
                   break;
               }
               default: {
                   // Some valid statuses are less than zero but if we hit this default case and
                   // we still hit a status we didn't expect to handle we shouldn't try to encode
                   // any data and just skip handling the status
                   if (encoderStatus < 0) {
                       Log.e(TAG, "Unexpected video encoderStatus: " + encoderStatus);
                       break;
                   }

                   // No edge case then do the work
                   ByteBuffer encodedData = videoEncoder.getOutputBuffer(encoderStatus);
                   if (encodedData == null) {
                       Log.e(TAG, "encoded video data " + encoderStatus + "null for some reason");
                       shouldMux = false;
                       break;
                   }

                   encodedData.position(videoBufferInfo.offset);
                   encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size);

                   if (videoFrameStart == 0 && videoBufferInfo.presentationTimeUs != 0) {
                       videoFrameStart = videoBufferInfo.presentationTimeUs;
                   }

                   videoBufferInfo.presentationTimeUs -= videoFrameStart;
                   if (muxerStarted) {
                       Log.i(TAG, "WRITING VIDEO DATA");
                       mediaMuxer.writeSampleData(videoTrackIndex, encodedData, videoBufferInfo);
                   }

                   isRunning = isRunning && (videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0;

                   videoEncoder.releaseOutputBuffer(encoderStatus, false);
                   if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                       shouldMux = false;
                       break;
                   }
               }
           }
        }
    }

    private void muxAudio() {
        // Keep dequeueing until nothing left
        boolean shouldMux = true;
        while (shouldMux) {
            int encoderStatus = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 0);

            switch(encoderStatus) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    shouldMux = false;
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    MediaFormat newFormat = audioEncoder.getOutputFormat();
                    Log.w(TAG, "audio encoder format changed: " + newFormat);

                    if (audioTrackIndex == -1) {
                        audioTrackIndex = mediaMuxer.addTrack(newFormat);
                        Log.i(TAG, "ADDING audio track to muxer " + audioTrackIndex);
                    }

                    if (videoTrackIndex != -1 && !muxerStarted) {
                        Log.i(TAG, "muxAudio STARTING MUXER");
                        mediaMuxer.start();
                        muxerStarted = true;
                    }

                    if (!muxerStarted) {
                        shouldMux = false;
                    }
                    break;
                }
                default: {
                    // Some valid statuses are less than zero but if we hit this default case and
                    // we still hit a status we didn't expect to handle we shouldn't try to encode
                    // any data and just skip handling the status
                    if (encoderStatus < 0) {
                        Log.e(TAG, "Unexpected video encoderStatus: " + encoderStatus);
                        break;
                    }

                    // No edge case then do the work
                    try {
                        ByteBuffer encodedData = audioEncoder.getOutputBuffer(encoderStatus);
                        if (encodedData == null) {
                            Log.e(TAG, "encoded audio data " + encoderStatus + "null for some reason");
                            shouldMux = false;
                            break;
                        }

                        encodedData.position(audioBufferInfo.offset);
                        encodedData.limit(audioBufferInfo.offset + audioBufferInfo.size);

                        if (muxerStarted) {
                            Log.i(TAG, "WRITING AUDIO DATA");
                            mediaMuxer.writeSampleData(audioTrackIndex, encodedData, audioBufferInfo);
                        }

                        isRunning = isRunning && (audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0;

                        audioEncoder.releaseOutputBuffer(encoderStatus, false);
                        if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            shouldMux = false;
                            break;
                        }
                    } catch (Exception e) {
                        Log.wtf(TAG, e);
                        shouldMux = false;
                    }
                    break;
                }
            }
        }
    }

    // This is the event that receives the audio data from the device
    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {
        if (!isRunning) {
            return;
        }

        // TODO: Might need to validate that we are good to do the following work...
        audioRenderThreadHandler.post(() -> {
            if (audioEncoder == null) {
                try {
                    audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                    MediaFormat audioFormat = MediaFormat.createAudioFormat(
                            MediaFormat.MIMETYPE_AUDIO_AAC,
                            audioSamples.getSampleRate(),
                            audioSamples.getChannelCount()
                    );

                    // If these aren't set it will cause an exception. I can't remember if it was when
                    // you configure or when you tried to dequeue
                    audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);
                    audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

                    audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    audioEncoder.start();
                } catch (IOException exception) {
                    Log.wtf(TAG, exception);
                }
            }

            int index = audioEncoder.dequeueInputBuffer(0);
            if(index >= 0) {
                ByteBuffer buffer = audioEncoder.getInputBuffer(index);

                byte[] audioData = audioSamples.getData();
                buffer.put(audioData);

                audioEncoder.queueInputBuffer(index, 0, audioData.length, presentationTime, 0);
                // Comment from the flutter webrtc says -- // 1000000 microseconds / 48000hz / 2 bytes
                // Likely this just uses the length and converts it over to the right unit over the span of
                // the entire sample and we increment our stored presentationTime by that amount
                presentationTime += audioData.length * 125 / 12; // TODO do better
            }
            muxAudio();
        });
    }
}
