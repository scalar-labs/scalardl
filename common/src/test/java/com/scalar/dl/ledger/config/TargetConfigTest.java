package com.scalar.dl.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;

public class TargetConfigTest {
  private static final String SOME_HOST = "some_host";
  private static final int SOME_PORT = 2;
  private static final int SOME_PRIVILEGED_PORT = 3;
  private static final boolean SOME_TLS_ENABLED = true;
  private static final String SOME_TLS_CA_ROOT_CERT = "some_tls_ca_root_cert";
  private static final String SOME_AUTHORIZATION_CREDENTIAL = "some_authorization_credential";

  @Test
  public void constructor_AllAttributesGiven_ShouldCreateProperly() {
    // Arrange

    // Act
    TargetConfig config =
        TargetConfig.newBuilder()
            .host(SOME_HOST)
            .port(SOME_PORT)
            .privilegedPort(SOME_PRIVILEGED_PORT)
            .tlsEnabled(SOME_TLS_ENABLED)
            .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
            .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
            .build();

    // Assert
    assertThat(config.getTargetHost()).isEqualTo(SOME_HOST);
    assertThat(config.getTargetPort()).isEqualTo(SOME_PORT);
    assertThat(config.getTargetPrivilegedPort()).isEqualTo(SOME_PRIVILEGED_PORT);
    assertThat(config.isTargetTlsEnabled()).isEqualTo(SOME_TLS_ENABLED);
    assertThat(config.getTargetTlsCaRootCert()).isEqualTo(SOME_TLS_CA_ROOT_CERT);
    assertThat(config.getTargetAuthorizationCredential()).isEqualTo(SOME_AUTHORIZATION_CREDENTIAL);
  }

  @Test
  public void constructor_SomeAttributeNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                TargetConfig.newBuilder()
                    .host(SOME_HOST)
                    .privilegedPort(SOME_PRIVILEGED_PORT)
                    .tlsEnabled(SOME_TLS_ENABLED)
                    .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
                    .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
                    .build());

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_SameAttributesGiven_ShouldReturnTrue() {
    // Arrange
    TargetConfig config1 =
        TargetConfig.newBuilder()
            .host(SOME_HOST)
            .port(SOME_PORT)
            .privilegedPort(SOME_PRIVILEGED_PORT)
            .tlsEnabled(SOME_TLS_ENABLED)
            .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
            .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
            .build();
    TargetConfig config2 =
        TargetConfig.newBuilder()
            .host(SOME_HOST)
            .port(SOME_PORT)
            .privilegedPort(SOME_PRIVILEGED_PORT)
            .tlsEnabled(SOME_TLS_ENABLED)
            .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
            .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isTrue();
  }

  @Test
  public void equals_DifferentAttributesGiven_ShouldReturnFalse() {
    // Arrange
    TargetConfig config1 =
        TargetConfig.newBuilder()
            .host(SOME_HOST)
            .port(SOME_PORT)
            .privilegedPort(SOME_PRIVILEGED_PORT)
            .tlsEnabled(SOME_TLS_ENABLED)
            .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
            .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
            .build();
    TargetConfig config2 =
        TargetConfig.newBuilder()
            .host(SOME_HOST)
            .port(SOME_PORT)
            .privilegedPort(100)
            .tlsEnabled(SOME_TLS_ENABLED)
            .tlsCaRootCert(SOME_TLS_CA_ROOT_CERT)
            .authorizationCredential(SOME_AUTHORIZATION_CREDENTIAL)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isFalse();
  }
}
