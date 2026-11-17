package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.testing.config.TransactionMode;
import com.scalar.dl.testing.container.LedgerContainer;
import com.scalar.dl.testing.container.LedgerTestCluster;
import com.scalar.dl.testing.contract.Create;
import com.scalar.dl.testing.schema.TestSchemas;
import com.scalar.dl.testing.util.TestCertificates;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the integration tests of the restriction on contract execution in the default
 * namespace in the Ledger-only configuration.
 *
 * <p>Ledger rejects contract execution requests whose context namespace is the default namespace
 * when it is disabled by the configuration, or when a namespace created for import exists. Each
 * test starts its own cluster because the scenarios need different configurations. Whether a
 * namespace created for import exists is cached in Ledger for a while, so the tests restart Ledger
 * to make it check the namespaces again.
 */
public abstract class LedgerOnlyDefaultNamespaceContractExecutionIntegrationTestBase {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String SOME_NAMESPACE = "namespace1";
  private static final String IMPORTED_NAMESPACE = "imported_namespace";
  private static final String SOME_CONTRACT_ID = "Create";
  private static final String SOME_ASSET_ID_1 = "A";
  private static final String SOME_ASSET_ID_2 = "B";
  private static final String SOME_ASSET_ID_3 = "C";
  private static final int SOME_AMOUNT = 1000;
  private static final String ERROR_CODE = "DL-LEDGER-407008";
  private static final ObjectMapper mapper = new ObjectMapper();

  private LedgerTestCluster cluster;
  private ClientServiceFactory clientServiceFactory;

  /** The client whose context namespace is the default namespace. */
  private ClientService clientService;

  /**
   * Returns the Ledger Docker image to use. Override to use a custom image.
   *
   * @return The Docker image tag
   */
  protected String getLedgerImage() {
    return System.getProperty("scalardl.ledger.image", LedgerContainer.DEFAULT_IMAGE);
  }

  @AfterEach
  void tearDownCluster() {
    if (cluster == null) {
      return;
    }
    for (String namespace : new String[] {SOME_NAMESPACE, IMPORTED_NAMESPACE}) {
      try {
        clientService.dropNamespace(namespace);
      } catch (Exception e) {
        // The namespace might not have been created or might have already been dropped
        logger.debug("Failed to drop the namespace {}", namespace, e);
      }
    }
    releaseClientResources();
    try {
      Properties props = cluster.getStorageConfig().getPropertiesForHost();
      SchemaLoader.unload(props, TestSchemas.getLedgerSchema(), true);
    } catch (Exception e) {
      logger.warn("Failed to unload tables", e);
    }
    cluster.close();
    cluster = null;
  }

  @Test
  void
      executeContract_DefaultNamespaceContractExecutionDisabled_ShouldRejectOnlyInDefaultNamespace()
          throws Exception {
    // Arrange
    startCluster(false);

    // Act & Assert: a contract can be registered but cannot be executed in the default namespace
    registerContract(clientService);
    assertContractExecutionRejected(clientService, SOME_ASSET_ID_1);

    // Act & Assert: a contract can be executed in a non-default namespace
    clientService.createNamespace(SOME_NAMESPACE);
    ClientService namespaceClientService = createClientService(SOME_NAMESPACE);
    registerContract(namespaceClientService);
    executeContract(namespaceClientService, SOME_ASSET_ID_1);

    // Act & Assert: validating an asset is not affected because the validation without Auditor is
    // not contract execution
    assertThat(namespaceClientService.validateLedger(SOME_ASSET_ID_1).getCode())
        .isEqualTo(StatusCode.OK);
    assertThat(clientService.validateLedger(SOME_ASSET_ID_1).getCode())
        .isEqualTo(StatusCode.ASSET_NOT_FOUND);
  }

  @Test
  void executeContract_NamespaceCreatedForImportExists_ShouldRejectInDefaultNamespace()
      throws Exception {
    // Arrange
    startCluster(true);
    registerContract(clientService);

    // Act & Assert: a contract can be executed in the default namespace at first
    executeContract(clientService, SOME_ASSET_ID_1);

    // Act & Assert: it is rejected once a namespace created for import exists
    clientService.createNamespace(IMPORTED_NAMESPACE, true);
    restartLedger();
    assertContractExecutionRejected(clientService, SOME_ASSET_ID_2);

    // Act & Assert: it can be executed again once the namespace is dropped
    clientService.dropNamespace(IMPORTED_NAMESPACE);
    restartLedger();
    executeContract(clientService, SOME_ASSET_ID_3);
  }

  private void startCluster(boolean defaultNamespaceContractExecutionEnabled) throws IOException {
    cluster =
        new LedgerTestCluster(
            AuthenticationMethod.HMAC, TransactionMode.CONSENSUS_COMMIT, getLedgerImage());
    if (!defaultNamespaceContractExecutionEnabled) {
      cluster.withDefaultNamespaceContractExecutionDisabled();
    }
    cluster.start();
    createClientServices();
  }

  /**
   * Stops and starts the Ledger container, then recreates the client because Testcontainers remaps
   * host ports after restart.
   */
  private void restartLedger() throws IOException {
    cluster.getLedger().stop();
    cluster.getLedger().start();
    createClientServices();
  }

  private void createClientServices() throws IOException {
    releaseClientResources();
    clientServiceFactory = new ClientServiceFactory();
    clientService = createClientService(null);
  }

  private void releaseClientResources() {
    if (clientServiceFactory != null) {
      try {
        clientServiceFactory.close();
      } catch (Exception e) {
        logger.warn("Failed to shut down client service factory", e);
      }
    }
  }

  /**
   * Creates a client of the entity A. If a namespace is specified, the secret key of the entity is
   * registered in the namespace and the context namespace of the client is set to the namespace.
   */
  private ClientService createClientService(@Nullable String namespace) throws IOException {
    Properties props = new Properties();
    props.setProperty(ClientConfig.SERVER_HOST, "localhost");
    props.setProperty(ClientConfig.SERVER_PORT, String.valueOf(cluster.getLedger().getPort()));
    props.setProperty(
        ClientConfig.SERVER_PRIVILEGED_PORT,
        String.valueOf(cluster.getLedger().getPrivilegedPort()));
    props.setProperty(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(ClientConfig.ENTITY_ID, TestCertificates.ENTITY_ID_A);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY, TestCertificates.SECRET_KEY_A);
    props.setProperty(ClientConfig.HMAC_SECRET_KEY_VERSION, "1");
    if (namespace != null) {
      clientService.registerSecret(
          namespace,
          TestCertificates.ENTITY_ID_A,
          TestCertificates.KEY_VERSION,
          TestCertificates.SECRET_KEY_A);
      props.setProperty(ClientConfig.CONTEXT_NAMESPACE, namespace);
    }
    return clientServiceFactory.create(new ClientConfig(props));
  }

  private void registerContract(ClientService clientService) {
    clientService.registerContract(
        SOME_CONTRACT_ID, Create.class.getName(), Common.getClassBytes(Create.class));
  }

  private void executeContract(ClientService clientService, String assetId) {
    ObjectNode argument =
        mapper
            .createObjectNode()
            .put(ASSET_ATTRIBUTE_NAME, assetId)
            .put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT);
    clientService.executeContract(SOME_CONTRACT_ID, argument);
  }

  private void assertContractExecutionRejected(ClientService clientService, String assetId) {
    assertThatThrownBy(() -> executeContract(clientService, assetId))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> {
              assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_REQUEST);
              assertThat(e.getMessage()).startsWith(ERROR_CODE);
            });
  }
}
