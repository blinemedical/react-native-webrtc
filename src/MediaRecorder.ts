import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import { uniqueID } from './RTCUtil';

const { WebRTCModule } = NativeModules;

// This could be used as a second param in the constructor, but we don't care yet
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
    private isStarted: boolean;
    private id: string;

    constructor(mediaStream: MediaStream) {
        this.stream = mediaStream;
        this.id = uniqueID();

        this.mimeType = 'video/avc'; // this is what flutter uses
        this.state = RecorderState.inactive;
        this.videoBitsPerSecond = 250000; // taken from example in specs
        this.audioBitsPerSecond = 128000; // taken from example in specs
        this.isStarted = false;

        WebRTCModule.mediaRecorderCreate(this.id, this.stream.id);
    }

    /**
     * Begins recording media
     * The specs say it can optionally be passed a timeslice param, but we're ignoring that
     * By ignoring timeslice, we'll always record the media in a single large chunk
     *
     * @param filePath The path where we'll save the file
     */
    start(filePath) {
        // TODO: use react-native-fs for filePath
        try {
            WebRTCModule.mediaRecorderStart(this.id, filePath);
            this.isStarted = true;
        } catch (e) {
            console.log(MediaRecorder.name + ' start(), ' + e);
        }
    }

    /**
     * Stops recording
     */
    stop() {
        // TODO: stop recording, fire stop event
        // The stop event would be preceeded by a `dataavailable` event, but we're
        // not implementing that.

        if (this.isStarted) {
            try {
                WebRTCModule.mediaRecorderStop(this.id);
                this.isStarted = false;
            } catch (e) {
                console.log(MediaRecorder.name + ' stop(), ' + e);
            }
        } else {
            throw Error('Cannot stop recording because recording hasn\'t started yet');
        }
    }
}