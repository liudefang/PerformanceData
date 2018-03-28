package com.boyaa.autotest.performancedata.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by JessicZeng on 2017/6/23.
 */
public class UploadFile {

    private static final String TAG = "FileUpload";

    /**
     *
     * @param remoteUrl  服务器url  http://wh.tunnel.qydev.com/texas_local/cwh/upload/upload_file.php
     * @param filePath   文件路径   /sdcard/tagMem1.csv
     * @param fileName   文件名     tagMem1.csv
     * @param fileKey    服务器端接收的key
     * @param mimeType   content-Type  每种文件类型需对应不同的类型   csv：application/vnd.ms-excel
     */
    public void upload(String remoteUrl, String filePath, String fileName,String fileKey,String mimeType) {
        Log.i(TAG, "开始执行upload");
        HttpURLConnection connection = null;
        DataOutputStream outStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead;
        int bytesAvailable;
        int bufferSize;

        byte[] buffer;

        int maxBufferSize = 1024 * 1024;
        int timeout = 10000;
        try {
            FileInputStream fileInputStream = null;
            try {
                File file = new File(filePath);
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                Log.i(TAG, "待上传文件打开失败");
                e.printStackTrace();
                return ;
            }

            URL url = new URL(remoteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            outStream = new DataOutputStream(connection.getOutputStream());

            outStream.writeBytes(twoHyphens + boundary + lineEnd);
            outStream.writeBytes("Content-Disposition: form-data;name=\""+ fileKey + "\";filename=\"" + fileName + "\"" + lineEnd
                    + "Content-Type: " + mimeType + lineEnd
                    + "Content-Transfer-Encoding: binary" + lineEnd + lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                outStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outStream.writeBytes(lineEnd);
            outStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            fileInputStream.close();
            outStream.flush();
            outStream.close();
            Log.i(TAG, "关闭file完成");
        } catch (MalformedURLException e) {
            Log.i(TAG, "MalformedURLException");
            e.printStackTrace();
            return ;
        } catch (ProtocolException e) {
            Log.i(TAG, "ProtocolException");
            e.printStackTrace();
            return ;
        }catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "ArrayIndexOutOfBoundsException");
            e.printStackTrace();
            return ;
        } catch (IOException e) {
            Log.i(TAG, "IOException");
            e.printStackTrace();
            return ;
        }

        try {
            BufferedReader in = null;
            String result = "";
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            in.close();
            connection.disconnect();
            Log.i(TAG, "网页返回值："+result);
        } catch (IOException e) {
            Log.i(TAG, "读取网页返回值失败");
            e.printStackTrace();
        } finally {

        }
    }
}
