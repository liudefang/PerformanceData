package com.boyaa.autotest.performancedata.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Message;

import com.boyaa.autotest.performancedata.log.GWSaveEntry;
import com.boyaa.autotest.performancedata.utils.FormatUtils;
import com.boyaa.autotest.performancedata.utils.Functions;
import com.boyaa.autotest.performancedata.MainActivity;
import com.boyaa.autotest.performancedata.api.CpuUtils;
import com.boyaa.autotest.performancedata.api.FpsTimerTask;
import com.boyaa.autotest.performancedata.api.MemUtils;
import com.boyaa.autotest.performancedata.api.NetUtils;
import com.boyaa.autotest.performancedata.api.ProcessUtils;
import com.boyaa.autotest.performancedata.model.PerfPara;
import com.boyaa.autotest.performancedata.model.TagTimeEntry;
import com.boyaa.autotest.performancedata.utils.Env;
import com.boyaa.autotest.performancedata.utils.FrameUtils;
import com.boyaa.autotest.performancedata.log.LogUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;

/**
 *  性能数据统计管理类
 * Created by JessicZeng on 2017/2/23.
 */
public class PerfManager {
    private Map<String, TagTimeEntry> dataMap;
    private static Map<String,PerfPara> paraLists;
    private CpuUtils mCpuUtils;
    private NetUtils mNetUtils;
    private Thread thread;
    private String[] perfIndexs;

    //写死为com.boyaa.sina的pid  默认值为-1，表示进程不存在
    private int pid;

    //pid为-1时，重试次数
    private int retryCount;

    //FPS相关
    Timer fpsTimer40;
    private boolean fps_gather = false;
    private boolean hasCheckSu = false;

    public static PerfManager INSTANCE;

    public PerfManager() {
        LogUtils.log("PerfManager ctor");
        dataMap = Collections.synchronizedMap(new LinkedHashMap<String, TagTimeEntry>());
        initParas();
    }

    public static PerfManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new PerfManager();
        }
        return INSTANCE;
    }

    public static Map<String,PerfPara> getParaLists(){
        return paraLists;
    }

    /**
     * 开始采集性能数据
     */
    public  void startCollect() {
        retryCount = 0;
        thread = new Thread(new EngineRunnable());
        thread.start();
        LogUtils.log("startCollect方法运行完成");
    }
    /**
     * 初始化相关参数
     */
    private void registerMonitor(PerfPara pdata) {
        if(pdata == null) return;
        LogUtils.log("PerfManager registerMonitor:" + pdata.getName());
        switch(pdata.getName()){
            case "CPU":
                startProfier(pdata, Functions.PERF_DIGITAL_CPU, "Process CPU occupy", "%");
                break;
            case "MEM":
                String[] subKeys_pss = { "total", "dalvik", "native" };
                int[] funIds_pss = { Functions.PERF_DIGITAL_MULT_MEM, Functions.PERF_DIGITAL_MULT_MEM,
                        Functions.PERF_DIGITAL_MULT_MEM };
                startProfier(pdata, subKeys_pss, funIds_pss, "", "MB");
                break;
            case "FPS":
                startProfier(pdata, Functions.PERF_DIGITAL_NORMAL, "FPS", "");
                break;
            case "NET":
                String[] subKeys = { "transmitted", "received" };
                int[] funIds = { Functions.PERF_DIGITAL_MULT, Functions.PERF_DIGITAL_MULT };
                startProfier(pdata, subKeys, funIds, "", "KB");
                break;
        }
    }

    private void initParas() {
        LogUtils.log("PerfManager initParas");
        ProcessUtils.init();
        pid = ProcessUtils.getProcessPID(Env.getPName());
        mCpuUtils = new CpuUtils();
        mNetUtils = new NetUtils(Env.getPName());
        mNetUtils.initProcessNetValue(Env.getPName());
        paraLists = new LinkedHashMap<String, PerfPara>();
        perfIndexs = Env.getPerIndexs();
        PerfPara para  = null;
        for(int i = 0;i<perfIndexs.length;i++)
        {
            para = new PerfPara(perfIndexs[i]);
            paraLists.put(Env.getPerIndexs()[i],para);
            registerMonitor(para);
        }
    }

    public class EngineRunnable implements Runnable {

        @Override
        public void run() {
            //不执行其他指标采集
            Boolean going = true;
            int perfArrLen = perfIndexs.length;
            if(perfArrLen <= 0){
                LogUtils.log("没有待采集指标，直接结束");
            }
            while (going) {
                for(int i = 0;i < perfArrLen;i++)
                {
                    going = getPerformanceDara(paraLists.get(perfIndexs[i]));
                    if(!going){
                        LogUtils.log("不符合采集数据条件，停止采集");
                        stopFPSCollect();
                        break;
                    }
                }

                try {
                    Thread.sleep(Env.getMsecond());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            LogUtils.log("EngineRunnable进程 run方法执行完成");
           try {
                stopApp();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    //采集数据完成，报错保存数据到文件并且stop进程
    private  void stopApp() throws InterruptedException {
        saveToSD();
        Thread.sleep(120 * 1000);
        LogUtils.log("开始执行关闭应用操作");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 停止FPS采集Timer
     */
    private void stopFPSCollect() {
        LogUtils.log("停止FPS相关数据采集");
        //若还未开始采集数据前就停止，此时fpsTimer40还未初始化
        if(fpsTimer40 != null){
            fpsTimer40.cancel();
            FpsTimerTask.stopCurrentTask();
        }

    }

    /**
     *获取每个指标数据
     * @param para 性能指标
     */
    private Boolean getPerformanceDara(PerfPara para){
        //重试多少次后，停止性能采集应用  数组循环内每次都会执行到
        if(pid == -1) {
            if (retryCount > Env.RETRY_LIMIT) {
                //达到重试上限，停止性能数据采集  等待下一次重新启动采集
                LogUtils.log("已达到重试上限，等待下一次重启启动采集");
                return false;
            } else {
                retryCount++;
                pid = ProcessUtils.getProcessPID(Env.getPName());
                if (pid == -1) {
                    LogUtils.log("第" + retryCount + "次获取pid失败");
                    return true;
                }
            }
        }
        if(!ProcessUtils.isProcessAlive(String.valueOf(pid))){
            LogUtils.log(" the pakeage is not running：" + pid);
            return false;
        }
        String value = "";
        switch (para.getName()){
            case "MEM":
                long[] long_value = MemUtils.getPSS(MainActivity.getContext(), pid);
                long tmp = long_value[0];
                long_value[0] = long_value[2];
                long_value[2] = tmp;
                for (int p = 0; p < long_value.length; p++) {
                    if (p == 2) {
                        value += " | Native:" + String.valueOf(long_value[p] + "KB");
                    } else if (p == 1) {
                        value += " | Dalvik:" + String.valueOf(long_value[p] + "KB");

                    } else if (p == 0) {
                        value += "Total:" + String.valueOf(long_value[p] + "KB");
                    }

                }
                addHistory(para, value, long_value);
                LogUtils.log("MEM:" + value);
                break;
            case "CPU":
                value = mCpuUtils.getProcessCpuUsage(pid);
                long tempValue = Double.valueOf((Double.valueOf(value.substring(0, value.length() - 1)) * 100))
                        .longValue();
                addHistory(para, value, tempValue);
                LogUtils.log("CPU:" + value + " 存储值：" + tempValue);
                break;
            case "NET":
                // 实际使用时候发现，收发的数据分成两条曲线最合理
                double lastT = mNetUtils.getP_t_add();
                double lastR = mNetUtils.getP_r_add();

                value = mNetUtils.getProcessNetValue(Env.getPName());

                double nowT = mNetUtils.getP_t_add();
                double nowR = mNetUtils.getP_r_add();

                // modify on 20120616 过滤有的手机进程流量偶尔输出负数的情况
                if ((nowT != lastT || nowR != lastR) && nowT >= 0 && nowR >= 0) {
                    addHistory(para, value, new long[] { (long) nowT, (long) nowR });
                }
                LogUtils.log("NET:" + value);
                break;
            case "FPS":
                if (!fps_gather) {
                    runFps();
                }
                break;
        }
        return true;
    }

    private synchronized void runFps() {
        if (Env.API >= 14) {
            fps_gather = true;

            if (!hasCheckSu) {
                thread = new Thread(new CheckSuRunnable(), "CheckSu");
                thread.setDaemon(true);
                thread.start();
            }

            // 因为这个Timer是延时执行，所以基本能赶上su判断线程出结果
            fpsTimer40 = new Timer();
            fpsTimer40.schedule(new FpsTimerTask(), 0, Env.getMsecondFPS());
        }
    }

    class CheckSuRunnable implements Runnable { // 无su，线程挂住
        @Override
        public void run() {
            hasCheckSu = true;
            FrameUtils.setPid();
        }

    }

    public void startProfier(PerfPara outPara, int funcId, String desc, String unit) {
        if (outPara == null) {
            return;
        }

        outPara.hasMonitorOnce = true;

        TagTimeEntry profilerEntry = new TagTimeEntry(null);
        profilerEntry.setName(outPara.getName());
        profilerEntry.setFunctionId(funcId);
        profilerEntry.setDesc(desc);
        profilerEntry.setUnit(unit);
        dataMap.put(profilerEntry.getName(), profilerEntry);
    }

    /**
     * 多维的启动性能统计，在UI上也需要展示多条曲线
     *
     * @param subKeys 子项key
     * @param funcIds IDs
     */
    public void startProfier(PerfPara outPara, String[] subKeys, int[] funcIds, String desc, String unit) {
        if (outPara == null) {
            return;
        }

        outPara.hasMonitorOnce = true;

        TagTimeEntry profilerEntry = new TagTimeEntry(null);
        profilerEntry.setName(outPara.getName());
        profilerEntry.setFunctionId(funcIds[0]);
        profilerEntry.setDesc(desc);
        profilerEntry.setUnit(unit);

        profilerEntry.initChildren(subKeys.length);
        int i = 0;
        for (TagTimeEntry subEntry : profilerEntry.getSubTagEntrys()) {
            subEntry.setName(subKeys[i]);
            subEntry.setFunctionId(funcIds[i]);
            i++;
        }
        dataMap.put(profilerEntry.getName(),profilerEntry);
    }

    public  void addHistory(PerfPara op, String nowValue, long data) {
        // 要在总控开关打开的前提下才要进行历史记录
        if (!Env.getIsRunning()) {
            return;
        }

        TagTimeEntry profilerEntry = dataMap.get(op.getName());
        if (null == profilerEntry) {
            return;
        }

        if (op.getMonitor()) // 有历史出参的告警
        {
            profilerEntry.add(data);

            // 阈值对象对本次输入值进行是否预备告警的记录
            profilerEntry.setLastValue(nowValue);
        } else {
            // 不记录历史数据的情况下统计告警，暂不使用
            // profilerEntry.getThresholdEntry().add(data);

        }
    }


    /**
     * 多维的历史数据，在UI上也需要展示多条曲线
     * 若使用static dataMap报错
     * @param parentOp
     *            对应出参的key
     * @param nowValue
     *            原始出参值
     * @param data
     *            对应多维数据的值数组
     */
    public  void addHistory(PerfPara parentOp, String nowValue, long[] data) {
        // 要在总控开关打开的前提下才要进行历史记录
        if (!Env.getIsRunning()) {
            return;
        }

        TagTimeEntry profilerEntry = dataMap.get(parentOp.getName());
        if (null == profilerEntry) {
            return;
        }

        if (parentOp.getMonitor()) // 有历史出参的告警
        {
            profilerEntry.setLastValue(nowValue);

            int i = 0;
            for (TagTimeEntry subEntry : profilerEntry.getChildren()) {

                subEntry.add(data[i]);
                i++;
            }
        } else
        {
            // 没有记录历史出参的告警，不做处理
            LogUtils.log("isMonitor开关未打开，不记录数据");
        }

    }

    /**
     * 将数据保存到SD卡中
     */
    public void saveToSD(){
        LogUtils.log("writeToFile | Performanager saveToSD");
        Thread savedata = new Thread(saveDataHandler);
        savedata.start();
    }

    Runnable saveDataHandler = new Runnable() {
        @Override
        public void run() {
            LogUtils.log("writeToFile | saveDataHandler run");
            GWSaveEntry saveEntry = new GWSaveEntry("", "", "", "");
            saveAllEnableGWData(saveEntry);
        }
    };

    public void saveAllEnableGWData(GWSaveEntry saveEntry) {
        LogUtils.log("开始saveAllEnableGWData");
        String now = FormatUtils.getSaveDate();
        saveEntry.setNow(now);
        LogUtils.log("dataMap.size:"+dataMap.size());
        TagTimeEntry[] ttes = new TagTimeEntry[dataMap.size()];
        dataMap.values().toArray(ttes);
        for (TagTimeEntry tte : ttes) {
            if (null != tte && tte.getAlias().equals("SM")) {
                //SM暂时不考虑
                //LogUtils.writeGWDataForSM(saveEntry, tte);
                LogUtils.log("tte包含SM，不进行任何操作");
            } else {
                LogUtils.log("符合要求，执行writeGWData");
                LogUtils.writeGWData(saveEntry, tte);
            }
        }
        LogUtils.log("写入文件完成");
        LogUtils.writeGWDesc(saveEntry, ttes);
        //写入设备及被测应用基本信息
        LogUtils.writeDevicesInfo(saveEntry);
    }
}
