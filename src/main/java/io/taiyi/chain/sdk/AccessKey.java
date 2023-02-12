package io.taiyi.chain.sdk;

public class AccessKey {
    class PrivateData {
        int version;
        String id;
        String encode_method;
        String private_key;

        public PrivateData(int version, String id, String encodeMethod, String privateKey) {
            this.version = version;
            this.id = id;
            this.encode_method = encodeMethod;
            this.private_key = privateKey;
        }

        public String getId() {
            return id;
        }

        public String getPrivateKey() {
            return private_key;
        }

        public String getEncodeMethod(){
            return encode_method;
        }

    }

    private PrivateData private_data;

    public AccessKey(int version, String id, String encodeMethod, String privateKey) {
        private_data = new PrivateData(version, id, encodeMethod, privateKey);
    }

    public PrivateData getPrivateData() {
        return private_data;
    }
}


