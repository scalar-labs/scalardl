package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.error.CommonError;

public enum TransactionState {
  COMMITTED(1),
  ABORTED(2),
  UNKNOWN(3);

  private final int id;

  TransactionState(final int id) {
    this.id = id;
  }

  public int get() {
    return id;
  }

  public static TransactionState getInstance(int id) {
    for (TransactionState state : TransactionState.values()) {
      if (state.get() == id) {
        return state;
      }
    }
    throw new IllegalArgumentException(
        CommonError.INVALID_TRANSACTION_STATE_SPECIFIED.buildMessage());
  }
}
