package com.scalar.application.bankaccount.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class CreateAccount extends Contract {
  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey("id")) {
      throw new ContractContextException("a required key is missing: id");
    }

    String id = argument.getString("id");
    Optional<Asset> response = ledger.get(id);

    if (response.isPresent()) {
      throw new ContractContextException("account already exists");
    }

    ledger.put(id, Json.createObjectBuilder().add("balance", 0).build());
    return Json.createObjectBuilder()
        .add("status", "succeeded")
        .add("message", "account " + id + " created")
        .build();
  }
}
