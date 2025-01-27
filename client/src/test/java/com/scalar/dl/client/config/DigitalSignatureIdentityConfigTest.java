package com.scalar.dl.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;

public class DigitalSignatureIdentityConfigTest {
  private static final String SOME_CERT_HOLDER_ID = "some_cert_holder_id";
  private static final int SOME_CERT_VERSION = 2;
  private static final String SOME_CERT_PEM = "some_cert_string";
  private static final String SOME_PRIVATE_KEY_PEM = "some_private_key_string";

  @Test
  public void constructor_AllAttributesGiven_ShouldCreateProperly() {
    // Arrange

    // Act
    DigitalSignatureIdentityConfig config =
        DigitalSignatureIdentityConfig.newBuilder()
            .entityId(SOME_CERT_HOLDER_ID)
            .certVersion(SOME_CERT_VERSION)
            .cert(SOME_CERT_PEM)
            .privateKey(SOME_PRIVATE_KEY_PEM)
            .build();

    // Assert
    assertThat(config.getEntityId()).isEqualTo(SOME_CERT_HOLDER_ID);
    assertThat(config.getCertVersion()).isEqualTo(SOME_CERT_VERSION);
    assertThat(config.getCert()).isEqualTo(SOME_CERT_PEM);
    assertThat(config.getPrivateKey()).isEqualTo(SOME_PRIVATE_KEY_PEM);
  }

  @Test
  public void constructor_SomeAttributeNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                DigitalSignatureIdentityConfig.newBuilder()
                    .entityId(SOME_CERT_HOLDER_ID)
                    .certVersion(SOME_CERT_VERSION)
                    .privateKey(SOME_PRIVATE_KEY_PEM)
                    .build());

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_SameAttributesGiven_ShouldReturnTrue() {
    // Arrange
    DigitalSignatureIdentityConfig config1 =
        DigitalSignatureIdentityConfig.newBuilder()
            .entityId(SOME_CERT_HOLDER_ID)
            .certVersion(SOME_CERT_VERSION)
            .cert(SOME_CERT_PEM)
            .privateKey(SOME_PRIVATE_KEY_PEM)
            .build();
    DigitalSignatureIdentityConfig config2 =
        DigitalSignatureIdentityConfig.newBuilder()
            .entityId(SOME_CERT_HOLDER_ID)
            .certVersion(SOME_CERT_VERSION)
            .cert(SOME_CERT_PEM)
            .privateKey(SOME_PRIVATE_KEY_PEM)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isTrue();
  }

  @Test
  public void equals_DifferentAttributesGiven_ShouldReturnFalse() {
    // Arrange
    DigitalSignatureIdentityConfig config1 =
        DigitalSignatureIdentityConfig.newBuilder()
            .entityId(SOME_CERT_HOLDER_ID)
            .certVersion(SOME_CERT_VERSION)
            .cert(SOME_CERT_PEM)
            .privateKey(SOME_PRIVATE_KEY_PEM)
            .build();
    DigitalSignatureIdentityConfig config2 =
        DigitalSignatureIdentityConfig.newBuilder()
            .entityId(SOME_CERT_HOLDER_ID)
            .certVersion(SOME_CERT_VERSION)
            .cert(SOME_CERT_PEM + "x")
            .privateKey(SOME_PRIVATE_KEY_PEM)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isFalse();
  }
}
