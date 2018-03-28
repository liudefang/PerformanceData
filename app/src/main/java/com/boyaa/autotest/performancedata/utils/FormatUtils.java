package com.boyaa.autotest.performancedata.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 格式类
 * Created by JessicZeng on 2017/2/22.
 */
public class FormatUtils {

    /**
     * 获得系统时间
     *
     * @return
     */
    private static SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static String getSystemTime() {
        Date date = new Date();
        return simpleTimeFormat.format(date);
    }

    public static String getSystemTime(long date) {
        return simpleTimeFormat.format(new Date(date));
    }

    // 获取系统短日期时间
    private static SimpleDateFormat simpleDateTimeFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    public static String getSystemDateTime() {
        Date date = new Date();
        return simpleDateTimeFormat.format(date);
    }

    public static String getSystemDateTime(long date) {
        return simpleDateTimeFormat.format(new Date(date));
    }

    // GPS使用的日期格式
    private static SimpleDateFormat gpsDataFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static String getGpsSaveTime() {
        Date date = new Date();
        return gpsDataFormatter.format(date);
    }

    public static String getGpsSaveTime(long data) {
        return gpsDataFormatter.format(new Date(data));
    }

    public static String getGpsSaveTime(Date date) {
        return gpsDataFormatter.format(date);
    }

    // 供外部模块做保存操作时引用的日期格式转换器
    private static SimpleDateFormat saveFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public static String getSaveTime() {
        Date date = new Date();
        return saveFormatter.format(date);
    }

    public static String getSaveTime(long data) {
        return saveFormatter.format(new Date(data));
    }

    // 日期，到ms
    private static SimpleDateFormat saveDateMsFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);

    public static String getSaveDateMs() {
        Date date = new Date();
        return saveDateMsFormatter.format(date);
    }

    public static String getSaveDateMs(long data) {
        return saveDateMsFormatter.format(new Date(data));
    }

    // 日期，到s
    private static SimpleDateFormat saveDateFormatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

    public static String getSaveDate() {
        Date date = new Date();
        return saveDateFormatter.format(date);
    }

    public static String getSaveDate(long data) {
        return saveDateFormatter.format(new Date(data));
    }

    // 日期，到日
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static String getDate() {
        Date date = new Date();
        return dateFormatter.format(date);
    }

    public static String getDate(long data) {
        return dateFormatter.format(new Date(data));
    }
}
