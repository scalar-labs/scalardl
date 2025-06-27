package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class InsertStatementTest {
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void getArguments_CorrectStatementGiven_ShouldReturnCorrectArguments() {
    // Arrange
    InsertStatement statement =
        InsertStatement.create("tbl", mapper.createObjectNode().put("col", "aaa"));
    String expected = "{\"table\":\"tbl\",\"values\":{\"col\":\"aaa\"}}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
