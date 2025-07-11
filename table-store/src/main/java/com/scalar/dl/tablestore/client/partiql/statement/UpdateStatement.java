package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import java.util.List;
import java.util.Objects;

public class UpdateStatement extends AbstractJacksonBasedContractStatement {

  private static final String contractId = Constants.CONTRACT_UPDATE;
  private final JsonNode arguments;

  private UpdateStatement(JsonNode arguments) {
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
    if (!(o instanceof UpdateStatement)) {
      return false;
    }
    UpdateStatement that = (UpdateStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
  }

  private static JsonNode buildArguments(String table, JsonNode values, List<JsonNode> predicates) {
    ObjectNode arguments = jacksonSerDe.getObjectMapper().createObjectNode();
    arguments.put(Constants.UPDATE_TABLE, table);
    arguments.set(Constants.UPDATE_VALUES, values);
    ArrayNode conditions = jacksonSerDe.getObjectMapper().createArrayNode();
    predicates.forEach(conditions::add);
    arguments.set(Constants.UPDATE_CONDITIONS, conditions);
    return arguments;
  }

  public static UpdateStatement create(String table, JsonNode values, List<JsonNode> predicates) {
    return new UpdateStatement(buildArguments(table, values, predicates));
  }
}
