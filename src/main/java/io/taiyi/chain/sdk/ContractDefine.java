package io.taiyi.chain.sdk;

import java.util.List;

public class ContractDefine {
    private List<ContractStep> steps;
    private List<ContractParameter> parameters = null;

    public List<ContractStep> getSteps() {
        return steps;
    }

    public ContractDefine(List<ContractStep> steps){
        this.steps = steps;
    }
    public ContractDefine(List<ContractStep> steps, List<ContractParameter> parameters){
        this.steps = steps;
        this.parameters = parameters;
    }

    public List<ContractParameter> getParameters() {
        return parameters;
    }

}
