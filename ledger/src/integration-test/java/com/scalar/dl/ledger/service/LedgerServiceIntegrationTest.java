package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.service.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.CONTRACT_ID_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID2;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID3;
import static com.scalar.dl.ledger.service.Constants.CREATE_CONTRACT_ID4;
import static com.scalar.dl.ledger.service.Constants.CREATE_FUNCTION_ID1;
import static com.scalar.dl.ledger.service.Constants.ENTITY_ID_A;
import static com.scalar.dl.ledger.service.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_NAMESPACE;
import static com.scalar.dl.ledger.service.Constants.FUNCTION_TABLE;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID2;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID3;
import static com.scalar.dl.ledger.service.Constants.GET_BALANCE_CONTRACT_ID4;
import static com.scalar.dl.ledger.service.Constants.ID_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.Constants.KEY_VERSION;
import static com.scalar.dl.ledger.service.Constants.PAYMENT_CONTRACT_ID1;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_A;
import static com.scalar.dl.ledger.service.Constants.SECRET_KEY_B;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_1;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_2;
import static com.scalar.dl.ledger.service.Constants.SOME_AMOUNT_3;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_1;
import static com.scalar.dl.ledger.service.Constants.SOME_ASSET_ID_2;
import static com.scalar.dl.ledger.service.Constants.SOME_BALANCE;
import static com.scalar.dl.ledger.service.Constants.SOME_ID;
import static com.scalar.dl.ledger.service.Constants.SOME_NONCE;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_A;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_B;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_A;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractExecutor;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.AuditorKeyValidator;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.CertificateManager;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.crypto.HmacSigner;
import com.scalar.dl.ledger.crypto.HmacValidator;
import com.scalar.dl.ledger.crypto.SecretManager;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.exception.UnloadableContractException;
import com.scalar.dl.ledger.exception.UnloadableKeyException;
import com.scalar.dl.ledger.function.FunctionMachine;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.service.contract.ContractUsingContext;
import com.scalar.dl.ledger.service.contract.Create;
import com.scalar.dl.ledger.service.contract.CreateWithJackson;
import com.scalar.dl.ledger.service.contract.CreateWithJsonp;
import com.scalar.dl.ledger.service.contract.CreateWithString;
import com.scalar.dl.ledger.service.contract.GetBalance;
import com.scalar.dl.ledger.service.contract.GetBalanceWithJackson;
import com.scalar.dl.ledger.service.contract.GetBalanceWithJsonp;
import com.scalar.dl.ledger.service.contract.GetBalanceWithString;
import com.scalar.dl.ledger.service.contract.Payment;
import com.scalar.dl.ledger.service.contract.PaymentWithJackson;
import com.scalar.dl.ledger.service.contract.PaymentWithJsonp;
import com.scalar.dl.ledger.service.contract.PaymentWithString;
import com.scalar.dl.ledger.service.function.CreateFunction;
import com.scalar.dl.ledger.service.function.CreateFunctionWithJackson;
import com.scalar.dl.ledger.service.function.CreateFunctionWithJsonp;
import com.scalar.dl.ledger.service.function.CreateFunctionWithString;
import com.scalar.dl.ledger.service.function.FunctionUsingContext;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerServiceIntegrationTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  @Mock private TransactionManager transactionManager;
  @Mock private TamperEvidentAssetLedger ledger;
  @Mock private MutableDatabase<Get, Scan, Put, Delete, Result> database;
  @Mock private CertificateManager certManager;
  @Mock private SecretManager secretManager;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private AuditorKeyValidator auditorKeyValidator;
  @Mock private ContractManager contractManager;
  @Mock private ClientIdentityKey clientIdentityKey;
  @Mock private FunctionManager functionManager;
  private LedgerService service;
  private DigitalSignatureSigner dsSigner;
  private DigitalSignatureValidator dsValidator;
  private HmacSigner hmacSigner;
  private HmacValidator hmacValidator;

  @BeforeEach
  public void setUp() throws UnloadableKeyException {
    MockitoAnnotations.openMocks(this);

    prepare(false);
  }

  private void prepare(boolean isFunctionEnabled) {
    Properties props = new Properties();
    props.setProperty(DatabaseConfig.CONTACT_POINTS, "localhost");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "false");
    props.setProperty(LedgerConfig.FUNCTION_ENABLED, Boolean.toString(isFunctionEnabled));
    LedgerConfig config = new LedgerConfig(props);
    ContractExecutor contractExecutor =
        new ContractExecutor(config, contractManager, functionManager, transactionManager);
    service =
        new LedgerService(
            new BaseService(certManager, secretManager, clientKeyValidator, contractManager),
            config,
            clientKeyValidator,
            auditorKeyValidator,
            contractExecutor,
            functionManager);

    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_A);
    dsValidator = new DigitalSignatureValidator(CERTIFICATE_A);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(dsValidator);
  }

  private String prepareArgumentForCreate(String assetId, int amount, String nonce) {
    return prepareArgumentForCreate(assetId, amount, nonce, false, false);
  }

  private String prepareArgumentForCreate(
      String assetId, int amount, String nonce, boolean isV2Argument, boolean executeNested) {
    JsonObject json =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, assetId)
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .add(EXECUTE_NESTED_ATTRIBUTE_NAME, executeNested)
            .build();

    if (isV2Argument) {
      return Argument.format(json, nonce, Collections.emptyList());
    } else {
      json = Json.createObjectBuilder(json).add(Argument.NONCE_KEY_NAME, nonce).build();
      return jsonpSerDe.serialize(json);
    }
  }

  private String prepareJacksonArgumentForCreate(String assetId, int amount, String nonce) {
    return prepareJacksonArgumentForCreate(assetId, amount, nonce, false, false);
  }

  private String prepareJacksonArgumentForCreate(
      String assetId, int amount, String nonce, boolean isV2Argument, boolean executeNested) {
    ObjectNode json =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, assetId)
            .put(AMOUNT_ATTRIBUTE_NAME, amount)
            .put(EXECUTE_NESTED_ATTRIBUTE_NAME, executeNested);

    if (isV2Argument) {
      return Argument.format(json, nonce, Collections.emptyList());
    } else {
      json.put(Argument.NONCE_KEY_NAME, nonce);
      return jacksonSerDe.serialize(json);
    }
  }

  private String prepareStringArgumentForCreate(String assetId, int amount, String nonce) {
    return prepareStringArgumentForCreate(assetId, amount, nonce, false);
  }

  private String prepareStringArgumentForCreate(
      String assetId, int amount, String nonce, boolean executeNested) {
    String argument = assetId + "," + amount;
    if (executeNested) {
      argument += ",true";
    }
    return Argument.format(argument, nonce, Collections.emptyList());
  }

  private JsonObject prepareArgumentForCreateFunction(String assetId, int balance) {
    return Json.createObjectBuilder()
        .add(ID_ATTRIBUTE_NAME, assetId)
        .add(BALANCE_ATTRIBUTE_NAME, balance)
        .build();
  }

  private String prepareStringArgumentForCreateFunction(String id, int balance) {
    return id + "," + balance;
  }

  private String prepareArgumentForPayment(List<String> assetIds, int amount, String nonce) {
    return prepareArgumentForPayment(assetIds, amount, nonce, false);
  }

  private String prepareArgumentForPayment(
      List<String> assetIds, int amount, String nonce, boolean isV2Argument) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    assetIds.forEach(builder::add);

    JsonObject json =
        Json.createObjectBuilder()
            .add(ASSETS_ATTRIBUTE_NAME, builder.build())
            .add(AMOUNT_ATTRIBUTE_NAME, amount)
            .build();
    if (isV2Argument) {
      return Argument.format(json, nonce, Collections.emptyList());
    } else {
      json = Json.createObjectBuilder(json).add(Argument.NONCE_KEY_NAME, nonce).build();
      return jsonpSerDe.serialize(json);
    }
  }

  private String prepareJacksonArgumentForPayment(List<String> assetIds, int amount, String nonce) {
    return prepareJacksonArgumentForPayment(assetIds, amount, nonce, false);
  }

  private String prepareJacksonArgumentForPayment(
      List<String> assetIds, int amount, String nonce, boolean isV2Argument) {
    ArrayNode array = mapper.createArrayNode();
    assetIds.forEach(array::add);

    ObjectNode json =
        mapper
            .createObjectNode()
            .put(AMOUNT_ATTRIBUTE_NAME, amount)
            .set(ASSETS_ATTRIBUTE_NAME, array);

    if (isV2Argument) {
      return Argument.format(json, nonce, Collections.emptyList());
    } else {
      json.put(Argument.NONCE_KEY_NAME, nonce);
      return jacksonSerDe.serialize(json);
    }
  }

  private String prepareStringArgumentForPayment(List<String> assetIds, int amount, String nonce) {
    String argument = Joiner.on(',').join(assetIds) + "," + amount;
    return Argument.format(argument, nonce, Collections.emptyList());
  }

  private InternalAsset prepareAssetMock(int balance) {
    JsonObject data = Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, balance).build();
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.data()).thenReturn(data.toString());
    return asset;
  }

  private InternalAsset prepareStringAssetMock(int balance) {
    String data = BALANCE_ATTRIBUTE_NAME + "," + balance;
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.data()).thenReturn(data);
    return asset;
  }

  private ContractRegistrationRequest prepareRegistrationRequest(
      SignatureSigner signer, String id, String name, byte[] contract) {
    byte[] serialized =
        ContractRegistrationRequest.serialize(id, name, contract, null, ENTITY_ID_A, KEY_VERSION);
    return new ContractRegistrationRequest(
        id, name, contract, null, ENTITY_ID_A, KEY_VERSION, signer.sign(serialized));
  }

  private ContractExecutionRequest prepareExecutionRequest(
      SignatureSigner signer, String id, String argument) {
    return prepareExecutionRequest(signer, id, argument, Collections.emptyList(), null, null);
  }

  private ContractExecutionRequest prepareExecutionRequest(
      SignatureSigner signer,
      String contractId,
      String contractArgument,
      List<String> functionIds,
      @Nullable String functionArgument) {
    return prepareExecutionRequest(
        signer, contractId, contractArgument, functionIds, functionArgument, null);
  }

  private ContractExecutionRequest prepareExecutionRequest(
      SignatureSigner signer,
      String contractId,
      String contractArgument,
      List<String> functionIds,
      @Nullable String functionArgument,
      @Nullable SignatureSigner auditorSigner) {
    byte[] serialized =
        ContractExecutionRequest.serialize(contractId, contractArgument, ENTITY_ID_A, KEY_VERSION);
    return new ContractExecutionRequest(
        SOME_NONCE,
        ENTITY_ID_A,
        KEY_VERSION,
        contractId,
        contractArgument,
        functionIds,
        functionArgument,
        signer.sign(serialized),
        auditorSigner == null ? null : auditorSigner.sign(serialized));
  }

  private ContractEntry prepareContractEntry(String contractId, String entityId, int certVersion) {
    return prepareContractEntry(contractId, entityId, certVersion, null);
  }

  private ContractEntry prepareContractEntry(
      String contractId, String entityId, int certVersion, @Nullable String properties) {
    ContractEntry entry = mock(ContractEntry.class);
    when(clientIdentityKey.getEntityId()).thenReturn(entityId);
    when(clientIdentityKey.getKeyVersion()).thenReturn(certVersion);
    ContractEntry.Key contractKey = new ContractEntry.Key(contractId, clientIdentityKey);
    when(entry.getKey()).thenReturn(contractKey);
    when(entry.getEntityId()).thenReturn(entityId);
    when(entry.getKeyVersion()).thenReturn(certVersion);
    when(entry.getClientIdentityKey()).thenReturn(clientIdentityKey);
    when(entry.getProperties()).thenReturn(Optional.ofNullable(properties));
    return entry;
  }

  @Test
  public void register_RequestSignedByCorrectPrivateKeyGiven_ShouldRegister() {
    // Arrange
    byte[] contract = "create".getBytes(StandardCharsets.UTF_8);
    ContractRegistrationRequest request =
        prepareRegistrationRequest(dsSigner, CREATE_CONTRACT_ID1, CREATE_CONTRACT_ID1, contract);

    // Act
    assertThatCode(() -> service.register(request)).doesNotThrowAnyException();

    // Assert
    verify(contractManager).register(any(ContractEntry.class));
  }

  @Test
  public void register_RequestSignedByIncompatiblePrivateKeyGiven_ShouldNotRegister() {
    // Arrange
    byte[] contract = "create".getBytes(StandardCharsets.UTF_8);
    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_B);
    ContractRegistrationRequest request =
        prepareRegistrationRequest(dsSigner, CREATE_CONTRACT_ID1, CREATE_CONTRACT_ID1, contract);

    // Act
    assertThatThrownBy(() -> service.register(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(contractManager, never()).register(any(ContractEntry.class));
  }

  @Test
  public void register_RequestWithIncompatibleCertIdGiven_ShouldNotRegister() {
    // Arrange
    byte[] contract = "create".getBytes(StandardCharsets.UTF_8);
    ContractRegistrationRequest request =
        prepareRegistrationRequest(dsSigner, CREATE_CONTRACT_ID1, CREATE_CONTRACT_ID1, contract);
    // simulate wrong cert id is specified (then wrong certificate is returned)
    dsValidator = new DigitalSignatureValidator(CERTIFICATE_B);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(dsValidator);

    // Act
    assertThatThrownBy(() -> service.register(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(contractManager, never()).register(any(ContractEntry.class));
  }

  @Test
  public void register_RequestSignedByCorrectHmacSecretKeyGiven_ShouldRegister() {
    // Arrange
    hmacSigner = new HmacSigner(SECRET_KEY_A);
    hmacValidator = new HmacValidator(SECRET_KEY_A);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(hmacValidator);

    byte[] contract = "create".getBytes(StandardCharsets.UTF_8);
    ContractRegistrationRequest request =
        prepareRegistrationRequest(hmacSigner, CREATE_CONTRACT_ID1, CREATE_CONTRACT_ID1, contract);

    // Act
    assertThatCode(() -> service.register(request)).doesNotThrowAnyException();

    // Assert
    verify(contractManager).register(any(ContractEntry.class));
  }

  @Test
  public void register_RequestSignedByIncompatibleHmacSecretKeyGiven_ShouldNotRegister() {
    // Arrange
    hmacSigner = new HmacSigner(SECRET_KEY_A);
    hmacValidator = new HmacValidator(SECRET_KEY_B);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(hmacValidator);

    byte[] contract = "create".getBytes(StandardCharsets.UTF_8);
    ContractRegistrationRequest request =
        prepareRegistrationRequest(hmacSigner, CREATE_CONTRACT_ID1, CREATE_CONTRACT_ID1, contract);

    // Act
    assertThatThrownBy(() -> service.register(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(contractManager, never()).register(any(ContractEntry.class));
  }

  @Test
  public void execute_RequestWithCreateContractGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Create());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_RequestWithCreateContractAndV2ArgumentGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Create());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_RequestWithCreateBasedOnJsonpContractGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).commit();
  }

  @Test
  public void
      execute_RequestWithCreateBasedOnJsonpContractAndV2ArgumentGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_RequestWithCreateBasedOnJacksonContractGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument = prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    verify(ledger).commit();
  }

  @Test
  public void
      execute_RequestWithCreateBasedOnJacksonContractWithV2ArgumentGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument =
        prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    verify(ledger).commit();
  }

  @Test
  public void
      execute_RequestWithCreateBasedOnStringContractWithV2ArgumentGiven_ShouldPutNewAssetEntry() {
    // Arrange
    String argument = prepareStringArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithString());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    verify(ledger).put(SOME_ASSET_ID_1, expected);
    verify(ledger).commit();
  }

  @Test
  public void
      execute_RequestSignedByHmacWithCreateBasedOnJacksonContractGiven_ShouldPutNewAssetEntry() {
    // Arrange
    hmacSigner = new HmacSigner(SECRET_KEY_A);
    hmacValidator = new HmacValidator(SECRET_KEY_A);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(hmacValidator);

    String argument = prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(hmacSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new CreateWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    verify(ledger).commit();
  }

  @Test
  public void execute_RequestSignedByIncompatiblePrivateKeyGiven_ShouldNotExecute() {
    // Arrange
    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_B);
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_RequestWithIncompatibleCertIdGiven_ShouldNotExecute() {
    // Arrange
    hmacSigner = new HmacSigner(SECRET_KEY_A);
    hmacValidator = new HmacValidator(SECRET_KEY_B);
    when(clientKeyValidator.getValidator(ENTITY_ID_A, KEY_VERSION)).thenReturn(hmacValidator);

    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractExecutionRequest request =
        prepareExecutionRequest(hmacSigner, CREATE_CONTRACT_ID1, argument);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_RequestSignedByIncompatibleHmacSecretKeyGiven_ShouldNotExecute() {
    // Arrange
    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_B);
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(SignatureException.class);

    // Assert
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_RequestWithCreateContractGivenAndExceptionThrownInCommit_ShouldThrowAsItIs() {
    // Arrange
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Create());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    DatabaseException toThrow = mock(DatabaseException.class);
    doThrow(toThrow).when(ledger).commit();

    // Act
    assertThatThrownBy(() -> service.execute(request)).isEqualTo(toThrow);
  }

  @Test
  public void execute_UnknownContractGiven_ShouldThrowContractException() {
    // Arrange
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argument);
    UnloadableContractException toThrow = mock(UnloadableContractException.class);
    when(contractManager.getInstance(any())).thenThrow(toThrow);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isEqualTo(toThrow);

    // Assert
    verify(ledger, never()).put(anyString(), anyString());
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_PaymentContractGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Payment());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expectedFrom =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_1, expectedFrom.toString());
    JsonObject expectedTo =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_2, expectedTo.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentContractWithV2ArgumentGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE, true);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Payment());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expectedFrom =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_1, expectedFrom.toString());
    JsonObject expectedTo =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_2, expectedTo.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentBasedOnJsonpContractGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new PaymentWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expectedFrom =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_1, expectedFrom.toString());
    JsonObject expectedTo =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_2, expectedTo.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentBasedOnJsonpContractWithV2ArgumentGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE, true);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new PaymentWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expectedFrom =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_1, expectedFrom.toString());
    JsonObject expectedTo =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2)
            .build();
    verify(ledger).put(SOME_ASSET_ID_2, expectedTo.toString());
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentBasedOnJacksonContractGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareJacksonArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new PaymentWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expectedFrom =
        mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expectedFrom));
    JsonNode expectedTo =
        mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_2, jacksonSerDe.serialize(expectedTo));
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentBasedOnJacksonContractWithV2ArgumentGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareJacksonArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE, true);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new PaymentWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expectedFrom =
        mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 - SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expectedFrom));
    JsonNode expectedTo =
        mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1 + SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_2, jacksonSerDe.serialize(expectedTo));
    verify(ledger).commit();
  }

  @Test
  public void execute_PaymentBasedOnStringContractWithV2ArgumentGiven_ShouldPutNewAssetEntries() {
    // Arrange
    String argument =
        prepareStringArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_2, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new PaymentWithString());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareStringAssetMock(SOME_AMOUNT_1);
    InternalAsset asset2 = prepareStringAssetMock(SOME_AMOUNT_1);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    String expectedFrom = BALANCE_ATTRIBUTE_NAME + "," + (SOME_AMOUNT_1 - SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_1, expectedFrom);
    String expectedTo = BALANCE_ATTRIBUTE_NAME + "," + (SOME_AMOUNT_1 + SOME_AMOUNT_2);
    verify(ledger).put(SOME_ASSET_ID_2, expectedTo);
    verify(ledger).commit();
  }

  @Test
  public void
      execute_PaymentContractGivenAndLackOfBalanceInPayee_ShouldThrowContractContextException() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Payment());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    InternalAsset asset1 = prepareAssetMock(SOME_AMOUNT_3);
    InternalAsset asset2 = prepareAssetMock(SOME_AMOUNT_2);
    when(ledger.get(SOME_ASSET_ID_1)).thenReturn(Optional.of(asset1));
    when(ledger.get(SOME_ASSET_ID_2)).thenReturn(Optional.of(asset2));

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(ContractContextException.class);

    // Assert
    verify(ledger, never()).put(anyString(), anyString());
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_PaymentContractGivenAndAssetNotFound_ShouldThrowContractExecutionException() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);
    ContractMachine contract = new ContractMachine(new Payment());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    DatabaseException toThrow = mock(DatabaseException.class);
    when(ledger.get(SOME_ASSET_ID_1)).thenThrow(toThrow);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(DatabaseException.class);

    // Assert
    verify(ledger, never()).put(anyString(), anyString());
    verify(ledger, never()).commit();
  }

  @Test
  public void execute_UnregisteredPaymentContractGiven_ShouldThrowMissingContractException() {
    // Arrange
    String argument =
        prepareArgumentForPayment(
            Arrays.asList(SOME_ASSET_ID_1, SOME_ASSET_ID_2), SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(PAYMENT_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenThrow(MissingContractException.class);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, PAYMENT_CONTRACT_ID1, argument);

    // Act
    assertThatThrownBy(() -> service.execute(request)).isInstanceOf(MissingContractException.class);

    // Assert
    verify(contractManager, never()).getInstance(entry);
  }

  @Test
  public void execute_CreateContractAndFunctionGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new Create());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunction());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateContractAndFunctionWithV2ArgumentGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new Create());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunction());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateJsonpBasedContractAndFunctionGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument = prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new CreateWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunctionWithJsonp());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateJsonpBasedContractAndFunctionWithV2ArgumentGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new CreateWithJsonp());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunctionWithJsonp());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateJacksonBasedContractAndFunctionGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument = prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new CreateWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunctionWithJackson());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateJacksonBasedContractAndFunctionWithV2ArgumentGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument =
        prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, false);
    JsonObject functionArgument = prepareArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument.toString());
    ContractMachine contract = new ContractMachine(new CreateWithJackson());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunctionWithJackson());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateStringBasedContractAndFunctionWithV2ArgumentGiven_ShouldExecuteBoth() {
    // Arrange
    prepare(true);
    String argument = prepareStringArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    String functionArgument = prepareStringArgumentForCreateFunction(SOME_ID, SOME_BALANCE);
    ContractEntry entry = prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner,
            CREATE_CONTRACT_ID1,
            argument,
            Collections.singletonList(CREATE_FUNCTION_ID1),
            functionArgument);
    ContractMachine contract = new ContractMachine(new CreateWithString());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new CreateFunctionWithString());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    service.execute(request);

    // Assert
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    verify(ledger).put(SOME_ASSET_ID_1, expected);
    Put expectedPut =
        new Put(new Key(ID_ATTRIBUTE_NAME, SOME_ID))
            .withValue(BALANCE_ATTRIBUTE_NAME, SOME_BALANCE)
            .forNamespace(FUNCTION_NAMESPACE)
            .forTable(FUNCTION_TABLE);
    verify(database).put(expectedPut);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateAndGetBalanceContractsGiven_ShouldPutNewAssetEntryAndReturn() {
    // Arrange
    String argumentForCreate =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, false, true);
    String createProperties =
        Json.createObjectBuilder()
            .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID1)
            .build()
            .toString();
    String getBalanceProperties =
        Json.createObjectBuilder()
            .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID1)
            .build()
            .toString();
    ContractEntry createEntry =
        prepareContractEntry(CREATE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION, createProperties);
    ContractEntry getBalanceEntry =
        prepareContractEntry(
            GET_BALANCE_CONTRACT_ID1, ENTITY_ID_A, KEY_VERSION, getBalanceProperties);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID_A, KEY_VERSION);
    ContractEntry.Key createContractKey = new ContractEntry.Key(CREATE_CONTRACT_ID1, certKey);
    ContractEntry.Key getBalanceContractKey =
        new ContractEntry.Key(GET_BALANCE_CONTRACT_ID1, certKey);
    when(contractManager.get(createContractKey)).thenReturn(createEntry);
    when(contractManager.get(getBalanceContractKey)).thenReturn(getBalanceEntry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID1, argumentForCreate);
    ContractMachine create = new ContractMachine(new Create());
    create.initialize(contractManager, certKey);
    ContractMachine getBalance = new ContractMachine(new GetBalance());
    getBalance.initialize(contractManager, certKey);
    when(contractManager.getInstance(any())).thenReturn(create).thenReturn(getBalance);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    InternalAsset asset = mock(InternalAsset.class);
    String expectedResult =
        Json.createObjectBuilder().add(ASSET_ATTRIBUTE_NAME, "expectedResult").build().toString();
    when(asset.data()).thenReturn(expectedResult);
    doReturn(Optional.of(asset)).when(ledger).get(anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    ContractExecutionResult result = service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(result.getContractResult()).isEqualTo(Optional.of(expectedResult));
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).get(SOME_ASSET_ID_1);
    verify(ledger).commit();
  }

  @Test
  public void execute_CreateAndGetBalanceJsonpBasedContractGiven_ShouldPutNewAssetEntryAndReturn() {
    // Arrange
    String argumentForCreate =
        prepareArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, true);
    String createProperties =
        Json.createObjectBuilder()
            .add(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID2)
            .build()
            .toString();
    String getBalanceProperties =
        Json.createObjectBuilder()
            .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID2)
            .build()
            .toString();
    ContractEntry createEntry =
        prepareContractEntry(CREATE_CONTRACT_ID2, ENTITY_ID_A, KEY_VERSION, createProperties);
    ContractEntry getBalanceEntry =
        prepareContractEntry(
            GET_BALANCE_CONTRACT_ID2, ENTITY_ID_A, KEY_VERSION, getBalanceProperties);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID_A, KEY_VERSION);
    ContractEntry.Key createContractKey = new ContractEntry.Key(CREATE_CONTRACT_ID2, certKey);
    ContractEntry.Key getBalanceContractKey =
        new ContractEntry.Key(GET_BALANCE_CONTRACT_ID2, certKey);
    when(contractManager.get(createContractKey)).thenReturn(createEntry);
    when(contractManager.get(getBalanceContractKey)).thenReturn(getBalanceEntry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID2, argumentForCreate);
    ContractMachine create = new ContractMachine(new CreateWithJsonp());
    create.initialize(contractManager, certKey);
    ContractMachine getBalance = new ContractMachine(new GetBalanceWithJsonp());
    getBalance.initialize(contractManager, certKey);
    when(contractManager.getInstance(any())).thenReturn(create).thenReturn(getBalance);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    InternalAsset asset = mock(InternalAsset.class);
    String expectedResult =
        Json.createObjectBuilder().add(ASSET_ATTRIBUTE_NAME, "expectedResult").build().toString();
    when(asset.data()).thenReturn(expectedResult);
    doReturn(Optional.of(asset)).when(ledger).get(anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    ContractExecutionResult result = service.execute(request);

    // Assert
    JsonObject expected =
        Json.createObjectBuilder().add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1).build();
    assertThat(result.getContractResult()).isEqualTo(Optional.of(expectedResult));
    verify(ledger).put(SOME_ASSET_ID_1, expected.toString());
    verify(ledger).get(SOME_ASSET_ID_1);
    verify(ledger).commit();
  }

  @Test
  public void
      execute_CreateAndGetBalanceJacksonBasedContractGiven_ShouldPutNewAssetEntryAndReturn() {
    // Arrange
    String argumentForCreate =
        prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true, true);
    String createProperties =
        jacksonSerDe.serialize(
            mapper.createObjectNode().put(CONTRACT_ID_ATTRIBUTE_NAME, CREATE_CONTRACT_ID3));
    String getBalanceProperties =
        jacksonSerDe.serialize(
            mapper.createObjectNode().put(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID3));
    ContractEntry createEntry =
        prepareContractEntry(CREATE_CONTRACT_ID3, ENTITY_ID_A, KEY_VERSION, createProperties);
    ContractEntry getBalanceEntry =
        prepareContractEntry(
            GET_BALANCE_CONTRACT_ID3, ENTITY_ID_A, KEY_VERSION, getBalanceProperties);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID_A, KEY_VERSION);
    ContractEntry.Key createContractKey = new ContractEntry.Key(CREATE_CONTRACT_ID3, certKey);
    ContractEntry.Key getBalanceContractKey =
        new ContractEntry.Key(GET_BALANCE_CONTRACT_ID3, certKey);
    when(contractManager.get(createContractKey)).thenReturn(createEntry);
    when(contractManager.get(getBalanceContractKey)).thenReturn(getBalanceEntry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID3, argumentForCreate);
    ContractMachine create = new ContractMachine(new CreateWithJackson());
    create.initialize(contractManager, certKey);
    ContractMachine getBalance = new ContractMachine(new GetBalanceWithJackson());
    getBalance.initialize(contractManager, certKey);
    when(contractManager.getInstance(any())).thenReturn(create).thenReturn(getBalance);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    InternalAsset asset = mock(InternalAsset.class);
    String expectedResult =
        jacksonSerDe.serialize(
            mapper.createObjectNode().put(ASSET_ATTRIBUTE_NAME, "expectedResult"));
    when(asset.data()).thenReturn(expectedResult);
    doReturn(Optional.of(asset)).when(ledger).get(anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    ContractExecutionResult result = service.execute(request);

    // Assert
    JsonNode expected = mapper.createObjectNode().put(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    assertThat(result.getContractResult()).isEqualTo(Optional.of(expectedResult));
    verify(ledger).put(SOME_ASSET_ID_1, jacksonSerDe.serialize(expected));
    verify(ledger).get(SOME_ASSET_ID_1);
    verify(ledger).commit();
  }

  @Test
  public void
      execute_CreateAndGetBalanceStringBasedContractGiven_ShouldPutNewAssetEntryAndReturn() {
    // Arrange
    String argumentForCreate =
        prepareStringArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE, true);
    ContractEntry createEntry =
        prepareContractEntry(CREATE_CONTRACT_ID4, ENTITY_ID_A, KEY_VERSION, CREATE_CONTRACT_ID4);
    ContractEntry getBalanceEntry =
        prepareContractEntry(
            GET_BALANCE_CONTRACT_ID4, ENTITY_ID_A, KEY_VERSION, GET_BALANCE_CONTRACT_ID4);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID_A, KEY_VERSION);
    ContractEntry.Key createContractKey = new ContractEntry.Key(CREATE_CONTRACT_ID4, certKey);
    ContractEntry.Key getBalanceContractKey =
        new ContractEntry.Key(GET_BALANCE_CONTRACT_ID4, certKey);
    when(contractManager.get(createContractKey)).thenReturn(createEntry);
    when(contractManager.get(getBalanceContractKey)).thenReturn(getBalanceEntry);
    ContractExecutionRequest request =
        prepareExecutionRequest(dsSigner, CREATE_CONTRACT_ID4, argumentForCreate);
    ContractMachine create = new ContractMachine(new CreateWithString());
    create.initialize(contractManager, certKey);
    ContractMachine getBalance = new ContractMachine(new GetBalanceWithString());
    getBalance.initialize(contractManager, certKey);
    when(contractManager.getInstance(any())).thenReturn(create).thenReturn(getBalance);
    Transaction transaction = new Transaction(ledger, null);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doNothing().when(ledger).put(anyString(), anyString());
    InternalAsset asset = mock(InternalAsset.class);
    String expectedResult = ASSET_ATTRIBUTE_NAME + "," + "expectedResult";
    when(asset.data()).thenReturn(expectedResult);
    doReturn(Optional.of(asset)).when(ledger).get(anyString());
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    ContractExecutionResult result = service.execute(request);

    // Assert
    String expected = BALANCE_ATTRIBUTE_NAME + "," + SOME_AMOUNT_1;
    assertThat(result.getContractResult()).isEqualTo(Optional.of(expectedResult));
    verify(ledger).put(SOME_ASSET_ID_1, expected);
    verify(ledger).get(SOME_ASSET_ID_1);
    verify(ledger).commit();
  }

  @Test
  public void
      execute_ContractAndFunctionUsingContextGiven_ShouldPassContextFromContractToFunction() {
    // Arrange
    prepare(true);
    String argument = prepareJacksonArgumentForCreate(SOME_ASSET_ID_1, SOME_AMOUNT_1, SOME_NONCE);
    ContractEntry entry = prepareContractEntry(SOME_ID, ENTITY_ID_A, KEY_VERSION);
    when(contractManager.get(entry.getKey())).thenReturn(entry);
    ContractExecutionRequest request =
        prepareExecutionRequest(
            dsSigner, SOME_ID, argument, Collections.singletonList(SOME_ID), null);
    ContractMachine contract = new ContractMachine(new ContractUsingContext());
    contract.initialize(contractManager, entry.getClientIdentityKey());
    when(contractManager.getInstance(any())).thenReturn(contract);
    FunctionMachine function = new FunctionMachine(new FunctionUsingContext());
    when(functionManager.getInstance(anyString())).thenReturn(function);
    Transaction transaction = new Transaction(ledger, database);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doReturn(Collections.emptyList()).when(ledger).commit();

    // Act
    ContractExecutionResult result = service.execute(request);

    // Assert
    assertThat(result.getFunctionResult().orElseThrow(AssertionError::new)).isEqualTo(argument);
  }
}
