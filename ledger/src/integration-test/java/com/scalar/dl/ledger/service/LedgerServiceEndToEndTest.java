package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.service.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.AUDITOR_ENTITY_ID;
import static com.scalar.dl.ledger.service.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.CERTIFICATE_A;
import static com.scalar.dl.ledger.service.Constants.CERTIFICATE_B;
import static com.scalar.dl.ledger.service.Constants.CONTRACT_ID_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID2;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID3;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID4;
import static com.scalar.dl.ledger.service.Constants.CREATE_FUNCTION_ID1;
import static com.scalar.dl.ledger.service.Constants.CREATE_FUNCTION_ID2;
import static com.scalar.dl.ledger.service.Constants.CREATE_FUNCTION_ID3;
import static com.scalar.dl.ledger.service.Constants.CREATE_FUNCTION_ID4;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_A;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_B;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_C;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_D;
import static com.scalar.dl.ledger.service.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_NAMESPACE;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_TABLE;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID2;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID3;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID4;
import static com.scalar.dl.ledger.service.Constants.HOLDER_CHECKER_CONTRACT_ID;
import static com.scalar.dl.ledger.service.Constants.ID_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.KEY_VERSION;
import static com.scalar.dl.ledger.service.Constants.NAMESPACE_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.PAYMENT_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.PAYMENT_CONTRACT_ID2;
import static com.scalar.dl.ledger.service.Constants.PAYMENT_CONTRACT_ID3;
import static com.scalar.dl.ledger.service.Constants.PAYMENT_CONTRACT_ID4;
import static com.scalar.dl.ledger.service.Constants.PRIVATE_KEY_A;
import static com.scalar.dl.ledger.service.Constants.PRIVATE_KEY_B;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_A;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_B;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_1;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_2;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_3;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_1;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_2;
import static com.scalar.dl.ledger.service.Constants.SOME_CIPHER_KEY;
import static com.scalar.dl.ledger.service.Constants.SOME_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
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
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.crypto.HmacSigner;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.database.scalardb.AssetAttribute;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.FunctionRegistrationRequest;
import com.scalar.dl.ledger.model.LedgerValidationRequest;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.function.CreateFunction;
import com.scalar.dl.ledger.service.function.CreateFunctionWithJackson;
import com.scalar.dl.ledger.service.function.CreateFunctionWithJsonp;
import com.scalar.dl.ledger.service.function.CreateFunctionWithString;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LedgerServiceEndToEndTest {
  private static final Logger logger = LoggerFactory.getLogger(LedgerServiceEndToEndTest.class);
  private static final String NAMESPACE = "scalar";
  private static final String ASSET_TABLE = "asset";
  private static final String ASSET_METADATA_TABLE = "asset_metadata";
  private static final String CONTRACT_PACKAGE_NAME = "com.scalar.dl.ledger.service.contract.";
  private static final String FUNCTION_PACKAGE_NAME = "com.scalar.dl.ledger.service.function.";
  private static final String CONTRACT_CLASS_DIR =
      "build/classes/java/integrationTest/com/scalar/dl/ledger/service/contract/";
  private static final String FUNCTION_CLASS_DIR =
      "build/classes/java/integrationTest/com/scalar/dl/ledger/service/function/";
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
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  private static LedgerService ledgerService;
  private static LedgerValidationService validationService;
  private static LedgerConfig ledgerConfig;
  private static StorageService storageService;
  private static TransactionService transactionService;
  private static DigitalSignatureSigner dsSigner1;
  private static DigitalSignatureSigner dsSigner2;
  private static HmacSigner hmacSigner1;
  private static HmacSigner hmacSigner2;
  private static Properties props;
  private static Path ledgerSchemaPath;
  private static Path databaseSchemaPath;
  private static DistributedStorageAdmin storageAdmin;
  private static Map<String, String> creationOptions = new HashMap<>();
  private static String namespace;
  private static String functionNamespace;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ledgerSchemaPath = Paths.get(System.getProperty("user.dir") + "/scripts/create_schema.json");
    databaseSchemaPath =
        Paths.get(System.getProperty("user.dir") + "/scripts/create_schema_function.json");
    String suffix = System.getProperty(PROP_NAMESPACE_SUFFIX, "");
    namespace = NAMESPACE + suffix;
    functionNamespace = FUNCTION_NAMESPACE + suffix;
    props = createProperties();
    ledgerConfig = new LedgerConfig(props);
    StorageFactory factory = new StorageFactory(new DatabaseConfig(props));
    storageAdmin = factory.getAdmin();
    dsSigner1 = new DigitalSignatureSigner(PRIVATE_KEY_A);
    dsSigner2 = new DigitalSignatureSigner(PRIVATE_KEY_B);
    createSchema();
    createServices(ledgerConfig);
    registerCertificate();

    Map<String, String> contractMap =
        ImmutableMap.<String, String>builder()
            .put(
                CONTRACT_PACKAGE_NAME + CREATE_CONTRACT_ID1,
                CONTRACT_CLASS_DIR + CREATE_CONTRACT_ID1 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + CREATE_CONTRACT_ID2,
                CONTRACT_CLASS_DIR + CREATE_CONTRACT_ID2 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + CREATE_CONTRACT_ID3,
                CONTRACT_CLASS_DIR + CREATE_CONTRACT_ID3 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + CREATE_CONTRACT_ID4,
                CONTRACT_CLASS_DIR + CREATE_CONTRACT_ID4 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + PAYMENT_CONTRACT_ID1,
                CONTRACT_CLASS_DIR + PAYMENT_CONTRACT_ID1 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + PAYMENT_CONTRACT_ID2,
                CONTRACT_CLASS_DIR + PAYMENT_CONTRACT_ID2 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + PAYMENT_CONTRACT_ID3,
                CONTRACT_CLASS_DIR + PAYMENT_CONTRACT_ID3 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + PAYMENT_CONTRACT_ID4,
                CONTRACT_CLASS_DIR + PAYMENT_CONTRACT_ID4 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + GET_BALANCE_CONTRACT_ID1,
                CONTRACT_CLASS_DIR + GET_BALANCE_CONTRACT_ID1 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + GET_BALANCE_CONTRACT_ID2,
                CONTRACT_CLASS_DIR + GET_BALANCE_CONTRACT_ID2 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + GET_BALANCE_CONTRACT_ID3,
                CONTRACT_CLASS_DIR + GET_BALANCE_CONTRACT_ID3 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + GET_BALANCE_CONTRACT_ID4,
                CONTRACT_CLASS_DIR + GET_BALANCE_CONTRACT_ID4 + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + HOLDER_CHECKER_CONTRACT_ID,
                CONTRACT_CLASS_DIR + HOLDER_CHECKER_CONTRACT_ID + ".class")
            .build();
    Map<String, String> propertiesMap =
        ImmutableMap.of(
            CREATE_CONTRACT_ID1,
            Json.createObjectBuilder()
                .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID1)
                .build()
                .toString(),
            CREATE_CONTRACT_ID2,
            Json.createObjectBuilder()
                .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID2)
                .build()
                .toString(),
            CREATE_CONTRACT_ID3,
            jacksonSerDe.serialize(
                mapper.createObjectNode().put(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID3)),
            CREATE_CONTRACT_ID4,
            CREATE_CONTRACT_ID4,
            GET_BALANCE_CONTRACT_ID1,
            Json.createObjectBuilder()
                .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID1)
                .build()
                .toString(),
            GET_BALANCE_CONTRACT_ID2,
            Json.createObjectBuilder()
                .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID2)
                .build()
                .toString(),
            GET_BALANCE_CONTRACT_ID3,
            jacksonSerDe.serialize(
                mapper
                    .createObjectNode()
                    .put(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID3)),
            GET_BALANCE_CONTRACT_ID4,
            GET_BALANCE_CONTRACT_ID4);
    registerContracts(ENTITY_ID_A, dsSigner1, contractMap, null, propertiesMap);
    registerContracts(
        ENTITY_ID_B,
        dsSigner2,
        ImmutableMap.of(
            CONTRACT_PACKAGE_NAME + HOLDER_CHECKER_CONTRACT_ID,
            CONTRACT_CLASS_DIR + HOLDER_CHECKER_CONTRACT_ID + ".class"),
        null,
        Collections.emptyMap());

    Map<String, String> functionMap =
        ImmutableMap.of(
            FUNCTION_PACKAGE_NAME + CREATE_FUNCTION_ID1,
            FUNCTION_CLASS_DIR + CREATE_FUNCTION_ID1 + ".class",
            FUNCTION_PACKAGE_NAME + CREATE_FUNCTION_ID2,
            FUNCTION_CLASS_DIR + CREATE_FUNCTION_ID2 + ".class",
            FUNCTION_PACKAGE_NAME + CREATE_FUNCTION_ID3,
            FUNCTION_CLASS_DIR + CREATE_FUNCTION_ID3 + ".class",
            FUNCTION_PACKAGE_NAME + CREATE_FUNCTION_ID4,
            FUNCTION_CLASS_DIR + CREATE_FUNCTION_ID4 + ".class");
    registerFunction(functionMap);

    // For HMAC
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    registerSecret();
    hmacSigner1 = new HmacSigner(SECRET_KEY_A);
    hmacSigner2 = new HmacSigner(SECRET_KEY_B);
    registerContracts(ENTITY_ID_C, hmacSigner1, contractMap, null, propertiesMap);
    registerContracts(
        ENTITY_ID_D,
        hmacSigner2,
        ImmutableMap.of(
            CONTRACT_PACKAGE_NAME + HOLDER_CHECKER_CONTRACT_ID,
            CONTRACT_CLASS_DIR + HOLDER_CHECKER_CONTRACT_ID + ".class"),
        null,
        Collections.emptyMap());

    // Set up the security manager
    System.setProperty("java.security.manager", "default");
    System.setProperty("java.security.policy", "src/dist/security.policy");
    System.setSecurityManager(new SecurityManager());
  }

  @AfterAll
  public static void tearDownAfterClass() {
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
  }

  @AfterEach
  public void tearDown() throws ExecutionException {
    storageAdmin.truncateTable(namespace, ASSET_TABLE);
    storageAdmin.truncateTable(namespace, ASSET_METADATA_TABLE);
    storageAdmin.truncateTable(functionNamespace, FUNCTION_TABLE);
    storageService.close();
    transactionService.close();
  }

  private static Properties createProperties() {
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

  @SuppressWarnings("unused")
  private boolean isTxStateManagementEnabled() {
    return ledgerConfig.isTxStateManagementEnabled();
  }

  private static void createSchema() throws SchemaLoaderException {
    SchemaLoader.load(props, ledgerSchemaPath, creationOptions, true);
    SchemaLoader.load(props, databaseSchemaPath, creationOptions, true);
  }

  private static void createServices(LedgerConfig config) {
    Injector injector = Guice.createInjector(new LedgerModule(config));
    ledgerService = injector.getInstance(LedgerService.class);
    validationService = injector.getInstance(LedgerValidationService.class);
    storageService = injector.getInstance(StorageService.class);
    transactionService = injector.getInstance(TransactionService.class);
  }

  private static void registerCertificate() {
    ledgerService.register(
        new CertificateRegistrationRequest(ENTITY_ID_A, KEY_VERSION, CERTIFICATE_A));
    ledgerService.register(
        new CertificateRegistrationRequest(ENTITY_ID_B, KEY_VERSION, CERTIFICATE_B));
    ledgerService.register(
        new CertificateRegistrationRequest(AUDITOR_ENTITY_ID, KEY_VERSION, CERTIFICATE_B));
  }

  private static void registerSecret() {
    ledgerService.register(new SecretEntry(ENTITY_ID_C, KEY_VERSION, SECRET_KEY_A, 1L));
    ledgerService.register(new SecretEntry(ENTITY_ID_D, KEY_VERSION, SECRET_KEY_B, 1L));
  }

  private static void registerContracts(
      String entityId,
      SignatureSigner signer,
      Map<String, String> contractMap,
      @Nullable Map<String, String> nameIdMap,
      @Nullable Map<String, String> properties)
      throws IOException {
    for (Map.Entry<String, String> entry : contractMap.entrySet()) {
      byte[] bytes = Files.readAllBytes(new File(entry.getValue()).toPath());

      String contractName = entry.getKey();
      String contractId = contractName.substring(contractName.lastIndexOf('.') + 1);
      if (nameIdMap != null && nameIdMap.containsKey(contractName)) {
        contractId = nameIdMap.get(contractName);
      }

      byte[] serialized =
          ContractRegistrationRequest.serialize(
              contractId, contractName, bytes, properties.get(contractId), entityId, KEY_VERSION);
      ContractRegistrationRequest request =
          new ContractRegistrationRequest(
              contractId,
              contractName,
              bytes,
              properties.get(contractId),
              entityId,
              KEY_VERSION,
              signer.sign(serialized));

      ledgerService.register(request);
    }
  }

  private static void registerFunction(Map<String, String> functionMap) throws IOException {
    for (Map.Entry<String, String> entry : functionMap.entrySet()) {
      byte[] bytes = Files.readAllBytes(new File(entry.getValue()).toPath());
      FunctionRegistrationRequest request =
          new FunctionRegistrationRequest(entry.getKey(), entry.getKey(), bytes);
      ledgerService.register(request);
    }
  }

  private ContractExecutionRequest prepareRequestForCreate(
      DeserializationType type,
      String assetId,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    switch (type) {
      case JSONP_JSON:
        return prepareRequestForCreateBasedOnJsonp(
            assetId, amount, nonce, entityId, signer, isV2Argument);
      case JACKSON_JSON:
        return prepareRequestForCreateBasedOnJackson(
            assetId, amount, nonce, entityId, signer, isV2Argument);
      case STRING:
        return prepareRequestForCreateBasedOnString(assetId, amount, nonce, entityId, signer);
      default: // DEPRECATED
        return prepareRequestForCreateBasedOnDeprecated(
            assetId, amount, nonce, entityId, signer, isV2Argument);
    }
  }

  private ContractExecutionRequest prepareRequestForCreateBasedOnDeprecated(
      String assetId,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = CREATE_CONTRACT_ID1;
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, assetId)
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .build();
    if (!isV2Argument) {
      contractArgument =
          Json.createObjectBuilder(contractArgument)
              .add(Argument.NONCE_KEY_NAME, nonce.toString())
              .build();
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForCreateBasedOnJsonp(
      String assetId,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = CREATE_CONTRACT_ID2;
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, assetId)
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .build();
    if (!isV2Argument) {
      contractArgument =
          Json.createObjectBuilder(contractArgument)
              .add(Argument.NONCE_KEY_NAME, nonce.toString())
              .build();
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForCreateBasedOnJackson(
      String assetId,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = CREATE_CONTRACT_ID3;
    ObjectNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, assetId)
            .put(AMOUNT_ATTRIBUTE_NAME, amount);

    if (!isV2Argument) {
      contractArgument.put(Argument.NONCE_KEY_NAME, nonce.toString());
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForCreateBasedOnString(
      String assetId, int amount, UUID nonce, String entityId, SignatureSigner signer) {
    String contractId = CREATE_CONTRACT_ID4;
    String contractArgument = assetId + "," + amount;

    String argument = Argument.format(contractArgument, nonce.toString(), Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForPayment(
      DeserializationType type,
      String assetIdFrom,
      String assetIdTo,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    switch (type) {
      case JSONP_JSON:
        return prepareRequestForPaymentBasedOnJsonp(
            assetIdFrom, assetIdTo, amount, nonce, entityId, signer, isV2Argument);
      case JACKSON_JSON:
        return prepareRequestForPaymentBasedOnJackson(
            assetIdFrom, assetIdTo, amount, nonce, entityId, signer, isV2Argument);
      case STRING:
        return prepareRequestForPaymentBasedOnString(
            assetIdFrom, assetIdTo, amount, nonce, entityId, signer);
      default: // DEPRECATED
        return prepareRequestForPaymentBasedOnDeprecated(
            assetIdFrom, assetIdTo, amount, nonce, entityId, signer, isV2Argument);
    }
  }

  private static ContractExecutionRequest prepareRequestForPaymentBasedOnDeprecated(
      String assetIdFrom,
      String assetIdTo,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = PAYMENT_CONTRACT_ID1;
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(assetIdFrom).add(assetIdTo).build())
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .build();
    if (!isV2Argument) {
      contractArgument =
          Json.createObjectBuilder(contractArgument)
              .add(Argument.NONCE_KEY_NAME, nonce.toString())
              .build();
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private static ContractExecutionRequest prepareRequestForPaymentBasedOnJsonp(
      String assetIdFrom,
      String assetIdTo,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = PAYMENT_CONTRACT_ID2;
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(assetIdFrom).add(assetIdTo).build())
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .build();
    if (!isV2Argument) {
      contractArgument =
          Json.createObjectBuilder(contractArgument)
              .add(Argument.NONCE_KEY_NAME, nonce.toString())
              .build();
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private static ContractExecutionRequest prepareRequestForPaymentBasedOnJackson(
      String assetIdFrom,
      String assetIdTo,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer,
      boolean isV2Argument) {
    String contractId = PAYMENT_CONTRACT_ID3;
    ObjectNode contractArgument =
        mapper
            .createObjectNode()
            .put(AMOUNT_ATTRIBUTE_NAME, amount)
            .set(ASSETS_ATTRIBUTE_NAME, mapper.createArrayNode().add(assetIdFrom).add(assetIdTo));

    if (!isV2Argument) {
      contractArgument.put(Argument.NONCE_KEY_NAME, nonce.toString());
    }

    String argument =
        isV2Argument
            ? Argument.format(contractArgument, nonce.toString(), Collections.emptyList())
            : contractArgument.toString();

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private static ContractExecutionRequest prepareRequestForPaymentBasedOnString(
      String assetIdFrom,
      String assetIdTo,
      int amount,
      UUID nonce,
      String entityId,
      SignatureSigner signer) {
    String contractId = PAYMENT_CONTRACT_ID4;
    String contractArgument = assetIdFrom + "," + assetIdTo + "," + amount;

    String argument = Argument.format(contractArgument, nonce.toString(), Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForHolderChecker(
      UUID nonce, String entityId, DigitalSignatureSigner signer) {
    JsonObject argument =
        Json.createObjectBuilder().add(Argument.NONCE_KEY_NAME, nonce.toString()).build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            HOLDER_CHECKER_CONTRACT_ID, argument.toString(), entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce.toString(),
        entityId,
        KEY_VERSION,
        HOLDER_CHECKER_CONTRACT_ID,
        argument.toString(),
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private static LedgerValidationRequest prepareValidationRequest(String assetId) {
    return prepareValidationRequest(assetId, 0, Integer.MAX_VALUE, ENTITY_ID_A, dsSigner1);
  }

  private static LedgerValidationRequest prepareValidationRequest(
      String assetId, int startAge, int endAge, String entityId, SignatureSigner signer) {
    byte[] serialized =
        LedgerValidationRequest.serialize(assetId, startAge, endAge, entityId, KEY_VERSION);
    return new LedgerValidationRequest(
        assetId, startAge, endAge, entityId, KEY_VERSION, signer.sign(serialized));
  }

  private void createAssets(Optional<UUID> nonce, DeserializationType type, boolean isV2Argument) {
    createAssets(nonce, type, isV2Argument, ENTITY_ID_A, dsSigner1);
  }

  private void createAssets(
      Optional<UUID> nonce,
      DeserializationType type,
      boolean isV2Argument,
      String entityId,
      SignatureSigner signer) {
    ledgerService.execute(
        prepareRequestForCreate(
            type,
            SOME_ASSET_ID_1,
            SOME_AMOUNT_1,
            nonce.orElse(UUID.randomUUID()),
            entityId,
            signer,
            isV2Argument));
    ledgerService.execute(
        prepareRequestForCreate(
            type,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_1,
            UUID.randomUUID(),
            entityId,
            signer,
            isV2Argument));
    ledgerService.execute(
        prepareRequestForPayment(
            type,
            SOME_ASSET_ID_1,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_2,
            nonce.orElse(UUID.randomUUID()),
            entityId,
            signer,
            isV2Argument));
    ledgerService.execute(
        prepareRequestForPayment(
            type,
            SOME_ASSET_ID_1,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_3,
            UUID.randomUUID(),
            entityId,
            signer,
            isV2Argument));
  }

  @Test
  public void validate_AssetsCreatedWithDeprecatedAndNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void
      validate_AssetsCreatedWithDeprecatedWithV2ArgumentAndNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, true); // use V2 argument formant

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetsCreatedWithJsonpAndNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.JSONP_JSON, false);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetsCreatedWithJsonpWithV2ArgumentNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.JSONP_JSON, true); // use V2 argument formant

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetsCreatedWithJacksonAndNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.JACKSON_JSON, false);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetsCreatedWithJacksonWithV2ArgumentNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(
        Optional.empty(), DeserializationType.JACKSON_JSON, true); // use V2 argument formant

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void
      validate_AssetsCreatedWithJacksonWithV2ArgumentNothingTamperedInParallel_ShouldReturnTrue() {
    // Arrange
    createAssets(
        Optional.empty(), DeserializationType.JACKSON_JSON, true); // use V2 argument formant
    AccessControlContext context = AccessController.getContext();

    // Act Assert
    List<String> assetIds = Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2);
    assetIds
        .parallelStream()
        .forEach(
            assetId ->
                AccessController.doPrivileged(
                    (PrivilegedAction<Void>)
                        () -> {
                          LedgerValidationResult result =
                              validationService.validate(prepareValidationRequest(assetId));
                          assertThat(result.getCode()).isEqualTo(StatusCode.OK);
                          return null;
                        },
                    context));
  }

  @Test
  public void validate_AssetsCreatedWithStringWithV2ArgumentNothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.STRING, true); // use V2 argument formant

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_MiddleRecordRemoved_ShouldReturnWithInconsistentStateValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // remove the middle record maliciously
    Delete delete =
        new Delete(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 1))
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.delete(delete);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
  }

  @Test
  public void validate_PrevHashTampered_ShouldReturnWithPrevHashValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // tamper with prev_hash (tampering with only prev_hash is not very meaningful test though)
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toPrevHashValue("tamperd".getBytes(StandardCharsets.UTF_8)))
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_PREV_HASH);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_OutputTampered_ShouldReturnWithOutputValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // maliciously update the output field of the latest asset of A
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toOutputValue("{\"balance\":7000}")) // instead of 700
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_InputTampered_ShouldReturnWithOutputValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // maliciously update the input field of the latest asset of A
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toInputValue("{\"A\":{\"age\":2},\"B\":{\"age\":1}}"))
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_ContractArgumentTampered_ShouldReturnWithContractValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // maliciously update the argument of the contract
    JsonObject argument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2).build())
            .add(AMOUNT_ATTRIBUTE_NAME, 0)
            .add(Argument.NONCE_KEY_NAME, UUID.randomUUID().toString())
            .build();
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toArgumentValue(argument.toString()))
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_CONTRACT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  @DisabledIf(
      value = "isTxStateManagementEnabled",
      disabledReason =
          "When tx_state_management is enabled, ScalarDL does not allow transactions to be committed using the same nonce.")
  public void validate_AssetAContainsDuplicateNonce_ShouldReturnFalseWithNonceValidatorError() {
    // Arrange
    createAssets(Optional.of(UUID.randomUUID()), DeserializationType.DEPRECATED, false);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_ASSET_ID_2));

    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_NONCE);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  @EnabledIf("isTxStateManagementEnabled")
  public void validate_ExecuteContractWithDuplicateNonce_ShouldThrowConflictException() {
    // Arrange Act Assert
    Assertions.assertThatThrownBy(
            () ->
                createAssets(
                    Optional.of(UUID.randomUUID()), DeserializationType.JACKSON_JSON, false))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  public void
      validate_OutputTamperedAndPartiallyValidatedIncludingTampered_ShouldReturnWithOutputValidatorError()
          throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // maliciously update the output field of the latest asset of A
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toOutputValue("{\"balance\":7000}")) // instead of 700
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(
            prepareValidationRequest(SOME_ASSET_ID_1, 1, 2, ENTITY_ID_A, dsSigner1));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
  }

  @Test
  public void validate_OutputTamperedAndPartiallyValidatedNotIncludingTampered_ShouldReturnWithOK()
      throws ExecutionException {
    // Arrange
    createAssets(Optional.empty(), DeserializationType.DEPRECATED, false);
    // maliciously update the output field of the latest asset of A
    Put put =
        new Put(new Key(AssetAttribute.ID, SOME_ASSET_ID_1), new Key(AssetAttribute.AGE, 2))
            .withValue(AssetAttribute.toOutputValue("{\"balance\":7000}")) // instead of 700
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    storageService.put(put);

    // Act
    // age 2 is tampered but it is not included in the range
    LedgerValidationResult resultA =
        validationService.validate(
            prepareValidationRequest(SOME_ASSET_ID_1, 0, 1, ENTITY_ID_A, dsSigner1));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AuditorEnabledAndLedgerValidationRequestGiven_ShouldThrowLedgerException() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUDITOR_ENABLED, "true");
    props2.put(LedgerConfig.AUDITOR_CERT_HOLDER_ID, AUDITOR_ENTITY_ID);
    props2.put(LedgerConfig.AUDITOR_CERT_VERSION, KEY_VERSION);
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    createServices(new LedgerConfig(props2));

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                validationService.validate(
                    prepareValidationRequest(SOME_ASSET_ID_1, 0, 1, ENTITY_ID_A, dsSigner1)));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.INVALID_REQUEST);
  }

  @Test
  public void execute_SameContractRegisteredWithDifferentCert_ShouldReturnProperCert() {
    // Arrange
    ContractExecutionRequest requestA =
        prepareRequestForHolderChecker(UUID.randomUUID(), ENTITY_ID_A, dsSigner1);
    ContractExecutionRequest requestB =
        prepareRequestForHolderChecker(UUID.randomUUID(), ENTITY_ID_B, dsSigner2);

    // Act
    ContractExecutionResult resultA = ledgerService.execute(requestA);
    ContractExecutionResult resultB = ledgerService.execute(requestB);

    // Assert
    assertThat(resultA.getResult().get().getString("holder")).isEqualTo(ENTITY_ID_A);
    assertThat(resultB.getResult().get().getString("holder")).isEqualTo(ENTITY_ID_B);
  }

  @Test
  public void execute_UnregisteredContractSpecified_ShouldThrowMissingContractException() {
    // Arrange
    ContractExecutionRequest requestA =
        prepareRequestForCreateBasedOnDeprecated(
            SOME_ASSET_ID_1, SOME_AMOUNT_1, UUID.randomUUID(), ENTITY_ID_A, dsSigner1, false);
    ContractExecutionRequest requestB =
        prepareRequestForCreateBasedOnDeprecated(
            SOME_ASSET_ID_2, SOME_AMOUNT_1, UUID.randomUUID(), ENTITY_ID_B, dsSigner2, false);

    // Act
    ContractExecutionResult resultA = ledgerService.execute(requestA);
    assertThatThrownBy(() -> ledgerService.execute(requestB))
        .isInstanceOf(MissingContractException.class);

    // Assert
    assertThat(resultA.getResult()).isEmpty();
  }

  @Test
  public void execute_ContractSpecifiedWithWrongSignature_ShouldThrowSignatureException() {
    // Arrange
    ContractExecutionRequest request =
        prepareRequestForCreateBasedOnDeprecated(
            SOME_ASSET_ID_1, SOME_AMOUNT_1, UUID.randomUUID(), ENTITY_ID_A, dsSigner2, false);

    // Act Assert
    assertThatThrownBy(() -> ledgerService.execute(request)).isInstanceOf(SignatureException.class);
  }

  @Test
  public void execute_CreateContractGiven_ShouldPutNewAssetEntry() throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual =
        jsonpSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateContractWithV2ArgumentGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID1, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual =
        jsonpSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateBasedOnJsonpContractGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID2, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            contractArgument.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual =
        jsonpSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateBasedOnJsonpContractWithV2ArgumentGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID2, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual =
        jsonpSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateBasedOnJacksonContractGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce);
    String argument = jacksonSerDe.serialize(contractArgument);

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual =
        jacksonSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateBasedOnJacksonContractWithV2ArgumentGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual =
        jacksonSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateBasedOnStringContractWithV2ArgumentGiven_ShouldPutNewAssetEntry()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    // StringBasedContract only supports V2 argument format
    String argument =
        Argument.format(SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID4, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID4,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    String actual = result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_ContractAndFunctionGiven_ShouldExecuteThemTogether()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, functionNamespace)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument.toString(),
            Collections.singletonList(CreateFunction.class.getName()),
            functionArgument.toString(),
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void execute_ContractAndFunctionWithV2ArgumentGiven_ShouldExecuteThemTogether()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, functionNamespace)
            .build();
    List<String> functionIds = Collections.singletonList(CreateFunction.class.getName());
    String argument = Argument.format(contractArgument, nonce, functionIds);

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID1, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            argument,
            functionIds,
            functionArgument.toString(),
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void execute_ContractAndFunctionBasedOnJsonpGiven_ShouldExecuteThemTogether()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, functionNamespace)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID2, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            contractArgument.toString(),
            Collections.singletonList(CreateFunctionWithJsonp.class.getName()),
            functionArgument.toString(),
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void execute_ContractAndFunctionBasedOnJsonpWithV2ArgumentGiven_ShouldExecuteThemTogether()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, functionNamespace)
            .build();
    List<String> functionIds = Collections.singletonList(CreateFunctionWithJsonp.class.getName());
    String argument = Argument.format(contractArgument, nonce, functionIds);

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID2, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            argument,
            functionIds,
            functionArgument.toString(),
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void execute_ContractAndFunctionBasedOnJacksonGiven_ShouldExecuteThemTogether()
      throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce);
    JsonNode functionArgument =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(NAMESPACE_ATTRIBUTE_NAME, functionNamespace);
    String contractArgumentString = jacksonSerDe.serialize(contractArgument);
    String functionArgumentString = jacksonSerDe.serialize(functionArgument);

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID3, contractArgumentString, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            contractArgumentString,
            Collections.singletonList(CreateFunctionWithJackson.class.getName()),
            functionArgumentString,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void
      execute_ContractAndFunctionBasedOnJacksonWithV2ArgumentGiven_ShouldExecuteThemTogether()
          throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce);
    JsonNode functionArgument =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(NAMESPACE_ATTRIBUTE_NAME, functionNamespace);
    String functionArgumentString = jacksonSerDe.serialize(functionArgument);
    List<String> functionIds = Collections.singletonList(CreateFunctionWithJackson.class.getName());
    String argument = Argument.format(contractArgument, nonce, functionIds);

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.singletonList(CreateFunctionWithJackson.class.getName()),
            functionArgumentString,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void
      execute_ContractAndFunctionBasedOnStringWithV2ArgumentGiven_ShouldExecuteThemTogether()
          throws TransactionException {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    List<String> functionIds = Collections.singletonList(CreateFunctionWithString.class.getName());
    String argument = Argument.format(SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1, nonce, functionIds);
    String functionArgument = SOME_ID + "," + SOME_AMOUNT_1 + "," + functionNamespace;

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID4, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID4,
            argument,
            Collections.singletonList(CreateFunctionWithString.class.getName()),
            functionArgument,
            dsSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .forNamespace(functionNamespace)
            .forTable(FUNCTION_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  public void
      execute_ContractAndFunctionGivenButFunctionArgumentNotGiven_ShouldThrowContractContextException() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument.toString(),
            Collections.singletonList(CreateFunction.class.getName()),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(ContractContextException.class);
  }

  @Test
  public void execute_FunctionTwiceWithPutButWithoutGet_ShouldPutRecordAndUpdateItCorrectly()
      throws TransactionException {
    // Arrange
    String nonce1 = UUID.randomUUID().toString();
    String nonce2 = UUID.randomUUID().toString();
    JsonNode contractArgument1 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce1);
    JsonNode contractArgument2 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .put(Argument.NONCE_KEY_NAME, nonce2);
    JsonNode functionArgument1 =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(NAMESPACE_ATTRIBUTE_NAME, functionNamespace);
    JsonNode functionArgument2 =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .put(NAMESPACE_ATTRIBUTE_NAME, functionNamespace);
    List<String> functionIds = Collections.singletonList(CreateFunctionWithJackson.class.getName());
    String argument1 = Argument.format(contractArgument1, nonce1, functionIds);
    String argument2 = Argument.format(contractArgument2, nonce2, functionIds);
    byte[] serialized1 =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID3, argument1, ENTITY_ID_A, KEY_VERSION);
    byte[] serialized2 =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID3, argument2, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request1 =
        new ContractExecutionRequest(
            nonce1,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument1,
            Collections.singletonList(CreateFunctionWithJackson.class.getName()),
            jacksonSerDe.serialize(functionArgument1),
            dsSigner1.sign(serialized1),
            null);
    ContractExecutionRequest request2 =
        new ContractExecutionRequest(
            nonce2,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument2,
            Collections.singletonList(CreateFunctionWithJackson.class.getName()),
            jacksonSerDe.serialize(functionArgument2),
            dsSigner1.sign(serialized2),
            null);

    // Act
    ledgerService.execute(request1);
    ledgerService.execute(request2);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(functionNamespace)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID))
            .build();
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> functionResult = transaction.get(get);
    transaction.commit();
    assertThat(functionResult.isPresent()).isTrue();
    assertThat(functionResult.get().getValue(BALANCE_ATTRIBUTE_NAME).get().getAsInt())
        .isEqualTo(SOME_AMOUNT_2);
  }

  @Test
  public void execute_CreateAndNestedGetBalanceContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceContractsWithV2ArgumentGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID1, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceBasedOnJsonpContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(Argument.NONCE_KEY_NAME, nonce)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID2, contractArgument.toString(), ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            contractArgument.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceBasedOnJsonpContractsWithV2ArgumentGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID2, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID2,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceBasedOnJacksonContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce)
            .put(EXECUTE_NESTED_ATTRIBUTE_NAME, true);
    String contractArgumentString = jacksonSerDe.serialize(contractArgument);

    byte[] serialized =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID3, contractArgumentString, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            contractArgumentString,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    assertThat(jacksonSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceBasedOnJacksonContractsWithV2ArgumentGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(EXECUTE_NESTED_ATTRIBUTE_NAME, true);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    assertThat(jacksonSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      execute_CreateAndNestedGetBalanceBasedOnStringContractsWithV2ArgumentGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String nonce = UUID.randomUUID().toString();
    String argument =
        Argument.format(
            SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1 + ",true", nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID4, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID4,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    assertThat(actual.getContractResult().get()).isEqualTo(expected);
  }

  @Test
  public void
      execute_ProofEnabledAndCreateBasedOnJacksonContractWithV2ArgumentGiven_ShouldPutNewAssetEntry() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            null);
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_B);

    // Act
    ContractExecutionResult result = ledgerService.execute(request);

    // Assert
    assertThat(result.getLedgerProofs().size()).isEqualTo(1);
    AssetProof proof = result.getLedgerProofs().get(0);
    byte[] toBeValidated =
        AssetProof.serialize(
            proof.getId(),
            proof.getAge(),
            proof.getNonce(),
            proof.getInput(),
            proof.getHash(),
            proof.getPrevHash());
    assertThat(validator.validate(toBeValidated, proof.getSignature())).isTrue();
  }

  @Test
  public void execute_AuditorEnabledAndValidAuditorDigitalSignatureGiven_ShouldExecuteProperly() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUDITOR_ENABLED, "true");
    props2.put(LedgerConfig.AUDITOR_CERT_HOLDER_ID, AUDITOR_ENTITY_ID);
    props2.put(LedgerConfig.AUDITOR_CERT_VERSION, KEY_VERSION);
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            dsSigner2.sign(nonce.getBytes(StandardCharsets.UTF_8)));

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      execute_AuditorEnabledAndInvalidAuditorDigitalSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUDITOR_ENABLED, "true");
    props2.put(LedgerConfig.AUDITOR_CERT_HOLDER_ID, AUDITOR_ENTITY_ID);
    props2.put(LedgerConfig.AUDITOR_CERT_VERSION, KEY_VERSION);
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            dsSigner1.sign(nonce.getBytes(StandardCharsets.UTF_8))); // invalid signature

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(SignatureException.class);
  }

  /** Execute selected tests With HMAC */
  @Test
  public void execute_HmacConfiguredAndValidHmacSignatureGiven_ShouldExecuteProperly()
      throws TransactionException {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_C, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_C,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            hmacSigner1.sign(serialized),
            null);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        new Get(new Key(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1), new Key(ASSET_AGE_COLUMN_NAME, 0))
            .withConsistency(Consistency.SEQUENTIAL)
            .forNamespace(namespace)
            .forTable(ASSET_TABLE);
    DistributedTransaction transaction = transactionService.start();
    Optional<Result> result = transaction.get(get);
    transaction.commit();
    assertThat(result.isPresent()).isTrue();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual =
        jacksonSerDe.deserialize(
            result.get().getValue(ASSET_OUTPUT_COLUMN_NAME).get().getAsString().get());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_HmacConfiguredAndInvalidHmacSignatureGiven_ShouldExecuteProperly() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_C, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_C,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            hmacSigner2.sign(serialized),
            null);

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(SignatureException.class);
  }

  @Test
  public void execute_AuditorEnabledAndValidAuditorHmacSignatureGiven_ShouldExecuteProperly() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUDITOR_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SECRET_KEY_A);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            hmacSigner1.sign(nonce.getBytes(StandardCharsets.UTF_8)));

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      execute_AuditorEnabledAndInvalidAuditorHmacSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUDITOR_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SECRET_KEY_A);
    createServices(new LedgerConfig(props2));
    String nonce = UUID.randomUUID().toString();
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(CREATE_CONTRACT_ID3, argument, ENTITY_ID_A, KEY_VERSION);
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            nonce,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID3,
            argument,
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized),
            hmacSigner2.sign(nonce.getBytes(StandardCharsets.UTF_8))); // invalid HMAC signature

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(SignatureException.class);
  }

  @Test
  public void validate_HmacConfiguredAndValidHmacSignatureGiven_ShouldReturnTrue() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    createAssets(
        Optional.empty(), DeserializationType.JACKSON_JSON, true, ENTITY_ID_C, hmacSigner1);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(
            prepareValidationRequest(
                SOME_ASSET_ID_1, 0, Integer.MAX_VALUE, ENTITY_ID_C, hmacSigner1));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_HmacConfiguredAndInvalidHmacSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    createAssets(
        Optional.empty(), DeserializationType.JACKSON_JSON, true, ENTITY_ID_C, hmacSigner1);

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                validationService.validate(
                    prepareValidationRequest(
                        SOME_ASSET_ID_1, 0, Integer.MAX_VALUE, ENTITY_ID_C, hmacSigner2)));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(SignatureException.class);
  }

  @Test
  public void execute_CreateContractGivenTwice_ShouldPutNewAssetEntryAndUpdateItWithoutGet()
      throws TransactionException {
    // Arrange
    String nonce1 = UUID.randomUUID().toString();
    String nonce2 = UUID.randomUUID().toString();
    JsonNode contractArgument1 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(Argument.NONCE_KEY_NAME, nonce1);
    JsonNode contractArgument2 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .put(Argument.NONCE_KEY_NAME, nonce2);

    byte[] serialized1 =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument1.toString(), ENTITY_ID_A, KEY_VERSION);
    byte[] serialized2 =
        ContractExecutionRequest.serialize(
            CREATE_CONTRACT_ID1, contractArgument2.toString(), ENTITY_ID_A, KEY_VERSION);

    ContractExecutionRequest request1 =
        new ContractExecutionRequest(
            nonce1,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument1.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized1),
            null);
    ContractExecutionRequest request2 =
        new ContractExecutionRequest(
            nonce2,
            ENTITY_ID_A,
            KEY_VERSION,
            CREATE_CONTRACT_ID1,
            contractArgument2.toString(),
            Collections.emptyList(),
            null,
            dsSigner1.sign(serialized2),
            null);

    JsonNode expected1 = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode expected2 = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_2);

    // Act
    ledgerService.execute(request1);
    ledgerService.execute(request2);

    // Assert
    DistributedTransaction transaction = transactionService.start();
    Scan scan =
        Scan.newBuilder()
            .namespace(namespace)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .build();
    List<Result> results = transaction.scan(scan);
    transaction.commit();
    assertThat(results.size()).isEqualTo(2);
    assertThat(results.get(0).getInt(ASSET_AGE_COLUMN_NAME)).isEqualTo(0);
    assertThat(results.get(1).getInt(ASSET_AGE_COLUMN_NAME)).isEqualTo(1);
    assertThat(jacksonSerDe.deserialize(results.get(0).getText(ASSET_OUTPUT_COLUMN_NAME)))
        .isEqualTo(expected1);
    assertThat(jacksonSerDe.deserialize(results.get(1).getText(ASSET_OUTPUT_COLUMN_NAME)))
        .isEqualTo(expected2);
  }
}
