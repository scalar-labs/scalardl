package com.scalar.dl.client.service;

import static com.scalar.dl.client.service.AbstractLedgerClient.STATUS_TRAILER_KEY;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.config.GrpcClientConfig;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.rpc.AssetProof;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.LedgerGrpc;
import com.scalar.dl.rpc.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class LedgerClientTest {
  private static final String ANY_ERROR_MESSAGE = "any error message";
  private static final String ANY_CONTRACT_RESULT = "{\"result\":\"contract_result\"}";
  private static final String ANY_FUNCTION_RESULT = "{\"result\":\"function_result\"}";
  private static final long ANY_DEADLINE_MILLIS = 30000;
  @Mock private LedgerGrpc.LedgerBlockingStub ledgerStub;
  @Mock private LedgerGrpc.LedgerBlockingStub anotherStub;
  @Mock private TargetConfig config;
  @Mock private GrpcClientConfig grpcClientConfig;
  private LedgerClient client;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(config.getGrpcClientConfig()).thenReturn(grpcClientConfig);
    when(grpcClientConfig.getDeadlineDurationMillis()).thenReturn(ANY_DEADLINE_MILLIS);
    client = new LedgerClient(config, ledgerStub, null);
  }

  @Test
  public void execute_CorrectContractExecutionRequestGiven_ShouldExecuteContract() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    AssetProof proof =
        AssetProof.newBuilder()
            .setAssetId("id")
            .setAge(1)
            .setNonce("nonce")
            .setInput("{}")
            .setHash(ByteString.EMPTY)
            .setPrevHash(ByteString.EMPTY)
            .setSignature(ByteString.EMPTY)
            .build();
    ContractExecutionResponse response =
        ContractExecutionResponse.newBuilder()
            .setContractResult(ANY_CONTRACT_RESULT)
            .setFunctionResult(ANY_FUNCTION_RESULT)
            .addProofs(proof)
            .build();
    when(ledgerStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(anotherStub);
    when(anotherStub.executeContract(request)).thenReturn(response);

    // Act
    ContractExecutionResult result = client.execute(request);

    // Assert
    verify(ledgerStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherStub).executeContract(request);
    assertThat(result.getContractResult()).isEqualTo(Optional.of(ANY_CONTRACT_RESULT));
    assertThat(result.getFunctionResult()).isEqualTo(Optional.of(ANY_FUNCTION_RESULT));
    assertThat(result.getLedgerProofs())
        .isEqualTo(Collections.singletonList(new com.scalar.dl.ledger.proof.AssetProof(proof)));
  }

  @Test
  public void execute_ExceptionWithStatusMetadataThrown_ShouldThrowClientExceptionWithSpecified() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    StatusCode expected = StatusCode.CONTRACT_CONTEXTUAL_ERROR;
    Metadata trailers = new Metadata();
    trailers.put(
        STATUS_TRAILER_KEY,
        Status.newBuilder().setCode(expected.get()).setMessage(ANY_ERROR_MESSAGE).build());
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(ledgerStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(anotherStub);
    when(anotherStub.executeContract(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.execute(request));

    // Assert
    verify(ledgerStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherStub).executeContract(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode()).isEqualTo(expected);
    assertThat(thrown.getMessage()).isEqualTo(ANY_ERROR_MESSAGE);
  }

  @Test
  public void execute_ExceptionWithoutStatusMetadataThrown_ShouldThrowClientExceptionWithUnknown() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    Metadata trailers = new Metadata();
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(ledgerStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(anotherStub);
    when(anotherStub.executeContract(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.execute(request));

    // Assert
    verify(ledgerStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherStub).executeContract(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode())
        .isEqualTo(StatusCode.UNKNOWN_TRANSACTION_STATUS);
    assertThat(thrown.getMessage()).isEqualTo(io.grpc.Status.INTERNAL.getCode().toString());
  }

  @Test
  public void execute_ExceptionWithoutMetadataThrown_ShouldThrowClientExceptionWithUnknown() {
    // Arrange
    ContractExecutionRequest request = ContractExecutionRequest.newBuilder().build();
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL);
    when(ledgerStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(anotherStub);
    when(anotherStub.executeContract(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.execute(request));

    // Assert
    verify(ledgerStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherStub).executeContract(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode())
        .isEqualTo(StatusCode.UNKNOWN_TRANSACTION_STATUS);
    assertThat(thrown.getMessage()).isEqualTo(io.grpc.Status.INTERNAL.getCode().toString());
  }
}
