package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class AssetProofRetrievalRequestTest {
  private static final String NAMESPACE = "namespace";
  private static final String ASSET_ID = "asset_id";
  private static final int AGE = 1;
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
                new AssetProofRetrievalRequest(
                    NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNamespaceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(
            () ->
                new AssetProofRetrievalRequest(
                    null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullAssetIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new AssetProofRetrievalRequest(
                    NAMESPACE, null, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_ProperRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    String namespace = request.getNamespace();
    String assetId = request.getAssetId();
    int age = request.getAge();
    String entityId = request.getEntityId();
    int keyVersion = request.getKeyVersion();
    byte[] signature = request.getSignature();

    // Assert
    assertThat(namespace).isEqualTo(NAMESPACE);
    assertThat(assetId).isEqualTo(ASSET_ID);
    assertThat(age).isEqualTo(AGE);
    assertThat(entityId).isEqualTo(ENTITY_ID);
    assertThat(keyVersion).isEqualTo(KEY_VERSION);
    assertThat(signature).isEqualTo(SIGNATURE);
  }

  @Test
  public void get_NullNamespaceGiven_ShouldReturnNull() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    String namespace = request.getNamespace();

    // Assert
    assertThat(namespace).isNull();
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameDataWithNullNamespace_ShouldReturnTrue() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentNamespace_ShouldReturnFalse() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(
            "different_namespace", ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentAssetId_ShouldReturnFalse() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(
            NAMESPACE, "different_asset_id", AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentAge_ShouldReturnFalse() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, 99, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSignature_ShouldReturnFalse() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(
            NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, WRONG_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    AssetProofRetrievalRequest request =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetProofRetrievalRequest other =
        new AssetProofRetrievalRequest(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION, SIGNATURE);

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
        AssetProofRetrievalRequest.serialize(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithNullNamespace_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized =
        AssetProofRetrievalRequest.serialize(null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithDifferentNamespace_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] serialized1 =
        AssetProofRetrievalRequest.serialize(NAMESPACE, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION);
    byte[] serialized2 =
        AssetProofRetrievalRequest.serialize(null, ASSET_ID, AGE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized1).isNotEqualTo(serialized2);
  }
}
