package com.scalar.dl.client.service;

import static com.scalar.dl.client.service.AbstractAuditorClient.STATUS_TRAILER_KEY;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.auditor.ordering.LockRecoveryResult;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.config.GrpcClientConfig;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.model.TransactionStatePurgeResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.rpc.AssetLockRecoveryRequest;
import com.scalar.dl.rpc.AssetLockRecoveryResponse;
import com.scalar.dl.rpc.AuditorPrivilegedGrpc;
import com.scalar.dl.rpc.Status;
import com.scalar.dl.rpc.TransactionStatePurgeRequest;
import com.scalar.dl.rpc.TransactionStatePurgeResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class AuditorClientTest {
  private static final String ANY_ERROR_MESSAGE = "any error message";
  private static final long ANY_DEADLINE_MILLIS = 30000;
  @Mock private AuditorPrivilegedGrpc.AuditorPrivilegedBlockingStub auditorPrivilegedStub;
  @Mock private AuditorPrivilegedGrpc.AuditorPrivilegedBlockingStub anotherPrivilegedStub;
  @Mock private TargetConfig config;
  @Mock private GrpcClientConfig grpcClientConfig;
  private AuditorClient client;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(config.getGrpcClientConfig()).thenReturn(grpcClientConfig);
    when(grpcClientConfig.getDeadlineDurationMillis()).thenReturn(ANY_DEADLINE_MILLIS);
    client = new AuditorClient(config, null, auditorPrivilegedStub);
  }

  @Test
  public void recover_CorrectRequestGiven_ShouldReturnResult() {
    // Arrange
    AssetLockRecoveryRequest request = AssetLockRecoveryRequest.newBuilder().build();
    AssetLockRecoveryResponse response =
        AssetLockRecoveryResponse.newBuilder()
            .setResult(AssetLockRecoveryResponse.Result.SUCCEEDED)
            .build();
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.recoverAssetLock(request)).thenReturn(response);

    // Act
    LockRecoveryResult result = client.recover(request);

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).recoverAssetLock(request);
    assertThat(result).isEqualTo(LockRecoveryResult.SUCCEEDED);
  }

  @Test
  public void recover_ExceptionWithStatusMetadataThrown_ShouldThrowClientExceptionWithSpecified() {
    // Arrange
    AssetLockRecoveryRequest request = AssetLockRecoveryRequest.newBuilder().build();
    StatusCode expected = StatusCode.RUNTIME_ERROR;
    Metadata trailers = new Metadata();
    trailers.put(
        STATUS_TRAILER_KEY,
        Status.newBuilder().setCode(expected.get()).setMessage(ANY_ERROR_MESSAGE).build());
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.recoverAssetLock(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.recover(request));

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).recoverAssetLock(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode()).isEqualTo(expected);
    assertThat(thrown.getMessage()).isEqualTo(ANY_ERROR_MESSAGE);
  }

  @Test
  public void recover_ExceptionWithoutStatusMetadataThrown_ShouldThrowClientExceptionWithUnknown() {
    // Arrange
    AssetLockRecoveryRequest request = AssetLockRecoveryRequest.newBuilder().build();
    Metadata trailers = new Metadata();
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.recoverAssetLock(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.recover(request));

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).recoverAssetLock(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode())
        .isEqualTo(StatusCode.UNKNOWN_TRANSACTION_STATUS);
    assertThat(thrown.getMessage()).isEqualTo(io.grpc.Status.INTERNAL.getCode().toString());
  }

  @Test
  public void recover_ExceptionWithoutMetadataThrown_ShouldThrowClientExceptionWithUnknown() {
    // Arrange
    AssetLockRecoveryRequest request = AssetLockRecoveryRequest.newBuilder().build();
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL);
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.recoverAssetLock(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.recover(request));

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).recoverAssetLock(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode())
        .isEqualTo(StatusCode.UNKNOWN_TRANSACTION_STATUS);
    assertThat(thrown.getMessage()).isEqualTo(io.grpc.Status.INTERNAL.getCode().toString());
  }

  @Test
  public void purgeTransactionStates_CorrectRequestGiven_ShouldReturnResult() {
    // Arrange
    TransactionStatePurgeRequest request = TransactionStatePurgeRequest.newBuilder().build();
    TransactionStatePurgeResponse response =
        TransactionStatePurgeResponse.newBuilder()
            .setTotalTargets(3)
            .setPurged(2)
            .setSkipped(1)
            .build();
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.purgeTransactionStates(request)).thenReturn(response);

    // Act
    TransactionStatePurgeResult result = client.purgeTransactionStates(request);

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).purgeTransactionStates(request);
    assertThat(result.getTotalTargets()).isEqualTo(3);
    assertThat(result.getPurged()).isEqualTo(2);
    assertThat(result.getSkipped()).isEqualTo(1);
  }

  @Test
  public void
      purgeTransactionStates_ExceptionWithStatusMetadataThrown_ShouldThrowClientExceptionWithSpecified() {
    // Arrange
    TransactionStatePurgeRequest request = TransactionStatePurgeRequest.newBuilder().build();
    StatusCode expected = StatusCode.RUNTIME_ERROR;
    Metadata trailers = new Metadata();
    trailers.put(
        STATUS_TRAILER_KEY,
        Status.newBuilder().setCode(expected.get()).setMessage(ANY_ERROR_MESSAGE).build());
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.purgeTransactionStates(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.purgeTransactionStates(request));

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).purgeTransactionStates(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode()).isEqualTo(expected);
    assertThat(thrown.getMessage()).isEqualTo(ANY_ERROR_MESSAGE);
  }

  @Test
  public void
      purgeTransactionStates_ExceptionWithoutStatusMetadataThrown_ShouldThrowClientExceptionWithUnknown() {
    // Arrange
    TransactionStatePurgeRequest request = TransactionStatePurgeRequest.newBuilder().build();
    Metadata trailers = new Metadata();
    StatusRuntimeException toThrow = new StatusRuntimeException(io.grpc.Status.INTERNAL, trailers);
    when(auditorPrivilegedStub.withDeadlineAfter(anyLong(), any(TimeUnit.class)))
        .thenReturn(anotherPrivilegedStub);
    when(anotherPrivilegedStub.purgeTransactionStates(request)).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> client.purgeTransactionStates(request));

    // Assert
    verify(auditorPrivilegedStub).withDeadlineAfter(ANY_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    verify(anotherPrivilegedStub).purgeTransactionStates(request);
    assertThat(thrown).isInstanceOf(ClientException.class).hasCause(toThrow);
    assertThat(((ClientException) thrown).getStatusCode())
        .isEqualTo(StatusCode.UNKNOWN_TRANSACTION_STATUS);
    assertThat(thrown.getMessage()).isEqualTo(io.grpc.Status.INTERNAL.getCode().toString());
  }
}
