package io.taiyi.chain.sdk;

public class ActorPrivileges {
    private String group;
    private boolean owner;
    private boolean executor;
    private boolean updater;
    private boolean viewer;

    public ActorPrivileges(String groupName, boolean isOwner, boolean isExecutor, boolean isUpdater, boolean isViewer){
        group = groupName;
        owner = isOwner;
        executor = isExecutor;
        updater = isUpdater;
        viewer = isViewer;
    }
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean isOwner) {
        this.owner = isOwner;
    }

    public boolean isExecutor() {
        return executor;
    }

    public void setExecutor(boolean isExecutor) {
        this.executor = isExecutor;
    }

    public boolean isUpdater() {
        return updater;
    }

    public void setUpdater(boolean isUpdater) {
        this.updater = isUpdater;
    }

    public boolean isViewer() {
        return viewer;
    }

    public void setViewer(boolean isViewer) {
        this.viewer = isViewer;
    }
}

