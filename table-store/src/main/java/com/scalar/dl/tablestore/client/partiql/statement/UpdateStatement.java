package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.table.Constants;
import java.util.List;

public class UpdateStatement extends AbstractJacksonBasedContractStatement {

  private UpdateStatement(JsonNode arguments) {
    super(Constants.CONTRACT_UPDATE, arguments);
  }

  private static JsonNode buildArguments(String table, JsonNode values, List<JsonNode> conditions) {
    ObjectNode arguments = jacksonSerDe.getObjectMapper().createObjectNode();
    arguments.put(Constants.UPDATE_TABLE, table);
    arguments.set(Constants.UPDATE_VALUES, values);
    ArrayNode conditionArray = jacksonSerDe.getObjectMapper().createArrayNode();
    conditions.forEach(conditionArray::add);
    arguments.set(Constants.UPDATE_CONDITIONS, conditionArray);
    return arguments;
  }

  public static UpdateStatement create(String table, JsonNode values, List<JsonNode> conditions) {
    return new UpdateStatement(buildArguments(table, values, conditions));
  }
}
