package com.scalar.dl.ledger.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class NamespaceAwareAssetFilterTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_ID = "some_id";

  @Test
  public void constructor_NamespaceAndIdGiven_ShouldCreateFilter() {
    // Arrange Act
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Assert
    assertThat(filter.getNamespace()).isEqualTo(SOME_NAMESPACE);
    assertThat(filter.getId()).isEqualTo(SOME_ID);
  }

  @Test
  public void withStartAge_AgeGiven_ShouldSetStartAge() {
    // Arrange
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withStartAge(10, true);

    // Assert
    assertThat(filter.getStartAge()).isPresent();
    assertThat(filter.getStartAge().get()).isEqualTo(10);
    assertThat(filter.isStartInclusive()).isTrue();
  }

  @Test
  public void withEndAge_AgeGiven_ShouldSetEndAge() {
    // Arrange
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withEndAge(20, false);

    // Assert
    assertThat(filter.getEndAge()).isPresent();
    assertThat(filter.getEndAge().get()).isEqualTo(20);
    assertThat(filter.isEndInclusive()).isFalse();
  }

  @Test
  public void withAgeOrder_AgeOrderGiven_ShouldSetAgeOrder() {
    // Arrange
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Assert
    assertThat(filter.getAgeOrder()).isPresent();
    assertThat(filter.getAgeOrder().get()).isEqualTo(AssetFilter.AgeOrder.DESC);
  }

  @Test
  public void withLimit_LimitGiven_ShouldSetLimit() {
    // Arrange
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withLimit(100);

    // Assert
    assertThat(filter.getLimit()).isEqualTo(100);
  }

  @Test
  public void equals_SameInstance_ShouldReturnTrue() {
    // Arrange
    NamespaceAwareAssetFilter filter = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter.equals(filter)).isTrue();
  }

  @Test
  public void equals_SameValues_ShouldReturnTrue() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isTrue();
    assertThat(filter2.equals(filter1)).isTrue();
  }

  @Test
  public void equals_DifferentNamespace_ShouldReturnFalse() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter("namespace1", SOME_ID);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter("namespace2", SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_DifferentId_ShouldReturnFalse() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, "id1");
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, "id2");

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_DifferentClass_ShouldReturnFalse() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    AssetFilter filter2 = new AssetFilter(SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_DifferentStartAge_ShouldReturnFalse() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(20, true);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_SameValuesWithFilters_ShouldReturnTrue() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Act Assert
    assertThat(filter1.equals(filter2)).isTrue();
  }

  @Test
  public void hashCode_SameValues_ShouldReturnSameHash() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
  }

  @Test
  public void hashCode_SameValuesWithFilters_ShouldReturnSameHash() {
    // Arrange
    NamespaceAwareAssetFilter filter1 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);
    NamespaceAwareAssetFilter filter2 = new NamespaceAwareAssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Act Assert
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
  }
}
