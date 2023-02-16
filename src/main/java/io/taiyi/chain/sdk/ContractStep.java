package io.taiyi.chain.sdk;

import java.util.List;

public class ContractStep {
    private String action;
    private String[] params = null;
    public ContractStep(String action){
        this.action = action;
    }

    public ContractStep(String action, String[] parameters){
        this.action = action;
        this.params = parameters;
    }
    public ContractStep(String action, List<String> parameters){
        this.action = action;
        this.params = parameters.toArray(new String[0]);
    }
    public String getAction() {
        return action;
    }

    public String[] getParams() {
        return params;
    }

}

