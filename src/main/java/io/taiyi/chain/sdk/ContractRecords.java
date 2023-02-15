package io.taiyi.chain.sdk;

import java.util.List;

public class ContractRecords {
    private ContractInfo[] contracts;
    private int limit;
    private int offset;
    private int total;

    public ContractInfo[] getContracts() {
        return contracts;
    }

    public void setContracts(ContractInfo[] contracts) {
        this.contracts = contracts;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

