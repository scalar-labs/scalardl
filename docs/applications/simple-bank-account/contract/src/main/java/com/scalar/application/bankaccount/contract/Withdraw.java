package com.scalar.application.bankaccount.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;

import javax.annotation.Nullable;
import java.util.Optional;

public class Withdraw extends JacksonBasedContract {
  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode property) {
    if (!(argument.has("id") && argument.has("amount"))) {
      throw new ContractContextException("a required key is missing: id and/or amount");
    }

    String id = argument.get("id").asText();
    long amount = argument.get("amount").asLong();

    if (amount < 0) {
      throw new ContractContextException("amount is negative");
    }

    Optional<Asset<JsonNode>> response = ledger.get(id);

    if (!response.isPresent()) {
      throw new ContractContextException("account does not exist");
    }

    long oldBalance = response.get().data().get("balance").asLong();
    long newBalance = oldBalance - amount;

    if (newBalance < 0) {
      throw new ContractContextException("insufficient funds");
    }

    ledger.put(id, getObjectMapper().createObjectNode().put("balance", newBalance));
    return getObjectMapper()
        .createObjectNode()
        .put("status", "succeeded")
        .put("old_balance", oldBalance)
        .put("new_balance", newBalance);
  }
}
