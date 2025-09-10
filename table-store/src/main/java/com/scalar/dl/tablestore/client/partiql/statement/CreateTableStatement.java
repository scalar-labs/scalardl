package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.tablestore.client.partiql.DataType;

public class CreateTableStatement extends AbstractJacksonBasedContractStatement {

  private CreateTableStatement(JsonNode arguments) {
    super(Constants.CONTRACT_CREATE, arguments);
  }

  private static JsonNode buildArguments(
      String table,
      String primaryKey,
      DataType primaryKeyType,
      ImmutableMap<String, DataType> indexes) {
    ObjectNode arguments = jacksonSerDe.getObjectMapper().createObjectNode();
    arguments.put(Constants.TABLE_NAME, table);
    arguments.put(Constants.TABLE_KEY, primaryKey);
    arguments.put(Constants.TABLE_KEY_TYPE, primaryKeyType.name());
    ArrayNode indexArray = jacksonSerDe.getObjectMapper().createArrayNode();
    indexes.forEach(
        (indexKey, indexKeyType) ->
            indexArray.add(
                jacksonSerDe
                    .getObjectMapper()
                    .createObjectNode()
                    .put(Constants.INDEX_KEY, indexKey)
                    .put(Constants.INDEX_KEY_TYPE, indexKeyType.name())));
    arguments.set(Constants.TABLE_INDEXES, indexArray);
    return arguments;
  }

  public static CreateTableStatement create(
      String table,
      String primaryKey,
      DataType primaryKeyType,
      ImmutableMap<String, DataType> indexes) {
    return new CreateTableStatement(buildArguments(table, primaryKey, primaryKeyType, indexes));
  }
}
