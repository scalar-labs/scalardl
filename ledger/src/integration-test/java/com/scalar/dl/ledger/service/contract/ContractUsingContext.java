package com.scalar.dl.ledger.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class ContractUsingContext extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    // pass the context to functions
    setContext(argument);

    return null;
  }
}
