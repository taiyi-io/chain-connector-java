package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private interface responseStatus {
        int getErrorCode();

        String getErrorMessage();
    }

    private static class responseBase implements responseStatus {
        private int error_code;
        private String message;

        public int getErrorCode() {
            return error_code;
        }

        public String getErrorMessage() {
            return message;
        }
    }

    //    private static class responseStatusWithPayload<T> implements responseStatus {
//        private int error_code;
//        private String message;
//        private T data;
//        public int getErrorCode(){
//            return error_code;
//        }
//        public String getErrorMessage(){
//            return message;
//        }
//        public T getData(){
//            return data;
//        }
//    }
    private static class sessionResponse extends responseBase {
        private SessionData data;

        public SessionData getData() {
            return data;
        }
    }
    private static class statusResponse extends responseBase{
        private ChainStatus data;
        ChainStatus getData(){
            return data;
        }
    }
    private static class transactionDataResponse extends responseBase {
        private TransactionData data;

        public TransactionData getData() {
            return data;
        }
    }
    private static class blockDataResponse extends responseBase {
        private BlockData data;

        public BlockData getData() {
            return data;
        }
    }
    private static class SchemaDataResponse extends responseBase {
        private DocumentSchema data;

        DocumentSchema getData() {
            return data;
        }
    }

    private static class schemaLogResponse extends responseBase{
        private LogRecords data;
        LogRecords getData(){
            return data;
        }
    }
    private static class documentResponse extends responseBase {
        private String data;

        public String getData() {
            return data;
        }
    }
    private static class logRecordsResponse extends responseBase {
        private LogRecords data;

        public LogRecords getData() {
            return data;
        }
    }
    private static class contractInfoResponse extends responseBase{
        private ContractInfo data;
        public ContractInfo getData(){
            return data;
        }
    }
    private static class contractData{
        private String name;
        private String content;
        public String getName(){
            return name;
        }
        public String getContent(){
            return content;
        }
    }
    private static class contractResponse extends responseBase {
        private contractData data;
        public contractData getData() {
            return data;
        }
    }

    private static class actorsResponse extends responseBase {
        private ActorPrivileges[] data;

        public ActorPrivileges[] getData() {
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

    private final Gson compactJSONMarshaller = new GsonBuilder().disableHtmlEscaping().create();

    public static ChainConnector NewConnectorFromAccess(AccessKey key) throws Exception {
        String id = key.getPrivateData().getId();
        String encodeMethod = key.getPrivateData().getEncodeMethod();
        String privateKey = key.getPrivateData().getPrivateKey();
        if (Constants.DEFAULT_KEY_ENCODE_METHOD.equals(encodeMethod)) {
            if (!isHex(privateKey)) {
                throw new Exception("invalid key format");
            }
            byte[] decoded = Hex.decodeHex(privateKey);
            if (decoded.length < requiredPrivateKeyLength) {
                throw new Exception("insufficient private key length");
            }
            return NewConnector(id, Arrays.copyOfRange(decoded, 0, requiredPrivateKeyLength));
        } else {
            throw new Exception("unsupported encode method: " + encodeMethod);
        }
    }

    public static ChainConnector NewConnector(String accessID, byte[] privateKey) {
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
        sessionResponse resp = rawRequest(RequestMethod.POST, "/sessions/", headers,
                requestPayload, sessionResponse.class);

        _sessionID = resp.getData().getSession();
        _timeout = resp.getData().getTimeout();
        _localIP = resp.getData().getAddress();
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: new session allocated\n", _sessionID);
            System.out.printf("<Chain-DEBUG> [%s]: session timeout in %d second(s)\n", _sessionID, _timeout);
            System.out.printf("<Chain-DEBUG> [%s]: local address %s\n", _sessionID, _localIP);
        }
    }

    public void activate() throws Exception {
        final String url = mapToAPI("/sessions/");
        doRequest(RequestMethod.PUT, url);
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: keep alive\n", _sessionID);
        }
    }

    public ChainStatus getStatus() throws Exception{
        final String url = mapToDomain("/status");
        statusResponse resp = fetchResponse(RequestMethod.GET, url, statusResponse.class);
        return resp.getData();
    }

    public blockDataResponse getBlock(String blockID) throws Exception {
        if (blockID == null || blockID.isEmpty()) {
            throw new Exception("block ID required");
        }

        final String url = mapToDomain("/blocks/" + blockID);
        blockDataResponse resp = fetchResponse(RequestMethod.GET, url, blockDataResponse.class);
        return resp;
    }

    public TransactionData getTransaction(String blockID, String transID) throws Exception {
        if (blockID == null || blockID.isEmpty()) {
            throw new Exception("block ID required");
        }
        if (transID == null || transID.isEmpty()) {
            throw new Exception("transaction ID required");
        }
        String url = mapToDomain("/blocks/" + blockID + "/transactions/" + transID);
        transactionDataResponse resp = fetchResponse(RequestMethod.GET, url, transactionDataResponse.class);
        return resp.getData();
    }

    public boolean hasSchema(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName);
        return peekRequest(RequestMethod.HEAD, url);
    }

    public DocumentSchema getSchema(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName);
        SchemaDataResponse resp = fetchResponse(RequestMethod.GET, url, SchemaDataResponse.class);
        return resp.getData();
    }
    /**
     * Get trace log of a schema
     * @param {string} schemaName schema name
     * @returns {LogRecords} list of log records
     */
    public LogRecords getSchemaLog(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/logs/");
        schemaLogResponse resp = fetchResponse(RequestMethod.GET, url, schemaLogResponse.class);
        return resp.getData();
    }
    /**
     * Get meta actors of a schema
     * @param {string} schemaName schema name
     * @returns {ActorPrivileges[]} list of actor privileges
     */
    public ActorPrivileges[] getSchemaActors(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/actors/");
        actorsResponse resp = fetchResponse(RequestMethod.GET, url, actorsResponse.class);
        return resp.getData();
    }

    public String getDocument(String schemaName, String docID) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID);
        documentResponse resp = fetchResponse(RequestMethod.GET, url, documentResponse.class);
        return resp.getData();
    }

    public LogRecords getDocumentLogs(String schemaName, String docID) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }

        final String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID + "/logs/");
        logRecordsResponse resp = fetchResponse(RequestMethod.GET, url, logRecordsResponse.class);
        return resp.getData();
    }

    public ActorPrivileges[] getDocumentActors(String schemaName, String docID) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }

        final String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID + "/actors/");
        actorsResponse resp = fetchResponse(RequestMethod.GET, url, actorsResponse.class);
        return resp.getData();
    }

    public ContractDefine getContract(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }
        String url = this.mapToDomain("/contracts/" + contractName);
        contractResponse resp = this.fetchResponse(RequestMethod.GET, url, contractResponse.class);
        ContractDefine define = compactJSONMarshaller.fromJson(resp.getData().getContent(), ContractDefine.class);
        return define;
    }
    /**
     * Get detail info of a contract
     * @param contractName target contract name
     * @return ContractInfo contract info
     * @throws Exception if contract name is not provided or API call fails
     */
    public ContractInfo getContractInfo(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }
        String url = this.mapToDomain("/contracts/" + contractName + "/info/");
        contractInfoResponse resp = this.fetchResponse(RequestMethod.GET, url, contractInfoResponse.class);
        return resp.getData();
    }

    public ActorPrivileges[] getContractActors(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }

        String url = mapToDomain("/contracts/" + contractName + "/actors/");
        actorsResponse resp = fetchResponse(RequestMethod.GET, url, actorsResponse.class);
        return resp.getData();
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
        String payload = compactJSONMarshaller.toJson(obj);
        if (_trace) {
            System.out.printf("<Chain-DEBUG> [%s]: signature payload\n%s\n", _sessionID, compactJSONMarshaller.toJson(payload));
        }
        byte[] contentBytes = payload.getBytes(StandardCharsets.UTF_8);
        EdDSAEngine engine = new EdDSAEngine();
        engine.initSign(_privateKey);
        engine.setParameter(EdDSAEngine.ONE_SHOT_MODE);
        engine.update(contentBytes);
        byte[] signed = engine.sign();
        String signature = Base64.getEncoder().encodeToString(signed);
        if (this._trace) {
            System.out.printf("<Chain-DEBUG> [%s]: signature: \n%s\n", _sessionID, signature);
        }
        return signature;
    }

    private <T extends responseStatus> T rawRequest(RequestMethod method, String path, List<Pair<String, String>> headers,
                                                    Object payload, Class<T> classofT) throws Exception {
        final String url = mapToAPI(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(new URI(url));
        builder.timeout(Duration.ofMillis(_requestTimeout));
        builder.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        String body;
        if (null != payload) {
            body = compactJSONMarshaller.toJson(payload);
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
        return getResult(request, classofT);
    }

    private void doRequest(RequestMethod method, String url) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        validateResult(request);
    }

    private void doRequestWithPayload(RequestMethod method, String url, Object payload) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
    }

    private <T extends responseStatus> T fetchResponse(RequestMethod method, String url, Class<T> classofT) throws Exception {
        HttpRequest request = prepareRequest(method, url, null);
        return getResult(request, classofT);
    }

    private <T extends responseStatus> T fetchResponseWithPayload(RequestMethod method, String url, Object payload, Class<T> classofT) throws Exception {
        HttpRequest request = prepareRequest(method, url, payload);
        validateResult(request);
        return getResult(request, classofT);
    }

    private void validateResult(HttpRequest request) throws Exception {
        parseResponse(request, responseBase.class);
    }

    private <T extends responseStatus> T getResult(HttpRequest request, Class<T> classofT) throws Exception {
        return parseResponse(request, classofT);
    }

    private <T extends responseStatus> T parseResponse(HttpRequest request, Class<T> classofT) throws Exception {
        HttpResponse<String> resp = fetch(request);
        if (200 != resp.statusCode()) {
            throw new Exception(String.format("fetch result failed with status %d", resp.statusCode()));
        }
        T respPayload = compactJSONMarshaller.fromJson(resp.body(), classofT);
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

        SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String timestamp = RFC3339.format(now);
        requestSignatureFormat signaturePayload = new requestSignatureFormat();
        signaturePayload.id = _sessionID;
        signaturePayload.method = method.toString().toUpperCase();
        signaturePayload.url = urlObject.getPath();
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
            bodyPayload = compactJSONMarshaller.toJson(payload);
            builder.method(signaturePayload.method, HttpRequest.BodyPublishers.ofString(bodyPayload));
        } else {
            bodyPayload = "";
            builder.method(signaturePayload.method, HttpRequest.BodyPublishers.noBody());
        }

        if (method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.DELETE || method == RequestMethod.PATCH) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(bodyPayload.getBytes(StandardCharsets.UTF_8));
            byte[] rawInput = bodyPayload.getBytes();
            if (_trace) {
                System.out.printf("<Chain-DEBUG> [%s]: body payload\n%s\n", _sessionID, Hex.encodeHexString(rawInput));
            }
            byte[] hash = digest.digest(rawInput);
            if (_trace) {
                System.out.printf("<Chain-DEBUG> [%s]: body hash\n%s\n", _sessionID, Hex.encodeHexString(hash));
            }
            signaturePayload.body = new String(Base64.getEncoder().encode(hash), StandardCharsets.US_ASCII);
        } else {
            signaturePayload.body = "";
        }
        String signature = base64Signature(signaturePayload);
        builder.headers(
                "Pragma", "no-cache",
                "Cache-Control", "no-cache",
                Constants.HEADER_NAME_SESSION, _sessionID,
                Constants.HEADER_NAME_TIMESTAMP, timestamp,
                Constants.HEADER_NAME_SIGNATURE_ALGORITHM, Constants.SIGNATURE_METHOD_ED25519,
                Constants.HEADER_NAME_SIGNATURE, signature);

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

