package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.config.ServerConfig;
import com.scalar.dl.ledger.namespace.NamespaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarNamespaceResolverTest {
  private static final String BASE_NAMESPACE = "scalar";
  private static final String LOGICAL_NAMESPACE_FOO = "foo";

  @Mock private ServerConfig serverConfig;

  private ScalarNamespaceResolver resolver;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(serverConfig.getNamespace()).thenReturn(BASE_NAMESPACE);
    resolver = new ScalarNamespaceResolver(serverConfig);
  }

  @Test
  public void resolve_DefaultNamespaceGiven_ShouldReturnBaseNamespace() {
    // Act
    String actual = resolver.resolve(NamespaceManager.DEFAULT_NAMESPACE);

    // Assert
    assertThat(actual).isEqualTo(BASE_NAMESPACE);
  }

  @Test
  public void resolve_LogicalNamespaceFooGiven_ShouldReturnBaseNamespaceWithSeparatorAndFoo() {
    // Act
    String actual = resolver.resolve(LOGICAL_NAMESPACE_FOO);

    // Assert
    assertThat(actual).isEqualTo(BASE_NAMESPACE + "_" + LOGICAL_NAMESPACE_FOO);
  }
}
