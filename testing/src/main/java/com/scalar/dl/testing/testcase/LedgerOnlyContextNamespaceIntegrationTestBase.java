package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.CREATE_FUNCTION_ID1;
import static com.scalar.dl.testing.contract.Constants.ID_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TABLE_METADATA;
import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.resolveNamespace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.api.Get;
import com.scalar.db.api.Result;
import com.scalar.db.io.Key;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.util.TestCertificates;
import java.io.IOException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
  // A namespace prefixed by the context namespace (with the separator). Functions in the context
  // namespace are allowed to access it under prefix-based access control.
  protected static final String PREFIXED_FUNCTION_NAMESPACE = TEST_NAMESPACE + "_sub";

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

  /**
   * In a non-default context, functions may only access the ScalarDB namespace with the same name
   * as the context namespace. The inherited function tests therefore operate on the context
   * namespace instead of the default {@code FUNCTION_NAMESPACE}.
   */
  @Override
  protected String getFunctionNamespace() {
    return TEST_NAMESPACE;
  }

  @Override
  protected void createFunctionTableSchema() throws Exception {
    super.createFunctionTableSchema();
    // Also create a namespace prefixed by the context namespace to verify prefix-based access.
    transactionAdmin.createNamespace(PREFIXED_FUNCTION_NAMESPACE, true);
    transactionAdmin.createTable(
        PREFIXED_FUNCTION_NAMESPACE, FUNCTION_TABLE, FUNCTION_TABLE_METADATA, true);
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

  @Test
  void executeContract_FunctionAccessingNamespacePrefixedByContextNamespace_ShouldSucceed()
      throws Exception {
    // Arrange: the function writes to a namespace whose name is prefixed by the context namespace
    // (context_test -> context_test_sub), which is allowed under prefix-based access control.
    String id = "prefixed_id";
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, id)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, PREFIXED_FUNCTION_NAMESPACE)
            .build();

    // Act
    clientServiceA.executeContract(
        CREATE_CONTRACT_ID1, contractArgument, CREATE_FUNCTION_ID1, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(PREFIXED_FUNCTION_NAMESPACE)
            .table(FUNCTION_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, id))
            .build();
    Optional<Result> result = storage.get(get);
    assertThat(result).isPresent();
    assertThat(result.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT_1);
  }

  @Test
  void executeContract_FunctionAccessingNamespaceOtherThanContextNamespace_ShouldThrowException() {
    // Arrange: the function tries to access FUNCTION_NAMESPACE, which differs from the context
    // namespace. A function registered in a non-default namespace may only access the ScalarDB
    // namespace with the same name as its context namespace, so this must be rejected.
    JsonObject contractArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, "id1")
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .add(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE)
            .build();

    // Act & Assert
    assertThatThrownBy(
            () ->
                clientServiceA.executeContract(
                    CREATE_CONTRACT_ID1, contractArgument, CREATE_FUNCTION_ID1, functionArgument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_FUNCTION));
  }

  @AfterAll
  @Override
  void tearDownCluster() {
    logger.info("Tearing down context namespace cluster");

    // Drop the prefixed function namespace created for prefix-access tests. This must run before
    // super.tearDownCluster() closes transactionAdmin.
    if (transactionAdmin != null) {
      try {
        transactionAdmin.dropTable(PREFIXED_FUNCTION_NAMESPACE, FUNCTION_TABLE);
        transactionAdmin.dropNamespace(PREFIXED_FUNCTION_NAMESPACE);
      } catch (Exception e) {
        logger.warn("Failed to drop prefixed function namespace", e);
      }
    }

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
