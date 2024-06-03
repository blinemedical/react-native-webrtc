#if TARGET_OS_IOS
#import <WebRTC/WebRTC.h>
#import <WebRTC/RTCAudioSource.h>
#import <AVFoundation/AVFoundation.h>
#import "MediaRecorderImpl.h"
#import "VideoAudioFileRenderer.h"

@implementation MediaRecorderImpl {
    NSURL* _filePath;
    NSDictionary* _videoTrackSettings;
    VideoAudioFileRenderer* _fileRenderer;
}

- (instancetype)initWithVideoTrack:(RTCVideoTrack*)video videoTrackSettings:(NSDictionary*)videoSettings audioTrack:(RTCAudioTrack*)audio {
    self = [super init];
    self.videoTrack = video;
    self.audioTrack = audio;
   
    _videoTrackSettings = videoSettings;
    _fileRenderer = [[VideoAudioFileRenderer alloc] initWithAudioTrack:audio videoConstraints:videoSettings];
    
    return self;
}

- (void)start:(NSURL *)path {
    _filePath = path;
    [self.videoTrack addRenderer:_fileRenderer];
    [_fileRenderer startCapture:_filePath];
}

- (NSURL*)stop {
    [self.videoTrack removeRenderer:_fileRenderer];
    [_fileRenderer stopCapture];
    return _filePath;
}

@end

#endif
