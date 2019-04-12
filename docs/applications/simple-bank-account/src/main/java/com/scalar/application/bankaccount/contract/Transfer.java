package com.scalar.application.bankaccount.contract;

import com.scalar.ledger.asset.Asset;
import com.scalar.ledger.contract.Contract;
import com.scalar.ledger.exception.ContractContextException;
import com.scalar.ledger.ledger.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class Transfer extends Contract {
  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!(argument.containsKey("from")
        && argument.containsKey("to")
        && argument.containsKey("amount"))) {
      throw new ContractContextException("a required key is missing: from, to, and/or amount");
    }

    String fromId = argument.getString("from");
    String toId = argument.getString("to");
    long amount = argument.getJsonNumber("amount").longValue();

    if (amount < 0) {
      throw new ContractContextException("amount is negative");
    }

    Optional<Asset> fromAsset = ledger.get(fromId);
    Optional<Asset> toAsset = ledger.get(toId);

    if (!fromAsset.isPresent()) {
      throw new ContractContextException("from account does not exist");
    }

    if (!toAsset.isPresent()) {
      throw new ContractContextException("to account does not exist");
    }

    long fromOldBalance = fromAsset.get().data().getInt("balance");
    long fromNewBalance = fromOldBalance - amount;
    long toOldBalance = toAsset.get().data().getInt("balance");
    long toNewBalance = toOldBalance + amount;

    if (fromNewBalance < 0) {
      throw new ContractContextException("insufficient funds");
    }

    ledger.put(fromId, Json.createObjectBuilder().add("balance", fromNewBalance).build());
    ledger.put(toId, Json.createObjectBuilder().add("balance", toNewBalance).build());
    return Json.createObjectBuilder()
        .add("status", "succeeded")
        .add("from_old_balance", fromOldBalance)
        .add("from_new_balance", fromNewBalance)
        .add("to_old_balance", toOldBalance)
        .add("to_new_balance", toNewBalance)
        .build();
  }
}
