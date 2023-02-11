package io.taiyi.chain.sdk;

public class ChainStatus {
    private String worldVersion;
    private int blockHeight;
    private String previousBlock;
    private String genesisBlock;
    private String allocatedTransactionId;

    public String getWorldVersion() {
        return worldVersion;
    }

    public void setWorldVersion(String worldVersion) {
        this.worldVersion = worldVersion;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getPreviousBlock() {
        return previousBlock;
    }

    public void setPreviousBlock(String previousBlock) {
        this.previousBlock = previousBlock;
    }

    public String getGenesisBlock() {
        return genesisBlock;
    }

    public void setGenesisBlock(String genesisBlock) {
        this.genesisBlock = genesisBlock;
    }

    public String getAllocatedTransactionId() {
        return allocatedTransactionId;
    }

    public void setAllocatedTransactionId(String allocatedTransactionId) {
        this.allocatedTransactionId = allocatedTransactionId;
    }
}

