package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitManager;
import com.scalar.db.transaction.jdbc.JdbcTransactionManager;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarTransactionManagerTest {
  private static final String NONCE = "nonce";
  private static final String SOME_ASSET_ID = "asset_id";
  private static final int SOME_ASSET_AGE = 1;
  @Mock private DistributedTransactionManager manager;
  @Mock private TamperEvidentAssetComposer assetComposer;
  @Mock private AssetProofComposer proofComposer;
  @Mock private TransactionStateManager stateManager;
  @Mock private ContractExecutionRequest request;
  @Mock private LedgerConfig config;
  private ScalarTransactionManager transactionManager;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void startWith_NullContractExecutionRequestGiven_ShouldStartWithNothing()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);

    // Act
    transactionManager.startWith(null);

    // Assert
    verify(manager).start();
  }

  @Test
  public void startWith_ContractExecutionRequestGiven_ShouldStartWithGivenTxid()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(request.getNonce()).thenReturn(NONCE);

    // Act
    transactionManager.startWith(request);

    // Assert
    verify(manager).start(NONCE);
  }

  @Test
  public void getState_CommittedGivenFromDatabaseUnderneath_ShouldReturnCommitted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    com.scalar.db.api.TransactionState dbState = com.scalar.db.api.TransactionState.COMMITTED;
    when(manager.getState(NONCE)).thenReturn(dbState);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.COMMITTED);
  }

  @Test
  public void getState_AbortedGivenFromDatabaseUnderneath_ShouldReturnAborted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    com.scalar.db.api.TransactionState dbState = com.scalar.db.api.TransactionState.ABORTED;
    when(manager.getState(NONCE)).thenReturn(dbState);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.ABORTED);
  }

  @Test
  public void getState_NeitherCommittedNorAbortedGivenFromDatabaseUnderneath_ShouldReturnUnknown()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    com.scalar.db.api.TransactionState dbState = com.scalar.db.api.TransactionState.UNKNOWN;
    when(manager.getState(NONCE)).thenReturn(dbState);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
  }

  @Test
  public void getState_StateManagementEnabledAndCommittedGivenFromState_ShouldReturnCommitted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.getState(NONCE)).thenReturn(TransactionState.COMMITTED);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.COMMITTED);
    verify(manager, never()).getState(NONCE);
  }

  @Test
  public void getState_StateManagementEnabledAndAbortedGivenFromState_ShouldReturnAborted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.getState(NONCE)).thenReturn(TransactionState.ABORTED);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.ABORTED);
    verify(manager, never()).getState(NONCE);
  }

  @Test
  public void getState_StateManagementEnabledAndUnknownGivenFromState_ShouldReturnUnknown()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.getState(NONCE)).thenReturn(TransactionState.UNKNOWN);

    // Act
    TransactionState state = transactionManager.getState(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
    verify(manager, never()).getState(NONCE);
  }

  @Test
  public void abort_StateManagementEnabledAndCommittedGivenFromState_ShouldReturnCommitted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.putAbort(NONCE)).thenReturn(TransactionState.COMMITTED);

    // Act
    TransactionState state = transactionManager.abort(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.COMMITTED);
    verify(manager, never()).abort(NONCE);
  }

  @Test
  public void abort_StateManagementEnabledAndAbortedGivenFromState_ShouldReturnAborted()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.putAbort(NONCE)).thenReturn(TransactionState.ABORTED);

    // Act
    TransactionState state = transactionManager.abort(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.ABORTED);
    verify(manager, never()).abort(NONCE);
  }

  @Test
  public void abort_StateManagementEnabledAndUnknownGivenFromState_ShouldReturnUnknown()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(manager, assetComposer, proofComposer, stateManager, config);
    when(stateManager.putAbort(NONCE)).thenReturn(TransactionState.UNKNOWN);

    // Act
    TransactionState state = transactionManager.abort(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
    verify(manager, never()).abort(NONCE);
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void recover_AssetIdsGivenAndConsensusCommitManagerUsed_ShouldRecoverAssetIds() {
    // Arrange
    ConsensusCommitManager manager = mock(ConsensusCommitManager.class);
    transactionManager =
        spy(
            new ScalarTransactionManager(
                manager, assetComposer, proofComposer, stateManager, config));
    Transaction transaction = mock(Transaction.class);
    TamperEvidentAssetLedger ledger = mock(TamperEvidentAssetLedger.class);
    doReturn(transaction).when(transactionManager).startWith();
    doReturn(ledger).when(transaction).getLedger();
    Map<String, Integer> ids = ImmutableMap.of(SOME_ASSET_ID, SOME_ASSET_AGE);

    // Act
    transactionManager.recover(ids);

    // Assert
    AssetFilter filter =
        new AssetFilter(SOME_ASSET_ID)
            .withStartAge(SOME_ASSET_AGE, true)
            .withEndAge(SOME_ASSET_AGE + 1, false);
    verify(ledger).scan(filter);
    verify(ledger).get(SOME_ASSET_ID);
    verify(transaction).commit();
    verify(transaction, never()).abort();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void
      recover_AssetIdsGivenAndConsensusCommitManagerUsedButDatabaseExceptionThrownInRecovery_ShouldAbort() {
    // Arrange
    ConsensusCommitManager manager = mock(ConsensusCommitManager.class);
    transactionManager =
        spy(
            new ScalarTransactionManager(
                manager, assetComposer, proofComposer, stateManager, config));
    Transaction transaction = mock(Transaction.class);
    TamperEvidentAssetLedger ledger = mock(TamperEvidentAssetLedger.class);
    doReturn(transaction).when(transactionManager).startWith();
    doReturn(ledger).when(transaction).getLedger();
    DatabaseException toThrow = mock(DatabaseException.class);
    doThrow(toThrow).when(ledger).scan(any());
    Map<String, Integer> ids = ImmutableMap.of(SOME_ASSET_ID, SOME_ASSET_AGE);

    // Act
    Throwable thrown = catchThrowable(() -> transactionManager.recover(ids));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
    verify(ledger).scan(any());
    verify(ledger, never()).get(anyString());
    verify(transaction, never()).commit();
    verify(transaction).abort();
  }

  @Test
  public void recover_AssetIdsGivenAndConsensusCommitManagerNotUsed_ShouldDoNothing() {
    // Arrange
    JdbcTransactionManager manager = mock(JdbcTransactionManager.class);
    transactionManager =
        spy(
            new ScalarTransactionManager(
                manager, assetComposer, proofComposer, stateManager, config));
    Map<String, Integer> ids = ImmutableMap.of(SOME_ASSET_ID, SOME_ASSET_AGE);

    // Act
    transactionManager.recover(ids);

    // Assert
    verify(transactionManager, never()).startWith();
  }
}
