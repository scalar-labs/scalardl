package com.scalar.dl.testing.util;

import com.scalar.db.config.DatabaseConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that forcibly terminates all database connections except the one it uses. It is used to
 * simulate connection loss (e.g., a server-side idle timeout, a database restart, or a network
 * interruption) so that tests can verify components re-establish connections properly.
 *
 * <p>Supported JDBC databases: MySQL, MariaDB, PostgreSQL (and compatibles), SQL Server, Oracle,
 * and Db2. The configured user must have the privilege to terminate other sessions (e.g., an
 * administrative user).
 */
@SuppressFBWarnings(
    value = {"OBL_UNSATISFIED_OBLIGATION", "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE"},
    justification =
        "OBL: the Statement and ResultSet are closed by try-with-resources; this is a known"
            + " SpotBugs false positive when the try-with-resources resource is reused afterward."
            + " SQL: the session/application identifiers are numeric values read from the result"
            + " set, and the KILL/ALTER SYSTEM statements cannot take bind parameters, so there is"
            + " no injection risk. This is a test-only utility.")
public final class ConnectionKiller {
  private static final Logger logger = LoggerFactory.getLogger(ConnectionKiller.class);

  private ConnectionKiller() {}

  /**
   * Returns true if the given storage properties point to a JDBC database that this utility
   * supports.
   *
   * @param storageProperties ScalarDB properties (host-side view)
   * @return true if connections can be killed for this storage
   */
  public static boolean isSupported(Properties storageProperties) {
    String storage = storageProperties.getProperty(DatabaseConfig.STORAGE, "");
    String url = storageProperties.getProperty(DatabaseConfig.CONTACT_POINTS, "");
    return storage.equals("jdbc")
        && (url.startsWith("jdbc:mysql:")
            || url.startsWith("jdbc:mariadb:")
            || url.startsWith("jdbc:postgresql:")
            || url.startsWith("jdbc:sqlserver:")
            || url.startsWith("jdbc:oracle:")
            || url.startsWith("jdbc:db2:"));
  }

  /**
   * Terminates all database connections other than the one used by this method.
   *
   * @param storageProperties ScalarDB properties (host-side view) containing the JDBC URL and
   *     administrative credentials
   * @return the number of other sessions that were successfully terminated
   * @throws SQLException if the sessions cannot be enumerated
   */
  public static int killAllOtherConnections(Properties storageProperties) throws SQLException {
    String url = storageProperties.getProperty(DatabaseConfig.CONTACT_POINTS);
    Properties info = new Properties();
    String username = storageProperties.getProperty(DatabaseConfig.USERNAME);
    String password = storageProperties.getProperty(DatabaseConfig.PASSWORD);
    if (username != null) {
      info.put("user", username);
    }
    if (password != null) {
      info.put("password", password);
    }

    if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:")) {
      // ScalarDB uses the MariaDB driver for MySQL, so let it accept the jdbc:mysql scheme.
      String mariadbUrl = url.replaceFirst("^jdbc:mysql:", "jdbc:mariadb:");
      return killMySqlConnections(mariadbUrl, info);
    } else if (url.startsWith("jdbc:postgresql:")) {
      return killPostgresConnections(url, info);
    } else if (url.startsWith("jdbc:sqlserver:")) {
      return killSqlServerConnections(url, info);
    } else if (url.startsWith("jdbc:oracle:")) {
      return killOracleConnections(url, info);
    } else if (url.startsWith("jdbc:db2:")) {
      return killDb2Connections(url, info);
    } else {
      throw new IllegalArgumentException("Unsupported JDBC URL: " + url);
    }
  }

  private static int killMySqlConnections(String url, Properties info) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, info);
        Statement statement = connection.createStatement()) {
      List<Long> ids = new ArrayList<>();
      try (ResultSet resultSet =
          statement.executeQuery(
              "SELECT id FROM information_schema.processlist WHERE id <> CONNECTION_ID()")) {
        while (resultSet.next()) {
          ids.add(resultSet.getLong(1));
        }
      }
      int killed = 0;
      for (long id : ids) {
        try {
          statement.execute("KILL " + id);
          killed++;
        } catch (SQLException e) {
          logger.debug("Failed to kill connection {} (it may already be gone)", id);
        }
      }
      logger.info("Killed {} MySQL/MariaDB connections", killed);
      return killed;
    }
  }

  private static int killPostgresConnections(String url, Properties info) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, info);
        Statement statement = connection.createStatement()) {
      int killed = 0;
      try (ResultSet resultSet =
          statement.executeQuery(
              "SELECT pg_terminate_backend(pid) FROM pg_stat_activity"
                  + " WHERE pid <> pg_backend_pid() AND backend_type = 'client backend'")) {
        while (resultSet.next()) {
          if (resultSet.getBoolean(1)) {
            killed++;
          }
        }
      }
      logger.info("Killed {} PostgreSQL connections", killed);
      return killed;
    }
  }

  private static int killSqlServerConnections(String url, Properties info) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, info);
        Statement statement = connection.createStatement()) {
      List<Integer> ids = new ArrayList<>();
      try (ResultSet resultSet =
          statement.executeQuery(
              "SELECT session_id FROM sys.dm_exec_sessions"
                  + " WHERE is_user_process = 1 AND session_id <> @@SPID")) {
        while (resultSet.next()) {
          ids.add(resultSet.getInt(1));
        }
      }
      int killed = 0;
      for (int id : ids) {
        try {
          statement.execute("KILL " + id);
          killed++;
        } catch (SQLException e) {
          logger.debug("Failed to kill session {} (it may already be gone)", id);
        }
      }
      logger.info("Killed {} SQL Server sessions", killed);
      return killed;
    }
  }

  private static int killOracleConnections(String url, Properties info) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, info);
        Statement statement = connection.createStatement()) {
      List<String> sessions = new ArrayList<>();
      try (ResultSet resultSet =
          statement.executeQuery(
              "SELECT sid, serial# FROM v$session"
                  + " WHERE type = 'USER' AND sid <> SYS_CONTEXT('USERENV', 'SID')")) {
        while (resultSet.next()) {
          sessions.add(resultSet.getLong(1) + "," + resultSet.getLong(2));
        }
      }
      int killed = 0;
      for (String session : sessions) {
        try {
          statement.execute("ALTER SYSTEM KILL SESSION '" + session + "' IMMEDIATE");
          killed++;
        } catch (SQLException e) {
          logger.debug("Failed to kill session {} (it may already be gone)", session);
        }
      }
      logger.info("Killed {} Oracle sessions", killed);
      return killed;
    }
  }

  private static int killDb2Connections(String url, Properties info) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, info);
        Statement statement = connection.createStatement()) {
      List<Long> handles = new ArrayList<>();
      try (ResultSet resultSet =
          statement.executeQuery(
              "SELECT application_handle FROM TABLE(MON_GET_CONNECTION(CAST(NULL AS BIGINT), -2))"
                  + " WHERE application_handle <> MON_GET_APPLICATION_HANDLE()")) {
        while (resultSet.next()) {
          handles.add(resultSet.getLong(1));
        }
      }
      int killed = 0;
      for (long handle : handles) {
        try {
          statement.execute("CALL SYSPROC.ADMIN_CMD('FORCE APPLICATION (" + handle + ")')");
          killed++;
        } catch (SQLException e) {
          logger.debug("Failed to force application {} (it may already be gone)", handle);
        }
      }
      // FORCE APPLICATION is asynchronous; callers should wait before relying on the result.
      logger.info("Forced {} Db2 applications", killed);
      return killed;
    }
  }
}
