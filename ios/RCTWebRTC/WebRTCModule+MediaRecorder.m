#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTLog.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#import <WebRTC/RTCVideoRenderer.h>

#import "WebRTCModule+MediaRecorder.h"

@interface MediaRecorderVideoRenderer : NSObject<RTCVideoRenderer>

@property(copy, nonatomic) WebRTCModule *module;
@property(copy, nonatomic) AVAssetWriterInput *input;

@end

@implementation MediaRecorderVideoRenderer

- (instancetype)initWith : (WebRTCModule *)module : (AVAssetWriterInput *)input {
    self = [super init];
    self.module = module;
    self.input = input;
   
    return self;
}

- (CMSampleBufferRef)getCMSampleBuffer : (RTCCVPixelBuffer *)buffer : (int64_t)timestamp {
    CMTime time;
    time.value = (double)timestamp;
    time.timescale = NSEC_PER_SEC;
    
    CMSampleTimingInfo timingInfo;
    timingInfo.duration = kCMTimeInvalid;
    timingInfo.presentationTimeStamp = time;
    timingInfo.decodeTimeStamp = kCMTimeInvalid;
    
    CMFormatDescriptionRef formatDesc;
    CMVideoFormatDescriptionCreateForImageBuffer(kCFAllocatorDefault, buffer.pixelBuffer, &formatDesc);
    
    CMSampleBufferRef sampleBuffer;
    CMSampleBufferCreateReadyWithImageBuffer(kCFAllocatorDefault, buffer.pixelBuffer, formatDesc, &timingInfo, &sampleBuffer);
    
    return sampleBuffer;
}

- (void)renderFrame:(nullable RTCVideoFrame *)frame {
    if (frame == nil) {
        return;
    }
    
    RTCCVPixelBuffer *pixelBuffer = (RTCCVPixelBuffer *)frame.buffer;
    CMSampleBufferRef sampleBuffer = [self getCMSampleBuffer:pixelBuffer : frame.timeStampNs];

    [self.input appendSampleBuffer:sampleBuffer];
}

- (void)setSize:(CGSize)size {
    // Maybe not needed for this renderers purpose???
}

@end

@interface WebRTCModule (MediaRecorder)

@end

@implementation WebRTCModule (MediaRecorder)

RCT_EXPORT_METHOD(mediaRecorderCreate : (nonnull NSString *)mediaRecorderID : (nonnull NSString *)streamID) {
    NSFileManager *fileManager = NSFileManager.defaultManager;
    NSArray<NSURL *>* urls = [fileManager URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask];
    NSURL* documentDirectory = urls.firstObject;
   
    if (documentDirectory == nil) {
        RCTLog(@"Failed to grab user document directory");
        return;
    }
   
    // For now but should use stream id for the file name
    NSURL *outputFileName = [documentDirectory URLByAppendingPathComponent:[NSString stringWithFormat:@"%@.mp4", streamID]];
    NSError *outError;
    AVAssetWriter *assetWriter = [AVAssetWriter assetWriterWithURL:outputFileName fileType:AVFileTypeMPEG4 error:&outError];
    
    if (assetWriter == nil) {
        RCTLog(@"Failed to create asset writer");
        return;
    }
    
    self.mediaRecorderFilePaths[mediaRecorderID] = outputFileName;
    self.assetWriters[mediaRecorderID] = assetWriter;

    RTCMediaStream *mediaStream = self.localStreams[streamID];
    RTCVideoTrack *videoTrack = [mediaStream.videoTracks firstObject];
    
    AVAssetWriterInput *videoInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeVideo outputSettings:nil];
    [assetWriter addInput:videoInput];
    
    MediaRecorderVideoRenderer *videoRenderer = [[MediaRecorderVideoRenderer alloc] initWith:self : videoInput];
    [videoTrack addRenderer:videoRenderer];
    
    RTCAudioTrack *audioTrack = [mediaStream.audioTracks firstObject];
    RTCAudioSource *audioSource = audioTrack.source;
    
    AVAssetWriterInput *audioInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeAudio outputSettings: nil];
}

RCT_EXPORT_METHOD(mediaRecorderStart : (nonnull NSString *)mediaRecorderID) {
    AVAssetWriter *assetWriter = self.assetWriters[mediaRecorderID];
    
    if (assetWriter == nil) {
        RCTLog(@"Unable to fine AssetWriter for given MediaRecorder ID");
        return;
    }
    
    [assetWriter startWriting];
}

RCT_EXPORT_METHOD(mediaRecorderStop: (nonnull NSString *)mediaRecorderID) {
    AVAssetWriter *assetWriter = self.assetWriters[mediaRecorderID];
    
    if (assetWriter == nil) {
        RCTLog(@"Unable to fine AssetWriter for given MediaRecorder ID");
        return;
    }
   
    [assetWriter finishWritingWithCompletionHandler:^(void) {
        NSURL *fileName = self.mediaRecorderFilePaths[mediaRecorderID];
        [self sendEventWithName:kEventMediaRecorderDataAvailable
                           body:@{
                                @"filePath" : fileName
                            }];
 
    }];
}

@end
