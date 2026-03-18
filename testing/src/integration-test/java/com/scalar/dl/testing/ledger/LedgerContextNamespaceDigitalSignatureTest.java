package com.scalar.dl.testing.ledger;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.testcase.LedgerOnlyContextNamespaceIntegrationTestBase;

/**
 * Context namespace integration tests for Ledger with Digital Signature authentication.
 *
 * <p>This test class runs all {@link LedgerOnlyContextNamespaceIntegrationTestBase} tests (which
 * inherit from {@code LedgerOnlyIntegrationTestBase}) with context namespace configuration. This
 * verifies that contract execution, function execution, and tamper detection work correctly when
 * using a non-default context namespace.
 *
 * <p>Key aspects tested:
 *
 * <ul>
 *   <li>Contract registration and execution with context namespace
 *   <li>Function registration via non-privileged port (SignedFunctionRegistrationRequest)
 *   <li>Function execution with context namespace
 *   <li>Ledger validation (validateLedger) with context namespace
 *   <li>All tamper detection tests with context namespace
 * </ul>
 */
class LedgerContextNamespaceDigitalSignatureTest
    extends LedgerOnlyContextNamespaceIntegrationTestBase {

  @Override
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.DIGITAL_SIGNATURE;
  }
}
