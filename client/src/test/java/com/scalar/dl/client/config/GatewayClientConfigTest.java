package com.scalar.dl.client.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

public class GatewayClientConfigTest {
  private static final String SOME_ENTITY_ID = "some_entity_id";
  private static final String SOME_CERT_VERSION = "2";
  private static final String SOME_CERT_PATH = "cert_path";
  private static final String SOME_CERT_PEM = "some_cert_string";
  private static final String SOME_PRIVATE_KEY_PATH = "private_key_path";
  private static final String SOME_PRIVATE_KEY_PEM = "some_private_key_string";
  private static final String SOME_GATEWAY_HOST = "192.168.1.100";
  private static final String SOME_GATEWAY_PORT = "70051";
  private static final String SOME_GATEWAY_PRIVILEGED_PORT = "70052";
  private static final String SOME_GATEWAY_TLS_ENABLED = "true";
  private static final String SOME_GATEWAY_TLS_CA_ROOT_CERT_PATH = "gateway_tls_ca_root_cert_path";
  private static final String SOME_GATEWAY_TLS_CA_ROOT_CERT_PEM = "tls.ca_root_cert_string.gateway";
  private static final String SOME_GATEWAY_TLS_OVERRIDE_AUTHORITY = "tls.override_authority";
  private static final String SOME_GATEWAY_AUTHORIZATION_CREDENTIAL =
      "authorization.credential.gateway";

  @TempDir Path folder;

  private void writeToFile(File file, String content) throws IOException {
    BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    writer.write(content);
    writer.close();
  }

  @Test
  public void constructor_AllGatewayPropertiesWithPemGiven_ShouldCreateInstance()
      throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(GatewayClientConfig.GATEWAY_HOST, SOME_GATEWAY_HOST);
    props.put(GatewayClientConfig.GATEWAY_PORT, SOME_GATEWAY_PORT);
    props.put(GatewayClientConfig.GATEWAY_PRIVILEGED_PORT, SOME_GATEWAY_PRIVILEGED_PORT);
    props.put(GatewayClientConfig.GATEWAY_TLS_ENABLED, SOME_GATEWAY_TLS_ENABLED);
    props.put(GatewayClientConfig.GATEWAY_TLS_CA_ROOT_CERT_PEM, SOME_GATEWAY_TLS_CA_ROOT_CERT_PEM);
    props.put(
        GatewayClientConfig.GATEWAY_TLS_OVERRIDE_AUTHORITY, SOME_GATEWAY_TLS_OVERRIDE_AUTHORITY);
    props.put(
        GatewayClientConfig.GATEWAY_AUTHORIZATION_CREDENTIAL,
        SOME_GATEWAY_AUTHORIZATION_CREDENTIAL);
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    GatewayClientConfig config = new GatewayClientConfig(props);

    // Assert
    ClientConfig clientConfig = config.getClientConfig();
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        clientConfig.getDigitalSignatureIdentityConfig();
    TargetConfig gatewayTargetConfig = config.getGatewayTargetConfig();
    assertThat(clientConfig.getClientMode()).isEqualTo(ClientMode.CLIENT);
    assertThat(clientConfig.isAuditorEnabled()).isEqualTo(false);
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(gatewayTargetConfig.getTargetHost()).isEqualTo(SOME_GATEWAY_HOST);
    assertThat(gatewayTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_GATEWAY_PORT));
    assertThat(gatewayTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_GATEWAY_PRIVILEGED_PORT));
    assertThat(gatewayTargetConfig.isTargetTlsEnabled()).isEqualTo(true);
    assertThat(gatewayTargetConfig.getTargetTlsCaRootCert())
        .isEqualTo(SOME_GATEWAY_TLS_CA_ROOT_CERT_PEM);
    assertThat(gatewayTargetConfig.getTargetTlsOverrideAuthority())
        .isEqualTo(SOME_GATEWAY_TLS_OVERRIDE_AUTHORITY);
    assertThat(gatewayTargetConfig.getTargetAuthorizationCredential())
        .isEqualTo(SOME_GATEWAY_AUTHORIZATION_CREDENTIAL);
    assertThat(gatewayTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(ClientConfig.DEFAULT_DEADLINE_DURATION_MILLIS);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize()).isEqualTo(0);
  }

  @Test
  public void constructor_AllGatewayPropertiesWithPathGiven_ShouldCreateInstance()
      throws IOException {
    // Arrange
    File certPath = folder.resolve(SOME_CERT_PATH).toFile();
    writeToFile(certPath.getCanonicalFile(), SOME_CERT_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_PRIVATE_KEY_PEM);
    File tlsCaRootCertPath = folder.resolve(SOME_GATEWAY_TLS_CA_ROOT_CERT_PATH).toFile();
    writeToFile(tlsCaRootCertPath.getCanonicalFile(), SOME_GATEWAY_TLS_CA_ROOT_CERT_PEM);

    Properties props = new Properties();
    props.put(GatewayClientConfig.GATEWAY_HOST, SOME_GATEWAY_HOST);
    props.put(GatewayClientConfig.GATEWAY_PORT, SOME_GATEWAY_PORT);
    props.put(GatewayClientConfig.GATEWAY_PRIVILEGED_PORT, SOME_GATEWAY_PRIVILEGED_PORT);
    props.put(GatewayClientConfig.GATEWAY_TLS_ENABLED, SOME_GATEWAY_TLS_ENABLED);
    props.put(
        GatewayClientConfig.GATEWAY_TLS_CA_ROOT_CERT_PATH, tlsCaRootCertPath.getCanonicalPath());
    props.put(
        GatewayClientConfig.GATEWAY_TLS_OVERRIDE_AUTHORITY, SOME_GATEWAY_TLS_OVERRIDE_AUTHORITY);
    props.put(
        GatewayClientConfig.GATEWAY_AUTHORIZATION_CREDENTIAL,
        SOME_GATEWAY_AUTHORIZATION_CREDENTIAL);
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_VERSION, SOME_CERT_VERSION);
    props.put(ClientConfig.DS_CERT_PATH, certPath.getCanonicalPath());
    props.put(ClientConfig.DS_PRIVATE_KEY_PATH, privateKeyPath.getCanonicalPath());

    // Act
    GatewayClientConfig config = new GatewayClientConfig(props);

    // Assert
    ClientConfig clientConfig = config.getClientConfig();
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        clientConfig.getDigitalSignatureIdentityConfig();
    TargetConfig gatewayTargetConfig = config.getGatewayTargetConfig();
    assertThat(clientConfig.getClientMode()).isEqualTo(ClientMode.CLIENT);
    assertThat(clientConfig.isAuditorEnabled()).isEqualTo(false);
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(Integer.parseInt(SOME_CERT_VERSION));
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(gatewayTargetConfig.getTargetHost()).isEqualTo(SOME_GATEWAY_HOST);
    assertThat(gatewayTargetConfig.getTargetPort()).isEqualTo(Integer.parseInt(SOME_GATEWAY_PORT));
    assertThat(gatewayTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(Integer.parseInt(SOME_GATEWAY_PRIVILEGED_PORT));
    assertThat(gatewayTargetConfig.isTargetTlsEnabled()).isEqualTo(true);
    assertThat(gatewayTargetConfig.getTargetTlsCaRootCert())
        .isEqualTo(SOME_GATEWAY_TLS_CA_ROOT_CERT_PEM);
    assertThat(gatewayTargetConfig.getTargetTlsOverrideAuthority())
        .isEqualTo(SOME_GATEWAY_TLS_OVERRIDE_AUTHORITY);
    assertThat(gatewayTargetConfig.getTargetAuthorizationCredential())
        .isEqualTo(SOME_GATEWAY_AUTHORIZATION_CREDENTIAL);
    assertThat(gatewayTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(ClientConfig.DEFAULT_DEADLINE_DURATION_MILLIS);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize()).isEqualTo(0);
  }

  @Test
  public void constructor_OnlyRequiredPropertiesGiven_ShouldCreateInstance() throws IOException {
    // Arrange
    Properties props = new Properties();
    props.put(ClientConfig.ENTITY_ID, SOME_ENTITY_ID);
    props.put(ClientConfig.DS_CERT_PEM, SOME_CERT_PEM);
    props.put(ClientConfig.DS_PRIVATE_KEY_PEM, SOME_PRIVATE_KEY_PEM);

    // Act
    GatewayClientConfig config = new GatewayClientConfig(props);

    // Assert
    ClientConfig clientConfig = config.getClientConfig();
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        clientConfig.getDigitalSignatureIdentityConfig();
    TargetConfig gatewayTargetConfig = config.getGatewayTargetConfig();
    assertThat(clientConfig.getClientMode()).isEqualTo(ClientMode.CLIENT);
    assertThat(clientConfig.isAuditorEnabled()).isEqualTo(false);
    assertThat(digitalSignatureIdentityConfig.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(digitalSignatureIdentityConfig.getCertVersion())
        .isEqualTo(ClientConfig.DEFAULT_CERT_VERSION);
    assertThat(digitalSignatureIdentityConfig.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(digitalSignatureIdentityConfig.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
    assertThat(gatewayTargetConfig.getTargetHost())
        .isEqualTo(GatewayClientConfig.DEFAULT_GATEWAY_HOST);
    assertThat(gatewayTargetConfig.getTargetPort())
        .isEqualTo(GatewayClientConfig.DEFAULT_GATEWAY_PORT);
    assertThat(gatewayTargetConfig.getTargetPrivilegedPort())
        .isEqualTo(GatewayClientConfig.DEFAULT_GATEWAY_PRIVILEGED_PORT);
    assertThat(gatewayTargetConfig.isTargetTlsEnabled())
        .isEqualTo(GatewayClientConfig.DEFAULT_GATEWAY_TLS_ENABLED);
    assertThat(gatewayTargetConfig.getGrpcClientConfig()).isNotNull();
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getDeadlineDurationMillis())
        .isEqualTo(ClientConfig.DEFAULT_DEADLINE_DURATION_MILLIS);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMessageSize()).isEqualTo(0);
    assertThat(gatewayTargetConfig.getGrpcClientConfig().getMaxInboundMetadataSize()).isEqualTo(0);
  }
}
