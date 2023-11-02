import { NativeModules } from 'react-native';
import { uniqueID } from './RTCUtil';

import MediaStream from './MediaStream';

const { WebRTCModule } = NativeModules;

export default class MediaRecorder {
    _stream: MediaStream;
    _mimeType: string;

    _id: string;

    // Could potentially use mimeType here on the native side to determine which file type
    // to output
    constructor(stream: MediaStream, mimeType = 'video/mp4') {
        this._stream = stream;
        this._mimeType = mimeType;

        this._id = uniqueID();

        WebRTCModule.mediaRecorderCreate(this._id, this._stream.id); 
    }

    get stream(): MediaStream {
        return this._stream;
    }

    get mimeType(): string {
        return this._mimeType;
    }

    start() {
        WebRTCModule.mediaRecorderStart(this._id);
    }

    stop() {
        WebRTCModule.mediaRecorderStop(this._id)
    }
}
