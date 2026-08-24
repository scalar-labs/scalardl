package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CONTRACT_ID_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.GET_BALANCE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.ID_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.NAMESPACE_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.NESTED_INVOKER_CONTRACT_ID;
import static com.scalar.dl.testing.contract.Constants.NOOP_CONTRACT_ID;
import static com.scalar.dl.testing.contract.Constants.UPSERT_FUNCTION_ID;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TEST_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.FUNCTION_TEST_TABLE_METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.Result;
import com.scalar.db.io.Key;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.TransactionFactory;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.testing.config.TransactionMode;
import com.scalar.dl.testing.container.AbstractTestCluster;
import com.scalar.dl.testing.container.LedgerContainer;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.contract.Create;
import com.scalar.dl.testing.contract.GetBalance;
import com.scalar.dl.testing.contract.NestedInvoker;
import com.scalar.dl.testing.contract.Noop;
import com.scalar.dl.testing.function.UpsertFunction;
import com.scalar.dl.testing.schema.TestSchemas;
import com.scalar.dl.testing.util.ConnectionKiller;
import com.scalar.dl.testing.util.TestCertificates;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the JDBC reconnection regression test.
 *
 * <p>This test verifies that Ledger can re-establish database connections while executing a
 * contract. When all pooled connections are lost (e.g., due to a server-side idle timeout, a
 * database restart, or a network interruption), the next in-contract database access forces
 * HikariCP to open new physical connections from within the contract's restricted {@code
 * ProtectionDomain} (the connection-adding worker thread inherits the restricted {@code
 * AccessControlContext} of the thread that requests a connection). If the {@code ProtectionDomain}
 * lacks permissions that the JDBC driver needs during connection setup, the execution fails with an
 * {@code AccessControlException} under the SecurityManager.
 *
 * <p>The scenarios only apply to JDBC storages; the tests are skipped for other storages.
 *
 * <p>Only the first access that has to open a new physical connection after a kill exercises the
 * reconnection, so each scenario performs its own kill and arranges its target operation to be the
 * first database access of the execution (per connection pool; the registries and the transaction
 * manager use separate pools). Three scenarios pin the privileged wraps that can be
 * deterministically exercised: an in-contract asset read ({@code ScalarTamperEvidentAssetLedger}),
 * a nested contract invocation whose callee is looked up for the first time after the kill ({@code
 * ScalarContractRegistry}), and an in-function database read ({@code ScalarMutableDatabase}). The
 * remaining wraps cannot be pinned here: the function-registry read always fires first on the
 * framework stack (before the contract is invoked) and re-warms its pool, and the
 * certificate/secret registries are shielded by caches that request signature validation keeps warm
 * on every request.
 *
 * <p>Subclasses can override {@link #createCluster()} and {@link
 * #createServerConnectionProperties()} to run the same scenarios against a different cluster
 * configuration (e.g., with Auditor).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LedgerJdbcReconnectIntegrationTestBase {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String SOME_ASSET_ID = "reconnect-test-asset";
  private static final String NESTED_ASSET_ID = "reconnect-test-nested-asset";
  private static final String SOME_FUNCTION_ROW_ID = "reconnect-test-function-row";
  private static final int SOME_AMOUNT = 100;

  // A copy of GetBalance registered under a dedicated ID. It must not be invoked before the
  // nested-invocation test kills the connections, so that its registry lookup there is an actual
  // database read (contract registration does not populate the registry cache, but lookups do).
  private static final String NESTED_TARGET_CONTRACT_ID = "GetBalanceNestedTarget";

  // Time to let the connection pools in the server containers finish their initial fill, so that
  // no connection survives the kill by being created concurrently with it.
  private static final long POOL_SETTLE_MILLIS = 3000;
  // HikariCP's connection-adding worker threads die after being idle for 5 seconds. Waiting
  // longer than that ensures the next in-contract connection request spawns a new worker thread
  // from the contract's restricted context.
  private static final long WORKER_EXPIRY_MILLIS = 7000;

  private static final ClientServiceFactory clientServiceFactory = new ClientServiceFactory();

  protected AbstractTestCluster cluster;
  protected ClientService clientService;
  private DistributedStorage storage;
  private DistributedTransactionAdmin transactionAdmin;

  /**
   * Returns the authentication method for this test.
   *
   * @return The AuthenticationMethod to use
   */
  protected AuthenticationMethod getAuthenticationMethod() {
    return AuthenticationMethod.HMAC;
  }

  /**
   * Returns the transaction mode for this test.
   *
   * @return The TransactionMode to use
   */
  protected TransactionMode getTransactionMode() {
    return TransactionMode.CONSENSUS_COMMIT;
  }

  /**
   * Returns the Ledger Docker image to use. Override to use a custom image.
   *
   * @return The Docker image tag
   */
  protected String getLedgerImage() {
    return System.getProperty("scalardl.ledger.image", LedgerContainer.DEFAULT_IMAGE);
  }

  /**
   * Creates the test cluster. Override to use a different cluster configuration (e.g., with
   * Auditor).
   *
   * @return The test cluster to use
   */
  protected AbstractTestCluster createCluster() {
    return new LedgerTestCluster(getAuthenticationMethod(), getTransactionMode(), getLedgerImage());
  }

  @BeforeAll
  void setUpCluster() throws Exception {
    cluster = createCluster();
    cluster.start();

    createStorageAccess();
    createFunctionTableSchema();
    createClientService();
    registerContracts();
    registerFunctions();
  }

  @AfterAll
  void tearDownCluster() {
    if (transactionAdmin != null) {
      try {
        transactionAdmin.dropTable(FUNCTION_NAMESPACE, FUNCTION_TEST_TABLE);
        transactionAdmin.dropNamespace(FUNCTION_NAMESPACE);
      } catch (Exception e) {
        logger.warn("Failed to drop function table", e);
      }
      transactionAdmin.close();
    }
    if (storage != null) {
      storage.close();
    }
    if (cluster != null) {
      try {
        Properties props = cluster.getStorageConfig().getPropertiesForHost();
        unloadSchemas(props);
      } catch (Exception e) {
        logger.warn("Failed to unload table", e);
      }
      cluster.close();
    }
  }

  private void createStorageAccess() {
    Properties props = cluster.getStorageConfig().getPropertiesForHost();
    storage = StorageFactory.create(props).getStorage();
    transactionAdmin = TransactionFactory.create(props).getTransactionAdmin();
  }

  private void createFunctionTableSchema() throws Exception {
    transactionAdmin.createNamespace(FUNCTION_NAMESPACE, true);
    transactionAdmin.createTable(
        FUNCTION_NAMESPACE, FUNCTION_TEST_TABLE, FUNCTION_TEST_TABLE_METADATA, true);
  }

  /**
   * Unloads the schemas created for this test. Override to unload additional schemas.
   *
   * @param props ScalarDB properties (host-side view)
   * @throws Exception if the unload fails
   */
  protected void unloadSchemas(Properties props) throws Exception {
    SchemaLoader.unload(props, TestSchemas.getLedgerSchema(), true);
  }

  /**
   * Creates client properties for connecting to the servers. Override to add additional connection
   * settings (e.g., for Auditor).
   *
   * @return Properties containing the server connection settings
   */
  protected Properties createServerConnectionProperties() {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    return props;
  }

  private void createClientService() throws IOException {
    Properties props = createServerConnectionProperties();
    props.setProperty(ClientConfig.AUTHENTICATION_METHOD, getAuthenticationMethod().getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, TestCertificates.ENTITY_ID_A);
    if (getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
      props.setProperty(ClientConfig.DS_PRIVATE_KEY_PEM, TestCertificates.PRIVATE_KEY_A);
      props.setProperty(ClientConfig.DS_CERT_PEM, TestCertificates.CERTIFICATE_A);
      props.setProperty(ClientConfig.DS_CERT_VERSION, "1");
    } else {
      props.setProperty(ClientConfig.HMAC_SECRET_KEY, TestCertificates.SECRET_KEY_A);
      props.setProperty(ClientConfig.HMAC_SECRET_KEY_VERSION, "1");
    }
    clientService = clientServiceFactory.create(new ClientConfig(props));
  }

  private void registerContracts() {
    String getBalanceProperties =
        Json.createObjectBuilder()
            .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID1)
            .build()
            .toString();
    clientService.registerContract(
        CREATE_CONTRACT_ID1,
        Create.class.getName(),
        Common.getClassBytes(Create.class),
        (String) null);
    clientService.registerContract(
        GET_BALANCE_CONTRACT_ID1,
        GetBalance.class.getName(),
        Common.getClassBytes(GetBalance.class),
        getBalanceProperties);
    clientService.registerContract(
        NESTED_TARGET_CONTRACT_ID,
        GetBalance.class.getName(),
        Common.getClassBytes(GetBalance.class),
        getBalanceProperties);
    clientService.registerContract(
        NESTED_INVOKER_CONTRACT_ID,
        NestedInvoker.class.getName(),
        Common.getClassBytes(NestedInvoker.class),
        (String) null);
    clientService.registerContract(
        NOOP_CONTRACT_ID, Noop.class.getName(), Common.getClassBytes(Noop.class), (String) null);
  }

  private void registerFunctions() {
    clientService.registerFunction(
        UPSERT_FUNCTION_ID,
        UpsertFunction.class.getName(),
        Common.getClassBytes(UpsertFunction.class));
  }

  @Test
  void executeContract_AfterAllDatabaseConnectionsKilled_ShouldReconnectAndSucceed()
      throws Exception {
    Properties storageProperties = cluster.getStorageConfig().getPropertiesForHost();
    assumeTrue(
        ConnectionKiller.isSupported(storageProperties), "This test only applies to JDBC storages");

    // Arrange: create an asset, and confirm that an in-contract read works with warm pools.
    JsonObject createArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT)
            .build();
    clientService.executeContract(CREATE_CONTRACT_ID1, createArgument);
    JsonObject getBalanceArgument =
        Json.createObjectBuilder().add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID).build();
    JsonObject warmResult =
        clientService
            .executeContract(GET_BALANCE_CONTRACT_ID1, getBalanceArgument)
            .getResult()
            .get();
    assertThat(warmResult.getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT);

    // Act: kill all database connections so that the next in-contract read must open a new
    // physical connection.
    killAllConnectionsAndAwaitWorkerExpiry(storageProperties);
    ContractExecutionResult result =
        executeAssertingReconnection(GET_BALANCE_CONTRACT_ID1, getBalanceArgument, null, null);

    // Assert
    assertThat(result.getResult().get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT);
  }

  @Test
  void executeContract_NestedInvocationAfterAllDatabaseConnectionsKilled_ShouldReconnectAndSucceed()
      throws Exception {
    Properties storageProperties = cluster.getStorageConfig().getPropertiesForHost();
    assumeTrue(
        ConnectionKiller.isSupported(storageProperties), "This test only applies to JDBC storages");

    // Arrange: create an asset, and warm the invoker contract by nesting an already-used callee,
    // so that the invoker's own registry lookup (which happens on the framework stack) is cached.
    // The dedicated nested target must stay untouched here: after the kill, its registry lookup
    // has to be the first registry-side database access of the execution, issued from within the
    // invoker contract's restricted ProtectionDomain.
    JsonObject createArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, NESTED_ASSET_ID)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT)
            .build();
    clientService.executeContract(CREATE_CONTRACT_ID1, createArgument);
    JsonObject warmArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, NESTED_ASSET_ID)
            .add(CONTRACT_ID_ATTRIBUTE_NAME, GET_BALANCE_CONTRACT_ID1)
            .build();
    JsonObject warmResult =
        clientService.executeContract(NESTED_INVOKER_CONTRACT_ID, warmArgument).getResult().get();
    assertThat(warmResult.getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT);

    // Act: kill all database connections and invoke the contract that nests the never-used
    // callee, so that the callee's registry lookup must open a new physical connection.
    killAllConnectionsAndAwaitWorkerExpiry(storageProperties);
    JsonObject nestedArgument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, NESTED_ASSET_ID)
            .add(CONTRACT_ID_ATTRIBUTE_NAME, NESTED_TARGET_CONTRACT_ID)
            .build();
    ContractExecutionResult result =
        executeAssertingReconnection(NESTED_INVOKER_CONTRACT_ID, nestedArgument, null, null);

    // Assert
    assertThat(result.getResult().get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT);
  }

  @Test
  void executeFunction_AfterAllDatabaseConnectionsKilled_ShouldReconnectAndSucceed()
      throws Exception {
    Properties storageProperties = cluster.getStorageConfig().getPropertiesForHost();
    assumeTrue(
        ConnectionKiller.isSupported(storageProperties), "This test only applies to JDBC storages");

    // Arrange: nothing to warm. The contract is a no-op, so the function's read is the first
    // transaction-side database access after the kill and is issued from within the function's
    // restricted ProtectionDomain. (The function-registry read that precedes it runs on the
    // framework stack and re-warms only the registry-side connection pool.)
    JsonObject contractArgument = Json.createObjectBuilder().build();
    JsonObject functionArgument =
        Json.createObjectBuilder()
            .add(ID_ATTRIBUTE_NAME, SOME_FUNCTION_ROW_ID)
            .add(BALANCE_ATTRIBUTE_NAME, SOME_AMOUNT)
            .add(NAMESPACE_ATTRIBUTE_NAME, FUNCTION_NAMESPACE)
            .build();

    // Act
    killAllConnectionsAndAwaitWorkerExpiry(storageProperties);
    executeAssertingReconnection(
        NOOP_CONTRACT_ID, contractArgument, UPSERT_FUNCTION_ID, functionArgument);

    // Assert
    Get get =
        Get.newBuilder()
            .namespace(FUNCTION_NAMESPACE)
            .table(FUNCTION_TEST_TABLE)
            .partitionKey(Key.ofText(ID_ATTRIBUTE_NAME, SOME_FUNCTION_ROW_ID))
            .build();
    Optional<Result> row = storage.get(get);
    assertThat(row).isPresent();
    assertThat(row.get().getInt(BALANCE_ATTRIBUTE_NAME)).isEqualTo(SOME_AMOUNT);
  }

  /**
   * Kills all server-side database connections so that the next database access must open a new
   * physical connection. Kills twice to also remove connections that were created between the first
   * kill and the expiry of HikariCP's connection-adding worker threads. The first kill must have
   * terminated at least one server-side session; otherwise the reconnection scenario is not
   * actually exercised (e.g., if the configured user lacks the privilege to see other sessions),
   * and a subsequent green result would be a false pass.
   *
   * <p>Note that only the first access that opens a new physical connection per connection pool
   * exercises the reconnection, so every scenario needs its own kill.
   *
   * @param storageProperties ScalarDB properties (host-side view)
   * @throws InterruptedException if interrupted while sleeping
   * @throws SQLException if killing the connections fails
   */
  private void killAllConnectionsAndAwaitWorkerExpiry(Properties storageProperties)
      throws InterruptedException, SQLException {
    Thread.sleep(POOL_SETTLE_MILLIS);
    int firstKilled = ConnectionKiller.killAllOtherConnections(storageProperties);
    assertThat(firstKilled)
        .as("Expected at least one server-side session to be terminated by the first kill")
        .isGreaterThan(0);
    Thread.sleep(WORKER_EXPIRY_MILLIS);
    ConnectionKiller.killAllOtherConnections(storageProperties);
    Thread.sleep(WORKER_EXPIRY_MILLIS);
  }

  private ContractExecutionResult executeAssertingReconnection(
      String contractId,
      JsonObject contractArgument,
      @Nullable String functionId,
      @Nullable JsonObject functionArgument) {
    try {
      if (functionId == null) {
        return clientService.executeContract(contractId, contractArgument);
      }
      return clientService.executeContract(
          contractId, contractArgument, functionId, functionArgument);
    } catch (ClientException e) {
      fail(
          "Contract execution failed after all database connections were killed. This usually"
              + " means the JDBC driver could not re-establish a connection under the contract's"
              + " restricted ProtectionDomain. Status: "
              + e.getStatusCode()
              + ", message: "
              + e.getMessage()
              + extractPermissionFailures());
      throw e; // unreachable
    }
  }

  /**
   * Returns the logs of the server containers keyed by container name, used to diagnose failures.
   * Override to add additional containers (e.g., Auditor).
   *
   * @return Container logs keyed by container name
   */
  protected Map<String, String> getServerLogsByName() {
    Map<String, String> logs = new LinkedHashMap<>();
    logs.put("ledger", cluster.getLedger().getLogs());
    return logs;
  }

  private String extractPermissionFailures() {
    StringBuilder builder = new StringBuilder();
    try {
      for (Map.Entry<String, String> entry : getServerLogsByName().entrySet()) {
        String suspiciousLines =
            Arrays.stream(entry.getValue().split("\n"))
                .filter(
                    line ->
                        line.contains("AccessControlException")
                            || line.contains("access denied")
                            || line.contains("SQLTransientConnectionException")
                            || line.contains("does not support authentication protocol"))
                .distinct()
                .map(line -> "[" + entry.getKey() + "] " + line)
                .collect(Collectors.joining("\n"));
        if (!suspiciousLines.isEmpty()) {
          builder.append("\n").append(suspiciousLines);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to extract server container logs", e);
      return "";
    }
    if (builder.length() == 0) {
      return "";
    }
    return "\nSuspicious lines in the server container logs:" + builder;
  }
}
