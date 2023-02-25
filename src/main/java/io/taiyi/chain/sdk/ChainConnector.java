package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;
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

    private static class statusResponse extends responseBase {
        private ChainStatus data;

        ChainStatus getData() {
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

    private static class contractInfoResponse extends responseBase {
        private ContractInfo data;

        public ContractInfo getData() {
            return data;
        }
    }

    private static class contractData {
        private String name;
        private String content;

        public String getName() {
            return name;
        }

        public String getContent() {
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
        private List<ActorPrivileges> data;

        public List<ActorPrivileges> getData() {
            return data;
        }
    }

    private static class blockRecordsResponse extends responseBase {
        private BlockRecords data;

        public BlockRecords getData() {
            return data;
        }
    }

    private static class transactionRecordsResponse extends responseBase {
        private TransactionRecords data;

        public TransactionRecords getData() {
            return data;
        }
    }

    private static class schemaRecordsResponse extends responseBase {
        private SchemaRecords data;

        public SchemaRecords getData() {
            return data;
        }
    }

    private static class documentRecordsResponse extends responseBase {
        private DocumentRecords data;

        public DocumentRecords getData() {
            return data;
        }
    }

    private static class contractRecordsResponse extends responseBase {
        private ContractRecords data;

        public ContractRecords getData() {
            return data;
        }
    }
    private static class newDocumentData {
        private String id;

        public String getId() {
            return id;
        }
    }
    private static class documentCreatedResponse extends responseBase {
        private newDocumentData data;

        public newDocumentData getData() {
            return data;
        }
    }

    //request defines
    private static class actorsRequest {
        private List<ActorPrivileges> actors;

        actorsRequest(List<ActorPrivileges> actors) {
            this.actors = actors;
        }
    }

    private static class paginationRequest {
        private int offset;
        private int limit;

        public paginationRequest(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }
    }

    private static class contentRequest {
        private String content;

        public contentRequest(String input) {
            content = input;
        }
    }

    private static class documentRequest {
        private String id;
        private String content;

        public documentRequest(String docID, String docContent) {
            id = docID;
            content = docContent;
        }

    }

    private static class flagRequest {
        private boolean enable;

        public flagRequest(boolean input) {
            enable = input;
        }
    }

    private static class parametersRequest {
        private List<String> parameters;

        public parametersRequest(List<String> values) {
            parameters = values;
        }
    }

    private static class propertyRequest {
        private String type;
        private Object value;

        public propertyRequest(PropertyType valueType, Object propertyValue) {
            type = valueType.toString();
            value = propertyValue;
        }
    }
    private static class blockQueryRequest {
        int from;
        int to;

        public blockQueryRequest(int from, int to) {
            this.from = from;
            this.to = to;
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

    final private String SDK_VERSION = "0.2.1";
    private String headerNameSession;
    private String headerNameTimestamp;
    private String headerNameSignature;
    private String headerNameSignatureAlgorithm;
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

    private final Gson compactJSONMarshaller = new GsonBuilder().disableHtmlEscaping().setLenient().create();

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
        setProject(Constants.DEFAULT_PROJECT_NAME);
    }

    public String getVersion() {
        return SDK_VERSION;
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

    public void setProject(String projectName) {
        headerNameSession = projectName + "-Session";
        headerNameTimestamp = projectName + "-Timestamp";
        headerNameSignature = projectName + "-Signature";
        headerNameSignatureAlgorithm = projectName + "-SignatureAlgorithm";
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
        headers.add(Pair.of(headerNameTimestamp, timestamp));
        headers.add(Pair.of(headerNameSignatureAlgorithm, signatureAlgorithm));
        headers.add(Pair.of(headerNameSignature, signature));
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

    public ChainStatus getStatus() throws Exception {
        final String url = mapToDomain("/status");
        statusResponse resp = fetchResponse(RequestMethod.GET, url, statusResponse.class);
        return resp.getData();
    }

    /**
     * Query blocks with pagination
     *
     * @param beginHeight begin block height, start from 1
     * @param endHeight   end block height, start from 1
     * @return list of block records
     */
    public BlockRecords queryBlocks(int beginHeight, int endHeight) throws Exception {
        if (endHeight < beginHeight) {
            throw new Exception("end height " + endHeight + " must greater than begin height " + beginHeight);
        }
        String url = this.mapToDomain("/blocks/");
        blockQueryRequest condition = new blockQueryRequest(beginHeight, endHeight);
        blockRecordsResponse resp = fetchResponseWithPayload(RequestMethod.POST, url, condition, blockRecordsResponse.class);
        return resp.getData();
    }

    public BlockData getBlock(String blockID) throws Exception {
        if (blockID == null || blockID.isEmpty()) {
            throw new Exception("block ID required");
        }

        final String url = mapToDomain("/blocks/" + blockID);
        blockDataResponse resp = fetchResponse(RequestMethod.GET, url, blockDataResponse.class);
        return resp.getData();
    }

    /**
     * Query transactions using pagination
     *
     * @param blockID   block ID
     * @param start     start offset for querying, start from 0
     * @param maxRecord max records returned
     * @return transaction records
     * @throws Exception on error
     */
    public TransactionRecords queryTransactions(String blockID, int start, int maxRecord) throws Exception {
        if (blockID == null || blockID.isEmpty()) {
            throw new IllegalArgumentException("Block ID is required");
        }
        String url = mapToDomain("/blocks/" + blockID + "/transactions/");
        paginationRequest condition = new paginationRequest(start, maxRecord);
        transactionRecordsResponse resp = fetchResponseWithPayload(RequestMethod.POST, url, condition, transactionRecordsResponse.class);
        return resp.getData();
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

    public SchemaRecords querySchemas(int queryStart, int maxRecord) throws Exception {
        final String url = mapToDomain("/schemas/");
        final paginationRequest condition = new paginationRequest(queryStart, maxRecord);
        final schemaRecordsResponse response = fetchResponseWithPayload(RequestMethod.POST, url, condition, schemaRecordsResponse.class);
        return response.getData();
    }

    /**
     * Rebuild index of a schema
     *
     * @param schemaName schema for rebuilding
     * @throws Exception if schemaName is null or empty
     */
    public void rebuildIndex(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName + "/index/");
        doRequest(RequestMethod.POST, url);
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
     * Create a new schema
     *
     * @param schemaName Name of new schema
     * @param properties Properties of new schema
     * @throws Exception if schemaName is null or empty
     */
    public void createSchema(String schemaName, List<DocumentProperty> properties) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName);
        doRequestWithPayload(RequestMethod.POST, url, properties);
    }

    /**
     * Update an existing schema
     *
     * @param schemaName Name of schema to update
     * @param properties Properties of schema to update
     */
    public void updateSchema(String schemaName, List<DocumentProperty> properties) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Error("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName);
        doRequestWithPayload(RequestMethod.PUT, url, properties);
    }


    /**
     * Delete a schema
     *
     * @param schemaName name of target schema
     * @throws Exception if schemaName is null or empty
     */
    public void deleteSchema(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = mapToDomain("/schemas/" + schemaName);
        doRequest(RequestMethod.DELETE, url);
    }


    /**
     * Get trace log of a schema
     *
     * @param {string} schemaName schema name
     * @returns {LogRecords} list of log records
     */
    public LogRecords getSchemaLog(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/logs/");
        logRecordsResponse resp = fetchResponse(RequestMethod.GET, url, logRecordsResponse.class);
        return resp.getData();
    }

    /**
     * Get meta actors of a schema
     *
     * @param {string} schemaName schema name
     * @returns {ActorPrivileges[]} list of actor privileges
     */
    public List<ActorPrivileges> getSchemaActors(String schemaName) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/actors/");
        actorsResponse resp = fetchResponse(RequestMethod.GET, url, actorsResponse.class);
        return resp.getData();
    }

    /**
     * Update meta actors of a schema
     *
     * @param schemaName schema name
     * @param actors     list of actor privileges
     * @throws IllegalArgumentException if schemaName or actors is null or empty
     * @throws Exception                if request fails
     */
    public void updateSchemaActors(String schemaName, List<ActorPrivileges> actors) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema name is required");
        }
        if (actors == null || actors.isEmpty()) {
            throw new IllegalArgumentException("Actor privileges list is required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/actors/");
        final actorsRequest payload = new actorsRequest(actors);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    /**
     * Query documents using the query condition
     *
     * @param schemaName schema name
     * @param condition  query condition
     * @return document records
     */
    public DocumentRecords queryDocuments(String schemaName, QueryCondition condition) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("schema name required");
        }
        String url = mapToDomain("/queries/schemas/" + schemaName + "/docs/");
        final documentRecordsResponse resp = fetchResponseWithPayload(RequestMethod.POST, url, condition, documentRecordsResponse.class);
        return resp.getData();
    }

    public boolean hasDocument(String schemaName, String docID) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }
        String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID);
        return peekRequest(RequestMethod.HEAD, url);
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

    /**
     * Add a new document to schema
     *
     * @param schemaName schema name
     * @param docID      optional document ID, generate when omit
     * @param docContent document content in JSON format
     * @return document ID
     */
    public String addDocument(String schemaName, String docID, String docContent) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        String url = this.mapToDomain("/schemas/" + schemaName + "/docs/");
        documentRequest payload = new documentRequest(docID, docContent);
        documentCreatedResponse resp = this.fetchResponseWithPayload(RequestMethod.POST, url, payload, documentCreatedResponse.class);
        return resp.getData().getId();
    }

    /**
     * Update content of a document
     *
     * @param schemaName schema name
     * @param docID      document ID
     * @param docContent document content in JSON format
     * @throws IllegalArgumentException if schemaName or docID is null or empty
     */
    public void updateDocument(String schemaName, String docID, String docContent) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new IllegalArgumentException("document ID required");
        }
        String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID);
        contentRequest payload = new contentRequest(docContent);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    /**
     * Update property value of a document
     * @param schemaName schema name
     * @param docID document ID
     * @param propertyName property for updating
     * @param valueType value type of property
     * @param value value for property
     */
    public void updateDocumentProperty(String schemaName, String docID, String propertyName, PropertyType valueType,
                                       Object value) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }
        if (propertyName == null || propertyName.isEmpty()) {
            throw new Exception("property name required");
        }
        String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID + "/properties/" + propertyName);
        propertyRequest payload = new propertyRequest(valueType, value);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }


    /**
     * Remove a document
     *
     * @param schemaName schema name
     * @param docID      document ID
     * @throws Exception if any error occurs
     */
    public void removeDocument(String schemaName, String docID) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new Exception("schema name required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new Exception("document ID required");
        }
        String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID);
        doRequest(RequestMethod.DELETE, url);
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

    public List<ActorPrivileges> getDocumentActors(String schemaName, String docID) throws Exception {
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

    /**
     * Update meta actors of a document
     *
     * @param schemaName name of target schema
     * @param docID      ID of target document
     * @param actors     list of actor privileges
     */
    public void updateDocumentActors(String schemaName, String docID, List<ActorPrivileges> actors) throws Exception {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema name is required");
        }
        if (docID == null || docID.isEmpty()) {
            throw new IllegalArgumentException("Document ID is required");
        }
        if (actors == null || actors.isEmpty()) {
            throw new IllegalArgumentException("Actor privileges list is required");
        }
        final String url = mapToDomain("/schemas/" + schemaName + "/docs/" + docID + "/actors/");
        final actorsRequest payload = new actorsRequest(actors);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    public ContractRecords queryContracts(int queryStart, int maxRecord) throws Exception {
        final String url = mapToDomain("/contracts/");

        final paginationRequest condition = new paginationRequest(queryStart, maxRecord);
        final contractRecordsResponse response = fetchResponseWithPayload(RequestMethod.POST, url, condition, contractRecordsResponse.class);
        return response.getData();
    }

    /**
     * Check if a contract exists
     *
     * @param {String} contractName target contract name
     * @return {boolean} true: exists/false: not exists
     * @throws Exception if contract name is not provided
     */
    public boolean hasContract(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }
        String url = mapToDomain("/contracts/" + contractName);
        return peekRequest(RequestMethod.HEAD, url);
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
     * Deploy a contract define
     *
     * @param {String}         contractName contract name
     * @param {ContractDefine} define contract define
     */
    public void deployContract(String contractName, ContractDefine define) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new IllegalArgumentException("contract name required");
        }
        String url = mapToDomain("/contracts/" + contractName);
        contentRequest payload = new contentRequest(compactJSONMarshaller.toJson(define));
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    public void withdrawContract(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }
        String url = mapToDomain("/contracts/" + contractName);
        doRequest(RequestMethod.DELETE, url);
    }

    /**
     * Invoke a contract with parameters
     *
     * @param {string}       contractName contract name
     * @param {List<String>} parameters parameters for invoking contract
     */
    public void callContract(String contractName, List<String> parameters) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }
        String url = this.mapToDomain("/contracts/" + contractName + "/sessions/");
        parametersRequest payload = new parametersRequest(parameters);
        doRequestWithPayload(RequestMethod.POST, url, payload);
    }

    /**
     * Enable contract tracing
     *
     * @param contractName contract name
     */
    public void enableContractTrace(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new IllegalArgumentException("contract name required");
        }
        String url = mapToDomain("/contracts/" + contractName + "/trace/");
        flagRequest payload = new flagRequest(true);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    /**
     * Disable contract tracing
     *
     * @param contractName contract name
     */
    public void disableContractTrace(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new IllegalArgumentException("contract name required");
        }
        String url = mapToDomain("/contracts/" + contractName + "/trace/");
        flagRequest payload = new flagRequest(false);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
    }

    /**
     * Get detail info of a contract
     *
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

    public List<ActorPrivileges> getContractActors(String contractName) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new Exception("contract name required");
        }

        String url = mapToDomain("/contracts/" + contractName + "/actors/");
        actorsResponse resp = fetchResponse(RequestMethod.GET, url, actorsResponse.class);
        return resp.getData();
    }

    public void updateContractActors(String contractName, List<ActorPrivileges> actors) throws Exception {
        if (contractName == null || contractName.isEmpty()) {
            throw new IllegalArgumentException("Contract name is required");
        }
        if (actors == null || actors.isEmpty()) {
            throw new IllegalArgumentException("Actor privileges list is required");
        }
        final String url = mapToDomain("/contracts/" + contractName + "/actors/");
        final actorsRequest payload = new actorsRequest(actors);
        doRequestWithPayload(RequestMethod.PUT, url, payload);
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
        var responseContent = resp.body();
        //check
        int errorCode = JsonPath.read(responseContent, "$.error_code");
        if (0 != errorCode){
            String errorMessage = JsonPath.read(responseContent, "$.message");
            throw new Exception(String.format("fetch failed: %s", errorMessage));
        }
        return compactJSONMarshaller.fromJson(resp.body(), classofT);
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
            if (_trace){
                System.out.printf("<Chain-DEBUG> [%s]: request payload: \n%s\n", _sessionID, bodyPayload);
            }
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
                headerNameSession, _sessionID,
                headerNameTimestamp, timestamp,
                headerNameSignatureAlgorithm, Constants.SIGNATURE_METHOD_ED25519,
                headerNameSignature, signature);

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

