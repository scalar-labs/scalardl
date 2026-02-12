package com.scalar.dl.ledger.service.contract;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class HolderChecker extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    return Json.createObjectBuilder().add("holder", getCertificateKey().getEntityId()).build();
  }
}
