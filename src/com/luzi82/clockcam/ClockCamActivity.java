package com.luzi82.clockcam;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.luzi82.clockcam.CameraInfo.Size;

public class ClockCamActivity extends PreferenceActivity {

	public static final String TAG = "ClockCam";
	public static final String PREFERENCE_NAME = "PREF";

	public CameraInfo mCameraInfo = null;

	public SharedPreferenceChangeBoardcast mSharedPreferenceChangeBoardcast = new SharedPreferenceChangeBoardcast(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		getPreferenceManager().setSharedPreferencesName(PREFERENCE_NAME);
		initValue(getPreferenceManager().getSharedPreferences());
		addPreferencesFromResource(R.xml.preferences);

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

	static int d(String msg) {
		return Log.d(TAG, msg);
	}

	static public void initValue(SharedPreferences sp) {
		String preference_setting_storage_path = sp.getString("preference_setting_storage_path", null);

		SharedPreferences.Editor editor = sp.edit();
		if (preference_setting_storage_path == null) {
			File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ClockCam/");
			editor.putString("preference_setting_storage_path", f.getAbsolutePath());
		}
		editor.commit();
	}

}