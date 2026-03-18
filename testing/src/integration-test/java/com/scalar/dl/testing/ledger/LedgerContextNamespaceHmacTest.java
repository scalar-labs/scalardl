package com.scalar.dl.testing.ledger;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.testcase.LedgerOnlyContextNamespaceIntegrationTestBase;

/**
 * Context namespace integration tests for Ledger with HMAC authentication.
 *
 * <p>This test class runs all {@link LedgerOnlyContextNamespaceIntegrationTestBase} tests (which
 * inherit from {@code LedgerOnlyIntegrationTestBase}) with context namespace configuration and HMAC
 * authentication. This verifies that contract execution, function execution, and tamper detection
 * work correctly when using a non-default context namespace with HMAC authentication.
 *
 * <p>Key aspects tested:
 *
 * <ul>
 *   <li>Contract registration and execution with context namespace (HMAC)
 *   <li>Function registration via non-privileged port (SignedFunctionRegistrationRequest with HMAC)
 *   <li>Function execution with context namespace (HMAC)
 *   <li>Ledger validation (validateLedger) with context namespace (HMAC)
 *   <li>All tamper detection tests with context namespace (HMAC)
 * </ul>
 */
class LedgerContextNamespaceHmacTest extends LedgerOnlyContextNamespaceIntegrationTestBase {

  @Override
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.HMAC;
  }
}
