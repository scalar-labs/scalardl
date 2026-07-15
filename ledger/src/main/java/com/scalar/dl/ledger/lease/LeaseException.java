package com.scalar.dl.ledger.lease;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.exception.LedgerException;

/**
 * Thrown when a lease operation fails due to an infrastructure error (e.g. the underlying storage
 * is unreachable). Callers that use a lease for best-effort coordination typically log this and
 * skip the current round rather than failing hard. This is distinct from a lost compare-and-swap,
 * which {@link Lease#tryAcquireOrRenew} reports by returning {@code false} rather than throwing.
 *
 * <p>It extends {@link LedgerException} so that, like the other ScalarDL exceptions, it carries a
 * {@link com.scalar.dl.ledger.service.StatusCode} that identifies the failure in logs.
 */
public class LeaseException extends LedgerException {

  public LeaseException(ScalarDlError error, Object... args) {
    super(error, args);
  }

  public LeaseException(ScalarDlError error, Throwable cause, Object... args) {
    super(error, cause, args);
  }
}
