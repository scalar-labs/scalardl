package com.scalar.dl.auditor.ordering;

import com.scalar.dl.ledger.error.CommonError;

public enum LockRecoveryResult {
  SUCCEEDED(0),
  FAILED(1),
  NOT_RECOVERABLE(2),
  ALREADY_RECOVERED(3);

  private final int id;

  LockRecoveryResult(final int id) {
    this.id = id;
  }

  public int get() {
    return id;
  }

  public static LockRecoveryResult getInstance(int id) {
    for (LockRecoveryResult result : LockRecoveryResult.values()) {
      if (result.get() == id) {
        return result;
      }
    }
    throw new IllegalArgumentException(
        CommonError.INVALID_LOCK_RECOVERY_RESULT_SPECIFIED.buildMessage());
  }
}
