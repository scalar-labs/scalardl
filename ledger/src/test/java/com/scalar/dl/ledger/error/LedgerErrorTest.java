package com.scalar.dl.ledger.error;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class LedgerErrorTest {

  @Test
  public void checkDuplicateErrorCode() {
    Assertions.assertThat(Arrays.stream(LedgerError.values()).map(LedgerError::buildCode))
        .doesNotHaveDuplicates();
  }

  @Test
  public void buildCode_ShouldBuildCorrectCode() {
    // Arrange
    LedgerError error = LedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED;

    // Act
    String code = error.buildCode();

    // Assert
    Assertions.assertThat(code).isEqualTo("DL-LEDGER-400001");
  }

  @Test
  public void buildMessage_ShouldBuildCorrectMessage() {
    // Arrange
    LedgerError error = LedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED;

    // Act
    String message = error.buildMessage();

    // Assert
    Assertions.assertThat(message)
        .isEqualTo("DL-LEDGER-400001: The request signature can't be validated.");
  }
}
