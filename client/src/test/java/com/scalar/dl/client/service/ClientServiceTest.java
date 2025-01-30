package com.scalar.dl.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.ClientMode;
import com.scalar.dl.client.config.DigitalSignatureIdentityConfig;
import com.scalar.dl.client.config.HmacIdentityConfig;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class ClientServiceTest {
  private static final String ANY_PRIVATE_KEY =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
          + "-----END EC PRIVATE KEY-----";
  private static final String ANY_CERT = "cert";
  private static final String ANY_SECRET_KEY = "secret_key";
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_KEY_VERSION = 1;
  private static final String ANY_CONTRACT_ID = "id";
  private static final String ANY_CONTRACT_NAME = "name";
  private static final String ANY_CONTRACT_PATH = "/dev/null";
  private static final String ANY_CONTRACT_ARGUMENT = "{\"asset_id\":\"asset_id\"}";
  private static final String ANY_CONTRACT_RESULT = "{\"result\":\"contract_result\"}";
  private static final String ANY_ASSET_ID = "asset_id";
  private static final String ANY_FUNCTION_ID = "id";
  private static final String ANY_FUNCTION_NAME = "name";
  private static final String ANY_FUNCTION_PATH = "/dev/null";
  private static final String ANY_FUNCTION_RESULT = "{\"result\":\"function_result\"}";
  private static final byte[] ANY_HASH = "hash".getBytes(StandardCharsets.UTF_8);
  @Mock private LedgerClient client;
  @Mock private AuditorClient auditorClient;
  private ClientServiceHandler handler;
  private RequestSigner signer;
  @Mock private ClientConfig config;
  @Mock private DigitalSignatureIdentityConfig digitalSignatureIdentityConfig;
  @Mock private HmacIdentityConfig hmacIdentityConfig;
  private ClientService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = spy(new DefaultClientServiceHandler(client, auditorClient));
    signer = spy(new RequestSigner(new DigitalSignatureSigner(ANY_PRIVATE_KEY)));
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(digitalSignatureIdentityConfig.getCert()).thenReturn(ANY_CERT);
    when(digitalSignatureIdentityConfig.getEntityId()).thenReturn(ANY_ENTITY_ID);
    when(digitalSignatureIdentityConfig.getCertVersion()).thenReturn(ANY_KEY_VERSION);
    when(config.getHmacIdentityConfig()).thenReturn(hmacIdentityConfig);
    when(hmacIdentityConfig.getEntityId()).thenReturn(ANY_ENTITY_ID);
    when(hmacIdentityConfig.getSecretKeyVersion()).thenReturn(ANY_KEY_VERSION);
    when(hmacIdentityConfig.getSecretKey()).thenReturn(ANY_SECRET_KEY);
    service = spy(new ClientService(config, handler, signer));
  }

  @Test
  public void registerCertificate_CorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);

    // Act
    service.registerCertificate();

    // Assert
    CertificateRegistrationRequest expected =
        CertificateRegistrationRequest.newBuilder()
            .setEntityId(ANY_ENTITY_ID)
            .setKeyVersion(ANY_KEY_VERSION)
            .setCertPem(ANY_CERT)
            .build();
    verify(client).register(expected);
  }

  @Test
  public void registerCertificate_SerializedBinaryGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    CertificateRegistrationRequest expected = CertificateRegistrationRequest.newBuilder().build();

    // Act
    service.registerCertificate(expected.toByteArray());

    // Assert
    verify(client).register(expected);
  }

  @Test
  public void registerCertificate_AuditorEnabledAndCorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    service.registerCertificate();

    // Assert
    CertificateRegistrationRequest expected =
        CertificateRegistrationRequest.newBuilder()
            .setEntityId(ANY_ENTITY_ID)
            .setKeyVersion(ANY_KEY_VERSION)
            .setCertPem(ANY_CERT)
            .build();
    InOrder inOrder = inOrder(auditorClient, client);
    inOrder.verify(auditorClient).register(expected);
    inOrder.verify(client).register(expected);
  }

  @Test
  public void registerSecret_CorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);

    // Act
    service.registerSecret();

    // Assert
    SecretRegistrationRequest expected =
        SecretRegistrationRequest.newBuilder()
            .setEntityId(ANY_ENTITY_ID)
            .setKeyVersion(ANY_KEY_VERSION)
            .setSecretKey(ANY_SECRET_KEY)
            .build();
    verify(client).register(expected);
  }

  @Test
  public void registerSecret_SerializedBinaryGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    SecretRegistrationRequest expected = SecretRegistrationRequest.newBuilder().build();

    // Act
    service.registerSecret(expected.toByteArray());

    // Assert
    verify(client).register(expected);
  }

  @Test
  public void registerSecret_AuditorEnabledAndCorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    service.registerSecret();

    // Assert
    SecretRegistrationRequest expected =
        SecretRegistrationRequest.newBuilder()
            .setEntityId(ANY_ENTITY_ID)
            .setKeyVersion(ANY_KEY_VERSION)
            .setSecretKey(ANY_SECRET_KEY)
            .build();
    InOrder inOrder = inOrder(auditorClient, client);
    inOrder.verify(auditorClient).register(expected);
    inOrder.verify(client).register(expected);
  }

  @Test
  public void registerFunction_CorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);

    // Act
    service.registerFunction(ANY_FUNCTION_ID, ANY_FUNCTION_NAME, ANY_FUNCTION_PATH);

    // Assert
    verify(client).register(any(FunctionRegistrationRequest.class));
  }

  @Test
  public void registerFunction_SerializedBinaryGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    FunctionRegistrationRequest expected = FunctionRegistrationRequest.newBuilder().build();

    // Act
    service.registerFunction(expected.toByteArray());

    // Assert
    verify(client).register(expected);
    verify(config, never()).getDigitalSignatureIdentityConfig();
    verify(config, never()).getHmacIdentityConfig();
  }

  @Test
  public void registerContract_CorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);

    // Act
    service.registerContract(ANY_CONTRACT_ID, ANY_CONTRACT_NAME, ANY_CONTRACT_PATH);

    // Assert
    verify(digitalSignatureIdentityConfig).getEntityId();
    verify(digitalSignatureIdentityConfig).getCertVersion();
    verify(hmacIdentityConfig, never()).getEntityId();
    verify(hmacIdentityConfig, never()).getSecretKeyVersion();
    // the reason why it does not verify the request is that the internal sign method
    // (SignatureSigner.sign) uses random number and the produced signature
    // changes every time.
    verify(client).register(any(ContractRegistrationRequest.class));
  }

  @Test
  public void registerContract_HmacAuthConfiguredAndCorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);

    // Act
    service.registerContract(ANY_CONTRACT_ID, ANY_CONTRACT_NAME, ANY_CONTRACT_PATH);

    // Assert
    verify(hmacIdentityConfig).getEntityId();
    verify(hmacIdentityConfig).getSecretKeyVersion();
    verify(digitalSignatureIdentityConfig, never()).getEntityId();
    verify(digitalSignatureIdentityConfig, never()).getCertVersion();
    // the reason why it does not verify the request is that the internal sign method
    // (SignatureSigner.sign) uses random number and the produced signature
    // changes every time.
    verify(client).register(any(ContractRegistrationRequest.class));
  }

  @Test
  public void registerContract_SerializedBinaryGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    ContractRegistrationRequest expected = ContractRegistrationRequest.newBuilder().build();

    // Act
    service.registerContract(expected.toByteArray());

    // Assert
    verify(client).register(expected);
    verify(config, never()).getDigitalSignatureIdentityConfig();
    verify(config, never()).getHmacIdentityConfig();
    verify(signer, never()).sign(any(ContractRegistrationRequest.Builder.class));
  }

  @Test
  public void registerContract_AuditorEnabledCorrectInputsGiven_ShouldRegisterProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    service.registerContract(ANY_CONTRACT_ID, ANY_CONTRACT_NAME, ANY_CONTRACT_PATH);

    // Assert
    verify(digitalSignatureIdentityConfig).getEntityId();
    verify(digitalSignatureIdentityConfig).getCertVersion();
    // the reason why it does not verify the request is that the internal sign method
    // (SignatureSigner.sign) uses random number and the produced signature
    // changes every time.
    InOrder inOrder = inOrder(auditorClient, client);
    inOrder.verify(auditorClient).register(any(ContractRegistrationRequest.class));
    inOrder.verify(client).register(any(ContractRegistrationRequest.class));
  }

  @Test
  public void listContracts_SerializedBinaryGiven_ShouldListProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    ContractsListingRequest expected = ContractsListingRequest.newBuilder().build();

    // Act
    service.listContracts(expected.toByteArray());

    // Assert
    verify(client).list(expected);
    verify(config, never()).getDigitalSignatureIdentityConfig();
    verify(config, never()).getHmacIdentityConfig();
    verify(signer, never()).sign(any(ContractsListingRequest.Builder.class));
  }

  @Test
  public void executeContract_CorrectInputsGiven_ShouldExecuteProperly() {
    // Arrange
    handler = new DefaultClientServiceHandler(client, null);
    service = spy(new ClientService(config, handler, signer));
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    JsonObject argument = Json.createReader(new StringReader(ANY_CONTRACT_ARGUMENT)).readObject();
    ContractExecutionResult result =
        new ContractExecutionResult(ANY_CONTRACT_RESULT, ANY_FUNCTION_RESULT, null, null);
    when(client.execute(any(ContractExecutionRequest.class), any())).thenReturn(result);

    // Act
    ContractExecutionResult actual = service.executeContract(ANY_CONTRACT_ID, argument);

    // Assert
    verify(digitalSignatureIdentityConfig).getEntityId();
    verify(digitalSignatureIdentityConfig).getCertVersion();
    verify(hmacIdentityConfig, never()).getEntityId();
    verify(hmacIdentityConfig, never()).getSecretKeyVersion();
    // the reason why it does not verify the result of sign() is that
    // the internal sign method uses random number and the produced signature changes every time.
    verify(client).execute(any(ContractExecutionRequest.class), any(ThrowableFunction.class));
    assertThat(actual.getContractResult()).isEqualTo(Optional.of(ANY_CONTRACT_RESULT));
    assertThat(actual.getFunctionResult()).isEqualTo(Optional.of(ANY_FUNCTION_RESULT));
  }

  @Test
  public void executeContract_HmacAuthConfiguredAndCorrectInputsGiven_ShouldExecuteProperly() {
    // Arrange
    handler = new DefaultClientServiceHandler(client, null);
    service = spy(new ClientService(config, handler, signer));
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    JsonObject argument = Json.createReader(new StringReader(ANY_CONTRACT_ARGUMENT)).readObject();
    ContractExecutionResult result =
        new ContractExecutionResult(ANY_CONTRACT_RESULT, ANY_FUNCTION_RESULT, null, null);
    when(client.execute(any(ContractExecutionRequest.class), any())).thenReturn(result);

    // Act
    ContractExecutionResult actual = service.executeContract(ANY_CONTRACT_ID, argument);

    // Assert
    verify(hmacIdentityConfig).getEntityId();
    verify(hmacIdentityConfig).getSecretKeyVersion();
    verify(digitalSignatureIdentityConfig, never()).getEntityId();
    verify(digitalSignatureIdentityConfig, never()).getCertVersion();
    // the reason why it does not verify the result of sign() is that
    // the internal sign method uses random number and the produced signature changes every time.
    verify(client).execute(any(ContractExecutionRequest.class), any(ThrowableFunction.class));
    assertThat(actual.getContractResult()).isEqualTo(Optional.of(ANY_CONTRACT_RESULT));
    assertThat(actual.getFunctionResult()).isEqualTo(Optional.of(ANY_FUNCTION_RESULT));
  }

  @Test
  public void executeContract_SerializedBinaryGiven_ShouldExecuteProperly() {
    // Arrange
    handler = new DefaultClientServiceHandler(client, null);
    service = spy(new ClientService(config, handler, null));
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    ContractExecutionRequest expected = ContractExecutionRequest.newBuilder().build();

    // Act
    service.executeContract(expected.toByteArray());

    // Assert
    verify(client).execute(any(ContractExecutionRequest.class), any(ThrowableFunction.class));
    verify(config, never()).getDigitalSignatureIdentityConfig();
    verify(config, never()).getHmacIdentityConfig();
    verify(signer, never()).sign(any(ContractExecutionRequest.Builder.class));
  }

  @Test
  public void executeContract_AuditorEnabledCorrectInputsGiven_ShouldExecuteProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);
    JsonObject argument = Json.createReader(new StringReader(ANY_CONTRACT_ARGUMENT)).readObject();
    ContractExecutionResult result =
        new ContractExecutionResult(ANY_CONTRACT_RESULT, ANY_FUNCTION_RESULT, null, null);
    when(client.execute(any(ContractExecutionRequest.class), any())).thenReturn(result);
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    // Act
    ContractExecutionResult actual = service.executeContract(ANY_CONTRACT_ID, argument);

    // Assert
    verify(digitalSignatureIdentityConfig).getEntityId();
    verify(digitalSignatureIdentityConfig).getCertVersion();
    // the reason why it does not verify the result of sign() is that
    // the internal sign method uses random number and the produced signature changes every time.
    InOrder inOrder = inOrder(auditorClient, client);
    inOrder.verify(auditorClient).order(any(ContractExecutionRequest.class));
    inOrder
        .verify(client)
        .execute(any(ContractExecutionRequest.class), any(ThrowableFunction.class));
    assertThat(actual.getContractResult()).isEqualTo(Optional.of(ANY_CONTRACT_RESULT));
    assertThat(actual.getFunctionResult()).isEqualTo(Optional.of(ANY_FUNCTION_RESULT));
  }

  @Test
  public void validateLedger_CorrectInputsGiven_ShouldValidateProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    AssetProof ledgerProof = mock(AssetProof.class);
    LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, ledgerProof, null);
    when(client.validate(any(LedgerValidationRequest.class))).thenReturn(result);

    // Act
    LedgerValidationResult actual = service.validateLedger(ANY_ASSET_ID);

    // Assert
    verify(digitalSignatureIdentityConfig).getEntityId();
    verify(digitalSignatureIdentityConfig).getCertVersion();
    verify(hmacIdentityConfig, never()).getEntityId();
    verify(hmacIdentityConfig, never()).getSecretKeyVersion();
    // the reason why it does not verify the result of sign() is that
    // the internal sign method uses random number and the produced signature changes every time.
    verify(client).validate(any(LedgerValidationRequest.class));
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof()).isEqualTo(Optional.of(ledgerProof));
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateLedger_HmacAuthConfiguredAndCorrectInputsGiven_ShouldValidateProperly() {
    // Arrange
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    AssetProof ledgerProof = mock(AssetProof.class);
    LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, ledgerProof, null);
    when(client.validate(any(LedgerValidationRequest.class))).thenReturn(result);

    // Act
    LedgerValidationResult actual = service.validateLedger(ANY_ASSET_ID);

    // Assert
    verify(hmacIdentityConfig).getEntityId();
    verify(hmacIdentityConfig).getSecretKeyVersion();
    verify(digitalSignatureIdentityConfig, never()).getEntityId();
    verify(digitalSignatureIdentityConfig, never()).getCertVersion();
    // the reason why it does not verify the result of sign() is that
    // the internal sign method uses random number and the produced signature changes every time.
    verify(client).validate(any(LedgerValidationRequest.class));
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof()).isEqualTo(Optional.of(ledgerProof));
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateLedger_AuditorEnabled_ShouldValidateWithExecuteContract() {
    // Arrange
    String contractId = "my-validate-ledger";
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);
    when(config.getAuditorLinearizableValidationContractId()).thenReturn(contractId);
    AssetProof ledgerProof = mock(AssetProof.class);
    AssetProof auditorProof = mock(AssetProof.class);
    when(ledgerProof.getHash()).thenReturn(ANY_HASH);
    when(auditorProof.getHash()).thenReturn(ANY_HASH);
    ContractExecutionResult result =
        new ContractExecutionResult(
            null,
            null,
            Collections.singletonList(ledgerProof),
            Collections.singletonList(auditorProof));
    doReturn(result).when(service).executeContract(anyString(), any(JsonObject.class));

    // Act
    LedgerValidationResult actual = service.validateLedger(ANY_ASSET_ID);

    // Assert
    verify(service)
        .executeContract(
            contractId,
            Json.createObjectBuilder()
                .add(ClientService.VALIDATE_LEDGER_ASSET_ID_KEY, ANY_ASSET_ID)
                .add(ClientService.VALIDATE_LEDGER_START_AGE_KEY, 0)
                .add(ClientService.VALIDATE_LEDGER_END_AGE_KEY, Integer.MAX_VALUE)
                .build());
    verify(client, never()).validate(any(LedgerValidationRequest.class));
    assertThat(actual.getLedgerProof()).isEqualTo(Optional.of(ledgerProof));
    assertThat(actual.getAuditorProof()).isEqualTo(Optional.of(auditorProof));
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void
      validateLedger_AuditorEnabledAndLinearizableValidationEnabledAndAgeRangeGiven_ShouldValidateWithExecuteContractWithSpecifiedRange() {
    // Arrange
    String contractId = "my-validate-ledger";
    when(config.getClientMode()).thenReturn(ClientMode.CLIENT);
    when(config.isAuditorEnabled()).thenReturn(true);
    when(config.getAuditorLinearizableValidationContractId()).thenReturn(contractId);
    AssetProof ledgerProof = mock(AssetProof.class);
    AssetProof auditorProof = mock(AssetProof.class);
    when(ledgerProof.getHash()).thenReturn(ANY_HASH);
    when(auditorProof.getHash()).thenReturn(ANY_HASH);
    ContractExecutionResult result =
        new ContractExecutionResult(
            null,
            null,
            Collections.singletonList(ledgerProof),
            Collections.singletonList(auditorProof));
    doReturn(result).when(service).executeContract(anyString(), any(JsonObject.class));
    int startAge = 10;
    int endAge = 20;

    // Act
    LedgerValidationResult actual = service.validateLedger(ANY_ASSET_ID, startAge, endAge);

    // Assert
    verify(service)
        .executeContract(
            contractId,
            Json.createObjectBuilder()
                .add(ClientService.VALIDATE_LEDGER_ASSET_ID_KEY, ANY_ASSET_ID)
                .add(ClientService.VALIDATE_LEDGER_START_AGE_KEY, startAge)
                .add(ClientService.VALIDATE_LEDGER_END_AGE_KEY, endAge)
                .build());
    verify(client, never()).validate(any(LedgerValidationRequest.class));
    assertThat(actual.getLedgerProof()).isEqualTo(Optional.of(ledgerProof));
    assertThat(actual.getAuditorProof()).isEqualTo(Optional.of(auditorProof));
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  public void
      validateLedger_AuditorEnabledAndLinearizableValidationEnabledInIntermediaryMode_ShouldThrowUnsupportedOperationException() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    Throwable thrown = catchThrowable(() -> service.validateLedger((byte[]) null));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(UnsupportedOperationException.class);
    verify(service, never()).executeContract(anyString(), any(JsonObject.class));
    verify(client, never()).validate(any(LedgerValidationRequest.class));
  }

  @Test
  public void validateLedger_SerializedBinaryGiven_ShouldValidateProperly() {
    // Arrange
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    LedgerValidationRequest expected = LedgerValidationRequest.newBuilder().build();

    // Act
    service.validateLedger(expected.toByteArray());

    // Assert
    verify(client).validate(expected);
    verify(config, never()).getDigitalSignatureIdentityConfig();
    verify(config, never()).getHmacIdentityConfig();
    verify(signer, never()).sign(any(LedgerValidationRequest.Builder.class));
  }
}
