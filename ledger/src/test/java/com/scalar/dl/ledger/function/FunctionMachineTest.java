package com.scalar.dl.ledger.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FunctionMachineTest {
  @Mock private Database<Get, Scan, Put, Delete, Result> database;
  @Mock private FunctionManager functionManager;
  private FunctionMachine machine;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void constructor_FunctionGiven_ShouldCreateDeprecatedFunction() {
    // Arrange

    // Act
    machine = new FunctionMachine(new TestFunction());

    // Assert
    assertThat(machine.getFunctionBase()).isInstanceOf(DeprecatedFunction.class);
  }

  @Test
  public void constructor_JsonpBasedFunctionGiven_ShouldKeepGivenFunction() {
    // Arrange

    // Act
    machine = new FunctionMachine(new TestJsonpBasedFunction());

    // Assert
    assertThat(machine.getFunctionBase()).isInstanceOf(JsonpBasedFunction.class);
  }

  @Test
  public void constructor_StringBasedFunctionGiven_ShouldKeepGivenFunction() {
    // Arrange

    // Act
    machine = new FunctionMachine(new TestStringBasedFunction());

    // Assert
    assertThat(machine.getFunctionBase()).isInstanceOf(StringBasedFunction.class);
  }

  @Test
  public void initialize_FunctionBaseGiven_ShouldCallFunctionBaseInitialize() {
    // Arrange
    TestStringBasedFunction function = mock(TestStringBasedFunction.class);
    machine = new FunctionMachine(function);

    // Act
    machine.initialize(functionManager);

    // Assert
    verify(function).initialize(functionManager);
  }

  @Test
  public void setRoot_FunctionBaseGiven_ShouldCallFunctionBaseSetRoot() {
    // Arrange
    TestStringBasedFunction function = mock(TestStringBasedFunction.class);
    machine = new FunctionMachine(function);

    // Act
    machine.setRoot(true);

    // Assert
    verify(function).setRoot(true);
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void isRoot_FunctionBaseGiven_ShouldCallFunctionBaseIsRoot() {
    // Arrange
    TestStringBasedFunction function = mock(TestStringBasedFunction.class);
    machine = new FunctionMachine(function);

    // Act
    machine.isRoot();

    // Assert
    verify(function).isRoot();
  }

  @Test
  public void invoke_FunctionGiven_ShouldInvokeProperly() {
    // Arrange
    TestFunction function = spy(TestFunction.class);
    doNothing().when(function).invoke(any(), any(), any(), any());
    machine = new FunctionMachine(function);
    String argument = JsonObject.EMPTY_JSON_OBJECT.toString();

    // Act
    String result = machine.invoke(database, null, argument, null);

    // Assert
    verify(function)
        .invoke(database, Optional.empty(), JsonObject.EMPTY_JSON_OBJECT, Optional.empty());
    assertThat(result).isNull();
  }

  @Test
  public void invoke_JsonpBasedFunctionGiven_ShouldInvokeProperly() {
    // Arrange
    TestJsonpBasedFunction function = spy(TestJsonpBasedFunction.class);
    when(function.invoke(any(Database.class), any(), any(), any()))
        .thenReturn(JsonObject.EMPTY_JSON_OBJECT);
    machine = new FunctionMachine(function);
    String argument = JsonObject.EMPTY_JSON_OBJECT.toString();

    // Act
    String result = machine.invoke(database, null, argument, null);

    // Assert
    verify(function).invoke(database, null, JsonValue.EMPTY_JSON_OBJECT, null);
    assertThat(result).isEqualTo(JsonObject.EMPTY_JSON_OBJECT.toString());
  }

  @Test
  public void invoke_StringBasedFunctionGiven_ShouldInvokeProperly() {
    // Arrange
    TestStringBasedFunction function = spy(TestStringBasedFunction.class);
    when(function.invoke(any(Database.class), any(), any(), any())).thenReturn("result");
    machine = new FunctionMachine(function);
    String argument = "x,y";

    // Act
    String result = machine.invoke(database, null, argument, null);

    // Assert
    verify(function).invoke(database, null, argument, null);
    assertThat(result).isEqualTo("result");
  }
}
