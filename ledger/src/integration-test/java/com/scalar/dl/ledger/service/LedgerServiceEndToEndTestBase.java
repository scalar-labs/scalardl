package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.service.Constants.AUDITOR_ENTITY_ID;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_A;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_B;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_C;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_D;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_NAMESPACE;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_TABLE;
import static com.scalar.dl.ledger.service.Constants.KEY_VERSION;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_A;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_B;
import static com.scalar.dl.ledger.service.Constants.SOME_CIPHER_KEY;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_A;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_B;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_A;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_B;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.StorageService;
import com.scalar.db.service.TransactionService;
import com.scalar.db.storage.cosmos.CosmosAdmin;
import com.scalar.db.storage.cosmos.CosmosConfig;
import com.scalar.db.storage.dynamo.DynamoAdmin;
import com.scalar.db.storage.dynamo.DynamoConfig;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.HmacSigner;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.FunctionRegistrationRequest;
import com.scalar.dl.ledger.model.SecretRegistrationRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LedgerServiceEndToEndTestBase {
  private static final Logger logger = LoggerFactory.getLogger(LedgerServiceEndToEndTestBase.class);
  protected static final String NAMESPACE = "scalar";
  protected static final String ASSET_TABLE = "asset";
  protected static final String ASSET_METADATA_TABLE = "asset_metadata";
  protected static final String CONTRACT_PACKAGE_NAME = "com.scalar.dl.ledger.service.contract.";
  protected static final String FUNCTION_PACKAGE_NAME = "com.scalar.dl.ledger.service.function.";
  protected static final String CONTRACT_CLASS_DIR =
      "../common-test/build/classes/java/main/com/scalar/dl/ledger/service/contract/";
  protected static final String FUNCTION_CLASS_DIR =
      "../common-test/build/classes/java/main/com/scalar/dl/ledger/service/function/";
  private static final String JDBC_TRANSACTION_MANAGER = "jdbc";
  private static final String PROP_NAMESPACE_SUFFIX = "scalardl.namespace_suffix";
  private static final String PROP_STORAGE = "scalardb.storage";
  private static final String PROP_CONTACT_POINTS = "scalardb.contact_points";
  private static final String PROP_USERNAME = "scalardb.username";
  private static final String PROP_PASSWORD = "scalardb.password";
  private static final String PROP_TRANSACTION_MANAGER = "scalardb.transaction_manager";
  private static final String PROP_COSMOS_REQUEST_UNIT = "scalardb.cosmos.ru";
  private static final String PROP_DYNAMO_ENDPOINT_OVERRIDE = "scalardb.dynamo.endpoint_override";
  private static final String DEFAULT_STORAGE = "cassandra";
  private static final String DEFAULT_CONTACT_POINTS = "localhost";
  private static final String DEFAULT_USERNAME = "cassandra";
  private static final String DEFAULT_PASSWORD = "cassandra";
  private static final String DEFAULT_TRANSACTION_MANAGER = "consensus-commit";
  private static final String DEFAULT_DYNAMO_ENDPOINT_OVERRIDE = "http://localhost:8000";
  protected static final Map<String, SignatureSigner> signers =
      ImmutableMap.of(
          ENTITY_ID_A, new DigitalSignatureSigner(PRIVATE_KEY_A),
          ENTITY_ID_B, new DigitalSignatureSigner(PRIVATE_KEY_B),
          ENTITY_ID_C, new HmacSigner(SECRET_KEY_A),
          ENTITY_ID_D, new HmacSigner(SECRET_KEY_B));

  private Properties props;
  private Path ledgerSchemaPath;
  private Path databaseSchemaPath;
  private Map<String, String> creationOptions = new HashMap<>();

  protected LedgerService ledgerService;
  protected LedgerValidationService validationService;
  protected LedgerConfig ledgerConfig;
  protected StorageService storageService;
  protected TransactionService transactionService;
  protected DistributedStorageAdmin storageAdmin;
  protected String namespace;
  protected String functionNamespace;

  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    ledgerSchemaPath = Paths.get(System.getProperty("user.dir") + "/scripts/create_schema.json");
    databaseSchemaPath =
        Paths.get(System.getProperty("user.dir") + "/scripts/create_schema_function.json");
    String suffix = System.getProperty(PROP_NAMESPACE_SUFFIX, "");
    namespace = NAMESPACE + suffix;
    functionNamespace = FUNCTION_NAMESPACE + suffix;

    props = createProperties();
    StorageFactory factory = StorageFactory.create(props);
    storageAdmin = factory.getStorageAdmin();
    createSchema();

    // For Digital Signature
    ledgerConfig = new LedgerConfig(props);
    createServices(ledgerConfig);
    registerCertificate();
    signers.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof DigitalSignatureSigner)
        .forEach(entry -> registerContracts(entry.getKey()));
    registerFunction(getFunctionsMap());

    // For HMAC
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    LedgerConfig hmacLedgerConfig = new LedgerConfig(props2);
    createServices(hmacLedgerConfig);
    registerSecret();
    signers.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof HmacSigner)
        .forEach(entry -> registerContracts(entry.getKey()));

    // Set up the security manager
    System.setProperty("java.security.manager", "default");
    System.setProperty("java.security.policy", "src/dist/security.policy");
    System.setSecurityManager(new SecurityManager());

    setUpBeforeClassPerTestInstance();
  }

  @AfterAll
  public void tearDownAfterClass() {
    tearDownAfterClassPerTestInstance();
    storageAdmin.close();
    try {
      SchemaLoader.unload(props, ledgerSchemaPath, true);
      SchemaLoader.unload(props, databaseSchemaPath, true);
    } catch (Exception e) {
      logger.warn("Failed to unload table", e);
    }
  }

  @BeforeEach
  public void setUp() {
    createServices(new LedgerConfig(props));
    setUpBeforeEach();
  }

  @AfterEach
  public void tearDown() throws ExecutionException {
    tearDownAfterEach();
    storageAdmin.truncateTable(namespace, ASSET_TABLE);
    storageAdmin.truncateTable(namespace, ASSET_METADATA_TABLE);
    storageAdmin.truncateTable(functionNamespace, FUNCTION_TABLE);
    storageService.close();
    transactionService.close();
  }

  abstract Map<String, Map<String, String>> getContractsMap();

  abstract Map<String, Map<String, String>> getContractPropertiesMap();

  abstract Map<String, String> getFunctionsMap();

  protected void setUpBeforeClassPerTestInstance() {}

  protected void tearDownAfterClassPerTestInstance() {}

  protected void setUpBeforeEach() {}

  protected void tearDownAfterEach() {}

  protected Properties createProperties() {
    String storage = System.getProperty(PROP_STORAGE, DEFAULT_STORAGE);
    String contactPoints = System.getProperty(PROP_CONTACT_POINTS, DEFAULT_CONTACT_POINTS);
    String username = System.getProperty(PROP_USERNAME, DEFAULT_USERNAME);
    String password = System.getProperty(PROP_PASSWORD, DEFAULT_PASSWORD);
    String transactionManager =
        System.getProperty(PROP_TRANSACTION_MANAGER, DEFAULT_TRANSACTION_MANAGER);
    String requestUnit =
        System.getProperty(PROP_COSMOS_REQUEST_UNIT, CosmosAdmin.DEFAULT_REQUEST_UNIT);
    String endpointOverride =
        System.getProperty(PROP_DYNAMO_ENDPOINT_OVERRIDE, DEFAULT_DYNAMO_ENDPOINT_OVERRIDE);

    Properties props = new Properties();
    props.put(LedgerConfig.NAMESPACE, namespace);
    if (transactionManager.equals(JDBC_TRANSACTION_MANAGER)) {
      props.put(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "true");
    }
    props.put(DatabaseConfig.STORAGE, storage);
    props.put(DatabaseConfig.CONTACT_POINTS, contactPoints);
    props.put(DatabaseConfig.USERNAME, username);
    props.put(DatabaseConfig.PASSWORD, password);
    props.put(DatabaseConfig.TRANSACTION_MANAGER, transactionManager);

    if (storage.equals(CosmosConfig.STORAGE_NAME)) {
      creationOptions = ImmutableMap.of(CosmosAdmin.REQUEST_UNIT, requestUnit);
    }

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

  protected void createServices(LedgerConfig config) {
    Injector injector = Guice.createInjector(new LedgerModule(config));
    ledgerService = injector.getInstance(LedgerService.class);
    validationService = injector.getInstance(LedgerValidationService.class);
    storageService = injector.getInstance(StorageService.class);
    transactionService = injector.getInstance(TransactionService.class);
  }

  private void registerCertificate() {
    ledgerService.register(
        new CertificateRegistrationRequest(null, ENTITY_ID_A, KEY_VERSION, CERTIFICATE_A));
    ledgerService.register(
        new CertificateRegistrationRequest(null, ENTITY_ID_B, KEY_VERSION, CERTIFICATE_B));
    ledgerService.register(
        new CertificateRegistrationRequest(null, AUDITOR_ENTITY_ID, KEY_VERSION, CERTIFICATE_B));
  }

  private void registerSecret() {
    ledgerService.register(
        new SecretRegistrationRequest(null, ENTITY_ID_C, KEY_VERSION, SECRET_KEY_A));
    ledgerService.register(
        new SecretRegistrationRequest(null, ENTITY_ID_D, KEY_VERSION, SECRET_KEY_B));
  }

  private void registerContracts(String entityId) {
    SignatureSigner signer = signers.get(entityId);
    Map<String, String> contracts =
        getContractsMap().containsKey(entityId)
            ? getContractsMap().get(entityId)
            : ImmutableMap.of();
    Map<String, String> contractProperties = getContractPropertiesMap().get(entityId);
    registerContracts(entityId, signer, contracts, contractProperties);
  }

  private void registerContracts(
      String entityId,
      SignatureSigner signer,
      Map<String, String> contractMap,
      @Nullable Map<String, String> properties) {
    final Map<String, String> contractProperties =
        properties != null ? properties : ImmutableMap.of();
    for (Map.Entry<String, String> entry : contractMap.entrySet()) {
      byte[] bytes;
      try {
        bytes = Files.readAllBytes(new File(entry.getValue()).toPath());
      } catch (IOException e) {
        throw new RuntimeException("Failed to read contract file: " + entry.getValue(), e);
      }

      String contractName = entry.getKey();
      String contractId = contractName.substring(contractName.lastIndexOf('.') + 1);

      byte[] serialized =
          ContractRegistrationRequest.serialize(
              contractId,
              contractName,
              bytes,
              contractProperties.get(contractId),
              null,
              entityId,
              KEY_VERSION);
      ContractRegistrationRequest request =
          new ContractRegistrationRequest(
              contractId,
              contractName,
              bytes,
              contractProperties.get(contractId),
              null,
              entityId,
              KEY_VERSION,
              signer.sign(serialized));

      ledgerService.register(request);
    }
  }

  private void registerFunction(Map<String, String> functionMap) {
    for (Map.Entry<String, String> entry : functionMap.entrySet()) {
      byte[] bytes;
      try {
        bytes = Files.readAllBytes(new File(entry.getValue()).toPath());
      } catch (IOException e) {
        throw new RuntimeException("Failed to read function file: " + entry.getValue(), e);
      }

      FunctionRegistrationRequest request =
          new FunctionRegistrationRequest(entry.getKey(), entry.getKey(), bytes);
      ledgerService.register(request);
    }
  }
}
