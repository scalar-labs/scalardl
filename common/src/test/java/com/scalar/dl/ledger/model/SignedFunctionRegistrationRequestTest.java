package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class SignedFunctionRegistrationRequestTest {
  private static final String FUNCTION_ID = "function_id";
  private static final String FUNCTION_BINARY_NAME = "com.example.Function";
  private static final byte[] FUNCTION_BYTE_CODE = "bytecode".getBytes(StandardCharsets.UTF_8);
  private static final String CONTEXT_NAMESPACE = "test_namespace";
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);

  @Test
  public void constructor_ProperValuesGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(
            () ->
                new SignedFunctionRegistrationRequest(
                    FUNCTION_ID,
                    FUNCTION_BINARY_NAME,
                    FUNCTION_BYTE_CODE,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
                    SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullContextNamespaceGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(
            () ->
                new SignedFunctionRegistrationRequest(
                    FUNCTION_ID,
                    FUNCTION_BINARY_NAME,
                    FUNCTION_BYTE_CODE,
                    null,
                    ENTITY_ID,
                    KEY_VERSION,
                    SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    SignedFunctionRegistrationRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    SignedFunctionRegistrationRequest other =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    Object arbitraryObject = new Object();

    // Act
    boolean result = request.equals(arbitraryObject);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentFunctionId_ShouldReturnFalse() {
    // Arrange
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    SignedFunctionRegistrationRequest other =
        new SignedFunctionRegistrationRequest(
            "different_function_id",
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentContextNamespace_ShouldReturnFalse() {
    // Arrange
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);
    SignedFunctionRegistrationRequest other =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            "different_namespace",
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void getters_ProperValuesGiven_ShouldReturnProperValues() {
    // Act
    SignedFunctionRegistrationRequest request =
        new SignedFunctionRegistrationRequest(
            FUNCTION_ID,
            FUNCTION_BINARY_NAME,
            FUNCTION_BYTE_CODE,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE);

    // Assert
    assertThat(request.getFunctionId()).isEqualTo(FUNCTION_ID);
    assertThat(request.getFunctionBinaryName()).isEqualTo(FUNCTION_BINARY_NAME);
    assertThat(request.getFunctionByteCode()).isEqualTo(FUNCTION_BYTE_CODE);
    assertThat(request.getContextNamespace()).isEqualTo(CONTEXT_NAMESPACE);
    assertThat(request.getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(request.getKeyVersion()).isEqualTo(KEY_VERSION);
    assertThat(request.getSignature()).isEqualTo(SIGNATURE);
  }
}
