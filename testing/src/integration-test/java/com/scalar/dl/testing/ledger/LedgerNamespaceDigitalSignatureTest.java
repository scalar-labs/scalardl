package com.scalar.dl.testing.ledger;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.testcase.LedgerOnlyNamespaceIntegrationTestBase;

/**
 * Namespace integration tests for Ledger with Digital Signature authentication.
 *
 * <p>This test class runs all namespace-related tests including:
 *
 * <ul>
 *   <li>Namespace management (create, drop, list)
 *   <li>Contract execution with namespaces
 *   <li>Validation of namespace-aware assets
 *   <li>Tamper detection tests (record removal, hash tampering, output tampering, etc.)
 * </ul>
 *
 * <p>The tamper detection tests are NOT applicable to Auditor configurations because Auditor uses
 * Byzantine Fault Detection which has different validation semantics.
 */
class LedgerNamespaceDigitalSignatureTest extends LedgerOnlyNamespaceIntegrationTestBase {

  @Override
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.DIGITAL_SIGNATURE;
  }
}
