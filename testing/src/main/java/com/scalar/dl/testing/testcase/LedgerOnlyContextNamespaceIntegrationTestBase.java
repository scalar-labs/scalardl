package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.resolveNamespace;

import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.util.TestCertificates;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for running all {@link LedgerOnlyIntegrationTestBase} tests (including Function tests)
 * within a non-default context namespace.
 *
 * <p>While the parent class operates in the default context, this class sets a specific context
 * namespace on ClientService instances so that every inherited test transparently runs against that
 * namespace. This verifies that existing Ledger-only functionality works correctly regardless of
 * the context namespace. The key differences from the parent class are:
 *
 * <ul>
 *   <li>ClientService instances are configured with a non-default context namespace
 *   <li>The Ledger server is configured to allow function registration via the non-privileged port
 *   <li>An admin ClientService (default namespace) is used for namespace creation and credential
 *       registration
 *   <li>Contract and function registration is performed via the non-privileged port using signed
 *       requests
 * </ul>
 *
 * <p>The test workflow is:
 *
 * <ol>
 *   <li>Start cluster with non-privileged port function registration enabled
 *   <li>Create admin ClientService (default namespace) for privileged operations
 *   <li>Create the test namespace and register credentials using admin ClientService
 *   <li>Create test ClientService instances with the context namespace
 *   <li>Register contracts and functions (automatically uses non-privileged port due to context
 *       namespace)
 *   <li>Run all inherited tests with the context namespace configuration
 * </ol>
 */
public abstract class LedgerOnlyContextNamespaceIntegrationTestBase
    extends LedgerOnlyIntegrationTestBase {

  protected static final String TEST_NAMESPACE = "context_test";

  // Admin ClientService (default namespace, for privileged operations)
  protected ClientService adminClientService;

  @Override
  protected String getPhysicalNamespace() {
    return resolveNamespace(SCALAR_NAMESPACE, TEST_NAMESPACE);
  }

  @Override
  protected String getContextNamespace() {
    return TEST_NAMESPACE;
  }

  @BeforeAll
  @Override
  void setUpCluster() throws Exception {
    logger.info(
        "Setting up context namespace cluster: authenticationMethod={}, transactionMode={}",
        getAuthenticationMethod(),
        getTransactionMode());

    cluster =
        new LedgerTestCluster(getAuthenticationMethod(), getTransactionMode(), getLedgerImage())
            .withNonPrivilegedPortFunctionRegistrationEnabled();
    cluster.start();

    setUpAfterClusterStart();
  }

  @Override
  protected void setUpAfterClusterStart() throws Exception {
    createStorage();
    createSchema();
    createFunctionTableSchema();

    // Phase 1: Create admin ClientService (default namespace)
    createAdminClientService();

    // Phase 2: Create namespace and register credentials using admin ClientService
    // Note: createNamespace() automatically creates the necessary asset tables
    adminClientService.createNamespace(TEST_NAMESPACE);
    registerCredentialsToNamespace();

    // Phase 3: Create test ClientService with context namespace
    createClientServices();

    // Phase 4: Register contracts and functions (via non-privileged port)
    registerContracts();
    registerFunctions();
  }

  private void createAdminClientService() throws IOException {
    // Admin client uses default namespace for privileged operations
    if (getAuthenticationMethod() == AuthenticationMethod.DIGITAL_SIGNATURE) {
      adminClientService =
          clientServiceFactory.create(
              getDigitalSignatureClientConfig(
                  TestCertificates.ENTITY_ID_ADMIN,
                  TestCertificates.PRIVATE_KEY_ADMIN,
                  TestCertificates.CERTIFICATE_ADMIN,
                  getAuthenticationMethod(),
                  Namespaces.DEFAULT));
    } else {
      adminClientService =
          clientServiceFactory.create(
              getHmacClientConfig(
                  TestCertificates.ENTITY_ID_ADMIN,
                  TestCertificates.SECRET_KEY_ADMIN,
                  getAuthenticationMethod(),
                  Namespaces.DEFAULT));
    }
  }

  protected void registerCredentialsToNamespace() {
    if (getAuthenticationMethod() == AuthenticationMethod.DIGITAL_SIGNATURE) {
      adminClientService.registerCertificate(
          TEST_NAMESPACE,
          TestCertificates.ENTITY_ID_A,
          TestCertificates.KEY_VERSION,
          TestCertificates.CERTIFICATE_A);
      adminClientService.registerCertificate(
          TEST_NAMESPACE,
          TestCertificates.ENTITY_ID_B,
          TestCertificates.KEY_VERSION,
          TestCertificates.CERTIFICATE_B);
    } else {
      adminClientService.registerSecret(
          TEST_NAMESPACE,
          TestCertificates.ENTITY_ID_A,
          TestCertificates.KEY_VERSION,
          TestCertificates.SECRET_KEY_A);
      adminClientService.registerSecret(
          TEST_NAMESPACE,
          TestCertificates.ENTITY_ID_B,
          TestCertificates.KEY_VERSION,
          TestCertificates.SECRET_KEY_B);
    }
  }

  @AfterAll
  @Override
  void tearDownCluster() {
    logger.info("Tearing down context namespace cluster");

    // Drop the test namespace (this also removes the namespace's asset tables)
    if (adminClientService != null) {
      try {
        adminClientService.dropNamespace(TEST_NAMESPACE);
      } catch (Exception e) {
        logger.warn("Failed to drop test namespace", e);
      }
    }

    // Call parent tearDown for default namespace cleanup
    super.tearDownCluster();
  }
}
