package io.taiyi.chain.sdk;

public class ContractInfo {
    private String name;
    private Integer parameters;
    private Integer steps;
    private Integer version;
    private String modified_time;
    private Boolean enabled;
    private Boolean trace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParameters() {
        return parameters;
    }

    public void setParameters(Integer parameters) {
        this.parameters = parameters;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getModifiedTime() {
        return modified_time;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modified_time = modifiedTime;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isTraced() {
        return trace;
    }

}
