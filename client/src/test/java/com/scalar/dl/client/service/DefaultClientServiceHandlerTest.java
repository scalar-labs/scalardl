package com.scalar.dl.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.AssetProof;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.ExecutionValidationRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultClientServiceHandlerTest {
  private static final String ANY_CONTRACT_RESULT = "{\"result\":\"contract_result\"}";
  private static final String DEFAULT_NAMESPACE = "default";
  private static final String CUSTOM_NAMESPACE = "custom_namespace";
  private static final String ASSET_ID_1 = "asset_id_1";
  private static final String ASSET_ID_2 = "asset_id_2";
  private static final int ANY_AGE = 1;
  private static final ByteString ANY_HASH =
      ByteString.copyFrom("hash".getBytes(StandardCharsets.UTF_8));
  private static final ByteString DIFFERENT_HASH =
      ByteString.copyFrom("different_hash".getBytes(StandardCharsets.UTF_8));

  @Mock private AbstractLedgerClient ledgerClient;
  @Mock private AbstractAuditorClient auditorClient;
  private DefaultClientServiceHandler handler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new DefaultClientServiceHandler(ledgerClient, auditorClient);
  }

  @Test
  public void executeContract_SameProofsGiven_ShouldReturnSuccessfully() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof proof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act
    ContractExecutionResult result = handler.executeContract(request);

    // Assert
    assertThat(result).isNotNull();
  }

  @Test
  public void
      executeContract_SameMultipleProofsWithCustomNamespaceGiven_ShouldReturnSuccessfullyWithMultipleProofs() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof proof1 =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof proof2 =
        AssetProof.newBuilder()
            .setNamespace(CUSTOM_NAMESPACE)
            .setAssetId(ASSET_ID_2)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof1)
            .addProofs(proof2)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof1)
            .addProofs(proof2)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act
    ContractExecutionResult result = handler.executeContract(request);

    // Assert
    assertThat(result).isNotNull();
  }

  @Test
  public void executeContract_DifferentNamespaceWithSameProofsGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof ledgerProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof auditorProof =
        AssetProof.newBuilder()
            .setNamespace(CUSTOM_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(ledgerProof)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(auditorProof)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void executeContract_DifferentHashForSameNamespaceAndAssetIdGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof ledgerProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof auditorProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(DIFFERENT_HASH) // Different hash for same namespace+assetId
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(ledgerProof)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(auditorProof)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void executeContract_DifferentAgeForSameNamespaceAndAssetIdGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof ledgerProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof auditorProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE + 1) // Different age for same namespace+assetId
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(ledgerProof)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(auditorProof)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void executeContract_AuditorProofMissingForNamespaceAssetKeyGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof ledgerProof1 =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof ledgerProof2 =
        AssetProof.newBuilder()
            .setNamespace(CUSTOM_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof auditorProof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    // Auditor has a different namespace+assetId combination
    AssetProof auditorProof2 =
        AssetProof.newBuilder()
            .setNamespace(CUSTOM_NAMESPACE)
            .setAssetId(ASSET_ID_2) // Different asset ID
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(ledgerProof1)
            .addProofs(ledgerProof2)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(auditorProof)
            .addProofs(auditorProof2)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void executeContract_DifferentContractResultGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof proof =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof)
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult("{\"result\":\"different_result\"}") // Different contract result
            .addProofs(proof)
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void executeContract_DifferentProofsCountGiven_ShouldThrowException() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    ExecutionOrderingResponse orderingResponse = mock(ExecutionOrderingResponse.class);
    when(orderingResponse.getSignature()).thenReturn(ByteString.EMPTY);
    when(auditorClient.order(any(ContractExecutionRequest.class))).thenReturn(orderingResponse);

    AssetProof proof1 =
        AssetProof.newBuilder()
            .setNamespace(DEFAULT_NAMESPACE)
            .setAssetId(ASSET_ID_1)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    AssetProof proof2 =
        AssetProof.newBuilder()
            .setNamespace(CUSTOM_NAMESPACE)
            .setAssetId(ASSET_ID_2)
            .setAge(ANY_AGE)
            .setHash(ANY_HASH)
            .build();
    ContractExecutionResponse ledgerResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof1)
            .addProofs(proof2) // Two proofs
            .build();
    ContractExecutionResponse auditorResponse =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .addProofs(proof1) // Only one proof
            .build();

    when(ledgerClient.execute(any(ContractExecutionRequest.class), any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> func =
                  invocation.getArgument(1);
              func.apply(ledgerResponse);
              return new ContractExecutionResult(
                  ledgerResponse.getContractResult(), null, null, null);
            });
    when(auditorClient.validate(any(ExecutionValidationRequest.class))).thenReturn(auditorResponse);

    // Act & Assert
    assertThatThrownBy(() -> handler.executeContract(request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void createNamespace_ProperRequestGiven_ShouldCreateNamespace() {
    // Arrange
    NamespaceCreationRequest request =
        NamespaceCreationRequest.newBuilder().setNamespace("test_namespace").build();

    // Act
    handler.createNamespace(request);

    // Assert
    verify(ledgerClient).create(request);
  }

  @Test
  public void listNamespaces_ProperRequestGiven_ShouldReturnNamespaces() {
    // Arrange
    NamespacesListingRequest request = NamespacesListingRequest.newBuilder().build();
    String expectedJson = "{\"namespaces\":[\"ns1\",\"ns2\"]}";
    when(ledgerClient.list(request)).thenReturn(expectedJson);

    // Act
    String result = handler.listNamespaces(request);

    // Assert
    verify(ledgerClient).list(request);
    assertThat(result).isEqualTo(expectedJson);
  }

  @Test
  public void listNamespaces_PatternFilterGiven_ShouldReturnFilteredNamespaces() {
    // Arrange
    NamespacesListingRequest request =
        NamespacesListingRequest.newBuilder().setPattern("test").build();
    String expectedJson = "{\"namespaces\":[\"test_namespace\"]}";
    when(ledgerClient.list(request)).thenReturn(expectedJson);

    // Act
    String result = handler.listNamespaces(request);

    // Assert
    verify(ledgerClient).list(request);
    assertThat(result).isEqualTo(expectedJson);
  }
}
