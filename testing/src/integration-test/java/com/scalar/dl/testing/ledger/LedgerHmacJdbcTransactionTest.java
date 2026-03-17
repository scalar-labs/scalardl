package com.scalar.dl.testing.ledger;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.testcase.LedgerJdbcTransactionTestBase;

/**
 * Integration tests for Ledger with HMAC authentication and JDBC transaction mode. Storage type is
 * determined from system properties (default: jdbc with MySQL container).
 */
class LedgerHmacJdbcTransactionTest extends LedgerJdbcTransactionTestBase {

  @Override
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.HMAC;
  }
}
