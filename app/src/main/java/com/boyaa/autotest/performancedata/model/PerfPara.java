package com.boyaa.autotest.performancedata.model;

/**
 * 性能指标参数
 * Created by JessicZeng on 2017/2/23.
 */
public class PerfPara {

    private String name;
    private String functionID;
    /**
     *标记是否开启监控，由于app启动便开始监控  默认为true
     */
    private boolean monitor = true; // 标记是否监控
    public boolean hasMonitorOnce = false; // 标记是否曾监控过

    public PerfPara(String pName){
        name = pName;
    }

    public String getName(){
        return name;
    }

    public String getFunctionID(){
        return functionID;
    }

    public boolean getMonitor(){
        return monitor;
    }

    public void setMonitor(boolean m){
        monitor = m;
    }
}
