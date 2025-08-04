package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.dl.tablestore.client.util.JacksonUtils;
import org.junit.jupiter.api.Test;

public class InsertStatementTest {

  @Test
  public void getArguments_CorrectStatementGiven_ShouldReturnCorrectArguments() {
    // Arrange
    InsertStatement statement =
        InsertStatement.create("tbl", JacksonUtils.createObjectNode().put("col", "aaa"));
    String expected = "{\"table\":\"tbl\",\"values\":{\"col\":\"aaa\"}}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
