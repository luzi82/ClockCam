package com.luzi82.clockcam;

import java.io.IOException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class ClockCamActivity extends PreferenceActivity {

	public static final String TAG = "ClockCam";
	public static final String PREFERENCE_NAME = "PREF";

	public CameraInfo mCameraInfo = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		getPreferenceManager().setSharedPreferencesName(
				PREFERENCE_NAME);
		initValue(getPreferenceManager().getSharedPreferences());
		addPreferencesFromResource(R.xml.preferences);
//		getPreferenceManager().getSharedPreferences()
//				.registerOnSharedPreferenceChangeListener(this);
		
//		// UI
//		setContentView(R.layout.main);
//
//		Button startButton = (Button) findViewById(R.id.startButton);
//		startButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				serviceStart(v);
//			}
//		});
//
//		Button stopButton = (Button) findViewById(R.id.stopButton);
//		stopButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				serviceStop(v);
//			}
//		});

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

		startService(new Intent(this, ClockCamService.class));
	}

	@Override
	protected void onDestroy() {
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