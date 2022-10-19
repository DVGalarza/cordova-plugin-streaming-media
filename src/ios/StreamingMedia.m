#import "StreamingMedia.h"
#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>
#import <AVKit/AVKit.h>
#import "LandscapeVideo.h"
#import "PortraitVideo.h"

@interface StreamingMedia()
- (void)play:(CDVInvokedUrlCommand *) command type:(NSString *) type;
- (void)startPlayer:(NSString*)uri;
- (void)moviePlayBackDidFinish:(NSNotification*)notification;
- (void)cleanup;
@end

@implementation StreamingMedia {
    NSString* callbackId;
    AVPlayer *avPlayer;
}

NSString * const TYPE_VIDEO = @"VIDEO";
NSString * const TYPE_AUDIO = @"AUDIO";
NSString * const DEFAULT_IMAGE_SCALE = @"center";

-(void)play:(CDVInvokedUrlCommand *) command type:(NSString *) type {
    NSLog(@"play called");
    callbackId = command.callbackId;
    NSString *mediaUrl  = [command.arguments objectAtIndex:0];
    
    [self startPlayer:mediaUrl];
}

-(void)stop:(CDVInvokedUrlCommand *) command type:(NSString *) type {
    NSLog(@"stop called");
    callbackId = command.callbackId;
    if (avPlayer.rate != 0.0) {
        [avPlayer pause];
    }
}

-(void)playVideo:(CDVInvokedUrlCommand *) command {
    NSLog(@"playvideo called");
    [self ignoreMute];
    [self play:command type:[NSString stringWithString:TYPE_VIDEO]];
}

-(void)playAudio:(CDVInvokedUrlCommand *) command {
    NSLog(@"playAudio called");
    [self ignoreMute];
    [self play:command type:[NSString stringWithString:TYPE_AUDIO]];
}

-(void)pauseAudio:(CDVInvokedUrlCommand *) command {
    NSLog(@"pauseAudio called");
    [avPlayer pause];
}

-(void)resumeAudio:(CDVInvokedUrlCommand *) command {
    NSLog(@"resumeAudio called");
    [avPlayer play];
}

-(void)stopAudio:(CDVInvokedUrlCommand *) command {
    NSLog(@"stopAudio called");
    [self stop:command type:[NSString stringWithString:TYPE_AUDIO]];
}

// Ignore the mute button
-(void)ignoreMute {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayback error:nil];
}

-(void)startPlayer:(NSString*)uri {
    NSLog(@"startplayer called");
    NSURL *url             =  [NSURL URLWithString:uri];
    avPlayer                  =  [AVPlayer playerWithURL:url];

    [avPlayer play];

    // Get the shared command center.
    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];

    commandCenter.previousTrackCommand.enabled = false;
    commandCenter.nextTrackCommand.enabled = false;

    commandCenter.playCommand.enabled = true;
    [commandCenter.playCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent * _Nonnull event) {
        NSLog(@"lock screen play called");
        if (self->avPlayer.rate == 0.0) {
            [self->avPlayer play];
            return MPRemoteCommandHandlerStatusSuccess;
        }
        return MPRemoteCommandHandlerStatusCommandFailed;
    }];

    commandCenter.pauseCommand.enabled = true;
    [commandCenter.pauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent * _Nonnull event) {
        NSLog(@"lock screen pause called");
        if (self->avPlayer.rate != 0.0) {
            [self->avPlayer pause];
            return MPRemoteCommandHandlerStatusSuccess;
        }
        return MPRemoteCommandHandlerStatusCommandFailed;
    }];
    
    // setup listners
    [self handleListeners];
}

- (void) handleListeners {
    
    // Listen for re-maximize
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(appDidBecomeActive:)
                                                 name:UIApplicationDidBecomeActiveNotification
                                               object:nil];
    
    // Listen for minimize
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(appDidEnterBackground:)
                                                 name:UIApplicationDidEnterBackgroundNotification
                                               object:nil];
    
    // Listen for playback finishing
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlayBackDidFinish:)
                                                 name:AVPlayerItemDidPlayToEndTimeNotification
                                               object:avPlayer.currentItem];
    
    // Listen for errors
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlayBackDidFinish:)
                                                 name:AVPlayerItemFailedToPlayToEndTimeNotification
                                               object:avPlayer.currentItem];

    // Listen for playback rate change
    // [[NSNotificationCenter defaultCenter] addObserver:self
    //                                          selector:@selector(playbackRateDidChange:)
    //                                              name:AVPlayerRateDidChangeNotification
    //                                            object:movie];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleInterruption:)
                                                 name:AVAudioSessionInterruptionNotification
                                               object:nil];

    /* Listen for click on the "Done" button
     
     // Deprecated.. AVPlayerController doesn't offer a "Done" listener... thanks apple. We'll listen for an error when playback finishes
     [[NSNotificationCenter defaultCenter] addObserver:self
     selector:@selector(doneButtonClick:)
     name:MPMoviePlayerWillExitFullscreenNotification
     object:nil];
     */
}

- (void) handleInterruption:(NSNotification*)notification {
    NSLog(@"handleInterruption %@", notification);
    NSDictionary *notificationUserInfo = [notification userInfo];
    NSNumber *interruptionType = [notificationUserInfo objectForKey:AVAudioSessionInterruptionTypeKey];

    if ([interruptionType intValue] == 1) {
        NSLog(@"interruption started");
        [avPlayer pause];
    } else if ([interruptionType intValue] == 0) {
        NSLog(@"interruption ended");
        [avPlayer play];
    }
}

- (void) appDidEnterBackground:(NSNotification*)notification {
    NSLog(@"appDidEnterBackground");
}

- (void) appDidBecomeActive:(NSNotification*)notification {
    NSLog(@"appDidBecomeActive");
}

- (void) playbackRateDidChange:(NSNotification*)notification {
    NSLog(@"playbackRateDidChange");
}

- (void) moviePlayBackDidFinish:(NSNotification*)notification {
    NSLog(@"Playback did finish with error message being %@", notification.userInfo);
    NSDictionary *notificationUserInfo = [notification userInfo];
    NSNumber *errorValue = [notificationUserInfo objectForKey:AVPlayerItemFailedToPlayToEndTimeErrorKey];
    NSString *errorMsg;
    if (errorValue) {
        NSError *mediaPlayerError = [notificationUserInfo objectForKey:@"error"];
        if (mediaPlayerError) {
            errorMsg = [mediaPlayerError localizedDescription];
        } else {
            errorMsg = @"Unknown error.";
        }
        NSLog(@"Playback failed: %@", errorMsg);
    }
    
    // if ([errorMsg length] != 0) {
        [self cleanup];
        CDVPluginResult* pluginResult;
        if ([errorMsg length] != 0) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMsg];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    // }
}


- (void)cleanup {
    NSLog(@"Clean up called");
    
    // Remove playback finished listener
    [[NSNotificationCenter defaultCenter]
     removeObserver:self
     name:AVPlayerItemDidPlayToEndTimeNotification
     object:avPlayer.currentItem];
    // Remove playback finished error listener
    [[NSNotificationCenter defaultCenter]
     removeObserver:self
     name:AVPlayerItemFailedToPlayToEndTimeNotification
     object:avPlayer.currentItem];
    
    if (avPlayer.rate != 0.0) {
        [avPlayer pause];
        avPlayer = nil;
    }
}
@end
