package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.AssetKey;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarTransactionManagerTest {
  private static final String NONCE = "nonce";
  private static final String BASE_NAMESPACE = "scalar";
  private static final String DEFAULT_NAMESPACE = "default";
  private static final String SOME_NAMESPACE = "namespace";
  private static final String SOME_ASSET_ID = "asset_id";
  private static final AssetKey SOME_ASSET_KEY = AssetKey.of(SOME_NAMESPACE, SOME_ASSET_ID);
  private static final int SOME_ASSET_AGE = 1;
  @Mock private DistributedTransactionManager manager;
  @Mock private TamperEvidentAssetComposer assetComposer;
  @Mock private AssetProofComposer proofComposer;
  @Mock private TransactionStateManager stateManager;
  @Mock private ScalarNamespaceResolver namespaceResolver;
  @Mock private ContractExecutionRequest request;
  @Mock private LedgerConfig config;
  private ScalarTransactionManager transactionManager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(namespaceResolver.resolve(DEFAULT_NAMESPACE)).thenReturn(BASE_NAMESPACE);
    when(request.getContextNamespaceOrDefault()).thenReturn(DEFAULT_NAMESPACE);
  }

  @Test
  public void startWith_NullContractExecutionRequestGiven_ShouldStartWithNothing()
      throws TransactionException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);

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
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
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
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
    when(stateManager.putAbort(NONCE)).thenReturn(TransactionState.UNKNOWN);

    // Act
    TransactionState state = transactionManager.abort(NONCE);

    // Assert
    assertThat(state).isEqualTo(TransactionState.UNKNOWN);
    verify(manager, never()).abort(NONCE);
  }

  @Test
  public void
      finish_PurgeEnabledAndStateManagementEnabled_ShouldThrowUnsupportedOperationException()
          throws TransactionException {
    // Arrange
    when(config.isTransactionStatePurgeEnabled()).thenReturn(true);
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);

    // Act
    Throwable thrown = catchThrowable(() -> transactionManager.finish(NONCE));

    // Assert
    assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    verify(stateManager, never()).deleteState(NONCE);
    verify(manager, never()).finishTransaction(NONCE);
  }

  @Test
  public void finish_PurgeEnabledAndStateManagementDisabled_ShouldCallFinishTransaction()
      throws TransactionException {
    // Arrange
    when(config.isTransactionStatePurgeEnabled()).thenReturn(true);
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);

    // Act
    transactionManager.finish(NONCE);

    // Assert
    verify(manager).finishTransaction(NONCE);
    verify(stateManager, never()).deleteState(NONCE);
  }

  @Test
  public void finish_PurgeDisabled_ShouldThrowLedgerExceptionAndNotTouchState()
      throws TransactionException {
    // Arrange
    when(config.isTransactionStatePurgeEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);

    // Act
    Throwable thrown = catchThrowable(() -> transactionManager.finish(NONCE));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.INVALID_REQUEST);
    verify(stateManager, never()).deleteState(NONCE);
    verify(manager, never()).finishTransaction(NONCE);
  }

  @Test
  public void finish_FinishTransactionThrowsTransactionException_ShouldThrowDatabaseException()
      throws TransactionException {
    // Arrange
    when(config.isTransactionStatePurgeEnabled()).thenReturn(true);
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    TransactionException cause = mock(TransactionException.class);
    doThrow(cause).when(manager).finishTransaction(NONCE);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);

    // Act
    Throwable thrown = catchThrowable(() -> transactionManager.finish(NONCE));

    // Assert
    assertThat(thrown).isInstanceOf(DatabaseException.class);
    assertThat(thrown.getCause()).isEqualTo(cause);
  }

  @Test
  public void recover_AssetKeysGivenAndConsensusCommitUsed_ShouldRecoverAssetAndMetadataRecords()
      throws TransactionException {
    // Arrange
    when(config.isConsensusCommitEnabled()).thenReturn(true);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);
    when(namespaceResolver.resolve(SOME_NAMESPACE)).thenReturn(BASE_NAMESPACE);
    when(manager.recoverRecord(anyString(), anyString(), any(), any())).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
    Map<AssetKey, Integer> keys = ImmutableMap.of(SOME_ASSET_KEY, SOME_ASSET_AGE);

    // Act
    transactionManager.recover(keys);

    // Assert
    verify(manager)
        .recoverRecord(
            BASE_NAMESPACE,
            ScalarTamperEvidentAssetLedger.TABLE,
            Key.ofText(AssetRecord.ID, SOME_ASSET_ID),
            Key.ofInt(AssetRecord.AGE, SOME_ASSET_AGE));
    verify(manager)
        .recoverRecord(
            BASE_NAMESPACE,
            ScalarTamperEvidentAssetLedger.Metadata.TABLE,
            Key.ofText(ScalarTamperEvidentAssetLedger.AssetMetadata.ID, SOME_ASSET_ID),
            null);
  }

  @Test
  public void recover_AssetKeysGivenAndDirectAssetAccessEnabled_ShouldRecoverOnlyAssetRecord()
      throws TransactionException {
    // Arrange
    when(config.isConsensusCommitEnabled()).thenReturn(true);
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);
    when(namespaceResolver.resolve(SOME_NAMESPACE)).thenReturn(BASE_NAMESPACE);
    when(manager.recoverRecord(anyString(), anyString(), any(), any())).thenReturn(true);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
    Map<AssetKey, Integer> keys = ImmutableMap.of(SOME_ASSET_KEY, SOME_ASSET_AGE);

    // Act
    transactionManager.recover(keys);

    // Assert
    verify(manager)
        .recoverRecord(
            BASE_NAMESPACE,
            ScalarTamperEvidentAssetLedger.TABLE,
            Key.ofText(AssetRecord.ID, SOME_ASSET_ID),
            Key.ofInt(AssetRecord.AGE, SOME_ASSET_AGE));
    verify(manager, never())
        .recoverRecord(
            anyString(), eq(ScalarTamperEvidentAssetLedger.Metadata.TABLE), any(), any());
  }

  @Test
  public void recover_RecoveringAssetRecordFailed_ShouldNotThrowAnyExceptionAndRecoverMetadata()
      throws TransactionException {
    // Arrange
    when(config.isConsensusCommitEnabled()).thenReturn(true);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);
    when(namespaceResolver.resolve(SOME_NAMESPACE)).thenReturn(BASE_NAMESPACE);
    TransactionException toThrow = mock(TransactionException.class);
    doThrow(toThrow)
        .when(manager)
        .recoverRecord(
            BASE_NAMESPACE,
            ScalarTamperEvidentAssetLedger.TABLE,
            Key.ofText(AssetRecord.ID, SOME_ASSET_ID),
            Key.ofInt(AssetRecord.AGE, SOME_ASSET_AGE));
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
    Map<AssetKey, Integer> keys = ImmutableMap.of(SOME_ASSET_KEY, SOME_ASSET_AGE);

    // Act
    Throwable thrown = catchThrowable(() -> transactionManager.recover(keys));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
    verify(manager)
        .recoverRecord(
            BASE_NAMESPACE,
            ScalarTamperEvidentAssetLedger.Metadata.TABLE,
            Key.ofText(ScalarTamperEvidentAssetLedger.AssetMetadata.ID, SOME_ASSET_ID),
            null);
  }

  @Test
  public void recover_AssetKeysGivenAndConsensusCommitNotUsed_ShouldDoNothing()
      throws TransactionException {
    // Arrange
    when(config.isConsensusCommitEnabled()).thenReturn(false);
    transactionManager =
        new ScalarTransactionManager(
            manager, assetComposer, proofComposer, stateManager, namespaceResolver, config);
    Map<AssetKey, Integer> keys = ImmutableMap.of(SOME_ASSET_KEY, SOME_ASSET_AGE);

    // Act
    transactionManager.recover(keys);

    // Assert
    verify(manager, never()).recoverRecord(anyString(), anyString(), any(), any());
  }
}
