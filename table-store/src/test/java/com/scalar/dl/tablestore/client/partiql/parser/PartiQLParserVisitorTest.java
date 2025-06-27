package com.scalar.dl.tablestore.client.partiql.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.tablestore.client.partiql.DataType;
import com.scalar.dl.tablestore.client.partiql.statement.ContractStatement;
import com.scalar.dl.tablestore.client.partiql.statement.CreateTableStatement;
import com.scalar.dl.tablestore.client.partiql.statement.InsertStatement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PartiQLParserVisitorTest {
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void parse_CreateTableSqlGiven_ShouldParseCorrectly() {
    // Arrange Act
    List<ContractStatement> statements =
        ScalarPartiQLParser.parse(
            "CREATE TABLE tbl (col1 STRING PRIMARY KEY);"
                + "CREATE TABLE tbl (col1 STRING PRIMARY KEY, col2 BOOL, col3 BOOLEAN);"
                + "CREATE TABLE tbl (col1 INT PRIMARY KEY, col2 STRING);"
                + "CREATE TABLE tbl (col1 BOOLEAN PRIMARY KEY, col2 INT, col3 INTEGER, col4 BIGINT, col5 FLOAT, col6 DOUBLE PRECISION);");

    // Assert
    assertThat(statements.size()).isEqualTo(4);
    assertThat(statements.get(0))
        .isEqualTo(CreateTableStatement.create("tbl", "col1", DataType.STRING, ImmutableMap.of()));
    assertThat(statements.get(1))
        .isEqualTo(
            CreateTableStatement.create(
                "tbl",
                "col1",
                DataType.STRING,
                ImmutableMap.of("col2", DataType.BOOLEAN, "col3", DataType.BOOLEAN)));
    assertThat(statements.get(2))
        .isEqualTo(
            CreateTableStatement.create(
                "tbl", "col1", DataType.NUMBER, ImmutableMap.of("col2", DataType.STRING)));
    assertThat(statements.get(3))
        .isEqualTo(
            CreateTableStatement.create(
                "tbl",
                "col1",
                DataType.BOOLEAN,
                ImmutableMap.of(
                    "col2",
                    DataType.NUMBER,
                    "col3",
                    DataType.NUMBER,
                    "col4",
                    DataType.NUMBER,
                    "col5",
                    DataType.NUMBER,
                    "col6",
                    DataType.NUMBER)));
  }

  @Test
  public void parse_CreateTableSqlWithoutPrimaryKeyGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                ScalarPartiQLParser.parse("CREATE TABLE tbl (col1 INT, col2 BOOLEAN, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      parse_CreateTableSqlWithMultiplePrimaryKeysGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                ScalarPartiQLParser.parse(
                    "CREATE TABLE tbl (col1 INT PRIMARY KEY, col2 BOOLEAN PRIMARY KEY, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void parse_CreateTableSqlWithInvalidConstraintGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                ScalarPartiQLParser.parse(
                    "CREATE TABLE tbl (col1 INT PRIMARY KEY, col2 BOOLEAN NOT NULL, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void parse_InsertSqlGiven_ShouldParseCorrectly() {
    // Arrange
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.valueOf(2);
    JsonNode array = mapper.createArrayNode().add(one).add(two);
    JsonNode object = mapper.createObjectNode().put("x", one).put("y", one);

    // Act
    List<ContractStatement> statements =
        ScalarPartiQLParser.parse(
            "INSERT INTO tbl VALUES {};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: false, col3: 123, col4: 1.23, col5: 1.23e4, col6: null};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: [1, [1, 2], {x: 1, y: 1}]};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: {col3: [1, 2]}};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: {col3: {x: 1, y: 1}}};"
                + "INSERT INTO \"tbl\" VALUES {\"col1\": 'aaa'};");

    // Assert
    assertThat(statements.size()).isEqualTo(6);
    assertThat(statements.get(0))
        .isEqualTo(InsertStatement.create("tbl", mapper.createObjectNode()));
    assertThat(statements.get(1))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                mapper
                    .createObjectNode()
                    .put("col1", "aaa")
                    .put("col2", false)
                    .put("col3", new BigInteger("123"))
                    .put("col4", new BigDecimal("1.23"))
                    .put("col5", new BigDecimal("1.23e4"))
                    .set("col6", null)));
    assertThat(statements.get(2))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                mapper
                    .createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", mapper.createArrayNode().add(one).add(array).add(object))));
    assertThat(statements.get(3))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                mapper
                    .createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", mapper.createObjectNode().set("col3", array))));
    assertThat(statements.get(4))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                mapper
                    .createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", mapper.createObjectNode().set("col3", object))));
    assertThat(statements.get(5))
        .isEqualTo(InsertStatement.create("tbl", mapper.createObjectNode().put("col1", "aaa")));
  }

  @Test
  public void parse_InsertSqlWithBigNumbersGiven_ShouldParseCorrectly() {
    // Arrange
    BigInteger bigInteger = BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE);
    BigDecimal bigDecimal = new BigDecimal("1.234567890123456789");

    // Act
    List<ContractStatement> statements =
        ScalarPartiQLParser.parse(
            "INSERT INTO tbl VALUES { col1: "
                + bigInteger
                + " };"
                + "INSERT INTO tbl VALUES { col1: "
                + bigDecimal
                + " };");

    // Assert
    assertThat(statements.size()).isEqualTo(2);
    assertThat(statements.get(0))
        .isEqualTo(
            InsertStatement.create("tbl", mapper.createObjectNode().put("col1", bigInteger)));
    assertThat(statements.get(1))
        .isEqualTo(
            InsertStatement.create("tbl", mapper.createObjectNode().put("col1", bigDecimal)));
  }

  @Test
  public void parse_InvalidInsertSqlGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    List<String> sqlStatements =
        ImmutableList.of(
            "INSERT INTO tbl(col1, col2) VALUES ('aaa', 'bbb')",
            "INSERT INTO tbl(col1, col2) VALUES {col1: 'aaa', col2: 'bbb'}",
            "INSERT INTO tbl VALUES ['aaa', 'bbb']",
            "INSERT INTO tbl VALUES {\"col1\": \"aaa\"}",
            "INSERT INTO tbl VALUES {col1: 'aaa', col2: 'bbb'} ON CONFLICT(col1) DO NOTHING",
            "INSERT INTO tbl AS t VALUES {col1: 'aaa', col2: 'bbb'}",
            "INSERT INTO tbl DEFAULT VALUES",
            "INSERT INTO tbl(col1, col2) SELECT col1, col2 FROM tbl2");

    // Act Assert
    for (String sql : sqlStatements) {
      assertThatThrownBy(() -> ScalarPartiQLParser.parse(sql), sql)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
