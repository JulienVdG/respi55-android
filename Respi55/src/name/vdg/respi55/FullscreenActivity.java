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

import name.vdg.respi55.util.SystemUiHider;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	private static final String TAG = "FullscreenActivity";

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	/**
	 * The instance of the {@link RespiStateManager} for this activity.
	 */
	private RespiStateManager mRespiStateManager;

	/**
	 * The instance of the {@link RespiView} for this activity.
	 */
	private RespiView mRespiView;

	/**
	 * The start/stop button
	 */
	private Button mButton;
	private boolean mStarted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.activity_fullscreen);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		mRespiView = (RespiView) findViewById(R.id.fullscreen_content);
		mButton = (Button) findViewById(R.id.startstop_button);


		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, mRespiView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
		.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				// was HONEYCOMB_MR2 but the animate does not seam to work nice on ICS so...
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(
								android.R.integer.config_shortAnimTime);
					}
					controlsView.animate()
					.translationY(visible ? 0 : mControlsHeight)
					.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					if (visible) {
						getActionBar().show();
					} else {
						getActionBar().hide();
					}
				}
				if (visible && AUTO_HIDE && mStarted) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		mRespiView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK && mStarted) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		mButton.setOnTouchListener(mDelayHideTouchListener);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		Intent intent= new Intent(this, RespiStateManager.class);
		bindService(intent, mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		unbindService(mConnection);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, 
				IBinder binder) {
			Log.d(TAG, "onServiceConnected");
			RespiStateManager.LocalBinder b = (RespiStateManager.LocalBinder) binder;
			mRespiStateManager = b.getService();
			mRespiView.setRespiStateManager(mRespiStateManager);
			updateButtonState(mRespiStateManager.isStarted());
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected");
			mRespiStateManager = null;
			mRespiView.setRespiStateManager(mRespiStateManager);
		}
	};

	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.d(TAG, "onWindowFocusChanged");
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && mRespiStateManager != null) {
			mRespiStateManager.setAudioFocus(this);
			updateButtonState(mRespiStateManager.isStarted());
		}
	}

	private void updateButtonState(boolean started) {
		if (started != mStarted) {
			mStarted = started;
			if (started) {
				mButton.setText(R.string.stop_button);
				delayedHide(100);
			} else {
				mButton.setText(R.string.start_button);
				cancelHide();
			}
		}
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	// No direct action here. The event is never handled (onTouch returns false).
	@SuppressLint("ClickableViewAccessibility")
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			if (mStarted) {
				mSystemUiHider.hide();
			}
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	/**
	 * Canceling any previously scheduled calls and call show now!
	 */
	private void cancelHide() {
		mHideHandler.removeCallbacks(mHideRunnable);
		mSystemUiHider.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		showSettingsActionIfNoMenuKey(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void showSettingsActionIfNoMenuKey(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
				menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_settings:
			openSettings();
			return true;
		case R.id.action_help:
			openText(R.string.action_help, R.raw.help);
			return true;
		case R.id.action_about:
			openText(R.string.action_about, R.raw.about);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings()
	{
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void openText(int titleId, int htmlId)
	{
		Intent intent = new Intent(this, TextActivity.class);
		intent.putExtra(TextActivity.EXTRA_TITLE_ID, titleId);
		intent.putExtra(TextActivity.EXTRA_RAW_HTML_ID, htmlId);
		startActivity(intent);
	}

	/** Called when the user clicks the StartStop button */
	public void onStartStop(View view) {
		Log.d(TAG, "onStartStop "+mStarted);
		// Do something in response to button
		Intent intent = new Intent(this, RespiStateManager.class);
		if (mStarted) {
			// Stop it
			intent.putExtra(RespiStateManager.EXTRA_CMD_ID, RespiStateManager.Command.Stop);
			startService(intent);

			updateButtonState(false);
		} else {
			// Start it
			intent.putExtra(RespiStateManager.EXTRA_CMD_ID, RespiStateManager.Command.Start);
			startService(intent);

			updateButtonState(true);
		}
	}
}
