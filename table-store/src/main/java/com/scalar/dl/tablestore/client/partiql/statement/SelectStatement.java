package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import java.util.List;

public class SelectStatement extends AbstractJacksonBasedContractStatement {

  private SelectStatement(JsonNode arguments) {
    super(Constants.CONTRACT_SELECT, arguments);
  }

  private static ObjectNode buildArguments(
      JsonNode table, List<JsonNode> predicates, List<String> projections) {
    ObjectNode arguments = jacksonSerDe.getObjectMapper().createObjectNode();
    arguments.set(Constants.QUERY_TABLE, table);
    ArrayNode conditions = jacksonSerDe.getObjectMapper().createArrayNode();
    predicates.forEach(conditions::add);
    arguments.set(Constants.QUERY_CONDITIONS, conditions);
    if (!projections.isEmpty()) {
      ArrayNode projectionArray = jacksonSerDe.getObjectMapper().createArrayNode();
      projections.forEach(projectionArray::add);
      arguments.set(Constants.QUERY_PROJECTIONS, projectionArray);
    }
    return arguments;
  }

  private static ObjectNode buildArguments(
      JsonNode table, List<JsonNode> joins, List<JsonNode> predicates, List<String> projections) {
    ObjectNode arguments = buildArguments(table, predicates, projections);
    ArrayNode joinArray = jacksonSerDe.getObjectMapper().createArrayNode();
    joins.forEach(joinArray::add);
    arguments.set(Constants.QUERY_JOINS, joinArray);
    return arguments;
  }

  public static SelectStatement create(
      JsonNode table, List<JsonNode> predicates, List<String> projections) {
    return new SelectStatement(buildArguments(table, predicates, projections));
  }

  public static SelectStatement create(
      JsonNode table, List<JsonNode> joins, List<JsonNode> predicates, List<String> projections) {
    return new SelectStatement(buildArguments(table, joins, predicates, projections));
  }
}
