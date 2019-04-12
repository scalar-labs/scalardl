package com.scalar.application.bankaccount.contract;

import com.scalar.ledger.asset.Asset;
import com.scalar.ledger.contract.Contract;
import com.scalar.ledger.exception.ContractContextException;
import com.scalar.ledger.ledger.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class Withdraw extends Contract {
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
    long newBalance = oldBalance - amount;

    if (newBalance < 0) {
      throw new ContractContextException("insufficient funds");
    }

    ledger.put(id, Json.createObjectBuilder().add("balance", newBalance).build());
    return Json.createObjectBuilder()
        .add("status", "succeeded")
        .add("old_balance", oldBalance)
        .add("new_balance", newBalance)
        .build();
  }
}
