package io.taiyi.chain.sdk;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    private List<ConditionFilter> filters;
    private String since;
    private int offset;
    private int limit;
    private String order;
    private boolean descend;

    public QueryBuilder() {
        this.filters = new ArrayList<>();
        this.since = null;
        this.offset = 0;
        this.limit = 0;
        this.order = "";
        this.descend = false;
    }

    public QueryBuilder PropertyEqual(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.EQUAL, value));
        return this;
    }

    public QueryBuilder PropertyNotEqual(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.NOT_EQUAL, value));
        return this;
    }

    public QueryBuilder PropertyGreaterThan(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.GREATER_THAN, value));
        return this;
    }

    public QueryBuilder PropertyLessThan(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.LESS_THAN, value));
        return this;
    }

    public QueryBuilder PropertyGreaterOrEqual(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.GREATER_OR_EQUAL, value));
        return this;
    }

    public QueryBuilder PropertyLessOrEqual(String propertyName, Object value) {
        this.filters.add(new ConditionFilter(propertyName, FilterOperator.LESS_OR_EQUAL, value));
        return this;
    }

    public QueryBuilder StartFrom(String value) {
        this.since = value;
        return this;
    }

    public QueryBuilder SetOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public QueryBuilder MaxRecord(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder AscendBy(String propertyName) {
        this.order = propertyName;
        return this;
    }

    public QueryBuilder DescendBy(String propertyName) {
        this.order = propertyName;
        this.descend = true;
        return this;
    }

    public QueryCondition Build() {
        return new QueryCondition(this.filters, this.since, this.offset, this.limit, this.order, this.descend);
    }
}

