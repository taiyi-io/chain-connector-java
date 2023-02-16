package io.taiyi.chain.sdk;

public class ChainStatus {
    private String world_version;
    private int block_height;
    private String previous_block;
    private String genesis_block;
    private String allocated_transaction_id;

    public String getWorldVersion() {
        return world_version;
    }

    public void setWorldVersion(String worldVersion) {
        this.world_version = worldVersion;
    }

    public int getBlockHeight() {
        return block_height;
    }

    public void setBlockHeight(int blockHeight) {
        this.block_height = blockHeight;
    }

    public String getPreviousBlock() {
        return previous_block;
    }

    public void setPreviousBlock(String previousBlock) {
        this.previous_block = previousBlock;
    }

    public String getGenesisBlock() {
        return genesis_block;
    }

    public void setGenesisBlock(String genesisBlock) {
        this.genesis_block = genesisBlock;
    }

    public String getAllocatedTransactionId() {
        return allocated_transaction_id;
    }

    public void setAllocatedTransactionId(String allocatedTransactionId) {
        this.allocated_transaction_id = allocatedTransactionId;
    }
}

