package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ID_SEPARATOR;
import static com.scalar.dl.testing.contract.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_AWARE_CREATE_ID;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_AWARE_GET_BALANCE_ID;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_AWARE_GET_HISTORY_ID;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_AWARE_PAYMENT_ID;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_METADATA_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.resolveNamespace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.Result;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.service.StorageFactory;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.testing.container.AbstractTestCluster;
import com.scalar.dl.testing.container.LedgerContainer;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.contract.NamespaceAwareCreate;
import com.scalar.dl.testing.contract.NamespaceAwareGetBalance;
import com.scalar.dl.testing.contract.NamespaceAwareGetHistory;
import com.scalar.dl.testing.contract.NamespaceAwarePayment;
import com.scalar.dl.testing.schema.TestSchemas;
import com.scalar.dl.testing.util.TestCertificates;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for namespace integration tests in the default context.
 *
 * <p>This class tests namespace-specific functionality using the default context, including
 * namespace management (create, drop, list), contract execution targeting specific namespaces, and
 * cross-namespace operations (e.g., payments between different namespaces). It also validates
 * namespace-aware assets.
 *
 * <p>Subclasses should override {@link #getAuthenticationMethod()} to configure the authentication
 * method.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LedgerNamespaceIntegrationTestBase {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected static final String NAMESPACE_1 = "namespace1";
  protected static final String NAMESPACE_2 = "namespace2";
  protected static final String NAMESPACE_3 = "namespace3";

  protected static final String SOME_ASSET_ID_1 = "A";
  protected static final String SOME_ASSET_ID_2 = "B";
  protected static final int SOME_AMOUNT_1 = 1000;
  protected static final int SOME_AMOUNT_2 = 100;
  protected static final int SOME_AMOUNT_3 = 200;

  protected static final ObjectMapper mapper = new ObjectMapper();
  protected static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  protected static final ClientServiceFactory clientServiceFactory = new ClientServiceFactory();

  protected AbstractTestCluster cluster;
  protected ClientService clientServiceA;
  protected ClientService clientServiceB;
  protected DistributedStorage storage;
  protected DistributedStorageAdmin storageAdmin;

  /**
   * Returns the authentication method for this test.
   *
   * @return The AuthenticationMethod to use
   */
  protected abstract AuthenticationMethod getAuthenticationMethod();

  /**
   * Returns the Ledger Docker image to use. Override to use a custom image.
   *
   * @return The Docker image tag
   */
  protected String getLedgerImage() {
    return System.getProperty("scalardl.ledger.image", LedgerContainer.DEFAULT_IMAGE);
  }

  @BeforeAll
  void setUpCluster() throws Exception {
    logger.info(
        "Setting up Namespace test cluster: authenticationMethod={}", getAuthenticationMethod());

    cluster =
        new LedgerTestCluster(
            getAuthenticationMethod(),
            com.scalar.dl.testing.config.TransactionMode.CONSENSUS_COMMIT,
            getLedgerImage());
    cluster.start();

    createStorage();
    createSchema();
    createClientServices();
    createNamespaces();
    registerNamespaceAwareContracts();
  }

  private void createStorage() throws Exception {
    Properties storageProps = cluster.getStorageConfig().getPropertiesForHost();
    StorageFactory factory = StorageFactory.create(storageProps);
    storage = factory.getStorage();
    storageAdmin = factory.getStorageAdmin();
  }

  private void createSchema() throws Exception {
    Properties props = cluster.getStorageConfig().getPropertiesForHost();
    SchemaLoader.load(props, TestSchemas.getLedgerSchema(), java.util.Collections.emptyMap(), true);
  }

  private void createClientServices() throws java.io.IOException {
    AuthenticationMethod authMethod = getAuthenticationMethod();
    if (authMethod == AuthenticationMethod.DIGITAL_SIGNATURE) {
      clientServiceA =
          createDigitalSignatureClientService(
              TestCertificates.ENTITY_ID_A,
              TestCertificates.PRIVATE_KEY_A,
              TestCertificates.CERTIFICATE_A);
      clientServiceB =
          createDigitalSignatureClientService(
              TestCertificates.ENTITY_ID_B,
              TestCertificates.PRIVATE_KEY_B,
              TestCertificates.CERTIFICATE_B);
    } else {
      clientServiceA =
          createHmacClientService(TestCertificates.ENTITY_ID_A, TestCertificates.SECRET_KEY_A);
      clientServiceB =
          createHmacClientService(TestCertificates.ENTITY_ID_B, TestCertificates.SECRET_KEY_B);
    }
    clientServiceA.bootstrap();
    clientServiceB.bootstrap();
  }

  private ClientService createDigitalSignatureClientService(
      String entityId, String privateKey, String certificate) throws java.io.IOException {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    props.setProperty(
        ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, entityId);
    props.setProperty(ClientConfig.DS_PRIVATE_KEY_PEM, privateKey);
    props.setProperty(ClientConfig.DS_CERT_PEM, certificate);
    props.setProperty(ClientConfig.DS_CERT_VERSION, "1");
    return clientServiceFactory.create(new ClientConfig(props));
  }

  private ClientService createHmacClientService(String entityId, String secretKey)
      throws java.io.IOException {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    props.setProperty(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, entityId);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY, secretKey);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY_VERSION, "1");
    return clientServiceFactory.create(new ClientConfig(props));
  }

  protected void createNamespaces() {
    clientServiceA.createNamespace(NAMESPACE_1);
    clientServiceA.createNamespace(NAMESPACE_2);
  }

  protected void registerNamespaceAwareContracts() {
    registerContract(clientServiceA, NAMESPACE_AWARE_CREATE_ID, NamespaceAwareCreate.class);
    registerContract(
        clientServiceA, NAMESPACE_AWARE_GET_BALANCE_ID, NamespaceAwareGetBalance.class);
    registerContract(
        clientServiceA, NAMESPACE_AWARE_GET_HISTORY_ID, NamespaceAwareGetHistory.class);
    registerContract(clientServiceA, NAMESPACE_AWARE_PAYMENT_ID, NamespaceAwarePayment.class);

    registerContract(clientServiceB, NAMESPACE_AWARE_CREATE_ID, NamespaceAwareCreate.class);
    registerContract(
        clientServiceB, NAMESPACE_AWARE_GET_BALANCE_ID, NamespaceAwareGetBalance.class);
    registerContract(
        clientServiceB, NAMESPACE_AWARE_GET_HISTORY_ID, NamespaceAwareGetHistory.class);
    registerContract(clientServiceB, NAMESPACE_AWARE_PAYMENT_ID, NamespaceAwarePayment.class);
  }

  private void registerContract(ClientService service, String contractId, Class<?> clazz) {
    try {
      service.registerContract(contractId, clazz.getName(), Common.getClassBytes(clazz));
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CONTRACT_ALREADY_REGISTERED)) {
        throw e;
      }
    }
  }

  @AfterEach
  void truncateNamespaceTables() throws Exception {
    truncateNamespaceTables(NAMESPACE_1);
    truncateNamespaceTables(NAMESPACE_2);
  }

  private void truncateNamespaceTables(String namespace) throws ExecutionException {
    storageAdmin.truncateTable(resolveNamespace(SCALAR_NAMESPACE, namespace), ASSET_TABLE);
    storageAdmin.truncateTable(resolveNamespace(SCALAR_NAMESPACE, namespace), ASSET_METADATA_TABLE);
  }

  @AfterAll
  void tearDownCluster() {
    logger.info("Tearing down Namespace test cluster");

    try {
      clientServiceA.dropNamespace(NAMESPACE_1);
      clientServiceA.dropNamespace(NAMESPACE_2);
    } catch (Exception e) {
      logger.warn("Failed to drop namespace", e);
    }

    if (storageAdmin != null) {
      try {
        SchemaLoader.unload(
            cluster.getStorageConfig().getPropertiesForHost(), TestSchemas.getLedgerSchema(), true);
      } catch (Exception e) {
        logger.warn("Failed to unload schema", e);
      }
      storageAdmin.close();
    }
    if (storage != null) {
      storage.close();
    }
    if (cluster != null) {
      cluster.close();
    }
  }

  // ============ Helper Methods ============

  protected ObjectNode createArgument(String namespace, String assetId, int amount) {
    return mapper
        .createObjectNode()
        .put(ASSET_ATTRIBUTE_NAME, namespace + ASSET_ID_SEPARATOR + assetId)
        .put(AMOUNT_ATTRIBUTE_NAME, amount);
  }

  protected ObjectNode getBalanceArgument(String namespace, String assetId) {
    return mapper
        .createObjectNode()
        .put(ASSET_ATTRIBUTE_NAME, namespace + ASSET_ID_SEPARATOR + assetId);
  }

  protected ObjectNode paymentArgument(
      String namespaceFrom, String assetIdFrom, String namespaceTo, String assetIdTo, int amount) {
    return mapper
        .createObjectNode()
        .put(AMOUNT_ATTRIBUTE_NAME, amount)
        .set(
            ASSETS_ATTRIBUTE_NAME,
            mapper
                .createArrayNode()
                .add(namespaceFrom + ASSET_ID_SEPARATOR + assetIdFrom)
                .add(namespaceTo + ASSET_ID_SEPARATOR + assetIdTo));
  }

  protected void createAssetsWithPaymentsInNamespaces() {
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_PAYMENT_ID,
        paymentArgument(NAMESPACE_1, SOME_ASSET_ID_1, NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_2));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_PAYMENT_ID,
        paymentArgument(NAMESPACE_1, SOME_ASSET_ID_1, NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_3));
  }

  // ============ Contract Execution Tests ============

  @Test
  void executeContract_CreateWithNamespaceGiven_ShouldCreateNewAccount() throws Exception {
    // Arrange
    ObjectNode argument = createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1);

    // Act
    ContractExecutionResult executionResult =
        clientServiceA.executeContract(NAMESPACE_AWARE_CREATE_ID, argument);

    // Assert: Verify database state
    Get get =
        Get.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual = jacksonSerDe.deserialize(result.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);

    // Assert: Verify proof contains namespace
    assertThat(executionResult.getLedgerProofs()).hasSize(1);
    AssetProof proof = executionResult.getLedgerProofs().get(0);
    assertThat(proof.getNamespace()).isEqualTo(NAMESPACE_1);
    assertThat(proof.getId()).isEqualTo(SOME_ASSET_ID_1);
  }

  @Test
  void executeContract_CreateWithNonExistingNamespaceGiven_ShouldThrowException() {
    // Arrange
    ObjectNode argument = createArgument("nonexistent", SOME_ASSET_ID_1, SOME_AMOUNT_1);

    // Act & Assert
    assertThatThrownBy(() -> clientServiceA.executeContract(NAMESPACE_AWARE_CREATE_ID, argument))
        .isInstanceOf(ClientException.class)
        .satisfies(
            e ->
                assertThat(((ClientException) e).getStatusCode())
                    .isEqualTo(StatusCode.NAMESPACE_NOT_FOUND));
  }

  @Test
  void executeContract_GetBalanceWithNamespaceGiven_ShouldGetAccountBalance() {
    // Arrange
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1));
    ObjectNode argument = getBalanceArgument(NAMESPACE_1, SOME_ASSET_ID_1);

    // Act
    ContractExecutionResult result =
        clientServiceA.executeContract(NAMESPACE_AWARE_GET_BALANCE_ID, argument);

    // Assert
    assertThat(result.getContractResult()).isPresent();
    JsonNode actual = jacksonSerDe.deserialize(result.getContractResult().get());
    assertThat(actual.get(BALANCE_ATTRIBUTE_NAME).asInt()).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void executeContract_GetBalanceWithNonExistingNamespaceGiven_ShouldThrowException() {
    // Arrange
    ObjectNode argument = getBalanceArgument("nonexistent", SOME_ASSET_ID_1);

    // Act & Assert
    assertThatThrownBy(
            () -> clientServiceA.executeContract(NAMESPACE_AWARE_GET_BALANCE_ID, argument))
        .isInstanceOf(ClientException.class)
        .satisfies(
            e ->
                assertThat(((ClientException) e).getStatusCode())
                    .isEqualTo(StatusCode.NAMESPACE_NOT_FOUND));
  }

  @Test
  void executeContract_PaymentWithCrossNamespace_ShouldPaidCorrectly() {
    // Arrange
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_1));

    // Act
    clientServiceA.executeContract(
        NAMESPACE_AWARE_PAYMENT_ID,
        paymentArgument(NAMESPACE_1, SOME_ASSET_ID_1, NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_2));

    // Assert
    ContractExecutionResult fromResult =
        clientServiceA.executeContract(
            NAMESPACE_AWARE_GET_BALANCE_ID, getBalanceArgument(NAMESPACE_1, SOME_ASSET_ID_1));
    JsonNode fromBalance = jacksonSerDe.deserialize(fromResult.getContractResult().get());
    assertThat(fromBalance.get(BALANCE_ATTRIBUTE_NAME).asInt())
        .isEqualTo(SOME_AMOUNT_1 - SOME_AMOUNT_2);

    ContractExecutionResult toResult =
        clientServiceA.executeContract(
            NAMESPACE_AWARE_GET_BALANCE_ID, getBalanceArgument(NAMESPACE_2, SOME_ASSET_ID_2));
    JsonNode toBalance = jacksonSerDe.deserialize(toResult.getContractResult().get());
    assertThat(toBalance.get(BALANCE_ATTRIBUTE_NAME).asInt())
        .isEqualTo(SOME_AMOUNT_1 + SOME_AMOUNT_2);
  }

  @Test
  void executeContract_GetHistoryWithNamespace_ShouldReturnHistoryAndProof() {
    // Arrange
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_PAYMENT_ID,
        paymentArgument(NAMESPACE_1, SOME_ASSET_ID_1, NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_2));
    ObjectNode argument = getBalanceArgument(NAMESPACE_1, SOME_ASSET_ID_1);

    // Act
    ContractExecutionResult result =
        clientServiceA.executeContract(NAMESPACE_AWARE_GET_HISTORY_ID, argument);

    // Assert
    assertThat(result.getContractResult()).isPresent();
    assertThat(result.getLedgerProofs()).hasSize(1);
    AssetProof proof = result.getLedgerProofs().get(0);
    assertThat(proof.getNamespace()).isEqualTo(NAMESPACE_1);
    assertThat(proof.getId()).isEqualTo(SOME_ASSET_ID_1);
    assertThat(proof.getAge()).isEqualTo(1);
  }

  // ============ Ledger Validation Tests (Normal Cases) ============

  @Test
  void validateLedger_NothingTamperedWithNamespace_ShouldReturnOk() {
    // Arrange
    createAssetsWithPaymentsInNamespaces();

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_CrossNamespacePayment_ShouldReturnOkForBothNamespaces() {
    // Arrange
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_1, SOME_ASSET_ID_1, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_CREATE_ID, createArgument(NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_1));
    clientServiceA.executeContract(
        NAMESPACE_AWARE_PAYMENT_ID,
        paymentArgument(NAMESPACE_1, SOME_ASSET_ID_1, NAMESPACE_2, SOME_ASSET_ID_2, SOME_AMOUNT_2));

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  // ============ Namespace Management Tests ============

  @Test
  void listNamespaces_WithoutFilter_ShouldReturnAllNamespaces() {
    // Act
    String result = clientServiceA.listNamespaces();

    // Assert
    assertThat(result).contains("default");
    assertThat(result).contains(NAMESPACE_1);
    assertThat(result).contains(NAMESPACE_2);
  }

  @Test
  void listNamespaces_WithFilter_ShouldReturnMatchingNamespaces() {
    // Act
    String result = clientServiceA.listNamespaces(NAMESPACE_1);

    // Assert
    assertThat(result).contains(NAMESPACE_1);
    assertThat(result).doesNotContain(NAMESPACE_2);
  }

  @Test
  void dropNamespace_ValidNamespace_ShouldDropSuccessfully() {
    // Arrange
    clientServiceA.createNamespace(NAMESPACE_3);

    // Act
    clientServiceA.dropNamespace(NAMESPACE_3);

    // Assert
    String result = clientServiceA.listNamespaces(NAMESPACE_3);
    assertThat(result).doesNotContain(NAMESPACE_3);
  }

  @Test
  void dropNamespace_DefaultNamespace_ShouldThrowException() {
    // Act & Assert
    assertThatThrownBy(() -> clientServiceA.dropNamespace("default"))
        .isInstanceOf(ClientException.class)
        .satisfies(
            e ->
                assertThat(((ClientException) e).getStatusCode())
                    .isEqualTo(StatusCode.INVALID_ARGUMENT));
  }
}
