package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import java.util.List;
import java.util.Objects;

public class GetHistoryStatement extends AbstractJacksonBasedContractStatement {

  private final String contractId;
  private final JsonNode arguments;

  private GetHistoryStatement(JsonNode arguments) {
    this.contractId = Constants.CONTRACT_GET_HISTORY;
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
    if (!(o instanceof GetHistoryStatement)) {
      return false;
    }
    GetHistoryStatement that = (GetHistoryStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
  }

  private static JsonNode buildArguments(JsonNode table, List<JsonNode> conditions, int limit) {
    ObjectNode arguments = JacksonUtils.createObjectNode();
    arguments.set(Constants.QUERY_TABLE, table);
    ArrayNode conditionArray = jacksonSerDe.getObjectMapper().createArrayNode();
    conditions.forEach(conditionArray::add);
    arguments.set(Constants.QUERY_CONDITIONS, conditionArray);
    if (limit > 0) {
      arguments.put(Constants.QUERY_LIMIT, limit);
    }
    return arguments;
  }

  public static GetHistoryStatement create(JsonNode table, List<JsonNode> conditions, int limit) {
    return new GetHistoryStatement(buildArguments(table, conditions, limit));
  }
}
