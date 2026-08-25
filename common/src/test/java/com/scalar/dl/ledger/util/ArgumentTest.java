package com.scalar.dl.ledger.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.json.Json;
import org.junit.jupiter.api.Test;

public class ArgumentTest {

  @Test
  public void getNonce_LegacyFormatWithoutNonceKeyGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    String argument = Json.createObjectBuilder().add("key", "value").build().toString();

    // Act Assert
    assertThatThrownBy(() -> Argument.getNonce(argument))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DL-COMMON-414017");
  }
}
