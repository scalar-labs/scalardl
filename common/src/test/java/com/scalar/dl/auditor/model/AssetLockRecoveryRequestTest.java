package com.scalar.dl.auditor.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class AssetLockRecoveryRequestTest {
  private static final String NAMESPACE = "test_namespace";
  private static final String ASSET_ID = "test_asset_id";

  @Test
  public void constructor_ProperValuesGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(() -> new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNamespaceGiven_ShouldThrowNullPointerException() {
    // Act Assert
    assertThatThrownBy(() -> new AssetLockRecoveryRequest(null, ASSET_ID))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_NullAssetIdGiven_ShouldThrowNullPointerException() {
    // Act Assert
    assertThatThrownBy(() -> new AssetLockRecoveryRequest(NAMESPACE, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void getNamespace_ProperValueGiven_ShouldReturnProperValue() {
    // Act
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);

    // Assert
    assertThat(request.getNamespace()).isEqualTo(NAMESPACE);
  }

  @Test
  public void getAssetId_ProperValueGiven_ShouldReturnProperValue() {
    // Act
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);

    // Assert
    assertThat(request.getAssetId()).isEqualTo(ASSET_ID);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnDifferentNamespace_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other = new AssetLockRecoveryRequest("different_namespace", ASSET_ID);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentAssetId_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other = new AssetLockRecoveryRequest(NAMESPACE, "different_asset_id");

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnNull_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);

    // Act
    boolean result = request.equals(null);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    Object arbitraryObject = new Object();

    // Act
    boolean result = request.equals(arbitraryObject);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);

    // Act
    int hashCode1 = request.hashCode();
    int hashCode2 = other.hashCode();

    // Assert
    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  public void hashCode_OnDifferentData_ShouldReturnDifferentHashCode() {
    // Arrange
    AssetLockRecoveryRequest request = new AssetLockRecoveryRequest(NAMESPACE, ASSET_ID);
    AssetLockRecoveryRequest other =
        new AssetLockRecoveryRequest("different_namespace", "different_asset_id");

    // Act
    int hashCode1 = request.hashCode();
    int hashCode2 = other.hashCode();

    // Assert
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }
}
