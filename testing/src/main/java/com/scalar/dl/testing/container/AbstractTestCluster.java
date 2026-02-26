package com.scalar.dl.testing.container;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.config.StorageConfig;
import com.scalar.dl.testing.config.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Abstract base class for test clusters using TestContainers.
 *
 * <p>This class provides common functionality for managing storage containers (MySQL) and network
 * configuration. Subclasses implement {@link #startContainers()} to start their specific
 * containers.
 *
 * <p>Supported system properties:
 *
 * <ul>
 *   <li>scalardb.storage - Storage type (jdbc). Default: jdbc
 *   <li>scalardb.contact_points - JDBC URL or contact points for external database
 *   <li>scalardb.username - Database username
 *   <li>scalardb.password - Database password
 *   <li>scalardb.port - Port to expose for external database access from containers
 * </ul>
 */
public abstract class AbstractTestCluster implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(AbstractTestCluster.class);

  // System property names
  private static final String PROP_STORAGE = "scalardb.storage";
  private static final String PROP_CONTACT_POINTS = "scalardb.contact_points";
  private static final String PROP_USERNAME = "scalardb.username";
  private static final String PROP_PASSWORD = "scalardb.password";
  private static final String PROP_PORT = "scalardb.port";

  // Default values
  private static final String DEFAULT_STORAGE = "jdbc";

  // MySQL container settings
  protected static final String MYSQL_NETWORK_ALIAS = "mysql";
  private static final String MYSQL_IMAGE = "mysql:8.0";
  private static final int MYSQL_PORT = 3306;

  // Cassandra settings (for external storage)
  private static final int CASSANDRA_PORT = 9042;

  protected final AuthenticationMethod authenticationMethod;
  protected final String storage;
  protected final TransactionMode transactionMode;

  protected Network network;
  protected MySQLContainer mysqlContainer;
  protected StorageConfig storageConfig;

  protected AbstractTestCluster(
      AuthenticationMethod authenticationMethod, TransactionMode transactionMode) {
    this.authenticationMethod = authenticationMethod;
    this.storage = System.getProperty(PROP_STORAGE, DEFAULT_STORAGE);
    this.transactionMode = transactionMode;
  }

  /** Starts the test cluster. */
  public void start() {
    logger.info(
        "Starting {}: authenticationMethod={}, storage={}, transactionMode={}, useExternalStorage={}",
        getClass().getSimpleName(),
        authenticationMethod,
        storage,
        transactionMode,
        useExternalStorage());

    // 1. Create network
    network = Network.newNetwork();

    // 2. Start storage container or expose host ports for external storage
    if (useExternalStorage()) {
      exposeHostPortsForStorage();
    } else {
      startStorageContainer();
    }

    // 3. Create storage config
    storageConfig = createStorageConfig();

    // 4. Start application containers (implemented by subclasses)
    startContainers();
  }

  /**
   * Starts the application containers (Ledger, Auditor, etc.).
   *
   * <p>This method is called after the storage container and network are ready. Subclasses should
   * implement this to start their specific containers.
   */
  protected abstract void startContainers();

  /**
   * Checks if external storage should be used based on system properties.
   *
   * @return true if external storage settings are provided
   */
  private boolean useExternalStorage() {
    return System.getProperty(PROP_CONTACT_POINTS) != null;
  }

  /** Starts the storage container based on storage type. */
  @SuppressWarnings("resource")
  private void startStorageContainer() {
    switch (storage) {
      case "jdbc":
        logger.info("Starting MySQL container");
        mysqlContainer =
            new MySQLContainer(MYSQL_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(MYSQL_NETWORK_ALIAS)
                .withUsername("root")
                .withPassword("mysql")
                .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("mysql"));
        mysqlContainer.start();
        logger.info(
            "MySQL container started on port {} (mapped from 3306)",
            mysqlContainer.getMappedPort(MYSQL_PORT));
        break;
      default:
        throw new UnsupportedOperationException(
            "Storage container not supported for: "
                + storage
                + ". Use external storage with -Dscalardb.contact_points=...");
    }
  }

  /** Exposes host ports for external storage access from containers. */
  private void exposeHostPortsForStorage() {
    String portProperty = System.getProperty(PROP_PORT);
    if (portProperty != null) {
      int port = Integer.parseInt(portProperty);
      Testcontainers.exposeHostPorts(port);
      logger.info("Exposed host port {} (from property)", port);
      return;
    }

    // Infer port from storage type
    int port = getDefaultPortForStorage();
    if (port > 0) {
      Testcontainers.exposeHostPorts(port);
      logger.info("Exposed host port {} for storage type: {}", port, storage);
    } else {
      logger.warn("No host port exposed for storage type: {}", storage);
    }
  }

  /**
   * Returns the default port for the configured storage type.
   *
   * @return Default port number, or -1 if unknown
   */
  private int getDefaultPortForStorage() {
    switch (storage) {
      case "jdbc":
        return MYSQL_PORT;
      case "cassandra":
        return CASSANDRA_PORT;
      default:
        return -1;
    }
  }

  /**
   * Creates a StorageConfig based on whether external or container storage is used.
   *
   * @return StorageConfig instance
   */
  private StorageConfig createStorageConfig() {
    if (useExternalStorage()) {
      String contactPoints = System.getProperty(PROP_CONTACT_POINTS);
      String username = System.getProperty(PROP_USERNAME, "");
      String password = System.getProperty(PROP_PASSWORD, "");
      return StorageConfig.forExternalDatabase(
          storage, transactionMode, contactPoints, username, password);
    } else {
      switch (storage) {
        case "jdbc":
          return StorageConfig.forMySQLContainer(
              mysqlContainer, MYSQL_NETWORK_ALIAS, transactionMode);
        default:
          throw new UnsupportedOperationException("Storage type not supported: " + storage);
      }
    }
  }

  /**
   * Returns the Ledger container.
   *
   * @return The LedgerContainer instance
   */
  public abstract LedgerContainer getLedger();

  /**
   * Returns the authentication method used by the cluster.
   *
   * @return The AuthenticationMethod
   */
  public AuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  /**
   * Returns the network used by the cluster.
   *
   * @return The Network instance
   */
  protected Network getNetwork() {
    return network;
  }

  /**
   * Returns the storage config used by the cluster.
   *
   * @return The StorageConfig instance
   */
  public StorageConfig getStorageConfig() {
    return storageConfig;
  }

  /** Stops the storage container and closes the network. Subclasses should call super.close(). */
  @Override
  public void close() {
    if (mysqlContainer != null) {
      try {
        mysqlContainer.stop();
      } catch (Exception e) {
        logger.warn("Error stopping MySQL container", e);
      }
    }

    if (network != null) {
      try {
        network.close();
      } catch (Exception e) {
        logger.warn("Error closing network", e);
      }
    }
  }
}
