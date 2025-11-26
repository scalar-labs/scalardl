package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_A;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_A;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.AssetProofRetrievalRequest;
import com.scalar.dl.ledger.model.LedgerValidationRequest;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import com.scalar.dl.ledger.validation.DeprecatedLedgerTracer;
import com.scalar.dl.ledger.validation.JacksonBasedLedgerTracer;
import com.scalar.dl.ledger.validation.JsonpBasedLedgerTracer;
import com.scalar.dl.ledger.validation.LedgerTracerBase;
import com.scalar.dl.ledger.validation.LedgerValidator;
import com.scalar.dl.ledger.validation.StringBasedLedgerTracer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.json.JsonObject;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("unchecked")
public class LedgerValidationServiceTest {
  @Mock private LedgerConfig config;
  @Mock private TransactionManager transactionManager;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private ContractManager contractManager;
  @Mock private AssetProofComposer proofComposer;
  @Mock private Transaction transaction;
  @Mock private TamperEvidentAssetLedger ledger;
  private LedgerValidationService service;
  private static final String ID = "id";
  private static final int AGE = 1;
  private static final String ENTITY_ID = "request_entity_id";
  private static final int KEY_VERSION = 1;
  private static final String CONTRACT_ID = "contract_id";
  private static final String CONTRACT_ARGUMENT = "contract_argument";
  private static final String CONTRACT_ID_IN_ASSET =
      ENTITY_ID + "/" + KEY_VERSION + "/" + CONTRACT_ID;
  private static final String ANY_INPUT = "input";
  private static final byte[] FIRST_HASH = "first".getBytes(StandardCharsets.UTF_8);
  private static final byte[] LAST_HASH = "second".getBytes(StandardCharsets.UTF_8);
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
    when(config.isAuditorEnabled()).thenReturn(false);
  }

  private List<LedgerValidator> createValidators() {
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator1 = mock(LedgerValidator.class);
    LedgerValidator validator2 = mock(LedgerValidator.class);
    validators.add(validator1);
    validators.add(validator2);
    return validators;
  }

  private List<InternalAsset> createAssetMocks() {
    InternalAsset asset1 = mock(InternalAsset.class);
    when(asset1.id()).thenReturn(ID);
    when(asset1.age()).thenReturn(0);
    when(asset1.input()).thenReturn(ANY_INPUT);
    when(asset1.argument()).thenReturn(CONTRACT_ARGUMENT);
    when(asset1.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset1.hash()).thenReturn(FIRST_HASH);
    when(asset1.prevHash()).thenReturn(FIRST_HASH);
    InternalAsset asset2 = mock(InternalAsset.class);
    when(asset2.id()).thenReturn(ID);
    when(asset2.age()).thenReturn(1);
    when(asset2.input()).thenReturn(ANY_INPUT);
    when(asset2.argument()).thenReturn(CONTRACT_ARGUMENT);
    when(asset2.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset2.hash()).thenReturn(LAST_HASH);
    when(asset2.prevHash()).thenReturn(LAST_HASH);
    return Arrays.asList(asset1, asset2);
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  private ContractMachine prepareContractBehaviors(List<InternalAsset> assets) {
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    doReturn(assets).when(ledger).scan(any(AssetFilter.class));
    ContractMachine contract = mock(ContractMachine.class);
    when(contractManager.getInstance(any(ContractEntry.class))).thenReturn(contract);
    ContractEntry entry = mock(ContractEntry.class);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(contractManager.get(any(ContractEntry.Key.class))).thenReturn(entry);
    when(contract.invoke(ArgumentMatchers.<Ledger<?>>any(), anyString(), nullable(String.class)))
        .thenReturn(null);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JSONP_JSON);
    return contract;
  }

  @Test
  public void validate_AssetIdGivenAndAllValidatorsReturnedTrue_ShouldReturnResultWithOK() {
    // Arrange
    List<InternalAsset> assets = createAssetMocks();
    ContractMachine contract = prepareContractBehaviors(assets);
    JsonpBasedLedgerTracer tracer = mock(JsonpBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    List<LedgerValidator> validators = createValidators();
    for (LedgerValidator v : validators) {
      when(v.validate(any(), any(ContractMachine.class), any(InternalAsset.class)))
          .thenReturn(StatusCode.OK);
    }
    service =
        spy(
            new LedgerValidationService(
                config,
                transactionManager,
                clientKeyValidator,
                contractManager,
                proofComposer,
                validators));
    when(service.getLedgerTracerBase(DeserializationType.JSONP_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    LedgerValidationResult result = service.validate(ID, 0, Integer.MAX_VALUE);

    // Assert
    assertThat(result.getCode()).isEqualTo(StatusCode.OK);
    ContractEntry.Key expected = new ContractEntry.Key(CONTRACT_ID, ENTITY_ID, KEY_VERSION);
    verify(contractManager, times(2)).get(expected);
    verify(contract, times(2)).invoke(tracer, CONTRACT_ARGUMENT, null);
    for (LedgerValidator v : validators) {
      verify(v).validate(tracer, contract, assets.get(0));
      verify(v).validate(tracer, contract, assets.get(1));
    }
    AssetFilter filter =
        new AssetFilter(ID)
            .withStartAge(0, true)
            .withEndAge(Integer.MAX_VALUE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(filter);
    verify(transaction).commit();
  }

  @Test
  public void validate_AssetIdGivenAndSomeValidatorReturnedFalse_ShouldReturnResultWithInvalid() {
    // Arrange
    List<InternalAsset> assets = createAssetMocks();
    ContractMachine contract = prepareContractBehaviors(assets);
    JsonpBasedLedgerTracer tracer = mock(JsonpBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    List<LedgerValidator> validators = createValidators();
    when(validators
            .get(0)
            .validate(any(Ledger.class), any(ContractMachine.class), any(InternalAsset.class)))
        .thenReturn(StatusCode.OK);
    when(validators
            .get(1)
            .validate(any(Ledger.class), any(ContractMachine.class), any(InternalAsset.class)))
        .thenReturn(StatusCode.INVALID_CONTRACT);
    service =
        spy(
            new LedgerValidationService(
                config,
                transactionManager,
                clientKeyValidator,
                contractManager,
                proofComposer,
                validators));
    when(service.getLedgerTracerBase(DeserializationType.JSONP_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    LedgerValidationResult result = service.validate(ID, 0, Integer.MAX_VALUE);

    // Assert
    assertThat(result.getCode()).isEqualTo(StatusCode.INVALID_CONTRACT);
    ContractEntry.Key expected = new ContractEntry.Key(CONTRACT_ID, ENTITY_ID, KEY_VERSION);
    verify(contractManager).get(expected);
    verify(contract).invoke(tracer, CONTRACT_ARGUMENT, null);
    for (LedgerValidator v : validators) {
      verify(v).validate(tracer, contract, assets.get(0));
    }
    verify(transaction).commit();
  }

  @Test
  public void validate_ExceptionThrownInAssetRetrieval_ShouldAbortAndThrowException() {
    // Arrange
    prepareContractBehaviors(createAssetMocks());
    DatabaseException toThrow = mock(DatabaseException.class);
    when(ledger.scan(any())).thenThrow(toThrow);
    List<LedgerValidator> validators = createValidators();

    service =
        new LedgerValidationService(
            config,
            transactionManager,
            clientKeyValidator,
            contractManager,
            proofComposer,
            validators);

    // Act
    Throwable thrown = catchThrowable(() -> service.validate(ID, 0, Integer.MAX_VALUE));

    // Assert
    assertThat(thrown).isEqualTo(toThrow);
    verify(transaction).abort();
  }

  @Test
  public void validate_LedgerValidationRequestSignedByCorrectKeyGiven_ShouldCallInternalValidate() {
    // Arrange
    prepareContractBehaviors(createAssetMocks());
    List<LedgerValidator> validators = createValidators();
    byte[] serialized =
        LedgerValidationRequest.serialize(ID, 0, Integer.MAX_VALUE, ENTITY_ID, KEY_VERSION);
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_A);
    when(clientKeyValidator.getValidator(ENTITY_ID, KEY_VERSION))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    JsonpBasedLedgerTracer tracer = mock(JsonpBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    service =
        spy(
            new LedgerValidationService(
                config,
                transactionManager,
                clientKeyValidator,
                contractManager,
                proofComposer,
                validators));
    when(service.getLedgerTracerBase(DeserializationType.JSONP_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    service.validate(
        new LedgerValidationRequest(
            ID, 0, Integer.MAX_VALUE, ENTITY_ID, KEY_VERSION, signer.sign(serialized)));

    // Assert
    verify(service).validate(ID, 0, Integer.MAX_VALUE);
  }

  @Test
  public void
      validate_LedgerValidationRequestSignedByWrongKeyGiven_ShouldThrowSignatureException() {
    // Arrange
    byte[] serialized =
        LedgerValidationRequest.serialize(ID, 0, Integer.MAX_VALUE, ENTITY_ID, KEY_VERSION);
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_B);
    when(clientKeyValidator.getValidator(ENTITY_ID, KEY_VERSION))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    assertThatThrownBy(
            () ->
                service.validate(
                    new LedgerValidationRequest(
                        ID, 0, Integer.MAX_VALUE, ENTITY_ID, KEY_VERSION, signer.sign(serialized))))
        .isInstanceOf(SignatureException.class);

    // Assert
    verify(service, never()).validate(ID, 0, Integer.MAX_VALUE);
  }

  @Test
  public void validate_AuditorEnabledAndLedgerValidationRequestGiven_ShouldThrowLedgerException() {
    // Arrange
    prepareContractBehaviors(createAssetMocks());
    List<LedgerValidator> validators = createValidators();
    byte[] serialized = LedgerValidationRequest.serialize(ID, 0, AGE, ENTITY_ID, KEY_VERSION);
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_A);
    when(clientKeyValidator.getValidator(ENTITY_ID, KEY_VERSION))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    when(config.isAuditorEnabled()).thenReturn(true);
    service =
        spy(
            new LedgerValidationService(
                config,
                transactionManager,
                clientKeyValidator,
                contractManager,
                proofComposer,
                validators));

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                service.validate(
                    new LedgerValidationRequest(
                        ID, 0, AGE, ENTITY_ID, KEY_VERSION, signer.sign(serialized))));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.INVALID_REQUEST);
    verify(service, never()).validate(anyString(), anyInt(), anyInt());
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void retrieve_AssetIdWithAgeGiven_ShouldReturnInternalAssetWithSpecifiedAge() {
    // Arrange
    int age = 1;
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    AssetFilter filter =
        new AssetFilter(ID)
            .withStartAge(age, true)
            .withEndAge(age, true)
            .withAgeOrder(AssetFilter.AgeOrder.ASC);
    InternalAsset asset = createAssetMocks().get(1);
    doReturn(Collections.singletonList(asset)).when(ledger).scan(filter);
    proofComposer = new AssetProofComposer(new DigitalSignatureSigner(PRIVATE_KEY_A));
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    InternalAsset actual = service.retrieve(ID, age);

    // Assert
    assertThat(actual.id()).isEqualTo(ID);
    assertThat(actual.age()).isEqualTo(age);
    verify(ledger).scan(filter);
    verify(transaction).commit();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void retrieve_AssetIdWithNegativeAgeGiven_ShouldReturnInternalAssetWithLatestAge() {
    // Arrange
    int age = 1;
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    InternalAsset asset = createAssetMocks().get(1);
    doReturn(Optional.of(asset)).when(ledger).get(ID);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    InternalAsset actual = service.retrieve(ID, -1);

    // Assert
    assertThat(actual.id()).isEqualTo(ID);
    assertThat(actual.age()).isEqualTo(age);
    verify(ledger).get(ID);
    verify(transaction).commit();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void retrieve_AssetIdWithMaxAgeGiven_ShouldReturnInternalAssetWithLatestAge() {
    // Arrange
    int age = 1;
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    InternalAsset asset = createAssetMocks().get(1);
    doReturn(Optional.of(asset)).when(ledger).get(ID);
    proofComposer = new AssetProofComposer(new DigitalSignatureSigner(PRIVATE_KEY_A));
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    InternalAsset actual = service.retrieve(ID, Integer.MAX_VALUE);

    // Assert
    assertThat(actual.id()).isEqualTo(ID);
    assertThat(actual.age()).isEqualTo(age);
    verify(ledger).get(ID);
    verify(transaction).commit();
  }

  @Test
  public void retrieve_ExceptionThrownInAssetRetrieval_ShouldAbortAndThrowException() {
    // Arrange
    prepareContractBehaviors(createAssetMocks());
    DatabaseException toThrow = mock(DatabaseException.class);
    when(ledger.get(any())).thenThrow(toThrow);
    service =
        new LedgerValidationService(
            config, transactionManager, clientKeyValidator, contractManager, proofComposer);

    // Act
    Throwable thrown = catchThrowable(() -> service.retrieve(ID, -1));

    // Assert
    assertThat(thrown).isEqualTo(toThrow);
    verify(ledger).get(ID);
    verify(transaction).abort();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void retrieve_AssetIdGivenButAssetNotFound_ShouldThrowValidationException() {
    // Arrange
    int age = 1;
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    when(ledger.scan(any())).thenReturn(Lists.emptyList());
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    Throwable thrown = catchThrowable(() -> service.retrieve(ID, age));

    // Assert
    assertThat(thrown).isInstanceOf(ValidationException.class);
    assertThat(((ValidationException) thrown).getCode()).isEqualTo(StatusCode.ASSET_NOT_FOUND);
    verify(transaction).commit();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void retrieve_AssetIdWithoutAgeGivenButAssetNotFound_ShouldThrowValidationException() {
    // Arrange
    when(transactionManager.startWith()).thenReturn(transaction);
    doReturn(ledger).when(transaction).getLedger();
    when(ledger.get(any())).thenReturn(Optional.empty());
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    Throwable thrown = catchThrowable(() -> service.retrieve(ID, -1));

    // Assert
    assertThat(thrown).isInstanceOf(ValidationException.class);
    assertThat(((ValidationException) thrown).getCode()).isEqualTo(StatusCode.ASSET_NOT_FOUND);
    verify(transaction).commit();
  }

  @Test
  public void retrieve_AssetRetrievalRequestSignedByCorrectKeyGiven_ShouldCallInternalValidate() {
    // Arrange
    prepareContractBehaviors(createAssetMocks());
    List<LedgerValidator> validators = createValidators();
    byte[] serialized = AssetProofRetrievalRequest.serialize(ID, AGE, ENTITY_ID, KEY_VERSION);
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_A);
    when(clientKeyValidator.getValidator(ENTITY_ID, KEY_VERSION))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    service =
        spy(
            new LedgerValidationService(
                config,
                transactionManager,
                clientKeyValidator,
                contractManager,
                proofComposer,
                validators));

    // Act
    service.retrieve(
        new AssetProofRetrievalRequest(ID, AGE, ENTITY_ID, KEY_VERSION, signer.sign(serialized)));

    // Assert
    verify(service).retrieve(ID, AGE);
  }

  @Test
  public void retrieve_AssetRetrievalRequestSignedByWrongKeyGiven_ShouldThrowSignatureException() {
    // Arrange
    byte[] serialized = AssetProofRetrievalRequest.serialize(ID, AGE, ENTITY_ID, KEY_VERSION);
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_B);
    when(clientKeyValidator.getValidator(ENTITY_ID, KEY_VERSION))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));

    // Act
    assertThatThrownBy(
            () ->
                service.retrieve(
                    new AssetProofRetrievalRequest(
                        ID, AGE, ENTITY_ID, KEY_VERSION, signer.sign(serialized))))
        .isInstanceOf(SignatureException.class);

    // Assert
    verify(service, never()).retrieve(ID, AGE);
  }

  @Test
  public void validateEach_ValidatorsAndAssetWithDeprecatedTracerGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonObject contractArgument = JsonObject.EMPTY_JSON_OBJECT;
    when(asset.argument()).thenReturn(jsonpSerDe.serialize(contractArgument));
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    DeprecatedLedgerTracer tracer = mock(DeprecatedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.DEPRECATED))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jsonpSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void
      validateEach_ValidatorsAndAssetWithDeprecatedTracerAndV2ArgumentGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonObject contractArgument = JsonObject.EMPTY_JSON_OBJECT;
    String argument = Argument.format(contractArgument, "nonce", Collections.emptyList());
    when(asset.argument()).thenReturn(argument);
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    DeprecatedLedgerTracer tracer = mock(DeprecatedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.DEPRECATED))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jsonpSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void validateEach_ValidatorsAndAssetWithJsonpBasedTracerGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonObject contractArgument = JsonObject.EMPTY_JSON_OBJECT;
    when(asset.argument()).thenReturn(jsonpSerDe.serialize(contractArgument));
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    JsonpBasedLedgerTracer tracer = mock(JsonpBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JSONP_JSON);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.JSONP_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jsonpSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void
      validateEach_ValidatorsAndAssetWithJsonpBasedTracerAndV2ArgumentGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonObject contractArgument = JsonObject.EMPTY_JSON_OBJECT;
    String argument = Argument.format(contractArgument, "nonce", Collections.emptyList());
    when(asset.argument()).thenReturn(argument);
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    JsonpBasedLedgerTracer tracer = mock(JsonpBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JSONP_JSON);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.JSONP_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jsonpSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void validateEach_ValidatorsAndAssetWithJacksonBasedTracerGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonNode contractArgument = mapper.createObjectNode();
    when(asset.argument()).thenReturn(jacksonSerDe.serialize(contractArgument));
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    JacksonBasedLedgerTracer tracer = mock(JacksonBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JACKSON_JSON);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.JACKSON_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jacksonSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void
      validateEach_ValidatorsAndAssetWithJacksonBasedTracerAndV2ArgumentGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    JsonNode contractArgument = mapper.createObjectNode();
    String argument = Argument.format(contractArgument, "nonce", Collections.emptyList());
    when(asset.argument()).thenReturn(argument);
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    JacksonBasedLedgerTracer tracer = mock(JacksonBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JACKSON_JSON);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.JACKSON_JSON))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, jacksonSerDe.serialize(contractArgument), null);
    verify(validator).validate(tracer, contract, asset);
  }

  @Test
  public void
      validateEach_ValidatorsAndAssetWithStringBasedTracerAndV2ArgumentGiven_ShouldValidateProperly() {
    // Arrange
    List<LedgerValidator> validators = new ArrayList<>();
    LedgerValidator validator = mock(LedgerValidator.class);
    validators.add(validator);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(CONTRACT_ID_IN_ASSET);
    when(asset.input()).thenReturn(ANY_INPUT);
    String contractArgument = "contract-argument";
    String argument = Argument.format(contractArgument, "nonce", Collections.emptyList());
    when(asset.argument()).thenReturn(argument);
    ContractEntry entry = mock(ContractEntry.class);
    ContractMachine contract = mock(ContractMachine.class);
    StringBasedLedgerTracer tracer = mock(StringBasedLedgerTracer.class);
    doNothing().when(tracer).setInput(anyString());
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.STRING);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(validator.validate(any(), any(), any())).thenReturn(StatusCode.OK);
    service =
        spy(
            new LedgerValidationService(
                config, transactionManager, clientKeyValidator, contractManager, proofComposer));
    when(service.getLedgerTracerBase(DeserializationType.STRING))
        .thenReturn((LedgerTracerBase) tracer);

    // Act
    StatusCode status = service.validateEach(validators, asset);

    // Assert
    assertThat(status).isEqualTo(StatusCode.OK);
    verify(contract).invoke(tracer, contractArgument, null);
    verify(validator).validate(tracer, contract, asset);
  }
}
