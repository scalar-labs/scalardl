package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.util.Objects;

public abstract class AbstractJacksonBasedContractStatement implements ContractStatement {

  private final String contractId;
  private final JsonNode arguments;
  protected static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  public AbstractJacksonBasedContractStatement(String contractId, JsonNode arguments) {
    this.contractId = contractId;
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
    if (!(o instanceof AbstractJacksonBasedContractStatement)) {
      return false;
    }
    AbstractJacksonBasedContractStatement that = (AbstractJacksonBasedContractStatement) o;
    return Objects.equals(contractId, that.contractId) && Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contractId, arguments.hashCode());
  }
}
