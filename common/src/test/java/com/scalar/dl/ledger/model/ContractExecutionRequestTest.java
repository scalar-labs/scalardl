package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.dl.ledger.util.Argument;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ContractExecutionRequestTest {
  private static final String NONCE = "nonce";
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  private static final String CONTRACT_ID = "TestContract";
  private static final String CONTRACT_ARGUMENT = "contract_argument";
  private static final String FUNCTION_ID = "TestFunction";
  private static final String FUNCTION_ARGUMENT = "function_argument";
  private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] AUDITOR_SIGNATURE =
      "auditor_signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WRONG_SIGNATURE = "wrongsignature".getBytes(StandardCharsets.UTF_8);
  private static final Object ARBITRARY_OBJECT = new Object();

  @Test
  public void constructor_ArgumentsGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    ENTITY_ID,
                    KEY_VERSION,
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNonceGiven_ShouldGetNonceFromContractArgument() {
    // Arrange
    String contractArgument = "{\"" + Argument.NONCE_KEY_NAME + "\":\"" + NONCE + "\"}";
    System.out.println(contractArgument);

    // Act
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            null,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            contractArgument,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Assert
    assertThat(request.getNonce()).isEqualTo(NONCE);
  }

  @Test
  public void constructor_NullEntityIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    null,
                    KEY_VERSION,
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_IllegalKeyVersionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    ENTITY_ID,
                    0, // illegal
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullContractIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    ENTITY_ID,
                    KEY_VERSION,
                    null,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullContractArgumentGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    ENTITY_ID,
                    KEY_VERSION,
                    CONTRACT_ID,
                    null,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_NullFunctionIdsGiven_ShouldGetFunctionIdsFromContractArgument() {
    // Arrange
    String contractArgument = "{\"" + Argument.FUNCTIONS_KEY + "\":[\"" + FUNCTION_ID + "\"]}";

    // Act
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            contractArgument,
            null,
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Assert
    assertThat(request.getFunctionIds()).isEqualTo(Collections.singletonList(FUNCTION_ID));
  }

  @Test
  public void constructor_NullSignatureGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () ->
                new ContractExecutionRequest(
                    NONCE,
                    ENTITY_ID,
                    KEY_VERSION,
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    null,
                    AUDITOR_SIGNATURE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentData_ShouldReturnFalse() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            WRONG_SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void get_ProperContractExecutionRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            ENTITY_ID,
            KEY_VERSION,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    String nonce = request.getNonce();
    String entityId = request.getEntityId();
    int keyVersion = request.getKeyVersion();
    String contractId = request.getContractId();
    String contractArgument = request.getContractArgument();
    List<String> functionIds = request.getFunctionIds();
    Optional<String> functionArgument = request.getFunctionArgument();
    byte[] signature = request.getSignature();
    byte[] auditorSignature = request.getAuditorSignature();

    // Assert
    assertThat(nonce).isEqualTo(NONCE);
    assertThat(entityId).isEqualTo(ENTITY_ID);
    assertThat(keyVersion).isEqualTo(KEY_VERSION);
    assertThat(contractId).isEqualTo(CONTRACT_ID);
    assertThat(contractArgument).isEqualTo(CONTRACT_ARGUMENT);
    assertThat(functionIds).isEqualTo(Collections.singletonList(FUNCTION_ID));
    assertThat(functionArgument).isEqualTo(Optional.of(FUNCTION_ARGUMENT));
    assertThat(signature).isEqualTo(SIGNATURE);
    assertThat(auditorSignature).isEqualTo(AUDITOR_SIGNATURE);
  }
}
