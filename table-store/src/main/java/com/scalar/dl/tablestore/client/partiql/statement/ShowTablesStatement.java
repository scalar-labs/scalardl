package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;

public class ShowTablesStatement extends AbstractJacksonBasedContractStatement {

  private ShowTablesStatement(JsonNode arguments) {
    super(Constants.CONTRACT_SHOW_TABLES, arguments);
  }

  private static ObjectNode buildArguments() {
    return jacksonSerDe.getObjectMapper().createObjectNode();
  }

  private static JsonNode buildArguments(String table) {
    return buildArguments().put(Constants.TABLE_NAME, table);
  }

  public static ShowTablesStatement create() {
    return new ShowTablesStatement(buildArguments());
  }

  public static ShowTablesStatement create(String table) {
    return new ShowTablesStatement(buildArguments(table));
  }
}
