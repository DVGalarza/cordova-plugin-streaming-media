package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import android.media.AudioManager;

public class StreamingMedia extends CordovaPlugin {
	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PAUSE_AUDIO = "pauseAudio";
	public static final String ACTION_RESUME_AUDIO = "resumeAudio";
	public static final String ACTION_STOP_AUDIO = "stopAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";

	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	private CallbackContext callbackContext;

	private static final String TAG = "StreamingMediaPlugin";

	SimpleAudioStream audioStreamer = null;
    private int origVolumeStream = -1;

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}

		Log.d(TAG, "action called: " + action);

		switch(action) {
			case ACTION_PLAY_AUDIO: {
				Log.d(TAG, "Got an id: " + (String)options.get("id"));
				return playAudio(args.getString(0), options);
			}
			case ACTION_PAUSE_AUDIO: {
				return pauseAudio(args.getString(0), options);
			}
			case ACTION_RESUME_AUDIO: {
				return resumeAudio(args.getString(0), options);
			}
			case ACTION_STOP_AUDIO: {
				return stopAudio(args.getString(0), options);
			}
			case ACTION_PLAY_VIDEO: {
				return playVideo(args.getString(0), options);
			}
			default: {
				callbackContext.error("streamingMedia." + action + " is not a supported method.");
				return false;
			}
		}
	}

	private boolean playAudio(String url, JSONObject options) {
		SimpleAudioStream streamer = getOrCreatePlayer();
		if (audioStreamer != null) {
			streamer.play(url);
			return true;
		}
		return false;
		// return play(SimpleAudioStream.class, url, options);
	}
	private boolean pauseAudio(String url, JSONObject options) {
		if (audioStreamer != null) {
			audioStreamer.pause();
			return true;
		}
		return false;// play(SimpleAudioStream.class, url, options);
	}
	private boolean resumeAudio(String url, JSONObject options) {
		if (audioStreamer != null) {
			audioStreamer.start();
			return true;
		}
		return false;// play(SimpleAudioStream.class, url, options);
	}
	private boolean stopAudio(String url, JSONObject options) {
		if (audioStreamer != null) {
			audioStreamer.stop();
			return true;
		}
		return false;// play(SimpleAudioStream.class, url, options);
	}
	private boolean playVideo(String url, JSONObject options) {
		return play(SimpleVideoStream.class, url, options);
	}

	private boolean play(final Class activityClass, final String url, final JSONObject options) {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;

		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
				Bundle extras = new Bundle();
				extras.putString("mediaUrl", url);

				if (options != null) {
					Iterator<String> optKeys = options.keys();
					while (optKeys.hasNext()) {
						try {
							final String optKey = (String)optKeys.next();
							if (options.get(optKey).getClass().equals(String.class)) {
								extras.putString(optKey, (String)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Boolean.class)) {
								extras.putBoolean(optKey, (Boolean)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							}

						} catch (JSONException e) {
							Log.e(TAG, "JSONException while trying to read options. Skipping option.");
						}
					}
					streamIntent.putExtras(extras);
				}

				cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
			}
		});
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		super.onActivityResult(requestCode, resultCode, intent);
		if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
			if (Activity.RESULT_OK == resultCode) {
				this.callbackContext.success();
			} else if (Activity.RESULT_CANCELED == resultCode) {
				String errMsg = "Error";
				if (intent != null && intent.hasExtra("message")) {
					errMsg = intent.getStringExtra("message");
				}
				this.callbackContext.error(errMsg);
			}
		}
	}

	private SimpleAudioStream getOrCreatePlayer() {
		if (audioStreamer == null) {
			onFirstPlayerCreated();
			audioStreamer = new SimpleAudioStream();
		}
		return audioStreamer;
	}

    private void onFirstPlayerCreated() {
        origVolumeStream = cordova.getActivity().getVolumeControlStream();
        cordova.getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
}