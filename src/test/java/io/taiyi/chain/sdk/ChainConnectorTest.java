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
    final static String host = "192.168.3.47";

//    final static String host = "192.168.25.223";
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
        try{
            System.out.println("test case: connect begin...");
            ChainConnector conn = getConnector();
            conn.activate();
            System.out.println("test case: connect passed");
        }catch (Exception e){
            System.out.println(e);
            fail(e.getCause());
        }
    }

    @Test
    void getStatus() {
        try{
            System.out.println("test case: get status begin...");
            ChainConnector conn = getConnector();
            ChainStatus status = conn.getStatus();
            System.out.println(status);
            System.out.println("test case: get status passed");
        }catch (Exception e){
            System.out.println(e);
            fail(e.getCause());
        }
    }

    @Test
    void testSchemas() throws Exception {
        ChainConnector conn = getConnector();
        final String schemaName = "js-test-case1-schema";
        if (conn.hasSchema(schemaName)){
            conn.deleteSchema(schemaName);
            System.out.printf("previous schema %s deleted\n", schemaName);
        }else{
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
}