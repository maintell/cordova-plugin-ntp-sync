package com.example.ntpsync;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NtpSyncService extends Service {
    
    private static final String TAG = "NtpSyncService";
    private static final long SYNC_INTERVAL = 30 * 60 * 1000; // 30分钟
    
    // NTP服务器组 - 按优先级排序
    private static final String[][] NTP_SERVERS = {
        {"ntp.aliyun.com"},
        {"ntp.tencent.com"},
        {"cn.pool.ntp.org"},
        {"time.cloudflare.com"}
    };
    
    private static final int NTP_PORT = 123;
    private static final int TIMEOUT = 5000; // 5秒超时
    
    private Handler handler;
    private Runnable syncRunnable;
    private static long lastSyncTime = 0;
    private static int syncCount = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "NTP同步服务已创建");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPeriodicSync();
        return START_STICKY;
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
                    long ntpTime = getNtpTimeWithFallback();
                    if (ntpTime > 0) {
                        boolean success = TimeUtils.setSystemTime(ntpTime);
                        if (success) {
                            lastSyncTime = System.currentTimeMillis();
                            syncCount++;
                            Log.d(TAG, "NTP时间同步成功: " + new java.util.Date(ntpTime));
                        } else {
                            Log.e(TAG, "NTP时间设置失败");
                        }
                    } else {
                        Log.e(TAG, "所有NTP服务器均失败");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "NTP同步异常", e);
                }
            }
        }).start();
    }
    
    public static long getNtpTimeWithFallback() {
        // 遍历所有服务器组
        for (String[] serverGroup : NTP_SERVERS) {
            for (String server : serverGroup) {
                try {
                    // 获取该域名的所有IP地址
                    InetAddress[] addresses = InetAddress.getAllByName(server);
                    
                    // 尝试每个IP地址
                    for (InetAddress address : addresses) {
                        Log.d(TAG, "尝试NTP服务器: " + server + " (" + address.getHostAddress() + ")");
                        long time = getNtpTime(address);
                        if (time > 0) {
                            Log.d(TAG, "成功从 " + server + " 获取时间");
                            return time;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "NTP服务器 " + server + " 失败: " + e.getMessage());
                }
            }
        }
        return -1;
    }
    
    private static long getNtpTime(InetAddress address) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT);
            
            byte[] buffer = new byte[48];
            buffer[0] = 0x1B; // NTP协议版本3，客户端模式
            
            DatagramPacket request = new DatagramPacket(buffer, buffer.size, address, NTP_PORT);
            socket.send(request);
            
            DatagramPacket response = new DatagramPacket(buffer, buffer.size);
            socket.receive(response);
            
            // 解析NTP时间戳（字节40-43是传输时间戳的秒部分）
            long seconds = ((long)(buffer[40] & 0xFF) << 24) |
                          ((long)(buffer[41] & 0xFF) << 16) |
                          ((long)(buffer[42] & 0xFF) << 8) |
                          ((long)(buffer[43] & 0xFF));
            
            // NTP epoch是1900年1月1日，Java epoch是1970年1月1日
            // 相差2208988800秒
            long epoch = seconds - 2208988800L;
            return epoch * 1000L; // 转换为毫秒
            
        } catch (Exception e) {
            Log.w(TAG, "NTP请求失败: " + e.getMessage());
            return -1;
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
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
        Log.d(TAG, "NTP同步服务已销毁");
    }
}