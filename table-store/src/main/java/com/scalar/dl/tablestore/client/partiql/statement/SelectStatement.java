package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import java.util.List;
import java.util.Objects;

public class SelectStatement extends AbstractJacksonBasedContractStatement {

  private static final String contractId = Constants.CONTRACT_SELECT;
  private final JsonNode arguments;

  private SelectStatement(JsonNode arguments) {
    this.arguments = Objects.requireNonNull(arguments);
  }

  @Override
  public String getContractId() {
    return contractId;
  }

  @Override
  public String getArguments() {
    return jacksonSerDe.serialize(arguments);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("contractId", getContractId())
        .add("arguments", getArguments())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SelectStatement)) {
      return false;
    }
    SelectStatement that = (SelectStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
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
