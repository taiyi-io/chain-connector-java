package io.taiyi.chain.sdk;

public class AccessKey {
    class PrivateData {
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

        public String getId() {
            return id;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getEncodeMethod(){
            return encodeMethod;
        }

    }

    private PrivateData privateData;

    public AccessKey(int version, String id, String encodeMethod, String privateKey) {
        this.privateData = new PrivateData(version, id, encodeMethod, privateKey);
    }

    public PrivateData getPrivateData() {
        return privateData;
    }
}


