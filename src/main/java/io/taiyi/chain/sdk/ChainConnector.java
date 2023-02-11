package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ChainConnector {
    private String _accessID = "";
    private byte[] _privateKey;
    private String _apiBase = "";
    private String _domain = "";
    private String _nonce = "";
    private String _sessionID = "";
    private int _timeout = 0;
    private String _localIP = "";
    private boolean _trace = false;
    private int _requestTimeout = Constants.DEFAULT_TIMEOUT_IN_SECONDS * 1000;

    public ChainConnector(String accessID, byte[] privateKey) {
        this._accessID = accessID;
        this._privateKey = privateKey;
        this._apiBase = "";
        this._domain = "";
        this._nonce = "";
        this._sessionID = "";
        this._timeout = 0;
        this._localIP = "";
        this._requestTimeout = Constants.DEFAULT_TIMEOUT_IN_SECONDS * 1000;
    }

    public String getVersion() {
        return Constants.SDK_VERSION;
    }

    public String getSessionID() {
        return this._sessionID;
    }

    public String getAccessID() {
        return this._accessID;
    }

    public String getLocalIP() {
        return this._localIP;
    }

    public void setTrace(boolean flag) {
        this._trace = flag;
    }

    public void setTimeout(int timeoutInSeconds) {
        this._requestTimeout = timeoutInSeconds * 1000;
    }

//    public Promise<Object> connect(String host, int port) {
//        return this.connectToDomain(host, port, defaultDomainName);
//    }

    private String newNonce() {
        final int nonceLength = 16;
        boolean useLetters = true;
        boolean useNumbers = true;
        return RandomStringUtils.random(nonceLength, useLetters, useNumbers);
    }

    private String base64Signature(Object obj) {
        return "not implement";
//        String content = new String(JSON.stringify(obj).getBytes("UTF-8"));
//        byte[] contentBytes = content.getBytes("UTF-8");
//
//        Signature signature = Signature.getInstance("Ed25519");
//        signature.initSign(this._privateKey);
//        signature.update(contentBytes);
//        byte[] signed = signature.sign();
//
//        return Base64.getEncoder().encodeToString(signed);
    }

    private HttpRequest prepareOptions(RequestMethod method, String url, Object payload) throws Exception {
        URL urlObject = new URL(url);
        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(now);
        class signaturePayload {
            private String id;
            private String method;
            private String url;
            private String body;
            private String access;
            private String timestamp;
            private String nonce;
            private String signature_algorithm;
        }
        signaturePayload content = new signaturePayload();
        content.id = _sessionID;
        content.method =  method.toString().toUpperCase();
        content.url = urlObject.getPath();
        content.body = "";
        content.access = _accessID;
        content.timestamp = timestamp;
        content.nonce = _nonce;
        content.signature_algorithm = Constants.SIGNATURE_METHOD_ED25519;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(new URI(url));
        //todo: set method
        String bodyPayload;
        if (null != payload){
            //has payload
            builder.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
            Gson gson = new GsonBuilder().create();
            bodyPayload = gson.toJson(payload);
        }else{
            bodyPayload = "";
        }

        if (method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.DELETE || method == RequestMethod.PATCH) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bodyPayload.getBytes(StandardCharsets.UTF_8));
            content.body = Base64.getEncoder().encodeToString(hash);
        }
        String signature = base64Signature(content);
        builder.headers(
                "Pragma", "no-cache",
                "Cache-Control", "no-cache",
                Constants.HEADER_NAME_SESSION, _sessionID,
                Constants.HEADER_NAME_TIMESTAMP, timestamp,
                Constants.HEADER_NAME_SIGNATURE_ALGORITHM, Constants.SIGNATURE_METHOD_ED25519,
                Constants.HEADER_NAME_SIGNATURE, signature
        );
        return builder.build();
    }

    private String mapToAPI(String path) {
        return this._apiBase + path;
    }
    private String mapToDomain(String path) {
        return this._apiBase + "/domains/" + this._domain + path;
    }
}