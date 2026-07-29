package com.scalar.dl.ledger.service;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class BadPropertyContract extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    return Json.createObjectBuilder().add("os_name", System.getProperty("os.name")).build();
  }
}
