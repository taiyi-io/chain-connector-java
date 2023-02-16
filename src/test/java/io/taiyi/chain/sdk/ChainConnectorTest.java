package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class ChainConnectorTest {
//    final static String host = "192.168.3.47";

    final static String host = "192.168.25.223";
    final static int port = 9100;
    final static String AccessFilename = "access_key.json";
    final static Gson objectMarshaller = new GsonBuilder().setPrettyPrinting().create();

    private ChainConnector getConnector() throws Exception {
        String currentDirectory = System.getProperty("user.dir");
        String filePath = currentDirectory + "/" + AccessFilename;
        // 读取文件内容
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        String fileContent = new String(fileBytes);
        Gson gson = new GsonBuilder().create();
        AccessKey accessKey = gson.fromJson(fileContent, AccessKey.class);
        ChainConnector connector = ChainConnector.NewConnectorFromAccess(accessKey);
        connector.setTrace(true);
        System.out.printf("connecting to gateway %s:%d...\n", host, port);
        connector.connect(host, port);
        System.out.printf("connected to gateway %s:%d\n", host, port);
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
            fail(e.getCause());
        }
    }

    @Test
    void testSchemas() throws Exception {
        ChainConnector conn = getConnector();
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
            conn.deleteSchema(schemaName);
            System.out.printf("schema %s deleted\n", schemaName);
        }
        System.out.println("test schemas pass");
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
    void testDocuments() throws Exception {
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
            if (conn.hasDocument(schemaName, docID)){
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
            System.out.println("doc " + docID + " updated");
        }
        for (var i = 0; i < docCount; i++) {
            String docID = docPrefix + (i + 1);
            conn.updateDocumentProperty(schemaName, docID, propertyNameAge, PropertyType.Integer, i);
            System.out.println("property age of doc " + docID + " updated");
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


}