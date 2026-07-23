package com.scalar.dl.testing.config;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.storage.dynamo.DynamoConfig;
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
 *   <li>External DB mode: Uses explicitly provided {@code scalardb.*} system properties
 * </ul>
 */
public class StorageConfig {

  private static final String SCALARDB_PREFIX = "scalardb.";
  private static final String CONTAINER_HOSTNAME = "host.testcontainers.internal";

  /**
   * ScalarDB keys whose values may contain a host endpoint and need {@code localhost} rewritten for
   * containers. Grow this allowlist (and tests) when ScalarDB adds new endpoint keys.
   */
  private static final ImmutableSet<String> ENDPOINT_PROPERTIES =
      ImmutableSet.of(DatabaseConfig.CONTACT_POINTS, DynamoConfig.ENDPOINT_OVERRIDE);

  // MySQL container settings
  private static final int MYSQL_PORT = 3306;
  private static final String MYSQL_USERNAME = "root";
  private static final String MYSQL_PASSWORD = "mysql";

  // Storage type constants
  public static final String STORAGE_JDBC = "jdbc";

  private final TransactionMode transactionMode;
  @Nullable private final String containerNetworkAlias;
  @Nullable private final MySQLContainer mysqlContainer;
  @Nullable private final Properties scalardbProperties;

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
    return new StorageConfig(transactionMode, networkAlias, container, null);
  }

  /**
   * Creates a StorageConfig for external storage using all {@code scalardb.*} properties.
   *
   * @param transactionMode Transaction mode
   * @param scalardbProperties Properties whose keys use the {@code scalardb.} prefix
   * @return StorageConfig configured for external storage
   */
  public static StorageConfig forExternalStorage(
      TransactionMode transactionMode, Properties scalardbProperties) {
    return new StorageConfig(transactionMode, null, null, scalardbProperties);
  }

  private StorageConfig(
      TransactionMode transactionMode,
      @Nullable String containerNetworkAlias,
      @Nullable MySQLContainer mysqlContainer,
      @Nullable Properties scalardbProperties) {
    this.transactionMode = transactionMode;
    this.containerNetworkAlias = containerNetworkAlias;
    this.mysqlContainer = mysqlContainer;
    this.scalardbProperties = scalardbProperties;
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
    if (mysqlContainer != null) {
      return getMySQLContainerProperties();
    }
    if (scalardbProperties != null) {
      return toScalarDbProperties(scalardbProperties, false);
    }
    throw new IllegalStateException(
        "StorageConfig must have either mysqlContainer or scalardbProperties");
  }

  /**
   * Returns ScalarDB properties for container-side usage. Uses the container network alias for
   * inter-container communication.
   *
   * @return Properties configured for container-side access
   */
  public Properties getPropertiesForContainer() {
    if (containerNetworkAlias != null) {
      return getMySQLContainerPropertiesForNetworkAlias(containerNetworkAlias);
    }
    if (scalardbProperties != null) {
      return toScalarDbProperties(scalardbProperties, true);
    }
    throw new IllegalStateException(
        "StorageConfig must have either containerNetworkAlias or scalardbProperties");
  }

  private Properties getMySQLContainerProperties() {
    Properties props = new Properties();
    props.put(DatabaseConfig.STORAGE, STORAGE_JDBC);
    props.put(DatabaseConfig.CONTACT_POINTS, mysqlContainer.getJdbcUrl());
    props.put(DatabaseConfig.USERNAME, mysqlContainer.getUsername());
    props.put(DatabaseConfig.PASSWORD, mysqlContainer.getPassword());
    applyTransactionManager(props);
    return props;
  }

  private Properties getMySQLContainerPropertiesForNetworkAlias(String networkAlias) {
    Properties props = new Properties();
    props.put(DatabaseConfig.STORAGE, STORAGE_JDBC);
    props.put(
        DatabaseConfig.CONTACT_POINTS, "jdbc:mysql://" + networkAlias + ":" + MYSQL_PORT + "/");
    props.put(DatabaseConfig.USERNAME, MYSQL_USERNAME);
    props.put(DatabaseConfig.PASSWORD, MYSQL_PASSWORD);
    applyTransactionManager(props);
    return props;
  }

  private Properties toScalarDbProperties(Properties source, boolean rewriteLocalhost) {
    Properties props = new Properties();
    source.forEach(
        (key, value) -> {
          String propertyName = (String) key;
          if (!propertyName.startsWith(SCALARDB_PREFIX)) {
            return;
          }
          String scalarDbKey =
              DatabaseConfig.PREFIX + propertyName.substring(SCALARDB_PREFIX.length());
          String propertyValue = (String) value;
          if (rewriteLocalhost && isEndpointProperty(scalarDbKey)) {
            propertyValue = propertyValue.replace("localhost", CONTAINER_HOSTNAME);
          }
          props.setProperty(scalarDbKey, propertyValue);
        });
    applyTransactionManager(props);
    return props;
  }

  private void applyTransactionManager(Properties props) {
    if (transactionMode == TransactionMode.JDBC) {
      props.put(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");
    }
  }

  private static boolean isEndpointProperty(String scalarDbKey) {
    return ENDPOINT_PROPERTIES.contains(scalarDbKey);
  }
}
