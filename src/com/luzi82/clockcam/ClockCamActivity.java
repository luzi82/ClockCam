package com.luzi82.clockcam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ClockCamActivity extends Activity {

	public static final String TAG = "ClockCam";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				serviceStart(v);
			}
		});

		Button stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				serviceStop(v);
			}
		});

		startService(new Intent(this, ClockCamService.class));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

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

}