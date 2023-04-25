package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class ChainConnectorTest {
    private static class TestConfig {
        String project;
        String host;
        int port;
        boolean debug;
    }
    final static String ACCESS_FILENAME = "access_key.json";
    final static String TEST_FILENAME = "test.json";
    final static Gson objectMarshaller = new GsonBuilder().setPrettyPrinting().create();

    private ChainConnector getConnector() throws Exception {
        String currentDirectory = System.getProperty("user.dir");
        String accessPath = currentDirectory + "/" + ACCESS_FILENAME;
        String testPath = currentDirectory + "/" + TEST_FILENAME;
        AccessKey accessKey;
        TestConfig config;
        {
            //load access
            byte[] fileBytes = Files.readAllBytes(Paths.get(accessPath));
            String fileContent = new String(fileBytes);
            accessKey = objectMarshaller.fromJson(fileContent, AccessKey.class);
        }
        {
            byte[] fileBytes = Files.readAllBytes(Paths.get(testPath));
            String fileContent = new String(fileBytes);
            config = objectMarshaller.fromJson(fileContent, TestConfig.class);
        }

        ChainConnector connector = ChainConnector.NewConnectorFromAccess(accessKey);
        if (!config.project.isEmpty()){
            connector.setProject(config.project);
        }
        if (config.debug){
            connector.setTrace(true);
        }
        if (config.debug){
            System.out.printf("connecting to gateway %s:%d...\n", config.host, config.port);
        }
        connector.connect(config.host, config.port);
        if (config.debug){
            System.out.printf("connected to gateway %s:%d\n", config.host, config.port);
        }
        return connector;
    }

    @Test
    void connectAndActivate() {
        try {
            System.out.println("test case: connect begin...");
            ChainConnector conn = getConnector();
            conn.activate();
            System.out.println("test case: connect passed");
        } catch (Exception e) {
            System.out.println(e);
            fail(e.getCause());
        }
    }

    @Test
    void getStatus() {
        try {
            System.out.println("test case: get status begin...");
            ChainConnector conn = getConnector();
            ChainStatus status = conn.getStatus();
            System.out.println(status);
            System.out.println("test case: get status passed");
        } catch (Exception e) {
            System.out.println(e);
            fail(e.toString());
        }
    }

    @Test
    void testSchemas() {
        try{

            ChainConnector conn = getConnector();
            SchemaRecords records = conn.querySchemas(0, 5);
            if (null != records.getSchemas()) {
                for (String schemaName : records.getSchemas()) {
                    System.out.printf("query returned schema %s\n", schemaName);
                }
            } else {
                System.out.println("no schema return by querying");
            }


            final String schemaName = "js-test-case1-schema";
            if (conn.hasSchema(schemaName)) {
                conn.deleteSchema(schemaName);
                System.out.printf("previous schema %s deleted\n", schemaName);
            } else {
                System.out.printf("previous schema %s not exists\n", schemaName);
            }
            {
                List<DocumentProperty> properties = new ArrayList<>();
                properties.add(new DocumentProperty("name", PropertyType.String));
                properties.add(new DocumentProperty("age", PropertyType.Integer));
                properties.add(new DocumentProperty("available", PropertyType.Boolean));
                conn.createSchema(schemaName, properties);
                DocumentSchema schema = conn.getSchema(schemaName);
                System.out.printf("schema created: %s\n", objectMarshaller.toJson(schema));
            }
            {
                List<DocumentProperty> properties = new ArrayList<>();
                properties.add(new DocumentProperty("name", PropertyType.String));
                properties.add(new DocumentProperty("age", PropertyType.Integer));
                properties.add(new DocumentProperty("amount", PropertyType.Currency));
                properties.add(new DocumentProperty("country", PropertyType.String));

                conn.updateSchema(schemaName, properties);
                DocumentSchema schema = conn.getSchema(schemaName);
                System.out.printf("schema updated: %s\n", objectMarshaller.toJson(schema));
            }
            {
                LogRecords logs = conn.getSchemaLog(schemaName);
                for (TraceLog log : logs.getLogs()) {
                    System.out.printf("Log v%d generated at %s by %s when %s, confirmed: %b\n", log.getVersion(),
                            log.getTimestamp(), log.getInvoker(), log.getOperate(), log.isConfirmed());
                }
            }
            {
                conn.deleteSchema(schemaName);
                System.out.printf("schema %s deleted\n", schemaName);
            }
            System.out.println("test schemas pass");
        }catch (Exception e){
            System.out.println(e);
            fail(e.toString());
        }

    }

    final static class testDocumentsPayload {
        private int age;
        private boolean enabled;

        public testDocumentsPayload(int v1, boolean v2) {
            age = v1;
            enabled = v2;
        }
    }

    @Test
    void testDocuments() {
        try{
            int docCount = 10;
            String propertyNameAge = "age";
            String propertyNameEnabled = "enabled";
            String schemaName = "js-test-case2-document";
            String docPrefix = "js-test-case2-";
            ChainConnector conn = getConnector();
            System.out.println("document test begin...");
            {
                if (conn.hasSchema(schemaName)) {
                    conn.deleteSchema(schemaName);
                    System.out.println("previous schema " + schemaName + " deleted");
                }
            }

            List<DocumentProperty> properties = new ArrayList<>();
            conn.createSchema(schemaName, properties);

            List<String> docList = new ArrayList<>();
            String content = "{}";
            for (int i = 0; i < docCount; i++) {
                String docID = docPrefix + (i + 1);
                if (conn.hasDocument(schemaName, docID)) {
                    conn.removeDocument(schemaName, docID);
                    System.out.printf("previous doc %s.%s removed", schemaName, docID);
                }
                String respID = conn.addDocument(schemaName, docID, content);
                System.out.println("doc " + respID + " added");
                docList.add(respID);
            }
            properties.clear();
            properties.add(new DocumentProperty(propertyNameAge, PropertyType.Integer));
            properties.add(new DocumentProperty(propertyNameEnabled, PropertyType.Boolean));
            conn.updateSchema(schemaName, properties);

            System.out.println("schema updated");
            for (var i = 0; i < docCount; i++) {
                String docID = docPrefix + (i + 1);
                if (0 == i % 2) {
                    content = objectMarshaller.toJson(new testDocumentsPayload(0, false));
                } else {
                    content = objectMarshaller.toJson(new testDocumentsPayload(0, true));
                }
                conn.updateDocument(schemaName, docID, content);
                conn.getDocument(schemaName, docID);
                System.out.println("doc " + docID + " updated");
            }
            for (var i = 0; i < docCount; i++) {
                String docID = docPrefix + (i + 1);
                conn.updateDocumentProperty(schemaName, docID, propertyNameAge, PropertyType.Integer, i);
                System.out.println("property age of doc " + docID + " updated");
                {
                    LogRecords logs = conn.getDocumentLogs(schemaName, docID);
                    for (TraceLog log : logs.getLogs()) {
                        System.out.printf("Log v%d generated at %s by %s when %s, confirmed: %b\n", log.getVersion(),
                                log.getTimestamp(), log.getInvoker(), log.getOperate(), log.isConfirmed());
                    }
                }
            }

            // test query builder
            {
                //ascend query
                int l = 5;
                int o = 3;
                QueryCondition condition = new QueryBuilder()
                        .AscendBy(propertyNameAge)
                        .MaxRecord(l)
                        .SetOffset(o)
                        .Build();
                int[] expected = {3, 4, 5, 6, 7};
                verifyQuery("ascend query", condition, docCount, expected, conn, schemaName);
            }

            {
                //descend query with filter
                int l = 3;
                int total = 4;
                QueryCondition condition = new QueryBuilder()
                        .DescendBy(propertyNameAge)
                        .MaxRecord(l)
                        .PropertyEqual("enabled", "true")
                        .PropertyLessThan(propertyNameAge, Integer.valueOf(8))
                        .Build();
                int[] expected = {7, 5, 3};
                verifyQuery("descend filter", condition, total, expected, conn, schemaName);
            }

            for (String docID : docList) {
                conn.removeDocument(schemaName, docID);
                System.out.println("doc " + docID + " removed");
            }

            conn.deleteSchema(schemaName);
            System.out.println("test schema " + schemaName + " deleted");
            System.out.println("document interfaces tested");
        }catch (Exception e){
            System.out.println(e);
            fail(e.toString());
        }

    }


    static void verifyQuery(String caseName, QueryCondition condition, int totalCount, int[] expectResult,
                            ChainConnector conn, String schemaName) throws Exception {
        DocumentRecords records = conn.queryDocuments(schemaName, condition);
        if (totalCount != records.getTotal()) {
            throw new Exception("unexpect count " + records.getTotal() + " => " + totalCount + " in case " + caseName);
        }
        int recordCount = records.getDocuments().size();
        if (expectResult.length != recordCount) {
            throw new Exception("unexpect result count " + recordCount + " => " + expectResult.length + " in case " + caseName);
        }

        System.out.println(recordCount + " / " + totalCount + " documents returned");
        for (int i = 0; i < recordCount; i++) {
            int expectValue = expectResult[i];
            Document doc = records.getDocuments().get(i);
            testDocumentsPayload contentPayload = objectMarshaller.fromJson(doc.getContent(), testDocumentsPayload.class);
            if (expectValue != contentPayload.age) {
                throw new Exception("unexpect value " + contentPayload.enabled + " => " + expectValue + " at doc " + doc.getId());
            }
            System.out.println(caseName + ": content of doc " + doc.getId() + " verified");
        }
        System.out.println(caseName + " test ok");
    }

    @Test
    void testContracts() {
        try{
            String propertyCatalog = "catalog";
            String propertyBalance = "balance";
            String propertyNumber = "number";
            String propertyAvailable = "available";
            String propertyWeight = "weight";
            String schemaName = "js-test-case3-contract";

            List<DocumentProperty> properties = new ArrayList<>();
            properties.add(new DocumentProperty(propertyCatalog, PropertyType.String, true));
            properties.add(new DocumentProperty(propertyBalance, PropertyType.Currency, true));
            properties.add(new DocumentProperty(propertyNumber, PropertyType.Integer, true));
            properties.add(new DocumentProperty(propertyAvailable, PropertyType.Boolean));
            properties.add(new DocumentProperty(propertyWeight, PropertyType.Float, true));
            ChainConnector conn = getConnector();
            if (conn.hasSchema(schemaName)) {
                conn.deleteSchema(schemaName);
                System.out.println("previous schema " + schemaName + " deleted");
            }
            conn.createSchema(schemaName, properties);
            String varName = "$s";
            final String createContractName = "contract_create";
            {
                List<ContractStep> steps = new ArrayList<>();
                steps.add(new ContractStep("create_doc", new String[]{varName, "@1", "@2"}));
                steps.add(new ContractStep("set_property", new String[]{varName, propertyCatalog, "@3"}));
                steps.add(new ContractStep("set_property", new String[]{varName, propertyBalance, "@4"}));
                steps.add(new ContractStep("set_property", new String[]{varName, propertyNumber, "@5"}));
                steps.add(new ContractStep("set_property", new String[]{varName, propertyAvailable, "@6"}));
                steps.add(new ContractStep("set_property", new String[]{varName, propertyWeight, "@7"}));
                steps.add(new ContractStep("update_doc", new String[]{"@1", varName}));
                steps.add(new ContractStep("submit"));
                ContractDefine contractDefine = new ContractDefine(steps);
                if (conn.hasContract(createContractName)) {
                    conn.withdrawContract(createContractName);
                    System.out.printf("previous contract %s removed\n", createContractName);
                }
                conn.deployContract(createContractName, contractDefine);
                System.out.printf("contract %s deployed\n", createContractName);
                ContractDefine define = conn.getContract(createContractName);
                System.out.printf("create contract define:\n%s\n", objectMarshaller.toJson(define));
            }

            final String deleteContractName = "contract_delete";
            {
                List<ContractStep> steps = new ArrayList<>();
                steps.add(new ContractStep("delete_doc", new String[]{"@1", "@2"}));
                steps.add(new ContractStep("submit"));
                ContractDefine contractDefine = new ContractDefine(steps);
                if (conn.hasContract(deleteContractName)) {
                    conn.withdrawContract(deleteContractName);
                    System.out.printf("previous contract %s removed\n", deleteContractName);
                }
                conn.deployContract(deleteContractName, contractDefine);
                System.out.printf("contract %s deployed\n", deleteContractName);
                ContractDefine define = conn.getContract(deleteContractName);
                System.out.printf("delete contract define:\n%s\n", objectMarshaller.toJson(define));
            }
            final String docID = "contract-doc";
            String[] parameters = {
                    schemaName,
                    docID,
                    schemaName,
                    String.valueOf(Math.random()),
                    String.valueOf((int) (Math.random() * 1000)),
                    Math.random() > 0.5 ? "true" : "false",
                    String.format("%.2f", Math.random() * 200)
            };
            ContractInfo info = conn.getContractInfo(createContractName);
            if (!info.isEnabled()) {
                conn.enableContractTrace(createContractName);
                System.out.printf("trace of contract %s enabled\n", createContractName);
            }
            conn.callContract(createContractName, new ArrayList<>(Arrays.asList(parameters)));
            conn.callContract(deleteContractName, new ArrayList<>(Arrays.asList(schemaName, docID)));
            if (!info.isEnabled()) {
                conn.disableContractTrace(createContractName);
                System.out.printf("trace of contract %s disabled\n", createContractName);
            }
            conn.withdrawContract(createContractName);
            conn.withdrawContract(deleteContractName);
            conn.deleteSchema(schemaName);
            System.out.printf("schema %s deleted\n", schemaName);
            System.out.println("test contract functions: ok");
        }catch (Exception e){
            System.out.println(e);
            fail(e.toString());
        }

    }

    @Test
    void testChain() {
        try{
            ChainConnector conn = getConnector();
            System.out.println("chain test begin...");
            ChainStatus status = conn.getStatus();
            System.out.println("world version " + status.getWorldVersion() + ", block height " + status.getBlockHeight());
            System.out.println("genesis block: " + status.getGenesisBlock() + ", previous block: " + status.getPreviousBlock());
            int maxRecord = 5;
            int lowestHeight = 1;
            int endHeight = status.getBlockHeight();
            int beginHeight;
            if (endHeight <= maxRecord) {
                beginHeight = lowestHeight;
            } else {
                beginHeight = endHeight - maxRecord;
            }
            BlockRecords blockRecords = conn.queryBlocks(beginHeight, endHeight);
            System.out.println("block range from " + blockRecords.getFrom() + " to " + blockRecords.getTo() + " at height " + blockRecords.getHeight() + " returned");
            for (String blockID : blockRecords.getBlocks()) {
                BlockData blockData = conn.getBlock(blockID);
                System.out.println("block " + blockID + " created at " + blockData.getTimestamp() + " on height " + blockData.getHeight());
                System.out.println("previous block: => " + blockData.getPreviousBlock());
                System.out.println("included transactions: " + blockData.getTransactions());
                TransactionRecords transactionRecords = conn.queryTransactions(blockID, 0, maxRecord);
                System.out.println(transactionRecords.getTransactions().length + " / " + transactionRecords.getTotal() + " transactions returned");
                for (String transID : transactionRecords.getTransactions()) {
                    TransactionData transactionData = conn.getTransaction(blockID, transID);
                    if (transactionData.isValidated()) {
                        System.out.println("transaction " + transactionData.getTransaction() + " created at " + transactionData.getTimestamp() + " committed");
                    } else {
                        System.out.println("transaction " + transactionData.getTransaction() + " created at " + transactionData.getTimestamp() + " not commit");
                    }
                }
            }

            System.out.println("chain interfaces tested");
        }catch (Exception e){
            System.out.println(e);
            fail(e.toString());
        }

    }

    @Test
    void testActors() {
        try{
            String schemaName = "js-test-case5-actors";
            ChainConnector conn = getConnector();
            System.out.println("actor test begin...");
            {
                if (conn.hasSchema(schemaName)) {
                    conn.deleteSchema(schemaName);
                    System.out.println("previous schema " + schemaName + " deleted");
                }
                DocumentProperty[] properties = {
                        new DocumentProperty("name", PropertyType.String),
                        new DocumentProperty("age", PropertyType.Integer),
                        new DocumentProperty("available", PropertyType.Boolean)
                };
                conn.createSchema(schemaName, Arrays.asList(properties));
                DocumentSchema schema = conn.getSchema(schemaName);
                System.out.printf("schema %s created:\n%s\n", schemaName, objectMarshaller.toJson(schema));
            }
            List<ActorPrivileges> actors = conn.getSchemaActors(schemaName);
            System.out.printf("actors of schema %s:\n%s\n", schemaName, objectMarshaller.toJson(actors));
            String currentGroup = actors.get(0).getGroup();
            ActorPrivileges[] privileges = {
                    new ActorPrivileges(currentGroup, true, true, true, true),
                    new ActorPrivileges("audit", false, false, false, true),
                    new ActorPrivileges("runner", false, true, true, true),
            };
            List<ActorPrivileges> actorConfigure = Arrays.asList(privileges);
            {
                conn.updateSchemaActors(schemaName, actorConfigure);
                List<ActorPrivileges> updatedActors = conn.getSchemaActors(schemaName);
                if (!objectMarshaller.toJson(updatedActors).equals(objectMarshaller.toJson(privileges))) {
                    throw new Exception("actors mismatch on schema");
                }
                System.out.printf("update schema actors success, current actors: \n%s\n",
                        objectMarshaller.toJson(updatedActors));

            }
            {
                String content = "{\"name\": \"hello\", \"age\": 20, \"available\": true}";
                String docID = conn.addDocument(schemaName, "", content);
                System.out.printf("test doc %s added", docID);
                conn.updateDocumentActors(schemaName, docID, actorConfigure);
                List<ActorPrivileges> updatedActors = conn.getDocumentActors(schemaName, docID);
                if (!objectMarshaller.toJson(updatedActors).equals(objectMarshaller.toJson(privileges))) {
                    throw new Exception("actors mismatch on document");
                }
                System.out.printf("update document actors success, current actors: \n%s\n",
                        objectMarshaller.toJson(updatedActors));
            }
            {
                final String contractName = schemaName;
                List<ContractStep> steps = new ArrayList<>();
                steps.add(new ContractStep("delete_doc", new String[]{"@1", "@2"}));
                steps.add(new ContractStep("submit"));
                ContractDefine contractDefine = new ContractDefine(steps);
                if (conn.hasContract(contractName)) {
                    conn.withdrawContract(contractName);
                    System.out.printf("previous contract %s removed\n", contractName);
                }
                conn.deployContract(contractName, contractDefine);
                System.out.printf("contract %s deployed\n", contractName);

                conn.updateContractActors(contractName, actorConfigure);
                List<ActorPrivileges> updatedActors = conn.getContractActors(contractName);
                if (!objectMarshaller.toJson(updatedActors).equals(objectMarshaller.toJson(privileges))) {
                    throw new Exception("actors mismatch on document");
                }
                System.out.printf("contract document actors success, current actors: \n%s\n",
                        objectMarshaller.toJson(updatedActors));
                conn.withdrawContract(contractName);
            }
            conn.deleteSchema(schemaName);
            System.out.println("test actor functions: pass");
        }catch (Exception e){
            System.out.println(e);
            fail(e.toString());
        }

    }
}