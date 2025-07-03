package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import com.scalar.dl.tablestore.client.partiql.DataType;
import java.util.Objects;

public class CreateTableStatement extends AbstractJacksonBasedContractStatement {

  private final String contractId;
  private final JsonNode arguments;

  private CreateTableStatement(JsonNode arguments) {
    this.contractId = Constants.CONTRACT_CREATE;
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
    if (!(o instanceof CreateTableStatement)) {
      return false;
    }
    CreateTableStatement that = (CreateTableStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
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
