package com.scalar.dl.ledger.database.scalardb;

import com.google.inject.Inject;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitManager;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.database.NamespaceRestrictedAssetLedger;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.statemachine.AssetKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class ScalarTransactionManager implements TransactionManager, TableMetadataProvider {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScalarTransactionManager.class.getName());
  private final DistributedTransactionManager manager;
  private final TamperEvidentAssetComposer assetComposer;
  private final AssetProofComposer proofComposer;
  private final TransactionStateManager stateManager;
  private final ScalarNamespaceResolver namespaceResolver;
  private final LedgerConfig config;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarTransactionManager(
      DistributedTransactionManager manager,
      TamperEvidentAssetComposer assetComposer,
      AssetProofComposer proofComposer,
      TransactionStateManager stateManager,
      ScalarNamespaceResolver namespaceResolver,
      LedgerConfig config) {
    this.manager = manager;
    this.assetComposer = assetComposer;
    this.proofComposer = proofComposer;
    this.stateManager = stateManager;
    this.namespaceResolver = namespaceResolver;
    this.config = config;
  }

  @Override
  public Map<String, TableMetadata> getTransactionTables() {
    return ScalarTamperEvidentAssetLedger.getTransactionTables();
  }

  @Override
  public Transaction startWith(@Nullable ContractExecutionRequest request) {
    DistributedTransaction transaction;
    try {
      if (request == null) {
        transaction = manager.start();
      } else {
        transaction = manager.start(request.getNonce());
      }
    } catch (TransactionException e) {
      throw new DatabaseException(LedgerError.STARTING_TRANSACTION_FAILED, e, e.getMessage());
    }

    MutableDatabase<Get, Scan, Put, Delete, Result> database = null;
    if (config.isFunctionEnabled()) {
      database = new ScalarMutableDatabase(transaction);
    }

    TamperEvidentAssetLedger ledger = createTamperEvidentAssetLedger(request, transaction);
    return new Transaction(ledger, database);
  }

  private TamperEvidentAssetLedger createTamperEvidentAssetLedger(
      ContractExecutionRequest request, DistributedTransaction transaction) {
    TamperEvidentAssetLedger ledger =
        new ScalarTamperEvidentAssetLedger(
            transaction,
            new ScalarTamperEvidentAssetLedger.Metadata(transaction, namespaceResolver),
            new Snapshot(),
            request,
            assetComposer,
            proofComposer,
            stateManager,
            namespaceResolver,
            config);
    String contextNamespace =
        request == null ? Namespaces.DEFAULT : request.getContextNamespaceOrDefault();
    if (!contextNamespace.equals(Namespaces.DEFAULT)) {
      ledger = new NamespaceRestrictedAssetLedger(ledger, contextNamespace);
    }
    return ledger;
  }

  @Override
  public Transaction startWith() {
    return startWith(null);
  }

  @Override
  public TransactionState getState(String transactionId) {
    try {
      if (config.isTxStateManagementEnabled()) {
        return stateManager.getState(transactionId);
      } else {
        return convert(manager.getState(transactionId));
      }
    } catch (TransactionException e) {
      throw new DatabaseException(LedgerError.GETTING_TRANSACTION_STATE_FAILED, e, e.getMessage());
    }
  }

  @Override
  public TransactionState abort(String transactionId) {
    try {
      if (config.isTxStateManagementEnabled()) {
        return stateManager.putAbort(transactionId);
      } else {
        return convert(manager.abort(transactionId));
      }
    } catch (TransactionException e) {
      LOGGER.warn("can't abort the transaction: " + transactionId, e);
      return TransactionState.UNKNOWN;
    }
  }

  @Override
  public void finish(String transactionId) {
    // Reject finish (purge) requests while purge is disabled so that transaction states are not
    // deleted unintentionally. finish is only ever called as part of transaction state purge, so
    // this does not affect the normal contract execution flow.
    if (!config.isTransactionStatePurgeEnabled()) {
      throw new LedgerException(LedgerError.TRANSACTION_STATE_PURGE_DISABLED);
    }

    if (config.isTxStateManagementEnabled()) {
      // Currently unreachable: transaction state purge is rejected for the JDBC transaction
      // manager at startup (see LedgerConfig), and tx state management is only used with JDBC,
      // so finish is never called here while purge is enabled. This path is kept for future
      // JDBC-mode purge support, but it must NOT simply delete the state: a force-aborted state
      // of a long-running transaction past the grace period must be retained (deleting it could
      // let that transaction commit afterward and cause an anomaly). That needs a separate
      // reclamation mechanism on a different time axis from the Auditor's periodic purge.
      stateManager.deleteState(transactionId);
    } else {
      // Consensus Commit manages the Coordinator states, so delegate to ScalarDB to finish the
      // transaction (run recovery if needed and delete the Coordinator state). A failure surfaces
      // to the Auditor-side purge resilience, which retries on a later scan (finishTransaction is
      // idempotent, so retrying with the same transaction id is safe).
      //
      // finishTransaction only deletes a Coordinator state that has a write set (a normally
      // committed or aborted transaction). A state written by a force-abort of a transaction that
      // exceeded the grace period (abort-by-id carries no write set) is intentionally NOT deleted.
      // Exceeding the grace period does not guarantee the transaction is dead; it may still be
      // active, and deleting its ABORTED Coordinator state would let it commit afterward (its
      // Coordinator state would be absent again), causing an anomaly. Retaining the ABORTED state
      // blocks that commit.
      try {
        manager.finishTransaction(transactionId);
      } catch (TransactionException e) {
        throw new DatabaseException(LedgerError.FINISHING_TRANSACTION_FAILED, e, e.getMessage());
      }
    }
  }

  @Override
  public void recover(Map<AssetKey, Integer> assetKeys) {
    if (manager instanceof ConsensusCommitManager) {
      /*
       * This rolls back asset records which might be left PREPARED due to some failure
       * at the time of recovery, and tries to keep asset records and asset metadata consistent.
       */
      Transaction transaction = startWith();

      try {
        assetKeys.forEach(
            (key, age) -> {
              AssetFilter filter =
                  new AssetFilter(key.namespace(), key.assetId())
                      .withStartAge(age, true)
                      .withEndAge(age + 1, false);
              transaction.getLedger().scan(filter);
              transaction
                  .getLedger()
                  .get( // for asset_metadata when it is enabled
                      key.namespace(), key.assetId());
            });
        transaction.commit();
      } catch (Exception e) {
        // Roll back might have been succeeded and might have been failed.
        // Even if it was failed, it will be recovered by this method eventually
        transaction.abort();
      }
    }
    // do nothing for the other DistributedTransactionManager for now
  }

  private TransactionState convert(com.scalar.db.api.TransactionState state) {
    switch (state) {
      case COMMITTED:
        return TransactionState.COMMITTED;
      case ABORTED:
        return TransactionState.ABORTED;
      default:
        return TransactionState.UNKNOWN;
    }
  }
}
