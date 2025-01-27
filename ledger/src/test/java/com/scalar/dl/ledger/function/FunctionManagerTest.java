package com.scalar.dl.ledger.function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.FunctionRegistry;
import com.scalar.dl.ledger.exception.MissingFunctionException;
import com.scalar.dl.ledger.exception.UnloadableFunctionException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FunctionManagerTest {
  private static final String ANY_FUNCTION_ID = "Test";
  @Mock private FunctionEntry entry;
  @Mock private FunctionRegistry registry;
  @Mock private FunctionLoader loader;
  private FunctionManager manager;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    manager = spy(new FunctionManager(registry, loader));
  }

  @Test
  public void register_FunctionEntryGiven_ShouldBind() {
    // Arrange

    // Act
    manager.register(entry);

    // Assert
    verify(registry).bind(entry);
  }

  @Test
  public void get_ExistingFunctionIdGiven_ShouldReturnEntry() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.of(entry));

    // Act
    FunctionEntry actual = manager.get(ANY_FUNCTION_ID);

    // Assert
    verify(registry).lookup(ANY_FUNCTION_ID);
    assertThat(actual).isEqualTo(entry);
  }

  @Test
  public void get_UnexistingFunctionIdGiven_ShouldThrowMissingFunctionException() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.empty());

    // Act
    assertThatThrownBy(() -> manager.get(ANY_FUNCTION_ID))
        .isInstanceOf(MissingFunctionException.class);

    // Assert
    verify(registry).lookup(ANY_FUNCTION_ID);
  }

  @Test
  public void getInstance_ExistingFunctionIdGiven_ShouldReturnFunction() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.of(entry));
    Class<? extends Function> clazz = TestFunction.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    FunctionMachine function = manager.getInstance(ANY_FUNCTION_ID);

    // Assert
    verify(loader).defineClass(entry);
    assertThat(function.getFunctionBase()).isInstanceOf(DeprecatedFunction.class);
  }

  @Test
  public void getInstance_ExistingJsonpBasedFunctionIdGiven_ShouldReturnFunction() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.of(entry));
    doReturn(TestJsonpBasedFunction.class).when(loader).defineClass(entry);

    // Act
    FunctionMachine function = manager.getInstance(ANY_FUNCTION_ID);

    // Assert
    verify(loader).defineClass(entry);
    assertThat(function.getFunctionBase()).isInstanceOf(JsonpBasedFunction.class);
  }

  @Test
  public void getInstance_ExistingJacksonBasedFunctionIdGiven_ShouldReturnFunction() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.of(entry));
    doReturn(TestJacksonBasedFunction.class).when(loader).defineClass(entry);

    // Act
    FunctionMachine function = manager.getInstance(ANY_FUNCTION_ID);

    // Assert
    verify(loader).defineClass(entry);
    assertThat(function.getFunctionBase()).isInstanceOf(JacksonBasedFunction.class);
  }

  @Test
  public void getInstance_ExistingStringBasedFunctionIdGiven_ShouldReturnFunction() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.of(entry));
    doReturn(TestStringBasedFunction.class).when(loader).defineClass(entry);

    // Act
    FunctionMachine function = manager.getInstance(ANY_FUNCTION_ID);

    // Assert
    verify(loader).defineClass(entry);
    assertThat(function.getFunctionBase()).isInstanceOf(StringBasedFunction.class);
  }

  @Test
  public void getInstance_NonexistingFunctionIdGiven_ShouldThrowUnloadableFunctionException() {
    // Arrange
    when(registry.lookup(ANY_FUNCTION_ID)).thenReturn(Optional.empty());

    // Act
    assertThatThrownBy(() -> manager.getInstance(ANY_FUNCTION_ID))
        .isInstanceOf(UnloadableFunctionException.class)
        .hasCauseExactlyInstanceOf(MissingFunctionException.class);

    // Assert
    verify(manager).findClass(ANY_FUNCTION_ID);
  }
}
