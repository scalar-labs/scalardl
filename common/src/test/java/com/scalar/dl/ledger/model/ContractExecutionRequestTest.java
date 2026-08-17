package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.Argument;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ContractExecutionRequestTest {
  private static final String NONCE = "550e8400-e29b-41d4-a716-446655440000";
  private static final String CONTEXT_NAMESPACE = "test_namespace";
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
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
                    SIGNATURE,
                    AUDITOR_SIGNATURE))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_NullNonceGiven_ShouldGetNonceFromContractArgument() {
    // Arrange
    String contractArgument = "{\"" + Argument.NONCE_KEY_NAME + "\":\"" + NONCE + "\"}";

    // Act
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            null,
            CONTRACT_ID,
            contractArgument,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
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
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    null,
                    KEY_VERSION,
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
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    0, // illegal
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
                    null,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
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
                    CONTRACT_ID,
                    null,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
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
            CONTRACT_ID,
            contractArgument,
            null,
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
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
                    CONTRACT_ID,
                    CONTRACT_ARGUMENT,
                    Collections.singletonList(FUNCTION_ID),
                    FUNCTION_ARGUMENT,
                    CONTEXT_NAMESPACE,
                    ENTITY_ID,
                    KEY_VERSION,
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
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
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
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
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
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(ARBITRARY_OBJECT);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentSignature_ShouldReturnFalse() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            WRONG_SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentContextNamespace_ShouldReturnFalse() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);
    ContractExecutionRequest other =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            "different_namespace",
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  private static ContractExecutionRequest buildRequest(String nonce, String contractArgument) {
    return new ContractExecutionRequest(
        nonce,
        CONTRACT_ID,
        contractArgument,
        Collections.singletonList(FUNCTION_ID),
        FUNCTION_ARGUMENT,
        CONTEXT_NAMESPACE,
        ENTITY_ID,
        KEY_VERSION,
        SIGNATURE,
        AUDITOR_SIGNATURE);
  }

  @Test
  public void constructor_LowercaseCanonicalUuidNonceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(() -> buildRequest("550e8400-e29b-41d4-a716-446655440000", CONTRACT_ARGUMENT))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_UppercaseCanonicalUuidNonceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(() -> buildRequest("550E8400-E29B-41D4-A716-446655440000", CONTRACT_ARGUMENT))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_MixedCaseCanonicalUuidNonceGiven_ShouldInstantiate() {
    // Arrange

    // Act Assert
    assertThatCode(() -> buildRequest("550e8400-E29B-41d4-A716-446655440000", CONTRACT_ARGUMENT))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_CommaContainingNonceGiven_ShouldThrowLedgerException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> buildRequest("abc,def", CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void
      constructor_EmptyNonceAndEmptyNonceInContractArgumentGiven_ShouldThrowLedgerException() {
    // Arrange
    String contractArgument = "{\"" + Argument.NONCE_KEY_NAME + "\":\"\"}";

    // Act Assert
    assertThatThrownBy(() -> buildRequest("", contractArgument))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NonCanonicalUuidNonceGiven_ShouldThrowLedgerException() {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> buildRequest("1-1-1-1-1", CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_SeparatorContainingNonceOf36CharsGiven_ShouldThrowLedgerException() {
    // Arrange
    // 36 characters, but contains the nonce separator character (U+0001)
    String nonce = "550e8400-e29b-41d4-a716-44665544000\u0001";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_TooShortNearUuidNonceGiven_ShouldThrowLedgerException() {
    // Arrange
    // 35 characters
    String nonce = "550e8400-e29b-41d4-a716-44665544000";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_TooLongNearUuidNonceGiven_ShouldThrowLedgerException() {
    // Arrange
    // 37 characters
    String nonce = "550e8400-e29b-41d4-a716-4466554400000";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NonHexLetterContainingNonceGiven_ShouldThrowLedgerException() {
    // Arrange
    // 36 characters, but contains a non-hex letter 'g'
    String nonce = "g50e8400-e29b-41d4-a716-446655440000";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NullNonceAndV2ArgumentWithEmptyNonceGiven_ShouldThrowLedgerException() {
    // Arrange
    String contractArgument = "V2" + FUNCTION_ID + "" + CONTRACT_ARGUMENT;

    // Act Assert
    assertThatThrownBy(() -> buildRequest(null, contractArgument))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NullNonceAndV1ArgumentWithEmptyNonceGiven_ShouldThrowLedgerException() {
    // Arrange
    String contractArgument = "{\"" + Argument.NONCE_KEY_NAME + "\":\"\"}";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(null, contractArgument))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NullNonceAndV1ArgumentWithoutNonceKeyGiven_ShouldThrowLedgerException() {
    // Arrange
    String contractArgument = "{\"key\":\"value\"}";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(null, contractArgument))
        .isInstanceOf(LedgerException.class)
        .satisfies(
            e -> assertThat(((LedgerException) e).getCode()).isEqualTo(StatusCode.INVALID_ARGUMENT))
        .hasMessageContaining("DL-COMMON-414022");
  }

  @Test
  public void constructor_NullNonceAndV1ArgumentWithUuidNonceGiven_ShouldGetNonceFromArgument() {
    // Arrange
    String contractArgument = "{\"" + Argument.NONCE_KEY_NAME + "\":\"" + NONCE + "\"}";

    // Act
    ContractExecutionRequest request = buildRequest(null, contractArgument);

    // Assert
    assertThat(request.getNonce()).isEqualTo(NONCE);
  }

  @Test
  public void constructor_NullNonceAndV2ArgumentWithUuidNonceGiven_ShouldGetNonceFromArgument() {
    // Arrange
    String contractArgument = "V2" + NONCE + "" + FUNCTION_ID + "" + CONTRACT_ARGUMENT;

    // Act
    ContractExecutionRequest request = buildRequest(null, contractArgument);

    // Assert
    assertThat(request.getNonce()).isEqualTo(NONCE);
  }

  @Test
  public void constructor_NullNonceAndV3ArgumentWithUuidNonceGiven_ShouldGetNonceFromArgument() {
    // Arrange
    String contractArgument =
        Argument.format(
            CONTRACT_ARGUMENT, NONCE, CONTEXT_NAMESPACE, Collections.singletonList(FUNCTION_ID));

    // Act
    ContractExecutionRequest request = buildRequest(null, contractArgument);

    // Assert
    assertThat(request.getNonce()).isEqualTo(NONCE);
  }

  @Test
  public void constructor_ControlCharacterContainingNonceGiven_ShouldSanitizeExceptionMessage() {
    // Arrange
    String nonce = "abc\ndef";

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .hasMessageContaining("DL-COMMON-414022")
        .hasMessageContaining("abc?def")
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain("\n"));
  }

  @Test
  public void constructor_VeryLongNonceGiven_ShouldTruncateNonceInExceptionMessage() {
    // Arrange
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      builder.append('a');
    }
    String nonce = builder.toString();

    // Act Assert
    assertThatThrownBy(() -> buildRequest(nonce, CONTRACT_ARGUMENT))
        .isInstanceOf(LedgerException.class)
        .hasMessageContaining("DL-COMMON-414022")
        .hasMessageContaining("(1000 chars)")
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain(nonce));
  }

  @Test
  public void get_ProperContractExecutionRequestGiven_ShouldReturnWhatsSet() {
    // Arrange
    ContractExecutionRequest request =
        new ContractExecutionRequest(
            NONCE,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            Collections.singletonList(FUNCTION_ID),
            FUNCTION_ARGUMENT,
            CONTEXT_NAMESPACE,
            ENTITY_ID,
            KEY_VERSION,
            SIGNATURE,
            AUDITOR_SIGNATURE);

    // Act
    String nonce = request.getNonce();
    String contextNamespace = request.getContextNamespace();
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
    assertThat(contextNamespace).isEqualTo(CONTEXT_NAMESPACE);
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
