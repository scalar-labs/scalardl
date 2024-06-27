package com.scalar.application.bankaccount.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class Transfer extends JacksonBasedContract {
  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode property) {
    if (!(argument.has("from") && argument.has("to") && argument.has("amount"))) {
      throw new ContractContextException("a required key is missing: from, to, and/or amount");
    }

    String fromId = argument.get("from").asText();
    String toId = argument.get("to").asText();
    long amount = argument.get("amount").asLong();

    if (amount < 0) {
      throw new ContractContextException("amount is negative");
    }

    Optional<Asset<JsonNode>> fromAsset = ledger.get(fromId);
    Optional<Asset<JsonNode>> toAsset = ledger.get(toId);

    if (!fromAsset.isPresent()) {
      throw new ContractContextException("from account does not exist");
    }

    if (!toAsset.isPresent()) {
      throw new ContractContextException("to account does not exist");
    }

    long fromOldBalance = fromAsset.get().data().get("balance").asLong();
    long fromNewBalance = fromOldBalance - amount;
    long toOldBalance = toAsset.get().data().get("balance").asLong();
    long toNewBalance = toOldBalance + amount;

    if (fromNewBalance < 0) {
      throw new ContractContextException("insufficient funds");
    }

    ledger.put(fromId, getObjectMapper().createObjectNode().put("balance", fromNewBalance));
    ledger.put(toId, getObjectMapper().createObjectNode().put("balance", toNewBalance));

    return getObjectMapper()
        .createObjectNode()
        .put("status", "succeeded")
        .put("from_old_balance", fromOldBalance)
        .put("from_new_balance", fromNewBalance)
        .put("to_old_balance", toOldBalance)
        .put("to_new_balance", toNewBalance);
  }
}
