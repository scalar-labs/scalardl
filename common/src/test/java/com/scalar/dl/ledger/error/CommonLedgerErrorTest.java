package com.scalar.dl.ledger.error;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonLedgerErrorTest {

  @Test
  public void checkDuplicateErrorCode() {
    Assertions.assertThat(
            Arrays.stream(CommonLedgerError.values()).map(CommonLedgerError::buildCode))
        .doesNotHaveDuplicates();
  }

  @Test
  public void buildCode_ShouldBuildCorrectCode() {
    // Arrange
    CommonLedgerError error = CommonLedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED;

    // Act
    String code = error.buildCode();

    // Assert
    Assertions.assertThat(code).isEqualTo("DL-LEDGER-400001");
  }

  @Test
  public void buildMessage_ShouldBuildCorrectMessage() {
    // Arrange
    CommonLedgerError error = CommonLedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED;

    // Act
    String message = error.buildMessage();

    // Assert
    Assertions.assertThat(message)
        .isEqualTo("DL-LEDGER-400001: The request signature can't be validated.");
  }
}
