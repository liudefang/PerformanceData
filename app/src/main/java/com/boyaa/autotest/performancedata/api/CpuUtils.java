package com.boyaa.autotest.performancedata.api;

import com.boyaa.autotest.performancedata.utils.DoubleUtils;
import com.boyaa.autotest.performancedata.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JessicZeng on 2017/2/20.
 */
public class CpuUtils {

    public static Map<String, CpuUtils> cpuInfoMap = new HashMap<String, CpuUtils>();
    private static boolean initCpu = true;
    private static double o_cpu = 0.0;
    private static double o_idle = 0.0;

    private double p_jif = 0.0;
    private double pCpu = 0.0;
    private double aCpu = 0.0;
    private double o_pCpu = 0.0;
    private double o_aCpu = 0.0;

    public String getProcessCpuUsage(int pid) {

        String result = "";
        String[] result1 = null;
        String[] result2 = null;
        if (pid >= 0) {

            result1 = getProcessCpuAction(pid);
            if (null != result1) {
                pCpu = Double.parseDouble(result1[1]) + Double.parseDouble(result1[2]);
            }
            result2 = getCpuAction();
            if (null != result2) {
                aCpu = 0.0;
                for (int i = 2; i < result2.length; i++) {

                    aCpu += Double.parseDouble(result2[i]);
                }
            }
            double usage = 0.0;
            if ((aCpu - o_aCpu) != 0) {
                usage = DoubleUtils.div(((pCpu - o_pCpu) * 100.00), (aCpu - o_aCpu), 2);
                if (usage < 0) {
                    usage = 0;
                } else if (usage > 100) {
                    usage = 100;
                }

            }
            o_pCpu = pCpu;
            o_aCpu = aCpu;
            result = String.valueOf(usage) + "%";
        }
        p_jif = pCpu;
        return result;
    }

    public String[] getProcessCpuAction(int pid) {
        String cpuPath = "/proc/" + pid + "/stat";
        String cpu = "";
        String[] result = new String[3];

        File f = new File(cpuPath);
        if (!f.exists() || !f.canRead()) {
			/*
			 * 进程信息可能无法读取， 同时发现此类进程的PSS信息也是无法获取的，用PS命令会发现此类进程的PPid是1，
			 * 即/init，而其他进程的PPid是zygote, 说明此类进程是直接new出来的，不是Android系统维护的
			 */
            return result;
        }

        FileReader fr = null;
        BufferedReader localBufferedReader = null;

        try {
            fr = new FileReader(f);
            localBufferedReader = new BufferedReader(fr, 8192);
            cpu = localBufferedReader.readLine();
            if (null != cpu) {
                String[] cpuSplit = cpu.split(" ");
                result[0] = cpuSplit[1];
                result[1] = cpuSplit[13];
                result[2] = cpuSplit[14];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileUtil.closeReader(localBufferedReader);
        return result;
    }

    public String[] getCpuAction() {
        String cpuPath = "/proc/stat";
        String cpu = "";
        String[] result = new String[7];

        File f = new File(cpuPath);
        if (!f.exists() || !f.canRead()) {
            return result;
        }

        FileReader fr = null;
        BufferedReader localBufferedReader = null;

        try {
            fr = new FileReader(f);
            localBufferedReader = new BufferedReader(fr, 8192);
            cpu = localBufferedReader.readLine();
            if (null != cpu) {
                result = cpu.split(" ");

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileUtil.closeReader(localBufferedReader);
        return result;
    }
}
