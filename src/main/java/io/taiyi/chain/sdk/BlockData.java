package io.taiyi.chain.sdk;

public class BlockData {
    private String id;
    private String timestamp;
    private String previous_block;
    private int height;
    private int transactions;
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPreviousBlock() {
        return previous_block;
    }

    public void setPreviousBlock(String previousBlock) {
        this.previous_block = previousBlock;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

