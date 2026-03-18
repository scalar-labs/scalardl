package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CONTRACT_ID_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID2;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID3;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID4;
import static com.scalar.dl.testing.contract.Constants.CREATE_FUNCTION_ID1;
import static com.scalar.dl.testing.contract.Constants.CREATE_FUNCTION_ID2;
import static com.scalar.dl.testing.contract.Constants.CREATE_FUNCTION_ID3;
import static com.scalar.dl.testing.contract.Constants.CREATE_FUNCTION_ID4;
import static com.scalar.dl.testing.contract.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.GET_BALANCE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.GET_BALANCE_CONTRACT_ID2;
import static com.scalar.dl.testing.contract.Constants.GET_BALANCE_CONTRACT_ID3;
import static com.scalar.dl.testing.contract.Constants.GET_BALANCE_CONTRACT_ID4;
import static com.scalar.dl.testing.contract.Constants.HOLDER_CHECKER_CONTRACT_ID;
import static com.scalar.dl.testing.contract.Constants.ID_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID2;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID3;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID4;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_METADATA_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TABLE_METADATA;
import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.io.Key;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.TransactionFactory;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import com.scalar.dl.testing.config.TransactionMode;
import com.scalar.dl.testing.container.AbstractTestCluster;
import com.scalar.dl.testing.container.LedgerContainer;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.contract.Create;
import com.scalar.dl.testing.contract.CreateWithJackson;
import com.scalar.dl.testing.contract.CreateWithJsonp;
import com.scalar.dl.testing.contract.CreateWithString;
import com.scalar.dl.testing.contract.GetBalance;
import com.scalar.dl.testing.contract.GetBalanceWithJackson;
import com.scalar.dl.testing.contract.GetBalanceWithJsonp;
import com.scalar.dl.testing.contract.GetBalanceWithString;
import com.scalar.dl.testing.contract.HolderChecker;
import com.scalar.dl.testing.contract.Payment;
import com.scalar.dl.testing.contract.PaymentWithJackson;
import com.scalar.dl.testing.contract.PaymentWithJsonp;
import com.scalar.dl.testing.contract.PaymentWithString;
import com.scalar.dl.testing.function.CreateFunction;
import com.scalar.dl.testing.function.CreateFunctionWithJackson;
import com.scalar.dl.testing.function.CreateFunctionWithJsonp;
import com.scalar.dl.testing.function.CreateFunctionWithString;
import com.scalar.dl.testing.schema.TestSchemas;
import com.scalar.dl.testing.util.TestCertificates;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Ledger integration tests. Provides cluster lifecycle management and ClientService
 * creation.
 *
 * <p>Subclasses should override {@link #getAuthenticationMethod()} to configure the test
 * environment. Storage type is determined from system properties (default: jdbc with MySQL
 * container).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LedgerIntegrationTestBase {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final ImmutableMap<String, Class<?>> CONTRACTS =
      ImmutableMap.<String, Class<?>>builder()
          .put(CREATE_CONTRACT_ID1, Create.class)
          .put(CREATE_CONTRACT_ID2, CreateWithJsonp.class)
          .put(CREATE_CONTRACT_ID3, CreateWithJackson.class)
          .put(CREATE_CONTRACT_ID4, CreateWithString.class)
          .put(PAYMENT_CONTRACT_ID1, Payment.class)
          .put(PAYMENT_CONTRACT_ID2, PaymentWithJsonp.class)
          .put(PAYMENT_CONTRACT_ID3, PaymentWithJackson.class)
          .put(PAYMENT_CONTRACT_ID4, PaymentWithString.class)
          .put(GET_BALANCE_CONTRACT_ID1, GetBalance.class)
          .put(GET_BALANCE_CONTRACT_ID2, GetBalanceWithJsonp.class)
          .put(GET_BALANCE_CONTRACT_ID3, GetBalanceWithJackson.class)
          .put(GET_BALANCE_CONTRACT_ID4, GetBalanceWithString.class)
          .put(HOLDER_CHECKER_CONTRACT_ID, HolderChecker.class)
          .build();
  private static final ImmutableMap<String, Class<?>> FUNCTIONS =
      ImmutableMap.<String, Class<?>>builder()
          .put(CREATE_FUNCTION_ID1, CreateFunction.class)
          .put(CREATE_FUNCTION_ID2, CreateFunctionWithJsonp.class)
          .put(CREATE_FUNCTION_ID3, CreateFunctionWithJackson.class)
          .put(CREATE_FUNCTION_ID4, CreateFunctionWithString.class)
          .build();

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);

  // Contract properties for nested contract execution
  private static final ImmutableMap<String, String> CONTRACT_PROPERTIES =
      ImmutableMap.<String, String>builder()
          .put(
              CREATE_CONTRACT_ID1,
              Json.createObjectBuilder()
                  .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID1)
                  .build()
                  .toString())
          .put(
              CREATE_CONTRACT_ID2,
              Json.createObjectBuilder()
                  .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID2)
                  .build()
                  .toString())
          .put(
              CREATE_CONTRACT_ID3,
              jacksonSerDe.serialize(
                  mapper.createObjectNode().put(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID3)))
          .put(CREATE_CONTRACT_ID4, CREATE_CONTRACT_ID4)
          .put(
              GET_BALANCE_CONTRACT_ID1,
              Json.createObjectBuilder()
                  .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID1)
                  .build()
                  .toString())
          .put(
              GET_BALANCE_CONTRACT_ID2,
              Json.createObjectBuilder()
                  .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID2)
                  .build()
                  .toString())
          .put(
              GET_BALANCE_CONTRACT_ID3,
              jacksonSerDe.serialize(
                  mapper
                      .createObjectNode()
                      .put(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID3)))
          .put(GET_BALANCE_CONTRACT_ID4, GET_BALANCE_CONTRACT_ID4)
          .build();

  // Test data (protected for use in subclasses)
  protected static final String SOME_ASSET_ID_1 = "A";
  protected static final String SOME_ASSET_ID_2 = "B";
  protected static final int SOME_AMOUNT_1 = 1000;
  protected static final int SOME_AMOUNT_2 = 100;
  protected static final int SOME_AMOUNT_3 = 10;
  private static final String SOME_ID_1 = "id1";

  protected AbstractTestCluster cluster;

  protected static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  protected static final ClientServiceFactory clientServiceFactory = new ClientServiceFactory();
  protected ClientService clientServiceA;
  protected ClientService clientServiceB;
  protected DistributedStorage storage;
  protected DistributedStorageAdmin storageAdmin;
  protected DistributedTransactionAdmin transactionAdmin;

  /**
   * Returns the authentication method for this test.
   *
   * @return The AuthenticationMethod to use
   */
  protected abstract AuthenticationMethod getAuthenticationMethod();

  /**
   * Returns the transaction mode for this test.
   *
   * @return The TransactionMode to use
   */
  protected TransactionMode getTransactionMode() {
    return TransactionMode.CONSENSUS_COMMIT;
  }

  /**
   * Returns the Ledger Docker image to use. Override to use a custom image.
   *
   * @return The Docker image tag
   */
  protected String getLedgerImage() {
    return System.getProperty("scalardl.ledger.image", LedgerContainer.DEFAULT_IMAGE);
  }

  /**
   * Returns the physical namespace for asset tables. Override in subclasses that use a context
   * namespace.
   *
   * @return The physical namespace for asset tables
   */
  protected String getPhysicalNamespace() {
    return SCALAR_NAMESPACE;
  }

  /**
   * Returns the context namespace for client configuration. Override in subclasses that use a
   * non-default context namespace.
   *
   * @return The context namespace
   */
  protected String getContextNamespace() {
    return Namespaces.DEFAULT;
  }

  @BeforeAll
  void setUpCluster() throws Exception {
    logger.info(
        "Setting up cluster: authenticationMethod={}, transactionMode={}",
        getAuthenticationMethod(),
        getTransactionMode());

    cluster =
        new LedgerTestCluster(getAuthenticationMethod(), getTransactionMode(), getLedgerImage());
    cluster.start();

    setUpAfterClusterStart();
  }

  /**
   * Called after the cluster has started. Override to perform additional setup such as registering
   * contracts.
   */
  protected void setUpAfterClusterStart() throws Exception {
    createStorage();
    createSchema();
    createFunctionTableSchema();
    createClientServices();
    registerContracts();
    registerFunctions();
  }

  @AfterEach
  void truncateTables() throws Exception {
    storageAdmin.truncateTable(getPhysicalNamespace(), ASSET_TABLE);
    storageAdmin.truncateTable(getPhysicalNamespace(), ASSET_METADATA_TABLE);
    transactionAdmin.truncateTable(FUNCTION_NAMESPACE, FUNCTION_TABLE);
  }

  @AfterAll
  void tearDownCluster() {
    logger.info("Tearing down cluster");

    if (transactionAdmin != null) {
      try {
        transactionAdmin.dropTable(FUNCTION_NAMESPACE, FUNCTION_TABLE);
        transactionAdmin.dropNamespace(FUNCTION_NAMESPACE);
      } catch (Exception e) {
        logger.warn("Failed to drop function table", e);
      }
      transactionAdmin.close();
    }

    if (storageAdmin != null) {
      storageAdmin.close();
    }

    if (storage != null) {
      storage.close();
    }

    if (cluster != null) {
      try {
        Properties props = cluster.getStorageConfig().getPropertiesForHost();
        SchemaLoader.unload(props, TestSchemas.getLedgerSchema(), true);
      } catch (Exception e) {
        logger.warn("Failed to unload table", e);
      }

      cluster.close();
    }
  }

  /**
   * Returns the ClientService for entity A.
   *
   * @return The ClientService for entity A
   */
  protected ClientService getClientServiceA() {
    return clientServiceA;
  }

  /**
   * Returns the ClientService for entity B.
   *
   * @return The ClientService for entity B
   */
  protected ClientService getClientServiceB() {
    return clientServiceB;
  }

  protected void createStorage() {
    Properties props = cluster.getStorageConfig().getPropertiesForHost();
    StorageFactory storageFactory = StorageFactory.create(props);
    storage = storageFactory.getStorage();
    storageAdmin = storageFactory.getStorageAdmin();
    TransactionFactory transactionFactory = TransactionFactory.create(props);
    transactionAdmin = transactionFactory.getTransactionAdmin();
  }

  protected void createSchema() throws SchemaLoaderException {
    Properties props = cluster.getStorageConfig().getPropertiesForHost();
    SchemaLoader.load(props, TestSchemas.getLedgerSchema(), Collections.emptyMap(), true);
  }

  protected void createFunctionTableSchema() throws Exception {
    transactionAdmin.createNamespace(FUNCTION_NAMESPACE, true);
    transactionAdmin.createTable(FUNCTION_NAMESPACE, FUNCTION_TABLE, FUNCTION_TABLE_METADATA, true);
  }

  protected ClientConfig getDigitalSignatureClientConfig(
      String entityId, String privateKey, String certificate) throws IOException {
    return getDigitalSignatureClientConfig(
        entityId, privateKey, certificate, getAuthenticationMethod(), getContextNamespace());
  }

  protected ClientConfig getDigitalSignatureClientConfig(
      String entityId, String privateKey, String certificate, AuthenticationMethod authMethod)
      throws IOException {
    return getDigitalSignatureClientConfig(
        entityId, privateKey, certificate, authMethod, getContextNamespace());
  }

  protected ClientConfig getDigitalSignatureClientConfig(
      String entityId,
      String privateKey,
      String certificate,
      AuthenticationMethod authMethod,
      String contextNamespace)
      throws IOException {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    props.setProperty(ClientConfig.AUTHENTICATION_METHOD, authMethod.getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, entityId);
    props.setProperty(ClientConfig.DS_PRIVATE_KEY_PEM, privateKey);
    props.setProperty(ClientConfig.DS_CERT_PEM, certificate);
    props.setProperty(ClientConfig.DS_CERT_VERSION, "1");
    props.setProperty(ClientConfig.CONTEXT_NAMESPACE, contextNamespace);
    return new ClientConfig(props);
  }

  protected ClientConfig getHmacClientConfig(String entityId, String secretKey) throws IOException {
    return getHmacClientConfig(
        entityId, secretKey, getAuthenticationMethod(), getContextNamespace());
  }

  protected ClientConfig getHmacClientConfig(
      String entityId, String secretKey, AuthenticationMethod authMethod) throws IOException {
    return getHmacClientConfig(entityId, secretKey, authMethod, getContextNamespace());
  }

  protected ClientConfig getHmacClientConfig(
      String entityId, String secretKey, AuthenticationMethod authMethod, String contextNamespace)
      throws IOException {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    props.setProperty(ClientConfig.AUTHENTICATION_METHOD, authMethod.getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, entityId);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY, secretKey);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY_VERSION, "1");
    props.setProperty(ClientConfig.CONTEXT_NAMESPACE, contextNamespace);
    return new ClientConfig(props);
  }

  protected void createClientServices() throws IOException {
    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      clientServiceA =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_A,
                  TestCertificates.PRIVATE_KEY_A,
                  TestCertificates.CERTIFICATE_A));
      clientServiceB =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_B,
                  TestCertificates.PRIVATE_KEY_B,
                  TestCertificates.CERTIFICATE_B));
    } else {
      clientServiceA =
          clientServiceFactory.create(
              getHmacClientConfig(TestCertificates.ENTITY_ID_A, TestCertificates.SECRET_KEY_A));
      clientServiceB =
          clientServiceFactory.create(
              getHmacClientConfig(TestCertificates.ENTITY_ID_B, TestCertificates.SECRET_KEY_B));
    }
  }

  protected void registerContracts() {
    // Register contracts for both entity A and B
    registerContractsFor(clientServiceA);
    registerContractsFor(clientServiceB);
  }

  protected void registerContractsFor(ClientService clientService) {
    for (Map.Entry<String, Class<?>> entry : CONTRACTS.entrySet()) {
      String contractId = entry.getKey();
      Class<?> clazz = entry.getValue();
      String contractBinaryName = clazz.getName();
      byte[] bytes = Common.getClassBytes(clazz);
      String properties = CONTRACT_PROPERTIES.get(contractId);
      clientService.registerContract(contractId, contractBinaryName, bytes, properties);
    }
  }

  protected void registerFunctions() {
    // Register functions with clientServiceA (functions are shared)
    registerFunctionsFor(clientServiceA);
  }

  protected void registerFunctionsFor(ClientService clientService) {
    for (Map.Entry<String, Class<?>> entry : FUNCTIONS.entrySet()) {
      String functionId = entry.getKey();
      Class<?> clazz = entry.getValue();
      String functionBinaryName = clazz.getName();
      byte[] bytes = Common.getClassBytes(clazz);
      clientService.registerFunction(functionId, functionBinaryName, bytes);
    }
  }

  // ============ Test Cases ============

  @Test
  void executeContract_SameContractRegisteredWithDifferentCert_ShouldReturnProperCert() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().build();

    // Act
    JsonObject resultA =
        clientServiceA.executeContract(HOLDER_CHECKER_CONTRACT_ID, argument).getResult().get();
    JsonObject resultB =
        clientServiceB.executeContract(HOLDER_CHECKER_CONTRACT_ID, argument).getResult().get();

    // Assert
    assertThat(resultA.getString("holder")).isEqualTo(TestCertificates.ENTITY_ID_A);
    assertThat(resultB.getString("holder")).isEqualTo(TestCertificates.ENTITY_ID_B);
  }

  @Test
  void
      executeContract_UnregisteredContractSpecified_ShouldThrowClientExceptionWithContractNotFound() {
    // Arrange
    String unregisteredContractId = "unregistered-contract-id";
    JsonObject argument = Json.createObjectBuilder().build();

    // Act & Assert
    assertThatThrownBy(() -> clientServiceA.executeContract(unregisteredContractId, argument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.CONTRACT_NOT_FOUND));
  }

  @Test
  void
      executeContract_ContractSpecifiedWithWrongSignature_ShouldThrowClientExceptionWithInvalidSignature()
          throws IOException {
    // Arrange: Create a client with entity A's identity but entity B's signing key
    ClientService wrongSignatureClient;
    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      // Use entity A's ID and certificate, but entity B's private key
      wrongSignatureClient =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_A,
                  TestCertificates.PRIVATE_KEY_B, // Wrong key!
                  TestCertificates.CERTIFICATE_A));
    } else {
      // Use entity A's ID, but entity B's secret key
      wrongSignatureClient =
          clientServiceFactory.create(
              getHmacClientConfig(
                  TestCertificates.ENTITY_ID_A, TestCertificates.SECRET_KEY_B)); // Wrong key!
    }

    JsonObject argument = Json.createObjectBuilder().build();

    // Act & Assert
    assertThatThrownBy(
            () -> wrongSignatureClient.executeContract(HOLDER_CHECKER_CONTRACT_ID, argument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_SIGNATURE));
  }

  @Test
  void executeContract_CreateContractGiven_ShouldPutNewAssetEntry() throws Exception {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();

    // Act
    clientServiceA.executeContract(CREATE_CONTRACT_ID1, argument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual = jsonpSerDe.deserialize(result.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void executeContract_CreateBasedOnJsonpContractGiven_ShouldPutNewAssetEntry() throws Exception {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();

    // Act
    clientServiceA.executeContract(CREATE_CONTRACT_ID2, argument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject actual = jsonpSerDe.deserialize(result.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void executeContract_CreateBasedOnJacksonContractGiven_ShouldPutNewAssetEntry() throws Exception {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();

    // Act
    clientServiceA.executeContract(CREATE_CONTRACT_ID3, argument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual = jacksonSerDe.deserialize(result.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void executeContract_CreateBasedOnStringContractGiven_ShouldPutNewAssetEntry() throws Exception {
    // Arrange
    String argument = SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1;

    // Act
    clientServiceA.executeContract(CREATE_CONTRACT_ID4, argument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    String actual = result.get().getText(ASSET_OUTPUT_COLUMN_NAME);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void executeContract_ContractAndFunctionGiven_ShouldExecuteThemTogether() throws Exception {
    // Arrange
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID_1)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE)
            .build();

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID1, contractArgument, CREATE_FUNCTION_ID1, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID_1))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void executeContract_ContractAndFunctionBasedOnJsonpGiven_ShouldExecuteThemTogether()
      throws Exception {
    // Arrange
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_ID_1)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE)
            .build();

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID2, contractArgument, CREATE_FUNCTION_ID2, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID_1))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void executeContract_ContractAndFunctionBasedOnJacksonGiven_ShouldExecuteThemTogether()
      throws Exception {
    // Arrange
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode functionArgument =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID_1)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE);

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID3, contractArgument, CREATE_FUNCTION_ID3, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID_1))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void executeContract_ContractAndFunctionBasedOnStringGiven_ShouldExecuteThemTogether()
      throws Exception {
    // Arrange
    String contractArgument = SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1;
    String functionArgument = SOME_ID_1 + "," + SOME_AMOUNT_1 + "," + FUNCTION_NAMESPACE;

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID4, contractArgument, CREATE_FUNCTION_ID4, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID_1))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void
      executeContract_ContractAndFunctionGivenButFunctionArgumentNotGiven_ShouldThrowClientExceptionWithContractContextualError() {
    // Arrange
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();

    // Act & Assert
    assertThatThrownBy(
            () ->
                clientServiceA.executeContract(
                    CREATE_CONTRACT_ID1, contractArgument, CREATE_FUNCTION_ID1, null))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.CONTRACT_CONTEXTUAL_ERROR));
  }

  @Test
  void executeContract_FunctionTwiceWithPutButWithoutGet_ShouldPutRecordAndUpdateItCorrectly()
      throws Exception {
    // Arrange
    JsonNode contractArgument1 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode contractArgument2 =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2);
    JsonNode functionArgument1 =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID_1)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE);
    JsonNode functionArgument2 =
        mapper
            .createObjectNode()
            .put(ID_ATTRIBUTE_NAME, SOME_ID_1)
            .put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .put(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE);

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID3, contractArgument1, CREATE_FUNCTION_ID3, functionArgument1);
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID3, contractArgument2, CREATE_FUNCTION_ID3, functionArgument2);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_ID_1))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_2);
  }

  @Test
  void executeContract_CreateAndNestedGetBalanceContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();

    // Act
    JsonObject actual =
        clientServiceA.executeContract(CREATE_CONTRACT_ID1, contractArgument).getResult().get();

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void
      executeContract_CreateAndNestedGetBalanceBasedOnJsonpContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, true)
            .build();

    // Act
    String resultString =
        clientServiceA
            .executeContract(CREATE_CONTRACT_ID2, contractArgument)
            .getContractResult()
            .get();

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(resultString)).isEqualTo(expected);
  }

  @Test
  void
      executeContract_CreateAndNestedGetBalanceBasedOnJacksonContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    JsonNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .put(EXECUTE_NESTED_ATTRIBUTE_NAME, true);

    // Act
    String resultString =
        clientServiceA
            .executeContract(CREATE_CONTRACT_ID3, contractArgument)
            .getContractResult()
            .get();

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    assertThat(jacksonSerDe.deserialize(resultString)).isEqualTo(expected);
  }

  @Test
  void
      executeContract_CreateAndNestedGetBalanceBasedOnStringContractsGiven_ShouldPutNewAssetEntryAndGet() {
    // Arrange
    String contractArgument = SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1 + ",true";

    // Act
    String actual =
        clientServiceA
            .executeContract(CREATE_CONTRACT_ID4, contractArgument)
            .getContractResult()
            .get();

    // Assert
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void executeContract_MismatchedAuthenticationMethod_ShouldThrowClientException()
      throws IOException {
    // Arrange: Create a client with the opposite authentication method using the same entity ID.
    // The entity is already registered with the server's authentication method via bootstrap.
    // When a client with a different authentication method sends a request,
    // the signature format is incompatible, resulting in INVALID_SIGNATURE.
    ClientService mismatchedClient;

    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      // DS server + HMAC client (same entity ID registered with DS)
      mismatchedClient =
          clientServiceFactory.create(
              getHmacClientConfig(
                  TestCertificates.ENTITY_ID_A,
                  TestCertificates.SECRET_KEY_A,
                  AuthenticationMethod.HMAC),
              false);
    } else {
      // HMAC server + DS client (same entity ID registered with HMAC)
      mismatchedClient =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_A,
                  TestCertificates.PRIVATE_KEY_A,
                  TestCertificates.CERTIFICATE_A,
                  AuthenticationMethod.DIGITAL_SIGNATURE),
              false);
    }

    JsonObject argument = Json.createObjectBuilder().build();

    // Act & Assert
    assertThatThrownBy(() -> mismatchedClient.executeContract(HOLDER_CHECKER_CONTRACT_ID, argument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_SIGNATURE));
  }

  @Test
  void executeContract_MismatchedAuthenticationMethodWithNewEntity_ShouldThrowClientException()
      throws IOException {
    // Arrange: Create a client with the opposite authentication method using a new entity ID.
    // Bootstrap registers the client's credential (which doesn't match server's authentication
    // method), then try to execute a contract.
    // The server will look for a credential matching its authentication method, so it will fail.
    String newEntityId = "mismatched_entity";
    ClientService mismatchedClient;
    StatusCode expectedStatusCode;

    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      // DS server + HMAC client: bootstrap registers secret, but server looks for certificate
      mismatchedClient =
          clientServiceFactory.create(
              getHmacClientConfig(
                  newEntityId, TestCertificates.SECRET_KEY_A, AuthenticationMethod.HMAC));
      expectedStatusCode = StatusCode.CERTIFICATE_NOT_FOUND;
    } else {
      // HMAC server + DS client: bootstrap registers certificate, but server looks for secret
      mismatchedClient =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  newEntityId,
                  TestCertificates.PRIVATE_KEY_A,
                  TestCertificates.CERTIFICATE_A,
                  AuthenticationMethod.DIGITAL_SIGNATURE));
      expectedStatusCode = StatusCode.SECRET_NOT_FOUND;
    }

    JsonObject argument = Json.createObjectBuilder().build();

    // Act & Assert: Contract execution should fail because server looks for the wrong credential
    // type
    assertThatThrownBy(() -> mismatchedClient.executeContract(HOLDER_CHECKER_CONTRACT_ID, argument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(expectedStatusCode));
  }

  @Test
  void executeContract_CreateContractGivenTwice_ShouldPutNewAssetEntryAndUpdateItWithoutGet()
      throws Exception {
    // Arrange
    JsonObject argument1 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject argument2 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .build();

    // Act
    clientServiceA.executeContract(CREATE_CONTRACT_ID1, argument1);
    clientServiceA.executeContract(CREATE_CONTRACT_ID1, argument2);

    // Assert: Two entries should be created with age 0 and 1
    Scan scan =
        Scan.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .build();

    List<Result> results;
    try (Scanner scanner = storage.scan(scan)) {
      results = scanner.all();
    }
    assertThat(results).hasSize(2);

    assertThat(results.get(0).getInt(ASSET_AGE_COLUMN_NAME)).isEqualTo(0);
    assertThat(results.get(1).getInt(ASSET_AGE_COLUMN_NAME)).isEqualTo(1);

    JsonObject expected1 =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    JsonObject expected2 =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_2).build();

    assertThat(jsonpSerDe.deserialize(results.get(0).getText(ASSET_OUTPUT_COLUMN_NAME)))
        .isEqualTo(expected1);
    assertThat(jsonpSerDe.deserialize(results.get(1).getText(ASSET_OUTPUT_COLUMN_NAME)))
        .isEqualTo(expected2);
  }

  // ============ Validate Ledger Test Cases ============

  @Test
  void validateLedger_AssetsCreatedWithDeprecatedContractAndNothingTampered_ShouldReturnOk() {
    // Arrange: Create two assets and perform two payments using deprecated (Contract) API
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_AssetsCreatedWithJsonpContractAndNothingTampered_ShouldReturnOk() {
    // Arrange: Create two assets and perform two payments using JSON-P API
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID2, PAYMENT_CONTRACT_ID2);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_AssetsCreatedWithJacksonContractAndNothingTampered_ShouldReturnOk() {
    // Arrange: Create two assets and perform two payments using Jackson API
    createAssetsWithPaymentsJackson();

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_AssetsCreatedWithJacksonContractInParallel_ShouldReturnOk() {
    // Arrange: Create two assets and perform two payments using Jackson API
    createAssetsWithPaymentsJackson();

    // Act & Assert: Validate both assets in parallel
    List<String> assetIds = Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2);
    assetIds.parallelStream()
        .forEach(
            assetId -> {
              LedgerValidationResult result = clientServiceA.validateLedger(assetId);
              assertThat(result.getCode()).isEqualTo(StatusCode.OK);
            });
  }

  @Test
  void
      validateLedger_AssetValidatedWithWrongSignature_ShouldThrowClientExceptionWithInvalidSignature()
          throws Exception {
    // Arrange: Create assets with clientServiceA
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Create a client with entity A's identity but entity B's signing key
    ClientService wrongSignatureClient;
    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      // Use entity A's ID and certificate, but entity B's private key
      wrongSignatureClient =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_A,
                  TestCertificates.PRIVATE_KEY_B, // Wrong key!
                  TestCertificates.CERTIFICATE_A));
    } else {
      // Use entity A's ID, but entity B's secret key
      wrongSignatureClient =
          clientServiceFactory.create(
              getHmacClientConfig(
                  TestCertificates.ENTITY_ID_A, TestCertificates.SECRET_KEY_B)); // Wrong key!
    }

    // Act & Assert: Validate should fail due to invalid signature
    assertThatThrownBy(() -> wrongSignatureClient.validateLedger(SOME_ASSET_ID_1))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_SIGNATURE));
  }

  // ============ Helper Methods for Asset Creation ============

  /**
   * Helper method to create two assets and perform two payment transactions using JSON-P based
   * contracts (deprecated Contract API or JsonpBasedContract).
   *
   * @param createContractId the contract ID to use for asset creation
   * @param paymentContractId the contract ID to use for payment
   */
  protected void createAssetsWithPaymentsJsonp(String createContractId, String paymentContractId) {
    // Create two assets with initial balance
    JsonObject createArg1 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject createArg2 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_2)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    clientServiceA.executeContract(createContractId, createArg1);
    clientServiceA.executeContract(createContractId, createArg2);

    // Perform two payments from asset1 to asset2
    JsonObject paymentArg1 =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .build();
    JsonObject paymentArg2 =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_3)
            .build();
    clientServiceA.executeContract(paymentContractId, paymentArg1);
    clientServiceA.executeContract(paymentContractId, paymentArg2);
  }

  /** Helper method to create two assets and perform two payment transactions using Jackson API. */
  protected void createAssetsWithPaymentsJackson() {
    // Create two assets with initial balance
    ObjectNode createArg1 = mapper.createObjectNode();
    createArg1.put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1);
    createArg1.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);

    ObjectNode createArg2 = mapper.createObjectNode();
    createArg2.put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_2);
    createArg2.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);

    clientServiceA.executeContract(CREATE_CONTRACT_ID3, createArg1);
    clientServiceA.executeContract(CREATE_CONTRACT_ID3, createArg2);

    // Perform two payments from asset1 to asset2
    ObjectNode payment1 = mapper.createObjectNode();
    payment1.putArray(ASSETS_ATTRIBUTE_NAME).add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2);
    payment1.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2);

    ObjectNode payment2 = mapper.createObjectNode();
    payment2.putArray(ASSETS_ATTRIBUTE_NAME).add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2);
    payment2.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_3);

    clientServiceA.executeContract(PAYMENT_CONTRACT_ID3, payment1);
    clientServiceA.executeContract(PAYMENT_CONTRACT_ID3, payment2);
  }

  /** Helper method to create two assets and perform two payment transactions using String API. */
  protected void createAssetsWithPaymentsString() {
    // Create two assets with initial balance
    // Format: <asset_id>,<amount>
    String createArg1 = SOME_ASSET_ID_1 + "," + SOME_AMOUNT_1;
    String createArg2 = SOME_ASSET_ID_2 + "," + SOME_AMOUNT_1;
    clientServiceA.executeContract(CREATE_CONTRACT_ID4, createArg1);
    clientServiceA.executeContract(CREATE_CONTRACT_ID4, createArg2);

    // Perform two payments from asset1 to asset2
    // Format: <from_asset_id>,<to_asset_id>,<amount>
    String paymentArg1 = SOME_ASSET_ID_1 + "," + SOME_ASSET_ID_2 + "," + SOME_AMOUNT_2;
    String paymentArg2 = SOME_ASSET_ID_1 + "," + SOME_ASSET_ID_2 + "," + SOME_AMOUNT_3;
    clientServiceA.executeContract(PAYMENT_CONTRACT_ID4, paymentArg1);
    clientServiceA.executeContract(PAYMENT_CONTRACT_ID4, paymentArg2);
  }
}
