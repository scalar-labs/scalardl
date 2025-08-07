package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import org.junit.jupiter.api.Test;

public class GetHistoryStatementTest {

  @Test
  public void getArguments_CorrectStatementWithoutLimitGiven_ShouldReturnCorrectArguments() {
    // Arrange
    GetHistoryStatement statement =
        GetHistoryStatement.create(
            JacksonUtils.buildTable("tbl"),
            ImmutableList.of(JacksonUtils.buildCondition("col1", "=", IntNode.valueOf(1))),
            0);
    String expected =
        "{\"table\":\"tbl\","
            + "\"conditions\":["
            + "{\"column\":\"col1\",\"operator\":\"EQ\",\"value\":1}"
            + "]}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }

  @Test
  public void getArguments_CorrectStatementWithLimitGiven_ShouldReturnCorrectArguments() {
    // Arrange
    GetHistoryStatement statement =
        GetHistoryStatement.create(
            JacksonUtils.buildTable("tbl"),
            ImmutableList.of(JacksonUtils.buildCondition("col1", "=", IntNode.valueOf(1))),
            10);
    String expected =
        "{\"table\":\"tbl\","
            + "\"conditions\":[{\"column\":\"col1\",\"operator\":\"EQ\",\"value\":1}],"
            + "\"limit\":10}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
