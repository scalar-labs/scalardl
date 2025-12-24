package com.scalar.dl.ledger.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class AssetKeyTest {
  private static final String NAMESPACE1 = "namespace1";
  private static final String NAMESPACE2 = "namespace2";
  private static final String ASSET_ID1 = "asset1";
  private static final String ASSET_ID2 = "asset2";

  @Test
  public void of_NamespaceAndAssetIdGiven_ShouldCreateAssetKey() {
    // Arrange Act
    AssetKey key = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Assert
    assertThat(key.namespace()).isEqualTo(NAMESPACE1);
    assertThat(key.assetId()).isEqualTo(ASSET_ID1);
  }

  @Test
  public void equals_SameInstance_ShouldReturnTrue() {
    // Arrange
    AssetKey key = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act Assert
    assertThat(key.equals(key)).isTrue();
  }

  @Test
  public void equals_SameValues_ShouldReturnTrue() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, ASSET_ID1);
    AssetKey key2 = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act Assert
    assertThat(key1.equals(key2)).isTrue();
    assertThat(key2.equals(key1)).isTrue();
  }

  @Test
  public void equals_DifferentNamespace_ShouldReturnFalse() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, ASSET_ID1);
    AssetKey key2 = AssetKey.of(NAMESPACE2, ASSET_ID1);

    // Act Assert
    assertThat(key1.equals(key2)).isFalse();
  }

  @Test
  public void equals_DifferentAssetId_ShouldReturnFalse() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, ASSET_ID1);
    AssetKey key2 = AssetKey.of(NAMESPACE1, ASSET_ID2);

    // Act Assert
    assertThat(key1.equals(key2)).isFalse();
  }

  @Test
  public void equals_Null_ShouldReturnFalse() {
    // Arrange
    AssetKey key = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act Assert
    assertThat(key.equals(null)).isFalse();
  }

  @Test
  public void hashCode_SameValues_ShouldReturnSameHash() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, ASSET_ID1);
    AssetKey key2 = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act Assert
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  public void compareTo_SameValues_ShouldReturnZero() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, ASSET_ID1);
    AssetKey key2 = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act Assert
    assertThat(key1.compareTo(key2)).isEqualTo(0);
  }

  @Test
  public void compareTo_DifferentNamespace_ShouldSortByNamespace() {
    // Arrange
    AssetKey key1 = AssetKey.of("aaa", ASSET_ID1);
    AssetKey key2 = AssetKey.of("bbb", ASSET_ID1);

    // Act Assert
    assertThat(key1.compareTo(key2)).isLessThan(0);
    assertThat(key2.compareTo(key1)).isGreaterThan(0);
  }

  @Test
  public void compareTo_SameNamespaceDifferentAssetId_ShouldSortByAssetId() {
    // Arrange
    AssetKey key1 = AssetKey.of(NAMESPACE1, "aaa");
    AssetKey key2 = AssetKey.of(NAMESPACE1, "bbb");

    // Act Assert
    assertThat(key1.compareTo(key2)).isLessThan(0);
    assertThat(key2.compareTo(key1)).isGreaterThan(0);
  }

  @Test
  public void toString_ShouldContainNamespaceAndAssetId() {
    // Arrange
    AssetKey key = AssetKey.of(NAMESPACE1, ASSET_ID1);

    // Act
    String result = key.toString();

    // Assert
    assertThat(result).contains(NAMESPACE1);
    assertThat(result).contains(ASSET_ID1);
  }
}
