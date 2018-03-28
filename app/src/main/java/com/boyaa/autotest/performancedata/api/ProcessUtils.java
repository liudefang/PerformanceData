package com.boyaa.autotest.performancedata.api;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.util.SparseArray;

import com.boyaa.autotest.performancedata.MainActivity;

/**
 * 进程信息工具类。
 */
public class ProcessUtils {

    private static IProcess processUtil;

    /**
     * 根据手机系统的版本号，适配ProcessUtils的实现，全局调用一次即可。 Android4.x及以下系统，用老的方式效率更高。
     * Android5.x及以上系统，用shell命令方式。
     */
    synchronized public static void init() {
        /*if (Env.API < 21) {
            processUtil = new Process4x();
        } else if (Env.API < 23) // 5.x+
        {
            processUtil = new Process5x();
        } else // 6.x+
        {
            processUtil = new Process6x();
        }*/
        processUtil = new Process4x();
    }

    interface IProcess {
        List<ProcessInfo> getAllRunningAppProcessInfo();

        String getPackageByUid(int uid);

        int getProcessPID(String pName);

        int getProcessUID(String pName);

        boolean hasProcessRunPkg(String pkgName);

        boolean isProcessAlive(String sPid);

        boolean initUidPkgCache();
    }

    /**
     * 内置类 进程信息类
     */
    public static class ProcessInfo {
        public String name; // 进程名
        public int pid; // PID
        public int ppid; // 父PID
        public int uid; // UID

        public ProcessInfo(int pid, String name, int ppid, int uid) {
            this.pid = pid;
            this.name = name;
            this.ppid = ppid;
            this.uid = uid;
        }

        @Override
        public int hashCode() {
            int result = 17;
            if (name != null)
                result = 37 * result + name.hashCode();
            result = 37 * result + (pid ^ (pid >>> 32));
            result = 37 * result + (ppid ^ (ppid >>> 32));
            result = 37 * result + (uid ^ (uid >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof ProcessInfo) {
                ProcessInfo another = (ProcessInfo) o;
                if (this.pid == another.pid && this.ppid == another.ppid && this.name != null && another.name != null
                        && this.name.equals(another.name)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 根据进程名，获取进程PID
     * @param pName
     *            进程名
     * @return 进程PID
     */
    public static int getProcessPID(String pName) {

        return processUtil.getProcessPID(pName);
    }

    /**
     * 根据进程名，获取进程UID，反查UID，性能需要高
     * @param pName
     *            进程名
     * @return 进程UID
     */
    public static int getProcessUID(String pName) {

        return processUtil.getProcessUID(pName);
    }

    /**
     * 判断进程是否在运行。
     *
     * @param sPid
     *            进程号
     * @return true 正在运行；false 停止运行
     */
    public static boolean isProcessAlive(String sPid) {

        return processUtil.isProcessAlive(sPid);
    }

    static class Process4x implements IProcess {
        // uid和package的对应
        private SparseArray<String> uidPkgCache = null;

        private SparseArray<String> getUidPkgCache() {
            return uidPkgCache;
        }

        @Override
        public List<ProcessInfo> getAllRunningAppProcessInfo() {
            ActivityManager am = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
            List<ProcessInfo> ret = new ArrayList<ProcessInfo>();
            for (ActivityManager.RunningAppProcessInfo info : appProcessList) {
                // pid目前不需要，默认赋值为-1
                ProcessInfo processInfo = new ProcessInfo(info.pid, info.processName, -1, info.uid);
                ret.add(processInfo);
            }

            return ret;
        }

        @Override
        public String getPackageByUid(int uid) {
            // Android4.x不需要此方法
            throw new UnsupportedOperationException();
        }

        @Override
        public int getProcessPID(String pName) {
            int pId = -1;
            ActivityManager am = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
            int pLength = appProcessList.size();
            for (int i = 0; i < pLength; i++) {
                if (appProcessList.get(i).processName.equals(pName)) {
                    pId = appProcessList.get(i).pid;
                    break;
                }
            }
            return pId;
        }

        @Override
        public int getProcessUID(String pName) {
            int uId = 0;
            ActivityManager am = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
            int pLength = appProcessList.size();
            for (int i = 0; i < pLength; i++) {
                if (appProcessList.get(i).processName.equals(pName)) {
                    uId = appProcessList.get(i).uid;
                    break;
                }
            }
            return uId;
        }

        @Override
        public boolean hasProcessRunPkg(String pkgName) {
            if (pkgName == null)
                return false;
            int uid = -1;
            int len = getUidPkgCache().size();
            for (int i = 0; i < len; i++) {
                if (pkgName.equals(getUidPkgCache().valueAt(i))) {
                    uid = getUidPkgCache().keyAt(i);
                    break;
                }
            }

            List<ProcessInfo> appProcessInfos = getAllRunningAppProcessInfo();
            for (ProcessInfo info : appProcessInfos) {
                if (info.uid == uid) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isProcessAlive(String sPid) {
            boolean isAlive = false;
            if (sPid != null && MainActivity.getContext() != null) {
                int pid = -1;
                try {
                    pid = Integer.parseInt(sPid);
                } catch (Exception e) {
                    return false;
                }

                ActivityManager am = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
                for (RunningAppProcessInfo info : appProcessList) {
                    if (info.pid == pid) {
                        isAlive = true;
                        break;
                    }
                }
            }

            return isAlive;
        }

        @Override
        public boolean initUidPkgCache() {
            // do nothing for 4.x
            uidPkgCache = new SparseArray<String>();
            ActivityManager am = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
            for (RunningAppProcessInfo info : appProcessList) {
                String[] pkgList = info.pkgList;
                for (String pkg : pkgList) {
                    uidPkgCache.put(info.uid, pkg);
                }
            }
            return true;
        }
    }


}

