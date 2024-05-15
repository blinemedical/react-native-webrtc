#if TARGET_OS_IOS
#import <WebRTC/WebRTC.h>
#import <AVFoundation/AVFoundation.h>
#import "MediaRecorderImpl.h"

@implementation MediaRecorderImpl {
    NSURL* _filePath;
}

- (instancetype)initWithVideoTrack:(RTCVideoTrack*)video audioTrack:(RTCAudioTrack*)audio {
    self = [super init];
    self.videoTrack = video;
    self.audioTrack = audio;
    
    return self;
}

- (void)start:(NSURL *)path {
    _filePath = path;
}

- (NSURL*)stop {
    return _filePath;
}

@end

#endif
