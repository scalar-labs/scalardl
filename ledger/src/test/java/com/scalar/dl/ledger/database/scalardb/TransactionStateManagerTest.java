package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.IntValue;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.exception.ConflictException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TransactionStateManagerTest {
  private static final String SOME_TX_ID = "some_tx_id";
  @Mock private DistributedTransactionManager manager;
  @InjectMocks private TransactionStateManager stateManager;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void putCommit_TransactionAndUncommittedTransactionIdGiven_ShouldGetAndPutToTransaction()
      throws CrudException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(transaction.get(any(Get.class))).thenReturn(Optional.empty());

    // Act
    stateManager.putCommit(transaction, SOME_TX_ID);

    // Assert
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);

    ArgumentCaptor<Put> putCaptor = ArgumentCaptor.forClass(Put.class);
    verify(transaction).put(putCaptor.capture());
    Put putCaptured = putCaptor.getValue();
    assertThat(putCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    assertThat(putCaptured.getValues().get(TransactionStateManager.STATE).getAsInt())
        .isEqualTo(TransactionState.COMMITTED.get());
  }

  @Test
  public void
      putCommit_TransactionAndCommittedTransactionIdGiven_ShouldGetAndThrowConflictException()
          throws CrudException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.COMMITTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    Throwable thrown = catchThrowable(() -> stateManager.putCommit(transaction, SOME_TX_ID));

    // Assert
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
    assertThat(thrown).isExactlyInstanceOf(ConflictException.class);
  }

  @Test
  public void putCommit_TransactionAndAbortedTransactionIdGiven_ShouldGetAndThrowConflictException()
      throws CrudException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.ABORTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    Throwable thrown = catchThrowable(() -> stateManager.putCommit(transaction, SOME_TX_ID));

    // Assert
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
    assertThat(thrown).isExactlyInstanceOf(ConflictException.class);
  }

  @Test
  public void putAbort_UncommittedTransactionIdGiven_ShouldAbortAndReturnAborted()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    when(transaction.get(any(Get.class))).thenReturn(Optional.empty());

    // Act
    TransactionState state = stateManager.putAbort(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.ABORTED);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);

    ArgumentCaptor<Put> putCaptor = ArgumentCaptor.forClass(Put.class);
    verify(transaction).put(putCaptor.capture());
    Put putCaptured = putCaptor.getValue();
    assertThat(putCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    assertThat(putCaptured.getValues().get(TransactionStateManager.STATE).getAsInt())
        .isEqualTo(TransactionState.ABORTED.get());
  }

  @Test
  public void putAbort_CommittedTransactionIdGiven_ShouldAbortAndReturnCommitted()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.COMMITTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    TransactionState state = stateManager.putAbort(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.COMMITTED);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
  }

  @Test
  public void putAbort_AbortedTransactionIdGiven_ShouldAbortAndReturnAborted()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.ABORTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    TransactionState state = stateManager.putAbort(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.ABORTED);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
  }

  @Test
  public void
      putAbort_UncommittedTransactionIdGivenButCrudExceptionThrown_ShouldAbortAndReturnUnknown()
          throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.ABORTED.get())));
    CrudException toThrow = mock(CrudException.class);
    when(transaction.get(any(Get.class))).thenThrow(toThrow);

    // Act
    TransactionState state = stateManager.putAbort(SOME_TX_ID);

    // Assert
    verify(transaction, never()).commit();
    verify(transaction).abort();
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
  }

  @Test
  public void getState_CommittedTransactionIdGiven_ShouldReturnCommitted()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.COMMITTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    TransactionState state = stateManager.getState(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.COMMITTED);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
  }

  @Test
  public void getState_AbortedTransactionIdGiven_ShouldReturnAborted() throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    Result result = mock(Result.class);
    when(result.getValue(TransactionStateManager.STATE))
        .thenReturn(
            Optional.of(
                new IntValue(TransactionStateManager.STATE, TransactionState.ABORTED.get())));
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    TransactionState state = stateManager.getState(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.ABORTED);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
  }

  @Test
  public void getState_UncommittedTransactionIdGiven_ShouldReturnUnknown()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    when(transaction.get(any(Get.class))).thenReturn(Optional.empty());

    // Act
    TransactionState state = stateManager.getState(SOME_TX_ID);

    // Assert
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction).get(getCaptor.capture());
    Get getCaptured = getCaptor.getValue();
    assertThat(getCaptured.getPartitionKey().get().get(0).getAsString().get())
        .isEqualTo(SOME_TX_ID);
    verify(transaction, never()).put(any(Put.class));
  }

  @Test
  public void getState_CommittedTransactionIdGivenButCrudExceptionThrown_ShouldReturnUnknown()
      throws TransactionException {
    // Arrange
    DistributedTransaction transaction = mock(DistributedTransaction.class);
    when(manager.start()).thenReturn(transaction);
    CrudException toThrow = mock(CrudException.class);
    when(transaction.get(any(Get.class))).thenThrow(toThrow);

    // Act
    TransactionState state = stateManager.getState(SOME_TX_ID);

    // Assert
    verify(transaction).abort();
    verify(transaction, never()).commit();
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
  }
}
