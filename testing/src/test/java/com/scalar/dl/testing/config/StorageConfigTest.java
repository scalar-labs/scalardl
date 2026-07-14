package com.scalar.dl.testing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.config.DatabaseConfig;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class StorageConfigTest {

  @Test
  void forExternalStorage_shouldMapPropertiesToScalarDb() {
    Properties scalardbProps = new Properties();
    scalardbProps.setProperty("scalardb.storage", "jdbc");
    scalardbProps.setProperty(
        "scalardb.contact_points", "jdbc:postgresql://localhost:5432/postgres");
    scalardbProps.setProperty("scalardb.username", "postgres");
    scalardbProps.setProperty("scalardb.password", "postgres");

    StorageConfig config =
        StorageConfig.forExternalStorage(TransactionMode.CONSENSUS_COMMIT, scalardbProps);

    Properties hostProps = config.getPropertiesForHost();
    assertThat(hostProps.getProperty(DatabaseConfig.STORAGE)).isEqualTo("jdbc");
    assertThat(hostProps.getProperty(DatabaseConfig.CONTACT_POINTS))
        .isEqualTo("jdbc:postgresql://localhost:5432/postgres");
    assertThat(hostProps.getProperty(DatabaseConfig.USERNAME)).isEqualTo("postgres");
    assertThat(hostProps.getProperty(DatabaseConfig.PASSWORD)).isEqualTo("postgres");
  }

  @Test
  void forExternalStorage_shouldRewriteLocalhostInContainerEndpointProperties() {
    Properties scalardbProps = new Properties();
    scalardbProps.setProperty("scalardb.storage", "dynamo");
    scalardbProps.setProperty("scalardb.contact_points", "us-west-2");
    scalardbProps.setProperty("scalardb.dynamo.endpoint_override", "http://localhost:8000");

    StorageConfig config =
        StorageConfig.forExternalStorage(TransactionMode.CONSENSUS_COMMIT, scalardbProps);

    Properties hostProps = config.getPropertiesForHost();
    assertThat(hostProps.getProperty("scalar.db.dynamo.endpoint_override"))
        .isEqualTo("http://localhost:8000");

    Properties containerProps = config.getPropertiesForContainer();
    assertThat(containerProps.getProperty("scalar.db.dynamo.endpoint_override"))
        .isEqualTo("http://host.testcontainers.internal:8000");
    assertThat(containerProps.getProperty(DatabaseConfig.CONTACT_POINTS)).isEqualTo("us-west-2");
  }

  @Test
  void forExternalStorage_shouldForwardAdditionalScalarDbProperties() {
    Properties scalardbProps = new Properties();
    scalardbProps.setProperty("scalardb.storage", "cassandra");
    scalardbProps.setProperty("scalardb.contact_points", "localhost");
    scalardbProps.setProperty("scalardb.cassandra.username", "cassandra");
    scalardbProps.setProperty("scalardb.cassandra.password", "cassandra");

    StorageConfig config =
        StorageConfig.forExternalStorage(TransactionMode.CONSENSUS_COMMIT, scalardbProps);

    Properties containerProps = config.getPropertiesForContainer();
    assertThat(containerProps.getProperty(DatabaseConfig.CONTACT_POINTS))
        .isEqualTo("host.testcontainers.internal");
    assertThat(containerProps.getProperty("scalar.db.cassandra.username")).isEqualTo("cassandra");
    assertThat(containerProps.getProperty("scalar.db.cassandra.password")).isEqualTo("cassandra");
  }

  @Test
  void forExternalStorage_withJdbcTransactionMode_shouldSetTransactionManager() {
    Properties scalardbProps = new Properties();
    scalardbProps.setProperty("scalardb.storage", "jdbc");
    scalardbProps.setProperty("scalardb.contact_points", "jdbc:mysql://localhost:3306/");
    scalardbProps.setProperty("scalardb.username", "root");
    scalardbProps.setProperty("scalardb.password", "mysql");

    StorageConfig config =
        StorageConfig.forExternalStorage(TransactionMode.JDBC, scalardbProps);

    Properties props = config.getPropertiesForHost();
    assertThat(props.getProperty(DatabaseConfig.TRANSACTION_MANAGER)).isEqualTo("jdbc");
  }
}
