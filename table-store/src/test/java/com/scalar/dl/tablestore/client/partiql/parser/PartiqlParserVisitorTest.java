package com.scalar.dl.tablestore.client.partiql.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.tablestore.client.partiql.DataType;
import com.scalar.dl.tablestore.client.partiql.statement.ContractStatement;
import com.scalar.dl.tablestore.client.partiql.statement.CreateTableStatement;
import com.scalar.dl.tablestore.client.partiql.statement.InsertStatement;
import com.scalar.dl.tablestore.client.partiql.statement.SelectStatement;
import com.scalar.dl.tablestore.client.partiql.statement.UpdateStatement;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PartiqlParserVisitorTest {

  @Test
  public void parse_CreateTableSqlGiven_ShouldParseCorrectly() {
    // Arrange Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
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
                ScalarPartiqlParser.parse("CREATE TABLE tbl (col1 INT, col2 BOOLEAN, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      parse_CreateTableSqlWithMultiplePrimaryKeysGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                ScalarPartiqlParser.parse(
                    "CREATE TABLE tbl (col1 INT PRIMARY KEY, col2 BOOLEAN PRIMARY KEY, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void parse_CreateTableSqlWithInvalidConstraintGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                ScalarPartiqlParser.parse(
                    "CREATE TABLE tbl (col1 INT PRIMARY KEY, col2 BOOLEAN NOT NULL, col3 STRING)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void parse_InsertSqlGiven_ShouldParseCorrectly() {
    // Arrange
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.valueOf(2);
    JsonNode array = JacksonUtils.createArrayNode().add(one).add(two);
    JsonNode object = JacksonUtils.createObjectNode().put("x", one).put("y", one);

    // Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
            "INSERT INTO tbl VALUES {};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: false, col3: 123, col4: 1.23, col5: 1.23e4, col6: null};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: [1, [1, 2], {x: 1, y: 1}]};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: {col3: [1, 2]}};"
                + "INSERT INTO tbl VALUES {col1: 'aaa', col2: {col3: {x: 1, y: 1}}};"
                + "INSERT INTO \"tbl\" VALUES {\"col1\": 'aaa'};");

    // Assert
    assertThat(statements.size()).isEqualTo(6);
    assertThat(statements.get(0))
        .isEqualTo(InsertStatement.create("tbl", JacksonUtils.createObjectNode()));
    assertThat(statements.get(1))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
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
                JacksonUtils.createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", JacksonUtils.createArrayNode().add(one).add(array).add(object))));
    assertThat(statements.get(3))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", JacksonUtils.createObjectNode().set("col3", array))));
    assertThat(statements.get(4))
        .isEqualTo(
            InsertStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .put("col1", "aaa")
                    .set("col2", JacksonUtils.createObjectNode().set("col3", object))));
    assertThat(statements.get(5))
        .isEqualTo(
            InsertStatement.create("tbl", JacksonUtils.createObjectNode().put("col1", "aaa")));
  }

  @Test
  public void parse_InsertSqlWithBigNumbersGiven_ShouldParseCorrectly() {
    // Arrange
    BigInteger bigInteger = BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE);
    BigDecimal bigDecimal = new BigDecimal("1.234567890123456789");

    // Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
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
            InsertStatement.create("tbl", JacksonUtils.createObjectNode().put("col1", bigInteger)));
    assertThat(statements.get(1))
        .isEqualTo(
            InsertStatement.create("tbl", JacksonUtils.createObjectNode().put("col1", bigDecimal)));
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
      assertThatThrownBy(() -> ScalarPartiqlParser.parse(sql), sql)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void parse_UpdateSqlGiven_ShouldParseCorrectly() {
    // Arrange
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.valueOf(2);
    JsonNode array = JacksonUtils.createArrayNode().add(one).add(two);
    JsonNode object = JacksonUtils.createObjectNode().put("x", one).put("y", one);

    // Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
            "UPDATE tbl SET col1='aaa';"
                + "UPDATE tbl SET col1='aaa', col2=false, col3=123, col4=1.23, col5=1.23e4, col6=null WHERE col1 = 'aaa';"
                + "UPDATE tbl SET col1='aaa' WHERE col1 = 'aaa' AND col2 != false AND col3 > 123 AND col4 < 1.23 AND col5 >= 1.23e4 AND col6 <= 1.23e-4;"
                + "UPDATE tbl SET col1='aaa' WHERE col1 IS NULL AND col2 IS NOT NULL;"
                + "UPDATE tbl SET col1='aaa' WHERE col1 = 'aaa' AND (col2 = 'bbb' AND col3 = 'ccc');"
                + "UPDATE tbl SET col1=[1, [1, 2], {x: 1, y: 1}];"
                + "UPDATE tbl SET col1={col2: [1, 2]};"
                + "UPDATE tbl SET col1={col2: {x: 1, y: 1}};"
                + "UPDATE \"tbl\" SET \"col1\"='aaa';");

    // Assert
    assertThat(statements.size()).isEqualTo(9);
    assertThat(statements.get(0))
        .isEqualTo(
            UpdateStatement.create(
                "tbl", JacksonUtils.createObjectNode().put("col1", "aaa"), ImmutableList.of()));
    assertThat(statements.get(1))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .put("col1", "aaa")
                    .put("col2", false)
                    .put("col3", new BigInteger("123"))
                    .put("col4", new BigDecimal("1.23"))
                    .put("col5", new BigDecimal("1.23e4"))
                    .set("col6", null),
                ImmutableList.of(
                    JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa")))));
    assertThat(statements.get(2))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode().put("col1", "aaa"),
                ImmutableList.of(
                    JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa")),
                    JacksonUtils.buildCondition("col2", "!=", BooleanNode.valueOf(false)),
                    JacksonUtils.buildCondition(
                        "col3", ">", BigIntegerNode.valueOf(new BigInteger("123"))),
                    JacksonUtils.buildCondition(
                        "col4", "<", DecimalNode.valueOf(new BigDecimal("1.23"))),
                    JacksonUtils.buildCondition(
                        "col5", ">=", DecimalNode.valueOf(new BigDecimal("1.23e4"))),
                    JacksonUtils.buildCondition(
                        "col6", "<=", DecimalNode.valueOf(new BigDecimal("1.23e-4"))))));
    assertThat(statements.get(3))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode().put("col1", "aaa"),
                ImmutableList.of(
                    JacksonUtils.buildNullCondition("col1", false),
                    JacksonUtils.buildNullCondition("col2", true))));
    assertThat(statements.get(4))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode().put("col1", "aaa"),
                ImmutableList.of(
                    JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa")),
                    JacksonUtils.buildCondition("col2", "=", TextNode.valueOf("bbb")),
                    JacksonUtils.buildCondition("col3", "=", TextNode.valueOf("ccc")))));
    assertThat(statements.get(5))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .set("col1", JacksonUtils.createArrayNode().add(one).add(array).add(object)),
                ImmutableList.of()));
    assertThat(statements.get(6))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .set("col1", JacksonUtils.createObjectNode().set("col2", array)),
                ImmutableList.of()));
    assertThat(statements.get(7))
        .isEqualTo(
            UpdateStatement.create(
                "tbl",
                JacksonUtils.createObjectNode()
                    .set("col1", JacksonUtils.createObjectNode().set("col2", object)),
                ImmutableList.of()));
    assertThat(statements.get(8))
        .isEqualTo(
            UpdateStatement.create(
                "tbl", JacksonUtils.createObjectNode().put("col1", "aaa"), ImmutableList.of()));
  }

  @Test
  public void parse_InvalidUpdateSqlGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    List<String> sqlStatements =
        ImmutableList.of(
            "UPDATE tbl AS t SET col1='aaa'",
            "UPDATE tbl SET tbl.col1='aaa'",
            "UPDATE tbl SET col1='aaa' WHERE col1 = 'aaa' OR col2 = 'bbb'",
            "UPDATE tbl SET col1='aaa' WHERE (col1 = 'aaa' OR col1 = 'bbb') AND col2 = 'ccc'");

    // Act Assert
    for (String sql : sqlStatements) {
      assertThatThrownBy(() -> ScalarPartiqlParser.parse(sql), sql)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void parse_SelectSqlGiven_ShouldParseCorrectly() {
    // Arrange
    JsonNode table = JacksonUtils.buildTable("tbl");
    JsonNode tableWithAlias = JacksonUtils.buildTable("tbl", "t");

    // Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
            "SELECT * FROM tbl;"
                + "SELECT col1, col2 FROM tbl;"
                + "SELECT * FROM tbl WHERE col1 = 'aaa';"
                + "SELECT * FROM tbl as t WHERE t.col1 = 'aaa';"
                + "SELECT * FROM \"tbl\" WHERE \"col1\" = 'aaa';"
                + "SELECT * FROM tbl WHERE col1 = 'aaa' AND col2 != false AND col3 > 123 AND col4 < 1.23 AND col5 >= 1.23e4 AND col6 <= 1.23e-4;"
                + "SELECT * FROM tbl WHERE col1 IS NULL AND col2 IS NOT NULL;"
                + "SELECT * FROM tbl WHERE col1 = NULL;" // Accept in parser, but reject in contract
                + "SELECT * FROM tbl WHERE col1 = 'aaa' AND (col2 = 'bbb' AND col3 = 'ccc');");

    // Assert
    assertThat(statements.size()).isEqualTo(9);
    assertThat(statements.get(0))
        .isEqualTo(SelectStatement.create(table, ImmutableList.of(), ImmutableList.of()));
    assertThat(statements.get(1))
        .isEqualTo(
            SelectStatement.create(table, ImmutableList.of(), ImmutableList.of("col1", "col2")));
    assertThat(statements.get(2))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa"))),
                ImmutableList.of()));
    assertThat(statements.get(3))
        .isEqualTo(
            SelectStatement.create(
                tableWithAlias,
                ImmutableList.of(
                    JacksonUtils.buildCondition("t.col1", "=", TextNode.valueOf("aaa"))),
                ImmutableList.of()));
    assertThat(statements.get(4))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa"))),
                ImmutableList.of()));
    assertThat(statements.get(5))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(
                    JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa")),
                    JacksonUtils.buildCondition("col2", "!=", BooleanNode.valueOf(false)),
                    JacksonUtils.buildCondition(
                        "col3", ">", BigIntegerNode.valueOf(new BigInteger("123"))),
                    JacksonUtils.buildCondition(
                        "col4", "<", DecimalNode.valueOf(new BigDecimal("1.23"))),
                    JacksonUtils.buildCondition(
                        "col5", ">=", DecimalNode.valueOf(new BigDecimal("1.23e4"))),
                    JacksonUtils.buildCondition(
                        "col6", "<=", DecimalNode.valueOf(new BigDecimal("1.23e-4")))),
                ImmutableList.of()));
    assertThat(statements.get(6))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(
                    JacksonUtils.buildNullCondition("col1", false),
                    JacksonUtils.buildNullCondition("col2", true)),
                ImmutableList.of()));
    assertThat(statements.get(7))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(JacksonUtils.buildCondition("col1", "=", NullNode.getInstance())),
                ImmutableList.of()));
    assertThat(statements.get(8))
        .isEqualTo(
            SelectStatement.create(
                table,
                ImmutableList.of(
                    JacksonUtils.buildCondition("col1", "=", TextNode.valueOf("aaa")),
                    JacksonUtils.buildCondition("col2", "=", TextNode.valueOf("bbb")),
                    JacksonUtils.buildCondition("col3", "=", TextNode.valueOf("ccc"))),
                ImmutableList.of()));
  }

  @Test
  public void parse_InvalidSelectSqlGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    List<String> sqlStatements =
        ImmutableList.of(
            "SELECT * FROM tbl AS t AT x",
            // We should reject below, but we cannot do it for now due to a parser-side issue.
            // The BY clause is just ignored.
            // "SELECT * FROM tbl AS t BY x",
            "SELECT * FROM UNPIVOT tbl",
            "SELECT * FROM (SELECT * FROM tbl) AS t",
            "SELECT DISTINCT * FROM tbl",
            "SELECT DISTINCT col1, col2 FROM tbl",
            "SELECT col1 AS c FROM tbl",
            "SELECT tbl.* AS c FROM tbl",
            "SELECT count() FROM tbl",
            "SELECT * FROM tbl WHERE col1 = {col2: 'aaa', col3: 'bbb'}",
            "SELECT * FROM tbl WHERE col1 = ['aaa', 'bbb']",
            "SELECT * FROM tbl WHERE a.b.col1 = 'aaa'",
            "SELECT * FROM tbl WHERE col1 = 'aaa' ORDER BY col1",
            "SELECT * FROM tbl WHERE col1 = 'aaa' OFFSET 10",
            "SELECT * FROM tbl WHERE col1 = 'aaa' LIMIT 10",
            "SELECT * EXCLUDE col1 FROM tbl",
            "SELECT * EXCLUDE t.col1 FROM tbl AS t",
            "SELECT * FROM tbl LET a AS b",
            "SELECT * FROM tbl GROUP BY col1",
            "SELECT * FROM tbl HAVING col1 = 'aaa'",
            "SELECT * FROM tbl1 UNION SELECT * FROM tbl2",
            "SELECT * FROM tbl1 EXCEPT SELECT * FROM tbl2",
            "WITH tmp AS (SELECT * FROM tbl) SELECT * FROM tmp",
            "PIVOT a AT b FROM tmp");

    // Act Assert
    for (String sql : sqlStatements) {
      assertThatThrownBy(() -> ScalarPartiqlParser.parse(sql), sql)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void parse_SelectSqlForJoinGiven_ShouldParseCorrectly() {
    // Arrange
    JsonNode table1 = JacksonUtils.buildTable("tbl1");
    JsonNode table2 = JacksonUtils.buildTable("tbl2");
    JsonNode tableWithAlias1 = JacksonUtils.buildTable("tbl1", "t1");
    JsonNode tableWithAlias2 = JacksonUtils.buildTable("tbl2", "t2");
    JsonNode tableWithAlias3 = JacksonUtils.buildTable("tbl3", "t3");

    // Act
    List<ContractStatement> statements =
        ScalarPartiqlParser.parse(
            "SELECT * FROM tbl1 INNER JOIN tbl2 ON tbl1.col1 = tbl2.id;"
                + "SELECT * FROM tbl1 JOIN tbl2 ON tbl1.col1 = tbl2.id;"
                + "SELECT t1.col1, t2.col2, t3.col3 FROM tbl1 AS t1 INNER JOIN tbl2 AS t2 ON t1.col1 = t2.id INNER JOIN tbl3 AS t3 ON t1.col2 = t3.id WHERE t1.id = 'aaa';");

    // Assert
    assertThat(statements.size()).isEqualTo(3);
    assertThat(statements.get(0))
        .isEqualTo(
            SelectStatement.create(
                table1,
                ImmutableList.of(JacksonUtils.buildJoin(table2, "tbl1.col1", "tbl2.id")),
                ImmutableList.of(),
                ImmutableList.of()));
    assertThat(statements.get(1))
        .isEqualTo(
            SelectStatement.create(
                table1,
                ImmutableList.of(JacksonUtils.buildJoin(table2, "tbl1.col1", "tbl2.id")),
                ImmutableList.of(),
                ImmutableList.of()));
    assertThat(statements.get(2))
        .isEqualTo(
            SelectStatement.create(
                tableWithAlias1,
                ImmutableList.of(
                    JacksonUtils.buildJoin(tableWithAlias2, "t1.col1", "t2.id"),
                    JacksonUtils.buildJoin(tableWithAlias3, "t1.col2", "t3.id")),
                ImmutableList.of(
                    JacksonUtils.buildCondition("t1.id", "=", TextNode.valueOf("aaa"))),
                ImmutableList.of("t1.col1", "t2.col2", "t3.col3")));
  }

  @Test
  public void parse_InvalidSelectSqlForJoinGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    List<String> sqlStatements =
        ImmutableList.of(
            "SELECT * FROM tbl1, tbl2 WHERE tbl1.col1 = tbl2.col1",
            "SELECT * FROM tbl1 OUTER JOIN tbl2 ON tbl1.col1 = tbl2.col1",
            "SELECT * FROM tbl1 INNER JOIN tbl2 ON tbl1.col1 > tbl2.col1",
            "SELECT * FROM tbl1 INNER JOIN tbl2 ON col1 = col2");

    // Act Assert
    for (String sql : sqlStatements) {
      assertThatThrownBy(() -> ScalarPartiqlParser.parse(sql), sql)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
