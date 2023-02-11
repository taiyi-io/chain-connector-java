package io.taiyi.chain.sdk;

public class BlockRecords {
    private String[] blocks;
    private int from;
    private int to;
    private int height;

    public String[] getBlocks() {
        return blocks;
    }

    public void setBlocks(String[] blocks) {
        this.blocks = blocks;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}

