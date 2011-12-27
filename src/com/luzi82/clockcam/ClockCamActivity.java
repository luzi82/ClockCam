package com.luzi82.clockcam;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
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
	private PreferenceSummaryUpdate mPreferenceSummaryUpdate = new PreferenceSummaryUpdate(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		getPreferenceManager().setSharedPreferencesName(PREFERENCE_NAME);
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

		mPreferenceSummaryUpdate.addKey("preference_setting_timerperiod");
		mPreferenceSummaryUpdate.addKey("preference_setting_photo_size");
		mPreferenceSummaryUpdate.addKey("preference_setting_photo_whitebalance");
		mPreferenceSummaryUpdate.addKey("preference_setting_storage_path");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ftp_domain");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ftp_port");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ftp_login");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ftp_remotepath");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ntp_domain");
		mPreferenceSummaryUpdate.addKey("preference_setting_upload_ntp_port");

		// initValue(getPreferenceManager().getSharedPreferences());
		setDefaultValue();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		mSharedPreferenceChangeBoardcast.register(sp);
		mPreferenceSummaryUpdate.register();
		mPreferenceSummaryUpdate.updateAll();
		startService(new Intent(this, ClockCamService.class));
	}

	@Override
	protected void onPause() {
		mSharedPreferenceChangeBoardcast.unRegister();
		mPreferenceSummaryUpdate.unRegister();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mSharedPreferenceChangeBoardcast.unRegister();
		mPreferenceSummaryUpdate.unRegister();
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

	public void setDefaultValue() {
		ListPreference preference_setting_photo_size_p = (ListPreference) findPreference("preference_setting_photo_size");
		if (preference_setting_photo_size_p.getValue() == null) {
			preference_setting_photo_size_p.setValueIndex(0);
		}

		ListPreference preference_setting_photo_whitebalance_p = (ListPreference) findPreference("preference_setting_photo_whitebalance");
		if (preference_setting_photo_whitebalance_p.getValue() == null) {
			preference_setting_photo_whitebalance_p.setValueIndex(0);
		}

		EditTextPreference preference_setting_storage_path_p = (EditTextPreference) findPreference("preference_setting_storage_path");
		if (preference_setting_storage_path_p.getText() == null) {
			File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ClockCam/");
			preference_setting_storage_path_p.setText(f.getAbsolutePath());
		}
	}
}