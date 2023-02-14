package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChainConnector {
    private static class requestFormat {
        private String id;
        private String nonce;

        public requestFormat() {

        }
    }
    private interface response{
        int getErrorCode();
        String getErrorMessage();
    }
    private static class responseWithoutPayload implements response {
        private int error_code;
        private String message;
        public int getErrorCode(){
            return error_code;
        }
        public String getErrorMessage(){
            return message;
        }
    }

    private static class responseWithPayload<T> implements response {
        private int error_code;
        private String message;
        private T data;
        public int getErrorCode(){
            return error_code;
        }
        public String getErrorMessage(){
            return message;
        }
        public T getData(){
            return data;
        }
    }

    private static class initialSignatureFormat {
        private String access;
        private String timestamp;
        private String nonce;
        private String signature_algorithm;
    }
    private static class requestSignatureFormat {
        private String id;
        private String method;
        private String url;
        private String body;
        private String access;
        private String timestamp;
        private String nonce;
        private String signature_algorithm;
    }

    final private static int requiredPrivateKeyLength = 32;
    private final String _accessID;
    private final PrivateKey _privateKey;
    private String _apiBase;
    private String _domain;
    private String _nonce;
    private String _sessionID;
    private int _timeout;
    private String _localIP;
    private boolean _trace = false;
    private int _requestTimeout = Constants.DEFAULT_TIMEOUT_IN_SECONDS * 1000;

    private final HttpClient _client;

    public static ChainConnector NewConnectorFromAccess(AccessKey key) throws Exception {
        String id = key.getPrivateData().getId();
        String encodeMethod = key.getPrivateData().getEncodeMethod();
        String privateKey = key.getPrivateData().getPrivateKey();
        if (Constants.DEFAULT_KEY_ENCODE_METHOD.equals(encodeMethod)) {
            if (!isHex(privateKey)) {
                throw new Exception("invalid key format");
            }
            byte[] decoded = Hex.decodeHex(privateKey);
            if (decoded.length < requiredPrivateKeyLength){
                throw new Exception("insufficient private key length");
            }
            return NewConnector(id, Arrays.copyOfRange(decoded, 0, requiredPrivateKeyLength));
        } else {
            throw new Exception("unsupported encode method: " + encodeMethod);
        }
    }

    public static ChainConnector NewConnector(String accessID, byte[] privateKey){
        return new ChainConnector(accessID, privateKey);
    }

    public ChainConnector(String accessID, byte[] privateKey) {
        this._accessID = accessID;
        this._apiBase = "";
        this._domain = "";
        this._nonce = "";
        this._sessionID = "";
        this._timeout = 0;
        this._localIP = "";
        this._client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        _privateKey = generatePrivateKey(privateKey);
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

        initialSignatureFormat signaturePayload = new initialSignatureFormat();
        signaturePayload.access = _accessID;
        signaturePayload.timestamp = timestamp;
        signaturePayload.nonce = _nonce;
        signaturePayload.signature_algorithm = signatureAlgorithm;
        //generate signature
        String signature = base64Signature(signaturePayload);

        //generate request payload
        requestFormat requestPayload = new requestFormat();
        requestPayload.id = _accessID;
        requestPayload.nonce = _nonce;

        ArrayList<Pair<String, String>> headers = new ArrayList<>();
        headers.add(Pair.of(Constants.HEADER_NAME_TIMESTAMP, timestamp));
        headers.add(Pair.of(Constants.HEADER_NAME_SIGNATURE_ALGORITHM, signatureAlgorithm));
        headers.add(Pair.of(Constants.HEADER_NAME_SIGNATURE, signature));
        SessionData resp = rawRequest(RequestMethod.POST, "/sessions/", headers,
                requestPayload);
        _sessionID = resp.getSession();
        _timeout = resp.getTimeout();
        _localIP = resp.getAddress();
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: new session allocated", _sessionID);
            System.out.printf("<Chain-DEBUG> [%s]: session timeout in %d second(s)", _sessionID, _timeout);
            System.out.printf("<Chain-DEBUG> [%s]: local address %s", _sessionID, _localIP);
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
    private PrivateKey generatePrivateKey(byte[] privateKeyBytes) {
        EdDSANamedCurveSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        return new EdDSAPrivateKey(new EdDSAPrivateKeySpec(privateKeyBytes, spec));
    }

    private String newNonce() {
        final int nonceLength = 16;
        boolean useLetters = true;
        boolean useNumbers = true;
        return RandomStringUtils.random(nonceLength, useLetters, useNumbers);
    }

    private String base64Signature(Object obj) throws SignatureException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        Gson gson = new GsonBuilder().create();
        String marshalled = gson.toJson(obj);
        if (_trace) {
            System.out.printf("try signature payload:\n%s\n", marshalled);
        }
        byte[] contentBytes = marshalled.getBytes(StandardCharsets.UTF_8);
        EdDSAEngine engine = new EdDSAEngine();
        engine.initSign(_privateKey);
        engine.setParameter(EdDSAEngine.ONE_SHOT_MODE);
        engine.update(contentBytes);
        byte[] signed = engine.sign();
        return Base64.getEncoder().encodeToString(signed);
    }

    private <T> T  rawRequest(RequestMethod method, String path, List<Pair<String, String>> headers,
                              Object payload) throws Exception {
        final String url = mapToAPI(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(new URI(url));
        builder.timeout(Duration.ofMillis(_requestTimeout));
        builder.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        String body;
        if (null != payload) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            body = gson.toJson(payload);
        } else {
            body = "";
        }
        if (_trace) {
            System.out.printf("body:\n%s\n", body);
        }
        builder.method(method.toString(), HttpRequest.BodyPublishers.ofString(body));
        for (Pair<String, String> header : headers) {
            builder.header(header.getKey(), header.getValue());
        }
        builder.headers("Pragma", "no-cache", "Cache-Control", "no-cache");
        HttpRequest request = builder.build();
        T data = getResult(request);
        return data;
    }

    private void doRequest(RequestMethod method, String url) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        validateResult(request);
    }

    private void doRequestWithPayload(RequestMethod method, String url, Object payload) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
    }

    private <T> T fetchResponse(RequestMethod method, String url) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        return getResult(request);
    }

    private <T> T fetchResponseWithPayload(RequestMethod method, String url, Object payload) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
        return getResult(request);
    }

    private void validateResult(HttpRequest request) throws Exception {
        parseResponse(request);
    }

    private <T> T getResult(HttpRequest request) throws Exception {
        responseWithPayload<T> resp = parseResponse(request);
        return resp.getData();
    }

    private <T extends response> T parseResponse(HttpRequest request) throws Exception {
        HttpResponse<String> resp = fetch(request);
        if (200 != resp.statusCode()) {
            throw new Exception(String.format("fetch result failed with status %d", resp.statusCode()));
        }
        Gson gson = new Gson();
        Type t = new TypeToken<T>(){}.getType();
        T respPayload = (T) gson.fromJson(resp.body(), t.getClass());
        if (0 != respPayload.getErrorCode()) {
            throw new Exception(String.format("fetch failed: %s", respPayload.getErrorMessage()));
        }
        return respPayload;
    }

    private Boolean peekRequest(RequestMethod method, String url) throws IOException,
            NoSuchAlgorithmException, URISyntaxException, InterruptedException, SignatureException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        HttpRequest request = prepareRequest(method, url, null);
        HttpResponse<String> resp = fetch(request);
        return 200 == resp.statusCode();
    }

    private HttpResponse<String> fetch(HttpRequest request) throws IOException, InterruptedException {
        return _client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest prepareRequest(RequestMethod method, String url, Object payload) throws MalformedURLException,
            NoSuchAlgorithmException, URISyntaxException, UnsupportedEncodingException, SignatureException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        URL urlObject = new URL(url);
        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(now);
        requestSignatureFormat signaturePayload = new requestSignatureFormat();
        signaturePayload.id = _sessionID;
        signaturePayload.method = method.toString().toUpperCase();
        signaturePayload.url = urlObject.getPath();
        signaturePayload.body = "";
        signaturePayload.access = _accessID;
        signaturePayload.timestamp = timestamp;
        signaturePayload.nonce = _nonce;
        signaturePayload.signature_algorithm = Constants.SIGNATURE_METHOD_ED25519;

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URI(url));
        builder.timeout(Duration.ofMillis(_requestTimeout));
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
        final String HEX_PATTERN = "^[0-9a-fA-F]+$";
        if (0 != (str.length() % 2)) {
            return false;
        }
        Pattern pattern = Pattern.compile(HEX_PATTERN);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

}

