package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class LedgerValidationRequestTest {
  private static final String NAMESPACE = "namespace";
  private static final String CONTEXT_NAMESPACE = "context_namespace";
  private static final String ASSET_ID = "asset_id";
  private static final int START_AGE = 0;
  private static final int END_AGE = 10;
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WRONG_SIGNATURE = "wrong_signature".getBytes(StandardCharsets.UTF_8);
  private static final Object ARBITRARY_OBJECT = new Object();

  @Test
  public void constructor_ArgumentsGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(
            () ->
                new LedgerValidationRequest(
                    NAMESPACE,
                    ASSET_ID,
                    START_AGE,
                    END_AGE,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
                    SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNamespaceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(
            () ->
                new LedgerValidationRequest(
                    null,
                    ASSET_ID,
                    START_AGE,
                    END_AGE,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
                    SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void get_ProperRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    String namespace = request.getNamespace();
    String assetId = request.getAssetId();
    int startAge = request.getStartAge();
    int endAge = request.getEndAge();
    String contextNamespace = request.getContextNamespace();
    String entityId = request.getEntityId();
    int keyVersion = request.getKeyVersion();
    byte[] signature = request.getSignature();

    // Assert
    assertThat(namespace).isEqualTo(NAMESPACE);
    assertThat(assetId).isEqualTo(ASSET_ID);
    assertThat(startAge).isEqualTo(START_AGE);
    assertThat(endAge).isEqualTo(END_AGE);
    assertThat(contextNamespace).isEqualTo(CONTEXT_NAMESPACE);
    assertThat(entityId).isEqualTo(ENTITY_ID);
    assertThat(keyVersion).isEqualTo(KEY_VERSION);
    assertThat(signature).isEqualTo(SIGNATURE);
  }

  @Test
  public void get_NullNamespaceGiven_ShouldReturnNull() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            null,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    String namespace = request.getNamespace();

    // Assert
    assertThat(namespace).isNull();
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameDataWithNullNamespace_ShouldReturnTrue() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            null,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            null,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentNamespace_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            "different_namespace",
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentContextNamespace_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            "different_context_namespace",
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentAssetId_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE, ASSET_ID, START_AGE, END_AGE, null, ENTITY_ID, KEY_VERSION, SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            "different_asset_id",
            START_AGE,
            END_AGE,
            null,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentStartAge_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE, ASSET_ID, 99, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentEndAge_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            99,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSignature_ShouldReturnFalse() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            WRONG_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    LedgerValidationRequest request =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    LedgerValidationRequest other =
        new LedgerValidationRequest(
            NAMESPACE,
            ASSET_ID,
            START_AGE,
            END_AGE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    int hash1 = request.hashCode();
    int hash2 = other.hashCode();

    // Assert
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void serialize_WithNamespace_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized =
        LedgerValidationRequest.serialize(
            NAMESPACE, ASSET_ID, START_AGE, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithNullNamespace_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized =
        LedgerValidationRequest.serialize(
            null, ASSET_ID, START_AGE, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithDifferentNamespace_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] serialized1 =
        LedgerValidationRequest.serialize(
            NAMESPACE, ASSET_ID, START_AGE, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION);
    byte[] serialized2 =
        LedgerValidationRequest.serialize(
            null, ASSET_ID, START_AGE, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized1).isNotEqualTo(serialized2);
  }

  @Test
  public void serialize_WithDifferentContextNamespace_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] serialized1 =
        LedgerValidationRequest.serialize(
            NAMESPACE, ASSET_ID, START_AGE, END_AGE, null, ENTITY_ID, KEY_VERSION);
    byte[] serialized2 =
        LedgerValidationRequest.serialize(
            NAMESPACE, ASSET_ID, START_AGE, END_AGE, CONTEXT_NAMESPACE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized1).isNotEqualTo(serialized2);
  }
}
