package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;

public class InsertStatement extends AbstractJacksonBasedContractStatement {

  private InsertStatement(JsonNode arguments) {
    super(Constants.CONTRACT_INSERT, arguments);
  }

  private static JsonNode buildArguments(String table, JsonNode values) {
    ObjectNode arguments = jacksonSerDe.getObjectMapper().createObjectNode();
    arguments.put(Constants.RECORD_TABLE, table);
    arguments.set(Constants.RECORD_VALUES, values);
    return arguments;
  }

  public static InsertStatement create(String table, JsonNode values) {
    return new InsertStatement(buildArguments(table, values));
  }
}
