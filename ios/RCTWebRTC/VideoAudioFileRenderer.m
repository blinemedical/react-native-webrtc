#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTLog.h>
#import "VideoAudioFileRenderer.h"


@implementation VideoAudioFileRenderer {
    int _frameRate;
    int64_t _startTS;
    NSURL* _path;
    AVAssetWriter* _Nullable _assetWriter;
    AVAssetWriterInput* _Nullable _videoInput;
    AVAssetWriterInput* _Nullable _audioInput;
}

- (instancetype)initWithAudioTrack:(RTCAudioTrack *)audio videoConstraints:(NSDictionary *)videoConstraints {
    self = [super init];
    _frameRate = [videoConstraints[@"frameRate"] intValue];
    _path = nil;
    _startTS = -1;
    return self;
}


- (void)startCapture:(NSURL *)path {
    _path = path;
}

// Still deciding if this should take in a callback to return a value
// once the video file has finished writing. Can be handled eventually
// across the bridge with a promise that the JS can await
- (void)stopCapture {
    [_videoInput markAsFinished];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->_assetWriter finishWritingWithCompletionHandler:^{
            NSError* error = self->_assetWriter.error;
            
            if (error != nil) {
                RCTLog(@"[VideoAudioFileRenderer]: Failed to finish writing stream to file %@", error.localizedDescription);
            }
        }];
    });
}

- (void)initAssetWriter:(CGSize)size {
    if (_assetWriter) {
        RCTLog(@"[VideoAudioFileRenderer]: Asset writer already initialized, did you call this function by mistake?");
        return;
    }
    
    NSDictionary* videoOutputSettings = @{
        AVVideoCompressionPropertiesKey: @{AVVideoAverageBitRateKey: @(6000000)},
        AVVideoCodecKey: AVVideoCodecTypeH264,
        AVVideoHeightKey: @(size.height),
        AVVideoWidthKey: @(size.width)
    };
    
    _videoInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeVideo outputSettings:videoOutputSettings];
    _videoInput.mediaTimeScale = _frameRate;
    _videoInput.expectsMediaDataInRealTime = true;
   
    NSError *error;
    _assetWriter = [[AVAssetWriter alloc] initWithURL:_path fileType:AVFileTypeMPEG4 error:&error];
    
    if (error != nil) {
        RCTLog(@"[VideoAudioFileRenderer]: Failed to init the AVAssetWriter - %@", error.localizedDescription);
        return;
    }
  
    _assetWriter.shouldOptimizeForNetworkUse = true;
    [_assetWriter addInput:_videoInput];
    [_assetWriter startWriting];
    [_assetWriter startSessionAtSourceTime:kCMTimeZero];
}

- (void)setSize:(CGSize)size {
    
}

- (void)renderFrame:(nullable RTCVideoFrame*)frame {
    // Using _path currently to know that startCapture was called.
    // Unsure if this will kept in the long term but will wait until
    // things are more built out to see if something else (maybe a
    // boolean) would be better
    if (frame == nil || _path == nil) {
        return;
    }

    if (_assetWriter == nil) {
        CGSize frameSize = CGSizeMake(frame.width, frame.height);
        [self initAssetWriter:frameSize];
    }

    // Don't try to append more frames if the input isn't ready
    if (!_videoInput.readyForMoreMediaData) {
        RCTLog(@"[VideoAudioFileRenderer]: Video input not ready for more data, dropping frame");
        return;
    }
    
    id <RTCVideoFrameBuffer> frameBuffer = frame.buffer;
    if (![frameBuffer isKindOfClass:[RTCCVPixelBuffer class]]) {
        RCTLog(@"[VideoAudioFileRenderer]: Unexpected frame buffer type");
        return;
    }

    CVPixelBufferRef pixelBufferRef = ((RTCCVPixelBuffer*)frameBuffer).pixelBuffer;
    CMVideoFormatDescriptionRef formatDesc;
    OSStatus result = CMVideoFormatDescriptionCreateForImageBuffer(kCFAllocatorDefault, pixelBufferRef, &formatDesc);
    
    if (result != noErr) {
        RCTLog(@"[VideoAudioFileRenderer]: failed to create format description");
        return;
    }
    
    CMSampleTimingInfo timingInfo;
    timingInfo.decodeTimeStamp = kCMTimeInvalid;

    if (_startTS == -1) {
        _startTS = frame.timeStampNs / 1000;
    }
    
    int64_t frameTime = (frame.timeStampNs / 1000) - _startTS;
    timingInfo.presentationTimeStamp = CMTimeMake(frameTime, 100000);
    
    CMSampleBufferRef outputBuffer;
    result = CMSampleBufferCreateReadyWithImageBuffer(
                                                      kCFAllocatorDefault,
                                                      pixelBufferRef,
                                                      formatDesc,
                                                      &timingInfo,
                                                      &outputBuffer
                                                      );
    
    if (result != noErr) {
        RCTLog(@"[VideoAudioFileRenderer]: Failed to create the output buffer with a given image buffer");
        return;
    }
    
    if(![_videoInput appendSampleBuffer:outputBuffer]) {
        RCTLog(@"[VideoAudioFileRenderer]: Failed to append image buffer to asset writer input");
    }
}

@end
