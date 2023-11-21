import MediaStream from './MediaStream';

interface RecorderOptions {
    mimeType?: string;
    audioBitsPerSecond?: number;
    videoBitsPerSecond?: number;
    bitsPerSecond?: number;
}

enum RecorderState {
    inactive,
    recording,
    paused,
}

/**
 * Custom implementation of the base class for MediaRecorder per the specs here:
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder
 */
export default class MediaRecorder {
    public stream: MediaStream;
    public mimeType: string; // find the sweet spot between ios and android
    public state: RecorderState;
    public videoBitsPerSecond: number;
    public audioBitsPerSecond: number;

    constructor(mediaStream: MediaStream) {
        /**
         * Android video formats can be found here:
         * https://developer.android.com/guide/topics/media/platform/supported-formats#video-formats
         * https://developer.mozilla.org/en-US/docs/Web/Media/Formats/WebRTC_codecs#supported_video_codecs
         * https://developer.mozilla.org/en-US/docs/Web/Media/Formats/codecs_parameter
         * https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Containers
         *
         * Android supported codecs:
         * video = H.263, avc (H.264), H.265 HEVC, MPEG-4 SP, vp8, vp9, AV1
         * audio = AAC LC, HE-AAC(v1&2), AMR-NB, AMR-WB, FLAC, MP3, opus, PCM/WAVE, Vorbis
         * video container file types = 3GP, MPEG-4 (MP4), Ogg, + others
         *
         * WebRTC supported codecs:
         * video = vp8, avc (ACV/H.264 Constrained Baseline (CB))
         * other video (not supported by all browsers) = vp9
         * audio = opus, G.711 (A-law and μ-law, aka PCMA and PCMU)
         * other audio (not supported by all browsers) = G.722, iLBC, iSAC
         *
         * Codecs 3GP supports:
         * video = vp8, AVC (H.264), + others
         * audio = none WebRTC supports?, + AAC LC, HE-AAC(v1&2), mp3, AMR-NB, AMR-WB
         *
         * // Flutter uses mp4
         * Codecs MPEG-4 (MP4) supports:
         * video = avc (H.264), vp9
         * audio = opus, + AAC LC, HE-AAC(v1&2), FLAC, MP3
         *
         * Codecs Ogg supports:
         * video = vp8, vp9
         * audio = opus (but android's docs don't think so)
         *
         * Codecs WebM supports:
         * video = vp8, vp9
         * audio = opus
         *
         * "If your target audience is likely to include users on mobile, especially
         *   on lower-end devices or on slow networks, consider providing a version
         *   of your media in a 3GP container with appropriate compression."
         *
         * Container format MIME types:
         * video/mp4; codecs="avc1[.PPCCLL]"
         * PP = profile number (42 for constrained baseline)
         * CC = constraint set flags (40 for constrained baseline)
         * LL = level, where the higher the level means higher the
         *   bandwidth of the stream, and higher max video dimensions
         * example format for CB = video/mp4; codecs="avc1.42402a"
         */
        this.mimeType = 'video/avc'; // this is what flutter uses
        this.state = RecorderState.inactive;
        this.videoBitsPerSecond = 250000; // taken from example in specs
        this.audioBitsPerSecond = 128000; // taken from example in specs
        this.stream = mediaStream;
    }

    /**
     * Begins recording media
     * The specs say it can optionally be passed a timeslice param, but we're ignoring that
     * By ignoring timeslice, we'll always record the media in a single large chunk
     *
     * @param filePath The path where we'll save the file
     */
    start(filePath) {
        // TODO: start recording, and save the path
        // use react-native-fs for filePath
    }

    /**
     * Stops recording
     */
    stop() {
        // TODO: stop recording, fire stop event
        // The stop event would be preceeded by a `dataavailable` event, but we're
        // not implementing that.
    }

    /**
     * On-stop event is fired when the recording stops
     */
    onStop() {
        // TODO: save the file to the filePath specified when we started recording
    }
}