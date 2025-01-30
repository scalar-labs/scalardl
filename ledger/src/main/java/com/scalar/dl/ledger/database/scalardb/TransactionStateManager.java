package com.scalar.dl.ledger.database.scalardb;

import static com.scalar.dl.ledger.database.TransactionState.ABORTED;
import static com.scalar.dl.ledger.database.TransactionState.COMMITTED;
import static com.scalar.dl.ledger.database.TransactionState.UNKNOWN;

import com.google.inject.Inject;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionStateManager {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TransactionStateManager.class.getName());
  static final String TABLE = "tx_state";
  static final String ID = "id";
  static final String STATE = "state";
  static final String CREATED_AT = "created_at";
  private final DistributedTransactionManager manager;

  @Inject
  public TransactionStateManager(DistributedTransactionManager manager) {
    this.manager = manager;
  }

  public void putCommit(DistributedTransaction transaction, String transactionId)
      throws CrudException {
    TransactionState state = get(transaction, transactionId);

    if (state == COMMITTED || state == ABORTED) {
      throw new ConflictException(
          "the transaction state has already been " + state.toString().toLowerCase());
    }

    put(transaction, transactionId, COMMITTED);
  }

  public TransactionState putAbort(String transactionId) {
    DistributedTransaction transaction = null;
    try {
      transaction = manager.start();
      TransactionState state = get(transaction, transactionId);
      if (state != COMMITTED && state != ABORTED) {
        put(transaction, transactionId, ABORTED);
        state = ABORTED;
      }
      transaction.commit();
      return state;
    } catch (TransactionException e) {
      if (transaction != null) {
        try {
          transaction.abort();
        } catch (AbortException ignored) {
          // ignore it since the transaction state is eventually settled
        }
      }
      LOGGER.warn("could not abort the transaction " + transactionId + " for some reason.", e);
      return UNKNOWN;
    }
  }

  public TransactionState getState(String transactionId) {
    DistributedTransaction transaction = null;
    try {
      transaction = manager.start();
      TransactionState state = get(transaction, transactionId);
      transaction.commit();
      return state;
    } catch (TransactionException e) {
      if (transaction != null) {
        try {
          transaction.abort();
        } catch (AbortException ignored) {
          // ignore it since the transaction state is eventually settled
        }
      }
      LOGGER.warn(
          "could not get the state of transaction " + transactionId + " for some reason.", e);
      return UNKNOWN;
    }
  }

  private void put(DistributedTransaction transaction, String transactionId, TransactionState state)
      throws CrudException {
    Put put =
        new Put(new Key(new TextValue(ID, transactionId)))
            .withValue(new IntValue(STATE, state.get()))
            .withValue(new BigIntValue(CREATED_AT, System.currentTimeMillis()))
            .forTable(TABLE);

    transaction.put(put);
  }

  private TransactionState get(DistributedTransaction transaction, String transactionId)
      throws CrudException {
    Get get = new Get(new Key(ID, transactionId)).forTable(TABLE);

    return transaction
        .get(get)
        .map(r -> TransactionState.getInstance(r.getValue(STATE).get().getAsInt()))
        .orElse(TransactionState.UNKNOWN);
  }
}
