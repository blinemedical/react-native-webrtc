#import <Foundation/Foundation.h>
#import <WebRTC/WebRTC.h>
#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN

// Will also need to hold a reference to a video audio file renderer class
// but that will be added in future work
@interface MediaRecorderImpl : NSObject

@property(nonatomic, strong) RTCVideoTrack* _Nullable videoTrack;
@property(nonatomic, strong) RTCAudioTrack* _Nullable audioTrack;


- (instancetype _Nonnull) initWithVideoTrack:(RTCVideoTrack* _Nullable)video
                                  audioTrack:(RTCAudioTrack* _Nullable)audio;

- (void) start:(NSURL* _Nonnull)path;
- (NSURL*) stop;

@end

NS_ASSUME_NONNULL_END
