============================================
使用示例 (在Cordova项目中)
============================================

// 插件会自动启动，无需手动调用startSync

// 立即执行一次NTP同步
NtpSync.syncNtpNow(
    function(msg) {
        console.log('NTP同步: ' + msg);
    },
    function(err) {
        console.error('NTP失败: ' + err);
    }
);

// 立即执行一次HTTP同步
NtpSync.syncHttpNow(
    function(msg) {
        console.log('HTTP同步: ' + msg);
    },
    function(err) {
        console.error('HTTP失败: ' + err);
    }
);

// 获取同步状态
NtpSync.getSyncStatus(
    function(status) {
        console.log('NTP最后同步: ' + new Date(status.ntpLastSync));
        console.log('HTTP最后同步: ' + new Date(status.httpLastSync));
        console.log('NTP同步次数: ' + status.ntpSyncCount);
        console.log('HTTP同步次数: ' + status.httpSyncCount);
    },
    function(err) {
        console.error('获取状态失败: ' + err);
    }
);

// 停止所有同步服务（一般不需要）
NtpSync.stopSync(
    function(msg) {
        console.log(msg);
    },
    function(err) {
        console.error(err);
    }
);
