package com.luzi82.clockcam;

import java.util.regex.Pattern;

import android.content.SharedPreferences;

public class ClockCamPreference {

	boolean preference_setting_mode;
	long preference_setting_timerperiod;
	int preference_setting_photo_size_width;
	int preference_setting_photo_size_height;
	String preference_setting_photo_whitebalance;

	String preference_setting_storage_path;

	boolean preference_setting_upload_ftp_enable;
	String preference_setting_upload_ftp_domain;
	int preference_setting_upload_ftp_port;
	String preference_setting_upload_ftp_login;
	String preference_setting_upload_ftp_password;
	String preference_setting_upload_ftp_remotepath;

	boolean preference_setting_upload_ntp_enable;
	String preference_setting_upload_ntp_domain;
	int preference_setting_upload_ntp_port;

	public static ClockCamPreference get(SharedPreferences aSharedPreferences) {
		ClockCamPreference ret = new ClockCamPreference();
		ret.read(aSharedPreferences);
		return ret;
	}

	void read(SharedPreferences aSharedPreferences) {
		preference_setting_mode = aSharedPreferences.getBoolean("preference_setting_mode", false);

		preference_setting_timerperiod = readInt(aSharedPreferences, "preference_setting_timerperiod", -1)*1000;
		int[] preference_setting_photo_size_v = readSize(aSharedPreferences, "preference_setting_photo_size", -1, -1);
		preference_setting_photo_size_width = preference_setting_photo_size_v[0];
		preference_setting_photo_size_height = preference_setting_photo_size_v[1];
		preference_setting_photo_whitebalance = aSharedPreferences.getString("preference_setting_photo_whitebalance", null);

		preference_setting_storage_path = aSharedPreferences.getString("preference_setting_storage_path", null);

		preference_setting_upload_ftp_enable = aSharedPreferences.getBoolean("preference_setting_upload_ftp_enable", false);
		preference_setting_upload_ftp_domain = aSharedPreferences.getString("preference_setting_upload_ftp_domain", null);
		preference_setting_upload_ftp_port = readInt(aSharedPreferences, "preference_setting_upload_ftp_port", -1);
		preference_setting_upload_ftp_login = aSharedPreferences.getString("preference_setting_upload_ftp_login", null);
		preference_setting_upload_ftp_password = aSharedPreferences.getString("preference_setting_upload_ftp_password", null);
		preference_setting_upload_ftp_remotepath = aSharedPreferences.getString("preference_setting_upload_ftp_remotepath", null);

		preference_setting_upload_ntp_enable = aSharedPreferences.getBoolean("preference_setting_upload_ntp_enable", false);
		preference_setting_upload_ntp_domain = aSharedPreferences.getString("preference_setting_upload_ntp_domain", null);
		preference_setting_upload_ntp_port = readInt(aSharedPreferences, "preference_setting_upload_ntp_port", -1);
	}

	static int readInt(SharedPreferences aSharedPreferences, String aKey, int aDefault) {
		String v = aSharedPreferences.getString(aKey, null);
		if (v != null) {
			try {
				return Integer.parseInt(v);
			} catch (NumberFormatException nfe) {
			}
		}
		return aDefault;
	}

	static int[] readSize(SharedPreferences aSharedPreferences, String aKey, int aDefaultW, int aDefaultH) {
		String v = aSharedPreferences.getString(aKey, null);
		if (v != null) {
			String[] vv = v.split(Pattern.quote("x"));
			if (vv.length == 2) {
				try {
					int w = Integer.parseInt(vv[0]);
					int h = Integer.parseInt(vv[1]);
					return new int[] { w, h };
				} catch (NumberFormatException nfe) {
				}
			}
		}
		return new int[] { aDefaultW, aDefaultH };
	}

}
