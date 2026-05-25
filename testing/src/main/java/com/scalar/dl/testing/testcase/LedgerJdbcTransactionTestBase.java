package com.scalar.dl.testing.testcase;

import com.scalar.dl.testing.config.TransactionMode;

/**
 * Base class for Ledger integration tests with JDBC transaction mode (tx_state_management=true).
 *
 * <p>Subclasses should override {@link #getAuthenticationMethod()}.
 */
public abstract class LedgerJdbcTransactionTestBase extends LedgerOnlyIntegrationTestBase {

  @Override
  protected TransactionMode getTransactionMode() {
    return TransactionMode.JDBC;
  }
}
