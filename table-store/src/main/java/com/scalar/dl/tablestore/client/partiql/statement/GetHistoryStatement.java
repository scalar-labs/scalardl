package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import java.util.List;

public class GetHistoryStatement extends AbstractJacksonBasedContractStatement {

  private GetHistoryStatement(JsonNode arguments) {
    super(Constants.CONTRACT_GET_HISTORY, arguments);
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
