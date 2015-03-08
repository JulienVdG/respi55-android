/* Respi 5/5
 *
 * Copyright 2014-2015 Julien Viard de Galbert
 *
 * This file is part of Respi 5/5.
 *
 * Respi 5/5 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Respi 5/5 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Respi 5/5.  If not, see <http://www.gnu.org/licenses/>.
 */
package name.vdg.respi55;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class RespiSound extends BroadcastReceiver {
	private static final String TAG = "RespiSound";

	private RespiStateManager mRespiStateManager;
	private AudioManager mAudioManager;
	private SoundPool soundPool;
	private int tick[] = new int[2], endSound, sound5sec;
	private ConcurrentMap<Integer,Boolean> loaded = new ConcurrentHashMap<Integer,Boolean>();
	Handler mHandler = new Handler();
	/** current sound states */
	private boolean mPlaySound = false;
	private boolean mActiveSound = false;
	/** config sound states */
	private long mSoundMode;
	private boolean mEnableTicks;
	private boolean mEnable5s;
	/** headset states */
	private boolean mWiredHeadsetOn = false;
	private boolean mBtHeadsetOn = false;


	public RespiSound(RespiStateManager respiStateManager) {
		mRespiStateManager = respiStateManager;
		Context context = respiStateManager;

		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		// Load the sounds
		soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				if (status == 0)
					loaded.put(sampleId, true);
			}
		});

		tick[0] = soundPool.load(context, R.raw.tick_even, 1);
		loaded.putIfAbsent(tick[0], false);
		tick[1] = soundPool.load(context, R.raw.tick_odd, 1);
		loaded.putIfAbsent(tick[1], false);
		endSound = soundPool.load(context, R.raw.gong, 1);
		loaded.putIfAbsent(endSound, false);
		sound5sec = soundPool.load(context, R.raw.ding, 1);
		loaded.putIfAbsent(sound5sec, false);

		// listen to headset broadcast intents
		setupIntentFilters(context);
	}


	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setupIntentFilters(Context context)
	{
		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
		filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		context.registerReceiver(this, filter);

		// TODO unregisterReceiver somewhere
	}

	@Override public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
			int state = intent.getIntExtra("state", -1);
			switch (state) {
			case 0:
				Log.d(TAG, "Headset is unplugged");
				mWiredHeadsetOn = false;
				break;
			case 1:
				Log.d(TAG, "Headset is plugged");
				mWiredHeadsetOn = true;
				break;
			default:
				Log.d(TAG, "I have no idea what the headset state is");
			}
			if (mSoundMode == 2) updateEnableSound();
		}
		if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(intent.getAction())) {
			int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE , -1);
			switch (state) {
			case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
				Log.d(TAG, "BT Audio is disconnected");
				mBtHeadsetOn = false;
				break;
			case AudioManager.SCO_AUDIO_STATE_CONNECTED:
				Log.d(TAG, "BT Audio is connected");
				mBtHeadsetOn = true;
				break;
			default:
				Log.d(TAG, "I have no idea what the headset state is");
			}
			if (mSoundMode == 2) updateEnableSound();
		}
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			mWiredHeadsetOn = false;
			mBtHeadsetOn = false;
			if (mSoundMode == 2) updateEnableSound();
		}

	}

	private boolean isSoundEnabled()
	{
		boolean es = false;
		switch ((int)mSoundMode) {
		case 0: es = false; break;
		case 1: es = true; break;
		case 2: es = mBtHeadsetOn || mWiredHeadsetOn; break;
		}
		return es;
	}

	private boolean tryActiveSound()
	{
		// Request audio focus for playback
		int result = mAudioManager.requestAudioFocus(afChangeListener,
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			// Start playback.
			// First Tick
			update(true);
			return true;
		}
		return false;
	}

	private void updateEnableSound()
	{
		boolean es=isSoundEnabled();
		// if started, update sound state
		if (mPlaySound) {
			if (es && !mActiveSound) {
				tryActiveSound();
			}
			if (!es && mActiveSound) {
				update(false);
				mAudioManager.abandonAudioFocus(afChangeListener);
			}
		}
	}

	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			Log.d(TAG, "AudioFocusChange="+focusChange);

			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
				// Pause playback
				update(false);
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				// Resume playback
				update(true);
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				mAudioManager.abandonAudioFocus(afChangeListener);
				// Stop playback
				update(false);
			}
		}
	};

	private void update(boolean activeSound)
	{
		if (activeSound && !mActiveSound) {
			startEnd();
			startTicks();
		}
		if (!activeSound && mActiveSound) {
			stopEnd();
			stopTicks();
		}
		mActiveSound = activeSound;
	}

	public void start(long soundMode, boolean enableTicks, boolean enable5s)
	{
		mPlaySound = true;
		mSoundMode = soundMode;
		mEnableTicks = enableTicks;
		mEnable5s = enable5s;

		if (isSoundEnabled()) {
			if (tryActiveSound()) {
				// First Tick
				if (mEnableTicks) playTick(0);
			}
		}
	}

	public void stop()
	{
		mPlaySound = false;
		update(false);
		mAudioManager.abandonAudioFocus(afChangeListener);
	}

	public void updateSoundMode(long soundMode)
	{
		if (!mPlaySound) return;
		mSoundMode = soundMode;
		updateEnableSound();
	}

	public void updateTicks(boolean enableTicks)
	{
		if (!mPlaySound) return;
		boolean start = (enableTicks && !mEnableTicks);
		boolean stop = (!enableTicks && mEnableTicks);
		if (enableTicks != mEnableTicks && mEnable5s) {
			long elapsed = System.nanoTime() - mRespiStateManager.getStartTime();
			elapsed += 10000000; // add 10 ms to round up some timing errors
			long sec = (elapsed/1000000000);
			if(sec%5 == 5) {
				// next event is in less than 5 -> no need to reschedule
				stop = false;
				start = false;
			} else {
				// force stop & start to reschedule
				stop = true;
				start = true;
			}
		}
		mEnableTicks = enableTicks;
		if (stop) stopTicks();
		if (start) startTicks();
	}

	public void update5s(boolean enable5s)
	{
		if (!mPlaySound) return;
		boolean start = (enable5s && !mEnable5s && !mEnableTicks);
		boolean stop = (!enable5s && mEnable5s && !mEnableTicks);
		mEnable5s = enable5s;
		if (stop) stopTicks();
		if (start) startTicks();
	}

	/* Play control */

	public void playTick(int oddEven)
	{
		int soundID = tick[oddEven%2];
		if (loaded.get(soundID))
			soundPool.play(soundID, 1, 1, 1, 0, 1f);
	}

	public void playEnd()
	{
		if (loaded.get(endSound))
			soundPool.play(endSound, 1, 1, 1, 0, 1f);
	}

	public void play5s()
	{
		if (loaded.get(sound5sec))
			soundPool.play(sound5sec, 1, 1, 1, 0, 1f);
	}

	/* schedule control */

	private void schedule(Runnable runable, long milliPerriod)
	{
		if (mRespiStateManager.isStarted())
		{
			// start in milli from nano
			long start = mRespiStateManager.getStartTime()/1000000;
			//
			long now = SystemClock.uptimeMillis();
			// compute next date based on nanoClock
			long date = start + (((now - start) / milliPerriod) + 1 ) * milliPerriod;
			mHandler.postAtTime(runable, date);
		}
	}

	Runnable mTickRunnable = new Runnable() {
		@Override
		public void run() {
			long elapsed = System.nanoTime() - mRespiStateManager.getStartTime();
			elapsed += 10000000; // add 10 ms to round up some timing errors
			long sec = (elapsed/1000000000);
			int oddEven=-1;
			if (mEnableTicks) {
				oddEven=(int) (sec % 2);
				playTick(oddEven);
			}
			if (mEnable5s && (sec % 5 == 0 )) {
				play5s();
			}
			startTicks();
			Log.d(TAG, "mTickRunnable "+oddEven+" s="+sec+" e="+elapsed);
		}
	};

	private void startTicks()
	{
		if (mEnableTicks) {
			schedule(mTickRunnable, 1000);
		} else if (mEnable5s) {
			schedule(mTickRunnable, 5*1000);
		}
	}

	private void stopTicks()
	{
		mHandler.removeCallbacks(mTickRunnable);
	}

	Runnable mEndRunnable = new Runnable() {
		@Override
		public void run() {
			playEnd();
			startEnd();
		}
	};

	private void startEnd()
	{
		schedule(mEndRunnable, 5 * 60 * 1000);
	}

	private void stopEnd()
	{
		mHandler.removeCallbacks(mEndRunnable);
	}


}
