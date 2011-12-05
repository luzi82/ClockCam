package com.luzi82.clockcam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class ClockCamConfig {

	public String mServer;
	public String mUsername;
	public String mPassword;
	public String mRemoteDir;

	static public ClockCamConfig load(String aFilename) throws IOException {
		ClockCamConfig ret = new ClockCamConfig();
		BufferedReader br = new BufferedReader(new FileReader(aFilename));
		while (true) {
			String line = br.readLine();
			if (line == null) {
				break;
			}
			line = line.trim();
			String[] cmd = line.split(Pattern.quote(" "));
			if ((cmd[0].equals("server")) && cmd.length == 2) {
				ret.mServer = cmd[1];
			} else if ((cmd[0].equals("username")) && cmd.length == 2) {
				ret.mUsername = cmd[1];
			} else if ((cmd[0].equals("password")) && cmd.length == 2) {
				ret.mPassword = cmd[1];
			} else if ((cmd[0].equals("remotedir")) && cmd.length == 2) {
				ret.mRemoteDir = cmd[1];
			}
		}
		return ret;
	}

}
