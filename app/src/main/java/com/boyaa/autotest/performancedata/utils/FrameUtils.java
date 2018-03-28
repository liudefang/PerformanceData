/*
 * Tencent is pleased to support the open source community by making
 * Tencent GT (Version 2.4 and subsequent versions) available.
 *
 * Notwithstanding anything to the contrary herein, any previous version
 * of Tencent GT shall not be subject to the license hereunder.
 * All right, title, and interest, including all intellectual property rights,
 * in and to the previous version of Tencent GT (including any and all copies thereof)
 * shall be owned and retained by Tencent and subject to the license under the
 * Tencent GT End User License Agreement (http://gt.qq.com/wp-content/EULA_EN.html).
 * 
 * Copyright (C) 2015 THL A29 Limited, a Tencent company. All rights reserved.
 * 
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://opensource.org/licenses/MIT
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.boyaa.autotest.performancedata.utils;

import android.util.Log;

import com.boyaa.autotest.performancedata.MainActivity;
import com.boyaa.autotest.performancedata.api.ProcessUtils;
import com.boyaa.autotest.performancedata.log.LogUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrameUtils {

	private static int pid = 0;
	public static boolean hasSu = false;

	public static boolean isHasSu() {
		return hasSu;
	}

	public static void setHasSu(boolean hasSu) {
		FrameUtils.hasSu = hasSu;
	}

	public static void setPid() {
		/*Boolean canRoot = canRunRootCommands();
		if(canRoot){
			LogUtils.log("Analysis | 可以执行root命令");
		}
		else{
			LogUtils.log("Analysis | 不可以执行root命令！！！！");
		}

		Boolean root = upgradeRootPermission(MainActivity.getContext().getPackageCodePath());
		if(root){
			LogUtils.log("Analysis | 获取root权限成功");
		}
		else{
			LogUtils.log("Analysis | 获取root权限失败");
		}*/


		setHasSu(false);
		try {
			ProcessBuilder execBuilder = null;
			if (pid == 0) {
				execBuilder = new ProcessBuilder("su", "-c", "ps");

				execBuilder.redirectErrorStream(true);

				Process exec = null;
				exec = execBuilder.start();
				InputStream is = exec.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));

				String line = "";
				while ((line = reader.readLine()) != null) {
					if (line.contains("surfaceflinger")) {
						LogUtils.log("PS命令获取值：" + line);
						String regEx = "\\s[0-9][0-9]*\\s";
						Pattern pat = Pattern.compile(regEx);
						Matcher mat = pat.matcher(line);
						if (mat.find()) {
							String temp = mat.group();
							temp = temp.replaceAll("\\s", "");
							pid = Integer.parseInt(temp);
						}
						break;
					}
				}
			}

			if (pid == 0) {
				if (ProcessUtils.getProcessPID("system_server") != -1) {
					LogUtils.log("从system_server获取pid");
					pid = ProcessUtils.getProcessPID("system_server");
				} else {
					LogUtils.log("从system_server获取pid");
					pid = ProcessUtils.getProcessPID("system");
				}

			}
			setHasSu(true);
		} catch (Exception e) {
			e.printStackTrace();
			setHasSu(false);
		}

		Log.d("Analysis | pid: ", String.valueOf(pid));
	}

	/**
	 * upgrade app to get root permission
	 *
	 * @return is root successfully
	 */
	public static boolean upgradeRootPermission(String pkgCodePath) {
		LogUtils.log("pkgCodePath:" + pkgCodePath);
		Process process = null;
		DataOutputStream os = null;
		try {
			String cmd = "chmod 777 " + pkgCodePath;
			process = Runtime.getRuntime().exec("su -c "); // 切换到root帐号
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
			int existValue = process.waitFor();
			if (existValue == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			Log.w("", "upgradeRootPermission exception=" + e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
			}
		}
	}

	public static boolean canRunRootCommands() {
		boolean retval = false;
		Process suProcess;

		try {
			suProcess = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
			DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

			if (null != os && null != osRes) {
				// Getting the id of the current user to check if this is root
				os.writeBytes("id\n");
				os.flush();

				String currUid = osRes.readLine();
				boolean exitSu = false;
				if (null == currUid) {
					retval = false;
					exitSu = false;
					Log.d("ROOT", "Can't get root access or denied by user");
				} else if (true == currUid.contains("uid=0")) {
					retval = true;
					exitSu = true;
					Log.d("ROOT", "Root access granted");
				} else {
					retval = false;
					exitSu = true;
					Log.d("ROOT", "Root access rejected: " + currUid);
				}

				if (exitSu) {
					os.writeBytes("exit\n");
					os.flush();
				}
			}
		} catch (Exception e) {
			// Can't get root !
			// Probably broken pipe exception on trying to write to output
			// stream after su failed, meaning that the device is not rooted

			retval = false;
			Log.d("Analysis | ROOT",
					"Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
		}

		return retval;
	}
}
