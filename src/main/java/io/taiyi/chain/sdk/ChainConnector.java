package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class ChainConnector {
    final private static String DefaultAlgorithmName = "Ed25519";
    private String _accessID = "";
    private PrivateKey _privateKey;
    private String _apiBase = "";
    private String _domain = "";
    private String _nonce = "";
    private String _sessionID = "";
    private int _timeout = 0;
    private String _localIP = "";
    private boolean _trace = false;
    private int _requestTimeout = Constants.DEFAULT_TIMEOUT_IN_SECONDS * 1000;

    private HttpClient _client;

    class responseFormat {
        private int error_code;
        private String message;
        private Object data;
    }

    public static ChainConnector NewConnectorFromAccess(AccessKey key) throws Exception {
        String id = key.getPrivateData().getId();
        String encodeMethod = key.getPrivateData().getEncodeMethod();
        String privateKey = key.getPrivateData().getPrivateKey();
        if (Constants.DEFAULT_KEY_ENCODE_METHOD.equals(encodeMethod)) {
            if (!isHex(privateKey)) {
                throw new Exception("invalid key format");
            }
            byte[] decoded = slice(hexToBin(privateKey), 0, 32);
            return NewConnector(id, decoded);
        } else {
            throw new Exception("unsupported encode method: " + encodeMethod);
        }
    }
    public static ChainConnector NewConnector(String accessID, byte[] privateKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        return new ChainConnector(accessID, privateKey);
    }

    public ChainConnector(String accessID, byte[] privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this._accessID = accessID;
        this._privateKey = generatePrivateKey(privateKey);
        this._apiBase = "";
        this._domain = "";
        this._nonce = "";
        this._sessionID = "";
        this._timeout = 0;
        this._localIP = "";
        this._requestTimeout = Constants.DEFAULT_TIMEOUT_IN_SECONDS * 1000;
        this._client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
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

    public void connect(String host, int port) throws Exception {
        connectToDomain(host, port, Constants.DEFAULT_DOMAIN_NAME);
    }

    public void connectToDomain(String host, int port, String domainName) throws Exception {
        String remoteHost;
        if (host.equals("")) {
            remoteHost = Constants.DEFAULT_DOMAIN_HOST;
        } else {
            remoteHost = host;
        }
        if (domainName.equals("")) {
            throw new Error("domain name omit");
        }
        if (port <= 0 || port > 0xFFFF) {
            throw new Error("invalid port " + port);
        }
        _apiBase = "http://" + remoteHost + ":" + port + "/api/v" + Constants.API_VERSION;
        _domain = domainName;
        _nonce = newNonce();
        Date now = new Date();
        SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String timestamp = RFC3339.format(now);
        String signatureAlgorithm = Constants.SIGNATURE_METHOD_ED25519;
        class signatureFormat {
            private String access;
            private String timestamp;
            private String nonce;
            private String signature_algorithm;
        }
        signatureFormat signaturePayload = new signatureFormat();
        signaturePayload.access = _accessID;
        signaturePayload.timestamp = timestamp;
        signaturePayload.nonce = _nonce;
        signaturePayload.signature_algorithm = signatureAlgorithm;
        //generate signature
        String signature = this.base64Signature(signaturePayload);

        //generate request payload
        class requestFormat {
            private String id;
            private String nonce;
        }
        requestFormat requestPayload = new requestFormat();
        requestPayload.id = _accessID;
        requestPayload.nonce = _nonce;

        ArrayList<Pair<String, String>> headers = new ArrayList<Pair<String, String>>();
        headers.add(Pair.of(Constants.HEADER_NAME_TIMESTAMP, timestamp));
        headers.add(Pair.of(Constants.HEADER_NAME_SIGNATURE_ALGORITHM, signatureAlgorithm));
        headers.add(Pair.of(Constants.HEADER_NAME_SIGNATURE, signature));

        SessionResponse resp = (SessionResponse) rawRequest(RequestMethod.POST, "/sessions/", headers, requestPayload);
        _sessionID = resp.getSession();
        _timeout = resp.getTimeout();
        _localIP = resp.getAddress();
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: new session allocated", _sessionID);
            System.out.printf("<Chain-DEBUG> [%s]: session timeout in %d second(s)", _sessionID, _timeout);
            System.out.printf("<Chain-DEBUG> [%s]: local address", _sessionID, _localIP);
        }
    }

    public void activate() throws Exception {
        final String url = mapToAPI("/sessions/");
        doRequest(RequestMethod.PUT, url);
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: keep alive", _sessionID);
        }
    }

    //private methods below
    private PrivateKey generatePrivateKey(byte[] privateKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(DefaultAlgorithmName);
        return keyFactory.generatePrivate(keySpec);
    }

    private String newNonce() {
        final int nonceLength = 16;
        boolean useLetters = true;
        boolean useNumbers = true;
        return RandomStringUtils.random(nonceLength, useLetters, useNumbers);
    }

    private String base64Signature(Object obj) throws SignatureException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeyException {
        Gson gson = new GsonBuilder().create();
        String marshalled = gson.toJson(obj);
        byte[] contentBytes = marshalled.getBytes("UTF-8");
        Signature signature = Signature.getInstance(DefaultAlgorithmName);
        signature.initSign(_privateKey);
        signature.update(contentBytes);
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }

    private Object rawRequest(RequestMethod method, String path, List<Pair<String, String>> headers, Object payload) throws Exception {
        final String url = mapToAPI(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(new URI(url));
        builder.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        String body;
        if (null != payload) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            body = gson.toJson(payload);
        } else {
            body = "";
        }
        builder.method(method.toString(), HttpRequest.BodyPublishers.ofString(body));
        for (Pair<String, String> header : headers) {
            builder.header(header.getKey(), header.getValue());
        }
        builder.headers("Pragma", "no-cache", "Cache-Control", "no-cache");
        HttpRequest request = builder.build();
        return getResult(request);
    }

    private void doRequest(RequestMethod method, String url) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        validateResult(request);
    }

    private void doRequestWithPayload(RequestMethod method, String url, Object payload) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
    }

    private Object fetchResponse(RequestMethod method, String url) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        return getResult(request);
    }

    private Object fetchResponseWithPayload(RequestMethod method, String url, Object payload) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
        return getResult(request);
    }

    private void validateResult(HttpRequest request) throws Exception {
        parseResponse(request);
    }

    private Object getResult(HttpRequest request) throws Exception {
        return parseResponse(request).data;
    }

    private responseFormat parseResponse(HttpRequest request) throws Exception {
        HttpResponse<String> resp = fetch(request);
        if (200 != resp.statusCode()) {
            throw new Exception(String.format("fetch result failed with status %d", resp.statusCode()));
        }
        Gson gson = new GsonBuilder().create();
        responseFormat respPayload = gson.fromJson(resp.body(), responseFormat.class);
        if (0 != respPayload.error_code) {
            throw new Exception(String.format("fetch failed: %s", respPayload.message));
        }
        return respPayload;
    }

    private Boolean peekRequest(RequestMethod method, String url) throws IOException,
            NoSuchAlgorithmException, URISyntaxException, InterruptedException, SignatureException, InvalidKeyException {
        HttpRequest request = prepareRequest(method, url, null);
        HttpResponse<String> resp = fetch(request);
        return 200 == resp.statusCode();
    }

    private HttpResponse<String> fetch(HttpRequest request) throws IOException, InterruptedException {
        return _client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest prepareRequest(RequestMethod method, String url, Object payload) throws MalformedURLException,
            NoSuchAlgorithmException, URISyntaxException, UnsupportedEncodingException, SignatureException, InvalidKeyException {
        URL urlObject = new URL(url);
        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(now);
        class signatureFormat {
            private String id;
            private String method;
            private String url;
            private String body;
            private String access;
            private String timestamp;
            private String nonce;
            private String signature_algorithm;
        }
        signatureFormat signaturePayload = new signatureFormat();
        signaturePayload.id = _sessionID;
        signaturePayload.method = method.toString().toUpperCase();
        signaturePayload.url = urlObject.getPath();
        signaturePayload.body = "";
        signaturePayload.access = _accessID;
        signaturePayload.timestamp = timestamp;
        signaturePayload.nonce = _nonce;
        signaturePayload.signature_algorithm = Constants.SIGNATURE_METHOD_ED25519;

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URI(url));
        builder.timeout(Duration.ofMillis(this._requestTimeout));
        String bodyPayload;
        if (null != payload) {
            //has payload
            builder.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
            Gson gson = new GsonBuilder().create();
            bodyPayload = gson.toJson(payload);
            builder.method(signaturePayload.method, HttpRequest.BodyPublishers.ofString(bodyPayload));
        } else {
            bodyPayload = "";
            builder.method(signaturePayload.method, HttpRequest.BodyPublishers.noBody());
        }

        if (method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.DELETE || method == RequestMethod.PATCH) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bodyPayload.getBytes(StandardCharsets.UTF_8));
            signaturePayload.body = Base64.getEncoder().encodeToString(hash);
        }
        String signature = base64Signature(signaturePayload);
        builder.headers("Pragma", "no-cache", "Cache-Control", "no-cache", Constants.HEADER_NAME_SESSION, _sessionID, Constants.HEADER_NAME_TIMESTAMP, timestamp, Constants.HEADER_NAME_SIGNATURE_ALGORITHM, Constants.SIGNATURE_METHOD_ED25519, Constants.HEADER_NAME_SIGNATURE, signature);
        if (this._trace) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.printf("<Chain-DEBUG> [%s]: signature payload\n%s", _sessionID, gson.toJson(signaturePayload));
            System.out.printf("<Chain-DEBUG> [%s]: signature: %s", _sessionID, signature);
        }
        return builder.build();
    }

    private String mapToAPI(String path) {
        return _apiBase + path;
    }

    private String mapToDomain(String path) {
        return _apiBase + "/domains/" + _domain + path;
    }

    private static boolean isHex(String str) {
        try {
            Long.parseLong(str, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static byte[] hexToBin(String hex) {
        return hex.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] slice(byte[] array, int start, int end) {
        return Arrays.copyOfRange(array, start, end);
    }
};

