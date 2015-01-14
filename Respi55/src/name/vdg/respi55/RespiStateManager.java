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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class RespiStateManager implements OnSharedPreferenceChangeListener {
	private static final String TAG = "RespiStateManager";
	private static final int NOTIFICATION_ID = R.string.notification_message;
	/**
	 * The View
	 */
	private RespiView mRespiView;

	/**
	 * The Sound manager
	 */
	private RespiSound mRespiSound;

	/** the notification */
	private NotificationCompat.Builder mNotificationBuilder;
	private NotificationManager mNM;

	/** config display states */
	private boolean mDisplayDigits;
	/** config sound states */
	private long mSoundMode = 0;
	private boolean mEnableTicks;
	private boolean mEnable5s;

	/** activity states */
	private boolean mStarted = false;
	private long mStartTime = Long.MAX_VALUE;

	private static RespiStateManager holder = null;
	public static RespiStateManager getInstance(Context context) {
		if (null == holder)
			holder = new RespiStateManager(context.getApplicationContext());
		return holder;
	}


	public RespiStateManager(Context context) {
		mRespiSound = new RespiSound(context);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		// Display settings
		onSharedPreferenceChanged(sp, SettingsActivity.KEY_DISPLAY_DIGITS);

		// Sound settings
		onSharedPreferenceChanged(sp, SettingsActivity.KEY_SOUND_ENABLE);
		onSharedPreferenceChanged(sp, SettingsActivity.KEY_ENABLE_TICKS);		
		onSharedPreferenceChanged(sp, SettingsActivity.KEY_ENABLE_5S);

		// listen to preference change
		sp.registerOnSharedPreferenceChangeListener(this);

		setupNotification(context);
	}

	public void setRespiView(RespiView respiView)
	{
		Log.d(TAG, "setRespiView " + respiView);
		mRespiView = respiView;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if(key.equals(SettingsActivity.KEY_DISPLAY_DIGITS)) {
			mDisplayDigits = sp.getBoolean(SettingsActivity.KEY_DISPLAY_DIGITS, true);
		}
		else if(key.equals(SettingsActivity.KEY_SOUND_ENABLE)) {
			try {
				mSoundMode = Integer.parseInt(sp.getString(SettingsActivity.KEY_SOUND_ENABLE, "1"));
			} catch (NumberFormatException e) {
				// if not a number, enable to be sure
				mSoundMode = 1;
			}
			mRespiSound.updateSoundMode(mSoundMode);
		}
		if(key.equals(SettingsActivity.KEY_ENABLE_TICKS)) {
			boolean et = sp.getBoolean(SettingsActivity.KEY_ENABLE_TICKS, true);
			// if started, update sound state
			if (mStarted) {
				if (et != mEnableTicks) mRespiSound.updateTicks(et);
			}
			mEnableTicks = et;
		}
		if(key.equals(SettingsActivity.KEY_ENABLE_5S)) {
			boolean e5 = sp.getBoolean(SettingsActivity.KEY_ENABLE_5S, true);
			// if started, update sound state
			if (mStarted) {
				if (e5 != mEnable5s) mRespiSound.update5s(e5);
			}
			mEnable5s = e5;
		}

	}

	private void setupNotification(Context context)
	{
		mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Instantiate a Builder object.
		mNotificationBuilder = new NotificationCompat.Builder(context);
		// Creates an Intent for the Activity
		PendingIntent pi = PendingIntent.getActivity(context, 0,
	                new Intent(context, FullscreenActivity.class),
	                PendingIntent.FLAG_UPDATE_CURRENT);
		// Puts the PendingIntent into the notification builder
		mNotificationBuilder.setContentIntent(pi);
		mNotificationBuilder.setContentTitle(context.getResources().getString(R.string.app_name));
		mNotificationBuilder.setContentText(context.getResources().getString(R.string.notification_message));
		mNotificationBuilder.setSmallIcon(R.drawable.ic_stat_started);
		mNotificationBuilder.setOngoing(true);
	}

	public static long getStartTime() {
		if (null == holder) return Long.MAX_VALUE;
		return holder.mStartTime;
	}


	public static boolean isStarted() {
		if (null == holder) return false;
		return holder.mStarted;
	}

	public void start()
	{
		mStarted = true;
		mStartTime = System.nanoTime();
		mRespiView.start();
		mRespiSound.start(mSoundMode, mEnableTicks, mEnable5s);
		// Send the notification.
		mNM.notify(NOTIFICATION_ID, mNotificationBuilder.build());
	}

	public void stop()
	{
		mStarted = false;
		mStartTime = Long.MAX_VALUE;
		mRespiView.stop();
		mRespiSound.stop();
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION_ID);
	}

	public void setAudioFocus(Activity a)
	{
		// Hardware buttons setting to adjust the media sound
		a.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Request audio focus depending on sound mode & started state
		mRespiSound.updateSoundMode(mSoundMode);
	}

	public static boolean isDigitDisplayed()
	{
		if (null == holder) return true;
		return holder.mDisplayDigits;
	}
}
