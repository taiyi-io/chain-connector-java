package io.taiyi.chain.sdk;

public class Constants {
    public static final String API_VERSION = "1";
    public static final String DEFAULT_PROJECT_NAME = "Taiyi";
    public static final String DEFAULT_DOMAIN_NAME = "system";
    public static final String DEFAULT_DOMAIN_HOST = "localhost";
    public static final String SIGNATURE_METHOD_ED25519 = "ed25519";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String KEY_ENCODE_METHOD_ED25519_HEX = "ed25519-hex";
    public static final String DEFAULT_KEY_ENCODE_METHOD = KEY_ENCODE_METHOD_ED25519_HEX;
    public static final int DEFAULT_TIMEOUT_IN_SECONDS = 3;
}

enum PropertyType {
    String("string"),
    Boolean("bool"),
    Integer("int"),
    Float("float"),
    Currency("currency"),
    Collection("collection"),
    Document("document");
    private final String value;

    PropertyType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
}

enum RequestMethod {
    GET("GET"),
    PUT("PUT"),
    POST("POST"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    PATCH("PATCH");

    private final String value;

    RequestMethod(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
}