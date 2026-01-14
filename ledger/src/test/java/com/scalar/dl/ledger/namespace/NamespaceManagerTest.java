package com.scalar.dl.ledger.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.NamespaceRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.LedgerException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NamespaceManagerTest {
  @Mock private NamespaceRegistry registry;
  private NamespaceManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    manager = new NamespaceManager(registry);
  }

  @Test
  public void create_ValidNamespaceNameGiven_ShouldCreateNamespace() {
    // Arrange
    String namespace = "valid_namespace";

    // Act
    assertThatCode(() -> manager.create(namespace)).doesNotThrowAnyException();

    // Assert
    verify(registry).create(namespace);
  }

  @Test
  public void create_ValidNamespaceWithAlphanumericAndUnderscore_ShouldCreateNamespace() {
    // Arrange
    String namespace = "a123_ABC";

    // Act
    assertThatCode(() -> manager.create(namespace)).doesNotThrowAnyException();

    // Assert
    verify(registry).create(namespace);
  }

  @Test
  public void create_ValidNamespaceWithMinimumLength_ShouldCreateNamespace() {
    // Arrange
    String namespace = "a";

    // Act
    assertThatCode(() -> manager.create(namespace)).doesNotThrowAnyException();

    // Assert
    verify(registry).create(namespace);
  }

  @Test
  public void create_NamespaceStartingWithNumber_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "1invalid";

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void create_NamespaceWithHyphen_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "invalid-name";

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void create_NamespaceWithSpecialCharacter_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "invalid@name";

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void create_EmptyNamespace_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "";

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void create_NamespaceStartingWithUnderscore_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "_invalid";

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void create_DefaultNamespaceGiven_ShouldThrowLedgerException() {
    // Arrange
    String namespace = NamespaceManager.DEFAULT_NAMESPACE;

    // Act Assert
    assertThatThrownBy(() -> manager.create(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.RESERVED_NAMESPACE.buildMessage(namespace));

    verify(registry, never()).create(namespace);
  }

  @Test
  public void scan_EmptyFilterGiven_ShouldReturnAllNamespacesWithDefault() {
    // Arrange
    List<String> namespaces = new ArrayList<>();
    namespaces.add("ns1");
    namespaces.add("ns2");
    namespaces.add("ns3");
    when(registry.scan("")).thenReturn(namespaces);

    // Act
    List<String> result = manager.scan("");

    // Assert
    verify(registry).scan("");
    // "default" should be added because "default".contains("") is true
    // "default" comes before "ns1" alphabetically
    assertThat(result).containsExactly("default", "ns1", "ns2", "ns3");
  }

  @Test
  public void scan_PatternNotMatchingDefault_ShouldReturnOnlyMatchingNamespaces() {
    // Arrange
    List<String> namespaces = new ArrayList<>();
    namespaces.add("ns2");
    when(registry.scan("ns2")).thenReturn(namespaces);

    // Act
    List<String> result = manager.scan("ns2");

    // Assert
    verify(registry).scan("ns2");
    // "default".contains("ns2") is false, so "default" should not be added
    assertThat(result).containsExactly("ns2");
  }

  @Test
  public void scan_NonMatchingPatternGiven_ShouldReturnEmptyList() {
    // Arrange
    when(registry.scan("non_existent")).thenReturn(new ArrayList<>());

    // Act
    List<String> result = manager.scan("non_existent");

    // Assert
    verify(registry).scan("non_existent");
    // "default".contains("non_existent") is false, so result is empty
    assertThat(result).isEmpty();
  }

  @Test
  public void scan_EmptyRegistryAndEmptyFilter_ShouldReturnOnlyDefault() {
    // Arrange
    when(registry.scan("")).thenReturn(new ArrayList<>());

    // Act
    List<String> result = manager.scan("");

    // Assert
    verify(registry).scan("");
    // "default".contains("") is true, so "default" should be added even for empty registry
    assertThat(result).containsExactly("default");
  }

  @Test
  public void scan_PatternMatchingDefault_ShouldIncludeDefaultInSortedPosition() {
    // Arrange
    List<String> namespaces = new ArrayList<>();
    namespaces.add("abc");
    namespaces.add("zzz");
    when(registry.scan("e")).thenReturn(namespaces);

    // Act
    List<String> result = manager.scan("e");

    // Assert
    verify(registry).scan("e");
    // "default".contains("e") is true, so "default" should be inserted in sorted position
    // "abc" < "default" < "zzz"
    assertThat(result).containsExactly("abc", "default", "zzz");
  }

  @Test
  public void scan_PatternMatchingDefaultExactly_ShouldIncludeDefault() {
    // Arrange
    when(registry.scan("default")).thenReturn(new ArrayList<>());

    // Act
    List<String> result = manager.scan("default");

    // Assert
    verify(registry).scan("default");
    // "default".contains("default") is true
    assertThat(result).containsExactly("default");
  }

  @Test
  public void scan_DefaultAlreadyInList_ShouldNotDuplicate() {
    // Arrange
    List<String> namespaces = new ArrayList<>();
    namespaces.add("abc");
    namespaces.add("default");
    namespaces.add("zzz");
    when(registry.scan("")).thenReturn(namespaces);

    // Act
    List<String> result = manager.scan("");

    // Assert
    verify(registry).scan("");
    // "default" is already in the list, so should not be duplicated
    assertThat(result).containsExactly("abc", "default", "zzz");
  }

  @Test
  public void drop_ValidNamespaceNameGiven_ShouldDropNamespace() {
    // Arrange
    String namespace = "valid_namespace";

    // Act
    assertThatCode(() -> manager.drop(namespace)).doesNotThrowAnyException();

    // Assert
    verify(registry).drop(namespace);
  }

  @Test
  public void drop_InvalidNamespaceName_ShouldThrowLedgerException() {
    // Arrange
    String namespace = "1invalid";

    // Act Assert
    assertThatThrownBy(() -> manager.drop(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.INVALID_NAMESPACE_NAME.buildMessage(namespace));

    verify(registry, never()).drop(namespace);
  }

  @Test
  public void drop_DefaultNamespaceGiven_ShouldThrowLedgerException() {
    // Arrange
    String namespace = NamespaceManager.DEFAULT_NAMESPACE;

    // Act Assert
    assertThatThrownBy(() -> manager.drop(namespace))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.RESERVED_NAMESPACE.buildMessage(namespace));

    verify(registry, never()).drop(namespace);
  }
}
