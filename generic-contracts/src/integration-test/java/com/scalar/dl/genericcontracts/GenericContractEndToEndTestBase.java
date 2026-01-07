package com.scalar.dl.genericcontracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.storage.dynamo.DynamoAdmin;
import com.scalar.db.storage.dynamo.DynamoConfig;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.service.GenericContractClientService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GenericContractEndToEndTestBase {
  private static final int THREAD_NUM = 10;
  private static final String SCALAR_NAMESPACE = "scalar";
  private static final String ASSET_TABLE = "asset";
  private static final String ASSET_METADATA_TABLE = "asset_metadata";
  private static final String LEDGER_SCHEMA_PATH = "/scripts/ledger-schema.json";
  private static final String FUNCTION_DB_SCHEMA_PATH = "/scripts/objects-table-schema.json";
  private static final String PACKAGE_PREFIX = "com.scalar.dl.genericcontracts.";
  private static final String CLASS_DIR = "build/classes/java/main/";
  private static final String FUNCTION_NAMESPACE = "test";
  private static final String FUNCTION_TABLE = "objects";

  private static final String PROP_STORAGE = "scalardb.storage";
  private static final String PROP_CONTACT_POINTS = "scalardb.contact_points";
  private static final String PROP_USERNAME = "scalardb.username";
  private static final String PROP_PASSWORD = "scalardb.password";
  private static final String PROP_TRANSACTION_MANAGER = "scalardb.transaction_manager";
  private static final String PROP_DYNAMO_ENDPOINT_OVERRIDE = "scalardb.dynamo.endpoint_override";
  private static final String DEFAULT_STORAGE = "jdbc";
  private static final String DEFAULT_CONTACT_POINTS = "jdbc:mysql://localhost/";
  private static final String DEFAULT_USERNAME = "root";
  private static final String DEFAULT_PASSWORD = "mysql";
  private static final String DEFAULT_TRANSACTION_MANAGER = "consensus-commit";
  private static final String DEFAULT_DYNAMO_ENDPOINT_OVERRIDE = "http://localhost:8000";

  private static final String SOME_ENTITY_1 = "entity1";
  private static final String SOME_ENTITY_2 = "entity2";
  private static final String SOME_PRIVATE_KEY =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
          + "-----END EC PRIVATE KEY-----";
  public static final String SOME_CERTIFICATE =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICQTCCAeagAwIBAgIUEKARigcZQ3sLEXdlEtjYissVx0cwCgYIKoZIzj0EAwIw\n"
          + "QTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzES\n"
          + "MBAGA1UEChMJU2FtcGxlIENBMB4XDTE4MDYyMTAyMTUwMFoXDTE5MDYyMTAyMTUw\n"
          + "MFowRTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5\n"
          + "bzEWMBQGA1UEChMNU2FtcGxlIENsaWVudDBZMBMGByqGSM49AgEGCCqGSM49AwEH\n"
          + "A0IABGNIv4gBcSAUt3rW46Egtf2lCkFeuvsImsC+G2apJbvOjmyHO8K4nxxMT5lY\n"
          + "ShUpJTsmahqDolWQNM39C2XhWjyjgbcwgbQwDgYDVR0PAQH/BAQDAgWgMB0GA1Ud\n"
          + "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n"
          + "BBTpBQl/JxB7yr77uMVT9mMicPeVJTAfBgNVHSMEGDAWgBQrJo3N3/0j3oPS6F6m\n"
          + "wunHe8xLpzA1BgNVHREELjAsghJjbGllbnQuZXhhbXBsZS5jb22CFnd3dy5jbGll\n"
          + "bnQuZXhhbXBsZS5jb20wCgYIKoZIzj0EAwIDSQAwRgIhAJPtXSzuncDJXnM+7us8\n"
          + "46MEVjGHJy70bRY1My23RkxbAiEA5oFgTKMvls8e4UpnmUgFNP+FH8a5bF4tUPaV\n"
          + "BQiBbgk=\n"
          + "-----END CERTIFICATE-----";
  private static final int SOME_KEY_VERSION = 1;

  private ExecutorService executorService;
  private Properties props;
  private Map<String, String> creationOptions = new HashMap<>();
  private Path ledgerSchemaPath;
  private Path databaseSchemaPath;
  private String functionNamespace;
  private String functionTable;

  protected DistributedStorage storage;
  protected DistributedStorageAdmin storageAdmin;
  protected final ClientServiceFactory clientServiceFactory = new ClientServiceFactory();
  protected GenericContractClientService clientService;
  protected GenericContractClientService anotherClientService;

  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    executorService = Executors.newFixedThreadPool(getThreadNum());
    props = createStorageProperties();
    StorageFactory factory = StorageFactory.create(props);
    storage = factory.getStorage();
    storageAdmin = factory.getStorageAdmin();
    ledgerSchemaPath = Paths.get(System.getProperty("user.dir") + LEDGER_SCHEMA_PATH);
    databaseSchemaPath = Paths.get(getFunctionDatabaseSchema());
    functionNamespace = getFunctionNamespace();
    functionTable = getFunctionTable();
    createSchema();

    clientService = createClientService(SOME_ENTITY_1);
    clientService.registerCertificate();
    registerContracts(clientService, getContractsMap(), getContractPropertiesMap());
    registerFunction(clientService, getFunctionsMap());

    anotherClientService = createClientService(SOME_ENTITY_2);
    anotherClientService.registerCertificate();
    registerContracts(anotherClientService, getContractsMap(), getAnotherContractPropertiesMap());
  }

  @AfterAll
  public void tearDownAfterClass() throws SchemaLoaderException {
    storage.close();
    storageAdmin.close();
    SchemaLoader.unload(props, ledgerSchemaPath, true);
    SchemaLoader.unload(props, databaseSchemaPath, true);
  }

  @BeforeEach
  public void setUp() {}

  @AfterEach
  public void tearDown() throws ExecutionException {
    storageAdmin.truncateTable(SCALAR_NAMESPACE, ASSET_TABLE);
    storageAdmin.truncateTable(SCALAR_NAMESPACE, ASSET_METADATA_TABLE);
    storageAdmin.truncateTable(functionNamespace, functionTable);
  }

  abstract Map<String, String> getContractsMap();

  abstract Map<String, String> getFunctionsMap();

  protected Map<String, JsonNode> getContractPropertiesMap() {
    return ImmutableMap.of();
  }

  protected Map<String, JsonNode> getAnotherContractPropertiesMap() {
    return ImmutableMap.of();
  }

  protected String getScalarNamespace() {
    return SCALAR_NAMESPACE;
  }

  protected String getAssetTable() {
    return ASSET_TABLE;
  }

  protected String getAssetMetadataTable() {
    return ASSET_METADATA_TABLE;
  }

  protected String getFunctionDatabaseSchema() {
    return System.getProperty("user.dir") + FUNCTION_DB_SCHEMA_PATH;
  }

  protected String getFunctionNamespace() {
    return FUNCTION_NAMESPACE;
  }

  protected String getFunctionTable() {
    return FUNCTION_TABLE;
  }

  protected static String getContractId(String prefix, String contractName) {
    return prefix + "." + contractName;
  }

  protected static String getContractBinaryName(String prefix, String contractName) {
    return getContractBinaryName(getContractId(prefix, contractName));
  }

  protected static String getContractBinaryName(String contractId) {
    return PACKAGE_PREFIX + contractId;
  }

  protected static String getFunctionId(String prefix, String functionName) {
    return prefix + "." + functionName;
  }

  protected static String getFunctionBinaryName(String prefix, String functionName) {
    return getFunctionBinaryName(getFunctionId(prefix, functionName));
  }

  protected static String getFunctionBinaryName(String functionId) {
    return PACKAGE_PREFIX + functionId;
  }

  protected int getThreadNum() {
    return THREAD_NUM;
  }

  protected void executeInParallel(List<Callable<Void>> testCallables)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    List<Future<Void>> futures = executorService.invokeAll(testCallables);
    for (Future<Void> future : futures) {
      future.get();
    }
  }

  private Properties createStorageProperties() {
    String storage = System.getProperty(PROP_STORAGE, DEFAULT_STORAGE);
    String contactPoints = System.getProperty(PROP_CONTACT_POINTS, DEFAULT_CONTACT_POINTS);
    String username = System.getProperty(PROP_USERNAME, DEFAULT_USERNAME);
    String password = System.getProperty(PROP_PASSWORD, DEFAULT_PASSWORD);
    String transactionManager =
        System.getProperty(PROP_TRANSACTION_MANAGER, DEFAULT_TRANSACTION_MANAGER);
    String endpointOverride =
        System.getProperty(PROP_DYNAMO_ENDPOINT_OVERRIDE, DEFAULT_DYNAMO_ENDPOINT_OVERRIDE);

    Properties props = new Properties();
    props.put(DatabaseConfig.STORAGE, storage);
    props.put(DatabaseConfig.CONTACT_POINTS, contactPoints);
    props.put(DatabaseConfig.USERNAME, username);
    props.put(DatabaseConfig.PASSWORD, password);
    props.put(DatabaseConfig.TRANSACTION_MANAGER, transactionManager);

    if (storage.equals(DynamoConfig.STORAGE_NAME)) {
      props.put(DynamoConfig.ENDPOINT_OVERRIDE, endpointOverride);
      props.put(
          DynamoConfig.TABLE_METADATA_NAMESPACE, DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME);
      creationOptions =
          ImmutableMap.of(DynamoAdmin.NO_SCALING, "true", DynamoAdmin.NO_BACKUP, "true");
    }

    return props;
  }

  private void createSchema() throws SchemaLoaderException {
    SchemaLoader.load(props, ledgerSchemaPath, creationOptions, true);
    SchemaLoader.load(props, databaseSchemaPath, creationOptions, true);
  }

  private GenericContractClientService createClientService(String entity) throws IOException {
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, entity);
    props.put(ClientConfig.DS_CERT_VERSION, String.valueOf(SOME_KEY_VERSION));
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERTIFICATE);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY);
    return clientServiceFactory.createForGenericContract(new ClientConfig(props));
  }

  private void registerContracts(
      GenericContractClientService clientService,
      Map<String, String> contractsMap,
      Map<String, JsonNode> propertiesMap)
      throws IOException {
    for (Map.Entry<String, String> entry : contractsMap.entrySet()) {
      String contractId = entry.getKey();
      String contractBinaryName = entry.getValue();
      String contractFilePath = CLASS_DIR + contractBinaryName.replace('.', '/') + ".class";
      byte[] bytes = Files.readAllBytes(new File(contractFilePath).toPath());
      JsonNode properties = propertiesMap.getOrDefault(contractId, null);
      clientService.registerContract(contractId, contractBinaryName, bytes, properties);
    }
  }

  private void registerFunction(
      GenericContractClientService clientService, Map<String, String> functionsMap)
      throws IOException {
    for (Map.Entry<String, String> entry : functionsMap.entrySet()) {
      String functionId = entry.getKey();
      String functionBinaryName = entry.getValue();
      String functionFilePath = CLASS_DIR + functionBinaryName.replace('.', '/') + ".class";
      byte[] bytes = Files.readAllBytes(new File(functionFilePath).toPath());
      clientService.registerFunction(functionId, functionBinaryName, bytes);
    }
  }
}
