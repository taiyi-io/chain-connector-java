# SDK of Chain Connector in Java binding

用于访问和操作链平台的Java语言SDK，基于openJDK 18编译。

The Java language SDK for accessing and operating on blockchain platforms is based on openJDK 18.

## 项目 Project

项目基于apache maven管理

The project is managed based on Apache Maven.

### 编译 Compile

```
$mvn compile
```

### 构建库 Build JAR

```
$mvn package
```

### 运行测试用例 Run testing

```
$mvn test
```



## 使用范例 Usage

### 连接链平台 Connect the chain

首先使用平台分配的私钥数据构建Connector，然后连接链平台的gateway模块。

Initial the connector using the private key data allocated by the chain platform, then connect to the gateway module.

```java
AccessKey accessKey = gson.fromJson(fileContent, AccessKey.class);
ChainConnector connector = ChainConnector.NewConnectorFromAccess(accessKey);
connector.connect(gatewayHost, gatewayPort);
```



## 构建与管理数字资产 Build and manage digital assets

首先为数字资产定义数据范式（Schema），然后就能够基于该Schema添加、修改、删除和查询数字资产(Document)。所有变更痕迹自动使用区块链技术持久化存储，并且能够通过getSchemaLog和getDocumentLog接口查询。

Define a data schema for digital assets, and then you can add, update, delete, and query documents (digital assets) under the schema. All changes are automatically persistently stored using blockchain and could be queried using getSchemaLog and getDocumentLog.

```java
//create new schema
String schemaName = "sample";
List<DocumentProperty> properties = new ArrayList<>();
properties.add(new DocumentProperty("name", PropertyType.String));
properties.add(new DocumentProperty("age", PropertyType.Integer));
properties.add(new DocumentProperty("available", PropertyType.Boolean));
conn.createSchema(schemaName, properties);
DocumentSchema schema = conn.getSchema(schemaName);

//add a document
String content = "{\"name\": \"hello\", \"age\": 20, \"available\": true}";
String docID = conn.addDocument(schemaName, "", content);

//check a document
if (conn.hasDocument(schemaName, docID)){
	//update a existed document
    String updatedContent = "{\"name\": \"alice\", \"age\": 18, \"available\": false}";
    conn.updateDocument(schemaName, docID, updatedContent);
}

//get change trace of a document
LogRecords logs = conn.getDocumentLogs(schemaName, docID);

//query documents
QueryCondition condition = new QueryBuilder()
    .AscendBy("name")
    .MaxRecord(20)
    .SetOffset(0)
    .Build();
DocumentRecords records = conn.queryDocuments(schemaName, condition);


//remove document
conn.removeDocument(schemaName, docID);

```



### 部署和调用智能合约 Deploy and invoke the Smart Contract

部署智能合约时，需要设定合约名称和执行步骤。调用时，指定合约名称和调用参数就可以启动执行。系统允许打开追踪开关，查看合约执行计划和实际运行情况。

It is necessary to assign a name and execute steps to deploy a Smart Contract. Then initiate execution using the contract name and call parameters. The system can enable the trace option for a contract, which allows the user to review the contract's execution plan and steps.



```java
String contractName = "contract_create";
List<ContractStep> steps = new ArrayList<>();
steps.add(new ContractStep("create_doc", new String[]{"$s", "@1", "@2"}));
steps.add(new ContractStep("set_property", new String[]{"$s", "catalog", "@3"}));
steps.add(new ContractStep("set_property", new String[]{"$s", "balance", "@4"}));
steps.add(new ContractStep("set_property", new String[]{"$s", "number", "@5"}));
steps.add(new ContractStep("set_property", new String[]{"$s", "available", "@6"}));
steps.add(new ContractStep("set_property", new String[]{"$s", "weight", "@7"}));
steps.add(new ContractStep("update_doc", new String[]{"@1", "$s"}));
steps.add(new ContractStep("submit"));

ContractDefine contractDefine = new ContractDefine(steps);
//check existed contract
if (conn.hasContract(contractName)) {
    //withdraw existed contract
    conn.withdrawContract(contractName);
    System.out.printf("previous contract %s removed\n", contractName);
}

//deploy contact
conn.deployContract(contractName, contractDefine);

//enable trace option
ContractInfo info = conn.getContractInfo(contractName);
if (!info.isEnabled()) {
    conn.enableContractTrace(contractName);
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

//call contract with parameters
conn.callContract(createContractName, new ArrayList<>(Arrays.asList(parameters)));

```



### 检查区块链与交易 Audit the block chain and transaction

通过SDK能够获取并检查链、区块、交易的全部详细信息，用于审计数据安全性和检查后台运行情况。

Through the SDK, you can obtain and check all the details of chains, blocks, and transactions, which can be used to audit data security and monitor the background operation.

```java
//check chain status
ChainStatus status = conn.getStatus();

//query blocks from height 1 to 10
BlockRecords blockRecords = conn.queryBlocks(1, 10);
for (String blockID : blockRecords.getBlocks()) {
    //get block data
    BlockData blockData = conn.getBlock(blockID);
    //query transactions in a block
    TransactionRecords transactionRecords = conn.queryTransactions(blockID, 0, 20);
    for (String transID : transactionRecords.getTransactions()) {
        //get transaction data
        TransactionData transactionData = conn.getTransaction(blockID, transID);
    }
}

```

