package com.scalar.dl.client.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.TargetConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClientConfigTest {
  private static final String SOME_ENTITY_ID = "some_entity_id";
  private static final String SOME_SECRET_KEY_VERSION = "3";
  private static final String SOME_SECRET_KEY = "some_secret_key";
  private static final String SOME_CERT_HOLDER_ID = "some_cert_holder_id";
  private static final String SOME_CERT_VERSION = "2";
  private static final String SOME_CERT_PATH = "cert_path";
  private static final String SOME_CERT_PEM = "some_cert_string";
  private static final String SOME_PRIVATE_KEY_PATH = "private_key_path";
  private static final String SOME_PRIVATE_KEY_PEM = "some_private_key_string";
  private static final String SOME_CLIENT_MODE = "CLIENT";
  private static final String SOME_INTERMEDIARY_MODE = "INTERMEDIARY";
  private static final String SOME_AUTHENTICATION_METHOD_FOR_CLIENT = "HMAC";
  private static final String SOME_AUTHENTICATION_METHOD_FOR_INTERMEDIARY = "PASS-THROUGH";
  private static final String SOME_HOST = "192.168.0.100";
  private static final String SOME_PORT = "60051";
  private static final String SOME_PRIVILEGED_PORT = "60052";
  private static final String SOME_TLS_ENABLED = "true";
  private static final String SOME_TLS_CA_ROOT_CERT_PATH = "tls_ca_root_cert_path";
  private static final String SOME_TLS_CA_ROOT_CERT_PEM = "tls.ca_root_cert_string";
  private static final String SOME_TLS_OVERRIDE_AUTHORITY = "tls_override_authority";
  private static final String SOME_AUTHORIZATION_CREDENTIAL = "authorization.credential";
  private static final String SOME_GRPC_DEADLINE_DEADLINE_DURATION_MILLIS = "30000";
  private static final String SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE = "2048";
  private static final String SOME_GRPC_MAX_INBOUND_METADATA_SIZE = "1024";
  private static final String SOME_AUDITOR_ENABLED = "true";
  private static final String SOME_AUDITOR_HOST = "192.168.1.100";
  private static final String SOME_AUDITOR_PORT = "70051";
  private static final String SOME_AUDITOR_PRIVILEGED_PORT = "70052";
  private static final String SOME_AUDITOR_TLS_ENABLED = "true";
  private static final String SOME_AUDITOR_TLS_CA_ROOT_CERT_PATH = "auditor_tls_ca_root_cert_path";
  private static final String SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM = "tls.ca_root_cert_string.auditor";
  private static final String SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY =
      "auditor_tls.override_authority";
  private static final String SOME_AUDITOR_AUTHORIZATION_CREDENTIAL =
      "authorization.credential.auditor";
  private static final String SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID = "validate";

  @TempDir Path folder;

  private void writeToFile(File file, String content) throws IOException {
    BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    writer.write(content);
    writer.close();
  }

  @Test
  public void constructor_AllPropertiesWithPemGiven_ShouldCreateInstance() throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props.put(ClientConfig.MODE, SOME_CLIENT_MODE);
    props.put(ClientConfig.SERVER_HOST, SOME_HOST);
    props.put(ClientConfig.SERVER_PORT, SOME_PORT);
    props.put(ClientConfig.SERVER_PRIVILEGED_PORT, SOME_PRIVILEGED_PORT);
    props.put(ClientConfig.TLS_ENABLED, SOME_TLS_ENABLED);
    props.put(ClientConfig.TLS_CA_ROOT_CERT_PEM, SOME_TLS_CA_ROOT_CERT_PEM);
    props.put(ClientConfig.TLS_OVERRIDE_AUTHORITY, SOME_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUTHORIZATION_CREDENTIAL, SOME_AUTHORIZATION_CREDENTIAL);
    props.put(
        ClientConfig.GRPC_DEADLINE_DURATION_MILLIS, SOME_GRPC_DEADLINE_DEADLINE_DURATION_MILLIS);
    props.put(ClientConfig.GRPC_MAX_INBOUND_MESSAGE_SIZE, SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE);
    props.put(ClientConfig.GRPC_MAX_INBOUND_METADATA_SIZE, SOME_GRPC_MAX_INBOUND_METADATA_SIZE);
    props.put(ClientConfig.AUDITOR_ENABLED, SOME_AUDITOR_ENABLED);
    props.put(ClientConfig.AUDITOR_HOST, SOME_AUDITOR_HOST);
    props.put(ClientConfig.AUDITOR_PORT, SOME_AUDITOR_PORT);
    props.put(ClientConfig.AUDITOR_PRIVILEGED_PORT, SOME_AUDITOR_PRIVILEGED_PORT);
    props.put(ClientConfig.AUDITOR_TLS_ENABLED, SOME_AUDITOR_TLS_ENABLED);
    props.put(ClientConfig.AUDITOR_TLS_CA_ROOT_CERT_PEM, SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);
    props.put(ClientConfig.AUDITOR_TLS_OVERRIDE_AUTHORITY, SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUDITOR_AUTHORIZATION_CREDENTIAL, SOME_AUDITOR_AUTHORIZATION_CREDENTIAL);
    props.put(
        ClientConfig.AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID,
        SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        config.getDigitalSignatureIdentityConfig();
    TargetConfig ledgerTargetConfig = config.getLedgerTargetConfig();
    TargetConfig auditorTargetConfig = config.getAuditorTargetConfig();
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_CERT_HOLDER_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.getClientMode()).isEqualTo(ClientMode.CLIENT);
    assertThat(ledgerTargetConfig.getTargetHost()).isEqualTo(SOME_HOST);
    assertThat(ledgerTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_PORT));
    assertThat(ledgerTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_PRIVILEGED_PORT));
    assertThat(ledgerTargetConfig.isTargetTlsEnabled()).isEqualTo(true);
    assertThat(ledgerTargetConfig.getTargetTlsCaRootCert()).isEqualTo(SOME_TLS_CA_ROOT_CERT_PEM);
    assertThat(ledgerTargetConfig.getTargetTlsOverrideAuthority())
        .isEqualTo(SOME_TLS_OVERRIDE_AUTHORITY);
    assertThat(ledgerTargetConfig.getTargetAuthorizationCredential())
        .isEqualTo(SOME_AUTHORIZATION_CREDENTIAL);
    assertThat(ledgerTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(Long.parseLong(SOME_GRPC_DEADLINE_DEADLINE_DURATION_MILLIS));
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE));
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_METADATA_SIZE));
    assertThat(config.isAuditorEnabled()).isEqualTo(true);
    assertThat(auditorTargetConfig).isNotNull();
    assertThat(auditorTargetConfig.getTargetHost()).isEqualTo(SOME_AUDITOR_HOST);
    assertThat(auditorTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_AUDITOR_PORT));
    assertThat(auditorTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_AUDITOR_PRIVILEGED_PORT));
    assertThat(auditorTargetConfig.getTargetTlsCaRootCert())
        .isEqualTo(SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);
    assertThat(config.getAuditorLinearizableValidationContractId())
        .isEqualTo(SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID + "-" + ValidateLedger.VERSION);
  }

  @Test
  public void constructor_AllPropertiesWithPathGiven_ShouldCreateInstance() throws IOException {
    // Arrange
    File certPath = folder.resolve(SOME_CERT_PATH).toFile();
    writeToFile(certPath.getCanonicalFile(), SOME_CERT_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_PRIVATE_KEY_PEM);
    File tlsCaRootCertPath = folder.resolve(SOME_TLS_CA_ROOT_CERT_PATH).toFile();
    writeToFile(tlsCaRootCertPath.getCanonicalFile(), SOME_TLS_CA_ROOT_CERT_PEM);
    File auditorTlsCaRootCertPath = folder.resolve(SOME_AUDITOR_TLS_CA_ROOT_CERT_PATH).toFile();
    writeToFile(auditorTlsCaRootCertPath.getCanonicalFile(), SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);

    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.CERT_PATH, certPath.getCanonicalPath());
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PATH, privateKeyPath.getCanonicalPath());
    props.put(ClientConfig.MODE, SOME_CLIENT_MODE);
    props.put(ClientConfig.SERVER_HOST, SOME_HOST);
    props.put(ClientConfig.SERVER_PORT, SOME_PORT);
    props.put(ClientConfig.SERVER_PRIVILEGED_PORT, SOME_PRIVILEGED_PORT);
    props.put(ClientConfig.TLS_ENABLED, SOME_TLS_ENABLED);
    props.put(ClientConfig.TLS_CA_ROOT_CERT_PATH, tlsCaRootCertPath.getCanonicalPath());
    props.put(ClientConfig.TLS_OVERRIDE_AUTHORITY, SOME_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUTHORIZATION_CREDENTIAL, SOME_AUTHORIZATION_CREDENTIAL);
    props.put(
        ClientConfig.GRPC_DEADLINE_DURATION_MILLIS, SOME_GRPC_DEADLINE_DEADLINE_DURATION_MILLIS);
    props.put(ClientConfig.GRPC_MAX_INBOUND_MESSAGE_SIZE, SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE);
    props.put(ClientConfig.GRPC_MAX_INBOUND_METADATA_SIZE, SOME_GRPC_MAX_INBOUND_METADATA_SIZE);
    props.put(ClientConfig.AUDITOR_ENABLED, SOME_AUDITOR_ENABLED);
    props.put(ClientConfig.AUDITOR_HOST, SOME_AUDITOR_HOST);
    props.put(ClientConfig.AUDITOR_PORT, SOME_AUDITOR_PORT);
    props.put(ClientConfig.AUDITOR_PRIVILEGED_PORT, SOME_AUDITOR_PRIVILEGED_PORT);
    props.put(ClientConfig.AUDITOR_TLS_ENABLED, SOME_AUDITOR_TLS_ENABLED);
    props.put(
        ClientConfig.AUDITOR_TLS_CA_ROOT_CERT_PATH, auditorTlsCaRootCertPath.getCanonicalPath());
    props.put(ClientConfig.AUDITOR_TLS_OVERRIDE_AUTHORITY, SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUDITOR_AUTHORIZATION_CREDENTIAL, SOME_AUDITOR_AUTHORIZATION_CREDENTIAL);
    props.put(
        ClientConfig.AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID,
        SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        config.getDigitalSignatureIdentityConfig();
    TargetConfig ledgerTargetConfig = config.getLedgerTargetConfig();
    TargetConfig auditorTargetConfig = config.getAuditorTargetConfig();
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_CERT_HOLDER_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.getClientMode()).isEqualTo(ClientMode.CLIENT);
    assertThat(ledgerTargetConfig.getTargetHost()).isEqualTo(SOME_HOST);
    assertThat(ledgerTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_PORT));
    assertThat(ledgerTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_PRIVILEGED_PORT));
    assertThat(ledgerTargetConfig.isTargetTlsEnabled()).isEqualTo(true);
    assertThat(ledgerTargetConfig.getTargetTlsCaRootCert()).isEqualTo(SOME_TLS_CA_ROOT_CERT_PEM);
    assertThat(ledgerTargetConfig.getTargetTlsOverrideAuthority())
        .isEqualTo(SOME_TLS_OVERRIDE_AUTHORITY);
    assertThat(ledgerTargetConfig.getTargetAuthorizationCredential())
        .isEqualTo(SOME_AUTHORIZATION_CREDENTIAL);
    assertThat(ledgerTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(Long.parseLong(SOME_GRPC_DEADLINE_DEADLINE_DURATION_MILLIS));
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_MESSAGE_SIZE));
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize())
        .isEqualTo(Integer.parseInt(SOME_GRPC_MAX_INBOUND_METADATA_SIZE));
    assertThat(config.isAuditorEnabled()).isEqualTo(true);
    assertThat(auditorTargetConfig).isNotNull();
    assertThat(auditorTargetConfig.getTargetHost()).isEqualTo(SOME_AUDITOR_HOST);
    assertThat(auditorTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_AUDITOR_PORT));
    assertThat(auditorTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_AUDITOR_PRIVILEGED_PORT));
    assertThat(auditorTargetConfig.getTargetTlsCaRootCert())
        .isEqualTo(SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);
    assertThat(auditorTargetConfig.getTargetTlsOverrideAuthority())
        .isEqualTo(SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY);
    assertThat(config.getAuditorLinearizableValidationContractId())
        .isEqualTo(SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID + "-" + ValidateLedger.VERSION);
  }

  @Test
  public void constructor_OnlyRequiredPropertiesGiven_ShouldCreateInstance() throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    TargetConfig ledgerTargetConfig = config.getLedgerTargetConfig();
    assertThat(ledgerTargetConfig).isNotNull();
    TargetConfig auditorTargetConfig = config.getAuditorTargetConfig();
    assertThat(auditorTargetConfig).isNull();
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        config.getDigitalSignatureIdentityConfig();
    assertThat(digitalSignatureIdentityConfig).isNotNull();
    assertThat(ledgerTargetConfig.getTargetHost()).isEqualTo(ClientConfig.DEFAULT_SERVER_HOST);
    assertThat(ledgerTargetConfig.getTargetPort()).isEqualTo(ClientConfig.DEFAULT_SERVER_PORT);
    assertThat(ledgerTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(ClientConfig.DEFAULT_SERVER_PRIVILEGED_PORT);
    assertThat(ledgerTargetConfig.isTargetTlsEnabled()).isEqualTo(ClientConfig.DEFAULT_TLS_ENABLED);
    assertThat(ledgerTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(ClientConfig.DEFAULT_DEADLINE_DURATION_MILLIS);
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize()).isEqualTo(0);
    assertThat(config.getClientMode()).isEqualTo(ClientConfig.DEFAULT_CLIENT_MODE);
    assertThat(config.getAuthenticationMethod())
        .isEqualTo(ClientConfig.DEFAULT_AUTHENTICATION_METHOD);
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_CERT_HOLDER_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(ClientConfig.DEFAULT_CERT_VERSION);
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.isAuditorEnabled()).isEqualTo(ClientConfig.DEFAULT_AUDITOR_ENABLED);
  }

  @Test
  public void constructor_OnlyRequiredPropertiesInHmacAuthenticationGiven_ShouldCreateInstance()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    HmacIdentityConfig hmacIdentityConfig = config.getHmacIdentityConfig();
    assertThat(hmacIdentityConfig).isNotNull();
    assertThat(hmacIdentityConfig.getSecretKey()).isEqualTo(SOME_SECRET_KEY);
    assertThat(hmacIdentityConfig.getSecretKeyVersion())
        .isEqualTo(ClientConfig.DEFAULT_SECRET_KEY_VERSION);
  }

  @Test
  public void constructor_OnlyRequiredPropertiesInAuditorModeGiven_ShouldCreateInstance()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props.put(ClientConfig.AUDITOR_ENABLED, "true");

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.isAuditorEnabled()).isTrue();
    TargetConfig auditorTargetConfig = config.getAuditorTargetConfig();
    assertThat(auditorTargetConfig).isNotNull();
    assertThat(auditorTargetConfig.getTargetHost()).isEqualTo(ClientConfig.DEFAULT_AUDITOR_HOST);
    assertThat(auditorTargetConfig.getTargetPort()).isEqualTo(ClientConfig.DEFAULT_AUDITOR_PORT);
    assertThat(auditorTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(ClientConfig.DEFAULT_AUDITOR_PRIVILEGED_PORT);
    assertThat(auditorTargetConfig.isTargetTlsEnabled())
        .isEqualTo(ClientConfig.DEFAULT_AUDITOR_TLS_ENABLED);
    assertThat(config.getAuditorLinearizableValidationContractId())
        .isEqualTo(
            ClientConfig.DEFAULT_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID
                + "-"
                + ValidateLedger.VERSION);
  }

  @Test
  public void constructor_OnlyRequiredPropertiesForIntermediaryGiven_ShouldCreateInstance()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.MODE, SOME_INTERMEDIARY_MODE);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    TargetConfig ledgerTargetConfig = config.getLedgerTargetConfig();
    assertThat(ledgerTargetConfig).isNotNull();
    TargetConfig auditorTargetConfig = config.getAuditorTargetConfig();
    assertThat(auditorTargetConfig).isNull();
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        config.getDigitalSignatureIdentityConfig();
    HmacIdentityConfig hmacIdentityConfig = config.getHmacIdentityConfig();
    assertThat(ledgerTargetConfig.getTargetHost()).isEqualTo(ClientConfig.DEFAULT_SERVER_HOST);
    assertThat(ledgerTargetConfig.getTargetPort()).isEqualTo(ClientConfig.DEFAULT_SERVER_PORT);
    assertThat(ledgerTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(ClientConfig.DEFAULT_SERVER_PRIVILEGED_PORT);
    assertThat(ledgerTargetConfig.isTargetTlsEnabled()).isEqualTo(ClientConfig.DEFAULT_TLS_ENABLED);
    assertThat(ledgerTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(ClientConfig.DEFAULT_DEADLINE_DURATION_MILLIS);
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(ledgerTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize()).isEqualTo(0);
    assertThat(config.getClientMode()).isEqualTo(ClientMode.INTERMEDIARY);
    assertThat(config.isAuditorEnabled()).isEqualTo(ClientConfig.DEFAULT_AUDITOR_ENABLED);
    assertThat(config.getAuthenticationMethod()).isEqualTo(AuthenticationMethod.PASS_THROUGH);
    assertThat(digitalSignatureIdentityConfig).isNull();
    assertThat(hmacIdentityConfig).isNull();
  }

  @Test
  public void
      constructor_ClientModeWithWrongAuthenticationMethodGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.MODE, SOME_CLIENT_MODE);
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.HMAC_SECRET_KEY, SOME_SECRET_KEY);
    props.put(ClientConfig.AUTHENTICATION_METHOD, SOME_AUTHENTICATION_METHOD_FOR_INTERMEDIARY);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_IntermediaryModeWithWrongAuthenticationMethodGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.MODE, SOME_INTERMEDIARY_MODE);
    props.put(ClientConfig.AUTHENTICATION_METHOD, SOME_AUTHENTICATION_METHOD_FOR_CLIENT);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_CertHolderIdAndEntityIdMissing_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_OnlyEntityIdAndCertAndPrivateKeyGiven_ShouldCreateConfigProperly()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
  }

  @Test
  public void constructor_OnlyCertHolderIdAndCertAndPrivateKeyGiven_ShouldCreateConfigProperly()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId())
        .isEqualTo(SOME_CERT_HOLDER_ID);
  }

  @Test
  public void
      constructor_EntityIdAndCertHolderIdAndCertAndPrivateKeyGiven_ShouldCreateConfigWithEntityIdProperly()
          throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
  }

  @Test
  public void constructor_HmacAuthEnabled_ShouldCreateConfigProperly() throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.HMAC_SECRET_KEY_VERSION, SOME_SECRET_KEY_VERSION);
    props.put(ClientConfig.HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getHmacIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getHmacIdentityConfig().getSecretKeyVersion())
        .isEqualTo(Integer.parseInt(SOME_SECRET_KEY_VERSION));
    assertThat(config.getHmacIdentityConfig().getSecretKey()).isEqualTo(SOME_SECRET_KEY);
    assertThat(config.getDigitalSignatureIdentityConfig()).isNull();
  }

  @Test
  public void constructor_DigitalSignatureAuthEnabled_ShouldCreateConfigProperly()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(
        ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getDigitalSignatureIdentityConfig().getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(config.getDigitalSignatureIdentityConfig().getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(config.getDigitalSignatureIdentityConfig().getPrivateKey())
        .isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.getHmacIdentityConfig()).isNull();
  }

  @Test
  public void constructor_HmacAuthEnabledButNoHmacInfoGiven_ShouldCreateConfigProperly() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());

    // Act
    Throwable thrown = catchThrowable(() -> new ClientConfig(props));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_HmacAuthEnabledButNoSecretKeyGiven_ShouldCreateConfigProperly() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.HMAC_SECRET_KEY_VERSION, SOME_SECRET_KEY_VERSION);

    // Act
    Throwable thrown = catchThrowable(() -> new ClientConfig(props));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_DigitalSignatureEnabledAndHmacDisabledAndHmacInformationGiven_ShouldCreateConfigProperly()
          throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(
        ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getDigitalSignatureIdentityConfig().getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(config.getDigitalSignatureIdentityConfig().getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(config.getDigitalSignatureIdentityConfig().getPrivateKey())
        .isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.getHmacIdentityConfig()).isNull();
  }

  @Test
  public void
      constructor_CertHolderIdGivenButBothCertStringAndPathMissing_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_EntityIdGivenButBothCertStringAndPathMissing_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_CertHolderIdGivenAndBothPrivateKeyStringAndPathMissing_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_EntityIdGivenAndBothPrivateKeyStringAndPathMissing_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_EntityIdGivenAndBothNewAndDeprecatedCertGiven_ShouldCreateConfigWithNewCert()
          throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_VERSION, "0");
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM + "New");
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM + "New");

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getDigitalSignatureIdentityConfig().getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(config.getDigitalSignatureIdentityConfig().getCert())
        .isEqualTo(SOME_CERT_PEM + "New");
    assertThat(config.getDigitalSignatureIdentityConfig().getPrivateKey())
        .isEqualTo(SOME_PRIVATE_KEY_PEM + "New");
  }

  @Test
  public void constructor_HmacAuthAsDeprecatedAuthenticationMethodGiven_ShouldCreateConfigProperly()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DEPRECATED_AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.HMAC_SECRET_KEY_VERSION, SOME_SECRET_KEY_VERSION);
    props.put(ClientConfig.HMAC_SECRET_KEY, SOME_SECRET_KEY);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getHmacIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getHmacIdentityConfig().getSecretKeyVersion())
        .isEqualTo(Integer.parseInt(SOME_SECRET_KEY_VERSION));
    assertThat(config.getHmacIdentityConfig().getSecretKey()).isEqualTo(SOME_SECRET_KEY);
    assertThat(config.getDigitalSignatureIdentityConfig()).isNull();
  }

  @Test
  public void
      constructor_BothDeprecatedAndNonDeprecatedAuthenticationMethodsGiven_ShouldCreateConfigWithNonDeprecatedOne()
          throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(
        ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.DIGITAL_SIGNATURE.getMethod());
    props.put(ClientConfig.DEPRECATED_AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    ClientConfig config = new ClientConfig(props);

    // Assert
    assertThat(config.getDigitalSignatureIdentityConfig().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getDigitalSignatureIdentityConfig().getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(config.getDigitalSignatureIdentityConfig().getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(config.getDigitalSignatureIdentityConfig().getPrivateKey())
        .isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(config.getHmacIdentityConfig()).isNull();
  }

  @Test
  public void constructor_SameClientConfigGiven_ShouldReturnEqualConfigs() throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props.put(ClientConfig.MODE, SOME_CLIENT_MODE);
    props.put(ClientConfig.SERVER_HOST, SOME_HOST);
    props.put(ClientConfig.SERVER_PORT, SOME_PORT);
    props.put(ClientConfig.SERVER_PRIVILEGED_PORT, SOME_PRIVILEGED_PORT);
    props.put(ClientConfig.TLS_ENABLED, SOME_TLS_ENABLED);
    props.put(ClientConfig.TLS_CA_ROOT_CERT_PEM, SOME_TLS_CA_ROOT_CERT_PEM);
    props.put(ClientConfig.TLS_OVERRIDE_AUTHORITY, SOME_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUTHORIZATION_CREDENTIAL, SOME_AUTHORIZATION_CREDENTIAL);
    props.put(ClientConfig.AUDITOR_ENABLED, SOME_AUDITOR_ENABLED);
    props.put(ClientConfig.AUDITOR_HOST, SOME_AUDITOR_HOST);
    props.put(ClientConfig.AUDITOR_PORT, SOME_AUDITOR_PORT);
    props.put(ClientConfig.AUDITOR_PRIVILEGED_PORT, SOME_AUDITOR_PRIVILEGED_PORT);
    props.put(ClientConfig.AUDITOR_TLS_ENABLED, SOME_AUDITOR_TLS_ENABLED);
    props.put(ClientConfig.AUDITOR_TLS_CA_ROOT_CERT_PEM, SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);
    props.put(ClientConfig.AUDITOR_TLS_OVERRIDE_AUTHORITY, SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY);
    props.put(ClientConfig.AUDITOR_AUTHORIZATION_CREDENTIAL, SOME_AUDITOR_AUTHORIZATION_CREDENTIAL);
    props.put(
        ClientConfig.AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID,
        SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID);

    // Act
    ClientConfig config1 = new ClientConfig(props);
    ClientConfig config2 = new ClientConfig(props);

    // Assert
    assertThat(config1.getDigitalSignatureIdentityConfig())
        .isEqualTo(config2.getDigitalSignatureIdentityConfig());
    assertThat(config1.getLedgerTargetConfig()).isEqualTo(config2.getLedgerTargetConfig());
    assertThat(config1.getAuditorTargetConfig()).isEqualTo(config2.getAuditorTargetConfig());
  }

  @Test
  public void constructor_DifferentClientConfigGiven_ShouldReturnNonEqualConfigs()
      throws IOException {
    // Arrange
    Properties props1 = new Properties();
    props1.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props1.put(ClientConfig.CERT_VERSION, SOME_CERT_VERSION);
    props1.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props1.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props1.put(ClientConfig.MODE, SOME_CLIENT_MODE);
    props1.put(ClientConfig.SERVER_HOST, SOME_HOST);
    props1.put(ClientConfig.SERVER_PORT, SOME_PORT);
    props1.put(ClientConfig.SERVER_PRIVILEGED_PORT, SOME_PRIVILEGED_PORT);
    props1.put(ClientConfig.TLS_ENABLED, SOME_TLS_ENABLED);
    props1.put(ClientConfig.TLS_CA_ROOT_CERT_PEM, SOME_TLS_CA_ROOT_CERT_PEM);
    props1.put(ClientConfig.TLS_OVERRIDE_AUTHORITY, SOME_TLS_OVERRIDE_AUTHORITY);
    props1.put(ClientConfig.AUTHORIZATION_CREDENTIAL, SOME_AUTHORIZATION_CREDENTIAL);
    props1.put(ClientConfig.AUDITOR_ENABLED, SOME_AUDITOR_ENABLED);
    props1.put(ClientConfig.AUDITOR_HOST, SOME_AUDITOR_HOST);
    props1.put(ClientConfig.AUDITOR_PORT, SOME_AUDITOR_PORT);
    props1.put(ClientConfig.AUDITOR_PRIVILEGED_PORT, SOME_AUDITOR_PRIVILEGED_PORT);
    props1.put(ClientConfig.AUDITOR_TLS_ENABLED, SOME_AUDITOR_TLS_ENABLED);
    props1.put(ClientConfig.AUDITOR_TLS_CA_ROOT_CERT_PEM, SOME_AUDITOR_TLS_CA_ROOT_CERT_PEM);
    props1.put(ClientConfig.AUDITOR_TLS_OVERRIDE_AUTHORITY, SOME_AUDITOR_TLS_OVERRIDE_AUTHORITY);
    props1.put(
        ClientConfig.AUDITOR_AUTHORIZATION_CREDENTIAL, SOME_AUDITOR_AUTHORIZATION_CREDENTIAL);
    props1.put(
        ClientConfig.AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID,
        SOME_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID);

    Properties props2 = new Properties();
    props2.putAll(props1);
    props2.put(ClientConfig.CERT_HOLDER_ID, "different_cert_holder_id");
    props2.put(ClientConfig.SERVER_HOST, "192.168.3.100");
    props2.put(ClientConfig.AUDITOR_HOST, "192.168.4.100");

    // Act
    ClientConfig config1 = new ClientConfig(props1);
    ClientConfig config2 = new ClientConfig(props2);

    // Assert
    assertThat(config1.getDigitalSignatureIdentityConfig())
        .isNotEqualTo(config2.getDigitalSignatureIdentityConfig());
    assertThat(config1.getLedgerTargetConfig()).isNotEqualTo(config2.getLedgerTargetConfig());
    assertThat(config1.getAuditorTargetConfig()).isNotEqualTo(config2.getAuditorTargetConfig());
  }

  @Test
  public void constructor_CertVersionZeroGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.DS_CERT_VERSION, "0");
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_DeprecatedCertVersionZeroGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_VERSION, "0");
    props.put(ClientConfig.CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      constructor_NewWrongCertVersionAndDeprecatedCorrectCertVersionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.DS_CERT_VERSION, "0");
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);
    props.put(ClientConfig.CERT_HOLDER_ID, SOME_CERT_HOLDER_ID);
    props.put(ClientConfig.CERT_VERSION, SOME_CERT_VERSION);

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_SecretVersionZeroGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.AUTHENTICATION_METHOD, AuthenticationMethod.HMAC.getMethod());
    props.put(ClientConfig.HMAC_SECRET_KEY, SOME_SECRET_KEY);
    props.put(ClientConfig.HMAC_SECRET_KEY_VERSION, "0");

    // Act Assert
    assertThatThrownBy(() -> new ClientConfig(props)).isInstanceOf(IllegalArgumentException.class);
  }
}
