package com.boyaa.autotest.performancedata.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import com.boyaa.autotest.performancedata.MainActivity;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 环境参数及方法工具类。
 */
public class Env {

	public static final int API = android.os.Build.VERSION.SDK_INT;

	public static final int RETRY_LIMIT = 100;

	private static Boolean isRunning = true;

	//FPS采集时间间隔
	private static int msecond_FPS = 1000;

	//其他性能指标采集时间间隔
	private static int msecond = 1000;

	//待采集指标数组
	private static String[] performanceIndexs = {"CPU", "MEM", "FPS", "NET"};

	//待采集性能指标类别
	private static int[] performanceTypes = {Functions.PERF_DIGITAL_CPU,};

	private static String packageName = "com.boyaa.sina";

	//应用是否在启动成功后就退到后台
	public static Boolean isToBack = true;

	public static String getPName() {
		return packageName;
	}

	public static void setPName(String panme) {
		packageName = panme;
	}

	public static int getMsecondFPS() {
		return msecond_FPS;
	}

	public static int getMsecond() {
		return msecond;
	}

	public static String[] getPerIndexs() {
		return performanceIndexs;
	}

	public static void setMesendFps(int ms) {
		msecond_FPS = ms;
	}

	public static void setMesend(int ms) {
		msecond = ms;
	}

	public static void setPerIndexs(String[] indexs) {
		performanceIndexs = indexs;
	}

	public static Boolean getIsRunning() {
		return isRunning;
	}

	public static void setIsRunning(Boolean run) {
		isRunning = run;
	}

	/**
	 * 获取指定包名对应的包版本号
	 *
	 * @param pName 待获得包包名  传入空字符串时，取待测应用包名
	 * @return 指定包的版本号, 默认值“”
	 */
	public static String getVersionName(String pName) {
		if(pName.equals("")) pName = Env.getPName();
		PackageManager curPManager = MainActivity.getContext().getPackageManager();
		List<PackageInfo> packages = curPManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		String versionName = "";
		try {
			String packageName = null;
			for (PackageInfo info : packages) {
				packageName = info.packageName;
				if (packageName.equals(pName)) {
					versionName = info.versionName;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return versionName;
	}

	/**
	 * 是否存在SD卡
	 */
	public static boolean isSDCardExist() {
		if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			// 对用户只提示一次，以免干扰
			return false;
		}
		return true;
	}

	public static String S_ROOT_FOLDER = SDCardPathHelper.getAbsoluteSdcardPath() + "/autotest/";

	private static boolean hasSDCardNotExistWarned = false;

	public static class SDCardPathHelper {

		public static final String CT_S_Sdcard_Sign_Storage_emulated = "storage/emulated/";
		public static final String CT_S_Sdcard_Sign_Storage_sdcard = "storage/sdcard";
		// 根据Nexus5 Android6.01适配
		public static final String CT_S_Sdcard_Sign_Storage_emulated_0 = "storage/emulated/0";
		public static final String CT_S_Sdcard_Sign_sdcard = "sdcard";

		private static String CD_S_SdcardPath = "";
		private static String CD_S_SdcardPathAbsolute = "";

		public static String getSdcardPath() {
			if (TextUtils.isEmpty(CD_S_SdcardPath))
				CD_S_SdcardPath = Environment.getExternalStorageDirectory().getPath();

			CD_S_SdcardPath = checkAndReplaceEmulatedPath(CD_S_SdcardPath);

			return CD_S_SdcardPath;
		}

		public static String getAbsoluteSdcardPath() {
			if (TextUtils.isEmpty(CD_S_SdcardPathAbsolute)) {
				CD_S_SdcardPathAbsolute = Environment.getExternalStorageDirectory().getAbsolutePath();
			}
			// 先试试默认的目录，如果创建目录失败再试其他方案
			String testFileName = FormatUtils.getSaveDateMs();
			File testF = new File(CD_S_SdcardPathAbsolute + "/GT/" + testFileName + "/");
			if (testF.mkdirs()) {
				FileUtil.deleteFile(testF);
				return CD_S_SdcardPathAbsolute;
			}

			// 默认路径不可用，尝试其他方案
			CD_S_SdcardPathAbsolute = checkAndReplaceEmulatedPath(CD_S_SdcardPathAbsolute);

			return CD_S_SdcardPathAbsolute;
		}

		public static File getSdcardPathFile() {
			return new File(getSdcardPath());
		}

		public static String checkAndReplaceEmulatedPath(String strSrc) {
			String result = strSrc;
			Pattern p = Pattern.compile("/?storage/emulated/\\d{1,2}");
			Matcher m = p.matcher(strSrc);
			if (m.find()) {
				result = strSrc.replace(CT_S_Sdcard_Sign_Storage_emulated, CT_S_Sdcard_Sign_Storage_sdcard);
				// 如果目录建立失败，最后尝试Nexus5 Android6.01适配
				String testFileName = FormatUtils.getSaveDateMs();
				File testFile = new File(CD_S_SdcardPathAbsolute + "/GT/" + testFileName + "/");
				if (testFile.mkdirs()) {
					FileUtil.deleteFile(testFile);
				} else {
					result = strSrc.replace(CT_S_Sdcard_Sign_Storage_emulated_0, CT_S_Sdcard_Sign_sdcard);

					// test
					File testF = new File(result + "/GT/" + testFileName + "/");
					if (testF.mkdirs()) {
						FileUtil.deleteFile(testF);
					}
				}

			}

			return result;
		}
	}

}
