package com.scalar.dl.ledger.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.dl.ledger.namespace.Namespaces;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import org.junit.jupiter.api.Test;

public class ArgumentTest {

  private static final String SOME_NONCE = "test-nonce";
  private static final String SOME_NAMESPACE = "ns1";
  private static final String SOME_CONTRACT_ARG = "{\"key\":\"value\"}";
  private static final String SOME_FUNCTION_ID1 = "function1";
  private static final String SOME_FUNCTION_ID2 = "function2";
  private static final JsonArray FUNCTION_ARRAY =
      Json.createArrayBuilder().add(SOME_FUNCTION_ID1).add(SOME_FUNCTION_ID2).build();
  private static final String V1_ARGUMENT =
      Json.createObjectBuilder()
          .add("nonce", SOME_NONCE)
          .add("_functions_", FUNCTION_ARRAY)
          .add("key", "value")
          .build()
          .toString();
  private static final String V2_ARGUMENT =
      "V2\u0001" + SOME_NONCE + "\u0003" + SOME_FUNCTION_ID1 + "\u0003" + SOME_CONTRACT_ARG;

  // V1 format tests

  @Test
  public void getNonce_V1FormatGiven_ShouldReturnNonce() {
    // Arrange Act
    String nonce = Argument.getNonce(V1_ARGUMENT);

    // Assert
    assertThat(nonce).isEqualTo(SOME_NONCE);
  }

  @Test
  public void getContractArgument_V1FormatGiven_ShouldReturnOriginalArgument() {
    // Arrange Act
    String contractArg = Argument.getContractArgument(V1_ARGUMENT);

    // Assert
    assertThat(contractArg).isEqualTo(V1_ARGUMENT);
  }

  @Test
  public void getFunctionIds_V1FormatGiven_ShouldReturnFunctionIds() {
    // Arrange Act
    List<String> functionIds = Argument.getFunctionIds(V1_ARGUMENT);

    // Assert
    assertThat(functionIds).isEqualTo(Arrays.asList(SOME_FUNCTION_ID1, SOME_FUNCTION_ID2));
  }

  @Test
  public void getContextNamespace_V1FormatGiven_ShouldReturnDefault() {
    // Arrange Act
    String namespace = Argument.getContextNamespace(V1_ARGUMENT);

    // Assert
    assertThat(namespace).isEqualTo(Namespaces.DEFAULT);
  }

  // V2 format tests (parsing V2 format strings)

  @Test
  public void getNonce_V2FormatGiven_ShouldReturnNonce() {
    // Arrange Act
    String nonce = Argument.getNonce(V2_ARGUMENT);

    // Assert
    assertThat(nonce).isEqualTo(SOME_NONCE);
  }

  @Test
  public void getContractArgument_V2FormatGiven_ShouldReturnContractArgument() {
    // Arrange Act
    String contractArg = Argument.getContractArgument(V2_ARGUMENT);

    // Assert
    assertThat(contractArg).isEqualTo(SOME_CONTRACT_ARG);
  }

  @Test
  public void getFunctionIds_V2FormatGiven_ShouldReturnEmpty() {
    // Arrange Act
    List<String> functionIds = Argument.getFunctionIds(V2_ARGUMENT);

    // Assert
    assertThat(functionIds).isEmpty();
  }

  @Test
  public void getContextNamespace_V2FormatGiven_ShouldReturnDefault() {
    // Arrange Act
    String namespace = Argument.getContextNamespace(V2_ARGUMENT);

    // Assert
    assertThat(namespace).isEqualTo(Namespaces.DEFAULT);
  }

  // Deprecated format method tests (should produce V3 format)

  @Test
  public void format_DeprecatedMethodWithEmptyFunctionIdsGiven_ShouldReturnV3Format() {
    // Arrange Act
    String result = Argument.format(SOME_CONTRACT_ARG, SOME_NONCE, Collections.emptyList());

    // Assert - deprecated method now produces V3 format
    assertThat(result).startsWith("V3\u0001");
    assertThat(Argument.getNonce(result)).isEqualTo(SOME_NONCE);
    assertThat(Argument.getContextNamespace(result)).isEqualTo(Namespaces.DEFAULT);
    assertThat(Argument.getContractArgument(result)).isEqualTo(SOME_CONTRACT_ARG);
  }

  @Test
  public void format_DeprecatedMethodWithFunctionIdsGiven_ShouldReturnV3Format() {
    // Arrange Act
    String result =
        Argument.format(
            SOME_CONTRACT_ARG, SOME_NONCE, Collections.singletonList(SOME_FUNCTION_ID1));

    // Assert - deprecated method now produces V3 format
    assertThat(result).startsWith("V3\u0001");
    assertThat(Argument.getFunctionIds(result)).isEmpty();
    assertThat(Argument.getContextNamespace(result)).isEqualTo(Namespaces.DEFAULT);
  }

  // V3 format tests

  @Test
  public void format_V3WithContextNamespaceGiven_ShouldReturnV3Format() {
    // Arrange & Act
    String result =
        Argument.format(SOME_CONTRACT_ARG, SOME_NONCE, SOME_NAMESPACE, Collections.emptyList());

    // Assert
    assertThat(result).startsWith("V3\u0001");
    assertThat(Argument.getNonce(result)).isEqualTo(SOME_NONCE);
    assertThat(Argument.getContextNamespace(result)).isEqualTo(SOME_NAMESPACE);
    assertThat(Argument.getContractArgument(result)).isEqualTo(SOME_CONTRACT_ARG);
  }

  @Test
  public void format_V3WithNullContextNamespaceGiven_ShouldUseDefault() {
    // Arrange & Act
    String result = Argument.format(SOME_CONTRACT_ARG, SOME_NONCE, null, Collections.emptyList());

    // Assert
    assertThat(result).startsWith("V3\u0001");
    assertThat(Argument.getContextNamespace(result)).isEqualTo(Namespaces.DEFAULT);
  }

  @Test
  public void format_V3WithFunctionIdsGiven_ShouldReturnV3Format() {
    // Arrange & Act
    String result =
        Argument.format(
            SOME_CONTRACT_ARG,
            SOME_NONCE,
            SOME_NAMESPACE,
            Collections.singletonList(SOME_FUNCTION_ID1));

    // Assert
    assertThat(result).startsWith("V3\u0001");
    assertThat(Argument.getFunctionIds(result)).isEmpty();
    assertThat(Argument.getContextNamespace(result)).isEqualTo(SOME_NAMESPACE);
    assertThat(Argument.getContractArgument(result)).isEqualTo(SOME_CONTRACT_ARG);
  }

  @Test
  public void getNonce_V3FormatGiven_ShouldReturnNonce() {
    // Arrange
    String v3 =
        Argument.format(SOME_CONTRACT_ARG, SOME_NONCE, SOME_NAMESPACE, Collections.emptyList());

    // Act
    String nonce = Argument.getNonce(v3);

    // Assert
    assertThat(nonce).isEqualTo(SOME_NONCE);
  }

  @Test
  public void getContractArgument_V3FormatGiven_ShouldReturnLastElement() {
    // Arrange
    String v3 =
        Argument.format(SOME_CONTRACT_ARG, SOME_NONCE, SOME_NAMESPACE, Collections.emptyList());

    // Act
    String contractArg = Argument.getContractArgument(v3);

    // Assert
    assertThat(contractArg).isEqualTo(SOME_CONTRACT_ARG);
  }

  // Error cases

  @Test
  public void getVersion_InvalidVersionGiven_ShouldThrowException() {
    // Arrange
    String invalidArg = "V9\u0001nonce\u0003arg";

    // Act & Assert
    assertThatThrownBy(() -> Argument.getNonce(invalidArg))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void getVersion_MalformedArgumentGiven_ShouldThrowException() {
    // Arrange
    String malformed = "Vnonce"; // missing separator

    // Act & Assert
    assertThatThrownBy(() -> Argument.getNonce(malformed))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
