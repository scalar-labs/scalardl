package com.scalar.dl.tablestore.client.error;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TableStoreClientErrorTest {

  @Test
  public void checkDuplicateErrorCode() {
    Assertions.assertThat(
            Arrays.stream(TableStoreClientError.values()).map(TableStoreClientError::buildCode))
        .doesNotHaveDuplicates();
  }

  @Test
  public void buildCode_ShouldBuildCorrectCode() {
    // Arrange
    TableStoreClientError error = TableStoreClientError.SYNTAX_ERROR_IN_PARTIQL_PARSER;

    // Act
    String code = error.buildCode();

    // Assert
    Assertions.assertThat(code).isEqualTo("DL-TABLE-STORE-414001");
  }

  @Test
  public void buildMessage_ShouldBuildCorrectMessage() {
    // Arrange
    TableStoreClientError error = TableStoreClientError.SYNTAX_ERROR_IN_PARTIQL_PARSER;

    // Act
    String message = error.buildMessage(1, 1, 1, "code");

    // Assert
    Assertions.assertThat(message)
        .isEqualTo("DL-TABLE-STORE-414001: Syntax error. Line=1, Offset=1, Length=1, Code=code");
  }
}
