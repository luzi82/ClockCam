package com.luzi82.clockcam;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class TimeManager {

	WeakReference<Context> mContext;
	long mOffset = 0;

	Timer mTimer;

	boolean mShouldRun = false;

	String mDomain = null;
	int mPort = -1;

	boolean mReceiverReg = false;

	// long UPDATE_RATE=60*60*1000;
	// long UPDATE_RATE = 10 * 1000;
	long UPDATE_RATE = 10 * 60 * 1000;
	int SAMPLE = 5;
	int MAX_FAIL = 10;

	public TimeManager(Context aContext) {
		mContext = new WeakReference<Context>(aContext);
	}

	synchronized void start() {
		if (mContext == null)
			return;
		Context c = mContext.get();
		if (c == null)
			return;
		if (mShouldRun)
			return;
		mShouldRun = true;

		restartTimer();

		if (!mReceiverReg) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
			intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
			intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			c.registerReceiver(mTimeChangeReceiver, intentFilter);
		}
		mReceiverReg = true;

		ClockCamActivity.d("START");
	}

	synchronized void stop() {
		if (mReceiverReg) {
			if (mContext != null) {
				Context c = mContext.get();
				if (c != null) {
					c.unregisterReceiver(mTimeChangeReceiver);
				}
			}
		}
		mReceiverReg = false;
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
			mCurrentTT = null;
		}
		mOffset = 0;
		mShouldRun = false;
	}

	synchronized void close() {
		stop();
		mContext = null;
	}

	class TT extends TimerTask {
		@Override
		public void run() {
			try {
				String domain = null;
				int port = -1;
				synchronized (TimeManager.this) {
					if (!mShouldRun)
						return;
					domain = mDomain;
					port = mPort;
				}
				if ((domain != null) && (port != -1)) {
					NTPUDPClient ntp = new NTPUDPClient();
					ntp.setDefaultTimeout(10000);
					ntp.open();
					long sum = 0;
					int sample = 0;
					int fail = 0;
					while (true) {
						try {
							TimeInfo ti = ntp.getTime(InetAddress.getByName(domain), port);
							ti.computeDetails();
							Long offset = ti.getOffset();
							if (offset != null) {
								sum += offset;
								++sample;
							} else {
								++fail;
							}
						} catch (Throwable t) {
							++fail;
						}
						if (sample > SAMPLE)
							break;
						if (fail > MAX_FAIL)
							break;
					}
					if (sample > 0) {
						synchronized (TimeManager.this) {
							if (mCurrentTT == this) {
								mOffset = sum / sample;
								ClockCamActivity.d("mOffset " + mOffset);
							}
						}
					}
				}
			} catch (Throwable t) {
			}
		}
	}

	TT mCurrentTT = null;

	private BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (TimeManager.this) {
				mOffset = 0;
				if (mShouldRun) {
					restartTimer();
				}
			}
		}
	};

	private void restartTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
			mCurrentTT = null;
		}
		mCurrentTT = new TT();
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(mCurrentTT, 0, UPDATE_RATE);
	}

	public synchronized void setDomain(String aDomain) {
		mDomain = aDomain;
	}

	public synchronized void setPort(int aPort) {
		mPort = aPort;
	}

	public synchronized long currentRealTime() {
		return toRealTime(System.currentTimeMillis());
	}

	public synchronized long toRealTime(long aDeviceTime) {
		return aDeviceTime + mOffset;
	}

	public synchronized long toDeviceTime(long aRealTime) {
		return aRealTime - mOffset;
	}

}
