package io.taiyi.chain.sdk;

public class Constants {
    public static final String SDK_VERSION = "0.4.0";
    public static final String API_VERSION = "1";
    public static final String PROJECT_NAME = "Paimon";
    public static final String HEADER_NAME_SESSION = PROJECT_NAME + "-Session";
    public static final String HEADER_NAME_TIMESTAMP = PROJECT_NAME + "-Timestamp";
    public static final String HEADER_NAME_SIGNATURE = PROJECT_NAME + "-Signature";
    public static final String HEADER_NAME_SIGNATURE_ALGORITHM = PROJECT_NAME + "-SignatureAlgorithm";
    public static final String DEFAULT_DOMAIN_NAME = "system";
    public static final String DEFAULT_DOMAIN_HOST = "localhost";

    public static final String SIGNATURE_ENCODE = "base64";
    public static final String SIGNATURE_METHOD_ED25519 = "ed25519";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String PAYLOAD_PATH_ERROR_CODE = "error_code";
    public static final String PAYLOAD_PATH_ERROR_MESSAGE = "message";
    public static final String PAYLOAD_PATH_DATA = "data";
    public static final String KEY_ENCODE_METHOD_ED25519_HEX = "ed25519-hex";
    public static final String DEFAULT_KEY_ENCODE_METHOD = KEY_ENCODE_METHOD_ED25519_HEX;
    public static final int DEFAULT_TIMEOUT_IN_SECONDS = 3;
}

enum PropertyType {
    String, Boolean, Integer, Float, Currency, Collection, Document;

    public String toString() {
        switch (this) {
            case String:
                return "string";
            case Boolean:
                return "bool";
            case Integer:
                return "int";
            case Float:
                return "float";
            case Currency:
                return "currency";
            case Collection:
                return "collection";
            case Document:
                return "document";
            default:
                return "";
        }
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