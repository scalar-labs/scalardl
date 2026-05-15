package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.dl.ledger.exception.SignatureException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class AssetLockRecoveryRequestTest {
  private static final String NAMESPACE = "namespace";
  private static final String ASSET_ID = "asset_id";
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
                new AssetLockRecoveryRequest(
                    NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNamespaceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(
            () -> new AssetLockRecoveryRequest(null, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullEntityIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () -> new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, null, KEY_VERSION, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullAssetIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () -> new AssetLockRecoveryRequest(NAMESPACE, null, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_ZeroKeyVersionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () -> new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, 0, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_ProperRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    String namespace = request.getNamespace();
    String assetId = request.getAssetId();
    String entityId = request.getEntityId();
    int keyVersion = request.getKeyVersion();
    byte[] signature = request.getSignature();

    // Assert
    assertThat(namespace).isEqualTo(NAMESPACE);
    assertThat(assetId).isEqualTo(ASSET_ID);
    assertThat(entityId).isEqualTo(ENTITY_ID);
    assertThat(keyVersion).isEqualTo(KEY_VERSION);
    assertThat(signature).isEqualTo(SIGNATURE);
  }

  @Test
  public void get_NullNamespaceGiven_ShouldReturnNull() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(null, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    String namespace = request.getNamespace();

    // Assert
    assertThat(namespace).isNull();
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameDataWithNullNamespace_ShouldReturnTrue() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(null, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(null, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentNamespace_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(
            "different_namespace", ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentAssetId_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(
            NAMESPACE, "different_asset_id", ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSignature_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, WRONG_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    int hash1 = request.hashCode();
    int hash2 = other.hashCode();

    // Assert
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void validateWith_ValidSignatureGiven_ShouldNotThrowException() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act Assert
    assertThatCode(() -> request.validateWith((toBeValidated, sig) -> true))
        .doesNotThrowAnyException();
  }

  @Test
  public void validateWith_InvalidSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act Assert
    assertThatThrownBy(() -> request.validateWith((toBeValidated, sig) -> false))
        .isInstanceOf(SignatureException.class);
  }

  @Test
  public void validateWith_ProperBytesAndSignatureGiven_ShouldPassCorrectArguments() {
    // Arrange
    byte[] expectedBytes =
        AssetLockRecoveryRequest.serialize(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION);
    AssetLockRecoveryRequest request =
        new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act Assert
    assertThatCode(
            () ->
                request.validateWith(
                    (toBeValidated, sig) -> {
                      assertThat(toBeValidated).isEqualTo(expectedBytes);
                      assertThat(sig).isEqualTo(SIGNATURE);
                      return true;
                    }))
        .doesNotThrowAnyException();
  }

  @Test
  public void serialize_WithNamespace_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized =
        AssetLockRecoveryRequest.serialize(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithNullNamespace_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized = AssetLockRecoveryRequest.serialize(null, ASSET_ID, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithDifferentNamespace_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] serialized1 =
        AssetLockRecoveryRequest.serialize(NAMESPACE, ASSET_ID, ENTITY_ID, KEY_VERSION);
    byte[] serialized2 = AssetLockRecoveryRequest.serialize(null, ASSET_ID, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized1).isNotEqualTo(serialized2);
  }
}
