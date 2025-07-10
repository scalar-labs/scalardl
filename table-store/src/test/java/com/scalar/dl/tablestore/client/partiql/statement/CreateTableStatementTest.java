package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.scalar.dl.tablestore.client.partiql.DataType;
import org.junit.jupiter.api.Test;

public class CreateTableStatementTest {

  @Test
  public void getArguments_CorrectStatementGiven_ShouldReturnCorrectArguments() {
    // Arrange
    CreateTableStatement statement =
        CreateTableStatement.create(
            "tbl",
            "pkey",
            DataType.STRING,
            ImmutableMap.of(
                "idx1", DataType.NUMBER,
                "idx2", DataType.BOOLEAN));
    String expected =
        "{\"name\":\"tbl\","
            + "\"key\":\"pkey\","
            + "\"type\":\"STRING\","
            + "\"indexes\":["
            + "{\"key\":\"idx1\",\"type\":\"NUMBER\"},"
            + "{\"key\":\"idx2\",\"type\":\"BOOLEAN\"}"
            + "]}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
