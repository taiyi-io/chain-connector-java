package io.taiyi.chain.sdk;

public enum FilterOperator {
    EQUAL(0),
    NOT_EQUAL(1),
    GREATER_THAN(2),
    LESS_THAN(3),
    GREATER_OR_EQUAL(4),
    LESS_OR_EQUAL(5);

    private int value;
    private FilterOperator(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}