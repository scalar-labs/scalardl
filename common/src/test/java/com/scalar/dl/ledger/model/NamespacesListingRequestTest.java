package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

public class NamespacesListingRequestTest {
  private static final String NAMESPACE = "test_namespace";

  @Test
  public void constructor_ProperValueGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(() -> new NamespacesListingRequest(NAMESPACE)).doesNotThrowAnyException();
  }

  @Test
  public void getNamespace_ProperValueGiven_ShouldReturnProperValue() {
    // Act
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);

    // Assert
    assertThat(request.getPattern()).isEqualTo(NAMESPACE);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    NamespacesListingRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    NamespacesListingRequest other = new NamespacesListingRequest(NAMESPACE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnDifferentData_ShouldReturnFalse() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    String differentNamespace = "different_namespace";
    NamespacesListingRequest other = new NamespacesListingRequest(differentNamespace);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnNull_ShouldReturnFalse() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);

    // Act
    boolean result = request.equals(null);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    Object arbitraryObject = new Object();

    // Act
    boolean result = request.equals(arbitraryObject);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    NamespacesListingRequest other = new NamespacesListingRequest(NAMESPACE);

    // Act
    int hashCode1 = request.hashCode();
    int hashCode2 = other.hashCode();

    // Assert
    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  public void hashCode_OnDifferentData_ShouldReturnDifferentHashCode() {
    // Arrange
    NamespacesListingRequest request = new NamespacesListingRequest(NAMESPACE);
    String differentNamespace = "different_namespace";
    NamespacesListingRequest other = new NamespacesListingRequest(differentNamespace);

    // Act
    int hashCode1 = request.hashCode();
    int hashCode2 = other.hashCode();

    // Assert
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }
}
