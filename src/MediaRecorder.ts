import { EventEmitter } from 'react-native';

import MediaStream from './MediaStream';

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
    private eventEmitter: EventEmitter;
    private isStarted: boolean;


    constructor(mediaStream: MediaStream) {
        this.mimeType = 'video/avc'; // this is what flutter uses
        this.state = RecorderState.inactive;
        this.videoBitsPerSecond = 250000; // taken from example in specs
        this.audioBitsPerSecond = 128000; // taken from example in specs
        this.stream = mediaStream;
        this.eventEmitter = new EventEmitter();
        this.isStarted = false;

        this.eventEmitter.addListener('stop', this.onstop);
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
        throw Error('Unimplemented');
    }

    /**
     * Stops recording
     */
    stop() {
        // TODO: stop recording, fire stop event
        // The stop event would be preceeded by a `dataavailable` event, but we're
        // not implementing that.
        throw Error('Unimplemented');

        if (this.isStarted) {
            this.eventEmitter.emit('stop');
        } else {
            throw Error('Cannot stop recording because recording hasn\'t started yet');
        }
    }

    /**
     * On 'stop' event handler; 'stop' is fired when the recording stops
     */
    onstop = (event: Event) => {
        // TODO: save the file to the filePath specified when we started recording
        throw Error('Unimplemented');
    };
}