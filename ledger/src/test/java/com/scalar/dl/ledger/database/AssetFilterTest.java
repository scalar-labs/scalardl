package com.scalar.dl.ledger.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class AssetFilterTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_ID = "some_id";

  @Test
  public void constructor_IdGiven_ShouldCreateFilterWithoutNamespace() {
    // Arrange Act
    AssetFilter filter = new AssetFilter(SOME_ID);

    // Assert
    assertThat(filter.getNamespace()).isEmpty();
    assertThat(filter.getId()).isEqualTo(SOME_ID);
  }

  @Test
  public void constructor_NamespaceAndIdGiven_ShouldCreateFilter() {
    // Arrange Act
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Assert
    assertThat(filter.getNamespace()).isPresent();
    assertThat(filter.getNamespace().get()).isEqualTo(SOME_NAMESPACE);
    assertThat(filter.getId()).isEqualTo(SOME_ID);
  }

  @Test
  public void withStartAge_AgeGiven_ShouldSetStartAge() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

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
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

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
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Assert
    assertThat(filter.getAgeOrder()).isPresent();
    assertThat(filter.getAgeOrder().get()).isEqualTo(AssetFilter.AgeOrder.DESC);
  }

  @Test
  public void withLimit_LimitGiven_ShouldSetLimit() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act
    filter.withLimit(100);

    // Assert
    assertThat(filter.getLimit()).isEqualTo(100);
  }

  @Test
  public void equals_SameInstance_ShouldReturnTrue() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter.equals(filter)).isTrue();
  }

  @Test
  public void equals_SameValues_ShouldReturnTrue() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isTrue();
    assertThat(filter2.equals(filter1)).isTrue();
  }

  @Test
  public void equals_DifferentNamespace_ShouldReturnFalse() {
    // Arrange
    AssetFilter filter1 = new AssetFilter("namespace1", SOME_ID);
    AssetFilter filter2 = new AssetFilter("namespace2", SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_OneWithNamespaceOneWithout_ShouldReturnFalse() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    AssetFilter filter2 = new AssetFilter(SOME_ID);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_DifferentId_ShouldReturnFalse() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, "id1");
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, "id2");

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_DifferentStartAge_ShouldReturnFalse() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true);
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(20, true);

    // Act Assert
    assertThat(filter1.equals(filter2)).isFalse();
  }

  @Test
  public void equals_SameValuesWithFilters_ShouldReturnTrue() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Act Assert
    assertThat(filter1.equals(filter2)).isTrue();
  }

  @Test
  public void hashCode_SameValues_ShouldReturnSameHash() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, SOME_ID);

    // Act Assert
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
  }

  @Test
  public void hashCode_SameValuesWithFilters_ShouldReturnSameHash() {
    // Arrange
    AssetFilter filter1 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter1.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);
    AssetFilter filter2 = new AssetFilter(SOME_NAMESPACE, SOME_ID);
    filter2.withStartAge(10, true).withEndAge(20, false).withAgeOrder(AssetFilter.AgeOrder.DESC);

    // Act Assert
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
  }
}
