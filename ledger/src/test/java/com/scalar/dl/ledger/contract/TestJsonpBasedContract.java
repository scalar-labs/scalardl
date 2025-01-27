package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public class TestJsonpBasedContract extends JsonpBasedContract {

  @Override
  public JsonObject invoke(
      Ledger<JsonObject> ledger, JsonObject argument, @Nullable JsonObject properties) {
    return JsonObject.EMPTY_JSON_OBJECT;
  }
}
