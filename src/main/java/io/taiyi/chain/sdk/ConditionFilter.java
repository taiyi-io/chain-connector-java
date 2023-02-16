package io.taiyi.chain.sdk;

public class ConditionFilter {
    private String property;
    private int operator;
    private String value;

    public ConditionFilter(String property, FilterOperator operator, Object value) {
        this.property = property;
        this.operator = operator.getValue();
        this.value = value.toString();
    }
}

