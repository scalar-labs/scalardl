package com.scalar.dl.ledger.lease;

import com.scalar.dl.ledger.error.ScalarDlError;

/**
 * Thrown by {@link Lease#get} when the backing lease table does not exist. Callers may react by
 * calling {@link Lease#createTable} once and retrying (reactive lazy provisioning), so that an
 * upgraded deployment does not have to re-run the schema loader before the lease can be used.
 */
public class LeaseTableNotFoundException extends LeaseException {

  public LeaseTableNotFoundException(ScalarDlError error, Throwable cause, Object... args) {
    super(error, cause, args);
  }
}
