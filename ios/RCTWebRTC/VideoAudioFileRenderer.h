#import <Foundation/Foundation.h>
#import <WebRTC/WebRTC.h>
#import "CaptureController.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@class VideoAudioFileRenderer;

@interface VideoAudioFileRenderer : NSObject<RTCVideoRenderer>

- (instancetype)initWithAudioTrack:(nonnull RTCAudioTrack*)audio videoConstraints:(nonnull NSDictionary*)videoConstraints;
- (void)startCapture:(nonnull NSURL*)path;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
