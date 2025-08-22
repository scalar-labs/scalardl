package com.scalar.dl.tablestore.client.partiql.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ShowTablesStatementTest {

  @Test
  public void getArguments_CorrectStatementWithoutTableGiven_ShouldReturnCorrectArguments() {
    // Arrange
    ShowTablesStatement statement = ShowTablesStatement.create();
    String expected = "{}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }

  @Test
  public void getArguments_CorrectStatementWithTableGiven_ShouldReturnCorrectArguments() {
    // Arrange
    ShowTablesStatement statement = ShowTablesStatement.create("tbl");
    String expected = "{\"name\":\"tbl\"}";

    // Act
    String arguments = statement.getArguments();

    // Assert
    assertThat(arguments).isEqualTo(expected);
  }
}
