package com.luzi82.clockcam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class CameraInfo implements Serializable {

	private static final long serialVersionUID = 2786751092237418751L;

	public static final String DEFAULT_FILENAME = "camerainfo.dat";

	public int mDataVersion;

	public float mExposureCompensationStep;
	public int mMaxExposureCompensation;
	public int mMinExposureCompensation;

	public int mMaxZoom;

	public String[] mSupportedAntibanding;
	public String[] mSupportedColorEffects;
	public String[] mSupportedFlashModes;
	public String[] mSupportedFocusModes;
	public Size[] mSupportedJpegThumbnailSizes;
	public Integer[] mSupportedPictureFormats;
	public Size[] mSupportedPictureSizes;
	public Integer[] mSupportedPreviewFormats;
	public Integer[] mSupportedPreviewFrameRates;
	public Size[] mSupportedPreviewSizes;
	public String[] mSupportedSceneModes;
	public String[] mSupportedWhiteBalance;
	public Integer[] mZoomRatios;
	public boolean mSmoothZoomSupported;
	public boolean mZoomSupported;

	public boolean mValid=false;

	public static CameraInfo readDefault(Context aContext) {
		int version = getVersion(aContext);
		File file = new File(aContext.getFilesDir(), DEFAULT_FILENAME);
		return readFile(file, version);
	}
	
	public void writeDefault(Context aContext) throws IOException {
		File file = new File(aContext.getFilesDir(), DEFAULT_FILENAME);
		writeFile(file);
	}

	// public static CameraInfo readFile(String aFilename, int aVersion) {
	// return readFile(new File(aFilename), aVersion);
	// }

	public static CameraInfo readFile(File aFile, int aVersion) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(aFile)));
			CameraInfo ret = (CameraInfo) ois.readObject();
			ois.close();
			return (ret.mDataVersion == aVersion) ? ret : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void writeFile(File aFile) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(aFile)));
		oos.writeObject(this);
		oos.flush();
		oos.close();
	}

	public static CameraInfo readCamera(Camera aCamera, int aVersion) {
		return readParameters(aCamera.getParameters(), aVersion);
	}

	public static CameraInfo readParameters(Parameters parameters, int aVersion) {
		ClockCamActivity.d(parameters.flatten());

		CameraInfo ret = new CameraInfo();

		ret.mDataVersion = aVersion;

		try{ret.mExposureCompensationStep = parameters.getExposureCompensationStep();}catch(NullPointerException e){}
		try{ret.mMaxExposureCompensation = parameters.getMaxExposureCompensation();}catch(NullPointerException e){}
		try{ret.mMinExposureCompensation = parameters.getMinExposureCompensation();}catch(NullPointerException e){}

		try{ret.mMaxZoom = parameters.getMaxZoom();}catch(NullPointerException e){}

		try{ret.mSupportedAntibanding = toAry(parameters.getSupportedAntibanding());}catch(NullPointerException e){}
		try{ret.mSupportedColorEffects = toAry(parameters.getSupportedColorEffects());}catch(NullPointerException e){}
		try{ret.mSupportedFlashModes = toAry(parameters.getSupportedFlashModes());}catch(NullPointerException e){}
		try{ret.mSupportedFocusModes = toAry(parameters.getSupportedFocusModes());}catch(NullPointerException e){}
		try{ret.mSupportedJpegThumbnailSizes = toAry(parameters.getSupportedJpegThumbnailSizes());}catch(NullPointerException e){}
		try{ret.mSupportedPictureFormats = toAry(parameters.getSupportedPictureFormats());}catch(NullPointerException e){}
		try{ret.mSupportedPictureSizes = toAry(parameters.getSupportedPictureSizes());}catch(NullPointerException e){}
		try{ret.mSupportedPreviewFormats = toAry(parameters.getSupportedPreviewFormats());}catch(NullPointerException e){}
		try{ret.mSupportedPreviewFrameRates = toAry(parameters.getSupportedPreviewFrameRates());}catch(NullPointerException e){}
		try{ret.mSupportedPreviewSizes = toAry(parameters.getSupportedPreviewSizes());}catch(NullPointerException e){}
		try{ret.mSupportedSceneModes = toAry(parameters.getSupportedSceneModes());}catch(NullPointerException e){}
		try{ret.mSupportedWhiteBalance = toAry(parameters.getSupportedWhiteBalance());}catch(NullPointerException e){}
		try{ret.mZoomRatios = toAry(parameters.getZoomRatios());}catch(NullPointerException e){}
		try{ret.mSmoothZoomSupported = parameters.isSmoothZoomSupported();}catch(NullPointerException e){}
		try{ret.mZoomSupported = parameters.isZoomSupported();}catch(NullPointerException e){}

		ret.mValid = true;

		return ret;
	}

	public static class Size implements Serializable {
		private static final long serialVersionUID = -5018355999707062771L;
		public int mWidth;
		public int mHeight;

		public Size(int aWidth, int aHeight) {
			mWidth = aWidth;
			mHeight = aHeight;
		}

		public Size(Camera.Size aSize) {
			this(aSize.width, aSize.height);
		}
	}

	final static Size[] SIZE0 = new Size[0];

	static private Size[] toAry(List<Camera.Size> aCameraSizeList) {
		if (aCameraSizeList == null)
			return null;
		LinkedList<Size> sizeList = new LinkedList<Size>();
		for (Camera.Size cs : aCameraSizeList) {
			sizeList.add(new Size(cs));
		}
		return sizeList.toArray(SIZE0);
	}

	final static String[] STRING0 = new String[0];

	static private String[] toAry(List<String> aStringList) {
		return (aStringList == null) ? null : aStringList.toArray(STRING0);
	}

	final static Integer[] INT0 = new Integer[0];

	static private Integer[] toAry(List<Integer> aIntegerList) {
		return (aIntegerList == null) ? null : aIntegerList.toArray(INT0);
	}

	static public int getVersion(Context aContext) {
		try {
			PackageManager pm = aContext.getPackageManager();
			String pn = aContext.getPackageName();
			PackageInfo pi = pm.getPackageInfo(pn, 0);
			return pi.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
