package com.scalar.dl.ledger.error;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonErrorTest {

  @Test
  public void checkDuplicateErrorCode() {
    Assertions.assertThat(Arrays.stream(CommonError.values()).map(CommonError::buildCode))
        .doesNotHaveDuplicates();
  }

  @Test
  public void buildCode_ShouldBuildCorrectCode() {
    // Arrange
    CommonError error = CommonError.SIGNATURE_SIGNING_FAILED;

    // Act
    String code = error.buildCode();

    // Assert
    Assertions.assertThat(code).isEqualTo("DL-COMMON-400001");
  }

  @Test
  public void buildMessage_ShouldBuildCorrectMessage() {
    // Arrange
    CommonError error = CommonError.SIGNATURE_SIGNING_FAILED;
    String reason = "reason";

    // Act
    String message = error.buildMessage(reason);

    // Assert
    Assertions.assertThat(message).isEqualTo("DL-COMMON-400001: Signing failed. Details: reason");
  }
}
