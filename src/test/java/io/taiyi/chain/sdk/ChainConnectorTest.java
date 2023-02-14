package io.taiyi.chain.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

class ChainConnectorTest {
//    final static String host = "192.168.3.47";

    final static String host = "192.168.25.223";
    final static int port = 9100;
    final static String AccessFilename = "access_key.json";

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
    void connect() {
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

}