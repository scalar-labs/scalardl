package com.scalar.dl.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.scalar.db.config.DatabaseConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LedgerConfigTest {
  private static final String SOME_NUMBER = "9999";
  private static final String SOME_PATH = "/dev/null";
  private static final String SOME_PEM = "-- PEM --";
  private static final String SOME_KEY = "key";
  private static final String SOME_NAME = "My Ledger";
  private static final String SOME_SECRET_KEY = "secret-key";
  private static final String SOME_CIPHER_KEY = "cipher-key";
  private static final String SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE = "2048";
  private static final String SOME_GRPC_MAX_INBOUND_METADATA_SIZE = "1024";
  private Properties props;

  @BeforeEach
  public void setUp() {
    props = new Properties();
    props.setProperty(DatabaseConfig.CONTACT_POINTS, "localhost");
  }

  @Test
  public void constructor_OnlyRequiredPropertiesGiven_ShouldCreateInstance() {
    // Arrange

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getProductName()).isEqualTo(LedgerConfig.PRODUCT_NAME);
    assertThat(config.getServiceName()).isEqualTo(LedgerConfig.SERVICE_NAME);
    assertThat(config.getName()).isEqualTo(LedgerConfig.DEFAULT_NAME);
    assertThat(config.getNamespace()).isEqualTo(LedgerConfig.DEFAULT_NAMESPACE);
    assertThat(config.getAuthenticationMethod())
        .isEqualTo(LedgerConfig.DEFAULT_AUTHENTICATION_METHOD);
    assertThat(config.getPort()).isEqualTo(LedgerConfig.DEFAULT_PORT);
    assertThat(config.getPrivilegedPort()).isEqualTo(LedgerConfig.DEFAULT_PRIVILEGED_PORT);
    assertThat(config.getAdminPort()).isEqualTo(LedgerConfig.DEFAULT_ADMIN_PORT);
    assertThat(config.getPrometheusExporterPort())
        .isEqualTo(LedgerConfig.DEFAULT_PROMETHEUS_EXPORTER_PORT);
    assertThat(config.getDecommissioningDurationSecs())
        .isEqualTo(LedgerConfig.DEFAULT_DECOMMISSIONING_DURATION_SECS);
    assertThat(config.isServerTlsEnabled()).isEqualTo(LedgerConfig.DEFAULT_TLS_ENABLED);
    assertThat(config.getGrpcServerConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(config.getGrpcServerConfig().getMaxInboundMetadataSize()).isEqualTo(0);
    assertThat(config.isProofEnabled()).isEqualTo(LedgerConfig.DEFAULT_PROOF_ENABLED);
    assertThat(config.isFunctionEnabled()).isEqualTo(LedgerConfig.DEFAULT_FUNCTION_ENABLED);
    assertThat(config.isAuditorEnabled()).isEqualTo(LedgerConfig.DEFAULT_AUDITOR_ENABLED);
    assertThat(config.getAuditorCertHolderId())
        .isEqualTo(LedgerConfig.DEFAULT_AUDITOR_CERT_HOLDER_ID);
    assertThat(config.getAuditorCertVersion()).isEqualTo(LedgerConfig.DEFAULT_AUDITOR_CERT_VERSION);
    assertThat(config.isDirectAssetAccessEnabled())
        .isEqualTo(LedgerConfig.DEFAULT_DIRECT_ASSET_ACCESS_ENABLED);
    assertThat(config.isTxStateManagementEnabled())
        .isEqualTo(LedgerConfig.DEFAULT_TX_STATE_MANAGEMENT_ENABLED);
  }

  @Test
  public void getPort_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_PORT, SOME_NUMBER);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getPort()).isEqualTo(Integer.parseInt(SOME_NUMBER));
  }

  @Test
  public void getPrivilegedPort_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_PRIVILEGED_PORT, SOME_NUMBER);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getPrivilegedPort()).isEqualTo(Integer.parseInt(SOME_NUMBER));
  }

  @Test
  public void getAdminPort_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_ADMIN_PORT, SOME_NUMBER);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getAdminPort()).isEqualTo(Integer.parseInt(SOME_NUMBER));
  }

  @Test
  public void getDecommissioningDurationSecs_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_DECOMMISSIONING_DURATION_SECS, "60");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getDecommissioningDurationSecs()).isEqualTo(60);
  }

  @Test
  public void isTlsEnabled_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_TLS_ENABLED, "true");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isServerTlsEnabled()).isTrue();
  }

  @Test
  public void getTlsCertChainPath_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_TLS_ENABLED, "true");
    props.setProperty(LedgerConfig.SERVER_TLS_CERT_CHAIN_PATH, SOME_PATH);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getServerTlsCertChainPath()).isEqualTo(SOME_PATH);
  }

  @Test
  public void getTlsPrivateKeyPath_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.SERVER_TLS_ENABLED, "true");
    props.setProperty(LedgerConfig.SERVER_TLS_PRIVATE_KEY_PATH, SOME_PATH);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getServerTlsPrivateKeyPath()).isEqualTo(SOME_PATH);
  }

  @Test
  public void getMaxInboundMessageSize_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(
        LedgerConfig.SERVER_GRPC_MAX_INBOUND_MESSAGE_SIZE, SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getGrpcServerConfig().getMaxInboundMessageSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE));
  }

  @Test
  public void getMaxInboundMetadataSize_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(
        LedgerConfig.SERVER_GRPC_MAX_INBOUND_METADATA_SIZE, SOME_GRPC_MAX_INBOUND_METADATA_SIZE);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getGrpcServerConfig().getMaxInboundMetadataSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_METADATA_SIZE));
  }

  @Test
  public void getName_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.NAME, SOME_NAME);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getName()).isEqualTo(SOME_NAME);
  }

  @Test
  public void isProofEnabled_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isProofEnabled()).isTrue();
  }

  @Test
  public void getProofPrivateKeyPem_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getProofPrivateKey()).isEqualTo(SOME_PEM);
  }

  @Test
  public void isFunctionEnabled_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.FUNCTION_ENABLED, "true");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isFunctionEnabled()).isTrue();
  }

  @Test
  public void getExecutableContractNames_PropertiesGiven_ShouldReturnSpecified()
      throws IOException {
    // Arrange
    List<String> lines =
        Arrays.asList(
            "[[contracts]]",
            "contract-binary-name = \"com.org1.contract.StateUpdater\"",
            "[[contracts]]",
            "contract-binary-name = \"com.org1.contract.StateReader\"");
    Files.write(Paths.get("/tmp/contracts.toml"), lines, StandardCharsets.UTF_8);
    props.setProperty(LedgerConfig.EXECUTABLE_CONTRACTS, "/tmp/contracts.toml");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getExecutableContractNames())
        .containsOnly("com.org1.contract.StateUpdater", "com.org1.contract.StateReader");
  }

  @Test
  public void isAuditorEnabled_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isAuditorEnabled()).isTrue();
  }

  @Test
  public void getAuditorCertHolderId_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_CERT_HOLDER_ID, SOME_NAME);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getAuditorCertHolderId()).isEqualTo(SOME_NAME);
  }

  @Test
  public void getAuditorCertVersion_PropertiesGiven_ShouldReturnSpecified() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_CERT_VERSION, SOME_NUMBER);

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.getAuditorCertVersion()).isEqualTo(Integer.parseInt(SOME_NUMBER));
  }

  @Test
  public void constructor_ProofEnabledAndPrivateKeySpecified_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      constructor_ProofEnabledAndPrivateKeyUnspecified_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_AuditorEnabledAndProofEnabled_ShouldConstructProperly() {
    // Arrange
    props.setProperty(
        LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void constructor_AuditorEnabledButProofNotEnabled_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_AuditorDisabledButProofEnabled_ShouldConstructProperly() {
    // Arrange
    props.setProperty(
        LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "false");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void constructor_TxStateManagementEnabled_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "true");
    props.setProperty(DatabaseConfig.STORAGE, "jdbc");
    props.setProperty(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isTxStateManagementEnabled()).isTrue();
  }

  @Test
  public void
      constructor_TxStateManagementDisabledAndConsensusCommitSpecified_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "false");
    props.setProperty(DatabaseConfig.TRANSACTION_MANAGER, "consensus-commit");

    // Act
    LedgerConfig config = new LedgerConfig(props);

    // Assert
    assertThat(config.isTxStateManagementEnabled()).isFalse();
  }

  @Test
  public void
      constructor_TxStateManagementDisabledButJdbcTransactionManagerSpecifiedAndAuditorEnabled_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "false");
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(DatabaseConfig.STORAGE, "jdbc");
    props.setProperty(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_TxStateManagementDisabledButJdbcTransactionManagerSpecified_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "false");
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "false"); // false by default
    props.setProperty(DatabaseConfig.STORAGE, "jdbc");
    props.setProperty(DatabaseConfig.TRANSACTION_MANAGER, "jdbc");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      constructor_ConsensusCommitManagerSpecifiedButTxStateManagementEnabled_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(DatabaseConfig.TRANSACTION_MANAGER, "consensus-commit");
    props.setProperty(LedgerConfig.TX_STATE_MANAGEMENT_ENABLED, "true");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_AuditorAndProofEnabledAndPrivateKeyGiven_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      constructor_AuditorAndProofEnabledButNeitherPrivateKeyNorSecretKeyGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_AuditorEnabledButProofDisabled_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "false");
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_ProofAndAuditorDisabledAndHmacConfiguredWithCipherKeyGiven_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      constructor_ProofAndAuditorEnabledAndHmacConfiguredWithCipherKeyAndSecretKeyGiven_ShouldConstructProperly() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    props.setProperty(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).doesNotThrowAnyException();
  }

  @Test
  public void
      constructor_AuditorEnabledButProofDisabledWithHmacConfiguration_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "false");
    props.setProperty(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    props.setProperty(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_HmacEnabledButCipherKeyNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "false");
    props.setProperty(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_AuditorAndProofEnabledInDigitalSignatureConfigurationButPrivateKeyNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(
        LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.setProperty(LedgerConfig.SERVERS_AUTHENTICATION_HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_AuditorAndProofEnabledInHmacConfigurationButSecretKeyNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    props.setProperty(LedgerConfig.AUDITOR_ENABLED, "true");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "true");
    props.setProperty(LedgerConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.setProperty(LedgerConfig.AUTHENTICATION_HMAC_CIPHER_KEY, SOME_CIPHER_KEY);
    props.setProperty(LedgerConfig.PROOF_PRIVATE_KEY_PEM, SOME_PEM);

    // Act
    Throwable thrown = catchThrowable(() -> new LedgerConfig(props));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }
}
