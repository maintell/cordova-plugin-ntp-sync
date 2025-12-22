package com.example.ntpsync;

import android.util.Log;
import java.io.DataOutputStream;

public class TimeUtils {
    
    private static final String TAG = "TimeUtils";
    
    public static boolean setSystemTime(long timeMillis) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            
            // 使用date命令设置系统时间
            String command = "date -s @" + (timeMillis / 1000) + "\n";
            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();
            
            int exitValue = process.waitFor();
            boolean success = exitValue == 0;
            
            if (success) {
                Log.d(TAG, "系统时间设置成功: " + new java.util.Date(timeMillis));
            } else {
                Log.e(TAG, "系统时间设置失败，退出码: " + exitValue);
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "设置系统时间异常", e);
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
}