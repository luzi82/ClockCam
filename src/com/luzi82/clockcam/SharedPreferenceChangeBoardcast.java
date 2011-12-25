package com.luzi82.clockcam;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class SharedPreferenceChangeBoardcast implements OnSharedPreferenceChangeListener {

	public static final String PREFERENCE_CMD = "com.luzi82.clockcam.preference";
	public static final String PREFERENCE_CMD_KEY = "com.luzi82.clockcam.preference.key";

	WeakReference<Context> mContextRef;
	WeakReference<SharedPreferences> mSharedPreferencesRef;

	SharedPreferenceChangeBoardcast(Context c) {
		mContextRef = new WeakReference<Context>(c);
	}

	public void register(SharedPreferences sp) {
		unRegister();
		sp.registerOnSharedPreferenceChangeListener(this);
		mSharedPreferencesRef = new WeakReference<SharedPreferences>(sp);
	}

	public void unRegister() {
		if (mSharedPreferencesRef != null) {
			SharedPreferences sp = mSharedPreferencesRef.get();
			if (sp != null) {
				sp.unregisterOnSharedPreferenceChangeListener(this);
			}
		}
		mSharedPreferencesRef = null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		ClockCamActivity.d("onSharedPreferenceChanged "+key);
		Context c = mContextRef.get();
		if (c == null) {
			unRegister();
			return;
		}
		Intent intent = new Intent(PREFERENCE_CMD);
		intent.putExtra(PREFERENCE_CMD_KEY, key);
		c.sendBroadcast(intent);
	}

}
