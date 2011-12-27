package com.luzi82.clockcam;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class PreferenceSummaryUpdate implements OnSharedPreferenceChangeListener {

	WeakReference<PreferenceActivity> mPreferenceActivityRef;
	Set<String> mKeySet = new HashSet<String>();

	public PreferenceSummaryUpdate(PreferenceActivity aPreferenceActivity) {
		mPreferenceActivityRef = new WeakReference<PreferenceActivity>(aPreferenceActivity);
	}

	public void register() {
		unRegister();
		SharedPreferences sp = getSharedPreferences();
		if (sp == null)
			return;
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	public void unRegister() {
		SharedPreferences sp = getSharedPreferences();
		if (sp == null)
			return;
		sp.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		PreferenceActivity pa = mPreferenceActivityRef.get();
		if (pa == null) {
			unRegister();
			return;
		}
		if (mKeySet.contains(key)) {
			updateSummary(pa, sharedPreferences, key);
		}
	}

	public void updateAll() {
		PreferenceActivity pa = mPreferenceActivityRef.get();
		SharedPreferences sp = getSharedPreferences();
		if (pa == null)
			return;
		if (sp == null)
			return;
		for (String k : mKeySet) {
			updateSummary(pa, sp, k);
		}
	}

	public void addKey(String aKey) {
		mKeySet.add(aKey);
	}

	private void updateSummary(PreferenceActivity pa, SharedPreferences sharedPreferences, String aKey) {
		Preference p = pa.findPreference(aKey);
		if (p == null)
			return;
		CharSequence v = null;
		if (p instanceof ListPreference) {
			ListPreference lp = (ListPreference) p;
			v = lp.getEntry();
		} else {
			v = sharedPreferences.getString(aKey, null);
		}
		if (v != null) {
			p.setSummary(v);
		} else {
			p.setSummary("");
		}
	}

	private SharedPreferences getSharedPreferences() {
		PreferenceActivity pa = mPreferenceActivityRef.get();
		if (pa == null)
			return null;
		SharedPreferences sp = pa.getPreferenceManager().getSharedPreferences();
		return sp;
	}

}
