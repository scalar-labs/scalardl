package com.scalar.dl.ledger.database.scalardb;

import com.google.inject.Inject;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitManager;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class ScalarTransactionManager implements TransactionManager {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScalarTransactionManager.class.getName());
  private final DistributedTransactionManager manager;
  private final TamperEvidentAssetComposer assetComposer;
  private final AssetProofComposer proofComposer;
  private final TransactionStateManager stateManager;
  private final LedgerConfig config;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarTransactionManager(
      DistributedTransactionManager manager,
      TamperEvidentAssetComposer assetComposer,
      AssetProofComposer proofComposer,
      TransactionStateManager stateManager,
      LedgerConfig config) {
    this.manager = manager;
    this.assetComposer = assetComposer;
    this.proofComposer = proofComposer;
    this.stateManager = stateManager;
    this.config = config;
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

    TamperEvidentAssetLedger ledger =
        new ScalarTamperEvidentAssetLedger(
            transaction,
            new ScalarTamperEvidentAssetLedger.Metadata(transaction),
            new Snapshot(),
            request,
            assetComposer,
            proofComposer,
            stateManager,
            config);
    return new Transaction(ledger, database);
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
  public void recover(Map<String, Integer> assetIds) {
    if (manager instanceof ConsensusCommitManager) {
      /*
       * This rolls back asset records which might be left PREPARED due to some failure
       * at the time of recovery, and tries to keep asset records and asset metadata consistent.
       */
      Transaction transaction = startWith();

      try {
        assetIds.forEach(
            (id, age) -> {
              AssetFilter filter =
                  new AssetFilter(id).withStartAge(age, true).withEndAge(age + 1, false);
              transaction.getLedger().scan(filter);
              transaction.getLedger().get(id); // for asset_metadata when it is enabled
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
