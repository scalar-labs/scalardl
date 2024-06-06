package com.scalar.application.bankaccount.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class Deposit extends Contract {
  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!(argument.containsKey("id") && argument.containsKey("amount"))) {
      throw new ContractContextException("a required key is missing: id and/or amount");
    }

    String id = argument.getString("id");
    long amount = argument.getJsonNumber("amount").longValue();

    if (amount < 0) {
      throw new ContractContextException("amount is negative");
    }

    Optional<Asset> response = ledger.get(id);

    if (!response.isPresent()) {
      throw new ContractContextException("account does not exist");
    }

    long oldBalance = response.get().data().getInt("balance");
    long newBalance = oldBalance + amount;

    ledger.put(id, Json.createObjectBuilder().add("balance", newBalance).build());
    return Json.createObjectBuilder()
        .add("status", "succeeded")
        .add("old_balance", oldBalance)
        .add("new_balance", newBalance)
        .build();
  }
}
