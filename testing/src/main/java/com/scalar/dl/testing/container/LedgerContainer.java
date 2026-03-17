package com.scalar.dl.testing.container;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.config.StorageConfig;
import com.scalar.dl.testing.config.TransactionMode;
import com.scalar.dl.testing.util.ScalarDlBuildInfo;
import com.scalar.dl.testing.util.TestCertificates;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers wrapper for ScalarDL Ledger.
 *
 * <p>Configuration is provided via {@link #withStorageConfig} and {@link
 * #withAuthenticationMethod}. The configuration is written to a properties file and copied to the
 * container.
 */
@SuppressFBWarnings(
    value = "EQ_DOESNT_OVERRIDE_EQUALS",
    justification = "Container identity is based on GenericContainer's implementation")
public class LedgerContainer extends GenericContainer<LedgerContainer> {

  public static final int PORT = 50051;
  public static final int PRIVILEGED_PORT = 50052;
  public static final String DEFAULT_IMAGE =
      "ghcr.io/scalar-labs/scalardl-ledger:" + ScalarDlBuildInfo.getVersion();

  private static final String PROPERTIES_PATH = "/scalar/ledger/ledger.properties";

  private StorageConfig storageConfig;
  private AuthenticationMethod authenticationMethod;
  private boolean proofEnabled = false;
  private boolean auditorEnabled = false;
  private String serversAuthenticationHmacSecretKey;
  private String proofPrivateKey;

  public LedgerContainer() {
    this(DEFAULT_IMAGE);
  }

  public LedgerContainer(String image) {
    super(DockerImageName.parse(image));
    withAccessToHost(true); // Enable access to host services
    withExposedPorts(PORT, PRIVILEGED_PORT);
    waitingFor(new GrpcHealthWaitStrategy(PORT));
  }

  /**
   * Configures the storage settings for this Ledger container.
   *
   * @param storageConfig The storage configuration
   * @return This container instance
   */
  public LedgerContainer withStorageConfig(StorageConfig storageConfig) {
    this.storageConfig = storageConfig;
    return this;
  }

  /**
   * Configures authentication method.
   *
   * @param authenticationMethod The authentication method
   * @return This container instance
   */
  public LedgerContainer withAuthenticationMethod(AuthenticationMethod authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
    return this;
  }

  /** Enables proof functionality. */
  public LedgerContainer withProofEnabled() {
    this.proofEnabled = true;
    return this;
  }

  /** Enables Auditor integration. */
  public LedgerContainer withAuditorEnabled() {
    this.auditorEnabled = true;
    return this;
  }

  /** Configures server authentication for Auditor communication. */
  public LedgerContainer withServersAuthenticationHmacSecretKey(String secretKey) {
    this.serversAuthenticationHmacSecretKey = secretKey;
    return this;
  }

  /**
   * Sets the private key for proof signing (required when Auditor is enabled with digital signature
   * authentication).
   *
   * @param privateKeyPem The private key in PEM format
   * @return This container instance
   */
  public LedgerContainer withProofPrivateKey(String privateKeyPem) {
    this.proofPrivateKey = privateKeyPem;
    return this;
  }

  /** Called before the container starts. Builds and copies the properties file to the container. */
  @Override
  protected void configure() {
    super.configure();

    Properties props = createLedgerProperties();
    try {
      StringWriter writer = new StringWriter();
      props.store(writer, null);
      withCopyToContainer(
          Transferable.of(writer.toString().getBytes(StandardCharsets.UTF_8)), PROPERTIES_PATH);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to serialize properties", e);
    }

    // Override the default CMD to bypass dockerize and use the test-provided properties directly.
    withCommand("./bin/scalar-ledger", "--config=" + PROPERTIES_PATH);
  }

  /**
   * Creates the Ledger properties combining all configured settings.
   *
   * @return The combined properties
   */
  private Properties createLedgerProperties() {
    Properties props = new Properties();

    // ScalarDB settings
    if (storageConfig != null) {
      props.putAll(storageConfig.getPropertiesForContainer());
    }

    // Authentication settings
    if (authenticationMethod == AuthenticationMethod.DIGITAL_SIGNATURE) {
      props.setProperty("scalar.dl.ledger.authentication.method", "digital-signature");
    } else if (authenticationMethod == AuthenticationMethod.HMAC) {
      props.setProperty("scalar.dl.ledger.authentication.method", "hmac");
      props.setProperty(
          "scalar.dl.ledger.authentication.hmac.cipher_key", TestCertificates.CIPHER_KEY);
    }

    // Ledger settings
    props.setProperty("scalar.dl.ledger.server.prometheus_exporter_port", "-1");
    props.setProperty("scalar.dl.ledger.function.enabled", String.valueOf(true));
    props.setProperty("scalar.dl.ledger.proof.enabled", String.valueOf(proofEnabled));
    props.setProperty("scalar.dl.ledger.auditor.enabled", String.valueOf(auditorEnabled));

    // Transaction mode settings
    if (storageConfig != null && storageConfig.getTransactionMode() == TransactionMode.JDBC) {
      props.setProperty("scalar.dl.ledger.tx_state_management.enabled", "true");
    }

    if (serversAuthenticationHmacSecretKey != null) {
      props.setProperty(
          "scalar.dl.ledger.servers.authentication.hmac.secret_key",
          serversAuthenticationHmacSecretKey);
    }

    if (proofPrivateKey != null) {
      props.setProperty("scalar.dl.ledger.proof.private_key_pem", proofPrivateKey);
    }

    return props;
  }

  /** Returns the mapped port for the main Ledger service. */
  public int getPort() {
    return getMappedPort(PORT);
  }

  /** Returns the mapped port for the privileged Ledger service. */
  public int getPrivilegedPort() {
    return getMappedPort(PRIVILEGED_PORT);
  }
}
