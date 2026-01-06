package com.maintell.ntpsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "系统启动完成，尝试启动时间同步服务");

            // 启动NTP同步服务
            try {
                Intent ntpIntent = new Intent(context, NtpSyncService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(ntpIntent);
                } else {
                    context.startService(ntpIntent);
                }
                Log.d(TAG, "BootReceiver: 已请求启动 NtpSyncService");
            } catch (Exception e) {
                Log.e(TAG, "BootReceiver: 启动 NtpSyncService 失败", e);
            }

            // 启动HTTP同步服务
            try {
                Intent httpIntent = new Intent(context, HttpTimeSyncService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(httpIntent);
                } else {
                    context.startService(httpIntent);
                }
                Log.d(TAG, "BootReceiver: 已请求启动 HttpTimeSyncService");
            } catch (Exception e) {
                Log.e(TAG, "BootReceiver: 启动 HttpTimeSyncService 失败", e);
            }
        }
    }
}
