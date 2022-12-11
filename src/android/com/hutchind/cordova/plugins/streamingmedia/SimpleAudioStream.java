package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.MediaController;

public class SimpleAudioStream implements
MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener,
MediaController.MediaPlayerControl {

	private String TAG = getClass().getSimpleName();
	private MediaPlayer mMediaPlayer = null;
	private Boolean mShouldAutoClose = true;

	// @Override
	// public void onCreate(Bundle icicle) {
	// 	super.onCreate(icicle);
	// 	Bundle b = getIntent().getExtras();
	// 	mAudioUrl = b.getString("mediaUrl");
	// 	mShouldAutoClose = b.getBoolean("shouldAutoClose", true);

	// 	play();
	// }

	public void play(String mAudioUrl) {
		// Uri myUri = Uri.parse(mAudioUrl);
		try {
			if (mMediaPlayer == null) {
				mMediaPlayer = new MediaPlayer();
			} else {
				try {
					mMediaPlayer.stop();
					mMediaPlayer.reset();
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
			mMediaPlayer.setDataSource(mAudioUrl); // Go to Initialized state
			// mMediaPlayer.setAudioAttributes(AudioAttributes.USAGE_MEDIA);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnPreparedListener(this::onPrepared);
			mMediaPlayer.setOnCompletionListener(this::onCompletion);
			mMediaPlayer.setOnBufferingUpdateListener(this::onBufferingUpdate);
			mMediaPlayer.setOnErrorListener(this::onError);
			mMediaPlayer.setScreenOnWhilePlaying(true);

			mMediaPlayer.prepareAsync();

			Log.d(TAG, "LoadClip Done");
		} catch (Throwable t) {
			Log.d(TAG, t.toString());
		}
	}

	// @Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Stream is prepared");
		mMediaPlayer.start();
	}

	// @Override
	public void start() {
		if (mMediaPlayer!=null) {
			mMediaPlayer.start();
		}
	}

	// @Override
	public void pause() {
		if (mMediaPlayer!=null) {
			try {
				mMediaPlayer.pause();
			} catch (Exception e) {
				Log.d(TAG, e.toString());
			}
		}
	}

	public void stop() {
		if (mMediaPlayer!=null) {
			try {
				mMediaPlayer.stop();
			} catch (Exception e) {
				Log.d(TAG, e.toString());
			}
		}
	}

	public int getDuration() {
		return (mMediaPlayer!=null) ? mMediaPlayer.getDuration() : 0;
	}

	public int getCurrentPosition() {
		return (mMediaPlayer!=null) ? mMediaPlayer.getCurrentPosition() : 0;
	}

	public void seekTo(int i) {
		if (mMediaPlayer!=null) {
			mMediaPlayer.seekTo(i);
		}
	}

	public boolean isPlaying() {
		if (mMediaPlayer!=null) {
			try {
				return mMediaPlayer.isPlaying();
			} catch (Exception e) {
				Log.d(TAG, e.toString());
			}
		}
		return false;
	}

	public int getBufferPercentage() {
		return 0;
	}

	public boolean canPause() {
		return true;
	}

	public boolean canSeekBackward() {
		return true;
	}

	public boolean canSeekForward() {
		return true;
	}

	// @Override
	public int getAudioSessionId() {
		return 0;
	}

	// @Override
	public void onDestroy() {
		// super.onDestroy();
		if (mMediaPlayer!=null){
			try {
				mMediaPlayer.reset();
				mMediaPlayer.release();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			mMediaPlayer = null;
		}
	}

	private void wrapItUp(int resultCode, String message) {
		Intent intent = new Intent();
		intent.putExtra("message", message);
		// setResult(resultCode, intent);
		// finish();
	}


	// @Override
	public void onCompletion(MediaPlayer mp) {
		stop();
		if (mShouldAutoClose) {
			Log.v(TAG, "FINISHING ACTIVITY");
			// wrapItUp(RESULT_OK, null);
		}

	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		StringBuilder sb = new StringBuilder();
		sb.append("Media Player Error: ");
		switch (what) {
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			sb.append("Not Valid for Progressive Playback");
			break;
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			sb.append("Server Died");
			break;
			case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			sb.append("Unknown");
			break;
			default:
			sb.append(" Non standard (");
			sb.append(what);
			sb.append(")");
		}
		sb.append(" (" + what + ") ");
		sb.append(extra);
		Log.e(TAG, sb.toString());
		// wrapItUp(RESULT_CANCELED, sb.toString());
		return true;
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(TAG, "PlayerService onBufferingUpdate : " + percent + "%");
	}

	// @Override
	public void onBackPressed() {
		// wrapItUp(RESULT_OK, null);
	}

	// @Override
	public void onConfigurationChanged(Configuration newConfig) {
		// super.onConfigurationChanged(newConfig);
	}

	// @Override
	public boolean onTouchEvent(MotionEvent event) {
	// if (mMediaController != null) {
	// 	mMediaController.show();
	// }
	return false;
	}
}
