package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.JsonObject;

public class TestContract extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    return JsonObject.EMPTY_JSON_OBJECT;
  }
}
