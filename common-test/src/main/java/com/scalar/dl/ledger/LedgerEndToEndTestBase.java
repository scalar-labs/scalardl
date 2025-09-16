package com.scalar.dl.ledger;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.server.AdminService;
import com.scalar.dl.ledger.server.BaseServer;
import com.scalar.dl.ledger.server.LedgerPrivilegedService;
import com.scalar.dl.ledger.server.LedgerServerModule;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LedgerEndToEndTestBase {
  private static final String SCALAR_NAMESPACE = "scalar";
  private static final String ASSET_TABLE = "asset";
  private static final String ASSET_METADATA_TABLE = "asset_metadata";
  private static final String LEDGER_SCHEMA_PATH = "/../schema-loader/ledger-schema.json";
  private static final String FUNCTION_DB_SCHEMA_PATH =
      "/../generic-contracts/scripts/objects-table-schema.json";
  private static final String FUNCTION_NAMESPACE = "test";
  private static final String FUNCTION_TABLE = "objects";

  private static final String JDBC_TRANSACTION_MANAGER = "jdbc";
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

  private BaseServer ledgerServer;
  private Properties props;
  private Map<String, String> creationOptions = new HashMap<>();
  private Path ledgerSchemaPath;
  private Path databaseSchemaPath;
  private DistributedStorage storage;
  private DistributedStorageAdmin storageAdmin;

  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    props = createLedgerProperties();
    StorageFactory factory = StorageFactory.create(props);
    storage = factory.getStorage();
    storageAdmin = factory.getStorageAdmin();
    ledgerSchemaPath = Paths.get(System.getProperty("user.dir") + LEDGER_SCHEMA_PATH);
    databaseSchemaPath = Paths.get(System.getProperty("user.dir") + FUNCTION_DB_SCHEMA_PATH);
    SchemaLoader.load(props, ledgerSchemaPath, creationOptions, true);
    SchemaLoader.load(props, databaseSchemaPath, creationOptions, true);
    createServer(new LedgerConfig(props));
  }

  @AfterAll
  public void tearDownAfterClass() throws SchemaLoaderException, InterruptedException {
    ledgerServer.stop();
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
    storageAdmin.truncateTable(FUNCTION_NAMESPACE, FUNCTION_TABLE);
  }

  private Properties createLedgerProperties() {
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
    if (transactionManager.equals(JDBC_TRANSACTION_MANAGER)) {
      props.put(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "true");
    }
    props.put(LedgerConfig.PROOF_ENABLED, "true");
    props.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY);

    if (storage.equals(DynamoConfig.STORAGE_NAME)) {
      props.put(DynamoConfig.ENDPOINT_OVERRIDE, endpointOverride);
      props.put(
          DynamoConfig.TABLE_METADATA_NAMESPACE, DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME);
      creationOptions =
          ImmutableMap.of(DynamoAdmin.NO_SCALING, "true", DynamoAdmin.NO_BACKUP, "true");
    }

    return props;
  }

  private void createServer(LedgerConfig config) throws IOException, InterruptedException {
    Injector injector = Guice.createInjector(new LedgerServerModule(config));
    ledgerServer = new BaseServer(injector, config);

    ledgerServer.start(com.scalar.dl.ledger.server.LedgerService.class);
    ledgerServer.startPrivileged(LedgerPrivilegedService.class);
    ledgerServer.startAdmin(AdminService.class);
  }

  protected ClientConfig createClientConfig(String entity) throws IOException {
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, entity);
    props.put(ClientConfig.DS_CERT_VERSION, String.valueOf(SOME_KEY_VERSION));
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERTIFICATE);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY);
    return new ClientConfig(props);
  }

  protected DistributedStorage getStorage() {
    return storage;
  }

  protected DistributedStorageAdmin getStorageAdmin() {
    return storageAdmin;
  }

  protected String getScalarNamespace() {
    return SCALAR_NAMESPACE;
  }

  protected String getAssetTable() {
    return ASSET_TABLE;
  }

  protected String getFunctionNamespace() {
    return FUNCTION_NAMESPACE;
  }

  protected String getFunctionTable() {
    return FUNCTION_TABLE;
  }
}
