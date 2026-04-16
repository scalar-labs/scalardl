package com.scalar.dl.testing.config;

import com.scalar.db.config.DatabaseConfig;
import java.util.Properties;
import javax.annotation.Nullable;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Configuration for ScalarDB storage. Provides properties for both host-side (test case) and
 * container-side usage.
 *
 * <p>This class supports two modes:
 *
 * <ul>
 *   <li>Container DB mode: Uses MySQLContainer for storage
 *   <li>External DB mode: Uses explicitly provided connection settings
 * </ul>
 */
public class StorageConfig {

  private static final String CONTAINER_HOSTNAME = "host.testcontainers.internal";

  // MySQL container settings
  private static final int MYSQL_PORT = 3306;
  private static final String MYSQL_USERNAME = "root";
  private static final String MYSQL_PASSWORD = "mysql";

  // Storage type constants
  public static final String STORAGE_JDBC = "jdbc";

  private final String storage;
  private final TransactionMode transactionMode;
  @Nullable private final String containerNetworkAlias;
  @Nullable private final MySQLContainer mysqlContainer;

  // External DB settings
  @Nullable private final String contactPoints;
  @Nullable private final String username;
  @Nullable private final String password;

  /**
   * Creates a StorageConfig for MySQLContainer.
   *
   * @param container The MySQLContainer instance
   * @param networkAlias Network alias of the MySQL container for inter-container communication
   * @param transactionMode Transaction mode
   * @return StorageConfig configured for MySQL container
   */
  public static StorageConfig forMySQLContainer(
      MySQLContainer container, String networkAlias, TransactionMode transactionMode) {
    return new StorageConfig(
        STORAGE_JDBC, transactionMode, networkAlias, container, null, null, null);
  }

  /**
   * Creates a StorageConfig for external database.
   *
   * @param storage Storage type (jdbc, cassandra)
   * @param transactionMode Transaction mode
   * @param contactPoints JDBC URL or contact points
   * @param username Database username
   * @param password Database password
   * @return StorageConfig configured for external database
   */
  public static StorageConfig forExternalDatabase(
      String storage,
      TransactionMode transactionMode,
      String contactPoints,
      String username,
      String password) {
    return new StorageConfig(
        storage, transactionMode, null, null, contactPoints, username, password);
  }

  private StorageConfig(
      String storage,
      TransactionMode transactionMode,
      @Nullable String containerNetworkAlias,
      @Nullable MySQLContainer mysqlContainer,
      @Nullable String contactPoints,
      @Nullable String username,
      @Nullable String password) {
    this.storage = storage;
    this.transactionMode = transactionMode;
    this.containerNetworkAlias = containerNetworkAlias;
    this.mysqlContainer = mysqlContainer;
    this.contactPoints = contactPoints;
    this.username = username;
    this.password = password;
  }

  /**
   * Returns the transaction mode.
   *
   * @return The TransactionMode
   */
  public TransactionMode getTransactionMode() {
    return transactionMode;
  }

  /**
   * Returns ScalarDB properties for host-side usage (test cases).
   *
   * @return Properties configured for host-side access to the database
   */
  public Properties getPropertiesForHost() {
    Properties props = new Properties();

    if (mysqlContainer != null) {
      props.put(DatabaseConfig.STORAGE, STORAGE_JDBC);
      props.put(DatabaseConfig.CONTACT_POINTS, mysqlContainer.getJdbcUrl());
      props.put(DatabaseConfig.USERNAME, mysqlContainer.getUsername());
      props.put(DatabaseConfig.PASSWORD, mysqlContainer.getPassword());
    } else if (contactPoints != null) {
      props.put(DatabaseConfig.STORAGE, storage);
      props.put(DatabaseConfig.CONTACT_POINTS, contactPoints);
      if (username != null && !username.isEmpty()) {
        props.put(DatabaseConfig.USERNAME, username);
      }
      if (password != null && !password.isEmpty()) {
        props.put(DatabaseConfig.PASSWORD, password);
      }
    } else {
      throw new IllegalStateException(
          "StorageConfig must have either mysqlContainer or contactPoints");
    }

    if (transactionMode == TransactionMode.JDBC) {
      props.put(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");
    }

    return props;
  }

  /**
   * Returns ScalarDB properties for container-side usage. Uses the container network alias for
   * inter-container communication.
   *
   * @return Properties configured for container-side access
   */
  public Properties getPropertiesForContainer() {
    Properties props = new Properties();

    if (containerNetworkAlias != null) {
      props.put(DatabaseConfig.STORAGE, STORAGE_JDBC);
      props.put(
          DatabaseConfig.CONTACT_POINTS,
          "jdbc:mysql://" + containerNetworkAlias + ":" + MYSQL_PORT + "/?permitMysqlScheme=true");
      props.put(DatabaseConfig.USERNAME, MYSQL_USERNAME);
      props.put(DatabaseConfig.PASSWORD, MYSQL_PASSWORD);
    } else if (contactPoints != null) {
      // External DB - container accesses via host.testcontainers.internal
      String containerContactPoints = contactPoints.replace("localhost", CONTAINER_HOSTNAME);
      props.put(DatabaseConfig.STORAGE, storage);
      props.put(DatabaseConfig.CONTACT_POINTS, containerContactPoints);
      if (username != null && !username.isEmpty()) {
        props.put(DatabaseConfig.USERNAME, username);
      }
      if (password != null && !password.isEmpty()) {
        props.put(DatabaseConfig.PASSWORD, password);
      }
    } else {
      throw new IllegalStateException(
          "StorageConfig must have either containerNetworkAlias or contactPoints");
    }

    if (transactionMode == TransactionMode.JDBC) {
      props.put(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");
    }

    return props;
  }
}
