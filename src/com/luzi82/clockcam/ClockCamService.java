package com.luzi82.clockcam;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;

public class ClockCamService extends Service {

	PowerManager mPowerManager;
	PowerManager.WakeLock mWakeLock;
	FtpManager mFtpManager;
	WifiManager mWifiManager;
	WifiLock mWifiLock;
	SharedPreferences mSharedPreferences;

	Camera mCamera;

	Timer mTimer;

	long mNextShot = -1;
	String mNextFilename = null;
	// long mPeriod = 15 * MIN;
	long mPeriod;
	long mPrepareTime = 5 * SEC;

	public static final String LOCAL_PATH = "/mnt/sdcard/DCIM/ClockCam/";

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ClockCamActivity.d("onReceive");
			String action = intent.getAction();
			if (action == null) {
			} else if (action.equals(SharedPreferenceChangeBoardcast.PREFERENCE_CMD)) {
				String param = intent.getStringExtra(SharedPreferenceChangeBoardcast.PREFERENCE_CMD_KEY);
				ClockCamActivity.d("PREFERENCE_CMD " + param);
				if (param == null) {
				} else if (param.equals("preference_setting_mode")) {
					boolean on = mSharedPreferences.getBoolean("preference_setting_mode", false);
					if (on) {
						startCam();
					} else {
						stopCam();
					}
				} else if (param.equals("preference_setting_photo_size") || param.equals("preference_setting_photo_whitebalance")) {
					if (mRunCamera) {
						setCameraParameters();
					}
				} else if (param.equals("preference_setting_timerperiod")) {
					if (mRunCamera) {
						synchronized (ClockCamService.this) {
							if (mNextTask != null) {
								mNextTask.cancel();
								mNextTask = null;
							}
							nextShot();
						}
					}
				}
			}
		}
	};

	@Override
	public IBinder onBind(Intent aIntent) {
		return null;
	}

	@Override
	public void onCreate() {
		ClockCamActivity.d("onDestroy");
		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ClockCamActivity.TAG);
		mWakeLock.setReferenceCounted(false);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, ClockCamActivity.TAG);
		mWifiLock.setReferenceCounted(false);

		mTimer = new Timer();

		File f = new File(LOCAL_PATH);
		f.mkdirs();

		mSharedPreferences = getSharedPreferences(ClockCamActivity.PREFERENCE_NAME, MODE_PRIVATE);

		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(SharedPreferenceChangeBoardcast.PREFERENCE_CMD);
		registerReceiver(mIntentReceiver, commandFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		ClockCamActivity.d("onDestroy");
		if (mTimer != null) {
			mTimer.cancel();
		}
		if (mCamera != null) {
			mCamera.release();
		}
		if (mWakeLock != null) {
			mWakeLock.release();
		}
		if (mWifiLock != null) {
			mWifiLock.release();
		}
		unregisterReceiver(mIntentReceiver);
	}

	boolean mRunCamera = false;

	public synchronized void startCam() {
		ClockCamActivity.d("startCam");
		if (mRunCamera == true) {
			return;
		}
		ClockCamActivity.d("startCam2");

		startForeground0();

		mCamera = Camera.open();
		mRunCamera = true;
		mWakeLock.acquire();
		mWifiLock.acquire();

		setCameraParameters();

		nextShot();
	}

	public synchronized void stopCam() {
		ClockCamActivity.d("stopCam");
		if (mRunCamera == false) {
			return;
		}
		ClockCamActivity.d("stopCam2");
		mRunCamera = false;
		mWakeLock.release();
		mWifiLock.release();
		if (mNextTask != null) {
			mNextTask.cancel();
			mNextTask = null;
		}
		if (mCameraState == CameraState.PREVIEW) {
			mNextTask = new StageEndTask();
			mTimer.schedule(mNextTask, 0);
		} else {
			ClockCamActivity.d("mCamera.release();");
			mCamera.release();
			mCamera = null;
		}
		// if (mFtpManager != null) {
		// mFtpManager.stopLater();
		// mFtpManager = null;
		// }
		stopForeground0();
	}

	enum CameraState {
		STOP, PREVIEW, SHOT, DONE,
	}

	CameraState mCameraState = CameraState.STOP;

	synchronized void nextShot() {
		if (!mRunCamera)
			return;
		String periodString = mSharedPreferences.getString("preference_setting_timerperiod", null);
		if (periodString != null) {
			mPeriod = Integer.parseInt(periodString) * SEC;
		} else {
			mPeriod = 60 * SEC;
		}
		mNextShot = (((System.currentTimeMillis()) / mPeriod) + 1) * mPeriod;
		mNextTask = new StagePrepareTask();
		mTimer.schedule(mNextTask, new Date(mNextShot - mPrepareTime));
	}

	TimerTask mNextTask = null;

	static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

	class StagePrepareTask extends TimerTask {
		@Override
		public void run() {
			synchronized (ClockCamService.this) {
				ClockCamActivity.d("StagePrepareTask");
				mNextTask = null;
				if (!mRunCamera)
					return;
				mCamera.startPreview();
				mNextTask = new StageShotTask();
				Date nextShotDate = new Date(mNextShot);
				mTimer.schedule(mNextTask, nextShotDate);
				mNextFilename = "/mnt/sdcard/DCIM/ClockCam/" + FILE_FORMAT.format(nextShotDate) + ".jpg";
				mCameraState = CameraState.PREVIEW;
			}
		}
	}

	class StageShotTask extends TimerTask {
		@Override
		public void run() {
			synchronized (ClockCamService.this) {
				ClockCamActivity.d("StageShotTask");
				mNextTask = null;
				if (!mRunCamera)
					return;
				mCamera.takePicture(null, null, mStatePicture);
				mCameraState = CameraState.SHOT;
			}
		}
	}

	PictureCallback mStatePicture = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			synchronized (ClockCamService.this) {
				ClockCamActivity.d("PictureCallback");
				if (mRunCamera) {
					ClockCamActivity.d(String.format("onPictureTaken %d", data.length));
					try {
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mNextFilename));
						bos.write(data);
						bos.flush();
						bos.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				mNextTask = new StageEndTask();
				mTimer.schedule(mNextTask, 0);
				mCameraState = CameraState.DONE;
			}
		}
	};

	class StageEndTask extends TimerTask {
		@Override
		public void run() {
			synchronized (ClockCamService.this) {
				ClockCamActivity.d("StageEndTask");
				mNextTask = null;
				if (mCameraState != CameraState.STOP) {
					mCamera.stopPreview();
					mCameraState = CameraState.STOP;
				}
				if (mRunCamera) {
					nextShot();
				} else {
					ClockCamActivity.d("mCamera.release();");
					mCamera.release();
					mCamera = null;
				}
			}
		}
	}

	static long SEC = 1000;
	static long MIN = 60 * SEC;

	void startForeground0() {
		Notification notification = new Notification(R.drawable.ic_launcher, "Clock Cam", System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ClockCamActivity.class), 0);
		notification.setLatestEventInfo(this, "Clock Cam", "Clock Cam", contentIntent);
		startForeground(100, notification);
	}

	void stopForeground0() {
		stopForeground(true);
	}

	void setCameraParameters() {
		if (mCamera == null)
			return;
		Camera.Parameters param = mCamera.getParameters();

		String sizeParam = mSharedPreferences.getString("preference_setting_photo_size", null);
		if (sizeParam != null) {
			String[] sizeParam2 = sizeParam.split(Pattern.quote("x"));
			if (sizeParam2.length == 2) {
				int w = Integer.parseInt(sizeParam2[0]);
				int h = Integer.parseInt(sizeParam2[1]);
				if ((w > 0) && (h > 0)) {
					param.setPictureSize(w, h);
				}
			}
		}

		String wbParam = mSharedPreferences.getString("preference_setting_photo_whitebalance", null);
		if (wbParam != null) {
			param.setWhiteBalance(wbParam);
		}

		mCamera.setParameters(param);
	}

}
