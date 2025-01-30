package com.scalar.dl.ledger.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.moandjiezana.toml.Toml;
import com.scalar.db.config.DatabaseConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public class LedgerConfig implements ServerConfig, ServersHmacAuthenticatable {
  private static final Logger LOGGER = LoggerFactory.getLogger(LedgerConfig.class);
  @VisibleForTesting static final String PRODUCT_NAME = "scalardl";
  @VisibleForTesting static final String SERVICE_NAME = "ledger";
  @VisibleForTesting static final String DEFAULT_NAME = "Scalar Ledger";
  @VisibleForTesting static final String DEFAULT_NAMESPACE = "scalar";
  @VisibleForTesting static final int DEFAULT_PORT = 50051;
  @VisibleForTesting static final int DEFAULT_PRIVILEGED_PORT = 50052;
  @VisibleForTesting static final int DEFAULT_ADMIN_PORT = 50053;
  @VisibleForTesting static final int DEFAULT_PROMETHEUS_EXPORTER_PORT = 8080;
  @VisibleForTesting static final boolean DEFAULT_TLS_ENABLED = false;
  @VisibleForTesting static final boolean DEFAULT_PROOF_ENABLED = false;
  @VisibleForTesting static final boolean DEFAULT_FUNCTION_ENABLED = true;
  @VisibleForTesting static final boolean DEFAULT_AUDITOR_ENABLED = false;
  @VisibleForTesting static final String DEFAULT_AUDITOR_CERT_HOLDER_ID = "auditor";
  @VisibleForTesting static final int DEFAULT_AUDITOR_CERT_VERSION = 1;
  @VisibleForTesting static final boolean DEFAULT_DIRECT_ASSET_ACCESS_ENABLED = false;
  @VisibleForTesting static final boolean DEFAULT_TX_STATE_MANAGEMENT_ENABLED = false;

  @VisibleForTesting
  static final AuthenticationMethod DEFAULT_AUTHENTICATION_METHOD =
      AuthenticationMethod.DIGITAL_SIGNATURE;

  private static final String PREFIX = "scalar.dl.ledger.";
  private static final String SERVER_PREFIX = PREFIX + "server.";

  /**
   * <code>scalar.dl.ledger.name</code> (Optional)<br>
   * Name of ledger ("Scalar Ledger" by default). It is used to identify a ledger.
   */
  public static final String NAME = PREFIX + "name";
  /**
   * <code>scalar.dl.ledger.namespace</code> (Optional)<br>
   * Namespace of ledger tables ("scalar" by default)
   */
  public static final String NAMESPACE = PREFIX + "namespace";
  /**
   * <code>scalar.dl.ledger.authentication.method</code> (Optional)<br>
   * The authentication method for a client and servers. ("digital-signature" by default) This has
   * to be consistent with the client configuration.
   */
  public static final String AUTHENTICATION_METHOD = PREFIX + "authentication.method";
  /**
   * <code>scalar.dl.ledger.authentication.hmac.cipher_key</code> (Optional)<br>
   * A cipher key used to encrypt and decrypt the HMAC secret keys of client entities. This variable
   * is used only when <code>scalar.dl.ledger.authentication.method</code> is "hmac". Please set an
   * unpredictable and long enough value.
   */
  public static final String AUTHENTICATION_HMAC_CIPHER_KEY =
      PREFIX + "authentication.hmac.cipher_key";
  /**
   * <code>scalar.dl.ledger.server.port</code><br>
   * Server port (50051 by default).
   */
  public static final String SERVER_PORT = SERVER_PREFIX + "port";
  /**
   * <code>scalar.dl.ledger.server.privileged_port</code><br>
   * Server privileged port (50052 by default).
   */
  public static final String SERVER_PRIVILEGED_PORT = SERVER_PREFIX + "privileged_port";
  /**
   * <code>scalar.dl.ledger.server.admin_port</code><br>
   * Server admin port (50053 by default).
   */
  public static final String SERVER_ADMIN_PORT = SERVER_PREFIX + "admin_port";
  /**
   * <code>scalar.dl.ledger.server.prometheus_exporter_port</code><br>
   * Prometheus exporter port (8080 by default). Prometheus exporter will not be started if a
   * negative number is given.
   */
  public static final String SERVER_PROMETHEUS_EXPORTER_PORT =
      SERVER_PREFIX + "prometheus_exporter_port";
  /**
   * <code>scalar.dl.ledger.server.tls.enabled</code><br>
   * A flag to enable TLS between clients and servers (false by default).
   */
  public static final String SERVER_TLS_ENABLED = SERVER_PREFIX + "tls.enabled";
  /**
   * <code>scalar.dl.ledger.server.tls.cert_chain_path</code><br>
   * Certificate chain file used for TLS communication.
   */
  public static final String SERVER_TLS_CERT_CHAIN_PATH = SERVER_PREFIX + "tls.cert_chain_path";
  /**
   * <code>scalar.dl.ledger.server.tls.private_key_path</code><br>
   * Private key file used for TLS communication.
   */
  public static final String SERVER_TLS_PRIVATE_KEY_PATH = SERVER_PREFIX + "tls.private_key_path";
  /**
   * <code>scalar.dl.ledger.proof.enabled</code><br>
   * A flag to enable asset proof that is used to verify assets (false by default). This feature
   * must be enabled in both client and server.
   */
  public static final String PROOF_ENABLED = PREFIX + "proof.enabled";
  /**
   * <code>scalar.dl.ledger.proof.private_key_path</code><br>
   * The path of a private key file in PEM format. Either this or <code>
   * scalar.dl.ledger.proof.private_key_pem
   * </code> is used for signing proofs with digital signatures. The signatures are also used for
   * Auditor to authenticate the corresponding proofs from Ledger if <code>
   * scalar.dl.ledger.servers.authentication.hmac.secret_key</code> is empty.
   */
  public static final String PROOF_PRIVATE_KEY_PATH = PREFIX + "proof.private_key_path";
  /**
   * <code>scalar.dl.ledger.proof.private_key_pem</code><br>
   * PEM-encoded private key data. Either this or <code>scalar.dl.ledger.proof.private_key_path
   * </code> is used for signing proofs with digital signatures. The signatures are also used for
   * Auditor to authenticate the corresponding proofs from Ledger if <code>
   * scalar.dl.ledger.servers.authentication.hmac.secret_key</code> is empty.
   */
  public static final String PROOF_PRIVATE_KEY_PEM = PREFIX + "proof.private_key_pem";
  /**
   * <code>scalar.dl.ledger.function.enabled</code><br>
   * A flag to enable function for mutable database (true by default).
   */
  public static final String FUNCTION_ENABLED = PREFIX + "function.enabled";
  /**
   * <code>scalar.dl.ledger.auditor.enabled</code><br>
   * A flag to enable Auditor (false by default).
   */
  public static final String AUDITOR_ENABLED = PREFIX + "auditor.enabled";
  /**
   * <code>scalar.dl.ledger.servers.authentication.hmac.secret_key</code><br>
   * A secret key of HMAC for the authentication of messages between (Ledger and Auditor) servers.
   * The same key has to be set in the corresponding Auditor as well. If this is not set, Ledger
   * uses digital signature authentication using scalar.dl.ledger.proof.private_key_pem/path for
   * signing and scalar.dl.ledger.auditor.cert_holder_id and its corresponding certificate (stored
   * in the database) for verification.
   */
  public static final String SERVERS_AUTHENTICATION_HMAC_SECRET_KEY =
      PREFIX + "servers.authentication.hmac.secret_key";
  /**
   * <code>scalar.dl.ledger.auditor.cert_holder_id</code><br>
   * Auditor certificate holder ID ("auditor" by default).
   *
   * @deprecated This will be deleted in release 5.0.0 since Ledger-Auditor authentication will use
   *     HMAC only.
   */
  @Deprecated public static final String AUDITOR_CERT_HOLDER_ID = PREFIX + "auditor.cert_holder_id";
  /**
   * <code>scalar.dl.ledger.auditor.cert_version</code><br>
   * Auditor certificate version (1 by default).
   *
   * @deprecated This will be deleted in release 5.0.0 since Ledger-Auditor authentication will use
   *     HMAC only.
   */
  @Deprecated public static final String AUDITOR_CERT_VERSION = PREFIX + "auditor.cert_version";
  /**
   * <code>scalar.dl.ledger.executable_contracts</code><br>
   * Binary names of contracts that can be executed. Please use the following format.
   *
   * <pre>{@code
   * [[contracts]]
   * contract-binary-name = "com.org1.contract.StateUpdater"
   *
   * [[contracts]]
   * contract-binary-name = "com.org1.contract.StateReader"
   * }</pre>
   */
  public static final String EXECUTABLE_CONTRACTS = PREFIX + "executable_contracts";
  /**
   * <code>scalar.dl.ledger.direct_asset_access.enabled</code><br>
   * A flag to access asset table directly without going through asset_metadata (false by default).
   * This should be set to false for some databases such as Cassandra that incur multiple database
   * lookups for scanning a clustering key with limit 1.
   */
  public static final String DIRECT_ASSET_ACCESS_ENABLED = PREFIX + "direct_asset_access.enabled";
  /**
   * <code>scalar.dl.ledger.tx_state_management.enabled</code><br>
   * A flag to manage transaction states by Ledger (false by default). This must be enabled when
   * using JdbcTransactionManager as the transaction manager of ScalarDB.
   */
  public static final String TX_STATE_MANAGEMENT_ENABLED = PREFIX + "tx_state_management.enabled";

  private final Properties props;
  private String name;
  private String namespace;
  private AuthenticationMethod authenticationMethod;
  private String hmacCipherKey;
  private int port;
  private int privilegedPort;
  private int adminPort;
  private int prometheusExporterPort;
  private boolean isServerTlsEnabled;
  private String serverTlsCertChainPath;
  private String serverTlsPrivateKeyPath;
  private boolean isProofEnabled;
  private String proofPrivateKey;
  private boolean isFunctionEnabled;
  private boolean isAuditorEnabled;
  private String serversAuthHmacSecretKey;
  private String auditorCertHolderId;
  private int auditorCertVersion;
  private Set<String> executableContractNames;
  private boolean isDirectAssetAccessEnabled;
  private boolean isTxStateManagementEnabled;

  public LedgerConfig(File propertiesFile) throws IOException {
    try (FileInputStream stream = new FileInputStream(propertiesFile)) {
      props = new Properties();
      props.load(stream);
    }
    load();
  }

  public LedgerConfig(InputStream stream) throws IOException {
    props = new Properties();
    props.load(stream);
    load();
  }

  public LedgerConfig(Properties properties) {
    props = new Properties();
    props.putAll(properties);
    load();
  }

  public DatabaseConfig getDatabaseConfig() {
    return new DatabaseConfig(props);
  }

  public String getProductName() {
    return PRODUCT_NAME;
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public AuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  @Nullable
  public String getHmacCipherKey() {
    return hmacCipherKey;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public int getPrivilegedPort() {
    return privilegedPort;
  }

  @Override
  public int getAdminPort() {
    return adminPort;
  }

  @Override
  public int getPrometheusExporterPort() {
    return prometheusExporterPort;
  }

  @Override
  public boolean isServerTlsEnabled() {
    return isServerTlsEnabled;
  }

  @Override
  public String getServerTlsCertChainPath() {
    return serverTlsCertChainPath;
  }

  @Override
  public String getServerTlsPrivateKeyPath() {
    return serverTlsPrivateKeyPath;
  }

  public boolean isProofEnabled() {
    return isProofEnabled;
  }

  public String getProofPrivateKey() {
    return proofPrivateKey;
  }

  public boolean isFunctionEnabled() {
    return isFunctionEnabled;
  }

  public boolean isAuditorEnabled() {
    return isAuditorEnabled;
  }

  @Nullable
  @Override
  public String getServersAuthenticationHmacSecretKey() {
    return serversAuthHmacSecretKey;
  }

  @Deprecated
  @Nullable
  public String getAuditorCertHolderId() {
    return auditorCertHolderId;
  }

  @Deprecated
  public int getAuditorCertVersion() {
    return auditorCertVersion;
  }

  public Set<String> getExecutableContractNames() {
    return ImmutableSet.copyOf(executableContractNames);
  }

  public boolean isDirectAssetAccessEnabled() {
    return isDirectAssetAccessEnabled;
  }

  public boolean isTxStateManagementEnabled() {
    return isTxStateManagementEnabled;
  }

  private void load() {
    name = ConfigUtils.getString(props, NAME, DEFAULT_NAME);
    namespace = ConfigUtils.getString(props, NAMESPACE, DEFAULT_NAMESPACE);
    authenticationMethod =
        AuthenticationMethod.get(
            Objects.requireNonNull(
                    ConfigUtils.getString(
                        props, AUTHENTICATION_METHOD, DEFAULT_AUTHENTICATION_METHOD.getMethod()))
                .toLowerCase());
    if (authenticationMethod == AuthenticationMethod.HMAC) {
      hmacCipherKey = ConfigUtils.getString(props, AUTHENTICATION_HMAC_CIPHER_KEY, null);
      if (hmacCipherKey == null) {
        throw new IllegalArgumentException(
            AUTHENTICATION_HMAC_CIPHER_KEY + " must be set if HMAC authentication is used.");
      }
    }
    port = ConfigUtils.getInt(props, SERVER_PORT, DEFAULT_PORT);
    privilegedPort = ConfigUtils.getInt(props, SERVER_PRIVILEGED_PORT, DEFAULT_PRIVILEGED_PORT);
    adminPort = ConfigUtils.getInt(props, SERVER_ADMIN_PORT, DEFAULT_ADMIN_PORT);
    prometheusExporterPort =
        ConfigUtils.getInt(
            props, SERVER_PROMETHEUS_EXPORTER_PORT, DEFAULT_PROMETHEUS_EXPORTER_PORT);
    isServerTlsEnabled = ConfigUtils.getBoolean(props, SERVER_TLS_ENABLED, DEFAULT_TLS_ENABLED);
    serverTlsCertChainPath = ConfigUtils.getString(props, SERVER_TLS_CERT_CHAIN_PATH, null);
    serverTlsPrivateKeyPath = ConfigUtils.getString(props, SERVER_TLS_PRIVATE_KEY_PATH, null);
    isProofEnabled = ConfigUtils.getBoolean(props, PROOF_ENABLED, DEFAULT_PROOF_ENABLED);
    if (isProofEnabled) {
      proofPrivateKey = ConfigUtils.getString(props, PROOF_PRIVATE_KEY_PEM, null);
      if (proofPrivateKey == null) {
        proofPrivateKey = ConfigUtils.getStringFromFilePath(props, PROOF_PRIVATE_KEY_PATH, null);
      }
    }
    isFunctionEnabled = ConfigUtils.getBoolean(props, FUNCTION_ENABLED, DEFAULT_FUNCTION_ENABLED);
    isAuditorEnabled = ConfigUtils.getBoolean(props, AUDITOR_ENABLED, DEFAULT_AUDITOR_ENABLED);
    if (isAuditorEnabled) {
      if (!isProofEnabled) {
        throw new IllegalArgumentException(
            PROOF_ENABLED + " must be set to true if auditor is enabled.");
      }
      serversAuthHmacSecretKey =
          ConfigUtils.getString(props, SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, null);
      if (serversAuthHmacSecretKey == null && proofPrivateKey == null) {
        throw new IllegalArgumentException(
            String.format(
                "Authentication between Ledger and Auditor is not correctly configured."
                    + "Set %s or set a private key with %s or %s.",
                SERVERS_AUTHENTICATION_HMAC_SECRET_KEY,
                PROOF_PRIVATE_KEY_PATH,
                PROOF_PRIVATE_KEY_PEM));
      }
      if (authenticationMethod == AuthenticationMethod.HMAC && serversAuthHmacSecretKey == null) {
        throw new IllegalArgumentException(
            String.format(
                "Authentication between Ledger and Auditor is not correctly configured."
                    + "Set %s if you use HMAC authentication with Auditor enabled.",
                SERVERS_AUTHENTICATION_HMAC_SECRET_KEY));
      }
    } else { // Auditor disabled
      if (isProofEnabled) {
        checkArgument(
            proofPrivateKey != null,
            "either "
                + PROOF_PRIVATE_KEY_PEM
                + " or "
                + PROOF_PRIVATE_KEY_PATH
                + " must be set if proof is enabled.");
      }
    }
    auditorCertHolderId =
        ConfigUtils.getString(props, AUDITOR_CERT_HOLDER_ID, DEFAULT_AUDITOR_CERT_HOLDER_ID);
    auditorCertVersion =
        ConfigUtils.getInt(props, AUDITOR_CERT_VERSION, DEFAULT_AUDITOR_CERT_VERSION);
    String executableContractsFile = ConfigUtils.getString(props, EXECUTABLE_CONTRACTS, null);
    executableContractNames =
        executableContractsFile == null
            ? Collections.emptySet()
            : loadContractNames(executableContractsFile);
    isDirectAssetAccessEnabled =
        ConfigUtils.getBoolean(
            props, DIRECT_ASSET_ACCESS_ENABLED, DEFAULT_DIRECT_ASSET_ACCESS_ENABLED);
    isTxStateManagementEnabled =
        ConfigUtils.getBoolean(
            props, TX_STATE_MANAGEMENT_ENABLED, DEFAULT_TX_STATE_MANAGEMENT_ENABLED);
    validateTransactionManager();

    LOGGER.info(name + " is configured with " + this + " (credential information is omitted)");
  }

  private Set<String> loadContractNames(String file) {
    return new Toml()
        .read(new File(file)).getTables("contracts").stream()
            .map(c -> c.getString("contract-binary-name"))
            .collect(Collectors.toSet());
  }

  private void validateTransactionManager() {
    DatabaseConfig databaseConfig = new DatabaseConfig(props);
    String transactionManager = databaseConfig.getTransactionManager().toLowerCase(Locale.ROOT);

    if (transactionManager.equals("jdbc")) {
      if (isAuditorEnabled && !isTxStateManagementEnabled) {
        throw new IllegalArgumentException(
            TX_STATE_MANAGEMENT_ENABLED
                + " must be set to true when using JdbcTransactionManager in the Auditor mode.");
      }
    } else if (transactionManager.equals("consensus-commit")) {
      if (isTxStateManagementEnabled) {
        throw new IllegalArgumentException(
            TX_STATE_MANAGEMENT_ENABLED
                + " must be disabled when using ConsensusCommitManager for performance reasons.");
      }
    }
  }

  @Override
  public String toString() {
    // Credential information (e.g., private keys) is omitted for security reasons.
    return MoreObjects.toStringHelper(this)
        .add(NAME, getName())
        .add(AUTHENTICATION_METHOD, getAuthenticationMethod())
        .add(SERVER_PORT, getPort())
        .add(SERVER_PRIVILEGED_PORT, getPrivilegedPort())
        .add(SERVER_ADMIN_PORT, getAdminPort())
        .add(SERVER_PROMETHEUS_EXPORTER_PORT, getPrometheusExporterPort())
        .add(SERVER_TLS_ENABLED, isServerTlsEnabled())
        .add(SERVER_TLS_CERT_CHAIN_PATH, getServerTlsCertChainPath())
        .add(PROOF_ENABLED, isProofEnabled())
        .add(FUNCTION_ENABLED, isFunctionEnabled())
        .add(AUDITOR_ENABLED, isAuditorEnabled())
        .add(AUDITOR_CERT_HOLDER_ID, getAuditorCertHolderId())
        .add(AUDITOR_CERT_VERSION, getAuditorCertVersion())
        .add(EXECUTABLE_CONTRACTS, getExecutableContractNames())
        .add(DIRECT_ASSET_ACCESS_ENABLED, isDirectAssetAccessEnabled())
        .add(TX_STATE_MANAGEMENT_ENABLED, isTxStateManagementEnabled())
        .toString();
  }
}
