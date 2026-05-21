package com.scalar.dl.auditor.ordering;

import com.scalar.dl.ledger.error.CommonError;

public enum LockRecoveryResult {
  SUCCEEDED(1),
  FAILED(2),
  NOT_RECOVERABLE(3),
  ALREADY_RECOVERED(4);

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
