package com.scalar.dl.tablestore.client.util;

import static com.scalar.dl.tablestore.client.partiql.parser.ScalarPartiqlParser.INFORMATION_SCHEMA_TABLE_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import com.scalar.dl.ledger.util.JacksonSerDe;

public class JacksonUtils {
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  private JacksonUtils() {}

  public static ObjectNode createObjectNode() {
    return jacksonSerDe.getObjectMapper().createObjectNode();
  }

  public static ArrayNode createArrayNode() {
    return jacksonSerDe.getObjectMapper().createArrayNode();
  }

  public static JsonNode buildTable(String name) {
    return TextNode.valueOf(name);
  }

  public static JsonNode buildTable(String name, String alias) {
    return jacksonSerDe
        .getObjectMapper()
        .createObjectNode()
        .put(Constants.ALIAS_NAME, name)
        .put(Constants.ALIAS_AS, alias);
  }

  public static ObjectNode buildJoin(JsonNode table, String leftColumn, String rightColumn) {
    return jacksonSerDe
        .getObjectMapper()
        .createObjectNode()
        .put(Constants.JOIN_LEFT_KEY, leftColumn)
        .put(Constants.JOIN_RIGHT_KEY, rightColumn)
        .set(Constants.JOIN_TABLE, table);
  }

  public static JsonNode buildCondition(String column, String operatorSymbol, JsonNode value) {
    return jacksonSerDe
        .getObjectMapper()
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, toOperatorFrom(operatorSymbol))
        .set(Constants.CONDITION_VALUE, value);
  }

  public static JsonNode buildNullCondition(String column, boolean isNot) {
    String operator = isNot ? Constants.OPERATOR_IS_NOT_NULL : Constants.OPERATOR_IS_NULL;
    return jacksonSerDe
        .getObjectMapper()
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  public static String buildColumnReference(String tableReference, String column) {
    return tableReference + Constants.COLUMN_SEPARATOR + column;
  }

  public static JsonNode getValueFromCondition(JsonNode condition) {
    return condition.get(Constants.CONDITION_VALUE);
  }

  public static boolean isConditionForShowTables(JsonNode condition) {
    return condition.isObject()
        && condition.get(Constants.CONDITION_COLUMN).asText().equals(INFORMATION_SCHEMA_TABLE_NAME)
        && condition
            .get(Constants.CONDITION_OPERATOR)
            .asText()
            .equalsIgnoreCase(Constants.OPERATOR_EQ)
        && condition.get(Constants.CONDITION_VALUE).isTextual();
  }

  private static String toOperatorFrom(String symbol) {
    switch (symbol) {
      case "=":
        return Constants.OPERATOR_EQ;
      case "!=":
      case "<>":
        return Constants.OPERATOR_NE;
      case ">":
        return Constants.OPERATOR_GT;
      case ">=":
        return Constants.OPERATOR_GTE;
      case "<":
        return Constants.OPERATOR_LT;
      case "<=":
        return Constants.OPERATOR_LTE;
      default:
        throw new IllegalArgumentException("Unsupported operator: " + symbol);
    }
  }
}
