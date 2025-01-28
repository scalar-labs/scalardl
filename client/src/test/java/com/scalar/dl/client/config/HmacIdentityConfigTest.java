package com.scalar.dl.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

public class HmacIdentityConfigTest {
  private static final String SOME_ENTITY_ID = "some_entity_id";
  private static final int SOME_SECRET_KEY_VERSION = 2;
  private static final String SOME_SECRET_KEY = "some_secret_key";

  @Test
  public void constructor_AllAttributesGiven_ShouldCreateProperly() {
    // Arrange

    // Act
    HmacIdentityConfig config =
        HmacIdentityConfig.newBuilder()
            .entityId(SOME_ENTITY_ID)
            .secretKeyVersion(SOME_SECRET_KEY_VERSION)
            .secretKey(SOME_SECRET_KEY)
            .build();

    // Assert
    assertThat(config.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(config.getSecretKeyVersion()).isEqualTo(SOME_SECRET_KEY_VERSION);
    assertThat(config.getSecretKey()).isEqualTo(SOME_SECRET_KEY);
  }

  @Test
  public void constructor_EntityIdNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                HmacIdentityConfig.newBuilder()
                    .secretKeyVersion(SOME_SECRET_KEY_VERSION)
                    .secretKey(SOME_SECRET_KEY)
                    .build());

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_KeyVersionNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                HmacIdentityConfig.newBuilder()
                    .entityId(SOME_ENTITY_ID)
                    .secretKey(SOME_SECRET_KEY)
                    .build());

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_SecretKeyNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () ->
                HmacIdentityConfig.newBuilder()
                    .entityId(SOME_ENTITY_ID)
                    .secretKeyVersion(SOME_SECRET_KEY_VERSION)
                    .build());

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_SameAttributesGiven_ShouldReturnTrue() {
    // Arrange
    HmacIdentityConfig config1 =
        HmacIdentityConfig.newBuilder()
            .entityId(SOME_ENTITY_ID)
            .secretKeyVersion(SOME_SECRET_KEY_VERSION)
            .secretKey(SOME_SECRET_KEY)
            .build();
    HmacIdentityConfig config2 =
        HmacIdentityConfig.newBuilder()
            .entityId(SOME_ENTITY_ID)
            .secretKeyVersion(SOME_SECRET_KEY_VERSION)
            .secretKey(SOME_SECRET_KEY)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isTrue();
  }

  @Test
  public void equals_DifferentAttributesGiven_ShouldReturnFalse() {
    // Arrange
    HmacIdentityConfig config1 =
        HmacIdentityConfig.newBuilder()
            .entityId(SOME_ENTITY_ID)
            .secretKeyVersion(SOME_SECRET_KEY_VERSION)
            .secretKey(SOME_SECRET_KEY + "x")
            .build();
    HmacIdentityConfig config2 =
        HmacIdentityConfig.newBuilder()
            .entityId(SOME_ENTITY_ID)
            .secretKeyVersion(SOME_SECRET_KEY_VERSION)
            .secretKey(SOME_SECRET_KEY)
            .build();

    // Act
    boolean ret = config1.equals(config2);

    // Assert
    assertThat(ret).isFalse();
  }
}
