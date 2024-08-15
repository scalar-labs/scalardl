package com.scalar.application.bankaccount.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class CreateAccount extends JacksonBasedContract {
  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode property) {
    if (!argument.has("id")) {
      throw new ContractContextException("a required key is missing: id");
    }

    String id = argument.get("id").asText();
    Optional<Asset<JsonNode>> response = ledger.get(id);

    if (response.isPresent()) {
      throw new ContractContextException("account already exists");
    }

    ledger.put(id, getObjectMapper().createObjectNode().put("balance", 0));
    return getObjectMapper()
        .createObjectNode()
        .put("status", "succeeded")
        .put("message", "account " + id + " created");
  }
}
