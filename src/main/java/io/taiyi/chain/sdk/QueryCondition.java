package io.taiyi.chain.sdk;

import java.util.ArrayList;
import java.util.List;

public class QueryCondition {
    private List<ConditionFilter> filters;
    private String since;
    private int offset;
    private int limit;
    private String order;
    private boolean descend;

    public QueryCondition() {
        this.filters = new ArrayList<>();
        this.since = "";
        this.offset = 0;
        this.limit = 0;
        this.order = "";
        this.descend = false;
    }

    public QueryCondition(List<ConditionFilter> filters, String since, int offset, int limit, String order, boolean descend) {
        this.filters = filters;
        this.since = since;
        this.offset = offset;
        this.limit = limit;
        this.order = order;
        this.descend = descend;
    }

    public List<ConditionFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<ConditionFilter> filters) {
        this.filters = filters;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isDescend() {
        return descend;
    }

    public void setDescend(boolean descend) {
        this.descend = descend;
    }
}


