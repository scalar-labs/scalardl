package com.scalar.application.bankaccount.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class Deposit extends JacksonBasedContract {
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    if (!argument.has("id") || !argument.has("amount")) {
      throw new ContractContextException("a required key is missing: id and/or amount");
    }

    String id = argument.get("id").asText();
    long amount = argument.get("amount").asLong();

    if (amount < 0) {
      throw new ContractContextException("amount is negative");
    }

    Optional<Asset<JsonNode>> asset = ledger.get(id);

    if (!asset.isPresent()) {
      throw new ContractContextException("account does not exist");
    }

    long oldBalance = asset.get().data().get("balance").asLong();
    long newBalance = oldBalance + amount;

    ledger.put(id, getObjectMapper().createObjectNode().put("balance", newBalance));
    return getObjectMapper()
        .createObjectNode()
        .put("status", "succeeded")
        .put("old_balance", oldBalance)
        .put("new_balance", newBalance);
  }
}
