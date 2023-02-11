package io.taiyi.chain.sdk;

public class ContractDefine {
    private ContractStep[] steps;
    private ContractParameter[] parameters;

    public ContractStep[] getSteps() {
        return steps;
    }

    public void setSteps(ContractStep[] steps) {
        this.steps = steps;
    }

    public ContractParameter[] getParameters() {
        return parameters;
    }

    public void setParameters(ContractParameter[] parameters) {
        this.parameters = parameters;
    }
}
