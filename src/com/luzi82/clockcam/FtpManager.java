package com.luzi82.clockcam;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FtpManager extends Thread {

	public static final long UPLOAD_TIME_DELAY = 30000;

	private static Object mStaticLock = new Object();

	public final String mServer;
	public final int mPort;
	public final String mUsername;
	public final String mPassword;
	public final String mRemoteDirectory;
	public final String mLocalDirectory;
	public FTPClient mFtp;
	// public LinkedList<SendFileTask> mSendFileTaskQueue = new
	// LinkedList<SendFileTask>();

	public boolean mShouldRun = true;
	public long mWatchDogTime;
	private WatchDog mWatchDog;

	public FtpManager(String aServer, int aPort, String aUsername, String aPassword, String aRemoteDirectory, String aLocalDirectory) {
		mServer = aServer;
		mPort = aPort;
		mUsername = aUsername;
		mPassword = aPassword;
		mRemoteDirectory = aRemoteDirectory;
		mLocalDirectory = aLocalDirectory;
	}

	public void run() {
		ClockCamActivity.d("ftp run");
		File localDir = new File(mLocalDirectory);
		mWatchDogTime = System.currentTimeMillis();
		mWatchDog = new WatchDog();
		mWatchDog.start();
		while (mShouldRun) {
			// connect loop
			ClockCamActivity.d("ftp connect");
			try {
				doConnect();
				while (shouldRun()) {
					// send file loop
					ClockCamActivity.d("ftp send");
					File[] fileList = null;
					while (shouldRun()) {
						// check file loop
						ClockCamActivity.d("ftp check");
						updateWatchdogTimer();
						synchronized (mStaticLock) {
							fileList = localDir.listFiles();
						}
						if ((fileList != null) && (fileList.length > 0)) {
							break;
						}
						synchronized (this) {
							try {
								wait(30000);
							} catch (InterruptedException e) {
							}
						}
					}
					if (!shouldRun()) {
						break;
					}
					if (fileList == null)
						continue;
					Arrays.sort(fileList);
					updateWatchdogTimer();
					for (File f : fileList) {
						if (!shouldRun()) {
							break;
						}
						long now = System.currentTimeMillis();
						long fTime = f.lastModified();
						ClockCamActivity.d(String.format("ftp file %s", f.getName()));
						if (now < fTime + UPLOAD_TIME_DELAY)
							continue;
						ClockCamActivity.d("ftp upload");
						synchronized (mStaticLock) {
							sendFile(f);
							f.delete();
						}
						updateWatchdogTimer();
					}
					if (!shouldRun()) {
						break;
					}
					updateWatchdogTimer();
					synchronized (this) {
						try {
							wait(5000);
						} catch (InterruptedException e) {
						}
					}
					updateWatchdogTimer();
				}
			} catch (Throwable t) {
				t.printStackTrace();
				doDisconnect();
			}
			if (!shouldRun()) {
				break;
			}
			synchronized (this) {
				try {
					wait(5000);
				} catch (Throwable t) {
				}
			}
		}
		doDisconnect();
	}

	private synchronized boolean shouldRun() {
		return mShouldRun;
	}

	public synchronized void stopLater() {
		mShouldRun = false;
	}

	private synchronized void doConnect() throws SocketException, IOException {
//		ClockCamActivity.d(String.format("server %s", mServer));
//		ClockCamActivity.d(String.format("username %s", mUsername));
//		ClockCamActivity.d(String.format("password %s", mPassword));
		mFtp = new FTPClient();
		// mFtp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
		mFtp.connect(mServer, mPort);
		mFtp.login(mUsername, mPassword);
		mFtp.setFileType(FTP.BINARY_FILE_TYPE);
		// mFtp.enterLocalActiveMode();
		mFtp.enterLocalPassiveMode();
		mWatchDogTime = System.currentTimeMillis();
	}

	private synchronized void doDisconnect() {
		if (mFtp != null) {
			try {
				mFtp.disconnect();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			mFtp = null;
		}
	}

	// public synchronized void connect() {
	// if (mShouldRun) {
	// return;
	// }
	// mShouldRun = true;
	// maintain();
	// }

	private void sendFile(File aFile) throws IOException {
		if (mFtp == null) {
			throw new IOException();
		}
		if (!aFile.exists()) {
			return;
		}
		BufferedInputStream bis = null;
		try {
			String remotePath = mRemoteDirectory + "/" + aFile.getName();

			boolean done = false;

//			mFtp.makeDirectory(mRemoteDirectory);
//			mFtp.changeWorkingDirectory(mRemoteDirectory);
			mkdirs(mFtp, mRemoteDirectory);
			ClockCamActivity.d(mRemoteDirectory);
			ClockCamActivity.d(mFtp.printWorkingDirectory());

			// done = mFtp.setFileType(FTPClient.BINARY_FILE_TYPE);
			// ClockCamActivity.d("setFileType " + done);

			bis = new BufferedInputStream(new FileInputStream(aFile));
			// mFtp.storeFile(aFile.getName(), bis);
			done = mFtp.storeFile(remotePath, bis);
			bis.close();
			mFtp.noop();
			// ClockCamActivity.d("storeFile " + done);
			if (!done) {
				throw new IOException();
			}
		} catch (IOException ioe) {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException ioe2) {
				}
			}
			throw ioe;
		}
	}

	private class WatchDog extends Thread {
		public void run() {
			while (mShouldRun) {
				ClockCamActivity.d("WatchDog turn");
				synchronized (this) {
					try {
						wait(30000);
					} catch (Throwable t) {
					}
				}
				if (!mShouldRun) {
					break;
				}
				long now = System.currentTimeMillis();
				if (now > mWatchDogTime + 120000) {
					ClockCamActivity.d("WatchDog timeout, disconnect");
					doDisconnect();
				} else {
					ClockCamActivity.d("WatchDog pass");
				}
			}
		}
	}

	private void updateWatchdogTimer() throws IOException {
		ClockCamActivity.d("updateWatchdogTimer");
		mFtp.noop();
		mWatchDogTime = System.currentTimeMillis();
	}

	private void mkdirs(FTPClient mFtpClient, String aPath) {
		String[] p = aPath.split(Pattern.quote("/"));
		String pp = "";
		for (String pi : p) {
			pp = pp + pi;
			if (!pp.endsWith("/")) {
				pp = pp + "/";
			}
			try {
				mFtpClient.makeDirectory(pp);
			} catch (IOException e) {
			}
		}
	}

}
