package com.maintell.ntpsync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Build;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NtpSyncPlugin extends CordovaPlugin {
    
    private static final String TAG = "NtpSyncPlugin";
    
    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        // 插件初始化时自动启动双重同步服务
        Log.d(TAG, "pluginInitialize called");
        Context context = this.cordova.getActivity().getApplicationContext();
        try {
            startServices(context);
            Log.d(TAG, "startServices invoked from pluginInitialize");
        } catch (Exception e) {
            Log.e(TAG, "启动同步服务失败", e);
        }
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
            throws JSONException {
        
        Context context = this.cordova.getActivity().getApplicationContext();
        
        if ("stopSync".equals(action)) {
            this.stopSync(context, callbackContext);
            return true;
        }
        
        if ("syncNtpNow".equals(action)) {
            this.syncNtpNow(context, callbackContext);
            return true;
        }
        
        if ("syncHttpNow".equals(action)) {
            this.syncHttpNow(context, callbackContext);
            return true;
        }
        
        if ("getSyncStatus".equals(action)) {
            this.getSyncStatus(callbackContext);
            return true;
        }
        
        return false;
    }
    
    private void startServices(Context context) {
        // 启动NTP同步服务
        try {
            Intent ntpIntent = new Intent(context, NtpSyncService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(ntpIntent);
            } else {
                context.startService(ntpIntent);
            }
            Log.d(TAG, "启动 NtpSyncService 调用已发出");
        } catch (Exception e) {
            Log.e(TAG, "启动 NtpSyncService 失败", e);
        }

        // 启动HTTP同步服务
        try {
            Intent httpIntent = new Intent(context, HttpTimeSyncService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(httpIntent);
            } else {
                context.startService(httpIntent);
            }
            Log.d(TAG, "启动 HttpTimeSyncService 调用已发出");
        } catch (Exception e) {
            Log.e(TAG, "启动 HttpTimeSyncService 失败", e);
        }
    }
    
    private void stopSync(Context context, CallbackContext callbackContext) {
        context.stopService(new Intent(context, NtpSyncService.class));
        context.stopService(new Intent(context, HttpTimeSyncService.class));
        callbackContext.success("所有同步服务已停止");
    }
    
    private void syncNtpNow(final Context context, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long ntpTime = NtpSyncService.getNtpTimeWithFallback();
                    if (ntpTime > 0) {
                        boolean success = TimeUtils.setSystemTime(ntpTime);
                        if (success) {
                            callbackContext.success("NTP时间同步成功: " + new java.util.Date(ntpTime));
                        } else {
                            callbackContext.error("设置系统时间失败(需要root或系统权限)");
                        }
                    } else {
                        callbackContext.error("获取NTP时间失败");
                    }
                } catch (Exception e) {
                    callbackContext.error("NTP同步失败: " + e.getMessage());
                }
            }
        });
    }
    
    private void syncHttpNow(final Context context, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long httpTime = HttpTimeSyncService.getHttpTimeWithFallback();
                    if (httpTime > 0) {
                        boolean success = TimeUtils.setSystemTime(httpTime);
                        if (success) {
                            callbackContext.success("HTTP时间同步成功: " + new java.util.Date(httpTime));
                        } else {
                            callbackContext.error("设置系统时间失败(需要root或系统权限)");
                        }
                    } else {
                        callbackContext.error("获取HTTP时间失败");
                    }
                } catch (Exception e) {
                    callbackContext.error("HTTP同步失败: " + e.getMessage());
                }
            }
        });
    }
    
    private void getSyncStatus(CallbackContext callbackContext) {
        try {
            JSONObject status = new JSONObject();
            status.put("ntpLastSync", NtpSyncService.getLastSyncTime());
            status.put("httpLastSync", HttpTimeSyncService.getLastSyncTime());
            status.put("ntpSyncCount", NtpSyncService.getSyncCount());
            status.put("httpSyncCount", HttpTimeSyncService.getSyncCount());
            callbackContext.success(status);
        } catch (Exception e) {
            callbackContext.error("获取状态失败: " + e.getMessage());
        }
    }
}
