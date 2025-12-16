package com.scalar.dl.ledger.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ContextTest {
  private static final String SOME_NAMESPACE = "some_namespace";

  @Test
  public void withNamespace_NamespaceGiven_ShouldCreateContext() {
    // Arrange Act
    Context context = Context.withNamespace(SOME_NAMESPACE);

    // Assert
    assertThat(context.getNamespace()).isEqualTo(SOME_NAMESPACE);
  }

  @Test
  public void withNamespace_NullGiven_ShouldThrowNullPointerException() {
    // Arrange Act Assert
    assertThatThrownBy(() -> Context.withNamespace(null)).isInstanceOf(NullPointerException.class);
  }
}
