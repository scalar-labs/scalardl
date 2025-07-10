package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import java.util.Objects;

public class InsertStatement extends AbstractJacksonBasedContractStatement {

  private final String contractId;
  private final JsonNode arguments;

  private InsertStatement(JsonNode arguments) {
    this.contractId = Constants.CONTRACT_INSERT;
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
    if (!(o instanceof InsertStatement)) {
      return false;
    }
    InsertStatement that = (InsertStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
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
