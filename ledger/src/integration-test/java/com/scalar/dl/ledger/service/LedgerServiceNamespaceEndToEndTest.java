package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.service.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ID_SEPARATOR;
import static com.scalar.dl.ledger.service.Constants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_A;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_C;
import static com.scalar.dl.ledger.service.Constants.KEY_VERSION;
import static com.scalar.dl.ledger.service.Constants.NAMESPACE_AWARE_CREATE_ID;
import static com.scalar.dl.ledger.service.Constants.NAMESPACE_AWARE_GET_BALANCE_ID;
import static com.scalar.dl.ledger.service.Constants.NAMESPACE_AWARE_PAYMENT_ID;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_1;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_2;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_3;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_1;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_2;
import static com.scalar.dl.ledger.service.Constants.SOME_CIPHER_KEY;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_B;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_B;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.database.scalardb.AssetAttribute;
import com.scalar.dl.ledger.database.scalardb.ScalarNamespaceResolver;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationRequest;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.model.NamespaceCreationRequest;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LedgerServiceNamespaceEndToEndTest extends LedgerServiceEndToEndTestBase {
  private static final String SOME_NAMESPACE1 = "namespace1";
  private static final String SOME_NAMESPACE2 = "namespace2";
  private static final ImmutableList<String> TABLES =
      ImmutableList.of(
          "asset",
          "asset_metadata",
          "certificate",
          "secret",
          "contract",
          "contract_class",
          "function");
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();

  private ScalarNamespaceResolver resolver;

  @Override
  protected void setUpBeforeClassPerTestInstance() {
    resolver = new ScalarNamespaceResolver(ledgerConfig);
    createServices(ledgerConfig);
    createNamespace(SOME_NAMESPACE1);
    createNamespace(SOME_NAMESPACE2);
  }

  @Override
  protected void tearDownAfterClassPerTestInstance() {
    try {
      dropNamespace(SOME_NAMESPACE1);
      dropNamespace(SOME_NAMESPACE2);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void tearDownAfterEach() {
    try {
      storageAdmin.truncateTable(resolver.resolve(SOME_NAMESPACE1), ASSET_TABLE);
      storageAdmin.truncateTable(resolver.resolve(SOME_NAMESPACE1), ASSET_METADATA_TABLE);
      storageAdmin.truncateTable(resolver.resolve(SOME_NAMESPACE2), ASSET_TABLE);
      storageAdmin.truncateTable(resolver.resolve(SOME_NAMESPACE2), ASSET_METADATA_TABLE);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Map<String, Map<String, String>> getContractsMap() {
    Map<String, String> contractsMap =
        ImmutableMap.<String, String>builder()
            .put(
                CONTRACT_PACKAGE_NAME + NAMESPACE_AWARE_CREATE_ID,
                CONTRACT_CLASS_DIR + NAMESPACE_AWARE_CREATE_ID + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + NAMESPACE_AWARE_GET_BALANCE_ID,
                CONTRACT_CLASS_DIR + NAMESPACE_AWARE_GET_BALANCE_ID + ".class")
            .put(
                CONTRACT_PACKAGE_NAME + NAMESPACE_AWARE_PAYMENT_ID,
                CONTRACT_CLASS_DIR + NAMESPACE_AWARE_PAYMENT_ID + ".class")
            .build();
    return ImmutableMap.of(ENTITY_ID_A, contractsMap, ENTITY_ID_C, contractsMap);
  }

  @Override
  protected Map<String, Map<String, String>> getContractPropertiesMap() {
    return ImmutableMap.of();
  }

  @Override
  protected Map<String, String> getFunctionsMap() {
    return ImmutableMap.of();
  }

  @Override
  protected Properties createProperties() {
    Properties properties = super.createProperties();
    properties.put(LedgerConfig.PROOF_ENABLED, "true");
    properties.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    return properties;
  }

  private void createNamespace(String namespace) {
    NamespaceCreationRequest request = new NamespaceCreationRequest(namespace);
    ledgerService.create(request);
  }

  private void dropNamespace(String namespace) throws ExecutionException {
    if (storageAdmin != null && resolver != null) {
      for (String table : TABLES) {
        storageAdmin.dropTable(resolver.resolve(namespace), table);
      }
      storageAdmin.dropNamespace(resolver.resolve(namespace));
    }
  }

  private ContractExecutionRequest prepareRequestForCreate(
      String namespace, String assetId, int amount, String entityId) {
    String nonce = UUID.randomUUID().toString();
    String contractId = NAMESPACE_AWARE_CREATE_ID;
    ObjectNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, namespace + ASSET_ID_SEPARATOR + assetId)
            .put(AMOUNT_ATTRIBUTE_NAME, amount);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce,
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signers.get(entityId).sign(serialized),
        null);
  }

  private ContractExecutionRequest prepareRequestForGetBalance(
      String namespace, String assetId, String entityId) {
    String nonce = UUID.randomUUID().toString();
    String contractId = NAMESPACE_AWARE_GET_BALANCE_ID;
    ObjectNode contractArgument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, namespace + ASSET_ID_SEPARATOR + assetId);
    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());

    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce,
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signers.get(entityId).sign(serialized),
        null);
  }

  private static ContractExecutionRequest prepareRequestForPayment(
      String namespaceFrom,
      String assetIdFrom,
      String namespaceTo,
      String assetIdTo,
      int amount,
      String entityId) {
    String nonce = UUID.randomUUID().toString();
    String contractId = NAMESPACE_AWARE_PAYMENT_ID;
    ObjectNode contractArgument =
        mapper
            .createObjectNode()
            .put(AMOUNT_ATTRIBUTE_NAME, amount)
            .set(
                ASSETS_ATTRIBUTE_NAME,
                mapper
                    .createArrayNode()
                    .add(namespaceFrom + ASSET_ID_SEPARATOR + assetIdFrom)
                    .add(namespaceTo + ASSET_ID_SEPARATOR + assetIdTo));

    String argument = Argument.format(contractArgument, nonce, Collections.emptyList());
    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, argument, entityId, KEY_VERSION);
    return new ContractExecutionRequest(
        nonce,
        entityId,
        KEY_VERSION,
        contractId,
        argument,
        Collections.emptyList(),
        null,
        signers.get(entityId).sign(serialized),
        null);
  }

  private static LedgerValidationRequest prepareValidationRequest(
      String namespace, String assetId) {
    return prepareValidationRequest(namespace, assetId, 0, Integer.MAX_VALUE, ENTITY_ID_A);
  }

  private static LedgerValidationRequest prepareValidationRequest(
      String namespace, String assetId, int startAge, int endAge, String entityId) {
    byte[] serialized =
        LedgerValidationRequest.serialize(
            namespace, assetId, startAge, endAge, entityId, KEY_VERSION);
    return new LedgerValidationRequest(
        namespace,
        assetId,
        startAge,
        endAge,
        entityId,
        KEY_VERSION,
        signers.get(entityId).sign(serialized));
  }

  private void createAssets(String entityId) {
    ledgerService.execute(
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, entityId));
    ledgerService.execute(
        prepareRequestForCreate(SOME_NAMESPACE2, SOME_ASSET_ID_2, SOME_AMOUNT_1, entityId));
    ledgerService.execute(
        prepareRequestForPayment(
            SOME_NAMESPACE1,
            SOME_ASSET_ID_1,
            SOME_NAMESPACE2,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_2,
            entityId));
    ledgerService.execute(
        prepareRequestForPayment(
            SOME_NAMESPACE1,
            SOME_ASSET_ID_1,
            SOME_NAMESPACE2,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_3,
            entityId));
  }

  @Test
  public void execute_CreateWithNamespaceGiven_ShouldCreateNewAccount() throws ExecutionException {
    // Arrange
    ContractExecutionRequest request =
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_A);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> result = storageService.get(get);
    assertThat(result.isPresent()).isTrue();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual = jacksonSerDe.deserialize(result.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void execute_CreateWithNonExistingNamespaceGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request =
        prepareRequestForCreate("ns", SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_A);

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void execute_GetBalanceWithNamespaceGiven_ShouldGetAccountBalance() {
    // Arrange
    ledgerService.execute(
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_A));
    ContractExecutionRequest request =
        prepareRequestForGetBalance(SOME_NAMESPACE1, SOME_ASSET_ID_1, ENTITY_ID_A);

    // Act
    ContractExecutionResult actual = ledgerService.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(jsonpSerDe.deserialize(actual.getContractResult().get())).isEqualTo(expected);
  }

  @Test
  public void execute_GetBalanceWithNonExistingNamespaceGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request =
        prepareRequestForGetBalance("ns", SOME_ASSET_ID_1, ENTITY_ID_A);

    // Act
    Throwable thrown = catchThrowable(() -> ledgerService.execute(request));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void execute_PaymentWithCrossNamespaceArgumentsGiven_ShouldPaidCorrectly() {
    // Arrange
    ledgerService.execute(
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_A));
    ledgerService.execute(
        prepareRequestForCreate(SOME_NAMESPACE2, SOME_ASSET_ID_2, SOME_AMOUNT_1, ENTITY_ID_A));
    ContractExecutionRequest request =
        prepareRequestForPayment(
            SOME_NAMESPACE1,
            SOME_ASSET_ID_1,
            SOME_NAMESPACE2,
            SOME_ASSET_ID_2,
            SOME_AMOUNT_2,
            ENTITY_ID_A);
    JsonObject fromExpected =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2)
            .build();
    JsonObject toExpected =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2)
            .build();

    // Act
    ledgerService.execute(request);

    // Assert
    ContractExecutionResult from =
        ledgerService.execute(
            prepareRequestForGetBalance(SOME_NAMESPACE1, SOME_ASSET_ID_1, ENTITY_ID_A));
    assertThat(jsonpSerDe.deserialize(from.getContractResult().get())).isEqualTo(fromExpected);
    ContractExecutionResult to =
        ledgerService.execute(
            prepareRequestForGetBalance(SOME_NAMESPACE2, SOME_ASSET_ID_2, ENTITY_ID_A));
    assertThat(jsonpSerDe.deserialize(to.getContractResult().get())).isEqualTo(toExpected);
  }

  @Test
  public void
      execute_ProofEnabledAndCreateWithNamespaceGiven_ShouldReturnProofWithGivenNamespace() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.PROOF_ENABLED, "true");
    props2.put(LedgerConfig.PROOF_PRIVATE_KEY_PEM, PRIVATE_KEY_B);
    createServices(new LedgerConfig(props2));
    ContractExecutionRequest request =
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_A);
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_B);

    // Act
    ContractExecutionResult result = ledgerService.execute(request);

    // Assert
    assertThat(result.getLedgerProofs().size()).isEqualTo(1);
    AssetProof proof = result.getLedgerProofs().get(0);
    byte[] toBeValidated =
        AssetProof.serialize(
            SOME_NAMESPACE1,
            proof.getId(),
            proof.getAge(),
            proof.getNonce(),
            proof.getInput(),
            proof.getHash(),
            proof.getPrevHash());
    assertThat(validator.validate(toBeValidated, proof.getSignature())).isTrue();
  }

  @Test
  public void
      execute_HmacConfiguredAndCreateWithNamespaceGiven_ShouldReturnProofWithGivenNamespace()
          throws ExecutionException {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    ContractExecutionRequest request =
        prepareRequestForCreate(SOME_NAMESPACE1, SOME_ASSET_ID_1, SOME_AMOUNT_1, ENTITY_ID_C);

    // Act
    ledgerService.execute(request);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 0))
            .build();
    Optional<Result> record = storageService.get(get);
    assertThat(record.isPresent()).isTrue();
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    JsonNode actual = jacksonSerDe.deserialize(record.get().getText(ASSET_OUTPUT_COLUMN_NAME));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void validate_NothingTampered_ShouldReturnTrue() {
    // Arrange
    createAssets(ENTITY_ID_A);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_HmacConfiguredAndNothingTampered_ShouldReturnTrue() {
    // Arrange
    Properties props2 = createProperties();
    props2.put(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props2.put(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    createServices(new LedgerConfig(props2));
    createAssets(ENTITY_ID_C);

    // Act
    LedgerValidationResult result =
        validationService.validate(
            prepareValidationRequest(
                SOME_NAMESPACE1, SOME_ASSET_ID_1, 0, Integer.MAX_VALUE, ENTITY_ID_C));

    // Assert
    assertThat(result.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_MiddleRecordRemoved_ShouldReturnWithInconsistentStateValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(ENTITY_ID_A);
    // remove the middle record maliciously
    Delete delete =
        Delete.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(AssetAttribute.AGE, 1))
            .build();
    storageService.delete(delete);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
  }

  @Test
  public void validate_PrevHashTampered_ShouldReturnWithPrevHashValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(ENTITY_ID_A);
    // tamper with prev_hash
    Put put =
        Put.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(AssetAttribute.AGE, 2))
            .blobValue(AssetAttribute.PREV_HASH, "tampered".getBytes(StandardCharsets.UTF_8))
            .build();
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_PREV_HASH);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_OutputTampered_ShouldReturnWithOutputValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(ENTITY_ID_A);
    // maliciously update the output field of the latest asset of A
    Put put =
        Put.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(AssetAttribute.AGE, 2))
            .textValue(AssetAttribute.OUTPUT, "{\"balance\":7000}") // instead of 700
            .build();
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_InputTampered_ShouldReturnWithOutputValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(ENTITY_ID_A);
    // maliciously update the input field of the latest asset of A
    // Use V2 format with namespace but tamper the age values (correct age is 1, not 2)
    String tamperedInput =
        "{\""
            + SOME_NAMESPACE1
            + "\":{\""
            + SOME_ASSET_ID_1
            + "\":{\"age\":2}},"
            + "\""
            + SOME_NAMESPACE2
            + "\":{\""
            + SOME_ASSET_ID_2
            + "\":{\"age\":1}},"
            + "\"_version\":2}";
    Put put =
        Put.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(AssetAttribute.AGE, 2))
            .textValue(AssetAttribute.INPUT, tamperedInput)
            .build();
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_ContractArgumentTampered_ShouldReturnWithContractValidatorError()
      throws ExecutionException {
    // Arrange
    createAssets(ENTITY_ID_A);
    // maliciously update the argument of the contract
    JsonObject argument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder()
                    .add(SOME_NAMESPACE1 + ASSET_ID_SEPARATOR + SOME_ASSET_ID_1)
                    .add(SOME_NAMESPACE2 + ASSET_ID_SEPARATOR + SOME_ASSET_ID_2)
                    .build())
            .add(AMOUNT_ATTRIBUTE_NAME, 0)
            .add(Argument.NONCE_KEY_NAME, UUID.randomUUID().toString())
            .build();
    Put put =
        Put.newBuilder()
            .namespace(resolver.resolve(SOME_NAMESPACE1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(AssetAttribute.AGE, 2))
            .textValue(AssetAttribute.ARGUMENT, argument.toString())
            .build();
    storageService.put(put);

    // Act
    LedgerValidationResult resultA =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE1, SOME_ASSET_ID_1));
    LedgerValidationResult resultB =
        validationService.validate(prepareValidationRequest(SOME_NAMESPACE2, SOME_ASSET_ID_2));

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_CONTRACT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }
}
