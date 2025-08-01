package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import org.junit.jupiter.api.Test;

public class SelectStatementTest {

  @Test
  public void
      getArguments_CorrectStatementWithoutProjectionsAndJoinsGiven_ShouldReturnCorrectArguments() {
    // Arrange
    SelectStatement statement =
        SelectStatement.create(
            JacksonUtils.buildTable("tbl"),
            ImmutableList.of(
                JacksonUtils.buildCondition("col1", "=", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col2", "!=", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col3", "<>", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col4", ">", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col5", ">=", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col6", "<", IntNode.valueOf(1)),
                JacksonUtils.buildCondition("col7", "<=", IntNode.valueOf(1)),
                JacksonUtils.buildNullCondition("col8", false),
                JacksonUtils.buildNullCondition("col9", true)),
            ImmutableList.of());
    String expected =
        "{\"table\":\"tbl\","
            + "\"conditions\":["
            + "{\"column\":\"col1\",\"operator\":\"EQ\",\"value\":1},"
            + "{\"column\":\"col2\",\"operator\":\"NE\",\"value\":1},"
            + "{\"column\":\"col3\",\"operator\":\"NE\",\"value\":1},"
            + "{\"column\":\"col4\",\"operator\":\"GT\",\"value\":1},"
            + "{\"column\":\"col5\",\"operator\":\"GTE\",\"value\":1},"
            + "{\"column\":\"col6\",\"operator\":\"LT\",\"value\":1},"
            + "{\"column\":\"col7\",\"operator\":\"LTE\",\"value\":1},"
            + "{\"column\":\"col8\",\"operator\":\"IS_NULL\"},"
            + "{\"column\":\"col9\",\"operator\":\"IS_NOT_NULL\"}"
            + "]}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }

  @Test
  public void
      getArguments_CorrectStatementWithProjectionsAndJoinsGiven_ShouldReturnCorrectArguments() {
    // Arrange
    SelectStatement statement =
        SelectStatement.create(
            JacksonUtils.buildTable("tbl1"),
            ImmutableList.of(
                JacksonUtils.buildJoin(JacksonUtils.buildTable("tbl2"), "tbl1.col2", "tbl2.col2")),
            ImmutableList.of(JacksonUtils.buildCondition("col1", "=", IntNode.valueOf(1))),
            ImmutableList.of("tbl1.col1", "tbl2.col2"));
    String expected =
        "{\"table\":\"tbl1\","
            + "\"conditions\":["
            + "{\"column\":\"col1\",\"operator\":\"EQ\",\"value\":1}"
            + "],"
            + "\"projections\":[\"tbl1.col1\",\"tbl2.col2\"],"
            + "\"joins\":["
            + "{\"left\":\"tbl1.col2\",\"right\":\"tbl2.col2\",\"table\":\"tbl2\"}"
            + "]}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
