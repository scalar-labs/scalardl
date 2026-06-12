package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.exception.SignatureException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ExecutionFinishRequestTest {
  private static final String NONCE = "nonce";
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WRONG_SIGNATURE = "wrong_signature".getBytes(StandardCharsets.UTF_8);
  private static final Object ARBITRARY_OBJECT = new Object();

  @Test
  public void constructor_ArgumentsGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(() -> new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullEntityIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> new ExecutionFinishRequest(NONCE, null, KEY_VERSION, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NonPositiveKeyVersionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> new ExecutionFinishRequest(NONCE, ENTITY_ID, 0, SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_ProperRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    String nonce = request.getNonce();
    String entityId = request.getEntityId();
    int keyVersion = request.getKeyVersion();
    byte[] signature = request.getSignature();

    // Assert
    assertThat(nonce).isEqualTo(NONCE);
    assertThat(entityId).isEqualTo(ENTITY_ID);
    assertThat(keyVersion).isEqualTo(KEY_VERSION);
    assertThat(signature).isEqualTo(SIGNATURE);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentNonce_ShouldReturnFalse() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other =
        new ExecutionFinishRequest("different_nonce", ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentEntityId_ShouldReturnFalse() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other =
        new ExecutionFinishRequest(NONCE, "different_entity_id", KEY_VERSION, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentKeyVersion_ShouldReturnFalse() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other = new ExecutionFinishRequest(NONCE, ENTITY_ID, 99, SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSignature_ShouldReturnFalse() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, WRONG_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void hashCode_OnTheSameData_ShouldReturnSameHashCode() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    ExecutionFinishRequest other =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);

    // Act
    int hash1 = request.hashCode();
    int hash2 = other.hashCode();

    // Assert
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void validateWith_ValidSignatureGiven_ShouldNotThrowAnyException() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    SignatureValidator validator = mock(SignatureValidator.class);
    when(validator.validate(any(byte[].class), any(byte[].class))).thenReturn(true);

    // Act Assert
    assertThatCode(() -> request.validateWith(validator)).doesNotThrowAnyException();

    byte[] expected = ExecutionFinishRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);
    verify(validator).validate(expected, SIGNATURE);
  }

  @Test
  public void validateWith_InvalidSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    ExecutionFinishRequest request =
        new ExecutionFinishRequest(NONCE, ENTITY_ID, KEY_VERSION, SIGNATURE);
    SignatureValidator validator = mock(SignatureValidator.class);
    when(validator.validate(any(byte[].class), any(byte[].class))).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> request.validateWith(validator))
        .isInstanceOf(SignatureException.class);
  }

  @Test
  public void serialize_ArgumentsGiven_ShouldReturnCorrectBytes() {
    // Arrange

    // Act
    byte[] serialized = ExecutionFinishRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isNotNull();
    assertThat(serialized.length).isGreaterThan(0);
  }

  @Test
  public void serialize_WithDifferentNonce_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] serialized1 = ExecutionFinishRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);
    byte[] serialized2 =
        ExecutionFinishRequest.serialize("different_nonce", ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized1).isNotEqualTo(serialized2);
  }

  @Test
  public void serialize_ArgumentsGiven_ShouldBePrefixedWithRequestTypeTag() {
    // Arrange
    byte[] tag = "finish".getBytes(StandardCharsets.UTF_8);
    byte[] nonce = NONCE.getBytes(StandardCharsets.UTF_8);
    byte[] entityId = ENTITY_ID.getBytes(StandardCharsets.UTF_8);
    byte[] expected =
        ByteBuffer.allocate(tag.length + nonce.length + entityId.length + Integer.BYTES)
            .put(tag)
            .put(nonce)
            .put(entityId)
            .putInt(KEY_VERSION)
            .array();

    // Act
    byte[] serialized = ExecutionFinishRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);

    // Assert
    assertThat(serialized).isEqualTo(expected);
  }

  @Test
  public void serialize_SameValuesAsAbortRequest_ShouldReturnDifferentBytes() {
    // Arrange

    // Act
    byte[] finish = ExecutionFinishRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);
    byte[] abort = ExecutionAbortRequest.serialize(NONCE, ENTITY_ID, KEY_VERSION);

    // Assert
    // The domain separator ensures a captured abort signature cannot be reused as a finish
    // signature (and vice versa) even when the signed values are identical.
    assertThat(finish).isNotEqualTo(abort);
  }
}
