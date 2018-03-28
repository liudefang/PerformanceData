package com.boyaa.autotest.performancedata;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.boyaa.autotest.performancedata.manager.PerfManager;
import com.boyaa.autotest.performancedata.log.LogUtils;
import com.boyaa.autotest.performancedata.utils.Env;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            toBackground(2);
        } catch (IOException e) {
            LogUtils.log("切换到后台运行失败");
            e.printStackTrace();
        }
        LogUtils.log("MainActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        PerfManager.getInstance().startCollect();
    }

    /**
     * 模拟按下back键，切换到后台运行
     * @param style  1:模拟按下返回键   2：moveTaskToBack  3：finish
     *  finish默认调用的也是moveToTaskBack
     */
    private void toBackground(int style) throws IOException {
        if(!Env.isToBack){
            LogUtils.log("!Env.isToBack开关未打开，退出设置应用自动退出");
            return;
        }
        LogUtils.log("Analysis | 选择的应用退到后台方法：" + style);
        switch (style){
            case 1:
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                break;
            case 2:
                moveTaskToBack(false);
                break;
            case 3:
                finish();
                break;
            default:
                moveTaskToBack(false);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        LogUtils.log("writeToFile | 应用被销毁11");
        //PerfManager.getInstance().saveToSD();
        super.onDestroy();
    }

    public static Context getContext() {
        return mContext;
    }
}
