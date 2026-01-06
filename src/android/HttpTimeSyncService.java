package com.maintell.ntpsync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HttpTimeSyncService extends Service {
    
    private static final String TAG = "HttpTimeSyncService";
    private static final long SYNC_INTERVAL = 30 * 60 * 1000; // 30分钟
    
    // HTTP时间源 - 按优先级排序
    private static final String[] HTTP_TIME_SOURCES = {
        "http://223.5.5.5",      // 阿里DNS
        "http://223.6.6.6",      // 阿里DNS备用
        "http://www.baidu.com",
        "http://www.sohu.com",
        "http://www.sina.com.cn"
    };
    
    private static final int TIMEOUT = 5000; // 5秒超时
    
    private Handler handler;
    private Runnable syncRunnable;
    private static long lastSyncTime = 0;
    private static int syncCount = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "HTTP时间同步服务已创建");
        ensureForeground();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPeriodicSync();
        return START_STICKY;
    }

    private void ensureForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "http_time_sync_channel";
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = nm.getNotificationChannel(channelId);
                    if (channel == null) {
                        channel = new NotificationChannel(channelId, "HTTP Time Sync", NotificationManager.IMPORTANCE_LOW);
                        nm.createNotificationChannel(channel);
                    }
                }
                Notification.Builder builder = new Notification.Builder(this, channelId)
                    .setContentTitle("时间同步")
                    .setContentText("HTTP 时间同步正在运行")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true);
                startForeground(1002, builder.build());
            } else {
                Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle("时间同步")
                    .setContentText("HTTP 时间同步正在运行")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true);
                startForeground(1002, builder.build());
            }
        } catch (Exception e) {
            Log.w(TAG, "确保前台通知失败", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startPeriodicSync() {
        if (syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
        }
        
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                performSync();
                handler.postDelayed(this, SYNC_INTERVAL);
            }
        };
        
        // 启动后立即执行一次
        handler.post(syncRunnable);
    }
    
    private void performSync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long httpTime = getHttpTimeWithFallback();
                    if (httpTime > 0) {
                        boolean success = TimeUtils.setSystemTime(httpTime);
                        if (success) {
                            lastSyncTime = System.currentTimeMillis();
                            syncCount++;
                            Log.d(TAG, "HTTP时间同步成功: " + new java.util.Date(httpTime));
                        } else {
                            Log.e(TAG, "HTTP时间设置失败");
                        }
                    } else {
                        Log.e(TAG, "所有HTTP时间源均失败");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "HTTP同步异常", e);
                }
            }
        }).start();
    }
    
    public static long getHttpTimeWithFallback() {
        // 遍历所有HTTP时间源
        for (String source : HTTP_TIME_SOURCES) {
            try {
                Log.d(TAG, "尝试HTTP时间源: " + source);
                long time = getHttpTime(source);
                if (time > 0) {
                    Log.d(TAG, "成功从 " + source + " 获取时间");
                    return time;
                }
            } catch (Exception e) {
                Log.w(TAG, "HTTP时间源 " + source + " 失败: " + e.getMessage());
            }
        }
        return -1;
    }
    
    private static long getHttpTime(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // 只获取头部
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setInstanceFollowRedirects(false);
            
            // 不验证主机名和证书（因为使用HTTP）
            connection.connect();
            
            // 获取Date头
            String dateHeader = connection.getHeaderField("Date");
            if (dateHeader == null || dateHeader.isEmpty()) {
                Log.w(TAG, "未找到Date头: " + urlString);
                return -1;
            }
            
            Log.d(TAG, "收到Date头: " + dateHeader);
            
            // 解析GMT时间
            SimpleDateFormat gmtFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            Date gmtDate = gmtFormat.parse(dateHeader);
            if (gmtDate == null) {
                Log.w(TAG, "解析Date头失败: " + dateHeader);
                return -1;
            }
            
            // gmtDate 表示从服务器返回的 GMT 时刻（其内部为 UTC epoch 毫秒）
            long gmtMillis = gmtDate.getTime();

            // 不要再人为加 8 小时 — gmtMillis 已经是 UTC epoch 毫秒，
            // TimeUtils 期望的是 epoch 毫秒（或除以1000的秒数）。
            Date localDate = new Date(gmtMillis);
            Log.d(TAG, "解析到 GMT instant (UTC): " + gmtDate + ", 本地时间显示: " + localDate);

            return gmtMillis;
            
        } catch (Exception e) {
            Log.w(TAG, "HTTP时间获取失败 " + urlString + ": " + e.getMessage());
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public static int getSyncCount() {
        return syncCount;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
        }
        Log.d(TAG, "HTTP时间同步服务已销毁");
    }
}