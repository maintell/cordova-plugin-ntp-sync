var exec = require('cordova/exec');

var NtpSync = {
    /**
     * 停止所有同步服务
     */
    stopSync: function(success, error) {
        exec(success, error, "NtpSync", "stopSync", []);
    },
    
    /**
     * 立即执行一次NTP时间同步
     */
    syncNtpNow: function(success, error) {
        exec(success, error, "NtpSync", "syncNtpNow", []);
    },
    
    /**
     * 立即执行一次HTTP时间同步
     */
    syncHttpNow: function(success, error) {
        exec(success, error, "NtpSync", "syncHttpNow", []);
    },
    
    /**
     * 获取同步状态
     */
    getSyncStatus: function(success, error) {
        exec(success, error, "NtpSync", "getSyncStatus", []);
    }
};

module.exports = NtpSync;