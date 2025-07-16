package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import org.junit.jupiter.api.Test;

public class UpdateStatementTest {

  @Test
  public void getArguments_CorrectStatementGiven_ShouldReturnCorrectArguments() {
    // Arrange
    UpdateStatement statement =
        UpdateStatement.create(
            "tbl",
            JacksonUtils.createObjectNode().put("col", "aaa"),
            ImmutableList.of(JacksonUtils.buildCondition("col1", "=", IntNode.valueOf(1))));
    String expected =
        "{\"table\":\"tbl\","
            + "\"values\":{\"col\":\"aaa\"},"
            + "\"conditions\":[{\"column\":\"col1\",\"operator\":\"EQ\",\"value\":1}]}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
