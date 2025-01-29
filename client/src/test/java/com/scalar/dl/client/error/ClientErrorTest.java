package com.scalar.dl.client.error;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientErrorTest {

  @Test
  public void checkDuplicateErrorCode() {
    Assertions.assertThat(Arrays.stream(ClientError.values()).map(ClientError::buildCode))
        .doesNotHaveDuplicates();
  }

  @Test
  public void buildCode_ShouldBuildCorrectCode() {
    // Arrange
    ClientError error = ClientError.OPTION_ASSET_ID_IS_MALFORMED;

    // Act
    String code = error.buildCode();

    // Assert
    Assertions.assertThat(code).isEqualTo("DL-CLIENT-414001");
  }

  @Test
  public void buildMessage_ShouldBuildCorrectMessage() {
    // Arrange
    ClientError error = ClientError.OPTION_ASSET_ID_IS_MALFORMED;

    // Act
    String message = error.buildMessage();

    // Assert
    Assertions.assertThat(message)
        .isEqualTo(
            "DL-CLIENT-414001: The specified option --asset-id is malformed. The format should be \"[assetId]\" or \"[assetId],[startAge],[endAge]\".");
  }
}
