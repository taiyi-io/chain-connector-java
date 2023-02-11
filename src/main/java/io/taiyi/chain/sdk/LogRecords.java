package io.taiyi.chain.sdk;

public class LogRecords {
    private int latestVersion;
    private TraceLog[] logs;

    public int getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(int latestVersion) {
        this.latestVersion = latestVersion;
    }

    public TraceLog[] getLogs() {
        return logs;
    }

    public void setLogs(TraceLog[] logs) {
        this.logs = logs;
    }
}

