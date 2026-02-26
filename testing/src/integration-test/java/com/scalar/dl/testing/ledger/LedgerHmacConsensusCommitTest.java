package com.scalar.dl.testing.ledger;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.testcase.LedgerConsensusCommitTestBase;

/**
 * Integration tests for Ledger with HMAC authentication and Consensus Commit transaction mode.
 * Storage type is determined from system properties (default: jdbc with MySQL container).
 */
class LedgerHmacConsensusCommitTest extends LedgerConsensusCommitTestBase {

  @Override
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.HMAC;
  }
}
