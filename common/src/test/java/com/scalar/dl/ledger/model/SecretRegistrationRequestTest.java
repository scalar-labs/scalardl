package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class SecretRegistrationRequestTest {
  private static final String CONTEXT_NAMESPACE = "test_namespace";
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  private static final int ILLEGAL_KEY_VERSION = 0;
  private static final String SECRET_KEY = "secret_key";

  @Test
  public void constructor_ProperValuesGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(
            () ->
                new SecretRegistrationRequest(
                    CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_IllegalKeyVersionGiven_ShouldThrowIllegalArgumentException() {
    // Act Assert
    assertThatThrownBy(
            () ->
                new SecretRegistrationRequest(
                    CONTEXT_NAMESPACE, ENTITY_ID, ILLEGAL_KEY_VERSION, SECRET_KEY))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullEntityIdGiven_ShouldThrowIllegalArgumentException() {
    // Act Assert
    assertThatThrownBy(
            () -> new SecretRegistrationRequest(CONTEXT_NAMESPACE, null, KEY_VERSION, SECRET_KEY))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullSecretKeyGiven_ShouldThrowNullPointerException() {
    // Act Assert
    assertThatThrownBy(
            () -> new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    Object arbitraryObject = new Object();

    // Act
    boolean result = request.equals(arbitraryObject);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSecretKey_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest(
            CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, "different_secret_key");

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentContextNamespace_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest("different_namespace", ENTITY_ID, KEY_VERSION, SECRET_KEY);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnNullAndNonNullContextNamespace_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(null, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentEntityId_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest(
            CONTEXT_NAMESPACE, "different_entity_id", KEY_VERSION, SECRET_KEY);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentKeyVersion_ShouldReturnFalse() {
    // Arrange
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);
    SecretRegistrationRequest other =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, 2, SECRET_KEY);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void getters_ProperValuesGiven_ShouldReturnProperValues() {
    // Act
    SecretRegistrationRequest request =
        new SecretRegistrationRequest(CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SECRET_KEY);

    // Assert
    assertThat(request.getContextNamespace()).isEqualTo(CONTEXT_NAMESPACE);
    assertThat(request.getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(request.getKeyVersion()).isEqualTo(KEY_VERSION);
    assertThat(request.getSecretKey()).isEqualTo(SECRET_KEY);
  }
}
