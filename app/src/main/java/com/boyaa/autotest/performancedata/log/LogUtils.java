package com.boyaa.autotest.performancedata.log;

import com.boyaa.autotest.performancedata.model.TagTimeEntry;
import com.boyaa.autotest.performancedata.model.TimeEntry;
import com.boyaa.autotest.performancedata.utils.Env;
import com.boyaa.autotest.performancedata.utils.FileUtil;
import com.boyaa.autotest.performancedata.utils.FormatUtils;
import com.boyaa.autotest.performancedata.utils.Functions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by JessicZeng on 2017/2/23.
 */
public class LogUtils {

    public static final long CAPACITY = 4 * 1024 * 1024;
    public static int CACHE = 1000;
    public static final String LOG_POSFIX = ".log";
    public static final String LOG_FILE_MATCHE = "\\d+.log";
    public static final String TLOG_POSFIX = ".csv";
    public static final String GW_POSFIX = ".csv";
    public static final String GW_DESC_PREFIX = "gtdesc_";
    public static final String GW_DESC_POSFIX = ".txt";
    public static final String GW_DEVICEINFO_PREFIX = "deviceinfo_";
    public static final String MAIN_TAG = "Analysis |";

    public static void printLog(String msg){
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        String logTime = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date);
        System.out.println(logTime + ":" + msg);
    }

    public static void log(String msg){
        System.out.println(LogUtils.MAIN_TAG + msg);
    }

    public static void writeDevicesInfo(final GWSaveEntry saveEntry){
        if (!Env.isSDCardExist()) {
            return;
        }
        String sFolder = Env.S_ROOT_FOLDER + saveEntry.path1 + FileUtil.separator + saveEntry.path2
                + FileUtil.separator + saveEntry.path3 + FileUtil.separator;
        File folder = new File(sFolder);
        folder.mkdirs();

        String fName = GW_DEVICEINFO_PREFIX + saveEntry.now + GW_POSFIX;
        File f = new File(folder, fName);
        if (f.exists()) {
            return;
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(f, true);
            StringBuffer sb = new StringBuffer();
            sb.append("versionName,");
            sb.append(Env.getVersionName(Env.getPName()));
            sb.append("\r\n");
            writeNotClose(sb.toString(), f, fw);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.closeWriter(fw);
        }
    }

    public static void writeGWData(final GWSaveEntry saveEntry, TagTimeEntry tte) {

        if (!Env.isSDCardExist() || tte == null) {
            LogUtils.log("Env.isSDCardExist() || tte == null");
            return;
        }

        if (!tte.hasChild() && tte.getRecordSize() == 0) {
            LogUtils.log("tte.hasChild() && tte.getRecordSize() == 0");
            return;
        } else if (tte.hasChild() && tte.getSubTagEntrys()[0].getRecordSize() == 0) {
            LogUtils.log("被记录数据为空");
            return;
        }
        String sFolder = Env.S_ROOT_FOLDER + saveEntry.path1 + FileUtil.separator + saveEntry.path2
                + FileUtil.separator + saveEntry.path3 + FileUtil.separator;
        File folder = new File(sFolder);
        folder.mkdirs();

        String fName = getTagTimeEntryFileName(tte, saveEntry);
        File f = new File(folder, fName);
        if (f.exists()) {
            LogUtils.log("文件已经存在存在："+sFolder+ "" +fName);
            return;
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(f, true);

            StringBuffer sb = new StringBuffer();
            sb.append("key,");
            sb.append(tte.getName());
            sb.append("\r\n");
            sb.append("alias,");
            sb.append(tte.getAlias());
            sb.append("\r\n");
            sb.append("unit,");

            // PSS和PD的单位特殊，保存的KB，曲线图上显示的MB
            if (tte.getFunctionId() == Functions.PERF_DIGITAL_MULT_MEM) {
                sb.append("(KB)");
            } else {
                sb.append(tte.getUnit());
            }
            sb.append("\r\n");

            if (!tte.hasChild()) {
                int size = tte.getRecordSize();
                long firstTime = tte.getRecord(0).time;
                long lastTime = tte.getRecord(size - 1).time;
                ArrayList<TimeEntry> tempRecordList = tte.getRecordList();

                sb.append("begin date,");
                sb.append(FormatUtils.getDate(firstTime));
                sb.append("\r\n");
                sb.append("end date,");
                sb.append(FormatUtils.getDate(lastTime));
                sb.append("\r\n");
                sb.append("count,");
                sb.append(size);
                sb.append("\r\n");
                sb.append("\r\n");

                sb.append("min,");
                sb.append(tte.getMin());
                sb.append("\r\n");
                sb.append("max,");
                sb.append(tte.getMax());
                sb.append("\r\n");
                sb.append("avg,");
                sb.append(tte.getAve());
                sb.append("\r\n");
                sb.append("\r\n");

                for (TimeEntry time : tempRecordList) {
                    if (sb.length() > 8192) {
                        writeNotClose(sb.toString(), f, fw);
                        sb = new StringBuffer();
                    }
                    sb.append(time);
                    sb.append("\r\n");
                }
            } else // 支持多组数据的保存
            {
                TagTimeEntry temp = tte.getChildren()[0];
                int size = temp.getRecordSize();
                long firstTime = temp.getRecord(0).time;
                long lastTime = temp.getRecord(size - 1).time;

                sb.append("begin date,");
                sb.append(FormatUtils.getDate(firstTime));
                sb.append("\r\n");
                sb.append("end date,");
                sb.append(FormatUtils.getDate(lastTime));
                sb.append("\r\n");
                sb.append("count,");
                sb.append(size);
                sb.append("\r\n");
                sb.append("\r\n");

                sb.append(",");
                for (TagTimeEntry child : tte.getChildren()) {
                    sb.append(child.getName());
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("\r\n");

                sb.append("min,");
                for (TagTimeEntry child : tte.getChildren()) {
                    sb.append(child.getMin());
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("\r\n");

                sb.append("max,");
                for (TagTimeEntry child : tte.getChildren()) {
                    sb.append(child.getMax());
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("\r\n");

                sb.append("avg,");
                for (TagTimeEntry child : tte.getChildren()) {
                    sb.append(child.getAve());
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("\r\n");
                sb.append("\r\n");

                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < tte.getSubTagEntrys().length; j++) {
                        TagTimeEntry subEntry = tte.getSubTagEntrys()[j];
                        TimeEntry time = subEntry.getRecord(i);

                        if (sb.length() > 8192) {
                            writeNotClose(sb.toString(), f, fw);
                            sb = new StringBuffer();
                        }

                        if (j == 0) {
                            sb.append(time);
                        } else {
                            sb.append(time.reduce);
                        }

                        if (j == tte.getSubTagEntrys().length - 1) {
                            sb.append("\r\n");
                        } else {
                            sb.append(",");
                        }
                    }
                }
            }
            if (tte.getRecordSize() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("\r\n");
            writeNotClose(sb.toString(), f, fw);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.closeWriter(fw);
        }

        // add on 20131225 有手动tag记录内存值的情况，先把tag的内存值保存了 start
        // 简单过滤保存  未操作

        File tagFile = new File(folder, "tagMem_" + saveEntry.now + LogUtils.GW_POSFIX);
        if (tagFile.exists()) {
            tagFile.delete();
        }

        FileWriter fwTagFile = null;
        try {
            fwTagFile = new FileWriter(tagFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer sb = null;

        sb = new StringBuffer();
        sb.append("time(ms)");
        sb.append(",");
        sb.append("DalvikHeapSize(KB)");
        sb.append(",");
        sb.append("DalvikAllocated(KB)");
        sb.append(",");
        sb.append("private_dirty(KB)");
        sb.append(",");
        sb.append("PSS_Total(KB)");
        sb.append(",");
        sb.append("PSS_Dalvik(KB)");
        sb.append(",");
        sb.append("PSS_Native(KB)");
        sb.append(",");
        sb.append("PSS_OtherDev(KB)");
        sb.append(",");
        sb.append("PSS_Graphics(KB)");
        sb.append(",");
        sb.append("PSS_GL(KB)");
        sb.append(",");
        sb.append("PSS_Unknow(KB)");
        sb.append("\r\n");

        //内存助手打开的情况

        writeNotClose(sb.toString(), tagFile, fwTagFile);
        FileUtil.closeWriter(fwTagFile);
        // add on 20131225 有手动tag记录内存值的情况，先把tag的内存值保存了 end
    }

    public static void writeGWDesc(final GWSaveEntry saveEntry, final TagTimeEntry... ttes) {
        String sFolder = Env.S_ROOT_FOLDER + saveEntry.path1 + FileUtil.separator + saveEntry.path2
                + FileUtil.separator + saveEntry.path3 + FileUtil.separator;
        File folder = new File(sFolder);
        folder.mkdirs();

        String fName = GW_DESC_PREFIX + saveEntry.now + GW_DESC_POSFIX;
        File f = new File(folder, fName);

        FileWriter fw = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("gtdesc:=");
            sb.append(saveEntry.desc);
            sb.append("\r\n");
            sb.append("\r\n");
            sb.append("opfiles:=");
            sb.append("\r\n");
            // 本次测试提交的文件
            boolean hasValidData = false;
            for (TagTimeEntry tte : ttes) {
                String tteFileName = getTagTimeEntryFileName(tte, saveEntry);
                if (!tte.hasChild() && tte.getRecordSize() > 0
                        || tte.hasChild() && tte.getSubTagEntrys()[0].getRecordSize() > 0) {
                    hasValidData = true;
                    sb.append(tteFileName);
                    sb.append("\r\n");
                }
            }
            if (hasValidData) {
                fw = new FileWriter(f, true);
                writeNotClose(sb.toString(), f, fw);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.closeWriter(fw);
        }
    }

    private static String getTagTimeEntryFileName(final TagTimeEntry tte, final GWSaveEntry saveEntry) {
        long lastDataTime = 0;
        if (!tte.hasChild() && tte.getRecordSize() > 0) {
            ArrayList<TimeEntry> dataList = tte.getRecordList();
            lastDataTime = dataList.get(dataList.size() - 1).time;
        } else if (tte.hasChild() && tte.getSubTagEntrys()[0].getRecordSize() > 0) {
            ArrayList<TimeEntry> dataList = tte.getSubTagEntrys()[0].getRecordList();
            lastDataTime = dataList.get(dataList.size() - 1).time;
        }

        String recordTime = lastDataTime == 0 ? saveEntry.now : FormatUtils.getSaveDate(lastDataTime);
        String tteFileName = tte.getName() + "_" + recordTime + LogUtils.GW_POSFIX;
        tteFileName = tteFileName.replace(':', '_');
        return tteFileName;
    }

    /*
	 * 不关闭输入输出流连接的写文件方式，用于保存日志快速读写的方式 但要保证调用该方法的事务都是完成即关闭输出流。 20140517
	 * 已查明本日以前调用该方法的地方，都是事务完成即关闭输出流的，所以不需要flush
	 */
    private static void writeNotClose(CharSequence sb, File f, FileWriter writer) {
        if (!f.exists()) {
            try {
                f.getParentFile().mkdirs();
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            // TODO writer如果长时间不写只能等关闭GT时做writer的全close操作
            writer.write(sb.toString());
            // writer.flush(); // 不flush是8k一存
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
