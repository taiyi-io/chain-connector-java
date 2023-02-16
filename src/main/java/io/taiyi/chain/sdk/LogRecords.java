package io.taiyi.chain.sdk;

public class LogRecords {
    private int latest_version;
    private TraceLog[] logs;

    public int getLatestVersion() {
        return latest_version;
    }

    public void setLatestVersion(int latestVersion) {
        this.latest_version = latestVersion;
    }

    public TraceLog[] getLogs() {
        return logs;
    }

    public void setLogs(TraceLog[] logs) {
        this.logs = logs;
    }
}

