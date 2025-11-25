package com.scalar.dl.ledger.namespace;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.scalar.dl.ledger.database.NamespaceRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.LedgerException;
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
}
