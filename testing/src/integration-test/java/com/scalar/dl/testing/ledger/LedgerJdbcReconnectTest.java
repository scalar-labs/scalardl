package com.scalar.dl.testing.ledger;

import com.scalar.dl.testing.testcase.LedgerJdbcReconnectIntegrationTestBase;

/**
 * Integration test that verifies Ledger re-establishes JDBC connections during contract execution
 * under the SecurityManager. Storage type is determined from system properties (default: jdbc with
 * MySQL container); the test is skipped for non-JDBC storages.
 */
class LedgerJdbcReconnectTest extends LedgerJdbcReconnectIntegrationTestBase {}
