package com.luzi82.clockcam;

import java.io.IOException;
import java.util.LinkedList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.luzi82.clockcam.CameraInfo.Size;

public class ClockCamActivity extends PreferenceActivity {

	public static final String TAG = "ClockCam";
	public static final String PREFERENCE_NAME = "PREF";

	public CameraInfo mCameraInfo = null;
	
	public SharedPreferenceChangeBoardcast mSharedPreferenceChangeBoardcast=new SharedPreferenceChangeBoardcast(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		getPreferenceManager().setSharedPreferencesName(PREFERENCE_NAME);
		initValue(getPreferenceManager().getSharedPreferences());
		addPreferencesFromResource(R.xml.preferences);
		// getPreferenceManager().getSharedPreferences()
		// .registerOnSharedPreferenceChangeListener(this);

		// // UI
		// setContentView(R.layout.main);
		//
		// Button startButton = (Button) findViewById(R.id.startButton);
		// startButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// serviceStart(v);
		// }
		// });
		//
		// Button stopButton = (Button) findViewById(R.id.stopButton);
		// stopButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// serviceStop(v);
		// }
		// });

		// init mCameraInfo
		if (mCameraInfo == null) {
			mCameraInfo = CameraInfo.readDefault(this);
		}
		if (mCameraInfo == null) {
			Camera camera = Camera.open();
			mCameraInfo = CameraInfo.readCamera(camera, CameraInfo.getVersion(this));
			camera.release();
			if (mCameraInfo != null) {
				try {
					mCameraInfo.writeDefault(this);
				} catch (IOException e) {
					throw new Error(e);
				}
			} else {
				throw new Error("cannot get camera info");
			}
		}

		// size
		LinkedList<String> entries = new LinkedList<String>();
		LinkedList<String> entryValues = new LinkedList<String>();
		for (Size s : mCameraInfo.mSupportedPictureSizes) {
			entries.add(String.format("%d × %d", s.mWidth, s.mHeight));
			entryValues.add(String.format("%dx%d", s.mWidth, s.mHeight));
		}
		ListPreference lp = (ListPreference) findPreference("preference_setting_photo_size");
		lp.setEntries(entries.toArray(new String[0]));
		lp.setEntryValues(entryValues.toArray(new String[0]));

		lp = (ListPreference) findPreference("preference_setting_photo_whitebalance");
		if ((mCameraInfo.mSupportedWhiteBalance != null) && (mCameraInfo.mSupportedWhiteBalance.length > 1)) {
			lp.setEntries(mCameraInfo.mSupportedWhiteBalance);
			lp.setEntryValues(mCameraInfo.mSupportedWhiteBalance);
		} else {
			lp.setEnabled(false);
			lp.setShouldDisableView(true);
		}

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSharedPreferenceChangeBoardcast.register(getPreferenceManager().getSharedPreferences());
		startService(new Intent(this, ClockCamService.class));
	}
	
	@Override
	protected void onPause() {
		mSharedPreferenceChangeBoardcast.unRegister();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mSharedPreferenceChangeBoardcast.unRegister();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_default, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_about: {
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// boolean mIsBound = false;
	// IClockCamService mService = null;
	//
	// private ServiceConnection mConnection = new ServiceConnection() {
	//
	// @Override
	// public void onServiceConnected(ComponentName aName, IBinder aService) {
	// mService = IClockCamService.Stub.asInterface(aService);
	// }
	//
	// @Override
	// public void onServiceDisconnected(ComponentName name) {
	// mService = null;
	// }
	//
	// };

	// private void doBindService() {
	// Intent i = new Intent();
	// i.setClassName("com.luzi82.clockcam",
	// "com.luzi82.clockcam.ClockCamService");
	// mIsBound = bindService(i, mConnection, BIND_AUTO_CREATE);
	// }
	//
	// private void doUnbindService() {
	// if (mIsBound) {
	// unbindService(mConnection);
	// mIsBound = false;
	// }
	// }

	public void serviceStart(View aView) {
		sendBroadcast(new Intent(ClockCamService.START_CMD));
	}

	public void serviceStop(View aView) {
		sendBroadcast(new Intent(ClockCamService.STOP_CMD));
	}

	static int d(String msg) {
		return Log.d(TAG, msg);
	}

	static public void initValue(SharedPreferences sp) {
		SharedPreferences.Editor editor = sp.edit();
		editor.commit();
	}

}