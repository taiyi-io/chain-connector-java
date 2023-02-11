package io.taiyi.chain.sdk;

public class AccessKey {
    private class PrivateData {
        int version;
        String id;
        String encodeMethod;
        String privateKey;

        public PrivateData(int version, String id, String encodeMethod, String privateKey) {
            this.version = version;
            this.id = id;
            this.encodeMethod = encodeMethod;
            this.privateKey = privateKey;
        }
    }

    private PrivateData privateData;

    public AccessKey(int version, String id, String encodeMethod, String privateKey) {
        this.privateData = new PrivateData(version, id, encodeMethod, privateKey);
    }
}


