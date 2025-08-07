package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import java.util.Objects;

public class ShowTablesStatement extends AbstractJacksonBasedContractStatement {

  private final String contractId;
  private final JsonNode arguments;

  private ShowTablesStatement(JsonNode arguments) {
    this.contractId = Constants.CONTRACT_SHOW_TABLES;
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
    if (!(o instanceof ShowTablesStatement)) {
      return false;
    }
    ShowTablesStatement that = (ShowTablesStatement) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments);
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
