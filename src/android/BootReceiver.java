package com.example.ntpsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "系统启动完成，启动时间同步服务");
            
            // 启动NTP同步服务
            Intent ntpIntent = new Intent(context, NtpSyncService.class);
            context.startService(ntpIntent);
            
            // 启动HTTP同步服务
            Intent httpIntent = new Intent(context, HttpTimeSyncService.class);
            context.startService(httpIntent);
        }
    }
}
